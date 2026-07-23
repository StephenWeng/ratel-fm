/**
 * ApiResponse 类型定义，用于约束页面状态、接口入参或接口返回数据结构。
 */
export interface ApiResponse<T> {
  /**
   * 字段 success：表示表单、筛选条件、接口数据或组件状态中的 success 值。
   */
  success: boolean
  /**
   * 字段 code：表示表单、筛选条件、接口数据或组件状态中的 code 值。
   */
  code: string
  /**
   * 字段 message：表示表单、筛选条件、接口数据或组件状态中的 message 值。
   */
  message: string
  /**
   * 字段 data：表示表单、筛选条件、接口数据或组件状态中的 data 值。
   */
  data: T
  /**
   * 字段 timestamp：表示表单、筛选条件、接口数据或组件状态中的 timestamp 值。
   */
  timestamp: string
}

/**
 * PermissionCode 类型定义，用于约束页面状态、接口入参或接口返回数据结构。
 */
export type PermissionCode =
  | 'SYSTEM_USER_MANAGE'
  | 'SYSTEM_ROLE_MANAGE'
  | 'BASIC_DICT_MANAGE'
  | 'FINANCE_SUBJECT_MANAGE'
  | 'FINANCE_VOUCHER_MANAGE'
  | 'PURCHASE_MANAGE'
  | 'LOGISTICS_MANAGE'
  | 'INVENTORY_MANAGE'
  | 'AR_AP_MANAGE'
  | 'WORKFLOW_USE'
  | 'WORKFLOW_MANAGE'
  | 'AI_ASSISTANT_USE'
  | 'REPORT_VIEW'
  | 'SEARCH_VIEW'
  | 'AUDIT_LOG_VIEW'

/**
 * AccountingSourceType 类型定义，用于限定会计平台可制证的业务来源。
 */
export type AccountingSourceType = 'PURCHASE_ORDER' | 'AR_AP_BILL' | 'INVENTORY_LEDGER' | 'CASHIER_TRANSACTION'

/**
 * AccountingPeriodStatus 类型定义，用于约束会计期间开启和关闭状态。
 */
export type AccountingPeriodStatus = 'OPEN' | 'CLOSED'

/**
 * CashierTransactionType 类型定义，用于约束出纳流水类型。
 */
export type CashierTransactionType = 'RECEIPT' | 'PAYMENT' | 'TRANSFER' | 'REFUND'

/**
 * CashierTransactionStatus 类型定义，用于约束出纳流水状态。
 */
export type CashierTransactionStatus = 'DRAFT' | 'CONFIRMED' | 'VOUCHERED' | 'CANCELLED'

/**
 * RoleView 类型定义，用于约束页面状态、接口入参或接口返回数据结构。
 */
export interface RoleView {
  /**
   * 字段 id：表示表单、筛选条件、接口数据或组件状态中的 id 值。
   */
  id: number
  /**
   * 字段 code：表示表单、筛选条件、接口数据或组件状态中的 code 值。
   */
  code: string
  /**
   * 字段 name：表示表单、筛选条件、接口数据或组件状态中的 name 值。
   */
  name: string
  /**
   * 字段 description：表示表单、筛选条件、接口数据或组件状态中的 description 值。
   */
  description?: string
  /**
   * 字段 permissions：表示表单、筛选条件、接口数据或组件状态中的 permissions 值。
   */
  permissions: PermissionCode[]
  /**
   * 字段 menuCodes：表示表单、筛选条件、接口数据或组件状态中的 menuCodes 值。
   */
  menuCodes: string[]
}

/**
 * MenuView 类型定义，用于约束页面状态、接口入参或接口返回数据结构。
 */
export interface MenuView {
  /**
   * 字段 id：表示表单、筛选条件、接口数据或组件状态中的 id 值。
   */
  id: number
  /**
   * 字段 code：表示表单、筛选条件、接口数据或组件状态中的 code 值。
   */
  code: string
  /**
   * 字段 name：表示表单、筛选条件、接口数据或组件状态中的 name 值。
   */
  name: string
  /**
   * 字段 type：表示表单、筛选条件、接口数据或组件状态中的 type 值。
   */
  type: 'MODULE' | 'PAGE' | 'BUTTON'
  /**
   * 字段 parentId：表示表单、筛选条件、接口数据或组件状态中的 parentId 值。
   */
  parentId?: number
  /**
   * 字段 routePath：表示表单、筛选条件、接口数据或组件状态中的 routePath 值。
   */
  routePath?: string
  /**
   * 字段 sortOrder：表示表单、筛选条件、接口数据或组件状态中的 sortOrder 值。
   */
  sortOrder: number
  /**
   * 字段 enabled：表示表单、筛选条件、接口数据或组件状态中的 enabled 值。
   */
  enabled: boolean
  /**
   * 字段 permissionCode：表示表单、筛选条件、接口数据或组件状态中的 permissionCode 值。
   */
  permissionCode?: PermissionCode
}

/**
 * UserView 类型定义，用于约束页面状态、接口入参或接口返回数据结构。
 */
export interface UserView {
  /**
   * 字段 id：表示表单、筛选条件、接口数据或组件状态中的 id 值。
   */
  id: number
  /**
   * 字段 username：表示表单、筛选条件、接口数据或组件状态中的 username 值。
   */
  username: string
  /**
   * 字段 realName：表示表单、筛选条件、接口数据或组件状态中的 realName 值。
   */
  realName: string
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
   * 字段 avatarUrl：表示表单、筛选条件、接口数据或组件状态中的 avatarUrl 值。
   */
  avatarUrl?: string
  /**
   * 字段 defaultAccount：表示表单、筛选条件、接口数据或组件状态中的 defaultAccount 值。
   */
  defaultAccount: boolean
  /**
   * 字段 enabled：表示表单、筛选条件、接口数据或组件状态中的 enabled 值。
   */
  enabled: boolean
  /**
   * 字段 roles：表示表单、筛选条件、接口数据或组件状态中的 roles 值。
   */
  roles: RoleView[]
}

/**
 * MenuCodeView 类型定义，用于约束页面状态、接口入参或接口返回数据结构。
 */
export interface MenuCodeView {
  /**
   * 字段 menuCodes：表示表单、筛选条件、接口数据或组件状态中的 menuCodes 值。
   */
  menuCodes: string[]
}

/**
 * MenuUsageView 类型定义，用于当前用户常用功能排序展示。
 */
export interface MenuUsageView {
  /**
   * 字段 menuCode：表示菜单编码。
   */
  menuCode: string
  /**
   * 字段 menuName：表示菜单名称快照。
   */
  menuName: string
  /**
   * 字段 routePath：表示前端路由路径。
   */
  routePath: string
  /**
   * 字段 useCount：表示用户进入该菜单的累计次数。
   */
  useCount: number
  /**
   * 字段 lastUsedAt：表示最近进入时间。
   */
  lastUsedAt?: string
}

/**
 * WeatherHourlyView 类型定义，用于约束页面状态、接口入参或接口返回数据结构。
 */
export interface WeatherHourlyView {
  /**
   * 字段 time：表示表单、筛选条件、接口数据或组件状态中的 time 值。
   */
  time: string
  /**
   * 字段 temperature：表示表单、筛选条件、接口数据或组件状态中的 temperature 值。
   */
  temperature?: number
  /**
   * 字段 uvIndex：表示紫外线指数。
   */
  uvIndex?: number
  /**
   * 字段 weatherCode：表示表单、筛选条件、接口数据或组件状态中的 weatherCode 值。
   */
  weatherCode?: number
  /**
   * 字段 weatherText：表示表单、筛选条件、接口数据或组件状态中的 weatherText 值。
   */
  weatherText: string
  /**
   * 字段 iconType：表示表单、筛选条件、接口数据或组件状态中的 iconType 值。
   */
  iconType: string
}

/**
 * WeatherView 类型定义，用于约束页面状态、接口入参或接口返回数据结构。
 */
export interface WeatherView {
  /**
   * 字段 available：表示表单、筛选条件、接口数据或组件状态中的 available 值。
   */
  available: boolean
  /**
   * 字段 source：表示表单、筛选条件、接口数据或组件状态中的 source 值。
   */
  source: string
  /**
   * 字段 locationSource：表示天气查询位置来源，BROWSER 为浏览器定位，IP 为公网 IP 粗定位，CONFIG 为配置兜底。
   */
  locationSource?: 'BROWSER' | 'IP' | 'CONFIG' | string
  /**
   * 字段 locationName：表示表单、筛选条件、接口数据或组件状态中的 locationName 值。
   */
  locationName: string
  /**
   * 字段 latitude：表示表单、筛选条件、接口数据或组件状态中的 latitude 值。
   */
  latitude?: number
  /**
   * 字段 longitude：表示表单、筛选条件、接口数据或组件状态中的 longitude 值。
   */
  longitude?: number
  /**
   * 字段 currentTime：表示表单、筛选条件、接口数据或组件状态中的 currentTime 值。
   */
  currentTime?: string
  /**
   * 字段 temperature：表示表单、筛选条件、接口数据或组件状态中的 temperature 值。
   */
  temperature?: number
  /**
   * 字段 uvIndex：表示紫外线指数。
   */
  uvIndex?: number
  /**
   * 字段 weatherCode：表示表单、筛选条件、接口数据或组件状态中的 weatherCode 值。
   */
  weatherCode?: number
  /**
   * 字段 weatherText：表示表单、筛选条件、接口数据或组件状态中的 weatherText 值。
   */
  weatherText: string
  /**
   * 字段 iconType：表示表单、筛选条件、接口数据或组件状态中的 iconType 值。
   */
  iconType: string
  /**
   * 字段 futureHours：表示表单、筛选条件、接口数据或组件状态中的 futureHours 值。
   */
  futureHours: WeatherHourlyView[]
  /**
   * 字段 errorMessage：表示表单、筛选条件、接口数据或组件状态中的 errorMessage 值。
   */
  errorMessage?: string
}

/**
 * SystemStatusView 类型定义，用于约束页面状态、接口入参或接口返回数据结构。
 */
export interface SystemStatusView {
  /**
   * 字段 serverTime：表示表单、筛选条件、接口数据或组件状态中的 serverTime 值。
   */
  serverTime: string
  /**
   * 字段 serverZone：表示表单、筛选条件、接口数据或组件状态中的 serverZone 值。
   */
  serverZone: string
  /**
   * 字段 weather：表示表单、筛选条件、接口数据或组件状态中的 weather 值。
   */
  weather?: WeatherView
}

/**
 * BasicDictionaryView 类型定义，用于约束页面状态、接口入参或接口返回数据结构。
 */
export interface BasicDictionaryView {
  /**
   * 字段 id：表示表单、筛选条件、接口数据或组件状态中的 id 值。
   */
  id: number
  /**
   * 字段 code：表示表单、筛选条件、接口数据或组件状态中的 code 值。
   */
  code: string
  /**
   * 字段 name：表示表单、筛选条件、接口数据或组件状态中的 name 值。
   */
  name: string
  /**
   * 字段 parentId：表示表单、筛选条件、接口数据或组件状态中的 parentId 值。
   */
  parentId?: number
  /**
   * 字段 sortOrder：表示表单、筛选条件、接口数据或组件状态中的 sortOrder 值。
   */
  sortOrder: number
  /**
   * 字段 enabled：表示表单、筛选条件、接口数据或组件状态中的 enabled 值。
   */
  enabled: boolean
  /**
   * 字段 description：表示表单、筛选条件、接口数据或组件状态中的 description 值。
   */
  description?: string
  /**
   * 字段 hasChildren：表示表单、筛选条件、接口数据或组件状态中的 hasChildren 值。
   */
  hasChildren: boolean
  /**
   * 字段 children：表示表单、筛选条件、接口数据或组件状态中的 children 值。
   */
  children?: BasicDictionaryView[]
}

