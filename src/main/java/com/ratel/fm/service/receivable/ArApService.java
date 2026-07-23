package com.ratel.fm.service.receivable;

import com.ratel.fm.common.BusinessException;
import com.ratel.fm.common.ResponseCode;
import com.ratel.fm.common.SearchSpecs;
import com.ratel.fm.config.export.ExportProperties;
import com.ratel.fm.domain.attachment.AttachmentBusinessType;
import com.ratel.fm.domain.receivable.ArApBill;
import com.ratel.fm.domain.receivable.ArApSettlement;
import com.ratel.fm.domain.receivable.ArApStatus;
import com.ratel.fm.domain.receivable.ArApType;
import com.ratel.fm.repository.receivable.ArApBillRepository;
import com.ratel.fm.repository.receivable.ArApSettlementRepository;
import com.ratel.fm.security.CompanyScope;
import com.ratel.fm.security.SecurityUtils;
import com.ratel.fm.service.attachment.AttachmentService;
import com.ratel.fm.service.audit.AuditLogService;
import com.ratel.fm.service.basic.CurrencyService;
import com.ratel.fm.service.basic.CurrencyService.CurrencySnapshot;
import com.ratel.fm.service.common.BusinessNumberSequenceService;
import com.ratel.fm.service.export.ExcelExportService;
import com.ratel.fm.service.export.ExcelExportService.ExcelColumn;
import com.ratel.fm.service.knowledge.KnowledgeIndexService;
import com.ratel.fm.service.operationlog.BusinessOperationLogService;
import com.ratel.fm.web.dto.phasetwo.PhaseTwoDtos.ArApExportRequest;
import com.ratel.fm.web.dto.phasetwo.PhaseTwoDtos.ArApPaymentStatsExportRequest;
import com.ratel.fm.web.dto.phasetwo.PhaseTwoDtos.ArApPaymentStatsRow;
import com.ratel.fm.web.dto.phasetwo.PhaseTwoDtos.ArApPaymentStatsView;
import com.ratel.fm.web.dto.phasetwo.PhaseTwoDtos.ArApRequest;
import com.ratel.fm.web.dto.phasetwo.PhaseTwoDtos.ArApSettlementRequest;
import com.ratel.fm.web.dto.phasetwo.PhaseTwoDtos.ArApSettlementView;
import com.ratel.fm.web.dto.phasetwo.PhaseTwoDtos.ArApView;
import com.ratel.fm.web.dto.operationlog.BusinessOperationLogDtos.BusinessOperationLogPage;
import com.ratel.fm.web.dto.operationlog.BusinessOperationLogDtos.BusinessOperationLogView;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.temporal.ChronoUnit;
import java.time.format.DateTimeFormatter;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * 应收应付服务。
 *
 * <p>当前提供基础单据新增、账龄和状态计算，后续可扩展收付款核销和付款计划提醒。</p>
 *
 * <p>开发组织：ratel；开发人员：WenZhang；联系方式：18782945613。</p>
 *
 * @author ratel
 */
@Service
public class ArApService {

    /**
     * 字段 repository：保存 repository 对应的业务数据、运行配置或依赖对象，供本类逻辑读取和维护。
     */
    private final ArApBillRepository repository;
    /**
     * 字段 settlementRepository：保存应收应付收付核销流水，支持多次收款或付款逐笔追溯。
     */
    private final ArApSettlementRepository settlementRepository;
    /**
     * 字段 auditLogService：保存 auditLogService 对应的业务数据、运行配置或依赖对象，供本类逻辑读取和维护。
     */
    private final AuditLogService auditLogService;
    /**
     * 字段 exportProperties：保存 exportProperties 对应的业务数据、运行配置或依赖对象，供本类逻辑读取和维护。
     */
    private final ExportProperties exportProperties;
    /**
     * 字段 excelExportService：保存 excelExportService 对应的业务数据、运行配置或依赖对象，供本类逻辑读取和维护。
     */
    private final ExcelExportService excelExportService;
    /**
     * 字段 currencyService：保存 currencyService 对应的业务数据、运行配置或依赖对象，供本类逻辑读取和维护。
     */
    private final CurrencyService currencyService;
    /**
     * 字段 attachmentService：保存 attachmentService 对应的业务数据、运行配置或依赖对象，供本类逻辑读取和维护。
     */
    private final AttachmentService attachmentService;
    /**
     * 字段 businessOperationLogService：保存 businessOperationLogService 对应的业务数据、运行配置或依赖对象，供本类逻辑读取和维护。
     */
    private final BusinessOperationLogService businessOperationLogService;
    /**
     * 字段 knowledgeIndexService：应收应付新增、核销和删除后同步刷新 AI 知识索引。
     */
    private final KnowledgeIndexService knowledgeIndexService;
    /** 业务单号序号服务，用于并发安全生成应收应付单号。 */
    private final BusinessNumberSequenceService numberSequenceService;

