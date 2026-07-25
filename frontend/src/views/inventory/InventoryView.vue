<template>
  <div class="page">
    <div class="page-header">
      <div>
        <h1 class="page-title">库存台账</h1>
        <p class="page-subtitle">记录入库、出库、调拨、盘点流水。</p>
      </div>
    </div>
    <el-tabs v-model="activeTab" class="inventory-tabs">
      <el-tab-pane label="库存流水" name="ledger">
        <el-form class="filter-form" :model="filters" label-width="82px">
          <el-row :gutter="12">
            <el-col :xs="24" :sm="12" :md="8" :lg="6">
              <el-form-item label="变动日期">
                <el-date-picker v-model="filters.dateRange" type="daterange" value-format="YYYY-MM-DD" start-placeholder="开始日期" end-placeholder="结束日期" class="full" />
              </el-form-item>
            </el-col>
            <el-col :xs="24" :sm="12" :md="8" :lg="5">
              <el-form-item label="流水号">
                <el-input v-model="filters.movementNo" clearable placeholder="模糊查询流水号" />
              </el-form-item>
            </el-col>
            <el-col :xs="24" :sm="12" :md="8" :lg="4">
              <el-form-item label="类型">
                <el-select v-model="filters.movementType" clearable class="full" placeholder="全部">
                  <el-option label="入库" value="INBOUND" />
                  <el-option label="出库" value="OUTBOUND" />
                  <el-option label="调拨" value="TRANSFER" />
                  <el-option label="盘点" value="CHECK" />
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
              <el-form-item label="物料名称">
                <el-input v-model="filters.itemName" clearable placeholder="模糊查询物料名称" />
              </el-form-item>
            </el-col>
            <el-col :xs="24" :sm="12" :md="8" :lg="5">
              <el-form-item label="来源仓">
                <el-select v-model="filters.fromWarehouse" clearable filterable class="full" placeholder="精确选择来源仓">
                  <el-option v-for="item in warehouseOptions" :key="item.id" :label="item.name" :value="item.name" />
                </el-select>
              </el-form-item>
            </el-col>
            <el-col :xs="24" :sm="12" :md="8" :lg="5">
              <el-form-item label="目标仓">
                <el-select v-model="filters.toWarehouse" clearable filterable class="full" placeholder="精确选择目标仓">
                  <el-option v-for="item in warehouseOptions" :key="item.id" :label="item.name" :value="item.name" />
                </el-select>
              </el-form-item>
            </el-col>
            <el-col :xs="24" :sm="12" :md="8" :lg="5">
              <el-form-item label="关联单号">
                <el-input v-model="filters.relatedBizNo" clearable placeholder="模糊查询关联单号" />
              </el-form-item>
            </el-col>
            <el-col :xs="24" :sm="12" :md="8" :lg="6">
              <el-form-item label=" " class="filter-actions">
                <el-button type="primary" @click="load">查询</el-button>
                <el-button @click="resetFilters">重置</el-button>
                <el-button v-if="auth.hasMenu('BTN_INVENTORY_CREATE')" type="primary" :icon="Plus" @click="openCreate">新增流水</el-button>
                <el-button v-if="auth.hasMenu('BTN_INVENTORY_BATCH_DELETE') && selectedRows.length > 0" type="danger" :icon="Delete" @click="batchRemove">批量删除</el-button>
                <el-button v-if="auth.hasMenu('BTN_INVENTORY_EXPORT')" :icon="Download" :loading="exporting" @click="exportRows">导出</el-button>
              </el-form-item>
            </el-col>
          </el-row>
        </el-form>
        <div class="panel">
          <el-table v-loading="loading" :data="rows" stripe @selection-change="handleSelectionChange">
            <el-table-column type="selection" width="48" />
            <el-table-column label="流水信息" min-width="190">
              <template #default="{ row }">
                <div class="stacked-cell">
                  <div class="stacked-cell__line"><span class="stacked-cell__label">流水号：</span>{{ row.movementNo || '' }}</div>
                  <div class="stacked-cell__line"><span class="stacked-cell__label">类型：</span>{{ row.movementType ? movementTypeLabel(row.movementType) : '' }}</div>
                </div>
              </template>
            </el-table-column>
            <el-table-column prop="movementDate" label="日期" width="120" />
            <el-table-column prop="projectName" label="项目" min-width="140" />
            <el-table-column prop="itemName" label="物料名称" min-width="160" />
            <el-table-column prop="specification" label="规格型号" min-width="130" />
            <el-table-column prop="stockOrganization" label="库存组织" min-width="130" />
            <el-table-column prop="ownerName" label="货主" min-width="120" />
            <el-table-column prop="batchNo" label="批号" min-width="120" />
            <el-table-column prop="quantity" label="数量" width="110" align="right">
              <template #default="{ row }">{{ formatQuantity(row.quantity) }} {{ row.unitName || '' }}</template>
            </el-table-column>
            <el-table-column prop="fromWarehouse" label="来源仓" min-width="120" />
            <el-table-column prop="toWarehouse" label="目标仓" min-width="120" />
            <el-table-column label="操作" width="190" fixed="right">
              <template #default="{ row }">
                <div class="table-actions">
                  <el-button v-if="auth.hasMenu('BTN_INVENTORY_ATTACHMENT') && (row.attachmentCount || 0) > 0" size="small" @click="openAttachment(row)">附件</el-button>
                  <el-button v-if="auth.hasMenu('BTN_VOUCHER_VIEW') && row.voucherId && row.voucherNo" size="small" @click="openOnlineVoucher(row.voucherNo)">在线凭证</el-button>
                  <el-button size="small" @click="openOperationLogs(row)">查看流水</el-button>
                </div>
              </template>
            </el-table-column>
          </el-table>
        </div>
      </el-tab-pane>
      <el-tab-pane label="物料库存" name="materialStock">
        <div class="panel stock-panel">
          <div class="panel-toolbar">
            <div>
              <h2 class="section-title">物料库存</h2>
              <p class="section-subtitle">按照物料字典层级统计库存数量。</p>
            </div>
            <el-button :icon="Refresh" :loading="stockTableLoading" @click="loadMaterialStock">刷新</el-button>
          </div>
          <el-table
            v-loading="stockTableLoading"
            :data="materialStockRows"
            row-key="itemCode"
            default-expand-all
            stripe
            :tree-props="{ children: 'children' }"
          >
            <el-table-column prop="itemCode" label="物料编码" min-width="180" />
            <el-table-column prop="itemName" label="物料名称" min-width="180" />
            <el-table-column prop="inboundQuantity" label="入库总数" width="130" align="right">
              <template #default="{ row }">{{ formatQuantity(row.inboundQuantity) }}</template>
            </el-table-column>
            <el-table-column prop="outboundQuantity" label="出库总数" width="130" align="right">
              <template #default="{ row }">{{ formatQuantity(row.outboundQuantity) }}</template>
            </el-table-column>
            <el-table-column prop="transferQuantity" label="调拨总数" width="130" align="right">
              <template #default="{ row }">{{ formatQuantity(row.transferQuantity) }}</template>
            </el-table-column>
            <el-table-column prop="stockQuantity" label="库存数量" width="130" align="right">
              <template #default="{ row }">
                <span :class="{ 'negative-stock': Number(row.stockQuantity || 0) < 0 }">{{ formatQuantity(row.stockQuantity) }}</span>
              </template>
            </el-table-column>
          </el-table>
        </div>
      </el-tab-pane>
    </el-tabs>
    <el-dialog v-model="dialogVisible" title="新增库存流水" width="min(1120px, 92vw)" top="5vh">
      <el-form ref="formRef" :model="form" :rules="rules" label-width="96px">
        <section class="business-form-section">
        <div class="section-heading"><span>基本信息</span></div>
        <el-row :gutter="12">
          <el-col :xs="24" :sm="8"><el-form-item label="类型"><el-select v-model="form.movementType" class="full"><el-option label="入库" value="INBOUND" /><el-option label="出库" value="OUTBOUND" /><el-option label="调拨" value="TRANSFER" /><el-option label="盘点" value="CHECK" /></el-select></el-form-item></el-col>
          <el-col :xs="24" :sm="8"><el-form-item label="日期"><el-date-picker v-model="form.movementDate" value-format="YYYY-MM-DD" class="full" /></el-form-item></el-col>
          <el-col :xs="24" :sm="8"><el-form-item label="项目" prop="projectCode"><el-select v-model="form.projectCode" clearable filterable class="full" placeholder="请选择项目" @change="onProjectChange"><el-option v-for="item in projectOptions" :key="item.id" :label="item.name" :value="item.code" /></el-select></el-form-item></el-col>
          <el-col :xs="24" :sm="8"><el-form-item label="库存组织"><el-select v-model="form.stockOrganization" clearable filterable class="full"><el-option v-for="item in organizationOptions" :key="item.value" :label="item.label" :value="item.value" /></el-select></el-form-item></el-col>
          <el-col :xs="24" :sm="8"><el-form-item label="货主"><el-select v-model="form.ownerName" clearable filterable class="full"><el-option v-for="item in partnerOptions" :key="item.id" :label="item.name" :value="item.name" /></el-select></el-form-item></el-col>
          <el-col :xs="24" :sm="8"><el-form-item label="来源类型"><el-select v-model="form.sourceBillType" clearable filterable class="full" placeholder="请选择来源类型"><el-option v-for="item in sourceBillTypeOptions" :key="item.id" :label="item.name" :value="item.name" /></el-select></el-form-item></el-col>
          <el-col :xs="24" :sm="8"><el-form-item label="关联单号" prop="relatedBizNo"><el-input v-model="form.relatedBizNo" :maxlength="fieldLimits.sourceBillNo" show-word-limit /></el-form-item></el-col>
          <el-col :xs="24"><el-form-item label="备注" prop="remark"><el-input v-model="form.remark" type="textarea" :rows="2" :maxlength="fieldLimits.remark" show-word-limit /></el-form-item></el-col>
        </el-row>
        </section>
        <section class="business-form-section">
        <div class="section-heading"><span>物料与仓库</span></div>
        <el-row :gutter="12">
          <el-col :xs="24" :sm="8">
            <el-form-item label="物料" prop="itemCode">
              <el-select v-model="form.itemCode" filterable class="full" placeholder="请选择物料" @change="onMaterialChange">
                <el-option v-for="item in materialOptions" :key="item.id" :label="item.name" :value="item.code" />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :xs="24" :sm="8"><el-form-item label="规格型号"><el-input v-model="form.specification" /></el-form-item></el-col>
          <el-col :xs="24" :sm="8"><el-form-item label="单位"><el-select v-model="form.unitName" clearable filterable class="full"><el-option v-for="item in unitOptions" :key="item.id" :label="item.name" :value="item.name" /></el-select></el-form-item></el-col>
          <el-col :xs="24" :sm="8"><el-form-item label="批号"><el-input v-model="form.batchNo" /></el-form-item></el-col>
          <el-col :xs="24" :sm="8"><el-form-item label="数量"><el-input-number v-model="form.quantity" :min="0.0001" :precision="4" class="full" /></el-form-item></el-col>
          <el-col :xs="24" :sm="8">
            <el-form-item label="来源仓">
              <el-select v-model="form.fromWarehouse" clearable filterable class="full" placeholder="请选择来源仓">
                <el-option v-for="item in warehouseOptions" :key="item.id" :label="item.name" :value="item.name" />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :xs="24" :sm="8">
            <el-form-item label="目标仓">
              <el-select v-model="form.toWarehouse" clearable filterable class="full" placeholder="请选择目标仓">
                <el-option v-for="item in warehouseOptions" :key="item.id" :label="item.name" :value="item.name" />
              </el-select>
            </el-form-item>
          </el-col>
        </el-row>
        </section>
        <el-alert
          v-if="stockMessage"
          :title="stockMessage"
          :type="stockExceeded ? 'error' : 'info'"
          :closable="false"
          show-icon
          class="stock-alert"
        />
      </el-form>
      <AttachmentList
        ref="createAttachmentRef"
        business-type="INVENTORY_LEDGER"
        :editable="auth.hasMenu('BTN_INVENTORY_ATTACHMENT')"
      />
      <template #footer><el-button @click="closeCreate">取消</el-button><el-button v-if="auth.hasMenu('BTN_INVENTORY_CREATE') && !stockExceeded" type="primary" @click="save">保存</el-button></template>
    </el-dialog>
    <el-dialog v-model="attachmentDialogVisible" title="库存附件" width="720px" @closed="load">
      <AttachmentList
        ref="manageAttachmentRef"
        business-type="INVENTORY_LEDGER"
        :business-id="attachmentBusinessId"
        :editable="auth.hasMenu('BTN_INVENTORY_ATTACHMENT')"
      />
    </el-dialog>
    <OperationLogDrawer ref="operationLogDrawerRef" />
  </div>
