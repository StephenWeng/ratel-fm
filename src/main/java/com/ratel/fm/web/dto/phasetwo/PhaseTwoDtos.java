package com.ratel.fm.web.dto.phasetwo;

import com.ratel.fm.domain.inventory.InventoryMovementType;
import com.ratel.fm.domain.receivable.ArApStatus;
import com.ratel.fm.domain.receivable.ArApType;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/**
 * 二期库存、应收应付、报表和 AI 请求 DTO。
 *
 * <p>实现目的：
 * 1. 统一库存流水和应收应付单据的入参校验；
 * 2. 将新增的组织、单据类型、来源单据、物料快照等字段返回给查看流水和智能检索；
 * 3. 在接口边界控制备注说明、来源单号等长文本字段，避免脏数据进入业务表。</p>
 */
public final class PhaseTwoDtos {

    private PhaseTwoDtos() {
    }

    @Schema(description = "库存流水请求。作者：ratel；开发人员：WenZhang；联系方式：18782945613。")
    /**
     * InventoryRequest 数据传输记录。
     * 
     * <p>用于承载 InventoryRequest 相关的业务数据、接口契约或处理逻辑，便于维护人员快速定位模块职责。</p>
     */
    public record InventoryRequest(
            @Schema(description = "库存变动类型：入库、出库、调拨、盘点。")
            @NotNull InventoryMovementType movementType,
            @Schema(description = "库存变动日期。")
            @NotNull LocalDate movementDate,
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
            @Schema(description = "规格型号，保存物料基础资料快照。")
            @Size(max = 160, message = "规格型号长度不能超过160个字符")
            /**
             * 记录组件 specification：表示接口入参或出参中的 specification 字段。
             */
            String specification,
            @Schema(description = "库存组织。")
            @Size(max = 120, message = "库存组织长度不能超过120个字符")
            /**
             * 记录组件 stockOrganization：表示接口入参或出参中的 stockOrganization 字段。
             */
            String stockOrganization,
            @Schema(description = "货主。")
            @Size(max = 120, message = "货主长度不能超过120个字符")
            /**
             * 记录组件 ownerName：表示接口入参或出参中的 ownerName 字段。
             */
            String ownerName,
            @Schema(description = "库存单位。")
            @Size(max = 60, message = "库存单位长度不能超过60个字符")
            /**
             * 记录组件 unitName：表示接口入参或出参中的 unitName 字段。
             */
            String unitName,
            @Schema(description = "批号。")
            @Size(max = 120, message = "批号长度不能超过120个字符")
            /**
             * 记录组件 batchNo：表示接口入参或出参中的 batchNo 字段。
             */
            String batchNo,
            @Schema(description = "变动数量，必须大于 0。")
            @Positive BigDecimal quantity,
            @Schema(description = "来源仓库，来自基础信息仓库字典。")
            @Size(max = 120, message = "来源仓库长度不能超过120个字符")
            /**
             * 记录组件 fromWarehouse：表示接口入参或出参中的 fromWarehouse 字段。
             */
            String fromWarehouse,
            @Schema(description = "目标仓库，来自基础信息仓库字典。")
            @Size(max = 120, message = "目标仓库长度不能超过120个字符")
            /**
             * 记录组件 toWarehouse：表示接口入参或出参中的 toWarehouse 字段。
             */
            String toWarehouse,
            @Schema(description = "关联业务单号。")
            @Size(max = 300, message = "关联业务单号长度不能超过300个中文字符")
            /**
             * 记录组件 relatedBizNo：表示接口入参或出参中的 relatedBizNo 字段。
             */
            String relatedBizNo,
            @Schema(description = "来源单据类型。")
            @Size(max = 80, message = "来源单据类型长度不能超过80个字符")
            /**
             * 记录组件 sourceBillType：表示接口入参或出参中的 sourceBillType 字段。
             */
            String sourceBillType,
            @Schema(description = "备注。")
            @Size(max = 2000, message = "备注长度不能超过2000个中文字符")
            /**
             * 记录组件 remark：表示接口入参或出参中的 remark 字段。
             */
            String remark
    ) {
    }

