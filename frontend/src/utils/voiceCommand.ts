import type { Router } from 'vue-router'
import type { useAuthStore } from '@/stores/auth'
import { pageMenus } from '@/router/menuRoutes'
import type { MenuView } from '@/types/api'
import { evaluateVoiceCommandRisk } from '@/utils/voiceRiskPolicy'

/**
 * AuthStore 类型定义，复用登录授权 Store 的实际返回类型，确保语音命令只基于当前人员已加载的菜单权限执行。
 */
type AuthStore = ReturnType<typeof useAuthStore>

/**
 * VoiceCommandResultType 类型定义，用于约束语音命令执行后的提示级别。
 */
export type VoiceCommandResultType = 'success' | 'warning' | 'info' | 'error'

/**
 * VoiceCommandResult 类型定义，用于向语音组件返回命令是否被处理和中文反馈。
 */
export interface VoiceCommandResult {
  /**
   * 字段 handled：表示本次语音文本是否匹配到可执行命令。
   */
  handled: boolean
  /**
   * 字段 type：表示前端反馈消息的展示级别。
   */
  type: VoiceCommandResultType
  /**
   * 字段 message：表示展示给用户的语音操作结果。
   */
  message: string
}

/**
 * VoiceCommandContext 类型定义，保存语音命令执行所需的路由和授权上下文。
 */
export interface VoiceCommandContext {
  /**
   * 字段 router：用于语音打开页面或切换菜单时触发现有路由守卫。
   */
  router: Router
  /**
   * 字段 auth：用于读取当前人员授权菜单，避免语音跳转到无权访问页面。
   */
  auth: AuthStore
}

/**
 * PageAlias 类型定义，维护菜单路由和常见中文语音叫法之间的匹配关系。
 */
interface PageAlias {
  /**
   * 字段 path：表示 Vue Router 中的业务页面路径。
   */
  path: string
  /**
   * 字段 menuCode：表示访问该页面所需的菜单资源编码。
   */
  menuCode: string
  /**
   * 字段 names：表示用户可能说出的页面别名。
   */
  names: string[]
}

/**
 * FieldAssignment 类型定义，表示从语音文本中解析出的“字段=值”填表指令。
 */
interface FieldAssignment {
  /**
   * 字段 label：表示表单项展示标签，例如“供应商”“备注”。
   */
  label: string
  /**
   * 字段 value：表示要写入表单控件的识别文本。
   */
  value: string
}

/**
 * ButtonClickOptions 类型定义，约束语音点击现有页面按钮时的匹配范围和偏好。
 */
interface ButtonClickOptions {
  /**
   * 字段 labels：表示候选按钮文案，按语音意图生成。
   */
  labels: string[]
  /**
   * 字段 allowTableActions：表示是否允许匹配表格行内按钮；默认避免误点行内删除、编辑。
   */
  allowTableActions?: boolean
  /**
   * 字段 preferTableActions：表示在详情、编辑第一条等场景优先点击表格行内操作。
   */
  preferTableActions?: boolean
  /**
   * 字段 scope：表示限定按钮搜索的 DOM 范围，例如当前弹窗或全页面。
   */
  scope?: ParentNode
}

/**
 * 常量 pageAliases：保存固定路由的中文语音别名，菜单管理中动态菜单会在运行时继续补充。
 */
const pageAliases: PageAlias[] = [
  { path: '/dashboard', menuCode: 'PAGE_DASHBOARD', names: ['首页', '首页概览', '仪表盘', '看板'] },
  { path: '/users', menuCode: 'PAGE_USERS', names: ['人员管理', '用户管理', '用户', '人员'] },
  { path: '/roles', menuCode: 'PAGE_ROLES', names: ['角色管理', '角色'] },
  { path: '/menus', menuCode: 'PAGE_MENUS', names: ['菜单管理', '菜单'] },
  { path: '/basic-dictionaries', menuCode: 'PAGE_BASIC_DICTIONARIES', names: ['字典管理', '基础字典', '基础信息', '字典'] },
  { path: '/subjects', menuCode: 'PAGE_SUBJECTS', names: ['会计科目', '科目'] },
  { path: '/vouchers', menuCode: 'PAGE_VOUCHERS', names: ['凭证记账', '财务凭证', '凭证'] },
  { path: '/accounting-periods', menuCode: 'PAGE_ACCOUNTING_PERIODS', names: ['会计期间', '期间'] },
  { path: '/cashier', menuCode: 'PAGE_CASHIER', names: ['出纳管理', '出纳流水', '出纳'] },
  { path: '/accounting-platform', menuCode: 'PAGE_ACCOUNTING_PLATFORM', names: ['会计平台', '制证平台', '自动制证'] },
  { path: '/purchase-orders', menuCode: 'PAGE_PURCHASE', names: ['采购管理', '采购订单', '采购单', '采购'] },
  { path: '/shipments', menuCode: 'PAGE_SHIPMENTS', names: ['物流管理', '物流单', '运输单', '运输', '物流'] },
  { path: '/inventory', menuCode: 'PAGE_INVENTORY', names: ['库存台账', '库存流水', '库存管理', '库存'] },
  { path: '/ar-ap', menuCode: 'PAGE_AR_AP', names: ['应收应付', '应收单', '应付单', '往来单'] },
  { path: '/workflow-center', menuCode: 'PAGE_WORKFLOW_CENTER', names: ['审批中心', '流程审批', '待办审批'] },
  { path: '/workflow-configs', menuCode: 'PAGE_WORKFLOW_CONFIGS', names: ['流程管理', '流程配置'] },
  { path: '/workflow-definitions', menuCode: 'PAGE_WORKFLOW_DEFINITIONS', names: ['流程定义'] },
  { path: '/reports', menuCode: 'PAGE_REPORTS', names: ['统计报表', '财务报表', '报表'] },
  { path: '/assistant', menuCode: 'PAGE_ASSISTANT', names: ['ratel助手', 'AI助手', '智能助手', '助手'] },
  { path: '/ai-status', menuCode: 'PAGE_AI_STATUS', names: ['AI状态', '组件状态', '模型状态', '向量库状态', '索引状态'] },
  { path: '/search', menuCode: 'PAGE_SEARCH', names: ['智能检索', '检索', '搜索'] },
  { path: '/operation-logs', menuCode: 'PAGE_OPERATION_LOGS', names: ['日志管理', '操作日志', '日志'] }
]

