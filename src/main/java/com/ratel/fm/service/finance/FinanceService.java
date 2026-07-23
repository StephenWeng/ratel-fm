package com.ratel.fm.service.finance;

import com.ratel.fm.common.BusinessException;
import com.ratel.fm.common.ResponseCode;
import com.ratel.fm.common.SearchSpecs;
import com.ratel.fm.domain.cashier.CashierTransaction;
import com.ratel.fm.domain.cashier.CashierTransactionStatus;
import com.ratel.fm.domain.finance.AccountingSourceType;
import com.ratel.fm.domain.finance.AccountingSubject;
import com.ratel.fm.domain.attachment.AttachmentBusinessType;
import com.ratel.fm.domain.finance.SubjectCategory;
import com.ratel.fm.domain.finance.Voucher;
import com.ratel.fm.domain.finance.VoucherLine;
import com.ratel.fm.domain.finance.VoucherStatus;
import com.ratel.fm.domain.inventory.InventoryLedger;
import com.ratel.fm.domain.purchase.PurchaseOrder;
import com.ratel.fm.domain.receivable.ArApBill;
import com.ratel.fm.domain.receivable.ArApType;
import com.ratel.fm.repository.cashier.CashierTransactionRepository;
import com.ratel.fm.repository.finance.AccountingSubjectRepository;
import com.ratel.fm.repository.finance.VoucherRepository;
import com.ratel.fm.repository.inventory.InventoryLedgerRepository;
import com.ratel.fm.repository.purchase.PurchaseOrderRepository;
import com.ratel.fm.repository.receivable.ArApBillRepository;
import com.ratel.fm.security.CompanyScope;
import com.ratel.fm.security.SecurityUtils;
import com.ratel.fm.service.audit.AuditLogService;
import com.ratel.fm.config.export.ExportProperties;
import com.ratel.fm.service.attachment.AttachmentService;
import com.ratel.fm.service.basic.CurrencyService;
import com.ratel.fm.service.basic.CurrencyService.CurrencySnapshot;
import com.ratel.fm.service.cashier.CashierService;
import com.ratel.fm.service.export.ExcelExportService;
import com.ratel.fm.service.export.ExcelExportService.ExcelColumn;
import com.ratel.fm.service.knowledge.KnowledgeIndexService;
import com.ratel.fm.service.operationlog.BusinessOperationLogService;
import com.ratel.fm.web.dto.finance.FinanceDtos.SubjectRequest;
import com.ratel.fm.web.dto.finance.FinanceDtos.SubjectView;
import com.ratel.fm.web.dto.finance.FinanceDtos.AccountingSourceView;
import com.ratel.fm.web.dto.finance.FinanceDtos.AutoVoucherRequest;
import com.ratel.fm.web.dto.finance.FinanceDtos.AutoVoucherResult;
import com.ratel.fm.web.dto.finance.FinanceDtos.TrialBalanceRow;
import com.ratel.fm.web.dto.finance.FinanceDtos.VoucherExportRequest;
import com.ratel.fm.web.dto.finance.FinanceDtos.VoucherLineRequest;
import com.ratel.fm.web.dto.finance.FinanceDtos.VoucherLineView;
import com.ratel.fm.web.dto.finance.FinanceDtos.VoucherRequest;
import com.ratel.fm.web.dto.finance.FinanceDtos.VoucherSourceDetail;
import com.ratel.fm.web.dto.finance.FinanceDtos.VoucherSourceField;
import com.ratel.fm.web.dto.finance.FinanceDtos.VoucherView;
import com.ratel.fm.web.dto.operationlog.BusinessOperationLogDtos.BusinessOperationLogPage;
import com.ratel.fm.web.dto.operationlog.BusinessOperationLogDtos.BusinessOperationLogView;
import org.springframework.http.HttpStatus;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

/**
 * 财务记账服务。
 *
 * <p>负责会计科目维护、凭证草稿、凭证修改、过账、作废和试算平衡统计。</p>
 *
 * <p>开发组织：ratel；开发人员：WenZhang；联系方式：18782945613。</p>
 *
 * @author ratel
 */
@Service
public class FinanceService {

    /**
     * 字段 subjectRepository：保存 subjectRepository 对应的业务数据、运行配置或依赖对象，供本类逻辑读取和维护。
     */
    private final AccountingSubjectRepository subjectRepository;
    /**
     * 字段 voucherRepository：保存 voucherRepository 对应的业务数据、运行配置或依赖对象，供本类逻辑读取和维护。
     */
    private final VoucherRepository voucherRepository;
    /**
     * 字段 purchaseOrderRepository：读取采购单来源，支持会计平台从采购业务生成应付类凭证草稿。
     */
    private final PurchaseOrderRepository purchaseOrderRepository;
    /**
     * 字段 arApBillRepository：读取应收应付来源，支持会计平台从往来单据生成应收或应付确认凭证草稿。
     */
    private final ArApBillRepository arApBillRepository;
    /**
     * 字段 inventoryLedgerRepository：读取库存流水来源，支持会计平台按库存入出库业务生成凭证草稿。
     */
    private final InventoryLedgerRepository inventoryLedgerRepository;
    /**
     * 字段 cashierTransactionRepository：读取出纳流水来源，支持会计平台按已确认资金流水生成凭证草稿。
     */
    private final CashierTransactionRepository cashierTransactionRepository;
    /**
     * 字段 cashierService：自动制证成功后回写出纳流水已制证状态。
     */
    private final CashierService cashierService;
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
     * 字段 knowledgeIndexService：凭证新增、修改、过账、作废和删除后同步刷新 AI 知识索引。
     */
    private final KnowledgeIndexService knowledgeIndexService;

    /**
     * 构造 FinanceService 实例。
     * 
     * <p>实现步骤：
     * 1. 接收调用方传入的依赖对象或初始化参数；
     * 2. 保存到成员字段，保证后续业务方法可以复用；
     * 3. 完成实例初始化。</p>
     */
    public FinanceService(
            AccountingSubjectRepository subjectRepository,
            VoucherRepository voucherRepository,
            PurchaseOrderRepository purchaseOrderRepository,
            ArApBillRepository arApBillRepository,
            InventoryLedgerRepository inventoryLedgerRepository,
            CashierTransactionRepository cashierTransactionRepository,
            CashierService cashierService,
            AuditLogService auditLogService,
            ExportProperties exportProperties,
            ExcelExportService excelExportService,
            CurrencyService currencyService,
            AttachmentService attachmentService,
            BusinessOperationLogService businessOperationLogService,
            KnowledgeIndexService knowledgeIndexService
    ) {
        this.subjectRepository = subjectRepository;
        this.voucherRepository = voucherRepository;
        this.purchaseOrderRepository = purchaseOrderRepository;
        this.arApBillRepository = arApBillRepository;
        this.inventoryLedgerRepository = inventoryLedgerRepository;
        this.cashierTransactionRepository = cashierTransactionRepository;
        this.cashierService = cashierService;
        this.auditLogService = auditLogService;
        this.exportProperties = exportProperties;
        this.excelExportService = excelExportService;
        this.currencyService = currencyService;
        this.attachmentService = attachmentService;
        this.businessOperationLogService = businessOperationLogService;
        this.knowledgeIndexService = knowledgeIndexService;
    }

    /**
     * 查询会计科目列表。
     *
     * <p>实现步骤：
     * 1. 读取全部科目并按科目编码排序；
     * 2. 管理页面查询全部时直接返回，保留停用节点用于维护；
     * 3. 业务页面只查启用时，过滤掉自身停用、任一上级停用或存在子级的分组科目，保证凭证只使用叶子科目。</p>
     */
    @Transactional(readOnly = true)
    public List<SubjectView> listSubjects(boolean onlyEnabled) {
        return listSubjects(onlyEnabled, null, null, null, null, null, null);
    }

    /**
     * 按字段查询会计科目列表。
     *
     * <p>实现步骤：
     * 1. 编码、名称、说明使用包含匹配；
     * 2. 类别、父级、启用状态使用等值匹配；
     * 3. 业务只查启用时，继续按“自身和所有上级均启用、且自身为叶子科目”过滤，保证凭证下拉不展示分组科目。</p>
     */
    @Transactional(readOnly = true)
    public List<SubjectView> listSubjects(
            boolean onlyEnabled,
            String code,
            String name,
            SubjectCategory category,
            Long parentId,
            Boolean enabled,
            String description
    ) {
        /**
         * 会计科目查询条件，包含当前账套隔离、编码名称、类别、父级、启停和说明筛选。
         */
        var spec = CompanyScope.<AccountingSubject>currentCompanySpec()
                .and(SearchSpecs.like("code", code))
                .and(SearchSpecs.like("name", name))
                .and(SearchSpecs.equal("category", category))
                .and(parentId == null ? SearchSpecs.unrestricted() : (root, query, cb) -> cb.equal(root.get("parent").get("id"), parentId))
                .and(SearchSpecs.equal("enabled", enabled))
                .and(SearchSpecs.like("description", description));
        // 步骤1：统一按科目编码正序排序，符合财务科目按编码阅读和录入的习惯。
        List<AccountingSubject> subjects = subjectRepository.findAll(spec, Sort.by(Sort.Direction.ASC, "code", "id"));
        // 步骤2：管理页面需要看到停用节点，方便重新启用或调整层级。
        if (!onlyEnabled) {
            return subjects.stream().map(this::toSubjectView).toList();
        }
        // 步骤3：业务页面只展示“自身和所有上级均启用，且没有下级”的叶子科目。
        return subjects.stream()
                .filter(subject -> isSubjectVisibleForBusiness(subject) && isLeafSubject(subject))
                .map(this::toSubjectView)
                .toList();
    }

    /**
     * 新增会计科目。
     *
     * <p>实现步骤：
     * 1. 校验科目编码唯一；
     * 2. 写入科目基础字段并计算层级；
     * 3. 保存科目；
     * 4. 记录关键财务操作日志。</p>
     */
    @Transactional
    public SubjectView createSubject(SubjectRequest request) {
        // 步骤1：科目编码是记账和报表的关键业务编码，必须唯一。
        String organizationCode = CompanyScope.currentCompanyCode();
        if (subjectRepository.existsByOrganizationCodeAndCode(organizationCode, request.code())) {
            throw new BusinessException("科目编码已存在");
        }
        // 步骤2-3：applySubject 会解析父级并计算 subjectLevel。
        AccountingSubject subject = new AccountingSubject();
        applySubject(subject, request);
        // 变量说明：view 保存当前步骤计算、查询或转换得到的中间结果。
        AccountingSubject saved = subjectRepository.save(subject);
        SubjectView view = toSubjectView(saved);
        knowledgeIndexService.rebuildSubject(saved);
        // 步骤4：科目变更会影响后续凭证录入和报表口径，必须记录审计日志。
        auditLogService.finance("CREATE_SUBJECT", request, "SUCCESS",
                "会计科目新增了科目" + view.code() + " " + view.name() + "。");
        return view;
    }