    /**
     * InventoryView 数据传输记录。
     * 
     * <p>用于承载 InventoryView 相关的业务数据、接口契约或处理逻辑，便于维护人员快速定位模块职责。</p>
     */
    public record InventoryView(
            /**
             * 记录组件 id：表示接口入参或出参中的 id 字段。
             */
            Long id,
            /**
             * 记录组件 movementNo：表示接口入参或出参中的 movementNo 字段。
             */
            String movementNo,
            /**
             * 记录组件 movementType：表示接口入参或出参中的 movementType 字段。
             */
            InventoryMovementType movementType,
            /**
             * 记录组件 movementDate：表示接口入参或出参中的 movementDate 字段。
             */
            LocalDate movementDate,
            /**
             * 记录组件 itemCode：表示接口入参或出参中的 itemCode 字段。
             */
            String itemCode,
            /**
             * 记录组件 itemName：表示接口入参或出参中的 itemName 字段。
             */
            String itemName,
            /**
             * 记录组件 projectCode：表示接口入参或出参中的 projectCode 字段。
             */
            String projectCode,
            /**
             * 记录组件 projectName：表示接口入参或出参中的 projectName 字段。
             */
            String projectName,
            /**
             * 记录组件 specification：表示接口入参或出参中的 specification 字段。
             */
            String specification,
            /**
             * 记录组件 stockOrganization：表示接口入参或出参中的 stockOrganization 字段。
             */
            String stockOrganization,
            /**
             * 记录组件 ownerName：表示接口入参或出参中的 ownerName 字段。
             */
            String ownerName,
            /**
             * 记录组件 unitName：表示接口入参或出参中的 unitName 字段。
             */
            String unitName,
            /**
             * 记录组件 batchNo：表示接口入参或出参中的 batchNo 字段。
             */
            String batchNo,
            /**
             * 记录组件 quantity：表示接口入参或出参中的 quantity 字段。
             */
            BigDecimal quantity,
            /**
             * 记录组件 fromWarehouse：表示接口入参或出参中的 fromWarehouse 字段。
             */
            String fromWarehouse,
            /**
             * 记录组件 toWarehouse：表示接口入参或出参中的 toWarehouse 字段。
             */
            String toWarehouse,
            /**
             * 记录组件 relatedBizNo：表示接口入参或出参中的 relatedBizNo 字段。
             */
            String relatedBizNo,
            /**
             * 记录组件 sourceBillType：表示接口入参或出参中的 sourceBillType 字段。
             */
            String sourceBillType,
            /**
             * 记录组件 remark：表示接口入参或出参中的 remark 字段。
             */
            String remark,
            /**
             * 记录组件 organizationCode：表示接口入参或出参中的 organizationCode 字段。
             */
            String organizationCode,
            /**
             * 记录组件 voucherId：表示库存流水通过会计平台生成的凭证主键。
             */
            Long voucherId,
            /**
             * 记录组件 voucherNo：表示库存流水通过会计平台生成的凭证号。
             */
            String voucherNo,
            /**
             * 记录组件 attachmentCount：表示该库存流水绑定的附件数量，用于列表判断是否展示附件按钮。
             */
            long attachmentCount
    ) {
    }

    /**
     * InventoryStockView 数据传输记录。
     * 
     * <p>用于承载 InventoryStockView 相关的业务数据、接口契约或处理逻辑，便于维护人员快速定位模块职责。</p>
     */
    public record InventoryStockView(
            /**
             * 记录组件 itemCode：表示接口入参或出参中的 itemCode 字段。
             */
            String itemCode,
            /**
             * 记录组件 itemName：表示接口入参或出参中的 itemName 字段。
             */
            String itemName,
            /**
             * 记录组件 warehouse：表示接口入参或出参中的 warehouse 字段。
             */
            String warehouse,
            /**
             * 记录组件 asOfDate：表示接口入参或出参中的 asOfDate 字段。
             */
            LocalDate asOfDate,
            /**
             * 记录组件 availableQuantity：表示接口入参或出参中的 availableQuantity 字段。
             */
            BigDecimal availableQuantity
    ) {
    }

