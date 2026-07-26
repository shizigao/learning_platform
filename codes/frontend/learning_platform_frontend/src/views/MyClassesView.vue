<script setup lang="ts">
import { ElMessage, ElMessageBox } from 'element-plus'
import { onMounted, ref } from 'vue'

import {
  joinClass,
  leaveClass,
  listClassAnnouncements,
  listClassContents,
  listClassExams,
  listClassMembers,
  listMyClasses,
} from '@/api/classroom'
import ContentCard from '@/components/ContentCard.vue'
import MarkdownRenderer from '@/components/MarkdownRenderer.vue'
import SectionPageHeader from '@/components/SectionPageHeader.vue'
import { useAuthStore } from '@/stores/auth'
import type {
  ClassAnnouncement,
  ClassMember,
  Classroom,
} from '@/types/classroom'
import type { ContentSummary } from '@/types/content'
import type { ExamStatus, ExamSummary } from '@/types/exam'

const roleLabels = { OWNER: '拥有者', ADMIN: '管理员', MEMBER: '成员' } as const
const authStore = useAuthStore()
const examStatusLabels: Record<ExamStatus, string> = {
  DRAFT: '草稿',
  PUBLISHED: '已发布',
  ONGOING: '进行中',
  FINISHED: '已结束',
  CANCELLED: '已取消',
}

const loading = ref(false)
const detailLoading = ref(false)
const classes = ref<Classroom[]>([])
const selected = ref<Classroom>()
const announcements = ref<ClassAnnouncement[]>([])
const members = ref<ClassMember[]>([])
const contents = ref<ContentSummary[]>([])
const exams = ref<ExamSummary[]>([])
const activeTab = ref('announcements')
const joinVisible = ref(false)
const joining = ref(false)
const inviteCode = ref('')

async function loadClasses(selectId?: number): Promise<void> {
  loading.value = true
  try {
    classes.value = await listMyClasses()
    const target = classes.value.find((item) => item.id === (selectId ?? selected.value?.id))
    selected.value = target ?? classes.value[0]
    if (selected.value) await loadDetail(selected.value)
  } catch (error) {
    ElMessage.error(error instanceof Error ? error.message : '班级列表加载失败')
  } finally {
    loading.value = false
  }
}

async function loadDetail(classroom: Classroom): Promise<void> {
  selected.value = classroom
  detailLoading.value = true
  try {
    const [announcementList, memberPage, contentPage, examPage] = await Promise.all([
      listClassAnnouncements(classroom.id),
      listClassMembers(classroom.id, '', 1, 100),
      listClassContents(classroom.id, 1, 100),
      listClassExams(classroom.id, 1, 100),
    ])
    announcements.value = announcementList
    members.value = memberPage.items
    contents.value = contentPage.items
    exams.value = examPage.items
  } catch (error) {
    ElMessage.error(error instanceof Error ? error.message : '班级详情加载失败')
  } finally {
    detailLoading.value = false
  }
}

async function submitJoin(): Promise<void> {
  if (!inviteCode.value.trim()) {
    ElMessage.warning('请输入班级邀请码')
    return
  }
  joining.value = true
  try {
    const classroom = await joinClass(inviteCode.value.trim())
    joinVisible.value = false
    inviteCode.value = ''
    ElMessage.success(`已加入班级“${classroom.name}”`)
    await loadClasses(classroom.id)
  } catch (error) {
    ElMessage.error(error instanceof Error ? error.message : '加入班级失败')
  } finally {
    joining.value = false
  }
}

async function leave(): Promise<void> {
  if (!selected.value) return
  try {
    await ElMessageBox.confirm(
      `确定退出班级“${selected.value.name}”吗？退出后将失去班级资料和考试访问权。`,
      '退出班级',
      { type: 'warning' },
    )
    await leaveClass(selected.value.id)
    ElMessage.success('已退出班级')
    selected.value = undefined
    await loadClasses()
  } catch (error) {
    if (error !== 'cancel') ElMessage.error(error instanceof Error ? error.message : '退出班级失败')
  }
}

