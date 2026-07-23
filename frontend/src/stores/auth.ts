import { defineStore } from 'pinia'
import { api } from '@/api/fm'
import type { LoginResponse, MenuView, PermissionCode, UserView } from '@/types/api'

/**
 * AuthState 类型定义，用于约束页面状态、接口入参或接口返回数据结构。
 */
interface AuthState {
  /**
   * 字段 expiresAt：表示表单、筛选条件、接口数据或组件状态中的 expiresAt 值。
   */
  expiresAt: string
  /**
   * 字段 user：表示表单、筛选条件、接口数据或组件状态中的 user 值。
   */
  user: UserView | null
  /**
   * 字段 preferredLoginPath：表示表单、筛选条件、接口数据或组件状态中的 preferredLoginPath 值。
   */
  preferredLoginPath: LoginPath
  /**
   * 字段 authorizedMenuCodes：表示表单、筛选条件、接口数据或组件状态中的 authorizedMenuCodes 值。
   */
  authorizedMenuCodes: string[]
  /**
   * 字段 authorizedMenus：表示表单、筛选条件、接口数据或组件状态中的 authorizedMenus 值。
   */
  authorizedMenus: MenuView[]
  /**
   * 字段 menuCodesLoaded：表示表单、筛选条件、接口数据或组件状态中的 menuCodesLoaded 值。
   */
  menuCodesLoaded: boolean
  /**
   * 字段 expiredDialogVisible：表示表单、筛选条件、接口数据或组件状态中的 expiredDialogVisible 值。
   */
  expiredDialogVisible: boolean
  /**
   * 字段 expiredMessage：表示表单、筛选条件、接口数据或组件状态中的 expiredMessage 值。
   */
  expiredMessage: string
  /**
   * 字段 expiredCountdown：表示表单、筛选条件、接口数据或组件状态中的 expiredCountdown 值。
   */
  expiredCountdown: number
  /**
   * 字段 expiredTimer：表示表单、筛选条件、接口数据或组件状态中的 expiredTimer 值。
   */
  expiredTimer: number | null
  /**
   * 字段 silentUnauthorized：表示表单、筛选条件、接口数据或组件状态中的 silentUnauthorized 值。
   */
  silentUnauthorized: boolean
}

/**
 * 常量 savedUser：保存当前模块的页面状态、配置项、接口实例或计算结果。
 */
const savedUser = localStorage.getItem('ratel-fm-user')
/**
 * 常量 savedExpiresAt：保存当前模块的页面状态、配置项、接口实例或计算结果。
 */
const savedExpiresAt = localStorage.getItem('ratel-fm-expires-at') || ''
/**
 * 常量 savedLoginPath：保存当前模块的页面状态、配置项、接口实例或计算结果。
 */
const savedLoginPath = normalizeLoginPath(localStorage.getItem('ratel-fm-login-path'))
/**
 * 常量 parsedSavedUser：保存当前模块的页面状态、配置项、接口实例或计算结果。
 */
const parsedSavedUser = savedUser ? (JSON.parse(savedUser) as UserView) : null
/**
 * LoginPath 类型定义，用于约束页面状态、接口入参或接口返回数据结构。
 */
type LoginPath = '/login' | '/login/star'
/** JWT Cookie 名称，和后端 app.security.token-cookie-name 默认值保持一致。 */
const TOKEN_COOKIE_NAME = 'FM_TOKEN'
if (parsedSavedUser && 'menuCodes' in parsedSavedUser) {
  delete (parsedSavedUser as UserView & { menuCodes?: string[] }).menuCodes
}

/**
 * 常量 useAuthStore：保存当前模块的页面状态、配置项、接口实例或计算结果。
 */
