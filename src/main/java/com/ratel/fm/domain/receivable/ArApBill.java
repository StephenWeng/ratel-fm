package com.ratel.fm.domain.receivable;

import com.ratel.fm.common.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import org.hibernate.annotations.Comment;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * 应收应付单。
 *
 * <p>记录客户应收或供应商应付，支持账龄和付款计划分析。</p>
 *
 * <p>开发组织：ratel；开发人员：WenZhang；联系方式：18782945613。</p>
 *
 * @author ratel
 */
@Entity
@Table(name = "fm_ar_ap_bills")
@Comment("应收应付单表，记录客户应收或供应商应付并支持账龄和付款计划分析")
public class ArApBill extends BaseEntity {

    /** 单据编号，全系统唯一，按单据类型和日期生成。 */
    @Column(nullable = false, unique = true, length = 60)
    @Comment("应收应付单据编号，全系统唯一")
    private String billNo;

    /** 单据类型：应收或应付。 */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    @Comment("单据类型：应收或应付")
    private ArApType billType;

    /** 往来单位名称。应收为客户，应付为供应商。 */
    @Column(nullable = false, length = 160)
    @Comment("往来单位名称")
    private String partnerName;

    /** 项目字典编码，来自基础信息项目字典，用于按项目统计应收应付。 */
    @Column(length = 80)
    @Comment("项目字典编码")
    private String projectCode;

    /** 项目名称快照，避免项目字典名称调整影响历史应收应付单展示。 */
    @Column(length = 160)
    @Comment("项目名称快照")
    private String projectName;

    /** 单据类型名称，如销售应收、采购应付、其他应收。 */
    @Column(length = 80)
    @Comment("业务单据类型")
    private String documentType;

    /** 业务组织。 */
    @Column(length = 120)
    @Comment("业务组织")
    private String businessOrganization;

    /** 结算组织。 */
    @Column(length = 120)
    @Comment("结算组织")
    private String settlementOrganization;

    /** 收付款组织。 */
    @Column(length = 120)
    @Comment("收付款组织")
    private String paymentOrganization;

    /** 收付款条件。 */
    @Column(length = 120)
    @Comment("收付款条件")
    private String paymentTerms;

    /** 结算方式。 */
    @Column(length = 120)
    @Comment("结算方式")
    private String settlementMethod;

    /** 来源单据类型。 */
    @Column(length = 80)
    @Comment("来源单据类型")
    private String sourceBillType;

    /** 来源单据编号。 */
    @Column(length = 300)
    @Comment("来源单据编号")
    private String sourceBillNo;

    /** 单据发生日期。 */
    @Column(nullable = false)
    @Comment("单据发生日期")
    private LocalDate billDate;

    /** 到期日期，用于账龄和逾期状态计算。 */
    @Column(nullable = false)
    @Comment("到期日期")
    private LocalDate dueDate;

    /** 应收或应付总金额，保留 8 位小数。 */
    @Column(nullable = false, precision = 26, scale = 8)
    @Comment("应收或应付总金额，保留8位小数")
    private BigDecimal amount;

    /** 已收或已付金额，默认 0，保留 8 位小数。 */
    @Column(nullable = false, precision = 26, scale = 8)
    @Comment("已收或已付金额，保留8位小数")
    private BigDecimal paidAmount = BigDecimal.ZERO;

    /** 币种编码，来自基础信息币种字典，默认人民币 CNY。 */
    @Column(length = 20)
    @Comment("币种编码，来自基础信息币种字典")
    private String currencyCode = "CNY";

    /** 币种名称，保存应收应付发生时的字典名称快照。 */
    @Column(length = 80)
    @Comment("币种名称快照")
    private String currencyName = "人民币";

    /** 应收应付发生时该币种折人民币汇率，人民币固定为 1。 */
    @Column(precision = 18, scale = 8)
    @Comment("应收应付发生时该币种折人民币汇率")
    private BigDecimal exchangeRateToCny = BigDecimal.ONE;

