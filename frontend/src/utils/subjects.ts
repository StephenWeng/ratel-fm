import type { SubjectView } from '@/types/api'

/**
 * SubjectTreeNode 类型定义，用于把接口返回的扁平科目列表转换为前端树。
 */
export interface SubjectTreeNode extends SubjectView {
  /**
   * 字段 children：保存当前科目的下级科目，供级联选择器按会计科目层级展示。
   */
  children?: SubjectTreeNode[]
}

/**
 * SubjectCascaderOption 类型定义，用于约束 Element Plus 级联选择器的科目节点。
 */
export interface SubjectCascaderOption {
  /**
   * 字段 label：前端展示的科目名称，不包含科目代码。
   */
  label: string
  /**
   * 字段 value：最终提交给后端保存的科目主键。
   */
  value: number
  /**
   * 字段 disabled：不可记账科目禁止选择，但仍保留层级用于展开。
   */
  disabled?: boolean
  /**
   * 字段 children：当前科目的下级科目级联节点。
   */
  children?: SubjectCascaderOption[]
}

/**
 * 常量 subjectCascaderProps：统一配置科目级联选择器。
 *
 * 实现步骤：
 * 1. value 使用科目 id，label 使用科目名称；
 * 2. emitPath=false 让 v-model 只保存最终选择的科目 id；
 * 3. checkStrictly=false 保持必须选择叶子科目的财务录入规则。
 */
export const subjectCascaderProps = {
  value: 'value',
  label: 'label',
  children: 'children',
  disabled: 'disabled',
  emitPath: false,
  checkStrictly: false
}

/**
 * 构建科目级联选择器选项。
 *
 * 实现步骤：
 * 1. allSubjects 提供完整启用科目层级，保证父级科目能作为树节点展开；
 * 2. selectableSubjects 提供后端判定可用于凭证记账的叶子科目；
 * 3. 递归生成只展示科目名称的级联节点，非可记账叶子节点禁用。
 */
export function buildSubjectCascaderOptions(allSubjects: SubjectView[], selectableSubjects: SubjectView[]) {
  /** 可记账科目 ID 集合，用于控制级联叶子节点是否可选。 */
  const selectableIds = new Set(selectableSubjects.map((item) => item.id))
  return buildSubjectTree(allSubjects).map((node) => toSubjectCascaderOption(node, selectableIds))
}

/**
 * 获取科目名称级联路径。
 *
 * 实现步骤：
 * 1. 根据科目 id 在完整科目列表中定位当前科目；
 * 2. 沿 parentId 向上查找父级科目；
 * 3. 只拼接科目名称，前端不显示科目代码。
 */
export function subjectNamePath(subjectId: number | undefined, allSubjects: SubjectView[]) {
  if (!subjectId) {
    return ''
  }
  /** 科目 ID 到科目对象的索引，用于沿 parentId 反向追溯名称路径。 */
  const subjectMap = new Map(allSubjects.map((item) => [item.id, item]))
  const names: string[] = []
  /** 当前追溯到的科目节点。 */
  let cursor = subjectMap.get(subjectId)
  /** 父级追溯保护计数，避免异常环形科目导致死循环。 */
  let guard = 0
  while (cursor && guard < 20) {
    names.unshift(cursor.name)
    cursor = cursor.parentId ? subjectMap.get(cursor.parentId) : undefined
    guard += 1
  }
  return names.join(' / ')
}

/**
 * 构建会计科目树。
 *
 * 实现步骤：
 * 1. 按科目 ID 创建节点副本，避免修改接口原对象；
 * 2. 根据 parentId 把子科目挂到父科目 children；
 * 3. 没有父级或父级缺失时作为根节点展示；
 * 4. 每一层都按科目编码正序排序，保证树结构稳定。
 */
function buildSubjectTree(rows: SubjectView[]) {
  /** 科目节点缓存，先创建所有节点副本再挂载父子关系。 */
  const nodeMap = new Map<number, SubjectTreeNode>()
  rows.forEach((item) => nodeMap.set(item.id, { ...item, children: [] }))
  const roots: SubjectTreeNode[] = []
  rows.forEach((item) => {
    /** 当前科目节点副本，用于挂载到父级或根节点集合。 */
    const node = nodeMap.get(item.id)
    if (!node) {
      return
    }
    if (item.parentId && nodeMap.has(item.parentId)) {
      nodeMap.get(item.parentId)?.children?.push(node)
      return
    }
    roots.push(node)
  })
  sortSubjectNodes(roots)
  return roots
}

/**
 * 递归转换为级联选择器节点。
 *
 * 实现步骤：
 * 1. 子级仍按树层级递归转换；
 * 2. 有子级的科目仅作为分组节点；
 * 3. 无子级但不在可记账集合中的科目禁用，避免前端提交后被后端拒绝。
 */
function toSubjectCascaderOption(node: SubjectTreeNode, selectableIds: Set<number>): SubjectCascaderOption {
  /** 当前科目的子级级联节点，递归保留会计科目层级。 */
  const children = (node.children || []).map((child) => toSubjectCascaderOption(child, selectableIds))
  return {
    label: node.name,
    value: node.id,
    disabled: children.length === 0 && !selectableIds.has(node.id),
    ...(children.length > 0 ? { children } : {})
  }
}

/**
 * 按科目编码递归排序科目树。
 *
 * 实现步骤：当前层级按 code 正序排序，编码一致时按 id 兜底；再对所有子级重复排序。
 */
function sortSubjectNodes(nodes: SubjectTreeNode[]) {
  nodes.sort((first, second) => first.code.localeCompare(second.code, 'zh-Hans-CN') || first.id - second.id)
  nodes.forEach((node) => sortSubjectNodes(node.children || []))
}
