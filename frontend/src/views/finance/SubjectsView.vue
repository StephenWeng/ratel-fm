<template>
  <div class="page">
    <div class="page-header">
      <div>
        <h1 class="page-title">会计科目</h1>
        <p class="page-subtitle">维护企业核算科目字典，供凭证记账使用。</p>
      </div>
    </div>

    <el-form class="filter-form" :model="filters" label-width="76px">
      <el-row :gutter="12">
        <el-col :xs="24" :sm="12" :md="8" :lg="5">
          <el-form-item label="编码">
            <el-input v-model="filters.code" clearable placeholder="模糊查询编码" />
          </el-form-item>
        </el-col>
        <el-col :xs="24" :sm="12" :md="8" :lg="5">
          <el-form-item label="名称">
            <el-input v-model="filters.name" clearable placeholder="模糊查询名称" />
          </el-form-item>
        </el-col>
        <el-col :xs="24" :sm="12" :md="8" :lg="5">
          <el-form-item label="类别">
            <el-select v-model="filters.category" clearable class="full" placeholder="全部">
              <el-option v-for="item in categories" :key="item.value" :label="item.label" :value="item.value" />
            </el-select>
          </el-form-item>
        </el-col>
        <el-col :xs="24" :sm="12" :md="8" :lg="5">
          <el-form-item label="父级">
            <el-select v-model="filters.parentId" clearable filterable class="full" placeholder="精确匹配父级">
              <el-option v-for="item in subjects" :key="item.id" :label="`${item.code} ${item.name}`" :value="item.id" />
            </el-select>
          </el-form-item>
        </el-col>
        <el-col :xs="24" :sm="12" :md="8" :lg="4">
          <el-form-item label="状态">
            <el-select v-model="filters.enabled" clearable class="full" placeholder="全部">
              <el-option label="启用" :value="true" />
              <el-option label="停用" :value="false" />
            </el-select>
          </el-form-item>
        </el-col>
        <el-col :xs="24" :sm="12" :md="8" :lg="5">
          <el-form-item label="说明">
            <el-input v-model="filters.description" clearable placeholder="模糊查询说明" />
          </el-form-item>
        </el-col>
        <el-col :xs="24" :sm="12" :md="8" :lg="6">
          <el-form-item label=" " class="filter-actions">
            <el-switch v-model="onlyEnabled" active-text="仅启用" @change="load" />
            <el-button type="primary" @click="load">查询</el-button>
            <el-button @click="resetFilters">重置</el-button>
            <el-button v-if="auth.hasMenu('BTN_SUBJECT_CREATE')" type="primary" :icon="Plus" @click="openCreate">新增科目</el-button>
            <el-button v-if="auth.hasMenu('BTN_SUBJECT_BATCH_DELETE') && selectedRows.length > 0" type="danger" :icon="Delete" @click="batchRemove">批量删除</el-button>
          </el-form-item>
        </el-col>
      </el-row>
    </el-form>

    <div class="panel">
      <el-table
        v-loading="loading"
        :data="subjectTree"
        row-key="id"
        stripe
        :expand-row-keys="expandedSubjectKeys"
        :tree-props="{ children: 'children' }"
        @selection-change="handleSelectionChange"
      >
        <el-table-column type="selection" width="48" />
        <el-table-column prop="code" label="编码" min-width="110" />
        <el-table-column prop="name" label="名称" min-width="150" />
        <el-table-column label="类别" min-width="110">
          <template #default="{ row }">{{ categoryLabel(row.category) }}</template>
        </el-table-column>
        <el-table-column prop="parentName" label="父级" min-width="140" />
        <el-table-column prop="subjectLevel" label="层级" width="80" />
        <el-table-column label="状态" width="90">
          <template #default="{ row }">
            <el-tag :type="row.enabled ? 'success' : 'info'">{{ row.enabled ? '启用' : '停用' }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="description" label="说明" min-width="220" />
        <el-table-column label="操作" width="170" fixed="right">
          <template #default="{ row }">
            <div class="table-actions">
              <el-button v-if="auth.hasMenu('BTN_SUBJECT_EDIT')" size="small" @click="openEdit(row)">编辑</el-button>
              <el-popconfirm title="确认删除该科目？" @confirm="remove(row.id)">
                <template #reference>
                  <el-button v-if="auth.hasMenu('BTN_SUBJECT_DELETE')" size="small" type="danger">删除</el-button>
                </template>
              </el-popconfirm>
            </div>
          </template>
        </el-table-column>
      </el-table>
    </div>

    <el-dialog v-model="dialogVisible" :title="editingId ? '编辑科目' : '新增科目'" width="620px">
      <el-form ref="formRef" :model="form" :rules="rules" label-width="90px">
        <el-form-item label="编码" prop="code">
          <el-input v-model="form.code" />
        </el-form-item>
        <el-form-item label="名称" prop="name">
          <el-input v-model="form.name" />
        </el-form-item>
        <el-form-item label="类别" prop="category">
          <el-select v-model="form.category" class="full">
            <el-option v-for="item in categories" :key="item.value" :label="item.label" :value="item.value" />
          </el-select>
        </el-form-item>
        <el-form-item label="父级">
          <el-select v-model="form.parentId" clearable filterable class="full">
            <el-option v-for="item in subjects" :key="item.id" :label="`${item.code} ${item.name}`" :value="item.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="说明" prop="description">
          <el-input v-model="form.description" type="textarea" :rows="3" :maxlength="fieldLimits.remark" show-word-limit />
        </el-form-item>
        <el-form-item label="启用">
          <el-switch v-model="form.enabled" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button v-if="editingId ? auth.hasMenu('BTN_SUBJECT_EDIT') : auth.hasMenu('BTN_SUBJECT_CREATE')" type="primary" @click="save">保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, reactive, ref, watch } from 'vue'
