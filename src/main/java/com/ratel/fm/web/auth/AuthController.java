package com.ratel.fm.web.auth;

import com.ratel.fm.common.ApiResponse;
import com.ratel.fm.common.ResponseCode;
import com.ratel.fm.common.web.ClientIpUtils;
import com.ratel.fm.domain.auth.UserAccount;
import com.ratel.fm.domain.auth.MenuType;
import com.ratel.fm.domain.auth.PermissionCode;
import com.ratel.fm.repository.auth.UserAccountRepository;
import com.ratel.fm.security.CurrentUser;
import com.ratel.fm.security.JwtCookieAuthenticationFilter;
import com.ratel.fm.security.JwtTokenService;
import com.ratel.fm.security.SecurityUtils;
import com.ratel.fm.service.auth.AuthService;
import com.ratel.fm.service.auth.AuthService.LoginIssue;
import com.ratel.fm.service.basic.BasicDictionaryService;
import com.ratel.fm.web.dto.auth.AuthDtos.LoginRequest;
import com.ratel.fm.web.dto.auth.AuthDtos.LoginResponse;
import com.ratel.fm.web.dto.auth.AuthDtos.MenuCodeView;
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
import com.ratel.fm.web.dto.basic.BasicDictionaryDtos.BasicDictionaryView;
import com.ratel.fm.web.dto.common.BatchIdsRequest;
import com.github.xiaoymin.knife4j.annotations.ApiOperationSupport;
import com.github.xiaoymin.knife4j.annotations.ApiSupport;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.io.IOException;
import java.util.Base64;
import java.util.List;
import java.util.Locale;
import java.util.Map;

@Tag(name = "人员与授权")
@ApiSupport(order = 10, author = "ratel / WenZhang / 18782945613")
@RestController
@RequestMapping("/api")
/**
 * AuthController 类。
 * 
 * <p>用于承载 AuthController 相关的业务数据、接口契约或处理逻辑，便于维护人员快速定位模块职责。</p>
 */
public class AuthController {

    /**
     * 字段 authService：保存 authService 对应的业务数据、运行配置或依赖对象，供本类逻辑读取和维护。
     */
    private final AuthService authService;
    /**
     * 字段 jwtCookieAuthenticationFilter：保存 jwtCookieAuthenticationFilter 对应的业务数据、运行配置或依赖对象，供本类逻辑读取和维护。
     */
    private final JwtCookieAuthenticationFilter jwtCookieAuthenticationFilter;
    /**
     * 字段 jwtTokenService：保存 jwtTokenService 对应的业务数据、运行配置或依赖对象，供本类逻辑读取和维护。
     */
    private final JwtTokenService jwtTokenService;
    /**
     * 字段 userAccountRepository：保存 userAccountRepository 对应的业务数据、运行配置或依赖对象，供本类逻辑读取和维护。
     */
    private final UserAccountRepository userAccountRepository;
    /** 基础字典服务，用于登录前读取启用所属公司账套。 */
    private final BasicDictionaryService dictionaryService;

    /**
     * 构造 AuthController 实例。
     * 
     * <p>实现步骤：
     * 1. 接收调用方传入的依赖对象或初始化参数；
     * 2. 保存到成员字段，保证后续业务方法可以复用；
     * 3. 完成实例初始化。</p>
     */
    public AuthController(
            AuthService authService,
            JwtCookieAuthenticationFilter jwtCookieAuthenticationFilter,
            JwtTokenService jwtTokenService,
            UserAccountRepository userAccountRepository,
            BasicDictionaryService dictionaryService
    ) {
        this.authService = authService;
        this.jwtCookieAuthenticationFilter = jwtCookieAuthenticationFilter;
        this.jwtTokenService = jwtTokenService;
        this.userAccountRepository = userAccountRepository;
        this.dictionaryService = dictionaryService;
    }

