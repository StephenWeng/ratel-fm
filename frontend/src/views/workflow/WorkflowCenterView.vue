<template>
  <div class="page">
    <div class="page-header">
      <div>
        <h1 class="page-title">审批中心</h1>
        <p class="page-subtitle">按当前用户查看待办、已办和发起事宜。</p>
      </div>
    </div>

    <div class="panel">
      <el-form class="filter-form workflow-filter" :model="filters" label-width="96px">
        <el-row :gutter="12">
          <el-col :xs="24" :sm="12" :md="6">
            <el-form-item label="业务模块">
              <el-select v-model="filters.businessModuleCode" clearable class="full" placeholder="全部">
                <el-option v-for="item in businessModules" :key="item.code" :label="item.name" :value="item.code" />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :xs="24" :sm="12" :md="6">
            <el-form-item label="审批标题">
              <el-input v-model.trim="filters.title" clearable placeholder="模糊查询审批标题" />
            </el-form-item>
          </el-col>
          <el-col :xs="24" :sm="12" :md="6">
            <el-form-item label="项目">
              <el-select v-model="filters.projectCode" clearable filterable class="full" placeholder="全部项目">
                <el-option v-for="item in projectOptions" :key="item.id" :label="item.name" :value="item.code" />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :xs="24" :sm="12" :md="6">
            <el-form-item label="申请时间">
              <el-date-picker
                v-model="filters.startedRange"
                type="daterange"
                unlink-panels
                start-placeholder="开始日期"
                end-placeholder="结束日期"
                value-format="YYYY-MM-DD"
                class="full"
              />
            </el-form-item>
          </el-col>
          <el-col :xs="24" :sm="12" :md="6">
            <el-form-item label="发起人姓名">
              <el-input v-model.trim="filters.starterName" clearable placeholder="模糊查询发起人" />
            </el-form-item>
          </el-col>
          <el-col :xs="24" :sm="12" :md="6">
            <el-form-item label="流程状态">
              <el-select v-model="filters.status" clearable class="full" placeholder="全部">
                <el-option label="审批中" value="RUNNING" />
                <el-option label="已同意" value="APPROVED" />
                <el-option label="已不同意" value="REJECTED" />
                <el-option label="已取消" value="CANCELLED" />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :xs="24" :sm="12" :md="10">
            <el-form-item label=" " class="filter-actions">
              <el-button type="primary" @click="loadActive">查询</el-button>
              <el-button @click="resetFilters">重置</el-button>
            </el-form-item>
          </el-col>
        </el-row>
      </el-form>
      <el-tabs v-model="activeTab" @tab-change="loadActive">
        <el-tab-pane label="待办事宜" name="todo">
          <el-table v-loading="loading" :data="todoItems" stripe>
            <el-table-column label="业务模块" min-width="150">
              <template #default="{ row }">{{ row.businessModuleName }} / {{ row.functionModuleName }}</template>
            </el-table-column>
            <el-table-column prop="title" label="审批标题" min-width="220" />
            <el-table-column label="项目" min-width="140">
              <template #default="{ row }">{{ projectText(row) }}</template>
            </el-table-column>
            <el-table-column label="发起人" min-width="140">
              <template #default="{ row }">{{ row.starterName }}（{{ row.starterUsername }}）</template>
            </el-table-column>
            <el-table-column prop="startedTime" label="申请时间" min-width="170">
              <template #default="{ row }">{{ formatTime(row.startedTime) }}</template>
            </el-table-column>
            <el-table-column prop="currentNodeName" label="当前节点" min-width="120" />
            <el-table-column label="流程状态" width="110">
              <template #default="{ row }"><el-tag :type="workflowStatusType(row.status)">{{ workflowStatusLabel(row.status) }}</el-tag></template>
            </el-table-column>
            <el-table-column label="下个节点审批人" min-width="180">
              <template #default="{ row }">
                <WorkflowApproverPopover :approver-info="row.nextApproverInfo" :users="row.nextApproverUsers || []" />
              </template>
            </el-table-column>
            <el-table-column label="操作" width="170" fixed="right">
              <template #default="{ row }">
                <div class="table-actions">
                  <el-button v-if="auth.hasMenu('BTN_WORKFLOW_APPROVE')" size="small" type="primary" @click="openApprove(row)">审批</el-button>
                  <el-button v-if="auth.hasMenu('BTN_WORKFLOW_VIEW')" size="small" @click="openDetail(row)">流程查看</el-button>
                </div>
              </template>
            </el-table-column>
          </el-table>
        </el-tab-pane>
        <el-tab-pane label="已办事宜" name="done">
          <el-table v-loading="loading" :data="doneItems" stripe>
            <el-table-column label="业务模块" min-width="150">
              <template #default="{ row }">{{ row.businessModuleName }} / {{ row.functionModuleName }}</template>
            </el-table-column>
            <el-table-column prop="title" label="审批标题" min-width="220" />
            <el-table-column label="项目" min-width="140">
              <template #default="{ row }">{{ projectText(row) }}</template>
            </el-table-column>
            <el-table-column label="发起人" min-width="140">
              <template #default="{ row }">{{ row.starterName }}（{{ row.starterUsername }}）</template>
            </el-table-column>
            <el-table-column prop="startedTime" label="申请时间" min-width="170">
              <template #default="{ row }">{{ formatTime(row.startedTime) }}</template>
            </el-table-column>
            <el-table-column label="审批结果" width="110">
              <template #default="{ row }">
                <el-tag :type="taskStatusType(row.taskStatus)">{{ taskStatusLabel(row.taskStatus) }}</el-tag>
              </template>
            </el-table-column>
            <el-table-column prop="actedTime" label="审批时间" min-width="170">
              <template #default="{ row }">{{ formatTime(row.actedTime) }}</template>
            </el-table-column>
            <el-table-column label="流程状态" width="110">
              <template #default="{ row }"><el-tag :type="workflowStatusType(row.status)">{{ workflowStatusLabel(row.status) }}</el-tag></template>
            </el-table-column>
            <el-table-column label="下个节点审批人" min-width="180">
              <template #default="{ row }">
                <WorkflowApproverPopover :approver-info="row.nextApproverInfo" :users="row.nextApproverUsers || []" />
              </template>
            </el-table-column>
            <el-table-column label="操作" width="110" fixed="right">
              <template #default="{ row }">
                <el-button v-if="auth.hasMenu('BTN_WORKFLOW_VIEW')" size="small" @click="openDetail(row)">流程查看</el-button>
              </template>
            </el-table-column>
          </el-table>
        </el-tab-pane>
        <el-tab-pane label="发起事宜" name="started">
          <el-table v-loading="loading" :data="startedItems" stripe>
            <el-table-column label="业务模块" min-width="150">
              <template #default="{ row }">{{ row.businessModuleName }} / {{ row.functionModuleName }}</template>
            </el-table-column>
            <el-table-column prop="title" label="审批标题" min-width="220" />
            <el-table-column label="项目" min-width="140">
              <template #default="{ row }">{{ projectText(row) }}</template>
            </el-table-column>
            <el-table-column prop="businessNo" label="业务单号" min-width="150" />
            <el-table-column prop="startedTime" label="申请时间" min-width="170">
              <template #default="{ row }">{{ formatTime(row.startedTime) }}</template>
            </el-table-column>
            <el-table-column prop="currentNodeName" label="当前节点" min-width="120" />
            <el-table-column label="流程状态" width="110">
              <template #default="{ row }"><el-tag :type="workflowStatusType(row.status)">{{ workflowStatusLabel(row.status) }}</el-tag></template>
            </el-table-column>
            <el-table-column label="下个节点审批人" min-width="180">
              <template #default="{ row }">
                <WorkflowApproverPopover :approver-info="row.nextApproverInfo" :users="row.nextApproverUsers || []" />
              </template>
            </el-table-column>
            <el-table-column label="操作" width="110" fixed="right">
              <template #default="{ row }">
                <el-button v-if="auth.hasMenu('BTN_WORKFLOW_VIEW')" size="small" @click="openDetail(row)">流程查看</el-button>
              </template>
            </el-table-column>
          </el-table>
        </el-tab-pane>
      </el-tabs>
    </div>

    <el-dialog v-model="approveVisible" title="流程审批" width="min(980px, 94vw)" top="5vh">
      <div class="approve-dialog-body">
        <WorkflowBusinessFormPreview v-loading="approveDetailLoading" :form="approveDetail?.businessForm" />
        <el-form ref="approveFormRef" :model="approveForm" :rules="approveRules" label-width="86px">
        <el-form-item label="审批结果">
          <el-radio-group v-model="approveForm.approved" @change="onApproveResultChange">
            <el-radio-button :label="true">同意</el-radio-button>
            <el-radio-button :label="false">不同意</el-radio-button>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="审批意见" prop="comment">
          <el-input v-model="approveForm.comment" type="textarea" :rows="5" :maxlength="fieldLimits.remark" show-word-limit />
        </el-form-item>
        </el-form>
      </div>
      <template #footer>
        <el-button @click="approveVisible = false">取消</el-button>
        <el-button type="primary" @click="submitApprove">确认</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="detailVisible" title="流程查看" width="min(1040px, 94vw)" top="5vh">
      <div v-if="detail" class="workflow-detail">
        <div class="detail-title">
          <div>
            <strong>{{ detail.instance.title }}</strong>
            <span class="detail-title__meta">{{ detail.instance.businessModuleName }} / {{ detail.instance.functionModuleName }}</span>
          </div>
          <span class="workflow-status-pill" :class="workflowStatusClass(detail.instance.status)">{{ workflowStatusLabel(detail.instance.status) }}</span>
        </div>
        <WorkflowBusinessFormPreview :form="detail.businessForm" />
        <div class="workflow-nodes">
          <div v-for="task in detail.tasks" :key="task.nodeOrder" class="workflow-node" :class="nodeClass(task)">
            <div class="node-index">{{ task.nodeOrder }}</div>
            <div class="node-main">
              <strong>{{ task.nodeName }}</strong>
              <span>{{ task.approverDisplay || '-' }}</span>
              <small>{{ taskStatusLabel(task.status) }}</small>
            </div>
          </div>
        </div>
        <el-timeline class="workflow-logs">
          <el-timeline-item
            v-for="log in detail.operationLogs"
            :key="log.id"
            :timestamp="formatTime(log.operationTime)"
            :type="log.operationType === 'REJECT' ? 'danger' : 'success'"
          >
            <div class="log-title">{{ operationTypeLabel(log.operationType) }} {{ log.nodeName ? ` - ${log.nodeName}` : '' }}</div>
            <div class="log-sub">{{ operationActorPrefix(log.operationType) }}{{ operatorLine(log) }}</div>
            <div class="log-comment">【审批意见】{{ log.comment || '-' }}</div>
          </el-timeline-item>
        </el-timeline>
      </div>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue'
