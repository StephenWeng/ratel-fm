package com.ratel.fm.config.web;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;

/**
 * 前端 SPA 页面路由兜底过滤器。
 *
 * <p>实现目的：当用户直接访问不存在或未显式登记的前端页面路径时，也要返回 Vue 首页，
 * 由前端路由继续执行 JWT 校验、菜单权限判断和登录过期倒计时逻辑，避免页面显示后端 JSON 错误。</p>
 *
 * <p>实现步骤：
 * 1. 只处理浏览器页面 GET/HEAD 请求；
 * 2. 排除后端接口、接口文档、监控、上传文件和静态资源请求；
 * 3. 对剩余无文件后缀的页面路由统一 forward 到 /index.html。</p>
 *
 * <p>开发组织：ratel；开发人员：WenZhang；联系方式：18782945613。</p>
 *
 * @author ratel
 */
@Component
@Order(Ordered.LOWEST_PRECEDENCE - 10)
public class SpaRouteForwardFilter extends OncePerRequestFilter {

    /**
     * 后端接口和静态服务路径前缀，这些请求不能转发到前端首页。
     */
    private static final List<String> BACKEND_PREFIXES = List.of(
            "/api",
            "/actuator",
            "/uploads",
            "/v3/api-docs",
            "/swagger-ui",
            "/webjars"
    );

    /**
     * 后端保留的精确路径，避免错误页面、文档和 favicon 被 SPA 路由接管。
     */
    private static final List<String> BACKEND_EXACT_PATHS = List.of(
            "/doc.html",
            "/error",
            "/favicon.ico",
            "/swagger-ui.html"
    );

    /**
     * 执行前端路由兜底转发。
     *
     * <p>实现步骤：
     * 1. 判断当前请求是否为前端页面路由；
     * 2. 页面路由直接转发到 Vue 首页；
     * 3. 其他请求继续交给 Spring MVC 或静态资源处理链。</p>
     */
    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException, IOException {
        if (shouldForwardToIndex(request)) {
            request.getRequestDispatcher("/index.html").forward(request, response);
            return;
        }
        filterChain.doFilter(request, response);
    }

    /**
     * 判断请求是否应该交给前端路由处理。
     *
     * <p>实现步骤：
     * 1. 非 GET/HEAD 请求不转发，避免影响表单提交和接口调用；
     * 2. 非 HTML 页面请求不转发，避免影响 JSON 请求；
     * 3. 后端路径和带文件后缀的静态资源不转发；
     * 4. 其余路径按前端页面路由处理。</p>
     */
    private boolean shouldForwardToIndex(HttpServletRequest request) {
        // 变量说明：method 保存当前步骤计算、查询或转换得到的中间结果。
        String method = request.getMethod();
        if (!"GET".equalsIgnoreCase(method) && !"HEAD".equalsIgnoreCase(method)) {
            return false;
        }

        if (!acceptsHtml(request)) {
            return false;
        }

        // 变量说明：path 保存当前步骤计算、查询或转换得到的中间结果。
        String path = normalizePath(request);
        if (BACKEND_EXACT_PATHS.contains(path)) {
            return false;
        }
        if (BACKEND_PREFIXES.stream().anyMatch(prefix -> path.equals(prefix) || path.startsWith(prefix + "/"))) {
            return false;
        }
        return !hasFileExtension(path);
    }

    /**
     * 去掉上下文路径后得到应用内部路径。
     */
    private String normalizePath(HttpServletRequest request) {
        // 变量说明：path 保存当前步骤计算、查询或转换得到的中间结果。
        String path = request.getRequestURI();
        // 变量说明：contextPath 保存当前步骤计算、查询或转换得到的中间结果。
        String contextPath = request.getContextPath();
        if (contextPath != null && !contextPath.isBlank() && path.startsWith(contextPath)) {
            path = path.substring(contextPath.length());
        }
        return path.isBlank() ? "/" : path;
    }

    /**
     * 判断浏览器是否期望 HTML 页面响应。
     */
    private boolean acceptsHtml(HttpServletRequest request) {
        // 变量说明：accept 保存当前步骤计算、查询或转换得到的中间结果。
        String accept = request.getHeader(HttpHeaders.ACCEPT);
        return accept == null || accept.contains(MediaType.TEXT_HTML_VALUE);
    }

    /**
     * 判断路径最后一段是否包含文件后缀。
     */
    private boolean hasFileExtension(String path) {
        // 变量说明：slashIndex 保存当前步骤计算、查询或转换得到的中间结果。
        int slashIndex = path.lastIndexOf('/');
        // 变量说明：dotIndex 保存当前步骤计算、查询或转换得到的中间结果。
        int dotIndex = path.lastIndexOf('.');
        return dotIndex > slashIndex;
    }
}
