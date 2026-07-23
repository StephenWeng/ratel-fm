package com.ratel.fm.web.operation;

import com.ratel.fm.common.ApiResponse;
import com.ratel.fm.domain.logistics.ShipmentStatus;
import com.ratel.fm.domain.purchase.PurchaseStatus;
import com.ratel.fm.service.operation.OperationService;
import com.ratel.fm.web.dto.operation.OperationDtos.PurchaseOrderExportRequest;
import com.ratel.fm.web.dto.operation.OperationDtos.PurchaseApprovalSubmitRequest;
import com.ratel.fm.web.dto.operation.OperationDtos.PurchaseCancelRequest;
import com.ratel.fm.web.dto.operation.OperationDtos.PurchaseOrderRequest;
import com.ratel.fm.web.dto.operation.OperationDtos.PurchaseOrderView;
import com.ratel.fm.web.dto.operation.OperationDtos.ShipmentExportRequest;
import com.ratel.fm.web.dto.operation.OperationDtos.ShipmentOperationLogPage;
import com.ratel.fm.web.dto.operation.OperationDtos.ShipmentRequest;
import com.ratel.fm.web.dto.operation.OperationDtos.ShipmentStatusConfirmRequest;
import com.ratel.fm.web.dto.operation.OperationDtos.ShipmentView;
import com.ratel.fm.web.dto.common.BatchIdsRequest;
import com.ratel.fm.web.dto.operationlog.BusinessOperationLogDtos.BusinessOperationLogPage;
import com.ratel.fm.web.export.ExcelDownload;
import com.github.xiaoymin.knife4j.annotations.ApiOperationSupport;
import com.github.xiaoymin.knife4j.annotations.ApiSupport;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;

@Tag(name = "采购与物流")
@ApiSupport(order = 30, author = "ratel / WenZhang / 18782945613")
@RestController
@RequestMapping("/api")
/**
 * OperationController 类。
 * 
 * <p>用于承载 OperationController 相关的业务数据、接口契约或处理逻辑，便于维护人员快速定位模块职责。</p>
 */
public class OperationController {

    /**
     * 字段 operationService：保存 operationService 对应的业务数据、运行配置或依赖对象，供本类逻辑读取和维护。
     */
    private final OperationService operationService;

    /**
     * 构造 OperationController 实例。
     * 
     * <p>实现步骤：
     * 1. 接收调用方传入的依赖对象或初始化参数；
     * 2. 保存到成员字段，保证后续业务方法可以复用；
     * 3. 完成实例初始化。</p>
     */
    public OperationController(OperationService operationService) {
        this.operationService = operationService;
    }

    @ApiOperationSupport(order = 10, author = "ratel / WenZhang / 18782945613")
    @Operation(summary = "查询采购单列表", description = "作者：ratel；开发人员：WenZhang；联系方式：18782945613。")
    @PreAuthorize("hasAuthority('PURCHASE_MANAGE') or hasAuthority('REPORT_VIEW')")
    @GetMapping("/purchase-orders")
    /**
     * 执行 listPurchaseOrders 方法。
     * 
     * <p>实现步骤：
     * 1. 接收并校验调用方传入的数据；
     * 2. 按当前方法职责执行业务查询、转换或持久化处理；
     * 3. 返回处理结果或更新对象状态。</p>
     */
    public ApiResponse<List<PurchaseOrderView>> listPurchaseOrders(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            @RequestParam(required = false) String orderNo,
            @RequestParam(required = false) String supplierName,
            @RequestParam(required = false) String projectCode,
            @RequestParam(required = false) PurchaseStatus status,
            @RequestParam(required = false) String createdBy,
            @RequestParam(required = false) String remark
    ) {
        return ApiResponse.ok(operationService.listPurchaseOrders(startDate, endDate, orderNo, supplierName, projectCode, status, createdBy, remark));
    }

