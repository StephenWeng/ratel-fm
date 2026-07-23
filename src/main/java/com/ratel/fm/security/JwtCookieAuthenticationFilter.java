package com.ratel.fm.security;

import com.ratel.fm.common.ResponseCode;
import com.ratel.fm.domain.auth.UserAccount;
import com.ratel.fm.domain.auth.LoginSessionStatus;
import com.ratel.fm.domain.auth.UserLoginSession;
import com.ratel.fm.repository.auth.UserLoginSessionRepository;
import com.ratel.fm.repository.auth.UserAccountRepository;
import io.jsonwebtoken.ExpiredJwtException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.time.OffsetDateTime;
import java.util.Arrays;
import java.util.Objects;

/**
 * 从浏览器 Cookie 中解析 JWT，并进行令牌、人员状态和人员信息一致性校验。
 *
 * <p>开发组织：ratel；开发人员：WenZhang；联系方式：18782945613。</p>
 *
 * @author ratel
 */
@Component
public class JwtCookieAuthenticationFilter extends OncePerRequestFilter {

    /** 认证失败响应码透传属性，AuthenticationEntryPoint 会读取该属性组装 ApiResponse。 */
    public static final String AUTH_ERROR_CODE_ATTR = JwtCookieAuthenticationFilter.class.getName() + ".AUTH_ERROR_CODE";

    /** 认证失败提示透传属性，AuthenticationEntryPoint 会读取该属性返回给前端倒计时弹窗。 */
    public static final String AUTH_ERROR_MESSAGE_ATTR = JwtCookieAuthenticationFilter.class.getName() + ".AUTH_ERROR_MESSAGE";

    /**
     * 字段 jwtTokenService：保存 jwtTokenService 对应的业务数据、运行配置或依赖对象，供本类逻辑读取和维护。
     */
    private final JwtTokenService jwtTokenService;
    /**
     * 字段 userAccountRepository：保存 userAccountRepository 对应的业务数据、运行配置或依赖对象，供本类逻辑读取和维护。
     */
    private final UserAccountRepository userAccountRepository;
    /**
     * 字段 sessionRepository：保存 sessionRepository 对应的业务数据、运行配置或依赖对象，供本类逻辑读取和维护。
     */
    private final UserLoginSessionRepository sessionRepository;

    /**
     * 构造 JwtCookieAuthenticationFilter 实例。
     * 
     * <p>实现步骤：
     * 1. 接收调用方传入的依赖对象或初始化参数；
     * 2. 保存到成员字段，保证后续业务方法可以复用；
     * 3. 完成实例初始化。</p>
     */
    public JwtCookieAuthenticationFilter(
            JwtTokenService jwtTokenService,
            UserAccountRepository userAccountRepository,
            UserLoginSessionRepository sessionRepository
    ) {
        this.jwtTokenService = jwtTokenService;
        this.userAccountRepository = userAccountRepository;
        this.sessionRepository = sessionRepository;
    }

