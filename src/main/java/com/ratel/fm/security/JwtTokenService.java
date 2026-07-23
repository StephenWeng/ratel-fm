package com.ratel.fm.security;

import com.ratel.fm.domain.auth.PermissionCode;
import com.ratel.fm.config.json.FastJsonJwtCodec;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.Arrays;
import java.util.Date;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * JWT 身份令牌服务。
 *
 * <p>开发组织：ratel；开发人员：WenZhang；联系方式：18782945613。</p>
 *
 * @author ratel
 */
@Service
public class JwtTokenService {

    /**
     * 字段 secretKey：保存 secretKey 对应的业务数据、运行配置或依赖对象，供本类逻辑读取和维护。
     */
    private final SecretKey secretKey;
    /**
     * 字段 ttlMinutes：保存 ttlMinutes 对应的业务数据、运行配置或依赖对象，供本类逻辑读取和维护。
     */
    private final long ttlMinutes;
    /**
     * 字段 refreshThresholdMinutes：保存 refreshThresholdMinutes 对应的业务数据、运行配置或依赖对象，供本类逻辑读取和维护。
     */
    private final long refreshThresholdMinutes;
    /**
     * 字段 cookieName：保存 cookieName 对应的业务数据、运行配置或依赖对象，供本类逻辑读取和维护。
     */
    private final String cookieName;
    /**
     * 字段 fastJsonJwtCodec：保存 fastJsonJwtCodec 对应的业务数据、运行配置或依赖对象，供本类逻辑读取和维护。
     */
    private final FastJsonJwtCodec fastJsonJwtCodec;

    /**
     * 构造 JwtTokenService 实例。
     * 
     * <p>实现步骤：
     * 1. 接收调用方传入的依赖对象或初始化参数；
     * 2. 保存到成员字段，保证后续业务方法可以复用；
     * 3. 完成实例初始化。</p>
     */
    public JwtTokenService(
            @Value("${app.security.token-secret}") String secret,
            @Value("${app.security.token-ttl-minutes}") long ttlMinutes,
            @Value("${app.security.token-refresh-threshold-minutes}") long refreshThresholdMinutes,
            @Value("${app.security.token-cookie-name}") String cookieName,
            FastJsonJwtCodec fastJsonJwtCodec
    ) {
        // JWT HMAC 密钥至少需要 32 字节，配置不足时用固定字符补足，避免本地环境启动失败。
        this.secretKey = Keys.hmacShaKeyFor(normalizeSecret(secret).getBytes(StandardCharsets.UTF_8));
        this.ttlMinutes = ttlMinutes;
        this.refreshThresholdMinutes = refreshThresholdMinutes;
        this.cookieName = cookieName;
        this.fastJsonJwtCodec = fastJsonJwtCodec;
    }

    /**
     * 签发 JWT。
     *
     * <p>实现步骤：
     * 1. 计算签发时间和过期时间；
     * 2. 将权限码排序后写入 claims，保证令牌内容稳定；
     * 3. 写入人员基础信息、终端信息和 sessionId；
     * 4. 使用 HS256 签名并返回令牌和过期时间。</p>
     */
    public TokenIssue issue(CurrentUser user) {
        // 步骤1：JWT 有效期由配置控制，当前默认 60 分钟。
        Instant now = Instant.now();
        // 变量说明：expiresAt 保存当前步骤计算、查询或转换得到的中间结果。
        Instant expiresAt = now.plus(Duration.ofMinutes(ttlMinutes));
        // 步骤2：权限码放入 JWT，后续过滤器转换为 Spring Security authority。
        String permissions = user.permissions().stream()
                .map(Enum::name)
                .sorted()
                .collect(Collectors.joining(","));
        // 步骤3-4：claims 中的人员和终端信息会在每次请求时和数据库信息二次比对。
        String token = Jwts.builder()
                .json(fastJsonJwtCodec)
                .issuer("ratel-fm")
                .subject(user.username())
                .id(String.valueOf(user.id()))
                .issuedAt(Date.from(now))
                .expiration(Date.from(expiresAt))
                .claim("realName", user.realName())
                .claim("identityNo", user.identityNo())
                .claim("department", user.department())
                .claim("organizationCode", user.organizationCode())
                .claim("position", user.position())
                .claim("contactPhone", user.contactPhone())
                .claim("terminalType", user.terminalType())
                .claim("terminalIdentifier", user.terminalIdentifier())
                .claim("sessionId", user.sessionId())
                .claim("permissions", permissions)
                .signWith(secretKey, Jwts.SIG.HS256)
                .compact();
        return new TokenIssue(token, OffsetDateTime.ofInstant(expiresAt, ZoneId.systemDefault()));
    }

