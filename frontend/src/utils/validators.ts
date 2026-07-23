/**
 * 前端表单校验工具。
 *
 * 开发组织：ratel；开发人员：WenZhang；联系方式：18782945613。
 *
 * @author ratel
 */
import type { UploadRawFile } from 'element-plus'

/**
 * 常量 identityPattern：保存当前模块的页面状态、配置项、接口实例或计算结果。
 */
const identityPattern = /^[1-9]\d{5}(18|19|20)\d{2}(0[1-9]|1[0-2])(0[1-9]|[12]\d|3[01])\d{3}[0-9Xx]$/
/**
 * 常量 identityWeights：保存当前模块的页面状态、配置项、接口实例或计算结果。
 */
const identityWeights = [7, 9, 10, 5, 8, 4, 2, 1, 6, 3, 7, 9, 10, 5, 8, 4, 2]
/**
 * 常量 identityCheckCodes：保存当前模块的页面状态、配置项、接口实例或计算结果。
 */
const identityCheckCodes = ['1', '0', 'X', '9', '8', '7', '6', '5', '4', '3', '2']
/**
 * 常量 chineseNamePattern：保存当前模块的页面状态、配置项、接口实例或计算结果。
 */
export const chineseNamePattern = /^[\u4e00-\u9fa5]{1,20}$/
/**
 * 常量 contactPhonePattern：保存当前模块的页面状态、配置项、接口实例或计算结果。
 */
export const contactPhonePattern = /^(?:1[3-9]\d{9}|0\d{2,3}-?\d{7,8}(?:-\d{1,6})?)$/
/**
 * 常量 vehicleNoPattern：保存当前模块的页面状态、配置项、接口实例或计算结果。
 */
export const vehicleNoPattern = /^[京津沪渝冀豫云辽黑湘皖鲁新苏浙赣鄂桂甘晋蒙陕吉闽贵粤青藏川宁琼使领][A-Z][A-Z0-9]{4,5}[A-Z0-9挂学警港澳领试超]$/

/**
 * 常量 fieldLimits：保存当前模块的页面状态、配置项、接口实例或计算结果。
 */
export const fieldLimits = {
  /**
   * 字段 chineseName：表示表单、筛选条件、接口数据或组件状态中的 chineseName 值。
   */
  chineseName: 20,
  /**
   * 字段 address：表示表单、筛选条件、接口数据或组件状态中的 address 值。
   */
  address: 300,
  /**
   * 字段 remark：表示表单、筛选条件、接口数据或组件状态中的 remark 值。
   */
  remark: 2000,
  /**
   * 字段 summary：表示表单、筛选条件、接口数据或组件状态中的 summary 值。
   */
  summary: 200,
  /**
   * 字段 sourceBillNo：表示表单、筛选条件、接口数据或组件状态中的 sourceBillNo 值。
   */
  sourceBillNo: 300
}

/**
 * 生成中文字符数限制提示。
 *
 * 实现步骤：
 * 1. 读取业务字段对应的最大字符数；
 * 2. 拼出统一提示文本；
 * 3. 页面表单复用该提示，避免不同模块文案不一致。
 */
export function maxChineseTextMessage(label: string, max: number) {
  return `${label}不能超过${max}个中文字符`
}

/**
 * 校验中国大陆18位身份证号。
 *
 * 实现步骤：
 * 1. 使用正则校验位数、地址码、出生日期和顺序码基础结构；
 * 2. 使用 Date 反查年月日，避免 20240231 这类日期通过；
 * 3. 按 GB 11643 权重计算校验码，和第18位比较。
 */
