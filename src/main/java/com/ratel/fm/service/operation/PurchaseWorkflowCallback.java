package com.ratel.fm.service.operation;

import com.ratel.fm.common.BusinessException;
import com.ratel.fm.domain.purchase.PurchaseOrder;
import com.ratel.fm.domain.purchase.PurchaseOrderLine;
import com.ratel.fm.domain.purchase.PurchaseStatus;
import com.ratel.fm.domain.workflow.WorkflowInstance;
import com.ratel.fm.domain.workflow.WorkflowStatus;
import com.ratel.fm.repository.purchase.PurchaseOrderRepository;
import com.ratel.fm.service.knowledge.KnowledgeIndexService;
import com.ratel.fm.service.operationlog.BusinessOperationLogService;
import com.ratel.fm.service.workflow.WorkflowBusinessCallback;
import com.ratel.fm.web.dto.workflow.WorkflowDtos.WorkflowBusinessFormFieldView;
import com.ratel.fm.web.dto.workflow.WorkflowDtos.WorkflowBusinessFormSectionView;
import com.ratel.fm.web.dto.workflow.WorkflowDtos.WorkflowBusinessFormTableColumnView;
import com.ratel.fm.web.dto.workflow.WorkflowDtos.WorkflowBusinessFormTableView;
import com.ratel.fm.web.dto.workflow.WorkflowDtos.WorkflowBusinessFormView;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Comparator;
import java.util.List;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 采购审批流程完成回调。
 *
 * <p>实现目的：
 * 1. 将流程引擎和采购业务状态机解耦；
 * 2. 审批完成时按流程最终结果回写采购单状态；
 * 3. 写入采购单操作流水，保证查看流水能看到审批结果。</p>
 */
@Service
public class PurchaseWorkflowCallback implements WorkflowBusinessCallback {

    /** 采购单流程业务类型，必须和采购提交审批时写入的业务类型一致。 */
    private static final String PURCHASE_WORKFLOW_BUSINESS_TYPE = "PURCHASE_ORDER";

    /** 采购单仓库，用于按所属公司和业务 ID 读取采购单。 */
    private final PurchaseOrderRepository purchaseOrderRepository;

    /** 业务操作流水服务，用于记录审批回写采购状态的结果。 */
    private final BusinessOperationLogService businessOperationLogService;

    /** 知识索引服务，用于审批回写状态后同步刷新 AI 检索上下文。 */
    private final KnowledgeIndexService knowledgeIndexService;

    /**
     * 构造采购流程回调。
     *
     * <p>实现步骤：
     * 1. 注入采购单仓库；
     * 2. 注入业务流水服务；
     * 3. 回调触发时只处理采购单自身状态，避免和 OperationService 形成循环依赖。</p>
     */
    public PurchaseWorkflowCallback(
            PurchaseOrderRepository purchaseOrderRepository,
            BusinessOperationLogService businessOperationLogService,
            KnowledgeIndexService knowledgeIndexService
    ) {
        this.purchaseOrderRepository = purchaseOrderRepository;
        this.businessOperationLogService = businessOperationLogService;
        this.knowledgeIndexService = knowledgeIndexService;
    }

    /**
     * 返回采购流程业务类型。
     *
     * <p>实现步骤：流程服务完成审批时通过该业务类型定位本回调。</p>
     */
    @Override
    public String businessType() {
        return PURCHASE_WORKFLOW_BUSINESS_TYPE;
    }

