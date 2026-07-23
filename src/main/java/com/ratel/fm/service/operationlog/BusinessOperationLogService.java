package com.ratel.fm.service.operationlog;

import com.alibaba.fastjson2.JSON;
import com.ratel.fm.domain.operation.BusinessOperationLog;
import com.ratel.fm.repository.operation.BusinessOperationLogRepository;
import com.ratel.fm.security.CompanyScope;
import com.ratel.fm.security.CurrentUser;
import com.ratel.fm.security.SecurityUtils;
import com.ratel.fm.web.dto.operationlog.BusinessOperationLogDtos.BusinessOperationLogPage;
import com.ratel.fm.web.dto.operationlog.BusinessOperationLogDtos.BusinessOperationLogView;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.List;

/**
 * 统一业务操作流水服务。
 *
 * <p>负责记录和查询业务记录自己的时间轴，供凭证、采购、库存、应收应付等模块复用。</p>
 *
 * <p>开发组织：ratel；开发人员：WenZhang；联系方式：18782945613。</p>
 *
 * @author ratel
 */
@Service
public class BusinessOperationLogService {

    /** 快照 JSON 最大保存长度，覆盖多明细采购单和新增业务字段，避免查看流水解析失败。 */
    private static final int SNAPSHOT_MAX_LENGTH = 10000;

    /**
     * 字段 repository：保存 repository 对应的业务数据、运行配置或依赖对象，供本类逻辑读取和维护。
     */
    private final BusinessOperationLogRepository repository;

    /**
     * 构造 BusinessOperationLogService 实例。
     * 
     * <p>实现步骤：
     * 1. 接收调用方传入的依赖对象或初始化参数；
     * 2. 保存到成员字段，保证后续业务方法可以复用；
     * 3. 完成实例初始化。</p>
     */
    public BusinessOperationLogService(BusinessOperationLogRepository repository) {
        this.repository = repository;
    }

    /**
     * 记录业务操作流水。
     *
     * <p>实现步骤：
     * 1. 获取当前登录人；
     * 2. 保存业务类型、业务 ID、编号、动作、前后状态和快照；
     * 3. 如果流水落库失败，直接吞掉异常，避免影响主业务流程。</p>
     */
    @Transactional
    public void record(
            String businessType,
            Long businessId,
            String businessNo,
            String businessTitle,
            String action,
            String actionName,
            String detail,
            String fromState,
            String toState,
            Object snapshot
    ) {
        try {
            // 变量说明：currentUser 保存当前步骤计算、查询或转换得到的中间结果。
            CurrentUser currentUser = SecurityUtils.currentUser();
            // 变量说明：log 保存当前步骤计算、查询或转换得到的中间结果。
            BusinessOperationLog log = new BusinessOperationLog();
            log.setOrganizationCode(CompanyScope.currentCompanyCode());
            log.setBusinessType(businessType);
            log.setBusinessId(businessId);
            log.setBusinessNo(truncate(defaultText(businessNo, "-"), 120));
            log.setBusinessTitle(truncate(defaultText(businessTitle, businessNo), 300));
            log.setAction(action);
            log.setActionName(actionName);
            log.setDetail(truncate(detail, 1000));
            log.setFromState(truncate(fromState, 120));
            log.setToState(truncate(toState, 120));
            // 步骤2.1：将业务视图转成 JSON 快照，前端查看流水优先解析该快照展示表单字段。
            log.setSnapshot(truncate(snapshot == null ? null : JSON.toJSONString(snapshot), SNAPSHOT_MAX_LENGTH));
            log.setOperatorId(currentUser.id());
            log.setOperatorUsername(currentUser.username());
            log.setOperatorName(currentUser.realName());
            log.setOperationTime(OffsetDateTime.now());
            repository.save(log);
        } catch (Exception ex) {
            // 步骤3：业务流水用于辅助追溯，记录失败不能影响凭证、采购、库存等主流程。
        }
    }

    /**
     * 查询业务操作流水。
     *
     * <p>实现步骤：按业务类型和业务 ID 查询，并按操作时间正序返回，前端以时间轴展示。</p>
     */
    @Transactional(readOnly = true)
    public List<BusinessOperationLogView> list(String businessType, Long businessId) {
        return repository.findByOrganizationCodeAndBusinessTypeAndBusinessIdOrderByOperationTimeAscIdAsc(
                        CompanyScope.currentCompanyCode(), businessType, businessId).stream()
                .map(this::toView)
                .toList();
    }

