package com.ratel.fm.web.finance;

import com.ratel.fm.common.ApiResponse;
import com.ratel.fm.domain.finance.AccountingSourceType;
import com.ratel.fm.domain.finance.SubjectCategory;
import com.ratel.fm.domain.finance.VoucherStatus;
import com.ratel.fm.service.finance.FinanceService;
import com.ratel.fm.service.finance.VoucherImportService;
import com.ratel.fm.web.dto.finance.FinanceDtos.SubjectRequest;
import com.ratel.fm.web.dto.finance.FinanceDtos.SubjectView;
import com.ratel.fm.web.dto.finance.FinanceDtos.AccountingSourceView;
import com.ratel.fm.web.dto.finance.FinanceDtos.AutoVoucherRequest;
import com.ratel.fm.web.dto.finance.FinanceDtos.AutoVoucherResult;
import com.ratel.fm.web.dto.finance.FinanceDtos.TrialBalanceRow;
import com.ratel.fm.web.dto.finance.FinanceDtos.VoucherExportRequest;
import com.ratel.fm.web.dto.finance.FinanceDtos.VoucherImportResult;
import com.ratel.fm.web.dto.finance.FinanceDtos.VoucherRequest;
import com.ratel.fm.web.dto.finance.FinanceDtos.VoucherSourceDetail;
import com.ratel.fm.web.dto.finance.FinanceDtos.VoucherView;
import com.ratel.fm.web.dto.common.BatchIdsRequest;
import com.ratel.fm.web.dto.operationlog.BusinessOperationLogDtos.BusinessOperationLogPage;
import com.ratel.fm.web.export.ExcelDownload;
import com.github.xiaoymin.knife4j.annotations.ApiOperationSupport;
import com.github.xiaoymin.knife4j.annotations.ApiSupport;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;

@Tag(name = "财务记账")
@ApiSupport(order = 20, author = "ratel / WenZhang / 18782945613")
@RestController
@RequestMapping("/api/finance")
/**
 * FinanceController 类。
 * 
 * <p>用于承载 FinanceController 相关的业务数据、接口契约或处理逻辑，便于维护人员快速定位模块职责。</p>
 */
public class FinanceController {

    /**
     * 字段 financeService：保存 financeService 对应的业务数据、运行配置或依赖对象，供本类逻辑读取和维护。
     */
    private final FinanceService financeService;
    /** 凭证导入识别服务，用于把图片或PDF转换为可编辑分录草稿。 */
    private final VoucherImportService voucherImportService;

    /**
     * 构造 FinanceController 实例。
     * 
     * <p>实现步骤：
     * 1. 接收调用方传入的依赖对象或初始化参数；
     * 2. 保存到成员字段，保证后续业务方法可以复用；
     * 3. 完成实例初始化。</p>
     */
    public FinanceController(FinanceService financeService, VoucherImportService voucherImportService) {
        this.financeService = financeService;
        this.voucherImportService = voucherImportService;
    }

    @ApiOperationSupport(order = 10, author = "ratel / WenZhang / 18782945613")
    @Operation(summary = "查询会计科目", description = "作者：ratel；开发人员：WenZhang；联系方式：18782945613。")
    @PreAuthorize("hasAuthority('FINANCE_SUBJECT_MANAGE') or hasAuthority('FINANCE_VOUCHER_MANAGE') or hasAuthority('REPORT_VIEW')")
    @GetMapping("/subjects")
    /**
     * 执行 listSubjects 方法。
     * 
     * <p>实现步骤：
     * 1. 接收并校验调用方传入的数据；
     * 2. 按当前方法职责执行业务查询、转换或持久化处理；
     * 3. 返回处理结果或更新对象状态。</p>
     */
    public ApiResponse<List<SubjectView>> listSubjects(
            @RequestParam(defaultValue = "false") boolean onlyEnabled,
            @RequestParam(required = false) String code,
            @RequestParam(required = false) String name,
            @RequestParam(required = false) SubjectCategory category,
            @RequestParam(required = false) Long parentId,
            @RequestParam(required = false) Boolean enabled,
            @RequestParam(required = false) String description
    ) {
        return ApiResponse.ok(financeService.listSubjects(onlyEnabled, code, name, category, parentId, enabled, description));
    }