    /**
     * 构造 ArApService 实例。
     * 
     * <p>实现步骤：
     * 1. 接收调用方传入的依赖对象或初始化参数；
     * 2. 保存到成员字段，保证后续业务方法可以复用；
     * 3. 完成实例初始化。</p>
     */
    public ArApService(
            ArApBillRepository repository,
            ArApSettlementRepository settlementRepository,
            AuditLogService auditLogService,
            ExportProperties exportProperties,
            ExcelExportService excelExportService,
            CurrencyService currencyService,
            AttachmentService attachmentService,
            BusinessOperationLogService businessOperationLogService,
            KnowledgeIndexService knowledgeIndexService,
            BusinessNumberSequenceService numberSequenceService
    ) {
        this.repository = repository;
        this.settlementRepository = settlementRepository;
        this.auditLogService = auditLogService;
        this.exportProperties = exportProperties;
        this.excelExportService = excelExportService;
        this.currencyService = currencyService;
        this.attachmentService = attachmentService;
        this.businessOperationLogService = businessOperationLogService;
        this.knowledgeIndexService = knowledgeIndexService;
        this.numberSequenceService = numberSequenceService;
    }

    /**
     * 查询最近 100 条应收应付单据，按到期日升序排列。
     */
    @Transactional(readOnly = true)
    public List<ArApView> list() {
        return list(null, null, null, null, null, null, null, null);
    }

    /**
     * 按字段查询应收应付单。
     *
     * <p>实现步骤：
     * 1. 未指定日期时默认查最近 100 条；
     * 2. 单号、付款计划使用包含匹配；
     * 3. 单据类型、往来单位和状态使用等值匹配，到期日按日期范围过滤。</p>
     */
    @Transactional(readOnly = true)
    public List<ArApView> list(
            LocalDate startDate,
            LocalDate endDate,
            String billNo,
            ArApType billType,
            String partnerName,
            String projectCode,
            ArApStatus status,
            String paymentPlan
    ) {
        /**
         * 应收应付列表查询条件，先限定当前账套，再叠加到期日、单号、类型、往来单位、项目和状态筛选。
         */
        var spec = CompanyScope.<ArApBill>currentCompanySpec()
                .and(SearchSpecs.dateBetween("dueDate", startDate, endDate))
                .and(SearchSpecs.like("billNo", billNo))
                .and(SearchSpecs.equal("billType", billType))
                .and(SearchSpecs.equal("partnerName", blankToNull(partnerName)))
                .and(SearchSpecs.equal("projectCode", blankToNull(projectCode)))
                .and(SearchSpecs.equal("status", status))
                .and(SearchSpecs.like("paymentPlan", paymentPlan));
        return repository.findAll(spec, PageRequest.of(0, 100, Sort.by(Sort.Direction.DESC, "modifyTime", "id"))).stream()
                .map(this::toView)
                .toList();
    }

    /**
     * 导出应收应付列表。
     *
     * <p>实现步骤：
     * 1. 如果请求携带选中单据 ID，则按选中数据导出；
     * 2. 如果未选择数据，则按当前搜索条件查询；
     * 3. 查询结果按配置最大行数截断；
     * 4. 导出字段与应收应付列表可见字段保持一致。</p>
     */
    @Transactional(readOnly = true)
    public byte[] export(ArApExportRequest request) {
        ArApExportRequest exportRequest = request == null
                ? new ArApExportRequest(null, null, null, null, null, null, null, null, null)
                : request;
        List<ArApView> rows = hasSelectedIds(exportRequest.ids())
                ? selectedRows(exportRequest.ids())
                : searchRowsForExport(exportRequest);
        return excelExportService.export("应收应付", List.of(
                new ExcelColumn<>("单号", ArApView::billNo),
                new ExcelColumn<>("类型", row -> billTypeText(row.billType())),
                new ExcelColumn<>("客户/供应商", ArApView::partnerName),
                new ExcelColumn<>("项目", ArApView::projectName),
                new ExcelColumn<>("到期日", ArApView::dueDate),
                new ExcelColumn<>("金额", ArApView::amount),
                new ExcelColumn<>("未结金额", ArApView::remainingAmount),
                new ExcelColumn<>("币种", row -> currencyDisplay(row.currencyCode(), row.currencyName())),
                new ExcelColumn<>("汇率", ArApView::exchangeRateToCny),
                new ExcelColumn<>("金额人民币", ArApView::amountCny),
                new ExcelColumn<>("未结人民币", ArApView::remainingAmountCny),
                new ExcelColumn<>("账龄天数", ArApView::agingDays),
                new ExcelColumn<>("状态", row -> statusText(row.status())),
                new ExcelColumn<>("付款计划", ArApView::paymentPlan)
        ), rows);
    }

