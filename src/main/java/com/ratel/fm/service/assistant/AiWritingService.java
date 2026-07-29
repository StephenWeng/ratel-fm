package com.ratel.fm.service.assistant;

import com.ratel.fm.common.SearchSpecs;
import com.ratel.fm.domain.auth.PermissionCode;
import com.ratel.fm.domain.purchase.PurchaseOrder;
import com.ratel.fm.domain.purchase.PurchaseOrderLine;
import com.ratel.fm.repository.purchase.PurchaseOrderRepository;
import com.ratel.fm.security.CompanyScope;
import com.ratel.fm.security.SecurityUtils;
import com.ratel.fm.service.agent.BusinessMetricsService;
import com.ratel.fm.service.operation.OperationService;
import com.ratel.fm.web.dto.operation.OperationDtos.PurchaseOrderExportRequest;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.font.PDType0Font;
import org.apache.poi.xslf.usermodel.XMLSlideShow;
import org.apache.poi.xslf.usermodel.XSLFSlide;
import org.apache.poi.xslf.usermodel.XSLFTextBox;
import org.apache.poi.xslf.usermodel.XSLFTextParagraph;
import org.apache.poi.xslf.usermodel.XSLFTextRun;
import org.apache.poi.xssf.usermodel.XSSFRow;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.apache.poi.xwpf.usermodel.XWPFParagraph;
import org.apache.poi.xwpf.usermodel.XWPFRun;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.awt.Rectangle;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import org.springframework.data.domain.Sort;

/**
 * AI 写作文件生成服务。
 *
 * <p>实现步骤：
 * 1. 根据用户自然语言判断目标文件类型和业务主题；
 * 2. 采购类 xlsx 复用现有采购导出服务，确保数据权限和统计口径一致；
 * 3. docx、pdf、pptx 使用 Apache POI 和 PDFBox 生成可下载文件。</p>
 */
@Service
public class AiWritingService {

    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("yyyyMMdd");

    private final OperationService operationService;
    private final PurchaseOrderRepository purchaseOrderRepository;
    private final BusinessMetricsService businessMetricsService;

    public AiWritingService(
            OperationService operationService,
            PurchaseOrderRepository purchaseOrderRepository,
            BusinessMetricsService businessMetricsService
    ) {
        this.operationService = operationService;
        this.purchaseOrderRepository = purchaseOrderRepository;
        this.businessMetricsService = businessMetricsService;
    }

    /**
     * 根据用户问题生成文件。
     */
    @Transactional(readOnly = true)
    public GeneratedFile generate(String question) {
        WritingIntent intent = parseIntent(question);
        String summary = buildSummary(question, intent);
        byte[] content = switch (intent.fileType()) {
            case "xlsx" -> generateXlsx(intent);
            case "docx" -> intent.businessReport() ? generateBusinessReportDocx(question, intent) : generateDocx(question, summary);
            case "pdf" -> intent.businessReport() ? generatePdf(question, businessReportText(question, intent)) : generatePdf(question, summary);
            case "pptx" -> intent.businessReport() ? generateBusinessReportPptx(question, intent) : generatePptx(question, summary);
            default -> generateDocx(question, summary);
        };
        return new GeneratedFile(filename(intent), contentType(intent.fileType()), content, summary);
    }

    private byte[] generateXlsx(WritingIntent intent) {
        if (intent.purchase() && intent.dailyStats()) {
            return generatePurchaseDailyStatsXlsx(intent);
        }
        if (intent.purchase()) {
            return operationService.exportPurchaseOrders(new PurchaseOrderExportRequest(
                    null,
                    intent.startDate(),
                    intent.endDate(),
                    null,
                    null,
                    null,
                    null,
                    null,
                    null
            ));
        }
        return generateGenericXlsx("当前文件类型为 xlsx，但暂未识别到可结构化导出的业务对象。");
    }