    /**
     * InventoryMaterialStockView 数据传输记录。
     * 
     * <p>用于承载 InventoryMaterialStockView 相关的业务数据、接口契约或处理逻辑，便于维护人员快速定位模块职责。</p>
     */
    public record InventoryMaterialStockView(
            /**
             * 记录组件 itemCode：表示接口入参或出参中的 itemCode 字段。
             */
            String itemCode,
            /**
             * 记录组件 itemName：表示接口入参或出参中的 itemName 字段。
             */
            String itemName,
            /**
             * 记录组件 inboundQuantity：表示接口入参或出参中的 inboundQuantity 字段。
             */
            BigDecimal inboundQuantity,
            /**
             * 记录组件 outboundQuantity：表示接口入参或出参中的 outboundQuantity 字段。
             */
            BigDecimal outboundQuantity,
            /**
             * 记录组件 transferQuantity：表示接口入参或出参中的 transferQuantity 字段。
             */
            BigDecimal transferQuantity,
            /**
             * 记录组件 stockQuantity：表示接口入参或出参中的 stockQuantity 字段。
             */
            BigDecimal stockQuantity,
            /**
             * 记录组件 children：表示接口入参或出参中的 children 字段。
             */
            List<InventoryMaterialStockView> children
    ) {
    }

    @Schema(description = "库存台账列表导出请求。作者：ratel；开发人员：WenZhang；联系方式：18782945613。")
    /**
     * InventoryExportRequest 数据传输记录。
     * 
     * <p>用于承载 InventoryExportRequest 相关的业务数据、接口契约或处理逻辑，便于维护人员快速定位模块职责。</p>
     */
    public record InventoryExportRequest(
            @Schema(description = "选中的库存流水 ID。为空时按筛选条件导出。")
            /**
             * 记录组件 ids：表示接口入参或出参中的 ids 字段。
             */
            List<Long> ids,
            @Schema(description = "变动开始日期，格式 yyyy-MM-dd。")
            /**
             * 记录组件 startDate：表示接口入参或出参中的 startDate 字段。
             */
            LocalDate startDate,
            @Schema(description = "变动结束日期，格式 yyyy-MM-dd。")
            /**
             * 记录组件 endDate：表示接口入参或出参中的 endDate 字段。
             */
            LocalDate endDate,
            @Schema(description = "流水号，包含匹配。")
            /**
             * 记录组件 movementNo：表示接口入参或出参中的 movementNo 字段。
             */
            String movementNo,
            @Schema(description = "库存变动类型，等值匹配。")
            /**
             * 记录组件 movementType：表示接口入参或出参中的 movementType 字段。
             */
            InventoryMovementType movementType,
            @Schema(description = "物料编码兼容字段，当前列表搜索不再使用。")
            /**
             * 记录组件 itemCode：表示接口入参或出参中的 itemCode 字段。
             */
            String itemCode,
            @Schema(description = "物料名称，包含匹配。")
            /**
             * 记录组件 itemName：表示接口入参或出参中的 itemName 字段。
             */
            String itemName,
            @Schema(description = "项目字典编码，等值匹配。")
            /**
             * 记录组件 projectCode：表示接口入参或出参中的 projectCode 字段。
             */
            String projectCode,
            @Schema(description = "来源仓库，等值匹配。")
            /**
             * 记录组件 fromWarehouse：表示接口入参或出参中的 fromWarehouse 字段。
             */
            String fromWarehouse,
            @Schema(description = "目标仓库，等值匹配。")
            /**
             * 记录组件 toWarehouse：表示接口入参或出参中的 toWarehouse 字段。
             */
            String toWarehouse,
            @Schema(description = "关联业务单号，包含匹配。")
            /**
             * 记录组件 relatedBizNo：表示接口入参或出参中的 relatedBizNo 字段。
             */
            String relatedBizNo
    ) {
    }

