<script setup lang="ts">
import { Back, House } from '@element-plus/icons-vue'
import { useRouter } from 'vue-router'

const router = useRouter()
</script>

<template>
  <main class="not-found">
    <div class="not-found-card">
      <span class="code">404</span>
      <h1>页面走丢了</h1>
      <p>你访问的页面不存在，可能已被移动或删除。</p>
      <div class="actions">
        <el-button :icon="Back" @click="router.back()">返回上一页</el-button>
        <el-button type="primary" :icon="House" @click="router.push('/')">返回首页</el-button>
      </div>
    </div>
  </main>
</template>

<style scoped>
.not-found { display: grid; min-height: 100vh; background: radial-gradient(circle at 20% 20%, rgb(96 165 250 / 16%), transparent 35%), var(--lp-background); padding: 24px; place-items: center; }
.not-found-card { width: min(520px, 100%); border: 1px solid var(--lp-border); border-radius: 24px; background: var(--lp-surface); box-shadow: var(--lp-shadow); padding: 54px 40px; text-align: center; }
.code { color: var(--lp-primary); font-size: 72px; font-weight: 800; letter-spacing: -0.06em; }
h1 { margin: 6px 0 12px; font-size: 28px; }
p { margin: 0; color: var(--lp-text-secondary); line-height: 1.7; }
.actions { display: flex; justify-content: center; gap: 10px; margin-top: 28px; }
</style>

