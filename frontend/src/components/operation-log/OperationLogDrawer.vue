<template>
  <el-drawer v-model="visible" :title="title" size="640px" direction="rtl" class="operation-log-drawer">
    <div class="drawer-filter">
      <el-date-picker
        v-model="timeRange"
        type="daterange"
        value-format="YYYY-MM-DD"
        start-placeholder="开始日期"
        end-placeholder="结束日期"
        class="time-picker"
      />
      <div class="drawer-actions">
        <el-button type="primary" :loading="loading" @click="reload">查询</el-button>
        <el-button @click="resetRange">重置</el-button>
      </div>
    </div>

    <div class="timeline-scroll" @scroll.passive="onScroll">
      <el-timeline v-if="logs.length > 0">
        <el-timeline-item
          v-for="item in logs"
          :key="item.id"
          :timestamp="displayDateTime(item.operationTime)"
          placement="top"
          type="primary"
        >
          <div class="timeline-card">
            <div class="timeline-title">{{ item.actionName }}</div>
            <template v-if="businessSnapshot(item)">
              <div class="snapshot-grid">
                <div v-for="field in snapshotFields(item)" :key="field.label" class="snapshot-field">
                  <span>{{ field.label }}</span>
                  <strong>
                    <AmountText
                      v-if="isAmountField(field.key) && isNumericAmount(field.rawValue)"
                      :value="Number(field.rawValue)"
                      :display="field.value"
                      :currency-code="field.currencyCode"
                      :currency-name="field.currencyName"
                    />
                    <template v-else>{{ field.value }}</template>
                  </strong>
                </div>
              </div>
              <div v-for="section in snapshotSections(item)" :key="section.title" class="snapshot-section">
                <div class="snapshot-section-title">{{ section.title }}</div>
                <div class="snapshot-table">
                  <table>
                    <thead>
                      <tr>
                        <th v-for="column in section.columns" :key="column.key">{{ column.label }}</th>
                      </tr>
                    </thead>
                    <tbody>
                      <tr v-for="(row, rowIndex) in section.rows" :key="rowIndex">
                        <td v-for="column in section.columns" :key="column.key">
                          <AmountText
                            v-if="isAmountField(column.key) && isNumericAmount(row[column.key])"
                            :value="Number(row[column.key])"
                            :display="displayValue(row[column.key], column.key, item.businessType)"
                            :currency-code="currencyCodeForRow(row, column.key)"
                            :currency-name="currencyNameForRow(row, column.key)"
                          />
                          <template v-else>{{ displayValue(row[column.key], column.key, item.businessType) }}</template>
                        </td>
                      </tr>
                    </tbody>
                  </table>
                </div>
              </div>
            </template>
            <div v-else class="timeline-detail">{{ item.detail }}</div>
            <div v-if="item.fromState || item.toState" class="timeline-meta">
              {{ displayState(item.fromState, item.businessType) }} -> {{ displayState(item.toState, item.businessType) }}
            </div>
            <div class="timeline-meta">操作人：{{ item.operatorName || item.operatorUsername || '-' }}</div>
          </div>
        </el-timeline-item>
      </el-timeline>
      <el-empty v-else-if="!loading" description="暂无操作流水" />
      <div v-if="loadingMore" class="load-tip">正在加载...</div>
      <div v-else-if="logs.length > 0 && !hasMore" class="load-tip">没有更多流水</div>
    </div>
  </el-drawer>
</template>

<script setup lang="ts">
import { ref } from 'vue'
import AmountText from '@/components/common/AmountText.vue'
import type { OperationLogQueryParams } from '@/api/fm'
import type { BusinessOperationLogPage, UnifiedOperationLogView } from '@/types/api'
import { formatLocalDate as formatDate, toLocalDateTimeBoundary as toDateTimeBoundary } from '@/utils/dateTime'

/**
 * 业务操作流水右侧抽屉。
 *
 * 开发组织：ratel；开发人员：WenZhang；联系方式：18782945613。
 *
 * 实现步骤：
 * 1. 打开时默认查询最近半个月流水；
 * 2. 按操作时间倒序展示时间轴；
 * 3. 滚动到底部时分页加载下一页，避免一次性拉取大量流水。
 *
 * @author ratel
 */
const visible = ref(false)
/**
 * 常量 title：保存当前模块的页面状态、配置项、接口实例或计算结果。
 */
const title = ref('操作流水')
/**
 * 常量 logs：保存当前模块的页面状态、配置项、接口实例或计算结果。
 */
const logs = ref<UnifiedOperationLogView[]>([])
/**
 * 常量 total：保存当前模块的页面状态、配置项、接口实例或计算结果。
 */
