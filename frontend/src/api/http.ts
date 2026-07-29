import axios from 'axios'
import type { AxiosRequestConfig, InternalAxiosRequestConfig } from 'axios'
import { ElMessageBox, ElNotification } from 'element-plus'
import router from '@/router'
import { useAuthStore } from '@/stores/auth'
import type { ApiResponse } from '@/types/api'
import { isAuthFailureNoticeActive, markApiResultNotice, markAuthFailureNotice } from '@/utils/apiNoticeDedupe'

/**
 * 常量 AUTH_FAILURE_CODES：保存当前模块的页面状态、配置项、接口实例或计算结果。
 */
const AUTH_FAILURE_CODES = new Set(['200001', '200002', '200003'])
/**
 * 常量 LOGIN_PATHS：保存当前模块的页面状态、配置项、接口实例或计算结果。
 */
const LOGIN_PATHS = new Set(['/login', '/login/star'])
/**
 * 常量 SUCCESS_NOTICE_DURATION：成功提示右下角展示 10 秒。
 */
const SUCCESS_NOTICE_DURATION = 10_000
/**
 * 常量 WARNING_NOTICE_DURATION：警告弹窗自动关闭倒计时 10 秒。
 */
const WARNING_NOTICE_DURATION = 10_000
/**
 * 常量 AUTH_FAILURE_NOTICE_DURATION：登录失效后屏蔽后续顶部报错的时间窗口。
 */
const AUTH_FAILURE_NOTICE_DURATION = 12_000
/**
 * 常量 basePath：保存当前模块的页面状态、配置项、接口实例或计算结果。
 */
const basePath = import.meta.env.BASE_URL.replace(/\/$/, '')

/**
 * RatelRequestConfig 为后台轮询补充静默错误提示开关。
 *
 * 轮询调用方仍会收到 Promise rejection 并维护失败次数，只是不再为每次网络失败打开全局弹窗。
 */
export interface RatelRequestConfig extends AxiosRequestConfig {
  /** 是否禁止统一响应拦截器展示失败弹窗。 */
  silentErrorNotice?: boolean
}

/**
 * AuthExpiredError 类型定义，用于区分登录失效熔断错误和普通业务异常。
 */
interface AuthExpiredError extends Error {
  /**
   * 字段 authExpired：标记本错误已经由登录失效弹窗接管。
   */
  authExpired: true
}

/**
 * 常量 http：保存当前模块的页面状态、配置项、接口实例或计算结果。
 */
export const http = axios.create({
  /**
   * 字段 baseURL：表示表单、筛选条件、接口数据或组件状态中的 baseURL 值。
   */
  baseURL: basePath,
  /**
   * 字段 timeout：表示表单、筛选条件、接口数据或组件状态中的 timeout 值。
   */
  timeout: 20000,
  /**
   * 字段 withCredentials：表示表单、筛选条件、接口数据或组件状态中的 withCredentials 值。
   */
  withCredentials: true
})

http.interceptors.request.use((config) => {
  try {
    ensureAuthRequestAllowed(config.url)
  } catch (error) {
    return Promise.reject(error)
  }
  return applyNoCacheForGet(config)
})

