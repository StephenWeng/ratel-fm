package com.ratel.fm.repository.receivable;

import com.ratel.fm.domain.receivable.ArApSettlement;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.List;

/**
 * 应收应付收付核销流水数据访问接口。
 *
 * <p>实现目的：按应收应付单读取核销历史，并支持后续收付明细审计和制证追溯。</p>
 */
public interface ArApSettlementRepository extends JpaRepository<ArApSettlement, Long>, JpaSpecificationExecutor<ArApSettlement> {

    /** 按账套和应收应付单主键查询核销流水。 */
    List<ArApSettlement> findByOrganizationCodeAndBillIdOrderBySettlementDateDescIdDesc(String organizationCode, Long billId);
}
