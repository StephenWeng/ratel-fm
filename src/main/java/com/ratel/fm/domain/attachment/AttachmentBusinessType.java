package com.ratel.fm.domain.attachment;

import com.ratel.fm.domain.auth.PermissionCode;

/**
 * 附件所属业务类型。
 *
 * <p>该枚举用于把统一附件表和具体业务记录建立关系，避免每个业务模块重复建设附件表。</p>
 *
 * <p>开发组织：ratel；开发人员：WenZhang；联系方式：18782945613。</p>
 *
 * @author ratel
 */
public enum AttachmentBusinessType {
    /** 财务凭证附件，作为凭证记账的业务证据。 */
    VOUCHER(PermissionCode.FINANCE_VOUCHER_MANAGE, true),
    /** 采购订单附件，保存采购合同、报价单等证据。 */
    PURCHASE_ORDER(PermissionCode.PURCHASE_MANAGE, true),
    /** 物流单附件，保存运单、签收单等证据。 */
    SHIPMENT(PermissionCode.LOGISTICS_MANAGE, true),
    /** 库存台账附件，保存入库、出库、调拨、盘点凭据。 */
    INVENTORY_LEDGER(PermissionCode.INVENTORY_MANAGE, true),
    /** 应收应付附件，保存发票、付款计划、对账资料等证据。 */
    AR_AP_BILL(PermissionCode.AR_AP_MANAGE, true);

    /**
     * 字段 managePermission：保存 managePermission 对应的业务数据、运行配置或依赖对象，供本类逻辑读取和维护。
     */
    private final PermissionCode managePermission;
    /**
     * 字段 reportVisible：保存 reportVisible 对应的业务数据、运行配置或依赖对象，供本类逻辑读取和维护。
     */
    private final boolean reportVisible;

    AttachmentBusinessType(PermissionCode managePermission, boolean reportVisible) {
        this.managePermission = managePermission;
        this.reportVisible = reportVisible;
    }

    /**
     * 执行 managePermission 方法。
     * 
     * <p>实现步骤：
     * 1. 接收并校验调用方传入的数据；
     * 2. 按当前方法职责执行业务查询、转换或持久化处理；
     * 3. 返回处理结果或更新对象状态。</p>
     */
    public PermissionCode managePermission() {
        return managePermission;
    }

    /**
     * 执行 reportVisible 方法。
     * 
     * <p>实现步骤：
     * 1. 接收并校验调用方传入的数据；
     * 2. 按当前方法职责执行业务查询、转换或持久化处理；
     * 3. 返回处理结果或更新对象状态。</p>
     */
    public boolean reportVisible() {
        return reportVisible;
    }
}
