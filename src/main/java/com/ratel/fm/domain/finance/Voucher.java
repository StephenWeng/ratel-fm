package com.ratel.fm.domain.finance;

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
 * 财务凭证主表。
 *
 * <p>凭证承载一笔复式记账业务，必须通过分录保证借贷平衡。</p>
 *
 * <p>开发组织：ratel；开发人员：WenZhang；联系方式：18782945613。</p>
 *
 * @author ratel
 */
@Entity
@Table(name = "fm_vouchers")
@Comment("财务凭证主表，记录复式记账业务的凭证头、状态和借贷合计")
public class Voucher extends BaseEntity {

    /** 凭证编号，全系统唯一，按业务日期生成。 */
    @Column(nullable = false, unique = true, length = 60)
    @Comment("凭证编号，全系统唯一")
    private String voucherNo;

    /** 所属公司字典编码，即账套编码，凭证及其分录按该字段隔离。 */
    @Column(nullable = false, length = 80)
    @Comment("所属公司字典编码，作为凭证账套隔离标识")
    private String organizationCode;

    /** 凭证日期，决定凭证归属会计期间和报表统计区间。 */
    @Column(nullable = false)
    @Comment("凭证日期")
    private LocalDate voucherDate;

    /** 所属年月，格式 yyyy-MM，用于标记该账目归属的业务发生月份。 */
    @Column(length = 7)
    @Comment("所属年月，格式 yyyy-MM")
    private String belongMonth;

    /** 项目字典编码，来自基础信息项目字典，用于按项目区分凭证归属。 */
    @Column(length = 80)
    @Comment("项目字典编码")
    private String projectCode;

    /** 项目名称快照，避免项目字典名称调整影响历史凭证展示。 */
    @Column(length = 160)
    @Comment("项目名称快照")
    private String projectName;

    /** 凭证摘要，概括本次记账业务内容。 */
    @Column(nullable = false, length = 200)
    @Comment("凭证摘要")
    private String summary;

    /** 凭证状态：草稿、已过账、已作废。只有已过账凭证进入报表统计。 */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    @Comment("凭证状态")
    private VoucherStatus status = VoucherStatus.DRAFT;

    /** 借方合计金额，由分录明细汇总得到，保留 8 位小数。 */
    @Column(nullable = false, precision = 26, scale = 8)
    @Comment("借方合计金额，保留8位小数")
    private BigDecimal totalDebit = BigDecimal.ZERO;

    /** 贷方合计金额，由分录明细汇总得到，保留 8 位小数，必须与借方合计相等。 */
    @Column(nullable = false, precision = 26, scale = 8)
    @Comment("贷方合计金额，保留8位小数")
    private BigDecimal totalCredit = BigDecimal.ZERO;

    /** 币种编码，来自基础信息币种字典，默认人民币 CNY。 */
    @Column(length = 20)
    @Comment("币种编码，来自基础信息币种字典")
    private String currencyCode = "CNY";

    /** 币种名称，保存业务发生时的字典名称快照。 */
    @Column(length = 80)
    @Comment("币种名称快照")
    private String currencyName = "人民币";

    /** 业务发生时该币种折人民币汇率，人民币固定为 1。 */
    @Column(precision = 18, scale = 8)
    @Comment("业务发生时该币种折人民币汇率")
    private BigDecimal exchangeRateToCny = BigDecimal.ONE;

    /** 借方合计折人民币金额，按业务发生时汇率计算并保留 8 位小数。 */
    @Column(precision = 26, scale = 8)
    @Comment("借方合计折人民币金额，保留8位小数")
    private BigDecimal totalDebitCny = BigDecimal.ZERO;

    /** 贷方合计折人民币金额，按业务发生时汇率计算并保留 8 位小数。 */
    @Column(precision = 26, scale = 8)
    @Comment("贷方合计折人民币金额，保留8位小数")
    private BigDecimal totalCreditCny = BigDecimal.ZERO;

    /** 创建凭证的登录账号。 */
    @Column(nullable = false, length = 80)
    @Comment("创建凭证的登录账号")
    private String createdBy;

