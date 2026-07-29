package com.ratel.fm.web.phasetwo;

import com.ratel.fm.common.ApiResponse;
import com.ratel.fm.domain.inventory.InventoryMovementType;
import com.ratel.fm.domain.receivable.ArApStatus;
import com.ratel.fm.domain.receivable.ArApType;
import com.ratel.fm.service.assistant.AiAssistantService;
import com.ratel.fm.service.assistant.AiAssistantStreamService;
import com.ratel.fm.service.assistant.AiWritingService;
import com.ratel.fm.service.inventory.InventoryService;
import com.ratel.fm.service.receivable.ArApService;
import com.ratel.fm.service.report.FinancialStatementService;
import com.ratel.fm.web.dto.phasetwo.PhaseTwoDtos.*;
import com.ratel.fm.web.dto.knowledge.KnowledgeDtos.AiAssistantResponse;
import com.ratel.fm.web.dto.common.BatchIdsRequest;
import com.ratel.fm.web.dto.operationlog.BusinessOperationLogDtos.BusinessOperationLogPage;
import com.ratel.fm.web.export.ExcelDownload;
import com.github.xiaoymin.knife4j.annotations.ApiOperationSupport;
import com.github.xiaoymin.knife4j.annotations.ApiSupport;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.MediaType;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.time.LocalDate;
import java.nio.charset.StandardCharsets;
import java.time.OffsetDateTime;
import java.util.List;

@Tag(name = "二期扩展能力")
@ApiSupport(order = 50, author = "ratel / WenZhang / 18782945613")
@RestController
@RequestMapping("/api")
/**
 * PhaseTwoController 类。
 * 
 * <p>用于承载 PhaseTwoController 相关的业务数据、接口契约或处理逻辑，便于维护人员快速定位模块职责。</p>
 */
public class PhaseTwoController {

    /**
     * 字段 inventoryService：保存 inventoryService 对应的业务数据、运行配置或依赖对象，供本类逻辑读取和维护。
     */
    private final InventoryService inventoryService;
    /**
     * 字段 arApService：保存 arApService 对应的业务数据、运行配置或依赖对象，供本类逻辑读取和维护。
     */
    private final ArApService arApService;
    /**
     * 字段 financialStatementService：保存 financialStatementService 对应的业务数据、运行配置或依赖对象，供本类逻辑读取和维护。
     */
    private final FinancialStatementService financialStatementService;
    /**
     * 字段 aiAssistantService：保存 aiAssistantService 对应的业务数据、运行配置或依赖对象，供本类逻辑读取和维护。
     */
    private final AiAssistantService aiAssistantService;
    /**
     * 字段 aiAssistantStreamService：保存 ratel助手 SSE 流式输出服务。
     */
    private final AiAssistantStreamService aiAssistantStreamService;
    private final AiWritingService aiWritingService;

    /**
     * 构造 PhaseTwoController 实例。
     * 
     * <p>实现步骤：
     * 1. 接收调用方传入的依赖对象或初始化参数；
     * 2. 保存到成员字段，保证后续业务方法可以复用；
     * 3. 完成实例初始化。</p>
     */
    public PhaseTwoController(
            InventoryService inventoryService,
            ArApService arApService,
            FinancialStatementService financialStatementService,
            AiAssistantService aiAssistantService,
            AiAssistantStreamService aiAssistantStreamService,
            AiWritingService aiWritingService
    ) {
        this.inventoryService = inventoryService;
        this.arApService = arApService;
        this.financialStatementService = financialStatementService;
        this.aiAssistantService = aiAssistantService;
        this.aiAssistantStreamService = aiAssistantStreamService;
        this.aiWritingService = aiWritingService;
    }