    /**
     * 导出收付统计。
     *
     * <p>实现步骤：
     * 1. 读取前端传入的项目和客户/供应商筛选条件；
     * 2. 复用页面收付统计查询方法，保证 Excel 与页面展示口径一致；
     * 3. 读取当前筛选条件下的全部明细；
     * 4. 写出应收应付单号、类型、项目、往来单位和六类金额列。</p>
     */
    @Transactional(readOnly = true)
    public byte[] exportPaymentStats(ArApPaymentStatsExportRequest request) {
        // 变量说明：exportRequest 保存导出请求，空请求代表导出当前账套全部收付统计结果。
        ArApPaymentStatsExportRequest exportRequest = request == null
                ? new ArApPaymentStatsExportRequest(null, null)
                : request;
        // 步骤2：复用统计查询，避免导出时重新拼接一套筛选逻辑造成结果不一致。
        ArApPaymentStatsView stats = paymentStats(exportRequest.projectCode(), exportRequest.partnerName());
        // 变量说明：rows 保存当前筛选条件下的全部统计明细行。
        List<ArApPaymentStatsRow> rows = stats.rows();
        return excelExportService.export("收付统计", List.of(
                new ExcelColumn<>("应收应付单号", ArApPaymentStatsRow::billNo),
                new ExcelColumn<>("类型", row -> billTypeText(row.billType())),
                new ExcelColumn<>("项目", ArApPaymentStatsRow::projectName),
                new ExcelColumn<>("客户/供应商", ArApPaymentStatsRow::partnerName),
                new ExcelColumn<>("应付金额", ArApPaymentStatsRow::payableAmount),
                new ExcelColumn<>("已付金额", ArApPaymentStatsRow::paidAmount),
                new ExcelColumn<>("待付金额", ArApPaymentStatsRow::pendingPayableAmount),
                new ExcelColumn<>("应收金额", ArApPaymentStatsRow::receivableAmount),
                new ExcelColumn<>("已收金额", ArApPaymentStatsRow::receivedAmount),
                new ExcelColumn<>("待收金额", ArApPaymentStatsRow::pendingReceivableAmount)
        ), rows);
    }

    /**
     * 按选中 ID 查询应收应付导出数据。
     */
    private List<ArApView> selectedRows(List<Long> ids) {
        List<Long> exportIds = ids.stream()
                .filter(Objects::nonNull)
                .distinct()
                .limit(exportProperties.maxRows())
                .toList();
        // 变量说明：orderMap 保存当前步骤计算、查询或转换得到的中间结果。
        Map<Long, Integer> orderMap = new LinkedHashMap<>();
        for (int index = 0; index < exportIds.size(); index++) {
            orderMap.put(exportIds.get(index), index);
        }
        return repository.findAllById(exportIds).stream()
                .filter(row -> Objects.equals(CompanyScope.currentCompanyCode(), row.getOrganizationCode()))
                .sorted(Comparator.comparingInt(row -> orderMap.getOrDefault(row.getId(), Integer.MAX_VALUE)))
                .map(this::toView)
                .toList();
    }

    /**
     * 按搜索条件查询应收应付导出数据。
     */
    private List<ArApView> searchRowsForExport(ArApExportRequest request) {
        /**
         * 应收应付导出查询条件，与列表筛选口径一致并限制当前所属公司。
         */
        var spec = CompanyScope.<ArApBill>currentCompanySpec()
                .and(SearchSpecs.dateBetween("dueDate", request.startDate(), request.endDate()))
                .and(SearchSpecs.like("billNo", request.billNo()))
                .and(SearchSpecs.equal("billType", request.billType()))
                .and(SearchSpecs.equal("partnerName", blankToNull(request.partnerName())))
                .and(SearchSpecs.equal("projectCode", blankToNull(request.projectCode())))
                .and(SearchSpecs.equal("status", request.status()))
                .and(SearchSpecs.like("paymentPlan", request.paymentPlan()));
        return repository.findAll(
                        spec,
                        PageRequest.of(0, exportProperties.maxRows(), Sort.by(Sort.Direction.DESC, "modifyTime", "id"))
                ).stream()
                .map(this::toView)
                .toList();
    }

