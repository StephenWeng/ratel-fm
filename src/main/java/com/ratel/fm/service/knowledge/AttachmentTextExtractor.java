package com.ratel.fm.service.knowledge;

import com.ratel.fm.config.attachment.AttachmentStorageProperties;
import com.ratel.fm.domain.attachment.AttachmentFile;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.apache.poi.xwpf.extractor.XWPFWordExtractor;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.apache.poi.xssf.extractor.XSSFExcelExtractor;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Locale;
import java.util.Set;

/**
 * 附件文本抽取服务。
 */
@Service
public class AttachmentTextExtractor {

    /**
     * 附件文本抽取日志对象，用于记录无法解析或读取失败的附件。
     */
    private static final Logger log = LoggerFactory.getLogger(AttachmentTextExtractor.class);
    /**
     * 可直接按文本读取的附件后缀集合，避免二进制文件误按文本解析。
     */
    private static final Set<String> TEXT_SUFFIXES = Set.of("txt", "csv", "log", "json", "xml", "html", "htm", "md");
    /**
     * 文本类附件直接读取的最大字节数，避免超大日志或导出文件进入索引时占用过多内存。
     */
    private static final long MAX_TEXT_BYTES = 2 * 1024 * 1024L;

    /**
     * 附件存储目录配置，用于把数据库中保存的相对路径解析到实际文件。
     */
    private final AttachmentStorageProperties storageProperties;

    /**
     * 构造 AttachmentTextExtractor 实例。
     *
     * <p>实现步骤：保存附件存储配置，后续解析路径时统一限制在附件根目录内。</p>
     */
    public AttachmentTextExtractor(AttachmentStorageProperties storageProperties) {
        this.storageProperties = storageProperties;
    }

    /**
     * 抽取附件可检索文本。
     *
     * <p>实现步骤：
     * 1. 校验附件路径并解析为存储目录内的文件；
     * 2. 按后缀选择 txt/csv/json/xml/html/md、pdf、docx、xlsx 解析器；
     * 3. 不支持或解析失败时返回空字符串，避免索引重建被单个附件中断。</p>
     */
    public String extract(AttachmentFile attachment) {
        if (attachment == null || attachment.getStoragePath() == null || attachment.getStoragePath().isBlank()) {
            return "";
        }
        Path path = resolveStoragePath(attachment.getStoragePath());
        if (!Files.isRegularFile(path)) {
            return "";
        }
        String suffix = attachment.getSuffix() == null ? "" : attachment.getSuffix().toLowerCase(Locale.ROOT);
        try {
            if (TEXT_SUFFIXES.contains(suffix)) {
                return readText(path);
            }
            if ("pdf".equals(suffix)) {
                return readPdf(path);
            }
            if ("docx".equals(suffix)) {
                return readDocx(path);
            }
            if ("xlsx".equals(suffix)) {
                return readXlsx(path);
            }
        } catch (Exception ex) {
            log.warn("附件文本解析失败，attachmentId={}, path={}", attachment.getId(), attachment.getStoragePath(), ex);
        }
        return "";
    }

    /**
     * 按 UTF-8 读取文本类附件。
     *
     * <p>实现步骤：先检查文件大小，超过限制直接跳过，防止超大文本文件进入内存。</p>
     */
    private String readText(Path path) throws Exception {
        if (Files.size(path) > MAX_TEXT_BYTES) {
            return "";
        }
        return Files.readString(path, StandardCharsets.UTF_8);
    }

    /**
     * 读取 PDF 附件文本。
     *
     * <p>实现步骤：使用 PDFBox 打开文档并提取可复制文本；扫描件图片 OCR 不在本地索引阶段处理。</p>
     */
    private String readPdf(Path path) throws Exception {
        try (PDDocument document = Loader.loadPDF(path.toFile())) {
            return new PDFTextStripper().getText(document);
        }
    }

    /**
     * 读取 Word 文档正文。
     *
     * <p>实现步骤：使用 Apache POI 读取 docx 文本内容，并通过 try-with-resources 关闭文件句柄。</p>
     */
    private String readDocx(Path path) throws Exception {
        try (InputStream input = Files.newInputStream(path);
             XWPFDocument document = new XWPFDocument(input);
             XWPFWordExtractor extractor = new XWPFWordExtractor(document)) {
            return extractor.getText();
        }
    }

    /**
     * 读取 Excel 工作簿文本。
     *
     * <p>实现步骤：使用 Apache POI 提取工作表名称和单元格文本，公式保留公式表达式，便于检索表格资料。</p>
     */
    private String readXlsx(Path path) throws Exception {
        try (InputStream input = Files.newInputStream(path);
             XSSFWorkbook workbook = new XSSFWorkbook(input);
             XSSFExcelExtractor extractor = new XSSFExcelExtractor(workbook)) {
            extractor.setFormulasNotResults(false);
            extractor.setIncludeSheetNames(true);
            return extractor.getText();
        }
    }

    /**
     * 解析附件相对存储路径。
     *
     * <p>实现步骤：以附件根目录为基准解析并规范化路径；如果目标路径逃逸根目录，则返回根目录，调用方会因不是文件而跳过。</p>
     */
    private Path resolveStoragePath(String storagePath) {
        Path basePath = Paths.get(storageProperties.getBaseDir()).toAbsolutePath().normalize();
        Path targetPath = basePath.resolve(storagePath).normalize();
        if (!targetPath.startsWith(basePath)) {
            return basePath;
        }
        return targetPath;
    }
}
