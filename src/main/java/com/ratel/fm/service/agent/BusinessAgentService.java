package com.ratel.fm.service.agent;

import com.ratel.fm.config.ai.AiProperties;
import com.ratel.fm.domain.auth.PermissionCode;
import com.ratel.fm.domain.cashier.CashierTransactionStatus;
import com.ratel.fm.domain.finance.VoucherStatus;
import com.ratel.fm.domain.inventory.InventoryMovementType;
import com.ratel.fm.domain.logistics.ShipmentStatus;
import com.ratel.fm.domain.purchase.PurchaseStatus;
import com.ratel.fm.security.CompanyScope;
import com.ratel.fm.security.CurrentUser;
import com.ratel.fm.security.SecurityUtils;
import com.ratel.fm.service.cashier.CashierService;
import com.ratel.fm.service.assistant.FinancialIntentTerms;
import com.ratel.fm.service.finance.FinanceService;
import com.ratel.fm.service.inventory.InventoryService;
import com.ratel.fm.service.knowledge.KnowledgeSearchService;
import com.ratel.fm.service.operation.OperationService;
import com.ratel.fm.service.receivable.ArApService;
import com.ratel.fm.service.workflow.WorkflowService;
import com.ratel.fm.web.dto.agent.BusinessAgentDtos.BusinessAgentCapabilityResult;
import com.ratel.fm.web.dto.agent.BusinessAgentDtos.BusinessAgentEvidence;
import com.ratel.fm.web.dto.agent.BusinessAgentDtos.BusinessAgentAction;
import com.ratel.fm.web.dto.agent.BusinessAgentDtos.BusinessAgentModuleResult;
import com.ratel.fm.web.dto.agent.BusinessAgentDtos.BusinessAgentRequest;
import com.ratel.fm.web.dto.agent.BusinessAgentDtos.BusinessAgentResponse;
import com.ratel.fm.web.dto.agent.BusinessAgentDtos.BusinessAgentSelfCheck;
import com.ratel.fm.web.dto.cashier.CashierDtos.CashierTransactionView;
import com.ratel.fm.web.dto.finance.FinanceDtos.VoucherView;
import com.ratel.fm.web.dto.knowledge.KnowledgeDtos.KnowledgeSearchResult;
import com.ratel.fm.web.dto.operation.OperationDtos.PurchaseOrderView;
import com.ratel.fm.web.dto.operation.OperationDtos.ShipmentView;
import com.ratel.fm.web.dto.phasetwo.PhaseTwoDtos.ArApView;
import com.ratel.fm.web.dto.phasetwo.PhaseTwoDtos.InventoryMaterialStockView;
import com.ratel.fm.web.dto.phasetwo.PhaseTwoDtos.InventoryView;
import com.ratel.fm.web.dto.workflow.WorkflowDtos.WorkflowItemView;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/**
 * 业务 Agent 服务。
 *
 * <p>第一版只做受控只读分析：结合采购、物流、库存、应收应付、财务和审批模块生成概览、风险和建议，
 * 不直接执行新增、修改、删除、审批、过账等写操作。</p>
 */
@Service
public class BusinessAgentService {

    private final OperationService operationService;
    private final InventoryService inventoryService;
    private final ArApService arApService;
    private final FinanceService financeService;
    private final WorkflowService workflowService;
    private final CashierService cashierService;
    private final KnowledgeSearchService knowledgeSearchService;
    private final AiProperties aiProperties;
    private final BusinessMetricsService businessMetricsService;
    private final BusinessAgentSelector businessAgentSelector;

    public BusinessAgentService(
            OperationService operationService,
            InventoryService inventoryService,
            ArApService arApService,
            FinanceService financeService,
            WorkflowService workflowService,
            CashierService cashierService,
            KnowledgeSearchService knowledgeSearchService,
            AiProperties aiProperties,
            BusinessMetricsService businessMetricsService,
            BusinessAgentSelector businessAgentSelector
    ) {
        this.operationService = operationService;
        this.inventoryService = inventoryService;
        this.arApService = arApService;
        this.financeService = financeService;
        this.workflowService = workflowService;
        this.cashierService = cashierService;
        this.knowledgeSearchService = knowledgeSearchService;
        this.aiProperties = aiProperties;
        this.businessMetricsService = businessMetricsService;
        this.businessAgentSelector = businessAgentSelector;
    }

    /**
     * 运行业务 Agent。
     */
    @Transactional(readOnly = true)
    public BusinessAgentResponse run(BusinessAgentRequest request) {
        String question = value(request == null ? null : request.question());
        String stage = normalizeStage(request == null ? null : request.stage());
        if (!aiProperties.getAgent().isEnabled()) {
            return disabledResponse(question, stage);
        }
        int limit = Math.max(1, Math.min(request == null || request.limit() == null ? 5 : request.limit(), 10));
        CurrentUser user = SecurityUtils.currentUser();
        Set<PermissionCode> permissions = user.permissions() == null ? Set.of() : user.permissions();
        List<String> modules = selectedModules(question, request == null ? null : request.modules(), permissions);
        List<BusinessAgentModuleResult> results = new ArrayList<>();
        for (String module : modules) {
            results.add(analyzeModule(module, question, limit, permissions));
        }
        List<BusinessAgentCapabilityResult> capabilityResults = buildCapabilityResults(
                question,
                selectedAgentTypes(question, request == null ? null : request.agentTypes()),
                limit,
                permissions
        );
        List<String> risks = combineDistinct(
                results.stream().flatMap(item -> item.risks().stream()).toList(),
                capabilityResults.stream().flatMap(item -> item.risks().stream()).toList()
        );
        List<String> suggestions = combineDistinct(
                results.stream().flatMap(item -> item.suggestions().stream()).toList(),
                capabilityResults.stream().flatMap(item -> item.suggestions().stream()).toList()
        );
        List<BusinessAgentAction> actions = buildActions(stage, results, capabilityResults, risks);
        List<BusinessAgentSelfCheck> selfChecks = buildSelfChecks(stage, results, actions);
        return new BusinessAgentResponse(
                question,
                stage,
                "所属公司：" + CompanyScope.currentCompanyCode() + "；执行边界：只读分析，不执行写操作。",
                overallSummary(results, capabilityResults, risks),
                results,
                capabilityResults,
                actions,
                selfChecks,
                risks,
                suggestions,
                guardrails(stage)
        );
    }

