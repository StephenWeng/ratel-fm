package com.ratel.fm.service.basic;

import com.ratel.fm.common.BusinessException;
import com.ratel.fm.common.ResponseCode;
import com.ratel.fm.domain.basic.BasicDictionary;
import com.ratel.fm.repository.basic.BasicDictionaryRepository;
import com.ratel.fm.security.CompanyScope;
import com.ratel.fm.service.audit.AuditLogService;
import com.ratel.fm.service.knowledge.KnowledgeIndexService;
import com.ratel.fm.web.dto.basic.BasicDictionaryDtos.BasicDictionaryRequest;
import com.ratel.fm.web.dto.basic.BasicDictionaryDtos.BasicDictionaryView;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 基础信息字典服务。
 *
 * <p>负责采购方、物流方等层级字典的维护、启用项读取和关键操作日志记录。</p>
 *
 * <p>开发组织：ratel；开发人员：WenZhang；联系方式：18782945613。</p>
 *
 * @author ratel
 */
@Service
public class BasicDictionaryService {

    /**
     * 常量 GENERATED_CODE_PREFIX：保存 GENERATED_CODE_PREFIX 对应的业务数据、运行配置或依赖对象，供本类逻辑读取和维护。
     */
    private static final String GENERATED_CODE_PREFIX = "DICT_";
    /**
     * 随机码生成器，用于用户未填写字典编码时生成低冲突字典编码。
     */
    private static final SecureRandom RANDOM = new SecureRandom();

    /**
     * 字段 dictionaryRepository：保存 dictionaryRepository 对应的业务数据、运行配置或依赖对象，供本类逻辑读取和维护。
     */
    private final BasicDictionaryRepository dictionaryRepository;
    /**
     * 字段 auditLogService：保存 auditLogService 对应的业务数据、运行配置或依赖对象，供本类逻辑读取和维护。
     */
    private final AuditLogService auditLogService;
    /**
     * 字段 knowledgeIndexService：基础字典变更后同步刷新 AI 知识库，避免智能检索命中旧基础资料。
     */
    private final KnowledgeIndexService knowledgeIndexService;

    /**
     * 构造 BasicDictionaryService 实例。
     * 
     * <p>实现步骤：
     * 1. 接收调用方传入的依赖对象或初始化参数；
     * 2. 保存到成员字段，保证后续业务方法可以复用；
     * 3. 完成实例初始化。</p>
     */
    public BasicDictionaryService(
            BasicDictionaryRepository dictionaryRepository,
            AuditLogService auditLogService,
            KnowledgeIndexService knowledgeIndexService
    ) {
        this.dictionaryRepository = dictionaryRepository;
        this.auditLogService = auditLogService;
        this.knowledgeIndexService = knowledgeIndexService;
    }

    /**
     * 查询全部基础字典树。
     *
     * <p>实现步骤：
     * 1. 按排序号和主键读取全部字典；
     * 2. 构造父子映射；
     * 3. 返回根节点集合，前端默认只展开第一层。</p>
     */
    @Transactional(readOnly = true)
    public List<BasicDictionaryView> listTree() {
        // 变量说明：dictionaries 保存当前步骤计算、查询或转换得到的中间结果。
        List<BasicDictionary> dictionaries = dictionaryRepository.findAllByOrderBySortOrderAscIdAsc().stream()
                .filter(this::visibleInDictionaryManagement)
                .toList();
        Map<Long, List<BasicDictionary>> childrenMap = dictionaries.stream()
                .filter(item -> item.getParent() != null)
                .collect(Collectors.groupingBy(item -> item.getParent().getId()));
        return dictionaries.stream()
                .filter(item -> item.getParent() == null)
                .map(item -> toView(item, childrenMap))
                .toList();
    }

