<template>
  <div class="page">
    <div class="page-header">
      <div>
        <h1 class="page-title">物流管理</h1>
        <p class="page-subtitle">维护物流单、承运信息和运输状态。</p>
      </div>
    </div>

    <el-form class="filter-form" :model="filters" label-width="82px">
      <el-row :gutter="12">
        <el-col :xs="24" :sm="12" :md="8" :lg="6">
          <el-form-item label="计划发运">
            <el-date-picker v-model="filters.dateRange" type="daterange" value-format="YYYY-MM-DD" start-placeholder="开始日期" end-placeholder="结束日期" class="full" />
          </el-form-item>
        </el-col>
        <el-col :xs="24" :sm="12" :md="8" :lg="5">
          <el-form-item label="物流单号">
            <el-input v-model="filters.shipmentNo" clearable placeholder="模糊查询物流单号" />
          </el-form-item>
        </el-col>
        <el-col :xs="24" :sm="12" :md="8" :lg="5">
          <el-form-item label="关联单号">
            <el-input v-model="filters.relatedOrderNo" clearable placeholder="模糊查询关联单号" />
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
          <el-form-item label="承运商">
            <el-select v-model="filters.carrierName" clearable filterable class="full" placeholder="精确选择承运商">
              <el-option v-for="item in carrierOptions" :key="item.id" :label="item.name" :value="item.name" />
            </el-select>
          </el-form-item>
        </el-col>
        <el-col :xs="24" :sm="12" :md="8" :lg="5">
          <el-form-item label="运单号">
            <el-input v-model="filters.trackingNo" clearable placeholder="模糊查询运单号" />
          </el-form-item>
        </el-col>
        <el-col :xs="24" :sm="12" :md="8" :lg="5">
          <el-form-item label="发货区划">
            <el-cascader
              v-model="filters.originDivisionCodes"
              :options="divisionOptions"
              :props="divisionSearchCascaderProps"
              clearable
              filterable
              class="full"
              placeholder="可多选发货区划"
            />
          </el-form-item>
        </el-col>
        <el-col :xs="24" :sm="12" :md="8" :lg="5">
          <el-form-item label="目的区划">
            <el-cascader
              v-model="filters.destinationDivisionCodes"
              :options="divisionOptions"
              :props="divisionSearchCascaderProps"
              clearable
              filterable
              class="full"
              placeholder="可多选目的区划"
            />
          </el-form-item>
        </el-col>
        <el-col :xs="24" :sm="12" :md="8" :lg="5">
          <el-form-item label="发货详址">
            <el-input v-model="filters.origin" clearable placeholder="模糊查询发货详址" />
          </el-form-item>
        </el-col>
        <el-col :xs="24" :sm="12" :md="8" :lg="5">
          <el-form-item label="目的详址">
            <el-input v-model="filters.destination" clearable placeholder="模糊查询目的详址" />
          </el-form-item>
        </el-col>
        <el-col :xs="24" :sm="12" :md="8" :lg="4">
          <el-form-item label="状态">
            <el-select v-model="filters.status" clearable class="full" placeholder="全部">
              <el-option v-for="item in statusOptions" :key="item.value" :label="item.label" :value="item.value" />
            </el-select>
          </el-form-item>
        </el-col>
        <el-col :xs="24" :sm="12" :md="8" :lg="6">
          <el-form-item label=" " class="filter-actions">
            <el-button type="primary" @click="load">查询</el-button>
            <el-button @click="resetFilters">重置</el-button>
            <el-button v-if="auth.hasMenu('BTN_SHIPMENT_CREATE')" type="primary" :icon="Plus" @click="openCreate">新增物流单</el-button>
            <el-button v-if="auth.hasMenu('BTN_SHIPMENT_BATCH_DELETE') && selectedRows.length > 0" type="danger" :icon="Delete" @click="batchRemove">批量删除</el-button>
            <el-button v-if="auth.hasMenu('BTN_SHIPMENT_EXPORT')" :icon="Download" :loading="exporting" @click="exportRows">导出</el-button>
          </el-form-item>
        </el-col>
      </el-row>
    </el-form>

    <div class="panel">
      <el-table v-loading="loading" :data="shipments" stripe @selection-change="handleSelectionChange">
        <el-table-column type="selection" width="48" />
        <el-table-column prop="shipmentNo" label="物流单号" min-width="160" />
        <el-table-column prop="relatedOrderNo" label="关联单号" min-width="150" />
        <el-table-column prop="projectName" label="项目" min-width="140" />
        <el-table-column prop="documentType" label="单据类型" width="120" />
        <el-table-column label="运输与承运" min-width="250">
          <template #default="{ row }">
            <div class="stacked-cell">
              <div class="stacked-cell__line"><span class="stacked-cell__label">运输方式：</span>{{ row.transportMode || '' }}</div>
              <div class="stacked-cell__line"><span class="stacked-cell__label">{{ carrierInfoForMode(row.transportMode).carrierLabel }}：</span>{{ row.carrierName || '' }}</div>
              <div class="stacked-cell__line"><span class="stacked-cell__label">{{ carrierInfoForMode(row.transportMode).trackingLabel }}：</span>{{ row.trackingNo || '' }}</div>
              <div class="stacked-cell__line"><span class="stacked-cell__label">{{ carrierInfoForMode(row.transportMode).driverLabel }}：</span>{{ row.driverName || '' }}</div>
              <div class="stacked-cell__line"><span class="stacked-cell__label">{{ carrierInfoForMode(row.transportMode).phoneLabel }}：</span>{{ row.driverPhone || '' }}</div>
              <div v-if="carrierInfoForMode(row.transportMode).showVehicle" class="stacked-cell__line"><span class="stacked-cell__label">{{ carrierInfoForMode(row.transportMode).vehicleLabel }}：</span>{{ row.vehicleNo || '' }}</div>
            </div>
          </template>
        </el-table-column>
        <el-table-column prop="shippingOrganization" label="发运组织" min-width="130" />
        <el-table-column label="收发地址" min-width="280">
          <template #default="{ row }">
            <div class="stacked-cell">
              <div class="stacked-cell__line"><span class="stacked-cell__label">发货区划：</span>{{ row.originDivisionName || '' }}</div>
              <div class="stacked-cell__line"><span class="stacked-cell__label">发货详址：</span>{{ row.origin || '' }}</div>
              <div class="stacked-cell__line"><span class="stacked-cell__label">目的区划：</span>{{ row.destinationDivisionName || '' }}</div>
              <div class="stacked-cell__line"><span class="stacked-cell__label">目的详址：</span>{{ row.destination || '' }}</div>
            </div>
          </template>
        </el-table-column>
        <el-table-column label="状态" width="110">
          <template #default="{ row }"><el-tag>{{ statusLabel(row.status) }}</el-tag></template>
        </el-table-column>
        <el-table-column label="运输日期" min-width="180">
          <template #default="{ row }">
            <div class="stacked-cell stacked-cell--nowrap">
              <div class="stacked-cell__line"><span class="stacked-cell__label">计划发运：</span>{{ row.plannedShipDate || '' }}</div>
              <div class="stacked-cell__line"><span class="stacked-cell__label">实际发运：</span>{{ row.actualShipDate || '' }}</div>
              <div class="stacked-cell__line"><span class="stacked-cell__label">实际送达：</span>{{ row.deliveredDate || '' }}</div>
            </div>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="320" fixed="right">
          <template #default="{ row }">
            <div class="table-actions">
              <el-button v-if="auth.hasMenu('BTN_SHIPMENT_EDIT') && canEditShipment(row.status)" size="small" @click="openEdit(row)">编辑</el-button>
              <el-button v-if="auth.hasMenu('BTN_SHIPMENT_STATUS') && canConfirmShipmentStatus(row.status)" size="small" type="primary" @click="openStatusConfirm(row)">状态确认</el-button>
              <el-button v-if="auth.hasMenu('BTN_SHIPMENT_LOG')" size="small" @click="openOperationLogs(row)">查看流水</el-button>
            </div>
          </template>
        </el-table-column>
      </el-table>
    </div>

    <el-dialog v-model="dialogVisible" :title="editingId ? '编辑物流单' : '新增物流单'" width="min(1180px, 92vw)" top="5vh">
      <el-form ref="formRef" :model="form" :rules="rules" label-width="96px">
        <section class="business-form-section">
        <div class="section-heading"><span>基本信息</span></div>
        <el-row :gutter="12">
          <el-col :xs="24" :sm="8"><el-form-item label="关联单号" prop="relatedOrderNo"><el-input v-model="form.relatedOrderNo" :maxlength="fieldLimits.sourceBillNo" show-word-limit /></el-form-item></el-col>
          <el-col :xs="24" :sm="8"><el-form-item label="单据类型"><el-select v-model="form.documentType" filterable class="full"><el-option v-for="item in logisticsDocumentTypeOptions" :key="item.id" :label="item.name" :value="item.name" /></el-select></el-form-item></el-col>
          <el-col :xs="24" :sm="8"><el-form-item label="运输方式"><el-select v-model="form.transportMode" clearable filterable class="full" @change="onTransportModeChange(form)"><el-option v-for="item in transportModeOptions" :key="item.id" :label="item.name" :value="item.name" /></el-select></el-form-item></el-col>
          <el-col :xs="24" :sm="8"><el-form-item label="项目" prop="projectCode"><el-select v-model="form.projectCode" clearable filterable class="full" placeholder="请选择项目" @change="onProjectChange(form)"><el-option v-for="item in projectOptions" :key="item.id" :label="item.name" :value="item.code" /></el-select></el-form-item></el-col>
          <el-col :xs="24" :sm="8"><el-form-item label="发运组织"><el-select v-model="form.shippingOrganization" clearable filterable class="full"><el-option v-for="item in organizationOptions" :key="item.value" :label="item.label" :value="item.value" /></el-select></el-form-item></el-col>
          <el-col :xs="24" :sm="8"><el-form-item label="收货组织"><el-select v-model="form.receivingOrganization" clearable filterable class="full"><el-option v-for="item in organizationOptions" :key="item.value" :label="item.label" :value="item.value" /></el-select></el-form-item></el-col>
          <el-col :xs="24" :sm="8"><el-form-item label="计划发运" prop="plannedShipDate"><el-date-picker v-model="form.plannedShipDate" type="date" value-format="YYYY-MM-DD" class="full" /></el-form-item></el-col>
        </el-row>
        </section>
        <section class="business-form-section">
        <div class="section-heading"><span>承运信息</span><small>{{ form.transportMode || '默认运输方式' }}：{{ formCarrierInfo.trackingLabel }}、{{ formCarrierInfo.driverLabel }}{{ formCarrierInfo.showVehicle ? `、${formCarrierInfo.vehicleLabel}` : '' }}</small></div>
        <el-row :gutter="12">
          <el-col :xs="24" :sm="8">
            <el-form-item :label="formCarrierInfo.carrierLabel" prop="carrierName">
              <el-select v-model="form.carrierName" filterable class="full" :placeholder="formCarrierInfo.carrierPlaceholder">
                <el-option v-for="item in carrierOptions" :key="item.id" :label="item.name" :value="item.name" />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :xs="24" :sm="8"><el-form-item :label="formCarrierInfo.trackingLabel"><el-input v-model="form.trackingNo" maxlength="120" :placeholder="formCarrierInfo.trackingPlaceholder" /></el-form-item></el-col>
          <el-col :xs="24" :sm="8"><el-form-item :label="formCarrierInfo.driverLabel" prop="driverName"><el-input v-model="form.driverName" :maxlength="fieldLimits.chineseName" show-word-limit :placeholder="formCarrierInfo.driverPlaceholder" /></el-form-item></el-col>
          <el-col :xs="24" :sm="8"><el-form-item :label="formCarrierInfo.phoneLabel" prop="driverPhone"><el-input v-model="form.driverPhone" maxlength="30" :placeholder="formCarrierInfo.phonePlaceholder" /></el-form-item></el-col>
          <el-col v-if="formCarrierInfo.showVehicle" :xs="24" :sm="8"><el-form-item :label="formCarrierInfo.vehicleLabel" prop="vehicleNo"><el-input v-model="form.vehicleNo" maxlength="12" :placeholder="formCarrierInfo.vehiclePlaceholder" /></el-form-item></el-col>
        </el-row>
        </section>
        <section class="business-form-section">
        <div class="section-heading"><span>收发地址</span></div>
        <el-row :gutter="12">
          <el-col :xs="24" :sm="12"><el-form-item label="发货区划" prop="originDivisionCode"><el-cascader v-model="form.originDivisionCode" :options="divisionOptions" :props="divisionCascaderProps" filterable class="full" placeholder="请选择发货行政区划" @change="onOriginDivisionChange" /></el-form-item></el-col>
          <el-col :xs="24" :sm="12"><el-form-item label="发货详址" prop="origin"><el-input v-model="form.origin" :maxlength="fieldLimits.address" show-word-limit /></el-form-item></el-col>
          <el-col :xs="24" :sm="12"><el-form-item label="目的区划" prop="destinationDivisionCode"><el-cascader v-model="form.destinationDivisionCode" :options="divisionOptions" :props="divisionCascaderProps" filterable class="full" placeholder="请选择目的行政区划" @change="onDestinationDivisionChange" /></el-form-item></el-col>
          <el-col :xs="24" :sm="12"><el-form-item label="目的详址" prop="destination"><el-input v-model="form.destination" :maxlength="fieldLimits.address" show-word-limit /></el-form-item></el-col>
          <el-col :xs="24"><el-form-item label="备注" prop="remark"><el-input v-model="form.remark" type="textarea" :rows="2" :maxlength="fieldLimits.remark" show-word-limit /></el-form-item></el-col>
        </el-row>
        </section>
      </el-form>
      <AttachmentList
        ref="attachmentRef"
        business-type="SHIPMENT"
        :business-id="editingId"
        :editable="canManageAttachment"
      />
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button v-if="editingId ? auth.hasMenu('BTN_SHIPMENT_EDIT') : auth.hasMenu('BTN_SHIPMENT_CREATE')" type="primary" @click="save">保存</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="statusDialogVisible" title="物流状态确认" width="min(980px, 90vw)" top="6vh">
      <el-form ref="statusFormRef" :model="statusForm" :rules="statusRules" label-width="110px">
        <el-form-item label="物流单号">
          <el-input v-model="statusForm.shipmentNo" disabled />
        </el-form-item>
        <el-form-item label="当前状态">
          <el-tag>{{ statusLabel(statusCurrentStatus) }}</el-tag>
        </el-form-item>
        <el-form-item label="确认状态" required>
          <el-select v-model="statusForm.status" class="full" placeholder="请选择确认状态">
            <el-option v-for="item in availableStatusOptions" :key="item.value" :label="item.label" :value="item.value" />
          </el-select>
        </el-form-item>
        <el-form-item label="关联单号" prop="relatedOrderNo">
          <el-input v-model="statusForm.relatedOrderNo" :maxlength="fieldLimits.sourceBillNo" show-word-limit />
        </el-form-item>
        <el-row :gutter="12">
          <el-col :xs="24" :sm="8"><el-form-item label="单据类型"><el-select v-model="statusForm.documentType" filterable class="full"><el-option v-for="item in logisticsDocumentTypeOptions" :key="item.id" :label="item.name" :value="item.name" /></el-select></el-form-item></el-col>
          <el-col :xs="24" :sm="8"><el-form-item label="运输方式"><el-select v-model="statusForm.transportMode" clearable filterable class="full" @change="onTransportModeChange(statusForm)"><el-option v-for="item in transportModeOptions" :key="item.id" :label="item.name" :value="item.name" /></el-select></el-form-item></el-col>
          <el-col :xs="24" :sm="8"><el-form-item label="项目" prop="projectCode"><el-select v-model="statusForm.projectCode" clearable filterable class="full" placeholder="请选择项目" @change="onProjectChange(statusForm)"><el-option v-for="item in projectOptions" :key="item.id" :label="item.name" :value="item.code" /></el-select></el-form-item></el-col>
          <el-col :xs="24" :sm="8"><el-form-item label="发运组织"><el-select v-model="statusForm.shippingOrganization" clearable filterable class="full"><el-option v-for="item in organizationOptions" :key="item.value" :label="item.label" :value="item.value" /></el-select></el-form-item></el-col>
          <el-col :xs="24" :sm="8"><el-form-item label="收货组织"><el-select v-model="statusForm.receivingOrganization" clearable filterable class="full"><el-option v-for="item in organizationOptions" :key="item.value" :label="item.label" :value="item.value" /></el-select></el-form-item></el-col>
        </el-row>
        <div class="carrier-mode-hint">{{ statusForm.transportMode || '默认运输方式' }}：{{ statusCarrierInfo.trackingLabel }}、{{ statusCarrierInfo.driverLabel }}{{ statusCarrierInfo.showVehicle ? `、${statusCarrierInfo.vehicleLabel}` : '' }}</div>
        <el-form-item :label="statusCarrierInfo.carrierLabel" prop="carrierName">
          <el-select v-model="statusForm.carrierName" filterable class="full" :placeholder="statusCarrierInfo.carrierPlaceholder">
            <el-option v-for="item in carrierOptions" :key="item.id" :label="item.name" :value="item.name" />
          </el-select>
        </el-form-item>
        <el-row :gutter="12">
          <el-col :xs="24" :sm="8"><el-form-item :label="statusCarrierInfo.trackingLabel"><el-input v-model="statusForm.trackingNo" maxlength="120" :placeholder="statusCarrierInfo.trackingPlaceholder" /></el-form-item></el-col>
          <el-col :xs="24" :sm="8"><el-form-item :label="statusCarrierInfo.driverLabel" prop="driverName"><el-input v-model="statusForm.driverName" :maxlength="fieldLimits.chineseName" show-word-limit :placeholder="statusCarrierInfo.driverPlaceholder" /></el-form-item></el-col>
          <el-col :xs="24" :sm="8"><el-form-item :label="statusCarrierInfo.phoneLabel" prop="driverPhone"><el-input v-model="statusForm.driverPhone" maxlength="30" :placeholder="statusCarrierInfo.phonePlaceholder" /></el-form-item></el-col>
          <el-col v-if="statusCarrierInfo.showVehicle" :xs="24" :sm="8"><el-form-item :label="statusCarrierInfo.vehicleLabel" prop="vehicleNo"><el-input v-model="statusForm.vehicleNo" maxlength="12" :placeholder="statusCarrierInfo.vehiclePlaceholder" /></el-form-item></el-col>
        </el-row>
        <el-form-item label="发货区划" prop="originDivisionCode">
          <el-cascader
            v-model="statusForm.originDivisionCode"
            :options="divisionOptions"
            :props="divisionCascaderProps"
            filterable
            class="full"
            placeholder="请选择发货行政区划"
            @change="onStatusOriginDivisionChange"
          />
        </el-form-item>
        <el-form-item label="发货详址" prop="origin">
          <el-input v-model="statusForm.origin" :maxlength="fieldLimits.address" show-word-limit />
        </el-form-item>
        <el-form-item label="目的区划" prop="destinationDivisionCode">
          <el-cascader
            v-model="statusForm.destinationDivisionCode"
            :options="divisionOptions"
            :props="divisionCascaderProps"
            filterable
            class="full"
            placeholder="请选择目的行政区划"
            @change="onStatusDestinationDivisionChange"
          />
        </el-form-item>
        <el-form-item label="目的详址" prop="destination">
          <el-input v-model="statusForm.destination" :maxlength="fieldLimits.address" show-word-limit />
        </el-form-item>
        <el-row :gutter="12">
          <el-col :xs="24" :sm="8">
            <el-form-item label="计划发运" prop="plannedShipDate">
              <el-date-picker v-model="statusForm.plannedShipDate" type="date" value-format="YYYY-MM-DD" class="full" />
            </el-form-item>
          </el-col>
          <el-col :xs="24" :sm="8">
            <el-form-item label="实际发运">
              <el-date-picker v-model="statusForm.actualShipDate" type="date" value-format="YYYY-MM-DD" class="full" />
            </el-form-item>
          </el-col>
          <el-col :xs="24" :sm="8">
            <el-form-item label="实际送达">
              <el-date-picker v-model="statusForm.deliveredDate" type="date" value-format="YYYY-MM-DD" class="full" />
            </el-form-item>
          </el-col>
        </el-row>
        <el-form-item label="物流备注" prop="remark">
          <el-input v-model="statusForm.remark" type="textarea" :rows="2" :maxlength="fieldLimits.remark" show-word-limit />
        </el-form-item>
        <el-form-item label="确认说明" prop="operationRemark">
          <el-input v-model="statusForm.operationRemark" type="textarea" :rows="2" :maxlength="fieldLimits.remark" show-word-limit placeholder="记录本次状态确认说明" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="statusDialogVisible = false">取消</el-button>
        <el-button type="primary" @click="confirmStatus">确认</el-button>
      </template>
    </el-dialog>

    <OperationLogDrawer ref="operationLogDrawerRef" />
  </div>
