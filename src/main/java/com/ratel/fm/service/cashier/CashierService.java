package com.ratel.fm.service.cashier;

import com.ratel.fm.common.BusinessException;
import com.ratel.fm.common.ResponseCode;
import com.ratel.fm.common.SearchSpecs;
import com.ratel.fm.config.export.ExportProperties;
import com.ratel.fm.domain.cashier.CashierTransaction;
import com.ratel.fm.domain.cashier.CashierTransactionStatus;
import com.ratel.fm.domain.cashier.CashierTransactionType;
import com.ratel.fm.repository.cashier.CashierTransactionRepository;
import com.ratel.fm.security.CompanyScope;
import com.ratel.fm.security.SecurityUtils;
import com.ratel.fm.service.audit.AuditLogService;
import com.ratel.fm.service.basic.CurrencyService;
import com.ratel.fm.service.basic.CurrencyService.CurrencySnapshot;
import com.ratel.fm.service.common.BusinessNumberSequenceService;
import com.ratel.fm.service.export.ExcelExportService;
import com.ratel.fm.service.export.ExcelExportService.ExcelColumn;
import com.ratel.fm.service.knowledge.KnowledgeIndexService;
import com.ratel.fm.service.operationlog.BusinessOperationLogService;
import com.ratel.fm.web.dto.cashier.CashierDtos.CashierExportRequest;
import com.ratel.fm.web.dto.cashier.CashierDtos.CashierTransactionRequest;
import com.ratel.fm.web.dto.cashier.CashierDtos.CashierTransactionView;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * 出纳管理服务。
 *
 * <p>实现目的：维护资金流水的新增、查询、确认、取消和导出，并为会计平台提供可制证资金来源。</p>
 */
@Service
public class CashierService {

    /** 出纳流水仓库。 */
    private final CashierTransactionRepository repository;
    /** 币种服务，用于形成发生时汇率和人民币金额快照。 */
    private final CurrencyService currencyService;
    /** 审计日志服务。 */
    private final AuditLogService auditLogService;
    /** 业务流水服务，用于查看出纳流水状态变化。 */
    private final BusinessOperationLogService businessOperationLogService;
    /** Excel 导出服务。 */
    private final ExcelExportService excelExportService;
    /** 导出配置。 */
    private final ExportProperties exportProperties;
    /** 知识索引服务，用于出纳流水变更后同步刷新智能检索和 ratel助手上下文。 */
    private final KnowledgeIndexService knowledgeIndexService;
    /** 业务单号序号服务，用于并发安全生成出纳流水号。 */
    private final BusinessNumberSequenceService numberSequenceService;

    /**
     * 构造出纳管理服务。
     *
     * <p>实现步骤：接收出纳仓库、币种、审计、业务流水和导出依赖，保存到成员字段供业务方法复用。</p>
     */
    public CashierService(
            CashierTransactionRepository repository,
            CurrencyService currencyService,
            AuditLogService auditLogService,
            BusinessOperationLogService businessOperationLogService,
            ExcelExportService excelExportService,
            ExportProperties exportProperties,
            KnowledgeIndexService knowledgeIndexService,
            BusinessNumberSequenceService numberSequenceService
    ) {
        this.repository = repository;
        this.currencyService = currencyService;
        this.auditLogService = auditLogService;
        this.businessOperationLogService = businessOperationLogService;
        this.excelExportService = excelExportService;
        this.exportProperties = exportProperties;
        this.knowledgeIndexService = knowledgeIndexService;
        this.numberSequenceService = numberSequenceService;
    }

