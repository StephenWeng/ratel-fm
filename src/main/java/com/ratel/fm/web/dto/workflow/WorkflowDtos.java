package com.ratel.fm.web.dto.workflow;

import com.ratel.fm.domain.workflow.WorkflowApproverType;
import com.ratel.fm.domain.workflow.WorkflowOperationType;
import com.ratel.fm.domain.workflow.WorkflowStatus;
import com.ratel.fm.domain.workflow.WorkflowTaskStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.List;
import java.util.Map;

/**
 * 工作流接口 DTO 集合。
 *
 * <p>实现目的：
 * 1. 将流程定义、流程配置、流程实例和任务列表统一封装为前后端契约；
 * 2. 对申请理由和审批意见等长文本进行长度约束；
 * 3. 为审批中心页面提供待办、已办、发起事宜和流程查看所需完整字段。</p>
 */
public final class WorkflowDtos {

    private WorkflowDtos() {
    }

    /** 流程节点请求。 */
    public record WorkflowNodeRequest(
            @Schema(description = "节点名称。")
            @NotBlank(message = "节点名称不能为空")
            @Size(max = 160, message = "节点名称长度不能超过160个字符")
            String nodeName,
            @Schema(description = "审批人类型。")
            @NotNull(message = "审批人类型不能为空")
            WorkflowApproverType approverType,
            @Schema(description = "指定审批用户ID。")
            Long approverUserId,
            @Schema(description = "指定审批用户账号。")
            @Size(max = 80, message = "审批用户账号长度不能超过80个字符")
            String approverUsername,
            @Schema(description = "指定审批用户姓名。")
            @Size(max = 120, message = "审批用户姓名长度不能超过120个字符")
            String approverName,
            @Schema(description = "审批部门名称。")
            @Size(max = 80, message = "审批部门长度不能超过80个字符")
            String approverDepartment,
            @Schema(description = "审批岗位名称。")
            @Size(max = 80, message = "审批岗位长度不能超过80个字符")
            String approverPosition
    ) {
    }

    /** 流程节点视图。 */
    public record WorkflowNodeView(
            int nodeOrder,
            String nodeName,
            WorkflowApproverType approverType,
            Long approverUserId,
            String approverUsername,
            String approverName,
            String approverDepartment,
            String approverPosition,
            String approverDisplay
    ) {
    }

    /** 流程定义保存请求。 */
    public record WorkflowDefinitionRequest(
            @NotBlank(message = "流程模板名称不能为空")
            @Size(max = 160, message = "流程模板名称长度不能超过160个字符")
            String name,
            @NotBlank(message = "流程模板编码不能为空")
            @Size(max = 120, message = "流程模板编码长度不能超过120个字符")
            String code,
            @Size(max = 500, message = "流程说明长度不能超过500个字符")
            String description,
            @Valid @NotEmpty(message = "至少配置一个审批节点")
            List<WorkflowNodeRequest> nodes,
            Boolean enabled
    ) {
    }

    /** 流程定义视图。 */
    public record WorkflowDefinitionView(
            Long id,
            String organizationCode,
            String name,
            String code,
            String description,
            List<WorkflowNodeView> nodes,
            boolean enabled
    ) {
    }

    /** 流程配置保存请求。 */
    public record WorkflowConfigRequest(
            @NotBlank(message = "业务模块编码不能为空")
            @Size(max = 80, message = "业务模块编码长度不能超过80个字符")
            String businessModuleCode,
            @NotBlank(message = "业务模块名称不能为空")
            @Size(max = 120, message = "业务模块名称长度不能超过120个字符")
            String businessModuleName,
            @NotBlank(message = "功能模块编码不能为空")
            @Size(max = 120, message = "功能模块编码长度不能超过120个字符")
            String functionModuleCode,
            @NotBlank(message = "功能模块名称不能为空")
            @Size(max = 160, message = "功能模块名称长度不能超过160个字符")
            String functionModuleName,
            @NotNull(message = "流程模板不能为空")
            Long definitionId,
            Boolean enabled
    ) {
    }

