package com.ratel.fm.service.workflow;

import com.alibaba.fastjson2.JSON;
import com.ratel.fm.common.BusinessException;
import com.ratel.fm.common.ResponseCode;
import com.ratel.fm.common.SearchSpecs;
import com.ratel.fm.domain.auth.UserAccount;
import com.ratel.fm.domain.workflow.WorkflowApproverType;
import com.ratel.fm.domain.workflow.WorkflowConfig;
import com.ratel.fm.domain.workflow.WorkflowDefinition;
import com.ratel.fm.domain.workflow.WorkflowInstance;
import com.ratel.fm.domain.workflow.WorkflowOperationLog;
import com.ratel.fm.domain.workflow.WorkflowOperationType;
import com.ratel.fm.domain.workflow.WorkflowStatus;
import com.ratel.fm.domain.workflow.WorkflowTask;
import com.ratel.fm.domain.workflow.WorkflowTaskStatus;
import com.ratel.fm.repository.auth.UserAccountRepository;
import com.ratel.fm.repository.workflow.WorkflowConfigRepository;
import com.ratel.fm.repository.workflow.WorkflowDefinitionRepository;
import com.ratel.fm.repository.workflow.WorkflowInstanceRepository;
import com.ratel.fm.repository.workflow.WorkflowOperationLogRepository;
import com.ratel.fm.repository.workflow.WorkflowTaskRepository;
import com.ratel.fm.security.CompanyScope;
import com.ratel.fm.security.CurrentUser;
import com.ratel.fm.security.SecurityUtils;
import com.ratel.fm.service.audit.AuditLogService;
import com.ratel.fm.web.dto.workflow.WorkflowDtos.WorkflowApproveRequest;
import com.ratel.fm.web.dto.workflow.WorkflowDtos.WorkflowBusinessFormView;
import com.ratel.fm.web.dto.workflow.WorkflowDtos.WorkflowConfigRequest;
import com.ratel.fm.web.dto.workflow.WorkflowDtos.WorkflowConfigView;
import com.ratel.fm.web.dto.workflow.WorkflowDtos.WorkflowDefinitionRequest;
import com.ratel.fm.web.dto.workflow.WorkflowDtos.WorkflowDefinitionView;
import com.ratel.fm.web.dto.workflow.WorkflowDtos.WorkflowInstanceDetailView;
import com.ratel.fm.web.dto.workflow.WorkflowDtos.WorkflowApproverUserView;
import com.ratel.fm.web.dto.workflow.WorkflowDtos.WorkflowItemView;
import com.ratel.fm.web.dto.workflow.WorkflowDtos.WorkflowNodeRequest;
import com.ratel.fm.web.dto.workflow.WorkflowDtos.WorkflowNodeView;
import com.ratel.fm.web.dto.workflow.WorkflowDtos.WorkflowOperationLogView;
import com.ratel.fm.web.dto.workflow.WorkflowDtos.WorkflowStartRequest;
import com.ratel.fm.web.dto.workflow.WorkflowDtos.WorkflowTaskView;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import jakarta.persistence.criteria.Path;
import jakarta.persistence.criteria.Predicate;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * 轻量流程引擎服务。
 *
 * <p>实现目的：
 * 1. 管理流程模板和功能模块流程配置；
 * 2. 业务单据按功能模块编码发起流程实例；
 * 3. 按当前登录人查询待办、已办、发起事宜；
 * 4. 审批同意时推进到下一节点，审批不同意时结束流程并回调业务模块。</p>
 */
@Service
public class WorkflowService {

    /** 流程模板数据访问对象。 */
    private final WorkflowDefinitionRepository definitionRepository;
    /** 流程配置数据访问对象。 */
    private final WorkflowConfigRepository configRepository;
    /** 流程实例数据访问对象。 */
    private final WorkflowInstanceRepository instanceRepository;
    /** 流程任务数据访问对象。 */
    private final WorkflowTaskRepository taskRepository;
    /** 流程操作流水数据访问对象。 */
    private final WorkflowOperationLogRepository operationLogRepository;
    /** 人员账号数据访问对象，用于解析部门和岗位审批范围。 */
    private final UserAccountRepository userRepository;
    /** 系统审计日志服务，用于记录流程关键操作。 */
    private final AuditLogService auditLogService;
    /** 业务回调映射，key 为业务类型，value 为对应业务模块回调实现。 */
    private final Map<String, WorkflowBusinessCallback> callbackMap;

    /**
     * 构造轻量流程引擎服务。
     *
     * <p>实现步骤：
     * 1. 注入流程定义、配置、实例、任务、流水和人员仓库；
     * 2. 注入审计日志服务；
     * 3. 将所有业务回调按 businessType 建立映射，供流程结束时调用。</p>
     */
    public WorkflowService(
            WorkflowDefinitionRepository definitionRepository,
            WorkflowConfigRepository configRepository,
            WorkflowInstanceRepository instanceRepository,
            WorkflowTaskRepository taskRepository,
            WorkflowOperationLogRepository operationLogRepository,
            UserAccountRepository userRepository,
            AuditLogService auditLogService,
            List<WorkflowBusinessCallback> callbacks
    ) {
        this.definitionRepository = definitionRepository;
        this.configRepository = configRepository;
        this.instanceRepository = instanceRepository;
        this.taskRepository = taskRepository;
        this.operationLogRepository = operationLogRepository;
        this.userRepository = userRepository;
        this.auditLogService = auditLogService;
        this.callbackMap = new HashMap<>();
        for (WorkflowBusinessCallback callback : callbacks) {
            this.callbackMap.put(callback.businessType(), callback);
        }
    }

    /**
     * 查询流程定义列表。
     *
     * <p>实现步骤：按当前所属公司过滤，再叠加名称、编码和启用状态查询，最后按编码正序返回。</p>
     */
    @Transactional(readOnly = true)
    public List<WorkflowDefinitionView> listDefinitions(String name, String code, Boolean enabled) {
        Specification<WorkflowDefinition> spec = CompanyScope.<WorkflowDefinition>currentCompanySpec()
                .and(SearchSpecs.like("name", name))
                .and(SearchSpecs.like("code", code))
                .and(SearchSpecs.equal("enabled", enabled));
        return definitionRepository.findAll(spec, Sort.by(Sort.Direction.ASC, "code", "id")).stream()
                .map(this::toDefinitionView)
                .toList();
    }

