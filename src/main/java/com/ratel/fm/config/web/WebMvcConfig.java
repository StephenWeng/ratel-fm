package com.ratel.fm.config.web;

import com.ratel.fm.config.json.FastJsonHttpMessageConverter;
import org.springframework.context.annotation.Configuration;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.converter.HttpMessageConverters;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.ViewControllerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.nio.file.Path;

/**
 * Web MVC 前端路由配置。
 *
 * <p>当前系统采用前后端同工程部署，Vue 构建产物由 Spring Boot 提供静态访问；该配置负责把前端路由转发到首页。</p>
 *
 * <p>开发组织：ratel；开发人员：WenZhang；联系方式：18782945613。</p>
 *
 * @author ratel
 */
@Configuration
public class WebMvcConfig implements WebMvcConfigurer {

    /**
     * 字段 avatarStoragePath：保存 avatarStoragePath 对应的业务数据、运行配置或依赖对象，供本类逻辑读取和维护。
     */
    private final Path avatarStoragePath;
    /**
     * 字段 fastJsonHttpMessageConverter：保存 fastJsonHttpMessageConverter 对应的业务数据、运行配置或依赖对象，供本类逻辑读取和维护。
     */
    private final FastJsonHttpMessageConverter fastJsonHttpMessageConverter;

    /**
     * 构造 WebMvcConfig 实例。
     * 
     * <p>实现步骤：
     * 1. 接收调用方传入的依赖对象或初始化参数；
     * 2. 保存到成员字段，保证后续业务方法可以复用；
     * 3. 完成实例初始化。</p>
     */
    public WebMvcConfig(
            @Value("${app.upload.avatar-dir:uploads/avatars}") String avatarDir,
            FastJsonHttpMessageConverter fastJsonHttpMessageConverter
    ) {
        this.avatarStoragePath = Path.of(avatarDir).toAbsolutePath().normalize();
        this.fastJsonHttpMessageConverter = fastJsonHttpMessageConverter;
    }

    /**
     * 配置 REST 接口 JSON 消息转换器。
     *
     * <p>实现步骤：
     * 1. 通过 Spring 7 的消息转换器构建器注册 FastJson；
     * 2. 让 `application/json` 请求和响应优先使用 FastJson；
     * 3. 保留 Spring MVC 默认转换器处理文件、字符串等非 JSON 场景。</p>
     */
    @Override
    public void configureMessageConverters(HttpMessageConverters.ServerBuilder builder) {
        builder.withJsonConverter(fastJsonHttpMessageConverter);
    }

    /**
     * 注册头像静态资源访问路径。
     *
     * <p>实现步骤：把 `/uploads/avatars/**` 映射到本地头像保存目录，前端可直接使用头像 URL 展示图片。</p>
     */
    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        registry.addResourceHandler("/uploads/avatars/**")
                .addResourceLocations(avatarStoragePath.toUri().toString());
    }

    /**
     * 注册前端页面路由转发。
     *
     * <p>实现步骤：
     * 1. 枚举当前 Vue Router 中的页面路径；
     * 2. 把这些路径统一 forward 到 `/index.html`；
     * 3. 由前端路由接管页面渲染，避免浏览器刷新页面时 404。</p>
     */
    @Override
    public void addViewControllers(ViewControllerRegistry registry) {
        String[] routes = {
                "/login",
                "/login/star",
                "/dashboard",
                "/users",
                "/roles",
                "/menus",
                "/basic-dictionaries",
                "/subjects",
                "/vouchers",
                "/accounting-platform",
                "/purchase-orders",
                "/shipments",
                "/inventory",
                "/ar-ap",
                "/ar-ap-stats",
                "/reports",
                "/assistant",
                "/search",
                "/operation-logs"
        };
        for (String route : routes) {
            registry.addViewController(route).setViewName("forward:/index.html");
        }
    }
}