    @ApiOperationSupport(order = 20, author = "ratel / WenZhang / 18782945613")
    @Operation(summary = "新增会计科目", description = "作者：ratel；开发人员：WenZhang；联系方式：18782945613。")
    @PreAuthorize("hasAuthority('FINANCE_SUBJECT_MANAGE')")
    @PostMapping("/subjects")
    /**
     * 执行 createSubject 方法。
     * 
     * <p>实现步骤：
     * 1. 接收并校验调用方传入的数据；
     * 2. 按当前方法职责执行业务查询、转换或持久化处理；
     * 3. 返回处理结果或更新对象状态。</p>
     */
    public ApiResponse<SubjectView> createSubject(@Valid @RequestBody SubjectRequest request) {
        return ApiResponse.ok("科目已创建", financeService.createSubject(request));
    }

    @ApiOperationSupport(order = 30, author = "ratel / WenZhang / 18782945613")
    @Operation(summary = "修改会计科目", description = "作者：ratel；开发人员：WenZhang；联系方式：18782945613。")
    @PreAuthorize("hasAuthority('FINANCE_SUBJECT_MANAGE')")
    @PutMapping("/subjects/{id}")
    /**
     * 执行 updateSubject 方法。
     * 
     * <p>实现步骤：
     * 1. 接收并校验调用方传入的数据；
     * 2. 按当前方法职责执行业务查询、转换或持久化处理；
     * 3. 返回处理结果或更新对象状态。</p>
     */
    public ApiResponse<SubjectView> updateSubject(@PathVariable Long id, @Valid @RequestBody SubjectRequest request) {
        return ApiResponse.ok("科目已更新", financeService.updateSubject(id, request));
    }

    @ApiOperationSupport(order = 40, author = "ratel / WenZhang / 18782945613")
    @Operation(summary = "删除会计科目", description = "作者：ratel；开发人员：WenZhang；联系方式：18782945613。")
    @PreAuthorize("hasAuthority('FINANCE_SUBJECT_MANAGE')")
    @DeleteMapping("/subjects/{id}")
    /**
     * 执行 deleteSubject 方法。
     * 
     * <p>实现步骤：
     * 1. 接收并校验调用方传入的数据；
     * 2. 按当前方法职责执行业务查询、转换或持久化处理；
     * 3. 返回处理结果或更新对象状态。</p>
     */
    public ApiResponse<Void> deleteSubject(@PathVariable Long id) {
        financeService.deleteSubject(id);
        return ApiResponse.ok("科目已删除", null);
    }

    @ApiOperationSupport(order = 45, author = "ratel / WenZhang / 18782945613")
    @Operation(summary = "批量删除会计科目", description = "作者：ratel；开发人员：WenZhang；联系方式：18782945613。前端必须二次确认后调用。")
    @PreAuthorize("hasAuthority('FINANCE_SUBJECT_MANAGE')")
    @PostMapping("/subjects/batch-delete")
    /**
     * 执行 deleteSubjects 方法。
     * 
     * <p>实现步骤：
     * 1. 接收并校验调用方传入的数据；
     * 2. 按当前方法职责执行业务查询、转换或持久化处理；
     * 3. 返回处理结果或更新对象状态。</p>
     */
    public ApiResponse<Void> deleteSubjects(@Valid @RequestBody BatchIdsRequest request) {
        financeService.deleteSubjects(request.ids());
        return ApiResponse.ok("科目已批量删除", null);
    }

