package com.ratel.fm.domain.inventory;

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
 * 库存台账流水。
 *
 * <p>记录入库、出库、调拨、盘点等库存数量变化，当前为二期基础实现。</p>
 *
 * <p>开发组织：ratel；开发人员：WenZhang；联系方式：18782945613。</p>
 *
 * @author ratel
 */
@Entity
@Table(name = "fm_inventory_ledgers")
@Comment("库存台账流水表，记录入库、出库、调拨和盘点等库存数量变化")
public class InventoryLedger extends BaseEntity {

    /** 库存流水号，同一所属公司内唯一，按业务日期和公司维度生成。 */
    @Column(nullable = false, length = 60)
    @Comment("库存流水号，同一所属公司内唯一")
    private String movementNo;

    /** 库存变动类型：入库、出库、调拨、盘点。 */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    @Comment("库存变动类型")
    private InventoryMovementType movementType;

    /** 库存变动日期，用于排序和账龄统计。 */
    @Column(nullable = false)
    @Comment("库存变动日期")
    private LocalDate movementDate;

    /** 物料编码。 */
    @Column(nullable = false, length = 80)
    @Comment("物料编码")
    private String itemCode;

    /** 物料名称。 */
    @Column(nullable = false, length = 160)
    @Comment("物料名称")
    private String itemName;

    /** 项目字典编码，来自基础信息项目字典，用于按项目归集库存变动。 */
    @Column(length = 80)
    @Comment("项目字典编码")
    private String projectCode;

    /** 项目名称快照，避免项目字典名称调整影响历史库存流水展示。 */
    @Column(length = 160)
    @Comment("项目名称快照")
    private String projectName;

    /** 规格型号，保存物料基础资料快照。 */
    @Column(length = 160)
    @Comment("规格型号")
    private String specification;

    /** 库存组织。 */
    @Column(length = 120)
    @Comment("库存组织")
    private String stockOrganization;

    /** 货主。 */
    @Column(length = 120)
    @Comment("货主")
    private String ownerName;

    /** 库存单位。 */
    @Column(length = 60)
    @Comment("库存单位")
    private String unitName;

    /** 批号。 */
    @Column(length = 120)
    @Comment("批号")
    private String batchNo;

    /** 变动数量，保留 4 位小数。 */
    @Column(nullable = false, precision = 18, scale = 4)
    @Comment("库存变动数量")
    private BigDecimal quantity;

    /** 来源仓库。入库可为空，出库/调拨通常必填。 */
    @Column(length = 80)
    @Comment("来源仓库")
    private String fromWarehouse;

    /** 目标仓库。出库可为空，入库/调拨通常必填。 */
    @Column(length = 80)
    @Comment("目标仓库")
    private String toWarehouse;

    /** 关联业务单号，如采购单、销售单或盘点单。 */
    @Column(length = 300)
    @Comment("关联业务单号")
    private String relatedBizNo;

    /** 来源单据类型。 */
    @Column(length = 80)
    @Comment("来源单据类型")
    private String sourceBillType;

    /** 库存流水备注，支持长文本说明。 */
    @Column(length = 2000)
    @Comment("库存流水备注")
    private String remark;

    /** 所属公司字典编码，即账套编码，库存流水按该字段隔离。 */
    @Column(length = 80)
    @Comment("所属公司字典编码，作为库存台账账套隔离标识")
    private String organizationCode;

    /** 该库存流水通过会计平台生成的凭证主键。为空表示尚未制证。 */
    @Column
    @Comment("关联凭证主键")
    private Long voucherId;

    /** 该库存流水通过会计平台生成的凭证号，用于列表展示和在线凭证跳转。 */
    @Column(length = 60)
    @Comment("关联凭证号")
    private String voucherNo;

