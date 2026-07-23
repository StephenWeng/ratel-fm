package com.ratel.fm.domain.finance;

/**
 * SubjectCategory 枚举。
 * 
 * <p>用于承载 SubjectCategory 相关的业务数据、接口契约或处理逻辑，便于维护人员快速定位模块职责。</p>
 */
public enum SubjectCategory {
    /**
     * 枚举值 ASSET：表示 ASSET 对应的业务状态或类型。
     */
    ASSET,
    /**
     * 枚举值 LIABILITY：表示 LIABILITY 对应的业务状态或类型。
     */
    LIABILITY,
    /**
     * 枚举值 COMMON：表示共同类科目，余额方向需要结合具体业务判断。
     */
    COMMON,
    /**
     * 枚举值 EQUITY：表示 EQUITY 对应的业务状态或类型。
     */
    EQUITY,
    /**
     * 枚举值 REVENUE：表示 REVENUE 对应的业务状态或类型。
     */
    REVENUE,
    /**
     * 枚举值 COST：表示 COST 对应的业务状态或类型。
     */
    COST,
    EXPENSE
}
