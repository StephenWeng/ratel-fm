package com.ratel.fm.domain.attachment;

import com.ratel.fm.common.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import org.hibernate.annotations.Comment;

/**
 * 统一附件文件表。
 *
 * <p>保存附件原始名称、展示名称、后缀、大小和磁盘存储路径，业务模块只通过关联表引用附件 ID。</p>
 *
 * <p>开发组织：ratel；开发人员：WenZhang；联系方式：18782945613。</p>
 *
 * @author ratel
 */
@Entity
@Table(name = "fm_attachments")
@Comment("统一附件文件表，保存附件名称、后缀、大小、类型和磁盘存储路径")
public class AttachmentFile extends BaseEntity {

    /** 上传时的原始文件名，用于追溯用户提交的附件名称。 */
    @Column(nullable = false, length = 255)
    @Comment("上传时的原始文件名")
    private String originalName;

    /** 页面展示和下载时使用的附件名称，允许后续改名。 */
    @Column(nullable = false, length = 255)
    @Comment("附件展示名称，支持后续改名")
    private String displayName;

    /** 文件后缀，不包含英文句点，便于列表展示和下载命名。 */
    @Column(length = 40)
    @Comment("文件后缀，不包含英文句点")
    private String suffix;

    /** 文件大小，单位字节。 */
    @Column(nullable = false)
    @Comment("文件大小，单位字节")
    private Long fileSize;

    /** 浏览器上传的内容类型，下载时作为响应 Content-Type 参考。 */
    @Column(length = 120)
    @Comment("文件内容类型")
    private String contentType;

    /** 相对 files 目录的存储路径，禁止保存绝对路径，方便整体迁移部署包。 */
    @Column(nullable = false, length = 500)
    @Comment("相对 files 目录的磁盘存储路径")
    private String storagePath;

    /** 上传人员主键，用于审计和后续追溯。 */
    @Column
    @Comment("上传人员主键")
    private Long uploaderId;

    /** 上传人员账号快照，用于列表展示和审计追溯。 */
    @Column(length = 80)
    @Comment("上传人员账号快照")
    private String uploaderUsername;

    /**
     * 执行 getOriginalName 方法。
     * 
     * <p>实现步骤：
     * 1. 接收并校验调用方传入的数据；
     * 2. 按当前方法职责执行业务查询、转换或持久化处理；
     * 3. 返回处理结果或更新对象状态。</p>
     */
    public String getOriginalName() {
        return originalName;
    }

    /**
     * 执行 setOriginalName 方法。
     * 
     * <p>实现步骤：
     * 1. 接收并校验调用方传入的数据；
     * 2. 按当前方法职责执行业务查询、转换或持久化处理；
     * 3. 返回处理结果或更新对象状态。</p>
     */
    public void setOriginalName(String originalName) {
        this.originalName = originalName;
    }

    /**
     * 执行 getDisplayName 方法。
     * 
     * <p>实现步骤：
     * 1. 接收并校验调用方传入的数据；
     * 2. 按当前方法职责执行业务查询、转换或持久化处理；
     * 3. 返回处理结果或更新对象状态。</p>
     */
    public String getDisplayName() {
        return displayName;
    }

    /**
     * 执行 setDisplayName 方法。
     * 
     * <p>实现步骤：
     * 1. 接收并校验调用方传入的数据；
     * 2. 按当前方法职责执行业务查询、转换或持久化处理；
     * 3. 返回处理结果或更新对象状态。</p>
     */
    public void setDisplayName(String displayName) {
        this.displayName = displayName;
    }

    /**
     * 执行 getSuffix 方法。
     * 
     * <p>实现步骤：
     * 1. 接收并校验调用方传入的数据；
     * 2. 按当前方法职责执行业务查询、转换或持久化处理；
     * 3. 返回处理结果或更新对象状态。</p>
     */
    public String getSuffix() {
        return suffix;
    }

