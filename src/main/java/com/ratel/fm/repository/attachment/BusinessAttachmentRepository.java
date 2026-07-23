package com.ratel.fm.repository.attachment;

import com.ratel.fm.domain.attachment.AttachmentBusinessType;
import com.ratel.fm.domain.attachment.BusinessAttachment;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

/**
 * 业务附件关联数据访问接口。
 *
 * <p>用于按业务记录加载附件清单，以及校验附件是否属于当前业务记录。</p>
 *
 * <p>开发组织：ratel；开发人员：WenZhang；联系方式：18782945613。</p>
 *
 * @author ratel
 */
public interface BusinessAttachmentRepository extends JpaRepository<BusinessAttachment, Long> {

    /**
     * 按业务类型和业务记录查询附件列表，并一次加载附件元数据。
     */
    @EntityGraph(attributePaths = "attachment")
    List<BusinessAttachment> findByBusinessTypeAndBusinessIdOrderBySortOrderAscIdAsc(AttachmentBusinessType businessType, Long businessId);

    /**
     * 查询指定业务记录下的指定附件关系，并加载附件元数据。
     */
    @EntityGraph(attributePaths = "attachment")
    Optional<BusinessAttachment> findByBusinessTypeAndBusinessIdAndAttachment_Id(
            AttachmentBusinessType businessType,
            Long businessId,
            Long attachmentId
    );

    /**
     * 判断附件是否还被其他业务关系引用。
     */
    boolean existsByAttachment_Id(Long attachmentId);

    /**
     * 按附件 ID 查询当前附件绑定的全部业务关系。
     */
    @EntityGraph(attributePaths = "attachment")
    List<BusinessAttachment> findByAttachment_IdOrderBySortOrderAscIdAsc(Long attachmentId);

    /**
     * 统计指定业务记录已绑定的附件数量。
     */
    long countByBusinessTypeAndBusinessId(AttachmentBusinessType businessType, Long businessId);
}