    @Schema(description = "应收应付请求。作者：ratel；开发人员：WenZhang；联系方式：18782945613。")
    /**
     * ArApRequest 数据传输记录。
     * 
     * <p>用于承载 ArApRequest 相关的业务数据、接口契约或处理逻辑，便于维护人员快速定位模块职责。</p>
     */
    public record ArApRequest(
            @Schema(description = "单据类型：应收或应付。")
            @NotNull ArApType billType,
            @Schema(description = "往来单位名称。应收为客户，应付为供应商，来自基础信息客户/供应商字典。")
            @NotBlank(message = "往来单位名称不能为空")
            @Size(max = 160, message = "往来单位名称长度不能超过160个字符")
            /**
             * 记录组件 partnerName：表示接口入参或出参中的 partnerName 字段。
             */
            String partnerName,
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
            @Schema(description = "业务单据类型，如销售应收、采购应付。")
            @Size(max = 80, message = "业务单据类型长度不能超过80个字符")
            /**
             * 记录组件 documentType：表示接口入参或出参中的 documentType 字段。
             */
            String documentType,
            @Schema(description = "业务组织。")
            @Size(max = 120, message = "业务组织长度不能超过120个字符")
            /**
             * 记录组件 businessOrganization：表示接口入参或出参中的 businessOrganization 字段。
             */
            String businessOrganization,
            @Schema(description = "结算组织。")
            @Size(max = 120, message = "结算组织长度不能超过120个字符")
            /**
             * 记录组件 settlementOrganization：表示接口入参或出参中的 settlementOrganization 字段。
             */
            String settlementOrganization,
            @Schema(description = "收付款组织。")
            @Size(max = 120, message = "收付款组织长度不能超过120个字符")
            /**
             * 记录组件 paymentOrganization：表示接口入参或出参中的 paymentOrganization 字段。
             */
            String paymentOrganization,
            @Schema(description = "收付款条件。")
            @Size(max = 120, message = "收付款条件长度不能超过120个字符")
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
            @Schema(description = "单据日期。")
            @NotNull LocalDate billDate,
            @Schema(description = "到期日期，用于账龄和逾期状态计算。")
            @NotNull LocalDate dueDate,
            @Schema(description = "应收或应付总金额。")
            @Positive BigDecimal amount,
            @Schema(description = "已收或已付金额；为空时按 0 处理。")
            @PositiveOrZero(message = "已收或已付金额不能小于0")
            /**
             * 记录组件 paidAmount：表示接口入参或出参中的 paidAmount 字段。
             */
            BigDecimal paidAmount,
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
            @Schema(description = "应收应付发生时该币种折人民币汇率；人民币固定为 1，非人民币必填。")
            @Positive(message = "汇率必须大于0")
            /**
             * 记录组件 exchangeRateToCny：表示接口入参或出参中的 exchangeRateToCny 字段。
             */
            BigDecimal exchangeRateToCny,
            @Schema(description = "收付款计划说明。")
            @Size(max = 2000, message = "收付款计划说明长度不能超过2000个中文字符")
            /**
             * 记录组件 paymentPlan：表示接口入参或出参中的 paymentPlan 字段。
             */
            String paymentPlan
    ) {
    }

