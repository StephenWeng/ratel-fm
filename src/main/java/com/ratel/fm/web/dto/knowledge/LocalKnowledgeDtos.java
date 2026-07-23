package com.ratel.fm.web.dto.knowledge;

import java.time.OffsetDateTime;

/**
 * 本地知识库接口 DTO。
 *
 * <p>实现目的：集中维护本地知识库上传、列表和状态展示需要的响应结构，避免 Controller 直接暴露实体字段。</p>
 */
public final class LocalKnowledgeDtos {

    /**
     * 私有构造方法。
     *
     * <p>实现步骤：禁止工具类被实例化，所有 DTO 通过内部 record 声明。</p>
     */
    private LocalKnowledgeDtos() {
    }

    /**
     * 本地知识库资料展示 DTO。
     *
     * @param id 资料主键
     * @param title 用户维护的资料标题，未填写时取原始文件名
     * @param description 资料说明，用于辅助检索和列表展示
     * @param originalName 上传时的原始文件名
     * @param suffix 文件后缀，用于判断解析方式和前端展示
     * @param fileSize 文件大小，单位字节
     * @param status 入库状态，取 PENDING、INDEXING、INDEXED、FAILED
     * @param chunkCount 已写入知识索引的分片数量
     * @param ocrUsed 是否使用图片 OCR 或视觉模型识别
     * @param uploadedBy 上传人用户名快照
     * @param organizationCode 所属公司编码，用于账套隔离展示和排查
     * @param errorMessage 入库失败时的用户可读原因
     * @param createdTime 上传记录创建时间
     * @param modifyTime 上传记录最近更新时间
     */
    public record LocalKnowledgeDocumentView(
            Long id,
            String title,
            String description,
            String originalName,
            String suffix,
            Long fileSize,
            String status,
            Integer chunkCount,
            Boolean ocrUsed,
            String uploadedBy,
            String organizationCode,
            String errorMessage,
            OffsetDateTime createdTime,
            OffsetDateTime modifyTime
    ) {
    }
}
