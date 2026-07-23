package com.ratel.fm.domain.logistics;

import com.ratel.fm.common.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import org.hibernate.annotations.Comment;

import java.time.LocalDate;

/**
 * 物流运输单。
 *
 * <p>用于跟踪采购或其他业务单据的运输履约状态。</p>
 *
 * <p>开发组织：ratel；开发人员：WenZhang；联系方式：18782945613。</p>
 *
 * @author ratel
 */
@Entity
@Table(name = "fm_shipment_orders")
@Comment("物流运输单表，跟踪采购或其他业务单据的运输履约状态")
public class ShipmentOrder extends BaseEntity {

    /** 物流单号，全系统唯一，按计划发运日期生成。 */
    @Column(nullable = false, unique = true, length = 60)
    @Comment("物流单号，全系统唯一")
    private String shipmentNo;

    /** 所属公司字典编码，即账套编码，物流单按该字段隔离。 */
    @Column(nullable = false, length = 80)
    @Comment("所属公司字典编码，作为物流单账套隔离标识")
    private String organizationCode;

    /** 关联业务单号，如采购单号，用于业务追溯。 */
    @Column(length = 300)
    @Comment("关联业务单号")
    private String relatedOrderNo;

    /** 单据类型，如采购发运、销售发运、调拨发运。 */
    @Column(length = 80)
    @Comment("物流单据类型")
    private String documentType = "采购发运";

    /** 项目字典编码，来自基础信息项目字典，用于按项目归集物流履约。 */
    @Column(length = 80)
    @Comment("项目字典编码")
    private String projectCode;

    /** 项目名称快照，避免项目字典名称调整影响历史物流单展示。 */
    @Column(length = 160)
    @Comment("项目名称快照")
    private String projectName;

    /** 运输方式。 */
    @Column(length = 80)
    @Comment("运输方式")
    private String transportMode;

    /** 发运组织。 */
    @Column(length = 120)
    @Comment("发运组织")
    private String shippingOrganization;

    /** 收货组织。 */
    @Column(length = 120)
    @Comment("收货组织")
    private String receivingOrganization;

    /** 承运商名称。 */
    @Column(nullable = false, length = 160)
    @Comment("承运商名称")
    private String carrierName;

    /** 司机姓名。 */
    @Column(length = 20)
    @Comment("司机姓名")
    private String driverName;

    /** 司机电话。 */
    @Column(length = 30)
    @Comment("司机电话")
    private String driverPhone;

    /** 车牌号，兼容普通车牌和新能源/电动车牌。 */
    @Column(length = 12)
    @Comment("车牌号")
    private String vehicleNo;

    /** 承运商运单号或跟踪号。 */
    @Column(length = 80)
    @Comment("承运商运单号或跟踪号")
    private String trackingNo;

    /** 发货地行政区划编码级联路径，来自全国行政区划字典，格式如 110000/110100/110102。 */
    @Column(length = 300)
    @Comment("发货地行政区划编码级联路径")
    private String originDivisionCode;

    /** 发货地行政区划名称级联快照，避免字典名称调整影响历史单据展示。 */
    @Column(length = 300)
    @Comment("发货地行政区划名称级联快照")
    private String originDivisionName;

    /** 目的地行政区划编码级联路径，来自全国行政区划字典。 */
    @Column(length = 300)
    @Comment("目的地行政区划编码级联路径")
    private String destinationDivisionCode;

    /** 目的地行政区划名称级联快照，避免字典名称调整影响历史单据展示。 */
    @Column(length = 300)
    @Comment("目的地行政区划名称级联快照")
    private String destinationDivisionName;

    /** 发货地详址，不包含省市区县行政区划。 */
    @Column(nullable = false, length = 300)
    @Comment("发货地详址")
    private String origin;

    /** 目的地详址，不包含省市区县行政区划。 */
    @Column(nullable = false, length = 300)
    @Comment("目的地详址")
    private String destination;

    /** 计划发运日期，用于排期和单号生成。 */
    @Column(nullable = false)
    @Comment("计划发运日期")
    private LocalDate plannedShipDate;

    /** 实际发运日期。计划与实际不一致时用于保存确认后的真实发运时间。 */
    @Comment("实际发运日期")
    private LocalDate actualShipDate;

