package com.ratel.fm.config.security;

import com.ratel.fm.repository.auth.UserAccountRepository;
import com.ratel.fm.security.JwtCookieAuthenticationFilter;
import jakarta.servlet.DispatcherType;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

/**
 * Spring Security 安全配置。
 *
 * <p>负责密码加密器、用户加载规则、JWT Cookie 过滤器接入和接口访问规则。</p>
 *
 * <p>开发组织：ratel；开发人员：WenZhang；联系方式：18782945613。</p>
 *
 * @author ratel
 */
@Configuration
@EnableMethodSecurity
public class SecurityConfig {

    /**
     * 构建 BCrypt 密码加密器。
     *
     * <p>实现目的：人员密码只保存哈希值，不保存明文。</p>
     */
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    /**
     * 构建 Spring Security 用户加载服务。
     *
     * <p>实现步骤：
     * 1. 优先按身份证号查询人员，查不到时按唯一登录账号查询；
     * 2. 把人员启停状态映射为 Security disabled 状态；
     * 3. 把人员角色中的权限码展开为 GrantedAuthority；
     * 4. 查询不到人员时抛出标准 UsernameNotFoundException。</p>
     */
    @Bean
    public UserDetailsService userDetailsService(UserAccountRepository userAccountRepository) {
        return loginName -> {
            throw new org.springframework.security.core.userdetails.UsernameNotFoundException(
                    "ratel-fm 使用 /api/auth/login 的所属公司维度登录，不使用全局 UserDetailsService: " + loginName);
        };
    }

    /**
     * 构建后端 API 安全过滤链。
     *
     * <p>实现步骤：
     * 1. 关闭 CSRF、HTTP Basic 和表单登录，系统只使用 JWT Cookie；
     * 2. 使用无状态 Session 策略，避免服务端 HttpSession；
     * 3. 统一未登录响应入口；
     * 4. 放行登录接口，保护所有 /api/** 接口；
     * 5. 把 JWT Cookie 过滤器放在用户名密码过滤器之前。</p>
     */
    @Bean
    public SecurityFilterChain securityFilterChain(
            HttpSecurity http,
            JwtCookieAuthenticationFilter jwtCookieAuthenticationFilter,
            ApiUnauthorizedHandler apiUnauthorizedHandler
    ) throws Exception {
        return http
                .csrf(AbstractHttpConfigurer::disable)
                .httpBasic(AbstractHttpConfigurer::disable)
                .formLogin(AbstractHttpConfigurer::disable)
                .cors(Customizer.withDefaults())
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .exceptionHandling(exception -> exception.authenticationEntryPoint(apiUnauthorizedHandler))
                .authorizeHttpRequests(auth -> auth
                        // SSE 等异步请求完成后会触发容器内部派发，响应可能已提交，不能再按 /api/** 二次鉴权。
                        .dispatcherTypeMatchers(DispatcherType.ASYNC, DispatcherType.ERROR).permitAll()
                        .requestMatchers("/api/auth/login", "/api/auth/companies").permitAll()
                        .requestMatchers("/api/**").authenticated()
                        .anyRequest().permitAll()
                )
                .addFilterBefore(jwtCookieAuthenticationFilter, UsernamePasswordAuthenticationFilter.class)
                .build();
    }
}
