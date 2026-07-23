package com.ratel.fm.security;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

/**
 * SecurityUtils 类。
 * 
 * <p>用于承载 SecurityUtils 相关的业务数据、接口契约或处理逻辑，便于维护人员快速定位模块职责。</p>
 */
public final class SecurityUtils {

    private SecurityUtils() {
    }

    /**
     * 执行 currentUser 方法。
     * 
     * <p>实现步骤：
     * 1. 接收并校验调用方传入的数据；
     * 2. 按当前方法职责执行业务查询、转换或持久化处理；
     * 3. 返回处理结果或更新对象状态。</p>
     */
    public static CurrentUser currentUser() {
        // 变量说明：authentication 保存当前步骤计算、查询或转换得到的中间结果。
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !(authentication.getPrincipal() instanceof CurrentUser currentUser)) {
            return new CurrentUser(0L, "system", "System", null, null, null, null, null, null, null, null, null, java.util.Set.of());
        }
        return currentUser;
    }
}
