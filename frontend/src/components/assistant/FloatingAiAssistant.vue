<template>
  <button
    class="ai-fab"
    title="ratel助手"
    aria-label="打开ratel助手"
    :style="fabStyle"
    @pointerdown="onFabPointerDown"
    @click="onFabClick"
  >
    <SystemLogo />
  </button>

  <transition name="ai-panel">
    <section v-if="visible" class="ai-panel" aria-label="ratel助手">
      <header class="ai-panel-header">
        <div class="ai-title">
          <span class="ai-title-icon">
            <SystemLogo />
          </span>
          <div>
            <strong>ratel助手</strong>
          </div>
        </div>
        <el-button :icon="Close" circle text title="关闭" @click="visible = false" />
      </header>

      <main ref="bodyRef" class="ai-panel-body" @scroll.passive="onConversationScroll">
        <div v-if="messages.length === 0" class="ai-empty">
          <strong>暂无对话</strong>
        </div>
        <div v-for="message in messages" :key="message.id" class="message" :class="message.role">
          <div class="message-bubble">
            <el-button
              v-if="message.role === 'user'"
              class="message-copy"
              :icon="CopyDocument"
              circle
              text
              size="small"
              title="复制问题"
              aria-label="复制问题"
              @click.stop="copyMessage(message.content)"
            />
            <p>{{ message.content }}</p>
            <div v-if="message.response && canEnterAnswer(message.response)" class="answer-action">
              <el-button size="small" type="primary" @click="enterResponse(message.response)">进入</el-button>
            </div>
          </div>
        </div>
      </main>

      <el-button
        v-if="showJumpToLatest"
        class="jump-latest"
        :icon="ArrowDown"
        circle
        title="查看最新消息"
        aria-label="查看最新消息"
        @click="scrollToBottom"
      />

      <footer class="ai-panel-footer">
        <div v-if="loading" class="ai-retrieving">正在检索</div>
        <el-input
          v-model="question"
          type="textarea"
          :rows="3"
          resize="none"
          maxlength="500"
          show-word-limit
          :disabled="loading"
          placeholder="输入问题"
          @keyup.enter.exact="ask"
        />
        <el-button type="primary" :icon="Promotion" :loading="loading" @click="ask">发送</el-button>
      </footer>
    </section>
  </transition>
</template>

