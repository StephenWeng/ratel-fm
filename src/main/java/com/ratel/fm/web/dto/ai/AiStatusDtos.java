package com.ratel.fm.web.dto.ai;

import java.time.OffsetDateTime;
import java.util.List;

/**
 * AI 运行状态相关接口 DTO。
 */
public final class AiStatusDtos {

    private AiStatusDtos() {
    }

    /**
     * AI 组件状态响应。
     *
     * @param modelProvider 当前大模型提供方，支持 ollama 或 qwen
     * @param vectorProvider 当前向量库提供方，支持 h2 或 qdrant
     * @param primaryChatModel 当前普通问答主模型
     * @param embeddingModel 当前知识索引 embedding 模型
     * @param streamEnabled ratel助手是否启用流式输出
     * @param agentEnabled 业务 Agent 是否启用；关闭时前端应隐藏 Agent 入口并避免调用 Agent 接口
     * @param indexDocumentCount 当前知识索引分片数量
     * @param lastRebuildAt 最近一次全量索引成功重建时间
     * @param lastRebuildError 最近一次全量索引失败原因
     * @param checkedAt 本次状态检查时间
     * @param components 组件明细状态
     * @param sourceTypeCounts 按知识来源类型统计的当前索引分片数量
     */
    public record AiComponentStatusResponse(
            String modelProvider,
            String vectorProvider,
            String primaryChatModel,
            String embeddingModel,
            boolean streamEnabled,
            boolean agentEnabled,
            long indexDocumentCount,
            OffsetDateTime lastRebuildAt,
            String lastRebuildError,
            OffsetDateTime checkedAt,
            List<AiComponentStatusItem> components,
            List<AiKnowledgeSourceCount> sourceTypeCounts
    ) {
    }

    /**
     * 单个 AI 组件状态。
     *
     * @param code 组件编码，供前端稳定识别
     * @param name 组件中文名称
     * @param status 状态值，取 UP、DOWN、WARN、DISABLED
     * @param detail 状态说明
     */
    public record AiComponentStatusItem(
            String code,
            String name,
            String status,
            String detail
    ) {
    }

    /**
     * 知识来源类型统计。
     *
     * @param sourceType 知识来源类型
     * @param count 当前索引分片数量
     */
    public record AiKnowledgeSourceCount(
            String sourceType,
            long count
    ) {
    }
}