/**
 * 常量 commandNoise：用于清理“请打开页面”等口语前缀，提高页面名称匹配稳定性。
 */
const commandNoise = /^(请|帮我|麻烦|系统)?(打开|进入|切换到|跳转到|跳到|转到|去到|去|查看|定位到|看一下|看下|看哈|看一哈|查一下|查下|查哈|查一哈|搜一下|搜下|搜哈|找一下|找下|找哈)?(一下|一哈|哈)?(菜单|模块|页面|功能)?/
/**
 * 常量 navigationWords：识别页面跳转类语音意图。
 */
const navigationWords = /(打开|进入|切换到|跳转到|跳到|转到|去到|去|查看|定位到|看一下|看下|看哈|看一哈)(菜单|模块|页面|功能)?/
/**
 * 常量 createWords：识别新增业务记录类语音意图。
 */
const createWords = /(新增|新建|添加|创建|整(一?个|一?条)|弄(一?个|一?条)|来(一?个|一?条)|加(一?个|一?条))/
/**
 * 常量 queryWords：识别查询、搜索和筛选类语音意图。
 */
const queryWords = /(查询|搜索|检索|筛选|查一下|查下|查哈|查一哈|搜一下|搜下|搜哈|找一下|找下|找哈)/
/**
 * 常量 saveWords：识别保存或提交当前表单类语音意图。
 */
const saveWords = /(保存|保存起|存起|提交保存|提交起)/
/**
 * 常量 resetWords：识别重置筛选条件或清空表单类语音意图。
 */
const resetWords = /(重置|清空|清一下|清下|清哈|清一哈)/
/**
 * 常量 exportWords：识别导出或下载类语音意图。
 */
const exportWords = /(导出|下载)/
/**
 * 常量 cancelWords：识别取消当前弹窗或关闭操作类语音意图。
 */
const cancelWords = /(取消|关闭弹窗|关掉弹窗|关一下|关哈|关一哈|关到|关了|算了|莫要了)/
/**
 * 常量 confirmWords：识别确认弹窗中的确认、确定、同意等二次确认命令。
 */
const confirmWords = /^(确认|确定|同意|要得|要的|好的|可以|行|好嘛|可以嘛|阔以|确认删除|确认删掉|确认删了|确认提交|确认保存|确认保存起|确认存起)$|确认(删除|删掉|删了|提交|提交起|保存|保存起|存起|操作)/

/**
 * 执行语音命令。
 *
 * 实现步骤：
 * 1. 解析语音文本中的页面、动作和填表字段；
 * 2. 页面跳转通过 Vue Router 执行，让现有路由守卫和菜单授权继续生效；
 * 3. 查询和打开表单可直接触发现有页面按钮，填表只写当前可见表单控件；
 * 4. 保存、提交、删除和二次确认必须使用“确认保存/确认删除”等明确口令，不在填表后自动提交；
 * 5. 返回中文执行结果给悬浮语音组件展示。
 */
