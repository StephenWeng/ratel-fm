import {
  createAuthExpiredError,
  deleteData,
  ensureAuthRequestAllowed,
  getBlob,
  getData,
  handleAuthFailure,
  isAuthFailureCode,
  postBlob,
  postData,
  postFormData,
  putData
} from './http'
import { createSseParser } from '@/utils/sse'
import type {
  AttachmentBizType,
  AttachmentView,
  AccountingPeriodStatus,
  AccountingPeriodView,
  AccountingSourceType,
  AccountingSourceView,
  AutoVoucherResult,
  DashboardOverview,
  AiAssistantResponse,
  AiAssistantContext,
  AiComponentStatusResponse,
  ArApPaymentStatsView,
  ArApSettlementView,
  ArApView,
  BasicDictionaryView,
  BusinessOperationLogPage,
  CashierTransactionStatus,
  CashierTransactionType,
  CashierTransactionView,
  ExchangeRateView,
  FinancialStatement,
  MenuCodeView,
  InventoryMaterialStockView,
  InventoryStockView,
  InventoryView,
  KnowledgeRebuildResponse,
  LocalKnowledgeDocumentView,
  LoginResponse,
  MenuUsageView,
  MenuView,
  OperationLogPage,
  PeriodCloseCheckView,
  PurchaseOrderView,
  PurchaseStatus,
  RoleView,
  SearchResponse,
  ShipmentStatus,
  ShipmentOperationLogView,
  ShipmentView,
  SubjectView,
  SystemStatusView,
  TrialBalanceRow,
  UserView,
  VoucherStatus,
  VoucherImportResult,
  VoucherSourceDetail,
  VoucherView,
  WorkflowConfigView,
  WorkflowDefinitionView,
  WorkflowInstanceDetailView,
  WorkflowItemView,
  WorkflowStatus
} from '@/types/api'

/**
 * AiAssistantStreamHandlers 类型定义，用于约束 ratel助手流式回调。
 */
export interface AiAssistantStreamHandlers {
  /**
   * 字段 signal：表示前端取消流式请求的 AbortSignal。
   */
  signal?: AbortSignal
  /**
   * 字段 onMeta：接收后端返回的引用、建议、模型等元数据。
   */
  onMeta?: (response: AiAssistantResponse) => void
  /**
   * 字段 onDelta：接收模型回答增量文本。
   */
  onDelta?: (content: string) => void
  /**
   * 字段 onDone：接收完整响应元数据。
   */
  onDone?: (response: AiAssistantResponse) => void
  /**
   * 字段 onError：接收服务端流内错误。
   */
  onError?: (message: string) => void
}

/**
 * BusinessAgentRequest 类型定义，用于约束业务 Agent 的请求参数。
 */
export interface BusinessAgentRequest {
  /**
   * 字段 question：自然语言问题或分析目标。
   */
  question?: string
  /**
   * 字段 stage：Agent 执行阶段。
   */
  stage?: 'readOnly' | 'draft' | 'controlled' | 'multiStep' | string
  /**
   * 字段 modules：限定业务模块；为空时后端按问题和权限自动选择。
   */
  modules?: string[]
  /**
   * 字段 agentTypes：限定 Agent 能力；为空时后端按问题自动选择。
   */
  agentTypes?: string[]
  /**
   * 字段 limit：每类结果最多返回的证据条数。
   */
  limit?: number
}

/**
 * BusinessAgentEvidence 类型定义，用于承载 Agent 引用的业务证据。
 */
export interface BusinessAgentEvidence {
  type: string
  id?: number
  no: string
  title: string
  status: string
  amount: string
  date: string
  route: string
}

/**
 * BusinessAgentModuleResult 类型定义，用于承载单个业务模块的 Agent 分析结果。
 */
export interface BusinessAgentModuleResult {
  module: string
  moduleName: string
  authorized: boolean
  summary: string
  findings: string[]
  risks: string[]
  suggestions: string[]
  evidences: BusinessAgentEvidence[]
}

/**
 * BusinessAgentCapabilityResult 类型定义，用于承载单个 Agent 能力分析结果。
 */
export interface BusinessAgentCapabilityResult {
  agentType: string
  agentName: string
  available: boolean
  summary: string
  findings: string[]
  risks: string[]
  suggestions: string[]
  drafts: string[]
  evidences: BusinessAgentEvidence[]
}

/**
 * BusinessAgentAction 类型定义，用于承载草稿动作或受控执行计划。
 */
export interface BusinessAgentAction {
  step: string
  stage: string
  module: string
  actionType: string
  writeOperation: boolean
  requiresUserConfirm: boolean
  executable: boolean
  title: string
  description: string
  preconditions: string[]
  blockedReasons: string[]
}

/**
 * BusinessAgentSelfCheck 类型定义，用于承载关键 Agent 自检结果。
 */
export interface BusinessAgentSelfCheck {
  item: string
  passed: boolean
  level: string
  detail: string
}

/**
 * BusinessAgentResponse 类型定义，用于承载业务 Agent 总响应。
 */
export interface BusinessAgentResponse {
  question: string
  stage: string
  scope: string
  summary: string
  modules: BusinessAgentModuleResult[]
  capabilities: BusinessAgentCapabilityResult[]
  actions: BusinessAgentAction[]
  selfChecks: BusinessAgentSelfCheck[]
  risks: string[]
  suggestions: string[]
  guardrails: string[]
}

/**
 * agentStatusCache 缓存最近一次 AI 状态，避免同一页面短时间内反复请求状态接口。
 */
let agentStatusCache: { value: AiComponentStatusResponse; expiresAt: number } | undefined

/**
 * 读取业务 Agent 启用状态。
 *
 * 实现步骤：
 * 1. 优先读取短缓存，降低状态接口调用频率；
 * 2. 缓存失效时请求 `/api/ai/status`；
 * 3. 返回包含 `agentEnabled` 的 AI 组件状态，供页面隐藏 Agent 入口或阻止调用。
 */
async function agentAwareStatus() {
  const now = Date.now()
  if (agentStatusCache && agentStatusCache.expiresAt > now) {
    return agentStatusCache.value
  }
  const status = await getData<AiComponentStatusResponse>('/api/ai/status', undefined, { silentErrorNotice: true })
  agentStatusCache = {
    value: status,
    expiresAt: now + 30_000
  }
  return status
}

/**
 * 运行业务 Agent。
 *
 * 实现步骤：
 * 1. 先读取 AI 状态中的 `agentEnabled`；
 * 2. 如果业务 Agent 未启用，则在前端阻断，不调用 `/api/agent/business`；
 * 3. 启用时再提交 Agent 请求，确保关闭配置在展示和执行流程中都不出现 Agent。
 */
async function runBusinessAgent(payload: BusinessAgentRequest) {
  const status = await agentAwareStatus()
  if (!status.agentEnabled) {
    throw new Error('业务 Agent 未启用')
  }
  return postData<BusinessAgentResponse>('/api/agent/business', payload, { timeout: 180000 })
}

/**
 * LoginPayload 类型定义，用于约束页面状态、接口入参或接口返回数据结构。
 */
export interface LoginPayload {
  /**
   * 字段 organizationCode：登录选择的所属公司字典编码，后端按该账套校验账号和身份证。
   */
  organizationCode: string
  /**
   * 字段 username：表示表单、筛选条件、接口数据或组件状态中的 username 值。
   */
  username: string
  /**
   * 字段 password：表示表单、筛选条件、接口数据或组件状态中的 password 值。
   */
  password: string
  /**
   * 字段 terminalType：表示表单、筛选条件、接口数据或组件状态中的 terminalType 值。
   */
  terminalType: 'PC' | 'APP'
  /**
   * 字段 terminalIdentifier：表示表单、筛选条件、接口数据或组件状态中的 terminalIdentifier 值。
   */
  terminalIdentifier?: string
  /**
   * 字段 force：表示表单、筛选条件、接口数据或组件状态中的 force 值。
   */
  force?: boolean
}

/**
 * WorkflowCenterQueryParams 类型定义，用于审批中心三个列表的统一搜索条件。
 */
export interface WorkflowCenterQueryParams extends Record<string, unknown> {
  businessModuleCode?: string
  title?: string
  projectCode?: string
  startedStart?: string
  startedEnd?: string
  starterName?: string
  status?: WorkflowStatus
}

/**
 * 常量 api：保存当前模块的页面状态、配置项、接口实例或计算结果。
 */