function displayTime(value: string): string {
  return new Date(value).toLocaleString('zh-CN', { hour12: false })
}

function examEnded(exam: ExamSummary): boolean {
  return new Date(exam.endAt).getTime() <= Date.now()
}

function isExamPublisher(exam: ExamSummary): boolean {
  return exam.publisherId === authStore.user?.id
}

onMounted(() => loadClasses())
</script>

<template>
  <section class="classes-page">
    <div class="page-container">
      <SectionPageHeader
        eyebrow="MY CLASSES"
        title="我的班级"
        description="通过邀请码加入班级，查看班级公告、学习资料和考试"
      >
        <el-button type="primary" @click="joinVisible = true">使用邀请码加入</el-button>
      </SectionPageHeader>

      <div v-loading="loading" class="class-layout">
        <aside class="class-sidebar">
          <button
            v-for="item in classes"
            :key="item.id"
            type="button"
            :class="{ active: selected?.id === item.id }"
            @click="loadDetail(item)"
          >
            <strong>{{ item.name }}</strong>
            <span>{{ roleLabels[item.currentRole] }} · {{ item.memberCount }} 人</span>
          </button>
          <el-empty v-if="!loading && classes.length === 0" :image-size="70" description="尚未加入班级" />
        </aside>

        <main v-if="selected" v-loading="detailLoading" class="class-detail">
          <header>
            <div>
              <el-tag>{{ roleLabels[selected.currentRole] }}</el-tag>
              <h2>{{ selected.name }}</h2>
              <p>{{ selected.description || '暂无班级介绍' }}</p>
            </div>
            <el-button
              v-if="selected.currentRole !== 'OWNER'"
              type="danger"
              plain
              @click="leave"
            >
              退出班级
            </el-button>
          </header>

          <el-tabs v-model="activeTab">
            <el-tab-pane label="公告" name="announcements">
              <div class="announcement-list">
                <article v-for="item in announcements" :key="item.id">
                  <div>
                    <RouterLink :to="`/users/${item.authorId}`" class="announcement-author">
                      <el-avatar :size="32" :src="item.authorAvatarUrl">{{ item.authorName.slice(0, 1) }}</el-avatar>
                      <span>{{ item.authorName }}</span>
                    </RouterLink>
                    <span><el-tag v-if="item.pinned" type="danger" size="small">置顶</el-tag> {{ displayTime(item.createdAt) }}</span>
                  </div>
                  <h3>{{ item.title }}</h3>
                  <MarkdownRenderer :source="item.body" />
                </article>
                <el-empty v-if="announcements.length === 0" description="暂无班级公告" />
              </div>
            </el-tab-pane>

            <el-tab-pane label="学习资料" name="contents">
              <div class="content-grid">
                <ContentCard v-for="item in contents" :key="item.id" :content="item" />
                <el-empty v-if="contents.length === 0" description="暂无班级资料" />
              </div>
            </el-tab-pane>

            <el-tab-pane label="考试" name="exams">
              <div class="exam-list">
                <article v-for="exam in exams" :key="exam.id">
                  <div>
                    <el-tag type="success">{{ examStatusLabels[exam.status] }}</el-tag>
                    <span>{{ exam.durationMinutes }} 分钟</span>
                  </div>
                  <h3>{{ exam.name }}</h3>
                  <p>{{ displayTime(exam.startAt) }} — {{ displayTime(exam.endAt) }}</p>
                  <div class="exam-actions">
                    <RouterLink :to="`/exams/${exam.id}`">查看考试 →</RouterLink>
                    <RouterLink
                      v-if="examEnded(exam)"
                      :to="`/exams/${exam.id}/result/ai-analysis`"
                    >
                      个人 AI 分析
                    </RouterLink>
                    <RouterLink
                      v-if="examEnded(exam) && isExamPublisher(exam)"
                      :to="`/publisher/exams/${exam.id}/grading/ai-analysis`"
                    >
                      整体 AI 分析
                    </RouterLink>
                  </div>
                </article>
                <el-empty v-if="exams.length === 0" description="暂无班级考试" />
              </div>
            </el-tab-pane>

            <el-tab-pane :label="`成员（${members.length}）`" name="members">
              <el-table :data="members">
                <el-table-column label="头像" width="76">
                  <template #default="{ row }">
                    <RouterLink :to="`/users/${row.userId}`">
                      <el-avatar :size="38" :src="row.avatarUrl">{{ row.nickname.slice(0, 1) }}</el-avatar>
                    </RouterLink>
                  </template>
                </el-table-column>
                <el-table-column prop="nickname" label="昵称" />
                <el-table-column prop="username" label="用户名" />
                <el-table-column label="班级角色" width="130">
                  <template #default="{ row }">{{ roleLabels[row.role as keyof typeof roleLabels] }}</template>
                </el-table-column>
                <el-table-column prop="joinedAt" label="加入时间" min-width="180" />
              </el-table>
            </el-tab-pane>
          </el-tabs>
        </main>
        <div v-else class="empty-detail"><el-empty description="加入或选择一个班级后查看详情" /></div>
      </div>
    </div>

    <el-dialog v-model="joinVisible" title="通过邀请码加入班级" width="min(480px, 94vw)">
      <el-form label-position="top">
        <el-form-item label="班级邀请码" required>
          <el-input
            v-model="inviteCode"
            maxlength="32"
            placeholder="请输入班级拥有者提供的邀请码"
            @keyup.enter="submitJoin"
          />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="joinVisible = false">取消</el-button>
        <el-button type="primary" :loading="joining" @click="submitJoin">加入班级</el-button>
      </template>
    </el-dialog>
  </section>