    /** 应收或应付总金额按当时汇率折算出的人民币金额，保留 8 位小数。 */
    @Column(precision = 26, scale = 8)
    @Comment("应收或应付总金额折人民币金额，保留8位小数")
    private BigDecimal amountCny = BigDecimal.ZERO;

    /** 已收或已付金额按当时汇率折算出的人民币金额，保留 8 位小数。 */
    @Column(precision = 26, scale = 8)
    @Comment("已收或已付金额折人民币金额，保留8位小数")
    private BigDecimal paidAmountCny = BigDecimal.ZERO;

    /** 单据状态：未结、部分结清、已结清、逾期。 */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    @Comment("应收应付单据状态")
    private ArApStatus status = ArApStatus.OPEN;

    /** 付款或收款计划说明，支持长文本说明。 */
    @Column(length = 2000)
    @Comment("付款或收款计划说明")
    private String paymentPlan;

    /** 所属公司字典编码，即账套编码，应收应付单按该字段隔离。 */
    @Column(length = 80)
    @Comment("所属公司字典编码，作为应收应付账套隔离标识")
    private String organizationCode;

    /** 该应收应付单通过会计平台生成的凭证主键。为空表示尚未制证。 */
    @Column
    @Comment("关联凭证主键")
    private Long voucherId;

    /** 该应收应付单通过会计平台生成的凭证号，用于列表展示和在线凭证跳转。 */
    @Column(length = 60)
    @Comment("关联凭证号")
    private String voucherNo;

    /**
     * 执行 getBillNo 方法。
     * 
     * <p>实现步骤：
     * 1. 接收并校验调用方传入的数据；
     * 2. 按当前方法职责执行业务查询、转换或持久化处理；
     * 3. 返回处理结果或更新对象状态。</p>
     */
    public String getBillNo() { return billNo; }
    /**
     * 执行 setBillNo 方法。
     * 
     * <p>实现步骤：
     * 1. 接收并校验调用方传入的数据；
     * 2. 按当前方法职责执行业务查询、转换或持久化处理；
     * 3. 返回处理结果或更新对象状态。</p>
     */
    public void setBillNo(String billNo) { this.billNo = billNo; }
    /**
     * 执行 getBillType 方法。
     * 
     * <p>实现步骤：
     * 1. 接收并校验调用方传入的数据；
     * 2. 按当前方法职责执行业务查询、转换或持久化处理；
     * 3. 返回处理结果或更新对象状态。</p>
     */
    public ArApType getBillType() { return billType; }
    /**
     * 执行 setBillType 方法。
     * 
     * <p>实现步骤：
     * 1. 接收并校验调用方传入的数据；
     * 2. 按当前方法职责执行业务查询、转换或持久化处理；
     * 3. 返回处理结果或更新对象状态。</p>
     */
    public void setBillType(ArApType billType) { this.billType = billType; }
    /**
     * 执行 getPartnerName 方法。
     * 
     * <p>实现步骤：
     * 1. 接收并校验调用方传入的数据；
     * 2. 按当前方法职责执行业务查询、转换或持久化处理；
     * 3. 返回处理结果或更新对象状态。</p>
     */
    public String getPartnerName() { return partnerName; }
    /**
     * 执行 setPartnerName 方法。
     * 
     * <p>实现步骤：
     * 1. 接收并校验调用方传入的数据；
     * 2. 按当前方法职责执行业务查询、转换或持久化处理；
     * 3. 返回处理结果或更新对象状态。</p>
     */
    public void setPartnerName(String partnerName) { this.partnerName = partnerName; }

    /**
     * 获取项目字典编码。
     *
     * <p>实现步骤：返回应收应付单保存的项目编码快照，收付统计按该字段过滤和汇总。</p>
     */
    public String getProjectCode() { return projectCode; }

    /**
     * 设置项目字典编码。
     *
     * <p>实现步骤：保存前端项目下拉框选择的字典编码，后续统计按项目归集。</p>
     */
    public void setProjectCode(String projectCode) { this.projectCode = projectCode; }

    /**
     * 获取项目名称快照。
     *
     * <p>实现步骤：返回单据保存时的项目名称，避免字典改名影响历史展示。</p>
     */
    public String getProjectName() { return projectName; }

