import type { Router } from 'vue-router'
import type { AiAssistantResponse } from '@/types/api'

/**
 * AiCitation 类型定义，用于约束页面状态、接口入参或接口返回数据结构。
 */
type AiCitation = AiAssistantResponse['citations'][number]

/**
 * 常量 sourceQueryKeyMap：保存当前模块的页面状态、配置项、接口实例或计算结果。
 */
const sourceQueryKeyMap: Record<string, string> = {
  /**
   * 字段 VOUCHER：表示表单、筛选条件、接口数据或组件状态中的 VOUCHER 值。
   */
  VOUCHER: 'voucherNo',
  /**
   * 字段 PURCHASE_ORDER：表示表单、筛选条件、接口数据或组件状态中的 PURCHASE_ORDER 值。
   */
  PURCHASE_ORDER: 'orderNo',
  /**
   * 字段 SHIPMENT：表示表单、筛选条件、接口数据或组件状态中的 SHIPMENT 值。
   */
  SHIPMENT: 'shipmentNo',
  /**
   * 字段 INVENTORY_LEDGER：表示表单、筛选条件、接口数据或组件状态中的 INVENTORY_LEDGER 值。
   */
  INVENTORY_LEDGER: 'movementNo',
  /**
   * 字段 AR_AP_BILL：表示表单、筛选条件、接口数据或组件状态中的 AR_AP_BILL 值。
   */
  AR_AP_BILL: 'billNo',
  /**
   * 字段 SUBJECT：表示表单、筛选条件、接口数据或组件状态中的 SUBJECT 值。
   */
  SUBJECT: 'code'
}

/**
 * 常量 fallbackRouteMap：保存当前模块的页面状态、配置项、接口实例或计算结果。
 */
const fallbackRouteMap: Record<string, string> = {
  /**
   * 字段 VOUCHER：表示表单、筛选条件、接口数据或组件状态中的 VOUCHER 值。
   */
  VOUCHER: '/vouchers',
  /**
   * 字段 PURCHASE_ORDER：表示表单、筛选条件、接口数据或组件状态中的 PURCHASE_ORDER 值。
   */
  PURCHASE_ORDER: '/purchase-orders',
  /**
   * 字段 SHIPMENT：表示表单、筛选条件、接口数据或组件状态中的 SHIPMENT 值。
   */
  SHIPMENT: '/shipments',
  /**
   * 字段 INVENTORY_LEDGER：表示表单、筛选条件、接口数据或组件状态中的 INVENTORY_LEDGER 值。
   */
  INVENTORY_LEDGER: '/inventory',
  /**
   * 字段 AR_AP_BILL：表示表单、筛选条件、接口数据或组件状态中的 AR_AP_BILL 值。
   */
  AR_AP_BILL: '/ar-ap',
  /**
   * 字段 SUBJECT：表示表单、筛选条件、接口数据或组件状态中的 SUBJECT 值。
   */
  SUBJECT: '/subjects'
}

/**
 * 执行 canEnterCitation 方法。
 * 
 * 实现步骤：
 * 1. 读取当前页面状态或调用参数；
 * 2. 完成对应的校验、接口调用或数据转换；
 * 3. 更新页面状态或返回处理结果。
 */
export function canEnterCitation(item: AiCitation) {
  return Boolean(citationTarget(item))
}

/**
 * 执行 canEnterAnswer 方法。
 * 
 * 实现步骤：
 * 1. 读取当前页面状态或调用参数；
 * 2. 完成对应的校验、接口调用或数据转换；
 * 3. 更新页面状态或返回处理结果。
 */
export function canEnterAnswer(response: AiAssistantResponse) {
  return Boolean(answerTarget(response))
}

/**
 * 执行 enterCitation 方法。
 * 
 * 实现步骤：
 * 1. 读取当前页面状态或调用参数；
 * 2. 完成对应的校验、接口调用或数据转换；
 * 3. 更新页面状态或返回处理结果。
 */
export async function enterCitation(router: Router, item: AiCitation) {
  /**
   * 常量 target：保存当前模块的页面状态、配置项、接口实例或计算结果。
   */
  const target = citationTarget(item)
  if (!target) {
    return
  }
  await router.push(target)
}

/**
 * 执行 enterAnswer 方法。
 * 
 * 实现步骤：
 * 1. 读取当前页面状态或调用参数；
 * 2. 完成对应的校验、接口调用或数据转换；
 * 3. 更新页面状态或返回处理结果。
 */
export async function enterAnswer(router: Router, response: AiAssistantResponse) {
  /**
   * 常量 target：保存当前模块的页面状态、配置项、接口实例或计算结果。
   */
  const target = answerTarget(response)
  if (!target) {
    return
  }
  await router.push(target)
}

/**
 * 执行 citationTarget 方法。
 * 
 * 实现步骤：
 * 1. 读取当前页面状态或调用参数；
 * 2. 完成对应的校验、接口调用或数据转换；
 * 3. 更新页面状态或返回处理结果。
 */