export const useAuthStore = defineStore('auth', {
  /**
   * 字段 state：表示表单、筛选条件、接口数据或组件状态中的 state 值。
   */
  state: (): AuthState => ({
    /**
     * 字段 expiresAt：表示表单、筛选条件、接口数据或组件状态中的 expiresAt 值。
     */
    expiresAt: savedExpiresAt,
    /**
     * 字段 user：表示表单、筛选条件、接口数据或组件状态中的 user 值。
     */
    user: parsedSavedUser,
    /**
     * 字段 preferredLoginPath：表示表单、筛选条件、接口数据或组件状态中的 preferredLoginPath 值。
     */
    preferredLoginPath: savedLoginPath,
    /**
     * 字段 authorizedMenuCodes：表示表单、筛选条件、接口数据或组件状态中的 authorizedMenuCodes 值。
     */
    authorizedMenuCodes: [],
    /**
     * 字段 authorizedMenus：表示表单、筛选条件、接口数据或组件状态中的 authorizedMenus 值。
     */
    authorizedMenus: [],
    /**
     * 字段 menuCodesLoaded：表示表单、筛选条件、接口数据或组件状态中的 menuCodesLoaded 值。
     */
    menuCodesLoaded: false,
    /**
     * 字段 expiredDialogVisible：表示表单、筛选条件、接口数据或组件状态中的 expiredDialogVisible 值。
     */
    expiredDialogVisible: false,
    /**
     * 字段 expiredMessage：表示表单、筛选条件、接口数据或组件状态中的 expiredMessage 值。
     */
    expiredMessage: '当前登录过期，即将调整登录首页。',
    /**
     * 字段 expiredCountdown：表示表单、筛选条件、接口数据或组件状态中的 expiredCountdown 值。
     */
    expiredCountdown: 10,
    /**
     * 字段 expiredTimer：表示表单、筛选条件、接口数据或组件状态中的 expiredTimer 值。
     */
    expiredTimer: null,
    /**
     * 字段 silentUnauthorized：表示表单、筛选条件、接口数据或组件状态中的 silentUnauthorized 值。
     */
    silentUnauthorized: false
  }),
  /**
   * 字段 getters：表示表单、筛选条件、接口数据或组件状态中的 getters 值。
   */
  getters: {
    permissions(state): PermissionCode[] {
      return state.user?.roles.flatMap((role) => role.permissions) || []
    },
    menuCodes(state): string[] {
      return state.authorizedMenuCodes
    },
    menus(state): MenuView[] {
      return state.authorizedMenus
    },
    isAuthenticated(state): boolean {
      return Boolean(state.user)
    }
  },
  /**
   * 字段 actions：表示表单、筛选条件、接口数据或组件状态中的 actions 值。
   */
  actions: {
    async login(organizationCode: string, username: string, password: string, force = false, loginPath?: LoginPath): Promise<LoginResponse> {
      /**
       * 常量 data：保存当前模块的页面状态、配置项、接口实例或计算结果。
       */
      const data = await api.login({ organizationCode, username, password, terminalType: 'PC', force })
      if (data.repeated) {
        return data
      }
      if (!data.expiresAt) {
        throw new Error('登录响应缺少过期时间')
      }
      this.expiresAt = data.expiresAt
      this.user = data.user
      if (loginPath) {
        this.setPreferredLoginPath(loginPath)
      }
      localStorage.setItem('ratel-fm-expires-at', data.expiresAt)
      localStorage.setItem('ratel-fm-user', JSON.stringify(data.user))
      await this.refreshMenuCodes()
      return data
    },
    async loadMe() {
      /**
       * 常量 data：保存当前模块的页面状态、配置项、接口实例或计算结果。
       */
      const data = await api.me()
      this.user = data
      localStorage.setItem('ratel-fm-user', JSON.stringify(data))
    },
    async refreshMenuCodes() {
      const [codeData, menus] = await Promise.all([api.myMenuCodes(), api.myMenus()])
      this.authorizedMenuCodes = codeData.menuCodes || []
      this.authorizedMenus = menus || []
      this.menuCodesLoaded = true
    },
    async ensureSessionReady(forceValidate = false) {
      if (!this.user || forceValidate) {
        await this.loadMe()
      }
      if (!this.menuCodesLoaded || forceValidate) {
        await this.refreshMenuCodes()
      }
    },
    setPreferredLoginPath(path: string) {
      // 记录当前使用的登录页，退出、过期和强制回登录时都回到同一个登录入口。
      this.preferredLoginPath = normalizeLoginPath(path)
      localStorage.setItem('ratel-fm-login-path', this.preferredLoginPath)
    },
    loginPath() {
      return this.preferredLoginPath
    },
    clearLocalSession() {
      // 清理前端保存的登录人、菜单授权和过期时间，避免旧数据继续参与页面渲染。
      this.expiresAt = ''
      this.user = null
      this.authorizedMenuCodes = []
      this.authorizedMenus = []
      this.menuCodesLoaded = false
      localStorage.removeItem('ratel-fm-expires-at')
      localStorage.removeItem('ratel-fm-user')
      this.clearTokenCookie()
    },
    clearTokenCookie() {
      // HttpOnly Cookie 需要后端 Set-Cookie 才能删除；这里处理非 HttpOnly 或旧版本遗留 Cookie。
      if (typeof document === 'undefined') {
        return
      }
      document.cookie = `${TOKEN_COOKIE_NAME}=; Max-Age=0; path=/`
      document.cookie = `${TOKEN_COOKIE_NAME}=; Max-Age=0; path=/ratel/fm`
    },
    setSilentUnauthorized(silent: boolean) {
      // 登录页预校验旧会话时启用静默模式，避免无效令牌触发倒计时弹窗。
      this.silentUnauthorized = silent
    },
    async logout() {
      try {
        await api.logout()
      } catch {
        // 退出时即使服务端会话已失效，也要清理本地登录状态。
      } finally {
        this.clearLocalSession()
      }
    },
    startExpiredCountdown(message?: string) {
      if (this.expiredDialogVisible) {
        return
      }
      this.clearLocalSession()
      this.expiredDialogVisible = true
      this.expiredMessage = message || '当前登录过期，即将调整登录首页。'
      this.expiredCountdown = 10
      this.expiredTimer = window.setInterval(() => {
        this.expiredCountdown -= 1
        if (this.expiredCountdown <= 0) {
          this.stopExpiredCountdown()
          import('@/router').then(({ default: router }) => router.replace(this.loginPath()))
        }
      }, 1000)
    },
    stopExpiredCountdown() {
      if (this.expiredTimer) {
        window.clearInterval(this.expiredTimer)
      }
      this.expiredTimer = null
      this.expiredDialogVisible = false
    },
    hasPermission(permission: PermissionCode) {
      return this.permissions.includes(permission)
    },
    hasAny(permissions: PermissionCode[]) {
      return permissions.some((permission) => this.permissions.includes(permission))
    },
    hasMenu(menuCode: string) {
      return this.menuCodes.includes(menuCode)
    },
    hasAnyMenu(menuCodes: string[]) {
      return menuCodes.some((menuCode) => this.menuCodes.includes(menuCode))
    },
    updateLocalUser(user: UserView) {
      this.user = user
      localStorage.setItem('ratel-fm-user', JSON.stringify(user))
    }
  }
})

/**
 * 规范化登录页路径。
 *
 * 实现步骤：只允许默认登录页和星空登录页两个入口；其他值全部回退到默认登录页。
 */
function normalizeLoginPath(path: string | null): LoginPath {
  return path === '/login/star' ? '/login/star' : '/login'
}