    private byte[] generatePurchaseDailyStatsXlsx(WritingIntent intent) {
        var spec = CompanyScope.<PurchaseOrder>currentCompanySpec()
                .and(SearchSpecs.dateBetween("orderDate", intent.startDate(), intent.endDate()));
        List<PurchaseOrder> orders = purchaseOrderRepository.findAll(spec, Sort.by(Sort.Direction.ASC, "orderDate", "id"));
        Map<LocalDate, DailyPurchaseStat> stats = new TreeMap<>();
        for (PurchaseOrder order : orders) {
            DailyPurchaseStat stat = stats.computeIfAbsent(order.getOrderDate(), ignored -> new DailyPurchaseStat());
            stat.orderCount++;
            stat.quantity = stat.quantity.add(sumQuantity(order));
            stat.amount = stat.amount.add(value(order.getTotalAmount()));
            stat.amountCny = stat.amountCny.add(value(order.getTotalAmountCny()));
        }
        try (XSSFWorkbook workbook = new XSSFWorkbook(); ByteArrayOutputStream output = new ByteArrayOutputStream()) {
            XSSFSheet sheet = workbook.createSheet("采购按日统计");
            XSSFRow header = sheet.createRow(0);
            header.createCell(0).setCellValue("日期");
            header.createCell(1).setCellValue("采购单数");
            header.createCell(2).setCellValue("采购数量合计");
            header.createCell(3).setCellValue("采购金额合计");
            header.createCell(4).setCellValue("折人民币金额合计");
            int rowIndex = 1;
            for (Map.Entry<LocalDate, DailyPurchaseStat> entry : stats.entrySet()) {
                XSSFRow row = sheet.createRow(rowIndex++);
                DailyPurchaseStat stat = entry.getValue();
                row.createCell(0).setCellValue(entry.getKey().toString());
                row.createCell(1).setCellValue(stat.orderCount);
                row.createCell(2).setCellValue(stat.quantity.doubleValue());
                row.createCell(3).setCellValue(stat.amount.doubleValue());
                row.createCell(4).setCellValue(stat.amountCny.doubleValue());
            }
            for (int i = 0; i < 5; i++) {
                sheet.setColumnWidth(i, 22 * 256);
            }
            workbook.write(output);
            return output.toByteArray();
        } catch (Exception ex) {
            throw new IllegalStateException("生成采购按日统计 Excel 文件失败", ex);
        }
    }

