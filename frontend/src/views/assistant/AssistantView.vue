<template>
  <div class="page">
    <div class="page-header">
      <div>
        <h1 class="page-title">ratel助手</h1>
        <p class="page-subtitle">基于业务数据、附件和知识索引的问答。</p>
      </div>
    </div>
    <el-tabs v-model="activeTab" class="assistant-tabs">
      <el-tab-pane label="助手问答" name="chat">
        <div class="search-band">
          <el-input v-model="question" type="textarea" :rows="4" placeholder="例如：哪些应收快到期？某个供应商最近有哪些采购和附件？" />
          <div class="actions">
            <el-button v-if="auth.hasMenu('BTN_ASSISTANT_ASK')" type="primary" :loading="loading" @click="ask">提问</el-button>
            <el-button v-if="auth.hasMenu('BTN_ASSISTANT_ASK')" :loading="rebuilding" @click="rebuild">重建索引</el-button>
          </div>
        </div>
        <div v-if="answer" class="panel panel-pad">
          <div class="answer-head">
            <h3>回答</h3>
            <el-tag size="small" :type="answer.aiEnabled ? 'success' : 'warning'">{{ answer.aiEnabled ? answer.model : '未配置模型' }}</el-tag>
          </div>
          <p class="answer-text">{{ answer.answer }}</p>
          <div v-if="canEnterAnswer(answer)" class="answer-action">
            <el-button type="primary" @click="enterResponse(answer)">进入</el-button>
          </div>
          <template v-if="responseCitations(answer).length">
            <el-divider />
            <h3>引用来源</h3>
            <div class="citation-list">
              <div v-for="item in responseCitations(answer)" :key="item.id" class="citation-item">
                <div class="citation-title">
                  <el-tag size="small">{{ item.category }}</el-tag>
                  <span>{{ item.title }}</span>
                  <el-button v-if="canEnterCitation(item)" size="small" link type="primary" @click="enter(item)">进入</el-button>
                  <small>{{ Math.round(item.score * 100) }}%</small>
                </div>
                <a v-if="item.url" class="citation-link" :href="item.url" target="_blank" rel="noopener noreferrer">{{ item.url }}</a>
                <p>{{ item.summary }}</p>
              </div>
            </div>
          </template>
          <template v-if="responseSuggestions(answer).length">
            <el-divider />
            <el-tag v-for="item in responseSuggestions(answer)" :key="item" class="tag">{{ item }}</el-tag>
          </template>
        </div>
      </el-tab-pane>
      <el-tab-pane label="业务 Agent" name="agent">
        <BusinessAgentPanel
          ref="businessAgentRef"
          :question="question"
          :modules="['purchase', 'shipment', 'inventory', 'arAp', 'finance', 'workflow']"
          :agent-types="['query', 'reconciliation', 'voucherSuggestion', 'dueReminder', 'workflowAssistant', 'inventoryRisk', 'businessAnalysis', 'knowledgeQa']"
          placeholder="例如：帮我检查采购、库存、应付、出纳和凭证链路有没有风险"
        />
      </el-tab-pane>
      <el-tab-pane label="本地知识库" name="knowledge">
    <div class="panel panel-pad knowledge-panel">
      <div class="knowledge-head">
        <div>
          <h3>本地知识库</h3>
          <p>上传历史文档、制度资料、合同模板或图片扫描件，入库后会参与 ratel助手本地检索。</p>
        </div>
        <el-button :loading="knowledgeLoading" @click="loadLocalKnowledge">刷新</el-button>
      </div>
      <el-form class="knowledge-upload" label-position="top">
        <el-form-item label="资料标题">
          <el-input v-model="knowledgeTitle" maxlength="120" show-word-limit placeholder="不填则使用文件名" />
        </el-form-item>
        <el-form-item label="资料说明">
          <el-input v-model="knowledgeDescription" maxlength="300" show-word-limit placeholder="例如：财务制度、物流操作手册、客户合同模板" />
        </el-form-item>
        <el-upload
          :auto-upload="false"
          :show-file-list="false"
          accept=".pdf,.docx,.xlsx,.txt,.md,.csv,.json,.xml,.html,.png,.jpg,.jpeg,.webp,.bmp"
          :on-change="uploadLocalKnowledge"
        >
          <el-button type="primary" :loading="knowledgeUploading">上传资料</el-button>
        </el-upload>
      </el-form>
      <el-table v-loading="knowledgeLoading" :data="localKnowledgeDocuments" size="small" row-key="id">
        <el-table-column prop="title" label="标题" min-width="180" show-overflow-tooltip />
        <el-table-column prop="originalName" label="文件" min-width="180" show-overflow-tooltip />
        <el-table-column label="状态" width="110">
          <template #default="{ row }">
            <el-tag :type="knowledgeStatusType(row.status)" size="small">{{ knowledgeStatusLabel(row.status) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="分片" width="80" prop="chunkCount" />
        <el-table-column label="OCR" width="80">
          <template #default="{ row }">{{ row.ocrUsed ? '是' : '否' }}</template>
        </el-table-column>
        <el-table-column label="大小" width="100">
          <template #default="{ row }">{{ formatFileSize(row.fileSize) }}</template>
        </el-table-column>
        <el-table-column label="错误" min-width="160" show-overflow-tooltip>
          <template #default="{ row }">{{ row.errorMessage || '-' }}</template>
        </el-table-column>
        <el-table-column label="操作" width="150" fixed="right">
          <template #default="{ row }">
            <el-button link type="primary" :loading="row.id === knowledgeRebuildingId" @click="rebuildLocalKnowledge(row.id)">重建</el-button>
            <el-button link type="danger" @click="deleteLocalKnowledge(row.id)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>
    </div>
      </el-tab-pane>
    </el-tabs>
  </div>
</template>

<script setup lang="ts">
import { nextTick, onBeforeUnmount, ref } from 'vue'
import { ElMessage, ElMessageBox, type UploadFile } from 'element-plus'
import { useRouter } from 'vue-router'
import { api } from '@/api/fm'
import { useAuthStore } from '@/stores/auth'
import type { AiAssistantResponse, AiConversationMessage, LocalKnowledgeDocumentView } from '@/types/api'
import { canEnterAnswer, canEnterCitation, enterAnswer, enterCitation } from '@/utils/aiNavigation'
import BusinessAgentPanel from '@/components/agent/BusinessAgentPanel.vue'

/**
 * 常量 auth：保存当前模块的页面状态、配置项、接口实例或计算结果。
 */
const auth = useAuthStore()
/**
 * 常量 router：保存当前模块的页面状态、配置项、接口实例或计算结果。
 */
const router = useRouter()
/**
 * 常量 question：保存当前模块的页面状态、配置项、接口实例或计算结果。
 */
const question = ref('')
const activeTab = ref<'chat' | 'agent' | 'knowledge'>('chat')
const businessAgentRef = ref<InstanceType<typeof BusinessAgentPanel>>()
/**
 * 常量 loading：保存当前模块的页面状态、配置项、接口实例或计算结果。
 */
const loading = ref(false)
/**
 * 常量 rebuilding：保存当前模块的页面状态、配置项、接口实例或计算结果。
 */
const rebuilding = ref(false)
/**
 * 常量 answer：保存当前模块的页面状态、配置项、接口实例或计算结果。
 */
const answer = ref<AiAssistantResponse>()
/**
 * 常量 conversationSummary：保存当前页面会话短摘要。
 */
const conversationSummary = ref('')
/**
 * 常量 conversationMessages：保存当前页面最近会话消息。
 */
const conversationMessages = ref<AiConversationMessage[]>([])
const localKnowledgeDocuments = ref<LocalKnowledgeDocumentView[]>([])
const knowledgeLoading = ref(false)
const knowledgeUploading = ref(false)
const knowledgeRebuildingId = ref<number>()
const knowledgeTitle = ref('')
const knowledgeDescription = ref('')
/**
 * 变量 streamController：保存当前流式请求取消控制器。
 */
let streamController: AbortController | undefined
/**
 * 变量 knowledgeRefreshTimer：本地知识库后台入库状态轮询定时器。
 */
let knowledgeRefreshTimer: number | undefined

onBeforeUnmount(() => {
  streamController?.abort()
  stopKnowledgeStatusPolling()
})

void loadLocalKnowledge()
/**
 * 执行 ask 方法。
 * 
 * 实现步骤：
 * 1. 读取当前页面状态或调用参数；
 * 2. 完成对应的校验、接口调用或数据转换；
 * 3. 更新页面状态或返回处理结果。
 */
async function ask() {
  if (!auth.hasMenu('BTN_ASSISTANT_ASK')) {
    ElMessage.warning('无 ratel助手提问权限')
    return
  }
  if (!question.value.trim()) {
    ElMessage.warning('请输入问题')
    return
  }
  loading.value = true
  try {
    const text = question.value.trim()
    const context = {
      conversationSummary: conversationSummary.value,
      conversationMessages: conversationMessages.value
    }
    let finalResponse: AiAssistantResponse | undefined
    let streamedText = ''
    let pendingResponse: AiAssistantResponse | undefined
    let streamErrorMessage = ''
    streamController?.abort()
    streamController = new AbortController()
    answer.value = undefined
    try {
      await api.streamAssistant(text, 'local', context, {
        signal: streamController.signal,
        onMeta: (response) => {
          pendingResponse = normalizeResponse(response, streamedText)
          if (answer.value) {
            answer.value = pendingResponse
          }
        },
        onDelta: (content) => {
          streamedText += content
          if (!streamedText.trim() && !answer.value) {
            return
          }
          answer.value = normalizeResponse(answer.value || pendingResponse || emptyAssistantResponse(text, 'local'), streamedText)
        },
        onDone: (response) => {
          finalResponse = normalizeResponse(response, streamedText || response.answer || '')
          if (finalResponse.answer.trim()) {
            answer.value = finalResponse
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
          finalResponse = normalizeResponse(answer.value || pendingResponse || emptyAssistantResponse(text, 'local'), streamedText)
          answer.value = finalResponse
        }
      } else if (!streamedText.trim()) {
        finalResponse = normalizeResponse(await api.askAssistant(text, 'local', context))
        if (finalResponse.answer.trim()) {
          answer.value = finalResponse
        }
      } else if (!finalResponse) {
        finalResponse = normalizeResponse(answer.value || pendingResponse || emptyAssistantResponse(text, 'local'), streamedText)
        answer.value = finalResponse
      } else {
        streamErrorMessage = error instanceof Error ? error.message : streamErrorMessage
      }
      if (!finalResponse && !streamedText.trim() && streamErrorMessage) {
        ElMessage.error(streamErrorMessage)
      }
    }
    if (finalResponse) {
      conversationSummary.value = finalResponse.conversationSummary || conversationSummary.value
      conversationMessages.value.push({ role: 'user', content: text }, { role: 'assistant', content: finalResponse.answer })
      trimConversationMessages(finalResponse.recentRawRounds)
      await triggerBusinessAgentByIntent(text)
    }
  } finally {
    loading.value = false
    streamController = undefined
  }
}

async function triggerBusinessAgentByIntent(text: string) {
  const agentTypes = agentTypesByQuestion(text)
  if (agentTypes.length === 0) {
    return
  }
  activeTab.value = 'agent'
  await nextTick()
  await businessAgentRef.value?.runAgent({
    question: text,
    modules: ['purchase', 'shipment', 'inventory', 'arAp', 'finance', 'workflow'],
    agentTypes,
    stage: 'readOnly',
    limit: 5
  })
}

function agentTypesByQuestion(text: string) {
  const result = new Set<string>()
  if (/对账|核对|链路|一致|匹配/.test(text)) result.add('reconciliation')
  if (/到期|逾期|未核销|待收|待付/.test(text)) result.add('dueReminder')
  if (/制证|凭证建议|生成凭证|会计平台/.test(text)) result.add('voucherSuggestion')
  if (/负库存|低库存|库存风险|调拨/.test(text)) result.add('inventoryRisk')
  return Array.from(result)
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
 * 构造流式输出开始前的空响应。
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
 * 读取引用来源数组，兼容异常响应未返回 citations 的情况。
 */
function responseCitations(response?: AiAssistantResponse) {
  return Array.isArray(response?.citations) ? response.citations : []
}

/**
 * 读取建议数组，兼容异常响应未返回 suggestions 的情况。
 */
function responseSuggestions(response?: AiAssistantResponse) {
  return Array.isArray(response?.suggestions) ? response.suggestions : []
}

/**
 * 按服务端返回的轮次裁剪当前页面最近消息。
 */
function trimConversationMessages(recentRawRounds?: number) {
  const rounds = Math.max(1, recentRawRounds || 4)
  const maxMessages = rounds * 2
  if (conversationMessages.value.length > maxMessages) {
    conversationMessages.value = conversationMessages.value.slice(conversationMessages.value.length - maxMessages)
  }
}

/**
 * 执行 rebuild 方法。
 * 
 * 实现步骤：
 * 1. 读取当前页面状态或调用参数；
 * 2. 完成对应的校验、接口调用或数据转换；
 * 3. 更新页面状态或返回处理结果。
 */
async function rebuild() {
  if (!auth.hasMenu('BTN_ASSISTANT_ASK')) {
    ElMessage.warning('无 ratel助手提问权限')
    return
  }
  rebuilding.value = true
  try {
    /**
     * 常量 response：保存当前模块的页面状态、配置项、接口实例或计算结果。
     */
    const response = await api.rebuildKnowledge()
    ElMessage.success(`索引已重建，共 ${response.documentCount} 个分片`)
  } finally {
    rebuilding.value = false
  }
}

async function loadLocalKnowledge() {
  if (knowledgeLoading.value) {
    return
  }
  knowledgeLoading.value = true
  try {
    localKnowledgeDocuments.value = await api.localKnowledgeDocuments()
    refreshKnowledgeStatusPolling()
  } finally {
    knowledgeLoading.value = false
  }
}

async function uploadLocalKnowledge(uploadFile: UploadFile) {
  const file = uploadFile.raw
  if (!file) {
    return
  }
  knowledgeUploading.value = true
  try {
    const payload = new FormData()
    payload.append('file', file)
    if (knowledgeTitle.value.trim()) {
      payload.append('title', knowledgeTitle.value.trim())
    }
    if (knowledgeDescription.value.trim()) {
      payload.append('description', knowledgeDescription.value.trim())
    }
    const document = await api.uploadLocalKnowledgeDocument(payload)
    ElMessage.success(`${document.title} 已上传，正在后台入库`)
    knowledgeTitle.value = ''
    knowledgeDescription.value = ''
    await loadLocalKnowledge()
  } finally {
    knowledgeUploading.value = false
  }
}

async function rebuildLocalKnowledge(id: number) {
  knowledgeRebuildingId.value = id
  try {
    const document = await api.rebuildLocalKnowledgeDocument(id)
    ElMessage.success(`${document.title} 已重建，生成 ${document.chunkCount} 个分片`)
    await loadLocalKnowledge()
  } finally {
    knowledgeRebuildingId.value = undefined
  }
}

async function deleteLocalKnowledge(id: number) {
  await ElMessageBox.confirm('删除后该资料将不再参与本地知识库检索。', '删除本地资料', { type: 'warning' })
  await api.deleteLocalKnowledgeDocument(id)
  ElMessage.success('本地资料已删除')
  await loadLocalKnowledge()
}

/**
 * 根据当前资料状态刷新后台入库轮询。
 *
 * 实现步骤：
 * 1. 存在 PENDING 或 INDEXING 资料时启动定时刷新；
 * 2. 全部完成或失败后停止轮询；
 * 3. 避免每次 loadLocalKnowledge 重复创建多个定时器。
 */
function refreshKnowledgeStatusPolling() {
  const hasRunningTask = localKnowledgeDocuments.value.some((item) => item.status === 'PENDING' || item.status === 'INDEXING')
  if (!hasRunningTask) {
    stopKnowledgeStatusPolling()
    return
  }
  if (knowledgeRefreshTimer) {
    return
  }
  knowledgeRefreshTimer = window.setInterval(() => {
    void loadLocalKnowledge()
  }, 3000)
}

/**
 * 停止本地知识库后台入库轮询。
 */
function stopKnowledgeStatusPolling() {
  if (!knowledgeRefreshTimer) {
    return
  }
  window.clearInterval(knowledgeRefreshTimer)
  knowledgeRefreshTimer = undefined
}

function knowledgeStatusLabel(status: LocalKnowledgeDocumentView['status']) {
  return {
    PENDING: '待入库',
    INDEXING: '入库中',
    INDEXED: '已入库',
    FAILED: '失败'
  }[status] || status
}

function knowledgeStatusType(status: LocalKnowledgeDocumentView['status']) {
  if (status === 'INDEXED') return 'success'
  if (status === 'FAILED') return 'danger'
  if (status === 'INDEXING') return 'warning'
  return 'info'
}

function formatFileSize(size?: number) {
  const value = Number(size || 0)
  if (value >= 1024 * 1024) return `${(value / 1024 / 1024).toFixed(1)} MB`
  if (value >= 1024) return `${(value / 1024).toFixed(1)} KB`
  return `${value} B`
}

/**
 * 执行 enter 方法。
 * 
 * 实现步骤：
 * 1. 读取当前页面状态或调用参数；
 * 2. 完成对应的校验、接口调用或数据转换；
 * 3. 更新页面状态或返回处理结果。
 */
async function enter(item: AiAssistantResponse['citations'][number]) {
  await enterCitation(router, item)
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
  await enterAnswer(router, response)
}
</script>

<style scoped>
.search-band{display:grid;gap:12px;padding:16px;border:1px solid #e5e7eb;border-radius:6px;background:#fff;}
.actions{display:flex;gap:8px;justify-content:flex-end;}
.knowledge-panel{margin-top:16px;}
.knowledge-head{display:flex;align-items:flex-start;justify-content:space-between;gap:16px;margin-bottom:12px;}
.knowledge-head h3{margin:0;color:#111827;}
.knowledge-head p{margin:6px 0 0;color:#64748b;}
.knowledge-upload{display:grid;grid-template-columns:minmax(160px,1fr) minmax(220px,1.5fr) auto;gap:12px;align-items:end;margin-bottom:12px;}
.knowledge-upload :deep(.el-form-item){margin-bottom:0;}
.answer-head{display:flex;align-items:center;gap:10px;}
.answer-text{white-space:pre-wrap;line-height:1.7;color:#1f2937;}
.answer-action{display:flex;justify-content:flex-end;margin:8px 0 2px;}
.citation-list{display:grid;gap:10px;}
.citation-item{border:1px solid #e5e7eb;border-radius:6px;padding:10px;background:#fff;}
.citation-title{display:flex;align-items:center;gap:8px;font-weight:600;color:#111827;}
.citation-title span{min-width:0;overflow:hidden;text-overflow:ellipsis;white-space:nowrap;}
.citation-title small{margin-left:auto;color:#64748b;font-weight:400;}
.citation-link{display:block;margin-top:8px;color:#2563eb;font-size:12px;overflow-wrap:anywhere;text-decoration:none;}
.citation-link:hover{text-decoration:underline;}
.citation-item p{margin:8px 0 0;color:#64748b;line-height:1.5;}
.tag{margin-right:8px;margin-bottom:8px;}
@media (max-width: 900px){
  .knowledge-upload{grid-template-columns:1fr;}
}
</style>
