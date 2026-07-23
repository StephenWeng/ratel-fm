package com.ratel.fm.service.auth;

import com.ratel.fm.common.BusinessException;
import com.ratel.fm.common.ResponseCode;
import com.ratel.fm.common.SearchSpecs;
import com.ratel.fm.domain.auth.PermissionCode;
import com.ratel.fm.domain.auth.LoginSessionStatus;
import com.ratel.fm.domain.auth.MenuType;
import com.ratel.fm.domain.auth.Role;
import com.ratel.fm.domain.auth.SystemMenu;
import com.ratel.fm.domain.auth.TerminalType;
import com.ratel.fm.domain.auth.UserAccount;
import com.ratel.fm.domain.auth.UserLoginSession;
import com.ratel.fm.domain.auth.UserMenuUsage;
import com.ratel.fm.domain.basic.BasicDictionary;
import com.ratel.fm.repository.basic.BasicDictionaryRepository;
import com.ratel.fm.repository.auth.RoleRepository;
import com.ratel.fm.repository.auth.SystemMenuRepository;
import com.ratel.fm.repository.auth.UserAccountRepository;
import com.ratel.fm.repository.auth.UserLoginSessionRepository;
import com.ratel.fm.repository.auth.UserMenuUsageRepository;
import com.ratel.fm.security.CompanyScope;
import com.ratel.fm.security.CurrentUser;
import com.ratel.fm.security.JwtTokenService;
import com.ratel.fm.security.SecurityUtils;
import com.ratel.fm.service.audit.AuditLogService;
import com.ratel.fm.web.dto.auth.AuthDtos.LoginRequest;
import com.ratel.fm.web.dto.auth.AuthDtos.LoginResponse;
import com.ratel.fm.web.dto.auth.AuthDtos.MenuRequest;
import com.ratel.fm.web.dto.auth.AuthDtos.MenuUsageRequest;
import com.ratel.fm.web.dto.auth.AuthDtos.MenuUsageView;
import com.ratel.fm.web.dto.auth.AuthDtos.MenuView;
import com.ratel.fm.web.dto.auth.AuthDtos.PasswordChangeRequest;
import com.ratel.fm.web.dto.auth.AuthDtos.ProfileUpdateRequest;
import com.ratel.fm.web.dto.auth.AuthDtos.RoleRequest;
import com.ratel.fm.web.dto.auth.AuthDtos.RoleView;
import com.ratel.fm.web.dto.auth.AuthDtos.UserCreateRequest;
import com.ratel.fm.web.dto.auth.AuthDtos.UserUpdateRequest;
import com.ratel.fm.web.dto.auth.AuthDtos.UserView;
import org.springframework.http.HttpStatus;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.time.OffsetDateTime;
import java.util.Objects;
import java.util.UUID;

/**
 * 人员认证、会话、人员档案和角色授权服务。
 *
 * <p>开发组织：ratel；开发人员：WenZhang；联系方式：18782945613。</p>
 *
 * @author ratel
 */
@Service
public class AuthService {

    /**
     * 个人中心固定菜单编码集合，所有已登录用户都应具备这些基础个人操作入口。
     */
    private static final Set<String> PERSONAL_MENU_CODES = Set.of(
            "MODULE_PERSONAL",
            "PAGE_PROFILE",
            "BTN_PROFILE_EDIT",
            "BTN_PROFILE_PASSWORD",
            "BTN_PROFILE_AVATAR",
            "BTN_LOGOUT"
    );

    /**
     * 字段 userRepository：保存 userRepository 对应的业务数据、运行配置或依赖对象，供本类逻辑读取和维护。
     */
    private final UserAccountRepository userRepository;
    /**
     * 字段 roleRepository：保存 roleRepository 对应的业务数据、运行配置或依赖对象，供本类逻辑读取和维护。
     */
    private final RoleRepository roleRepository;
    /**
     * 字段 menuRepository：保存 menuRepository 对应的业务数据、运行配置或依赖对象，供本类逻辑读取和维护。
     */
    private final SystemMenuRepository menuRepository;
    /**
     * 字段 passwordHashService：统一处理登录密码哈希生成、校验和历史明文升级。
     */
    private final PasswordHashService passwordHashService;
    /**
     * 字段 jwtTokenService：保存 jwtTokenService 对应的业务数据、运行配置或依赖对象，供本类逻辑读取和维护。
     */
    private final JwtTokenService jwtTokenService;
    /**
     * 字段 sessionRepository：保存 sessionRepository 对应的业务数据、运行配置或依赖对象，供本类逻辑读取和维护。
     */
    private final UserLoginSessionRepository sessionRepository;
    /** 用户常用菜单统计仓储。 */
    private final UserMenuUsageRepository menuUsageRepository;
    /**
     * 字段 dictionaryRepository：保存 dictionaryRepository 对应的业务数据、运行配置或依赖对象，供本类逻辑读取和维护。
     */
    private final BasicDictionaryRepository dictionaryRepository;
    /**
     * 字段 auditLogService：保存 auditLogService 对应的业务数据、运行配置或依赖对象，供本类逻辑读取和维护。
     */
    private final AuditLogService auditLogService;
    /**
     * 字段 defaultAdminIdentityNo：保存 defaultAdminIdentityNo 对应的业务数据、运行配置或依赖对象，供本类逻辑读取和维护。
     */
    private final String defaultAdminIdentityNo;

    /**
     * 构造 AuthService 实例。
     * 
     * <p>实现步骤：
     * 1. 接收调用方传入的依赖对象或初始化参数；
     * 2. 保存到成员字段，保证后续业务方法可以复用；
     * 3. 完成实例初始化。</p>
     */
    public AuthService(
            UserAccountRepository userRepository,
            RoleRepository roleRepository,
            SystemMenuRepository menuRepository,
            PasswordHashService passwordHashService,
            JwtTokenService jwtTokenService,
            UserLoginSessionRepository sessionRepository,
            UserMenuUsageRepository menuUsageRepository,
            BasicDictionaryRepository dictionaryRepository,
            AuditLogService auditLogService,
            @Value("${app.bootstrap.admin-identity-no:ADMIN_IDENTITY_0001}") String defaultAdminIdentityNo
    ) {
        this.userRepository = userRepository;
        this.roleRepository = roleRepository;
        this.menuRepository = menuRepository;
        this.passwordHashService = passwordHashService;
        this.jwtTokenService = jwtTokenService;
        this.sessionRepository = sessionRepository;
        this.menuUsageRepository = menuUsageRepository;
        this.dictionaryRepository = dictionaryRepository;
        this.auditLogService = auditLogService;
        this.defaultAdminIdentityNo = defaultAdminIdentityNo;
    }