import type { FormInstance, FormRules } from 'element-plus'
import { ElMessage } from 'element-plus'
import { api } from '@/api/fm'
import WorkflowApproverPopover from '@/components/workflow/WorkflowApproverPopover.vue'
import WorkflowBusinessFormPreview from '@/components/workflow/WorkflowBusinessFormPreview.vue'
import { useAuthStore } from '@/stores/auth'
import type { BasicDictionaryView, WorkflowInstanceDetailView, WorkflowItemView, WorkflowStatus, WorkflowTaskStatus, WorkflowTaskView, WorkflowOperationType, WorkflowOperationLogView } from '@/types/api'
import type { WorkflowCenterQueryParams } from '@/api/fm'
import { fieldLimits } from '@/utils/validators'

/**
 * 审批中心页面。
 *
 * <p>实现步骤：
 * 1. 按当前用户分别加载待办、已办、发起事宜；
 * 2. 待办流程可填写审批意见并选择同意或不同意；
 * 3. 流程查看弹窗展示节点进度和操作流水。</p>
 */
const auth = useAuthStore()
const loading = ref(false)
const activeTab = ref<'todo' | 'done' | 'started'>('todo')
const todoItems = ref<WorkflowItemView[]>([])
const doneItems = ref<WorkflowItemView[]>([])
const startedItems = ref<WorkflowItemView[]>([])
const projectOptions = ref<BasicDictionaryView[]>([])
const approveVisible = ref(false)
const approveTarget = ref<WorkflowItemView>()
const approveFormRef = ref<FormInstance>()
const approveDetail = ref<WorkflowInstanceDetailView>()
const approveDetailLoading = ref(false)
const detailVisible = ref(false)
const detail = ref<WorkflowInstanceDetailView>()