    /**
     * 保存流程定义。
     *
     * <p>实现步骤：
     * 1. 新增时校验模板编码在当前所属公司内唯一；
     * 2. 编辑时按当前所属公司读取旧模板；
     * 3. 规范化节点顺序和审批人显示文本；
     * 4. 保存节点 JSON，便于后续流程实例按快照运行；
     * 5. 记录流程定义维护审计日志。</p>
     */
    @Transactional
    public WorkflowDefinitionView saveDefinition(Long id, WorkflowDefinitionRequest request) {
        WorkflowDefinition definition = id == null
                ? new WorkflowDefinition()
                : definitionRepository.findByOrganizationCodeAndId(CompanyScope.currentCompanyCode(), id)
                .orElseThrow(() -> new BusinessException(ResponseCode.REF_OBJ_NOT_EXISIT, "流程定义不存在"));
        if (id == null && definitionRepository.existsByOrganizationCodeAndCode(CompanyScope.currentCompanyCode(), request.code())) {
            throw new BusinessException(ResponseCode.OBJ_BEEN_USED, "流程模板编码已存在");
        }
        if (id != null && !Objects.equals(definition.getCode(), request.code())
                && definitionRepository.existsByOrganizationCodeAndCode(CompanyScope.currentCompanyCode(), request.code())) {
            throw new BusinessException(ResponseCode.OBJ_BEEN_USED, "流程模板编码已存在");
        }
        List<WorkflowNodeView> nodes = normalizeNodes(request.nodes());
        definition.setOrganizationCode(CompanyScope.currentCompanyCode());
        definition.setName(request.name());
        definition.setCode(request.code());
        definition.setDescription(request.description());
        definition.setNodesJson(JSON.toJSONString(nodes));
        definition.setEnabled(request.enabled() == null || request.enabled());
        WorkflowDefinitionView view = toDefinitionView(definitionRepository.save(definition));
        auditLogService.record("SAVE_WORKFLOW_DEFINITION", request, "SUCCESS",
                "流程定义保存了模板" + view.name() + "(" + view.code() + ")。");
        return view;
    }

    /**
     * 删除流程定义。
     *
     * <p>实现步骤：按所属公司校验模板存在后删除；初期项目不保留历史表结构约束，后续可扩展为已使用模板禁止删除。</p>
     */
    @Transactional
    public void deleteDefinition(Long id) {
        WorkflowDefinition definition = definitionRepository.findByOrganizationCodeAndId(CompanyScope.currentCompanyCode(), id)
                .orElseThrow(() -> new BusinessException(ResponseCode.REF_OBJ_NOT_EXISIT, "流程定义不存在"));
        definitionRepository.delete(definition);
        auditLogService.record("DELETE_WORKFLOW_DEFINITION", "definitionId=" + id, "SUCCESS",
                "流程定义删除了模板" + definition.getName() + "(" + definition.getCode() + ")。");
    }

    /**
     * 查询启用流程定义。
     *
     * <p>实现步骤：固定当前所属公司，只返回启用模板给流程管理页面下拉选择。</p>
     */
    @Transactional(readOnly = true)
    public List<WorkflowDefinitionView> listEnabledDefinitions() {
        return definitionRepository.findByOrganizationCodeAndEnabledTrueOrderByNameAscIdAsc(CompanyScope.currentCompanyCode())
                .stream()
                .map(this::toDefinitionView)
                .toList();
    }

    /**
     * 查询流程配置列表。
     *
     * <p>实现步骤：按当前所属公司过滤，再叠加业务模块、功能模块和启用状态筛选。</p>
     */
    @Transactional(readOnly = true)
    public List<WorkflowConfigView> listConfigs(String businessModuleCode, String functionModuleCode, Boolean enabled) {
        Specification<WorkflowConfig> spec = CompanyScope.<WorkflowConfig>currentCompanySpec()
                .and(SearchSpecs.equal("businessModuleCode", blankToNull(businessModuleCode)))
                .and(SearchSpecs.equal("functionModuleCode", blankToNull(functionModuleCode)))
                .and(SearchSpecs.equal("enabled", enabled));
        return configRepository.findAll(spec, Sort.by(Sort.Direction.ASC, "businessModuleCode", "functionModuleCode", "id")).stream()
                .map(this::toConfigView)
                .toList();
    }

    /**
     * 保存流程配置。
     *
     * <p>实现步骤：
     * 1. 校验当前所属公司的流程定义存在且启用；
     * 2. 新增或编辑流程配置，功能模块编码同公司内唯一；
     * 3. 保存流程定义名称快照；
     * 4. 记录流程配置维护审计日志。</p>
     */
    @Transactional
    public WorkflowConfigView saveConfig(Long id, WorkflowConfigRequest request) {
        WorkflowDefinition definition = definitionRepository.findByOrganizationCodeAndId(CompanyScope.currentCompanyCode(), request.definitionId())
                .orElseThrow(() -> new BusinessException(ResponseCode.REF_OBJ_NOT_EXISIT, "流程定义不存在"));
        if (!definition.isEnabled()) {
            throw new BusinessException(ResponseCode.ILLEGAL_PARAM, "流程定义已停用，不能绑定");
        }
        WorkflowConfig config = id == null
                ? new WorkflowConfig()
                : configRepository.findByOrganizationCodeAndId(CompanyScope.currentCompanyCode(), id)
                .orElseThrow(() -> new BusinessException(ResponseCode.REF_OBJ_NOT_EXISIT, "流程配置不存在"));
        if (id == null && configRepository.existsByOrganizationCodeAndFunctionModuleCode(CompanyScope.currentCompanyCode(), request.functionModuleCode())) {
            throw new BusinessException(ResponseCode.OBJ_BEEN_USED, "该功能模块已配置流程");
        }
        if (id != null && !Objects.equals(config.getFunctionModuleCode(), request.functionModuleCode())
                && configRepository.existsByOrganizationCodeAndFunctionModuleCode(CompanyScope.currentCompanyCode(), request.functionModuleCode())) {
            throw new BusinessException(ResponseCode.OBJ_BEEN_USED, "该功能模块已配置流程");
        }
        config.setOrganizationCode(CompanyScope.currentCompanyCode());
        config.setBusinessModuleCode(request.businessModuleCode());
        config.setBusinessModuleName(request.businessModuleName());
        config.setFunctionModuleCode(request.functionModuleCode());
        config.setFunctionModuleName(request.functionModuleName());
        config.setDefinitionId(definition.getId());
        config.setDefinitionName(definition.getName());
        config.setEnabled(request.enabled() == null || request.enabled());
        WorkflowConfigView view = toConfigView(configRepository.save(config));
        auditLogService.record("SAVE_WORKFLOW_CONFIG", request, "SUCCESS",
                "流程管理保存了" + view.businessModuleName() + "/" + view.functionModuleName() + "配置。");
        return view;
    }

