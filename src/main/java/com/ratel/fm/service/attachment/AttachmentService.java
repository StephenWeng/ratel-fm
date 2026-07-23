package com.ratel.fm.service.attachment;

import com.ratel.fm.common.BusinessException;
import com.ratel.fm.common.ResponseCode;
import com.ratel.fm.config.attachment.AttachmentStorageProperties;
import com.ratel.fm.domain.attachment.AttachmentBusinessType;
import com.ratel.fm.domain.attachment.AttachmentFile;
import com.ratel.fm.domain.attachment.BusinessAttachment;
import com.ratel.fm.repository.attachment.AttachmentFileRepository;
import com.ratel.fm.repository.attachment.BusinessAttachmentRepository;
import com.ratel.fm.repository.basic.BasicDictionaryRepository;
import com.ratel.fm.security.CompanyScope;
import com.ratel.fm.security.CurrentUser;
import com.ratel.fm.security.SecurityUtils;
import com.ratel.fm.service.audit.AuditLogService;
import com.ratel.fm.service.knowledge.KnowledgeIndexService;
import com.ratel.fm.web.dto.attachment.AttachmentDtos.AttachmentView;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

/**
 * 统一附件管理服务。
 *
 * <p>负责附件元数据落库、文件写入 files 目录、业务关系维护、附件下载和物理文件清理。</p>
 *
 * <p>开发组织：ratel；开发人员：WenZhang；联系方式：18782945613。</p>
 *
 * @author ratel
 */
@Service
public class AttachmentService {

    /**
     * 附件服务日志对象，用于记录文件保存、读取、删除和异常信息。
     */
    private static final Logger log = LoggerFactory.getLogger(AttachmentService.class);
    /**
     * 附件存储月份目录格式，按 yyyy/MM 分散文件，避免单目录文件过多。
     */
    private static final DateTimeFormatter STORAGE_MONTH_FORMAT = DateTimeFormatter.ofPattern("yyyy/MM");

    /**
     * 字段 attachmentFileRepository：保存 attachmentFileRepository 对应的业务数据、运行配置或依赖对象，供本类逻辑读取和维护。
     */
    private final AttachmentFileRepository attachmentFileRepository;
    /**
     * 字段 businessAttachmentRepository：保存 businessAttachmentRepository 对应的业务数据、运行配置或依赖对象，供本类逻辑读取和维护。
     */
    private final BusinessAttachmentRepository businessAttachmentRepository;
    /**
     * 字段 dictionaryRepository：用于按当前所属公司编码查找公司名称，生成公司级附件目录。
     */
    private final BasicDictionaryRepository dictionaryRepository;
    /**
     * 字段 storageProperties：保存 storageProperties 对应的业务数据、运行配置或依赖对象，供本类逻辑读取和维护。
     */
    private final AttachmentStorageProperties storageProperties;
    /**
     * 字段 businessRecordValidator：保存 businessRecordValidator 对应的业务数据、运行配置或依赖对象，供本类逻辑读取和维护。
     */
    private final BusinessRecordValidator businessRecordValidator;
    /**
     * 字段 auditLogService：保存 auditLogService 对应的业务数据、运行配置或依赖对象，供本类逻辑读取和维护。
     */
    private final AuditLogService auditLogService;
    /**
     * 字段 knowledgeIndexService：附件上传、改名和删除后同步刷新 AI 知识索引。
     */
    private final KnowledgeIndexService knowledgeIndexService;

    /**
     * 构造 AttachmentService 实例。
     * 
     * <p>实现步骤：
     * 1. 接收调用方传入的依赖对象或初始化参数；
     * 2. 保存到成员字段，保证后续业务方法可以复用；
     * 3. 完成实例初始化。</p>
     */
    public AttachmentService(
            AttachmentFileRepository attachmentFileRepository,
            BusinessAttachmentRepository businessAttachmentRepository,
            BasicDictionaryRepository dictionaryRepository,
            AttachmentStorageProperties storageProperties,
            BusinessRecordValidator businessRecordValidator,
            AuditLogService auditLogService,
            KnowledgeIndexService knowledgeIndexService
    ) {
        this.attachmentFileRepository = attachmentFileRepository;
        this.businessAttachmentRepository = businessAttachmentRepository;
        this.dictionaryRepository = dictionaryRepository;
        this.storageProperties = storageProperties;
        this.businessRecordValidator = businessRecordValidator;
        this.auditLogService = auditLogService;
        this.knowledgeIndexService = knowledgeIndexService;
    }

