package com.ratel.fm.domain.workflow;

/**
 * 流程节点审批人来源类型。
 *
 * <p>字段含义：
 * 1. USER 表示指定具体人员审批；
 * 2. DEPARTMENT 表示指定部门内任一人员可审批；
 * 3. DEPARTMENT_POSITION 表示指定部门下指定岗位人员可审批。</p>
 */
public enum WorkflowApproverType {
    /** 指定具体人员审批。 */
    USER,
    /** 指定部门下人员审批。 */
    DEPARTMENT,
    /** 指定部门下指定岗位人员审批。 */
    DEPARTMENT_POSITION
}