    /**
     * 删除流程配置。
     *
     * <p>实现步骤：按所属公司读取配置后删除，并记录审计日志。</p>
     */
    @Transactional
    public void deleteConfig(Long id) {
        WorkflowConfig config = configRepository.findByOrganizationCodeAndId(CompanyScope.currentCompanyCode(), id)
                .orElseThrow(() -> new BusinessException(ResponseCode.REF_OBJ_NOT_EXISIT, "流程配置不存在"));
        configRepository.delete(config);
        auditLogService.record("DELETE_WORKFLOW_CONFIG", "configId=" + id, "SUCCESS",
                "流程管理删除了" + config.getBusinessModuleName() + "/" + config.getFunctionModuleName() + "配置。");
    }

    /**
     * 发起流程审批。
     *
     * <p>实现步骤：
     * 1. 按功能模块编码读取启用流程配置；
     * 2. 校验业务单据不存在运行中的重复流程；
     * 3. 按流程定义节点快照创建流程实例；
     * 4. 创建第一个待办任务；
     * 5. 记录流程发起流水和审计日志。</p>
     */
    @Transactional
    public WorkflowItemView startWorkflow(WorkflowStartRequest request) {
        WorkflowConfig config = configRepository
                .findByOrganizationCodeAndFunctionModuleCode(CompanyScope.currentCompanyCode(), request.functionModuleCode())
                .filter(WorkflowConfig::isEnabled)
                .orElseThrow(() -> new BusinessException(ResponseCode.REF_OBJ_NOT_EXISIT, "当前功能模块未配置启用流程"));
        WorkflowDefinition definition = definitionRepository.findByOrganizationCodeAndId(CompanyScope.currentCompanyCode(), config.getDefinitionId())
                .filter(WorkflowDefinition::isEnabled)
                .orElseThrow(() -> new BusinessException(ResponseCode.REF_OBJ_NOT_EXISIT, "流程定义不存在或已停用"));
        instanceRepository.findFirstByOrganizationCodeAndBusinessTypeAndBusinessIdAndStatusOrderByIdDesc(
                CompanyScope.currentCompanyCode(), request.businessType(), request.businessId(), WorkflowStatus.RUNNING
        ).ifPresent(existing -> {
            throw new BusinessException(ResponseCode.OBJ_BEEN_USED, "当前单据已有审批中流程");
        });
        List<WorkflowNodeView> nodes = parseNodes(definition.getNodesJson());
        if (nodes.isEmpty()) {
            throw new BusinessException(ResponseCode.ILLEGAL_PARAM, "流程定义未配置审批节点");
        }
        CurrentUser currentUser = SecurityUtils.currentUser();
        WorkflowInstance instance = new WorkflowInstance();
        instance.setOrganizationCode(CompanyScope.currentCompanyCode());
        instance.setDefinitionId(definition.getId());
        instance.setDefinitionName(definition.getName());
        instance.setBusinessModuleCode(config.getBusinessModuleCode());
        instance.setBusinessModuleName(config.getBusinessModuleName());
        instance.setFunctionModuleCode(config.getFunctionModuleCode());
        instance.setFunctionModuleName(config.getFunctionModuleName());
        instance.setBusinessType(request.businessType());
        instance.setBusinessId(request.businessId());
        instance.setBusinessNo(request.businessNo());
        instance.setProjectCode(request.projectCode());
        instance.setProjectName(request.projectName());
        instance.setTitle(request.title());
        instance.setApplyReason(request.applyReason());
        instance.setStarterId(currentUser.id());
        instance.setStarterUsername(currentUser.username());
        instance.setStarterName(currentUser.realName());
        instance.setStartedTime(OffsetDateTime.now());
        instance.setCurrentNodeOrder(nodes.get(0).nodeOrder());
        instance.setCurrentNodeName(nodes.get(0).nodeName());
        instance.setStatus(WorkflowStatus.RUNNING);
        instance.setNodesSnapshotJson(JSON.toJSONString(nodes));
        WorkflowInstance saved = instanceRepository.save(instance);
        createTask(saved, nodes.get(0));
        recordOperation(saved, WorkflowOperationType.START, null, null, currentUser, request.applyReason());
        auditLogService.record("START_WORKFLOW", request, "SUCCESS",
                "审批中心发起了" + saved.getTitle() + "。");
        return toInstanceItem(saved, pendingTask(saved.getId()), null);
    }

