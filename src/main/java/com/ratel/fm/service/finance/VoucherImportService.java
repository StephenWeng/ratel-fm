package com.ratel.fm.service.finance;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONArray;
import com.alibaba.fastjson2.JSONObject;
import com.ratel.fm.common.BusinessException;
import com.ratel.fm.domain.finance.AccountingSubject;
import com.ratel.fm.repository.finance.AccountingSubjectRepository;
import com.ratel.fm.security.CompanyScope;
import com.ratel.fm.service.ai.AiOcrService;
import com.ratel.fm.service.ai.QwenClient.VisionInput;
import com.ratel.fm.service.basic.CurrencyService;
import com.ratel.fm.web.dto.finance.FinanceDtos.VoucherImportLine;
import com.ratel.fm.web.dto.finance.FinanceDtos.VoucherImportResult;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.rendering.ImageType;
import org.apache.pdfbox.rendering.PDFRenderer;
import org.apache.pdfbox.text.PDFTextStripper;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Set;

/**
 * 凭证图片/PDF 导入识别服务。
 *
 * <p>实现目的：把用户上传的凭证图片或 PDF 交给本地或云端视觉模型识别，返回可编辑的凭证分录草稿，不直接保存正式凭证。</p>
 */
@Service
public class VoucherImportService {

    /** 单次导入允许的最大文件数，避免用户误选大量文件造成模型请求过大。 */
    private static final int MAX_FILES = 10;

    /** 单个文件最大识别大小，上传附件本身仍走附件配置，这里只限制 AI 识别输入。 */
    private static final long MAX_RECOGNITION_FILE_SIZE = 15 * 1024 * 1024L;

    /** PDF 文本抽取最大字符数，超出后截断，避免提示词过长。 */
    private static final int MAX_PDF_TEXT_LENGTH = 12_000;

    /** PDF 扫描页最多渲染前 3 页做视觉识别。 */
    private static final int MAX_PDF_RENDER_PAGES = 3;

    /** 可直接发送给视觉模型的图片后缀。 */
    private static final Set<String> IMAGE_SUFFIXES = Set.of("jpg", "jpeg", "png", "webp", "bmp");

    /** OCR 路由服务，用于优先本地视觉模型、再回退千问视觉模型。 */
    private final AiOcrService aiOcrService;

    /** 会计科目仓库，用于把模型识别的科目编码或名称匹配到当前账套启用末级科目。 */
    private final AccountingSubjectRepository subjectRepository;

    /**
     * 构造凭证导入识别服务。
     *
     * <p>实现步骤：保存千问客户端和科目仓库，识别时先调用模型，再按当前账套科目进行可信匹配。</p>
     */
    public VoucherImportService(AiOcrService aiOcrService, AccountingSubjectRepository subjectRepository) {
        this.aiOcrService = aiOcrService;
        this.subjectRepository = subjectRepository;
    }

