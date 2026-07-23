package com.ratel.fm.repository.finance;

import com.ratel.fm.domain.finance.AccountingSourceType;
import com.ratel.fm.domain.finance.Voucher;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

/**
 * 财务凭证数据访问接口。
 *
 * <p>用于凭证新增、查询、过账、作废、报表汇总和智能检索。</p>
 *
 * <p>开发组织：ratel；开发人员：WenZhang；联系方式：18782945613。</p>
 *
 * @author ratel
 */
public interface VoucherRepository extends JpaRepository<Voucher, Long>, JpaSpecificationExecutor<Voucher> {

    /**
     * 判断凭证编号是否已经存在。
     */
    boolean existsByVoucherNo(String voucherNo);

    /**
     * 判断当前所属公司内凭证编号是否已经存在。
     */
    boolean existsByOrganizationCodeAndVoucherNo(String organizationCode, String voucherNo);

    /**
     * 判断指定来源单号是否已经存在未作废凭证。
     *
     * <p>实现步骤：按来源业务单号和凭证状态查询，自动凭证生成前据此拦截重复制证。</p>
     */
    boolean existsBySourceBizNoAndStatusNot(String sourceBizNo, com.ratel.fm.domain.finance.VoucherStatus status);

    /**
     * 判断当前所属公司内指定来源单号是否已经存在未作废凭证。
     *
     * <p>实现步骤：会计平台自动制证时按公司维度去重，避免其他公司相同来源单号误拦截。</p>
     */
    boolean existsByOrganizationCodeAndSourceBizNoAndStatusNot(
            String organizationCode,
            String sourceBizNo,
            com.ratel.fm.domain.finance.VoucherStatus status
    );

    /**
     * 判断当前所属公司内指定来源类型和来源主键是否已经存在未作废凭证。
     *
     * <p>实现步骤：会计平台自动制证优先按来源类型和来源主键去重，避免不同模块相同单号互相影响。</p>
     */
    boolean existsByOrganizationCodeAndSourceTypeAndSourceIdAndStatusNot(
            String organizationCode,
            AccountingSourceType sourceType,
            Long sourceId,
            com.ratel.fm.domain.finance.VoucherStatus status
    );

    /**
     * 按主键查询凭证，并同时加载分录和分录科目。
     *
     * <p>实现目的：避免服务层访问凭证明细时触发懒加载异常或 N+1 查询。</p>
     */
    @EntityGraph(attributePaths = {"lines", "lines.subject"})
    Optional<Voucher> findWithLinesById(Long id);

    /**
     * 按所属公司和主键查询凭证，并同时加载分录和分录科目。
     */
    @EntityGraph(attributePaths = {"lines", "lines.subject"})
    Optional<Voucher> findWithLinesByOrganizationCodeAndId(String organizationCode, Long id);

    /**
     * 按凭证日期区间查询凭证，并同时加载分录和分录科目。
     */
    @EntityGraph(attributePaths = {"lines", "lines.subject"})
    List<Voucher> findByVoucherDateBetweenOrderByVoucherDateDesc(LocalDate startDate, LocalDate endDate);

    /**
     * 按所属公司和凭证日期区间查询凭证，并同时加载分录和分录科目。
     */
    @EntityGraph(attributePaths = {"lines", "lines.subject"})
    List<Voucher> findByOrganizationCodeAndVoucherDateBetweenOrderByVoucherDateDesc(String organizationCode, LocalDate startDate, LocalDate endDate);
}
