package com.ratel.fm.service.audit;

import com.ratel.fm.security.CompanyScope;
import com.ratel.fm.security.CurrentUser;
import com.ratel.fm.security.SecurityUtils;
import com.ratel.fm.domain.audit.UserOperationLog;
import com.ratel.fm.domain.auth.UserAccount;
import com.ratel.fm.repository.audit.UserOperationLogRepository;
import com.ratel.fm.web.dto.audit.AuditDtos.OperationLogPage;
import com.ratel.fm.web.dto.audit.AuditDtos.OperationLogView;
import jakarta.persistence.criteria.Predicate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * 关键业务操作审计日志服务。
 *
 * <p>开发组织：ratel；开发人员：WenZhang；联系方式：18782945613。</p>
 *
 * @author ratel
 */
@Service
public class AuditLogService {

    /**
     * 业务审计专用日志器，用于输出独立可检索的操作审计日志行。
     */
    private static final Logger AUDIT_LOG = LoggerFactory.getLogger("com.ratel.fm.audit.operation");
    /**
     * 服务内部日志器，用于记录审计落库失败等技术异常。
     */
    private static final Logger LOGGER = LoggerFactory.getLogger(AuditLogService.class);

    /**
     * 字段 repository：保存 repository 对应的业务数据、运行配置或依赖对象，供本类逻辑读取和维护。
     */
    private final UserOperationLogRepository repository;
    /**
     * 字段 transactionTemplate：保存 transactionTemplate 对应的业务数据、运行配置或依赖对象，供本类逻辑读取和维护。
     */
    private final TransactionTemplate transactionTemplate;

    /**
     * 构造 AuditLogService 实例。
     * 
     * <p>实现步骤：
     * 1. 接收调用方传入的依赖对象或初始化参数；
     * 2. 保存到成员字段，保证后续业务方法可以复用；
     * 3. 完成实例初始化。</p>
     */
    public AuditLogService(UserOperationLogRepository repository, PlatformTransactionManager transactionManager) {
        this.repository = repository;
        this.transactionTemplate = new TransactionTemplate(transactionManager);
        this.transactionTemplate.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
    }

    /**
     * 记录财务类关键操作日志。
     *
     * <p>保留该方法是为了让财务模块调用语义更清晰，实际落库逻辑统一委托给 record 方法。</p>
     */
    public void finance(String action, Object parameters, String result, String impact) {
        record(action, parameters, result, impact);
    }

    /**
     * 记录用户关键操作日志。
     *
     * <p>实现步骤：
     * 1. 从 Spring Security 上下文读取当前登录人并转换为审计操作者；
     * 2. 根据 action 映射操作模块和操作功能；
     * 3. 先写系统文件日志，确保数据库不可用时仍能追踪关键操作；
     * 4. 使用独立事务写入 fm_user_operation_logs；
     * 5. 捕获数据库日志异常，仅写 warning 文件日志，不影响业务主流程。</p>
     */
    public void record(String action, Object parameters, String result, String impact) {
        record(action, parameters, result, impact, impact);
    }

    /**
     * 记录用户关键操作日志，并允许调用方单独指定响应值。
     *
     * <p>该方法用于后续需要把接口返回对象或异常摘要写入审计日志的场景。</p>
     */
    public void record(String action, Object parameters, String result, String responseValue, String impact) {
        recordInternal(AuditOperator.fromCurrentUser(SecurityUtils.currentUser()), action, parameters, result, responseValue, impact);
    }

    /**
     * 记录登录类关键操作日志。
     *
     * <p>登录成功或失败时 SecurityContext 还没有当前登录人，因此由认证服务显式传入人员和终端信息。
     * 该方法同样先写文件日志、再用独立事务写数据库日志，任何日志异常都不会影响登录主流程。</p>
     */
    public void recordLogin(
            UserAccount user,
            String attemptedAccount,
            String terminalType,
            String terminalIdentifier,
            String action,
            Object parameters,
            String result,
            String responseValue,
            String impact
    ) {
        recordInternal(
                AuditOperator.fromLogin(user, attemptedAccount, terminalType, terminalIdentifier),
                action,
                parameters,
                result,
                responseValue,
                impact
        );
    }