http.interceptors.response.use(
  (response) => {
    /**
     * 常量 payload：保存当前模块的页面状态、配置项、接口实例或计算结果。
     */
    const payload = response.data as ApiResponse<unknown>
    if (payload && payload.success === false) {
      if (payload.code === '400002') {
        return response
      }
      if (AUTH_FAILURE_CODES.has(payload.code || '')) {
        const message = readableMessage(payload.message || '', '当前登录过期，请重新登录')
        handleAuthFailure(message)
        return Promise.reject(createAuthExpiredError(message))
      }
      if (payload.code === '201') {
        const message = readableMessage(payload.message || '', '请求警告')
        showApiResult('warning', message, response.config.url, response.config.method, payload.data, response.config)
        return Promise.reject(new Error(message))
      }
      const message = readableMessage(payload.message || '', '请求失败')
      showApiResult('failure', message, response.config.url, response.config.method, payload.data, response.config)
      return Promise.reject(new Error(message))
    }
    if (payload && payload.success === true && shouldNotifySuccess(response.config.method, response.config.url)) {
      showApiResult('success', payload.message || '操作成功', response.config.url, response.config.method, payload.data)
    }
    return response
  },
  (error) => {
    if (isAuthExpiredError(error)) {
      return Promise.reject(error)
    }
    /**
     * 常量 status：保存当前模块的页面状态、配置项、接口实例或计算结果。
     */
    const status = error.response?.status
    /**
     * 常量 message：保存当前模块的页面状态、配置项、接口实例或计算结果。
     */
    const message = userFriendlyRequestError(error)
    if (status === 401) {
      handleAuthFailure(message)
      return Promise.reject(createAuthExpiredError(message))
    } else {
      showApiResult('error', message, error.config?.url, error.config?.method, undefined, error.config)
    }
    return Promise.reject(new Error(message))
  }
)

/**
 * 给 GET 请求追加防缓存控制。
 *
 * 实现步骤：
 * 1. 只处理查询类 GET 请求，写操作保持原样；
 * 2. 设置 Cache-Control、Pragma 和 Expires 请求头，要求浏览器和中间代理重新校验；
 * 3. 增加 `_t` 时间戳参数，绕过浏览器对相同 URL 的本地缓存；
 * 4. 返回修改后的 Axios 配置，后续响应拦截器继续按统一响应格式处理。
 */
function applyNoCacheForGet(config: InternalAxiosRequestConfig) {
  /** 当前请求方法，空值按 GET 兼容 axios 默认行为。 */
  const method = (config.method || 'get').toLowerCase()
  if (method !== 'get') {
    return config
  }
  config.headers.set('Cache-Control', 'no-cache, no-store, must-revalidate')
  config.headers.set('Pragma', 'no-cache')
  config.headers.set('Expires', '0')
  config.params = {
    ...(config.params || {}),
    _t: Date.now()
  }
  return config
}

/**
 * 统一处理无令牌、令牌无效和令牌过期。
 *
 * 实现步骤：
 * 1. 登录页预校验旧会话时只清理本地状态，不打扰用户；
 * 2. 业务页面只弹出一个登录过期倒计时；
 * 3. 同步开启顶部消息屏蔽窗口，避免并发请求重复报错。
 */
export function handleAuthFailure(message: string) {
  /**
   * 常量 auth：保存当前模块的页面状态、配置项、接口实例或计算结果。
   */
  const auth = useAuthStore()
  // 登录页面和登录页预校验旧会话时，不弹“登录过期”倒计时，只清理令牌和本地缓存。
  if (auth.silentUnauthorized || LOGIN_PATHS.has(router.currentRoute.value.path)) {
    auth.clearLocalSession()
    return
  }
  markAuthFailureNotice(AUTH_FAILURE_NOTICE_DURATION)
  auth.startExpiredCountdown(message)
}

/**
 * 业务请求在登录失效弹窗期间直接熔断，避免页面组件继续并发请求并重复弹错。
 */
export function ensureAuthRequestAllowed(url?: string) {
  const auth = useAuthStore()
  if (!auth.expiredDialogVisible || canRequestDuringAuthFailure(url)) {
    return
  }
  markAuthFailureNotice(AUTH_FAILURE_NOTICE_DURATION)
  throw createAuthExpiredError(auth.expiredMessage || '当前登录已失效，请重新登录。')
}

/**
 * 判断当前业务编码是否属于无令牌、令牌无效或令牌过期。
 */
export function isAuthFailureCode(code?: string) {
  return AUTH_FAILURE_CODES.has(code || '')
}

/**
 * 创建登录失效专用错误，调用方可据此跳过普通失败提示。
 */
export function createAuthExpiredError(message: string) {
  const error = new Error(message) as AuthExpiredError
  error.name = 'AuthExpiredError'
  error.authExpired = true
  return error
}

/**
 * 判断错误是否已经由登录失效处理接管。
 */
