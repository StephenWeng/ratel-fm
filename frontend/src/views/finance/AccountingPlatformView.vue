<template>
  <div class="page">
    <div class="page-header">
      <div>
        <h1 class="page-title">会计平台</h1>
        <p class="page-subtitle">从业务单据生成凭证草稿，保留来源链路。</p>
      </div>
      <div class="header-actions">
        <el-segmented v-model="sourceType" :options="sourceTypeOptions" />
        <el-button v-if="auth.hasMenu('BTN_ACCOUNTING_SOURCE_QUERY')" type="primary" @click="loadSources">刷新来源</el-button>
      </div>
    </div>

    <div class="accounting-layout">
      <section class="panel source-panel">
        <div class="section-head">
          <strong>业务来源</strong>
          <span class="muted">{{ sources.length }} 条</span>
        </div>
        <el-table
          v-loading="loading"
          :data="sources"
          row-key="sourceNo"
          stripe
          highlight-current-row
          height="520"
          @current-change="selectSource"
        >
          <el-table-column prop="sourceNo" label="来源单号" min-width="150" />
          <el-table-column prop="partnerName" label="往来单位" min-width="150" />
          <el-table-column prop="projectName" label="项目" min-width="120" />
          <el-table-column prop="businessDate" label="日期" width="110" />
          <el-table-column label="金额" width="140" align="right">
            <template #default="{ row }"><AmountText :value="row.amount" :display="`${money(row.amount)} ${row.currencyCode}`" :currency-code="row.currencyCode" :currency-name="row.currencyName" /></template>
          </el-table-column>
          <el-table-column label="状态" width="105">
            <template #default="{ row }">
              <el-tag :type="row.voucherGenerated ? 'warning' : 'success'">{{ row.voucherGenerated ? '已制证' : row.statusText }}</el-tag>
            </template>
          </el-table-column>
        </el-table>
      </section>

      <section class="panel form-panel">
        <div class="section-head">
          <strong>凭证草稿</strong>
          <el-tag v-if="currentSource" type="info">{{ currentSource.sourceTitle }}</el-tag>
        </div>
        <el-empty v-if="!currentSource" description="请选择左侧业务来源" />
        <el-form v-else :model="form" label-width="94px" class="auto-voucher-form">
          <el-form-item label="来源单号">
            <el-input :model-value="currentSource.sourceNo" disabled />
          </el-form-item>
          <el-form-item label="业务金额">
            <div class="amount-field">
              <AmountText :value="currentSource.amount" :display="`${money(currentSource.amount)} ${currentSource.currencyCode}`" :currency-code="currentSource.currencyCode" :currency-name="currentSource.currencyName" />
              <span>/</span>
              <AmountText :value="currentSource.amountCny" :display="`人民币 ${money(currentSource.amountCny)}`" currency-code="CNY" currency-name="人民币" />
            </div>
          </el-form-item>
          <el-form-item label="借方科目" required>
            <el-cascader
              v-model="form.debitSubjectId"
              :options="subjectCascaderOptions"
              :props="subjectCascaderProps"
              filterable
              clearable
              class="full"
              placeholder="请选择借方科目"
              separator=" / "
            />
          </el-form-item>
          <el-form-item label="贷方科目" required>
            <el-cascader
              v-model="form.creditSubjectId"
              :options="subjectCascaderOptions"
              :props="subjectCascaderProps"
              filterable
              clearable
              class="full"
              placeholder="请选择贷方科目"
              separator=" / "
            />
          </el-form-item>
          <el-form-item label="凭证摘要">
            <el-input v-model="form.summary" type="textarea" :rows="3" maxlength="200" show-word-limit />
          </el-form-item>
          <el-form-item label="允许重复">
            <el-switch v-model="form.allowDuplicate" active-text="允许重复制证" inactive-text="拦截重复" />
          </el-form-item>
          <div class="voucher-preview">
            <div class="preview-row">
              <span>借</span>
              <strong>{{ subjectName(form.debitSubjectId) || '未选择科目' }}</strong>
              <AmountText :value="currentSource.amount" :display="`${money(currentSource.amount)} ${currentSource.currencyCode}`" :currency-code="currentSource.currencyCode" :currency-name="currentSource.currencyName" />
            </div>
            <div class="preview-row">
              <span>贷</span>
              <strong>{{ subjectName(form.creditSubjectId) || '未选择科目' }}</strong>
              <AmountText :value="currentSource.amount" :display="`${money(currentSource.amount)} ${currentSource.currencyCode}`" :currency-code="currentSource.currencyCode" :currency-name="currentSource.currencyName" />
            </div>
          </div>
          <div class="form-actions">
            <el-button @click="resetForm">重置</el-button>
            <el-button
              v-if="auth.hasMenu('BTN_ACCOUNTING_AUTO_VOUCHER') && canSubmit"
              type="primary"
              :loading="submitting"
              @click="generateVoucher"
            >
              生成凭证草稿
            </el-button>
          </div>
        </el-form>
      </section>
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, reactive, ref, watch } from 'vue'
import { ElMessage } from 'element-plus'
import { api } from '@/api/fm'
import AmountText from '@/components/common/AmountText.vue'
import { useAuthStore } from '@/stores/auth'
import type { AccountingSourceType, AccountingSourceView, SubjectView } from '@/types/api'
import { buildSubjectCascaderOptions, subjectCascaderProps, subjectNamePath } from '@/utils/subjects'

