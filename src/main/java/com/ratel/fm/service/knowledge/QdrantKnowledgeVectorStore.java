package com.ratel.fm.service.knowledge;

import com.ratel.fm.domain.knowledge.KnowledgeDocument;
import com.ratel.fm.domain.knowledge.KnowledgeSourceType;
import com.ratel.fm.repository.knowledge.KnowledgeDocumentRepository;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Qdrant 知识向量库实现。
 *
 * <p>Qdrant 模式只把向量分片写入 Qdrant，并同步清理 H2 历史分片，避免同一份知识存在两套索引。</p>
 */
@Component
public class QdrantKnowledgeVectorStore implements KnowledgeVectorStore {

    /**
     * 字段 qdrantKnowledgeClient：负责 Qdrant HTTP API 调用。
     */
    private final QdrantKnowledgeClient qdrantKnowledgeClient;
    /**
     * 字段 knowledgeRepository：Qdrant 模式下仅用于清理 H2 历史分片。
     */
    private final KnowledgeDocumentRepository knowledgeRepository;

    /**
     * 构造 QdrantKnowledgeVectorStore 实例。
     */
    public QdrantKnowledgeVectorStore(QdrantKnowledgeClient qdrantKnowledgeClient, KnowledgeDocumentRepository knowledgeRepository) {
        this.qdrantKnowledgeClient = qdrantKnowledgeClient;
        this.knowledgeRepository = knowledgeRepository;
    }

    @Override
    public String provider() {
        return "qdrant";
    }

    @Override
    public boolean requiresEmbedding() {
        return true;
    }

    @Override
    public void replaceAll(List<KnowledgeDocument> documents) {
        qdrantKnowledgeClient.replaceAll(safeDocuments(documents));
        knowledgeRepository.deleteAllInBatch();
        knowledgeRepository.flush();
    }

    @Override
    public void replaceSourceType(KnowledgeSourceType sourceType, List<KnowledgeDocument> documents) {
        if (sourceType == null) {
            return;
        }
        qdrantKnowledgeClient.deleteBySourceType(sourceType);
        qdrantKnowledgeClient.upsert(safeDocuments(documents));
        knowledgeRepository.deleteBySourceType(sourceType);
    }

    @Override
    public void replaceSource(KnowledgeSourceType sourceType, Long sourceId, List<KnowledgeDocument> documents) {
        if (sourceType == null || sourceId == null) {
            return;
        }
        qdrantKnowledgeClient.deleteBySourceTypeAndSourceId(sourceType, sourceId);
        qdrantKnowledgeClient.upsert(safeDocuments(documents));
        knowledgeRepository.deleteBySourceTypeAndSourceId(sourceType, sourceId);
    }

    @Override
    public void deleteSource(KnowledgeSourceType sourceType, Long sourceId) {
        if (sourceType == null || sourceId == null) {
            return;
        }
        qdrantKnowledgeClient.deleteBySourceTypeAndSourceId(sourceType, sourceId);
        knowledgeRepository.deleteBySourceTypeAndSourceId(sourceType, sourceId);
    }

    @Override
    public long count() {
        return qdrantKnowledgeClient.count();
    }

    /**
     * 把空集合兜底为不可变空列表，简化调用方判断。
     */
    private List<KnowledgeDocument> safeDocuments(List<KnowledgeDocument> documents) {
        return documents == null ? List.of() : documents;
    }
}
