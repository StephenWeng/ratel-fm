package com.ratel.fm.domain.cashier;

import com.ratel.fm.common.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import org.hibernate.annotations.Comment;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;

/**
 * 出纳资金流水。
 *
 * <p>实现目的：记录银行、现金收付和转账事实，形成类似用友/金蝶出纳管理的资金台账，
 * 并作为会计平台自动生成凭证的业务来源。</p>
 */
@Entity
@Table(name = "fm_cashier_transactions")
@Comment("出纳资金流水表，记录收款、付款、转账和退款等资金事实")
public class CashierTransaction extends BaseEntity {

    /** 所属公司字典编码，即账套编码，出纳流水按该字段隔离。 */
    @Column(nullable = false, length = 80)
    @Comment("所属公司字典编码，作为出纳流水账套隔离标识")
    private String organizationCode;

    /** 出纳流水号，当前所属公司内唯一。 */
    @Column(nullable = false, length = 60)
    @Comment("出纳流水号")
    private String transactionNo;

    /** 出纳交易日期。 */
    @Column(nullable = false)
    @Comment("出纳交易日期")
    private LocalDate transactionDate;

    /** 出纳流水类型。 */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    @Comment("出纳流水类型")
    private CashierTransactionType transactionType;

    /** 出纳流水状态。 */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    @Comment("出纳流水状态")
    private CashierTransactionStatus status = CashierTransactionStatus.DRAFT;

    /** 项目字典编码，用于项目资金统计。 */
    @Column(length = 80)
    @Comment("项目字典编码")
    private String projectCode;

    /** 项目名称快照。 */
    @Column(length = 160)
    @Comment("项目名称快照")
    private String projectName;

    /** 往来单位名称，收款时通常为客户，付款时通常为供应商。 */
    @Column(length = 160)
    @Comment("往来单位名称")
    private String partnerName;

    /** 银行或现金账户，来自基础信息银行账户字典。 */
    @Column(length = 160)
    @Comment("银行或现金账户")
    private String bankAccount;

    /** 结算方式，如银行转账、现金、承兑等。 */
    @Column(length = 120)
    @Comment("结算方式")
    private String settlementMethod;

    /** 原币金额。 */
    @Column(nullable = false, precision = 26, scale = 8)
    @Comment("原币金额")
    private BigDecimal amount = BigDecimal.ZERO;

    /** 币种编码，默认人民币 CNY。 */
    @Column(length = 20)
    @Comment("币种编码")
    private String currencyCode = "CNY";

    /** 币种名称快照，默认人民币。 */
    @Column(length = 80)
    @Comment("币种名称快照")
    private String currencyName = "人民币";

    /** 发生时折人民币汇率。 */
    @Column(precision = 18, scale = 8)
    @Comment("发生时折人民币汇率")
    private BigDecimal exchangeRateToCny = BigDecimal.ONE;

    /** 折人民币金额。 */
    @Column(precision = 26, scale = 8)
    @Comment("折人民币金额")
    private BigDecimal amountCny = BigDecimal.ZERO;

    /** 关联业务单号，如应收应付单号、采购单号或销售单号。 */
    @Column(length = 300)
    @Comment("关联业务单号")
    private String relatedBizNo;

    /** 摘要，说明本次资金收付事项。 */
    @Column(length = 200)
    @Comment("资金流水摘要")
    private String summary;

    /** 备注说明，记录业务背景。 */
    @Column(length = 2000)
    @Comment("出纳流水备注")
    private String remark;

    /** 创建人账号。 */
    @Column(length = 80)
    @Comment("创建人账号")
    private String createdBy;

    /** 确认人账号。 */
    @Column(length = 80)
    @Comment("确认人账号")
    private String confirmedBy;

    /** 确认时间。 */
    @Comment("确认时间")
    private OffsetDateTime confirmedTime;

    /** 生成的凭证主键，出纳流水制证后回写。 */
    @Comment("生成的凭证主键")
    private Long voucherId;

    /** 生成的凭证号，出纳流水制证后回写。 */
    @Column(length = 60)
    @Comment("生成的凭证号")
    private String voucherNo;

