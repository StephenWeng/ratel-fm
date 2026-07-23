package com.ratel.fm.web.knowledge;

import com.ratel.fm.common.ApiResponse;
import com.ratel.fm.service.knowledge.LocalKnowledgeDocumentService;
import com.ratel.fm.web.dto.knowledge.LocalKnowledgeDtos.LocalKnowledgeDocumentView;
import com.github.xiaoymin.knife4j.annotations.ApiOperationSupport;
import com.github.xiaoymin.knife4j.annotations.ApiSupport;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

/**
 * 本地知识库资料接口。
 */
@RestController
@RequestMapping("/api/ai/local-knowledge")
@Tag(name = "本地知识库", description = "上传企业历史文档、资料和图片 OCR 后供 ratel助手检索问答。")
@ApiSupport(author = "ratel / WenZhang / 18782945613")
public class LocalKnowledgeController {

    private final LocalKnowledgeDocumentService service;

    public LocalKnowledgeController(LocalKnowledgeDocumentService service) {
        this.service = service;
    }

    @ApiOperationSupport(order = 1)
    @Operation(summary = "本地知识库资料列表")
    @PreAuthorize("hasAuthority('AI_ASSISTANT_USE')")
    @GetMapping("/documents")
    public ApiResponse<List<LocalKnowledgeDocumentView>> list() {
        return ApiResponse.ok(service.list());
    }

    @ApiOperationSupport(order = 2)
    @Operation(summary = "上传本地资料并后台入库", description = "支持 pdf、docx、xlsx、txt、md、csv 和图片 OCR；上传成功后后台解析和写入索引。")
    @PreAuthorize("hasAuthority('AI_ASSISTANT_USE')")
    @PostMapping("/documents")
    public ApiResponse<LocalKnowledgeDocumentView> upload(
            @RequestParam(required = false) String title,
            @RequestParam(required = false) String description,
            @RequestParam MultipartFile file
    ) {
        return ApiResponse.ok("本地资料已上传，正在后台入库", service.upload(title, description, file));
    }

    @ApiOperationSupport(order = 3)
    @Operation(summary = "重新解析并入库本地资料")
    @PreAuthorize("hasAuthority('AI_ASSISTANT_USE')")
    @PostMapping("/documents/{id}/rebuild")
    public ApiResponse<LocalKnowledgeDocumentView> rebuild(@PathVariable Long id) {
        return ApiResponse.ok("本地资料索引已重建", service.rebuild(id));
    }

    @ApiOperationSupport(order = 4)
    @Operation(summary = "删除本地资料")
    @PreAuthorize("hasAuthority('AI_ASSISTANT_USE')")
    @DeleteMapping("/documents/{id}")
    public ApiResponse<Void> delete(@PathVariable Long id) {
        service.delete(id);
        return ApiResponse.ok(null);
    }
}