    /**
     * 审批当前任务。
     *
     * <p>实现步骤：
     * 1. 读取当前所属公司下的任务和流程实例；
     * 2. 校验任务待处理且当前用户属于审批人范围；
     * 3. 写入审批意见和处理人；
     * 4. 同意则推进下一节点，最后一个节点通过后结束流程；
     * 5. 不同意则直接驳回流程并回调业务模块。</p>
     */
    @Transactional
    public WorkflowItemView approveTask(Long taskId, WorkflowApproveRequest request) {
        WorkflowTask task = taskRepository.findByOrganizationCodeAndId(CompanyScope.currentCompanyCode(), taskId)
                .orElseThrow(() -> new BusinessException(ResponseCode.REF_OBJ_NOT_EXISIT, "审批任务不存在"));
        WorkflowInstance instance = task.getInstance();
        CompanyScope.requireCurrentCompany(instance.getOrganizationCode(), "流程实例");
        if (task.getStatus() != WorkflowTaskStatus.PENDING || instance.getStatus() != WorkflowStatus.RUNNING) {
            throw new BusinessException(ResponseCode.MODIFY_FORBIDDEN, "该任务已处理或流程已结束");
        }
        CurrentUser currentUser = SecurityUtils.currentUser();
        if (!canApprove(task, currentUser)) {
            throw new BusinessException(ResponseCode.NO_AUTH, "当前用户不是该节点审批人");
        }
        boolean approved = Boolean.TRUE.equals(request.approved());
        OffsetDateTime now = OffsetDateTime.now();
        task.setStatus(approved ? WorkflowTaskStatus.APPROVED : WorkflowTaskStatus.REJECTED);
        task.setActedById(currentUser.id());
        task.setActedByUsername(currentUser.username());
        task.setActedByName(currentUser.realName());
        task.setComment(request.comment());
        task.setActedAt(now);
        WorkflowOperationType operationType = approved ? WorkflowOperationType.APPROVE : WorkflowOperationType.REJECT;
        recordOperation(instance, operationType, task.getNodeOrder(), task.getNodeName(), currentUser, request.comment());
        if (approved) {
            moveToNextNodeOrComplete(instance, task, request.comment());
        } else {
            instance.setStatus(WorkflowStatus.REJECTED);
            instance.setCompletedTime(now);
            callbackBusiness(instance, WorkflowStatus.REJECTED, request.comment());
        }
        auditLogService.record(approved ? "APPROVE_WORKFLOW" : "REJECT_WORKFLOW", "taskId=" + taskId + ", " + request, "SUCCESS",
                "审批中心处理了" + instance.getTitle() + "，结果为" + (approved ? "同意" : "不同意") + "。");
        return toInstanceItem(instance, task, task);
    }

    /**
     * 查询当前用户待办事宜。
     *
     * <p>实现步骤：
     * 1. 按当前所属公司和待办状态查询任务；
     * 2. 叠加业务模块、审批标题、申请时间、发起人和流程状态筛选；
     * 3. 待办列表固定只展示运行中流程；
     * 4. 再按当前用户审批范围过滤，返回待当前用户审批的流程。</p>
     */
    @Transactional(readOnly = true)
    public List<WorkflowItemView> todoItems(
            String businessModuleCode,
            String title,
            String projectCode,
            LocalDate startedStart,
            LocalDate startedEnd,
            String starterName,
            WorkflowStatus status
    ) {
        if (status != null && status != WorkflowStatus.RUNNING) {
            return List.of();
        }
        CurrentUser currentUser = SecurityUtils.currentUser();
        Specification<WorkflowTask> spec = centerTaskSpec(
                WorkflowTaskStatus.PENDING,
                null,
                businessModuleCode,
                title,
                projectCode,
                startedStart,
                startedEnd,
                starterName,
                WorkflowStatus.RUNNING
        );
        return taskRepository.findAll(spec, Sort.by(Sort.Direction.DESC, "createdAt", "id")).stream()
                .filter(task -> canApprove(task, currentUser))
                .map(task -> toInstanceItem(task.getInstance(), task, null))
                .toList();
    }

    /**
     * 查询当前用户已办事宜。
     *
     * <p>实现步骤：读取当前所属公司下由当前用户实际处理过的任务，并按业务模块、标题、申请时间、发起人和状态过滤。</p>
     */
    @Transactional(readOnly = true)
    public List<WorkflowItemView> doneItems(
            String businessModuleCode,
            String title,
            String projectCode,
            LocalDate startedStart,
            LocalDate startedEnd,
            String starterName,
            WorkflowStatus status
    ) {
        CurrentUser currentUser = SecurityUtils.currentUser();
        Specification<WorkflowTask> spec = centerTaskSpec(
                null,
                currentUser.id(),
                businessModuleCode,
                title,
                projectCode,
                startedStart,
                startedEnd,
                starterName,
                status
        );
        return taskRepository.findAll(spec, Sort.by(Sort.Direction.DESC, "actedAt", "id")).stream()
                .map(task -> toInstanceItem(task.getInstance(), pendingTask(task.getInstance().getId()), task))
                .toList();
    }

    /**
     * 查询当前用户发起事宜。
     *
     * <p>实现步骤：按当前用户 ID 查询其发起的流程实例，并按业务模块、标题、申请时间、发起人和状态过滤。</p>
     */
    @Transactional(readOnly = true)
    public List<WorkflowItemView> startedItems(
            String businessModuleCode,
            String title,
            String projectCode,
            LocalDate startedStart,
            LocalDate startedEnd,
            String starterName,
            WorkflowStatus status
    ) {
        CurrentUser currentUser = SecurityUtils.currentUser();
        Specification<WorkflowInstance> spec = centerInstanceSpec(
                currentUser.id(),
                businessModuleCode,
                title,
                projectCode,
                startedStart,
                startedEnd,
                starterName,
                status
        );
        return instanceRepository.findAll(spec, Sort.by(Sort.Direction.DESC, "startedTime", "id")).stream()
                .map(instance -> toInstanceItem(instance, pendingTask(instance.getId()), null))
                .toList();
    }

    /**
     * 查看流程详情。
     *
     * <p>实现步骤：
     * 1. 按所属公司校验实例存在；
     * 2. 读取全部节点任务，供前端流程示意图标绿已走节点；
     * 3. 读取流程操作流水，供前端展示发起理由和每次审批意见；
     * 4. 通过业务回调读取单据表单预览，保证审批人能看到审批内容。</p>
     */
    @Transactional(readOnly = true)
    public WorkflowInstanceDetailView detail(Long instanceId) {
        WorkflowInstance instance = instanceRepository.findByOrganizationCodeAndId(CompanyScope.currentCompanyCode(), instanceId)
                .orElseThrow(() -> new BusinessException(ResponseCode.REF_OBJ_NOT_EXISIT, "流程实例不存在"));
        List<WorkflowTaskView> tasks = mergeSnapshotTasks(instance);
        List<WorkflowOperationLogView> logs = operationLogRepository.findByInstance_IdOrderByOperationTimeAscIdAsc(instanceId).stream()
                .map(this::toOperationLogView)
                .toList();
        return new WorkflowInstanceDetailView(toInstanceItem(instance, pendingTask(instanceId), null), tasks, logs, businessForm(instance));
    }