<script setup lang="ts">
import { computed, nextTick, onBeforeUnmount, onMounted, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { ArrowDown, Close, CopyDocument, Promotion } from '@element-plus/icons-vue'
import { useRouter } from 'vue-router'
import { api } from '@/api/fm'
import SystemLogo from '@/components/brand/SystemLogo.vue'
import type { AiAssistantResponse, AiConversationMessage } from '@/types/api'
import { canEnterAnswer, enterAnswer } from '@/utils/aiNavigation'

/**
 * AssistantMessage 类型定义，用于约束页面状态、接口入参或接口返回数据结构。
 */
interface AssistantMessage {
  /**
   * 字段 id：表示表单、筛选条件、接口数据或组件状态中的 id 值。
   */
  id: number
  /**
   * 字段 role：表示表单、筛选条件、接口数据或组件状态中的 role 值。
   */
  role: 'user' | 'assistant'
  /**
   * 字段 content：表示表单、筛选条件、接口数据或组件状态中的 content 值。
   */
  content: string
  /**
   * 字段 response：表示表单、筛选条件、接口数据或组件状态中的 response 值。
   */
  response?: AiAssistantResponse
}

/**
 * 常量 visible：保存当前模块的页面状态、配置项、接口实例或计算结果。
 */
const visible = ref(false)
/**
 * 常量 router：保存当前模块的页面状态、配置项、接口实例或计算结果。
 */
const router = useRouter()
/**
 * 常量 question：保存当前模块的页面状态、配置项、接口实例或计算结果。
 */
const question = ref('')
/**
 * 常量 loading：保存当前模块的页面状态、配置项、接口实例或计算结果。
 */
const loading = ref(false)
/**
 * 常量 messages：保存当前模块的页面状态、配置项、接口实例或计算结果。
 */
const messages = ref<AssistantMessage[]>([])
/**
 * 常量 lastResponse：保存当前模块的页面状态、配置项、接口实例或计算结果。
 */
const lastResponse = ref<AiAssistantResponse>()
/**
 * 常量 conversationSummary：保存 ratel助手当前浮窗会话短摘要。
 */
const conversationSummary = ref('')
/**
 * 常量 bodyRef：保存当前模块的页面状态、配置项、接口实例或计算结果。
 */
const bodyRef = ref<HTMLElement>()
const showJumpToLatest = ref(false)
const conversationPinnedToBottom = ref(true)
/**
 * 常量 fabPosition：保存当前模块的页面状态、配置项、接口实例或计算结果。
 */
const fabPosition = ref<{ x: number; y: number }>()
/**
 * 变量 messageId：保存当前模块的页面状态、配置项、接口实例或计算结果。
 */
let messageId = 0
/**
 * 变量 suppressNextClick：保存当前模块的页面状态、配置项、接口实例或计算结果。
 */
let suppressNextClick = false
/**
 * 变量 dragState：保存当前模块的页面状态、配置项、接口实例或计算结果。
 */
let dragState:
  | {
      /**
       * 字段 pointerId：表示表单、筛选条件、接口数据或组件状态中的 pointerId 值。
       */
      pointerId: number
      /**
       * 字段 startClientX：表示表单、筛选条件、接口数据或组件状态中的 startClientX 值。
       */
      startClientX: number
      /**
       * 字段 startClientY：表示表单、筛选条件、接口数据或组件状态中的 startClientY 值。
       */
      startClientY: number
      /**
       * 字段 startX：表示表单、筛选条件、接口数据或组件状态中的 startX 值。
       */
      startX: number
      /**
       * 字段 startY：表示表单、筛选条件、接口数据或组件状态中的 startY 值。
       */
      startY: number
      /**
       * 字段 moved：表示表单、筛选条件、接口数据或组件状态中的 moved 值。
       */
      moved: boolean
    }
  | undefined
/**
 * 变量 streamController：保存当前助手流式请求取消控制器。
 */
let streamController: AbortController | undefined
/**
 * 常量 FAB_SIZE：保存当前模块的页面状态、配置项、接口实例或计算结果。
 */
const FAB_SIZE = 52
/**
 * 常量 FAB_MARGIN：保存当前模块的页面状态、配置项、接口实例或计算结果。
 */
const FAB_MARGIN = 12

/**
 * 常量 fabStyle：保存当前模块的页面状态、配置项、接口实例或计算结果。
 */
const fabStyle = computed(() => {
  if (!fabPosition.value) {
    return {}
  }
  return {
    /**
     * 字段 left：表示表单、筛选条件、接口数据或组件状态中的 left 值。
     */
    left: `${fabPosition.value.x}px`,
    /**
     * 字段 top：表示表单、筛选条件、接口数据或组件状态中的 top 值。
     */
    top: `${fabPosition.value.y}px`,
    /**
     * 字段 right：表示表单、筛选条件、接口数据或组件状态中的 right 值。
     */
    right: 'auto',
    /**
     * 字段 bottom：表示表单、筛选条件、接口数据或组件状态中的 bottom 值。
     */
    bottom: 'auto'
  }
})

onMounted(() => {
  window.addEventListener('resize', clampFabToViewport)
})

onBeforeUnmount(() => {
  streamController?.abort()
  removeDragListeners()
  window.removeEventListener('resize', clampFabToViewport)
})

/**
 * 执行 onFabClick 方法。
 * 
 * 实现步骤：
 * 1. 读取当前页面状态或调用参数；
 * 2. 完成对应的校验、接口调用或数据转换；
 * 3. 更新页面状态或返回处理结果。
 */
function onFabClick() {
  if (suppressNextClick) {
    suppressNextClick = false
    return
  }
  visible.value = true
}

/**
 * 执行 onFabPointerDown 方法。
 * 
 * 实现步骤：
 * 1. 读取当前页面状态或调用参数；
 * 2. 完成对应的校验、接口调用或数据转换；
 * 3. 更新页面状态或返回处理结果。
 */
function onFabPointerDown(event: PointerEvent) {
  if (event.pointerType === 'mouse' && event.button !== 0) {
    return
  }
  /**
   * 常量 target：保存当前模块的页面状态、配置项、接口实例或计算结果。
   */
  const target = event.currentTarget as HTMLElement
  /**
   * 常量 rect：保存当前模块的页面状态、配置项、接口实例或计算结果。
   */
  const rect = target.getBoundingClientRect()
  dragState = {
    /**
     * 字段 pointerId：表示表单、筛选条件、接口数据或组件状态中的 pointerId 值。
     */
    pointerId: event.pointerId,
    /**
     * 字段 startClientX：表示表单、筛选条件、接口数据或组件状态中的 startClientX 值。
     */
    startClientX: event.clientX,
    /**
     * 字段 startClientY：表示表单、筛选条件、接口数据或组件状态中的 startClientY 值。
     */
    startClientY: event.clientY,
    /**
     * 字段 startX：表示表单、筛选条件、接口数据或组件状态中的 startX 值。
     */
    startX: rect.left,
    /**
     * 字段 startY：表示表单、筛选条件、接口数据或组件状态中的 startY 值。
     */
    startY: rect.top,
    /**
     * 字段 moved：表示表单、筛选条件、接口数据或组件状态中的 moved 值。
     */
    moved: false
  }
  target.setPointerCapture?.(event.pointerId)
  window.addEventListener('pointermove', onFabPointerMove)
  window.addEventListener('pointerup', onFabPointerUp)
  window.addEventListener('pointercancel', onFabPointerUp)
}

/**
 * 执行 onFabPointerMove 方法。
 * 
 * 实现步骤：
 * 1. 读取当前页面状态或调用参数；
 * 2. 完成对应的校验、接口调用或数据转换；
 * 3. 更新页面状态或返回处理结果。
 */
function onFabPointerMove(event: PointerEvent) {
  if (!dragState || event.pointerId !== dragState.pointerId) {
    return
  }
  /**
   * 常量 dx：保存当前模块的页面状态、配置项、接口实例或计算结果。
   */
  const dx = event.clientX - dragState.startClientX
  /**
   * 常量 dy：保存当前模块的页面状态、配置项、接口实例或计算结果。
   */
  const dy = event.clientY - dragState.startClientY
  if (!dragState.moved && Math.hypot(dx, dy) < 4) {
    return
  }
  dragState.moved = true
  fabPosition.value = clampPosition(dragState.startX + dx, dragState.startY + dy)
  event.preventDefault()
}

/**
 * 执行 onFabPointerUp 方法。
 * 
 * 实现步骤：
 * 1. 读取当前页面状态或调用参数；
 * 2. 完成对应的校验、接口调用或数据转换；
 * 3. 更新页面状态或返回处理结果。
 */
function onFabPointerUp(event: PointerEvent) {
  if (!dragState || event.pointerId !== dragState.pointerId) {
    return
  }
  suppressNextClick = dragState.moved
  dragState = undefined
  removeDragListeners()
}

/**
 * 执行 removeDragListeners 方法。
 * 
 * 实现步骤：
 * 1. 读取当前页面状态或调用参数；
 * 2. 完成对应的校验、接口调用或数据转换；
 * 3. 更新页面状态或返回处理结果。
 */
function removeDragListeners() {
  window.removeEventListener('pointermove', onFabPointerMove)
  window.removeEventListener('pointerup', onFabPointerUp)
  window.removeEventListener('pointercancel', onFabPointerUp)
}

/**
 * 执行 clampFabToViewport 方法。
 * 
 * 实现步骤：
 * 1. 读取当前页面状态或调用参数；
 * 2. 完成对应的校验、接口调用或数据转换；
 * 3. 更新页面状态或返回处理结果。
 */
function clampFabToViewport() {
  if (fabPosition.value) {
    fabPosition.value = clampPosition(fabPosition.value.x, fabPosition.value.y)
  }
}

/**
 * 执行 clampPosition 方法。
 * 
 * 实现步骤：
 * 1. 读取当前页面状态或调用参数；
 * 2. 完成对应的校验、接口调用或数据转换；
 * 3. 更新页面状态或返回处理结果。
 */
function clampPosition(x: number, y: number) {
  /**
   * 常量 maxX：保存当前模块的页面状态、配置项、接口实例或计算结果。
   */
  const maxX = Math.max(FAB_MARGIN, window.innerWidth - FAB_SIZE - FAB_MARGIN)
  /**
   * 常量 maxY：保存当前模块的页面状态、配置项、接口实例或计算结果。
   */
  const maxY = Math.max(FAB_MARGIN, window.innerHeight - FAB_SIZE - FAB_MARGIN)
  return {
    /**
     * 字段 x：表示表单、筛选条件、接口数据或组件状态中的 x 值。
     */
    x: Math.min(Math.max(x, FAB_MARGIN), maxX),
    /**
     * 字段 y：表示表单、筛选条件、接口数据或组件状态中的 y 值。
     */
    y: Math.min(Math.max(y, FAB_MARGIN), maxY)
  }
}

/**
 * 执行 ask 方法。
 * 
 * 实现步骤：
 * 1. 读取当前页面状态或调用参数；
 * 2. 完成对应的校验、接口调用或数据转换；
 * 3. 更新页面状态或返回处理结果。
 */
async function ask() {
  /**
   * 常量 text：保存当前模块的页面状态、配置项、接口实例或计算结果。
   */
  const text = question.value.trim()
  if (!text || loading.value) {
    return
  }
  question.value = ''
  const context = assistantContext()
  messages.value.push({
    /**
     * 字段 id：表示表单、筛选条件、接口数据或组件状态中的 id 值。
     */
    id: ++messageId,
    /**
     * 字段 role：表示表单、筛选条件、接口数据或组件状态中的 role 值。
     */
    role: 'user',
    /**
     * 字段 content：表示表单、筛选条件、接口数据或组件状态中的 content 值。
     */
    content: text
  })
  await scrollToBottom()
  loading.value = true
  try {
    let streamedText = ''
    let finalResponse: AiAssistantResponse | undefined
    let assistantMessage: AssistantMessage | undefined
    let pendingResponse: AiAssistantResponse | undefined
    let streamErrorMessage = ''
    streamController?.abort()
    streamController = new AbortController()
    try {
      await api.streamAssistant(text, 'local', context, {
        signal: streamController.signal,
        onMeta: (response) => {
          pendingResponse = normalizeResponse(response, streamedText)
          if (assistantMessage) {
            assistantMessage.response = pendingResponse
          }
          lastResponse.value = pendingResponse
        },
        onDelta: (content) => {
          streamedText += content
          if (!streamedText.trim() && !assistantMessage) {
            return
          }
          assistantMessage = ensureAssistantMessage(assistantMessage, pendingResponse)
          assistantMessage.content = streamedText
          assistantMessage.response = normalizeResponse(assistantMessage.response || pendingResponse || emptyAssistantResponse(text, 'local'), streamedText)
          if (conversationPinnedToBottom.value) {
            void scrollToBottom()
          } else {
            showJumpToLatest.value = true
          }
        },
        onDone: (response) => {
          finalResponse = normalizeResponse(response, streamedText || response.answer || '')
          if (finalResponse.answer.trim()) {
            assistantMessage = ensureAssistantMessage(assistantMessage, pendingResponse)
            assistantMessage.content = finalResponse.answer
            assistantMessage.response = finalResponse
            lastResponse.value = finalResponse
          }
        },
        onError: (message) => {
          streamErrorMessage = message
        }
      })
    } catch (error) {
      if (streamController.signal.aborted) {
        return
      }
      if (streamErrorMessage) {
        if (!streamedText.trim()) {
          ElMessage.error(streamErrorMessage)
        } else if (!finalResponse) {
          const response = assistantMessage?.response || pendingResponse || emptyAssistantResponse(text, 'local')
          finalResponse = normalizeResponse(response, streamedText)
          if (assistantMessage) {
            assistantMessage.response = finalResponse
          }
        }
      } else if (!streamedText.trim()) {
        const response = await api.askAssistant(text, 'local', context)
        finalResponse = normalizeResponse(response)
        if (finalResponse.answer.trim()) {
          assistantMessage = ensureAssistantMessage(assistantMessage, finalResponse)
          assistantMessage.content = finalResponse.answer
          assistantMessage.response = finalResponse
          lastResponse.value = finalResponse
        }
      } else if (!finalResponse) {
        const response = assistantMessage?.response || pendingResponse || emptyAssistantResponse(text, 'local')
        finalResponse = normalizeResponse(response, streamedText)
        if (assistantMessage) {
          assistantMessage.response = finalResponse
        }
      } else {
        streamErrorMessage = error instanceof Error ? error.message : streamErrorMessage
      }
      if (!finalResponse && !streamedText.trim() && streamErrorMessage) {
        ElMessage.error(streamErrorMessage)
      }
    }
    if (finalResponse) {
      conversationSummary.value = finalResponse.conversationSummary || conversationSummary.value
      trimConversationMessages(finalResponse.recentRawRounds)
    }
    await scrollToBottom()
  } catch {
    ElMessage.error('ratel助手请求失败')
  } finally {
    loading.value = false
    streamController = undefined
  }
}

/**
 * 确保助手回答真正有内容时才渲染气泡，避免空白输出框造成误导。
 */
function ensureAssistantMessage(current?: AssistantMessage, response?: AiAssistantResponse) {
  if (current) {
    return current
  }
  const message: AssistantMessage = {
    /**
     * 字段 id：表示当前助手消息的唯一标识。
     */
    id: ++messageId,
    /**
     * 字段 role：表示消息来自助手。
     */
    role: 'assistant',
    /**
     * 字段 content：表示助手已输出的回答内容。
     */
    content: '',
    /**
     * 字段 response：保存助手回答元数据。
     */
    response
  }
  messages.value.push(message)
  return message
}

/**
 * 规整助手响应，防止异常流式返回缺少数组字段时导致页面渲染中断。
 */
function normalizeResponse(response: AiAssistantResponse, answer = response.answer || ''): AiAssistantResponse {
  return {
    ...response,
    answer,
    citations: Array.isArray(response.citations) ? response.citations : [],
    suggestions: Array.isArray(response.suggestions) ? response.suggestions : []
  }
}

/**
 * 构造本地空响应，覆盖先收到流式文本、暂未收到 meta 的边界情况。
 */
function emptyAssistantResponse(question: string, mode: AiAssistantResponse['mode']): AiAssistantResponse {
  return {
    question,
    answer: '',
    aiEnabled: true,
    model: '生成中',
    mode,
    citations: [],
    suggestions: [],
    conversationSummary: conversationSummary.value,
    recentRawRounds: 4
  }
}

/**
 * 构造发送给服务端的会话上下文。
 */
function assistantContext() {
  return {
    /**
     * 字段 conversationSummary：表示当前会话短摘要。
     */
    conversationSummary: conversationSummary.value,
    /**
     * 字段 conversationMessages：表示最近会话消息。
     */
    conversationMessages: messages.value.map(toConversationMessage)
  }
}

/**
 * 转换为后端会话消息。
 */
function toConversationMessage(message: AssistantMessage): AiConversationMessage {
  return {
    /**
     * 字段 role：表示消息角色。
     */
    role: message.role,
    /**
     * 字段 content：表示消息内容。
     */
    content: message.content
  }
}

/**
 * 按服务端返回的轮次裁剪浮窗本地消息。
 */
function trimConversationMessages(recentRawRounds?: number) {
  const rounds = Math.max(1, recentRawRounds || 4)
  const maxMessages = rounds * 2 + 2
  if (messages.value.length > maxMessages) {
    messages.value = messages.value.slice(messages.value.length - maxMessages)
  }
}

/**
 * 执行 scrollToBottom 方法。
 * 
 * 实现步骤：
 * 1. 读取当前页面状态或调用参数；
 * 2. 完成对应的校验、接口调用或数据转换；
 * 3. 更新页面状态或返回处理结果。
 */
async function scrollToBottom() {
  await nextTick()
  if (bodyRef.value) {
    bodyRef.value.scrollTop = bodyRef.value.scrollHeight
  }
  conversationPinnedToBottom.value = true
  showJumpToLatest.value = false
}

function onConversationScroll() {
  const body = bodyRef.value
  if (!body) {
    return
  }
  const distance = body.scrollHeight - body.scrollTop - body.clientHeight
  conversationPinnedToBottom.value = distance <= 48
  showJumpToLatest.value = !conversationPinnedToBottom.value && messages.value.length > 0
}

/**
 * 复制用户问题文本，方便再次粘贴到输入框或其它业务字段。
 */
async function copyMessage(content: string) {
  const text = content.trim()
  if (!text) {
    return
  }
  try {
    if (navigator.clipboard?.writeText) {
      await navigator.clipboard.writeText(text)
    } else {
      fallbackCopyText(text)
    }
    ElMessage.success('已复制问题')
  } catch {
    fallbackCopyText(text)
    ElMessage.success('已复制问题')
  }
}

/**
 * 使用临时 textarea 兜底复制，覆盖不支持 Clipboard API 的浏览器环境。
 */
function fallbackCopyText(text: string) {
  const textarea = document.createElement('textarea')
  textarea.value = text
  textarea.setAttribute('readonly', 'readonly')
  textarea.style.position = 'fixed'
  textarea.style.left = '-9999px'
  textarea.style.top = '0'
  document.body.appendChild(textarea)
  textarea.select()
  document.execCommand('copy')
  document.body.removeChild(textarea)
}

/**
 * 执行 enterResponse 方法。
 * 
 * 实现步骤：
 * 1. 读取当前页面状态或调用参数；
 * 2. 完成对应的校验、接口调用或数据转换；
 * 3. 更新页面状态或返回处理结果。
 */
async function enterResponse(response: AiAssistantResponse) {
  visible.value = false
  await enterAnswer(router, response)
}
</script>

<style scoped>
.ai-fab {
  position: fixed;
  right: 24px;
  bottom: 24px;
  z-index: 900;
  display: grid;
  width: 52px;
  height: 52px;
  place-items: center;
  border: 0;
  border-radius: 8px;
  background: var(--primary-color);
  color: var(--primary-contrast);
  box-shadow: 0 12px 28px var(--shadow-color);
  cursor: pointer;
  touch-action: none;
  user-select: none;
}

.ai-fab :deep(.system-logo) {
  width: 28px;
  height: 28px;
}

.ai-panel {
  position: fixed;
  right: 24px;
  bottom: 88px;
  z-index: 901;
  display: grid;
  grid-template-rows: auto minmax(0, 1fr) auto;
  width: min(420px, calc(100vw - 32px));
  max-height: min(680px, calc(100vh - 112px));
  border: 1px solid var(--border-color);
  border-radius: 8px;
  background: var(--surface-color);
  color: var(--text-color);
  box-shadow: 0 20px 45px var(--shadow-color);
  overflow: hidden;
}

.jump-latest {
  position: absolute;
  left: 50%;
  bottom: 116px;
  z-index: 4;
  transform: translateX(-50%);
  box-shadow: 0 6px 18px var(--shadow-color);
}

.ai-retrieving {
  color: var(--primary-color);
  font-size: 12px;
  font-weight: 700;
}

.ai-panel-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  min-height: 58px;
  padding: 12px 14px;
  border-bottom: 1px solid var(--border-color);
  background: var(--surface-color);
}

