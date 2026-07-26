<script setup lang="ts">
import { Delete, Edit, Plus, Promotion } from '@element-plus/icons-vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { onMounted, reactive, ref } from 'vue'

import { deleteContent, listPublisherContents, submitContent } from '@/api/content'
import ContentStatusTag from '@/components/ContentStatusTag.vue'
import PublisherExamNav from '@/components/PublisherExamNav.vue'
import SectionPageHeader from '@/components/SectionPageHeader.vue'
import type { ContentPage, ContentStatus } from '@/types/content'

const loading = ref(false)
const page = ref<ContentPage>({ items: [], total: 0, pageNumber: 1, pageSize: 12, totalPages: 0 })
const filters = reactive<{ keyword: string; status?: ContentStatus; pageNumber: number }>({
  keyword: '',
  pageNumber: 1,
})

async function load(): Promise<void> {
  loading.value = true
  try {
    page.value = await listPublisherContents({ ...filters, pageSize: 12 })
  } catch (error) {
    ElMessage.error(error instanceof Error ? error.message : '加载资料失败')
  } finally {
    loading.value = false
  }
}

async function submit(id: number): Promise<void> {
  try {
    await ElMessageBox.confirm('提交后将不能继续编辑，确定提交审核吗？', '提交审核')
    await submitContent(id)
    ElMessage.success('已提交审核')
    await load()
  } catch (error) {
    if (error !== 'cancel') ElMessage.error(error instanceof Error ? error.message : '提交失败')
  }
}

async function remove(id: number): Promise<void> {
  try {
    await ElMessageBox.confirm('删除后无法恢复，确定继续吗？', '删除资料', { type: 'warning' })
    await deleteContent(id)
    ElMessage.success('资料已删除')
    await load()
  } catch (error) {
    if (error !== 'cancel') ElMessage.error(error instanceof Error ? error.message : '删除失败')
  }
}

onMounted(load)
</script>

<template>
  <section class="publisher-page"><div class="page-container">
    <PublisherExamNav />
    <SectionPageHeader eyebrow="PUBLISHER STUDIO" title="发布者工作台" description="创建、维护并跟踪学习资料的审核与发布状态">
      <RouterLink to="/publisher/contents/new"><el-button type="primary" :icon="Plus">新建资料</el-button></RouterLink>
    </SectionPageHeader>
    <div class="toolbar">
      <el-input v-model="filters.keyword" clearable placeholder="搜索我的资料" @keyup.enter="load" />
      <el-select v-model="filters.status" clearable placeholder="全部状态">
        <el-option label="草稿" value="DRAFT" /><el-option label="待审核" value="PENDING_REVIEW" />
        <el-option label="已发布" value="PUBLISHED" /><el-option label="已驳回" value="REJECTED" /><el-option label="已下架" value="OFFLINE" />
      </el-select>
      <el-button type="primary" @click="load">查询</el-button>
    </div>
    <div class="table-card">
      <el-table v-loading="loading" :data="page.items">
        <el-table-column label="资料" min-width="260">
          <template #default="{ row }"><strong>{{ row.title }}</strong><p class="summary">{{ row.summary || '暂无简介' }}</p></template>
        </el-table-column>
        <el-table-column label="发放方式" width="120">
          <template #default="{ row }">
            <el-tag v-if="row.distributionMode === 'CLASS'" type="warning">班级发放</el-tag>
            <span v-else>{{ row.isFree ? '公开 · 免费' : `公开 · ¥${Number(row.price).toFixed(2)}` }}</span>
          </template>
        </el-table-column>
        <el-table-column label="状态" width="120"><template #default="{ row }"><ContentStatusTag :status="row.status" /></template></el-table-column>
        <el-table-column label="数据" width="150"><template #default="{ row }">{{ row.viewCount }} 浏览 · {{ row.likeCount }} 赞</template></el-table-column>
        <el-table-column label="操作" width="230" fixed="right">
          <template #default="{ row }">
            <RouterLink :to="`/publisher/contents/${row.id}/edit`"><el-button text type="primary" :icon="Edit">编辑</el-button></RouterLink>
            <el-button v-if="['DRAFT','REJECTED'].includes(row.status)" text type="success" :icon="Promotion" @click="submit(row.id)">提交</el-button>
            <el-button v-if="['DRAFT','REJECTED'].includes(row.status)" text type="danger" :icon="Delete" @click="remove(row.id)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>
      <el-empty v-if="!loading && page.items.length === 0" description="还没有资料，创建第一份吧" />
    </div>
    <el-pagination v-if="page.total > page.pageSize" v-model:current-page="filters.pageNumber" class="pagination" layout="prev, pager, next" :page-size="page.pageSize" :total="page.total" @current-change="load" />
  </div></section>
</template>

<style scoped>
.publisher-page { min-height: calc(100vh - 145px); padding: 54px 0 78px; background: linear-gradient(180deg, #f4f7ff, #f8f9fc 320px); }
.toolbar { display: grid; gap: 12px; margin-bottom: 18px; grid-template-columns: 1fr 180px auto; }.table-card { overflow: hidden; border: 1px solid var(--lp-border); border-radius: 18px; background: #fff; box-shadow: var(--lp-shadow); padding: 8px 16px 16px; }.summary { overflow: hidden; max-width: 420px; margin: 6px 0 0; color: #98a2b3; font-size: 12px; text-overflow: ellipsis; white-space: nowrap; }.pagination { justify-content: center; margin-top: 26px; }
@media (max-width: 620px) { .toolbar { grid-template-columns: 1fr; } }
</style>
