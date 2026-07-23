// @vitest-environment jsdom
import { beforeEach, describe, expect, it, vi } from 'vitest'
import { executeVoiceCommand, type VoiceCommandContext } from '@/utils/voiceCommand'

describe('voiceCommand', () => {
  beforeEach(() => {
    document.body.innerHTML = ''
    installVisibleDomMocks()
  })

  it('does not navigate to a page without menu permission', async () => {
    const context = createContext([])

    const result = await executeVoiceCommand(context, '打开采购管理')

    expect(result.handled).toBe(false)
    expect(context.router.push).not.toHaveBeenCalled()
  })

  it('opens an authorized page with Sichuan navigation wording', async () => {
    const context = createContext(['PAGE_PURCHASE'])

    const result = await executeVoiceCommand(context, '看哈采购管理')

    expect(result.handled).toBe(true)
    expect(result.type).toBe('success')
    expect(context.router.push).toHaveBeenCalledWith('/purchase-orders')
  })

  it('does not click save when the user has not explicitly confirmed', async () => {
    const clicked = vi.fn()
    document.body.innerHTML = '<main class="workspace-body"><button id="save">保存</button></main>'
    document.querySelector('#save')?.addEventListener('click', clicked)

    const result = await executeVoiceCommand(createContext([]), '保存')

    expect(result.type).toBe('warning')
    expect(clicked).not.toHaveBeenCalled()
  })

  it('clicks save only after explicit Sichuan save confirmation', async () => {
    const clicked = vi.fn()
    document.body.innerHTML = '<main class="workspace-body"><button id="save">保存</button></main>'
    document.querySelector('#save')?.addEventListener('click', clicked)

    const result = await executeVoiceCommand(createContext([]), '确认保存起')

    expect(result.handled).toBe(true)
    expect(result.type).toBe('success')
    expect(clicked).toHaveBeenCalledTimes(1)
  })

  it('fills form fields but does not submit when save is included in the same utterance', async () => {
    const clicked = vi.fn()
    document.body.innerHTML = `
      <main class="workspace-body">
        <div class="el-form-item">
          <label class="el-form-item__label">供应商</label>
          <input id="supplier" />
        </div>
        <button id="save">保存</button>
      </main>
    `
    document.querySelector('#save')?.addEventListener('click', clicked)

    const result = await executeVoiceCommand(createContext([]), '供应商弄成青岗供应商并保存起')

    expect(result.handled).toBe(true)
    expect(result.type).toBe('warning')
    expect((document.querySelector('#supplier') as HTMLInputElement).value).toBe('青岗供应商')
    expect(clicked).not.toHaveBeenCalled()
  })

  it('does not treat vague Sichuan confirmation as a write confirmation', async () => {
    const clicked = vi.fn()
    document.body.innerHTML = '<main class="workspace-body"><button id="save">保存</button></main>'
    document.querySelector('#save')?.addEventListener('click', clicked)

    const result = await executeVoiceCommand(createContext([]), '要得')

    expect(result.type).toBe('warning')
    expect(clicked).not.toHaveBeenCalled()
  })

  it('requires an existing delete confirmation dialog before final delete confirmation', async () => {
    const result = await executeVoiceCommand(createContext([]), '确认删掉')

    expect(result.type).toBe('warning')
    expect(result.message).toContain('删除确认框')
  })

  it('confirms delete only inside a delete confirmation dialog', async () => {
    const clicked = vi.fn()
    document.body.innerHTML = `
      <div class="el-message-box">
        <p>确认删除该菜单？</p>
        <button id="confirm">确认</button>
      </div>
    `
    document.querySelector('#confirm')?.addEventListener('click', clicked)

    const result = await executeVoiceCommand(createContext([]), '确认删掉')

    expect(result.type).toBe('success')
    expect(clicked).toHaveBeenCalledTimes(1)
  })
})

/**
 * 创建语音命令所需的最小路由和菜单授权上下文。
 */
function createContext(menuCodes: string[], initialPath = '/dashboard'): VoiceCommandContext {
  const router = {
    currentRoute: {
      value: {
        path: initialPath
      }
    },
    push: vi.fn(async (path: string) => {
      router.currentRoute.value.path = path
    })
  }
  return {
    router: router as unknown as VoiceCommandContext['router'],
    auth: {
      menus: [],
      hasMenu: (code: string) => menuCodes.includes(code)
    } as unknown as VoiceCommandContext['auth']
  }
}

/**
 * 安装 jsdom 可见性和 innerText 兼容能力，让语音命令的 DOM 过滤逻辑与浏览器行为接近。
 */
function installVisibleDomMocks() {
  Object.defineProperty(HTMLElement.prototype, 'innerText', {
    configurable: true,
    get() {
      return this.textContent || ''
    },
    set(value: string) {
      this.textContent = value
    }
  })
  HTMLElement.prototype.getClientRects = vi.fn(() => [{
    bottom: 1,
    height: 1,
    left: 0,
    right: 1,
    top: 0,
    width: 1,
    x: 0,
    y: 0,
    toJSON: () => ({})
  }] as unknown as DOMRectList)
}