    /**
     * 修改会计科目。
     *
     * <p>实现步骤：
     * 1. 读取现有科目；
     * 2. 校验新编码没有被其他科目占用；
     * 3. 停用存在启用后代的科目时校验二次确认；
     * 4. 更新字段和层级；
     * 5. 记录关键操作日志。</p>
     */
    @Transactional
    public SubjectView updateSubject(Long id, SubjectRequest request) {
        // 步骤1：修改必须基于已存在的科目。
        AccountingSubject subject = subjectRepository.findById(id)
                .orElseThrow(() -> new BusinessException(HttpStatus.NOT_FOUND, "科目不存在"));
        CompanyScope.requireCurrentCompany(subject.getOrganizationCode(), "科目");
        // 步骤2：允许保持原编码，但不允许改成其他科目的编码。
        subjectRepository.findByOrganizationCodeAndCode(CompanyScope.currentCompanyCode(), request.code())
                .filter(existing -> !existing.getId().equals(id))
                .ifPresent(existing -> {
                    throw new BusinessException("科目编码已存在");
                });
        // 步骤3：停用有启用下级的父科目时，必须由前端完成二次确认。
        ensureDisableWithEnabledChildrenConfirmed(subject, request);
        // 步骤4：统一复用字段赋值逻辑，保证新增和修改口径一致。
        applySubject(subject, request);
        // 变量说明：view 保存当前步骤计算、查询或转换得到的中间结果。
        SubjectView view = toSubjectView(subject);
        knowledgeIndexService.rebuildSubject(subject);
        // 步骤5：科目修改可能影响后续查询和核算口径。
        auditLogService.finance("UPDATE_SUBJECT", request, "SUCCESS",
                "会计科目修改了科目" + view.code() + " " + view.name() + "。");
        return view;
    }

    /**
     * 删除会计科目。
     *
     * <p>实现步骤：
     * 1. 校验科目存在；
     * 2. 删除科目；
     * 3. 记录审计日志，提示可能影响历史凭证查询。</p>
     */
    @Transactional
    public void deleteSubject(Long id) {
        AccountingSubject subject = subjectRepository.findById(id)
                .orElseThrow(() -> new BusinessException(HttpStatus.NOT_FOUND, "科目不存在"));
        CompanyScope.requireCurrentCompany(subject.getOrganizationCode(), "科目");
        // 变量说明：subjectText 保存当前步骤计算、查询或转换得到的中间结果。
        String subjectText = subject.getCode() + " " + subject.getName();
        knowledgeIndexService.deleteSubject(id);
        subjectRepository.delete(subject);
        auditLogService.finance("DELETE_SUBJECT", "subjectId=" + id, "SUCCESS",
                "会计科目删除了科目" + subjectText + "，可能影响历史凭证查询。");
    }

    /**
     * 批量删除会计科目。
     *
     * <p>实现步骤：
     * 1. 清理并去重前端传入的科目 ID；
     * 2. 逐个复用单条删除逻辑，保持存在性校验和审计口径一致；
     * 3. 在同一事务内执行，任一科目删除失败则整体回滚；
     * 4. 记录批量删除审计日志。</p>
     */
    @Transactional
    public void deleteSubjects(List<Long> ids) {
        // 变量说明：deleteIds 保存当前步骤计算、查询或转换得到的中间结果。
        List<Long> deleteIds = normalizeBatchIds(ids);
        // 变量说明：subjects 保存当前步骤计算、查询或转换得到的中间结果。
        List<String> subjects = new java.util.ArrayList<>();
        for (Long id : deleteIds) {
            AccountingSubject subject = subjectRepository.findById(id)
                    .orElseThrow(() -> new BusinessException(HttpStatus.NOT_FOUND, "科目不存在: " + id));
            CompanyScope.requireCurrentCompany(subject.getOrganizationCode(), "科目");
            subjects.add(subject.getCode() + " " + subject.getName());
            knowledgeIndexService.deleteSubject(id);
            subjectRepository.delete(subject);
        }
        auditLogService.finance("BATCH_DELETE_SUBJECTS", "subjectIds=" + deleteIds,
                "SUCCESS", "会计科目批量删除了科目: " + String.join("、", subjects) + "。");
    }

    /**
     * 新增凭证草稿。
     *
     * <p>实现步骤：
     * 1. 按凭证日期生成唯一凭证号；
     * 2. 写入凭证主表字段和创建人；
     * 3. 替换并校验分录，确保借贷平衡；
     * 4. 保存凭证；
     * 5. 记录关键财务操作日志。</p>
     */
    @Transactional
    public VoucherView createVoucher(VoucherRequest request) {
        // 步骤1-2：凭证先以草稿创建，只有过账后才进入报表统计。
        Voucher voucher = new Voucher();
        voucher.setOrganizationCode(CompanyScope.currentCompanyCode());
        voucher.setVoucherNo(nextVoucherNo(request.voucherDate()));
        voucher.setVoucherDate(request.voucherDate());
        voucher.setBelongMonth(resolveBelongMonth(request.belongMonth(), request.voucherDate()));
        voucher.setProjectCode(firstText(request.projectCode(), null));
        voucher.setProjectName(firstText(request.projectName(), null));
        voucher.setSummary(request.summary());
        voucher.setSourceBizNo(firstText(request.sourceBizNo(), null));
        voucher.setSourceType(request.sourceType());
        voucher.setSourceId(request.sourceId());
        voucher.setSourceTitle(firstText(request.sourceTitle(), null));
        voucher.setCreatedBy(SecurityUtils.currentUser().username());
        // 步骤3：分录校验包含科目启用、单行借贷互斥、整张凭证人民币金额借贷平衡，并保存行级币种和汇率快照。
        replaceVoucherLines(voucher, request);
        // 变量说明：view 保存当前步骤计算、查询或转换得到的中间结果。
        Voucher saved = voucherRepository.save(voucher);
        knowledgeIndexService.rebuildVoucher(saved);
        VoucherView view = toVoucherView(saved);
        businessOperationLogService.record("VOUCHER", view.id(), view.voucherNo(), voucherTitle(view), "CREATE", "新增凭证",
                "新增凭证号" + view.voucherNo() + "，摘要为" + view.summary() + "。", null, voucherStatusText(view.status()), view);
        // 步骤4：凭证新增属于核心财务操作，必须落库审计。
        auditLogService.finance("CREATE_VOUCHER", request, "SUCCESS",
                "凭证记账新增了凭证号" + view.voucherNo() + "。");
        return view;
    }

    /**
     * 修改凭证草稿。
     *
     * <p>实现步骤：
     * 1. 读取凭证及分录；
     * 2. 仅允许修改草稿凭证；
     * 3. 更新主表字段；
     * 4. 清空旧分录并重建新分录；
     * 5. 记录关键操作日志。</p>
     */
    @Transactional
    public VoucherView updateVoucher(Long id, VoucherRequest request) {
        // 步骤1：带分录读取，避免更新时触发懒加载问题。
        Voucher voucher = voucherRepository.findWithLinesByOrganizationCodeAndId(CompanyScope.currentCompanyCode(), id)
                .orElseThrow(() -> new BusinessException(HttpStatus.NOT_FOUND, "凭证不存在"));
        // 步骤2：已过账凭证不能直接修改，避免破坏报表统计结果。
        if (voucher.getStatus() != VoucherStatus.DRAFT) {
            throw new BusinessException("仅草稿凭证允许修改");
        }
        // 步骤3-4：主表字段和分录整体替换，保持请求即最终状态。
        voucher.setVoucherDate(request.voucherDate());
        voucher.setBelongMonth(resolveBelongMonth(request.belongMonth(), request.voucherDate()));
        voucher.setProjectCode(firstText(request.projectCode(), null));
        voucher.setProjectName(firstText(request.projectName(), null));
        voucher.setSummary(request.summary());
        voucher.setSourceBizNo(firstText(request.sourceBizNo(), null));
        // 步骤3补充：自动凭证的来源链路不可被旧表单误清空；只有请求明确携带来源字段时才更新。
        if (request.sourceType() != null || request.sourceId() != null || firstText(request.sourceTitle(), null) != null) {
            voucher.setSourceType(request.sourceType());
            voucher.setSourceId(request.sourceId());
            voucher.setSourceTitle(firstText(request.sourceTitle(), null));
        }
        replaceVoucherLines(voucher, request);
        // 变量说明：view 保存当前步骤计算、查询或转换得到的中间结果。
        knowledgeIndexService.rebuildVoucher(voucher);
        VoucherView view = toVoucherView(voucher);
        businessOperationLogService.record("VOUCHER", view.id(), view.voucherNo(), voucherTitle(view), "UPDATE", "修改凭证",
                "修改凭证号" + view.voucherNo() + "，摘要为" + view.summary() + "。", voucherStatusText(view.status()), voucherStatusText(view.status()), view);
        // 步骤5：凭证修改可能改变财务核算内容，必须记录。
        auditLogService.finance("UPDATE_VOUCHER", request, "SUCCESS",
                "凭证记账修改了凭证号" + view.voucherNo() + "。");
        return view;
    }

    /**
     * 查询会计平台可制证业务来源。
     *
     * <p>实现步骤：
     * 1. 按来源类型读取最近业务单据；
     * 2. 将采购单、应收应付单、库存流水和出纳流水统一转换为会计平台来源行；
     * 3. 标记来源单据是否已经存在未作废凭证，帮助前端避免重复制证；
     * 4. 返回按业务日期倒序排列的来源清单。</p>
     */
    @Transactional(readOnly = true)
    public List<AccountingSourceView> accountingSources(AccountingSourceType sourceType) {
        AccountingSourceType resolvedType = sourceType == null ? AccountingSourceType.PURCHASE_ORDER : sourceType;
        if (resolvedType == AccountingSourceType.PURCHASE_ORDER) {
            return purchaseOrderRepository.findTop50ByOrganizationCodeOrderByOrderDateDesc(CompanyScope.currentCompanyCode()).stream()
                    .map(this::purchaseAccountingSource)
                    .toList();
        }
        if (resolvedType == AccountingSourceType.AR_AP_BILL) {
            return arApBillRepository.findTop100ByOrderByDueDateAsc().stream()
                    .filter(bill -> Objects.equals(CompanyScope.currentCompanyCode(), bill.getOrganizationCode()))
                    .sorted(Comparator.comparing(ArApBill::getBillDate, Comparator.nullsLast(Comparator.reverseOrder())))
                    .limit(50)
                    .map(this::arApAccountingSource)
                    .toList();
        }
        if (resolvedType == AccountingSourceType.INVENTORY_LEDGER) {
            return inventoryLedgerRepository.findAll(CompanyScope.<InventoryLedger>currentCompanySpec(),
                            PageRequest.of(0, 50, Sort.by(Sort.Direction.DESC, "movementDate", "id"))).stream()
                    .map(this::inventoryAccountingSource)
                    .toList();
        }
        return cashierTransactionRepository.findTop50ByOrganizationCodeOrderByTransactionDateDesc(CompanyScope.currentCompanyCode()).stream()
                .filter(bill -> Objects.equals(CompanyScope.currentCompanyCode(), bill.getOrganizationCode()))
                .filter(transaction -> transaction.getStatus() == CashierTransactionStatus.CONFIRMED
                        || transaction.getStatus() == CashierTransactionStatus.VOUCHERED)
                .map(this::cashierAccountingSource)
                .toList();
    }