    /**
     * 查询根层级基础字典。
     *
     * <p>实现步骤：
     * 1. 只读取 parent_id 为空的第一层字典；
     * 2. 每个节点只返回自身字段和是否存在下级；
     * 3. 前端展开节点时再调用直接子级接口，避免全国行政区划等大字典一次性加载导致页面卡顿。</p>
     */
    @Transactional(readOnly = true)
    public List<BasicDictionaryView> listRoots() {
        return dictionaryRepository.findByParentIsNullOrderBySortOrderAscIdAsc().stream()
                .filter(this::visibleInDictionaryManagement)
                .map(this::toLazyView)
                .toList();
    }

    /**
     * 查询指定父级的直接子级基础字典。
     *
     * <p>实现步骤：
     * 1. 校验父级字典存在；
     * 2. 只读取该父级下一层子节点；
     * 3. 返回每个子节点的 hasChildren 标记，继续支持树表格按需展开。</p>
     */
    @Transactional(readOnly = true)
    public List<BasicDictionaryView> listChildren(Long parentId) {
        if (parentId == null) {
            return listRoots();
        }
        BasicDictionary parent = dictionaryRepository.findById(parentId).orElse(null);
        if (parent == null || !visibleInDictionaryManagement(parent)) {
            throw new BusinessException(HttpStatus.NOT_FOUND, ResponseCode.REF_OBJ_NOT_EXISIT, "父级字典不存在");
        }
        return dictionaryRepository.findByParentIdOrderBySortOrderAscIdAsc(parentId).stream()
                .filter(this::visibleInDictionaryManagement)
                .map(this::toLazyView)
                .toList();
    }

    /**
     * 按关键字搜索基础字典。
     *
     * <p>实现步骤：
     * 1. 关键字为空时只返回根层级，保持首屏轻量；
     * 2. 服务端读取全量字典进行名称、编码、说明匹配；
     * 3. 对命中节点补齐祖先链，保证搜索结果仍然能看出层级位置；
     * 4. 只返回命中路径，不返回整棵行政区划树，避免搜索后前端渲染过重。</p>
     */
    @Transactional(readOnly = true)
    public List<BasicDictionaryView> searchTree(String code, String name, String description, Boolean enabled, Long parentId) {
        // 变量说明：codeText 保存当前步骤计算、查询或转换得到的中间结果。
        String codeText = normalizedSearchText(code);
        // 变量说明：nameText 保存当前步骤计算、查询或转换得到的中间结果。
        String nameText = normalizedSearchText(name);
        // 变量说明：descriptionText 保存当前步骤计算、查询或转换得到的中间结果。
        String descriptionText = normalizedSearchText(description);
        if (codeText == null && nameText == null && descriptionText == null && enabled == null && parentId == null) {
            return listRoots();
        }
        // 变量说明：dictionaries 保存当前步骤计算、查询或转换得到的中间结果。
        List<BasicDictionary> dictionaries = dictionaryRepository.findAllByOrderBySortOrderAscIdAsc().stream()
                .filter(this::visibleInDictionaryManagement)
                .toList();
        // 变量说明：includedIds 保存当前步骤计算、查询或转换得到的中间结果。
        Set<Long> includedIds = new HashSet<>();
        for (BasicDictionary dictionary : dictionaries) {
            if (matchesSearch(dictionary, codeText, nameText, descriptionText, enabled, parentId)) {
                addSelfAndAncestors(dictionary, includedIds);
            }
        }
        Map<Long, List<BasicDictionary>> childrenMap = dictionaries.stream()
                .filter(item -> includedIds.contains(item.getId()))
                .filter(item -> item.getParent() != null && includedIds.contains(item.getParent().getId()))
                .collect(Collectors.groupingBy(item -> item.getParent().getId()));
        return dictionaries.stream()
                .filter(item -> includedIds.contains(item.getId()))
                .filter(item -> item.getParent() == null)
                .map(item -> toSearchView(item, childrenMap))
                .toList();
    }

