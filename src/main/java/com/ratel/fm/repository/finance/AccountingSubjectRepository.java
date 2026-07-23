package com.ratel.fm.repository.finance;

import com.ratel.fm.domain.finance.AccountingSubject;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.List;
import java.util.Optional;

/**
 * 会计科目数据访问接口。
 *
 * <p>用于科目字典维护、凭证录入时加载科目、试算平衡和智能检索。</p>
 *
 * <p>开发组织：ratel；开发人员：WenZhang；联系方式：18782945613。</p>
 *
 * @author ratel
 */
public interface AccountingSubjectRepository extends JpaRepository<AccountingSubject, Long>, JpaSpecificationExecutor<AccountingSubject> {

    /**
     * 根据所属公司和科目编码查询科目。
     */
    Optional<AccountingSubject> findByOrganizationCodeAndCode(String organizationCode, String code);

    /**
     * 判断科目编码在指定所属公司内是否已经存在。
     */
    boolean existsByOrganizationCodeAndCode(String organizationCode, String code);

    /**
     * 查询所有启用科目，并按科目编码升序返回。
     */
    List<AccountingSubject> findByOrganizationCodeAndEnabledTrueOrderByCodeAsc(String organizationCode);

    /**
     * 查询指定父级下启用的直接下级科目，用于停用父级前进行二次确认校验。
     */
    List<AccountingSubject> findByParentIdAndEnabledTrueOrderByCodeAsc(Long parentId);

    /**
     * 兼容初始化默认科目时的全局编码查询。
     *
     * <p>实现步骤：仅用于启动种子数据修正，业务查询必须使用所属公司维度方法。</p>
     */
    Optional<AccountingSubject> findByCode(String code);
}
