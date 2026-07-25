package com.ratel.fm.web.dto.operation;

import com.ratel.fm.common.validation.ChineseName;
import com.ratel.fm.common.validation.ContactPhone;
import com.ratel.fm.common.validation.VehicleNo;
import com.ratel.fm.domain.logistics.ShipmentStatus;
import com.ratel.fm.domain.purchase.PurchaseStatus;
import com.ratel.fm.web.dto.workflow.WorkflowDtos.WorkflowInstanceDetailView;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/**
 * 采购管理和物流管理接口 DTO。
 *
 * <p>实现目的：
 * 1. 将前端提交的业务表单映射为后端服务可校验的请求对象；
 * 2. 在 DTO 层统一声明字段含义、最大长度和格式要求；
 * 3. 将实体转换后的视图字段完整返回给列表、表单和查看流水页面使用。</p>
 */
public final class OperationDtos {

    private OperationDtos() {
    }

    @Schema(description = "采购明细请求。作者：ratel；开发人员：WenZhang；联系方式：18782945613。")
    /**
     * PurchaseLineRequest 数据传输记录。
     * 
     * <p>用于承载 PurchaseLineRequest 相关的业务数据、接口契约或处理逻辑，便于维护人员快速定位模块职责。</p>
     */
    public record PurchaseLineRequest(
            @Schema(description = "物料编码，来自基础信息物料字典的 code。")
            @NotBlank(message = "物料编码不能为空")
            @Size(max = 80, message = "物料编码长度不能超过80个字符")
            /**
             * 记录组件 itemCode：表示接口入参或出参中的 itemCode 字段。
             */
            String itemCode,
            @Schema(description = "物料名称，来自基础信息物料字典的名称快照。")
            @NotBlank(message = "物料名称不能为空")
            @Size(max = 160, message = "物料名称长度不能超过160个字符")
            /**
             * 记录组件 itemName：表示接口入参或出参中的 itemName 字段。
             */
            String itemName,
            @Schema(description = "规格型号，保存物料基础资料快照。")
            @Size(max = 160, message = "规格型号长度不能超过160个字符")
            /**
             * 记录组件 specification：表示接口入参或出参中的 specification 字段。
             */
            String specification,
            @Schema(description = "计量单位。")
            @Size(max = 60, message = "计量单位长度不能超过60个字符")
            /**
             * 记录组件 unitName：表示接口入参或出参中的 unitName 字段。
             */
            String unitName,
            @Schema(description = "采购数量，必须大于 0。")
            @Positive BigDecimal quantity,
            @Schema(description = "采购单价，必须大于 0。")
            @Positive BigDecimal unitPrice,
            @Schema(description = "税率，小数形式，如 0.13 表示 13%。")
            /**
             * 记录组件 taxRate：表示接口入参或出参中的 taxRate 字段。
             */
            BigDecimal taxRate,
            @Schema(description = "计划到货日期。")
            /**
             * 记录组件 plannedArrivalDate：表示接口入参或出参中的 plannedArrivalDate 字段。
             */
            LocalDate plannedArrivalDate,
            @Schema(description = "收货仓库。")
            @Size(max = 120, message = "收货仓库长度不能超过120个字符")
            /**
             * 记录组件 receiveWarehouse：表示接口入参或出参中的 receiveWarehouse 字段。
             */
            String receiveWarehouse,
            @Schema(description = "明细币种编码，来自基础信息币种字典；为空时默认人民币 CNY。")
            @Size(max = 20, message = "币种编码长度不能超过20个字符")
            /**
             * 记录组件 currencyCode：表示接口入参或出参中的 currencyCode 字段。
             */
            String currencyCode,
            @Schema(description = "明细币种名称快照；服务端会优先按字典编码解析。")
            @Size(max = 80, message = "币种名称长度不能超过80个字符")
            /**
             * 记录组件 currencyName：表示接口入参或出参中的 currencyName 字段。
             */
            String currencyName,
            @Schema(description = "采购发生时该币种折人民币汇率；人民币固定为 1，非人民币必填。")
            @Positive(message = "汇率必须大于0")
            /**
             * 记录组件 exchangeRateToCny：表示接口入参或出参中的 exchangeRateToCny 字段。
             */
            BigDecimal exchangeRateToCny
    ) {
    }