    /**
     * 执行 getMovementNo 方法。
     * 
     * <p>实现步骤：
     * 1. 接收并校验调用方传入的数据；
     * 2. 按当前方法职责执行业务查询、转换或持久化处理；
     * 3. 返回处理结果或更新对象状态。</p>
     */
    public String getMovementNo() { return movementNo; }
    /**
     * 执行 setMovementNo 方法。
     * 
     * <p>实现步骤：
     * 1. 接收并校验调用方传入的数据；
     * 2. 按当前方法职责执行业务查询、转换或持久化处理；
     * 3. 返回处理结果或更新对象状态。</p>
     */
    public void setMovementNo(String movementNo) { this.movementNo = movementNo; }
    /**
     * 执行 getMovementType 方法。
     * 
     * <p>实现步骤：
     * 1. 接收并校验调用方传入的数据；
     * 2. 按当前方法职责执行业务查询、转换或持久化处理；
     * 3. 返回处理结果或更新对象状态。</p>
     */
    public InventoryMovementType getMovementType() { return movementType; }
    /**
     * 执行 setMovementType 方法。
     * 
     * <p>实现步骤：
     * 1. 接收并校验调用方传入的数据；
     * 2. 按当前方法职责执行业务查询、转换或持久化处理；
     * 3. 返回处理结果或更新对象状态。</p>
     */
    public void setMovementType(InventoryMovementType movementType) { this.movementType = movementType; }
    /**
     * 执行 getMovementDate 方法。
     * 
     * <p>实现步骤：
     * 1. 接收并校验调用方传入的数据；
     * 2. 按当前方法职责执行业务查询、转换或持久化处理；
     * 3. 返回处理结果或更新对象状态。</p>
     */
    public LocalDate getMovementDate() { return movementDate; }
    /**
     * 执行 setMovementDate 方法。
     * 
     * <p>实现步骤：
     * 1. 接收并校验调用方传入的数据；
     * 2. 按当前方法职责执行业务查询、转换或持久化处理；
     * 3. 返回处理结果或更新对象状态。</p>
     */
    public void setMovementDate(LocalDate movementDate) { this.movementDate = movementDate; }
    /**
     * 执行 getItemCode 方法。
     * 
     * <p>实现步骤：
     * 1. 接收并校验调用方传入的数据；
     * 2. 按当前方法职责执行业务查询、转换或持久化处理；
     * 3. 返回处理结果或更新对象状态。</p>
     */
    public String getItemCode() { return itemCode; }
    /**
     * 执行 setItemCode 方法。
     * 
     * <p>实现步骤：
     * 1. 接收并校验调用方传入的数据；
     * 2. 按当前方法职责执行业务查询、转换或持久化处理；
     * 3. 返回处理结果或更新对象状态。</p>
     */
    public void setItemCode(String itemCode) { this.itemCode = itemCode; }
    /**
     * 执行 getItemName 方法。
     * 
     * <p>实现步骤：
     * 1. 接收并校验调用方传入的数据；
     * 2. 按当前方法职责执行业务查询、转换或持久化处理；
     * 3. 返回处理结果或更新对象状态。</p>
     */
    public String getItemName() { return itemName; }
    /**
     * 执行 setItemName 方法。
     * 
     * <p>实现步骤：
     * 1. 接收并校验调用方传入的数据；
     * 2. 按当前方法职责执行业务查询、转换或持久化处理；
     * 3. 返回处理结果或更新对象状态。</p>
     */
    public void setItemName(String itemName) { this.itemName = itemName; }

    /**
     * 获取项目字典编码。
     *
     * <p>实现步骤：返回库存流水保存的项目编码快照，用于列表筛选和库存业务追溯。</p>
     */
    public String getProjectCode() { return projectCode; }

    /**
     * 设置项目字典编码。
     *
     * <p>实现步骤：保存库存表单选择的项目字典编码，后续可按项目归集库存变动。</p>
     */
    public void setProjectCode(String projectCode) { this.projectCode = projectCode; }

    /**
     * 获取项目名称快照。
     *
     * <p>实现步骤：返回库存流水创建时保存的项目名称，避免字典改名影响历史展示。</p>
     */
    public String getProjectName() { return projectName; }

    /**
     * 设置项目名称快照。
     *
     * <p>实现步骤：保存项目字典名称快照，供列表、导出和查看流水展示。</p>
     */
    public void setProjectName(String projectName) { this.projectName = projectName; }

