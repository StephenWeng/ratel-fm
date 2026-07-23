/** 中文大写金额数字表，按财务票据常用写法输出。 */
const CHINESE_AMOUNT_NUMBERS = ['零', '壹', '贰', '叁', '肆', '伍', '陆', '柒', '捌', '玖']

/** 中文大写金额的节内单位，覆盖个、十、百、千位。 */
const CHINESE_AMOUNT_UNITS = ['', '拾', '佰', '仟']

/** 中文大写金额的节单位，覆盖万、亿、兆。 */
const CHINESE_AMOUNT_SECTION_UNITS = ['', '万', '亿', '兆']

/** 财务业务金额统一保留的小数位，兼容数量单价计算和多币种换算。 */
export const BUSINESS_MONEY_FRACTION_DIGITS = 8

/** 将计算结果按财务业务精度舍入，避免浮点尾差进入表单和接口参数。 */
export function roundBusinessMoney(value: number) {
  return Number(Number(value || 0).toFixed(BUSINESS_MONEY_FRACTION_DIGITS))
}

/**
 * 读取币种切换期间可立即使用的本地汇率。
 *
 * 人民币固定为 1；外币优先保留已有正数汇率，没有有效值时暂按 1，
 * 等异步参考汇率返回后再由页面覆盖。
 */
export function fallbackExchangeRateToCny(currencyCode: string, currentRate: number) {
  if (currencyCode === 'CNY') {
    return 1
  }
  return Number(currentRate || 0) > 0 ? currentRate : 1
}

/**
 * 格式化页面金额。
 *
 * 实现步骤：
 * 1. 将任意输入转换为数字；
 * 2. 非法数字按 0 处理；
 * 3. 按指定小数位使用中文地区格式输出，保证各模块金额展示一致。
 */
export function formatMoney(value: unknown, fractionDigits = 8) {
  /** 转换后的数字金额，供格式化输出使用。 */
  const numberValue = Number(value || 0)
  /** 合法数字金额，避免 NaN 或 Infinity 进入页面。 */
  const safeValue = Number.isFinite(numberValue) ? numberValue : 0
  return safeValue.toLocaleString('zh-CN', { minimumFractionDigits: fractionDigits, maximumFractionDigits: fractionDigits })
}

/**
 * 格式化原始数字金额。
 *
 * 实现步骤：
 * 1. 将金额转为数字；
 * 2. 非法值按 0 兜底；
 * 3. 使用固定小数位且不加千分位，方便用户悬浮时查看本身数字型金额。
 */
export function formatPlainMoney(value: unknown, fractionDigits = 8) {
  /** 转换后的数字金额，保留原始数值语义。 */
  const numberValue = Number(value || 0)
  /** 合法数字金额，避免非法值影响 tooltip 展示。 */
  const safeValue = Number.isFinite(numberValue) ? numberValue : 0
  return safeValue.toFixed(fractionDigits)
}

/**
 * 转换为中文大写金额。
 *
 * 实现步骤：
 * 1. 将金额按分四舍五入，符合财务中文大写金额到角分的常规规则；
 * 2. 整数部分按四位一节转换为万、亿、兆；
 * 3. 小数部分转换为角分，零角整分时补“零”，无角分时输出“整”；
 * 4. 负数保留“负”前缀，币种前缀默认人民币。
 */
