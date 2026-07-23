package com.ratel.fm.repository.cashier;

import com.ratel.fm.domain.cashier.CashierTransaction;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.List;
import java.util.Optional;

/**
 * 出纳资金流水数据访问接口。
 *
 * <p>实现目的：支持出纳流水新增、确认、取消、查询和会计平台制证来源读取。</p>
 */
public interface CashierTransactionRepository extends JpaRepository<CashierTransaction, Long>, JpaSpecificationExecutor<CashierTransaction> {

    /** 判断当前所属公司内出纳流水号是否已经存在。 */
    boolean existsByOrganizationCodeAndTransactionNo(String organizationCode, String transactionNo);

    /** 查询当前所属公司内指定日期前缀下最大的出纳流水号。 */
    Optional<CashierTransaction> findFirstByOrganizationCodeAndTransactionNoStartingWithOrderByTransactionNoDesc(
            String organizationCode,
            String transactionNoPrefix
    );

    /** 按所属公司和主键读取出纳流水。 */
    Optional<CashierTransaction> findByOrganizationCodeAndId(String organizationCode, Long id);

    /** 查询最近 50 条出纳流水，供会计平台默认展示。 */
    List<CashierTransaction> findTop50ByOrganizationCodeOrderByTransactionDateDesc(String organizationCode);
}
