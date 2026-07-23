<template>
  <section class="page">
    <div class="page-header">
      <div>
        <h1>AI 组件状态</h1>
        <p>查看大模型、向量库、知识索引和流式输出的当前运行情况。</p>
      </div>
      <el-button type="primary" :loading="loading" @click="loadStatus">
        <el-icon><Refresh /></el-icon>
        刷新
      </el-button>
    </div>

    <div v-if="status" class="summary-grid">
      <div class="summary-item">
        <span>大模型</span>
        <strong>{{ status.modelProvider || '-' }}</strong>
        <small>{{ status.primaryChatModel || '未检测到主模型' }}</small>
      </div>
      <div class="summary-item">
        <span>向量库</span>
        <strong>{{ status.vectorProvider || '-' }}</strong>
        <small>知识分片 {{ status.indexDocumentCount }}</small>
      </div>
      <div class="summary-item">
        <span>Embedding</span>
        <strong>{{ status.embeddingModel || '-' }}</strong>
        <small>{{ status.lastRebuildAt ? `最近重建 ${formatTime(status.lastRebuildAt)}` : '暂无重建记录' }}</small>
      </div>
      <div class="summary-item">
        <span>流式输出</span>
        <strong>{{ status.streamEnabled ? '开启' : '关闭' }}</strong>
        <small>检查时间 {{ formatTime(status.checkedAt) }}</small>
      </div>
      <div class="summary-item">
        <span>业务 Agent</span>
        <strong>{{ status.agentEnabled ? '开启' : '关闭' }}</strong>
        <small>{{ status.agentEnabled ? '入口和调用可用' : '入口隐藏，调用阻断' }}</small>
      </div>
    </div>

    <el-alert
      v-if="status?.lastRebuildError"
      class="status-alert"
      type="warning"
      :closable="false"
      :title="`最近一次索引重建失败：${status.lastRebuildError}`"
    />

    <el-table v-loading="loading" :data="status?.components || []" border class="status-table" empty-text="暂无状态数据">
      <el-table-column prop="name" label="组件" width="180" />
      <el-table-column label="状态" width="120">
        <template #default="{ row }">
          <el-tag :type="statusTagType(row.status)" effect="light">{{ statusLabel(row.status) }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="detail" label="说明" min-width="360" />
    </el-table>

    <h2 class="section-title">索引来源统计</h2>
    <el-table :data="status?.sourceTypeCounts || []" border class="status-table" empty-text="暂无索引来源统计">
      <el-table-column label="来源类型" min-width="220">
        <template #default="{ row }">
          <div class="source-type">
            <span>{{ sourceTypeLabel(row.sourceType) }}</span>
            <small>{{ row.sourceType }}</small>
          </div>
        </template>
      </el-table-column>
      <el-table-column prop="count" label="分片数量" width="140" />
    </el-table>
  </section>
</template>

<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { Refresh } from '@element-plus/icons-vue'
import { ElMessage } from 'element-plus'
import { api } from '@/api/fm'
import type { AiComponentStatusResponse, AiComponentStatusItem } from '@/types/api'

/**
 * status 保存当前 AI 组件状态响应。
 */
const status = ref<AiComponentStatusResponse>()
/**
 * loading 控制状态页刷新按钮和表格加载态。
 */
const loading = ref(false)

/**
 * 加载 AI 组件状态。
 */
async function loadStatus() {
  loading.value = true
  try {
    status.value = await api.aiStatus()
  } catch (error) {
    ElMessage.error(error instanceof Error ? error.message : 'AI 组件状态读取失败')
  } finally {
    loading.value = false
  }
}

/**
 * 状态值转中文。
 */
function statusLabel(value: AiComponentStatusItem['status']) {
  return {
    UP: '正常',
    DOWN: '异常',
    WARN: '警告',
    DISABLED: '未启用'
  }[value]
}

/**
 * 状态值转 Element Plus 标签类型。
 */
function statusTagType(value: AiComponentStatusItem['status']) {
  return {
    UP: 'success',
    DOWN: 'danger',
    WARN: 'warning',
    DISABLED: 'info'
  }[value] as 'success' | 'danger' | 'warning' | 'info'
}

/**
 * 知识来源类型转业务中文，英文枚举保留给管理员排查索引明细。
 */
function sourceTypeLabel(value: string) {
  return {
    SYSTEM_MODULE: '系统模块',
    BASIC_DICTIONARY: '基础字典',
    SUBJECT: '会计科目',
    VOUCHER: '财务凭证',
    PURCHASE_ORDER: '采购单',
    SHIPMENT: '物流单',
    INVENTORY_LEDGER: '库存流水',
    AR_AP_BILL: '应收应付',
    CASHIER_TRANSACTION: '出纳流水',
    ACCOUNTING_PERIOD: '会计期间',
    ATTACHMENT: '业务附件'
  }[value] || value
}

/**
 * 格式化后端时间字符串。
 */
function formatTime(value?: string) {
  if (!value) {
    return '-'
  }
  const date = new Date(value)
  if (Number.isNaN(date.getTime())) {
    return value
  }
  return date.toLocaleString()
}

onMounted(loadStatus)
</script>

<style scoped>
.page {
  padding: 24px;
}

.page-header {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 16px;
  margin-bottom: 18px;
}

.page-header h1 {
  margin: 0 0 8px;
  font-size: 24px;
  line-height: 1.2;
}

.page-header p {
  margin: 0;
  color: var(--el-text-color-secondary);
}

.summary-grid {
  display: grid;
  grid-template-columns: repeat(5, minmax(0, 1fr));
  gap: 12px;
  margin-bottom: 16px;
}

.summary-item {
  display: flex;
  min-height: 94px;
  flex-direction: column;
  justify-content: center;
  gap: 8px;
  border: 1px solid var(--el-border-color);
  border-radius: 6px;
  padding: 16px;
  background: var(--el-bg-color);
}

.summary-item span {
  color: var(--el-text-color-secondary);
}

.summary-item strong {
  font-size: 22px;
  line-height: 1.1;
}

.summary-item small {
  color: var(--el-text-color-secondary);
}

.status-alert {
  margin-bottom: 16px;
}

.status-table {
  width: 100%;
}

.source-type {
  display: flex;
  flex-direction: column;
  gap: 2px;
  line-height: 1.35;
}

.source-type small {
  color: var(--el-text-color-secondary);
}

.section-title {
  margin: 20px 0 12px;
  font-size: 18px;
  line-height: 1.3;
}

@media (max-width: 1100px) {
  .summary-grid {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }
}

@media (max-width: 640px) {
  .page {
    padding: 16px;
  }

  .page-header {
    flex-direction: column;
  }

  .summary-grid {
    grid-template-columns: 1fr;
  }
}
</style>