    /**
     * 根据业务来源生成凭证草稿。
     *
     * <p>实现步骤：
     * 1. 读取采购单、应收应付单、库存流水或出纳流水来源，并组装统一来源信息；
     * 2. 默认拦截已有未作废凭证的来源单据，避免同一业务重复入账；
     * 3. 校验借方、贷方科目都是可用于业务的启用科目；
     * 4. 按来源金额生成借贷两条分录，摘要、项目、币种和汇率沿用业务来源；
     * 5. 复用现有凭证创建流程保存草稿并记录业务流水和审计日志。</p>
     */
    @Transactional
    public AutoVoucherResult generateAutoVoucher(AutoVoucherRequest request) {
        // 步骤1：读取来源业务并转换为统一来源行，后续凭证生成不再关心业务表差异。
        AccountingSourceView source = requireAccountingSource(request.sourceType(), request.sourceId());
        // 步骤2：除非用户显式允许重复，否则同一来源存在未作废凭证时直接拦截。
        if (!Boolean.TRUE.equals(request.allowDuplicate()) && hasGeneratedVoucher(source.sourceType(), source.sourceId(), source.sourceNo())) {
            throw new BusinessException("该业务单据已经存在未作废凭证，请勿重复制证");
        }
        // 步骤3：借贷科目都必须是启用且父级链路启用的科目，避免自动生成不可用凭证。
        AccountingSubject debitSubject = requireBusinessSubject(request.debitSubjectId(), "借方科目不存在或已停用");
        AccountingSubject creditSubject = requireBusinessSubject(request.creditSubjectId(), "贷方科目不存在或已停用");
        if (debitSubject.getId().equals(creditSubject.getId())) {
            throw new BusinessException("借方科目和贷方科目不能相同");
        }
        // 步骤4：来源金额必须大于 0，金额为 0 的业务单据没有制证意义。
        BigDecimal amount = source.amount() == null ? BigDecimal.ZERO : source.amount();
        if (amount.signum() <= 0) {
            throw new BusinessException("来源单据金额必须大于0才能生成凭证");
        }
        String summary = firstText(request.summary(), autoVoucherSummary(source));
        BigDecimal exchangeRate = source.exchangeRateToCny() == null ? BigDecimal.ONE : source.exchangeRateToCny();
        String currencyCode = firstText(source.currencyCode(), CurrencyService.DEFAULT_CURRENCY_CODE);
        String currencyName = firstText(source.currencyName(), CurrencyService.DEFAULT_CURRENCY_NAME);
        VoucherRequest voucherRequest = new VoucherRequest(
                source.businessDate() == null ? LocalDate.now() : source.businessDate(),
                null,
                source.projectCode(),
                source.projectName(),
                summary,
                source.sourceNo(),
                source.sourceType(),
                source.sourceId(),
                source.sourceTitle(),
                currencyCode,
                currencyName,
                exchangeRate,
                List.of(
                        new VoucherLineRequest(debitSubject.getId(), summary, amount, BigDecimal.ZERO, currencyCode, currencyName, exchangeRate, source.sourceTitle()),
                        new VoucherLineRequest(creditSubject.getId(), summary, BigDecimal.ZERO, amount, currencyCode, currencyName, exchangeRate, source.sourceTitle())
                )
        );
        // 步骤5：复用 createVoucher 的平衡校验、币种快照、流水和审计，保证手工凭证与自动凭证口径一致。
        VoucherView voucher = createVoucher(voucherRequest);
        markSourceVoucherGenerated(source, voucher);
        auditLogService.finance("AUTO_GENERATE_VOUCHER", request, "SUCCESS",
                "会计平台根据" + source.sourceTitle() + "生成凭证号" + voucher.voucherNo() + "。");
        return new AutoVoucherResult(voucher, source, "已生成凭证草稿：" + voucher.voucherNo());
    }

    /**
     * 凭证过账。
     *
     * <p>实现步骤：
     * 1. 读取凭证及分录；
     * 2. 仅允许草稿凭证过账；
     * 3. 修改状态为 POSTED 并记录过账人；
     * 4. 记录审计日志。过账后凭证进入报表统计。</p>
     */
    @Transactional
    public VoucherView postVoucher(Long id) {
        Voucher voucher = voucherRepository.findWithLinesByOrganizationCodeAndId(CompanyScope.currentCompanyCode(), id)
                .orElseThrow(() -> new BusinessException(HttpStatus.NOT_FOUND, "凭证不存在"));
        if (voucher.getStatus() != VoucherStatus.DRAFT) {
            throw new BusinessException("仅草稿凭证允许过账");
        }
        voucher.setStatus(VoucherStatus.POSTED);
        voucher.setPostedBy(SecurityUtils.currentUser().username());
        // 变量说明：view 保存当前步骤计算、查询或转换得到的中间结果。
        knowledgeIndexService.rebuildVoucher(voucher);
        VoucherView view = toVoucherView(voucher);
        businessOperationLogService.record("VOUCHER", view.id(), view.voucherNo(), voucherTitle(view), "POST", "凭证过账",
                "将凭证号" + view.voucherNo() + "过账，凭证进入报表统计。", voucherStatusText(VoucherStatus.DRAFT), voucherStatusText(view.status()), view);
        auditLogService.finance("POST_VOUCHER", "voucherId=" + id, "SUCCESS",
                "凭证记账将凭证号" + view.voucherNo() + "过账，凭证进入报表统计。");
        return view;
    }

    /**
     * 凭证作废。
     *
     * <p>实现步骤：
     * 1. 读取凭证及分录；
     * 2. 防止重复作废；
     * 3. 修改状态为 VOIDED；
     * 4. 记录审计日志。作废凭证不再作为有效业务凭证。</p>
     */
    @Transactional
    public VoucherView voidVoucher(Long id) {
        Voucher voucher = voucherRepository.findWithLinesByOrganizationCodeAndId(CompanyScope.currentCompanyCode(), id)
                .orElseThrow(() -> new BusinessException(HttpStatus.NOT_FOUND, "凭证不存在"));
        if (voucher.getStatus() == VoucherStatus.VOIDED) {
            throw new BusinessException("凭证已作废");
        }
        voucher.setStatus(VoucherStatus.VOIDED);
        // 变量说明：view 保存当前步骤计算、查询或转换得到的中间结果。
        knowledgeIndexService.rebuildVoucher(voucher);
        VoucherView view = toVoucherView(voucher);
        businessOperationLogService.record("VOUCHER", view.id(), view.voucherNo(), voucherTitle(view), "VOID", "凭证作废",
                "作废凭证号" + view.voucherNo() + "，该凭证不再作为有效业务凭证。", null, voucherStatusText(view.status()), view);
        auditLogService.finance("VOID_VOUCHER", "voucherId=" + id, "SUCCESS",
                "凭证记账作废了凭证号" + view.voucherNo() + "，该凭证不再作为有效业务凭证。");
        return view;
    }

    /**
     * 批量删除凭证。
     *
     * <p>实现步骤：
     * 1. 清理并去重前端传入的凭证 ID；
     * 2. 逐个读取凭证，任一 ID 不存在则抛出异常；
     * 3. 删除凭证主表，明细通过 orphanRemoval 级联删除；
     * 4. 记录凭证删除审计日志。</p>
     */
    @Transactional
    public void deleteVouchers(List<Long> ids) {
        // 变量说明：deleteIds 保存当前步骤计算、查询或转换得到的中间结果。
        List<Long> deleteIds = normalizeBatchIds(ids);
        // 变量说明：voucherNos 保存当前步骤计算、查询或转换得到的中间结果。
        List<String> voucherNos = new java.util.ArrayList<>();
        for (Long id : deleteIds) {
            Voucher voucher = voucherRepository.findById(id)
                    .orElseThrow(() -> new BusinessException(HttpStatus.NOT_FOUND, "凭证不存在: " + id));
            CompanyScope.requireCurrentCompany(voucher.getOrganizationCode(), "凭证");
            voucherNos.add(voucher.getVoucherNo());
            businessOperationLogService.record("VOUCHER", voucher.getId(), voucher.getVoucherNo(), voucher.getSummary(), "DELETE", "删除凭证",
                    "删除凭证号" + voucher.getVoucherNo() + "。", voucherStatusText(voucher.getStatus()), "已删除", voucher.getVoucherNo());
            knowledgeIndexService.deleteVoucher(id);
            attachmentService.deleteAllForBusiness(AttachmentBusinessType.VOUCHER, id);
            voucherRepository.delete(voucher);
        }
        auditLogService.finance("BATCH_DELETE_VOUCHERS", "voucherIds=" + deleteIds + ", voucherNos=" + voucherNos,
                "SUCCESS", "凭证记账删除了凭证号: " + String.join("、", voucherNos) + "。");
    }

    /**
     * 按日期区间查询凭证列表。
     *
     * <p>未传日期时默认查询最近一个月。</p>
     */
    @Transactional(readOnly = true)
    public List<VoucherView> listVouchers(LocalDate startDate, LocalDate endDate) {
        return listVouchers(startDate, endDate, null, null, null, null, null, null, null);
    }