    /**
     * 新增应收应付单。
     *
     * <p>实现步骤：
     * 1. 根据单据类型和日期生成编号；
     * 2. 写入往来单位、单据日期、到期日、金额和付款计划；
     * 3. paidAmount 为空时按 0 处理；
     * 4. 根据金额、已付金额和到期日计算状态；
     * 5. 保存单据并记录关键操作日志。</p>
     */
    @Transactional
    public ArApView create(ArApRequest request) {
        // 步骤1-3：应收应付单先记录基础债权债务事实。
        ArApBill bill = new ArApBill();
        bill.setBillNo(nextNo(request));
        bill.setBillType(request.billType());
        bill.setPartnerName(request.partnerName());
        bill.setProjectCode(blankToNull(request.projectCode()));
        bill.setProjectName(blankToNull(request.projectName()));
        bill.setDocumentType(defaultText(request.documentType(), request.billType() == com.ratel.fm.domain.receivable.ArApType.RECEIVABLE ? "销售应收" : "采购应付"));
        bill.setBusinessOrganization(request.businessOrganization());
        bill.setSettlementOrganization(request.settlementOrganization());
        bill.setPaymentOrganization(request.paymentOrganization());
        bill.setPaymentTerms(request.paymentTerms());
        bill.setSettlementMethod(request.settlementMethod());
        bill.setSourceBillType(request.sourceBillType());
        bill.setSourceBillNo(request.sourceBillNo());
        bill.setBillDate(request.billDate());
        bill.setDueDate(request.dueDate());
        // 变量说明：currency 保存当前步骤计算、查询或转换得到的中间结果。
        CurrencySnapshot currency = currencyService.snapshot(request.currencyCode(), request.currencyName(), request.exchangeRateToCny());
        // 变量说明：amount 保存当前步骤计算、查询或转换得到的中间结果。
        BigDecimal amount = money(request.amount());
        // 变量说明：paidAmount 保存当前步骤计算、查询或转换得到的中间结果。
        BigDecimal paidAmount = money(request.paidAmount());
        bill.setAmount(amount);
        bill.setPaidAmount(paidAmount);
        bill.setCurrencyCode(currency.currencyCode());
        bill.setCurrencyName(currency.currencyName());
        bill.setExchangeRateToCny(currency.exchangeRateToCny());
        bill.setAmountCny(currencyService.toCnyAmount(amount, currency));
        bill.setPaidAmountCny(currencyService.toCnyAmount(paidAmount, currency));
        bill.setPaymentPlan(request.paymentPlan());
        bill.setOrganizationCode(CompanyScope.currentCompanyCode());
        // 步骤4：状态由金额和日期推导，减少前端传错状态的风险。
        bill.setStatus(status(bill));
        // 变量说明：view 保存当前步骤计算、查询或转换得到的中间结果。
        ArApView view = toView(repository.save(bill));
        businessOperationLogService.record("AR_AP_BILL", view.id(), view.billNo(), arApTitle(view), "CREATE", "新增应收应付单",
                "新增应收应付单号" + view.billNo() + "，往来单位为" + view.partnerName() + "，金额为" + view.amount() + " " + view.currencyCode() + "。",
                null, statusText(view.status()), view);
        knowledgeIndexService.rebuildArApBill(bill);
        // 步骤5：应收应付直接影响资金计划，必须记录审计。
        auditLogService.record("CREATE_AR_AP_BILL", request, "SUCCESS",
                "应收应付新增了单据号" + view.billNo() + "，往来单位为" + view.partnerName() + "，金额为" + view.amount() + " " + view.currencyCode() + "。");
        return view;
    }

    /**
     * 查询应收应付收付核销流水。
     *
     * <p>实现步骤：
     * 1. 读取应收应付单并校验所属公司；
     * 2. 按核销日期倒序读取该单据的核销流水；
     * 3. 转换为前端可展示的核销明细。</p>
     */
    @Transactional(readOnly = true)
    public List<ArApSettlementView> listSettlements(Long billId) {
        ArApBill bill = repository.findById(billId)
                .orElseThrow(() -> new BusinessException(HttpStatus.NOT_FOUND, "应收应付单不存在"));
        CompanyScope.requireCurrentCompany(bill.getOrganizationCode(), "应收应付单");
        return settlementRepository.findByOrganizationCodeAndBillIdOrderBySettlementDateDescIdDesc(
                        CompanyScope.currentCompanyCode(), billId).stream()
                .map(this::toSettlementView)
                .toList();
    }

