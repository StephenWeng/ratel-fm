import { api } from '@/api/fm'
import type { MenuUsageView, MenuView, UserView } from '@/types/api'

export const MENU_USAGE_UPDATED = 'ratel-fm-menu-usage-updated'

export interface FrequentMenu {
  path: string
  label: string
  menuCode: string
  count: number
}

interface MenuUsageRecord {
  path: string
  label: string
  menuCode: string
  count: number
  lastUsedAt: number
}

/**
 * 按当前用户生成常用功能本地存储键。
 */
export function menuUsageUserKey(user?: UserView | null) {
  if (!user) {
    return ''
  }
  return `${user.organizationCode || 'default'}:${user.id}:${user.username}`
}

/**
 * 记录用户进入一次功能菜单。
 */
export function recordMenuUsage(userKey: string, menu: FrequentMenu) {
  if (!userKey || !menu.path || !menu.menuCode) {
    return
  }
  const records = readMenuUsage(userKey)
  const previous = records[menu.menuCode]
  records[menu.menuCode] = {
    path: menu.path,
    label: menu.label,
    menuCode: menu.menuCode,
    count: (previous?.count || 0) + 1,
    lastUsedAt: Date.now()
  }
  writeMenuUsage(userKey, records)
  window.dispatchEvent(new CustomEvent(MENU_USAGE_UPDATED, { detail: { userKey } }))
  void api.recordMenuUsage({ menuCode: menu.menuCode, menuName: menu.label, routePath: menu.path }).catch(() => undefined)
}

/**
 * 从后端读取持久化常用功能，并同步到本地缓存。
 */
export async function syncMenuUsageFromServer(userKey: string, limit = 10) {
  if (!userKey) {
    return
  }
  const rows = await api.myMenuUsages(limit).catch(() => [] as MenuUsageView[])
  if (!rows.length) {
    return
  }
  const records = readMenuUsage(userKey)
  rows.forEach((row) => {
    if (!row.menuCode || !row.routePath) {
      return
    }
    const previous = records[row.menuCode]
    records[row.menuCode] = {
      path: row.routePath,
      label: row.menuName || previous?.label || row.menuCode,
      menuCode: row.menuCode,
      count: Math.max(previous?.count || 0, row.useCount || 0),
      lastUsedAt: Math.max(previous?.lastUsedAt || 0, row.lastUsedAt ? Date.parse(row.lastUsedAt) || 0 : 0)
    }
  })
  writeMenuUsage(userKey, records)
  window.dispatchEvent(new CustomEvent(MENU_USAGE_UPDATED, { detail: { userKey } }))
}

/**
 * 读取当前用户常用功能前 N 项。
 */
export function frequentMenus(userKey: string, menus: MenuView[], limit: number, fallbackMenuCodes: string[] = []) {
  const records = readMenuUsage(userKey)
  const pageMenus = menus
    .filter((menu) => menu.type === 'PAGE' && Boolean(menu.routePath))
    .sort((left, right) => left.sortOrder - right.sortOrder || left.id - right.id)
  const byCode = new Map(pageMenus.map((menu) => [menu.code, menu]))
  const ranked = pageMenus
    .map((menu) => toFrequentMenu(menu, records[menu.code]?.count || 0))
    .filter((menu) => menu.count > 0)
    .sort((left, right) => right.count - left.count || left.label.localeCompare(right.label, 'zh-CN'))

  if (ranked.length >= limit) {
    return ranked.slice(0, limit)
  }

  const usedCodes = new Set(ranked.map((menu) => menu.menuCode))
  const fallback = fallbackMenuCodes
    .map((code) => byCode.get(code))
    .filter((menu): menu is MenuView => {
      if (!menu) {
        return false
      }
      return !usedCodes.has(menu.code)
    })
    .map((menu) => toFrequentMenu(menu, 0))
  return [...ranked, ...fallback].slice(0, limit)
}

/**
 * 查找当前路由对应的菜单项。
 */
export function menuByPath(menus: MenuView[], path: string) {
  const menu = menus.find((item) => item.type === 'PAGE' && item.routePath === path)
  return menu ? toFrequentMenu(menu, 0) : undefined
}

function toFrequentMenu(menu: MenuView, count: number): FrequentMenu {
  return {
    path: menu.routePath || '/',
    label: menu.name,
    menuCode: menu.code,
    count
  }
}

function readMenuUsage(userKey: string) {
  if (!userKey) {
    return {} as Record<string, MenuUsageRecord>
  }
  try {
    return JSON.parse(localStorage.getItem(storageKey(userKey)) || '{}') as Record<string, MenuUsageRecord>
  } catch {
    return {}
  }
}

function writeMenuUsage(userKey: string, records: Record<string, MenuUsageRecord>) {
  localStorage.setItem(storageKey(userKey), JSON.stringify(records))
}

function storageKey(userKey: string) {
  return `ratel-fm-menu-usage:${userKey}`
}
