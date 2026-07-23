<template>
  <div class="page">
    <div class="page-header">
      <div>
        <h1 class="page-title">角色管理</h1>
        <p class="page-subtitle">配置角色和可访问模块权限。</p>
      </div>
    </div>

    <el-form class="filter-form" label-width="72px">
      <el-row :gutter="12">
        <el-col :xs="24" :sm="12" :md="10" :lg="8">
          <el-form-item label="关键词">
            <el-input v-model="keywordDraft" clearable placeholder="搜索角色编码、名称、说明、授权菜单" />
          </el-form-item>
        </el-col>
        <el-col :xs="24" :sm="12" :md="8" :lg="7">
          <el-form-item label=" " class="filter-actions">
            <el-button type="primary" @click="applySearch">查询</el-button>
            <el-button @click="resetSearch">重置</el-button>
            <el-button v-if="auth.hasMenu('BTN_ROLE_CREATE')" type="primary" :icon="Plus" @click="openCreate">新增角色</el-button>
          </el-form-item>
        </el-col>
      </el-row>
    </el-form>

    <div class="panel">
      <el-table v-loading="loading" :data="filteredRoles" stripe>
        <el-table-column prop="code" label="角色编码" min-width="130" />
        <el-table-column prop="name" label="角色名称" min-width="140" />
        <el-table-column prop="description" label="说明" min-width="220" />
        <el-table-column label="菜单授权" min-width="360">
          <template #default="{ row }">
            <el-tooltip :content="roleMenuText(row)" placement="top" effect="light" :show-after="300">
              <div class="menu-auth-summary">
                <el-tag v-for="menuCode in row.menuCodes" :key="menuCode" size="small" class="tag-gap">
                  {{ menuLabel(menuCode) }}
                </el-tag>
                <span v-if="!row.menuCodes.length" class="empty-text">未授权菜单</span>
              </div>
            </el-tooltip>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="170" fixed="right">
          <template #default="{ row }">
            <div class="table-actions">
              <el-button v-if="auth.hasMenu('BTN_ROLE_EDIT')" size="small" @click="openEdit(row)">编辑</el-button>
              <el-popconfirm title="确认删除该角色？" @confirm="remove(row.id)">
                <template #reference>
                  <el-button v-if="auth.hasMenu('BTN_ROLE_DELETE')" size="small" type="danger">删除</el-button>
                </template>
              </el-popconfirm>
            </div>
          </template>
        </el-table-column>
      </el-table>
    </div>

    <el-dialog v-model="dialogVisible" :title="editing ? '编辑角色' : '新增角色'" width="720px">
      <el-form ref="formRef" :model="form" :rules="rules" label-width="90px">
        <el-form-item label="角色编码" prop="code">
          <el-input v-model="form.code" />
        </el-form-item>
        <el-form-item label="角色名称" prop="name">
          <el-input v-model="form.name" />
        </el-form-item>
        <el-form-item label="说明" prop="description">
          <el-input v-model="form.description" type="textarea" :rows="3" :maxlength="fieldLimits.remark" show-word-limit />
        </el-form-item>
        <el-form-item label="菜单授权">
          <el-tree
            ref="menuTreeRef"
            class="menu-tree"
            node-key="code"
            show-checkbox
            check-strictly
            :data="menuTree"
            :props="{ label: 'name', children: 'children' }"
            @check="syncCheckedAncestors"
          />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button v-if="editing ? auth.hasMenu('BTN_ROLE_EDIT') : auth.hasMenu('BTN_ROLE_CREATE')" type="primary" @click="save">保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue'
import { ElMessage } from 'element-plus'
import type { FormInstance, FormRules } from 'element-plus'
import { Plus } from '@element-plus/icons-vue'
import { api } from '@/api/fm'
import { useAuthStore } from '@/stores/auth'
import type { MenuView, RoleView } from '@/types/api'
import { fieldLimits } from '@/utils/validators'

/**
 * 常量 auth：保存当前模块的页面状态、配置项、接口实例或计算结果。
 */
const auth = useAuthStore()
/**
 * 常量 loading：保存当前模块的页面状态、配置项、接口实例或计算结果。
 */
const loading = ref(false)
/**
 * 常量 roles：保存当前模块的页面状态、配置项、接口实例或计算结果。
 */
const roles = ref<RoleView[]>([])
/**
 * 常量 formRef：指向角色弹窗表单实例，用于字段级校验和红框提示。
 */