    @Schema(description = "采购单请求。作者：ratel；开发人员：WenZhang；联系方式：18782945613。")
    /**
     * PurchaseOrderRequest 数据传输记录。
     * 
     * <p>用于承载 PurchaseOrderRequest 相关的业务数据、接口契约或处理逻辑，便于维护人员快速定位模块职责。</p>
     */
    public record PurchaseOrderRequest(
            @Schema(description = "供应商名称。")
            @NotBlank(message = "供应商名称不能为空")
            @Size(max = 160, message = "供应商名称长度不能超过160个字符")
            /**
             * 记录组件 supplierName：表示接口入参或出参中的 supplierName 字段。
             */
            String supplierName,
            @Schema(description = "单据类型，如标准采购订单。")
            @Size(max = 80, message = "单据类型长度不能超过80个字符")
            /**
             * 记录组件 documentType：表示接口入参或出参中的 documentType 字段。
             */
            String documentType,
            @Schema(description = "业务类型，如标准采购。")
            @Size(max = 80, message = "业务类型长度不能超过80个字符")
            /**
             * 记录组件 businessType：表示接口入参或出参中的 businessType 字段。
             */
            String businessType,
            @Schema(description = "项目字典编码，来自基础信息项目字典。")
            @NotBlank(message = "项目不能为空")
            @Size(max = 80, message = "项目编码长度不能超过80个字符")
            /**
             * 记录组件 projectCode：表示接口入参或出参中的 projectCode 字段。
             */
            String projectCode,
            @Schema(description = "项目名称快照，随项目编码一并提交。")
            @Size(max = 160, message = "项目名称长度不能超过160个字符")
            /**
             * 记录组件 projectName：表示接口入参或出参中的 projectName 字段。
             */
            String projectName,
            @Schema(description = "采购组织。")
            @Size(max = 120, message = "采购组织长度不能超过120个字符")
            /**
             * 记录组件 purchaseOrganization：表示接口入参或出参中的 purchaseOrganization 字段。
             */
            String purchaseOrganization,
            @Schema(description = "采购部门。")
            @Size(max = 120, message = "采购部门长度不能超过120个字符")
            /**
             * 记录组件 purchaseDepartment：表示接口入参或出参中的 purchaseDepartment 字段。
             */
            String purchaseDepartment,
            @Schema(description = "采购员。")
            @Size(max = 120, message = "采购员长度不能超过120个字符")
            /**
             * 记录组件 purchaserName：表示接口入参或出参中的 purchaserName 字段。
             */
            String purchaserName,
            @Schema(description = "结算组织。")
            @Size(max = 120, message = "结算组织长度不能超过120个字符")
            /**
             * 记录组件 settlementOrganization：表示接口入参或出参中的 settlementOrganization 字段。
             */
            String settlementOrganization,
            @Schema(description = "付款条件。")
            @Size(max = 120, message = "付款条件长度不能超过120个字符")
            /**
             * 记录组件 paymentTerms：表示接口入参或出参中的 paymentTerms 字段。
             */
            String paymentTerms,
            @Schema(description = "结算方式。")
            @Size(max = 120, message = "结算方式长度不能超过120个字符")
            /**
             * 记录组件 settlementMethod：表示接口入参或出参中的 settlementMethod 字段。
             */
            String settlementMethod,
            @Schema(description = "交货条件。")
            @Size(max = 120, message = "交货条件长度不能超过120个字符")
            /**
             * 记录组件 deliveryTerms：表示接口入参或出参中的 deliveryTerms 字段。
             */
            String deliveryTerms,
            @Schema(description = "来源单据类型。")
            @Size(max = 80, message = "来源单据类型长度不能超过80个字符")
            /**
             * 记录组件 sourceBillType：表示接口入参或出参中的 sourceBillType 字段。
             */
            String sourceBillType,
            @Schema(description = "来源单据编号。")
            @Size(max = 300, message = "来源单据编号长度不能超过300个中文字符")
            /**
             * 记录组件 sourceBillNo：表示接口入参或出参中的 sourceBillNo 字段。
             */
            String sourceBillNo,
            @Schema(description = "采购日期。")
            @NotNull LocalDate orderDate,
            @Schema(description = "采购备注。")
            @Size(max = 2000, message = "采购备注长度不能超过2000个中文字符")
            /**
             * 记录组件 remark：表示接口入参或出参中的 remark 字段。
             */
            String remark,
            @Schema(description = "币种编码，来自基础信息币种字典；为空时默认人民币 CNY。")
            @Size(max = 20, message = "币种编码长度不能超过20个字符")
            /**
             * 记录组件 currencyCode：表示接口入参或出参中的 currencyCode 字段。
             */
            String currencyCode,
            @Schema(description = "币种名称快照；服务端会优先按字典编码解析。")
            @Size(max = 80, message = "币种名称长度不能超过80个字符")
            /**
             * 记录组件 currencyName：表示接口入参或出参中的 currencyName 字段。
             */
            String currencyName,
            @Schema(description = "采购发生时该币种折人民币汇率；人民币固定为 1，非人民币必填。")
            @Positive(message = "汇率必须大于0")
            /**
             * 记录组件 exchangeRateToCny：表示接口入参或出参中的 exchangeRateToCny 字段。
             */
            BigDecimal exchangeRateToCny,
            @Schema(description = "采购明细集合，系统按数量和单价自动计算金额。")
            @Valid @NotEmpty List<PurchaseLineRequest> lines
    ) {
    }

    /**
     * PurchaseLineView 数据传输记录。
     * 
     * <p>用于承载 PurchaseLineView 相关的业务数据、接口契约或处理逻辑，便于维护人员快速定位模块职责。</p>
     */
    public record PurchaseLineView(
            /**
             * 记录组件 id：表示接口入参或出参中的 id 字段。
             */
            Long id,
            /**
             * 记录组件 lineNo：表示接口入参或出参中的 lineNo 字段。
             */
            int lineNo,
            /**
             * 记录组件 itemCode：表示接口入参或出参中的 itemCode 字段。
             */
            String itemCode,
            /**
             * 记录组件 itemName：表示接口入参或出参中的 itemName 字段。
             */
            String itemName,
            /**
             * 记录组件 specification：表示接口入参或出参中的 specification 字段。
             */
            String specification,
            /**
             * 记录组件 unitName：表示接口入参或出参中的 unitName 字段。
             */
            String unitName,
            /**
             * 记录组件 quantity：表示接口入参或出参中的 quantity 字段。
             */
            BigDecimal quantity,
            /**
             * 记录组件 unitPrice：表示接口入参或出参中的 unitPrice 字段。
             */
            BigDecimal unitPrice,
            /**
             * 记录组件 amount：表示接口入参或出参中的 amount 字段。
             */
            BigDecimal amount,
            /**
             * 记录组件 taxRate：表示接口入参或出参中的 taxRate 字段。
             */
            BigDecimal taxRate,
            /**
             * 记录组件 taxAmount：表示接口入参或出参中的 taxAmount 字段。
             */
            BigDecimal taxAmount,
            /**
             * 记录组件 amountWithTax：表示接口入参或出参中的 amountWithTax 字段。
             */
            BigDecimal amountWithTax,
            /**
             * 记录组件 plannedArrivalDate：表示接口入参或出参中的 plannedArrivalDate 字段。
             */
            LocalDate plannedArrivalDate,
            /**
             * 记录组件 receiveWarehouse：表示接口入参或出参中的 receiveWarehouse 字段。
             */
            String receiveWarehouse,
            /**
             * 记录组件 currencyCode：表示接口入参或出参中的 currencyCode 字段。
             */
            String currencyCode,
            /**
             * 记录组件 currencyName：表示接口入参或出参中的 currencyName 字段。
             */
            String currencyName,
            /**
             * 记录组件 exchangeRateToCny：表示接口入参或出参中的 exchangeRateToCny 字段。
             */
            BigDecimal exchangeRateToCny,
            /**
             * 记录组件 unitPriceCny：表示接口入参或出参中的 unitPriceCny 字段。
             */
            BigDecimal unitPriceCny,
            /**
             * 记录组件 amountCny：表示接口入参或出参中的 amountCny 字段。
             */
            BigDecimal amountCny
    ) {
    }