    /**
     * 查询业务附件列表。
     *
     * <p>实现步骤：
     * 1. 校验业务记录存在；
     * 2. 按业务类型和业务 ID 读取关联记录；
     * 3. 转换为前端列表视图。</p>
     */
    @Transactional(readOnly = true)
    public List<AttachmentView> list(AttachmentBusinessType businessType, Long businessId) {
        businessRecordValidator.ensureExists(businessType, businessId);
        return businessAttachmentRepository.findByBusinessTypeAndBusinessIdOrderBySortOrderAscIdAsc(businessType, businessId)
                .stream()
                .map(relation -> toView(relation.getAttachment()))
                .toList();
    }

    /**
     * 统计业务记录的附件数量。
     *
     * <p>实现步骤：
     * 1. 过滤空业务类型、空业务 ID 或非法业务 ID；
     * 2. 直接统计业务附件关系数量，避免列表页为了展示按钮逐行加载附件明细；
     * 3. 返回数量给业务列表视图，前端据此决定是否展示附件入口。</p>
     */
    @Transactional(readOnly = true)
    public long count(AttachmentBusinessType businessType, Long businessId) {
        if (businessType == null || businessId == null || businessId <= 0) {
            return 0L;
        }
        return businessAttachmentRepository.countByBusinessTypeAndBusinessId(businessType, businessId);
    }

    /**
     * 上传并绑定多个业务附件。
     *
     * <p>实现步骤：
     * 1. 校验业务记录存在和文件列表不为空；
     * 2. 为每个文件生成按年月分组的唯一存储路径；
     * 3. 写入磁盘文件和附件元数据；
     * 4. 写入业务附件关系；
     * 5. 记录操作日志，便于追溯上传证据。</p>
     */
    @Transactional
    public List<AttachmentView> upload(AttachmentBusinessType businessType, Long businessId, MultipartFile[] files) {
        businessRecordValidator.ensureExists(businessType, businessId);
        if (files == null || files.length == 0) {
            throw new BusinessException(ResponseCode.ILLEGAL_PARAM, "请选择需要上传的附件");
        }
        // 变量说明：views 保存当前步骤计算、查询或转换得到的中间结果。
        List<AttachmentView> views = new ArrayList<>();
        // 变量说明：nextSortOrder 保存当前步骤计算、查询或转换得到的中间结果。
        int nextSortOrder = currentAttachmentCount(businessType, businessId);
        for (MultipartFile file : files) {
            if (file == null || file.isEmpty()) {
                continue;
            }
            // 变量说明：attachment 保存当前步骤计算、查询或转换得到的中间结果。
            AttachmentFile attachment = storeFile(file);
            // 变量说明：relation 保存当前步骤计算、查询或转换得到的中间结果。
            BusinessAttachment relation = new BusinessAttachment();
            relation.setBusinessType(businessType);
            relation.setBusinessId(businessId);
            relation.setAttachment(attachment);
            relation.setSortOrder(++nextSortOrder);
            businessAttachmentRepository.save(relation);
            businessAttachmentRepository.flush();
            knowledgeIndexService.rebuildAttachment(attachment.getId());
            views.add(toView(attachment));
        }
        if (views.isEmpty()) {
            throw new BusinessException(ResponseCode.ILLEGAL_PARAM, "上传附件不能为空");
        }
        auditLogService.record("UPLOAD_ATTACHMENTS", "businessType=" + businessType + ", businessId=" + businessId + ", count=" + views.size(),
                "SUCCESS", "附件管理为" + businessLabel(businessType) + "上传了" + views.size() + "个附件。");
        return views;
    }

