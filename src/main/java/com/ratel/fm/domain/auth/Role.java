package com.ratel.fm.domain.auth;

import com.ratel.fm.common.BaseEntity;
import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.Table;
import org.hibernate.annotations.Comment;

import java.util.HashSet;
import java.util.Set;

/**
 * 系统角色。
 *
 * <p>角色用于把多个权限码组合成模块授权集合，再分配给人员。</p>
 *
 * <p>开发组织：ratel；开发人员：WenZhang；联系方式：18782945613。</p>
 *
 * @author ratel
 */
@Entity
@Table(name = "fm_roles")
@Comment("系统角色表，保存角色编码、名称和模块权限集合")
public class Role extends BaseEntity {

    /** 角色编码，全系统唯一，如 ADMIN、FINANCE、OPERATOR。 */
    @Column(nullable = false, unique = true, length = 80)
    @Comment("角色编码，全系统唯一")
    private String code;

    /** 角色名称，用于前端展示。 */
    @Column(nullable = false, length = 120)
    @Comment("角色名称")
    private String name;

    /** 角色说明，描述该角色的业务职责和授权范围，支持长文本说明。 */
    @Column(length = 2000)
    @Comment("角色说明")
    private String description;

    /** 权限码集合，Spring Security 以这些权限码进行方法级授权判断。 */
    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "fm_role_permissions", joinColumns = @JoinColumn(name = "role_id"))
    @Enumerated(EnumType.STRING)
    @Column(name = "permission_code", nullable = false, length = 80)
    @Comment("角色拥有的权限码")
    /**
     * 权限码集合字段，保存角色可授予用户的后端接口权限。
     */
    private Set<PermissionCode> permissions = new HashSet<>();

    /** 角色拥有的菜单资源集合，覆盖模块、页面和按钮授权。 */
    @ManyToMany(fetch = FetchType.EAGER)
    @JoinTable(
            name = "fm_role_menus",
            joinColumns = @JoinColumn(name = "role_id"),
            inverseJoinColumns = @JoinColumn(name = "menu_id")
    )
    /**
     * 菜单资源集合字段，保存角色可访问的模块、页面和按钮资源。
     */
    private Set<SystemMenu> menus = new HashSet<>();

    /**
     * 执行 getCode 方法。
     * 
     * <p>实现步骤：
     * 1. 接收并校验调用方传入的数据；
     * 2. 按当前方法职责执行业务查询、转换或持久化处理；
     * 3. 返回处理结果或更新对象状态。</p>
     */
    public String getCode() {
        return code;
    }

    /**
     * 执行 setCode 方法。
     * 
     * <p>实现步骤：
     * 1. 接收并校验调用方传入的数据；
     * 2. 按当前方法职责执行业务查询、转换或持久化处理；
     * 3. 返回处理结果或更新对象状态。</p>
     */
    public void setCode(String code) {
        this.code = code;
    }

    /**
     * 执行 getName 方法。
     * 
     * <p>实现步骤：
     * 1. 接收并校验调用方传入的数据；
     * 2. 按当前方法职责执行业务查询、转换或持久化处理；
     * 3. 返回处理结果或更新对象状态。</p>
     */
    public String getName() {
        return name;
    }

    /**
     * 执行 setName 方法。
     * 
     * <p>实现步骤：
     * 1. 接收并校验调用方传入的数据；
     * 2. 按当前方法职责执行业务查询、转换或持久化处理；
     * 3. 返回处理结果或更新对象状态。</p>
     */
    public void setName(String name) {
        this.name = name;
    }

    /**
     * 执行 getDescription 方法。
     * 
     * <p>实现步骤：
     * 1. 接收并校验调用方传入的数据；
     * 2. 按当前方法职责执行业务查询、转换或持久化处理；
     * 3. 返回处理结果或更新对象状态。</p>
     */
    public String getDescription() {
        return description;
    }

    /**
     * 执行 setDescription 方法。
     * 
     * <p>实现步骤：
     * 1. 接收并校验调用方传入的数据；
     * 2. 按当前方法职责执行业务查询、转换或持久化处理；
     * 3. 返回处理结果或更新对象状态。</p>
     */
    public void setDescription(String description) {
        this.description = description;
    }

    /**
     * 执行 getPermissions 方法。
     * 
     * <p>实现步骤：
     * 1. 接收并校验调用方传入的数据；
     * 2. 按当前方法职责执行业务查询、转换或持久化处理；
     * 3. 返回处理结果或更新对象状态。</p>
     */
    public Set<PermissionCode> getPermissions() {
        return permissions;
    }

    /**
     * 执行 setPermissions 方法。
     * 
     * <p>实现步骤：
     * 1. 接收并校验调用方传入的数据；
     * 2. 按当前方法职责执行业务查询、转换或持久化处理；
     * 3. 返回处理结果或更新对象状态。</p>
     */
    public void setPermissions(Set<PermissionCode> permissions) {
        this.permissions = permissions;
    }

    /**
     * 执行 getMenus 方法。
     * 
     * <p>实现步骤：
     * 1. 接收并校验调用方传入的数据；
     * 2. 按当前方法职责执行业务查询、转换或持久化处理；
     * 3. 返回处理结果或更新对象状态。</p>
     */
    public Set<SystemMenu> getMenus() {
        return menus;
    }

    /**
     * 执行 setMenus 方法。
     * 
     * <p>实现步骤：
     * 1. 接收并校验调用方传入的数据；
     * 2. 按当前方法职责执行业务查询、转换或持久化处理；
     * 3. 返回处理结果或更新对象状态。</p>
     */
    public void setMenus(Set<SystemMenu> menus) {
        this.menus = menus;
    }
}