    /**
     * ArApView 数据传输记录。
     * 
     * <p>用于承载 ArApView 相关的业务数据、接口契约或处理逻辑，便于维护人员快速定位模块职责。</p>
     */
    public record ArApView(
            /**
             * 记录组件 id：表示接口入参或出参中的 id 字段。
             */
            Long id,
            /**
             * 记录组件 billNo：表示接口入参或出参中的 billNo 字段。
             */
            String billNo,
            /**
             * 记录组件 billType：表示接口入参或出参中的 billType 字段。
             */
            ArApType billType,
            /**
             * 记录组件 partnerName：表示接口入参或出参中的 partnerName 字段。
             */
            String partnerName,
            /**
             * 记录组件 projectCode：表示接口入参或出参中的 projectCode 字段。
             */
            String projectCode,
            /**
             * 记录组件 projectName：表示接口入参或出参中的 projectName 字段。
             */
            String projectName,
            /**
             * 记录组件 documentType：表示接口入参或出参中的 documentType 字段。
             */
            String documentType,
            /**
             * 记录组件 businessOrganization：表示接口入参或出参中的 businessOrganization 字段。
             */
            String businessOrganization,
            /**
             * 记录组件 settlementOrganization：表示接口入参或出参中的 settlementOrganization 字段。
             */
            String settlementOrganization,
            /**
             * 记录组件 paymentOrganization：表示接口入参或出参中的 paymentOrganization 字段。
             */
            String paymentOrganization,
            /**
             * 记录组件 paymentTerms：表示接口入参或出参中的 paymentTerms 字段。
             */
            String paymentTerms,
            /**
             * 记录组件 settlementMethod：表示接口入参或出参中的 settlementMethod 字段。
             */
            String settlementMethod,
            /**
             * 记录组件 sourceBillType：表示接口入参或出参中的 sourceBillType 字段。
             */
            String sourceBillType,
            /**
             * 记录组件 sourceBillNo：表示接口入参或出参中的 sourceBillNo 字段。
             */
            String sourceBillNo,
            /**
             * 记录组件 billDate：表示接口入参或出参中的 billDate 字段。
             */
            LocalDate billDate,
            /**
             * 记录组件 dueDate：表示接口入参或出参中的 dueDate 字段。
             */
            LocalDate dueDate,
            /**
             * 记录组件 amount：表示接口入参或出参中的 amount 字段。
             */
            BigDecimal amount,
            /**
             * 记录组件 paidAmount：表示接口入参或出参中的 paidAmount 字段。
             */
            BigDecimal paidAmount,
            /**
             * 记录组件 remainingAmount：表示接口入参或出参中的 remainingAmount 字段。
             */
            BigDecimal remainingAmount,
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
             * 记录组件 amountCny：表示接口入参或出参中的 amountCny 字段。
             */
            BigDecimal amountCny,
            /**
             * 记录组件 paidAmountCny：表示接口入参或出参中的 paidAmountCny 字段。
             */
            BigDecimal paidAmountCny,
            /**
             * 记录组件 remainingAmountCny：表示接口入参或出参中的 remainingAmountCny 字段。
             */
            BigDecimal remainingAmountCny,
            /**
             * 记录组件 status：表示接口入参或出参中的 status 字段。
             */
            ArApStatus status,
            /**
             * 记录组件 agingDays：表示接口入参或出参中的 agingDays 字段。
             */
            long agingDays,
            /**
             * 记录组件 paymentPlan：表示接口入参或出参中的 paymentPlan 字段。
             */
            String paymentPlan,
            /**
             * 记录组件 organizationCode：表示接口入参或出参中的 organizationCode 字段。
             */
            String organizationCode,
            /**
             * 记录组件 voucherId：表示应收应付单通过会计平台生成的凭证主键。
             */
            Long voucherId,
            /**
             * 记录组件 voucherNo：表示应收应付单通过会计平台生成的凭证号。
             */
            String voucherNo,
            /**
             * 记录组件 attachmentCount：表示该应收应付单绑定的附件数量，用于列表判断是否展示附件按钮。
             */
            long attachmentCount
    ) {
    }