    /**
     * 按字段查询凭证列表。
     *
     * <p>实现步骤：
     * 1. 日期为空时默认最近一个月；
     * 2. 凭证号、摘要、来源单号、制单人、过账人按包含匹配；
     * 3. 凭证状态按等值匹配。</p>
     */
    @Transactional(readOnly = true)
    public List<VoucherView> listVouchers(
            LocalDate startDate,
            LocalDate endDate,
            String belongMonth,
            String projectCode,
            String voucherNo,
            String summary,
            String sourceBizNo,
            VoucherStatus status,
            String createdBy
    ) {
        // 变量说明：start 保存当前步骤计算、查询或转换得到的中间结果。
        LocalDate start = startDate == null ? LocalDate.now().minusMonths(1) : startDate;
        // 变量说明：end 保存当前步骤计算、查询或转换得到的中间结果。
        LocalDate end = endDate == null ? LocalDate.now() : endDate;
        /**
         * 凭证列表查询条件，先限定当前账套，再叠加日期、期间、项目、凭证号、摘要和状态筛选。
         */
        var spec = CompanyScope.<Voucher>currentCompanySpec()
                .and(SearchSpecs.dateBetween("voucherDate", start, end))
                .and(SearchSpecs.equal("belongMonth", firstText(belongMonth, null)))
                .and(SearchSpecs.equal("projectCode", firstText(projectCode, null)))
                .and(SearchSpecs.like("voucherNo", voucherNo))
                .and(SearchSpecs.like("summary", summary))
                .and(SearchSpecs.like("sourceBizNo", sourceBizNo))
                .and(SearchSpecs.equal("status", status))
                .and(SearchSpecs.like("createdBy", createdBy));
        return voucherRepository.findAll(spec, Sort.by(Sort.Direction.DESC, "modifyTime", "id")).stream()
                .map(this::toVoucherView)
                .toList();
    }

    /**
     * 导出凭证列表。
     *
     * <p>实现步骤：
     * 1. 如果请求携带选中凭证 ID，则按选中 ID 读取并保留前端选择顺序；
     * 2. 如果没有选中 ID，则使用当前列表搜索条件查询；
     * 3. 无论哪种方式，都按配置的最大导出行数截断；
     * 4. 按凭证列表可见字段生成 Excel。</p>
     */
    @Transactional(readOnly = true)
    public byte[] exportVouchers(VoucherExportRequest request) {
        // 变量说明：exportRequest 保存当前步骤计算、查询或转换得到的中间结果。
        VoucherExportRequest exportRequest = request == null ? new VoucherExportRequest(null, null, null, null, null, null, null, null, null, null) : request;
        List<VoucherView> rows = hasSelectedIds(exportRequest.ids())
                ? selectedVoucherRows(exportRequest.ids())
                : searchVoucherRowsForExport(exportRequest);
        return excelExportService.export("凭证记账", List.of(
                new ExcelColumn<>("凭证号", VoucherView::voucherNo),
                new ExcelColumn<>("日期", VoucherView::voucherDate),
                new ExcelColumn<>("所属年月", VoucherView::belongMonth),
                new ExcelColumn<>("项目", VoucherView::projectName),
                new ExcelColumn<>("摘要", VoucherView::summary),
                new ExcelColumn<>("状态", row -> voucherStatusText(row.status())),
                new ExcelColumn<>("借方合计", VoucherView::totalDebit),
                new ExcelColumn<>("贷方合计", VoucherView::totalCredit),
                new ExcelColumn<>("币种", row -> currencyDisplay(row.currencyCode(), row.currencyName())),
                new ExcelColumn<>("汇率", VoucherView::exchangeRateToCny),
                new ExcelColumn<>("借方合计人民币", VoucherView::totalDebitCny),
                new ExcelColumn<>("贷方合计人民币", VoucherView::totalCreditCny),
                new ExcelColumn<>("制单人", VoucherView::createdBy)
        ), rows);
    }

    /**
     * 按选中 ID 查询凭证导出数据。
     */
    private List<VoucherView> selectedVoucherRows(List<Long> ids) {
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
        return voucherRepository.findAllById(exportIds).stream()
                .filter(row -> Objects.equals(CompanyScope.currentCompanyCode(), row.getOrganizationCode()))
                .sorted(Comparator.comparingInt(row -> orderMap.getOrDefault(row.getId(), Integer.MAX_VALUE)))
                .map(this::toVoucherView)
                .toList();
    }

    /**
     * 按搜索条件查询凭证导出数据。
     */
    private List<VoucherView> searchVoucherRowsForExport(VoucherExportRequest request) {
        // 变量说明：start 保存当前步骤计算、查询或转换得到的中间结果。
        LocalDate start = request.startDate() == null ? LocalDate.now().minusMonths(1) : request.startDate();
        // 变量说明：end 保存当前步骤计算、查询或转换得到的中间结果。
        LocalDate end = request.endDate() == null ? LocalDate.now() : request.endDate();
        /**
         * 凭证导出查询条件，复用列表筛选口径并限制在当前所属公司。
         */
        var spec = CompanyScope.<Voucher>currentCompanySpec()
                .and(SearchSpecs.dateBetween("voucherDate", start, end))
                .and(SearchSpecs.equal("belongMonth", firstText(request.belongMonth(), null)))
                .and(SearchSpecs.equal("projectCode", firstText(request.projectCode(), null)))
                .and(SearchSpecs.like("voucherNo", request.voucherNo()))
                .and(SearchSpecs.like("summary", request.summary()))
                .and(SearchSpecs.like("sourceBizNo", request.sourceBizNo()))
                .and(SearchSpecs.equal("status", request.status()))
                .and(SearchSpecs.like("createdBy", request.createdBy()));
        return voucherRepository.findAll(
                        spec,
                        PageRequest.of(0, exportProperties.maxRows(), Sort.by(Sort.Direction.DESC, "modifyTime", "id"))
                ).stream()
                .map(this::toVoucherView)
                .toList();
    }

    /**
     * 读取会计平台制证来源。
     *
     * <p>实现步骤：根据来源类型分别读取采购单、应收应付单、库存流水或出纳流水，读取失败时返回明确业务错误，再统一转换为来源行。</p>
     */
    private AccountingSourceView requireAccountingSource(AccountingSourceType sourceType, Long sourceId) {
        if (sourceType == AccountingSourceType.PURCHASE_ORDER) {
            PurchaseOrder order = purchaseOrderRepository.findWithLinesByOrganizationCodeAndId(CompanyScope.currentCompanyCode(), sourceId)
                    .orElseThrow(() -> new BusinessException(HttpStatus.NOT_FOUND, "采购单不存在"));
            return purchaseAccountingSource(order);
        }
        if (sourceType == AccountingSourceType.AR_AP_BILL) {
            ArApBill bill = arApBillRepository.findById(sourceId)
                    .orElseThrow(() -> new BusinessException(HttpStatus.NOT_FOUND, "应收应付单不存在"));
            CompanyScope.requireCurrentCompany(bill.getOrganizationCode(), "应收应付单");
            return arApAccountingSource(bill);
        }
        if (sourceType == AccountingSourceType.INVENTORY_LEDGER) {
            InventoryLedger ledger = inventoryLedgerRepository.findById(sourceId)
                    .orElseThrow(() -> new BusinessException(HttpStatus.NOT_FOUND, "库存流水不存在"));
            CompanyScope.requireCurrentCompany(ledger.getOrganizationCode(), "库存流水");
            return inventoryAccountingSource(ledger);
        }
        if (sourceType == AccountingSourceType.CASHIER_TRANSACTION) {
            CashierTransaction transaction = cashierService.requireTransaction(sourceId);
            return cashierAccountingSource(transaction);
        }
        throw new BusinessException(ResponseCode.ILLEGAL_PARAM, "不支持的会计来源类型");
    }

    /**
     * 将采购单转换为会计平台来源行。
     *
     * <p>实现步骤：读取采购主表金额、币种、项目、供应商和状态，判断来源单号是否已经存在未作废凭证。</p>
     */
    private AccountingSourceView purchaseAccountingSource(PurchaseOrder order) {
        return new AccountingSourceView(
                AccountingSourceType.PURCHASE_ORDER,
                order.getId(),
                order.getOrderNo(),
                "采购单 " + order.getOrderNo(),
                order.getProjectCode(),
                order.getProjectName(),
                order.getOrderDate(),
                order.getSupplierName(),
                normalizeMoney(order.getTotalAmount()),
                normalizeMoney(order.getTotalAmountCny()),
                firstText(order.getCurrencyCode(), CurrencyService.DEFAULT_CURRENCY_CODE),
                firstText(order.getCurrencyName(), CurrencyService.DEFAULT_CURRENCY_NAME),
                normalizeRate(order.getExchangeRateToCny()),
                purchaseStatusText(order.getStatus()),
                hasGeneratedVoucher(AccountingSourceType.PURCHASE_ORDER, order.getId(), order.getOrderNo())
        );
    }

    /**
     * 将应收应付单转换为会计平台来源行。
     *
     * <p>实现步骤：读取往来单据金额、币种、项目、往来单位和状态，判断来源单号是否已经存在未作废凭证。</p>
     */
    private AccountingSourceView arApAccountingSource(ArApBill bill) {
        return new AccountingSourceView(
                AccountingSourceType.AR_AP_BILL,
                bill.getId(),
                bill.getBillNo(),
                arApTypeText(bill.getBillType()) + "单 " + bill.getBillNo(),
                bill.getProjectCode(),
                bill.getProjectName(),
                bill.getBillDate(),
                bill.getPartnerName(),
                normalizeMoney(bill.getAmount()),
                normalizeMoney(bill.getAmountCny()),
                firstText(bill.getCurrencyCode(), CurrencyService.DEFAULT_CURRENCY_CODE),
                firstText(bill.getCurrencyName(), CurrencyService.DEFAULT_CURRENCY_NAME),
                normalizeRate(bill.getExchangeRateToCny()),
                arApStatusText(bill.getStatus()),
                hasGeneratedVoucher(AccountingSourceType.AR_AP_BILL, bill.getId(), bill.getBillNo())
        );
    }

    /**
     * 将库存流水转换为会计平台来源行。
     *
     * <p>实现步骤：读取库存流水数量、物料、项目、仓库和状态，数量作为库存业务来源金额基数，后续可由规则引擎接入单价成本。</p>
     */
    private AccountingSourceView inventoryAccountingSource(InventoryLedger ledger) {
        BigDecimal amount = normalizeMoney(ledger.getQuantity());
        return new AccountingSourceView(
                AccountingSourceType.INVENTORY_LEDGER,
                ledger.getId(),
                ledger.getMovementNo(),
                "库存流水 " + ledger.getMovementNo(),
                ledger.getProjectCode(),
                ledger.getProjectName(),
                ledger.getMovementDate(),
                ledger.getItemName(),
                amount,
                amount,
                CurrencyService.DEFAULT_CURRENCY_CODE,
                CurrencyService.DEFAULT_CURRENCY_NAME,
                BigDecimal.ONE.setScale(CurrencyService.MONEY_SCALE, RoundingMode.HALF_UP),
                inventoryMovementTypeText(ledger.getMovementType()),
                hasGeneratedVoucher(AccountingSourceType.INVENTORY_LEDGER, ledger.getId(), ledger.getMovementNo())
        );
    }

