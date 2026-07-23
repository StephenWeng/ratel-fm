package com.ratel.fm.domain.auth;

import com.ratel.fm.common.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.Table;
import org.hibernate.annotations.Comment;

import java.util.HashSet;
import java.util.Set;

/**
 * 系统登录人员账号。
 *
 * <p>该实体既承载登录账号，也承载 JWT 中需要校验的人员基础信息。登录账号和身份证号在同一所属公司内唯一，
 * 不同所属公司可存在相同账号或身份证号，用于支持多公司账套隔离。</p>
 *
 * <p>开发组织：ratel；开发人员：WenZhang；联系方式：18782945613。</p>
 *
 * @author ratel
 */
@Entity
@Table(name = "fm_users")
@Comment("系统人员账号表，保存登录账号、人员基础信息和角色授权关系")
public class UserAccount extends BaseEntity {

    /** 登录账号，在同一所属公司内唯一，可用于账号密码登录。 */
    @Column(nullable = false, length = 80)
    @Comment("登录账号，同一所属公司内唯一，可用于账号密码登录")
    private String username;

    /** 人员真实姓名，会写入 JWT，并在每次请求时和数据库人员信息比对；限制为 20 个中文字符。 */
    @Column(nullable = false, length = 20)
    @Comment("人员真实姓名")
    private String realName;

    /** BCrypt 哈希后的密码，不保存明文密码。 */
    @Column(nullable = false)
    @Comment("BCrypt加密后的登录密码")
    private String passwordHash;

    /** 人员所属部门，会写入 JWT，用于展示和后续数据权限扩展。 */
    @Column(length = 80)
    @Comment("人员所属部门")
    private String department;

    /** 所属公司字典编码，即账套编码，所有业务数据按该编码隔离。 */
    @Column(length = 80)
    @Comment("人员所属公司字典编码，作为账套隔离标识")
    private String organizationCode;

    /** 岗位名称，预留给岗位级权限和审计展示。 */
    @Column(length = 80)
    @Comment("人员岗位名称")
    private String position;

    /** 身份证号，在同一所属公司内唯一，是人员管理唯一性校验字段和唯一登录控制业务主键。 */
    @Column(length = 40)
    @Comment("身份证号，同一所属公司内唯一，是人员管理唯一性校验字段")
    private String identityNo;

    /** 联系电话，支持手机号或座机号。APP 端登录时可作为终端唯一标识。 */
    @Column(length = 30)
    @Comment("人员联系电话")
    private String phone;

    /** 邮箱地址，仅作为人员档案信息，不参与登录。 */
    @Column(length = 120)
    @Comment("人员邮箱地址")
    private String email;

    /** 兼容历史头像照片访问地址，新的上传头像保存到 avatarBase64。 */
    @Column(length = 300)
    @Comment("兼容历史头像照片访问地址")
    private String avatarUrl;

    /** 头像图片 Base64 数据，保存 data:image/...;base64,... 形式，避免小规模部署额外依赖文件存储。 */
    @Column(columnDefinition = "text")
    @Comment("头像图片Base64数据")
    private String avatarBase64;

    /** 是否启用。禁用人员即使 JWT 未过期也不能继续访问接口。 */
    @Column(nullable = false)
    @Comment("是否启用账号")
    private boolean enabled = true;

    /** 人员拥有的角色集合，使用立即加载以便请求鉴权时快速组装权限码。 */
    @ManyToMany(fetch = FetchType.EAGER)
    @JoinTable(
            name = "fm_user_roles",
            joinColumns = @JoinColumn(name = "user_id"),
            inverseJoinColumns = @JoinColumn(name = "role_id")
    )
    /**
     * 用户角色集合字段，用于登录鉴权时展开权限码和菜单资源。
     */
    private Set<Role> roles = new HashSet<>();

    /**
     * 执行 getUsername 方法。
     * 
     * <p>实现步骤：
     * 1. 接收并校验调用方传入的数据；
     * 2. 按当前方法职责执行业务查询、转换或持久化处理；
     * 3. 返回处理结果或更新对象状态。</p>
     */
    public String getUsername() {
        return username;
    }

    /**
     * 执行 setUsername 方法。
     * 
     * <p>实现步骤：
     * 1. 接收并校验调用方传入的数据；
     * 2. 按当前方法职责执行业务查询、转换或持久化处理；
     * 3. 返回处理结果或更新对象状态。</p>
     */
    public void setUsername(String username) {
        this.username = username;
    }