const total = ref(0)
/**
 * 常量 page：保存当前模块的页面状态、配置项、接口实例或计算结果。
 */
const page = ref(0)
/**
 * 常量 size：保存当前模块的页面状态、配置项、接口实例或计算结果。
 */
const size = 20
/**
 * 常量 loading：保存当前模块的页面状态、配置项、接口实例或计算结果。
 */
const loading = ref(false)
/**
 * 常量 loadingMore：保存当前模块的页面状态、配置项、接口实例或计算结果。
 */
const loadingMore = ref(false)
/**
 * 常量 timeRange：保存当前模块的页面状态、配置项、接口实例或计算结果。
 */
const timeRange = ref<string[]>(defaultTimeRange())
/**
 * 变量 loader：保存当前模块的页面状态、配置项、接口实例或计算结果。
 */
let loader: ((params: OperationLogQueryParams) => Promise<BusinessOperationLogPage>) | undefined

/**
 * 常量 hasMore：保存当前模块的页面状态、配置项、接口实例或计算结果。
 */
const hasMore = ref(false)

/**
 * SnapshotField 类型定义，用于约束页面状态、接口入参或接口返回数据结构。
 */
interface SnapshotField {
  /**
   * 字段 label：表示表单、筛选条件、接口数据或组件状态中的 label 值。
   */
  label: string
  /**
   * 字段 value：表示表单、筛选条件、接口数据或组件状态中的 value 值。
   */
  value: string
  /**
   * 字段 key：业务快照 JSON 中的字段名，用于识别金额字段和枚举字段。
   */
  key: string
  /**
   * 字段 rawValue：业务快照原始值，用于金额悬浮展示数字和中文大写。
   */
  rawValue: unknown
  /**
   * 字段 currencyCode：金额字段对应的币种编码。
   */
  currencyCode?: string
  /**
   * 字段 currencyName：金额字段对应的币种名称。
   */
  currencyName?: string
}

/**
 * SnapshotFieldDefinition 类型定义，用于声明业务快照字段的来源 key 和页面显示标签。
 */
interface SnapshotFieldDefinition {
  /**
   * 字段 key：业务快照 JSON 中的字段名。
   */
  key: string
  /**
   * 字段 label：操作流水中展示的字段中文名。
   */
  label: string
}

/**
 * SnapshotSection 类型定义，用于约束页面状态、接口入参或接口返回数据结构。
 */
interface SnapshotSection {
  /**
   * 字段 title：表示表单、筛选条件、接口数据或组件状态中的 title 值。
   */
  title: string
  /**
   * 字段 columns：表示表单、筛选条件、接口数据或组件状态中的 columns 值。
   */
  columns: Array<{ key: string; label: string }>
  /**
   * 字段 rows：表示表单、筛选条件、接口数据或组件状态中的 rows 值。
   */
  rows: Array<Record<string, unknown>>
}

/**
 * 打开操作流水抽屉。
 */
function open(options: { title: string; load: (params: OperationLogQueryParams) => Promise<BusinessOperationLogPage> }) {
  title.value = options.title
  loader = options.load
  visible.value = true
  timeRange.value = defaultTimeRange()
  void reload()
}

/**
 * 重新加载第一页流水。
 */
async function reload() {
  if (!loader) {
    return
  }
  loading.value = true
  page.value = 0
  try {
    /**
     * 常量 result：保存当前模块的页面状态、配置项、接口实例或计算结果。
     */
    const result = await loader(buildParams(0))
    logs.value = result.rows
    total.value = result.total
    hasMore.value = logs.value.length < total.value
  } finally {
    loading.value = false
  }
}

/**
 * 加载下一页流水。
 */
async function loadMore() {
  if (!loader || loading.value || loadingMore.value || !hasMore.value) {
    return
  }
  loadingMore.value = true
  /**
   * 常量 nextPage：保存当前模块的页面状态、配置项、接口实例或计算结果。
   */
  const nextPage = page.value + 1
  try {
    /**
     * 常量 result：保存当前模块的页面状态、配置项、接口实例或计算结果。
     */
    const result = await loader(buildParams(nextPage))
    page.value = nextPage
    logs.value.push(...result.rows)
    total.value = result.total
    hasMore.value = logs.value.length < total.value
  } finally {
    loadingMore.value = false
  }
}

/**
 * 滚动到底部时继续分页加载。
 */
function onScroll(event: Event) {
  /**
   * 常量 target：保存当前模块的页面状态、配置项、接口实例或计算结果。
   */
  const target = event.target as HTMLElement
  if (target.scrollTop + target.clientHeight >= target.scrollHeight - 48) {
    void loadMore()
  }
}

/**
 * 重置到最近半个月。
 */
