<template>
  <div class="voice-command-widget" :class="{ 'is-listening': listening, 'is-open': panelVisible }">
    <transition name="voice-panel">
      <section v-if="panelVisible" class="voice-panel" aria-label="语音操作">
        <header class="voice-panel-header">
          <div class="voice-title">
            <el-icon><Headset /></el-icon>
            <strong>语音操作</strong>
          </div>
          <el-button :icon="Close" circle text title="关闭" @click="panelVisible = false" />
        </header>
        <div class="voice-panel-body">
          <div v-if="listening" class="voice-wave" aria-label="正在识别">
            <span v-for="index in 9" :key="index" :style="{ animationDelay: `${index * 70}ms` }" />
          </div>
          <p class="voice-status" :class="statusType">{{ statusText }}</p>
          <div v-if="transcript" class="voice-transcript">
            <span>识别结果</span>
            <p>{{ transcript }}</p>
          </div>
        </div>
      </section>
    </transition>

    <el-tooltip :content="tooltipText" placement="top" effect="light">
      <span class="voice-fab-wrapper">
        <button
          class="voice-fab"
          type="button"
          :disabled="!supported"
          :aria-pressed="listening"
          :aria-label="listening ? '关闭语音操作' : '开启语音操作'"
          @click="toggleListening"
        >
          <span class="voice-fab-icon" aria-hidden="true">
            <el-icon><Microphone /></el-icon>
            <span v-if="!listening" class="voice-fab-slash" />
          </span>
        </button>
      </span>
    </el-tooltip>
  </div>
</template>