    /**
     * 执行 getRealName 方法。
     * 
     * <p>实现步骤：
     * 1. 接收并校验调用方传入的数据；
     * 2. 按当前方法职责执行业务查询、转换或持久化处理；
     * 3. 返回处理结果或更新对象状态。</p>
     */
    public String getRealName() {
        return realName;
    }

    /**
     * 执行 setRealName 方法。
     * 
     * <p>实现步骤：
     * 1. 接收并校验调用方传入的数据；
     * 2. 按当前方法职责执行业务查询、转换或持久化处理；
     * 3. 返回处理结果或更新对象状态。</p>
     */
    public void setRealName(String realName) {
        this.realName = realName;
    }

    /**
     * 执行 getPasswordHash 方法。
     * 
     * <p>实现步骤：
     * 1. 接收并校验调用方传入的数据；
     * 2. 按当前方法职责执行业务查询、转换或持久化处理；
     * 3. 返回处理结果或更新对象状态。</p>
     */
    public String getPasswordHash() {
        return passwordHash;
    }

    /**
     * 执行 setPasswordHash 方法。
     * 
     * <p>实现步骤：
     * 1. 接收并校验调用方传入的数据；
     * 2. 按当前方法职责执行业务查询、转换或持久化处理；
     * 3. 返回处理结果或更新对象状态。</p>
     */
    public void setPasswordHash(String passwordHash) {
        this.passwordHash = passwordHash;
    }

    /**
     * 执行 getDepartment 方法。
     * 
     * <p>实现步骤：
     * 1. 接收并校验调用方传入的数据；
     * 2. 按当前方法职责执行业务查询、转换或持久化处理；
     * 3. 返回处理结果或更新对象状态。</p>
     */
    public String getDepartment() {
        return department;
    }

    /**
     * 执行 setDepartment 方法。
     * 
     * <p>实现步骤：
     * 1. 接收并校验调用方传入的数据；
     * 2. 按当前方法职责执行业务查询、转换或持久化处理；
     * 3. 返回处理结果或更新对象状态。</p>
     */
    public void setDepartment(String department) {
        this.department = department;
    }

    /**
     * 执行 getOrganizationCode 方法。
     * 
     * <p>实现步骤：
     * 1. 接收并校验调用方传入的数据；
     * 2. 按当前方法职责执行业务查询、转换或持久化处理；
     * 3. 返回处理结果或更新对象状态。</p>
     */
    public String getOrganizationCode() {
        return organizationCode;
    }

    /**
     * 执行 setOrganizationCode 方法。
     * 
     * <p>实现步骤：
     * 1. 接收并校验调用方传入的数据；
     * 2. 按当前方法职责执行业务查询、转换或持久化处理；
     * 3. 返回处理结果或更新对象状态。</p>
     */
    public void setOrganizationCode(String organizationCode) {
        this.organizationCode = organizationCode;
    }

    /**
     * 执行 getPosition 方法。
     * 
     * <p>实现步骤：
     * 1. 接收并校验调用方传入的数据；
     * 2. 按当前方法职责执行业务查询、转换或持久化处理；
     * 3. 返回处理结果或更新对象状态。</p>
     */
    public String getPosition() {
        return position;
    }

    /**
     * 执行 setPosition 方法。
     * 
     * <p>实现步骤：
     * 1. 接收并校验调用方传入的数据；
     * 2. 按当前方法职责执行业务查询、转换或持久化处理；
     * 3. 返回处理结果或更新对象状态。</p>
     */
    public void setPosition(String position) {
        this.position = position;
    }

    /**
     * 执行 getIdentityNo 方法。
     * 
     * <p>实现步骤：
     * 1. 接收并校验调用方传入的数据；
     * 2. 按当前方法职责执行业务查询、转换或持久化处理；
     * 3. 返回处理结果或更新对象状态。</p>
     */
    public String getIdentityNo() {
        return identityNo;
    }

    /**
     * 执行 setIdentityNo 方法。
     * 
     * <p>实现步骤：
     * 1. 接收并校验调用方传入的数据；
     * 2. 按当前方法职责执行业务查询、转换或持久化处理；
     * 3. 返回处理结果或更新对象状态。</p>
     */
    public void setIdentityNo(String identityNo) {
        this.identityNo = identityNo;
    }

