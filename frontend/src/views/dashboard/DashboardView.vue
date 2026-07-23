<template>
  <div class="page">
    <div class="page-header">
      <div>
        <h1 class="page-title">财务工作台</h1>
        <p class="page-subtitle">汇总业务待办、核算建议、风险提醒和月结检查。</p>
      </div>
      <div class="dashboard-actions">
        <el-switch v-model="autoRefresh" active-text="自动刷新" />
        <el-button v-if="auth.hasMenu('BTN_DASHBOARD_REFRESH') && !autoRefresh" :icon="Refresh" :loading="loading" @click="load(false)">刷新</el-button>
      </div>
    </div>

    <div class="metric-grid">
      <div v-for="metric in metrics" :key="metric.label" class="metric">
        <div class="metric-label">{{ metric.label }}</div>
        <div class="metric-value">
          <AmountText v-if="metric.amount" :value="metric.rawValue" :display="String(metric.value)" currency-code="CNY" currency-name="人民币" />
          <template v-else>{{ metric.value }}</template>
        </div>
      </div>
    </div>

    <div class="workbench-grid">
      <section class="panel panel-pad">
        <div class="section-head">
          <h3 class="section-title">风险提醒</h3>
          <span class="section-count">{{ overview?.risks?.length ?? 0 }} 条</span>
        </div>
        <div class="risk-list">
          <button v-for="risk in overview?.risks ?? []" :key="`${risk.title}-${risk.description}`" class="risk-row" type="button" @click="go(risk)">
            <el-tag :type="tagType(risk.level)" effect="light">{{ severityLabel(risk.level) }}</el-tag>
            <span>
              <strong>{{ risk.title }}</strong>
              <small>{{ risk.description }}</small>
            </span>
          </button>
        </div>
      </section>

      <section class="panel panel-pad">
        <div class="section-head">
          <h3 class="section-title">快捷入口</h3>
          <span class="section-count">常用业务</span>
        </div>
        <div class="quick-links">
          <el-button v-for="entry in quickEntries" :key="entry.menuCode" @click="openQuickEntry(entry)">
            {{ entry.label }}
          </el-button>
          <span v-if="quickEntries.length === 0" class="empty-text">暂无常用功能</span>
        </div>
      </section>
    </div>

    <section class="panel panel-pad">
      <div class="section-head">
        <h3 class="section-title">智能核算建议</h3>
        <span class="section-count">{{ overview?.accountingSuggestions?.length ?? 0 }} 条</span>
      </div>
      <el-table :data="overview?.accountingSuggestions ?? []" empty-text="暂无核算建议" stripe>
        <el-table-column prop="sourceNo" label="来源单号" min-width="150" />
        <el-table-column prop="title" label="建议事项" min-width="150" />
        <el-table-column label="建议分录" min-width="230">
          <template #default="{ row }">
            <span>{{ row.debitSubject }} / {{ row.creditSubject }}</span>
          </template>
        </el-table-column>
        <el-table-column label="金额" width="140" align="right">
          <template #default="{ row }"><AmountText :value="row.amount ?? 0" :display="money.format(row.amount ?? 0)" currency-code="CNY" currency-name="人民币" /></template>
        </el-table-column>
        <el-table-column prop="reason" label="依据" min-width="280" show-overflow-tooltip />
        <el-table-column label="操作" width="88" align="right">
          <template #default="{ row }">
            <el-button link type="primary" @click="go(row)">进入</el-button>
          </template>
        </el-table-column>
      </el-table>
    </section>

    <div class="workbench-grid">
      <section class="panel panel-pad">
        <div class="section-head">
          <h3 class="section-title">待办中心</h3>
          <span class="section-count">{{ overview?.todos?.length ?? 0 }} 项</span>
        </div>
        <el-table :data="overview?.todos ?? []" class="compact-table" empty-text="暂无待办" stripe>
          <el-table-column prop="title" label="事项" min-width="120" />
          <el-table-column prop="description" label="说明" min-width="260" show-overflow-tooltip />
          <el-table-column label="级别" width="88">
            <template #default="{ row }">
              <el-tag :type="tagType(row.severity)" effect="light">{{ severityLabel(row.severity) }}</el-tag>
            </template>
          </el-table-column>
          <el-table-column label="操作" width="88" align="right">
            <template #default="{ row }">
              <el-button link type="primary" @click="go(row)">进入</el-button>
            </template>
          </el-table-column>
        </el-table>
      </section>

      <section class="panel panel-pad">
        <div class="section-head">
          <h3 class="section-title">月结检查</h3>
          <span class="section-count">{{ closeReadyText }}</span>
        </div>
        <div class="check-list">
          <button v-for="item in overview?.monthCloseChecks ?? []" :key="item.code" class="check-row" type="button" @click="go(item)">
            <el-icon :class="['check-icon', item.status]"><CircleCheck v-if="item.status === 'success'" /><Warning v-else /></el-icon>
            <span>
              <strong>{{ item.title }}</strong>
              <small>{{ item.description }}</small>
            </span>
          </button>
        </div>
      </section>
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed, onBeforeUnmount, onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import { CircleCheck, Refresh, Warning } from '@element-plus/icons-vue'
import { api } from '@/api/fm'
import AmountText from '@/components/common/AmountText.vue'
import { useKeepAlivePolling } from '@/composables/useKeepAlivePolling'
import { useAuthStore } from '@/stores/auth'
import { MENU_USAGE_UPDATED, frequentMenus, menuUsageUserKey, recordMenuUsage, syncMenuUsageFromServer, type FrequentMenu } from '@/utils/menuUsage'
import type { DashboardOverview } from '@/types/api'

