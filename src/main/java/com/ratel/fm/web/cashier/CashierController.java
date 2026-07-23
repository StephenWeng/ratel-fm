package com.ratel.fm.web.cashier;

import com.github.xiaoymin.knife4j.annotations.ApiOperationSupport;
import com.github.xiaoymin.knife4j.annotations.ApiSupport;
import com.ratel.fm.common.ApiResponse;
import com.ratel.fm.domain.cashier.CashierTransactionStatus;
import com.ratel.fm.domain.cashier.CashierTransactionType;
import com.ratel.fm.service.cashier.CashierService;
import com.ratel.fm.web.dto.cashier.CashierDtos.CashierExportRequest;
import com.ratel.fm.web.dto.cashier.CashierDtos.CashierTransactionRequest;
import com.ratel.fm.web.dto.cashier.CashierDtos.CashierTransactionView;
import com.ratel.fm.web.dto.common.BatchIdsRequest;
import com.ratel.fm.web.export.ExcelDownload;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

/**
 * 出纳管理控制器。
 *
 * <p>实现目的：对外提供出纳流水查询、新增、确认、取消、删除和导出接口。</p>
 */
@Tag(name = "出纳管理")
@ApiSupport(order = 36, author = "ratel / WenZhang / 18782945613")
@RestController
@RequestMapping("/api/cashier-transactions")
public class CashierController {

    /** 出纳管理服务，封装资金流水业务规则。 */
    private final CashierService service;

    /**
     * 构造出纳控制器。
     *
     * <p>实现步骤：接收出纳服务并保存到成员字段，接口层只做参数绑定和权限控制。</p>
     */
    public CashierController(CashierService service) {
        this.service = service;
    }

    /**
     * 查询出纳流水列表。
     *
     * <p>实现步骤：接收页面筛选条件，调用服务层按当前账套返回资金流水。</p>
     */
    @ApiOperationSupport(order = 10)
    @Operation(summary = "出纳流水列表")
    @PreAuthorize("hasAuthority('FINANCE_VOUCHER_MANAGE') or hasAuthority('REPORT_VIEW')")
    @GetMapping
    public ApiResponse<List<CashierTransactionView>> list(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            @RequestParam(required = false) CashierTransactionType transactionType,
            @RequestParam(required = false) CashierTransactionStatus status,
            @RequestParam(required = false) String projectCode,
            @RequestParam(required = false) String partnerName,
            @RequestParam(required = false) String relatedBizNo
    ) {
        return ApiResponse.ok(service.list(startDate, endDate, transactionType, status, projectCode, partnerName, relatedBizNo));
    }

    /**
     * 新增出纳流水。
     *
     * <p>实现步骤：校验请求后创建草稿状态资金流水。</p>
     */
    @ApiOperationSupport(order = 20)
    @Operation(summary = "新增出纳流水")
    @PreAuthorize("hasAuthority('FINANCE_VOUCHER_MANAGE')")
    @PostMapping
    /**
     * 接收新增出纳流水请求。
     *
     * <p>实现步骤：
     * 1. 使用 Bean Validation 校验请求体；
     * 2. 调用服务层创建草稿资金流水；
     * 3. 使用统一响应体返回创建结果和提示语。</p>
     */
    public ApiResponse<CashierTransactionView> create(@Valid @RequestBody CashierTransactionRequest request) {
        return ApiResponse.ok("出纳流水已创建", service.create(request));
    }

    /**
     * 确认出纳流水。
     *
     * <p>实现步骤：将草稿出纳流水确认成正式资金事实。</p>
     */
    @ApiOperationSupport(order = 30)
    @Operation(summary = "确认出纳流水")
    @PreAuthorize("hasAuthority('FINANCE_VOUCHER_MANAGE')")
    @PostMapping("/{id}/confirm")
    /**
     * 接收出纳流水确认请求。
     *
     * <p>实现步骤：
     * 1. 从路径中读取流水 ID；
     * 2. 调用服务层执行状态校验和确认；
     * 3. 返回确认后的资金流水视图。</p>
     */
    public ApiResponse<CashierTransactionView> confirm(@PathVariable Long id) {
        return ApiResponse.ok("出纳流水已确认", service.confirm(id));
    }

    /**
     * 取消出纳流水。
     *
     * <p>实现步骤：将未制证出纳流水置为已取消。</p>
     */
    @ApiOperationSupport(order = 40)
    @Operation(summary = "取消出纳流水")
    @PreAuthorize("hasAuthority('FINANCE_VOUCHER_MANAGE')")
    @PostMapping("/{id}/cancel")
    /**
     * 接收出纳流水取消请求。
     *
     * <p>实现步骤：
     * 1. 从路径中读取流水 ID；
     * 2. 调用服务层校验是否允许取消；
     * 3. 返回取消后的资金流水视图。</p>
     */
    public ApiResponse<CashierTransactionView> cancel(@PathVariable Long id) {
        return ApiResponse.ok("出纳流水已取消", service.cancel(id));
    }

    /**
     * 批量删除出纳流水。
     *
     * <p>实现步骤：只允许删除草稿或已取消流水，删除前前端需二次确认。</p>
     */
    @ApiOperationSupport(order = 50)
    @Operation(summary = "批量删除出纳流水")
    @PreAuthorize("hasAuthority('FINANCE_VOUCHER_MANAGE')")
    @PostMapping("/batch-delete")
    /**
     * 接收出纳流水批量删除请求。
     *
     * <p>实现步骤：
     * 1. 校验前端提交的 ID 集合；
     * 2. 调用服务层按业务规则删除；
     * 3. 返回统一成功提示。</p>
     */
    public ApiResponse<Void> delete(@Valid @RequestBody BatchIdsRequest request) {
        service.delete(request.ids());
        return ApiResponse.ok("出纳流水已批量删除", null);
    }

    /**
     * 导出出纳流水。
     *
     * <p>实现步骤：选中行优先导出，否则按当前筛选条件导出。</p>
     */
    @ApiOperationSupport(order = 60)
    @Operation(summary = "导出出纳流水")
    @PreAuthorize("hasAuthority('FINANCE_VOUCHER_MANAGE') or hasAuthority('REPORT_VIEW')")
    @PostMapping("/export")
    /**
     * 接收出纳流水导出请求。
     *
     * <p>实现步骤：
     * 1. 读取选中行或当前筛选条件；
     * 2. 调用服务层生成 Excel 字节；
     * 3. 通过下载响应返回 xlsx 文件。</p>
     */
    public ResponseEntity<byte[]> export(@RequestBody(required = false) CashierExportRequest request) {
        return ExcelDownload.response("出纳流水.xlsx", service.export(request));
    }
}