function resetRange() {
  timeRange.value = defaultTimeRange()
  void reload()
}

/**
 * 组装服务端查询参数。
 */
function buildParams(targetPage: number) {
  return {
    /**
     * 字段 startTime：表示表单、筛选条件、接口数据或组件状态中的 startTime 值。
     */
    startTime: toDateTimeBoundary(timeRange.value?.[0]),
    /**
     * 字段 endTime：表示表单、筛选条件、接口数据或组件状态中的 endTime 值。
     */
    endTime: toDateTimeBoundary(timeRange.value?.[1], true),
    /**
     * 字段 page：表示表单、筛选条件、接口数据或组件状态中的 page 值。
     */
    page: targetPage,
    size
  }
}

/**
 * 默认查询最近半个月。
 */
function defaultTimeRange() {
  /**
   * 常量 end：保存当前模块的页面状态、配置项、接口实例或计算结果。
   */
  const end = new Date()
  /**
   * 常量 start：保存当前模块的页面状态、配置项、接口实例或计算结果。
   */
  const start = new Date(end)
  start.setDate(start.getDate() - 15)
  return [formatDate(start), formatDate(end)]
}

/**
 * 执行 formatDate 方法。
 * 
 * 实现步骤：
 * 1. 读取当前页面状态或调用参数；
 * 2. 完成对应的校验、接口调用或数据转换；
 * 3. 更新页面状态或返回处理结果。
 */
/**
 * 执行 toDateTimeBoundary 方法。
 * 
 * 实现步骤：
 * 1. 读取当前页面状态或调用参数；
 * 2. 完成对应的校验、接口调用或数据转换；
 * 3. 更新页面状态或返回处理结果。
 */
/**
 * 执行 localTimezoneOffset 方法。
 * 
 * 实现步骤：
 * 1. 读取当前页面状态或调用参数；
 * 2. 完成对应的校验、接口调用或数据转换；
 * 3. 更新页面状态或返回处理结果。
 */
/**
 * 执行 displayDateTime 方法。
 * 
 * 实现步骤：
 * 1. 读取当前页面状态或调用参数；
 * 2. 完成对应的校验、接口调用或数据转换；
 * 3. 更新页面状态或返回处理结果。
 */
function displayDateTime(value?: string) {
  if (!value) {
    return ''
  }
  /**
   * 常量 date：保存当前模块的页面状态、配置项、接口实例或计算结果。
   */
  const date = new Date(value)
  if (Number.isNaN(date.getTime())) {
    return value
  }
  /**
   * 常量 pad：保存当前模块的页面状态、配置项、接口实例或计算结果。
   */
  const pad = (item: number) => String(item).padStart(2, '0')
  return `${date.getFullYear()}-${pad(date.getMonth() + 1)}-${pad(date.getDate())} ${pad(date.getHours())}:${pad(date.getMinutes())}:${pad(date.getSeconds())}`
}

/**
 * 解析业务快照。
 *
 * 开发组织：ratel；开发人员：WenZhang；联系方式：18782945613。
 *
 * 实现步骤：优先解析流水中的 snapshot JSON；解析失败时返回 undefined，由页面回退展示操作说明。
 */
function businessSnapshot(item: UnifiedOperationLogView): Record<string, unknown> | undefined {
  if (!item.snapshot) {
    return undefined
  }
  try {
    /**
     * 常量 parsed：保存当前模块的页面状态、配置项、接口实例或计算结果。
     */
    const parsed = JSON.parse(item.snapshot) as Record<string, unknown>
    return parsed && typeof parsed === 'object' ? parsed : undefined
  } catch {
    return undefined
  }
}

/**
 * 根据模块类型生成业务表单字段。
 *
 * 实现步骤：按 businessType 选择对应模块字段；字段值统一走 displayValue，保证空值、金额、日期等展示风格一致。
 */
function snapshotFields(item: UnifiedOperationLogView): SnapshotField[] {
  /**
   * 常量 snapshot：保存当前模块的页面状态、配置项、接口实例或计算结果。
   */
  const snapshot = businessSnapshot(item)
  if (!snapshot) {
    return []
  }
  /**
   * 常量 fields：保存当前模块的页面状态、配置项、接口实例或计算结果。
   */
  const fields = snapshotFieldDefinitions(item.businessType, snapshot)
  return fields.map((field) => ({
    /**
     * 字段 label：表示表单、筛选条件、接口数据或组件状态中的 label 值。
     */
    label: field.label,
    key: field.key,
    rawValue: snapshot[field.key],
    currencyCode: currencyCodeForRow(snapshot, field.key),
    currencyName: currencyNameForRow(snapshot, field.key),
    /**
     * 字段 value：表示表单、筛选条件、接口数据或组件状态中的 value 值。
     */
    value: displayValue(snapshot[field.key], field.key, item.businessType)
  }))
}