    /**
     * PurchaseOrderView 数据传输记录。
     * 
     * <p>用于承载 PurchaseOrderView 相关的业务数据、接口契约或处理逻辑，便于维护人员快速定位模块职责。</p>
     */
    public record PurchaseOrderView(
            /**
             * 记录组件 id：表示接口入参或出参中的 id 字段。
             */
            Long id,
            /**
             * 记录组件 orderNo：表示接口入参或出参中的 orderNo 字段。
             */
            String orderNo,
            /**
             * 记录组件 supplierName：表示接口入参或出参中的 supplierName 字段。
             */
            String supplierName,
            /**
             * 记录组件 documentType：表示接口入参或出参中的 documentType 字段。
             */
            String documentType,
            /**
             * 记录组件 businessType：表示接口入参或出参中的 businessType 字段。
             */
            String businessType,
            /**
             * 记录组件 projectCode：表示接口入参或出参中的 projectCode 字段。
             */
            String projectCode,
            /**
             * 记录组件 projectName：表示接口入参或出参中的 projectName 字段。
             */
            String projectName,
            /**
             * 记录组件 purchaseOrganization：表示接口入参或出参中的 purchaseOrganization 字段。
             */
            String purchaseOrganization,
            /**
             * 记录组件 purchaseDepartment：表示接口入参或出参中的 purchaseDepartment 字段。
             */
            String purchaseDepartment,
            /**
             * 记录组件 purchaserName：表示接口入参或出参中的 purchaserName 字段。
             */
            String purchaserName,
            /**
             * 记录组件 settlementOrganization：表示接口入参或出参中的 settlementOrganization 字段。
             */
            String settlementOrganization,
            /**
             * 记录组件 paymentTerms：表示接口入参或出参中的 paymentTerms 字段。
             */
            String paymentTerms,
            /**
             * 记录组件 settlementMethod：表示接口入参或出参中的 settlementMethod 字段。
             */
            String settlementMethod,
            /**
             * 记录组件 deliveryTerms：表示接口入参或出参中的 deliveryTerms 字段。
             */
            String deliveryTerms,
            /**
             * 记录组件 sourceBillType：表示接口入参或出参中的 sourceBillType 字段。
             */
            String sourceBillType,
            /**
             * 记录组件 sourceBillNo：表示接口入参或出参中的 sourceBillNo 字段。
             */
            String sourceBillNo,
            /**
             * 记录组件 orderDate：表示接口入参或出参中的 orderDate 字段。
             */
            LocalDate orderDate,
            /**
             * 记录组件 status：表示接口入参或出参中的 status 字段。
             */
            PurchaseStatus status,
            /**
             * 记录组件 totalAmount：表示接口入参或出参中的 totalAmount 字段。
             */
            BigDecimal totalAmount,
            /**
             * 记录组件 currencyCode：表示接口入参或出参中的 currencyCode 字段。
             */
            String currencyCode,
            /**
             * 记录组件 currencyName：表示接口入参或出参中的 currencyName 字段。
             */
            String currencyName,
            /**
             * 记录组件 exchangeRateToCny：表示接口入参或出参中的 exchangeRateToCny 字段。
             */
            BigDecimal exchangeRateToCny,
            /**
             * 记录组件 totalAmountCny：表示接口入参或出参中的 totalAmountCny 字段。
             */
            BigDecimal totalAmountCny,
            /**
             * 记录组件 createdBy：表示接口入参或出参中的 createdBy 字段。
             */
            String createdBy,
            /**
             * 记录组件 remark：表示接口入参或出参中的 remark 字段。
             */
            String remark,
            /**
             * 记录组件 cancelType：表示取消采购类型名称。
             */
            String cancelType,
            /**
             * 记录组件 cancelReason：表示取消采购原因。
             */
            String cancelReason,
            /**
             * 记录组件 voucherId：表示采购单通过会计平台生成的凭证主键。
             */
            Long voucherId,
            /**
             * 记录组件 voucherNo：表示采购单通过会计平台生成的凭证号。
             */
            String voucherNo,
            /**
             * 记录组件 workflow：表示该采购单最近一次审批流程详情。
             */
            WorkflowInstanceDetailView workflow,
            /**
             * 记录组件 lines：表示接口入参或出参中的 lines 字段。
             */
            List<PurchaseLineView> lines
    ) {
    }

