import { ElMessage } from 'element-plus'

type NoticeType = 'success' | 'error' | 'warning' | 'info' | 'all'

/** 最近一次统一接口提示的失效时间，用于屏蔽紧随其后的顶部重复提示。 */
let suppressUntil = 0
/** 最近一次统一接口提示的类型；同类型顶部消息才会被去重。 */
let suppressType: NoticeType = 'all'
/** 登录失效期间屏蔽所有顶部消息，避免并发请求重复报错导致页面卡顿。 */
let authFailureSuppressUntil = 0
/** 防止重复安装 Element Plus Message 补丁。 */
let installed = false

/**
 * 标记统一接口提示已经展示。
 *
 * 实现步骤：
 * 1. 记录统一弹窗或右下角通知的类型；
 * 2. 在短时间窗口内屏蔽同类型顶部 ElMessage；
 * 3. 保留本地表单校验等非接口提示的正常展示。
 */
export function markApiResultNotice(type: NoticeType, duration = 2500) {
  suppressType = type
  suppressUntil = Date.now() + duration
}

/**
 * 标记登录失效提示已经接管页面。
 *
 * 实现步骤：
 * 1. 关闭已经出现的顶部消息；
 * 2. 在登录过期倒计时窗口内屏蔽所有后续顶部消息；
 * 3. 避免并发请求的 catch 分支重复弹出“请求失败”。
 */
export function markAuthFailureNotice(duration = 12_000) {
  const until = Date.now() + duration
  authFailureSuppressUntil = Math.max(authFailureSuppressUntil, until)
  suppressType = 'all'
  suppressUntil = Math.max(suppressUntil, until)
  ;(ElMessage as unknown as { closeAll?: () => void }).closeAll?.()
}

/**
 * 判断当前是否处于登录失效消息屏蔽窗口。
 */
export function isAuthFailureNoticeActive() {
  return Date.now() <= authFailureSuppressUntil
}

/**
 * 安装顶部消息去重补丁。
 *
 * 实现步骤：
 * 1. 包装 Element Plus 的 success、error、warning、info 方法；
 * 2. 如果统一接口提示刚刚出现，则不再展示顶部同类型提示；
 * 3. 其它场景原样调用 Element Plus，避免影响页面本地校验提示。
 */
export function installApiNoticeDedupe() {
  if (installed) {
    return
  }
  installed = true
  ;(['success', 'error', 'warning', 'info'] as const).forEach((method) => {
    const original = ElMessage[method].bind(ElMessage)
    ;(ElMessage as unknown as Record<typeof method, typeof original>)[method] = ((...args: Parameters<typeof original>) => {
      if (shouldSuppress(method)) {
        return { close: () => undefined } as ReturnType<typeof original>
      }
      return original(...args)
    }) as typeof original
  })
}

/**
 * 判断当前顶部消息是否应被统一接口提示覆盖。
 */
function shouldSuppress(type: NoticeType) {
  if (isAuthFailureNoticeActive()) {
    return true
  }
  return Date.now() <= suppressUntil && (suppressType === 'all' || suppressType === type)
}