    /**
     * 识别上传的凭证图片或 PDF。
     *
     * <p>实现步骤：
     * 1. 校验文件数量、类型和大小；
     * 2. 将图片转为 data URL，将可抽取文本的 PDF 转为文本，将扫描 PDF 渲染为图片；
     * 3. 把当前账套启用末级科目清单作为约束发送给模型，减少凭空编造科目；
     * 4. 解析模型返回 JSON，并把科目编码/名称二次匹配为 subjectId；
     * 5. 返回分录草稿和风险提示，前端等待用户确认保存。</p>
     */
    public VoucherImportResult recognize(List<MultipartFile> files) {
        if (files == null || files.isEmpty()) {
            throw new BusinessException("请上传凭证图片或PDF");
        }
        if (files.size() > MAX_FILES) {
            throw new BusinessException("单次最多导入" + MAX_FILES + "个图片或PDF文件");
        }

        // 步骤1：按当前账套读取启用末级科目，后续模型识别和本地匹配都只在这份清单内进行。
        List<AccountingSubject> subjects = enabledLeafSubjects();
        if (subjects.isEmpty()) {
            throw new BusinessException("当前所属公司没有可用的末级会计科目，请先维护会计科目");
        }

        // 步骤2：把上传文件转换为视觉模型可理解的多模态输入。
        List<VisionInput> inputs = new ArrayList<>();
        List<String> warnings = new ArrayList<>();
        for (MultipartFile file : files) {
            addFileInput(file, inputs, warnings);
        }
        if (inputs.isEmpty()) {
            throw new BusinessException("未读取到可识别的图片或PDF内容");
        }

        // 步骤3：优先调用本地视觉模型，失败时回退千问视觉模型，并解析结构化 JSON。
        String answer = aiOcrService.recognize(systemPrompt(), userPrompt(subjects), inputs);
        JSONObject payload = parseJsonObject(answer);
        JSONArray lineArray = payload.getJSONArray("lines");
        if (lineArray == null || lineArray.isEmpty()) {
            warnings.add("未识别到有效凭证分录，请检查图片清晰度或手工录入");
        }

        // 步骤4：逐行匹配科目并转换为前端草稿格式。
        List<VoucherImportLine> lines = new ArrayList<>();
        if (lineArray != null) {
            for (int i = 0; i < lineArray.size(); i++) {
                JSONObject line = lineArray.getJSONObject(i);
                if (line == null) {
                    continue;
                }
                lines.add(toImportLine(line, subjects));
            }
        }
        warnings.addAll(jsonStringArray(payload.getJSONArray("warnings")));
        return new VoucherImportResult(
                parseDate(payload.getString("voucherDate")),
                limitText(payload.getString("summary"), 200),
                limitText(payload.getString("sourceBizNo"), 300),
                lines,
                warnings.stream().filter(value -> value != null && !value.isBlank()).distinct().toList()
        );
    }

    /**
     * 将上传文件转换为模型输入。
     *
     * <p>实现步骤：
     * 1. 校验文件不为空且大小在识别限制内；
     * 2. 图片直接转 data URL；
     * 3. PDF 优先抽取文本，文本为空时渲染前几页为图片；
     * 4. 不支持的类型写入 warning，不中断其他文件识别。</p>
     */
    private void addFileInput(MultipartFile file, List<VisionInput> inputs, List<String> warnings) {
        if (file == null || file.isEmpty()) {
            return;
        }
        if (file.getSize() > MAX_RECOGNITION_FILE_SIZE) {
            warnings.add(file.getOriginalFilename() + "超过识别大小限制，已跳过");
            return;
        }
        String fileName = safeFileName(file.getOriginalFilename());
        String suffix = suffix(fileName);
        try {
            byte[] bytes = file.getBytes();
            if (IMAGE_SUFFIXES.contains(suffix)) {
                inputs.add(new VisionInput(fileName, dataUrl(contentType(file, suffix), bytes), null));
                return;
            }
            if ("pdf".equals(suffix)) {
                addPdfInput(fileName, bytes, inputs);
                return;
            }
            warnings.add(fileName + "不是支持的图片或PDF文件，已跳过");
        } catch (Exception ex) {
            warnings.add(fileName + "读取失败，已跳过");
        }
    }

    /**
     * 处理 PDF 文件输入。
     *
     * <p>实现步骤：先使用 PDFTextStripper 抽取文本；文本足够时直接走文本结构化，文本为空时把前几页渲染为 PNG 交给视觉模型。</p>
     */
    private void addPdfInput(String fileName, byte[] bytes, List<VisionInput> inputs) throws Exception {
        try (PDDocument document = Loader.loadPDF(bytes)) {
            String text = new PDFTextStripper().getText(document);
            if (text != null && !text.isBlank()) {
                inputs.add(new VisionInput(fileName, null, limitText(text, MAX_PDF_TEXT_LENGTH)));
                return;
            }
            PDFRenderer renderer = new PDFRenderer(document);
            int pages = Math.min(document.getNumberOfPages(), MAX_PDF_RENDER_PAGES);
            for (int page = 0; page < pages; page++) {
                BufferedImage image = renderer.renderImageWithDPI(page, 160, ImageType.RGB);
                ByteArrayOutputStream output = new ByteArrayOutputStream();
                ImageIO.write(image, "png", output);
                inputs.add(new VisionInput(fileName + "-第" + (page + 1) + "页.png", dataUrl("image/png", output.toByteArray()), null));
            }
        }
    }