    private BusinessAgentResponse disabledResponse(String question, String stage) {
        return new BusinessAgentResponse(
                question,
                stage,
                "所属公司：" + CompanyScope.currentCompanyCode() + "；业务 Agent 已关闭。",
                "业务 Agent 已通过配置关闭，本次未执行任何 Agent 分析。",
                List.of(),
                List.of(),
                List.of(),
                List.of(new BusinessAgentSelfCheck("业务 Agent 开关", true, "INFO", "配置关闭时不选择模块、不读取业务证据、不生成 Agent 计划。")),
                List.of(),
                List.of(),
                List.of("业务 Agent 未启用，前端应隐藏入口；如直接调用接口，后端只返回禁用说明，不参与业务流程。")
        );
    }

    private List<String> guardrails(String stage) {
        List<String> guardrails = new ArrayList<>(List.of(
                        "Agent 当前不会直接保存、删除、审批、取消或过账。",
                        "所有读取均复用现有业务 Service，并受当前用户权限和所属公司限制。",
                        "涉及写操作时只能生成建议或草稿说明，必须由用户在对应页面检查后手工确认。"
                ));
        if (!"readOnly".equals(stage)) {
            guardrails.add("草稿、受控执行和多步骤阶段当前只生成计划，不落库执行。");
        }
        if (aiProperties.getAgent().isSelfCheckEnabled()) {
            guardrails.add("关键 Agent 已启用自检，未通过自检的计划必须阻断执行。");
        }
        return guardrails;
    }

    private BusinessAgentModuleResult analyzeModule(String module, String question, int limit, Set<PermissionCode> permissions) {
        return switch (module) {
            case "purchase" -> analyzePurchase(question, limit, permissions);
            case "shipment" -> analyzeShipment(question, limit, permissions);
            case "inventory" -> analyzeInventory(question, limit, permissions);
            case "arAp" -> analyzeArAp(question, limit, permissions);
            case "finance" -> analyzeFinance(question, limit, permissions);
            case "workflow" -> analyzeWorkflow(question, limit, permissions);
            default -> unauthorized(module, moduleName(module), "暂不支持该业务模块。");
        };
    }

    private BusinessAgentModuleResult analyzePurchase(String question, int limit, Set<PermissionCode> permissions) {
        if (!hasAny(permissions, PermissionCode.PURCHASE_MANAGE, PermissionCode.REPORT_VIEW)) {
            return unauthorized("purchase", "采购", "当前用户无采购或报表查看权限。");
        }
        List<PurchaseOrderView> rows = operationService.listPurchaseOrders(null, null, businessNo(question), keyword(question), null, null, null, null);
        List<String> risks = new ArrayList<>();
        long inApproval = rows.stream().filter(item -> item.status() == PurchaseStatus.IN_APPROVAL).count();
        long rejected = rows.stream().filter(item -> item.status() == PurchaseStatus.APPROVAL_REJECTED).count();
        long noVoucher = rows.stream().filter(item -> item.voucherNo() == null || item.voucherNo().isBlank()).count();
        if (inApproval > 0) {
            risks.add("有 " + inApproval + " 张采购单处于审批中，需要关注审批节点。");
        }
        if (rejected > 0) {
            risks.add("有 " + rejected + " 张采购单审批不同意，建议复核申请原因后重新提交。");
        }
        if (noVoucher > 0) {
            risks.add("有 " + noVoucher + " 张采购单尚未关联凭证，可在会计平台检查是否需要制证。");
        }
        return new BusinessAgentModuleResult(
                "purchase",
                "采购",
                true,
                "命中采购单 " + rows.size() + " 张，合计人民币金额 " + money(rows.stream().map(PurchaseOrderView::totalAmountCny).toList()) + "。",
                List.of("审批中：" + inApproval, "审批不同意：" + rejected, "未关联凭证：" + noVoucher),
                risks,
                List.of("优先处理审批中和审批不同意采购单。", "对已收货采购单检查库存和应付是否已联动。"),
                rows.stream().limit(limit).map(item -> evidence("PURCHASE_ORDER", item.id(), item.orderNo(), item.supplierName(), item.status(), item.totalAmountCny(), item.orderDate(), "/purchase-orders")).toList()
        );
    }

    private BusinessAgentModuleResult analyzeShipment(String question, int limit, Set<PermissionCode> permissions) {
        if (!hasAny(permissions, PermissionCode.LOGISTICS_MANAGE, PermissionCode.REPORT_VIEW)) {
            return unauthorized("shipment", "物流", "当前用户无物流或报表查看权限。");
        }
        List<ShipmentView> rows = operationService.listShipments(null, null, businessNo(question), businessNo(question), null, null, null, null, null, null, null, null);
        LocalDate today = LocalDate.now();
        long inTransit = rows.stream().filter(item -> item.status() == ShipmentStatus.IN_TRANSIT).count();
        long overdue = rows.stream().filter(item -> item.deliveredDate() == null && item.plannedShipDate() != null && item.plannedShipDate().isBefore(today)).count();
        List<String> risks = new ArrayList<>();
        if (inTransit > 0) {
            risks.add("有 " + inTransit + " 张物流单仍在运输中，建议关注预计送达。");
        }
        if (overdue > 0) {
            risks.add("有 " + overdue + " 张物流单计划日期已过但未送达。");
        }
        return new BusinessAgentModuleResult(
                "shipment",
                "物流",
                true,
                "命中物流单 " + rows.size() + " 张，运输中 " + inTransit + " 张。",
                List.of("运输中：" + inTransit, "计划日期已过未送达：" + overdue),
                risks,
                List.of("对运输中单据补充承运商、车牌、司机和跟踪号。", "对已送达物流单检查是否应触发库存或应收应付后续处理。"),
                rows.stream().limit(limit).map(item -> evidence("SHIPMENT", item.id(), item.shipmentNo(), item.carrierName(), item.status(), null, item.plannedShipDate(), "/shipments")).toList()
        );
    }