</template>

<script setup lang="ts">
import { computed, onActivated, onMounted, reactive, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { Delete, Download, Plus, Refresh } from '@element-plus/icons-vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import type { FormInstance, FormRules } from 'element-plus'
import AttachmentList from '@/components/attachments/AttachmentList.vue'
import OperationLogDrawer from '@/components/operation-log/OperationLogDrawer.vue'
import { api } from '@/api/fm'
import { saveBlob } from '@/api/http'
import { useAuthStore } from '@/stores/auth'
import type { BasicDictionaryView, InventoryMaterialStockView, InventoryView } from '@/types/api'
import { flattenDictionaryOptions, withFallbackDictionaryOption, type DictionaryOption } from '@/utils/dictionaries'
import { fieldLimits } from '@/utils/validators'
import { queryString } from '@/utils/routeQuery'

/**
 * 常量 loading：保存当前模块的页面状态、配置项、接口实例或计算结果。
 */
const loading = ref(false)
/**
 * 常量 stockTableLoading：保存当前模块的页面状态、配置项、接口实例或计算结果。
 */
const stockTableLoading = ref(false)
/**
 * 常量 exporting：保存当前模块的页面状态、配置项、接口实例或计算结果。
 */
const exporting = ref(false)
/**
 * 常量 auth：保存当前模块的页面状态、配置项、接口实例或计算结果。
 */
const auth = useAuthStore()
/**
 * 常量 route：保存当前模块的页面状态、配置项、接口实例或计算结果。
 */
const route = useRoute()
/** 路由实例，用于从库存台账跳转到凭证记账页面查看在线凭证。 */
const router = useRouter()
/**
 * 常量 handledCreateQueryKey：记录已经消费过的新增流水路由参数，防止筛选刷新时重复打开弹窗。
 */
const handledCreateQueryKey = ref('')
/**
 * 常量 activeTab：保存当前模块的页面状态、配置项、接口实例或计算结果。
 */
const activeTab = ref<'ledger' | 'materialStock'>('ledger')
/**
 * 常量 dialogVisible：保存当前模块的页面状态、配置项、接口实例或计算结果。
 */
const dialogVisible = ref(false)
/**
 * 常量 attachmentDialogVisible：保存当前模块的页面状态、配置项、接口实例或计算结果。
 */
const attachmentDialogVisible = ref(false)
/**
 * 常量 attachmentBusinessId：保存当前模块的页面状态、配置项、接口实例或计算结果。
 */
const attachmentBusinessId = ref<number>()
/**
 * 常量 createAttachmentRef：保存当前模块的页面状态、配置项、接口实例或计算结果。
 */
const createAttachmentRef = ref<InstanceType<typeof AttachmentList>>()
/**
 * 常量 formRef：指向新增库存流水表单实例，用于触发字段级校验并在输入框下方显示错误。
 */
const formRef = ref<FormInstance>()
/**
 * 常量 manageAttachmentRef：保存当前模块的页面状态、配置项、接口实例或计算结果。
 */
const manageAttachmentRef = ref<InstanceType<typeof AttachmentList>>()
/**
 * 常量 operationLogDrawerRef：保存当前模块的页面状态、配置项、接口实例或计算结果。
 */
const operationLogDrawerRef = ref<InstanceType<typeof OperationLogDrawer>>()
/**
 * 常量 rows：保存当前模块的页面状态、配置项、接口实例或计算结果。
 */
const rows = ref<InventoryView[]>([])
/**
 * 常量 materialStockRows：保存当前模块的页面状态、配置项、接口实例或计算结果。
 */
const materialStockRows = ref<InventoryMaterialStockView[]>([])
/**
 * 常量 selectedRows：保存当前模块的页面状态、配置项、接口实例或计算结果。
 */
const selectedRows = ref<InventoryView[]>([])
/**
 * 常量 materialOptions：保存当前模块的页面状态、配置项、接口实例或计算结果。
 */
const materialOptions = ref<BasicDictionaryView[]>([])
/**
 * 常量 warehouseOptions：保存当前模块的页面状态、配置项、接口实例或计算结果。
 */
const warehouseOptions = ref<BasicDictionaryView[]>([])
/**
 * 常量 projectOptions：保存项目字典下拉选项，用于库存流水项目维度筛选和新增。
 */
const projectOptions = ref<BasicDictionaryView[]>([])
/**
 * 常量 organizationOptions：保存当前模块的页面状态、配置项、接口实例或计算结果。
 */
const organizationOptions = ref<DictionaryOption[]>([])
/**
 * 常量 partnerOptions：保存当前模块的页面状态、配置项、接口实例或计算结果。
 */
const partnerOptions = ref<BasicDictionaryView[]>([])
/**
 * 常量 sourceBillTypeOptions：保存当前模块的页面状态、配置项、接口实例或计算结果。
 */
const sourceBillTypeOptions = ref<BasicDictionaryView[]>([])
/**
 * 常量 unitOptions：保存当前模块的页面状态、配置项、接口实例或计算结果。
 */
const unitOptions = ref<BasicDictionaryView[]>([])
/**
 * 常量 availableStock：保存当前模块的页面状态、配置项、接口实例或计算结果。
 */
const availableStock = ref<number>()
/**
 * 常量 stockLoading：保存当前模块的页面状态、配置项、接口实例或计算结果。
 */
const stockLoading = ref(false)
/**
 * 常量 filters：保存当前模块的页面状态、配置项、接口实例或计算结果。
 */
const filters = reactive({
  /**
   * 字段 dateRange：表示表单、筛选条件、接口数据或组件状态中的 dateRange 值。
   */
  dateRange: [] as string[],
  /**
   * 字段 movementNo：表示表单、筛选条件、接口数据或组件状态中的 movementNo 值。
   */
  movementNo: '',
  /**
   * 字段 movementType：表示表单、筛选条件、接口数据或组件状态中的 movementType 值。
   */
  movementType: undefined as InventoryView['movementType'] | undefined,
  /**
   * 字段 projectCode：表示项目字典编码，用于按项目筛选库存流水。
   */
  projectCode: '',
  /**
   * 字段 itemName：表示表单、筛选条件、接口数据或组件状态中的 itemName 值。
   */
  itemName: '',
  /**
   * 字段 fromWarehouse：表示表单、筛选条件、接口数据或组件状态中的 fromWarehouse 值。
   */
  fromWarehouse: '',
  /**
   * 字段 toWarehouse：表示表单、筛选条件、接口数据或组件状态中的 toWarehouse 值。
   */
  toWarehouse: '',
  /**
   * 字段 relatedBizNo：表示表单、筛选条件、接口数据或组件状态中的 relatedBizNo 值。
   */
  relatedBizNo: ''
})
/**
 * InventoryMovementType 类型定义，用于约束页面状态、接口入参或接口返回数据结构。
 */
type InventoryMovementType = InventoryView['movementType']
/**
 * 常量 form：保存当前模块的页面状态、配置项、接口实例或计算结果。
 */
const form = reactive({
  /**
   * 字段 movementType：表示表单、筛选条件、接口数据或组件状态中的 movementType 值。
   */
  movementType: 'INBOUND' as InventoryMovementType,
  /**
   * 字段 movementDate：表示表单、筛选条件、接口数据或组件状态中的 movementDate 值。
   */
  movementDate: new Date().toISOString().slice(0, 10),
  /**
   * 字段 projectCode：表示项目字典编码，保存库存流水所属项目。
   */
  projectCode: '',
  /**
   * 字段 projectName：表示项目名称快照，保存库存流水新增时的项目名称。
   */
  projectName: '',
  /**
   * 字段 stockOrganization：表示表单、筛选条件、接口数据或组件状态中的 stockOrganization 值。
   */
  stockOrganization: '',
  /**
   * 字段 ownerName：表示表单、筛选条件、接口数据或组件状态中的 ownerName 值。
   */
  ownerName: '',
  /**
   * 字段 itemCode：表示表单、筛选条件、接口数据或组件状态中的 itemCode 值。
   */
  itemCode: '',
  /**
   * 字段 itemName：表示表单、筛选条件、接口数据或组件状态中的 itemName 值。
   */
  itemName: '',
  /**
   * 字段 specification：表示表单、筛选条件、接口数据或组件状态中的 specification 值。
   */
  specification: '',
  /**
   * 字段 unitName：表示表单、筛选条件、接口数据或组件状态中的 unitName 值。
   */
  unitName: '',
  /**
   * 字段 batchNo：表示表单、筛选条件、接口数据或组件状态中的 batchNo 值。
   */
  batchNo: '',
  /**
   * 字段 quantity：表示表单、筛选条件、接口数据或组件状态中的 quantity 值。
   */
  quantity: 1,
  /**
   * 字段 fromWarehouse：表示表单、筛选条件、接口数据或组件状态中的 fromWarehouse 值。
   */
  fromWarehouse: '',
  /**
   * 字段 toWarehouse：表示表单、筛选条件、接口数据或组件状态中的 toWarehouse 值。
   */
  toWarehouse: '',
  /**
   * 字段 relatedBizNo：表示表单、筛选条件、接口数据或组件状态中的 relatedBizNo 值。
   */
  relatedBizNo: '',
  /**
   * 字段 sourceBillType：表示表单、筛选条件、接口数据或组件状态中的 sourceBillType 值。
   */
  sourceBillType: '',
  /**
   * 字段 remark：表示表单、筛选条件、接口数据或组件状态中的 remark 值。
   */
  remark: ''
})

/**
 * 库存流水新增表单字段校验规则。
 *
 * 实现步骤：
 * 1. 必填字段交给 Element Plus 表单规则在字段下方提示；
 * 2. 关联单号和备注按统一长度限制校验；
 * 3. 所有输入型字段在鼠标移出输入框时触发 blur 校验，自动显示红色边框。
 */
const rules: FormRules = {
  itemCode: [{ required: true, message: '请选择物料', trigger: 'change' }],
  projectCode: [{ required: true, message: '请选择项目', trigger: 'change' }],
  relatedBizNo: [{ max: fieldLimits.sourceBillNo, message: `关联单号不能超过${fieldLimits.sourceBillNo}个字符`, trigger: 'blur' }],
  remark: [{ max: fieldLimits.remark, message: `备注不能超过${fieldLimits.remark}个字符`, trigger: 'blur' }]
}
/**
 * 常量 stockCheckType：保存当前模块的页面状态、配置项、接口实例或计算结果。
 */
const stockCheckType = computed(() => form.movementType === 'OUTBOUND' || form.movementType === 'TRANSFER')
/**
 * 常量 stockExceeded：保存当前模块的页面状态、配置项、接口实例或计算结果。
 */
const stockExceeded = computed(() => stockCheckType.value && availableStock.value !== undefined && Number(form.quantity || 0) > availableStock.value)
/**
 * 常量 stockMessage：保存当前模块的页面状态、配置项、接口实例或计算结果。
 */
const stockMessage = computed(() => {
  if (!stockCheckType.value || !form.itemCode || !form.fromWarehouse) {
    return ''
  }
  if (stockLoading.value) {
    return '正在校验来源仓可用库存'
  }
  if (availableStock.value === undefined) {
    return '未查询到来源仓可用库存，请确认物料、来源仓和入库记录'
  }
  /**
   * 常量 text：保存当前模块的页面状态、配置项、接口实例或计算结果。
   */
  const text = `来源仓 ${form.fromWarehouse} 当前可用库存：${formatQuantity(availableStock.value)}`
  return stockExceeded.value ? `${text}，本次数量 ${formatQuantity(form.quantity)} 已超过可用库存，不能提交` : text
})

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
    rows.value = await api.inventoryLedgers(inventorySearchParams())
    selectedRows.value = []
  } finally {
    loading.value = false
  }
}
/**
 * 执行 loadMaterialStock 方法。
 * 
 * 实现步骤：
 * 1. 读取当前页面状态或调用参数；
 * 2. 完成对应的校验、接口调用或数据转换；
 * 3. 更新页面状态或返回处理结果。
 */