    @Schema(description = "采购单提交审批请求。作者：ratel；开发人员：WenZhang；联系方式：18782945613。")
    public record PurchaseApprovalSubmitRequest(
            @Schema(description = "申请理由。")
            @Size(max = 2000, message = "申请理由长度不能超过2000个中文字符")
            String applyReason
    ) {
    }

    @Schema(description = "取消采购请求。作者：ratel；开发人员：WenZhang；联系方式：18782945613。")
    public record PurchaseCancelRequest(
            @Schema(description = "取消类型，来自取消类型字典。")
            @NotBlank(message = "取消类型不能为空")
            @Size(max = 120, message = "取消类型长度不能超过120个字符")
            String cancelType,
            @Schema(description = "取消原因。")
            @NotBlank(message = "取消原因不能为空")
            @Size(max = 2000, message = "取消原因长度不能超过2000个中文字符")
            String cancelReason
    ) {
    }

    @Schema(description = "采购单列表导出请求。作者：ratel；开发人员：WenZhang；联系方式：18782945613。")
    /**
     * PurchaseOrderExportRequest 数据传输记录。
     * 
     * <p>用于承载 PurchaseOrderExportRequest 相关的业务数据、接口契约或处理逻辑，便于维护人员快速定位模块职责。</p>
     */
    public record PurchaseOrderExportRequest(
            @Schema(description = "选中的采购单 ID。为空时按筛选条件导出。")
            /**
             * 记录组件 ids：表示接口入参或出参中的 ids 字段。
             */
            List<Long> ids,
            @Schema(description = "采购开始日期，格式 yyyy-MM-dd。")
            /**
             * 记录组件 startDate：表示接口入参或出参中的 startDate 字段。
             */
            LocalDate startDate,
            @Schema(description = "采购结束日期，格式 yyyy-MM-dd。")
            /**
             * 记录组件 endDate：表示接口入参或出参中的 endDate 字段。
             */
            LocalDate endDate,
            @Schema(description = "采购单号，包含匹配。")
            /**
             * 记录组件 orderNo：表示接口入参或出参中的 orderNo 字段。
             */
            String orderNo,
            @Schema(description = "供应商名称，等值匹配。")
            /**
             * 记录组件 supplierName：表示接口入参或出参中的 supplierName 字段。
             */
            String supplierName,
            @Schema(description = "项目字典编码，等值匹配。")
            /**
             * 记录组件 projectCode：表示接口入参或出参中的 projectCode 字段。
             */
            String projectCode,
            @Schema(description = "采购状态，等值匹配。")
            /**
             * 记录组件 status：表示接口入参或出参中的 status 字段。
             */
            PurchaseStatus status,
            @Schema(description = "创建人，包含匹配。")
            /**
             * 记录组件 createdBy：表示接口入参或出参中的 createdBy 字段。
             */
            String createdBy,
            @Schema(description = "备注，包含匹配。")
            /**
             * 记录组件 remark：表示接口入参或出参中的 remark 字段。
             */
            String remark
    ) {
    }

