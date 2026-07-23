package com.ratel.fm.repository.knowledge;

import com.ratel.fm.domain.knowledge.LocalKnowledgeDocument;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

/**
 * 本地知识库资料数据访问接口。
 *
 * <p>实现目的：按当前所属公司维护用户上传资料的元数据，实际正文分片写入知识索引表或外部向量库。</p>
 */
public interface LocalKnowledgeDocumentRepository extends JpaRepository<LocalKnowledgeDocument, Long> {

    /**
     * 按所属公司查询本地知识库资料。
     *
     * <p>实现步骤：使用 JWT 中的所属公司编码作为条件，按创建时间倒序返回资料列表，避免跨账套查看上传记录。</p>
     */
    List<LocalKnowledgeDocument> findByOrganizationCodeOrderByCreatedTimeDesc(String organizationCode);
}
