<template>
  <div class="page">
    <div class="page-header">
      <div>
        <h1 class="page-title">流程管理</h1>
        <p class="page-subtitle">按业务模块和功能模块绑定当前启用的流程模板。</p>
      </div>
    </div>

    <el-form class="filter-form" :model="filters" label-width="82px">
      <el-row :gutter="12">
        <el-col :xs="24" :sm="8">
          <el-form-item label="业务模块">
            <el-select v-model="filters.businessModuleCode" clearable class="full" placeholder="全部">
              <el-option v-for="item in businessModules" :key="item.code" :label="item.name" :value="item.code" />
            </el-select>
          </el-form-item>
        </el-col>
        <el-col :xs="24" :sm="8">
          <el-form-item label="功能模块">
            <el-select v-model="filters.functionModuleCode" clearable class="full" placeholder="全部">
              <el-option v-for="item in functionModules" :key="item.code" :label="item.name" :value="item.code" />
            </el-select>
          </el-form-item>
        </el-col>
        <el-col :xs="24" :sm="8">
          <el-form-item label="状态">
            <el-select v-model="filters.enabled" clearable class="full" placeholder="全部">
              <el-option label="启用" :value="true" />
              <el-option label="停用" :value="false" />
            </el-select>
          </el-form-item>
        </el-col>
        <el-col :xs="24" :sm="12">
          <el-form-item label=" " class="filter-actions">
            <el-button type="primary" @click="load">查询</el-button>
            <el-button @click="resetFilters">重置</el-button>
            <el-button v-if="auth.hasMenu('BTN_WORKFLOW_CONFIG_CREATE')" type="primary" :icon="Plus" @click="openCreate">新增配置</el-button>
          </el-form-item>
        </el-col>
      </el-row>
    </el-form>

    <div class="panel">
      <el-table v-loading="loading" :data="rows" stripe>
        <el-table-column prop="businessModuleName" label="业务模块" min-width="140" />
        <el-table-column prop="functionModuleName" label="功能模块" min-width="160" />
        <el-table-column prop="definitionName" label="流程模板" min-width="180" />
        <el-table-column label="状态" width="90">
          <template #default="{ row }"><el-tag :type="row.enabled ? 'success' : 'info'">{{ row.enabled ? '启用' : '停用' }}</el-tag></template>
        </el-table-column>
        <el-table-column label="操作" width="160" fixed="right">
          <template #default="{ row }">
            <div class="table-actions">
              <el-button v-if="auth.hasMenu('BTN_WORKFLOW_CONFIG_EDIT')" size="small" @click="openEdit(row)">编辑</el-button>
              <el-popconfirm title="确认删除该流程配置？" @confirm="remove(row.id)">
                <template #reference>
                  <el-button v-if="auth.hasMenu('BTN_WORKFLOW_CONFIG_DELETE')" size="small" type="danger">删除</el-button>
                </template>
              </el-popconfirm>
            </div>
          </template>
        </el-table-column>
      </el-table>
    </div>

    <el-dialog v-model="dialogVisible" :title="editingId ? '编辑流程配置' : '新增流程配置'" width="620px">
      <el-form ref="formRef" :model="form" :rules="rules" label-width="96px">
        <el-form-item label="业务模块" prop="businessModuleCode">
          <el-select v-model="form.businessModuleCode" class="full" @change="onBusinessModuleChange">
            <el-option v-for="item in businessModules" :key="item.code" :label="item.name" :value="item.code" />
          </el-select>
        </el-form-item>
        <el-form-item label="功能模块" prop="functionModuleCode">
          <el-select v-model="form.functionModuleCode" class="full" @change="onFunctionModuleChange">
            <el-option v-for="item in functionModules" :key="item.code" :label="item.name" :value="item.code" />
          </el-select>
        </el-form-item>
        <el-form-item label="流程模板" prop="definitionId">
          <el-select v-model="form.definitionId" filterable class="full" placeholder="请选择启用模板">
            <el-option v-for="item in definitions" :key="item.id" :label="`${item.name}（${item.code}）`" :value="item.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="是否启用">
          <el-switch v-model="form.enabled" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" @click="save">保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue'
import type { FormInstance, FormRules } from 'element-plus'
import { ElMessage } from 'element-plus'
import { Plus } from '@element-plus/icons-vue'
import { api } from '@/api/fm'
import { useAuthStore } from '@/stores/auth'
import type { WorkflowConfigView, WorkflowDefinitionView } from '@/types/api'

