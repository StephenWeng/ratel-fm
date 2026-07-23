package com.ratel.fm.service.knowledge;

import com.alibaba.fastjson2.JSONObject;
import com.ratel.fm.common.BusinessException;
import com.ratel.fm.common.concurrent.NamedDaemonThreadFactory;
import com.ratel.fm.common.ResponseCode;
import com.ratel.fm.config.ai.AiProperties;
import com.ratel.fm.config.attachment.AttachmentStorageProperties;
import com.ratel.fm.domain.attachment.AttachmentFile;
import com.ratel.fm.domain.auth.PermissionCode;
import com.ratel.fm.domain.knowledge.KnowledgeDocument;
import com.ratel.fm.domain.knowledge.KnowledgeSourceType;
import com.ratel.fm.domain.knowledge.LocalKnowledgeDocument;
import com.ratel.fm.domain.knowledge.LocalKnowledgeDocumentStatus;
import com.ratel.fm.repository.knowledge.LocalKnowledgeDocumentRepository;
import com.ratel.fm.security.CompanyScope;
import com.ratel.fm.security.CurrentUser;
import com.ratel.fm.security.SecurityUtils;
import com.ratel.fm.service.ai.OllamaClient;
import com.ratel.fm.service.ai.AiOcrService;
import com.ratel.fm.service.ai.QwenClient.VisionInput;
import com.ratel.fm.web.dto.knowledge.LocalKnowledgeDtos.LocalKnowledgeDocumentView;
import org.apache.commons.io.FilenameUtils;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.web.multipart.MultipartFile;

import jakarta.annotation.PreDestroy;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.Base64;
import java.util.HexFormat;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * 本地知识库资料上传、解析和入库服务。
 *
 * <p>实现目的：把用户上传的历史资料、制度文件、图片扫描件等转换为统一知识分片，
 * 并写入当前启用的知识向量库，供智能检索和 ratel 助手按账套、权限引用。</p>
 */
@Service
public class LocalKnowledgeDocumentService {

    /** 本地知识库支持的文件后缀，文本类走解析器，图片类走 OCR 策略服务。 */
    private static final Set<String> SUPPORTED_SUFFIXES = Set.of(
            "txt", "md", "csv", "json", "xml", "html", "htm", "log",
            "pdf", "docx", "xlsx",
            "png", "jpg", "jpeg", "webp", "bmp"
    );
    /** 需要调用 OCR/视觉模型的图片后缀集合。 */
    private static final Set<String> IMAGE_SUFFIXES = Set.of("png", "jpg", "jpeg", "webp", "bmp");

    /** 本地知识库资料元数据仓库，只保存上传记录和入库状态。 */
    private final LocalKnowledgeDocumentRepository repository;
    /** 附件文本抽取器，复用业务附件的 PDF、Office 和文本解析能力。 */
    private final AttachmentTextExtractor textExtractor;
    /** 附件存储配置，决定本地知识库文件保存到部署包 files 目录下的位置。 */
    private final AttachmentStorageProperties storageProperties;
    /** OCR 策略服务，按本地视觉模型优先、千问回退的顺序识别图片。 */
    private final AiOcrService aiOcrService;
    /** Ollama 客户端，用于本地 embedding 模型检查和向量生成。 */
    private final OllamaClient ollamaClient;
    /** 知识向量库路由器，根据配置写入 Qdrant 或内置 H2 向量库。 */
    private final KnowledgeVectorStoreRouter vectorStoreRouter;
    /** AI 配置项，用于读取知识分片大小、重叠长度和 embedding 开关。 */
    private final AiProperties aiProperties;
    /** 后台入库事务模板，保证上传提交后再解析、OCR 和写入向量库。 */
    private final TransactionTemplate transactionTemplate;
    /** 本地知识库后台入库线程池，避免图片 OCR 和 embedding 占用上传请求线程。 */
    private final ExecutorService indexExecutor = Executors.newSingleThreadExecutor(
            new NamedDaemonThreadFactory("local-knowledge-index-")
    );