    @ApiOperationSupport(order = 50, author = "ratel / WenZhang / 18782945613")
    @Operation(summary = "查询凭证列表", description = "作者：ratel；开发人员：WenZhang；联系方式：18782945613。")
    @PreAuthorize("hasAuthority('FINANCE_VOUCHER_MANAGE') or hasAuthority('REPORT_VIEW')")
    @GetMapping("/vouchers")
    /**
     * 执行 listVouchers 方法。
     * 
     * <p>实现步骤：
     * 1. 接收并校验调用方传入的数据；
     * 2. 按当前方法职责执行业务查询、转换或持久化处理；
     * 3. 返回处理结果或更新对象状态。</p>
     */
    public ApiResponse<List<VoucherView>> listVouchers(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            @RequestParam(required = false) String belongMonth,
            @RequestParam(required = false) String projectCode,
            @RequestParam(required = false) String voucherNo,
            @RequestParam(required = false) String summary,
            @RequestParam(required = false) String sourceBizNo,
            @RequestParam(required = false) VoucherStatus status,
            @RequestParam(required = false) String createdBy
    ) {
        return ApiResponse.ok(financeService.listVouchers(startDate, endDate, belongMonth, projectCode, voucherNo, summary, sourceBizNo, status, createdBy));
    }

    @ApiOperationSupport(order = 55, author = "ratel / WenZhang / 18782945613")
    @Operation(summary = "导出凭证列表", description = "作者：ratel；开发人员：WenZhang；联系方式：18782945613。选中行优先导出，未选中时按当前搜索条件导出。")
    @PreAuthorize("hasAuthority('FINANCE_VOUCHER_MANAGE') or hasAuthority('REPORT_VIEW')")
    @PostMapping("/vouchers/export")
    /**
     * 执行 exportVouchers 方法。
     * 
     * <p>实现步骤：
     * 1. 接收并校验调用方传入的数据；
     * 2. 按当前方法职责执行业务查询、转换或持久化处理；
     * 3. 返回处理结果或更新对象状态。</p>
     */
    public ResponseEntity<byte[]> exportVouchers(@RequestBody(required = false) VoucherExportRequest request) {
        return ExcelDownload.response("凭证记账.xlsx", financeService.exportVouchers(request));
    }

    @ApiOperationSupport(order = 60, author = "ratel / WenZhang / 18782945613")
    @Operation(summary = "查询凭证明细", description = "作者：ratel；开发人员：WenZhang；联系方式：18782945613。")
    @PreAuthorize("hasAuthority('FINANCE_VOUCHER_MANAGE') or hasAuthority('REPORT_VIEW')")
    @GetMapping("/vouchers/{id}")
    /**
     * 执行 getVoucher 方法。
     * 
     * <p>实现步骤：
     * 1. 接收并校验调用方传入的数据；
     * 2. 按当前方法职责执行业务查询、转换或持久化处理；
     * 3. 返回处理结果或更新对象状态。</p>
     */
    public ApiResponse<VoucherView> getVoucher(@PathVariable Long id) {
        return ApiResponse.ok(financeService.getVoucher(id));
    }

    @ApiOperationSupport(order = 65, author = "ratel / WenZhang / 18782945613")
    @Operation(summary = "查询凭证来源详情", description = "用于自动凭证反向查看来源模块和来源单据核心信息。")
    @PreAuthorize("hasAuthority('FINANCE_VOUCHER_MANAGE') or hasAuthority('REPORT_VIEW')")
    @GetMapping("/vouchers/{id}/source")
    /**
     * 查询凭证来源详情。
     *
     * <p>实现步骤：
     * 1. 接收凭证主键；
     * 2. 调用财务服务按来源类型和来源主键反查业务单据；
     * 3. 返回通用键值字段，前端在查看来源弹窗中展示。</p>
     */
    public ApiResponse<VoucherSourceDetail> getVoucherSource(@PathVariable Long id) {
        return ApiResponse.ok(financeService.getVoucherSourceDetail(id));
    }

    @ApiOperationSupport(order = 70, author = "ratel / WenZhang / 18782945613")
    @Operation(summary = "新增凭证草稿", description = "作者：ratel；开发人员：WenZhang；联系方式：18782945613。")
    @PreAuthorize("hasAuthority('FINANCE_VOUCHER_MANAGE')")
    @PostMapping("/vouchers")
    /**
     * 执行 createVoucher 方法。
     * 
     * <p>实现步骤：
     * 1. 接收并校验调用方传入的数据；
     * 2. 按当前方法职责执行业务查询、转换或持久化处理；
     * 3. 返回处理结果或更新对象状态。</p>
     */
    public ApiResponse<VoucherView> createVoucher(@Valid @RequestBody VoucherRequest request) {
        return ApiResponse.ok("凭证已创建", financeService.createVoucher(request));
    }