const businessModules = [
  { code: 'FINANCE', name: '财务管理' },
  { code: 'OPERATION', name: '业务管理' },
  { code: 'INVENTORY', name: '库存管理' },
  { code: 'AR_AP', name: '应收应付' }
]

const filters = reactive({
  businessModuleCode: '',
  title: '',
  projectCode: '',
  startedRange: [] as string[],
  starterName: '',
  status: undefined as WorkflowStatus | undefined
})

const approveForm = reactive({
  approved: true,
  comment: '同意申请。'
})

const approveRules: FormRules = {
  comment: [
    { required: true, message: '请填写审批意见', trigger: 'blur' },
    { max: fieldLimits.remark, message: `审批意见不能超过${fieldLimits.remark}个字符`, trigger: 'blur' }
  ]
}

/**
 * 加载当前页签列表。
 *
 * <p>实现步骤：根据页签调用对应接口，接口已按当前登录人和所属公司过滤。</p>
 */
async function loadActive() {
  loading.value = true
  try {
    const params = workflowCenterParams()
    if (activeTab.value === 'todo') {
      todoItems.value = await api.workflowTodo(params)
    } else if (activeTab.value === 'done') {
      doneItems.value = await api.workflowDone(params)
    } else {
      startedItems.value = await api.workflowStarted(params)
    }
  } finally {
    loading.value = false
  }
}

