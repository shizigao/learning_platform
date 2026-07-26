<script setup lang="ts">
import { Collection, Star, View } from '@element-plus/icons-vue'
import { ElMessage } from 'element-plus'
import { computed, ref, watch } from 'vue'
import { useRoute } from 'vue-router'

import { getPublicUser, listPublicUserContents } from '@/api/user'
import { getTeacherByUser } from '@/api/offline-teaching'
import ContentCard from '@/components/ContentCard.vue'
import ThumbUpIcon from '@/components/ThumbUpIcon.vue'
import UserSearchPanel from '@/components/UserSearchPanel.vue'
import { useAuthStore } from '@/stores/auth'
import type { ContentSummary } from '@/types/content'
import type { PublicUserProfile } from '@/types/user'
import type { TeacherProfile } from '@/types/offline-teaching'

const route = useRoute()
const authStore = useAuthStore()
const loading = ref(false)
const profile = ref<PublicUserProfile>()
const contents = ref<ContentSummary[]>([])
const teacherProfile = ref<TeacherProfile>()
const contentPage = ref(1)
const contentTotal = ref(0)
const pageSize = 12
const userId = computed(() => Number(route.params.id))
const isSelf = computed(() => authStore.user?.id === userId.value)

const roleLabels = { USER: '学习者', PUBLISHER: '发布者', ADMIN: '管理员' } as const

async function load(): Promise<void> {
  if (!Number.isSafeInteger(userId.value) || userId.value <= 0) return
  loading.value = true
  try {
    const [user, contentPageResult] = await Promise.all([
      getPublicUser(userId.value),
      listPublicUserContents(userId.value, contentPage.value, pageSize),
    ])
    profile.value = user
    contents.value = contentPageResult.items
    contentTotal.value = contentPageResult.total
    try {
      teacherProfile.value = await getTeacherByUser(userId.value)
    } catch {
      teacherProfile.value = undefined
    }
  } catch (error) {
    ElMessage.error(error instanceof Error ? error.message : '个人中心加载失败')
  } finally {
    loading.value = false
  }
}

watch(
  () => route.params.id,
  () => {
    contentPage.value = 1
    void load()
  },
  { immediate: true },
)
</script>

<template>
  <section v-loading="loading" class="public-profile-page">
    <div v-if="profile" class="page-container">
      <div class="profile-hero">
        <el-avatar :size="108" :src="profile.avatarUrl">
          {{ (profile.nickname || profile.username).slice(0, 1).toUpperCase() }}
        </el-avatar>
        <div class="identity">
          <div class="name-row">
            <div><h1>{{ profile.nickname }}</h1><p>@{{ profile.username }}</p></div>
            <RouterLink v-if="isSelf" to="/profile"><el-button type="primary">编辑个人资料</el-button></RouterLink>
          </div>
          <div class="roles">
            <el-tag v-for="role in profile.roles" :key="role" round>{{ roleLabels[role] }}</el-tag>
          </div>
          <p class="bio">{{ profile.bio || '这个用户还没有填写个人简介。' }}</p>
          <small>加入平台于 {{ new Date(profile.createdAt).toLocaleDateString('zh-CN') }}</small>
        </div>
      </div>

      <div class="stats-grid">
        <div><el-icon><Collection /></el-icon><strong>{{ profile.statistics.contentCount }}</strong><span>公开资料</span></div>
        <div><el-icon><View /></el-icon><strong>{{ profile.statistics.viewCount }}</strong><span>总浏览量</span></div>
        <div><el-icon><ThumbUpIcon /></el-icon><strong>{{ profile.statistics.likeCount }}</strong><span>总点赞量</span></div>
        <div><el-icon><Star /></el-icon><strong>{{ profile.statistics.favoriteCount }}</strong><span>总收藏量</span></div>
      </div>

      <section v-if="teacherProfile" class="teacher-section">
        <div class="teacher-title">
          <div>
            <el-tag type="success" round>平台审核线下教师</el-tag>
            <h2>{{ teacherProfile.teacherName }}的线下教学信息</h2>
          </div>
          <strong>¥{{ Number(teacherProfile.hourlyRate).toFixed(2) }}/课时</strong>
        </div>
        <p>{{ teacherProfile.teachingContent }}</p>
        <p>可上课时间：{{ teacherProfile.availability || '请联系教师确认' }}</p>
        <div class="teacher-meta">
          <span>{{ teacherProfile.province }} {{ teacherProfile.city }} {{ teacherProfile.district || '' }}</span>
          <span>{{ teacherProfile.institution || '个人教师' }}</span>
          <span v-if="teacherProfile.contactWechat">微信：{{ teacherProfile.contactWechat }}</span>
          <span v-if="teacherProfile.contactQq">QQ：{{ teacherProfile.contactQq }}</span>
          <span v-if="teacherProfile.contactEmail">邮箱：{{ teacherProfile.contactEmail }}</span>
        </div>
        <div class="teacher-tags">
          <el-tag v-for="tag in teacherProfile.teachingTags" :key="tag">{{ tag }}</el-tag>
        </div>
        <RouterLink :to="{ path: '/offline-teaching', query: { teacher: teacherProfile.id } }">
          <el-button type="primary" plain>前往线下教学</el-button>
        </RouterLink>
      </section>

      <section class="content-section">
        <div class="section-heading"><div><h2>公开学习资料</h2><p>查看该用户发布的全部公开资料</p></div><span>共 {{ contentTotal }} 项</span></div>
        <div class="content-grid">
          <ContentCard v-for="item in contents" :key="item.id" :content="item" />
        </div>
        <el-empty v-if="!loading && contents.length === 0" description="该用户暂未发布公开资料" />
        <el-pagination
          v-if="contentTotal > pageSize"
          v-model:current-page="contentPage"
          class="pagination"
          background
          layout="prev, pager, next"
          :page-size="pageSize"
          :total="contentTotal"
          @current-change="load"
        />
      </section>

      <UserSearchPanel />
    </div>
  </section>
