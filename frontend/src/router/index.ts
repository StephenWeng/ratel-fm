import { createRouter, createWebHistory, type RouteRecordRaw } from 'vue-router'
import { useAuthStore } from '@/stores/auth'
import { pageMenus } from '@/router/menuRoutes'

/**
 * 常量 routes：保存当前模块的页面状态、配置项、接口实例或计算结果。
 */
const routes: RouteRecordRaw[] = [
  { path: '/login', component: () => import('@/views/auth/LoginView.vue'), meta: { public: true, loginPage: true } },
  { path: '/login/star', component: () => import('@/views/auth/StarLoginView.vue'), meta: { public: true, loginPage: true } },
  {
    /**
     * 字段 path：表示表单、筛选条件、接口数据或组件状态中的 path 值。
     */
    path: '/',
    /**
     * 字段 component：表示表单、筛选条件、接口数据或组件状态中的 component 值。
     */
    component: () => import('@/views/shell/ShellView.vue'),
    /**
     * 字段 redirect：表示表单、筛选条件、接口数据或组件状态中的 redirect 值。
     */
    redirect: '/dashboard',
    /**
     * 字段 children：表示表单、筛选条件、接口数据或组件状态中的 children 值。
     */
    children: [
      { path: 'dashboard', component: () => import('@/views/dashboard/DashboardView.vue'), meta: { menuCode: 'PAGE_DASHBOARD' } },
      { path: 'users', component: () => import('@/views/system/UsersView.vue'), meta: { menuCode: 'PAGE_USERS' } },
      { path: 'roles', component: () => import('@/views/system/RolesView.vue'), meta: { menuCode: 'PAGE_ROLES' } },
      { path: 'menus', component: () => import('@/views/system/MenusView.vue'), meta: { menuCode: 'PAGE_MENUS' } },
      { path: 'basic-dictionaries', component: () => import('@/views/basic/BasicDictionariesView.vue'), meta: { menuCode: 'PAGE_BASIC_DICTIONARIES' } },
      { path: 'subjects', component: () => import('@/views/finance/SubjectsView.vue'), meta: { menuCode: 'PAGE_SUBJECTS' } },
      { path: 'vouchers', component: () => import('@/views/finance/VouchersView.vue'), meta: { menuCode: 'PAGE_VOUCHERS' } },
      { path: 'accounting-periods', component: () => import('@/views/finance/AccountingPeriodsView.vue'), meta: { menuCode: 'PAGE_ACCOUNTING_PERIODS' } },
      { path: 'cashier', component: () => import('@/views/finance/CashierView.vue'), meta: { menuCode: 'PAGE_CASHIER' } },
      { path: 'accounting-platform', component: () => import('@/views/finance/AccountingPlatformView.vue'), meta: { menuCode: 'PAGE_ACCOUNTING_PLATFORM' } },
      { path: 'purchase-orders', component: () => import('@/views/operation/PurchaseOrdersView.vue'), meta: { menuCode: 'PAGE_PURCHASE' } },
      { path: 'shipments', component: () => import('@/views/operation/ShipmentsView.vue'), meta: { menuCode: 'PAGE_SHIPMENTS' } },
      { path: 'inventory', component: () => import('@/views/inventory/InventoryView.vue'), meta: { menuCode: 'PAGE_INVENTORY' } },
      { path: 'ar-ap', component: () => import('@/views/receivable/ArApView.vue'), meta: { menuCode: 'PAGE_AR_AP' } },
      { path: 'ar-ap-stats', redirect: { path: '/ar-ap', query: { tab: 'paymentStats' } }, meta: { menuCode: 'PAGE_AR_AP_STATS' } },
      { path: 'workflow-center', component: () => import('@/views/workflow/WorkflowCenterView.vue'), meta: { menuCode: 'PAGE_WORKFLOW_CENTER' } },
      { path: 'workflow-configs', component: () => import('@/views/workflow/WorkflowConfigsView.vue'), meta: { menuCode: 'PAGE_WORKFLOW_CONFIGS' } },
      { path: 'workflow-definitions', component: () => import('@/views/workflow/WorkflowDefinitionsView.vue'), meta: { menuCode: 'PAGE_WORKFLOW_DEFINITIONS' } },
      { path: 'assistant', component: () => import('@/views/assistant/AssistantView.vue'), meta: { menuCode: 'PAGE_ASSISTANT' } },
      { path: 'ai-status', component: () => import('@/views/assistant/AiStatusView.vue'), meta: { menuCode: 'PAGE_AI_STATUS' } },
      { path: 'reports', component: () => import('@/views/finance/ReportsView.vue'), meta: { menuCode: 'PAGE_REPORTS' } },
      { path: 'search', component: () => import('@/views/search/SearchView.vue'), meta: { menuCode: 'PAGE_SEARCH' } },
      { path: 'operation-logs', component: () => import('@/views/audit/OperationLogsView.vue'), meta: { menuCode: 'PAGE_OPERATION_LOGS' } }
    ]
  },
  {
    /**
     * 字段 path：表示表单、筛选条件、接口数据或组件状态中的 path 值。
     */
    path: '/:pathMatch(.*)*',
    /**
     * 字段 component：表示表单、筛选条件、接口数据或组件状态中的 component 值。
     */
    component: () => import('@/views/error/RouteFallbackView.vue'),
    /**
     * 字段 meta：表示表单、筛选条件、接口数据或组件状态中的 meta 值。
     */
    meta: { fallbackRoute: true }
  }
]

