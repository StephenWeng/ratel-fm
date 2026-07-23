<template>
  <main class="login-page">
    <section class="login-visual">
      <div class="brand-lockup">
        <div class="brand-mark">
          <SystemLogo />
        </div>
        <div>
          <h1>Ratel FM</h1>
          <p>财务管理 ERP</p>
        </div>
      </div>
      <div class="visual-grid">
        <div class="visual-cell strong">
          <span>总账</span>
          <strong>平衡</strong>
        </div>
        <div class="visual-cell">
          <span>采购</span>
          <strong>协同</strong>
        </div>
        <div class="visual-cell">
          <span>物流</span>
          <strong>跟踪</strong>
        </div>
        <div class="visual-cell strong">
          <span>权限</span>
          <strong>可控</strong>
        </div>
      </div>
    </section>

    <section class="login-panel">
      <div class="brand-row">
        <div class="brand-mark compact">
          <SystemLogo />
        </div>
        <div>
          <h1>Ratel FM</h1>
          <p>财务管理 ERP</p>
        </div>
      </div>
      <h2>账号登录</h2>
      <el-form ref="formRef" :model="form" :rules="rules" label-position="top" @keyup.enter="submit">
        <el-form-item label="所属公司" prop="organizationCode">
          <el-select v-model="form.organizationCode" size="large" filterable class="full" placeholder="请选择所属公司" :loading="companiesLoading">
            <el-option v-for="item in companyOptions" :key="item.code" :label="item.name" :value="item.code" />
          </el-select>
        </el-form-item>
        <el-form-item label="登录账号" prop="username">
          <el-input v-model="form.username" size="large" autocomplete="username" placeholder="请输入账号或身份证号" />
        </el-form-item>
        <el-form-item label="密码" prop="password">
          <el-input v-model="form.password" size="large" type="password" autocomplete="current-password" show-password />
        </el-form-item>
        <el-button class="login-button" type="primary" size="large" :loading="loading" @click="submit">
          登录
        </el-button>
      </el-form>
    </section>
  </main>
</template>

<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue'
import type { FormInstance, FormRules } from 'element-plus'
import { ElMessage, ElMessageBox } from 'element-plus'
import { useRouter } from 'vue-router'
import SystemLogo from '@/components/brand/SystemLogo.vue'
import { api } from '@/api/fm'
import { useAuthStore } from '@/stores/auth'
import { pageMenus } from '@/router/menuRoutes'
import type { BasicDictionaryView } from '@/types/api'

/**
 * 常量 router：保存当前模块的页面状态、配置项、接口实例或计算结果。
 */
const router = useRouter()
/**
 * 常量 auth：保存当前模块的页面状态、配置项、接口实例或计算结果。
 */
const auth = useAuthStore()
/**
 * 常量 formRef：保存当前模块的页面状态、配置项、接口实例或计算结果。
 */
const formRef = ref<FormInstance>()
/**
 * 常量 loading：保存当前模块的页面状态、配置项、接口实例或计算结果。
 */
const loading = ref(false)
/** 所属公司下拉加载状态，避免登录页重复提交前公司列表尚未准备完成。 */
const companiesLoading = ref(false)
/** 登录页可选所属公司账套，来自后端 ORGANIZATION 启用字典。 */
const companyOptions = ref<BasicDictionaryView[]>([])
/**
 * 常量 form：保存当前模块的页面状态、配置项、接口实例或计算结果。
 */
const form = reactive({
  /**
   * 字段 organizationCode：表示登录选择的所属公司账套编码。
   */
  organizationCode: '',
  /**
   * 字段 username：表示表单、筛选条件、接口数据或组件状态中的 username 值。
   */
  username: '',
  /**
   * 字段 password：表示表单、筛选条件、接口数据或组件状态中的 password 值。
   */
  password: ''
})

/**
 * 常量 rules：保存当前模块的页面状态、配置项、接口实例或计算结果。
 */