export const api = {
  /**
   * 字段 systemStatus：表示表单、筛选条件、接口数据或组件状态中的 systemStatus 值。
   */
  systemStatus: (params?: { latitude?: number; longitude?: number; accuracy?: number; locationSource?: string; locationName?: string }) =>
    getData<SystemStatusView>('/api/system/status', params, { silentErrorNotice: true }),
  /**
   * 字段 login：表示表单、筛选条件、接口数据或组件状态中的 login 值。
   */
  login: (payload: LoginPayload) => postData<LoginResponse>('/api/auth/login', payload),
  /**
   * 字段 loginCompanies：登录前读取启用所属公司账套列表，只返回 ORGANIZATION 字典下的公司节点。
   */
  loginCompanies: () => getData<BasicDictionaryView[]>('/api/auth/companies'),
  /**
   * 字段 me：表示表单、筛选条件、接口数据或组件状态中的 me 值。
   */
  me: () => getData<UserView>('/api/auth/me'),
  /**
   * 字段 myMenuCodes：表示表单、筛选条件、接口数据或组件状态中的 myMenuCodes 值。
   */
  myMenuCodes: () => getData<MenuCodeView>('/api/auth/menu-codes'),
  /**
   * 字段 myMenus：表示表单、筛选条件、接口数据或组件状态中的 myMenus 值。
   */
  myMenus: () => getData<MenuView[]>('/api/auth/menus'),
  /**
   * 字段 myMenuUsages：读取当前用户常用功能。
   */
  myMenuUsages: (limit = 10) => getData<MenuUsageView[]>('/api/auth/menu-usages', { limit }),
  /**
   * 字段 recordMenuUsage：记录当前用户进入一次功能菜单。
   */
  recordMenuUsage: (payload: { menuCode: string; menuName: string; routePath: string }) =>
    postData<MenuUsageView>('/api/auth/menu-usages', payload),
  /**
   * 字段 updateProfile：表示表单、筛选条件、接口数据或组件状态中的 updateProfile 值。
   */
  updateProfile: (payload: unknown) => putData<UserView>('/api/auth/profile', payload),
  /**
   * 字段 changeMyPassword：表示表单、筛选条件、接口数据或组件状态中的 changeMyPassword 值。
   */
  changeMyPassword: (payload: unknown) => putData<void>('/api/auth/password', payload),
  /**
   * 字段 uploadMyAvatar：表示表单、筛选条件、接口数据或组件状态中的 uploadMyAvatar 值。
   */
  uploadMyAvatar: (payload: FormData) => postData<UserView>('/api/auth/avatar', payload),
  /**
   * 字段 logout：表示表单、筛选条件、接口数据或组件状态中的 logout 值。
   */
  logout: () => postData<void>('/api/auth/logout'),
  /**
   * 字段 attachments：表示表单、筛选条件、接口数据或组件状态中的 attachments 值。
   */
  attachments: (businessType: AttachmentBizType, businessId: number) =>
    getData<AttachmentView[]>(`/api/attachments/${businessType}/${businessId}`),
  /**
   * 字段 uploadAttachments：表示表单、筛选条件、接口数据或组件状态中的 uploadAttachments 值。
   */
  uploadAttachments: (businessType: AttachmentBizType, businessId: number, payload: FormData) =>
    postFormData<AttachmentView[]>(`/api/attachments/${businessType}/${businessId}`, payload),
  /**
   * 字段 renameAttachment：表示表单、筛选条件、接口数据或组件状态中的 renameAttachment 值。
   */
  renameAttachment: (businessType: AttachmentBizType, businessId: number, attachmentId: number, displayName: string) =>
    putData<AttachmentView>(`/api/attachments/${businessType}/${businessId}/${attachmentId}`, { displayName }),
  /**
   * 字段 deleteAttachment：表示表单、筛选条件、接口数据或组件状态中的 deleteAttachment 值。
   */
  deleteAttachment: (businessType: AttachmentBizType, businessId: number, attachmentId: number) =>
    deleteData<void>(`/api/attachments/${businessType}/${businessId}/${attachmentId}`),
  /**
   * 字段 previewAttachment：表示表单、筛选条件、接口数据或组件状态中的 previewAttachment 值。
   */
  previewAttachment: (businessType: AttachmentBizType, businessId: number, attachmentId: number) =>
    getBlob(`/api/attachments/${businessType}/${businessId}/${attachmentId}/preview`),
  /**
   * 字段 downloadAttachment：表示表单、筛选条件、接口数据或组件状态中的 downloadAttachment 值。
   */
  downloadAttachment: (businessType: AttachmentBizType, businessId: number, attachmentId: number) =>
    getBlob(`/api/attachments/${businessType}/${businessId}/${attachmentId}/download`),
  /**
   * 字段 users：表示表单、筛选条件、接口数据或组件状态中的 users 值。
   */
  users: (params?: {
    /**
     * 字段 username：表示表单、筛选条件、接口数据或组件状态中的 username 值。
     */
    username?: string
    /**
     * 字段 realName：表示表单、筛选条件、接口数据或组件状态中的 realName 值。
     */
    realName?: string
    /**
     * 字段 identityNo：表示表单、筛选条件、接口数据或组件状态中的 identityNo 值。
     */
    identityNo?: string
    /**
     * 字段 phone：表示表单、筛选条件、接口数据或组件状态中的 phone 值。
     */
    phone?: string
    /**
     * 字段 email：表示表单、筛选条件、接口数据或组件状态中的 email 值。
     */
    email?: string
    /**
     * 字段 department：表示表单、筛选条件、接口数据或组件状态中的 department 值。
     */
    department?: string
    /**
     * 字段 organizationCode：表示表单、筛选条件、接口数据或组件状态中的 organizationCode 值。
     */
    organizationCode?: string
    /**
     * 字段 position：表示表单、筛选条件、接口数据或组件状态中的 position 值。
     */
    position?: string
    /**
     * 字段 enabled：表示表单、筛选条件、接口数据或组件状态中的 enabled 值。
     */
    enabled?: boolean
  }) => getData<UserView[]>('/api/users', params),
  /**
   * 字段 createUser：表示表单、筛选条件、接口数据或组件状态中的 createUser 值。
   */
  createUser: (payload: unknown) => postData<UserView>('/api/users', payload),
  /**
   * 字段 updateUser：表示表单、筛选条件、接口数据或组件状态中的 updateUser 值。
   */
  updateUser: (id: number, payload: unknown) => putData<UserView>(`/api/users/${id}`, payload),
  /**
   * 字段 changeUserPassword：表示表单、筛选条件、接口数据或组件状态中的 changeUserPassword 值。
   */
  changeUserPassword: (id: number, payload: unknown) => putData<void>(`/api/users/${id}/password`, payload),
  /**
   * 字段 uploadUserAvatar：表示表单、筛选条件、接口数据或组件状态中的 uploadUserAvatar 值。
   */
  uploadUserAvatar: (id: number, payload: FormData) => postData<UserView>(`/api/users/${id}/avatar`, payload),
  /**
   * 字段 deleteUser：表示表单、筛选条件、接口数据或组件状态中的 deleteUser 值。
   */
  deleteUser: (id: number) => deleteData<void>(`/api/users/${id}`),
  /**
   * 字段 batchDeleteUsers：表示表单、筛选条件、接口数据或组件状态中的 batchDeleteUsers 值。
   */
  batchDeleteUsers: (ids: number[]) => postData<void>('/api/users/batch-delete', { ids }),
  /**
   * 字段 roles：表示表单、筛选条件、接口数据或组件状态中的 roles 值。
   */
  roles: () => getData<RoleView[]>('/api/roles'),
  /**
   * 字段 menus：表示表单、筛选条件、接口数据或组件状态中的 menus 值。
   */
  menus: () => getData<MenuView[]>('/api/menus'),
  /**
   * 字段 allMenus：表示表单、筛选条件、接口数据或组件状态中的 allMenus 值。
   */
  allMenus: (params?: {
    /**
     * 字段 code：表示表单、筛选条件、接口数据或组件状态中的 code 值。
     */
    code?: string
    /**
     * 字段 name：表示表单、筛选条件、接口数据或组件状态中的 name 值。
     */
    name?: string
    /**
     * 字段 type：表示表单、筛选条件、接口数据或组件状态中的 type 值。
     */
    type?: 'MODULE' | 'PAGE' | 'BUTTON'
    /**
     * 字段 routePath：表示表单、筛选条件、接口数据或组件状态中的 routePath 值。
     */
    routePath?: string
    /**
     * 字段 permissionCode：表示表单、筛选条件、接口数据或组件状态中的 permissionCode 值。
     */
    permissionCode?: string
    /**
     * 字段 enabled：表示表单、筛选条件、接口数据或组件状态中的 enabled 值。
     */
    enabled?: boolean
  }) => getData<MenuView[]>('/api/menus/all', params),
  /**
   * 字段 saveMenu：表示表单、筛选条件、接口数据或组件状态中的 saveMenu 值。
   */
  saveMenu: (payload: unknown) => postData<MenuView>('/api/menus', payload),
  /**
   * 字段 deleteMenu：表示表单、筛选条件、接口数据或组件状态中的 deleteMenu 值。
   */
  deleteMenu: (id: number) => deleteData<void>(`/api/menus/${id}`),
  /**
   * 字段 saveRole：表示表单、筛选条件、接口数据或组件状态中的 saveRole 值。
   */
  saveRole: (payload: unknown) => postData<RoleView>('/api/roles', payload),
  /**
   * 字段 deleteRole：表示表单、筛选条件、接口数据或组件状态中的 deleteRole 值。
   */
  deleteRole: (id: number) => deleteData<void>(`/api/roles/${id}`),
  /**
   * 字段 dictionaries：表示表单、筛选条件、接口数据或组件状态中的 dictionaries 值。
   */
  dictionaries: () => getData<BasicDictionaryView[]>('/api/basic/dictionaries'),
  /**
   * 字段 dictionaryRoots：表示表单、筛选条件、接口数据或组件状态中的 dictionaryRoots 值。
   */
  dictionaryRoots: () => getData<BasicDictionaryView[]>('/api/basic/dictionaries/roots'),
  /**
   * 字段 dictionaryChildren：表示表单、筛选条件、接口数据或组件状态中的 dictionaryChildren 值。
   */
  dictionaryChildren: (parentId?: number) => getData<BasicDictionaryView[]>('/api/basic/dictionaries/children', { parentId }),
  /**
   * 字段 searchDictionaries：表示表单、筛选条件、接口数据或组件状态中的 searchDictionaries 值。
   */
  searchDictionaries: (params?: {
    /**
     * 字段 code：表示表单、筛选条件、接口数据或组件状态中的 code 值。
     */
    code?: string
    /**
     * 字段 name：表示表单、筛选条件、接口数据或组件状态中的 name 值。
     */
    name?: string
    /**
     * 字段 description：表示表单、筛选条件、接口数据或组件状态中的 description 值。
     */
    description?: string
    /**
     * 字段 enabled：表示表单、筛选条件、接口数据或组件状态中的 enabled 值。
     */
    enabled?: boolean
    /**
     * 字段 parentId：表示表单、筛选条件、接口数据或组件状态中的 parentId 值。
     */
    parentId?: number
  }) => getData<BasicDictionaryView[]>('/api/basic/dictionaries/search', params),
  /**
   * 字段 enabledDictionaryChildren：表示表单、筛选条件、接口数据或组件状态中的 enabledDictionaryChildren 值。
   */
  enabledDictionaryChildren: (parentCode: string) =>
    getData<BasicDictionaryView[]>('/api/basic/dictionaries/enabled-children', { parentCode }),
  /**
   * 字段 enabledDictionaryChildrenByParent：按父级字典 ID 懒加载启用子级，用于行政区划等大字典级联选择。
   */
  enabledDictionaryChildrenByParent: (parentId: number) =>
    getData<BasicDictionaryView[]>('/api/basic/dictionaries/enabled-children-by-parent', { parentId }),
  /**
   * 字段 enabledDictionaryTree：表示表单、筛选条件、接口数据或组件状态中的 enabledDictionaryTree 值。
   */
  enabledDictionaryTree: (rootCode: string) =>
    getData<BasicDictionaryView[]>('/api/basic/dictionaries/enabled-tree', { rootCode }),
  /**
   * 字段 exchangeRate：查询币种折人民币的最新公开参考汇率，并返回来源和汇率日期。
   */
  exchangeRate: (currencyCode: string) =>
    getData<ExchangeRateView>('/api/basic/dictionaries/exchange-rate', { currencyCode }),
  /**
   * 字段 createDictionary：表示表单、筛选条件、接口数据或组件状态中的 createDictionary 值。
   */
  createDictionary: (payload: unknown) => postData<BasicDictionaryView>('/api/basic/dictionaries', payload),
  /**
   * 字段 updateDictionary：表示表单、筛选条件、接口数据或组件状态中的 updateDictionary 值。
   */
  updateDictionary: (id: number, payload: unknown) => putData<BasicDictionaryView>(`/api/basic/dictionaries/${id}`, payload),
  /**
   * 字段 deleteDictionary：表示表单、筛选条件、接口数据或组件状态中的 deleteDictionary 值。
   */
  deleteDictionary: (id: number) => deleteData<void>(`/api/basic/dictionaries/${id}`),
  /**
   * 字段 subjects：表示表单、筛选条件、接口数据或组件状态中的 subjects 值。
   */
  subjects: (onlyEnabled = false, params?: {
    /**
     * 字段 code：表示表单、筛选条件、接口数据或组件状态中的 code 值。
     */
    code?: string
    /**
     * 字段 name：表示表单、筛选条件、接口数据或组件状态中的 name 值。
     */
    name?: string
    /**
     * 字段 category：表示表单、筛选条件、接口数据或组件状态中的 category 值。
     */
    category?: string
    /**
     * 字段 parentId：表示表单、筛选条件、接口数据或组件状态中的 parentId 值。
     */
    parentId?: number
    /**
     * 字段 enabled：表示表单、筛选条件、接口数据或组件状态中的 enabled 值。
     */
    enabled?: boolean
    /**
     * 字段 description：表示表单、筛选条件、接口数据或组件状态中的 description 值。
     */
    description?: string
  }) => getData<SubjectView[]>('/api/finance/subjects', { onlyEnabled, ...params }),
  /**
   * 字段 createSubject：表示表单、筛选条件、接口数据或组件状态中的 createSubject 值。
   */
  createSubject: (payload: unknown) => postData<SubjectView>('/api/finance/subjects', payload),
  /**
   * 字段 updateSubject：表示表单、筛选条件、接口数据或组件状态中的 updateSubject 值。
   */
  updateSubject: (id: number, payload: unknown) => putData<SubjectView>(`/api/finance/subjects/${id}`, payload),
  /**
   * 字段 deleteSubject：表示表单、筛选条件、接口数据或组件状态中的 deleteSubject 值。
   */
  deleteSubject: (id: number) => deleteData<void>(`/api/finance/subjects/${id}`),
  /**
   * 字段 batchDeleteSubjects：表示表单、筛选条件、接口数据或组件状态中的 batchDeleteSubjects 值。
   */
  batchDeleteSubjects: (ids: number[]) => postData<void>('/api/finance/subjects/batch-delete', { ids }),
  /**
   * 字段 vouchers：表示表单、筛选条件、接口数据或组件状态中的 vouchers 值。
   */
  vouchers: (params?: {
    /**
     * 字段 startDate：表示表单、筛选条件、接口数据或组件状态中的 startDate 值。
     */
    startDate?: string
    /**
     * 字段 endDate：表示表单、筛选条件、接口数据或组件状态中的 endDate 值。
     */
    endDate?: string
    /**
     * 字段 belongMonth：表示表单、筛选条件、接口数据或组件状态中的 belongMonth 值。
     */
    belongMonth?: string
    /**
     * 字段 projectCode：表示项目字典编码，用于按项目筛选凭证。
     */
    projectCode?: string
    /**
     * 字段 voucherNo：表示表单、筛选条件、接口数据或组件状态中的 voucherNo 值。
     */
    voucherNo?: string
    /**
     * 字段 summary：表示表单、筛选条件、接口数据或组件状态中的 summary 值。
     */
    summary?: string
    /**
     * 字段 sourceBizNo：表示表单、筛选条件、接口数据或组件状态中的 sourceBizNo 值。
     */
    sourceBizNo?: string
    /**
     * 字段 status：表示表单、筛选条件、接口数据或组件状态中的 status 值。
     */
    status?: VoucherStatus
    /**
     * 字段 createdBy：表示表单、筛选条件、接口数据或组件状态中的 createdBy 值。
     */
    createdBy?: string
  }) => getData<VoucherView[]>('/api/finance/vouchers', params),
  /**
   * 字段 voucher：表示表单、筛选条件、接口数据或组件状态中的 voucher 值。
   */
  voucher: (id: number) => getData<VoucherView>(`/api/finance/vouchers/${id}`),
  /**
   * 字段 voucherSource：按凭证主键反向查询来源模块和来源单据详情。
   */
  voucherSource: (id: number) => getData<VoucherSourceDetail>(`/api/finance/vouchers/${id}/source`),
  /**
   * 字段 voucherOperationLogs：表示表单、筛选条件、接口数据或组件状态中的 voucherOperationLogs 值。
   */
  voucherOperationLogs: (id: number, params?: OperationLogQueryParams) =>
    getData<BusinessOperationLogPage>(`/api/finance/vouchers/${id}/operation-logs`, params),
  /**
   * 字段 createVoucher：表示表单、筛选条件、接口数据或组件状态中的 createVoucher 值。
   */
  createVoucher: (payload: unknown) => postData<VoucherView>('/api/finance/vouchers', payload),
  /**
   * 字段 updateVoucher：表示表单、筛选条件、接口数据或组件状态中的 updateVoucher 值。
   */
  updateVoucher: (id: number, payload: unknown) => putData<VoucherView>(`/api/finance/vouchers/${id}`, payload),
  /**
   * 字段 importVoucher：上传图片或PDF识别为凭证分录草稿，识别结果不直接落库。
   */
  importVoucher: (payload: FormData) => postFormData<VoucherImportResult>('/api/finance/vouchers/import-recognize', payload),
  /**
   * 字段 postVoucher：表示表单、筛选条件、接口数据或组件状态中的 postVoucher 值。
   */
  postVoucher: (id: number) => postData<VoucherView>(`/api/finance/vouchers/${id}/post`),
  /**
   * 字段 voidVoucher：表示表单、筛选条件、接口数据或组件状态中的 voidVoucher 值。
   */
  voidVoucher: (id: number) => postData<VoucherView>(`/api/finance/vouchers/${id}/void`),
  /**
   * 字段 batchDeleteVouchers：表示表单、筛选条件、接口数据或组件状态中的 batchDeleteVouchers 值。
   */
  batchDeleteVouchers: (ids: number[]) => postData<void>('/api/finance/vouchers/batch-delete', { ids }),
  /**
   * 字段 exportVouchers：表示表单、筛选条件、接口数据或组件状态中的 exportVouchers 值。
   */
  exportVouchers: (payload: unknown) => postBlob('/api/finance/vouchers/export', payload),
  /**
   * 字段 accountingPeriods：查询会计期间列表，支持期间编码和状态筛选。
   */
  accountingPeriods: (params?: { periodCode?: string; status?: AccountingPeriodStatus }) =>
    getData<AccountingPeriodView[]>('/api/accounting-periods', params),
  /**
   * 字段 createAccountingPeriod：创建当前账套会计期间。
   */
  createAccountingPeriod: (payload: unknown) => postData<AccountingPeriodView>('/api/accounting-periods', payload),
  /**
   * 字段 accountingPeriodCloseCheck：执行月结前检查。
   */
  accountingPeriodCloseCheck: (periodCode: string) =>
    getData<PeriodCloseCheckView>(`/api/accounting-periods/${periodCode}/close-check`),
  /**
   * 字段 closeAccountingPeriod：关闭指定会计期间。
   */
  closeAccountingPeriod: (periodCode: string, payload?: unknown) =>
    postData<AccountingPeriodView>(`/api/accounting-periods/${periodCode}/close`, payload || {}),
  /**
   * 字段 reopenAccountingPeriod：反结账并打开指定会计期间。
   */
  reopenAccountingPeriod: (periodCode: string, payload?: unknown) =>
    postData<AccountingPeriodView>(`/api/accounting-periods/${periodCode}/reopen`, payload || {}),
  /**
   * 字段 cashierTransactions：查询出纳流水列表。
   */
  cashierTransactions: (params?: {
    startDate?: string
    endDate?: string
    transactionType?: CashierTransactionType
    status?: CashierTransactionStatus
    projectCode?: string
    partnerName?: string
    relatedBizNo?: string
  }) => getData<CashierTransactionView[]>('/api/cashier-transactions', params),
  /**
   * 字段 createCashierTransaction：新增出纳流水草稿。
   */
  createCashierTransaction: (payload: unknown) => postData<CashierTransactionView>('/api/cashier-transactions', payload),
  /**
   * 字段 confirmCashierTransaction：确认出纳流水。
   */
  confirmCashierTransaction: (id: number) => postData<CashierTransactionView>(`/api/cashier-transactions/${id}/confirm`),
  /**
   * 字段 cancelCashierTransaction：取消出纳流水。
   */
  cancelCashierTransaction: (id: number) => postData<CashierTransactionView>(`/api/cashier-transactions/${id}/cancel`),
  /**
   * 字段 batchDeleteCashierTransactions：批量删除出纳流水。
   */
  batchDeleteCashierTransactions: (ids: number[]) => postData<void>('/api/cashier-transactions/batch-delete', { ids }),
  /**
   * 字段 exportCashierTransactions：导出出纳流水。
   */
  exportCashierTransactions: (payload: unknown) => postBlob('/api/cashier-transactions/export', payload),
  /**
   * 字段 accountingSources：查询会计平台可制证业务来源，包含采购单、应收应付单、库存流水和出纳流水。
   */
  accountingSources: (sourceType: AccountingSourceType) =>
    getData<AccountingSourceView[]>('/api/finance/accounting-platform/sources', { sourceType }),
  /**
   * 字段 generateAutoVoucher：根据业务来源和借贷科目生成凭证草稿。
   */
  generateAutoVoucher: (payload: unknown) =>
    postData<AutoVoucherResult>('/api/finance/accounting-platform/auto-vouchers', payload),
  /**
   * 字段 trialBalance：表示表单、筛选条件、接口数据或组件状态中的 trialBalance 值。
   */
  trialBalance: (params?: { startDate?: string; endDate?: string }) =>
    getData<TrialBalanceRow[]>('/api/finance/reports/trial-balance', params),
  /**
   * 字段 purchaseOrders：表示表单、筛选条件、接口数据或组件状态中的 purchaseOrders 值。
   */
  purchaseOrders: (params?: {
    /**
     * 字段 startDate：表示表单、筛选条件、接口数据或组件状态中的 startDate 值。
     */
    startDate?: string
    /**
     * 字段 endDate：表示表单、筛选条件、接口数据或组件状态中的 endDate 值。
     */
    endDate?: string
    /**
     * 字段 orderNo：表示表单、筛选条件、接口数据或组件状态中的 orderNo 值。
     */
    orderNo?: string
    /**
     * 字段 supplierName：表示表单、筛选条件、接口数据或组件状态中的 supplierName 值。
     */
    supplierName?: string
    /**
     * 字段 projectCode：表示项目字典编码，用于按项目筛选采购单。
     */
    projectCode?: string
    /**
     * 字段 status：表示表单、筛选条件、接口数据或组件状态中的 status 值。
     */
    status?: PurchaseStatus
    /**
     * 字段 createdBy：表示表单、筛选条件、接口数据或组件状态中的 createdBy 值。
     */
    createdBy?: string
    /**
     * 字段 remark：表示表单、筛选条件、接口数据或组件状态中的 remark 值。
     */
    remark?: string
  }) => getData<PurchaseOrderView[]>('/api/purchase-orders', params),
  /**
   * 字段 purchaseOrder：表示表单、筛选条件、接口数据或组件状态中的 purchaseOrder 值。
   */
  purchaseOrder: (id: number) => getData<PurchaseOrderView>(`/api/purchase-orders/${id}`),
  /**
   * 字段 purchaseOrderOperationLogs：表示表单、筛选条件、接口数据或组件状态中的 purchaseOrderOperationLogs 值。
   */
  purchaseOrderOperationLogs: (id: number, params?: OperationLogQueryParams) =>
    getData<BusinessOperationLogPage>(`/api/purchase-orders/${id}/operation-logs`, params),
  /**
   * 字段 createPurchaseOrder：表示表单、筛选条件、接口数据或组件状态中的 createPurchaseOrder 值。
   */
  createPurchaseOrder: (payload: unknown) => postData<PurchaseOrderView>('/api/purchase-orders', payload),
  /**
   * 字段 updatePurchaseOrder：表示表单、筛选条件、接口数据或组件状态中的 updatePurchaseOrder 值。
   */
  updatePurchaseOrder: (id: number, payload: unknown) => putData<PurchaseOrderView>(`/api/purchase-orders/${id}`, payload),
  /**
   * 字段 changePurchaseStatus：表示表单、筛选条件、接口数据或组件状态中的 changePurchaseStatus 值。
   */
  changePurchaseStatus: (id: number, status: PurchaseStatus) =>
    postData<PurchaseOrderView>(`/api/purchase-orders/${id}/status/${status}`),
  /**
   * 字段 submitPurchaseApproval：提交采购单审批流程。
   */
  submitPurchaseApproval: (id: number, payload: { applyReason?: string }) =>
    postData<PurchaseOrderView>(`/api/purchase-orders/${id}/submit-approval`, payload),
  /**
   * 字段 startPurchase：审批同意后发起采购履约。
   */
  startPurchase: (id: number) => postData<PurchaseOrderView>(`/api/purchase-orders/${id}/start-purchase`),
  /**
   * 字段 receivePurchase：采购中状态确认已收货。
   */
  receivePurchase: (id: number) => postData<PurchaseOrderView>(`/api/purchase-orders/${id}/receive`),
  /**
   * 字段 cancelPurchase：取消采购并保存取消类型和原因。
   */
  cancelPurchase: (id: number, payload: { cancelType: string; cancelReason: string }) =>
    postData<PurchaseOrderView>(`/api/purchase-orders/${id}/cancel`, payload),
  /**
   * 字段 batchDeletePurchaseOrders：表示表单、筛选条件、接口数据或组件状态中的 batchDeletePurchaseOrders 值。
   */
  batchDeletePurchaseOrders: (ids: number[]) => postData<void>('/api/purchase-orders/batch-delete', { ids }),
  /**
   * 字段 exportPurchaseOrders：表示表单、筛选条件、接口数据或组件状态中的 exportPurchaseOrders 值。
   */
  exportPurchaseOrders: (payload: unknown) => postBlob('/api/purchase-orders/export', payload),
  /**
   * 字段 shipments：表示表单、筛选条件、接口数据或组件状态中的 shipments 值。
   */
  shipments: (params?: {
    /**
     * 字段 startDate：表示表单、筛选条件、接口数据或组件状态中的 startDate 值。
     */
    startDate?: string
    /**
     * 字段 endDate：表示表单、筛选条件、接口数据或组件状态中的 endDate 值。
     */
    endDate?: string
    /**
     * 字段 shipmentNo：表示表单、筛选条件、接口数据或组件状态中的 shipmentNo 值。
     */
    shipmentNo?: string
    /**
     * 字段 relatedOrderNo：表示表单、筛选条件、接口数据或组件状态中的 relatedOrderNo 值。
     */
    relatedOrderNo?: string
    /**
     * 字段 projectCode：表示项目字典编码，用于按项目筛选物流单。
     */
    projectCode?: string
    /**
     * 字段 carrierName：表示表单、筛选条件、接口数据或组件状态中的 carrierName 值。
     */
    carrierName?: string
    /**
     * 字段 trackingNo：表示表单、筛选条件、接口数据或组件状态中的 trackingNo 值。
     */
    trackingNo?: string
    /**
     * 字段 originDivisionCodes：表示表单、筛选条件、接口数据或组件状态中的 originDivisionCodes 值。
     */
    originDivisionCodes?: string
    /**
     * 字段 destinationDivisionCodes：表示表单、筛选条件、接口数据或组件状态中的 destinationDivisionCodes 值。
     */
    destinationDivisionCodes?: string
    /**
     * 字段 origin：表示表单、筛选条件、接口数据或组件状态中的 origin 值。
     */
    origin?: string
    /**
     * 字段 destination：表示表单、筛选条件、接口数据或组件状态中的 destination 值。
     */
    destination?: string
    /**
     * 字段 status：表示表单、筛选条件、接口数据或组件状态中的 status 值。
     */
    status?: ShipmentStatus
  }) => getData<ShipmentView[]>('/api/shipments', params),
  /**
   * 字段 createShipment：表示表单、筛选条件、接口数据或组件状态中的 createShipment 值。
   */
  createShipment: (payload: unknown) => postData<ShipmentView>('/api/shipments', payload),
  /**
   * 字段 updateShipment：表示表单、筛选条件、接口数据或组件状态中的 updateShipment 值。
   */
  updateShipment: (id: number, payload: unknown) => putData<ShipmentView>(`/api/shipments/${id}`, payload),
  /**
   * 字段 confirmShipmentStatus：表示表单、筛选条件、接口数据或组件状态中的 confirmShipmentStatus 值。
   */
  confirmShipmentStatus: (id: number, payload: unknown) => postData<ShipmentView>(`/api/shipments/${id}/status-confirm`, payload),
  /**
   * 字段 shipmentOperationLogs：表示表单、筛选条件、接口数据或组件状态中的 shipmentOperationLogs 值。
   */
  shipmentOperationLogs: async (id: number, params?: OperationLogQueryParams) => {
    /**
     * 常量 page：保存当前模块的页面状态、配置项、接口实例或计算结果。
     */
    const page = await getData<{ rows: ShipmentOperationLogView[]; total: number }>(`/api/shipments/${id}/operation-logs`, params)
    return {
      /**
       * 字段 rows：表示表单、筛选条件、接口数据或组件状态中的 rows 值。
       */
      rows: page.rows.map((item) => ({
        /**
         * 字段 id：表示表单、筛选条件、接口数据或组件状态中的 id 值。
         */
        id: item.id,
        /**
         * 字段 businessType：表示表单、筛选条件、接口数据或组件状态中的 businessType 值。
         */
        businessType: 'SHIPMENT',
        /**
         * 字段 businessId：表示表单、筛选条件、接口数据或组件状态中的 businessId 值。
         */
        businessId: id,
        /**
         * 字段 businessNo：表示表单、筛选条件、接口数据或组件状态中的 businessNo 值。
         */
        businessNo: item.shipmentNo,
        /**
         * 字段 businessTitle：表示表单、筛选条件、接口数据或组件状态中的 businessTitle 值。
         */
        businessTitle: item.shipmentNo,
        /**
         * 字段 action：表示表单、筛选条件、接口数据或组件状态中的 action 值。
         */
        action: item.fromStatus ? 'STATUS_CHANGE' : 'CREATE',
        /**
         * 字段 actionName：表示表单、筛选条件、接口数据或组件状态中的 actionName 值。
         */
        actionName: shipmentActionName(item),
        /**
         * 字段 detail：表示表单、筛选条件、接口数据或组件状态中的 detail 值。
         */
        detail: shipmentLogDetail(item),
        /**
         * 字段 fromState：表示表单、筛选条件、接口数据或组件状态中的 fromState 值。
         */
        fromState: shipmentStatusLabel(item.fromStatus),
        /**
         * 字段 toState：表示表单、筛选条件、接口数据或组件状态中的 toState 值。
         */
        toState: shipmentStatusLabel(item.toStatus),
        /**
         * 字段 snapshot：表示表单、筛选条件、接口数据或组件状态中的 snapshot 值。
         */
        snapshot: JSON.stringify({
          /**
           * 字段 id：表示表单、筛选条件、接口数据或组件状态中的 id 值。
           */
          id: item.id,
          /**
           * 字段 shipmentNo：表示表单、筛选条件、接口数据或组件状态中的 shipmentNo 值。
           */
          shipmentNo: item.shipmentNo,
          /**
           * 字段 relatedOrderNo：表示表单、筛选条件、接口数据或组件状态中的 relatedOrderNo 值。
           */
          relatedOrderNo: item.relatedOrderNo,
          /**
           * 字段 documentType：表示表单、筛选条件、接口数据或组件状态中的 documentType 值。
           */
          documentType: item.documentType,
          /**
           * 字段 transportMode：表示表单、筛选条件、接口数据或组件状态中的 transportMode 值。
           */
          transportMode: item.transportMode,
          /**
           * 字段 projectCode：表示项目字典编码，用于物流流水快照追溯。
           */
          projectCode: item.projectCode,
          /**
           * 字段 projectName：表示项目名称快照，用于物流流水快照展示。
           */
          projectName: item.projectName,
          /**
           * 字段 shippingOrganization：表示表单、筛选条件、接口数据或组件状态中的 shippingOrganization 值。
           */
          shippingOrganization: item.shippingOrganization,
          /**
           * 字段 receivingOrganization：表示表单、筛选条件、接口数据或组件状态中的 receivingOrganization 值。
           */
          receivingOrganization: item.receivingOrganization,
          /**
           * 字段 carrierName：表示表单、筛选条件、接口数据或组件状态中的 carrierName 值。
           */
          carrierName: item.carrierName,
          /**
           * 字段 trackingNo：表示表单、筛选条件、接口数据或组件状态中的 trackingNo 值。
           */
          trackingNo: item.trackingNo,
          /**
           * 字段 driverName：表示表单、筛选条件、接口数据或组件状态中的 driverName 值。
           */
          driverName: item.driverName,
          /**
           * 字段 driverPhone：表示表单、筛选条件、接口数据或组件状态中的 driverPhone 值。
           */
          driverPhone: item.driverPhone,
          /**
           * 字段 vehicleNo：表示表单、筛选条件、接口数据或组件状态中的 vehicleNo 值。
           */
          vehicleNo: item.vehicleNo,
          /**
           * 字段 originDivisionCode：表示表单、筛选条件、接口数据或组件状态中的 originDivisionCode 值。
           */
          originDivisionCode: item.originDivisionCode,
          /**
           * 字段 originDivisionName：表示表单、筛选条件、接口数据或组件状态中的 originDivisionName 值。
           */
          originDivisionName: item.originDivisionName,
          /**
           * 字段 destinationDivisionCode：表示表单、筛选条件、接口数据或组件状态中的 destinationDivisionCode 值。
           */
          destinationDivisionCode: item.destinationDivisionCode,
          /**
           * 字段 destinationDivisionName：表示表单、筛选条件、接口数据或组件状态中的 destinationDivisionName 值。
           */
          destinationDivisionName: item.destinationDivisionName,
          /**
           * 字段 origin：表示表单、筛选条件、接口数据或组件状态中的 origin 值。
           */
          origin: item.origin,
          /**
           * 字段 destination：表示表单、筛选条件、接口数据或组件状态中的 destination 值。
           */
          destination: item.destination,
          /**
           * 字段 plannedShipDate：表示表单、筛选条件、接口数据或组件状态中的 plannedShipDate 值。
           */
          plannedShipDate: item.plannedShipDate,
          /**
           * 字段 actualShipDate：表示表单、筛选条件、接口数据或组件状态中的 actualShipDate 值。
           */
          actualShipDate: item.actualShipDate,
          /**
           * 字段 deliveredDate：表示表单、筛选条件、接口数据或组件状态中的 deliveredDate 值。
           */
          deliveredDate: item.deliveredDate,
          /**
           * 字段 status：表示表单、筛选条件、接口数据或组件状态中的 status 值。
           */
          status: item.toStatus,
          /**
           * 字段 remark：表示表单、筛选条件、接口数据或组件状态中的 remark 值。
           */
          remark: item.remark,
          /**
           * 字段 operationRemark：表示表单、筛选条件、接口数据或组件状态中的 operationRemark 值。
           */
          operationRemark: item.operationRemark
        }),
        /**
         * 字段 operatorId：表示表单、筛选条件、接口数据或组件状态中的 operatorId 值。
         */
        operatorId: item.operatorId,
        /**
         * 字段 operatorUsername：表示表单、筛选条件、接口数据或组件状态中的 operatorUsername 值。
         */
        operatorUsername: item.operatorUsername,
        /**
         * 字段 operatorName：表示表单、筛选条件、接口数据或组件状态中的 operatorName 值。
         */
        operatorName: item.operatorName,
        /**
         * 字段 operationTime：表示表单、筛选条件、接口数据或组件状态中的 operationTime 值。
         */
        operationTime: item.operationTime
      })),
      /**
       * 字段 total：表示表单、筛选条件、接口数据或组件状态中的 total 值。
       */
      total: page.total
    } as BusinessOperationLogPage
  },
  /**
   * 字段 batchDeleteShipments：表示表单、筛选条件、接口数据或组件状态中的 batchDeleteShipments 值。
   */
  batchDeleteShipments: (ids: number[]) => postData<void>('/api/shipments/batch-delete', { ids }),
  /**
   * 字段 exportShipments：表示表单、筛选条件、接口数据或组件状态中的 exportShipments 值。
   */
  exportShipments: (payload: unknown) => postBlob('/api/shipments/export', payload),
  /**
   * 字段 overview：表示表单、筛选条件、接口数据或组件状态中的 overview 值。
   */
  overview: (silentErrorNotice = false) => getData<DashboardOverview>('/api/insights/overview', undefined, { silentErrorNotice }),
  /**
   * 字段 search：表示表单、筛选条件、接口数据或组件状态中的 search 值。
   */
  search: (keyword: string, mode = 'hybrid') => getData<SearchResponse>('/api/search', { keyword, mode }, { timeout: 60000 }),
  /**
   * 字段 rebuildKnowledge：表示表单、筛选条件、接口数据或组件状态中的 rebuildKnowledge 值。
   */
  rebuildKnowledge: () => postData<KnowledgeRebuildResponse>('/api/ai/knowledge/rebuild', undefined, { timeout: 600000 }),
  localKnowledgeDocuments: () => getData<LocalKnowledgeDocumentView[]>('/api/ai/local-knowledge/documents'),
  uploadLocalKnowledgeDocument: (payload: FormData) =>
    postFormData<LocalKnowledgeDocumentView>('/api/ai/local-knowledge/documents', payload),
  rebuildLocalKnowledgeDocument: (id: number) =>
    postData<LocalKnowledgeDocumentView>(`/api/ai/local-knowledge/documents/${id}/rebuild`, undefined, { timeout: 180000 }),
  deleteLocalKnowledgeDocument: (id: number) =>
    deleteData<void>(`/api/ai/local-knowledge/documents/${id}`),
  /**
   * 字段 aiStatus：读取大模型、向量库、知识索引、流式输出和业务 Agent 状态。
   */
  aiStatus: () => agentAwareStatus(),
  /**
   * 字段 businessAgentEnabled：读取业务 Agent 是否启用，用于页面隐藏入口和阻断调用。
   */
  businessAgentEnabled: async () => (await agentAwareStatus()).agentEnabled,
  /**
   * 字段 runBusinessAgent：运行业务 Agent；关闭时不调用后端 Agent 接口。
   */
  runBusinessAgent,
  /**
   * 字段 inventoryLedgers：表示表单、筛选条件、接口数据或组件状态中的 inventoryLedgers 值。
   */
  inventoryLedgers: (params?: {
    /**
     * 字段 startDate：表示表单、筛选条件、接口数据或组件状态中的 startDate 值。
     */
    startDate?: string
    /**
     * 字段 endDate：表示表单、筛选条件、接口数据或组件状态中的 endDate 值。
     */
    endDate?: string
    /**
     * 字段 movementNo：表示表单、筛选条件、接口数据或组件状态中的 movementNo 值。
     */
    movementNo?: string
    /**
     * 字段 movementType：表示表单、筛选条件、接口数据或组件状态中的 movementType 值。
     */
    movementType?: InventoryView['movementType']
    /**
     * 字段 projectCode：表示项目字典编码，用于按项目筛选库存流水。
     */
    projectCode?: string
    /**
     * 字段 itemName：表示表单、筛选条件、接口数据或组件状态中的 itemName 值。
     */
    itemName?: string
    /**
     * 字段 fromWarehouse：表示表单、筛选条件、接口数据或组件状态中的 fromWarehouse 值。
     */
    fromWarehouse?: string
    /**
     * 字段 toWarehouse：表示表单、筛选条件、接口数据或组件状态中的 toWarehouse 值。
     */
    toWarehouse?: string
    /**
     * 字段 relatedBizNo：表示表单、筛选条件、接口数据或组件状态中的 relatedBizNo 值。
     */
    relatedBizNo?: string
  }) => getData<InventoryView[]>('/api/inventory-ledgers', params),
  /**
   * 字段 inventoryStock：表示表单、筛选条件、接口数据或组件状态中的 inventoryStock 值。
   */
  inventoryStock: (params: { itemCode: string; warehouse: string; asOfDate?: string }) =>
    getData<InventoryStockView>('/api/inventory-ledgers/stock', params),
  /**
   * 字段 inventoryMaterialStock：表示表单、筛选条件、接口数据或组件状态中的 inventoryMaterialStock 值。
   */
  inventoryMaterialStock: () => getData<InventoryMaterialStockView[]>('/api/inventory-ledgers/material-stock'),
  /**
   * 字段 createInventoryLedger：表示表单、筛选条件、接口数据或组件状态中的 createInventoryLedger 值。
   */
  createInventoryLedger: (payload: unknown) => postData<InventoryView>('/api/inventory-ledgers', payload),
  /**
   * 字段 inventoryOperationLogs：表示表单、筛选条件、接口数据或组件状态中的 inventoryOperationLogs 值。
   */
  inventoryOperationLogs: (id: number, params?: OperationLogQueryParams) =>
    getData<BusinessOperationLogPage>(`/api/inventory-ledgers/${id}/operation-logs`, params),
  /**
   * 字段 batchDeleteInventoryLedgers：表示表单、筛选条件、接口数据或组件状态中的 batchDeleteInventoryLedgers 值。
   */
  batchDeleteInventoryLedgers: (ids: number[]) => postData<void>('/api/inventory-ledgers/batch-delete', { ids }),
  /**
   * 字段 exportInventoryLedgers：表示表单、筛选条件、接口数据或组件状态中的 exportInventoryLedgers 值。
   */
  exportInventoryLedgers: (payload: unknown) => postBlob('/api/inventory-ledgers/export', payload),
  /**
   * 字段 arApBills：表示表单、筛选条件、接口数据或组件状态中的 arApBills 值。
   */
  arApBills: (params?: {
    /**
     * 字段 startDate：表示表单、筛选条件、接口数据或组件状态中的 startDate 值。
     */
    startDate?: string
    /**
     * 字段 endDate：表示表单、筛选条件、接口数据或组件状态中的 endDate 值。
     */
    endDate?: string
    /**
     * 字段 billNo：表示表单、筛选条件、接口数据或组件状态中的 billNo 值。
     */
    billNo?: string
    /**
     * 字段 billType：表示表单、筛选条件、接口数据或组件状态中的 billType 值。
     */
    billType?: ArApView['billType']
    /**
     * 字段 partnerName：表示表单、筛选条件、接口数据或组件状态中的 partnerName 值。
     */
    partnerName?: string
    /**
     * 字段 projectCode：表示项目字典编码，用于按项目筛选应收应付单。
     */
    projectCode?: string
    /**
     * 字段 status：表示表单、筛选条件、接口数据或组件状态中的 status 值。
     */
    status?: ArApView['status']
    /**
     * 字段 paymentPlan：表示表单、筛选条件、接口数据或组件状态中的 paymentPlan 值。
     */
    paymentPlan?: string
  }) => getData<ArApView[]>('/api/ar-ap-bills', params),
  /**
   * 字段 createArApBill：表示表单、筛选条件、接口数据或组件状态中的 createArApBill 值。
   */
  createArApBill: (payload: unknown) => postData<ArApView>('/api/ar-ap-bills', payload),
  /**
   * 字段 arApPaymentStats：表示应收应付收付统计查询接口。
   */
  arApPaymentStats: (params?: { projectCode?: string; partnerName?: string }) =>
    getData<ArApPaymentStatsView>('/api/ar-ap-bills/payment-stats', params),
  /**
   * 字段 exportArApPaymentStats：表示按当前项目和客户/供应商条件导出收付统计。
   */
  exportArApPaymentStats: (payload: unknown) => postBlob('/api/ar-ap-bills/payment-stats/export', payload),
  /**
   * 字段 arApSettlements：查询应收应付核销流水。
   */
  arApSettlements: (id: number) => getData<ArApSettlementView[]>(`/api/ar-ap-bills/${id}/settlements`),
  /**
   * 字段 settleArApBill：新增应收应付收付核销。
   */
  settleArApBill: (id: number, payload: unknown) => postData<ArApView>(`/api/ar-ap-bills/${id}/settlements`, payload),
  /**
   * 字段 arApOperationLogs：表示表单、筛选条件、接口数据或组件状态中的 arApOperationLogs 值。
   */
  arApOperationLogs: (id: number, params?: OperationLogQueryParams) =>
    getData<BusinessOperationLogPage>(`/api/ar-ap-bills/${id}/operation-logs`, params),
  /**
   * 字段 batchDeleteArApBills：表示表单、筛选条件、接口数据或组件状态中的 batchDeleteArApBills 值。
   */
  batchDeleteArApBills: (ids: number[]) => postData<void>('/api/ar-ap-bills/batch-delete', { ids }),
  /**
   * 字段 exportArApBills：表示表单、筛选条件、接口数据或组件状态中的 exportArApBills 值。
   */
  exportArApBills: (payload: unknown) => postBlob('/api/ar-ap-bills/export', payload),
  /**
   * 字段 balanceSheet：表示表单、筛选条件、接口数据或组件状态中的 balanceSheet 值。
   */
  balanceSheet: (date?: string) => getData<FinancialStatement>('/api/finance/reports/balance-sheet', { date }),
  /**
   * 字段 incomeStatement：表示表单、筛选条件、接口数据或组件状态中的 incomeStatement 值。
   */
  incomeStatement: (date?: string) => getData<FinancialStatement>('/api/finance/reports/income-statement', { date }),
  /**
   * 字段 cashFlow：表示表单、筛选条件、接口数据或组件状态中的 cashFlow 值。
   */
  cashFlow: (date?: string) => getData<FinancialStatement>('/api/finance/reports/cash-flow', { date }),
  /**
   * 字段 askAssistant：表示表单、筛选条件、接口数据或组件状态中的 askAssistant 值。
   */
  askAssistant: (question: string, mode = 'hybrid', context?: AiAssistantContext) =>
    postData<AiAssistantResponse>('/api/ai/assistant', { question, mode, ...(context || {}) }, { timeout: 180000 }),
  /**
   * 字段 streamAssistant：使用 fetch POST + SSE 流式读取 ratel助手回答。
   */
  streamAssistant: async (question: string, mode = 'hybrid', context?: AiAssistantContext, handlers?: AiAssistantStreamHandlers) =>
    streamAssistant(question, mode, context, handlers),
  /**
   * 字段 workflowTodo：查询当前用户待办事宜。
   */
  workflowTodo: (params?: WorkflowCenterQueryParams) => getData<WorkflowItemView[]>('/api/workflows/center/todo', params),
  /**
   * 字段 workflowDone：查询当前用户已办事宜。
   */
  workflowDone: (params?: WorkflowCenterQueryParams) => getData<WorkflowItemView[]>('/api/workflows/center/done', params),
  /**
   * 字段 workflowStarted：查询当前用户发起事宜。
   */
  workflowStarted: (params?: WorkflowCenterQueryParams) => getData<WorkflowItemView[]>('/api/workflows/center/started', params),
  /**
   * 字段 workflowDetail：查询流程实例详情。
   */
  workflowDetail: (id: number) => getData<WorkflowInstanceDetailView>(`/api/workflows/instances/${id}`),
  /**
   * 字段 latestWorkflow：查询业务单据最近流程详情。
   */
  latestWorkflow: (businessType: string, businessId: number) =>
    getData<WorkflowInstanceDetailView | null>('/api/workflows/instances/latest', { businessType, businessId }),
  /**
   * 字段 approveWorkflowTask：审批待办任务。
   */
  approveWorkflowTask: (taskId: number, payload: { approved: boolean; comment: string }) =>
    postData<WorkflowItemView>(`/api/workflows/tasks/${taskId}/approve`, payload),
  /**
   * 字段 workflowDefinitions：查询流程定义列表。
   */
  workflowDefinitions: (params?: { name?: string; code?: string; enabled?: boolean }) =>
    getData<WorkflowDefinitionView[]>('/api/workflows/definitions', params),
  /**
   * 字段 enabledWorkflowDefinitions：查询启用流程定义。
   */
  enabledWorkflowDefinitions: () => getData<WorkflowDefinitionView[]>('/api/workflows/definitions/enabled'),
  /**
   * 字段 createWorkflowDefinition：新增流程定义。
   */
  createWorkflowDefinition: (payload: unknown) => postData<WorkflowDefinitionView>('/api/workflows/definitions', payload),
  /**
   * 字段 updateWorkflowDefinition：修改流程定义。
   */
  updateWorkflowDefinition: (id: number, payload: unknown) => putData<WorkflowDefinitionView>(`/api/workflows/definitions/${id}`, payload),
  /**
   * 字段 deleteWorkflowDefinition：删除流程定义。
   */
  deleteWorkflowDefinition: (id: number) => deleteData<void>(`/api/workflows/definitions/${id}`),
  /**
   * 字段 workflowConfigs：查询流程配置列表。
   */
  workflowConfigs: (params?: { businessModuleCode?: string; functionModuleCode?: string; enabled?: boolean }) =>
    getData<WorkflowConfigView[]>('/api/workflows/configs', params),
  /**
   * 字段 createWorkflowConfig：新增流程配置。
   */
  createWorkflowConfig: (payload: unknown) => postData<WorkflowConfigView>('/api/workflows/configs', payload),
  /**
   * 字段 updateWorkflowConfig：修改流程配置。
   */
  updateWorkflowConfig: (id: number, payload: unknown) => putData<WorkflowConfigView>(`/api/workflows/configs/${id}`, payload),
  /**
   * 字段 deleteWorkflowConfig：删除流程配置。
   */
  deleteWorkflowConfig: (id: number) => deleteData<void>(`/api/workflows/configs/${id}`),
  /**
   * 字段 operationLogs：表示表单、筛选条件、接口数据或组件状态中的 operationLogs 值。
   */
  operationLogs: (params: {
    /**
     * 字段 startTime：表示表单、筛选条件、接口数据或组件状态中的 startTime 值。
     */
    startTime?: string
    /**
     * 字段 endTime：表示表单、筛选条件、接口数据或组件状态中的 endTime 值。
     */
    endTime?: string
    /**
     * 字段 account：表示表单、筛选条件、接口数据或组件状态中的 account 值。
     */
    account?: string
    /**
     * 字段 identityNo：表示表单、筛选条件、接口数据或组件状态中的 identityNo 值。
     */
    identityNo?: string
    /**
     * 字段 contactPhone：表示表单、筛选条件、接口数据或组件状态中的 contactPhone 值。
     */
    contactPhone?: string
    /**
     * 字段 department：表示表单、筛选条件、接口数据或组件状态中的 department 值。
     */
    department?: string
    /**
     * 字段 terminalType：表示表单、筛选条件、接口数据或组件状态中的 terminalType 值。
     */
    terminalType?: 'PC' | 'APP' | ''
    /**
     * 字段 terminalIdentifier：表示表单、筛选条件、接口数据或组件状态中的 terminalIdentifier 值。
     */
    terminalIdentifier?: string
    /**
     * 字段 page：表示表单、筛选条件、接口数据或组件状态中的 page 值。
     */
    page?: number
    /**
     * 字段 size：表示表单、筛选条件、接口数据或组件状态中的 size 值。
     */
    size?: number
  }) => getData<OperationLogPage>('/api/audit/operation-logs', params)
}

