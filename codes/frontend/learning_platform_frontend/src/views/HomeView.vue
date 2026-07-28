<script setup lang="ts">
import { ArrowRight, Document, MagicStick, Monitor } from '@element-plus/icons-vue'
import { onBeforeUnmount, onMounted, ref } from 'vue'
import { useRoute } from 'vue-router'

import { listContents } from '@/api/content'
import { getHealthStatus } from '@/api/health'
import ContentCard from '@/components/ContentCard.vue'
import { useAuthStore } from '@/stores/auth'
import type { ContentSummary } from '@/types/content'

type BackendState = 'checking' | 'online' | 'offline'
const HEALTH_POLL_INTERVAL_MS = 10_000
const backendState = ref<BackendState>('checking')
const authStore = useAuthStore()
const route = useRoute()
const featuredLoading = ref(false)
const featuredContents = ref<ContentSummary[]>([])
const featuredError = ref('')

let healthPollTimer: ReturnType<typeof setInterval> | undefined
let healthCheckRunning = false

async function checkBackend(showChecking = true): Promise<void> {
  if (healthCheckRunning) return
  healthCheckRunning = true
  if (showChecking) backendState.value = 'checking'
  try {
    const health = await getHealthStatus()
    backendState.value = health.status === 'UP' ? 'online' : 'offline'
  } catch {
    backendState.value = 'offline'
  } finally {
    healthCheckRunning = false
  }
}

async function loadFeatured(): Promise<void> {
  if (!authStore.isAuthenticated) return
  featuredLoading.value = true
  featuredError.value = ''
  try {
    const result = await listContents({ pageNumber: 1, pageSize: 3 })
    featuredContents.value = result.items
  } catch (error) {
    featuredContents.value = []
    featuredError.value = error instanceof Error ? error.message : '精选资料加载失败'
  } finally {
    featuredLoading.value = false
  }
}

onMounted(() => {
  void checkBackend()
  void loadFeatured()
  healthPollTimer = setInterval(() => void checkBackend(false), HEALTH_POLL_INTERVAL_MS)
})

onBeforeUnmount(() => {
  if (healthPollTimer) clearInterval(healthPollTimer)
})
</script>

<template>
  <div v-if="route.query.forbidden === '1'" class="page-container access-alert">
    <el-alert
      title="你没有访问该页面的权限"
      description="系统已阻止本次越权访问。如需使用该功能，请联系管理员分配对应角色。"
      type="warning"
      show-icon
      closable
    />
  </div>
  <section class="hero-section">
    <div class="hero-decoration hero-decoration-one" />
    <div class="hero-decoration hero-decoration-two" />
    <div class="page-container hero-content">
      <div class="hero-copy">
        <div class="eyebrow">学习 · 考试 · AI 助手</div>
        <h1 class="page-title">让每一次学习<br />都有清晰的成果</h1>
        <p class="page-description">汇集优质学习资料、在线考试与 AI 知识讲解，帮助学习者建立完整的学习与检验闭环。</p>
        <div class="hero-actions">
          <RouterLink to="/contents"><el-button type="primary" size="large">开始学习<el-icon class="el-icon--right"><ArrowRight /></el-icon></el-button></RouterLink>
          <RouterLink to="/exams"><el-button size="large">进入考试中心</el-button></RouterLink>
        </div>
        <button class="backend-status" type="button" @click="checkBackend()">
          <span :class="['status-dot', backendState]" />
          <span v-if="backendState === 'checking'">正在检查后端服务</span>
          <span v-else-if="backendState === 'online'">后端服务连接正常</span>
          <span v-else>后端暂未连接，点击重试</span>
        </button>
      </div>
      <div class="hero-panel" aria-label="平台能力预览">
        <!-- <div class="panel-heading"><span>平台能力概览</span><el-tag type="success" effect="light" round>核心功能已开放</el-tag></div> -->
        <div class="feature-grid">
          <article><span class="feature-icon blue"><Document /></span><strong>学习资料</strong><small>图文、文档与视频</small></article>
          <article><span class="feature-icon purple"><MagicStick /></span><strong>AI 助手</strong><small>总结与知识讲解</small></article>
          <article><span class="feature-icon green"><Monitor /></span><strong>在线考试</strong><small>答题、评分与分析</small></article>
        </div>
      </div>
    </div>
  </section>
  <section class="featured-section">
    <div class="page-container">
      <div class="featured-heading">
        <div><span>精选资料</span><h2>从一份好资料开始学习</h2></div>
        <RouterLink v-if="authStore.isAuthenticated" to="/contents">
          <el-button text type="primary">查看全部 <el-icon><ArrowRight /></el-icon></el-button>
        </RouterLink>
      </div>
      <div v-if="authStore.isAuthenticated" v-loading="featuredLoading" class="featured-grid">
        <el-alert
          v-if="featuredError && !featuredLoading"
          :title="featuredError"
          type="error"
          show-icon
          :closable="false"
        >
          <template #default>
            <el-button link type="primary" @click="loadFeatured">重新加载</el-button>
          </template>
        </el-alert>
        <ContentCard v-for="item in featuredContents" :key="item.id" :content="item" />
        <el-empty
          v-if="!featuredLoading && !featuredError && featuredContents.length === 0"
          description="暂无已发布资料"
        />
      </div>
      <div v-else class="guest-featured">
        <div>
          <strong>登录后浏览完整资料库</strong>
          <p>按分类、类型和价格筛选资料，并记录学习进度与收藏。</p>
        </div>
        <RouterLink to="/login"><el-button type="primary">登录 / 注册</el-button></RouterLink>
      </div>
    </div>
  </section>
  <section class="capability-section">
    <div class="page-container section-heading">
      <div><span>核心能力</span><h2>一个平台，完成学习全过程</h2></div>
      <p>版本更新说明：<br>v1.3：增加了班级功能与错题复习功能</p>
    </div>
  </section>
