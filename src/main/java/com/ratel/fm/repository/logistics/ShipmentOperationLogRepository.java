package com.ratel.fm.repository.logistics;

import com.ratel.fm.domain.logistics.ShipmentOperationLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.List;

/**
 * 物流管理操作流水数据访问接口。
 *
 * <p>用于记录和查询物流状态确认时的物流信息快照。</p>
 *
 * <p>开发组织：ratel；开发人员：WenZhang；联系方式：18782945613。</p>
 *
 * @author ratel
 */
public interface ShipmentOperationLogRepository extends JpaRepository<ShipmentOperationLog, Long>, JpaSpecificationExecutor<ShipmentOperationLog> {

    /**
     * 查询指定物流单的操作流水，按操作时间正序组成时间轴。
     */
    List<ShipmentOperationLog> findByShipmentOrder_IdOrderByOperationTimeAscIdAsc(Long shipmentOrderId);

    /**
     * 删除指定物流单下的全部操作流水，避免删除物流单时外键阻止主表删除。
     */
    void deleteByShipmentOrder_Id(Long shipmentOrderId);
}
