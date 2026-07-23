import { describe, expect, it } from 'vitest'
import { withFallbackDictionaryOption } from '@/utils/dictionaries'
import { formatLocalDate, toLocalDateTimeBoundary } from '@/utils/dateTime'
import { fallbackExchangeRateToCny, roundBusinessMoney } from '@/utils/money'
import { queryString } from '@/utils/routeQuery'
import type { BasicDictionaryView } from '@/types/api'

/** 构造共享字典工具测试使用的最小后端选项。 */
function dictionaryOption(id: number, code: string, name: string): BasicDictionaryView {
  return { id, code, name, sortOrder: id, enabled: true, hasChildren: false, children: [] }
}

describe('共享业务工具', () => {
  it('按指定位置补充字典兜底项且不重复已有名称', () => {
    const options = [dictionaryOption(1, 'EXISTING', '已有项')]
    const prepended = withFallbackDictionaryOption(options, 'DEFAULT', '默认项')
    const appended = withFallbackDictionaryOption(options, 'DEFAULT', '默认项', 'append')

    expect(prepended.map((item) => item.name)).toEqual(['默认项', '已有项'])
    expect(appended.map((item) => item.name)).toEqual(['已有项', '默认项'])
    expect(withFallbackDictionaryOption(options, 'IGNORED', '已有项')).toBe(options)
  })

  it('统一读取路由单值、多值和空查询参数', () => {
    expect(queryString('A')).toBe('A')
    expect(queryString(['A', 'B'])).toBe('A')
    expect(queryString(undefined)).toBe('')
  })

  it('统一财务金额精度和币种本地兜底汇率', () => {
    expect(roundBusinessMoney(1.123456789)).toBe(1.12345679)
    expect(fallbackExchangeRateToCny('CNY', 7.2)).toBe(1)
    expect(fallbackExchangeRateToCny('USD', 7.2)).toBe(7.2)
    expect(fallbackExchangeRateToCny('USD', 0)).toBe(1)
  })

  it('按本地日期生成后端可解析的全天边界', () => {
    expect(formatLocalDate(new Date(2026, 6, 16))).toBe('2026-07-16')
    expect(toLocalDateTimeBoundary('2026-07-16')).toMatch(/^2026-07-16T00:00:00[+-]\d{2}:\d{2}$/)
    expect(toLocalDateTimeBoundary('2026-07-16', true)).toMatch(/^2026-07-16T23:59:59[+-]\d{2}:\d{2}$/)
  })
})