/**
 * OperationLogQueryParams 类型定义，用于约束页面状态、接口入参或接口返回数据结构。
 */
export interface OperationLogQueryParams extends Record<string, unknown> {
  /**
   * 字段 startTime：表示表单、筛选条件、接口数据或组件状态中的 startTime 值。
   */
  startTime?: string
  /**
   * 字段 endTime：表示表单、筛选条件、接口数据或组件状态中的 endTime 值。
   */
  endTime?: string
  /**
   * 字段 page：表示表单、筛选条件、接口数据或组件状态中的 page 值。
   */
  page?: number
  /**
   * 字段 size：表示表单、筛选条件、接口数据或组件状态中的 size 值。
   */
  size?: number
}

/**
 * 执行 shipmentActionName 方法。
 * 
 * 实现步骤：
 * 1. 读取当前页面状态或调用参数；
 * 2. 完成对应的校验、接口调用或数据转换；
 * 3. 更新页面状态或返回处理结果。
 */
function shipmentActionName(item: ShipmentOperationLogView) {
  if (!item.fromStatus) {
    return '新增物流单'
  }
  if (item.fromStatus === item.toStatus) {
    return '修改物流信息'
  }
  return '物流状态确认'
}

/**
 * 执行 shipmentLogDetail 方法。
 * 
 * 实现步骤：
 * 1. 读取当前页面状态或调用参数；
 * 2. 完成对应的校验、接口调用或数据转换；
 * 3. 更新页面状态或返回处理结果。
 */
