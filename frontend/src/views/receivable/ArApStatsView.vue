<template>
  <div class="page">
    <div class="page-header">
      <div>
        <h1 class="page-title">收付统计</h1>
        <p class="page-subtitle">按项目和客户/供应商统计应收、应付、已收、已付、待收和待付金额。</p>
      </div>
    </div>

    <el-form class="filter-form" :model="filters" label-width="96px">
      <el-row :gutter="12">
        <el-col :xs="24" :sm="12" :md="8" :lg="5">
          <el-form-item label="项目">
            <el-select v-model="filters.projectCode" clearable filterable class="full" placeholder="全部项目">
              <el-option v-for="item in projectOptions" :key="item.id" :label="item.name" :value="item.code" />
            </el-select>
          </el-form-item>
        </el-col>
        <el-col :xs="24" :sm="12" :md="8" :lg="5">
          <el-form-item label="客户/供应商">
            <el-select v-model="filters.partnerName" clearable filterable class="full" placeholder="全部往来单位">
              <el-option v-for="item in partnerOptions" :key="item.id" :label="item.name" :value="item.name" />
            </el-select>
          </el-form-item>
        </el-col>
        <el-col :xs="24" :sm="12" :md="8" :lg="6">
          <el-form-item label=" " class="filter-actions">
            <el-button v-if="auth.hasMenu('BTN_AR_AP_STATS_QUERY')" type="primary" :icon="Search" @click="load">查询</el-button>
            <el-button @click="resetFilters">重置</el-button>
          </el-form-item>
        </el-col>
      </el-row>
    </el-form>

    <div class="stats-groups">
      <div class="stats-group">
        <div class="stats-group__title">付款统计</div>
        <div class="stats-grid">
          <div class="metric">
            <span>应付合计</span>
            <strong><AmountText :value="stats.totalPayableAmount" currency-code="CNY" currency-name="人民币" /></strong>
          </div>
          <div class="metric">
            <span>已付合计</span>
            <strong><AmountText :value="stats.totalPaidAmount" currency-code="CNY" currency-name="人民币" /></strong>
          </div>
          <div class="metric">
            <span>待付合计</span>
            <strong><AmountText :value="stats.totalPendingPayableAmount" currency-code="CNY" currency-name="人民币" /></strong>
          </div>
        </div>
      </div>
      <div class="stats-group">
        <div class="stats-group__title">收款统计</div>
        <div class="stats-grid">
          <div class="metric">
            <span>应收合计</span>
            <strong><AmountText :value="stats.totalReceivableAmount" currency-code="CNY" currency-name="人民币" /></strong>
          </div>
          <div class="metric">
            <span>已收合计</span>
            <strong><AmountText :value="stats.totalReceivedAmount" currency-code="CNY" currency-name="人民币" /></strong>
          </div>
          <div class="metric">
            <span>待收合计</span>
            <strong><AmountText :value="stats.totalPendingReceivableAmount" currency-code="CNY" currency-name="人民币" /></strong>
          </div>
        </div>
      </div>
    </div>

    <div class="panel">
      <el-table v-loading="loading" :data="stats.rows" stripe show-summary :summary-method="summaryMethod">
        <el-table-column prop="billNo" label="应收应付单号" min-width="160" />
        <el-table-column prop="billType" label="类型" width="100">
          <template #default="{ row }">{{ billTypeLabel(row.billType) }}</template>
        </el-table-column>
        <el-table-column prop="projectName" label="项目" min-width="140" />
        <el-table-column prop="partnerName" label="客户/供应商" min-width="180" />
        <el-table-column prop="payableAmount" label="应付金额" width="140" align="right">
          <template #default="{ row }"><AmountText :value="row.payableAmount" currency-code="CNY" currency-name="人民币" /></template>
        </el-table-column>
        <el-table-column prop="paidAmount" label="已付金额" width="140" align="right">
          <template #default="{ row }"><AmountText :value="row.paidAmount" currency-code="CNY" currency-name="人民币" /></template>
        </el-table-column>
        <el-table-column prop="pendingPayableAmount" label="待付金额" width="140" align="right">
          <template #default="{ row }"><AmountText :value="row.pendingPayableAmount" currency-code="CNY" currency-name="人民币" /></template>
        </el-table-column>
        <el-table-column prop="receivableAmount" label="应收金额" width="140" align="right">
          <template #default="{ row }"><AmountText :value="row.receivableAmount" currency-code="CNY" currency-name="人民币" /></template>
        </el-table-column>
        <el-table-column prop="receivedAmount" label="已收金额" width="140" align="right">
          <template #default="{ row }"><AmountText :value="row.receivedAmount" currency-code="CNY" currency-name="人民币" /></template>
        </el-table-column>
        <el-table-column prop="pendingReceivableAmount" label="待收金额" width="140" align="right">
          <template #default="{ row }"><AmountText :value="row.pendingReceivableAmount" currency-code="CNY" currency-name="人民币" /></template>
        </el-table-column>
      </el-table>
    </div>
  </div>