/**
 * ExchangeRateView 类型定义，用于约束币种最新公开参考汇率接口返回数据结构。
 */
export interface ExchangeRateView {
  /**
   * 字段 currencyCode：表示本次查询的币种编码。
   */
  currencyCode: string
  /**
   * 字段 currencyName：表示本次查询的币种名称。
   */
  currencyName: string
  /**
   * 字段 exchangeRateToCny：表示一单位当前币种折人民币的最新公开参考汇率，不代表秒级实时交易价。
   */
  exchangeRateToCny: number
  /**
   * 字段 quoteCurrencyCode：表示目标币种编码，当前固定为 CNY。
   */
  quoteCurrencyCode: string
  /**
   * 字段 source：表示汇率来源，用于页面提示和后续问题追溯。
   */
  source: string
  /**
   * 字段 rateDate：表示外部服务返回的汇率日期，系统固定汇率时可为空。
   */
  rateDate?: string
}

/**
 * AttachmentBizType 类型定义，用于约束页面状态、接口入参或接口返回数据结构。
 */
export type AttachmentBizType = 'VOUCHER' | 'PURCHASE_ORDER' | 'SHIPMENT' | 'INVENTORY_LEDGER' | 'AR_AP_BILL'

/**
 * AttachmentView 类型定义，用于约束页面状态、接口入参或接口返回数据结构。
 */
export interface AttachmentView {
  /**
   * 字段 id：表示表单、筛选条件、接口数据或组件状态中的 id 值。
   */
  id: number
  /**
   * 字段 originalName：表示表单、筛选条件、接口数据或组件状态中的 originalName 值。
   */
  originalName: string
  /**
   * 字段 displayName：表示表单、筛选条件、接口数据或组件状态中的 displayName 值。
   */
  displayName: string
  /**
   * 字段 suffix：表示表单、筛选条件、接口数据或组件状态中的 suffix 值。
   */
  suffix?: string
  /**
   * 字段 fileSize：表示表单、筛选条件、接口数据或组件状态中的 fileSize 值。
   */
  fileSize: number
  /**
   * 字段 contentType：表示表单、筛选条件、接口数据或组件状态中的 contentType 值。
   */
  contentType?: string
  /**
   * 字段 uploaderUsername：表示表单、筛选条件、接口数据或组件状态中的 uploaderUsername 值。
   */
  uploaderUsername?: string
  /**
   * 字段 createdTime：表示表单、筛选条件、接口数据或组件状态中的 createdTime 值。
   */
  createdTime?: string
}

/**
 * LoginResponse 类型定义，用于约束页面状态、接口入参或接口返回数据结构。
 */
export interface LoginResponse {
  /**
   * 字段 expiresAt：表示表单、筛选条件、接口数据或组件状态中的 expiresAt 值。
   */
  expiresAt?: string
  /**
   * 字段 user：表示表单、筛选条件、接口数据或组件状态中的 user 值。
   */
  user: UserView
  /**
   * 字段 repeated：表示表单、筛选条件、接口数据或组件状态中的 repeated 值。
   */
  repeated: boolean
  /**
   * 字段 conflictMessage：表示表单、筛选条件、接口数据或组件状态中的 conflictMessage 值。
   */
  conflictMessage?: string
}

/**
 * SubjectCategory 类型定义，用于约束页面状态、接口入参或接口返回数据结构。
 */
export type SubjectCategory = 'ASSET' | 'LIABILITY' | 'COMMON' | 'EQUITY' | 'REVENUE' | 'COST' | 'EXPENSE'

/**
 * SubjectView 类型定义，用于约束页面状态、接口入参或接口返回数据结构。
 */
export interface SubjectView {
  /**
   * 字段 id：表示表单、筛选条件、接口数据或组件状态中的 id 值。
   */
  id: number
  /**
   * 字段 code：表示表单、筛选条件、接口数据或组件状态中的 code 值。
   */
  code: string
  /**
   * 字段 name：表示表单、筛选条件、接口数据或组件状态中的 name 值。
   */
  name: string
  /**
   * 字段 category：表示表单、筛选条件、接口数据或组件状态中的 category 值。
   */
  category: SubjectCategory
  /**
   * 字段 parentId：表示表单、筛选条件、接口数据或组件状态中的 parentId 值。
   */
  parentId?: number
  /**
   * 字段 parentName：表示表单、筛选条件、接口数据或组件状态中的 parentName 值。
   */
  parentName?: string
  /**
   * 字段 subjectLevel：表示表单、筛选条件、接口数据或组件状态中的 subjectLevel 值。
   */
  subjectLevel: number
  /**
   * 字段 enabled：表示表单、筛选条件、接口数据或组件状态中的 enabled 值。
   */
  enabled: boolean
  /**
   * 字段 description：表示表单、筛选条件、接口数据或组件状态中的 description 值。
   */
  description?: string
}

/**
 * VoucherStatus 类型定义，用于约束页面状态、接口入参或接口返回数据结构。
 */
export type VoucherStatus = 'DRAFT' | 'POSTED' | 'VOIDED'

/**
 * VoucherLineView 类型定义，用于约束页面状态、接口入参或接口返回数据结构。
 */
export interface VoucherLineView {
  /**
   * 字段 id：表示表单、筛选条件、接口数据或组件状态中的 id 值。
   */
  id: number
  /**
   * 字段 lineNo：表示表单、筛选条件、接口数据或组件状态中的 lineNo 值。
   */
  lineNo: number
  /**
   * 字段 subjectId：表示表单、筛选条件、接口数据或组件状态中的 subjectId 值。
   */
  subjectId: number
  /**
   * 字段 subjectCode：表示表单、筛选条件、接口数据或组件状态中的 subjectCode 值。
   */
  subjectCode: string
  /**
   * 字段 subjectName：表示表单、筛选条件、接口数据或组件状态中的 subjectName 值。
   */
  subjectName: string
  /**
   * 字段 subjectFullName：表示会计科目从一级到末级的完整级联名称。
   */
  subjectFullName?: string
  /**
   * 字段 summary：表示表单、筛选条件、接口数据或组件状态中的 summary 值。
   */
  summary: string
  /**
   * 字段 debitAmount：表示表单、筛选条件、接口数据或组件状态中的 debitAmount 值。
   */
  debitAmount: number
  /**
   * 字段 creditAmount：表示表单、筛选条件、接口数据或组件状态中的 creditAmount 值。
   */
  creditAmount: number
  /**
   * 字段 currencyCode：表示表单、筛选条件、接口数据或组件状态中的 currencyCode 值。
   */
  currencyCode: string
  /**
   * 字段 currencyName：表示表单、筛选条件、接口数据或组件状态中的 currencyName 值。
   */
  currencyName: string
  /**
   * 字段 exchangeRateToCny：表示表单、筛选条件、接口数据或组件状态中的 exchangeRateToCny 值。
   */
  exchangeRateToCny: number
  /**
   * 字段 debitAmountCny：表示表单、筛选条件、接口数据或组件状态中的 debitAmountCny 值。
   */
  debitAmountCny: number
  /**
   * 字段 creditAmountCny：表示表单、筛选条件、接口数据或组件状态中的 creditAmountCny 值。
   */
  creditAmountCny: number
  /**
   * 字段 auxiliary：表示表单、筛选条件、接口数据或组件状态中的 auxiliary 值。
   */
  auxiliary?: string
}

/**
 * VoucherView 类型定义，用于约束页面状态、接口入参或接口返回数据结构。
 */
export interface VoucherView {
  /**
   * 字段 id：表示表单、筛选条件、接口数据或组件状态中的 id 值。
   */
  id: number
  /**
   * 字段 voucherNo：表示表单、筛选条件、接口数据或组件状态中的 voucherNo 值。
   */
  voucherNo: string
  /**
   * 字段 voucherDate：表示表单、筛选条件、接口数据或组件状态中的 voucherDate 值。
   */
  voucherDate: string
  /**
   * 字段 belongMonth：表示表单、筛选条件、接口数据或组件状态中的 belongMonth 值。
   */
  belongMonth: string
  /**
   * 字段 projectCode：表示项目字典编码，用于按项目维度筛选和追溯凭证。
   */
  projectCode?: string
  /**
   * 字段 projectName：表示项目名称快照，用于列表、表单和查看流水展示。
   */
  projectName?: string
  /**
   * 字段 summary：表示表单、筛选条件、接口数据或组件状态中的 summary 值。
   */
  summary: string
  /**
   * 字段 status：表示表单、筛选条件、接口数据或组件状态中的 status 值。
   */
  status: VoucherStatus
  /**
   * 字段 totalDebit：表示表单、筛选条件、接口数据或组件状态中的 totalDebit 值。
   */
  totalDebit: number
  /**
   * 字段 totalCredit：表示表单、筛选条件、接口数据或组件状态中的 totalCredit 值。
   */
  totalCredit: number
  /**
   * 字段 currencyCode：表示表单、筛选条件、接口数据或组件状态中的 currencyCode 值。
   */
  currencyCode: string
  /**
   * 字段 currencyName：表示表单、筛选条件、接口数据或组件状态中的 currencyName 值。
   */
  currencyName: string
  /**
   * 字段 exchangeRateToCny：表示表单、筛选条件、接口数据或组件状态中的 exchangeRateToCny 值。
   */
  exchangeRateToCny: number
  /**
   * 字段 totalDebitCny：表示表单、筛选条件、接口数据或组件状态中的 totalDebitCny 值。
   */
  totalDebitCny: number
  /**
   * 字段 totalCreditCny：表示表单、筛选条件、接口数据或组件状态中的 totalCreditCny 值。
   */
  totalCreditCny: number
  /**
   * 字段 createdBy：表示表单、筛选条件、接口数据或组件状态中的 createdBy 值。
   */
  createdBy: string
  /**
   * 字段 postedBy：表示表单、筛选条件、接口数据或组件状态中的 postedBy 值。
   */
  postedBy?: string
  /**
   * 字段 sourceBizNo：表示表单、筛选条件、接口数据或组件状态中的 sourceBizNo 值。
   */
  sourceBizNo?: string
  /** 字段 sourceType：表示凭证来源业务类型，手工凭证为空。 */
  sourceType?: AccountingSourceType
  /** 字段 sourceId：表示凭证来源业务主键，用于反向查看来源单据。 */
  sourceId?: number
  /** 字段 sourceTitle：表示凭证来源业务标题。 */
  sourceTitle?: string
  /**
   * 字段 lines：表示表单、筛选条件、接口数据或组件状态中的 lines 值。
   */
  lines: VoucherLineView[]
}

/**
 * VoucherImportLine 类型定义，用于约束凭证图片或 PDF 识别后的分录草稿。
 */
