<script setup lang="ts">
import { ElMessage } from 'element-plus'
import { onMounted, ref } from 'vue'

import { getContent, listLearningProgress } from '@/api/content'
import SectionPageHeader from '@/components/SectionPageHeader.vue'
import type { ContentDetail, LearningProgress } from '@/types/content'

interface LearningItem { progress: LearningProgress; content: ContentDetail }
const loading = ref(true)
const items = ref<LearningItem[]>([])

onMounted(async () => {
  try {
    const progresses = await listLearningProgress()
    const results = await Promise.all(
      progresses.map(async (progress) => {
        try {
          return { progress, content: await getContent(progress.contentId) }
        } catch {
          return null
        }
      }),
    )
    items.value = results.filter((item): item is LearningItem => item !== null)
  } catch (error) {
    ElMessage.error(error instanceof Error ? error.message : '加载学习记录失败')
  } finally {
    loading.value = false
  }
})
</script>

<template>
  <section class="record-page"><div class="page-container">
    <SectionPageHeader eyebrow="MY LEARNING" title="我的学习" description="继续上次的进度，清晰查看每份资料的学习状态">
      <RouterLink to="/contents"><el-button type="primary">发现更多资料</el-button></RouterLink>
    </SectionPageHeader>
    <div v-loading="loading" class="record-list">
      <RouterLink v-for="item in items" :key="item.content.id" :to="`/contents/${item.content.id}/learn`" class="record-card">
        <div class="record-main"><el-tag effect="light">{{ item.content.contentType }}</el-tag><div><h3>{{ item.content.title }}</h3><p>最后学习：{{ new Date(item.progress.lastLearnedAt).toLocaleString() }}</p></div></div>
        <div class="record-progress"><strong>{{ Number(item.progress.progressPercent) }}%</strong><el-progress :percentage="Number(item.progress.progressPercent)" :show-text="false" /></div>
      </RouterLink>
    </div>
    <el-empty v-if="!loading && items.length === 0" description="还没有学习记录" />
  </div></section>
</template>

<style scoped>
.record-page { min-height: calc(100vh - 145px); padding: 54px 0 78px; background: linear-gradient(180deg, #f4f7ff, #f8f9fc 320px); }
.record-list { display: grid; gap: 14px; }.record-card { display: grid; align-items: center; gap: 26px; border: 1px solid var(--lp-border); border-radius: 16px; background: #fff; box-shadow: 0 8px 24px rgb(16 24 40 / 5%); grid-template-columns: 1fr 240px; padding: 20px; transition: .18s ease; }.record-card:hover { border-color: #bfd3ff; transform: translateY(-2px); }
.record-main { display: flex; align-items: center; gap: 16px; }.record-main h3 { margin: 0 0 7px; }.record-main p { margin: 0; color: var(--lp-text-secondary); font-size: 12px; }.record-progress strong { display: block; margin-bottom: 8px; color: var(--lp-primary); text-align: right; }
@media (max-width: 640px) { .record-card { grid-template-columns: 1fr; }.record-progress strong { text-align: left; } }
</style>