import { useRoute } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import type { FormInstance, FormRules } from 'element-plus'
import { Delete, Plus } from '@element-plus/icons-vue'
import { api } from '@/api/fm'
import { useAuthStore } from '@/stores/auth'
import type { SubjectCategory, SubjectView } from '@/types/api'
import { fieldLimits } from '@/utils/validators'
import { queryString } from '@/utils/routeQuery'

/**
 * SubjectNode 类型定义，用于约束页面状态、接口入参或接口返回数据结构。
 */
interface SubjectNode extends SubjectView {
  /**
   * 字段 children：表示表单、筛选条件、接口数据或组件状态中的 children 值。
   */
  children?: SubjectNode[]
}

/**
 * 常量 auth：保存当前模块的页面状态、配置项、接口实例或计算结果。
 */
const auth = useAuthStore()
/**
 * 常量 route：保存当前模块的页面状态、配置项、接口实例或计算结果。
 */
const route = useRoute()
/**
 * 常量 loading：保存当前模块的页面状态、配置项、接口实例或计算结果。
 */
const loading = ref(false)
/**
 * 常量 formRef：指向会计科目弹窗表单实例，用于字段级校验和红框提示。
 */
const formRef = ref<FormInstance>()
/**
 * 常量 subjects：保存当前模块的页面状态、配置项、接口实例或计算结果。
 */
const subjects = ref<SubjectView[]>([])
/**
 * 常量 selectedRows：保存当前模块的页面状态、配置项、接口实例或计算结果。
 */
const selectedRows = ref<SubjectView[]>([])
/**
 * 常量 onlyEnabled：保存当前模块的页面状态、配置项、接口实例或计算结果。
 */
const onlyEnabled = ref(false)
/**
 * 常量 dialogVisible：保存当前模块的页面状态、配置项、接口实例或计算结果。
 */
const dialogVisible = ref(false)
/**
 * 常量 editingId：保存当前模块的页面状态、配置项、接口实例或计算结果。
 */
const editingId = ref<number>()
/**
 * 常量 filters：保存当前模块的页面状态、配置项、接口实例或计算结果。
 */