async function loadMaterialStock() {
  stockTableLoading.value = true
  try {
    materialStockRows.value = await api.inventoryMaterialStock()
  } finally {
    stockTableLoading.value = false
  }
}
/**
 * 执行 openAttachment 方法。
 * 
 * 实现步骤：
 * 1. 读取当前页面状态或调用参数；
 * 2. 完成对应的校验、接口调用或数据转换；
 * 3. 更新页面状态或返回处理结果。
 */
function openAttachment(row: InventoryView) {
  attachmentBusinessId.value = row.id
  attachmentDialogVisible.value = true
  void manageAttachmentRef.value?.reload(row.id)
}
/**
 * 执行 openOperationLogs 方法。
 * 
 * 实现步骤：
 * 1. 读取当前页面状态或调用参数；
 * 2. 完成对应的校验、接口调用或数据转换；
 * 3. 更新页面状态或返回处理结果。
 */
async function openOperationLogs(row: InventoryView) {
  operationLogDrawerRef.value?.open({
    /**
     * 字段 title：表示表单、筛选条件、接口数据或组件状态中的 title 值。
     */
    title: `${row.movementNo} 库存流水`,
    /**
     * 字段 load：表示表单、筛选条件、接口数据或组件状态中的 load 值。
     */
    load: (params) => api.inventoryOperationLogs(row.id, params)
  })
}

