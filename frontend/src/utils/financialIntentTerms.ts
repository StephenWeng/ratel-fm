const businessAnalysisTerms = [
  '经营', '经营情况', '经营分析', '经营统计', '经营概览', '经营建议', '业绩', '收入', '成本', '费用',
  '利润', '毛利', '毛利率', '净利', '现金流', '营运资本', '现金转换周期', 'CCC', '预算', '预算执行',
  '预算偏差', '滚动预测', '投入产出', '投资回报', 'ROI', 'ROE', 'ROA', '收支', '盈亏', '趋势',
  '同比', '环比', '占比', '结构', '排行', '汇总', '统计', '分析', '复盘', '看板', '指标', '项目',
  '供应商', '客户', '物料', '生意怎么样', '赚不赚钱', '有没有赚钱', '钱花在哪', '哪里亏'
]

const accountingTerms = [
  '财务', '会计', '核算', '凭证', '分录', '借方', '贷方', '过账', '反过账', '试算平衡', '科目',
  '会计期间', '结账', '期初', '期末', '余额', '发生额', '制证', '自动制证', '凭证建议',
  '生成凭证', '会计平台', '已制证', '未制证', '来源单据', '总账', '明细账', '日记账', '暂估',
  '暂估入账', '冲回', '红冲', '摊销', '折旧', '预提', '递延', '权责发生制', '收付实现制',
  '关账', '月结', '账平不平', '借贷平不平', '还有哪些没做账'
]

const arApTerms = [
  '应收', '应付', '应收应付', '往来', '往来账', '客户', '供应商', '收款', '付款', '已收', '已付',
  '待收', '待付', '未收', '未付', '未结', '未结余额', '核销', '未核销', '账龄', '到期', '逾期',
  '回款', '付款计划', '收付统计', '坏账', '坏账准备', '信用减值', '呆账', '催收', '催款',
  'DSO', 'DPO', '应收周转', '应付周转', '周转天数', '周转率', '账期', '信用期', '未到票',
  '到票', '发票匹配', '三单匹配', '欠了多久', '拖了多久', '谁还没给钱', '谁的钱没回来',
  '该收的钱', '该付的钱', '哪些快到期', '哪些拖过期', '催哪些客户', '先付哪家'
]

const inventoryTerms = [
  '库存', '物料', '仓库', '入库', '出库', '调拨', '盘点', '库存流水', '物料库存', '负库存',
  '低库存', '安全库存', '库存风险', '库存结构', '存货', '存货跌价', '库龄', '呆滞料', '滞销',
  '存货周转', '周转天数', '在途库存', '在途物资', '缺货', '积压', '东西压库', '货压太多',
  '哪些货不动', '哪些料很久没动', '库存够不够', '仓库数量对不上'
]

const reconciliationTerms = [
  '对账', '核对', '勾稽', '一致', '不一致', '匹配', '链路', '闭环', '业务链路', '财务链路',
  '差异', '异常', '三单一致', '三流一致', '单据流', '资金流', '发票流', '合同流', '账实相符',
  '账账相符', '账表相符', '对不上', '金额对不上', '数量对不上', '货票款对不上', '票货不一致'
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
  if (hasAny(text, [
    '到期', '逾期', '未核销', '待收', '待付', '未结', '账龄', '账期', '回款', '付款计划',
    '催收', '催款', 'DSO', 'DPO', '应收周转', '应付周转', '周转天数', '资金缺口',
    '钱够不够', '够不够付', '账上钱', '现金转换周期', '营运资本'
  ])) {
    result.add('dueReminder')
  }
  if (hasAny(text, workflowTerms)) {
    result.add('workflowAssistant')
  }
  if (hasAny(text, inventoryTerms)) {
    result.add('inventoryRisk')
  }
  if (hasAny(text, businessAnalysisTerms) || hasAny(text, arApTerms)
    || hasAny(text, ['存货周转', '周转天数', '库龄', '呆滞料', '积压', '营运资本', '现金转换周期'])) {
    result.add('businessAnalysis')
  }
  if (hasAny(text, knowledgeTerms)) {
    result.add('knowledgeQa')
  }
  return Array.from(result)
}