export function isAuthExpiredError(error: unknown) {
  return Boolean((error as Partial<AuthExpiredError> | undefined)?.authExpired)
}

/**
 * 登录失效倒计时期间允许登录页必要接口通过，其它业务接口全部熔断。
 */
function canRequestDuringAuthFailure(url?: string) {
  const text = url || ''
  return text.includes('/api/auth/login') || text.includes('/api/auth/companies')
}

/**
 * 将 Axios 或后端返回的技术异常转换成用户可理解提示。
 *
 * 实现步骤：
 * 1. 优先读取后端统一响应体中的 message；
 * 2. 根据 HTTP 状态码转换 404、500、502、超时等常见问题；
 * 3. 对 Network Error、JDBC、SQL、文件不存在等英文或技术错误做兜底翻译；
 * 4. 始终返回面向用户的处理建议，不把底层异常原文直接展示到页面。
 */
function userFriendlyRequestError(error: unknown) {
  /** Axios 错误对象，统一按可选字段读取，避免非 Axios 错误导致二次异常。 */
  const axiosError = error as {
    code?: string
    message?: string
    response?: {
      status?: number
      data?: {
        message?: string
      }
    }
  }
  /** HTTP 状态码，用于没有标准业务响应体时按协议状态兜底。 */
  const status = axiosError.response?.status
  /** 后端响应体或 Axios 自身错误消息，后续会进一步识别是否是技术文本。 */
  const rawMessage = axiosError.response?.data?.message || axiosError.message || ''
  if (status === 401) {
    return readableMessage(rawMessage, '当前登录已失效，请重新登录。')
  }
  if (status) {
    return readableMessage(rawMessage, httpStatusMessage(status))
  }
  if (axiosError.code === 'ECONNABORTED' || rawMessage.toLowerCase().includes('timeout')) {
    return '请求超时，请检查网络后重试。'
  }
  return readableMessage(rawMessage, '网络连接异常，请检查网络或稍后重试。')
}

/**
 * 清洗接口错误消息。
 *
 * 实现步骤：
 * 1. 空消息使用调用方传入的兜底中文；
 * 2. 识别英文网络错误、数据库错误、SQL 错误和文件错误；
 * 3. 已经是明确业务中文时原样返回，保留业务校验提示的准确性。
 */
function readableMessage(message: string, fallback: string) {
  /** 去除首尾空格后的原始消息。 */
  const text = (message || '').trim()
  if (!text) {
    return fallback
  }
  /** 小写技术消息，用于集中匹配英文异常关键字。 */
  const lower = text.toLowerCase()
  if (lower === 'network error' || lower.includes('network error')) {
    return '网络连接异常，请检查网络或稍后重试。'
  }
  if (lower.includes('timeout') || lower.includes('timed out')) {
    return '请求超时，请检查网络后重试。'
  }
  if (lower.includes('jdbc') || lower.includes('sql ') || lower.includes('sqlstate') || lower.includes('constraint') || lower.includes('database')) {
    return '数据库暂时无法完成本次操作，请稍后重试或联系管理员。'
  }
  if (lower.includes('file not found') || lower.includes('no such file') || lower.includes('filenotfound')) {
    return '文件不存在或已被删除，请刷新页面后重试。'
  }
  if (lower.includes('not found') || text === '404') {
    return '当前访问的功能或资源不存在，请刷新页面后重试。'
  }
  if (lower.includes('json') || lower.includes('parse') || lower.includes('message not readable')) {
    return '提交的数据格式不正确，请检查表单内容后重新提交。'
  }
  if (/^[a-z\s:._-]+$/i.test(text) && !/[\u4e00-\u9fa5]/.test(text)) {
    return fallback
  }
  return text
}

/**
 * 将 HTTP 状态码转换为用户可理解提示。
 */