</template>

<style scoped>
.access-alert { padding-top: 18px; }
.hero-section { position: relative; overflow: hidden; padding: 78px 0 88px; background: linear-gradient(145deg, #f8fbff 0%, #f4f7ff 54%, #f8f9fc 100%); }
.hero-content { position: relative; z-index: 1; display: grid; align-items: center; gap: 70px; grid-template-columns: minmax(0, 1fr) minmax(420px, 0.88fr); }
.eyebrow { display: inline-flex; border: 1px solid #bfdbfe; border-radius: 999px; margin-bottom: 22px; color: var(--lp-primary); background: #eff6ff; font-size: 13px; font-weight: 700; letter-spacing: 0.08em; padding: 8px 13px; }
.hero-copy .page-title { font-size: clamp(40px, 5vw, 62px); line-height: 1.12; }
.hero-copy .page-description { max-width: 600px; }
.hero-actions { display: flex; gap: 12px; margin-top: 30px; }
.backend-status { display: inline-flex; border: 0; align-items: center; gap: 8px; margin-top: 24px; color: var(--lp-text-secondary); background: transparent; cursor: pointer; font-size: 13px; padding: 0; }
.status-dot { width: 8px; height: 8px; border-radius: 50%; background: #98a2b3; }
.status-dot.online { background: var(--lp-success); box-shadow: 0 0 0 4px rgb(18 183 106 / 12%); }
.status-dot.offline { background: #f04438; }
.status-dot.checking { animation: pulse 1s infinite alternate; }
.hero-panel { border: 1px solid rgb(255 255 255 / 75%); border-radius: 24px; background: rgb(255 255 255 / 88%); box-shadow: 0 32px 80px rgb(29 78 216 / 13%); padding: 28px; backdrop-filter: blur(18px); }
.panel-heading { display: flex; align-items: center; justify-content: space-between; }
.panel-heading { font-size: 16px; font-weight: 700; }
.feature-grid { display: grid; gap: 12px; grid-template-columns: repeat(3, 1fr); }
.feature-grid article { display: flex; min-width: 0; border: 1px solid var(--lp-border); border-radius: 14px; flex-direction: column; padding: 16px; }
.feature-grid strong { margin-top: 14px; font-size: 14px; }
.feature-grid small { overflow: hidden; margin-top: 6px; color: var(--lp-text-secondary); font-size: 11px; text-overflow: ellipsis; white-space: nowrap; }
.feature-icon { display: grid; width: 36px; height: 36px; border-radius: 10px; padding: 9px; place-items: center; }
.feature-icon.blue { color: #2563eb; background: #eff6ff; }
.feature-icon.purple { color: #7c3aed; background: #f5f3ff; }
.feature-icon.green { color: #039855; background: #ecfdf3; }
.hero-decoration { position: absolute; border-radius: 50%; filter: blur(1px); }
.hero-decoration-one { top: -180px; right: -160px; width: 500px; height: 500px; background: rgb(96 165 250 / 13%); }
.hero-decoration-two { bottom: -280px; left: -180px; width: 460px; height: 460px; background: rgb(167 139 250 / 9%); }
.featured-section { padding: 72px 0; background: #f7f9fd; }
.featured-heading { display: flex; align-items: flex-end; justify-content: space-between; margin-bottom: 28px; }
.featured-heading span { color: var(--lp-primary); font-size: 13px; font-weight: 700; }
.featured-heading h2 { margin: 9px 0 0; font-size: 30px; }
.featured-grid { display: grid; min-height: 180px; gap: 20px; grid-template-columns: repeat(3, minmax(0, 1fr)); }
.featured-grid .el-empty { grid-column: 1 / -1; }
.featured-grid > .el-alert { grid-column: 1 / -1; }
.guest-featured { display: flex; border: 1px solid #dbe7fb; border-radius: 18px; align-items: center; justify-content: space-between; gap: 24px; background: linear-gradient(135deg, #fff, #eef5ff); padding: 28px 32px; }
.guest-featured strong { font-size: 18px; }
.guest-featured p { margin: 8px 0 0; color: var(--lp-text-secondary); }
.capability-section { padding: 70px 0; background: var(--lp-surface); }
.section-heading { display: flex; align-items: flex-end; justify-content: space-between; gap: 40px; }
.section-heading span { color: var(--lp-primary); font-size: 13px; font-weight: 700; }
.section-heading h2 { margin: 10px 0 0; font-size: 30px; }
.section-heading p { max-width: 430px; margin: 0; color: var(--lp-text-secondary); line-height: 1.7; }
@keyframes pulse { from { opacity: 0.35; } to { opacity: 1; } }
@media (max-width: 960px) { .hero-content { gap: 46px; grid-template-columns: 1fr; } .hero-panel { max-width: 680px; } .featured-grid { grid-template-columns: repeat(2, minmax(0, 1fr)); } }
@media (max-width: 640px) {
  .hero-section { padding: 54px 0 60px; }
  .hero-actions, .section-heading, .guest-featured { align-items: stretch; flex-direction: column; }
  .featured-grid { grid-template-columns: 1fr; }
  .feature-grid { grid-template-columns: 1fr; }
  .feature-grid article { display: grid; align-items: center; gap: 0 12px; grid-template-columns: auto 1fr; }
  .feature-grid strong { margin-top: 0; }
  .feature-grid small { grid-column: 2; }
}
</style>
