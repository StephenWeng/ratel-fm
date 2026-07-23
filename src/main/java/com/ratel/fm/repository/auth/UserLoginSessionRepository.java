package com.ratel.fm.repository.auth;

import com.ratel.fm.domain.auth.LoginSessionStatus;
import com.ratel.fm.domain.auth.TerminalType;
import com.ratel.fm.domain.auth.UserLoginSession;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

/**
 * 人员登录会话数据访问。
 *
 * <p>开发组织：ratel；开发人员：WenZhang；联系方式：18782945613。</p>
 *
 * @author ratel
 */
public interface UserLoginSessionRepository extends JpaRepository<UserLoginSession, Long> {

    /**
     * 根据 JWT 中携带的会话 ID 查询登录会话。
     */
    Optional<UserLoginSession> findBySessionId(String sessionId);

    /**
     * 查询同一所属公司、同一身份证号、同一终端类型、指定状态的登录会话。
     *
     * <p>登录时用它发现当前公司内是否已经存在 ACTIVE 会话，从而触发重复登录确认或强制下线。</p>
     */
    List<UserLoginSession> findByOrganizationCodeAndIdentityNoAndTerminalTypeAndStatus(
            String organizationCode,
            String identityNo,
            TerminalType terminalType,
            LoginSessionStatus status
    );
}
