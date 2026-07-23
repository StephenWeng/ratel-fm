package com.ratel.fm.domain.knowledge;

import com.ratel.fm.domain.auth.PermissionCode;

/**
 * 知识索引来源类型。
 */
public enum KnowledgeSourceType {
    /**
     * 枚举值 BASIC_DICTIONARY：表示基础字典、项目、部门、岗位、物料、供应商、客户等基础资料知识。
     */
    BASIC_DICTIONARY(PermissionCode.BASIC_DICT_MANAGE, "基础字典"),
    /**
     * 枚举值 SUBJECT：表示 SUBJECT 对应的业务状态或类型。
     */
    SUBJECT(PermissionCode.FINANCE_SUBJECT_MANAGE, "会计科目"),
    /**
     * 枚举值 VOUCHER：表示 VOUCHER 对应的业务状态或类型。
     */
    VOUCHER(PermissionCode.FINANCE_VOUCHER_MANAGE, "财务凭证"),
    /**
     * 枚举值 PURCHASE_ORDER：表示 PURCHASE_ORDER 对应的业务状态或类型。
     */
    PURCHASE_ORDER(PermissionCode.PURCHASE_MANAGE, "采购单"),
    /**
     * 枚举值 SHIPMENT：表示 SHIPMENT 对应的业务状态或类型。
     */
    SHIPMENT(PermissionCode.LOGISTICS_MANAGE, "物流单"),
    /**
     * 枚举值 INVENTORY_LEDGER：表示 INVENTORY_LEDGER 对应的业务状态或类型。
     */
    INVENTORY_LEDGER(PermissionCode.INVENTORY_MANAGE, "库存流水"),
    /**
     * 枚举值 AR_AP_BILL：表示 AR_AP_BILL 对应的业务状态或类型。
     */
    AR_AP_BILL(PermissionCode.AR_AP_MANAGE, "应收应付"),
    /**
     * 枚举值 CASHIER_TRANSACTION：表示出纳收款、付款、转账和退款流水知识。
     */
    CASHIER_TRANSACTION(PermissionCode.FINANCE_VOUCHER_MANAGE, "出纳流水"),
    /**
     * 枚举值 ATTACHMENT：表示 ATTACHMENT 对应的业务状态或类型。
     */
    ATTACHMENT(null, "业务附件"),
    /**
     * 枚举值 USER_DOCUMENT：表示用户上传的本地知识库资料。
     */
    USER_DOCUMENT(PermissionCode.AI_ASSISTANT_USE, "本地知识库");

    /**
     * 字段 permissionCode：保存 permissionCode 对应的业务数据、运行配置或依赖对象，供本类逻辑读取和维护。
     */
    private final PermissionCode permissionCode;
    /**
     * 字段 label：保存 label 对应的业务数据、运行配置或依赖对象，供本类逻辑读取和维护。
     */
    private final String label;

    KnowledgeSourceType(PermissionCode permissionCode, String label) {
        this.permissionCode = permissionCode;
        this.label = label;
    }

    /**
     * 执行 permissionCode 方法。
     * 
     * <p>实现步骤：
     * 1. 接收并校验调用方传入的数据；
     * 2. 按当前方法职责执行业务查询、转换或持久化处理；
     * 3. 返回处理结果或更新对象状态。</p>
     */
    public PermissionCode permissionCode() {
        return permissionCode;
    }

    /**
     * 执行 label 方法。
     * 
     * <p>实现步骤：
     * 1. 接收并校验调用方传入的数据；
     * 2. 按当前方法职责执行业务查询、转换或持久化处理；
     * 3. 返回处理结果或更新对象状态。</p>
     */
    public String label() {
        return label;
    }
}