    /**
     * 将出纳流水转换为会计平台来源行。
     *
     * <p>实现步骤：读取资金金额、币种、往来单位、项目和状态，作为现金银行类凭证来源。</p>
     */
    private AccountingSourceView cashierAccountingSource(CashierTransaction transaction) {
        return new AccountingSourceView(
                AccountingSourceType.CASHIER_TRANSACTION,
                transaction.getId(),
                transaction.getTransactionNo(),
                "出纳流水 " + transaction.getTransactionNo(),
                transaction.getProjectCode(),
                transaction.getProjectName(),
                transaction.getTransactionDate(),
                transaction.getPartnerName(),
                normalizeMoney(transaction.getAmount()),
                normalizeMoney(transaction.getAmountCny()),
                firstText(transaction.getCurrencyCode(), CurrencyService.DEFAULT_CURRENCY_CODE),
                firstText(transaction.getCurrencyName(), CurrencyService.DEFAULT_CURRENCY_NAME),
                normalizeRate(transaction.getExchangeRateToCny()),
                cashierStatusText(transaction.getStatus()),
                hasGeneratedVoucher(AccountingSourceType.CASHIER_TRANSACTION, transaction.getId(), transaction.getTransactionNo())
        );
    }

    /**
     * 判断来源业务是否已经存在未作废凭证。
     *
     * <p>实现步骤：
     * 1. 优先按来源类型和来源主键判断，避免不同模块相同单号互相影响；
     * 2. 如果是历史旧数据没有来源类型和来源主键，则退回按来源单号判断；
     * 3. 所有判断都限定当前所属公司，保证多公司账套隔离。</p>
     */
    private boolean hasGeneratedVoucher(AccountingSourceType sourceType, Long sourceId, String sourceNo) {
        // 步骤1：新链路以来源类型和来源主键作为强关联。
        if (sourceType != null && sourceId != null
                && voucherRepository.existsByOrganizationCodeAndSourceTypeAndSourceIdAndStatusNot(
                CompanyScope.currentCompanyCode(), sourceType, sourceId, VoucherStatus.VOIDED)) {
            return true;
        }
        // 步骤2：兼容旧凭证只写 sourceBizNo 的数据。
        String normalizedSourceNo = firstText(sourceNo, null);
        return normalizedSourceNo != null
                && voucherRepository.existsByOrganizationCodeAndSourceBizNoAndStatusNot(
                CompanyScope.currentCompanyCode(), normalizedSourceNo, VoucherStatus.VOIDED);
    }

    /**
     * 自动制证成功后回写来源单据的凭证关联字段。
     *
     * <p>实现步骤：
     * 1. 根据来源类型定位来源业务表；
     * 2. 按当前所属公司校验来源单据归属；
     * 3. 写入 voucherId 和 voucherNo，便于来源模块直接展示在线凭证入口。</p>
     */
    private void markSourceVoucherGenerated(AccountingSourceView source, VoucherView voucher) {
        // 步骤1：采购单生成凭证后，采购列表可直接展示凭证号和在线凭证按钮。
        if (source.sourceType() == AccountingSourceType.PURCHASE_ORDER) {
            PurchaseOrder order = purchaseOrderRepository.findWithLinesByOrganizationCodeAndId(CompanyScope.currentCompanyCode(), source.sourceId())
                    .orElseThrow(() -> new BusinessException(HttpStatus.NOT_FOUND, "采购单不存在"));
            order.setVoucherId(voucher.id());
            order.setVoucherNo(voucher.voucherNo());
            return;
        }
        // 步骤2：应收应付单生成凭证后，往来列表可直接追溯凭证。
        if (source.sourceType() == AccountingSourceType.AR_AP_BILL) {
            ArApBill bill = arApBillRepository.findById(source.sourceId())
                    .orElseThrow(() -> new BusinessException(HttpStatus.NOT_FOUND, "应收应付单不存在"));
            CompanyScope.requireCurrentCompany(bill.getOrganizationCode(), "应收应付单");
            bill.setVoucherId(voucher.id());
            bill.setVoucherNo(voucher.voucherNo());
            return;
        }
        // 步骤3：库存流水生成凭证后，库存台账列表可直接查看对应在线凭证。
        if (source.sourceType() == AccountingSourceType.INVENTORY_LEDGER) {
            InventoryLedger ledger = inventoryLedgerRepository.findById(source.sourceId())
                    .orElseThrow(() -> new BusinessException(HttpStatus.NOT_FOUND, "库存流水不存在"));
            CompanyScope.requireCurrentCompany(ledger.getOrganizationCode(), "库存流水");
            ledger.setVoucherId(voucher.id());
            ledger.setVoucherNo(voucher.voucherNo());
            return;
        }
        // 步骤4：出纳流水沿用既有服务方法，保持出纳侧状态回写口径不变。
        if (source.sourceType() == AccountingSourceType.CASHIER_TRANSACTION) {
            cashierService.markVoucherGenerated(source.sourceId(), voucher.id(), voucher.voucherNo());
        }
    }

    /**
     * 读取可用于业务制证的启用科目。
     *
     * <p>实现步骤：按主键查询科目，再复用业务可见性和叶子科目规则校验自身是否可入账。</p>
     */
    private AccountingSubject requireBusinessSubject(Long subjectId, String message) {
        AccountingSubject subject = subjectRepository.findById(subjectId)
                .orElseThrow(() -> new BusinessException(HttpStatus.NOT_FOUND, message));
        CompanyScope.requireCurrentCompany(subject.getOrganizationCode(), "科目");
        if (!isSubjectVisibleForBusiness(subject) || !isLeafSubject(subject)) {
            throw new BusinessException(message);
        }
        return subject;
    }

    /**
     * 生成自动凭证默认摘要。
     *
     * <p>实现步骤：优先使用来源标题和往来单位，形成财务人员可读的草稿摘要。</p>
     */
    private String autoVoucherSummary(AccountingSourceView source) {
        String partnerName = firstText(source.partnerName(), null);
        return partnerName == null
                ? "自动凭证-" + source.sourceTitle()
                : "自动凭证-" + source.sourceTitle() + "-" + partnerName;
    }

    /**
     * 查询单张凭证明细。
     */
    @Transactional(readOnly = true)
    public VoucherView getVoucher(Long id) {
        return voucherRepository.findWithLinesByOrganizationCodeAndId(CompanyScope.currentCompanyCode(), id)
                .map(this::toVoucherView)
                .orElseThrow(() -> new BusinessException(HttpStatus.NOT_FOUND, "凭证不存在"));
    }

    /**
     * 查询凭证来源业务详情。
     *
     * <p>实现步骤：
     * 1. 按当前所属公司读取凭证，确保不能跨公司查看来源；
     * 2. 校验凭证必须由业务模块自动生成，手工凭证没有来源详情；
     * 3. 根据来源类型和来源主键读取最新来源单据；
     * 4. 转换为通用字段列表，前端可在同一个弹窗展示不同模块来源。</p>
     */
    @Transactional(readOnly = true)
    public VoucherSourceDetail getVoucherSourceDetail(Long id) {
        // 步骤1：凭证是来源查看入口，必须先按账套隔离读取。
        Voucher voucher = voucherRepository.findWithLinesByOrganizationCodeAndId(CompanyScope.currentCompanyCode(), id)
                .orElseThrow(() -> new BusinessException(HttpStatus.NOT_FOUND, "凭证不存在"));
        // 步骤2：手工凭证没有 sourceType/sourceId，不允许前端误展示空来源。
        if (voucher.getSourceType() == null || voucher.getSourceId() == null) {
            throw new BusinessException("该凭证没有关联来源单据");
        }
        // 步骤3：复用会计平台来源读取逻辑，保证来源权限、账套隔离和状态文本一致。
        AccountingSourceView source = requireAccountingSource(voucher.getSourceType(), voucher.getSourceId());
        // 步骤4：组装通用字段，页面无需感知采购、应收应付、库存、出纳的实体差异。
        List<VoucherSourceField> fields = new ArrayList<>();
        fields.add(new VoucherSourceField("来源模块", sourceTypeText(source.sourceType())));
        fields.add(new VoucherSourceField("来源单号", defaultString(source.sourceNo(), "-")));
        fields.add(new VoucherSourceField("来源标题", defaultString(source.sourceTitle(), "-")));
        fields.add(new VoucherSourceField("业务日期", source.businessDate() == null ? "-" : source.businessDate().toString()));
        fields.add(new VoucherSourceField("项目", defaultString(source.projectName(), "-")));
        fields.add(new VoucherSourceField(sourcePartnerLabel(source.sourceType()), defaultString(source.partnerName(), "-")));
        fields.add(new VoucherSourceField("金额", moneyText(source.amount(), source.currencyName())));
        fields.add(new VoucherSourceField("人民币金额", moneyText(source.amountCny(), CurrencyService.DEFAULT_CURRENCY_NAME)));
        fields.add(new VoucherSourceField("币种/汇率", currencyDisplay(source.currencyCode(), source.currencyName()) + " / " + defaultRate(source.exchangeRateToCny())));
        fields.add(new VoucherSourceField("来源状态", defaultString(source.statusText(), "-")));
        return new VoucherSourceDetail(
                source.sourceType(),
                source.sourceId(),
                source.sourceNo(),
                source.sourceTitle(),
                sourceTypeText(source.sourceType()),
                fields
        );
    }

    /**
     * 查询凭证操作流水。
     *
     * <p>实现步骤：先确认凭证存在，再按时间正序返回流水，前端以时间轴展示。</p>
     */
    @Transactional(readOnly = true)
    public List<BusinessOperationLogView> listVoucherOperationLogs(Long id) {
        voucherRepository.findWithLinesByOrganizationCodeAndId(CompanyScope.currentCompanyCode(), id)
                .orElseThrow(() -> new BusinessException(HttpStatus.NOT_FOUND, "凭证不存在"));
        return businessOperationLogService.list("VOUCHER", id);
    }

    /**
     * 分页查询凭证操作流水。
     *
     * <p>实现步骤：先确认凭证存在，再按操作时间范围和分页条件查询，前端右侧抽屉滚动加载。</p>
     */
    @Transactional(readOnly = true)
    public BusinessOperationLogPage pageVoucherOperationLogs(Long id, OffsetDateTime startTime, OffsetDateTime endTime, int page, int size) {
        voucherRepository.findWithLinesByOrganizationCodeAndId(CompanyScope.currentCompanyCode(), id)
                .orElseThrow(() -> new BusinessException(HttpStatus.NOT_FOUND, "凭证不存在"));
        return businessOperationLogService.page("VOUCHER", id, startTime, endTime, page, size);
    }