    /**
     * 查询指定业务单据最近流程详情。
     *
     * <p>实现步骤：按业务类型和业务 ID 定位最近流程实例，不存在时返回 null。</p>
     */
    @Transactional(readOnly = true)
    public WorkflowInstanceDetailView latestBusinessWorkflow(String businessType, Long businessId) {
        return instanceRepository.findFirstByOrganizationCodeAndBusinessTypeAndBusinessIdOrderByIdDesc(
                        CompanyScope.currentCompanyCode(), businessType, businessId)
                .map(instance -> detail(instance.getId()))
                .orElse(null);
    }

    /**
     * 取消指定业务单据正在运行的流程。
     *
     * <p>实现步骤：
     * 1. 按所属公司、业务类型和业务 ID 查找运行中的流程实例；
     * 2. 找到后把流程实例置为 CANCELLED，并把所有待办任务置为 SKIPPED；
     * 3. 记录一条流程取消流水，避免审批中心继续出现已取消业务的待办。</p>
     */
    @Transactional
    public void cancelRunningWorkflow(String businessType, Long businessId, String comment) {
        instanceRepository.findFirstByOrganizationCodeAndBusinessTypeAndBusinessIdAndStatusOrderByIdDesc(
                CompanyScope.currentCompanyCode(), businessType, businessId, WorkflowStatus.RUNNING
        ).ifPresent(instance -> {
            instance.setStatus(WorkflowStatus.CANCELLED);
            instance.setCompletedTime(OffsetDateTime.now());
            for (WorkflowTask task : taskRepository.findByInstance_IdAndStatusOrderByNodeOrderAscIdAsc(instance.getId(), WorkflowTaskStatus.PENDING)) {
                task.setStatus(WorkflowTaskStatus.SKIPPED);
            }
            recordOperation(instance, WorkflowOperationType.CANCEL, instance.getCurrentNodeOrder(), instance.getCurrentNodeName(),
                    SecurityUtils.currentUser(), comment);
        });
    }

    /**
     * 创建待办任务。
     *
     * <p>实现步骤：根据节点定义写入审批人范围、节点顺序和展示文本，任务初始状态为 PENDING。</p>
     */
    private WorkflowTask createTask(WorkflowInstance instance, WorkflowNodeView node) {
        WorkflowTask task = new WorkflowTask();
        task.setInstance(instance);
        task.setOrganizationCode(instance.getOrganizationCode());
        task.setNodeOrder(node.nodeOrder());
        task.setNodeName(node.nodeName());
        task.setApproverType(node.approverType());
        task.setApproverUserId(node.approverUserId());
        task.setApproverUsername(node.approverUsername());
        task.setApproverName(node.approverName());
        task.setApproverDepartment(node.approverDepartment());
        task.setApproverPosition(node.approverPosition());
        task.setApproverDisplay(approverDisplay(node.approverType(), node.approverUsername(), node.approverName(), node.approverDepartment(), node.approverPosition()));
        task.setStatus(WorkflowTaskStatus.PENDING);
        task.setCreatedAt(OffsetDateTime.now());
        return taskRepository.save(task);
    }

    /**
     * 构建审批中心任务查询条件。
     *
     * <p>实现步骤：
     * 1. 固定任务所属公司，防止跨账套查询；
     * 2. 按待办状态或已办审批人筛选任务；
     * 3. 通过任务关联的流程实例叠加审批中心通用搜索条件。</p>
     */
    private Specification<WorkflowTask> centerTaskSpec(
            WorkflowTaskStatus taskStatus,
            Long actedById,
            String businessModuleCode,
            String title,
            String projectCode,
            LocalDate startedStart,
            LocalDate startedEnd,
            String starterName,
            WorkflowStatus status
    ) {
        return (root, query, criteriaBuilder) -> {
            List<Predicate> predicates = new ArrayList<>();
            predicates.add(criteriaBuilder.equal(root.get("organizationCode"), CompanyScope.currentCompanyCode()));
            if (taskStatus != null) {
                predicates.add(criteriaBuilder.equal(root.get("status"), taskStatus));
            }
            if (actedById != null) {
                predicates.add(criteriaBuilder.equal(root.get("actedById"), actedById));
            }
            addWorkflowInstanceFilters(predicates, root.get("instance"), criteriaBuilder,
                    businessModuleCode, title, projectCode, startedStart, startedEnd, starterName, status);
            return criteriaBuilder.and(predicates.toArray(Predicate[]::new));
        };
    }

    /**
     * 构建审批中心流程实例查询条件。
     *
     * <p>实现步骤：固定当前所属公司和当前发起人，再复用流程实例通用筛选条件。</p>
     */
    private Specification<WorkflowInstance> centerInstanceSpec(
            Long starterId,
            String businessModuleCode,
            String title,
            String projectCode,
            LocalDate startedStart,
            LocalDate startedEnd,
            String starterName,
            WorkflowStatus status
    ) {
        return (root, query, criteriaBuilder) -> {
            List<Predicate> predicates = new ArrayList<>();
            predicates.add(criteriaBuilder.equal(root.get("organizationCode"), CompanyScope.currentCompanyCode()));
            predicates.add(criteriaBuilder.equal(root.get("starterId"), starterId));
            addWorkflowInstanceFilters(predicates, root, criteriaBuilder,
                    businessModuleCode, title, projectCode, startedStart, startedEnd, starterName, status);
            return criteriaBuilder.and(predicates.toArray(Predicate[]::new));
        };
    }

    /**
     * 添加审批中心通用流程实例搜索条件。
     *
     * <p>实现步骤：
     * 1. 业务模块按编码精确匹配；
     * 2. 审批标题和发起人姓名按包含关系模糊匹配；
     * 3. 申请时间范围按自然日闭区间换算为 OffsetDateTime；
     * 4. 流程状态按枚举精确匹配。</p>
     */
    private void addWorkflowInstanceFilters(
            List<Predicate> predicates,
            Path<WorkflowInstance> instance,
            jakarta.persistence.criteria.CriteriaBuilder criteriaBuilder,
            String businessModuleCode,
            String title,
            String projectCode,
            LocalDate startedStart,
            LocalDate startedEnd,
            String starterName,
            WorkflowStatus status
    ) {
        String moduleCode = blankToNull(businessModuleCode);
        if (moduleCode != null) {
            predicates.add(criteriaBuilder.equal(instance.get("businessModuleCode"), moduleCode));
        }
        String titleText = blankToNull(title);
        if (titleText != null) {
            predicates.add(criteriaBuilder.like(instance.get("title"), "%" + titleText + "%"));
        }
        String project = blankToNull(projectCode);
        if (project != null) {
            predicates.add(criteriaBuilder.equal(instance.get("projectCode"), project));
        }
        OffsetDateTime startedFrom = startOfDay(startedStart);
        if (startedFrom != null) {
            predicates.add(criteriaBuilder.greaterThanOrEqualTo(instance.get("startedTime"), startedFrom));
        }
        OffsetDateTime startedToExclusive = nextDayStart(startedEnd);
        if (startedToExclusive != null) {
            predicates.add(criteriaBuilder.lessThan(instance.get("startedTime"), startedToExclusive));
        }
        String starter = blankToNull(starterName);
        if (starter != null) {
            predicates.add(criteriaBuilder.like(instance.get("starterName"), "%" + starter + "%"));
        }
        if (status != null) {
            predicates.add(criteriaBuilder.equal(instance.get("status"), status));
        }
    }

