package com.ratel.fm.domain.knowledge;

import com.ratel.fm.common.BaseEntity;
import com.ratel.fm.domain.auth.PermissionCode;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Index;
import jakarta.persistence.Lob;
import jakarta.persistence.Table;
import org.hibernate.annotations.Comment;

/**
 * AI 知识检索文档分片。
 */
@Entity
@Table(
        name = "fm_knowledge_documents",
        indexes = {
                @Index(name = "idx_fm_knowledge_source", columnList = "source_type,source_id"),
                @Index(name = "idx_fm_knowledge_permission", columnList = "permission_code"),
                @Index(name = "idx_fm_knowledge_hash", columnList = "content_hash")
        }
)
@Comment("AI知识检索文档分片表，保存业务数据和附件文本的可检索内容与向量")
/**
 * KnowledgeDocument 类。
 * 
 * <p>用于承载 KnowledgeDocument 相关的业务数据、接口契约或处理逻辑，便于维护人员快速定位模块职责。</p>
 */
public class KnowledgeDocument extends BaseEntity {

    @Enumerated(EnumType.STRING)
    @Column(name = "source_type", nullable = false, length = 40)
    @Comment("知识来源类型")
    /**
     * 字段 sourceType：保存 sourceType 对应的业务数据、运行配置或依赖对象，供本类逻辑读取和维护。
     */
    private KnowledgeSourceType sourceType;

    @Column(name = "source_id")
    @Comment("来源业务记录ID")
    /**
     * 字段 sourceId：保存 sourceId 对应的业务数据、运行配置或依赖对象，供本类逻辑读取和维护。
     */
    private Long sourceId;

    @Column(name = "source_no", length = 120)
    @Comment("来源业务编号")
    /**
     * 字段 sourceNo：保存 sourceNo 对应的业务数据、运行配置或依赖对象，供本类逻辑读取和维护。
     */
    private String sourceNo;

    @Column(nullable = false, length = 300)
    @Comment("知识标题")
    /**
     * 字段 title：保存 title 对应的业务数据、运行配置或依赖对象，供本类逻辑读取和维护。
     */
    private String title;

    @Column(nullable = false, length = 120)
    @Comment("业务显示分类")
    /**
     * 字段 category：保存 category 对应的业务数据、运行配置或依赖对象，供本类逻辑读取和维护。
     */
    private String category;

    @Column(nullable = false, length = 4000)
    @Comment("知识文本分片内容")
    /**
     * 字段 content：保存 content 对应的业务数据、运行配置或依赖对象，供本类逻辑读取和维护。
     */
    private String content;

    @Column(length = 2000)
    @Comment("检索摘要")
    /**
     * 字段 summary：保存 summary 对应的业务数据、运行配置或依赖对象，供本类逻辑读取和维护。
     */
    private String summary;

    @Column(length = 1000)
    @Comment("来源元数据JSON")
    /**
     * 字段 metadata：保存 metadata 对应的业务数据、运行配置或依赖对象，供本类逻辑读取和维护。
     */
    private String metadata;

    @Enumerated(EnumType.STRING)
    @Column(name = "permission_code", length = 80)
    @Comment("访问该知识所需权限码")
    /**
     * 字段 permissionCode：保存 permissionCode 对应的业务数据、运行配置或依赖对象，供本类逻辑读取和维护。
     */
    private PermissionCode permissionCode;

    @Column(name = "organization_code", length = 80)
    @Comment("所属公司字典编码，作为AI知识检索账套隔离标识")
    /**
     * 字段 organizationCode：保存所属公司字典编码，用于AI知识文档按账套隔离检索。
     */
    private String organizationCode;

    @Column(name = "content_hash", nullable = false, length = 64)
    @Comment("内容哈希，用于判断是否需要重建向量")
    /**
     * 字段 contentHash：保存 contentHash 对应的业务数据、运行配置或依赖对象，供本类逻辑读取和维护。
     */
    private String contentHash;

    @Lob
    @Column(name = "embedding_json")
    @Comment("向量JSON数组")
    /**
     * 字段 embeddingJson：保存 embeddingJson 对应的业务数据、运行配置或依赖对象，供本类逻辑读取和维护。
     */
    private String embeddingJson;

    @Column(name = "embedding_model", length = 80)
    @Comment("向量模型名称")
    /**
     * 字段 embeddingModel：保存 embeddingModel 对应的业务数据、运行配置或依赖对象，供本类逻辑读取和维护。
     */
    private String embeddingModel;

    @Column(name = "chunk_index")
    @Comment("来源内容分片序号")
    /**
     * 字段 chunkIndex：保存 chunkIndex 对应的业务数据、运行配置或依赖对象，供本类逻辑读取和维护。
     */
    private Integer chunkIndex = 0;

    /**
     * 执行 getSourceType 方法。
     * 
     * <p>实现步骤：
     * 1. 接收并校验调用方传入的数据；
     * 2. 按当前方法职责执行业务查询、转换或持久化处理；
     * 3. 返回处理结果或更新对象状态。</p>
     */
    public KnowledgeSourceType getSourceType() {
        return sourceType;
    }