    @ApiOperationSupport(order = 15, author = "ratel / WenZhang / 18782945613")
    @Operation(summary = "导出采购单列表", description = "作者：ratel；开发人员：WenZhang；联系方式：18782945613。选中行优先导出，未选中时按当前搜索条件导出。")
    @PreAuthorize("hasAuthority('PURCHASE_MANAGE') or hasAuthority('REPORT_VIEW')")
    @PostMapping("/purchase-orders/export")
    /**
     * 执行 exportPurchaseOrders 方法。
     * 
     * <p>实现步骤：
     * 1. 接收并校验调用方传入的数据；
     * 2. 按当前方法职责执行业务查询、转换或持久化处理；
     * 3. 返回处理结果或更新对象状态。</p>
     */
    public ResponseEntity<byte[]> exportPurchaseOrders(@RequestBody(required = false) PurchaseOrderExportRequest request) {
        return ExcelDownload.response("采购管理.xlsx", operationService.exportPurchaseOrders(request));
    }

    @ApiOperationSupport(order = 20, author = "ratel / WenZhang / 18782945613")
    @Operation(summary = "查询采购单明细", description = "作者：ratel；开发人员：WenZhang；联系方式：18782945613。")
    @PreAuthorize("hasAuthority('PURCHASE_MANAGE') or hasAuthority('REPORT_VIEW')")
    @GetMapping("/purchase-orders/{id}")
    /**
     * 执行 getPurchaseOrder 方法。
     * 
     * <p>实现步骤：
     * 1. 接收并校验调用方传入的数据；
     * 2. 按当前方法职责执行业务查询、转换或持久化处理；
     * 3. 返回处理结果或更新对象状态。</p>
     */
    public ApiResponse<PurchaseOrderView> getPurchaseOrder(@PathVariable Long id) {
        return ApiResponse.ok(operationService.getPurchaseOrder(id));
    }

    @ApiOperationSupport(order = 30, author = "ratel / WenZhang / 18782945613")
    @Operation(summary = "新增采购单", description = "作者：ratel；开发人员：WenZhang；联系方式：18782945613。")
    @PreAuthorize("hasAuthority('PURCHASE_MANAGE')")
    @PostMapping("/purchase-orders")
    /**
     * 执行 createPurchaseOrder 方法。
     * 
     * <p>实现步骤：
     * 1. 接收并校验调用方传入的数据；
     * 2. 按当前方法职责执行业务查询、转换或持久化处理；
     * 3. 返回处理结果或更新对象状态。</p>
     */
    public ApiResponse<PurchaseOrderView> createPurchaseOrder(@Valid @RequestBody PurchaseOrderRequest request) {
        return ApiResponse.ok("采购单已创建", operationService.createPurchaseOrder(request));
    }

    @ApiOperationSupport(order = 40, author = "ratel / WenZhang / 18782945613")
    @Operation(summary = "修改采购单", description = "作者：ratel；开发人员：WenZhang；联系方式：18782945613。")
    @PreAuthorize("hasAuthority('PURCHASE_MANAGE')")
    @PutMapping("/purchase-orders/{id}")
    /**
     * 执行 updatePurchaseOrder 方法。
     * 
     * <p>实现步骤：
     * 1. 接收并校验调用方传入的数据；
     * 2. 按当前方法职责执行业务查询、转换或持久化处理；
     * 3. 返回处理结果或更新对象状态。</p>
     */
    public ApiResponse<PurchaseOrderView> updatePurchaseOrder(
            @PathVariable Long id,
            @Valid @RequestBody PurchaseOrderRequest request
    ) {
        return ApiResponse.ok("采购单已更新", operationService.updatePurchaseOrder(id, request));
    }

    @ApiOperationSupport(order = 50, author = "ratel / WenZhang / 18782945613")
    @Operation(summary = "变更采购单状态", description = "作者：ratel；开发人员：WenZhang；联系方式：18782945613。")
    @PreAuthorize("hasAuthority('PURCHASE_MANAGE')")
    @PostMapping("/purchase-orders/{id}/status/{status}")
    /**
     * 执行 changePurchaseStatus 方法。
     * 
     * <p>实现步骤：
     * 1. 接收并校验调用方传入的数据；
     * 2. 按当前方法职责执行业务查询、转换或持久化处理；
     * 3. 返回处理结果或更新对象状态。</p>
     */
    public ApiResponse<PurchaseOrderView> changePurchaseStatus(
            @PathVariable Long id,
            @PathVariable PurchaseStatus status
    ) {
        return ApiResponse.ok("采购单状态已更新", operationService.changePurchaseStatus(id, status));
    }

