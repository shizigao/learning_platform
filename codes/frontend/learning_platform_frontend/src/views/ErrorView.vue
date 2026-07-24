<script setup lang="ts">
import { House, Refresh } from '@element-plus/icons-vue'
import { computed } from 'vue'
import { useRoute, useRouter } from 'vue-router'

const route = useRoute()
const router = useRouter()
const message = computed(() =>
  typeof route.query.message === 'string'
    ? route.query.message
    : '页面加载时发生异常，请稍后重试。',
)

function reload(): void {
  window.location.reload()
}
</script>

<template>
  <main class="error-page">
    <div class="error-card">
      <span class="code">500</span>
      <h1>页面暂时无法加载</h1>
      <p>{{ message }}</p>
      <p class="hint">如果问题持续出现，请记录当前时间并联系管理员。</p>
      <div class="actions">
        <el-button :icon="Refresh" @click="reload">重新加载</el-button>
        <el-button type="primary" :icon="House" @click="router.push('/')">返回首页</el-button>
      </div>
    </div>
  </main>
</template>

<style scoped>
.error-page { display: grid; min-height: 100vh; background: radial-gradient(circle at 20% 20%, rgb(244 63 94 / 12%), transparent 35%), var(--lp-background); padding: 24px; place-items: center; }
.error-card { width: min(540px, 100%); border: 1px solid var(--lp-border); border-radius: 24px; background: var(--lp-surface); box-shadow: var(--lp-shadow); padding: 54px 40px; text-align: center; }
.code { color: #d92d20; font-size: 72px; font-weight: 800; letter-spacing: -.06em; }
h1 { margin: 6px 0 12px; font-size: 28px; }
p { margin: 0; color: var(--lp-text-secondary); line-height: 1.7; }
.hint { margin-top: 8px; font-size: 13px; }
.actions { display: flex; justify-content: center; gap: 10px; margin-top: 28px; }
</style>
