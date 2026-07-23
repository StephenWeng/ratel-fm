package com.ratel.fm.repository.operation;

import com.ratel.fm.domain.operation.BusinessOperationLog;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.List;

/**
 * 统一业务操作流水仓储。
 *
 * <p>开发组织：ratel；开发人员：WenZhang；联系方式：18782945613。</p>
 *
 * @author ratel
 */
public interface BusinessOperationLogRepository extends JpaRepository<BusinessOperationLog, Long>, JpaSpecificationExecutor<BusinessOperationLog> {

    /**
     * 按业务类型和业务 ID 查询时间轴。
     */
    List<BusinessOperationLog> findByOrganizationCodeAndBusinessTypeAndBusinessIdOrderByOperationTimeAscIdAsc(
            String organizationCode,
            String businessType,
            Long businessId
    );

    /**
     * 按业务类型、业务 ID 和时间范围分页查询操作流水。
     */
    Page<BusinessOperationLog> findByOrganizationCodeAndBusinessTypeAndBusinessIdAndOperationTimeBetween(
            String organizationCode,
            String businessType,
            Long businessId,
            java.time.OffsetDateTime startTime,
            java.time.OffsetDateTime endTime,
            Pageable pageable
    );

    /**
     * 删除业务记录关联的全部流水。
     */
    void deleteByBusinessTypeAndBusinessId(String businessType, Long businessId);
}