    /**
     * 执行审计日志写入的统一入口。
     *
     * <p>实现步骤：
     * 1. 根据 action 映射业务模块和功能；
     * 2. 截断参数、响应和影响文本，避免日志字段超长；
     * 3. 先写审计文件日志；
     * 4. 使用独立事务写数据库日志，捕获异常后仅写 warning。</p>
     */
    private void recordInternal(AuditOperator operator, String action, Object parameters, String result, String responseValue, String impact) {
        // 步骤2：操作模块和功能由统一映射维护，避免列表页面只能看到技术 action 编码。
        AuditActionMeta meta = resolveActionMeta(action);
        // 变量说明：operationTime 保存当前步骤计算、查询或转换得到的中间结果。
        OffsetDateTime operationTime = OffsetDateTime.now();
        // 变量说明：parameterText 保存当前步骤计算、查询或转换得到的中间结果。
        String parameterText = limit(String.valueOf(parameters), 2000);
        // 变量说明：resultText 保存当前步骤计算、查询或转换得到的中间结果。
        String resultText = displayResult(result);
        // 变量说明：responseText 保存当前步骤计算、查询或转换得到的中间结果。
        String responseText = limit(responseValue, 2000);
        // 变量说明：impactText 保存当前步骤计算、查询或转换得到的中间结果。
        String impactText = limit(impact, 1000);

        // 步骤3：先写系统文件日志，数据库日志失败时仍能从文件日志追踪操作者和操作内容。
        AUDIT_LOG.info(
                "operatorId={} username={} identityNo={} organizationCode={} phone={} department={} terminalType={} terminalIdentifier={} module={} function={} action={} success={} result={} parameters={} response={} impact={}",
                operator.id(), operator.username(), operator.identityNo(), operator.organizationCode(), operator.contactPhone(), operator.department(),
                operator.terminalType(), operator.terminalIdentifier(), meta.module(), meta.function(), action,
                isSuccess(result), resultText, parameterText, responseText, impactText
        );

        // 步骤4-5：数据库日志用独立事务写入并吞掉异常，避免审计表故障反向影响主业务提交。
        try {
            transactionTemplate.executeWithoutResult(status -> repository.save(buildLog(
                    operator, meta, operationTime, action, parameterText, resultText, responseText, impactText
            )));
        } catch (Exception ex) {
            LOGGER.warn("数据库操作日志写入失败，业务主流程不受影响。action={}, operatorUsername={}, message={}",
                    action, operator.username(), ex.getMessage(), ex);
        }
    }

    /**
     * 查询数据库操作日志。
     *
     * <p>实现步骤：
     * 1. 根据操作时间、账号、身份证、联系方式、部门、终端类型和终端标识构建动态条件；
     * 2. 按操作时间倒序分页查询；
     * 3. 将实体映射为前端列表需要的展示对象。</p>
     */
    @Transactional(readOnly = true)
    public OperationLogPage searchLogs(
            OffsetDateTime startTime,
            OffsetDateTime endTime,
            String account,
            String identityNo,
            String contactPhone,
            String department,
            String terminalType,
            String terminalIdentifier,
            int page,
            int size
    ) {
        // 变量说明：safePage 保存当前步骤计算、查询或转换得到的中间结果。
        int safePage = Math.max(page, 0);
        // 变量说明：safeSize 保存当前步骤计算、查询或转换得到的中间结果。
        int safeSize = Math.min(Math.max(size, 1), 200);
        // 变量说明：effectiveEndTime 保存当前步骤计算、查询或转换得到的中间结果。
        OffsetDateTime effectiveEndTime = endTime;
        // 变量说明：effectiveStartTime 保存当前步骤计算、查询或转换得到的中间结果。
        OffsetDateTime effectiveStartTime = startTime;
        if (effectiveStartTime == null && effectiveEndTime == null) {
            effectiveEndTime = OffsetDateTime.now();
            effectiveStartTime = effectiveEndTime.minusMonths(1);
        } else if (effectiveStartTime == null) {
            effectiveStartTime = effectiveEndTime.minusMonths(1);
        } else if (effectiveEndTime == null) {
            effectiveEndTime = effectiveStartTime.plusMonths(1);
        }

        Page<UserOperationLog> resultPage = repository.findAll(
                buildSpecification(effectiveStartTime, effectiveEndTime, account, identityNo, contactPhone, department, terminalType, terminalIdentifier),
                PageRequest.of(safePage, safeSize, Sort.by(Sort.Direction.DESC, "operationTime", "id"))
        );
        return new OperationLogPage(resultPage.getContent().stream().map(this::toView).toList(), resultPage.getTotalElements());
    }

