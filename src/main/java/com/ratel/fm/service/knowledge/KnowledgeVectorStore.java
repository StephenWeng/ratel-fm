package com.ratel.fm.service.knowledge;

import com.ratel.fm.domain.knowledge.KnowledgeDocument;
import com.ratel.fm.domain.knowledge.KnowledgeSourceType;

import java.util.List;

/**
 * 知识向量库写入接口。
 *
 * <p>索引服务只表达“替换或删除哪些知识分片”，H2、Qdrant 等存储差异由实现类处理。</p>
 */
public interface KnowledgeVectorStore {

    /**
     * 返回配置使用的向量库 provider 编码。
     */
    String provider();

    /**
     * 判断该向量库是否强制要求 embedding。
     */
    boolean requiresEmbedding();

    /**
     * 全量替换知识分片。
     */
    void replaceAll(List<KnowledgeDocument> documents);

    /**
     * 替换指定来源类型的知识分片。
     */
    void replaceSourceType(KnowledgeSourceType sourceType, List<KnowledgeDocument> documents);

    /**
     * 替换指定业务记录的知识分片。
     */
    void replaceSource(KnowledgeSourceType sourceType, Long sourceId, List<KnowledgeDocument> documents);

    /**
     * 删除指定业务记录的知识分片。
     */
    void deleteSource(KnowledgeSourceType sourceType, Long sourceId);

    /**
     * 统计当前向量库分片数量。
     */
    long count();
}