    /**
     * 执行 setSourceType 方法。
     * 
     * <p>实现步骤：
     * 1. 接收并校验调用方传入的数据；
     * 2. 按当前方法职责执行业务查询、转换或持久化处理；
     * 3. 返回处理结果或更新对象状态。</p>
     */
    public void setSourceType(KnowledgeSourceType sourceType) {
        this.sourceType = sourceType;
    }

    /**
     * 执行 getSourceId 方法。
     * 
     * <p>实现步骤：
     * 1. 接收并校验调用方传入的数据；
     * 2. 按当前方法职责执行业务查询、转换或持久化处理；
     * 3. 返回处理结果或更新对象状态。</p>
     */
    public Long getSourceId() {
        return sourceId;
    }

    /**
     * 执行 setSourceId 方法。
     * 
     * <p>实现步骤：
     * 1. 接收并校验调用方传入的数据；
     * 2. 按当前方法职责执行业务查询、转换或持久化处理；
     * 3. 返回处理结果或更新对象状态。</p>
     */
    public void setSourceId(Long sourceId) {
        this.sourceId = sourceId;
    }

    /**
     * 执行 getSourceNo 方法。
     * 
     * <p>实现步骤：
     * 1. 接收并校验调用方传入的数据；
     * 2. 按当前方法职责执行业务查询、转换或持久化处理；
     * 3. 返回处理结果或更新对象状态。</p>
     */
    public String getSourceNo() {
        return sourceNo;
    }

    /**
     * 执行 setSourceNo 方法。
     * 
     * <p>实现步骤：
     * 1. 接收并校验调用方传入的数据；
     * 2. 按当前方法职责执行业务查询、转换或持久化处理；
     * 3. 返回处理结果或更新对象状态。</p>
     */
    public void setSourceNo(String sourceNo) {
        this.sourceNo = sourceNo;
    }

    /**
     * 执行 getTitle 方法。
     * 
     * <p>实现步骤：
     * 1. 接收并校验调用方传入的数据；
     * 2. 按当前方法职责执行业务查询、转换或持久化处理；
     * 3. 返回处理结果或更新对象状态。</p>
     */
    public String getTitle() {
        return title;
    }

    /**
     * 执行 setTitle 方法。
     * 
     * <p>实现步骤：
     * 1. 接收并校验调用方传入的数据；
     * 2. 按当前方法职责执行业务查询、转换或持久化处理；
     * 3. 返回处理结果或更新对象状态。</p>
     */
    public void setTitle(String title) {
        this.title = title;
    }

    /**
     * 执行 getCategory 方法。
     * 
     * <p>实现步骤：
     * 1. 接收并校验调用方传入的数据；
     * 2. 按当前方法职责执行业务查询、转换或持久化处理；
     * 3. 返回处理结果或更新对象状态。</p>
     */
    public String getCategory() {
        return category;
    }

    /**
     * 执行 setCategory 方法。
     * 
     * <p>实现步骤：
     * 1. 接收并校验调用方传入的数据；
     * 2. 按当前方法职责执行业务查询、转换或持久化处理；
     * 3. 返回处理结果或更新对象状态。</p>
     */
    public void setCategory(String category) {
        this.category = category;
    }

    /**
     * 执行 getContent 方法。
     * 
     * <p>实现步骤：
     * 1. 接收并校验调用方传入的数据；
     * 2. 按当前方法职责执行业务查询、转换或持久化处理；
     * 3. 返回处理结果或更新对象状态。</p>
     */
    public String getContent() {
        return content;
    }

    /**
     * 执行 setContent 方法。
     * 
     * <p>实现步骤：
     * 1. 接收并校验调用方传入的数据；
     * 2. 按当前方法职责执行业务查询、转换或持久化处理；
     * 3. 返回处理结果或更新对象状态。</p>
     */
    public void setContent(String content) {
        this.content = content;
    }

    /**
     * 执行 getSummary 方法。
     * 
     * <p>实现步骤：
     * 1. 接收并校验调用方传入的数据；
     * 2. 按当前方法职责执行业务查询、转换或持久化处理；
     * 3. 返回处理结果或更新对象状态。</p>
     */
    public String getSummary() {
        return summary;
    }

    /**
     * 执行 setSummary 方法。
     * 
     * <p>实现步骤：
     * 1. 接收并校验调用方传入的数据；
     * 2. 按当前方法职责执行业务查询、转换或持久化处理；
     * 3. 返回处理结果或更新对象状态。</p>
     */
    public void setSummary(String summary) {
        this.summary = summary;
    }

    /**
     * 执行 getMetadata 方法。
     * 
     * <p>实现步骤：
     * 1. 接收并校验调用方传入的数据；
     * 2. 按当前方法职责执行业务查询、转换或持久化处理；
     * 3. 返回处理结果或更新对象状态。</p>
     */
    public String getMetadata() {
        return metadata;
    }

