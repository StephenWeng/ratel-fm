package com.ratel.fm.config.security;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONWriter;
import com.ratel.fm.common.ApiResponse;
import com.ratel.fm.common.ResponseCode;
import com.ratel.fm.security.JwtCookieAuthenticationFilter;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.MediaType;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.stereotype.Component;

import java.io.IOException;

/**
 * API 未登录统一响应。
 *
 * <p>开发组织：ratel；开发人员：WenZhang；联系方式：18782945613。</p>
 *
 * @author ratel
 */
@Component
public class ApiUnauthorizedHandler implements AuthenticationEntryPoint {

    /**
     * 字段 jwtCookieAuthenticationFilter：保存 jwtCookieAuthenticationFilter 对应的业务数据、运行配置或依赖对象，供本类逻辑读取和维护。
     */
    private final JwtCookieAuthenticationFilter jwtCookieAuthenticationFilter;

    /**
     * 构造 ApiUnauthorizedHandler 实例。
     * 
     * <p>实现步骤：
     * 1. 接收调用方传入的依赖对象或初始化参数；
     * 2. 保存到成员字段，保证后续业务方法可以复用；
     * 3. 完成实例初始化。</p>
     */
    public ApiUnauthorizedHandler(JwtCookieAuthenticationFilter jwtCookieAuthenticationFilter) {
        this.jwtCookieAuthenticationFilter = jwtCookieAuthenticationFilter;
    }

    /**
     * 输出未认证或认证失效的统一 JSON 响应。
     *
     * <p>实现步骤：
     * 1. 把 HTTP 状态设置为 401；
     * 2. 清理浏览器中的 JWT Cookie，避免登录页继续携带无效令牌；
     * 3. 从 JWT 过滤器写入的 request attribute 中读取业务错误码和错误消息；
     * 4. 缺少明确错误码时按无认证信息处理；
     * 5. 使用统一 ApiResponse 写回前端，前端按当前路由决定静默清理或弹出过期提示。</p>
     */
    @Override
    public void commence(HttpServletRequest request, HttpServletResponse response, AuthenticationException authException)
            throws IOException {
        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding("UTF-8");
        // 步骤2：未认证响应一定携带清 Cookie 指令，确保 HttpOnly JWT 也能被浏览器删除。
        jwtCookieAuthenticationFilter.writeCookie(response, "", 0);
        ResponseCode code = request.getAttribute(JwtCookieAuthenticationFilter.AUTH_ERROR_CODE_ATTR) instanceof ResponseCode responseCode
                ? responseCode
                : ResponseCode.NO_TOKEN_ERROR;
        String message = request.getAttribute(JwtCookieAuthenticationFilter.AUTH_ERROR_MESSAGE_ATTR) instanceof String authMessage
                ? authMessage
                : "当前登录过期，请重新登录";
        // 步骤4：使用 FastJson 直接写出，避免安全异常入口依赖 Spring MVC 消息转换器。
        response.getWriter().write(JSON.toJSONString(
                ApiResponse.fail(code, message),
                JSONWriter.Feature.WriteMapNullValue
        ));
    }
}
