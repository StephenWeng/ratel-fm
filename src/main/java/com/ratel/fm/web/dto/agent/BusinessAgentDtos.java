package com.ratel.fm.web.dto.agent;

import java.util.List;

/**
 * 业务 Agent 接口 DTO。
 *
 * <p>用于承载采购、物流、库存、应收应付、财务和审批模块的只读分析结果。</p>
 */
public final class BusinessAgentDtos {

    private BusinessAgentDtos() {
    }

    /**
     * 业务 Agent 请求。
     */
    public record BusinessAgentRequest(
            /**
             * 用户自然语言问题或分析目标。
             */
            String question,
            /**
             * Agent 阶段：readOnly、draft、controlled、multiStep。
             */
            String stage,
            /**
             * 限定模块编码；为空时由服务按问题和权限自动选择。
             */
            List<String> modules,
            /**
             * 限定 Agent 能力类型；为空时由服务按问题自动选择。
             */
            List<String> agentTypes,
            /**
             * 每个模块最多返回的引用数据条数。
             */
            Integer limit
    ) {
    }

    /**
     * 业务 Agent 总响应。
     */
    public record BusinessAgentResponse(
            String question,
            String stage,
            String scope,
            String summary,
            List<BusinessAgentModuleResult> modules,
            List<BusinessAgentCapabilityResult> capabilities,
            List<BusinessAgentAction> actions,
            List<BusinessAgentSelfCheck> selfChecks,
            List<String> risks,
            List<String> suggestions,
            List<String> guardrails
    ) {
    }

    /**
     * 单个业务模块的 Agent 分析结果。
     */
    public record BusinessAgentModuleResult(
            String module,
            String moduleName,
            boolean authorized,
            String summary,
            List<String> findings,
            List<String> risks,
            List<String> suggestions,
            List<BusinessAgentEvidence> evidences
    ) {
    }

    /**
     * 单个 Agent 能力的分析结果。
     */
    public record BusinessAgentCapabilityResult(
            String agentType,
            String agentName,
            boolean available,
            String summary,
            List<String> findings,
            List<String> risks,
            List<String> suggestions,
            List<String> drafts,
            List<BusinessAgentEvidence> evidences
    ) {
    }

    /**
     * Agent 引用的业务证据。
     */
    public record BusinessAgentEvidence(
            String type,
            Long id,
            String no,
            String title,
            String status,
            String amount,
            String date,
            String route
    ) {
    }

    /**
     * Agent 生成的草稿动作或受控执行计划。
     */
    public record BusinessAgentAction(
            String step,
            String stage,
            String module,
            String actionType,
            boolean writeOperation,
            boolean requiresUserConfirm,
            boolean executable,
            String title,
            String description,
            List<String> preconditions,
            List<String> blockedReasons
    ) {
    }

    /**
     * 关键 Agent 自检结果。
     */
    public record BusinessAgentSelfCheck(
            String item,
            boolean passed,
            String level,
            String detail
    ) {
    }
}