function shipmentLogDetail(item: ShipmentOperationLogView) {
  /**
   * 常量 statusText：保存当前模块的页面状态、配置项、接口实例或计算结果。
   */
  const statusText = item.fromStatus && item.fromStatus !== item.toStatus
    ? `状态从${shipmentStatusLabel(item.fromStatus)}调整为${shipmentStatusLabel(item.toStatus)}，`
    : ''
  /**
   * 常量 routeText：保存当前模块的页面状态、配置项、接口实例或计算结果。
   */
  const routeText = `发货地为${[item.originDivisionName, item.origin].filter(Boolean).join(' ')}，目的地为${[item.destinationDivisionName, item.destination].filter(Boolean).join(' ')}。`
  /**
   * 常量 carrierText：保存当前模块的页面状态、配置项、接口实例或计算结果。
   */
  const carrierText = `承运商为${item.carrierName}${item.trackingNo ? `，运单号为${item.trackingNo}` : ''}。`
  return `${statusText}${carrierText}${routeText}${item.operationRemark ? `说明：${item.operationRemark}` : ''}`
}

/**
 * 执行 shipmentStatusLabel 方法。
 * 
 * 实现步骤：
 * 1. 读取当前页面状态或调用参数；
 * 2. 完成对应的校验、接口调用或数据转换；
 * 3. 更新页面状态或返回处理结果。
 */
