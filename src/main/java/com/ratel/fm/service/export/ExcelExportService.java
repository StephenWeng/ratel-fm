package com.ratel.fm.service.export;

import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.Font;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.function.Function;

/**
 * Excel 导出服务。
 *
 * <p>负责把业务列表数据转换为 xlsx 字节数组，业务模块只需要提供表头和字段取值逻辑。</p>
 *
 * <p>开发组织：ratel；开发人员：WenZhang；联系方式：18782945613。</p>
 *
 * @author ratel
 */
@Service
public class ExcelExportService {

    /**
     * 日期字段导出格式，统一输出 yyyy-MM-dd。
     */
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    /**
     * 日期时间字段导出格式，统一输出 yyyy-MM-dd HH:mm:ss。
     */
    private static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    /**
     * 导出 Excel。
     *
     * <p>实现步骤：
     * 1. 创建工作簿和工作表；
     * 2. 写入表头并设置加粗样式；
     * 3. 按业务列定义逐行写入数据；
     * 4. 根据列数做基础列宽控制；
     * 5. 输出为 xlsx 字节数组。</p>
     */
    public <T> byte[] export(String sheetName, List<ExcelColumn<T>> columns, List<T> rows) {
        try (XSSFWorkbook workbook = new XSSFWorkbook(); ByteArrayOutputStream output = new ByteArrayOutputStream()) {
            // 变量说明：sheet 保存当前步骤计算、查询或转换得到的中间结果。
            Sheet sheet = workbook.createSheet(safeSheetName(sheetName));
            // 变量说明：headerStyle 保存当前步骤计算、查询或转换得到的中间结果。
            CellStyle headerStyle = createHeaderStyle(workbook);
            writeHeader(sheet, columns, headerStyle);
            writeRows(sheet, columns, rows);
            applyColumnWidth(sheet, columns.size());
            workbook.write(output);
            return output.toByteArray();
        } catch (Exception ex) {
            throw new IllegalStateException("导出 Excel 失败", ex);
        }
    }

    /**
     * 写入表头。
     */
    private <T> void writeHeader(Sheet sheet, List<ExcelColumn<T>> columns, CellStyle headerStyle) {
        // 变量说明：row 保存当前步骤计算、查询或转换得到的中间结果。
        Row row = sheet.createRow(0);
        for (int columnIndex = 0; columnIndex < columns.size(); columnIndex++) {
            // 变量说明：cell 保存当前步骤计算、查询或转换得到的中间结果。
            Cell cell = row.createCell(columnIndex);
            cell.setCellValue(columns.get(columnIndex).title());
            cell.setCellStyle(headerStyle);
        }
    }

    /**
     * 写入业务数据。
     *
     * <p>实现步骤：逐行读取业务对象，再按列定义获取值，最后根据值类型写入单元格。</p>
     */
    private <T> void writeRows(Sheet sheet, List<ExcelColumn<T>> columns, List<T> rows) {
        for (int rowIndex = 0; rowIndex < rows.size(); rowIndex++) {
            // 变量说明：row 保存当前步骤计算、查询或转换得到的中间结果。
            Row row = sheet.createRow(rowIndex + 1);
            // 变量说明：source 保存当前步骤计算、查询或转换得到的中间结果。
            T source = rows.get(rowIndex);
            for (int columnIndex = 0; columnIndex < columns.size(); columnIndex++) {
                // 变量说明：value 保存当前步骤计算、查询或转换得到的中间结果。
                Object value = columns.get(columnIndex).extractor().apply(source);
                writeCell(row.createCell(columnIndex), value);
            }
        }
    }

    /**
     * 根据值类型写入单元格。
     */
    private void writeCell(Cell cell, Object value) {
        if (value == null) {
            cell.setBlank();
        } else if (value instanceof BigDecimal decimal) {
            cell.setCellValue(decimal.doubleValue());
        } else if (value instanceof Number number) {
            cell.setCellValue(number.doubleValue());
        } else if (value instanceof Boolean bool) {
            cell.setCellValue(bool ? "是" : "否");
        } else if (value instanceof LocalDate date) {
            cell.setCellValue(DATE_FORMATTER.format(date));
        } else if (value instanceof LocalDateTime dateTime) {
            cell.setCellValue(DATE_TIME_FORMATTER.format(dateTime));
        } else {
            cell.setCellValue(String.valueOf(value));
        }
    }

    /**
     * 创建表头样式。
     */
    private CellStyle createHeaderStyle(XSSFWorkbook workbook) {
        // 变量说明：font 保存当前步骤计算、查询或转换得到的中间结果。
        Font font = workbook.createFont();
        font.setBold(true);
        // 变量说明：style 保存当前步骤计算、查询或转换得到的中间结果。
        CellStyle style = workbook.createCellStyle();
        style.setFont(font);
        return style;
    }

    /**
     * 设置基础列宽。
     *
     * <p>说明：按固定宽度处理，避免大数据导出时 autoSizeColumn 扫描全部内容造成额外耗时。</p>
     */
    private void applyColumnWidth(Sheet sheet, int columnSize) {
        for (int columnIndex = 0; columnIndex < columnSize; columnIndex++) {
            sheet.setColumnWidth(columnIndex, 18 * 256);
        }
    }

    /**
     * 清理工作表名称中的非法字符。
     */
    private String safeSheetName(String sheetName) {
        // 变量说明：value 保存当前步骤计算、查询或转换得到的中间结果。
        String value = sheetName == null || sheetName.isBlank() ? "导出数据" : sheetName;
        return value.replaceAll("[\\\\/?*\\[\\]:]", "_");
    }

    /**
     * Excel 列定义。
     *
     * @param title     表头名称
     * @param extractor 从业务对象提取列值的函数
     */
    public record ExcelColumn<T>(String title, Function<T, Object> extractor) {
    }
}
