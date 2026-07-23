package com.ratel.fm.domain.purchase;

import com.ratel.fm.common.BaseEntity;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
import jakarta.persistence.Table;
import org.hibernate.annotations.Comment;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * 采购订单主表。
 *
 * <p>采购单为后续应付、库存入库和财务凭证提供业务来源。</p>
 *
 * <p>开发组织：ratel；开发人员：WenZhang；联系方式：18782945613。</p>
 *
 * @author ratel
 */
@Entity
@Table(name = "fm_purchase_orders")
@Comment("采购订单主表，为应付、库存入库和财务凭证提供业务来源")
public class PurchaseOrder extends BaseEntity {

    /** 采购单号，全系统唯一，按采购日期生成。 */
    @Column(nullable = false, unique = true, length = 60)
    @Comment("采购单号，全系统唯一")
    private String orderNo;

    /** 所属公司字典编码，即账套编码，采购单及明细按该字段隔离。 */
    @Column(nullable = false, length = 80)
    @Comment("所属公司字典编码，作为采购单账套隔离标识")
    private String organizationCode;

    /** 供应商名称。 */
    @Column(nullable = false, length = 160)
    @Comment("供应商名称")
    private String supplierName;

    /** 单据类型，如标准采购订单、费用采购订单。 */
    @Column(length = 80)
    @Comment("采购单据类型")
    private String documentType = "标准采购订单";

    /** 业务类型，如标准采购、委外采购、资产采购。 */
    @Column(length = 80)
    @Comment("采购业务类型")
    private String businessType = "标准采购";

    /** 项目字典编码，来自基础信息项目字典，用于按项目归集采购业务。 */
    @Column(length = 80)
    @Comment("项目字典编码")
    private String projectCode;

    /** 项目名称快照，避免项目字典名称变化影响历史采购单展示。 */
    @Column(length = 160)
    @Comment("项目名称快照")
    private String projectName;

    /** 采购组织。 */
    @Column(length = 120)
    @Comment("采购组织")
    private String purchaseOrganization;

    /** 采购部门。 */
    @Column(length = 120)
    @Comment("采购部门")
    private String purchaseDepartment;

    /** 采购员。 */
    @Column(length = 120)
    @Comment("采购员")
    private String purchaserName;

    /** 结算组织。 */
    @Column(length = 120)
    @Comment("结算组织")
    private String settlementOrganization;

    /** 付款条件。 */
    @Column(length = 120)
    @Comment("付款条件")
    private String paymentTerms;

    /** 结算方式。 */
    @Column(length = 120)
    @Comment("结算方式")
    private String settlementMethod;

    /** 交货条件。 */
    @Column(length = 120)
    @Comment("交货条件")
    private String deliveryTerms;

    /** 来源单据类型。 */
    @Column(length = 80)
    @Comment("来源单据类型")
    private String sourceBillType;

    /** 来源单据编号。 */
    @Column(length = 300)
    @Comment("来源单据编号")
    private String sourceBillNo;

    /** 采购日期，用于排序、统计和单号生成。 */
    @Column(nullable = false)
    @Comment("采购日期")
    private LocalDate orderDate;

    /** 采购状态，控制是否允许修改以及业务流转位置。 */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    @Comment("采购状态")
    private PurchaseStatus status = PurchaseStatus.DRAFT;

    /** 采购总金额，由明细数量乘单价汇总得到，保留 8 位小数。 */
    @Column(nullable = false, precision = 26, scale = 8)
    @Comment("采购总金额，保留8位小数")
    private BigDecimal totalAmount = BigDecimal.ZERO;

    /** 币种编码，来自基础信息币种字典，默认人民币 CNY。 */
    @Column(length = 20)
    @Comment("币种编码，来自基础信息币种字典")
    private String currencyCode = "CNY";

    /** 币种名称，保存采购业务发生时的字典名称快照。 */
    @Column(length = 80)
    @Comment("币种名称快照")
    private String currencyName = "人民币";