    /** 实际送达日期。状态改为 DELIVERED 时若为空则自动写入当前日期。 */
    @Comment("实际送达日期")
    private LocalDate deliveredDate;

    /** 物流状态，用于跟踪创建、发运、运输中、送达、取消。 */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    @Comment("物流状态")
    private ShipmentStatus status = ShipmentStatus.CREATED;

    /** 物流备注，支持长文本说明。 */
    @Column(length = 2000)
    @Comment("物流备注")
    private String remark;

    /**
     * 执行 getShipmentNo 方法。
     * 
     * <p>实现步骤：
     * 1. 接收并校验调用方传入的数据；
     * 2. 按当前方法职责执行业务查询、转换或持久化处理；
     * 3. 返回处理结果或更新对象状态。</p>
     */
    public String getShipmentNo() {
        return shipmentNo;
    }

    /**
     * 执行 setShipmentNo 方法。
     * 
     * <p>实现步骤：
     * 1. 接收并校验调用方传入的数据；
     * 2. 按当前方法职责执行业务查询、转换或持久化处理；
     * 3. 返回处理结果或更新对象状态。</p>
     */
    public void setShipmentNo(String shipmentNo) {
        this.shipmentNo = shipmentNo;
    }

    /**
     * 获取物流单所属公司编码。
     *
     * <p>实现步骤：直接返回物流单创建时写入的账套编码，列表、状态确认和导出均按该字段隔离。</p>
     */
    public String getOrganizationCode() {
        return organizationCode;
    }

    /**
     * 设置物流单所属公司编码。
     *
     * <p>实现步骤：新增物流单时写入当前登录公司的字典编码，确保物流履约数据只在当前公司可见。</p>
     */
    public void setOrganizationCode(String organizationCode) {
        this.organizationCode = organizationCode;
    }

    /**
     * 执行 getRelatedOrderNo 方法。
     * 
     * <p>实现步骤：
     * 1. 接收并校验调用方传入的数据；
     * 2. 按当前方法职责执行业务查询、转换或持久化处理；
     * 3. 返回处理结果或更新对象状态。</p>
     */
    public String getRelatedOrderNo() {
        return relatedOrderNo;
    }

