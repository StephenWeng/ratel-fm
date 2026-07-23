<template>
  <div class="page">
    <div class="page-header">
      <div>
        <h1 class="page-title">会计期间</h1>
        <p class="page-subtitle">维护月度期间，执行月结检查、结账和反结账。</p>
      </div>
    </div>

    <el-form class="filter-form" :model="filters" label-width="78px">
      <el-row :gutter="12">
        <el-col :xs="24" :sm="8" :md="6">
          <el-form-item label="期间">
            <el-date-picker
              v-model="filters.periodCode"
              type="month"
              value-format="YYYY-MM"
              class="full"
              clearable
              placeholder="请选择年月"
            />
          </el-form-item>
        </el-col>
        <el-col :xs="24" :sm="8" :md="5">
          <el-form-item label="状态">
            <el-select v-model="filters.status" clearable class="full" placeholder="全部">
              <el-option label="开启" value="OPEN" />
              <el-option label="已关闭" value="CLOSED" />
            </el-select>
          </el-form-item>
        </el-col>
        <el-col :xs="24" :sm="8" :md="8">
          <el-form-item label=" " class="filter-actions">
            <el-button type="primary" @click="load">查询</el-button>
            <el-button @click="resetFilters">重置</el-button>
            <el-button v-if="auth.hasMenu('BTN_PERIOD_CREATE')" type="primary" :icon="Plus" @click="openCreate">创建期间</el-button>
          </el-form-item>
        </el-col>
      </el-row>
    </el-form>

    <div class="panel">
      <el-table v-loading="loading" :data="rows" stripe>
        <el-table-column prop="periodCode" label="期间" width="120" />
        <el-table-column prop="startDate" label="开始日期" width="120" />
        <el-table-column prop="endDate" label="结束日期" width="120" />
        <el-table-column label="状态" width="110">
          <template #default="{ row }">
            <el-tag :type="row.status === 'CLOSED' ? 'info' : 'success'">{{ periodStatusLabel(row.status) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="closedBy" label="结账人" width="120" />
        <el-table-column prop="closedTime" label="结账时间" min-width="180">
          <template #default="{ row }">{{ formatDateTime(row.closedTime) }}</template>
        </el-table-column>
        <el-table-column prop="remark" label="备注" min-width="220" />
        <el-table-column label="操作" width="220" fixed="right">
          <template #default="{ row }">
            <div class="table-actions">
              <el-button v-if="auth.hasMenu('BTN_PERIOD_CLOSE_CHECK')" size="small" @click="checkPeriod(row)">月结检查</el-button>
              <el-button v-if="auth.hasMenu('BTN_PERIOD_CLOSE') && row.status === 'OPEN'" size="small" type="primary" @click="closePeriod(row)">结账</el-button>
              <el-button v-if="auth.hasMenu('BTN_PERIOD_REOPEN') && row.status === 'CLOSED'" size="small" @click="reopenPeriod(row)">反结账</el-button>
            </div>
          </template>
        </el-table-column>
      </el-table>
    </div>

    <el-dialog v-model="createDialogVisible" title="创建会计期间" width="420px">
      <el-form :model="createForm" label-width="86px">
        <el-form-item label="期间" required>
          <el-date-picker v-model="createForm.periodCode" type="month" value-format="YYYY-MM" class="full" placeholder="请选择月份" />
        </el-form-item>
        <el-form-item label="备注">
          <el-input v-model="createForm.remark" type="textarea" :rows="3" maxlength="2000" show-word-limit />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="createDialogVisible = false">取消</el-button>
        <el-button type="primary" @click="createPeriod">保存</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="checkDialogVisible" title="月结检查" width="560px">
      <div v-if="checkResult" class="close-check">
        <el-alert
          :title="checkResult.closable ? '当前期间允许结账' : '当前期间存在阻断项，暂不能结账'"
          :type="checkResult.closable ? 'success' : 'error'"
          show-icon
          :closable="false"
        />
        <section v-if="checkResult.blockingItems.length" class="check-section">
          <strong>阻断项</strong>
          <ul>
            <li v-for="item in checkResult.blockingItems" :key="item">{{ item }}</li>
          </ul>
        </section>
        <section v-if="checkResult.warningItems.length" class="check-section">
          <strong>提示项</strong>
          <ul>
            <li v-for="item in checkResult.warningItems" :key="item">{{ item }}</li>
          </ul>
        </section>
        <el-empty v-if="!checkResult.blockingItems.length && !checkResult.warningItems.length" description="没有发现月结风险" />
      </div>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue'
import { Plus } from '@element-plus/icons-vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { api } from '@/api/fm'
import { useAuthStore } from '@/stores/auth'
import type { AccountingPeriodStatus, AccountingPeriodView, PeriodCloseCheckView } from '@/types/api'

/**
 * 常量 auth：保存当前登录人员的菜单权限，用于控制期间操作按钮。
 */
const auth = useAuthStore()

/**
 * 常量 rows：保存会计期间表格数据。
 */
const rows = ref<AccountingPeriodView[]>([])

/**
 * 常量 loading：表示会计期间列表是否正在加载。
 */
const loading = ref(false)

/**
 * 常量 createDialogVisible：控制创建期间弹窗显示状态。
 */
const createDialogVisible = ref(false)

/**
 * 常量 checkDialogVisible：控制月结检查结果弹窗显示状态。
 */
const checkDialogVisible = ref(false)

/**
 * 常量 checkResult：保存最近一次月结检查返回结果。
 */
const checkResult = ref<PeriodCloseCheckView>()

/**
 * 常量 filters：保存会计期间筛选条件。
 */
const filters = reactive({
  periodCode: '',
  status: undefined as AccountingPeriodStatus | undefined
})

/**
 * 常量 createForm：保存创建会计期间表单数据。
 */
const createForm = reactive({
  periodCode: new Date().toISOString().slice(0, 7),
  remark: ''
})

/**
 * 加载会计期间列表。
 *
 * 实现步骤：
 * 1. 读取筛选条件；
 * 2. 调用会计期间列表接口；
 * 3. 将结果写入表格。
 */
async function load() {
  loading.value = true
  try {
    rows.value = await api.accountingPeriods({
      periodCode: filters.periodCode.trim() || undefined,
      status: filters.status
    })
  } finally {
    loading.value = false
  }
}

/**
 * 重置筛选条件。
 *
 * 实现步骤：清空期间和状态筛选，然后重新加载列表。
 */
function resetFilters() {
  filters.periodCode = ''
  filters.status = undefined
  void load()
}

/**
 * 打开创建期间弹窗。
 *
 * 实现步骤：默认选中当前年月并清空备注。
 */
function openCreate() {
  createForm.periodCode = new Date().toISOString().slice(0, 7)
  createForm.remark = ''
  createDialogVisible.value = true
}

/**
 * 创建会计期间。
 *
 * 实现步骤：
 * 1. 校验期间编码；
 * 2. 调用创建接口；
 * 3. 保存成功后关闭弹窗并刷新列表。
 */
async function createPeriod() {
  if (!createForm.periodCode) {
    ElMessage.warning('请选择会计期间')
    return
  }
  await api.createAccountingPeriod({ ...createForm })
  ElMessage.success('会计期间已创建')
  createDialogVisible.value = false
  await load()
}

/**
 * 执行月结检查。
 *
 * 实现步骤：调用检查接口并打开结果弹窗，阻断项和提示项分别展示。
 */
async function checkPeriod(row: AccountingPeriodView) {
  checkResult.value = await api.accountingPeriodCloseCheck(row.periodCode)
  checkDialogVisible.value = true
}

/**
 * 关闭会计期间。
 *
 * 实现步骤：
 * 1. 先执行月结检查；
 * 2. 如存在阻断项则打开检查弹窗并停止；
 * 3. 二次确认后调用结账接口。
 */
async function closePeriod(row: AccountingPeriodView) {
  checkResult.value = await api.accountingPeriodCloseCheck(row.periodCode)
  if (!checkResult.value.closable) {
    checkDialogVisible.value = true
    return
  }
  /** 月结检查返回的警告内容，拼接到二次确认弹窗中提醒用户。 */
  const warningText = checkResult.value.warningItems.length ? `\n${checkResult.value.warningItems.join('\n')}` : ''
  await ElMessageBox.confirm(`确认关闭会计期间 ${row.periodCode}？${warningText}`, '结账确认', { type: 'warning' })
  await api.closeAccountingPeriod(row.periodCode, { remark: row.remark || '' })
  ElMessage.success('会计期间已关闭')
  await load()
}

/**
 * 反结账并重新打开期间。
 *
 * 实现步骤：二次确认后调用反结账接口，成功后刷新列表。
 */
async function reopenPeriod(row: AccountingPeriodView) {
  await ElMessageBox.confirm(`确认反结账并重新打开 ${row.periodCode}？`, '反结账确认', { type: 'warning' })
  await api.reopenAccountingPeriod(row.periodCode, { remark: row.remark || '' })
  ElMessage.success('会计期间已重新打开')
  await load()
}

/**
 * 转换期间状态中文。
 *
 * 实现步骤：按后端状态枚举返回页面展示文本。
 */
function periodStatusLabel(status: AccountingPeriodStatus) {
  return status === 'CLOSED' ? '已关闭' : '开启'
}

/**
 * 格式化日期时间。
 *
 * 实现步骤：空值返回空字符串，非空值使用浏览器本地格式展示。
 */
function formatDateTime(value?: string) {
  return value ? new Date(value).toLocaleString('zh-CN', { hour12: false }) : ''
}

onMounted(load)
</script>

<style scoped>
.full {
  width: 100%;
}

.filter-form {
  margin-bottom: 14px;
  padding: 14px 14px 0;
  border: 1px solid var(--border-color);
  border-radius: 6px;
  background: var(--surface-color);
}

.close-check {
  display: grid;
  gap: 14px;
}

.check-section {
  padding: 12px;
  border: 1px solid var(--border-color);
  border-radius: 6px;
  background: var(--subtle-surface-color);
}

.check-section ul {
  margin: 8px 0 0;
  padding-left: 18px;
}
</style>