</template>

<script setup lang="ts">
import { computed, onActivated, onMounted, reactive, ref, watch } from 'vue'
import { useRoute } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import type { FormInstance, FormRules } from 'element-plus'
import { Delete, Download, Plus } from '@element-plus/icons-vue'
import AttachmentList from '@/components/attachments/AttachmentList.vue'
import OperationLogDrawer from '@/components/operation-log/OperationLogDrawer.vue'
import { api } from '@/api/fm'
import { saveBlob } from '@/api/http'
import { useAuthStore } from '@/stores/auth'
import type { BasicDictionaryView, ShipmentStatus, ShipmentView } from '@/types/api'
import { flattenDictionaryOptions, withFallbackDictionaryOption, type DictionaryOption } from '@/utils/dictionaries'
import { chineseNamePattern, contactPhonePattern, fieldLimits, vehicleNoPattern } from '@/utils/validators'
import { queryString } from '@/utils/routeQuery'

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
 * 常量 shipments：保存当前模块的页面状态、配置项、接口实例或计算结果。
 */
const shipments = ref<ShipmentView[]>([])
/**
 * 常量 selectedRows：保存当前模块的页面状态、配置项、接口实例或计算结果。
 */
const selectedRows = ref<ShipmentView[]>([])
/**
 * 常量 carrierOptions：保存当前模块的页面状态、配置项、接口实例或计算结果。
 */