    /**
     * 修改附件展示名称。
     *
     * <p>实现步骤：
     * 1. 校验业务记录存在；
     * 2. 校验附件属于当前业务记录；
     * 3. 更新附件展示名称；
     * 4. 记录附件改名日志。</p>
     */
    @Transactional
    public AttachmentView rename(AttachmentBusinessType businessType, Long businessId, Long attachmentId, String displayName) {
        businessRecordValidator.ensureExists(businessType, businessId);
        // 变量说明：normalizedName 保存当前步骤计算、查询或转换得到的中间结果。
        String normalizedName = normalizeDisplayName(displayName);
        // 变量说明：relation 保存当前步骤计算、查询或转换得到的中间结果。
        BusinessAttachment relation = relationOrThrow(businessType, businessId, attachmentId);
        // 变量说明：attachment 保存当前步骤计算、查询或转换得到的中间结果。
        AttachmentFile attachment = relation.getAttachment();
        // 变量说明：oldName 保存当前步骤计算、查询或转换得到的中间结果。
        String oldName = attachment.getDisplayName();
        attachment.setDisplayName(normalizedName);
        // 变量说明：view 保存当前步骤计算、查询或转换得到的中间结果。
        AttachmentView view = toView(attachment);
        knowledgeIndexService.rebuildAttachment(attachment.getId());
        auditLogService.record("RENAME_ATTACHMENT", "businessType=" + businessType + ", businessId=" + businessId
                + ", attachmentId=" + attachmentId + ", oldName=" + oldName + ", newName=" + normalizedName,
                "SUCCESS", "附件管理将" + businessLabel(businessType) + "的附件名称从" + oldName + "修改为" + normalizedName + "。");
        return view;
    }

    /**
     * 删除业务附件。
     *
     * <p>实现步骤：
     * 1. 校验业务记录存在和附件归属；
     * 2. 删除业务附件关系；
     * 3. 若附件没有其他业务引用，同步删除磁盘文件和附件元数据；
     * 4. 文件删除异常只记录系统日志，不影响关系删除主流程。</p>
     */
    @Transactional
    public void delete(AttachmentBusinessType businessType, Long businessId, Long attachmentId) {
        businessRecordValidator.ensureExists(businessType, businessId);
        // 变量说明：relation 保存当前步骤计算、查询或转换得到的中间结果。
        BusinessAttachment relation = relationOrThrow(businessType, businessId, attachmentId);
        // 变量说明：attachment 保存当前步骤计算、查询或转换得到的中间结果。
        AttachmentFile attachment = relation.getAttachment();
        businessAttachmentRepository.delete(relation);
        businessAttachmentRepository.flush();
        if (!businessAttachmentRepository.existsByAttachment_Id(attachmentId)) {
            knowledgeIndexService.deleteAttachment(attachmentId);
            deletePhysicalFileQuietly(attachment);
            attachmentFileRepository.delete(attachment);
        } else {
            knowledgeIndexService.rebuildAttachment(attachmentId);
        }
        auditLogService.record("DELETE_ATTACHMENT", "businessType=" + businessType + ", businessId=" + businessId
                + ", attachmentId=" + attachmentId + ", displayName=" + attachment.getDisplayName(),
                "SUCCESS", "附件管理删除了" + businessLabel(businessType) + "的附件" + attachment.getDisplayName() + "。");
    }

    /**
     * 清理指定业务记录下的全部附件。
     *
     * <p>实现步骤：
     * 1. 按业务类型和业务 ID 读取全部附件关系；
     * 2. 逐条删除关系；
     * 3. 附件没有其他业务引用时同步删除物理文件和元数据；
     * 4. 记录清理数量，便于业务单据删除后追溯证据文件清理情况。</p>
     */
    @Transactional
    public void deleteAllForBusiness(AttachmentBusinessType businessType, Long businessId) {
        // 变量说明：relations 保存当前步骤计算、查询或转换得到的中间结果。
        List<BusinessAttachment> relations = businessAttachmentRepository.findByBusinessTypeAndBusinessIdOrderBySortOrderAscIdAsc(businessType, businessId);
        if (relations.isEmpty()) {
            return;
        }
        for (BusinessAttachment relation : relations) {
            // 变量说明：attachment 保存当前步骤计算、查询或转换得到的中间结果。
            AttachmentFile attachment = relation.getAttachment();
            // 变量说明：attachmentId 保存当前步骤计算、查询或转换得到的中间结果。
            Long attachmentId = attachment.getId();
            businessAttachmentRepository.delete(relation);
            businessAttachmentRepository.flush();
            if (!businessAttachmentRepository.existsByAttachment_Id(attachmentId)) {
                knowledgeIndexService.deleteAttachment(attachmentId);
                deletePhysicalFileQuietly(attachment);
                attachmentFileRepository.delete(attachment);
            } else {
                knowledgeIndexService.rebuildAttachment(attachmentId);
            }
        }
        auditLogService.record("DELETE_BUSINESS_ATTACHMENTS", "businessType=" + businessType + ", businessId=" + businessId
                + ", count=" + relations.size(), "SUCCESS",
                "附件管理在删除" + businessLabel(businessType) + "业务记录时清理了" + relations.size() + "个附件。");
    }