/**
 * 组装审批中心搜索参数。
 *
 * 实现步骤：
 * 1. 空字符串转为 undefined，避免后端把空条件参与匹配；
 * 2. 日期范围拆成 startedStart 和 startedEnd；
 * 3. 三个列表共用同一套查询参数。
 */
function workflowCenterParams(): WorkflowCenterQueryParams {
  return {
    businessModuleCode: filters.businessModuleCode || undefined,
    title: filters.title || undefined,
    projectCode: filters.projectCode || undefined,
    startedStart: filters.startedRange?.[0] || undefined,
    startedEnd: filters.startedRange?.[1] || undefined,
    starterName: filters.starterName || undefined,
    status: filters.status
  }
}

/**
 * 重置审批中心搜索条件。
 *
 * 实现步骤：清空所有筛选项后重新加载当前页签数据。
 */
function resetFilters() {
  Object.assign(filters, {
    businessModuleCode: '',
    title: '',
    projectCode: '',
    startedRange: [],
    starterName: '',
    status: undefined
  })
  void loadActive()
}

/**
 * 打开审批弹窗。
 *
 * <p>实现步骤：保存当前待办任务，默认同意且自动填入“同意申请。”。</p>
 */
async function openApprove(row: WorkflowItemView) {
  approveTarget.value = row
  approveDetail.value = undefined
  approveForm.approved = true
  approveForm.comment = '同意申请。'
  approveVisible.value = true
  approveDetailLoading.value = true
  try {
    approveDetail.value = await api.workflowDetail(row.instanceId)
  } finally {
    approveDetailLoading.value = false
  }
}

