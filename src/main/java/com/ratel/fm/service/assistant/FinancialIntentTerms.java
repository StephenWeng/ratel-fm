package com.ratel.fm.service.assistant;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/**
 * 财务业务专业术语表。
 *
 * <p>集中维护 ratel 助手、业务 Agent 和系统上下文路由使用的财务、经营、对账、制证、往来和库存术语，
 * 避免各入口各写一套关键词导致意图识别不一致。</p>
 */
public final class FinancialIntentTerms {

    public static final List<String> BUSINESS_ANALYSIS = List.of(
            "经营", "经营情况", "经营分析", "经营统计", "经营概览", "经营建议", "经营风险",
            "业绩", "收入", "成本", "费用", "利润", "毛利", "毛利率", "净利", "净利润",
            "息税前利润", "EBIT", "EBITDA", "ROI", "ROE", "ROA", "投入产出", "投资回报",
            "收支", "盈亏", "亏损", "盈利", "贡献", "边际贡献", "规模", "趋势", "预测", "同比", "环比",
            "占比", "结构", "排行", "排名", "汇总", "统计", "分析", "复盘", "看板", "指标", "KPI",
            "预算", "预算执行", "预算偏差", "滚动预测", "项目", "供应商", "客户", "物料", "品类", "部门", "账套",
            "生意怎么样", "赚不赚钱", "有没有赚钱", "钱花在哪", "哪里亏", "哪个项目挣钱", "哪个项目不赚钱"
    );

    public static final List<String> ACCOUNTING = List.of(
            "财务", "会计", "核算", "凭证", "分录", "借方", "贷方", "借贷", "过账", "反过账",
            "试算平衡", "科目", "会计科目", "辅助核算", "会计期间", "结账", "期初", "期末",
            "余额", "发生额", "本期发生", "累计发生", "摘要", "制证", "自动制证", "凭证建议",
            "生成凭证", "会计平台", "已制证", "未制证", "重复制证", "来源单据",
            "总账", "明细账", "日记账", "账簿", "未达账项", "暂估", "暂估入账", "冲回", "红冲",
            "摊销", "折旧", "预提", "递延", "权责发生制", "收付实现制", "关账", "月结", "月底关账",
            "账平不平", "借贷平不平", "这个月能不能关账", "还有哪些没做账", "单子没做凭证"
    );

    public static final List<String> REPORTING = List.of(
            "报表", "财务报表", "统计报表", "资产负债表", "利润表", "现金流量表",
            "资产", "负债", "所有者权益", "权益", "营业收入", "营业成本", "期间费用",
            "销售费用", "管理费用", "财务费用", "经营活动现金流量", "投资活动现金流量", "筹资活动现金流量",
            "现金流", "现金流量", "自由现金流", "资金缺口", "现金转换周期", "CCC", "营运资本", "净营运资本",
            "流动比率", "速动比率", "资产负债率", "试算", "平衡", "报表口径", "账上钱够不够", "现金够不够",
            "钱够不够付", "下个月够不够付"
    );

    public static final List<String> AR_AP = List.of(
            "应收", "应付", "应收应付", "往来", "往来账", "往来单位", "客户", "供应商",
            "收款", "付款", "已收", "已付", "待收", "待付", "未收", "未付", "未结",
            "未结余额", "余额", "核销", "未核销", "结算", "账龄", "到期", "逾期", "欠款", "欠钱", "钱还没",
            "坏账", "坏账准备", "信用减值", "减值准备", "呆账", "催收", "催款", "回款", "付款计划", "收付统计",
            "DSO", "DPO", "应收周转", "应付周转", "周转天数", "周转率", "账期", "信用期", "月结", "票到付款",
            "未到票", "到票", "发票匹配", "三单匹配", "欠了多久", "拖了多久", "谁还没给钱", "谁的钱没回来",
            "该收的钱", "该付的钱", "哪些快到期", "哪些拖过期", "催哪些客户", "先付哪家"
    );

    public static final List<String> CASHIER = List.of(
            "出纳", "流水", "资金", "现金", "银行", "收款流水", "付款流水", "转账", "退款",
            "账户", "银行账户", "确认流水", "取消流水", "资金计划", "现金流风险", "资金日报",
            "资金余额", "可用资金", "银行存款", "现金余额", "备用金", "付款排程", "收款排程",
            "账上余额", "账上的钱", "银行卡流水", "银行流水", "钱够不够", "先收后付"
    );

    public static final List<String> PURCHASE = List.of(
            "采购", "采购单", "采购订单", "采购金额", "供应商", "采购入库", "采购完成",
            "未入库", "付款条件", "交货条件", "采购部门", "采购员", "买东西", "买货"
    );

    public static final List<String> INVENTORY = List.of(
            "库存", "物料", "仓库", "入库", "出库", "调拨", "盘点", "库存流水",
            "物料库存", "库存数量", "负库存", "低库存", "安全库存", "库存风险", "收货", "入仓",
            "批号", "项目库存", "库存结构", "存货", "存货跌价", "存货跌价准备", "库龄", "呆滞料",
            "滞销", "周转率", "存货周转", "周转天数", "在途库存", "在途物资", "缺货", "积压",
            "东西压库", "货压太多", "哪些货不动", "哪些料很久没动", "库存够不够", "仓库数量对不上"
    );

    public static final List<String> LOGISTICS = List.of(
            "物流", "运输", "运单", "物流单", "承运", "承运商", "发货", "送达",
            "计划发运", "实际发运", "运输状态", "运费"
    );