    private BusinessAgentModuleResult analyzeInventory(String question, int limit, Set<PermissionCode> permissions) {
        if (!hasAny(permissions, PermissionCode.INVENTORY_MANAGE, PermissionCode.REPORT_VIEW)) {
            return unauthorized("inventory", "库存", "当前用户无库存或报表查看权限。");
        }
        List<InventoryView> ledgers = inventoryService.list(null, null, businessNo(question), null, keyword(question), null, null, null, businessNo(question));
        List<InventoryMaterialStockView> stocks = inventoryService.materialStock();
        List<InventoryMaterialStockView> flatStocks = flattenStocks(stocks);
        long negative = flatStocks.stream().filter(item -> amount(item.stockQuantity()).compareTo(BigDecimal.ZERO) < 0).count();
        List<String> risks = new ArrayList<>();
        if (negative > 0) {
            risks.add("有 " + negative + " 个物料库存为负数，需要复核出库、调拨或期初数据。");
        }
        return new BusinessAgentModuleResult(
                "inventory",
                "库存",
                true,
                "命中库存流水 " + ledgers.size() + " 条，物料库存统计 " + flatStocks.size() + " 项。",
                List.of("负库存物料数：" + negative, "库存流水数：" + ledgers.size()),
                risks,
                List.of("优先复核负库存物料。", "按相关业务单号追踪采购收货、物流送达和库存入库是否一致。"),
                ledgers.stream().limit(limit).map(item -> evidence("INVENTORY_LEDGER", item.id(), item.movementNo(), item.itemName(), item.movementType(), item.quantity(), item.movementDate(), "/inventory")).toList()
        );
    }

    private BusinessAgentModuleResult analyzeArAp(String question, int limit, Set<PermissionCode> permissions) {
        if (!hasAny(permissions, PermissionCode.AR_AP_MANAGE, PermissionCode.REPORT_VIEW)) {
            return unauthorized("arAp", "应收应付", "当前用户无应收应付或报表查看权限。");
        }
        List<ArApView> rows = arApService.list(null, null, businessNo(question), null, keyword(question), null, null, null);
        LocalDate today = LocalDate.now();
        long unsettled = rows.stream().filter(item -> amount(item.remainingAmountCny()).compareTo(BigDecimal.ZERO) > 0).count();
        long overdue = rows.stream().filter(item -> amount(item.remainingAmountCny()).compareTo(BigDecimal.ZERO) > 0
                && item.dueDate() != null && item.dueDate().isBefore(today)).count();
        List<String> risks = new ArrayList<>();
        if (unsettled > 0) {
            risks.add("有 " + unsettled + " 张应收应付单存在未结金额。");
        }
        if (overdue > 0) {
            risks.add("有 " + overdue + " 张应收应付单已到期或逾期未结。");
        }
        return new BusinessAgentModuleResult(
                "arAp",
                "应收应付",
                true,
                "命中应收应付单 " + rows.size() + " 张，未结人民币金额 " + money(rows.stream().map(ArApView::remainingAmountCny).toList()) + "。",
                List.of("未结单据：" + unsettled, "逾期未结：" + overdue),
                risks,
                List.of("优先处理逾期未结单据。", "核对采购、收货、付款和凭证是否形成完整链路。"),
                rows.stream().limit(limit).map(item -> evidence("AR_AP_BILL", item.id(), item.billNo(), item.partnerName(), item.status(), item.remainingAmountCny(), item.dueDate(), "/ar-ap")).toList()
        );
    }

    private BusinessAgentModuleResult analyzeFinance(String question, int limit, Set<PermissionCode> permissions) {
        if (!hasAny(permissions, PermissionCode.FINANCE_VOUCHER_MANAGE, PermissionCode.REPORT_VIEW)) {
            return unauthorized("finance", "财务", "当前用户无凭证或报表查看权限。");
        }
        List<VoucherView> rows = financeService.listVouchers(null, null, businessNo(question), null, null, null, null, null, null);
        long draft = rows.stream().filter(item -> item.status() == VoucherStatus.DRAFT).count();
        long unbalanced = rows.stream().filter(item -> amount(item.totalDebitCny()).compareTo(amount(item.totalCreditCny())) != 0).count();
        List<String> risks = new ArrayList<>();
        if (draft > 0) {
            risks.add("有 " + draft + " 张凭证仍为草稿，建议检查是否需要过账。");
        }
        if (unbalanced > 0) {
            risks.add("有 " + unbalanced + " 张凭证借贷不平，需要立即复核。");
        }
        return new BusinessAgentModuleResult(
                "finance",
                "财务",
                true,
                "命中凭证 " + rows.size() + " 张，借方人民币合计 " + money(rows.stream().map(VoucherView::totalDebitCny).toList()) + "。",
                List.of("草稿凭证：" + draft, "借贷不平：" + unbalanced),
                risks,
                List.of("对草稿凭证检查附件、来源单据和分录完整性。", "过账前必须确认借贷平衡和所属会计期间。"),
                rows.stream().limit(limit).map(item -> evidence("VOUCHER", item.id(), item.voucherNo(), item.summary(), item.status(), item.totalDebitCny(), item.voucherDate(), "/vouchers")).toList()
        );
    }