const carrierOptions = ref<BasicDictionaryView[]>([])
/**
 * 常量 projectOptions：保存项目字典下拉选项，用于物流单项目维度筛选和录入。
 */
const projectOptions = ref<BasicDictionaryView[]>([])
/**
 * 常量 divisionOptions：保存当前模块的页面状态、配置项、接口实例或计算结果。
 */
const divisionOptions = ref<BasicDictionaryView[]>([])
/**
 * 常量 organizationOptions：保存当前模块的页面状态、配置项、接口实例或计算结果。
 */
const organizationOptions = ref<DictionaryOption[]>([])
/**
 * 常量 logisticsDocumentTypeOptions：保存当前模块的页面状态、配置项、接口实例或计算结果。
 */
const logisticsDocumentTypeOptions = ref<BasicDictionaryView[]>([])
/**
 * 常量 transportModeOptions：保存当前模块的页面状态、配置项、接口实例或计算结果。
 */
const transportModeOptions = ref<BasicDictionaryView[]>([])
/**
 * 常量 attachmentRef：保存当前模块的页面状态、配置项、接口实例或计算结果。
 */
const attachmentRef = ref<InstanceType<typeof AttachmentList>>()
/**
 * 常量 formRef：指向物流新增/编辑表单实例，用于字段级校验和红框提示。
 */
const formRef = ref<FormInstance>()
/**
 * 常量 operationLogDrawerRef：保存当前模块的页面状态、配置项、接口实例或计算结果。
 */
const operationLogDrawerRef = ref<InstanceType<typeof OperationLogDrawer>>()
/**
 * 常量 statusFormRef：指向物流状态确认表单实例，用于字段级校验和红框提示。
 */
const statusFormRef = ref<FormInstance>()
/**
 * 常量 dialogVisible：保存当前模块的页面状态、配置项、接口实例或计算结果。
 */
const dialogVisible = ref(false)
/**
 * 常量 statusDialogVisible：保存当前模块的页面状态、配置项、接口实例或计算结果。
 */
const statusDialogVisible = ref(false)
/**
 * 常量 editingId：保存当前模块的页面状态、配置项、接口实例或计算结果。
 */
const editingId = ref<number>()
/**
 * 常量 statusEditingId：保存当前模块的页面状态、配置项、接口实例或计算结果。
 */
const statusEditingId = ref<number>()
/**
 * 常量 statusCurrentStatus：保存当前模块的页面状态、配置项、接口实例或计算结果。
 */
const statusCurrentStatus = ref<ShipmentStatus>('CREATED')
/**
 * 常量 filters：保存当前模块的页面状态、配置项、接口实例或计算结果。
 */
const filters = reactive({
  /**
   * 字段 dateRange：表示表单、筛选条件、接口数据或组件状态中的 dateRange 值。
   */
  dateRange: [] as string[],
  /**
   * 字段 shipmentNo：表示表单、筛选条件、接口数据或组件状态中的 shipmentNo 值。
   */
  shipmentNo: '',
  /**
   * 字段 relatedOrderNo：表示表单、筛选条件、接口数据或组件状态中的 relatedOrderNo 值。
   */
  relatedOrderNo: '',
  /**
   * 字段 projectCode：表示项目字典编码，用于按项目筛选物流单。
   */
  projectCode: '',
  /**
   * 字段 carrierName：表示表单、筛选条件、接口数据或组件状态中的 carrierName 值。
   */
  carrierName: '',
  /**
   * 字段 trackingNo：表示表单、筛选条件、接口数据或组件状态中的 trackingNo 值。
   */
  trackingNo: '',
  /**
   * 字段 originDivisionCodes：表示表单、筛选条件、接口数据或组件状态中的 originDivisionCodes 值。
   */
  originDivisionCodes: [] as Array<string[] | string>,
  /**
   * 字段 destinationDivisionCodes：表示表单、筛选条件、接口数据或组件状态中的 destinationDivisionCodes 值。
   */
  destinationDivisionCodes: [] as Array<string[] | string>,
  /**
   * 字段 origin：表示表单、筛选条件、接口数据或组件状态中的 origin 值。
   */
  origin: '',
  /**
   * 字段 destination：表示表单、筛选条件、接口数据或组件状态中的 destination 值。
   */
  destination: '',
  /**
   * 字段 status：表示表单、筛选条件、接口数据或组件状态中的 status 值。
   */
  status: undefined as ShipmentStatus | undefined
})
/**
 * 常量 form：保存当前模块的页面状态、配置项、接口实例或计算结果。
 */
const form = reactive({
  /**
   * 字段 relatedOrderNo：表示表单、筛选条件、接口数据或组件状态中的 relatedOrderNo 值。
   */
  relatedOrderNo: '',
  /**
   * 字段 documentType：表示表单、筛选条件、接口数据或组件状态中的 documentType 值。
   */
  documentType: '采购发运',
  /**
   * 字段 transportMode：表示表单、筛选条件、接口数据或组件状态中的 transportMode 值。
   */
  transportMode: '公路运输',
  /**
   * 字段 projectCode：表示项目字典编码，保存物流单所属项目。
   */
  projectCode: '',
  /**
   * 字段 projectName：表示项目名称快照，保存物流单创建或修改时的项目名称。
   */
  projectName: '',
  /**
   * 字段 shippingOrganization：表示表单、筛选条件、接口数据或组件状态中的 shippingOrganization 值。
   */
  shippingOrganization: '',
  /**
   * 字段 receivingOrganization：表示表单、筛选条件、接口数据或组件状态中的 receivingOrganization 值。
   */
  receivingOrganization: '',
  /**
   * 字段 carrierName：表示表单、筛选条件、接口数据或组件状态中的 carrierName 值。
   */
  carrierName: '',
  /**
   * 字段 trackingNo：表示表单、筛选条件、接口数据或组件状态中的 trackingNo 值。
   */
  trackingNo: '',
  /**
   * 字段 driverName：表示表单、筛选条件、接口数据或组件状态中的 driverName 值。
   */
  driverName: '',
  /**
   * 字段 driverPhone：表示表单、筛选条件、接口数据或组件状态中的 driverPhone 值。
   */
  driverPhone: '',
  /**
   * 字段 vehicleNo：表示表单、筛选条件、接口数据或组件状态中的 vehicleNo 值。
   */
  vehicleNo: '',
  /**
   * 字段 originDivisionCode：表示表单、筛选条件、接口数据或组件状态中的 originDivisionCode 值。
   */
  originDivisionCode: [] as string[] | string,
  /**
   * 字段 originDivisionName：表示表单、筛选条件、接口数据或组件状态中的 originDivisionName 值。
   */
  originDivisionName: '',
  /**
   * 字段 destinationDivisionCode：表示表单、筛选条件、接口数据或组件状态中的 destinationDivisionCode 值。
   */
  destinationDivisionCode: [] as string[] | string,
  /**
   * 字段 destinationDivisionName：表示表单、筛选条件、接口数据或组件状态中的 destinationDivisionName 值。
   */
  destinationDivisionName: '',
  /**
   * 字段 origin：表示表单、筛选条件、接口数据或组件状态中的 origin 值。
   */
  origin: '',
  /**
   * 字段 destination：表示表单、筛选条件、接口数据或组件状态中的 destination 值。
   */
  destination: '',
  /**
   * 字段 plannedShipDate：表示表单、筛选条件、接口数据或组件状态中的 plannedShipDate 值。
   */
  plannedShipDate: new Date().toISOString().slice(0, 10),
  /**
   * 字段 remark：表示表单、筛选条件、接口数据或组件状态中的 remark 值。
   */
  remark: ''
})
/**
 * 常量 statusForm：保存当前模块的页面状态、配置项、接口实例或计算结果。
 */
const statusForm = reactive({
  /**
   * 字段 shipmentNo：表示表单、筛选条件、接口数据或组件状态中的 shipmentNo 值。
   */
  shipmentNo: '',
  /**
   * 字段 status：表示表单、筛选条件、接口数据或组件状态中的 status 值。
   */
  status: 'DISPATCHED' as ShipmentStatus,
  /**
   * 字段 relatedOrderNo：表示表单、筛选条件、接口数据或组件状态中的 relatedOrderNo 值。
   */
  relatedOrderNo: '',
  /**
   * 字段 documentType：表示表单、筛选条件、接口数据或组件状态中的 documentType 值。
   */
  documentType: '采购发运',
  /**
   * 字段 transportMode：表示表单、筛选条件、接口数据或组件状态中的 transportMode 值。
   */
  transportMode: '公路运输',
  /**
   * 字段 projectCode：表示项目字典编码，状态确认时继续保留物流单所属项目。
   */
  projectCode: '',
  /**
   * 字段 projectName：表示项目名称快照，状态确认时用于生成完整流水快照。
   */
  projectName: '',
  /**
   * 字段 shippingOrganization：表示表单、筛选条件、接口数据或组件状态中的 shippingOrganization 值。
   */
  shippingOrganization: '',
  /**
   * 字段 receivingOrganization：表示表单、筛选条件、接口数据或组件状态中的 receivingOrganization 值。
   */
  receivingOrganization: '',
  /**
   * 字段 carrierName：表示表单、筛选条件、接口数据或组件状态中的 carrierName 值。
   */
  carrierName: '',
  /**
   * 字段 trackingNo：表示表单、筛选条件、接口数据或组件状态中的 trackingNo 值。
   */
  trackingNo: '',
  /**
   * 字段 driverName：表示表单、筛选条件、接口数据或组件状态中的 driverName 值。
   */
  driverName: '',
  /**
   * 字段 driverPhone：表示表单、筛选条件、接口数据或组件状态中的 driverPhone 值。
   */
  driverPhone: '',
  /**
   * 字段 vehicleNo：表示表单、筛选条件、接口数据或组件状态中的 vehicleNo 值。
   */
  vehicleNo: '',
  /**
   * 字段 originDivisionCode：表示表单、筛选条件、接口数据或组件状态中的 originDivisionCode 值。
   */
  originDivisionCode: [] as string[] | string,
  /**
   * 字段 originDivisionName：表示表单、筛选条件、接口数据或组件状态中的 originDivisionName 值。
   */
  originDivisionName: '',
  /**
   * 字段 destinationDivisionCode：表示表单、筛选条件、接口数据或组件状态中的 destinationDivisionCode 值。
   */
  destinationDivisionCode: [] as string[] | string,
  /**
   * 字段 destinationDivisionName：表示表单、筛选条件、接口数据或组件状态中的 destinationDivisionName 值。
   */
  destinationDivisionName: '',
  /**
   * 字段 origin：表示表单、筛选条件、接口数据或组件状态中的 origin 值。
   */
  origin: '',
  /**
   * 字段 destination：表示表单、筛选条件、接口数据或组件状态中的 destination 值。
   */
  destination: '',
  /**
   * 字段 plannedShipDate：表示表单、筛选条件、接口数据或组件状态中的 plannedShipDate 值。
   */
  plannedShipDate: new Date().toISOString().slice(0, 10),
  /**
   * 字段 actualShipDate：表示表单、筛选条件、接口数据或组件状态中的 actualShipDate 值。
   */
  actualShipDate: '',
  /**
   * 字段 deliveredDate：表示表单、筛选条件、接口数据或组件状态中的 deliveredDate 值。
   */
  deliveredDate: '',
  /**
   * 字段 remark：表示表单、筛选条件、接口数据或组件状态中的 remark 值。
   */
  remark: '',
  /**
   * 字段 operationRemark：表示表单、筛选条件、接口数据或组件状态中的 operationRemark 值。
   */
  operationRemark: ''
})

