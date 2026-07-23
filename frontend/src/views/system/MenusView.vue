<template>
  <div class="page">
    <div class="page-header">
      <div>
        <h1 class="page-title">菜单管理</h1>
        <p class="page-subtitle">维护模块、页面、按钮三级授权资源。</p>
      </div>
    </div>

    <el-form class="filter-form" :model="filters" label-width="86px">
      <el-row :gutter="12">
        <el-col :xs="24" :sm="12" :md="8" :lg="6">
          <el-form-item label="菜单名称">
            <el-input v-model="filters.name" clearable placeholder="模糊查询名称" />
          </el-form-item>
        </el-col>
        <el-col :xs="24" :sm="12" :md="8" :lg="6">
          <el-form-item label="菜单编码">
            <el-input v-model="filters.code" clearable placeholder="模糊查询编码" />
          </el-form-item>
        </el-col>
        <el-col :xs="24" :sm="12" :md="8" :lg="6">
          <el-form-item label="类型">
            <el-select v-model="filters.type" clearable class="full-width" placeholder="全部">
              <el-option v-for="item in menuTypeOptions" :key="item.value" :label="item.label" :value="item.value" />
            </el-select>
          </el-form-item>
        </el-col>
        <el-col :xs="24" :sm="12" :md="8" :lg="6">
          <el-form-item label="路由">
            <el-input v-model="filters.routePath" clearable placeholder="模糊查询路由" />
          </el-form-item>
        </el-col>
        <el-col :xs="24" :sm="12" :md="8" :lg="6">
          <el-form-item label="权限码">
            <el-select v-model="filters.permissionCode" clearable filterable class="full-width" placeholder="全部">
              <el-option v-for="item in permissionOptions" :key="item" :label="item" :value="item" />
            </el-select>
          </el-form-item>
        </el-col>
        <el-col :xs="24" :sm="12" :md="8" :lg="6">
          <el-form-item label="状态">
            <el-select v-model="filters.enabled" clearable class="full-width" placeholder="全部">
              <el-option label="启用" :value="true" />
              <el-option label="停用" :value="false" />
            </el-select>
          </el-form-item>
        </el-col>
        <el-col :xs="24" :sm="12" :md="8" :lg="6">
          <el-form-item label=" " class="filter-actions">
            <el-button type="primary" @click="load">查询</el-button>
            <el-button @click="resetFilters">重置</el-button>
            <el-button v-if="auth.hasMenu('BTN_MENU_CREATE')" type="primary" :icon="Plus" @click="openCreate">新增菜单</el-button>
          </el-form-item>
        </el-col>
      </el-row>
    </el-form>

    <div class="panel">
      <el-table
        v-loading="loading"
        :data="menuTree"
        row-key="id"
        stripe
        :expand-row-keys="rootExpandedKeys"
        :tree-props="{ children: 'children' }"
      >
        <el-table-column prop="name" label="菜单名称" min-width="180" />
        <el-table-column prop="code" label="菜单编码" min-width="210" />
        <el-table-column label="类型" width="110">
          <template #default="{ row }">
            <el-tag :type="typeTag(row.type)" size="small">{{ typeLabel(row.type) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="routePath" label="路由" min-width="160" />
        <el-table-column prop="permissionCode" label="后端权限码" min-width="180" />
        <el-table-column prop="sortOrder" label="排序" width="90" />
        <el-table-column label="启用" width="90">
          <template #default="{ row }">
            <el-switch v-model="row.enabled" disabled />
          </template>
        </el-table-column>
        <el-table-column label="操作" width="180" fixed="right">
          <template #default="{ row }">
            <div class="table-actions">
              <el-button v-if="auth.hasMenu('BTN_MENU_EDIT')" size="small" @click="openEdit(row)">编辑</el-button>
              <el-popconfirm title="确认删除该菜单？" @confirm="remove(row.id)">
                <template #reference>
                  <el-button v-if="auth.hasMenu('BTN_MENU_DELETE')" size="small" type="danger">删除</el-button>
                </template>
              </el-popconfirm>
            </div>
          </template>
        </el-table-column>
      </el-table>
    </div>

    <el-dialog v-model="dialogVisible" :title="editing ? '编辑菜单' : '新增菜单'" width="640px">
      <el-form :model="form" label-width="104px">
        <el-form-item label="菜单编码" required>
          <el-input v-model="form.code" placeholder="例如 PAGE_MENUS、BTN_MENU_CREATE" />
        </el-form-item>
        <el-form-item label="菜单名称" required>
          <el-input v-model="form.name" />
        </el-form-item>
        <el-form-item label="菜单类型" required>
          <el-segmented v-model="form.type" :options="menuTypeOptions" @change="onTypeChange" />
        </el-form-item>
        <el-form-item v-if="form.type !== 'MODULE'" label="父级菜单" required>
          <el-select v-model="form.parentId" filterable clearable class="full-width">
            <el-option
              v-for="item in parentOptions"
              :key="item.id"
              :label="`${item.name}（${item.code}）`"
              :value="item.id"
            />
          </el-select>
        </el-form-item>
        <el-form-item label="前端路由">
          <el-input v-model="form.routePath" placeholder="页面菜单填写，例如 /menus" />
        </el-form-item>
        <el-form-item label="后端权限码">
          <el-select v-model="form.permissionCode" clearable filterable class="full-width">
            <el-option v-for="item in permissionOptions" :key="item" :label="item" :value="item" />
          </el-select>
        </el-form-item>
        <el-form-item label="排序号" required>
          <el-input-number v-model="form.sortOrder" :min="0" :step="1" :precision="0" controls-position="right" />
        </el-form-item>
        <el-form-item label="是否启用">
          <el-switch v-model="form.enabled" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button v-if="editing ? auth.hasMenu('BTN_MENU_EDIT') : auth.hasMenu('BTN_MENU_CREATE')" type="primary" @click="save">保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { Plus } from '@element-plus/icons-vue'
import { api } from '@/api/fm'
import { useAuthStore } from '@/stores/auth'
import type { MenuView, PermissionCode } from '@/types/api'

/**
 * MenuType 类型定义，用于约束页面状态、接口入参或接口返回数据结构。
 */
type MenuType = MenuView['type']

/**
 * MenuNode 类型定义，用于约束页面状态、接口入参或接口返回数据结构。
 */
interface MenuNode extends MenuView {
  /**
   * 字段 children：表示表单、筛选条件、接口数据或组件状态中的 children 值。
   */
  children?: MenuNode[]
}

/**
 * 常量 auth：保存当前模块的页面状态、配置项、接口实例或计算结果。
 */
const auth = useAuthStore()
/**
 * 常量 loading：保存当前模块的页面状态、配置项、接口实例或计算结果。
 */
const loading = ref(false)
/**
 * 常量 dialogVisible：保存当前模块的页面状态、配置项、接口实例或计算结果。
 */
const dialogVisible = ref(false)
/**
 * 常量 editing：保存当前模块的页面状态、配置项、接口实例或计算结果。
 */
const editing = ref(false)
/**
 * 常量 menus：保存当前模块的页面状态、配置项、接口实例或计算结果。
 */
const menus = ref<MenuView[]>([])
/**
 * 常量 menuTree：保存当前模块的页面状态、配置项、接口实例或计算结果。
 */
const menuTree = ref<MenuNode[]>([])
/**
 * 常量 editingId：保存当前模块的页面状态、配置项、接口实例或计算结果。
 */
const editingId = ref<number | null>(null)
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
   * 字段 type：表示表单、筛选条件、接口数据或组件状态中的 type 值。
   */
  type: undefined as MenuType | undefined,
  /**
   * 字段 routePath：表示表单、筛选条件、接口数据或组件状态中的 routePath 值。
   */
  routePath: '',
  /**
   * 字段 permissionCode：表示表单、筛选条件、接口数据或组件状态中的 permissionCode 值。
   */
  permissionCode: undefined as PermissionCode | undefined,
  /**
   * 字段 enabled：表示表单、筛选条件、接口数据或组件状态中的 enabled 值。
   */
  enabled: undefined as boolean | undefined
})