const rules: FormRules = {
  /**
   * 字段 organizationCode：登录所属公司必填，后端据此按账套校验账号和身份证。
   */
  organizationCode: [
    { required: true, message: '请选择所属公司', trigger: 'change' },
    { max: 80, message: '所属公司编码长度不能超过80个字符', trigger: 'change' }
  ],
  /**
   * 字段 username：表示表单、筛选条件、接口数据或组件状态中的 username 值。
   */
  username: [
    { required: true, message: '请输入登录账号', trigger: 'blur' },
    { max: 80, message: '登录账号长度不能超过80个字符', trigger: 'blur' }
  ],
  /**
   * 字段 password：表示表单、筛选条件、接口数据或组件状态中的 password 值。
   */
  password: [
    { required: true, message: '请输入密码', trigger: 'blur' },
    { max: 72, message: '密码长度不能超过72个字符', trigger: 'blur' }
  ]
}

/**
 * 执行 submit 方法。
 * 
 * 实现步骤：
 * 1. 读取当前页面状态或调用参数；
 * 2. 完成对应的校验、接口调用或数据转换；
 * 3. 更新页面状态或返回处理结果。
 */
async function submit() {
  /**
   * 常量 valid：保存当前模块的页面状态、配置项、接口实例或计算结果。
   */
  const valid = await formRef.value?.validate().catch(() => false)
  if (!valid) {
    return
  }
  loading.value = true
  try {
    /**
     * 常量 result：保存当前模块的页面状态、配置项、接口实例或计算结果。
     */
    const result = await auth.login(form.organizationCode, form.username, form.password, false, '/login')
    if (result.repeated) {
      /**
       * 常量 confirmed：保存当前模块的页面状态、配置项、接口实例或计算结果。
       */
      const confirmed = await confirmForceLogin(result.conflictMessage || '当前人员已在同类终端登录，是否挤掉之前登录者？')
      if (!confirmed) {
        return
      }
      await auth.login(form.organizationCode, form.username, form.password, true, '/login')
    }
    ElMessage.success('登录成功')
    router.replace(firstAuthorizedPath())
  } finally {
    loading.value = false
  }
}

/**
 * 执行 confirmForceLogin 方法。
 * 
 * 实现步骤：
 * 1. 读取当前页面状态或调用参数；
 * 2. 完成对应的校验、接口调用或数据转换；
 * 3. 更新页面状态或返回处理结果。
 */
async function confirmForceLogin(message: string): Promise<boolean> {
  try {
    await ElMessageBox.confirm(message, '重复登录提醒', {
      /**
       * 字段 confirmButtonText：表示表单、筛选条件、接口数据或组件状态中的 confirmButtonText 值。
       */
      confirmButtonText: '是，挤掉之前登录者',
      /**
       * 字段 cancelButtonText：表示表单、筛选条件、接口数据或组件状态中的 cancelButtonText 值。
       */
      cancelButtonText: '否',
      /**
       * 字段 type：表示表单、筛选条件、接口数据或组件状态中的 type 值。
       */
      type: 'warning'
    })
    return true
  } catch {
    return false
  }
}

/**
 * 执行 firstAuthorizedPath 方法。
 * 
 * 实现步骤：
 * 1. 读取当前页面状态或调用参数；
 * 2. 完成对应的校验、接口调用或数据转换；
 * 3. 更新页面状态或返回处理结果。
 */
function firstAuthorizedPath() {
  return pageMenus.find((item) => auth.hasMenu(item.menuCode))?.path || auth.loginPath()
}

/**
 * 加载登录所属公司选项。
 *
 * 实现步骤：
 * 1. 调用登录前专用接口读取启用所属公司；
 * 2. 默认选中第一家公司；
 * 3. 读取失败时回退系统预置公司编码，避免首次部署时无法登录初始化管理员。
 */
async function loadCompanies() {
  companiesLoading.value = true
  try {
    companyOptions.value = await api.loginCompanies()
    form.organizationCode = companyOptions.value[0]?.code || 'ORGANIZATION_RATEL'
  } catch {
    companyOptions.value = [{ id: 0, code: 'ORGANIZATION_RATEL', name: 'Ratel默认公司', sortOrder: 0, enabled: true, hasChildren: false, children: [] }]
    form.organizationCode = 'ORGANIZATION_RATEL'
  } finally {
    companiesLoading.value = false
  }
}

onMounted(loadCompanies)
</script>

<style scoped>
.login-page {
  position: relative;
  display: grid;
  min-height: 100vh;
  align-items: center;
  justify-items: end;
  padding: 32px clamp(28px, 5vw, 72px);
  background: var(--app-bg);
  color: var(--text-color);
}