function shipmentStatusLabel(value?: ShipmentStatus) {
  if (!value) {
    return ''
  }
  return {
    /**
     * 字段 CREATED：表示表单、筛选条件、接口数据或组件状态中的 CREATED 值。
     */
    CREATED: '已创建',
    /**
     * 字段 DISPATCHED：表示表单、筛选条件、接口数据或组件状态中的 DISPATCHED 值。
     */
    DISPATCHED: '已发送',
    /**
     * 字段 IN_TRANSIT：表示表单、筛选条件、接口数据或组件状态中的 IN_TRANSIT 值。
     */
    IN_TRANSIT: '运输中',
    /**
     * 字段 DELIVERED：表示表单、筛选条件、接口数据或组件状态中的 DELIVERED 值。
     */
    DELIVERED: '已送达',
    /**
     * 字段 CANCELLED：表示表单、筛选条件、接口数据或组件状态中的 CANCELLED 值。
     */
    CANCELLED: '已取消'
  }[value] || value
}

/**
 * 使用 fetch POST + SSE 流式读取 ratel助手回答。
 *
 * 实现步骤：
 * 1. 使用与 axios 相同的 BASE_URL 拼接后端地址，并携带 Cookie；
 * 2. 逐块读取 ReadableStream，交给 SSE 解析器处理；
 * 3. meta/delta/done/error 分别回调页面，网络异常交给调用方降级。
 */
