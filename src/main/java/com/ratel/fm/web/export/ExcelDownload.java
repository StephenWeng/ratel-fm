package com.ratel.fm.web.export;

import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;

import java.nio.charset.StandardCharsets;

/**
 * Excel 下载响应工具。
 *
 * <p>负责统一 xlsx 下载响应头，保证中文文件名在浏览器中正常显示。</p>
 *
 * <p>开发组织：ratel；开发人员：WenZhang；联系方式：18782945613。</p>
 *
 * @author ratel
 */
public final class ExcelDownload {

    private ExcelDownload() {
    }

    /**
     * 构建 Excel 下载响应。
     *
     * <p>实现步骤：设置 xlsx 内容类型、附件文件名和二进制内容体。</p>
     */
    public static ResponseEntity<byte[]> response(String filename, byte[] content) {
        // 变量说明：downloadName 保存当前步骤计算、查询或转换得到的中间结果。
        String downloadName = filename.endsWith(".xlsx") ? filename : filename + ".xlsx";
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
                .header(HttpHeaders.CONTENT_DISPOSITION, ContentDisposition.attachment()
                        .filename(downloadName, StandardCharsets.UTF_8)
                        .build()
                        .toString())
                .body(content);
    }
}