    /**
     * 执行 setSuffix 方法。
     * 
     * <p>实现步骤：
     * 1. 接收并校验调用方传入的数据；
     * 2. 按当前方法职责执行业务查询、转换或持久化处理；
     * 3. 返回处理结果或更新对象状态。</p>
     */
    public void setSuffix(String suffix) {
        this.suffix = suffix;
    }

    /**
     * 执行 getFileSize 方法。
     * 
     * <p>实现步骤：
     * 1. 接收并校验调用方传入的数据；
     * 2. 按当前方法职责执行业务查询、转换或持久化处理；
     * 3. 返回处理结果或更新对象状态。</p>
     */
    public Long getFileSize() {
        return fileSize;
    }

    /**
     * 执行 setFileSize 方法。
     * 
     * <p>实现步骤：
     * 1. 接收并校验调用方传入的数据；
     * 2. 按当前方法职责执行业务查询、转换或持久化处理；
     * 3. 返回处理结果或更新对象状态。</p>
     */
    public void setFileSize(Long fileSize) {
        this.fileSize = fileSize;
    }

    /**
     * 执行 getContentType 方法。
     * 
     * <p>实现步骤：
     * 1. 接收并校验调用方传入的数据；
     * 2. 按当前方法职责执行业务查询、转换或持久化处理；
     * 3. 返回处理结果或更新对象状态。</p>
     */
    public String getContentType() {
        return contentType;
    }

    /**
     * 执行 setContentType 方法。
     * 
     * <p>实现步骤：
     * 1. 接收并校验调用方传入的数据；
     * 2. 按当前方法职责执行业务查询、转换或持久化处理；
     * 3. 返回处理结果或更新对象状态。</p>
     */
    public void setContentType(String contentType) {
        this.contentType = contentType;
    }

    /**
     * 执行 getStoragePath 方法。
     * 
     * <p>实现步骤：
     * 1. 接收并校验调用方传入的数据；
     * 2. 按当前方法职责执行业务查询、转换或持久化处理；
     * 3. 返回处理结果或更新对象状态。</p>
     */
    public String getStoragePath() {
        return storagePath;
    }

    /**
     * 执行 setStoragePath 方法。
     * 
     * <p>实现步骤：
     * 1. 接收并校验调用方传入的数据；
     * 2. 按当前方法职责执行业务查询、转换或持久化处理；
     * 3. 返回处理结果或更新对象状态。</p>
     */
    public void setStoragePath(String storagePath) {
        this.storagePath = storagePath;
    }

    /**
     * 执行 getUploaderId 方法。
     * 
     * <p>实现步骤：
     * 1. 接收并校验调用方传入的数据；
     * 2. 按当前方法职责执行业务查询、转换或持久化处理；
     * 3. 返回处理结果或更新对象状态。</p>
     */
    public Long getUploaderId() {
        return uploaderId;
    }

    /**
     * 执行 setUploaderId 方法。
     * 
     * <p>实现步骤：
     * 1. 接收并校验调用方传入的数据；
     * 2. 按当前方法职责执行业务查询、转换或持久化处理；
     * 3. 返回处理结果或更新对象状态。</p>
     */
    public void setUploaderId(Long uploaderId) {
        this.uploaderId = uploaderId;
    }

    /**
     * 执行 getUploaderUsername 方法。
     * 
     * <p>实现步骤：
     * 1. 接收并校验调用方传入的数据；
     * 2. 按当前方法职责执行业务查询、转换或持久化处理；
     * 3. 返回处理结果或更新对象状态。</p>
     */
    public String getUploaderUsername() {
        return uploaderUsername;
    }

    /**
     * 执行 setUploaderUsername 方法。
     * 
     * <p>实现步骤：
     * 1. 接收并校验调用方传入的数据；
     * 2. 按当前方法职责执行业务查询、转换或持久化处理；
     * 3. 返回处理结果或更新对象状态。</p>
     */
    public void setUploaderUsername(String uploaderUsername) {
        this.uploaderUsername = uploaderUsername;
    }
}
