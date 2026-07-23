package com.ratel.fm.domain.purchase;

/**
 * PurchaseStatus 枚举。
 * 
 * <p>用于承载 PurchaseStatus 相关的业务数据、接口契约或处理逻辑，便于维护人员快速定位模块职责。</p>
 */
public enum PurchaseStatus {
    /**
     * 枚举值 DRAFT：表示 DRAFT 对应的业务状态或类型。
     */
    DRAFT,
    /**
     * 枚举值 IN_APPROVAL：表示采购单已提交流程，正在审批中心流转。
     */
    IN_APPROVAL,
    /**
     * 枚举值 APPROVAL_REJECTED：表示流程已审批完成但结果为不同意，采购单可继续编辑后重新提交。
     */
    APPROVAL_REJECTED,
    /**
     * 枚举值 SUBMITTED：表示 SUBMITTED 对应的业务状态或类型。
     */
    SUBMITTED,
    /**
     * 枚举值 APPROVED：表示 APPROVED 对应的业务状态或类型。
     */
    APPROVED,
    /**
     * 枚举值 PURCHASING：表示审批同意后已发起采购履约。
     */
    PURCHASING,
    /**
     * 枚举值 PURCHASE_COMPLETED：表示采购履约完成，通常由已收货动作触发。
     */
    PURCHASE_COMPLETED,
    /**
     * 枚举值 RECEIVED：表示 RECEIVED 对应的业务状态或类型。
     */
    RECEIVED,
    /**
     * 枚举值 CLOSED：表示 CLOSED 对应的业务状态或类型。
     */
    CLOSED,
    CANCELLED
}