    /**
     * 解析并验签 JWT。
     *
     * <p>实现步骤：
     * 1. 使用同一 HMAC 密钥验签；
     * 2. 读取标准字段和自定义 claims；
     * 3. 将权限字符串还原为权限枚举集合；
     * 4. 组装为 CurrentUser，交给过滤器做数据库一致性校验。</p>
     */
    public CurrentUser parse(String token) {
        // 步骤1：签名、格式或过期校验失败时，JJWT 会抛出异常，由过滤器统一转成业务响应码。
        Claims claims = Jwts.parser()
                .json(fastJsonJwtCodec)
                .verifyWith(secretKey)
                .build()
                .parseSignedClaims(token)
                .getPayload();
        // 步骤2-3：权限码为空时返回空集合，避免空指针影响匿名或异常流程。
        Set<PermissionCode> permissions = permissions(claims.get("permissions", String.class));
        // 步骤4：这里只还原令牌内容，是否仍有效由数据库人员和会话状态决定。
        return new CurrentUser(
                Long.parseLong(claims.getId()),
                claims.getSubject(),
                claim(claims, "realName"),
                claim(claims, "identityNo"),
                claim(claims, "department"),
                claim(claims, "organizationCode"),
                claim(claims, "position"),
                claim(claims, "contactPhone"),
                claim(claims, "terminalType"),
                claim(claims, "terminalIdentifier"),
                claim(claims, "sessionId"),
                OffsetDateTime.ofInstant(claims.getExpiration().toInstant(), ZoneId.systemDefault()),
                permissions
        );
    }

    /**
     * 判断 JWT 是否需要自动续期。
     *
     * <p>当当前时间已经进入“过期前 refreshThresholdMinutes 分钟”的窗口时返回 true。</p>
     */
    public boolean shouldRefresh(CurrentUser user) {
        return user.expiresAt().toInstant().minus(Duration.ofMinutes(refreshThresholdMinutes)).isBefore(Instant.now());
    }

    /**
     * 返回 Cookie 最大存活秒数。
     *
     * <p>Cookie 存活时间与 JWT 有效期保持一致，避免浏览器继续携带明显过期的令牌。</p>
     */
    public int cookieMaxAgeSeconds() {
        return Math.toIntExact(Duration.ofMinutes(ttlMinutes).toSeconds());
    }

    /**
     * 执行 cookieName 方法。
     * 
     * <p>实现步骤：
     * 1. 接收并校验调用方传入的数据；
     * 2. 按当前方法职责执行业务查询、转换或持久化处理；
     * 3. 返回处理结果或更新对象状态。</p>
     */
    public String cookieName() {
        return cookieName;
    }

    /**
     * 执行 permissions 方法。
     * 
     * <p>实现步骤：
     * 1. 接收并校验调用方传入的数据；
     * 2. 按当前方法职责执行业务查询、转换或持久化处理；
     * 3. 返回处理结果或更新对象状态。</p>
     */
    private Set<PermissionCode> permissions(String permissions) {
        if (permissions == null || permissions.isBlank()) {
            return Set.of();
        }
        // 权限码写入 JWT 时使用逗号拼接，解析时逐个还原为枚举。
        return Arrays.stream(permissions.split(","))
                .map(PermissionCode::valueOf)
                .collect(Collectors.toSet());
    }

    /**
     * 执行 claim 方法。
     * 
     * <p>实现步骤：
     * 1. 接收并校验调用方传入的数据；
     * 2. 按当前方法职责执行业务查询、转换或持久化处理；
     * 3. 返回处理结果或更新对象状态。</p>
     */
    private String claim(Claims claims, String name) {
        return claims.get(name, String.class);
    }

    /**
     * 执行 normalizeSecret 方法。
     * 
     * <p>实现步骤：
     * 1. 接收并校验调用方传入的数据；
     * 2. 按当前方法职责执行业务查询、转换或持久化处理；
     * 3. 返回处理结果或更新对象状态。</p>
     */
    private String normalizeSecret(String secret) {
        if (secret.length() >= 32) {
            return secret;
        }
        // JJWT HS256 要求密钥长度足够，本地开发配置过短时补齐到 32 位。
        return (secret + "--------------------------------").substring(0, 32);
    }

    /**
     * JWT 签发结果。
     *
     * @param token JWT 字符串
     * @param expiresAt JWT 过期时间
     */
    public record TokenIssue(String token, OffsetDateTime expiresAt) {
    }
}