.ai-title {
  display: flex;
  align-items: center;
  gap: 10px;
  min-width: 0;
}

.ai-title-icon {
  display: grid;
  width: 34px;
  height: 34px;
  place-items: center;
  border-radius: 6px;
  background: var(--primary-light-color);
  color: var(--primary-color);
}

.ai-title-icon :deep(.system-logo) {
  width: 22px;
  height: 22px;
}

.ai-title strong,
.ai-title span {
  display: block;
}

.ai-title strong {
  color: var(--heading-color);
  font-size: 15px;
  line-height: 1.2;
}

.ai-title span {
  margin-top: 3px;
  color: var(--muted-text-color);
  font-size: 12px;
}

.ai-panel-body {
  display: grid;
  align-content: start;
  gap: 12px;
  min-width: 0;
  padding: 14px;
  overflow-x: hidden;
  overflow-y: auto;
  background: var(--subtle-surface-color);
}

.ai-empty {
  display: grid;
  gap: 6px;
  min-height: 150px;
  align-content: center;
  justify-items: center;
  color: var(--muted-text-color);
  text-align: center;
}

.ai-empty strong {
  color: var(--heading-color);
  font-size: 16px;
}

.ai-empty span {
  max-width: 260px;
  line-height: 1.55;
  font-size: 13px;
}