/**
 * 流程管理页面。
 *
 * <p>实现步骤：
 * 1. 查询所属公司下的功能模块流程配置；
 * 2. 新增或编辑时绑定业务模块、功能模块和启用流程模板；
 * 3. 业务发起审批时只按功能模块编码定位最新配置。</p>
 */
const auth = useAuthStore()
const loading = ref(false)
const rows = ref<WorkflowConfigView[]>([])
const definitions = ref<WorkflowDefinitionView[]>([])
const dialogVisible = ref(false)
const editingId = ref<number>()
const formRef = ref<FormInstance>()

const businessModules = [
  { code: 'FINANCE', name: '财务管理' },
  { code: 'OPERATION', name: '业务管理' },
  { code: 'INVENTORY', name: '库存管理' },
  { code: 'AR_AP', name: '应收应付' }
]

const functionModules = [
  { code: 'PURCHASE_APPROVAL', name: '采购审批', businessModuleCode: 'OPERATION' },
  { code: 'INVENTORY_INBOUND_APPROVAL', name: '入库审批', businessModuleCode: 'INVENTORY' },
  { code: 'INVENTORY_OUTBOUND_APPROVAL', name: '出库审批', businessModuleCode: 'INVENTORY' }
]

const filters = reactive({
  businessModuleCode: '',
  functionModuleCode: '',
  enabled: undefined as boolean | undefined
})

const form = reactive({
  businessModuleCode: 'OPERATION',
  businessModuleName: '业务管理',
  functionModuleCode: 'PURCHASE_APPROVAL',
  functionModuleName: '采购审批',
  definitionId: undefined as number | undefined,
  enabled: true
})

const rules: FormRules = {
  businessModuleCode: [{ required: true, message: '请选择业务模块', trigger: 'change' }],
  functionModuleCode: [{ required: true, message: '请选择功能模块', trigger: 'change' }],
  definitionId: [{ required: true, message: '请选择流程模板', trigger: 'change' }]
}

async function load() {
  loading.value = true
  try {
    rows.value = await api.workflowConfigs({
      businessModuleCode: filters.businessModuleCode || undefined,
      functionModuleCode: filters.functionModuleCode || undefined,
      enabled: filters.enabled
    })
  } finally {
    loading.value = false
  }
}

async function loadDefinitions() {
  definitions.value = await api.enabledWorkflowDefinitions()
}

function resetFilters() {
  Object.assign(filters, { businessModuleCode: '', functionModuleCode: '', enabled: undefined })
  void load()
}

function openCreate() {
  editingId.value = undefined
  Object.assign(form, {
    businessModuleCode: 'OPERATION',
    businessModuleName: '业务管理',
    functionModuleCode: 'PURCHASE_APPROVAL',
    functionModuleName: '采购审批',
    definitionId: definitions.value[0]?.id,
    enabled: true
  })
  dialogVisible.value = true
}

function openEdit(row: WorkflowConfigView) {
  editingId.value = row.id
  Object.assign(form, {
    businessModuleCode: row.businessModuleCode,
    businessModuleName: row.businessModuleName,
    functionModuleCode: row.functionModuleCode,
    functionModuleName: row.functionModuleName,
    definitionId: row.definitionId,
    enabled: row.enabled
  })
  dialogVisible.value = true
}

function onBusinessModuleChange() {
  const selected = businessModules.find((item) => item.code === form.businessModuleCode)
  form.businessModuleName = selected?.name || ''
}

function onFunctionModuleChange() {
  const selected = functionModules.find((item) => item.code === form.functionModuleCode)
  form.functionModuleName = selected?.name || ''
  if (selected) {
    form.businessModuleCode = selected.businessModuleCode
    onBusinessModuleChange()
  }
}

async function save() {
  const valid = await formRef.value?.validate().catch(() => false)
  if (!valid) {
    return
  }
  const payload = { ...form }
  if (editingId.value) {
    await api.updateWorkflowConfig(editingId.value, payload)
  } else {
    await api.createWorkflowConfig(payload)
  }
  ElMessage.success('保存成功')
  dialogVisible.value = false
  await load()
}

async function remove(id: number) {
  await api.deleteWorkflowConfig(id)
  ElMessage.success('删除成功')
  await load()
}

onMounted(async () => {
  await Promise.all([load(), loadDefinitions()])
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
</style>