const formRef = ref<FormInstance>()
/**
 * 常量 menus：保存当前模块的页面状态、配置项、接口实例或计算结果。
 */
const menus = ref<MenuView[]>([])
/**
 * 常量 keyword：保存当前模块的页面状态、配置项、接口实例或计算结果。
 */
const keyword = ref('')
/**
 * 常量 keywordDraft：保存当前模块的页面状态、配置项、接口实例或计算结果。
 */
const keywordDraft = ref('')
/**
 * 常量 menuTree：保存当前模块的页面状态、配置项、接口实例或计算结果。
 */
const menuTree = ref<Array<MenuNode>>([])
/**
 * 常量 menuTreeRef：保存当前模块的页面状态、配置项、接口实例或计算结果。
 */
const menuTreeRef = ref()
/**
 * 常量 dialogVisible：保存当前模块的页面状态、配置项、接口实例或计算结果。
 */
const dialogVisible = ref(false)
/**
 * 常量 editing：保存当前模块的页面状态、配置项、接口实例或计算结果。
 */
const editing = ref(false)
/**
 * 常量 syncingTree：保存当前模块的页面状态、配置项、接口实例或计算结果。
 */
const syncingTree = ref(false)
/**
 * 常量 filteredRoles：保存当前模块的页面状态、配置项、接口实例或计算结果。
 */
const filteredRoles = computed(() => {
  /**
   * 常量 text：保存当前模块的页面状态、配置项、接口实例或计算结果。
   */
  const text = keyword.value.trim().toLowerCase()
  if (!text) {
    return roles.value
  }
  return roles.value.filter((role) =>
    [
      role.code,
      role.name,
      role.description,
      role.menuCodes.map((code) => menuLabel(code)).join(',')
    ]
      .filter(Boolean)
      .some((value) => String(value).toLowerCase().includes(text))
  )
})

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
   * 字段 description：表示表单、筛选条件、接口数据或组件状态中的 description 值。
   */
  description: '',
  /**
   * 字段 menuCodes：表示表单、筛选条件、接口数据或组件状态中的 menuCodes 值。
   */
  menuCodes: [] as string[]
})

/**
 * 角色弹窗字段校验规则。
 *
 * 实现步骤：角色编码、名称作为必填字段，说明按统一长文本限制校验，错误显示在对应字段下方。
 */
const rules: FormRules = {
  code: [{ required: true, message: '请输入角色编码', trigger: 'blur' }],
  name: [{ required: true, message: '请输入角色名称', trigger: 'blur' }],
  description: [{ max: fieldLimits.remark, message: `说明不能超过${fieldLimits.remark}个字符`, trigger: 'blur' }]
}

/**
 * 执行 menuLabel 方法。
 * 
 * 实现步骤：
 * 1. 读取当前页面状态或调用参数；
 * 2. 完成对应的校验、接口调用或数据转换；
 * 3. 更新页面状态或返回处理结果。
 */
function menuLabel(value: string) {
  return menus.value.find((item) => item.code === value)?.name || value
}

/**
 * 生成角色授权菜单完整文本，用于列表授权摘要悬浮查看。
 */
function roleMenuText(row: RoleView) {
  return row.menuCodes.map((code) => menuLabel(code)).join('、') || '未授权菜单'
}

/**
 * 执行 parentChainCodes 方法。
 * 
 * 实现步骤：
 * 1. 读取当前页面状态或调用参数；
 * 2. 完成对应的校验、接口调用或数据转换；
 * 3. 更新页面状态或返回处理结果。
 */
function parentChainCodes(menuCode: string) {
  /**
   * 常量 codes：保存当前模块的页面状态、配置项、接口实例或计算结果。
   */
  const codes: string[] = []
  /**
   * 变量 current：保存当前模块的页面状态、配置项、接口实例或计算结果。
   */
  let current = menus.value.find((item) => item.code === menuCode)
  while (current?.parentId) {
    /**
     * 常量 parent：保存当前模块的页面状态、配置项、接口实例或计算结果。
     */
    const parent = menus.value.find((item) => item.id === current?.parentId)
    if (!parent) {
      break
    }
    codes.push(parent.code)
    current = parent
  }
  return codes
}

/**
 * 执行 withAncestorCodes 方法。
 * 
 * 实现步骤：
 * 1. 读取当前页面状态或调用参数；
 * 2. 完成对应的校验、接口调用或数据转换；
 * 3. 更新页面状态或返回处理结果。
 */