/**
 * 生成物流表单字段级校验规则。
 *
 * 实现步骤：
 * 1. 对必填字段使用 Element Plus 规则，让错误出现在对应输入控件下方；
 * 2. 对司机姓名、联系电话、车牌号分别应用中文姓名、手机号/座机、普通和新能源车牌规则；
 * 3. 对地址、备注、关联单号按统一长度限制校验，blur 后触发红框提示。
 */
function shipmentRulesFor(getTransportMode: () => string | undefined): FormRules {
  return {
    carrierName: [{ required: true, message: '请选择承运商', trigger: 'change' }],
    projectCode: [{ required: true, message: '请选择项目', trigger: 'change' }],
    plannedShipDate: [{ required: true, message: '请选择计划发运日期', trigger: 'change' }],
    originDivisionCode: [{ required: true, message: '请选择发货区划', trigger: 'change' }],
    destinationDivisionCode: [{ required: true, message: '请选择目的区划', trigger: 'change' }],
    relatedOrderNo: [{ max: fieldLimits.sourceBillNo, message: `关联单号不能超过${fieldLimits.sourceBillNo}个字符`, trigger: 'blur' }],
    driverName: [{ pattern: chineseNamePattern, message: '姓名必须为1到20个中文字符', trigger: 'blur' }],
    driverPhone: [{ pattern: contactPhonePattern, message: '联系方式必须为手机号或座机号', trigger: 'blur' }],
    vehicleNo: [{
      validator: (_rule, value: string, callback) => {
        if (carrierInfoForMode(getTransportMode()).vehicleRequiresPlate && value && !vehicleNoPattern.test(value.trim().toUpperCase())) {
          callback(new Error('车牌号格式不正确，需兼容普通或新能源车牌'))
          return
        }
        callback()
      },
      trigger: 'blur'
    }],
    origin: [
      { required: true, message: '请输入发货详址', trigger: 'blur' },
      { max: fieldLimits.address, message: `发货详址不能超过${fieldLimits.address}个字符`, trigger: 'blur' }
    ],
    destination: [
      { required: true, message: '请输入目的详址', trigger: 'blur' },
      { max: fieldLimits.address, message: `目的详址不能超过${fieldLimits.address}个字符`, trigger: 'blur' }
    ],
    remark: [{ max: fieldLimits.remark, message: `物流备注不能超过${fieldLimits.remark}个字符`, trigger: 'blur' }],
    operationRemark: [{ max: fieldLimits.remark, message: `确认说明不能超过${fieldLimits.remark}个字符`, trigger: 'blur' }]
  }
}

/**
 * 常量 rules：物流新增/编辑表单规则，随运输方式动态决定车牌字段是否按车牌规则校验。
 */
const rules = computed<FormRules>(() => shipmentRulesFor(() => form.transportMode))

/**
 * 常量 statusRules：物流状态确认表单规则，随运输方式动态决定车牌字段是否按车牌规则校验。
 */
const statusRules = computed<FormRules>(() => ({
  ...shipmentRulesFor(() => statusForm.transportMode),
  status: [{ required: true, message: '请选择确认状态', trigger: 'change' }]
}))

/**
 * 常量 statusOptions：保存当前模块的页面状态、配置项、接口实例或计算结果。
 */
const statusOptions: Array<{ label: string; value: ShipmentStatus }> = [
  { label: '草稿', value: 'CREATED' },
  { label: '已发送', value: 'DISPATCHED' },
  { label: '运输中', value: 'IN_TRANSIT' },
  { label: '已送达', value: 'DELIVERED' },
  { label: '已取消', value: 'CANCELLED' }
]
/**
 * CarrierInfo 类型定义，用于描述不同运输方式下承运信息表单的字段文案和显示规则。
 */
interface CarrierInfo {
  /**
   * 字段 carrierLabel：表示承运主体字段标签。
   */
  carrierLabel: string
  /**
   * 字段 carrierPlaceholder：表示承运主体字段占位提示。
   */
  carrierPlaceholder: string
  /**
   * 字段 trackingLabel：表示运输单据编号字段标签。
   */
  trackingLabel: string
  /**
   * 字段 trackingPlaceholder：表示运输单据编号占位提示。
   */
  trackingPlaceholder: string
  /**
   * 字段 driverLabel：表示运输联系人字段标签。
   */
  driverLabel: string
  /**
   * 字段 driverPlaceholder：表示运输联系人占位提示。
   */
  driverPlaceholder: string
  /**
   * 字段 phoneLabel：表示运输联系人电话字段标签。
   */
  phoneLabel: string
  /**
   * 字段 phonePlaceholder：表示运输联系人电话占位提示。
   */
  phonePlaceholder: string
  /**
   * 字段 vehicleLabel：表示运输工具标识字段标签。
   */
  vehicleLabel: string
  /**
   * 字段 vehiclePlaceholder：表示运输工具标识占位提示。
   */
  vehiclePlaceholder: string
  /**
   * 字段 showVehicle：表示当前运输方式是否展示运输工具标识字段。
   */
  showVehicle: boolean
  /**
   * 字段 vehicleRequiresPlate：表示运输工具标识是否必须按车牌号规则校验。
   */
  vehicleRequiresPlate: boolean
}
/**
 * 常量 defaultCarrierInfo：保存公路运输和未知运输方式的默认承运信息表单配置。
 */
const defaultCarrierInfo: CarrierInfo = {
  carrierLabel: '承运商',
  carrierPlaceholder: '请选择承运商',
  trackingLabel: '运单号',
  trackingPlaceholder: '请输入承运商运单号',
  driverLabel: '司机',
  driverPlaceholder: '中文姓名',
  phoneLabel: '司机电话',
  phonePlaceholder: '手机号或座机号',
  vehicleLabel: '车牌号',
  vehiclePlaceholder: '普通或新能源车牌',
  showVehicle: true,
  vehicleRequiresPlate: true
}
/**
 * 常量 transportCarrierInfos：保存不同运输方式对应的承运信息字段展示配置。
 */
const transportCarrierInfos: Array<{ keywords: string[]; info: CarrierInfo }> = [
  {
    keywords: ['铁路', '火车', '高铁', '动车', '列车', '轨道'],
    info: {
      carrierLabel: '铁路承运方',
      carrierPlaceholder: '请选择铁路承运方',
      trackingLabel: '铁路运单号',
      trackingPlaceholder: '请输入铁路运单号',
      driverLabel: '跟单人',
      driverPlaceholder: '中文姓名',
      phoneLabel: '联系电话',
      phonePlaceholder: '手机号或座机号',
      vehicleLabel: '车次',
      vehiclePlaceholder: '如 G1234 / K56',
      showVehicle: true,
      vehicleRequiresPlate: false
    }
  },
  {
    keywords: ['航空', '飞机', '航班', '空运'],
    info: {
      carrierLabel: '航空公司',
      carrierPlaceholder: '请选择航空公司',
      trackingLabel: '空运单号',
      trackingPlaceholder: '请输入空运单号',
      driverLabel: '地面联系人',
      driverPlaceholder: '中文姓名',
      phoneLabel: '联系电话',
      phonePlaceholder: '手机号或座机号',
      vehicleLabel: '航班号',
      vehiclePlaceholder: '如 CA1234',
      showVehicle: true,
      vehicleRequiresPlate: false
    }
  },
  {
    keywords: ['水', '海运', '船', '航运', '江运', '河运'],
    info: {
      carrierLabel: '船运公司',
      carrierPlaceholder: '请选择船运公司',
      trackingLabel: '提单号',
      trackingPlaceholder: '请输入提单号',
      driverLabel: '联系人',
      driverPlaceholder: '中文姓名',
      phoneLabel: '联系电话',
      phonePlaceholder: '手机号或座机号',
      vehicleLabel: '船名/航次',
      vehiclePlaceholder: '如 COSCO STAR / V001',
      showVehicle: true,
      vehicleRequiresPlate: false
    }
  },
  {
    keywords: ['快递', '快运', '配送', '同城'],
    info: {
      carrierLabel: '快递公司',
      carrierPlaceholder: '请选择快递公司',
      trackingLabel: '快递单号',
      trackingPlaceholder: '请输入快递单号',
      driverLabel: '派送员',
      driverPlaceholder: '中文姓名',
      phoneLabel: '派送员电话',
      phonePlaceholder: '手机号或座机号',
      vehicleLabel: '派送车辆',
      vehiclePlaceholder: '可选填车牌或车辆编号',
      showVehicle: false,
      vehicleRequiresPlate: false
    }
  }
]

/**
 * 常量 canManageAttachment：保存当前模块的页面状态、配置项、接口实例或计算结果。
 */
const canManageAttachment = computed(() => auth.hasMenu('BTN_SHIPMENT_ATTACHMENT'))
/**
 * 常量 divisionCascaderProps：保存当前模块的页面状态、配置项、接口实例或计算结果。
 */