    /**
     * 分页查询业务操作流水。
     *
     * <p>实现步骤：
     * 1. 固定按业务类型和业务 ID 定位单据自己的流水；
     * 2. 操作时间按前端传入的半月默认范围或用户筛选范围过滤；
     * 3. 按操作时间倒序分页返回，前端滚动到底部时继续加载下一页。</p>
     */
    @Transactional(readOnly = true)
    public BusinessOperationLogPage page(String businessType, Long businessId, OffsetDateTime startTime, OffsetDateTime endTime, int page, int size) {
        Specification<BusinessOperationLog> spec = (root, query, criteriaBuilder) -> criteriaBuilder.and(
                criteriaBuilder.equal(root.get("organizationCode"), CompanyScope.currentCompanyCode()),
                criteriaBuilder.equal(root.get("businessType"), businessType),
                criteriaBuilder.equal(root.get("businessId"), businessId)
        );
        if (startTime != null) {
            spec = spec.and((root, query, criteriaBuilder) -> criteriaBuilder.greaterThanOrEqualTo(root.get("operationTime"), startTime));
        }
        if (endTime != null) {
            spec = spec.and((root, query, criteriaBuilder) -> criteriaBuilder.lessThanOrEqualTo(root.get("operationTime"), endTime));
        }
        /**
         * 业务操作流水分页结果，限定当前账套、业务类型、业务 ID 和时间范围。
         */
        var result = repository.findAll(
                spec,
                PageRequest.of(safePage(page), safeSize(size), Sort.by(Sort.Direction.DESC, "operationTime", "id"))
        );
        return new BusinessOperationLogPage(result.getContent().stream().map(this::toView).toList(), result.getTotalElements());
    }

    /**
     * 删除业务记录关联流水。
     */
    @Transactional
    public void deleteAll(String businessType, Long businessId) {
        repository.deleteByBusinessTypeAndBusinessId(businessType, businessId);
    }

    /**
     * 执行 toView 方法。
     * 
     * <p>实现步骤：
     * 1. 接收并校验调用方传入的数据；
     * 2. 按当前方法职责执行业务查询、转换或持久化处理；
     * 3. 返回处理结果或更新对象状态。</p>
     */
    private BusinessOperationLogView toView(BusinessOperationLog log) {
        return new BusinessOperationLogView(
                log.getId(),
                log.getBusinessType(),
                log.getBusinessId(),
                log.getBusinessNo(),
                log.getBusinessTitle(),
                log.getAction(),
                log.getActionName(),
                log.getDetail(),
                log.getFromState(),
                log.getToState(),
                log.getSnapshot(),
                log.getOperatorId(),
                log.getOperatorUsername(),
                log.getOperatorName(),
                log.getOperationTime() == null ? null : log.getOperationTime().toString()
        );
    }

    /**
     * 执行 defaultText 方法。
     * 
     * <p>实现步骤：
     * 1. 接收并校验调用方传入的数据；
     * 2. 按当前方法职责执行业务查询、转换或持久化处理；
     * 3. 返回处理结果或更新对象状态。</p>
     */
    private String defaultText(String value, String fallback) {
        return value == null || value.isBlank() ? fallback : value;
    }

    /**
     * 执行 truncate 方法。
     * 
     * <p>实现步骤：
     * 1. 接收并校验调用方传入的数据；
     * 2. 按当前方法职责执行业务查询、转换或持久化处理；
     * 3. 返回处理结果或更新对象状态。</p>
     */
    private String truncate(String value, int maxLength) {
        if (value == null || value.length() <= maxLength) {
            return value;
        }
        return value.substring(0, maxLength);
    }

    /**
     * 执行 safePage 方法。
     * 
     * <p>实现步骤：
     * 1. 接收并校验调用方传入的数据；
     * 2. 按当前方法职责执行业务查询、转换或持久化处理；
     * 3. 返回处理结果或更新对象状态。</p>
     */
    private int safePage(int page) {
        return Math.max(page, 0);
    }

    /**
     * 执行 safeSize 方法。
     * 
     * <p>实现步骤：
     * 1. 接收并校验调用方传入的数据；
     * 2. 按当前方法职责执行业务查询、转换或持久化处理；
     * 3. 返回处理结果或更新对象状态。</p>
     */
    private int safeSize(int size) {
        if (size <= 0) {
            return 20;
        }
        return Math.min(size, 100);
    }
}
