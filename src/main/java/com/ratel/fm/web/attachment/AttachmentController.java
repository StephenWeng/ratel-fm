package com.ratel.fm.web.attachment;

import com.github.xiaoymin.knife4j.annotations.ApiOperationSupport;
import com.github.xiaoymin.knife4j.annotations.ApiSupport;
import com.ratel.fm.common.ApiResponse;
import com.ratel.fm.domain.attachment.AttachmentBusinessType;
import com.ratel.fm.service.attachment.AttachmentService;
import com.ratel.fm.web.dto.attachment.AttachmentDtos.AttachmentRenameRequest;
import com.ratel.fm.web.dto.attachment.AttachmentDtos.AttachmentView;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.core.io.Resource;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.nio.charset.StandardCharsets;
import java.util.List;

/**
 * 统一附件管理接口。
 *
 * <p>提供业务附件列表、上传、改名、删除和下载能力；权限按业务类型动态映射到对应模块权限。</p>
 *
 * <p>开发组织：ratel；开发人员：WenZhang；联系方式：18782945613。</p>
 *
 * @author ratel
 */
@Tag(name = "附件管理")
@ApiSupport(order = 90, author = "ratel / WenZhang / 18782945613")
@RestController
@RequestMapping("/api/attachments")
public class AttachmentController {

    /**
     * 字段 attachmentService：保存 attachmentService 对应的业务数据、运行配置或依赖对象，供本类逻辑读取和维护。
     */
    private final AttachmentService attachmentService;

    /**
     * 构造 AttachmentController 实例。
     * 
     * <p>实现步骤：
     * 1. 接收调用方传入的依赖对象或初始化参数；
     * 2. 保存到成员字段，保证后续业务方法可以复用；
     * 3. 完成实例初始化。</p>
     */
    public AttachmentController(AttachmentService attachmentService) {
        this.attachmentService = attachmentService;
    }

    @ApiOperationSupport(order = 10, author = "ratel / WenZhang / 18782945613")
    @Operation(summary = "查询业务附件列表", description = "作者：ratel；开发人员：WenZhang；联系方式：18782945613。")
    @PreAuthorize("@attachmentPermissionEvaluator.canView(#businessType)")
    @GetMapping("/{businessType}/{businessId}")
    /**
     * 执行 list 方法。
     * 
     * <p>实现步骤：
     * 1. 接收并校验调用方传入的数据；
     * 2. 按当前方法职责执行业务查询、转换或持久化处理；
     * 3. 返回处理结果或更新对象状态。</p>
     */
    public ApiResponse<List<AttachmentView>> list(
            @PathVariable AttachmentBusinessType businessType,
            @PathVariable Long businessId
    ) {
        return ApiResponse.ok(attachmentService.list(businessType, businessId));
    }

