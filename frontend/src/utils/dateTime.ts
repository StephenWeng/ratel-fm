/** 两位日期时间数字补零。 */
function padDateTimePart(value: number) {
  return String(value).padStart(2, '0')
}

/** 按浏览器本地时区格式化为 yyyy-MM-dd，供日期筛选控件使用。 */
export function formatLocalDate(date: Date) {
  return `${date.getFullYear()}-${padDateTimePart(date.getMonth() + 1)}-${padDateTimePart(date.getDate())}`
}

/** 将浏览器本地时区偏移转换为 OffsetDateTime 使用的 +08:00 格式。 */
export function localTimezoneOffset() {
  /** JavaScript 时区偏移方向与 ISO 8601 相反，因此先取反。 */
  const offsetMinutes = -new Date().getTimezoneOffset()
  /** 正偏移必须显式保留加号，负偏移保留减号。 */
  const sign = offsetMinutes >= 0 ? '+' : '-'
  const absoluteMinutes = Math.abs(offsetMinutes)
  return `${sign}${padDateTimePart(Math.floor(absoluteMinutes / 60))}:${padDateTimePart(absoluteMinutes % 60)}`
}

/**
 * 将业务日期转换为后端 OffsetDateTime 可解析的全天边界。
 *
 * 开始边界使用 00:00:00，结束边界使用 23:59:59，并附加浏览器本地时区，
 * 保证不同时区的操作日志查询仍以用户选择的本地业务日期为准。
 */
export function toLocalDateTimeBoundary(dateText: string | undefined, endOfDay = false) {
  if (!dateText) {
    return undefined
  }
  return `${dateText}T${endOfDay ? '23:59:59' : '00:00:00'}${localTimezoneOffset()}`
}