function citationTarget(item: AiCitation) {
  if (item.url) {
    return null
  }
  /**
   * 常量 routePath：保存当前模块的页面状态、配置项、接口实例或计算结果。
   */
  const routePath = item.routePath || fallbackRouteMap[item.type]
  if (!routePath) {
    return null
  }
  /**
   * 常量 queryKey：保存当前模块的页面状态、配置项、接口实例或计算结果。
   */
  const queryKey = sourceQueryKeyMap[item.type]
  /**
   * 常量 sourceNo：保存当前模块的页面状态、配置项、接口实例或计算结果。
   */
  const sourceNo = item.sourceNo && !item.sourceNo.startsWith('MODULE_') ? item.sourceNo : ''
  /**
   * 常量 query：保存当前模块的页面状态、配置项、接口实例或计算结果。
   */
  const query = queryKey && sourceNo ? { [queryKey]: sourceNo } : undefined
  return { path: routePath, query }
}

/**
 * 执行 answerTarget 方法。
 * 
 * 实现步骤：
 * 1. 读取当前页面状态或调用参数；
 * 2. 完成对应的校验、接口调用或数据转换；
 * 3. 更新页面状态或返回处理结果。
 */
function answerTarget(response: AiAssistantResponse) {
  /**
   * 常量 answer：保存当前模块的页面状态、配置项、接口实例或计算结果。
   */
  const answer = response.answer || ''
  if (!answer.includes('进入')) {
    return null
  }
  /**
   * 常量 moduleTarget：保存当前模块的页面状态、配置项、接口实例或计算结果。
   */
  const moduleTarget = answerModuleTarget(answer)
  if (!moduleTarget) {
    return null
  }
  /**
   * 常量 sourceNo：保存当前模块的页面状态、配置项、接口实例或计算结果。
   */
  const sourceNo = answerBusinessNo(answer)
  /**
   * 常量 queryKey：保存当前模块的页面状态、配置项、接口实例或计算结果。
   */
  const queryKey = answerQueryKey(answer, moduleTarget)
  /**
   * 常量 query：保存当前模块的页面状态、配置项、接口实例或计算结果。
   */
  const query = sourceNo && queryKey ? { [queryKey]: sourceNo } : undefined
  return { path: moduleTarget.path, query }
}

/**
 * 执行 answerModuleTarget 方法。
 * 
 * 实现步骤：
 * 1. 读取当前页面状态或调用参数；
 * 2. 完成对应的校验、接口调用或数据转换；
 * 3. 更新页面状态或返回处理结果。
 */
function answerModuleTarget(answer: string) {
  return [
    { names: ['物流运输模块', '物流管理', '物流单', '物流'], path: '/shipments', defaultQueryKey: 'shipmentNo' },
    { names: ['库存管理', '库存台账', '库存流水'], path: '/inventory', defaultQueryKey: 'movementNo' },
    { names: ['采购管理', '采购订单', '采购单'], path: '/purchase-orders', defaultQueryKey: 'orderNo' },
    { names: ['凭证记账', '财务凭证', '凭证'], path: '/vouchers', defaultQueryKey: 'voucherNo' },
    { names: ['应收应付'], path: '/ar-ap', defaultQueryKey: 'billNo' },
    { names: ['会计科目', '科目'], path: '/subjects', defaultQueryKey: 'code' }
  ].find((item) => item.names.some((name) => answer.includes(`进入【${name}】`) || answer.includes(`进入 ${name}`)))
}

/**
 * 执行 answerBusinessNo 方法。
 * 
 * 实现步骤：
 * 1. 读取当前页面状态或调用参数；
 * 2. 完成对应的校验、接口调用或数据转换；
 * 3. 更新页面状态或返回处理结果。
 */
function answerBusinessNo(answer: string) {
  /**
   * 常量 explicit：保存当前模块的页面状态、配置项、接口实例或计算结果。
   */
  const explicit = answer.match(/(?:检索|查询|搜索|筛选)[^A-Za-z0-9]{0,12}(?:单号|流水号|编号|编码|运单号)?[^A-Za-z0-9]{0,12}([A-Za-z]{1,12}\d{4,}|\d{4,})/i)
  if (explicit?.[1]) {
    return explicit[1]
  }
  return answer.match(/(?:单号|流水号|编号|编码|运单号)[^A-Za-z0-9]{0,12}([A-Za-z]{1,12}\d{4,}|\d{4,})/i)?.[1] || ''
}

/**
 * 执行 answerQueryKey 方法。
 * 
 * 实现步骤：
 * 1. 读取当前页面状态或调用参数；
 * 2. 完成对应的校验、接口调用或数据转换；
 * 3. 更新页面状态或返回处理结果。
 */
function answerQueryKey(answer: string, moduleTarget: { path: string; defaultQueryKey: string }) {
  if (moduleTarget.path === '/shipments') {
    if (answer.includes('运单号')) return 'trackingNo'
    if (answer.includes('关联单号') || answer.includes('关联采购单')) return 'relatedOrderNo'
  }
  if (moduleTarget.path === '/inventory' && answer.includes('关联')) {
    return 'relatedBizNo'
  }
  if (moduleTarget.path === '/vouchers' && answer.includes('来源')) {
    return 'sourceBizNo'
  }
  return moduleTarget.defaultQueryKey
}