export function toChineseCapitalAmount(value: unknown, currencyPrefix = '人民币') {
  /** 转换后的数字金额，用于判断正负和计算分值。 */
  const numberValue = Number(value || 0)
  /** 合法数字金额，非法值按 0 输出。 */
  const safeValue = Number.isFinite(numberValue) ? numberValue : 0
  /** 负数前缀，财务展示中明确标识负向金额。 */
  const signText = safeValue < 0 ? '负' : ''
  /** 按分四舍五入后的绝对金额，后续用整数避免浮点尾差。 */
  const totalCents = BigInt(Math.round(Math.abs(safeValue) * 100))
  /** 整数元部分。 */
  const yuan = totalCents / 100n
  /** 角位数字。 */
  const jiao = Number((totalCents / 10n) % 10n)
  /** 分位数字。 */
  const fen = Number(totalCents % 10n)
  /** 整数元中文大写，0 元时固定输出零。 */
  const yuanText = yuan === 0n ? CHINESE_AMOUNT_NUMBERS[0] : integerToChineseAmount(yuan)
  /** 角分中文大写，按财务票据习惯生成。 */
  const centsText = decimalToChineseAmount(yuan, jiao, fen)
  return `${signText}${currencyPrefix}${yuanText}元${centsText}`
}

/**
 * 转换整数金额为中文大写。
 *
 * 实现步骤：
 * 1. 每四位拆成一个节；
 * 2. 每节内部转换个十百千；
 * 3. 节之间补必要的零并追加万、亿、兆单位。
 */
function integerToChineseAmount(value: bigint) {
  /** 待处理的整数金额，循环中不断右移四位。 */
  let remaining = value
  /** 当前四位节位置，对应 section unit。 */
  let sectionIndex = 0
  /** 已拼接的中文金额文本。 */
  let result = ''
  /** 是否需要在下一个非零节前补零。 */
  let needZero = false
  while (remaining > 0n) {
    /** 当前四位金额节。 */
    const section = Number(remaining % 10000n)
    if (section === 0) {
      needZero = result.length > 0
    } else {
      /** 当前四位金额节的中文文本。 */
      const sectionText = sectionToChineseAmount(section)
      result = `${needZero ? '零' : ''}${sectionText}${CHINESE_AMOUNT_SECTION_UNITS[sectionIndex]}${result}`
      needZero = section < 1000 && remaining / 10000n > 0n
    }
    remaining /= 10000n
    sectionIndex += 1
  }
  return result.replace(/零+/g, '零').replace(/零$/g, '')
}

/**
 * 转换四位以内金额节为中文大写。
 *
 * 实现步骤：
 * 1. 从个位开始逐位读取；
 * 2. 非零数字追加对应位单位；
 * 3. 连续零只保留一个零，避免出现重复零。
 */
function sectionToChineseAmount(section: number) {
  /** 当前节的剩余数字。 */
  let remaining = section
  /** 当前位序号，0 表示个位。 */
  let unitIndex = 0
  /** 当前节中文文本。 */
  let result = ''
  /** 上一位是否为零，用于压缩连续零。 */
  let zero = true
  while (remaining > 0) {
    /** 当前最低位数字。 */
    const digit = remaining % 10
    if (digit === 0) {
      if (!zero && result) {
        result = `${CHINESE_AMOUNT_NUMBERS[0]}${result}`
      }
      zero = true
    } else {
      result = `${CHINESE_AMOUNT_NUMBERS[digit]}${CHINESE_AMOUNT_UNITS[unitIndex]}${result}`
      zero = false
    }
    remaining = Math.floor(remaining / 10)
    unitIndex += 1
  }
  return result.replace(/零+$/g, '')
}

/**
 * 转换角分部分为中文大写。
 *
 * 实现步骤：
 * 1. 角分均为 0 时输出“整”；
 * 2. 有角时输出“X角”；
 * 3. 整数元后直接出现分时补“零”；
 * 4. 有分时输出“X分”。
 */
function decimalToChineseAmount(yuan: bigint, jiao: number, fen: number) {
  if (jiao === 0 && fen === 0) {
    return '整'
  }
  /** 角分中文文本片段。 */
  const parts: string[] = []
  if (jiao > 0) {
    parts.push(`${CHINESE_AMOUNT_NUMBERS[jiao]}角`)
  }
  if (jiao === 0 && fen > 0 && yuan > 0n) {
    parts.push('零')
  }
  if (fen > 0) {
    parts.push(`${CHINESE_AMOUNT_NUMBERS[fen]}分`)
  }
  return parts.join('')
}
