package com.ratel.fm.domain.finance;

import com.ratel.fm.common.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import org.hibernate.annotations.Comment;

import java.math.BigDecimal;

/**
 * 财务凭证分录。
 *
 * <p>每条分录对应一个会计科目的借方或贷方发生额。</p>
 *
 * <p>开发组织：ratel；开发人员：WenZhang；联系方式：18782945613。</p>
 *
 * @author ratel
 */
@Entity
@Table(name = "fm_voucher_lines")
@Comment("财务凭证明细表，记录每条会计科目的借方或贷方发生额")
public class VoucherLine extends BaseEntity {

    /** 所属凭证主表。 */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "voucher_id", nullable = false)
    @Comment("所属凭证主表ID")
    private Voucher voucher;

    /** 分录行号，用于保持录入顺序和前端展示顺序。 */
    @Column(nullable = false)
    @Comment("分录行号")
    private int lineNo;

    /** 记账科目。新增凭证时只能选择启用科目。 */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "subject_id", nullable = false)
    @Comment("记账科目ID")
    private AccountingSubject subject;

    /** 分录摘要，可与凭证摘要不同，用于描述该行科目发生原因。 */
    @Column(nullable = false, length = 200)
    @Comment("分录摘要")
    private String summary;

    /** 借方金额，保留 8 位小数。同一分录不能同时有借方和贷方金额。 */
    @Column(precision = 26, scale = 8)
    @Comment("借方金额，保留8位小数")
    private BigDecimal debitAmount = BigDecimal.ZERO;

    /** 贷方金额，保留 8 位小数。同一分录不能同时有借方和贷方金额。 */
    @Column(precision = 26, scale = 8)
    @Comment("贷方金额，保留8位小数")
    private BigDecimal creditAmount = BigDecimal.ZERO;

    /** 分录币种编码，来自币种字典。每条分录独立保存币种，支持同一凭证多币种记账。 */
    @Column(length = 20)
    @Comment("分录币种编码，来自基础信息币种字典")
    private String currencyCode = "CNY";

    /** 分录币种名称快照，避免后续字典改名影响历史凭证展示。 */
    @Column(length = 80)
    @Comment("分录币种名称快照")
    private String currencyName = "人民币";

    /** 分录业务发生时该币种折人民币汇率，后续统计使用该快照而不是实时汇率。 */
    @Column(precision = 18, scale = 8)
    @Comment("分录业务发生时该币种折人民币汇率")
    private BigDecimal exchangeRateToCny = BigDecimal.ONE.setScale(8);

    /** 借方金额按分录保存的当时汇率折算出的人民币金额，保留 8 位小数。 */
    @Column(nullable = false, precision = 26, scale = 8)
    @Comment("借方折人民币金额，保留8位小数")
    private BigDecimal debitAmountCny = BigDecimal.ZERO;

    /** 贷方金额按分录保存的当时汇率折算出的人民币金额，保留 8 位小数。 */
    @Column(nullable = false, precision = 26, scale = 8)
    @Comment("贷方折人民币金额，保留8位小数")
    private BigDecimal creditAmountCny = BigDecimal.ZERO;

    /** 辅助核算信息，预留客户、供应商、项目、部门等扩展。 */
    @Column(length = 200)
    @Comment("辅助核算信息")
    private String auxiliary;

    /**
     * 执行 getVoucher 方法。
     * 
     * <p>实现步骤：
     * 1. 接收并校验调用方传入的数据；
     * 2. 按当前方法职责执行业务查询、转换或持久化处理；
     * 3. 返回处理结果或更新对象状态。</p>
     */
    public Voucher getVoucher() {
        return voucher;
    }

    /**
     * 执行 setVoucher 方法。
     * 
     * <p>实现步骤：
     * 1. 接收并校验调用方传入的数据；
     * 2. 按当前方法职责执行业务查询、转换或持久化处理；
     * 3. 返回处理结果或更新对象状态。</p>
     */
    public void setVoucher(Voucher voucher) {
        this.voucher = voucher;
    }

    /**
     * 执行 getLineNo 方法。
     * 
     * <p>实现步骤：
     * 1. 接收并校验调用方传入的数据；
     * 2. 按当前方法职责执行业务查询、转换或持久化处理；
     * 3. 返回处理结果或更新对象状态。</p>
     */
    public int getLineNo() {
        return lineNo;
    }

    /**
     * 执行 setLineNo 方法。
     * 
     * <p>实现步骤：
     * 1. 接收并校验调用方传入的数据；
     * 2. 按当前方法职责执行业务查询、转换或持久化处理；
     * 3. 返回处理结果或更新对象状态。</p>
     */
    public void setLineNo(int lineNo) {
        this.lineNo = lineNo;
    }

    /**
     * 执行 getSubject 方法。
     * 
     * <p>实现步骤：
     * 1. 接收并校验调用方传入的数据；
     * 2. 按当前方法职责执行业务查询、转换或持久化处理；
     * 3. 返回处理结果或更新对象状态。</p>
     */
    public AccountingSubject getSubject() {
        return subject;
    }

    /**
     * 执行 setSubject 方法。
     * 
     * <p>实现步骤：
     * 1. 接收并校验调用方传入的数据；
     * 2. 按当前方法职责执行业务查询、转换或持久化处理；
     * 3. 返回处理结果或更新对象状态。</p>
     */
    public void setSubject(AccountingSubject subject) {
        this.subject = subject;
    }

    /**
     * 执行 getSummary 方法。
     * 
     * <p>实现步骤：
     * 1. 接收并校验调用方传入的数据；
     * 2. 按当前方法职责执行业务查询、转换或持久化处理；
     * 3. 返回处理结果或更新对象状态。</p>
     */
    public String getSummary() {
        return summary;
    }

    /**
     * 执行 setSummary 方法。
     * 
     * <p>实现步骤：
     * 1. 接收并校验调用方传入的数据；
     * 2. 按当前方法职责执行业务查询、转换或持久化处理；
     * 3. 返回处理结果或更新对象状态。</p>
     */
    public void setSummary(String summary) {
        this.summary = summary;
    }

    /**
     * 执行 getDebitAmount 方法。
     * 
     * <p>实现步骤：
     * 1. 接收并校验调用方传入的数据；
     * 2. 按当前方法职责执行业务查询、转换或持久化处理；
     * 3. 返回处理结果或更新对象状态。</p>
     */
    public BigDecimal getDebitAmount() {
        return debitAmount;
    }

    /**
     * 执行 setDebitAmount 方法。
     * 
     * <p>实现步骤：
     * 1. 接收并校验调用方传入的数据；
     * 2. 按当前方法职责执行业务查询、转换或持久化处理；
     * 3. 返回处理结果或更新对象状态。</p>
     */
    public void setDebitAmount(BigDecimal debitAmount) {
        this.debitAmount = debitAmount;
    }

    /**
     * 执行 getCreditAmount 方法。
     * 
     * <p>实现步骤：
     * 1. 接收并校验调用方传入的数据；
     * 2. 按当前方法职责执行业务查询、转换或持久化处理；
     * 3. 返回处理结果或更新对象状态。</p>
     */
    public BigDecimal getCreditAmount() {
        return creditAmount;
    }

    /**
     * 执行 setCreditAmount 方法。
     * 
     * <p>实现步骤：
     * 1. 接收并校验调用方传入的数据；
     * 2. 按当前方法职责执行业务查询、转换或持久化处理；
     * 3. 返回处理结果或更新对象状态。</p>
     */
    public void setCreditAmount(BigDecimal creditAmount) {
        this.creditAmount = creditAmount;
    }

    /**
     * 执行 getCurrencyCode 方法。
     * 
     * <p>实现步骤：
     * 1. 接收并校验调用方传入的数据；
     * 2. 按当前方法职责执行业务查询、转换或持久化处理；
     * 3. 返回处理结果或更新对象状态。</p>
     */
    public String getCurrencyCode() {
        return currencyCode;
    }

    /**
     * 执行 setCurrencyCode 方法。
     * 
     * <p>实现步骤：
     * 1. 接收并校验调用方传入的数据；
     * 2. 按当前方法职责执行业务查询、转换或持久化处理；
     * 3. 返回处理结果或更新对象状态。</p>
     */
    public void setCurrencyCode(String currencyCode) {
        this.currencyCode = currencyCode;
    }

    /**
     * 执行 getCurrencyName 方法。
     * 
     * <p>实现步骤：
     * 1. 接收并校验调用方传入的数据；
     * 2. 按当前方法职责执行业务查询、转换或持久化处理；
     * 3. 返回处理结果或更新对象状态。</p>
     */
    public String getCurrencyName() {
        return currencyName;
    }

    /**
     * 执行 setCurrencyName 方法。
     * 
     * <p>实现步骤：
     * 1. 接收并校验调用方传入的数据；
     * 2. 按当前方法职责执行业务查询、转换或持久化处理；
     * 3. 返回处理结果或更新对象状态。</p>
     */
    public void setCurrencyName(String currencyName) {
        this.currencyName = currencyName;
    }

    /**
     * 执行 getExchangeRateToCny 方法。
     * 
     * <p>实现步骤：
     * 1. 接收并校验调用方传入的数据；
     * 2. 按当前方法职责执行业务查询、转换或持久化处理；
     * 3. 返回处理结果或更新对象状态。</p>
     */
    public BigDecimal getExchangeRateToCny() {
        return exchangeRateToCny;
    }

    /**
     * 执行 setExchangeRateToCny 方法。
     * 
     * <p>实现步骤：
     * 1. 接收并校验调用方传入的数据；
     * 2. 按当前方法职责执行业务查询、转换或持久化处理；
     * 3. 返回处理结果或更新对象状态。</p>
     */
    public void setExchangeRateToCny(BigDecimal exchangeRateToCny) {
        this.exchangeRateToCny = exchangeRateToCny;
    }

    /**
     * 执行 getDebitAmountCny 方法。
     * 
     * <p>实现步骤：
     * 1. 接收并校验调用方传入的数据；
     * 2. 按当前方法职责执行业务查询、转换或持久化处理；
     * 3. 返回处理结果或更新对象状态。</p>
     */
    public BigDecimal getDebitAmountCny() {
        return debitAmountCny;
    }

    /**
     * 执行 setDebitAmountCny 方法。
     * 
     * <p>实现步骤：
     * 1. 接收并校验调用方传入的数据；
     * 2. 按当前方法职责执行业务查询、转换或持久化处理；
     * 3. 返回处理结果或更新对象状态。</p>
     */
    public void setDebitAmountCny(BigDecimal debitAmountCny) {
        this.debitAmountCny = debitAmountCny;
    }

    /**
     * 执行 getCreditAmountCny 方法。
     * 
     * <p>实现步骤：
     * 1. 接收并校验调用方传入的数据；
     * 2. 按当前方法职责执行业务查询、转换或持久化处理；
     * 3. 返回处理结果或更新对象状态。</p>
     */
    public BigDecimal getCreditAmountCny() {
        return creditAmountCny;
    }

    /**
     * 执行 setCreditAmountCny 方法。
     * 
     * <p>实现步骤：
     * 1. 接收并校验调用方传入的数据；
     * 2. 按当前方法职责执行业务查询、转换或持久化处理；
     * 3. 返回处理结果或更新对象状态。</p>
     */
    public void setCreditAmountCny(BigDecimal creditAmountCny) {
        this.creditAmountCny = creditAmountCny;
    }

    /**
     * 执行 getAuxiliary 方法。
     * 
     * <p>实现步骤：
     * 1. 接收并校验调用方传入的数据；
     * 2. 按当前方法职责执行业务查询、转换或持久化处理；
     * 3. 返回处理结果或更新对象状态。</p>
     */
    public String getAuxiliary() {
        return auxiliary;
    }

    /**
     * 执行 setAuxiliary 方法。
     * 
     * <p>实现步骤：
     * 1. 接收并校验调用方传入的数据；
     * 2. 按当前方法职责执行业务查询、转换或持久化处理；
     * 3. 返回处理结果或更新对象状态。</p>
     */
    public void setAuxiliary(String auxiliary) {
        this.auxiliary = auxiliary;
    }
}