    @Schema(description = "物流单请求。作者：ratel；开发人员：WenZhang；联系方式：18782945613。")
    /**
     * ShipmentRequest 数据传输记录。
     * 
     * <p>用于承载 ShipmentRequest 相关的业务数据、接口契约或处理逻辑，便于维护人员快速定位模块职责。</p>
     */
    public record ShipmentRequest(
            @Schema(description = "关联业务单号，如采购单号。")
            @Size(max = 300, message = "关联业务单号长度不能超过300个中文字符")
            /**
             * 记录组件 relatedOrderNo：表示接口入参或出参中的 relatedOrderNo 字段。
             */
            String relatedOrderNo,
            @Schema(description = "单据类型，如采购发运、销售发运。")
            @Size(max = 80, message = "单据类型长度不能超过80个字符")
            /**
             * 记录组件 documentType：表示接口入参或出参中的 documentType 字段。
             */
            String documentType,
            @Schema(description = "项目字典编码，来自基础信息项目字典。")
            @NotBlank(message = "项目不能为空")
            @Size(max = 80, message = "项目编码长度不能超过80个字符")
            /**
             * 记录组件 projectCode：表示接口入参或出参中的 projectCode 字段。
             */
            String projectCode,
            @Schema(description = "项目名称快照，随项目编码一并提交。")
            @Size(max = 160, message = "项目名称长度不能超过160个字符")
            /**
             * 记录组件 projectName：表示接口入参或出参中的 projectName 字段。
             */
            String projectName,
            @Schema(description = "运输方式。")
            @Size(max = 80, message = "运输方式长度不能超过80个字符")
            /**
             * 记录组件 transportMode：表示接口入参或出参中的 transportMode 字段。
             */
            String transportMode,
            @Schema(description = "发运组织。")
            @Size(max = 120, message = "发运组织长度不能超过120个字符")
            /**
             * 记录组件 shippingOrganization：表示接口入参或出参中的 shippingOrganization 字段。
             */
            String shippingOrganization,
            @Schema(description = "收货组织。")
            @Size(max = 120, message = "收货组织长度不能超过120个字符")
            /**
             * 记录组件 receivingOrganization：表示接口入参或出参中的 receivingOrganization 字段。
             */
            String receivingOrganization,
            @Schema(description = "承运商名称。")
            @NotBlank(message = "承运商名称不能为空")
            @Size(max = 160, message = "承运商名称长度不能超过160个字符")
            /**
             * 记录组件 carrierName：表示接口入参或出参中的 carrierName 字段。
             */
            String carrierName,
            @Schema(description = "承运商运单号或跟踪号。")
            @Size(max = 120, message = "承运商运单号长度不能超过120个字符")
            /**
             * 记录组件 trackingNo：表示接口入参或出参中的 trackingNo 字段。
             */
            String trackingNo,
            @Schema(description = "司机姓名。")
            @ChineseName(message = "司机姓名必须为1到20个中文字符", groups = {}, payload = {})
            @Size(max = 20, message = "司机姓名不能超过20个中文字符")
            /**
             * 记录组件 driverName：表示接口入参或出参中的 driverName 字段。
             */
            String driverName,
            @Schema(description = "司机电话。")
            @ContactPhone(message = "司机电话必须为手机号或座机号", groups = {}, payload = {})
            @Size(max = 30, message = "司机电话长度不能超过30个字符")
            /**
             * 记录组件 driverPhone：表示接口入参或出参中的 driverPhone 字段。
             */
            String driverPhone,
            @Schema(description = "车牌号。")
            @VehicleNo(message = "车牌号格式不正确", groups = {}, payload = {})
            @Size(max = 12, message = "车牌号长度不能超过12个字符")
            /**
             * 记录组件 vehicleNo：表示接口入参或出参中的 vehicleNo 字段。
             */
            String vehicleNo,
            @Schema(description = "发货地行政区划编码级联路径，来自全国行政区划字典，格式如 110000/110100/110102。")
            @NotBlank(message = "发货地行政区划不能为空")
            @Size(max = 300, message = "发货地行政区划编码长度不能超过300个字符")
            /**
             * 记录组件 originDivisionCode：表示接口入参或出参中的 originDivisionCode 字段。
             */
            String originDivisionCode,
            @Schema(description = "发货地行政区划名称级联快照。")
            @NotBlank(message = "发货地行政区划名称不能为空")
            @Size(max = 300, message = "发货地行政区划名称长度不能超过300个字符")
            /**
             * 记录组件 originDivisionName：表示接口入参或出参中的 originDivisionName 字段。
             */
            String originDivisionName,
            @Schema(description = "目的地行政区划编码级联路径，来自全国行政区划字典。")
            @NotBlank(message = "目的地行政区划不能为空")
            @Size(max = 300, message = "目的地行政区划编码长度不能超过300个字符")
            /**
             * 记录组件 destinationDivisionCode：表示接口入参或出参中的 destinationDivisionCode 字段。
             */
            String destinationDivisionCode,
            @Schema(description = "目的地行政区划名称级联快照。")
            @NotBlank(message = "目的地行政区划名称不能为空")
            @Size(max = 300, message = "目的地行政区划名称长度不能超过300个字符")
            /**
             * 记录组件 destinationDivisionName：表示接口入参或出参中的 destinationDivisionName 字段。
             */
            String destinationDivisionName,
            @Schema(description = "发货地详址。")
            @NotBlank(message = "发货地详址不能为空")
            @Size(max = 300, message = "发货地详址长度不能超过300个中文字符")
            /**
             * 记录组件 origin：表示接口入参或出参中的 origin 字段。
             */
            String origin,
            @Schema(description = "目的地详址。")
            @NotBlank(message = "目的地详址不能为空")
            @Size(max = 300, message = "目的地详址长度不能超过300个中文字符")
            /**
             * 记录组件 destination：表示接口入参或出参中的 destination 字段。
             */
            String destination,
            @Schema(description = "计划发运日期。")
            @NotNull LocalDate plannedShipDate,
            @Schema(description = "物流备注。")
            @Size(max = 2000, message = "物流备注长度不能超过2000个中文字符")
            /**
             * 记录组件 remark：表示接口入参或出参中的 remark 字段。
             */
            String remark
    ) {
    }

