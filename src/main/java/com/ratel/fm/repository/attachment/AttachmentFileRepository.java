package com.ratel.fm.repository.attachment;

import com.ratel.fm.domain.attachment.AttachmentFile;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * 统一附件文件数据访问接口。
 *
 * <p>用于附件元数据的新增、改名、下载查询和删除。</p>
 *
 * <p>开发组织：ratel；开发人员：WenZhang；联系方式：18782945613。</p>
 *
 * @author ratel
 */
public interface AttachmentFileRepository extends JpaRepository<AttachmentFile, Long> {
}