export interface VoucherImportLine {
  /** 匹配成功的会计科目 ID，未匹配成功时为空。 */
  subjectId?: number
  /** 模型识别或匹配到的科目名称。 */
  subjectName?: string
  /** 模型识别或匹配到的完整级联科目名称。 */
  subjectFullName?: string
  /** 分录摘要。 */
  summary: string
  /** 借方金额。 */
  debitAmount: number
  /** 贷方金额。 */
  creditAmount: number
  /** 币种编码。 */
  currencyCode: string
  /** 币种名称。 */
  currencyName: string
  /** 折人民币汇率。 */
  exchangeRateToCny: number
  /** 辅助核算信息。 */
  auxiliary?: string
  /** 模型识别可信度。 */
  confidence?: number
  /** 该行需要用户关注的提示。 */
  warning?: string
}

/**
 * VoucherImportResult 类型定义，用于约束凭证导入识别接口返回结构。
 */
export interface VoucherImportResult {
  /** 识别出的凭证日期。 */
  voucherDate?: string
  /** 识别出的整张凭证摘要。 */
  summary?: string
  /** 识别出的来源单号。 */
  sourceBizNo?: string
  /** 识别出的凭证分录草稿。 */
  lines: VoucherImportLine[]
  /** 识别提示和需要人工确认的问题。 */
  warnings: string[]
}

/**
 * AccountingSourceView 类型定义，用于会计平台统一展示采购单、应收应付单等可制证业务来源。
 */
export interface AccountingSourceView {
  /** 字段 sourceType：表示业务来源类型。 */
  sourceType: AccountingSourceType
  /** 字段 sourceId：表示来源业务单据主键。 */
  sourceId: number
  /** 字段 sourceNo：表示来源业务单号。 */
  sourceNo: string
  /** 字段 sourceTitle：表示来源业务单据标题。 */
  sourceTitle: string
  /** 字段 projectCode：表示来源业务单据项目编码。 */
  projectCode?: string
  /** 字段 projectName：表示来源业务单据项目名称。 */
  projectName?: string
  /** 字段 businessDate：表示来源业务发生日期。 */
  businessDate?: string
  /** 字段 partnerName：表示供应商、客户或其他往来单位。 */
  partnerName?: string
  /** 字段 amount：表示来源业务原币金额。 */
  amount: number
  /** 字段 amountCny：表示来源业务折人民币金额。 */
  amountCny: number
  /** 字段 currencyCode：表示来源业务币种编码。 */
  currencyCode: string
  /** 字段 currencyName：表示来源业务币种名称。 */
  currencyName: string
  /** 字段 exchangeRateToCny：表示来源业务折人民币汇率。 */
  exchangeRateToCny: number
  /** 字段 statusText：表示来源业务中文状态。 */
  statusText: string
  /** 字段 voucherGenerated：表示该来源是否已经存在未作废凭证。 */
  voucherGenerated: boolean
}

/**
 * VoucherSourceField 类型定义，用于凭证来源弹窗展示通用字段。
 */
export interface VoucherSourceField {
  /** 字段 label：表示来源字段中文名称。 */
  label: string
  /** 字段 value：表示来源字段展示值。 */
  value: string
}

/**
 * VoucherSourceDetail 类型定义，用于凭证反向查看来源模块和来源单据。
 */
export interface VoucherSourceDetail {
  /** 字段 sourceType：表示来源业务类型。 */
  sourceType: AccountingSourceType
  /** 字段 sourceId：表示来源业务主键。 */
  sourceId: number
  /** 字段 sourceNo：表示来源业务单号。 */
  sourceNo: string
  /** 字段 sourceTitle：表示来源业务标题。 */
  sourceTitle: string
  /** 字段 sourceModule：表示来源模块中文名称。 */
  sourceModule: string
  /** 字段 fields：表示来源详情通用字段。 */
  fields: VoucherSourceField[]
}

/**
 * AutoVoucherResult 类型定义，用于承载会计平台自动生成凭证后的返回结果。
 */
export interface AutoVoucherResult {
  /** 字段 voucher：表示生成的凭证草稿。 */
  voucher: VoucherView
  /** 字段 source：表示本次制证使用的业务来源。 */
  source: AccountingSourceView
  /** 字段 message：表示后端返回的生成说明。 */
  message: string
}

/**
 * AccountingPeriodView 类型定义，用于约束会计期间列表数据结构。
 */
export interface AccountingPeriodView {
  /** 字段 id：表示会计期间主键。 */
  id: number
  /** 字段 organizationCode：表示所属公司字典编码。 */
  organizationCode: string
  /** 字段 periodCode：表示期间编码，格式 yyyy-MM。 */
  periodCode: string
  /** 字段 startDate：表示期间开始日期。 */
  startDate: string
  /** 字段 endDate：表示期间结束日期。 */
  endDate: string
  /** 字段 status：表示会计期间状态。 */
  status: AccountingPeriodStatus
  /** 字段 closedBy：表示结账人账号。 */
  closedBy?: string
  /** 字段 closedTime：表示结账时间。 */
  closedTime?: string
  /** 字段 remark：表示期间备注。 */
  remark?: string
}

/**
 * PeriodCloseCheckView 类型定义，用于约束月结前检查结果。
 */
export interface PeriodCloseCheckView {
  /** 字段 periodCode：表示检查的会计期间编码。 */
  periodCode: string
  /** 字段 closable：表示是否允许结账。 */
  closable: boolean
  /** 字段 blockingItems：表示阻断结账的问题。 */
  blockingItems: string[]
  /** 字段 warningItems：表示允许继续但需要确认的提示。 */
  warningItems: string[]
}

/**
 * CashierTransactionView 类型定义，用于约束出纳资金流水。
 */
export interface CashierTransactionView {
  /** 字段 id：表示出纳流水主键。 */
  id: number
  /** 字段 organizationCode：表示所属公司字典编码。 */
  organizationCode: string
  /** 字段 transactionNo：表示出纳流水号。 */
  transactionNo: string
  /** 字段 transactionDate：表示交易日期。 */
  transactionDate: string
  /** 字段 transactionType：表示资金流水类型。 */
  transactionType: CashierTransactionType
  /** 字段 status：表示资金流水状态。 */
  status: CashierTransactionStatus
  /** 字段 projectCode：表示项目字典编码。 */
  projectCode?: string
  /** 字段 projectName：表示项目名称快照。 */
  projectName?: string
  /** 字段 partnerName：表示往来单位。 */
  partnerName?: string
  /** 字段 bankAccount：表示银行或现金账户。 */
  bankAccount?: string
  /** 字段 settlementMethod：表示结算方式。 */
  settlementMethod?: string
  /** 字段 amount：表示原币金额。 */
  amount: number
  /** 字段 currencyCode：表示币种编码。 */
  currencyCode: string
  /** 字段 currencyName：表示币种名称。 */
  currencyName: string
  /** 字段 exchangeRateToCny：表示折人民币汇率。 */
  exchangeRateToCny: number
  /** 字段 amountCny：表示折人民币金额。 */
  amountCny: number
  /** 字段 relatedBizNo：表示关联业务单号。 */
  relatedBizNo?: string
  /** 字段 summary：表示摘要。 */
  summary: string
  /** 字段 remark：表示备注。 */
  remark?: string
  /** 字段 createdBy：表示创建人账号。 */
  createdBy?: string
  /** 字段 confirmedBy：表示确认人账号。 */
  confirmedBy?: string
  /** 字段 confirmedTime：表示确认时间。 */
  confirmedTime?: string
  /** 字段 voucherId：表示生成的凭证主键。 */
  voucherId?: number
  /** 字段 voucherNo：表示生成的凭证号。 */
  voucherNo?: string
}

/**
 * TrialBalanceRow 类型定义，用于约束页面状态、接口入参或接口返回数据结构。
 */
export interface TrialBalanceRow {
  /**
   * 字段 subjectId：表示表单、筛选条件、接口数据或组件状态中的 subjectId 值。
   */
  subjectId: number
  /**
   * 字段 subjectCode：表示表单、筛选条件、接口数据或组件状态中的 subjectCode 值。
   */
  subjectCode: string
  /**
   * 字段 subjectName：表示表单、筛选条件、接口数据或组件状态中的 subjectName 值。
   */
  subjectName: string
  /**
   * 字段 debitAmount：表示表单、筛选条件、接口数据或组件状态中的 debitAmount 值。
   */
  debitAmount: number
  /**
   * 字段 creditAmount：表示表单、筛选条件、接口数据或组件状态中的 creditAmount 值。
   */
  creditAmount: number
  /**
   * 字段 balance：表示表单、筛选条件、接口数据或组件状态中的 balance 值。
   */
  balance: number
}

/**
 * PurchaseStatus 类型定义，用于约束页面状态、接口入参或接口返回数据结构。
 */
export type PurchaseStatus =
  | 'DRAFT'
  | 'IN_APPROVAL'
  | 'APPROVAL_REJECTED'
  | 'SUBMITTED'
  | 'APPROVED'
  | 'PURCHASING'
  | 'PURCHASE_COMPLETED'
  | 'RECEIVED'
  | 'CLOSED'
  | 'CANCELLED'

/**
 * PurchaseLineView 类型定义，用于约束页面状态、接口入参或接口返回数据结构。
 */
export interface PurchaseLineView {
  /**
   * 字段 id：表示表单、筛选条件、接口数据或组件状态中的 id 值。
   */
  id: number
  /**
   * 字段 lineNo：表示表单、筛选条件、接口数据或组件状态中的 lineNo 值。
   */
  lineNo: number
  /**
   * 字段 itemCode：表示表单、筛选条件、接口数据或组件状态中的 itemCode 值。
   */
  itemCode: string
  /**
   * 字段 itemName：表示表单、筛选条件、接口数据或组件状态中的 itemName 值。
   */
  itemName: string
  /**
   * 字段 specification：表示表单、筛选条件、接口数据或组件状态中的 specification 值。
   */
  specification?: string
  /**
   * 字段 unitName：表示表单、筛选条件、接口数据或组件状态中的 unitName 值。
   */
  unitName?: string
  /**
   * 字段 quantity：表示表单、筛选条件、接口数据或组件状态中的 quantity 值。
   */
  quantity: number
  /**
   * 字段 unitPrice：表示表单、筛选条件、接口数据或组件状态中的 unitPrice 值。
   */
  unitPrice: number
  /**
   * 字段 amount：表示表单、筛选条件、接口数据或组件状态中的 amount 值。
   */
  amount: number
  /**
   * 字段 taxRate：表示表单、筛选条件、接口数据或组件状态中的 taxRate 值。
   */
  taxRate?: number
  /**
   * 字段 taxAmount：表示表单、筛选条件、接口数据或组件状态中的 taxAmount 值。
   */
  taxAmount?: number
  /**
   * 字段 amountWithTax：表示表单、筛选条件、接口数据或组件状态中的 amountWithTax 值。
   */
  amountWithTax?: number
  /**
   * 字段 plannedArrivalDate：表示表单、筛选条件、接口数据或组件状态中的 plannedArrivalDate 值。
   */
  plannedArrivalDate?: string
  /**
   * 字段 receiveWarehouse：表示表单、筛选条件、接口数据或组件状态中的 receiveWarehouse 值。
   */
  receiveWarehouse?: string
  /**
   * 字段 currencyCode：表示表单、筛选条件、接口数据或组件状态中的 currencyCode 值。
   */
  currencyCode: string
  /**
   * 字段 currencyName：表示表单、筛选条件、接口数据或组件状态中的 currencyName 值。
   */
  currencyName: string
  /**
   * 字段 exchangeRateToCny：表示表单、筛选条件、接口数据或组件状态中的 exchangeRateToCny 值。
   */
  exchangeRateToCny: number
  /**
   * 字段 unitPriceCny：表示表单、筛选条件、接口数据或组件状态中的 unitPriceCny 值。
   */
  unitPriceCny: number
  /**
   * 字段 amountCny：表示表单、筛选条件、接口数据或组件状态中的 amountCny 值。
   */
  amountCny: number
}