/**
 * 根据模块类型生成明细区块。
 *
 * 实现步骤：
 * 1. 先解析业务快照 JSON；
 * 2. 凭证和采购读取 lines 明细，生成可横向滚动的明细表；
 * 3. 其他模块没有明细时返回空数组，只展示表单主信息。
 */
function snapshotSections(item: UnifiedOperationLogView): SnapshotSection[] {
  /**
   * 常量 snapshot：保存当前模块的页面状态、配置项、接口实例或计算结果。
   */
  const snapshot = businessSnapshot(item)
  if (!snapshot) {
    return []
  }
  if (item.businessType === 'VOUCHER') {
    return buildLineSection('凭证分录', snapshot.lines, [
      { key: 'lineNo', label: '行号' },
      { key: 'subjectCode', label: '科目编码' },
      { key: 'subjectName', label: '科目名称' },
      { key: 'summary', label: '摘要' },
      { key: 'debitAmount', label: '借方金额' },
      { key: 'creditAmount', label: '贷方金额' },
      { key: 'currencyName', label: '币种' },
      { key: 'exchangeRateToCny', label: '汇率' },
      { key: 'debitAmountCny', label: '借方人民币' },
      { key: 'creditAmountCny', label: '贷方人民币' },
      { key: 'auxiliary', label: '辅助核算' }
    ])
  }
  if (item.businessType === 'PURCHASE_ORDER') {
    return buildLineSection('采购明细', snapshot.lines, [
      { key: 'lineNo', label: '行号' },
      { key: 'itemCode', label: '物料编码' },
      { key: 'itemName', label: '物料名称' },
      { key: 'specification', label: '规格型号' },
      { key: 'unitName', label: '单位' },
      { key: 'quantity', label: '数量' },
      { key: 'unitPrice', label: '单价' },
      { key: 'taxRate', label: '税率' },
      { key: 'taxAmount', label: '税额' },
      { key: 'amount', label: '金额' },
      { key: 'amountWithTax', label: '价税合计' },
      { key: 'plannedArrivalDate', label: '计划到货' },
      { key: 'receiveWarehouse', label: '收货仓库' },
      { key: 'currencyName', label: '币种' },
      { key: 'exchangeRateToCny', label: '汇率' },
      { key: 'unitPriceCny', label: '单价人民币' },
      { key: 'amountCny', label: '金额人民币' }
    ])
  }
  return []
}

/**
 * 执行 buildLineSection 方法。
 * 
 * 实现步骤：
 * 1. 读取当前页面状态或调用参数；
 * 2. 完成对应的校验、接口调用或数据转换；
 * 3. 更新页面状态或返回处理结果。
 */
function buildLineSection(
  /**
   * 字段 title：表示表单、筛选条件、接口数据或组件状态中的 title 值。
   */
  title: string,
  /**
   * 字段 rows：表示表单、筛选条件、接口数据或组件状态中的 rows 值。
   */
  rows: unknown,
  /**
   * 字段 columns：表示表单、筛选条件、接口数据或组件状态中的 columns 值。
   */
  columns: Array<{ key: string; label: string }>
): SnapshotSection[] {
  if (!Array.isArray(rows) || rows.length === 0) {
    return []
  }
  return [{
    title,
    columns,
    /**
     * 字段 rows：表示表单、筛选条件、接口数据或组件状态中的 rows 值。
     */
    rows: rows.filter((row): row is Record<string, unknown> => row !== null && typeof row === 'object')
  }]
}

/**
 * 返回各业务模块查看流水需要展示的快照字段。
 *
 * 实现步骤：
 * 1. 根据 businessType 选择字段清单；
 * 2. 字段 key 必须和后端快照 JSON 或前端物流转换快照保持一致；
 * 3. 新增表单字段必须同步补充到这里，确保“新增/编辑表单”和“查看流水”展示一致。
 */