    @ApiOperationSupport(order = 10, author = "ratel / WenZhang / 18782945613")
    @Operation(summary = "人员登录", description = "作者：ratel；开发人员：WenZhang；联系方式：18782945613。登录成功后写入 HttpOnly JWT Cookie，支持同一身份证同终端类型唯一登录。")
    @PostMapping("/auth/login")
    /**
     * 执行 login 方法。
     * 
     * <p>实现步骤：
     * 1. 接收并校验调用方传入的数据；
     * 2. 按当前方法职责执行业务查询、转换或持久化处理；
     * 3. 返回处理结果或更新对象状态。</p>
     */
    public ApiResponse<LoginResponse> login(
            @Valid @RequestBody LoginRequest request,
            HttpServletRequest servletRequest,
            HttpServletResponse response
    ) {
        // 变量说明：login 保存当前步骤计算、查询或转换得到的中间结果。
        LoginIssue login = authService.login(request, clientIp(servletRequest));
        if (login.repeated()) {
            return ApiResponse.warn(ResponseCode.REPEAT_ERROR, login.response().conflictMessage(), login.response());
        }
        if (login.hasToken()) {
            jwtCookieAuthenticationFilter.writeCookie(response, login.token(), login.cookieMaxAgeSeconds());
        }
        return ApiResponse.ok(login.response());
    }

    @ApiOperationSupport(order = 12, author = "ratel / WenZhang / 18782945613")
    @Operation(summary = "查询登录所属公司", description = "作者：ratel；开发人员：WenZhang；联系方式：18782945613。登录页面读取启用所属公司账套，登录时账号和身份证按所属公司维度校验。")
    @GetMapping("/auth/companies")
    /**
     * 查询登录可选所属公司。
     *
     * <p>实现步骤：
     * 1. 读取 ORGANIZATION 根字典下启用的公司节点；
     * 2. 保留树形结构供登录页展示；
     * 3. 不返回其他业务字典，避免未登录状态暴露基础资料。</p>
     */
    public ApiResponse<List<BasicDictionaryView>> companies() {
        return ApiResponse.ok(dictionaryService.listEnabledTree("ORGANIZATION"));
    }