/**
 * PurchaseOrderView 类型定义，用于约束页面状态、接口入参或接口返回数据结构。
 */
export interface PurchaseOrderView {
  /**
   * 字段 id：表示表单、筛选条件、接口数据或组件状态中的 id 值。
   */
  id: number
  /**
   * 字段 orderNo：表示表单、筛选条件、接口数据或组件状态中的 orderNo 值。
   */
  orderNo: string
  /**
   * 字段 supplierName：表示表单、筛选条件、接口数据或组件状态中的 supplierName 值。
   */
  supplierName: string
  /**
   * 字段 documentType：表示表单、筛选条件、接口数据或组件状态中的 documentType 值。
   */
  documentType?: string
  /**
   * 字段 businessType：表示表单、筛选条件、接口数据或组件状态中的 businessType 值。
   */
  businessType?: string
  /**
   * 字段 projectCode：表示项目字典编码，用于区分采购单所属项目。
   */
  projectCode?: string
  /**
   * 字段 projectName：表示项目名称快照，用于采购列表、表单和查看流水展示。
   */
  projectName?: string
  /**
   * 字段 purchaseOrganization：表示表单、筛选条件、接口数据或组件状态中的 purchaseOrganization 值。
   */
  purchaseOrganization?: string
  /**
   * 字段 purchaseDepartment：表示表单、筛选条件、接口数据或组件状态中的 purchaseDepartment 值。
   */
  purchaseDepartment?: string
  /**
   * 字段 purchaserName：表示表单、筛选条件、接口数据或组件状态中的 purchaserName 值。
   */
  purchaserName?: string
  /**
   * 字段 settlementOrganization：表示表单、筛选条件、接口数据或组件状态中的 settlementOrganization 值。
   */
  settlementOrganization?: string
  /**
   * 字段 paymentTerms：表示表单、筛选条件、接口数据或组件状态中的 paymentTerms 值。
   */
  paymentTerms?: string
  /**
   * 字段 settlementMethod：表示表单、筛选条件、接口数据或组件状态中的 settlementMethod 值。
   */
  settlementMethod?: string
  /**
   * 字段 deliveryTerms：表示表单、筛选条件、接口数据或组件状态中的 deliveryTerms 值。
   */
  deliveryTerms?: string
  /**
   * 字段 sourceBillType：表示表单、筛选条件、接口数据或组件状态中的 sourceBillType 值。
   */
  sourceBillType?: string
  /**
   * 字段 sourceBillNo：表示表单、筛选条件、接口数据或组件状态中的 sourceBillNo 值。
   */
  sourceBillNo?: string
  /**
   * 字段 orderDate：表示表单、筛选条件、接口数据或组件状态中的 orderDate 值。
   */
  orderDate: string
  /**
   * 字段 status：表示表单、筛选条件、接口数据或组件状态中的 status 值。
   */
  status: PurchaseStatus
  /**
   * 字段 totalAmount：表示表单、筛选条件、接口数据或组件状态中的 totalAmount 值。
   */
  totalAmount: number
  /**
   * 字段 currencyCode：表示表单、筛选条件、接口数据或组件状态中的 currencyCode 值。
   */
  currencyCode: string
  /**
   * 字段 currencyName：表示表单、筛选条件、接口数据或组件状态中的 currencyName 值。
   */
  currencyName: string
  /**
   * 字段 exchangeRateToCny：表示表单、筛选条件、接口数据或组件状态中的 exchangeRateToCny 值。
   */
  exchangeRateToCny: number
  /**
   * 字段 totalAmountCny：表示表单、筛选条件、接口数据或组件状态中的 totalAmountCny 值。
   */
  totalAmountCny: number
  /**
   * 字段 createdBy：表示表单、筛选条件、接口数据或组件状态中的 createdBy 值。
   */
  createdBy: string
  /**
   * 字段 remark：表示表单、筛选条件、接口数据或组件状态中的 remark 值。
   */
  remark?: string
  /**
   * 字段 cancelType：表示取消采购类型名称。
   */
  cancelType?: string
  /**
   * 字段 cancelReason：表示取消采购原因。
   */
  cancelReason?: string
  /** 字段 voucherId：表示采购单通过会计平台生成的凭证主键。 */
  voucherId?: number
  /** 字段 voucherNo：表示采购单通过会计平台生成的凭证号。 */
  voucherNo?: string
  /**
   * 字段 workflow：表示采购单最近一次审批流程详情。
   */
  workflow?: WorkflowInstanceDetailView
  /**
   * 字段 lines：表示表单、筛选条件、接口数据或组件状态中的 lines 值。
   */
  lines: PurchaseLineView[]
}

/**
 * WorkflowStatus 类型定义，用于约束流程实例运行状态。
 */
export type WorkflowStatus = 'RUNNING' | 'APPROVED' | 'REJECTED' | 'CANCELLED'

/**
 * WorkflowTaskStatus 类型定义，用于约束流程任务节点状态。
 */
export type WorkflowTaskStatus = 'PENDING' | 'APPROVED' | 'REJECTED' | 'SKIPPED'

/**
 * WorkflowApproverType 类型定义，用于约束流程节点审批人来源。
 */
export type WorkflowApproverType = 'USER' | 'DEPARTMENT' | 'DEPARTMENT_POSITION'

/**
 * WorkflowOperationType 类型定义，用于约束流程操作流水类型。
 */
export type WorkflowOperationType = 'START' | 'APPROVE' | 'REJECT' | 'CANCEL'

/**
 * WorkflowNodeView 类型定义，用于流程定义节点展示和保存。
 */
export interface WorkflowNodeView {
  nodeOrder: number
  nodeName: string
  approverType: WorkflowApproverType
  approverUserId?: number
  approverUsername?: string
  approverName?: string
  approverDepartment?: string
  approverPosition?: string
  approverDisplay?: string
}

/**
 * WorkflowDefinitionView 类型定义，用于流程定义列表和表单。
 */
export interface WorkflowDefinitionView {
  id: number
  organizationCode: string
  name: string
  code: string
  description?: string
  nodes: WorkflowNodeView[]
  enabled: boolean
}

/**
 * WorkflowConfigView 类型定义，用于流程管理列表和表单。
 */
export interface WorkflowConfigView {
  id: number
  organizationCode: string
  businessModuleCode: string
  businessModuleName: string
  functionModuleCode: string
  functionModuleName: string
  definitionId: number
  definitionName: string
  enabled: boolean
}

/**
 * WorkflowItemView 类型定义，用于审批中心待办、已办和发起事宜。
 */
export interface WorkflowItemView {
  instanceId: number
  taskId?: number
  businessModuleCode: string
  businessModuleName: string
  functionModuleCode: string
  functionModuleName: string
  businessType: string
  businessId: number
  businessNo: string
  projectCode?: string
  projectName?: string
  title: string
  starterId: number
  starterUsername: string
  starterName: string
  startedTime: string
  currentNodeName?: string
  status: WorkflowStatus
  nextApproverInfo?: string
  nextApproverUsers?: WorkflowApproverUserView[]
  taskStatus?: WorkflowTaskStatus
  actedTime?: string
}

/**
 * WorkflowApproverUserView 类型定义，用于展示下个审批节点命中的人员姓名和联系方式。
 */
export interface WorkflowApproverUserView {
  name: string
  phone?: string
}

/**
 * WorkflowTaskView 类型定义，用于流程查看节点进度。
 */
export interface WorkflowTaskView {
  id?: number
  nodeOrder: number
  nodeName: string
  approverDisplay?: string
  status: WorkflowTaskStatus
  actedById?: number
  actedByUsername?: string
  actedByName?: string
  comment?: string
  actedAt?: string
}

/**
 * WorkflowOperationLogView 类型定义，用于流程操作流水。
 */
export interface WorkflowOperationLogView {
  id: number
  operationType: WorkflowOperationType
  nodeOrder?: number
  nodeName?: string
  operatorId: number
  operatorUsername: string
  operatorName: string
  operatorPhone?: string
  comment?: string
  operationTime: string
}

/**
 * WorkflowBusinessFormFieldView 类型定义，用于流程审批弹窗展示业务表单字段。
 */
export interface WorkflowBusinessFormFieldView {
  label: string
  value?: string
}

/**
 * WorkflowBusinessFormSectionView 类型定义，用于流程审批弹窗展示业务表单字段分组。
 */
export interface WorkflowBusinessFormSectionView {
  title: string
  fields: WorkflowBusinessFormFieldView[]
}

/**
 * WorkflowBusinessFormTableColumnView 类型定义，用于流程审批弹窗展示业务明细列。
 */
export interface WorkflowBusinessFormTableColumnView {
  key: string
  label: string
}

/**
 * WorkflowBusinessFormTableView 类型定义，用于流程审批弹窗展示业务明细表格。
 */
export interface WorkflowBusinessFormTableView {
  title: string
  columns: WorkflowBusinessFormTableColumnView[]
  rows: Record<string, string>[]
}

/**
 * WorkflowBusinessFormView 类型定义，用于承载不同业务流程的表单预览内容。
 */
export interface WorkflowBusinessFormView {
  title?: string
  sections: WorkflowBusinessFormSectionView[]
  tables: WorkflowBusinessFormTableView[]
}

/**
 * WorkflowInstanceDetailView 类型定义，用于流程查看弹窗。
 */
export interface WorkflowInstanceDetailView {
  instance: WorkflowItemView
  tasks: WorkflowTaskView[]
  operationLogs: WorkflowOperationLogView[]
  businessForm?: WorkflowBusinessFormView
}

/**
 * ShipmentStatus 类型定义，用于约束页面状态、接口入参或接口返回数据结构。
 */
export type ShipmentStatus = 'CREATED' | 'DISPATCHED' | 'IN_TRANSIT' | 'DELIVERED' | 'CANCELLED'

/**
 * ShipmentView 类型定义，用于约束页面状态、接口入参或接口返回数据结构。
 */
