package com.ratel.fm.domain.auth;

import com.ratel.fm.common.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import org.hibernate.annotations.Comment;

import java.time.OffsetDateTime;

/**
 * 用户常用菜单统计实体。
 *
 * <p>实现目的：按所属公司、用户和菜单编码累计进入次数，供首页快捷入口按个人使用习惯排序展示。</p>
 */
@Entity
@Table(
        name = "fm_user_menu_usages",
        uniqueConstraints = @UniqueConstraint(name = "uk_fm_user_menu_usage_user_menu", columnNames = {"organization_code", "user_id", "menu_code"})
)
@Comment("用户常用菜单统计表，记录每个用户进入功能菜单的次数")
public class UserMenuUsage extends BaseEntity {

    /** 所属公司字典编码，用于账套隔离。 */
    @Column(nullable = false, length = 80)
    @Comment("所属公司字典编码")
    private String organizationCode;

    /** 用户主键。 */
    @Column(nullable = false)
    @Comment("用户主键")
    private Long userId;

    /** 用户账号快照。 */
    @Column(nullable = false, length = 80)
    @Comment("用户账号快照")
    private String username;

    /** 菜单编码。 */
    @Column(nullable = false, length = 120)
    @Comment("菜单编码")
    private String menuCode;

    /** 菜单名称快照。 */
    @Column(nullable = false, length = 120)
    @Comment("菜单名称快照")
    private String menuName;

    /** 菜单路由快照。 */
    @Column(nullable = false, length = 200)
    @Comment("菜单路由快照")
    private String routePath;

    /** 进入次数。 */
    @Column(nullable = false)
    @Comment("进入次数")
    private long useCount;

    /** 最近进入时间。 */
    @Column(nullable = false)
    @Comment("最近进入时间")
    private OffsetDateTime lastUsedAt;

    public String getOrganizationCode() {
        return organizationCode;
    }

    public void setOrganizationCode(String organizationCode) {
        this.organizationCode = organizationCode;
    }

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getMenuCode() {
        return menuCode;
    }

    public void setMenuCode(String menuCode) {
        this.menuCode = menuCode;
    }

    public String getMenuName() {
        return menuName;
    }

    public void setMenuName(String menuName) {
        this.menuName = menuName;
    }

    public String getRoutePath() {
        return routePath;
    }

    public void setRoutePath(String routePath) {
        this.routePath = routePath;
    }

    public long getUseCount() {
        return useCount;
    }

    public void setUseCount(long useCount) {
        this.useCount = useCount;
    }

    public OffsetDateTime getLastUsedAt() {
        return lastUsedAt;
    }

    public void setLastUsedAt(OffsetDateTime lastUsedAt) {
        this.lastUsedAt = lastUsedAt;
    }
}
