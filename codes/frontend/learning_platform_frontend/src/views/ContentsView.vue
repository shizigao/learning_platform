<script setup lang="ts">
import { Search } from '@element-plus/icons-vue'
import { ElMessage } from 'element-plus'
import { onMounted, reactive, ref } from 'vue'

import { listContents } from '@/api/content'
import CategoryPickerDialog from '@/components/CategoryPickerDialog.vue'
import ContentCard from '@/components/ContentCard.vue'
import SectionPageHeader from '@/components/SectionPageHeader.vue'
import type { ContentPage } from '@/types/content'

const loading = ref(false)
const page = ref<ContentPage>({ items: [], total: 0, pageNumber: 1, pageSize: 12, totalPages: 0 })
const filters = reactive<{
  keyword: string
  categoryId?: number
  free?: boolean
  pageNumber: number
}>({ keyword: '', pageNumber: 1 })

async function load(): Promise<void> {
  loading.value = true
  try {
    page.value = await listContents({ ...filters, pageSize: 12 })
  } catch (error) {
    ElMessage.error(error instanceof Error ? error.message : '加载学习资料失败')
  } finally {
    loading.value = false
  }
}

function search(): void {
  filters.pageNumber = 1
  void load()
}

function reset(): void {
  filters.keyword = ''
  filters.categoryId = undefined
  filters.free = undefined
  search()
}

onMounted(load)
</script>

<template>
  <section class="content-page">
    <div class="page-container">
      <SectionPageHeader
        eyebrow="LEARNING LIBRARY"
        title="学习资料"
        description="按分类与关键词快速找到适合你的下一份学习内容"
      >
        <RouterLink to="/my-learning"><el-button>我的学习</el-button></RouterLink>
        <RouterLink to="/favorites"><el-button>我的收藏</el-button></RouterLink>
      </SectionPageHeader>

      <div class="filter-panel">
        <el-input
          v-model="filters.keyword"
          :prefix-icon="Search"
          clearable
          placeholder="搜索标题或简介"
          @keyup.enter="search"
        />
        <CategoryPickerDialog
          v-model="filters.categoryId"
          allow-clear
          placeholder="全部分类"
        />
        <el-select v-model="filters.free" clearable placeholder="全部价格">
          <el-option label="免费" :value="true" />
          <el-option label="付费" :value="false" />
        </el-select>
        <el-button type="primary" @click="search">筛选</el-button>
        <el-button @click="reset">重置</el-button>
      </div>

      <div v-loading="loading" class="content-grid">
        <ContentCard
          v-for="item in page.items"
          :key="item.id"
          :content="item"
        />
      </div>
      <el-empty v-if="!loading && page.items.length === 0" description="暂时没有符合条件的资料" />
      <el-pagination
        v-if="page.total > page.pageSize"
        v-model:current-page="filters.pageNumber"
        class="pagination"
        layout="prev, pager, next"
        :page-size="page.pageSize"
        :total="page.total"
        @current-change="load"
      />
    </div>
  </section>
</template>

<style scoped>
.content-page { min-height: calc(100vh - 145px); padding: 54px 0 78px; background: linear-gradient(180deg, #f4f7ff, #f8f9fc 320px); }
.filter-panel { display: grid; gap: 12px; border: 1px solid var(--lp-border); border-radius: 16px; margin-bottom: 26px; background: #fff; box-shadow: var(--lp-shadow); grid-template-columns: minmax(220px, 1fr) 180px 160px auto auto; padding: 16px; }
.content-grid { display: grid; min-height: 180px; gap: 20px; grid-template-columns: repeat(3, minmax(0, 1fr)); }
.pagination { justify-content: center; margin-top: 34px; }
@media (max-width: 980px) { .filter-panel { grid-template-columns: repeat(2, 1fr); } .content-grid { grid-template-columns: repeat(2, 1fr); } }
@media (max-width: 620px) { .filter-panel, .content-grid { grid-template-columns: 1fr; } }
</style>