    @ApiOperationSupport(order = 20, author = "ratel / WenZhang / 18782945613")
    @Operation(summary = "当前登录人", description = "作者：ratel；开发人员：WenZhang；联系方式：18782945613。")
    @GetMapping("/auth/me")
    /**
     * 执行 me 方法。
     * 
     * <p>实现步骤：
     * 1. 接收并校验调用方传入的数据；
     * 2. 按当前方法职责执行业务查询、转换或持久化处理；
     * 3. 返回处理结果或更新对象状态。</p>
     */
    public ApiResponse<UserView> me() {
        // 变量说明：userId 保存当前步骤计算、查询或转换得到的中间结果。
        Long userId = SecurityUtils.currentUser().id();
        UserAccount account = userAccountRepository.findById(userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "登录已失效"));
        return ApiResponse.ok(authService.toUserView(account));
    }

    @ApiOperationSupport(order = 22, author = "ratel / WenZhang / 18782945613")
    @Operation(summary = "当前登录人授权菜单编码", description = "作者：ratel；开发人员：WenZhang；联系方式：18782945613。登录成功和页面刷新后由前端调用，用于匹配渲染模块、页面和按钮。")
    @GetMapping("/auth/menu-codes")
    /**
     * 执行 myMenuCodes 方法。
     * 
     * <p>实现步骤：
     * 1. 接收并校验调用方传入的数据；
     * 2. 按当前方法职责执行业务查询、转换或持久化处理；
     * 3. 返回处理结果或更新对象状态。</p>
     */
    public ApiResponse<MenuCodeView> myMenuCodes() {
        return ApiResponse.ok(new MenuCodeView(authService.listAuthorizedMenuCodes(SecurityUtils.currentUser().id())));
    }

    @ApiOperationSupport(order = 23, author = "ratel / WenZhang / 18782945613")
    @Operation(summary = "当前登录人授权菜单资源", description = "作者：ratel；开发人员：WenZhang；联系方式：18782945613。返回当前登录人有权访问的启用菜单资源，前端按菜单管理中的层级渲染左侧导航。")
    @GetMapping("/auth/menus")
    /**
     * 执行 myMenus 方法。
     * 
     * <p>实现步骤：
     * 1. 接收并校验调用方传入的数据；
     * 2. 按当前方法职责执行业务查询、转换或持久化处理；
     * 3. 返回处理结果或更新对象状态。</p>
     */
    public ApiResponse<List<MenuView>> myMenus() {
        return ApiResponse.ok(authService.listAuthorizedMenus(SecurityUtils.currentUser().id()));
    }

    @ApiOperationSupport(order = 24, author = "ratel / WenZhang / 18782945613")
    @Operation(summary = "当前登录人常用菜单", description = "作者：ratel；开发人员：WenZhang；联系方式：18782945613。按进入次数返回当前登录人常用功能。")
    @GetMapping("/auth/menu-usages")
    public ApiResponse<List<MenuUsageView>> myMenuUsages(@RequestParam(defaultValue = "10") int limit) {
        return ApiResponse.ok(authService.listMyMenuUsages(limit));
    }

    @ApiOperationSupport(order = 25, author = "ratel / WenZhang / 18782945613")
    @Operation(summary = "记录当前登录人进入菜单", description = "作者：ratel；开发人员：WenZhang；联系方式：18782945613。用户点击进入功能菜单时调用，累计常用功能次数。")
    @PostMapping("/auth/menu-usages")
    public ApiResponse<MenuUsageView> recordMyMenuUsage(@Valid @RequestBody MenuUsageRequest request) {
        return ApiResponse.ok(authService.recordMyMenuUsage(request));
    }

    @ApiOperationSupport(order = 26, author = "ratel / WenZhang / 18782945613")
    @Operation(summary = "修改个人资料", description = "作者：ratel；开发人员：WenZhang；联系方式：18782945613。默认账号不允许自行修改。")
    @PutMapping("/auth/profile")
    /**
     * 执行 updateProfile 方法。
     * 
     * <p>实现步骤：
     * 1. 接收并校验调用方传入的数据；
     * 2. 按当前方法职责执行业务查询、转换或持久化处理；
     * 3. 返回处理结果或更新对象状态。</p>
     */
    public ApiResponse<UserView> updateProfile(@Valid @RequestBody ProfileUpdateRequest request, HttpServletResponse response) {
        // 变量说明：currentUser 保存当前步骤计算、查询或转换得到的中间结果。
        CurrentUser currentUser = SecurityUtils.currentUser();
        // 变量说明：userView 保存当前步骤计算、查询或转换得到的中间结果。
        UserView userView = authService.updateProfile(currentUser.id(), request);
        JwtTokenService.TokenIssue issue = authService.refreshCurrentToken(
                currentUser.sessionId(),
                currentUser.terminalType(),
                currentUser.terminalIdentifier()
        );
        jwtCookieAuthenticationFilter.writeCookie(response, issue.token(), jwtTokenService.cookieMaxAgeSeconds());
        response.setHeader("X-Token-Refreshed", "true");
        response.setHeader("X-Token-Expires-At", issue.expiresAt().toString());
        return ApiResponse.ok("个人资料已更新", userView);
    }

    @ApiOperationSupport(order = 27, author = "ratel / WenZhang / 18782945613")
    @Operation(summary = "修改个人密码", description = "作者：ratel；开发人员：WenZhang；联系方式：18782945613。默认账号不允许自行修改。")
    @PutMapping("/auth/password")
    /**
     * 执行 changeMyPassword 方法。
     * 
     * <p>实现步骤：
     * 1. 接收并校验调用方传入的数据；
     * 2. 按当前方法职责执行业务查询、转换或持久化处理；
     * 3. 返回处理结果或更新对象状态。</p>
     */
    public ApiResponse<Void> changeMyPassword(@Valid @RequestBody PasswordChangeRequest request) {
        authService.changeMyPassword(SecurityUtils.currentUser().id(), request);
        return ApiResponse.ok("密码已修改", null);
    }

    @ApiOperationSupport(order = 28, author = "ratel / WenZhang / 18782945613")
    @Operation(summary = "上传个人头像", description = "作者：ratel；开发人员：WenZhang；联系方式：18782945613。默认账号不允许自行上传；头像以 Base64 存入用户表，只允许 jpg、jpeg、png、webp 图片。")
    @PostMapping("/auth/avatar")
    /**
     * 执行 uploadMyAvatar 方法。
     * 
     * <p>实现步骤：
     * 1. 接收并校验调用方传入的数据；
     * 2. 按当前方法职责执行业务查询、转换或持久化处理；
     * 3. 返回处理结果或更新对象状态。</p>
     */
    public ApiResponse<UserView> uploadMyAvatar(@RequestParam("file") MultipartFile file) throws IOException {
        // 变量说明：avatarBase64 保存当前步骤计算、查询或转换得到的中间结果。
        String avatarBase64 = readAvatarBase64(file);
        return ApiResponse.ok("头像已上传", authService.updateAvatar(SecurityUtils.currentUser().id(), avatarBase64, true));
    }

    @ApiOperationSupport(order = 30, author = "ratel / WenZhang / 18782945613")
    @Operation(summary = "退出登录", description = "作者：ratel；开发人员：WenZhang；联系方式：18782945613。清除 JWT Cookie。")
    @PostMapping("/auth/logout")
    /**
     * 执行 logout 方法。
     * 
     * <p>实现步骤：
     * 1. 接收并校验调用方传入的数据；
     * 2. 按当前方法职责执行业务查询、转换或持久化处理；
     * 3. 返回处理结果或更新对象状态。</p>
     */
    public ApiResponse<Void> logout(HttpServletResponse response) {
        authService.logout(SecurityUtils.currentUser().sessionId());
        jwtCookieAuthenticationFilter.writeCookie(response, "", 0);
        return ApiResponse.ok("已退出登录", null);
    }

    @ApiOperationSupport(order = 40, author = "ratel / WenZhang / 18782945613")
    @Operation(summary = "查询人员列表", description = "作者：ratel；开发人员：WenZhang；联系方式：18782945613。")
    @PreAuthorize("hasAuthority('SYSTEM_USER_MANAGE')")
    @GetMapping("/users")
    /**
     * 执行 listUsers 方法。
     * 
     * <p>实现步骤：
     * 1. 接收并校验调用方传入的数据；
     * 2. 按当前方法职责执行业务查询、转换或持久化处理；
     * 3. 返回处理结果或更新对象状态。</p>
     */
    public ApiResponse<List<UserView>> listUsers(
            @RequestParam(required = false) String username,
            @RequestParam(required = false) String realName,
            @RequestParam(required = false) String identityNo,
            @RequestParam(required = false) String phone,
            @RequestParam(required = false) String email,
            @RequestParam(required = false) String department,
            @RequestParam(required = false) String organizationCode,
            @RequestParam(required = false) String position,
            @RequestParam(required = false) Boolean enabled
    ) {
        return ApiResponse.ok(authService.listUsers(username, realName, identityNo, phone, email,
                department, organizationCode, position, enabled));
    }

    @ApiOperationSupport(order = 50, author = "ratel / WenZhang / 18782945613")
    @Operation(summary = "新增人员", description = "作者：ratel；开发人员：WenZhang；联系方式：18782945613。身份证号必填且全系统唯一。")
    @PreAuthorize("hasAuthority('SYSTEM_USER_MANAGE')")
    @PostMapping("/users")
    /**
     * 执行 createUser 方法。
     * 
     * <p>实现步骤：
     * 1. 接收并校验调用方传入的数据；
     * 2. 按当前方法职责执行业务查询、转换或持久化处理；
     * 3. 返回处理结果或更新对象状态。</p>
     */
    public ApiResponse<UserView> createUser(@Valid @RequestBody UserCreateRequest request) {
        return ApiResponse.ok("人员已创建", authService.createUser(request));
    }

    @ApiOperationSupport(order = 60, author = "ratel / WenZhang / 18782945613")
    @Operation(summary = "修改人员", description = "作者：ratel；开发人员：WenZhang；联系方式：18782945613。")
    @PreAuthorize("hasAuthority('SYSTEM_USER_MANAGE')")
    @PutMapping("/users/{id}")
    /**
     * 执行 updateUser 方法。
     * 
     * <p>实现步骤：
     * 1. 接收并校验调用方传入的数据；
     * 2. 按当前方法职责执行业务查询、转换或持久化处理；
     * 3. 返回处理结果或更新对象状态。</p>
     */
    public ApiResponse<UserView> updateUser(@PathVariable Long id, @Valid @RequestBody UserUpdateRequest request) {
        return ApiResponse.ok("人员已更新", authService.updateUser(id, request));
    }

    @ApiOperationSupport(order = 65, author = "ratel / WenZhang / 18782945613")
    @Operation(summary = "修改人员密码", description = "作者：ratel；开发人员：WenZhang；联系方式：18782945613。有人员管理授权者可重置人员密码。")
    @PreAuthorize("hasAuthority('SYSTEM_USER_MANAGE')")
    @PutMapping("/users/{id}/password")
    /**
     * 执行 changeUserPassword 方法。
     * 
     * <p>实现步骤：
     * 1. 接收并校验调用方传入的数据；
     * 2. 按当前方法职责执行业务查询、转换或持久化处理；
     * 3. 返回处理结果或更新对象状态。</p>
     */
    public ApiResponse<Void> changeUserPassword(@PathVariable Long id, @Valid @RequestBody PasswordChangeRequest request) {
        authService.changeUserPassword(id, request);
        return ApiResponse.ok("人员密码已修改", null);
    }

    @ApiOperationSupport(order = 66, author = "ratel / WenZhang / 18782945613")
    @Operation(summary = "上传人员头像", description = "作者：ratel；开发人员：WenZhang；联系方式：18782945613。有人员管理授权者可维护人员头像；头像以 Base64 存入用户表，只允许 jpg、jpeg、png、webp 图片。")
    @PreAuthorize("hasAuthority('SYSTEM_USER_MANAGE')")
    @PostMapping("/users/{id}/avatar")
    /**
     * 执行 uploadUserAvatar 方法。
     * 
     * <p>实现步骤：
     * 1. 接收并校验调用方传入的数据；
     * 2. 按当前方法职责执行业务查询、转换或持久化处理；
     * 3. 返回处理结果或更新对象状态。</p>
     */
    public ApiResponse<UserView> uploadUserAvatar(@PathVariable Long id, @RequestParam("file") MultipartFile file) throws IOException {
        // 变量说明：avatarBase64 保存当前步骤计算、查询或转换得到的中间结果。
        String avatarBase64 = readAvatarBase64(file);
        return ApiResponse.ok("人员头像已上传", authService.updateAvatar(id, avatarBase64, false));
    }

    @ApiOperationSupport(order = 70, author = "ratel / WenZhang / 18782945613")
    @Operation(summary = "删除人员", description = "作者：ratel；开发人员：WenZhang；联系方式：18782945613。")
    @PreAuthorize("hasAuthority('SYSTEM_USER_MANAGE')")
    @DeleteMapping("/users/{id}")
    /**
     * 执行 deleteUser 方法。
     * 
     * <p>实现步骤：
     * 1. 接收并校验调用方传入的数据；
     * 2. 按当前方法职责执行业务查询、转换或持久化处理；
     * 3. 返回处理结果或更新对象状态。</p>
     */
    public ApiResponse<Void> deleteUser(@PathVariable Long id) {
        authService.deleteUser(id);
        return ApiResponse.ok("人员已删除", null);
    }

    @ApiOperationSupport(order = 75, author = "ratel / WenZhang / 18782945613")
    @Operation(summary = "批量删除人员", description = "作者：ratel；开发人员：WenZhang；联系方式：18782945613。前端必须二次确认后调用。")
    @PreAuthorize("hasAuthority('SYSTEM_USER_MANAGE')")
    @PostMapping("/users/batch-delete")
    /**
     * 执行 deleteUsers 方法。
     * 
     * <p>实现步骤：
     * 1. 接收并校验调用方传入的数据；
     * 2. 按当前方法职责执行业务查询、转换或持久化处理；
     * 3. 返回处理结果或更新对象状态。</p>
     */
    public ApiResponse<Void> deleteUsers(@Valid @RequestBody BatchIdsRequest request) {
        authService.deleteUsers(request.ids());
        return ApiResponse.ok("人员已批量删除", null);
    }

    @ApiOperationSupport(order = 80, author = "ratel / WenZhang / 18782945613")
    @Operation(summary = "查询角色列表", description = "作者：ratel；开发人员：WenZhang；联系方式：18782945613。")
    @PreAuthorize("hasAuthority('SYSTEM_ROLE_MANAGE')")
    @GetMapping("/roles")
    /**
     * 执行 listRoles 方法。
     * 
     * <p>实现步骤：
     * 1. 接收并校验调用方传入的数据；
     * 2. 按当前方法职责执行业务查询、转换或持久化处理；
     * 3. 返回处理结果或更新对象状态。</p>
     */
    public ApiResponse<List<RoleView>> listRoles() {
        return ApiResponse.ok(authService.listRoles());
    }

    @ApiOperationSupport(order = 85, author = "ratel / WenZhang / 18782945613")
    @Operation(summary = "查询菜单资源", description = "作者：ratel；开发人员：WenZhang；联系方式：18782945613。角色授权时选择模块、页面和按钮。")
    @PreAuthorize("hasAuthority('SYSTEM_ROLE_MANAGE')")
    @GetMapping("/menus")
    /**
     * 执行 listMenus 方法。
     * 
     * <p>实现步骤：
     * 1. 接收并校验调用方传入的数据；
     * 2. 按当前方法职责执行业务查询、转换或持久化处理；
     * 3. 返回处理结果或更新对象状态。</p>
     */
    public ApiResponse<List<MenuView>> listMenus() {
        return ApiResponse.ok(authService.listMenus());
    }

    @ApiOperationSupport(order = 86, author = "ratel / WenZhang / 18782945613")
    @Operation(summary = "查询全部菜单资源", description = "作者：ratel；开发人员：WenZhang；联系方式：18782945613。菜单管理页面使用，包含停用菜单。")
    @PreAuthorize("hasAuthority('SYSTEM_ROLE_MANAGE')")
    @GetMapping("/menus/all")
    /**
     * 执行 listAllMenus 方法。
     * 
     * <p>实现步骤：
     * 1. 接收并校验调用方传入的数据；
     * 2. 按当前方法职责执行业务查询、转换或持久化处理；
     * 3. 返回处理结果或更新对象状态。</p>
     */
    public ApiResponse<List<MenuView>> listAllMenus(
            @RequestParam(required = false) String code,
            @RequestParam(required = false) String name,
            @RequestParam(required = false) MenuType type,
            @RequestParam(required = false) String routePath,
            @RequestParam(required = false) PermissionCode permissionCode,
            @RequestParam(required = false) Boolean enabled
    ) {
        return ApiResponse.ok(authService.listAllMenus(code, name, type, routePath, permissionCode, enabled));
    }

    @ApiOperationSupport(order = 87, author = "ratel / WenZhang / 18782945613")
    @Operation(summary = "新增或更新菜单资源", description = "作者：ratel；开发人员：WenZhang；联系方式：18782945613。维护模块、页面、按钮层级授权资源。")
    @PreAuthorize("hasAuthority('SYSTEM_ROLE_MANAGE')")
    @PostMapping("/menus")
    /**
     * 执行 saveMenu 方法。
     * 
     * <p>实现步骤：
     * 1. 接收并校验调用方传入的数据；
     * 2. 按当前方法职责执行业务查询、转换或持久化处理；
     * 3. 返回处理结果或更新对象状态。</p>
     */
    public ApiResponse<MenuView> saveMenu(@Valid @RequestBody MenuRequest request) {
        return ApiResponse.ok("菜单已保存", authService.saveMenu(request));
    }

    @ApiOperationSupport(order = 88, author = "ratel / WenZhang / 18782945613")
    @Operation(summary = "删除菜单资源", description = "作者：ratel；开发人员：WenZhang；联系方式：18782945613。存在下级菜单时不允许删除。")
    @PreAuthorize("hasAuthority('SYSTEM_ROLE_MANAGE')")
    @DeleteMapping("/menus/{id}")
    /**
     * 执行 deleteMenu 方法。
     * 
     * <p>实现步骤：
     * 1. 接收并校验调用方传入的数据；
     * 2. 按当前方法职责执行业务查询、转换或持久化处理；
     * 3. 返回处理结果或更新对象状态。</p>
     */
    public ApiResponse<Void> deleteMenu(@PathVariable Long id) {
        authService.deleteMenu(id);
        return ApiResponse.ok("菜单已删除", null);
    }

    @ApiOperationSupport(order = 90, author = "ratel / WenZhang / 18782945613")
    @Operation(summary = "新增或更新角色", description = "作者：ratel；开发人员：WenZhang；联系方式：18782945613。")
    @PreAuthorize("hasAuthority('SYSTEM_ROLE_MANAGE')")
    @PostMapping("/roles")
    /**
     * 执行 saveRole 方法。
     * 
     * <p>实现步骤：
     * 1. 接收并校验调用方传入的数据；
     * 2. 按当前方法职责执行业务查询、转换或持久化处理；
     * 3. 返回处理结果或更新对象状态。</p>
     */
    public ApiResponse<RoleView> saveRole(@Valid @RequestBody RoleRequest request) {
        return ApiResponse.ok("角色已保存", authService.saveRole(request));
    }

    @ApiOperationSupport(order = 100, author = "ratel / WenZhang / 18782945613")
    @Operation(summary = "删除角色", description = "作者：ratel；开发人员：WenZhang；联系方式：18782945613。")
    @PreAuthorize("hasAuthority('SYSTEM_ROLE_MANAGE')")
    @DeleteMapping("/roles/{id}")
    /**
     * 执行 deleteRole 方法。
     * 
     * <p>实现步骤：
     * 1. 接收并校验调用方传入的数据；
     * 2. 按当前方法职责执行业务查询、转换或持久化处理；
     * 3. 返回处理结果或更新对象状态。</p>
     */
    public ApiResponse<Void> deleteRole(@PathVariable Long id) {
        authService.deleteRole(id);
        return ApiResponse.ok("角色已删除", null);
    }

    /**
     * 执行 clientIp 方法。
     * 
     * <p>实现步骤：
     * 1. 接收并校验调用方传入的数据；
     * 2. 按当前方法职责执行业务查询、转换或持久化处理；
     * 3. 返回处理结果或更新对象状态。</p>
     */
    private String clientIp(HttpServletRequest request) {
        return ClientIpUtils.resolve(request);
    }

    /**
     * 读取头像图片并转换为浏览器可直接展示的 Base64 数据 URL。
     *
     * <p>实现步骤：
     * 1. 校验文件非空，限制 2MB 以内，避免用户表存入过大的图片文本；
     * 2. 同时校验文件扩展名和 Content-Type，防止非图片文件伪装上传；
     * 3. 读取文件字节并编码为 data:image/...;base64,...，由服务层写入用户表 avatar_base64 字段。</p>
     */
    private String readAvatarBase64(MultipartFile file) throws IOException {
        if (file == null || file.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "头像文件不能为空");
        }
        if (file.getSize() > 2 * 1024 * 1024) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "头像文件不能超过2MB");
        }
        // 变量说明：originalFilename 保存当前步骤计算、查询或转换得到的中间结果。
        String originalFilename = file.getOriginalFilename() == null ? "" : file.getOriginalFilename();
        // 变量说明：extension 保存当前步骤计算、查询或转换得到的中间结果。
        String extension = StringUtils.getFilenameExtension(originalFilename);
        // 变量说明：normalizedExtension 保存当前步骤计算、查询或转换得到的中间结果。
        String normalizedExtension = extension == null ? "" : extension.toLowerCase(Locale.ROOT);
        Map<String, String> contentTypes = Map.of(
                "jpg", "image/jpeg",
                "jpeg", "image/jpeg",
                "png", "image/png",
                "webp", "image/webp"
        );
        // 变量说明：expectedContentType 保存当前步骤计算、查询或转换得到的中间结果。
        String expectedContentType = contentTypes.get(normalizedExtension);
        // 变量说明：actualContentType 保存当前步骤计算、查询或转换得到的中间结果。
        String actualContentType = file.getContentType() == null ? "" : file.getContentType().toLowerCase(Locale.ROOT);
        if (expectedContentType == null || !expectedContentType.equals(actualContentType)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "头像仅支持 jpg、jpeg、png、webp");
        }
        return "data:" + expectedContentType + ";base64," + Base64.getEncoder().encodeToString(file.getBytes());
    }
}