    @Schema(description = "应收应付列表导出请求。作者：ratel；开发人员：WenZhang；联系方式：18782945613。")
    /**
     * ArApExportRequest 数据传输记录。
     * 
     * <p>用于承载 ArApExportRequest 相关的业务数据、接口契约或处理逻辑，便于维护人员快速定位模块职责。</p>
     */
    public record ArApExportRequest(
            @Schema(description = "选中的应收应付单 ID。为空时按筛选条件导出。")
            /**
             * 记录组件 ids：表示接口入参或出参中的 ids 字段。
             */
            List<Long> ids,
            @Schema(description = "到期开始日期，格式 yyyy-MM-dd。")
            /**
             * 记录组件 startDate：表示接口入参或出参中的 startDate 字段。
             */
            LocalDate startDate,
            @Schema(description = "到期结束日期，格式 yyyy-MM-dd。")
            /**
             * 记录组件 endDate：表示接口入参或出参中的 endDate 字段。
             */
            LocalDate endDate,
            @Schema(description = "单号，包含匹配。")
            /**
             * 记录组件 billNo：表示接口入参或出参中的 billNo 字段。
             */
            String billNo,
            @Schema(description = "单据类型，等值匹配。")
            /**
             * 记录组件 billType：表示接口入参或出参中的 billType 字段。
             */
            ArApType billType,
            @Schema(description = "客户或供应商名称，等值匹配。")
            /**
             * 记录组件 partnerName：表示接口入参或出参中的 partnerName 字段。
             */
            String partnerName,
            @Schema(description = "项目字典编码，等值匹配。")
            /**
             * 记录组件 projectCode：表示接口入参或出参中的 projectCode 字段。
             */
            String projectCode,
            @Schema(description = "单据状态，等值匹配。")
            /**
             * 记录组件 status：表示接口入参或出参中的 status 字段。
             */
            ArApStatus status,
            @Schema(description = "付款计划，包含匹配。")
            /**
             * 记录组件 paymentPlan：表示接口入参或出参中的 paymentPlan 字段。
             */
            String paymentPlan
    ) {
    }

    /**
     * ArApPaymentStatsExportRequest 数据传输记录。
     *
     * <p>用于承载收付统计导出的筛选条件，保持导出结果与页面查询结果使用同一套项目和往来单位过滤口径。</p>
     */
    public record ArApPaymentStatsExportRequest(
            @Schema(description = "项目字典编码，空值表示导出全部项目。")
            /**
             * 记录组件 projectCode：表示收付统计导出使用的项目字典编码。
             */
            String projectCode,
            @Schema(description = "客户或供应商名称，空值表示导出全部往来单位。")
            /**
             * 记录组件 partnerName：表示收付统计导出使用的客户或供应商名称。
             */
            String partnerName
    ) {
    }

    /**
     * ArApPaymentStatsRow 数据传输记录。
     *
     * <p>用于承载收付统计中的单据行，按应收应付单号分别计算应收、应付、已收、已付、待收和待付金额。</p>
     */
    public record ArApPaymentStatsRow(
            /**
             * 记录组件 billNo：表示应收应付单号。
             */
            String billNo,
            /**
             * 记录组件 billType：表示单据类型，用于区分应收和应付。
             */
            ArApType billType,
            /**
             * 记录组件 projectCode：表示项目字典编码。
             */
            String projectCode,
            /**
             * 记录组件 projectName：表示项目名称快照。
             */
            String projectName,
            /**
             * 记录组件 partnerName：表示客户或供应商名称。
             */
            String partnerName,
            /**
             * 记录组件 receivableAmount：表示该单据应收金额。
             */
            BigDecimal receivableAmount,
            /**
             * 记录组件 payableAmount：表示该单据应付金额。
             */
            BigDecimal payableAmount,
            /**
             * 记录组件 receivedAmount：表示该单据已收金额，应收单据按已核销金额填充。
             */
            BigDecimal receivedAmount,
            /**
             * 记录组件 paidAmount：表示该单据已付金额，应付单据按已核销金额填充。
             */
            BigDecimal paidAmount,
            /**
             * 记录组件 pendingReceivableAmount：表示该单据待收金额。
             */
            BigDecimal pendingReceivableAmount,
            /**
             * 记录组件 pendingPayableAmount：表示该单据待付金额。
             */
            BigDecimal pendingPayableAmount
    ) {
    }

