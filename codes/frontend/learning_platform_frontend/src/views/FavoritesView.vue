<script setup lang="ts">
import { ElMessage } from 'element-plus'
import { onMounted, ref } from 'vue'

import { listCategories, listFavorites } from '@/api/content'
import ContentCard from '@/components/ContentCard.vue'
import SectionPageHeader from '@/components/SectionPageHeader.vue'
import type { ContentCategory, ContentSummary } from '@/types/content'

const loading = ref(true)
const items = ref<ContentSummary[]>([])
const categories = ref<ContentCategory[]>([])

function categoryName(id: number): string {
  return categories.value.find((item) => item.id === id)?.name ?? '学习资料'
}

onMounted(async () => {
  try {
    ;[items.value, categories.value] = await Promise.all([listFavorites(), listCategories()])
  } catch (error) {
    ElMessage.error(error instanceof Error ? error.message : '加载收藏失败')
  } finally {
    loading.value = false
  }
})
</script>

<template>
  <section class="favorites-page"><div class="page-container">
    <SectionPageHeader eyebrow="SAVED CONTENT" title="我的收藏" description="集中查看你标记过的优质学习资料">
      <RouterLink to="/contents"><el-button type="primary">浏览资料库</el-button></RouterLink>
    </SectionPageHeader>
    <div v-loading="loading" class="favorites-grid"><ContentCard v-for="item in items" :key="item.id" :content="item" :category-name="categoryName(item.categoryId)" /></div>
    <el-empty v-if="!loading && items.length === 0" description="还没有收藏内容" />
  </div></section>
</template>

<style scoped>
.favorites-page { min-height: calc(100vh - 145px); padding: 54px 0 78px; background: linear-gradient(180deg, #f4f7ff, #f8f9fc 320px); }
.favorites-grid { display: grid; gap: 20px; grid-template-columns: repeat(3, minmax(0, 1fr)); }
@media (max-width: 900px) { .favorites-grid { grid-template-columns: repeat(2, 1fr); } }
@media (max-width: 580px) { .favorites-grid { grid-template-columns: 1fr; } }
</style>