    @ApiOperationSupport(order = 10, author = "ratel / WenZhang / 18782945613")
    @Operation(summary = "库存台账列表", description = "作者：ratel；开发人员：WenZhang；联系方式：18782945613。支持入库、出库、调拨、盘点流水查询。")
    @PreAuthorize("hasAuthority('INVENTORY_MANAGE') or hasAuthority('REPORT_VIEW')")
    @GetMapping("/inventory-ledgers")
    /**
     * 执行 listInventory 方法。
     * 
     * <p>实现步骤：
     * 1. 接收并校验调用方传入的数据；
     * 2. 按当前方法职责执行业务查询、转换或持久化处理；
     * 3. 返回处理结果或更新对象状态。</p>
     */
    public ApiResponse<List<InventoryView>> listInventory(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            @RequestParam(required = false) String movementNo,
            @RequestParam(required = false) InventoryMovementType movementType,
            @RequestParam(required = false) String itemName,
            @RequestParam(required = false) String projectCode,
            @RequestParam(required = false) String fromWarehouse,
            @RequestParam(required = false) String toWarehouse,
            @RequestParam(required = false) String relatedBizNo
    ) {
        return ApiResponse.ok(inventoryService.list(startDate, endDate, movementNo, movementType,
                itemName, projectCode, fromWarehouse, toWarehouse, relatedBizNo));
    }

    @ApiOperationSupport(order = 12, author = "ratel / WenZhang / 18782945613")
    @Operation(summary = "查询库存可用量", description = "按物料、仓库和日期查询可用库存，用于出库和调拨前校验。")
    @PreAuthorize("hasAuthority('INVENTORY_MANAGE') or hasAuthority('REPORT_VIEW')")
    @GetMapping("/inventory-ledgers/stock")
    /**
     * 执行 inventoryStock 方法。
     * 
     * <p>实现步骤：
     * 1. 接收并校验调用方传入的数据；
     * 2. 按当前方法职责执行业务查询、转换或持久化处理；
     * 3. 返回处理结果或更新对象状态。</p>
     */
    public ApiResponse<InventoryStockView> inventoryStock(
            @RequestParam String itemCode,
            @RequestParam String warehouse,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate asOfDate
    ) {
        return ApiResponse.ok(inventoryService.stock(itemCode, warehouse, asOfDate));
    }

    @ApiOperationSupport(order = 13, author = "ratel / WenZhang / 18782945613")
    @Operation(summary = "物料库存统计", description = "按物料字典层级统计入库总数、出库总数、调拨总数和库存数量。")
    @PreAuthorize("hasAuthority('INVENTORY_MANAGE') or hasAuthority('REPORT_VIEW')")
    @GetMapping("/inventory-ledgers/material-stock")
    /**
     * 执行 materialStock 方法。
     * 
     * <p>实现步骤：
     * 1. 接收并校验调用方传入的数据；
     * 2. 按当前方法职责执行业务查询、转换或持久化处理；
     * 3. 返回处理结果或更新对象状态。</p>
     */
    public ApiResponse<List<InventoryMaterialStockView>> materialStock() {
        return ApiResponse.ok(inventoryService.materialStock());
    }

    @ApiOperationSupport(order = 15, author = "ratel / WenZhang / 18782945613")
    @Operation(summary = "导出库存台账列表", description = "作者：ratel；开发人员：WenZhang；联系方式：18782945613。选中行优先导出，未选中时按当前搜索条件导出。")
    @PreAuthorize("hasAuthority('INVENTORY_MANAGE') or hasAuthority('REPORT_VIEW')")
    @PostMapping("/inventory-ledgers/export")
    /**
     * 执行 exportInventory 方法。
     * 
     * <p>实现步骤：
     * 1. 接收并校验调用方传入的数据；
     * 2. 按当前方法职责执行业务查询、转换或持久化处理；
     * 3. 返回处理结果或更新对象状态。</p>
     */
    public ResponseEntity<byte[]> exportInventory(@RequestBody(required = false) InventoryExportRequest request) {
        return ExcelDownload.response("库存台账.xlsx", inventoryService.export(request));
    }

    @ApiOperationSupport(order = 20, author = "ratel / WenZhang / 18782945613")
    @Operation(summary = "新增库存流水", description = "作者：ratel；开发人员：WenZhang；联系方式：18782945613。")
    @PreAuthorize("hasAuthority('INVENTORY_MANAGE')")
    @PostMapping("/inventory-ledgers")
    /**
     * 执行 createInventory 方法。
     * 
     * <p>实现步骤：
     * 1. 接收并校验调用方传入的数据；
     * 2. 按当前方法职责执行业务查询、转换或持久化处理；
     * 3. 返回处理结果或更新对象状态。</p>
     */
    public ApiResponse<InventoryView> createInventory(@Valid @RequestBody InventoryRequest request) {
        return ApiResponse.ok("库存流水已创建", inventoryService.create(request));
    }

