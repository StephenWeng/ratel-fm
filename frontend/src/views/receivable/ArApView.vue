<template>
  <div class="page">
    <div class="page-header">
      <div><h1 class="page-title">应收应付</h1><p class="page-subtitle">维护客户/供应商账龄和付款计划。</p></div>
    </div>
    <el-tabs v-model="activeTab" class="ar-ap-tabs" @tab-change="handleTabChange">
      <el-tab-pane label="应收应付单据" name="bills">
    <el-form class="filter-form" :model="filters" label-width="92px">
      <el-row :gutter="12">
        <el-col :xs="24" :sm="12" :md="8" :lg="6">
          <el-form-item label="到期日期">
            <el-date-picker v-model="filters.dateRange" type="daterange" value-format="YYYY-MM-DD" start-placeholder="开始日期" end-placeholder="结束日期" class="full" />
          </el-form-item>
        </el-col>
        <el-col :xs="24" :sm="12" :md="8" :lg="5">
          <el-form-item label="单号">
            <el-input v-model="filters.billNo" clearable placeholder="模糊查询单号" />
          </el-form-item>
        </el-col>
        <el-col :xs="24" :sm="12" :md="8" :lg="4">
          <el-form-item label="类型">
            <el-select v-model="filters.billType" clearable class="full" placeholder="全部">
              <el-option label="应收" value="RECEIVABLE" />
              <el-option label="应付" value="PAYABLE" />
            </el-select>
          </el-form-item>
        </el-col>
        <el-col :xs="24" :sm="12" :md="8" :lg="5">
          <el-form-item label="客户/供应商">
            <el-select v-model="filters.partnerName" clearable filterable class="full" placeholder="精确选择往来单位">
              <el-option v-for="item in partnerOptions" :key="item.id" :label="item.name" :value="item.name" />
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
              <el-option label="未结" value="OPEN" />
              <el-option label="部分结清" value="PARTIAL" />
              <el-option label="已结清" value="CLOSED" />
              <el-option label="逾期" value="OVERDUE" />
            </el-select>
          </el-form-item>
        </el-col>
        <el-col :xs="24" :sm="12" :md="8" :lg="5">
          <el-form-item label="付款计划">
            <el-input v-model="filters.paymentPlan" clearable placeholder="模糊查询付款计划" />
          </el-form-item>
        </el-col>
        <el-col :xs="24" :sm="12" :md="8" :lg="6">
          <el-form-item label=" " class="filter-actions">
            <el-button type="primary" @click="load">查询</el-button>
            <el-button @click="resetFilters">重置</el-button>
            <el-button v-if="auth.hasMenu('BTN_AR_AP_CREATE')" type="primary" :icon="Plus" @click="openCreate">新增单据</el-button>
            <el-button v-if="auth.hasMenu('BTN_AR_AP_BATCH_DELETE') && selectedRows.length > 0" type="danger" :icon="Delete" @click="batchRemove">批量删除</el-button>
            <el-button v-if="auth.hasMenu('BTN_AR_AP_EXPORT')" :icon="Download" :loading="exporting" @click="exportRows">导出</el-button>
          </el-form-item>
        </el-col>
      </el-row>
    </el-form>
    <div class="panel">
      <el-table v-loading="loading" :data="rows" stripe @selection-change="handleSelectionChange">
        <el-table-column type="selection" width="48" />
        <el-table-column label="单据信息" min-width="190">
          <template #default="{ row }">
            <div class="stacked-cell">
              <div class="stacked-cell__line"><span class="stacked-cell__label">单号：</span>{{ row.billNo || '' }}</div>
              <div class="stacked-cell__line"><span class="stacked-cell__label">类型：</span>{{ row.billType ? billTypeLabel(row.billType) : '' }}</div>
              <div class="stacked-cell__line"><span class="stacked-cell__label">单据类型：</span>{{ row.documentType || '' }}</div>
            </div>
          </template>
        </el-table-column>
        <el-table-column prop="partnerName" label="客户/供应商" min-width="180" />
        <el-table-column prop="projectName" label="项目" min-width="140" />
        <el-table-column prop="settlementOrganization" label="结算组织" min-width="130" />
        <el-table-column prop="dueDate" label="到期日" width="120" />
        <el-table-column prop="amount" label="金额" width="140" align="right">
          <template #default="{ row }"><AmountText :value="row.amount" :currency-code="row.currencyCode" :currency-name="row.currencyName" /></template>
        </el-table-column>
        <el-table-column prop="remainingAmount" label="未结金额" width="140" align="right">
          <template #default="{ row }"><AmountText :value="row.remainingAmount" :currency-code="row.currencyCode" :currency-name="row.currencyName" /></template>
        </el-table-column>
        <el-table-column label="币种/汇率" width="150">
          <template #default="{ row }">
            <div class="stacked-cell">
              <div class="stacked-cell__line"><span class="stacked-cell__label">币种：</span>{{ row.currencyName || row.currencyCode || '' }}</div>
              <div class="stacked-cell__line"><span class="stacked-cell__label">汇率：</span>{{ row.exchangeRateToCny || '' }}</div>
            </div>
          </template>
        </el-table-column>
        <el-table-column prop="remainingAmountCny" label="未结人民币" width="130" align="right">
          <template #default="{ row }"><AmountText :value="row.remainingAmountCny" currency-code="CNY" currency-name="人民币" /></template>
        </el-table-column>
        <el-table-column prop="agingDays" label="账龄天数" width="100" />
        <el-table-column label="状态" width="110">
          <template #default="{ row }">
            <el-tag :type="statusTagType(row.status)">{{ statusLabel(row.status) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="paymentPlan" label="付款计划" min-width="220" />
        <el-table-column label="操作" width="280" fixed="right">
          <template #default="{ row }">
            <div class="table-actions">
              <el-button v-if="auth.hasMenu('BTN_AR_AP_SETTLE')" size="small" type="primary" @click="openSettlement(row)">{{ settleActionLabel(row) }}</el-button>
              <el-button v-if="auth.hasMenu('BTN_AR_AP_ATTACHMENT') && (row.attachmentCount || 0) > 0" size="small" @click="openAttachment(row)">附件</el-button>
              <el-button v-if="auth.hasMenu('BTN_VOUCHER_VIEW') && row.voucherId && row.voucherNo" size="small" @click="openOnlineVoucher(row.voucherNo)">在线凭证</el-button>
              <el-button size="small" @click="openOperationLogs(row)">查看流水</el-button>
            </div>
          </template>
        </el-table-column>
      </el-table>
    </div>
      </el-tab-pane>
      <el-tab-pane label="收付统计" name="paymentStats">
        <el-form class="filter-form" :model="statsFilters" label-width="96px">
          <el-row :gutter="12">
            <el-col :xs="24" :sm="12" :md="8" :lg="5">
              <el-form-item label="项目">
                <el-select v-model="statsFilters.projectCode" clearable filterable class="full" placeholder="全部项目">
                  <el-option v-for="item in projectOptions" :key="item.id" :label="item.name" :value="item.code" />
                </el-select>
              </el-form-item>
            </el-col>
            <el-col :xs="24" :sm="12" :md="8" :lg="5">
              <el-form-item label="客户/供应商">
                <el-select v-model="statsFilters.partnerName" clearable filterable class="full" placeholder="全部往来单位">
                  <el-option v-for="item in partnerOptions" :key="item.id" :label="item.name" :value="item.name" />
                </el-select>
              </el-form-item>
            </el-col>
            <el-col :xs="24" :sm="12" :md="10" :lg="8">
              <el-form-item label=" " class="filter-actions">
                <el-button v-if="canQueryPaymentStats" type="primary" :icon="Search" @click="loadPaymentStats">查询</el-button>
                <el-button @click="resetPaymentStatsFilters">重置</el-button>
                <el-button v-if="canExportPaymentStats" :icon="Download" :loading="paymentStatsExporting" @click="exportPaymentStatsRows">导出</el-button>
              </el-form-item>
            </el-col>
          </el-row>
        </el-form>

        <div class="payment-stats-groups">
          <div class="payment-stats-group">
            <div class="payment-stats-group__title">付款统计</div>
            <div class="payment-stats-grid">
              <div class="payment-stat-card">
                <span>应付合计</span>
                <strong><AmountText :value="paymentStats.totalPayableAmount" currency-code="CNY" currency-name="人民币" /></strong>
              </div>
              <div class="payment-stat-card">
                <span>已付合计</span>
                <strong><AmountText :value="paymentStats.totalPaidAmount" currency-code="CNY" currency-name="人民币" /></strong>
              </div>
              <div class="payment-stat-card">
                <span>待付合计</span>
                <strong><AmountText :value="paymentStats.totalPendingPayableAmount" currency-code="CNY" currency-name="人民币" /></strong>
              </div>
            </div>
          </div>
          <div class="payment-stats-group">
            <div class="payment-stats-group__title">收款统计</div>
            <div class="payment-stats-grid">
              <div class="payment-stat-card">
                <span>应收合计</span>
                <strong><AmountText :value="paymentStats.totalReceivableAmount" currency-code="CNY" currency-name="人民币" /></strong>
              </div>
              <div class="payment-stat-card">
                <span>已收合计</span>
                <strong><AmountText :value="paymentStats.totalReceivedAmount" currency-code="CNY" currency-name="人民币" /></strong>
              </div>
              <div class="payment-stat-card">
                <span>待收合计</span>
                <strong><AmountText :value="paymentStats.totalPendingReceivableAmount" currency-code="CNY" currency-name="人民币" /></strong>
              </div>
            </div>
          </div>
        </div>

        <div class="panel">
          <el-table v-loading="paymentStatsLoading" :data="paymentStats.rows" stripe show-summary :summary-method="paymentStatsSummaryMethod">
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
      </el-tab-pane>
    </el-tabs>
    <el-dialog v-model="dialogVisible" title="新增应收应付单" width="min(1120px, 92vw)" top="5vh">
      <el-form ref="formRef" :model="form" :rules="rules" label-width="106px">
        <section class="business-form-section">
        <div class="section-heading"><span>基本信息</span></div>
        <el-row :gutter="12">
          <el-col :xs="24" :sm="8"><el-form-item label="类型"><el-select v-model="form.billType" class="full" @change="onBillTypeChange"><el-option label="应收" value="RECEIVABLE" /><el-option label="应付" value="PAYABLE" /></el-select></el-form-item></el-col>
          <el-col :xs="24" :sm="8"><el-form-item label="单据类型"><el-select v-model="form.documentType" filterable class="full"><el-option v-for="item in arApDocumentTypeOptions" :key="item.id" :label="item.name" :value="item.name" /></el-select></el-form-item></el-col>
          <el-col :xs="24" :sm="8"><el-form-item label="项目" prop="projectCode"><el-select v-model="form.projectCode" clearable filterable class="full" placeholder="请选择项目" @change="onProjectChange"><el-option v-for="item in projectOptions" :key="item.id" :label="item.name" :value="item.code" /></el-select></el-form-item></el-col>
          <el-col :xs="24" :sm="8">
            <el-form-item label="客户/供应商" prop="partnerName">
              <el-select v-model="form.partnerName" filterable class="full" placeholder="请选择客户/供应商">
                <el-option v-for="item in partnerOptions" :key="item.id" :label="item.name" :value="item.name" />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :xs="24" :sm="8"><el-form-item label="单据日期"><el-date-picker v-model="form.billDate" value-format="YYYY-MM-DD" class="full" /></el-form-item></el-col>
          <el-col :xs="24" :sm="8"><el-form-item label="到期日"><el-date-picker v-model="form.dueDate" value-format="YYYY-MM-DD" class="full" /></el-form-item></el-col>
          <el-col :xs="24" :sm="8"><el-form-item label="来源类型"><el-select v-model="form.sourceBillType" clearable filterable class="full"><el-option v-for="item in sourceBillTypeOptions" :key="item.id" :label="item.name" :value="item.name" /></el-select></el-form-item></el-col>
          <el-col :xs="24" :sm="8"><el-form-item label="来源单号" prop="sourceBillNo"><el-input v-model="form.sourceBillNo" :maxlength="fieldLimits.sourceBillNo" show-word-limit /></el-form-item></el-col>
        </el-row>
        </section>
        <section class="business-form-section">
        <div class="section-heading"><span>组织与结算</span></div>
        <el-row :gutter="12">
          <el-col :xs="24" :sm="8"><el-form-item label="业务组织"><el-select v-model="form.businessOrganization" clearable filterable class="full"><el-option v-for="item in organizationOptions" :key="item.value" :label="item.label" :value="item.value" /></el-select></el-form-item></el-col>
          <el-col :xs="24" :sm="8"><el-form-item label="结算组织"><el-select v-model="form.settlementOrganization" clearable filterable class="full"><el-option v-for="item in organizationOptions" :key="item.value" :label="item.label" :value="item.value" /></el-select></el-form-item></el-col>
          <el-col :xs="24" :sm="8"><el-form-item label="收付款组织"><el-select v-model="form.paymentOrganization" clearable filterable class="full"><el-option v-for="item in organizationOptions" :key="item.value" :label="item.label" :value="item.value" /></el-select></el-form-item></el-col>
          <el-col :xs="24" :sm="8"><el-form-item label="收付款条件"><el-select v-model="form.paymentTerms" clearable filterable class="full"><el-option v-for="item in paymentTermsOptions" :key="item.id" :label="item.name" :value="item.name" /></el-select></el-form-item></el-col>
          <el-col :xs="24" :sm="8"><el-form-item label="结算方式"><el-select v-model="form.settlementMethod" clearable filterable class="full"><el-option v-for="item in settlementMethodOptions" :key="item.id" :label="item.name" :value="item.name" /></el-select></el-form-item></el-col>
        </el-row>
        </section>
        <section class="business-form-section">
        <div class="section-heading"><span>金额与计划</span></div>
        <el-row :gutter="12">
          <el-col :xs="24" :sm="10">
            <el-form-item label="金额">
              <div class="money-input">
                <el-input-number v-model="form.amount" :min="0.00000001" :precision="8" :controls="false" class="money-number" />
                <el-select v-model="form.currencyCode" filterable class="money-currency" @change="onCurrencyChange">
                  <el-option v-for="item in currencyOptions" :key="item.code" :label="item.code" :value="item.code" />
                </el-select>
              </div>
            </el-form-item>
          </el-col>
          <el-col :xs="24" :sm="7"><el-form-item label="已收/已付"><el-input-number v-model="form.paidAmount" :min="0" :precision="8" :controls="false" class="full" /></el-form-item></el-col>
          <el-col :xs="24" :sm="7"><el-form-item label="汇率"><el-input-number v-model="form.exchangeRateToCny" :min="0.00000001" :precision="8" :controls="false" :disabled="form.currencyCode === 'CNY'" class="full" /></el-form-item></el-col>
          <el-col :xs="24"><el-form-item label="付款计划" prop="paymentPlan"><el-input v-model="form.paymentPlan" type="textarea" :rows="2" :maxlength="fieldLimits.remark" show-word-limit /></el-form-item></el-col>
        </el-row>
        </section>
      </el-form>
      <AttachmentList
        ref="createAttachmentRef"
        business-type="AR_AP_BILL"
        :editable="auth.hasMenu('BTN_AR_AP_ATTACHMENT')"
      />
      <template #footer><el-button @click="closeCreate">取消</el-button><el-button v-if="auth.hasMenu('BTN_AR_AP_CREATE')" type="primary" @click="save">保存</el-button></template>
    </el-dialog>
    <el-dialog v-model="attachmentDialogVisible" title="应收应付附件" width="720px" @closed="load">
      <AttachmentList
        ref="manageAttachmentRef"
        business-type="AR_AP_BILL"
        :business-id="attachmentBusinessId"
        :editable="auth.hasMenu('BTN_AR_AP_ATTACHMENT')"
      />
    </el-dialog>
    <el-dialog v-model="settlementDialogVisible" :title="settlementTitle" width="min(1080px, 92vw)" top="5vh">
      <div v-if="currentSettlementBill" class="settlement-summary">
        <div><span>单号</span><strong>{{ currentSettlementBill.billNo }}</strong></div>
        <div><span>类型</span><strong>{{ currentSettlementBill.billType === 'RECEIVABLE' ? '应收' : '应付' }}</strong></div>
        <div><span>往来单位</span><strong>{{ currentSettlementBill.partnerName }}</strong></div>
        <div><span>总金额</span><strong><AmountText :value="currentSettlementBill.amount" :currency-code="currentSettlementBill.currencyCode" :currency-name="currentSettlementBill.currencyName" /></strong></div>
        <div><span>已收/已付</span><strong><AmountText :value="currentSettlementBill.paidAmount" :currency-code="currentSettlementBill.currencyCode" :currency-name="currentSettlementBill.currencyName" /></strong></div>
        <div><span>未结金额</span><strong><AmountText :value="currentSettlementBill.remainingAmount" :currency-code="currentSettlementBill.currencyCode" :currency-name="currentSettlementBill.currencyName" /></strong></div>
      </div>
      <el-form v-if="currentSettlementBill && currentSettlementBill.remainingAmount > 0" ref="settlementFormRef" :model="settlementForm" :rules="settlementRules" label-width="106px" class="settlement-form">
        <el-row :gutter="12">
          <el-col :xs="24" :sm="8"><el-form-item label="核销日期" required><el-date-picker v-model="settlementForm.settlementDate" value-format="YYYY-MM-DD" class="full" /></el-form-item></el-col>
          <el-col :xs="24" :sm="8"><el-form-item label="核销金额" required><el-input-number v-model="settlementForm.amount" :min="0.00000001" :max="currentSettlementBill.remainingAmount" :precision="8" :controls="false" class="full" /></el-form-item></el-col>
          <el-col :xs="24" :sm="8"><el-form-item label="结算方式"><el-select v-model="settlementForm.settlementMethod" clearable filterable class="full"><el-option v-for="item in settlementMethodOptions" :key="item.id" :label="item.name" :value="item.name" /></el-select></el-form-item></el-col>
          <el-col :xs="24" :sm="8"><el-form-item label="银行账户"><el-select v-model="settlementForm.bankAccount" clearable filterable class="full"><el-option v-for="item in bankAccountOptions" :key="item.id" :label="item.name" :value="item.name" /></el-select></el-form-item></el-col>
          <el-col :xs="24" :sm="8"><el-form-item label="出纳流水"><el-input v-model="settlementForm.cashierTransactionNo" maxlength="80" /></el-form-item></el-col>
          <el-col :xs="24"><el-form-item label="核销说明" prop="remark"><el-input v-model="settlementForm.remark" type="textarea" :rows="3" :maxlength="fieldLimits.remark" show-word-limit /></el-form-item></el-col>
        </el-row>
      </el-form>
      <el-alert v-else-if="currentSettlementBill" title="该单据已经结清，不能继续新增核销。" type="info" show-icon :closable="false" />
      <div class="settlement-history">
        <div class="section-heading"><span>核销流水</span></div>
        <el-table v-loading="settlementLoading" :data="settlementRows" stripe max-height="260">
          <el-table-column prop="settlementDate" label="核销日期" width="120" />
          <el-table-column prop="amount" label="金额" width="140" align="right">
            <template #default="{ row }"><AmountText :value="row.amount" :currency-code="currentSettlementBill?.currencyCode || 'CNY'" :currency-name="currentSettlementBill?.currencyName || '人民币'" /></template>
          </el-table-column>
          <el-table-column prop="amountCny" label="人民币金额" width="130" align="right">
            <template #default="{ row }"><AmountText :value="row.amountCny" currency-code="CNY" currency-name="人民币" /></template>
          </el-table-column>
          <el-table-column prop="settlementMethod" label="结算方式" width="120" />
          <el-table-column prop="bankAccount" label="银行账户" min-width="150" />
          <el-table-column prop="cashierTransactionNo" label="出纳流水" min-width="130" />
          <el-table-column prop="remark" label="说明" min-width="180" />
        </el-table>
      </div>
      <template #footer>
        <el-button @click="settlementDialogVisible = false">关闭</el-button>
        <el-button
          v-if="currentSettlementBill && currentSettlementBill.remainingAmount > 0 && auth.hasMenu('BTN_AR_AP_SETTLE')"
          type="primary"
          :loading="settlementSubmitting"
          @click="submitSettlement"
        >
          保存核销
        </el-button>
      </template>
    </el-dialog>
    <OperationLogDrawer ref="operationLogDrawerRef" />
  </div>
</template>

<script setup lang="ts">
import { computed, onActivated, onMounted, reactive, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { Delete, Download, Plus, Search } from '@element-plus/icons-vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import type { FormInstance, FormRules } from 'element-plus'
import AttachmentList from '@/components/attachments/AttachmentList.vue'
import AmountText from '@/components/common/AmountText.vue'
import OperationLogDrawer from '@/components/operation-log/OperationLogDrawer.vue'
import { api } from '@/api/fm'
import { saveBlob } from '@/api/http'
import { useAuthStore } from '@/stores/auth'
import type { ArApPaymentStatsView, ArApSettlementView, ArApView, BasicDictionaryView } from '@/types/api'
import { flattenDictionaryOptions, withFallbackDictionaryOption, type DictionaryOption } from '@/utils/dictionaries'
import { fieldLimits } from '@/utils/validators'
import { formatMoney, formatPlainMoney, toChineseCapitalAmount } from '@/utils/money'
import { queryString } from '@/utils/routeQuery'

/**
 * 常量 rows：保存当前模块的页面状态、配置项、接口实例或计算结果。
 */
const rows = ref<ArApView[]>([])
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
/** 路由实例，用于从应收应付跳转到凭证记账页面查看在线凭证。 */
const router = useRouter()
/**
 * 常量 activeTab：控制应收应付页面当前打开的页签，bills 为单据列表，paymentStats 为收付统计。
 */
const activeTab = ref<'bills' | 'paymentStats'>('bills')
/**
 * 常量 paymentStatsLoading：表示收付统计页签是否正在查询后端统计结果。
 */
const paymentStatsLoading = ref(false)
/**
 * 常量 paymentStatsExporting：表示收付统计导出接口是否正在生成 Excel。
 */
const paymentStatsExporting = ref(false)
/**
 * 常量 paymentStatsLoaded：记录收付统计是否已经加载过，避免首次进入单据页签时重复请求。
 */
const paymentStatsLoaded = ref(false)
/**
 * 常量 dialogVisible：保存当前模块的页面状态、配置项、接口实例或计算结果。
 */
const dialogVisible = ref(false)
/**
 * 常量 attachmentDialogVisible：保存当前模块的页面状态、配置项、接口实例或计算结果。
 */
const attachmentDialogVisible = ref(false)
/**
 * 常量 settlementDialogVisible：控制收付核销弹窗显示状态。
 */
const settlementDialogVisible = ref(false)
/**
 * 常量 attachmentBusinessId：保存当前模块的页面状态、配置项、接口实例或计算结果。
 */
const attachmentBusinessId = ref<number>()
/**
 * 常量 createAttachmentRef：保存当前模块的页面状态、配置项、接口实例或计算结果。
 */
const createAttachmentRef = ref<InstanceType<typeof AttachmentList>>()
/**
 * 常量 formRef：指向新增应收应付单表单实例，用于字段级校验和红框提示。
 */
const formRef = ref<FormInstance>()
/**
 * 常量 manageAttachmentRef：保存当前模块的页面状态、配置项、接口实例或计算结果。
 */
const manageAttachmentRef = ref<InstanceType<typeof AttachmentList>>()
/**
 * 常量 settlementFormRef：指向收付核销表单实例，用于核销说明等字段级校验。
 */
const settlementFormRef = ref<FormInstance>()
/**
 * 常量 operationLogDrawerRef：保存当前模块的页面状态、配置项、接口实例或计算结果。
 */
const operationLogDrawerRef = ref<InstanceType<typeof OperationLogDrawer>>()
/**
 * 常量 selectedRows：保存当前模块的页面状态、配置项、接口实例或计算结果。
 */
const selectedRows = ref<ArApView[]>([])
/**
 * 常量 settlementRows：保存当前应收应付单的核销流水明细。
 */
const settlementRows = ref<ArApSettlementView[]>([])
/**
 * 常量 currentSettlementBill：保存当前正在核销或查看核销流水的应收应付单。
 */
const currentSettlementBill = ref<ArApView>()
/**
 * 常量 settlementLoading：表示核销流水是否正在加载。
 */
const settlementLoading = ref(false)
/**
 * 常量 settlementSubmitting：表示核销保存接口是否正在提交。
 */
const settlementSubmitting = ref(false)
/**
 * 常量 currencyOptions：保存当前模块的页面状态、配置项、接口实例或计算结果。
 */
const currencyOptions = ref<BasicDictionaryView[]>([])
/**
 * 常量 partnerOptions：保存当前模块的页面状态、配置项、接口实例或计算结果。
 */
const partnerOptions = ref<BasicDictionaryView[]>([])
/**
 * 常量 projectOptions：保存项目字典下拉选项，用于应收应付项目维度筛选和新增。
 */
const projectOptions = ref<BasicDictionaryView[]>([])
/**
 * 常量 organizationOptions：保存当前模块的页面状态、配置项、接口实例或计算结果。
 */
const organizationOptions = ref<DictionaryOption[]>([])
/**
 * 常量 arApDocumentTypeOptions：保存当前模块的页面状态、配置项、接口实例或计算结果。
 */
const arApDocumentTypeOptions = ref<BasicDictionaryView[]>([])
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
 * 常量 bankAccountOptions：保存银行账户字典选项，用于核销时选择收付款账户。
 */
const bankAccountOptions = ref<BasicDictionaryView[]>([])
/**
 * 常量 settlementTitle：保存核销弹窗标题。
 */
const settlementTitle = ref('收付核销')
/**
 * 常量 settlementForm：保存新增收付核销表单。
 */
const settlementForm = reactive({
  settlementDate: new Date().toISOString().slice(0, 10),
  amount: 0,
  settlementMethod: '',
  bankAccount: '',
  cashierTransactionNo: '',
  remark: ''
})

/**
 * 收付核销表单字段校验规则。
 *
 * 实现步骤：核销说明作为说明类文本按 2000 字符校验，blur 后在字段下方提示。
 */
const settlementRules: FormRules = {
  remark: [{ max: fieldLimits.remark, message: `核销说明不能超过${fieldLimits.remark}个字符`, trigger: 'blur' }]
}
/**
 * 常量 today：保存当前模块的页面状态、配置项、接口实例或计算结果。
 */
const today = new Date().toISOString().slice(0, 10)
/**
 * 常量 form：保存当前模块的页面状态、配置项、接口实例或计算结果。
 */
const form = reactive({
  /**
   * 字段 billType：表示表单、筛选条件、接口数据或组件状态中的 billType 值。
   */
  billType: 'RECEIVABLE',
  /**
   * 字段 documentType：表示表单、筛选条件、接口数据或组件状态中的 documentType 值。
   */
  documentType: '销售应收',
  /**
   * 字段 partnerName：表示表单、筛选条件、接口数据或组件状态中的 partnerName 值。
   */
  partnerName: '',
  /**
   * 字段 projectCode：表示项目字典编码，保存应收应付单所属项目。
   */
  projectCode: '',
  /**
   * 字段 projectName：表示项目名称快照，保存应收应付单新增时的项目名称。
   */
  projectName: '',
  /**
   * 字段 businessOrganization：表示表单、筛选条件、接口数据或组件状态中的 businessOrganization 值。
   */
  businessOrganization: '',
  /**
   * 字段 settlementOrganization：表示表单、筛选条件、接口数据或组件状态中的 settlementOrganization 值。
   */
  settlementOrganization: '',
  /**
   * 字段 paymentOrganization：表示表单、筛选条件、接口数据或组件状态中的 paymentOrganization 值。
   */
  paymentOrganization: '',
  /**
   * 字段 paymentTerms：表示表单、筛选条件、接口数据或组件状态中的 paymentTerms 值。
   */
  paymentTerms: '',
  /**
   * 字段 settlementMethod：表示表单、筛选条件、接口数据或组件状态中的 settlementMethod 值。
   */
  settlementMethod: '',
  /**
   * 字段 sourceBillType：表示表单、筛选条件、接口数据或组件状态中的 sourceBillType 值。
   */
  sourceBillType: '',
  /**
   * 字段 sourceBillNo：表示表单、筛选条件、接口数据或组件状态中的 sourceBillNo 值。
   */
  sourceBillNo: '',
  /**
   * 字段 billDate：表示表单、筛选条件、接口数据或组件状态中的 billDate 值。
   */
  billDate: today,
  /**
   * 字段 dueDate：表示表单、筛选条件、接口数据或组件状态中的 dueDate 值。
   */
  dueDate: today,
  /**
   * 字段 amount：表示表单、筛选条件、接口数据或组件状态中的 amount 值。
   */
  amount: 1,
  /**
   * 字段 paidAmount：表示表单、筛选条件、接口数据或组件状态中的 paidAmount 值。
   */
  paidAmount: 0,
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
  exchangeRateToCny: 1,
  /**
   * 字段 paymentPlan：表示表单、筛选条件、接口数据或组件状态中的 paymentPlan 值。
   */
  paymentPlan: ''
})
/**
 * 常量 filters：保存当前模块的页面状态、配置项、接口实例或计算结果。
 */
const filters = reactive({
  /**
   * 字段 dateRange：表示表单、筛选条件、接口数据或组件状态中的 dateRange 值。
   */
  dateRange: [] as string[],
  /**
   * 字段 billNo：表示表单、筛选条件、接口数据或组件状态中的 billNo 值。
   */
  billNo: '',
  /**
   * 字段 billType：表示表单、筛选条件、接口数据或组件状态中的 billType 值。
   */
  billType: undefined as ArApView['billType'] | undefined,
  /**
   * 字段 partnerName：表示表单、筛选条件、接口数据或组件状态中的 partnerName 值。
   */
  partnerName: '',
  /**
   * 字段 projectCode：表示项目字典编码，用于按项目筛选应收应付单。
   */
  projectCode: '',
  /**
   * 字段 status：表示表单、筛选条件、接口数据或组件状态中的 status 值。
   */
  status: undefined as ArApView['status'] | undefined,
  /**
   * 字段 paymentPlan：表示表单、筛选条件、接口数据或组件状态中的 paymentPlan 值。
   */
  paymentPlan: ''
})

/**
 * 应收应付新增表单字段校验规则。
 *
 * 实现步骤：
 * 1. 客户/供应商作为必填项在字段下方提示；
 * 2. 来源单号和付款计划按统一长度限制校验；
 * 3. 文本输入框在 blur 时立即校验并由 Element Plus 标红。
 */
const rules: FormRules = {
  partnerName: [{ required: true, message: '请选择客户/供应商', trigger: 'change' }],
  projectCode: [{ required: true, message: '请选择项目', trigger: 'change' }],
  sourceBillNo: [{ max: fieldLimits.sourceBillNo, message: `来源单号不能超过${fieldLimits.sourceBillNo}个字符`, trigger: 'blur' }],
  paymentPlan: [{ max: fieldLimits.remark, message: `付款计划不能超过${fieldLimits.remark}个字符`, trigger: 'blur' }]
}
/**
 * 常量 statsFilters：保存收付统计页签的项目和客户/供应商筛选条件。
 */
const statsFilters = reactive({
  /**
   * 字段 projectCode：表示统计使用的项目字典编码，空值代表全部项目。
   */
  projectCode: '',
  /**
   * 字段 partnerName：表示统计使用的客户或供应商名称，空值代表全部往来单位。
   */
  partnerName: ''
})
/**
 * 常量 paymentStats：保存收付统计明细行和页面汇总金额。
 */
const paymentStats = reactive<ArApPaymentStatsView>({
  /**
   * 字段 rows：表示按应收应付单号统计的明细行。
   */
  rows: [],
  /**
   * 字段 totalReceivableAmount：表示当前筛选条件下的应收合计。
   */
  totalReceivableAmount: 0,
  /**
   * 字段 totalPayableAmount：表示当前筛选条件下的应付合计。
   */
  totalPayableAmount: 0,
  /**
   * 字段 totalReceivedAmount：表示当前筛选条件下的已收合计。
   */
  totalReceivedAmount: 0,
  /**
   * 字段 totalPaidAmount：表示当前筛选条件下的已付合计。
   */
  totalPaidAmount: 0,
  /**
   * 字段 totalPendingReceivableAmount：表示当前筛选条件下的待收合计。
   */
  totalPendingReceivableAmount: 0,
  /**
   * 字段 totalPendingPayableAmount：表示当前筛选条件下的待付合计。
   */
  totalPendingPayableAmount: 0
})
/**
 * 常量 canQueryPaymentStats：根据菜单权限控制收付统计查询按钮显示。
 */
const canQueryPaymentStats = computed(() => auth.hasMenu('BTN_AR_AP_STATS_QUERY') || auth.hasMenu('PAGE_AR_AP_STATS') || auth.hasMenu('PAGE_AR_AP'))
/**
 * 常量 canExportPaymentStats：根据菜单权限控制收付统计导出按钮显示。
 */
const canExportPaymentStats = computed(() => auth.hasMenu('BTN_AR_AP_STATS_EXPORT') || auth.hasMenu('BTN_AR_AP_EXPORT') || auth.hasMenu('PAGE_AR_AP_STATS'))
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
    rows.value = await api.arApBills(arApSearchParams())
    selectedRows.value = []
  } finally {
    loading.value = false
  }
}

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
 * 查询收付统计。
 *
 * 实现步骤：
 * 1. 读取收付统计页签中的项目和客户/供应商筛选条件；
 * 2. 调用后端同口径统计接口获取明细和汇总；
 * 3. 覆盖页签统计状态，驱动顶部金额卡和表格刷新。
 */
async function loadPaymentStats() {
  paymentStatsLoading.value = true
  try {
    /** 收付统计接口返回结果，包含当前筛选条件下的明细和汇总。 */
    const result = await api.arApPaymentStats(paymentStatsSearchParams())
    Object.assign(paymentStats, result)
    paymentStatsLoaded.value = true
  } finally {
    paymentStatsLoading.value = false
  }
}

/**
 * 组装收付统计查询参数。
 *
 * 实现步骤：
 * 1. 项目为空时传 undefined，让后端不过滤项目维度；
 * 2. 客户/供应商为空时传 undefined，让后端不过滤往来单位；
 * 3. 返回对象同时供页面查询和导出接口复用。
 */
function paymentStatsSearchParams() {
  return {
    /**
     * 字段 projectCode：表示当前统计导出或查询使用的项目编码。
     */
    projectCode: statsFilters.projectCode || undefined,
    /**
     * 字段 partnerName：表示当前统计导出或查询使用的往来单位名称。
     */
    partnerName: statsFilters.partnerName || undefined
  }
}

/**
 * 重置收付统计筛选条件。
 *
 * 实现步骤：
 * 1. 清空项目筛选；
 * 2. 清空客户/供应商筛选；
 * 3. 重新加载全部收付统计。
 */
function resetPaymentStatsFilters() {
  statsFilters.projectCode = ''
  statsFilters.partnerName = ''
  void loadPaymentStats()
}

/**
 * 导出收付统计。
 *
 * 实现步骤：
 * 1. 使用当前项目和客户/供应商筛选条件请求后端导出；
 * 2. 前端按业务要求组装文件名并过滤非法文件名字符；
 * 3. 触发浏览器下载生成的 xlsx 文件。
 */
async function exportPaymentStatsRows() {
  paymentStatsExporting.value = true
  try {
    const { blob } = await api.exportArApPaymentStats(paymentStatsSearchParams())
    saveBlob(blob, buildPaymentStatsFilename())
    ElMessage.success('导出成功')
  } finally {
    paymentStatsExporting.value = false
  }
}

/**
 * 生成收付统计导出文件名。
 *
 * 实现步骤：
 * 1. 固定以“收付统计”作为文件名前缀；
 * 2. 项目名称和客户/供应商名称有值时才追加，避免空筛选出现多余下划线；
 * 3. 追加 yyyyMMddHHmmss 格式的当前时间并补 xlsx 后缀。
 */
function buildPaymentStatsFilename() {
  /** 已清理非法字符的项目名称片段，空值时不会追加到文件名。 */
  const projectName = safeFilenameSegment(selectedStatsProjectName())
  /** 已清理非法字符的客户/供应商名称片段，空值时不会追加到文件名。 */
  const partnerName = safeFilenameSegment(statsFilters.partnerName)
  /** 导出文件名片段集合，按业务规则有值才拼接下划线。 */
  const parts = ['收付统计']
  if (projectName) {
    parts.push(projectName)
  }
  if (partnerName) {
    parts.push(partnerName)
  }
  parts.push(formatFilenameDateTime(new Date()))
  return `${parts.join('_')}.xlsx`
}

/**
 * 获取当前统计筛选项目名称。
 *
 * 实现步骤：优先使用项目字典名称，字典未加载或异常时用项目编码兜底，避免导出文件名缺失筛选信息。
 */
function selectedStatsProjectName() {
  if (!statsFilters.projectCode) {
    return ''
  }
  return projectOptions.value.find((item) => item.code === statsFilters.projectCode)?.name || statsFilters.projectCode
}

/**
 * 清理文件名片段。
 *
 * 实现步骤：
 * 1. 去除首尾空格；
 * 2. 删除 Windows 文件名非法字符；
 * 3. 空结果返回空字符串，由文件名组装逻辑自动省略。
 */
function safeFilenameSegment(value: string) {
  return value.trim().replace(/[\\/:*?"<>|]/g, '')
}

/**
 * 格式化文件名时间。
 *
 * 实现步骤：按年月日时分秒补零输出，满足导出文件名中的当前时间要求。
 */
function formatFilenameDateTime(value: Date) {
  /** 时间补零函数，确保年月日时分秒均为固定宽度。 */
  const pad = (part: number) => String(part).padStart(2, '0')
  return `${value.getFullYear()}${pad(value.getMonth() + 1)}${pad(value.getDate())}${pad(value.getHours())}${pad(value.getMinutes())}${pad(value.getSeconds())}`
}

/**
 * 转换应收应付类型展示文本。
 *
 * 实现步骤：RECEIVABLE 显示应收，PAYABLE 显示应付，未知值原样返回便于排查数据问题。
 */
function billTypeLabel(value: string) {
  return value === 'RECEIVABLE' ? '应收' : value === 'PAYABLE' ? '应付' : value
}

/**
 * 转换应收应付状态展示文本。
 *
 * 实现步骤：把后端枚举值映射为业务中文，未知值原样返回便于排查数据问题。
 */
function statusLabel(value: ArApView['status']) {
  return {
    OPEN: '未结',
    PARTIAL: '部分结清',
    CLOSED: '已结清',
    OVERDUE: '逾期'
  }[value] || value
}

/**
 * 应收应付状态标签颜色。
 *
 * 实现步骤：按未结、部分结清、已结清和逾期的业务含义选择 Element Plus 标签类型。
 */
function statusTagType(value: ArApView['status']) {
  return {
    OPEN: 'warning',
    PARTIAL: 'primary',
    CLOSED: 'success',
    OVERDUE: 'danger'
  }[value] as 'primary' | 'success' | 'warning' | 'danger'
}

/**
 * 汇总收付统计表格底部合计行。
 *
 * 实现步骤：第一列显示合计，金额列直接使用后端汇总金额，避免前端二次累加误差。
 */
function paymentStatsSummaryMethod() {
  return [
    '合计',
    '',
    '',
    '',
    amountSummaryText(paymentStats.totalPayableAmount),
    amountSummaryText(paymentStats.totalPaidAmount),
    amountSummaryText(paymentStats.totalPendingPayableAmount),
    amountSummaryText(paymentStats.totalReceivableAmount),
    amountSummaryText(paymentStats.totalReceivedAmount),
    amountSummaryText(paymentStats.totalPendingReceivableAmount)
  ]
}

/**
 * 生成收付统计合计行金额文本。
 *
 * 实现步骤：
 * 1. 保留表格 summary 行只能返回文本的限制；
 * 2. 文本中同时包含页面金额、数字金额和中文大写金额；
 * 3. 确保合计行也满足金额完整展示要求。
 */
function amountSummaryText(value: number) {
  return `${money(value)}（${formatPlainMoney(value)} CNY / ${toChineseCapitalAmount(value, '人民币')}）`
}

/**
 * 处理应收应付页签切换。
 *
 * 实现步骤：
 * 1. 用户切到收付统计页签时判断是否已经加载过；
 * 2. 未加载时调用统计接口；
 * 3. 已加载时保留当前筛选和结果，避免切换页签造成重复请求。
 */
function handleTabChange(name: string | number) {
  if (name === 'paymentStats' && !paymentStatsLoaded.value) {
    void loadPaymentStats()
  }
}

/**
 * 执行 selectedCurrencyName 方法。
 * 
 * 实现步骤：
 * 1. 读取当前页面状态或调用参数；
 * 2. 完成对应的校验、接口调用或数据转换；
 * 3. 更新页面状态或返回处理结果。
 */
function selectedCurrencyName() {
  return currencyOptions.value.find((item) => item.code === form.currencyCode)?.name || (form.currencyCode === 'CNY' ? '人民币' : form.currencyCode)
}

/**
 * 读取应收应付币种切换时的本地兜底汇率。
 *
 * 实现步骤：
 * 1. 人民币固定为 1；
 * 2. 外币保留现有正数汇率，避免外部接口慢时输入框不变化；
 * 3. 无有效旧值时先填 1，接口成功后自动覆盖为参考汇率。
 */
function fallbackExchangeRate() {
  if (form.currencyCode === 'CNY') {
    return 1
  }
  return Number(form.exchangeRateToCny || 0) > 0 ? form.exchangeRateToCny : 1
}

/**
 * 执行 onCurrencyChange 方法。
 * 
 * 实现步骤：
 * 1. 读取当前页面状态或调用参数；
 * 2. 完成对应的校验、接口调用或数据转换；
 * 3. 更新页面状态或返回处理结果。
 */
async function onCurrencyChange() {
  form.currencyName = selectedCurrencyName()
  form.exchangeRateToCny = fallbackExchangeRate()
  if (form.currencyCode === 'CNY') {
    return
  }
  /**
   * 常量 selectedCode：保存当前模块的页面状态、配置项、接口实例或计算结果。
   */
  const selectedCode = form.currencyCode
  try {
    /**
     * 常量 rate：保存当前模块的页面状态、配置项、接口实例或计算结果。
     */
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
 * 执行 loadPartnerOptions 方法。
 * 
 * 实现步骤：
 * 1. 读取当前页面状态或调用参数；
 * 2. 完成对应的校验、接口调用或数据转换；
 * 3. 更新页面状态或返回处理结果。
 */
async function loadPartnerOptions() {
  partnerOptions.value = await api.enabledDictionaryChildren('PARTNER')
}

/**
 * 加载项目字典选项。
 *
 * 实现步骤：
 * 1. 从基础字典读取启用的 PROJECT 子项；
 * 2. 保存到项目下拉数据源；
 * 3. 应收应付列表、表单和收付统计共用该项目维度。
 */
async function loadProjectOptions() {
  projectOptions.value = await api.enabledDictionaryChildren('PROJECT')
}

/**
 * 加载应收应付业务字典。
 *
 * 实现步骤：
 * 1. 并行读取组织、应收应付单据类型、来源单据、收付款条件和结算方式；
 * 2. 组织按树形层级转换为可识别层级的下拉选项；
 * 3. 给应收应付单据类型补默认项，避免旧环境字典尚未初始化时无法选择。
 */
async function loadBusinessDictionaryOptions() {
  const [organizations, arApDocumentTypes, sourceBillTypes, paymentTerms, settlementMethods, bankAccounts] = await Promise.all([
    api.enabledDictionaryTree('ORGANIZATION'),
    api.enabledDictionaryChildren('AR_AP_DOCUMENT_TYPE'),
    api.enabledDictionaryChildren('SOURCE_BILL_TYPE'),
    api.enabledDictionaryChildren('PAYMENT_TERMS'),
    api.enabledDictionaryChildren('SETTLEMENT_METHOD'),
    api.enabledDictionaryChildren('BANK_ACCOUNT')
  ])
  organizationOptions.value = flattenDictionaryOptions(organizations)
  arApDocumentTypeOptions.value = withFallbackDictionaryOption(
    withFallbackDictionaryOption(arApDocumentTypes, 'AR_AP_DOC_AR_SALES', '销售应收', 'append'),
    'AR_AP_DOC_AP_PURCHASE',
    '采购应付',
    'append'
  )
  sourceBillTypeOptions.value = sourceBillTypes
  paymentTermsOptions.value = paymentTerms
  settlementMethodOptions.value = settlementMethods
  bankAccountOptions.value = bankAccounts
}

/**
 * 给应收应付业务字典下拉补默认项。
 *
 * 实现步骤：
 * 1. 判断后端选项是否已经包含指定名称；
 * 2. 存在时直接返回，保留字典排序；
 * 3. 不存在时追加兜底选项，保证表单默认值可选。
 */
/**
 * 根据应收/应付类型切换默认业务单据类型。
 *
 * 实现步骤：
 * 1. 用户选择应收时默认使用“销售应收”；
 * 2. 用户选择应付时默认使用“采购应付”；
 * 3. 只改默认单据类型，其余组织和金额字段保持用户已录入内容。
 */
function onBillTypeChange() {
  form.documentType = form.billType === 'RECEIVABLE' ? '销售应收' : '采购应付'
}

/**
 * 根据当前项目编码同步项目名称快照。
 *
 * 实现步骤：
 * 1. 按项目编码在项目字典中查找当前选项；
 * 2. 找到项目时写入项目名称，清空项目时清空名称；
 * 3. 保存应收应付单时把编码和名称一起提交给后端。
 */
function onProjectChange() {
  form.projectName = projectOptions.value.find((item) => item.code === form.projectCode)?.name || ''
}
/**
 * 执行 arApSearchParams 方法。
 * 
 * 实现步骤：
 * 1. 读取当前页面状态或调用参数；
 * 2. 完成对应的校验、接口调用或数据转换；
 * 3. 更新页面状态或返回处理结果。
 */
function arApSearchParams() {
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
     * 字段 billNo：表示表单、筛选条件、接口数据或组件状态中的 billNo 值。
     */
    billNo: filters.billNo.trim() || undefined,
    /**
     * 字段 billType：表示表单、筛选条件、接口数据或组件状态中的 billType 值。
     */
    billType: filters.billType,
    /**
     * 字段 partnerName：表示表单、筛选条件、接口数据或组件状态中的 partnerName 值。
     */
    partnerName: filters.partnerName || undefined,
    /**
     * 字段 projectCode：表示项目字典编码，用于按项目筛选应收应付单。
     */
    projectCode: filters.projectCode || undefined,
    /**
     * 字段 status：表示表单、筛选条件、接口数据或组件状态中的 status 值。
     */
    status: filters.status,
    /**
     * 字段 paymentPlan：表示表单、筛选条件、接口数据或组件状态中的 paymentPlan 值。
     */
    paymentPlan: filters.paymentPlan.trim() || undefined
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
     * 字段 billNo：表示表单、筛选条件、接口数据或组件状态中的 billNo 值。
     */
    billNo: '',
    /**
     * 字段 billType：表示表单、筛选条件、接口数据或组件状态中的 billType 值。
     */
    billType: undefined,
    /**
     * 字段 partnerName：表示表单、筛选条件、接口数据或组件状态中的 partnerName 值。
     */
    partnerName: '',
    /**
     * 字段 projectCode：表示项目字典编码，用于清空项目筛选条件。
     */
    projectCode: '',
    /**
     * 字段 status：表示表单、筛选条件、接口数据或组件状态中的 status 值。
     */
    status: undefined,
    /**
     * 字段 paymentPlan：表示表单、筛选条件、接口数据或组件状态中的 paymentPlan 值。
     */
    paymentPlan: ''
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
function handleSelectionChange(selection: ArApView[]) {
  selectedRows.value = selection
}
/**
 * 执行 openAttachment 方法。
 * 
 * 实现步骤：
 * 1. 读取当前页面状态或调用参数；
 * 2. 完成对应的校验、接口调用或数据转换；
 * 3. 更新页面状态或返回处理结果。
 */
function openAttachment(row: ArApView) {
  attachmentBusinessId.value = row.id
  attachmentDialogVisible.value = true
  void manageAttachmentRef.value?.reload(row.id)
}

/**
 * 打开收付核销弹窗。
 *
 * 实现步骤：
 * 1. 保存当前应收应付单；
 * 2. 根据应收或应付生成弹窗标题和默认核销金额；
 * 3. 加载该单据历史核销流水；
 * 4. 显示核销弹窗。
 */
async function openSettlement(row: ArApView) {
  currentSettlementBill.value = row
  settlementTitle.value = row.billType === 'RECEIVABLE' ? '收款核销' : '付款核销'
  settlementForm.settlementDate = today
  settlementForm.amount = Number(row.remainingAmount || 0)
  settlementForm.settlementMethod = row.settlementMethod || ''
  settlementForm.bankAccount = ''
  settlementForm.cashierTransactionNo = ''
  settlementForm.remark = ''
  settlementDialogVisible.value = true
  await loadSettlements(row.id)
}

/**
 * 加载应收应付核销流水。
 *
 * 实现步骤：按应收应付单主键调用后端核销流水接口，结果写入弹窗明细表。
 */
async function loadSettlements(billId: number) {
  settlementLoading.value = true
  try {
    settlementRows.value = await api.arApSettlements(billId)
  } finally {
    settlementLoading.value = false
  }
}

/**
 * 提交收付核销。
 *
 * 实现步骤：
 * 1. 校验当前单据和核销金额；
 * 2. 拦截超过未结金额的核销；
 * 3. 调用后端核销接口；
 * 4. 成功后刷新列表和核销流水。
 */
async function submitSettlement() {
  if (!currentSettlementBill.value) {
    return
  }
  /** 核销表单校验结果，失败时字段下方显示错误并阻止提交。 */
  const valid = await settlementFormRef.value?.validate().catch(() => false)
  if (!valid) {
    return
  }
  if (Number(settlementForm.amount || 0) <= 0) {
    ElMessage.warning('核销金额必须大于0')
    return
  }
  if (Number(settlementForm.amount || 0) > Number(currentSettlementBill.value.remainingAmount || 0)) {
    ElMessage.warning('核销金额不能超过未结金额')
    return
  }
  settlementSubmitting.value = true
  try {
    /** 核销后的应收应付单最新状态，用于刷新未结金额和流水。 */
    const updated = await api.settleArApBill(currentSettlementBill.value.id, { ...settlementForm })
    currentSettlementBill.value = updated
    settlementForm.amount = Number(updated.remainingAmount || 0)
    settlementForm.remark = ''
    await Promise.all([load(), loadSettlements(updated.id)])
    ElMessage.success('核销成功')
  } finally {
    settlementSubmitting.value = false
  }
}

/**
 * 生成收付核销按钮文本。
 *
 * 实现步骤：应收单显示收款核销，应付单显示付款核销，已结清时仍可进入查看核销流水。
 */
function settleActionLabel(row: ArApView) {
  if (row.remainingAmount <= 0 || row.status === 'CLOSED') {
    return '核销流水'
  }
  return row.billType === 'RECEIVABLE' ? '收款核销' : '付款核销'
}
/**
 * 执行 openOperationLogs 方法。
 * 
 * 实现步骤：
 * 1. 读取当前页面状态或调用参数；
 * 2. 完成对应的校验、接口调用或数据转换；
 * 3. 更新页面状态或返回处理结果。
 */
async function openOperationLogs(row: ArApView) {
  operationLogDrawerRef.value?.open({
    /**
     * 字段 title：表示表单、筛选条件、接口数据或组件状态中的 title 值。
     */
    title: `${row.billNo} 应收应付流水`,
    /**
     * 字段 load：表示表单、筛选条件、接口数据或组件状态中的 load 值。
     */
    load: (params) => api.arApOperationLogs(row.id, params)
  })
}

/**
 * 从应收应付单跳转查看在线凭证。
 *
 * 实现步骤：
 * 1. 接收应收应付单已关联的凭证号；
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
 * 重置应收应付新增表单。
 *
 * 实现步骤：
 * 1. 恢复为应收、销售应收和当天业务日期；
 * 2. 清空往来单位、组织、结算、来源单据和付款计划；
 * 3. 金额恢复为 1，已收已付恢复为 0，币种恢复人民币。
 */
function resetForm() {
  Object.assign(form, {
    /**
     * 字段 billType：表示表单、筛选条件、接口数据或组件状态中的 billType 值。
     */
    billType: 'RECEIVABLE',
    /**
     * 字段 documentType：表示表单、筛选条件、接口数据或组件状态中的 documentType 值。
     */
    documentType: '销售应收',
    /**
     * 字段 partnerName：表示表单、筛选条件、接口数据或组件状态中的 partnerName 值。
     */
    partnerName: '',
    /**
     * 字段 projectCode：表示项目字典编码，新增应收应付单时默认为空。
     */
    projectCode: '',
    /**
     * 字段 projectName：表示项目名称快照，新增应收应付单时默认为空。
     */
    projectName: '',
    /**
     * 字段 businessOrganization：表示表单、筛选条件、接口数据或组件状态中的 businessOrganization 值。
     */
    businessOrganization: '',
    /**
     * 字段 settlementOrganization：表示表单、筛选条件、接口数据或组件状态中的 settlementOrganization 值。
     */
    settlementOrganization: '',
    /**
     * 字段 paymentOrganization：表示表单、筛选条件、接口数据或组件状态中的 paymentOrganization 值。
     */
    paymentOrganization: '',
    /**
     * 字段 paymentTerms：表示表单、筛选条件、接口数据或组件状态中的 paymentTerms 值。
     */
    paymentTerms: '',
    /**
     * 字段 settlementMethod：表示表单、筛选条件、接口数据或组件状态中的 settlementMethod 值。
     */
    settlementMethod: '',
    /**
     * 字段 sourceBillType：表示表单、筛选条件、接口数据或组件状态中的 sourceBillType 值。
     */
    sourceBillType: '',
    /**
     * 字段 sourceBillNo：表示表单、筛选条件、接口数据或组件状态中的 sourceBillNo 值。
     */
    sourceBillNo: '',
    /**
     * 字段 billDate：表示表单、筛选条件、接口数据或组件状态中的 billDate 值。
     */
    billDate: today,
    /**
     * 字段 dueDate：表示表单、筛选条件、接口数据或组件状态中的 dueDate 值。
     */
    dueDate: today,
    /**
     * 字段 amount：表示表单、筛选条件、接口数据或组件状态中的 amount 值。
     */
    amount: 1,
    /**
     * 字段 paidAmount：表示表单、筛选条件、接口数据或组件状态中的 paidAmount 值。
     */
    paidAmount: 0,
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
    exchangeRateToCny: 1,
    /**
     * 字段 paymentPlan：表示表单、筛选条件、接口数据或组件状态中的 paymentPlan 值。
     */
    paymentPlan: ''
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
 * 导出应收应付列表。
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
      : arApSearchParams()
    const { blob, filename } = await api.exportArApBills(payload)
    saveBlob(blob, filename || '应收应付.xlsx')
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
  /** 应收应付新增表单校验结果，失败时字段下方显示错误并阻止保存。 */
  const valid = await formRef.value?.validate().catch(() => false)
  if (!valid) {
    return
  }
  /**
   * 常量 saved：保存当前模块的页面状态、配置项、接口实例或计算结果。
   */
  const saved = await api.createArApBill({ ...form, currencyName: selectedCurrencyName() })
  await createAttachmentRef.value?.uploadPending(saved.id)
  ElMessage.success('保存成功')
  dialogVisible.value = false
  createAttachmentRef.value?.reset()
  resetForm()
  await load()
}
/**
 * 批量删除应收应付单。
 *
 * 实现步骤：
 * 1. 校验是否已经勾选应收应付单；
 * 2. 弹出二次确认，避免误删往来账款数据；
 * 3. 调用后端批量删除接口；
 * 4. 删除成功后刷新列表。
 */
async function batchRemove() {
  if (selectedRows.value.length === 0) {
    ElMessage.warning('请选择需要删除的应收应付单')
    return
  }
  await ElMessageBox.confirm(`确认删除选中的 ${selectedRows.value.length} 张应收应付单？`, '批量删除确认', {
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
  await api.batchDeleteArApBills(selectedRows.value.map((row) => row.id))
  ElMessage.success('批量删除成功')
  await load()
}
onMounted(async () => {
  applyRouteQuery()
  await Promise.all([load(), refreshDictionaryOptions()])
  if (activeTab.value === 'paymentStats') {
    await loadPaymentStats()
  }
})

/**
 * 重新读取应收应付页面使用的全部基础字典。
 *
 * 实现步骤：
 * 1. 重新请求币种、客户/供应商、项目、组织、单据类型、来源类型、付款条件、结算方式和银行账户；
 * 2. HTTP 层禁止 GET 缓存，确保基础信息维护后下拉立即更新；
 * 3. 页面激活或打开新增单据弹窗时调用。
 */
async function refreshDictionaryOptions() {
  await Promise.all([loadCurrencies(), loadPartnerOptions(), loadProjectOptions(), loadBusinessDictionaryOptions()])
}

onActivated(() => {
  void refreshDictionaryOptions()
})

watch(
  () => route.query,
  async () => {
    /** 路由参数是否改变了应收应付列表筛选条件。 */
    const shouldReloadBills = applyRouteQuery()
    if (activeTab.value === 'paymentStats' && !paymentStatsLoaded.value) {
      await loadPaymentStats()
    }
    if (shouldReloadBills) {
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
   * 常量 tabName：保存路由中传入的目标页签名称。
   */
  const tabName = queryString(route.query.tab)
  /**
   * 常量 billNo：保存当前模块的页面状态、配置项、接口实例或计算结果。
   */
  const billNo = queryString(route.query.billNo)
  if (billNo && filters.billNo !== billNo) {
    filters.billNo = billNo
    activeTab.value = 'bills'
    return true
  }
  activeTab.value = tabName === 'paymentStats' ? 'paymentStats' : 'bills'
  return false
}

</script>

<style scoped>
.full{width:100%;}

.ar-ap-tabs {
  --el-tabs-header-height: 42px;
}

.ar-ap-tabs :deep(.el-tabs__header) {
  margin-bottom: 14px;
}

.filter-form {
  margin-bottom: 14px;
  padding: 14px 14px 0;
  border: 1px solid var(--border-color);
  border-radius: 6px;
  background: var(--surface-color);
}

.payment-stats-groups {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 12px;
  margin-bottom: 14px;
}

.payment-stats-group {
  display: grid;
  gap: 10px;
}

.payment-stats-group__title {
  color: var(--heading-color);
  font-size: 14px;
  font-weight: 700;
}

.payment-stats-grid {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 12px;
}

.payment-stat-card {
  display: grid;
  gap: 8px;
  min-height: 82px;
  padding: 14px;
  border: 1px solid var(--border-color);
  border-radius: 6px;
  background: var(--surface-color);
}

.payment-stat-card span {
  color: var(--muted-text-color);
  font-size: 13px;
}

.payment-stat-card strong {
  color: var(--heading-color);
  font-size: 20px;
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

.settlement-summary {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 10px;
  margin-bottom: 14px;
  padding: 12px;
  border: 1px solid #e5e7eb;
  border-radius: 6px;
  background: #f8fafc;
}

.settlement-summary div {
  display: grid;
  gap: 4px;
}

.settlement-summary span {
  color: #6b7280;
  font-size: 12px;
}

.settlement-form {
  margin-bottom: 14px;
}

.settlement-history {
  margin-top: 14px;
}

@media (max-width: 760px) {
  .settlement-summary {
    grid-template-columns: 1fr;
  }

  .payment-stats-grid {
    grid-template-columns: 1fr;
  }

  .payment-stats-groups {
    grid-template-columns: 1fr;
  }
}

</style>