    /**
     * 构建数据库日志实体。
     */
    private UserOperationLog buildLog(
            AuditOperator operator,
            AuditActionMeta meta,
            OffsetDateTime operationTime,
            String action,
            String parameterText,
            String result,
            String responseText,
            String impactText
    ) {
        // 变量说明：log 保存当前步骤计算、查询或转换得到的中间结果。
        UserOperationLog log = new UserOperationLog();
        log.setOperatorId(operator.id());
        log.setOperatorUsername(limit(operator.username(), 80));
        log.setOperatorName(limit(operator.realName(), 120));
        log.setIdentityNo(limit(operator.identityNo(), 40));
        log.setOrganizationCode(limit(operator.organizationCode(), 80));
        log.setDepartment(limit(operator.department(), 80));
        log.setContactPhone(limit(operator.contactPhone(), 40));
        log.setTerminalType(limit(operator.terminalType(), 20));
        log.setTerminalIdentifier(limit(operator.terminalIdentifier(), 120));
        log.setOperationModule(limit(meta.module(), 80));
        log.setOperationFunction(limit(meta.function(), 120));
        log.setOperationTime(operationTime);
        log.setAction(limit(action, 120));
        log.setOperationParameters(parameterText);
        log.setSuccess(isSuccess(result));
        log.setOperationResult(limit(result, 80));
        log.setResponseValue(responseText);
        log.setImpact(impactText);
        return log;
    }

    /**
     * 根据查询条件构建 JPA Specification。
     */
    private Specification<UserOperationLog> buildSpecification(
            OffsetDateTime startTime,
            OffsetDateTime endTime,
            String account,
            String identityNo,
            String contactPhone,
            String department,
            String terminalType,
            String terminalIdentifier
    ) {
        return (root, query, builder) -> {
            // 变量说明：predicates 保存当前步骤计算、查询或转换得到的中间结果。
            List<Predicate> predicates = new ArrayList<>();
            predicates.add(builder.equal(root.get("organizationCode"), CompanyScope.currentCompanyCode()));
            if (startTime != null) {
                predicates.add(builder.greaterThanOrEqualTo(root.get("operationTime"), startTime));
            }
            if (endTime != null) {
                predicates.add(builder.lessThanOrEqualTo(root.get("operationTime"), endTime));
            }
            addLike(predicates, builder, root.get("operatorUsername"), account);
            addLike(predicates, builder, root.get("identityNo"), identityNo);
            addLike(predicates, builder, root.get("contactPhone"), contactPhone);
            addLike(predicates, builder, root.get("department"), department);
            addLike(predicates, builder, root.get("terminalIdentifier"), terminalIdentifier);
            if (terminalType != null && !terminalType.isBlank()) {
                predicates.add(builder.equal(root.get("terminalType"), terminalType.trim().toUpperCase(Locale.ROOT)));
            }
            return builder.and(predicates.toArray(Predicate[]::new));
        };
    }

    /**
     * 添加模糊查询条件。
     */
    private void addLike(List<Predicate> predicates, jakarta.persistence.criteria.CriteriaBuilder builder,
                         jakarta.persistence.criteria.Expression<String> field, String value) {
        if (value != null && !value.isBlank()) {
            predicates.add(builder.like(builder.lower(field), "%" + value.trim().toLowerCase(Locale.ROOT) + "%"));
        }
    }