/**
 * 根据审批结果同步默认审批意见。
 */
function onApproveResultChange() {
  if (approveForm.approved && !approveForm.comment.trim()) {
    approveForm.comment = '同意申请。'
  }
}

/**
 * 提交审批。
 *
 * <p>实现步骤：
 * 1. 校验审批意见必填；
 * 2. 调用任务审批接口；
 * 3. 刷新待办、已办和发起列表，保证状态及时变化。</p>
 */
async function submitApprove() {
  const valid = await approveFormRef.value?.validate().catch(() => false)
  if (!valid || !approveTarget.value?.taskId) {
    return
  }
  await api.approveWorkflowTask(approveTarget.value.taskId, {
    approved: approveForm.approved,
    comment: approveForm.comment
  })
  ElMessage.success('审批完成')
  approveVisible.value = false
  await Promise.all([loadTodo(), loadDone(), loadStarted()])
}

/**
 * 打开流程查看弹窗。
 */
async function openDetail(row: WorkflowItemView) {
  detail.value = await api.workflowDetail(row.instanceId)
  detailVisible.value = true
}

async function loadTodo() {
  todoItems.value = await api.workflowTodo(workflowCenterParams())
}

async function loadDone() {
  doneItems.value = await api.workflowDone(workflowCenterParams())
}

async function loadStarted() {
  startedItems.value = await api.workflowStarted(workflowCenterParams())
}

function workflowStatusLabel(status?: WorkflowStatus) {
  const labels: Record<WorkflowStatus, string> = {
    RUNNING: '审批中',
    APPROVED: '已同意',
    REJECTED: '已不同意',
    CANCELLED: '已取消'
  }
  return status ? labels[status] : '-'
}

function workflowStatusType(status?: WorkflowStatus) {
  return status === 'APPROVED' ? 'success' : status === 'REJECTED' ? 'danger' : status === 'CANCELLED' ? 'info' : 'warning'
}

function workflowStatusClass(status?: WorkflowStatus) {
  return {
    'workflow-status-pill--running': status === 'RUNNING',
    'workflow-status-pill--approved': status === 'APPROVED',
    'workflow-status-pill--rejected': status === 'REJECTED',
    'workflow-status-pill--cancelled': status === 'CANCELLED'
  }
}

function taskStatusLabel(status?: WorkflowTaskStatus) {
  const labels: Record<WorkflowTaskStatus, string> = {
    PENDING: '待审批',
    APPROVED: '同意',
    REJECTED: '不同意',
    SKIPPED: '未到达'
  }
  return status ? labels[status] : '-'
}

function taskStatusType(status?: WorkflowTaskStatus) {
  return status === 'APPROVED' ? 'success' : status === 'REJECTED' ? 'danger' : status === 'PENDING' ? 'warning' : 'info'
}

function operationTypeLabel(type: WorkflowOperationType) {
  return {
    START: '发起流程',
    APPROVE: '审批同意',
    REJECT: '审批不同意',
    CANCEL: '业务取消'
  }[type]
}

/**
 * 返回流程流水操作人前缀。
 *
 * 实现步骤：
 * 1. 发起流程展示“发起人”，避免把发起动作误读为审批动作；
 * 2. 审批、驳回和取消动作展示“审批人”，满足流程操作流水阅读习惯。
 */
function operationActorPrefix(type: WorkflowOperationType) {
  return type === 'START' ? '【发起人】' : '【审批人】'
}

/**
 * 组装流程流水操作人展示文本。
 *
 * 实现步骤：
 * 1. 优先展示人员姓名，姓名为空时回退账号；
 * 2. 账号存在时用括号补充，便于同名人员区分；
 * 3. 联系方式存在时追加展示，方便审批追踪。
 */
