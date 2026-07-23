package com.ratel.fm.domain.auth;

import com.ratel.fm.common.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import org.hibernate.annotations.Comment;

import java.time.OffsetDateTime;

/**
 * 人员登录会话，用于控制同一所属公司、同一身份证、同一终端类型的唯一在线登录。
 *
 * <p>开发组织：ratel；开发人员：WenZhang；联系方式：18782945613。</p>
 *
 * @author ratel
 */
@Entity
@Table(name = "fm_user_login_sessions")
@Comment("用户登录会话表，支持同一身份证同一终端类型唯一在线登录")
public class UserLoginSession extends BaseEntity {

    /** 登录会话唯一 ID，写入 JWT，用于把令牌和数据库会话绑定起来。 */
    @Column(nullable = false, unique = true, length = 80)
    @Comment("登录会话唯一ID")
    private String sessionId;

    /** 登录人员主键，便于按人员追踪和校验会话归属。 */
    @Column(nullable = false)
    @Comment("登录人员主键")
    private Long userId;

    /** 登录用户名，保留登录当时的账号信息，便于审计排查。 */
    @Column(nullable = false, length = 80)
    @Comment("登录用户名")
    private String username;

    /** 登录人员姓名，保留登录当时的人员信息。 */
    @Column(nullable = false, length = 120)
    @Comment("登录人员姓名")
    private String realName;

    /** 身份证号，同一身份证号加同一终端类型只允许一个 ACTIVE 会话。 */
    @Column(nullable = false, length = 40)
    @Comment("登录人员身份证号")
    private String identityNo;

    /** 所属公司字典编码，登录会话唯一性和强制下线均按公司维度隔离。 */
    @Column(nullable = false, length = 80)
    @Comment("所属公司字典编码，作为登录会话账套隔离标识")
    private String organizationCode;

    /** 终端类型：PC 或 APP。唯一登录控制按该字段分组。 */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    @Comment("终端类型：PC或APP")
    private TerminalType terminalType;

    /** 终端唯一标识。PC 为请求 IP，APP 为手机号。 */
    @Column(nullable = false, length = 120)
    @Comment("终端唯一标识，PC为IP，APP为手机号")
    private String terminalIdentifier;

    /** 会话状态。旧登录被挤掉后会置为 FORCE_LOGOUT，退出置为 LOGOUT，过期置为 EXPIRED。 */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    @Comment("会话状态")
    private LoginSessionStatus status = LoginSessionStatus.ACTIVE;

    /** 登录成功时间，用于审计登录行为。 */
    @Column(nullable = false)
    @Comment("登录成功时间")
    private OffsetDateTime loginTime;

    /** 会话过期时间，和 JWT 过期时间保持同步，自动续期时会同步刷新。 */
    @Column(nullable = false)
    @Comment("会话过期时间")
    private OffsetDateTime expiresAt;

    /** 会话结束时间，退出、强制下线、过期时写入。 */
    @Comment("会话结束时间")
    private OffsetDateTime logoutTime;