function httpStatusMessage(status: number) {
  if (status === 400) return '请求内容不正确，请检查输入后重试。'
  if (status === 401) return '当前登录已失效，请重新登录。'
  if (status === 403) return '您没有权限执行当前操作。'
  if (status === 404) return '当前访问的功能或资源不存在，请刷新页面后重试。'
  if (status === 405) return '当前功能请求方式不正确，请刷新页面后重试。'
  if (status === 409) return '当前数据状态已变化，请刷新页面后重试。'
  if (status === 413) return '上传或提交的内容过大，请减少内容后重试。'
  if (status === 415) return '上传或提交的文件格式不支持，请更换文件后重试。'
  if (status === 429) return '当前操作过于频繁，请稍后再试。'
  if ([502, 503, 504].includes(status)) return '外部服务或网络暂时不可用，请稍后重试。'
  if (status >= 500) return '系统暂时无法完成本次操作，请稍后重试或联系管理员。'
  return '请求无法处理，请检查当前页面内容后重试。'
}

/**
 * 执行 getData 方法。
 * 
 * 实现步骤：
 * 1. 读取当前页面状态或调用参数；
 * 2. 完成对应的校验、接口调用或数据转换；
 * 3. 更新页面状态或返回处理结果。
 */
export async function getData<T>(url: string, params?: Record<string, unknown>, config?: RatelRequestConfig): Promise<T> {
  /**
   * 常量 response：保存当前模块的页面状态、配置项、接口实例或计算结果。
   */
  const response = await http.get<ApiResponse<T>>(url, { ...config, params })
  return response.data.data
}

/**
 * 执行 postData 方法。
 * 
 * 实现步骤：
 * 1. 读取当前页面状态或调用参数；
 * 2. 完成对应的校验、接口调用或数据转换；
 * 3. 更新页面状态或返回处理结果。
 */
export async function postData<T>(url: string, data?: unknown, config?: RatelRequestConfig): Promise<T> {
  /**
   * 常量 response：保存当前模块的页面状态、配置项、接口实例或计算结果。
   */
  const response = await http.post<ApiResponse<T>>(url, data, config)
  return response.data.data
}

/**
 * 上传 multipart/form-data 表单。
 *
 * 实现步骤：
 * 1. 由调用方构造 FormData；
 * 2. 交给 axios 自动生成 multipart 边界；
 * 3. 按统一 ApiResponse 解析业务数据。
 *
 * 开发组织：ratel；开发人员：WenZhang；联系方式：18782945613。
 */
export async function postFormData<T>(url: string, data: FormData): Promise<T> {
  /**
   * 常量 response：保存当前模块的页面状态、配置项、接口实例或计算结果。
   */
  const response = await http.post<ApiResponse<T>>(url, data, {
    /**
     * 字段 headers：表示表单、筛选条件、接口数据或组件状态中的 headers 值。
     */
    headers: { 'Content-Type': 'multipart/form-data' }
  })
  return response.data.data
}

/**
 * 发送导出请求并返回浏览器下载所需的 Blob 和文件名。
 *
 * 实现步骤：
 * 1. 以 blob 响应类型请求后端导出接口；
 * 2. 从 Content-Disposition 中解析后端文件名；
 * 3. 将二进制内容交给页面触发下载。
 *
 * 开发组织：ratel；开发人员：WenZhang；联系方式：18782945613。
 */
export async function postBlob(url: string, data?: unknown): Promise<{ blob: Blob; filename?: string }> {
  /**
   * 常量 response：保存当前模块的页面状态、配置项、接口实例或计算结果。
   */
  const response = await http.post<Blob>(url, data, { responseType: 'blob' })
  /**
   * 常量 contentType：保存当前模块的页面状态、配置项、接口实例或计算结果。
   */
  const contentType = String(response.headers['content-type'] || '')
  if (contentType.includes('application/json')) {
    await handleBlobApiResponse(response.data)
  }
  return {
    /**
     * 字段 blob：表示表单、筛选条件、接口数据或组件状态中的 blob 值。
     */
    blob: response.data,
    /**
     * 字段 filename：表示表单、筛选条件、接口数据或组件状态中的 filename 值。
     */
    filename: filenameFromDisposition(response.headers['content-disposition'])
  }
}