    private BusinessAgentModuleResult analyzeWorkflow(String question, int limit, Set<PermissionCode> permissions) {
        if (!permissions.contains(PermissionCode.WORKFLOW_USE)) {
            return unauthorized("workflow", "审批", "当前用户无审批中心权限。");
        }
        List<WorkflowItemView> todos = workflowService.todoItems(null, keyword(question), null, null, null, null, null);
        List<WorkflowItemView> started = workflowService.startedItems(null, keyword(question), null, null, null, null, null);
        List<String> risks = new ArrayList<>();
        if (!todos.isEmpty()) {
            risks.add("当前用户有 " + todos.size() + " 条审批待办。");
        }
        return new BusinessAgentModuleResult(
                "workflow",
                "审批",
                true,
                "当前待办 " + todos.size() + " 条，发起事宜 " + started.size() + " 条。",
                List.of("待办：" + todos.size(), "发起：" + started.size()),
                risks,
                List.of("优先处理等待当前用户审批的待办。", "审批意见可由 Agent 生成草稿，但最终同意或不同意必须人工确认。"),
                todos.stream().limit(limit).map(item -> new BusinessAgentEvidence("WORKFLOW", item.instanceId(), item.businessNo(), item.title(), item.status() == null ? "" : item.status().name(), "", item.startedTime(), "/workflow-center")).toList()
        );
    }

    private List<BusinessAgentCapabilityResult> buildCapabilityResults(String question, List<String> agentTypes, int limit, Set<PermissionCode> permissions) {
        List<BusinessAgentCapabilityResult> results = new ArrayList<>();
        for (String agentType : agentTypes) {
            results.add(switch (agentType) {
                case "query" -> queryAgent(question, limit, permissions);
                case "reconciliation" -> reconciliationAgent(question, limit, permissions);
                case "voucherSuggestion" -> voucherSuggestionAgent(question, limit, permissions);
                case "dueReminder" -> dueReminderAgent(question, limit, permissions);
                case "workflowAssistant" -> workflowAssistantAgent(question, limit, permissions);
                case "inventoryRisk" -> inventoryRiskAgent(question, limit, permissions);
                case "businessAnalysis" -> businessAnalysisAgent(question, limit, permissions);
                case "knowledgeQa" -> knowledgeQaAgent(question, limit, permissions);
                default -> unavailable(agentType, agentName(agentType), "暂不支持该 Agent 能力。");
            });
        }
        return results;
    }

    private BusinessAgentCapabilityResult queryAgent(String question, int limit, Set<PermissionCode> permissions) {
        List<BusinessAgentModuleResult> modules = selectedModules(question, List.of(), permissions).stream()
                .map(module -> analyzeModule(module, question, limit, permissions))
                .toList();
        return new BusinessAgentCapabilityResult(
                "query",
                "查询型 Agent",
                true,
                "已按自然语言问题查询采购、物流、库存、应收应付、凭证和审批中的授权数据。",
                modules.stream().flatMap(item -> item.findings().stream()).distinct().toList(),
                modules.stream().flatMap(item -> item.risks().stream()).distinct().toList(),
                List.of("点击证据中的 route 可进入对应页面复核。", "涉及金额、状态和单号时，以证据列表为准。"),
                List.of("查询结论草稿：围绕返回证据逐条说明，不补充未查询到的业务事实。"),
                modules.stream().flatMap(item -> item.evidences().stream()).limit((long) limit * 3).toList()
        );
    }

    private BusinessAgentCapabilityResult reconciliationAgent(String question, int limit, Set<PermissionCode> permissions) {
        if (!hasAny(permissions, PermissionCode.PURCHASE_MANAGE, PermissionCode.INVENTORY_MANAGE, PermissionCode.AR_AP_MANAGE, PermissionCode.FINANCE_VOUCHER_MANAGE, PermissionCode.REPORT_VIEW)) {
            return unavailable("reconciliation", "对账检查 Agent", "当前用户缺少采购、库存、应收应付、凭证或报表权限。");
        }
        List<PurchaseOrderView> purchases = operationService.listPurchaseOrders(null, null, businessNo(question), keyword(question), null, null, null, null);
        List<InventoryView> ledgers = inventoryService.list(null, null, businessNo(question), null, keyword(question), null, null, null, businessNo(question));
        List<ArApView> arAps = arApService.list(null, null, businessNo(question), null, keyword(question), null, null, null);
        List<VoucherView> vouchers = financeService.listVouchers(null, null, businessNo(question), null, null, null, null, null, null);
        List<CashierTransactionView> cashiers = cashierService.list(null, null, null, null, null, keyword(question), businessNo(question));
        long purchaseNoVoucher = purchases.stream().filter(item -> blank(item.voucherNo())).count();
        long arApUnsettled = arAps.stream().filter(item -> amount(item.remainingAmountCny()).compareTo(BigDecimal.ZERO) > 0).count();
        long cashierNoVoucher = cashiers.stream().filter(item -> blank(item.voucherNo()) && item.status() == CashierTransactionStatus.CONFIRMED).count();
        List<String> risks = new ArrayList<>();
        if (purchaseNoVoucher > 0) risks.add("有 " + purchaseNoVoucher + " 张采购单未关联凭证。");
        if (arApUnsettled > 0) risks.add("有 " + arApUnsettled + " 张应收应付单仍有未结余额。");
        if (cashierNoVoucher > 0) risks.add("有 " + cashierNoVoucher + " 条已确认出纳流水未制证。");
        return new BusinessAgentCapabilityResult(
                "reconciliation",
                "对账检查 Agent",
                true,
                "已检查采购 " + purchases.size() + " 张、库存流水 " + ledgers.size() + " 条、应收应付 " + arAps.size() + " 张、凭证 " + vouchers.size() + " 张、出纳流水 " + cashiers.size() + " 条。",
                List.of("采购未制证：" + purchaseNoVoucher, "应收应付未结：" + arApUnsettled, "已确认出纳未制证：" + cashierNoVoucher),
                risks,
                List.of("优先按业务单号核对采购、库存、应付、付款和凭证链路。", "如果同一业务单号缺少后续单据，应回到对应模块补齐或说明原因。"),
                List.of("对账结论草稿：列出缺少凭证、未结余额、未制证出纳流水，并要求人工复核来源单号。"),
                combineEvidences(
                        purchases.stream().limit(limit).map(item -> evidence("PURCHASE_ORDER", item.id(), item.orderNo(), item.supplierName(), item.status(), item.totalAmountCny(), item.orderDate(), "/purchase-orders")).toList(),
                        arAps.stream().limit(limit).map(item -> evidence("AR_AP_BILL", item.id(), item.billNo(), item.partnerName(), item.status(), item.remainingAmountCny(), item.dueDate(), "/ar-ap")).toList(),
                        vouchers.stream().limit(limit).map(item -> evidence("VOUCHER", item.id(), item.voucherNo(), item.summary(), item.status(), item.totalDebitCny(), item.voucherDate(), "/vouchers")).toList()
                )
        );
    }