    /** 采购业务发生时该币种折人民币汇率，人民币固定为 1。 */
    @Column(precision = 18, scale = 8)
    @Comment("采购业务发生时该币种折人民币汇率")
    private BigDecimal exchangeRateToCny = BigDecimal.ONE;

    /** 采购总金额按当时汇率折算出的人民币金额，保留 8 位小数。 */
    @Column(precision = 26, scale = 8)
    @Comment("采购总金额折人民币金额，保留8位小数")
    private BigDecimal totalAmountCny = BigDecimal.ZERO;

    /** 创建采购单的登录账号。 */
    @Column(nullable = false, length = 80)
    @Comment("创建采购单的登录账号")
    private String createdBy;

    /** 采购备注，支持长文本说明，页面以文本域录入。 */
    @Column(length = 2000)
    @Comment("采购备注")
    private String remark;

    /** 取消采购类型，来自取消类型字典，保存名称快照。 */
    @Column(length = 120)
    @Comment("取消采购类型")
    private String cancelType;

    /** 取消采购原因，支持长文本说明。 */
    @Column(length = 2000)
    @Comment("取消采购原因")
    private String cancelReason;

    /** 该采购单通过会计平台生成的凭证主键。为空表示尚未制证。 */
    @Column
    @Comment("关联凭证主键")
    private Long voucherId;

    /** 该采购单通过会计平台生成的凭证号，用于列表展示和在线凭证跳转。 */
    @Column(length = 60)
    @Comment("关联凭证号")
    private String voucherNo;