const divisionCascaderProps = { label: 'name', value: 'code', children: 'children', emitPath: true, checkStrictly: true, lazy: true, lazyLoad: loadDivisionNode }
/**
 * 常量 divisionSearchCascaderProps：保存当前模块的页面状态、配置项、接口实例或计算结果。
 */
const divisionSearchCascaderProps = { ...divisionCascaderProps, multiple: true }
/**
 * 常量 availableStatusOptions：保存当前模块的页面状态、配置项、接口实例或计算结果。
 */
const availableStatusOptions = computed(() => statusOptions.filter((item) => canChangeToStatus(statusCurrentStatus.value, item.value)))
/**
 * 常量 formCarrierInfo：根据新增/编辑表单运输方式动态计算承运信息字段配置。
 */
const formCarrierInfo = computed(() => carrierInfoForMode(form.transportMode))
/**
 * 常量 statusCarrierInfo：根据状态确认表单运输方式动态计算承运信息字段配置。
 */
const statusCarrierInfo = computed(() => carrierInfoForMode(statusForm.transportMode))

/**
 * 执行 statusLabel 方法。
 * 
 * 实现步骤：
 * 1. 读取当前页面状态或调用参数；
 * 2. 完成对应的校验、接口调用或数据转换；
 * 3. 更新页面状态或返回处理结果。
 */
function statusLabel(value?: ShipmentStatus) {
  if (!value) {
    return '无'
  }
  return statusOptions.find((item) => item.value === value)?.label || value
}

/**
 * 根据运输方式获取承运信息字段配置。
 *
 * 实现步骤：
 * 1. 把运输方式名称统一转成字符串；
 * 2. 按关键字匹配铁路、航空、水运、快递等专属表单配置；
 * 3. 未命中时返回公路运输默认配置。
 */
function carrierInfoForMode(mode?: string) {
  /** 归一化后的运输方式文本，用于关键字匹配承运字段配置。 */
  const normalizedMode = String(mode || '')
  return transportCarrierInfos.find((item) => item.keywords.some((keyword) => normalizedMode.includes(keyword)))?.info || defaultCarrierInfo
}

/**
 * 处理运输方式切换。
 *
 * 实现步骤：
 * 1. 根据切换后的运输方式读取字段显示配置；
 * 2. 如果当前运输方式不展示运输工具标识，则清空旧车牌/车次/航班值；
 * 3. 其余已录入的承运商、运单和联系人信息保留，避免用户误切换后丢数据。
 */
function onTransportModeChange(target: { transportMode?: string; vehicleNo?: string }) {
  /** 当前运输方式对应的承运配置，用于决定是否保留运输工具标识。 */
  const carrierInfo = carrierInfoForMode(target.transportMode)
  if (!carrierInfo.showVehicle) {
    target.vehicleNo = ''
  }
  formRef.value?.clearValidate('vehicleNo')
  statusFormRef.value?.clearValidate('vehicleNo')
}

/**
 * 执行 statusOrder 方法。
 * 
 * 实现步骤：
 * 1. 读取当前页面状态或调用参数；
 * 2. 完成对应的校验、接口调用或数据转换；
 * 3. 更新页面状态或返回处理结果。
 */
function statusOrder(value: ShipmentStatus) {
  /**
   * 常量 orderMap：保存当前模块的页面状态、配置项、接口实例或计算结果。
   */
  const orderMap: Record<ShipmentStatus, number> = {
    /**
     * 字段 CREATED：表示表单、筛选条件、接口数据或组件状态中的 CREATED 值。
     */
    CREATED: 10,
    /**
     * 字段 DISPATCHED：表示表单、筛选条件、接口数据或组件状态中的 DISPATCHED 值。
     */
    DISPATCHED: 20,
    /**
     * 字段 IN_TRANSIT：表示表单、筛选条件、接口数据或组件状态中的 IN_TRANSIT 值。
     */
    IN_TRANSIT: 30,
    /**
     * 字段 DELIVERED：表示表单、筛选条件、接口数据或组件状态中的 DELIVERED 值。
     */
    DELIVERED: 40,
    /**
     * 字段 CANCELLED：表示表单、筛选条件、接口数据或组件状态中的 CANCELLED 值。
     */
    CANCELLED: 99
  }
  return orderMap[value]
}

/**
 * 执行 statusReadonly 方法。
 * 
 * 实现步骤：
 * 1. 读取当前页面状态或调用参数；
 * 2. 完成对应的校验、接口调用或数据转换；
 * 3. 更新页面状态或返回处理结果。
 */
function statusReadonly(value: ShipmentStatus) {
  return value === 'DELIVERED' || value === 'CANCELLED'
}

/**
 * 执行 canEditShipment 方法。
 * 
 * 实现步骤：
 * 1. 读取当前页面状态或调用参数；
 * 2. 完成对应的校验、接口调用或数据转换；
 * 3. 更新页面状态或返回处理结果。
 */
function canEditShipment(value: ShipmentStatus) {
  return value === 'CREATED'
}

/**
 * 执行 canConfirmShipmentStatus 方法。
 * 
 * 实现步骤：
 * 1. 读取当前页面状态或调用参数；
 * 2. 完成对应的校验、接口调用或数据转换；
 * 3. 更新页面状态或返回处理结果。
 */
function canConfirmShipmentStatus(value: ShipmentStatus) {
  return !statusReadonly(value)
}

/**
 * 执行 canChangeToStatus 方法。
 * 
 * 实现步骤：
 * 1. 读取当前页面状态或调用参数；
 * 2. 完成对应的校验、接口调用或数据转换；
 * 3. 更新页面状态或返回处理结果。
 */
function canChangeToStatus(current: ShipmentStatus, target: ShipmentStatus) {
  if (statusReadonly(current)) {
    return false
  }
  return statusOrder(target) > statusOrder(current)
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
    const [shipmentRows, carriers] = await Promise.all([
      api.shipments(shipmentSearchParams()),
      api.enabledDictionaryChildren('CARRIER')
    ])
    shipments.value = shipmentRows
    selectedRows.value = []
    carrierOptions.value = carriers
  } finally {
    loading.value = false
  }
}

/**
 * 执行 loadDivisionOptions 方法。
 * 
 * 实现步骤：
 * 1. 读取当前页面状态或调用参数；
 * 2. 完成对应的校验、接口调用或数据转换；
 * 3. 更新页面状态或返回处理结果。
 */
async function loadDivisionOptions() {
  divisionOptions.value = await api.enabledDictionaryChildren('ADMINISTRATIVE_DIVISION')
}

/**
 * 懒加载行政区划级联节点。
 *
 * 实现步骤：
 * 1. 根节点由 loadDivisionOptions 预先加载省级选项；
 * 2. 展开省、市等节点时按节点 ID 请求下一层启用子级；
 * 3. 没有子级时返回空数组，级联组件把该节点视为叶子节点。
 */
async function loadDivisionNode(node: { level: number; data?: BasicDictionaryView }, resolve: (data: BasicDictionaryView[]) => void) {
  if (node.level === 0) {
    if (divisionOptions.value.length === 0) {
      await loadDivisionOptions()
    }
    resolve(divisionOptions.value)
    return
  }
  const parentId = node.data?.id
  if (!parentId) {
    resolve([])
    return
  }
  try {
    resolve(await api.enabledDictionaryChildrenByParent(parentId))
  } catch {
    resolve([])
  }
}

/**
 * 确保指定行政区划编码路径已加载到本地级联树。
 *
 * 实现步骤：
 * 1. 先加载省级根选项；
 * 2. 按编码路径逐级查找当前节点；
 * 3. 如果当前节点还有子级但未加载，按节点 ID 加载下一层并写回 children。
 */
async function ensureDivisionPathLoaded(codePath?: string | string[]) {
  const codes = splitCascadePath(codePath)
  if (codes.length === 0) {
    return
  }
  if (divisionOptions.value.length === 0) {
    await loadDivisionOptions()
  }
  let currentNodes = divisionOptions.value
  for (const code of codes) {
    const current = currentNodes.find((node) => node.code === code)
    if (!current) {
      return
    }
    if (current.hasChildren && (!current.children || current.children.length === 0)) {
      current.children = await api.enabledDictionaryChildrenByParent(current.id)
    }
    currentNodes = current.children || []
  }
}

/**
 * 批量补齐多个行政区划编码路径。
 *
 * 实现步骤：
 * 1. 过滤空路径；
 * 2. 并行加载每条路径缺失的层级；
 * 3. 用于打开编辑和状态确认弹窗前保证回显能显示完整名称。
 */
async function ensureDivisionPathsLoaded(...paths: Array<string | string[] | undefined>) {
  await Promise.all(paths.map((path) => ensureDivisionPathLoaded(path)))
}

/**
 * 加载项目字典选项。
 *
 * 实现步骤：
 * 1. 从基础字典读取启用的 PROJECT 子项；
 * 2. 保存到项目下拉数据源；
 * 3. 物流查询、主表保存和状态确认流水共用该项目维度。
 */
async function loadProjectOptions() {
  projectOptions.value = await api.enabledDictionaryChildren('PROJECT')
}

/**
 * 加载物流业务表单字典。
 *
 * 实现步骤：
 * 1. 并行读取组织、物流单据类型和运输方式；
 * 2. 组织按树形层级转换为下拉展示选项，保存时仍保存名称；
 * 3. 给单据类型和运输方式补默认项，兼容尚未初始化新字典的环境。
 */
async function loadBusinessDictionaryOptions() {
  const [organizations, logisticsDocumentTypes, transportModes] = await Promise.all([
    api.enabledDictionaryTree('ORGANIZATION'),
    api.enabledDictionaryChildren('LOGISTICS_DOCUMENT_TYPE'),
    api.enabledDictionaryChildren('TRANSPORT_MODE')
  ])
  organizationOptions.value = flattenDictionaryOptions(organizations)
  logisticsDocumentTypeOptions.value = withFallbackDictionaryOption(logisticsDocumentTypes, 'LOGISTICS_DOC_PURCHASE', '采购发运')
  transportModeOptions.value = withFallbackDictionaryOption(transportModes, 'TRANSPORT_ROAD', '公路运输')
}

/**
 * 执行 findDivisionPath 方法。
 * 
 * 实现步骤：
 * 1. 读取当前页面状态或调用参数；
 * 2. 完成对应的校验、接口调用或数据转换；
 * 3. 更新页面状态或返回处理结果。
 */
