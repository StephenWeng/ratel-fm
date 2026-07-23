<template>
  <div class="page">
    <div class="page-header">
      <div>
        <h1 class="page-title">人员管理</h1>
        <p class="page-subtitle">维护系统登录人员和角色分配。</p>
      </div>
    </div>

    <el-form class="filter-form" :model="filters" label-width="72px">
      <el-row :gutter="12">
        <el-col v-if="isAdmin" :xs="24" :sm="12" :md="8" :lg="6">
          <el-form-item label="所属公司">
            <el-select v-model="filters.organizationCode" clearable filterable class="full" placeholder="精确选择所属公司">
              <el-option v-for="item in organizationOptions" :key="item.value" :label="item.label" :value="item.value" />
            </el-select>
          </el-form-item>
        </el-col>
        <el-col :xs="24" :sm="12" :md="8" :lg="6">
          <el-form-item label="账号">
            <el-input v-model="filters.username" clearable placeholder="模糊查询账号" />
          </el-form-item>
        </el-col>
        <el-col :xs="24" :sm="12" :md="8" :lg="6">
          <el-form-item label="姓名">
            <el-input v-model="filters.realName" clearable placeholder="模糊查询姓名" />
          </el-form-item>
        </el-col>
        <el-col :xs="24" :sm="12" :md="8" :lg="6">
          <el-form-item label="身份证">
            <el-input v-model="filters.identityNo" clearable placeholder="模糊查询身份证" />
          </el-form-item>
        </el-col>
        <el-col :xs="24" :sm="12" :md="8" :lg="6">
          <el-form-item label="联系方式">
            <el-input v-model="filters.phone" clearable placeholder="模糊查询联系方式" />
          </el-form-item>
        </el-col>
        <el-col :xs="24" :sm="12" :md="8" :lg="6">
          <el-form-item label="部门">
            <el-select v-model="filters.department" clearable filterable class="full" placeholder="精确选择部门">
              <el-option v-for="item in departmentOptions" :key="item.value" :label="item.label" :value="item.value" />
            </el-select>
          </el-form-item>
        </el-col>
        <el-col :xs="24" :sm="12" :md="8" :lg="6">
          <el-form-item label="岗位">
            <el-select v-model="filters.position" clearable filterable class="full" placeholder="精确选择岗位">
              <el-option v-for="item in positionOptions" :key="item.value" :label="item.label" :value="item.value" />
            </el-select>
          </el-form-item>
        </el-col>
        <el-col :xs="24" :sm="12" :md="8" :lg="6">
          <el-form-item label="状态">
            <el-select v-model="filters.enabled" clearable class="full" placeholder="全部">
              <el-option label="启用" :value="true" />
              <el-option label="停用" :value="false" />
            </el-select>
          </el-form-item>
        </el-col>
        <el-col :xs="24" :sm="12" :md="8" :lg="6">
          <el-form-item label="邮箱">
            <el-input v-model="filters.email" clearable placeholder="模糊查询邮箱" />
          </el-form-item>
        </el-col>
        <el-col :xs="24" :sm="12" :md="8" :lg="6">
          <el-form-item label=" " class="filter-actions">
            <el-button type="primary" @click="load">查询</el-button>
            <el-button @click="resetFilters">重置</el-button>
            <el-button v-if="auth.hasMenu('BTN_USER_CREATE')" type="primary" :icon="Plus" @click="openCreate">新增人员</el-button>
            <el-button v-if="auth.hasMenu('BTN_USER_BATCH_DELETE') && selectedRows.length > 0" type="danger" :icon="Delete" @click="batchRemove">批量删除</el-button>
          </el-form-item>
        </el-col>
      </el-row>
    </el-form>

    <div class="panel">
      <el-table v-loading="loading" :data="users" stripe @selection-change="handleSelectionChange">
        <el-table-column type="selection" width="48" />
        <el-table-column v-if="isAdmin" prop="organizationCode" label="所属公司" min-width="130">
          <template #default="{ row }">{{ organizationLabel(row.organizationCode) }}</template>
        </el-table-column>
        <el-table-column prop="username" label="账号" min-width="120" />
        <el-table-column label="头像" width="74">
          <template #default="{ row }">
            <el-avatar :size="36" :src="row.avatarUrl">{{ (row.realName || row.username).slice(0, 1) }}</el-avatar>
          </template>
        </el-table-column>
        <el-table-column prop="realName" label="姓名" min-width="120" />
        <el-table-column prop="department" label="部门" min-width="120" />
        <el-table-column prop="position" label="岗位" min-width="120" />
        <el-table-column prop="identityNo" label="身份证" min-width="160" />
        <el-table-column label="角色" min-width="220">
          <template #default="{ row }">
            <el-tag v-for="role in row.roles" :key="role.code" size="small" class="tag-gap">{{ role.name }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="状态" width="90">
          <template #default="{ row }">
            <el-tag :type="row.enabled ? 'success' : 'info'">{{ row.enabled ? '启用' : '停用' }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="270" fixed="right">
          <template #default="{ row }">
            <div class="table-actions">
              <el-button v-if="auth.hasMenu('BTN_USER_EDIT')" size="small" @click="openEdit(row)">编辑</el-button>
              <el-button v-if="auth.hasMenu('BTN_USER_PASSWORD')" size="small" @click="openPassword(row)">密码</el-button>
              <el-button
                v-if="auth.hasMenu('BTN_USER_AVATAR')"
                size="small"
                @click="openAvatar(row)"
              >
                头像
              </el-button>
              <el-popconfirm title="确认删除该人员？" @confirm="remove(row.id)">
                <template #reference>
                  <el-button v-if="auth.hasMenu('BTN_USER_DELETE')" size="small" type="danger">删除</el-button>
                </template>
              </el-popconfirm>
            </div>
          </template>
        </el-table-column>
      </el-table>
    </div>

    <el-dialog v-model="dialogVisible" :title="editingId ? '编辑人员' : '新增人员'" width="620px">
      <el-form ref="formRef" :model="form" :rules="rules" label-width="88px">
        <el-form-item label="所属公司" prop="organizationCode">
          <el-select v-model="form.organizationCode" clearable filterable class="full" placeholder="请选择所属公司" :disabled="!isAdmin">
            <el-option v-for="item in organizationOptions" :key="item.value" :label="item.label" :value="item.value" />
          </el-select>
        </el-form-item>
        <el-form-item label="账号" prop="username">
          <el-input v-model="form.username" :disabled="Boolean(editingId)" placeholder="账号在同一所属公司内唯一" />
        </el-form-item>
        <el-form-item label="姓名" prop="realName">
          <el-input v-model="form.realName" :maxlength="fieldLimits.chineseName" show-word-limit placeholder="请输入中文姓名" />
        </el-form-item>
        <el-form-item label="密码" prop="password">
          <el-input v-model="form.password" type="password" show-password />
        </el-form-item>
        <el-form-item label="部门" prop="department">
          <el-select v-model="form.department" clearable filterable class="full" placeholder="请选择部门">
            <el-option v-for="item in departmentOptions" :key="item.value" :label="item.label" :value="item.value" />
          </el-select>
        </el-form-item>
        <el-form-item label="岗位" prop="position">
          <el-select v-model="form.position" clearable filterable class="full" placeholder="请选择岗位">
            <el-option v-for="item in positionOptions" :key="item.value" :label="item.label" :value="item.value" />
          </el-select>
        </el-form-item>
        <el-form-item label="身份证" prop="identityNo">
          <el-input v-model="form.identityNo" maxlength="18" placeholder="请输入18位身份证号" />
        </el-form-item>
        <el-form-item label="电话" prop="phone">
          <el-input v-model="form.phone" maxlength="30" placeholder="手机号或座机号" />
        </el-form-item>
        <el-form-item label="邮箱" prop="email">
          <el-input v-model="form.email" />
        </el-form-item>
        <el-form-item label="角色">
          <el-select v-model="form.roleCodes" multiple class="full">
            <el-option v-for="role in roles" :key="role.code" :label="role.name" :value="role.code" />
          </el-select>
        </el-form-item>
        <el-form-item label="启用">
          <el-switch v-model="form.enabled" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button v-if="canSaveUser" type="primary" @click="save">保存</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="passwordVisible" title="修改人员密码" width="460px">
      <el-form ref="passwordFormRef" :model="passwordForm" :rules="passwordRules" label-width="86px">
        <el-form-item label="新密码" prop="newPassword">
          <el-input v-model="passwordForm.newPassword" type="password" show-password />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="passwordVisible = false">取消</el-button>
        <el-button v-if="auth.hasMenu('BTN_USER_PASSWORD')" type="primary" @click="savePassword">保存</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="avatarVisible" title="维护人员头像" width="420px" @closed="resetAvatarDialog">
      <div class="avatar-dialog-body">
        <el-avatar :size="96" :src="avatarPreview || avatarTarget?.avatarUrl">{{ avatarInitial }}</el-avatar>
        <el-upload
          :auto-upload="false"
          :show-file-list="false"
          :on-change="selectAvatarFile"
          accept=".jpg,.jpeg,.png,.webp"
        >
          <el-button>选择图片</el-button>
        </el-upload>
      </div>
      <template #footer>
        <el-button @click="avatarVisible = false">取消</el-button>
        <el-button
          v-if="auth.hasMenu('BTN_USER_AVATAR')"
          type="primary"
          :disabled="!avatarFile"
          @click="confirmAvatar"
        >
          确认绑定
        </el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { computed, onActivated, onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox, type FormInstance, type FormRules, type UploadFile, type UploadRawFile } from 'element-plus'
import { Delete, Plus } from '@element-plus/icons-vue'
import { api } from '@/api/fm'
import { useAuthStore } from '@/stores/auth'
import type { BasicDictionaryView, RoleView, UserView } from '@/types/api'
import { fieldLimits, validateAvatarImage, validateChineseName, validateContactPhone, validateIdentityNo } from '@/utils/validators'
import { flattenDictionaryOptions } from '@/utils/dictionaries'

/**
 * 常量 auth：保存当前模块的页面状态、配置项、接口实例或计算结果。
 */
const auth = useAuthStore()
/**
 * 常量 loading：保存当前模块的页面状态、配置项、接口实例或计算结果。
 */
const loading = ref(false)
/**
 * 常量 users：保存当前模块的页面状态、配置项、接口实例或计算结果。
 */
const users = ref<UserView[]>([])
/**
 * 常量 selectedRows：保存当前模块的页面状态、配置项、接口实例或计算结果。
 */
const selectedRows = ref<UserView[]>([])
/**
 * 常量 roles：保存当前模块的页面状态、配置项、接口实例或计算结果。
 */
const roles = ref<RoleView[]>([])
/**
 * 常量 departmentTree：保存当前模块的页面状态、配置项、接口实例或计算结果。
 */
const departmentTree = ref<BasicDictionaryView[]>([])
/**
 * 常量 organizationTree：保存当前模块的页面状态、配置项、接口实例或计算结果。
 */
const organizationTree = ref<BasicDictionaryView[]>([])
/**
 * 常量 positionTree：保存当前模块的页面状态、配置项、接口实例或计算结果。
 */
const positionTree = ref<BasicDictionaryView[]>([])
/**
 * 常量 dialogVisible：保存当前模块的页面状态、配置项、接口实例或计算结果。
 */
const dialogVisible = ref(false)
/**
 * 常量 passwordVisible：保存当前模块的页面状态、配置项、接口实例或计算结果。
 */
const passwordVisible = ref(false)
/**
 * 常量 avatarVisible：保存当前模块的页面状态、配置项、接口实例或计算结果。
 */
const avatarVisible = ref(false)
/**
 * 常量 editingId：保存当前模块的页面状态、配置项、接口实例或计算结果。
 */
const editingId = ref<number>()
/**
 * 常量 passwordUserId：保存当前模块的页面状态、配置项、接口实例或计算结果。
 */
const passwordUserId = ref<number>()
/**
 * 常量 avatarTarget：保存当前模块的页面状态、配置项、接口实例或计算结果。
 */
const avatarTarget = ref<UserView>()
/**
 * 常量 avatarFile：保存当前模块的页面状态、配置项、接口实例或计算结果。
 */
const avatarFile = ref<UploadRawFile>()
/**
 * 常量 avatarPreview：保存当前模块的页面状态、配置项、接口实例或计算结果。
 */
const avatarPreview = ref('')
/**
 * 常量 formRef：保存当前模块的页面状态、配置项、接口实例或计算结果。
 */
const formRef = ref<FormInstance>()
/**
 * 常量 passwordFormRef：保存当前模块的页面状态、配置项、接口实例或计算结果。
 */
const passwordFormRef = ref<FormInstance>()
/**
 * 常量 filters：保存当前模块的页面状态、配置项、接口实例或计算结果。
 */
const filters = reactive({
  /**
   * 字段 username：表示表单、筛选条件、接口数据或组件状态中的 username 值。
   */
  username: '',
  /**
   * 字段 realName：表示表单、筛选条件、接口数据或组件状态中的 realName 值。
   */
  realName: '',
  /**
   * 字段 identityNo：表示表单、筛选条件、接口数据或组件状态中的 identityNo 值。
   */
  identityNo: '',
  /**
   * 字段 phone：表示表单、筛选条件、接口数据或组件状态中的 phone 值。
   */
  phone: '',
  /**
   * 字段 email：表示表单、筛选条件、接口数据或组件状态中的 email 值。
   */
  email: '',
  /**
   * 字段 department：表示表单、筛选条件、接口数据或组件状态中的 department 值。
   */
  department: '',
  /**
   * 字段 organizationCode：表示表单、筛选条件、接口数据或组件状态中的 organizationCode 值。
   */
  organizationCode: '',
  /**
   * 字段 position：表示表单、筛选条件、接口数据或组件状态中的 position 值。
   */
  position: '',
  /**
   * 字段 enabled：表示表单、筛选条件、接口数据或组件状态中的 enabled 值。
   */
  enabled: undefined as boolean | undefined
})

/**
 * 常量 form：保存当前模块的页面状态、配置项、接口实例或计算结果。
 */
const form = reactive({
  /**
   * 字段 username：表示表单、筛选条件、接口数据或组件状态中的 username 值。
   */
  username: '',
  /**
   * 字段 realName：表示表单、筛选条件、接口数据或组件状态中的 realName 值。
   */
  realName: '',
  /**
   * 字段 password：表示表单、筛选条件、接口数据或组件状态中的 password 值。
   */
  password: '',
  /**
   * 字段 department：表示表单、筛选条件、接口数据或组件状态中的 department 值。
   */
  department: '',
  /**
   * 字段 organizationCode：表示表单、筛选条件、接口数据或组件状态中的 organizationCode 值。
   */
  organizationCode: '',
  /**
   * 字段 position：表示表单、筛选条件、接口数据或组件状态中的 position 值。
   */
  position: '',
  /**
   * 字段 identityNo：表示表单、筛选条件、接口数据或组件状态中的 identityNo 值。
   */
  identityNo: '',
  /**
   * 字段 phone：表示表单、筛选条件、接口数据或组件状态中的 phone 值。
   */
  phone: '',
  /**
   * 字段 email：表示表单、筛选条件、接口数据或组件状态中的 email 值。
   */
  email: '',
  /**
   * 字段 enabled：表示表单、筛选条件、接口数据或组件状态中的 enabled 值。
   */
  enabled: true,
  /**
   * 字段 roleCodes：表示表单、筛选条件、接口数据或组件状态中的 roleCodes 值。
   */
  roleCodes: [] as string[]
})
/**
 * 常量 passwordForm：保存当前模块的页面状态、配置项、接口实例或计算结果。
 */
const passwordForm = reactive({
  /**
   * 字段 newPassword：表示表单、筛选条件、接口数据或组件状态中的 newPassword 值。
   */
  newPassword: ''
})
/**
 * 常量 canSaveUser：保存当前模块的页面状态、配置项、接口实例或计算结果。
 */
const canSaveUser = computed(() => editingId.value ? auth.hasMenu('BTN_USER_EDIT') : auth.hasMenu('BTN_USER_CREATE'))
/** 当前登录人是否默认管理员，只有 admin 可跨所属公司维护人员。 */
const isAdmin = computed(() => auth.user?.username === 'admin')
/**
 * 常量 avatarInitial：保存当前模块的页面状态、配置项、接口实例或计算结果。
 */
const avatarInitial = computed(() => (avatarTarget.value?.realName || avatarTarget.value?.username || 'U').slice(0, 1))
/**
 * 常量 departmentOptions：保存当前模块的页面状态、配置项、接口实例或计算结果。
 */
const departmentOptions = computed(() => flattenDictionaryOptions(departmentTree.value))
/**
 * 常量 organizationOptions：保存当前模块的页面状态、配置项、接口实例或计算结果。
 */
const organizationOptions = computed(() => flattenDictionaryCodeOptions(organizationTree.value))
/**
 * 常量 positionOptions：保存当前模块的页面状态、配置项、接口实例或计算结果。
 */
const positionOptions = computed(() => flattenDictionaryOptions(positionTree.value))

/**
 * 读取当前登录人的所属公司编码。
 *
 * 实现步骤：
 * 1. 优先使用当前登录人 JWT/用户信息中的 organizationCode；
 * 2. 空值时回退系统预置公司编码；
 * 3. 非 admin 新增和编辑人员时统一使用该值，前端不允许手工改公司。
 */
function currentUserCompanyCode() {
  return auth.user?.organizationCode || 'ORGANIZATION_RATEL'
}

/**
 * 将所属公司字典树转换为编码下拉选项。
 *
 * 实现步骤：
 * 1. 递归遍历 ORGANIZATION 启用字典树；
 * 2. label 展示字典名称并保留层级缩进；
 * 3. value 使用字典编码，保证人员所属公司字段和登录账套隔离标识一致。
 */
function flattenDictionaryCodeOptions(nodes: BasicDictionaryView[], level = 0) {
  const result: { label: string; value: string }[] = []
  nodes.forEach((node) => {
    result.push({ label: `${'　'.repeat(level)}${node.name}`, value: node.code })
    result.push(...flattenDictionaryCodeOptions(node.children || [], level + 1))
  })
  return result
}

/**
 * 将所属公司编码转换为页面展示名称。
 *
 * 实现步骤：在所属公司下拉选项中按编码查找，找到时展示名称，找不到时保留原编码便于排查历史数据。</p>
 */
function organizationLabel(code?: string) {
  if (!code) {
    return ''
  }
  return organizationOptions.value.find((item) => item.value === code)?.label.trim() || code
}

/**
 * 人员维护表单规则。
 *
 * 开发组织：ratel；开发人员：WenZhang；联系方式：18782945613。
 *
 * 实现步骤：账号、姓名、身份证做必填和格式校验；密码新增必填、编辑可空；联系方式、邮箱和文本长度在前端先拦截。
 */
const rules = computed<FormRules>(() => ({
  /**
   * 字段 username：表示表单、筛选条件、接口数据或组件状态中的 username 值。
   */
  username: [
    { required: !editingId.value, message: '请输入账号', trigger: 'blur' },
    { min: 3, max: 80, message: '账号长度必须在3到80个字符之间', trigger: 'blur' },
    { pattern: /^[A-Za-z0-9_.@-]+$/, message: '账号只能包含字母、数字、下划线、横线、点和@', trigger: 'blur' }
  ],
  /**
   * 字段 realName：表示表单、筛选条件、接口数据或组件状态中的 realName 值。
   */
  realName: [
    { required: true, message: '请输入姓名', trigger: 'blur' },
    { validator: validateChineseName, trigger: 'blur' }
  ],
  /**
   * 字段 password：表示表单、筛选条件、接口数据或组件状态中的 password 值。
   */
  password: [
    { required: !editingId.value, message: '请输入密码', trigger: 'blur' },
    { pattern: /^$|^.{6,72}$/, message: '密码长度必须在6到72个字符之间', trigger: 'blur' }
  ],
  /**
   * 字段 department：表示表单、筛选条件、接口数据或组件状态中的 department 值。
   */
  department: [{ max: 80, message: '部门长度不能超过80个字符', trigger: 'blur' }],
  /**
   * 字段 organizationCode：表示表单、筛选条件、接口数据或组件状态中的 organizationCode 值。
   */
  organizationCode: [{ max: 80, message: '所属公司编码长度不能超过80个字符', trigger: 'blur' }],
  /**
   * 字段 position：表示表单、筛选条件、接口数据或组件状态中的 position 值。
   */
  position: [{ max: 80, message: '岗位长度不能超过80个字符', trigger: 'blur' }],
  /**
   * 字段 identityNo：表示表单、筛选条件、接口数据或组件状态中的 identityNo 值。
   */
  identityNo: [
    { required: true, message: '请输入身份证号', trigger: 'blur' },
    { validator: validateIdentityNo, trigger: 'blur' }
  ],
  /**
   * 字段 phone：表示表单、筛选条件、接口数据或组件状态中的 phone 值。
   */
  phone: [{ validator: validateContactPhone, trigger: 'blur' }],
  /**
   * 字段 email：表示表单、筛选条件、接口数据或组件状态中的 email 值。
   */
  email: [
    { type: 'email', message: '邮箱格式不正确', trigger: 'blur' },
    { max: 120, message: '邮箱长度不能超过120个字符', trigger: 'blur' }
  ]
}))
/**
 * 常量 passwordRules：保存当前模块的页面状态、配置项、接口实例或计算结果。
 */
const passwordRules: FormRules = {
  /**
   * 字段 newPassword：表示表单、筛选条件、接口数据或组件状态中的 newPassword 值。
   */
  newPassword: [
    { required: true, message: '请输入新密码', trigger: 'blur' },
    { min: 6, max: 72, message: '新密码长度必须在6到72个字符之间', trigger: 'blur' }
  ]
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
    const userRows = await api.users(userSearchParams())
    users.value = userRows
    selectedRows.value = []
  } finally {
    loading.value = false
  }
}

/**
 * 重新读取人员管理依赖的角色和基础字典。
 *
 * 实现步骤：
 * 1. 并行请求角色、部门、所属公司和岗位；
 * 2. 只在初始化、页面重新激活、打开人员弹窗时调用，不在每次查询列表时重复拉取；
 * 3. GET 请求会自动追加防缓存参数，保证基础信息修改后下拉立即生效。
 */
async function refreshDictionaryOptions() {
  const [roleRows, departments, organizations, positions] = await Promise.all([
    api.roles(),
    api.enabledDictionaryTree('DEPARTMENT'),
    api.enabledDictionaryTree('ORGANIZATION'),
    api.enabledDictionaryTree('POSITION')
  ])
  roles.value = roleRows
  departmentTree.value = departments
  organizationTree.value = organizations
  positionTree.value = positions
}

/**
 * 执行 handleSelectionChange 方法。
 * 
 * 实现步骤：
 * 1. 读取当前页面状态或调用参数；
 * 2. 完成对应的校验、接口调用或数据转换；
 * 3. 更新页面状态或返回处理结果。
 */
function handleSelectionChange(selection: UserView[]) {
  selectedRows.value = selection
}

/**
 * 组装人员字段级搜索参数。
 *
 * 实现步骤：输入框字段传给后端做包含匹配；部门、组织、岗位、状态按确定值等值匹配。
 */
function userSearchParams() {
  return {
    /**
     * 字段 username：表示表单、筛选条件、接口数据或组件状态中的 username 值。
     */
    username: filters.username.trim() || undefined,
    /**
     * 字段 realName：表示表单、筛选条件、接口数据或组件状态中的 realName 值。
     */
    realName: filters.realName.trim() || undefined,
    /**
     * 字段 identityNo：表示表单、筛选条件、接口数据或组件状态中的 identityNo 值。
     */
    identityNo: filters.identityNo.trim() || undefined,
    /**
     * 字段 phone：表示表单、筛选条件、接口数据或组件状态中的 phone 值。
     */
    phone: filters.phone.trim() || undefined,
    /**
     * 字段 email：表示表单、筛选条件、接口数据或组件状态中的 email 值。
     */
    email: filters.email.trim() || undefined,
    /**
     * 字段 department：表示表单、筛选条件、接口数据或组件状态中的 department 值。
     */
    department: filters.department || undefined,
    /**
     * 字段 organizationCode：表示表单、筛选条件、接口数据或组件状态中的 organizationCode 值。
     */
    organizationCode: isAdmin.value ? (filters.organizationCode || undefined) : undefined,
    /**
     * 字段 position：表示表单、筛选条件、接口数据或组件状态中的 position 值。
     */
    position: filters.position || undefined,
    /**
     * 字段 enabled：表示表单、筛选条件、接口数据或组件状态中的 enabled 值。
     */
    enabled: filters.enabled
  }
}

/**
 * 重置人员搜索条件。
 */
function resetFilters() {
  Object.assign(filters, {
    /**
     * 字段 username：表示表单、筛选条件、接口数据或组件状态中的 username 值。
     */
    username: '',
    /**
     * 字段 realName：表示表单、筛选条件、接口数据或组件状态中的 realName 值。
     */
    realName: '',
    /**
     * 字段 identityNo：表示表单、筛选条件、接口数据或组件状态中的 identityNo 值。
     */
    identityNo: '',
    /**
     * 字段 phone：表示表单、筛选条件、接口数据或组件状态中的 phone 值。
     */
    phone: '',
    /**
     * 字段 email：表示表单、筛选条件、接口数据或组件状态中的 email 值。
     */
    email: '',
    /**
     * 字段 department：表示表单、筛选条件、接口数据或组件状态中的 department 值。
     */
    department: '',
    /**
     * 字段 organizationCode：表示表单、筛选条件、接口数据或组件状态中的 organizationCode 值。
     */
    organizationCode: '',
    /**
     * 字段 position：表示表单、筛选条件、接口数据或组件状态中的 position 值。
     */
    position: '',
    /**
     * 字段 enabled：表示表单、筛选条件、接口数据或组件状态中的 enabled 值。
     */
    enabled: undefined
  })
  void load()
}

/**
 * 执行 resetForm 方法。
 * 
 * 实现步骤：
 * 1. 读取当前页面状态或调用参数；
 * 2. 完成对应的校验、接口调用或数据转换；
 * 3. 更新页面状态或返回处理结果。
 */
function resetForm() {
  Object.assign(form, {
    /**
     * 字段 username：表示表单、筛选条件、接口数据或组件状态中的 username 值。
     */
    username: '',
    /**
     * 字段 realName：表示表单、筛选条件、接口数据或组件状态中的 realName 值。
     */
    realName: '',
    /**
     * 字段 password：表示表单、筛选条件、接口数据或组件状态中的 password 值。
     */
    password: '',
    /**
     * 字段 department：表示表单、筛选条件、接口数据或组件状态中的 department 值。
     */
    department: '',
    /**
     * 字段 organizationCode：表示表单、筛选条件、接口数据或组件状态中的 organizationCode 值。
     */
    organizationCode: currentUserCompanyCode(),
    /**
     * 字段 position：表示表单、筛选条件、接口数据或组件状态中的 position 值。
     */
    position: '',
    /**
     * 字段 identityNo：表示表单、筛选条件、接口数据或组件状态中的 identityNo 值。
     */
    identityNo: '',
    /**
     * 字段 phone：表示表单、筛选条件、接口数据或组件状态中的 phone 值。
     */
    phone: '',
    /**
     * 字段 email：表示表单、筛选条件、接口数据或组件状态中的 email 值。
     */
    email: '',
    /**
     * 字段 enabled：表示表单、筛选条件、接口数据或组件状态中的 enabled 值。
     */
    enabled: true,
    /**
     * 字段 roleCodes：表示表单、筛选条件、接口数据或组件状态中的 roleCodes 值。
     */
    roleCodes: []
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
  resetForm()
  form.organizationCode = currentUserCompanyCode()
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
async function openEdit(row: UserView) {
  await refreshDictionaryOptions()
  editingId.value = row.id
  Object.assign(form, {
    /**
     * 字段 username：表示表单、筛选条件、接口数据或组件状态中的 username 值。
     */
    username: row.username,
    /**
     * 字段 realName：表示表单、筛选条件、接口数据或组件状态中的 realName 值。
     */
    realName: row.realName,
    /**
     * 字段 password：表示表单、筛选条件、接口数据或组件状态中的 password 值。
     */
    password: '',
    /**
     * 字段 department：表示表单、筛选条件、接口数据或组件状态中的 department 值。
     */
    department: row.department || '',
    /**
     * 字段 organizationCode：表示表单、筛选条件、接口数据或组件状态中的 organizationCode 值。
     */
    organizationCode: isAdmin.value ? (row.organizationCode || '') : currentUserCompanyCode(),
    /**
     * 字段 position：表示表单、筛选条件、接口数据或组件状态中的 position 值。
     */
    position: row.position || '',
    /**
     * 字段 identityNo：表示表单、筛选条件、接口数据或组件状态中的 identityNo 值。
     */
    identityNo: row.identityNo || '',
    /**
     * 字段 phone：表示表单、筛选条件、接口数据或组件状态中的 phone 值。
     */
    phone: row.phone || '',
    /**
     * 字段 email：表示表单、筛选条件、接口数据或组件状态中的 email 值。
     */
    email: row.email || '',
    /**
     * 字段 enabled：表示表单、筛选条件、接口数据或组件状态中的 enabled 值。
     */
    enabled: row.enabled,
    /**
     * 字段 roleCodes：表示表单、筛选条件、接口数据或组件状态中的 roleCodes 值。
     */
    roleCodes: row.roles.map((role) => role.code)
  })
  dialogVisible.value = true
}

/**
 * 执行 openPassword 方法。
 * 
 * 实现步骤：
 * 1. 读取当前页面状态或调用参数；
 * 2. 完成对应的校验、接口调用或数据转换；
 * 3. 更新页面状态或返回处理结果。
 */
function openPassword(row: UserView) {
  passwordUserId.value = row.id
  passwordForm.newPassword = ''
  passwordVisible.value = true
}

/**
 * 执行 openAvatar 方法。
 * 
 * 实现步骤：
 * 1. 读取当前页面状态或调用参数；
 * 2. 完成对应的校验、接口调用或数据转换；
 * 3. 更新页面状态或返回处理结果。
 */
function openAvatar(row: UserView) {
  avatarTarget.value = row
  avatarFile.value = undefined
  avatarPreview.value = ''
  avatarVisible.value = true
}

/**
 * 执行 savePassword 方法。
 * 
 * 实现步骤：
 * 1. 读取当前页面状态或调用参数；
 * 2. 完成对应的校验、接口调用或数据转换；
 * 3. 更新页面状态或返回处理结果。
 */
async function savePassword() {
  /**
   * 常量 valid：保存当前模块的页面状态、配置项、接口实例或计算结果。
   */
  const valid = await passwordFormRef.value?.validate().catch(() => false)
  if (!passwordUserId.value || !valid) {
    return
  }
  await api.changeUserPassword(passwordUserId.value, { newPassword: passwordForm.newPassword })
  ElMessage.success('密码已修改')
  passwordVisible.value = false
}

/**
 * 选择人员头像文件。
 *
 * 开发组织：ratel；开发人员：WenZhang；联系方式：18782945613。
 *
 * 实现步骤：
 * 1. 只在前端暂存用户选择的图片文件，不立即请求后端；
 * 2. 校验图片类型和大小；
 * 3. 使用 FileReader 生成本地预览，等待用户点击确认后再和人员绑定。
 */
function selectAvatarFile(uploadFile: UploadFile) {
  /**
   * 常量 rawFile：保存当前模块的页面状态、配置项、接口实例或计算结果。
   */
  const rawFile = uploadFile.raw
  if (!rawFile) {
    return
  }
  /**
   * 常量 message：保存当前模块的页面状态、配置项、接口实例或计算结果。
   */
  const message = validateAvatarImage(rawFile)
  if (message) {
    ElMessage.warning(message)
    avatarFile.value = undefined
    avatarPreview.value = ''
    return
  }
  avatarFile.value = rawFile
  /**
   * 常量 reader：保存当前模块的页面状态、配置项、接口实例或计算结果。
   */
  const reader = new FileReader()
  reader.onload = () => {
    avatarPreview.value = String(reader.result || '')
  }
  reader.readAsDataURL(rawFile)
}

/**
 * 执行 confirmAvatar 方法。
 * 
 * 实现步骤：
 * 1. 读取当前页面状态或调用参数；
 * 2. 完成对应的校验、接口调用或数据转换；
 * 3. 更新页面状态或返回处理结果。
 */
async function confirmAvatar() {
  if (!avatarTarget.value || !avatarFile.value) {
    ElMessage.warning('请先选择头像图片')
    return
  }
  /**
   * 常量 formData：保存当前模块的页面状态、配置项、接口实例或计算结果。
   */
  const formData = new FormData()
  formData.append('file', avatarFile.value)
  /**
   * 常量 user：保存当前模块的页面状态、配置项、接口实例或计算结果。
   */
  const user = await api.uploadUserAvatar(avatarTarget.value.id, formData)
  if (auth.user?.id === user.id) {
    auth.updateLocalUser(user)
    await auth.loadMe()
  }
  ElMessage.success('头像已绑定')
  avatarVisible.value = false
  await load()
}

/**
 * 执行 resetAvatarDialog 方法。
 * 
 * 实现步骤：
 * 1. 读取当前页面状态或调用参数；
 * 2. 完成对应的校验、接口调用或数据转换；
 * 3. 更新页面状态或返回处理结果。
 */
function resetAvatarDialog() {
  avatarTarget.value = undefined
  avatarFile.value = undefined
  avatarPreview.value = ''
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
  const payload = { ...form, organizationCode: isAdmin.value ? form.organizationCode : currentUserCompanyCode() }
  if (editingId.value) {
    await api.updateUser(editingId.value, payload)
  } else {
    await api.createUser(payload)
  }
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
  await api.deleteUser(id)
  ElMessage.success('删除成功')
  await load()
}

/**
 * 批量删除人员。
 *
 * 实现步骤：
 * 1. 校验表格是否已经勾选人员；
 * 2. 弹出二次确认，避免误删人员账号和授权；
 * 3. 调用后端批量删除接口；
 * 4. 删除成功后刷新列表。
 */
async function batchRemove() {
  if (selectedRows.value.length === 0) {
    ElMessage.warning('请选择需要删除的人员')
    return
  }
  await ElMessageBox.confirm(`确认删除选中的 ${selectedRows.value.length} 个人员？`, '批量删除确认', {
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
  await api.batchDeleteUsers(selectedRows.value.map((row) => row.id))
  ElMessage.success('批量删除成功')
  await load()
}

onMounted(async () => {
  await Promise.all([load(), refreshDictionaryOptions()])
})

onActivated(() => {
  void refreshDictionaryOptions()
})
</script>

<style scoped>
.tag-gap {
  margin-right: 6px;
}

.full {
  width: 100%;
}

.avatar-dialog-body {
  display: flex;
  align-items: center;
  gap: 18px;
}

.filter-form {
  margin-bottom: 14px;
  padding: 14px 14px 0;
  border: 1px solid #e5e7eb;
  border-radius: 6px;
  background: #fff;
}
</style>