    /**
     * 生成试算平衡表。
     *
     * <p>实现步骤：
     * 1. 计算查询期间，默认当前月；
     * 2. 查询期间内凭证；
     * 3. 只统计已过账凭证；
     * 4. 按科目汇总借方和贷方发生额；
     * 5. 按科目编码排序返回。</p>
     */
    @Transactional(readOnly = true)
    public List<TrialBalanceRow> trialBalance(LocalDate startDate, LocalDate endDate) {
        // 步骤1：默认当前月，方便首页或报表页面直接调用。
        LocalDate start = startDate == null ? LocalDate.now().withDayOfMonth(1) : startDate;
        // 变量说明：end 保存当前步骤计算、查询或转换得到的中间结果。
        LocalDate end = endDate == null ? LocalDate.now() : endDate;
        // 变量说明：rows 保存当前步骤计算、查询或转换得到的中间结果。
        Map<Long, TrialAccumulator> rows = new LinkedHashMap<>();
        // 步骤2-4：试算平衡只统计 POSTED 凭证，草稿和作废凭证不参与。
        voucherRepository.findByOrganizationCodeAndVoucherDateBetweenOrderByVoucherDateDesc(
                        CompanyScope.currentCompanyCode(), start, end).stream()
                .filter(voucher -> voucher.getStatus() == VoucherStatus.POSTED)
                .flatMap(voucher -> voucher.getLines().stream())
                .forEach(line -> rows.computeIfAbsent(line.getSubject().getId(), ignored -> new TrialAccumulator(line.getSubject()))
                        .add(cnyDebit(line), cnyCredit(line)));
        // 步骤5：按科目编码排序，符合财务报表阅读习惯。
        return rows.values().stream()
                .map(TrialAccumulator::toRow)
                .sorted(Comparator.comparing(TrialBalanceRow::subjectCode))
                .toList();
    }

    /**
     * 将科目请求写入科目实体，并计算父子层级。
     */
    private void applySubject(AccountingSubject subject, SubjectRequest request) {
        // 变量说明：parent 保存当前步骤计算、查询或转换得到的中间结果。
        AccountingSubject parent = resolveSubjectParent(subject.getId(), request.parentId());
        subject.setOrganizationCode(CompanyScope.currentCompanyCode());
        subject.setCode(request.code());
        subject.setName(request.name());
        subject.setCategory(request.category());
        subject.setParent(parent);
        subject.setSubjectLevel(parent == null ? 1 : parent.getSubjectLevel() + 1);
        subject.setEnabled(request.enabled() == null || request.enabled());
        subject.setDescription(request.description());
    }

    /**
     * 解析并校验会计科目父级。
     *
     * <p>实现步骤：
     * 1. 父级为空时返回 null，表示一级科目；
     * 2. 修改时不允许把自己设为自己的父级；
     * 3. 父级必须存在；
     * 4. 修改时不允许选择自己的任意下级作为父级，避免形成环形科目树。</p>
     */
    private AccountingSubject resolveSubjectParent(Long currentSubjectId, Long parentId) {
        if (parentId == null) {
            return null;
        }
        if (Objects.equals(currentSubjectId, parentId)) {
            throw new BusinessException(ResponseCode.ILLEGAL_PARAM, "父级科目不能选择自身");
        }
        AccountingSubject parent = subjectRepository.findById(parentId)
                .orElseThrow(() -> new BusinessException("父级科目不存在"));
        CompanyScope.requireCurrentCompany(parent.getOrganizationCode(), "父级科目");
        if (isSubjectParentDescendant(parent, currentSubjectId)) {
            throw new BusinessException(ResponseCode.ILLEGAL_PARAM, "父级科目不能选择自身下级");
        }
        return parent;
    }

    /**
     * 校验停用父级科目时是否完成二次确认。
     *
     * <p>实现步骤：
     * 1. 仅在已启用科目被改为停用时检查；
     * 2. 遍历全部科目，判断是否存在启用状态的任意层级后代；
     * 3. 如果存在启用后代且请求未携带确认标记，则拒绝保存并提示前端弹出二次确认。</p>
     */
    private void ensureDisableWithEnabledChildrenConfirmed(AccountingSubject subject, SubjectRequest request) {
        // 变量说明：willDisable 保存当前步骤计算、查询或转换得到的中间结果。
        boolean willDisable = Boolean.FALSE.equals(request.enabled());
        if (!subject.isEnabled() || !willDisable) {
            return;
        }
        boolean hasEnabledDescendant = subjectRepository.findAll(CompanyScope.<AccountingSubject>currentCompanySpec()).stream()
                .anyMatch(candidate -> candidate.isEnabled() && isDescendantOf(candidate, subject.getId()));
        if (!hasEnabledDescendant || Boolean.TRUE.equals(request.confirmDisableWithEnabledChildren())) {
            return;
        }
        throw new BusinessException(ResponseCode.WARN,
                "该科目存在启用状态的下级科目，停用后后续页面将不显示该科目及其下级数据，请确认是否停用。");
    }

    /**
     * 判断科目是否可用于后续业务页面展示。
     *
     * <p>实现步骤：从当前科目向上追溯父级；只要自身或任一上级已停用，就判定整条链路不可展示。</p>
     */
    private boolean isSubjectVisibleForBusiness(AccountingSubject subject) {
        // 变量说明：visited 保存当前步骤计算、查询或转换得到的中间结果。
        Set<Long> visited = new HashSet<>();
        // 变量说明：cursor 保存当前步骤计算、查询或转换得到的中间结果。
        AccountingSubject cursor = subject;
        while (cursor != null) {
            if (!cursor.isEnabled()) {
                return false;
            }
            if (cursor.getId() != null && !visited.add(cursor.getId())) {
                return false;
            }
            cursor = cursor.getParent();
        }
        return true;
    }

    /**
     * 判断科目是否为叶子科目。
     *
     * <p>实现步骤：
     * 1. 按当前账套读取全部科目；
     * 2. 判断是否存在任意科目把当前科目作为父级；
     * 3. 没有下级时才允许作为凭证分录、会计平台制证等业务科目使用。</p>
     */
    private boolean isLeafSubject(AccountingSubject subject) {
        return subjectRepository.findAll(CompanyScope.<AccountingSubject>currentCompanySpec()).stream()
                .noneMatch(candidate -> candidate.getParent() != null && Objects.equals(candidate.getParent().getId(), subject.getId()));
    }

    /**
     * 判断候选科目是否属于指定祖先科目的任意层级下级。
     *
     * <p>实现步骤：从候选科目的父级开始向上追溯；遇到祖先 ID 则说明属于该祖先下级，遇到环形数据则中断。</p>
     */
    private boolean isDescendantOf(AccountingSubject candidate, Long ancestorId) {
        if (ancestorId == null || candidate.getId() == null || candidate.getId().equals(ancestorId)) {
            return false;
        }
        // 变量说明：visited 保存当前步骤计算、查询或转换得到的中间结果。
        Set<Long> visited = new HashSet<>();
        // 变量说明：cursor 保存当前步骤计算、查询或转换得到的中间结果。
        AccountingSubject cursor = candidate.getParent();
        while (cursor != null) {
            if (cursor.getId() != null && cursor.getId().equals(ancestorId)) {
                return true;
            }
            if (cursor.getId() != null && !visited.add(cursor.getId())) {
                return false;
            }
            cursor = cursor.getParent();
        }
        return false;
    }

    /**
     * 判断候选父级是否已经位于当前科目的下级链路中。
     *
     * <p>实现步骤：从候选父级自身开始向上追溯；如果追溯到当前科目，说明选择该父级会造成环形嵌套。</p>
     */
    private boolean isSubjectParentDescendant(AccountingSubject candidateParent, Long currentSubjectId) {
        if (currentSubjectId == null || candidateParent == null) {
            return false;
        }
        // 变量说明：visited 保存当前步骤计算、查询或转换得到的中间结果。
        Set<Long> visited = new HashSet<>();
        // 变量说明：cursor 保存当前步骤计算、查询或转换得到的中间结果。
        AccountingSubject cursor = candidateParent;
        while (cursor != null) {
            if (Objects.equals(cursor.getId(), currentSubjectId)) {
                return true;
            }
            if (cursor.getId() != null && !visited.add(cursor.getId())) {
                return false;
            }
            cursor = cursor.getParent();
        }
        return false;
    }