/**
 * 发送二进制下载请求。
 *
 * 实现步骤：
 * 1. 使用 blob 响应类型请求后端下载接口；
 * 2. 如果后端返回统一 JSON 错误，则复用 blob 错误解析；
 * 3. 返回 Blob 和响应头中的文件名。
 *
 * 开发组织：ratel；开发人员：WenZhang；联系方式：18782945613。
 */
export async function getBlob(url: string, params?: Record<string, unknown>): Promise<{ blob: Blob; filename?: string }> {
  /**
   * 常量 response：保存当前模块的页面状态、配置项、接口实例或计算结果。
   */
  const response = await http.get<Blob>(url, { params, responseType: 'blob' })
  /**
   * 常量 contentType：保存当前模块的页面状态、配置项、接口实例或计算结果。
   */
  const contentType = String(response.headers['content-type'] || '')
  if (contentType.includes('application/json')) {
    await handleBlobApiResponse(response.data)
  }
  return {
    /**
     * 字段 blob：表示表单、筛选条件、接口数据或组件状态中的 blob 值。
     */
    blob: response.data,
    /**
     * 字段 filename：表示表单、筛选条件、接口数据或组件状态中的 filename 值。
     */
    filename: filenameFromDisposition(response.headers['content-disposition'])
  }
}

/**
 * 执行 putData 方法。
 * 
 * 实现步骤：
 * 1. 读取当前页面状态或调用参数；
 * 2. 完成对应的校验、接口调用或数据转换；
 * 3. 更新页面状态或返回处理结果。
 */
export async function putData<T>(url: string, data?: unknown): Promise<T> {
  /**
   * 常量 response：保存当前模块的页面状态、配置项、接口实例或计算结果。
   */
  const response = await http.put<ApiResponse<T>>(url, data)
  return response.data.data
}

/**
 * 执行 deleteData 方法。
 * 
 * 实现步骤：
 * 1. 读取当前页面状态或调用参数；
 * 2. 完成对应的校验、接口调用或数据转换；
 * 3. 更新页面状态或返回处理结果。
 */
export async function deleteData<T>(url: string): Promise<T> {
  /**
   * 常量 response：保存当前模块的页面状态、配置项、接口实例或计算结果。
   */
  const response = await http.delete<ApiResponse<T>>(url)
  return response.data.data
}

/**
 * 保存后端返回的二进制文件。
 */
export function saveBlob(blob: Blob, filename: string) {
  /**
   * 常量 url：保存当前模块的页面状态、配置项、接口实例或计算结果。
   */
  const url = URL.createObjectURL(blob)
  /**
   * 常量 link：保存当前模块的页面状态、配置项、接口实例或计算结果。
   */
  const link = document.createElement('a')
  link.href = url
  link.download = filename
  document.body.appendChild(link)
  link.click()
  document.body.removeChild(link)
  URL.revokeObjectURL(url)
}

/**
 * 解析下载响应头里的文件名。
 */
function filenameFromDisposition(disposition?: string): string | undefined {
  if (!disposition) {
    return undefined
  }
  /**
   * 常量 encodedMatch：保存当前模块的页面状态、配置项、接口实例或计算结果。
   */
  const encodedMatch = disposition.match(/filename\*=UTF-8''([^;]+)/i)
  if (encodedMatch?.[1]) {
    return decodeURIComponent(encodedMatch[1])
  }
  /**
   * 常量 plainMatch：保存当前模块的页面状态、配置项、接口实例或计算结果。
   */
  const plainMatch = disposition.match(/filename="?([^";]+)"?/i)
  return plainMatch?.[1]
}

/**
 * 解析 blob 模式下返回的业务错误。
 */
