package com.ratel.fm.repository.auth;

import com.ratel.fm.domain.auth.Role;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

/**
 * 角色数据访问接口。
 *
 * <p>用于角色维护、模块授权保存和人员角色分配时按角色编码加载角色。</p>
 *
 * <p>开发组织：ratel；开发人员：WenZhang；联系方式：18782945613。</p>
 *
 * @author ratel
 */
public interface RoleRepository extends JpaRepository<Role, Long> {

    /**
     * 根据角色编码查询角色。
     */
    Optional<Role> findByCode(String code);

    /**
     * 判断角色编码是否已经存在。
     */
    boolean existsByCode(String code);
}