    /**
     * 替换凭证分录并执行复式记账校验。
     *
     * <p>实现步骤：
     * 1. 清空旧分录；
     * 2. 逐行校验科目存在且启用；
     * 3. 按明细行解析币种和汇率快照；
     * 4. 校验单行不能同时有借方和贷方；
     * 5. 汇总原币金额和折人民币金额；
     * 6. 校验整张凭证折人民币金额借贷平衡；
     * 7. 回写凭证主表合计和币种摘要。</p>
     */
    private void replaceVoucherLines(Voucher voucher, VoucherRequest request) {
        // 步骤1：使用 orphanRemoval 删除数据库中的旧分录，保证请求分录就是最终分录。
        voucher.getLines().clear();
        // 变量说明：lineRequests 保存当前步骤计算、查询或转换得到的中间结果。
        List<VoucherLineRequest> lineRequests = request.lines();
        // 变量说明：totalDebit 保存当前步骤计算、查询或转换得到的中间结果。
        BigDecimal totalDebit = BigDecimal.ZERO;
        // 变量说明：totalCredit 保存当前步骤计算、查询或转换得到的中间结果。
        BigDecimal totalCredit = BigDecimal.ZERO;
        // 变量说明：totalDebitCny 保存当前步骤计算、查询或转换得到的中间结果。
        BigDecimal totalDebitCny = BigDecimal.ZERO;
        // 变量说明：totalCreditCny 保存当前步骤计算、查询或转换得到的中间结果。
        BigDecimal totalCreditCny = BigDecimal.ZERO;
        // 变量说明：firstCurrency 保存当前步骤计算、查询或转换得到的中间结果。
        CurrencySnapshot firstCurrency = null;
        // 变量说明：multiCurrency 保存当前步骤计算、查询或转换得到的中间结果。
        boolean multiCurrency = false;
        // 变量说明：lineNo 保存当前步骤计算、查询或转换得到的中间结果。
        int lineNo = 1;
        for (VoucherLineRequest lineRequest : lineRequests) {
            // 步骤2：停用科目不能继续记新账。
            AccountingSubject subject = subjectRepository.findById(lineRequest.subjectId())
                    .filter(item -> isSubjectVisibleForBusiness(item) && isLeafSubject(item))
                    .orElseThrow(() -> new BusinessException("科目不存在或已停用: " + lineRequest.subjectId()));
            CompanyScope.requireCurrentCompany(subject.getOrganizationCode(), "科目");
            // 步骤3：每条分录独立解析币种和汇率，支持同一凭证中不同明细使用不同币种。
            CurrencySnapshot lineCurrency = resolveLineCurrency(
                    lineRequest.currencyCode(),
                    lineRequest.currencyName(),
                    lineRequest.exchangeRateToCny(),
                    request.currencyCode(),
                    request.currencyName(),
                    request.exchangeRateToCny()
            );
            if (firstCurrency == null) {
                firstCurrency = lineCurrency;
            } else if (isDifferentCurrencySnapshot(firstCurrency, lineCurrency)) {
                multiCurrency = true;
            }
            // 步骤4：所有凭证金额统一保留 8 位小数，满足多币种和高精度核算需要。
            BigDecimal debit = money(lineRequest.debitAmount());
            // 变量说明：credit 保存当前步骤计算、查询或转换得到的中间结果。
            BigDecimal credit = money(lineRequest.creditAmount());
            // 变量说明：debitCny 保存当前步骤计算、查询或转换得到的中间结果。
            BigDecimal debitCny = currencyService.toCnyAmount(debit, lineCurrency);
            // 变量说明：creditCny 保存当前步骤计算、查询或转换得到的中间结果。
            BigDecimal creditCny = currencyService.toCnyAmount(credit, lineCurrency);
            // 步骤5：复式记账要求一条分录只能落在借方或贷方其中一侧。
            if (debit.signum() > 0 && credit.signum() > 0) {
                throw new BusinessException("同一分录不能同时有借方和贷方金额");
            }
            if (debit.signum() == 0 && credit.signum() == 0) {
                throw new BusinessException("分录借方或贷方金额至少填写一项");
            }
            // 步骤6：创建新分录并累加借贷合计。
            VoucherLine line = new VoucherLine();
            line.setVoucher(voucher);
            line.setLineNo(lineNo++);
            line.setSubject(subject);
            line.setSummary(lineRequest.summary());
            line.setDebitAmount(debit);
            line.setCreditAmount(credit);
            line.setCurrencyCode(lineCurrency.currencyCode());
            line.setCurrencyName(lineCurrency.currencyName());
            line.setExchangeRateToCny(lineCurrency.exchangeRateToCny());
            line.setDebitAmountCny(debitCny);
            line.setCreditAmountCny(creditCny);
            line.setAuxiliary(lineRequest.auxiliary());
            voucher.getLines().add(line);
            totalDebit = totalDebit.add(debit);
            totalCredit = totalCredit.add(credit);
            totalDebitCny = totalDebitCny.add(debitCny);
            totalCreditCny = totalCreditCny.add(creditCny);
        }
        // 步骤7：多币种场景以折人民币金额校验复式记账平衡，避免不同币种原币金额被直接相加。
        if (totalDebitCny.compareTo(totalCreditCny) != 0) {
            throw new BusinessException("凭证借贷人民币金额不平衡");
        }
        applyVoucherCurrencySummary(voucher, firstCurrency, multiCurrency);
        // 步骤8：主表保存合计金额，减少列表和报表读取时的重复计算。
        voucher.setTotalDebit(totalDebit);
        voucher.setTotalCredit(totalCredit);
        voucher.setTotalDebitCny(totalDebitCny);
        voucher.setTotalCreditCny(totalCreditCny);
    }

    /**
     * 解析分录币种快照。
     *
     * <p>实现步骤：优先使用分录自己的币种和汇率；如果历史调用没有传分录币种，则回退到凭证主表币种。</p>
     */
    private CurrencySnapshot resolveLineCurrency(
            String lineCurrencyCode,
            String lineCurrencyName,
            BigDecimal lineExchangeRate,
            String fallbackCurrencyCode,
            String fallbackCurrencyName,
            BigDecimal fallbackExchangeRate
    ) {
        // 变量说明：currencyCode 保存当前步骤计算、查询或转换得到的中间结果。
        String currencyCode = firstText(lineCurrencyCode, fallbackCurrencyCode);
        // 变量说明：currencyName 保存当前步骤计算、查询或转换得到的中间结果。
        String currencyName = firstText(lineCurrencyName, fallbackCurrencyName);
        // 变量说明：exchangeRate 保存当前步骤计算、查询或转换得到的中间结果。
        BigDecimal exchangeRate = lineExchangeRate == null ? fallbackExchangeRate : lineExchangeRate;
        return currencyService.snapshot(currencyCode, currencyName, exchangeRate);
    }

    /**
     * 将分录币种摘要写入凭证主表。
     *
     * <p>实现步骤：单币种凭证保存该币种快照；多币种凭证保存 MULTI/多币种，列表直接提示用户该凭证存在多币种明细。</p>
     */
    private void applyVoucherCurrencySummary(Voucher voucher, CurrencySnapshot firstCurrency, boolean multiCurrency) {
        if (firstCurrency == null) {
            // 变量说明：defaultCurrency 保存当前步骤计算、查询或转换得到的中间结果。
            CurrencySnapshot defaultCurrency = currencyService.snapshot(null, null, null);
            voucher.setCurrencyCode(defaultCurrency.currencyCode());
            voucher.setCurrencyName(defaultCurrency.currencyName());
            voucher.setExchangeRateToCny(defaultCurrency.exchangeRateToCny());
            return;
        }
        if (multiCurrency) {
            voucher.setCurrencyCode("MULTI");
            voucher.setCurrencyName("多币种/多汇率");
            voucher.setExchangeRateToCny(BigDecimal.ONE.setScale(CurrencyService.MONEY_SCALE, RoundingMode.HALF_UP));
            return;
        }
        voucher.setCurrencyCode(firstCurrency.currencyCode());
        voucher.setCurrencyName(firstCurrency.currencyName());
        voucher.setExchangeRateToCny(firstCurrency.exchangeRateToCny());
    }

    /**
     * 判断两个币种快照是否存在币种或汇率差异。
     *
     * <p>实现目的：同一凭证可能使用同一外币但手工录入不同汇率，此时主表不应展示成单一汇率。</p>
     */
    private boolean isDifferentCurrencySnapshot(CurrencySnapshot left, CurrencySnapshot right) {
        return !Objects.equals(left.currencyCode(), right.currencyCode())
                || left.exchangeRateToCny().compareTo(right.exchangeRateToCny()) != 0;
    }

    /**
     * 解析凭证所属年月。
     *
     * <p>实现步骤：优先使用前端传入的 yyyy-MM；为空时按凭证日期截取年月，保证新增凭证始终有可用于月度统计的归属月份。</p>
     */
    private String resolveBelongMonth(String belongMonth, LocalDate voucherDate) {
        // 变量说明：normalized 保存当前步骤计算、查询或转换得到的中间结果。
        String normalized = firstText(belongMonth, null);
        if (normalized != null) {
            return normalized;
        }
        return voucherDate.format(DateTimeFormatter.ofPattern("yyyy-MM"));
    }

    /**
     * 按日期生成凭证号。
     *
     * <p>格式为 VyyyyMMdd0001，若当天已有同号则递增序号。</p>
     */
    private String nextVoucherNo(LocalDate date) {
        // 变量说明：prefix 保存当前步骤计算、查询或转换得到的中间结果。
        String prefix = "V" + date.format(DateTimeFormatter.BASIC_ISO_DATE);
        String voucherNo;
        // 变量说明：sequence 保存当前步骤计算、查询或转换得到的中间结果。
        int sequence = 1;
        do {
            voucherNo = prefix + String.format("%04d", sequence++);
        } while (voucherRepository.existsByOrganizationCodeAndVoucherNo(CompanyScope.currentCompanyCode(), voucherNo));
        return voucherNo;
    }

    /**
     * 统一金额精度。
     *
     * <p>空金额按 0 处理，金额保留 8 位小数。</p>
     */
    private BigDecimal money(BigDecimal value) {
        return (value == null ? BigDecimal.ZERO : value).setScale(CurrencyService.MONEY_SCALE, RoundingMode.HALF_UP);
    }

    /**
     * 规范化来源单据金额。
     *
     * <p>实现步骤：空金额按 0 处理，统一保留系统金额精度，保证前端展示和凭证分录金额一致。</p>
     */
    private BigDecimal normalizeMoney(BigDecimal value) {
        return money(value);
    }

    /**
     * 规范化来源单据汇率。
     *
     * <p>实现步骤：空汇率按人民币 1 处理，非空汇率统一保留 8 位小数。</p>
     */
    private BigDecimal normalizeRate(BigDecimal value) {
        return (value == null ? BigDecimal.ONE : value).setScale(CurrencyService.MONEY_SCALE, RoundingMode.HALF_UP);
    }

    /**
     * 采购状态中文文本。
     *
     * <p>实现步骤：将后端枚举转换为业务人员可读的中文状态，用于会计平台来源列表展示。</p>
     */
    private String purchaseStatusText(com.ratel.fm.domain.purchase.PurchaseStatus status) {
        if (status == null) {
            return "";
        }
        return switch (status) {
            case DRAFT -> "草稿";
            case IN_APPROVAL -> "审批中";
            case APPROVAL_REJECTED -> "已审批【不同意】";
            case SUBMITTED -> "已提交";
            case APPROVED -> "已审批【同意】";
            case PURCHASING -> "采购中";
            case PURCHASE_COMPLETED -> "采购完成";
            case RECEIVED -> "已收货";
            case CLOSED -> "已关闭";
            case CANCELLED -> "取消采购";
        };
    }

    /**
     * 应收应付类型中文文本。
     *
     * <p>实现步骤：将应收、应付枚举转换为中文名称，用于摘要和来源标题。</p>
     */
    private String arApTypeText(ArApType type) {
        if (type == null) {
            return "往来";
        }
        return type == ArApType.RECEIVABLE ? "应收" : "应付";
    }

    /**
     * 应收应付状态中文文本。
     *
     * <p>实现步骤：将往来单据状态转换为业务人员可读的中文状态，用于会计平台来源列表展示。</p>
     */
    private String arApStatusText(com.ratel.fm.domain.receivable.ArApStatus status) {
        if (status == null) {
            return "";
        }
        return switch (status) {
            case OPEN -> "未结";
            case PARTIAL -> "部分结清";
            case CLOSED -> "已结清";
            case OVERDUE -> "逾期";
        };
    }

