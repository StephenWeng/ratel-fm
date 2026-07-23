<template>
  <main class="route-fallback" aria-hidden="true"></main>
</template>

<script setup lang="ts">
/**
 * 未登记前端路由兜底页。
 *
 * 开发组织：ratel；开发人员：WenZhang；联系方式：18782945613。
 *
 * 说明：该页面不展示业务内容，只用于承接直接输入的未知页面地址。
 * 路由守卫会先完成 JWT 校验和菜单权限判断，再把有登录态的用户带回首页。
 *
 * @author ratel
 */
</script>

<style scoped>
.route-fallback {
  min-height: 100vh;
  background: #f5f7fb;
}
</style>
