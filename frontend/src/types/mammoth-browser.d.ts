/**
 * mammoth 浏览器构建包的最小类型声明。
 *
 * <p>开发组织：ratel；开发人员：WenZhang；联系方式：18782945613。</p>
 *
 * @author ratel
 */
declare module 'mammoth/mammoth.browser' {
  /**
   * MammothConvertResult 类型定义，用于约束页面状态、接口入参或接口返回数据结构。
   */
  export interface MammothConvertResult {
    /**
     * 字段 value：表示表单、筛选条件、接口数据或组件状态中的 value 值。
     */
    value: string
    /**
     * 字段 messages：表示表单、筛选条件、接口数据或组件状态中的 messages 值。
     */
    messages: unknown[]
  }

  /**
   * 执行 convertToHtml 方法。
   * 
   * 实现步骤：
   * 1. 读取当前页面状态或调用参数；
   * 2. 完成对应的校验、接口调用或数据转换；
   * 3. 更新页面状态或返回处理结果。
   */
  export function convertToHtml(input: { arrayBuffer: ArrayBuffer }): Promise<MammothConvertResult>
}
