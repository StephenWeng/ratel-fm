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
 * 流程操作流水实体。
 *
 * <p>实现目的：按时间顺序记录流程发起、每个节点审批人和审批意见，
 * 供流程查看弹窗展示完整审批轨迹。</p>
 */
@Entity
@Table(name = "fm_workflow_operation_logs")
@Comment("流程操作流水表，记录发起和每次审批动作")
public class WorkflowOperationLog extends BaseEntity {

    /** 所属流程实例。 */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "instance_id", nullable = false)
    @Comment("流程实例ID")
    private WorkflowInstance instance;

    /** 所属公司字典编码，流水按账套隔离。 */
    @Column(nullable = false, length = 80)
    @Comment("所属公司字典编码")
    private String organizationCode;

    /** 操作类型。 */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    @Comment("操作类型")
    private WorkflowOperationType operationType;

    /** 操作节点序号。 */
    @Column
    @Comment("操作节点序号")
    private Integer nodeOrder;

    /** 操作节点名称。 */
    @Column(length = 160)
    @Comment("操作节点名称")
    private String nodeName;

    /** 操作人用户 ID。 */
    @Column(nullable = false)
    @Comment("操作人用户ID")
    private Long operatorId;

    /** 操作人账号。 */
    @Column(nullable = false, length = 80)
    @Comment("操作人账号")
    private String operatorUsername;

    /** 操作人姓名。 */
    @Column(nullable = false, length = 120)
    @Comment("操作人姓名")
    private String operatorName;

    /** 审批意见或申请理由。 */
    @Column(length = 2000)
    @Comment("审批意见或申请理由")
    private String comment;

    /** 操作时间。 */
    @Column(nullable = false)
    @Comment("操作时间")
    private OffsetDateTime operationTime;

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

    public WorkflowOperationType getOperationType() {
        return operationType;
    }

    public void setOperationType(WorkflowOperationType operationType) {
        this.operationType = operationType;
    }

    public Integer getNodeOrder() {
        return nodeOrder;
    }

    public void setNodeOrder(Integer nodeOrder) {
        this.nodeOrder = nodeOrder;
    }

    public String getNodeName() {
        return nodeName;
    }

    public void setNodeName(String nodeName) {
        this.nodeName = nodeName;
    }

    public Long getOperatorId() {
        return operatorId;
    }

    public void setOperatorId(Long operatorId) {
        this.operatorId = operatorId;
    }

    public String getOperatorUsername() {
        return operatorUsername;
    }

    public void setOperatorUsername(String operatorUsername) {
        this.operatorUsername = operatorUsername;
    }

    public String getOperatorName() {
        return operatorName;
    }

    public void setOperatorName(String operatorName) {
        this.operatorName = operatorName;
    }

    public String getComment() {
        return comment;
    }

    public void setComment(String comment) {
        this.comment = comment;
    }

    public OffsetDateTime getOperationTime() {
        return operationTime;
    }

    public void setOperationTime(OffsetDateTime operationTime) {
        this.operationTime = operationTime;
    }
}