    /**
     * 设置项目名称快照。
     *
     * <p>实现步骤：保存项目字典名称快照，供列表、导出、收付统计和查看流水展示。</p>
     */
    public void setProjectName(String projectName) { this.projectName = projectName; }

    /**
     * 执行 getDocumentType 方法。
     * 
     * <p>实现步骤：
     * 1. 接收并校验调用方传入的数据；
     * 2. 按当前方法职责执行业务查询、转换或持久化处理；
     * 3. 返回处理结果或更新对象状态。</p>
     */
    public String getDocumentType() { return documentType; }
    /**
     * 执行 setDocumentType 方法。
     * 
     * <p>实现步骤：
     * 1. 接收并校验调用方传入的数据；
     * 2. 按当前方法职责执行业务查询、转换或持久化处理；
     * 3. 返回处理结果或更新对象状态。</p>
     */
    public void setDocumentType(String documentType) { this.documentType = documentType; }
    /**
     * 执行 getBusinessOrganization 方法。
     * 
     * <p>实现步骤：
     * 1. 接收并校验调用方传入的数据；
     * 2. 按当前方法职责执行业务查询、转换或持久化处理；
     * 3. 返回处理结果或更新对象状态。</p>
     */
    public String getBusinessOrganization() { return businessOrganization; }
    /**
     * 执行 setBusinessOrganization 方法。
     * 
     * <p>实现步骤：
     * 1. 接收并校验调用方传入的数据；
     * 2. 按当前方法职责执行业务查询、转换或持久化处理；
     * 3. 返回处理结果或更新对象状态。</p>
     */
    public void setBusinessOrganization(String businessOrganization) { this.businessOrganization = businessOrganization; }
    /**
     * 执行 getSettlementOrganization 方法。
     * 
     * <p>实现步骤：
     * 1. 接收并校验调用方传入的数据；
     * 2. 按当前方法职责执行业务查询、转换或持久化处理；
     * 3. 返回处理结果或更新对象状态。</p>
     */
    public String getSettlementOrganization() { return settlementOrganization; }
    /**
     * 执行 setSettlementOrganization 方法。
     * 
     * <p>实现步骤：
     * 1. 接收并校验调用方传入的数据；
     * 2. 按当前方法职责执行业务查询、转换或持久化处理；
     * 3. 返回处理结果或更新对象状态。</p>
     */
    public void setSettlementOrganization(String settlementOrganization) { this.settlementOrganization = settlementOrganization; }
    /**
     * 执行 getPaymentOrganization 方法。
     * 
     * <p>实现步骤：
     * 1. 接收并校验调用方传入的数据；
     * 2. 按当前方法职责执行业务查询、转换或持久化处理；
     * 3. 返回处理结果或更新对象状态。</p>
     */
    public String getPaymentOrganization() { return paymentOrganization; }
    /**
     * 执行 setPaymentOrganization 方法。
     * 
     * <p>实现步骤：
     * 1. 接收并校验调用方传入的数据；
     * 2. 按当前方法职责执行业务查询、转换或持久化处理；
     * 3. 返回处理结果或更新对象状态。</p>
     */
    public void setPaymentOrganization(String paymentOrganization) { this.paymentOrganization = paymentOrganization; }
    /**
     * 执行 getPaymentTerms 方法。
     * 
     * <p>实现步骤：
     * 1. 接收并校验调用方传入的数据；
     * 2. 按当前方法职责执行业务查询、转换或持久化处理；
     * 3. 返回处理结果或更新对象状态。</p>
     */
    public String getPaymentTerms() { return paymentTerms; }
    /**
     * 执行 setPaymentTerms 方法。
     * 
     * <p>实现步骤：
     * 1. 接收并校验调用方传入的数据；
     * 2. 按当前方法职责执行业务查询、转换或持久化处理；
     * 3. 返回处理结果或更新对象状态。</p>
     */
    public void setPaymentTerms(String paymentTerms) { this.paymentTerms = paymentTerms; }
    /**
     * 执行 getSettlementMethod 方法。
     * 
     * <p>实现步骤：
     * 1. 接收并校验调用方传入的数据；
     * 2. 按当前方法职责执行业务查询、转换或持久化处理；
     * 3. 返回处理结果或更新对象状态。</p>
     */
    public String getSettlementMethod() { return settlementMethod; }
    /**
     * 执行 setSettlementMethod 方法。
     * 
     * <p>实现步骤：
     * 1. 接收并校验调用方传入的数据；
     * 2. 按当前方法职责执行业务查询、转换或持久化处理；
     * 3. 返回处理结果或更新对象状态。</p>
     */
    public void setSettlementMethod(String settlementMethod) { this.settlementMethod = settlementMethod; }
    /**
     * 执行 getSourceBillType 方法。
     * 
     * <p>实现步骤：
     * 1. 接收并校验调用方传入的数据；
     * 2. 按当前方法职责执行业务查询、转换或持久化处理；
     * 3. 返回处理结果或更新对象状态。</p>
     */
    public String getSourceBillType() { return sourceBillType; }
    /**
     * 执行 setSourceBillType 方法。
     * 
     * <p>实现步骤：
     * 1. 接收并校验调用方传入的数据；
     * 2. 按当前方法职责执行业务查询、转换或持久化处理；
     * 3. 返回处理结果或更新对象状态。</p>
     */
    public void setSourceBillType(String sourceBillType) { this.sourceBillType = sourceBillType; }
    /**
     * 执行 getSourceBillNo 方法。
     * 
     * <p>实现步骤：
     * 1. 接收并校验调用方传入的数据；
     * 2. 按当前方法职责执行业务查询、转换或持久化处理；
     * 3. 返回处理结果或更新对象状态。</p>
     */
    public String getSourceBillNo() { return sourceBillNo; }
    /**
     * 执行 setSourceBillNo 方法。
     * 
     * <p>实现步骤：
     * 1. 接收并校验调用方传入的数据；
     * 2. 按当前方法职责执行业务查询、转换或持久化处理；
     * 3. 返回处理结果或更新对象状态。</p>
     */
    public void setSourceBillNo(String sourceBillNo) { this.sourceBillNo = sourceBillNo; }
    /**
     * 执行 getBillDate 方法。
     * 
     * <p>实现步骤：
     * 1. 接收并校验调用方传入的数据；
     * 2. 按当前方法职责执行业务查询、转换或持久化处理；
     * 3. 返回处理结果或更新对象状态。</p>
     */
    public LocalDate getBillDate() { return billDate; }
    /**
     * 执行 setBillDate 方法。
     * 
     * <p>实现步骤：
     * 1. 接收并校验调用方传入的数据；
     * 2. 按当前方法职责执行业务查询、转换或持久化处理；
     * 3. 返回处理结果或更新对象状态。</p>
     */
    public void setBillDate(LocalDate billDate) { this.billDate = billDate; }
    /**
     * 执行 getDueDate 方法。
     * 
     * <p>实现步骤：
     * 1. 接收并校验调用方传入的数据；
     * 2. 按当前方法职责执行业务查询、转换或持久化处理；
     * 3. 返回处理结果或更新对象状态。</p>
     */
    public LocalDate getDueDate() { return dueDate; }
    /**
     * 执行 setDueDate 方法。
     * 
     * <p>实现步骤：
     * 1. 接收并校验调用方传入的数据；
     * 2. 按当前方法职责执行业务查询、转换或持久化处理；
     * 3. 返回处理结果或更新对象状态。</p>
     */
    public void setDueDate(LocalDate dueDate) { this.dueDate = dueDate; }
    /**
     * 执行 getAmount 方法。
     * 
     * <p>实现步骤：
     * 1. 接收并校验调用方传入的数据；
     * 2. 按当前方法职责执行业务查询、转换或持久化处理；
     * 3. 返回处理结果或更新对象状态。</p>
     */
    public BigDecimal getAmount() { return amount; }
    /**
     * 执行 setAmount 方法。
     * 
     * <p>实现步骤：
     * 1. 接收并校验调用方传入的数据；
     * 2. 按当前方法职责执行业务查询、转换或持久化处理；
     * 3. 返回处理结果或更新对象状态。</p>
     */
    public void setAmount(BigDecimal amount) { this.amount = amount; }
    /**
     * 执行 getPaidAmount 方法。
     * 
     * <p>实现步骤：
     * 1. 接收并校验调用方传入的数据；
     * 2. 按当前方法职责执行业务查询、转换或持久化处理；
     * 3. 返回处理结果或更新对象状态。</p>
     */
    public BigDecimal getPaidAmount() { return paidAmount; }
    /**
     * 执行 setPaidAmount 方法。
     * 
     * <p>实现步骤：
     * 1. 接收并校验调用方传入的数据；
     * 2. 按当前方法职责执行业务查询、转换或持久化处理；
     * 3. 返回处理结果或更新对象状态。</p>
     */
    public void setPaidAmount(BigDecimal paidAmount) { this.paidAmount = paidAmount; }
    /**
     * 执行 getCurrencyCode 方法。
     * 
     * <p>实现步骤：
     * 1. 接收并校验调用方传入的数据；
     * 2. 按当前方法职责执行业务查询、转换或持久化处理；
     * 3. 返回处理结果或更新对象状态。</p>
     */
    public String getCurrencyCode() { return currencyCode; }
    /**
     * 执行 setCurrencyCode 方法。
     * 
     * <p>实现步骤：
     * 1. 接收并校验调用方传入的数据；
     * 2. 按当前方法职责执行业务查询、转换或持久化处理；
     * 3. 返回处理结果或更新对象状态。</p>
     */
    public void setCurrencyCode(String currencyCode) { this.currencyCode = currencyCode; }
    /**
     * 执行 getCurrencyName 方法。
     * 
     * <p>实现步骤：
     * 1. 接收并校验调用方传入的数据；
     * 2. 按当前方法职责执行业务查询、转换或持久化处理；
     * 3. 返回处理结果或更新对象状态。</p>
     */
    public String getCurrencyName() { return currencyName; }
    /**
     * 执行 setCurrencyName 方法。
     * 
     * <p>实现步骤：
     * 1. 接收并校验调用方传入的数据；
     * 2. 按当前方法职责执行业务查询、转换或持久化处理；
     * 3. 返回处理结果或更新对象状态。</p>
     */
    public void setCurrencyName(String currencyName) { this.currencyName = currencyName; }
    /**
     * 执行 getExchangeRateToCny 方法。
     * 
     * <p>实现步骤：
     * 1. 接收并校验调用方传入的数据；
     * 2. 按当前方法职责执行业务查询、转换或持久化处理；
     * 3. 返回处理结果或更新对象状态。</p>
     */
    public BigDecimal getExchangeRateToCny() { return exchangeRateToCny; }
    /**
     * 执行 setExchangeRateToCny 方法。
     * 
     * <p>实现步骤：
     * 1. 接收并校验调用方传入的数据；
     * 2. 按当前方法职责执行业务查询、转换或持久化处理；
     * 3. 返回处理结果或更新对象状态。</p>
     */
    public void setExchangeRateToCny(BigDecimal exchangeRateToCny) { this.exchangeRateToCny = exchangeRateToCny; }
    /**
     * 执行 getAmountCny 方法。
     * 
     * <p>实现步骤：
     * 1. 接收并校验调用方传入的数据；
     * 2. 按当前方法职责执行业务查询、转换或持久化处理；
     * 3. 返回处理结果或更新对象状态。</p>
     */
    public BigDecimal getAmountCny() { return amountCny; }
    /**
     * 执行 setAmountCny 方法。
     * 
     * <p>实现步骤：
     * 1. 接收并校验调用方传入的数据；
     * 2. 按当前方法职责执行业务查询、转换或持久化处理；
     * 3. 返回处理结果或更新对象状态。</p>
     */
    public void setAmountCny(BigDecimal amountCny) { this.amountCny = amountCny; }
    /**
     * 执行 getPaidAmountCny 方法。
     * 
     * <p>实现步骤：
     * 1. 接收并校验调用方传入的数据；
     * 2. 按当前方法职责执行业务查询、转换或持久化处理；
     * 3. 返回处理结果或更新对象状态。</p>
     */
    public BigDecimal getPaidAmountCny() { return paidAmountCny; }
    /**
     * 执行 setPaidAmountCny 方法。
     * 
     * <p>实现步骤：
     * 1. 接收并校验调用方传入的数据；
     * 2. 按当前方法职责执行业务查询、转换或持久化处理；
     * 3. 返回处理结果或更新对象状态。</p>
     */
    public void setPaidAmountCny(BigDecimal paidAmountCny) { this.paidAmountCny = paidAmountCny; }
    /**
     * 执行 getStatus 方法。
     * 
     * <p>实现步骤：
     * 1. 接收并校验调用方传入的数据；
     * 2. 按当前方法职责执行业务查询、转换或持久化处理；
     * 3. 返回处理结果或更新对象状态。</p>
     */
    public ArApStatus getStatus() { return status; }
    /**
     * 执行 setStatus 方法。
     * 
     * <p>实现步骤：
     * 1. 接收并校验调用方传入的数据；
     * 2. 按当前方法职责执行业务查询、转换或持久化处理；
     * 3. 返回处理结果或更新对象状态。</p>
     */
    public void setStatus(ArApStatus status) { this.status = status; }
    /**
     * 执行 getPaymentPlan 方法。
     * 
     * <p>实现步骤：
     * 1. 接收并校验调用方传入的数据；
     * 2. 按当前方法职责执行业务查询、转换或持久化处理；
     * 3. 返回处理结果或更新对象状态。</p>
     */
    public String getPaymentPlan() { return paymentPlan; }
    /**
     * 执行 setPaymentPlan 方法。
     * 
     * <p>实现步骤：
     * 1. 接收并校验调用方传入的数据；
     * 2. 按当前方法职责执行业务查询、转换或持久化处理；
     * 3. 返回处理结果或更新对象状态。</p>
     */
    public void setPaymentPlan(String paymentPlan) { this.paymentPlan = paymentPlan; }
    /**
     * 执行 getOrganizationCode 方法。
     * 
     * <p>实现步骤：
     * 1. 接收并校验调用方传入的数据；
     * 2. 按当前方法职责执行业务查询、转换或持久化处理；
     * 3. 返回处理结果或更新对象状态。</p>
     */
    public String getOrganizationCode() { return organizationCode; }
    /**
     * 执行 setOrganizationCode 方法。
     * 
     * <p>实现步骤：
     * 1. 接收并校验调用方传入的数据；
     * 2. 按当前方法职责执行业务查询、转换或持久化处理；
     * 3. 返回处理结果或更新对象状态。</p>
     */
    public void setOrganizationCode(String organizationCode) { this.organizationCode = organizationCode; }

    /**
     * 获取应收应付单关联凭证主键。
     *
     * <p>实现步骤：返回会计平台自动制证成功后回写的凭证 ID，供应收应付列表判断是否显示在线凭证入口。</p>
     */
    public Long getVoucherId() { return voucherId; }

    /**
     * 设置应收应付单关联凭证主键。
     *
     * <p>实现步骤：自动生成凭证成功后由会计服务写入，保持往来单据到凭证的正向追溯链路。</p>
     */
    public void setVoucherId(Long voucherId) { this.voucherId = voucherId; }

    /**
     * 获取应收应付单关联凭证号。
     *
     * <p>实现步骤：返回会计平台生成的凭证号，供列表展示和在线凭证跳转使用。</p>
     */
    public String getVoucherNo() { return voucherNo; }

    /**
     * 设置应收应付单关联凭证号。
     *
     * <p>实现步骤：自动生成凭证成功后保存凭证号快照，方便用户从往来单据直接查看凭证。</p>
     */
    public void setVoucherNo(String voucherNo) { this.voucherNo = voucherNo; }
}
