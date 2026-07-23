package com.ratel.fm.service.period;

import com.ratel.fm.common.BusinessException;
import com.ratel.fm.common.SearchSpecs;
import com.ratel.fm.domain.finance.Voucher;
import com.ratel.fm.domain.finance.VoucherStatus;
import com.ratel.fm.domain.period.AccountingPeriod;
import com.ratel.fm.domain.period.AccountingPeriodStatus;
import com.ratel.fm.domain.receivable.ArApBill;
import com.ratel.fm.domain.receivable.ArApStatus;
import com.ratel.fm.repository.finance.VoucherRepository;
import com.ratel.fm.repository.period.AccountingPeriodRepository;
import com.ratel.fm.repository.receivable.ArApBillRepository;
import com.ratel.fm.security.CompanyScope;
import com.ratel.fm.security.SecurityUtils;
import com.ratel.fm.service.audit.AuditLogService;
import com.ratel.fm.web.dto.period.AccountingPeriodDtos.AccountingPeriodActionRequest;
import com.ratel.fm.web.dto.period.AccountingPeriodDtos.AccountingPeriodRequest;
import com.ratel.fm.web.dto.period.AccountingPeriodDtos.AccountingPeriodView;
import com.ratel.fm.web.dto.period.AccountingPeriodDtos.PeriodCloseCheckView;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.LinkedHashSet;
import java.util.Set;

/**
 * 会计期间服务。
 *
 * <p>实现目的：提供会计期间创建、关闭检查、结账和反结账能力，作为总账月末处理的核心控制点。</p>
 */
@Service
public class AccountingPeriodService {

    /** 会计期间仓库，用于按账套保存和查询期间状态。 */
    private final AccountingPeriodRepository periodRepository;
    /** 凭证仓库，用于月结前检查本期是否还有草稿凭证。 */
    private final VoucherRepository voucherRepository;
    /** 应收应付仓库，用于月结前提示本期到期但未结清的往来单据。 */
    private final ArApBillRepository arApBillRepository;
    /** 审计日志服务，用于记录结账和反结账等关键财务操作。 */
    private final AuditLogService auditLogService;

    /**
     * 构造会计期间服务。
     *
     * <p>实现步骤：接收仓库和审计依赖并保存到成员字段，后续期间操作统一复用这些依赖。</p>
     */
    public AccountingPeriodService(
            AccountingPeriodRepository periodRepository,
            VoucherRepository voucherRepository,
            ArApBillRepository arApBillRepository,
            AuditLogService auditLogService
    ) {
        this.periodRepository = periodRepository;
        this.voucherRepository = voucherRepository;
        this.arApBillRepository = arApBillRepository;
        this.auditLogService = auditLogService;
    }

    /**
     * 查询会计期间列表。
     *
     * <p>实现步骤：
     * 1. 按当前所属公司限制查询范围；
     * 2. 可按期间编码和状态筛选；
     * 3. 按期间倒序返回最近 100 条，方便财务人员优先处理最近月份。</p>
     */
    @Transactional(readOnly = true)
    public List<AccountingPeriodView> list(String periodCode, AccountingPeriodStatus status) {
        /**
         * 会计期间列表查询条件，限定当前账套并按期间编码和状态筛选。
         */
        var spec = CompanyScope.<AccountingPeriod>currentCompanySpec()
                .and(SearchSpecs.like("periodCode", periodCode))
                .and(SearchSpecs.equal("status", status));
        return periodRepository.findAll(spec, PageRequest.of(0, 100, Sort.by(Sort.Direction.DESC, "periodCode", "id"))).stream()
                .map(this::toView)
                .toList();
    }

    /**
     * 创建或返回已存在的会计期间。
     *
     * <p>实现步骤：
     * 1. 校验 periodCode 格式并解析自然月开始和结束日期；
     * 2. 如果当前账套期间已存在，返回已有期间，避免重复创建；
     * 3. 不存在时按开启状态创建新期间。</p>
     */
    @Transactional
    public AccountingPeriodView create(AccountingPeriodRequest request) {
        String organizationCode = CompanyScope.currentCompanyCode();
        AccountingPeriod existing = periodRepository.findByOrganizationCodeAndPeriodCode(organizationCode, request.periodCode())
                .orElse(null);
        if (existing != null) {
            return toView(existing);
        }
        YearMonth yearMonth = YearMonth.parse(request.periodCode());
        AccountingPeriod period = new AccountingPeriod();
        period.setOrganizationCode(organizationCode);
        period.setPeriodCode(request.periodCode());
        period.setStartDate(yearMonth.atDay(1));
        period.setEndDate(yearMonth.atEndOfMonth());
        period.setStatus(AccountingPeriodStatus.OPEN);
        period.setRemark(request.remark());
        AccountingPeriodView view = toView(periodRepository.save(period));
        auditLogService.finance("CREATE_ACCOUNTING_PERIOD", request, "SUCCESS",
                "创建会计期间" + view.periodCode() + "。");
        return view;
    }

