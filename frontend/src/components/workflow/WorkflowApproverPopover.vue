<template>
  <el-popover trigger="hover" placement="top" width="280">
    <template #reference>
      <span class="approver-trigger">{{ approverInfo || '-' }}</span>
    </template>
    <div class="approver-popover">
      <div class="approver-popover__title">{{ approverInfo || '暂无下个节点审批人' }}</div>
      <div v-if="users.length" class="approver-user-list">
        <div v-for="user in users" :key="`${user.name}-${user.phone || ''}`" class="approver-user">
          <span>{{ user.name || '-' }}</span>
          <small>{{ user.phone || '-' }}</small>
        </div>
      </div>
      <el-empty v-else :image-size="48" description="暂无匹配人员" />
    </div>
  </el-popover>
</template>

<script setup lang="ts">
import type { WorkflowApproverUserView } from '@/types/api'

/**
 * 下个节点审批人悬浮明细组件。
 *
 * 实现步骤：
 * 1. 列表中展示后端已经标识好的审批组合文本；
 * 2. 鼠标悬浮时展示该组合下实际匹配的启用人员；
 * 3. 没有匹配人员时展示空状态，方便管理员发现流程配置问题。
 */
defineProps<{
  /** 审批组合展示文本，例如“人员：张三”“部门：管理部”“部门(岗位)：管理部(经理)”。 */
  approverInfo?: string
  /** 当前审批组合命中的人员列表，只展示姓名和联系方式。 */
  users: WorkflowApproverUserView[]
}>()
</script>

<style scoped>
.approver-trigger {
  color: var(--brand-color);
  cursor: help;
  white-space: normal;
}

.approver-popover {
  display: grid;
  gap: 10px;
}

.approver-popover__title {
  color: var(--heading-color);
  font-weight: 700;
}

.approver-user-list {
  display: grid;
  gap: 8px;
  max-height: 220px;
  overflow: auto;
}

.approver-user {
  display: flex;
  justify-content: space-between;
  gap: 12px;
  padding: 8px 10px;
  border: 1px solid var(--border-color);
  border-radius: 6px;
  background: var(--subtle-surface-color);
}

.approver-user span {
  color: var(--text-color);
  font-weight: 600;
}

.approver-user small {
  color: var(--secondary-text-color);
  white-space: nowrap;
}
</style>
