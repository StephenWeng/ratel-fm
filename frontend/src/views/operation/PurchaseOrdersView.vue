<template>
  <div class="page">
    <div class="page-header">
      <div>
        <h1 class="page-title">采购管理</h1>
        <p class="page-subtitle">维护采购单、采购明细和业务状态。</p>
      </div>
    </div>

    <el-form class="filter-form" :model="filters" label-width="82px">
      <el-row :gutter="12">
        <el-col :xs="24" :sm="12" :md="8" :lg="6">
          <el-form-item label="采购日期">
            <el-date-picker v-model="filters.dateRange" type="daterange" value-format="YYYY-MM-DD" start-placeholder="开始日期" end-placeholder="结束日期" class="full" />
          </el-form-item>
        </el-col>
        <el-col :xs="24" :sm="12" :md="8" :lg="5">
          <el-form-item label="采购单号">
            <el-input v-model="filters.orderNo" clearable placeholder="模糊查询单号" />
          </el-form-item>
        </el-col>
        <el-col :xs="24" :sm="12" :md="8" :lg="5">
          <el-form-item label="供应商">
            <el-select v-model="filters.supplierName" clearable filterable class="full" placeholder="精确选择供应商">
              <el-option v-for="item in supplierOptions" :key="item.id" :label="item.name" :value="item.name" />
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
        <el-col :xs="24" :sm="12" :md="8" :lg="4">
          <el-form-item label="状态">
            <el-select v-model="filters.status" clearable class="full" placeholder="全部">
              <el-option v-for="item in statusOptions" :key="item.value" :label="item.label" :value="item.value" />
            </el-select>
          </el-form-item>
        </el-col>
        <el-col :xs="24" :sm="12" :md="8" :lg="5">
          <el-form-item label="创建人">
            <el-input v-model="filters.createdBy" clearable placeholder="模糊查询创建人" />
          </el-form-item>
        </el-col>
        <el-col :xs="24" :sm="12" :md="8" :lg="5">
          <el-form-item label="备注">
            <el-input v-model="filters.remark" clearable placeholder="模糊查询备注" />
          </el-form-item>
        </el-col>
        <el-col :xs="24" :sm="12" :md="8" :lg="6">
          <el-form-item label=" " class="filter-actions">
            <el-button type="primary" @click="load">查询</el-button>
            <el-button @click="resetFilters">重置</el-button>
            <el-button v-if="auth.hasMenu('BTN_PURCHASE_CREATE')" type="primary" :icon="Plus" @click="openCreate">新增采购单</el-button>
            <el-button v-if="auth.hasMenu('BTN_PURCHASE_BATCH_DELETE') && selectedRows.length > 0" type="danger" :icon="Delete" @click="batchRemove">批量删除</el-button>
            <el-button v-if="auth.hasMenu('BTN_PURCHASE_EXPORT')" :icon="Download" :loading="exporting" @click="exportRows">导出</el-button>
          </el-form-item>
        </el-col>
      </el-row>
    </el-form>

    <div class="panel">
      <el-table v-loading="loading" :data="orders" stripe @selection-change="handleSelectionChange">
        <el-table-column type="selection" width="48" />
        <el-table-column label="采购信息" min-width="210">
          <template #default="{ row }">
            <div class="stacked-cell">
              <div class="stacked-cell__line"><span class="stacked-cell__label">采购单号：</span>{{ row.orderNo || '' }}</div>
              <div class="stacked-cell__line"><span class="stacked-cell__label">单据类型：</span>{{ row.documentType || '' }}</div>
              <div class="stacked-cell__line"><span class="stacked-cell__label">业务类型：</span>{{ row.businessType || '' }}</div>
            </div>
          </template>
        </el-table-column>
        <el-table-column prop="supplierName" label="供应商" min-width="180" />
        <el-table-column prop="projectName" label="项目" min-width="140" />
        <el-table-column label="组织与结算" min-width="260">
          <template #default="{ row }">
            <div class="stacked-cell">
              <div class="stacked-cell__line"><span class="stacked-cell__label">采购组织：</span>{{ blankText(row.purchaseOrganization) }}</div>
              <div class="stacked-cell__line"><span class="stacked-cell__label">采购部门：</span>{{ blankText(row.purchaseDepartment) }}</div>
              <div class="stacked-cell__line"><span class="stacked-cell__label">采购员：</span>{{ blankText(row.purchaserName) }}</div>
              <div class="stacked-cell__line"><span class="stacked-cell__label">结算组织：</span>{{ blankText(row.settlementOrganization) }}</div>
              <div class="stacked-cell__line"><span class="stacked-cell__label">付款条件：</span>{{ blankText(row.paymentTerms) }}</div>
              <div class="stacked-cell__line"><span class="stacked-cell__label">结算方式：</span>{{ blankText(row.settlementMethod) }}</div>
              <div class="stacked-cell__line"><span class="stacked-cell__label">交货条件：</span>{{ blankText(row.deliveryTerms) }}</div>
            </div>
          </template>
        </el-table-column>
        <el-table-column prop="orderDate" label="采购日期" width="120" />
        <el-table-column label="状态" width="150">
          <template #default="{ row }">
            <div v-if="row.status === 'APPROVED' || row.status === 'APPROVAL_REJECTED'" class="approval-status">
              <el-tag type="success">已审批</el-tag>
              <el-tag :type="row.status === 'APPROVED' ? 'success' : 'danger'">
                {{ row.status === 'APPROVED' ? '同意' : '不同意' }}
              </el-tag>
            </div>
            <el-tag v-else :type="statusTagType(row.status)">{{ statusLabel(row.status) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="totalAmount" label="总金额" width="130" align="right">
          <template #default="{ row }">
            <AmountText v-if="row.currencyCode !== 'MULTI'" :value="row.totalAmount" :currency-code="row.currencyCode || 'CNY'" :currency-name="row.currencyName" />
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
        <el-table-column prop="totalAmountCny" label="总金额人民币" width="140" align="right">
          <template #default="{ row }"><AmountText :value="row.totalAmountCny" currency-code="CNY" currency-name="人民币" /></template>
        </el-table-column>
        <el-table-column label="操作" width="430" fixed="right">
          <template #default="{ row }">
            <div class="table-actions">
              <el-button v-if="auth.hasMenu('BTN_PURCHASE_VIEW')" size="small" @click="view(row)">详情</el-button>
              <el-button v-if="auth.hasMenu('BTN_PURCHASE_EDIT') && canEditPurchase(row.status)" size="small" @click="openEdit(row)">编辑</el-button>
              <el-button v-if="auth.hasMenu('BTN_PURCHASE_STATUS') && canSubmitApproval(row.status)" size="small" type="primary" @click="openApproval(row)">提交审批</el-button>
              <el-button v-if="auth.hasMenu('BTN_PURCHASE_STATUS') && row.status === 'APPROVED'" size="small" type="success" @click="startPurchase(row)">发起采购</el-button>
              <el-button v-if="auth.hasMenu('BTN_PURCHASE_STATUS') && row.status === 'PURCHASING'" size="small" type="success" @click="receivePurchase(row)">已收货</el-button>
              <el-button v-if="auth.hasMenu('BTN_PURCHASE_STATUS') && canCancelPurchase(row.status)" size="small" type="danger" @click="openCancelPurchase(row)">取消采购</el-button>
              <el-button v-if="auth.hasMenu('BTN_VOUCHER_VIEW') && row.voucherId && row.voucherNo" size="small" @click="openOnlineVoucher(row.voucherNo)">在线凭证</el-button>
              <el-button v-if="auth.hasMenu('BTN_PURCHASE_VIEW')" size="small" @click="openOperationLogs(row)">查看流水</el-button>
            </div>
          </template>
        </el-table-column>
      </el-table>
    </div>

    <el-dialog v-model="dialogVisible" :title="dialogTitle" width="min(1280px, 94vw)" top="5vh">
      <el-form ref="formRef" :model="form" :rules="rules" label-width="96px" :disabled="readonly">
        <section class="business-form-section">
        <div class="section-heading"><span>基本信息</span></div>
        <el-row :gutter="12">
          <el-col :xs="24" :sm="8">
            <el-form-item label="供应商" prop="supplierName">
              <el-select v-model="form.supplierName" filterable class="full" placeholder="请选择供应商">
                <el-option v-for="item in supplierOptions" :key="item.id" :label="item.name" :value="item.name" />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :xs="24" :sm="8">
            <el-form-item label="单据类型">
              <el-select v-model="form.documentType" filterable class="full">
                <el-option v-for="item in purchaseDocumentTypeOptions" :key="item.id" :label="item.name" :value="item.name" />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :xs="24" :sm="8">
            <el-form-item label="业务类型">
              <el-select v-model="form.businessType" filterable class="full">
                <el-option v-for="item in purchaseBusinessTypeOptions" :key="item.id" :label="item.name" :value="item.name" />
              </el-select>
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
            <el-form-item label="采购日期" prop="orderDate">
              <el-date-picker v-model="form.orderDate" type="date" value-format="YYYY-MM-DD" class="full" />
            </el-form-item>
          </el-col>
          <el-col :xs="24" :sm="8">
            <el-form-item label="来源类型">
              <el-select v-model="form.sourceBillType" clearable filterable class="full" placeholder="请选择来源类型">
                <el-option v-for="item in sourceBillTypeOptions" :key="item.id" :label="item.name" :value="item.name" />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :xs="24" :sm="8">
            <el-form-item label="来源单号" prop="sourceBillNo">
              <el-input v-model="form.sourceBillNo" :maxlength="fieldLimits.sourceBillNo" show-word-limit />
            </el-form-item>
          </el-col>
          <el-col :xs="24" :sm="24">
            <el-form-item label="备注" prop="remark">
              <el-input v-model="form.remark" type="textarea" :rows="2" :maxlength="fieldLimits.remark" show-word-limit />
            </el-form-item>
          </el-col>
        </el-row>
        </section>
        <section class="business-form-section">
        <div class="section-heading"><span>组织与结算</span></div>
        <el-row :gutter="12">
          <el-col :xs="24" :sm="8"><el-form-item label="采购组织"><el-select v-model="form.purchaseOrganization" clearable filterable class="full"><el-option v-for="item in organizationOptions" :key="item.value" :label="item.label" :value="item.value" /></el-select></el-form-item></el-col>
          <el-col :xs="24" :sm="8"><el-form-item label="采购部门"><el-select v-model="form.purchaseDepartment" clearable filterable class="full"><el-option v-for="item in departmentOptions" :key="item.value" :label="item.label" :value="item.value" /></el-select></el-form-item></el-col>
          <el-col :xs="24" :sm="8"><el-form-item label="采购员" prop="purchaserName"><el-input v-model="form.purchaserName" :maxlength="fieldLimits.chineseName" show-word-limit placeholder="中文姓名" /></el-form-item></el-col>
          <el-col :xs="24" :sm="8"><el-form-item label="结算组织"><el-select v-model="form.settlementOrganization" clearable filterable class="full"><el-option v-for="item in organizationOptions" :key="item.value" :label="item.label" :value="item.value" /></el-select></el-form-item></el-col>
          <el-col :xs="24" :sm="8"><el-form-item label="付款条件"><el-select v-model="form.paymentTerms" clearable filterable class="full"><el-option v-for="item in paymentTermsOptions" :key="item.id" :label="item.name" :value="item.name" /></el-select></el-form-item></el-col>
          <el-col :xs="24" :sm="8"><el-form-item label="结算方式"><el-select v-model="form.settlementMethod" clearable filterable class="full"><el-option v-for="item in settlementMethodOptions" :key="item.id" :label="item.name" :value="item.name" /></el-select></el-form-item></el-col>
          <el-col :xs="24" :sm="8"><el-form-item label="交货条件"><el-select v-model="form.deliveryTerms" clearable filterable class="full"><el-option v-for="item in deliveryTermsOptions" :key="item.id" :label="item.name" :value="item.name" /></el-select></el-form-item></el-col>
        </el-row>
        </section>
        <section class="business-form-section">
        <div class="section-heading">
          <span>采购明细</span>
          <el-button v-if="!readonly && auth.hasAnyMenu(['BTN_PURCHASE_CREATE', 'BTN_PURCHASE_EDIT'])" size="small" :icon="Plus" @click="addLine">新增明细</el-button>
        </div>
        <el-table :data="form.lines" border>
          <el-table-column label="物料" min-width="240">
            <template #default="{ row }">
              <el-select v-model="row.itemCode" filterable class="full" placeholder="请选择物料" @change="onLineMaterialChange(row)">
                <el-option v-for="item in materialOptions" :key="item.id" :label="item.name" :value="item.code" />
              </el-select>
            </template>
          </el-table-column>
          <el-table-column label="规格型号" width="140">
            <template #default="{ row }"><el-input v-model="row.specification" /></template>
          </el-table-column>
          <el-table-column label="单位" width="90">
            <template #default="{ row }">
              <el-select v-model="row.unitName" clearable filterable class="full">
                <el-option v-for="item in unitOptions" :key="item.id" :label="item.name" :value="item.name" />
              </el-select>
            </template>
          </el-table-column>
          <el-table-column label="数量" width="140">
            <template #default="{ row }"><el-input-number v-model="row.quantity" :min="0.0001" :precision="4" :controls="false" class="full" /></template>
          </el-table-column>
          <el-table-column label="单价" width="150">
            <template #default="{ row }">
              <el-input-number v-model="row.unitPrice" :min="0.00000001" :precision="8" :controls="false" class="full" />
            </template>
          </el-table-column>
          <el-table-column label="税率" width="120">
            <template #default="{ row }">
              <el-input-number v-model="row.taxRate" :min="0" :max="1" :precision="6" :controls="false" class="full" />
            </template>
          </el-table-column>
          <el-table-column label="币种" width="120">
            <template #default="{ row }">
              <el-select v-model="row.currencyCode" filterable class="full" @change="onLineCurrencyChange(row)">
                <el-option v-for="item in currencyOptions" :key="item.code" :label="item.name" :value="item.code" />
              </el-select>
            </template>
          </el-table-column>
          <el-table-column label="汇率" width="150">
            <template #default="{ row }">
              <el-input-number v-model="row.exchangeRateToCny" :min="0.00000001" :precision="8" :controls="false" :disabled="row.currencyCode === 'CNY'" class="full" />
            </template>
          </el-table-column>
          <el-table-column label="金额" width="130" align="right">
            <template #default="{ row }"><AmountText :value="lineAmount(row)" :currency-code="row.currencyCode || 'CNY'" :currency-name="row.currencyName" /></template>
          </el-table-column>
          <el-table-column label="税额" width="130" align="right">
            <template #default="{ row }"><AmountText :value="lineTaxAmount(row)" :currency-code="row.currencyCode || 'CNY'" :currency-name="row.currencyName" /></template>
          </el-table-column>
          <el-table-column label="价税合计" width="140" align="right">
            <template #default="{ row }"><AmountText :value="lineAmountWithTax(row)" :currency-code="row.currencyCode || 'CNY'" :currency-name="row.currencyName" /></template>
          </el-table-column>
          <el-table-column label="金额(人民币)" width="150" align="right">
            <template #default="{ row }"><AmountText :value="lineAmountCny(row)" currency-code="CNY" currency-name="人民币" /></template>
          </el-table-column>
          <el-table-column label="到货日期" width="150">
            <template #default="{ row }"><el-date-picker v-model="row.plannedArrivalDate" type="date" value-format="YYYY-MM-DD" class="full" /></template>
          </el-table-column>
          <el-table-column label="收货仓库" width="140">
            <template #default="{ row }">
              <el-select v-model="row.receiveWarehouse" clearable filterable class="full">
                <el-option v-for="item in warehouseOptions" :key="item.id" :label="item.name" :value="item.name" />
              </el-select>
            </template>
          </el-table-column>
          <el-table-column label="操作" width="90">
            <template #default="{ $index }">
              <el-button v-if="canDeleteLine && form.lines.length > 1" size="small" type="danger" @click="form.lines.splice($index, 1)">删除</el-button>
            </template>
          </el-table-column>
        </el-table>
        </section>
        <div class="total-bar">采购人民币合计：<AmountText :value="totalAmountCny" currency-code="CNY" currency-name="人民币" /></div>
      </el-form>
      <AttachmentList
        ref="attachmentRef"
        business-type="PURCHASE_ORDER"
        :business-id="editingId"
        :editable="canManageAttachment"
      />
      <template #footer>
        <el-button @click="dialogVisible = false">关闭</el-button>
        <el-button v-if="canSaveOrder" type="primary" @click="save">保存</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="approvalVisible" title="提交采购审批" width="min(640px, 92vw)" top="16vh">
      <el-form ref="approvalFormRef" :model="approvalForm" :rules="approvalRules" label-width="96px">
        <el-form-item label="采购单号">
          <el-input :model-value="approvalTarget?.orderNo || ''" disabled />
        </el-form-item>
        <el-form-item label="申请理由" prop="applyReason">
          <el-input
            v-model="approvalForm.applyReason"
            type="textarea"
            :rows="5"
            :maxlength="fieldLimits.remark"
            show-word-limit
            resize="vertical"
          />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="approvalVisible = false">取消</el-button>
        <el-button type="primary" @click="submitApproval">确认提交</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="cancelVisible" title="取消采购" width="min(640px, 92vw)" top="16vh">
      <el-form ref="cancelFormRef" :model="cancelForm" :rules="cancelRules" label-width="96px">
        <el-form-item label="采购单号">
          <el-input :model-value="cancelTarget?.orderNo || ''" disabled />
        </el-form-item>
        <el-form-item label="取消类型" prop="cancelType">
          <el-select v-model="cancelForm.cancelType" filterable class="full" placeholder="请选择取消类型">
            <el-option v-for="item in purchaseCancelTypeOptions" :key="item.id" :label="item.name" :value="item.name" />
          </el-select>
        </el-form-item>
        <el-form-item label="取消原因" prop="cancelReason">
          <el-input
            v-model="cancelForm.cancelReason"
            type="textarea"
            :rows="5"
            :maxlength="fieldLimits.remark"
            show-word-limit
            resize="vertical"
          />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="cancelVisible = false">关闭</el-button>
        <el-button type="danger" @click="cancelPurchase">确认取消</el-button>
      </template>
    </el-dialog>

    <OperationLogDrawer ref="operationLogDrawerRef" />
  </div>
</template>

<script setup lang="ts">
import { computed, onActivated, onMounted, reactive, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import type { FormInstance, FormRules } from 'element-plus'
import { Delete, Download, Plus } from '@element-plus/icons-vue'
import AttachmentList from '@/components/attachments/AttachmentList.vue'
import AmountText from '@/components/common/AmountText.vue'
import OperationLogDrawer from '@/components/operation-log/OperationLogDrawer.vue'
import { api } from '@/api/fm'
import { saveBlob } from '@/api/http'
import { useAuthStore } from '@/stores/auth'
import type { BasicDictionaryView, PurchaseOrderView, PurchaseStatus } from '@/types/api'
import { flattenDictionaryOptions, withFallbackDictionaryOption, type DictionaryOption } from '@/utils/dictionaries'
import { chineseNamePattern, fieldLimits } from '@/utils/validators'
import { fallbackExchangeRateToCny, formatMoney, roundBusinessMoney } from '@/utils/money'
import { queryString } from '@/utils/routeQuery'

/**
 * PurchaseLineForm 类型定义，用于约束页面状态、接口入参或接口返回数据结构。
 */
interface PurchaseLineForm {
  /**
   * 字段 itemCode：表示表单、筛选条件、接口数据或组件状态中的 itemCode 值。
   */
  itemCode: string
  /**
   * 字段 itemName：表示表单、筛选条件、接口数据或组件状态中的 itemName 值。
   */
  itemName: string
  /**
   * 字段 specification：表示表单、筛选条件、接口数据或组件状态中的 specification 值。
   */
  specification: string
  /**
   * 字段 unitName：表示表单、筛选条件、接口数据或组件状态中的 unitName 值。
   */
  unitName: string
  /**
   * 字段 quantity：表示表单、筛选条件、接口数据或组件状态中的 quantity 值。
   */
  quantity: number
  /**
   * 字段 unitPrice：表示表单、筛选条件、接口数据或组件状态中的 unitPrice 值。
   */
  unitPrice: number
  /**
   * 字段 taxRate：表示表单、筛选条件、接口数据或组件状态中的 taxRate 值。
   */
  taxRate: number
  /**
   * 字段 plannedArrivalDate：表示表单、筛选条件、接口数据或组件状态中的 plannedArrivalDate 值。
   */
  plannedArrivalDate: string
  /**
   * 字段 receiveWarehouse：表示表单、筛选条件、接口数据或组件状态中的 receiveWarehouse 值。
   */
  receiveWarehouse: string
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
 * 常量 auth：保存当前模块的页面状态、配置项、接口实例或计算结果。
 */
const auth = useAuthStore()
/**
 * 常量 route：保存当前模块的页面状态、配置项、接口实例或计算结果。
 */
const route = useRoute()
/**
 * 常量 router：负责采购状态变更后的跨模块跳转，当前用于打开库存台账新增流水弹窗。
 */
const router = useRouter()
/**
 * 常量 orders：保存当前模块的页面状态、配置项、接口实例或计算结果。
 */
const orders = ref<PurchaseOrderView[]>([])
/**
 * 常量 selectedRows：保存当前模块的页面状态、配置项、接口实例或计算结果。
 */
const selectedRows = ref<PurchaseOrderView[]>([])
/**
 * 常量 supplierOptions：保存当前模块的页面状态、配置项、接口实例或计算结果。
 */
const supplierOptions = ref<BasicDictionaryView[]>([])
/**
 * 常量 projectOptions：保存项目字典下拉选项，用于采购单项目维度筛选和录入。
 */
const projectOptions = ref<BasicDictionaryView[]>([])
/**
 * 常量 materialOptions：保存当前模块的页面状态、配置项、接口实例或计算结果。
 */
const materialOptions = ref<BasicDictionaryView[]>([])
/**
 * 常量 currencyOptions：保存当前模块的页面状态、配置项、接口实例或计算结果。
 */
const currencyOptions = ref<BasicDictionaryView[]>([])
/**
 * 常量 warehouseOptions：保存当前模块的页面状态、配置项、接口实例或计算结果。
 */
const warehouseOptions = ref<BasicDictionaryView[]>([])
/**
 * 常量 organizationOptions：保存当前模块的页面状态、配置项、接口实例或计算结果。
 */
const organizationOptions = ref<DictionaryOption[]>([])
/**
 * 常量 departmentOptions：保存当前模块的页面状态、配置项、接口实例或计算结果。
 */
const departmentOptions = ref<DictionaryOption[]>([])
/**
 * 常量 purchaseDocumentTypeOptions：保存当前模块的页面状态、配置项、接口实例或计算结果。
 */
const purchaseDocumentTypeOptions = ref<BasicDictionaryView[]>([])
/**
 * 常量 purchaseBusinessTypeOptions：保存当前模块的页面状态、配置项、接口实例或计算结果。
 */
const purchaseBusinessTypeOptions = ref<BasicDictionaryView[]>([])
/**
 * 常量 sourceBillTypeOptions：保存当前模块的页面状态、配置项、接口实例或计算结果。
 */
const sourceBillTypeOptions = ref<BasicDictionaryView[]>([])
/**
 * 常量 paymentTermsOptions：保存当前模块的页面状态、配置项、接口实例或计算结果。
 */
const paymentTermsOptions = ref<BasicDictionaryView[]>([])
/**
 * 常量 settlementMethodOptions：保存当前模块的页面状态、配置项、接口实例或计算结果。
 */
const settlementMethodOptions = ref<BasicDictionaryView[]>([])
/**
 * 常量 deliveryTermsOptions：保存当前模块的页面状态、配置项、接口实例或计算结果。
 */
const deliveryTermsOptions = ref<BasicDictionaryView[]>([])
/**
 * 常量 unitOptions：保存当前模块的页面状态、配置项、接口实例或计算结果。
 */
const unitOptions = ref<BasicDictionaryView[]>([])
/**
 * 常量 purchaseCancelTypeOptions：保存取消采购类型字典选项，用于取消采购弹窗。
 */
const purchaseCancelTypeOptions = ref<BasicDictionaryView[]>([])
/**
 * 常量 attachmentRef：保存当前模块的页面状态、配置项、接口实例或计算结果。
 */
const attachmentRef = ref<InstanceType<typeof AttachmentList>>()
/**
 * 常量 formRef：指向采购单弹窗表单实例，用于字段级校验并显示红框。
 */
const formRef = ref<FormInstance>()
/**
 * 常量 approvalFormRef：指向提交审批弹窗表单实例，用于校验申请理由长度。
 */
const approvalFormRef = ref<FormInstance>()
/**
 * 常量 cancelFormRef：指向取消采购弹窗表单实例，用于校验取消类型和取消原因。
 */
const cancelFormRef = ref<FormInstance>()
/**
 * 常量 operationLogDrawerRef：保存当前模块的页面状态、配置项、接口实例或计算结果。
 */
const operationLogDrawerRef = ref<InstanceType<typeof OperationLogDrawer>>()
/**
 * 常量 dialogVisible：保存当前模块的页面状态、配置项、接口实例或计算结果。
 */
const dialogVisible = ref(false)
/**
 * 常量 editingId：保存当前模块的页面状态、配置项、接口实例或计算结果。
 */
const editingId = ref<number>()
/**
 * 常量 readonly：保存当前模块的页面状态、配置项、接口实例或计算结果。
 */
const readonly = ref(false)
/**
 * 常量 approvalVisible：控制提交采购审批弹窗显示状态。
 */
const approvalVisible = ref(false)
/**
 * 常量 approvalTarget：保存当前准备提交审批的采购单。
 */
const approvalTarget = ref<PurchaseOrderView>()
/**
 * 常量 cancelVisible：控制取消采购弹窗显示状态。
 */
const cancelVisible = ref(false)
/**
 * 常量 cancelTarget：保存当前准备取消采购的采购单。
 */
const cancelTarget = ref<PurchaseOrderView>()
/**
 * 常量 filters：保存当前模块的页面状态、配置项、接口实例或计算结果。
 */
const filters = reactive({
  /**
   * 字段 dateRange：表示表单、筛选条件、接口数据或组件状态中的 dateRange 值。
   */
  dateRange: [] as string[],
  /**
   * 字段 orderNo：表示表单、筛选条件、接口数据或组件状态中的 orderNo 值。
   */
  orderNo: '',
  /**
   * 字段 supplierName：表示表单、筛选条件、接口数据或组件状态中的 supplierName 值。
   */
  supplierName: '',
  /**
   * 字段 projectCode：表示项目字典编码，用于按项目筛选采购单。
   */
  projectCode: '',
  /**
   * 字段 status：表示表单、筛选条件、接口数据或组件状态中的 status 值。
   */
  status: undefined as PurchaseStatus | undefined,
  /**
   * 字段 createdBy：表示表单、筛选条件、接口数据或组件状态中的 createdBy 值。
   */
  createdBy: '',
  /**
   * 字段 remark：表示表单、筛选条件、接口数据或组件状态中的 remark 值。
   */
  remark: ''
})
/**
 * 常量 form：保存当前模块的页面状态、配置项、接口实例或计算结果。
 */
const form = reactive({
  /**
   * 字段 supplierName：表示表单、筛选条件、接口数据或组件状态中的 supplierName 值。
   */
  supplierName: '',
  /**
   * 字段 documentType：表示表单、筛选条件、接口数据或组件状态中的 documentType 值。
   */
  documentType: '标准采购订单',
  /**
   * 字段 businessType：表示表单、筛选条件、接口数据或组件状态中的 businessType 值。
   */
  businessType: '标准采购',
  /**
   * 字段 projectCode：表示项目字典编码，保存采购单所属项目。
   */
  projectCode: '',
  /**
   * 字段 projectName：表示项目名称快照，保存采购单创建或修改时的项目名称。
   */
  projectName: '',
  /**
   * 字段 purchaseOrganization：表示表单、筛选条件、接口数据或组件状态中的 purchaseOrganization 值。
   */
  purchaseOrganization: '',
  /**
   * 字段 purchaseDepartment：表示表单、筛选条件、接口数据或组件状态中的 purchaseDepartment 值。
   */
  purchaseDepartment: '',
  /**
   * 字段 purchaserName：表示表单、筛选条件、接口数据或组件状态中的 purchaserName 值。
   */
  purchaserName: '',
  /**
   * 字段 settlementOrganization：表示表单、筛选条件、接口数据或组件状态中的 settlementOrganization 值。
   */
  settlementOrganization: '',
  /**
   * 字段 paymentTerms：表示表单、筛选条件、接口数据或组件状态中的 paymentTerms 值。
   */
  paymentTerms: '',
  /**
   * 字段 settlementMethod：表示表单、筛选条件、接口数据或组件状态中的 settlementMethod 值。
   */
  settlementMethod: '',
  /**
   * 字段 deliveryTerms：表示表单、筛选条件、接口数据或组件状态中的 deliveryTerms 值。
   */
  deliveryTerms: '',
  /**
   * 字段 sourceBillType：表示表单、筛选条件、接口数据或组件状态中的 sourceBillType 值。
   */
  sourceBillType: '',
  /**
   * 字段 sourceBillNo：表示表单、筛选条件、接口数据或组件状态中的 sourceBillNo 值。
   */
  sourceBillNo: '',
  /**
   * 字段 orderDate：表示表单、筛选条件、接口数据或组件状态中的 orderDate 值。
   */
  orderDate: new Date().toISOString().slice(0, 10),
  /**
   * 字段 remark：表示表单、筛选条件、接口数据或组件状态中的 remark 值。
   */
  remark: '',
  /**
   * 字段 lines：表示表单、筛选条件、接口数据或组件状态中的 lines 值。
   */
  lines: [] as PurchaseLineForm[]
})

/**
 * 常量 approvalForm：保存提交审批弹窗的申请理由。
 */
const approvalForm = reactive({
  /**
   * 字段 applyReason：采购单进入审批流程时提交给审批人的申请说明。
   */
  applyReason: ''
})

/**
 * 常量 cancelForm：保存取消采购弹窗的取消类型和取消原因。
 */
const cancelForm = reactive({
  /**
   * 字段 cancelType：取消采购类型，来自基础字典 PURCHASE_CANCEL_TYPE。
   */
  cancelType: '',
  /**
   * 字段 cancelReason：取消采购原因，作为业务流水和后续追溯依据。
   */
  cancelReason: ''
})

/**
 * 采购单表单字段校验规则。
 *
 * 实现步骤：
 * 1. 供应商和采购日期作为必填字段由表单组件提示；
 * 2. 采购员按中文姓名规则校验；
 * 3. 来源单号和备注按统一长度限制校验，blur 后输入框红框并在下方显示提示。
 */
const rules: FormRules = {
  supplierName: [{ required: true, message: '请选择供应商', trigger: 'change' }],
  projectCode: [{ required: true, message: '请选择项目', trigger: 'change' }],
  orderDate: [{ required: true, message: '请选择采购日期', trigger: 'change' }],
  purchaserName: [{ pattern: chineseNamePattern, message: '采购员姓名必须为1到20个中文字符', trigger: 'blur' }],
  sourceBillNo: [{ max: fieldLimits.sourceBillNo, message: `来源单号不能超过${fieldLimits.sourceBillNo}个字符`, trigger: 'blur' }],
  remark: [{ max: fieldLimits.remark, message: `备注不能超过${fieldLimits.remark}个字符`, trigger: 'blur' }]
}

/**
 * 提交审批弹窗校验规则。
 *
 * 实现步骤：
 * 1. 申请理由允许为空但必须限制最大长度；
 * 2. blur 时在文本域下方展示错误，避免用户去页面顶部找提示；
 * 3. 后端仍会再次校验，保证接口层和页面层一致。
 */
const approvalRules: FormRules = {
  applyReason: [{ max: fieldLimits.remark, message: `申请理由不能超过${fieldLimits.remark}个字符`, trigger: 'blur' }]
}

/**
 * 取消采购弹窗校验规则。
 *
 * 实现步骤：
 * 1. 取消类型和取消原因均必填；
 * 2. 取消原因使用统一备注长度限制；
 * 3. 校验失败时 Element Plus 自动把错误提示放到字段下方并标红输入框。
 */
const cancelRules: FormRules = {
  cancelType: [{ required: true, message: '请选择取消类型', trigger: 'change' }],
  cancelReason: [
    { required: true, message: '请输入取消原因', trigger: 'blur' },
    { max: fieldLimits.remark, message: `取消原因不能超过${fieldLimits.remark}个字符`, trigger: 'blur' }
  ]
}

/**
 * 常量 statusOptions：保存当前模块的页面状态、配置项、接口实例或计算结果。
 */
const statusOptions: Array<{ label: string; value: PurchaseStatus }> = [
  { label: '草稿', value: 'DRAFT' },
  { label: '审批中', value: 'IN_APPROVAL' },
  { label: '已审批【不同意】', value: 'APPROVAL_REJECTED' },
  { label: '已提交', value: 'SUBMITTED' },
  { label: '已审批【同意】', value: 'APPROVED' },
  { label: '采购中', value: 'PURCHASING' },
  { label: '采购完成', value: 'PURCHASE_COMPLETED' },
  { label: '已收货', value: 'RECEIVED' },
  { label: '已关闭', value: 'CLOSED' },
  { label: '取消采购', value: 'CANCELLED' }
]

/**
 * 常量 totalAmountCny：保存当前模块的页面状态、配置项、接口实例或计算结果。
 */
const totalAmountCny = computed(() => roundBusinessMoney(form.lines.reduce((sum, line) => sum + lineAmountCny(line), 0)))
/**
 * 常量 dialogTitle：保存当前模块的页面状态、配置项、接口实例或计算结果。
 */
const dialogTitle = computed(() => (readonly.value ? '采购单明细' : editingId.value ? '编辑采购单' : '新增采购单'))
/**
 * 常量 canSaveOrder：保存当前模块的页面状态、配置项、接口实例或计算结果。
 */
const canSaveOrder = computed(() => !readonly.value && (editingId.value ? auth.hasMenu('BTN_PURCHASE_EDIT') : auth.hasMenu('BTN_PURCHASE_CREATE')))
/**
 * 常量 canDeleteLine：保存当前模块的页面状态、配置项、接口实例或计算结果。
 */
const canDeleteLine = computed(() => !readonly.value && auth.hasMenu('BTN_PURCHASE_LINE_DELETE'))
/**
 * 常量 canManageAttachment：保存当前模块的页面状态、配置项、接口实例或计算结果。
 */
const canManageAttachment = computed(() => !readonly.value && auth.hasMenu('BTN_PURCHASE_ATTACHMENT'))

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
function currencyDisplay(row: PurchaseOrderView) {
  return row.currencyCode === 'MULTI' ? row.currencyName : row.currencyCode
}

/**
 * 空字段统一显示短横线，保持采购列表合并列的视觉密度稳定。
 */
function blankText(value: unknown) {
  const text = String(value ?? '').trim()
  return text || '-'
}

/**
 * 执行 rateDisplay 方法。
 * 
 * 实现步骤：
 * 1. 读取当前页面状态或调用参数；
 * 2. 完成对应的校验、接口调用或数据转换；
 * 3. 更新页面状态或返回处理结果。
 */
function rateDisplay(row: PurchaseOrderView) {
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
 * 1. 人民币固定返回 1；
 * 2. 非人民币优先保留当前明细已有正数汇率，避免接口返回前输入框为空；
 * 3. 当前无有效汇率时先填 1，后续参考汇率查询成功后覆盖。
 */

/**
 * 根据当前项目编码同步项目名称快照。
 *
 * 实现步骤：
 * 1. 从项目字典选项中定位当前项目；
 * 2. 选中项目时写入项目名称，清空项目时同步清空名称；
 * 3. 保存采购单时把编码和名称一起提交，便于列表和流水稳定展示。
 */
function onProjectChange() {
  form.projectName = projectOptions.value.find((item) => item.code === form.projectCode)?.name || ''
}

/**
 * 执行 lineAmount 方法。
 * 
 * 实现步骤：
 * 1. 读取当前页面状态或调用参数；
 * 2. 完成对应的校验、接口调用或数据转换；
 * 3. 更新页面状态或返回处理结果。
 */
function lineAmount(line: PurchaseLineForm) {
  return roundBusinessMoney(Number(line.quantity || 0) * Number(line.unitPrice || 0))
}

/**
 * 计算采购明细税额。
 *
 * 实现步骤：
 * 1. 使用数量乘单价得到不含税金额；
 * 2. 按明细税率计算税额；
 * 3. 统一按金额精度四舍五入，保持和后端计算口径一致。
 */
function lineTaxAmount(line: PurchaseLineForm) {
  return roundBusinessMoney(lineAmount(line) * Number(line.taxRate || 0))
}

/**
 * 计算采购明细价税合计。
 *
 * 实现步骤：
 * 1. 先计算明细不含税金额；
 * 2. 再计算明细税额；
 * 3. 返回两者相加后的展示金额。
 */
function lineAmountWithTax(line: PurchaseLineForm) {
  return roundBusinessMoney(lineAmount(line) + lineTaxAmount(line))
}

/**
 * 执行 lineAmountCny 方法。
 * 
 * 实现步骤：
 * 1. 读取当前页面状态或调用参数；
 * 2. 完成对应的校验、接口调用或数据转换；
 * 3. 更新页面状态或返回处理结果。
 */
function lineAmountCny(line: PurchaseLineForm) {
  return roundBusinessMoney(lineAmount(line) * Number(line.exchangeRateToCny || 1))
}

/**
 * 执行 onLineCurrencyChange 方法。
 * 
 * 实现步骤：
 * 1. 读取当前页面状态或调用参数；
 * 2. 完成对应的校验、接口调用或数据转换；
 * 3. 更新页面状态或返回处理结果。
 */
async function onLineCurrencyChange(line: PurchaseLineForm) {
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

/**
 * 执行 onLineMaterialChange 方法。
 * 
 * 实现步骤：
 * 1. 读取当前页面状态或调用参数；
 * 2. 完成对应的校验、接口调用或数据转换；
 * 3. 更新页面状态或返回处理结果。
 */
function onLineMaterialChange(line: PurchaseLineForm) {
  /**
   * 常量 selected：保存当前模块的页面状态、配置项、接口实例或计算结果。
   */
  const selected = materialOptions.value.find((item) => item.code === line.itemCode)
  line.itemName = selected?.name || ''
  line.specification = selected?.description || ''
  line.unitName = line.unitName || '件'
}

/**
 * 执行 statusLabel 方法。
 * 
 * 实现步骤：
 * 1. 读取当前页面状态或调用参数；
 * 2. 完成对应的校验、接口调用或数据转换；
 * 3. 更新页面状态或返回处理结果。
 */
function statusLabel(value: PurchaseStatus) {
  return statusOptions.find((item) => item.value === value)?.label || value
}

/**
 * 返回采购状态标签颜色。
 *
 * 实现步骤：
 * 1. 草稿和旧已提交显示普通颜色；
 * 2. 审批中和采购中显示警告色，提示流程或履约仍在进行；
 * 3. 已完成显示成功色，终止类状态显示信息色。
 */
function statusTagType(value: PurchaseStatus) {
  if (value === 'IN_APPROVAL' || value === 'PURCHASING') {
    return 'warning'
  }
  if (value === 'PURCHASE_COMPLETED' || value === 'RECEIVED') {
    return 'success'
  }
  if (value === 'CLOSED' || value === 'CANCELLED') {
    return 'info'
  }
  return ''
}

/**
 * 判断采购单是否允许编辑。
 *
 * 实现步骤：只有草稿和审批不同意允许返回编辑状态，其余流转状态只能查看。
 */
function canEditPurchase(value: PurchaseStatus) {
  return value === 'DRAFT' || value === 'APPROVAL_REJECTED'
}

/**
 * 判断采购单是否允许提交审批。
 *
 * 实现步骤：草稿和审批不同意都可以重新发起审批，审批中和终态不展示入口。
 */
function canSubmitApproval(value: PurchaseStatus) {
  return value === 'DRAFT' || value === 'APPROVAL_REJECTED'
}

/**
 * 判断采购单是否允许取消采购。
 *
 * 实现步骤：采购完成、已收货、已关闭和已取消属于终态，不再展示取消入口；其他状态保留取消能力。
 */
function canCancelPurchase(value: PurchaseStatus) {
  return !['PURCHASE_COMPLETED', 'RECEIVED', 'CLOSED', 'CANCELLED'].includes(value)
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
    const [orderRows, suppliers] = await Promise.all([
      api.purchaseOrders(purchaseSearchParams()),
      api.enabledDictionaryChildren('SUPPLIER')
    ])
    orders.value = orderRows
    selectedRows.value = []
    supplierOptions.value = suppliers
  } finally {
    loading.value = false
  }
}

/**
 * 执行 loadMaterialOptions 方法。
 * 
 * 实现步骤：
 * 1. 读取当前页面状态或调用参数；
 * 2. 完成对应的校验、接口调用或数据转换；
 * 3. 更新页面状态或返回处理结果。
 */
async function loadMaterialOptions() {
  materialOptions.value = await api.enabledDictionaryChildren('MATERIAL')
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
      id: -1,
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
 * 加载仓库基础资料。
 *
 * 实现步骤：
 * 1. 从基础字典读取启用的仓库子项；
 * 2. 保存到收货仓库下拉数据源；
 * 3. 仅展示字典名称，编码继续作为内部基础资料标识。
 */
async function loadWarehouseOptions() {
  warehouseOptions.value = await api.enabledDictionaryChildren('WAREHOUSE')
}

/**
 * 加载项目字典选项。
 *
 * 实现步骤：
 * 1. 从基础字典读取启用的 PROJECT 子项；
 * 2. 保存到项目下拉数据源；
 * 3. 采购查询、表单保存和查看流水共用该项目维度。
 */
async function loadProjectOptions() {
  projectOptions.value = await api.enabledDictionaryChildren('PROJECT')
}

/**
 * 加载采购表单业务字典。
 *
 * 实现步骤：
 * 1. 并行读取组织、部门、单据类型、业务类型、来源单据、付款条件、结算方式、交货条件和计量单位；
 * 2. 组织和部门保留树形层级缩进后展示，实际保存字典名称；
 * 3. 对关键默认值做兜底，避免旧环境尚未初始化新增字典时表单空白。
 */
async function loadBusinessDictionaryOptions() {
  const [
    organizations,
    departments,
    purchaseDocumentTypes,
    purchaseBusinessTypes,
    sourceBillTypes,
    paymentTerms,
    settlementMethods,
    deliveryTerms,
    units
  ] = await Promise.all([
    api.enabledDictionaryTree('ORGANIZATION'),
    api.enabledDictionaryTree('DEPARTMENT'),
    api.enabledDictionaryChildren('PURCHASE_DOCUMENT_TYPE'),
    api.enabledDictionaryChildren('PURCHASE_BUSINESS_TYPE'),
    api.enabledDictionaryChildren('SOURCE_BILL_TYPE'),
    api.enabledDictionaryChildren('PAYMENT_TERMS'),
    api.enabledDictionaryChildren('SETTLEMENT_METHOD'),
    api.enabledDictionaryChildren('DELIVERY_TERMS'),
    api.enabledDictionaryChildren('UNIT')
  ])
  organizationOptions.value = flattenDictionaryOptions(organizations)
  departmentOptions.value = flattenDictionaryOptions(departments)
  purchaseDocumentTypeOptions.value = withFallbackDictionaryOption(purchaseDocumentTypes, 'PURCHASE_DOC_STANDARD', '标准采购订单')
  purchaseBusinessTypeOptions.value = withFallbackDictionaryOption(purchaseBusinessTypes, 'PURCHASE_BIZ_STANDARD', '标准采购')
  sourceBillTypeOptions.value = sourceBillTypes
  paymentTermsOptions.value = paymentTerms
  settlementMethodOptions.value = settlementMethods
  deliveryTermsOptions.value = deliveryTerms
  unitOptions.value = withFallbackDictionaryOption(units, 'UNIT_PIECE', '件')
}

/**
 * 加载取消采购类型字典。
 *
 * 实现步骤：
 * 1. 优先从后端 PURCHASE_CANCEL_TYPE 字典读取启用子项；
 * 2. 当前环境未初始化字典时补一组前端兜底项；
 * 3. 取消采购弹窗只展示名称，编码继续留在基础资料中维护。
 */
async function loadPurchaseCancelTypeOptions() {
  const options = await api.enabledDictionaryChildren('PURCHASE_CANCEL_TYPE')
  purchaseCancelTypeOptions.value = options.length > 0
    ? options
    : [
      { id: -101, code: 'PURCHASE_CANCEL_BUSINESS', name: '业务取消', sortOrder: 1, enabled: true, hasChildren: false, children: [] },
      { id: -102, code: 'PURCHASE_CANCEL_SUPPLIER', name: '供应商原因', sortOrder: 2, enabled: true, hasChildren: false, children: [] },
      { id: -103, code: 'PURCHASE_CANCEL_PRICE', name: '价格变更', sortOrder: 3, enabled: true, hasChildren: false, children: [] },
      { id: -104, code: 'PURCHASE_CANCEL_OTHER', name: '其他', sortOrder: 4, enabled: true, hasChildren: false, children: [] }
    ]
}

/**
 * 给业务字典下拉补默认项。
 *
 * 实现步骤：
 * 1. 判断后端返回的字典是否已有指定名称；
 * 2. 已有时直接返回原列表；
 * 3. 没有时追加一个前端兜底选项，保证用户可以继续录入核心单据。
 */
/**
 * 执行 purchaseSearchParams 方法。
 * 
 * 实现步骤：
 * 1. 读取当前页面状态或调用参数；
 * 2. 完成对应的校验、接口调用或数据转换；
 * 3. 更新页面状态或返回处理结果。
 */
function purchaseSearchParams() {
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
     * 字段 orderNo：表示表单、筛选条件、接口数据或组件状态中的 orderNo 值。
     */
    orderNo: filters.orderNo.trim() || undefined,
    /**
     * 字段 supplierName：表示表单、筛选条件、接口数据或组件状态中的 supplierName 值。
     */
    supplierName: filters.supplierName || undefined,
    /**
     * 字段 projectCode：表示项目字典编码，用于按项目筛选采购单。
     */
    projectCode: filters.projectCode || undefined,
    /**
     * 字段 status：表示表单、筛选条件、接口数据或组件状态中的 status 值。
     */
    status: filters.status,
    /**
     * 字段 createdBy：表示表单、筛选条件、接口数据或组件状态中的 createdBy 值。
     */
    createdBy: filters.createdBy.trim() || undefined,
    /**
     * 字段 remark：表示表单、筛选条件、接口数据或组件状态中的 remark 值。
     */
    remark: filters.remark.trim() || undefined
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
     * 字段 orderNo：表示表单、筛选条件、接口数据或组件状态中的 orderNo 值。
     */
    orderNo: '',
    /**
     * 字段 supplierName：表示表单、筛选条件、接口数据或组件状态中的 supplierName 值。
     */
    supplierName: '',
    /**
     * 字段 projectCode：表示项目字典编码，用于清空项目筛选条件。
     */
    projectCode: '',
    /**
     * 字段 status：表示表单、筛选条件、接口数据或组件状态中的 status 值。
     */
    status: undefined,
    /**
     * 字段 createdBy：表示表单、筛选条件、接口数据或组件状态中的 createdBy 值。
     */
    createdBy: '',
    /**
     * 字段 remark：表示表单、筛选条件、接口数据或组件状态中的 remark 值。
     */
    remark: ''
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
function handleSelectionChange(selection: PurchaseOrderView[]) {
  selectedRows.value = selection
}

/**
 * 导出采购单列表。
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
      : purchaseSearchParams()
    const { blob, filename } = await api.exportPurchaseOrders(payload)
    saveBlob(blob, filename || '采购管理.xlsx')
    ElMessage.success('导出成功')
  } finally {
    exporting.value = false
  }
}

/**
 * 执行 reset 方法。
 * 
 * 实现步骤：
 * 1. 读取当前页面状态或调用参数；
 * 2. 完成对应的校验、接口调用或数据转换；
 * 3. 更新页面状态或返回处理结果。
 */
function reset() {
  Object.assign(form, {
    /**
     * 字段 supplierName：表示表单、筛选条件、接口数据或组件状态中的 supplierName 值。
     */
    supplierName: '',
    /**
     * 字段 documentType：表示表单、筛选条件、接口数据或组件状态中的 documentType 值。
     */
    documentType: '标准采购订单',
    /**
     * 字段 businessType：表示表单、筛选条件、接口数据或组件状态中的 businessType 值。
     */
    businessType: '标准采购',
    /**
     * 字段 projectCode：表示项目字典编码，新增采购单时默认为空。
     */
    projectCode: '',
    /**
     * 字段 projectName：表示项目名称快照，新增采购单时默认为空。
     */
    projectName: '',
    /**
     * 字段 purchaseOrganization：表示表单、筛选条件、接口数据或组件状态中的 purchaseOrganization 值。
     */
    purchaseOrganization: '',
    /**
     * 字段 purchaseDepartment：表示表单、筛选条件、接口数据或组件状态中的 purchaseDepartment 值。
     */
    purchaseDepartment: '',
    /**
     * 字段 purchaserName：表示表单、筛选条件、接口数据或组件状态中的 purchaserName 值。
     */
    purchaserName: '',
    /**
     * 字段 settlementOrganization：表示表单、筛选条件、接口数据或组件状态中的 settlementOrganization 值。
     */
    settlementOrganization: '',
    /**
     * 字段 paymentTerms：表示表单、筛选条件、接口数据或组件状态中的 paymentTerms 值。
     */
    paymentTerms: '',
    /**
     * 字段 settlementMethod：表示表单、筛选条件、接口数据或组件状态中的 settlementMethod 值。
     */
    settlementMethod: '',
    /**
     * 字段 deliveryTerms：表示表单、筛选条件、接口数据或组件状态中的 deliveryTerms 值。
     */
    deliveryTerms: '',
    /**
     * 字段 sourceBillType：表示表单、筛选条件、接口数据或组件状态中的 sourceBillType 值。
     */
    sourceBillType: '',
    /**
     * 字段 sourceBillNo：表示表单、筛选条件、接口数据或组件状态中的 sourceBillNo 值。
     */
    sourceBillNo: '',
    /**
     * 字段 orderDate：表示表单、筛选条件、接口数据或组件状态中的 orderDate 值。
     */
    orderDate: new Date().toISOString().slice(0, 10),
    /**
     * 字段 remark：表示表单、筛选条件、接口数据或组件状态中的 remark 值。
     */
    remark: '',
    /**
     * 字段 lines：表示表单、筛选条件、接口数据或组件状态中的 lines 值。
     */
    lines: [newLine()]
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
  editingId.value = undefined
  readonly.value = false
  attachmentRef.value?.reset()
  reset()
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
function fill(row: PurchaseOrderView) {
  Object.assign(form, {
    /**
     * 字段 supplierName：表示表单、筛选条件、接口数据或组件状态中的 supplierName 值。
     */
    supplierName: row.supplierName,
    /**
     * 字段 documentType：表示表单、筛选条件、接口数据或组件状态中的 documentType 值。
     */
    documentType: row.documentType || '标准采购订单',
    /**
     * 字段 businessType：表示表单、筛选条件、接口数据或组件状态中的 businessType 值。
     */
    businessType: row.businessType || '标准采购',
    /**
     * 字段 projectCode：表示项目字典编码，用于回填采购单所属项目。
     */
    projectCode: row.projectCode || '',
    /**
     * 字段 projectName：表示项目名称快照，用于回填采购单所属项目名称。
     */
    projectName: row.projectName || '',
    /**
     * 字段 purchaseOrganization：表示表单、筛选条件、接口数据或组件状态中的 purchaseOrganization 值。
     */
    purchaseOrganization: row.purchaseOrganization || '',
    /**
     * 字段 purchaseDepartment：表示表单、筛选条件、接口数据或组件状态中的 purchaseDepartment 值。
     */
    purchaseDepartment: row.purchaseDepartment || '',
    /**
     * 字段 purchaserName：表示表单、筛选条件、接口数据或组件状态中的 purchaserName 值。
     */
    purchaserName: row.purchaserName || '',
    /**
     * 字段 settlementOrganization：表示表单、筛选条件、接口数据或组件状态中的 settlementOrganization 值。
     */
    settlementOrganization: row.settlementOrganization || '',
    /**
     * 字段 paymentTerms：表示表单、筛选条件、接口数据或组件状态中的 paymentTerms 值。
     */
    paymentTerms: row.paymentTerms || '',
    /**
     * 字段 settlementMethod：表示表单、筛选条件、接口数据或组件状态中的 settlementMethod 值。
     */
    settlementMethod: row.settlementMethod || '',
    /**
     * 字段 deliveryTerms：表示表单、筛选条件、接口数据或组件状态中的 deliveryTerms 值。
     */
    deliveryTerms: row.deliveryTerms || '',
    /**
     * 字段 sourceBillType：表示表单、筛选条件、接口数据或组件状态中的 sourceBillType 值。
     */
    sourceBillType: row.sourceBillType || '',
    /**
     * 字段 sourceBillNo：表示表单、筛选条件、接口数据或组件状态中的 sourceBillNo 值。
     */
    sourceBillNo: row.sourceBillNo || '',
    /**
     * 字段 orderDate：表示表单、筛选条件、接口数据或组件状态中的 orderDate 值。
     */
    orderDate: row.orderDate,
    /**
     * 字段 remark：表示表单、筛选条件、接口数据或组件状态中的 remark 值。
     */
    remark: row.remark || '',
    /**
     * 字段 lines：表示表单、筛选条件、接口数据或组件状态中的 lines 值。
     */
    lines: row.lines.map((line) => ({
      /**
       * 字段 itemCode：表示表单、筛选条件、接口数据或组件状态中的 itemCode 值。
       */
      itemCode: line.itemCode,
      /**
       * 字段 itemName：表示表单、筛选条件、接口数据或组件状态中的 itemName 值。
       */
      itemName: line.itemName,
      /**
       * 字段 specification：表示表单、筛选条件、接口数据或组件状态中的 specification 值。
       */
      specification: line.specification || '',
      /**
       * 字段 unitName：表示表单、筛选条件、接口数据或组件状态中的 unitName 值。
       */
      unitName: line.unitName || '',
      /**
       * 字段 quantity：表示表单、筛选条件、接口数据或组件状态中的 quantity 值。
       */
      quantity: Number(line.quantity),
      /**
       * 字段 unitPrice：表示表单、筛选条件、接口数据或组件状态中的 unitPrice 值。
       */
      unitPrice: Number(line.unitPrice),
      /**
       * 字段 taxRate：表示表单、筛选条件、接口数据或组件状态中的 taxRate 值。
       */
      taxRate: Number(line.taxRate || 0),
      /**
       * 字段 plannedArrivalDate：表示表单、筛选条件、接口数据或组件状态中的 plannedArrivalDate 值。
       */
      plannedArrivalDate: line.plannedArrivalDate || '',
      /**
       * 字段 receiveWarehouse：表示表单、筛选条件、接口数据或组件状态中的 receiveWarehouse 值。
       */
      receiveWarehouse: line.receiveWarehouse || '',
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
      exchangeRateToCny: Number(line.exchangeRateToCny || row.exchangeRateToCny || 1)
    }))
  })
}

/**
 * 执行 view 方法。
 * 
 * 实现步骤：
 * 1. 读取当前页面状态或调用参数；
 * 2. 完成对应的校验、接口调用或数据转换；
 * 3. 更新页面状态或返回处理结果。
 */
async function view(row: PurchaseOrderView) {
  await refreshDictionaryOptions()
  /**
   * 常量 detail：保存当前模块的页面状态、配置项、接口实例或计算结果。
   */
  const detail = await api.purchaseOrder(row.id)
  editingId.value = row.id
  readonly.value = true
  fill(detail)
  await attachmentRef.value?.reload(row.id)
  dialogVisible.value = true
}

/**
 * 执行 openEdit 方法。
 * 
 * 实现步骤：
 * 1. 读取当前页面状态或调用参数；
 * 2. 完成对应的校验、接口调用或数据转换；
 * 3. 更新页面状态或返回处理结果。
 */
async function openEdit(row: PurchaseOrderView) {
  await refreshDictionaryOptions()
  /**
   * 常量 detail：保存当前模块的页面状态、配置项、接口实例或计算结果。
   */
  const detail = await api.purchaseOrder(row.id)
  editingId.value = row.id
  readonly.value = false
  fill(detail)
  await attachmentRef.value?.reload(row.id)
  dialogVisible.value = true
}

/**
 * 打开提交审批弹窗。
 *
 * 实现步骤：
 * 1. 后端读取最新采购单详情，避免列表旧数据导致状态判断不准；
 * 2. 先做页面侧提交审批前置校验，必填和明细完整性不满足时直接提示；
 * 3. 初始化申请理由后显示弹窗，用户确认后再真正发起流程。
 */
async function openApproval(row: PurchaseOrderView) {
  const detail = await api.purchaseOrder(row.id)
  const error = validatePurchaseBeforeApproval(detail)
  if (error) {
    ElMessage.warning(error)
    return
  }
  approvalTarget.value = detail
  approvalForm.applyReason = ''
  approvalVisible.value = true
}

/**
 * 提交采购审批。
 *
 * 实现步骤：
 * 1. 校验申请理由文本域长度；
 * 2. 调用采购审批接口，后端负责流程实例创建和采购状态事务更新；
 * 3. 成功后关闭弹窗并刷新列表，失败时由统一接口提示承载错误。
 */
async function submitApproval() {
  const valid = await approvalFormRef.value?.validate().catch(() => false)
  if (!valid || !approvalTarget.value) {
    return
  }
  await api.submitPurchaseApproval(approvalTarget.value.id, {
    applyReason: approvalForm.applyReason.trim() || undefined
  })
  ElMessage.success('采购审批已提交')
  approvalVisible.value = false
  await load()
}

/**
 * 采购审批同意后发起采购。
 *
 * 实现步骤：调用后端状态机进入采购中，成功后刷新列表并保留操作流水。
 */
async function startPurchase(row: PurchaseOrderView) {
  await api.startPurchase(row.id)
  ElMessage.success('采购已发起')
  await load()
}

/**
 * 确认采购已收货。
 *
 * 实现步骤：
 * 1. 调用后端状态机把采购中推进为采购完成；
 * 2. 刷新采购列表；
 * 3. 弹出库存台账录入确认，用户确认后带采购信息跳转库存台账新增流水。
 */
async function receivePurchase(row: PurchaseOrderView) {
  await api.receivePurchase(row.id)
  ElMessage.success('采购已收货')
  await load()
  try {
    await ElMessageBox.confirm('采购单已收货，是否进行库存台账录入？', '库存台账录入', {
      type: 'info',
      confirmButtonText: '是',
      cancelButtonText: '否'
    })
    const detail = await api.purchaseOrder(row.id)
    await router.push({ path: '/inventory', query: buildInventoryCreateQuery(detail) })
  } catch (error) {
    if (error !== 'cancel' && error !== 'close') {
      throw error
    }
  }
}

/**
 * 打开取消采购弹窗。
 *
 * 实现步骤：
 * 1. 记录当前待取消采购单；
 * 2. 清空取消类型和取消原因；
 * 3. 展示弹窗，由字段级校验引导用户补齐取消信息。
 */
function openCancelPurchase(row: PurchaseOrderView) {
  cancelTarget.value = row
  Object.assign(cancelForm, { cancelType: '', cancelReason: '' })
  cancelVisible.value = true
}

/**
 * 提交取消采购。
 *
 * 实现步骤：
 * 1. 校验取消类型和取消原因；
 * 2. 调用后端取消采购接口，后端同步取消运行中的审批流程；
 * 3. 成功后关闭弹窗并刷新列表。
 */
async function cancelPurchase() {
  const valid = await cancelFormRef.value?.validate().catch(() => false)
  if (!valid || !cancelTarget.value) {
    return
  }
  await api.cancelPurchase(cancelTarget.value.id, {
    cancelType: cancelForm.cancelType,
    cancelReason: cancelForm.cancelReason.trim()
  })
  ElMessage.success('采购已取消')
  cancelVisible.value = false
  await load()
}

/**
 * 页面侧校验采购单是否满足提交审批条件。
 *
 * 实现步骤：
 * 1. 校验采购头关键字段；
 * 2. 校验至少存在一条采购明细；
 * 3. 校验每条明细的物料、数量、单价和汇率，提前减少无效审批发起。
 */
function validatePurchaseBeforeApproval(order: PurchaseOrderView) {
  if (!canSubmitApproval(order.status)) {
    return '仅草稿或审批不同意的采购单允许提交审批'
  }
  if (!order.supplierName) {
    return '请选择供应商'
  }
  if (!order.orderDate) {
    return '请选择采购日期'
  }
  if (!order.lines || order.lines.length === 0) {
    return '请至少维护一条采购明细'
  }
  for (const [index, line] of order.lines.entries()) {
    const rowNo = index + 1
    if (!line.itemCode || !line.itemName) {
      return `第${rowNo}行采购明细未选择物料`
    }
    if (Number(line.quantity || 0) <= 0) {
      return `第${rowNo}行采购数量必须大于0`
    }
    if (Number(line.unitPrice || 0) <= 0) {
      return `第${rowNo}行采购单价必须大于0`
    }
    if (Number(line.exchangeRateToCny || 0) <= 0) {
      return `第${rowNo}行汇率必须大于0`
    }
  }
  return ''
}

/**
 * 执行 openOperationLogs 方法。
 * 
 * 实现步骤：
 * 1. 读取当前页面状态或调用参数；
 * 2. 完成对应的校验、接口调用或数据转换；
 * 3. 更新页面状态或返回处理结果。
 */
async function openOperationLogs(row: PurchaseOrderView) {
  operationLogDrawerRef.value?.open({
    /**
     * 字段 title：表示表单、筛选条件、接口数据或组件状态中的 title 值。
     */
    title: `${row.orderNo} 采购流水`,
    /**
     * 字段 load：表示表单、筛选条件、接口数据或组件状态中的 load 值。
     */
    load: (params) => api.purchaseOrderOperationLogs(row.id, params)
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
  form.lines.push(newLine())
}

/**
 * 构造新的采购明细行。
 *
 * 实现步骤：
 * 1. 初始化物料、规格、单位和收货仓库为空；
 * 2. 默认数量和单价为 1，税率按常见 13% 初始化；
 * 3. 默认币种为人民币，汇率固定为 1。
 */
function newLine(): PurchaseLineForm {
  return {
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
     * 字段 quantity：表示表单、筛选条件、接口数据或组件状态中的 quantity 值。
     */
    quantity: 1,
    /**
     * 字段 unitPrice：表示表单、筛选条件、接口数据或组件状态中的 unitPrice 值。
     */
    unitPrice: 1,
    /**
     * 字段 taxRate：表示表单、筛选条件、接口数据或组件状态中的 taxRate 值。
     */
    taxRate: 0.13,
    /**
     * 字段 plannedArrivalDate：表示表单、筛选条件、接口数据或组件状态中的 plannedArrivalDate 值。
     */
    plannedArrivalDate: '',
    /**
     * 字段 receiveWarehouse：表示表单、筛选条件、接口数据或组件状态中的 receiveWarehouse 值。
     */
    receiveWarehouse: '',
    /**
     * 字段 currencyCode：表示表单、筛选条件、接口数据或组件状态中的 currencyCode 值。
     */
    currencyCode: 'CNY',
    /**
     * 字段 currencyName：表示表单、筛选条件、接口数据或组件状态中的 currencyName 值。
     */
    currencyName: '人民币',
    /**
     * 字段 exchangeRateToCny：表示表单、筛选条件、接口数据或组件状态中的 exchangeRateToCny 值。
     */
    exchangeRateToCny: 1
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
  /** 采购单表单校验结果，失败时字段下方显示错误并阻止保存。 */
  const valid = await formRef.value?.validate().catch(() => false)
  if (!valid) {
    return
  }
  if (form.lines.some((line) => !line.itemCode || !line.itemName)) {
    ElMessage.warning('请完善采购单信息')
    return
  }
  /**
   * 常量 payload：保存当前模块的页面状态、配置项、接口实例或计算结果。
   */
  const payload = {
    ...form,
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
  let saved: PurchaseOrderView
  if (editingId.value) {
    saved = await api.updatePurchaseOrder(editingId.value, payload)
  } else {
    saved = await api.createPurchaseOrder(payload)
  }
  await attachmentRef.value?.uploadPending(saved.id)
  ElMessage.success('保存成功')
  dialogVisible.value = false
  await load()
}

/**
 * 从采购单跳转查看在线凭证。
 *
 * 实现步骤：
 * 1. 接收采购单已关联的凭证号；
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
 * 构造采购收货生成库存流水的路由参数。
 *
 * 实现步骤：
 * 1. 固定库存流水类型为入库，日期默认为当天；
 * 2. 采购单号写入关联单号，采购组织、项目、供应商写入库存组织、项目和货主；
 * 3. 首条采购明细带出物料、规格、单位、数量和收货仓库；
 * 4. 过滤空值，避免库存页把空字符串误当成有效预填值。
 */
function buildInventoryCreateQuery(order: PurchaseOrderView) {
  /** 采购单首条明细，用于预填库存流水的物料、数量、单位和仓库。 */
  const firstLine = order.lines?.[0]
  const query: Record<string, string> = {
    action: 'createInventoryLedger',
    source: 'purchaseReceived',
    movementType: 'INBOUND',
    movementDate: new Date().toISOString().slice(0, 10),
    relatedBizNo: order.orderNo,
    sourceBillType: '采购入库单',
    projectCode: order.projectCode || '',
    projectName: order.projectName || '',
    stockOrganization: order.purchaseOrganization || '',
    ownerName: order.supplierName || '',
    itemCode: firstLine?.itemCode || '',
    itemName: firstLine?.itemName || '',
    specification: firstLine?.specification || '',
    unitName: firstLine?.unitName || '',
    quantity: firstLine?.quantity === undefined ? '' : String(firstLine.quantity),
    toWarehouse: firstLine?.receiveWarehouse || '',
    remark: order.orderNo ? `采购单${order.orderNo}收货入库` : '采购收货入库'
  }
  return Object.fromEntries(Object.entries(query).filter(([, value]) => value !== ''))
}

/**
 * 批量删除采购单。
 *
 * 实现步骤：
 * 1. 校验是否已经勾选采购单；
 * 2. 弹出二次确认，避免误删采购业务数据；
 * 3. 调用后端批量删除接口；
 * 4. 删除成功后刷新列表。
 */
async function batchRemove() {
  if (selectedRows.value.length === 0) {
    ElMessage.warning('请选择需要删除的采购单')
    return
  }
  await ElMessageBox.confirm(`确认删除选中的 ${selectedRows.value.length} 张采购单？`, '批量删除确认', {
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
  await api.batchDeletePurchaseOrders(selectedRows.value.map((row) => row.id))
  ElMessage.success('批量删除成功')
  await load()
}

onMounted(async () => {
  applyRouteQuery()
  await Promise.all([load(), refreshDictionaryOptions()])
})

/**
 * 重新读取采购页面使用的全部基础字典。
 *
 * 实现步骤：
 * 1. 并行请求币种、物料、仓库、项目、采购表单字典和取消类型；
 * 2. HTTP 层会给 GET 请求追加防缓存参数，确保拿到后端最新基础资料；
 * 3. 页面被 keep-alive 重新激活或打开弹窗时调用，保证基础信息修改后立即生效。
 */
async function refreshDictionaryOptions() {
  await Promise.all([
    loadCurrencies(),
    loadMaterialOptions(),
    loadWarehouseOptions(),
    loadProjectOptions(),
    loadBusinessDictionaryOptions(),
    loadPurchaseCancelTypeOptions()
  ])
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
   * 常量 orderNo：保存当前模块的页面状态、配置项、接口实例或计算结果。
   */
  const orderNo = queryString(route.query.orderNo)
  if (orderNo && filters.orderNo !== orderNo) {
    filters.orderNo = orderNo
    return true
  }
  return false
}

</script>

<style scoped>
.total-bar {
  display: flex;
  align-items: center;
  justify-content: flex-end;
  gap: 12px;
  margin: 12px 0;
  color: #374151;
  font-weight: 700;
}

.approval-status {
  display: flex;
  align-items: center;
  flex-wrap: wrap;
  gap: 6px;
}

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
  gap: 12px;
  margin-bottom: 12px;
  padding-bottom: 8px;
  border-bottom: 1px solid #e5e7eb;
  color: #111827;
  font-size: 14px;
  font-weight: 700;
}

</style>