const filters = reactive({
  /**
   * 字段 code：表示表单、筛选条件、接口数据或组件状态中的 code 值。
   */
  code: '',
  /**
   * 字段 name：表示表单、筛选条件、接口数据或组件状态中的 name 值。
   */
  name: '',
  /**
   * 字段 category：表示表单、筛选条件、接口数据或组件状态中的 category 值。
   */
  category: undefined as SubjectCategory | undefined,
  /**
   * 字段 parentId：表示表单、筛选条件、接口数据或组件状态中的 parentId 值。
   */
  parentId: undefined as number | undefined,
  /**
   * 字段 enabled：表示表单、筛选条件、接口数据或组件状态中的 enabled 值。
   */
  enabled: undefined as boolean | undefined,
  /**
   * 字段 description：表示表单、筛选条件、接口数据或组件状态中的 description 值。
   */
  description: ''
})

/**
 * 常量 categories：保存当前模块的页面状态、配置项、接口实例或计算结果。
 */
const categories: Array<{ label: string; value: SubjectCategory }> = [
  { label: '资产', value: 'ASSET' },
  { label: '负债', value: 'LIABILITY' },
  { label: '共同', value: 'COMMON' },
  { label: '权益', value: 'EQUITY' },
  { label: '收入', value: 'REVENUE' },
  { label: '成本', value: 'COST' },
  { label: '费用', value: 'EXPENSE' }
]

/**
 * 常量 form：保存当前模块的页面状态、配置项、接口实例或计算结果。
 */
const form = reactive({
  /**
   * 字段 code：表示表单、筛选条件、接口数据或组件状态中的 code 值。
   */
  code: '',
  /**
   * 字段 name：表示表单、筛选条件、接口数据或组件状态中的 name 值。
   */
  name: '',
  /**
   * 字段 category：表示表单、筛选条件、接口数据或组件状态中的 category 值。
   */
  category: 'ASSET' as SubjectCategory,
  /**
   * 字段 parentId：表示表单、筛选条件、接口数据或组件状态中的 parentId 值。
   */
  parentId: undefined as number | undefined,
  /**
   * 字段 enabled：表示表单、筛选条件、接口数据或组件状态中的 enabled 值。
   */
  enabled: true,
  /**
   * 字段 description：表示表单、筛选条件、接口数据或组件状态中的 description 值。
   */
  description: ''
})

/**
 * 会计科目弹窗字段校验规则。
 *
 * 实现步骤：必填项和说明长度都交给 Element Plus 表单校验，blur 后在字段下方提示并标红。
 */
const rules: FormRules = {
  code: [{ required: true, message: '请输入编码', trigger: 'blur' }],
  name: [{ required: true, message: '请输入名称', trigger: 'blur' }],
  category: [{ required: true, message: '请选择类别', trigger: 'change' }],
  description: [{ max: fieldLimits.remark, message: `说明不能超过${fieldLimits.remark}个字符`, trigger: 'blur' }]
}

/**
 * 常量 subjectTree：保存当前模块的页面状态、配置项、接口实例或计算结果。
 */
const subjectTree = computed(() => buildSubjectTree(filteredSubjects.value))
/**
 * 常量 expandedSubjectKeys：保存当前模块的页面状态、配置项、接口实例或计算结果。
 */
const expandedSubjectKeys = computed(() => {
  return subjectTree.value.map((item) => item.id)
})

/**
 * 常量 filteredSubjects：保存当前模块的页面状态、配置项、接口实例或计算结果。
 */
const filteredSubjects = computed(() => subjects.value)

/**
 * 执行 categoryLabel 方法。
 * 
 * 实现步骤：
 * 1. 读取当前页面状态或调用参数；
 * 2. 完成对应的校验、接口调用或数据转换；
 * 3. 更新页面状态或返回处理结果。
 */
function categoryLabel(value: SubjectCategory) {
  return categories.find((item) => item.value === value)?.label || value
}

/**
 * 构建会计科目树。
 *
 * 开发组织：ratel；开发人员：WenZhang；联系方式：18782945613。
 *
 * 实现步骤：
 * 1. 按科目 ID 创建节点副本，避免直接修改接口返回对象；
 * 2. 根据 parentId 把子科目挂到父科目 children 中；
 * 3. 找不到父级或父级不在当前筛选结果中时作为根节点展示，避免搜索后节点丢失；
 * 4. 对根节点和每层子节点按科目编码正序排序，保证树形列表稳定展示。
 */