    /** 流程配置视图。 */
    public record WorkflowConfigView(
            Long id,
            String organizationCode,
            String businessModuleCode,
            String businessModuleName,
            String functionModuleCode,
            String functionModuleName,
            Long definitionId,
            String definitionName,
            boolean enabled
    ) {
    }

    /** 流程发起请求。 */
    public record WorkflowStartRequest(
            @NotBlank(message = "功能模块编码不能为空")
            String functionModuleCode,
            @NotBlank(message = "业务类型不能为空")
            String businessType,
            @NotNull(message = "业务ID不能为空")
            Long businessId,
            @NotBlank(message = "业务单号不能为空")
            String businessNo,
            @Size(max = 80, message = "项目编码长度不能超过80个字符")
            String projectCode,
            @Size(max = 160, message = "项目名称长度不能超过160个字符")
            String projectName,
            @NotBlank(message = "审批标题不能为空")
            @Size(max = 300, message = "审批标题长度不能超过300个字符")
            String title,
            @Size(max = 2000, message = "申请理由长度不能超过2000个字符")
            String applyReason
    ) {
    }

    /** 流程审批请求。 */
    public record WorkflowApproveRequest(
            @NotNull(message = "审批结果不能为空")
            Boolean approved,
            @NotBlank(message = "审批意见不能为空")
            @Size(max = 2000, message = "审批意见长度不能超过2000个字符")
            String comment
    ) {
    }

    /** 审批中心列表项。 */
    public record WorkflowItemView(
            Long instanceId,
            Long taskId,
            String businessModuleCode,
            String businessModuleName,
            String functionModuleCode,
            String functionModuleName,
            String businessType,
            Long businessId,
            String businessNo,
            String projectCode,
            String projectName,
            String title,
            Long starterId,
            String starterUsername,
            String starterName,
            String startedTime,
            String currentNodeName,
            WorkflowStatus status,
            String nextApproverInfo,
            List<WorkflowApproverUserView> nextApproverUsers,
            WorkflowTaskStatus taskStatus,
            String actedTime
    ) {
    }

    /** 下个节点审批人范围命中的人员。 */
    public record WorkflowApproverUserView(
            String name,
            String phone
    ) {
    }

    /** 流程任务节点视图。 */
    public record WorkflowTaskView(
            Long id,
            int nodeOrder,
            String nodeName,
            String approverDisplay,
            WorkflowTaskStatus status,
            Long actedById,
            String actedByUsername,
            String actedByName,
            String comment,
            String actedAt
    ) {
    }

    /** 流程操作流水视图。 */
    public record WorkflowOperationLogView(
            Long id,
            WorkflowOperationType operationType,
            Integer nodeOrder,
            String nodeName,
            Long operatorId,
            String operatorUsername,
            String operatorName,
            String operatorPhone,
            String comment,
            String operationTime
    ) {
    }

    /** 流程携带的业务表单预览。 */
    public record WorkflowBusinessFormView(
            String title,
            List<WorkflowBusinessFormSectionView> sections,
            List<WorkflowBusinessFormTableView> tables
    ) {
    }

    /** 业务表单字段分组。 */
    public record WorkflowBusinessFormSectionView(
            String title,
            List<WorkflowBusinessFormFieldView> fields
    ) {
    }

    /** 业务表单字段。 */
    public record WorkflowBusinessFormFieldView(
            String label,
            String value
    ) {
    }

    /** 业务表单明细表格。 */
    public record WorkflowBusinessFormTableView(
            String title,
            List<WorkflowBusinessFormTableColumnView> columns,
            List<Map<String, String>> rows
    ) {
    }

    /** 业务表单明细表格列定义。 */
    public record WorkflowBusinessFormTableColumnView(
            String key,
            String label
    ) {
    }

    /** 流程查看详情。 */
    public record WorkflowInstanceDetailView(
            WorkflowItemView instance,
            List<WorkflowTaskView> tasks,
            List<WorkflowOperationLogView> operationLogs,
            WorkflowBusinessFormView businessForm
    ) {
    }
}