/**
 * 从库存流水跳转查看在线凭证。
 *
 * 实现步骤：
 * 1. 接收库存流水已关联的凭证号；
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
 * 处理库存物料选择变化。
 *
 * 实现步骤：
 * 1. 根据物料编码从基础资料中找到物料名称；
 * 2. 带出规格型号和默认库存单位；
 * 3. 重新校验来源仓可用库存，避免出库/调拨数量超限。
 */
function onMaterialChange(code: string) {
  /**
   * 常量 selected：保存当前模块的页面状态、配置项、接口实例或计算结果。
   */
  const selected = materialOptions.value.find((item) => item.code === code)
  form.itemName = selected?.name || ''
  form.specification = selected?.description || ''
  form.unitName = form.unitName || '件'
  void refreshAvailableStock()
}

/**
 * 根据当前项目编码同步项目名称快照。
 *
 * 实现步骤：
 * 1. 按项目编码从项目字典中定位选项；
 * 2. 选中项目时写入项目名称，清空项目时清空名称；
 * 3. 保存库存流水时一起提交项目编码和名称，保证查看流水能展示项目。
 */
function onProjectChange() {
  form.projectName = projectOptions.value.find((item) => item.code === form.projectCode)?.name || ''
}

/**
 * 重置库存流水表单。
 *
 * 实现步骤：
 * 1. 恢复入库类型、当天日期和默认数量；
 * 2. 清空组织、货主、物料、批号、仓库和来源单据信息；
 * 3. 保证下一次新增不会继承上一次录入内容。
 */