    @ApiOperationSupport(order = 80, author = "ratel / WenZhang / 18782945613")
    @Operation(summary = "修改凭证草稿", description = "作者：ratel；开发人员：WenZhang；联系方式：18782945613。")
    @PreAuthorize("hasAuthority('FINANCE_VOUCHER_MANAGE')")
    @PutMapping("/vouchers/{id}")
    /**
     * 执行 updateVoucher 方法。
     * 
     * <p>实现步骤：
     * 1. 接收并校验调用方传入的数据；
     * 2. 按当前方法职责执行业务查询、转换或持久化处理；
     * 3. 返回处理结果或更新对象状态。</p>
     */
    public ApiResponse<VoucherView> updateVoucher(@PathVariable Long id, @Valid @RequestBody VoucherRequest request) {
        return ApiResponse.ok("凭证已更新", financeService.updateVoucher(id, request));
    }

    @ApiOperationSupport(order = 85, author = "ratel / WenZhang / 18782945613")
    @Operation(summary = "识别导入凭证图片或PDF", description = "上传多个图片或PDF，AI识别后返回凭证分录草稿；接口不保存凭证，用户确认保存后附件再随凭证上传。")
    @PreAuthorize("hasAuthority('FINANCE_VOUCHER_MANAGE')")
    @PostMapping("/vouchers/import-recognize")
    /**
     * 识别导入凭证图片或 PDF。
     *
     * <p>实现步骤：
     * 1. 接收前端上传的多个图片或 PDF；
     * 2. 调用凭证导入识别服务生成分录草稿；
     * 3. 返回识别结果，前端只回填表单，等待用户确认保存。</p>
     */
    public ApiResponse<VoucherImportResult> importVoucher(@RequestParam("files") List<MultipartFile> files) {
        return ApiResponse.ok("凭证导入识别完成", voucherImportService.recognize(files));
    }

    @ApiOperationSupport(order = 90, author = "ratel / WenZhang / 18782945613")
    @Operation(summary = "凭证过账", description = "作者：ratel；开发人员：WenZhang；联系方式：18782945613。")
    @PreAuthorize("hasAuthority('FINANCE_VOUCHER_MANAGE')")
    @PostMapping("/vouchers/{id}/post")
    /**
     * 执行 postVoucher 方法。
     * 
     * <p>实现步骤：
     * 1. 接收并校验调用方传入的数据；
     * 2. 按当前方法职责执行业务查询、转换或持久化处理；
     * 3. 返回处理结果或更新对象状态。</p>
     */
    public ApiResponse<VoucherView> postVoucher(@PathVariable Long id) {
        return ApiResponse.ok("凭证已过账", financeService.postVoucher(id));
    }

    @ApiOperationSupport(order = 100, author = "ratel / WenZhang / 18782945613")
    @Operation(summary = "凭证作废", description = "作者：ratel；开发人员：WenZhang；联系方式：18782945613。")
    @PreAuthorize("hasAuthority('FINANCE_VOUCHER_MANAGE')")
    @PostMapping("/vouchers/{id}/void")
    /**
     * 执行 voidVoucher 方法。
     * 
     * <p>实现步骤：
     * 1. 接收并校验调用方传入的数据；
     * 2. 按当前方法职责执行业务查询、转换或持久化处理；
     * 3. 返回处理结果或更新对象状态。</p>
     */
    public ApiResponse<VoucherView> voidVoucher(@PathVariable Long id) {
        return ApiResponse.ok("凭证已作废", financeService.voidVoucher(id));
    }

    @ApiOperationSupport(order = 102, author = "ratel / WenZhang / 18782945613")
    @Operation(summary = "查询会计平台业务来源", description = "按用友类会计平台逻辑查询可制证的采购单或应收应付单，并标记是否已经生成未作废凭证。")
    @PreAuthorize("hasAuthority('FINANCE_VOUCHER_MANAGE')")
    @GetMapping("/accounting-platform/sources")
    /**
     * 查询会计平台业务来源。
     *
     * <p>实现步骤：
     * 1. 接收来源类型；
     * 2. 调用财务服务读取采购或应收应付业务来源；
     * 3. 返回统一来源行，供前端生成凭证草稿。</p>
     */
    public ApiResponse<List<AccountingSourceView>> accountingSources(@RequestParam(required = false) AccountingSourceType sourceType) {
        return ApiResponse.ok(financeService.accountingSources(sourceType));
    }