async function streamAssistant(
  question: string,
  mode = 'hybrid',
  context?: AiAssistantContext,
  handlers?: AiAssistantStreamHandlers
) {
  ensureAuthRequestAllowed('/api/ai/assistant/stream')
  const response = await fetch(streamUrl('/api/ai/assistant/stream'), {
    method: 'POST',
    credentials: 'include',
    headers: {
      'Content-Type': 'application/json',
      Accept: 'text/event-stream, application/json',
      'Cache-Control': 'no-cache'
    },
    body: JSON.stringify({ question, mode, ...(context || {}) }),
    signal: handlers?.signal
  })
  if (!response.ok || !response.body) {
    const error = await streamErrorMessage(response)
    if (response.status === 401 || isAuthFailureCode(error.code)) {
      handleAuthFailure(error.message)
      throw createAuthExpiredError(error.message)
    }
    throw new Error(error.message)
  }
  const reader = response.body.getReader()
  const decoder = new TextDecoder()
  const streamState = { doneReceived: false }
  const parser = createSseParser((message) => handleAssistantStreamMessage(message.event, message.data, handlers, streamState))
  try {
    while (true) {
      let value: Uint8Array | undefined
      let done = false
      try {
        const chunk = await reader.read()
        value = chunk.value
        done = chunk.done
      } catch (error) {
        if (streamState.doneReceived) {
          return
        }
        throw error
      }
      if (done) {
        break
      }
      parser.feed(decoder.decode(value, { stream: true }))
    }
    parser.feed(decoder.decode())
    parser.end()
  } finally {
    reader.releaseLock()
  }
}