function resetForm() {
  Object.assign(form, {
    /**
     * 字段 movementType：表示表单、筛选条件、接口数据或组件状态中的 movementType 值。
     */
    movementType: 'INBOUND',
    /**
     * 字段 movementDate：表示表单、筛选条件、接口数据或组件状态中的 movementDate 值。
     */
    movementDate: new Date().toISOString().slice(0, 10),
    /**
     * 字段 projectCode：表示项目字典编码，新增库存流水时默认为空。
     */
    projectCode: '',
    /**
     * 字段 projectName：表示项目名称快照，新增库存流水时默认为空。
     */
    projectName: '',
    /**
     * 字段 stockOrganization：表示表单、筛选条件、接口数据或组件状态中的 stockOrganization 值。
     */
    stockOrganization: '',
    /**
     * 字段 ownerName：表示表单、筛选条件、接口数据或组件状态中的 ownerName 值。
     */
    ownerName: '',
    /**
     * 字段 itemCode：表示表单、筛选条件、接口数据或组件状态中的 itemCode 值。
     */
    itemCode: '',
    /**
     * 字段 itemName：表示表单、筛选条件、接口数据或组件状态中的 itemName 值。
     */
    itemName: '',
    /**
     * 字段 specification：表示表单、筛选条件、接口数据或组件状态中的 specification 值。
     */
    specification: '',
    /**
     * 字段 unitName：表示表单、筛选条件、接口数据或组件状态中的 unitName 值。
     */
    unitName: '',
    /**
     * 字段 batchNo：表示表单、筛选条件、接口数据或组件状态中的 batchNo 值。
     */
    batchNo: '',
    /**
     * 字段 quantity：表示表单、筛选条件、接口数据或组件状态中的 quantity 值。
     */
    quantity: 1,
    /**
     * 字段 fromWarehouse：表示表单、筛选条件、接口数据或组件状态中的 fromWarehouse 值。
     */
    fromWarehouse: '',
    /**
     * 字段 toWarehouse：表示表单、筛选条件、接口数据或组件状态中的 toWarehouse 值。
     */
    toWarehouse: '',
    /**
     * 字段 relatedBizNo：表示表单、筛选条件、接口数据或组件状态中的 relatedBizNo 值。
     */
    relatedBizNo: '',
    /**
     * 字段 sourceBillType：表示表单、筛选条件、接口数据或组件状态中的 sourceBillType 值。
     */
    sourceBillType: '',
    /**
     * 字段 remark：表示表单、筛选条件、接口数据或组件状态中的 remark 值。
     */
    remark: ''
  })
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
  resetForm()
  createAttachmentRef.value?.reset()
  dialogVisible.value = true
}
/**
 * 执行 closeCreate 方法。
 * 
 * 实现步骤：
 * 1. 读取当前页面状态或调用参数；
 * 2. 完成对应的校验、接口调用或数据转换；
 * 3. 更新页面状态或返回处理结果。
 */
