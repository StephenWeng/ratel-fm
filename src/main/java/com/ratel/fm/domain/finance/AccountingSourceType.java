package com.ratel.fm.domain.finance;

/**
 * 自动凭证业务来源类型。
 *
 * <p>用于在凭证主表和会计平台 DTO 之间统一标识来源模块，保证凭证可以反向追溯到原始业务单据。</p>
 */
public enum AccountingSourceType {
    /** 采购单来源，通常用于生成采购应付或采购费用类凭证草稿。 */
    PURCHASE_ORDER,
    /** 应收应付单来源，按应收或应付类型生成往来确认凭证草稿。 */
    AR_AP_BILL,
    /** 库存流水来源，按入库、出库、调拨等库存事实生成存货或成本类凭证草稿。 */
    INVENTORY_LEDGER,
    /** 出纳流水来源，按已确认资金收付生成现金或银行存款类凭证草稿。 */
    CASHIER_TRANSACTION
}