function buildSubjectTree(rows: SubjectView[]) {
  /**
   * 常量 nodeMap：保存当前模块的页面状态、配置项、接口实例或计算结果。
   */
  const nodeMap = new Map<number, SubjectNode>()
  rows.forEach((item) => nodeMap.set(item.id, { ...item, children: [] }))
  /**
   * 常量 roots：保存当前模块的页面状态、配置项、接口实例或计算结果。
   */
  const roots: SubjectNode[] = []
  rows.forEach((item) => {
    /**
     * 常量 node：保存当前模块的页面状态、配置项、接口实例或计算结果。
     */
    const node = nodeMap.get(item.id)
    if (!node) {
      return
    }
    if (item.parentId && nodeMap.has(item.parentId)) {
      nodeMap.get(item.parentId)?.children?.push(node)
      return
    }
    roots.push(node)
  })
  sortSubjectNodes(roots)
  return roots
}

/**
 * 按科目编码递归排序科目树。
 *
 * 实现步骤：
 * 1. 当前层级先按 code 正序排序，编码一致时按 id 正序兜底；
 * 2. 对每个节点的 children 继续执行同样排序，保证所有层级展示顺序一致。
 */
function sortSubjectNodes(nodes: SubjectNode[]) {
  nodes.sort((first, second) => first.code.localeCompare(second.code, 'zh-Hans-CN') || first.id - second.id)
  nodes.forEach((node) => sortSubjectNodes(node.children || []))
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
    subjects.value = await api.subjects(onlyEnabled.value, subjectSearchParams())
    selectedRows.value = []
  } finally {
    loading.value = false
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
function handleSelectionChange(selection: SubjectView[]) {
  selectedRows.value = selection
}

/**
 * 组装会计科目字段级搜索参数。
 */
function subjectSearchParams() {
  return {
    /**
     * 字段 code：表示表单、筛选条件、接口数据或组件状态中的 code 值。
     */
    code: filters.code.trim() || undefined,
    /**
     * 字段 name：表示表单、筛选条件、接口数据或组件状态中的 name 值。
     */
    name: filters.name.trim() || undefined,
    /**
     * 字段 category：表示表单、筛选条件、接口数据或组件状态中的 category 值。
     */
    category: filters.category,
    /**
     * 字段 parentId：表示表单、筛选条件、接口数据或组件状态中的 parentId 值。
     */
    parentId: filters.parentId,
    /**
     * 字段 enabled：表示表单、筛选条件、接口数据或组件状态中的 enabled 值。
     */
    enabled: filters.enabled,
    /**
     * 字段 description：表示表单、筛选条件、接口数据或组件状态中的 description 值。
     */
    description: filters.description.trim() || undefined
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
     * 字段 code：表示表单、筛选条件、接口数据或组件状态中的 code 值。
     */
    code: '',
    /**
     * 字段 name：表示表单、筛选条件、接口数据或组件状态中的 name 值。
     */
    name: '',
    /**
     * 字段 category：表示表单、筛选条件、接口数据或组件状态中的 category 值。
     */
    category: undefined,
    /**
     * 字段 parentId：表示表单、筛选条件、接口数据或组件状态中的 parentId 值。
     */
    parentId: undefined,
    /**
     * 字段 enabled：表示表单、筛选条件、接口数据或组件状态中的 enabled 值。
     */
    enabled: undefined,
    /**
     * 字段 description：表示表单、筛选条件、接口数据或组件状态中的 description 值。
     */
    description: ''
  })
  void load()
}

/**
 * 执行 openCreate 方法。
 * 
 * 实现步骤：
 * 1. 读取当前页面状态或调用参数；
 * 2. 完成对应的校验、接口调用或数据转换；
 * 3. 更新页面状态或返回处理结果。
 */