function closeCreate() {
  createAttachmentRef.value?.reset()
  dialogVisible.value = false
}
/**
 * 执行 inventorySearchParams 方法。
 * 
 * 实现步骤：
 * 1. 读取当前页面状态或调用参数；
 * 2. 完成对应的校验、接口调用或数据转换；
 * 3. 更新页面状态或返回处理结果。
 */
function inventorySearchParams() {
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
     * 字段 movementNo：表示表单、筛选条件、接口数据或组件状态中的 movementNo 值。
     */
    movementNo: filters.movementNo.trim() || undefined,
    /**
     * 字段 movementType：表示表单、筛选条件、接口数据或组件状态中的 movementType 值。
     */
    movementType: filters.movementType,
    /**
     * 字段 projectCode：表示项目字典编码，用于按项目筛选库存流水。
     */
    projectCode: filters.projectCode || undefined,
    /**
     * 字段 itemName：表示表单、筛选条件、接口数据或组件状态中的 itemName 值。
     */
    itemName: filters.itemName.trim() || undefined,
    /**
     * 字段 fromWarehouse：表示表单、筛选条件、接口数据或组件状态中的 fromWarehouse 值。
     */
    fromWarehouse: filters.fromWarehouse || undefined,
    /**
     * 字段 toWarehouse：表示表单、筛选条件、接口数据或组件状态中的 toWarehouse 值。
     */
    toWarehouse: filters.toWarehouse || undefined,
    /**
     * 字段 relatedBizNo：表示表单、筛选条件、接口数据或组件状态中的 relatedBizNo 值。
     */
    relatedBizNo: filters.relatedBizNo.trim() || undefined
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
     * 字段 movementNo：表示表单、筛选条件、接口数据或组件状态中的 movementNo 值。
     */
    movementNo: '',
    /**
     * 字段 movementType：表示表单、筛选条件、接口数据或组件状态中的 movementType 值。
     */
    movementType: undefined,
    /**
     * 字段 projectCode：表示项目字典编码，用于清空项目筛选条件。
     */
    projectCode: '',
    /**
     * 字段 itemName：表示表单、筛选条件、接口数据或组件状态中的 itemName 值。
     */
    itemName: '',
    /**
     * 字段 fromWarehouse：表示表单、筛选条件、接口数据或组件状态中的 fromWarehouse 值。
     */
    fromWarehouse: '',
    /**
     * 字段 toWarehouse：表示表单、筛选条件、接口数据或组件状态中的 toWarehouse 值。
     */
    toWarehouse: '',
    /**
     * 字段 relatedBizNo：表示表单、筛选条件、接口数据或组件状态中的 relatedBizNo 值。
     */
    relatedBizNo: ''
  })
  void load()
}
/**
 * 执行 handleSelectionChange 方法。
 * 
 * 实现步骤：
 * 1. 读取当前页面状态或调用参数；
 * 2. 完成对应的校验、接口调用或数据转换；
 * 3. 更新页面状态或返回处理结果。
 */