    /**
     * 查询指定根字典下启用的直接子级。
     *
     * <p>实现步骤：
     * 1. 按根字典编码定位根节点；
     * 2. 根节点自身或任一上级停用时返回空集合；
     * 3. 根节点可展示时，只返回自身和所有上级均启用的直接子级。</p>
     */
    @Transactional(readOnly = true)
    public List<BasicDictionaryView> listEnabledChildren(String parentCode) {
        if (parentCode == null || parentCode.isBlank()) {
            return List.of();
        }
        return dictionaryRepository.findByCode(parentCode.trim())
                .filter(this::isDictionaryVisibleForBusiness)
                .map(parent -> dictionaryRepository.findByParentIdAndEnabledTrueOrderBySortOrderAscIdAsc(parent.getId()).stream()
                        .filter(this::isDictionaryVisibleForBusiness)
                        .map(item -> new BasicDictionaryView(
                                item.getId(),
                                item.getCode(),
                                item.getName(),
                                parent.getId(),
                                item.getSortOrder(),
                                item.isEnabled(),
                                item.getDescription(),
                                dictionaryRepository.existsByParentId(item.getId()),
                                List.of()
                        ))
                        .toList())
                .orElse(List.of());
    }

    /**
     * 查询指定父级 ID 下启用的直接子级字典。
     *
     * <p>实现步骤：
     * 1. 按父级 ID 读取父节点，父节点不存在或任一上级停用时返回空集合；
     * 2. 只查询该父级下一层启用子节点；
     * 3. 返回轻量节点和 hasChildren 标记，供前端级联组件继续懒加载下一层。</p>
     */
    @Transactional(readOnly = true)
    public List<BasicDictionaryView> listEnabledChildrenByParentId(Long parentId) {
        if (parentId == null) {
            return List.of();
        }
        BasicDictionary parent = dictionaryRepository.findById(parentId).orElse(null);
        if (parent == null || !isDictionaryVisibleForBusiness(parent)) {
            return List.of();
        }
        return dictionaryRepository.findByParentIdAndEnabledTrueOrderBySortOrderAscIdAsc(parent.getId()).stream()
                .filter(this::isDictionaryVisibleForBusiness)
                .map(this::toLazyView)
                .toList();
    }

    /**
     * 查询指定根字典下的启用字典树。
     *
     * <p>实现步骤：
     * 1. 按根字典编码定位根节点，根节点不存在或被停用时返回空集合；
     * 2. 读取全部字典构造父子映射；
     * 3. 从根节点直接子级开始递归，只保留自身和所有上级均启用的节点，供人员部门、组织、岗位等业务表单选择。</p>
     */
    @Transactional(readOnly = true)
    public List<BasicDictionaryView> listEnabledTree(String rootCode) {
        if (rootCode == null || rootCode.isBlank()) {
            return List.of();
        }
        BasicDictionary root = dictionaryRepository.findByCode(rootCode.trim())
                .filter(this::isDictionaryVisibleForBusiness)
                .orElse(null);
        if (root == null) {
            return List.of();
        }
        return dictionaryRepository.findByParentIdAndEnabledTrueOrderBySortOrderAscIdAsc(root.getId()).stream()
                .filter(this::isDictionaryVisibleForBusiness)
                .map(this::toEnabledViewByRepository)
                .toList();
    }

    /**
     * 新增基础字典。
     *
     * <p>实现步骤：
     * 1. 解析父级字典；
     * 2. 校验同一父级下名称唯一；
     * 3. 使用用户输入编码或生成随机编码，并校验编码唯一；
     * 4. 保存字典；
     * 5. 记录关键操作日志。</p>
     */
    @Transactional
    public BasicDictionaryView create(BasicDictionaryRequest request) {
        // 变量说明：dictionary 保存当前步骤计算、查询或转换得到的中间结果。
        BasicDictionary dictionary = new BasicDictionary();
        // 变量说明：parent 保存当前步骤计算、查询或转换得到的中间结果。
        BasicDictionary parent = resolveParent(request.parentId(), null);
        assertCanManageOrganizationDictionary(parent);
        assertCanUseOrganizationRootCode(request.code());
        // 变量说明：name 保存当前步骤计算、查询或转换得到的中间结果。
        String name = normalizedName(request.name());
        ensureSiblingNameUnique(parent, name, null);
        dictionary.setCode(resolveNewCode(request.code()));
        apply(dictionary, request, parent, name);
        // 变量说明：saved 保存已落库的字典实体，确保知识索引可以拿到稳定业务 ID。
        BasicDictionary saved = dictionaryRepository.save(dictionary);
        // 步骤：字典新增后立即写入知识库，使项目、物料、供应商等基础资料无需手动重建即可被检索。
        knowledgeIndexService.rebuildBasicDictionary(saved);
        // 变量说明：view 保存当前步骤计算、查询或转换得到的中间结果。
        BasicDictionaryView view = toView(saved, Map.of());
        auditLogService.record("CREATE_BASIC_DICTIONARY", request, "SUCCESS",
                "字典管理新增了字典项" + view.name() + "(" + view.code() + ")。");
        return view;
    }