    public LocalKnowledgeDocumentService(
            LocalKnowledgeDocumentRepository repository,
            AttachmentTextExtractor textExtractor,
            AttachmentStorageProperties storageProperties,
            AiOcrService aiOcrService,
            OllamaClient ollamaClient,
            KnowledgeVectorStoreRouter vectorStoreRouter,
            AiProperties aiProperties,
            TransactionTemplate transactionTemplate
    ) {
        this.repository = repository;
        this.textExtractor = textExtractor;
        this.storageProperties = storageProperties;
        this.aiOcrService = aiOcrService;
        this.ollamaClient = ollamaClient;
        this.vectorStoreRouter = vectorStoreRouter;
        this.aiProperties = aiProperties;
        this.transactionTemplate = transactionTemplate;
    }

    @Transactional(readOnly = true)
    public List<LocalKnowledgeDocumentView> list() {
        return repository.findByOrganizationCodeOrderByCreatedTimeDesc(CompanyScope.currentCompanyCode())
                .stream()
                .map(this::toView)
                .toList();
    }

    /**
     * 上传本地知识库资料并立即入库。
     *
     * <p>实现步骤：
     * 1. 校验文件存在和后缀是否受支持；
     * 2. 生成资料元数据并按当前所属公司保存源文件；
     * 3. 保存上传记录后注册提交后后台入库任务；
     * 4. 立即返回待入库资料状态，失败原因由后台任务写回。</p>
     */
    @Transactional
    public LocalKnowledgeDocumentView upload(String title, String description, MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new BusinessException("请选择要上传的资料文件。");
        }
        String originalName = safeOriginalName(file.getOriginalFilename());
        String suffix = FilenameUtils.getExtension(originalName).toLowerCase(Locale.ROOT);
        if (!SUPPORTED_SUFFIXES.contains(suffix)) {
            throw new BusinessException(HttpStatus.BAD_REQUEST, ResponseCode.ILLEGAL_PARAM,
                    "暂不支持该文件类型，请上传 pdf、docx、xlsx、txt、md、csv 或图片文件。");
        }
        LocalKnowledgeDocument document = new LocalKnowledgeDocument();
        document.setTitle(blankToDefault(title, FilenameUtils.getBaseName(originalName)));
        document.setDescription(truncate(description, 500));
        document.setOriginalName(originalName);
        document.setSuffix(suffix);
        document.setContentType(file.getContentType());
        document.setFileSize(file.getSize());
        document.setOrganizationCode(CompanyScope.currentCompanyCode());
        CurrentUser user = SecurityUtils.currentUser();
        document.setUploadedBy(user.username());
        document.setStatus(LocalKnowledgeDocumentStatus.PENDING);
        document.setStoragePath(store(file, suffix));
        document = repository.saveAndFlush(document);
        enqueueRebuild(document.getId(), document.getOrganizationCode());
        return toView(document);
    }

    /**
     * 重建单个本地知识库资料索引。
     *
     * <p>实现步骤：
     * 1. 校验资料属于当前所属公司并标记为 INDEXING；
     * 2. 根据文件类型执行文本抽取或 OCR 策略识别；
     * 3. 将正文按知识库配置切片并补齐权限、账套和元数据；
     * 4. 使用 replaceSource 原子替换该资料旧分片；
     * 5. 成功时记录分片数，失败时清理旧分片并写入失败原因。</p>
     */
    @Transactional
    public LocalKnowledgeDocumentView rebuild(Long id) {
        return rebuildForCompany(id, CompanyScope.currentCompanyCode());
    }

    /**
     * 按指定账套重建本地知识库资料索引。
     *
     * <p>实现步骤：后台任务使用上传时记录的 organizationCode，避免异步线程没有登录上下文时误判权限。</p>
     */
    private LocalKnowledgeDocumentView rebuildForCompany(Long id, String organizationCode) {
        LocalKnowledgeDocument document = requireDocument(id, organizationCode);
        document.setStatus(LocalKnowledgeDocumentStatus.INDEXING);
        document.setErrorMessage(null);
        document.setChunkCount(0);
        repository.flush();
        try {
            ExtractedText extracted = extract(document);
            if (extracted.text().isBlank()) {
                throw new BusinessException("未解析出可入库文本；图片 OCR 需要可用的本地视觉模型，或配置 QWEN_API_KEY 回退千问。");
            }
            List<KnowledgeDocument> chunks = buildKnowledgeDocuments(document, extracted);
            vectorStore().replaceSource(KnowledgeSourceType.USER_DOCUMENT, document.getId(), chunks);
            document.setStatus(LocalKnowledgeDocumentStatus.INDEXED);
            document.setOcrUsed(extracted.ocrUsed());
            document.setChunkCount(chunks.size());
            document.setErrorMessage(null);
        } catch (RuntimeException ex) {
            vectorStore().deleteSource(KnowledgeSourceType.USER_DOCUMENT, document.getId());
            document.setStatus(LocalKnowledgeDocumentStatus.FAILED);
            document.setErrorMessage(truncate(ex.getMessage(), 2000));
        }
        return toView(document);
    }

    /**
     * 删除本地知识库资料及其索引分片。
     *
     * <p>实现步骤：先校验当前所属公司权限，再删除向量库中的该资料分片，最后删除上传记录。</p>
     */
    @Transactional
    public void delete(Long id) {
        LocalKnowledgeDocument document = requireCurrentDocument(id);
        vectorStore().deleteSource(KnowledgeSourceType.USER_DOCUMENT, document.getId());
        repository.delete(document);
    }

    /**
     * 抽取资料正文。
     *
     * <p>实现步骤：图片文件调用 OCR 策略服务，非图片文件复用附件文本抽取器，并返回是否使用 OCR 的标记。</p>
     */
    private ExtractedText extract(LocalKnowledgeDocument document) {
        if (IMAGE_SUFFIXES.contains(document.getSuffix())) {
            return new ExtractedText(ocr(document), true);
        }
        AttachmentFile attachment = new AttachmentFile();
        attachment.setOriginalName(document.getOriginalName());
        attachment.setDisplayName(document.getTitle());
        attachment.setSuffix(document.getSuffix());
        attachment.setStoragePath(document.getStoragePath());
        attachment.setFileSize(document.getFileSize());
        attachment.setContentType(document.getContentType());
        String text = textExtractor.extract(attachment);
        return new ExtractedText(text, false);
    }

    /**
     * 对图片资料执行 OCR。
     *
     * <p>实现步骤：
     * 1. 按受控 storagePath 解析源文件绝对路径；
     * 2. 转换为 data URL 作为多模态输入；
     * 3. 通过 AiOcrService 按本地视觉模型优先、千问回退策略识别；
     * 4. 识别失败时抛出用户可读异常，资料状态由 rebuild 标记为 FAILED。</p>
     */
    private String ocr(LocalKnowledgeDocument document) {
        Path path = storagePath(document.getStoragePath());
        try {
            String dataUrl = "data:" + contentType(document) + ";base64,"
                    + Base64.getEncoder().encodeToString(Files.readAllBytes(path));
            return aiOcrService.recognize(
                    "你是企业知识库 OCR 助手。只提取图片中的可见文字，保持原始标题、段落、表格字段和编号，不要编造。",
                    "请识别文件 " + document.getOriginalName() + " 中的全部文字，直接输出可检索正文。",
                    List.of(new VisionInput(document.getOriginalName(), dataUrl, null))
            );
        } catch (Exception ex) {
            throw new BusinessException(HttpStatus.BAD_GATEWAY, ResponseCode.LOAD_CLIENT_ERROR,
                    "图片 OCR 失败：" + ex.getMessage());
        }
    }

    /**
     * 把抽取正文转换为知识分片。
     *
     * <p>实现步骤：组装资料标题、原文件名、说明、解析方式和正文，按配置切片，
     * 为每个分片补齐来源、权限、账套、内容哈希和 embedding。</p>
     */
    private List<KnowledgeDocument> buildKnowledgeDocuments(LocalKnowledgeDocument source, ExtractedText extracted) {
        String content = normalize(lines(
                "本地知识库资料: " + source.getTitle(),
                "文件名: " + source.getOriginalName(),
                "说明: " + value(source.getDescription()),
                extracted.ocrUsed() ? "解析方式: 图片 OCR" : "解析方式: 文档文本抽取",
                "正文:",
                extracted.text()
        ));
        List<KnowledgeDocument> documents = new ArrayList<>();
        List<KnowledgeChunk> chunks = chunks(content);
        for (int index = 0; index < chunks.size(); index++) {
            KnowledgeChunk chunk = chunks.get(index);
            KnowledgeDocument document = new KnowledgeDocument();
            document.setSourceType(KnowledgeSourceType.USER_DOCUMENT);
            document.setSourceId(source.getId());
            document.setSourceNo(source.getOriginalName());
            document.setTitle(truncate(source.getTitle(), 300));
            document.setCategory("本地知识库");
            document.setContent(truncate(chunk.content(), 4000));
            document.setSummary(truncate(chunk.content(), 500));
            document.setMetadata(metadata(source, extracted.ocrUsed(), index, chunks.size(), chunk.sectionTitle()));
            document.setPermissionCode(PermissionCode.AI_ASSISTANT_USE);
            document.setOrganizationCode(source.getOrganizationCode());
            document.setContentHash(sha256("USER_DOCUMENT:" + source.getId() + ":" + index + ":" + chunk.content()));
            document.setChunkIndex(index);
            fillEmbedding(document, chunk.content());
            documents.add(document);
        }
        return documents;
    }

    /**
     * 为知识分片生成本地向量。
     *
     * <p>实现步骤：根据知识库配置和向量库要求判断是否必须生成 embedding；
     * 必需但模型不可用时失败，非必需时允许跳过。</p>
     */
    private void fillEmbedding(KnowledgeDocument document, String chunk) {
        if (!aiProperties.getKnowledge().isEmbeddingEnabled() && !vectorStoreRouter.requiresEmbedding()) {
            return;
        }
        if (!ollamaClient.embeddingAvailable()) {
            if (vectorStoreRouter.requiresEmbedding()) {
                throw new BusinessException(HttpStatus.BAD_GATEWAY, ResponseCode.LOAD_CLIENT_ERROR,
                        "当前向量库模式需要本地 embedding 模型，请先启动 Ollama 并下载 " + ollamaClient.embeddingModel() + "。");
            }
            return;
        }
        List<Double> embedding = ollamaClient.embedding(chunk);
        if (embedding.isEmpty() && vectorStoreRouter.requiresEmbedding()) {
            throw new BusinessException(HttpStatus.BAD_GATEWAY, ResponseCode.LOAD_CLIENT_ERROR,
                    "本地 embedding 模型未返回有效向量。");
        }
        if (!embedding.isEmpty()) {
            document.setEmbeddingJson(JSONObject.toJSONString(embedding));
            document.setEmbeddingModel(ollamaClient.embeddingModel());
        }
    }

    /**
     * 读取并校验当前账套可访问的本地资料。
     *
     * <p>实现步骤：按主键查询上传记录，再用 CompanyScope 校验所属公司，防止跨账套重建或删除。</p>
     */
    private LocalKnowledgeDocument requireCurrentDocument(Long id) {
        return requireDocument(id, CompanyScope.currentCompanyCode());
    }

    /**
     * 读取并校验指定账套可访问的本地资料。
     *
     * <p>实现步骤：按主键查询上传记录，再按传入公司编码校验所属公司，供同步接口和后台任务复用。</p>
     */
    private LocalKnowledgeDocument requireDocument(Long id, String organizationCode) {
        LocalKnowledgeDocument document = repository.findById(id)
                .orElseThrow(() -> new BusinessException(ResponseCode.REF_OBJ_NOT_EXISIT, "本地知识资料不存在。"));
        if (!Objects.equals(document.getOrganizationCode(), organizationCode)) {
            throw new BusinessException(ResponseCode.ILLEGAL_PARAM, "本地知识资料不属于当前账套。");
        }
        return document;
    }

    /**
     * 提交本地知识库后台入库任务。
     *
     * <p>实现步骤：事务提交后再启动后台任务，避免任务线程读取不到刚保存的上传记录。</p>
     */
    private void enqueueRebuild(Long id, String organizationCode) {
        Runnable task = () -> rebuildInBackground(id, organizationCode);
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    indexExecutor.submit(task);
                }
            });
            return;
        }
        indexExecutor.submit(task);
    }

    /**
     * 后台执行资料解析和入库。
     *
     * <p>实现步骤：使用独立事务调用重建逻辑；异常由重建逻辑写入失败状态，兜底异常也会记录到资料状态。</p>
     */
    private void rebuildInBackground(Long id, String organizationCode) {
        try {
            transactionTemplate.executeWithoutResult(status -> rebuildForCompany(id, organizationCode));
        } catch (RuntimeException ex) {
            transactionTemplate.executeWithoutResult(status -> markFailed(id, organizationCode, ex.getMessage()));
        }
    }

    /**
     * 标记后台入库失败。
     */
    private void markFailed(Long id, String organizationCode, String message) {
        LocalKnowledgeDocument document = repository.findById(id).orElse(null);
        if (document == null || !Objects.equals(document.getOrganizationCode(), organizationCode)) {
            return;
        }
        document.setStatus(LocalKnowledgeDocumentStatus.FAILED);
        document.setChunkCount(0);
        document.setErrorMessage(truncate(message, 2000));
    }

    /**
     * 关闭本地知识库后台线程池。
     */
    @PreDestroy
    public void shutdown() {
        indexExecutor.shutdownNow();
    }

    /**
     * 为全量知识索引重建生成当前已入库本地资料分片。
     */
    public List<KnowledgeDocument> indexedDocumentsForRebuild() {
        List<KnowledgeDocument> documents = new ArrayList<>();
        for (LocalKnowledgeDocument document : repository.findAll()) {
            if (document.getStatus() != LocalKnowledgeDocumentStatus.INDEXED) {
                continue;
            }
            try {
                documents.addAll(buildKnowledgeDocuments(document, extract(document)));
            } catch (RuntimeException ex) {
                document.setStatus(LocalKnowledgeDocumentStatus.FAILED);
                document.setErrorMessage(truncate(ex.getMessage(), 2000));
            }
        }
        return documents;
    }

    private KnowledgeVectorStore vectorStore() {
        return vectorStoreRouter.active();
    }

    /**
     * 保存上传文件到本地知识库目录。
     *
     * <p>实现步骤：以附件 baseDir 为根目录，按 knowledge/所属公司/随机文件名 存储，并校验最终路径不越界。</p>
     */
    private String store(MultipartFile file, String suffix) {
        try {
            Path base = Paths.get(storageProperties.getBaseDir()).toAbsolutePath().normalize();
            Path dir = base.resolve("knowledge").resolve(CompanyScope.currentCompanyCode()).normalize();
            if (!dir.startsWith(base)) {
                throw new BusinessException("本地知识库存储路径异常。");
            }
            Files.createDirectories(dir);
            String fileName = UUID.randomUUID() + "." + suffix;
            Path target = dir.resolve(fileName).normalize();
            file.transferTo(target);
            return base.relativize(target).toString().replace('\\', '/');
        } catch (BusinessException ex) {
            throw ex;
        } catch (Exception ex) {
            throw new BusinessException(HttpStatus.INTERNAL_SERVER_ERROR, ResponseCode.FAILED,
                    "保存本地知识库文件失败。");
        }
    }

    /**
     * 把相对存储路径解析为受控绝对路径。
     *
     * <p>实现步骤：从附件根目录 resolve 相对路径并 normalize，最终路径必须仍在根目录内。</p>
     */
    private Path storagePath(String storagePath) {
        Path base = Paths.get(storageProperties.getBaseDir()).toAbsolutePath().normalize();
        Path target = base.resolve(storagePath).normalize();
        if (!target.startsWith(base)) {
            throw new BusinessException("本地知识库文件路径异常。");
        }
        return target;
    }

    /**
     * 按配置切分知识正文。
     *
     * <p>实现步骤：读取 chunkSize 和 chunkOverlap，使用重叠窗口切分，保证上下文连续且避免死循环。</p>
     */
    private List<KnowledgeChunk> chunks(String content) {
        if (content == null || content.isBlank()) {
            return List.of();
        }
        int chunkSize = Math.max(300, aiProperties.getKnowledge().getChunkSize());
        int overlap = Math.max(0, Math.min(aiProperties.getKnowledge().getChunkOverlap(), chunkSize / 2));
        List<KnowledgeChunk> chunks = new ArrayList<>();
        String currentSection = "";
        StringBuilder buffer = new StringBuilder();
        for (String block : semanticBlocks(content)) {
            String normalizedBlock = block.trim();
            if (normalizedBlock.isEmpty()) {
                continue;
            }
            if (isSectionHeading(normalizedBlock)) {
                currentSection = truncate(normalizedBlock, 200);
            }
            if (normalizedBlock.length() > chunkSize) {
                flushChunk(chunks, buffer, currentSection, overlap);
                chunks.addAll(characterChunks(normalizedBlock, currentSection, chunkSize, overlap));
                continue;
            }
            int nextLength = buffer.isEmpty()
                    ? normalizedBlock.length()
                    : buffer.length() + 2 + normalizedBlock.length();
            if (nextLength > chunkSize) {
                flushChunk(chunks, buffer, currentSection, overlap);
            }
            if (!buffer.isEmpty()) {
                buffer.append("\n\n");
            }
            buffer.append(normalizedBlock);
        }
        flushChunk(chunks, buffer, currentSection, overlap);
        return chunks;
    }

    /**
     * 构建分片元数据 JSON。
     *
     * <p>实现步骤：记录前端路由、本地资料主键、原始文件名、OCR 标记、分片位置和章节标题，方便检索结果跳转和排查。</p>
     */
    private String metadata(LocalKnowledgeDocument document, boolean ocrUsed, int chunkIndex, int totalChunks, String sectionTitle) {
        JSONObject object = new JSONObject();
        object.put("route", "/assistant");
        object.put("localDocumentId", document.getId());
        object.put("originalName", document.getOriginalName());
        object.put("ocrUsed", ocrUsed);
        object.put("chunkIndex", chunkIndex);
        object.put("totalChunks", totalChunks);
        object.put("previousChunkIndex", chunkIndex > 0 ? chunkIndex - 1 : null);
        object.put("nextChunkIndex", chunkIndex + 1 < totalChunks ? chunkIndex + 1 : null);
        object.put("sectionTitle", value(sectionTitle));
        return object.toJSONString();
    }

    /**
     * 把正文拆成尽量保持语义边界的段落块。
     */
    private List<String> semanticBlocks(String content) {
        List<String> blocks = new ArrayList<>();
        StringBuilder paragraph = new StringBuilder();
        for (String line : content.split("\\R")) {
            String normalizedLine = line.trim();
            if (normalizedLine.isEmpty()) {
                flushBlock(blocks, paragraph);
                continue;
            }
            if (isTableLine(normalizedLine) || isSectionHeading(normalizedLine)) {
                flushBlock(blocks, paragraph);
                blocks.add(normalizedLine);
                continue;
            }
            if (!paragraph.isEmpty()) {
                paragraph.append('\n');
            }
            paragraph.append(normalizedLine);
        }
        flushBlock(blocks, paragraph);
        return blocks;
    }

    private void flushBlock(List<String> blocks, StringBuilder paragraph) {
        if (!paragraph.isEmpty()) {
            blocks.add(paragraph.toString());
            paragraph.setLength(0);
        }
    }

    private void flushChunk(List<KnowledgeChunk> chunks, StringBuilder buffer, String sectionTitle, int overlap) {
        if (buffer.isEmpty()) {
            return;
        }
        String content = buffer.toString().trim();
        if (!content.isEmpty()) {
            chunks.add(new KnowledgeChunk(content, sectionTitle));
        }
        String tail = tail(content, overlap);
        buffer.setLength(0);
        if (!tail.isBlank()) {
            buffer.append(tail);
        }
    }

    private List<KnowledgeChunk> characterChunks(String content, String sectionTitle, int chunkSize, int overlap) {
        List<KnowledgeChunk> chunks = new ArrayList<>();
        int start = 0;
        while (start < content.length()) {
            int end = Math.min(content.length(), start + chunkSize);
            chunks.add(new KnowledgeChunk(content.substring(start, end).trim(), sectionTitle));
            if (end >= content.length()) {
                break;
            }
            start = Math.max(end - overlap, start + 1);
        }
        return chunks;
    }

    private String tail(String content, int overlap) {
        if (overlap <= 0 || content.length() <= overlap) {
            return "";
        }
        int start = Math.max(0, content.length() - overlap);
        int boundary = Math.max(content.lastIndexOf('\n', start), content.lastIndexOf('。', start));
        if (boundary > 0 && content.length() - boundary <= overlap) {
            return content.substring(boundary + 1).trim();
        }
        return content.substring(start).trim();
    }

    private boolean isTableLine(String line) {
        return line.startsWith("|") || line.contains("\t") || line.matches(".*\\s{2,}.*");
    }

    private boolean isSectionHeading(String line) {
        return line.length() <= 120 && (
                line.matches("^#{1,6}\\s+.+")
                        || line.matches("^[一二三四五六七八九十]+[、.．].+")
                        || line.matches("^\\d+(\\.\\d+)*[、.．]\\s*.+")
                        || line.matches("^第[一二三四五六七八九十\\d]+[章节条].+")
        );
    }

    /**
     * 转换资料实体为前端展示 DTO。
     *
     * <p>实现步骤：把空分片数和 OCR 标记归一化，避免前端在新上传或失败状态下处理 null。</p>
     */
    private LocalKnowledgeDocumentView toView(LocalKnowledgeDocument document) {
        return new LocalKnowledgeDocumentView(
                document.getId(),
                document.getTitle(),
                document.getDescription(),
                document.getOriginalName(),
                document.getSuffix(),
                document.getFileSize(),
                document.getStatus().name(),
                document.getChunkCount() == null ? 0 : document.getChunkCount(),
                Boolean.TRUE.equals(document.getOcrUsed()),
                document.getUploadedBy(),
                document.getOrganizationCode(),
                document.getErrorMessage(),
                document.getCreatedTime(),
                document.getModifyTime()
        );
    }

    /**
     * 取得安全的原始文件名。
     *
     * <p>实现步骤：丢弃上传路径信息，只保留文件名，并限制长度。</p>
     */
    private String safeOriginalName(String originalName) {
        String name = originalName == null || originalName.isBlank() ? "knowledge-file" : Paths.get(originalName).getFileName().toString();
        return truncate(name, 300);
    }

    /**
     * 获取图片 OCR 使用的 MIME 类型。
     *
     * <p>实现步骤：优先使用浏览器上传的 contentType，缺失时按文件后缀给出稳定默认值。</p>
     */
    private String contentType(LocalKnowledgeDocument document) {
        if (document.getContentType() != null && !document.getContentType().isBlank()) {
            return document.getContentType();
        }
        return switch (document.getSuffix()) {
            case "jpg", "jpeg" -> "image/jpeg";
            case "webp" -> "image/webp";
            case "bmp" -> "image/bmp";
            default -> "image/png";
        };
    }

    /**
     * 拼接多段非空文本。
     *
     * <p>实现步骤：过滤 null 和空白内容，按换行组织成可切片正文。</p>
     */
    private String lines(Object... values) {
        List<String> lines = new ArrayList<>();
        for (Object value : values) {
            if (value != null && !String.valueOf(value).isBlank()) {
                lines.add(String.valueOf(value));
            }
        }
        return String.join("\n", lines);
    }

    /**
     * 为空标题补默认值并限制长度。
     */
    private String blankToDefault(String value, String defaultValue) {
        return value == null || value.isBlank() ? truncate(defaultValue, 240) : truncate(value, 240);
    }

    /**
     * 把 null 文本转换为空串。
     */
    private String value(String value) {
        return value == null ? "" : value;
    }

    /**
     * 归一化入库文本。
     *
     * <p>实现步骤：去除空字符，压缩横向空白，保留换行语义交给上游组织。</p>
     */
    private String normalize(String value) {
        return value == null ? "" : value.replace('\u0000', ' ').replaceAll("[\\t\\x0B\\f\\r ]+", " ").trim();
    }

    /**
     * 截断文本到数据库字段允许长度。
     */
    private String truncate(String value, int maxLength) {
        if (value == null) {
            return "";
        }
        String normalized = normalize(value);
        if (normalized.length() <= maxLength) {
            return normalized;
        }
        return normalized.substring(0, maxLength);
    }

    /**
     * 生成知识分片内容哈希。
     *
     * <p>实现步骤：优先使用 SHA-256；极端环境不可用时退回稳定的字符串哈希，避免入库流程中断。</p>
     */
    private String sha256(String value) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(digest.digest(value.getBytes(StandardCharsets.UTF_8)));
        } catch (Exception ex) {
            return Integer.toHexString(Objects.hashCode(value)).toLowerCase(Locale.ROOT);
        }
    }

    private record KnowledgeChunk(String content, String sectionTitle) {
    }

    private record ExtractedText(String text, boolean ocrUsed) {
    }
}