    /**
     * 将数据库日志实体转换为列表展示对象。
     */
    private OperationLogView toView(UserOperationLog log) {
        return new OperationLogView(
                log.getId(),
                log.getOperatorUsername(),
                log.getOperatorName(),
                log.getIdentityNo(),
                log.getContactPhone(),
                log.getDepartment(),
                log.getOperationTime(),
                log.getTerminalType(),
                log.getTerminalIdentifier(),
                log.getOperationModule(),
                log.getOperationFunction(),
                log.getAction(),
                log.getOperationParameters(),
                log.getSuccess(),
                log.getOperationResult(),
                log.getResponseValue(),
                log.getImpact()
        );
    }

    /**
     * 根据操作动作解析业务模块和功能。
     */
    private AuditActionMeta resolveActionMeta(String action) {
        return switch (action == null ? "" : action) {
            case "LOGIN_SUCCESS", "LOGIN_FAILED", "LOGIN_REPEAT", "LOGIN_FORCE" ->
                    new AuditActionMeta("认证管理", "人员登录");
            case "CREATE_USER", "UPDATE_USER", "DELETE_USER", "BATCH_DELETE_USERS", "CHANGE_USER_PASSWORD", "UPDATE_USER_AVATAR" ->
                    new AuditActionMeta("基础信息", "人员信息管理");
            case "SAVE_ROLE", "DELETE_ROLE" -> new AuditActionMeta("基础信息", "角色管理");
            case "SAVE_MENU", "DELETE_MENU" -> new AuditActionMeta("基础信息", "菜单管理");
            case "CREATE_BASIC_DICTIONARY", "UPDATE_BASIC_DICTIONARY", "DELETE_BASIC_DICTIONARY" ->
                    new AuditActionMeta("基础信息", "字典管理");
            case "UPDATE_PROFILE", "CHANGE_MY_PASSWORD", "UPDATE_MY_AVATAR" -> new AuditActionMeta("个人中心", "个人资料维护");
            case "CREATE_SUBJECT", "UPDATE_SUBJECT", "DELETE_SUBJECT", "BATCH_DELETE_SUBJECTS" -> new AuditActionMeta("财务管理", "会计科目");
            case "CREATE_VOUCHER", "UPDATE_VOUCHER", "POST_VOUCHER", "VOID_VOUCHER", "BATCH_DELETE_VOUCHERS" -> new AuditActionMeta("财务管理", "凭证记账");
            case "CREATE_PURCHASE_ORDER", "UPDATE_PURCHASE_ORDER", "CHANGE_PURCHASE_STATUS", "BATCH_DELETE_PURCHASE_ORDERS" -> new AuditActionMeta("业务管理", "采购管理");
            case "CREATE_SHIPMENT", "UPDATE_SHIPMENT", "CHANGE_SHIPMENT_STATUS", "BATCH_DELETE_SHIPMENTS" -> new AuditActionMeta("业务管理", "物流管理");
            case "CREATE_INVENTORY_LEDGER", "BATCH_DELETE_INVENTORY_LEDGERS" -> new AuditActionMeta("库存管理", "库存台账");
            case "CREATE_AR_AP_BILL", "BATCH_DELETE_AR_AP_BILLS" -> new AuditActionMeta("应收应付", "应收应付单据");
            case "UPLOAD_ATTACHMENTS", "RENAME_ATTACHMENT", "DELETE_ATTACHMENT", "DELETE_BUSINESS_ATTACHMENTS" ->
                    new AuditActionMeta("附件管理", "业务证据附件");
            default -> new AuditActionMeta("业务操作", "关键操作");
        };
    }

    /**
     * 判断操作结果是否成功。
     */
    private boolean isSuccess(String result) {
        return result != null && List.of("SUCCESS", "OK", "200", "操作成功").contains(result.trim().toUpperCase(Locale.ROOT));
    }