    @Schema(description = "物流状态确认请求。作者：ratel；开发人员：WenZhang；联系方式：18782945613。")
    /**
     * ShipmentStatusConfirmRequest 数据传输记录。
     * 
     * <p>用于承载 ShipmentStatusConfirmRequest 相关的业务数据、接口契约或处理逻辑，便于维护人员快速定位模块职责。</p>
     */
    public record ShipmentStatusConfirmRequest(
            @Schema(description = "确认后的目标物流状态。")
            @NotNull(message = "物流状态不能为空")
            /**
             * 记录组件 status：表示接口入参或出参中的 status 字段。
             */
            ShipmentStatus status,
            @Schema(description = "关联业务单号，如采购单号。")
            @Size(max = 300, message = "关联业务单号长度不能超过300个中文字符")
            /**
             * 记录组件 relatedOrderNo：表示接口入参或出参中的 relatedOrderNo 字段。
             */
            String relatedOrderNo,
            @Schema(description = "单据类型，如采购发运、销售发运。")
            @Size(max = 80, message = "单据类型长度不能超过80个字符")
            /**
             * 记录组件 documentType：表示接口入参或出参中的 documentType 字段。
             */
            String documentType,
            @Schema(description = "项目字典编码，来自基础信息项目字典。")
            @NotBlank(message = "项目不能为空")
            @Size(max = 80, message = "项目编码长度不能超过80个字符")
            /**
             * 记录组件 projectCode：表示接口入参或出参中的 projectCode 字段。
             */
            String projectCode,
            @Schema(description = "项目名称快照，随项目编码一并提交。")
            @Size(max = 160, message = "项目名称长度不能超过160个字符")
            /**
             * 记录组件 projectName：表示接口入参或出参中的 projectName 字段。
             */
            String projectName,
            @Schema(description = "运输方式。")
            @Size(max = 80, message = "运输方式长度不能超过80个字符")
            /**
             * 记录组件 transportMode：表示接口入参或出参中的 transportMode 字段。
             */
            String transportMode,
            @Schema(description = "发运组织。")
            @Size(max = 120, message = "发运组织长度不能超过120个字符")
            /**
             * 记录组件 shippingOrganization：表示接口入参或出参中的 shippingOrganization 字段。
             */
            String shippingOrganization,
            @Schema(description = "收货组织。")
            @Size(max = 120, message = "收货组织长度不能超过120个字符")
            /**
             * 记录组件 receivingOrganization：表示接口入参或出参中的 receivingOrganization 字段。
             */
            String receivingOrganization,
            @Schema(description = "承运商名称。")
            @NotBlank(message = "承运商名称不能为空")
            @Size(max = 160, message = "承运商名称长度不能超过160个字符")
            /**
             * 记录组件 carrierName：表示接口入参或出参中的 carrierName 字段。
             */
            String carrierName,
            @Schema(description = "承运商运单号或跟踪号。")
            @Size(max = 120, message = "承运商运单号长度不能超过120个字符")
            /**
             * 记录组件 trackingNo：表示接口入参或出参中的 trackingNo 字段。
             */
            String trackingNo,
            @Schema(description = "司机姓名。")
            @ChineseName(message = "司机姓名必须为1到20个中文字符", groups = {}, payload = {})
            @Size(max = 20, message = "司机姓名不能超过20个中文字符")
            /**
             * 记录组件 driverName：表示接口入参或出参中的 driverName 字段。
             */
            String driverName,
            @Schema(description = "司机电话。")
            @ContactPhone(message = "司机电话必须为手机号或座机号", groups = {}, payload = {})
            @Size(max = 30, message = "司机电话长度不能超过30个字符")
            /**
             * 记录组件 driverPhone：表示接口入参或出参中的 driverPhone 字段。
             */
            String driverPhone,
            @Schema(description = "车牌号。")
            @VehicleNo(message = "车牌号格式不正确", groups = {}, payload = {})
            @Size(max = 12, message = "车牌号长度不能超过12个字符")
            /**
             * 记录组件 vehicleNo：表示接口入参或出参中的 vehicleNo 字段。
             */
            String vehicleNo,
            @Schema(description = "发货地行政区划编码级联路径，来自全国行政区划字典。")
            @NotBlank(message = "发货地行政区划不能为空")
            @Size(max = 300, message = "发货地行政区划编码长度不能超过300个字符")
            /**
             * 记录组件 originDivisionCode：表示接口入参或出参中的 originDivisionCode 字段。
             */
            String originDivisionCode,
            @Schema(description = "发货地行政区划名称级联快照。")
            @NotBlank(message = "发货地行政区划名称不能为空")
            @Size(max = 300, message = "发货地行政区划名称长度不能超过300个字符")
            /**
             * 记录组件 originDivisionName：表示接口入参或出参中的 originDivisionName 字段。
             */
            String originDivisionName,
            @Schema(description = "目的地行政区划编码级联路径，来自全国行政区划字典。")
            @NotBlank(message = "目的地行政区划不能为空")
            @Size(max = 300, message = "目的地行政区划编码长度不能超过300个字符")
            /**
             * 记录组件 destinationDivisionCode：表示接口入参或出参中的 destinationDivisionCode 字段。
             */
            String destinationDivisionCode,
            @Schema(description = "目的地行政区划名称级联快照。")
            @NotBlank(message = "目的地行政区划名称不能为空")
            @Size(max = 300, message = "目的地行政区划名称长度不能超过300个字符")
            /**
             * 记录组件 destinationDivisionName：表示接口入参或出参中的 destinationDivisionName 字段。
             */
            String destinationDivisionName,
            @Schema(description = "发货地详址。")
            @NotBlank(message = "发货地详址不能为空")
            @Size(max = 300, message = "发货地详址长度不能超过300个中文字符")
            /**
             * 记录组件 origin：表示接口入参或出参中的 origin 字段。
             */
            String origin,
            @Schema(description = "目的地详址。")
            @NotBlank(message = "目的地详址不能为空")
            @Size(max = 300, message = "目的地详址长度不能超过300个中文字符")
            /**
             * 记录组件 destination：表示接口入参或出参中的 destination 字段。
             */
            String destination,
            @Schema(description = "计划发运日期。")
            @NotNull(message = "计划发运日期不能为空")
            /**
             * 记录组件 plannedShipDate：表示接口入参或出参中的 plannedShipDate 字段。
             */
            LocalDate plannedShipDate,
            @Schema(description = "实际发运日期。")
            /**
             * 记录组件 actualShipDate：表示接口入参或出参中的 actualShipDate 字段。
             */
            LocalDate actualShipDate,
            @Schema(description = "实际送达日期。")
            /**
             * 记录组件 deliveredDate：表示接口入参或出参中的 deliveredDate 字段。
             */
            LocalDate deliveredDate,
            @Schema(description = "物流备注。")
            @Size(max = 2000, message = "物流备注长度不能超过2000个中文字符")
            /**
             * 记录组件 remark：表示接口入参或出参中的 remark 字段。
             */
            String remark,
            @Schema(description = "本次状态确认说明。")
            @Size(max = 2000, message = "状态确认说明长度不能超过2000个中文字符")
            /**
             * 记录组件 operationRemark：表示接口入参或出参中的 operationRemark 字段。
             */
            String operationRemark
    ) {
    }