function handleSelectionChange(selection: InventoryView[]) {
  selectedRows.value = selection
}
/**
 * 导出库存台账列表。
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
      : inventorySearchParams()
    const { blob, filename } = await api.exportInventoryLedgers(payload)
    saveBlob(blob, filename || '库存台账.xlsx')
    ElMessage.success('导出成功')
  } finally {
    exporting.value = false
  }
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
  /** 库存流水表单校验结果，失败时字段下方显示错误并阻止提交。 */
  const valid = await formRef.value?.validate().catch(() => false)
  if (!valid) {
    return
  }
  if (stockCheckType.value) {
    await refreshAvailableStock()
    if (stockExceeded.value) {
      ElMessage.error(stockMessage.value)
      return
    }
  }
  /**
   * 常量 saved：保存当前模块的页面状态、配置项、接口实例或计算结果。
   */
  const saved = await api.createInventoryLedger({ ...form })
  await createAttachmentRef.value?.uploadPending(saved.id)
  ElMessage.success('保存成功')
  dialogVisible.value = false
  createAttachmentRef.value?.reset()
  resetForm()
  await Promise.all([load(), loadMaterialStock()])
}

/**
 * 执行 refreshAvailableStock 方法。
 * 
 * 实现步骤：
 * 1. 读取当前页面状态或调用参数；
 * 2. 完成对应的校验、接口调用或数据转换；
 * 3. 更新页面状态或返回处理结果。
 */
async function refreshAvailableStock() {
  if (!stockCheckType.value || !form.itemCode || !form.fromWarehouse) {
    availableStock.value = undefined
    return
  }
  stockLoading.value = true
  try {
    /**
     * 常量 stock：保存当前模块的页面状态、配置项、接口实例或计算结果。
     */
    const stock = await api.inventoryStock({
      /**
       * 字段 itemCode：表示表单、筛选条件、接口数据或组件状态中的 itemCode 值。
       */
      itemCode: form.itemCode,
      /**
       * 字段 warehouse：表示表单、筛选条件、接口数据或组件状态中的 warehouse 值。
       */
      warehouse: form.fromWarehouse,
      /**
       * 字段 asOfDate：表示表单、筛选条件、接口数据或组件状态中的 asOfDate 值。
       */
      asOfDate: form.movementDate
    })
    availableStock.value = Number(stock.availableQuantity || 0)
  } catch {
    availableStock.value = undefined
  } finally {
    stockLoading.value = false
  }
}

/**
 * 执行 movementTypeLabel 方法。
 * 
 * 实现步骤：
 * 1. 读取当前页面状态或调用参数；
 * 2. 完成对应的校验、接口调用或数据转换；
 * 3. 更新页面状态或返回处理结果。
 */
