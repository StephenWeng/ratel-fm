package com.ratel.fm.web.dto.auth;

import com.ratel.fm.domain.auth.PermissionCode;
import com.ratel.fm.domain.auth.MenuType;
import com.ratel.fm.domain.auth.TerminalType;
import com.ratel.fm.common.validation.ChineseIdentityNo;
import com.ratel.fm.common.validation.ChineseName;
import com.ratel.fm.common.validation.ContactPhone;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Set;

/**
 * 认证、人员、角色和菜单接口 DTO。
 *
 * <p>实现目的：
 * 1. 登录请求和人员资料请求在 DTO 层完成基础格式约束；
 * 2. 人员姓名、身份证号和联系方式使用统一校验注解，避免各页面规则不一致；
 * 3. 角色、菜单字段保留系统授权所需的编码和层级信息。</p>
 */
public final class AuthDtos {

    private AuthDtos() {
    }

    @Schema(description = "登录请求。作者：ratel；开发人员：WenZhang；联系方式：18782945613。")
    /**
     * LoginRequest 数据传输记录。
     * 
     * <p>用于承载 LoginRequest 相关的业务数据、接口契约或处理逻辑，便于维护人员快速定位模块职责。</p>
     */
    public record LoginRequest(
            @Schema(description = "所属公司字典编码。登录时必须选择公司/账套，后端写入 JWT Cookie 用于后续数据隔离。")
            @NotBlank(message = "所属公司不能为空")
            @Size(max = 80, message = "所属公司编码长度不能超过80个字符")
            /**
             * 记录组件 organizationCode：表示登录选择的所属公司字典编码。
             */
            String organizationCode,
            @Schema(description = "登录账号。可填写唯一账号或唯一身份证号。")
            @NotBlank(message = "登录账号不能为空")
            @Size(max = 80, message = "登录账号长度不能超过80个字符")
            /**
             * 记录组件 username：表示接口入参或出参中的 username 字段。
             */
            String username,
            @Schema(description = "登录密码。")
            @NotBlank(message = "登录密码不能为空")
            @Size(max = 72, message = "登录密码长度不能超过72个字符")
            /**
             * 记录组件 password：表示接口入参或出参中的 password 字段。
             */
            String password,
            @Schema(description = "终端类型，PC 或 APP；为空时默认 PC。")
            /**
             * 记录组件 terminalType：表示接口入参或出参中的 terminalType 字段。
             */
            TerminalType terminalType,
            @Schema(description = "终端标识。PC 端由后端使用请求 IP，不信任前端传值；APP 端使用手机号。")
            @Size(max = 120, message = "终端标识长度不能超过120个字符")
            /**
             * 记录组件 terminalIdentifier：表示接口入参或出参中的 terminalIdentifier 字段。
             */
            String terminalIdentifier,
            @Schema(description = "是否强制登录。重复登录确认后传 true，用于挤掉旧登录会话。")
            /**
             * 记录组件 force：表示接口入参或出参中的 force 字段。
             */
            Boolean force
    ) {
    }

    @Schema(description = "登录响应。JWT 写入浏览器 HttpOnly Cookie。作者：ratel；开发人员：WenZhang；联系方式：18782945613。")
    /**
     * LoginResponse 数据传输记录。
     * 
     * <p>用于承载 LoginResponse 相关的业务数据、接口契约或处理逻辑，便于维护人员快速定位模块职责。</p>
     */
    public record LoginResponse(
            @Schema(description = "JWT 和登录会话过期时间。重复登录提醒时为空。")
            /**
             * 记录组件 expiresAt：表示接口入参或出参中的 expiresAt 字段。
             */
            OffsetDateTime expiresAt,
            @Schema(description = "登录人员基础信息和角色权限。")
            /**
             * 记录组件 user：表示接口入参或出参中的 user 字段。
             */
            UserView user,
            @Schema(description = "是否发生同身份证同终端类型重复登录。")
            /**
             * 记录组件 repeated：表示接口入参或出参中的 repeated 字段。
             */
            Boolean repeated,
            @Schema(description = "重复登录提示语，前端用于确认是否挤掉之前登录者。")
            /**
             * 记录组件 conflictMessage：表示接口入参或出参中的 conflictMessage 字段。
             */
            String conflictMessage
    ) {
    }

