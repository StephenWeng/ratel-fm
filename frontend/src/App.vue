<template>
  <el-dialog
    v-model="auth.expiredDialogVisible"
    width="420px"
    class="session-expired-dialog"
    :close-on-click-modal="false"
    :show-close="false"
    align-center
  >
    <template #header>
      <div class="expired-header">
        <span class="expired-icon">
          <el-icon><Lock /></el-icon>
        </span>
        <div class="expired-title">
          <strong>登录已过期</strong>
          <span>为保护账户安全，请重新登录后继续操作</span>
        </div>
      </div>
    </template>
    <div class="expired-body">
      <p class="expired-message">{{ auth.expiredMessage }}</p>
      <div class="countdown-box">
        <span class="countdown-label">自动跳转倒计时</span>
        <strong>{{ auth.expiredCountdown }}</strong>
        <span class="countdown-unit">秒</span>
      </div>
    </div>
    <template #footer>
      <el-button class="login-button" type="primary" :icon="Right" @click="goLogin">立即前往登录</el-button>
    </template>
  </el-dialog>
  <router-view />
</template>

<script setup lang="ts">
import { Lock, Right } from '@element-plus/icons-vue'
import { useRouter } from 'vue-router'
import { useAuthStore } from '@/stores/auth'

/**
 * 常量 router：保存当前模块的页面状态、配置项、接口实例或计算结果。
 */
const router = useRouter()
/**
 * 常量 auth：保存当前模块的页面状态、配置项、接口实例或计算结果。
 */
const auth = useAuthStore()

/**
 * 执行 goLogin 方法。
 * 
 * 实现步骤：
 * 1. 读取当前页面状态或调用参数；
 * 2. 完成对应的校验、接口调用或数据转换；
 * 3. 更新页面状态或返回处理结果。
 */
function goLogin() {
  auth.stopExpiredCountdown()
  router.replace(auth.loginPath())
}
</script>

<style scoped>
:deep(.session-expired-dialog) {
  border-radius: 8px;
  overflow: hidden;
}

:deep(.session-expired-dialog .el-dialog__header) {
  padding: 22px 24px 14px;
  margin-right: 0;
}

:deep(.session-expired-dialog .el-dialog__body) {
  padding: 0 24px 8px;
}

:deep(.session-expired-dialog .el-dialog__footer) {
  padding: 12px 24px 24px;
}

.expired-header {
  display: flex;
  align-items: center;
  gap: 12px;
}

.expired-icon {
  display: grid;
  width: 44px;
  height: 44px;
  flex: 0 0 auto;
  place-items: center;
  border: 1px solid var(--accent-border-color);
  border-radius: var(--radius-md);
  background: var(--primary-light-color);
  color: var(--primary-color);
  font-size: 22px;
}

.expired-title {
  display: grid;
  gap: 5px;
  min-width: 0;
}

.expired-title strong {
  color: var(--heading-color);
  font-size: 18px;
  line-height: 1.25;
}

.expired-title span {
  color: var(--muted-text-color);
  font-size: 13px;
  line-height: 1.45;
}

.expired-body {
  display: grid;
  gap: 14px;
}

.expired-message {
  margin: 0;
  padding: 12px 14px;
  border: 1px solid var(--border-color);
  border-radius: var(--radius-md);
  background: var(--subtle-surface-color);
  color: var(--text-color);
  line-height: 1.6;
}

.countdown-box {
  display: flex;
  align-items: baseline;
  justify-content: center;
  gap: 6px;
  padding: 12px;
  border: 1px solid var(--warning-border-color);
  border-radius: var(--radius-md);
  background: var(--warning-surface-color);
  color: var(--warning-color);
}

.countdown-label {
  color: var(--muted-text-color);
  font-size: 13px;
}

.countdown-box strong {
  color: var(--warning-color);
  font-size: 26px;
  line-height: 1;
}

.countdown-unit {
  color: var(--warning-color);
  font-size: 14px;
  font-weight: 700;
}

.login-button {
  min-width: 132px;
  height: 36px;
  font-weight: 700;
}
</style>