    /**
     * 执行 setMetadata 方法。
     * 
     * <p>实现步骤：
     * 1. 接收并校验调用方传入的数据；
     * 2. 按当前方法职责执行业务查询、转换或持久化处理；
     * 3. 返回处理结果或更新对象状态。</p>
     */
    public void setMetadata(String metadata) {
        this.metadata = metadata;
    }

    /**
     * 执行 getPermissionCode 方法。
     * 
     * <p>实现步骤：
     * 1. 接收并校验调用方传入的数据；
     * 2. 按当前方法职责执行业务查询、转换或持久化处理；
     * 3. 返回处理结果或更新对象状态。</p>
     */
    public PermissionCode getPermissionCode() {
        return permissionCode;
    }

    /**
     * 执行 setPermissionCode 方法。
     * 
     * <p>实现步骤：
     * 1. 接收并校验调用方传入的数据；
     * 2. 按当前方法职责执行业务查询、转换或持久化处理；
     * 3. 返回处理结果或更新对象状态。</p>
     */
    public void setPermissionCode(PermissionCode permissionCode) {
        this.permissionCode = permissionCode;
    }

    /**
     * 执行 getOrganizationCode 方法。
     * 
     * <p>实现步骤：
     * 1. 接收并校验调用方传入的数据；
     * 2. 按当前方法职责执行业务查询、转换或持久化处理；
     * 3. 返回处理结果或更新对象状态。</p>
     */
    public String getOrganizationCode() {
        return organizationCode;
    }

    /**
     * 执行 setOrganizationCode 方法。
     * 
     * <p>实现步骤：
     * 1. 接收并校验调用方传入的数据；
     * 2. 按当前方法职责执行业务查询、转换或持久化处理；
     * 3. 返回处理结果或更新对象状态。</p>
     */
    public void setOrganizationCode(String organizationCode) {
        this.organizationCode = organizationCode;
    }

    /**
     * 执行 getContentHash 方法。
     * 
     * <p>实现步骤：
     * 1. 接收并校验调用方传入的数据；
     * 2. 按当前方法职责执行业务查询、转换或持久化处理；
     * 3. 返回处理结果或更新对象状态。</p>
     */
    public String getContentHash() {
        return contentHash;
    }

    /**
     * 执行 setContentHash 方法。
     * 
     * <p>实现步骤：
     * 1. 接收并校验调用方传入的数据；
     * 2. 按当前方法职责执行业务查询、转换或持久化处理；
     * 3. 返回处理结果或更新对象状态。</p>
     */
    public void setContentHash(String contentHash) {
        this.contentHash = contentHash;
    }

    /**
     * 执行 getEmbeddingJson 方法。
     * 
     * <p>实现步骤：
     * 1. 接收并校验调用方传入的数据；
     * 2. 按当前方法职责执行业务查询、转换或持久化处理；
     * 3. 返回处理结果或更新对象状态。</p>
     */
    public String getEmbeddingJson() {
        return embeddingJson;
    }

    /**
     * 执行 setEmbeddingJson 方法。
     * 
     * <p>实现步骤：
     * 1. 接收并校验调用方传入的数据；
     * 2. 按当前方法职责执行业务查询、转换或持久化处理；
     * 3. 返回处理结果或更新对象状态。</p>
     */
    public void setEmbeddingJson(String embeddingJson) {
        this.embeddingJson = embeddingJson;
    }

    /**
     * 执行 getEmbeddingModel 方法。
     * 
     * <p>实现步骤：
     * 1. 接收并校验调用方传入的数据；
     * 2. 按当前方法职责执行业务查询、转换或持久化处理；
     * 3. 返回处理结果或更新对象状态。</p>
     */
    public String getEmbeddingModel() {
        return embeddingModel;
    }

    /**
     * 执行 setEmbeddingModel 方法。
     * 
     * <p>实现步骤：
     * 1. 接收并校验调用方传入的数据；
     * 2. 按当前方法职责执行业务查询、转换或持久化处理；
     * 3. 返回处理结果或更新对象状态。</p>
     */
    public void setEmbeddingModel(String embeddingModel) {
        this.embeddingModel = embeddingModel;
    }

    /**
     * 执行 getChunkIndex 方法。
     * 
     * <p>实现步骤：
     * 1. 接收并校验调用方传入的数据；
     * 2. 按当前方法职责执行业务查询、转换或持久化处理；
     * 3. 返回处理结果或更新对象状态。</p>
     */
    public Integer getChunkIndex() {
        return chunkIndex;
    }

    /**
     * 执行 setChunkIndex 方法。
     * 
     * <p>实现步骤：
     * 1. 接收并校验调用方传入的数据；
     * 2. 按当前方法职责执行业务查询、转换或持久化处理；
     * 3. 返回处理结果或更新对象状态。</p>
     */
    public void setChunkIndex(Integer chunkIndex) {
        this.chunkIndex = chunkIndex;
    }
}
