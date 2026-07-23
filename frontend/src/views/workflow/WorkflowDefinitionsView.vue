<template>
  <div class="page">
    <div class="page-header">
      <div>
        <h1 class="page-title">流程定义</h1>
        <p class="page-subtitle">设计流程模板和审批节点。</p>
      </div>
    </div>

    <el-form class="filter-form" :model="filters" label-width="82px">
      <el-row :gutter="12">
        <el-col :xs="24" :sm="8">
          <el-form-item label="名称">
            <el-input v-model="filters.name" clearable placeholder="模糊查询名称" />
          </el-form-item>
        </el-col>
        <el-col :xs="24" :sm="8">
          <el-form-item label="编码">
            <el-input v-model="filters.code" clearable placeholder="模糊查询编码" />
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
            <el-button v-if="auth.hasMenu('BTN_WORKFLOW_DEFINITION_CREATE')" type="primary" :icon="Plus" @click="openCreate">新增定义</el-button>
          </el-form-item>
        </el-col>
      </el-row>
    </el-form>

    <div class="panel">
      <el-table v-loading="loading" :data="rows" stripe>
        <el-table-column prop="name" label="流程名称" min-width="180" />
        <el-table-column prop="code" label="流程编码" min-width="180" />
        <el-table-column label="节点" min-width="260">
          <template #default="{ row }">
            <div class="node-tags">
              <el-tag v-for="node in row.nodes" :key="node.nodeOrder" size="small">
                {{ node.nodeOrder }}. {{ node.nodeName }}：{{ node.approverDisplay }}
              </el-tag>
            </div>
          </template>
        </el-table-column>
        <el-table-column prop="description" label="说明" min-width="220" />
        <el-table-column label="状态" width="90">
          <template #default="{ row }"><el-tag :type="row.enabled ? 'success' : 'info'">{{ row.enabled ? '启用' : '停用' }}</el-tag></template>
        </el-table-column>
        <el-table-column label="操作" width="160" fixed="right">
          <template #default="{ row }">
            <div class="table-actions">
              <el-button v-if="auth.hasMenu('BTN_WORKFLOW_DEFINITION_EDIT')" size="small" @click="openEdit(row)">编辑</el-button>
              <el-popconfirm title="确认删除该流程定义？" @confirm="remove(row.id)">
                <template #reference>
                  <el-button v-if="auth.hasMenu('BTN_WORKFLOW_DEFINITION_DELETE')" size="small" type="danger">删除</el-button>
                </template>
              </el-popconfirm>
            </div>
          </template>
        </el-table-column>
      </el-table>
    </div>

    <el-dialog v-model="dialogVisible" :title="editingId ? '编辑流程定义' : '新增流程定义'" width="min(1120px, 94vw)" top="5vh">
      <el-form ref="formRef" :model="form" :rules="rules" label-width="96px">
        <section class="business-form-section">
          <div class="section-heading"><span>基本信息</span></div>
          <el-row :gutter="12">
            <el-col :xs="24" :sm="8">
              <el-form-item label="流程名称" prop="name">
                <el-input v-model="form.name" maxlength="160" />
              </el-form-item>
            </el-col>
            <el-col :xs="24" :sm="8">
              <el-form-item label="流程编码" prop="code">
                <el-input v-model="form.code" maxlength="120" />
              </el-form-item>
            </el-col>
            <el-col :xs="24" :sm="8">
              <el-form-item label="是否启用">
                <el-switch v-model="form.enabled" />
              </el-form-item>
            </el-col>
            <el-col :xs="24">
              <el-form-item label="说明" prop="description">
                <el-input v-model="form.description" type="textarea" :rows="3" maxlength="500" show-word-limit />
              </el-form-item>
            </el-col>
          </el-row>
        </section>

        <section class="business-form-section">
          <div class="section-heading"><span>流程图预览</span></div>
          <div class="workflow-preview">
            <div class="workflow-preview__node workflow-preview__node--start">开始</div>
            <template v-for="(node, index) in form.nodes" :key="index">
              <div class="workflow-preview__line"></div>
              <div class="workflow-preview__node">
                <strong>{{ index + 1 }}. {{ node.nodeName || '审批节点' }}</strong>
                <span>{{ approverDisplay(node) }}</span>
              </div>
            </template>
            <div class="workflow-preview__line"></div>
            <div class="workflow-preview__node workflow-preview__node--end">结束</div>
          </div>
        </section>

        <section class="business-form-section">
          <div class="section-heading">
            <span>审批节点</span>
            <el-button size="small" :icon="Plus" @click="addNode">新增节点</el-button>
          </div>
          <el-table :data="form.nodes" border>
            <el-table-column label="节点名称" min-width="160">
              <template #default="{ row }"><el-input v-model="row.nodeName" maxlength="160" /></template>
            </el-table-column>
            <el-table-column label="审批人来源" width="160">
              <template #default="{ row }">
                <el-select v-model="row.approverType" class="full" @change="clearApprover(row)">
                  <el-option label="指定人员" value="USER" />
                  <el-option label="部门" value="DEPARTMENT" />
                  <el-option label="部门+岗位" value="DEPARTMENT_POSITION" />
                </el-select>
              </template>
            </el-table-column>
            <el-table-column label="审批人员" min-width="190">
              <template #default="{ row }">
                <el-select
                  v-if="row.approverType === 'USER'"
                  v-model="row.approverUserId"
                  filterable
                  class="full"
                  placeholder="请选择人员"
                  @change="onUserChange(row)"
                >
                  <el-option v-for="user in userOptions" :key="user.id" :label="`${user.realName}（${user.username}）`" :value="user.id" />
                </el-select>
                <span v-else class="muted">按组织范围审批</span>
              </template>
            </el-table-column>
            <el-table-column label="部门" min-width="180">
              <template #default="{ row }">
                <el-select
                  v-if="row.approverType !== 'USER'"
                  v-model="row.approverDepartment"
                  filterable
                  class="full"
                  placeholder="请选择部门"
                >
                  <el-option v-for="item in departmentOptions" :key="item.value" :label="item.label" :value="item.value" />
                </el-select>
                <span v-else class="muted">-</span>
              </template>
            </el-table-column>
            <el-table-column label="岗位" min-width="160">
              <template #default="{ row }">
                <el-select
                  v-if="row.approverType === 'DEPARTMENT_POSITION'"
                  v-model="row.approverPosition"
                  filterable
                  class="full"
                  placeholder="请选择岗位"
                >
                  <el-option v-for="item in positionOptions" :key="item.value" :label="item.label" :value="item.value" />
                </el-select>
                <span v-else class="muted">-</span>
              </template>
            </el-table-column>
            <el-table-column label="操作" width="90">
              <template #default="{ $index }">
                <el-button v-if="form.nodes.length > 1" size="small" type="danger" @click="form.nodes.splice($index, 1)">删除</el-button>
              </template>
            </el-table-column>
          </el-table>
        </section>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" @click="save">保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { onActivated, onMounted, reactive, ref } from 'vue'
