package com.ratel.fm.repository.receivable;

import com.ratel.fm.domain.receivable.ArApBill;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.List;
import java.util.Optional;

/**
 * 应收应付单数据访问接口。
 *
 * <p>用于应收应付单据创建、账龄分析和付款计划查询。</p>
 *
 * <p>开发组织：ratel；开发人员：WenZhang；联系方式：18782945613。</p>
 *
 * @author ratel
 */
public interface ArApBillRepository extends JpaRepository<ArApBill, Long>, JpaSpecificationExecutor<ArApBill> {

    /**
     * 判断应收应付单据编号是否已经存在。
     */
    boolean existsByBillNo(String billNo);

    /**
     * 判断当前所属公司内应收应付单据编号是否已经存在。
     */
    boolean existsByOrganizationCodeAndBillNo(String organizationCode, String billNo);

    /**
     * 查询当前所属公司内指定日期前缀下最大的应收应付单据编号。
     */
    Optional<ArApBill> findFirstByOrganizationCodeAndBillNoStartingWithOrderByBillNoDesc(String organizationCode, String billNoPrefix);

    /**
     * 查询最近 100 条应收应付单，按到期日期升序返回。
     */
    List<ArApBill> findTop100ByOrderByDueDateAsc();
}