/** 首页概览待办、风险和建议中可跳转业务项的导航信息。 */
interface NavigableItem {
  /** 目标业务页面；后端未提供时该概览项仅用于展示。 */
  routePath?: string
  /** 目标页面用于定位业务数据的查询参数名。 */
  searchKey?: string
  /** 与查询参数名配套的业务定位值。 */
  searchValue?: string
}

/** 首页概览请求加载状态，用于手动刷新按钮反馈。 */
const loading = ref(false)
/** 当前登录人员及其菜单权限。 */
const auth = useAuthStore()
/** 处理概览业务项和快捷入口的站内跳转。 */
const router = useRouter()
/** 后端汇总的首页指标、风险、建议、待办和月结检查结果。 */
const overview = ref<DashboardOverview>()
/** 用户控制的自动刷新开关，连续失败达到阈值时也会自动关闭。 */
const autoRefresh = ref(true)
/** 自动刷新连续失败次数，达到上限后停止轮询。 */
const autoRefreshFailures = ref(0)
/** 标记概览请求正在执行，避免 5 秒定时器叠加未完成的请求。 */
const overviewRequestRunning = ref(false)
/** 后台接口连续失败上限。 */
const MAX_AUTO_REFRESH_FAILURES = 5
/** 首页概览自动刷新周期，集中定义以便后续按部署环境配置。 */
const OVERVIEW_REFRESH_INTERVAL_MS = 5000
/** 后端严重级别与首页中文展示文案的映射，新增级别时在此集中扩展。 */
const SEVERITY_LABELS: Readonly<Record<string, string>> = {
  success: '正常',
  primary: '关注',
  warning: '预警',
  danger: '紧急'
}
/** 常用功能本地计数变更版本，用于触发快捷入口重新计算。 */
const usageVersion = ref(0)
/**
 * 常用功能兜底菜单编码，用户没有使用记录时按业务常用菜单展示。
 */
const QUICK_ENTRY_FALLBACK_CODES = ['PAGE_VOUCHERS', 'PAGE_PURCHASE', 'PAGE_INVENTORY', 'PAGE_AR_AP', 'PAGE_REPORTS', 'PAGE_SEARCH']

/** 首页人民币指标格式化器，金额统一保留两位小数。 */
const money = new Intl.NumberFormat('zh-CN', {
  style: 'currency',
  currency: 'CNY',
  minimumFractionDigits: 2,
  maximumFractionDigits: 2
})

/** 将概览汇总值转换为顶部指标卡统一展示模型。 */
const metrics = computed(() => [
  { label: '凭证总数', value: overview.value?.voucherCount ?? 0, amount: false, rawValue: overview.value?.voucherCount ?? 0 },
  { label: '草稿凭证', value: overview.value?.draftVoucherCount ?? 0, amount: false, rawValue: overview.value?.draftVoucherCount ?? 0 },
  { label: '待跟进采购', value: overview.value?.pendingPurchaseCount ?? 0, amount: false, rawValue: overview.value?.pendingPurchaseCount ?? 0 },
  { label: '在途物流', value: overview.value?.inTransitShipmentCount ?? 0, amount: false, rawValue: overview.value?.inTransitShipmentCount ?? 0 },
  { label: '逾期往来', value: overview.value?.overdueArApCount ?? 0, amount: false, rawValue: overview.value?.overdueArApCount ?? 0 },
  { label: '采购总金额', value: money.format(overview.value?.purchaseTotal ?? 0), amount: true, rawValue: overview.value?.purchaseTotal ?? 0 },
  { label: '未结应收', value: money.format(overview.value?.receivableOpenAmount ?? 0), amount: true, rawValue: overview.value?.receivableOpenAmount ?? 0 },
  { label: '未结应付', value: money.format(overview.value?.payableOpenAmount ?? 0), amount: true, rawValue: overview.value?.payableOpenAmount ?? 0 }
])

/** 全部月结检查通过时展示可月结，否则提示继续处理。 */
const closeReadyText = computed(() => {
  /** 当前期间的月结检查项；接口未返回时按未就绪处理。 */
  const checks = overview.value?.monthCloseChecks ?? []
  return checks.length && checks.every((item) => item.status === 'success') ? '可月结' : '需处理'
})

/**
 * 当前用户常用功能快捷入口。
 */