async function handleBlobApiResponse(blob: Blob) {
  /**
   * 常量 text：保存当前模块的页面状态、配置项、接口实例或计算结果。
   */
  const text = await blob.text()
  /**
   * 常量 payload：保存当前模块的页面状态、配置项、接口实例或计算结果。
   */
  const payload = JSON.parse(text) as ApiResponse<unknown>
  if (!payload || payload.success !== false) {
    return
  }
  if (isAuthFailureCode(payload.code)) {
    const message = readableMessage(payload.message || '', '当前登录过期，请重新登录')
    handleAuthFailure(message)
    throw createAuthExpiredError(message)
  } else if (payload.code === '201') {
    showApiResult('warning', readableMessage(payload.message || '', '请求警告'), undefined, undefined, payload.data)
  } else {
    showApiResult('failure', readableMessage(payload.message || '', '请求失败'), undefined, undefined, payload.data)
  }
  throw new Error(readableMessage(payload.message || '', '请求失败'))
}

/**
 * 判断当前请求是否需要展示成功提示。
 *
 * 实现步骤：
 * 1. 查询类 GET 请求不弹成功提示，避免列表刷新频繁打扰；
 * 2. 新增、修改、删除、导出、上传等写操作统一展示右下角成功通知；
 * 3. method 为空时按不展示处理，避免未知请求误提示。
 */
function shouldNotifySuccess(method?: string, url?: string) {
  if (shouldSuppressApiResult(url)) {
    return false
  }
  /** 归一化后的 HTTP 方法，用于统一判断写操作类型。 */
  const normalized = (method || '').toUpperCase()
  return ['POST', 'PUT', 'PATCH', 'DELETE'].includes(normalized)
}

/**
 * 展示统一接口调用结果。
 *
 * 实现步骤：
 * 1. 根据请求 URL 推断模块名称、页面名称和操作类型；
 * 2. 成功使用右下角通知，10 秒后自动消失；
 * 3. 失败/异常使用页面中间弹窗，需要用户点击关闭；
 * 4. 警告使用页面中间弹窗，10 秒倒计时或用户手工关闭。
 */
function showApiResult(
  type: 'success' | 'failure' | 'error' | 'warning',
  message: string,
  url?: string,
  method?: string,
  data?: unknown,
  config?: AxiosRequestConfig
) {
  if (isAuthFailureNoticeActive() || shouldSuppressApiResult(url) || isSilentErrorNotice(config)) {
    return
  }
  markApiResultNotice(type === 'failure' || type === 'error' ? 'error' : type)
  /** 结果提示标题，包含模块、页面和操作类型。 */
  const title = resultTitle(type, url, method)
  /** 结果提示正文，包含业务概述和带颜色的操作结果。 */
  const content = resultMessage(type, message, url, data)
  if (type === 'success') {
    ElNotification({
      title,
      message: content,
      type: 'success',
      position: 'bottom-right',
      duration: SUCCESS_NOTICE_DURATION,
      dangerouslyUseHTMLString: true
    })
    return
  }
  if (type === 'warning') {
    showCountdownDialog(title, content, 'warning')
    return
  }
  ElMessageBox.alert(content, title, {
    type: 'error',
    confirmButtonText: '关闭',
    center: true,
    dangerouslyUseHTMLString: true
  }).catch(() => undefined)
}

/** 判断当前请求是否由后台轮询静默处理错误。 */
function isSilentErrorNotice(config?: AxiosRequestConfig) {
  return Boolean((config as RatelRequestConfig | undefined)?.silentErrorNotice)
}

/**
 * 判断是否跳过统一接口结果提示。
 *
 * 实现步骤：ratel助手问答由聊天框自身展示状态，不再弹全局操作结果通知，避免把一次提问当成新增/操作成功提示。
 */
function shouldSuppressApiResult(url?: string) {
  const text = url || ''
  return text.includes('/api/ai/assistant') || text.includes('/api/ai/writing') || text.includes('/api/auth/menu-usages')
}

/**
 * 警告类弹窗倒计时关闭。
 *
 * 实现步骤：
 * 1. 打开居中警告弹窗；
 * 2. 启动 10 秒定时器；
 * 3. 到期后主动关闭弹窗，用户提前点击关闭时清理定时器。
 */
function showCountdownDialog(title: string, message: string, type: 'warning') {
  /** 警告弹窗自动关闭定时器，用户提前关闭时会被清理。 */
  const timer = window.setTimeout(() => ElMessageBox.close(), WARNING_NOTICE_DURATION)
  ElMessageBox.alert(message, title, {
    type,
    confirmButtonText: '关闭',
    center: true,
    dangerouslyUseHTMLString: true,
    callback: () => window.clearTimeout(timer)
  }).catch(() => window.clearTimeout(timer))
}