    /**
     * ArApPaymentStatsView 数据传输记录。
     *
     * <p>用于承载收付统计结果，包含明细行和页面底部汇总金额。</p>
     */
    public record ArApPaymentStatsView(
            /**
             * 记录组件 rows：表示按应收应付单号统计的明细行。
             */
            List<ArApPaymentStatsRow> rows,
            /**
             * 记录组件 totalReceivableAmount：表示全部应收金额汇总。
             */
            BigDecimal totalReceivableAmount,
            /**
             * 记录组件 totalPayableAmount：表示全部应付金额汇总。
             */
            BigDecimal totalPayableAmount,
            /**
             * 记录组件 totalReceivedAmount：表示全部已收金额汇总。
             */
            BigDecimal totalReceivedAmount,
            /**
             * 记录组件 totalPaidAmount：表示全部已付金额汇总。
             */
            BigDecimal totalPaidAmount,
            /**
             * 记录组件 totalPendingReceivableAmount：表示全部待收金额汇总。
             */
            BigDecimal totalPendingReceivableAmount,
            /**
             * 记录组件 totalPendingPayableAmount：表示全部待付金额汇总。
             */
            BigDecimal totalPendingPayableAmount
    ) {
    }

    /**
     * ArApSettlementRequest 数据传输记录。
     *
     * <p>用于承载应收应付收款、付款或核销动作的接口入参。</p>
     */
    public record ArApSettlementRequest(
            @Schema(description = "核销日期，即实际收款或付款日期。")
            @NotNull LocalDate settlementDate,
            @Schema(description = "本次核销原币金额。")
            @Positive(message = "核销金额必须大于0")
            /**
             * 记录组件 amount：表示本次核销原币金额。
             */
            BigDecimal amount,
            @Schema(description = "结算方式。")
            @Size(max = 120, message = "结算方式长度不能超过120个字符")
            /**
             * 记录组件 settlementMethod：表示本次收付使用的结算方式。
             */
            String settlementMethod,
            @Schema(description = "银行或现金账户。")
            @Size(max = 160, message = "银行或现金账户长度不能超过160个字符")
            /**
             * 记录组件 bankAccount：表示本次收付使用的银行或现金账户。
             */
            String bankAccount,
            @Schema(description = "关联出纳流水号。")
            @Size(max = 80, message = "关联出纳流水号长度不能超过80个字符")
            /**
             * 记录组件 cashierTransactionNo：表示关联的出纳资金流水号。
             */
            String cashierTransactionNo,
            @Schema(description = "核销说明。")
            @Size(max = 2000, message = "核销说明长度不能超过2000个中文字符")
            /**
             * 记录组件 remark：表示本次核销说明。
             */
            String remark
    ) {
    }

    /**
     * ArApSettlementView 数据传输记录。
     *
     * <p>用于承载应收应付单的收付核销流水，供页面查看每次收款或付款明细。</p>
     */
    public record ArApSettlementView(
            /**
             * 记录组件 id：表示核销流水主键。
             */
            Long id,
            /**
             * 记录组件 billId：表示被核销的应收应付单主键。
             */
            Long billId,
            /**
             * 记录组件 billNo：表示被核销的应收应付单号。
             */
            String billNo,
            /**
             * 记录组件 settlementDate：表示实际收款或付款日期。
             */
            LocalDate settlementDate,
            /**
             * 记录组件 amount：表示本次核销原币金额。
             */
            BigDecimal amount,
            /**
             * 记录组件 amountCny：表示本次核销折人民币金额。
             */
            BigDecimal amountCny,
            /**
             * 记录组件 settlementMethod：表示结算方式。
             */
            String settlementMethod,
            /**
             * 记录组件 bankAccount：表示银行或现金账户。
             */
            String bankAccount,
            /**
             * 记录组件 cashierTransactionNo：表示关联出纳流水号。
             */
            String cashierTransactionNo,
            /**
             * 记录组件 remark：表示核销说明。
             */
            String remark
    ) {
    }

