<script setup lang="ts">
import { ElMessage, ElMessageBox } from 'element-plus'
import { computed, onMounted, reactive, ref } from 'vue'

import {
  archiveClass,
  createClass,
  createClassAnnouncement,
  deleteClassAnnouncement,
  listClassAnnouncements,
  listClassMembers,
  listManagedClasses,
  regenerateClassInvite,
  removeClassMember,
  setClassInviteEnabled,
  setClassMemberRole,
  transferClassOwnership,
  updateClass,
  updateClassAnnouncement,
} from '@/api/classroom'
import MarkdownRenderer from '@/components/MarkdownRenderer.vue'
import SectionPageHeader from '@/components/SectionPageHeader.vue'
import { useAuthStore } from '@/stores/auth'
import type { ClassAnnouncement, ClassMember, Classroom } from '@/types/classroom'

const authStore = useAuthStore()
const roleLabels = { OWNER: '拥有者', ADMIN: '管理员', MEMBER: '成员' } as const
const loading = ref(false)
const detailLoading = ref(false)
const classes = ref<Classroom[]>([])
const selected = ref<Classroom>()
const members = ref<ClassMember[]>([])
const announcements = ref<ClassAnnouncement[]>([])
const memberKeyword = ref('')
const activeTab = ref('members')

const classDialogVisible = ref(false)
const classSaving = ref(false)
const editingClass = ref(false)
const classForm = reactive({ name: '', description: '' })

const announcementDialogVisible = ref(false)
const announcementSaving = ref(false)
const editingAnnouncementId = ref<number>()
const announcementForm = reactive({ title: '', body: '', pinned: false })

const isOwner = computed(() => selected.value?.currentRole === 'OWNER')
const currentUserId = computed(() => authStore.user?.id)

async function loadClasses(selectId?: number): Promise<void> {
  loading.value = true
  try {
    classes.value = await listManagedClasses()
    const next = classes.value.find((item) => item.id === (selectId ?? selected.value?.id))
    selected.value = next ?? classes.value[0]
    if (selected.value) await loadDetail(selected.value)
  } catch (error) {
    ElMessage.error(error instanceof Error ? error.message : '班级管理数据加载失败')
  } finally {
    loading.value = false
  }
}

async function loadDetail(classroom: Classroom): Promise<void> {
  selected.value = classroom
  detailLoading.value = true
  try {
    const [memberPage, announcementList] = await Promise.all([
      listClassMembers(classroom.id, memberKeyword.value, 1, 100),
      listClassAnnouncements(classroom.id),
    ])
    members.value = memberPage.items
    announcements.value = announcementList
  } catch (error) {
    ElMessage.error(error instanceof Error ? error.message : '班级详情加载失败')
  } finally {
    detailLoading.value = false
  }
}

function openCreate(): void {
  editingClass.value = false
  classForm.name = ''
  classForm.description = ''
  classDialogVisible.value = true
}

function openEdit(): void {
  if (!selected.value) return
  editingClass.value = true
  classForm.name = selected.value.name
  classForm.description = selected.value.description ?? ''
  classDialogVisible.value = true
}

async function saveClass(): Promise<void> {
  if (!classForm.name.trim()) {
    ElMessage.warning('请输入班级名称')
    return
  }
  classSaving.value = true
  try {
    const payload = {
      name: classForm.name.trim(),
      description: classForm.description.trim(),
    }
    const saved = editingClass.value && selected.value
      ? await updateClass(selected.value.id, payload)
      : await createClass(payload)
    classDialogVisible.value = false
    ElMessage.success(editingClass.value ? '班级信息已更新' : '班级已创建')
    await loadClasses(saved.id)
  } catch (error) {
    ElMessage.error(error instanceof Error ? error.message : '班级保存失败')
  } finally {
    classSaving.value = false
  }
}

async function copyInvite(): Promise<void> {
  if (!selected.value?.inviteCode) return
  await navigator.clipboard.writeText(selected.value.inviteCode)
  ElMessage.success('邀请码已复制')
}

async function regenerateInvite(): Promise<void> {
  if (!selected.value) return
  try {
    await ElMessageBox.confirm('重新生成后旧邀请码立即失效，确定继续吗？', '重新生成邀请码', {
      type: 'warning',
    })
    selected.value = await regenerateClassInvite(selected.value.id)
    await loadClasses(selected.value.id)
    ElMessage.success('邀请码已重新生成')
  } catch (error) {
    if (error !== 'cancel') ElMessage.error(error instanceof Error ? error.message : '更新邀请码失败')
  }
}