    /**
     * 获取附件下载资源。
     *
     * <p>实现步骤：
     * 1. 校验业务记录存在和附件归属；
     * 2. 按附件相对路径解析为 files 目录下的绝对路径；
     * 3. 文件存在时返回 Spring Resource 和下载元数据。</p>
     */
    @Transactional(readOnly = true)
    public AttachmentDownload download(AttachmentBusinessType businessType, Long businessId, Long attachmentId) {
        businessRecordValidator.ensureExists(businessType, businessId);
        // 变量说明：attachment 保存当前步骤计算、查询或转换得到的中间结果。
        AttachmentFile attachment = relationOrThrow(businessType, businessId, attachmentId).getAttachment();
        // 变量说明：filePath 保存当前步骤计算、查询或转换得到的中间结果。
        Path filePath = resolveStoragePath(attachment.getStoragePath());
        if (!Files.exists(filePath) || !Files.isRegularFile(filePath)) {
            throw new BusinessException(HttpStatus.NOT_FOUND, "附件文件不存在");
        }
        // 变量说明：resource 保存当前步骤计算、查询或转换得到的中间结果。
        Resource resource = new FileSystemResource(filePath);
        return new AttachmentDownload(downloadFileName(attachment), attachment.getContentType(), attachment.getFileSize(), resource);
    }

    /**
     * 执行 currentAttachmentCount 方法。
     * 
     * <p>实现步骤：
     * 1. 接收并校验调用方传入的数据；
     * 2. 按当前方法职责执行业务查询、转换或持久化处理；
     * 3. 返回处理结果或更新对象状态。</p>
     */
    private int currentAttachmentCount(AttachmentBusinessType businessType, Long businessId) {
        return businessAttachmentRepository.findByBusinessTypeAndBusinessIdOrderBySortOrderAscIdAsc(businessType, businessId).size();
    }

    /**
     * 执行 storeFile 方法。
     * 
     * <p>实现步骤：
     * 1. 接收并校验调用方传入的数据；
     * 2. 按当前方法职责执行业务查询、转换或持久化处理；
     * 3. 返回处理结果或更新对象状态。</p>
     */
    private AttachmentFile storeFile(MultipartFile file) {
        // 变量说明：originalName 保存当前步骤计算、查询或转换得到的中间结果。
        String originalName = safeOriginalName(file.getOriginalFilename());
        // 变量说明：suffix 保存当前步骤计算、查询或转换得到的中间结果。
        String suffix = suffixOf(originalName);
        // 变量说明：storageRelativePath 保存当前步骤计算、查询或转换得到的中间结果。
        String storageRelativePath = storageRelativePath(suffix);
        // 变量说明：targetPath 保存当前步骤计算、查询或转换得到的中间结果。
        Path targetPath = resolveStoragePath(storageRelativePath);
        try {
            Files.createDirectories(targetPath.getParent());
            file.transferTo(targetPath);
        } catch (IOException ex) {
            throw new BusinessException(HttpStatus.INTERNAL_SERVER_ERROR, ResponseCode.DATABASE_OPERATION_ERROR, "附件保存失败");
        }
        // 变量说明：currentUser 保存当前步骤计算、查询或转换得到的中间结果。
        CurrentUser currentUser = SecurityUtils.currentUser();
        // 变量说明：attachment 保存当前步骤计算、查询或转换得到的中间结果。
        AttachmentFile attachment = new AttachmentFile();
        attachment.setOriginalName(originalName);
        attachment.setDisplayName(originalName);
        attachment.setSuffix(suffix);
        attachment.setFileSize(file.getSize());
        attachment.setContentType(file.getContentType());
        attachment.setStoragePath(storageRelativePath);
        attachment.setUploaderId(currentUser.id());
        attachment.setUploaderUsername(currentUser.username());
        return attachmentFileRepository.save(attachment);
    }

