package com.ratel.fm.domain.cashier;

/**
 * 出纳流水类型。
 *
 * <p>实现目的：区分收款、付款、内部转账和退款等资金动作，供出纳台账和会计平台制证使用。</p>
 */
public enum CashierTransactionType {
    /** 收到客户、员工或其他单位款项。 */
    RECEIPT,
    /** 支付供应商、员工或其他单位款项。 */
    PAYMENT,
    /** 银行账户、现金账户之间的内部调拨。 */
    TRANSFER,
    /** 业务退款或冲回。 */
    REFUND
}
