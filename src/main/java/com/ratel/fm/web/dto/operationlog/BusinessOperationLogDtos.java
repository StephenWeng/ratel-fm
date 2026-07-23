package com.ratel.fm.web.dto.operationlog;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

/**
 * 业务操作流水 DTO。
 *
 * <p>开发组织：ratel；开发人员：WenZhang；联系方式：18782945613。</p>
 *
 * @author ratel
 */
public final class BusinessOperationLogDtos {

    private BusinessOperationLogDtos() {
    }

    @Schema(description = "业务操作流水视图。作者：ratel；开发人员：WenZhang；联系方式：18782945613。")
    /**
     * BusinessOperationLogView 数据传输记录。
     * 
     * <p>用于承载 BusinessOperationLogView 相关的业务数据、接口契约或处理逻辑，便于维护人员快速定位模块职责。</p>
     */
    public record BusinessOperationLogView(
            @Schema(description = "流水主键。")
            /**
             * 记录组件 id：表示接口入参或出参中的 id 字段。
             */
            Long id,
            @Schema(description = "业务类型。")
            /**
             * 记录组件 businessType：表示接口入参或出参中的 businessType 字段。
             */
            String businessType,
            @Schema(description = "业务记录主键 ID。")
            /**
             * 记录组件 businessId：表示接口入参或出参中的 businessId 字段。
             */
            Long businessId,
            @Schema(description = "业务编号快照。")
            /**
             * 记录组件 businessNo：表示接口入参或出参中的 businessNo 字段。
             */
            String businessNo,
            @Schema(description = "业务标题快照。")
            /**
             * 记录组件 businessTitle：表示接口入参或出参中的 businessTitle 字段。
             */
            String businessTitle,
            @Schema(description = "操作动作。")
            /**
             * 记录组件 action：表示接口入参或出参中的 action 字段。
             */
            String action,
            @Schema(description = "操作动作中文名称。")
            /**
             * 记录组件 actionName：表示接口入参或出参中的 actionName 字段。
             */
            String actionName,
            @Schema(description = "操作详情。")
            /**
             * 记录组件 detail：表示接口入参或出参中的 detail 字段。
             */
            String detail,
            @Schema(description = "操作前状态或关键值。")
            /**
             * 记录组件 fromState：表示接口入参或出参中的 fromState 字段。
             */
            String fromState,
            @Schema(description = "操作后状态或关键值。")
            /**
             * 记录组件 toState：表示接口入参或出参中的 toState 字段。
             */
            String toState,
            @Schema(description = "业务快照 JSON。")
            /**
             * 记录组件 snapshot：表示接口入参或出参中的 snapshot 字段。
             */
            String snapshot,
            @Schema(description = "操作人员主键。")
            /**
             * 记录组件 operatorId：表示接口入参或出参中的 operatorId 字段。
             */
            Long operatorId,
            @Schema(description = "操作人员账号。")
            /**
             * 记录组件 operatorUsername：表示接口入参或出参中的 operatorUsername 字段。
             */
            String operatorUsername,
            @Schema(description = "操作人员姓名。")
            /**
             * 记录组件 operatorName：表示接口入参或出参中的 operatorName 字段。
             */
            String operatorName,
            @Schema(description = "操作发生时间。")
            /**
             * 记录组件 operationTime：表示接口入参或出参中的 operationTime 字段。
             */
            String operationTime
    ) {
    }

    @Schema(description = "业务操作流水分页结果。作者：ratel；开发人员：WenZhang；联系方式：18782945613。")
    /**
     * BusinessOperationLogPage 数据传输记录。
     * 
     * <p>用于承载 BusinessOperationLogPage 相关的业务数据、接口契约或处理逻辑，便于维护人员快速定位模块职责。</p>
     */
    public record BusinessOperationLogPage(
            @Schema(description = "当前页业务流水。")
            /**
             * 记录组件 rows：表示接口入参或出参中的 rows 字段。
             */
            List<BusinessOperationLogView> rows,
            @Schema(description = "符合查询条件的流水总数。")
            /**
             * 记录组件 total：表示接口入参或出参中的 total 字段。
             */
            long total
    ) {
    }
}