    private BusinessAgentCapabilityResult voucherSuggestionAgent(String question, int limit, Set<PermissionCode> permissions) {
        if (!hasAny(permissions, PermissionCode.FINANCE_VOUCHER_MANAGE, PermissionCode.REPORT_VIEW)) {
            return unavailable("voucherSuggestion", "凭证建议 Agent", "当前用户无凭证或报表查看权限。");
        }
        List<PurchaseOrderView> purchases = operationService.listPurchaseOrders(null, null, businessNo(question), keyword(question), null, null, null, null);
        List<InventoryView> ledgers = inventoryService.list(null, null, businessNo(question), null, keyword(question), null, null, null, businessNo(question));
        List<ArApView> arAps = arApService.list(null, null, businessNo(question), null, keyword(question), null, null, null);
        List<CashierTransactionView> cashiers = cashierService.list(null, null, null, CashierTransactionStatus.CONFIRMED, null, keyword(question), businessNo(question));
        long purchaseDrafts = purchases.stream().filter(item -> blank(item.voucherNo())).count();
        long inventoryDrafts = ledgers.stream().filter(item -> blank(item.voucherNo())).count();
        long arApDrafts = arAps.stream().filter(item -> blank(item.voucherNo())).count();
        long cashierDrafts = cashiers.stream().filter(item -> blank(item.voucherNo())).count();
        return new BusinessAgentCapabilityResult(
                "voucherSuggestion",
                "凭证建议 Agent",
                true,
                "可建议制证来源：采购 " + purchaseDrafts + "、库存 " + inventoryDrafts + "、应收应付 " + arApDrafts + "、出纳 " + cashierDrafts + "。",
                List.of("采购待制证：" + purchaseDrafts, "库存待制证：" + inventoryDrafts, "应收应付待制证：" + arApDrafts, "出纳待制证：" + cashierDrafts),
                List.of("凭证建议只生成草稿，不自动过账。"),
                List.of("在会计平台选择来源单据后补充借贷科目。", "过账前人工确认摘要、项目、币种、汇率和借贷平衡。"),
                List.of("凭证草稿建议：按来源单号生成摘要，金额使用业务发生时人民币金额，借贷科目由财务人员在会计平台确认。"),
                combineEvidences(
                        purchases.stream().filter(item -> blank(item.voucherNo())).limit(limit).map(item -> evidence("PURCHASE_ORDER", item.id(), item.orderNo(), item.supplierName(), item.status(), item.totalAmountCny(), item.orderDate(), "/accounting-platform")).toList(),
                        cashiers.stream().filter(item -> blank(item.voucherNo())).limit(limit).map(item -> evidence("CASHIER_TRANSACTION", item.id(), item.transactionNo(), item.summary(), item.status(), item.amountCny(), item.transactionDate(), "/accounting-platform")).toList()
                )
        );
    }

    private BusinessAgentCapabilityResult dueReminderAgent(String question, int limit, Set<PermissionCode> permissions) {
        if (!hasAny(permissions, PermissionCode.AR_AP_MANAGE, PermissionCode.REPORT_VIEW)) {
            return unavailable("dueReminder", "到期提醒 Agent", "当前用户无应收应付或报表权限。");
        }
        LocalDate today = LocalDate.now();
        List<ArApView> rows = arApService.list(null, null, businessNo(question), null, keyword(question), null, null, null);
        List<ArApView> due = rows.stream()
                .filter(item -> amount(item.remainingAmountCny()).compareTo(BigDecimal.ZERO) > 0)
                .filter(item -> item.dueDate() != null && !item.dueDate().isAfter(today.plusDays(7)))
                .toList();
        long overdue = due.stream().filter(item -> item.dueDate() != null && item.dueDate().isBefore(today)).count();
        return new BusinessAgentCapabilityResult(
                "dueReminder",
                "到期提醒 Agent",
                true,
                "未来 7 天到期或已逾期未结单据 " + due.size() + " 张，其中逾期 " + overdue + " 张。",
                List.of("到期/逾期：" + due.size(), "逾期：" + overdue, "未结金额：" + money(due.stream().map(ArApView::remainingAmountCny).toList())),
                overdue > 0 ? List.of("存在逾期未结往来，需要优先处理。") : List.of(),
                List.of("按到期日升序联系往来单位。", "核对是否已有付款或收款但未核销。"),
                List.of("提醒草稿：说明单号、往来单位、到期日、未结金额，并要求业务人员确认收付款计划。"),
                due.stream().limit(limit).map(item -> evidence("AR_AP_BILL", item.id(), item.billNo(), item.partnerName(), item.status(), item.remainingAmountCny(), item.dueDate(), "/ar-ap")).toList()
        );
    }