export async function executeVoiceCommand(context: VoiceCommandContext, transcript: string): Promise<VoiceCommandResult> {
  /**
   * 常量 text：保存去除首尾空白后的语音转写文本。
   */
  const text = transcript.trim()
  if (!text) {
    return result(false, 'info', '未识别到有效语音')
  }

  if (/^(停止|关闭|退出)(语音|识别|语音识别|语音操作)$/.test(normalizeText(text))) {
    return result(true, 'info', '语音识别已停止')
  }

  /**
   * 常量 risk：集中评估语音文本中的写操作和确认风险。
   */
  const risk = evaluateVoiceCommandRisk(text)
  /**
   * 常量 target：保存语音中匹配到的授权页面目标。
   */
  const target = findRouteTarget(context.auth, text)
  /**
   * 常量 assignmentsBeforeAction：保存语音中解析出的填表字段和值。
   */
  const assignmentsBeforeAction = extractFieldAssignments(text)
  /**
   * 常量 createIntent：表示当前语音是否包含新增或创建意图。
   */
  const createIntent = createWords.test(text)
  if (target && shouldNavigate(text, createIntent, assignmentsBeforeAction.length > 0, context.router.currentRoute.value.path, target.path)) {
    await context.router.push(target.path)
    await waitForUi()
    if (!createIntent && assignmentsBeforeAction.length === 0 && !hasActionIntent(text)) {
      return result(true, 'success', `已打开${target.name}`)
    }
  }

  if (risk.explicitWriteConfirmation) {
    return executeExplicitWriteConfirmation(text)
  }

  /**
   * 常量 fillThenSaveIntent：表示“先填表、再保存”的连贯语音，允许填表但不自动提交。
   */
  const fillThenSaveIntent = assignmentsBeforeAction.length > 0 && saveWords.test(text)
  if ((risk.requiresManualCheck || confirmWords.test(text) || isWriteIntent(text)) && !fillThenSaveIntent) {
    return guardedWriteResult(text)
  }

  if (createIntent) {
    /**
     * 常量 createResult：保存点击新增类按钮后的执行结果。
     */
    const createResult = await clickButtonResultAsync({
      labels: createButtonLabels(text, target?.name),
      allowTableActions: false
    }, '已打开新增窗口')
    if (!createResult.handled) {
      return createResult
    }
    await waitForUi()
  }

  if (assignmentsBeforeAction.length > 0) {
    /**
     * 常量 filled：保存批量填表后的执行结果。
     */
    const filled = await fillAssignments(assignmentsBeforeAction)
    if (filled.handled && saveWords.test(text)) {
      return result(true, 'warning', '已填充表单，请检查内容后再说“确认保存”')
    }
    return filled
  }

  if (/^(输入|填入|录入|输一下|输一哈|写入)/.test(text)) {
    /**
     * 常量 value：保存要写入当前焦点输入框的纯文本。
     */
    const value = cleanFieldValue(text.replace(/^(输入|填入|录入|输一下|输一哈|写入)/, ''))
    if (value && fillActiveElement(value)) {
      return result(true, 'success', '已填入当前输入框')
    }
  }

  if (queryWords.test(text)) {
    return clickButtonResult({ labels: ['查询', '搜索', '检索', '执行检索'], allowTableActions: false }, '已执行查询')
  }

  if (resetWords.test(text)) {
    return clickButtonResult({ labels: ['重置', '清空'], allowTableActions: false }, '已重置')
  }

  if (exportWords.test(text)) {
    return clickButtonResult({ labels: ['导出', '下载'], allowTableActions: false }, '已触发导出')
  }

  if (/(编辑|修改)(第一条|当前|这一条|第[一二三四五六七八九十\d]+条)?/.test(text)) {
    return clickEdit(text)
  }

  if (/详情|查看明细|查看流水|在线凭证/.test(text)) {
    return clickButtonResult({ labels: ['详情', '查看明细', '查看流水', '在线凭证'], allowTableActions: true, preferTableActions: true }, '已打开详情')
  }

  if (cancelWords.test(text)) {
    return clickButtonResult({ labels: ['取消', '关闭'], scope: activeDialogOrBody(), allowTableActions: false }, '已取消')
  }

  if (target && navigationWords.test(text)) {
    await context.router.push(target.path)
    return result(true, 'success', `已打开${target.name}`)
  }

  return result(false, 'warning', '未匹配到可执行的语音指令')
}

/**
 * 判断当前语音是否需要先跳转页面。
 *
 * 实现步骤：
 * 1. 当前页面已经是目标页面时不重复跳转；
 * 2. 打开、进入、新增和跨页面填表类命令需要先进入目标页面；
 * 3. 普通保存、查询等命令只作用于当前页面。
 */
function shouldNavigate(text: string, hasCreateIntent: boolean, hasAssignments: boolean, currentPath: string, targetPath: string) {
  if (currentPath === targetPath) {
    return false
  }
  return navigationWords.test(text) || hasCreateIntent || hasAssignments
}

/**
 * 判断语音是否包含业务动作词。
 *
 * 实现步骤：检查新增、查询、保存、重置和导出关键词，供页面跳转后判断是否继续执行按钮动作。
 */
function hasActionIntent(text: string) {
  return createWords.test(text) || queryWords.test(text) || saveWords.test(text) || resetWords.test(text) || exportWords.test(text)
}

/**
 * 判断语音是否属于写操作意图。
 *
 * 实现步骤：识别保存、提交、删除、批量删除和二次确认类词语，用于在未明确确认前阻止执行。
 */
function isWriteIntent(text: string) {
  return saveWords.test(text) || /提交|提交起|删除|删掉|删了|删哈|删咯|批量删除|批量删掉|批量删了|删除选中|删除已选|删除选择/.test(text)
}

/**
 * 判断语音是否是明确写操作确认口令。
 *
 * 实现步骤：只接受“确认保存、确认提交、确认删除、确认批量删除、确认修改、确认新增”等强确认表达，不接受单独“确认”。
 */
function isExplicitWriteConfirmation(text: string) {
  return /^确认(保存|保存起|存起|提交|提交起|删除|删掉|删了|批量删除|批量删掉|批量删了|修改|改了|新增|核销|过账|作废|取消)$/.test(normalizeText(text))
}

/**
 * 执行明确确认后的写操作。
 *
 * 实现步骤：
 * 1. 确认删除只在删除确认弹窗已经出现时点击弹窗确认按钮；
 * 2. 确认批量删除在没有弹窗时只打开批量删除确认框，有弹窗时才最终确认；
 * 3. 确认保存或提交只点击当前弹窗/页面中的保存、提交按钮；
 * 4. 仍然只触发现有按钮，让页面校验和后端权限兜底继续生效。
 */
