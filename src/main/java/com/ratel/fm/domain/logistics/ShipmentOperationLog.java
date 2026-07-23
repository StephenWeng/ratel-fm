package com.ratel.fm.domain.logistics;

import com.ratel.fm.common.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import org.hibernate.annotations.Comment;

import java.time.LocalDate;
import java.time.OffsetDateTime;

/**
 * 物流管理操作流水。
 *
 * <p>每次物流状态确认时，保存确认后的物流信息快照，便于后续按时间轴追踪物流信息如何变化。</p>
 *
 * <p>开发组织：ratel；开发人员：WenZhang；联系方式：18782945613。</p>
 *
 * @author ratel
 */
@Entity
@Table(name = "fm_shipment_operation_logs")
@Comment("物流管理操作流水表，记录每次状态确认后的物流信息快照")
public class ShipmentOperationLog extends BaseEntity {

    /** 所属物流单。 */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "shipment_order_id", nullable = false)
    @Comment("所属物流单ID")
    private ShipmentOrder shipmentOrder;

    /** 物流单号快照，便于物流单号变更或脱离关联时仍可追溯。 */
    @Column(nullable = false, length = 60)
    @Comment("物流单号快照")
    private String shipmentNo;

    /** 原状态，创建流水时可能为空。 */
    @Enumerated(EnumType.STRING)
    @Column(length = 30)
    @Comment("原物流状态")
    private ShipmentStatus fromStatus;

    /** 目标状态。 */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    @Comment("目标物流状态")
    private ShipmentStatus toStatus;

    /** 关联业务单号快照。 */
    @Column(length = 300)
    @Comment("关联业务单号快照")
    private String relatedOrderNo;

    /** 单据类型快照。 */
    @Column(length = 80)
    @Comment("物流单据类型快照")
    private String documentType;

    /** 项目字典编码快照，用于还原状态确认发生时物流单所属项目。 */
    @Column(length = 80)
    @Comment("项目字典编码快照")
    private String projectCode;

    /** 项目名称快照，用于查看物流状态确认流水时直接展示项目。 */
    @Column(length = 160)
    @Comment("项目名称快照")
    private String projectName;

    /** 运输方式快照。 */
    @Column(length = 80)
    @Comment("运输方式快照")
    private String transportMode;

    /** 发运组织快照。 */
    @Column(length = 120)
    @Comment("发运组织快照")
    private String shippingOrganization;

    /** 收货组织快照。 */
    @Column(length = 120)
    @Comment("收货组织快照")
    private String receivingOrganization;

    /** 承运商名称快照。 */
    @Column(nullable = false, length = 160)
    @Comment("承运商名称快照")
    private String carrierName;

    /** 运单号快照。 */
    @Column(length = 120)
    @Comment("承运商运单号或跟踪号快照")
    private String trackingNo;

    /** 司机姓名快照。 */
    @Column(length = 20)
    @Comment("司机姓名快照")
    private String driverName;

    /** 司机电话快照。 */
    @Column(length = 30)
    @Comment("司机电话快照")
    private String driverPhone;

    /** 车牌号快照，兼容普通车牌和新能源/电动车牌。 */
    @Column(length = 12)
    @Comment("车牌号快照")
    private String vehicleNo;

    /** 发货地行政区划编码级联路径快照。 */
    @Column(length = 300)
    @Comment("发货地行政区划编码级联路径快照")
    private String originDivisionCode;

    /** 发货地行政区划名称级联快照。 */
    @Column(length = 300)
    @Comment("发货地行政区划名称级联快照")
    private String originDivisionName;

    /** 目的地行政区划编码级联路径快照。 */
    @Column(length = 300)
    @Comment("目的地行政区划编码级联路径快照")
    private String destinationDivisionCode;

    /** 目的地行政区划名称级联快照。 */
    @Column(length = 300)
    @Comment("目的地行政区划名称级联快照")
    private String destinationDivisionName;

    /** 发货详细地址快照。 */
    @Column(nullable = false, length = 300)
    @Comment("发货地详细地址快照")
    private String origin;

    /** 目的详细地址快照。 */
    @Column(nullable = false, length = 300)
    @Comment("目的地详细地址快照")
    private String destination;

    /** 计划发运日期快照。 */
    @Column(nullable = false)
    @Comment("计划发运日期快照")
    private LocalDate plannedShipDate;

    /** 实际发运日期快照。 */
    @Comment("实际发运日期快照")
    private LocalDate actualShipDate;

    /** 实际送达日期快照。 */
    @Comment("实际送达日期快照")
    private LocalDate deliveredDate;

    /** 物流备注快照。 */
    @Column(length = 2000)
    @Comment("物流备注快照")
    private String remark;

    /** 本次状态确认说明。 */
    @Column(length = 2000)
    @Comment("本次状态确认说明")
    private String operationRemark;

    /** 操作人员主键。 */
    @Comment("操作人员主键")
    private Long operatorId;

    /** 操作人员账号。 */
    @Column(length = 80)
    @Comment("操作人员账号")
    private String operatorUsername;

    /** 操作人员姓名。 */
    @Column(length = 120)
    @Comment("操作人员姓名")
    private String operatorName;

    /** 操作发生时间。 */
    @Column(nullable = false)
    @Comment("操作发生时间")
    private OffsetDateTime operationTime;

    /**
     * 执行 getShipmentOrder 方法。
     * 
     * <p>实现步骤：
     * 1. 接收并校验调用方传入的数据；
     * 2. 按当前方法职责执行业务查询、转换或持久化处理；
     * 3. 返回处理结果或更新对象状态。</p>
     */
    public ShipmentOrder getShipmentOrder() {
        return shipmentOrder;
    }

    /**
     * 执行 setShipmentOrder 方法。
     * 
     * <p>实现步骤：
     * 1. 接收并校验调用方传入的数据；
     * 2. 按当前方法职责执行业务查询、转换或持久化处理；
     * 3. 返回处理结果或更新对象状态。</p>
     */
    public void setShipmentOrder(ShipmentOrder shipmentOrder) {
        this.shipmentOrder = shipmentOrder;
    }

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
     * 执行 getFromStatus 方法。
     * 
     * <p>实现步骤：
     * 1. 接收并校验调用方传入的数据；
     * 2. 按当前方法职责执行业务查询、转换或持久化处理；
     * 3. 返回处理结果或更新对象状态。</p>
     */
    public ShipmentStatus getFromStatus() {
        return fromStatus;
    }

    /**
     * 执行 setFromStatus 方法。
     * 
     * <p>实现步骤：
     * 1. 接收并校验调用方传入的数据；
     * 2. 按当前方法职责执行业务查询、转换或持久化处理；
     * 3. 返回处理结果或更新对象状态。</p>
     */
    public void setFromStatus(ShipmentStatus fromStatus) {
        this.fromStatus = fromStatus;
    }

    /**
     * 执行 getToStatus 方法。
     * 
     * <p>实现步骤：
     * 1. 接收并校验调用方传入的数据；
     * 2. 按当前方法职责执行业务查询、转换或持久化处理；
     * 3. 返回处理结果或更新对象状态。</p>
     */
    public ShipmentStatus getToStatus() {
        return toStatus;
    }

    /**
     * 执行 setToStatus 方法。
     * 
     * <p>实现步骤：
     * 1. 接收并校验调用方传入的数据；
     * 2. 按当前方法职责执行业务查询、转换或持久化处理；
     * 3. 返回处理结果或更新对象状态。</p>
     */
    public void setToStatus(ShipmentStatus toStatus) {
        this.toStatus = toStatus;
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
     * 获取项目字典编码快照。
     *
     * <p>实现步骤：返回状态确认流水保存时的项目编码，便于追溯历史确认所属项目。</p>
     */
    public String getProjectCode() {
        return projectCode;
    }

    /**
     * 设置项目字典编码快照。
     *
     * <p>实现步骤：在生成物流操作流水时从物流主表复制项目编码。</p>
     */
    public void setProjectCode(String projectCode) {
        this.projectCode = projectCode;
    }

    /**
     * 获取项目名称快照。
     *
     * <p>实现步骤：返回状态确认流水保存时的项目名称，供查看流水直接展示。</p>
     */
    public String getProjectName() {
        return projectName;
    }

    /**
     * 设置项目名称快照。
     *
     * <p>实现步骤：在生成物流操作流水时从物流主表复制项目名称。</p>
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
     * 执行 getOperationRemark 方法。
     * 
     * <p>实现步骤：
     * 1. 接收并校验调用方传入的数据；
     * 2. 按当前方法职责执行业务查询、转换或持久化处理；
     * 3. 返回处理结果或更新对象状态。</p>
     */
    public String getOperationRemark() {
        return operationRemark;
    }

    /**
     * 执行 setOperationRemark 方法。
     * 
     * <p>实现步骤：
     * 1. 接收并校验调用方传入的数据；
     * 2. 按当前方法职责执行业务查询、转换或持久化处理；
     * 3. 返回处理结果或更新对象状态。</p>
     */
    public void setOperationRemark(String operationRemark) {
        this.operationRemark = operationRemark;
    }

    /**
     * 执行 getOperatorId 方法。
     * 
     * <p>实现步骤：
     * 1. 接收并校验调用方传入的数据；
     * 2. 按当前方法职责执行业务查询、转换或持久化处理；
     * 3. 返回处理结果或更新对象状态。</p>
     */
    public Long getOperatorId() {
        return operatorId;
    }

    /**
     * 执行 setOperatorId 方法。
     * 
     * <p>实现步骤：
     * 1. 接收并校验调用方传入的数据；
     * 2. 按当前方法职责执行业务查询、转换或持久化处理；
     * 3. 返回处理结果或更新对象状态。</p>
     */
    public void setOperatorId(Long operatorId) {
        this.operatorId = operatorId;
    }

    /**
     * 执行 getOperatorUsername 方法。
     * 
     * <p>实现步骤：
     * 1. 接收并校验调用方传入的数据；
     * 2. 按当前方法职责执行业务查询、转换或持久化处理；
     * 3. 返回处理结果或更新对象状态。</p>
     */
    public String getOperatorUsername() {
        return operatorUsername;
    }

    /**
     * 执行 setOperatorUsername 方法。
     * 
     * <p>实现步骤：
     * 1. 接收并校验调用方传入的数据；
     * 2. 按当前方法职责执行业务查询、转换或持久化处理；
     * 3. 返回处理结果或更新对象状态。</p>
     */
    public void setOperatorUsername(String operatorUsername) {
        this.operatorUsername = operatorUsername;
    }

    /**
     * 执行 getOperatorName 方法。
     * 
     * <p>实现步骤：
     * 1. 接收并校验调用方传入的数据；
     * 2. 按当前方法职责执行业务查询、转换或持久化处理；
     * 3. 返回处理结果或更新对象状态。</p>
     */
    public String getOperatorName() {
        return operatorName;
    }

    /**
     * 执行 setOperatorName 方法。
     * 
     * <p>实现步骤：
     * 1. 接收并校验调用方传入的数据；
     * 2. 按当前方法职责执行业务查询、转换或持久化处理；
     * 3. 返回处理结果或更新对象状态。</p>
     */
    public void setOperatorName(String operatorName) {
        this.operatorName = operatorName;
    }

    /**
     * 执行 getOperationTime 方法。
     * 
     * <p>实现步骤：
     * 1. 接收并校验调用方传入的数据；
     * 2. 按当前方法职责执行业务查询、转换或持久化处理；
     * 3. 返回处理结果或更新对象状态。</p>
     */
    public OffsetDateTime getOperationTime() {
        return operationTime;
    }

    /**
     * 执行 setOperationTime 方法。
     * 
     * <p>实现步骤：
     * 1. 接收并校验调用方传入的数据；
     * 2. 按当前方法职责执行业务查询、转换或持久化处理；
     * 3. 返回处理结果或更新对象状态。</p>
     */
    public void setOperationTime(OffsetDateTime operationTime) {
        this.operationTime = operationTime;
    }
}
