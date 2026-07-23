package com.ratel.fm.repository.logistics;

import com.ratel.fm.domain.logistics.ShipmentOrder;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.List;
import java.util.Optional;

/**
 * 物流运输单数据访问接口。
 *
 * <p>用于物流单维护、状态流转、经营概览和智能检索。</p>
 *
 * <p>开发组织：ratel；开发人员：WenZhang；联系方式：18782945613。</p>
 *
 * @author ratel
 */
public interface ShipmentOrderRepository extends JpaRepository<ShipmentOrder, Long>, JpaSpecificationExecutor<ShipmentOrder> {

    /**
     * 判断物流单号是否已经存在。
     */
    boolean existsByShipmentNo(String shipmentNo);

    /**
     * 判断当前所属公司内物流单号是否已经存在。
     */
    boolean existsByOrganizationCodeAndShipmentNo(String organizationCode, String shipmentNo);

    /**
     * 查询当前所属公司内指定日期前缀下最大的物流单号。
     */
    Optional<ShipmentOrder> findFirstByOrganizationCodeAndShipmentNoStartingWithOrderByShipmentNoDesc(String organizationCode, String shipmentNoPrefix);

    /**
     * 查询最近 50 条物流单，按计划发运日期倒序返回。
     */
    List<ShipmentOrder> findTop50ByOrderByPlannedShipDateDesc();

    /**
     * 查询当前所属公司最近 50 条物流单，按计划发运日期倒序返回。
     */
    List<ShipmentOrder> findTop50ByOrganizationCodeOrderByPlannedShipDateDesc(String organizationCode);
}
