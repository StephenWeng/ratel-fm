package com.ratel.fm.web.dto.knowledge;

import java.time.OffsetDateTime;
import java.util.List;

/**
 * KnowledgeDtos 类。
 * 
 * <p>用于承载 KnowledgeDtos 相关的业务数据、接口契约或处理逻辑，便于维护人员快速定位模块职责。</p>
 */
public final class KnowledgeDtos {

    private KnowledgeDtos() {
    }

    /**
     * KnowledgeSearchResult 数据传输记录。
     * 
     * <p>用于承载 KnowledgeSearchResult 相关的业务数据、接口契约或处理逻辑，便于维护人员快速定位模块职责。</p>
     */
    public record KnowledgeSearchResult(
            /**
             * 记录组件 id：表示接口入参或出参中的 id 字段。
             */
            Long id,
            /**
             * 记录组件 type：表示接口入参或出参中的 type 字段。
             */
            String type,
            /**
             * 记录组件 sourceId：表示接口入参或出参中的 sourceId 字段。
             */
            Long sourceId,
            /**
             * 记录组件 sourceNo：表示接口入参或出参中的 sourceNo 字段。
             */
            String sourceNo,
            /**
             * 记录组件 title：表示接口入参或出参中的 title 字段。
             */
            String title,
            /**
             * 记录组件 category：表示接口入参或出参中的 category 字段。
             */
            String category,
            /**
             * 记录组件 summary：表示接口入参或出参中的 summary 字段。
             */
            String summary,
            /**
             * 记录组件 content：表示接口入参或出参中的 content 字段。
             */
            String content,
            /**
             * 记录组件 score：表示接口入参或出参中的 score 字段。
             */
            double score,
            /**
             * 记录组件 routePath：表示接口入参或出参中的 routePath 字段。
             */
            String routePath
    ) {
    }

    /**
     * KnowledgeSearchResponse 数据传输记录。
     * 
     * <p>用于承载 KnowledgeSearchResponse 相关的业务数据、接口契约或处理逻辑，便于维护人员快速定位模块职责。</p>
     */
    public record KnowledgeSearchResponse(
            /**
             * 记录组件 keyword：表示接口入参或出参中的 keyword 字段。
             */
            String keyword,
            /**
             * 记录组件 mode：表示接口入参或出参中的 mode 字段。
             */
            String mode,
            /**
             * 记录组件 aiEnabled：表示接口入参或出参中的 aiEnabled 字段。
             */
            boolean aiEnabled,
            /**
             * 记录组件 total：表示接口入参或出参中的 total 字段。
             */
            int total,
            /**
             * 记录组件 rewrittenQueries：表示接口入参或出参中的 rewrittenQueries 字段。
             */
            List<String> rewrittenQueries,
            /**
             * 记录组件 results：表示接口入参或出参中的 results 字段。
             */
            List<KnowledgeSearchResult> results
    ) {
    }

    /**
     * AiCitation 数据传输记录。
     * 
     * <p>用于承载 AiCitation 相关的业务数据、接口契约或处理逻辑，便于维护人员快速定位模块职责。</p>
     */
    public record AiCitation(
            /**
             * 记录组件 id：表示接口入参或出参中的 id 字段。
             */
            Long id,
            /**
             * 记录组件 type：表示接口入参或出参中的 type 字段。
             */
            String type,
            /**
             * 记录组件 sourceId：表示接口入参或出参中的 sourceId 字段。
             */
            Long sourceId,
            /**
             * 记录组件 sourceNo：表示接口入参或出参中的 sourceNo 字段。
             */
            String sourceNo,
            /**
             * 记录组件 title：表示接口入参或出参中的 title 字段。
             */
            String title,
            /**
             * 记录组件 category：表示接口入参或出参中的 category 字段。
             */
            String category,
            /**
             * 记录组件 summary：表示接口入参或出参中的 summary 字段。
             */
            String summary,
            /**
             * 记录组件 score：表示接口入参或出参中的 score 字段。
             */
            double score,
            /**
             * 记录组件 routePath：表示接口入参或出参中的 routePath 字段。
             */
            String routePath,
            /**
             * 记录组件 url：表示接口入参或出参中的 url 字段。
             */
            String url
    ) {
    }

    /**
     * AiAssistantResponse 数据传输记录。
     * 
     * <p>用于承载 AiAssistantResponse 相关的业务数据、接口契约或处理逻辑，便于维护人员快速定位模块职责。</p>
     */
    public record AiAssistantResponse(
            /**
             * 记录组件 question：表示接口入参或出参中的 question 字段。
             */
            String question,
            /**
             * 记录组件 answer：表示接口入参或出参中的 answer 字段。
             */
            String answer,
            /**
             * 记录组件 aiEnabled：表示接口入参或出参中的 aiEnabled 字段。
             */
            boolean aiEnabled,
            /**
             * 记录组件 model：表示接口入参或出参中的 model 字段。
             */
            String model,
            /**
             * 记录组件 mode：表示接口入参或出参中的 mode 字段。
             */
            String mode,
            /**
             * 记录组件 citations：表示接口入参或出参中的 citations 字段。
             */
            List<AiCitation> citations,
            /**
             * 记录组件 suggestions：表示接口入参或出参中的 suggestions 字段。
             */
            List<String> suggestions,
            /**
             * 记录组件 conversationSummary：表示服务端更新后的会话短摘要。
             */
            String conversationSummary,
            /**
             * 记录组件 recentRawRounds：表示服务端本次实际采用的最近原文轮次。
             */
            int recentRawRounds
    ) {
    }

    /**
     * WebSearchResult 数据传输记录。
     * 
     * <p>用于承载 WebSearchResult 相关的业务数据、接口契约或处理逻辑，便于维护人员快速定位模块职责。</p>
     */
    public record WebSearchResult(
            /**
             * 记录组件 title：表示接口入参或出参中的 title 字段。
             */
            String title,
            /**
             * 记录组件 url：表示接口入参或出参中的 url 字段。
             */
            String url,
            /**
             * 记录组件 summary：表示接口入参或出参中的 summary 字段。
             */
            String summary,
            /**
             * 记录组件 source：表示接口入参或出参中的 source 字段。
             */
            String source,
            /**
             * 记录组件 score：表示接口入参或出参中的 score 字段。
             */
            double score
    ) {
    }

    /**
     * KnowledgeRebuildResponse 数据传输记录。
     * 
     * <p>用于承载 KnowledgeRebuildResponse 相关的业务数据、接口契约或处理逻辑，便于维护人员快速定位模块职责。</p>
     */
    public record KnowledgeRebuildResponse(
            /**
             * 记录组件 documentCount：表示接口入参或出参中的 documentCount 字段。
             */
            long documentCount,
            /**
             * 记录组件 rebuiltAt：表示接口入参或出参中的 rebuiltAt 字段。
             */
            OffsetDateTime rebuiltAt
    ) {
    }
}