    /**
     * 新增应收应付收付核销。
     *
     * <p>实现步骤：
     * 1. 读取当前账套应收应付单，并计算剩余未结金额；
     * 2. 校验本次核销金额大于 0 且不能超过未结金额；
     * 3. 按单据币种汇率计算本次核销折人民币金额；
     * 4. 保存核销流水，并把核销金额累加到单据已收/已付金额；
     * 5. 重新计算单据状态并记录业务流水和审计日志。</p>
     */
    @Transactional
    public ArApView settle(Long billId, ArApSettlementRequest request) {
        // 步骤1：应收应付核销必须基于当前账套内存在的单据。
        ArApBill bill = repository.findById(billId)
                .orElseThrow(() -> new BusinessException(HttpStatus.NOT_FOUND, "应收应付单不存在"));
        CompanyScope.requireCurrentCompany(bill.getOrganizationCode(), "应收应付单");
        // 变量说明：settlementAmount 保存本次核销原币金额。
        BigDecimal settlementAmount = money(request.amount());
        // 变量说明：remainingAmount 保存核销前剩余未结金额。
        BigDecimal remainingAmount = money(bill.getAmount()).subtract(money(bill.getPaidAmount()));
        if (settlementAmount.signum() <= 0) {
            throw new BusinessException(ResponseCode.ILLEGAL_PARAM, "核销金额必须大于0");
        }
        if (settlementAmount.compareTo(remainingAmount) > 0) {
            throw new BusinessException(ResponseCode.ILLEGAL_PARAM,
                    "核销金额不能超过未结金额，当前未结金额为" + remainingAmount);
        }
        // 步骤3：使用应收应付单发生时币种和汇率，保持历史核算口径稳定。
        CurrencySnapshot currency = currencyService.snapshot(bill.getCurrencyCode(), bill.getCurrencyName(), bill.getExchangeRateToCny());
        BigDecimal settlementAmountCny = currencyService.toCnyAmount(settlementAmount, currency);
        // 步骤4：先落核销流水，再更新应收应付单累计已收/已付金额。
        ArApSettlement settlement = new ArApSettlement();
        settlement.setOrganizationCode(CompanyScope.currentCompanyCode());
        settlement.setBill(bill);
        settlement.setSettlementDate(request.settlementDate());
        settlement.setAmount(settlementAmount);
        settlement.setAmountCny(settlementAmountCny);
        settlement.setSettlementMethod(blankToNull(request.settlementMethod()));
        settlement.setBankAccount(blankToNull(request.bankAccount()));
        settlement.setCashierTransactionNo(blankToNull(request.cashierTransactionNo()));
        settlement.setRemark(request.remark());
        settlementRepository.save(settlement);
        ArApStatus beforeStatus = status(bill);
        BigDecimal paidBefore = money(bill.getPaidAmount());
        BigDecimal paidCnyBefore = defaultMoney(bill.getPaidAmountCny(), paidBefore);
        bill.setPaidAmount(paidBefore.add(settlementAmount));
        bill.setPaidAmountCny(paidCnyBefore.add(settlementAmountCny));
        bill.setStatus(status(bill));
        ArApView view = toView(bill);
        // 步骤5：核销会改变待收待付金额和状态，必须进入查看流水和审计日志。
        String actionTitle = bill.getBillType() == ArApType.RECEIVABLE ? "收款核销" : "付款核销";
        businessOperationLogService.record("AR_AP_BILL", view.id(), view.billNo(), arApTitle(view), "SETTLE", actionTitle,
                actionTitle + "单号" + view.billNo() + "，本次金额为" + settlementAmount + " " + view.currencyCode() + "。",
                statusText(beforeStatus), statusText(view.status()), view);
        knowledgeIndexService.rebuildArApBill(bill);
        auditLogService.record("SETTLE_AR_AP_BILL", request, "SUCCESS",
                "应收应付对单据号" + view.billNo() + "进行了" + actionTitle + "，本次金额为" + settlementAmount + " " + view.currencyCode() + "。");
        return view;
    }

    /**
     * 查询收付统计。
     *
     * <p>实现步骤：
     * 1. 按项目编码和客户/供应商筛选应收应付单；
     * 2. 将每张单据转换为统计行，应收单写应收、已收和待收，应付单写应付、已付和待付；
     * 3. 按人民币金额快照统计，避免多币种直接相加导致口径不一致；
     * 4. 汇总全部明细行金额，返回给前端展示总计。</p>
     */
    @Transactional(readOnly = true)
    public ArApPaymentStatsView paymentStats(String projectCode, String partnerName) {
        // 步骤1：项目和往来单位都是下拉条件，空值表示不过滤该维度。
        Specification<ArApBill> spec = CompanyScope.<ArApBill>currentCompanySpec()
                .and(SearchSpecs.<ArApBill, String>equal("projectCode", blankToNull(projectCode)))
                .and(SearchSpecs.<ArApBill, String>equal("partnerName", blankToNull(partnerName)));
        // 变量说明：rows 保存按应收应付单号转换后的统计明细。
        List<ArApPaymentStatsRow> rows = repository.findAll(spec, Sort.by(Sort.Direction.DESC, "modifyTime", "id")).stream()
                .map(this::toPaymentStatsRow)
                .toList();
        // 步骤4：使用 BigDecimal 精确累加统计金额，避免前端重复计算带来的小数误差。
        BigDecimal totalReceivableAmount = rows.stream().map(ArApPaymentStatsRow::receivableAmount).reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal totalPayableAmount = rows.stream().map(ArApPaymentStatsRow::payableAmount).reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal totalReceivedAmount = rows.stream().map(ArApPaymentStatsRow::receivedAmount).reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal totalPaidAmount = rows.stream().map(ArApPaymentStatsRow::paidAmount).reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal totalPendingReceivableAmount = rows.stream().map(ArApPaymentStatsRow::pendingReceivableAmount).reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal totalPendingPayableAmount = rows.stream().map(ArApPaymentStatsRow::pendingPayableAmount).reduce(BigDecimal.ZERO, BigDecimal::add);
        return new ArApPaymentStatsView(
                rows,
                money(totalReceivableAmount),
                money(totalPayableAmount),
                money(totalReceivedAmount),
                money(totalPaidAmount),
                money(totalPendingReceivableAmount),
                money(totalPendingPayableAmount)
        );
    }

