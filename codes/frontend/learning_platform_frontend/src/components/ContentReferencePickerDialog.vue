<script setup lang="ts">
import { Search } from '@element-plus/icons-vue'
import { ElMessage } from 'element-plus'
import { ref, watch } from 'vue'

import { listContentReferenceCandidates } from '@/api/content'
import type { ContentPage, ContentSummary } from '@/types/content'

const props = withDefaults(defineProps<{
  modelValue: boolean
  excludeContentId?: number
}>(), {
  excludeContentId: undefined,
})

const emit = defineEmits<{
  'update:modelValue': [value: boolean]
  select: [content: ContentSummary]
}>()

const titleKeyword = ref('')
const publisherKeyword = ref('')
const loading = ref(false)
const page = ref<ContentPage>({
  items: [],
  total: 0,
  pageNumber: 1,
  pageSize: 20,
  totalPages: 0,
})

async function load(pageNumber = 1): Promise<void> {
  loading.value = true
  try {
    page.value = await listContentReferenceCandidates(
      titleKeyword.value,
      publisherKeyword.value,
      props.excludeContentId,
      pageNumber,
      page.value.pageSize,
    )
  } catch (error) {
    ElMessage.error(error instanceof Error ? error.message : '加载可引用资料失败')
  } finally {
    loading.value = false
  }
}

function search(): void {
  void load(1)
}

function choose(row: unknown): void {
  emit('select', row as ContentSummary)
  emit('update:modelValue', false)
}

watch(
  () => props.modelValue,
  (visible) => {
    if (!visible) return
    titleKeyword.value = ''
    publisherKeyword.value = ''
    void load(1)
  },
)
</script>

<template>
  <el-dialog
    :model-value="modelValue"
    title="引用其他学习资料"
    width="min(900px, 94vw)"
    destroy-on-close
    @update:model-value="emit('update:modelValue', $event)"
  >
    <div class="reference-search">
      <div class="search-field">
        <label>资料名称</label>
        <el-input
          v-model="titleKeyword"
          :prefix-icon="Search"
          clearable
          placeholder="输入资料名称"
          @keyup.enter="search"
          @clear="search"
        />
      </div>
      <div class="search-field">
        <label>发布者名字</label>
        <el-input
          v-model="publisherKeyword"
          :prefix-icon="Search"
          clearable
          placeholder="输入发布者名字"
          @keyup.enter="search"
          @clear="search"
        />
      </div>
      <el-button type="primary" @click="search">搜索</el-button>
    </div>

    <el-table
      v-loading="loading"
      :data="page.items"
      height="440"
      @row-dblclick="choose"
    >
      <el-table-column label="资料名称" min-width="230">
        <template #default="{ row }">
          <div class="reference-title">
            <strong>{{ row.title }}</strong>
            <small>{{ row.categoryName || '学习资料' }}</small>
          </div>
        </template>
      </el-table-column>
      <el-table-column label="发布者" min-width="150">
        <template #default="{ row }">
          {{ row.publisherName || `用户 ${row.publisherId}` }}
        </template>
      </el-table-column>
      <el-table-column label="简介" min-width="280">
        <template #default="{ row }">
          <span class="reference-summary">{{ row.summary || '暂无简介' }}</span>
        </template>
      </el-table-column>
      <el-table-column label="操作" width="90" align="center">
        <template #default="{ row }">
          <el-button link type="primary" @click="choose(row)">引用</el-button>
        </template>
      </el-table-column>
      <template #empty>
        <el-empty description="没有匹配的已发布学习资料" />
      </template>
    </el-table>

    <el-pagination
      v-if="page.total > page.pageSize"
      class="reference-pagination"
      layout="total, prev, pager, next"
      :current-page="page.pageNumber"
      :page-size="page.pageSize"
      :total="page.total"
      @current-change="load"
    />
    <p class="reference-tip">只显示已发布资料；双击资料行也可以快速插入引用。</p>
  </el-dialog>
</template>

<style scoped>
.reference-search { display: grid; align-items: end; gap: 12px; margin-bottom: 16px; grid-template-columns: repeat(2, minmax(0, 1fr)) auto; }
.search-field { display: flex; min-width: 0; flex-direction: column; gap: 6px; }
.search-field label { color: #475467; font-size: 12px; font-weight: 650; }
.reference-title { display: flex; min-width: 0; flex-direction: column; gap: 5px; }
.reference-title strong, .reference-summary { display: block; overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }
.reference-title small { color: #98a2b3; }
.reference-pagination { justify-content: center; margin-top: 18px; }
.reference-tip { margin: 10px 0 0; color: #98a2b3; font-size: 12px; text-align: center; }
@media (max-width: 680px) { .reference-search { grid-template-columns: 1fr; }.reference-search .el-button { width: 100%; } }
</style>
