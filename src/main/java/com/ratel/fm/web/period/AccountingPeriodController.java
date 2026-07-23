package com.ratel.fm.web.period;

import com.github.xiaoymin.knife4j.annotations.ApiOperationSupport;
import com.github.xiaoymin.knife4j.annotations.ApiSupport;
import com.ratel.fm.common.ApiResponse;
import com.ratel.fm.domain.period.AccountingPeriodStatus;
import com.ratel.fm.service.period.AccountingPeriodService;
import com.ratel.fm.web.dto.period.AccountingPeriodDtos.AccountingPeriodActionRequest;
import com.ratel.fm.web.dto.period.AccountingPeriodDtos.AccountingPeriodRequest;
import com.ratel.fm.web.dto.period.AccountingPeriodDtos.AccountingPeriodView;
import com.ratel.fm.web.dto.period.AccountingPeriodDtos.PeriodCloseCheckView;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 会计期间控制器。
 *
 * <p>实现目的：对外提供会计期间查询、创建、月结检查、结账和反结账接口。</p>
 */
@Tag(name = "会计期间")
@ApiSupport(order = 35, author = "ratel / WenZhang / 18782945613")
@RestController
@RequestMapping("/api/accounting-periods")
public class AccountingPeriodController {

    /** 会计期间服务，承接期间业务规则。 */
    private final AccountingPeriodService service;

    /**
     * 构造会计期间控制器。
     *
     * <p>实现步骤：接收会计期间服务并保存到成员字段，所有接口统一委托服务处理。</p>
     */
    public AccountingPeriodController(AccountingPeriodService service) {
        this.service = service;
    }

    /**
     * 查询会计期间列表。
     *
     * <p>实现步骤：接收筛选条件，调用服务层按当前账套返回期间列表。</p>
     */
    @ApiOperationSupport(order = 10)
    @Operation(summary = "会计期间列表")
    @PreAuthorize("hasAuthority('FINANCE_VOUCHER_MANAGE') or hasAuthority('REPORT_VIEW')")
    @GetMapping
    public ApiResponse<List<AccountingPeriodView>> list(
            @RequestParam(required = false) String periodCode,
            @RequestParam(required = false) AccountingPeriodStatus status
    ) {
        return ApiResponse.ok(service.list(periodCode, status));
    }

    /**
     * 创建会计期间。
     *
     * <p>实现步骤：校验请求体后创建当前账套对应期间，已存在时返回已有期间。</p>
     */
    @ApiOperationSupport(order = 20)
    @Operation(summary = "创建会计期间")
    @PreAuthorize("hasAuthority('FINANCE_VOUCHER_MANAGE')")
    @PostMapping
    /**
     * 接收创建会计期间请求。
     *
     * <p>实现步骤：
     * 1. 校验期间编码和备注；
     * 2. 调用服务层创建当前账套期间；
     * 3. 返回已创建或已存在的期间视图。</p>
     */
    public ApiResponse<AccountingPeriodView> create(@Valid @RequestBody AccountingPeriodRequest request) {
        return ApiResponse.ok("会计期间已创建", service.create(request));
    }

    /**
     * 月结前检查。
     *
     * <p>实现步骤：按期间编码检查草稿凭证和未结往来，返回阻断项和提示项。</p>
     */
    @ApiOperationSupport(order = 30)
    @Operation(summary = "月结前检查")
    @PreAuthorize("hasAuthority('FINANCE_VOUCHER_MANAGE') or hasAuthority('REPORT_VIEW')")
    @GetMapping("/{periodCode}/close-check")
    /**
     * 接收月结前检查请求。
     *
     * <p>实现步骤：
     * 1. 从路径读取会计期间编码；
     * 2. 调用服务层检查草稿凭证和未结往来；
     * 3. 返回阻断项和风险提示项。</p>
     */
    public ApiResponse<PeriodCloseCheckView> closeCheck(@PathVariable String periodCode) {
        return ApiResponse.ok(service.closeCheck(periodCode));
    }

    /**
     * 关闭会计期间。
     *
     * <p>实现步骤：调用服务层执行月结检查和关闭动作，存在阻断项时返回业务错误。</p>
     */
    @ApiOperationSupport(order = 40)
    @Operation(summary = "关闭会计期间")
    @PreAuthorize("hasAuthority('FINANCE_VOUCHER_MANAGE')")
    @PostMapping("/{periodCode}/close")
    public ApiResponse<AccountingPeriodView> close(
            @PathVariable String periodCode,
            @RequestBody(required = false) AccountingPeriodActionRequest request
    ) {
        return ApiResponse.ok("会计期间已关闭", service.close(periodCode, request));
    }

    /**
     * 反结账并打开会计期间。
     *
     * <p>实现步骤：调用服务层将期间状态恢复为开启，便于开发期重新调整业务数据。</p>
     */
    @ApiOperationSupport(order = 50)
    @Operation(summary = "反结账")
    @PreAuthorize("hasAuthority('FINANCE_VOUCHER_MANAGE')")
    @PostMapping("/{periodCode}/reopen")
    public ApiResponse<AccountingPeriodView> reopen(
            @PathVariable String periodCode,
            @RequestBody(required = false) AccountingPeriodActionRequest request
    ) {
        return ApiResponse.ok("会计期间已重新打开", service.reopen(periodCode, request));
    }
}