async function toggleInvite(enabled: boolean): Promise<void> {
  if (!selected.value) return
  try {
    selected.value = await setClassInviteEnabled(selected.value.id, enabled)
    await loadClasses(selected.value.id)
  } catch (error) {
    ElMessage.error(error instanceof Error ? error.message : '邀请码状态更新失败')
  }
}

async function changeRole(member: ClassMember, role: 'ADMIN' | 'MEMBER'): Promise<void> {
  if (!selected.value) return
  try {
    await setClassMemberRole(selected.value.id, member.userId, role)
    ElMessage.success(role === 'ADMIN' ? '已设为班级管理员' : '已撤销管理员权限')
    await loadDetail(selected.value)
  } catch (error) {
    ElMessage.error(error instanceof Error ? error.message : '成员角色更新失败')
  }
}

async function removeMember(member: ClassMember): Promise<void> {
  if (!selected.value) return
  try {
    await ElMessageBox.confirm(
      `确定将“${member.nickname}”移出班级吗？`,
      '移除成员',
      { type: 'warning' },
    )
    await removeClassMember(selected.value.id, member.userId)
    ElMessage.success('成员已移除')
    await loadDetail(selected.value)
  } catch (error) {
    if (error !== 'cancel') ElMessage.error(error instanceof Error ? error.message : '移除成员失败')
  }
}

async function transferOwner(member: ClassMember): Promise<void> {
  if (!selected.value) return
  try {
    await ElMessageBox.confirm(
      `确定将班级所有权转让给“${member.nickname}”吗？转让后你将成为普通成员。`,
      '转让班级',
      { type: 'warning', confirmButtonText: '确认转让' },
    )
    await transferClassOwnership(selected.value.id, member.userId)
    ElMessage.success('班级所有权已转让')
    await loadClasses(selected.value.id)
  } catch (error) {
    if (error !== 'cancel') ElMessage.error(error instanceof Error ? error.message : '转让失败')
  }
}

function openAnnouncement(item?: ClassAnnouncement): void {
  editingAnnouncementId.value = item?.id
  announcementForm.title = item?.title ?? ''
  announcementForm.body = item?.body ?? ''
  announcementForm.pinned = item?.pinned ?? false
  announcementDialogVisible.value = true
}

async function saveAnnouncement(): Promise<void> {
  if (!selected.value || !announcementForm.title.trim() || !announcementForm.body.trim()) {
    ElMessage.warning('请填写公告标题和内容')
    return
  }
  announcementSaving.value = true
  try {
    const payload = {
      title: announcementForm.title.trim(),
      body: announcementForm.body.trim(),
      pinned: announcementForm.pinned,
    }
    if (editingAnnouncementId.value) {
      await updateClassAnnouncement(selected.value.id, editingAnnouncementId.value, payload)
    } else {
      await createClassAnnouncement(selected.value.id, payload)
    }
    announcementDialogVisible.value = false
    announcements.value = await listClassAnnouncements(selected.value.id)
    ElMessage.success(editingAnnouncementId.value ? '公告已更新' : '公告已发布')
  } catch (error) {
    ElMessage.error(error instanceof Error ? error.message : '公告保存失败')
  } finally {
    announcementSaving.value = false
  }
}

async function removeAnnouncement(item: ClassAnnouncement): Promise<void> {
  if (!selected.value) return
  try {
    await ElMessageBox.confirm(`确定删除公告“${item.title}”吗？`, '删除公告', {
      type: 'warning',
    })
    await deleteClassAnnouncement(selected.value.id, item.id)
    announcements.value = await listClassAnnouncements(selected.value.id)
  } catch (error) {
    if (error !== 'cancel') ElMessage.error(error instanceof Error ? error.message : '公告删除失败')
  }
}

async function dissolve(): Promise<void> {
  if (!selected.value) return
  try {
    await ElMessageBox.confirm(
      `解散“${selected.value.name}”后，成员将失去班级资源访问权。确定继续吗？`,
      '解散班级',
      { type: 'warning', confirmButtonText: '确认解散' },
    )
    await archiveClass(selected.value.id)
    selected.value = undefined
    ElMessage.success('班级已解散')
    await loadClasses()
  } catch (error) {
    if (error !== 'cancel') ElMessage.error(error instanceof Error ? error.message : '解散班级失败')
  }
}