    /**
     * ShipmentView 数据传输记录。
     * 
     * <p>用于承载 ShipmentView 相关的业务数据、接口契约或处理逻辑，便于维护人员快速定位模块职责。</p>
     */
    public record ShipmentView(
            /**
             * 记录组件 id：表示接口入参或出参中的 id 字段。
             */
            Long id,
            /**
             * 记录组件 shipmentNo：表示接口入参或出参中的 shipmentNo 字段。
             */
            String shipmentNo,
            /**
             * 记录组件 relatedOrderNo：表示接口入参或出参中的 relatedOrderNo 字段。
             */
            String relatedOrderNo,
            /**
             * 记录组件 documentType：表示接口入参或出参中的 documentType 字段。
             */
            String documentType,
            /**
             * 记录组件 projectCode：表示接口入参或出参中的 projectCode 字段。
             */
            String projectCode,
            /**
             * 记录组件 projectName：表示接口入参或出参中的 projectName 字段。
             */
            String projectName,
            /**
             * 记录组件 transportMode：表示接口入参或出参中的 transportMode 字段。
             */
            String transportMode,
            /**
             * 记录组件 shippingOrganization：表示接口入参或出参中的 shippingOrganization 字段。
             */
            String shippingOrganization,
            /**
             * 记录组件 receivingOrganization：表示接口入参或出参中的 receivingOrganization 字段。
             */
            String receivingOrganization,
            /**
             * 记录组件 carrierName：表示接口入参或出参中的 carrierName 字段。
             */
            String carrierName,
            /**
             * 记录组件 trackingNo：表示接口入参或出参中的 trackingNo 字段。
             */
            String trackingNo,
            /**
             * 记录组件 driverName：表示接口入参或出参中的 driverName 字段。
             */
            String driverName,
            /**
             * 记录组件 driverPhone：表示接口入参或出参中的 driverPhone 字段。
             */
            String driverPhone,
            /**
             * 记录组件 vehicleNo：表示接口入参或出参中的 vehicleNo 字段。
             */
            String vehicleNo,
            /**
             * 记录组件 originDivisionCode：表示接口入参或出参中的 originDivisionCode 字段。
             */
            String originDivisionCode,
            /**
             * 记录组件 originDivisionName：表示接口入参或出参中的 originDivisionName 字段。
             */
            String originDivisionName,
            /**
             * 记录组件 destinationDivisionCode：表示接口入参或出参中的 destinationDivisionCode 字段。
             */
            String destinationDivisionCode,
            /**
             * 记录组件 destinationDivisionName：表示接口入参或出参中的 destinationDivisionName 字段。
             */
            String destinationDivisionName,
            /**
             * 记录组件 origin：表示接口入参或出参中的 origin 字段。
             */
            String origin,
            /**
             * 记录组件 destination：表示接口入参或出参中的 destination 字段。
             */
            String destination,
            /**
             * 记录组件 plannedShipDate：表示接口入参或出参中的 plannedShipDate 字段。
             */
            LocalDate plannedShipDate,
            /**
             * 记录组件 actualShipDate：表示接口入参或出参中的 actualShipDate 字段。
             */
            LocalDate actualShipDate,
            /**
             * 记录组件 deliveredDate：表示接口入参或出参中的 deliveredDate 字段。
             */
            LocalDate deliveredDate,
            /**
             * 记录组件 status：表示接口入参或出参中的 status 字段。
             */
            ShipmentStatus status,
            /**
             * 记录组件 remark：表示接口入参或出参中的 remark 字段。
             */
            String remark
    ) {
    }

    /**
     * ShipmentOperationLogView 数据传输记录。
     * 
     * <p>用于承载 ShipmentOperationLogView 相关的业务数据、接口契约或处理逻辑，便于维护人员快速定位模块职责。</p>
     */
    public record ShipmentOperationLogView(
            /**
             * 记录组件 id：表示接口入参或出参中的 id 字段。
             */
            Long id,
            /**
             * 记录组件 shipmentNo：表示接口入参或出参中的 shipmentNo 字段。
             */
            String shipmentNo,
            /**
             * 记录组件 fromStatus：表示接口入参或出参中的 fromStatus 字段。
             */
            ShipmentStatus fromStatus,
            /**
             * 记录组件 toStatus：表示接口入参或出参中的 toStatus 字段。
             */
            ShipmentStatus toStatus,
            /**
             * 记录组件 relatedOrderNo：表示接口入参或出参中的 relatedOrderNo 字段。
             */
            String relatedOrderNo,
            /**
             * 记录组件 documentType：表示接口入参或出参中的 documentType 字段。
             */
            String documentType,
            /**
             * 记录组件 projectCode：表示接口入参或出参中的 projectCode 字段。
             */
            String projectCode,
            /**
             * 记录组件 projectName：表示接口入参或出参中的 projectName 字段。
             */
            String projectName,
            /**
             * 记录组件 transportMode：表示接口入参或出参中的 transportMode 字段。
             */
            String transportMode,
            /**
             * 记录组件 shippingOrganization：表示接口入参或出参中的 shippingOrganization 字段。
             */
            String shippingOrganization,
            /**
             * 记录组件 receivingOrganization：表示接口入参或出参中的 receivingOrganization 字段。
             */
            String receivingOrganization,
            /**
             * 记录组件 carrierName：表示接口入参或出参中的 carrierName 字段。
             */
            String carrierName,
            /**
             * 记录组件 trackingNo：表示接口入参或出参中的 trackingNo 字段。
             */
            String trackingNo,
            /**
             * 记录组件 driverName：表示接口入参或出参中的 driverName 字段。
             */
            String driverName,
            /**
             * 记录组件 driverPhone：表示接口入参或出参中的 driverPhone 字段。
             */
            String driverPhone,
            /**
             * 记录组件 vehicleNo：表示接口入参或出参中的 vehicleNo 字段。
             */
            String vehicleNo,
            /**
             * 记录组件 originDivisionCode：表示接口入参或出参中的 originDivisionCode 字段。
             */
            String originDivisionCode,
            /**
             * 记录组件 originDivisionName：表示接口入参或出参中的 originDivisionName 字段。
             */
            String originDivisionName,
            /**
             * 记录组件 destinationDivisionCode：表示接口入参或出参中的 destinationDivisionCode 字段。
             */
            String destinationDivisionCode,
            /**
             * 记录组件 destinationDivisionName：表示接口入参或出参中的 destinationDivisionName 字段。
             */
            String destinationDivisionName,
            /**
             * 记录组件 origin：表示接口入参或出参中的 origin 字段。
             */
            String origin,
            /**
             * 记录组件 destination：表示接口入参或出参中的 destination 字段。
             */
            String destination,
            /**
             * 记录组件 plannedShipDate：表示接口入参或出参中的 plannedShipDate 字段。
             */
            LocalDate plannedShipDate,
            /**
             * 记录组件 actualShipDate：表示接口入参或出参中的 actualShipDate 字段。
             */
            LocalDate actualShipDate,
            /**
             * 记录组件 deliveredDate：表示接口入参或出参中的 deliveredDate 字段。
             */
            LocalDate deliveredDate,
            /**
             * 记录组件 remark：表示接口入参或出参中的 remark 字段。
             */
            String remark,
            /**
             * 记录组件 operationRemark：表示接口入参或出参中的 operationRemark 字段。
             */
            String operationRemark,
            /**
             * 记录组件 operatorId：表示接口入参或出参中的 operatorId 字段。
             */
            Long operatorId,
            /**
             * 记录组件 operatorUsername：表示接口入参或出参中的 operatorUsername 字段。
             */
            String operatorUsername,
            /**
             * 记录组件 operatorName：表示接口入参或出参中的 operatorName 字段。
             */
            String operatorName,
            /**
             * 记录组件 operationTime：表示接口入参或出参中的 operationTime 字段。
             */
            String operationTime
    ) {
    }