    /**
     * 库存变动类型中文文本。
     *
     * <p>实现步骤：将库存流水枚举转换为会计平台来源列表可读的中文状态。</p>
     */
    private String inventoryMovementTypeText(com.ratel.fm.domain.inventory.InventoryMovementType type) {
        if (type == null) {
            return "";
        }
        return switch (type) {
            case INBOUND -> "入库";
            case OUTBOUND -> "出库";
            case TRANSFER -> "调拨";
            case CHECK -> "盘点";
        };
    }

    /**
     * 出纳流水状态中文文本。
     *
     * <p>实现步骤：将出纳流水状态转换为会计平台来源列表可读的中文状态。</p>
     */
    private String cashierStatusText(CashierTransactionStatus status) {
        if (status == null) {
            return "";
        }
        return switch (status) {
            case DRAFT -> "草稿";
            case CONFIRMED -> "已确认";
            case VOUCHERED -> "已制证";
            case CANCELLED -> "已取消";
        };
    }

    /**
     * 判断是否存在有效选中 ID。
     */
    private boolean hasSelectedIds(List<Long> ids) {
        return ids != null && ids.stream().anyMatch(Objects::nonNull);
    }

    /**
     * 清理批量操作 ID。
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
     * 凭证状态中文化，用于 Excel 导出。
     */
    private String voucherStatusText(VoucherStatus status) {
        return switch (status) {
            case DRAFT -> "草稿";
            case POSTED -> "已过账";
            case VOIDED -> "已作废";
        };
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
     * 自动凭证来源类型中文化。
     *
     * <p>实现步骤：将来源枚举转换为业务人员可读的模块名称，供查看来源弹窗和来源字段展示。</p>
     */
    private String sourceTypeText(AccountingSourceType sourceType) {
        if (sourceType == null) {
            return "未知来源";
        }
        return switch (sourceType) {
            case PURCHASE_ORDER -> "业务管理/采购管理";
            case AR_AP_BILL -> "应收应付/应收应付单据";
            case INVENTORY_LEDGER -> "库存管理/库存台账";
            case CASHIER_TRANSACTION -> "财务管理/出纳管理";
        };
    }

    /**
     * 自动凭证来源往来对象标签。
     *
     * <p>实现步骤：按来源模块差异返回供应商、客户/供应商、物料或往来单位，保证查看来源弹窗字段含义清晰。</p>
     */
    private String sourcePartnerLabel(AccountingSourceType sourceType) {
        if (sourceType == AccountingSourceType.PURCHASE_ORDER) {
            return "供应商";
        }
        if (sourceType == AccountingSourceType.INVENTORY_LEDGER) {
            return "物料";
        }
        if (sourceType == AccountingSourceType.CASHIER_TRANSACTION) {
            return "往来单位";
        }
        return "客户/供应商";
    }

    /**
     * 组装金额展示文本。
     *
     * <p>实现步骤：金额为空时按 0 展示，币种名称为空时只展示数字，供来源详情弹窗避免空值。</p>
     */
    private String moneyText(BigDecimal amount, String currencyName) {
        BigDecimal safeAmount = amount == null ? BigDecimal.ZERO : amount;
        String suffix = firstText(currencyName, null);
        return suffix == null ? safeAmount.toPlainString() : safeAmount.toPlainString() + " " + suffix;
    }

    /**
     * 生成凭证流水标题。
     */
    private String voucherTitle(VoucherView view) {
        return view.voucherNo() + " " + view.summary();
    }

    /**
     * 返回第一个非空白文本。
     *
     * <p>实现目的：搜索等值条件和所属年月兜底处理都需要把空字符串视为未传，避免生成无意义的精确匹配条件。</p>
     */
    private String firstText(String value, String fallback) {
        if (value != null && !value.isBlank()) {
            return value.trim();
        }
        return fallback;
    }

    /**
     * 读取分录借方人民币金额，兼容历史数据没有人民币快照的情况。
     */
    private BigDecimal cnyDebit(VoucherLine line) {
        return line.getDebitAmountCny() == null ? money(line.getDebitAmount()) : line.getDebitAmountCny();
    }

    /**
     * 读取分录贷方人民币金额，兼容历史数据没有人民币快照的情况。
     */
    private BigDecimal cnyCredit(VoucherLine line) {
        return line.getCreditAmountCny() == null ? money(line.getCreditAmount()) : line.getCreditAmountCny();
    }

    /**
     * 执行 toSubjectView 方法。
     * 
     * <p>实现步骤：
     * 1. 接收并校验调用方传入的数据；
     * 2. 按当前方法职责执行业务查询、转换或持久化处理；
     * 3. 返回处理结果或更新对象状态。</p>
     */
    private SubjectView toSubjectView(AccountingSubject subject) {
        // 变量说明：parent 保存当前步骤计算、查询或转换得到的中间结果。
        AccountingSubject parent = subject.getParent();
        return new SubjectView(
                subject.getId(),
                subject.getCode(),
                subject.getName(),
                subject.getCategory(),
                parent == null ? null : parent.getId(),
                parent == null ? null : parent.getName(),
                subject.getSubjectLevel(),
                subject.isEnabled(),
                subject.getDescription()
        );
    }

    /**
     * 执行 toVoucherView 方法。
     * 
     * <p>实现步骤：
     * 1. 接收并校验调用方传入的数据；
     * 2. 按当前方法职责执行业务查询、转换或持久化处理；
     * 3. 返回处理结果或更新对象状态。</p>
     */
    private VoucherView toVoucherView(Voucher voucher) {
        return new VoucherView(
                voucher.getId(),
                voucher.getVoucherNo(),
                voucher.getVoucherDate(),
                defaultString(voucher.getBelongMonth(), voucher.getVoucherDate().format(DateTimeFormatter.ofPattern("yyyy-MM"))),
                voucher.getProjectCode(),
                voucher.getProjectName(),
                voucher.getSummary(),
                voucher.getStatus(),
                voucher.getTotalDebit(),
                voucher.getTotalCredit(),
                defaultString(voucher.getCurrencyCode(), CurrencyService.DEFAULT_CURRENCY_CODE),
                defaultString(voucher.getCurrencyName(), CurrencyService.DEFAULT_CURRENCY_NAME),
                defaultRate(voucher.getExchangeRateToCny()),
                defaultMoney(voucher.getTotalDebitCny(), voucher.getTotalDebit()),
                defaultMoney(voucher.getTotalCreditCny(), voucher.getTotalCredit()),
                voucher.getCreatedBy(),
                voucher.getPostedBy(),
                voucher.getSourceBizNo(),
                voucher.getSourceType(),
                voucher.getSourceId(),
                voucher.getSourceTitle(),
                voucher.getLines().stream().map(this::toVoucherLineView).toList()
        );
    }

    /**
     * 执行 toVoucherLineView 方法。
     * 
     * <p>实现步骤：
     * 1. 接收并校验调用方传入的数据；
     * 2. 按当前方法职责执行业务查询、转换或持久化处理；
     * 3. 返回处理结果或更新对象状态。</p>
     */
    private VoucherLineView toVoucherLineView(VoucherLine line) {
        return new VoucherLineView(
                line.getId(),
                line.getLineNo(),
                line.getSubject().getId(),
                line.getSubject().getCode(),
                line.getSubject().getName(),
                subjectFullName(line.getSubject()),
                line.getSummary(),
                line.getDebitAmount(),
                line.getCreditAmount(),
                defaultString(line.getCurrencyCode(), CurrencyService.DEFAULT_CURRENCY_CODE),
                defaultString(line.getCurrencyName(), CurrencyService.DEFAULT_CURRENCY_NAME),
                defaultRate(line.getExchangeRateToCny()),
                cnyDebit(line),
                cnyCredit(line),
                line.getAuxiliary()
        );
    }

    /**
     * 生成会计科目完整级联名称。
     *
     * <p>实现步骤：
     * 1. 从当前末级科目开始向上追溯父级科目；
     * 2. 每次把科目名称加入路径集合，并用保护计数避免异常环形父级造成死循环；
     * 3. 反转后用“ / ”拼接，供在线凭证显示完整科目层级。</p>
     */
    private String subjectFullName(AccountingSubject subject) {
        // 步骤1：空科目通常只会出现在历史异常数据中，返回空字符串避免凭证渲染失败。
        if (subject == null) {
            return "";
        }
        // 变量说明：names 保存从末级向一级追溯到的科目名称。
        List<String> names = new ArrayList<>();
        // 变量说明：cursor 指向当前正在处理的科目节点。
        AccountingSubject cursor = subject;
        // 变量说明：guard 限制最多追溯 20 层，避免异常数据产生无限循环。
        int guard = 0;
        while (cursor != null && guard < 20) {
            names.add(cursor.getName());
            cursor = cursor.getParent();
            guard++;
        }
        // 步骤3：反转后得到一级到末级的标准展示顺序。
        Collections.reverse(names);
        return String.join(" / ", names);
    }

    /**
     * 空字符串时返回默认文本。
     */
    private String defaultString(String value, String defaultValue) {
        return value == null || value.isBlank() ? defaultValue : value;
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

    /**
     * TrialAccumulator 类。
     * 
     * <p>用于承载 TrialAccumulator 相关的业务数据、接口契约或处理逻辑，便于维护人员快速定位模块职责。</p>
     */
    private static final class TrialAccumulator {
        /**
         * 字段 subject：保存 subject 对应的业务数据、运行配置或依赖对象，供本类逻辑读取和维护。
         */
        private final AccountingSubject subject;
        /**
         * 字段 debit：保存 debit 对应的业务数据、运行配置或依赖对象，供本类逻辑读取和维护。
         */
        private BigDecimal debit = BigDecimal.ZERO;
        /**
         * 字段 credit：保存 credit 对应的业务数据、运行配置或依赖对象，供本类逻辑读取和维护。
         */
        private BigDecimal credit = BigDecimal.ZERO;

        private TrialAccumulator(AccountingSubject subject) {
            this.subject = subject;
        }

        /**
         * 执行 add 方法。
         * 
         * <p>实现步骤：
         * 1. 接收并校验调用方传入的数据；
         * 2. 按当前方法职责执行业务查询、转换或持久化处理；
         * 3. 返回处理结果或更新对象状态。</p>
         */
        private void add(BigDecimal debitAmount, BigDecimal creditAmount) {
            debit = debit.add(debitAmount);
            credit = credit.add(creditAmount);
        }

        /**
         * 执行 toRow 方法。
         * 
         * <p>实现步骤：
         * 1. 接收并校验调用方传入的数据；
         * 2. 按当前方法职责执行业务查询、转换或持久化处理；
         * 3. 返回处理结果或更新对象状态。</p>
         */
        private TrialBalanceRow toRow() {
            return new TrialBalanceRow(subject.getId(), subject.getCode(), subject.getName(),
                    debit, credit, debit.subtract(credit));
        }
    }
}