.login-visual {
  position: absolute;
  inset: 0;
  display: flex;
  flex-direction: column;
  justify-content: space-between;
  padding: 48px;
  overflow: hidden;
  background:
    linear-gradient(rgba(13, 42, 59, 0.88), rgba(13, 42, 59, 0.76)),
    url("https://images.unsplash.com/photo-1454165804606-c3d57bc86b40?auto=format&fit=crop&w=1600&q=80") center/cover;
  color: #fff;
}

:global(:root[data-theme='dark']) .login-visual {
  background:
    linear-gradient(rgba(6, 12, 24, 0.92), rgba(10, 19, 35, 0.84)),
    url("https://images.unsplash.com/photo-1454165804606-c3d57bc86b40?auto=format&fit=crop&w=1600&q=80") center/cover;
}

:global(:root[data-theme='emerald']) .login-visual {
  background:
    linear-gradient(rgba(8, 72, 58, 0.86), rgba(13, 95, 76, 0.72)),
    url("https://images.unsplash.com/photo-1454165804606-c3d57bc86b40?auto=format&fit=crop&w=1600&q=80") center/cover;
}

:global(:root[data-theme='finance-blue']) .login-visual {
  background:
    linear-gradient(rgba(15, 47, 92, 0.88), rgba(30, 64, 119, 0.72)),
    url("https://images.unsplash.com/photo-1454165804606-c3d57bc86b40?auto=format&fit=crop&w=1600&q=80") center/cover;
}

.brand-lockup {
  display: flex;
  align-items: center;
  gap: 14px;
}

.brand-mark {
  display: grid;
  width: 54px;
  height: 54px;
  place-items: center;
  padding: 8px;
  border: 1px solid rgba(255, 255, 255, 0.65);
  border-radius: 6px;
  background: rgba(255, 255, 255, 0.14);
  color: #fff;
}

.brand-lockup h1 {
  margin: 0;
  font-size: 32px;
  line-height: 1.05;
}

.brand-lockup p {
  margin: 6px 0 0;
  color: rgba(255, 255, 255, 0.76);
}

.visual-grid {
  display: grid;
  max-width: 620px;
  grid-template-columns: repeat(4, minmax(110px, 1fr));
  gap: 12px;
}

.visual-cell {
  min-height: 112px;
  padding: 16px;
  border: 1px solid rgba(255, 255, 255, 0.24);
  border-radius: 6px;
  background: rgba(255, 255, 255, 0.12);
}

.visual-cell span {
  display: block;
  color: rgba(255, 255, 255, 0.74);
}

.visual-cell strong {
  display: block;
  margin-top: 24px;
  font-size: 22px;
}

.visual-cell.strong {
  background: rgba(46, 204, 113, 0.18);
}

.login-panel {
  position: relative;
  z-index: 2;
  display: flex;
  flex-direction: column;
  justify-content: center;
  width: min(420px, calc(100vw - 48px));
  padding: 34px;
  border: 1px solid var(--border-color);
  border-radius: 8px;
  background: var(--surface-color);
  box-shadow: 0 24px 70px var(--shadow-color);
  color: var(--text-color);
  backdrop-filter: blur(10px);
}

.login-panel h2 {
  margin: 0 0 24px;
  color: var(--heading-color);
  font-size: 24px;
}

.brand-row {
  display: flex;
  align-items: center;
  gap: 14px;
  margin-bottom: 22px;
}

.brand-mark.compact {
  width: 52px;
  height: 52px;
  border-color: var(--border-color);
  background: var(--primary-light-color);
  color: var(--primary-color);
}

.brand-row h1 {
  margin: 0;
  color: var(--heading-color);
  font-size: 30px;
  line-height: 1.05;
}

.brand-row p {
  margin: 6px 0 0;
  color: var(--secondary-text-color);
}

.login-button {
  width: 100%;
  margin-top: 4px;
}

.full {
  width: 100%;
}

.login-meta {
  margin-top: 18px;
  color: var(--muted-text-color);
  font-size: 13px;
}

:deep(.el-form-item__label) {
  color: var(--secondary-text-color);
}

@media (max-width: 860px) {
  .login-page {
    justify-items: center;
    padding: 28px 18px;
  }

  .login-visual {
    padding: 28px;
  }

  .visual-grid {
    grid-template-columns: repeat(2, minmax(110px, 1fr));
  }

  .login-panel {
    padding: 24px;
  }
}
</style>