    /**
     * 把模型返回的一行 JSON 转为凭证分录草稿。
     *
     * <p>实现步骤：
     * 1. 读取模型返回的科目 ID、编码和名称；
     * 2. 在当前账套启用末级科目清单内做确定性匹配；
     * 3. 金额、币种、汇率做默认值和精度处理；
     * 4. 未匹配科目时保留 warning，要求用户手工选择。</p>
     */
    private VoucherImportLine toImportLine(JSONObject line, List<AccountingSubject> subjects) {
        AccountingSubject subject = matchSubject(line, subjects);
        String warning = "";
        if (subject == null) {
            String rawSubject = firstText(line.getString("subjectFullName"), line.getString("subjectName"), line.getString("subjectCode"));
            warning = rawSubject == null ? "未识别到科目，请手工选择" : "科目“" + rawSubject + "”未能匹配当前账套末级科目，请手工选择";
        }
        String currencyCode = firstText(line.getString("currencyCode"), CurrencyService.DEFAULT_CURRENCY_CODE);
        String currencyName = "CNY".equalsIgnoreCase(currencyCode)
                ? CurrencyService.DEFAULT_CURRENCY_NAME
                : firstText(line.getString("currencyName"), currencyCode);
        return new VoucherImportLine(
                subject == null ? null : subject.getId(),
                subject == null ? limitText(line.getString("subjectName"), 120) : subject.getName(),
                subject == null ? limitText(line.getString("subjectFullName"), 300) : subjectFullName(subject),
                firstText(limitText(line.getString("summary"), 200), "凭证导入"),
                money(line.getBigDecimal("debitAmount")),
                money(line.getBigDecimal("creditAmount")),
                currencyCode.toUpperCase(Locale.ROOT),
                currencyName,
                rate(line.getBigDecimal("exchangeRateToCny")),
                limitText(line.getString("auxiliary"), 300),
                confidence(line.getBigDecimal("confidence")),
                warning
        );
    }

    /**
     * 匹配当前账套启用末级科目。
     *
     * <p>实现步骤：优先按 subjectId 精确命中，再按科目编码命中，再按完整级联名称和末级名称命中；所有匹配都限定在传入科目清单内。</p>
     */
    private AccountingSubject matchSubject(JSONObject line, List<AccountingSubject> subjects) {
        Long subjectId = line.getLong("subjectId");
        if (subjectId != null) {
            AccountingSubject match = subjects.stream()
                    .filter(subject -> Objects.equals(subject.getId(), subjectId))
                    .findFirst()
                    .orElse(null);
            if (match != null) {
                return match;
            }
        }
        String subjectCode = normalize(line.getString("subjectCode"));
        if (subjectCode != null) {
            AccountingSubject match = subjects.stream()
                    .filter(subject -> subjectCode.equalsIgnoreCase(subject.getCode()))
                    .findFirst()
                    .orElse(null);
            if (match != null) {
                return match;
            }
        }
        String subjectFullName = normalizeSubjectName(line.getString("subjectFullName"));
        if (subjectFullName != null) {
            AccountingSubject match = subjects.stream()
                    .filter(subject -> subjectFullName.equals(normalizeSubjectName(subjectFullName(subject))))
                    .findFirst()
                    .orElse(null);
            if (match != null) {
                return match;
            }
        }
        String subjectName = normalizeSubjectName(line.getString("subjectName"));
        if (subjectName == null) {
            return null;
        }
        List<AccountingSubject> matches = subjects.stream()
                .filter(subject -> subjectName.equals(normalizeSubjectName(subject.getName()))
                        || normalizeSubjectName(subjectFullName(subject)).endsWith("/" + subjectName))
                .toList();
        return matches.size() == 1 ? matches.getFirst() : null;
    }

