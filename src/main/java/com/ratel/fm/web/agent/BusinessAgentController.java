package com.ratel.fm.web.agent;

import com.github.xiaoymin.knife4j.annotations.ApiOperationSupport;
import com.github.xiaoymin.knife4j.annotations.ApiSupport;
import com.ratel.fm.common.ApiResponse;
import com.ratel.fm.service.agent.BusinessAgentService;
import com.ratel.fm.web.dto.agent.BusinessAgentDtos.BusinessAgentRequest;
import com.ratel.fm.web.dto.agent.BusinessAgentDtos.BusinessAgentResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 业务 Agent 接口。
 *
 * <p>按只读、草稿、受控执行和多步骤四个阶段提供采购、物流、库存、应收应付、财务和审批模块的 Agent 能力。</p>
 */
@Tag(name = "业务 Agent")
@ApiSupport(order = 82, author = "ratel / WenZhang / 18782945613")
@RestController
@RequestMapping("/api/agent")
public class BusinessAgentController {

    private final BusinessAgentService businessAgentService;

    public BusinessAgentController(BusinessAgentService businessAgentService) {
        this.businessAgentService = businessAgentService;
    }

    @ApiOperationSupport(order = 10, author = "ratel / WenZhang / 18782945613")
    @Operation(summary = "运行业务 Agent", description = "结合采购、物流、库存、应收应付、财务和审批模块执行阶段化 Agent 分析和计划生成。")
    @PreAuthorize("hasAuthority('AI_ASSISTANT_USE')")
    @PostMapping("/business")
    public ApiResponse<BusinessAgentResponse> run(@RequestBody(required = false) BusinessAgentRequest request) {
        return ApiResponse.ok(businessAgentService.run(request));
    }
}
