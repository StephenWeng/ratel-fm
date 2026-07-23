package com.ratel.fm.security;

import com.ratel.fm.domain.auth.PermissionCode;

import java.time.OffsetDateTime;
import java.util.Set;

/**
 * 当前登录人员上下文。
 *
 * <p>开发组织：ratel；开发人员：WenZhang；联系方式：18782945613。</p>
 *
 * @author ratel
 */
public record CurrentUser(
        /**
         * 记录组件 id：表示接口入参或出参中的 id 字段。
         */
        Long id,
        /**
         * 记录组件 username：表示接口入参或出参中的 username 字段。
         */
        String username,
        /**
         * 记录组件 realName：表示接口入参或出参中的 realName 字段。
         */
        String realName,
        /**
         * 记录组件 identityNo：表示接口入参或出参中的 identityNo 字段。
         */
        String identityNo,
        /**
         * 记录组件 department：表示接口入参或出参中的 department 字段。
         */
        String department,
        /**
         * 记录组件 organizationCode：表示接口入参或出参中的 organizationCode 字段。
         */
        String organizationCode,
        /**
         * 记录组件 position：表示接口入参或出参中的 position 字段。
         */
        String position,
        /**
         * 记录组件 contactPhone：表示接口入参或出参中的 contactPhone 字段。
         */
        String contactPhone,
        /**
         * 记录组件 terminalType：表示接口入参或出参中的 terminalType 字段。
         */
        String terminalType,
        /**
         * 记录组件 terminalIdentifier：表示接口入参或出参中的 terminalIdentifier 字段。
         */
        String terminalIdentifier,
        /**
         * 记录组件 sessionId：表示接口入参或出参中的 sessionId 字段。
         */
        String sessionId,
        /**
         * 记录组件 expiresAt：表示接口入参或出参中的 expiresAt 字段。
         */
        OffsetDateTime expiresAt,
        /**
         * 记录组件 permissions：表示接口入参或出参中的 permissions 字段。
         */
        Set<PermissionCode> permissions
) {
}