    /**
     * 流程完成后回写采购状态。
     *
     * <p>实现步骤：
     * 1. 按流程实例中的所属公司和业务 ID 读取采购单；
     * 2. 审批同意时回写已审批同意，审批不同意时回写已审批不同意；
     * 3. 记录采购操作流水，方便用户在查看流水中看到审批结果和意见。</p>
     */
    @Override
    @Transactional
    public void onWorkflowCompleted(WorkflowInstance instance, WorkflowStatus status, String comment) {
        PurchaseOrder order = purchaseOrderRepository.findWithLinesByOrganizationCodeAndId(instance.getOrganizationCode(), instance.getBusinessId())
                .orElseThrow(() -> new BusinessException(HttpStatus.NOT_FOUND, "采购单不存在"));
        PurchaseStatus oldStatus = order.getStatus();
        PurchaseStatus targetStatus = status == WorkflowStatus.APPROVED ? PurchaseStatus.APPROVED : PurchaseStatus.APPROVAL_REJECTED;
        order.setStatus(targetStatus);
        businessOperationLogService.record(
                PURCHASE_WORKFLOW_BUSINESS_TYPE,
                order.getId(),
                order.getOrderNo(),
                order.getOrderNo() + " " + order.getSupplierName(),
                "APPROVAL_RESULT",
                "采购审批完成",
                "采购单号" + order.getOrderNo() + "审批完成，结果为" + (status == WorkflowStatus.APPROVED ? "同意" : "不同意")
                        + "，审批意见：" + defaultText(comment, "-") + "。",
                purchaseStatusText(oldStatus),
                purchaseStatusText(targetStatus),
                purchaseSnapshot(order, targetStatus, status, comment)
        );
        knowledgeIndexService.rebuildPurchaseOrder(order);
    }

    /**
     * 构建采购流程业务表单预览。
     *
     * <p>实现步骤：
     * 1. 按流程实例所属公司和业务 ID 读取采购单及明细；
     * 2. 将采购单头拆成基本信息、组织与结算、金额信息、备注说明四个字段分组；
     * 3. 将采购明细转换为通用表格行，供审批和查看流程弹窗共用展示。</p>
     */
    @Override
    @Transactional(readOnly = true)
    public WorkflowBusinessFormView businessForm(WorkflowInstance instance) {
        PurchaseOrder order = purchaseOrderRepository.findWithLinesByOrganizationCodeAndId(instance.getOrganizationCode(), instance.getBusinessId())
                .orElseThrow(() -> new BusinessException(HttpStatus.NOT_FOUND, "采购单不存在"));
        return new WorkflowBusinessFormView(
                "采购单：" + order.getOrderNo(),
                List.of(
                        section("基本信息",
                                field("采购单号", order.getOrderNo()),
                                field("供应商", order.getSupplierName()),
                                field("单据类型", order.getDocumentType()),
                                field("业务类型", order.getBusinessType()),
                                field("项目", order.getProjectName()),
                                field("采购日期", text(order.getOrderDate())),
                                field("来源类型", order.getSourceBillType()),
                                field("来源单号", order.getSourceBillNo()),
                                field("状态", purchaseStatusText(order.getStatus()))
                        ),
                        section("组织与结算",
                                field("采购组织", order.getPurchaseOrganization()),
                                field("采购部门", order.getPurchaseDepartment()),
                                field("采购员", order.getPurchaserName()),
                                field("结算组织", order.getSettlementOrganization()),
                                field("付款条件", order.getPaymentTerms()),
                                field("结算方式", order.getSettlementMethod()),
                                field("交货条件", order.getDeliveryTerms())
                        ),
                        section("金额信息",
                                field("币种", defaultText(order.getCurrencyName(), order.getCurrencyCode())),
                                field("汇率", text(order.getExchangeRateToCny())),
                                field("采购金额", text(order.getTotalAmount())),
                                field("采购人民币金额", text(order.getTotalAmountCny()))
                        ),
                        section("备注说明",
                                field("备注", order.getRemark()),
                                field("取消类型", order.getCancelType()),
                                field("取消原因", order.getCancelReason())
                        )
                ),
                List.of(purchaseLineTable(order))
        );
    }

