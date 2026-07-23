package com.ratel.fm.service.report;

import com.ratel.fm.domain.finance.SubjectCategory;
import com.ratel.fm.domain.finance.VoucherStatus;
import com.ratel.fm.repository.finance.VoucherRepository;
import com.ratel.fm.security.CompanyScope;
import com.ratel.fm.web.dto.phasetwo.PhaseTwoDtos.FinancialStatement;
import com.ratel.fm.web.dto.phasetwo.PhaseTwoDtos.StatementLine;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

/**
 * 财务三大报表服务。
 *
 * <p>当前为基础版，按已过账凭证的科目类别汇总。后续可引入报表项目映射、现金流量项目和期间结账。</p>
 *
 * <p>开发组织：ratel；开发人员：WenZhang；联系方式：18782945613。</p>
 *
 * @author ratel
 */
@Service
public class FinancialStatementService {

    /**
     * 字段 voucherRepository：保存 voucherRepository 对应的业务数据、运行配置或依赖对象，供本类逻辑读取和维护。
     */
    private final VoucherRepository voucherRepository;

    /**
     * 构造 FinancialStatementService 实例。
     * 
     * <p>实现步骤：
     * 1. 接收调用方传入的依赖对象或初始化参数；
     * 2. 保存到成员字段，保证后续业务方法可以复用；
     * 3. 完成实例初始化。</p>
     */
    public FinancialStatementService(VoucherRepository voucherRepository) {
        this.voucherRepository = voucherRepository;
    }

    /**
     * 生成资产负债表基础版。
     *
     * <p>实现步骤：
     * 1. 汇总指定日期之前已过账凭证；
     * 2. 按科目类别取资产、负债、权益金额；
     * 3. 组装为报表行返回。</p>
     */
    @Transactional(readOnly = true)
    public FinancialStatement balanceSheet(LocalDate date) {
        // 步骤1：totals 只统计 POSTED 凭证，草稿和作废不进入报表。
        Map<SubjectCategory, BigDecimal> totals = totals(date);
        return new FinancialStatement("资产负债表", date, List.of(
                new StatementLine("资产", totals.getOrDefault(SubjectCategory.ASSET, BigDecimal.ZERO)),
                new StatementLine("负债", totals.getOrDefault(SubjectCategory.LIABILITY, BigDecimal.ZERO)),
                new StatementLine("所有者权益", totals.getOrDefault(SubjectCategory.EQUITY, BigDecimal.ZERO))
        ));
    }

    /**
     * 生成利润表基础版。
     *
     * <p>实现步骤：
     * 1. 汇总指定日期之前已过账凭证；
     * 2. 读取收入、成本、费用类别金额；
     * 3. 计算利润总额 = 收入 - 成本 - 费用。</p>
     */
    @Transactional(readOnly = true)
    public FinancialStatement incomeStatement(LocalDate date) {
        // 变量说明：totals 保存当前步骤计算、查询或转换得到的中间结果。
        Map<SubjectCategory, BigDecimal> totals = totals(date);
        // 变量说明：revenue 保存当前步骤计算、查询或转换得到的中间结果。
        BigDecimal revenue = totals.getOrDefault(SubjectCategory.REVENUE, BigDecimal.ZERO).abs();
        // 变量说明：cost 保存当前步骤计算、查询或转换得到的中间结果。
        BigDecimal cost = totals.getOrDefault(SubjectCategory.COST, BigDecimal.ZERO).abs();
        // 变量说明：expense 保存当前步骤计算、查询或转换得到的中间结果。
        BigDecimal expense = totals.getOrDefault(SubjectCategory.EXPENSE, BigDecimal.ZERO).abs();
        return new FinancialStatement("利润表", date, List.of(
                new StatementLine("营业收入", revenue),
                new StatementLine("营业成本", cost),
                new StatementLine("期间费用", expense),
                new StatementLine("利润总额", revenue.subtract(cost).subtract(expense))
        ));
    }

    /**
     * 生成现金流量表基础版。
     *
     * <p>当前尚未建立现金流量项目映射，暂以资产类净额作为经营活动现金流量基础展示。</p>
     */
    @Transactional(readOnly = true)
    public FinancialStatement cashFlowStatement(LocalDate date) {
        // 变量说明：totals 保存当前步骤计算、查询或转换得到的中间结果。
        Map<SubjectCategory, BigDecimal> totals = totals(date);
        // 变量说明：cash 保存当前步骤计算、查询或转换得到的中间结果。
        BigDecimal cash = totals.getOrDefault(SubjectCategory.ASSET, BigDecimal.ZERO);
        return new FinancialStatement("现金流量表", date, List.of(
                new StatementLine("经营活动现金流量净额", cash),
                new StatementLine("投资活动现金流量净额", BigDecimal.ZERO),
                new StatementLine("筹资活动现金流量净额", BigDecimal.ZERO),
                new StatementLine("现金及现金等价物净增加额", cash)
        ));
    }

    /**
     * 按科目类别汇总已过账凭证金额。
     *
     * <p>实现步骤：
     * 1. 未传日期时默认截至今天；
     * 2. 查询 1970-01-01 到截止日的凭证；
     * 3. 只统计 POSTED 凭证；
     * 4. 按科目类别合并借方减贷方的净额。</p>
     */
    private Map<SubjectCategory, BigDecimal> totals(LocalDate date) {
        // 变量说明：end 保存当前步骤计算、查询或转换得到的中间结果。
        LocalDate end = date == null ? LocalDate.now() : date;
        // 变量说明：totals 保存当前步骤计算、查询或转换得到的中间结果。
        Map<SubjectCategory, BigDecimal> totals = new EnumMap<>(SubjectCategory.class);
        voucherRepository.findByOrganizationCodeAndVoucherDateBetweenOrderByVoucherDateDesc(
                        CompanyScope.currentCompanyCode(), LocalDate.of(1970, 1, 1), end).stream()
                .filter(voucher -> voucher.getStatus() == VoucherStatus.POSTED)
                .flatMap(voucher -> voucher.getLines().stream())
                .forEach(line -> totals.merge(line.getSubject().getCategory(),
                        cnyDebit(line).subtract(cnyCredit(line)), BigDecimal::add));
        return totals;
    }

    /**
     * 读取借方人民币金额，历史数据没有快照时回退到原币金额。
     */
    private BigDecimal cnyDebit(com.ratel.fm.domain.finance.VoucherLine line) {
        return line.getDebitAmountCny() == null ? line.getDebitAmount() : line.getDebitAmountCny();
    }

    /**
     * 读取贷方人民币金额，历史数据没有快照时回退到原币金额。
     */
    private BigDecimal cnyCredit(com.ratel.fm.domain.finance.VoucherLine line) {
        return line.getCreditAmountCny() == null ? line.getCreditAmount() : line.getCreditAmountCny();
    }
}