export interface ShipmentView {
  /**
   * 字段 id：表示表单、筛选条件、接口数据或组件状态中的 id 值。
   */
  id: number
  /**
   * 字段 shipmentNo：表示表单、筛选条件、接口数据或组件状态中的 shipmentNo 值。
   */
  shipmentNo: string
  /**
   * 字段 relatedOrderNo：表示表单、筛选条件、接口数据或组件状态中的 relatedOrderNo 值。
   */
  relatedOrderNo?: string
  /**
   * 字段 documentType：表示表单、筛选条件、接口数据或组件状态中的 documentType 值。
   */
  documentType?: string
  /**
   * 字段 transportMode：表示表单、筛选条件、接口数据或组件状态中的 transportMode 值。
   */
  transportMode?: string
  /**
   * 字段 projectCode：表示项目字典编码，用于区分物流单所属项目。
   */
  projectCode?: string
  /**
   * 字段 projectName：表示项目名称快照，用于物流列表、表单和查看流水展示。
   */
  projectName?: string
  /**
   * 字段 shippingOrganization：表示表单、筛选条件、接口数据或组件状态中的 shippingOrganization 值。
   */
  shippingOrganization?: string
  /**
   * 字段 receivingOrganization：表示表单、筛选条件、接口数据或组件状态中的 receivingOrganization 值。
   */
  receivingOrganization?: string
  /**
   * 字段 carrierName：表示表单、筛选条件、接口数据或组件状态中的 carrierName 值。
   */
  carrierName: string
  /**
   * 字段 trackingNo：表示表单、筛选条件、接口数据或组件状态中的 trackingNo 值。
   */
  trackingNo?: string
  /**
   * 字段 driverName：表示表单、筛选条件、接口数据或组件状态中的 driverName 值。
   */
  driverName?: string
  /**
   * 字段 driverPhone：表示表单、筛选条件、接口数据或组件状态中的 driverPhone 值。
   */
  driverPhone?: string
  /**
   * 字段 vehicleNo：表示表单、筛选条件、接口数据或组件状态中的 vehicleNo 值。
   */
  vehicleNo?: string
  /**
   * 字段 originDivisionCode：表示表单、筛选条件、接口数据或组件状态中的 originDivisionCode 值。
   */
  originDivisionCode?: string
  /**
   * 字段 originDivisionName：表示表单、筛选条件、接口数据或组件状态中的 originDivisionName 值。
   */
  originDivisionName?: string
  /**
   * 字段 destinationDivisionCode：表示表单、筛选条件、接口数据或组件状态中的 destinationDivisionCode 值。
   */
  destinationDivisionCode?: string
  /**
   * 字段 destinationDivisionName：表示表单、筛选条件、接口数据或组件状态中的 destinationDivisionName 值。
   */
  destinationDivisionName?: string
  /**
   * 字段 origin：表示表单、筛选条件、接口数据或组件状态中的 origin 值。
   */
  origin: string
  /**
   * 字段 destination：表示表单、筛选条件、接口数据或组件状态中的 destination 值。
   */
  destination: string
  /**
   * 字段 plannedShipDate：表示表单、筛选条件、接口数据或组件状态中的 plannedShipDate 值。
   */
  plannedShipDate: string
  /**
   * 字段 actualShipDate：表示表单、筛选条件、接口数据或组件状态中的 actualShipDate 值。
   */
  actualShipDate?: string
  /**
   * 字段 deliveredDate：表示表单、筛选条件、接口数据或组件状态中的 deliveredDate 值。
   */
  deliveredDate?: string
  /**
   * 字段 status：表示表单、筛选条件、接口数据或组件状态中的 status 值。
   */
  status: ShipmentStatus
  /**
   * 字段 remark：表示表单、筛选条件、接口数据或组件状态中的 remark 值。
   */
  remark?: string
}

/**
 * ShipmentOperationLogView 类型定义，用于约束页面状态、接口入参或接口返回数据结构。
 */
export interface ShipmentOperationLogView {
  /**
   * 字段 id：表示表单、筛选条件、接口数据或组件状态中的 id 值。
   */
  id: number
  /**
   * 字段 shipmentNo：表示表单、筛选条件、接口数据或组件状态中的 shipmentNo 值。
   */
  shipmentNo: string
  /**
   * 字段 fromStatus：表示表单、筛选条件、接口数据或组件状态中的 fromStatus 值。
   */
  fromStatus?: ShipmentStatus
  /**
   * 字段 toStatus：表示表单、筛选条件、接口数据或组件状态中的 toStatus 值。
   */
  toStatus: ShipmentStatus
  /**
   * 字段 relatedOrderNo：表示表单、筛选条件、接口数据或组件状态中的 relatedOrderNo 值。
   */
  relatedOrderNo?: string
  /**
   * 字段 documentType：表示表单、筛选条件、接口数据或组件状态中的 documentType 值。
   */
  documentType?: string
  /**
   * 字段 transportMode：表示表单、筛选条件、接口数据或组件状态中的 transportMode 值。
   */
  transportMode?: string
  /**
   * 字段 projectCode：表示项目字典编码，用于在物流状态流水中保留项目维度。
   */
  projectCode?: string
  /**
   * 字段 projectName：表示项目名称快照，用于物流状态流水快照展示。
   */
  projectName?: string
  /**
   * 字段 shippingOrganization：表示表单、筛选条件、接口数据或组件状态中的 shippingOrganization 值。
   */
  shippingOrganization?: string
  /**
   * 字段 receivingOrganization：表示表单、筛选条件、接口数据或组件状态中的 receivingOrganization 值。
   */
  receivingOrganization?: string
  /**
   * 字段 carrierName：表示表单、筛选条件、接口数据或组件状态中的 carrierName 值。
   */
  carrierName: string
  /**
   * 字段 trackingNo：表示表单、筛选条件、接口数据或组件状态中的 trackingNo 值。
   */
  trackingNo?: string
  /**
   * 字段 driverName：表示表单、筛选条件、接口数据或组件状态中的 driverName 值。
   */
  driverName?: string
  /**
   * 字段 driverPhone：表示表单、筛选条件、接口数据或组件状态中的 driverPhone 值。
   */
  driverPhone?: string
  /**
   * 字段 vehicleNo：表示表单、筛选条件、接口数据或组件状态中的 vehicleNo 值。
   */
  vehicleNo?: string
  /**
   * 字段 originDivisionCode：表示表单、筛选条件、接口数据或组件状态中的 originDivisionCode 值。
   */
  originDivisionCode?: string
  /**
   * 字段 originDivisionName：表示表单、筛选条件、接口数据或组件状态中的 originDivisionName 值。
   */
  originDivisionName?: string
  /**
   * 字段 destinationDivisionCode：表示表单、筛选条件、接口数据或组件状态中的 destinationDivisionCode 值。
   */
  destinationDivisionCode?: string
  /**
   * 字段 destinationDivisionName：表示表单、筛选条件、接口数据或组件状态中的 destinationDivisionName 值。
   */
  destinationDivisionName?: string
  /**
   * 字段 origin：表示表单、筛选条件、接口数据或组件状态中的 origin 值。
   */
  origin: string
  /**
   * 字段 destination：表示表单、筛选条件、接口数据或组件状态中的 destination 值。
   */
  destination: string
  /**
   * 字段 plannedShipDate：表示表单、筛选条件、接口数据或组件状态中的 plannedShipDate 值。
   */
  plannedShipDate: string
  /**
   * 字段 actualShipDate：表示表单、筛选条件、接口数据或组件状态中的 actualShipDate 值。
   */
  actualShipDate?: string
  /**
   * 字段 deliveredDate：表示表单、筛选条件、接口数据或组件状态中的 deliveredDate 值。
   */
  deliveredDate?: string
  /**
   * 字段 remark：表示表单、筛选条件、接口数据或组件状态中的 remark 值。
   */
  remark?: string
  /**
   * 字段 operationRemark：表示表单、筛选条件、接口数据或组件状态中的 operationRemark 值。
   */
  operationRemark?: string
  /**
   * 字段 operatorId：表示表单、筛选条件、接口数据或组件状态中的 operatorId 值。
   */
  operatorId?: number
  /**
   * 字段 operatorUsername：表示表单、筛选条件、接口数据或组件状态中的 operatorUsername 值。
   */
  operatorUsername?: string
  /**
   * 字段 operatorName：表示表单、筛选条件、接口数据或组件状态中的 operatorName 值。
   */
  operatorName?: string
  /**
   * 字段 operationTime：表示表单、筛选条件、接口数据或组件状态中的 operationTime 值。
   */
  operationTime?: string
}

/**
 * BusinessOperationLogView 类型定义，用于约束页面状态、接口入参或接口返回数据结构。
 */
export interface BusinessOperationLogView {
  /**
   * 字段 id：表示表单、筛选条件、接口数据或组件状态中的 id 值。
   */
  id: number
  /**
   * 字段 businessType：表示表单、筛选条件、接口数据或组件状态中的 businessType 值。
   */
  businessType: string
  /**
   * 字段 businessId：表示表单、筛选条件、接口数据或组件状态中的 businessId 值。
   */
  businessId: number
  /**
   * 字段 businessNo：表示表单、筛选条件、接口数据或组件状态中的 businessNo 值。
   */
  businessNo: string
  /**
   * 字段 businessTitle：表示表单、筛选条件、接口数据或组件状态中的 businessTitle 值。
   */
  businessTitle: string
  /**
   * 字段 action：表示表单、筛选条件、接口数据或组件状态中的 action 值。
   */
  action: string
  /**
   * 字段 actionName：表示表单、筛选条件、接口数据或组件状态中的 actionName 值。
   */
  actionName: string
  /**
   * 字段 detail：表示表单、筛选条件、接口数据或组件状态中的 detail 值。
   */
  detail: string
  /**
   * 字段 fromState：表示表单、筛选条件、接口数据或组件状态中的 fromState 值。
   */
  fromState?: string
  /**
   * 字段 toState：表示表单、筛选条件、接口数据或组件状态中的 toState 值。
   */
  toState?: string
  /**
   * 字段 snapshot：表示表单、筛选条件、接口数据或组件状态中的 snapshot 值。
   */
  snapshot?: string
  /**
   * 字段 operatorId：表示表单、筛选条件、接口数据或组件状态中的 operatorId 值。
   */
  operatorId?: number
  /**
   * 字段 operatorUsername：表示表单、筛选条件、接口数据或组件状态中的 operatorUsername 值。
   */
  operatorUsername?: string
  /**
   * 字段 operatorName：表示表单、筛选条件、接口数据或组件状态中的 operatorName 值。
   */
  operatorName?: string
  /**
   * 字段 operationTime：表示表单、筛选条件、接口数据或组件状态中的 operationTime 值。
   */
  operationTime?: string
}

/**
 * UnifiedOperationLogView 类型定义，用于约束页面状态、接口入参或接口返回数据结构。
 */
export type UnifiedOperationLogView = BusinessOperationLogView

/**
 * BusinessOperationLogPage 类型定义，用于约束页面状态、接口入参或接口返回数据结构。
 */
export interface BusinessOperationLogPage {
  /**
   * 字段 rows：表示表单、筛选条件、接口数据或组件状态中的 rows 值。
   */
  rows: UnifiedOperationLogView[]
  /**
   * 字段 total：表示表单、筛选条件、接口数据或组件状态中的 total 值。
   */
  total: number
}

/**
 * DashboardOverview 类型定义，用于约束页面状态、接口入参或接口返回数据结构。
 */
