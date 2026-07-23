<template>
  <div class="business-agent-panel">
    <div class="agent-toolbar">
      <el-input v-model="questionText" type="textarea" :rows="3" :placeholder="placeholder" />
      <div class="agent-actions">
        <el-select v-model="stageValue" class="stage-select">
          <el-option label="只读分析" value="readOnly" />
          <el-option label="草稿计划" value="draft" />
          <el-option label="受控计划" value="controlled" />
          <el-option label="多步骤计划" value="multiStep" />
        </el-select>
        <el-button type="primary" :loading="loading" @click="runAgent">Agent 分析</el-button>
      </div>
    </div>

    <el-alert
      v-if="agentEnabled === false"
      class="agent-alert"
      type="info"
      :closable="false"
      title="业务 Agent 未启用，当前页面不会调用 Agent。"
    />

    <div v-if="response" class="agent-result">
      <el-alert class="agent-alert" type="success" :closable="false" :title="response.summary" />
      <section v-if="response.capabilities.length" class="agent-section">
        <h3>能力分析</h3>
        <el-collapse>
          <el-collapse-item v-for="item in response.capabilities" :key="item.agentType" :title="`${item.agentName}：${item.summary}`">
            <div class="agent-list">
              <el-tag v-for="risk in item.risks" :key="risk" type="danger" effect="light">{{ risk }}</el-tag>
              <el-tag v-for="finding in item.findings" :key="finding" effect="light">{{ finding }}</el-tag>
            </div>
            <ul v-if="item.suggestions.length" class="agent-text-list">
              <li v-for="suggestion in item.suggestions" :key="suggestion">{{ suggestion }}</li>
            </ul>
            <p v-for="draft in item.drafts" :key="draft" class="agent-draft">{{ draft }}</p>
            <el-table v-if="item.evidences.length" :data="item.evidences" size="small" border>
              <el-table-column prop="type" label="类型" width="150" />
              <el-table-column prop="no" label="单号" min-width="150" show-overflow-tooltip />
              <el-table-column prop="title" label="标题" min-width="180" show-overflow-tooltip />
              <el-table-column prop="status" label="状态" width="120" />
              <el-table-column prop="amount" label="金额/得分" width="130" />
              <el-table-column prop="date" label="日期" width="120" />
            </el-table>
          </el-collapse-item>
        </el-collapse>
      </section>

      <section v-if="response.modules.length" class="agent-section">
        <h3>模块分析</h3>
        <el-table :data="response.modules" size="small" border>
          <el-table-column prop="moduleName" label="模块" width="120" />
          <el-table-column prop="summary" label="摘要" min-width="260" show-overflow-tooltip />
          <el-table-column label="风险" min-width="220">
            <template #default="{ row }">{{ row.risks.join('；') || '-' }}</template>
          </el-table-column>
        </el-table>
      </section>

      <section v-if="response.actions.length" class="agent-section">
        <h3>动作计划</h3>
        <el-table :data="response.actions" size="small" border>
          <el-table-column prop="step" label="步骤" width="70" />
          <el-table-column prop="title" label="标题" min-width="180" />
          <el-table-column prop="description" label="说明" min-width="260" show-overflow-tooltip />
          <el-table-column label="可执行" width="90">
            <template #default="{ row }">
              <el-tag :type="row.executable ? 'success' : 'info'">{{ row.executable ? '是' : '否' }}</el-tag>
            </template>
          </el-table-column>
        </el-table>
      </section>
    </div>
  </div>
</template>

<script setup lang="ts">
import { onMounted, ref, watch } from 'vue'
import { ElMessage } from 'element-plus'
import { api, type BusinessAgentRequest, type BusinessAgentResponse } from '@/api/fm'

const props = withDefaults(defineProps<{
  question?: string
  modules?: string[]
  agentTypes?: string[]
  placeholder?: string
}>(), {
  question: '',
  modules: () => [],
  agentTypes: () => [],
  placeholder: '请输入要分析的业务问题'
})

const questionText = ref(props.question)
const stageValue = ref('readOnly')
const loading = ref(false)
const agentEnabled = ref<boolean>()
const response = ref<BusinessAgentResponse>()

watch(() => props.question, (value) => {
  questionText.value = value || ''
})

onMounted(loadAgentEnabled)

async function loadAgentEnabled() {
  agentEnabled.value = await api.businessAgentEnabled().catch(() => false)
}

async function runAgent(payload?: Partial<BusinessAgentRequest>) {
  if (agentEnabled.value === false) {
    ElMessage.info('业务 Agent 未启用')
    return
  }
  const question = (payload?.question || questionText.value || props.question).trim()
  if (!question) {
    ElMessage.warning('请输入 Agent 分析问题')
    return
  }
  loading.value = true
  try {
    response.value = await api.runBusinessAgent({
      question,
      stage: payload?.stage || stageValue.value,
      modules: payload?.modules || props.modules,
      agentTypes: payload?.agentTypes || props.agentTypes,
      limit: payload?.limit || 5
    })
  } finally {
    loading.value = false
  }
}

defineExpose({ runAgent, loadAgentEnabled })
</script>

<style scoped>
.business-agent-panel {
  display: grid;
  gap: 14px;
}

.agent-toolbar {
  display: grid;
  gap: 10px;
}

.agent-actions {
  display: flex;
  justify-content: flex-end;
  gap: 8px;
}

.stage-select {
  width: 132px;
}

.agent-alert,
.agent-section {
  margin-top: 2px;
}

.agent-section h3 {
  margin: 0 0 10px;
  font-size: 16px;
}

.agent-list {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  margin-bottom: 10px;
}

.agent-text-list {
  margin: 0 0 10px;
  padding-left: 18px;
  color: #374151;
  line-height: 1.7;
}

.agent-draft {
  margin: 8px 0;
  color: #374151;
  line-height: 1.7;
}
</style>
