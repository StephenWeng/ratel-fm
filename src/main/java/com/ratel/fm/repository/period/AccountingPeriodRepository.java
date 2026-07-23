package com.ratel.fm.repository.period;

import com.ratel.fm.domain.period.AccountingPeriod;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.Optional;

/**
 * 会计期间数据访问接口。
 *
 * <p>实现目的：为会计期间列表、关闭检查、结账和反结账提供账套内查询能力。</p>
 */
public interface AccountingPeriodRepository extends JpaRepository<AccountingPeriod, Long>, JpaSpecificationExecutor<AccountingPeriod> {

    /** 判断当前所属公司内期间编码是否已经存在。 */
    boolean existsByOrganizationCodeAndPeriodCode(String organizationCode, String periodCode);

    /** 按所属公司和期间编码查询单个会计期间。 */
    Optional<AccountingPeriod> findByOrganizationCodeAndPeriodCode(String organizationCode, String periodCode);

    /** 查询指定所属公司最新的会计期间，用于启动或定时任务从最新期间之后补齐缺失月份。 */
    Optional<AccountingPeriod> findFirstByOrganizationCodeOrderByPeriodCodeDesc(String organizationCode);
}
