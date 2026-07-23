<template>
  <section class="business-form-preview">
    <div class="business-form-preview__header">
      <strong>业务表单内容</strong>
      <span>{{ form?.title || '暂无业务表单内容' }}</span>
    </div>

    <el-empty v-if="!hasContent" description="暂无业务表单内容" :image-size="64" />

    <template v-else>
      <div v-for="section in form?.sections || []" :key="section.title" class="business-form-preview__section">
        <h3>{{ section.title }}</h3>
        <dl class="business-form-preview__fields">
          <div v-for="field in section.fields" :key="`${section.title}-${field.label}`" class="business-form-preview__field">
            <dt>{{ field.label }}</dt>
            <dd>{{ field.value || '-' }}</dd>
          </div>
        </dl>
      </div>

      <div v-for="table in form?.tables || []" :key="table.title" class="business-form-preview__table">
        <div class="business-form-preview__table-title">{{ table.title }}</div>
        <el-table :data="table.rows || []" border size="small" max-height="260">
          <el-table-column
            v-for="column in table.columns"
            :key="column.key"
            :prop="column.key"
            :label="column.label"
            min-width="120"
            show-overflow-tooltip
          />
        </el-table>
      </div>
    </template>
  </section>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import type { WorkflowBusinessFormView } from '@/types/api'

/**
 * 流程业务表单预览组件。
 *
 * 实现步骤：
 * 1. 接收后端按通用结构返回的字段分组和明细表格；
 * 2. 字段分组按 label/value 双列展示，兼容不同业务模块的字段数量差异；
 * 3. 明细表格按 columns/rows 动态渲染，后续采购、物流、付款等流程可复用。
 */
const props = defineProps<{
  form?: WorkflowBusinessFormView | null
}>()

/**
 * 判断业务表单是否包含可展示内容。
 *
 * 实现步骤：字段分组或明细表格任一存在即认为有内容，否则展示空态。
 */
const hasContent = computed(() => {
  return Boolean((props.form?.sections || []).length || (props.form?.tables || []).length)
})
</script>

<style scoped>
.business-form-preview {
  display: grid;
  gap: 12px;
  padding: 12px;
  border: 1px solid var(--border-color);
  border-radius: 6px;
  background: var(--subtle-surface-color);
  color: var(--text-color);
}

.business-form-preview__header {
  display: flex;
  align-items: baseline;
  justify-content: space-between;
  gap: 12px;
  padding-bottom: 8px;
  border-bottom: 1px solid var(--border-color);
}

.business-form-preview__header strong,
.business-form-preview__section h3,
.business-form-preview__table-title {
  color: var(--heading-color);
}

.business-form-preview__header span {
  color: var(--muted-text-color);
  font-size: 13px;
}

.business-form-preview__section {
  display: grid;
  gap: 8px;
}

.business-form-preview__section h3 {
  margin: 0;
  font-size: 14px;
}

.business-form-preview__fields {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(210px, 1fr));
  gap: 8px 14px;
  margin: 0;
}

.business-form-preview__field {
  display: grid;
  grid-template-columns: 88px minmax(0, 1fr);
  gap: 8px;
  min-width: 0;
}

.business-form-preview__field dt {
  color: var(--muted-text-color);
  font-weight: 600;
}

.business-form-preview__field dd {
  min-width: 0;
  margin: 0;
  color: var(--text-color);
  word-break: break-word;
}

.business-form-preview__table {
  display: grid;
  gap: 8px;
  min-width: 0;
}

.business-form-preview__table-title {
  font-weight: 700;
}

@media (max-width: 720px) {
  .business-form-preview__header {
    align-items: flex-start;
    flex-direction: column;
  }

  .business-form-preview__field {
    grid-template-columns: 76px minmax(0, 1fr);
  }
}
</style>
