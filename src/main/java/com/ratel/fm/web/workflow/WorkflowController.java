package com.ratel.fm.web.workflow;

import com.github.xiaoymin.knife4j.annotations.ApiOperationSupport;
import com.github.xiaoymin.knife4j.annotations.ApiSupport;
import com.ratel.fm.common.ApiResponse;
import com.ratel.fm.domain.workflow.WorkflowStatus;
import com.ratel.fm.service.workflow.WorkflowService;
import com.ratel.fm.web.dto.workflow.WorkflowDtos.WorkflowApproveRequest;
import com.ratel.fm.web.dto.workflow.WorkflowDtos.WorkflowConfigRequest;
import com.ratel.fm.web.dto.workflow.WorkflowDtos.WorkflowConfigView;
import com.ratel.fm.web.dto.workflow.WorkflowDtos.WorkflowDefinitionRequest;
import com.ratel.fm.web.dto.workflow.WorkflowDtos.WorkflowDefinitionView;
import com.ratel.fm.web.dto.workflow.WorkflowDtos.WorkflowInstanceDetailView;
import com.ratel.fm.web.dto.workflow.WorkflowDtos.WorkflowItemView;
import com.ratel.fm.web.dto.workflow.WorkflowDtos.WorkflowStartRequest;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.List;

/**
 * 审批中心和流程管理接口。
 *
 * <p>实现目的：
 * 1. 暴露待办、已办、发起事宜查询；
 * 2. 暴露流程定义和流程配置维护；
 * 3. 暴露流程发起、审批处理和流程查看能力。</p>
 */
@Tag(name = "审批中心")
@ApiSupport(order = 70, author = "ratel / WenZhang / 18782945613")
@RestController
@RequestMapping("/api/workflows")
public class WorkflowController {

    /** 流程服务，封装流程定义、配置、实例、任务和审批流转逻辑。 */
    private final WorkflowService workflowService;

    /**
     * 构造审批中心控制器。
     *
     * <p>实现步骤：注入 WorkflowService，后续接口只负责参数接收和响应包装。</p>
     */
    public WorkflowController(WorkflowService workflowService) {
        this.workflowService = workflowService;
    }

    @ApiOperationSupport(order = 10)
    @Operation(summary = "查询待办事宜")
    @PreAuthorize("hasAuthority('WORKFLOW_USE')")
    @GetMapping("/center/todo")
    public ApiResponse<List<WorkflowItemView>> todoItems(
            @RequestParam(required = false) String businessModuleCode,
            @RequestParam(required = false) String title,
            @RequestParam(required = false) String projectCode,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startedStart,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startedEnd,
            @RequestParam(required = false) String starterName,
            @RequestParam(required = false) WorkflowStatus status
    ) {
        return ApiResponse.ok(workflowService.todoItems(businessModuleCode, title, projectCode, startedStart, startedEnd, starterName, status));
    }

    @ApiOperationSupport(order = 20)
    @Operation(summary = "查询已办事宜")
    @PreAuthorize("hasAuthority('WORKFLOW_USE')")
    @GetMapping("/center/done")
    public ApiResponse<List<WorkflowItemView>> doneItems(
            @RequestParam(required = false) String businessModuleCode,
            @RequestParam(required = false) String title,
            @RequestParam(required = false) String projectCode,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startedStart,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startedEnd,
            @RequestParam(required = false) String starterName,
            @RequestParam(required = false) WorkflowStatus status
    ) {
        return ApiResponse.ok(workflowService.doneItems(businessModuleCode, title, projectCode, startedStart, startedEnd, starterName, status));
    }

    @ApiOperationSupport(order = 30)
    @Operation(summary = "查询发起事宜")
    @PreAuthorize("hasAuthority('WORKFLOW_USE')")
    @GetMapping("/center/started")
    public ApiResponse<List<WorkflowItemView>> startedItems(
            @RequestParam(required = false) String businessModuleCode,
            @RequestParam(required = false) String title,
            @RequestParam(required = false) String projectCode,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startedStart,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startedEnd,
            @RequestParam(required = false) String starterName,
            @RequestParam(required = false) WorkflowStatus status
    ) {
        return ApiResponse.ok(workflowService.startedItems(businessModuleCode, title, projectCode, startedStart, startedEnd, starterName, status));
    }

    @ApiOperationSupport(order = 40)
    @Operation(summary = "查看流程详情")
    @PreAuthorize("hasAuthority('WORKFLOW_USE')")
    @GetMapping("/instances/{id}")
    public ApiResponse<WorkflowInstanceDetailView> detail(@PathVariable Long id) {
        return ApiResponse.ok(workflowService.detail(id));
    }

    @ApiOperationSupport(order = 50)
    @Operation(summary = "查询业务单据最近流程")
    @PreAuthorize("hasAuthority('WORKFLOW_USE')")
    @GetMapping("/instances/latest")
    public ApiResponse<WorkflowInstanceDetailView> latestBusinessWorkflow(
            @RequestParam String businessType,
            @RequestParam Long businessId
    ) {
        return ApiResponse.ok(workflowService.latestBusinessWorkflow(businessType, businessId));
    }

