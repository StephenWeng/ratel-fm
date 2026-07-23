package com.ratel.fm.web.insight;

import com.ratel.fm.common.ApiResponse;
import com.ratel.fm.service.ai.AiComponentHealthService;
import com.ratel.fm.web.dto.ai.AiStatusDtos.AiComponentStatusResponse;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * AI 组件运行状态接口。
 */
@RestController
@RequestMapping("/api/ai/status")
public class AiStatusController {

    private final AiComponentHealthService healthService;

    public AiStatusController(AiComponentHealthService healthService) {
        this.healthService = healthService;
    }

    /**
     * 查询当前大模型、向量库、索引和流式输出状态。
     */
    @GetMapping
    @PreAuthorize("hasAuthority('SEARCH_VIEW') or hasAuthority('AI_ASSISTANT_USE')")
    public ApiResponse<AiComponentStatusResponse> status() {
        return ApiResponse.ok(healthService.status());
    }
}