function executeExplicitWriteConfirmation(text: string) {
  /**
   * 常量 normalizedText：保存标准化后的确认口令。
   */
  const normalizedText = normalizeText(text)
  if (/删除|删掉|删了|删哈|删咯/.test(normalizedText)) {
    if (/批量/.test(normalizedText)) {
      /**
       * 常量 dialogScope：保存当前批量删除确认弹窗。
       */
      const dialogScope = activeDialogOnly()
      if (dialogScope) {
        if (!dialogContainsDeleteIntent(dialogScope)) {
          return result(true, 'warning', '当前弹窗不是删除确认框，未执行删除确认')
        }
        return clickButtonResult({ labels: ['确认删除', '确定', '确认'], scope: dialogScope, allowTableActions: false }, '已按确认口令触发批量删除')
      }
      return clickButtonResult({ labels: ['批量删除', '删除选中'], allowTableActions: false }, '已打开批量删除确认，请检查后再说“确认批量删除”')
    }
    /**
     * 常量 dialogScope：保存当前最上层弹窗，只有弹窗存在时才允许最终确认删除。
     */
    const dialogScope = activeDialogOnly()
    if (!dialogScope) {
      return result(true, 'warning', '请先打开删除确认框，检查对象后再说“确认删除”')
    }
    if (!dialogContainsDeleteIntent(dialogScope)) {
      return result(true, 'warning', '当前弹窗不是删除确认框，未执行删除确认')
    }
    return clickButtonResult({ labels: ['确认删除', '确定', '确认'], scope: dialogScope, allowTableActions: false }, '已按确认口令触发删除')
  }
  return clickButtonResult({ labels: ['保存核销', '保存', '提交'], allowTableActions: false }, '已按确认口令提交')
}

/**
 * 拦截未确认的写操作语音。
 *
 * 实现步骤：
 * 1. 删除类命令最多帮助打开现有删除确认框，不点击确认按钮；
 * 2. 保存和提交类命令只提示用户检查表单；
 * 3. 只有后续明确“确认保存/确认删除”才进入写操作确认分支。
 */
function guardedWriteResult(text: string): VoiceCommandResult {
  if (/批量删除|批量删掉|批量删了|删除选中|删除已选|删除选择/.test(text)) {
    return clickButtonResult({ labels: ['批量删除', '删除选中'], allowTableActions: false }, '已打开批量删除确认，请检查后再说“确认批量删除”')
  }
  if (/删除|删掉|删了|删哈|删咯/.test(text)) {
    if (/第一条|当前|这一条|第[一二三四五六七八九十\d]+条/.test(text)) {
      return clickButtonResult({ labels: ['删除'], allowTableActions: true, preferTableActions: true }, '已打开删除确认，请检查后再说“确认删除”')
    }
    return result(true, 'warning', '删除操作需要先说明范围，例如“删除选中”或“删除第一条”；确认框出现后再说“确认删除”')
  }
  if (/提交|提交起|保存|保存起|存起/.test(text)) {
    return result(true, 'warning', '请先检查表单内容，确认无误后再说“确认保存”或“确认提交”')
  }
  return result(true, 'warning', '写操作需要明确确认，请检查内容后说“确认保存”或“确认删除”')
}

/**
 * 查找语音文本对应的授权页面。
 *
 * 实现步骤：
 * 1. 合并后端授权菜单和前端固定别名；
 * 2. 清理口语前缀后按页面名称、别名和完整语音计算匹配分；
 * 3. 只返回达到阈值的目标，避免误跳转。
 */
function findRouteTarget(auth: AuthStore, text: string) {
  /**
   * 常量 candidates：保存当前人员有权访问的页面候选。
   */
  const candidates = authorizedRouteCandidates(auth)
  /**
   * 常量 phrase：保存清理口语前缀后的页面候选文本。
   */
  const phrase = normalizeText(text.replace(commandNoise, ''))
  /**
   * 常量 normalizedText：保存完整语音的标准化文本，兜底匹配“打开采购管理并新增”这类表达。
   */
  const normalizedText = normalizeText(text)
  /**
   * 变量 best：保存当前分数最高的页面匹配结果。
   */
  let best: { path: string; name: string; score: number } | undefined

  for (const candidate of candidates) {
    for (const name of candidate.names) {
      /**
       * 常量 score：保存当前候选页面名称与语音文本的匹配分。
       */
      const score = routeScore(phrase, normalizedText, normalizeText(name))
      if (score > (best?.score || 0)) {
        best = { path: candidate.path, name, score }
      }
    }
  }

  return best && best.score >= 58 ? best : undefined
}

/**
 * 组装当前人员有权访问的路由候选。
 *
 * 实现步骤：
 * 1. 优先读取后端返回的授权 PAGE 菜单，保证菜单管理新增页面也能被语音打开；
 * 2. 合并前端固定别名，补充“采购单”“凭证”等口语表达；
 * 3. 过滤未授权页面，语音入口不扩大现有菜单权限边界。
 */