    /**
     * 同意后推进到下一节点或完成流程。
     *
     * <p>实现步骤：
     * 1. 从实例节点快照中寻找当前节点后的下一节点；
     * 2. 存在下一节点时更新实例当前节点并创建新待办；
     * 3. 不存在下一节点时将流程置为 APPROVED 并回调业务模块。</p>
     */
    private void moveToNextNodeOrComplete(WorkflowInstance instance, WorkflowTask currentTask, String comment) {
        List<WorkflowNodeView> nodes = parseNodes(instance.getNodesSnapshotJson());
        WorkflowNodeView nextNode = nodes.stream()
                .filter(node -> node.nodeOrder() > currentTask.getNodeOrder())
                .min(Comparator.comparingInt(WorkflowNodeView::nodeOrder))
                .orElse(null);
        if (nextNode == null) {
            instance.setStatus(WorkflowStatus.APPROVED);
            instance.setCompletedTime(OffsetDateTime.now());
            callbackBusiness(instance, WorkflowStatus.APPROVED, comment);
            return;
        }
        instance.setCurrentNodeOrder(nextNode.nodeOrder());
        instance.setCurrentNodeName(nextNode.nodeName());
        createTask(instance, nextNode);
    }

    /**
     * 回调业务模块。
     *
     * <p>实现步骤：按业务类型查找回调实现；存在时传入流程实例、最终状态和审批意见；不存在时流程仍正常结束。</p>
     */
    private void callbackBusiness(WorkflowInstance instance, WorkflowStatus status, String comment) {
        WorkflowBusinessCallback callback = callbackMap.get(instance.getBusinessType());
        if (callback != null) {
            callback.onWorkflowCompleted(instance, status, comment);
        }
    }

    /**
     * 构建流程业务表单预览。
     *
     * <p>实现步骤：按流程实例的 businessType 定位业务回调；存在回调时委托业务模块读取自身单据并组装通用展示结构。</p>
     */
    private WorkflowBusinessFormView businessForm(WorkflowInstance instance) {
        WorkflowBusinessCallback callback = callbackMap.get(instance.getBusinessType());
        return callback == null ? null : callback.businessForm(instance);
    }

    /**
     * 判断当前用户是否可以审批任务。
     *
     * <p>实现步骤：
     * 1. USER 节点匹配用户 ID 或账号；
     * 2. DEPARTMENT 节点匹配当前用户部门；
     * 3. DEPARTMENT_POSITION 节点同时匹配部门和岗位。</p>
     */
    private boolean canApprove(WorkflowTask task, CurrentUser user) {
        if (task.getApproverType() == WorkflowApproverType.USER) {
            return Objects.equals(task.getApproverUserId(), user.id())
                    || Objects.equals(task.getApproverUsername(), user.username());
        }
        if (task.getApproverType() == WorkflowApproverType.DEPARTMENT) {
            return hasText(task.getApproverDepartment()) && Objects.equals(task.getApproverDepartment(), user.department());
        }
        if (task.getApproverType() == WorkflowApproverType.DEPARTMENT_POSITION) {
            return hasText(task.getApproverDepartment())
                    && Objects.equals(task.getApproverDepartment(), user.department())
                    && hasText(task.getApproverPosition())
                    && Objects.equals(task.getApproverPosition(), user.position());
        }
        return false;
    }

    /**
     * 记录流程操作流水。
     *
     * <p>实现步骤：写入操作类型、节点、操作人、意见和操作时间，流程查看按该表展示完整轨迹。</p>
     */
    private void recordOperation(
            WorkflowInstance instance,
            WorkflowOperationType operationType,
            Integer nodeOrder,
            String nodeName,
            CurrentUser currentUser,
            String comment
    ) {
        WorkflowOperationLog log = new WorkflowOperationLog();
        log.setInstance(instance);
        log.setOrganizationCode(instance.getOrganizationCode());
        log.setOperationType(operationType);
        log.setNodeOrder(nodeOrder);
        log.setNodeName(nodeName);
        log.setOperatorId(currentUser.id());
        log.setOperatorUsername(currentUser.username());
        log.setOperatorName(currentUser.realName());
        log.setComment(comment);
        log.setOperationTime(OffsetDateTime.now());
        operationLogRepository.save(log);
    }

    /**
     * 规范化流程节点。
     *
     * <p>实现步骤：按请求顺序赋予节点序号，校验不同审批人来源必须具备对应字段，并生成审批人展示文本。</p>
     */
    private List<WorkflowNodeView> normalizeNodes(List<WorkflowNodeRequest> requests) {
        return java.util.stream.IntStream.range(0, requests.size())
                .mapToObj(index -> normalizeNode(index + 1, requests.get(index)))
                .toList();
    }