    /**
     * 修改基础字典。
     *
     * <p>实现步骤：
     * 1. 读取现有字典；
     * 2. 解析父级并防止选择自身为父级；
     * 3. 校验同级名称唯一和编码唯一；
     * 4. 停用存在启用后代的字典时校验二次确认；
     * 5. 更新字典字段；
     * 6. 记录关键操作日志。</p>
     */
    @Transactional
    public BasicDictionaryView update(Long id, BasicDictionaryRequest request) {
        BasicDictionary dictionary = dictionaryRepository.findById(id)
                .orElseThrow(() -> new BusinessException(HttpStatus.NOT_FOUND, ResponseCode.REF_OBJ_NOT_EXISIT, "基础字典不存在"));
        assertCanManageOrganizationDictionary(dictionary);
        // 变量说明：parent 保存当前步骤计算、查询或转换得到的中间结果。
        BasicDictionary parent = resolveParent(request.parentId(), id);
        assertCanManageOrganizationDictionary(parent);
        // 变量说明：name 保存当前步骤计算、查询或转换得到的中间结果。
        String name = normalizedName(request.name());
        ensureSiblingNameUnique(parent, name, id);
        // 变量说明：requestedCode 保存当前步骤计算、查询或转换得到的中间结果。
        String requestedCode = normalizedCode(request.code());
        assertCanUseOrganizationRootCode(requestedCode);
        if (requestedCode != null && !requestedCode.equals(dictionary.getCode())) {
            dictionaryRepository.findByCode(requestedCode)
                    .filter(existing -> !Objects.equals(existing.getId(), id))
                    .ifPresent(existing -> {
                        throw new BusinessException(ResponseCode.OBJ_BEEN_USED, "字典编码已存在");
            });
            dictionary.setCode(requestedCode);
        }
        ensureDisableWithEnabledChildrenConfirmed(dictionary, request);
        apply(dictionary, request, parent, name);
        // 步骤：字典名称、层级或启停状态变更后刷新自身和后代索引，保证层级路径检索结果准确。
        rebuildDictionaryKnowledge(dictionary);
        // 变量说明：view 保存当前步骤计算、查询或转换得到的中间结果。
        BasicDictionaryView view = toView(dictionary, Map.of());
        auditLogService.record("UPDATE_BASIC_DICTIONARY", "dictionaryId=" + id + ", " + request, "SUCCESS",
                "字典管理修改了字典项" + view.name() + "(" + view.code() + ")。");
        return view;
    }

    /**
     * 删除基础字典。
     *
     * <p>实现步骤：校验字典存在；存在下级字典时拒绝删除；删除叶子节点并记录审计日志。</p>
     */
    @Transactional
    public void delete(Long id) {
        BasicDictionary dictionary = dictionaryRepository.findById(id)
                .orElseThrow(() -> new BusinessException(HttpStatus.NOT_FOUND, ResponseCode.REF_OBJ_NOT_EXISIT, "基础字典不存在"));
        assertCanManageOrganizationDictionary(dictionary);
        if (dictionaryRepository.existsByParentId(id)) {
            throw new BusinessException(ResponseCode.DELETE_FORBIDDEN, "存在下级字典，不允许删除");
        }
        dictionaryRepository.delete(dictionary);
        // 步骤：字典删除后同步移除知识库分片，避免 AI 助手继续引用已删除基础资料。
        knowledgeIndexService.deleteBasicDictionary(id);
        auditLogService.record("DELETE_BASIC_DICTIONARY", "dictionaryId=" + id + ", code=" + dictionary.getCode(),
                "SUCCESS", "字典管理删除了字典项" + dictionary.getName() + "(" + dictionary.getCode() + ")。");
    }

