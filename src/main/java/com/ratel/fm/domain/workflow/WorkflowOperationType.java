package com.ratel.fm.domain.workflow;

/**
 * 流程操作流水类型枚举。
 *
 * <p>字段含义：START 记录发起动作，APPROVE 记录同意动作，REJECT 记录不同意动作，CANCEL 记录业务取消动作。</p>
 */
public enum WorkflowOperationType {
    /** 发起流程。 */
    START,
    /** 审批同意。 */
    APPROVE,
    /** 审批不同意。 */
    REJECT,
    /** 业务取消流程。 */
    CANCEL
}