    /**
     * 生成采购审批完成流水快照。
     *
     * <p>实现步骤：
     * 1. 写入采购头完整字段，保证查看流水与采购表单展示内容一致；
     * 2. 写入审批结果和审批意见，避免有快照时操作说明被折叠后看不到审批意见；
     * 3. 写入采购明细集合，供流水抽屉展示明细列表。</p>
     */
    private Map<String, Object> purchaseSnapshot(PurchaseOrder order, PurchaseStatus status, WorkflowStatus workflowStatus, String comment) {
        Map<String, Object> snapshot = new LinkedHashMap<>();
        snapshot.put("orderNo", order.getOrderNo());
        snapshot.put("supplierName", order.getSupplierName());
        snapshot.put("documentType", order.getDocumentType());
        snapshot.put("businessType", order.getBusinessType());
        snapshot.put("projectCode", order.getProjectCode());
        snapshot.put("projectName", order.getProjectName());
        snapshot.put("purchaseOrganization", order.getPurchaseOrganization());
        snapshot.put("purchaseDepartment", order.getPurchaseDepartment());
        snapshot.put("purchaserName", order.getPurchaserName());
        snapshot.put("settlementOrganization", order.getSettlementOrganization());
        snapshot.put("paymentTerms", order.getPaymentTerms());
        snapshot.put("settlementMethod", order.getSettlementMethod());
        snapshot.put("deliveryTerms", order.getDeliveryTerms());
        snapshot.put("sourceBillType", order.getSourceBillType());
        snapshot.put("sourceBillNo", order.getSourceBillNo());
        snapshot.put("orderDate", order.getOrderDate());
        snapshot.put("status", status);
        snapshot.put("statusText", purchaseStatusText(status));
        snapshot.put("approvalResult", workflowStatus == WorkflowStatus.APPROVED ? "同意" : "不同意");
        snapshot.put("approvalComment", defaultText(comment, "-"));
        snapshot.put("totalAmount", order.getTotalAmount());
        snapshot.put("currencyCode", order.getCurrencyCode());
        snapshot.put("currencyName", order.getCurrencyName());
        snapshot.put("exchangeRateToCny", order.getExchangeRateToCny());
        snapshot.put("totalAmountCny", order.getTotalAmountCny());
        snapshot.put("createdBy", order.getCreatedBy());
        snapshot.put("remark", order.getRemark());
        snapshot.put("lines", order.getLines().stream()
                .sorted(Comparator.comparingInt(PurchaseOrderLine::getLineNo))
                .map(this::purchaseLineSnapshot)
                .toList());
        return snapshot;
    }

    /**
     * 生成采购明细流水快照。
     *
     * <p>实现步骤：逐行提取物料、数量、价格、税额、币种、汇率和到货仓库，供查看流水明细表展示。</p>
     */
    private Map<String, Object> purchaseLineSnapshot(PurchaseOrderLine line) {
        Map<String, Object> snapshot = new LinkedHashMap<>();
        snapshot.put("lineNo", line.getLineNo());
        snapshot.put("itemCode", line.getItemCode());
        snapshot.put("itemName", line.getItemName());
        snapshot.put("specification", line.getSpecification());
        snapshot.put("unitName", line.getUnitName());
        snapshot.put("quantity", line.getQuantity());
        snapshot.put("unitPrice", line.getUnitPrice());
        snapshot.put("taxRate", line.getTaxRate());
        snapshot.put("taxAmount", line.getTaxAmount());
        snapshot.put("amount", line.getAmount());
        snapshot.put("amountWithTax", line.getAmountWithTax());
        snapshot.put("plannedArrivalDate", line.getPlannedArrivalDate());
        snapshot.put("receiveWarehouse", line.getReceiveWarehouse());
        snapshot.put("currencyCode", line.getCurrencyCode());
        snapshot.put("currencyName", line.getCurrencyName());
        snapshot.put("exchangeRateToCny", line.getExchangeRateToCny());
        snapshot.put("unitPriceCny", line.getUnitPriceCny());
        snapshot.put("amountCny", line.getAmountCny());
        return snapshot;
    }