/**
 * 常量 auth：保存当前登录用户授权信息，用于控制会计平台按钮权限。
 */
const auth = useAuthStore()

/**
 * 常量 sourceTypeOptions：定义会计平台支持的业务来源类型。
 */
const sourceTypeOptions = [
  { label: '采购单', value: 'PURCHASE_ORDER' },
  { label: '应收应付', value: 'AR_AP_BILL' },
  { label: '库存流水', value: 'INVENTORY_LEDGER' },
  { label: '出纳流水', value: 'CASHIER_TRANSACTION' }
]

/**
 * 常量 sourceType：保存当前选中的业务来源类型。
 */
const sourceType = ref<AccountingSourceType>('PURCHASE_ORDER')

/**
 * 常量 loading：表示业务来源列表是否正在加载。
 */
const loading = ref(false)

/**
 * 常量 submitting：表示自动生成凭证接口是否正在提交。
 */
const submitting = ref(false)

/**
 * 常量 sources：保存会计平台业务来源行。
 */
const sources = ref<AccountingSourceView[]>([])

/**
 * 常量 currentSource：保存当前选中的业务来源。
 */
const currentSource = ref<AccountingSourceView>()

/**
 * 常量 allSubjects：保存完整启用科目层级，用于会计平台按树型结构选择借贷科目。
 */
const allSubjects = ref<SubjectView[]>([])

/**
 * 常量 subjectOptions：保存后端判定可用于记账的启用叶子科目。
 */
const subjectOptions = ref<SubjectView[]>([])

/**
 * 常量 subjectCascaderOptions：保存只展示科目名称的树型级联选项。
 */
const subjectCascaderOptions = computed(() => buildSubjectCascaderOptions(allSubjects.value, subjectOptions.value))

/**
 * 常量 form：保存自动生成凭证表单数据。
 */
const form = reactive({
  debitSubjectId: undefined as number | undefined,
  creditSubjectId: undefined as number | undefined,
  summary: '',
  allowDuplicate: false
})

/**
 * 常量 canSubmit：判断当前表单是否满足自动制证提交条件。
 */
const canSubmit = computed(() => Boolean(currentSource.value && form.debitSubjectId && form.creditSubjectId && form.debitSubjectId !== form.creditSubjectId))

/**
 * 加载会计平台业务来源。
 *
 * 实现步骤：
 * 1. 清理当前选中来源；
 * 2. 按来源类型调用后端会计平台来源接口；
 * 3. 将返回结果写入表格，供财务人员选择制证。
 */
async function loadSources() {
  loading.value = true
  try {
    currentSource.value = undefined
    sources.value = await api.accountingSources(sourceType.value)
  } finally {
    loading.value = false
  }
}

/**
 * 加载启用会计科目。
 *
 * 实现步骤：
 * 1. 调用会计科目接口，只取启用科目；
 * 2. 写入借贷科目下拉框，停用科目不得继续制证。
 */