    /**
     * 执行 setRelatedOrderNo 方法。
     * 
     * <p>实现步骤：
     * 1. 接收并校验调用方传入的数据；
     * 2. 按当前方法职责执行业务查询、转换或持久化处理；
     * 3. 返回处理结果或更新对象状态。</p>
     */
    public void setRelatedOrderNo(String relatedOrderNo) {
        this.relatedOrderNo = relatedOrderNo;
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
     * 获取项目字典编码。
     *
     * <p>实现步骤：返回物流单保存的项目编码快照，供列表筛选和业务追溯使用。</p>
     */
    public String getProjectCode() {
        return projectCode;
    }

    /**
     * 设置项目字典编码。
     *
     * <p>实现步骤：保存前端项目下拉框选中的字典编码，后续按项目查询物流单。</p>
     */
    public void setProjectCode(String projectCode) {
        this.projectCode = projectCode;
    }

    /**
     * 获取项目名称快照。
     *
     * <p>实现步骤：返回物流单保存时的项目名称，保障历史数据展示稳定。</p>
     */
    public String getProjectName() {
        return projectName;
    }

    /**
     * 设置项目名称快照。
     *
     * <p>实现步骤：保存项目字典名称快照，供物流列表、导出和状态流水展示。</p>
     */
    public void setProjectName(String projectName) {
        this.projectName = projectName;
    }

    /**
     * 执行 getTransportMode 方法。
     * 
     * <p>实现步骤：
     * 1. 接收并校验调用方传入的数据；
     * 2. 按当前方法职责执行业务查询、转换或持久化处理；
     * 3. 返回处理结果或更新对象状态。</p>
     */
    public String getTransportMode() {
        return transportMode;
    }

    /**
     * 执行 setTransportMode 方法。
     * 
     * <p>实现步骤：
     * 1. 接收并校验调用方传入的数据；
     * 2. 按当前方法职责执行业务查询、转换或持久化处理；
     * 3. 返回处理结果或更新对象状态。</p>
     */
    public void setTransportMode(String transportMode) {
        this.transportMode = transportMode;
    }

    /**
     * 执行 getShippingOrganization 方法。
     * 
     * <p>实现步骤：
     * 1. 接收并校验调用方传入的数据；
     * 2. 按当前方法职责执行业务查询、转换或持久化处理；
     * 3. 返回处理结果或更新对象状态。</p>
     */
    public String getShippingOrganization() {
        return shippingOrganization;
    }

    /**
     * 执行 setShippingOrganization 方法。
     * 
     * <p>实现步骤：
     * 1. 接收并校验调用方传入的数据；
     * 2. 按当前方法职责执行业务查询、转换或持久化处理；
     * 3. 返回处理结果或更新对象状态。</p>
     */
    public void setShippingOrganization(String shippingOrganization) {
        this.shippingOrganization = shippingOrganization;
    }

    /**
     * 执行 getReceivingOrganization 方法。
     * 
     * <p>实现步骤：
     * 1. 接收并校验调用方传入的数据；
     * 2. 按当前方法职责执行业务查询、转换或持久化处理；
     * 3. 返回处理结果或更新对象状态。</p>
     */
    public String getReceivingOrganization() {
        return receivingOrganization;
    }

    /**
     * 执行 setReceivingOrganization 方法。
     * 
     * <p>实现步骤：
     * 1. 接收并校验调用方传入的数据；
     * 2. 按当前方法职责执行业务查询、转换或持久化处理；
     * 3. 返回处理结果或更新对象状态。</p>
     */
    public void setReceivingOrganization(String receivingOrganization) {
        this.receivingOrganization = receivingOrganization;
    }

    /**
     * 执行 getCarrierName 方法。
     * 
     * <p>实现步骤：
     * 1. 接收并校验调用方传入的数据；
     * 2. 按当前方法职责执行业务查询、转换或持久化处理；
     * 3. 返回处理结果或更新对象状态。</p>
     */
    public String getCarrierName() {
        return carrierName;
    }

    /**
     * 执行 setCarrierName 方法。
     * 
     * <p>实现步骤：
     * 1. 接收并校验调用方传入的数据；
     * 2. 按当前方法职责执行业务查询、转换或持久化处理；
     * 3. 返回处理结果或更新对象状态。</p>
     */
    public void setCarrierName(String carrierName) {
        this.carrierName = carrierName;
    }

    /**
     * 执行 getDriverName 方法。
     * 
     * <p>实现步骤：
     * 1. 接收并校验调用方传入的数据；
     * 2. 按当前方法职责执行业务查询、转换或持久化处理；
     * 3. 返回处理结果或更新对象状态。</p>
     */
    public String getDriverName() {
        return driverName;
    }

    /**
     * 执行 setDriverName 方法。
     * 
     * <p>实现步骤：
     * 1. 接收并校验调用方传入的数据；
     * 2. 按当前方法职责执行业务查询、转换或持久化处理；
     * 3. 返回处理结果或更新对象状态。</p>
     */
    public void setDriverName(String driverName) {
        this.driverName = driverName;
    }

    /**
     * 执行 getDriverPhone 方法。
     * 
     * <p>实现步骤：
     * 1. 接收并校验调用方传入的数据；
     * 2. 按当前方法职责执行业务查询、转换或持久化处理；
     * 3. 返回处理结果或更新对象状态。</p>
     */
    public String getDriverPhone() {
        return driverPhone;
    }

    /**
     * 执行 setDriverPhone 方法。
     * 
     * <p>实现步骤：
     * 1. 接收并校验调用方传入的数据；
     * 2. 按当前方法职责执行业务查询、转换或持久化处理；
     * 3. 返回处理结果或更新对象状态。</p>
     */
    public void setDriverPhone(String driverPhone) {
        this.driverPhone = driverPhone;
    }

    /**
     * 执行 getVehicleNo 方法。
     * 
     * <p>实现步骤：
     * 1. 接收并校验调用方传入的数据；
     * 2. 按当前方法职责执行业务查询、转换或持久化处理；
     * 3. 返回处理结果或更新对象状态。</p>
     */
    public String getVehicleNo() {
        return vehicleNo;
    }

    /**
     * 执行 setVehicleNo 方法。
     * 
     * <p>实现步骤：
     * 1. 接收并校验调用方传入的数据；
     * 2. 按当前方法职责执行业务查询、转换或持久化处理；
     * 3. 返回处理结果或更新对象状态。</p>
     */
    public void setVehicleNo(String vehicleNo) {
        this.vehicleNo = vehicleNo;
    }

    /**
     * 执行 getTrackingNo 方法。
     * 
     * <p>实现步骤：
     * 1. 接收并校验调用方传入的数据；
     * 2. 按当前方法职责执行业务查询、转换或持久化处理；
     * 3. 返回处理结果或更新对象状态。</p>
     */
    public String getTrackingNo() {
        return trackingNo;
    }

    /**
     * 执行 setTrackingNo 方法。
     * 
     * <p>实现步骤：
     * 1. 接收并校验调用方传入的数据；
     * 2. 按当前方法职责执行业务查询、转换或持久化处理；
     * 3. 返回处理结果或更新对象状态。</p>
     */
    public void setTrackingNo(String trackingNo) {
        this.trackingNo = trackingNo;
    }

    /**
     * 执行 getOriginDivisionCode 方法。
     * 
     * <p>实现步骤：
     * 1. 接收并校验调用方传入的数据；
     * 2. 按当前方法职责执行业务查询、转换或持久化处理；
     * 3. 返回处理结果或更新对象状态。</p>
     */
    public String getOriginDivisionCode() {
        return originDivisionCode;
    }

    /**
     * 执行 setOriginDivisionCode 方法。
     * 
     * <p>实现步骤：
     * 1. 接收并校验调用方传入的数据；
     * 2. 按当前方法职责执行业务查询、转换或持久化处理；
     * 3. 返回处理结果或更新对象状态。</p>
     */
    public void setOriginDivisionCode(String originDivisionCode) {
        this.originDivisionCode = originDivisionCode;
    }

    /**
     * 执行 getOriginDivisionName 方法。
     * 
     * <p>实现步骤：
     * 1. 接收并校验调用方传入的数据；
     * 2. 按当前方法职责执行业务查询、转换或持久化处理；
     * 3. 返回处理结果或更新对象状态。</p>
     */
    public String getOriginDivisionName() {
        return originDivisionName;
    }

    /**
     * 执行 setOriginDivisionName 方法。
     * 
     * <p>实现步骤：
     * 1. 接收并校验调用方传入的数据；
     * 2. 按当前方法职责执行业务查询、转换或持久化处理；
     * 3. 返回处理结果或更新对象状态。</p>
     */
    public void setOriginDivisionName(String originDivisionName) {
        this.originDivisionName = originDivisionName;
    }

    /**
     * 执行 getDestinationDivisionCode 方法。
     * 
     * <p>实现步骤：
     * 1. 接收并校验调用方传入的数据；
     * 2. 按当前方法职责执行业务查询、转换或持久化处理；
     * 3. 返回处理结果或更新对象状态。</p>
     */
    public String getDestinationDivisionCode() {
        return destinationDivisionCode;
    }

    /**
     * 执行 setDestinationDivisionCode 方法。
     * 
     * <p>实现步骤：
     * 1. 接收并校验调用方传入的数据；
     * 2. 按当前方法职责执行业务查询、转换或持久化处理；
     * 3. 返回处理结果或更新对象状态。</p>
     */
    public void setDestinationDivisionCode(String destinationDivisionCode) {
        this.destinationDivisionCode = destinationDivisionCode;
    }

    /**
     * 执行 getDestinationDivisionName 方法。
     * 
     * <p>实现步骤：
     * 1. 接收并校验调用方传入的数据；
     * 2. 按当前方法职责执行业务查询、转换或持久化处理；
     * 3. 返回处理结果或更新对象状态。</p>
     */
    public String getDestinationDivisionName() {
        return destinationDivisionName;
    }

    /**
     * 执行 setDestinationDivisionName 方法。
     * 
     * <p>实现步骤：
     * 1. 接收并校验调用方传入的数据；
     * 2. 按当前方法职责执行业务查询、转换或持久化处理；
     * 3. 返回处理结果或更新对象状态。</p>
     */
    public void setDestinationDivisionName(String destinationDivisionName) {
        this.destinationDivisionName = destinationDivisionName;
    }

    /**
     * 执行 getOrigin 方法。
     * 
     * <p>实现步骤：
     * 1. 接收并校验调用方传入的数据；
     * 2. 按当前方法职责执行业务查询、转换或持久化处理；
     * 3. 返回处理结果或更新对象状态。</p>
     */
    public String getOrigin() {
        return origin;
    }

    /**
     * 执行 setOrigin 方法。
     * 
     * <p>实现步骤：
     * 1. 接收并校验调用方传入的数据；
     * 2. 按当前方法职责执行业务查询、转换或持久化处理；
     * 3. 返回处理结果或更新对象状态。</p>
     */
    public void setOrigin(String origin) {
        this.origin = origin;
    }

    /**
     * 执行 getDestination 方法。
     * 
     * <p>实现步骤：
     * 1. 接收并校验调用方传入的数据；
     * 2. 按当前方法职责执行业务查询、转换或持久化处理；
     * 3. 返回处理结果或更新对象状态。</p>
     */
    public String getDestination() {
        return destination;
    }

    /**
     * 执行 setDestination 方法。
     * 
     * <p>实现步骤：
     * 1. 接收并校验调用方传入的数据；
     * 2. 按当前方法职责执行业务查询、转换或持久化处理；
     * 3. 返回处理结果或更新对象状态。</p>
     */
    public void setDestination(String destination) {
        this.destination = destination;
    }

    /**
     * 执行 getPlannedShipDate 方法。
     * 
     * <p>实现步骤：
     * 1. 接收并校验调用方传入的数据；
     * 2. 按当前方法职责执行业务查询、转换或持久化处理；
     * 3. 返回处理结果或更新对象状态。</p>
     */
    public LocalDate getPlannedShipDate() {
        return plannedShipDate;
    }

    /**
     * 执行 setPlannedShipDate 方法。
     * 
     * <p>实现步骤：
     * 1. 接收并校验调用方传入的数据；
     * 2. 按当前方法职责执行业务查询、转换或持久化处理；
     * 3. 返回处理结果或更新对象状态。</p>
     */
    public void setPlannedShipDate(LocalDate plannedShipDate) {
        this.plannedShipDate = plannedShipDate;
    }

    /**
     * 执行 getActualShipDate 方法。
     * 
     * <p>实现步骤：
     * 1. 接收并校验调用方传入的数据；
     * 2. 按当前方法职责执行业务查询、转换或持久化处理；
     * 3. 返回处理结果或更新对象状态。</p>
     */
    public LocalDate getActualShipDate() {
        return actualShipDate;
    }

    /**
     * 执行 setActualShipDate 方法。
     * 
     * <p>实现步骤：
     * 1. 接收并校验调用方传入的数据；
     * 2. 按当前方法职责执行业务查询、转换或持久化处理；
     * 3. 返回处理结果或更新对象状态。</p>
     */
    public void setActualShipDate(LocalDate actualShipDate) {
        this.actualShipDate = actualShipDate;
    }

    /**
     * 执行 getDeliveredDate 方法。
     * 
     * <p>实现步骤：
     * 1. 接收并校验调用方传入的数据；
     * 2. 按当前方法职责执行业务查询、转换或持久化处理；
     * 3. 返回处理结果或更新对象状态。</p>
     */
    public LocalDate getDeliveredDate() {
        return deliveredDate;
    }

    /**
     * 执行 setDeliveredDate 方法。
     * 
     * <p>实现步骤：
     * 1. 接收并校验调用方传入的数据；
     * 2. 按当前方法职责执行业务查询、转换或持久化处理；
     * 3. 返回处理结果或更新对象状态。</p>
     */
    public void setDeliveredDate(LocalDate deliveredDate) {
        this.deliveredDate = deliveredDate;
    }

    /**
     * 执行 getStatus 方法。
     * 
     * <p>实现步骤：
     * 1. 接收并校验调用方传入的数据；
     * 2. 按当前方法职责执行业务查询、转换或持久化处理；
     * 3. 返回处理结果或更新对象状态。</p>
     */
    public ShipmentStatus getStatus() {
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
    public void setStatus(ShipmentStatus status) {
        this.status = status;
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
}