    private BusinessAgentCapabilityResult workflowAssistantAgent(String question, int limit, Set<PermissionCode> permissions) {
        if (!permissions.contains(PermissionCode.WORKFLOW_USE)) {
            return unavailable("workflowAssistant", "流程助手 Agent", "当前用户无审批中心权限。");
        }
        List<WorkflowItemView> todos = workflowService.todoItems(null, keyword(question), null, null, null, null, null);
        List<WorkflowItemView> started = workflowService.startedItems(null, keyword(question), null, null, null, null, null);
        return new BusinessAgentCapabilityResult(
                "workflowAssistant",
                "流程助手 Agent",
                true,
                "当前待办 " + todos.size() + " 条，发起流程 " + started.size() + " 条。",
                List.of("待办：" + todos.size(), "发起：" + started.size()),
                todos.isEmpty() ? List.of() : List.of("当前用户存在待审批任务。"),
                List.of("优先处理最早发起的待办。", "审批意见草稿必须由审批人最终确认。"),
                List.of("同意意见草稿：同意申请，请按流程继续办理。", "不同意意见草稿：不同意申请，请补充原因、附件或修正业务数据后重新提交。"),
                todos.stream().limit(limit).map(item -> new BusinessAgentEvidence("WORKFLOW", item.instanceId(), item.businessNo(), item.title(), item.status() == null ? "" : item.status().name(), "", item.startedTime(), "/workflow-center")).toList()
        );
    }

    private BusinessAgentCapabilityResult inventoryRiskAgent(String question, int limit, Set<PermissionCode> permissions) {
        if (!hasAny(permissions, PermissionCode.INVENTORY_MANAGE, PermissionCode.PURCHASE_MANAGE, PermissionCode.REPORT_VIEW)) {
            return unavailable("inventoryRisk", "库存风险 Agent", "当前用户缺少库存、采购或报表权限。");
        }
        List<InventoryView> ledgers = inventoryService.list(null, null, businessNo(question), null, keyword(question), null, null, null, businessNo(question));
        List<InventoryMaterialStockView> stocks = flattenStocks(inventoryService.materialStock());
        List<PurchaseOrderView> purchases = operationService.listPurchaseOrders(null, null, businessNo(question), keyword(question), null, null, null, null);
        long negative = stocks.stream().filter(item -> amount(item.stockQuantity()).compareTo(BigDecimal.ZERO) < 0).count();
        long low = stocks.stream().filter(item -> amount(item.stockQuantity()).compareTo(BigDecimal.ZERO) > 0 && amount(item.stockQuantity()).compareTo(new BigDecimal("10")) <= 0).count();
        long transfers = ledgers.stream().filter(item -> item.movementType() == InventoryMovementType.TRANSFER).count();
        long receivedNoLedger = purchases.stream().filter(item -> item.status() == PurchaseStatus.PURCHASE_COMPLETED)
                .filter(item -> ledgers.stream().noneMatch(ledger -> value(ledger.relatedBizNo()).contains(value(item.orderNo()))))
                .count();
        List<String> risks = new ArrayList<>();
        if (negative > 0) risks.add("存在 " + negative + " 个负库存物料。");
        if (low > 0) risks.add("存在 " + low + " 个低库存物料，阈值暂按 10 判断。");
        if (receivedNoLedger > 0) risks.add("有 " + receivedNoLedger + " 张采购完成单未在库存流水中匹配到来源单号。");
        return new BusinessAgentCapabilityResult(
                "inventoryRisk",
                "库存风险 Agent",
                true,
                "已检查库存流水 " + ledgers.size() + " 条、物料库存 " + stocks.size() + " 项、采购单 " + purchases.size() + " 张。",
                List.of("负库存：" + negative, "低库存：" + low, "调拨流水：" + transfers, "采购完成未匹配入库：" + receivedNoLedger),
                risks,
                List.of("优先复核负库存和采购完成未入库。", "低库存阈值后续可改为物料字典安全库存字段。"),
                List.of("库存风险草稿：列出负库存、低库存、调拨异常和采购完成未入库单据，要求仓库人员复核。"),
                ledgers.stream().limit(limit).map(item -> evidence("INVENTORY_LEDGER", item.id(), item.movementNo(), item.itemName(), item.movementType(), item.quantity(), item.movementDate(), "/inventory")).toList()
        );
    }

    private BusinessAgentCapabilityResult businessAnalysisAgent(String question, int limit, Set<PermissionCode> permissions) {
        if (!permissions.contains(PermissionCode.REPORT_VIEW)) {
            return unavailable("businessAnalysis", "经营分析 Agent", "当前用户无统计报表查看权限。");
        }
        BusinessMetricsService.BusinessMetricsSnapshot metrics = businessMetricsService.snapshot(businessNo(question), keyword(question), permissions);
        List<String> risks = new ArrayList<>();
        if (metrics.remainingTotal().compareTo(BigDecimal.ZERO) > 0) {
            risks.add("存在未结往来余额，影响现金流判断。");
        }
        if (metrics.negativeStockKinds() > 0) {
            risks.add("存在 " + metrics.negativeStockKinds() + " 个负库存物料，经营结论需要结合库存准确性复核。");
        }
        return new BusinessAgentCapabilityResult(
                "businessAnalysis",
                "经营分析 Agent",
                true,
                "采购金额 " + metrics.purchaseTotal().toPlainString() + "，往来未结 " + metrics.remainingTotal().toPlainString() + "，有库存物料 " + metrics.materialKinds() + " 项。",
                List.of("采购人民币金额：" + metrics.purchaseTotal().toPlainString(), "往来未结金额：" + metrics.remainingTotal().toPlainString(), "有库存物料数：" + metrics.materialKinds(), "负库存物料数：" + metrics.negativeStockKinds()),
                risks,
                List.of("可按项目、供应商、客户和物料继续拆分分析。", "经营结论必须结合当前筛选条件和证据，不应外推到全公司。"),
                List.of("经营分析草稿：说明采购规模、未结往来、库存结构和待跟进风险。"),
                metrics.purchases().stream().limit(limit).map(item -> evidence("PURCHASE_ORDER", item.id(), item.orderNo(), item.supplierName(), item.status(), item.totalAmountCny(), item.orderDate(), "/purchase-orders")).toList()
        );
    }

