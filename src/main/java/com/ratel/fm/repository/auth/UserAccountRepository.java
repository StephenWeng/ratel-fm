package com.ratel.fm.repository.auth;

import com.ratel.fm.domain.auth.UserAccount;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.List;
import java.util.Optional;

/**
 * 人员账号数据访问接口。
 *
 * <p>主要用于登录校验、人员唯一性校验、人员管理增删改查和 JWT 人员信息比对。登录账号和身份证号均按
 * 所属公司维度唯一，支持不同公司使用相同人员账号或身份证号。</p>
 *
 * <p>开发组织：ratel；开发人员：WenZhang；联系方式：18782945613。</p>
 *
 * @author ratel
 */
public interface UserAccountRepository extends JpaRepository<UserAccount, Long>, JpaSpecificationExecutor<UserAccount> {

    /**
     * 根据所属公司和登录账号查询人员账号。
     *
     * <p>实现步骤：登录请求已经选择所属公司，因此账号只在该公司内匹配。</p>
     */
    Optional<UserAccount> findByOrganizationCodeAndUsername(String organizationCode, String username);

    /**
     * 根据所属公司和身份证号查询人员账号。
     *
     * <p>实现步骤：同一身份证号在不同公司可重复，只在当前登录公司内匹配。</p>
     */
    Optional<UserAccount> findByOrganizationCodeAndIdentityNo(String organizationCode, String identityNo);

    /**
     * 根据所属公司和人员 ID 查询启用人员。
     *
     * <p>实现步骤：流程指定人员审批时使用该查询解析悬浮提示中的审批人联系方式。</p>
     */
    Optional<UserAccount> findByOrganizationCodeAndIdAndEnabledTrue(String organizationCode, Long id);

    /**
     * 根据所属公司和登录账号查询启用人员。
     *
     * <p>实现步骤：兼容流程节点仅保存审批账号、未保存人员 ID 的历史或手工配置场景。</p>
     */
    Optional<UserAccount> findByOrganizationCodeAndUsernameAndEnabledTrue(String organizationCode, String username);

    /**
     * 根据所属公司、部门和启用状态查询人员。
     *
     * <p>实现步骤：流程部门审批节点使用该查询列出当前部门下可审批人员。</p>
     */
    List<UserAccount> findByOrganizationCodeAndDepartmentAndEnabledTrueOrderByRealNameAscIdAsc(String organizationCode, String department);

    /**
     * 根据所属公司、部门、岗位和启用状态查询人员。
     *
     * <p>实现步骤：流程部门岗位审批节点使用该查询列出当前部门岗位组合下可审批人员。</p>
     */
    List<UserAccount> findByOrganizationCodeAndDepartmentAndPositionAndEnabledTrueOrderByRealNameAscIdAsc(String organizationCode, String department, String position);

    /**
     * 判断登录账号在指定所属公司内是否已经存在。
     */
    boolean existsByOrganizationCodeAndUsername(String organizationCode, String username);

    /**
     * 判断身份证号在指定所属公司内是否已经存在。
     */
    boolean existsByOrganizationCodeAndIdentityNo(String organizationCode, String identityNo);

    /**
     * 兼容初始化和 JWT ID 精确校验的全局账号查询。
     *
     * <p>实现步骤：仅用于默认管理员种子数据定位，不参与普通登录唯一性判断。</p>
     */
    Optional<UserAccount> findByUsername(String username);

    /**
     * 兼容初始化和历史默认管理员定位的全局身份证查询。
     *
     * <p>实现步骤：仅用于默认管理员种子数据定位，不参与普通登录唯一性判断。</p>
     */
    Optional<UserAccount> findByIdentityNo(String identityNo);
}
