<script setup lang="ts">
import { Collection, DataBoard, Document, VideoCamera } from '@element-plus/icons-vue'
import { computed } from 'vue'

import type { ContentSummary } from '@/types/content'

const props = defineProps<{
  content: ContentSummary
  categoryName?: string
}>()

const icon = computed(() => {
  if (props.content.contentType === 'VIDEO') return VideoCamera
  if (props.content.contentType === 'ARTICLE') return Document
  if (props.content.contentType === 'MIXED') return DataBoard
  return Collection
})

const typeLabel = computed(
  () =>
    ({
      ARTICLE: '图文',
      DOCUMENT: '文档',
      VIDEO: '视频',
      ATTACHMENT: '附件',
      MIXED: '综合',
    })[props.content.contentType],
)
</script>

<template>
  <RouterLink :to="`/contents/${content.id}`" class="content-card">
    <div class="cover">
      <el-icon><component :is="icon" /></el-icon>
      <span>{{ typeLabel }}</span>
    </div>
    <div class="card-body">
      <div class="meta">
        <span>{{ categoryName || '学习资料' }}</span>
        <el-tag :type="content.isFree ? 'success' : 'warning'" size="small" effect="light">
          {{ content.isFree ? '免费' : `¥${Number(content.price).toFixed(2)}` }}
        </el-tag>
      </div>
      <h3>{{ content.title }}</h3>
      <div class="publisher">发布者：{{ content.publisherName || `用户 ${content.publisherId}` }}</div>
      <p>{{ content.summary || '暂无简介' }}</p>
      <div class="statistics">
        <span>{{ content.viewCount }} 次浏览</span>
        <span>{{ content.likeCount }} 赞</span>
        <span>{{ content.favoriteCount }} 收藏</span>
      </div>
    </div>
  </RouterLink>
</template>

<style scoped>
.content-card { display: flex; overflow: hidden; min-width: 0; border: 1px solid var(--lp-border); border-radius: 18px; flex-direction: column; background: #fff; box-shadow: 0 8px 26px rgb(16 24 40 / 5%); transition: 180ms ease; }
.content-card:hover { border-color: #bfd3ff; box-shadow: 0 18px 42px rgb(37 99 235 / 12%); transform: translateY(-3px); }
.cover { display: flex; height: 142px; align-items: center; justify-content: center; flex-direction: column; gap: 10px; color: #2563eb; background: linear-gradient(135deg, #eff6ff, #eef2ff); }
.cover .el-icon { font-size: 34px; }
.cover span { font-size: 12px; font-weight: 700; letter-spacing: .12em; }
.card-body { display: flex; min-height: 190px; flex-direction: column; padding: 18px; }
.meta, .statistics { display: flex; align-items: center; justify-content: space-between; gap: 8px; }
.meta > span { overflow: hidden; color: var(--lp-primary); font-size: 12px; font-weight: 700; text-overflow: ellipsis; white-space: nowrap; }
h3 { overflow: hidden; margin: 14px 0 8px; font-size: 18px; line-height: 1.4; text-overflow: ellipsis; white-space: nowrap; }
.publisher { overflow: hidden; margin-bottom: 7px; color: #475467; font-size: 12px; text-overflow: ellipsis; white-space: nowrap; }
p { display: -webkit-box; overflow: hidden; margin: 0; color: var(--lp-text-secondary); font-size: 13px; line-height: 1.7; -webkit-box-orient: vertical; -webkit-line-clamp: 2; }
.statistics { border-top: 1px solid #f0f2f5; margin-top: auto; color: #98a2b3; font-size: 11px; padding-top: 14px; }
</style>