    private BusinessAgentCapabilityResult knowledgeQaAgent(String question, int limit, Set<PermissionCode> permissions) {
        if (!permissions.contains(PermissionCode.AI_ASSISTANT_USE)) {
            return unavailable("knowledgeQa", "附件/知识问答 Agent", "当前用户无 AI 助手使用权限。");
        }
        List<KnowledgeSearchResult> rows = value(question).isBlank() ? List.of() : knowledgeSearchService.search(value(question), "hybrid", limit).results();
        return new BusinessAgentCapabilityResult(
                "knowledgeQa",
                "附件/知识问答 Agent",
                true,
                "已从本地知识库、附件文本和系统业务索引中召回 " + rows.size() + " 条资料。",
                rows.stream().map(item -> item.title() + " / " + item.category()).toList(),
                rows.isEmpty() ? List.of("未召回可引用资料，不能确认该问题。") : List.of(),
                List.of("回答必须引用召回资料。", "资料不足时应明确说明无法确认。"),
                List.of("知识问答草稿：基于召回资料回答，逐条列出来源；缺少来源时回答“不足以判断”。"),
                rows.stream().map(item -> new BusinessAgentEvidence(item.type(), item.sourceId(), item.sourceNo(), item.title(), item.category(), String.valueOf(item.score()), "", item.routePath())).toList()
        );
    }

    private List<BusinessAgentAction> buildActions(String stage, List<BusinessAgentModuleResult> results, List<BusinessAgentCapabilityResult> capabilityResults, List<String> risks) {
        if ("readOnly".equals(stage)) {
            return List.of();
        }
        List<BusinessAgentAction> actions = new ArrayList<>();
        int step = 1;
        for (BusinessAgentModuleResult result : results) {
            if (!result.authorized()) {
                continue;
            }
            if ("draft".equals(stage)) {
                actions.add(draftAction(step++, result));
            } else if ("controlled".equals(stage)) {
                actions.add(controlledAction(step++, result));
            } else if ("multiStep".equals(stage)) {
                actions.add(draftAction(step++, result));
                actions.add(controlledAction(step++, result));
            }
        }
        for (BusinessAgentCapabilityResult result : capabilityResults) {
            if (!result.available()) {
                continue;
            }
            if ("draft".equals(stage) || "multiStep".equals(stage)) {
                actions.add(new BusinessAgentAction(
                        String.valueOf(step++),
                        "draft",
                        result.agentType(),
                        "GENERATE_AGENT_DRAFT",
                        false,
                        false,
                        true,
                        "生成" + result.agentName() + "草稿",
                        "根据该 Agent 的证据、风险和建议生成可复制到业务页面的处理说明草稿。",
                        List.of("必须引用返回的业务证据", "不能把草稿当作已执行结果"),
                        List.of()
                ));
            }
            if ("controlled".equals(stage) || "multiStep".equals(stage)) {
                actions.add(new BusinessAgentAction(
                        String.valueOf(step++),
                        "controlled",
                        result.agentType(),
                        "PREPARE_AGENT_CONTROLLED_ACTION",
                        true,
                        true,
                        false,
                        "准备" + result.agentName() + "受控执行",
                        "为后续生成凭证草稿、创建跟进任务、提交审批或更新状态准备计划。",
                        List.of("用户明确确认", "服务端写入前重新查询业务数据", "写入 Agent 审计记录"),
                        List.of("当前版本没有确认令牌、Agent 审计表和工具白名单，禁止自动执行。")
                ));
            }
        }
        if ("multiStep".equals(stage) && !risks.isEmpty()) {
            actions.add(new BusinessAgentAction(
                    String.valueOf(step),
                    stage,
                    "workflow",
                    "FOLLOW_UP_PLAN",
                    false,
                    true,
                    false,
                    "生成跨模块跟进计划",
                    "把采购、物流、库存、应收应付、财务和审批中的风险项汇总为人工跟进清单。",
                    List.of("用户确认风险项属实", "确认需要生成跟进计划"),
                    List.of("当前尚未接入 Agent 审计表和用户确认令牌，因此只返回计划，不自动创建任务。")
            ));
        }
        return actions;
    }

    private BusinessAgentAction draftAction(int step, BusinessAgentModuleResult result) {
        return new BusinessAgentAction(
                String.valueOf(step),
                "draft",
                result.module(),
                "GENERATE_DRAFT",
                false,
                false,
                true,
                "生成" + result.moduleName() + "处理草稿",
                "根据已命中的业务证据生成处理说明、审批意见或核对建议草稿。",
                List.of("至少存在一条授权读取结果", "草稿内容必须引用返回的业务证据"),
                List.of()
        );
    }

    private BusinessAgentAction controlledAction(int step, BusinessAgentModuleResult result) {
        return new BusinessAgentAction(
                String.valueOf(step),
                "controlled",
                result.module(),
                "PREPARE_CONTROLLED_ACTION",
                true,
                true,
                false,
                "准备" + result.moduleName() + "受控执行",
                "为后续保存、提交审批、生成凭证、生成应收应付或创建跟进任务准备执行计划。",
                List.of("当前用户具有目标业务写权限", "用户在前端确认执行内容", "服务端写入前重新查询并校验业务状态"),
                List.of("当前版本未接入确认令牌、Agent 审计表和写操作工具白名单，禁止自动执行。")
        );
    }

