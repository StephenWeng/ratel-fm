import { defineStore } from 'pinia'

/**
 * ThemeName 类型定义。
 *
 * <p>用于约束系统支持的主题编码，主题编码会写入 localStorage 并同步到 html[data-theme]。</p>
 */
export type ThemeName = 'light' | 'dark' | 'emerald' | 'finance-blue'

/**
 * ThemeOption 类型定义。
 *
 * <p>用于描述主题切换菜单展示所需的名称、说明和颜色预览。</p>
 */
export interface ThemeOption {
  /** 主题编码，作为持久化和 CSS 选择器的稳定标识。 */
  value: ThemeName
  /** 主题中文名称，展示在顶栏主题切换入口和下拉菜单中。 */
  label: string
  /** 主题说明，用于后续扩展悬浮提示或设置页说明。 */
  description: string
  /** 主题主色预览，用于下拉菜单色块展示。 */
  preview: string
}

/** 本地存储键名，用于刷新页面后恢复用户最近选择的主题。 */
const THEME_STORAGE_KEY = 'ratel-fm-theme'

/** 默认主题编码，首次打开或读取到非法主题时使用浅色主题。 */
const DEFAULT_THEME: ThemeName = 'light'

/**
 * 系统支持的主题列表。
 *
 * <p>实现步骤：每个主题只暴露稳定编码和展示信息，具体颜色变量统一在 global.css 中维护。</p>
 */
export const themeOptions: ThemeOption[] = [
  { value: 'light', label: '浅色', description: '标准浅色办公主题', preview: '#1f7a5a' },
  { value: 'dark', label: '深色', description: '低亮度夜间办公主题', preview: '#7cc18b' },
  { value: 'emerald', label: '墨绿', description: '偏财务工作台的墨绿主题', preview: '#0f766e' },
  { value: 'finance-blue', label: '金融蓝', description: '偏金融系统的蓝色主题', preview: '#2563eb' }
]

/**
 * 判断入参是否为系统支持的主题编码。
 *
 * 实现步骤：
 * 1. 接收任意来源的主题值；
 * 2. 只要能在 themeOptions 中命中 value，就认为是合法主题。
 */
function isThemeName(value: unknown): value is ThemeName {
  return typeof value === 'string' && themeOptions.some((item) => item.value === value)
}

/**
 * 读取本地保存的主题。
 *
 * 实现步骤：
 * 1. 从 localStorage 读取主题编码；
 * 2. 校验编码是否合法；
 * 3. 读取失败或编码非法时返回默认主题。
 */
function readSavedTheme(): ThemeName {
  try {
    /** 浏览器本地保存的主题编码，可能为空或历史非法值。 */
    const savedTheme = window.localStorage.getItem(THEME_STORAGE_KEY)
    return isThemeName(savedTheme) ? savedTheme : DEFAULT_THEME
  } catch {
    return DEFAULT_THEME
  }
}

/**
 * 保存当前主题到本地。
 *
 * 实现步骤：捕获浏览器隐私模式或存储不可用异常，避免主题切换影响主业务功能。
 */
function saveTheme(themeName: ThemeName) {
  try {
    window.localStorage.setItem(THEME_STORAGE_KEY, themeName)
  } catch {
    // 主题持久化失败不影响本次页面内切换。
  }
}

/**
 * 把主题应用到页面根节点。
 *
 * 实现步骤：
 * 1. 设置 html[data-theme]，让全局 CSS 变量切换到目标主题；
 * 2. 设置 color-scheme，帮助浏览器和 Element Plus 在深色主题下使用合适的原生控件颜色。
 */
function applyDocumentTheme(themeName: ThemeName) {
  document.documentElement.dataset.theme = themeName
  document.documentElement.style.colorScheme = themeName === 'dark' ? 'dark' : 'light'
}

/**
 * 全局主题状态。
 *
 * <p>实现目的：为顶栏主题切换、刷新恢复和全局 CSS 变量应用提供统一入口。</p>
 */
export const useThemeStore = defineStore('theme', {
  state: () => ({
    /** 当前主题编码，初始化时从 localStorage 恢复。 */
    current: readSavedTheme()
  }),
  getters: {
    /**
     * 当前主题展示配置。
     *
     * 实现步骤：根据 current 找到主题配置；理论上 current 已校验，这里仍保留默认值兜底。
     */
    currentOption(state): ThemeOption {
      return themeOptions.find((item) => item.value === state.current) || themeOptions[0]
    }
  },
  actions: {
    /**
     * 应用当前主题。
     *
     * 实现步骤：把 store 中的 current 同步到 html[data-theme]，通常在 App 初始化时调用。
     */
    applyTheme() {
      applyDocumentTheme(this.current)
    },
    /**
     * 切换主题。
     *
     * 实现步骤：
     * 1. 校验用户选择的主题编码；
     * 2. 写入 store；
     * 3. 写入 localStorage；
     * 4. 立即同步到页面根节点。
     */
    setTheme(themeName: unknown) {
      if (!isThemeName(themeName)) {
        return
      }
      this.current = themeName
      saveTheme(themeName)
      applyDocumentTheme(themeName)
    }
  }
})
