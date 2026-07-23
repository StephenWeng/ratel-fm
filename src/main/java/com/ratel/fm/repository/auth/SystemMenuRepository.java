package com.ratel.fm.repository.auth;

import com.ratel.fm.domain.auth.SystemMenu;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.List;
import java.util.Optional;

/**
 * 系统菜单资源数据访问接口。
 *
 * <p>用于菜单初始化、角色菜单授权和当前登录人可见菜单计算。</p>
 *
 * <p>开发组织：ratel；开发人员：WenZhang；联系方式：18782945613。</p>
 *
 * @author ratel
 */
public interface SystemMenuRepository extends JpaRepository<SystemMenu, Long>, JpaSpecificationExecutor<SystemMenu> {

    /**
     * 根据菜单编码查询菜单资源。
     */
    @EntityGraph(attributePaths = "parent")
    Optional<SystemMenu> findByCode(String code);

    /**
     * 查询所有启用菜单，并加载父级菜单用于前端构造授权树。
     */
    @EntityGraph(attributePaths = "parent")
    List<SystemMenu> findByEnabledTrueOrderBySortOrderAscIdAsc();

    /**
     * 查询全部菜单资源，并加载父级菜单，用于菜单管理页面维护模块、页面、按钮层级。
     */
    @EntityGraph(attributePaths = "parent")
    List<SystemMenu> findAllByOrderBySortOrderAscIdAsc();

    /**
     * 判断指定父级下是否存在子菜单，删除菜单前用于保护模块、页面、按钮层级完整性。
     */
    boolean existsByParentId(Long parentId);
}