    /**
     * 刷新基础字典知识索引。
     *
     * <p>实现步骤：
     * 1. 先重建当前字典知识分片；
     * 2. 再查找当前字典的所有后代节点；
     * 3. 逐个重建后代知识分片，确保父级名称或层级调整后完整路径同步更新。</p>
     */
    private void rebuildDictionaryKnowledge(BasicDictionary dictionary) {
        knowledgeIndexService.rebuildBasicDictionary(dictionary);
        if (dictionary.getId() == null) {
            return;
        }
        dictionaryRepository.findAllByOrderBySortOrderAscIdAsc().stream()
                .filter(item -> !Objects.equals(item.getId(), dictionary.getId()))
                .filter(item -> isDescendantOf(item, dictionary.getId()))
                .forEach(knowledgeIndexService::rebuildBasicDictionary);
    }

    /**
     * 保存字典字段。
     *
     * <p>父级、名称、排序、启用状态和说明在新增修改时共用同一套赋值规则。</p>
     */
    private void apply(BasicDictionary dictionary, BasicDictionaryRequest request, BasicDictionary parent, String name) {
        dictionary.setName(name);
        dictionary.setParent(parent);
        dictionary.setSortOrder(request.sortOrder() == null ? 0 : Math.max(request.sortOrder(), 0));
        dictionary.setEnabled(request.enabled() == null || request.enabled());
        dictionary.setDescription(request.description());
    }

    /**
     * 校验停用父级字典时是否完成二次确认。
     *
     * <p>实现步骤：
     * 1. 仅在已启用字典被改为停用时检查；
     * 2. 遍历全部字典，判断是否存在启用状态的任意层级后代；
     * 3. 如果存在启用后代且请求未携带确认标记，则拒绝保存并提示前端弹出二次确认。</p>
     */
    private void ensureDisableWithEnabledChildrenConfirmed(BasicDictionary dictionary, BasicDictionaryRequest request) {
        // 变量说明：willDisable 保存当前步骤计算、查询或转换得到的中间结果。
        boolean willDisable = Boolean.FALSE.equals(request.enabled());
        if (!dictionary.isEnabled() || !willDisable) {
            return;
        }
        boolean hasEnabledDescendant = dictionaryRepository.findAllByOrderBySortOrderAscIdAsc().stream()
                .anyMatch(candidate -> candidate.isEnabled() && isDescendantOf(candidate, dictionary.getId()));
        if (!hasEnabledDescendant || Boolean.TRUE.equals(request.confirmDisableWithEnabledChildren())) {
            return;
        }
        throw new BusinessException(ResponseCode.WARN,
                "该字典存在启用状态的下级字典，停用后后续页面将不显示该字典及其下级数据，请确认是否停用。");
    }

    /**
     * 解析父级字典。
     *
     * <p>修改时不允许把字典挂到自身下，避免形成直接环形层级。</p>
     */
    private BasicDictionary resolveParent(Long parentId, Long currentId) {
        if (parentId == null) {
            return null;
        }
        if (Objects.equals(parentId, currentId)) {
            throw new BusinessException(ResponseCode.ILLEGAL_PARAM, "父级字典不能选择自身");
        }
        BasicDictionary parent = dictionaryRepository.findById(parentId)
                .orElseThrow(() -> new BusinessException(HttpStatus.NOT_FOUND, ResponseCode.REF_OBJ_NOT_EXISIT, "父级字典不存在"));
        if (isDescendantOf(parent, currentId)) {
            throw new BusinessException(ResponseCode.ILLEGAL_PARAM, "父级字典不能选择自身下级");
        }
        return parent;
    }