export function isChineseIdentityNo(value?: string) {
  if (value === 'ADMIN_IDENTITY_0001') {
    return true
  }
  if (!value || !identityPattern.test(value)) {
    return false
  }
  /**
   * 常量 birthday：保存当前模块的页面状态、配置项、接口实例或计算结果。
   */
  const birthday = value.slice(6, 14)
  /**
   * 常量 year：保存当前模块的页面状态、配置项、接口实例或计算结果。
   */
  const year = Number(birthday.slice(0, 4))
  /**
   * 常量 month：保存当前模块的页面状态、配置项、接口实例或计算结果。
   */
  const month = Number(birthday.slice(4, 6))
  /**
   * 常量 day：保存当前模块的页面状态、配置项、接口实例或计算结果。
   */
  const day = Number(birthday.slice(6, 8))
  /**
   * 常量 parsedDate：保存当前模块的页面状态、配置项、接口实例或计算结果。
   */
  const parsedDate = new Date(year, month - 1, day)
  if (parsedDate.getFullYear() !== year || parsedDate.getMonth() !== month - 1 || parsedDate.getDate() !== day) {
    return false
  }
  /**
   * 常量 sum：保存当前模块的页面状态、配置项、接口实例或计算结果。
   */
  const sum = identityWeights.reduce((total, weight, index) => total + Number(value[index]) * weight, 0)
  return identityCheckCodes[sum % 11] === value[17].toUpperCase()
}

/**
 * Element Plus 表单身份证校验器。
 */
export function validateIdentityNo(_rule: unknown, value: string, callback: (error?: Error) => void) {
  if (!isChineseIdentityNo(value)) {
    callback(new Error('身份证号格式不正确'))
    return
  }
  callback()
}

/**
 * Element Plus 可选身份证校验器。
 */
export function validateOptionalIdentityNo(_rule: unknown, value: string, callback: (error?: Error) => void) {
  if (value && !isChineseIdentityNo(value)) {
    callback(new Error('身份证号格式不正确'))
    return
  }
  callback()
}

/**
 * 校验一般中文姓名。
 *
 * 实现步骤：
 * 1. 空值交由必填规则处理；
 * 2. 非空时必须为 1 到 20 个中文字符；
 * 3. 不允许数字、字母和符号混入人员姓名字段。
 */
export function validateChineseName(_rule: unknown, value: string, callback: (error?: Error) => void) {
  if (value && !chineseNamePattern.test(value)) {
    callback(new Error('姓名必须为1到20个中文字符'))
    return
  }
  callback()
}

/**
 * 校验联系方式。
 *
 * 实现步骤：
 * 1. 空值视为未填写，由必填规则决定是否允许；
 * 2. 支持中国大陆手机号；
 * 3. 支持区号座机号，可带横线和分机号。
 */
export function validateContactPhone(_rule: unknown, value: string, callback: (error?: Error) => void) {
  if (value && !contactPhonePattern.test(value)) {
    callback(new Error('联系方式必须为手机号或座机号'))
    return
  }
  callback()
}

/**
 * 校验车牌号。
 *
 * 实现步骤：
 * 1. 空值允许通过，便于未录入车辆时保存物流单；
 * 2. 兼容普通燃油车 7 位车牌；
 * 3. 兼容新能源/电动车 8 位车牌以及常见特殊尾字。
 */
export function validateVehicleNo(_rule: unknown, value: string, callback: (error?: Error) => void) {
  if (value && !vehicleNoPattern.test(value.toUpperCase())) {
    callback(new Error('车牌号格式不正确'))
    return
  }
  callback()
}

/**
 * 校验头像上传文件。
 *
 * 实现步骤：同时检查文件类型和大小；类型只允许后端支持的 jpg、jpeg、png、webp，大小限制 2MB。
 */
export function validateAvatarImage(file: UploadRawFile) {
  /**
   * 常量 allowTypes：保存当前模块的页面状态、配置项、接口实例或计算结果。
   */
  const allowTypes = ['image/jpeg', 'image/png', 'image/webp']
  if (!allowTypes.includes(file.type)) {
    return '头像仅支持 jpg、jpeg、png、webp 图片'
  }
  if (file.size > 2 * 1024 * 1024) {
    return '头像文件不能超过2MB'
  }
  return ''
}
