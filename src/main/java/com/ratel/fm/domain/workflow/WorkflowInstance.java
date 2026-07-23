package com.ratel.fm.domain.workflow;

import com.ratel.fm.common.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import org.hibernate.annotations.Comment;

import java.time.OffsetDateTime;

/**
 * 流程实例实体。
 *
 * <p>实现目的：业务单据发起审批后生成流程实例，实例保存业务定位信息、当前节点和流程状态，
 * 供审批中心按待办、已办、发起事宜查询。</p>
 */
@Entity
@Table(name = "fm_workflow_instances")
@Comment("流程实例表，保存每次业务审批的运行状态")
public class WorkflowInstance extends BaseEntity {

    /** 所属公司字典编码，流程实例按账套隔离。 */
    @Column(nullable = false, length = 80)
    @Comment("所属公司字典编码，作为流程实例账套隔离标识")
    private String organizationCode;

    /** 流程定义 ID 快照。 */
    @Column(nullable = false)
    @Comment("流程定义ID")
    private Long definitionId;

    /** 流程定义名称快照。 */
    @Column(nullable = false, length = 160)
    @Comment("流程定义名称快照")
    private String definitionName;

    /** 业务模块编码。 */
    @Column(nullable = false, length = 80)
    @Comment("业务模块编码")
    private String businessModuleCode;

    /** 业务模块名称。 */
    @Column(nullable = false, length = 120)
    @Comment("业务模块名称")
    private String businessModuleName;

    /** 功能模块编码。 */
    @Column(nullable = false, length = 120)
    @Comment("功能模块编码")
    private String functionModuleCode;

    /** 功能模块名称。 */
    @Column(nullable = false, length = 160)
    @Comment("功能模块名称")
    private String functionModuleName;

    /** 业务类型，例如 PURCHASE_ORDER。 */
    @Column(nullable = false, length = 80)
    @Comment("业务类型")
    private String businessType;

    /** 业务单据 ID。 */
    @Column(nullable = false)
    @Comment("业务单据ID")
    private Long businessId;

    /** 业务单据编号。 */
    @Column(nullable = false, length = 120)
    @Comment("业务单据编号")
    private String businessNo;

    /** 项目字典编码快照。 */
    @Column(length = 80)
    @Comment("项目字典编码快照")
    private String projectCode;

    /** 项目名称快照。 */
    @Column(length = 160)
    @Comment("项目名称快照")
    private String projectName;

    /** 审批标题。 */
    @Column(nullable = false, length = 300)
    @Comment("审批标题")
    private String title;

    /** 发起理由。 */
    @Column(length = 2000)
    @Comment("申请理由")
    private String applyReason;

    /** 发起人用户 ID。 */
    @Column(nullable = false)
    @Comment("发起人用户ID")
    private Long starterId;

    /** 发起人账号。 */
    @Column(nullable = false, length = 80)
    @Comment("发起人账号")
    private String starterUsername;

    /** 发起人姓名。 */
    @Column(nullable = false, length = 120)
    @Comment("发起人姓名")
    private String starterName;

    /** 申请时间。 */
    @Column(nullable = false)
    @Comment("申请时间")
    private OffsetDateTime startedTime;

    /** 完成时间。 */
    @Column
    @Comment("完成时间")
    private OffsetDateTime completedTime;

    /** 当前节点序号，从 1 开始。 */
    @Column(nullable = false)
    @Comment("当前节点序号")
    private int currentNodeOrder = 1;

    /** 当前节点名称。 */
    @Column(length = 160)
    @Comment("当前节点名称")
    private String currentNodeName;

    /** 流程状态。 */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    @Comment("流程状态")
    private WorkflowStatus status = WorkflowStatus.RUNNING;

    /** 发起时流程节点 JSON 快照。 */
    @Column(nullable = false, columnDefinition = "text")
    @Comment("流程节点JSON快照")
    private String nodesSnapshotJson;

    public String getOrganizationCode() {
        return organizationCode;
    }

    public void setOrganizationCode(String organizationCode) {
        this.organizationCode = organizationCode;
    }

    public Long getDefinitionId() {
        return definitionId;
    }

    public void setDefinitionId(Long definitionId) {
        this.definitionId = definitionId;
    }

    public String getDefinitionName() {
        return definitionName;
    }

    public void setDefinitionName(String definitionName) {
        this.definitionName = definitionName;
    }

    public String getBusinessModuleCode() {
        return businessModuleCode;
    }

    public void setBusinessModuleCode(String businessModuleCode) {
        this.businessModuleCode = businessModuleCode;
    }

    public String getBusinessModuleName() {
        return businessModuleName;
    }

    public void setBusinessModuleName(String businessModuleName) {
        this.businessModuleName = businessModuleName;
    }

    public String getFunctionModuleCode() {
        return functionModuleCode;
    }

    public void setFunctionModuleCode(String functionModuleCode) {
        this.functionModuleCode = functionModuleCode;
    }

    public String getFunctionModuleName() {
        return functionModuleName;
    }

    public void setFunctionModuleName(String functionModuleName) {
        this.functionModuleName = functionModuleName;
    }

    public String getBusinessType() {
        return businessType;
    }

    public void setBusinessType(String businessType) {
        this.businessType = businessType;
    }

    public Long getBusinessId() {
        return businessId;
    }

    public void setBusinessId(Long businessId) {
        this.businessId = businessId;
    }

    public String getBusinessNo() {
        return businessNo;
    }

    public void setBusinessNo(String businessNo) {
        this.businessNo = businessNo;
    }

    public String getProjectCode() {
        return projectCode;
    }

    public void setProjectCode(String projectCode) {
        this.projectCode = projectCode;
    }

    public String getProjectName() {
        return projectName;
    }

    public void setProjectName(String projectName) {
        this.projectName = projectName;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getApplyReason() {
        return applyReason;
    }

    public void setApplyReason(String applyReason) {
        this.applyReason = applyReason;
    }

    public Long getStarterId() {
        return starterId;
    }

    public void setStarterId(Long starterId) {
        this.starterId = starterId;
    }

    public String getStarterUsername() {
        return starterUsername;
    }

    public void setStarterUsername(String starterUsername) {
        this.starterUsername = starterUsername;
    }

    public String getStarterName() {
        return starterName;
    }

    public void setStarterName(String starterName) {
        this.starterName = starterName;
    }

    public OffsetDateTime getStartedTime() {
        return startedTime;
    }

    public void setStartedTime(OffsetDateTime startedTime) {
        this.startedTime = startedTime;
    }

    public OffsetDateTime getCompletedTime() {
        return completedTime;
    }

    public void setCompletedTime(OffsetDateTime completedTime) {
        this.completedTime = completedTime;
    }

    public int getCurrentNodeOrder() {
        return currentNodeOrder;
    }

    public void setCurrentNodeOrder(int currentNodeOrder) {
        this.currentNodeOrder = currentNodeOrder;
    }

    public String getCurrentNodeName() {
        return currentNodeName;
    }

    public void setCurrentNodeName(String currentNodeName) {
        this.currentNodeName = currentNodeName;
    }

    public WorkflowStatus getStatus() {
        return status;
    }

    public void setStatus(WorkflowStatus status) {
        this.status = status;
    }

    public String getNodesSnapshotJson() {
        return nodesSnapshotJson;
    }

    public void setNodesSnapshotJson(String nodesSnapshotJson) {
        this.nodesSnapshotJson = nodesSnapshotJson;
    }
}
