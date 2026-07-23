package com.ratel.fm.web.dto.attachment;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.time.OffsetDateTime;

/**
 * 附件管理接口 DTO。
 *
 * <p>开发组织：ratel；开发人员：WenZhang；联系方式：18782945613。</p>
 *
 * @author ratel
 */
public final class AttachmentDtos {

    private AttachmentDtos() {
    }

    @Schema(description = "附件改名请求；作者：ratel；开发人员：WenZhang；联系方式：18782945613。")
    /**
     * AttachmentRenameRequest 数据传输记录。
     * 
     * <p>用于承载 AttachmentRenameRequest 相关的业务数据、接口契约或处理逻辑，便于维护人员快速定位模块职责。</p>
     */
    public record AttachmentRenameRequest(
            @Schema(description = "附件展示名称")
            @NotBlank(message = "附件名称不能为空")
            @Size(max = 255, message = "附件名称不能超过255个字符")
            /**
             * 记录组件 displayName：表示接口入参或出参中的 displayName 字段。
             */
            String displayName
    ) {
    }

    @Schema(description = "附件列表视图；作者：ratel；开发人员：WenZhang；联系方式：18782945613。")
    /**
     * AttachmentView 数据传输记录。
     * 
     * <p>用于承载 AttachmentView 相关的业务数据、接口契约或处理逻辑，便于维护人员快速定位模块职责。</p>
     */
    public record AttachmentView(
            @Schema(description = "附件ID")
            /**
             * 记录组件 id：表示接口入参或出参中的 id 字段。
             */
            Long id,
            @Schema(description = "上传原始文件名")
            /**
             * 记录组件 originalName：表示接口入参或出参中的 originalName 字段。
             */
            String originalName,
            @Schema(description = "附件展示名称")
            /**
             * 记录组件 displayName：表示接口入参或出参中的 displayName 字段。
             */
            String displayName,
            @Schema(description = "文件后缀")
            /**
             * 记录组件 suffix：表示接口入参或出参中的 suffix 字段。
             */
            String suffix,
            @Schema(description = "文件大小，单位字节")
            /**
             * 记录组件 fileSize：表示接口入参或出参中的 fileSize 字段。
             */
            Long fileSize,
            @Schema(description = "文件内容类型")
            /**
             * 记录组件 contentType：表示接口入参或出参中的 contentType 字段。
             */
            String contentType,
            @Schema(description = "上传人员账号")
            /**
             * 记录组件 uploaderUsername：表示接口入参或出参中的 uploaderUsername 字段。
             */
            String uploaderUsername,
            @Schema(description = "上传时间")
            /**
             * 记录组件 createdTime：表示接口入参或出参中的 createdTime 字段。
             */
            OffsetDateTime createdTime
    ) {
    }
}