    /**
     * 为一批所属公司自动补齐缺失的会计期间。
     *
     * <p>实现步骤：
     * 1. 规范化调用方传入的公司编码并去重，同时兜底加入系统默认公司；
     * 2. 逐个公司检查最新期间，已有期间从最新期间下月补到当前月；
     * 3. 没有任何期间的公司从当前年度 1 月补到当前月；
     * 4. 返回本次实际新增的期间数量，供启动任务和定时任务记录执行结果。</p>
     */
    @Transactional
    public int ensurePeriodsForCompanies(Collection<String> organizationCodes) {
        // 步骤1：构造稳定顺序的公司编码集合，避免重复字典节点导致重复检查。
        Set<String> normalizedCodes = new LinkedHashSet<>();
        if (organizationCodes != null) {
            organizationCodes.stream()
                    .map(CompanyScope::normalizeCompanyCode)
                    .forEach(normalizedCodes::add);
        }
        normalizedCodes.add(CompanyScope.DEFAULT_COMPANY_CODE);

        // 步骤2：按公司逐个补齐期间；单个事务保证本次任务的新增期间一起提交。
        int createdCount = 0;
        for (String organizationCode : normalizedCodes) {
            createdCount += ensurePeriodsForCompany(organizationCode, YearMonth.now());
        }
        return createdCount;
    }

    /**
     * 为单个所属公司补齐当前月以前缺失的会计期间。
     *
     * <p>实现步骤：
     * 1. 查询公司内最新会计期间；
     * 2. 已有期间从最新期间的下一个月开始补齐，没有期间则从当前年度 1 月开始；
     * 3. 循环创建到当前月份为止，创建前再次按公司和期间编码检查存在性；
     * 4. 每个自动生成期间默认开启，备注标识为系统自动生成。</p>
     */
    private int ensurePeriodsForCompany(String organizationCode, YearMonth currentMonth) {
        // 步骤1：最新期间决定补齐起点；若没有初始化过，则从当前年度一月开始建立年度期间。
        YearMonth startMonth = periodRepository.findFirstByOrganizationCodeOrderByPeriodCodeDesc(organizationCode)
                .map(AccountingPeriod::getPeriodCode)
                .map(YearMonth::parse)
                .map(month -> month.plusMonths(1))
                .orElse(YearMonth.of(currentMonth.getYear(), 1));
        if (startMonth.isAfter(currentMonth)) {
            return 0;
        }

        // 步骤2：按月补齐缺失期间，存在性检查用于处理手工补录或重复触发的幂等场景。
        int createdCount = 0;
        for (YearMonth month = startMonth; !month.isAfter(currentMonth); month = month.plusMonths(1)) {
            String periodCode = month.toString();
            if (periodRepository.existsByOrganizationCodeAndPeriodCode(organizationCode, periodCode)) {
                continue;
            }
            periodRepository.save(buildAutoPeriod(organizationCode, month));
            createdCount++;
        }
        return createdCount;
    }

    /**
     * 构造系统自动生成的开启期间实体。
     *
     * <p>实现步骤：根据自然月填充开始日期、结束日期和期间编码，状态固定为开启，备注写入来源说明。</p>
     */
    private AccountingPeriod buildAutoPeriod(String organizationCode, YearMonth month) {
        AccountingPeriod period = new AccountingPeriod();
        period.setOrganizationCode(organizationCode);
        period.setPeriodCode(month.toString());
        period.setStartDate(month.atDay(1));
        period.setEndDate(month.atEndOfMonth());
        period.setStatus(AccountingPeriodStatus.OPEN);
        period.setRemark("系统自动生成会计期间");
        return period;
    }

