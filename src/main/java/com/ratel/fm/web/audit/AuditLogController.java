package com.ratel.fm.web.audit;

import com.github.xiaoymin.knife4j.annotations.ApiOperationSupport;
import com.github.xiaoymin.knife4j.annotations.ApiSupport;
import com.ratel.fm.common.ApiResponse;
import com.ratel.fm.service.audit.AuditLogService;
import com.ratel.fm.web.dto.audit.AuditDtos.OperationLogPage;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.OffsetDateTime;

/**
 * 操作日志管理接口。
 *
 * <p>开发组织：ratel；开发人员：WenZhang；联系方式：18782945613。</p>
 *
 * @author ratel
 */
@Tag(name = "日志管理")
@ApiSupport(order = 70, author = "ratel / WenZhang / 18782945613")
@RestController
@RequestMapping("/api/audit")
public class AuditLogController {

    /**
     * 字段 auditLogService：保存 auditLogService 对应的业务数据、运行配置或依赖对象，供本类逻辑读取和维护。
     */
    private final AuditLogService auditLogService;

    /**
     * 构造 AuditLogController 实例。
     * 
     * <p>实现步骤：
     * 1. 接收调用方传入的依赖对象或初始化参数；
     * 2. 保存到成员字段，保证后续业务方法可以复用；
     * 3. 完成实例初始化。</p>
     */
    public AuditLogController(AuditLogService auditLogService) {
        this.auditLogService = auditLogService;
    }

    /**
     * 查询业务系统操作日志。
     *
     * <p>实现步骤：
     * 1. 接收前端提交的操作时间范围、账号、身份证、联系方式、部门和终端条件；
     * 2. 交由审计服务按动态条件分页查询；
     * 3. 返回分页列表和总数，供日志管理页面展示。</p>
     */
    @ApiOperationSupport(order = 10, author = "ratel / WenZhang / 18782945613")
    @Operation(summary = "查询业务系统操作日志", description = "作者：ratel；开发人员：WenZhang；联系方式：18782945613。支持按操作时间范围、账号、身份证、联系方式、部门、终端类型和终端标识查询。")
    @PreAuthorize("hasAuthority('AUDIT_LOG_VIEW')")
    @GetMapping("/operation-logs")
    public ApiResponse<OperationLogPage> searchOperationLogs(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) OffsetDateTime startTime,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) OffsetDateTime endTime,
            @RequestParam(required = false) String account,
            @RequestParam(required = false) String identityNo,
            @RequestParam(required = false) String contactPhone,
            @RequestParam(required = false) String department,
            @RequestParam(required = false) String terminalType,
            @RequestParam(required = false) String terminalIdentifier,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        OperationLogPage result = auditLogService.searchLogs(
                startTime,
                endTime,
                account,
                identityNo,
                contactPhone,
                department,
                terminalType,
                terminalIdentifier,
                page,
                size
        );
        return ApiResponse.ok(result);
    }
}
