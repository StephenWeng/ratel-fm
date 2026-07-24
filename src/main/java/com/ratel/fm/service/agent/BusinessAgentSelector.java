package com.ratel.fm.service.agent;

import com.ratel.fm.domain.auth.PermissionCode;
import com.ratel.fm.service.assistant.FinancialIntentTerms;
import org.springframework.stereotype.Component;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * 业务 Agent 选择器。
 *
 * <p>负责规范化 Agent 阶段、业务模块和 Agent 能力类型，避免编排服务混入意图选择细节。</p>
 */
@Component
public class BusinessAgentSelector {

    public String normalizeStage(String stage) {
        String value = stage == null ? "" : stage.trim().toLowerCase();
        return switch (value) {
            case "", "readonly", "read_only", "read-only", "只读" -> "readOnly";
            case "draft", "草稿" -> "draft";
            case "controlled", "controlled_execution", "受控执行" -> "controlled";
            case "multistep", "multi_step", "multi-step", "多步骤" -> "multiStep";
            default -> "readOnly";
        };
    }

    public int stageOrder(String stage) {
        return switch (stage) {
            case "readOnly" -> 1;
            case "draft" -> 2;
            case "controlled" -> 3;
            case "multiStep" -> 4;
            default -> 0;
        };
    }

    public List<String> selectedModules(String question, List<String> requested, Set<PermissionCode> permissions) {
        LinkedHashSet<String> modules = new LinkedHashSet<>();
        if (requested != null) {
            requested.stream().map(this::normalizeModule).filter(item -> !item.isBlank()).forEach(modules::add);
        }
        if (modules.isEmpty()) {
            modules.addAll(FinancialIntentTerms.selectedModules(question));
        }
        if (modules.isEmpty()) {
            if (hasAny(permissions, PermissionCode.PURCHASE_MANAGE, PermissionCode.REPORT_VIEW)) modules.add("purchase");
            if (hasAny(permissions, PermissionCode.LOGISTICS_MANAGE, PermissionCode.REPORT_VIEW)) modules.add("shipment");
            if (hasAny(permissions, PermissionCode.INVENTORY_MANAGE, PermissionCode.REPORT_VIEW)) modules.add("inventory");
            if (hasAny(permissions, PermissionCode.AR_AP_MANAGE, PermissionCode.REPORT_VIEW)) modules.add("arAp");
            if (hasAny(permissions, PermissionCode.FINANCE_VOUCHER_MANAGE, PermissionCode.REPORT_VIEW)) modules.add("finance");
            if (permissions.contains(PermissionCode.WORKFLOW_USE)) modules.add("workflow");
        }
        return modules.stream().toList();
    }

    public List<String> selectedAgentTypes(String question, List<String> requested) {
        LinkedHashSet<String> agentTypes = new LinkedHashSet<>();
        if (requested != null) {
            requested.stream().map(this::normalizeAgentType).filter(item -> !item.isBlank()).forEach(agentTypes::add);
        }
        if (agentTypes.isEmpty()) {
            agentTypes.addAll(FinancialIntentTerms.selectedAgentTypes(question));
        }
        if (agentTypes.isEmpty()) {
            agentTypes.add("query");
            agentTypes.add("reconciliation");
            agentTypes.add("dueReminder");
            agentTypes.add("inventoryRisk");
            agentTypes.add("businessAnalysis");
            agentTypes.add("knowledgeQa");
        }
        return agentTypes.stream().toList();
    }

    public String normalizeModule(String module) {
        if (module == null || module.isBlank()) {
            return "";
        }
        return switch (module.trim().toLowerCase()) {
            case "purchase", "采购" -> "purchase";
            case "shipment", "logistics", "物流" -> "shipment";
            case "inventory", "库存" -> "inventory";
            case "arap", "ar_ap", "ar-ap", "应收应付" -> "arAp";
            case "finance", "accounting", "财务", "会计" -> "finance";
            case "workflow", "审批" -> "workflow";
            default -> module.trim();
        };
    }

    public String normalizeAgentType(String agentType) {
        if (agentType == null || agentType.isBlank()) {
            return "";
        }
        return switch (agentType.trim().toLowerCase()) {
            case "query", "查询" -> "query";
            case "reconciliation", "reconcile", "对账", "对账检查" -> "reconciliation";
            case "vouchersuggestion", "voucher_suggestion", "voucher-suggestion", "凭证建议", "制证建议" -> "voucherSuggestion";
            case "duereminder", "due_reminder", "due-reminder", "到期提醒", "逾期提醒" -> "dueReminder";
            case "workflowassistant", "workflow_assistant", "workflow-assistant", "流程助手" -> "workflowAssistant";
            case "inventoryrisk", "inventory_risk", "inventory-risk", "库存风险" -> "inventoryRisk";
            case "businessanalysis", "business_analysis", "business-analysis", "经营分析" -> "businessAnalysis";
            case "knowledgeqa", "knowledge_qa", "knowledge-qa", "知识问答" -> "knowledgeQa";
            default -> agentType.trim();
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
}