    /**
     * 执行 getSessionId 方法。
     * 
     * <p>实现步骤：
     * 1. 接收并校验调用方传入的数据；
     * 2. 按当前方法职责执行业务查询、转换或持久化处理；
     * 3. 返回处理结果或更新对象状态。</p>
     */
    public String getSessionId() { return sessionId; }
    /**
     * 执行 setSessionId 方法。
     * 
     * <p>实现步骤：
     * 1. 接收并校验调用方传入的数据；
     * 2. 按当前方法职责执行业务查询、转换或持久化处理；
     * 3. 返回处理结果或更新对象状态。</p>
     */
    public void setSessionId(String sessionId) { this.sessionId = sessionId; }
    /**
     * 执行 getUserId 方法。
     * 
     * <p>实现步骤：
     * 1. 接收并校验调用方传入的数据；
     * 2. 按当前方法职责执行业务查询、转换或持久化处理；
     * 3. 返回处理结果或更新对象状态。</p>
     */
    public Long getUserId() { return userId; }
    /**
     * 执行 setUserId 方法。
     * 
     * <p>实现步骤：
     * 1. 接收并校验调用方传入的数据；
     * 2. 按当前方法职责执行业务查询、转换或持久化处理；
     * 3. 返回处理结果或更新对象状态。</p>
     */
    public void setUserId(Long userId) { this.userId = userId; }
    /**
     * 执行 getUsername 方法。
     * 
     * <p>实现步骤：
     * 1. 接收并校验调用方传入的数据；
     * 2. 按当前方法职责执行业务查询、转换或持久化处理；
     * 3. 返回处理结果或更新对象状态。</p>
     */
    public String getUsername() { return username; }
    /**
     * 执行 setUsername 方法。
     * 
     * <p>实现步骤：
     * 1. 接收并校验调用方传入的数据；
     * 2. 按当前方法职责执行业务查询、转换或持久化处理；
     * 3. 返回处理结果或更新对象状态。</p>
     */
    public void setUsername(String username) { this.username = username; }
    /**
     * 执行 getRealName 方法。
     * 
     * <p>实现步骤：
     * 1. 接收并校验调用方传入的数据；
     * 2. 按当前方法职责执行业务查询、转换或持久化处理；
     * 3. 返回处理结果或更新对象状态。</p>
     */
    public String getRealName() { return realName; }
    /**
     * 执行 setRealName 方法。
     * 
     * <p>实现步骤：
     * 1. 接收并校验调用方传入的数据；
     * 2. 按当前方法职责执行业务查询、转换或持久化处理；
     * 3. 返回处理结果或更新对象状态。</p>
     */
    public void setRealName(String realName) { this.realName = realName; }
    /**
     * 执行 getIdentityNo 方法。
     * 
     * <p>实现步骤：
     * 1. 接收并校验调用方传入的数据；
     * 2. 按当前方法职责执行业务查询、转换或持久化处理；
     * 3. 返回处理结果或更新对象状态。</p>
     */
    public String getIdentityNo() { return identityNo; }
    /**
     * 执行 setIdentityNo 方法。
     * 
     * <p>实现步骤：
     * 1. 接收并校验调用方传入的数据；
     * 2. 按当前方法职责执行业务查询、转换或持久化处理；
     * 3. 返回处理结果或更新对象状态。</p>
     */
    public void setIdentityNo(String identityNo) { this.identityNo = identityNo; }
    /**
     * 获取登录会话所属公司编码。
     *
     * <p>实现步骤：直接返回登录成功时写入的所属公司字典编码，供重复登录和 JWT 会话校验使用。</p>
     */
    public String getOrganizationCode() { return organizationCode; }
    /**
     * 设置登录会话所属公司编码。
     *
     * <p>实现步骤：保存登录请求选择的所属公司编码，使同一身份证在不同公司可以分别在线。</p>
     */
    public void setOrganizationCode(String organizationCode) { this.organizationCode = organizationCode; }
    /**
     * 执行 getTerminalType 方法。
     * 
     * <p>实现步骤：
     * 1. 接收并校验调用方传入的数据；
     * 2. 按当前方法职责执行业务查询、转换或持久化处理；
     * 3. 返回处理结果或更新对象状态。</p>
     */
    public TerminalType getTerminalType() { return terminalType; }
    /**
     * 执行 setTerminalType 方法。
     * 
     * <p>实现步骤：
     * 1. 接收并校验调用方传入的数据；
     * 2. 按当前方法职责执行业务查询、转换或持久化处理；
     * 3. 返回处理结果或更新对象状态。</p>
     */
    public void setTerminalType(TerminalType terminalType) { this.terminalType = terminalType; }
    /**
     * 执行 getTerminalIdentifier 方法。
     * 
     * <p>实现步骤：
     * 1. 接收并校验调用方传入的数据；
     * 2. 按当前方法职责执行业务查询、转换或持久化处理；
     * 3. 返回处理结果或更新对象状态。</p>
     */
    public String getTerminalIdentifier() { return terminalIdentifier; }
    /**
     * 执行 setTerminalIdentifier 方法。
     * 
     * <p>实现步骤：
     * 1. 接收并校验调用方传入的数据；
     * 2. 按当前方法职责执行业务查询、转换或持久化处理；
     * 3. 返回处理结果或更新对象状态。</p>
     */
    public void setTerminalIdentifier(String terminalIdentifier) { this.terminalIdentifier = terminalIdentifier; }
    /**
     * 执行 getStatus 方法。
     * 
     * <p>实现步骤：
     * 1. 接收并校验调用方传入的数据；
     * 2. 按当前方法职责执行业务查询、转换或持久化处理；
     * 3. 返回处理结果或更新对象状态。</p>
     */
    public LoginSessionStatus getStatus() { return status; }
    /**
     * 执行 setStatus 方法。
     * 
     * <p>实现步骤：
     * 1. 接收并校验调用方传入的数据；
     * 2. 按当前方法职责执行业务查询、转换或持久化处理；
     * 3. 返回处理结果或更新对象状态。</p>
     */
    public void setStatus(LoginSessionStatus status) { this.status = status; }
    /**
     * 执行 getLoginTime 方法。
     * 
     * <p>实现步骤：
     * 1. 接收并校验调用方传入的数据；
     * 2. 按当前方法职责执行业务查询、转换或持久化处理；
     * 3. 返回处理结果或更新对象状态。</p>
     */
    public OffsetDateTime getLoginTime() { return loginTime; }
    /**
     * 执行 setLoginTime 方法。
     * 
     * <p>实现步骤：
     * 1. 接收并校验调用方传入的数据；
     * 2. 按当前方法职责执行业务查询、转换或持久化处理；
     * 3. 返回处理结果或更新对象状态。</p>
     */
    public void setLoginTime(OffsetDateTime loginTime) { this.loginTime = loginTime; }
    /**
     * 执行 getExpiresAt 方法。
     * 
     * <p>实现步骤：
     * 1. 接收并校验调用方传入的数据；
     * 2. 按当前方法职责执行业务查询、转换或持久化处理；
     * 3. 返回处理结果或更新对象状态。</p>
     */
    public OffsetDateTime getExpiresAt() { return expiresAt; }
    /**
     * 执行 setExpiresAt 方法。
     * 
     * <p>实现步骤：
     * 1. 接收并校验调用方传入的数据；
     * 2. 按当前方法职责执行业务查询、转换或持久化处理；
     * 3. 返回处理结果或更新对象状态。</p>
     */
    public void setExpiresAt(OffsetDateTime expiresAt) { this.expiresAt = expiresAt; }
    /**
     * 执行 getLogoutTime 方法。
     * 
     * <p>实现步骤：
     * 1. 接收并校验调用方传入的数据；
     * 2. 按当前方法职责执行业务查询、转换或持久化处理；
     * 3. 返回处理结果或更新对象状态。</p>
     */
    public OffsetDateTime getLogoutTime() { return logoutTime; }
    /**
     * 执行 setLogoutTime 方法。
     * 
     * <p>实现步骤：
     * 1. 接收并校验调用方传入的数据；
     * 2. 按当前方法职责执行业务查询、转换或持久化处理；
     * 3. 返回处理结果或更新对象状态。</p>
     */
    public void setLogoutTime(OffsetDateTime logoutTime) { this.logoutTime = logoutTime; }
}