    @Schema(description = "人员新增请求。作者：ratel；开发人员：WenZhang；联系方式：18782945613。")
    /**
     * UserCreateRequest 数据传输记录。
     * 
     * <p>用于承载 UserCreateRequest 相关的业务数据、接口契约或处理逻辑，便于维护人员快速定位模块职责。</p>
     */
    public record UserCreateRequest(
            @Schema(description = "登录账号，同一所属公司内唯一。")
            @NotBlank(message = "登录账号不能为空")
            @Size(min = 3, max = 80, message = "登录账号长度必须在3到80个字符之间")
            @jakarta.validation.constraints.Pattern(regexp = "^[A-Za-z0-9_.@-]+$", message = "登录账号只能包含字母、数字、下划线、横线、点和@")
            /**
             * 记录组件 username：表示接口入参或出参中的 username 字段。
             */
            String username,
            @Schema(description = "人员真实姓名。")
            @NotBlank(message = "姓名不能为空")
            @ChineseName
            @Size(max = 20, message = "姓名不能超过20个中文字符")
            /**
             * 记录组件 realName：表示接口入参或出参中的 realName 字段。
             */
            String realName,
            @Schema(description = "初始密码，保存时会 BCrypt 加密。")
            @NotBlank(message = "初始密码不能为空")
            @Size(min = 6, max = 72, message = "密码长度必须在6到72个字符之间")
            /**
             * 记录组件 password：表示接口入参或出参中的 password 字段。
             */
            String password,
            @Schema(description = "部门。")
            @Size(max = 80, message = "部门长度不能超过80个字符")
            /**
             * 记录组件 department：表示接口入参或出参中的 department 字段。
             */
            String department,
            @Schema(description = "所属公司字典编码。admin 可维护；非 admin 创建人员时自动使用当前登录公司。")
            @Size(max = 80, message = "所属公司编码长度不能超过80个字符")
            /**
             * 记录组件 organizationCode：表示接口入参或出参中的 organizationCode 字段。
             */
            String organizationCode,
            @Schema(description = "岗位。")
            @Size(max = 80, message = "岗位长度不能超过80个字符")
            /**
             * 记录组件 position：表示接口入参或出参中的 position 字段。
             */
            String position,
            @Schema(description = "身份证号，同一所属公司内唯一，是唯一登录判断依据。")
            @NotBlank(message = "身份证号不能为空")
            @ChineseIdentityNo
            /**
             * 记录组件 identityNo：表示接口入参或出参中的 identityNo 字段。
             */
            String identityNo,
            @Schema(description = "联系电话；APP 端可作为终端标识。")
            @ContactPhone
            @Size(max = 30, message = "联系电话长度不能超过30个字符")
            /**
             * 记录组件 phone：表示接口入参或出参中的 phone 字段。
             */
            String phone,
            @Schema(description = "邮箱。")
            @Email(message = "邮箱格式不正确")
            @Size(max = 120, message = "邮箱长度不能超过120个字符")
            /**
             * 记录组件 email：表示接口入参或出参中的 email 字段。
             */
            String email,
            @Schema(description = "兼容历史头像访问地址；新增头像请通过头像上传接口写入 Base64。")
            @Size(max = 300, message = "头像地址长度不能超过300个字符")
            /**
             * 记录组件 avatarUrl：表示接口入参或出参中的 avatarUrl 字段。
             */
            String avatarUrl,
            @Schema(description = "是否启用；为空时按启用处理。")
            /**
             * 记录组件 enabled：表示接口入参或出参中的 enabled 字段。
             */
            Boolean enabled,
            @Schema(description = "角色编码集合。")
            /**
             * 记录组件 roleCodes：表示接口入参或出参中的 roleCodes 字段。
             */
            Set<String> roleCodes
    ) {
    }