    /**
     * 批量删除应收应付单。
     *
     * <p>实现步骤：
     * 1. 清理并去重前端传入的单据 ID；
     * 2. 逐个读取单据，任一 ID 不存在则整体失败；
     * 3. 删除应收应付单主表；
     * 4. 记录关键操作日志。</p>
     */
    @Transactional
    public void delete(List<Long> ids) {
        // 变量说明：deleteIds 保存当前步骤计算、查询或转换得到的中间结果。
        List<Long> deleteIds = normalizeBatchIds(ids);
        // 变量说明：billNos 保存当前步骤计算、查询或转换得到的中间结果。
        List<String> billNos = new java.util.ArrayList<>();
        for (Long id : deleteIds) {
            ArApBill bill = repository.findById(id)
                    .orElseThrow(() -> new BusinessException(HttpStatus.NOT_FOUND, "应收应付单不存在: " + id));
            CompanyScope.requireCurrentCompany(bill.getOrganizationCode(), "应收应付单");
            billNos.add(bill.getBillNo());
            businessOperationLogService.record("AR_AP_BILL", bill.getId(), bill.getBillNo(), bill.getPartnerName(), "DELETE", "删除应收应付单",
                    "删除应收应付单号" + bill.getBillNo() + "，往来单位为" + bill.getPartnerName() + "。",
                    statusText(status(bill)), "已删除", bill.getBillNo());
            attachmentService.deleteAllForBusiness(AttachmentBusinessType.AR_AP_BILL, id);
            knowledgeIndexService.deleteArApBill(id);
            repository.delete(bill);
        }
        auditLogService.record("BATCH_DELETE_AR_AP_BILLS", "arApBillIds=" + deleteIds + ", billNos=" + billNos,
                "SUCCESS", "应收应付删除了单据号: " + String.join("、", billNos) + "。");
    }

    /**
     * 根据付款金额和到期日计算应收应付状态。
     */
    private ArApStatus status(ArApBill bill) {
        if (bill.getPaidAmount().compareTo(bill.getAmount()) >= 0) {
            return ArApStatus.CLOSED;
        }
        if (bill.getDueDate().isBefore(LocalDate.now())) {
            return ArApStatus.OVERDUE;
        }
        if (bill.getPaidAmount().signum() > 0) {
            return ArApStatus.PARTIAL;
        }
        return ArApStatus.OPEN;
    }

    /**
     * 按单据类型和日期生成应收应付编号。
     *
     * <p>应收使用 AR 前缀，应付使用 AP 前缀，格式为 AR/AP + yyyyMMdd + 4 位序号。</p>
     */
    private String nextNo(ArApRequest request) {
        String prefix = (request.billType().name().startsWith("RECEIVABLE") ? "AR" : "AP")
                + request.billDate().format(DateTimeFormatter.BASIC_ISO_DATE);
        String companyCode = CompanyScope.currentCompanyCode();
        return numberSequenceService.next(
                "AR_AP_BILL",
                companyCode,
                prefix,
                () -> repository.findFirstByOrganizationCodeAndBillNoStartingWithOrderByBillNoDesc(companyCode, prefix)
                        .map(bill -> billNoSequence(bill.getBillNo(), prefix) + 1)
                        .orElse(1),
                no -> repository.existsByOrganizationCodeAndBillNo(companyCode, no)
        );
    }

    /**
     * 提取应收应付单号末尾序号。
     */
    private int billNoSequence(String billNo, String prefix) {
        if (billNo == null || !billNo.startsWith(prefix)) {
            return 0;
        }
        try {
            return Integer.parseInt(billNo.substring(prefix.length()));
        } catch (NumberFormatException ex) {
            return 0;
        }
    }

