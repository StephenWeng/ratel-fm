package com.ratel.fm.repository.purchase;

import com.ratel.fm.domain.purchase.PurchaseOrder;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.List;
import java.util.Optional;

/**
 * 采购订单数据访问接口。
 *
 * <p>用于采购单维护、状态流转、经营概览和智能检索。</p>
 *
 * <p>开发组织：ratel；开发人员：WenZhang；联系方式：18782945613。</p>
 *
 * @author ratel
 */
public interface PurchaseOrderRepository extends JpaRepository<PurchaseOrder, Long>, JpaSpecificationExecutor<PurchaseOrder> {

    /**
     * 判断采购单号是否已经存在。
     */
    boolean existsByOrderNo(String orderNo);

    /**
     * 判断当前所属公司内采购单号是否已经存在。
     */
    boolean existsByOrganizationCodeAndOrderNo(String organizationCode, String orderNo);

    /**
     * 查询当前所属公司内指定日期前缀下最大的采购单号。
     */
    Optional<PurchaseOrder> findFirstByOrganizationCodeAndOrderNoStartingWithOrderByOrderNoDesc(String organizationCode, String orderNoPrefix);

    /**
     * 按主键查询采购单，并同时加载采购明细。
     */
    @EntityGraph(attributePaths = "lines")
    Optional<PurchaseOrder> findWithLinesById(Long id);

    /**
     * 按所属公司和主键查询采购单，并同时加载采购明细。
     */
    @EntityGraph(attributePaths = "lines")
    Optional<PurchaseOrder> findWithLinesByOrganizationCodeAndId(String organizationCode, Long id);

    /**
     * 查询最近 50 条采购单，并同时加载采购明细。
     */
    @EntityGraph(attributePaths = "lines")
    List<PurchaseOrder> findTop50ByOrderByOrderDateDesc();

    /**
     * 查询当前所属公司最近 50 条采购单，并同时加载采购明细。
     */
    @EntityGraph(attributePaths = "lines")
    List<PurchaseOrder> findTop50ByOrganizationCodeOrderByOrderDateDesc(String organizationCode);
}