    public static final List<String> RECONCILIATION = List.of(
            "对账", "核对", "勾稽", "一致", "不一致", "匹配", "链路", "闭环",
            "业务链路", "财务链路", "采购到付款", "收货到入库", "入库到应付",
            "应付到付款", "付款到凭证", "凭证链路", "差异", "异常"
            , "三单一致", "三流一致", "单据流", "资金流", "发票流", "合同流", "账实相符",
            "账账相符", "账表相符", "对不上", "不对账", "金额对不上", "数量对不上", "单号对不上",
            "货票款对不上", "票货不一致"
    );

    public static final List<String> WORKFLOW = List.of(
            "审批", "流程", "待办", "已办", "节点", "审批意见", "驳回", "同意",
            "流程卡", "流程助手", "下一步", "谁处理"
    );

    public static final List<String> KNOWLEDGE = List.of(
            "附件", "合同", "制度", "文档", "资料", "文件", "知识", "知识库",
            "发票", "单据附件", "上传", "简历", "候选人"
    );

    private FinancialIntentTerms() {
    }

    public static boolean isFinancialProfessionalQuestion(String text) {
        return containsAny(text, allFinancialTerms());
    }

    public static boolean isKnowledgeQuestion(String text) {
        return containsAny(text, KNOWLEDGE);
    }

    public static boolean isBusinessAnalysisQuestion(String text) {
        return containsAny(text, BUSINESS_ANALYSIS) || containsAny(text, REPORTING);
    }

    public static boolean isReasoningQuestion(String text) {
        return containsAny(text, BUSINESS_ANALYSIS)
                || containsAny(text, REPORTING)
                || containsAny(text, RECONCILIATION)
                || containsAny(text, "原因", "为什么", "风险", "异常", "建议", "优化", "预测", "趋势", "对比", "同比", "环比");
    }

    public static List<String> selectedModules(String text) {
        LinkedHashSet<String> modules = new LinkedHashSet<>();
        // 先放更具体的财务对象，再放“供应商、客户、项目”等可能跨模块的经营维度。
        if (containsAny(text, AR_AP) || containsAny(text, CASHIER)) {
            modules.add("arAp");
        }
        if (containsAny(text, ACCOUNTING) || containsAny(text, REPORTING) || containsAny(text, CASHIER)) {
            modules.add("finance");
        }
        if (containsAny(text, INVENTORY)) {
            modules.add("inventory");
        }
        if (containsAny(text, PURCHASE)) {
            modules.add("purchase");
        }
        if (containsAny(text, LOGISTICS)) {
            modules.add("shipment");
        }
        if (containsAny(text, WORKFLOW)) {
            modules.add("workflow");
        }
        if (isBusinessAnalysisQuestion(text) || containsAny(text, RECONCILIATION)) {
            modules.addAll(List.of("finance", "arAp", "purchase", "inventory", "shipment"));
        }
        return modules.stream().toList();
    }

    public static List<String> selectedAgentTypes(String text) {
        LinkedHashSet<String> agentTypes = new LinkedHashSet<>();
        if (containsAny(text, "查询", "查一下", "看看", "单号", "明细", "列表") || containsAny(text, ACCOUNTING) || containsAny(text, PURCHASE) || containsAny(text, LOGISTICS)) {
            agentTypes.add("query");
        }
        if (containsAny(text, RECONCILIATION)) {
            agentTypes.add("reconciliation");
        }
        if (containsAny(text, "制证", "凭证建议", "生成凭证", "会计平台", "未制证", "已制证", "来源单据")) {
            agentTypes.add("voucherSuggestion");
        }
        if (containsAny(text, "到期", "逾期", "未核销", "待收", "待付", "未结", "账龄", "账期", "回款", "付款计划",
                "催收", "催款", "DSO", "DPO", "应收周转", "应付周转", "周转天数", "资金缺口", "钱够不够",
                "够不够付", "账上钱", "现金转换周期", "营运资本")) {
            agentTypes.add("dueReminder");
        }
        if (containsAny(text, WORKFLOW)) {
            agentTypes.add("workflowAssistant");
        }
        if (containsAny(text, INVENTORY)) {
            agentTypes.add("inventoryRisk");
        }
        if (isBusinessAnalysisQuestion(text) || containsAny(text, "供应商", "客户", "项目", "物料", "毛利", "利润", "现金流",
                "存货周转", "周转天数", "库龄", "呆滞料", "积压", "营运资本", "现金转换周期")) {
            agentTypes.add("businessAnalysis");
        }
        if (containsAny(text, KNOWLEDGE)) {
            agentTypes.add("knowledgeQa");
        }
        return agentTypes.stream().toList();
    }

    private static Set<String> allFinancialTerms() {
        LinkedHashSet<String> terms = new LinkedHashSet<>();
        terms.addAll(BUSINESS_ANALYSIS);
        terms.addAll(ACCOUNTING);
        terms.addAll(REPORTING);
        terms.addAll(AR_AP);
        terms.addAll(CASHIER);
        terms.addAll(PURCHASE);
        terms.addAll(INVENTORY);
        terms.addAll(LOGISTICS);
        terms.addAll(RECONCILIATION);
        terms.addAll(WORKFLOW);
        return terms;
    }

    private static boolean containsAny(String text, Iterable<String> terms) {
        String normalized = value(text).toLowerCase(Locale.ROOT);
        for (String term : terms) {
            if (!value(term).isBlank() && normalized.contains(term.toLowerCase(Locale.ROOT))) {
                return true;
            }
        }
        return false;
    }

    private static boolean containsAny(String text, String... terms) {
        return containsAny(text, List.of(terms));
    }

    private static String value(String text) {
        return text == null ? "" : text;
    }
}