    @Schema(description = "人员修改请求。作者：ratel；开发人员：WenZhang；联系方式：18782945613。")
    /**
     * UserUpdateRequest 数据传输记录。
     * 
     * <p>用于承载 UserUpdateRequest 相关的业务数据、接口契约或处理逻辑，便于维护人员快速定位模块职责。</p>
     */
    public record UserUpdateRequest(
            @Schema(description = "人员真实姓名。")
            @NotBlank(message = "姓名不能为空")
            @ChineseName
            @Size(max = 20, message = "姓名不能超过20个中文字符")
            /**
             * 记录组件 realName：表示接口入参或出参中的 realName 字段。
             */
            String realName,
            @Schema(description = "新密码；为空表示不修改密码。")
            @jakarta.validation.constraints.Pattern(regexp = "^$|^.{6,72}$", message = "密码长度必须在6到72个字符之间")
            /**
             * 记录组件 password：表示接口入参或出参中的 password 字段。
             */
            String password,
            @Schema(description = "部门。")
            @Size(max = 80, message = "部门长度不能超过80个字符")
            /**
             * 记录组件 department：表示接口入参或出参中的 department 字段。
             */
            String department,
            @Schema(description = "所属公司字典编码。admin 可维护；非 admin 修改人员时自动使用当前登录公司。")
            @Size(max = 80, message = "所属公司编码长度不能超过80个字符")
            /**
             * 记录组件 organizationCode：表示接口入参或出参中的 organizationCode 字段。
             */
            String organizationCode,
            @Schema(description = "岗位。")
            @Size(max = 80, message = "岗位长度不能超过80个字符")
            /**
             * 记录组件 position：表示接口入参或出参中的 position 字段。
             */
            String position,
            @Schema(description = "身份证号，同一所属公司内唯一，是唯一登录判断依据。")
            @NotBlank(message = "身份证号不能为空")
            @ChineseIdentityNo
            /**
             * 记录组件 identityNo：表示接口入参或出参中的 identityNo 字段。
             */
            String identityNo,
            @Schema(description = "联系电话；APP 端可作为终端标识。")
            @ContactPhone
            @Size(max = 30, message = "联系电话长度不能超过30个字符")
            /**
             * 记录组件 phone：表示接口入参或出参中的 phone 字段。
             */
            String phone,
            @Schema(description = "邮箱。")
            @Email(message = "邮箱格式不正确")
            @Size(max = 120, message = "邮箱长度不能超过120个字符")
            /**
             * 记录组件 email：表示接口入参或出参中的 email 字段。
             */
            String email,
            @Schema(description = "兼容历史头像访问地址；新增头像请通过头像上传接口写入 Base64。")
            @Size(max = 300, message = "头像地址长度不能超过300个字符")
            /**
             * 记录组件 avatarUrl：表示接口入参或出参中的 avatarUrl 字段。
             */
            String avatarUrl,
            @Schema(description = "是否启用。禁用后人员不能继续访问接口。")
            /**
             * 记录组件 enabled：表示接口入参或出参中的 enabled 字段。
             */
            Boolean enabled,
            @Schema(description = "角色编码集合。")
            /**
             * 记录组件 roleCodes：表示接口入参或出参中的 roleCodes 字段。
             */
            Set<String> roleCodes
    ) {
    }