    /**
     * 判断候选父级是否是当前字典的下级。
     *
     * <p>实现步骤：从候选父级开始向上追溯父链，只要遇到当前字典 ID，就说明会形成环形树。</p>
     */
    private boolean isDescendantOf(BasicDictionary candidateParent, Long currentId) {
        if (currentId == null) {
            return false;
        }
        // 变量说明：cursor 保存当前步骤计算、查询或转换得到的中间结果。
        BasicDictionary cursor = candidateParent;
        while (cursor != null) {
            if (Objects.equals(cursor.getId(), currentId)) {
                return true;
            }
            cursor = cursor.getParent();
        }
        return false;
    }

    /**
     * 判断字典管理页面是否可展示当前字典。
     *
     * <p>实现步骤：
     * 1. 默认 admin 可以查看和维护全部字典；
     * 2. 非 admin 隐藏 ORGANIZATION 根字典及其子级；
     * 3. 业务表单下拉仍通过启用树接口读取组织类字典，不受字典管理可见性影响。</p>
     */
    private boolean visibleInDictionaryManagement(BasicDictionary dictionary) {
        if (CompanyScope.isSuperAdmin()) {
            return true;
        }
        return !isRootOrDescendantOf(dictionary, "ORGANIZATION");
    }

    /**
     * 校验当前登录人是否可以维护所属公司字典。
     *
     * <p>实现步骤：
     * 1. 空父级不需要特殊校验；
     * 2. admin 可维护全部字典；
     * 3. 非 admin 试图新增、修改或删除 ORGANIZATION 根及其子级时直接拒绝。</p>
     */
    private void assertCanManageOrganizationDictionary(BasicDictionary dictionary) {
        if (dictionary == null || visibleInDictionaryManagement(dictionary)) {
            return;
        }
        throw new BusinessException(ResponseCode.NO_AUTH, "只有admin用户可以维护所属公司字典");
    }

    /**
     * 校验当前登录人是否可以使用所属公司根编码。
     *
     * <p>实现步骤：
     * 1. 空编码继续走自动生成规则；
     * 2. 非 ORGANIZATION 编码不做限制；
     * 3. 非 admin 使用 ORGANIZATION 根编码时拒绝，防止新增或改名绕过所属公司字典权限。</p>
     */
    private void assertCanUseOrganizationRootCode(String code) {
        if (code == null || code.isBlank() || !"ORGANIZATION".equals(code.trim())) {
            return;
        }
        if (!CompanyScope.isSuperAdmin()) {
            throw new BusinessException(ResponseCode.NO_AUTH, "只有admin用户可以维护所属公司字典");
        }
    }

    /**
     * 判断字典是否为指定根编码或其后代。
     *
     * <p>实现步骤：从当前字典沿 parent 链向上查找，只要任一节点编码等于 rootCode 即命中。</p>
     */
    private boolean isRootOrDescendantOf(BasicDictionary dictionary, String rootCode) {
        BasicDictionary cursor = dictionary;
        while (cursor != null) {
            if (rootCode.equals(cursor.getCode())) {
                return true;
            }
            cursor = cursor.getParent();
        }
        return false;
    }

    /**
     * 判断字典是否可用于后续业务页面展示。
     *
     * <p>实现步骤：从当前字典向上追溯父级；只要自身或任一上级已停用，就判定整条链路不可展示。</p>
     */
    private boolean isDictionaryVisibleForBusiness(BasicDictionary dictionary) {
        // 变量说明：visited 保存当前步骤计算、查询或转换得到的中间结果。
        Set<Long> visited = new HashSet<>();
        // 变量说明：cursor 保存当前步骤计算、查询或转换得到的中间结果。
        BasicDictionary cursor = dictionary;
        while (cursor != null) {
            if (!cursor.isEnabled()) {
                return false;
            }
            if (cursor.getId() != null && !visited.add(cursor.getId())) {
                return false;
            }
            cursor = cursor.getParent();
        }
        return true;
    }

