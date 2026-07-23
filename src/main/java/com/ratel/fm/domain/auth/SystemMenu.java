package com.ratel.fm.domain.auth;

import com.ratel.fm.common.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import org.hibernate.annotations.Comment;

/**
 * 系统菜单资源。
 *
 * <p>菜单资源覆盖模块、页面和按钮。角色关联菜单后，前端根据菜单编码决定对应模块、页面和按钮是否显示。</p>
 *
 * <p>开发组织：ratel；开发人员：WenZhang；联系方式：18782945613。</p>
 *
 * @author ratel
 */
@Entity
@Table(name = "fm_menus")
@Comment("系统菜单资源表，保存模块、页面、按钮等可授权资源")
public class SystemMenu extends BaseEntity {

    /** 菜单编码，全系统唯一，前端用它控制显示与操作。 */
    @Column(nullable = false, unique = true, length = 120)
    @Comment("菜单编码，全系统唯一")
    private String code;

    /** 菜单名称，用于角色授权页面展示。 */
    @Column(nullable = false, length = 120)
    @Comment("菜单名称")
    private String name;

    /** 菜单类型：模块、页面或按钮。 */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    @Comment("菜单类型：模块、页面或按钮")
    private MenuType type;

    /** 父级菜单。模块为空，页面挂模块下，按钮挂页面下。 */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "parent_id")
    @Comment("父级菜单ID")
    private SystemMenu parent;

    /** 前端路由路径，页面菜单使用，按钮可为空。 */
    @Column(length = 160)
    @Comment("前端路由路径")
    private String routePath;

    /** 排序号，越小越靠前。 */
    @Column(nullable = false)
    @Comment("菜单排序号")
    private int sortOrder;

    /** 是否启用。停用后不参与授权和前端展示。 */
    @Column(nullable = false)
    @Comment("是否启用菜单")
    private boolean enabled = true;

    /** 绑定的后端权限码，按钮或页面需要后端方法鉴权时使用。 */
    @Enumerated(EnumType.STRING)
    @Column(length = 80)
    @Comment("绑定的后端权限码")
    private PermissionCode permissionCode;

    /**
     * 执行 getCode 方法。
     * 
     * <p>实现步骤：
     * 1. 接收并校验调用方传入的数据；
     * 2. 按当前方法职责执行业务查询、转换或持久化处理；
     * 3. 返回处理结果或更新对象状态。</p>
     */
    public String getCode() { return code; }
    /**
     * 执行 setCode 方法。
     * 
     * <p>实现步骤：
     * 1. 接收并校验调用方传入的数据；
     * 2. 按当前方法职责执行业务查询、转换或持久化处理；
     * 3. 返回处理结果或更新对象状态。</p>
     */
    public void setCode(String code) { this.code = code; }
    /**
     * 执行 getName 方法。
     * 
     * <p>实现步骤：
     * 1. 接收并校验调用方传入的数据；
     * 2. 按当前方法职责执行业务查询、转换或持久化处理；
     * 3. 返回处理结果或更新对象状态。</p>
     */
    public String getName() { return name; }
    /**
     * 执行 setName 方法。
     * 
     * <p>实现步骤：
     * 1. 接收并校验调用方传入的数据；
     * 2. 按当前方法职责执行业务查询、转换或持久化处理；
     * 3. 返回处理结果或更新对象状态。</p>
     */
    public void setName(String name) { this.name = name; }
    /**
     * 执行 getType 方法。
     * 
     * <p>实现步骤：
     * 1. 接收并校验调用方传入的数据；
     * 2. 按当前方法职责执行业务查询、转换或持久化处理；
     * 3. 返回处理结果或更新对象状态。</p>
     */
    public MenuType getType() { return type; }
    /**
     * 执行 setType 方法。
     * 
     * <p>实现步骤：
     * 1. 接收并校验调用方传入的数据；
     * 2. 按当前方法职责执行业务查询、转换或持久化处理；
     * 3. 返回处理结果或更新对象状态。</p>
     */
    public void setType(MenuType type) { this.type = type; }
    /**
     * 执行 getParent 方法。
     * 
     * <p>实现步骤：
     * 1. 接收并校验调用方传入的数据；
     * 2. 按当前方法职责执行业务查询、转换或持久化处理；
     * 3. 返回处理结果或更新对象状态。</p>
     */
    public SystemMenu getParent() { return parent; }
    /**
     * 执行 setParent 方法。
     * 
     * <p>实现步骤：
     * 1. 接收并校验调用方传入的数据；
     * 2. 按当前方法职责执行业务查询、转换或持久化处理；
     * 3. 返回处理结果或更新对象状态。</p>
     */
    public void setParent(SystemMenu parent) { this.parent = parent; }
    /**
     * 执行 getRoutePath 方法。
     * 
     * <p>实现步骤：
     * 1. 接收并校验调用方传入的数据；
     * 2. 按当前方法职责执行业务查询、转换或持久化处理；
     * 3. 返回处理结果或更新对象状态。</p>
     */
    public String getRoutePath() { return routePath; }
    /**
     * 执行 setRoutePath 方法。
     * 
     * <p>实现步骤：
     * 1. 接收并校验调用方传入的数据；
     * 2. 按当前方法职责执行业务查询、转换或持久化处理；
     * 3. 返回处理结果或更新对象状态。</p>
     */
    public void setRoutePath(String routePath) { this.routePath = routePath; }
    /**
     * 执行 getSortOrder 方法。
     * 
     * <p>实现步骤：
     * 1. 接收并校验调用方传入的数据；
     * 2. 按当前方法职责执行业务查询、转换或持久化处理；
     * 3. 返回处理结果或更新对象状态。</p>
     */
    public int getSortOrder() { return sortOrder; }
    /**
     * 执行 setSortOrder 方法。
     * 
     * <p>实现步骤：
     * 1. 接收并校验调用方传入的数据；
     * 2. 按当前方法职责执行业务查询、转换或持久化处理；
     * 3. 返回处理结果或更新对象状态。</p>
     */
    public void setSortOrder(int sortOrder) { this.sortOrder = sortOrder; }
    /**
     * 执行 isEnabled 方法。
     * 
     * <p>实现步骤：
     * 1. 接收并校验调用方传入的数据；
     * 2. 按当前方法职责执行业务查询、转换或持久化处理；
     * 3. 返回处理结果或更新对象状态。</p>
     */
    public boolean isEnabled() { return enabled; }
    /**
     * 执行 setEnabled 方法。
     * 
     * <p>实现步骤：
     * 1. 接收并校验调用方传入的数据；
     * 2. 按当前方法职责执行业务查询、转换或持久化处理；
     * 3. 返回处理结果或更新对象状态。</p>
     */
    public void setEnabled(boolean enabled) { this.enabled = enabled; }
    /**
     * 执行 getPermissionCode 方法。
     * 
     * <p>实现步骤：
     * 1. 接收并校验调用方传入的数据；
     * 2. 按当前方法职责执行业务查询、转换或持久化处理；
     * 3. 返回处理结果或更新对象状态。</p>
     */
    public PermissionCode getPermissionCode() { return permissionCode; }
    /**
     * 执行 setPermissionCode 方法。
     * 
     * <p>实现步骤：
     * 1. 接收并校验调用方传入的数据；
     * 2. 按当前方法职责执行业务查询、转换或持久化处理；
     * 3. 返回处理结果或更新对象状态。</p>
     */
    public void setPermissionCode(PermissionCode permissionCode) { this.permissionCode = permissionCode; }
}