    private BigDecimal sumQuantity(PurchaseOrder order) {
        return order.getLines() == null ? BigDecimal.ZERO : order.getLines().stream()
                .map(PurchaseOrderLine::getQuantity)
                .map(this::value)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private byte[] generateGenericXlsx(String summary) {
        try (XSSFWorkbook workbook = new XSSFWorkbook(); ByteArrayOutputStream output = new ByteArrayOutputStream()) {
            XSSFSheet sheet = workbook.createSheet("AI写作");
            XSSFRow header = sheet.createRow(0);
            header.createCell(0).setCellValue("项目");
            header.createCell(1).setCellValue("内容");
            XSSFRow row = sheet.createRow(1);
            row.createCell(0).setCellValue("生成说明");
            row.createCell(1).setCellValue(summary);
            sheet.setColumnWidth(0, 18 * 256);
            sheet.setColumnWidth(1, 80 * 256);
            workbook.write(output);
            return output.toByteArray();
        } catch (Exception ex) {
            throw new IllegalStateException("生成 Excel 文件失败", ex);
        }
    }

    private byte[] generateDocx(String question, String summary) {
        try (XWPFDocument document = new XWPFDocument(); ByteArrayOutputStream output = new ByteArrayOutputStream()) {
            XWPFParagraph title = document.createParagraph();
            XWPFRun titleRun = title.createRun();
            titleRun.setBold(true);
            titleRun.setFontSize(18);
            titleRun.setText("AI写作结果");

            writeParagraph(document, "用户问题：" + value(question));
            writeParagraph(document, summary);
            writeParagraph(document, "说明：本文件由 ratel助手按用户选择的 AI写作 意图生成。涉及业务数据时，后端按当前登录人的所属公司、权限和确定性查询口径生成。");
            document.write(output);
            return output.toByteArray();
        } catch (Exception ex) {
            throw new IllegalStateException("生成 Word 文件失败", ex);
        }
    }

    private byte[] generateBusinessReportDocx(String question, WritingIntent intent) {
        BusinessReport report = buildBusinessReport(question, intent);
        try (XWPFDocument document = new XWPFDocument(); ByteArrayOutputStream output = new ByteArrayOutputStream()) {
            XWPFParagraph title = document.createParagraph();
            XWPFRun titleRun = title.createRun();
            titleRun.setBold(true);
            titleRun.setFontSize(20);
            titleRun.setText("经营分析报告");
            writeParagraph(document, "报告期间：" + intent.startDate() + " 至 " + intent.endDate());
            writeParagraph(document, "一、经营概览");
            report.overview().forEach(item -> writeParagraph(document, item));
            writeParagraph(document, "二、风险提示");
            report.risks().forEach(item -> writeParagraph(document, item));
            writeParagraph(document, "三、经营建议");
            report.suggestions().forEach(item -> writeParagraph(document, item));
            writeParagraph(document, "四、数据口径");
            writeParagraph(document, report.scope());
            document.write(output);
            return output.toByteArray();
        } catch (Exception ex) {
            throw new IllegalStateException("生成经营分析 Word 报告失败", ex);
        }
    }

    private void writeParagraph(XWPFDocument document, String text) {
        XWPFParagraph paragraph = document.createParagraph();
        XWPFRun run = paragraph.createRun();
        run.setFontSize(11);
        run.setText(text);
    }

    private byte[] generatePdf(String question, String summary) {
        try (PDDocument document = new PDDocument(); ByteArrayOutputStream output = new ByteArrayOutputStream()) {
            PDPage page = new PDPage(PDRectangle.A4);
            document.addPage(page);
            PDType0Font font = loadChineseFont(document);
            try (PDPageContentStream stream = new PDPageContentStream(document, page)) {
                stream.beginText();
                stream.setFont(font, 13);
                stream.setLeading(20);
                stream.newLineAtOffset(54, 780);
                for (String line : ("AI写作结果\n用户问题：" + value(question) + "\n" + summary).split("\n")) {
                    stream.showText(line.length() > 52 ? line.substring(0, 52) : line);
                    stream.newLine();
                }
                stream.endText();
            }
            document.save(output);
            return output.toByteArray();
        } catch (Exception ex) {
            throw new IllegalStateException("生成 PDF 文件失败", ex);
        }
    }

    private PDType0Font loadChineseFont(PDDocument document) throws Exception {
        for (String path : new String[]{
                "C:/Windows/Fonts/simhei.ttf",
                "C:/Windows/Fonts/msyh.ttf",
                "C:/Windows/Fonts/msyh.ttc",
                "/usr/share/fonts/opentype/noto/NotoSansCJK-Regular.otf",
                "/usr/share/fonts/truetype/noto/NotoSansCJK-Regular.ttf",
                "/usr/share/fonts/truetype/dejavu/DejaVuSans.ttf"
        }) {
            File file = new File(path);
            if (file.exists()) {
                return PDType0Font.load(document, file);
            }
        }
        throw new IllegalStateException("未找到可用中文字体，无法生成 PDF");
    }

    private byte[] generatePptx(String question, String summary) {
        try (XMLSlideShow ppt = new XMLSlideShow(); ByteArrayOutputStream output = new ByteArrayOutputStream()) {
            ppt.setPageSize(new java.awt.Dimension(1280, 720));
            XSLFSlide slide = ppt.createSlide();
            XSLFTextBox title = slide.createTextBox();
            title.setAnchor(new Rectangle(60, 50, 1120, 80));
            XSLFTextRun titleRun = title.addNewTextParagraph().addNewTextRun();
            titleRun.setText("AI写作结果");
            titleRun.setBold(true);
            titleRun.setFontSize(32.0);

            XSLFTextBox body = slide.createTextBox();
            body.setAnchor(new Rectangle(70, 150, 1080, 420));
            XSLFTextParagraph paragraph = body.addNewTextParagraph();
            XSLFTextRun bodyRun = paragraph.addNewTextRun();
            bodyRun.setFontSize(20.0);
            bodyRun.setText("用户问题：" + value(question) + "\n" + summary);
            ppt.write(output);
            return output.toByteArray();
        } catch (Exception ex) {
            throw new IllegalStateException("生成 PPT 文件失败", ex);
        }
    }

    private byte[] generateBusinessReportPptx(String question, WritingIntent intent) {
        BusinessReport report = buildBusinessReport(question, intent);
        try (XMLSlideShow ppt = new XMLSlideShow(); ByteArrayOutputStream output = new ByteArrayOutputStream()) {
            ppt.setPageSize(new java.awt.Dimension(1280, 720));
            addReportSlide(ppt, "经营分析报告", List.of("报告期间：" + intent.startDate() + " 至 " + intent.endDate(), report.scope()));
            addReportSlide(ppt, "经营概览", report.overview());
            addReportSlide(ppt, "风险提示与建议", combine(report.risks(), report.suggestions()));
            ppt.write(output);
            return output.toByteArray();
        } catch (Exception ex) {
            throw new IllegalStateException("生成经营分析 PPT 报告失败", ex);
        }
    }

    private void addReportSlide(XMLSlideShow ppt, String titleText, List<String> lines) {
        XSLFSlide slide = ppt.createSlide();
        XSLFTextBox title = slide.createTextBox();
        title.setAnchor(new Rectangle(60, 50, 1120, 70));
        XSLFTextRun titleRun = title.addNewTextParagraph().addNewTextRun();
        titleRun.setText(titleText);
        titleRun.setBold(true);
        titleRun.setFontSize(30.0);
        XSLFTextBox body = slide.createTextBox();
        body.setAnchor(new Rectangle(80, 145, 1080, 460));
        XSLFTextParagraph paragraph = body.addNewTextParagraph();
        XSLFTextRun run = paragraph.addNewTextRun();
        run.setFontSize(20.0);
        run.setText(String.join("\n", lines));
    }

    private WritingIntent parseIntent(String question) {
        String text = value(question).toLowerCase(Locale.ROOT);
        String fileType = text.contains("ppt") || text.contains("演示") ? "pptx"
                : text.contains("pdf") ? "pdf"
                : text.contains("word") || text.contains("doc") || text.contains("文档") ? "docx"
                : text.contains("excel") || text.contains("xlsx") || text.contains("表格") || text.contains("清单") || text.contains("统计") ? "xlsx"
                : "docx";
        YearMonth month = text.contains("上月") ? YearMonth.now().minusMonths(1) : YearMonth.now();
        boolean purchase = text.contains("采购");
        boolean dailyStats = text.contains("每天") || text.contains("每日") || text.contains("按天") || text.contains("按日");
        boolean businessReport = text.contains("经营") || text.contains("经营分析") || text.contains("经营情况") || text.contains("经营报告");
        return new WritingIntent(fileType, purchase, dailyStats, businessReport, month.atDay(1), month.atEndOfMonth());
    }

    private String buildSummary(String question, WritingIntent intent) {
        StringBuilder builder = new StringBuilder();
        builder.append("结论：已按 AI写作 意图生成文件。");
        builder.append("\n关键依据：文件类型=").append(intent.fileType());
        builder.append("；时间范围=").append(intent.startDate()).append(" 至 ").append(intent.endDate());
        builder.append("；业务对象=").append(intent.purchase() ? "采购单" : "通用写作文档");
        if (intent.businessReport()) {
            builder.append("；报告类型=经营分析报告");
        }
        builder.append("；统计维度=").append(intent.dailyStats() ? "按天" : "明细或通用文档");
        if (intent.purchase() && "xlsx".equals(intent.fileType())) {
            builder.append("；数据口径=当前所属公司内采购单").append(intent.dailyStats() ? "按日聚合统计。" : "列表。");
        }
        builder.append("\n用户原始需求：").append(value(question));
        return builder.toString();
    }

    private String filename(WritingIntent intent) {
        String prefix = intent.businessReport() ? "经营分析报告" : intent.purchase() ? "采购清单" : "AI写作";
        return prefix + "_" + LocalDate.now().format(DATE_FORMAT) + "." + intent.fileType();
    }

    private String contentType(String fileType) {
        return switch (fileType) {
            case "xlsx" -> "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet";
            case "docx" -> "application/vnd.openxmlformats-officedocument.wordprocessingml.document";
            case "pptx" -> "application/vnd.openxmlformats-officedocument.presentationml.presentation";
            case "pdf" -> "application/pdf";
            default -> "application/octet-stream";
        };
    }

    private String value(String text) {
        return text == null ? "" : text.trim();
    }

    public record GeneratedFile(String filename, String contentType, byte[] content, String summary) {
    }

    private BigDecimal value(BigDecimal number) {
        return number == null ? BigDecimal.ZERO : number;
    }

    private String businessReportText(String question, WritingIntent intent) {
        BusinessReport report = buildBusinessReport(question, intent);
        return "经营分析报告\n报告期间：" + intent.startDate() + " 至 " + intent.endDate()
                + "\n\n一、经营概览\n" + String.join("\n", report.overview())
                + "\n\n二、风险提示\n" + String.join("\n", report.risks())
                + "\n\n三、经营建议\n" + String.join("\n", report.suggestions())
                + "\n\n四、数据口径\n" + report.scope();
    }

    private BusinessReport buildBusinessReport(String question, WritingIntent intent) {
        Set<PermissionCode> permissions = SecurityUtils.currentUser().permissions();
        if (!permissions.contains(PermissionCode.REPORT_VIEW)) {
            return new BusinessReport(
                    List.of("1、当前用户缺少统计报表查看权限，系统未读取经营统计数据。"),
                    List.of("1、权限不足时不能生成经营结论，避免越权展示或误导。"),
                    List.of("1、请由管理员授予报表查看权限后重新生成。"),
                    "权限口径：未授权 REPORT_VIEW，未读取经营分析数据。"
            );
        }
        BusinessMetricsService.BusinessMetricsSnapshot metrics = businessMetricsService.snapshot(null, null, permissions);
        List<String> overview = List.of(
                "1、采购人民币金额合计：" + metrics.purchaseTotal().toPlainString() + "。",
                "2、往来未结人民币金额合计：" + metrics.remainingTotal().toPlainString() + "。",
                "3、有库存数量的物料数：" + metrics.materialKinds() + "。",
                "4、负库存物料数：" + metrics.negativeStockKinds() + "。"
        );
        List<String> risks = new java.util.ArrayList<>();
        if (metrics.remainingTotal().compareTo(BigDecimal.ZERO) > 0) {
            risks.add("1、存在未结往来余额，需要按客户、供应商和到期日继续拆分。");
        }
        if (metrics.negativeStockKinds() > 0) {
            risks.add((risks.size() + 1) + "、存在负库存物料，库存准确性会影响经营判断。");
        }
        if (risks.isEmpty()) {
            risks.add("1、当前结构化指标未发现未结往来或负库存风险。");
        }
        List<String> suggestions = List.of(
                "1、优先跟进未结往来余额，按账龄和金额排序处理。",
                "2、复核负库存和库存数量异常物料，先保证库存台账准确。",
                "3、后续可按项目、供应商、客户和物料维度继续生成拆分报告。"
        );
        return new BusinessReport(overview, risks, suggestions, "数据口径：当前登录人所属公司、当前权限范围内的采购、应收应付和库存结构化数据。用户需求：" + value(question));
    }

    private List<String> combine(List<String> first, List<String> second) {
        List<String> result = new java.util.ArrayList<>();
        result.addAll(first);
        result.addAll(second);
        return result;
    }

    private record WritingIntent(String fileType, boolean purchase, boolean dailyStats, boolean businessReport, LocalDate startDate, LocalDate endDate) {
    }

    private record BusinessReport(List<String> overview, List<String> risks, List<String> suggestions, String scope) {
    }

    private static final class DailyPurchaseStat {
        private int orderCount;
        private BigDecimal quantity = BigDecimal.ZERO;
        private BigDecimal amount = BigDecimal.ZERO;
        private BigDecimal amountCny = BigDecimal.ZERO;
    }
}
