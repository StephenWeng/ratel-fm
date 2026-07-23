package com.ratel.fm.domain.receivable;

import com.ratel.fm.common.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import org.hibernate.annotations.Comment;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * 应收应付收付核销流水。
 *
 * <p>实现目的：记录每次收款或付款核销动作，累计更新应收应付单已收/已付金额，
 * 让往来状态由资金事实驱动，而不是只靠新增表单一次性录入。</p>
 */
@Entity
@Table(name = "fm_ar_ap_settlements")
@Comment("应收应付收付核销流水表，记录每次收款或付款核销")
public class ArApSettlement extends BaseEntity {

    /** 所属公司字典编码，即账套编码，核销流水按该字段隔离。 */
    @Column(nullable = false, length = 80)
    @Comment("所属公司字典编码，作为收付核销账套隔离标识")
    private String organizationCode;

    /** 被核销的应收应付单。 */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "bill_id", nullable = false)
    @Comment("被核销的应收应付单主键")
    private ArApBill bill;

    /** 核销日期，即实际收款或付款日期。 */
    @Column(nullable = false)
    @Comment("核销日期")
    private LocalDate settlementDate;

    /** 本次核销原币金额。 */
    @Column(nullable = false, precision = 26, scale = 8)
    @Comment("本次核销原币金额")
    private BigDecimal amount = BigDecimal.ZERO;

    /** 本次核销折人民币金额。 */
    @Column(nullable = false, precision = 26, scale = 8)
    @Comment("本次核销折人民币金额")
    private BigDecimal amountCny = BigDecimal.ZERO;

    /** 结算方式。 */
    @Column(length = 120)
    @Comment("结算方式")
    private String settlementMethod;

    /** 银行或现金账户。 */
    @Column(length = 160)
    @Comment("银行或现金账户")
    private String bankAccount;

    /** 关联出纳流水号，存在出纳流水时回填。 */
    @Column(length = 80)
    @Comment("关联出纳流水号")
    private String cashierTransactionNo;

    /** 核销说明。 */
    @Column(length = 2000)
    @Comment("核销说明")
    private String remark;

    /** 获取所属公司字典编码。 */
    public String getOrganizationCode() { return organizationCode; }
    /** 设置所属公司字典编码。 */
    public void setOrganizationCode(String organizationCode) { this.organizationCode = organizationCode; }
    /** 获取被核销的应收应付单。 */
    public ArApBill getBill() { return bill; }
    /** 设置被核销的应收应付单。 */
    public void setBill(ArApBill bill) { this.bill = bill; }
    /** 获取核销日期。 */
    public LocalDate getSettlementDate() { return settlementDate; }
    /** 设置核销日期。 */
    public void setSettlementDate(LocalDate settlementDate) { this.settlementDate = settlementDate; }
    /** 获取本次核销原币金额。 */
    public BigDecimal getAmount() { return amount; }
    /** 设置本次核销原币金额。 */
    public void setAmount(BigDecimal amount) { this.amount = amount; }
    /** 获取本次核销折人民币金额。 */
    public BigDecimal getAmountCny() { return amountCny; }
    /** 设置本次核销折人民币金额。 */
    public void setAmountCny(BigDecimal amountCny) { this.amountCny = amountCny; }
    /** 获取结算方式。 */
    public String getSettlementMethod() { return settlementMethod; }
    /** 设置结算方式。 */
    public void setSettlementMethod(String settlementMethod) { this.settlementMethod = settlementMethod; }
    /** 获取银行或现金账户。 */
    public String getBankAccount() { return bankAccount; }
    /** 设置银行或现金账户。 */
    public void setBankAccount(String bankAccount) { this.bankAccount = bankAccount; }
    /** 获取关联出纳流水号。 */
    public String getCashierTransactionNo() { return cashierTransactionNo; }
    /** 设置关联出纳流水号。 */
    public void setCashierTransactionNo(String cashierTransactionNo) { this.cashierTransactionNo = cashierTransactionNo; }
    /** 获取核销说明。 */
    public String getRemark() { return remark; }
    /** 设置核销说明。 */
    public void setRemark(String remark) { this.remark = remark; }
}