</template>

<style scoped>
.public-profile-page { min-height: calc(100vh - 145px); padding: 48px 0 78px; background: linear-gradient(180deg, #f3f7ff, #f7f9fc 340px); }
.profile-hero { display: grid; align-items: center; gap: 28px; border: 1px solid var(--lp-border); border-radius: 24px; background: #fff; box-shadow: var(--lp-shadow); padding: 36px; grid-template-columns: auto 1fr; }
.identity h1 { margin: 0; font-size: 32px; }.identity p { margin: 6px 0 0; color: var(--lp-text-secondary); }.name-row { display: flex; align-items: flex-start; justify-content: space-between; gap: 20px; }.roles { display: flex; flex-wrap: wrap; gap: 7px; margin-top: 16px; }.identity .bio { max-width: 760px; margin-top: 18px; color: var(--lp-text); line-height: 1.75; }.identity small { display: block; margin-top: 12px; color: #98a2b3; }
.stats-grid { display: grid; gap: 16px; margin-top: 22px; grid-template-columns: repeat(4, 1fr); }.stats-grid > div { display: grid; align-items: center; gap: 3px 12px; border: 1px solid var(--lp-border); border-radius: 16px; background: #fff; padding: 20px; grid-template-columns: auto 1fr; }.stats-grid .el-icon { width: 42px; height: 42px; border-radius: 12px; color: var(--lp-primary); background: #eef5ff; font-size: 20px; grid-row: 1 / 3; }.stats-grid strong { font-size: 24px; }.stats-grid span { color: var(--lp-text-secondary); font-size: 12px; }
.content-section { border: 1px solid var(--lp-border); border-radius: 20px; margin-top: 24px; background: #fff; box-shadow: var(--lp-shadow); padding: 28px; }.section-heading { display: flex; align-items: flex-end; justify-content: space-between; gap: 20px; margin-bottom: 20px; }.section-heading h2 { margin: 0; }.section-heading p { margin: 7px 0 0; color: var(--lp-text-secondary); }.section-heading > span { color: var(--lp-text-secondary); }.content-grid { display: grid; gap: 18px; grid-template-columns: repeat(auto-fill, minmax(250px, 1fr)); }.pagination { justify-content: center; margin-top: 28px; }
.teacher-section { border: 1px solid #b7ebc9; border-radius: 20px; margin-top: 24px; background: linear-gradient(135deg, #f3fff7, #fff); box-shadow: var(--lp-shadow); padding: 28px; }.teacher-title { display: flex; align-items: flex-start; justify-content: space-between; gap: 20px; }.teacher-title h2 { margin: 10px 0 0; }.teacher-title > strong { color: #f79009; font-size: 20px; }.teacher-section > p { color: var(--lp-text-secondary); line-height: 1.75; }.teacher-meta, .teacher-tags { display: flex; flex-wrap: wrap; gap: 8px 18px; margin: 14px 0; }.teacher-meta span { color: #475467; font-size: 13px; }.teacher-tags { gap: 7px; }
@media (max-width: 760px) { .profile-hero { justify-items: center; grid-template-columns: 1fr; text-align: center; }.name-row { align-items: center; flex-direction: column; }.roles { justify-content: center; }.stats-grid { grid-template-columns: repeat(2, 1fr); } }
@media (max-width: 480px) { .stats-grid { grid-template-columns: 1fr; }.content-section, .profile-hero { padding: 22px 18px; } }
</style>