    /** 过账人员账号。草稿状态为空，过账时写入当前登录人。 */
    @Column(length = 80)
    @Comment("过账人员账号")
    private String postedBy;

    /** 来源业务单号，如采购单号、物流单号，便于业务追溯。 */
    @Column(length = 300)
    @Comment("来源业务单号")
    private String sourceBizNo;

    /** 来源业务类型，标识凭证由哪个业务模块生成，手工凭证为空。 */
    @Enumerated(EnumType.STRING)
    @Column(length = 40)
    @Comment("来源业务类型")
    private AccountingSourceType sourceType;

    /** 来源业务主键，配合来源业务类型精确定位原始单据。 */
    @Column
    @Comment("来源业务主键")
    private Long sourceId;

    /** 来源业务标题，保存生成凭证时的业务标题快照。 */
    @Column(length = 160)
    @Comment("来源业务标题快照")
    private String sourceTitle;

    /** 凭证分录集合。使用 orphanRemoval 保证替换分录时删除旧明细。 */
    @OneToMany(mappedBy = "voucher", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("lineNo ASC")
    private List<VoucherLine> lines = new ArrayList<>();

    /**
     * 执行 getVoucherNo 方法。
     * 
     * <p>实现步骤：
     * 1. 接收并校验调用方传入的数据；
     * 2. 按当前方法职责执行业务查询、转换或持久化处理；
     * 3. 返回处理结果或更新对象状态。</p>
     */
    public String getVoucherNo() {
        return voucherNo;
    }

    /**
     * 执行 setVoucherNo 方法。
     * 
     * <p>实现步骤：
     * 1. 接收并校验调用方传入的数据；
     * 2. 按当前方法职责执行业务查询、转换或持久化处理；
     * 3. 返回处理结果或更新对象状态。</p>
     */
    public void setVoucherNo(String voucherNo) {
        this.voucherNo = voucherNo;
    }

    /**
     * 获取凭证所属公司编码。
     *
     * <p>实现步骤：直接返回凭证创建时写入的账套编码，列表、过账、作废和报表统计均按该字段隔离。</p>
     */
    public String getOrganizationCode() {
        return organizationCode;
    }

    /**
     * 设置凭证所属公司编码。
     *
     * <p>实现步骤：新增凭证时写入当前登录公司的字典编码，后续接口只允许访问当前公司凭证。</p>
     */
    public void setOrganizationCode(String organizationCode) {
        this.organizationCode = organizationCode;
    }

    /**
     * 执行 getVoucherDate 方法。
     * 
     * <p>实现步骤：
     * 1. 接收并校验调用方传入的数据；
     * 2. 按当前方法职责执行业务查询、转换或持久化处理；
     * 3. 返回处理结果或更新对象状态。</p>
     */
    public LocalDate getVoucherDate() {
        return voucherDate;
    }

    /**
     * 执行 setVoucherDate 方法。
     * 
     * <p>实现步骤：
     * 1. 接收并校验调用方传入的数据；
     * 2. 按当前方法职责执行业务查询、转换或持久化处理；
     * 3. 返回处理结果或更新对象状态。</p>
     */
    public void setVoucherDate(LocalDate voucherDate) {
        this.voucherDate = voucherDate;
    }

    /**
     * 执行 getBelongMonth 方法。
     * 
     * <p>实现步骤：
     * 1. 接收并校验调用方传入的数据；
     * 2. 按当前方法职责执行业务查询、转换或持久化处理；
     * 3. 返回处理结果或更新对象状态。</p>
     */
    public String getBelongMonth() {
        return belongMonth;
    }

    /**
     * 执行 setBelongMonth 方法。
     * 
     * <p>实现步骤：
     * 1. 接收并校验调用方传入的数据；
     * 2. 按当前方法职责执行业务查询、转换或持久化处理；
     * 3. 返回处理结果或更新对象状态。</p>
     */
    public void setBelongMonth(String belongMonth) {
        this.belongMonth = belongMonth;
    }

    /**
     * 获取项目字典编码。
     *
     * <p>实现步骤：直接返回凭证保存的项目编码快照，列表筛选和报表统计按该字段等值匹配。</p>
     */
    public String getProjectCode() {
        return projectCode;
    }

    /**
     * 设置项目字典编码。
     *
     * <p>实现步骤：接收前端项目下拉框传入的字典编码，并保存到凭证主表。</p>
     */
    public void setProjectCode(String projectCode) {
        this.projectCode = projectCode;
    }

    /**
     * 获取项目名称快照。
     *
     * <p>实现步骤：直接返回凭证创建或修改时保存的项目名称，避免字典改名影响历史流水。</p>
     */
    public String getProjectName() {
        return projectName;
    }

    /**
     * 设置项目名称快照。
     *
     * <p>实现步骤：接收前端随项目编码一并传入的项目名称并保存，供列表和查看流水展示。</p>
     */
    public void setProjectName(String projectName) {
        this.projectName = projectName;
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
     * 执行 getStatus 方法。
     * 
     * <p>实现步骤：
     * 1. 接收并校验调用方传入的数据；
     * 2. 按当前方法职责执行业务查询、转换或持久化处理；
     * 3. 返回处理结果或更新对象状态。</p>
     */
    public VoucherStatus getStatus() {
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
    public void setStatus(VoucherStatus status) {
        this.status = status;
    }

    /**
     * 执行 getTotalDebit 方法。
     * 
     * <p>实现步骤：
     * 1. 接收并校验调用方传入的数据；
     * 2. 按当前方法职责执行业务查询、转换或持久化处理；
     * 3. 返回处理结果或更新对象状态。</p>
     */
    public BigDecimal getTotalDebit() {
        return totalDebit;
    }

    /**
     * 执行 setTotalDebit 方法。
     * 
     * <p>实现步骤：
     * 1. 接收并校验调用方传入的数据；
     * 2. 按当前方法职责执行业务查询、转换或持久化处理；
     * 3. 返回处理结果或更新对象状态。</p>
     */
    public void setTotalDebit(BigDecimal totalDebit) {
        this.totalDebit = totalDebit;
    }

    /**
     * 执行 getTotalCredit 方法。
     * 
     * <p>实现步骤：
     * 1. 接收并校验调用方传入的数据；
     * 2. 按当前方法职责执行业务查询、转换或持久化处理；
     * 3. 返回处理结果或更新对象状态。</p>
     */
    public BigDecimal getTotalCredit() {
        return totalCredit;
    }

    /**
     * 执行 setTotalCredit 方法。
     * 
     * <p>实现步骤：
     * 1. 接收并校验调用方传入的数据；
     * 2. 按当前方法职责执行业务查询、转换或持久化处理；
     * 3. 返回处理结果或更新对象状态。</p>
     */
    public void setTotalCredit(BigDecimal totalCredit) {
        this.totalCredit = totalCredit;
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
     * 执行 getTotalDebitCny 方法。
     * 
     * <p>实现步骤：
     * 1. 接收并校验调用方传入的数据；
     * 2. 按当前方法职责执行业务查询、转换或持久化处理；
     * 3. 返回处理结果或更新对象状态。</p>
     */
    public BigDecimal getTotalDebitCny() {
        return totalDebitCny;
    }

    /**
     * 执行 setTotalDebitCny 方法。
     * 
     * <p>实现步骤：
     * 1. 接收并校验调用方传入的数据；
     * 2. 按当前方法职责执行业务查询、转换或持久化处理；
     * 3. 返回处理结果或更新对象状态。</p>
     */
    public void setTotalDebitCny(BigDecimal totalDebitCny) {
        this.totalDebitCny = totalDebitCny;
    }

    /**
     * 执行 getTotalCreditCny 方法。
     * 
     * <p>实现步骤：
     * 1. 接收并校验调用方传入的数据；
     * 2. 按当前方法职责执行业务查询、转换或持久化处理；
     * 3. 返回处理结果或更新对象状态。</p>
     */
    public BigDecimal getTotalCreditCny() {
        return totalCreditCny;
    }

    /**
     * 执行 setTotalCreditCny 方法。
     * 
     * <p>实现步骤：
     * 1. 接收并校验调用方传入的数据；
     * 2. 按当前方法职责执行业务查询、转换或持久化处理；
     * 3. 返回处理结果或更新对象状态。</p>
     */
    public void setTotalCreditCny(BigDecimal totalCreditCny) {
        this.totalCreditCny = totalCreditCny;
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
     * 执行 getPostedBy 方法。
     * 
     * <p>实现步骤：
     * 1. 接收并校验调用方传入的数据；
     * 2. 按当前方法职责执行业务查询、转换或持久化处理；
     * 3. 返回处理结果或更新对象状态。</p>
     */
    public String getPostedBy() {
        return postedBy;
    }

    /**
     * 执行 setPostedBy 方法。
     * 
     * <p>实现步骤：
     * 1. 接收并校验调用方传入的数据；
     * 2. 按当前方法职责执行业务查询、转换或持久化处理；
     * 3. 返回处理结果或更新对象状态。</p>
     */
    public void setPostedBy(String postedBy) {
        this.postedBy = postedBy;
    }

    /**
     * 执行 getSourceBizNo 方法。
     * 
     * <p>实现步骤：
     * 1. 接收并校验调用方传入的数据；
     * 2. 按当前方法职责执行业务查询、转换或持久化处理；
     * 3. 返回处理结果或更新对象状态。</p>
     */
    public String getSourceBizNo() {
        return sourceBizNo;
    }

    /**
     * 执行 setSourceBizNo 方法。
     * 
     * <p>实现步骤：
     * 1. 接收并校验调用方传入的数据；
     * 2. 按当前方法职责执行业务查询、转换或持久化处理；
     * 3. 返回处理结果或更新对象状态。</p>
     */
    public void setSourceBizNo(String sourceBizNo) {
        this.sourceBizNo = sourceBizNo;
    }

    /**
     * 获取凭证来源业务类型。
     *
     * <p>实现步骤：返回自动制证时写入的来源模块类型，手工凭证返回空。</p>
     */
    public AccountingSourceType getSourceType() {
        return sourceType;
    }

    /**
     * 设置凭证来源业务类型。
     *
     * <p>实现步骤：会计平台自动制证时写入来源模块类型，供凭证反向查看原始单据。</p>
     */
    public void setSourceType(AccountingSourceType sourceType) {
        this.sourceType = sourceType;
    }

    /**
     * 获取凭证来源业务主键。
     *
     * <p>实现步骤：返回来源单据主键，配合来源类型精确定位采购、应收应付、库存或出纳单据。</p>
     */
    public Long getSourceId() {
        return sourceId;
    }

    /**
     * 设置凭证来源业务主键。
     *
     * <p>实现步骤：会计平台自动制证时保存来源单据主键，避免仅凭单号反查产生歧义。</p>
     */
    public void setSourceId(Long sourceId) {
        this.sourceId = sourceId;
    }

    /**
     * 获取凭证来源标题。
     *
     * <p>实现步骤：返回生成凭证时保存的来源标题快照，用于列表和查看来源弹窗展示。</p>
     */
    public String getSourceTitle() {
        return sourceTitle;
    }

    /**
     * 设置凭证来源标题。
     *
     * <p>实现步骤：自动制证时保存来源模块和单号组成的可读标题，基础数据变更不影响历史展示。</p>
     */
    public void setSourceTitle(String sourceTitle) {
        this.sourceTitle = sourceTitle;
    }

    /**
     * 执行 getLines 方法。
     * 
     * <p>实现步骤：
     * 1. 接收并校验调用方传入的数据；
     * 2. 按当前方法职责执行业务查询、转换或持久化处理；
     * 3. 返回处理结果或更新对象状态。</p>
     */
    public List<VoucherLine> getLines() {
        return lines;
    }
}