    /**
     * 查询出纳流水列表。
     *
     * <p>实现步骤：
     * 1. 按当前所属公司限制数据范围；
     * 2. 支持日期、类型、状态、项目、往来单位和关联单号筛选；
     * 3. 按修改时间倒序返回最近 100 条。</p>
     */
    @Transactional(readOnly = true)
    public List<CashierTransactionView> list(
            LocalDate startDate,
            LocalDate endDate,
            CashierTransactionType transactionType,
            CashierTransactionStatus status,
            String projectCode,
            String partnerName,
            String relatedBizNo
    ) {
        /**
         * 出纳流水查询条件，先限定当前所属公司，再叠加日期、类型、状态、项目和往来单位筛选。
         */
        var spec = CompanyScope.<CashierTransaction>currentCompanySpec()
                .and(SearchSpecs.dateBetween("transactionDate", startDate, endDate))
                .and(SearchSpecs.equal("transactionType", transactionType))
                .and(SearchSpecs.equal("status", status))
                .and(SearchSpecs.equal("projectCode", blankToNull(projectCode)))
                .and(SearchSpecs.like("partnerName", partnerName))
                .and(SearchSpecs.like("relatedBizNo", relatedBizNo));
        return repository.findAll(spec, PageRequest.of(0, 100, Sort.by(Sort.Direction.DESC, "modifyTime", "id"))).stream()
                .map(this::toView)
                .toList();
    }

    /**
     * 新增出纳资金流水草稿。
     *
     * <p>实现步骤：
     * 1. 按交易日期生成账套内唯一流水号；
     * 2. 解析币种和汇率快照，计算人民币金额；
     * 3. 保存草稿状态资金流水；
     * 4. 记录业务流水和审计日志。</p>
     */
    @Transactional
    public CashierTransactionView create(CashierTransactionRequest request) {
        CashierTransaction transaction = new CashierTransaction();
        transaction.setOrganizationCode(CompanyScope.currentCompanyCode());
        transaction.setTransactionNo(nextNo(request.transactionDate()));
        transaction.setTransactionDate(request.transactionDate());
        transaction.setTransactionType(request.transactionType());
        transaction.setStatus(CashierTransactionStatus.DRAFT);
        applyRequest(transaction, request);
        transaction.setCreatedBy(SecurityUtils.currentUser().username());
        CashierTransactionView view = toView(repository.save(transaction));
        businessOperationLogService.record("CASHIER_TRANSACTION", view.id(), view.transactionNo(), view.summary(),
                "CREATE", "新增出纳流水", "新增出纳流水号" + view.transactionNo() + "，金额为" + view.amount() + view.currencyCode() + "。",
                null, cashierStatusText(view.status()), view);
        auditLogService.finance("CREATE_CASHIER_TRANSACTION", request, "SUCCESS",
                "新增出纳流水" + view.transactionNo() + "。");
        knowledgeIndexService.rebuildCashierTransaction(transaction);
        return view;
    }

    /**
     * 确认出纳流水。
     *
     * <p>实现步骤：读取当前账套草稿流水，将状态改为已确认并写入确认人和确认时间，确认后才能用于会计平台制证。</p>
     */
    @Transactional
    public CashierTransactionView confirm(Long id) {
        CashierTransaction transaction = requireTransaction(id);
        if (transaction.getStatus() != CashierTransactionStatus.DRAFT) {
            throw new BusinessException("仅草稿出纳流水允许确认");
        }
        transaction.setStatus(CashierTransactionStatus.CONFIRMED);
        transaction.setConfirmedBy(SecurityUtils.currentUser().username());
        transaction.setConfirmedTime(OffsetDateTime.now());
        CashierTransactionView view = toView(transaction);
        businessOperationLogService.record("CASHIER_TRANSACTION", view.id(), view.transactionNo(), view.summary(),
                "CONFIRM", "确认出纳流水", "确认出纳流水号" + view.transactionNo() + "。",
                cashierStatusText(CashierTransactionStatus.DRAFT), cashierStatusText(view.status()), view);
        auditLogService.finance("CONFIRM_CASHIER_TRANSACTION", "cashierTransactionId=" + id, "SUCCESS",
                "确认出纳流水" + view.transactionNo() + "。");
        knowledgeIndexService.rebuildCashierTransaction(transaction);
        return view;
    }