function findDivisionPath(nodes: BasicDictionaryView[], codePath: string | string[]): { codePath: string; namePath: string } {
  /**
   * 常量 codes：保存当前模块的页面状态、配置项、接口实例或计算结果。
   */
  const codes = Array.isArray(codePath) ? codePath : splitCascadePath(codePath)
  /**
   * 常量 names：保存当前模块的页面状态、配置项、接口实例或计算结果。
   */
  const names: string[] = []
  /**
   * 变量 currentNodes：保存当前模块的页面状态、配置项、接口实例或计算结果。
   */
  let currentNodes = nodes
  /**
   * 常量 resolvedCodes：保存当前模块的页面状态、配置项、接口实例或计算结果。
   */
  const resolvedCodes: string[] = []
  for (const code of codes) {
    /**
     * 常量 current：保存当前模块的页面状态、配置项、接口实例或计算结果。
     */
    const current = currentNodes.find((node) => node.code === code)
    if (!current) {
      break
    }
    resolvedCodes.push(current.code)
    names.push(current.name)
    currentNodes = current.children || []
  }
  return { codePath: resolvedCodes.join('/'), namePath: names.join('/') }
}

/**
 * 执行 splitCascadePath 方法。
 * 
 * 实现步骤：
 * 1. 读取当前页面状态或调用参数；
 * 2. 完成对应的校验、接口调用或数据转换；
 * 3. 更新页面状态或返回处理结果。
 */
function splitCascadePath(value?: string | string[]) {
  return Array.isArray(value)
    ? value
    : String(value || '').split('/').map((item) => item.trim()).filter(Boolean)
}

/**
 * 执行 normalizeCascadeValue 方法。
 * 
 * 实现步骤：
 * 1. 读取当前页面状态或调用参数；
 * 2. 完成对应的校验、接口调用或数据转换；
 * 3. 更新页面状态或返回处理结果。
 */
function normalizeCascadeValue(value?: string | string[]) {
  return splitCascadePath(value).join('/')
}

/**
 * 执行 onOriginDivisionChange 方法。
 * 
 * 实现步骤：
 * 1. 读取当前页面状态或调用参数；
 * 2. 完成对应的校验、接口调用或数据转换；
 * 3. 更新页面状态或返回处理结果。
 */
function onOriginDivisionChange(codePath: string[]) {
  /**
   * 常量 selected：保存当前模块的页面状态、配置项、接口实例或计算结果。
   */
  const selected = findDivisionPath(divisionOptions.value, codePath)
  form.originDivisionName = selected.namePath
}

/**
 * 执行 onDestinationDivisionChange 方法。
 * 
 * 实现步骤：
 * 1. 读取当前页面状态或调用参数；
 * 2. 完成对应的校验、接口调用或数据转换；
 * 3. 更新页面状态或返回处理结果。
 */
function onDestinationDivisionChange(codePath: string[]) {
  /**
   * 常量 selected：保存当前模块的页面状态、配置项、接口实例或计算结果。
   */
  const selected = findDivisionPath(divisionOptions.value, codePath)
  form.destinationDivisionName = selected.namePath
}

/**
 * 执行 onStatusOriginDivisionChange 方法。
 * 
 * 实现步骤：
 * 1. 读取当前页面状态或调用参数；
 * 2. 完成对应的校验、接口调用或数据转换；
 * 3. 更新页面状态或返回处理结果。
 */
function onStatusOriginDivisionChange(codePath: string[]) {
  /**
   * 常量 selected：保存当前模块的页面状态、配置项、接口实例或计算结果。
   */
  const selected = findDivisionPath(divisionOptions.value, codePath)
  statusForm.originDivisionName = selected.namePath
}

/**
 * 执行 onStatusDestinationDivisionChange 方法。
 * 
 * 实现步骤：
 * 1. 读取当前页面状态或调用参数；
 * 2. 完成对应的校验、接口调用或数据转换；
 * 3. 更新页面状态或返回处理结果。
 */
function onStatusDestinationDivisionChange(codePath: string[]) {
  /**
   * 常量 selected：保存当前模块的页面状态、配置项、接口实例或计算结果。
   */
  const selected = findDivisionPath(divisionOptions.value, codePath)
  statusForm.destinationDivisionName = selected.namePath
}

/**
 * 根据项目编码同步项目名称快照。
 *
 * 实现步骤：
 * 1. 在项目字典选项中查找当前编码；
 * 2. 找到项目时写入名称快照，清空项目时清空名称；
 * 3. 主表保存和状态确认提交时都携带名称快照，保证流水展示完整。
 */
function onProjectChange(target: { projectCode?: string; projectName?: string }) {
  target.projectName = projectOptions.value.find((item) => item.code === target.projectCode)?.name || ''
}

/**
 * 执行 shipmentSearchParams 方法。
 * 
 * 实现步骤：
 * 1. 读取当前页面状态或调用参数；
 * 2. 完成对应的校验、接口调用或数据转换；
 * 3. 更新页面状态或返回处理结果。
 */
