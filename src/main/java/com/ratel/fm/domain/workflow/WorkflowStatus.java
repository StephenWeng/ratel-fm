package com.ratel.fm.domain.workflow;

/**
 * 流程实例状态枚举。
 *
 * <p>字段含义：
 * 1. RUNNING 表示流程正在审批中；
 * 2. APPROVED 表示流程所有节点均同意；
 * 3. REJECTED 表示任一节点不同意后流程结束；
 * 4. CANCELLED 预留给后续发起人撤回或业务取消。</p>
 */
public enum WorkflowStatus {
    /** 流程正在审批中。 */
    RUNNING,
    /** 流程已同意通过。 */
    APPROVED,
    /** 流程已不同意驳回。 */
    REJECTED,
    /** 流程已取消。 */
    CANCELLED
}