import type { FormInstance, FormRules } from 'element-plus'
import { ElMessage } from 'element-plus'
import { Plus } from '@element-plus/icons-vue'
import { api } from '@/api/fm'
import { useAuthStore } from '@/stores/auth'
import type { UserView, WorkflowApproverType, WorkflowDefinitionView, WorkflowNodeView } from '@/types/api'
import { flattenDictionaryOptions, type DictionaryOption } from '@/utils/dictionaries'

interface WorkflowNodeForm {
  nodeName: string
  approverType: WorkflowApproverType
  approverUserId?: number
  approverUsername?: string
  approverName?: string
  approverDepartment?: string
  approverPosition?: string
}

const auth = useAuthStore()
const loading = ref(false)
const rows = ref<WorkflowDefinitionView[]>([])
const userOptions = ref<UserView[]>([])
const departmentOptions = ref<DictionaryOption[]>([])
const positionOptions = ref<DictionaryOption[]>([])
const dialogVisible = ref(false)
const editingId = ref<number>()
const formRef = ref<FormInstance>()

const filters = reactive({
  name: '',
  code: '',
  enabled: undefined as boolean | undefined
})

const form = reactive({
  name: '',
  code: '',
  description: '',
  enabled: true,
  nodes: [] as WorkflowNodeForm[]
})

const rules: FormRules = {
  name: [{ required: true, message: '请输入流程名称', trigger: 'blur' }],
  code: [{ required: true, message: '请输入流程编码', trigger: 'blur' }]
}

/**
 * 加载流程定义列表。
 */
async function load() {
  loading.value = true
  try {
    rows.value = await api.workflowDefinitions({
      name: filters.name.trim() || undefined,
      code: filters.code.trim() || undefined,
      enabled: filters.enabled
    })
  } finally {
    loading.value = false
  }
}

async function loadOptions() {
  const [users, departments, positions] = await Promise.all([
    api.users({ enabled: true }),
    api.enabledDictionaryTree('DEPARTMENT'),
    api.enabledDictionaryTree('POSITION')
  ])
  userOptions.value = users
  departmentOptions.value = flattenDictionaryOptions(departments)
  positionOptions.value = flattenDictionaryOptions(positions)
}

function resetFilters() {
  Object.assign(filters, { name: '', code: '', enabled: undefined })
  void load()
}

async function openCreate() {
  await loadOptions()
  editingId.value = undefined
  Object.assign(form, {
    name: '',
    code: '',
    description: '',
    enabled: true,
    nodes: [newNode()]
  })
  dialogVisible.value = true
}

async function openEdit(row: WorkflowDefinitionView) {
  await loadOptions()
  editingId.value = row.id
  Object.assign(form, {
    name: row.name,
    code: row.code,
    description: row.description || '',
    enabled: row.enabled,
    nodes: row.nodes.map(toNodeForm)
  })
  dialogVisible.value = true
}