function openCreate() {
  editingId.value = undefined
  Object.assign(form, { code: '', name: '', category: 'ASSET', parentId: undefined, enabled: true, description: '' })
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
function openEdit(row: SubjectView) {
  editingId.value = row.id
  Object.assign(form, {
    /**
     * 字段 code：表示表单、筛选条件、接口数据或组件状态中的 code 值。
     */
    code: row.code,
    /**
     * 字段 name：表示表单、筛选条件、接口数据或组件状态中的 name 值。
     */
    name: row.name,
    /**
     * 字段 category：表示表单、筛选条件、接口数据或组件状态中的 category 值。
     */
    category: row.category,
    /**
     * 字段 parentId：表示表单、筛选条件、接口数据或组件状态中的 parentId 值。
     */
    parentId: row.parentId,
    /**
     * 字段 enabled：表示表单、筛选条件、接口数据或组件状态中的 enabled 值。
     */
    enabled: row.enabled,
    /**
     * 字段 description：表示表单、筛选条件、接口数据或组件状态中的 description 值。
     */
    description: row.description || ''
  })
  dialogVisible.value = true
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
  /** 科目表单校验结果，失败时字段下方显示错误并阻止保存。 */
  const valid = await formRef.value?.validate().catch(() => false)
  if (!valid) {
    return
  }
  /**
   * 常量 payload：保存当前模块的页面状态、配置项、接口实例或计算结果。
   */
  const payload = { ...form, confirmDisableWithEnabledChildren: false }
  if (editingId.value && shouldConfirmDisableSubject()) {
    await ElMessageBox.confirm(
      '该科目存在启用状态的下级科目，停用后后续页面将不显示该科目及其下级数据，是否确认停用？',
      '停用确认',
      { type: 'warning', confirmButtonText: '确认停用', cancelButtonText: '取消' }
    )
    payload.confirmDisableWithEnabledChildren = true
  }
  if (editingId.value) {
    await api.updateSubject(editingId.value, payload)
  } else {
    await api.createSubject(payload)
  }
  ElMessage.success('保存成功')
  dialogVisible.value = false
  await load()
}

/**
 * 判断当前编辑科目停用前是否需要二次确认。
 *
 * 开发组织：ratel；开发人员：WenZhang；联系方式：18782945613。
 *
 * 实现步骤：仅在启用科目被改为停用时检查；从当前科目向下递归查找启用后代；存在启用后代则提示用户确认。
 */
function shouldConfirmDisableSubject() {
  /**
   * 常量 id：保存当前模块的页面状态、配置项、接口实例或计算结果。
   */
  const id = editingId.value
  if (!id || form.enabled) {
    return false
  }
  /**
   * 常量 current：保存当前模块的页面状态、配置项、接口实例或计算结果。
   */
  const current = subjects.value.find((item) => item.id === id)
  if (!current?.enabled) {
    return false
  }
  return hasEnabledSubjectDescendant(id)
}

/**
 * 递归判断科目是否存在启用后代。
 */
function hasEnabledSubjectDescendant(subjectId: number): boolean {
  return subjects.value
    .filter((item) => item.parentId === subjectId)
    .some((child) => child.enabled || hasEnabledSubjectDescendant(child.id))
}

/**
 * 执行 remove 方法。
 * 
 * 实现步骤：
 * 1. 读取当前页面状态或调用参数；
 * 2. 完成对应的校验、接口调用或数据转换；
 * 3. 更新页面状态或返回处理结果。
 */
async function remove(id: number) {
  await api.deleteSubject(id)
  ElMessage.success('删除成功')
  await load()
}

/**
 * 批量删除会计科目。
 *
 * 实现步骤：
 * 1. 校验是否已经勾选会计科目；
 * 2. 弹出二次确认，提醒删除会影响凭证和历史查询；
 * 3. 调用后端批量删除接口；
 * 4. 删除成功后刷新科目树。
 */
async function batchRemove() {
  if (selectedRows.value.length === 0) {
    ElMessage.warning('请选择需要删除的科目')
    return
  }
  await ElMessageBox.confirm(`确认删除选中的 ${selectedRows.value.length} 个会计科目？`, '批量删除确认', {
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
  await api.batchDeleteSubjects(selectedRows.value.map((row) => row.id))
  ElMessage.success('批量删除成功')
  await load()
}

onMounted(() => {
  applyRouteQuery()
  void load()
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
   * 常量 code：保存当前模块的页面状态、配置项、接口实例或计算结果。
   */
  const code = queryString(route.query.code)
  if (code && filters.code !== code) {
    filters.code = code
    return true
  }
  return false
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
</style>
