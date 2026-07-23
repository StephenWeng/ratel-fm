<template>
  <div class="page">
    <div class="page-header">
      <div>
        <h1 class="page-title">统计报表</h1>
        <p class="page-subtitle">查看已过账凭证的试算平衡结果。</p>
      </div>
    </div>

    <div class="toolbar">
      <div class="date-filters">
        <el-date-picker v-model="filters.startDate" type="date" value-format="YYYY-MM-DD" placeholder="开始日期" />
        <el-date-picker v-model="filters.endDate" type="date" value-format="YYYY-MM-DD" placeholder="结束日期" />
        <el-input v-model="keyword" clearable placeholder="搜索科目编码、科目名称" style="width: 240px" />
        <el-button v-if="auth.hasMenu('BTN_REPORT_QUERY')" type="primary" :icon="Search" @click="load">查询</el-button>
      </div>
    </div>

    <el-tabs v-model="activeTab">
      <el-tab-pane label="试算平衡" name="trial">
        <div class="panel">
          <el-table v-loading="loading" :data="filteredRows" stripe show-summary :summary-method="summary">
            <el-table-column prop="subjectCode" label="科目编码" width="130" />
            <el-table-column prop="subjectName" label="科目名称" min-width="180" />
            <el-table-column prop="debitAmount" label="借方发生额" width="150" align="right"><template #default="{ row }"><AmountText :value="row.debitAmount" currency-code="CNY" currency-name="人民币" /></template></el-table-column>
            <el-table-column prop="creditAmount" label="贷方发生额" width="150" align="right"><template #default="{ row }"><AmountText :value="row.creditAmount" currency-code="CNY" currency-name="人民币" /></template></el-table-column>
            <el-table-column prop="balance" label="余额" width="150" align="right"><template #default="{ row }"><AmountText :value="row.balance" currency-code="CNY" currency-name="人民币" /></template></el-table-column>
          </el-table>
        </div>
      </el-tab-pane>
      <el-tab-pane label="资产负债表" name="balance"><statement-table :statement="balanceSheet" /></el-tab-pane>
      <el-tab-pane label="利润表" name="income"><statement-table :statement="incomeStatement" /></el-tab-pane>
      <el-tab-pane label="现金流量表" name="cash"><statement-table :statement="cashFlow" /></el-tab-pane>
    </el-tabs>
  </div>
</template>

<script setup lang="ts">
import { computed, defineComponent, h, onMounted, reactive, ref } from 'vue'
import { Search } from '@element-plus/icons-vue'
import { api } from '@/api/fm'
import AmountText from '@/components/common/AmountText.vue'
import { useAuthStore } from '@/stores/auth'
import type { FinancialStatement, TrialBalanceRow } from '@/types/api'
import { formatMoney, formatPlainMoney, toChineseCapitalAmount } from '@/utils/money'

/**
 * 常量 loading：保存当前模块的页面状态、配置项、接口实例或计算结果。
 */
const loading = ref(false)
/**
 * 常量 auth：保存当前模块的页面状态、配置项、接口实例或计算结果。
 */
const auth = useAuthStore()
/**
 * 常量 rows：保存当前模块的页面状态、配置项、接口实例或计算结果。
 */
const rows = ref<TrialBalanceRow[]>([])
/**
 * 常量 keyword：保存当前模块的页面状态、配置项、接口实例或计算结果。
 */
const keyword = ref('')
/**
 * 常量 activeTab：保存当前模块的页面状态、配置项、接口实例或计算结果。
 */
const activeTab = ref('trial')
/**
 * 常量 balanceSheet：保存当前模块的页面状态、配置项、接口实例或计算结果。
 */
const balanceSheet = ref<FinancialStatement>()
/**
 * 常量 incomeStatement：保存当前模块的页面状态、配置项、接口实例或计算结果。
 */
const incomeStatement = ref<FinancialStatement>()
/**
 * 常量 cashFlow：保存当前模块的页面状态、配置项、接口实例或计算结果。
 */
const cashFlow = ref<FinancialStatement>()
/**
 * 常量 filters：保存当前模块的页面状态、配置项、接口实例或计算结果。
 */
const filters = reactive({ startDate: '', endDate: '' })
/**
 * 常量 filteredRows：保存当前模块的页面状态、配置项、接口实例或计算结果。
 */
