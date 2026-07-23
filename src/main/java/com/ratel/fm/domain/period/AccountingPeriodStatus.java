package com.ratel.fm.domain.period;

/**
 * 会计期间状态。
 *
 * <p>实现目的：标识当前账套某个月度期间是否允许继续录入凭证和业务单据，
 * 关闭状态用于月结后冻结核算口径。</p>
 */
public enum AccountingPeriodStatus {
    /** 期间开启，允许继续录入和过账业务。 */
    OPEN,
    /** 期间已关闭，代表财务已经完成本期结账。 */
    CLOSED
}
