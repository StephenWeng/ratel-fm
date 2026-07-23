<template>
  <div class="page">
    <div class="page-header">
      <div>
        <h1 class="page-title">字典管理</h1>
        <p class="page-subtitle">维护采购方、物流方等层级基础字典。</p>
      </div>
    </div>

    <el-form class="filter-form" :model="filters" label-width="72px">
      <el-row :gutter="12">
        <el-col :xs="24" :sm="12" :md="8" :lg="5">
          <el-form-item label="名称">
            <el-input v-model="filters.name" clearable placeholder="包含匹配名称" />
          </el-form-item>
        </el-col>
        <el-col :xs="24" :sm="12" :md="8" :lg="5">
          <el-form-item label="编码">
            <el-input v-model="filters.code" clearable placeholder="精确匹配编码" />
          </el-form-item>
        </el-col>
        <el-col :xs="24" :sm="12" :md="8" :lg="5">
          <el-form-item label="状态">
            <el-select v-model="filters.enabled" clearable placeholder="全部" class="full-width">
              <el-option label="启用" :value="true" />
              <el-option label="禁用" :value="false" />
            </el-select>
          </el-form-item>
        </el-col>
        <el-col :xs="24" :sm="12" :md="8" :lg="5">
          <el-form-item label="父级">
            <el-select v-model="filters.parentId" clearable filterable class="full-width" placeholder="精确匹配父级">
              <el-option v-for="item in flatOptions" :key="item.id" :label="item.label" :value="item.id" />
            </el-select>
          </el-form-item>
        </el-col>
        <el-col :xs="24" :sm="12" :md="8" :lg="4">
          <el-form-item label="说明">
            <el-input v-model="filters.description" clearable placeholder="包含匹配说明" />
          </el-form-item>
        </el-col>
        <el-col :xs="24" :sm="12" :md="8" :lg="5">
          <el-form-item label=" " class="filter-actions">
            <el-button type="primary" @click="load">查询</el-button>
            <el-button @click="resetFilters">重置</el-button>
            <el-button v-if="auth.hasMenu('BTN_DICT_CREATE')" type="primary" :icon="Plus" @click="openCreateRoot">新增根字典</el-button>
          </el-form-item>
        </el-col>
      </el-row>
    </el-form>

    <div class="panel">
      <el-table
        :key="tableRenderKey"
        ref="tableRef"
        v-loading="loading"
        :data="dictionaryTree"
        row-key="id"
        stripe
        :lazy="tableLazy"
        :load="loadDictionaryChildren"
        highlight-current-row
        :tree-props="{ children: 'children', hasChildren: 'hasChildren' }"
        @expand-change="handleExpandChange"
        @current-change="handleCurrentChange"
      >
        <el-table-column prop="name" label="字典名称" min-width="180" />
        <el-table-column prop="code" label="字典编码" min-width="190" />
        <el-table-column label="状态" width="96">
          <template #default="{ row }">
            <el-tag :type="row.enabled ? 'success' : 'info'" size="small">{{ row.enabled ? '启用' : '禁用' }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="sortOrder" label="排序" width="90" />
        <el-table-column prop="description" label="说明" min-width="220" />
        <el-table-column label="操作" width="240" fixed="right">
          <template #default="{ row }">
            <div class="table-actions">
              <el-button v-if="auth.hasMenu('BTN_DICT_CREATE')" size="small" @click="openCreateChild(row)">新增下级</el-button>
              <el-button v-if="auth.hasMenu('BTN_DICT_EDIT')" size="small" @click="openEdit(row)">编辑</el-button>
              <el-popconfirm title="确认删除该字典？" @confirm="remove(row.id)">
                <template #reference>
                  <el-button v-if="auth.hasMenu('BTN_DICT_DELETE')" size="small" type="danger">删除</el-button>
                </template>
              </el-popconfirm>
            </div>
          </template>
        </el-table-column>
      </el-table>
    </div>

    <el-dialog v-model="dialogVisible" :title="dialogTitle" width="620px">
      <el-form ref="formRef" :model="form" :rules="rules" label-width="96px">
        <el-form-item label="父级字典">
          <el-select v-model="form.parentId" clearable filterable class="full-width" placeholder="不选择则为根字典">
            <el-option
              v-for="item in flatOptions"
              :key="item.id"
              :label="item.label"
              :value="item.id"
              :disabled="editingId === item.id"
            />
          </el-select>
        </el-form-item>
        <el-form-item label="字典编码" prop="code">
          <el-input v-model="form.code" placeholder="可不填，后端自动生成" />
        </el-form-item>
        <el-form-item label="字典名称" prop="name">
          <el-input v-model="form.name" />
        </el-form-item>
        <el-form-item label="排序号" prop="sortOrder">
          <el-input-number v-model="form.sortOrder" :min="0" :step="1" :precision="0" controls-position="right" />
        </el-form-item>
        <el-form-item label="是否启用">
          <el-switch v-model="form.enabled" />
        </el-form-item>
        <el-form-item label="说明" prop="description">
          <el-input v-model="form.description" type="textarea" :rows="3" :maxlength="fieldLimits.remark" show-word-limit />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button v-if="canSave" type="primary" @click="save">保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { computed, nextTick, onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox, type FormInstance, type FormRules } from 'element-plus'
import type { TableInstance } from 'element-plus'
import { Plus } from '@element-plus/icons-vue'
import { api } from '@/api/fm'
import { useAuthStore } from '@/stores/auth'
import type { BasicDictionaryView } from '@/types/api'
import { fieldLimits } from '@/utils/validators'

/**
 * 字典管理页面。
 *
 * 开发组织：ratel；开发人员：WenZhang；联系方式：18782945613。
 *
 * @author ratel
 */
const auth = useAuthStore()
/**
 * 常量 loading：保存当前模块的页面状态、配置项、接口实例或计算结果。
 */
const loading = ref(false)
/**
 * 常量 dictionaryTree：保存当前模块的页面状态、配置项、接口实例或计算结果。
 */
const dictionaryTree = ref<BasicDictionaryView[]>([])
/**
 * 常量 tableLazy：控制字典树表格是否通过展开节点按需加载子级，避免首屏一次性渲染全量字典。
 */
const tableLazy = ref(true)
/**
 * 常量 tableRef：引用字典树表格实例，用于刷新后恢复当前选中行。
 */
const tableRef = ref<TableInstance>()
/**
 * 常量 tableRenderKey：重新加载根数据时刷新表格实例，清理 Element Plus 懒加载树的内部缓存。
 */
const tableRenderKey = ref(0)
/**
 * 常量 expandedRowKeys：保存用户已经展开的字典节点 ID，新增、修改、删除后继续展开这些节点。
 */
const expandedRowKeys = ref<number[]>([])
/**
 * 常量 currentRowId：保存当前操作节点 ID，数据刷新后优先定位回该节点。
 */
const currentRowId = ref<number | undefined>()
/**
 * 常量 dialogVisible：保存当前模块的页面状态、配置项、接口实例或计算结果。
 */
const dialogVisible = ref(false)
/**
 * 常量 editingId：保存当前模块的页面状态、配置项、接口实例或计算结果。
 */
const editingId = ref<number | null>(null)
/**
 * 常量 formRef：保存当前模块的页面状态、配置项、接口实例或计算结果。
 */
const formRef = ref<FormInstance>()
/**
 * 变量 loadSerial：保存当前模块的页面状态、配置项、接口实例或计算结果。
 */
let loadSerial = 0

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
   * 字段 description：表示表单、筛选条件、接口数据或组件状态中的 description 值。
   */
  description: '',
  /**
   * 字段 enabled：表示表单、筛选条件、接口数据或组件状态中的 enabled 值。
   */
  enabled: undefined as boolean | undefined,
  /**
   * 字段 parentId：表示表单、筛选条件、接口数据或组件状态中的 parentId 值。
   */
  parentId: undefined as number | undefined
})

