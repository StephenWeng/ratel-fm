package com.ratel.fm.domain.inventory;

/**
 * InventoryMovementType 枚举。
 * 
 * <p>用于承载 InventoryMovementType 相关的业务数据、接口契约或处理逻辑，便于维护人员快速定位模块职责。</p>
 */
public enum InventoryMovementType {
    /**
     * 枚举值 INBOUND：表示 INBOUND 对应的业务状态或类型。
     */
    INBOUND,
    /**
     * 枚举值 OUTBOUND：表示 OUTBOUND 对应的业务状态或类型。
     */
    OUTBOUND,
    /**
     * 枚举值 TRANSFER：表示 TRANSFER 对应的业务状态或类型。
     */
    TRANSFER,
    CHECK
}