/**
 * 常量 router：保存当前模块的页面状态、配置项、接口实例或计算结果。
 */
const router = createRouter({
  /**
   * 字段 history：表示表单、筛选条件、接口数据或组件状态中的 history 值。
   */
  history: createWebHistory(import.meta.env.BASE_URL),
  routes
})

router.beforeEach(async (to) => {
  /**
   * 常量 auth：保存当前模块的页面状态、配置项、接口实例或计算结果。
   */
  const auth = useAuthStore()
  if (to.meta.loginPage) {
    auth.setPreferredLoginPath(to.path)
  }
  if (to.meta.loginPage && auth.isAuthenticated) {
    try {
      // 登录页只用旧本地会话尝试自动跳转，失败时静默清理 Cookie 和本地缓存，不弹过期倒计时。
      auth.setSilentUnauthorized(true)
      await auth.ensureSessionReady(true)
    } catch {
      auth.clearLocalSession()
      return true
    } finally {
      auth.setSilentUnauthorized(false)
    }
    /**
     * 常量 authorizedPath：保存当前模块的页面状态、配置项、接口实例或计算结果。
     */
    const authorizedPath = firstAuthorizedPath(auth)
    return authorizedPath === to.path ? true : authorizedPath
  }
  if (!to.meta.public) {
    try {
      // 业务页面直接访问时必须先用 Cookie 向后端校验 JWT，再加载当前人员和授权菜单。
      await auth.ensureSessionReady(true)
    } catch (error) {
      // 401 已由 Axios 拦截器触发倒计时弹窗；这里中断本次页面进入，等待倒计时跳回登录页。
      if (!auth.expiredDialogVisible && isUnauthorizedError(error)) {
        auth.startExpiredCountdown('当前登录过期，即将调整登录首页。')
      }
      return false
    }
  }
  /**
   * 常量 menuCode：保存当前模块的页面状态、配置项、接口实例或计算结果。
   */
  const menuCode = to.meta.menuCode as string | undefined
  if (menuCode && !auth.hasMenu(menuCode)) {
    /**
     * 常量 authorizedPath：保存当前模块的页面状态、配置项、接口实例或计算结果。
     */
    const authorizedPath = homePath(auth)
    return authorizedPath === to.path ? true : authorizedPath
  }
  if (to.meta.fallbackRoute) {
    return homePath(auth)
  }
  return true
})

/**
 * 执行 isUnauthorizedError 方法。
 * 
 * 实现步骤：
 * 1. 读取当前页面状态或调用参数；
 * 2. 完成对应的校验、接口调用或数据转换；
 * 3. 更新页面状态或返回处理结果。
 */
function isUnauthorizedError(error: unknown) {
  return Boolean((error as { response?: { status?: number } })?.response?.status === 401 || authFailureVisible())
}

/**
 * 执行 authFailureVisible 方法。
 * 
 * 实现步骤：
 * 1. 读取当前页面状态或调用参数；
 * 2. 完成对应的校验、接口调用或数据转换；
 * 3. 更新页面状态或返回处理结果。
 */
function authFailureVisible() {
  return useAuthStore().expiredDialogVisible
}

/**
 * 执行 homePath 方法。
 * 
 * 实现步骤：
 * 1. 读取当前页面状态或调用参数；
 * 2. 完成对应的校验、接口调用或数据转换；
 * 3. 更新页面状态或返回处理结果。
 */
function homePath(auth: ReturnType<typeof useAuthStore>) {
  return auth.hasMenu('PAGE_DASHBOARD') ? '/dashboard' : firstAuthorizedPath(auth)
}

/**
 * 执行 firstAuthorizedPath 方法。
 * 
 * 实现步骤：
 * 1. 读取当前页面状态或调用参数；
 * 2. 完成对应的校验、接口调用或数据转换；
 * 3. 更新页面状态或返回处理结果。
 */
function firstAuthorizedPath(auth: ReturnType<typeof useAuthStore>) {
  return pageMenus.find((item) => auth.hasMenu(item.menuCode))?.path || auth.loginPath()
}

export default router