/**
 * 常量 menuTypeOptions：保存当前模块的页面状态、配置项、接口实例或计算结果。
 */
const menuTypeOptions = [
  { label: '模块', value: 'MODULE' },
  { label: '页面', value: 'PAGE' },
  { label: '按钮', value: 'BUTTON' }
]

/**
 * 常量 permissionOptions：保存当前模块的页面状态、配置项、接口实例或计算结果。
 */
const permissionOptions: PermissionCode[] = [
  'SYSTEM_USER_MANAGE',
  'SYSTEM_ROLE_MANAGE',
  'BASIC_DICT_MANAGE',
  'FINANCE_SUBJECT_MANAGE',
  'FINANCE_VOUCHER_MANAGE',
  'PURCHASE_MANAGE',
  'LOGISTICS_MANAGE',
  'INVENTORY_MANAGE',
  'AR_AP_MANAGE',
  'AI_ASSISTANT_USE',
  'REPORT_VIEW',
  'SEARCH_VIEW',
  'AUDIT_LOG_VIEW'
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
   * 字段 type：表示表单、筛选条件、接口数据或组件状态中的 type 值。
   */
  type: 'MODULE' as MenuType,
  /**
   * 字段 parentId：表示表单、筛选条件、接口数据或组件状态中的 parentId 值。
   */
  parentId: undefined as number | undefined,
  /**
   * 字段 routePath：表示表单、筛选条件、接口数据或组件状态中的 routePath 值。
   */
  routePath: '',
  /**
   * 字段 sortOrder：表示表单、筛选条件、接口数据或组件状态中的 sortOrder 值。
   */
  sortOrder: 0,
  /**
   * 字段 enabled：表示表单、筛选条件、接口数据或组件状态中的 enabled 值。
   */
  enabled: true,
  /**
   * 字段 permissionCode：表示表单、筛选条件、接口数据或组件状态中的 permissionCode 值。
   */
  permissionCode: undefined as PermissionCode | undefined
})