export interface DashboardOverview {
  /**
   * 字段 userCount：表示表单、筛选条件、接口数据或组件状态中的 userCount 值。
   */
  userCount: number
  /**
   * 字段 subjectCount：表示表单、筛选条件、接口数据或组件状态中的 subjectCount 值。
   */
  subjectCount: number
  /**
   * 字段 voucherCount：表示表单、筛选条件、接口数据或组件状态中的 voucherCount 值。
   */
  voucherCount: number
  /**
   * 字段 purchaseOrderCount：表示表单、筛选条件、接口数据或组件状态中的 purchaseOrderCount 值。
   */
  purchaseOrderCount: number
  /**
   * 字段 shipmentOrderCount：表示表单、筛选条件、接口数据或组件状态中的 shipmentOrderCount 值。
   */
  shipmentOrderCount: number
  /**
   * 字段 draftVoucherCount：表示表单、筛选条件、接口数据或组件状态中的 draftVoucherCount 值。
   */
  draftVoucherCount: number
  /**
   * 字段 pendingPurchaseCount：表示表单、筛选条件、接口数据或组件状态中的 pendingPurchaseCount 值。
   */
  pendingPurchaseCount: number
  /**
   * 字段 inTransitShipmentCount：表示表单、筛选条件、接口数据或组件状态中的 inTransitShipmentCount 值。
   */
  inTransitShipmentCount: number
  /**
   * 字段 overdueArApCount：表示表单、筛选条件、接口数据或组件状态中的 overdueArApCount 值。
   */
  overdueArApCount: number
  /**
   * 字段 postedDebitTotal：表示表单、筛选条件、接口数据或组件状态中的 postedDebitTotal 值。
   */
  postedDebitTotal: number
  /**
   * 字段 purchaseTotal：表示表单、筛选条件、接口数据或组件状态中的 purchaseTotal 值。
   */
  purchaseTotal: number
  /**
   * 字段 receivableOpenAmount：表示表单、筛选条件、接口数据或组件状态中的 receivableOpenAmount 值。
   */
  receivableOpenAmount: number
  /**
   * 字段 payableOpenAmount：表示表单、筛选条件、接口数据或组件状态中的 payableOpenAmount 值。
   */
  payableOpenAmount: number
  /**
   * 字段 todos：表示表单、筛选条件、接口数据或组件状态中的 todos 值。
   */
  todos: WorkbenchTodo[]
  /**
   * 字段 risks：表示表单、筛选条件、接口数据或组件状态中的 risks 值。
   */
  risks: RiskAlert[]
  /**
   * 字段 accountingSuggestions：表示表单、筛选条件、接口数据或组件状态中的 accountingSuggestions 值。
   */
  accountingSuggestions: AccountingSuggestion[]
  /**
   * 字段 monthCloseChecks：表示表单、筛选条件、接口数据或组件状态中的 monthCloseChecks 值。
   */
  monthCloseChecks: MonthCloseCheck[]
}

/**
 * WorkbenchTodo 类型定义，用于约束页面状态、接口入参或接口返回数据结构。
 */
export interface WorkbenchTodo {
  /**
   * 字段 type：表示表单、筛选条件、接口数据或组件状态中的 type 值。
   */
  type: string
  /**
   * 字段 title：表示表单、筛选条件、接口数据或组件状态中的 title 值。
   */
  title: string
  /**
   * 字段 description：表示表单、筛选条件、接口数据或组件状态中的 description 值。
   */
  description: string
  /**
   * 字段 severity：表示表单、筛选条件、接口数据或组件状态中的 severity 值。
   */
  severity: 'success' | 'primary' | 'warning' | 'danger' | string
  /**
   * 字段 routePath：表示表单、筛选条件、接口数据或组件状态中的 routePath 值。
   */
  routePath?: string
  /**
   * 字段 searchKey：表示表单、筛选条件、接口数据或组件状态中的 searchKey 值。
   */
  searchKey?: string
  /**
   * 字段 searchValue：表示表单、筛选条件、接口数据或组件状态中的 searchValue 值。
   */
  searchValue?: string
}

/**
 * RiskAlert 类型定义，用于约束页面状态、接口入参或接口返回数据结构。
 */
export interface RiskAlert {
  /**
   * 字段 level：表示表单、筛选条件、接口数据或组件状态中的 level 值。
   */
  level: 'success' | 'primary' | 'warning' | 'danger' | string
  /**
   * 字段 title：表示表单、筛选条件、接口数据或组件状态中的 title 值。
   */
  title: string
  /**
   * 字段 description：表示表单、筛选条件、接口数据或组件状态中的 description 值。
   */
  description: string
  /**
   * 字段 routePath：表示表单、筛选条件、接口数据或组件状态中的 routePath 值。
   */
  routePath?: string
  /**
   * 字段 searchKey：表示表单、筛选条件、接口数据或组件状态中的 searchKey 值。
   */
  searchKey?: string
  /**
   * 字段 searchValue：表示表单、筛选条件、接口数据或组件状态中的 searchValue 值。
   */
  searchValue?: string
}

/**
 * AccountingSuggestion 类型定义，用于约束页面状态、接口入参或接口返回数据结构。
 */
export interface AccountingSuggestion {
  /**
   * 字段 sourceType：表示表单、筛选条件、接口数据或组件状态中的 sourceType 值。
   */
  sourceType: string
  /**
   * 字段 sourceNo：表示表单、筛选条件、接口数据或组件状态中的 sourceNo 值。
   */
  sourceNo: string
  /**
   * 字段 title：表示表单、筛选条件、接口数据或组件状态中的 title 值。
   */
  title: string
  /**
   * 字段 reason：表示表单、筛选条件、接口数据或组件状态中的 reason 值。
   */
  reason: string
  /**
   * 字段 amount：表示表单、筛选条件、接口数据或组件状态中的 amount 值。
   */
  amount: number
  /**
   * 字段 debitSubject：表示表单、筛选条件、接口数据或组件状态中的 debitSubject 值。
   */
  debitSubject: string
  /**
   * 字段 creditSubject：表示表单、筛选条件、接口数据或组件状态中的 creditSubject 值。
   */
  creditSubject: string
  /**
   * 字段 routePath：表示表单、筛选条件、接口数据或组件状态中的 routePath 值。
   */
  routePath?: string
  /**
   * 字段 searchKey：表示表单、筛选条件、接口数据或组件状态中的 searchKey 值。
   */
  searchKey?: string
  /**
   * 字段 searchValue：表示表单、筛选条件、接口数据或组件状态中的 searchValue 值。
   */
  searchValue?: string
}

/**
 * MonthCloseCheck 类型定义，用于约束页面状态、接口入参或接口返回数据结构。
 */
export interface MonthCloseCheck {
  /**
   * 字段 code：表示表单、筛选条件、接口数据或组件状态中的 code 值。
   */
  code: string
  /**
   * 字段 title：表示表单、筛选条件、接口数据或组件状态中的 title 值。
   */
  title: string
  /**
   * 字段 status：表示表单、筛选条件、接口数据或组件状态中的 status 值。
   */
  status: 'success' | 'warning' | 'danger' | string
  /**
   * 字段 description：表示表单、筛选条件、接口数据或组件状态中的 description 值。
   */
  description: string
  /**
   * 字段 routePath：表示表单、筛选条件、接口数据或组件状态中的 routePath 值。
   */
  routePath?: string
}

/**
 * SearchResult 类型定义，用于约束页面状态、接口入参或接口返回数据结构。
 */
export interface SearchResult {
  /**
   * 字段 id：表示表单、筛选条件、接口数据或组件状态中的 id 值。
   */
  id: number
  /**
   * 字段 type：表示表单、筛选条件、接口数据或组件状态中的 type 值。
   */
  type: string
  /**
   * 字段 sourceId：表示表单、筛选条件、接口数据或组件状态中的 sourceId 值。
   */
  sourceId?: number
  /**
   * 字段 sourceNo：表示表单、筛选条件、接口数据或组件状态中的 sourceNo 值。
   */
  sourceNo?: string
  /**
   * 字段 title：表示表单、筛选条件、接口数据或组件状态中的 title 值。
   */
  title: string
  /**
   * 字段 category：表示表单、筛选条件、接口数据或组件状态中的 category 值。
   */
  category: string
  /**
   * 字段 summary：表示表单、筛选条件、接口数据或组件状态中的 summary 值。
   */
  summary: string
  /**
   * 字段 content：表示表单、筛选条件、接口数据或组件状态中的 content 值。
   */
  content: string
  /**
   * 字段 score：表示表单、筛选条件、接口数据或组件状态中的 score 值。
   */
  score: number
  /**
   * 字段 routePath：表示表单、筛选条件、接口数据或组件状态中的 routePath 值。
   */
  routePath?: string
}

/**
 * SearchResponse 类型定义，用于约束页面状态、接口入参或接口返回数据结构。
 */
export interface SearchResponse {
  /**
   * 字段 keyword：表示表单、筛选条件、接口数据或组件状态中的 keyword 值。
   */
  keyword: string
  /**
   * 字段 mode：表示表单、筛选条件、接口数据或组件状态中的 mode 值。
   */
  mode: string
  /**
   * 字段 aiEnabled：表示表单、筛选条件、接口数据或组件状态中的 aiEnabled 值。
   */
  aiEnabled: boolean
  /**
   * 字段 total：表示表单、筛选条件、接口数据或组件状态中的 total 值。
   */
  total: number
  /**
   * 字段 rewrittenQueries：表示表单、筛选条件、接口数据或组件状态中的 rewrittenQueries 值。
   */
  rewrittenQueries: string[]
  /**
   * 字段 results：表示表单、筛选条件、接口数据或组件状态中的 results 值。
   */
  results: SearchResult[]
}

/**
 * InventoryView 类型定义，用于约束页面状态、接口入参或接口返回数据结构。
 */
export interface InventoryView {
  /**
   * 字段 id：表示表单、筛选条件、接口数据或组件状态中的 id 值。
   */
  id: number
  /**
   * 字段 movementNo：表示表单、筛选条件、接口数据或组件状态中的 movementNo 值。
   */
  movementNo: string
  /**
   * 字段 movementType：表示表单、筛选条件、接口数据或组件状态中的 movementType 值。
   */
  movementType: 'INBOUND' | 'OUTBOUND' | 'TRANSFER' | 'CHECK'
  /**
   * 字段 movementDate：表示表单、筛选条件、接口数据或组件状态中的 movementDate 值。
   */
  movementDate: string
  /**
   * 字段 projectCode：表示项目字典编码，用于区分库存流水所属项目。
   */
  projectCode?: string
  /**
   * 字段 projectName：表示项目名称快照，用于库存台账列表、表单和查看流水展示。
   */
  projectName?: string
  /**
   * 字段 itemCode：表示表单、筛选条件、接口数据或组件状态中的 itemCode 值。
   */
  itemCode: string
  /**
   * 字段 itemName：表示表单、筛选条件、接口数据或组件状态中的 itemName 值。
   */
  itemName: string
  /**
   * 字段 specification：表示表单、筛选条件、接口数据或组件状态中的 specification 值。
   */
  specification?: string
  /**
   * 字段 stockOrganization：表示表单、筛选条件、接口数据或组件状态中的 stockOrganization 值。
   */
  stockOrganization?: string
  /**
   * 字段 ownerName：表示表单、筛选条件、接口数据或组件状态中的 ownerName 值。
   */
  ownerName?: string
  /**
   * 字段 unitName：表示表单、筛选条件、接口数据或组件状态中的 unitName 值。
   */
  unitName?: string
  /**
   * 字段 batchNo：表示表单、筛选条件、接口数据或组件状态中的 batchNo 值。
   */
  batchNo?: string
  /**
   * 字段 quantity：表示表单、筛选条件、接口数据或组件状态中的 quantity 值。
   */
  quantity: number
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
  /**
   * 字段 sourceBillType：表示表单、筛选条件、接口数据或组件状态中的 sourceBillType 值。
   */
  sourceBillType?: string
  /**
   * 字段 remark：表示表单、筛选条件、接口数据或组件状态中的 remark 值。
   */
  remark?: string
  /**
   * 字段 organizationCode：表示表单、筛选条件、接口数据或组件状态中的 organizationCode 值。
   */
  organizationCode?: string
  /** 字段 voucherId：表示库存流水通过会计平台生成的凭证主键。 */
  voucherId?: number
  /** 字段 voucherNo：表示库存流水通过会计平台生成的凭证号。 */
  voucherNo?: string
  /**
   * 字段 attachmentCount：表示该库存流水绑定的附件数量，用于列表判断是否展示附件按钮。
   */
  attachmentCount?: number
}

