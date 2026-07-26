<script setup lang="ts">
import { Search } from '@element-plus/icons-vue'
import { ElMessage } from 'element-plus'
import { ref, watch } from 'vue'

import { getCategory, searchCategories } from '@/api/content'
import type { ContentCategory, ContentCategoryPage } from '@/types/content'

const props = withDefaults(defineProps<{
  modelValue?: number
  disabled?: boolean
  allowClear?: boolean
  placeholder?: string
}>(), {
  disabled: false,
  allowClear: false,
  placeholder: '请选择资料分类',
})

const emit = defineEmits<{
  'update:modelValue': [value: number | undefined]
  change: [value: ContentCategory | undefined]
}>()

const visible = ref(false)
const loading = ref(false)
const selected = ref<ContentCategory>()
const keyword = ref('')
const page = ref<ContentCategoryPage>({
  items: [],
  total: 0,
  pageNumber: 1,
  pageSize: 20,
  totalPages: 0,
})

async function syncSelected(categoryId?: number): Promise<void> {
  if (!categoryId) {
    selected.value = undefined
    return
  }
  if (selected.value?.id === categoryId) return
  try {
    selected.value = await getCategory(categoryId)
  } catch {
    selected.value = undefined
  }
}

async function load(pageNumber = page.value.pageNumber): Promise<void> {
  loading.value = true
  try {
    page.value = await searchCategories(keyword.value, pageNumber, page.value.pageSize)
  } catch (error) {
    ElMessage.error(error instanceof Error ? error.message : '加载资料分类失败')
  } finally {
    loading.value = false
  }
}

function open(): void {
  if (props.disabled) return
  visible.value = true
  keyword.value = ''
  void load(1)
}

function search(): void {
  void load(1)
}

function choose(row: unknown): void {
  const category = row as ContentCategory
  selected.value = category
  emit('update:modelValue', category.id)
  emit('change', category)
  visible.value = false
}

function clear(): void {
  selected.value = undefined
  emit('update:modelValue', undefined)
  emit('change', undefined)
}

watch(() => props.modelValue, (value) => void syncSelected(value), { immediate: true })
</script>

<template>
  <div class="category-picker" :class="{ disabled }">
    <button type="button" class="category-trigger" :disabled="disabled" @click="open">
      <span :class="{ placeholder: !selected }">{{ selected?.name || placeholder }}</span>
      <small v-if="selected">{{ selected.slug }}</small>
      <strong>选择</strong>
    </button>
    <el-button
      v-if="allowClear && modelValue"
      class="clear-button"
      text
      type="primary"
      :disabled="disabled"
      @click="clear"
    >
      清除
    </el-button>
  </div>

  <el-dialog v-model="visible" title="选择资料分类" width="min(820px, 92vw)" destroy-on-close>
    <div class="category-search">
      <el-input
        v-model="keyword"
        :prefix-icon="Search"
        clearable
        placeholder="搜索分类名称、标识或说明"
        @keyup.enter="search"
        @clear="search"
      />
      <el-button type="primary" @click="search">搜索</el-button>
    </div>

    <el-table
      v-loading="loading"
      :data="page.items"
      height="420"
      highlight-current-row
      row-class-name="category-row"
      @row-dblclick="choose"
    >
      <el-table-column label="分类名称" min-width="180">
        <template #default="{ row }">
          <div class="category-name">
            <strong>{{ row.name }}</strong>
            <small>{{ row.slug }}</small>
          </div>
        </template>
      </el-table-column>
      <el-table-column prop="description" label="分类说明" min-width="280">
        <template #default="{ row }">{{ row.description || '暂无说明' }}</template>
      </el-table-column>
      <el-table-column label="操作" width="90" align="center">
        <template #default="{ row }">
          <el-button
            link
            type="primary"
            :disabled="row.id === modelValue"
            @click="choose(row)"
          >
            {{ row.id === modelValue ? '已选择' : '选择' }}
          </el-button>
        </template>
      </el-table-column>
      <template #empty><el-empty description="没有匹配的资料分类" /></template>
    </el-table>

    <el-pagination
      v-if="page.total > page.pageSize"
      class="category-pagination"
      layout="total, prev, pager, next"
      :current-page="page.pageNumber"
      :page-size="page.pageSize"
      :total="page.total"
      @current-change="load"
    />
    <p class="category-tip">双击分类行也可以快速选择。</p>
  </el-dialog>
</template>

<style scoped>
.category-picker { display: flex; align-items: center; gap: 4px; width: 100%; }
.category-trigger { display: grid; width: 100%; min-height: 40px; border: 1px solid #dcdfe6; border-radius: 4px; align-items: center; gap: 3px 10px; color: var(--lp-text); background: #fff; cursor: pointer; grid-template-columns: minmax(0, 1fr) auto; padding: 6px 12px; text-align: left; transition: border-color .18s; }
.category-trigger:hover { border-color: var(--lp-primary); }
.category-trigger span { overflow: hidden; font-size: 14px; text-overflow: ellipsis; white-space: nowrap; }
.category-trigger span.placeholder { color: #a8abb2; }
.category-trigger small { overflow: hidden; color: #98a2b3; font-size: 11px; grid-column: 1; text-overflow: ellipsis; white-space: nowrap; }
.category-trigger strong { color: var(--lp-primary); font-size: 13px; grid-column: 2; grid-row: 1 / span 2; }
.category-picker.disabled { opacity: .65; }
.category-trigger:disabled { cursor: not-allowed; }
.clear-button { flex: none; }
.category-search { display: grid; gap: 10px; margin-bottom: 16px; grid-template-columns: minmax(0, 1fr) auto; }
.category-name { display: flex; flex-direction: column; gap: 4px; }
.category-name small { color: #98a2b3; }
.category-pagination { justify-content: center; margin-top: 18px; }
.category-tip { margin: 10px 0 0; color: #98a2b3; font-size: 12px; text-align: center; }
</style>