    @ApiOperationSupport(order = 20, author = "ratel / WenZhang / 18782945613")
    @Operation(summary = "上传业务附件", description = "作者：ratel；开发人员：WenZhang；联系方式：18782945613。支持一次上传多个附件。")
    @PreAuthorize("@attachmentPermissionEvaluator.canManage(#businessType)")
    @PostMapping(value = "/{businessType}/{businessId}", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    /**
     * 执行 upload 方法。
     * 
     * <p>实现步骤：
     * 1. 接收并校验调用方传入的数据；
     * 2. 按当前方法职责执行业务查询、转换或持久化处理；
     * 3. 返回处理结果或更新对象状态。</p>
     */
    public ApiResponse<List<AttachmentView>> upload(
            @PathVariable AttachmentBusinessType businessType,
            @PathVariable Long businessId,
            @RequestParam("files") MultipartFile[] files
    ) {
        return ApiResponse.ok("附件已上传", attachmentService.upload(businessType, businessId, files));
    }

    @ApiOperationSupport(order = 30, author = "ratel / WenZhang / 18782945613")
    @Operation(summary = "修改附件名称", description = "作者：ratel；开发人员：WenZhang；联系方式：18782945613。")
    @PreAuthorize("@attachmentPermissionEvaluator.canManage(#businessType)")
    @PutMapping("/{businessType}/{businessId}/{attachmentId}")
    /**
     * 执行 rename 方法。
     * 
     * <p>实现步骤：
     * 1. 接收并校验调用方传入的数据；
     * 2. 按当前方法职责执行业务查询、转换或持久化处理；
     * 3. 返回处理结果或更新对象状态。</p>
     */
    public ApiResponse<AttachmentView> rename(
            @PathVariable AttachmentBusinessType businessType,
            @PathVariable Long businessId,
            @PathVariable Long attachmentId,
            @Valid @org.springframework.web.bind.annotation.RequestBody AttachmentRenameRequest request
    ) {
        return ApiResponse.ok("附件名称已修改", attachmentService.rename(businessType, businessId, attachmentId, request.displayName()));
    }

    @ApiOperationSupport(order = 40, author = "ratel / WenZhang / 18782945613")
    @Operation(summary = "删除业务附件", description = "作者：ratel；开发人员：WenZhang；联系方式：18782945613。")
    @PreAuthorize("@attachmentPermissionEvaluator.canManage(#businessType)")
    @DeleteMapping("/{businessType}/{businessId}/{attachmentId}")
    /**
     * 执行 delete 方法。
     * 
     * <p>实现步骤：
     * 1. 接收并校验调用方传入的数据；
     * 2. 按当前方法职责执行业务查询、转换或持久化处理；
     * 3. 返回处理结果或更新对象状态。</p>
     */
    public ApiResponse<Void> delete(
            @PathVariable AttachmentBusinessType businessType,
            @PathVariable Long businessId,
            @PathVariable Long attachmentId
    ) {
        attachmentService.delete(businessType, businessId, attachmentId);
        return ApiResponse.ok("附件已删除", null);
    }

    @ApiOperationSupport(order = 50, author = "ratel / WenZhang / 18782945613")
    @Operation(summary = "下载业务附件", description = "作者：ratel；开发人员：WenZhang；联系方式：18782945613。")
    @PreAuthorize("@attachmentPermissionEvaluator.canView(#businessType)")
    @GetMapping("/{businessType}/{businessId}/{attachmentId}/download")
    /**
     * 执行 download 方法。
     * 
     * <p>实现步骤：
     * 1. 接收并校验调用方传入的数据；
     * 2. 按当前方法职责执行业务查询、转换或持久化处理；
     * 3. 返回处理结果或更新对象状态。</p>
     */
    public ResponseEntity<Resource> download(
            @PathVariable AttachmentBusinessType businessType,
            @PathVariable Long businessId,
            @PathVariable Long attachmentId
    ) {
        // 变量说明：download 保存当前步骤计算、查询或转换得到的中间结果。
        AttachmentService.AttachmentDownload download = attachmentService.download(businessType, businessId, attachmentId);
        return ResponseEntity.ok()
                .contentType(contentType(download.contentType()))
                .contentLength(download.fileSize() == null ? -1 : download.fileSize())
                .header(HttpHeaders.CONTENT_DISPOSITION, ContentDisposition.attachment()
                        .filename(download.filename(), StandardCharsets.UTF_8)
                        .build()
                        .toString())
                .body(download.resource());
    }

    @ApiOperationSupport(order = 55, author = "ratel / WenZhang / 18782945613")
    @Operation(summary = "预览业务附件", description = "作者：ratel；开发人员：WenZhang；联系方式：18782945613。返回 inline 文件流，供浏览器和前端轻量 Office 预览使用。")
    @PreAuthorize("@attachmentPermissionEvaluator.canView(#businessType)")
    @GetMapping("/{businessType}/{businessId}/{attachmentId}/preview")
    /**
     * 执行 preview 方法。
     * 
     * <p>实现步骤：
     * 1. 接收并校验调用方传入的数据；
     * 2. 按当前方法职责执行业务查询、转换或持久化处理；
     * 3. 返回处理结果或更新对象状态。</p>
     */
    public ResponseEntity<Resource> preview(
            @PathVariable AttachmentBusinessType businessType,
            @PathVariable Long businessId,
            @PathVariable Long attachmentId
    ) {
        /*
         * 预览接口与下载接口复用附件归属校验和文件读取逻辑。
         * 区别在于 Content-Disposition 使用 inline，浏览器会优先打开图片、PDF 等可预览资源；
         * docx/xlsx 由前端获取二进制后进行小体量解析渲染。
         */
        AttachmentService.AttachmentDownload download = attachmentService.download(businessType, businessId, attachmentId);
        return ResponseEntity.ok()
                .contentType(contentType(download.contentType()))
                .contentLength(download.fileSize() == null ? -1 : download.fileSize())
                .header(HttpHeaders.CONTENT_DISPOSITION, ContentDisposition.inline()
                        .filename(download.filename(), StandardCharsets.UTF_8)
                        .build()
                        .toString())
                .body(download.resource());
    }

    /**
     * 执行 contentType 方法。
     * 
     * <p>实现步骤：
     * 1. 接收并校验调用方传入的数据；
     * 2. 按当前方法职责执行业务查询、转换或持久化处理；
     * 3. 返回处理结果或更新对象状态。</p>
     */
    private MediaType contentType(String contentType) {
        if (contentType == null || contentType.isBlank()) {
            return MediaType.APPLICATION_OCTET_STREAM;
        }
        try {
            return MediaType.parseMediaType(contentType);
        } catch (IllegalArgumentException ex) {
            return MediaType.APPLICATION_OCTET_STREAM;
        }
    }
}