function operatorLine(log: WorkflowOperationLogView) {
  const name = log.operatorName || log.operatorUsername || '-'
  const username = log.operatorUsername ? `（${log.operatorUsername}）` : ''
  const phone = log.operatorPhone ? `，联系方式：${log.operatorPhone}` : ''
  return `${name}${username}${phone}`
}

function nodeClass(task: WorkflowTaskView) {
  return {
    'workflow-node--done': task.status === 'APPROVED',
    'workflow-node--reject': task.status === 'REJECTED',
    'workflow-node--pending': task.status === 'PENDING'
  }
}

function formatTime(value?: string) {
  if (!value) {
    return '-'
  }
  return value.replace('T', ' ').replace(/\.\d+.*$/, '')
}

function projectText(row: WorkflowItemView) {
  return row.projectName || row.projectCode || '-'
}

onMounted(async () => {
  projectOptions.value = await api.enabledDictionaryChildren('PROJECT').catch(() => [])
  await loadActive()
})
</script>

<style scoped>
.workflow-detail {
  display: grid;
  gap: 18px;
}

.approve-dialog-body {
  display: grid;
  gap: 16px;
}

.workflow-filter {
  padding: 16px 16px 2px;
  border: 0 !important;
  border-bottom: 1px solid var(--border-color) !important;
  border-radius: 0 !important;
}

.detail-title {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
}

.detail-title div {
  display: grid;
  gap: 4px;
}

.detail-title__meta {
  color: var(--muted-text-color);
  font-size: 13px;
}

.workflow-status-pill {
  display: inline-flex;
  align-items: center;
  height: 26px;
  padding: 0 10px;
  border: 1px solid var(--warning-border-color);
  border-radius: var(--radius-sm);
  background: var(--warning-surface-color);
  color: var(--warning-color);
  font-weight: 700;
  white-space: nowrap;
}

.workflow-status-pill--approved {
  border-color: var(--success-border-color);
  background: var(--success-surface-color);
  color: var(--success-color);
}

.workflow-status-pill--rejected {
  border-color: var(--danger-border-color);
  background: var(--danger-surface-color);
  color: var(--danger-color);
}

.workflow-status-pill--cancelled {
  border-color: var(--border-color);
  background: var(--subtle-surface-color);
  color: var(--muted-text-color);
}

.workflow-nodes {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(160px, 1fr));
  gap: 12px;
}

.workflow-node {
  display: flex;
  gap: 10px;
  padding: 12px;
  border: 1px solid var(--border-color);
  border-radius: var(--radius-md);
  background: var(--surface-color);
  color: var(--text-color);
}

.workflow-node--done {
  border-color: var(--success-border-color);
  background: var(--success-surface-color);
}

.workflow-node--reject {
  border-color: var(--danger-border-color);
  background: var(--danger-surface-color);
}

.workflow-node--pending {
  border-color: var(--warning-border-color);
  background: var(--warning-surface-color);
}

.node-index {
  display: grid;
  place-items: center;
  width: 28px;
  height: 28px;
  border-radius: 50%;
  background: var(--heading-color);
  color: var(--surface-color);
  font-weight: 700;
}

.node-main {
  display: grid;
  gap: 3px;
}

.node-main strong {
  color: var(--heading-color);
}

.node-main span,
.node-main small {
  color: var(--secondary-text-color);
}

.workflow-logs {
  max-height: 360px;
  overflow: auto;
  padding-left: 142px;
  padding-right: 8px;
}

.workflow-logs :deep(.el-timeline-item__content) {
  position: relative;
}

.workflow-logs :deep(.el-timeline-item__timestamp) {
  position: absolute;
  left: -140px;
  top: 0;
  width: 112px;
  margin: 0;
  color: #94a3b8;
  line-height: 20px;
  text-align: right;
  white-space: nowrap;
}

.log-title {
  font-weight: 700;
}

.log-sub {
  margin-top: 2px;
  color: #64748b;
}

.log-comment {
  margin-top: 6px;
  white-space: pre-wrap;
}
</style>