    /**
     * 将技术结果码转换为日志列表可读文本。
     *
     * <p>实现步骤：常见成功码统一展示为“操作成功”；常见失败码统一展示为“操作失败”；其他业务提示保持原文。</p>
     */
    private String displayResult(String result) {
        if (result == null || result.isBlank()) {
            return "未知结果";
        }
        // 变量说明：normalized 保存当前步骤计算、查询或转换得到的中间结果。
        String normalized = result.trim().toUpperCase(Locale.ROOT);
        if (List.of("SUCCESS", "OK", "200", "操作成功").contains(normalized)) {
            return "操作成功";
        }
        if (List.of("FAILED", "FAIL", "ERROR", "999999", "操作失败").contains(normalized)) {
            return "操作失败";
        }
        return result;
    }

    /**
     * 按字段长度截断文本。
     *
     * <p>日志记录不能因为参数过长导致主业务失败，因此在服务层主动截断。</p>
     */
    private String limit(String text, int maxLength) {
        if (text == null || text.length() <= maxLength) {
            return text;
        }
        return text.substring(0, maxLength);
    }

    /**
     * 操作动作对应的业务模块和业务功能。
     */
    private record AuditActionMeta(String module, String function) {
    }

    /**
     * 审计日志中的操作者快照。
     *
     * <p>登录接口在签发 JWT 前没有 SecurityContext，因此这里同时支持从当前登录人和人员实体构造审计操作者。</p>
     */
    private record AuditOperator(
            /**
             * 记录组件 id：表示接口入参或出参中的 id 字段。
             */
            Long id,
            /**
             * 记录组件 username：表示接口入参或出参中的 username 字段。
             */
            String username,
            /**
             * 记录组件 realName：表示接口入参或出参中的 realName 字段。
             */
            String realName,
            /**
             * 记录组件 identityNo：表示接口入参或出参中的 identityNo 字段。
             */
            String identityNo,
            /**
             * 记录组件 organizationCode：表示操作发生时登录人所属公司/账套编码。
             */
            String organizationCode,
            /**
             * 记录组件 department：表示接口入参或出参中的 department 字段。
             */
            String department,
            /**
             * 记录组件 contactPhone：表示接口入参或出参中的 contactPhone 字段。
             */
            String contactPhone,
            /**
             * 记录组件 terminalType：表示接口入参或出参中的 terminalType 字段。
             */
            String terminalType,
            /**
             * 记录组件 terminalIdentifier：表示接口入参或出参中的 terminalIdentifier 字段。
             */
            String terminalIdentifier
    ) {

        /**
         * 执行 fromCurrentUser 方法。
         * 
         * <p>实现步骤：
         * 1. 接收并校验调用方传入的数据；
         * 2. 按当前方法职责执行业务查询、转换或持久化处理；
         * 3. 返回处理结果或更新对象状态。</p>
         */
        private static AuditOperator fromCurrentUser(CurrentUser user) {
            return new AuditOperator(
                    user.id(),
                    user.username(),
                    user.realName(),
                    user.identityNo(),
                    user.organizationCode(),
                    user.department(),
                    user.contactPhone(),
                    user.terminalType(),
                    user.terminalIdentifier()
            );
        }

        /**
         * 执行 fromLogin 方法。
         * 
         * <p>实现步骤：
         * 1. 接收并校验调用方传入的数据；
         * 2. 按当前方法职责执行业务查询、转换或持久化处理；
         * 3. 返回处理结果或更新对象状态。</p>
         */
        private static AuditOperator fromLogin(UserAccount user, String attemptedAccount, String terminalType, String terminalIdentifier) {
            if (user == null) {
                return new AuditOperator(null, attemptedAccount, null, null, CompanyScope.currentCompanyCode(), null, null, terminalType, terminalIdentifier);
            }
            return new AuditOperator(
                    user.getId(),
                    user.getUsername(),
                    user.getRealName(),
                    user.getIdentityNo(),
                    user.getOrganizationCode(),
                    user.getDepartment(),
                    user.getPhone(),
                    terminalType,
                    terminalIdentifier
            );
        }
    }
}
