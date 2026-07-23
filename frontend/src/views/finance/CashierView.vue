<template>
  <div class="page">
    <div class="page-header">
      <div>
        <h1 class="page-title">出纳管理</h1>
        <p class="page-subtitle">登记收款、付款、转账和退款流水，确认后可进入会计平台制证。</p>
      </div>
    </div>

    <el-form class="filter-form" :model="filters" label-width="82px">
      <el-row :gutter="12">
        <el-col :xs="24" :sm="12" :md="8" :lg="6">
          <el-form-item label="交易日期">
            <el-date-picker v-model="filters.dateRange" type="daterange" value-format="YYYY-MM-DD" start-placeholder="开始日期" end-placeholder="结束日期" class="full" />
          </el-form-item>
        </el-col>
        <el-col :xs="24" :sm="12" :md="8" :lg="4">
          <el-form-item label="类型">
            <el-select v-model="filters.transactionType" clearable class="full" placeholder="全部">
              <el-option label="收款" value="RECEIPT" />
              <el-option label="付款" value="PAYMENT" />
              <el-option label="转账" value="TRANSFER" />
              <el-option label="退款" value="REFUND" />
            </el-select>
          </el-form-item>
        </el-col>
        <el-col :xs="24" :sm="12" :md="8" :lg="4">
          <el-form-item label="状态">
            <el-select v-model="filters.status" clearable class="full" placeholder="全部">
              <el-option label="草稿" value="DRAFT" />
              <el-option label="已确认" value="CONFIRMED" />
              <el-option label="已制证" value="VOUCHERED" />
              <el-option label="已取消" value="CANCELLED" />
            </el-select>
          </el-form-item>
        </el-col>
        <el-col :xs="24" :sm="12" :md="8" :lg="5">
          <el-form-item label="项目">
            <el-select v-model="filters.projectCode" clearable filterable class="full" placeholder="全部项目">
              <el-option v-for="item in projectOptions" :key="item.id" :label="item.name" :value="item.code" />
            </el-select>
          </el-form-item>
        </el-col>
        <el-col :xs="24" :sm="12" :md="8" :lg="5">
          <el-form-item label="往来单位">
            <el-select v-model="filters.partnerName" clearable filterable class="full" placeholder="全部往来单位">
              <el-option v-for="item in partnerOptions" :key="item.id" :label="item.name" :value="item.name" />
            </el-select>
          </el-form-item>
        </el-col>
        <el-col :xs="24" :sm="12" :md="8" :lg="5">
          <el-form-item label="关联单号">
            <el-input v-model="filters.relatedBizNo" clearable placeholder="模糊查询关联单号" />
          </el-form-item>
        </el-col>
        <el-col :xs="24" :sm="12" :md="8" :lg="8">
          <el-form-item label=" " class="filter-actions">
            <el-button type="primary" @click="load">查询</el-button>
            <el-button @click="resetFilters">重置</el-button>
            <el-button v-if="auth.hasMenu('BTN_CASHIER_CREATE')" type="primary" :icon="Plus" @click="openCreate">新增流水</el-button>
            <el-button v-if="auth.hasMenu('BTN_CASHIER_BATCH_DELETE') && selectedRows.length > 0" type="danger" :icon="Delete" @click="batchRemove">批量删除</el-button>
            <el-button v-if="auth.hasMenu('BTN_CASHIER_EXPORT')" :icon="Download" :loading="exporting" @click="exportRows">导出</el-button>
          </el-form-item>
        </el-col>
      </el-row>
    </el-form>

    <div class="panel">
      <el-table v-loading="loading" :data="rows" stripe @selection-change="handleSelectionChange">
        <el-table-column type="selection" width="48" />
        <el-table-column prop="transactionNo" label="流水号" min-width="160" />
        <el-table-column prop="transactionDate" label="交易日期" width="120" />
        <el-table-column label="类型" width="90">
          <template #default="{ row }">{{ transactionTypeLabel(row.transactionType) }}</template>
        </el-table-column>
        <el-table-column label="状态" width="100">
          <template #default="{ row }">
            <el-tag :type="statusTagType(row.status)">{{ cashierStatusLabel(row.status) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="projectName" label="项目" min-width="130" />
        <el-table-column prop="partnerName" label="往来单位" min-width="160" />
        <el-table-column prop="bankAccount" label="银行/现金账户" min-width="160" />
        <el-table-column prop="settlementMethod" label="结算方式" width="120" />
        <el-table-column label="金额" width="150" align="right">
          <template #default="{ row }"><AmountText :value="row.amount" :display="`${money(row.amount)} ${row.currencyCode}`" :currency-code="row.currencyCode" :currency-name="row.currencyName" /></template>
        </el-table-column>
        <el-table-column label="人民币金额" width="130" align="right">
          <template #default="{ row }"><AmountText :value="row.amountCny" :display="money(row.amountCny)" currency-code="CNY" currency-name="人民币" /></template>
        </el-table-column>
        <el-table-column prop="relatedBizNo" label="关联单号" min-width="150" />
        <el-table-column prop="summary" label="摘要" min-width="220" />
        <el-table-column prop="voucherNo" label="凭证号" min-width="140" />
        <el-table-column label="操作" width="260" fixed="right">
          <template #default="{ row }">
            <div class="table-actions">
              <el-button v-if="auth.hasMenu('BTN_CASHIER_CONFIRM') && row.status === 'DRAFT'" size="small" type="primary" @click="confirmRow(row)">确认</el-button>
              <el-button v-if="auth.hasMenu('BTN_CASHIER_CANCEL') && row.status !== 'VOUCHERED' && row.status !== 'CANCELLED'" size="small" @click="cancelRow(row)">取消</el-button>
              <el-button v-if="auth.hasMenu('BTN_VOUCHER_VIEW') && row.voucherId && row.voucherNo" size="small" @click="openOnlineVoucher(row.voucherNo)">在线凭证</el-button>
            </div>
          </template>
        </el-table-column>
      </el-table>
    </div>

    <el-dialog v-model="dialogVisible" title="新增出纳流水" width="min(1180px, 92vw)" top="5vh">
      <el-form :model="form" label-width="106px">
        <section class="business-form-section">
          <div class="section-heading"><span>基本信息</span></div>
          <el-row :gutter="12">
            <el-col :xs="24" :sm="8">
              <el-form-item label="交易日期" required>
                <el-date-picker v-model="form.transactionDate" value-format="YYYY-MM-DD" class="full" />
              </el-form-item>
            </el-col>
            <el-col :xs="24" :sm="8">
              <el-form-item label="类型" required>
                <el-select v-model="form.transactionType" class="full">
                  <el-option label="收款" value="RECEIPT" />
                  <el-option label="付款" value="PAYMENT" />
                  <el-option label="转账" value="TRANSFER" />
                  <el-option label="退款" value="REFUND" />
                </el-select>
              </el-form-item>
            </el-col>
            <el-col :xs="24" :sm="8">
              <el-form-item label="项目" required>
                <el-select v-model="form.projectCode" clearable filterable class="full" placeholder="请选择项目" @change="onProjectChange">
                  <el-option v-for="item in projectOptions" :key="item.id" :label="item.name" :value="item.code" />
                </el-select>
              </el-form-item>
            </el-col>
            <el-col :xs="24" :sm="8">
              <el-form-item label="往来单位">
                <el-select v-model="form.partnerName" clearable filterable class="full" placeholder="请选择往来单位">
                  <el-option v-for="item in partnerOptions" :key="item.id" :label="item.name" :value="item.name" />
                </el-select>
              </el-form-item>
            </el-col>
            <el-col :xs="24" :sm="8">
              <el-form-item label="关联单号">
                <el-input v-model="form.relatedBizNo" maxlength="300" show-word-limit />
              </el-form-item>
            </el-col>
            <el-col :xs="24" :sm="8">
              <el-form-item label="结算方式">
                <el-select v-model="form.settlementMethod" clearable filterable class="full">
                  <el-option v-for="item in settlementMethodOptions" :key="item.id" :label="item.name" :value="item.name" />
                </el-select>
              </el-form-item>
            </el-col>
          </el-row>
        </section>

        <section class="business-form-section">
          <div class="section-heading"><span>资金信息</span></div>
          <el-row :gutter="12">
            <el-col :xs="24" :sm="10">
              <el-form-item label="金额" required>
                <div class="money-input">
                  <el-input-number v-model="form.amount" :min="0.00000001" :precision="8" :controls="false" class="money-number" />
                  <el-select v-model="form.currencyCode" filterable class="money-currency" @change="onCurrencyChange">
                    <el-option v-for="item in currencyOptions" :key="item.code" :label="item.code" :value="item.code" />
                  </el-select>
                </div>
              </el-form-item>
            </el-col>
            <el-col :xs="24" :sm="7">
              <el-form-item label="汇率">
                <el-input-number v-model="form.exchangeRateToCny" :min="0.00000001" :precision="8" :controls="false" :disabled="form.currencyCode === 'CNY'" class="full" />
              </el-form-item>
            </el-col>
            <el-col :xs="24" :sm="7">
              <el-form-item label="银行账户">
                <el-select v-model="form.bankAccount" clearable filterable class="full" placeholder="请选择账户">
                  <el-option v-for="item in bankAccountOptions" :key="item.id" :label="item.name" :value="item.name" />
                </el-select>
              </el-form-item>
            </el-col>
            <el-col :xs="24">
              <el-form-item label="摘要" required>
                <el-input v-model="form.summary" maxlength="200" show-word-limit />
              </el-form-item>
            </el-col>
            <el-col :xs="24">
              <el-form-item label="备注">
                <el-input v-model="form.remark" type="textarea" :rows="3" maxlength="2000" show-word-limit />
              </el-form-item>
            </el-col>
          </el-row>
        </section>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button v-if="auth.hasMenu('BTN_CASHIER_CREATE')" type="primary" @click="save">保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { onActivated, onMounted, reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { Delete, Download, Plus } from '@element-plus/icons-vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { api } from '@/api/fm'
import { saveBlob } from '@/api/http'
import AmountText from '@/components/common/AmountText.vue'
import { useAuthStore } from '@/stores/auth'
import type { BasicDictionaryView, CashierTransactionStatus, CashierTransactionType, CashierTransactionView } from '@/types/api'

/**
 * 常量 auth：保存当前登录人员菜单权限，用于控制出纳按钮显示。
 */
const auth = useAuthStore()
/** 路由实例，用于从出纳流水跳转到凭证记账页面查看在线凭证。 */
const router = useRouter()

/**
 * 常量 rows：保存出纳流水表格数据。
 */
const rows = ref<CashierTransactionView[]>([])

/**
 * 常量 selectedRows：保存表格已勾选出纳流水。
 */
const selectedRows = ref<CashierTransactionView[]>([])

/**
 * 常量 loading：表示表格是否正在加载。
 */
const loading = ref(false)

/**
 * 常量 exporting：表示导出接口是否正在执行。
 */
const exporting = ref(false)

/**
 * 常量 dialogVisible：控制新增出纳流水弹窗。
 */
const dialogVisible = ref(false)

/**
 * 常量 projectOptions：保存项目字典选项。
 */
const projectOptions = ref<BasicDictionaryView[]>([])

/**
 * 常量 partnerOptions：保存客户/供应商字典选项。
 */
const partnerOptions = ref<BasicDictionaryView[]>([])

/**
 * 常量 bankAccountOptions：保存银行账户字典选项。
 */
const bankAccountOptions = ref<BasicDictionaryView[]>([])

/**
 * 常量 settlementMethodOptions：保存结算方式字典选项。
 */
const settlementMethodOptions = ref<BasicDictionaryView[]>([])

/**
 * 常量 currencyOptions：保存币种字典选项。
 */
const currencyOptions = ref<BasicDictionaryView[]>([])

/**
 * 常量 today：保存当前日期字符串，作为新建流水默认日期。
 */
const today = new Date().toISOString().slice(0, 10)

/**
 * 常量 filters：保存出纳流水筛选条件。
 */
const filters = reactive({
  dateRange: [] as string[],
  transactionType: undefined as CashierTransactionType | undefined,
  status: undefined as CashierTransactionStatus | undefined,
  projectCode: '',
  partnerName: '',
  relatedBizNo: ''
})

/**
 * 常量 form：保存新增出纳流水表单。
 */
const form = reactive({
  transactionDate: today,
  transactionType: 'RECEIPT' as CashierTransactionType,
  projectCode: '',
  projectName: '',
  partnerName: '',
  bankAccount: '',
  settlementMethod: '',
  amount: 1,
  currencyCode: 'CNY',
  currencyName: '人民币',
  exchangeRateToCny: 1,
  relatedBizNo: '',
  summary: '',
  remark: ''
})

/**
 * 加载出纳流水列表。
 *
 * 实现步骤：
 * 1. 将页面筛选条件转换为接口参数；
 * 2. 调用出纳列表接口；
 * 3. 将返回结果写入表格。
 */
async function load() {
  loading.value = true
  try {
    rows.value = await api.cashierTransactions(searchParams())
  } finally {
    loading.value = false
  }
}

/**
 * 加载下拉字典。
 *
 * 实现步骤：并行读取项目、往来单位、银行账户、结算方式和币种字典，保证新增弹窗可直接选择。
 */
async function loadDictionaries() {
  const [projects, partners, bankAccounts, settlementMethods, currencies] = await Promise.all([
    api.enabledDictionaryChildren('PROJECT'),
    api.enabledDictionaryChildren('PARTNER'),
    api.enabledDictionaryChildren('BANK_ACCOUNT'),
    api.enabledDictionaryChildren('SETTLEMENT_METHOD'),
    api.enabledDictionaryChildren('CURRENCY')
  ])
  projectOptions.value = projects
  partnerOptions.value = partners
  bankAccountOptions.value = bankAccounts
  settlementMethodOptions.value = settlementMethods
  currencyOptions.value = currencies.length ? currencies : [{
    id: -1,
    code: 'CNY',
    name: '人民币',
    sortOrder: 0,
    enabled: true,
    hasChildren: false,
    children: []
  }]
}

/**
 * 重置筛选条件。
 *
 * 实现步骤：清空全部筛选字段并重新加载列表。
 */
function resetFilters() {
  Object.assign(filters, {
    dateRange: [],
    transactionType: undefined,
    status: undefined,
    projectCode: '',
    partnerName: '',
    relatedBizNo: ''
  })
  void load()
}

/**
 * 打开新增弹窗。
 *
 * 实现步骤：重置表单为默认收款流水，再显示弹窗。
 */
async function openCreate() {
  await loadDictionaries()
  resetForm()
  dialogVisible.value = true
}

/**
 * 重置新增表单。
 *
 * 实现步骤：恢复默认日期、收款类型、人民币和空业务字段。
 */
function resetForm() {
  Object.assign(form, {
    transactionDate: today,
    transactionType: 'RECEIPT',
    projectCode: '',
    projectName: '',
    partnerName: '',
    bankAccount: '',
    settlementMethod: '',
    amount: 1,
    currencyCode: 'CNY',
    currencyName: '人民币',
    exchangeRateToCny: 1,
    relatedBizNo: '',
    summary: '',
    remark: ''
  })
}

/**
 * 保存出纳流水。
 *
 * 实现步骤：
 * 1. 校验摘要和金额；
 * 2. 同步币种名称；
 * 3. 调用新增接口；
 * 4. 关闭弹窗并刷新列表。
 */
async function save() {
  if (!form.projectCode) {
    ElMessage.warning('请选择项目')
    return
  }
  if (!form.summary.trim()) {
    ElMessage.warning('请输入摘要')
    return
  }
  if (Number(form.amount || 0) <= 0) {
    ElMessage.warning('金额必须大于0')
    return
  }
  await api.createCashierTransaction({ ...form, currencyName: selectedCurrencyName() })
  ElMessage.success('保存成功')
  dialogVisible.value = false
  await load()
}

/**
 * 确认出纳流水。
 *
 * 实现步骤：二次确认后调用确认接口，成功后刷新列表。
 */
async function confirmRow(row: CashierTransactionView) {
  await ElMessageBox.confirm(`确认出纳流水 ${row.transactionNo}？`, '确认出纳流水', { type: 'warning' })
  await api.confirmCashierTransaction(row.id)
  ElMessage.success('确认成功')
  await load()
}

/**
 * 取消出纳流水。
 *
 * 实现步骤：二次确认后调用取消接口，成功后刷新列表。
 */
async function cancelRow(row: CashierTransactionView) {
  await ElMessageBox.confirm(`确认取消出纳流水 ${row.transactionNo}？`, '取消出纳流水', { type: 'warning' })
  await api.cancelCashierTransaction(row.id)
  ElMessage.success('取消成功')
  await load()
}

/**
 * 批量删除出纳流水。
 *
 * 实现步骤：确认已勾选数据，二次确认后调用批量删除接口并刷新列表。
 */
async function batchRemove() {
  if (!selectedRows.value.length) {
    ElMessage.warning('请选择需要删除的出纳流水')
    return
  }
  await ElMessageBox.confirm(`确认删除选中的 ${selectedRows.value.length} 条出纳流水？`, '批量删除确认', { type: 'warning' })
  await api.batchDeleteCashierTransactions(selectedRows.value.map((row) => row.id))
  ElMessage.success('删除成功')
  await load()
}

/**
 * 从出纳流水跳转查看在线凭证。
 *
 * 实现步骤：
 * 1. 接收出纳流水已关联的凭证号；
 * 2. 跳转到凭证记账页面并按凭证号筛选；
 * 3. 通过 openImage=1 让凭证页面自动打开在线凭证弹窗。
 */
function openOnlineVoucher(voucherNo?: string) {
  if (!voucherNo) {
    return
  }
  void router.push({ path: '/vouchers', query: { voucherNo, openImage: '1' } })
}

/**
 * 导出出纳流水。
 *
 * 实现步骤：选中行优先导出，否则按当前搜索条件导出。
 */
async function exportRows() {
  exporting.value = true
  try {
    /** 出纳导出请求参数，选中行优先，否则使用当前搜索条件。 */
    const payload = selectedRows.value.length ? { ids: selectedRows.value.map((row) => row.id) } : searchParams()
    const { blob, filename } = await api.exportCashierTransactions(payload)
    saveBlob(blob, filename || '出纳流水.xlsx')
    ElMessage.success('导出成功')
  } finally {
    exporting.value = false
  }
}

/**
 * 记录表格选中行。
 *
 * 实现步骤：将 Element Plus 表格 selection 写入本地响应式变量。
 */
function handleSelectionChange(selection: CashierTransactionView[]) {
  selectedRows.value = selection
}

/**
 * 根据项目编码同步项目名称快照。
 *
 * 实现步骤：在项目字典中查找当前编码，保存名称快照供后端落库。
 */
function onProjectChange() {
  form.projectName = projectOptions.value.find((item) => item.code === form.projectCode)?.name || ''
}

/**
 * 切换币种。
 *
 * 实现步骤：
 * 1. 同步当前币种名称快照；
 * 2. 人民币固定汇率为 1；
 * 3. 非人民币调用后端参考汇率接口，成功后自动填入汇率输入框，失败时保留用户可手工填写。
 */
async function onCurrencyChange() {
  form.currencyName = selectedCurrencyName()
  if (form.currencyCode === 'CNY') {
    form.exchangeRateToCny = 1
    return
  }
  /** 当前选择的币种编码，用于避免异步返回覆盖用户后续切换结果。 */
  const selectedCode = form.currencyCode
  try {
    /** 后端返回的最新公开参考汇率，不代表秒级实时交易价。 */
    const rate = await api.exchangeRate(selectedCode)
    if (form.currencyCode !== selectedCode) {
      return
    }
    form.currencyName = rate.currencyName || selectedCurrencyName()
    form.exchangeRateToCny = Number(rate.exchangeRateToCny || form.exchangeRateToCny || 1)
    ElMessage.info(`已填入最新参考汇率${rate.rateDate ? `（${rate.rateDate}）` : ''}，来源：${rate.source || '公开汇率源'}，请按业务凭证核对`)
  } catch {
    ElMessage.warning('最新参考汇率获取失败，请手工填写汇率')
  }
}

/**
 * 读取当前币种名称。
 *
 * 实现步骤：按币种编码在字典中查找名称，找不到时回退编码本身。
 */
function selectedCurrencyName() {
  return currencyOptions.value.find((item) => item.code === form.currencyCode)?.name || form.currencyName || form.currencyCode
}

/**
 * 生成搜索参数。
 *
 * 实现步骤：把日期区间拆分为开始和结束日期，并清理空白字符串。
 */
function searchParams() {
  return {
    startDate: filters.dateRange?.[0],
    endDate: filters.dateRange?.[1],
    transactionType: filters.transactionType,
    status: filters.status,
    projectCode: filters.projectCode || undefined,
    partnerName: filters.partnerName || undefined,
    relatedBizNo: filters.relatedBizNo.trim() || undefined
  }
}

/**
 * 格式化金额。
 *
 * 实现步骤：空值按 0 处理，再按中文地区格式输出 2 到 8 位小数。
 */
function money(value: number) {
  return new Intl.NumberFormat('zh-CN', { minimumFractionDigits: 2, maximumFractionDigits: 8 }).format(Number(value || 0))
}

/**
 * 出纳流水类型中文化。
 *
 * 实现步骤：按后端枚举返回页面展示文本。
 */
function transactionTypeLabel(value: CashierTransactionType) {
  return {
    RECEIPT: '收款',
    PAYMENT: '付款',
    TRANSFER: '转账',
    REFUND: '退款'
  }[value] || value
}

/**
 * 出纳流水状态中文化。
 *
 * 实现步骤：按后端枚举返回页面展示文本。
 */
function cashierStatusLabel(value: CashierTransactionStatus) {
  return {
    DRAFT: '草稿',
    CONFIRMED: '已确认',
    VOUCHERED: '已制证',
    CANCELLED: '已取消'
  }[value] || value
}

/**
 * 出纳状态标签颜色。
 *
 * 实现步骤：根据状态风险和完成度选择 Element Plus 标签类型。
 */
function statusTagType(value: CashierTransactionStatus) {
  return {
    DRAFT: 'warning',
    CONFIRMED: 'success',
    VOUCHERED: 'info',
    CANCELLED: 'danger'
  }[value] as 'warning' | 'success' | 'info' | 'danger'
}

onMounted(async () => {
  await Promise.all([load(), loadDictionaries()])
})

onActivated(() => {
  void loadDictionaries()
})
</script>

<style scoped>
.full {
  width: 100%;
}

.filter-form {
  margin-bottom: 14px;
  padding: 14px 14px 0;
  border: 1px solid #e5e7eb;
  border-radius: 6px;
  background: #fff;
}

.business-form-section {
  margin-bottom: 14px;
  padding: 12px 14px 4px;
  border: 1px solid #dbe3ef;
  border-radius: 6px;
  background: #f8fafc;
}

.section-heading {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 12px;
  padding-bottom: 8px;
  border-bottom: 1px solid #e5e7eb;
  color: #111827;
  font-size: 14px;
  font-weight: 700;
}

.money-input {
  display: flex;
  gap: 6px;
  width: 100%;
}

.money-number {
  flex: 1 1 auto;
}

.money-currency {
  flex: 0 0 86px;
}
</style>