    /**
     * 判断是否存在有效选中 ID。
     */
    private boolean hasSelectedIds(List<Long> ids) {
        return ids != null && ids.stream().anyMatch(Objects::nonNull);
    }

    /**
     * 清理批量删除 ID。
     */
    private List<Long> normalizeBatchIds(List<Long> ids) {
        if (ids == null) {
            throw new BusinessException(ResponseCode.ILLEGAL_PARAM, "请选择需要删除的数据");
        }
        List<Long> normalizedIds = ids.stream()
                .filter(Objects::nonNull)
                .distinct()
                .toList();
        if (normalizedIds.isEmpty()) {
            throw new BusinessException(ResponseCode.ILLEGAL_PARAM, "请选择需要删除的数据");
        }
        return normalizedIds;
    }

    /**
     * 将下拉筛选清空后的空字符串转为空值。
     *
     * <p>实现步骤：空文本返回 null；非空文本去除首尾空格后用于字典类等值查询。</p>
     */
    private String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }

    /**
     * 单据类型中文化，用于 Excel 导出。
     */
    private String billTypeText(ArApType billType) {
        return switch (billType) {
            case RECEIVABLE -> "应收";
            case PAYABLE -> "应付";
        };
    }

    /**
     * 应收应付状态中文化，用于 Excel 导出。
     */
    private String statusText(ArApStatus status) {
        return switch (status) {
            case OPEN -> "未结";
            case PARTIAL -> "部分结清";
            case CLOSED -> "已结清";
            case OVERDUE -> "逾期";
        };
    }

    /**
     * 统一金额精度。
     *
     * <p>金额统一保留 8 位小数，避免多币种换算和后续统计时损失精度。</p>
     */
    private BigDecimal money(BigDecimal value) {
        return (value == null ? BigDecimal.ZERO : value).setScale(CurrencyService.MONEY_SCALE, RoundingMode.HALF_UP);
    }

    /**
     * 组装币种显示文本，用于 Excel 导出。
     */
    private String currencyDisplay(String currencyCode, String currencyName) {
        if (currencyName == null || currencyName.isBlank()) {
            return currencyCode == null ? "" : currencyCode;
        }
        return currencyName + "(" + currencyCode + ")";
    }

    /**
     * 查询应收应付操作流水。
     */
    @Transactional(readOnly = true)
    public List<BusinessOperationLogView> listOperationLogs(Long id) {
        ArApBill bill = repository.findById(id)
                .orElseThrow(() -> new BusinessException(HttpStatus.NOT_FOUND, "应收应付单不存在"));
        CompanyScope.requireCurrentCompany(bill.getOrganizationCode(), "应收应付单");
        return businessOperationLogService.list("AR_AP_BILL", id);
    }

    /**
     * 分页查询应收应付操作流水。
     *
     * <p>实现步骤：先确认应收应付单存在，再按操作时间范围和分页条件查询，前端右侧抽屉滚动加载。</p>
     */
    @Transactional(readOnly = true)
    public BusinessOperationLogPage pageOperationLogs(Long id, OffsetDateTime startTime, OffsetDateTime endTime, int page, int size) {
        ArApBill bill = repository.findById(id)
                .orElseThrow(() -> new BusinessException(HttpStatus.NOT_FOUND, "应收应付单不存在"));
        CompanyScope.requireCurrentCompany(bill.getOrganizationCode(), "应收应付单");
        return businessOperationLogService.page("AR_AP_BILL", id, startTime, endTime, page, size);
    }

    /**
     * 生成应收应付流水标题。
     */
    private String arApTitle(ArApView view) {
        return view.billNo() + " " + view.partnerName();
    }

    /**
     * 转换为前端视图，并实时计算剩余金额和账龄。
     */
    private ArApView toView(ArApBill bill) {
        // 变量说明：remaining 保存当前步骤计算、查询或转换得到的中间结果。
        BigDecimal remaining = bill.getAmount().subtract(bill.getPaidAmount());
        BigDecimal remainingCny = defaultMoney(bill.getAmountCny(), bill.getAmount())
                .subtract(defaultMoney(bill.getPaidAmountCny(), bill.getPaidAmount()));
        // 变量说明：aging 保存当前步骤计算、查询或转换得到的中间结果。
        long aging = Math.max(0, ChronoUnit.DAYS.between(bill.getDueDate(), LocalDate.now()));
        return new ArApView(bill.getId(), bill.getBillNo(), bill.getBillType(), bill.getPartnerName(),
                bill.getProjectCode(), bill.getProjectName(),
                bill.getDocumentType(), bill.getBusinessOrganization(), bill.getSettlementOrganization(),
                bill.getPaymentOrganization(), bill.getPaymentTerms(), bill.getSettlementMethod(),
                bill.getSourceBillType(), bill.getSourceBillNo(), bill.getBillDate(), bill.getDueDate(),
                bill.getAmount(), bill.getPaidAmount(), remaining,
                defaultString(bill.getCurrencyCode(), CurrencyService.DEFAULT_CURRENCY_CODE),
                defaultString(bill.getCurrencyName(), CurrencyService.DEFAULT_CURRENCY_NAME),
                defaultRate(bill.getExchangeRateToCny()),
                defaultMoney(bill.getAmountCny(), bill.getAmount()),
                defaultMoney(bill.getPaidAmountCny(), bill.getPaidAmount()),
                remainingCny,
                status(bill), aging, bill.getPaymentPlan(), bill.getOrganizationCode(),
                bill.getVoucherId(), bill.getVoucherNo(),
                attachmentService.count(AttachmentBusinessType.AR_AP_BILL, bill.getId()));
    }

    /**
     * 将应收应付单转换为收付统计行。
     *
     * <p>实现步骤：
     * 1. 读取单据总金额、已核销金额和剩余金额的人民币快照；
     * 2. 应收单写入应收、已收和待收金额，应付相关列置零；
     * 3. 应付单写入应付、已付和待付金额，应收相关列置零；
     * 4. 返回单据号、项目和往来单位，前端按行展示。</p>
     */
    private ArApPaymentStatsRow toPaymentStatsRow(ArApBill bill) {
        // 变量说明：amountCny 保存当前步骤计算、查询或转换得到的中间结果。
        BigDecimal amountCny = defaultMoney(bill.getAmountCny(), bill.getAmount());
        // 变量说明：paidCny 保存当前单据已收或已付金额的人民币快照。
        BigDecimal paidCny = defaultMoney(bill.getPaidAmountCny(), bill.getPaidAmount());
        // 变量说明：remainingCny 保存当前步骤计算、查询或转换得到的中间结果。
        BigDecimal remainingCny = amountCny.subtract(paidCny);
        // 变量说明：zero 保存统一精度的零金额，避免每个分支重复构造。
        BigDecimal zero = BigDecimal.ZERO.setScale(CurrencyService.MONEY_SCALE, RoundingMode.HALF_UP);
        if (bill.getBillType() == ArApType.RECEIVABLE) {
            return new ArApPaymentStatsRow(
                    bill.getBillNo(),
                    bill.getBillType(),
                    bill.getProjectCode(),
                    bill.getProjectName(),
                    bill.getPartnerName(),
                    money(amountCny),
                    zero,
                    money(paidCny),
                    zero,
                    money(remainingCny),
                    zero
            );
        }
        return new ArApPaymentStatsRow(
                bill.getBillNo(),
                bill.getBillType(),
                bill.getProjectCode(),
                bill.getProjectName(),
                bill.getPartnerName(),
                zero,
                money(amountCny),
                zero,
                money(paidCny),
                zero,
                money(remainingCny)
        );
    }

    /**
     * 转换应收应付核销流水视图。
     *
     * <p>实现步骤：读取核销实体和关联单据字段，输出页面查看核销明细所需的字段。</p>
     */
    private ArApSettlementView toSettlementView(ArApSettlement settlement) {
        return new ArApSettlementView(
                settlement.getId(),
                settlement.getBill().getId(),
                settlement.getBill().getBillNo(),
                settlement.getSettlementDate(),
                settlement.getAmount(),
                settlement.getAmountCny(),
                settlement.getSettlementMethod(),
                settlement.getBankAccount(),
                settlement.getCashierTransactionNo(),
                settlement.getRemark()
        );
    }

    /**
     * 空字符串时返回默认文本。
     */
    private String defaultString(String value, String defaultValue) {
        return value == null || value.isBlank() ? defaultValue : value;
    }

    /**
     * 执行 defaultText 方法。
     * 
     * <p>实现步骤：
     * 1. 接收并校验调用方传入的数据；
     * 2. 按当前方法职责执行业务查询、转换或持久化处理；
     * 3. 返回处理结果或更新对象状态。</p>
     */
    private String defaultText(String value, String defaultValue) {
        return value == null || value.isBlank() ? defaultValue : value.trim();
    }

    /**
     * 空汇率按人民币 1 处理，兼容升级前历史数据。
     */
    private BigDecimal defaultRate(BigDecimal value) {
        return value == null ? BigDecimal.ONE.setScale(CurrencyService.MONEY_SCALE, RoundingMode.HALF_UP) : value;
    }

    /**
     * 空人民币快照按原币金额回退，兼容升级前历史数据。
     */
    private BigDecimal defaultMoney(BigDecimal value, BigDecimal fallback) {
        return value == null ? money(fallback) : value;
    }
}
