import type { App } from 'vue'

/**
 * 可触发表单字段校验的 Element Plus 控件选择器。
 *
 * 实现步骤：
 * 1. 覆盖输入框、文本域、下拉框、日期、数字、级联、树选等常用录入控件；
 * 2. 事件统一绑定在控件根节点，减少每个页面重复写 @mouseleave；
 * 3. 离开控件时查找最近的 el-form-item，并调用其 validate 方法触发字段级红框和错误文案。
 */
const VALIDATION_SELECTOR = [
  '.el-input',
  '.el-textarea',
  '.el-select',
  '.el-date-editor',
  '.el-input-number',
  '.el-cascader',
  '.el-tree-select',
  '.el-checkbox-group',
  '.el-radio-group',
  '.el-switch'
].join(',')

/**
 * 常量 installed：记录全局 mouseleave 监听是否已经安装，避免热更新或重复初始化时重复绑定。
 */
let installed = false

/**
 * 注册全局鼠标移出表单校验监听。
 *
 * 实现步骤：
 * 1. 应用启动时安装一次 document 级 mouseleave 捕获监听；
 * 2. 用户鼠标离开任意 Element Plus 输入控件时，查找最近表单项；
 * 3. 触发表单项自己的 validate 方法，错误文案和红框仍由 Element Plus 统一渲染；
 * 4. document 级监听可以覆盖 dialog、drawer、popover 等 Teleport 到 body 的弹层表单。
 */
export function installFormMouseleaveValidation(app: App) {
  void app
  if (installed || typeof document === 'undefined') {
    return
  }
  installed = true
  document.addEventListener('mouseleave', handleMouseleave, true)
}

/**
 * 处理全局 mouseleave 事件。
 *
 * 实现步骤：
 * 1. 从事件目标向上查找最近的支持控件根节点；
 * 2. 不在支持控件内时直接跳过；
 * 3. 命中控件后触发表单项字段校验。
 */
function handleMouseleave(event: Event) {
  /** 鼠标离开事件的原始目标，用于判断是否来自可校验控件。 */
  const source = event.target
  if (!(source instanceof Element)) {
    return
  }
  /** 最近的 Element Plus 控件根节点，未命中时说明不需要触发表单校验。 */
  const target = source.closest(VALIDATION_SELECTOR)
  if (target) {
    validateNearestFormItem(target)
  }
}

/**
 * 触发最近表单项的字段校验。
 *
 * 实现步骤：
 * 1. 从控件向上查找最近的 .el-form-item；
 * 2. 读取 Element Plus 挂在 DOM 上的 Vue 组件实例；
 * 3. 组件实例存在 validate 方法时执行校验，错误由 Element Plus 自己展示在字段下方。
 */
function validateNearestFormItem(target: Element) {
  /** 当前控件所在的最近表单项，字段错误文案会挂在该节点下方。 */
  const formItem = target.closest('.el-form-item') as (HTMLElement & { __vueParentComponent?: unknown }) | null
  /** 表单项 Vue 组件暴露实例，包含 Element Plus 的 validate 方法。 */
  const exposed = formItem?.__vueParentComponent ? componentExposed(formItem.__vueParentComponent) : undefined
  if (exposed && typeof exposed.validate === 'function') {
    void exposed.validate('blur').catch(() => undefined)
  }
}

/**
 * 读取 Vue 组件暴露实例。
 *
 * 实现步骤：兼容 Element Plus 组件在不同 Vue 构建下的 exposed、proxy 两种挂载位置。
 */
function componentExposed(component: unknown): { validate?: (trigger?: string) => Promise<void> } | undefined {
  /** Vue 内部组件记录，兼容 exposed 和 proxy 两种可访问位置。 */
  const record = component as { exposed?: unknown; proxy?: unknown }
  return (record.exposed || record.proxy) as { validate?: (trigger?: string) => Promise<void> } | undefined
}