    /**
     * 检查指定期间是否可以结账。
     *
     * <p>实现步骤：
     * 1. 读取当前账套期间，不存在时提示先创建期间；
     * 2. 检查本期草稿凭证，草稿凭证属于阻断项；
     * 3. 检查本期到期但未结清的应收应付，作为风险提示项；
     * 4. 返回阻断项和提示项，前端按结果决定是否允许结账。</p>
     */
    @Transactional(readOnly = true)
    public PeriodCloseCheckView closeCheck(String periodCode) {
        AccountingPeriod period = periodRepository.findByOrganizationCodeAndPeriodCode(CompanyScope.currentCompanyCode(), periodCode)
                .orElseThrow(() -> new BusinessException(HttpStatus.NOT_FOUND, "会计期间不存在，请先创建期间"));
        List<String> blockingItems = new ArrayList<>();
        List<String> warningItems = new ArrayList<>();
        /**
         * 当前会计期间内的草稿凭证统计条件，存在结果时阻断结账。
         */
        var draftVoucherSpec = CompanyScope.<Voucher>currentCompanySpec()
                .and(SearchSpecs.equal("belongMonth", period.getPeriodCode()))
                .and(SearchSpecs.equal("status", VoucherStatus.DRAFT));
        long draftVoucherCount = voucherRepository.count(draftVoucherSpec);
        if (draftVoucherCount > 0) {
            blockingItems.add("本期存在" + draftVoucherCount + "张草稿凭证，请先过账或作废。");
        }
        /**
         * 当前期间到期且未结清的应收应付统计条件，作为结账风险提示。
         */
        var openArApSpec = CompanyScope.<ArApBill>currentCompanySpec()
                .and(SearchSpecs.dateBetween("dueDate", period.getStartDate(), period.getEndDate()))
                .and((root, query, cb) -> root.get("status").in(ArApStatus.OPEN, ArApStatus.PARTIAL, ArApStatus.OVERDUE));
        long openArApCount = arApBillRepository.count(openArApSpec);
        if (openArApCount > 0) {
            warningItems.add("本期到期但未结清的应收应付单共" + openArApCount + "张，请确认是否继续结账。");
        }
        return new PeriodCloseCheckView(period.getPeriodCode(), blockingItems.isEmpty(), blockingItems, warningItems);
    }

    /**
     * 关闭会计期间。
     *
     * <p>实现步骤：
     * 1. 先执行关闭检查，存在阻断项时拒绝结账；
     * 2. 状态从开启改为关闭，并写入关闭人和关闭时间；
     * 3. 记录财务审计日志。</p>
     */
    @Transactional
    public AccountingPeriodView close(String periodCode, AccountingPeriodActionRequest request) {
        PeriodCloseCheckView check = closeCheck(periodCode);
        if (!check.closable()) {
            throw new BusinessException(String.join("；", check.blockingItems()));
        }
        AccountingPeriod period = periodRepository.findByOrganizationCodeAndPeriodCode(CompanyScope.currentCompanyCode(), periodCode)
                .orElseThrow(() -> new BusinessException(HttpStatus.NOT_FOUND, "会计期间不存在"));
        if (period.getStatus() == AccountingPeriodStatus.CLOSED) {
            return toView(period);
        }
        period.setStatus(AccountingPeriodStatus.CLOSED);
        period.setClosedBy(SecurityUtils.currentUser().username());
        period.setClosedTime(OffsetDateTime.now());
        period.setRemark(request == null ? period.getRemark() : request.remark());
        AccountingPeriodView view = toView(period);
        auditLogService.finance("CLOSE_ACCOUNTING_PERIOD", "periodCode=" + periodCode, "SUCCESS",
                "关闭会计期间" + periodCode + "。");
        return view;
    }

    /**
     * 反结账并重新打开会计期间。
     *
     * <p>实现步骤：读取当前账套期间，将状态改为开启并清空关闭信息，保留备注用于记录反结账原因。</p>
     */
    @Transactional
    public AccountingPeriodView reopen(String periodCode, AccountingPeriodActionRequest request) {
        AccountingPeriod period = periodRepository.findByOrganizationCodeAndPeriodCode(CompanyScope.currentCompanyCode(), periodCode)
                .orElseThrow(() -> new BusinessException(HttpStatus.NOT_FOUND, "会计期间不存在"));
        period.setStatus(AccountingPeriodStatus.OPEN);
        period.setClosedBy(null);
        period.setClosedTime(null);
        period.setRemark(request == null ? period.getRemark() : request.remark());
        AccountingPeriodView view = toView(period);
        auditLogService.finance("REOPEN_ACCOUNTING_PERIOD", "periodCode=" + periodCode, "SUCCESS",
                "反结账并打开会计期间" + periodCode + "。");
        return view;
    }

    /**
     * 转换会计期间视图。
     *
     * <p>实现步骤：读取实体字段并组装为前端列表需要的只读结构。</p>
     */
    private AccountingPeriodView toView(AccountingPeriod period) {
        return new AccountingPeriodView(
                period.getId(),
                period.getOrganizationCode(),
                period.getPeriodCode(),
                period.getStartDate(),
                period.getEndDate(),
                period.getStatus(),
                period.getClosedBy(),
                period.getClosedTime(),
                period.getRemark()
        );
    }
}
