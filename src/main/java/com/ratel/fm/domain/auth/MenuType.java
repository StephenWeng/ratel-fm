package com.ratel.fm.domain.auth;

/**
 * 菜单类型。
 *
 * <p>用于区分模块、页面和按钮，前端根据授权菜单编码控制导航、页面入口和按钮显隐。</p>
 *
 * <p>开发组织：ratel；开发人员：WenZhang；联系方式：18782945613。</p>
 *
 * @author ratel
 */
public enum MenuType {
    /**
     * 枚举值 MODULE：表示 MODULE 对应的业务状态或类型。
     */
    MODULE,
    /**
     * 枚举值 PAGE：表示 PAGE 对应的业务状态或类型。
     */
    PAGE,
    BUTTON
}