    @Schema(description = "物流操作流水分页结果。作者：ratel；开发人员：WenZhang；联系方式：18782945613。")
    /**
     * ShipmentOperationLogPage 数据传输记录。
     * 
     * <p>用于承载 ShipmentOperationLogPage 相关的业务数据、接口契约或处理逻辑，便于维护人员快速定位模块职责。</p>
     */
    public record ShipmentOperationLogPage(
            @Schema(description = "当前页物流操作流水。")
            /**
             * 记录组件 rows：表示接口入参或出参中的 rows 字段。
             */
            List<ShipmentOperationLogView> rows,
            @Schema(description = "符合查询条件的流水总数。")
            /**
             * 记录组件 total：表示接口入参或出参中的 total 字段。
             */
            long total
    ) {
    }

    @Schema(description = "物流单列表导出请求。作者：ratel；开发人员：WenZhang；联系方式：18782945613。")
    /**
     * ShipmentExportRequest 数据传输记录。
     * 
     * <p>用于承载 ShipmentExportRequest 相关的业务数据、接口契约或处理逻辑，便于维护人员快速定位模块职责。</p>
     */
    public record ShipmentExportRequest(
            @Schema(description = "选中的物流单 ID。为空时按筛选条件导出。")
            /**
             * 记录组件 ids：表示接口入参或出参中的 ids 字段。
             */
            List<Long> ids,
            @Schema(description = "计划发运开始日期，格式 yyyy-MM-dd。")
            /**
             * 记录组件 startDate：表示接口入参或出参中的 startDate 字段。
             */
            LocalDate startDate,
            @Schema(description = "计划发运结束日期，格式 yyyy-MM-dd。")
            /**
             * 记录组件 endDate：表示接口入参或出参中的 endDate 字段。
             */
            LocalDate endDate,
            @Schema(description = "物流单号，包含匹配。")
            /**
             * 记录组件 shipmentNo：表示接口入参或出参中的 shipmentNo 字段。
             */
            String shipmentNo,
            @Schema(description = "关联单号，包含匹配。")
            /**
             * 记录组件 relatedOrderNo：表示接口入参或出参中的 relatedOrderNo 字段。
             */
            String relatedOrderNo,
            @Schema(description = "项目字典编码，等值匹配。")
            /**
             * 记录组件 projectCode：表示接口入参或出参中的 projectCode 字段。
             */
            String projectCode,
            @Schema(description = "承运商名称，等值匹配。")
            /**
             * 记录组件 carrierName：表示接口入参或出参中的 carrierName 字段。
             */
            String carrierName,
            @Schema(description = "运单号，包含匹配。")
            /**
             * 记录组件 trackingNo：表示接口入参或出参中的 trackingNo 字段。
             */
            String trackingNo,
            @Schema(description = "发货地行政区划编码级联路径集合，逗号分隔；服务端按右 like 匹配下级区划。")
            /**
             * 记录组件 originDivisionCodes：表示接口入参或出参中的 originDivisionCodes 字段。
             */
            String originDivisionCodes,
            @Schema(description = "目的地行政区划编码级联路径集合，逗号分隔；服务端按右 like 匹配下级区划。")
            /**
             * 记录组件 destinationDivisionCodes：表示接口入参或出参中的 destinationDivisionCodes 字段。
             */
            String destinationDivisionCodes,
            @Schema(description = "发货地详址，包含匹配。")
            /**
             * 记录组件 origin：表示接口入参或出参中的 origin 字段。
             */
            String origin,
            @Schema(description = "目的地详址，包含匹配。")
            /**
             * 记录组件 destination：表示接口入参或出参中的 destination 字段。
             */
            String destination,
            @Schema(description = "物流状态，等值匹配。")
            /**
             * 记录组件 status：表示接口入参或出参中的 status 字段。
             */
            ShipmentStatus status
    ) {
    }
}
