package com.ratel.fm.repository.workflow;

import com.ratel.fm.domain.workflow.WorkflowTask;
import com.ratel.fm.domain.workflow.WorkflowTaskStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.List;
import java.util.Optional;

/**
 * 流程任务数据访问接口。
 *
 * <p>实现目的：支持待办、已办列表查询和按流程实例查找当前待审批节点。</p>
 */
public interface WorkflowTaskRepository extends JpaRepository<WorkflowTask, Long>, JpaSpecificationExecutor<WorkflowTask> {

    /** 按实例和状态查询任务，通常用于查找当前待办节点。 */
    List<WorkflowTask> findByInstance_IdAndStatusOrderByNodeOrderAscIdAsc(Long instanceId, WorkflowTaskStatus status);

    /** 按实例查询全部任务，供流程图和流程查看弹窗展示。 */
    List<WorkflowTask> findByInstance_IdOrderByNodeOrderAscIdAsc(Long instanceId);

    /** 按所属公司和任务 ID 查询任务，防止跨账套审批。 */
    Optional<WorkflowTask> findByOrganizationCodeAndId(String organizationCode, Long id);
}