    /** 采购明细集合。替换明细时通过 orphanRemoval 删除旧明细。 */
    @OneToMany(mappedBy = "purchaseOrder", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("lineNo ASC")
    private List<PurchaseOrderLine> lines = new ArrayList<>();

    /**
     * 执行 getOrderNo 方法。
     * 
     * <p>实现步骤：
     * 1. 接收并校验调用方传入的数据；
     * 2. 按当前方法职责执行业务查询、转换或持久化处理；
     * 3. 返回处理结果或更新对象状态。</p>
     */
    public String getOrderNo() {
        return orderNo;
    }

    /**
     * 执行 setOrderNo 方法。
     * 
     * <p>实现步骤：
     * 1. 接收并校验调用方传入的数据；
     * 2. 按当前方法职责执行业务查询、转换或持久化处理；
     * 3. 返回处理结果或更新对象状态。</p>
     */
    public void setOrderNo(String orderNo) {
        this.orderNo = orderNo;
    }

    /**
     * 获取采购单所属公司编码。
     *
     * <p>实现步骤：直接返回采购单创建时写入的账套编码，列表、导出、状态流转和会计平台均按该字段隔离。</p>
     */
    public String getOrganizationCode() {
        return organizationCode;
    }

    /**
     * 设置采购单所属公司编码。
     *
     * <p>实现步骤：新增采购单时写入当前登录公司的字典编码，确保不同公司采购数据互不可见。</p>
     */
    public void setOrganizationCode(String organizationCode) {
        this.organizationCode = organizationCode;
    }

    /**
     * 执行 getSupplierName 方法。
     * 
     * <p>实现步骤：
     * 1. 接收并校验调用方传入的数据；
     * 2. 按当前方法职责执行业务查询、转换或持久化处理；
     * 3. 返回处理结果或更新对象状态。</p>
     */
    public String getSupplierName() {
        return supplierName;
    }

    /**
     * 执行 setSupplierName 方法。
     * 
     * <p>实现步骤：
     * 1. 接收并校验调用方传入的数据；
     * 2. 按当前方法职责执行业务查询、转换或持久化处理；
     * 3. 返回处理结果或更新对象状态。</p>
     */
    public void setSupplierName(String supplierName) {
        this.supplierName = supplierName;
    }

    /**
     * 执行 getDocumentType 方法。
     * 
     * <p>实现步骤：
     * 1. 接收并校验调用方传入的数据；
     * 2. 按当前方法职责执行业务查询、转换或持久化处理；
     * 3. 返回处理结果或更新对象状态。</p>
     */
    public String getDocumentType() {
        return documentType;
    }

    /**
     * 执行 setDocumentType 方法。
     * 
     * <p>实现步骤：
     * 1. 接收并校验调用方传入的数据；
     * 2. 按当前方法职责执行业务查询、转换或持久化处理；
     * 3. 返回处理结果或更新对象状态。</p>
     */
    public void setDocumentType(String documentType) {
        this.documentType = documentType;
    }

    /**
     * 执行 getBusinessType 方法。
     * 
     * <p>实现步骤：
     * 1. 接收并校验调用方传入的数据；
     * 2. 按当前方法职责执行业务查询、转换或持久化处理；
     * 3. 返回处理结果或更新对象状态。</p>
     */
    public String getBusinessType() {
        return businessType;
    }

    /**
     * 执行 setBusinessType 方法。
     * 
     * <p>实现步骤：
     * 1. 接收并校验调用方传入的数据；
     * 2. 按当前方法职责执行业务查询、转换或持久化处理；
     * 3. 返回处理结果或更新对象状态。</p>
     */
    public void setBusinessType(String businessType) {
        this.businessType = businessType;
    }

    /**
     * 获取项目字典编码。
     *
     * <p>实现步骤：返回采购单保存的项目编码快照，用于采购列表筛选和业务流水展示。</p>
     */
    public String getProjectCode() {
        return projectCode;
    }

    /**
     * 设置项目字典编码。
     *
     * <p>实现步骤：保存前端项目下拉框选中的字典编码，后续统计按该编码归集。</p>
     */
    public void setProjectCode(String projectCode) {
        this.projectCode = projectCode;
    }

    /**
     * 获取项目名称快照。
     *
     * <p>实现步骤：返回采购单保存的项目名称，避免字典维护影响历史单据可读性。</p>
     */
    public String getProjectName() {
        return projectName;
    }

    /**
     * 设置项目名称快照。
     *
     * <p>实现步骤：保存项目字典名称快照，供列表、导出和查看流水直接展示。</p>
     */
    public void setProjectName(String projectName) {
        this.projectName = projectName;
    }

    /**
     * 执行 getPurchaseOrganization 方法。
     * 
     * <p>实现步骤：
     * 1. 接收并校验调用方传入的数据；
     * 2. 按当前方法职责执行业务查询、转换或持久化处理；
     * 3. 返回处理结果或更新对象状态。</p>
     */
    public String getPurchaseOrganization() {
        return purchaseOrganization;
    }

    /**
     * 执行 setPurchaseOrganization 方法。
     * 
     * <p>实现步骤：
     * 1. 接收并校验调用方传入的数据；
     * 2. 按当前方法职责执行业务查询、转换或持久化处理；
     * 3. 返回处理结果或更新对象状态。</p>
     */
    public void setPurchaseOrganization(String purchaseOrganization) {
        this.purchaseOrganization = purchaseOrganization;
    }

    /**
     * 执行 getPurchaseDepartment 方法。
     * 
     * <p>实现步骤：
     * 1. 接收并校验调用方传入的数据；
     * 2. 按当前方法职责执行业务查询、转换或持久化处理；
     * 3. 返回处理结果或更新对象状态。</p>
     */
    public String getPurchaseDepartment() {
        return purchaseDepartment;
    }

    /**
     * 执行 setPurchaseDepartment 方法。
     * 
     * <p>实现步骤：
     * 1. 接收并校验调用方传入的数据；
     * 2. 按当前方法职责执行业务查询、转换或持久化处理；
     * 3. 返回处理结果或更新对象状态。</p>
     */
    public void setPurchaseDepartment(String purchaseDepartment) {
        this.purchaseDepartment = purchaseDepartment;
    }

    /**
     * 执行 getPurchaserName 方法。
     * 
     * <p>实现步骤：
     * 1. 接收并校验调用方传入的数据；
     * 2. 按当前方法职责执行业务查询、转换或持久化处理；
     * 3. 返回处理结果或更新对象状态。</p>
     */
    public String getPurchaserName() {
        return purchaserName;
    }

    /**
     * 执行 setPurchaserName 方法。
     * 
     * <p>实现步骤：
     * 1. 接收并校验调用方传入的数据；
     * 2. 按当前方法职责执行业务查询、转换或持久化处理；
     * 3. 返回处理结果或更新对象状态。</p>
     */
    public void setPurchaserName(String purchaserName) {
        this.purchaserName = purchaserName;
    }

    /**
     * 执行 getSettlementOrganization 方法。
     * 
     * <p>实现步骤：
     * 1. 接收并校验调用方传入的数据；
     * 2. 按当前方法职责执行业务查询、转换或持久化处理；
     * 3. 返回处理结果或更新对象状态。</p>
     */
    public String getSettlementOrganization() {
        return settlementOrganization;
    }

    /**
     * 执行 setSettlementOrganization 方法。
     * 
     * <p>实现步骤：
     * 1. 接收并校验调用方传入的数据；
     * 2. 按当前方法职责执行业务查询、转换或持久化处理；
     * 3. 返回处理结果或更新对象状态。</p>
     */
    public void setSettlementOrganization(String settlementOrganization) {
        this.settlementOrganization = settlementOrganization;
    }

    /**
     * 执行 getPaymentTerms 方法。
     * 
     * <p>实现步骤：
     * 1. 接收并校验调用方传入的数据；
     * 2. 按当前方法职责执行业务查询、转换或持久化处理；
     * 3. 返回处理结果或更新对象状态。</p>
     */
    public String getPaymentTerms() {
        return paymentTerms;
    }

    /**
     * 执行 setPaymentTerms 方法。
     * 
     * <p>实现步骤：
     * 1. 接收并校验调用方传入的数据；
     * 2. 按当前方法职责执行业务查询、转换或持久化处理；
     * 3. 返回处理结果或更新对象状态。</p>
     */
    public void setPaymentTerms(String paymentTerms) {
        this.paymentTerms = paymentTerms;
    }

    /**
     * 执行 getSettlementMethod 方法。
     * 
     * <p>实现步骤：
     * 1. 接收并校验调用方传入的数据；
     * 2. 按当前方法职责执行业务查询、转换或持久化处理；
     * 3. 返回处理结果或更新对象状态。</p>
     */
    public String getSettlementMethod() {
        return settlementMethod;
    }

    /**
     * 执行 setSettlementMethod 方法。
     * 
     * <p>实现步骤：
     * 1. 接收并校验调用方传入的数据；
     * 2. 按当前方法职责执行业务查询、转换或持久化处理；
     * 3. 返回处理结果或更新对象状态。</p>
     */
    public void setSettlementMethod(String settlementMethod) {
        this.settlementMethod = settlementMethod;
    }

    /**
     * 执行 getDeliveryTerms 方法。
     * 
     * <p>实现步骤：
     * 1. 接收并校验调用方传入的数据；
     * 2. 按当前方法职责执行业务查询、转换或持久化处理；
     * 3. 返回处理结果或更新对象状态。</p>
     */
    public String getDeliveryTerms() {
        return deliveryTerms;
    }

    /**
     * 执行 setDeliveryTerms 方法。
     * 
     * <p>实现步骤：
     * 1. 接收并校验调用方传入的数据；
     * 2. 按当前方法职责执行业务查询、转换或持久化处理；
     * 3. 返回处理结果或更新对象状态。</p>
     */
    public void setDeliveryTerms(String deliveryTerms) {
        this.deliveryTerms = deliveryTerms;
    }

    /**
     * 执行 getSourceBillType 方法。
     * 
     * <p>实现步骤：
     * 1. 接收并校验调用方传入的数据；
     * 2. 按当前方法职责执行业务查询、转换或持久化处理；
     * 3. 返回处理结果或更新对象状态。</p>
     */
    public String getSourceBillType() {
        return sourceBillType;
    }

    /**
     * 执行 setSourceBillType 方法。
     * 
     * <p>实现步骤：
     * 1. 接收并校验调用方传入的数据；
     * 2. 按当前方法职责执行业务查询、转换或持久化处理；
     * 3. 返回处理结果或更新对象状态。</p>
     */
    public void setSourceBillType(String sourceBillType) {
        this.sourceBillType = sourceBillType;
    }

    /**
     * 执行 getSourceBillNo 方法。
     * 
     * <p>实现步骤：
     * 1. 接收并校验调用方传入的数据；
     * 2. 按当前方法职责执行业务查询、转换或持久化处理；
     * 3. 返回处理结果或更新对象状态。</p>
     */
    public String getSourceBillNo() {
        return sourceBillNo;
    }

    /**
     * 执行 setSourceBillNo 方法。
     * 
     * <p>实现步骤：
     * 1. 接收并校验调用方传入的数据；
     * 2. 按当前方法职责执行业务查询、转换或持久化处理；
     * 3. 返回处理结果或更新对象状态。</p>
     */
    public void setSourceBillNo(String sourceBillNo) {
        this.sourceBillNo = sourceBillNo;
    }

    /**
     * 执行 getOrderDate 方法。
     * 
     * <p>实现步骤：
     * 1. 接收并校验调用方传入的数据；
     * 2. 按当前方法职责执行业务查询、转换或持久化处理；
     * 3. 返回处理结果或更新对象状态。</p>
     */
    public LocalDate getOrderDate() {
        return orderDate;
    }

    /**
     * 执行 setOrderDate 方法。
     * 
     * <p>实现步骤：
     * 1. 接收并校验调用方传入的数据；
     * 2. 按当前方法职责执行业务查询、转换或持久化处理；
     * 3. 返回处理结果或更新对象状态。</p>
     */
    public void setOrderDate(LocalDate orderDate) {
        this.orderDate = orderDate;
    }

    /**
     * 执行 getStatus 方法。
     * 
     * <p>实现步骤：
     * 1. 接收并校验调用方传入的数据；
     * 2. 按当前方法职责执行业务查询、转换或持久化处理；
     * 3. 返回处理结果或更新对象状态。</p>
     */
    public PurchaseStatus getStatus() {
        return status;
    }

    /**
     * 执行 setStatus 方法。
     * 
     * <p>实现步骤：
     * 1. 接收并校验调用方传入的数据；
     * 2. 按当前方法职责执行业务查询、转换或持久化处理；
     * 3. 返回处理结果或更新对象状态。</p>
     */
    public void setStatus(PurchaseStatus status) {
        this.status = status;
    }

    /**
     * 执行 getTotalAmount 方法。
     * 
     * <p>实现步骤：
     * 1. 接收并校验调用方传入的数据；
     * 2. 按当前方法职责执行业务查询、转换或持久化处理；
     * 3. 返回处理结果或更新对象状态。</p>
     */
    public BigDecimal getTotalAmount() {
        return totalAmount;
    }

    /**
     * 执行 setTotalAmount 方法。
     * 
     * <p>实现步骤：
     * 1. 接收并校验调用方传入的数据；
     * 2. 按当前方法职责执行业务查询、转换或持久化处理；
     * 3. 返回处理结果或更新对象状态。</p>
     */
    public void setTotalAmount(BigDecimal totalAmount) {
        this.totalAmount = totalAmount;
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
     * 执行 getTotalAmountCny 方法。
     * 
     * <p>实现步骤：
     * 1. 接收并校验调用方传入的数据；
     * 2. 按当前方法职责执行业务查询、转换或持久化处理；
     * 3. 返回处理结果或更新对象状态。</p>
     */
    public BigDecimal getTotalAmountCny() {
        return totalAmountCny;
    }

    /**
     * 执行 setTotalAmountCny 方法。
     * 
     * <p>实现步骤：
     * 1. 接收并校验调用方传入的数据；
     * 2. 按当前方法职责执行业务查询、转换或持久化处理；
     * 3. 返回处理结果或更新对象状态。</p>
     */
    public void setTotalAmountCny(BigDecimal totalAmountCny) {
        this.totalAmountCny = totalAmountCny;
    }

    /**
     * 执行 getCreatedBy 方法。
     * 
     * <p>实现步骤：
     * 1. 接收并校验调用方传入的数据；
     * 2. 按当前方法职责执行业务查询、转换或持久化处理；
     * 3. 返回处理结果或更新对象状态。</p>
     */
    public String getCreatedBy() {
        return createdBy;
    }

    /**
     * 执行 setCreatedBy 方法。
     * 
     * <p>实现步骤：
     * 1. 接收并校验调用方传入的数据；
     * 2. 按当前方法职责执行业务查询、转换或持久化处理；
     * 3. 返回处理结果或更新对象状态。</p>
     */
    public void setCreatedBy(String createdBy) {
        this.createdBy = createdBy;
    }

    /**
     * 执行 getRemark 方法。
     * 
     * <p>实现步骤：
     * 1. 接收并校验调用方传入的数据；
     * 2. 按当前方法职责执行业务查询、转换或持久化处理；
     * 3. 返回处理结果或更新对象状态。</p>
     */
    public String getRemark() {
        return remark;
    }

    /**
     * 执行 setRemark 方法。
     * 
     * <p>实现步骤：
     * 1. 接收并校验调用方传入的数据；
     * 2. 按当前方法职责执行业务查询、转换或持久化处理；
     * 3. 返回处理结果或更新对象状态。</p>
     */
    public void setRemark(String remark) {
        this.remark = remark;
    }

    /**
     * 获取取消采购类型。
     *
     * <p>实现步骤：返回采购取消动作保存的类型名称，列表和流水可直接展示。</p>
     */
    public String getCancelType() {
        return cancelType;
    }

    /**
     * 设置取消采购类型。
     *
     * <p>实现步骤：取消采购时由前端字典选择带入，用于后续业务追溯。</p>
     */
    public void setCancelType(String cancelType) {
        this.cancelType = cancelType;
    }

    /**
     * 获取取消采购原因。
     *
     * <p>实现步骤：返回取消采购时填写的详细说明，供查看流水和后续审计使用。</p>
     */
    public String getCancelReason() {
        return cancelReason;
    }

    /**
     * 设置取消采购原因。
     *
     * <p>实现步骤：取消采购时保存用户填写的原因，最大长度由接口 DTO 和页面共同校验。</p>
     */
    public void setCancelReason(String cancelReason) {
        this.cancelReason = cancelReason;
    }

    /**
     * 获取采购单关联凭证主键。
     *
     * <p>实现步骤：返回会计平台自动制证成功后回写的凭证 ID，供采购列表判断是否显示在线凭证按钮。</p>
     */
    public Long getVoucherId() {
        return voucherId;
    }

    /**
     * 设置采购单关联凭证主键。
     *
     * <p>实现步骤：自动生成凭证成功后由会计服务写入，保持采购单到凭证的正向追溯链路。</p>
     */
    public void setVoucherId(Long voucherId) {
        this.voucherId = voucherId;
    }

    /**
     * 获取采购单关联凭证号。
     *
     * <p>实现步骤：返回会计平台生成的凭证号，供列表展示和在线凭证入口使用。</p>
     */
    public String getVoucherNo() {
        return voucherNo;
    }

    /**
     * 设置采购单关联凭证号。
     *
     * <p>实现步骤：自动生成凭证成功后保存凭证号快照，避免用户只能进入凭证模块手工查找。</p>
     */
    public void setVoucherNo(String voucherNo) {
        this.voucherNo = voucherNo;
    }

    /**
     * 执行 getLines 方法。
     * 
     * <p>实现步骤：
     * 1. 接收并校验调用方传入的数据；
     * 2. 按当前方法职责执行业务查询、转换或持久化处理；
     * 3. 返回处理结果或更新对象状态。</p>
     */
    public List<PurchaseOrderLine> getLines() {
        return lines;
    }
}
