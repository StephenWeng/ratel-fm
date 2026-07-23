<template>
  <div class="page">
    <div class="page-header">
      <div>
        <h1 class="page-title">智能检索</h1>
        <p class="page-subtitle">统一检索业务单据、附件和知识索引。</p>
      </div>
      <el-button v-if="auth.hasMenu('BTN_SEARCH_QUERY')" :loading="rebuilding" @click="rebuild">重建索引</el-button>
    </div>

    <div class="search-band">
      <el-input v-model="keyword" size="large" clearable placeholder="输入基础资料、科目、凭证号、单号、供应商、物料、附件内容等关键词" @keyup.enter="load" />
      <el-segmented v-model="mode" :options="modeOptions" />
      <el-button v-if="auth.hasMenu('BTN_SEARCH_QUERY')" type="primary" :icon="Search" :loading="loading" @click="load">检索</el-button>
    </div>

    <div class="panel">
      <div v-if="lastResponse" class="search-meta">
        <span>{{ modeLabel(lastResponse.mode) }}</span>
        <span>结果 {{ lastResponse.total }} 条</span>
        <el-tag size="small" :type="lastResponse.aiEnabled ? 'success' : 'warning'">{{ lastResponse.aiEnabled ? '语义检索可用' : '仅关键词检索' }}</el-tag>
      </div>
      <div v-if="lastResponse?.rewrittenQueries?.length" class="rewrite-meta">
        <span>改写检索</span>
        <el-tag v-for="item in lastResponse.rewrittenQueries" :key="item" size="small" effect="plain">{{ item }}</el-tag>
      </div>
      <el-table v-loading="loading" :data="results" stripe empty-text="暂无结果">
        <el-table-column label="类型" width="120">
          <template #default="{ row }"><el-tag>{{ row.category || typeLabel(row.type) }}</el-tag></template>
        </el-table-column>
        <el-table-column label="编码/单号" min-width="160">
          <template #default="{ row }">
            <span v-html="highlightText(row.sourceNo || '')"></span>
          </template>
        </el-table-column>
        <el-table-column label="内容" min-width="420">
          <template #default="{ row }">
            <div class="result-title" v-html="highlightText(row.title)"></div>
            <div class="result-summary" v-html="highlightText(row.summary)"></div>
            <div v-if="contentExcerpt(row)" class="result-content" v-html="highlightText(contentExcerpt(row))"></div>
          </template>
        </el-table-column>
        <el-table-column label="评分" width="100" align="right">
          <template #default="{ row }">{{ percent(row.score) }}</template>
        </el-table-column>
      </el-table>
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { Search } from '@element-plus/icons-vue'
import { api } from '@/api/fm'
import { useAuthStore } from '@/stores/auth'
import type { SearchResponse, SearchResult } from '@/types/api'

/**
 * 常量 auth：保存当前模块的页面状态、配置项、接口实例或计算结果。
 */
const auth = useAuthStore()
/**
 * 常量 keyword：保存当前模块的页面状态、配置项、接口实例或计算结果。
 */
const keyword = ref('')
/**
 * 常量 mode：保存当前模块的页面状态、配置项、接口实例或计算结果。
 */
const mode = ref('hybrid')
/**
 * 常量 loading：保存当前模块的页面状态、配置项、接口实例或计算结果。
 */
const loading = ref(false)
/**
 * 常量 rebuilding：保存当前模块的页面状态、配置项、接口实例或计算结果。
 */
const rebuilding = ref(false)
/**
 * 常量 results：保存当前模块的页面状态、配置项、接口实例或计算结果。
 */
const results = ref<SearchResult[]>([])
/**
 * 常量 lastResponse：保存当前模块的页面状态、配置项、接口实例或计算结果。
 */
const lastResponse = ref<SearchResponse>()

/**
 * 常量 highlightTerms：保存当前检索词和改写词拆出的高亮片段。
 */
const highlightTerms = computed(() => buildHighlightTerms(keyword.value, lastResponse.value?.rewrittenQueries || []))

/**
 * 常量 modeOptions：保存当前模块的页面状态、配置项、接口实例或计算结果。
 */
const modeOptions = [
  { label: '混合', value: 'hybrid' },
  { label: '关键词', value: 'keyword' },
  { label: '语义', value: 'semantic' }
]

/**
 * 执行 typeLabel 方法。
 * 
 * 实现步骤：
 * 1. 读取当前页面状态或调用参数；
 * 2. 完成对应的校验、接口调用或数据转换；
 * 3. 更新页面状态或返回处理结果。
 */