function canEditAnnouncement(item: ClassAnnouncement): boolean {
  return isOwner.value || item.authorId === currentUserId.value
}

onMounted(() => loadClasses())
</script>

<template>
  <section class="management-page">
    <div class="page-container">
      <SectionPageHeader
        eyebrow="CLASS MANAGEMENT"
        title="班级管理"
        description="创建班级、管理成员与邀请码，并向班级发布公告"
      >
        <el-button type="primary" @click="openCreate">新建班级</el-button>
      </SectionPageHeader>

      <div v-loading="loading" class="management-layout">
        <aside class="class-list">
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
          <el-empty v-if="classes.length === 0" :image-size="70" description="暂无可管理班级" />
        </aside>

        <main v-if="selected" v-loading="detailLoading" class="management-detail">
          <header>
            <div><el-tag>{{ roleLabels[selected.currentRole] }}</el-tag><h2>{{ selected.name }}</h2><p>{{ selected.description || '暂无介绍' }}</p></div>
            <div v-if="isOwner" class="header-actions">
              <el-button @click="openEdit">编辑信息</el-button>
              <el-button type="danger" plain @click="dissolve">解散班级</el-button>
            </div>
          </header>

          <section v-if="isOwner" class="invite-card">
            <div><span>班级邀请码</span><strong>{{ selected.inviteCode }}</strong><small>{{ selected.inviteEnabled ? '当前可用' : '当前已停用' }}</small></div>
            <el-button @click="copyInvite">复制</el-button>
            <el-button @click="regenerateInvite">重新生成</el-button>
            <el-switch :model-value="selected.inviteEnabled" active-text="允许加入" @change="toggleInvite(Boolean($event))" />
          </section>

          <el-tabs v-model="activeTab">
            <el-tab-pane label="成员管理" name="members">
              <div class="member-toolbar">
                <el-input v-model="memberKeyword" clearable placeholder="搜索用户名或昵称" @keyup.enter="selected && loadDetail(selected)" />
                <el-button @click="selected && loadDetail(selected)">查询</el-button>
              </div>
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
                <el-table-column label="角色" width="110"><template #default="{ row }">{{ roleLabels[row.role as keyof typeof roleLabels] }}</template></el-table-column>
                <el-table-column prop="joinedAt" label="加入时间" min-width="170" />
                <el-table-column label="操作" width="310" fixed="right">
                  <template #default="{ row }">
                    <template v-if="row.role !== 'OWNER'">
                      <el-button v-if="isOwner && row.role === 'MEMBER'" link type="primary" @click="changeRole(row as ClassMember, 'ADMIN')">设为管理员</el-button>
                      <el-button v-if="isOwner && row.role === 'ADMIN'" link type="warning" @click="changeRole(row as ClassMember, 'MEMBER')">撤销管理员</el-button>
                      <el-button v-if="isOwner" link type="success" @click="transferOwner(row as ClassMember)">转让班级</el-button>
                      <el-button v-if="isOwner || row.role === 'MEMBER'" link type="danger" @click="removeMember(row as ClassMember)">移除</el-button>
                    </template>
                  </template>
                </el-table-column>
              </el-table>
            </el-tab-pane>

            <el-tab-pane label="班级公告" name="announcements">
              <el-button type="primary" class="new-announcement" @click="openAnnouncement()">发布公告</el-button>
              <article v-for="item in announcements" :key="item.id" class="announcement">
                <div>
                  <span class="announcement-meta">
                    <RouterLink :to="`/users/${item.authorId}`" class="announcement-author">
                      <el-avatar :size="32" :src="item.authorAvatarUrl">{{ item.authorName.slice(0, 1) }}</el-avatar>
                      {{ item.authorName }}
                    </RouterLink>
                    <el-tag v-if="item.pinned" type="danger" size="small">置顶</el-tag>
                    {{ item.createdAt }}
                  </span>
                  <div v-if="canEditAnnouncement(item)"><el-button link type="primary" @click="openAnnouncement(item)">编辑</el-button><el-button link type="danger" @click="removeAnnouncement(item)">删除</el-button></div>
                </div>
                <h3>{{ item.title }}</h3>
                <MarkdownRenderer :source="item.body" />
              </article>
              <el-empty v-if="announcements.length === 0" description="暂无公告" />
            </el-tab-pane>
          </el-tabs>
        </main>
        <div v-else class="empty-detail"><el-empty description="请新建或选择班级" /></div>
      </div>
    </div>

    <el-dialog v-model="classDialogVisible" :title="editingClass ? '编辑班级' : '新建班级'" width="min(540px, 94vw)">
      <el-form label-position="top">
        <el-form-item label="班级名称" required><el-input v-model="classForm.name" maxlength="150" /></el-form-item>
        <el-form-item label="班级介绍"><el-input v-model="classForm.description" type="textarea" :rows="4" maxlength="1000" /></el-form-item>
      </el-form>
      <template #footer><el-button @click="classDialogVisible = false">取消</el-button><el-button type="primary" :loading="classSaving" @click="saveClass">保存</el-button></template>
    </el-dialog>

    <el-dialog v-model="announcementDialogVisible" :title="editingAnnouncementId ? '编辑公告' : '发布公告'" width="min(680px, 94vw)">
      <el-form label-position="top">
        <el-form-item label="公告标题" required><el-input v-model="announcementForm.title" maxlength="200" /></el-form-item>
        <el-form-item label="公告内容（支持 Markdown）" required><el-input v-model="announcementForm.body" type="textarea" :rows="10" maxlength="20000" show-word-limit /></el-form-item>
        <el-switch v-model="announcementForm.pinned" active-text="置顶公告" />
      </el-form>
      <template #footer><el-button @click="announcementDialogVisible = false">取消</el-button><el-button type="primary" :loading="announcementSaving" @click="saveAnnouncement">保存公告</el-button></template>
    </el-dialog>
  </section>