function authorizedRouteCandidates(auth: AuthStore) {
  /**
   * 常量 dynamicMenus：保存后端菜单管理返回的动态页面路由。
   */
  const dynamicMenus = auth.menus
    .filter((menu): menu is MenuView & { routePath: string } => menu.type === 'PAGE' && Boolean(menu.routePath) && auth.hasMenu(menu.code))
    .map((menu) => ({
      path: menu.routePath,
      names: [menu.name, menu.routePath.replace(/^\//, '')]
    }))

  /**
   * 常量 dynamicPaths：用于避免动态菜单和固定别名重复生成候选。
   */
  const dynamicPaths = new Set(dynamicMenus.map((item) => item.path))
  /**
   * 常量 staticMenus：保存前端固定路由别名中当前人员有权访问的部分。
   */
  const staticMenus = pageAliases
    .filter((item) => auth.hasMenu(item.menuCode) || pageMenus.some((menu) => menu.path === item.path && auth.hasMenu(menu.menuCode)))
    .map((item) => ({ path: item.path, names: item.names }))

  return [
    ...dynamicMenus.map((item) => ({
      ...item,
      names: [...item.names, ...(pageAliases.find((alias) => alias.path === item.path)?.names || [])]
    })),
    ...staticMenus.filter((item) => !dynamicPaths.has(item.path))
  ]
}

/**
 * 计算页面名称与语音文本的匹配分。
 *
 * 实现步骤：完全匹配优先，其次是清理后的短语包含页面名，再其次使用完整语音兜底。
 */
function routeScore(phrase: string, fullText: string, name: string) {
  if (!name) {
    return 0
  }
  if (phrase === name || fullText === name) {
    return 100
  }
  if (phrase.includes(name)) {
    return 90 - Math.min(20, phrase.length - name.length)
  }
  if (fullText.includes(name)) {
    return 76 - Math.min(18, fullText.length - name.length)
  }
  if (name.includes(phrase) && phrase.length >= 2) {
    return 62 + phrase.length
  }
  return 0
}

/**
 * 生成新增类按钮候选文案。
 *
 * 实现步骤：
 * 1. 基础候选覆盖“新增、新建、添加、创建”；
 * 2. 从语音中提取对象名，例如“采购单”，提高精确按钮匹配；
 * 3. 页面名称存在时追加“新增当前页面名称”的候选。
 */
function createButtonLabels(text: string, targetName?: string) {
  /**
   * 常量 labels：保存按钮匹配候选文案，顺序越靠前越优先。
   */
  const labels = ['新增', '新建', '添加', '创建']
  /**
   * 常量 objectName：保存语音动作后的业务对象名称。
   */
  const objectName = actionObjectName(text)
  if (objectName) {
    labels.unshift(`新增${objectName}`, `新建${objectName}`, `添加${objectName}`)
  }
  if (targetName) {
    labels.unshift(`新增${targetName}`, `新建${targetName}`)
  }
  return labels
}

/**
 * 从新增类语音中提取业务对象名称。
 *
 * 实现步骤：匹配“新增采购单”中的“采购单”，并截断后续连接词后的内容。
 */
function actionObjectName(text: string) {
  /**
   * 常量 match：保存新增类动词后的对象名匹配结果。
   */
  const match = text.match(/(?:新增|新建|添加|创建)([\u4e00-\u9fa5A-Za-z0-9_-]{1,12})/)
  return match?.[1]?.replace(/(并|然后|同时|把|将).*$/, '') || ''
}

/**
 * 批量填充语音解析出的表单字段。
 *
 * 实现步骤：
 * 1. 逐个按表单标签查找当前可见表单项；
 * 2. 根据控件类型写入输入框、下拉框或开关；
 * 3. 汇总成功和失败字段，给用户明确反馈。
 */
async function fillAssignments(assignments: FieldAssignment[]): Promise<VoiceCommandResult> {
  /**
   * 变量 filledCount：累计已成功填充的字段数量。
   */
  let filledCount = 0
  /**
   * 常量 missed：保存未找到或未能写入的字段标签。
   */
  const missed: string[] = []
  for (const assignment of assignments) {
    /**
     * 常量 filled：保存单个字段是否成功写入。
     */
    const filled = await fillFormField(assignment.label, assignment.value)
    if (filled) {
      filledCount += 1
    } else {
      missed.push(assignment.label)
    }
  }
  if (filledCount > 0 && missed.length === 0) {
    return result(true, 'success', `已填充 ${filledCount} 个字段`)
  }
  if (filledCount > 0) {
    return result(true, 'warning', `已填充 ${filledCount} 个字段，未找到：${missed.join('、')}`)
  }
  return result(false, 'warning', `未找到字段：${missed.join('、')}`)
}

/**
 * 从语音文本中解析字段填充值。
 *
 * 实现步骤：
 * 1. 读取当前页面可见表单标签作为字段词典；
 * 2. 按逗号、句号和分号切分多字段语音；
 * 3. 识别“字段填为值”“字段是值”等表达并去重。
 */
function extractFieldAssignments(text: string): FieldAssignment[] {
  /**
   * 常量 labels：保存当前可见表单项标签，按长度倒序避免短标签抢匹配。
   */
  const labels = visibleFormLabels()
  if (labels.length === 0) {
    return []
  }

  /**
   * 常量 normalizedSegments：保存拆分后的语音片段，每段最多解析一个字段。
   */
  const normalizedSegments = text
    .replace(/[，。；;]/g, ',')
    .split(',')
    .map((item) => item.trim())
    .filter(Boolean)

  /**
   * 常量 assignments：保存从语音片段中解析出的字段和值。
   */
  const assignments: FieldAssignment[] = []
  for (const segment of normalizedSegments) {
    for (const label of labels) {
      /**
       * 常量 escaped：保存转义后的表单标签，避免正则特殊字符影响匹配。
       */
      const escaped = escapeRegExp(label)
      /**
       * 常量 match：保存当前片段对某个表单标签的赋值匹配。
       */
      const match = segment.match(new RegExp(`(?:把|将)?\\s*${escaped}\\s*(?:填成|填为|填写为|设置为|设为|改成|改为|弄成|弄为|弄到|整成|整为|整到|输入|录入|写入|填|是|为|=|：|:)\\s*(.+)$`))
      /**
       * 常量 value：保存清理动作词后的字段值。
       */
      const value = cleanFieldValue(match?.[1] || '')
      if (value) {
        assignments.push({ label, value })
        break
      }
    }
  }
  return dedupeAssignments(assignments)
}

/**
 * 读取当前页面所有可见表单标签。
 *
 * 实现步骤：遍历 Element Plus 表单项，清理冒号和必填星号，再按长度倒序返回。
 */
function visibleFormLabels() {
  return visibleElements('.el-form-item')
    .map((item) => item.querySelector<HTMLElement>('.el-form-item__label')?.innerText || '')
    .map((item) => item.replace(/[:：*]/g, '').trim())
    .filter((item) => item && item !== ' ')
    .sort((left, right) => right.length - left.length)
}

/**
 * 对字段赋值指令按标签去重。
 *
 * 实现步骤：标准化字段标签后保留第一次出现的赋值，避免同一语音片段重复命中。
 */
function dedupeAssignments(assignments: FieldAssignment[]) {
  /**
   * 常量 seen：保存已经处理过的字段标签。
   */
  const seen = new Set<string>()
  return assignments.filter((item) => {
    /**
     * 常量 key：保存标准化后的字段标签，用于去重。
     */
    const key = normalizeText(item.label)
    if (seen.has(key)) {
      return false
    }
    seen.add(key)
    return true
  })
}

/**
 * 清理字段值中的连接词和后续动作词。
 *
 * 实现步骤：移除“为、成、到”等口语前缀，并截掉末尾“并保存、并查询”等动作尾巴。
 */
function cleanFieldValue(value: string) {
  return value
    .replace(/^(为|成|到)\s*/, '')
    .replace(/\s*(并)?(保存|保存起|存起|提交|提交起|查询|搜索|检索|查哈|查一哈|搜哈|找哈)\s*$/, '')
    .trim()
}

/**
 * 根据表单标签写入具体控件。
 *
 * 实现步骤：
 * 1. 查找当前可见表单项；
 * 2. 优先处理开关和下拉框；
 * 3. 最后写入 input 或 textarea 并触发 Vue 的 input/change 事件。
 */
async function fillFormField(label: string, value: string) {
  /**
   * 常量 item：保存匹配到的 Element Plus 表单项 DOM。
   */
  const item = findFormItem(label)
  if (!item) {
    return false
  }

  /**
   * 常量 switchElement：保存当前字段下的开关控件。
   */
  const switchElement = item.querySelector<HTMLElement>('.el-switch')
  if (switchElement) {
    return fillSwitch(switchElement, value)
  }

  /**
   * 常量 selectElement：保存当前字段下的下拉选择控件。
   */
  const selectElement = item.querySelector<HTMLElement>('.el-select')
  if (selectElement) {
    return fillSelect(selectElement, value)
  }

  /**
   * 常量 input：保存当前字段下的文本输入控件。
   */
  const input = item.querySelector<HTMLInputElement | HTMLTextAreaElement>('textarea,input')
  if (input && !input.disabled && !input.readOnly) {
    setNativeValue(input, value)
    return true
  }

  return false
}

/**
 * 按标签查找当前可见表单项。
 *
 * 实现步骤：标准化语音标签和页面标签后做完全匹配和包含匹配，兼容“客户供应商”等口语省略。
 */
function findFormItem(label: string) {
  /**
   * 常量 normalizedLabel：保存标准化后的语音字段标签。
   */
  const normalizedLabel = normalizeText(label)
  return visibleElements('.el-form-item').find((item) => {
    /**
     * 常量 itemLabel：保存页面表单项展示标签。
     */
    const itemLabel = item.querySelector<HTMLElement>('.el-form-item__label')?.innerText || ''
    /**
     * 常量 normalizedItemLabel：保存标准化后的页面表单项标签。
     */
    const normalizedItemLabel = normalizeText(itemLabel)
    return normalizedItemLabel === normalizedLabel || normalizedItemLabel.includes(normalizedLabel) || normalizedLabel.includes(normalizedItemLabel)
  })
}

/**
 * 按语音值设置开关控件。
 *
 * 实现步骤：把“启用、开启、是”等值识别为 true，只在目标状态和当前状态不一致时点击开关。
 */
function fillSwitch(switchElement: HTMLElement, value: string) {
  /**
   * 常量 desired：保存语音值对应的目标布尔状态。
   */
  const desired = /^(启用|开启|打开|是|真|对|yes|true|1)$/i.test(normalizeText(value))
  /**
   * 常量 checked：保存 Element Plus 开关当前状态。
   */
  const checked = switchElement.classList.contains('is-checked')
  if (desired !== checked) {
    switchElement.click()
  }
  return true
}

/**
 * 按语音值选择下拉框选项。
 *
 * 实现步骤：
 * 1. 打开当前下拉框；
 * 2. 先在可见选项中按完全匹配和包含匹配查找；
 * 3. 如果是可筛选下拉框，则输入关键词后再匹配过滤结果。
 */
async function fillSelect(selectElement: HTMLElement, value: string) {
  /**
   * 常量 trigger：保存用于打开 Element Plus 下拉框的触发节点。
   */
  const trigger = selectElement.querySelector<HTMLElement>('.el-select__wrapper') || selectElement
  trigger.click()
  await sleep(120)

  /**
   * 常量 option：保存打开下拉后直接匹配到的选项。
   */
  const option = findVisibleOption(value)
  if (option) {
    option.click()
    return true
  }

  /**
   * 常量 input：保存可筛选下拉框内部输入框。
   */
  const input = selectElement.querySelector<HTMLInputElement>('input')
  if (input && !input.disabled && !input.readOnly) {
    setNativeValue(input, value)
    await sleep(120)
    /**
     * 常量 filteredOption：保存输入过滤词后匹配到的选项。
     */
    const filteredOption = findVisibleOption(value)
    if (filteredOption) {
      filteredOption.click()
      return true
    }
  }

  return false
}

/**
 * 在当前打开的下拉框中查找可见选项。
 *
 * 实现步骤：排除禁用选项后，按完全匹配、选项包含语音值、语音值包含选项三种方式兜底。
 */
function findVisibleOption(value: string) {
  /**
   * 常量 normalizedValue：保存标准化后的语音选项值。
   */
  const normalizedValue = normalizeText(value)
  /**
   * 常量 options：保存当前页面可见且可选择的下拉选项。
   */
  const options = visibleElements('.el-select-dropdown__item').filter((item) => !item.classList.contains('is-disabled'))
  return options.find((item) => normalizeText(item.innerText) === normalizedValue)
    || options.find((item) => normalizeText(item.innerText).includes(normalizedValue))
    || options.find((item) => normalizedValue.includes(normalizeText(item.innerText)))
}

/**
 * 向当前获得焦点的输入框写入语音内容。
 *
 * 实现步骤：只允许写入非禁用、非只读的 input 或 textarea，避免误改按钮和其他控件。
 */
function fillActiveElement(value: string) {
  /**
   * 常量 active：保存浏览器当前焦点元素。
   */
  const active = document.activeElement
  if (active instanceof HTMLInputElement || active instanceof HTMLTextAreaElement) {
    if (!active.disabled && !active.readOnly) {
      setNativeValue(active, value)
      return true
    }
  }
  return false
}

/**
 * 以原生 setter 写入输入框并通知 Vue。
 *
 * 实现步骤：调用 HTMLInputElement/HTMLTextAreaElement 原型上的 value setter，再派发 input 和 change 事件。
 */
function setNativeValue(element: HTMLInputElement | HTMLTextAreaElement, value: string) {
  element.focus()
  /**
   * 常量 prototype：保存当前控件类型对应的原型对象。
   */
  const prototype = element instanceof HTMLTextAreaElement ? HTMLTextAreaElement.prototype : HTMLInputElement.prototype
  /**
   * 常量 valueSetter：保存原生 value setter，避免只改 DOM 属性不触发 Vue v-model。
   */
  const valueSetter = Object.getOwnPropertyDescriptor(prototype, 'value')?.set
  valueSetter?.call(element, value)
  element.dispatchEvent(new Event('input', { bubbles: true }))
  element.dispatchEvent(new Event('change', { bubbles: true }))
}

/**
 * 处理编辑类语音命令。
 *
 * 实现步骤：带“第一条、当前”时允许点击表格行内编辑；否则只匹配页面级编辑按钮，降低误操作概率。
 */
function clickEdit(text: string) {
  if (/第一条|当前|这一条|第[一二三四五六七八九十\d]+条/.test(text)) {
    return clickButtonResult({ labels: ['编辑', '修改'], allowTableActions: true, preferTableActions: true }, '已打开编辑窗口')
  }
  return clickButtonResult({ labels: ['编辑', '修改'], allowTableActions: false }, '已打开编辑窗口')
}

/**
 * 等待界面渲染后点击按钮。
 *
 * 实现步骤：用于新增弹窗等异步渲染场景，先等待短暂 UI 更新，再复用普通按钮点击逻辑。
 */
async function clickButtonResultAsync(options: ButtonClickOptions, successMessage: string) {
  await waitForUi()
  return clickButtonResult(options, successMessage)
}

/**
 * 点击最匹配的现有页面按钮并返回执行结果。
 *
 * 实现步骤：
 * 1. 在限定范围内查找候选按钮；
 * 2. 未找到时返回 warning；
 * 3. 找到后触发 click，让页面原有权限、校验和确认逻辑继续工作。
 */
function clickButtonResult(options: ButtonClickOptions, successMessage: string): VoiceCommandResult {
  /**
   * 常量 button：保存按语音意图匹配到的最佳按钮。
   */
  const button = findBestButton(options)
  if (!button) {
    return result(false, 'warning', '当前页面未找到可点击按钮')
  }
  button.click()
  return result(true, 'success', successMessage)
}

/**
 * 查找当前意图下分数最高的按钮。
 *
 * 实现步骤：调用匹配函数获取排序后的候选列表，并返回第一项。
 */
function findBestButton(options: ButtonClickOptions) {
  /**
   * 常量 matches：保存按匹配分倒序排列的按钮列表。
   */
  const matches = matchingButtons(options)
  return matches[0]
}

/**
 * 查找并排序候选按钮。
 *
 * 实现步骤：
 * 1. 取当前弹窗或主工作区作为搜索范围；
 * 2. 标准化候选按钮文案；
 * 3. 计算每个按钮得分，过滤零分后按高分优先返回。
 */
function matchingButtons(options: ButtonClickOptions) {
  /**
   * 常量 scope：保存按钮搜索范围。
   */
  const scope = options.scope || activeDialogOrWorkspace()
  /**
   * 常量 labels：保存标准化后的按钮候选文案。
   */
  const labels = options.labels.map(normalizeText).filter(Boolean)
  return visibleButtons(scope)
    .map((button) => ({ button, score: buttonScore(button, labels, options) }))
    .filter((item) => item.score > 0)
    .sort((left, right) => right.score - left.score)
    .map((item) => item.button)
}

/**
 * 计算按钮与语音动作的匹配分。
 *
 * 实现步骤：
 * 1. 排除语音控件自身按钮；
 * 2. 按配置控制是否允许表格行内按钮；
 * 3. 根据按钮文本完全匹配、包含匹配和所在区域加权；
 * 4. 禁用按钮返回零分。
 */
function buttonScore(button: HTMLElement, labels: string[], options: ButtonClickOptions) {
  if (button.closest('.voice-command-widget')) {
    return 0
  }
  /**
   * 常量 inTableActions：表示按钮是否位于表格操作列中。
   */
  const inTableActions = Boolean(button.closest('.table-actions') || button.closest('.el-table'))
  if (inTableActions && !options.allowTableActions) {
    return 0
  }

  /**
   * 常量 text：保存按钮可读文案，优先取 innerText，兜底取辅助属性。
   */
  const text = normalizeText(button.innerText || button.getAttribute('aria-label') || button.getAttribute('title') || '')
  if (!text) {
    return 0
  }

  /**
   * 变量 score：保存当前按钮对语音意图的匹配分。
   */
  let score = 0
  for (const label of labels) {
    if (text === label) {
      score = Math.max(score, 100)
    } else if (text.includes(label)) {
      score = Math.max(score, 78)
    } else if (label.includes(text) && text.length >= 2) {
      score = Math.max(score, 58)
    }
  }
  if (score === 0) {
    return 0
  }

  if (button.closest('.el-dialog__footer') || button.closest('.el-message-box__btns')) {
    score += 20
  }
  if (button.closest('.filter-actions') || button.closest('.header-actions')) {
    score += 14
  }
  if (inTableActions) {
    score += options.preferTableActions ? 18 : -18
  }
  if (button.classList.contains('is-disabled') || button.getAttribute('disabled') !== null) {
    return 0
  }
  return score
}

/**
 * 获取指定范围内可点击的可见按钮。
 *
 * 实现步骤：查询 button 和 Element Plus 按钮节点，去重后排除不可见、disabled 和 is-disabled 元素。
 */
function visibleButtons(scope: ParentNode) {
  return Array.from(scope.querySelectorAll<HTMLElement>('button,.el-button'))
    .filter((button, index, buttons) => buttons.indexOf(button) === index)
    .filter(isVisible)
    .filter((button) => button.getAttribute('disabled') === null && !button.classList.contains('is-disabled'))
}

/**
 * 获取语音命令默认作用区域。
 *
 * 实现步骤：优先使用当前弹窗；没有弹窗时限定在主工作区，避免误点顶栏和悬浮控件。
 */
function activeDialogOrWorkspace(): ParentNode {
  return activeDialogOrBody() === document.body
    ? document.querySelector<HTMLElement>('.workspace-body') || document.body
    : activeDialogOrBody()
}

/**
 * 获取当前最上层弹窗或全页面。
 *
 * 实现步骤：查找可见对话框、消息框和确认框，返回最后一个作为最上层弹窗。
 */
function activeDialogOrBody(): ParentNode {
  /**
   * 常量 overlays：保存当前可见的弹窗类容器。
   */
  const overlays = visibleElements('.el-dialog,.el-message-box,.el-popconfirm')
  return overlays[overlays.length - 1] || document.body
}

/**
 * 获取当前最上层弹窗；没有弹窗时返回 undefined。
 *
 * 实现步骤：只查找可见对话框、消息框和确认框，不回退到 body，防止确认口令误点页面按钮。
 */
function activeDialogOnly(): ParentNode | undefined {
  /**
   * 常量 overlays：保存当前可见的确认或对话弹窗。
   */
  const overlays = visibleElements('.el-dialog,.el-message-box,.el-popconfirm')
  return overlays[overlays.length - 1]
}

/**
 * 判断当前弹窗是否是删除确认语义。
 *
 * 实现步骤：读取弹窗文本并检查“删除、批量删除、确认删除”等关键词，防止确认删除口令误点其他确认框。
 */
function dialogContainsDeleteIntent(scope: ParentNode) {
  /**
   * 常量 text：保存当前弹窗可见文本。
   */
  const text = scope instanceof HTMLElement ? scope.innerText : ''
  return /删除|删掉|删了|删哈|批量删除|批量删掉|确认删除|确认删掉|确认删了/.test(text)
}

/**
 * 查询可见 DOM 元素。
 *
 * 实现步骤：按选择器取元素后统一经过 isVisible 过滤。
 */
function visibleElements(selector: string) {
  return Array.from(document.querySelectorAll<HTMLElement>(selector)).filter(isVisible)
}

/**
 * 判断 DOM 元素是否在当前页面可见。
 *
 * 实现步骤：检查 display、visibility、opacity 和布局矩形，避免点击隐藏节点。
 */
function isVisible(element: HTMLElement) {
  /**
   * 常量 style：保存元素当前计算样式。
   */
  const style = window.getComputedStyle(element)
  return style.display !== 'none' && style.visibility !== 'hidden' && style.opacity !== '0' && element.getClientRects().length > 0
}

/**
 * 等待前端界面完成短暂渲染。
 *
 * 实现步骤：使用统一短延时等待路由切换、弹窗打开或下拉框渲染。
 */
async function waitForUi() {
  await sleep(180)
}

/**
 * 返回基于 window.setTimeout 的延时 Promise。
 *
 * 实现步骤：用于串联 UI 点击和 DOM 渲染。
 */
function sleep(ms: number) {
  return new Promise((resolve) => window.setTimeout(resolve, ms))
}

/**
 * 标准化中文语音和页面文案。
 *
 * 实现步骤：移除标点、空白和“菜单/模块/页面/功能”等泛化词，并统一转小写。
 */
function normalizeText(text: string) {
  return text
    .replace(/[，。、“”‘’：:；;,.!?？！\s]/g, '')
    .replace(/菜单|模块|页面|功能/g, '')
    .toLowerCase()
}

/**
 * 转义字符串中的正则特殊字符。
 *
 * 实现步骤：用于把表单标签安全拼入动态正则。
 */
function escapeRegExp(value: string) {
  return value.replace(/[.*+?^${}()|[\]\\]/g, '\\$&')
}

/**
 * 构造语音命令执行结果。
 *
 * 实现步骤：统一返回 handled、type 和 message，便于语音组件展示状态。
 */
function result(handled: boolean, type: VoiceCommandResultType, message: string): VoiceCommandResult {
  return { handled, type, message }
}