    /** 获取所属公司字典编码。 */
    public String getOrganizationCode() { return organizationCode; }
    /** 设置所属公司字典编码。 */
    public void setOrganizationCode(String organizationCode) { this.organizationCode = organizationCode; }
    /** 获取出纳流水号。 */
    public String getTransactionNo() { return transactionNo; }
    /** 设置出纳流水号。 */
    public void setTransactionNo(String transactionNo) { this.transactionNo = transactionNo; }
    /** 获取出纳交易日期。 */
    public LocalDate getTransactionDate() { return transactionDate; }
    /** 设置出纳交易日期。 */
    public void setTransactionDate(LocalDate transactionDate) { this.transactionDate = transactionDate; }
    /** 获取出纳流水类型。 */
    public CashierTransactionType getTransactionType() { return transactionType; }
    /** 设置出纳流水类型。 */
    public void setTransactionType(CashierTransactionType transactionType) { this.transactionType = transactionType; }
    /** 获取出纳流水状态。 */
    public CashierTransactionStatus getStatus() { return status; }
    /** 设置出纳流水状态。 */
    public void setStatus(CashierTransactionStatus status) { this.status = status; }
    /** 获取项目字典编码。 */
    public String getProjectCode() { return projectCode; }
    /** 设置项目字典编码。 */
    public void setProjectCode(String projectCode) { this.projectCode = projectCode; }
    /** 获取项目名称快照。 */
    public String getProjectName() { return projectName; }
    /** 设置项目名称快照。 */
    public void setProjectName(String projectName) { this.projectName = projectName; }
    /** 获取往来单位名称。 */
    public String getPartnerName() { return partnerName; }
    /** 设置往来单位名称。 */
    public void setPartnerName(String partnerName) { this.partnerName = partnerName; }
    /** 获取银行或现金账户。 */
    public String getBankAccount() { return bankAccount; }
    /** 设置银行或现金账户。 */
    public void setBankAccount(String bankAccount) { this.bankAccount = bankAccount; }
    /** 获取结算方式。 */
    public String getSettlementMethod() { return settlementMethod; }
    /** 设置结算方式。 */
    public void setSettlementMethod(String settlementMethod) { this.settlementMethod = settlementMethod; }
    /** 获取原币金额。 */
    public BigDecimal getAmount() { return amount; }
    /** 设置原币金额。 */
    public void setAmount(BigDecimal amount) { this.amount = amount; }
    /** 获取币种编码。 */
    public String getCurrencyCode() { return currencyCode; }
    /** 设置币种编码。 */
    public void setCurrencyCode(String currencyCode) { this.currencyCode = currencyCode; }
    /** 获取币种名称快照。 */
    public String getCurrencyName() { return currencyName; }
    /** 设置币种名称快照。 */
    public void setCurrencyName(String currencyName) { this.currencyName = currencyName; }
    /** 获取发生时折人民币汇率。 */
    public BigDecimal getExchangeRateToCny() { return exchangeRateToCny; }
    /** 设置发生时折人民币汇率。 */
    public void setExchangeRateToCny(BigDecimal exchangeRateToCny) { this.exchangeRateToCny = exchangeRateToCny; }
    /** 获取折人民币金额。 */
    public BigDecimal getAmountCny() { return amountCny; }
    /** 设置折人民币金额。 */
    public void setAmountCny(BigDecimal amountCny) { this.amountCny = amountCny; }
    /** 获取关联业务单号。 */
    public String getRelatedBizNo() { return relatedBizNo; }
    /** 设置关联业务单号。 */
    public void setRelatedBizNo(String relatedBizNo) { this.relatedBizNo = relatedBizNo; }
    /** 获取摘要。 */
    public String getSummary() { return summary; }
    /** 设置摘要。 */
    public void setSummary(String summary) { this.summary = summary; }
    /** 获取备注说明。 */
    public String getRemark() { return remark; }
    /** 设置备注说明。 */
    public void setRemark(String remark) { this.remark = remark; }
    /** 获取创建人账号。 */
    public String getCreatedBy() { return createdBy; }
    /** 设置创建人账号。 */
    public void setCreatedBy(String createdBy) { this.createdBy = createdBy; }
    /** 获取确认人账号。 */
    public String getConfirmedBy() { return confirmedBy; }
    /** 设置确认人账号。 */
    public void setConfirmedBy(String confirmedBy) { this.confirmedBy = confirmedBy; }
    /** 获取确认时间。 */
    public OffsetDateTime getConfirmedTime() { return confirmedTime; }
    /** 设置确认时间。 */
    public void setConfirmedTime(OffsetDateTime confirmedTime) { this.confirmedTime = confirmedTime; }
    /** 获取生成的凭证主键。 */
    public Long getVoucherId() { return voucherId; }
    /** 设置生成的凭证主键。 */
    public void setVoucherId(Long voucherId) { this.voucherId = voucherId; }
    /** 获取生成的凭证号。 */
    public String getVoucherNo() { return voucherNo; }
    /** 设置生成的凭证号。 */
    public void setVoucherNo(String voucherNo) { this.voucherNo = voucherNo; }
}