    /**
     * 当前账套可用于凭证分录的启用末级科目。
     *
     * <p>实现步骤：按公司读取启用科目，剔除存在启用子级的分组科目，再按编码正序返回。</p>
     */
    private List<AccountingSubject> enabledLeafSubjects() {
        List<AccountingSubject> subjects = subjectRepository.findByOrganizationCodeAndEnabledTrueOrderByCodeAsc(CompanyScope.currentCompanyCode());
        return subjects.stream()
                .filter(subject -> subjects.stream().noneMatch(candidate -> candidate.getParent() != null && Objects.equals(candidate.getParent().getId(), subject.getId())))
                .sorted(Comparator.comparing(AccountingSubject::getCode))
                .toList();
    }

    /**
     * 凭证导入系统提示词。
     */
    private String systemPrompt() {
        return """
                你是财务凭证识别助手。请从用户上传的记账凭证图片、扫描件或PDF文本中识别凭证信息。
                只允许输出严格 JSON，不要输出 Markdown，不要解释。
                无法确认的字段填空字符串或 0，不要编造。
                借方和贷方金额必须是数字，不能带逗号或币种符号。
                科目只能从用户提供的科目清单中选择；不能确定时 subjectId 置空。
                JSON 格式：
                {
                  "voucherDate":"yyyy-MM-dd或空",
                  "summary":"整张凭证摘要或空",
                  "sourceBizNo":"来源单号或空",
                  "lines":[
                    {
                      "subjectId":科目ID或null,
                      "subjectCode":"科目编码或空",
                      "subjectName":"科目末级名称或空",
                      "subjectFullName":"完整科目路径或空",
                      "summary":"分录摘要",
                      "debitAmount":0,
                      "creditAmount":0,
                      "currencyCode":"CNY",
                      "currencyName":"人民币",
                      "exchangeRateToCny":1,
                      "auxiliary":"",
                      "confidence":0.0
                    }
                  ],
                  "warnings":[]
                }
                """;
    }

    /**
     * 凭证导入用户提示词。
     *
     * <p>实现步骤：把当前账套启用末级科目清单拼接给模型，使模型只能从真实科目中选择。</p>
     */
    private String userPrompt(List<AccountingSubject> subjects) {
        StringBuilder builder = new StringBuilder();
        builder.append("请识别下面文件中的凭证分录，返回 JSON。当前账套可用末级科目如下：\n");
        for (AccountingSubject subject : subjects) {
            builder.append("- id=").append(subject.getId())
                    .append(", code=").append(subject.getCode())
                    .append(", name=").append(subjectFullName(subject))
                    .append('\n');
        }
        builder.append("如果图片里只有金额和摘要但科目不确定，请保留摘要和金额，subjectId 置空。");
        return builder.toString();
    }

    /**
     * 解析模型 JSON 内容。
     *
     * <p>实现步骤：去掉可能的代码块包裹，截取首尾大括号后解析，解析失败时返回空结构并提示用户。</p>
     */
    private JSONObject parseJsonObject(String answer) {
        String text = answer == null ? "" : answer.trim();
        text = text.replaceFirst("^```json\\s*", "").replaceFirst("^```\\s*", "").replaceFirst("\\s*```$", "").trim();
        int start = text.indexOf('{');
        int end = text.lastIndexOf('}');
        if (start >= 0 && end > start) {
            text = text.substring(start, end + 1);
        }
        try {
            JSONObject object = JSON.parseObject(text);
            return object == null ? new JSONObject() : object;
        } catch (Exception ex) {
            JSONObject fallback = new JSONObject();
            fallback.put("warnings", List.of("AI识别结果不是有效JSON，请重新上传更清晰的凭证图片或手工录入"));
            return fallback;
        }
    }