/**
 * 常量 parentOptions：保存当前模块的页面状态、配置项、接口实例或计算结果。
 */
const parentOptions = computed(() => {
  /**
   * 常量 requiredType：保存当前模块的页面状态、配置项、接口实例或计算结果。
   */
  const requiredType: MenuType = form.type === 'PAGE' ? 'MODULE' : 'PAGE'
  return menus.value.filter((item) => item.type === requiredType && item.id !== editingId.value)
})
/**
 * 常量 rootExpandedKeys：保存当前模块的页面状态、配置项、接口实例或计算结果。
 */
const rootExpandedKeys = computed(() => menuTree.value.map((item) => item.id))

/**
 * 执行 buildMenuTree 方法。
 * 
 * 实现步骤：
 * 1. 读取当前页面状态或调用参数；
 * 2. 完成对应的校验、接口调用或数据转换；
 * 3. 更新页面状态或返回处理结果。
 */
function buildMenuTree(rows: MenuView[]) {
  /**
   * 常量 nodeMap：保存当前模块的页面状态、配置项、接口实例或计算结果。
   */
  const nodeMap = new Map<number, MenuNode>()
  sortedRows(rows).forEach((item) => nodeMap.set(item.id, { ...item, children: [] }))
  /**
   * 常量 roots：保存当前模块的页面状态、配置项、接口实例或计算结果。
   */
  const roots: MenuNode[] = []
  nodeMap.forEach((node) => {
    if (node.parentId && nodeMap.has(node.parentId)) {
      nodeMap.get(node.parentId)?.children?.push(node)
    } else {
      roots.push(node)
    }
  })
  return sortMenuNodes(roots)
}

/**
 * 执行 sortedRows 方法。
 * 
 * 实现步骤：
 * 1. 读取当前页面状态或调用参数；
 * 2. 完成对应的校验、接口调用或数据转换；
 * 3. 更新页面状态或返回处理结果。
 */
function sortedRows(rows: MenuView[]) {
  return [...rows].sort(compareSortOrder)
}

/**
 * 执行 sortMenuNodes 方法。
 * 
 * 实现步骤：
 * 1. 读取当前页面状态或调用参数；
 * 2. 完成对应的校验、接口调用或数据转换；
 * 3. 更新页面状态或返回处理结果。
 */
function sortMenuNodes(nodes: MenuNode[]) {
  nodes.sort(compareSortOrder)
  nodes.forEach((node) => {
    if (node.children?.length) {
      sortMenuNodes(node.children)
    }
  })
  return nodes
}

/**
 * 执行 compareSortOrder 方法。
 * 
 * 实现步骤：
 * 1. 读取当前页面状态或调用参数；
 * 2. 完成对应的校验、接口调用或数据转换；
 * 3. 更新页面状态或返回处理结果。
 */
function compareSortOrder(left: Pick<MenuView, 'sortOrder' | 'id'>, right: Pick<MenuView, 'sortOrder' | 'id'>) {
  if (left.sortOrder !== right.sortOrder) {
    return left.sortOrder - right.sortOrder
  }
  return left.id - right.id
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
    menus.value = await api.allMenus(menuSearchParams())
    menuTree.value = buildMenuTree(menus.value)
  } finally {
    loading.value = false
  }
}

/**
 * 组装菜单字段级搜索参数。
 */
