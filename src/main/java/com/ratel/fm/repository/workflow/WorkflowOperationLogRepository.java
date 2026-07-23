package com.ratel.fm.repository.workflow;

import com.ratel.fm.domain.workflow.WorkflowOperationLog;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

/**
 * 流程操作流水数据访问接口。
 *
 * <p>实现目的：按流程实例时间顺序读取发起和审批轨迹。</p>
 */
public interface WorkflowOperationLogRepository extends JpaRepository<WorkflowOperationLog, Long> {

    /** 查询指定流程实例全部操作流水。 */
    List<WorkflowOperationLog> findByInstance_IdOrderByOperationTimeAscIdAsc(Long instanceId);
}