    /**
     * 执行账号密码登录，并处理同一身份证同一终端类型的唯一登录控制。
     *
     * <p>实现步骤：
     * 1. 按身份证号优先、账号唯一的规则解析登录人员，并校验启用状态和密码；
     * 2. 解析终端类型和终端标识，PC 端使用客户端 IP，APP 端使用手机号；
     * 3. 查询同身份证、同终端类型的有效会话，发现冲突且未强制登录时返回重复登录提示；
     * 4. 若允许强制登录，将旧会话置为 FORCE_LOGOUT；
     * 5. 创建新登录会话，签发 JWT，并返回给 Controller 写入 Cookie。</p>
     */
    @Transactional
    public LoginIssue login(LoginRequest request, String clientIp) {
        // 变量说明：terminalType 保存当前步骤计算、查询或转换得到的中间结果。
        TerminalType terminalType = request.terminalType() == null ? TerminalType.PC : request.terminalType();
        // 步骤1：先解析并校验登录所属公司，停用账套不允许继续匹配账号。
        BasicDictionary loginCompany = resolveLoginCompany(request.organizationCode());
        String organizationCode = loginCompany.getCode();
        // 步骤1：身份证号和账号都在同一公司内唯一；登录输入可以填写任意一种。账号不存在也要记录失败登录审计。
        UserAccount user;
        try {
            user = resolveLoginUser(organizationCode, request.username());
        } catch (BadCredentialsException ex) {
            // 变量说明：attemptedTerminalIdentifier 保存当前步骤计算、查询或转换得到的中间结果。
            String attemptedTerminalIdentifier = resolveTerminalIdentifierForLog(request.terminalIdentifier(), terminalType, clientIp, null);
            auditLogService.recordLogin(
                    null,
                    request.username(),
                    terminalType.name(),
                    attemptedTerminalIdentifier,
                    "LOGIN_FAILED",
                    "organizationCode=" + organizationCode + ", loginName=" + request.username()
                            + ", terminalType=" + terminalType + ", terminalIdentifier=" + attemptedTerminalIdentifier,
                    "FAILED",
                    "账号不存在或密码错误",
                    "人员登录失败，未签发JWT"
            );
            throw ex;
        }
        // 变量说明：terminalIdentifier 保存当前步骤计算、查询或转换得到的中间结果。
        String terminalIdentifier = resolveTerminalIdentifierForLog(request.terminalIdentifier(), terminalType, clientIp, user);
        if (!user.isEnabled()) {
            String disabledMessage = "账号【" + user.getUsername() + "】已停用，请联系贵公司管理员";
            recordLoginAttempt(user, request, terminalType, terminalIdentifier, "LOGIN_FAILED", "FAILED", disabledMessage, "禁用人员登录被拒绝");
            throw new BusinessException(ResponseCode.NO_AUTH, disabledMessage);
        }
        // 步骤1：使用统一密码哈希服务校验密码，密码错误统一抛出认证异常。
        if (!passwordHashService.matches(request.password(), user.getPasswordHash())) {
            recordLoginAttempt(user, request, terminalType, terminalIdentifier, "LOGIN_FAILED", "FAILED", "密码错误", "人员登录失败，未签发JWT");
            throw new BadCredentialsException("Bad credentials");
        }
        // 步骤1：兼容历史明文密码；只要登录成功就立即升级为 BCrypt，后续不再明文存储。
        if (passwordHashService.requiresUpgrade(user.getPasswordHash())) {
            user.setPasswordHash(passwordHashService.hash(request.password()));
        }
        // 步骤2：确定终端类型和终端标识，后续写入 JWT 和登录会话表。
        try {
            terminalIdentifier = resolveTerminalIdentifier(request.terminalIdentifier(), terminalType, clientIp, user);
        } catch (BusinessException ex) {
            recordLoginAttempt(user, request, terminalType, terminalIdentifier, "LOGIN_FAILED", "FAILED", ex.getMessage(), "终端标识不完整，登录被拒绝");
            throw ex;
        }
        if (user.getIdentityNo() == null || user.getIdentityNo().isBlank()) {
            recordLoginAttempt(user, request, terminalType, terminalIdentifier, "LOGIN_FAILED", "FAILED", "用户信息为空", "人员身份证缺失，登录被拒绝");
            throw new BusinessException(ResponseCode.USER_INFO_NULL);
        }
        // 步骤3：查询同公司、同身份证、同终端类型的 ACTIVE 会话，并先剔除已经过期的会话。
        List<UserLoginSession> activeSessions = sessionRepository.findByOrganizationCodeAndIdentityNoAndTerminalTypeAndStatus(
                organizationCode, user.getIdentityNo(), terminalType, LoginSessionStatus.ACTIVE);
        activeSessions = clearExpiredSessions(activeSessions);
        // 变量说明：conflict 保存当前步骤计算、查询或转换得到的中间结果。
        boolean conflict = !activeSessions.isEmpty();
        // 步骤3：有冲突且本次不是强制登录时，不签发 JWT，只返回重复登录提醒给前端二次确认。
        if (conflict && !Boolean.TRUE.equals(request.force())) {
            recordLoginAttempt(user, request, terminalType, terminalIdentifier, "LOGIN_REPEAT", "FAILED",
                    "重复登录待确认", "同一身份证同一终端类型已有有效登录，本次未签发JWT");
            LoginResponse response = new LoginResponse(
                    null,
                    toUserView(user),
                    true,
                    "当前人员已在" + terminalType.name() + "终端登录，是否挤掉之前登录者？"
            );
            return new LoginIssue(response, null, 0);
        }
        // 步骤4：确认强制登录或无冲突时，将旧 ACTIVE 会话置为强制下线，旧令牌后续请求会返回 FORCE_LOGOUT。
        if (conflict) {
            recordLoginAttempt(user, request, terminalType, terminalIdentifier, "LOGIN_FORCE", "SUCCESS",
                    "强制登录", "剔除旧会话数量: " + activeSessions.size());
        }
        activeSessions.forEach(this::forceLogout);
        // 步骤5：生成新会话 ID，并把会话 ID、终端信息写入 JWT，后续请求按会话表校验。
        String sessionId = UUID.randomUUID().toString();
        JwtTokenService.TokenIssue issue = jwtTokenService.issue(
                toCurrentUser(user, null, terminalType.name(), terminalIdentifier, sessionId)
        );
        // 步骤5：落库新登录会话，数据库会话过期时间和 JWT 过期时间保持一致。
        UserLoginSession session = new UserLoginSession();
        session.setSessionId(sessionId);
        session.setUserId(user.getId());
        session.setUsername(user.getUsername());
        session.setRealName(user.getRealName());
        session.setIdentityNo(user.getIdentityNo());
        session.setOrganizationCode(organizationCode);
        session.setTerminalType(terminalType);
        session.setTerminalIdentifier(terminalIdentifier);
        session.setLoginTime(OffsetDateTime.now());
        session.setExpiresAt(issue.expiresAt());
        sessionRepository.save(session);
        recordLoginAttempt(user, request, terminalType, terminalIdentifier, "LOGIN_SUCCESS", "SUCCESS",
                "expiresAt=" + issue.expiresAt(), "签发JWT并创建登录会话");
        return new LoginIssue(
                new LoginResponse(issue.expiresAt(), toUserView(user), false, null),
                issue.token(),
                jwtTokenService.cookieMaxAgeSeconds()
        );
    }

    /**
     * 退出当前登录会话。
     *
     * <p>实现步骤：
     * 1. 空会话 ID 直接忽略；
     * 2. 找到会话后置为 LOGOUT；
     * 3. 写入退出时间，便于后续审计。</p>
     */
    @Transactional
    public void logout(String sessionId) {
        if (sessionId == null || sessionId.isBlank()) {
            return;
        }
        sessionRepository.findBySessionId(sessionId).ifPresent(session -> {
            session.setStatus(LoginSessionStatus.LOGOUT);
            session.setLogoutTime(OffsetDateTime.now());
        });
    }

    /**
     * 查询所有系统人员，并转换为前端展示视图。
     */
    @Transactional(readOnly = true)
    public List<UserView> listUsers() {
        return listUsers(null, null, null, null, null, null, null, null, null);
    }

    /**
     * 按字段查询系统人员。
     *
     * <p>实现步骤：
     * 1. 账号、姓名、身份证、手机号、邮箱等输入框字段按包含匹配；
     * 2. admin 可按所属公司筛选，非 admin 强制使用当前登录公司；
     * 3. 部门、岗位、启用状态等确定性条件按等值匹配；
     * 4. 按修改时间倒序返回，保证最新维护人员优先展示。</p>
     */
    @Transactional(readOnly = true)
    public List<UserView> listUsers(
            String username,
            String realName,
            String identityNo,
            String phone,
            String email,
            String department,
            String organizationCode,
            String position,
            Boolean enabled
    ) {
        // 步骤2：人员数据按所属公司隔离；只有默认 admin 可以跨公司维护人员档案。
        String effectiveOrganizationCode = CompanyScope.isSuperAdmin()
                ? firstText(organizationCode, null)
                : CompanyScope.currentCompanyCode();
        /**
         * 人员列表查询条件，组合所属公司隔离、账号、姓名、证件号和启停状态筛选。
         */
        var spec = SearchSpecs.<UserAccount>like("username", username)
                .and(SearchSpecs.like("realName", realName))
                .and(SearchSpecs.like("identityNo", identityNo))
                .and(SearchSpecs.like("phone", phone))
                .and(SearchSpecs.like("email", email))
                .and(SearchSpecs.equal("department", firstText(department, null)))
                .and(SearchSpecs.equal("organizationCode", effectiveOrganizationCode))
                .and(SearchSpecs.equal("position", firstText(position, null)))
                .and(SearchSpecs.equal("enabled", enabled));
        return userRepository.findAll(spec, Sort.by(Sort.Direction.DESC, "modifyTime", "id")).stream()
                .map(this::toUserView)
                .toList();
    }

    /**
     * 新增系统人员。
     *
     * <p>实现步骤：
     * 1. 解析所属公司，admin 可指定，非 admin 默认当前登录公司；
     * 2. 校验登录账号和身份证号在所属公司内唯一；
     * 3. 使用 BCrypt 加密初始密码；
     * 4. 写入人员基础字段和角色授权；
     * 5. 记录关键操作日志，便于审计人员追踪授权变化。</p>
     */
    @Transactional
    public UserView createUser(UserCreateRequest request) {
        // 步骤1：所属公司决定账套隔离边界，非 admin 只能在当前公司内新增人员。
        String organizationCode = resolveEditableCompanyCode(request.organizationCode(), "所属公司");
        // 步骤2：登录账号和身份证号在同一所属公司内必须唯一，避免登录和唯一在线判断发生歧义。
        String username = normalizeRequired(request.username(), "登录账号不能为空");
        // 变量说明：identityNo 保存当前步骤计算、查询或转换得到的中间结果。
        String identityNo = normalizeRequired(request.identityNo(), "身份证号不能为空");
        if (userRepository.existsByOrganizationCodeAndUsername(organizationCode, username)) {
            throw new BusinessException(ResponseCode.OBJ_BEEN_USED, "登录账号已被当前所属公司的其他人员使用");
        }
        if (userRepository.existsByOrganizationCodeAndIdentityNo(organizationCode, identityNo)) {
            throw new BusinessException(ResponseCode.OBJ_BEEN_USED, "身份证号已被当前所属公司的其他人员使用");
        }
        // 步骤3-4：创建人员实体，密码只保存哈希值，角色由编码解析成实体集合。
        UserAccount user = new UserAccount();
        user.setUsername(username);
        user.setPasswordHash(passwordHashService.hash(request.password()));
        applyUserFields(user, request.realName(), request.department(), organizationCode, request.position(),
                identityNo, request.phone(), request.email(), request.avatarUrl(), request.enabled(), request.roleCodes());
        // 变量说明：view 保存当前步骤计算、查询或转换得到的中间结果。
        UserView view = toUserView(userRepository.save(user));
        // 步骤5：人员新增属于关键授权操作，必须落库审计。
        auditLogService.record("CREATE_USER", safeUserRequest(request), "SUCCESS",
                "人员管理新增了用户" + displayUser(view) + "。");
        return view;
    }

