package com.ratel.fm.service.auth;

import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 密码哈希服务测试。
 */
class PasswordHashServiceTest {

    private final PasswordHashService service = new PasswordHashService(new BCryptPasswordEncoder());

    @Test
    void hashShouldStoreBcryptInsteadOfPlainText() {
        String rawPassword = "Ratel123!";
        String hash = service.hash(rawPassword);

        assertThat(hash).isNotEqualTo(rawPassword);
        assertThat(hash).startsWith("$2");
        assertThat(service.matches(rawPassword, hash)).isTrue();
        assertThat(service.requiresUpgrade(hash)).isFalse();
    }

    @Test
    void legacyPlainTextShouldMatchAndRequireUpgrade() {
        String rawPassword = "legacyPassword";

        assertThat(service.matches(rawPassword, rawPassword)).isTrue();
        assertThat(service.requiresUpgrade(rawPassword)).isTrue();
    }
}