<script setup lang="ts">
import { computed, onBeforeUnmount, onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { Close, Headset, Microphone } from '@element-plus/icons-vue'
import { useAuthStore } from '@/stores/auth'
import { executeVoiceCommand, type VoiceCommandResultType } from '@/utils/voiceCommand'

/**
 * SpeechRecognitionAlternative 类型定义，描述浏览器语音识别返回的一条转写候选。
 */
interface SpeechRecognitionAlternative {
  /**
   * 字段 transcript：表示浏览器识别出的语音文本。
   */
  transcript: string
}

/**
 * SpeechRecognitionResult 类型定义，描述一次语音识别结果及其是否为最终文本。
 */
interface SpeechRecognitionResult {
  /**
   * 字段 isFinal：表示当前识别片段是否已经结束，可用于执行命令。
   */
  readonly isFinal: boolean
  /**
   * 字段 length：表示当前识别片段中的候选数量。
   */
  readonly length: number
  /**
   * 方法 item：按索引读取语音识别候选文本。
   */
  item(index: number): SpeechRecognitionAlternative
  /**
   * 索引签名：兼容 Web Speech API 的数组式结果访问。
   */
  [index: number]: SpeechRecognitionAlternative
}

/**
 * SpeechRecognitionResultList 类型定义，描述浏览器返回的语音识别结果集合。
 */
interface SpeechRecognitionResultList {
  /**
   * 字段 length：表示结果集合数量。
   */
  readonly length: number
  /**
   * 方法 item：按索引读取识别结果。
   */
  item(index: number): SpeechRecognitionResult
  /**
   * 索引签名：兼容 Web Speech API 的数组式结果集合访问。
   */
  [index: number]: SpeechRecognitionResult
}

/**
 * SpeechRecognitionEvent 类型定义，描述语音识别 result 事件。
 */
interface SpeechRecognitionEvent extends Event {
  /**
   * 字段 resultIndex：表示本次事件中新结果开始的位置。
   */
  readonly resultIndex: number
  /**
   * 字段 results：表示浏览器累计返回的识别结果集合。
   */
  readonly results: SpeechRecognitionResultList
}

/**
 * SpeechRecognitionErrorEvent 类型定义，描述语音识别错误事件。
 */
interface SpeechRecognitionErrorEvent extends Event {
  /**
   * 字段 error：表示浏览器语音识别错误码。
   */
  readonly error: string
}

/**
 * SpeechRecognitionInstance 类型定义，描述浏览器 Web Speech API 识别器实例。
 */
interface SpeechRecognitionInstance extends EventTarget {
  /**
   * 字段 continuous：表示是否持续监听语音命令。
   */
  continuous: boolean
  /**
   * 字段 interimResults：表示是否返回临时识别结果，用于面板实时显示。
   */
  interimResults: boolean
  /**
   * 字段 lang：表示识别语言，当前固定为中文。
   */
  lang: string
  /**
   * 字段 maxAlternatives：表示单次识别候选数量。
   */
  maxAlternatives: number
  /**
   * 字段 onend：表示识别服务结束后的回调。
   */
  onend: (() => void) | null
  /**
   * 字段 onerror：表示识别失败后的错误回调。
   */
  onerror: ((event: SpeechRecognitionErrorEvent) => void) | null
  /**
   * 字段 onresult：表示识别出文本后的结果回调。
   */
  onresult: ((event: SpeechRecognitionEvent) => void) | null
  /**
   * 方法 start：请求浏览器开始使用麦克风识别语音。
   */
  start(): void
  /**
   * 方法 stop：请求浏览器停止当前语音识别。
   */
  stop(): void
}

/**
 * SpeechRecognitionConstructor 类型定义，用于从浏览器 window 上创建识别器实例。
 */
interface SpeechRecognitionConstructor {
  /**
   * 构造函数：创建 Web Speech API 语音识别器。
   */
  new (): SpeechRecognitionInstance
}

/**
 * SpeechWindow 类型定义，兼容标准 SpeechRecognition 和 Chrome/Edge 的 webkitSpeechRecognition。
 */
type SpeechWindow = Window & {
  /**
   * 字段 SpeechRecognition：标准浏览器语音识别构造器。
   */
  SpeechRecognition?: SpeechRecognitionConstructor
  /**
   * 字段 webkitSpeechRecognition：Chromium 浏览器语音识别构造器。
   */
  webkitSpeechRecognition?: SpeechRecognitionConstructor
}

/**
 * MicrophoneSupportState 类型定义，描述当前浏览器检测到的麦克风可用状态。
 */
type MicrophoneSupportState = 'checking' | 'available' | 'unavailable' | 'unsupported' | 'error'

/**
 * 常量 router：用于语音命令打开菜单页面，路由守卫继续执行授权校验。
 */
const router = useRouter()
/**
 * 常量 auth：用于读取当前人员菜单资源，控制语音命令入口和页面跳转边界。
 */
const auth = useAuthStore()
/**
 * 常量 listening：表示浏览器麦克风识别是否处于监听状态。
 */
const listening = ref(false)
/**
 * 常量 manuallyStopped：区分用户主动停止和浏览器自动结束，避免自动重启造成误监听。
 */
const manuallyStopped = ref(false)
/**
 * 常量 panelVisible：控制语音操作状态面板是否展示。
 */
const panelVisible = ref(false)
/**
 * 常量 transcript：保存最近一次语音识别文本，便于用户确认执行依据。
 */
const transcript = ref('')
/**
 * 常量 statusText：保存语音入口当前状态或命令执行结果。
 */
const statusText = ref(supportsSpeechRecognition() ? '待命' : '浏览器不支持语音识别')
/**
 * 常量 statusType：保存状态文本的提示级别。
 */
const statusType = ref<VoiceCommandResultType>(supportsSpeechRecognition() ? 'info' : 'warning')
/**
 * 常量 recognition：缓存浏览器语音识别器，避免每次点击重复创建。
 */
const recognition = ref<SpeechRecognitionInstance>()
/**
 * 常量 microphoneState：保存当前电脑是否存在可用麦克风输入设备的检测结果。
 */
const microphoneState = ref<MicrophoneSupportState>(supportsMicrophoneDetection() ? 'checking' : 'unsupported')

/**
 * 常量 supported：表示当前浏览器语音识别和电脑麦克风检测结果是否都满足启用条件。
 */
const supported = computed(() => supportsSpeechRecognition() && isSecureVoiceContext() && microphoneState.value === 'available')
/**
 * 常量 tooltipText：根据浏览器、麦克风和监听状态生成悬浮提示。
 */
const tooltipText = computed(() => {
  if (!supportsSpeechRecognition() || microphoneState.value !== 'available') {
    return microphoneAvailabilityText()
  }
  return listening.value ? '停止语音操作' : '开始语音操作'
})

/**
 * 组件挂载后检测当前电脑是否存在麦克风。
 *
 * 实现步骤：调用浏览器媒体设备枚举能力，只检测音频输入设备，不主动申请麦克风权限；非安全上下文只提示原因。
 */
onMounted(() => {
  void detectMicrophoneSupport()
})

/**
 * 组件卸载前停止语音识别。
 *
 * 实现步骤：标记为手动停止，再调用浏览器识别器 stop，避免离开页面后继续监听。
 */
onBeforeUnmount(() => {
  manuallyStopped.value = true
  recognition.value?.stop()
})

/**
 * 切换语音监听状态。
 *
 * 实现步骤：
 * 1. 展开语音状态面板；
 * 2. 不支持 Web Speech API 时给出提示；
 * 3. 根据当前监听状态执行开始或停止。
 */
async function toggleListening() {
  panelVisible.value = true
  if (!supported.value) {
    showResult('warning', microphoneAvailabilityText())
    return
  }
  if (listening.value) {
    stopListening()
  } else {
    await startListening()
  }
}

/**
 * 开始浏览器语音识别。
 *
 * 实现步骤：
 * 1. 获取或创建 Web Speech API 识别器；
 * 2. 设置手动停止标记为 false；
 * 3. 启动识别并更新前端状态。
 */
async function startListening() {
  if (!supported.value) {
    showResult('warning', microphoneAvailabilityText())
    return
  }

  /**
   * 常量 instance：保存浏览器语音识别器实例。
   */
  const microphoneReady = await requestMicrophoneAccess()
  if (!microphoneReady) {
    return
  }
  const instance = ensureRecognition()
  if (!instance) {
    showResult('warning', '当前浏览器不支持语音识别')
    return
  }

  manuallyStopped.value = false
  try {
    instance.start()
    listening.value = true
    showResult('info', '正在识别')
  } catch {
    listening.value = true
  }
}

/**
 * 停止浏览器语音识别。
 *
 * 实现步骤：标记为用户主动停止，调用识别器 stop，并更新前端状态。
 */
function stopListening(showStopped = true) {
  manuallyStopped.value = true
  recognition.value?.stop()
  listening.value = false
  if (showStopped) {
    showResult('info', '语音识别已停止')
  }
}

/**
 * 获取或创建浏览器语音识别器。
 *
 * 实现步骤：
 * 1. 已存在实例时直接复用；
 * 2. 从 window 读取标准或 Chromium 语音识别构造器；
 * 3. 配置中文、单句识别、临时结果和事件回调。
 */
function ensureRecognition() {
  if (recognition.value) {
    return recognition.value
  }

  /**
   * 常量 SpeechRecognition：保存当前浏览器可用的语音识别构造器。
   */
  const SpeechRecognition = (window as SpeechWindow).SpeechRecognition || (window as SpeechWindow).webkitSpeechRecognition
  if (!SpeechRecognition) {
    return undefined
  }

  /**
   * 常量 instance：保存新创建的语音识别器实例。
   */
  const instance = new SpeechRecognition()
  instance.lang = 'zh-CN'
  instance.continuous = false
  instance.interimResults = true
  instance.maxAlternatives = 1
  instance.onresult = onSpeechResult
  instance.onerror = onSpeechError
  instance.onend = onSpeechEnd
  recognition.value = instance
  return instance
}

/**
 * 处理浏览器语音识别结果。
 *
 * 实现步骤：
 * 1. 汇总临时文本用于面板展示；
 * 2. 只在最终文本产生后执行命令；
 * 3. 命令执行通过 voiceCommand 工具触发现有路由和页面按钮，不绕过业务权限。
 */
async function onSpeechResult(event: SpeechRecognitionEvent) {
  /**
   * 变量 finalText：保存已经结束、可以执行的语音文本。
   */
  let finalText = ''
  /**
   * 变量 interimText：保存识别中的临时文本，只用于展示。
   */
  let interimText = ''

  for (let index = event.resultIndex; index < event.results.length; index += 1) {
    /**
     * 常量 item：保存当前索引的识别结果。
     */
    const item = event.results[index]
    /**
     * 常量 text：保存当前识别候选的文本内容。
     */
    const text = item[0]?.transcript?.trim() || ''
    if (item.isFinal) {
      finalText += text
    } else {
      interimText += text
    }
  }

  transcript.value = finalText || interimText || transcript.value
  if (!finalText) {
    return
  }

  /**
   * 常量 result：保存语音命令执行后的处理结果。
   */
  const result = await executeVoiceCommand({ router, auth }, finalText)
  showResult(result.type, result.message)
  stopListening(false)
}

/**
 * 处理浏览器语音识别错误。
 *
 * 实现步骤：把浏览器错误码转换为中文提示，并停止前端监听状态。
 */
function onSpeechError(event: SpeechRecognitionErrorEvent) {
  /**
   * 常量 message：保存面向用户展示的中文错误信息。
   */
  const message = errorMessage(event.error)
  listening.value = false
  if (event.error === 'network' || event.error === 'service-not-allowed' || event.error === 'not-allowed') {
    manuallyStopped.value = true
  }
  showResult('warning', message)
}

/**
 * 处理语音识别结束事件。
 *
 * 实现步骤：浏览器判定一句话结束后关闭前端监听状态，下一次语音操作由用户再次点击触发。
 */
function onSpeechEnd() {
  listening.value = false
  if (!manuallyStopped.value) {
    manuallyStopped.value = true
    showResult('info', '一句话识别已结束')
  }
}

/**
 * 展示语音操作状态。
 *
 * 实现步骤：更新面板状态文本，并按结果级别调用 Element Plus 全局消息。
 */
function showResult(type: VoiceCommandResultType, message: string) {
  statusType.value = type
  statusText.value = message
  if (type === 'success') {
    ElMessage.success(message)
  } else if (type === 'warning') {
    ElMessage.warning(message)
  } else if (type === 'error') {
    ElMessage.error(message)
  }
}

/**
 * 将浏览器语音识别错误码转换为中文业务提示。
 *
 * 实现步骤：优先处理麦克风权限、无语音和网络错误，其他错误统一提示识别失败。
 */
function errorMessage(error: string) {
  if (error === 'not-allowed' || error === 'service-not-allowed') {
    return '麦克风权限未开启'
  }
  if (error === 'no-speech') {
    return '未识别到语音'
  }
  if (error === 'network') {
    return '浏览器语音识别服务连接失败，请检查网络或改用最新版 Edge/Chrome'
  }
  return '语音识别失败'
}

/** Requests microphone permission before starting the browser recognition service. */
async function requestMicrophoneAccess() {
  if (!navigator.mediaDevices?.getUserMedia) {
    showResult('warning', '当前浏览器无法申请麦克风权限')
    return false
  }
  try {
    const stream = await navigator.mediaDevices.getUserMedia({ audio: true })
    stream.getTracks().forEach((track) => track.stop())
    microphoneState.value = 'available'
    return true
  } catch (error) {
    microphoneState.value = 'error'
    const name = error instanceof DOMException ? error.name : ''
    showResult('warning', name === 'NotAllowedError' ? '麦克风权限未开启' : '无法使用麦克风，请检查设备和浏览器权限')
    return false
  }
}

/**
 * 检测当前电脑是否存在音频输入设备。
 *
 * 实现步骤：
 * 1. 浏览器不支持媒体设备枚举时标记为无法检测；
 * 2. 读取媒体设备列表并查找 audioinput；
 * 3. 根据检测结果更新按钮可用性和面板状态文案。
 */
async function detectMicrophoneSupport() {
  if (!isSecureVoiceContext()) {
    microphoneState.value = 'unsupported'
    syncMicrophoneStatus()
    return
  }
  if (!supportsMicrophoneDetection()) {
    microphoneState.value = 'unsupported'
    syncMicrophoneStatus()
    return
  }

  microphoneState.value = 'checking'
  syncMicrophoneStatus()

  try {
    /**
     * 常量 devices：保存浏览器当前可枚举的媒体设备列表。
     */
    const devices = await navigator.mediaDevices.enumerateDevices()
    /**
     * 常量 hasAudioInput：表示设备列表中是否存在麦克风或其他音频输入设备。
     */
    const hasAudioInput = devices.some((device) => device.kind === 'audioinput')
    microphoneState.value = hasAudioInput ? 'available' : 'unavailable'
  } catch {
    microphoneState.value = 'error'
  }
  syncMicrophoneStatus()
}

/**
 * 同步麦克风检测结果到状态面板。
 *
 * 实现步骤：监听中不覆盖执行状态；空闲时展示当前可用性和提示级别。
 */
function syncMicrophoneStatus() {
  if (listening.value) {
    return
  }
  statusType.value = microphoneState.value === 'available' ? 'info' : 'warning'
  statusText.value = microphoneAvailabilityText()
}

/**
 * 生成麦克风和语音识别能力提示文案。
 *
 * 实现步骤：先判断语音识别 API，再按麦克风检测状态返回用户可理解的提示。
 */
function microphoneAvailabilityText() {
  if (!supportsSpeechRecognition()) {
    return '当前浏览器不支持语音识别'
  }
  if (!isSecureVoiceContext()) {
    return '当前访问地址不是安全上下文，浏览器不会弹出麦克风授权；请使用 HTTPS、localhost 或 127.0.0.1'
  }
  if (microphoneState.value === 'checking') {
    return '正在检测麦克风'
  }
  if (microphoneState.value === 'available') {
    return '开始语音操作'
  }
  if (microphoneState.value === 'unavailable') {
    return '当前电脑未检测到麦克风'
  }
  if (microphoneState.value === 'unsupported') {
    return '当前浏览器无法检测麦克风'
  }
  return '麦克风检测失败'
}

/**
 * 判断当前浏览器是否支持麦克风设备枚举。
 *
 * 实现步骤：检查 navigator.mediaDevices.enumerateDevices 是否存在。
 */
function supportsMicrophoneDetection() {
  if (typeof navigator === 'undefined') {
    return false
  }
  return Boolean(navigator.mediaDevices?.enumerateDevices)
}

/**
 * 判断当前访问地址是否允许浏览器申请麦克风。
 *
 * 实现步骤：浏览器通常只允许 HTTPS、安全 localhost 和 127.0.0.1 使用麦克风；HTTP 内网 IP 可能不会弹授权提示。
 */
function isSecureVoiceContext() {
  if (typeof window === 'undefined') {
    return false
  }
  const hostname = window.location.hostname
  return window.isSecureContext || hostname === 'localhost' || hostname === '127.0.0.1'
}

/**
 * 判断当前浏览器是否支持语音识别。
 *
 * 实现步骤：检查 window 上是否存在标准 SpeechRecognition 或 webkitSpeechRecognition 构造器。
 */
function supportsSpeechRecognition() {
  if (typeof window === 'undefined') {
    return false
  }
  return Boolean((window as SpeechWindow).SpeechRecognition || (window as SpeechWindow).webkitSpeechRecognition)
}
</script>

<style scoped>
.voice-command-widget {
  position: fixed;
  right: 88px;
  bottom: 24px;
  z-index: 902;
}

.voice-fab {
  display: grid;
  width: 52px;
  height: 52px;
  place-items: center;
  border: 0;
  border-radius: 8px;
  background: #8b95a1;
  color: #fff;
  box-shadow: 0 12px 28px rgb(17 24 39 / 18%);
  cursor: pointer;
  transition: background-color 0.16s ease, box-shadow 0.16s ease, transform 0.16s ease;
}

.voice-fab-wrapper {
  display: inline-grid;
}

.voice-fab:disabled {
  cursor: not-allowed;
  opacity: 0.58;
}

.voice-fab-icon {
  position: relative;
  display: grid;
  width: 28px;
  height: 28px;
  place-items: center;
}

.voice-fab-icon .el-icon {
  font-size: 24px;
}

.voice-command-widget.is-listening .voice-fab {
  background: #38bdf8;
  box-shadow: 0 14px 30px rgb(56 189 248 / 28%);
}

.voice-fab-slash {
  position: absolute;
  width: 34px;
  height: 4px;
  border-radius: 999px;
  background: #dc2626;
  box-shadow: 0 0 0 1px rgb(255 255 255 / 72%);
  transform: rotate(-45deg);
  transform-origin: center;
}

.voice-panel {
  position: absolute;
  right: 0;
  bottom: 64px;
  width: min(320px, calc(100vw - 32px));
  border: 1px solid #d8e4f2;
  border-radius: 8px;
  background: var(--surface-color);
  color: var(--text-color);
  box-shadow: 0 20px 45px rgb(17 24 39 / 18%);
  overflow: hidden;
}

.voice-panel-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  min-height: 48px;
  padding: 10px 12px;
  border-bottom: 1px solid var(--border-color);
}