    /**
     * 修改系统人员。
     *
     * <p>实现步骤：
     * 1. 根据主键读取人员，不存在则返回业务异常；
     * 2. 如果请求携带新密码，则重新 BCrypt 加密；
     * 3. 解析目标所属公司，admin 可调整，非 admin 只能保持当前公司；
     * 4. 校验身份证号没有被同一所属公司其他人员占用；
     * 5. 更新人员字段和角色授权；
     * 6. 记录关键操作日志。</p>
     */
    @Transactional
    public UserView updateUser(Long id, UserUpdateRequest request) {
        // 步骤1：修改必须基于已存在的人员记录。
        UserAccount user = userRepository.findById(id)
                .orElseThrow(() -> new BusinessException(HttpStatus.NOT_FOUND, "人员不存在"));
        assertUserCompanyManageable(user);
        // 步骤2：密码为空表示不修改密码，非空才重新计算哈希。
        if (request.password() != null && !request.password().isBlank()) {
            user.setPasswordHash(passwordHashService.hash(request.password()));
        }
        // 步骤3：所属公司是账号和身份证唯一性边界，非 admin 不能跨公司改人或调公司。
        String organizationCode = resolveEditableCompanyCode(request.organizationCode(), "所属公司");
        // 步骤4：身份证号允许保持原值，但不能改成当前所属公司其他人员已使用的值。
        String identityNo = normalizeRequired(request.identityNo(), "身份证号不能为空");
        userRepository.findByOrganizationCodeAndIdentityNo(organizationCode, identityNo)
                .filter(existing -> !existing.getId().equals(id))
                .ifPresent(existing -> {
                    throw new BusinessException(ResponseCode.OBJ_BEEN_USED, "身份证号已被当前所属公司的其他人员使用");
                });
        userRepository.findByOrganizationCodeAndUsername(organizationCode, user.getUsername())
                .filter(existing -> !existing.getId().equals(id))
                .ifPresent(existing -> {
                    throw new BusinessException(ResponseCode.OBJ_BEEN_USED, "登录账号已被当前所属公司的其他人员使用");
                });
        // 步骤5：统一复用字段赋值逻辑，避免新增和修改字段口径不一致。
        applyUserFields(user, request.realName(), request.department(), organizationCode, request.position(),
                identityNo, request.phone(), request.email(), request.avatarUrl(), request.enabled(), request.roleCodes());
        // 变量说明：view 保存当前步骤计算、查询或转换得到的中间结果。
        UserView view = toUserView(user);
        // 步骤6：人员修改可能改变权限、状态或登录基础信息，必须记录审计日志。
        auditLogService.record("UPDATE_USER", "userId=" + id + ", " + safeUserRequest(request), "SUCCESS",
                "人员管理修改了用户" + displayUser(view) + "的信息。");
        return view;
    }

    /**
     * 删除系统人员。
     *
     * <p>实现步骤：
     * 1. 校验人员存在；
     * 2. 删除人员记录；
     * 3. 记录删除审计日志。</p>
     */
    @Transactional
    public void deleteUser(Long id) {
        UserAccount user = userRepository.findById(id)
                .orElseThrow(() -> new BusinessException(HttpStatus.NOT_FOUND, "人员不存在"));
        assertUserCompanyManageable(user);
        if (isDefaultAccount(user)) {
            throw new BusinessException(ResponseCode.DELETE_FORBIDDEN, "默认管理员账号不允许删除");
        }
        userRepository.delete(user);
        auditLogService.record("DELETE_USER", "userId=" + id + ", username=" + user.getUsername(),
                "SUCCESS", "人员管理删除了用户" + displayUser(user) + "，该用户将不能继续登录系统。");
    }

    /**
     * 批量删除系统人员。
     *
     * <p>实现步骤：
     * 1. 清理并去重前端传入的人员 ID；
     * 2. 逐个执行单人删除逻辑，复用默认管理员保护和存在性校验；
     * 3. 在同一事务内执行，任一人员删除失败则整体回滚；
     * 4. 记录批量删除审计日志。</p>
     */
    @Transactional
    public void deleteUsers(List<Long> ids) {
        // 变量说明：deleteIds 保存当前步骤计算、查询或转换得到的中间结果。
        List<Long> deleteIds = normalizeBatchIds(ids);
        // 变量说明：users 保存当前步骤计算、查询或转换得到的中间结果。
        List<String> users = new java.util.ArrayList<>();
        for (Long id : deleteIds) {
            UserAccount user = userRepository.findById(id)
                    .orElseThrow(() -> new BusinessException(HttpStatus.NOT_FOUND, "人员不存在"));
            assertUserCompanyManageable(user);
            if (isDefaultAccount(user)) {
                throw new BusinessException(ResponseCode.DELETE_FORBIDDEN, "默认管理员账号不允许删除");
            }
            users.add(displayUser(user));
            userRepository.delete(user);
        }
        auditLogService.record("BATCH_DELETE_USERS", "userIds=" + deleteIds,
                "SUCCESS", "人员管理批量删除了用户: " + String.join("、", users) + "。");
    }

    /**
     * 查询角色列表，用于人员授权和角色维护页面。
     */
    @Transactional(readOnly = true)
    public List<RoleView> listRoles() {
        return roleRepository.findAll(Sort.by(Sort.Direction.DESC, "modifyTime", "id")).stream()
                .map(this::toRoleView)
                .toList();
    }

    /**
     * 新增或更新角色。
     *
     * <p>实现步骤：
     * 1. 按角色编码查找现有角色，不存在则新建；
     * 2. 写入角色名称、描述和菜单集合；
     * 3. 根据菜单绑定的权限码推导后端接口权限；
     * 4. 保存角色并记录授权变更审计日志。</p>
     */
    @Transactional
    public RoleView saveRole(RoleRequest request) {
        // 步骤1：角色编码是业务唯一键，保存接口同时承担新增和更新职责。
        Role role = roleRepository.findByCode(request.code()).orElseGet(Role::new);
        // 步骤2：角色直接关联菜单，菜单覆盖模块、页面和按钮。
        role.setCode(request.code());
        role.setName(request.name());
        role.setDescription(request.description());
        // 变量说明：menus 保存当前步骤计算、查询或转换得到的中间结果。
        Set<SystemMenu> menus = withAncestorMenus(resolveMenus(request.menuCodes()));
        menus.addAll(resolveMenus(PERSONAL_MENU_CODES));
        role.setMenus(menus);
        // 步骤3：后端方法鉴权继续使用权限码，权限码由菜单上绑定的 permissionCode 自动推导。
        role.setPermissions(resolvePermissions(menus));
        // 变量说明：view 保存当前步骤计算、查询或转换得到的中间结果。
        RoleView view = toRoleView(roleRepository.save(role));
        // 步骤4：角色授权变化会影响人员可访问模块，必须记录审计日志。
        auditLogService.record("SAVE_ROLE", request, "SUCCESS",
                "角色管理保存了角色" + view.name() + "(" + view.code() + ")的基本信息和菜单授权。");
        return view;
    }

    /**
     * 删除角色。
     *
     * <p>实现步骤：
     * 1. 校验角色存在；
     * 2. 删除角色；
     * 3. 记录审计日志，提示该操作会影响已分配角色人员的权限。</p>
     */
    @Transactional
    public void deleteRole(Long id) {
        Role role = roleRepository.findById(id)
                .orElseThrow(() -> new BusinessException(HttpStatus.NOT_FOUND, "角色不存在"));
        // 变量说明：roleText 保存当前步骤计算、查询或转换得到的中间结果。
        String roleText = role.getName() + "(" + role.getCode() + ")";
        roleRepository.delete(role);
        auditLogService.record("DELETE_ROLE", "roleId=" + id, "SUCCESS",
                "角色管理删除了角色" + roleText + "，已分配该角色的人员授权会受到影响。");
    }

    /**
     * 查询所有启用菜单，供角色授权页面选择模块、页面和按钮。
     */
    @Transactional(readOnly = true)
    public List<MenuView> listMenus() {
        return menuRepository.findByEnabledTrueOrderBySortOrderAscIdAsc().stream()
                .map(this::toMenuView)
                .toList();
    }

    /**
     * 查询全部菜单资源，供菜单管理页面维护模块、页面和按钮层级。
     */
    @Transactional(readOnly = true)
    public List<MenuView> listAllMenus() {
        return listAllMenus(null, null, null, null, null, null);
    }

