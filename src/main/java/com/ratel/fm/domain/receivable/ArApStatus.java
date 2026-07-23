package com.ratel.fm.domain.receivable;

/**
 * ArApStatus 枚举。
 * 
 * <p>用于承载 ArApStatus 相关的业务数据、接口契约或处理逻辑，便于维护人员快速定位模块职责。</p>
 */
public enum ArApStatus {
    /**
     * 枚举值 OPEN：表示 OPEN 对应的业务状态或类型。
     */
    OPEN,
    /**
     * 枚举值 PARTIAL：表示 PARTIAL 对应的业务状态或类型。
     */
    PARTIAL,
    /**
     * 枚举值 CLOSED：表示 CLOSED 对应的业务状态或类型。
     */
    CLOSED,
    OVERDUE
}