/**
 * InventoryStockView 类型定义，用于约束页面状态、接口入参或接口返回数据结构。
 */
export interface InventoryStockView {
  /**
   * 字段 itemCode：表示表单、筛选条件、接口数据或组件状态中的 itemCode 值。
   */
  itemCode: string
  /**
   * 字段 itemName：表示表单、筛选条件、接口数据或组件状态中的 itemName 值。
   */
  itemName?: string
  /**
   * 字段 warehouse：表示表单、筛选条件、接口数据或组件状态中的 warehouse 值。
   */
  warehouse: string
  /**
   * 字段 asOfDate：表示表单、筛选条件、接口数据或组件状态中的 asOfDate 值。
   */
  asOfDate: string
  /**
   * 字段 availableQuantity：表示表单、筛选条件、接口数据或组件状态中的 availableQuantity 值。
   */
  availableQuantity: number
}

/**
 * InventoryMaterialStockView 类型定义，用于约束页面状态、接口入参或接口返回数据结构。
 */
export interface InventoryMaterialStockView {
  /**
   * 字段 itemCode：表示表单、筛选条件、接口数据或组件状态中的 itemCode 值。
   */
  itemCode: string
  /**
   * 字段 itemName：表示表单、筛选条件、接口数据或组件状态中的 itemName 值。
   */
  itemName: string
  /**
   * 字段 inboundQuantity：表示表单、筛选条件、接口数据或组件状态中的 inboundQuantity 值。
   */
  inboundQuantity: number
  /**
   * 字段 outboundQuantity：表示表单、筛选条件、接口数据或组件状态中的 outboundQuantity 值。
   */
  outboundQuantity: number
  /**
   * 字段 transferQuantity：表示表单、筛选条件、接口数据或组件状态中的 transferQuantity 值。
   */
  transferQuantity: number
  /**
   * 字段 stockQuantity：表示表单、筛选条件、接口数据或组件状态中的 stockQuantity 值。
   */
  stockQuantity: number
  /**
   * 字段 children：表示表单、筛选条件、接口数据或组件状态中的 children 值。
   */
  children: InventoryMaterialStockView[]
}

/**
 * ArApView 类型定义，用于约束页面状态、接口入参或接口返回数据结构。
 */
export interface ArApView {
  /**
   * 字段 id：表示表单、筛选条件、接口数据或组件状态中的 id 值。
   */
  id: number
  /**
   * 字段 billNo：表示表单、筛选条件、接口数据或组件状态中的 billNo 值。
   */
  billNo: string
  /**
   * 字段 billType：表示表单、筛选条件、接口数据或组件状态中的 billType 值。
   */
  billType: 'RECEIVABLE' | 'PAYABLE'
  /**
   * 字段 partnerName：表示表单、筛选条件、接口数据或组件状态中的 partnerName 值。
   */
  partnerName: string
  /**
   * 字段 projectCode：表示项目字典编码，用于区分应收应付单所属项目。
   */
  projectCode?: string
  /**
   * 字段 projectName：表示项目名称快照，用于应收应付列表、表单、查看流水和收付统计展示。
   */
  projectName?: string
  /**
   * 字段 documentType：表示表单、筛选条件、接口数据或组件状态中的 documentType 值。
   */
  documentType?: string
  /**
   * 字段 businessOrganization：表示表单、筛选条件、接口数据或组件状态中的 businessOrganization 值。
   */
  businessOrganization?: string
  /**
   * 字段 settlementOrganization：表示表单、筛选条件、接口数据或组件状态中的 settlementOrganization 值。
   */
  settlementOrganization?: string
  /**
   * 字段 paymentOrganization：表示表单、筛选条件、接口数据或组件状态中的 paymentOrganization 值。
   */
  paymentOrganization?: string
  /**
   * 字段 paymentTerms：表示表单、筛选条件、接口数据或组件状态中的 paymentTerms 值。
   */
  paymentTerms?: string
  /**
   * 字段 settlementMethod：表示表单、筛选条件、接口数据或组件状态中的 settlementMethod 值。
   */
  settlementMethod?: string
  /**
   * 字段 sourceBillType：表示表单、筛选条件、接口数据或组件状态中的 sourceBillType 值。
   */
  sourceBillType?: string
  /**
   * 字段 sourceBillNo：表示表单、筛选条件、接口数据或组件状态中的 sourceBillNo 值。
   */
  sourceBillNo?: string
  /**
   * 字段 billDate：表示表单、筛选条件、接口数据或组件状态中的 billDate 值。
   */
  billDate: string
  /**
   * 字段 dueDate：表示表单、筛选条件、接口数据或组件状态中的 dueDate 值。
   */
  dueDate: string
  /**
   * 字段 amount：表示表单、筛选条件、接口数据或组件状态中的 amount 值。
   */
  amount: number
  /**
   * 字段 paidAmount：表示表单、筛选条件、接口数据或组件状态中的 paidAmount 值。
   */
  paidAmount: number
  /**
   * 字段 remainingAmount：表示表单、筛选条件、接口数据或组件状态中的 remainingAmount 值。
   */
  remainingAmount: number
  /**
   * 字段 currencyCode：表示表单、筛选条件、接口数据或组件状态中的 currencyCode 值。
   */
  currencyCode: string
  /**
   * 字段 currencyName：表示表单、筛选条件、接口数据或组件状态中的 currencyName 值。
   */
  currencyName: string
  /**
   * 字段 exchangeRateToCny：表示表单、筛选条件、接口数据或组件状态中的 exchangeRateToCny 值。
   */
  exchangeRateToCny: number
  /**
   * 字段 amountCny：表示表单、筛选条件、接口数据或组件状态中的 amountCny 值。
   */
  amountCny: number
  /**
   * 字段 paidAmountCny：表示表单、筛选条件、接口数据或组件状态中的 paidAmountCny 值。
   */
  paidAmountCny: number
  /**
   * 字段 remainingAmountCny：表示表单、筛选条件、接口数据或组件状态中的 remainingAmountCny 值。
   */
  remainingAmountCny: number
  /**
   * 字段 status：表示表单、筛选条件、接口数据或组件状态中的 status 值。
   */
  status: 'OPEN' | 'PARTIAL' | 'CLOSED' | 'OVERDUE'
  /**
   * 字段 agingDays：表示表单、筛选条件、接口数据或组件状态中的 agingDays 值。
   */
  agingDays: number
  /**
   * 字段 paymentPlan：表示表单、筛选条件、接口数据或组件状态中的 paymentPlan 值。
   */
  paymentPlan?: string
  /**
   * 字段 organizationCode：表示表单、筛选条件、接口数据或组件状态中的 organizationCode 值。
   */
  organizationCode?: string
  /** 字段 voucherId：表示应收应付单通过会计平台生成的凭证主键。 */
  voucherId?: number
  /** 字段 voucherNo：表示应收应付单通过会计平台生成的凭证号。 */
  voucherNo?: string
  /**
   * 字段 attachmentCount：表示该应收应付单绑定的附件数量，用于列表判断是否展示附件按钮。
   */
  attachmentCount?: number
}

/**
 * ArApPaymentStatsRow 类型定义，用于约束收付统计中的单据行结构。
 */
export interface ArApPaymentStatsRow {
  /**
   * 字段 billNo：表示应收应付单号。
   */
  billNo: string
  /**
   * 字段 billType：表示单据类型，用于区分应收和应付。
   */
  billType: 'RECEIVABLE' | 'PAYABLE'
  /**
   * 字段 projectCode：表示项目字典编码。
   */
  projectCode?: string
  /**
   * 字段 projectName：表示项目名称快照。
   */
  projectName?: string
  /**
   * 字段 partnerName：表示客户或供应商名称。
   */
  partnerName: string
  /**
   * 字段 receivableAmount：表示该单据应收金额。
   */
  receivableAmount: number
  /**
   * 字段 payableAmount：表示该单据应付金额。
   */
  payableAmount: number
  /**
   * 字段 receivedAmount：表示该单据已收金额。
   */
  receivedAmount: number
  /**
   * 字段 paidAmount：表示该单据已付金额。
   */
  paidAmount: number
  /**
   * 字段 pendingReceivableAmount：表示该单据待收金额。
   */
  pendingReceivableAmount: number
  /**
   * 字段 pendingPayableAmount：表示该单据待付金额。
   */
  pendingPayableAmount: number
}

/**
 * ArApPaymentStatsView 类型定义，用于约束收付统计页面的汇总返回结构。
 */
export interface ArApPaymentStatsView {
  /**
   * 字段 rows：表示按应收应付单号统计的明细行。
   */
  rows: ArApPaymentStatsRow[]
  /**
   * 字段 totalReceivableAmount：表示全部应收金额汇总。
   */
  totalReceivableAmount: number
  /**
   * 字段 totalPayableAmount：表示全部应付金额汇总。
   */
  totalPayableAmount: number
  /**
   * 字段 totalReceivedAmount：表示全部已收金额汇总。
   */
  totalReceivedAmount: number
  /**
   * 字段 totalPaidAmount：表示全部已付金额汇总。
   */
  totalPaidAmount: number
  /**
   * 字段 totalPendingReceivableAmount：表示全部待收金额汇总。
   */
  totalPendingReceivableAmount: number
  /**
   * 字段 totalPendingPayableAmount：表示全部待付金额汇总。
   */
  totalPendingPayableAmount: number
}

/**
 * ArApSettlementView 类型定义，用于约束应收应付收付核销流水。
 */
export interface ArApSettlementView {
  /** 字段 id：表示核销流水主键。 */
  id: number
  /** 字段 billId：表示被核销的应收应付单主键。 */
  billId: number
  /** 字段 billNo：表示被核销的应收应付单号。 */
  billNo: string
  /** 字段 settlementDate：表示实际收款或付款日期。 */
  settlementDate: string
  /** 字段 amount：表示本次核销原币金额。 */
  amount: number
  /** 字段 amountCny：表示本次核销折人民币金额。 */
  amountCny: number
  /** 字段 settlementMethod：表示结算方式。 */
  settlementMethod?: string
  /** 字段 bankAccount：表示银行或现金账户。 */
  bankAccount?: string
  /** 字段 cashierTransactionNo：表示关联出纳流水号。 */
  cashierTransactionNo?: string
  /** 字段 remark：表示核销说明。 */
  remark?: string
}