    /**
     * 执行 getSpecification 方法。
     * 
     * <p>实现步骤：
     * 1. 接收并校验调用方传入的数据；
     * 2. 按当前方法职责执行业务查询、转换或持久化处理；
     * 3. 返回处理结果或更新对象状态。</p>
     */
    public String getSpecification() { return specification; }
    /**
     * 执行 setSpecification 方法。
     * 
     * <p>实现步骤：
     * 1. 接收并校验调用方传入的数据；
     * 2. 按当前方法职责执行业务查询、转换或持久化处理；
     * 3. 返回处理结果或更新对象状态。</p>
     */
    public void setSpecification(String specification) { this.specification = specification; }
    /**
     * 执行 getStockOrganization 方法。
     * 
     * <p>实现步骤：
     * 1. 接收并校验调用方传入的数据；
     * 2. 按当前方法职责执行业务查询、转换或持久化处理；
     * 3. 返回处理结果或更新对象状态。</p>
     */
    public String getStockOrganization() { return stockOrganization; }
    /**
     * 执行 setStockOrganization 方法。
     * 
     * <p>实现步骤：
     * 1. 接收并校验调用方传入的数据；
     * 2. 按当前方法职责执行业务查询、转换或持久化处理；
     * 3. 返回处理结果或更新对象状态。</p>
     */
    public void setStockOrganization(String stockOrganization) { this.stockOrganization = stockOrganization; }
    /**
     * 执行 getOwnerName 方法。
     * 
     * <p>实现步骤：
     * 1. 接收并校验调用方传入的数据；
     * 2. 按当前方法职责执行业务查询、转换或持久化处理；
     * 3. 返回处理结果或更新对象状态。</p>
     */
    public String getOwnerName() { return ownerName; }
    /**
     * 执行 setOwnerName 方法。
     * 
     * <p>实现步骤：
     * 1. 接收并校验调用方传入的数据；
     * 2. 按当前方法职责执行业务查询、转换或持久化处理；
     * 3. 返回处理结果或更新对象状态。</p>
     */
    public void setOwnerName(String ownerName) { this.ownerName = ownerName; }
    /**
     * 执行 getUnitName 方法。
     * 
     * <p>实现步骤：
     * 1. 接收并校验调用方传入的数据；
     * 2. 按当前方法职责执行业务查询、转换或持久化处理；
     * 3. 返回处理结果或更新对象状态。</p>
     */
    public String getUnitName() { return unitName; }
    /**
     * 执行 setUnitName 方法。
     * 
     * <p>实现步骤：
     * 1. 接收并校验调用方传入的数据；
     * 2. 按当前方法职责执行业务查询、转换或持久化处理；
     * 3. 返回处理结果或更新对象状态。</p>
     */
    public void setUnitName(String unitName) { this.unitName = unitName; }
    /**
     * 执行 getBatchNo 方法。
     * 
     * <p>实现步骤：
     * 1. 接收并校验调用方传入的数据；
     * 2. 按当前方法职责执行业务查询、转换或持久化处理；
     * 3. 返回处理结果或更新对象状态。</p>
     */
    public String getBatchNo() { return batchNo; }
    /**
     * 执行 setBatchNo 方法。
     * 
     * <p>实现步骤：
     * 1. 接收并校验调用方传入的数据；
     * 2. 按当前方法职责执行业务查询、转换或持久化处理；
     * 3. 返回处理结果或更新对象状态。</p>
     */
    public void setBatchNo(String batchNo) { this.batchNo = batchNo; }
    /**
     * 执行 getQuantity 方法。
     * 
     * <p>实现步骤：
     * 1. 接收并校验调用方传入的数据；
     * 2. 按当前方法职责执行业务查询、转换或持久化处理；
     * 3. 返回处理结果或更新对象状态。</p>
     */
    public BigDecimal getQuantity() { return quantity; }
    /**
     * 执行 setQuantity 方法。
     * 
     * <p>实现步骤：
     * 1. 接收并校验调用方传入的数据；
     * 2. 按当前方法职责执行业务查询、转换或持久化处理；
     * 3. 返回处理结果或更新对象状态。</p>
     */
    public void setQuantity(BigDecimal quantity) { this.quantity = quantity; }
    /**
     * 执行 getFromWarehouse 方法。
     * 
     * <p>实现步骤：
     * 1. 接收并校验调用方传入的数据；
     * 2. 按当前方法职责执行业务查询、转换或持久化处理；
     * 3. 返回处理结果或更新对象状态。</p>
     */
    public String getFromWarehouse() { return fromWarehouse; }
    /**
     * 执行 setFromWarehouse 方法。
     * 
     * <p>实现步骤：
     * 1. 接收并校验调用方传入的数据；
     * 2. 按当前方法职责执行业务查询、转换或持久化处理；
     * 3. 返回处理结果或更新对象状态。</p>
     */
    public void setFromWarehouse(String fromWarehouse) { this.fromWarehouse = fromWarehouse; }
    /**
     * 执行 getToWarehouse 方法。
     * 
     * <p>实现步骤：
     * 1. 接收并校验调用方传入的数据；
     * 2. 按当前方法职责执行业务查询、转换或持久化处理；
     * 3. 返回处理结果或更新对象状态。</p>
     */
    public String getToWarehouse() { return toWarehouse; }
    /**
     * 执行 setToWarehouse 方法。
     * 
     * <p>实现步骤：
     * 1. 接收并校验调用方传入的数据；
     * 2. 按当前方法职责执行业务查询、转换或持久化处理；
     * 3. 返回处理结果或更新对象状态。</p>
     */
    public void setToWarehouse(String toWarehouse) { this.toWarehouse = toWarehouse; }
    /**
     * 执行 getRelatedBizNo 方法。
     * 
     * <p>实现步骤：
     * 1. 接收并校验调用方传入的数据；
     * 2. 按当前方法职责执行业务查询、转换或持久化处理；
     * 3. 返回处理结果或更新对象状态。</p>
     */
    public String getRelatedBizNo() { return relatedBizNo; }
    /**
     * 执行 setRelatedBizNo 方法。
     * 
     * <p>实现步骤：
     * 1. 接收并校验调用方传入的数据；
     * 2. 按当前方法职责执行业务查询、转换或持久化处理；
     * 3. 返回处理结果或更新对象状态。</p>
     */
    public void setRelatedBizNo(String relatedBizNo) { this.relatedBizNo = relatedBizNo; }
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
     * 执行 getRemark 方法。
     * 
     * <p>实现步骤：
     * 1. 接收并校验调用方传入的数据；
     * 2. 按当前方法职责执行业务查询、转换或持久化处理；
     * 3. 返回处理结果或更新对象状态。</p>
     */
    public String getRemark() { return remark; }
    /**
     * 执行 setRemark 方法。
     * 
     * <p>实现步骤：
     * 1. 接收并校验调用方传入的数据；
     * 2. 按当前方法职责执行业务查询、转换或持久化处理；
     * 3. 返回处理结果或更新对象状态。</p>
     */
    public void setRemark(String remark) { this.remark = remark; }
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
     * 获取库存流水关联凭证主键。
     *
     * <p>实现步骤：返回会计平台自动制证成功后回写的凭证 ID，供库存列表判断是否显示在线凭证入口。</p>
     */
    public Long getVoucherId() { return voucherId; }

    /**
     * 设置库存流水关联凭证主键。
     *
     * <p>实现步骤：自动生成凭证成功后由会计服务写入，保持库存流水到凭证的正向追溯链路。</p>
     */
    public void setVoucherId(Long voucherId) { this.voucherId = voucherId; }

    /**
     * 获取库存流水关联凭证号。
     *
     * <p>实现步骤：返回会计平台生成的凭证号，供列表展示和在线凭证跳转使用。</p>
     */
    public String getVoucherNo() { return voucherNo; }

    /**
     * 设置库存流水关联凭证号。
     *
     * <p>实现步骤：自动生成凭证成功后保存凭证号快照，方便用户从库存台账直接查看凭证。</p>
     */
    public void setVoucherNo(String voucherNo) { this.voucherNo = voucherNo; }
}