    /**
     * 每次请求从 Cookie 读取 JWT 并建立 Spring Security 登录上下文。
     *
     * <p>实现步骤：
     * 1. 从 Cookie 读取 JWT；
     * 2. 解析 JWT 并验签；
     * 3. 校验数据库人员存在且启用；
     * 4. 校验登录会话状态、过期时间和终端信息；
     * 5. 校验 JWT 中人员信息和数据库人员信息一致；
     * 6. 写入 Spring Security 上下文；
     * 7. 如果距离过期不足阈值，则刷新 JWT Cookie 和会话过期时间；
     * 8. 认证失败时清理上下文和 Cookie，并把失败码写入 request attribute。</p>
     */
    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        // 步骤1：系统只从 HttpOnly Cookie 中读取 JWT，不从 Authorization 头读取。
        String token = readCookie(request);
        if (token != null) {
            try {
                // 步骤2：解析 JWT，签名错误或格式错误会进入异常分支。
                CurrentUser tokenUser = jwtTokenService.parse(token);
                // 步骤3：人员必须存在且启用。认证人员使用 JWT ID 精确定位，避免账号变更影响会话校验。
                UserAccount account = userAccountRepository.findById(tokenUser.id())
                        .orElseThrow(() -> authFailure(request, ResponseCode.NO_TOKEN_ERROR, "认证人员不存在"));
                if (!account.isEnabled()) {
                    throw authFailure(request, ResponseCode.NO_AUTH, "人员已禁用");
                }
                // 步骤4：会话必须存在且 ACTIVE，旧登录被挤掉时会返回 FORCE_LOGOUT。
                UserLoginSession session = sessionRepository.findBySessionId(tokenUser.sessionId())
                        .orElseThrow(() -> authFailure(request, ResponseCode.NO_TOKEN_ERROR, "登录会话不存在"));
                validateSession(request, session, tokenUser, account);
                // 步骤5：用数据库当前人员信息重建 CurrentUser，再和 JWT 内容逐项比对。
                CurrentUser currentUser = toCurrentUser(
                        account,
                        tokenUser.expiresAt(),
                        tokenUser.terminalType(),
                        tokenUser.terminalIdentifier(),
                        tokenUser.sessionId()
                );
                if (!matches(tokenUser, currentUser)) {
                    throw authFailure(request, ResponseCode.NO_TOKEN_ERROR, "认证人员信息不一致");
                }
                // 步骤6：把权限码转换为 Spring Security authority，供 @PreAuthorize 使用。
                var authorities = currentUser.permissions().stream()
                        .map(permission -> new SimpleGrantedAuthority(permission.name()))
                        .toList();
                SecurityContextHolder.getContext()
                        .setAuthentication(new UsernamePasswordAuthenticationToken(currentUser, null, authorities));
                // 步骤7：进入续期窗口时重新签发 JWT，同时同步数据库会话过期时间。
                if (jwtTokenService.shouldRefresh(currentUser)) {
                    // 变量说明：issue 保存当前步骤计算、查询或转换得到的中间结果。
                    JwtTokenService.TokenIssue issue = jwtTokenService.issue(currentUser);
                    session.setExpiresAt(issue.expiresAt());
                    sessionRepository.save(session);
                    writeCookie(response, issue.token(), jwtTokenService.cookieMaxAgeSeconds());
                    response.setHeader("X-Token-Refreshed", "true");
                    response.setHeader("X-Token-Expires-At", issue.expiresAt().toString());
                }
            } catch (ExpiredJwtException ex) {
                // 步骤8：JWT 自身已过期时返回专门的 JWT_OVERTIME 业务码。
                markAuthFailure(request, ResponseCode.JWT_OVERTIME, "认证信息过期");
                SecurityContextHolder.clearContext();
                writeCookie(response, "", 0);
            } catch (Exception ex) {
                // 步骤8：其他认证失败如果没有写入更具体业务码，则按无效认证信息处理。
                if (request.getAttribute(AUTH_ERROR_CODE_ATTR) == null) {
                    markAuthFailure(request, ResponseCode.NO_TOKEN_ERROR, "认证信息无效");
                }
                SecurityContextHolder.clearContext();
                writeCookie(response, "", 0);
            }
        }
        filterChain.doFilter(request, response);
    }

    /**
     * 写入或清除 JWT Cookie。
     *
     * <p>maxAgeSeconds 为 0 时浏览器会删除 Cookie；大于 0 时表示登录有效期。</p>
     */
    public void writeCookie(HttpServletResponse response, String token, int maxAgeSeconds) {
        // 变量说明：cookie 保存当前步骤计算、查询或转换得到的中间结果。
        Cookie cookie = new Cookie(jwtTokenService.cookieName(), token);
        cookie.setHttpOnly(true);
        cookie.setSecure(false);
        cookie.setPath("/");
        cookie.setMaxAge(maxAgeSeconds);
        response.addCookie(cookie);
    }

    /**
     * 从请求 Cookie 中读取 JWT。
     */
    private String readCookie(HttpServletRequest request) {
        // 变量说明：cookies 保存当前步骤计算、查询或转换得到的中间结果。
        Cookie[] cookies = request.getCookies();
        if (cookies == null) {
            return null;
        }
        return Arrays.stream(cookies)
                .filter(cookie -> jwtTokenService.cookieName().equals(cookie.getName()))
                .map(Cookie::getValue)
                .findFirst()
                .orElse(null);
    }

    /**
     * 根据数据库人员信息和 JWT 会话信息重建当前登录人上下文。
     */
    private CurrentUser toCurrentUser(
            UserAccount user,
            OffsetDateTime expiresAt,
            String terminalType,
            String terminalIdentifier,
            String sessionId
    ) {
        /**
         * 当前用户聚合后的权限码集合，从所有角色展开后写入 CurrentUser。
         */
        var permissions = user.getRoles().stream()
                .flatMap(role -> role.getPermissions().stream())
                .collect(java.util.stream.Collectors.toSet());
        return new CurrentUser(
                user.getId(),
                user.getUsername(),
                user.getRealName(),
                user.getIdentityNo(),
                user.getDepartment(),
                user.getOrganizationCode(),
                user.getPosition(),
                user.getPhone(),
                terminalType,
                terminalIdentifier,
                sessionId,
                expiresAt,
                permissions
        );
    }

    /**
     * 校验 JWT 中的人员、终端和会话字段是否与数据库当前状态一致。
     *
     * <p>只要人员档案被修改、账号被替换、终端信息不一致或会话 ID 不一致，旧 JWT 都会失效。</p>
     */
    private boolean matches(CurrentUser tokenUser, CurrentUser accountUser) {
        return tokenUser.id().equals(accountUser.id())
                && same(tokenUser.username(), accountUser.username())
                && same(tokenUser.realName(), accountUser.realName())
                && same(tokenUser.identityNo(), accountUser.identityNo())
                && same(tokenUser.department(), accountUser.department())
                && same(tokenUser.organizationCode(), accountUser.organizationCode())
                && same(tokenUser.position(), accountUser.position())
                && same(tokenUser.contactPhone(), accountUser.contactPhone())
                && same(tokenUser.terminalType(), accountUser.terminalType())
                && same(tokenUser.terminalIdentifier(), accountUser.terminalIdentifier())
                && same(tokenUser.sessionId(), accountUser.sessionId());
    }

    /**
     * 执行 same 方法。
     * 
     * <p>实现步骤：
     * 1. 接收并校验调用方传入的数据；
     * 2. 按当前方法职责执行业务查询、转换或持久化处理；
     * 3. 返回处理结果或更新对象状态。</p>
     */
    private boolean same(String left, String right) {
        return Objects.equals(left == null ? "" : left, right == null ? "" : right);
    }

    /**
     * 校验登录会话状态。
     *
     * <p>实现步骤：
     * 1. FORCE_LOGOUT 返回强制下线；
     * 2. 已过期会话置为 EXPIRED 并返回认证过期；
     * 3. 非 ACTIVE 会话返回登录会话失效；
     * 4. 校验会话中的人员、身份证、终端类型和终端标识与 JWT 一致。</p>
     */
    private void validateSession(
            HttpServletRequest request,
            UserLoginSession session,
            CurrentUser tokenUser,
            UserAccount account
    ) {
        if (session.getStatus() == LoginSessionStatus.FORCE_LOGOUT) {
            throw authFailure(request, ResponseCode.FORCE_LOGOUT, "当前账号已在其他位置登录");
        }
        if (session.getStatus() == LoginSessionStatus.EXPIRED
                || session.getExpiresAt() == null
                || !session.getExpiresAt().isAfter(OffsetDateTime.now())) {
            session.setStatus(LoginSessionStatus.EXPIRED);
            session.setLogoutTime(OffsetDateTime.now());
            sessionRepository.save(session);
            throw authFailure(request, ResponseCode.JWT_OVERTIME, "认证信息过期");
        }
        if (session.getStatus() != LoginSessionStatus.ACTIVE) {
            throw authFailure(request, ResponseCode.NO_TOKEN_ERROR, "登录会话已失效");
        }
        if (!Objects.equals(session.getUserId(), account.getId())
                || !same(session.getIdentityNo(), tokenUser.identityNo())
                || !same(session.getOrganizationCode(), tokenUser.organizationCode())
                || !same(session.getTerminalType().name(), tokenUser.terminalType())
                || !same(session.getTerminalIdentifier(), tokenUser.terminalIdentifier())) {
            throw authFailure(request, ResponseCode.NO_TOKEN_ERROR, "认证会话信息不一致");
        }
    }

    /**
     * 创建认证失败异常，并把响应码和提示写入 request，供统一未登录处理器使用。
     */
    private RuntimeException authFailure(HttpServletRequest request, ResponseCode code, String message) {
        markAuthFailure(request, code, message);
        return new IllegalArgumentException(message);
    }

    /**
     * 标记认证失败原因。
     */
    private void markAuthFailure(HttpServletRequest request, ResponseCode code, String message) {
        request.setAttribute(AUTH_ERROR_CODE_ATTR, code);
        request.setAttribute(AUTH_ERROR_MESSAGE_ATTR, message);
    }
}