const quickEntries = computed(() => {
  usageVersion.value
  return frequentMenus(menuUsageUserKey(auth.user), auth.menus, 10, QUICK_ENTRY_FALLBACK_CODES)
})

/**
 * 加载首页概览并维护自动刷新失败计数。
 * 
 * 实现步骤：
 * 1. 忽略尚未结束的重复请求，避免慢接口导致并发叠加；
 * 2. 请求成功后更新概览并清零失败次数；
 * 3. 静默轮询连续失败达到阈值时关闭自动刷新，手动刷新失败仍由全局请求层提示。
 */
async function load(silentErrorNotice = false) {
  if (overviewRequestRunning.value) {
    return
  }
  overviewRequestRunning.value = true
  loading.value = true
  try {
    overview.value = await api.overview(silentErrorNotice)
    autoRefreshFailures.value = 0
  } catch {
    if (silentErrorNotice) {
      autoRefreshFailures.value += 1
      if (autoRefreshFailures.value >= MAX_AUTO_REFRESH_FAILURES) {
        autoRefresh.value = false
      }
    }
  } finally {
    loading.value = false
    overviewRequestRunning.value = false
  }
}

/**
 * 将后端严重级别转换为 Element Plus 标签类型，未知值统一按普通关注展示。
 */
function tagType(level?: string) {
  if (level === 'danger' || level === 'warning' || level === 'success') {
    return level
  }
  return 'primary'
}

/**
 * 将后端严重级别转换为用户可读中文，映射集中维护便于扩展新级别。
 */
function severityLabel(level?: string) {
  return SEVERITY_LABELS[level || ''] || '关注'
}

/**
 * 跳转到概览项对应业务页面。
 * 
 * 实现步骤：
 * 1. 没有目标页面的展示项不执行跳转；
 * 2. 后端同时提供查询键和值时携带定位参数；
 * 3. 交由路由器进入目标业务页面。
 */
function go(item: NavigableItem) {
  if (!item.routePath) {
    return
  }
  /** 可选业务定位参数，用于目标列表自动筛选对应单据。 */
  const query = item.searchKey && item.searchValue ? { [item.searchKey]: item.searchValue } : undefined
  void router.push({ path: item.routePath, query })
}

/**
 * 打开常用功能，并把本次点击也计入常用次数。
 */
function openQuickEntry(entry: FrequentMenu) {
  recordMenuUsage(menuUsageUserKey(auth.user), entry)
  void router.push(entry.path)
}

/**
 * 接收其他组件写入常用功能后的刷新信号。
 */
function refreshQuickEntries() {
  usageVersion.value += 1
}

useKeepAlivePolling({
  enabled: autoRefresh,
  task: () => load(true),
  intervalMs: OVERVIEW_REFRESH_INTERVAL_MS,
  onStart: () => {
    autoRefreshFailures.value = 0
  }
})

onMounted(() => {
  window.addEventListener(MENU_USAGE_UPDATED, refreshQuickEntries)
  void syncMenuUsageFromServer(menuUsageUserKey(auth.user), 10)
})
onBeforeUnmount(() => {
  window.removeEventListener(MENU_USAGE_UPDATED, refreshQuickEntries)
})
</script>

<style scoped>
.dashboard-actions {
  display: flex;
  align-items: center;
  gap: 12px;
}

.workbench-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 16px;
  margin-top: 16px;
}

.section-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  margin-bottom: 12px;
}

.section-title {
  margin: 0;
  color: var(--heading-color);
  font-size: 16px;
}

.section-count {
  color: var(--muted-text-color);
  font-size: 13px;
}

.compact-table {
  min-height: 260px;
}

.check-list,
.risk-list {
  display: grid;
  gap: 10px;
}

.check-row,
.risk-row {
  display: grid;
  grid-template-columns: auto minmax(0, 1fr);
  align-items: start;
  gap: 10px;
  width: 100%;
  padding: 10px 0;
  border: 0;
  border-bottom: 1px solid var(--border-color);
  background: transparent;
  color: inherit;
  text-align: left;
  cursor: pointer;
}

.check-row:last-child,
.risk-row:last-child {
  border-bottom: 0;
}

.check-row strong,
.risk-row strong {
  display: block;
  color: var(--heading-color);
  font-size: 14px;
  font-weight: 600;
}

.check-row small,
.risk-row small {
  display: block;
  margin-top: 4px;
  color: var(--secondary-text-color);
  font-size: 13px;
  line-height: 1.5;
}

.check-icon {
  margin-top: 2px;
  font-size: 18px;
}

.check-icon.success {
  color: var(--success-color);
}

.check-icon.warning {
  color: var(--warning-color);
}

.check-icon.danger {
  color: var(--danger-color);
}

.quick-links {
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
}

.empty-text {
  color: var(--muted-text-color);
  font-size: 13px;
}

@media (max-width: 960px) {
  .workbench-grid {
    grid-template-columns: 1fr;
  }
}
</style>
