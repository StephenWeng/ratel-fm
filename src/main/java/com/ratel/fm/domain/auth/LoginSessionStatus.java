package com.ratel.fm.domain.auth;

/**
 * 人员登录会话状态。
 *
 * <p>开发组织：ratel；开发人员：WenZhang；联系方式：18782945613。</p>
 *
 * @author ratel
 */
public enum LoginSessionStatus {
    /**
     * 枚举值 ACTIVE：表示 ACTIVE 对应的业务状态或类型。
     */
    ACTIVE,
    /**
     * 枚举值 FORCE_LOGOUT：表示 FORCE_LOGOUT 对应的业务状态或类型。
     */
    FORCE_LOGOUT,
    /**
     * 枚举值 LOGOUT：表示 LOGOUT 对应的业务状态或类型。
     */
    LOGOUT,
    EXPIRED
}
