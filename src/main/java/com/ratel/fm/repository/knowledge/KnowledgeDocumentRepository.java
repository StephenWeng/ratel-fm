package com.ratel.fm.repository.knowledge;

import com.ratel.fm.domain.knowledge.KnowledgeDocument;
import com.ratel.fm.domain.knowledge.KnowledgeSourceType;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.Collection;
import java.util.List;

/**
 * AI 知识文档数据访问接口。
 */
public interface KnowledgeDocumentRepository extends JpaRepository<KnowledgeDocument, Long> {

    void deleteBySourceType(KnowledgeSourceType sourceType);

    void deleteBySourceTypeAndSourceId(KnowledgeSourceType sourceType, Long sourceId);

    List<KnowledgeDocument> findBySourceTypeAndSourceIdOrderByChunkIndexAscIdAsc(KnowledgeSourceType sourceType, Long sourceId);

    @Query("""
            select doc from KnowledgeDocument doc
            where doc.permissionCode is null or doc.permissionCode in :permissionCodes
            """)
    List<KnowledgeDocument> findVisible(Collection<com.ratel.fm.domain.auth.PermissionCode> permissionCodes);

    @Query("""
            select doc from KnowledgeDocument doc
            where (doc.permissionCode is null or doc.permissionCode in :permissionCodes)
              and (doc.organizationCode is null or doc.organizationCode = :organizationCode)
            """)
    List<KnowledgeDocument> findVisibleInCompany(
            Collection<com.ratel.fm.domain.auth.PermissionCode> permissionCodes,
            String organizationCode
    );

    @Query("""
            select doc from KnowledgeDocument doc
            where (doc.permissionCode is null or doc.permissionCode in :permissionCodes)
              and (doc.organizationCode is null or doc.organizationCode = :organizationCode)
              and (
                    lower(doc.title) like lower(concat('%', :term, '%'))
                 or lower(coalesce(doc.sourceNo, '')) like lower(concat('%', :term, '%'))
                 or lower(doc.category) like lower(concat('%', :term, '%'))
                 or lower(coalesce(doc.summary, '')) like lower(concat('%', :term, '%'))
                 or lower(doc.content) like lower(concat('%', :term, '%'))
              )
            """)
    List<KnowledgeDocument> findVisibleInCompanyMatchingTerm(
            Collection<com.ratel.fm.domain.auth.PermissionCode> permissionCodes,
            String organizationCode,
            String term,
            Pageable pageable
    );

    @Query("""
            select doc.sourceType as sourceType, count(doc.id) as count
            from KnowledgeDocument doc
            group by doc.sourceType
            """)
    List<SourceTypeCount> countBySourceType();

    /**
     * 知识来源类型统计投影。
     */
    interface SourceTypeCount {
        KnowledgeSourceType getSourceType();

        long getCount();
    }
}
