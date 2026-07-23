package com.ratel.fm.domain.auth;

/**
 * PermissionCode 枚举。
 * 
 * <p>用于承载 PermissionCode 相关的业务数据、接口契约或处理逻辑，便于维护人员快速定位模块职责。</p>
 */
public enum PermissionCode {
    /**
     * 枚举值 SYSTEM_USER_MANAGE：表示 SYSTEM_USER_MANAGE 对应的业务状态或类型。
     */
    SYSTEM_USER_MANAGE,
    /**
     * 枚举值 SYSTEM_ROLE_MANAGE：表示 SYSTEM_ROLE_MANAGE 对应的业务状态或类型。
     */
    SYSTEM_ROLE_MANAGE,
    /**
     * 枚举值 BASIC_DICT_MANAGE：表示 BASIC_DICT_MANAGE 对应的业务状态或类型。
     */
    BASIC_DICT_MANAGE,
    /**
     * 枚举值 FINANCE_SUBJECT_MANAGE：表示 FINANCE_SUBJECT_MANAGE 对应的业务状态或类型。
     */
    FINANCE_SUBJECT_MANAGE,
    /**
     * 枚举值 FINANCE_VOUCHER_MANAGE：表示 FINANCE_VOUCHER_MANAGE 对应的业务状态或类型。
     */
    FINANCE_VOUCHER_MANAGE,
    /**
     * 枚举值 PURCHASE_MANAGE：表示 PURCHASE_MANAGE 对应的业务状态或类型。
     */
    PURCHASE_MANAGE,
    /**
     * 枚举值 LOGISTICS_MANAGE：表示 LOGISTICS_MANAGE 对应的业务状态或类型。
     */
    LOGISTICS_MANAGE,
    /**
     * 枚举值 INVENTORY_MANAGE：表示 INVENTORY_MANAGE 对应的业务状态或类型。
     */
    INVENTORY_MANAGE,
    /**
     * 枚举值 AR_AP_MANAGE：表示 AR_AP_MANAGE 对应的业务状态或类型。
     */
    AR_AP_MANAGE,
    /**
     * 枚举值 WORKFLOW_USE：表示审批中心待办、已办、发起事宜和审批处理权限。
     */
    WORKFLOW_USE,
    /**
     * 枚举值 WORKFLOW_MANAGE：表示流程定义和流程配置维护权限。
     */
    WORKFLOW_MANAGE,
    /**
     * 枚举值 AI_ASSISTANT_USE：表示 AI_ASSISTANT_USE 对应的业务状态或类型。
     */
    AI_ASSISTANT_USE,
    /**
     * 枚举值 REPORT_VIEW：表示 REPORT_VIEW 对应的业务状态或类型。
     */
    REPORT_VIEW,
    /**
     * 枚举值 SEARCH_VIEW：表示 SEARCH_VIEW 对应的业务状态或类型。
     */
    SEARCH_VIEW,
    AUDIT_LOG_VIEW
}
