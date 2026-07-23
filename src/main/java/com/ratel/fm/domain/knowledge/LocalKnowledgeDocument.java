package com.ratel.fm.domain.knowledge;

import com.ratel.fm.common.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Index;
import jakarta.persistence.Lob;
import jakarta.persistence.Table;
import org.hibernate.annotations.Comment;

/**
 * 用户上传的本地知识库资料。
 */
@Entity
@Table(
        name = "fm_local_knowledge_documents",
        indexes = {
                @Index(name = "idx_fm_local_knowledge_company", columnList = "organization_code"),
                @Index(name = "idx_fm_local_knowledge_status", columnList = "status")
        }
)
@Comment("本地知识库上传资料表")
public class LocalKnowledgeDocument extends BaseEntity {

    @Column(nullable = false, length = 240)
    @Comment("资料标题")
    private String title;

    @Column(length = 500)
    @Comment("资料说明")
    private String description;

    @Column(name = "original_name", nullable = false, length = 300)
    @Comment("原始文件名")
    private String originalName;

    @Column(name = "storage_path", nullable = false, length = 500)
    @Comment("文件相对保存路径")
    private String storagePath;

    @Column(nullable = false, length = 40)
    @Comment("文件后缀")
    private String suffix;

    @Column(name = "content_type", length = 120)
    @Comment("文件 MIME 类型")
    private String contentType;

    @Column(name = "file_size", nullable = false)
    @Comment("文件大小")
    private Long fileSize;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    @Comment("入库状态")
    private LocalKnowledgeDocumentStatus status = LocalKnowledgeDocumentStatus.PENDING;

    @Column(name = "chunk_count")
    @Comment("知识分片数量")
    private Integer chunkCount = 0;

    @Column(name = "ocr_used")
    @Comment("是否使用 OCR")
    private Boolean ocrUsed = false;

    @Column(name = "organization_code", nullable = false, length = 80)
    @Comment("所属公司编码")
    private String organizationCode;

    @Column(name = "uploaded_by", length = 120)
    @Comment("上传人用户名")
    private String uploadedBy;

    @Lob
    @Column(name = "error_message")
    @Comment("失败原因")
    private String errorMessage;

    /**
     * 获取资料标题。
     *
     * @return 资料标题
     */
    public String getTitle() {
        return title;
    }

    /**
     * 设置资料标题。
     *
     * @param title 资料标题
     */
    public void setTitle(String title) {
        this.title = title;
    }

    /**
     * 获取资料说明。
     *
     * @return 资料说明
     */
    public String getDescription() {
        return description;
    }

    /**
     * 设置资料说明。
     *
     * @param description 资料说明
     */
    public void setDescription(String description) {
        this.description = description;
    }

    /**
     * 获取原始文件名。
     *
     * @return 原始文件名
     */
    public String getOriginalName() {
        return originalName;
    }

    /**
     * 设置原始文件名。
     *
     * @param originalName 原始文件名
     */
    public void setOriginalName(String originalName) {
        this.originalName = originalName;
    }

    /**
     * 获取文件相对保存路径。
     *
     * @return 文件相对保存路径
     */
    public String getStoragePath() {
        return storagePath;
    }

    /**
     * 设置文件相对保存路径。
     *
     * @param storagePath 文件相对保存路径
     */
    public void setStoragePath(String storagePath) {
        this.storagePath = storagePath;
    }

    /**
     * 获取文件后缀。
     *
     * @return 文件后缀
     */
    public String getSuffix() {
        return suffix;
    }

    /**
     * 设置文件后缀。
     *
     * @param suffix 文件后缀
     */
    public void setSuffix(String suffix) {
        this.suffix = suffix;
    }

    /**
     * 获取文件 MIME 类型。
     *
     * @return 文件 MIME 类型
     */
    public String getContentType() {
        return contentType;
    }

    /**
     * 设置文件 MIME 类型。
     *
     * @param contentType 文件 MIME 类型
     */
    public void setContentType(String contentType) {
        this.contentType = contentType;
    }

    /**
     * 获取文件大小。
     *
     * @return 文件大小，单位字节
     */
    public Long getFileSize() {
        return fileSize;
    }

    /**
     * 设置文件大小。
     *
     * @param fileSize 文件大小，单位字节
     */
    public void setFileSize(Long fileSize) {
        this.fileSize = fileSize;
    }

    /**
     * 获取入库状态。
     *
     * @return 入库状态
     */
    public LocalKnowledgeDocumentStatus getStatus() {
        return status;
    }

    /**
     * 设置入库状态。
     *
     * @param status 入库状态
     */
    public void setStatus(LocalKnowledgeDocumentStatus status) {
        this.status = status;
    }

    /**
     * 获取知识分片数量。
     *
     * @return 知识分片数量
     */
    public Integer getChunkCount() {
        return chunkCount;
    }

    /**
     * 设置知识分片数量。
     *
     * @param chunkCount 知识分片数量
     */
    public void setChunkCount(Integer chunkCount) {
        this.chunkCount = chunkCount;
    }

    /**
     * 获取是否使用 OCR。
     *
     * @return 是否使用 OCR
     */
    public Boolean getOcrUsed() {
        return ocrUsed;
    }

    /**
     * 设置是否使用 OCR。
     *
     * @param ocrUsed 是否使用 OCR
     */
    public void setOcrUsed(Boolean ocrUsed) {
        this.ocrUsed = ocrUsed;
    }

    /**
     * 获取所属公司编码。
     *
     * @return 所属公司编码
     */
    public String getOrganizationCode() {
        return organizationCode;
    }

    /**
     * 设置所属公司编码。
     *
     * @param organizationCode 所属公司编码
     */
    public void setOrganizationCode(String organizationCode) {
        this.organizationCode = organizationCode;
    }

    /**
     * 获取上传人用户名。
     *
     * @return 上传人用户名
     */
    public String getUploadedBy() {
        return uploadedBy;
    }

    /**
     * 设置上传人用户名。
     *
     * @param uploadedBy 上传人用户名
     */
    public void setUploadedBy(String uploadedBy) {
        this.uploadedBy = uploadedBy;
    }

    /**
     * 获取失败原因。
     *
     * @return 失败原因
     */
    public String getErrorMessage() {
        return errorMessage;
    }

    /**
     * 设置失败原因。
     *
     * @param errorMessage 失败原因
     */
    public void setErrorMessage(String errorMessage) {
        this.errorMessage = errorMessage;
    }
}