    /**
     * 校验同一父级下字典名称唯一。
     */
    private void ensureSiblingNameUnique(BasicDictionary parent, String name, Long currentId) {
        boolean exists = parent == null
                ? dictionaryRepository.existsByParentIsNullAndName(name)
                : dictionaryRepository.existsByParentIdAndName(parent.getId(), name);
        if (!exists) {
            return;
        }
        dictionaryRepository.findAllByOrderBySortOrderAscIdAsc().stream()
                .filter(item -> Objects.equals(item.getName(), name))
                .filter(item -> Objects.equals(parentId(item), parent == null ? null : parent.getId()))
                .filter(item -> !Objects.equals(item.getId(), currentId))
                .findFirst()
                .ifPresent(item -> {
                    throw new BusinessException(ResponseCode.OBJ_BEEN_USED, "同一层级下字典名称已存在");
                });
    }

    /**
     * 新增时解析字典编码。
     *
     * <p>用户输入编码时按唯一编码保存；用户未输入时生成随机编码并循环校验唯一性。</p>
     */
    private String resolveNewCode(String code) {
        // 变量说明：normalizedCode 保存当前步骤计算、查询或转换得到的中间结果。
        String normalizedCode = normalizedCode(code);
        if (normalizedCode != null) {
            if (dictionaryRepository.existsByCode(normalizedCode)) {
                throw new BusinessException(ResponseCode.OBJ_BEEN_USED, "字典编码已存在");
            }
            return normalizedCode;
        }
        String generatedCode;
        do {
            generatedCode = GENERATED_CODE_PREFIX + Long.toUnsignedString(RANDOM.nextLong(), 36).toUpperCase(Locale.ROOT);
        } while (dictionaryRepository.existsByCode(generatedCode));
        return generatedCode;
    }

    /**
     * 规范化字典编码。
     */
    private String normalizedCode(String code) {
        if (code == null || code.isBlank()) {
            return null;
        }
        return code.trim();
    }

    /**
     * 规范化字典名称。
     */
    private String normalizedName(String name) {
        return name == null ? "" : name.trim();
    }

    /**
     * 读取字典父级 ID。
     */
    private Long parentId(BasicDictionary dictionary) {
        return dictionary.getParent() == null ? null : dictionary.getParent().getId();
    }

    /**
     * 判断字典是否命中搜索关键字。
     */
    private boolean matchesSearch(
            BasicDictionary dictionary,
            String code,
            String name,
            String description,
            Boolean enabled,
            Long parentId
    ) {
        if (code != null && !containsIgnoreCase(dictionary.getCode(), code)) {
            return false;
        }
        if (name != null && !containsIgnoreCase(dictionary.getName(), name)) {
            return false;
        }
        if (description != null && !containsIgnoreCase(dictionary.getDescription(), description)) {
            return false;
        }
        if (enabled != null && enabled != dictionary.isEnabled()) {
            return false;
        }
        return parentId == null || Objects.equals(parentId(dictionary), parentId);
    }

    /**
     * 判断字段值是否包含搜索关键字。
     */
    private boolean containsIgnoreCase(String value, String keywordText) {
        return value != null && value.toLowerCase(Locale.ROOT).contains(keywordText);
    }

    /**
     * 规范化 like 搜索文本。
     */
    private String normalizedSearchText(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.trim().toLowerCase(Locale.ROOT);
    }

    /**
     * 将命中字典和所有上级加入搜索结果集合。
     */
    private void addSelfAndAncestors(BasicDictionary dictionary, Set<Long> includedIds) {
        // 变量说明：cursor 保存当前步骤计算、查询或转换得到的中间结果。
        BasicDictionary cursor = dictionary;
        while (cursor != null && cursor.getId() != null && includedIds.add(cursor.getId())) {
            cursor = cursor.getParent();
        }
    }

    /**
     * 将字典实体转换为懒加载树节点。
     *
     * <p>该方法只填充当前节点，不递归加载子级；hasChildren 用于提示前端继续按需加载。</p>
     */
    private BasicDictionaryView toLazyView(BasicDictionary dictionary) {
        return new BasicDictionaryView(
                dictionary.getId(),
                dictionary.getCode(),
                dictionary.getName(),
                parentId(dictionary),
                dictionary.getSortOrder(),
                dictionary.isEnabled(),
                dictionary.getDescription(),
                dictionaryRepository.existsByParentId(dictionary.getId()),
                null
        );
    }

