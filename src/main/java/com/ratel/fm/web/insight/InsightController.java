package com.ratel.fm.web.insight;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.github.xiaoymin.knife4j.annotations.ApiOperationSupport;
import com.github.xiaoymin.knife4j.annotations.ApiSupport;
import com.ratel.fm.common.ApiResponse;
import com.ratel.fm.service.insight.InsightService;
import com.ratel.fm.service.knowledge.KnowledgeIndexService;
import com.ratel.fm.web.dto.insight.InsightDtos.DashboardOverview;
import com.ratel.fm.web.dto.knowledge.KnowledgeDtos.KnowledgeRebuildResponse;
import com.ratel.fm.web.dto.knowledge.KnowledgeDtos.KnowledgeSearchResponse;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;

@Tag(name = "统计分析与智能检索")
@ApiSupport(order = 40, author = "ratel / WenZhang / 18782945613")
@RestController
@RequestMapping("/api")
/**
 * InsightController 类。
 * 
 * <p>
 * 用于承载 InsightController 相关的业务数据、接口契约或处理逻辑，便于维护人员快速定位模块职责。
 * </p>
 */
public class InsightController {

	/**
	 * 字段 insightService：保存 insightService 对应的业务数据、运行配置或依赖对象，供本类逻辑读取和维护。
	 */
	private final InsightService insightService;
	/**
	 * 字段 knowledgeIndexService：保存 knowledgeIndexService
	 * 对应的业务数据、运行配置或依赖对象，供本类逻辑读取和维护。
	 */
	private final KnowledgeIndexService knowledgeIndexService;

	/**
	 * 构造 InsightController 实例。
	 * 
	 * <p>
	 * 实现步骤： 1. 接收调用方传入的依赖对象或初始化参数； 2. 保存到成员字段，保证后续业务方法可以复用； 3. 完成实例初始化。
	 * </p>
	 */
	public InsightController(InsightService insightService, KnowledgeIndexService knowledgeIndexService) {
		this.insightService = insightService;
		this.knowledgeIndexService = knowledgeIndexService;
	}

	@ApiOperationSupport(order = 10, author = "ratel / WenZhang / 18782945613")
	@Operation(summary = "经营与财务概览", description = "作者：ratel；开发人员：WenZhang；联系方式：18782945613。")
	@PreAuthorize("hasAuthority('REPORT_VIEW')")
	@GetMapping("/insights/overview")
	/**
	 * 执行 overview 方法。
	 * 
	 * <p>
	 * 实现步骤： 1. 接收并校验调用方传入的数据； 2. 按当前方法职责执行业务查询、转换或持久化处理； 3. 返回处理结果或更新对象状态。
	 * </p>
	 */
	public ApiResponse<DashboardOverview> overview() {
		return ApiResponse.ok(insightService.overview());
	}

	@ApiOperationSupport(order = 20, author = "ratel / WenZhang / 18782945613")
	@Operation(summary = "智能检索", description = "作者：ratel；开发人员：WenZhang；联系方式：18782945613。")
	@PreAuthorize("hasAuthority('SEARCH_VIEW')")
	@GetMapping("/search")
	/**
	 * 执行 search 方法。
	 * 
	 * <p>
	 * 实现步骤： 1. 接收并校验调用方传入的数据； 2. 按当前方法职责执行业务查询、转换或持久化处理； 3. 返回处理结果或更新对象状态。
	 * </p>
	 */
	public ApiResponse<KnowledgeSearchResponse> search(@RequestParam String keyword,
			@RequestParam(required = false, defaultValue = "hybrid") String mode,
			@RequestParam(required = false, defaultValue = "50") int limit) {
		return ApiResponse.ok(insightService.search(keyword, mode, limit));
	}

	@ApiOperationSupport(order = 30, author = "ratel / WenZhang / 18782945613")
	@Operation(summary = "重建 AI 知识索引", description = "作者：ratel；开发人员：WenZhang；联系方式：18782945613。")
	@PreAuthorize("hasAuthority('SEARCH_VIEW') and hasAuthority('AI_ASSISTANT_USE')")
	@PostMapping("/ai/knowledge/rebuild")
	/**
	 * 执行 rebuildKnowledge 方法。
	 * 
	 * <p>
	 * 实现步骤： 1. 接收并校验调用方传入的数据； 2. 按当前方法职责执行业务查询、转换或持久化处理； 3. 返回处理结果或更新对象状态。
	 * </p>
	 */
	public ApiResponse<KnowledgeRebuildResponse> rebuildKnowledge() {
		return ApiResponse.ok("AI 知识索引已重建", knowledgeIndexService.rebuildAll());
	}
}