.voice-title {
  display: inline-flex;
  align-items: center;
  gap: 8px;
  color: var(--heading-color);
  font-size: 14px;
}

.voice-title .el-icon {
  color: #245c8f;
  font-size: 18px;
}

.voice-panel-body {
  display: grid;
  gap: 8px;
  padding: 12px;
}

.voice-wave {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 4px;
  height: 38px;
}

.voice-wave span {
  width: 3px;
  height: 10px;
  border-radius: 2px;
  background: #38bdf8;
  animation: voice-wave 0.8s ease-in-out infinite alternate;
}

@keyframes voice-wave {
  from { transform: scaleY(0.45); opacity: 0.45; }
  to { transform: scaleY(2.6); opacity: 1; }
}

.voice-status,
.voice-transcript {
  margin: 0;
  line-height: 1.5;
  overflow-wrap: anywhere;
}

.voice-transcript span {
  color: var(--muted-text-color);
  font-size: 11px;
  font-weight: 700;
}

.voice-transcript p {
  margin: 4px 0 0;
  color: var(--text-color);
  font-size: 13px;
}

.voice-status {
  font-size: 13px;
  font-weight: 700;
}

.voice-status.success {
  color: var(--success-color);
}

.voice-status.warning {
  color: var(--warning-color);
}

.voice-status.error {
  color: var(--danger-color);
}

.voice-status.info {
  color: var(--muted-text-color);
}

.voice-transcript {
  padding: 8px;
  border: 1px solid var(--soft-border-color);
  border-radius: 6px;
  background: var(--subtle-surface-color);
  color: var(--secondary-text-color);
  font-size: 12px;
}

.voice-panel-enter-active,
.voice-panel-leave-active {
  transition: opacity 0.16s ease, transform 0.16s ease;
}

.voice-panel-enter-from,
.voice-panel-leave-to {
  opacity: 0;
  transform: translateY(8px);
}

@media (max-width: 640px) {
  .voice-command-widget {
    right: 76px;
    bottom: 14px;
  }

  .voice-panel {
    right: -62px;
  }
}
</style>