/**
 * FinancialStatement 类型定义，用于约束页面状态、接口入参或接口返回数据结构。
 */
export interface FinancialStatement {
  /**
   * 字段 statementName：表示表单、筛选条件、接口数据或组件状态中的 statementName 值。
   */
  statementName: string
  /**
   * 字段 reportDate：表示表单、筛选条件、接口数据或组件状态中的 reportDate 值。
   */
  reportDate: string
  /**
   * 字段 lines：表示表单、筛选条件、接口数据或组件状态中的 lines 值。
   */
  lines: Array<{ itemName: string; amount: number }>
}

/**
 * AiAssistantResponse 类型定义，用于约束页面状态、接口入参或接口返回数据结构。
 */
export interface AiConversationMessage {
  /**
   * 字段 role：表示消息角色。
   */
  role: 'user' | 'assistant'
  /**
   * 字段 content：表示消息内容。
   */
  content: string
}

/**
 * AiAssistantContext 类型定义，用于约束 ratel助手会话上下文。
 */
export interface AiAssistantContext {
  /**
   * 字段 conversationSummary：表示会话短摘要。
   */
  conversationSummary?: string
  /**
   * 字段 conversationMessages：表示最近会话消息。
   */
  conversationMessages?: AiConversationMessage[]
}

export interface AiAssistantResponse {
  /**
   * 字段 question：表示表单、筛选条件、接口数据或组件状态中的 question 值。
   */
  question: string
  /**
   * 字段 answer：表示表单、筛选条件、接口数据或组件状态中的 answer 值。
   */
  answer: string
  /**
   * 字段 aiEnabled：表示表单、筛选条件、接口数据或组件状态中的 aiEnabled 值。
   */
  aiEnabled: boolean
  /**
   * 字段 model：表示表单、筛选条件、接口数据或组件状态中的 model 值。
   */
  model: string
  /**
   * 字段 mode：表示表单、筛选条件、接口数据或组件状态中的 mode 值。
   */
  mode: 'local' | 'web' | 'hybrid' | 'command'
  /**
   * 字段 citations：表示表单、筛选条件、接口数据或组件状态中的 citations 值。
   */
  citations: Array<{
    /**
     * 字段 id：表示表单、筛选条件、接口数据或组件状态中的 id 值。
     */
    id?: number
    /**
     * 字段 type：表示表单、筛选条件、接口数据或组件状态中的 type 值。
     */
    type: string
    /**
     * 字段 sourceId：表示表单、筛选条件、接口数据或组件状态中的 sourceId 值。
     */
    sourceId?: number
    /**
     * 字段 sourceNo：表示表单、筛选条件、接口数据或组件状态中的 sourceNo 值。
     */
    sourceNo?: string
    /**
     * 字段 title：表示表单、筛选条件、接口数据或组件状态中的 title 值。
     */
    title: string
    /**
     * 字段 category：表示表单、筛选条件、接口数据或组件状态中的 category 值。
     */
    category: string
    /**
     * 字段 summary：表示表单、筛选条件、接口数据或组件状态中的 summary 值。
     */
    summary: string
    /**
     * 字段 score：表示表单、筛选条件、接口数据或组件状态中的 score 值。
     */
    score: number
    /**
     * 字段 routePath：表示表单、筛选条件、接口数据或组件状态中的 routePath 值。
     */
    routePath?: string
    /**
     * 字段 url：表示表单、筛选条件、接口数据或组件状态中的 url 值。
     */
    url?: string
  }>
  /**
   * 字段 suggestions：表示表单、筛选条件、接口数据或组件状态中的 suggestions 值。
   */
  suggestions: string[]
  /**
   * 字段 conversationSummary：表示服务端更新后的会话短摘要。
   */
  conversationSummary?: string
  /**
   * 字段 recentRawRounds：表示服务端本次实际采用的最近原文轮次。
   */
  recentRawRounds?: number
}

/**
 * LocalKnowledgeDocumentView 类型定义，表示用户上传的本地知识库资料。
 */
export interface LocalKnowledgeDocumentView {
  /**
   * 字段 id：本地知识库资料主键，用于重建索引和删除资料。
   */
  id: number
  /**
   * 字段 title：用户维护的资料标题，未填写时后端取原始文件名。
   */
  title: string
  /**
   * 字段 description：资料说明，用于列表展示和辅助检索。
   */
  description?: string
  /**
   * 字段 originalName：上传时的原始文件名，用于用户识别资料来源。
   */
  originalName: string
  /**
   * 字段 suffix：文件后缀，前端用于展示文件类型，后端用于选择文本抽取或 OCR。
   */
  suffix: string
  /**
   * 字段 fileSize：文件大小，单位字节，用于列表格式化展示。
   */
  fileSize: number
  /**
   * 字段 status：资料入库状态，决定前端标签颜色和是否展示失败原因。
   */
  status: 'PENDING' | 'INDEXING' | 'INDEXED' | 'FAILED'
  /**
   * 字段 chunkCount：写入知识索引或向量库的分片数量。
   */
  chunkCount: number
  /**
   * 字段 ocrUsed：是否使用图片 OCR 或本地视觉模型识别。
   */
  ocrUsed: boolean
  /**
   * 字段 uploadedBy：上传人用户名快照，用于资料维护追溯。
   */
  uploadedBy?: string
  /**
   * 字段 organizationCode：所属公司编码，用于账套隔离排查。
   */
  organizationCode: string
  /**
   * 字段 errorMessage：入库失败时后端写入的用户可读原因。
   */
  errorMessage?: string
  /**
   * 字段 createdTime：资料上传记录创建时间。
   */
  createdTime: string
  /**
   * 字段 modifyTime：资料上传记录最近更新时间。
   */
  modifyTime: string
}

/**
 * AiComponentStatusItem 类型定义，表示单个 AI 组件的健康状态。
 */
export interface AiComponentStatusItem {
  /**
   * 字段 code：组件编码，用于前端稳定识别。
   */
  code: string
  /**
   * 字段 name：组件中文名称。
   */
  name: string
  /**
   * 字段 status：组件状态，取 UP、DOWN、WARN、DISABLED。
   */
  status: 'UP' | 'DOWN' | 'WARN' | 'DISABLED'
  /**
   * 字段 detail：组件状态说明。
   */
  detail: string
}

/**
 * AiComponentStatusResponse 类型定义，表示 AI 模型、向量库和知识索引的整体状态。
 */
export interface AiComponentStatusResponse {
  /**
   * 字段 modelProvider：当前大模型提供方。
   */
  modelProvider: string
  /**
   * 字段 vectorProvider：当前向量库提供方。
   */
  vectorProvider: string
  /**
   * 字段 primaryChatModel：普通问答主模型。
   */
  primaryChatModel: string
  /**
   * 字段 embeddingModel：知识索引用 embedding 模型。
   */
  embeddingModel: string
  /**
   * 字段 streamEnabled：ratel助手是否启用流式输出。
   */
  streamEnabled: boolean
  /**
   * 字段 agentEnabled：业务 Agent 是否启用；关闭时前端隐藏 Agent 入口并避免调用 Agent 接口。
   */
  agentEnabled: boolean
  /**
   * 字段 indexDocumentCount：当前知识索引分片数量。
   */
  indexDocumentCount: number
  /**
   * 字段 lastRebuildAt：最近一次索引成功重建时间。
   */
  lastRebuildAt?: string
  /**
   * 字段 lastRebuildError：最近一次索引失败原因。
   */
  lastRebuildError?: string
  /**
   * 字段 checkedAt：本次状态检查时间。
   */
  checkedAt: string
  /**
   * 字段 components：组件明细状态。
   */
  components: AiComponentStatusItem[]
  /**
   * 字段 sourceTypeCounts：按知识来源类型统计的当前索引分片数量。
   */
  sourceTypeCounts: Array<{
    /**
     * 字段 sourceType：知识来源类型。
     */
    sourceType: string
    /**
     * 字段 count：当前分片数量。
     */
    count: number
  }>
}

/**
 * KnowledgeRebuildResponse 类型定义，用于约束页面状态、接口入参或接口返回数据结构。
 */
export interface KnowledgeRebuildResponse {
  /**
   * 字段 documentCount：表示表单、筛选条件、接口数据或组件状态中的 documentCount 值。
   */
  documentCount: number
  /**
   * 字段 rebuiltAt：表示表单、筛选条件、接口数据或组件状态中的 rebuiltAt 值。
   */
  rebuiltAt: string
}

/**
 * OperationLogView 类型定义，用于约束页面状态、接口入参或接口返回数据结构。
 */
export interface OperationLogView {
  /**
   * 字段 id：表示表单、筛选条件、接口数据或组件状态中的 id 值。
   */
  id: number
  /**
   * 字段 operatorUsername：表示表单、筛选条件、接口数据或组件状态中的 operatorUsername 值。
   */
  operatorUsername?: string
  /**
   * 字段 operatorName：表示表单、筛选条件、接口数据或组件状态中的 operatorName 值。
   */
  operatorName?: string
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
   * 字段 operationTime：表示表单、筛选条件、接口数据或组件状态中的 operationTime 值。
   */
  operationTime: string
  /**
   * 字段 terminalType：表示表单、筛选条件、接口数据或组件状态中的 terminalType 值。
   */
  terminalType?: 'PC' | 'APP' | string
  /**
   * 字段 terminalIdentifier：表示表单、筛选条件、接口数据或组件状态中的 terminalIdentifier 值。
   */
  terminalIdentifier?: string
  /**
   * 字段 operationModule：表示表单、筛选条件、接口数据或组件状态中的 operationModule 值。
   */
  operationModule?: string
  /**
   * 字段 operationFunction：表示表单、筛选条件、接口数据或组件状态中的 operationFunction 值。
   */
  operationFunction?: string
  /**
   * 字段 action：表示表单、筛选条件、接口数据或组件状态中的 action 值。
   */
  action?: string
  /**
   * 字段 operationParameters：表示表单、筛选条件、接口数据或组件状态中的 operationParameters 值。
   */
  operationParameters?: string
  /**
   * 字段 success：表示表单、筛选条件、接口数据或组件状态中的 success 值。
   */
  success?: boolean
  /**
   * 字段 operationResult：表示表单、筛选条件、接口数据或组件状态中的 operationResult 值。
   */
  operationResult?: string
  /**
   * 字段 responseValue：表示表单、筛选条件、接口数据或组件状态中的 responseValue 值。
   */
  responseValue?: string
  /**
   * 字段 impact：表示表单、筛选条件、接口数据或组件状态中的 impact 值。
   */
  impact?: string
}

/**
 * OperationLogPage 类型定义，用于约束页面状态、接口入参或接口返回数据结构。
 */
export interface OperationLogPage {
  /**
   * 字段 rows：表示表单、筛选条件、接口数据或组件状态中的 rows 值。
   */
  rows: OperationLogView[]
  /**
   * 字段 total：表示表单、筛选条件、接口数据或组件状态中的 total 值。
   */
  total: number
}