    @ApiOperationSupport(order = 60)
    @Operation(summary = "发起流程")
    @PreAuthorize("hasAuthority('WORKFLOW_USE')")
    @PostMapping("/instances")
    public ApiResponse<WorkflowItemView> startWorkflow(@Valid @RequestBody WorkflowStartRequest request) {
        return ApiResponse.ok("流程已发起", workflowService.startWorkflow(request));
    }

    @ApiOperationSupport(order = 70)
    @Operation(summary = "审批流程任务")
    @PreAuthorize("hasAuthority('WORKFLOW_USE')")
    @PostMapping("/tasks/{taskId}/approve")
    public ApiResponse<WorkflowItemView> approveTask(
            @PathVariable Long taskId,
            @Valid @RequestBody WorkflowApproveRequest request
    ) {
        return ApiResponse.ok("审批已处理", workflowService.approveTask(taskId, request));
    }

    @ApiOperationSupport(order = 100)
    @Operation(summary = "查询流程定义")
    @PreAuthorize("hasAuthority('WORKFLOW_MANAGE')")
    @GetMapping("/definitions")
    public ApiResponse<List<WorkflowDefinitionView>> listDefinitions(
            @RequestParam(required = false) String name,
            @RequestParam(required = false) String code,
            @RequestParam(required = false) Boolean enabled
    ) {
        return ApiResponse.ok(workflowService.listDefinitions(name, code, enabled));
    }

    @ApiOperationSupport(order = 110)
    @Operation(summary = "查询启用流程定义")
    @PreAuthorize("hasAuthority('WORKFLOW_MANAGE') or hasAuthority('WORKFLOW_USE')")
    @GetMapping("/definitions/enabled")
    public ApiResponse<List<WorkflowDefinitionView>> listEnabledDefinitions() {
        return ApiResponse.ok(workflowService.listEnabledDefinitions());
    }

    @ApiOperationSupport(order = 120)
    @Operation(summary = "新增流程定义")
    @PreAuthorize("hasAuthority('WORKFLOW_MANAGE')")
    @PostMapping("/definitions")
    public ApiResponse<WorkflowDefinitionView> createDefinition(@Valid @RequestBody WorkflowDefinitionRequest request) {
        return ApiResponse.ok("流程定义已保存", workflowService.saveDefinition(null, request));
    }

    @ApiOperationSupport(order = 130)
    @Operation(summary = "修改流程定义")
    @PreAuthorize("hasAuthority('WORKFLOW_MANAGE')")
    @PutMapping("/definitions/{id}")
    public ApiResponse<WorkflowDefinitionView> updateDefinition(
            @PathVariable Long id,
            @Valid @RequestBody WorkflowDefinitionRequest request
    ) {
        return ApiResponse.ok("流程定义已保存", workflowService.saveDefinition(id, request));
    }

    @ApiOperationSupport(order = 140)
    @Operation(summary = "删除流程定义")
    @PreAuthorize("hasAuthority('WORKFLOW_MANAGE')")
    @DeleteMapping("/definitions/{id}")
    public ApiResponse<Void> deleteDefinition(@PathVariable Long id) {
        workflowService.deleteDefinition(id);
        return ApiResponse.ok("流程定义已删除", null);
    }

    @ApiOperationSupport(order = 200)
    @Operation(summary = "查询流程配置")
    @PreAuthorize("hasAuthority('WORKFLOW_MANAGE')")
    @GetMapping("/configs")
    public ApiResponse<List<WorkflowConfigView>> listConfigs(
            @RequestParam(required = false) String businessModuleCode,
            @RequestParam(required = false) String functionModuleCode,
            @RequestParam(required = false) Boolean enabled
    ) {
        return ApiResponse.ok(workflowService.listConfigs(businessModuleCode, functionModuleCode, enabled));
    }

    @ApiOperationSupport(order = 210)
    @Operation(summary = "新增流程配置")
    @PreAuthorize("hasAuthority('WORKFLOW_MANAGE')")
    @PostMapping("/configs")
    public ApiResponse<WorkflowConfigView> createConfig(@Valid @RequestBody WorkflowConfigRequest request) {
        return ApiResponse.ok("流程配置已保存", workflowService.saveConfig(null, request));
    }

    @ApiOperationSupport(order = 220)
    @Operation(summary = "修改流程配置")
    @PreAuthorize("hasAuthority('WORKFLOW_MANAGE')")
    @PutMapping("/configs/{id}")
    public ApiResponse<WorkflowConfigView> updateConfig(
            @PathVariable Long id,
            @Valid @RequestBody WorkflowConfigRequest request
    ) {
        return ApiResponse.ok("流程配置已保存", workflowService.saveConfig(id, request));
    }

    @ApiOperationSupport(order = 230)
    @Operation(summary = "删除流程配置")
    @PreAuthorize("hasAuthority('WORKFLOW_MANAGE')")
    @DeleteMapping("/configs/{id}")
    public ApiResponse<Void> deleteConfig(@PathVariable Long id) {
        workflowService.deleteConfig(id);
        return ApiResponse.ok("流程配置已删除", null);
    }
}