/**
 * 处理 ratel助手 SSE 消息。
 */
function handleAssistantStreamMessage(
  event: string,
  data: string,
  handlers?: AiAssistantStreamHandlers,
  streamState?: { doneReceived: boolean }
) {
  if (event === 'heartbeat') {
    return
  }
  const payload = parseJson<Record<string, unknown>>(data)
  if (event === 'meta') {
    handlers?.onMeta?.(payload as unknown as AiAssistantResponse)
  } else if (event === 'delta') {
    handlers?.onDelta?.(String(payload.content || ''))
  } else if (event === 'done') {
    if (streamState) {
      streamState.doneReceived = true
    }
    handlers?.onDone?.(payload as unknown as AiAssistantResponse)
  } else if (event === 'error') {
    const code = String(payload.code || '')
    const message = String(payload.message || 'ratel助手流式请求失败')
    if (isAuthFailureCode(code)) {
      handleAuthFailure(message)
      throw createAuthExpiredError(message)
    }
    handlers?.onError?.(message)
    throw new Error(message)
  }
}

/**
 * 安全解析 JSON。
 */
function parseJson<T>(value: string): T {
  try {
    const parsed = JSON.parse(value)
    if (typeof parsed === 'string') {
      try {
        return JSON.parse(parsed) as T
      } catch {
        return parsed as T
      }
    }
    return parsed as T
  } catch {
    return {} as T
  }
}

/**
 * 拼接流式接口地址。
 */
function streamUrl(url: string) {
  const basePath = import.meta.env.BASE_URL.replace(/\/$/, '')
  return `${basePath}${url.startsWith('/') ? url : `/${url}`}`
}

/**
 * 读取流式请求失败消息。
 */
async function streamErrorMessage(response: Response) {
  try {
    const text = await response.text()
    const payload = JSON.parse(text) as { code?: string; message?: string }
    return {
      code: payload.code,
      message: payload.message || (response.status === 401 ? '当前登录已失效，请重新登录。' : 'ratel助手流式请求失败')
    }
  } catch {
    return {
      code: undefined,
      message: response.status === 401 ? '当前登录已失效，请重新登录。' : 'ratel助手流式请求失败'
    }
  }
}