/**
 * 常量 form：保存当前模块的页面状态、配置项、接口实例或计算结果。
 */
const form = reactive({
  /**
   * 字段 parentId：表示表单、筛选条件、接口数据或组件状态中的 parentId 值。
   */
  parentId: undefined as number | undefined,
  /**
   * 字段 code：表示表单、筛选条件、接口数据或组件状态中的 code 值。
   */
  code: '',
  /**
   * 字段 name：表示表单、筛选条件、接口数据或组件状态中的 name 值。
   */
  name: '',
  /**
   * 字段 sortOrder：表示表单、筛选条件、接口数据或组件状态中的 sortOrder 值。
   */
  sortOrder: 0,
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
 * 常量 rules：保存当前模块的页面状态、配置项、接口实例或计算结果。
 */
const rules: FormRules = {
  /**
   * 字段 code：表示表单、筛选条件、接口数据或组件状态中的 code 值。
   */
  code: [{ max: 80, message: '字典编码长度不能超过80个字符', trigger: 'blur' }],
  /**
   * 字段 name：表示表单、筛选条件、接口数据或组件状态中的 name 值。
   */
  name: [
    { required: true, message: '请输入字典名称', trigger: 'blur' },
    { max: 120, message: '字典名称长度不能超过120个字符', trigger: 'blur' }
  ],
  /**
   * 字段 description：表示表单、筛选条件、接口数据或组件状态中的 description 值。
   */
  description: [{ max: fieldLimits.remark, message: '说明长度不能超过2000个中文字符', trigger: 'blur' }]
}

/**
 * 常量 dialogTitle：保存当前模块的页面状态、配置项、接口实例或计算结果。
 */
const dialogTitle = computed(() => (editingId.value ? '编辑字典' : '新增字典'))
/**
 * 常量 canSave：保存当前模块的页面状态、配置项、接口实例或计算结果。
 */
const canSave = computed(() => editingId.value ? auth.hasMenu('BTN_DICT_EDIT') : auth.hasMenu('BTN_DICT_CREATE'))
/**
 * 常量 flatOptions：保存当前模块的页面状态、配置项、接口实例或计算结果。
 */
const flatOptions = computed(() => flattenOptions(dictionaryTree.value))

/**
 * 加载基础字典树或搜索结果。
 *
 * 实现步骤：
 * 1. 记录刷新前的展开节点和当前节点，避免保存后丢失用户正在维护的位置；
 * 2. 无搜索条件时读取完整字典树，有搜索条件时调用搜索接口返回命中路径；
 * 3. 数据写入后过滤已经不存在的展开节点，并优先定位到调用方指定节点；
 * 4. 等待 DOM 更新后设置表格当前行，让用户继续停留在原操作上下文。
 */
async function load(options: { focusId?: number; fallbackId?: number } = {}) {
  /**
   * 常量 serial：保存当前模块的页面状态、配置项、接口实例或计算结果。
   */
  const serial = ++loadSerial
  /**
   * 常量 params：保存当前模块的页面状态、配置项、接口实例或计算结果。
   */
  const params = dictionarySearchParams()
  /** 刷新前展开的节点集合，用于新数据回来后做交集过滤并恢复展开状态。 */
  const previousExpandedIds = new Set(expandedRowKeys.value)
  /** 刷新后优先定位的节点 ID，新增时回到父节点，编辑时回到编辑节点，删除时回到前序节点。 */
  const preferredFocusId = options.focusId ?? currentRowId.value
  loading.value = true
  try {
    /**
     * 常量 rows：保存当前模块的页面状态、配置项、接口实例或计算结果。
     */
    const rows = params ? await api.searchDictionaries(params) : await api.dictionaryRoots()
    if (serial === loadSerial) {
      tableLazy.value = !params
      dictionaryTree.value = sortDictionaryNodes(rows)
      tableRenderKey.value += 1
      const nodeMap = flattenDictionaryMap(dictionaryTree.value)
      const focusId = preferredFocusId && nodeMap.has(preferredFocusId)
        ? preferredFocusId
        : options.fallbackId && nodeMap.has(options.fallbackId)
          ? options.fallbackId
          : undefined
      expandedRowKeys.value = Array.from(previousExpandedIds).filter((id) => nodeMap.has(id))
      if (focusId) {
        expandAncestors(focusId, nodeMap)
      }
      currentRowId.value = focusId
      await restoreExpandedRows(nodeMap)
      await restoreCurrentRow(focusId, nodeMap)
    }
  } finally {
    if (serial === loadSerial) {
      loading.value = false
    }
  }
}

/**
 * 组装字典搜索参数。
 *
 * 实现步骤：名称和说明交给后端做包含匹配；编码、父级和启用状态按等值匹配；没有任何条件时返回 undefined，让页面只加载根节点。
 */
function dictionarySearchParams() {
  /**
   * 常量 params：保存当前模块的页面状态、配置项、接口实例或计算结果。
   */
  const params = {
    /**
     * 字段 code：表示表单、筛选条件、接口数据或组件状态中的 code 值。
     */
    code: filters.code.trim() || undefined,
    /**
     * 字段 name：表示表单、筛选条件、接口数据或组件状态中的 name 值。
     */
    name: filters.name.trim() || undefined,
    /**
     * 字段 description：表示表单、筛选条件、接口数据或组件状态中的 description 值。
     */
    description: filters.description.trim() || undefined,
    /**
     * 字段 enabled：表示表单、筛选条件、接口数据或组件状态中的 enabled 值。
     */
    enabled: filters.enabled,
    /**
     * 字段 parentId：表示表单、筛选条件、接口数据或组件状态中的 parentId 值。
     */
    parentId: filters.parentId
  }
  return Object.values(params).some((value) => value !== undefined && value !== '') ? params : undefined
}

/**
 * 记录树节点展开或收起。
 *
 * 实现步骤：
 * 1. 展开节点时把节点 ID 加入 expandedRowKeys；
 * 2. 收起节点时从 expandedRowKeys 删除该 ID；
 * 3. 后续保存、删除或重新查询时按该集合恢复用户已打开的节点。
 */
function handleExpandChange(row: BasicDictionaryView, expanded: boolean | BasicDictionaryView[]) {
  /** 当前展开集合，使用 Set 避免重复 ID。 */
  const ids = new Set(expandedRowKeys.value)
  const isExpanded = Array.isArray(expanded)
    ? expanded.some((item) => item.id === row.id)
    : expanded
  if (isExpanded) {
    ids.add(row.id)
  } else {
    ids.delete(row.id)
  }
  expandedRowKeys.value = Array.from(ids)
}

/**
 * 懒加载字典表格子节点。
 *
 * 实现步骤：
 * 1. Element Plus 展开树节点时传入当前行；
 * 2. 按当前字典 ID 只请求直接子级；
 * 3. 将返回子级挂回当前本地树，后续父级下拉和定位逻辑可以复用已加载节点。
 */
async function loadDictionaryChildren(row: BasicDictionaryView, _treeNode: unknown, resolve: (data: BasicDictionaryView[]) => void) {
  try {
    const children = sortDictionaryNodes(await api.dictionaryChildren(row.id))
    resolve(children)
    attachChildren(dictionaryTree.value, row.id, children)
  } catch {
    // 接口层已经展示统一异常弹窗；这里结束本次加载，避免树表格 loading 状态残留。
    resolve([])
  }
}

/**
 * 更新已展开字典节点的子级。
 *
 * 实现步骤：
 * 1. 把后端返回的直接子级写回本地字典树；
 * 2. 同步 Element Plus 懒加载表格内部缓存；
 * 3. 保存、新增下级、删除后复用该方法，避免重新渲染整棵字典树。
 */
function updateLoadedChildren(parentId: number, children: BasicDictionaryView[]) {
  const normalizedChildren = sortDictionaryNodes(children)
  attachChildren(dictionaryTree.value, parentId, normalizedChildren)
  const table = tableRef.value as unknown as { updateKeyChildren?: (key: number, data: BasicDictionaryView[]) => void }
  table?.updateKeyChildren?.(parentId, normalizedChildren)
}

/**
 * 记录当前选中字典行。
 *
 * 实现步骤：
 * 1. 表格当前行变化时读取当前字典 ID；
 * 2. 保存到 currentRowId；
 * 3. 新增、编辑、删除后刷新数据时复用该 ID 做定位。
 */
function handleCurrentChange(row?: BasicDictionaryView) {
  currentRowId.value = row?.id
}

/**
 * 打开新增根字典弹窗。
 */
function openCreateRoot() {
  editingId.value = null
  resetForm(undefined)
  dialogVisible.value = true
}

/**
 * 打开新增下级字典弹窗。
 */
function openCreateChild(row: BasicDictionaryView) {
  currentRowId.value = row.id
  if (!expandedRowKeys.value.includes(row.id)) {
    expandedRowKeys.value = [...expandedRowKeys.value, row.id]
  }
  editingId.value = null
  resetForm(row.id)
  dialogVisible.value = true
}

/**
 * 打开编辑字典弹窗。
 */
function openEdit(row: BasicDictionaryView) {
  currentRowId.value = row.id
  editingId.value = row.id
  Object.assign(form, {
    /**
     * 字段 parentId：表示表单、筛选条件、接口数据或组件状态中的 parentId 值。
     */
    parentId: row.parentId,
    /**
     * 字段 code：表示表单、筛选条件、接口数据或组件状态中的 code 值。
     */
    code: row.code,
    /**
     * 字段 name：表示表单、筛选条件、接口数据或组件状态中的 name 值。
     */
    name: row.name,
    /**
     * 字段 sortOrder：表示表单、筛选条件、接口数据或组件状态中的 sortOrder 值。
     */
    sortOrder: row.sortOrder,
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
 * 重置字典表单。
 */
function resetForm(parentId?: number) {
  Object.assign(form, {
    parentId,
    /**
     * 字段 code：表示表单、筛选条件、接口数据或组件状态中的 code 值。
     */
    code: '',
    /**
     * 字段 name：表示表单、筛选条件、接口数据或组件状态中的 name 值。
     */
    name: '',
    /**
     * 字段 sortOrder：表示表单、筛选条件、接口数据或组件状态中的 sortOrder 值。
     */
    sortOrder: 0,
    /**
     * 字段 enabled：表示表单、筛选条件、接口数据或组件状态中的 enabled 值。
     */
    enabled: true,
    /**
     * 字段 description：表示表单、筛选条件、接口数据或组件状态中的 description 值。
     */
    description: ''
  })
}

/**
 * 保存基础字典。
 *
 * 实现步骤：
 * 1. 执行前端字段格式校验；
 * 2. 按编辑状态调用新增或修改接口；
 * 3. 成功后刷新树表格。
 */
async function save() {
  /**
   * 常量 valid：保存当前模块的页面状态、配置项、接口实例或计算结果。
   */
  const valid = await formRef.value?.validate().catch(() => false)
  if (!valid) {
    return
  }
  /**
   * 常量 payload：保存当前模块的页面状态、配置项、接口实例或计算结果。
   */
  const payload = {
    ...form,
    /**
     * 字段 code：表示表单、筛选条件、接口数据或组件状态中的 code 值。
     */
    code: form.code || undefined,
    /**
     * 字段 parentId：表示表单、筛选条件、接口数据或组件状态中的 parentId 值。
     */
    parentId: form.parentId || null,
    /**
     * 字段 confirmDisableWithEnabledChildren：表示表单、筛选条件、接口数据或组件状态中的 confirmDisableWithEnabledChildren 值。
     */
    confirmDisableWithEnabledChildren: false
  }
  /** 保存完成后需要定位的字典节点：新增下级回到父级，编辑回到当前编辑节点。 */
  const focusId = editingId.value ?? form.parentId ?? currentRowId.value
  /** 新增根字典且之前没有当前节点时，使用后端返回的新节点作为兜底定位点。 */
  let saved: BasicDictionaryView | undefined
  if (editingId.value) {
    if (shouldConfirmDisableDictionary()) {
      await ElMessageBox.confirm(
        '该字典存在启用状态的下级字典，停用后后续页面将不显示该字典及其下级数据，是否确认停用？',
        '停用确认',
        { type: 'warning', confirmButtonText: '确认停用', cancelButtonText: '取消' }
      )
      payload.confirmDisableWithEnabledChildren = true
    }
    saved = await api.updateDictionary(editingId.value, payload)
    updateDictionaryNode(dictionaryTree.value, saved)
  } else {
    saved = await api.createDictionary(payload)
    if (!dictionarySearchParams()) {
      if (saved.parentId) {
        const children = sortDictionaryNodes(await api.dictionaryChildren(saved.parentId))
        updateLoadedChildren(saved.parentId, children)
        if (!expandedRowKeys.value.includes(saved.parentId)) {
          expandedRowKeys.value = [...expandedRowKeys.value, saved.parentId]
        }
      } else {
        dictionaryTree.value = sortDictionaryNodes([...dictionaryTree.value, saved])
      }
    }
  }
  ElMessage.success('保存成功')
  dialogVisible.value = false
  if (dictionarySearchParams()) {
    await load({ focusId: focusId ?? saved?.id })
    return
  }
  const nodeMap = flattenDictionaryMap(dictionaryTree.value)
  const finalFocusId = saved?.id && nodeMap.has(saved.id) ? saved.id : focusId
  currentRowId.value = finalFocusId
  await restoreCurrentRow(finalFocusId, nodeMap)
}

/**
 * 判断当前编辑字典停用前是否需要二次确认。
 *
 * 开发组织：ratel；开发人员：WenZhang；联系方式：18782945613。
 *
 * 实现步骤：仅在启用字典被改为停用时检查；懒加载模式下本地不保证已经拥有全部子孙，所以只要当前节点存在下级就提示确认，后端继续做启用后代的精确校验。
 */
function shouldConfirmDisableDictionary() {
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
  const current = findDictionaryNode(dictionaryTree.value, id)
  return Boolean(current?.enabled && current.hasChildren)
}

/**
 * 按 ID 在字典树中查找节点。
 */
function findDictionaryNode(nodes: BasicDictionaryView[], id: number): BasicDictionaryView | undefined {
  for (const node of nodes) {
    if (node.id === id) {
      return node
    }
    /**
     * 常量 found：保存当前模块的页面状态、配置项、接口实例或计算结果。
     */
    const found = findDictionaryNode(node.children || [], id)
    if (found) {
      return found
    }
  }
  return undefined
}

/**
 * 拉平成字典节点映射。
 *
 * 实现步骤：
 * 1. 深度遍历当前字典树；
 * 2. 记录每个节点本身、父节点 ID 和同级节点列表；
 * 3. 供刷新定位、展开祖先和删除后回退定位复用。
 */
function flattenDictionaryMap(nodes: BasicDictionaryView[], parentId?: number, result = new Map<number, { node: BasicDictionaryView; parentId?: number; siblings: BasicDictionaryView[] }>()) {
  for (const node of nodes) {
    result.set(node.id, { node, parentId, siblings: nodes })
    flattenDictionaryMap(node.children || [], node.id, result)
  }
  return result
}

/**
 * 展开指定节点的所有祖先节点。
 *
 * 实现步骤：
 * 1. 根据节点映射从目标节点逐级读取 parentId；
 * 2. 把每个祖先 ID 写入展开集合；
 * 3. 确保刷新后目标节点不会因为父级收起而不可见。
 */
function expandAncestors(id: number, nodeMap: Map<number, { parentId?: number }>) {
  /** 展开集合，使用 Set 避免重复写入。 */
  const ids = new Set(expandedRowKeys.value)
  /** 当前正在向上查找祖先链的节点 ID。 */
  let cursor: number | undefined = id
  while (cursor) {
    const parentId: number | undefined = nodeMap.get(cursor)?.parentId
    if (!parentId) {
      break
    }
    ids.add(parentId)
    cursor = parentId
  }
  expandedRowKeys.value = Array.from(ids)
}

/**
 * 恢复树表格当前行。
 *
 * 实现步骤：
 * 1. 等待 Vue 完成表格 DOM 更新；
 * 2. 根据目标 ID 从映射中取出最新节点对象；
 * 3. 调用 Element Plus 表格实例设置当前行。
 */
async function restoreCurrentRow(focusId: number | undefined, nodeMap: Map<number, { node: BasicDictionaryView }>) {
  await nextTick()
  tableRef.value?.setCurrentRow(focusId ? nodeMap.get(focusId)?.node : undefined)
}

/**
 * 恢复刷新前已经展开的字典节点。
 *
 * 实现步骤：
 * 1. 等待表格重新渲染根节点；
 * 2. 只对当前数据中仍存在的节点执行展开；
 * 3. 使用表格实例方法恢复展开，避免懒加载树表被 expand-row-keys 受控状态卡住。
 */
async function restoreExpandedRows(nodeMap: Map<number, { node: BasicDictionaryView }>) {
  await nextTick()
  for (const id of expandedRowKeys.value) {
    const node = nodeMap.get(id)?.node
    if (node) {
      tableRef.value?.toggleRowExpansion(node, true)
    }
  }
}

/**
 * 计算删除节点后的回退定位节点。
 *
 * 实现步骤：
 * 1. 在当前树中找到待删除节点所属同级列表；
 * 2. 优先返回上一个同级节点，符合“删除当前节点定位到上一个节点”的操作习惯；
 * 3. 没有上一个同级时返回父节点，没有父节点时再返回下一个同级节点。
 */
function previousDictionaryNodeId(id: number) {
  /** 当前字典树的扁平节点映射。 */
  const nodeMap = flattenDictionaryMap(dictionaryTree.value)
  /** 当前待删除节点的位置信息。 */
  const current = nodeMap.get(id)
  if (!current) {
    return currentRowId.value
  }
  /** 当前节点在同级列表中的位置。 */
  const index = current.siblings.findIndex((node) => node.id === id)
  return current.siblings[index - 1]?.id ?? current.parentId ?? current.siblings[index + 1]?.id
}

/**
 * 从展开集合中移除删除节点及其子孙节点。
 *
 * 实现步骤：
 * 1. 在删除前的本地树中收集待删除节点子树 ID；
 * 2. 从 expandedRowKeys 中剔除这些 ID；
 * 3. 避免刷新后继续尝试展开已经不存在的节点。
 */
function removeExpandedBranch(id: number) {
  /** 待删除子树 ID 集合。 */
  const removingIds = new Set<number>()
  collectDictionaryIds(findDictionaryNode(dictionaryTree.value, id), removingIds)
  expandedRowKeys.value = expandedRowKeys.value.filter((item) => !removingIds.has(item))
}

/**
 * 收集某个字典节点及其全部子孙 ID。
 */
function collectDictionaryIds(node: BasicDictionaryView | undefined, result: Set<number>) {
  if (!node) {
    return
  }
  result.add(node.id)
  ;(node.children || []).forEach((child) => collectDictionaryIds(child, result))
}

/**
 * 判断当前节点是否属于指定父节点的子孙节点。
 */
function isDescendantId(nodes: BasicDictionaryView[], id: number | undefined, ancestorId: number) {
  if (!id) {
    return false
  }
  /** 祖先节点，找不到时说明当前本地树没有该分支。 */
  const ancestor = findDictionaryNode(nodes, ancestorId)
  /** 在祖先子树下查找当前节点，命中则表示当前节点会随祖先一起删除。 */
  return Boolean(ancestor && findDictionaryNode(ancestor.children || [], id))
}

/**
 * 删除基础字典。
 */
async function remove(id: number) {
  /** 删除前先计算回退定位点，避免数据删除后无法判断同级前序节点。 */
  const fallbackId = previousDictionaryNodeId(id)
  const deletedParentId = flattenDictionaryMap(dictionaryTree.value).get(id)?.parentId
  await api.deleteDictionary(id)
  removeExpandedBranch(id)
  if (currentRowId.value === id || isDescendantId(dictionaryTree.value, currentRowId.value, id)) {
    currentRowId.value = fallbackId
  }
  ElMessage.success('删除成功')
  if (dictionarySearchParams()) {
    await load({ focusId: fallbackId })
    return
  }
  removeDictionaryNode(dictionaryTree.value, id)
  if (deletedParentId) {
    const children = sortDictionaryNodes(await api.dictionaryChildren(deletedParentId))
    updateLoadedChildren(deletedParentId, children)
  }
  const nodeMap = flattenDictionaryMap(dictionaryTree.value)
  await restoreCurrentRow(fallbackId, nodeMap)
}

/**
 * 重置搜索条件。
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
     * 字段 description：表示表单、筛选条件、接口数据或组件状态中的 description 值。
     */
    description: '',
    /**
     * 字段 enabled：表示表单、筛选条件、接口数据或组件状态中的 enabled 值。
     */
    enabled: undefined,
    /**
     * 字段 parentId：表示表单、筛选条件、接口数据或组件状态中的 parentId 值。
     */
    parentId: undefined
  })
  void load()
}

/**
 * 把懒加载返回的子级挂回本地树。
 */
function attachChildren(nodes: BasicDictionaryView[], id: number, children: BasicDictionaryView[]): boolean {
  for (const node of nodes) {
    if (node.id === id) {
      const normalizedChildren = sortDictionaryNodes(children)
      node.hasChildren = normalizedChildren.length > 0
      if (normalizedChildren.length > 0) {
        node.children = normalizedChildren
      } else {
        delete node.children
      }
      return true
    }
    if (attachChildren(node.children || [], id, children)) {
      return true
    }
  }
  return false
}

/**
 * 更新当前已加载字典树中的单个节点。
 *
 * 实现步骤：
 * 1. 递归查找保存结果对应的节点；
 * 2. 找到后覆盖基础字段，并保留已加载的 children；
 * 3. 返回是否命中，调用方可在未命中时选择重新查询。
 */
function updateDictionaryNode(nodes: BasicDictionaryView[], updated: BasicDictionaryView): boolean {
  for (const node of nodes) {
    if (node.id === updated.id) {
      const loadedChildren = Array.isArray(node.children) && node.children.length > 0 ? node.children : updated.children
      Object.assign(node, { ...updated })
      if (loadedChildren && loadedChildren.length > 0) {
        node.children = loadedChildren
      } else {
        delete node.children
      }
      return true
    }
    if (updateDictionaryNode(node.children || [], updated)) {
      return true
    }
  }
  return false
}

/**
 * 从当前已加载字典树中删除一个节点。
 *
 * 实现步骤：
 * 1. 先在当前层级查找待删除节点；
 * 2. 命中后从数组中移除；
 * 3. 未命中时继续递归已加载子级。
 */
function removeDictionaryNode(nodes: BasicDictionaryView[], id: number): boolean {
  const index = nodes.findIndex((node) => node.id === id)
  if (index >= 0) {
    nodes.splice(index, 1)
    return true
  }
  return nodes.some((node) => removeDictionaryNode(node.children || [], id))
}

/**
 * 拉平树节点，供父级字典下拉选择。
 */
function flattenOptions(nodes: BasicDictionaryView[], level = 0, seen = new Set<number>()): Array<{ id: number; label: string }> {
  /**
   * 常量 result：保存当前模块的页面状态、配置项、接口实例或计算结果。
   */
  const result: Array<{ id: number; label: string }> = []
  sortDictionaryNodes(nodes).forEach((node) => {
    if (seen.has(node.id)) {
      return
    }
    seen.add(node.id)
    result.push({ id: node.id, label: `${'　'.repeat(level)}${node.name}` })
    result.push(...flattenOptions(node.children || [], level + 1, seen))
  })
  return result
}

/**
 * 执行 sortDictionaryNodes 方法。
 * 
 * 实现步骤：
 * 1. 读取当前页面状态或调用参数；
 * 2. 完成对应的校验、接口调用或数据转换；
 * 3. 更新页面状态或返回处理结果。
 */
function sortDictionaryNodes(nodes: BasicDictionaryView[]): BasicDictionaryView[] {
  return [...nodes].sort(compareDictionary).map((node) => {
    const sortedNode: BasicDictionaryView = { ...node, hasChildren: Boolean(node.hasChildren) }
    if (Array.isArray(node.children) && node.children.length > 0) {
      sortedNode.children = sortDictionaryNodes(node.children)
      sortedNode.hasChildren = true
    } else {
      delete sortedNode.children
    }
    return sortedNode
  })
}

/**
 * 执行 compareDictionary 方法。
 * 
 * 实现步骤：
 * 1. 读取当前页面状态或调用参数；
 * 2. 完成对应的校验、接口调用或数据转换；
 * 3. 更新页面状态或返回处理结果。
 */
function compareDictionary(left: BasicDictionaryView, right: BasicDictionaryView) {
  if (left.sortOrder !== right.sortOrder) {
    return left.sortOrder - right.sortOrder
  }
  return left.id - right.id
}

onMounted(load)
</script>

<style scoped>
.full-width {
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