function typeLabel(type: string) {
  return {
    /**
     * 字段 BASIC_DICTIONARY：表示基础资料字典知识来源。
     */
    BASIC_DICTIONARY: '基础字典',
    /**
     * 字段 SUBJECT：表示表单、筛选条件、接口数据或组件状态中的 SUBJECT 值。
     */
    SUBJECT: '科目',
    /**
     * 字段 VOUCHER：表示表单、筛选条件、接口数据或组件状态中的 VOUCHER 值。
     */
    VOUCHER: '凭证',
    /**
     * 字段 PURCHASE_ORDER：表示表单、筛选条件、接口数据或组件状态中的 PURCHASE_ORDER 值。
     */
    PURCHASE_ORDER: '采购',
    /**
     * 字段 SHIPMENT：表示表单、筛选条件、接口数据或组件状态中的 SHIPMENT 值。
     */
    SHIPMENT: '物流',
    /**
     * 字段 INVENTORY_LEDGER：表示表单、筛选条件、接口数据或组件状态中的 INVENTORY_LEDGER 值。
     */
    INVENTORY_LEDGER: '库存',
    /**
     * 字段 AR_AP_BILL：表示表单、筛选条件、接口数据或组件状态中的 AR_AP_BILL 值。
     */
    AR_AP_BILL: '应收应付',
    /**
     * 字段 ATTACHMENT：表示表单、筛选条件、接口数据或组件状态中的 ATTACHMENT 值。
     */
    ATTACHMENT: '附件'
  }[type] || type
}

/**
 * 执行 modeLabel 方法。
 * 
 * 实现步骤：
 * 1. 读取当前页面状态或调用参数；
 * 2. 完成对应的校验、接口调用或数据转换；
 * 3. 更新页面状态或返回处理结果。
 */
function modeLabel(value: string) {
  return {
    /**
     * 字段 hybrid：表示表单、筛选条件、接口数据或组件状态中的 hybrid 值。
     */
    hybrid: '混合检索',
    /**
     * 字段 keyword：表示表单、筛选条件、接口数据或组件状态中的 keyword 值。
     */
    keyword: '关键词检索',
    /**
     * 字段 semantic：表示表单、筛选条件、接口数据或组件状态中的 semantic 值。
     */
    semantic: '语义检索'
  }[value] || value
}

/**
 * 执行 percent 方法。
 * 
 * 实现步骤：
 * 1. 读取当前页面状态或调用参数；
 * 2. 完成对应的校验、接口调用或数据转换；
 * 3. 更新页面状态或返回处理结果。
 */
function percent(value: number) {
  return `${Math.round((value || 0) * 100)}%`
}

/**
 * 对检索结果文本做安全高亮。
 *
 * 实现步骤：
 * 1. 使用当前关键词和改写关键词生成候选片段；
 * 2. 对普通文本先做 HTML 转义；
 * 3. 仅对命中片段包裹 mark 标签，避免后端内容直接注入页面。
 */
function highlightText(value: unknown) {
  const text = String(value ?? '')
  const terms = highlightTerms.value
  if (!text || terms.length === 0) {
    return escapeHtml(text)
  }
  const pattern = new RegExp(terms.map(escapeRegExp).join('|'), 'gi')
  let html = ''
  let lastIndex = 0
  let match: RegExpExecArray | null
  while ((match = pattern.exec(text)) !== null) {
    html += escapeHtml(text.slice(lastIndex, match.index))
    html += `<mark class="search-highlight">${escapeHtml(match[0])}</mark>`
    lastIndex = match.index + match[0].length
    if (match[0].length === 0) {
      pattern.lastIndex += 1
    }
  }
  html += escapeHtml(text.slice(lastIndex))
  return html
}

/**
 * 截取命中词附近的正文片段，辅助用户判断结果为何被召回。
 */
function contentExcerpt(row: SearchResult) {
  const content = String(row.content || '')
  if (!content || content === row.summary) {
    return ''
  }
  const lowerContent = content.toLowerCase()
  const term = highlightTerms.value.find((item) => lowerContent.includes(item.toLowerCase()))
  if (!term) {
    return ''
  }
  const index = lowerContent.indexOf(term.toLowerCase())
  const start = Math.max(0, index - 60)
  const end = Math.min(content.length, index + Math.max(term.length, 1) + 160)
  return `${start > 0 ? '...' : ''}${content.slice(start, end)}${end < content.length ? '...' : ''}`
}

/**
 * 生成高亮词列表，兼容中文自然问句。
 */
