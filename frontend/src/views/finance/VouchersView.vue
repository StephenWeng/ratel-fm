<template>
  <div class="page">
    <div class="page-header">
      <div>
        <h1 class="page-title">凭证记账</h1>
        <p class="page-subtitle">录入复式记账凭证，系统校验借贷平衡。</p>
      </div>
    </div>

    <el-form class="filter-form" :model="filters" label-width="82px">
      <el-row :gutter="12">
        <el-col :xs="24" :sm="12" :md="8" :lg="6">
          <el-form-item label="凭证日期">
            <el-date-picker v-model="filters.dateRange" type="daterange" value-format="YYYY-MM-DD" start-placeholder="开始日期" end-placeholder="结束日期" class="full" />
          </el-form-item>
        </el-col>
        <el-col :xs="24" :sm="12" :md="8" :lg="5">
          <el-form-item label="所属年月">
            <el-date-picker v-model="filters.belongMonth" type="month" value-format="YYYY-MM" placeholder="选择年月" class="full" />
          </el-form-item>
        </el-col>
        <el-col :xs="24" :sm="12" :md="8" :lg="5">
          <el-form-item label="凭证号">
            <el-input v-model="filters.voucherNo" clearable placeholder="模糊查询凭证号" />
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
          <el-form-item label="摘要">
            <el-input v-model="filters.summary" clearable placeholder="模糊查询摘要" />
          </el-form-item>
        </el-col>
        <el-col :xs="24" :sm="12" :md="8" :lg="5">
          <el-form-item label="来源单号">
            <el-input v-model="filters.sourceBizNo" clearable placeholder="模糊查询来源单号" />
          </el-form-item>
        </el-col>
        <el-col :xs="24" :sm="12" :md="8" :lg="4">
          <el-form-item label="状态">
            <el-select v-model="filters.status" clearable class="full" placeholder="全部">
              <el-option label="草稿" value="DRAFT" />
              <el-option label="已过账" value="POSTED" />
              <el-option label="已作废" value="VOIDED" />
            </el-select>
          </el-form-item>
        </el-col>
        <el-col :xs="24" :sm="12" :md="8" :lg="5">
          <el-form-item label="制单人">
            <el-input v-model="filters.createdBy" clearable placeholder="模糊查询制单人" />
          </el-form-item>
        </el-col>
        <el-col :xs="24" :sm="12" :md="8" :lg="6">
          <el-form-item label=" " class="filter-actions">
            <el-button v-if="auth.hasMenu('BTN_VOUCHER_QUERY')" type="primary" :icon="Search" @click="load">查询</el-button>
            <el-button @click="resetFilters">重置</el-button>
            <el-button v-if="auth.hasMenu('BTN_VOUCHER_CREATE')" type="primary" :icon="Plus" @click="openCreate">新增凭证</el-button>
            <el-button v-if="auth.hasMenu('BTN_VOUCHER_BATCH_DELETE') && selectedRows.length > 0" type="danger" :icon="Delete" @click="batchRemove">批量删除</el-button>
            <el-button v-if="auth.hasMenu('BTN_VOUCHER_VIEW') && selectedRows.length > 0" :icon="Download" :loading="batchGeneratingVoucherImages" @click="batchDownloadVoucherImages">批量生成凭证</el-button>
            <el-button v-if="auth.hasMenu('BTN_VOUCHER_EXPORT')" :icon="Download" :loading="exporting" @click="exportRows">导出</el-button>
          </el-form-item>
        </el-col>
      </el-row>
    </el-form>

    <div class="panel">
      <el-table v-loading="loading" :data="vouchers" stripe @selection-change="handleSelectionChange">
        <el-table-column type="selection" width="48" />
        <el-table-column prop="voucherNo" label="凭证号" min-width="150" />
        <el-table-column prop="voucherDate" label="日期" width="120" />
        <el-table-column prop="belongMonth" label="所属年月" width="110" />
        <el-table-column prop="projectName" label="项目" min-width="140" />
        <el-table-column prop="summary" label="摘要" min-width="220" />
        <el-table-column label="状态" width="100">
          <template #default="{ row }">
            <el-tag :type="statusType(row.status)">{{ statusLabel(row.status) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="totalDebit" label="借方合计" width="130" align="right">
          <template #default="{ row }">
            <AmountText v-if="row.currencyCode !== 'MULTI'" :value="row.totalDebit" :currency-code="row.currencyCode || 'CNY'" :currency-name="row.currencyName" />
            <span v-else>查看明细</span>
          </template>
        </el-table-column>
        <el-table-column prop="totalCredit" label="贷方合计" width="130" align="right">
          <template #default="{ row }">
            <AmountText v-if="row.currencyCode !== 'MULTI'" :value="row.totalCredit" :currency-code="row.currencyCode || 'CNY'" :currency-name="row.currencyName" />
            <span v-else>查看明细</span>
          </template>
        </el-table-column>
        <el-table-column label="币种/汇率" width="150">
          <template #default="{ row }">
            <div class="stacked-cell">
              <div class="stacked-cell__line"><span class="stacked-cell__label">币种：</span>{{ currencyDisplay(row) }}</div>
              <div class="stacked-cell__line"><span class="stacked-cell__label">汇率：</span>{{ rateDisplay(row) }}</div>
            </div>
          </template>
        </el-table-column>
        <el-table-column prop="totalDebitCny" label="借方人民币" width="130" align="right">
          <template #default="{ row }"><AmountText :value="row.totalDebitCny" currency-code="CNY" currency-name="人民币" /></template>
        </el-table-column>
        <el-table-column prop="createdBy" label="制单人" width="110" />
        <el-table-column label="操作" width="340" fixed="right">
          <template #default="{ row }">
            <div class="table-actions">
              <el-button v-if="auth.hasMenu('BTN_VOUCHER_VIEW')" size="small" @click="view(row)">查看</el-button>
              <el-button v-if="auth.hasMenu('BTN_VOUCHER_VIEW')" size="small" @click="openVoucherImage(row)">生成凭证</el-button>
              <el-button v-if="auth.hasMenu('BTN_VOUCHER_VIEW') && row.sourceType && row.sourceId" size="small" @click="openVoucherSource(row)">查看来源</el-button>
              <el-button v-if="auth.hasMenu('BTN_VOUCHER_EDIT') && row.status === 'DRAFT'" size="small" @click="openEdit(row)">编辑</el-button>
              <el-button v-if="auth.hasMenu('BTN_VOUCHER_POST') && row.status === 'DRAFT'" size="small" type="success" @click="post(row.id)">过账</el-button>
              <el-button v-if="auth.hasMenu('BTN_VOUCHER_VOID') && row.status !== 'VOIDED'" size="small" type="warning" @click="voidIt(row.id)">作废</el-button>
              <el-button v-if="auth.hasMenu('BTN_VOUCHER_VIEW')" size="small" @click="openOperationLogs(row)">查看流水</el-button>
            </div>
          </template>
        </el-table-column>
      </el-table>
    </div>

    <el-dialog v-model="dialogVisible" :title="dialogTitle" width="min(1560px, 96vw)" top="4vh" class="voucher-dialog">
      <el-form ref="formRef" :model="form" :rules="rules" label-width="86px" :disabled="readonly">
        <el-row :gutter="12">
          <el-col :xs="24" :sm="8">
            <el-form-item label="凭证日期" prop="voucherDate">
              <el-date-picker v-model="form.voucherDate" type="date" value-format="YYYY-MM-DD" class="full" />
            </el-form-item>
          </el-col>
          <el-col :xs="24" :sm="8">
            <el-form-item label="所属年月" prop="belongMonth">
              <el-date-picker v-model="form.belongMonth" type="month" value-format="YYYY-MM" class="full" />
            </el-form-item>
          </el-col>
          <el-col :xs="24" :sm="8">
            <el-form-item label="项目" prop="projectCode">
              <el-select v-model="form.projectCode" clearable filterable class="full" placeholder="请选择项目" @change="onProjectChange">
                <el-option v-for="item in projectOptions" :key="item.id" :label="item.name" :value="item.code" />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :xs="24" :sm="8">
            <el-form-item label="来源单号" prop="sourceBizNo">
              <el-input v-model="form.sourceBizNo" :maxlength="fieldLimits.sourceBillNo" show-word-limit />
            </el-form-item>
          </el-col>
          <el-col :xs="24" :sm="8">
            <el-form-item label="摘要" prop="summary">
              <el-input v-model="form.summary" :maxlength="fieldLimits.summary" show-word-limit />
            </el-form-item>
          </el-col>
        </el-row>

        <div class="line-toolbar">
          <strong>凭证分录</strong>
          <div class="line-toolbar-actions">
            <el-button v-if="!readonly && auth.hasAnyMenu(['BTN_VOUCHER_CREATE', 'BTN_VOUCHER_EDIT'])" size="small" :icon="Plus" @click="addLine">新增分录</el-button>
            <el-upload
              ref="voucherImportUploadRef"
              v-if="!readonly && auth.hasAnyMenu(['BTN_VOUCHER_CREATE', 'BTN_VOUCHER_EDIT'])"
              :auto-upload="false"
              :show-file-list="false"
              multiple
              accept=".jpg,.jpeg,.png,.webp,.bmp,.pdf,image/*,application/pdf"
              :on-change="handleVoucherImport"
            >
              <el-button size="small" :icon="Upload" :loading="voucherImporting">凭证导入</el-button>
            </el-upload>
          </div>
        </div>
        <div class="voucher-lines-table">
          <el-table :data="form.lines" border>
            <el-table-column label="科目" min-width="230">
              <template #default="{ row }">
                <el-cascader
                  v-model="row.subjectId"
                  :options="subjectCascaderOptions"
                  :props="subjectCascaderProps"
                  :disabled="readonly"
                  filterable
                  clearable
                  class="full"
                  placeholder="请选择会计科目"
                  separator=" / "
                />
              </template>
            </el-table-column>
            <el-table-column label="摘要" min-width="190">
              <template #default="{ row, $index }">
                <el-input
                  v-model="row.summary"
                  :class="{ 'line-input-error': lineSummaryTouched[$index] && lineSummaryError(row) }"
                  :maxlength="fieldLimits.summary"
                  :disabled="readonly"
                  show-word-limit
                  @blur="touchLineSummary($index)"
                />
                <div v-if="lineSummaryTouched[$index] && lineSummaryError(row)" class="line-field-error">{{ lineSummaryError(row) }}</div>
              </template>
            </el-table-column>
            <el-table-column label="借方金额" width="145">
              <template #default="{ row }">
                <el-input-number v-model="row.debitAmount" :min="0" :precision="8" :controls="false" :disabled="readonly" class="full" />
              </template>
            </el-table-column>
            <el-table-column label="贷方金额" width="145">
              <template #default="{ row }">
                <el-input-number v-model="row.creditAmount" :min="0" :precision="8" :controls="false" :disabled="readonly" class="full" />
              </template>
            </el-table-column>
            <el-table-column label="币种" width="115">
              <template #default="{ row }">
                <el-select v-model="row.currencyCode" filterable class="full" :disabled="readonly" @change="onLineCurrencyChange(row)">
                  <el-option v-for="item in currencyOptions" :key="item.code" :label="item.code" :value="item.code" />
                </el-select>
              </template>
            </el-table-column>
            <el-table-column label="汇率" width="145">
              <template #default="{ row }">
                <el-input-number v-model="row.exchangeRateToCny" :min="0.00000001" :precision="8" :controls="false" :disabled="readonly || row.currencyCode === 'CNY'" class="full" />
              </template>
            </el-table-column>
            <el-table-column label="金额(人民币)" width="140" align="right">
              <template #default="{ row }">
                <AmountText :value="lineCnyAmount(row)" currency-code="CNY" currency-name="人民币" />
              </template>
            </el-table-column>
            <el-table-column label="辅助核算" min-width="160">
              <template #default="{ row }">
                <el-input v-model="row.auxiliary" :disabled="readonly" />
              </template>
            </el-table-column>
            <el-table-column label="操作" width="90">
              <template #default="{ $index }">
                <el-button v-if="canDeleteLine && form.lines.length > 2" size="small" type="danger" @click="form.lines.splice($index, 1)">删除</el-button>
              </template>
            </el-table-column>
          </el-table>
        </div>

        <div class="total-bar">
          <span>借方人民币：<AmountText :value="totalDebitCny" currency-code="CNY" currency-name="人民币" /></span>
          <span>贷方人民币：<AmountText :value="totalCreditCny" currency-code="CNY" currency-name="人民币" /></span>
          <el-tag :type="balanced ? 'success' : 'danger'">{{ balanced ? '借贷平衡' : '借贷不平衡' }}</el-tag>
        </div>
      </el-form>
      <AttachmentList
        ref="attachmentRef"
        business-type="VOUCHER"
        :business-id="editingId"
        :editable="canManageAttachment"
      />
      <template #footer>
        <el-button @click="dialogVisible = false">关闭</el-button>
        <el-button v-if="canSaveVoucher" type="primary" @click="save">保存</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="voucherImageVisible" title="在线凭证" width="1180px" top="4vh" @opened="renderVoucherImage">
      <div class="voucher-image-toolbar">
        <span>{{ voucherImageTitle }}</span>
        <el-button type="primary" :icon="Download" @click="downloadVoucherImage">下载图片</el-button>
      </div>
      <div class="voucher-image-preview">
        <canvas ref="voucherCanvasRef" width="1120" height="820"></canvas>
      </div>
    </el-dialog>

    <el-dialog v-model="voucherSourceVisible" title="查看来源" width="720px" top="8vh">
      <div v-loading="voucherSourceLoading" class="voucher-source-dialog">
        <div v-if="voucherSourceDetail" class="voucher-source-header">
          <div>
            <div class="voucher-source-title">{{ voucherSourceDetail.sourceTitle }}</div>
            <div class="voucher-source-subtitle">{{ voucherSourceDetail.sourceModule }} / {{ voucherSourceDetail.sourceNo }}</div>
          </div>
        </div>
        <el-descriptions v-if="voucherSourceDetail" :column="2" border>
          <el-descriptions-item v-for="field in voucherSourceDetail.fields" :key="field.label" :label="field.label">
            {{ field.value || '-' }}
          </el-descriptions-item>
        </el-descriptions>
        <el-empty v-else description="暂无来源信息" />
      </div>
      <template #footer>
        <el-button @click="voucherSourceVisible = false">关闭</el-button>
      </template>
    </el-dialog>

    <OperationLogDrawer ref="operationLogDrawerRef" />
  </div>
</template>

<script setup lang="ts">
import { computed, onActivated, onMounted, reactive, ref, watch } from 'vue'
import { useRoute } from 'vue-router'
import JSZip from 'jszip'
import { ElMessage, ElMessageBox } from 'element-plus'
import type { FormInstance, FormRules, UploadFile, UploadInstance } from 'element-plus'
import { Delete, Download, Plus, Search, Upload } from '@element-plus/icons-vue'
import AttachmentList from '@/components/attachments/AttachmentList.vue'
import AmountText from '@/components/common/AmountText.vue'
import OperationLogDrawer from '@/components/operation-log/OperationLogDrawer.vue'
import { api } from '@/api/fm'
import { saveBlob } from '@/api/http'
import { useAuthStore } from '@/stores/auth'
import type { BasicDictionaryView, SubjectView, VoucherImportLine, VoucherImportResult, VoucherSourceDetail, VoucherStatus, VoucherView } from '@/types/api'
import { buildSubjectCascaderOptions, subjectCascaderProps, subjectNamePath } from '@/utils/subjects'
import { fieldLimits } from '@/utils/validators'
import { fallbackExchangeRateToCny, formatMoney, roundBusinessMoney } from '@/utils/money'
import { queryString } from '@/utils/routeQuery'

/**
 * VoucherLineForm 类型定义，用于约束页面状态、接口入参或接口返回数据结构。
 */
interface VoucherLineForm {
  /**
   * 字段 subjectId：表示表单、筛选条件、接口数据或组件状态中的 subjectId 值。
   */
  subjectId?: number
  /**
   * 字段 summary：表示表单、筛选条件、接口数据或组件状态中的 summary 值。
   */
  summary: string
  /**
   * 字段 debitAmount：表示表单、筛选条件、接口数据或组件状态中的 debitAmount 值。
   */
  debitAmount: number
  /**
   * 字段 creditAmount：表示表单、筛选条件、接口数据或组件状态中的 creditAmount 值。
   */
  creditAmount: number
  /**
   * 字段 currencyCode：表示表单、筛选条件、接口数据或组件状态中的 currencyCode 值。
   */
  currencyCode: string
  /**
   * 字段 currencyName：表示表单、筛选条件、接口数据或组件状态中的 currencyName 值。
   */
  currencyName: string
  /**
   * 字段 exchangeRateToCny：表示表单、筛选条件、接口数据或组件状态中的 exchangeRateToCny 值。
   */
  exchangeRateToCny: number
  /**
   * 字段 auxiliary：表示表单、筛选条件、接口数据或组件状态中的 auxiliary 值。
   */
  auxiliary: string
}

/**
 * 常量 loading：保存当前模块的页面状态、配置项、接口实例或计算结果。
 */
const loading = ref(false)
/**
 * 常量 exporting：保存当前模块的页面状态、配置项、接口实例或计算结果。
 */
const exporting = ref(false)
/**
 * 常量 batchGeneratingVoucherImages：标记批量在线凭证图片压缩包是否正在生成，防止用户重复点击。
 */
const batchGeneratingVoucherImages = ref(false)
/**
 * 常量 voucherImporting：标记凭证图片/PDF是否正在AI识别，避免重复上传导致分录重复追加。
 */
const voucherImporting = ref(false)
/**
 * 常量 auth：保存当前模块的页面状态、配置项、接口实例或计算结果。
 */
const auth = useAuthStore()
/**
 * 常量 route：保存当前模块的页面状态、配置项、接口实例或计算结果。
 */
const route = useRoute()
/**
 * 常量 vouchers：保存当前模块的页面状态、配置项、接口实例或计算结果。
 */
const vouchers = ref<VoucherView[]>([])
/**
 * 常量 selectedRows：保存当前模块的页面状态、配置项、接口实例或计算结果。
 */
const selectedRows = ref<VoucherView[]>([])
/**
 * 常量 subjects：保存完整启用科目列表，用于构建凭证科目树型级联层级。
 */
const subjects = ref<SubjectView[]>([])
/**
 * 常量 enabledSubjects：保存后端判定可用于记账的启用叶子科目。
 */
const enabledSubjects = ref<SubjectView[]>([])
/**
 * 常量 currencyOptions：保存当前模块的页面状态、配置项、接口实例或计算结果。
 */
const currencyOptions = ref<BasicDictionaryView[]>([])
/**
 * 常量 projectOptions：保存项目字典下拉选项，用于凭证项目维度筛选和录入。
 */
const projectOptions = ref<BasicDictionaryView[]>([])
/**
 * 常量 attachmentRef：保存当前模块的页面状态、配置项、接口实例或计算结果。
 */
const attachmentRef = ref<InstanceType<typeof AttachmentList>>()
/**
 * 常量 voucherImportUploadRef：保存凭证导入上传组件实例，用于识别完成后清空内部文件列表。
 */
const voucherImportUploadRef = ref<UploadInstance>()
/**
 * 常量 formRef：指向凭证新增/编辑表单实例，用于主表字段级校验和红框提示。
 */
const formRef = ref<FormInstance>()
/**
 * 常量 operationLogDrawerRef：保存当前模块的页面状态、配置项、接口实例或计算结果。
 */
const operationLogDrawerRef = ref<InstanceType<typeof OperationLogDrawer>>()
/**
 * 常量 dialogVisible：保存当前模块的页面状态、配置项、接口实例或计算结果。
 */
const dialogVisible = ref(false)
/** 在线凭证图片预览弹窗显示状态。 */
const voucherImageVisible = ref(false)
/** 在线凭证画布实例，用于绘制和下载 PNG。 */
const voucherCanvasRef = ref<HTMLCanvasElement>()
/** 当前正在预览的凭证详情，包含分录明细。 */
const voucherImageDetail = ref<VoucherView>()
/** 查看来源弹窗显示状态。 */
const voucherSourceVisible = ref(false)
/** 查看来源弹窗加载状态。 */
const voucherSourceLoading = ref(false)
/** 当前凭证反向关联的来源业务详情。 */
const voucherSourceDetail = ref<VoucherSourceDetail>()
/** 路由携带 openImage=1 时记录自动打开请求，列表加载完成后只消费一次。 */
const pendingOpenImage = ref(false)
/**
 * 常量 editingId：保存当前模块的页面状态、配置项、接口实例或计算结果。
 */
const editingId = ref<number>()
/**
 * 常量 readonly：保存当前模块的页面状态、配置项、接口实例或计算结果。
 */
const readonly = ref(false)
/**
 * 常量 filters：保存当前模块的页面状态、配置项、接口实例或计算结果。
 */
const filters = reactive({
  /**
   * 字段 dateRange：表示表单、筛选条件、接口数据或组件状态中的 dateRange 值。
   */
  dateRange: [] as string[],
  /**
   * 字段 belongMonth：表示表单、筛选条件、接口数据或组件状态中的 belongMonth 值。
   */
  belongMonth: '',
  /**
   * 字段 voucherNo：表示表单、筛选条件、接口数据或组件状态中的 voucherNo 值。
   */
  voucherNo: '',
  /**
   * 字段 projectCode：表示项目字典编码，用于按项目筛选凭证。
   */
  projectCode: '',
  /**
   * 字段 summary：表示表单、筛选条件、接口数据或组件状态中的 summary 值。
   */
  summary: '',
  /**
   * 字段 sourceBizNo：表示表单、筛选条件、接口数据或组件状态中的 sourceBizNo 值。
   */
  sourceBizNo: '',
  /**
   * 字段 status：表示表单、筛选条件、接口数据或组件状态中的 status 值。
   */
  status: undefined as VoucherStatus | undefined,
  /**
   * 字段 createdBy：表示表单、筛选条件、接口数据或组件状态中的 createdBy 值。
   */
  createdBy: ''
})
/**
 * 常量 form：保存当前模块的页面状态、配置项、接口实例或计算结果。
 */
const form = reactive({
  /**
   * 字段 voucherDate：表示表单、筛选条件、接口数据或组件状态中的 voucherDate 值。
   */
  voucherDate: new Date().toISOString().slice(0, 10),
  /**
   * 字段 belongMonth：表示表单、筛选条件、接口数据或组件状态中的 belongMonth 值。
   */
  belongMonth: new Date().toISOString().slice(0, 7),
  /**
   * 字段 projectCode：表示项目字典编码，保存凭证所属项目。
   */
  projectCode: '',
  /**
   * 字段 projectName：表示项目名称快照，保存凭证创建或修改时的项目名称。
   */
  projectName: '',
  /**
   * 字段 summary：表示表单、筛选条件、接口数据或组件状态中的 summary 值。
   */
  summary: '',
  /**
   * 字段 sourceBizNo：表示表单、筛选条件、接口数据或组件状态中的 sourceBizNo 值。
   */
  sourceBizNo: '',
  /**
   * 字段 lines：表示表单、筛选条件、接口数据或组件状态中的 lines 值。
   */
  lines: [] as VoucherLineForm[]
})

/**
 * 常量 lineSummaryTouched：记录每一行分录摘要是否已经 blur，避免未编辑时提前显示行内错误。
 */
const lineSummaryTouched = ref<boolean[]>([])
/**
 * 常量 voucherImportQueue：暂存本次选择的凭证导入文件，等待短延迟合并后一次性识别。
 */
const voucherImportQueue = ref<File[]>([])
/**
 * 常量 voucherImportTimer：保存导入合并定时器，避免多文件选择时逐个调用识别接口。
 */
let voucherImportTimer: ReturnType<typeof setTimeout> | undefined

/**
 * 凭证主表字段校验规则。
 *
 * 实现步骤：
 * 1. 凭证日期、所属年月和摘要作为必填项做字段级提示；
 * 2. 来源单号和摘要按统一长度限制校验；
 * 3. 输入框 blur 后由 Element Plus 显示红框和字段下方错误文案。
 */
const rules: FormRules = {
  voucherDate: [{ required: true, message: '请选择凭证日期', trigger: 'change' }],
  belongMonth: [{ required: true, message: '请选择所属年月', trigger: 'change' }],
  projectCode: [{ required: true, message: '请选择项目', trigger: 'change' }],
  summary: [
    { required: true, message: '请输入摘要', trigger: 'blur' },
    { max: fieldLimits.summary, message: `摘要不能超过${fieldLimits.summary}个字符`, trigger: 'blur' }
  ],
  sourceBizNo: [{ max: fieldLimits.sourceBillNo, message: `来源单号不能超过${fieldLimits.sourceBillNo}个字符`, trigger: 'blur' }]
}

/**
 * 常量 subjectCascaderOptions：保存只展示科目名称的树型级联选项。
 */
const subjectCascaderOptions = computed(() => buildSubjectCascaderOptions(subjects.value, enabledSubjects.value))
/**
 * 常量 totalDebitCny：保存当前模块的页面状态、配置项、接口实例或计算结果。
 */
const totalDebitCny = computed(() => roundBusinessMoney(form.lines.reduce((sum, line) => sum + toCny(line.debitAmount, line), 0)))
/**
 * 常量 totalCreditCny：保存当前模块的页面状态、配置项、接口实例或计算结果。
 */
const totalCreditCny = computed(() => roundBusinessMoney(form.lines.reduce((sum, line) => sum + toCny(line.creditAmount, line), 0)))
/**
 * 常量 balanced：保存当前模块的页面状态、配置项、接口实例或计算结果。
 */
const balanced = computed(() => totalDebitCny.value > 0 && Math.abs(totalDebitCny.value - totalCreditCny.value) < 0.000000005)
/**
 * 常量 dialogTitle：保存当前模块的页面状态、配置项、接口实例或计算结果。
 */
const dialogTitle = computed(() => (readonly.value ? '凭证明细' : editingId.value ? '编辑凭证' : '新增凭证'))
/**
 * 常量 canSaveVoucher：保存当前模块的页面状态、配置项、接口实例或计算结果。
 */
const canSaveVoucher = computed(() => !readonly.value && (editingId.value ? auth.hasMenu('BTN_VOUCHER_EDIT') : auth.hasMenu('BTN_VOUCHER_CREATE')))
/**
 * 常量 canDeleteLine：保存当前模块的页面状态、配置项、接口实例或计算结果。
 */
const canDeleteLine = computed(() => !readonly.value && auth.hasMenu('BTN_VOUCHER_LINE_DELETE'))
/**
 * 常量 canManageAttachment：保存当前模块的页面状态、配置项、接口实例或计算结果。
 */
const canManageAttachment = computed(() => !readonly.value && auth.hasMenu('BTN_VOUCHER_ATTACHMENT'))
/** 在线凭证预览标题，优先显示凭证号。 */
const voucherImageTitle = computed(() => voucherImageDetail.value ? `${voucherImageDetail.value.voucherNo} 在线凭证` : '在线凭证')

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
 * 执行 currencyDisplay 方法。
 * 
 * 实现步骤：
 * 1. 读取当前页面状态或调用参数；
 * 2. 完成对应的校验、接口调用或数据转换；
 * 3. 更新页面状态或返回处理结果。
 */
function currencyDisplay(row: VoucherView) {
  return row.currencyCode === 'MULTI' ? row.currencyName : row.currencyCode
}

/**
 * 执行 rateDisplay 方法。
 * 
 * 实现步骤：
 * 1. 读取当前页面状态或调用参数；
 * 2. 完成对应的校验、接口调用或数据转换；
 * 3. 更新页面状态或返回处理结果。
 */
function rateDisplay(row: VoucherView) {
  return row.currencyCode === 'MULTI' ? row.currencyName : money(row.exchangeRateToCny)
}

/**
 * 执行 roundMoney 方法。
 * 
 * 实现步骤：
 * 1. 读取当前页面状态或调用参数；
 * 2. 完成对应的校验、接口调用或数据转换；
 * 3. 更新页面状态或返回处理结果。
 */

/**
 * 执行 selectedCurrencyName 方法。
 * 
 * 实现步骤：
 * 1. 读取当前页面状态或调用参数；
 * 2. 完成对应的校验、接口调用或数据转换；
 * 3. 更新页面状态或返回处理结果。
 */
function selectedCurrencyName(currencyCode: string) {
  return currencyOptions.value.find((item) => item.code === currencyCode)?.name || (currencyCode === 'CNY' ? '人民币' : currencyCode)
}

/**
 * 读取币种切换时的本地兜底汇率。
 *
 * 实现步骤：
 * 1. 人民币直接返回 1；
 * 2. 外币保留现有大于 0 的汇率，避免网络查询前清空用户手工输入；
 * 3. 没有有效旧值时先返回 1，后续异步接口成功后会覆盖为参考汇率。
 */

/**
 * 执行 toCny 方法。
 * 
 * 实现步骤：
 * 1. 读取当前页面状态或调用参数；
 * 2. 完成对应的校验、接口调用或数据转换；
 * 3. 更新页面状态或返回处理结果。
 */
function toCny(amount: number, line: VoucherLineForm) {
  return roundBusinessMoney(Number(amount || 0) * Number(line.exchangeRateToCny || 1))
}

/**
 * 执行 lineCnyAmount 方法。
 * 
 * 实现步骤：
 * 1. 读取当前页面状态或调用参数；
 * 2. 完成对应的校验、接口调用或数据转换；
 * 3. 更新页面状态或返回处理结果。
 */
function lineCnyAmount(line: VoucherLineForm) {
  return roundBusinessMoney(toCny(line.debitAmount, line) + toCny(line.creditAmount, line))
}

/**
 * 执行 onLineCurrencyChange 方法。
 * 
 * 实现步骤：
 * 1. 读取当前页面状态或调用参数；
 * 2. 完成对应的校验、接口调用或数据转换；
 * 3. 更新页面状态或返回处理结果。
 */
async function onLineCurrencyChange(line: VoucherLineForm) {
  line.currencyName = selectedCurrencyName(line.currencyCode)
  line.exchangeRateToCny = fallbackExchangeRateToCny(line.currencyCode, line.exchangeRateToCny)
  if (line.currencyCode === 'CNY') {
    return
  }
  /**
   * 常量 selectedCode：保存当前模块的页面状态、配置项、接口实例或计算结果。
   */
  const selectedCode = line.currencyCode
  try {
    /**
     * 常量 rate：保存当前模块的页面状态、配置项、接口实例或计算结果。
     */
    const rate = await api.exchangeRate(selectedCode)
    if (line.currencyCode !== selectedCode) {
      return
    }
    line.currencyName = rate.currencyName || selectedCurrencyName(selectedCode)
    line.exchangeRateToCny = Number(rate.exchangeRateToCny || line.exchangeRateToCny || 1)
    ElMessage.info(`已填入最新参考汇率${rate.rateDate ? `（${rate.rateDate}）` : ''}，来源：${rate.source || '公开汇率源'}，请按业务凭证核对`)
  } catch {
    ElMessage.warning('最新参考汇率获取失败，请手工填写汇率')
  }
}

watch(
  () => form.voucherDate,
  (voucherDate, oldDate) => {
    /**
     * 常量 oldMonth：保存当前模块的页面状态、配置项、接口实例或计算结果。
     */
    const oldMonth = oldDate ? oldDate.slice(0, 7) : ''
    if (voucherDate && (!form.belongMonth || form.belongMonth === oldMonth)) {
      form.belongMonth = voucherDate.slice(0, 7)
    }
  }
)

/**
 * 执行 statusLabel 方法。
 * 
 * 实现步骤：
 * 1. 读取当前页面状态或调用参数；
 * 2. 完成对应的校验、接口调用或数据转换；
 * 3. 更新页面状态或返回处理结果。
 */
function statusLabel(value: VoucherStatus) {
  return { DRAFT: '草稿', POSTED: '已过账', VOIDED: '已作废' }[value]
}

/**
 * 执行 statusType 方法。
 * 
 * 实现步骤：
 * 1. 读取当前页面状态或调用参数；
 * 2. 完成对应的校验、接口调用或数据转换；
 * 3. 更新页面状态或返回处理结果。
 */
function statusType(value: VoucherStatus) {
  return value === 'POSTED' ? 'success' : value === 'VOIDED' ? 'info' : 'warning'
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
    vouchers.value = await api.vouchers({
      /**
       * 字段 startDate：表示表单、筛选条件、接口数据或组件状态中的 startDate 值。
       */
      startDate: filters.dateRange?.[0],
      /**
       * 字段 endDate：表示表单、筛选条件、接口数据或组件状态中的 endDate 值。
       */
      endDate: filters.dateRange?.[1],
      /**
       * 字段 belongMonth：表示表单、筛选条件、接口数据或组件状态中的 belongMonth 值。
       */
      belongMonth: filters.belongMonth || undefined,
      /**
       * 字段 voucherNo：表示表单、筛选条件、接口数据或组件状态中的 voucherNo 值。
       */
      voucherNo: filters.voucherNo.trim() || undefined,
      /**
       * 字段 projectCode：表示项目字典编码，用于按项目筛选凭证。
       */
      projectCode: filters.projectCode || undefined,
      /**
       * 字段 summary：表示表单、筛选条件、接口数据或组件状态中的 summary 值。
       */
      summary: filters.summary.trim() || undefined,
      /**
       * 字段 sourceBizNo：表示表单、筛选条件、接口数据或组件状态中的 sourceBizNo 值。
       */
      sourceBizNo: filters.sourceBizNo.trim() || undefined,
      /**
       * 字段 status：表示表单、筛选条件、接口数据或组件状态中的 status 值。
       */
      status: filters.status,
      /**
       * 字段 createdBy：表示表单、筛选条件、接口数据或组件状态中的 createdBy 值。
       */
      createdBy: filters.createdBy.trim() || undefined
    })
    selectedRows.value = []
    await maybeOpenRouteVoucherImage()
  } finally {
    loading.value = false
  }
}

/**
 * 执行 voucherSearchParams 方法。
 * 
 * 实现步骤：
 * 1. 读取当前页面状态或调用参数；
 * 2. 完成对应的校验、接口调用或数据转换；
 * 3. 更新页面状态或返回处理结果。
 */
function voucherSearchParams() {
  return {
    /**
     * 字段 startDate：表示表单、筛选条件、接口数据或组件状态中的 startDate 值。
     */
    startDate: filters.dateRange?.[0],
    /**
     * 字段 endDate：表示表单、筛选条件、接口数据或组件状态中的 endDate 值。
     */
    endDate: filters.dateRange?.[1],
    /**
     * 字段 belongMonth：表示表单、筛选条件、接口数据或组件状态中的 belongMonth 值。
     */
    belongMonth: filters.belongMonth || undefined,
    /**
     * 字段 voucherNo：表示表单、筛选条件、接口数据或组件状态中的 voucherNo 值。
     */
    voucherNo: filters.voucherNo.trim() || undefined,
    /**
     * 字段 projectCode：表示项目字典编码，用于按项目筛选凭证。
     */
    projectCode: filters.projectCode || undefined,
    /**
     * 字段 summary：表示表单、筛选条件、接口数据或组件状态中的 summary 值。
     */
    summary: filters.summary.trim() || undefined,
    /**
     * 字段 sourceBizNo：表示表单、筛选条件、接口数据或组件状态中的 sourceBizNo 值。
     */
    sourceBizNo: filters.sourceBizNo.trim() || undefined,
    /**
     * 字段 status：表示表单、筛选条件、接口数据或组件状态中的 status 值。
     */
    status: filters.status,
    /**
     * 字段 createdBy：表示表单、筛选条件、接口数据或组件状态中的 createdBy 值。
     */
    createdBy: filters.createdBy.trim() || undefined
  }
}

/**
 * 执行 handleSelectionChange 方法。
 * 
 * 实现步骤：
 * 1. 读取当前页面状态或调用参数；
 * 2. 完成对应的校验、接口调用或数据转换；
 * 3. 更新页面状态或返回处理结果。
 */
function handleSelectionChange(selection: VoucherView[]) {
  selectedRows.value = selection
}

/**
 * 导出凭证列表。
 *
 * 实现步骤：
 * 1. 如果表格已有勾选行，则只把勾选行 ID 传给后端导出；
 * 2. 如果没有勾选行，则把当前搜索条件传给后端导出；
 * 3. 后端生成 Excel 后，前端按响应文件名保存到本地。
 */
async function exportRows() {
  exporting.value = true
  try {
    /**
     * 常量 payload：保存当前模块的页面状态、配置项、接口实例或计算结果。
     */
    const payload = selectedRows.value.length > 0
      ? { ids: selectedRows.value.map((row) => row.id) }
      : voucherSearchParams()
    const { blob, filename } = await api.exportVouchers(payload)
    saveBlob(blob, filename || '凭证记账.xlsx')
    ElMessage.success('导出成功')
  } finally {
    exporting.value = false
  }
}

/**
 * 执行 resetFilters 方法。
 * 
 * 实现步骤：
 * 1. 读取当前页面状态或调用参数；
 * 2. 完成对应的校验、接口调用或数据转换；
 * 3. 更新页面状态或返回处理结果。
 */
function resetFilters() {
  Object.assign(filters, {
    /**
     * 字段 dateRange：表示表单、筛选条件、接口数据或组件状态中的 dateRange 值。
     */
    dateRange: [],
    /**
     * 字段 belongMonth：表示表单、筛选条件、接口数据或组件状态中的 belongMonth 值。
     */
    belongMonth: '',
    /**
     * 字段 voucherNo：表示表单、筛选条件、接口数据或组件状态中的 voucherNo 值。
     */
    voucherNo: '',
    /**
     * 字段 projectCode：表示项目字典编码，用于清空项目筛选条件。
     */
    projectCode: '',
    /**
     * 字段 summary：表示表单、筛选条件、接口数据或组件状态中的 summary 值。
     */
    summary: '',
    /**
     * 字段 sourceBizNo：表示表单、筛选条件、接口数据或组件状态中的 sourceBizNo 值。
     */
    sourceBizNo: '',
    /**
     * 字段 status：表示表单、筛选条件、接口数据或组件状态中的 status 值。
     */
    status: undefined,
    /**
     * 字段 createdBy：表示表单、筛选条件、接口数据或组件状态中的 createdBy 值。
     */
    createdBy: ''
  })
  void load()
}

/**
 * 执行 loadSubjects 方法。
 * 
 * 实现步骤：
 * 1. 读取当前页面状态或调用参数；
 * 2. 完成对应的校验、接口调用或数据转换；
 * 3. 更新页面状态或返回处理结果。
 */
async function loadSubjects() {
  const [allRows, businessRows] = await Promise.all([
    api.subjects(false, { enabled: true }),
    api.subjects(true)
  ])
  subjects.value = allRows
  enabledSubjects.value = businessRows
}

/**
 * 执行 loadCurrencies 方法。
 * 
 * 实现步骤：
 * 1. 读取当前页面状态或调用参数；
 * 2. 完成对应的校验、接口调用或数据转换；
 * 3. 更新页面状态或返回处理结果。
 */
async function loadCurrencies() {
  currencyOptions.value = await api.enabledDictionaryChildren('CURRENCY')
  if (!currencyOptions.value.some((item) => item.code === 'CNY')) {
    currencyOptions.value.unshift({
      /**
       * 字段 id：表示表单、筛选条件、接口数据或组件状态中的 id 值。
       */
      id: 0,
      /**
       * 字段 code：表示表单、筛选条件、接口数据或组件状态中的 code 值。
       */
      code: 'CNY',
      /**
       * 字段 name：表示表单、筛选条件、接口数据或组件状态中的 name 值。
       */
      name: '人民币',
      /**
       * 字段 sortOrder：表示表单、筛选条件、接口数据或组件状态中的 sortOrder 值。
       */
      sortOrder: 0,
      /**
       * 字段 enabled：表示表单、筛选条件、接口数据或组件状态中的 enabled 值。
       */
      enabled: true,
      /**
       * 字段 hasChildren：表示表单、筛选条件、接口数据或组件状态中的 hasChildren 值。
       */
      hasChildren: false,
      /**
       * 字段 children：表示表单、筛选条件、接口数据或组件状态中的 children 值。
       */
      children: []
    })
  }
}

/**
 * 加载项目字典选项。
 *
 * 实现步骤：
 * 1. 调用基础字典接口读取 PROJECT 下级字典；
 * 2. 保存到项目下拉选项；
 * 3. 后续查询、表单保存和查看流水均使用同一份项目编码与名称快照。
 */
async function loadProjects() {
  projectOptions.value = await api.enabledDictionaryChildren('PROJECT')
}

/**
 * 根据当前项目编码同步项目名称快照。
 *
 * 实现步骤：
 * 1. 按项目编码在项目字典中查找当前选项；
 * 2. 找到时写入项目名称，未选择时清空项目名称；
 * 3. 保存时随项目编码提交给后端，保证历史单据展示稳定。
 */
function onProjectChange() {
  form.projectName = projectOptions.value.find((item) => item.code === form.projectCode)?.name || ''
}

/**
 * 执行 defaultLines 方法。
 * 
 * 实现步骤：
 * 1. 读取当前页面状态或调用参数；
 * 2. 完成对应的校验、接口调用或数据转换；
 * 3. 更新页面状态或返回处理结果。
 */
function defaultLines() {
  return [
    { subjectId: undefined, summary: '', debitAmount: 0, creditAmount: 0, currencyCode: 'CNY', currencyName: '人民币', exchangeRateToCny: 1, auxiliary: '' },
    { subjectId: undefined, summary: '', debitAmount: 0, creditAmount: 0, currencyCode: 'CNY', currencyName: '人民币', exchangeRateToCny: 1, auxiliary: '' }
  ]
}

/**
 * 执行 openCreate 方法。
 * 
 * 实现步骤：
 * 1. 读取当前页面状态或调用参数；
 * 2. 完成对应的校验、接口调用或数据转换；
 * 3. 更新页面状态或返回处理结果。
 */
async function openCreate() {
  await refreshDictionaryOptions()
  editingId.value = undefined
  readonly.value = false
  attachmentRef.value?.reset()
  lineSummaryTouched.value = []
  Object.assign(form, {
    /**
     * 字段 voucherDate：表示表单、筛选条件、接口数据或组件状态中的 voucherDate 值。
     */
    voucherDate: new Date().toISOString().slice(0, 10),
    /**
     * 字段 belongMonth：表示表单、筛选条件、接口数据或组件状态中的 belongMonth 值。
     */
    belongMonth: new Date().toISOString().slice(0, 7),
    /**
     * 字段 projectCode：表示项目字典编码，新增凭证时默认为空。
     */
    projectCode: '',
    /**
     * 字段 projectName：表示项目名称快照，新增凭证时默认为空。
     */
    projectName: '',
    /**
     * 字段 summary：表示表单、筛选条件、接口数据或组件状态中的 summary 值。
     */
    summary: '',
    /**
     * 字段 sourceBizNo：表示表单、筛选条件、接口数据或组件状态中的 sourceBizNo 值。
     */
    sourceBizNo: '',
    /**
     * 字段 lines：表示表单、筛选条件、接口数据或组件状态中的 lines 值。
     */
    lines: defaultLines()
  })
  lineSummaryTouched.value = form.lines.map(() => false)
  dialogVisible.value = true
}

/**
 * 执行 fill 方法。
 * 
 * 实现步骤：
 * 1. 读取当前页面状态或调用参数；
 * 2. 完成对应的校验、接口调用或数据转换；
 * 3. 更新页面状态或返回处理结果。
 */
function fill(row: VoucherView) {
  lineSummaryTouched.value = []
  Object.assign(form, {
    /**
     * 字段 voucherDate：表示表单、筛选条件、接口数据或组件状态中的 voucherDate 值。
     */
    voucherDate: row.voucherDate,
    /**
     * 字段 belongMonth：表示表单、筛选条件、接口数据或组件状态中的 belongMonth 值。
     */
    belongMonth: row.belongMonth || row.voucherDate.slice(0, 7),
    /**
     * 字段 projectCode：表示项目字典编码，用于回填凭证所属项目。
     */
    projectCode: row.projectCode || '',
    /**
     * 字段 projectName：表示项目名称快照，用于回填凭证所属项目名称。
     */
    projectName: row.projectName || '',
    /**
     * 字段 summary：表示表单、筛选条件、接口数据或组件状态中的 summary 值。
     */
    summary: row.summary,
    /**
     * 字段 sourceBizNo：表示表单、筛选条件、接口数据或组件状态中的 sourceBizNo 值。
     */
    sourceBizNo: row.sourceBizNo || '',
    /**
     * 字段 lines：表示表单、筛选条件、接口数据或组件状态中的 lines 值。
     */
    lines: row.lines.map((line) => ({
      /**
       * 字段 subjectId：表示表单、筛选条件、接口数据或组件状态中的 subjectId 值。
       */
      subjectId: line.subjectId,
      /**
       * 字段 summary：表示表单、筛选条件、接口数据或组件状态中的 summary 值。
       */
      summary: line.summary,
      /**
       * 字段 debitAmount：表示表单、筛选条件、接口数据或组件状态中的 debitAmount 值。
       */
      debitAmount: Number(line.debitAmount || 0),
      /**
       * 字段 creditAmount：表示表单、筛选条件、接口数据或组件状态中的 creditAmount 值。
       */
      creditAmount: Number(line.creditAmount || 0),
      /**
       * 字段 currencyCode：表示表单、筛选条件、接口数据或组件状态中的 currencyCode 值。
       */
      currencyCode: line.currencyCode || row.currencyCode || 'CNY',
      /**
       * 字段 currencyName：表示表单、筛选条件、接口数据或组件状态中的 currencyName 值。
       */
      currencyName: line.currencyName || row.currencyName || selectedCurrencyName(line.currencyCode || row.currencyCode || 'CNY'),
      /**
       * 字段 exchangeRateToCny：表示表单、筛选条件、接口数据或组件状态中的 exchangeRateToCny 值。
       */
      exchangeRateToCny: Number(line.exchangeRateToCny || row.exchangeRateToCny || 1),
      /**
       * 字段 auxiliary：表示表单、筛选条件、接口数据或组件状态中的 auxiliary 值。
       */
      auxiliary: line.auxiliary || ''
    }))
  })
  lineSummaryTouched.value = form.lines.map(() => false)
}

/**
 * 执行 openEdit 方法。
 * 
 * 实现步骤：
 * 1. 读取当前页面状态或调用参数；
 * 2. 完成对应的校验、接口调用或数据转换；
 * 3. 更新页面状态或返回处理结果。
 */
async function openEdit(row: VoucherView) {
  await refreshDictionaryOptions()
  /**
   * 常量 detail：保存当前模块的页面状态、配置项、接口实例或计算结果。
   */
  const detail = await api.voucher(row.id)
  editingId.value = row.id
  readonly.value = false
  fill(detail)
  await attachmentRef.value?.reload(row.id)
  dialogVisible.value = true
}

/**
 * 执行 view 方法。
 * 
 * 实现步骤：
 * 1. 读取当前页面状态或调用参数；
 * 2. 完成对应的校验、接口调用或数据转换；
 * 3. 更新页面状态或返回处理结果。
 */
async function view(row: VoucherView) {
  await refreshDictionaryOptions()
  /**
   * 常量 detail：保存当前模块的页面状态、配置项、接口实例或计算结果。
   */
  const detail = await api.voucher(row.id)
  editingId.value = row.id
  readonly.value = true
  fill(detail)
  await attachmentRef.value?.reload(row.id)
  dialogVisible.value = true
}

/**
 * 打开在线凭证图片预览。
 *
 * 实现步骤：
 * 1. 按凭证 ID 读取完整凭证明细；
 * 2. 保存到当前预览状态；
 * 3. 打开弹窗，弹窗渲染完成后由 @opened 触发 Canvas 绘制。</p>
 */
async function openVoucherImage(row: VoucherView) {
  voucherImageDetail.value = await api.voucher(row.id)
  voucherImageVisible.value = true
}

/**
 * 打开凭证来源详情。
 *
 * 实现步骤：
 * 1. 清空旧来源详情并打开弹窗；
 * 2. 调用后端来源详情接口；
 * 3. 用通用字段展示采购、应收应付、库存或出纳来源内容。
 */
async function openVoucherSource(row: VoucherView) {
  voucherSourceDetail.value = undefined
  voucherSourceVisible.value = true
  voucherSourceLoading.value = true
  try {
    voucherSourceDetail.value = await api.voucherSource(row.id)
  } finally {
    voucherSourceLoading.value = false
  }
}

/**
 * 绘制在线凭证图片。
 *
 * 实现步骤：
 * 1. 获取 Canvas 2D 上下文并清空画布；
 * 2. 绘制凭证标题、凭证号、日期和附件数；
 * 3. 绘制摘要、科目、借方金额、贷方金额表格；
 * 4. 绘制合计、项目客户备注和制单审核签字位，供用户预览或下载。</p>
 */
function renderVoucherImage() {
  /** 在线凭证 Canvas 节点，承载预览和下载的位图内容。 */
  const canvas = voucherCanvasRef.value
  /** 当前要绘制的凭证明细数据。 */
  const voucher = voucherImageDetail.value
  if (!canvas || !voucher) {
    return
  }
  /** Canvas 2D 绘图上下文，用于逐项绘制参考图样式的凭证栏目。 */
  const ctx = canvas.getContext('2d')
  if (!ctx) {
    return
  }
  ctx.clearRect(0, 0, canvas.width, canvas.height)
  ctx.fillStyle = '#ffffff'
  ctx.fillRect(0, 0, canvas.width, canvas.height)
  drawVoucherFrame(ctx, voucher)
}

/**
 * 绘制凭证主体。
 *
 * 实现步骤：
 * 1. 按参考图定义联查凭证纸面边界、主表栏目、金额分位栏和底部辅助栏；
 * 2. 绘制标题、记字号、制单日期、附单据数以及右上角虚线占位；
 * 3. 绘制摘要、科目名称、借方金额、贷方金额四个主栏目，金额栏按会计凭证分位格线展示；
 * 4. 绘制票号/日期、单价/数量、合计、备注、项目、客户、部门、业务员、个人和签字栏。</p>
 */
function drawVoucherFrame(ctx: CanvasRenderingContext2D, voucher: VoucherView) {
  /** 凭证主框左侧坐标，决定在线凭证相对画布的水平边距。 */
  const left = 44
  /** 凭证主框顶部坐标，决定标题和表格相对画布的垂直起点。 */
  const top = 42
  /** 凭证主框总宽度，用于统一控制标题、表格和签字栏宽度。 */
  const width = 1032
  /** 主分录表格顶部坐标，承接标题区域下方。 */
  const tableTop = 158
  /** 表头高度，包含摘要、科目名称、借方金额、贷方金额四个栏目标题。 */
  const headerHeight = 46
  /** 每条分录行的固定高度，保证凭证图片行距稳定。 */
  const rowHeight = 66
  /** 凭证图片固定展示的分录行数，空行也保留参考图样式。 */
  const bodyRows = 5
  /** 分录区域底部坐标，作为合计区和辅助区的起点。 */
  const bodyBottom = tableTop + headerHeight + rowHeight * bodyRows
  /** 合计区域底部坐标，控制票号/日期/金额合计栏高度。 */
  const totalBottom = bodyBottom + 92
  /** 底部辅助信息区域底部坐标，控制备注、项目、客户等栏目边界。 */
  const footerBottom = totalBottom + 92
  /** 主表列坐标集合，依次表示摘要、科目、借方金额、贷方金额列边界。 */
  const cols = [left, left + 208, left + 612, left + 824, left + width]
  drawVoucherPaperShadow(ctx, left, top, width, footerBottom - top + 44)
  drawVoucherTitle(ctx, voucher, left, top, width, tableTop)
  drawVoucherMainGrid(ctx, left, tableTop, width, headerHeight, rowHeight, bodyRows, bodyBottom, totalBottom, footerBottom, cols)
  drawVoucherHeader(ctx, cols, tableTop, headerHeight)
  drawVoucherLines(ctx, voucher, cols, tableTop, headerHeight, rowHeight, bodyRows)
  drawVoucherTotalSection(ctx, voucher, left, width, cols, bodyBottom, totalBottom)
  drawVoucherFooter(ctx, voucher, left, width, totalBottom, footerBottom)
  drawVoucherSignature(ctx, voucher, left, width, footerBottom)
}

/** 金额栏分位格数量，按参考图保留窄格金额栏效果。 */
const VOUCHER_AMOUNT_DIGITS = 11

/**
 * 绘制凭证纸张叠放阴影。
 *
 * 实现步骤：在主凭证边框外侧绘制两层浅灰偏移线，让在线图片更接近参考图的联查凭证纸面。
 */
function drawVoucherPaperShadow(ctx: CanvasRenderingContext2D, left: number, top: number, width: number, height: number) {
  ctx.save()
  ctx.strokeStyle = '#9ca3af'
  ctx.lineWidth = 1
  ctx.strokeRect(left - 12, top + 18, width + 24, height)
  ctx.strokeRect(left - 6, top + 10, width + 18, height)
  ctx.restore()
}

/**
 * 绘制凭证标题和表格上方信息。
 *
 * 实现步骤：标题使用参考图中的青绿色、字间距和下划线；左侧显示记字号，中间显示制单日期，右侧保留附单据数。
 */
function drawVoucherTitle(ctx: CanvasRenderingContext2D, voucher: VoucherView, left: number, top: number, width: number, tableTop: number) {
  /** 凭证标题基线纵坐标，用于同时定位下划线和右上角虚线。 */
  const titleY = top + 46
  ctx.fillStyle = '#0f766e'
  ctx.font = '700 34px SimSun, "Microsoft YaHei", sans-serif'
  ctx.textAlign = 'center'
  ctx.fillText('记 账 凭 证', left + width / 2, titleY)
  ctx.strokeStyle = '#0f766e'
  ctx.lineWidth = 2
  ctx.beginPath()
  ctx.moveTo(left + 402, titleY + 10)
  ctx.lineTo(left + 630, titleY + 10)
  ctx.stroke()
  ctx.setLineDash([8, 8])
  ctx.strokeStyle = '#2dd4bf'
  ctx.beginPath()
  ctx.moveTo(left + width - 152, titleY - 20)
  ctx.lineTo(left + width - 32, titleY - 20)
  ctx.moveTo(left + width - 152, titleY + 20)
  ctx.lineTo(left + width - 32, titleY + 20)
  ctx.stroke()
  ctx.setLineDash([])
  ctx.fillStyle = '#111827'
  ctx.font = '18px SimSun, "Microsoft YaHei", sans-serif'
  ctx.textAlign = 'left'
  ctx.fillText(`记 字 ${voucherSerial(voucher.voucherNo)}`, left + 62, tableTop - 18)
  ctx.textAlign = 'center'
  ctx.fillText(`制单日期：${formatVoucherDate(voucher.voucherDate)}`, left + width / 2, tableTop - 18)
  ctx.textAlign = 'right'
  ctx.fillText('附单据数：', left + width - 44, tableTop - 18)
}

/**
 * 绘制凭证表格框线。
 *
 * 实现步骤：先绘制主表外框和横向行线，再绘制摘要、科目、借方金额、贷方金额列线，最后在金额列中绘制逐位金额格线。
 */
function drawVoucherMainGrid(
  ctx: CanvasRenderingContext2D,
  left: number,
  tableTop: number,
  width: number,
  headerHeight: number,
  rowHeight: number,
  bodyRows: number,
  bodyBottom: number,
  totalBottom: number,
  footerBottom: number,
  cols: number[]
) {
  ctx.strokeStyle = '#1f2a7a'
  ctx.lineWidth = 2
  ctx.strokeRect(left, tableTop, width, footerBottom - tableTop)
  cols.forEach((x) => {
    ctx.beginPath()
    ctx.moveTo(x, tableTop)
    ctx.lineTo(x, bodyBottom)
    ctx.stroke()
  })
  ctx.beginPath()
  ctx.moveTo(left, tableTop + headerHeight)
  ctx.lineTo(left + width, tableTop + headerHeight)
  ctx.stroke()
  for (let index = 0; index <= bodyRows; index += 1) {
    /** 当前分录横线的纵坐标，逐行绘制形成固定行高表格。 */
    const y = tableTop + headerHeight + rowHeight * index
    ctx.beginPath()
    ctx.moveTo(left, y)
    ctx.lineTo(left + width, y)
    ctx.stroke()
  }
  ;[bodyBottom, totalBottom, footerBottom].forEach((y) => {
    ctx.beginPath()
    ctx.moveTo(left, y)
    ctx.lineTo(left + width, y)
    ctx.stroke()
  })
  drawVoucherAmountColumnGrid(ctx, cols[2], tableTop, cols[3] - cols[2], totalBottom - tableTop)
  drawVoucherAmountColumnGrid(ctx, cols[3], tableTop, cols[4] - cols[3], totalBottom - tableTop)
  ;[left + 520, cols[2], cols[3]].forEach((x) => {
    ctx.strokeStyle = '#1f2a7a'
    ctx.lineWidth = 2
    ctx.beginPath()
    ctx.moveTo(x, bodyBottom)
    ctx.lineTo(x, totalBottom)
    ctx.stroke()
  })
  ;[left + 58, left + 520, left + 824].forEach((x) => {
    ctx.beginPath()
    ctx.moveTo(x, totalBottom)
    ctx.lineTo(x, footerBottom)
    ctx.stroke()
  })
}

/**
 * 绘制金额列分位格线。
 *
 * 实现步骤：在借方或贷方金额区域内按固定数量拆成窄格，普通线用灰色，千/万分组线用蓝色，元角分分界线用红色。
 */
function drawVoucherAmountColumnGrid(ctx: CanvasRenderingContext2D, left: number, top: number, width: number, height: number) {
  /** 单个金额分位格宽度，保证借贷金额栏按固定格数等分。 */
  const unit = width / VOUCHER_AMOUNT_DIGITS
  for (let index = 1; index < VOUCHER_AMOUNT_DIGITS; index += 1) {
    /** 当前金额分位竖线的横坐标。 */
    const x = left + unit * index
    ctx.strokeStyle = index === VOUCHER_AMOUNT_DIGITS - 2 ? '#d94848' : index % 3 === 0 ? '#3746b8' : '#9ca3af'
    ctx.lineWidth = index === VOUCHER_AMOUNT_DIGITS - 2 || index % 3 === 0 ? 1.5 : 1
    ctx.beginPath()
    ctx.moveTo(x, top)
    ctx.lineTo(x, top + height)
    ctx.stroke()
  }
  ctx.strokeStyle = '#1f2a7a'
  ctx.lineWidth = 2
}

/**
 * 绘制凭证表头文字。
 *
 * 实现步骤：按参考图四个主栏目居中绘制“摘要、科目名称、借方金额、贷方金额”。
 */
function drawVoucherHeader(ctx: CanvasRenderingContext2D, cols: number[], tableTop: number, headerHeight: number) {
  /** 凭证主表头文字，顺序和列坐标数组保持一致。 */
  const headers = ['摘 要', '科 目 名 称', '借 方 金 额', '贷 方 金 额']
  ctx.fillStyle = '#0f766e'
  ctx.font = '700 22px SimSun, "Microsoft YaHei", sans-serif'
  ctx.textAlign = 'center'
  headers.forEach((text, index) => {
    ctx.fillText(text, (cols[index] + cols[index + 1]) / 2, tableTop + headerHeight / 2 + 8)
  })
}

/**
 * 绘制凭证明细行。
 *
 * 实现步骤：
 * 1. 分录只落入参考图主表可见行；
 * 2. 摘要和科目自动换行；
 * 3. 借方/贷方金额统一使用人民币快照字段，避免不同币种原币金额直接混合展示。
 */
function drawVoucherLines(ctx: CanvasRenderingContext2D, voucher: VoucherView, cols: number[], tableTop: number, headerHeight: number, rowHeight: number, bodyRows: number) {
  /** 可见分录行集合，超过凭证固定展示行数的内容不绘制到图片主表中。 */
  const rows = voucher.lines.slice(0, bodyRows)
  ctx.fillStyle = '#111827'
  rows.forEach((line, index) => {
    /** 当前分录文字基线纵坐标，基于行号和固定行高计算。 */
    const y = tableTop + headerHeight + rowHeight * index + 24
    ctx.font = '18px SimSun, "Microsoft YaHei", sans-serif'
    ctx.textAlign = 'left'
    drawWrappedText(ctx, line.summary || voucher.summary || '', cols[0] + 8, y, cols[1] - cols[0] - 16, 22, 2)
    drawWrappedText(ctx, voucherSubjectText(line), cols[1] + 8, y, cols[2] - cols[1] - 16, 22, 2)
    drawAmountDigits(ctx, line.debitAmountCny, cols[2], y + 22, cols[3] - cols[2])
    drawAmountDigits(ctx, line.creditAmountCny, cols[3], y + 22, cols[4] - cols[3])
  })
}

/**
 * 绘制票号、日期、单价、数量和合计栏。
 *
 * 实现步骤：
 * 1. 底部第一块区域按参考图拆分为左侧票据信息、中间合计标题、右侧借贷合计金额格；
 * 2. 合计金额统一使用人民币借贷合计；
 * 3. 底部币种固定标注人民币，明确在线凭证金额口径。
 */
function drawVoucherTotalSection(ctx: CanvasRenderingContext2D, voucher: VoucherView, left: number, width: number, cols: number[], bodyBottom: number, totalBottom: number) {
  ctx.fillStyle = '#111827'
  ctx.font = '20px SimSun, "Microsoft YaHei", sans-serif'
  ctx.textAlign = 'left'
  ctx.fillText('票号', left + 8, bodyBottom + 28)
  ctx.fillText('日期', left + 8, bodyBottom + 58)
  ctx.fillText('单价', left + 238, bodyBottom + 58)
  ctx.fillText('数量', left + 238, bodyBottom + 86)
  ctx.font = '700 20px SimSun, "Microsoft YaHei", sans-serif'
  ctx.textAlign = 'center'
  ctx.fillText('合 计', cols[2] - 44, bodyBottom + 60)
  drawAmountDigits(ctx, voucher.totalDebitCny, cols[2], bodyBottom + 62, cols[3] - cols[2])
  drawAmountDigits(ctx, voucher.totalCreditCny, cols[3], bodyBottom + 62, cols[4] - cols[3])
  ctx.font = '16px SimSun, "Microsoft YaHei", sans-serif'
  ctx.textAlign = 'right'
  ctx.fillText('人民币', left + width - 10, totalBottom - 8)
}

/**
 * 绘制凭证底部辅助信息栏。
 *
 * 实现步骤：按参考图固定栏目绘制备注、项目、客户、部门、业务员、个人；没有对应业务字段时只保留栏目名称。
 */
function drawVoucherFooter(ctx: CanvasRenderingContext2D, voucher: VoucherView, left: number, width: number, totalBottom: number, footerBottom: number) {
  ctx.fillStyle = '#111827'
  ctx.font = '20px SimSun, "Microsoft YaHei", sans-serif'
  ctx.textAlign = 'center'
  ctx.fillText('备注', left + 29, totalBottom + 56)
  ctx.textAlign = 'left'
  ctx.fillText('项  目', left + 72, totalBottom + 30)
  ctx.fillText('客  户', left + 72, totalBottom + 62)
  ctx.fillText('部  门', left + 510, totalBottom + 30)
  ctx.fillText('业务员', left + 510, totalBottom + 62)
  ctx.fillText('个  人', left + 868, totalBottom + 30)
  ctx.font = '16px SimSun, "Microsoft YaHei", sans-serif'
  drawWrappedText(ctx, voucher.projectName || voucher.projectCode || '', left + 146, totalBottom + 30, 330, 20, 1)
  drawWrappedText(ctx, voucher.sourceBizNo || '', left + 146, totalBottom + 62, 330, 20, 1)
  drawWrappedText(ctx, voucher.summary || '', left + 8, footerBottom - 10, width - 16, 18, 1)
}

/**
 * 绘制记账、审核、出纳和制单签字栏。
 *
 * 实现步骤：签字栏位于凭证纸面底部，保留参考图“记账、审核、出纳、制单”的横向分布。
 */
function drawVoucherSignature(ctx: CanvasRenderingContext2D, voucher: VoucherView, left: number, width: number, footerBottom: number) {
  /** 签字栏文字基线纵坐标，位于凭证主框下方。 */
  const y = footerBottom + 32
  ctx.fillStyle = '#111827'
  ctx.font = '20px SimSun, "Microsoft YaHei", sans-serif'
  ctx.textAlign = 'left'
  ctx.fillText(`记账  ${voucher.postedBy || ''}`, left + 64, y)
  ctx.fillText('审核', left + 278, y)
  ctx.fillText('出纳', left + 540, y)
  ctx.fillText(`制单  ${voucher.createdBy || ''}`, left + width - 214, y)
}

/**
 * 生成凭证科目显示文字。
 *
 * 实现步骤：
 * 1. 优先使用后端返回的 subjectFullName，保证在线凭证直接展示完整级联科目；
 * 2. 后端字段缺失时按本地科目树用 subjectId 还原名称路径；
 * 3. 如果分录存在辅助核算，则用“科目路径/辅助核算”形式补充。
 */
function voucherSubjectText(line: VoucherView['lines'][number]) {
  /** 当前分录的完整科目名称路径，页面展示不再拼接科目代码。 */
  const subjectName = line.subjectFullName || subjectNamePath(line.subjectId, subjects.value) || line.subjectName || ''
  return line.auxiliary ? `${subjectName}/${line.auxiliary}` : subjectName
}

/**
 * 绘制金额分位数字。
 *
 * 实现步骤：金额转为分位整数并右对齐到金额格，每一个数字单独绘制到对应窄格内，不绘制小数点。
 */
function drawAmountDigits(ctx: CanvasRenderingContext2D, value: number | undefined, left: number, baseline: number, width: number) {
  /** 金额分位字符数组，空白位保留为空格以维持右对齐。 */
  const chars = amountDigitCharacters(value)
  /** 单个金额分位格宽度，用于把每个数字居中落入窄格。 */
  const unit = width / VOUCHER_AMOUNT_DIGITS
  ctx.save()
  ctx.fillStyle = '#111827'
  ctx.font = '20px Consolas, "Microsoft YaHei", sans-serif'
  ctx.textAlign = 'center'
  chars.forEach((char, index) => {
    if (char.trim()) {
      ctx.fillText(char, left + unit * index + unit / 2, baseline)
    }
  })
  ctx.restore()
}

/**
 * 把金额转换为会计分位字符数组。
 *
 * 实现步骤：金额乘以 100 转为分；不足固定格数时左侧补空格，超出时保留右侧有效位。
 */
function amountDigitCharacters(value?: number) {
  /** 转为数字后的金额，避免空值或非法值影响分位计算。 */
  const amount = Number(value || 0)
  if (!Number.isFinite(amount) || Math.abs(amount) < 0.000000005) {
    return Array.from({ length: VOUCHER_AMOUNT_DIGITS }, () => ' ')
  }
  /** 按分转换后的整数金额字符串，用于去掉小数点并右对齐。 */
  const cents = String(Math.round(Math.abs(amount) * 100))
  return cents.slice(-VOUCHER_AMOUNT_DIGITS).padStart(VOUCHER_AMOUNT_DIGITS, ' ').split('')
}

/**
 * 提取凭证字号。
 *
 * 实现步骤：优先取凭证号末尾四位数字并补足 4 位；没有数字时保留原凭证号，保证顶部“记 字”栏始终有可读编号。
 */
function voucherSerial(value?: string) {
  if (!value) {
    return ''
  }
  /** 从凭证号中提取的数字串，优先用于生成四位记字号。 */
  const digits = value.match(/\d+/g)?.join('') || ''
  return digits ? digits.slice(-4).padStart(4, '0') : value
}

/**
 * 绘制自动换行文字。
 */
function drawWrappedText(ctx: CanvasRenderingContext2D, text: string, x: number, y: number, maxWidth: number, lineHeight: number, maxLines: number) {
  /** 当前正在累计的一行文本。 */
  let current = ''
  /** 已绘制的行号，从 0 开始用于计算下一行 y 坐标。 */
  let line = 0
  for (const char of text) {
    /** 尝试追加当前字符后的候选文本，用于判断是否超出最大宽度。 */
    const next = current + char
    if (ctx.measureText(next).width > maxWidth && current) {
      ctx.fillText(current, x, y + line * lineHeight)
      current = char
      line += 1
      if (line >= maxLines) {
        return
      }
    } else {
      current = next
    }
  }
  if (current && line < maxLines) {
    ctx.fillText(current, x, y + line * lineHeight)
  }
}

/**
 * 下载当前在线凭证图片。
 */
function downloadVoucherImage() {
  /** 在线凭证 Canvas 节点，提供 toDataURL 生成下载图片。 */
  const canvas = voucherCanvasRef.value
  /** 当前下载的凭证数据，用于生成默认文件名。 */
  const voucher = voucherImageDetail.value
  if (!canvas || !voucher) {
    ElMessage.warning('请先生成凭证图片')
    return
  }
  /** 临时下载链接，触发浏览器保存 PNG 文件。 */
  const link = document.createElement('a')
  link.download = `${voucher.voucherNo || '记账凭证'}.png`
  link.href = canvas.toDataURL('image/png')
  link.click()
}

/**
 * 批量生成在线凭证图片压缩包。
 *
 * 实现步骤：
 * 1. 校验用户已勾选凭证且有凭证查看权限；
 * 2. 逐条读取凭证明细，保证分录、完整级联科目和人民币金额字段完整；
 * 3. 复用在线凭证 Canvas 绘制逻辑生成 PNG；
 * 4. 将所有 PNG 加入 ZIP 压缩包并下载。
 */
async function batchDownloadVoucherImages() {
  if (!auth.hasMenu('BTN_VOUCHER_VIEW')) {
    ElMessage.warning('无凭证查看权限')
    return
  }
  if (selectedRows.value.length === 0) {
    ElMessage.warning('请先勾选要生成的凭证')
    return
  }
  batchGeneratingVoucherImages.value = true
  try {
    /** ZIP 实例，用于保存多张在线凭证 PNG 图片。 */
    const zip = new JSZip()
    /** 已使用的文件名集合，用于处理重复凭证号导致的 ZIP 内文件覆盖问题。 */
    const usedNames = new Set<string>()
    for (let index = 0; index < selectedRows.value.length; index += 1) {
      /** 当前勾选行，只包含列表字段，需要继续读取完整凭证明细。 */
      const row = selectedRows.value[index]
      /** 当前凭证明细，包含分录、科目完整名称和人民币金额。 */
      const detail = await api.voucher(row.id)
      /** 当前凭证渲染后的 PNG Blob。 */
      const imageBlob = await renderVoucherImageBlob(detail)
      /** ZIP 内图片文件名，使用凭证号并处理非法文件名字符。 */
      const filename = uniqueFilename(`${safeFilename(detail.voucherNo || `记账凭证_${index + 1}`)}.png`, usedNames)
      zip.file(filename, imageBlob)
    }
    /** 最终 ZIP Blob，采用 DEFLATE 压缩以减少下载体积。 */
    const zipBlob = await zip.generateAsync({ type: 'blob', compression: 'DEFLATE' })
    saveBlob(zipBlob, `在线凭证_${timestampText()}.zip`)
    ElMessage.success(`已生成 ${selectedRows.value.length} 张凭证图片`)
  } finally {
    batchGeneratingVoucherImages.value = false
  }
}

/**
 * 将单张凭证明细渲染为 PNG Blob。
 *
 * 实现步骤：
 * 1. 创建离屏 Canvas，尺寸与预览弹窗保持一致；
 * 2. 绘制白色背景和完整凭证框架；
 * 3. 使用 toBlob 输出 PNG，供批量 ZIP 打包。
 */
function renderVoucherImageBlob(voucher: VoucherView) {
  return new Promise<Blob>((resolve, reject) => {
    /** 离屏 Canvas，用于批量生成时复用凭证图片绘制逻辑，不影响当前页面预览弹窗。 */
    const canvas = document.createElement('canvas')
    canvas.width = 1120
    canvas.height = 820
    /** Canvas 2D 上下文，用于绘制在线凭证图片。 */
    const ctx = canvas.getContext('2d')
    if (!ctx) {
      reject(new Error('浏览器无法创建凭证图片画布'))
      return
    }
    ctx.fillStyle = '#ffffff'
    ctx.fillRect(0, 0, canvas.width, canvas.height)
    drawVoucherFrame(ctx, voucher)
    canvas.toBlob((blob) => {
      if (blob) {
        resolve(blob)
      } else {
        reject(new Error('凭证图片生成失败'))
      }
    }, 'image/png')
  })
}

/**
 * 清理文件名中的非法字符。
 *
 * 实现步骤：替换 Windows 和 ZIP 文件名中容易引发歧义的特殊字符，并截断过长名称。
 */
function safeFilename(value: string) {
  return value.replace(/[\\/:*?"<>|]/g, '_').trim().slice(0, 80) || '记账凭证'
}

/**
 * 生成 ZIP 内唯一文件名。
 *
 * 实现步骤：如果文件名已存在，则在扩展名前追加序号，避免同名凭证图片互相覆盖。
 */
function uniqueFilename(filename: string, usedNames: Set<string>) {
  if (!usedNames.has(filename)) {
    usedNames.add(filename)
    return filename
  }
  /** 文件名最后一个点的位置，用于拆分主名和扩展名。 */
  const dotIndex = filename.lastIndexOf('.')
  /** 文件主名，不包含扩展名。 */
  const base = dotIndex > 0 ? filename.slice(0, dotIndex) : filename
  /** 文件扩展名，保留 .png。 */
  const extension = dotIndex > 0 ? filename.slice(dotIndex) : ''
  /** 重名序号，从 2 开始符合常见文件下载命名习惯。 */
  let index = 2
  while (usedNames.has(`${base}_${index}${extension}`)) {
    index += 1
  }
  const next = `${base}_${index}${extension}`
  usedNames.add(next)
  return next
}

/**
 * 生成批量凭证压缩包时间戳。
 *
 * 实现步骤：读取当前本地时间并格式化为年月日时分秒，方便用户区分多次下载文件。
 */
function timestampText() {
  const now = new Date()
  const pad = (value: number) => String(value).padStart(2, '0')
  return `${now.getFullYear()}${pad(now.getMonth() + 1)}${pad(now.getDate())}${pad(now.getHours())}${pad(now.getMinutes())}${pad(now.getSeconds())}`
}

/**
 * 格式化凭证日期。
 */
function formatVoucherDate(value?: string) {
  return value ? value.replaceAll('-', '.') : ''
}

/**
 * 执行 openOperationLogs 方法。
 * 
 * 实现步骤：
 * 1. 读取当前页面状态或调用参数；
 * 2. 完成对应的校验、接口调用或数据转换；
 * 3. 更新页面状态或返回处理结果。
 */
async function openOperationLogs(row: VoucherView) {
  operationLogDrawerRef.value?.open({
    /**
     * 字段 title：表示表单、筛选条件、接口数据或组件状态中的 title 值。
     */
    title: `${row.voucherNo} 凭证流水`,
    /**
     * 字段 load：表示表单、筛选条件、接口数据或组件状态中的 load 值。
     */
    load: (params) => api.voucherOperationLogs(row.id, params)
  })
}

/**
 * 执行 addLine 方法。
 * 
 * 实现步骤：
 * 1. 读取当前页面状态或调用参数；
 * 2. 完成对应的校验、接口调用或数据转换；
 * 3. 更新页面状态或返回处理结果。
 */
function addLine() {
  form.lines.push({ subjectId: undefined, summary: form.summary, debitAmount: 0, creditAmount: 0, currencyCode: 'CNY', currencyName: '人民币', exchangeRateToCny: 1, auxiliary: '' })
  lineSummaryTouched.value.push(false)
}

/**
 * 处理凭证图片/PDF导入。
 *
 * 实现步骤：
 * 1. 从 Element Plus 上传事件读取本次选择的原始文件；
 * 2. 调用后端 AI 识别接口获取凭证分录草稿；
 * 3. 将识别到的主表字段和分录填入当前新增/编辑表单；
 * 4. 将原始文件追加到附件暂存列表，用户保存凭证后自动上传为凭证附件。
 */
async function handleVoucherImport(uploadFile: UploadFile) {
  const file = uploadFile.raw
  if (!file) {
    return
  }
  if (!isVoucherImportFile(file)) {
    ElMessage.warning('凭证导入仅支持图片或PDF文件')
    return
  }
  voucherImportQueue.value.push(file)
  if (voucherImportTimer) {
    clearTimeout(voucherImportTimer)
  }
  voucherImportTimer = setTimeout(() => {
    void recognizeQueuedVoucherFiles()
  }, 120)
}

/**
 * 执行队列中的凭证导入识别。
 *
 * 实现步骤：读取并清空当前队列；构造 FormData 调用后端；识别完成后回填凭证分录并把原文件加入附件暂存。
 */
async function recognizeQueuedVoucherFiles() {
  if (voucherImporting.value) {
    return
  }
  const allowedFiles = [...voucherImportQueue.value]
  voucherImportQueue.value = []
  voucherImportUploadRef.value?.clearFiles()
  if (allowedFiles.length === 0) {
    return
  }
  voucherImporting.value = true
  try {
    const formData = new FormData()
    allowedFiles.forEach((file) => formData.append('files', file))
    const result = await api.importVoucher(formData)
    applyVoucherImportResult(result)
    await attachmentRef.value?.addPendingFiles(allowedFiles)
    const warningText = (result.warnings || []).join('；')
    if (warningText) {
      ElMessage.warning(warningText)
    } else {
      ElMessage.success('凭证识别完成，请核对分录后保存')
    }
  } finally {
    voucherImporting.value = false
  }
}

/**
 * 判断文件是否属于凭证导入支持类型。
 *
 * 实现步骤：按 MIME 和后缀双重判断，兼容部分浏览器上传 PDF 时 contentType 为空的情况。
 */
function isVoucherImportFile(file: File) {
  const suffix = file.name.includes('.') ? file.name.split('.').pop()?.toLowerCase() : ''
  return file.type.startsWith('image/') || file.type === 'application/pdf' || ['jpg', 'jpeg', 'png', 'webp', 'bmp', 'pdf'].includes(suffix || '')
}

/**
 * 回填凭证导入识别结果。
 *
 * 实现步骤：
 * 1. 有识别日期、摘要、来源单号时填入主表，缺失时保留用户当前输入；
 * 2. 将识别分录转换为表格行；
 * 3. 当前表格只有默认空白行时替换，否则追加到已有分录后面；
 * 4. 同步行内校验状态，等待用户确认保存。
 */
function applyVoucherImportResult(result: VoucherImportResult) {
  if (result.voucherDate) {
    form.voucherDate = result.voucherDate
    form.belongMonth = result.voucherDate.slice(0, 7)
  }
  if (result.summary && !form.summary) {
    form.summary = result.summary
  }
  if (result.sourceBizNo && !form.sourceBizNo) {
    form.sourceBizNo = result.sourceBizNo
  }
  const importedLines = (result.lines || [])
    .map(importLineToForm)
    .filter((line) => line.debitAmount > 0 || line.creditAmount > 0 || line.summary || line.subjectId)
  if (importedLines.length === 0) {
    ElMessage.warning('未识别到可用分录，请手工录入')
    return
  }
  const replaceBlankLines = form.lines.every(isBlankLine)
  form.lines = replaceBlankLines ? importedLines : [...form.lines, ...importedLines]
  lineSummaryTouched.value = form.lines.map(() => false)
}

/**
 * 将识别分录转换为前端表单行。
 *
 * 实现步骤：科目 ID 命中时直接回填；未命中时保持为空，用户必须在保存前手工选择。
 */
function importLineToForm(line: VoucherImportLine): VoucherLineForm {
  const currencyCode = line.currencyCode || 'CNY'
  return {
    subjectId: line.subjectId,
    summary: line.summary || form.summary || '凭证导入',
    debitAmount: Number(line.debitAmount || 0),
    creditAmount: Number(line.creditAmount || 0),
    currencyCode,
    currencyName: line.currencyName || selectedCurrencyName(currencyCode),
    exchangeRateToCny: Number(line.exchangeRateToCny || 1),
    auxiliary: line.auxiliary || ''
  }
}

/**
 * 判断分录是否还是新增凭证默认空白行。
 */
function isBlankLine(line: VoucherLineForm) {
  return !line.subjectId
    && !line.summary
    && Number(line.debitAmount || 0) === 0
    && Number(line.creditAmount || 0) === 0
    && (!line.auxiliary || line.auxiliary.length === 0)
}

/**
 * 执行 save 方法。
 * 
 * 实现步骤：
 * 1. 读取当前页面状态或调用参数；
 * 2. 完成对应的校验、接口调用或数据转换；
 * 3. 更新页面状态或返回处理结果。
 */
async function save() {
  /** 表单校验结果，失败时保留字段下方错误提示并阻止提交。 */
  const valid = await formRef.value?.validate().catch(() => false)
  if (!valid) {
    return
  }
  if (!balanced.value) {
    ElMessage.warning('凭证借贷不平衡')
    return
  }
  if (form.lines.some((line) => !line.subjectId || !line.summary)) {
    ElMessage.warning('请完善分录科目和摘要')
    return
  }
  if (!validateVoucherForm()) {
    return
  }
  /**
   * 常量 payload：保存当前模块的页面状态、配置项、接口实例或计算结果。
   */
  const payload = {
    /**
     * 字段 voucherDate：表示表单、筛选条件、接口数据或组件状态中的 voucherDate 值。
     */
    voucherDate: form.voucherDate,
    /**
     * 字段 belongMonth：表示表单、筛选条件、接口数据或组件状态中的 belongMonth 值。
     */
    belongMonth: form.belongMonth,
    /**
     * 字段 projectCode：表示项目字典编码，用于保存凭证项目维度。
     */
    projectCode: form.projectCode || undefined,
    /**
     * 字段 projectName：表示项目名称快照，用于保存凭证项目维度展示值。
     */
    projectName: form.projectName || undefined,
    /**
     * 字段 summary：表示表单、筛选条件、接口数据或组件状态中的 summary 值。
     */
    summary: form.summary,
    /**
     * 字段 sourceBizNo：表示表单、筛选条件、接口数据或组件状态中的 sourceBizNo 值。
     */
    sourceBizNo: form.sourceBizNo,
    /**
     * 字段 lines：表示表单、筛选条件、接口数据或组件状态中的 lines 值。
     */
    lines: form.lines.map((line) => ({
      ...line,
      /**
       * 字段 currencyName：表示表单、筛选条件、接口数据或组件状态中的 currencyName 值。
       */
      currencyName: selectedCurrencyName(line.currencyCode)
    }))
  }
  /**
   * 变量 saved：保存当前模块的页面状态、配置项、接口实例或计算结果。
   */
  let saved: VoucherView
  if (editingId.value) {
    saved = await api.updateVoucher(editingId.value, payload)
  } else {
    saved = await api.createVoucher(payload)
  }
  await attachmentRef.value?.uploadPending(saved.id)
  ElMessage.success('保存成功')
  dialogVisible.value = false
  await load()
}

/**
 * 校验凭证摘要和来源单号。
 *
 * 实现步骤：
 * 1. 主表摘要按 200 字符上限校验；
 * 2. 来源单号按 300 字符上限校验；
 * 3. 每一条分录摘要按 200 字符上限校验；
 * 4. 任一字段超限时阻止保存并提示具体字段。
 */
function validateVoucherForm() {
  /**
   * 常量 invalidLineIndex：保存当前模块的页面状态、配置项、接口实例或计算结果。
   */
  const invalidLineIndex = form.lines.findIndex((line) => lineSummaryError(line))
  if (invalidLineIndex >= 0) {
    touchLineSummary(invalidLineIndex)
    return false
  }
  return true
}

/**
 * 标记指定分录摘要已被用户离开输入框或保存扫描到，用于展示行内错误。
 */
function touchLineSummary(index: number) {
  lineSummaryTouched.value[index] = true
}

/**
 * 返回分录摘要错误文案。
 *
 * 实现步骤：摘要为空时由业务完整性校验处理；摘要超过 200 字符时返回行内错误文案。
 */
function lineSummaryError(line: VoucherLineForm) {
  if (line.summary && line.summary.length > fieldLimits.summary) {
    return `分录摘要不能超过${fieldLimits.summary}个字符`
  }
  return ''
}

/**
 * 执行 post 方法。
 * 
 * 实现步骤：
 * 1. 读取当前页面状态或调用参数；
 * 2. 完成对应的校验、接口调用或数据转换；
 * 3. 更新页面状态或返回处理结果。
 */
async function post(id: number) {
  await api.postVoucher(id)
  ElMessage.success('过账成功')
  await load()
}

/**
 * 执行 voidIt 方法。
 * 
 * 实现步骤：
 * 1. 读取当前页面状态或调用参数；
 * 2. 完成对应的校验、接口调用或数据转换；
 * 3. 更新页面状态或返回处理结果。
 */
async function voidIt(id: number) {
  await api.voidVoucher(id)
  ElMessage.success('作废成功')
  await load()
}

/**
 * 批量删除凭证。
 *
 * 实现步骤：
 * 1. 校验是否已经勾选凭证；
 * 2. 弹出二次确认，避免误删财务凭证；
 * 3. 调用后端批量删除接口；
 * 4. 删除成功后刷新列表。
 */
async function batchRemove() {
  if (selectedRows.value.length === 0) {
    ElMessage.warning('请选择需要删除的凭证')
    return
  }
  await ElMessageBox.confirm(`确认删除选中的 ${selectedRows.value.length} 张凭证？`, '批量删除确认', {
    /**
     * 字段 type：表示表单、筛选条件、接口数据或组件状态中的 type 值。
     */
    type: 'warning',
    /**
     * 字段 confirmButtonText：表示表单、筛选条件、接口数据或组件状态中的 confirmButtonText 值。
     */
    confirmButtonText: '确认删除',
    /**
     * 字段 cancelButtonText：表示表单、筛选条件、接口数据或组件状态中的 cancelButtonText 值。
     */
    cancelButtonText: '取消'
  })
  await api.batchDeleteVouchers(selectedRows.value.map((row) => row.id))
  ElMessage.success('批量删除成功')
  await load()
}

onMounted(async () => {
  applyRouteQuery()
  await Promise.all([load(), refreshDictionaryOptions()])
})

/**
 * 重新读取凭证页面使用的基础数据。
 *
 * 实现步骤：
 * 1. 重新请求启用会计科目、币种和项目字典；
 * 2. GET 请求已追加防缓存参数，避免基础信息修改后仍显示旧下拉；
 * 3. 页面激活或打开凭证弹窗时调用，保证科目和项目最新。
 */
async function refreshDictionaryOptions() {
  await Promise.all([loadSubjects(), loadCurrencies(), loadProjects()])
}

onActivated(() => {
  void refreshDictionaryOptions()
})

watch(
  () => route.query,
  async () => {
    if (applyRouteQuery()) {
      await load()
    } else {
      await maybeOpenRouteVoucherImage()
    }
  }
)

/**
 * 执行 applyRouteQuery 方法。
 * 
 * 实现步骤：
 * 1. 读取当前页面状态或调用参数；
 * 2. 完成对应的校验、接口调用或数据转换；
 * 3. 更新页面状态或返回处理结果。
 */
function applyRouteQuery() {
  /**
   * 变量 changed：保存当前模块的页面状态、配置项、接口实例或计算结果。
   */
  let changed = false
  /**
   * 常量 voucherNo：保存当前模块的页面状态、配置项、接口实例或计算结果。
   */
  const voucherNo = queryString(route.query.voucherNo)
  /**
   * 常量 sourceBizNo：保存当前模块的页面状态、配置项、接口实例或计算结果。
   */
  const sourceBizNo = queryString(route.query.sourceBizNo)
  const openImage = queryString(route.query.openImage) === '1'
  if (openImage) {
    pendingOpenImage.value = true
  }
  if (voucherNo && filters.voucherNo !== voucherNo) {
    filters.voucherNo = voucherNo
    changed = true
  }
  if (sourceBizNo && filters.sourceBizNo !== sourceBizNo) {
    filters.sourceBizNo = sourceBizNo
    changed = true
  }
  return changed
}

/**
 * 根据路由参数自动打开在线凭证。
 *
 * 实现步骤：
 * 1. 判断路由是否携带 openImage=1；
 * 2. 等待凭证列表按 voucherNo 筛选完成；
 * 3. 使用第一条匹配记录打开在线凭证弹窗，并消费自动打开标记。
 */
async function maybeOpenRouteVoucherImage() {
  if (!pendingOpenImage.value || vouchers.value.length === 0) {
    return
  }
  pendingOpenImage.value = false
  await openVoucherImage(vouchers.value[0])
}

</script>

<style scoped>
.line-toolbar,
.total-bar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  margin: 12px 0;
}

.total-bar {
  justify-content: flex-end;
  color: #374151;
}

.line-toolbar-actions {
  display: flex;
  align-items: center;
  gap: 8px;
  flex-wrap: wrap;
}

.voucher-lines-table {
  width: 100%;
  min-width: 0;
  overflow-x: hidden;
}

.voucher-lines-table :deep(.el-table) {
  width: 100% !important;
}

:global(.voucher-dialog .el-dialog__body) {
  overflow-x: hidden;
}

:global(.voucher-dialog .el-table__inner-wrapper),
:global(.voucher-dialog .el-table__body-wrapper),
:global(.voucher-dialog .el-table__header-wrapper) {
  overflow-x: hidden !important;
}

.line-input-error :deep(.el-input__wrapper) {
  box-shadow: 0 0 0 1px var(--danger-color) inset;
}

.line-field-error {
  margin-top: 4px;
  color: var(--danger-color);
  font-size: 12px;
  line-height: 1.2;
}

.full {
  width: 100%;
}

.filter-form {
  margin-bottom: 14px;
  padding: 14px 14px 0;
  border: 1px solid var(--border-color);
  border-radius: var(--radius-sm);
  background: var(--surface-color);
}

.voucher-image-toolbar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  margin-bottom: 12px;
  color: #374151;
  font-weight: 600;
}

.voucher-image-preview {
  overflow: auto;
  padding: 12px;
  border: 1px solid #dbe3ef;
  border-radius: 6px;
  background: #f8fafc;
}

.voucher-image-preview canvas {
  display: block;
  max-width: none;
  border: 1px solid #cbd5e1;
  background: #fff;
}

.voucher-source-dialog {
  min-height: 160px;
}

.voucher-source-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  margin-bottom: 14px;
  color: var(--text-primary);
}

.voucher-source-title {
  font-size: 16px;
  font-weight: 700;
}

.voucher-source-subtitle {
  margin-top: 4px;
  color: var(--text-secondary);
  font-size: 13px;
}

</style>