const filteredRows = computed(() => {
  /**
   * 常量 text：保存当前模块的页面状态、配置项、接口实例或计算结果。
   */
  const text = keyword.value.trim().toLowerCase()
  if (!text) {
    return rows.value
  }
  return rows.value.filter((row) =>
    [row.subjectCode, row.subjectName]
      .filter(Boolean)
      .some((value) => String(value).toLowerCase().includes(text))
  )
})

/**
 * 执行 money 方法。
 * 
 * 实现步骤：
 * 1. 读取当前页面状态或调用参数；
 * 2. 完成对应的校验、接口调用或数据转换；
 * 3. 更新页面状态或返回处理结果。
 */
function money(value: number) {
  return formatMoney(value)
}

/**
 * 执行 summary 方法。
 * 
 * 实现步骤：
 * 1. 读取当前页面状态或调用参数；
 * 2. 完成对应的校验、接口调用或数据转换；
 * 3. 更新页面状态或返回处理结果。
 */
function summary() {
  /**
   * 常量 debit：保存当前模块的页面状态、配置项、接口实例或计算结果。
   */
  const debit = filteredRows.value.reduce((sum, row) => sum + Number(row.debitAmount || 0), 0)
  /**
   * 常量 credit：保存当前模块的页面状态、配置项、接口实例或计算结果。
   */
  const credit = filteredRows.value.reduce((sum, row) => sum + Number(row.creditAmount || 0), 0)
  /**
   * 常量 balance：保存当前模块的页面状态、配置项、接口实例或计算结果。
   */
  const balance = filteredRows.value.reduce((sum, row) => sum + Number(row.balance || 0), 0)
  return ['合计', '', amountSummaryText(debit), amountSummaryText(credit), amountSummaryText(balance)]
}

/**
 * 生成报表合计行金额文本。
 *
 * 实现步骤：
 * 1. 保留 Element Plus summary 行的文本返回形式；
 * 2. 同时拼接数字金额和中文大写金额；
 * 3. 保证合计行也能看到完整金额含义。
 */
function amountSummaryText(value: number) {
  return `${money(value)}（${formatPlainMoney(value)} CNY / ${toChineseCapitalAmount(value, '人民币')}）`
}

/**
 * 执行 load 方法。
 * 
 * 实现步骤：
 * 1. 读取当前页面状态或调用参数；
 * 2. 完成对应的校验、接口调用或数据转换；
 * 3. 更新页面状态或返回处理结果。
 */
async function load() {
  loading.value = true
  try {
    /**
     * 常量 date：保存当前模块的页面状态、配置项、接口实例或计算结果。
     */
    const date = filters.endDate || undefined
    const [trial, balance, income, cash] = await Promise.all([
      api.trialBalance({
      /**
       * 字段 startDate：表示表单、筛选条件、接口数据或组件状态中的 startDate 值。
       */
      startDate: filters.startDate || undefined,
      /**
       * 字段 endDate：表示表单、筛选条件、接口数据或组件状态中的 endDate 值。
       */
      endDate: filters.endDate || undefined
      }),
      api.balanceSheet(date),
      api.incomeStatement(date),
      api.cashFlow(date)
    ])
    rows.value = trial
    balanceSheet.value = balance
    incomeStatement.value = income
    cashFlow.value = cash
  } finally {
    loading.value = false
  }
}

/**
 * 常量 StatementTable：保存当前模块的页面状态、配置项、接口实例或计算结果。
 */
const StatementTable = defineComponent({
  /**
   * 字段 props：表示表单、筛选条件、接口数据或组件状态中的 props 值。
   */
  props: { statement: Object },
  setup(props) {
    return () => h('div', { class: 'panel' }, [
      h('div', { class: 'panel-pad muted' }, (props.statement as FinancialStatement | undefined)?.statementName || ''),
      h('table', { class: 'plain-table' }, [
        h('tbody', ((props.statement as FinancialStatement | undefined)?.lines || []).map((line) =>
          h('tr', [
            h('td', line.itemName),
            h('td', { class: 'money', title: `数字金额：${formatPlainMoney(line.amount)} CNY\n中文大写：${toChineseCapitalAmount(line.amount, '人民币')}` }, money(line.amount))
          ])
        ))
      ])
    ])
  }
})

onMounted(load)
</script>

<style scoped>
.date-filters {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}

:deep(.plain-table) {
  width: 100%;
  border-collapse: collapse;
}

:deep(.plain-table td) {
  padding: 12px;
  border-top: 1px solid #e5e7eb;
}
</style>