    /**
     * 执行 getPhone 方法。
     * 
     * <p>实现步骤：
     * 1. 接收并校验调用方传入的数据；
     * 2. 按当前方法职责执行业务查询、转换或持久化处理；
     * 3. 返回处理结果或更新对象状态。</p>
     */
    public String getPhone() {
        return phone;
    }

    /**
     * 执行 setPhone 方法。
     * 
     * <p>实现步骤：
     * 1. 接收并校验调用方传入的数据；
     * 2. 按当前方法职责执行业务查询、转换或持久化处理；
     * 3. 返回处理结果或更新对象状态。</p>
     */
    public void setPhone(String phone) {
        this.phone = phone;
    }

    /**
     * 执行 getEmail 方法。
     * 
     * <p>实现步骤：
     * 1. 接收并校验调用方传入的数据；
     * 2. 按当前方法职责执行业务查询、转换或持久化处理；
     * 3. 返回处理结果或更新对象状态。</p>
     */
    public String getEmail() {
        return email;
    }

    /**
     * 执行 setEmail 方法。
     * 
     * <p>实现步骤：
     * 1. 接收并校验调用方传入的数据；
     * 2. 按当前方法职责执行业务查询、转换或持久化处理；
     * 3. 返回处理结果或更新对象状态。</p>
     */
    public void setEmail(String email) {
        this.email = email;
    }

    /**
     * 执行 getAvatarUrl 方法。
     * 
     * <p>实现步骤：
     * 1. 接收并校验调用方传入的数据；
     * 2. 按当前方法职责执行业务查询、转换或持久化处理；
     * 3. 返回处理结果或更新对象状态。</p>
     */
    public String getAvatarUrl() {
        return avatarUrl;
    }

    /**
     * 执行 setAvatarUrl 方法。
     * 
     * <p>实现步骤：
     * 1. 接收并校验调用方传入的数据；
     * 2. 按当前方法职责执行业务查询、转换或持久化处理；
     * 3. 返回处理结果或更新对象状态。</p>
     */
    public void setAvatarUrl(String avatarUrl) {
        this.avatarUrl = avatarUrl;
    }

    /**
     * 执行 getAvatarBase64 方法。
     * 
     * <p>实现步骤：
     * 1. 接收并校验调用方传入的数据；
     * 2. 按当前方法职责执行业务查询、转换或持久化处理；
     * 3. 返回处理结果或更新对象状态。</p>
     */
    public String getAvatarBase64() {
        return avatarBase64;
    }

    /**
     * 执行 setAvatarBase64 方法。
     * 
     * <p>实现步骤：
     * 1. 接收并校验调用方传入的数据；
     * 2. 按当前方法职责执行业务查询、转换或持久化处理；
     * 3. 返回处理结果或更新对象状态。</p>
     */
    public void setAvatarBase64(String avatarBase64) {
        this.avatarBase64 = avatarBase64;
    }

    /**
     * 执行 isEnabled 方法。
     * 
     * <p>实现步骤：
     * 1. 接收并校验调用方传入的数据；
     * 2. 按当前方法职责执行业务查询、转换或持久化处理；
     * 3. 返回处理结果或更新对象状态。</p>
     */
    public boolean isEnabled() {
        return enabled;
    }

    /**
     * 执行 setEnabled 方法。
     * 
     * <p>实现步骤：
     * 1. 接收并校验调用方传入的数据；
     * 2. 按当前方法职责执行业务查询、转换或持久化处理；
     * 3. 返回处理结果或更新对象状态。</p>
     */
    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    /**
     * 执行 getRoles 方法。
     * 
     * <p>实现步骤：
     * 1. 接收并校验调用方传入的数据；
     * 2. 按当前方法职责执行业务查询、转换或持久化处理；
     * 3. 返回处理结果或更新对象状态。</p>
     */
    public Set<Role> getRoles() {
        return roles;
    }

    /**
     * 执行 setRoles 方法。
     * 
     * <p>实现步骤：
     * 1. 接收并校验调用方传入的数据；
     * 2. 按当前方法职责执行业务查询、转换或持久化处理；
     * 3. 返回处理结果或更新对象状态。</p>
     */
    public void setRoles(Set<Role> roles) {
        this.roles = roles;
    }
}