function withAncestorCodes(menuCodes: string[]) {
  /**
   * 常量 merged：保存当前模块的页面状态、配置项、接口实例或计算结果。
   */
  const merged = new Set<string>()
  menuCodes.forEach((code) => {
    merged.add(code)
    parentChainCodes(code).forEach((parentCode) => merged.add(parentCode))
  })
  return [...merged]
}

/**
 * 执行 syncCheckedAncestors 方法。
 * 
 * 实现步骤：
 * 1. 读取当前页面状态或调用参数；
 * 2. 完成对应的校验、接口调用或数据转换；
 * 3. 更新页面状态或返回处理结果。
 */
function syncCheckedAncestors() {
  if (syncingTree.value) {
    return
  }
  /**
   * 常量 checkedKeys：保存当前模块的页面状态、配置项、接口实例或计算结果。
   */
  const checkedKeys = (menuTreeRef.value?.getCheckedKeys(false) || []) as string[]
  syncingTree.value = true
  menuTreeRef.value?.setCheckedKeys(withAncestorCodes(checkedKeys), false)
  setTimeout(() => {
    syncingTree.value = false
  })
}

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
  rows.forEach((item) => nodeMap.set(item.id, { ...item, children: [] }))
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
  return roots
}

/**
 * 应用角色搜索条件。
 *
 * 实现步骤：将输入框暂存值提交为正式关键词，列表计算属性按该关键词过滤角色。
 */
function applySearch() {
  keyword.value = keywordDraft.value
}

/**
 * 重置角色搜索条件。
 */
function resetSearch() {
  keywordDraft.value = ''
  keyword.value = ''
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
    const [roleRows, menuRows] = await Promise.all([api.roles(), api.menus()])
    roles.value = roleRows
    menus.value = menuRows
    menuTree.value = buildMenuTree(menuRows)
  } finally {
    loading.value = false
  }
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
  Object.assign(form, { code: '', name: '', description: '', menuCodes: [] })
  dialogVisible.value = true
  setTimeout(() => menuTreeRef.value?.setCheckedKeys([]))
}

/**
 * 执行 openEdit 方法。
 * 
 * 实现步骤：
 * 1. 读取当前页面状态或调用参数；
 * 2. 完成对应的校验、接口调用或数据转换；
 * 3. 更新页面状态或返回处理结果。
 */
function openEdit(row: RoleView) {
  editing.value = true
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
     * 字段 description：表示表单、筛选条件、接口数据或组件状态中的 description 值。
     */
    description: row.description || '',
    /**
     * 字段 menuCodes：表示表单、筛选条件、接口数据或组件状态中的 menuCodes 值。
     */
    menuCodes: [...row.menuCodes]
  })
  dialogVisible.value = true
  setTimeout(() => menuTreeRef.value?.setCheckedKeys(withAncestorCodes(row.menuCodes), false))
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
  /** 角色表单校验结果，失败时字段下方显示错误并阻止保存。 */
  const valid = await formRef.value?.validate().catch(() => false)
  if (!valid) {
    return
  }
  /**
   * 常量 checkedKeys：保存当前模块的页面状态、配置项、接口实例或计算结果。
   */
  const checkedKeys = menuTreeRef.value?.getCheckedKeys(false) || []
  await api.saveRole({ ...form, menuCodes: withAncestorCodes(checkedKeys) })
  ElMessage.success('保存成功')
  dialogVisible.value = false
  await load()
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
  await api.deleteRole(id)
  ElMessage.success('删除成功')
  await load()
}

onMounted(load)
</script>

<style scoped>
.tag-gap {
  margin-right: 6px;
  margin-bottom: 4px;
}

.menu-auth-summary {
  display: -webkit-box;
  max-height: 84px;
  overflow: hidden;
  -webkit-box-orient: vertical;
  -webkit-line-clamp: 3;
  line-height: 28px;
}

.empty-text {
  color: var(--muted-text-color);
  font-size: 13px;
}

.menu-tree {
  width: 100%;
  max-height: 420px;
  overflow: auto;
  border: 1px solid var(--border-color);
  border-radius: 6px;
  background: var(--surface-color);
  color: var(--text-color);
  padding: 8px;
}

.filter-form {
  margin-bottom: 14px;
  padding: 14px 14px 0;
  border: 1px solid var(--border-color);
  border-radius: 6px;
  background: var(--surface-color);
}
</style>
