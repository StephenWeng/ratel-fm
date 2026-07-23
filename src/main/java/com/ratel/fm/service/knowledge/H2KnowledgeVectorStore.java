package com.ratel.fm.service.knowledge;

import com.ratel.fm.domain.knowledge.KnowledgeDocument;
import com.ratel.fm.domain.knowledge.KnowledgeSourceType;
import com.ratel.fm.repository.knowledge.KnowledgeDocumentRepository;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * H2 知识向量库实现。
 *
 * <p>使用 fm_knowledge_documents 表保存分片、关键词和可选 embedding，适合便携包单机部署。</p>
 */
@Component
public class H2KnowledgeVectorStore implements KnowledgeVectorStore {

    /**
     * 字段 knowledgeRepository：保存 H2 知识分片。
     */
    private final KnowledgeDocumentRepository knowledgeRepository;

    /**
     * 构造 H2KnowledgeVectorStore 实例。
     */
    public H2KnowledgeVectorStore(KnowledgeDocumentRepository knowledgeRepository) {
        this.knowledgeRepository = knowledgeRepository;
    }

    @Override
    public String provider() {
        return "h2";
    }

    @Override
    public boolean requiresEmbedding() {
        return false;
    }

    @Override
    public void replaceAll(List<KnowledgeDocument> documents) {
        knowledgeRepository.deleteAllInBatch();
        knowledgeRepository.flush();
        knowledgeRepository.saveAll(safeDocuments(documents));
    }

    @Override
    public void replaceSourceType(KnowledgeSourceType sourceType, List<KnowledgeDocument> documents) {
        if (sourceType == null) {
            return;
        }
        knowledgeRepository.deleteBySourceType(sourceType);
        knowledgeRepository.saveAll(safeDocuments(documents));
    }

    @Override
    public void replaceSource(KnowledgeSourceType sourceType, Long sourceId, List<KnowledgeDocument> documents) {
        if (sourceType == null || sourceId == null) {
            return;
        }
        knowledgeRepository.deleteBySourceTypeAndSourceId(sourceType, sourceId);
        knowledgeRepository.saveAll(safeDocuments(documents));
    }

    @Override
    public void deleteSource(KnowledgeSourceType sourceType, Long sourceId) {
        if (sourceType == null || sourceId == null) {
            return;
        }
        knowledgeRepository.deleteBySourceTypeAndSourceId(sourceType, sourceId);
    }

    @Override
    public long count() {
        return knowledgeRepository.count();
    }

    /**
     * 把空集合兜底为不可变空列表，简化调用方判断。
     */
    private List<KnowledgeDocument> safeDocuments(List<KnowledgeDocument> documents) {
        return documents == null ? List.of() : documents;
    }
}