    @ApiOperationSupport(order = 103, author = "ratel / WenZhang / 18782945613")
    @Operation(summary = "自动生成凭证草稿", description = "按业务来源、借方科目和贷方科目生成凭证草稿，保留来源单号用于追溯。")
    @PreAuthorize("hasAuthority('FINANCE_VOUCHER_MANAGE')")
    @PostMapping("/accounting-platform/auto-vouchers")
    /**
     * 自动生成凭证草稿。
     *
     * <p>实现步骤：
     * 1. 接收业务来源和借贷科目；
     * 2. 调用财务服务校验重复制证、科目状态和金额；
     * 3. 返回生成后的凭证草稿，供财务人员继续审核过账。</p>
     */
    public ApiResponse<AutoVoucherResult> generateAutoVoucher(@Valid @RequestBody AutoVoucherRequest request) {
        return ApiResponse.ok("凭证草稿已生成", financeService.generateAutoVoucher(request));
    }

    @ApiOperationSupport(order = 105, author = "ratel / WenZhang / 18782945613")
    @Operation(summary = "批量删除凭证", description = "作者：ratel；开发人员：WenZhang；联系方式：18782945613。前端必须二次确认后调用。")
    @PreAuthorize("hasAuthority('FINANCE_VOUCHER_MANAGE')")
    @PostMapping("/vouchers/batch-delete")
    /**
     * 执行 deleteVouchers 方法。
     * 
     * <p>实现步骤：
     * 1. 接收并校验调用方传入的数据；
     * 2. 按当前方法职责执行业务查询、转换或持久化处理；
     * 3. 返回处理结果或更新对象状态。</p>
     */
    public ApiResponse<Void> deleteVouchers(@Valid @RequestBody BatchIdsRequest request) {
        financeService.deleteVouchers(request.ids());
        return ApiResponse.ok("凭证已批量删除", null);
    }

    @ApiOperationSupport(order = 106, author = "ratel / WenZhang / 18782945613")
    @Operation(summary = "查询凭证操作流水", description = "作者：ratel；开发人员：WenZhang；联系方式：18782945613。按操作时间倒序分页返回凭证新增、修改、过账、作废等操作记录。")
    @PreAuthorize("hasAuthority('FINANCE_VOUCHER_MANAGE') or hasAuthority('REPORT_VIEW')")
    @GetMapping("/vouchers/{id}/operation-logs")
    /**
     * 执行 listVoucherOperationLogs 方法。
     * 
     * <p>实现步骤：
     * 1. 接收并校验调用方传入的数据；
     * 2. 按当前方法职责执行业务查询、转换或持久化处理；
     * 3. 返回处理结果或更新对象状态。</p>
     */
    public ApiResponse<BusinessOperationLogPage> listVoucherOperationLogs(
            @PathVariable Long id,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) OffsetDateTime startTime,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) OffsetDateTime endTime,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        return ApiResponse.ok(financeService.pageVoucherOperationLogs(id, startTime, endTime, page, size));
    }

    @ApiOperationSupport(order = 110, author = "ratel / WenZhang / 18782945613")
    @Operation(summary = "试算平衡表", description = "作者：ratel；开发人员：WenZhang；联系方式：18782945613。")
    @PreAuthorize("hasAuthority('REPORT_VIEW')")
    @GetMapping("/reports/trial-balance")
    /**
     * 执行 trialBalance 方法。
     * 
     * <p>实现步骤：
     * 1. 接收并校验调用方传入的数据；
     * 2. 按当前方法职责执行业务查询、转换或持久化处理；
     * 3. 返回处理结果或更新对象状态。</p>
     */
    public ApiResponse<List<TrialBalanceRow>> trialBalance(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate
    ) {
        return ApiResponse.ok(financeService.trialBalance(startDate, endDate));
    }
}