    /**
     * 按字段查询全部菜单资源。
     *
     * <p>实现步骤：
     * 1. 菜单编码、名称、路由使用包含匹配；
     * 2. 菜单类型、后端权限码和启用状态使用等值匹配；
     * 3. 按排序号和主键排序，保持模块、页面、按钮授权资源展示稳定。</p>
     */
    @Transactional(readOnly = true)
    public List<MenuView> listAllMenus(
            String code,
            String name,
            MenuType type,
            String routePath,
            PermissionCode permissionCode,
            Boolean enabled
    ) {
        /**
         * 菜单资源查询条件，组合编码、名称、类型、路由、权限码和启停状态筛选。
         */
        var spec = SearchSpecs.<SystemMenu>like("code", code)
                .and(SearchSpecs.like("name", name))
                .and(SearchSpecs.equal("type", type))
                .and(SearchSpecs.like("routePath", routePath))
                .and(SearchSpecs.equal("permissionCode", permissionCode))
                .and(SearchSpecs.equal("enabled", enabled));
        return menuRepository.findAll(spec, Sort.by(Sort.Direction.ASC, "sortOrder", "id")).stream()
                .map(this::toMenuView)
                .toList();
    }

    /**
     * 新增或修改菜单资源。
     *
     * <p>实现步骤：
     * 1. 按菜单编码查询现有菜单，不存在则新建；
     * 2. 校验模块、页面、按钮的父子层级合法；
     * 3. 写入菜单基础信息、排序、启停和后端权限码；
     * 4. 记录菜单维护审计日志，便于追踪后续授权资源变化。</p>
     */
    @Transactional
    public MenuView saveMenu(MenuRequest request) {
        // 步骤1：菜单编码是前端授权匹配的稳定键，保存接口承担新增和编辑职责。
        SystemMenu menu = menuRepository.findByCode(request.code()).orElseGet(SystemMenu::new);
        // 步骤2：菜单类型必填，并按模块、页面、按钮关系解析父级菜单。
        if (request.type() == null) {
            throw new BusinessException(ResponseCode.ILLEGAL_PARAM, "菜单类型不能为空");
        }
        // 变量说明：menuType 保存当前步骤计算、查询或转换得到的中间结果。
        MenuType menuType = request.type();
        // 变量说明：parent 保存当前步骤计算、查询或转换得到的中间结果。
        SystemMenu parent = resolveMenuParent(menu.getId(), menuType, request.parentId());
        // 步骤3：写入菜单资源字段，排序为空时使用 0，启停为空时默认启用。
        menu.setCode(request.code());
        menu.setName(request.name());
        menu.setType(menuType);
        menu.setParent(parent);
        menu.setRoutePath(request.routePath());
        menu.setSortOrder(request.sortOrder() == null ? 0 : request.sortOrder());
        menu.setEnabled(request.enabled() == null || request.enabled());
        menu.setPermissionCode(request.permissionCode());
        // 变量说明：view 保存当前步骤计算、查询或转换得到的中间结果。
        MenuView view = toMenuView(menuRepository.save(menu));
        // 步骤4：菜单维护会影响角色授权范围和前端功能显隐，必须记录审计日志。
        auditLogService.record("SAVE_MENU", request, "SUCCESS",
                "菜单管理保存了菜单" + view.name() + "(" + view.code() + ")。");
        return view;
    }

    /**
     * 删除菜单资源。
     *
     * <p>实现步骤：
     * 1. 校验菜单存在；
     * 2. 若存在下级模块、页面或按钮，则禁止删除，避免角色授权树断层；
     * 3. 删除菜单并记录审计日志。</p>
     */
    @Transactional
    public void deleteMenu(Long id) {
        SystemMenu menu = menuRepository.findById(id)
                .orElseThrow(() -> new BusinessException(HttpStatus.NOT_FOUND, "菜单不存在"));
        if (menuRepository.existsByParentId(id)) {
            throw new BusinessException(ResponseCode.DELETE_FORBIDDEN, "存在下级菜单，不允许删除");
        }
        menuRepository.delete(menu);
        auditLogService.record("DELETE_MENU", "menuId=" + id + ", code=" + menu.getCode(), "SUCCESS",
                "菜单管理删除了菜单" + menu.getName() + "(" + menu.getCode() + ")，角色授权和前端功能显示会受到影响。");
    }

    /**
     * 查询指定人员当前拥有的授权菜单编码。
     *
     * <p>实现步骤：
     * 1. 根据当前登录人 ID 查询人员；
     * 2. 遍历人员角色关联的菜单资源；
     * 3. 仅返回启用菜单编码，供前端刷新页面后重新匹配渲染模块、页面和按钮。</p>
     */
    @Transactional(readOnly = true)
    public Set<String> listAuthorizedMenuCodes(Long currentUserId) {
        // 步骤1：人员不存在说明登录态已经失效，由统一异常响应处理。
        UserAccount user = userRepository.findById(currentUserId)
                .orElseThrow(() -> new BusinessException(ResponseCode.NO_TOKEN_ERROR, "认证人员不存在"));
        // 步骤2-3：菜单授权可能来自多个角色，使用 Set 去重。
        return authorizedMenuCodes(user);
    }

    /**
     * 查询指定人员当前拥有的授权菜单资源。
     *
     * <p>实现步骤：
     * 1. 根据当前登录人 ID 查询人员；
     * 2. 汇总人员角色中的启用菜单编码；
     * 3. 按菜单表排序读取启用菜单资源；
     * 4. 只返回当前人员拥有的菜单资源，前端据此按模块、页面、按钮层级渲染。</p>
     */
    @Transactional(readOnly = true)
    public List<MenuView> listAuthorizedMenus(Long currentUserId) {
        UserAccount user = userRepository.findById(currentUserId)
                .orElseThrow(() -> new BusinessException(ResponseCode.NO_TOKEN_ERROR, "认证人员不存在"));
        // 变量说明：menuCodes 保存当前步骤计算、查询或转换得到的中间结果。
        Set<String> menuCodes = authorizedMenuCodes(user);
        return menuRepository.findByEnabledTrueOrderBySortOrderAscIdAsc().stream()
                .filter(menu -> menuCodes.contains(menu.getCode()))
                .map(this::toMenuView)
                .toList();
    }

    /**
     * 记录当前登录人进入一次功能菜单。
     *
     * <p>实现步骤：
     * 1. 校验菜单存在、启用、类型为页面且当前用户有权访问；
     * 2. 按所属公司、用户和菜单编码定位累计记录；
     * 3. 次数加一并刷新最近进入时间，返回最新统计。</p>
     */
    @Transactional
    public MenuUsageView recordMyMenuUsage(MenuUsageRequest request) {
        CurrentUser currentUser = SecurityUtils.currentUser();
        UserAccount user = userRepository.findById(currentUser.id())
                .orElseThrow(() -> new BusinessException(ResponseCode.NO_TOKEN_ERROR, "认证人员不存在"));
        SystemMenu menu = menuRepository.findByCode(request.menuCode())
                .orElseThrow(() -> new BusinessException(ResponseCode.REF_OBJ_NOT_EXISIT, "菜单不存在"));
        if (!menu.isEnabled() || menu.getType() != MenuType.PAGE || !authorizedMenuCodes(user).contains(menu.getCode())) {
            throw new BusinessException(ResponseCode.NO_AUTH, "当前用户无权访问该菜单");
        }
        String routePath = menu.getRoutePath() == null || menu.getRoutePath().isBlank() ? request.routePath() : menu.getRoutePath();
        if (routePath == null || routePath.isBlank()) {
            throw new BusinessException(ResponseCode.ILLEGAL_PARAM, "菜单路由不能为空");
        }
        UserMenuUsage usage = menuUsageRepository
                .findByOrganizationCodeAndUserIdAndMenuCode(CompanyScope.currentCompanyCode(), currentUser.id(), menu.getCode())
                .orElseGet(UserMenuUsage::new);
        OffsetDateTime now = OffsetDateTime.now();
        usage.setOrganizationCode(CompanyScope.currentCompanyCode());
        usage.setUserId(currentUser.id());
        usage.setUsername(currentUser.username());
        usage.setMenuCode(menu.getCode());
        usage.setMenuName(menu.getName());
        usage.setRoutePath(routePath);
        usage.setUseCount(usage.getUseCount() + 1);
        usage.setLastUsedAt(now);
        return toMenuUsageView(menuUsageRepository.save(usage));
    }

    /**
     * 读取当前登录人的常用功能菜单。
     */
    @Transactional(readOnly = true)
    public List<MenuUsageView> listMyMenuUsages(int limit) {
        CurrentUser currentUser = SecurityUtils.currentUser();
        UserAccount user = userRepository.findById(currentUser.id())
                .orElseThrow(() -> new BusinessException(ResponseCode.NO_TOKEN_ERROR, "认证人员不存在"));
        int pageSize = Math.max(1, Math.min(limit, 50));
        Set<String> menuCodes = authorizedMenuCodes(user);
        return menuUsageRepository
                .findByOrganizationCodeAndUserIdOrderByUseCountDescLastUsedAtDescIdAsc(
                        CompanyScope.currentCompanyCode(), currentUser.id(), PageRequest.of(0, pageSize))
                .stream()
                .filter(usage -> menuCodes.contains(usage.getMenuCode()))
                .map(this::toMenuUsageView)
                .toList();
    }