    /**
     * 构建采购明细通用表格。
     *
     * <p>实现步骤：定义稳定列编码和中文列名，再把每条采购明细按列编码写入字符串值。</p>
     */
    private WorkflowBusinessFormTableView purchaseLineTable(PurchaseOrder order) {
        List<WorkflowBusinessFormTableColumnView> columns = List.of(
                column("lineNo", "行号"),
                column("itemName", "物料"),
                column("specification", "规格型号"),
                column("unitName", "单位"),
                column("quantity", "数量"),
                column("unitPrice", "单价"),
                column("taxRate", "税率"),
                column("amountWithTax", "价税合计"),
                column("currencyName", "币种"),
                column("plannedArrivalDate", "计划到货"),
                column("receiveWarehouse", "收货仓库")
        );
        List<Map<String, String>> rows = order.getLines().stream()
                .sorted(Comparator.comparingInt(PurchaseOrderLine::getLineNo))
                .map(line -> {
                    Map<String, String> row = new LinkedHashMap<>();
                    row.put("lineNo", text(line.getLineNo()));
                    row.put("itemName", defaultText(line.getItemName(), "-"));
                    row.put("specification", defaultText(line.getSpecification(), "-"));
                    row.put("unitName", defaultText(line.getUnitName(), "-"));
                    row.put("quantity", text(line.getQuantity()));
                    row.put("unitPrice", text(line.getUnitPrice()));
                    row.put("taxRate", text(line.getTaxRate()));
                    row.put("amountWithTax", text(line.getAmountWithTax()));
                    row.put("currencyName", defaultText(line.getCurrencyName(), line.getCurrencyCode()));
                    row.put("plannedArrivalDate", text(line.getPlannedArrivalDate()));
                    row.put("receiveWarehouse", defaultText(line.getReceiveWarehouse(), "-"));
                    return row;
                })
                .toList();
        return new WorkflowBusinessFormTableView("采购明细", columns, rows);
    }

    /**
     * 构建业务表单字段分组。
     *
     * <p>实现步骤：保留分组标题和传入字段顺序，前端按顺序渲染。</p>
     */
    private WorkflowBusinessFormSectionView section(String title, WorkflowBusinessFormFieldView... fields) {
        return new WorkflowBusinessFormSectionView(title, List.of(fields));
    }

    /**
     * 构建业务表单字段。
     *
     * <p>实现步骤：将任意对象转为字符串并为空值兜底，避免前端出现空白单元格。</p>
     */
    private WorkflowBusinessFormFieldView field(String label, Object value) {
        return new WorkflowBusinessFormFieldView(label, text(value));
    }

    /**
     * 构建业务表单明细列。
     */
    private WorkflowBusinessFormTableColumnView column(String key, String label) {
        return new WorkflowBusinessFormTableColumnView(key, label);
    }

    /**
     * 采购状态中文化。
     *
     * <p>实现步骤：把状态枚举转换成用户在采购管理和流水中看到的中文状态。</p>
     */
    private String purchaseStatusText(PurchaseStatus status) {
        return switch (status) {
            case DRAFT -> "草稿";
            case IN_APPROVAL -> "审批中";
            case APPROVAL_REJECTED -> "已审批【不同意】";
            case SUBMITTED -> "已提交";
            case APPROVED -> "已审批【同意】";
            case PURCHASING -> "采购中";
            case PURCHASE_COMPLETED -> "采购完成";
            case RECEIVED -> "已收货";
            case CLOSED -> "已关闭";
            case CANCELLED -> "取消采购";
        };
    }

    /**
     * 空白文本兜底。
     *
     * <p>实现步骤：审批意见为空时返回横线，避免流水显示空白。</p>
     */
    private String defaultText(String value, String fallback) {
        return value == null || value.isBlank() ? fallback : value;
    }

    /**
     * 将业务值转为审批表单显示文本。
     *
     * <p>实现步骤：空值返回横线；非空值直接调用 toString，兼容日期、枚举、数字和普通文本。</p>
     */
    private String text(Object value) {
        return value == null ? "-" : value.toString();
    }
}
