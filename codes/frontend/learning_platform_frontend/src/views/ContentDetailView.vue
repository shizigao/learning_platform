<script setup lang="ts">
import { ChatDotRound, Download, Lock, MagicStick, Star, StarFilled, VideoPlay } from '@element-plus/icons-vue'
import { ElMessage } from 'element-plus'
import { computed, onMounted, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'

import {
  createComment,
  favoriteContent,
  getContent,
  getContentFileUrl,
  getReaction,
  likeContent,
  listComments,
  startLearning,
  unfavoriteContent,
  unlikeContent,
} from '@/api/content'
import MarkdownRenderer from '@/components/MarkdownRenderer.vue'
import ThumbUpIcon from '@/components/ThumbUpIcon.vue'
import type { CommentPage, ContentDetail, ContentReaction } from '@/types/content'

const route = useRoute()
const router = useRouter()
const contentId = Number(route.params.id)
const loading = ref(true)
const actionLoading = ref(false)
const detail = ref<ContentDetail>()
const reaction = ref<ContentReaction>({ liked: false, favorited: false, likeCount: 0, favoriteCount: 0 })
const comments = ref<CommentPage>({ items: [], total: 0, pageNumber: 1, pageSize: 20, totalPages: 0 })
const commentBody = ref('')
const priceText = computed(() =>
  detail.value?.distributionMode === 'CLASS'
    ? '班级资料'
    : detail.value?.isFree
      ? '免费学习'
      : `¥${Number(detail.value?.price ?? 0).toFixed(2)}`,
)

async function load(): Promise<void> {
  loading.value = true
  try {
    ;[detail.value, reaction.value, comments.value] = await Promise.all([
      getContent(contentId),
      getReaction(contentId),
      listComments(contentId),
    ])
  } catch (error) {
    ElMessage.error(error instanceof Error ? error.message : '加载资料失败')
  } finally {
    loading.value = false
  }
}

async function start(): Promise<void> {
  actionLoading.value = true
  try {
    await startLearning(contentId)
    await router.push(`/contents/${contentId}/learn`)
  } catch (error) {
    ElMessage.error(error instanceof Error ? error.message : '暂时无法开始学习')
  } finally {
    actionLoading.value = false
  }
}

async function purchase(): Promise<void> {
  await router.push({ path: '/commerce', query: { type: 'CONTENT' } })
}

async function toggleLike(): Promise<void> {
  try {
    reaction.value = reaction.value.liked
      ? await unlikeContent(contentId)
      : await likeContent(contentId)
  } catch (error) {
    ElMessage.error(error instanceof Error ? error.message : '操作失败')
  }
}

async function toggleFavorite(): Promise<void> {
  try {
    reaction.value = reaction.value.favorited
      ? await unfavoriteContent(contentId)
      : await favoriteContent(contentId)
  } catch (error) {
    ElMessage.error(error instanceof Error ? error.message : '操作失败')
  }
}

async function openFile(fileId: number, mode: 'preview' | 'download'): Promise<void> {
  try {
    window.open(await getContentFileUrl(contentId, fileId, mode), '_blank', 'noopener')
  } catch (error) {
    ElMessage.error(error instanceof Error ? error.message : '文件访问失败')
  }
}

async function submitComment(): Promise<void> {
  const body = commentBody.value.trim()
  if (!body) return
  try {
    await createComment(contentId, body)
    commentBody.value = ''
    comments.value = await listComments(contentId)
    ElMessage.success('评论已发布')
  } catch (error) {
    ElMessage.error(error instanceof Error ? error.message : '评论发布失败')
  }
}

onMounted(load)
</script>

<template>
  <section v-loading="loading" class="detail-page">
    <div v-if="detail" class="page-container">
      <div class="breadcrumb"><RouterLink to="/contents">学习资料</RouterLink><span>/</span><span>{{ detail.title }}</span></div>
      <div class="detail-layout">
        <article class="main-card">
          <div class="badges">
            <el-tag>{{ detail.categoryName || '学习资料' }}</el-tag>
            <el-tag :type="detail.isFree ? 'success' : 'warning'">{{ priceText }}</el-tag>
          </div>
          <h1>{{ detail.title }}</h1>
          <p class="summary">{{ detail.summary }}</p>
          <RouterLink :to="`/users/${detail.publisherId}`" class="publisher-card">
            <el-avatar :size="46" :src="detail.publisherAvatarUrl">
              {{ (detail.publisherName || detail.publisherUsername || '用').slice(0, 1) }}
            </el-avatar>
            <span>
              <small>资料发布者</small>
              <strong>{{ detail.publisherName || `用户 ${detail.publisherId}` }}</strong>
              <em v-if="detail.publisherUsername">@{{ detail.publisherUsername }}</em>
            </span>
          </RouterLink>
          <div class="metrics">
            <span>{{ detail.viewCount }} 浏览</span>
            <span>{{ reaction.likeCount }} 点赞</span>
            <span>{{ reaction.favoriteCount }} 收藏</span>
            <span>{{ comments.total }} 评论</span>
          </div>

          <MarkdownRenderer
            v-if="detail.hasAccess && detail.articleBody"
            class="article-preview"
            :source="detail.articleBody"
            :content-id="contentId"
            :content-reference-navigation-enabled="false"
          />
          <div v-else-if="!detail.hasAccess" class="locked-panel">
            <el-icon><Lock /></el-icon>
            <div><strong>这是付费学习资料</strong><p>完成购买后即可查看完整正文和下载文件。</p></div>
          </div>

          <section v-if="detail.files.length" class="files">
            <h2>资料文件</h2>
            <div v-for="file in detail.files" :key="file.id" class="file-row">
              <div><strong>{{ file.originalName }}</strong><small>{{ (file.sizeBytes / 1024 / 1024).toFixed(2) }} MB · {{ file.fileRole }}</small></div>
              <div>
                <el-button :disabled="!detail.hasAccess" @click="openFile(file.id, 'preview')">预览</el-button>
                <el-button :icon="Download" :disabled="!detail.hasAccess" @click="openFile(file.id, 'download')">下载</el-button>
              </div>
            </div>
          </section>
        </article>

        <aside class="action-card">
          <div class="price">{{ priceText }}</div>
          <p>
            {{ detail.hasAccess ? '你已获得该资料的学习权限' : '可前往权益商城创建测试订单并进行模拟支付，不会真实扣款' }}
          </p>
          <el-button
            v-if="detail.hasAccess"
            type="primary"
            size="large"
            :icon="VideoPlay"
            :loading="actionLoading"
            @click="start"
          >
            开始学习
          </el-button>
          <el-button v-else type="warning" size="large" @click="purchase">
            前往模拟购买（不真实扣款）
          </el-button>
          <RouterLink
            v-if="detail.hasAccess"
            :to="{ path: '/ai-assistant', query: { contentId } }"
          >
            <el-button size="large" :icon="MagicStick">使用 AI 学习助手</el-button>
          </RouterLink>
          <div class="reaction-actions">
            <el-button :type="reaction.liked ? 'primary' : 'default'" :icon="ThumbUpIcon" @click="toggleLike">{{ reaction.liked ? '已点赞' : '点赞' }}</el-button>
            <el-button :type="reaction.favorited ? 'primary' : 'default'" :icon="reaction.favorited ? StarFilled : Star" @click="toggleFavorite">{{ reaction.favorited ? '已收藏' : '收藏' }}</el-button>
          </div>
        </aside>
      </div>

      <section class="comment-card">
        <h2><el-icon><ChatDotRound /></el-icon>学习讨论</h2>
        <div class="comment-editor">
          <el-input v-model="commentBody" maxlength="2000" :rows="3" type="textarea" placeholder="分享你的学习收获或疑问" />
          <el-button type="primary" :disabled="!detail.hasAccess" @click="submitComment">发表评论</el-button>
        </div>
        <div v-for="item in comments.items" :key="item.id" class="comment-item">
          <el-avatar :size="34">学</el-avatar>
          <div><strong>用户 {{ item.userId }}</strong><p>{{ item.body }}</p><small>{{ new Date(item.createdAt).toLocaleString() }}</small></div>
        </div>
        <el-empty v-if="comments.items.length === 0" description="还没有评论，来发表第一条吧" :image-size="72" />
      </section>
    </div>
  </section>
</template>

<style scoped>
.detail-page { min-height: calc(100vh - 145px); padding: 38px 0 78px; background: #f6f8fc; }
.breadcrumb { display: flex; overflow: hidden; gap: 8px; margin-bottom: 20px; color: var(--lp-text-secondary); font-size: 13px; white-space: nowrap; }
.breadcrumb span:last-child { overflow: hidden; text-overflow: ellipsis; }
.detail-layout { display: grid; align-items: start; gap: 24px; grid-template-columns: minmax(0, 1fr) 300px; }
.main-card, .action-card, .comment-card { border: 1px solid var(--lp-border); border-radius: 20px; background: #fff; box-shadow: var(--lp-shadow); }
.main-card { padding: 34px; }
.badges { display: flex; gap: 8px; }
h1 { margin: 18px 0 12px; font-size: clamp(28px, 4vw, 40px); letter-spacing: -.03em; }
.summary { color: var(--lp-text-secondary); font-size: 16px; line-height: 1.8; }
.publisher-card { display: flex; align-items: center; width: fit-content; gap: 12px; border: 1px solid var(--lp-border); border-radius: 14px; margin-top: 22px; color: inherit; background: #f8faff; padding: 11px 14px; }.publisher-card:hover { border-color: #b7d0ff; }.publisher-card span, .publisher-card small, .publisher-card em { display: block; }.publisher-card small, .publisher-card em { color: var(--lp-text-secondary); font-size: 11px; font-style: normal; }.publisher-card strong { display: block; margin: 3px 0; }
.metrics { display: flex; flex-wrap: wrap; gap: 24px; border-bottom: 1px solid var(--lp-border); margin-top: 18px; color: #98a2b3; font-size: 13px; padding-bottom: 24px; }
.article-preview { margin-top: 28px; font-size: 15px; }
.locked-panel { display: flex; align-items: center; gap: 16px; border-radius: 14px; margin-top: 26px; color: #92400e; background: #fffbeb; padding: 20px; }
.locked-panel .el-icon { font-size: 30px; }.locked-panel p { margin: 5px 0 0; font-size: 13px; }
.action-card { position: sticky; top: 94px; padding: 26px; }
.action-card .price { color: var(--lp-primary); font-size: 28px; font-weight: 800; }
.action-card > p { color: var(--lp-text-secondary); font-size: 13px; line-height: 1.7; }.action-card > .el-button, .action-card > a { display: block; width: 100%; margin-top: 18px; }.action-card > a .el-button { width: 100%; }
.reaction-actions { display: grid; gap: 8px; margin-top: 12px; grid-template-columns: 1fr 1fr; }.reaction-actions .el-button { margin: 0; }
.files { margin-top: 30px; }.files h2, .comment-card h2 { font-size: 18px; }
.file-row { display: flex; align-items: center; justify-content: space-between; gap: 12px; border-top: 1px solid var(--lp-border); padding: 14px 0; }.file-row small { display: block; margin-top: 5px; color: #98a2b3; }
.comment-card { margin-top: 24px; padding: 28px; }.comment-card h2 { display: flex; align-items: center; gap: 8px; }
.comment-editor { display: flex; align-items: flex-end; gap: 12px; }.comment-item { display: flex; gap: 12px; border-top: 1px solid var(--lp-border); margin-top: 20px; padding-top: 20px; }.comment-item p { margin: 8px 0; line-height: 1.7; }.comment-item small { color: #98a2b3; }
@media (max-width: 860px) { .detail-layout { grid-template-columns: 1fr; } .action-card { position: static; } }
@media (max-width: 560px) { .main-card, .comment-card { padding: 22px 18px; } .file-row, .comment-editor { align-items: stretch; flex-direction: column; } }
</style>
