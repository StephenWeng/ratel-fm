package com.ratel.fm.repository.workflow;

import com.ratel.fm.domain.workflow.WorkflowInstance;
import com.ratel.fm.domain.workflow.WorkflowStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.List;
import java.util.Optional;

/**
 * 流程实例数据访问接口。
 *
 * <p>实现目的：为发起事宜、业务单据当前流程状态和流程查看弹窗提供流程实例查询能力。</p>
 */
public interface WorkflowInstanceRepository extends JpaRepository<WorkflowInstance, Long>, JpaSpecificationExecutor<WorkflowInstance> {

    /** 按所属公司和流程实例 ID 查询实例。 */
    Optional<WorkflowInstance> findByOrganizationCodeAndId(String organizationCode, Long id);

    /** 查询指定业务单据正在运行的流程，防止重复提交审批。 */
    Optional<WorkflowInstance> findFirstByOrganizationCodeAndBusinessTypeAndBusinessIdAndStatusOrderByIdDesc(
            String organizationCode,
            String businessType,
            Long businessId,
            WorkflowStatus status
    );

    /** 查询指定业务单据最近流程实例，供采购列表回显审批状态使用。 */
    Optional<WorkflowInstance> findFirstByOrganizationCodeAndBusinessTypeAndBusinessIdOrderByIdDesc(
            String organizationCode,
            String businessType,
            Long businessId
    );

    /** 查询当前用户发起的流程实例。 */
    List<WorkflowInstance> findByOrganizationCodeAndStarterIdOrderByStartedTimeDescIdDesc(String organizationCode, Long starterId);
}