    private List<BusinessAgentSelfCheck> buildSelfChecks(String stage, List<BusinessAgentModuleResult> results, List<BusinessAgentAction> actions) {
        if (!aiProperties.getAgent().isSelfCheckEnabled()) {
            return List.of(new BusinessAgentSelfCheck("关键 Agent 自检开关", true, "INFO", "配置已关闭自检，仅返回基础执行边界。"));
        }
        List<BusinessAgentSelfCheck> checks = new ArrayList<>();
        checks.add(new BusinessAgentSelfCheck(
                "权限边界",
                results.stream().allMatch(item -> item.authorized() || item.evidences().isEmpty()),
                "HIGH",
                "未授权模块不会返回业务证据。"
        ));
        checks.add(new BusinessAgentSelfCheck(
                "证据约束",
                results.stream().filter(BusinessAgentModuleResult::authorized).allMatch(item -> item.summary() != null && item.findings() != null),
                "HIGH",
                "每个授权模块必须有摘要和结构化发现，回答不能脱离业务查询结果。"
        ));
        checks.add(new BusinessAgentSelfCheck(
                "写操作阻断",
                actions.stream().filter(BusinessAgentAction::writeOperation).noneMatch(BusinessAgentAction::executable),
                "CRITICAL",
                "涉及写操作的受控执行计划在当前版本均不可直接执行。"
        ));
        checks.add(new BusinessAgentSelfCheck(
                "阶段顺序",
                stageOrder(stage) >= 1,
                "MEDIUM",
                "Agent 阶段必须属于 readOnly、draft、controlled、multiStep。"
        ));
        return checks;
    }

    private String normalizeStage(String stage) {
        return businessAgentSelector.normalizeStage(stage);
    }

    private int stageOrder(String stage) {
        return businessAgentSelector.stageOrder(stage);
    }

    private List<String> selectedModules(String question, List<String> requested, Set<PermissionCode> permissions) {
        return businessAgentSelector.selectedModules(question, requested, permissions);
    }

    private List<String> selectedAgentTypes(String question, List<String> requested) {
        return businessAgentSelector.selectedAgentTypes(question, requested);
    }

    private BusinessAgentModuleResult unauthorized(String module, String moduleName, String reason) {
        return new BusinessAgentModuleResult(module, moduleName, false, reason, List.of(), List.of(reason), List.of(), List.of());
    }

    private BusinessAgentCapabilityResult unavailable(String agentType, String agentName, String reason) {
        return new BusinessAgentCapabilityResult(agentType, agentName, false, reason, List.of(), List.of(reason), List.of(), List.of(), List.of());
    }

    private BusinessAgentEvidence evidence(String type, Long id, String no, String title, Object status, BigDecimal amount, LocalDate date, String route) {
        return new BusinessAgentEvidence(type, id, value(no), value(title), status == null ? "" : status.toString(), amount == null ? "" : amount.toPlainString(), date == null ? "" : date.toString(), route);
    }

    private List<InventoryMaterialStockView> flattenStocks(List<InventoryMaterialStockView> rows) {
        List<InventoryMaterialStockView> result = new ArrayList<>();
        if (rows == null) {
            return result;
        }
        for (InventoryMaterialStockView row : rows) {
            result.add(row);
            result.addAll(flattenStocks(row.children()));
        }
        return result;
    }

    private String overallSummary(List<BusinessAgentModuleResult> results, List<BusinessAgentCapabilityResult> capabilityResults, List<String> risks) {
        long authorized = results.stream().filter(BusinessAgentModuleResult::authorized).count();
        long available = capabilityResults.stream().filter(BusinessAgentCapabilityResult::available).count();
        return "已分析 " + authorized + " 个授权业务模块、" + available + " 类 Agent 能力，发现 " + risks.size() + " 条风险或关注点。";
    }

    private String keyword(String question) {
        String value = value(question);
        return value.isBlank() ? null : value;
    }

    private String businessNo(String question) {
        String text = value(question);
        java.util.regex.Matcher matcher = java.util.regex.Pattern.compile("[A-Za-z]{1,8}[-_]?\\d{4,}|\\d{6,}").matcher(text);
        return matcher.find() ? matcher.group() : null;
    }

    private String normalizeModule(String module) {
        return businessAgentSelector.normalizeModule(module);
    }

    private String normalizeAgentType(String agentType) {
        return businessAgentSelector.normalizeAgentType(agentType);
    }

    private String moduleName(String module) {
        return switch (module) {
            case "purchase" -> "采购";
            case "shipment" -> "物流";
            case "inventory" -> "库存";
            case "arAp" -> "应收应付";
            case "finance" -> "财务";
            case "workflow" -> "审批";
            default -> module;
        };
    }

    private String agentName(String agentType) {
        return switch (agentType) {
            case "query" -> "查询型 Agent";
            case "reconciliation" -> "对账检查 Agent";
            case "voucherSuggestion" -> "凭证建议 Agent";
            case "dueReminder" -> "到期提醒 Agent";
            case "workflowAssistant" -> "流程助手 Agent";
            case "inventoryRisk" -> "库存风险 Agent";
            case "businessAnalysis" -> "经营分析 Agent";
            case "knowledgeQa" -> "附件/知识问答 Agent";
            default -> agentType;
        };
    }

    private boolean hasAny(Set<PermissionCode> permissions, PermissionCode... codes) {
        for (PermissionCode code : codes) {
            if (permissions.contains(code)) {
                return true;
            }
        }
        return false;
    }

    private boolean containsAny(String text, String... terms) {
        for (String term : terms) {
            if (text.contains(term)) {
                return true;
            }
        }
        return false;
    }

    private boolean blank(String value) {
        return value == null || value.isBlank();
    }

    @SafeVarargs
    private final List<BusinessAgentEvidence> combineEvidences(List<BusinessAgentEvidence>... groups) {
        List<BusinessAgentEvidence> result = new ArrayList<>();
        for (List<BusinessAgentEvidence> group : groups) {
            result.addAll(group);
        }
        return result;
    }

    private List<String> combineDistinct(List<String> first, List<String> second) {
        LinkedHashSet<String> result = new LinkedHashSet<>();
        result.addAll(first);
        result.addAll(second);
        return result.stream().toList();
    }

    private BigDecimal amount(BigDecimal value) {
        return value == null ? BigDecimal.ZERO : value;
    }

    private String money(List<BigDecimal> values) {
        return values.stream().map(this::amount).reduce(BigDecimal.ZERO, BigDecimal::add).toPlainString();
    }

    private String value(String value) {
        return value == null ? "" : value.trim();
    }
}