    /**
     * 取消出纳流水。
     *
     * <p>实现步骤：读取当前账套流水，已制证流水禁止取消，其余流水改为已取消并记录审计。</p>
     */
    @Transactional
    public CashierTransactionView cancel(Long id) {
        CashierTransaction transaction = requireTransaction(id);
        if (transaction.getStatus() == CashierTransactionStatus.VOUCHERED) {
            throw new BusinessException("已制证出纳流水不能取消");
        }
        CashierTransactionStatus before = transaction.getStatus();
        transaction.setStatus(CashierTransactionStatus.CANCELLED);
        CashierTransactionView view = toView(transaction);
        businessOperationLogService.record("CASHIER_TRANSACTION", view.id(), view.transactionNo(), view.summary(),
                "CANCEL", "取消出纳流水", "取消出纳流水号" + view.transactionNo() + "。",
                cashierStatusText(before), cashierStatusText(view.status()), view);
        auditLogService.finance("CANCEL_CASHIER_TRANSACTION", "cashierTransactionId=" + id, "SUCCESS",
                "取消出纳流水" + view.transactionNo() + "。");
        knowledgeIndexService.rebuildCashierTransaction(transaction);
        return view;
    }

    /**
     * 删除出纳流水。
     *
     * <p>实现步骤：只允许删除草稿或已取消流水，确认和已制证资金事实必须通过取消或冲销保留轨迹。</p>
     */
    @Transactional
    public void delete(List<Long> ids) {
        List<Long> deleteIds = normalizeBatchIds(ids);
        for (Long id : deleteIds) {
            CashierTransaction transaction = requireTransaction(id);
            if (transaction.getStatus() != CashierTransactionStatus.DRAFT
                    && transaction.getStatus() != CashierTransactionStatus.CANCELLED) {
                throw new BusinessException("仅草稿或已取消出纳流水允许删除: " + transaction.getTransactionNo());
            }
            businessOperationLogService.record("CASHIER_TRANSACTION", transaction.getId(), transaction.getTransactionNo(), transaction.getSummary(),
                    "DELETE", "删除出纳流水", "删除出纳流水号" + transaction.getTransactionNo() + "。",
                    cashierStatusText(transaction.getStatus()), "已删除", transaction.getTransactionNo());
            repository.delete(transaction);
            knowledgeIndexService.deleteCashierTransaction(id);
        }
        auditLogService.finance("DELETE_CASHIER_TRANSACTIONS", "cashierTransactionIds=" + deleteIds, "SUCCESS",
                "批量删除出纳流水。");
    }

    /**
     * 导出出纳流水。
     *
     * <p>实现步骤：优先按选中 ID 导出，未选中时按搜索条件查询，并按导出配置限制最大行数。</p>
     */
    @Transactional(readOnly = true)
    public byte[] export(CashierExportRequest request) {
        CashierExportRequest exportRequest = request == null
                ? new CashierExportRequest(null, null, null, null, null, null, null, null)
                : request;
        List<CashierTransactionView> rows = hasSelectedIds(exportRequest.ids())
                ? selectedRows(exportRequest.ids())
                : list(exportRequest.startDate(), exportRequest.endDate(), exportRequest.transactionType(),
                exportRequest.status(), exportRequest.projectCode(), exportRequest.partnerName(), exportRequest.relatedBizNo());
        if (rows.size() > exportProperties.maxRows()) {
            rows = rows.subList(0, exportProperties.maxRows());
        }
        return excelExportService.export("出纳流水", List.of(
                new ExcelColumn<>("流水号", CashierTransactionView::transactionNo),
                new ExcelColumn<>("交易日期", CashierTransactionView::transactionDate),
                new ExcelColumn<>("类型", row -> cashierTypeText(row.transactionType())),
                new ExcelColumn<>("状态", row -> cashierStatusText(row.status())),
                new ExcelColumn<>("项目", CashierTransactionView::projectName),
                new ExcelColumn<>("往来单位", CashierTransactionView::partnerName),
                new ExcelColumn<>("银行账户", CashierTransactionView::bankAccount),
                new ExcelColumn<>("结算方式", CashierTransactionView::settlementMethod),
                new ExcelColumn<>("金额", CashierTransactionView::amount),
                new ExcelColumn<>("币种", CashierTransactionView::currencyCode),
                new ExcelColumn<>("人民币金额", CashierTransactionView::amountCny),
                new ExcelColumn<>("关联单号", CashierTransactionView::relatedBizNo),
                new ExcelColumn<>("摘要", CashierTransactionView::summary)
        ), rows);
    }