    /**
     * 规范化单个节点。
     *
     * <p>实现步骤：根据审批人类型检查必填项，USER 可按用户 ID 反查人员姓名，最后组装稳定节点视图。</p>
     */
    private WorkflowNodeView normalizeNode(int nodeOrder, WorkflowNodeRequest request) {
        WorkflowApproverType type = request.approverType();
        Long userId = request.approverUserId();
        String username = request.approverUsername();
        String userName = request.approverName();
        if (type == WorkflowApproverType.USER) {
            if (userId == null && !hasText(username)) {
                throw new BusinessException(ResponseCode.ILLEGAL_PARAM, "指定人员审批节点必须选择审批人");
            }
            if (userId != null) {
                UserAccount user = userRepository.findById(userId)
                        .filter(item -> Objects.equals(item.getOrganizationCode(), CompanyScope.currentCompanyCode()))
                        .orElseThrow(() -> new BusinessException(ResponseCode.REF_OBJ_NOT_EXISIT, "审批人不存在"));
                username = user.getUsername();
                userName = user.getRealName();
            }
        }
        if (type == WorkflowApproverType.DEPARTMENT && !hasText(request.approverDepartment())) {
            throw new BusinessException(ResponseCode.ILLEGAL_PARAM, "部门审批节点必须选择部门");
        }
        if (type == WorkflowApproverType.DEPARTMENT_POSITION
                && (!hasText(request.approverDepartment()) || !hasText(request.approverPosition()))) {
            throw new BusinessException(ResponseCode.ILLEGAL_PARAM, "部门岗位审批节点必须选择部门和岗位");
        }
        return new WorkflowNodeView(
                nodeOrder,
                request.nodeName(),
                type,
                userId,
                username,
                userName,
                request.approverDepartment(),
                request.approverPosition(),
                approverDisplay(type, username, userName, request.approverDepartment(), request.approverPosition())
        );
    }

    /**
     * 组装审批人展示文本。
     *
     * <p>实现步骤：
     * 1. 指定人员审批统一展示“人员：姓名”；
     * 2. 部门审批统一展示“部门：部门名称”；
     * 3. 部门岗位审批统一展示“部门(岗位)：部门名称(岗位名称)”。</p>
     */
    private String approverDisplay(WorkflowApproverType type, String username, String userName, String department, String position) {
        return switch (type) {
            case USER -> "人员：" + defaultText(userName, username);
            case DEPARTMENT -> "部门：" + defaultText(department, "-");
            case DEPARTMENT_POSITION -> "部门(岗位)：" + defaultText(department, "-") + "(" + defaultText(position, "-") + ")";
        };
    }

    /**
     * 解析审批人范围命中的人员列表。
     *
     * <p>实现步骤：
     * 1. 指定人员审批按任务中的人员 ID 查询启用人员；
     * 2. 部门审批查询该部门下所有启用人员；
     * 3. 部门岗位审批查询该部门和岗位组合下所有启用人员；
     * 4. 转换为只包含姓名和联系方式的轻量视图，供列表悬浮层展示。</p>
     */
    private List<WorkflowApproverUserView> approverUsers(WorkflowTask task) {
        if (task == null) {
            return List.of();
        }
        String organizationCode = task.getOrganizationCode();
        List<UserAccount> users = switch (task.getApproverType()) {
            case USER -> approverUserByTask(organizationCode, task).stream().toList();
            case DEPARTMENT -> hasText(task.getApproverDepartment())
                    ? userRepository.findByOrganizationCodeAndDepartmentAndEnabledTrueOrderByRealNameAscIdAsc(organizationCode, task.getApproverDepartment())
                    : List.of();
            case DEPARTMENT_POSITION -> hasText(task.getApproverDepartment()) && hasText(task.getApproverPosition())
                    ? userRepository.findByOrganizationCodeAndDepartmentAndPositionAndEnabledTrueOrderByRealNameAscIdAsc(
                    organizationCode, task.getApproverDepartment(), task.getApproverPosition())
                    : List.of();
        };
        return users.stream()
                .map(user -> new WorkflowApproverUserView(defaultText(user.getRealName(), user.getUsername()), defaultText(user.getPhone(), "-")))
                .toList();
    }

    /**
     * 查询指定人员审批节点命中的人员。
     *
     * <p>实现步骤：优先按人员 ID 查找；人员 ID 为空或未命中时按登录账号兜底，兼容旧流程快照。</p>
     */
    private java.util.Optional<UserAccount> approverUserByTask(String organizationCode, WorkflowTask task) {
        if (task.getApproverUserId() != null) {
            java.util.Optional<UserAccount> user = userRepository.findByOrganizationCodeAndIdAndEnabledTrue(organizationCode, task.getApproverUserId());
            if (user.isPresent()) {
                return user;
            }
        }
        if (hasText(task.getApproverUsername())) {
            return userRepository.findByOrganizationCodeAndUsernameAndEnabledTrue(organizationCode, task.getApproverUsername());
        }
        return java.util.Optional.empty();
    }

    /**
     * 解析节点 JSON。
     *
     * <p>实现步骤：空 JSON 返回空集合；正常 JSON 转为节点视图列表并按节点顺序排序。</p>
     */
    private List<WorkflowNodeView> parseNodes(String nodesJson) {
        if (!hasText(nodesJson)) {
            return List.of();
        }
        return JSON.parseArray(nodesJson, WorkflowNodeView.class).stream()
                .sorted(Comparator.comparingInt(WorkflowNodeView::nodeOrder))
                .toList();
    }

    /**
     * 查找实例当前待办任务。
     *
     * <p>实现步骤：按实例 ID 和 PENDING 状态读取最靠前任务，用于列表展示下个审批人。</p>
     */
    private WorkflowTask pendingTask(Long instanceId) {
        return taskRepository.findByInstance_IdAndStatusOrderByNodeOrderAscIdAsc(instanceId, WorkflowTaskStatus.PENDING)
                .stream()
                .findFirst()
                .orElse(null);
    }

    /**
     * 合并流程快照节点和实际任务。
     *
     * <p>实现步骤：
     * 1. 先读取流程实例发起时保存的全部节点快照；
     * 2. 再读取已经生成的实际任务，按节点序号建立映射；
     * 3. 已生成任务按真实状态展示，尚未到达的节点补成 SKIPPED，前端可据此展示未到达节点。</p>
     */
    private List<WorkflowTaskView> mergeSnapshotTasks(WorkflowInstance instance) {
        Map<Integer, WorkflowTask> taskMap = new HashMap<>();
        taskRepository.findByInstance_IdOrderByNodeOrderAscIdAsc(instance.getId())
                .forEach(task -> taskMap.put(task.getNodeOrder(), task));
        return parseNodes(instance.getNodesSnapshotJson()).stream()
                .map(node -> {
                    WorkflowTask task = taskMap.get(node.nodeOrder());
                    if (task != null) {
                        return toTaskView(task);
                    }
                    return new WorkflowTaskView(
                            null,
                            node.nodeOrder(),
                            node.nodeName(),
                            approverDisplay(node.approverType(), node.approverUsername(), node.approverName(), node.approverDepartment(), node.approverPosition()),
                            WorkflowTaskStatus.SKIPPED,
                            null,
                            null,
                            null,
                            null,
                            null
                    );
                })
                .toList();
    }