function snapshotFieldDefinitions(businessType: string, snapshot?: Record<string, unknown>): SnapshotFieldDefinition[] {
  if (businessType === 'VOUCHER') {
    return [
      { key: 'voucherNo', label: '凭证号' },
      { key: 'voucherDate', label: '凭证日期' },
      { key: 'belongMonth', label: '所属年月' },
      { key: 'projectName', label: '项目' },
      { key: 'summary', label: '摘要' },
      { key: 'status', label: '状态' },
      { key: 'totalDebit', label: '借方合计' },
      { key: 'totalCredit', label: '贷方合计' },
      { key: 'currencyName', label: '币种' },
      { key: 'exchangeRateToCny', label: '汇率' },
      { key: 'totalDebitCny', label: '借方人民币合计' },
      { key: 'totalCreditCny', label: '贷方人民币合计' },
      { key: 'createdBy', label: '创建人' },
      { key: 'postedBy', label: '过账人' },
      { key: 'sourceBizNo', label: '来源业务号' }
    ]
  }
  if (businessType === 'PURCHASE_ORDER') {
    return [
      { key: 'orderNo', label: '采购单号' },
      { key: 'supplierName', label: '供应商' },
      { key: 'documentType', label: '单据类型' },
      { key: 'businessType', label: '业务类型' },
      { key: 'projectName', label: '项目' },
      { key: 'purchaseOrganization', label: '采购组织' },
      { key: 'purchaseDepartment', label: '采购部门' },
      { key: 'purchaserName', label: '采购员' },
      { key: 'settlementOrganization', label: '结算组织' },
      { key: 'paymentTerms', label: '付款条件' },
      { key: 'settlementMethod', label: '结算方式' },
      { key: 'deliveryTerms', label: '交货条件' },
      { key: 'sourceBillType', label: '来源类型' },
      { key: 'sourceBillNo', label: '来源单号' },
      { key: 'orderDate', label: '采购日期' },
      { key: 'status', label: '状态' },
      { key: 'approvalResult', label: '审批结果' },
      { key: 'approvalComment', label: '审批意见' },
      { key: 'totalAmount', label: '总金额' },
      { key: 'currencyName', label: '币种' },
      { key: 'exchangeRateToCny', label: '汇率' },
      { key: 'totalAmountCny', label: '总金额人民币' },
      { key: 'createdBy', label: '创建人' },
      { key: 'remark', label: '备注' }
    ]
  }
  if (businessType === 'SHIPMENT') {
    return [
      { key: 'shipmentNo', label: '物流单号' },
      { key: 'relatedOrderNo', label: '关联单号' },
      { key: 'documentType', label: '单据类型' },
      { key: 'projectName', label: '项目' },
      { key: 'transportMode', label: '运输方式' },
      { key: 'shippingOrganization', label: '发运组织' },
      { key: 'receivingOrganization', label: '收货组织' },
      ...shipmentCarrierFields(snapshot),
      { key: 'originDivisionName', label: '发货区划' },
      { key: 'origin', label: '发货地详址' },
      { key: 'destinationDivisionName', label: '目的区划' },
      { key: 'destination', label: '目的地详址' },
      { key: 'plannedShipDate', label: '计划发货日期' },
      { key: 'actualShipDate', label: '实际发货日期' },
      { key: 'deliveredDate', label: '送达日期' },
      { key: 'status', label: '状态' },
      { key: 'remark', label: '备注' },
      { key: 'operationRemark', label: '确认说明' }
    ]
  }
  if (businessType === 'INVENTORY_LEDGER') {
    return [
      { key: 'movementNo', label: '流水号' },
      { key: 'movementType', label: '业务类型' },
      { key: 'movementDate', label: '业务日期' },
      { key: 'projectName', label: '项目' },
      { key: 'itemCode', label: '物料编码' },
      { key: 'itemName', label: '物料名称' },
      { key: 'specification', label: '规格型号' },
      { key: 'stockOrganization', label: '库存组织' },
      { key: 'ownerName', label: '货主' },
      { key: 'unitName', label: '单位' },
      { key: 'batchNo', label: '批号' },
      { key: 'quantity', label: '数量' },
      { key: 'fromWarehouse', label: '来源仓' },
      { key: 'toWarehouse', label: '目标仓' },
      { key: 'relatedBizNo', label: '关联业务号' },
      { key: 'sourceBillType', label: '来源类型' },
      { key: 'organizationCode', label: '组织' },
      { key: 'remark', label: '备注' }
    ]
  }
  if (businessType === 'AR_AP_BILL') {
    return [
      { key: 'billNo', label: '单据号' },
      { key: 'billType', label: '单据类型' },
      { key: 'partnerName', label: '客户/供应商' },
      { key: 'projectName', label: '项目' },
      { key: 'documentType', label: '业务单据类型' },
      { key: 'businessOrganization', label: '业务组织' },
      { key: 'settlementOrganization', label: '结算组织' },
      { key: 'paymentOrganization', label: '收付款组织' },
      { key: 'paymentTerms', label: '收付款条件' },
      { key: 'settlementMethod', label: '结算方式' },
      { key: 'sourceBillType', label: '来源类型' },
      { key: 'sourceBillNo', label: '来源单号' },
      { key: 'billDate', label: '单据日期' },
      { key: 'dueDate', label: '到期日期' },
      { key: 'amount', label: '金额' },
      { key: 'paidAmount', label: '已收/付金额' },
      { key: 'remainingAmount', label: '剩余金额' },
      { key: 'currencyName', label: '币种' },
      { key: 'exchangeRateToCny', label: '汇率' },
      { key: 'amountCny', label: '金额人民币' },
      { key: 'paidAmountCny', label: '已收/付人民币' },
      { key: 'remainingAmountCny', label: '剩余人民币' },
      { key: 'status', label: '状态' },
      { key: 'agingDays', label: '账龄天数' },
      { key: 'paymentPlan', label: '付款计划' },
      { key: 'organizationCode', label: '组织' }
    ]
  }
  return [
    { key: 'businessNo', label: '业务编号' },
    { key: 'businessTitle', label: '业务标题' }
  ]
}