</template>

<script setup lang="ts">
import { onActivated, onMounted, reactive, ref } from 'vue'
import { Search } from '@element-plus/icons-vue'
import { api } from '@/api/fm'
import AmountText from '@/components/common/AmountText.vue'
import { useAuthStore } from '@/stores/auth'
import type { ArApPaymentStatsView, BasicDictionaryView } from '@/types/api'
import { formatMoney, formatPlainMoney, toChineseCapitalAmount } from '@/utils/money'

/**
 * 常量 auth：保存当前登录用户授权信息，用于控制收付统计查询按钮权限。
 */
const auth = useAuthStore()
/**
 * 常量 loading：表示收付统计接口是否正在查询中。
 */
const loading = ref(false)
/**
 * 常量 projectOptions：保存项目字典选项，供统计筛选条件使用。
 */
const projectOptions = ref<BasicDictionaryView[]>([])
/**
 * 常量 partnerOptions：保存客户/供应商字典选项，供统计筛选条件使用。
 */
const partnerOptions = ref<BasicDictionaryView[]>([])
/**
 * 常量 filters：保存收付统计搜索条件。
 */
const filters = reactive({
  /**
   * 字段 projectCode：表示项目字典编码，空值代表统计全部项目。
   */
  projectCode: '',
  /**
   * 字段 partnerName：表示客户或供应商名称，空值代表统计全部往来单位。
   */
  partnerName: ''
})
/**
 * 常量 stats：保存收付统计接口返回的明细和汇总金额。
 */
const stats = reactive<ArApPaymentStatsView>({
  /**
   * 字段 rows：表示每个应收应付单号的统计明细。
   */
  rows: [],
  /**
   * 字段 totalReceivableAmount：表示全部应收金额。
   */
  totalReceivableAmount: 0,
  /**
   * 字段 totalPayableAmount：表示全部应付金额。
   */
  totalPayableAmount: 0,
  /**
   * 字段 totalReceivedAmount：表示全部已收金额。
   */
  totalReceivedAmount: 0,
  /**
   * 字段 totalPaidAmount：表示全部已付金额。
   */
  totalPaidAmount: 0,
  /**
   * 字段 totalPendingReceivableAmount：表示全部待收金额。
   */
  totalPendingReceivableAmount: 0,
  /**
   * 字段 totalPendingPayableAmount：表示全部待付金额。
   */
  totalPendingPayableAmount: 0
})

/**
 * 加载收付统计数据。
 *
 * 实现步骤：
 * 1. 读取当前项目和客户/供应商筛选条件；
 * 2. 调用后端收付统计接口获取明细行和汇总金额；
 * 3. 覆盖页面统计状态，驱动顶部汇总和表格同步刷新。
 */
async function load() {
  loading.value = true
  try {
    /** 收付统计接口返回结果，包含明细行和汇总金额。 */
    const result = await api.arApPaymentStats({
      /**
       * 字段 projectCode：表示项目筛选条件。
       */
      projectCode: filters.projectCode || undefined,
      /**
       * 字段 partnerName：表示客户或供应商筛选条件。
       */
      partnerName: filters.partnerName || undefined
    })
    Object.assign(stats, result)
  } finally {
    loading.value = false
  }
}

