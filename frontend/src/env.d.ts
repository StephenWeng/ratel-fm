declare module '*.vue' {
  import type { DefineComponent } from 'vue'
  /**
   * 常量 component：保存当前模块的页面状态、配置项、接口实例或计算结果。
   */
  const component: DefineComponent<Record<string, unknown>, Record<string, unknown>, unknown>
  export default component
}