</template>

<style scoped>
.classes-page { min-height: calc(100vh - 145px); padding: 48px 0 76px; background: #f6f8fc; }.class-layout { display: grid; min-height: 560px; gap: 20px; grid-template-columns: 280px minmax(0, 1fr); }.class-sidebar, .class-detail, .empty-detail { border: 1px solid var(--lp-border); border-radius: 18px; background: #fff; box-shadow: var(--lp-shadow); }.class-sidebar { align-self: start; overflow: hidden; padding: 10px; }.class-sidebar button { display: block; width: 100%; border: 0; border-radius: 10px; background: transparent; cursor: pointer; padding: 14px; text-align: left; }.class-sidebar button:hover, .class-sidebar button.active { color: var(--lp-primary); background: #eff6ff; }.class-sidebar strong, .class-sidebar span { display: block; }.class-sidebar span { margin-top: 5px; color: var(--lp-text-secondary); font-size: 12px; }.class-detail { overflow: hidden; padding: 26px; }.class-detail > header { display: flex; align-items: flex-start; justify-content: space-between; gap: 20px; border-bottom: 1px solid var(--lp-border); margin-bottom: 16px; padding-bottom: 20px; }.class-detail h2 { margin: 10px 0 5px; }.class-detail header p { margin: 0; color: var(--lp-text-secondary); }.announcement-list article, .exam-list article { border: 1px solid var(--lp-border); border-radius: 14px; margin-bottom: 14px; background: #fff; padding: 20px; }.announcement-list article > div, .exam-list article > div { display: flex; align-items: center; justify-content: space-between; gap: 10px; color: var(--lp-text-secondary); font-size: 12px; }.announcement-list h3, .exam-list h3 { margin: 15px 0 10px; }.content-grid { display: grid; gap: 16px; grid-template-columns: repeat(auto-fill, minmax(250px, 1fr)); }.exam-list article > p { color: var(--lp-text-secondary); }.exam-actions { display: flex; flex-wrap: wrap; gap: 16px; }.exam-actions > a { color: var(--lp-primary); font-weight: 700; }.empty-detail { display: grid; place-items: center; }.content-grid > .el-empty, .exam-list > .el-empty { grid-column: 1 / -1; }
.announcement-author { display: flex; align-items: center; gap: 8px; color: var(--lp-text); font-weight: 700; }
@media (max-width: 800px) { .class-layout { grid-template-columns: 1fr; }.class-sidebar { display: flex; overflow-x: auto; }.class-sidebar button { min-width: 200px; }.class-detail > header { flex-direction: column; } }
</style>
