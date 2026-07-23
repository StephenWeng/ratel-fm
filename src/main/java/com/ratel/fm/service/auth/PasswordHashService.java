package com.ratel.fm.service.auth;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.regex.Pattern;

/**
 * 登录密码哈希服务。
 *
 * <p>系统只保存 BCrypt 哈希，不保存明文密码；登录、个人改密、管理员重置密码和初始化账号都应通过本服务处理。</p>
 */
@Service
public class PasswordHashService {

    /**
     * BCrypt 哈希格式：$2a/$2b/$2y + cost + 53 位盐和哈希内容。
     */
    private static final Pattern BCRYPT_PATTERN = Pattern.compile("^\\$2[aby]\\$\\d{2}\\$.{53}$");

    /**
     * 字段 passwordEncoder：Spring Security 提供的 BCrypt 编码器。
     */
    private final PasswordEncoder passwordEncoder;

    /**
     * 构造 PasswordHashService 实例。
     */
    public PasswordHashService(PasswordEncoder passwordEncoder) {
        this.passwordEncoder = passwordEncoder;
    }

    /**
     * 生成登录密码哈希。
     *
     * <p>实现步骤：使用当前 PasswordEncoder 生成不可逆 BCrypt 哈希，调用方只能保存返回值。</p>
     */
    public String hash(String rawPassword) {
        return passwordEncoder.encode(rawPassword);
    }

    /**
     * 校验登录密码。
     *
     * <p>实现步骤：
     * 1. BCrypt 哈希使用 PasswordEncoder.matches 校验；
     * 2. 仅为历史明文数据提供一次性兼容校验，登录成功后调用方应立即升级为 BCrypt。</p>
     */
    public boolean matches(String rawPassword, String storedPassword) {
        if (rawPassword == null || storedPassword == null || storedPassword.isBlank()) {
            return false;
        }
        if (isBcryptHash(storedPassword)) {
            return passwordEncoder.matches(rawPassword, storedPassword);
        }
        return constantTimeEquals(rawPassword, storedPassword);
    }

    /**
     * 判断已存密码是否需要升级为 BCrypt。
     */
    public boolean requiresUpgrade(String storedPassword) {
        return storedPassword != null && !storedPassword.isBlank() && !isBcryptHash(storedPassword);
    }

    /**
     * 判断已存密码是否为空。
     */
    public boolean missing(String storedPassword) {
        return storedPassword == null || storedPassword.isBlank();
    }

    /**
     * 判断字符串是否为 BCrypt 哈希。
     */
    private boolean isBcryptHash(String storedPassword) {
        return BCRYPT_PATTERN.matcher(storedPassword).matches();
    }

    /**
     * 使用固定时间比较兼容历史明文密码，避免明显的长度短路时序差异。
     */
    private boolean constantTimeEquals(String rawPassword, String storedPassword) {
        byte[] rawBytes = rawPassword.getBytes(StandardCharsets.UTF_8);
        byte[] storedBytes = storedPassword.getBytes(StandardCharsets.UTF_8);
        return MessageDigest.isEqual(rawBytes, storedBytes);
    }
}