/**
 * 拼接统一结果提示标题。
 *
 * 实现步骤：主题由模块名称、页面名称、操作类型三段组成，例如“业务管理/采购管理/新增采购单”。
 */
function resultTitle(type: 'success' | 'failure' | 'error' | 'warning', url?: string, method?: string) {
  /** 当前操作名称，异常和警告优先显示固定动作，成功时根据 URL 推断业务动作。 */
  const action = type === 'error' ? '接口异常' : type === 'failure' ? '操作失败' : type === 'warning' ? '操作警告' : inferActionName(url, method)
  return `${inferModuleName(url)}/${inferPageName(url)}/${action}`
}

/**
 * 拼接统一结果提示正文。
 *
 * 实现步骤：
 * 1. 第一行只展示操作内容概述本身，不再输出“操作内容概述：”标签；
 * 2. 第二行展示操作结果，并按结果类型给“成功/失败/异常/警告”字样着色；
 * 3. 所有后端消息和业务摘要先做 HTML 转义，再交给 Element Plus 渲染。
 */
function resultMessage(type: 'success' | 'failure' | 'error' | 'warning', message: string, url?: string, data?: unknown) {
  /** 操作结果中文文本，用于在提示正文第二行高亮展示。 */
  const resultText = type === 'success' ? '成功' : type === 'warning' ? '警告' : type === 'failure' ? '失败' : '异常'
  return [
    `<div>${escapeHtml(operationSummary(url, message, data))}</div>`,
    `<div class="ratel-api-result-line">操作结果：<span style="color: ${resultColor(type)}; font-weight: 700;">${resultText}</span></div>`
  ].join('')
}

/**
 * 根据接口调用结果返回提示文字颜色。
 *
 * 实现步骤：
 * 1. 成功使用绿色，和右下角成功通知保持一致；
 * 2. 失败和异常使用红色，强调需要用户处理；
 * 3. 警告使用黄色，表示可以关注但不是系统崩溃。
 */
function resultColor(type: 'success' | 'failure' | 'error' | 'warning') {
  if (type === 'success') {
    return '#16a34a'
  }
  if (type === 'warning') {
    return '#d97706'
  }
  return '#dc2626'
}

/**
 * 转义后端提示和业务摘要中的 HTML 特殊字符。
 *
 * 实现步骤：
 * 1. 将任意输入转成字符串；
 * 2. 替换可能影响 HTML 结构的特殊字符；
 * 3. 返回可安全插入提示弹窗的文本。
 */
function escapeHtml(value: unknown) {
  return String(value ?? '')
    .replaceAll('&', '&amp;')
    .replaceAll('<', '&lt;')
    .replaceAll('>', '&gt;')
    .replaceAll('"', '&quot;')
    .replaceAll("'", '&#39;')
}

/**
 * 根据接口 URL 推断模块名称。
 */
function inferModuleName(url?: string) {
  /** 待推断的接口地址文本，空地址按空串处理。 */
  const text = url || ''
  if (text.includes('/purchase-orders') || text.includes('/shipments') || text.includes('/inventory-ledgers') || text.includes('/ar-ap') || text.includes('/cashier')) {
    return '业务管理'
  }
  if (text.includes('/finance') || text.includes('/vouchers') || text.includes('/subjects') || text.includes('/accounting-periods')) {
    return '财务管理'
  }
  if (text.includes('/auth') || text.includes('/menus') || text.includes('/roles')) {
    return '系统管理'
  }
  if (text.includes('/dictionaries')) {
    return '基础信息'
  }
  if (text.includes('/ai') || text.includes('/search')) {
    return '智能检索'
  }
  return '系统'
}

/**
 * 根据接口 URL 推断页面名称。
 */
