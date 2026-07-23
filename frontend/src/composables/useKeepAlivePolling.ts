import { onActivated, onBeforeUnmount, onDeactivated, ref, watch, type Ref } from 'vue'

/** 缓存页面轮询的通用配置。 */
export interface KeepAlivePollingOptions {
  /** 用户或业务开关；关闭后即使页面处于激活状态也不执行轮询。 */
  enabled: Ref<boolean>
  /** 每次轮询执行的任务，异步错误由任务自身按业务口径处理。 */
  task: () => void | Promise<void>
  /** 两次任务调度之间的固定间隔，单位为毫秒。 */
  intervalMs: number
  /** 每次新建轮询实例前执行，用于重置调用方的周期状态。 */
  onStart?: () => void
  /** 页面激活时是否立即执行一次任务，默认执行。 */
  immediate?: boolean
}

/**
 * 管理 keep-alive 页面内轮询任务的生命周期。
 *
 * 实现步骤：
 * 1. 页面激活时记录状态，并按业务开关启动轮询；
 * 2. 页面停用或组件卸载时清除定时器，避免缓存页面继续请求；
 * 3. 页面激活期间响应业务开关变化，确保定时器只有一个实例。
 */
export function useKeepAlivePolling(options: KeepAlivePollingOptions) {
  /** 当前缓存页面是否正在展示，用于阻止停用期间由业务开关启动定时器。 */
  const pageActive = ref(false)
  /** 当前轮询定时器标识；未启动时保持为空。 */
  let timer: number | undefined

  /** 清除现有轮询实例，供停用、卸载和重新同步配置时统一调用。 */
  function stop() {
    if (timer === undefined) {
      return
    }
    window.clearInterval(timer)
    timer = undefined
  }

  /** 根据页面激活状态和业务开关重建唯一的轮询实例。 */
  function sync() {
    stop()
    if (!pageActive.value || !options.enabled.value) {
      return
    }
    options.onStart?.()
    if (options.immediate !== false) {
      void options.task()
    }
    timer = window.setInterval(() => {
      void options.task()
    }, options.intervalMs)
  }

  watch(options.enabled, sync)
  onActivated(() => {
    pageActive.value = true
    sync()
  })
  onDeactivated(() => {
    pageActive.value = false
    stop()
  })
  onBeforeUnmount(() => {
    pageActive.value = false
    stop()
  })
}