    @Schema(description = "角色保存请求。作者：ratel；开发人员：WenZhang；联系方式：18782945613。")
    /**
     * RoleRequest 数据传输记录。
     * 
     * <p>用于承载 RoleRequest 相关的业务数据、接口契约或处理逻辑，便于维护人员快速定位模块职责。</p>
     */
    public record RoleRequest(
            @Schema(description = "角色编码，全系统唯一。")
            @NotBlank(message = "角色编码不能为空")
            @Size(max = 80, message = "角色编码长度不能超过80个字符")
            @jakarta.validation.constraints.Pattern(regexp = "^[A-Z0-9_]+$", message = "角色编码只能包含大写字母、数字和下划线")
            /**
             * 记录组件 code：表示接口入参或出参中的 code 字段。
             */
            String code,
            @Schema(description = "角色名称。")
            @NotBlank(message = "角色名称不能为空")
            @Size(max = 120, message = "角色名称长度不能超过120个字符")
            /**
             * 记录组件 name：表示接口入参或出参中的 name 字段。
             */
            String name,
            @Schema(description = "角色说明。")
            @Size(max = 2000, message = "角色说明长度不能超过2000个中文字符")
            /**
             * 记录组件 description：表示接口入参或出参中的 description 字段。
             */
            String description,
            @Schema(description = "菜单编码集合，覆盖模块、页面和按钮授权。")
            /**
             * 记录组件 menuCodes：表示接口入参或出参中的 menuCodes 字段。
             */
            Set<String> menuCodes
    ) {
    }

    @Schema(description = "角色视图。作者：ratel；开发人员：WenZhang；联系方式：18782945613。")
    /**
     * RoleView 数据传输记录。
     * 
     * <p>用于承载 RoleView 相关的业务数据、接口契约或处理逻辑，便于维护人员快速定位模块职责。</p>
     */
    public record RoleView(
            /**
             * 记录组件 id：表示接口入参或出参中的 id 字段。
             */
            Long id,
            /**
             * 记录组件 code：表示接口入参或出参中的 code 字段。
             */
            String code,
            /**
             * 记录组件 name：表示接口入参或出参中的 name 字段。
             */
            String name,
            /**
             * 记录组件 description：表示接口入参或出参中的 description 字段。
             */
            String description,
            /**
             * 记录组件 permissions：表示接口入参或出参中的 permissions 字段。
             */
            Set<PermissionCode> permissions,
            /**
             * 记录组件 menuCodes：表示接口入参或出参中的 menuCodes 字段。
             */
            Set<String> menuCodes
    ) {
    }

    @Schema(description = "菜单保存请求，维护模块、页面、按钮层级授权资源。作者：ratel；开发人员：WenZhang；联系方式：18782945613。")
    /**
     * MenuRequest 数据传输记录。
     * 
     * <p>用于承载 MenuRequest 相关的业务数据、接口契约或处理逻辑，便于维护人员快速定位模块职责。</p>
     */
    public record MenuRequest(
            @Schema(description = "菜单编码，全系统唯一，前端用该编码控制模块、页面和按钮显隐。")
            @NotBlank(message = "菜单编码不能为空")
            @Size(max = 120, message = "菜单编码长度不能超过120个字符")
            @jakarta.validation.constraints.Pattern(regexp = "^[A-Z0-9_]+$", message = "菜单编码只能包含大写字母、数字和下划线")
            /**
             * 记录组件 code：表示接口入参或出参中的 code 字段。
             */
            String code,
            @Schema(description = "菜单名称。")
            @NotBlank(message = "菜单名称不能为空")
            @Size(max = 120, message = "菜单名称长度不能超过120个字符")
            /**
             * 记录组件 name：表示接口入参或出参中的 name 字段。
             */
            String name,
            @Schema(description = "菜单类型：MODULE 表示模块，PAGE 表示页面，BUTTON 表示按钮。")
            /**
             * 记录组件 type：表示接口入参或出参中的 type 字段。
             */
            MenuType type,
            @Schema(description = "父级菜单ID。模块为空，页面选择模块，按钮选择页面。")
            /**
             * 记录组件 parentId：表示接口入参或出参中的 parentId 字段。
             */
            Long parentId,
            @Schema(description = "前端路由路径，页面菜单使用，模块和按钮可为空。")
            @Size(max = 200, message = "前端路由长度不能超过200个字符")
            /**
             * 记录组件 routePath：表示接口入参或出参中的 routePath 字段。
             */
            String routePath,
            @Schema(description = "排序号，越小越靠前。")
            /**
             * 记录组件 sortOrder：表示接口入参或出参中的 sortOrder 字段。
             */
            Integer sortOrder,
            @Schema(description = "是否启用；为空时按启用处理。")
            /**
             * 记录组件 enabled：表示接口入参或出参中的 enabled 字段。
             */
            Boolean enabled,
            @Schema(description = "绑定的后端权限码，用于接口 @PreAuthorize 兜底校验；没有后端接口时可为空。")
            /**
             * 记录组件 permissionCode：表示接口入参或出参中的 permissionCode 字段。
             */
            PermissionCode permissionCode
    ) {
    }