function newNode(): WorkflowNodeForm {
  return {
    nodeName: '审批节点',
    approverType: 'DEPARTMENT_POSITION',
    approverDepartment: '管理部',
    approverPosition: 'Administrator'
  }
}

function toNodeForm(node: WorkflowNodeView): WorkflowNodeForm {
  return {
    nodeName: node.nodeName,
    approverType: node.approverType,
    approverUserId: node.approverUserId,
    approverUsername: node.approverUsername,
    approverName: node.approverName,
    approverDepartment: node.approverDepartment,
    approverPosition: node.approverPosition
  }
}

function addNode() {
  form.nodes.push(newNode())
}

function clearApprover(row: WorkflowNodeForm) {
  row.approverUserId = undefined
  row.approverUsername = undefined
  row.approverName = undefined
  row.approverDepartment = row.approverType === 'USER' ? undefined : row.approverDepartment
  row.approverPosition = row.approverType === 'DEPARTMENT_POSITION' ? row.approverPosition : undefined
}

function onUserChange(row: WorkflowNodeForm) {
  const user = userOptions.value.find((item) => item.id === row.approverUserId)
  row.approverUsername = user?.username
  row.approverName = user?.realName
}

/**
 * 生成流程图节点上的审批人显示文本。
 *
 * 实现步骤：
 * 1. 指定人员节点优先展示人员姓名或账号；
 * 2. 部门节点展示部门名称；
 * 3. 部门岗位节点同时展示部门和岗位，便于维护人员检查流程配置。
 */
function approverDisplay(node: WorkflowNodeForm) {
  if (node.approverType === 'USER') {
    return node.approverName || node.approverUsername || '未选择人员'
  }
  if (node.approverType === 'DEPARTMENT') {
    return node.approverDepartment || '未选择部门'
  }
  return `${node.approverDepartment || '未选择部门'} / ${node.approverPosition || '未选择岗位'}`
}

async function save() {
  const valid = await formRef.value?.validate().catch(() => false)
  if (!valid) {
    return
  }
  const error = validateNodes()
  if (error) {
    ElMessage.warning(error)
    return
  }
  const payload = {
    name: form.name,
    code: form.code,
    description: form.description,
    enabled: form.enabled,
    nodes: form.nodes
  }
  if (editingId.value) {
    await api.updateWorkflowDefinition(editingId.value, payload)
  } else {
    await api.createWorkflowDefinition(payload)
  }
  ElMessage.success('保存成功')
  dialogVisible.value = false
  await load()
}

function validateNodes() {
  if (form.nodes.length === 0) {
    return '至少配置一个审批节点'
  }
  for (const node of form.nodes) {
    if (!node.nodeName.trim()) {
      return '请填写节点名称'
    }
    if (node.approverType === 'USER' && !node.approverUserId) {
      return '指定人员审批节点必须选择审批人'
    }
    if (node.approverType === 'DEPARTMENT' && !node.approverDepartment) {
      return '部门审批节点必须选择部门'
    }
    if (node.approverType === 'DEPARTMENT_POSITION' && (!node.approverDepartment || !node.approverPosition)) {
      return '部门岗位审批节点必须选择部门和岗位'
    }
  }
  return ''
}

async function remove(id: number) {
  await api.deleteWorkflowDefinition(id)
  ElMessage.success('删除成功')
  await load()
}

onMounted(async () => {
  await Promise.all([load(), loadOptions()])
})

onActivated(() => {
  void loadOptions()
})
</script>

<style scoped>
.full {
  width: 100%;
}

.filter-form,
.business-form-section {
  margin-bottom: 14px;
  padding: 14px 14px 0;
  border: 1px solid #e5e7eb;
  border-radius: 6px;
  background: #fff;
}

.business-form-section {
  padding-bottom: 14px;
}

.section-heading {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 12px;
  padding-bottom: 8px;
  border-bottom: 1px solid #e5e7eb;
  font-weight: 700;
}

.node-tags {
  display: flex;
  flex-wrap: wrap;
  gap: 6px;
}

.workflow-preview {
  display: flex;
  align-items: center;
  gap: 10px;
  overflow-x: auto;
  padding: 6px 0 2px;
}

.workflow-preview__node {
  display: inline-flex;
  min-width: 150px;
  max-width: 220px;
  min-height: 64px;
  flex-direction: column;
  justify-content: center;
  gap: 6px;
  padding: 10px 12px;
  border: 1px solid #bfdbfe;
  border-radius: 6px;
  background: #eff6ff;
  color: #1e3a8a;
  white-space: normal;
}

.workflow-preview__node span {
  color: #64748b;
  font-size: 12px;
}

.workflow-preview__node--start,
.workflow-preview__node--end {
  min-width: 88px;
  align-items: center;
  border-color: #86efac;
  background: #f0fdf4;
  color: #166534;
  font-weight: 700;
}

.workflow-preview__line {
  width: 44px;
  height: 2px;
  flex: 0 0 44px;
  background: #93c5fd;
}

.muted {
  color: #94a3b8;
}
</style>