    /**
     * 将字典实体转换为树节点。
     */
    private BasicDictionaryView toView(BasicDictionary dictionary, Map<Long, List<BasicDictionary>> childrenMap) {
        // 变量说明：children 保存当前步骤计算、查询或转换得到的中间结果。
        List<BasicDictionaryView> children = new ArrayList<>();
        for (BasicDictionary child : childrenMap.getOrDefault(dictionary.getId(), List.of())) {
            children.add(toView(child, childrenMap));
        }
        return new BasicDictionaryView(
                dictionary.getId(),
                dictionary.getCode(),
                dictionary.getName(),
                parentId(dictionary),
                dictionary.getSortOrder(),
                dictionary.isEnabled(),
                dictionary.getDescription(),
                !children.isEmpty(),
                children
        );
    }

    /**
     * 将搜索结果中的字典实体转换为局部树节点。
     *
     * <p>搜索结果只包含命中路径；如果子级已经随命中路径返回，则不再标记懒加载，避免树表格把同一节点同时当作本地子树和远程懒加载节点。</p>
     */
    private BasicDictionaryView toSearchView(BasicDictionary dictionary, Map<Long, List<BasicDictionary>> childrenMap) {
        // 变量说明：children 保存当前步骤计算、查询或转换得到的中间结果。
        List<BasicDictionaryView> children = new ArrayList<>();
        for (BasicDictionary child : childrenMap.getOrDefault(dictionary.getId(), List.of())) {
            children.add(toSearchView(child, childrenMap));
        }
        return new BasicDictionaryView(
                dictionary.getId(),
                dictionary.getCode(),
                dictionary.getName(),
                parentId(dictionary),
                dictionary.getSortOrder(),
                dictionary.isEnabled(),
                dictionary.getDescription(),
                children.isEmpty() && dictionaryRepository.existsByParentId(dictionary.getId()),
                children
        );
    }

    /**
     * 将启用字典实体转换为业务选择树节点。
     *
     * <p>实现步骤：递归遍历子级，仅保留自身和所有上级均启用的节点，确保父级停用后下级不会继续出现在业务页面。</p>
     */
    private BasicDictionaryView toEnabledView(BasicDictionary dictionary, Map<Long, List<BasicDictionary>> childrenMap) {
        List<BasicDictionaryView> children = childrenMap.getOrDefault(dictionary.getId(), List.of()).stream()
                .filter(this::isDictionaryVisibleForBusiness)
                .map(item -> toEnabledView(item, childrenMap))
                .toList();
        return new BasicDictionaryView(
                dictionary.getId(),
                dictionary.getCode(),
                dictionary.getName(),
                parentId(dictionary),
                dictionary.getSortOrder(),
                dictionary.isEnabled(),
                dictionary.getDescription(),
                !children.isEmpty(),
                children
        );
    }

    /**
     * 将启用字典实体转换为业务选择树节点。
     *
     * <p>实现步骤：
     * 1. 仅按当前节点 ID 查询直接启用子级，避免为小字典下拉读取全量基础字典；
     * 2. 递归转换启用子级，停用链路不返回；
     * 3. 返回完整层级树，供组织、部门、岗位等中小型字典展示层级名称。</p>
     */
    private BasicDictionaryView toEnabledViewByRepository(BasicDictionary dictionary) {
        List<BasicDictionaryView> children = dictionaryRepository.findByParentIdAndEnabledTrueOrderBySortOrderAscIdAsc(dictionary.getId()).stream()
                .filter(this::isDictionaryVisibleForBusiness)
                .map(this::toEnabledViewByRepository)
                .toList();
        return new BasicDictionaryView(
                dictionary.getId(),
                dictionary.getCode(),
                dictionary.getName(),
                parentId(dictionary),
                dictionary.getSortOrder(),
                dictionary.isEnabled(),
                dictionary.getDescription(),
                !children.isEmpty(),
                children
        );
    }
}