/**
 * 根据运输方式返回物流流水承运信息字段。
 *
 * 实现步骤：
 * 1. 从物流快照读取 transportMode；
 * 2. 按公路、铁路、航空、水运、快递匹配不同承运字段标签；
 * 3. 快递等表单隐藏运输工具的场景，流水也同步隐藏 vehicleNo。
 */
function shipmentCarrierFields(snapshot?: Record<string, unknown>): SnapshotFieldDefinition[] {
  /** 运输方式对应的承运字段标签配置，保证流水展示和物流表单保持一致。 */
  const info = shipmentCarrierInfo(String(snapshot?.transportMode || ''))
  const fields: SnapshotFieldDefinition[] = [
    { key: 'carrierName', label: info.carrierLabel },
    { key: 'trackingNo', label: info.trackingLabel },
    { key: 'driverName', label: info.driverLabel },
    { key: 'driverPhone', label: info.phoneLabel }
  ]
  if (info.showVehicle) {
    fields.push({ key: 'vehicleNo', label: info.vehicleLabel })
  }
  return fields
}

/**
 * 匹配物流运输方式对应的字段标签。
 *
 * 实现步骤：按运输方式关键字返回字段标签；未命中时回退公路运输默认标签。
 */
function shipmentCarrierInfo(transportMode: string) {
  if (transportMode.includes('铁路')) {
    return { carrierLabel: '铁路承运方', trackingLabel: '铁路运单号', driverLabel: '跟单人', phoneLabel: '联系电话', vehicleLabel: '车次', showVehicle: true }
  }
  if (transportMode.includes('航空')) {
    return { carrierLabel: '航空公司', trackingLabel: '空运单号', driverLabel: '地面联系人', phoneLabel: '联系电话', vehicleLabel: '航班号', showVehicle: true }
  }
  if (transportMode.includes('水') || transportMode.includes('海') || transportMode.includes('船')) {
    return { carrierLabel: '船运公司', trackingLabel: '提单号', driverLabel: '联系人', phoneLabel: '联系电话', vehicleLabel: '船名/航次', showVehicle: true }
  }
  if (transportMode.includes('快递')) {
    return { carrierLabel: '快递公司', trackingLabel: '快递单号', driverLabel: '派送员', phoneLabel: '派送员电话', vehicleLabel: '派送车辆', showVehicle: false }
  }
  return { carrierLabel: '承运商', trackingLabel: '运单号', driverLabel: '司机', phoneLabel: '司机电话', vehicleLabel: '车牌号', showVehicle: true }
}

/**
 * 执行 displayValue 方法。
 * 
 * 实现步骤：
 * 1. 读取当前页面状态或调用参数；
 * 2. 完成对应的校验、接口调用或数据转换；
 * 3. 更新页面状态或返回处理结果。
 */
function displayValue(value: unknown, key?: string, businessType?: string) {
  if (value === undefined || value === null || value === '') {
    return '-'
  }
  if (typeof value === 'string') {
    return enumLabel(value, key, businessType)
  }
  if (typeof value === 'number') {
    return Number.isInteger(value) ? String(value) : value.toFixed(8).replace(/0+$/, '').replace(/\.$/, '')
  }
  if (typeof value === 'boolean') {
    return value ? '是' : '否'
  }
  return String(value)
}

/**
 * 执行 displayState 方法。
 * 
 * 实现步骤：
 * 1. 读取当前页面状态或调用参数；
 * 2. 完成对应的校验、接口调用或数据转换；
 * 3. 更新页面状态或返回处理结果。
 */
function displayState(value: string | undefined, businessType: string) {
  if (!value) {
    return '-'
  }
  return enumLabel(value, 'status', businessType)
}