    /**
     * 当前登录人修改自己的个人资料。
     *
     * <p>实现步骤：
     * 1. 根据当前登录人 ID 查询人员；
     * 2. 默认管理员账号不允许自行修改个人资料，避免初始化账号被误改；
     * 3. 校验身份证号不被其他人员占用；
     * 4. 更新姓名、身份证、手机号和邮箱，头像统一通过上传接口维护；
     * 5. 记录关键操作日志。</p>
     */
    @Transactional
    public UserView updateProfile(Long currentUserId, ProfileUpdateRequest request) {
        UserAccount user = userRepository.findById(currentUserId)
                .orElseThrow(() -> new BusinessException(HttpStatus.NOT_FOUND, "人员不存在"));
        assertNotDefaultAccount(user);
        String identityNo = request.identityNo() == null || request.identityNo().isBlank()
                ? user.getIdentityNo()
                : request.identityNo().trim();
        userRepository.findByOrganizationCodeAndIdentityNo(user.getOrganizationCode(), identityNo)
                .filter(existing -> !existing.getId().equals(user.getId()))
                .ifPresent(existing -> {
                    throw new BusinessException(ResponseCode.OBJ_BEEN_USED, "身份证号已被当前所属公司的其他人员使用");
                });
        user.setRealName(request.realName());
        user.setIdentityNo(identityNo);
        user.setPhone(request.phone());
        user.setEmail(request.email());
        if (request.avatarUrl() != null) {
            user.setAvatarUrl(request.avatarUrl());
        }
        // 变量说明：view 保存当前步骤计算、查询或转换得到的中间结果。
        UserView view = toUserView(user);
        auditLogService.record("UPDATE_PROFILE", "userId=" + currentUserId, "SUCCESS",
                "个人中心修改了当前登录人的个人资料: " + displayUser(view) + "。");
        return view;
    }

