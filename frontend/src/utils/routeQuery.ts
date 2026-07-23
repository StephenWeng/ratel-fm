/**
 * 将 Vue Router 的单值或多值查询参数统一转换为字符串。
 *
 * 数组参数只读取第一个值，空值统一返回空字符串，避免各业务页面重复处理
 * `string | string[] | null | undefined` 的路由输入差异。
 */
export function queryString(value: unknown) {
  return Array.isArray(value) ? String(value[0] || '') : String(value || '')
}
