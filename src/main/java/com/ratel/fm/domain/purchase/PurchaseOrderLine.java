package com.ratel.fm.domain.purchase;

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
 * 采购订单明细。
 *
 * <p>每条明细记录一个物料的采购数量、单价和金额。</p>
 *
 * <p>开发组织：ratel；开发人员：WenZhang；联系方式：18782945613。</p>
 *
 * @author ratel
 */
@Entity
@Table(name = "fm_purchase_order_lines")
@Comment("采购订单明细表，记录物料采购数量、单价和金额")
public class PurchaseOrderLine extends BaseEntity {

    /** 所属采购订单主表。 */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "purchase_order_id", nullable = false)
    @Comment("所属采购订单主表ID")
    private PurchaseOrder purchaseOrder;

    /** 明细行号，用于保持录入顺序。 */
    @Column(nullable = false)
    @Comment("明细行号")
    private int lineNo;

    /** 物料编码。 */
    @Column(nullable = false, length = 80)
    @Comment("物料编码")
    private String itemCode;

    /** 物料名称。 */
    @Column(nullable = false, length = 160)
    @Comment("物料名称")
    private String itemName;

    /** 规格型号，保存物料基础资料快照。 */
    @Column(length = 160)
    @Comment("规格型号")
    private String specification;

    /** 计量单位。 */
    @Column(length = 60)
    @Comment("计量单位")
    private String unitName;

    /** 采购数量，保留 4 位小数以支持重量、体积等非整数采购。 */
    @Column(nullable = false, precision = 18, scale = 4)
    @Comment("采购数量")
    private BigDecimal quantity;

    /** 不含税或业务口径下的采购单价，保留 8 位小数。 */
    @Column(precision = 26, scale = 8)
    @Comment("采购单价，保留8位小数")
    private BigDecimal unitPrice;

    /** 行金额，按 quantity * unitPrice 计算并四舍五入到 8 位小数。 */
    @Column(precision = 26, scale = 8)
    @Comment("采购明细金额，保留8位小数")
    private BigDecimal amount;

    /** 税率，按小数保存，如 0.13 表示 13%。 */
    @Column(precision = 10, scale = 6)
    @Comment("税率")
    private BigDecimal taxRate = BigDecimal.ZERO;

    /** 税额。 */
    @Column(precision = 26, scale = 8)
    @Comment("税额")
    private BigDecimal taxAmount = BigDecimal.ZERO;

    /** 价税合计金额。 */
    @Column(precision = 26, scale = 8)
    @Comment("价税合计金额")
    private BigDecimal amountWithTax = BigDecimal.ZERO;

    /** 计划到货日期。 */
    @Comment("计划到货日期")
    private java.time.LocalDate plannedArrivalDate;

    /** 收货仓库。 */
    @Column(length = 120)
    @Comment("收货仓库")
    private String receiveWarehouse;

    /** 明细币种编码，来自币种字典。每条采购明细独立保存币种。 */
    @Column(length = 20)
    @Comment("采购明细币种编码，来自基础信息币种字典")
    private String currencyCode = "CNY";

    /** 明细币种名称快照，避免字典变更影响历史采购记录展示。 */
    @Column(length = 80)
    @Comment("采购明细币种名称快照")
    private String currencyName = "人民币";

    /** 采购发生时该币种折人民币汇率，后续统计使用该快照。 */
    @Column(precision = 18, scale = 8)
    @Comment("采购明细发生时该币种折人民币汇率")
    private BigDecimal exchangeRateToCny = BigDecimal.ONE.setScale(8);

    /** 采购单价按明细保存的当时汇率折算出的人民币金额，保留 8 位小数。 */
    @Column(nullable = false, precision = 26, scale = 8)
    @Comment("采购单价折人民币金额，保留8位小数")
    private BigDecimal unitPriceCny;

    /** 行金额按明细保存的当时汇率折算出的人民币金额，保留 8 位小数。 */
    @Column(nullable = false, precision = 26, scale = 8)
    @Comment("采购明细折人民币金额，保留8位小数")
    private BigDecimal amountCny;

    /**
     * 执行 getPurchaseOrder 方法。
     * 
     * <p>实现步骤：
     * 1. 接收并校验调用方传入的数据；
     * 2. 按当前方法职责执行业务查询、转换或持久化处理；
     * 3. 返回处理结果或更新对象状态。</p>
     */
    public PurchaseOrder getPurchaseOrder() {
        return purchaseOrder;
    }

    /**
     * 执行 setPurchaseOrder 方法。
     * 
     * <p>实现步骤：
     * 1. 接收并校验调用方传入的数据；
     * 2. 按当前方法职责执行业务查询、转换或持久化处理；
     * 3. 返回处理结果或更新对象状态。</p>
     */
    public void setPurchaseOrder(PurchaseOrder purchaseOrder) {
        this.purchaseOrder = purchaseOrder;
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
     * 执行 getItemCode 方法。
     * 
     * <p>实现步骤：
     * 1. 接收并校验调用方传入的数据；
     * 2. 按当前方法职责执行业务查询、转换或持久化处理；
     * 3. 返回处理结果或更新对象状态。</p>
     */
    public String getItemCode() {
        return itemCode;
    }

    /**
     * 执行 setItemCode 方法。
     * 
     * <p>实现步骤：
     * 1. 接收并校验调用方传入的数据；
     * 2. 按当前方法职责执行业务查询、转换或持久化处理；
     * 3. 返回处理结果或更新对象状态。</p>
     */
    public void setItemCode(String itemCode) {
        this.itemCode = itemCode;
    }

    /**
     * 执行 getItemName 方法。
     * 
     * <p>实现步骤：
     * 1. 接收并校验调用方传入的数据；
     * 2. 按当前方法职责执行业务查询、转换或持久化处理；
     * 3. 返回处理结果或更新对象状态。</p>
     */
    public String getItemName() {
        return itemName;
    }

    /**
     * 执行 setItemName 方法。
     * 
     * <p>实现步骤：
     * 1. 接收并校验调用方传入的数据；
     * 2. 按当前方法职责执行业务查询、转换或持久化处理；
     * 3. 返回处理结果或更新对象状态。</p>
     */
    public void setItemName(String itemName) {
        this.itemName = itemName;
    }

    /**
     * 执行 getSpecification 方法。
     * 
     * <p>实现步骤：
     * 1. 接收并校验调用方传入的数据；
     * 2. 按当前方法职责执行业务查询、转换或持久化处理；
     * 3. 返回处理结果或更新对象状态。</p>
     */
    public String getSpecification() {
        return specification;
    }

    /**
     * 执行 setSpecification 方法。
     * 
     * <p>实现步骤：
     * 1. 接收并校验调用方传入的数据；
     * 2. 按当前方法职责执行业务查询、转换或持久化处理；
     * 3. 返回处理结果或更新对象状态。</p>
     */
    public void setSpecification(String specification) {
        this.specification = specification;
    }

    /**
     * 执行 getUnitName 方法。
     * 
     * <p>实现步骤：
     * 1. 接收并校验调用方传入的数据；
     * 2. 按当前方法职责执行业务查询、转换或持久化处理；
     * 3. 返回处理结果或更新对象状态。</p>
     */
    public String getUnitName() {
        return unitName;
    }

    /**
     * 执行 setUnitName 方法。
     * 
     * <p>实现步骤：
     * 1. 接收并校验调用方传入的数据；
     * 2. 按当前方法职责执行业务查询、转换或持久化处理；
     * 3. 返回处理结果或更新对象状态。</p>
     */
    public void setUnitName(String unitName) {
        this.unitName = unitName;
    }

    /**
     * 执行 getQuantity 方法。
     * 
     * <p>实现步骤：
     * 1. 接收并校验调用方传入的数据；
     * 2. 按当前方法职责执行业务查询、转换或持久化处理；
     * 3. 返回处理结果或更新对象状态。</p>
     */
    public BigDecimal getQuantity() {
        return quantity;
    }

    /**
     * 执行 setQuantity 方法。
     * 
     * <p>实现步骤：
     * 1. 接收并校验调用方传入的数据；
     * 2. 按当前方法职责执行业务查询、转换或持久化处理；
     * 3. 返回处理结果或更新对象状态。</p>
     */
    public void setQuantity(BigDecimal quantity) {
        this.quantity = quantity;
    }

    /**
     * 执行 getUnitPrice 方法。
     * 
     * <p>实现步骤：
     * 1. 接收并校验调用方传入的数据；
     * 2. 按当前方法职责执行业务查询、转换或持久化处理；
     * 3. 返回处理结果或更新对象状态。</p>
     */
    public BigDecimal getUnitPrice() {
        return unitPrice;
    }

    /**
     * 执行 setUnitPrice 方法。
     * 
     * <p>实现步骤：
     * 1. 接收并校验调用方传入的数据；
     * 2. 按当前方法职责执行业务查询、转换或持久化处理；
     * 3. 返回处理结果或更新对象状态。</p>
     */
    public void setUnitPrice(BigDecimal unitPrice) {
        this.unitPrice = unitPrice;
    }

    /**
     * 执行 getAmount 方法。
     * 
     * <p>实现步骤：
     * 1. 接收并校验调用方传入的数据；
     * 2. 按当前方法职责执行业务查询、转换或持久化处理；
     * 3. 返回处理结果或更新对象状态。</p>
     */
    public BigDecimal getAmount() {
        return amount;
    }

    /**
     * 执行 setAmount 方法。
     * 
     * <p>实现步骤：
     * 1. 接收并校验调用方传入的数据；
     * 2. 按当前方法职责执行业务查询、转换或持久化处理；
     * 3. 返回处理结果或更新对象状态。</p>
     */
    public void setAmount(BigDecimal amount) {
        this.amount = amount;
    }

    /**
     * 执行 getTaxRate 方法。
     * 
     * <p>实现步骤：
     * 1. 接收并校验调用方传入的数据；
     * 2. 按当前方法职责执行业务查询、转换或持久化处理；
     * 3. 返回处理结果或更新对象状态。</p>
     */
    public BigDecimal getTaxRate() {
        return taxRate;
    }

    /**
     * 执行 setTaxRate 方法。
     * 
     * <p>实现步骤：
     * 1. 接收并校验调用方传入的数据；
     * 2. 按当前方法职责执行业务查询、转换或持久化处理；
     * 3. 返回处理结果或更新对象状态。</p>
     */
    public void setTaxRate(BigDecimal taxRate) {
        this.taxRate = taxRate;
    }

    /**
     * 执行 getTaxAmount 方法。
     * 
     * <p>实现步骤：
     * 1. 接收并校验调用方传入的数据；
     * 2. 按当前方法职责执行业务查询、转换或持久化处理；
     * 3. 返回处理结果或更新对象状态。</p>
     */
    public BigDecimal getTaxAmount() {
        return taxAmount;
    }

    /**
     * 执行 setTaxAmount 方法。
     * 
     * <p>实现步骤：
     * 1. 接收并校验调用方传入的数据；
     * 2. 按当前方法职责执行业务查询、转换或持久化处理；
     * 3. 返回处理结果或更新对象状态。</p>
     */
    public void setTaxAmount(BigDecimal taxAmount) {
        this.taxAmount = taxAmount;
    }

    /**
     * 执行 getAmountWithTax 方法。
     * 
     * <p>实现步骤：
     * 1. 接收并校验调用方传入的数据；
     * 2. 按当前方法职责执行业务查询、转换或持久化处理；
     * 3. 返回处理结果或更新对象状态。</p>
     */
    public BigDecimal getAmountWithTax() {
        return amountWithTax;
    }

    /**
     * 执行 setAmountWithTax 方法。
     * 
     * <p>实现步骤：
     * 1. 接收并校验调用方传入的数据；
     * 2. 按当前方法职责执行业务查询、转换或持久化处理；
     * 3. 返回处理结果或更新对象状态。</p>
     */
    public void setAmountWithTax(BigDecimal amountWithTax) {
        this.amountWithTax = amountWithTax;
    }

    /**
     * 执行 getPlannedArrivalDate 方法。
     * 
     * <p>实现步骤：
     * 1. 接收并校验调用方传入的数据；
     * 2. 按当前方法职责执行业务查询、转换或持久化处理；
     * 3. 返回处理结果或更新对象状态。</p>
     */
    public java.time.LocalDate getPlannedArrivalDate() {
        return plannedArrivalDate;
    }

    /**
     * 执行 setPlannedArrivalDate 方法。
     * 
     * <p>实现步骤：
     * 1. 接收并校验调用方传入的数据；
     * 2. 按当前方法职责执行业务查询、转换或持久化处理；
     * 3. 返回处理结果或更新对象状态。</p>
     */
    public void setPlannedArrivalDate(java.time.LocalDate plannedArrivalDate) {
        this.plannedArrivalDate = plannedArrivalDate;
    }

    /**
     * 执行 getReceiveWarehouse 方法。
     * 
     * <p>实现步骤：
     * 1. 接收并校验调用方传入的数据；
     * 2. 按当前方法职责执行业务查询、转换或持久化处理；
     * 3. 返回处理结果或更新对象状态。</p>
     */
    public String getReceiveWarehouse() {
        return receiveWarehouse;
    }

    /**
     * 执行 setReceiveWarehouse 方法。
     * 
     * <p>实现步骤：
     * 1. 接收并校验调用方传入的数据；
     * 2. 按当前方法职责执行业务查询、转换或持久化处理；
     * 3. 返回处理结果或更新对象状态。</p>
     */
    public void setReceiveWarehouse(String receiveWarehouse) {
        this.receiveWarehouse = receiveWarehouse;
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
     * 执行 getUnitPriceCny 方法。
     * 
     * <p>实现步骤：
     * 1. 接收并校验调用方传入的数据；
     * 2. 按当前方法职责执行业务查询、转换或持久化处理；
     * 3. 返回处理结果或更新对象状态。</p>
     */
    public BigDecimal getUnitPriceCny() {
        return unitPriceCny;
    }

    /**
     * 执行 setUnitPriceCny 方法。
     * 
     * <p>实现步骤：
     * 1. 接收并校验调用方传入的数据；
     * 2. 按当前方法职责执行业务查询、转换或持久化处理；
     * 3. 返回处理结果或更新对象状态。</p>
     */
    public void setUnitPriceCny(BigDecimal unitPriceCny) {
        this.unitPriceCny = unitPriceCny;
    }

    /**
     * 执行 getAmountCny 方法。
     * 
     * <p>实现步骤：
     * 1. 接收并校验调用方传入的数据；
     * 2. 按当前方法职责执行业务查询、转换或持久化处理；
     * 3. 返回处理结果或更新对象状态。</p>
     */
    public BigDecimal getAmountCny() {
        return amountCny;
    }

    /**
     * 执行 setAmountCny 方法。
     * 
     * <p>实现步骤：
     * 1. 接收并校验调用方传入的数据；
     * 2. 按当前方法职责执行业务查询、转换或持久化处理；
     * 3. 返回处理结果或更新对象状态。</p>
     */
    public void setAmountCny(BigDecimal amountCny) {
        this.amountCny = amountCny;
    }
}
