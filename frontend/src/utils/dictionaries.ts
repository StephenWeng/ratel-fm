import type { BasicDictionaryView } from '@/types/api'

/** 树形业务字典转换后的通用下拉选项。 */
export interface DictionaryOption {
  /** 带层级缩进的用户可读名称。 */
  label: string
  /** 表单实际保存的字典名称。 */
  value: string
}

/** 字典兜底项插入位置；默认置顶以优先展示核心默认值。 */
export type FallbackOptionPlacement = 'prepend' | 'append'

/**
 * 将业务字典树转换为下拉选项。
 *
 * 开发组织：ratel；开发人员：WenZhang；联系方式：18782945613。
 *
 * 实现步骤：
 * 1. 递归遍历后端返回的启用字典树；
 * 2. 使用全角空格保留层级缩进，方便人员维护时识别部门、组织、岗位层级；
 * 3. 选项值使用字典名称，保持和人员表、JWT、审计日志现有展示字段一致。
 */
export function flattenDictionaryOptions(nodes: BasicDictionaryView[], level = 0): DictionaryOption[] {
  /** 当前层及全部子层转换后的扁平选项。 */
  const result: DictionaryOption[] = []
  nodes.forEach((node) => {
    result.push({
      label: `${'　'.repeat(level)}${node.name}`,
      value: node.name
    })
    result.push(...flattenDictionaryOptions(node.children || [], level + 1))
  })
  return result
}

/**
 * 在后端字典缺少关键默认值时补充前端兜底项。
 *
 * 实现步骤：
 * 1. 按名称判断选项是否已经存在，存在时保留原数组及排序；
 * 2. 使用负数临时标识构造只供当前表单选择的启用项；
 * 3. 按页面展示要求置顶或追加，兼容旧数据库尚未初始化新字典的场景。
 */
export function withFallbackDictionaryOption(
  options: BasicDictionaryView[],
  code: string,
  name: string,
  placement: FallbackOptionPlacement = 'prepend'
) {
  if (options.some((item) => item.name === name)) {
    return options
  }
  /** 不落库的临时字典项，负数标识避免与后端主键混淆。 */
  const fallback: BasicDictionaryView = {
    id: -options.length - 1,
    code,
    name,
    sortOrder: 0,
    enabled: true,
    hasChildren: false,
    children: []
  }
  return placement === 'append' ? [...options, fallback] : [fallback, ...options]
}
