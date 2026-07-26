<script setup lang="ts">
import { Download, MagicStick } from '@element-plus/icons-vue'
import { ElMessage } from 'element-plus'
import { computed, nextTick, onBeforeUnmount, onMounted, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'

import { getContent, getContentFileUrl, startLearning, updateLearningProgress } from '@/api/content'
import MarkdownRenderer from '@/components/MarkdownRenderer.vue'
import type { ContentDetail, LearningProgress } from '@/types/content'
import { calculateReadingProgress as calculateProgressFromGeometry } from '@/utils/reading-progress'

const route = useRoute()
const router = useRouter()
const contentId = Number(route.params.id)
const loading = ref(true)
const saving = ref(false)
const detail = ref<ContentDetail>()
const progress = ref<LearningProgress>()
const percent = ref(0)
const lastPosition = ref('')
const currentReadingPercent = ref(0)
const readerCard = ref<HTMLElement>()
let readerResizeObserver: ResizeObserver | undefined
let scrollFrame: number | undefined
const resourceFiles = computed(
  () => detail.value?.files.filter((file) => !['COVER', 'INLINE_IMAGE'].includes(file.fileRole)) ?? [],
)

async function load(): Promise<void> {
  loading.value = true
  try {
    detail.value = await getContent(contentId)
    progress.value = await startLearning(contentId)
    percent.value = Number(progress.value.progressPercent)
    lastPosition.value = progress.value.lastPosition ?? ''
  } catch (error) {
    ElMessage.error(error instanceof Error ? error.message : '无法进入学习页面')
    await router.replace(`/contents/${contentId}`)
  } finally {
    loading.value = false
  }
}

async function saveProgress(targetPercent = percent.value, successMessage?: string): Promise<void> {
  saving.value = true
  try {
    progress.value = await updateLearningProgress(contentId, targetPercent, lastPosition.value)
    percent.value = Number(progress.value.progressPercent)
    ElMessage.success(
      successMessage || (percent.value >= 100 ? '恭喜完成学习' : '学习进度已保存'),
    )
  } catch (error) {
    ElMessage.error(error instanceof Error ? error.message : '保存进度失败')
  } finally {
    saving.value = false
  }
}

async function applyCurrentProgress(): Promise<void> {
  percent.value = currentReadingPercent.value
  await saveProgress(currentReadingPercent.value, `已将当前进度 ${currentReadingPercent.value}% 应用于学习进度`)
}

function calculateReadingProgress(): void {
  const element = readerCard.value
  if (!element) return
  const rect = element.getBoundingClientRect()
  currentReadingPercent.value = calculateProgressFromGeometry(
    rect.top,
    rect.height,
    window.innerHeight,
  )
}

function scheduleReadingProgress(): void {
  if (scrollFrame !== undefined) return
  scrollFrame = window.requestAnimationFrame(() => {
    scrollFrame = undefined
    calculateReadingProgress()
  })
}

function startReadingProgressTracking(): void {
  if (!readerCard.value) return
  readerResizeObserver?.disconnect()
  readerResizeObserver = new ResizeObserver(scheduleReadingProgress)
  readerResizeObserver.observe(readerCard.value)
  window.addEventListener('scroll', scheduleReadingProgress, { passive: true })
  window.addEventListener('resize', scheduleReadingProgress)
  calculateReadingProgress()
}

function stopReadingProgressTracking(): void {
  readerResizeObserver?.disconnect()
  window.removeEventListener('scroll', scheduleReadingProgress)
  window.removeEventListener('resize', scheduleReadingProgress)
  if (scrollFrame !== undefined) window.cancelAnimationFrame(scrollFrame)
}

async function openFile(fileId: number, mode: 'preview' | 'download'): Promise<void> {
  try {
    window.open(await getContentFileUrl(contentId, fileId, mode), '_blank', 'noopener')
  } catch (error) {
    ElMessage.error(error instanceof Error ? error.message : '文件访问失败')
  }
}

onMounted(async () => {
  await load()
  await nextTick()
  startReadingProgressTracking()
})
onBeforeUnmount(stopReadingProgressTracking)
</script>

<template>
  <section v-loading="loading" class="learning-page">
    <div v-if="detail" class="page-container">
      <header class="learning-header">
        <div><RouterLink :to="`/contents/${contentId}`">← 返回资料详情</RouterLink><h1>{{ detail.title }}</h1><p>发布者：{{ detail.publisherName || `用户 ${detail.publisherId}` }} · 专注学习，及时记录你的当前位置和完成进度。</p></div>
        <div class="progress-summary"><strong>{{ percent }}%</strong><span>学习进度</span></div>
      </header>
      <div class="learning-layout">
        <main ref="readerCard" class="reader-card">
          <MarkdownRenderer
            v-if="detail.articleBody"
            class="article-body"
            :source="detail.articleBody"
            :content-id="contentId"
          />
          <div v-if="resourceFiles.length" class="file-learning" :class="{ 'with-body': detail.articleBody }">
            <h2>资料文件</h2>
            <div v-for="file in resourceFiles" :key="file.id" class="file-item">
              <div><strong>{{ file.originalName }}</strong><small>{{ file.mimeType }}</small></div>
              <div><el-button @click="openFile(file.id, 'preview')">打开预览</el-button><el-button :icon="Download" @click="openFile(file.id, 'download')">下载</el-button></div>
            </div>
          </div>
          <el-empty
            v-if="!detail.articleBody && resourceFiles.length === 0"
            description="该资料暂时没有可学习的正文或文件"
          />
        </main>
        <aside class="progress-card">
          <h2>学习记录</h2>
          <div class="current-reading">
            <div><span>当前进度</span><strong>{{ currentReadingPercent }}%</strong></div>
            <el-progress :percentage="currentReadingPercent" :show-text="false" />
            <p>根据当前页面在资料总长度中的位置实时计算。</p>
            <el-button
              type="primary"
              plain
              :loading="saving"
              @click="applyCurrentProgress"
            >
              应用当前进度
            </el-button>
          </div>
          <p>学习进度可按 1% 调整，也可以向前或向后修改。</p>
          <el-slider v-model="percent" :step="1" show-input />
          <el-input v-model="lastPosition" maxlength="255" placeholder="例如：第 6 页、12:30 或第三章" />
          <el-button type="primary" :loading="saving" @click="saveProgress()">保存学习进度</el-button>
          <RouterLink :to="{ path: '/ai-assistant', query: { contentId } }">
            <el-button :icon="MagicStick">打开 AI 学习助手</el-button>
          </RouterLink>
          <small v-if="progress">最后学习：{{ new Date(progress.lastLearnedAt).toLocaleString() }}</small>
        </aside>
      </div>
    </div>
  </section>
</template>

<style scoped>
.learning-page { min-height: calc(100vh - 145px); padding: 38px 0 70px; background: #f4f6fa; }
.learning-header { display: flex; align-items: flex-end; justify-content: space-between; gap: 24px; margin-bottom: 22px; }.learning-header a { color: var(--lp-primary); font-size: 13px; }.learning-header h1 { margin: 12px 0 6px; font-size: 32px; }.learning-header p { margin: 0; color: var(--lp-text-secondary); }
.progress-summary { display: flex; align-items: flex-end; flex-direction: column; }.progress-summary strong { color: var(--lp-primary); font-size: 34px; }.progress-summary span { color: #98a2b3; font-size: 12px; }
.learning-layout { display: grid; align-items: start; gap: 22px; grid-template-columns: minmax(0, 1fr) 310px; }.reader-card, .progress-card { border: 1px solid var(--lp-border); border-radius: 18px; background: #fff; box-shadow: var(--lp-shadow); }
.reader-card { min-height: 560px; padding: 38px; }.article-body { font-size: 16px; }
.progress-card { position: sticky; top: 94px; padding: 24px; }.progress-card h2 { margin-top: 0; }.progress-card p { color: var(--lp-text-secondary); font-size: 13px; line-height: 1.7; }.progress-card .el-input, .progress-card .el-button { width: 100%; margin-top: 18px; }.progress-card a { display: block; }.progress-card small { display: block; margin-top: 18px; color: #98a2b3; }
.current-reading { border: 1px solid #cfe0ff; border-radius: 12px; margin-bottom: 18px; background: #f5f8ff; padding: 14px; }.current-reading > div { display: flex; align-items: center; justify-content: space-between; gap: 12px; margin-bottom: 10px; }.current-reading span { color: #475467; font-size: 13px; font-weight: 650; }.current-reading strong { color: var(--lp-primary); font-size: 24px; }.current-reading p { margin: 9px 0 0; font-size: 11px; }.current-reading .el-button { margin-top: 12px; }
.file-item { display: flex; align-items: center; justify-content: space-between; gap: 12px; border-bottom: 1px solid var(--lp-border); padding: 16px 0; }.file-item small { display: block; margin-top: 6px; color: #98a2b3; }
.file-learning.with-body { border-top: 1px solid var(--lp-border); margin-top: 32px; padding-top: 22px; }
@media (max-width: 820px) { .learning-layout { grid-template-columns: 1fr; } .progress-card { position: static; } }
</style>
