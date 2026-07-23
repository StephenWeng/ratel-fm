package com.ratel.fm.service.knowledge;

import com.ratel.fm.common.BusinessException;
import com.ratel.fm.common.ResponseCode;
import com.ratel.fm.config.ai.AiProperties;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 知识向量库路由器。
 *
 * <p>根据 app.ai.knowledge.vector-database-provider 选择 H2 或 Qdrant，不在业务服务中散落 provider 判断。</p>
 */
@Service
public class KnowledgeVectorStoreRouter {

    /**
     * 字段 aiProperties：读取向量库 provider 配置。
     */
    private final AiProperties aiProperties;
    /**
     * 字段 stores：按 provider 编码索引所有向量库实现。
     */
    private final Map<String, KnowledgeVectorStore> stores;

    /**
     * 构造 KnowledgeVectorStoreRouter 实例。
     */
    public KnowledgeVectorStoreRouter(AiProperties aiProperties, List<KnowledgeVectorStore> stores) {
        this.aiProperties = aiProperties;
        this.stores = stores.stream()
                .collect(Collectors.toUnmodifiableMap(
                        item -> normalizeProvider(item.provider()),
                        item -> item
                ));
    }

    /**
     * 返回当前配置的向量库实现。
     */
    public KnowledgeVectorStore active() {
        String provider = normalizeProvider(aiProperties.getKnowledge().getVectorDatabaseProvider());
        KnowledgeVectorStore store = stores.get(provider);
        if (store == null) {
            throw new BusinessException(HttpStatus.BAD_REQUEST, ResponseCode.FAILED,
                    "未知的向量数据库提供方：" + provider + "，请配置为 h2 或 qdrant。");
        }
        return store;
    }

    /**
     * 判断当前向量库是否强制要求 embedding。
     */
    public boolean requiresEmbedding() {
        return active().requiresEmbedding();
    }

    /**
     * 规范化 provider 配置值。
     */
    private String normalizeProvider(String provider) {
        if (provider == null || provider.isBlank()) {
            return "qdrant";
        }
        return provider.trim().toLowerCase(Locale.ROOT);
    }
}