    /**
     * 按 ID 获取出纳流水并校验账套。
     *
     * <p>实现步骤：按当前所属公司和主键查询，查不到时返回 404 业务异常。</p>
     */
    @Transactional(readOnly = true)
    public CashierTransactionView get(Long id) {
        return toView(requireTransaction(id));
    }

    /**
     * 将自动制证后的凭证号回写到出纳流水。
     *
     * <p>实现步骤：会计平台生成凭证后调用本方法，状态改为已制证并保存凭证主键和凭证号。</p>
     */
    @Transactional
    public void markVoucherGenerated(Long id, Long voucherId, String voucherNo) {
        CashierTransaction transaction = requireTransaction(id);
        transaction.setStatus(CashierTransactionStatus.VOUCHERED);
        transaction.setVoucherId(voucherId);
        transaction.setVoucherNo(voucherNo);
        knowledgeIndexService.rebuildCashierTransaction(transaction);
    }

    /**
     * 读取出纳实体。
     *
     * <p>实现步骤：按当前账套读取实体，保证后续会计平台和状态操作不会跨账套访问数据。</p>
     */
    public CashierTransaction requireTransaction(Long id) {
        return repository.findByOrganizationCodeAndId(CompanyScope.currentCompanyCode(), id)
                .orElseThrow(() -> new BusinessException(HttpStatus.NOT_FOUND, "出纳流水不存在"));
    }

    /**
     * 将请求字段写入出纳实体。
     *
     * <p>实现步骤：保存项目、往来、账户、结算、摘要和备注，并通过币种服务计算金额快照。</p>
     */
    private void applyRequest(CashierTransaction transaction, CashierTransactionRequest request) {
        CurrencySnapshot currency = currencyService.snapshot(request.currencyCode(), request.currencyName(), request.exchangeRateToCny());
        BigDecimal amount = money(request.amount());
        transaction.setProjectCode(blankToNull(request.projectCode()));
        transaction.setProjectName(blankToNull(request.projectName()));
        transaction.setPartnerName(blankToNull(request.partnerName()));
        transaction.setBankAccount(blankToNull(request.bankAccount()));
        transaction.setSettlementMethod(blankToNull(request.settlementMethod()));
        transaction.setAmount(amount);
        transaction.setCurrencyCode(currency.currencyCode());
        transaction.setCurrencyName(currency.currencyName());
        transaction.setExchangeRateToCny(currency.exchangeRateToCny());
        transaction.setAmountCny(currencyService.toCnyAmount(amount, currency));
        transaction.setRelatedBizNo(blankToNull(request.relatedBizNo()));
        transaction.setSummary(request.summary());
        transaction.setRemark(request.remark());
    }