</template>

<style scoped>
.management-page { min-height: calc(100vh - 145px); padding: 48px 0 76px; background: #f6f8fc; }.management-layout { display: grid; min-height: 580px; gap: 20px; grid-template-columns: 280px minmax(0, 1fr); }.class-list, .management-detail, .empty-detail { border: 1px solid var(--lp-border); border-radius: 18px; background: #fff; box-shadow: var(--lp-shadow); }.class-list { align-self: start; padding: 10px; }.class-list button { display: block; width: 100%; border: 0; border-radius: 10px; background: transparent; cursor: pointer; padding: 14px; text-align: left; }.class-list button:hover, .class-list button.active { color: var(--lp-primary); background: #eff6ff; }.class-list strong, .class-list span { display: block; }.class-list span { margin-top: 5px; color: var(--lp-text-secondary); font-size: 12px; }.management-detail { overflow: hidden; padding: 26px; }.management-detail > header { display: flex; align-items: flex-start; justify-content: space-between; gap: 20px; border-bottom: 1px solid var(--lp-border); padding-bottom: 20px; }.management-detail h2 { margin: 10px 0 5px; }.management-detail header p { margin: 0; color: var(--lp-text-secondary); }.header-actions { display: flex; }.invite-card { display: flex; border: 1px solid #cfe0ff; border-radius: 14px; align-items: center; gap: 10px; margin: 20px 0; background: #f5f8ff; padding: 16px; }.invite-card > div { min-width: 0; flex: 1; }.invite-card span, .invite-card strong, .invite-card small { display: block; }.invite-card strong { margin: 4px 0; color: var(--lp-primary); font-size: 21px; letter-spacing: .12em; }.invite-card small { color: var(--lp-text-secondary); }.member-toolbar { display: grid; max-width: 560px; gap: 10px; margin-bottom: 14px; grid-template-columns: 1fr auto; }.new-announcement { margin-bottom: 14px; }.announcement { border: 1px solid var(--lp-border); border-radius: 14px; margin-bottom: 14px; padding: 20px; }.announcement > div { display: flex; align-items: center; justify-content: space-between; color: var(--lp-text-secondary); font-size: 12px; }.announcement h3 { margin: 14px 0 8px; }.empty-detail { display: grid; place-items: center; }
.announcement-meta, .announcement-author { display: flex; align-items: center; gap: 8px; }.announcement-author { color: var(--lp-text); font-weight: 700; }
@media (max-width: 850px) { .management-layout { grid-template-columns: 1fr; }.class-list { display: flex; overflow-x: auto; }.class-list button { min-width: 210px; }.management-detail > header, .invite-card { align-items: flex-start; flex-direction: column; } }
</style>