    /**
     * 转换流程定义视图。
     *
     * <p>实现步骤：解析节点 JSON 并保留模板基础信息。</p>
     */
    private WorkflowDefinitionView toDefinitionView(WorkflowDefinition definition) {
        return new WorkflowDefinitionView(
                definition.getId(),
                definition.getOrganizationCode(),
                definition.getName(),
                definition.getCode(),
                definition.getDescription(),
                parseNodes(definition.getNodesJson()),
                definition.isEnabled()
        );
    }

    /**
     * 转换流程配置视图。
     *
     * <p>实现步骤：返回业务模块、功能模块和流程定义绑定关系。</p>
     */
    private WorkflowConfigView toConfigView(WorkflowConfig config) {
        return new WorkflowConfigView(
                config.getId(),
                config.getOrganizationCode(),
                config.getBusinessModuleCode(),
                config.getBusinessModuleName(),
                config.getFunctionModuleCode(),
                config.getFunctionModuleName(),
                config.getDefinitionId(),
                config.getDefinitionName(),
                config.isEnabled()
        );
    }

    /**
     * 转换审批中心列表项。
     *
     * <p>实现步骤：从流程实例读取业务定位和发起信息；从当前待办任务读取下个节点审批人；从已办任务读取审批状态。</p>
     */
    private WorkflowItemView toInstanceItem(WorkflowInstance instance, WorkflowTask pendingTask, WorkflowTask actedTask) {
        return new WorkflowItemView(
                instance.getId(),
                pendingTask == null ? null : pendingTask.getId(),
                instance.getBusinessModuleCode(),
                instance.getBusinessModuleName(),
                instance.getFunctionModuleCode(),
                instance.getFunctionModuleName(),
                instance.getBusinessType(),
                instance.getBusinessId(),
                instance.getBusinessNo(),
                instance.getProjectCode(),
                instance.getProjectName(),
                instance.getTitle(),
                instance.getStarterId(),
                instance.getStarterUsername(),
                instance.getStarterName(),
                timeText(instance.getStartedTime()),
                instance.getCurrentNodeName(),
                instance.getStatus(),
                pendingTask == null ? "" : approverDisplay(pendingTask.getApproverType(), pendingTask.getApproverUsername(), pendingTask.getApproverName(), pendingTask.getApproverDepartment(), pendingTask.getApproverPosition()),
                approverUsers(pendingTask),
                actedTask == null ? null : actedTask.getStatus(),
                actedTask == null ? null : timeText(actedTask.getActedAt())
        );
    }

    /**
     * 转换流程任务节点视图。
     *
     * <p>实现步骤：保留节点审批状态、处理人和审批意见，前端流程图据此标识绿色已处理节点。</p>
     */
    private WorkflowTaskView toTaskView(WorkflowTask task) {
        return new WorkflowTaskView(
                task.getId(),
                task.getNodeOrder(),
                task.getNodeName(),
                approverDisplay(task.getApproverType(), task.getApproverUsername(), task.getApproverName(), task.getApproverDepartment(), task.getApproverPosition()),
                task.getStatus(),
                task.getActedById(),
                task.getActedByUsername(),
                task.getActedByName(),
                task.getComment(),
                timeText(task.getActedAt())
        );
    }

    /**
     * 转换流程操作流水视图。
     *
     * <p>实现步骤：按前端需要返回操作类型、节点、操作人、联系方式、意见和时间。</p>
     */
    private WorkflowOperationLogView toOperationLogView(WorkflowOperationLog log) {
        return new WorkflowOperationLogView(
                log.getId(),
                log.getOperationType(),
                log.getNodeOrder(),
                log.getNodeName(),
                log.getOperatorId(),
                log.getOperatorUsername(),
                log.getOperatorName(),
                operatorPhone(log),
                log.getComment(),
                timeText(log.getOperationTime())
        );
    }

    /**
     * 查询流程操作人的联系电话。
     *
     * <p>实现步骤：
     * 1. 优先按操作人 ID 读取人员档案；
     * 2. 校验人员所属公司与流水所属公司一致，避免跨账套展示联系方式；
     * 3. 返回人员联系电话，未找到时返回空值。</p>
     */
    private String operatorPhone(WorkflowOperationLog log) {
        if (log.getOperatorId() == null) {
            return null;
        }
        return userRepository.findById(log.getOperatorId())
                .filter(user -> Objects.equals(user.getOrganizationCode(), log.getOrganizationCode()))
                .map(UserAccount::getPhone)
                .orElse(null);
    }

    /** 空白文本转 null。 */
    private String blankToNull(String value) {
        return hasText(value) ? value.trim() : null;
    }

    /** 空白时返回兜底文本。 */
    private String defaultText(String value, String fallback) {
        return hasText(value) ? value : fallback;
    }

    /**
     * 将自然日转换为当天开始时间。
     *
     * <p>实现步骤：使用系统默认时区构造当天零点，再转换为 OffsetDateTime 参与数据库时间筛选。</p>
     */
    private OffsetDateTime startOfDay(LocalDate date) {
        return date == null ? null : date.atStartOfDay(ZoneId.systemDefault()).toOffsetDateTime();
    }

    /**
     * 将自然日转换为下一天开始时间。
     *
     * <p>实现步骤：结束日期使用小于下一天零点的写法，覆盖结束日当天所有审批申请时间。</p>
     */
    private OffsetDateTime nextDayStart(LocalDate date) {
        return date == null ? null : date.plusDays(1).atStartOfDay(ZoneId.systemDefault()).toOffsetDateTime();
    }

    /** 判断文本是否包含非空白字符。 */
    private boolean hasText(String value) {
        return value != null && !value.isBlank();
    }

    /** 时间统一转 ISO 字符串，前端再按本地格式展示。 */
    private String timeText(OffsetDateTime time) {
        return time == null ? null : time.toString();
    }
}
