package com.ratel.fm.domain.workflow;

/**
 * 流程任务状态枚举。
 *
 * <p>字段含义：
 * 1. PENDING 表示该任务仍待审批人处理；
 * 2. APPROVED 表示该任务审批同意；
 * 3. REJECTED 表示该任务审批不同意；
 * 4. SKIPPED 预留给后续条件分支跳过节点。</p>
 */
public enum WorkflowTaskStatus {
    /** 待当前节点审批人处理。 */
    PENDING,
    /** 当前节点已同意。 */
    APPROVED,
    /** 当前节点已不同意。 */
    REJECTED,
    /** 当前节点被条件规则跳过。 */
    SKIPPED
}
