package com.ratel.fm.domain.workflow;

import com.ratel.fm.common.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import org.hibernate.annotations.Comment;

import java.time.OffsetDateTime;

/**
 * 流程审批任务实体。
 *
 * <p>实现目的：每个流程节点生成一个任务，任务保存节点审批人范围和实际处理人，
 * 审批中心待办、已办列表均基于该表查询。</p>
 */
@Entity
@Table(name = "fm_workflow_tasks")
@Comment("流程审批任务表，保存每个节点的待办和处理结果")
public class WorkflowTask extends BaseEntity {

    /** 所属流程实例。 */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "instance_id", nullable = false)
    @Comment("流程实例ID")
    private WorkflowInstance instance;

    /** 所属公司字典编码，任务按账套隔离。 */
    @Column(nullable = false, length = 80)
    @Comment("所属公司字典编码")
    private String organizationCode;

    /** 节点序号，从 1 开始。 */
    @Column(nullable = false)
    @Comment("节点序号")
    private int nodeOrder;

    /** 节点名称。 */
    @Column(nullable = false, length = 160)
    @Comment("节点名称")
    private String nodeName;

    /** 审批人来源类型。 */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 40)
    @Comment("审批人来源类型")
    private WorkflowApproverType approverType;

    /** 指定审批用户 ID。 */
    @Column
    @Comment("指定审批用户ID")
    private Long approverUserId;

    /** 指定审批用户账号。 */
    @Column(length = 80)
    @Comment("指定审批用户账号")
    private String approverUsername;

    /** 指定审批用户姓名。 */
    @Column(length = 120)
    @Comment("指定审批用户姓名")
    private String approverName;

    /** 审批部门名称。 */
    @Column(length = 80)
    @Comment("审批部门名称")
    private String approverDepartment;

    /** 审批岗位名称。 */
    @Column(length = 80)
    @Comment("审批岗位名称")
    private String approverPosition;

    /** 待办展示的下个节点审批人信息。 */
    @Column(length = 300)
    @Comment("审批人展示文本")
    private String approverDisplay;

    /** 任务状态。 */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    @Comment("任务状态")
    private WorkflowTaskStatus status = WorkflowTaskStatus.PENDING;

    /** 实际审批人用户 ID。 */
    @Column
    @Comment("实际审批人用户ID")
    private Long actedById;

    /** 实际审批人账号。 */
    @Column(length = 80)
    @Comment("实际审批人账号")
    private String actedByUsername;

    /** 实际审批人姓名。 */
    @Column(length = 120)
    @Comment("实际审批人姓名")
    private String actedByName;

    /** 审批意见。 */
    @Column(length = 2000)
    @Comment("审批意见")
    private String comment;

    /** 任务创建时间。 */
    @Column(nullable = false)
    @Comment("任务创建时间")
    private OffsetDateTime createdAt;

    /** 实际审批时间。 */
    @Column
    @Comment("实际审批时间")
    private OffsetDateTime actedAt;

    public WorkflowInstance getInstance() {
        return instance;
    }

    public void setInstance(WorkflowInstance instance) {
        this.instance = instance;
    }

    public String getOrganizationCode() {
        return organizationCode;
    }

    public void setOrganizationCode(String organizationCode) {
        this.organizationCode = organizationCode;
    }

    public int getNodeOrder() {
        return nodeOrder;
    }

    public void setNodeOrder(int nodeOrder) {
        this.nodeOrder = nodeOrder;
    }

    public String getNodeName() {
        return nodeName;
    }

    public void setNodeName(String nodeName) {
        this.nodeName = nodeName;
    }

    public WorkflowApproverType getApproverType() {
        return approverType;
    }

    public void setApproverType(WorkflowApproverType approverType) {
        this.approverType = approverType;
    }

    public Long getApproverUserId() {
        return approverUserId;
    }

    public void setApproverUserId(Long approverUserId) {
        this.approverUserId = approverUserId;
    }

    public String getApproverUsername() {
        return approverUsername;
    }

    public void setApproverUsername(String approverUsername) {
        this.approverUsername = approverUsername;
    }

    public String getApproverName() {
        return approverName;
    }

    public void setApproverName(String approverName) {
        this.approverName = approverName;
    }

    public String getApproverDepartment() {
        return approverDepartment;
    }

    public void setApproverDepartment(String approverDepartment) {
        this.approverDepartment = approverDepartment;
    }

    public String getApproverPosition() {
        return approverPosition;
    }

    public void setApproverPosition(String approverPosition) {
        this.approverPosition = approverPosition;
    }

    public String getApproverDisplay() {
        return approverDisplay;
    }

    public void setApproverDisplay(String approverDisplay) {
        this.approverDisplay = approverDisplay;
    }

    public WorkflowTaskStatus getStatus() {
        return status;
    }

    public void setStatus(WorkflowTaskStatus status) {
        this.status = status;
    }

    public Long getActedById() {
        return actedById;
    }

    public void setActedById(Long actedById) {
        this.actedById = actedById;
    }

    public String getActedByUsername() {
        return actedByUsername;
    }

    public void setActedByUsername(String actedByUsername) {
        this.actedByUsername = actedByUsername;
    }

    public String getActedByName() {
        return actedByName;
    }

    public void setActedByName(String actedByName) {
        this.actedByName = actedByName;
    }

    public String getComment() {
        return comment;
    }

    public void setComment(String comment) {
        this.comment = comment;
    }

    public OffsetDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(OffsetDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public OffsetDateTime getActedAt() {
        return actedAt;
    }

    public void setActedAt(OffsetDateTime actedAt) {
        this.actedAt = actedAt;
    }
}
