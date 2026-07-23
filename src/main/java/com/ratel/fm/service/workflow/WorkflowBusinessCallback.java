package com.ratel.fm.service.workflow;

import com.ratel.fm.domain.workflow.WorkflowStatus;
import com.ratel.fm.domain.workflow.WorkflowInstance;
import com.ratel.fm.web.dto.workflow.WorkflowDtos.WorkflowBusinessFormView;

/**
 * 流程业务回调接口。
 *
 * <p>实现目的：流程引擎只负责审批实例和任务流转，采购等业务模块通过该接口接收流程完成结果，
 * 避免流程服务直接依赖所有业务状态机。</p>
 */
public interface WorkflowBusinessCallback {

    /** 返回该回调支持的业务类型，例如 PURCHASE_ORDER。 */
    String businessType();

    /**
     * 流程完成后回调业务模块。
     *
     * <p>实现步骤：
     * 1. 判断流程最终状态为同意或不同意；
     * 2. 根据业务类型更新对应单据状态；
     * 3. 记录业务操作流水。</p>
     */
    void onWorkflowCompleted(WorkflowInstance instance, WorkflowStatus status, String comment);

    /**
     * 构建流程详情中的业务表单预览。
     *
     * <p>实现步骤：业务模块按自身单据结构返回通用字段分组和明细表格；未接入的业务模块返回 null，
     * 前端以空态展示，后续新增流程只需在对应回调中覆写该方法。</p>
     */
    default WorkflowBusinessFormView businessForm(WorkflowInstance instance) {
        return null;
    }
}