    @ApiOperationSupport(order = 51, author = "ratel / WenZhang / 18782945613")
    @Operation(summary = "提交采购审批", description = "作者：ratel；开发人员：WenZhang；联系方式：18782945613。")
    @PreAuthorize("hasAuthority('PURCHASE_MANAGE')")
    @PostMapping("/purchase-orders/{id}/submit-approval")
    public ApiResponse<PurchaseOrderView> submitPurchaseApproval(
            @PathVariable Long id,
            @Valid @RequestBody PurchaseApprovalSubmitRequest request
    ) {
        return ApiResponse.ok("采购审批已提交", operationService.submitPurchaseApproval(id, request));
    }

    @ApiOperationSupport(order = 52, author = "ratel / WenZhang / 18782945613")
    @Operation(summary = "发起采购", description = "作者：ratel；开发人员：WenZhang；联系方式：18782945613。")
    @PreAuthorize("hasAuthority('PURCHASE_MANAGE')")
    @PostMapping("/purchase-orders/{id}/start-purchase")
    public ApiResponse<PurchaseOrderView> startPurchase(@PathVariable Long id) {
        return ApiResponse.ok("采购已发起", operationService.startPurchase(id));
    }

    @ApiOperationSupport(order = 53, author = "ratel / WenZhang / 18782945613")
    @Operation(summary = "确认采购已收货", description = "作者：ratel；开发人员：WenZhang；联系方式：18782945613。")
    @PreAuthorize("hasAuthority('PURCHASE_MANAGE')")
    @PostMapping("/purchase-orders/{id}/receive")
    public ApiResponse<PurchaseOrderView> receivePurchase(@PathVariable Long id) {
        return ApiResponse.ok("采购已收货", operationService.receivePurchase(id));
    }

    @ApiOperationSupport(order = 54, author = "ratel / WenZhang / 18782945613")
    @Operation(summary = "取消采购", description = "作者：ratel；开发人员：WenZhang；联系方式：18782945613。")
    @PreAuthorize("hasAuthority('PURCHASE_MANAGE')")
    @PostMapping("/purchase-orders/{id}/cancel")
    public ApiResponse<PurchaseOrderView> cancelPurchase(
            @PathVariable Long id,
            @Valid @RequestBody PurchaseCancelRequest request
    ) {
        return ApiResponse.ok("采购已取消", operationService.cancelPurchase(id, request));
    }

    @ApiOperationSupport(order = 55, author = "ratel / WenZhang / 18782945613")
    @Operation(summary = "批量删除采购单", description = "作者：ratel；开发人员：WenZhang；联系方式：18782945613。前端必须二次确认后调用。")
    @PreAuthorize("hasAuthority('PURCHASE_MANAGE')")
    @PostMapping("/purchase-orders/batch-delete")
    /**
     * 执行 deletePurchaseOrders 方法。
     * 
     * <p>实现步骤：
     * 1. 接收并校验调用方传入的数据；
     * 2. 按当前方法职责执行业务查询、转换或持久化处理；
     * 3. 返回处理结果或更新对象状态。</p>
     */
    public ApiResponse<Void> deletePurchaseOrders(@Valid @RequestBody BatchIdsRequest request) {
        operationService.deletePurchaseOrders(request.ids());
        return ApiResponse.ok("采购单已批量删除", null);
    }

