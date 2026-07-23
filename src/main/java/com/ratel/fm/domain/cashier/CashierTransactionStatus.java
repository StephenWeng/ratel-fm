package com.ratel.fm.domain.cashier;

/**
 * 出纳流水状态。
 *
 * <p>实现目的：控制出纳流水从草稿到确认、制证、取消的业务流转，避免未确认资金直接进入凭证。</p>
 */
public enum CashierTransactionStatus {
    /** 草稿状态，允许继续修改或删除。 */
    DRAFT,
    /** 已确认状态，表示资金事实已经发生。 */
    CONFIRMED,
    /** 已制证状态，表示已经生成财务凭证。 */
    VOUCHERED,
    /** 已取消状态，表示该资金流水作废。 */
    CANCELLED
}
