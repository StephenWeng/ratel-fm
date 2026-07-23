package com.ratel.fm.repository.auth;

import com.ratel.fm.domain.auth.UserMenuUsage;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

/**
 * 用户常用菜单统计数据访问接口。
 */
public interface UserMenuUsageRepository extends JpaRepository<UserMenuUsage, Long> {

    /**
     * 定位当前用户某个菜单的累计记录。
     */
    Optional<UserMenuUsage> findByOrganizationCodeAndUserIdAndMenuCode(String organizationCode, Long userId, String menuCode);

    /**
     * 按使用次数和最近使用时间读取当前用户常用菜单。
     */
    List<UserMenuUsage> findByOrganizationCodeAndUserIdOrderByUseCountDescLastUsedAtDescIdAsc(String organizationCode, Long userId, Pageable pageable);
}