async function loadSubjects() {
  const [allRows, businessRows] = await Promise.all([
    api.subjects(false, { enabled: true }),
    api.subjects(true)
  ])
  allSubjects.value = allRows
  subjectOptions.value = businessRows
}

/**
 * 选择业务来源。
 *
 * 实现步骤：
 * 1. 保存表格当前行；
 * 2. 生成默认凭证摘要；
 * 3. 如果该来源已经制证，保留提示但允许用户手动开启重复制证。
 */
function selectSource(row?: AccountingSourceView) {
  currentSource.value = row
  form.summary = row ? `自动凭证-${row.sourceTitle}${row.partnerName ? `-${row.partnerName}` : ''}` : ''
  form.allowDuplicate = false
}

/**
 * 重置自动凭证表单。
 *
 * 实现步骤：保留当前来源，清空借贷科目和摘要，关闭重复制证开关。
 */
function resetForm() {
  form.debitSubjectId = undefined
  form.creditSubjectId = undefined
  form.summary = currentSource.value ? `自动凭证-${currentSource.value.sourceTitle}` : ''
  form.allowDuplicate = false
}

/**
 * 自动生成凭证草稿。
 *
 * 实现步骤：
 * 1. 校验当前已选择业务来源和借贷科目；
 * 2. 调用后端自动制证接口；
 * 3. 成功后提示凭证号，并刷新来源列表，使已制证状态立即可见。
 */
async function generateVoucher() {
  if (!currentSource.value || !canSubmit.value) {
    ElMessage.warning('请选择来源单据和借贷科目')
    return
  }
  submitting.value = true
  try {
    /** 自动生成凭证接口返回结果，包含提示消息和生成的凭证草稿信息。 */
    const result = await api.generateAutoVoucher({
      sourceType: currentSource.value.sourceType,
      sourceId: currentSource.value.sourceId,
      debitSubjectId: form.debitSubjectId,
      creditSubjectId: form.creditSubjectId,
      summary: form.summary,
      allowDuplicate: form.allowDuplicate
    })
    ElMessage.success(result.message || `已生成凭证草稿：${result.voucher.voucherNo}`)
    await loadSources()
  } finally {
    submitting.value = false
  }
}

/**
 * 格式化金额。
 *
 * 实现步骤：将空值按 0 处理，再按中文地区格式输出 2 到 8 位小数。
 */
function money(value: number) {
  return new Intl.NumberFormat('zh-CN', { minimumFractionDigits: 2, maximumFractionDigits: 8 }).format(Number(value || 0))
}

/**
 * 根据科目 ID 返回科目显示名称。
 *
 * 实现步骤：在完整启用科目树中查找匹配 ID，只展示科目名称级联路径。
 */
function subjectName(subjectId?: number) {
  return subjectNamePath(subjectId, allSubjects.value)
}

watch(sourceType, loadSources)

onMounted(async () => {
  await Promise.all([loadSubjects(), loadSources()])
})
</script>

<style scoped>
.accounting-layout {
  display: grid;
  grid-template-columns: minmax(0, 1.4fr) minmax(360px, 0.8fr);
  gap: 14px;
}

.source-panel,
.form-panel {
  padding: 14px;
}

.section-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  margin-bottom: 12px;
}

.auto-voucher-form {
  max-width: 640px;
}

.amount-field {
  display: flex;
  align-items: center;
  min-height: 32px;
  gap: 8px;
  padding: 0 11px;
  border: 1px solid var(--el-border-color);
  border-radius: 4px;
  background: var(--el-disabled-bg-color);
  color: var(--el-text-color-primary);
}

.voucher-preview {
  display: grid;
  gap: 8px;
  margin: 12px 0 16px;
  padding: 12px;
  border: 1px solid #e5e7eb;
  border-radius: 6px;
  background: #f9fafb;
}

.preview-row {
  display: grid;
  grid-template-columns: 32px 1fr auto;
  gap: 10px;
  align-items: center;
  min-height: 30px;
}

.form-actions {
  display: flex;
  justify-content: flex-end;
  gap: 10px;
}

@media (max-width: 980px) {
  .accounting-layout {
    grid-template-columns: 1fr;
  }
}
</style>