    /**
     * 管理员或有授权人员修改指定人员密码。
     *
     * <p>实现步骤：读取目标人员，校验存在后直接重置密码哈希，并记录关键操作日志。</p>
     */
    @Transactional
    public void changeUserPassword(Long userId, PasswordChangeRequest request) {
        UserAccount user = userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException(HttpStatus.NOT_FOUND, "人员不存在"));
        assertUserCompanyManageable(user);
        user.setPasswordHash(passwordHashService.hash(request.newPassword()));
        auditLogService.record("CHANGE_USER_PASSWORD", "userId=" + userId, "SUCCESS",
                "人员管理修改了用户" + displayUser(user) + "的登录密码。");
    }

    /**
     * 当前登录人修改自己的密码。
     *
     * <p>实现步骤：
     * 1. 默认管理员账号不允许通过个人中心修改密码；
     * 2. 校验原密码；
     * 3. 保存新密码哈希；
     * 4. 记录关键操作日志。</p>
     */
    @Transactional
    public void changeMyPassword(Long currentUserId, PasswordChangeRequest request) {
        UserAccount user = userRepository.findById(currentUserId)
                .orElseThrow(() -> new BusinessException(HttpStatus.NOT_FOUND, "人员不存在"));
        assertNotDefaultAccount(user);
        if (!passwordHashService.matches(request.oldPassword(), user.getPasswordHash())) {
            throw new BusinessException(ResponseCode.PASSWORD_ERROR);
        }
        user.setPasswordHash(passwordHashService.hash(request.newPassword()));
        auditLogService.record("CHANGE_MY_PASSWORD", "userId=" + currentUserId, "SUCCESS",
                "个人中心修改了当前登录人的登录密码。");
    }

    /**
     * 更新人员头像图片 Base64。
     *
     * <p>实现步骤：
     * 1. 校验目标人员存在；
     * 2. 个人中心上传时保护默认管理员账号；
     * 3. 将 data:image/...;base64,... 写入 avatarBase64，并清理历史头像 URL；
     * 4. 记录关键操作日志，但不把完整图片 Base64 写入日志，避免日志膨胀。</p>
     */
    @Transactional
    public UserView updateAvatar(Long userId, String avatarBase64, boolean selfOperation) {
        UserAccount user = userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException(HttpStatus.NOT_FOUND, "人员不存在"));
        if (!selfOperation) {
            assertUserCompanyManageable(user);
        }
        if (selfOperation) {
            assertNotDefaultAccount(user);
        }
        user.setAvatarBase64(avatarBase64);
        user.setAvatarUrl(null);
        auditLogService.record(selfOperation ? "UPDATE_MY_AVATAR" : "UPDATE_USER_AVATAR",
                "userId=" + userId + ", avatarBase64Length=" + (avatarBase64 == null ? 0 : avatarBase64.length()),
                "SUCCESS",
                (selfOperation ? "个人中心上传了当前登录人的头像照片。" : "人员管理上传了用户" + displayUser(user) + "的头像照片。"));
        return toUserView(user);
    }

    /**
     * 个人资料变更后重新签发当前登录会话的 JWT。
     *
     * <p>实现步骤：
     * 1. 根据 sessionId 查询当前 ACTIVE 会话；
     * 2. 读取会话关联人员，使用数据库最新姓名、身份证、部门、手机号重建 CurrentUser；
     * 3. 重新签发 JWT，并把会话中的姓名、身份证和过期时间同步为最新值。</p>
     */
    @Transactional
    public JwtTokenService.TokenIssue refreshCurrentToken(String sessionId, String terminalType, String terminalIdentifier) {
        // 步骤1：会话不存在时说明当前 Cookie 已经失效，直接返回认证错误。
        UserLoginSession session = sessionRepository.findBySessionId(sessionId)
                .orElseThrow(() -> new BusinessException(ResponseCode.NO_TOKEN_ERROR, "登录会话不存在"));
        if (session.getStatus() != LoginSessionStatus.ACTIVE) {
            throw new BusinessException(ResponseCode.NO_TOKEN_ERROR, "登录会话已失效");
        }
        // 步骤2：使用人员表最新资料重建 JWT 用户上下文，避免个人资料修改后旧令牌立即失效。
        UserAccount user = userRepository.findById(session.getUserId())
                .orElseThrow(() -> new BusinessException(ResponseCode.NO_TOKEN_ERROR, "认证人员不存在"));
        String effectiveTerminalIdentifier = "APP".equals(terminalType)
                ? firstText(user.getPhone(), terminalIdentifier)
                : terminalIdentifier;
        JwtTokenService.TokenIssue issue = jwtTokenService.issue(
                toCurrentUser(user, null, terminalType, effectiveTerminalIdentifier, sessionId)
        );
        // 步骤3：登录会话保留最新人员姓名、身份证号和过期时间，保证下一次请求的会话一致性校验通过。
        session.setRealName(user.getRealName());
        session.setIdentityNo(user.getIdentityNo());
        session.setOrganizationCode(user.getOrganizationCode());
        session.setTerminalIdentifier(effectiveTerminalIdentifier);
        session.setExpiresAt(issue.expiresAt());
        return issue;
    }

    /**
     * 将新增或修改请求中的人员基础字段统一写入实体。
     *
     * <p>统一字段赋值可以保证新增和修改使用同一套字段口径，同时集中解析角色编码。</p>
     */
    private void applyUserFields(
            UserAccount user,
            String realName,
            String department,
            String organizationCode,
            String position,
            String identityNo,
            String phone,
            String email,
            String avatarUrl,
            Boolean enabled,
            Set<String> roleCodes
    ) {
        user.setRealName(realName);
        user.setDepartment(resolveBusinessDictionaryName("DEPARTMENT", department, "部门"));
        user.setOrganizationCode(resolveCompanyCode(organizationCode, "所属公司"));
        user.setPosition(resolveBusinessDictionaryName("POSITION", position, "岗位"));
        user.setIdentityNo(identityNo);
        user.setPhone(phone);
        user.setEmail(email);
        if (avatarUrl != null) {
            user.setAvatarUrl(avatarUrl);
        }
        user.setEnabled(enabled == null || enabled);
        user.setRoles(resolveRoles(roleCodes));
    }

    /**
     * 解析所属公司字典字段。
     *
     * <p>实现步骤：
     * 1. 空值回退预置公司，保证系统初始化和历史空值用户仍可归属默认账套；
     * 2. 非空值必须命中 ORGANIZATION 根字典下的启用节点；
     * 3. 返回字典编码存入人员表，保证公司改名后账套隔离仍然稳定。</p>
     */
    private String resolveCompanyCode(String value, String label) {
        String normalizedValue = firstText(value, CompanyScope.DEFAULT_COMPANY_CODE);
        BasicDictionary dictionary = resolveBusinessDictionary("ORGANIZATION", normalizedValue, label);
        return dictionary.getCode();
    }

    /**
     * 解析登录所属公司，并明确区分“未选择”和“已停用”两类错误。
     *
     * <p>实现步骤：
     * 1. 空值回退默认公司，兼容历史登录页和初始化管理员；
     * 2. 按公司编码或名称定位 ORGANIZATION 根字典下的公司节点；
     * 3. 公司自身或任一上级停用时拒绝登录，并提示所属公司已停用；
     * 4. 返回公司字典实体，后续账号和身份证号都在该公司维度下匹配。</p>
     */
    private BasicDictionary resolveLoginCompany(String value) {
        String normalizedValue = firstText(value, CompanyScope.DEFAULT_COMPANY_CODE);
        BasicDictionary dictionary = dictionaryRepository.findByCode(normalizedValue)
                .or(() -> dictionaryRepository.findAllByOrderBySortOrderAscIdAsc().stream()
                        .filter(item -> Objects.equals(item.getName(), normalizedValue))
                        .filter(item -> isDictionaryUnderRoot(item, "ORGANIZATION"))
                        .findFirst())
                .orElseThrow(() -> new BusinessException(ResponseCode.ILLEGAL_PARAM, "所属公司必须从启用字典项中选择"));
        if (!isDictionaryUnderRoot(dictionary, "ORGANIZATION")) {
            throw new BusinessException(ResponseCode.ILLEGAL_PARAM, "所属公司必须从启用字典项中选择");
        }
        if (!isDictionaryVisibleForBusiness(dictionary)) {
            throw new BusinessException(ResponseCode.NO_AUTH, "所属公司【" + dictionary.getName() + "】已停用，请联系管理员！");
        }
        return dictionary;
    }

    /**
     * 解析人员档案中的普通业务字典字段。
     *
     * <p>实现步骤：
     * 1. 空值允许保存为空，方便暂未分配部门或岗位的人员；
     * 2. 非空值必须命中指定根字典下的启用节点；
     * 3. 返回字典名称存入人员表，保证列表和审计日志展示稳定的人类可读文本。</p>
     */
    private String resolveBusinessDictionaryName(String rootCode, String value, String label) {
        // 变量说明：normalizedValue 保存当前步骤计算、查询或转换得到的中间结果。
        String normalizedValue = firstText(value, null);
        if (normalizedValue == null) {
            return null;
        }
        return resolveBusinessDictionary(rootCode, normalizedValue, label).getName();
    }

    /**
     * 根据根字典和值解析启用字典节点。
     *
     * <p>实现步骤：先按 code 精确查找，再按 name 兼容旧页面提交名称；最后校验该字典处于指定根下且自身及上级均启用。</p>
     */
    private BasicDictionary resolveBusinessDictionary(String rootCode, String value, String label) {
        String normalizedValue = firstText(value, null);
        if (normalizedValue == null) {
            throw new BusinessException(ResponseCode.ILLEGAL_PARAM, label + "必须从启用字典项中选择");
        }
        BasicDictionary dictionary = dictionaryRepository.findByCode(normalizedValue)
                .or(() -> dictionaryRepository.findAllByOrderBySortOrderAscIdAsc().stream()
                        .filter(item -> Objects.equals(item.getName(), normalizedValue))
                        .filter(item -> isDictionaryUnderRoot(item, rootCode))
                        .findFirst())
                .orElseThrow(() -> new BusinessException(ResponseCode.ILLEGAL_PARAM, label + "必须从启用字典项中选择"));
        if (!isDictionaryVisibleForBusiness(dictionary) || !isDictionaryUnderRoot(dictionary, rootCode)) {
            throw new BusinessException(ResponseCode.ILLEGAL_PARAM, label + "必须从启用字典项中选择");
        }
        return dictionary;
    }

    /**
     * 判断字典是否位于指定根字典下。
     *
     * <p>实现步骤：从当前字典向上追溯父级；遇到目标根编码则返回 true；遇到环形数据时中断。</p>
     */
    private boolean isDictionaryUnderRoot(BasicDictionary dictionary, String rootCode) {
        // 变量说明：visited 保存当前步骤计算、查询或转换得到的中间结果。
        Set<Long> visited = new HashSet<>();
        // 变量说明：cursor 保存当前步骤计算、查询或转换得到的中间结果。
        BasicDictionary cursor = dictionary;
        while (cursor != null) {
            if (Objects.equals(cursor.getCode(), rootCode)) {
                return !Objects.equals(dictionary.getCode(), rootCode);
            }
            if (cursor.getId() != null && !visited.add(cursor.getId())) {
                return false;
            }
            cursor = cursor.getParent();
        }
        return false;
    }

    /**
     * 判断字典自身和所有上级是否启用。
     */
    private boolean isDictionaryVisibleForBusiness(BasicDictionary dictionary) {
        // 变量说明：visited 保存当前步骤计算、查询或转换得到的中间结果。
        Set<Long> visited = new HashSet<>();
        // 变量说明：cursor 保存当前步骤计算、查询或转换得到的中间结果。
        BasicDictionary cursor = dictionary;
        while (cursor != null) {
            if (!cursor.isEnabled()) {
                return false;
            }
            if (cursor.getId() != null && !visited.add(cursor.getId())) {
                return false;
            }
            cursor = cursor.getParent();
        }
        return true;
    }

    /**
     * 根据角色编码集合解析角色实体集合。
     *
     * <p>实现步骤：
     * 1. 空角色集合直接返回空集合；
     * 2. 逐个角色编码查询角色；
     * 3. 任一角色不存在则抛出业务异常，避免保存无效授权。</p>
     */
    private Set<Role> resolveRoles(Set<String> roleCodes) {
        if (roleCodes == null || roleCodes.isEmpty()) {
            return Set.of();
        }
        // 变量说明：roles 保存当前步骤计算、查询或转换得到的中间结果。
        Set<Role> roles = new HashSet<>();
        for (String roleCode : roleCodes) {
            Role role = roleRepository.findByCode(roleCode)
                    .orElseThrow(() -> new BusinessException("角色不存在: " + roleCode));
            roles.add(role);
        }
        return roles;
    }

    /**
     * 根据菜单编码集合解析菜单实体集合。
     */
    private Set<SystemMenu> resolveMenus(Set<String> menuCodes) {
        if (menuCodes == null || menuCodes.isEmpty()) {
            return Set.of();
        }
        // 变量说明：menus 保存当前步骤计算、查询或转换得到的中间结果。
        Set<SystemMenu> menus = new HashSet<>();
        for (String menuCode : menuCodes) {
            SystemMenu menu = menuRepository.findByCode(menuCode)
                    .orElseThrow(() -> new BusinessException("菜单不存在: " + menuCode));
            menus.add(menu);
        }
        return menus;
    }

    /**
     * 补齐菜单的所有上级资源。
     *
     * <p>实现目的：角色授权时选择按钮必须自动拥有对应页面和模块，选择页面必须自动拥有对应模块，保证前端导航和按钮显隐可以完整联动。</p>
     */
    private Set<SystemMenu> withAncestorMenus(Set<SystemMenu> menus) {
        // 变量说明：mergedMenus 保存当前步骤计算、查询或转换得到的中间结果。
        Set<SystemMenu> mergedMenus = new HashSet<>(menus);
        for (SystemMenu menu : menus) {
            // 变量说明：parent 保存当前步骤计算、查询或转换得到的中间结果。
            SystemMenu parent = menu.getParent();
            // 变量说明：visited 保存当前步骤计算、查询或转换得到的中间结果。
            Set<Long> visited = new HashSet<>();
            while (parent != null) {
                if (parent.getId() != null && !visited.add(parent.getId())) {
                    break;
                }
                mergedMenus.add(parent);
                parent = parent.getParent();
            }
        }
        return mergedMenus;
    }

    /**
     * 解析并校验菜单父级。
     *
     * <p>实现步骤：
     * 1. 模块菜单必须无父级；
     * 2. 页面菜单父级必须是模块；
     * 3. 按钮菜单父级必须是页面；
     * 4. 编辑时禁止把菜单父级设置为自己；
     * 5. 编辑时禁止把自己的下级菜单设置为父级，避免形成环形菜单树。</p>
     */
    private SystemMenu resolveMenuParent(Long currentMenuId, MenuType menuType, Long parentId) {
        if (menuType == MenuType.MODULE) {
            if (parentId != null) {
                throw new BusinessException(ResponseCode.ILLEGAL_PARAM, "模块菜单不能选择父级");
            }
            return null;
        }
        if (parentId == null) {
            throw new BusinessException(ResponseCode.ILLEGAL_PARAM, menuType == MenuType.PAGE ? "页面菜单必须选择模块父级" : "按钮菜单必须选择页面父级");
        }
        if (Objects.equals(currentMenuId, parentId)) {
            throw new BusinessException(ResponseCode.ILLEGAL_PARAM, "菜单父级不能选择自己");
        }
        SystemMenu parent = menuRepository.findById(parentId)
                .orElseThrow(() -> new BusinessException(HttpStatus.NOT_FOUND, "父级菜单不存在"));
        if (menuType == MenuType.PAGE && parent.getType() != MenuType.MODULE) {
            throw new BusinessException(ResponseCode.ILLEGAL_PARAM, "页面菜单只能挂在模块下");
        }
        if (menuType == MenuType.BUTTON && parent.getType() != MenuType.PAGE) {
            throw new BusinessException(ResponseCode.ILLEGAL_PARAM, "按钮菜单只能挂在页面下");
        }
        if (isMenuParentDescendant(parent, currentMenuId)) {
            throw new BusinessException(ResponseCode.ILLEGAL_PARAM, "菜单父级不能选择自身下级");
        }
        return parent;
    }

    /**
     * 判断候选父级是否已经位于当前菜单的下级链路中。
     *
     * <p>实现步骤：从候选父级自身开始向上追溯；如果追溯到当前菜单，说明选择该父级会造成环形嵌套。</p>
     */
    private boolean isMenuParentDescendant(SystemMenu candidateParent, Long currentMenuId) {
        if (currentMenuId == null || candidateParent == null) {
            return false;
        }
        // 变量说明：visited 保存当前步骤计算、查询或转换得到的中间结果。
        Set<Long> visited = new HashSet<>();
        // 变量说明：cursor 保存当前步骤计算、查询或转换得到的中间结果。
        SystemMenu cursor = candidateParent;
        while (cursor != null) {
            if (Objects.equals(cursor.getId(), currentMenuId)) {
                return true;
            }
            if (cursor.getId() != null && !visited.add(cursor.getId())) {
                return false;
            }
            cursor = cursor.getParent();
        }
        return false;
    }

    /**
     * 从菜单集合中提取绑定的后端权限码。
     */
    private Set<PermissionCode> resolvePermissions(Set<SystemMenu> menus) {
        // 变量说明：permissions 保存当前步骤计算、查询或转换得到的中间结果。
        Set<PermissionCode> permissions = new HashSet<>();
        for (SystemMenu menu : menus) {
            if (menu.getPermissionCode() != null) {
                permissions.add(menu.getPermissionCode());
            }
        }
        return permissions;
    }

    /**
     * 默认管理员账号保护。
     */
    private void assertNotDefaultAccount(UserAccount user) {
        if (isDefaultAccount(user)) {
            throw new BusinessException(ResponseCode.MODIFY_FORBIDDEN, "默认账号不允许在个人中心修改资料、密码或头像");
        }
    }

    /**
     * 判断是否为系统初始化的默认管理员账号。
     *
     * <p>默认账号使用初始化身份证号判断，避免管理员账号名被调整后默认账号保护失效。</p>
     */
    private boolean isDefaultAccount(UserAccount user) {
        return Objects.equals(defaultAdminIdentityNo, user.getIdentityNo());
    }

    /**
     * 判断目标人员是否允许由当前登录人维护。
     *
     * <p>实现步骤：
     * 1. 默认 admin 允许跨所属公司维护人员；
     * 2. 普通人员只能维护当前登录公司下的人员；
     * 3. 发现跨公司操作时返回无权限，避免通过人员管理接口修改其他账套人员。</p>
     */
    private void assertUserCompanyManageable(UserAccount user) {
        if (CompanyScope.isSuperAdmin()) {
            return;
        }
        CompanyScope.requireCurrentCompany(user.getOrganizationCode(), "人员");
    }

    /**
     * 解析人员维护时可编辑的所属公司。
     *
     * <p>实现步骤：
     * 1. admin 使用请求中选择的所属公司，并校验必须来自所属公司字典；
     * 2. 非 admin 忽略前端传入值，强制使用当前登录人的所属公司；
     * 3. 返回稳定字典编码，供账号、身份证唯一性和人员表保存使用。</p>
     */
    private String resolveEditableCompanyCode(String requestedOrganizationCode, String label) {
        if (CompanyScope.isSuperAdmin()) {
            return resolveCompanyCode(requestedOrganizationCode, label);
        }
        return resolveCompanyCode(CompanyScope.currentCompanyCode(), label);
    }

    /**
     * 将人员实体转换为当前登录人上下文。
     *
     * <p>该重载用于不关心终端和会话信息的场景。</p>
     */
    public CurrentUser toCurrentUser(UserAccount user, java.time.OffsetDateTime expiresAt) {
        return toCurrentUser(user, expiresAt, null, null, null);
    }

    /**
     * 将人员实体转换为当前登录人上下文。
     *
     * <p>实现步骤：
     * 1. 汇总人员所有角色中的权限码；
     * 2. 复制 JWT 校验需要的人员基础字段；
     * 3. 写入终端类型、终端标识、会话 ID 和过期时间。</p>
     */
    public CurrentUser toCurrentUser(UserAccount user, java.time.OffsetDateTime expiresAt, String terminalType, String terminalIdentifier, String sessionId) {
        // 步骤1：角色可能有多个，权限码取并集后写入当前用户上下文。
        Set<PermissionCode> permissions = new HashSet<>();
        for (Role role : user.getRoles()) {
            permissions.addAll(role.getPermissions());
        }
        // 步骤2-3：这些字段会进入 JWT，后续每次请求都会和数据库人员信息比对。
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
     * 将人员实体转换为接口视图，隐藏 passwordHash 等敏感字段。
     */
    public UserView toUserView(UserAccount user) {
        return new UserView(
                user.getId(),
                user.getUsername(),
                user.getRealName(),
                user.getDepartment(),
                user.getOrganizationCode(),
                user.getPosition(),
                user.getIdentityNo(),
                user.getPhone(),
                user.getEmail(),
                avatarSource(user),
                isDefaultAccount(user),
                user.isEnabled(),
                user.getRoles().stream().map(this::toRoleView).toList()
        );
    }

    /**
     * 解析前端展示头像来源。
     *
     * <p>实现步骤：优先使用用户表 avatar_base64 字段中的 data URL；若历史数据只有 avatar_url，则继续返回历史 URL。</p>
     */
    private String avatarSource(UserAccount user) {
        return firstText(user.getAvatarBase64(), user.getAvatarUrl());
    }

    /**
     * 组装人员在操作日志中的展示名称。
     *
     * <p>实现步骤：优先展示真实姓名；真实姓名为空时回退账号；括号中补充账号，方便日志用户定位具体人员。</p>
     */
    private String displayUser(UserAccount user) {
        // 变量说明：name 保存当前步骤计算、查询或转换得到的中间结果。
        String name = firstText(user.getRealName(), user.getUsername());
        // 变量说明：username 保存当前步骤计算、查询或转换得到的中间结果。
        String username = firstText(user.getUsername(), "");
        return username == null || username.isBlank() || username.equals(name) ? name : name + "(" + username + ")";
    }

    /**
     * 组装人员视图在操作日志中的展示名称。
     */
    private String displayUser(UserView user) {
        // 变量说明：name 保存当前步骤计算、查询或转换得到的中间结果。
        String name = firstText(user.realName(), user.username());
        // 变量说明：username 保存当前步骤计算、查询或转换得到的中间结果。
        String username = firstText(user.username(), "");
        return username == null || username.isBlank() || username.equals(name) ? name : name + "(" + username + ")";
    }

    /**
     * 根据登录输入和所属公司解析人员。
     *
     * <p>实现步骤：
     * 1. 先按公司加身份证号查询人员，因为身份证号是人员业务主键；
     * 2. 查不到时按公司加登录账号查询；
     * 3. 两种方式均查不到时按密码错误处理，避免暴露账号是否存在。</p>
     */
    private UserAccount resolveLoginUser(String organizationCode, String loginName) {
        // 变量说明：normalizedLoginName 保存当前步骤计算、查询或转换得到的中间结果。
        String normalizedLoginName = loginName == null ? "" : loginName.trim();
        return userRepository.findByOrganizationCodeAndIdentityNo(organizationCode, normalizedLoginName)
                .or(() -> userRepository.findByOrganizationCodeAndUsername(organizationCode, normalizedLoginName))
                .orElseThrow(() -> new BadCredentialsException("Bad credentials"));
    }

    /**
     * 规范化必填字符串。
     *
     * <p>实现步骤：先去除首尾空格，再判断是否为空；为空时返回非法参数异常，避免账号或身份证保存出不可见空格。</p>
     */
    private String normalizeRequired(String value, String message) {
        // 变量说明：normalized 保存当前步骤计算、查询或转换得到的中间结果。
        String normalized = value == null ? "" : value.trim();
        if (normalized.isEmpty()) {
            throw new BusinessException(ResponseCode.ILLEGAL_PARAM, message);
        }
        return normalized;
    }

    /**
     * 清理批量操作 ID。
     */
    private List<Long> normalizeBatchIds(List<Long> ids) {
        if (ids == null) {
            throw new BusinessException(ResponseCode.ILLEGAL_PARAM, "请选择需要删除的数据");
        }
        List<Long> normalizedIds = ids.stream()
                .filter(Objects::nonNull)
                .distinct()
                .toList();
        if (normalizedIds.isEmpty()) {
            throw new BusinessException(ResponseCode.ILLEGAL_PARAM, "请选择需要删除的数据");
        }
        return normalizedIds;
    }

    /**
     * 执行 authorizedMenuCodes 方法。
     * 
     * <p>实现步骤：
     * 1. 接收并校验调用方传入的数据；
     * 2. 按当前方法职责执行业务查询、转换或持久化处理；
     * 3. 返回处理结果或更新对象状态。</p>
     */
    private Set<String> authorizedMenuCodes(UserAccount user) {
        // 变量说明：menuCodes 保存当前步骤计算、查询或转换得到的中间结果。
        Set<String> menuCodes = new HashSet<>();
        for (Role role : user.getRoles()) {
            for (SystemMenu menu : role.getMenus()) {
                addEnabledMenuWithAncestors(menu, menuCodes);
            }
        }
        return menuCodes;
    }

    /**
     * 收集启用菜单及其启用父级菜单。
     *
     * <p>实现目的：菜单层级调整后，历史角色可能只保存了页面或按钮授权；返回给前端前补齐父级模块，保证左侧导航仍能按菜单树正常展示。</p>
     */
    private void addEnabledMenuWithAncestors(SystemMenu menu, Set<String> menuCodes) {
        if (menu == null || !menu.isEnabled()) {
            return;
        }
        menuCodes.add(menu.getCode());
        addEnabledMenuWithAncestors(menu.getParent(), menuCodes);
    }

    /**
     * 执行 toRoleView 方法。
     * 
     * <p>实现步骤：
     * 1. 接收并校验调用方传入的数据；
     * 2. 按当前方法职责执行业务查询、转换或持久化处理；
     * 3. 返回处理结果或更新对象状态。</p>
     */
    private RoleView toRoleView(Role role) {
        Set<String> menuCodes = role.getMenus().stream()
                .map(SystemMenu::getCode)
                .collect(java.util.stream.Collectors.toSet());
        return new RoleView(role.getId(), role.getCode(), role.getName(), role.getDescription(), role.getPermissions(), menuCodes);
    }

    /**
     * 执行 toMenuView 方法。
     * 
     * <p>实现步骤：
     * 1. 接收并校验调用方传入的数据；
     * 2. 按当前方法职责执行业务查询、转换或持久化处理；
     * 3. 返回处理结果或更新对象状态。</p>
     */
    private MenuView toMenuView(SystemMenu menu) {
        return new MenuView(
                menu.getId(),
                menu.getCode(),
                menu.getName(),
                menu.getType().name(),
                menu.getParent() == null ? null : menu.getParent().getId(),
                menu.getRoutePath(),
                menu.getSortOrder(),
                menu.isEnabled(),
                menu.getPermissionCode()
        );
    }

    /**
     * 转换用户常用菜单统计视图。
     */
    private MenuUsageView toMenuUsageView(UserMenuUsage usage) {
        return new MenuUsageView(
                usage.getMenuCode(),
                usage.getMenuName(),
                usage.getRoutePath(),
                usage.getUseCount(),
                usage.getLastUsedAt() == null ? null : usage.getLastUsedAt().toString()
        );
    }

    /**
     * 解析终端唯一标识。
     *
     * <p>实现步骤：
     * 1. PC 端只信任服务端获取的客户端 IP；
     * 2. APP 端优先使用请求携带的手机号，缺省时使用人员档案手机号；
     * 3. 解析不到终端标识时返回非法参数异常。</p>
     */
    private String resolveTerminalIdentifier(String requestedIdentifier, TerminalType terminalType, String clientIp, UserAccount user) {
        String terminalIdentifier = terminalType == TerminalType.PC
                ? firstText(clientIp, null)
                : firstText(requestedIdentifier, user.getPhone());
        if (terminalIdentifier == null) {
            throw new BusinessException(ResponseCode.ILLEGAL_PARAM, terminalType == TerminalType.PC
                    ? "PC端终端标识不能为空，需提供客户端IP"
                    : "APP端终端标识不能为空，需提供手机号");
        }
        return terminalIdentifier;
    }

    /**
     * 登录失败审计使用的终端标识解析。
     *
     * <p>实现步骤：尽量按正式登录规则提取终端标识；提取不到时返回空值，不因为日志字段缺失改变认证失败的主流程结果。</p>
     */
    private String resolveTerminalIdentifierForLog(String requestedIdentifier, TerminalType terminalType, String clientIp, UserAccount user) {
        return terminalType == TerminalType.PC
                ? firstText(clientIp, null)
                : firstText(requestedIdentifier, user == null ? null : user.getPhone());
    }

    /**
     * 清理已过期的 ACTIVE 登录会话。
     *
     * <p>实现步骤：
     * 1. 找出过期时间小于等于当前时间的会话；
     * 2. 将它们置为 EXPIRED 并记录退出时间；
     * 3. 返回仍然 ACTIVE 的会话供重复登录判断。</p>
     */
    private List<UserLoginSession> clearExpiredSessions(List<UserLoginSession> sessions) {
        // 变量说明：now 保存当前步骤计算、查询或转换得到的中间结果。
        OffsetDateTime now = OffsetDateTime.now();
        sessions.stream()
                .filter(session -> session.getExpiresAt() != null && !session.getExpiresAt().isAfter(now))
                .forEach(session -> {
                    session.setStatus(LoginSessionStatus.EXPIRED);
                    session.setLogoutTime(now);
                });
        return sessions.stream()
                .filter(session -> session.getStatus() == LoginSessionStatus.ACTIVE)
                .toList();
    }

    /**
     * 将旧登录会话标记为强制下线。
     *
     * <p>旧 JWT 不会立即从浏览器删除，但后续请求会根据会话状态返回 FORCE_LOGOUT。</p>
     */
    private void forceLogout(UserLoginSession session) {
        session.setStatus(LoginSessionStatus.FORCE_LOGOUT);
        session.setLogoutTime(OffsetDateTime.now());
    }

    /**
     * 从两个候选字符串中取第一个非空文本，并去除首尾空格。
     */
    private String firstText(String first, String second) {
        if (first != null && !first.isBlank()) {
            return first.trim();
        }
        if (second != null && !second.isBlank()) {
            return second.trim();
        }
        return null;
    }

    /**
     * 记录登录数据库审计日志。
     *
     * <p>实现步骤：构造脱敏登录参数，委托审计服务显式写入登录人、终端、结果和影响；审计服务内部会吞掉日志异常，避免影响登录主流程。</p>
     */
    private void recordLoginAttempt(
            UserAccount user,
            LoginRequest request,
            TerminalType terminalType,
            String terminalIdentifier,
            String action,
            String result,
            String responseValue,
            String impact
    ) {
        String parameters = "loginName=" + request.username()
                + ", organizationCode=" + request.organizationCode()
                + ", terminalType=" + terminalType
                + ", terminalIdentifier=" + terminalIdentifier
                + ", force=" + Boolean.TRUE.equals(request.force())
                + ", passwordProvided=" + (request.password() != null && !request.password().isBlank());
        auditLogService.recordLogin(
                user,
                request.username(),
                terminalType == null ? null : terminalType.name(),
                terminalIdentifier,
                action,
                parameters,
                result,
                responseValue,
                impact
        );
    }

    /**
     * 登录服务内部返回对象。
     *
     * <p>Controller 根据 repeated 判断是否返回重复登录提醒，根据 token 判断是否写 Cookie。</p>
     */
    public record LoginIssue(LoginResponse response, String token, int cookieMaxAgeSeconds) {

        /**
         * 执行 repeated 方法。
         * 
         * <p>实现步骤：
         * 1. 接收并校验调用方传入的数据；
         * 2. 按当前方法职责执行业务查询、转换或持久化处理；
         * 3. 返回处理结果或更新对象状态。</p>
         */
        public boolean repeated() {
            return Boolean.TRUE.equals(response.repeated());
        }

        /**
         * 执行 hasToken 方法。
         * 
         * <p>实现步骤：
         * 1. 接收并校验调用方传入的数据；
         * 2. 按当前方法职责执行业务查询、转换或持久化处理；
         * 3. 返回处理结果或更新对象状态。</p>
         */
        public boolean hasToken() {
            return Objects.nonNull(token) && !token.isBlank();
        }
    }

    /**
     * 执行 safeUserRequest 方法。
     * 
     * <p>实现步骤：
     * 1. 接收并校验调用方传入的数据；
     * 2. 按当前方法职责执行业务查询、转换或持久化处理；
     * 3. 返回处理结果或更新对象状态。</p>
     */
    private String safeUserRequest(UserCreateRequest request) {
        return "username=" + request.username()
                + ", realName=" + request.realName()
                + ", department=" + request.department()
                + ", organizationCode=" + request.organizationCode()
                + ", position=" + request.position()
                + ", identityNo=" + request.identityNo()
                + ", phone=" + request.phone()
                + ", email=" + request.email()
                + ", avatarUrlProvided=" + (request.avatarUrl() != null && !request.avatarUrl().isBlank())
                + ", enabled=" + request.enabled()
                + ", roleCodes=" + request.roleCodes();
    }

    /**
     * 执行 safeUserRequest 方法。
     * 
     * <p>实现步骤：
     * 1. 接收并校验调用方传入的数据；
     * 2. 按当前方法职责执行业务查询、转换或持久化处理；
     * 3. 返回处理结果或更新对象状态。</p>
     */
    private String safeUserRequest(UserUpdateRequest request) {
        return "realName=" + request.realName()
                + ", department=" + request.department()
                + ", organizationCode=" + request.organizationCode()
                + ", position=" + request.position()
                + ", identityNo=" + request.identityNo()
                + ", phone=" + request.phone()
                + ", email=" + request.email()
                + ", avatarUrlProvided=" + (request.avatarUrl() != null && !request.avatarUrl().isBlank())
                + ", enabled=" + request.enabled()
                + ", roleCodes=" + request.roleCodes()
                + ", passwordChanged=" + (request.password() != null && !request.password().isBlank());
    }
}
