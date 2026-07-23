package com.ratel.fm.web.dto.audit;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.OffsetDateTime;
import java.util.List;

/**
 * 操作日志管理接口 DTO。
 *
 * <p>开发组织：ratel；开发人员：WenZhang；联系方式：18782945613。</p>
 *
 * @author ratel
 */
public final class AuditDtos {

    private AuditDtos() {
    }

    /**
     * 操作日志分页查询结果。
     *
     * @param rows 当前页日志记录
     * @param total 符合条件的总记录数
     */
    @Schema(description = "操作日志分页查询结果")
    public record OperationLogPage(
            @Schema(description = "当前页日志记录") List<OperationLogView> rows,
            @Schema(description = "符合条件的总记录数") long total
    ) {
    }

    /**
     * 操作日志列表展示对象。
     *
     * @param id 日志主键
     * @param operatorUsername 操作账号
     * @param operatorName 操作人姓名
     * @param identityNo 操作人身份证号
     * @param contactPhone 操作人联系方式
     * @param department 操作人部门
     * @param operationTime 操作发生时间
     * @param terminalType 操作终端类型
     * @param terminalIdentifier 操作终端标识
     * @param operationModule 操作模块
     * @param operationFunction 操作功能
     * @param action 操作动作编码
     * @param operationParameters 操作参数
     * @param success 操作是否成功
     * @param operationResult 操作结果
     * @param responseValue 操作响应值
     * @param impact 操作影响说明
     */
    @Schema(description = "操作日志列表展示对象")
    public record OperationLogView(
            @Schema(description = "日志主键") Long id,
            @Schema(description = "操作账号") String operatorUsername,
            @Schema(description = "操作人姓名") String operatorName,
            @Schema(description = "操作人身份证号") String identityNo,
            @Schema(description = "操作人联系方式") String contactPhone,
            @Schema(description = "操作人部门") String department,
            @Schema(description = "操作发生时间") OffsetDateTime operationTime,
            @Schema(description = "操作终端类型") String terminalType,
            @Schema(description = "操作终端标识") String terminalIdentifier,
            @Schema(description = "操作模块") String operationModule,
            @Schema(description = "操作功能") String operationFunction,
            @Schema(description = "操作动作编码") String action,
            @Schema(description = "操作参数") String operationParameters,
            @Schema(description = "操作是否成功") Boolean success,
            @Schema(description = "操作结果") String operationResult,
            @Schema(description = "操作响应值") String responseValue,
            @Schema(description = "操作影响说明") String impact
    ) {
    }
}