    /**
     * 生成出纳流水号。
     *
     * <p>实现步骤：使用 CT + yyyyMMdd + 4 位序号生成账套内唯一编号。</p>
     */
    private String nextNo(LocalDate date) {
        String prefix = "CT" + date.format(DateTimeFormatter.BASIC_ISO_DATE);
        String companyCode = CompanyScope.currentCompanyCode();
        return numberSequenceService.next(
                "CASHIER_TRANSACTION",
                companyCode,
                prefix,
                () -> repository.findFirstByOrganizationCodeAndTransactionNoStartingWithOrderByTransactionNoDesc(companyCode, prefix)
                        .map(transaction -> transactionNoSequence(transaction.getTransactionNo(), prefix) + 1)
                        .orElse(1),
                no -> repository.existsByOrganizationCodeAndTransactionNo(companyCode, no)
        );
    }

    /** 提取出纳流水号末尾序号。 */
    private int transactionNoSequence(String transactionNo, String prefix) {
        if (transactionNo == null || !transactionNo.startsWith(prefix)) {
            return 0;
        }
        try {
            return Integer.parseInt(transactionNo.substring(prefix.length()));
        } catch (NumberFormatException ex) {
            return 0;
        }
    }

    /**
     * 按选中 ID 查询导出数据。
     *
     * <p>实现步骤：去重并保留前端选择顺序，再过滤当前账套内数据。</p>
     */
    private List<CashierTransactionView> selectedRows(List<Long> ids) {
        List<Long> exportIds = ids.stream().filter(Objects::nonNull).distinct().limit(exportProperties.maxRows()).toList();
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

    /** 判断是否存在有效选中 ID。 */
    private boolean hasSelectedIds(List<Long> ids) {
        return ids != null && ids.stream().anyMatch(Objects::nonNull);
    }

    /** 清理批量删除 ID。 */
    private List<Long> normalizeBatchIds(List<Long> ids) {
        if (ids == null) {
            throw new BusinessException(ResponseCode.ILLEGAL_PARAM, "请选择需要删除的数据");
        }
        List<Long> normalizedIds = ids.stream().filter(Objects::nonNull).distinct().toList();
        if (normalizedIds.isEmpty()) {
            throw new BusinessException(ResponseCode.ILLEGAL_PARAM, "请选择需要删除的数据");
        }
        return normalizedIds;
    }

    /** 空白字符串转为空值。 */
    private String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }

    /** 统一金额精度。 */
    private BigDecimal money(BigDecimal value) {
        return (value == null ? BigDecimal.ZERO : value).setScale(CurrencyService.MONEY_SCALE, RoundingMode.HALF_UP);
    }

    /** 出纳流水类型中文化。 */
    private String cashierTypeText(CashierTransactionType type) {
        return switch (type) {
            case RECEIPT -> "收款";
            case PAYMENT -> "付款";
            case TRANSFER -> "转账";
            case REFUND -> "退款";
        };
    }

    /** 出纳流水状态中文化。 */
    private String cashierStatusText(CashierTransactionStatus status) {
        return switch (status) {
            case DRAFT -> "草稿";
            case CONFIRMED -> "已确认";
            case VOUCHERED -> "已制证";
            case CANCELLED -> "已取消";
        };
    }

    /** 将出纳实体转换为前端视图。 */
    private CashierTransactionView toView(CashierTransaction transaction) {
        return new CashierTransactionView(
                transaction.getId(),
                transaction.getOrganizationCode(),
                transaction.getTransactionNo(),
                transaction.getTransactionDate(),
                transaction.getTransactionType(),
                transaction.getStatus(),
                transaction.getProjectCode(),
                transaction.getProjectName(),
                transaction.getPartnerName(),
                transaction.getBankAccount(),
                transaction.getSettlementMethod(),
                money(transaction.getAmount()),
                transaction.getCurrencyCode(),
                transaction.getCurrencyName(),
                transaction.getExchangeRateToCny(),
                money(transaction.getAmountCny()),
                transaction.getRelatedBizNo(),
                transaction.getSummary(),
                transaction.getRemark(),
                transaction.getCreatedBy(),
                transaction.getConfirmedBy(),
                transaction.getConfirmedTime(),
                transaction.getVoucherId(),
                transaction.getVoucherNo()
        );
    }
}