    @ApiOperationSupport(order = 56, author = "ratel / WenZhang / 18782945613")
    @Operation(summary = "查询采购单操作流水", description = "作者：ratel；开发人员：WenZhang；联系方式：18782945613。按操作时间倒序分页返回采购单新增、修改、状态变更等操作记录。")
    @PreAuthorize("hasAuthority('PURCHASE_MANAGE') or hasAuthority('REPORT_VIEW')")
    @GetMapping("/purchase-orders/{id}/operation-logs")
    /**
     * 执行 listPurchaseOperationLogs 方法。
     * 
     * <p>实现步骤：
     * 1. 接收并校验调用方传入的数据；
     * 2. 按当前方法职责执行业务查询、转换或持久化处理；
     * 3. 返回处理结果或更新对象状态。</p>
     */
    public ApiResponse<BusinessOperationLogPage> listPurchaseOperationLogs(
            @PathVariable Long id,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) OffsetDateTime startTime,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) OffsetDateTime endTime,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        return ApiResponse.ok(operationService.pagePurchaseOperationLogs(id, startTime, endTime, page, size));
    }

    @ApiOperationSupport(order = 60, author = "ratel / WenZhang / 18782945613")
    @Operation(summary = "查询物流单列表", description = "作者：ratel；开发人员：WenZhang；联系方式：18782945613。")
    @PreAuthorize("hasAuthority('LOGISTICS_MANAGE') or hasAuthority('REPORT_VIEW')")
    @GetMapping("/shipments")
    /**
     * 执行 listShipments 方法。
     * 
     * <p>实现步骤：
     * 1. 接收并校验调用方传入的数据；
     * 2. 按当前方法职责执行业务查询、转换或持久化处理；
     * 3. 返回处理结果或更新对象状态。</p>
     */
    public ApiResponse<List<ShipmentView>> listShipments(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            @RequestParam(required = false) String shipmentNo,
            @RequestParam(required = false) String relatedOrderNo,
            @RequestParam(required = false) String projectCode,
            @RequestParam(required = false) String carrierName,
            @RequestParam(required = false) String trackingNo,
            @RequestParam(required = false) String originDivisionCodes,
            @RequestParam(required = false) String destinationDivisionCodes,
            @RequestParam(required = false) String origin,
            @RequestParam(required = false) String destination,
            @RequestParam(required = false) ShipmentStatus status
    ) {
        return ApiResponse.ok(operationService.listShipments(startDate, endDate, shipmentNo, relatedOrderNo, projectCode,
                carrierName, trackingNo, originDivisionCodes, destinationDivisionCodes, origin, destination, status));
    }

    @ApiOperationSupport(order = 65, author = "ratel / WenZhang / 18782945613")
    @Operation(summary = "导出物流单列表", description = "作者：ratel；开发人员：WenZhang；联系方式：18782945613。选中行优先导出，未选中时按当前搜索条件导出。")
    @PreAuthorize("hasAuthority('LOGISTICS_MANAGE') or hasAuthority('REPORT_VIEW')")
    @PostMapping("/shipments/export")
    /**
     * 执行 exportShipments 方法。
     * 
     * <p>实现步骤：
     * 1. 接收并校验调用方传入的数据；
     * 2. 按当前方法职责执行业务查询、转换或持久化处理；
     * 3. 返回处理结果或更新对象状态。</p>
     */
    public ResponseEntity<byte[]> exportShipments(@RequestBody(required = false) ShipmentExportRequest request) {
        return ExcelDownload.response("物流管理.xlsx", operationService.exportShipments(request));
    }

    @ApiOperationSupport(order = 70, author = "ratel / WenZhang / 18782945613")
    @Operation(summary = "新增物流单", description = "作者：ratel；开发人员：WenZhang；联系方式：18782945613。")
    @PreAuthorize("hasAuthority('LOGISTICS_MANAGE')")
    @PostMapping("/shipments")
    /**
     * 执行 createShipment 方法。
     * 
     * <p>实现步骤：
     * 1. 接收并校验调用方传入的数据；
     * 2. 按当前方法职责执行业务查询、转换或持久化处理；
     * 3. 返回处理结果或更新对象状态。</p>
     */
    public ApiResponse<ShipmentView> createShipment(@Valid @RequestBody ShipmentRequest request) {
        return ApiResponse.ok("物流单已创建", operationService.createShipment(request));
    }

    @ApiOperationSupport(order = 80, author = "ratel / WenZhang / 18782945613")
    @Operation(summary = "修改物流单", description = "作者：ratel；开发人员：WenZhang；联系方式：18782945613。")
    @PreAuthorize("hasAuthority('LOGISTICS_MANAGE')")
    @PutMapping("/shipments/{id}")
    /**
     * 执行 updateShipment 方法。
     * 
     * <p>实现步骤：
     * 1. 接收并校验调用方传入的数据；
     * 2. 按当前方法职责执行业务查询、转换或持久化处理；
     * 3. 返回处理结果或更新对象状态。</p>
     */
    public ApiResponse<ShipmentView> updateShipment(@PathVariable Long id, @Valid @RequestBody ShipmentRequest request) {
        return ApiResponse.ok("物流单已更新", operationService.updateShipment(id, request));
    }

    @ApiOperationSupport(order = 92, author = "ratel / WenZhang / 18782945613")
    @Operation(summary = "确认物流状态和最新物流信息", description = "作者：ratel；开发人员：WenZhang；联系方式：18782945613。状态确认时同步保存最新物流信息并写入物流操作流水。")
    @PreAuthorize("hasAuthority('LOGISTICS_MANAGE')")
    @PostMapping("/shipments/{id}/status-confirm")
    /**
     * 执行 confirmShipmentStatus 方法。
     * 
     * <p>实现步骤：
     * 1. 接收并校验调用方传入的数据；
     * 2. 按当前方法职责执行业务查询、转换或持久化处理；
     * 3. 返回处理结果或更新对象状态。</p>
     */
    public ApiResponse<ShipmentView> confirmShipmentStatus(
            @PathVariable Long id,
            @Valid @RequestBody ShipmentStatusConfirmRequest request
    ) {
        return ApiResponse.ok("物流单状态已确认", operationService.confirmShipmentStatus(id, request));
    }

    @ApiOperationSupport(order = 93, author = "ratel / WenZhang / 18782945613")
    @Operation(summary = "查询物流操作流水", description = "作者：ratel；开发人员：WenZhang；联系方式：18782945613。按操作时间倒序分页返回物流新增、修改和状态确认记录。")
    @PreAuthorize("hasAuthority('LOGISTICS_MANAGE') or hasAuthority('REPORT_VIEW')")
    @GetMapping("/shipments/{id}/operation-logs")
    /**
     * 执行 listShipmentOperationLogs 方法。
     * 
     * <p>实现步骤：
     * 1. 接收并校验调用方传入的数据；
     * 2. 按当前方法职责执行业务查询、转换或持久化处理；
     * 3. 返回处理结果或更新对象状态。</p>
     */
    public ApiResponse<ShipmentOperationLogPage> listShipmentOperationLogs(
            @PathVariable Long id,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) OffsetDateTime startTime,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) OffsetDateTime endTime,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        return ApiResponse.ok(operationService.pageShipmentOperationLogs(id, startTime, endTime, page, size));
    }

    @ApiOperationSupport(order = 95, author = "ratel / WenZhang / 18782945613")
    @Operation(summary = "批量删除物流单", description = "作者：ratel；开发人员：WenZhang；联系方式：18782945613。前端必须二次确认后调用。")
    @PreAuthorize("hasAuthority('LOGISTICS_MANAGE')")
    @PostMapping("/shipments/batch-delete")
    /**
     * 执行 deleteShipments 方法。
     * 
     * <p>实现步骤：
     * 1. 接收并校验调用方传入的数据；
     * 2. 按当前方法职责执行业务查询、转换或持久化处理；
     * 3. 返回处理结果或更新对象状态。</p>
     */
    public ApiResponse<Void> deleteShipments(@Valid @RequestBody BatchIdsRequest request) {
        operationService.deleteShipments(request.ids());
        return ApiResponse.ok("物流单已批量删除", null);
    }
}