    /**
     * 执行 relationOrThrow 方法。
     * 
     * <p>实现步骤：
     * 1. 接收并校验调用方传入的数据；
     * 2. 按当前方法职责执行业务查询、转换或持久化处理；
     * 3. 返回处理结果或更新对象状态。</p>
     */
    private BusinessAttachment relationOrThrow(AttachmentBusinessType businessType, Long businessId, Long attachmentId) {
        if (attachmentId == null || attachmentId <= 0) {
            throw new BusinessException(ResponseCode.ILLEGAL_PARAM, "附件ID不正确");
        }
        return businessAttachmentRepository.findByBusinessTypeAndBusinessIdAndAttachment_Id(businessType, businessId, attachmentId)
                .orElseThrow(() -> new BusinessException(HttpStatus.NOT_FOUND, "附件不存在或不属于当前业务记录"));
    }

    /**
     * 执行 normalizeDisplayName 方法。
     * 
     * <p>实现步骤：
     * 1. 接收并校验调用方传入的数据；
     * 2. 按当前方法职责执行业务查询、转换或持久化处理；
     * 3. 返回处理结果或更新对象状态。</p>
     */
    private String normalizeDisplayName(String displayName) {
        // 变量说明：normalizedName 保存当前步骤计算、查询或转换得到的中间结果。
        String normalizedName = displayName == null ? "" : displayName.trim();
        if (normalizedName.isEmpty()) {
            throw new BusinessException(ResponseCode.ILLEGAL_PARAM, "附件名称不能为空");
        }
        if (normalizedName.length() > 255) {
            throw new BusinessException(ResponseCode.ILLEGAL_PARAM, "附件名称不能超过255个字符");
        }
        return normalizedName;
    }

    /**
     * 执行 safeOriginalName 方法。
     * 
     * <p>实现步骤：
     * 1. 接收并校验调用方传入的数据；
     * 2. 按当前方法职责执行业务查询、转换或持久化处理；
     * 3. 返回处理结果或更新对象状态。</p>
     */
    private String safeOriginalName(String originalFilename) {
        if (originalFilename == null || originalFilename.isBlank()) {
            return "未命名附件";
        }
        return Paths.get(originalFilename).getFileName().toString();
    }

    /**
     * 执行 suffixOf 方法。
     * 
     * <p>实现步骤：
     * 1. 接收并校验调用方传入的数据；
     * 2. 按当前方法职责执行业务查询、转换或持久化处理；
     * 3. 返回处理结果或更新对象状态。</p>
     */
    private String suffixOf(String originalName) {
        // 变量说明：dotIndex 保存当前步骤计算、查询或转换得到的中间结果。
        int dotIndex = originalName.lastIndexOf('.');
        if (dotIndex < 0 || dotIndex == originalName.length() - 1) {
            return "";
        }
        return originalName.substring(dotIndex + 1).toLowerCase(Locale.ROOT);
    }

    /**
     * 执行 storageRelativePath 方法。
     * 
     * <p>实现步骤：
     * 1. 接收并校验调用方传入的数据；
     * 2. 按当前方法职责执行业务查询、转换或持久化处理；
     * 3. 返回处理结果或更新对象状态。</p>
     */
    private String storageRelativePath(String suffix) {
        // 步骤1：所属公司目录放在 files 根目录第一层，隔离不同账套的附件文件。
        String companyDirectory = currentCompanyDirectory();
        // 变量说明：month 保存当前步骤计算、查询或转换得到的中间结果。
        String month = LocalDate.now().format(STORAGE_MONTH_FORMAT);
        // 变量说明：fileName 保存当前步骤计算、查询或转换得到的中间结果。
        String fileName = UUID.randomUUID() + (suffix == null || suffix.isBlank() ? "" : "." + suffix);
        return companyDirectory + "/" + month + "/" + fileName;
    }

    /**
     * 生成当前所属公司的附件顶层目录名。
     *
     * <p>实现步骤：
     * 1. 从当前登录上下文读取所属公司编码；
     * 2. 按公司编码查询组织字典名称，查不到时用编码兜底；
     * 3. 对编码和名称做文件名安全处理，最终形成“所属公司代码_所属公司名称”。</p>
     */
    private String currentCompanyDirectory() {
        String companyCode = CompanyScope.currentCompanyCode();
        String companyName = dictionaryRepository.findByCode(companyCode)
                .map(dictionary -> dictionary.getName() == null || dictionary.getName().isBlank() ? companyCode : dictionary.getName())
                .orElse(companyCode);
        return safePathSegment(companyCode) + "_" + safePathSegment(companyName);
    }