function shipmentSearchParams() {
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
     * 字段 shipmentNo：表示表单、筛选条件、接口数据或组件状态中的 shipmentNo 值。
     */
    shipmentNo: filters.shipmentNo.trim() || undefined,
    /**
     * 字段 relatedOrderNo：表示表单、筛选条件、接口数据或组件状态中的 relatedOrderNo 值。
     */
    relatedOrderNo: filters.relatedOrderNo.trim() || undefined,
    /**
     * 字段 projectCode：表示项目字典编码，用于按项目筛选物流单。
     */
    projectCode: filters.projectCode || undefined,
    /**
     * 字段 carrierName：表示表单、筛选条件、接口数据或组件状态中的 carrierName 值。
     */
    carrierName: filters.carrierName || undefined,
    /**
     * 字段 trackingNo：表示表单、筛选条件、接口数据或组件状态中的 trackingNo 值。
     */
    trackingNo: filters.trackingNo.trim() || undefined,
    /**
     * 字段 originDivisionCodes：表示表单、筛选条件、接口数据或组件状态中的 originDivisionCodes 值。
     */
    originDivisionCodes: filters.originDivisionCodes.length > 0 ? filters.originDivisionCodes.map(normalizeCascadeValue).join(',') : undefined,
    /**
     * 字段 destinationDivisionCodes：表示表单、筛选条件、接口数据或组件状态中的 destinationDivisionCodes 值。
     */
    destinationDivisionCodes: filters.destinationDivisionCodes.length > 0 ? filters.destinationDivisionCodes.map(normalizeCascadeValue).join(',') : undefined,
    /**
     * 字段 origin：表示表单、筛选条件、接口数据或组件状态中的 origin 值。
     */
    origin: filters.origin.trim() || undefined,
    /**
     * 字段 destination：表示表单、筛选条件、接口数据或组件状态中的 destination 值。
     */
    destination: filters.destination.trim() || undefined,
    /**
     * 字段 status：表示表单、筛选条件、接口数据或组件状态中的 status 值。
     */
    status: filters.status
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
     * 字段 shipmentNo：表示表单、筛选条件、接口数据或组件状态中的 shipmentNo 值。
     */
    shipmentNo: '',
    /**
     * 字段 relatedOrderNo：表示表单、筛选条件、接口数据或组件状态中的 relatedOrderNo 值。
     */
    relatedOrderNo: '',
    /**
     * 字段 projectCode：表示项目字典编码，用于清空项目筛选条件。
     */
    projectCode: '',
    /**
     * 字段 carrierName：表示表单、筛选条件、接口数据或组件状态中的 carrierName 值。
     */
    carrierName: '',
    /**
     * 字段 trackingNo：表示表单、筛选条件、接口数据或组件状态中的 trackingNo 值。
     */
    trackingNo: '',
    /**
     * 字段 originDivisionCodes：表示表单、筛选条件、接口数据或组件状态中的 originDivisionCodes 值。
     */
    originDivisionCodes: [],
    /**
     * 字段 destinationDivisionCodes：表示表单、筛选条件、接口数据或组件状态中的 destinationDivisionCodes 值。
     */
    destinationDivisionCodes: [],
    /**
     * 字段 origin：表示表单、筛选条件、接口数据或组件状态中的 origin 值。
     */
    origin: '',
    /**
     * 字段 destination：表示表单、筛选条件、接口数据或组件状态中的 destination 值。
     */
    destination: '',
    /**
     * 字段 status：表示表单、筛选条件、接口数据或组件状态中的 status 值。
     */
    status: undefined
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
function handleSelectionChange(selection: ShipmentView[]) {
  selectedRows.value = selection
}

/**
 * 导出物流单列表。
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
      : shipmentSearchParams()
    const { blob, filename } = await api.exportShipments(payload)
    saveBlob(blob, filename || '物流管理.xlsx')
    ElMessage.success('导出成功')
  } finally {
    exporting.value = false
  }
}

/**
 * 重置物流新增表单。
 *
 * 实现步骤：
 * 1. 清空来源单号、承运信息和收发地址；
 * 2. 恢复单据类型、运输方式和计划发运日期默认值；
 * 3. 清空行政区划级联值，避免上一次新增残留。
 */
function reset() {
  Object.assign(form, {
    /**
     * 字段 relatedOrderNo：表示表单、筛选条件、接口数据或组件状态中的 relatedOrderNo 值。
     */
    relatedOrderNo: '',
    /**
     * 字段 documentType：表示表单、筛选条件、接口数据或组件状态中的 documentType 值。
     */
    documentType: '采购发运',
    /**
     * 字段 transportMode：表示表单、筛选条件、接口数据或组件状态中的 transportMode 值。
     */
    transportMode: '公路运输',
    /**
     * 字段 projectCode：表示项目字典编码，新增物流单时默认为空。
     */
    projectCode: '',
    /**
     * 字段 projectName：表示项目名称快照，新增物流单时默认为空。
     */
    projectName: '',
    /**
     * 字段 shippingOrganization：表示表单、筛选条件、接口数据或组件状态中的 shippingOrganization 值。
     */
    shippingOrganization: '',
    /**
     * 字段 receivingOrganization：表示表单、筛选条件、接口数据或组件状态中的 receivingOrganization 值。
     */
    receivingOrganization: '',
    /**
     * 字段 carrierName：表示表单、筛选条件、接口数据或组件状态中的 carrierName 值。
     */
    carrierName: '',
    /**
     * 字段 trackingNo：表示表单、筛选条件、接口数据或组件状态中的 trackingNo 值。
     */
    trackingNo: '',
    /**
     * 字段 driverName：表示表单、筛选条件、接口数据或组件状态中的 driverName 值。
     */
    driverName: '',
    /**
     * 字段 driverPhone：表示表单、筛选条件、接口数据或组件状态中的 driverPhone 值。
     */
    driverPhone: '',
    /**
     * 字段 vehicleNo：表示表单、筛选条件、接口数据或组件状态中的 vehicleNo 值。
     */
    vehicleNo: '',
    /**
     * 字段 originDivisionCode：表示表单、筛选条件、接口数据或组件状态中的 originDivisionCode 值。
     */
    originDivisionCode: [],
    /**
     * 字段 originDivisionName：表示表单、筛选条件、接口数据或组件状态中的 originDivisionName 值。
     */
    originDivisionName: '',
    /**
     * 字段 destinationDivisionCode：表示表单、筛选条件、接口数据或组件状态中的 destinationDivisionCode 值。
     */
    destinationDivisionCode: [],
    /**
     * 字段 destinationDivisionName：表示表单、筛选条件、接口数据或组件状态中的 destinationDivisionName 值。
     */
    destinationDivisionName: '',
    /**
     * 字段 origin：表示表单、筛选条件、接口数据或组件状态中的 origin 值。
     */
    origin: '',
    /**
     * 字段 destination：表示表单、筛选条件、接口数据或组件状态中的 destination 值。
     */
    destination: '',
    /**
     * 字段 plannedShipDate：表示表单、筛选条件、接口数据或组件状态中的 plannedShipDate 值。
     */
    plannedShipDate: new Date().toISOString().slice(0, 10),
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
  editingId.value = undefined
  attachmentRef.value?.reset()
  reset()
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
async function openEdit(row: ShipmentView) {
  await refreshDictionaryOptions()
  await ensureDivisionPathsLoaded(row.originDivisionCode, row.destinationDivisionCode)
  editingId.value = row.id
  Object.assign(form, {
    /**
     * 字段 relatedOrderNo：表示表单、筛选条件、接口数据或组件状态中的 relatedOrderNo 值。
     */
    relatedOrderNo: row.relatedOrderNo || '',
    /**
     * 字段 documentType：表示表单、筛选条件、接口数据或组件状态中的 documentType 值。
     */
    documentType: row.documentType || '采购发运',
    /**
     * 字段 transportMode：表示表单、筛选条件、接口数据或组件状态中的 transportMode 值。
     */
    transportMode: row.transportMode || '公路运输',
    /**
     * 字段 projectCode：表示项目字典编码，用于回填物流单所属项目。
     */
    projectCode: row.projectCode || '',
    /**
     * 字段 projectName：表示项目名称快照，用于回填物流单所属项目名称。
     */
    projectName: row.projectName || '',
    /**
     * 字段 shippingOrganization：表示表单、筛选条件、接口数据或组件状态中的 shippingOrganization 值。
     */
    shippingOrganization: row.shippingOrganization || '',
    /**
     * 字段 receivingOrganization：表示表单、筛选条件、接口数据或组件状态中的 receivingOrganization 值。
     */
    receivingOrganization: row.receivingOrganization || '',
    /**
     * 字段 carrierName：表示表单、筛选条件、接口数据或组件状态中的 carrierName 值。
     */
    carrierName: row.carrierName,
    /**
     * 字段 trackingNo：表示表单、筛选条件、接口数据或组件状态中的 trackingNo 值。
     */
    trackingNo: row.trackingNo || '',
    /**
     * 字段 driverName：表示表单、筛选条件、接口数据或组件状态中的 driverName 值。
     */
    driverName: row.driverName || '',
    /**
     * 字段 driverPhone：表示表单、筛选条件、接口数据或组件状态中的 driverPhone 值。
     */
    driverPhone: row.driverPhone || '',
    /**
     * 字段 vehicleNo：表示表单、筛选条件、接口数据或组件状态中的 vehicleNo 值。
     */
    vehicleNo: row.vehicleNo || '',
    /**
     * 字段 originDivisionCode：表示表单、筛选条件、接口数据或组件状态中的 originDivisionCode 值。
     */
    originDivisionCode: splitCascadePath(row.originDivisionCode),
    /**
     * 字段 originDivisionName：表示表单、筛选条件、接口数据或组件状态中的 originDivisionName 值。
     */
    originDivisionName: row.originDivisionName || '',
    /**
     * 字段 destinationDivisionCode：表示表单、筛选条件、接口数据或组件状态中的 destinationDivisionCode 值。
     */
    destinationDivisionCode: splitCascadePath(row.destinationDivisionCode),
    /**
     * 字段 destinationDivisionName：表示表单、筛选条件、接口数据或组件状态中的 destinationDivisionName 值。
     */
    destinationDivisionName: row.destinationDivisionName || '',
    /**
     * 字段 origin：表示表单、筛选条件、接口数据或组件状态中的 origin 值。
     */
    origin: row.origin,
    /**
     * 字段 destination：表示表单、筛选条件、接口数据或组件状态中的 destination 值。
     */
    destination: row.destination,
    /**
     * 字段 plannedShipDate：表示表单、筛选条件、接口数据或组件状态中的 plannedShipDate 值。
     */
    plannedShipDate: row.plannedShipDate,
    /**
     * 字段 remark：表示表单、筛选条件、接口数据或组件状态中的 remark 值。
     */
    remark: row.remark || ''
  })
  await attachmentRef.value?.reload(row.id)
  dialogVisible.value = true
}

/**
 * 保存物流单。
 *
 * 实现步骤：
 * 1. 校验承运商、收发区划、收发详址和计划发运日期；
 * 2. 将级联区划数组转换为后端统一保存的编码路径和名称路径；
 * 3. 连同单据类型、运输方式、组织、司机车辆等采集项一起提交；
 * 4. 新增或编辑成功后上传暂存附件并刷新列表。
 */
async function save() {
  /** 物流单表单校验结果，失败时字段下方显示错误并阻止保存。 */
  const valid = await formRef.value?.validate().catch(() => false)
  if (!valid) {
    return
  }
  await ensureDivisionPathsLoaded(form.originDivisionCode, form.destinationDivisionCode)
  /**
   * 常量 originDivision：保存当前模块的页面状态、配置项、接口实例或计算结果。
   */
  const originDivision = findDivisionPath(divisionOptions.value, form.originDivisionCode)
  /**
   * 常量 destinationDivision：保存当前模块的页面状态、配置项、接口实例或计算结果。
   */
  const destinationDivision = findDivisionPath(divisionOptions.value, form.destinationDivisionCode)
  /**
   * 常量 payload：保存当前模块的页面状态、配置项、接口实例或计算结果。
   */
  const payload = {
    ...form,
    /**
     * 字段 originDivisionCode：表示表单、筛选条件、接口数据或组件状态中的 originDivisionCode 值。
     */
    originDivisionCode: originDivision.codePath,
    /**
     * 字段 originDivisionName：表示表单、筛选条件、接口数据或组件状态中的 originDivisionName 值。
     */
    originDivisionName: originDivision.namePath,
    /**
     * 字段 destinationDivisionCode：表示表单、筛选条件、接口数据或组件状态中的 destinationDivisionCode 值。
     */
    destinationDivisionCode: destinationDivision.codePath,
    /**
     * 字段 destinationDivisionName：表示表单、筛选条件、接口数据或组件状态中的 destinationDivisionName 值。
     */
    destinationDivisionName: destinationDivision.namePath
  }
  /**
   * 变量 saved：保存当前模块的页面状态、配置项、接口实例或计算结果。
   */
  let saved: ShipmentView
  if (editingId.value) {
    saved = await api.updateShipment(editingId.value, payload)
  } else {
    saved = await api.createShipment(payload)
  }
  await attachmentRef.value?.uploadPending(saved.id)
  ElMessage.success('保存成功')
  dialogVisible.value = false
  await load()
}

/**
 * 打开物流状态确认弹窗。
 *
 * 实现步骤：
 * 1. 记录当前物流单 ID 和当前状态；
 * 2. 计算可流转的下一状态；
 * 3. 将主表最新的承运、组织、司机车辆、地址和日期字段带入确认表单。
 */
async function openStatusConfirm(row: ShipmentView) {
  await refreshDictionaryOptions()
  await ensureDivisionPathsLoaded(row.originDivisionCode, row.destinationDivisionCode)
  statusEditingId.value = row.id
  statusCurrentStatus.value = row.status
  /**
   * 常量 nextStatus：保存当前模块的页面状态、配置项、接口实例或计算结果。
   */
  const nextStatus = statusOptions.find((item) => canChangeToStatus(row.status, item.value))?.value || row.status
  Object.assign(statusForm, {
    /**
     * 字段 shipmentNo：表示表单、筛选条件、接口数据或组件状态中的 shipmentNo 值。
     */
    shipmentNo: row.shipmentNo,
    /**
     * 字段 status：表示表单、筛选条件、接口数据或组件状态中的 status 值。
     */
    status: nextStatus,
    /**
     * 字段 relatedOrderNo：表示表单、筛选条件、接口数据或组件状态中的 relatedOrderNo 值。
     */
    relatedOrderNo: row.relatedOrderNo || '',
    /**
     * 字段 documentType：表示表单、筛选条件、接口数据或组件状态中的 documentType 值。
     */
    documentType: row.documentType || '采购发运',
    /**
     * 字段 transportMode：表示表单、筛选条件、接口数据或组件状态中的 transportMode 值。
     */
    transportMode: row.transportMode || '公路运输',
    /**
     * 字段 projectCode：表示项目字典编码，用于状态确认时保留物流单所属项目。
     */
    projectCode: row.projectCode || '',
    /**
     * 字段 projectName：表示项目名称快照，用于状态确认流水展示项目。
     */
    projectName: row.projectName || '',
    /**
     * 字段 shippingOrganization：表示表单、筛选条件、接口数据或组件状态中的 shippingOrganization 值。
     */
    shippingOrganization: row.shippingOrganization || '',
    /**
     * 字段 receivingOrganization：表示表单、筛选条件、接口数据或组件状态中的 receivingOrganization 值。
     */
    receivingOrganization: row.receivingOrganization || '',
    /**
     * 字段 carrierName：表示表单、筛选条件、接口数据或组件状态中的 carrierName 值。
     */
    carrierName: row.carrierName,
    /**
     * 字段 trackingNo：表示表单、筛选条件、接口数据或组件状态中的 trackingNo 值。
     */
    trackingNo: row.trackingNo || '',
    /**
     * 字段 driverName：表示表单、筛选条件、接口数据或组件状态中的 driverName 值。
     */
    driverName: row.driverName || '',
    /**
     * 字段 driverPhone：表示表单、筛选条件、接口数据或组件状态中的 driverPhone 值。
     */
    driverPhone: row.driverPhone || '',
    /**
     * 字段 vehicleNo：表示表单、筛选条件、接口数据或组件状态中的 vehicleNo 值。
     */
    vehicleNo: row.vehicleNo || '',
    /**
     * 字段 originDivisionCode：表示表单、筛选条件、接口数据或组件状态中的 originDivisionCode 值。
     */
    originDivisionCode: splitCascadePath(row.originDivisionCode),
    /**
     * 字段 originDivisionName：表示表单、筛选条件、接口数据或组件状态中的 originDivisionName 值。
     */
    originDivisionName: row.originDivisionName || '',
    /**
     * 字段 destinationDivisionCode：表示表单、筛选条件、接口数据或组件状态中的 destinationDivisionCode 值。
     */
    destinationDivisionCode: splitCascadePath(row.destinationDivisionCode),
    /**
     * 字段 destinationDivisionName：表示表单、筛选条件、接口数据或组件状态中的 destinationDivisionName 值。
     */
    destinationDivisionName: row.destinationDivisionName || '',
    /**
     * 字段 origin：表示表单、筛选条件、接口数据或组件状态中的 origin 值。
     */
    origin: row.origin,
    /**
     * 字段 destination：表示表单、筛选条件、接口数据或组件状态中的 destination 值。
     */
    destination: row.destination,
    /**
     * 字段 plannedShipDate：表示表单、筛选条件、接口数据或组件状态中的 plannedShipDate 值。
     */
    plannedShipDate: row.plannedShipDate,
    /**
     * 字段 actualShipDate：表示表单、筛选条件、接口数据或组件状态中的 actualShipDate 值。
     */
    actualShipDate: row.actualShipDate || '',
    /**
     * 字段 deliveredDate：表示表单、筛选条件、接口数据或组件状态中的 deliveredDate 值。
     */
    deliveredDate: row.deliveredDate || '',
    /**
     * 字段 remark：表示表单、筛选条件、接口数据或组件状态中的 remark 值。
     */
    remark: row.remark || '',
    /**
     * 字段 operationRemark：表示表单、筛选条件、接口数据或组件状态中的 operationRemark 值。
     */
    operationRemark: ''
  })
  statusDialogVisible.value = true
}

/**
 * 确认物流状态。
 *
 * 实现步骤：
 * 1. 校验目标状态、承运商、收发区划、收发详址和计划发运日期；
 * 2. 将发货和目的区划转换为编码路径与名称路径；
 * 3. 提交状态确认，同时保留组织、运输方式、司机车辆等采集字段；
 * 4. 成功后关闭弹窗并刷新物流列表。
 */
async function confirmStatus() {
  if (!statusEditingId.value) {
    return
  }
  /** 物流状态确认表单校验结果，失败时字段下方显示错误并阻止提交。 */
  const valid = await statusFormRef.value?.validate().catch(() => false)
  if (!valid) {
    return
  }
  await ensureDivisionPathsLoaded(statusForm.originDivisionCode, statusForm.destinationDivisionCode)
  /**
   * 常量 originDivision：保存当前模块的页面状态、配置项、接口实例或计算结果。
   */
  const originDivision = findDivisionPath(divisionOptions.value, statusForm.originDivisionCode)
  /**
   * 常量 destinationDivision：保存当前模块的页面状态、配置项、接口实例或计算结果。
   */
  const destinationDivision = findDivisionPath(divisionOptions.value, statusForm.destinationDivisionCode)
  await api.confirmShipmentStatus(statusEditingId.value, {
    /**
     * 字段 status：表示表单、筛选条件、接口数据或组件状态中的 status 值。
     */
    status: statusForm.status,
    /**
     * 字段 relatedOrderNo：表示表单、筛选条件、接口数据或组件状态中的 relatedOrderNo 值。
     */
    relatedOrderNo: statusForm.relatedOrderNo,
    /**
     * 字段 documentType：表示表单、筛选条件、接口数据或组件状态中的 documentType 值。
     */
    documentType: statusForm.documentType,
    /**
     * 字段 transportMode：表示表单、筛选条件、接口数据或组件状态中的 transportMode 值。
     */
    transportMode: statusForm.transportMode,
    /**
     * 字段 projectCode：表示项目字典编码，用于保存状态确认后的物流项目维度。
     */
    projectCode: statusForm.projectCode || undefined,
    /**
     * 字段 projectName：表示项目名称快照，用于保存状态确认后的物流项目展示值。
     */
    projectName: statusForm.projectName || undefined,
    /**
     * 字段 shippingOrganization：表示表单、筛选条件、接口数据或组件状态中的 shippingOrganization 值。
     */
    shippingOrganization: statusForm.shippingOrganization,
    /**
     * 字段 receivingOrganization：表示表单、筛选条件、接口数据或组件状态中的 receivingOrganization 值。
     */
    receivingOrganization: statusForm.receivingOrganization,
    /**
     * 字段 carrierName：表示表单、筛选条件、接口数据或组件状态中的 carrierName 值。
     */
    carrierName: statusForm.carrierName,
    /**
     * 字段 trackingNo：表示表单、筛选条件、接口数据或组件状态中的 trackingNo 值。
     */
    trackingNo: statusForm.trackingNo,
    /**
     * 字段 driverName：表示表单、筛选条件、接口数据或组件状态中的 driverName 值。
     */
    driverName: statusForm.driverName,
    /**
     * 字段 driverPhone：表示表单、筛选条件、接口数据或组件状态中的 driverPhone 值。
     */
    driverPhone: statusForm.driverPhone,
    /**
     * 字段 vehicleNo：表示表单、筛选条件、接口数据或组件状态中的 vehicleNo 值。
     */
    vehicleNo: statusForm.vehicleNo,
    /**
     * 字段 originDivisionCode：表示表单、筛选条件、接口数据或组件状态中的 originDivisionCode 值。
     */
    originDivisionCode: originDivision.codePath,
    /**
     * 字段 originDivisionName：表示表单、筛选条件、接口数据或组件状态中的 originDivisionName 值。
     */
    originDivisionName: originDivision.namePath,
    /**
     * 字段 destinationDivisionCode：表示表单、筛选条件、接口数据或组件状态中的 destinationDivisionCode 值。
     */
    destinationDivisionCode: destinationDivision.codePath,
    /**
     * 字段 destinationDivisionName：表示表单、筛选条件、接口数据或组件状态中的 destinationDivisionName 值。
     */
    destinationDivisionName: destinationDivision.namePath,
    /**
     * 字段 origin：表示表单、筛选条件、接口数据或组件状态中的 origin 值。
     */
    origin: statusForm.origin,
    /**
     * 字段 destination：表示表单、筛选条件、接口数据或组件状态中的 destination 值。
     */
    destination: statusForm.destination,
    /**
     * 字段 plannedShipDate：表示表单、筛选条件、接口数据或组件状态中的 plannedShipDate 值。
     */
    plannedShipDate: statusForm.plannedShipDate,
    /**
     * 字段 actualShipDate：表示表单、筛选条件、接口数据或组件状态中的 actualShipDate 值。
     */
    actualShipDate: statusForm.actualShipDate || undefined,
    /**
     * 字段 deliveredDate：表示表单、筛选条件、接口数据或组件状态中的 deliveredDate 值。
     */
    deliveredDate: statusForm.deliveredDate || undefined,
    /**
     * 字段 remark：表示表单、筛选条件、接口数据或组件状态中的 remark 值。
     */
    remark: statusForm.remark,
    /**
     * 字段 operationRemark：表示表单、筛选条件、接口数据或组件状态中的 operationRemark 值。
     */
    operationRemark: statusForm.operationRemark
  })
  ElMessage.success('物流状态已确认')
  statusDialogVisible.value = false
  await load()
}

/**
 * 执行 openOperationLogs 方法。
 * 
 * 实现步骤：
 * 1. 读取当前页面状态或调用参数；
 * 2. 完成对应的校验、接口调用或数据转换；
 * 3. 更新页面状态或返回处理结果。
 */
async function openOperationLogs(row: ShipmentView) {
  operationLogDrawerRef.value?.open({
    /**
     * 字段 title：表示表单、筛选条件、接口数据或组件状态中的 title 值。
     */
    title: `${row.shipmentNo} 物流流水`,
    /**
     * 字段 load：表示表单、筛选条件、接口数据或组件状态中的 load 值。
     */
    load: (params) => api.shipmentOperationLogs(row.id, params)
  })
}

/**
 * 批量删除物流单。
 *
 * 实现步骤：
 * 1. 校验是否已经勾选物流单；
 * 2. 弹出二次确认，避免误删运输履约数据；
 * 3. 调用后端批量删除接口；
 * 4. 删除成功后刷新列表。
 */
async function batchRemove() {
  if (selectedRows.value.length === 0) {
    ElMessage.warning('请选择需要删除的物流单')
    return
  }
  await ElMessageBox.confirm(`确认删除选中的 ${selectedRows.value.length} 张物流单？`, '批量删除确认', {
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
  await api.batchDeleteShipments(selectedRows.value.map((row) => row.id))
  ElMessage.success('批量删除成功')
  await load()
}

onMounted(async () => {
  applyRouteQuery()
  await Promise.all([load(), refreshDictionaryOptions()])
})

/**
 * 重新读取物流页面使用的基础字典。
 *
 * 实现步骤：
 * 1. 刷新行政区划首层、项目、承运商、组织、物流单据类型和运输方式；
 * 2. 行政区划只加载首层，省市区下级由级联组件展开时懒加载；
 * 3. 页面激活或打开弹窗时调用，保证基础信息修改后下拉框立即使用最新数据。
 */
async function refreshDictionaryOptions() {
  const [, carriers] = await Promise.all([
    loadDivisionOptions(),
    api.enabledDictionaryChildren('CARRIER'),
    loadProjectOptions(),
    loadBusinessDictionaryOptions()
  ])
  carrierOptions.value = carriers
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
   * 变量 changed：保存当前模块的页面状态、配置项、接口实例或计算结果。
   */
  let changed = false
  /**
   * 常量 shipmentNo：保存当前模块的页面状态、配置项、接口实例或计算结果。
   */
  const shipmentNo = queryString(route.query.shipmentNo)
  /**
   * 常量 relatedOrderNo：保存当前模块的页面状态、配置项、接口实例或计算结果。
   */
  const relatedOrderNo = queryString(route.query.relatedOrderNo)
  /**
   * 常量 trackingNo：保存当前模块的页面状态、配置项、接口实例或计算结果。
   */
  const trackingNo = queryString(route.query.trackingNo)
  if (shipmentNo && filters.shipmentNo !== shipmentNo) {
    filters.shipmentNo = shipmentNo
    changed = true
  }
  if (relatedOrderNo && filters.relatedOrderNo !== relatedOrderNo) {
    filters.relatedOrderNo = relatedOrderNo
    changed = true
  }
  if (trackingNo && filters.trackingNo !== trackingNo) {
    filters.trackingNo = trackingNo
    changed = true
  }
  return changed
}

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
  gap: 12px;
  margin-bottom: 12px;
  padding-bottom: 8px;
  border-bottom: 1px solid #e5e7eb;
  color: #111827;
  font-size: 14px;
  font-weight: 700;
}

.section-heading small,
.carrier-mode-hint {
  color: var(--muted-text-color);
  font-size: 12px;
  font-weight: 400;
}

.carrier-mode-hint {
  margin: -2px 0 12px;
}

</style>