    @ApiOperationSupport(order = 25, author = "ratel / WenZhang / 18782945613")
    @Operation(summary = "批量删除库存流水", description = "作者：ratel；开发人员：WenZhang；联系方式：18782945613。前端必须二次确认后调用。")
    @PreAuthorize("hasAuthority('INVENTORY_MANAGE')")
    @PostMapping("/inventory-ledgers/batch-delete")
    /**
     * 执行 deleteInventory 方法。
     * 
     * <p>实现步骤：
     * 1. 接收并校验调用方传入的数据；
     * 2. 按当前方法职责执行业务查询、转换或持久化处理；
     * 3. 返回处理结果或更新对象状态。</p>
     */
    public ApiResponse<Void> deleteInventory(@Valid @RequestBody BatchIdsRequest request) {
        inventoryService.delete(request.ids());
        return ApiResponse.ok("库存流水已批量删除", null);
    }

    @ApiOperationSupport(order = 26, author = "ratel / WenZhang / 18782945613")
    @Operation(summary = "查询库存流水操作流水", description = "作者：ratel；开发人员：WenZhang；联系方式：18782945613。按操作时间倒序分页返回库存流水新增、删除等操作记录。")
    @PreAuthorize("hasAuthority('INVENTORY_MANAGE') or hasAuthority('REPORT_VIEW')")
    @GetMapping("/inventory-ledgers/{id}/operation-logs")
    /**
     * 执行 listInventoryOperationLogs 方法。
     * 
     * <p>实现步骤：
     * 1. 接收并校验调用方传入的数据；
     * 2. 按当前方法职责执行业务查询、转换或持久化处理；
     * 3. 返回处理结果或更新对象状态。</p>
     */
    public ApiResponse<BusinessOperationLogPage> listInventoryOperationLogs(
            @PathVariable Long id,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) OffsetDateTime startTime,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) OffsetDateTime endTime,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        return ApiResponse.ok(inventoryService.pageOperationLogs(id, startTime, endTime, page, size));
    }

    @ApiOperationSupport(order = 30, author = "ratel / WenZhang / 18782945613")
    @Operation(summary = "应收应付列表", description = "作者：ratel；开发人员：WenZhang；联系方式：18782945613。包含客户/供应商、账龄和付款计划。")
    @PreAuthorize("hasAuthority('AR_AP_MANAGE') or hasAuthority('REPORT_VIEW')")
    @GetMapping("/ar-ap-bills")
    /**
     * 执行 listArAp 方法。
     * 
     * <p>实现步骤：
     * 1. 接收并校验调用方传入的数据；
     * 2. 按当前方法职责执行业务查询、转换或持久化处理；
     * 3. 返回处理结果或更新对象状态。</p>
     */
    public ApiResponse<List<ArApView>> listArAp(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            @RequestParam(required = false) String billNo,
            @RequestParam(required = false) ArApType billType,
            @RequestParam(required = false) String partnerName,
            @RequestParam(required = false) String projectCode,
            @RequestParam(required = false) ArApStatus status,
            @RequestParam(required = false) String paymentPlan
    ) {
        return ApiResponse.ok(arApService.list(startDate, endDate, billNo, billType, partnerName, projectCode, status, paymentPlan));
    }

    @ApiOperationSupport(order = 32, author = "ratel / WenZhang / 18782945613")
    @Operation(summary = "收付统计", description = "按项目和客户/供应商统计每张应收应付单的应收、应付、待收和待付金额，并返回汇总。")
    @PreAuthorize("hasAuthority('AR_AP_MANAGE') or hasAuthority('REPORT_VIEW')")
    @GetMapping("/ar-ap-bills/payment-stats")
    /**
     * 执行 arApPaymentStats 方法。
     *
     * <p>实现步骤：
     * 1. 接收项目编码和客户/供应商筛选条件；
     * 2. 调用应收应付服务按人民币金额快照统计；
     * 3. 返回明细行与合计金额。</p>
     */
    public ApiResponse<ArApPaymentStatsView> arApPaymentStats(
            @RequestParam(required = false) String projectCode,
            @RequestParam(required = false) String partnerName
    ) {
        return ApiResponse.ok(arApService.paymentStats(projectCode, partnerName));
    }

    @ApiOperationSupport(order = 34, author = "ratel / WenZhang / 18782945613")
    @Operation(summary = "导出收付统计", description = "按当前项目和客户/供应商筛选条件导出收付统计明细。")
    @PreAuthorize("hasAuthority('AR_AP_MANAGE') or hasAuthority('REPORT_VIEW')")
    @PostMapping("/ar-ap-bills/payment-stats/export")
    /**
     * 导出收付统计。
     *
     * <p>实现步骤：
     * 1. 接收项目和客户/供应商筛选条件；
     * 2. 调用应收应付服务导出同口径统计明细；
     * 3. 返回 xlsx 文件字节流。</p>
     */
    public ResponseEntity<byte[]> exportArApPaymentStats(@RequestBody(required = false) ArApPaymentStatsExportRequest request) {
        return ExcelDownload.response("收付统计.xlsx", arApService.exportPaymentStats(request));
    }

    @ApiOperationSupport(order = 35, author = "ratel / WenZhang / 18782945613")
    @Operation(summary = "导出应收应付列表", description = "作者：ratel；开发人员：WenZhang；联系方式：18782945613。选中行优先导出，未选中时按当前搜索条件导出。")
    @PreAuthorize("hasAuthority('AR_AP_MANAGE') or hasAuthority('REPORT_VIEW')")
    @PostMapping("/ar-ap-bills/export")
    /**
     * 执行 exportArAp 方法。
     * 
     * <p>实现步骤：
     * 1. 接收并校验调用方传入的数据；
     * 2. 按当前方法职责执行业务查询、转换或持久化处理；
     * 3. 返回处理结果或更新对象状态。</p>
     */
    public ResponseEntity<byte[]> exportArAp(@RequestBody(required = false) ArApExportRequest request) {
        return ExcelDownload.response("应收应付.xlsx", arApService.export(request));
    }

    @ApiOperationSupport(order = 40, author = "ratel / WenZhang / 18782945613")
    @Operation(summary = "新增应收应付单", description = "作者：ratel；开发人员：WenZhang；联系方式：18782945613。")
    @PreAuthorize("hasAuthority('AR_AP_MANAGE')")
    @PostMapping("/ar-ap-bills")
    /**
     * 执行 createArAp 方法。
     * 
     * <p>实现步骤：
     * 1. 接收并校验调用方传入的数据；
     * 2. 按当前方法职责执行业务查询、转换或持久化处理；
     * 3. 返回处理结果或更新对象状态。</p>
     */
    public ApiResponse<ArApView> createArAp(@Valid @RequestBody ArApRequest request) {
        return ApiResponse.ok("应收应付单已创建", arApService.create(request));
    }

    @ApiOperationSupport(order = 42, author = "ratel / WenZhang / 18782945613")
    @Operation(summary = "查询应收应付核销流水", description = "作者：ratel；开发人员：WenZhang；联系方式：18782945613。用于查看每张应收应付单的收款、付款或核销明细。")
    @PreAuthorize("hasAuthority('AR_AP_MANAGE') or hasAuthority('REPORT_VIEW')")
    @GetMapping("/ar-ap-bills/{id}/settlements")
    /**
     * 查询应收应付收付核销流水。
     *
     * <p>实现步骤：接收应收应付单主键，调用服务层按当前账套返回核销明细。</p>
     */
    public ApiResponse<List<ArApSettlementView>> listArApSettlements(@PathVariable Long id) {
        return ApiResponse.ok(arApService.listSettlements(id));
    }

    @ApiOperationSupport(order = 43, author = "ratel / WenZhang / 18782945613")
    @Operation(summary = "新增应收应付核销", description = "作者：ratel；开发人员：WenZhang；联系方式：18782945613。核销金额不能超过单据未结金额。")
    @PreAuthorize("hasAuthority('AR_AP_MANAGE')")
    @PostMapping("/ar-ap-bills/{id}/settlements")
    /**
     * 新增应收应付收付核销。
     *
     * <p>实现步骤：校验请求体后调用服务层新增核销流水，并返回更新后的应收应付单状态。</p>
     */
    public ApiResponse<ArApView> settleArAp(@PathVariable Long id, @Valid @RequestBody ArApSettlementRequest request) {
        return ApiResponse.ok("核销成功", arApService.settle(id, request));
    }

    @ApiOperationSupport(order = 45, author = "ratel / WenZhang / 18782945613")
    @Operation(summary = "批量删除应收应付单", description = "作者：ratel；开发人员：WenZhang；联系方式：18782945613。前端必须二次确认后调用。")
    @PreAuthorize("hasAuthority('AR_AP_MANAGE')")
    @PostMapping("/ar-ap-bills/batch-delete")
    /**
     * 执行 deleteArAp 方法。
     * 
     * <p>实现步骤：
     * 1. 接收并校验调用方传入的数据；
     * 2. 按当前方法职责执行业务查询、转换或持久化处理；
     * 3. 返回处理结果或更新对象状态。</p>
     */
    public ApiResponse<Void> deleteArAp(@Valid @RequestBody BatchIdsRequest request) {
        arApService.delete(request.ids());
        return ApiResponse.ok("应收应付单已批量删除", null);
    }

    @ApiOperationSupport(order = 46, author = "ratel / WenZhang / 18782945613")
    @Operation(summary = "查询应收应付操作流水", description = "作者：ratel；开发人员：WenZhang；联系方式：18782945613。按操作时间倒序分页返回应收应付单新增、删除等操作记录。")
    @PreAuthorize("hasAuthority('AR_AP_MANAGE') or hasAuthority('REPORT_VIEW')")
    @GetMapping("/ar-ap-bills/{id}/operation-logs")
    /**
     * 执行 listArApOperationLogs 方法。
     * 
     * <p>实现步骤：
     * 1. 接收并校验调用方传入的数据；
     * 2. 按当前方法职责执行业务查询、转换或持久化处理；
     * 3. 返回处理结果或更新对象状态。</p>
     */
    public ApiResponse<BusinessOperationLogPage> listArApOperationLogs(
            @PathVariable Long id,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) OffsetDateTime startTime,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) OffsetDateTime endTime,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        return ApiResponse.ok(arApService.pageOperationLogs(id, startTime, endTime, page, size));
    }

    @ApiOperationSupport(order = 50, author = "ratel / WenZhang / 18782945613")
    @Operation(summary = "资产负债表", description = "作者：ratel；开发人员：WenZhang；联系方式：18782945613。基础版按科目类别汇总已过账凭证。")
    @PreAuthorize("hasAuthority('REPORT_VIEW')")
    @GetMapping("/finance/reports/balance-sheet")
    /**
     * 执行 balanceSheet 方法。
     * 
     * <p>实现步骤：
     * 1. 接收并校验调用方传入的数据；
     * 2. 按当前方法职责执行业务查询、转换或持久化处理；
     * 3. 返回处理结果或更新对象状态。</p>
     */
    public ApiResponse<FinancialStatement> balanceSheet(@RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        return ApiResponse.ok(financialStatementService.balanceSheet(date));
    }

    @ApiOperationSupport(order = 60, author = "ratel / WenZhang / 18782945613")
    @Operation(summary = "利润表", description = "作者：ratel；开发人员：WenZhang；联系方式：18782945613。基础版按收入、成本、费用类科目汇总。")
    @PreAuthorize("hasAuthority('REPORT_VIEW')")
    @GetMapping("/finance/reports/income-statement")
    /**
     * 执行 incomeStatement 方法。
     * 
     * <p>实现步骤：
     * 1. 接收并校验调用方传入的数据；
     * 2. 按当前方法职责执行业务查询、转换或持久化处理；
     * 3. 返回处理结果或更新对象状态。</p>
     */
    public ApiResponse<FinancialStatement> incomeStatement(@RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        return ApiResponse.ok(financialStatementService.incomeStatement(date));
    }

    @ApiOperationSupport(order = 70, author = "ratel / WenZhang / 18782945613")
    @Operation(summary = "现金流量表", description = "作者：ratel；开发人员：WenZhang；联系方式：18782945613。基础版现金流量表，后续可接入现金流量项目映射。")
    @PreAuthorize("hasAuthority('REPORT_VIEW')")
    @GetMapping("/finance/reports/cash-flow")
    /**
     * 执行 cashFlow 方法。
     * 
     * <p>实现步骤：
     * 1. 接收并校验调用方传入的数据；
     * 2. 按当前方法职责执行业务查询、转换或持久化处理；
     * 3. 返回处理结果或更新对象状态。</p>
     */
    public ApiResponse<FinancialStatement> cashFlow(@RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        return ApiResponse.ok(financialStatementService.cashFlowStatement(date));
    }

    @ApiOperationSupport(order = 80, author = "ratel / WenZhang / 18782945613")
    @Operation(summary = "ratel助手", description = "作者：ratel；开发人员：WenZhang；联系方式：18782945613。当前为规则型助手，提供自然语言摘要、异常提示和经营建议。")
    @PreAuthorize("hasAuthority('AI_ASSISTANT_USE')")
    @PostMapping("/ai/assistant")
    /**
     * 执行 assistant 方法。
     * 
     * <p>实现步骤：
     * 1. 接收并校验调用方传入的数据；
     * 2. 按当前方法职责执行业务查询、转换或持久化处理；
     * 3. 返回处理结果或更新对象状态。</p>
     */
    public ApiResponse<AiAssistantResponse> assistant(@Valid @RequestBody AiAssistantRequest request) {
        return ApiResponse.ok(aiAssistantService.ask(
                request.question(),
                request.mode(),
                request.conversationSummary(),
                request.conversationMessages() == null
                        ? List.of()
                        : request.conversationMessages().stream()
                        .map(item -> new AiAssistantService.ConversationMessage(item.role(), item.content()))
                        .toList()
        ));
    }

    @ApiOperationSupport(order = 81, author = "ratel / WenZhang / 18782945613")
    @Operation(summary = "ratel助手流式输出", description = "使用 SSE 流式返回助手回答，服务端带并发、超时、心跳和断开取消保护。")
    @PreAuthorize("hasAuthority('AI_ASSISTANT_USE')")
    @PostMapping(value = "/ai/assistant/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    /**
     * 执行 assistantStream 方法。
     *
     * <p>实现步骤：
     * 1. 接收前端问题、模式和会话上下文；
     * 2. 由流式服务统一完成权限上下文检索、模型调用、心跳、超时和断开取消；
     * 3. 返回 SSE 连接，前端按 meta/delta/done/error 事件更新页面。</p>
     */
    public SseEmitter assistantStream(@Valid @RequestBody AiAssistantRequest request) {
        return aiAssistantStreamService.stream(
                request.question(),
                request.mode(),
                request.conversationSummary(),
                request.conversationMessages() == null
                        ? List.of()
                        : request.conversationMessages().stream()
                        .map(item -> new AiAssistantService.ConversationMessage(item.role(), item.content()))
                        .toList()
        );
    }

    @ApiOperationSupport(order = 82, author = "ratel / WenZhang / 18782945613")
    @Operation(summary = "ratel助手 AI写作生成文件", description = "根据用户手动选择的 AI写作 意图生成 xlsx、docx、pdf 或 pptx 文件。")
    @PreAuthorize("hasAuthority('AI_ASSISTANT_USE')")
    @PostMapping("/ai/writing/generate")
    public ResponseEntity<byte[]> aiWriting(@Valid @RequestBody AiAssistantRequest request) {
        AiWritingService.GeneratedFile file = aiWritingService.generate(request.question());
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(file.contentType()))
                .header(HttpHeaders.CONTENT_DISPOSITION, ContentDisposition.attachment()
                        .filename(file.filename(), StandardCharsets.UTF_8)
                        .build()
                        .toString())
                .header("X-AI-Writing-Summary", java.util.Base64.getEncoder().encodeToString(file.summary().getBytes(StandardCharsets.UTF_8)))
                .body(file.content());
    }
}
