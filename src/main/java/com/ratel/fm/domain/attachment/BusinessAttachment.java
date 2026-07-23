package com.ratel.fm.domain.attachment;

import com.ratel.fm.common.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import org.hibernate.annotations.Comment;

/**
 * 业务附件关联表。
 *
 * <p>通过业务类型、业务记录 ID 和附件 ID 建立多附件关系，满足一条凭证或业务单据关联多个证据文件。</p>
 *
 * <p>开发组织：ratel；开发人员：WenZhang；联系方式：18782945613。</p>
 *
 * @author ratel
 */
@Entity
@Table(name = "fm_business_attachments")
@Comment("业务附件关联表，记录业务类型、业务记录ID和附件ID的多附件关系")
public class BusinessAttachment extends BaseEntity {

    /** 业务模块类型，例如凭证、采购单、物流单、库存流水或应收应付单。 */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 40)
    @Comment("业务模块类型")
    private AttachmentBusinessType businessType;

    /** 业务表主键 ID。 */
    @Column(nullable = false)
    @Comment("业务记录主键ID")
    private Long businessId;

    /** 关联的统一附件文件。 */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "attachment_id", nullable = false)
    @Comment("附件文件主键ID")
    private AttachmentFile attachment;

    /** 附件显示顺序，默认按上传顺序排列。 */
    @Column(nullable = false)
    @Comment("附件显示顺序")
    private Integer sortOrder = 0;

    /**
     * 执行 getBusinessType 方法。
     * 
     * <p>实现步骤：
     * 1. 接收并校验调用方传入的数据；
     * 2. 按当前方法职责执行业务查询、转换或持久化处理；
     * 3. 返回处理结果或更新对象状态。</p>
     */
    public AttachmentBusinessType getBusinessType() {
        return businessType;
    }

    /**
     * 执行 setBusinessType 方法。
     * 
     * <p>实现步骤：
     * 1. 接收并校验调用方传入的数据；
     * 2. 按当前方法职责执行业务查询、转换或持久化处理；
     * 3. 返回处理结果或更新对象状态。</p>
     */
    public void setBusinessType(AttachmentBusinessType businessType) {
        this.businessType = businessType;
    }

    /**
     * 执行 getBusinessId 方法。
     * 
     * <p>实现步骤：
     * 1. 接收并校验调用方传入的数据；
     * 2. 按当前方法职责执行业务查询、转换或持久化处理；
     * 3. 返回处理结果或更新对象状态。</p>
     */
    public Long getBusinessId() {
        return businessId;
    }

    /**
     * 执行 setBusinessId 方法。
     * 
     * <p>实现步骤：
     * 1. 接收并校验调用方传入的数据；
     * 2. 按当前方法职责执行业务查询、转换或持久化处理；
     * 3. 返回处理结果或更新对象状态。</p>
     */
    public void setBusinessId(Long businessId) {
        this.businessId = businessId;
    }

    /**
     * 执行 getAttachment 方法。
     * 
     * <p>实现步骤：
     * 1. 接收并校验调用方传入的数据；
     * 2. 按当前方法职责执行业务查询、转换或持久化处理；
     * 3. 返回处理结果或更新对象状态。</p>
     */
    public AttachmentFile getAttachment() {
        return attachment;
    }

    /**
     * 执行 setAttachment 方法。
     * 
     * <p>实现步骤：
     * 1. 接收并校验调用方传入的数据；
     * 2. 按当前方法职责执行业务查询、转换或持久化处理；
     * 3. 返回处理结果或更新对象状态。</p>
     */
    public void setAttachment(AttachmentFile attachment) {
        this.attachment = attachment;
    }

    /**
     * 执行 getSortOrder 方法。
     * 
     * <p>实现步骤：
     * 1. 接收并校验调用方传入的数据；
     * 2. 按当前方法职责执行业务查询、转换或持久化处理；
     * 3. 返回处理结果或更新对象状态。</p>
     */
    public Integer getSortOrder() {
        return sortOrder;
    }

    /**
     * 执行 setSortOrder 方法。
     * 
     * <p>实现步骤：
     * 1. 接收并校验调用方传入的数据；
     * 2. 按当前方法职责执行业务查询、转换或持久化处理；
     * 3. 返回处理结果或更新对象状态。</p>
     */
    public void setSortOrder(Integer sortOrder) {
        this.sortOrder = sortOrder;
    }
}