    /**
     * 清洗附件目录片段，避免公司名称中的路径分隔符影响落盘路径。
     *
     * <p>实现步骤：去除首尾空白；把 Windows 和 Linux 路径非法字符替换为下划线；空值回退为 unknown。</p>
     */
    private String safePathSegment(String value) {
        String normalized = value == null ? "" : value.trim().replaceAll("[\\\\/:*?\"<>|\\r\\n\\t]", "_");
        return normalized.isBlank() ? "unknown" : normalized;
    }

    /**
     * 执行 storageBasePath 方法。
     * 
     * <p>实现步骤：
     * 1. 接收并校验调用方传入的数据；
     * 2. 按当前方法职责执行业务查询、转换或持久化处理；
     * 3. 返回处理结果或更新对象状态。</p>
     */
    private Path storageBasePath() {
        return Paths.get(storageProperties.getBaseDir()).toAbsolutePath().normalize();
    }

    /**
     * 执行 resolveStoragePath 方法。
     * 
     * <p>实现步骤：
     * 1. 接收并校验调用方传入的数据；
     * 2. 按当前方法职责执行业务查询、转换或持久化处理；
     * 3. 返回处理结果或更新对象状态。</p>
     */
    private Path resolveStoragePath(String storagePath) {
        // 变量说明：basePath 保存当前步骤计算、查询或转换得到的中间结果。
        Path basePath = storageBasePath();
        // 变量说明：targetPath 保存当前步骤计算、查询或转换得到的中间结果。
        Path targetPath = basePath.resolve(storagePath).normalize();
        if (!targetPath.startsWith(basePath)) {
            throw new BusinessException(ResponseCode.ILLEGAL_PARAM, "附件路径不合法");
        }
        return targetPath;
    }

    /**
     * 执行 downloadFileName 方法。
     * 
     * <p>实现步骤：
     * 1. 接收并校验调用方传入的数据；
     * 2. 按当前方法职责执行业务查询、转换或持久化处理；
     * 3. 返回处理结果或更新对象状态。</p>
     */
    private String downloadFileName(AttachmentFile attachment) {
        // 变量说明：displayName 保存当前步骤计算、查询或转换得到的中间结果。
        String displayName = attachment.getDisplayName();
        if (displayName == null || displayName.isBlank()) {
            displayName = attachment.getOriginalName();
        }
        // 变量说明：suffix 保存当前步骤计算、查询或转换得到的中间结果。
        String suffix = attachment.getSuffix();
        if (suffix == null || suffix.isBlank() || displayName.toLowerCase(Locale.ROOT).endsWith("." + suffix.toLowerCase(Locale.ROOT))) {
            return displayName;
        }
        return displayName + "." + suffix;
    }

    /**
     * 执行 deletePhysicalFileQuietly 方法。
     * 
     * <p>实现步骤：
     * 1. 接收并校验调用方传入的数据；
     * 2. 按当前方法职责执行业务查询、转换或持久化处理；
     * 3. 返回处理结果或更新对象状态。</p>
     */
    private void deletePhysicalFileQuietly(AttachmentFile attachment) {
        try {
            Files.deleteIfExists(resolveStoragePath(attachment.getStoragePath()));
        } catch (RuntimeException | IOException ex) {
            log.warn("删除附件物理文件失败，attachmentId={}, path={}", attachment.getId(), attachment.getStoragePath(), ex);
        }
    }

    /**
     * 执行 toView 方法。
     * 
     * <p>实现步骤：
     * 1. 接收并校验调用方传入的数据；
     * 2. 按当前方法职责执行业务查询、转换或持久化处理；
     * 3. 返回处理结果或更新对象状态。</p>
     */
    private AttachmentView toView(AttachmentFile attachment) {
        return new AttachmentView(
                attachment.getId(),
                attachment.getOriginalName(),
                attachment.getDisplayName(),
                attachment.getSuffix(),
                attachment.getFileSize(),
                attachment.getContentType(),
                attachment.getUploaderUsername(),
                attachment.getCreatedTime()
        );
    }

    /**
     * 附件下载响应数据。
     */
    public record AttachmentDownload(String filename, String contentType, Long fileSize, Resource resource) {
    }

    /**
     * 将附件业务类型转换为日志可读模块名称。
     */
    private String businessLabel(AttachmentBusinessType businessType) {
        return switch (businessType) {
            case VOUCHER -> "凭证记账";
            case PURCHASE_ORDER -> "采购管理";
            case SHIPMENT -> "物流管理";
            case INVENTORY_LEDGER -> "库存台账";
            case AR_AP_BILL -> "应收应付";
        };
    }
}