    @Schema(description = "菜单资源视图。作者：ratel；开发人员：WenZhang；联系方式：18782945613。")
    /**
     * MenuView 数据传输记录。
     * 
     * <p>用于承载 MenuView 相关的业务数据、接口契约或处理逻辑，便于维护人员快速定位模块职责。</p>
     */
    public record MenuView(
            /**
             * 记录组件 id：表示接口入参或出参中的 id 字段。
             */
            Long id,
            /**
             * 记录组件 code：表示接口入参或出参中的 code 字段。
             */
            String code,
            /**
             * 记录组件 name：表示接口入参或出参中的 name 字段。
             */
            String name,
            /**
             * 记录组件 type：表示接口入参或出参中的 type 字段。
             */
            String type,
            /**
             * 记录组件 parentId：表示接口入参或出参中的 parentId 字段。
             */
            Long parentId,
            /**
             * 记录组件 routePath：表示接口入参或出参中的 routePath 字段。
             */
            String routePath,
            /**
             * 记录组件 sortOrder：表示接口入参或出参中的 sortOrder 字段。
             */
            Integer sortOrder,
            /**
             * 记录组件 enabled：表示接口入参或出参中的 enabled 字段。
             */
            Boolean enabled,
            /**
             * 记录组件 permissionCode：表示接口入参或出参中的 permissionCode 字段。
             */
            PermissionCode permissionCode
    ) {
    }

    @Schema(description = "当前用户常用菜单记录请求。")
    public record MenuUsageRequest(
            @Schema(description = "菜单编码。")
            @NotBlank(message = "菜单编码不能为空")
            @Size(max = 120, message = "菜单编码长度不能超过120个字符")
            String menuCode,
            @Schema(description = "菜单名称快照。")
            @Size(max = 120, message = "菜单名称长度不能超过120个字符")
            String menuName,
            @Schema(description = "前端路由路径。")
            @Size(max = 200, message = "前端路由长度不能超过200个字符")
            String routePath
    ) {
    }

    @Schema(description = "当前用户常用菜单视图。")
    public record MenuUsageView(
            String menuCode,
            String menuName,
            String routePath,
            long useCount,
            String lastUsedAt
    ) {
    }