    /**
     * 生成科目完整级联名称。
     */
    private String subjectFullName(AccountingSubject subject) {
        List<String> names = new ArrayList<>();
        AccountingSubject cursor = subject;
        int guard = 0;
        while (cursor != null && guard < 20) {
            names.add(cursor.getName());
            cursor = cursor.getParent();
            guard++;
        }
        java.util.Collections.reverse(names);
        return String.join(" / ", names);
    }

    /**
     * 生成图片 data URL。
     */
    private String dataUrl(String contentType, byte[] bytes) {
        return "data:" + contentType + ";base64," + Base64.getEncoder().encodeToString(bytes);
    }

    /**
     * 根据上传文件和后缀解析 MIME 类型。
     */
    private String contentType(MultipartFile file, String suffix) {
        String contentType = file.getContentType();
        if (contentType != null && !contentType.isBlank()) {
            return contentType;
        }
        return switch (suffix) {
            case "jpg", "jpeg" -> "image/jpeg";
            case "webp" -> "image/webp";
            case "bmp" -> "image/bmp";
            default -> "image/png";
        };
    }

    /**
     * 读取文件后缀。
     */
    private String suffix(String fileName) {
        int index = fileName.lastIndexOf('.');
        return index >= 0 && index < fileName.length() - 1 ? fileName.substring(index + 1).toLowerCase(Locale.ROOT) : "";
    }

    /**
     * 清理文件名用于提示。
     */
    private String safeFileName(String value) {
        String fileName = value == null || value.isBlank() ? "未命名文件" : value.trim();
        return fileName.replace("\\", "/").substring(fileName.replace("\\", "/").lastIndexOf('/') + 1);
    }

    /**
     * 标准化金额字段。
     */
    private BigDecimal money(BigDecimal value) {
        return (value == null ? BigDecimal.ZERO : value).setScale(CurrencyService.MONEY_SCALE, RoundingMode.HALF_UP);
    }

    /**
     * 标准化汇率字段。
     */
    private BigDecimal rate(BigDecimal value) {
        BigDecimal rate = value == null || value.signum() <= 0 ? BigDecimal.ONE : value;
        return rate.setScale(CurrencyService.MONEY_SCALE, RoundingMode.HALF_UP);
    }

    /**
     * 标准化可信度字段。
     */
    private BigDecimal confidence(BigDecimal value) {
        if (value == null) {
            return BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
        }
        BigDecimal bounded = value.max(BigDecimal.ZERO).min(BigDecimal.ONE);
        return bounded.setScale(2, RoundingMode.HALF_UP);
    }

    /**
     * 限制文本长度。
     */
    private String limitText(String value, int maxLength) {
        if (value == null) {
            return null;
        }
        String text = value.trim();
        return text.length() <= maxLength ? text : text.substring(0, maxLength);
    }

    /**
     * 解析日期。
     */
    private LocalDate parseDate(String value) {
        try {
            return value == null || value.isBlank() ? null : LocalDate.parse(value.trim());
        } catch (Exception ex) {
            return null;
        }
    }

    /**
     * 转换 JSON 字符串数组。
     */
    private List<String> jsonStringArray(JSONArray array) {
        if (array == null || array.isEmpty()) {
            return List.of();
        }
        List<String> values = new ArrayList<>();
        for (int i = 0; i < array.size(); i++) {
            String value = array.getString(i);
            if (value != null && !value.isBlank()) {
                values.add(value);
            }
        }
        return values;
    }

    /**
     * 取第一个非空文本。
     */
    private String firstText(String... values) {
        for (String value : values) {
            if (value != null && !value.isBlank()) {
                return value.trim();
            }
        }
        return null;
    }

    /**
     * 标准化普通文本。
     */
    private String normalize(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }

    /**
     * 标准化科目名称，去除空格并统一路径分隔符。
     */
    private String normalizeSubjectName(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.trim()
                .replace("／", "/")
                .replace("\\", "/")
                .replace(">", "/")
                .replace("》", "/")
                .replaceAll("\\s+", "");
    }
}
