package com.ratel.fm.repository.inventory;

import com.ratel.fm.domain.inventory.InventoryLedger;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.List;
import java.util.Optional;

/**
 * 库存台账数据访问接口。
 *
 * <p>用于库存流水创建、库存查询和二期库存分析。</p>
 *
 * <p>开发组织：ratel；开发人员：WenZhang；联系方式：18782945613。</p>
 *
 * @author ratel
 */
public interface InventoryLedgerRepository extends JpaRepository<InventoryLedger, Long>, JpaSpecificationExecutor<InventoryLedger> {

    /**
     * 判断库存流水号是否已经存在。
     */
    boolean existsByMovementNo(String movementNo);

    /**
     * 判断当前所属公司内库存流水号是否已经存在。
     */
    boolean existsByOrganizationCodeAndMovementNo(String organizationCode, String movementNo);

    /**
     * 查询当前所属公司内指定日期前缀下最大的库存流水号。
     *
     * <p>实现步骤：按所属公司和 INVyyyyMMdd 前缀筛选，再按流水号倒序取第一条，用于生成下一号段。</p>
     */
    Optional<InventoryLedger> findFirstByOrganizationCodeAndMovementNoStartingWithOrderByMovementNoDesc(String organizationCode, String movementNoPrefix);

    /**
     * 查询最近 100 条库存流水，按库存变动日期倒序返回。
     */
    List<InventoryLedger> findTop100ByOrderByMovementDateDesc();
}