/**
 * 执行 enumLabel 方法。
 * 
 * 实现步骤：
 * 1. 读取当前页面状态或调用参数；
 * 2. 完成对应的校验、接口调用或数据转换；
 * 3. 更新页面状态或返回处理结果。
 */
function enumLabel(value: string, key?: string, businessType?: string) {
  if (!value) {
    return '-'
  }
  if (key === 'movementType') {
    return {
      /**
       * 字段 INBOUND：表示表单、筛选条件、接口数据或组件状态中的 INBOUND 值。
       */
      INBOUND: '入库',
      /**
       * 字段 OUTBOUND：表示表单、筛选条件、接口数据或组件状态中的 OUTBOUND 值。
       */
      OUTBOUND: '出库',
      /**
       * 字段 TRANSFER：表示表单、筛选条件、接口数据或组件状态中的 TRANSFER 值。
       */
      TRANSFER: '调拨',
      /**
       * 字段 CHECK：表示表单、筛选条件、接口数据或组件状态中的 CHECK 值。
       */
      CHECK: '盘点'
    }[value] || value
  }
  if (key === 'billType') {
    return {
      /**
       * 字段 RECEIVABLE：表示表单、筛选条件、接口数据或组件状态中的 RECEIVABLE 值。
       */
      RECEIVABLE: '应收',
      /**
       * 字段 PAYABLE：表示表单、筛选条件、接口数据或组件状态中的 PAYABLE 值。
       */
      PAYABLE: '应付'
    }[value] || value
  }
  if (key === 'status') {
    return statusLabel(value, businessType)
  }
  return value
}

/**
 * 执行 statusLabel 方法。
 * 
 * 实现步骤：
 * 1. 读取当前页面状态或调用参数；
 * 2. 完成对应的校验、接口调用或数据转换；
 * 3. 更新页面状态或返回处理结果。
 */
function statusLabel(value: string, businessType?: string) {
  /**
   * 常量 common：保存当前模块的页面状态、配置项、接口实例或计算结果。
   */
  const common: Record<string, string> = {
    /**
     * 字段 DRAFT：表示表单、筛选条件、接口数据或组件状态中的 DRAFT 值。
     */
    DRAFT: '草稿',
    /**
     * 字段 POSTED：表示表单、筛选条件、接口数据或组件状态中的 POSTED 值。
     */
    POSTED: '已过账',
    /**
     * 字段 VOIDED：表示表单、筛选条件、接口数据或组件状态中的 VOIDED 值。
     */
    VOIDED: '已作废',
    /**
     * 字段 SUBMITTED：表示表单、筛选条件、接口数据或组件状态中的 SUBMITTED 值。
     */
    SUBMITTED: '已提交',
    /**
     * 字段 APPROVED：表示表单、筛选条件、接口数据或组件状态中的 APPROVED 值。
     */
    APPROVED: '已审批',
    /**
     * 字段 RECEIVED：表示表单、筛选条件、接口数据或组件状态中的 RECEIVED 值。
     */
    RECEIVED: '已收货',
    /**
     * 字段 CLOSED：表示表单、筛选条件、接口数据或组件状态中的 CLOSED 值。
     */
    CLOSED: '已关闭',
    /**
     * 字段 CANCELLED：表示表单、筛选条件、接口数据或组件状态中的 CANCELLED 值。
     */
    CANCELLED: '已取消',
    /**
     * 字段 CREATED：表示表单、筛选条件、接口数据或组件状态中的 CREATED 值。
     */
    CREATED: '已创建',
    /**
     * 字段 DISPATCHED：表示表单、筛选条件、接口数据或组件状态中的 DISPATCHED 值。
     */
    DISPATCHED: '已发送',
    /**
     * 字段 IN_TRANSIT：表示表单、筛选条件、接口数据或组件状态中的 IN_TRANSIT 值。
     */
    IN_TRANSIT: '运输中',
    /**
     * 字段 DELIVERED：表示表单、筛选条件、接口数据或组件状态中的 DELIVERED 值。
     */
    DELIVERED: '已送达',
    /**
     * 字段 OPEN：表示表单、筛选条件、接口数据或组件状态中的 OPEN 值。
     */
    OPEN: '未结',
    /**
     * 字段 PARTIAL：表示表单、筛选条件、接口数据或组件状态中的 PARTIAL 值。
     */
    PARTIAL: '部分结清',
    /**
     * 字段 OVERDUE：表示表单、筛选条件、接口数据或组件状态中的 OVERDUE 值。
     */
    OVERDUE: '逾期'
  }
  if (businessType === 'AR_AP_BILL' && value === 'CLOSED') {
    return '已结清'
  }
  return common[value] || value
}

