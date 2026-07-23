package com.ratel.fm.service.knowledge;

import com.ratel.fm.config.ai.AiProperties;
import org.springframework.stereotype.Service;

/**
 * 智能检索读侧后端路由器。
 *
 * <p>集中判断当前检索读取 H2 还是 Qdrant，以及不同检索模式是否需要关键词和 embedding，避免业务检索代码到处写 provider 判断。</p>
 */
@Service
public class KnowledgeSearchBackendRouter {

    private final AiProperties aiProperties;
    private final QdrantKnowledgeClient qdrantKnowledgeClient;

    public KnowledgeSearchBackendRouter(AiProperties aiProperties, QdrantKnowledgeClient qdrantKnowledgeClient) {
        this.aiProperties = aiProperties;
        this.qdrantKnowledgeClient = qdrantKnowledgeClient;
    }

    /**
     * 当前是否使用 Qdrant 读侧后端。
     */
    public boolean useQdrant() {
        return qdrantKnowledgeClient.selected();
    }

    /**
     * 当前检索模式是否需要关键词评分或 payload 关键词召回。
     */
    public boolean useKeyword(String mode) {
        return !aiProperties.getKnowledge().isEmbeddingEnabled()
                || "keyword".equals(mode)
                || "hybrid".equals(mode);
    }

    /**
     * 当前检索模式是否需要查询 embedding。
     */
    public boolean useEmbedding(String mode) {
        return ("semantic".equals(mode) || "hybrid".equals(mode))
                && (useQdrant() || embeddingScoringEnabled());
    }

    /**
     * 当前后端是否启用向量评分。
     */
    public boolean embeddingScoringEnabled() {
        return aiProperties.getKnowledge().isEmbeddingEnabled() || useQdrant();
    }
}
