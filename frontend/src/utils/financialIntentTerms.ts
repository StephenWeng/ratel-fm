const businessAnalysisTerms = [
  '经营', '经营情况', '经营分析', '经营统计', '经营概览', '经营建议', '业绩', '收入', '成本', '费用',
  '利润', '毛利', '毛利率', '净利', '现金流', '收支', '盈亏', '趋势', '同比', '环比', '占比',
  '结构', '排行', '汇总', '统计', '分析', '复盘', '看板', '指标', '项目', '供应商', '客户', '物料'
]

const accountingTerms = [
  '财务', '会计', '核算', '凭证', '分录', '借方', '贷方', '过账', '反过账', '试算平衡', '科目',
  '会计期间', '结账', '期初', '期末', '余额', '发生额', '制证', '自动制证', '凭证建议',
  '生成凭证', '会计平台', '已制证', '未制证', '来源单据'
]

const arApTerms = [
  '应收', '应付', '应收应付', '往来', '往来账', '客户', '供应商', '收款', '付款', '已收', '已付',
  '待收', '待付', '未收', '未付', '未结', '未结余额', '核销', '未核销', '账龄', '到期', '逾期',
  '回款', '付款计划', '收付统计'
]

const inventoryTerms = [
  '库存', '物料', '仓库', '入库', '出库', '调拨', '盘点', '库存流水', '物料库存', '负库存',
  '低库存', '安全库存', '库存风险', '库存结构'
]

const reconciliationTerms = [
  '对账', '核对', '勾稽', '一致', '不一致', '匹配', '链路', '闭环', '业务链路', '财务链路',
  '差异', '异常'
]

const workflowTerms = ['审批', '流程', '待办', '已办', '节点', '审批意见', '驳回', '同意', '下一步']
const knowledgeTerms = ['附件', '合同', '制度', '文档', '资料', '文件', '知识', '知识库', '发票', '简历', '候选人']

function hasAny(text: string, terms: string[]) {
  return terms.some((term) => text.includes(term))
}

export function agentTypesByFinancialQuestion(value: string) {
  const text = value || ''
  const result = new Set<string>()
  if (hasAny(text, ['查询', '查一下', '看看', '单号', '明细', '列表']) || hasAny(text, accountingTerms)) {
    result.add('query')
  }
  if (hasAny(text, reconciliationTerms)) {
    result.add('reconciliation')
  }
  if (hasAny(text, ['制证', '凭证建议', '生成凭证', '会计平台', '未制证', '已制证', '来源单据'])) {
    result.add('voucherSuggestion')
  }
  if (hasAny(text, ['到期', '逾期', '未核销', '待收', '待付', '未结', '账龄', '回款', '付款计划'])) {
    result.add('dueReminder')
  }
  if (hasAny(text, workflowTerms)) {
    result.add('workflowAssistant')
  }
  if (hasAny(text, inventoryTerms)) {
    result.add('inventoryRisk')
  }
  if (hasAny(text, businessAnalysisTerms) || hasAny(text, arApTerms)) {
    result.add('businessAnalysis')
  }
  if (hasAny(text, knowledgeTerms)) {
    result.add('knowledgeQa')
  }
  return Array.from(result)
}
