package com.ratel.fm.domain.logistics;

/**
 * ShipmentStatus 枚举。
 * 
 * <p>用于承载 ShipmentStatus 相关的业务数据、接口契约或处理逻辑，便于维护人员快速定位模块职责。</p>
 */
public enum ShipmentStatus {
    /**
     * 枚举值 CREATED：表示 CREATED 对应的业务状态或类型。
     */
    CREATED,
    /**
     * 枚举值 DISPATCHED：表示 DISPATCHED 对应的业务状态或类型。
     */
    DISPATCHED,
    /**
     * 枚举值 IN_TRANSIT：表示 IN_TRANSIT 对应的业务状态或类型。
     */
    IN_TRANSIT,
    /**
     * 枚举值 DELIVERED：表示 DELIVERED 对应的业务状态或类型。
     */
    DELIVERED,
    CANCELLED
}