    @Schema(description = "个人资料修改请求。作者：ratel；开发人员：WenZhang；联系方式：18782945613。")
    /**
     * ProfileUpdateRequest 数据传输记录。
     * 
     * <p>用于承载 ProfileUpdateRequest 相关的业务数据、接口契约或处理逻辑，便于维护人员快速定位模块职责。</p>
     */
    public record ProfileUpdateRequest(
            @Schema(description = "姓名。")
            @NotBlank(message = "姓名不能为空")
            @ChineseName
            @Size(max = 20, message = "姓名不能超过20个中文字符")
            /**
             * 记录组件 realName：表示接口入参或出参中的 realName 字段。
             */
            String realName,
            @Schema(description = "身份证号。默认账号不允许自行修改。")
            @ChineseIdentityNo
            /**
             * 记录组件 identityNo：表示接口入参或出参中的 identityNo 字段。
             */
            String identityNo,
            @Schema(description = "联系方式，支持手机号或座机号。")
            @ContactPhone
            @Size(max = 30, message = "联系电话长度不能超过30个字符")
            /**
             * 记录组件 phone：表示接口入参或出参中的 phone 字段。
             */
            String phone,
            @Schema(description = "邮箱。")
            @Email(message = "邮箱格式不正确")
            @Size(max = 120, message = "邮箱长度不能超过120个字符")
            /**
             * 记录组件 email：表示接口入参或出参中的 email 字段。
             */
            String email,
            @Schema(description = "兼容历史头像访问地址；新增头像请通过头像上传接口写入 Base64。")
            @Size(max = 300, message = "头像地址长度不能超过300个字符")
            /**
             * 记录组件 avatarUrl：表示接口入参或出参中的 avatarUrl 字段。
             */
            String avatarUrl
    ) {
    }

    @Schema(description = "密码修改请求。作者：ratel；开发人员：WenZhang；联系方式：18782945613。")
    /**
     * PasswordChangeRequest 数据传输记录。
     * 
     * <p>用于承载 PasswordChangeRequest 相关的业务数据、接口契约或处理逻辑，便于维护人员快速定位模块职责。</p>
     */
    public record PasswordChangeRequest(
            @Schema(description = "原密码。")
            @Size(max = 72, message = "原密码长度不能超过72个字符")
            /**
             * 记录组件 oldPassword：表示接口入参或出参中的 oldPassword 字段。
             */
            String oldPassword,
            @Schema(description = "新密码。")
            @NotBlank(message = "新密码不能为空")
            @Size(min = 6, max = 72, message = "新密码长度必须在6到72个字符之间")
            /**
             * 记录组件 newPassword：表示接口入参或出参中的 newPassword 字段。
             */
            String newPassword
    ) {
    }

    @Schema(description = "人员视图，包含姓名、身份证、部门、联系方式等 JWT 基础信息。作者：ratel；开发人员：WenZhang；联系方式：18782945613。")
    /**
     * UserView 数据传输记录。
     * 
     * <p>用于承载 UserView 相关的业务数据、接口契约或处理逻辑，便于维护人员快速定位模块职责。</p>
     */
    public record UserView(
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
             * 记录组件 identityNo：表示接口入参或出参中的 identityNo 字段。
             */
            String identityNo,
            /**
             * 记录组件 phone：表示接口入参或出参中的 phone 字段。
             */
            String phone,
            /**
             * 记录组件 email：表示接口入参或出参中的 email 字段。
             */
            String email,
            /**
             * 记录组件 avatarUrl：表示接口入参或出参中的 avatarUrl 字段。
             */
            String avatarUrl,
            /**
             * 记录组件 defaultAccount：表示接口入参或出参中的 defaultAccount 字段。
             */
            boolean defaultAccount,
            /**
             * 记录组件 enabled：表示接口入参或出参中的 enabled 字段。
             */
            boolean enabled,
            /**
             * 记录组件 roles：表示接口入参或出参中的 roles 字段。
             */
            List<RoleView> roles
    ) {
    }

    @Schema(description = "当前登录人授权菜单编码视图。作者：ratel；开发人员：WenZhang；联系方式：18782945613。")
    /**
     * MenuCodeView 数据传输记录。
     * 
     * <p>用于承载 MenuCodeView 相关的业务数据、接口契约或处理逻辑，便于维护人员快速定位模块职责。</p>
     */
    public record MenuCodeView(
            @Schema(description = "当前登录人拥有的模块、页面和按钮菜单编码集合。")
            /**
             * 记录组件 menuCodes：表示接口入参或出参中的 menuCodes 字段。
             */
            Set<String> menuCodes
    ) {
    }
}