.message {
  display: flex;
  min-width: 0;
  max-width: 100%;
}

.message.user {
  justify-content: flex-end;
}

.message.assistant {
  justify-content: flex-start;
}

.message-bubble {
  position: relative;
  max-width: 88%;
  min-width: 0;
  padding: 10px 12px;
  border: 1px solid var(--border-color);
  border-radius: 8px;
  background: var(--surface-color);
  color: var(--text-color);
  overflow-wrap: anywhere;
  word-break: break-word;
}

.message.user .message-bubble {
  padding-right: 38px;
  border-color: var(--primary-color);
  background: var(--primary-color);
  color: var(--primary-contrast);
}

.message-copy {
  position: absolute;
  top: 6px;
  right: 6px;
  width: 24px;
  height: 24px;
  color: var(--primary-contrast);
  opacity: 0;
  transition: opacity 0.15s ease, background-color 0.15s ease;
}

.message.user .message-bubble:hover .message-copy,
.message-copy:focus-visible {
  opacity: 1;
}

.message-copy:hover,
.message-copy:focus-visible {
  background: rgb(255 255 255 / 18%);
  color: var(--primary-contrast);
}

.message-bubble p {
  margin: 0;
  max-width: 100%;
  white-space: pre-wrap;
  overflow-wrap: anywhere;
  word-break: break-word;
  line-height: 1.6;
  font-size: 13px;
}

.answer-action {
  display: flex;
  justify-content: flex-end;
  margin-top: 8px;
}

.ai-panel-footer {
  display: grid;
  grid-template-columns: minmax(0, 1fr) auto;
  align-items: end;
  gap: 10px;
  padding: 12px;
  border-top: 1px solid var(--border-color);
  background: var(--surface-color);
}

.ai-panel-enter-active,
.ai-panel-leave-active {
  transition: opacity 0.16s ease, transform 0.16s ease;
}

.ai-panel-enter-from,
.ai-panel-leave-to {
  opacity: 0;
  transform: translateY(8px);
}

@media (max-width: 640px) {
  .ai-fab {
    right: 14px;
    bottom: 14px;
  }

  .ai-panel {
    right: 14px;
    bottom: 76px;
    width: calc(100vw - 28px);
    max-height: calc(100vh - 96px);
  }

  .ai-panel-footer {
    grid-template-columns: 1fr;
  }

  .ai-panel-footer .el-button {
    width: 100%;
  }
}
</style>