function menuSearchParams() {
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
     * 字段 type：表示表单、筛选条件、接口数据或组件状态中的 type 值。
     */
    type: filters.type,
    /**
     * 字段 routePath：表示表单、筛选条件、接口数据或组件状态中的 routePath 值。
     */
    routePath: filters.routePath.trim() || undefined,
    /**
     * 字段 permissionCode：表示表单、筛选条件、接口数据或组件状态中的 permissionCode 值。
     */
    permissionCode: filters.permissionCode,
    /**
     * 字段 enabled：表示表单、筛选条件、接口数据或组件状态中的 enabled 值。
     */
    enabled: filters.enabled
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
     * 字段 type：表示表单、筛选条件、接口数据或组件状态中的 type 值。
     */
    type: undefined,
    /**
     * 字段 routePath：表示表单、筛选条件、接口数据或组件状态中的 routePath 值。
     */
    routePath: '',
    /**
     * 字段 permissionCode：表示表单、筛选条件、接口数据或组件状态中的 permissionCode 值。
     */
    permissionCode: undefined,
    /**
     * 字段 enabled：表示表单、筛选条件、接口数据或组件状态中的 enabled 值。
     */
    enabled: undefined
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
  editing.value = false
  editingId.value = null
  Object.assign(form, {
    /**
     * 字段 code：表示表单、筛选条件、接口数据或组件状态中的 code 值。
     */
    code: '',
    /**
     * 字段 name：表示表单、筛选条件、接口数据或组件状态中的 name 值。
     */
    name: '',
    /**
     * 字段 type：表示表单、筛选条件、接口数据或组件状态中的 type 值。
     */
    type: 'MODULE',
    /**
     * 字段 parentId：表示表单、筛选条件、接口数据或组件状态中的 parentId 值。
     */
    parentId: undefined,
    /**
     * 字段 routePath：表示表单、筛选条件、接口数据或组件状态中的 routePath 值。
     */
    routePath: '',
    /**
     * 字段 sortOrder：表示表单、筛选条件、接口数据或组件状态中的 sortOrder 值。
     */
    sortOrder: 0,
    /**
     * 字段 enabled：表示表单、筛选条件、接口数据或组件状态中的 enabled 值。
     */
    enabled: true,
    /**
     * 字段 permissionCode：表示表单、筛选条件、接口数据或组件状态中的 permissionCode 值。
     */
    permissionCode: undefined
  })
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
function openEdit(row: MenuView) {
  editing.value = true
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
     * 字段 type：表示表单、筛选条件、接口数据或组件状态中的 type 值。
     */
    type: row.type,
    /**
     * 字段 parentId：表示表单、筛选条件、接口数据或组件状态中的 parentId 值。
     */
    parentId: row.parentId,
    /**
     * 字段 routePath：表示表单、筛选条件、接口数据或组件状态中的 routePath 值。
     */
    routePath: row.routePath || '',
    /**
     * 字段 sortOrder：表示表单、筛选条件、接口数据或组件状态中的 sortOrder 值。
     */
    sortOrder: row.sortOrder,
    /**
     * 字段 enabled：表示表单、筛选条件、接口数据或组件状态中的 enabled 值。
     */
    enabled: row.enabled,
    /**
     * 字段 permissionCode：表示表单、筛选条件、接口数据或组件状态中的 permissionCode 值。
     */
    permissionCode: row.permissionCode
  })
  dialogVisible.value = true
}

/**
 * 执行 onTypeChange 方法。
 * 
 * 实现步骤：
 * 1. 读取当前页面状态或调用参数；
 * 2. 完成对应的校验、接口调用或数据转换；
 * 3. 更新页面状态或返回处理结果。
 */
function onTypeChange() {
  form.parentId = undefined
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
  if (!form.code || !form.name || !form.type) {
    ElMessage.warning('请填写菜单编码、名称和类型')
    return
  }
  if (form.type !== 'MODULE' && !form.parentId) {
    ElMessage.warning(form.type === 'PAGE' ? '页面菜单必须选择模块父级' : '按钮菜单必须选择页面父级')
    return
  }
  await api.saveMenu({ ...form, parentId: form.type === 'MODULE' ? null : form.parentId })
  ElMessage.success('保存成功')
  dialogVisible.value = false
  await load()
  await auth.refreshMenuCodes()
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
  await api.deleteMenu(id)
  ElMessage.success('删除成功')
  await load()
  await auth.refreshMenuCodes()
}

/**
 * 执行 typeLabel 方法。
 * 
 * 实现步骤：
 * 1. 读取当前页面状态或调用参数；
 * 2. 完成对应的校验、接口调用或数据转换；
 * 3. 更新页面状态或返回处理结果。
 */
function typeLabel(type: MenuType) {
  return type === 'MODULE' ? '模块' : type === 'PAGE' ? '页面' : '按钮'
}

/**
 * 执行 typeTag 方法。
 * 
 * 实现步骤：
 * 1. 读取当前页面状态或调用参数；
 * 2. 完成对应的校验、接口调用或数据转换；
 * 3. 更新页面状态或返回处理结果。
 */
function typeTag(type: MenuType) {
  return type === 'MODULE' ? 'primary' : type === 'PAGE' ? 'success' : 'info'
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