/**
 * 重置收付统计查询条件。
 *
 * 实现步骤：
 * 1. 清空项目筛选；
 * 2. 清空客户/供应商筛选；
 * 3. 重新查询全部统计数据。
 */
function resetFilters() {
  filters.projectCode = ''
  filters.partnerName = ''
  void load()
}

/**
 * 加载页面需要的字典选项。
 *
 * 实现步骤：
 * 1. 并行读取项目字典和往来单位字典；
 * 2. 分别保存到对应下拉选项；
 * 3. 页面初始化后立即使用这些选项进行筛选。
 */
async function loadOptions() {
  const [projects, partners] = await Promise.all([
    api.enabledDictionaryChildren('PROJECT'),
    api.enabledDictionaryChildren('PARTNER')
  ])
  projectOptions.value = projects
  partnerOptions.value = partners
}

/**
 * 格式化金额展示。
 *
 * 实现步骤：
 * 1. 空值按 0 处理；
 * 2. 使用中文区域格式化千分位；
 * 3. 固定显示 8 位小数，保持和业务金额精度一致。
 */
function money(value: number) {
  return formatMoney(value)
}

/**
 * 转换应收应付类型展示文本。
 *
 * 实现步骤：
 * 1. RECEIVABLE 显示为应收；
 * 2. PAYABLE 显示为应付；
 * 3. 未识别值原样返回，便于发现异常数据。
 */
function billTypeLabel(value: string) {
  return value === 'RECEIVABLE' ? '应收' : value === 'PAYABLE' ? '应付' : value
}

/**
 * 生成表格底部汇总行。
 *
 * 实现步骤：
 * 1. 前两列显示汇总标识；
 * 2. 金额列使用后端返回的汇总值；
 * 3. 非金额列保持为空，避免误导为明细字段。
 */
function summaryMethod() {
  return [
    '合计',
    '',
    '',
    '',
    amountSummaryText(stats.totalPayableAmount),
    amountSummaryText(stats.totalPaidAmount),
    amountSummaryText(stats.totalPendingPayableAmount),
    amountSummaryText(stats.totalReceivableAmount),
    amountSummaryText(stats.totalReceivedAmount),
    amountSummaryText(stats.totalPendingReceivableAmount)
  ]
}

/**
 * 生成收付统计合计行金额文本。
 *
 * 实现步骤：
 * 1. 合计行保留普通文本输出；
 * 2. 文本中追加数字金额和中文大写；
 * 3. 让表格底部汇总也能提供完整金额信息。
 */
function amountSummaryText(value: number) {
  return `${money(value)}（${formatPlainMoney(value)} CNY / ${toChineseCapitalAmount(value, '人民币')}）`
}

onMounted(async () => {
  await Promise.all([loadOptions(), load()])
})

onActivated(() => {
  void loadOptions()
})
</script>

<style scoped>
.filter-form {
  margin-bottom: 14px;
  padding: 14px 14px 0;
  border: 1px solid #e5e7eb;
  border-radius: 6px;
  background: #fff;
}

.full {
  width: 100%;
}

.stats-groups {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 12px;
  margin-bottom: 14px;
}

.stats-group {
  display: grid;
  gap: 10px;
}

.stats-group__title {
  color: var(--heading-color);
  font-size: 14px;
  font-weight: 700;
}

.stats-grid {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 12px;
}

.metric {
  display: flex;
  min-height: 78px;
  flex-direction: column;
  justify-content: center;
  gap: 8px;
  padding: 14px 16px;
  border: 1px solid #e5e7eb;
  border-radius: 6px;
  background: #fff;
}

.metric span {
  color: #6b7280;
  font-size: 13px;
}

.metric strong {
  color: #111827;
  font-size: 20px;
  font-weight: 700;
}

@media (max-width: 900px) {
  .stats-groups {
    grid-template-columns: 1fr;
  }
}

@media (max-width: 560px) {
  .stats-grid {
    grid-template-columns: 1fr;
  }
}
</style>