    /**
     * FinancialStatement 数据传输记录。
     * 
     * <p>用于承载 FinancialStatement 相关的业务数据、接口契约或处理逻辑，便于维护人员快速定位模块职责。</p>
     */
    public record FinancialStatement(
            /**
             * 记录组件 statementName：表示接口入参或出参中的 statementName 字段。
             */
            String statementName,
            /**
             * 记录组件 reportDate：表示接口入参或出参中的 reportDate 字段。
             */
            LocalDate reportDate,
            /**
             * 记录组件 lines：表示接口入参或出参中的 lines 字段。
             */
            List<StatementLine> lines
    ) {
    }

    /**
     * StatementLine 数据传输记录。
     * 
     * <p>用于承载 StatementLine 相关的业务数据、接口契约或处理逻辑，便于维护人员快速定位模块职责。</p>
     */
    public record StatementLine(
            /**
             * 记录组件 itemName：表示接口入参或出参中的 itemName 字段。
             */
            String itemName,
            /**
             * 记录组件 amount：表示接口入参或出参中的 amount 字段。
             */
            BigDecimal amount
    ) {
    }

    @Schema(description = "ratel助手请求。作者：ratel；开发人员：WenZhang；联系方式：18782945613。")
    /**
     * AiAssistantRequest 数据传输记录。
     * 
     * <p>用于承载 AiAssistantRequest 相关的业务数据、接口契约或处理逻辑，便于维护人员快速定位模块职责。</p>
     */
    public record AiAssistantRequest(
            @Schema(description = "用户自然语言问题。")
            @NotBlank(message = "问题不能为空")
            @Size(max = 500, message = "问题长度不能超过500个字符")
            /**
             * 记录组件 question：表示接口入参或出参中的 question 字段。
             */
            String question,
            @Schema(description = "检索模式：local 本地知识库；web 互联网检索；hybrid 混合检索；command 语音/操作指令。")
            @Size(max = 20, message = "检索模式长度不能超过20个字符")
            /**
             * 记录组件 mode：表示接口入参或出参中的 mode 字段。
             */
            String mode,
            @Schema(description = "客户端当前会话短摘要，仅用于追问指代，不作为实时业务事实依据。")
            @Size(max = 2000, message = "会话摘要长度不能超过2000个字符")
            /**
             * 记录组件 conversationSummary：表示接口入参中的会话短摘要字段。
             */
            String conversationSummary,
            @Schema(description = "客户端保留的最近对话消息，服务端会按配置裁剪最近原文轮次。")
            @Size(max = 20, message = "会话消息不能超过20条")
            /**
             * 记录组件 conversationMessages：表示接口入参中的最近对话消息字段。
             */
            List<AiConversationMessage> conversationMessages
    ) {
    }

    @Schema(description = "ratel助手会话消息。作者：ratel；开发人员：WenZhang；联系方式：18782945613。")
    /**
     * AiConversationMessage 数据传输记录。
     */
    public record AiConversationMessage(
            @Schema(description = "消息角色：user 或 assistant。")
            @Size(max = 20, message = "消息角色长度不能超过20个字符")
            /**
             * 记录组件 role：表示接口入参中的消息角色。
             */
            String role,
            @Schema(description = "消息内容。")
            @Size(max = 4000, message = "消息内容不能超过4000个字符")
            /**
             * 记录组件 content：表示接口入参中的消息内容。
             */
            String content
    ) {
    }

    /**
     * AiAssistantResponse 数据传输记录。
     * 
     * <p>用于承载 AiAssistantResponse 相关的业务数据、接口契约或处理逻辑，便于维护人员快速定位模块职责。</p>
     */
    public record AiAssistantResponse(
            /**
             * 记录组件 question：表示接口入参或出参中的 question 字段。
             */
            String question,
            /**
             * 记录组件 answer：表示接口入参或出参中的 answer 字段。
             */
            String answer,
            /**
             * 记录组件 suggestions：表示接口入参或出参中的 suggestions 字段。
             */
            List<String> suggestions
    ) {
    }
}