/**
 * 判断字段是否为金额类字段。
 *
 * 实现步骤：
 * 1. 按字段名识别金额、单价、借贷方、应收应付、余额等金额语义；
 * 2. 排除汇率、税率、数量、天数等非金额数值；
 * 3. 供流水主信息和明细表统一决定是否启用金额悬浮。
 */
function isAmountField(key?: string) {
  if (!key) {
    return false
  }
  /** 小写字段名，用于统一匹配英文业务字段。 */
  const text = key.toLowerCase()
  if (/(rate|quantity|count|days|lineno|line_no)/i.test(text)) {
    return false
  }
  return /(amount|price|balance|debit|credit|payable|receivable|remaining|total)/i.test(text)
}

/**
 * 判断字段值是否可作为金额展示。
 *
 * 实现步骤：只允许可转换成有限数字的值进入 AmountText，避免普通文本误触发金额 tooltip。
 */
function isNumericAmount(value: unknown) {
  return value !== undefined && value !== null && value !== '' && Number.isFinite(Number(value))
}

/**
 * 根据金额字段推断币种编码。
 *
 * 实现步骤：
 * 1. 人民币金额字段统一返回 CNY；
 * 2. 明细或主表有 currencyCode 时使用业务币种；
 * 3. 缺失时按人民币兜底，保证中文大写金额有稳定币种。
 */
function currencyCodeForRow(row: Record<string, unknown>, key?: string) {
  if (key && key.toLowerCase().includes('cny')) {
    return 'CNY'
  }
  /** 快照中的币种编码字段。 */
  const code = row.currencyCode
  return typeof code === 'string' && code ? code : 'CNY'
}

/**
 * 根据金额字段推断币种名称。
 *
 * 实现步骤：
 * 1. 人民币金额字段统一返回人民币；
 * 2. 明细或主表有 currencyName 时使用业务币种名称；
 * 3. 缺失时按人民币兜底。
 */
function currencyNameForRow(row: Record<string, unknown>, key?: string) {
  if (key && key.toLowerCase().includes('cny')) {
    return '人民币'
  }
  /** 快照中的币种名称字段。 */
  const name = row.currencyName
  return typeof name === 'string' && name ? name : '人民币'
}

defineExpose({ open })
</script>

<style scoped>
.drawer-filter {
  display: flex;
  flex-direction: column;
  gap: 10px;
  padding-bottom: 14px;
  border-bottom: 1px solid var(--border-color);
}

.time-picker {
  width: 100%;
}

.drawer-actions {
  display: flex;
  gap: 10px;
}

.timeline-scroll {
  height: calc(100vh - 156px);
  padding: 18px 6px 0 0;
  overflow-y: auto;
}

.timeline-card {
  padding: 10px 12px;
  border: 1px solid var(--border-color);
  border-radius: 6px;
  background: var(--surface-color);
  color: var(--text-color);
}

.timeline-title {
  color: var(--heading-color);
  font-weight: 700;
}

.timeline-detail {
  margin-top: 4px;
  color: var(--secondary-text-color);
  line-height: 1.55;
}

.snapshot-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 8px 12px;
  margin-top: 8px;
}

.snapshot-field {
  min-width: 0;
}

.snapshot-field span {
  display: block;
  color: var(--muted-text-color);
  font-size: 12px;
}

.snapshot-field strong {
  display: block;
  margin-top: 2px;
  color: var(--text-color);
  font-size: 13px;
  font-weight: 600;
  overflow-wrap: anywhere;
}

.snapshot-section {
  margin-top: 12px;
}

.snapshot-section-title {
  margin-bottom: 6px;
  color: var(--heading-color);
  font-size: 13px;
  font-weight: 700;
}

.snapshot-table {
  overflow-x: auto;
}

.snapshot-table table {
  min-width: 760px;
  width: 100%;
  border-collapse: collapse;
  font-size: 12px;
}

.snapshot-table th,
.snapshot-table td {
  padding: 6px 8px;
  border: 1px solid var(--border-color);
  text-align: left;
  white-space: nowrap;
  color: var(--text-color);
}

.snapshot-table th {
  background: var(--subtle-surface-color);
  color: var(--secondary-text-color);
  font-weight: 600;
}

.timeline-meta {
  margin-top: 4px;
  color: var(--muted-text-color);
  font-size: 12px;
}

.load-tip {
  padding: 12px 0;
  color: var(--muted-text-color);
  text-align: center;
  font-size: 12px;
}

:deep(.operation-log-drawer .el-drawer__body) {
  background: var(--surface-color);
  color: var(--text-color);
}

:deep(.el-timeline-item__timestamp) {
  color: var(--secondary-text-color);
}

:deep(.el-timeline-item__tail) {
  border-left-color: var(--border-color);
}
</style>