function movementTypeLabel(value: InventoryView['movementType']) {
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

/**
 * 执行 formatQuantity 方法。
 * 
 * 实现步骤：
 * 1. 读取当前页面状态或调用参数；
 * 2. 完成对应的校验、接口调用或数据转换；
 * 3. 更新页面状态或返回处理结果。
 */
function formatQuantity(value: number) {
  /**
   * 常量 numberValue：保存当前模块的页面状态、配置项、接口实例或计算结果。
   */
  const numberValue = Number(value || 0)
  if (Number.isInteger(numberValue)) {
    return String(numberValue)
  }
  return numberValue.toFixed(4).replace(/0+$/, '').replace(/\.$/, '')
}

/**
 * 批量删除库存流水。
 *
 * 实现步骤：
 * 1. 校验是否已经勾选库存流水；
 * 2. 弹出二次确认，避免误删库存台账；
 * 3. 调用后端批量删除接口；
 * 4. 删除成功后刷新列表。
 */
async function batchRemove() {
  if (selectedRows.value.length === 0) {
    ElMessage.warning('请选择需要删除的库存流水')
    return
  }
  await ElMessageBox.confirm(`确认删除选中的 ${selectedRows.value.length} 条库存流水？`, '批量删除确认', {
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
  await api.batchDeleteInventoryLedgers(selectedRows.value.map((row) => row.id))
  ElMessage.success('批量删除成功')
  await load()
}
/**
 * 加载库存流水表单字典。
 *
 * 实现步骤：
 * 1. 并行读取物料、仓库、组织、往来单位、来源单据和计量单位字典；
 * 2. 组织字典按树形层级转换为下拉选项，方便识别上下级组织；
 * 3. 给计量单位补默认项，保证旧环境未初始化新字典时仍可新增库存流水。
 */
async function loadDictionaryOptions() {
  const [materials, warehouses, projects, organizations, partners, sourceBillTypes, units] = await Promise.all([
    api.enabledDictionaryChildren('MATERIAL'),
    api.enabledDictionaryChildren('WAREHOUSE'),
    api.enabledDictionaryChildren('PROJECT'),
    api.enabledDictionaryTree('ORGANIZATION'),
    api.enabledDictionaryChildren('PARTNER'),
    api.enabledDictionaryChildren('SOURCE_BILL_TYPE'),
    api.enabledDictionaryChildren('UNIT')
  ])
  materialOptions.value = materials
  warehouseOptions.value = warehouses
  projectOptions.value = projects
  organizationOptions.value = flattenDictionaryOptions(organizations)
  partnerOptions.value = partners
  sourceBillTypeOptions.value = sourceBillTypes
  unitOptions.value = withFallbackDictionaryOption(units, 'UNIT_PIECE', '件')
}
onMounted(async () => {
  applyRouteQuery()
  await Promise.all([load(), refreshDictionaryOptions(), loadMaterialStock()])
})

/**
 * 重新读取库存页面使用的全部基础字典。
 *
 * 实现步骤：
 * 1. 重新请求物料、仓库、项目、组织、往来单位、来源单据和计量单位；
 * 2. HTTP 层自动追加防缓存参数，确保基础信息维护后能立即取到最新选项；
 * 3. 页面被 keep-alive 激活或打开新增流水弹窗时调用。
 */
async function refreshDictionaryOptions() {
  await loadDictionaryOptions()
}

onActivated(() => {
  void refreshDictionaryOptions()
})

watch(
  () => route.query,
  async () => {
    if (applyRouteQuery()) {
      await load()
    }
  }
)

watch(
  () => [form.movementType, form.itemCode, form.fromWarehouse, form.movementDate],
  () => {
    void refreshAvailableStock()
  }
)

watch(activeTab, (value) => {
  if (value === 'materialStock') {
    void loadMaterialStock()
  }
})

/**
 * 处理库存台账页面路由参数。
 *
 * 实现步骤：
 * 1. 读取 movementNo、relatedBizNo，继续支持智能检索进入库存页后的列表筛选；
 * 2. 识别 action=createInventoryLedger 的跨模块新增意图；
 * 3. 新增意图未消费过时打开新增库存流水弹窗并预填采购收货数据；
 * 4. 返回筛选条件是否变化，供调用方决定是否重新加载列表。
 */
function applyRouteQuery() {
  /**
   * 变量 changed：保存当前模块的页面状态、配置项、接口实例或计算结果。
   */
  let changed = false
  /**
   * 常量 movementNo：保存当前模块的页面状态、配置项、接口实例或计算结果。
   */
  const movementNo = queryString(route.query.movementNo)
  /**
   * 常量 relatedBizNo：保存当前模块的页面状态、配置项、接口实例或计算结果。
   */
  const relatedBizNo = queryString(route.query.relatedBizNo)
  if (movementNo && filters.movementNo !== movementNo) {
    filters.movementNo = movementNo
    changed = true
  }
  if (relatedBizNo && filters.relatedBizNo !== relatedBizNo) {
    filters.relatedBizNo = relatedBizNo
    changed = true
  }
  applyCreateLedgerQuery()
  return changed
}

/**
 * 根据跨模块路由参数打开新增库存流水弹窗。
 *
 * 实现步骤：
 * 1. 仅处理采购收货等模块传入的 createInventoryLedger 动作；
 * 2. 对当前 query 生成稳定 key，同一 key 已处理时直接跳过；
 * 3. 调用 openCreate 复用新增表单重置和附件重置逻辑；
 * 4. 将路由参数写入表单字段，未提供的字段保持新增表单默认值，由用户继续补录。
 */
function applyCreateLedgerQuery() {
  if (queryString(route.query.action) !== 'createInventoryLedger') {
    return
  }
  /** 当前路由参数的稳定标识，用于避免同一次跳转重复打开新增弹窗。 */
  const queryKey = JSON.stringify(route.query)
  if (handledCreateQueryKey.value === queryKey) {
    return
  }
  handledCreateQueryKey.value = queryKey
  activeTab.value = 'ledger'
  openCreate()
  Object.assign(form, {
    movementType: queryString(route.query.movementType) || 'INBOUND',
    movementDate: queryString(route.query.movementDate) || new Date().toISOString().slice(0, 10),
    projectCode: queryString(route.query.projectCode),
    projectName: queryString(route.query.projectName),
    stockOrganization: queryString(route.query.stockOrganization),
    ownerName: queryString(route.query.ownerName),
    itemCode: queryString(route.query.itemCode),
    itemName: queryString(route.query.itemName),
    specification: queryString(route.query.specification),
    unitName: queryString(route.query.unitName),
    quantity: queryNumber(route.query.quantity) || 1,
    toWarehouse: queryString(route.query.toWarehouse),
    relatedBizNo: queryString(route.query.relatedBizNo),
    sourceBillType: queryString(route.query.sourceBillType),
    remark: queryString(route.query.remark)
  })
  void refreshAvailableStock()
}

/**
 * 执行 queryString 方法。
 * 
 * 实现步骤：
 * 1. 读取当前页面状态或调用参数；
 * 2. 完成对应的校验、接口调用或数据转换；
 * 3. 更新页面状态或返回处理结果。
 */

/**
 * 将路由参数转换为库存数量数值。
 *
 * 实现步骤：
 * 1. 复用 queryString 兼容 Vue Router 的数组和字符串两种 query 形态；
 * 2. 空值或非数字返回 undefined，由调用方使用默认数量；
 * 3. 有效数字直接返回，确保库存表单的 input-number 组件拿到 number 类型。
 */
function queryNumber(value: unknown) {
  /** 路由参数转换后的文本值，兼容单值和数组两种 query 形态。 */
  const text = queryString(value)
  if (!text) {
    return undefined
  }
  /** 文本转换后的数量值，非法数字会被丢弃并由调用方使用默认值。 */
  const numberValue = Number(text)
  return Number.isFinite(numberValue) ? numberValue : undefined
}
</script>

<style scoped>
.full{width:100%;}

.filter-form {
  margin-bottom: 14px;
  padding: 16px 16px 2px;
  border: 1px solid var(--border-color);
  border-radius: var(--radius-md);
  background:
    linear-gradient(180deg, var(--surface-tint-color), transparent 150px),
    var(--surface-color);
}

.business-form-section {
  margin-bottom: 14px;
  padding: 12px 14px 4px;
  border: 1px solid var(--border-color);
  border-radius: var(--radius-md);
  background: var(--subtle-surface-color);
}

.section-heading {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  margin-bottom: 12px;
  padding-bottom: 8px;
  border-bottom: 1px solid var(--border-color);
  color: var(--heading-color);
  font-size: 14px;
  font-weight: 700;
}

.stock-alert {
  margin: 8px 0 2px;
}

.inventory-tabs {
  margin-top: 6px;
}

.stock-panel {
  padding: 16px;
}

.panel-toolbar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  margin-bottom: 12px;
}

.section-title {
  margin: 0;
  font-size: 16px;
  font-weight: 600;
  color: var(--heading-color);
}

.section-subtitle {
  margin: 4px 0 0;
  font-size: 13px;
  color: var(--muted-text-color);
}

.negative-stock {
  color: var(--danger-color);
  font-weight: 600;
}

</style>