function inferPageName(url?: string) {
  /** 待推断的接口地址文本，供页面名称规则逐项匹配。 */
  const text = url || ''
  if (text.includes('/purchase-orders')) return '采购管理'
  if (text.includes('/shipments')) return '物流管理'
  if (text.includes('/inventory-ledgers')) return '库存台账'
  if (text.includes('/ar-ap')) return '应收应付'
  if (text.includes('/cashier')) return '出纳管理'
  if (text.includes('/vouchers')) return '凭证记账'
  if (text.includes('/subjects')) return '会计科目'
  if (text.includes('/accounting-periods')) return '会计期间'
  if (text.includes('/dictionaries')) return '字典管理'
  if (text.includes('/users')) return '人员管理'
  if (text.includes('/roles')) return '角色管理'
  if (text.includes('/menus')) return '菜单管理'
  if (text.includes('/attachments')) return '附件管理'
  if (text.includes('/ai')) return 'ratel助手'
  if (text.includes('/search')) return '智能检索'
  return '当前页面'
}

/**
 * 根据接口 URL 推断操作名称。
 */
function inferActionName(url?: string, method?: string) {
  /** 待推断的接口地址文本，供状态、导出、附件等特殊动作优先匹配。 */
  const text = url || ''
  /** 归一化后的 HTTP 方法，供新增、修改、删除动作兜底判断。 */
  const normalizedMethod = (method || '').toUpperCase()
  if (text.includes('/status/')) return '状态变更'
  if (text.includes('/batch-delete')) return '批量删除'
  if (text.includes('/export')) return '导出'
  if (text.includes('/settle')) return '收付款登记'
  if (text.includes('/post')) return '过账'
  if (text.includes('/void')) return '作废'
  if (text.includes('/login')) return '登录'
  if (text.includes('/attachments') && text.includes('/upload')) return '上传附件'
  if (text.includes('/attachments')) return '附件操作'
  if (text.includes('/knowledge/rebuild')) return '重建索引'
  if (normalizedMethod === 'POST') return `新增${inferBusinessObjectName(url)}`
  if (normalizedMethod === 'PUT' || normalizedMethod === 'PATCH') return `修改${inferBusinessObjectName(url)}`
  if (normalizedMethod === 'DELETE') return `删除${inferBusinessObjectName(url)}`
  return '保存'
}

/**
 * 根据接口 URL 推断业务对象名称。
 */
function inferBusinessObjectName(url?: string) {
  /** 由 URL 推断出的页面名称，作为业务对象名称的来源。 */
  const page = inferPageName(url)
  return page === '当前页面' ? '数据' : page.replace('管理', '')
}

/**
 * 生成操作内容概述。
 *
 * 实现步骤：
 * 1. 后端返回了明确业务消息时优先使用；
 * 2. 默认“操作成功”这类泛化消息会结合返回数据的单号、编码、名称补充上下文；
 * 3. 返回数据为空时退回页面名称，保证提示语结构完整。
 */
function operationSummary(url: string | undefined, message: string, data: unknown) {
  if (message && message !== '操作成功') {
    return message
  }
  /** 接口返回的业务对象，用于从单号、编码、名称等字段提取摘要。 */
  const record = data && typeof data === 'object' ? data as Record<string, unknown> : undefined
  /** 当前操作对象的关键标识，优先取业务单号，其次取编码或名称。 */
  const identifier = firstText(
    record?.orderNo,
    record?.shipmentNo,
    record?.movementNo,
    record?.billNo,
    record?.transactionNo,
    record?.voucherNo,
    record?.code,
    record?.username,
    record?.name,
    record?.displayName
  )
  /** 当前接口对应的业务对象名称，用于生成自然语言提示。 */
  const objectName = inferBusinessObjectName(url)
  return identifier ? `您操作了一条编号/名称为 ${identifier} 的${objectName}记录。` : `您完成了一次${inferPageName(url)}操作。`
}

/**
 * 返回第一个非空文本。
 */
function firstText(...values: unknown[]) {
  for (const value of values) {
    if (value !== undefined && value !== null && String(value).trim()) {
      return String(value).trim()
    }
  }
  return ''
}