function buildHighlightTerms(original: string, rewrittenQueries: string[]) {
  const values = [original, ...rewrittenQueries]
  const terms = new Set<string>()
  values.forEach((value) => {
    splitHighlightTerms(value).forEach((term) => terms.add(term))
  })
  return Array.from(terms)
    .filter((term) => term.length >= 2 && term.length <= 40)
    .sort((left, right) => right.length - left.length)
    .slice(0, 24)
}

/**
 * 拆分高亮词，去掉问句虚词后保留核心中文片段。
 */
function splitHighlightTerms(value: string) {
  const text = String(value || '').trim()
  if (!text) {
    return []
  }
  const parts = text.split(/[\s,，。；;]+/).filter(Boolean)
  let cleaned = text
  ;[
    '有没有',
    '是否',
    '查询',
    '检索',
    '搜索',
    '查看',
    '查找',
    '帮我',
    '当前',
    '系统',
    '里面',
    '现在',
    '这个',
    '一下',
    '相关',
    '记录',
    '数据',
    '内容',
    '情况',
    '信息',
    '哪些',
    '有什么',
    '物流',
    '运输',
    '发货地',
    '目的地',
    '发货',
    '送达',
    '承运商',
    '的',
    '了',
    '吗',
    '呢',
    '那'
  ].forEach((item) => {
    cleaned = cleaned.replaceAll(item, ' ')
  })
  cleaned.split(/[\s,，。；;]+/).forEach((item) => {
    if (item) {
      parts.push(item)
    }
  })
  return parts
}

/**
 * 转义 HTML 特殊字符，供 v-html 安全展示。
 */
function escapeHtml(value: string) {
  return value
    .replaceAll('&', '&amp;')
    .replaceAll('<', '&lt;')
    .replaceAll('>', '&gt;')
    .replaceAll('"', '&quot;')
    .replaceAll("'", '&#39;')
}

/**
 * 转义正则特殊字符，避免检索词破坏高亮表达式。
 */
function escapeRegExp(value: string) {
  return value.replace(/[.*+?^${}()|[\]\\]/g, '\\$&')
}

/**
 * 执行 load 方法。
 * 
 * 实现步骤：
 * 1. 读取当前页面状态或调用参数；
 * 2. 完成对应的校验、接口调用或数据转换；
 * 3. 更新页面状态或返回处理结果。
 */
async function load() {
  if (!auth.hasMenu('BTN_SEARCH_QUERY')) {
    ElMessage.warning('无检索权限')
    return
  }
  if (!keyword.value.trim()) {
    ElMessage.warning('请输入检索关键词')
    return
  }
  loading.value = true
  try {
    /**
     * 常量 response：保存当前模块的页面状态、配置项、接口实例或计算结果。
     */
    const response = await api.search(keyword.value, mode.value)
    lastResponse.value = response
    results.value = response.results
  } finally {
    loading.value = false
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
  if (!auth.hasMenu('BTN_SEARCH_QUERY')) {
    ElMessage.warning('无检索权限')
    return
  }
  rebuilding.value = true
  try {
    /**
     * 常量 response：保存当前模块的页面状态、配置项、接口实例或计算结果。
     */
    const response = await api.rebuildKnowledge()
    ElMessage.success(`索引已重建，共 ${response.documentCount} 个分片`)
    if (keyword.value.trim()) {
      await load()
    }
  } finally {
    rebuilding.value = false
  }
}
</script>

<style scoped>
.search-band {
  display: grid;
  grid-template-columns: minmax(260px, 1fr) auto auto;
  gap: 12px;
  align-items: center;
  padding: 16px;
  border: 1px solid #e5e7eb;
  border-radius: 6px;
  background: #fff;
}
.search-meta {
  display: flex;
  gap: 12px;
  align-items: center;
  margin-bottom: 12px;
  color: #64748b;
  font-size: 13px;
}
.rewrite-meta {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  align-items: center;
  margin-bottom: 12px;
  color: #64748b;
  font-size: 13px;
}
.result-title {
  color: #111827;
  font-weight: 600;
}
.result-summary {
  margin-top: 4px;
  color: #64748b;
  line-height: 1.5;
  white-space: pre-wrap;
}
.result-content {
  margin-top: 6px;
  color: #475569;
  line-height: 1.55;
  white-space: pre-wrap;
}
:deep(.search-highlight) {
  padding: 0 2px;
  border-radius: 3px;
  background: #fef08a;
  color: #854d0e;
}
@media (max-width: 760px) {
  .search-band {
    grid-template-columns: 1fr;
  }
}
</style>
