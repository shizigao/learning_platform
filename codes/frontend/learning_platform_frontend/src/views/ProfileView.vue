<script setup lang="ts">
import { Camera, Collection, Star, User, View } from '@element-plus/icons-vue'
import type { FormInstance, FormRules } from 'element-plus'
import { ElMessage, ElMessageBox } from 'element-plus'
import { computed, onMounted, reactive, ref, watch } from 'vue'

import {
  deleteCurrentUserAvatar,
  getPublicUser,
  listPublicUserContents,
  uploadCurrentUserAvatar,
} from '@/api/user'
import ContentCard from '@/components/ContentCard.vue'
import ThumbUpIcon from '@/components/ThumbUpIcon.vue'
import UserSearchPanel from '@/components/UserSearchPanel.vue'
import { useAuthStore } from '@/stores/auth'
import type { ContentSummary } from '@/types/content'
import type { PublicUserProfile } from '@/types/user'

interface ProfileForm {
  nickname: string
  email: string
  phone: string
  gender: string
  bio: string
}

const authStore = useAuthStore()
const formRef = ref<FormInstance>()
const avatarInput = ref<HTMLInputElement>()
const avatarUploading = ref(false)
const publicLoading = ref(false)
const publicProfile = ref<PublicUserProfile>()
const publicContents = ref<ContentSummary[]>([])
const form = reactive<ProfileForm>({
  nickname: '',
  email: '',
  phone: '',
  gender: 'UNKNOWN',
  bio: '',
})

const rules: FormRules<ProfileForm> = {
  nickname: [
    { required: true, message: '请输入昵称', trigger: 'blur' },
    { max: 64, message: '昵称不能超过 64 个字符', trigger: 'blur' },
  ],
  email: [{ type: 'email', message: '请输入正确的邮箱地址', trigger: 'blur' }],
  phone: [{ pattern: /^\+?[0-9]{6,20}$/, message: '请输入正确的手机号', trigger: 'blur' }],
  bio: [{ max: 500, message: '个人简介不能超过 500 个字符', trigger: 'blur' }],
}

const avatarText = computed(() => {
  const source = form.nickname || authStore.user?.username || 'U'
  return source.slice(0, 1).toUpperCase()
})

function fillForm(): void {
  const user = authStore.user
  if (!user) return
  form.nickname = user.nickname ?? ''
  form.email = user.email ?? ''
  form.phone = user.phone ?? ''
  form.gender = user.gender ?? 'UNKNOWN'
  form.bio = user.bio ?? ''
}

async function loadPublicData(): Promise<void> {
  const userId = authStore.user?.id
  if (!userId) return
  publicLoading.value = true
  try {
    ;[publicProfile.value, publicContents.value] = await Promise.all([
      getPublicUser(userId),
      listPublicUserContents(userId, 1, 12).then((page) => page.items),
    ])
  } catch (error) {
    ElMessage.error(error instanceof Error ? error.message : '公开资料加载失败')
  } finally {
    publicLoading.value = false
  }
}

async function submit(): Promise<void> {
  if (!formRef.value || !(await formRef.value.validate())) return
  try {
    await authStore.updateProfile({
      nickname: form.nickname.trim(),
      email: form.email.trim(),
      phone: form.phone.trim(),
      gender: form.gender,
      bio: form.bio.trim(),
    })
    await loadPublicData()
    ElMessage.success('个人资料已保存')
  } catch (error) {
    ElMessage.error(error instanceof Error ? error.message : '保存失败，请稍后重试')
  }
}

function chooseAvatar(): void {
  avatarInput.value?.click()
}

async function uploadAvatar(event: Event): Promise<void> {
  const input = event.target as HTMLInputElement
  const file = input.files?.[0]
  input.value = ''
  if (!file) return
  if (!['image/jpeg', 'image/png', 'image/webp'].includes(file.type)) {
    ElMessage.warning('头像仅支持 JPG、PNG、WebP 图片')
    return
  }
  if (file.size > 5 * 1024 * 1024) {
    ElMessage.warning('头像图片不能超过 5 MB')
    return
  }
  avatarUploading.value = true
  try {
    await uploadCurrentUserAvatar(file)
    await authStore.refreshProfile()
    await loadPublicData()
    ElMessage.success('头像已更新')
  } catch (error) {
    ElMessage.error(error instanceof Error ? error.message : '头像上传失败')
  } finally {
    avatarUploading.value = false
  }
}

async function removeAvatar(): Promise<void> {
  try {
    await ElMessageBox.confirm('确定删除当前头像吗？', '删除头像', { type: 'warning' })
    await deleteCurrentUserAvatar()
    await authStore.refreshProfile()
    await loadPublicData()
    ElMessage.success('头像已删除')
  } catch (error) {
    if (error !== 'cancel') {
      ElMessage.error(error instanceof Error ? error.message : '删除头像失败')
    }
  }
}

watch(() => authStore.user, fillForm, { immediate: true })
onMounted(loadPublicData)
</script>

<template>
  <section class="profile-page">
    <div class="page-container">
      <header class="page-header">
        <div><p>ACCOUNT SETTINGS</p><h1>个人中心</h1><span>管理账户信息，并查看你的公开发布成果</span></div>
      </header>

      <div class="profile-layout">
        <aside class="identity-card">
          <el-avatar :size="92" :src="authStore.user?.avatarUrl">{{ avatarText }}</el-avatar>
          <h2>{{ form.nickname || authStore.user?.username }}</h2>
          <p>@{{ authStore.user?.username }}</p>
          <div class="role-list"><el-tag v-for="role in authStore.user?.roles ?? []" :key="role" effect="light" round>{{ role }}</el-tag></div>
          <input ref="avatarInput" class="file-input" type="file" accept=".jpg,.jpeg,.png,.webp,image/jpeg,image/png,image/webp" @change="uploadAvatar" />
          <div class="avatar-actions">
            <el-button type="primary" plain :icon="Camera" :loading="avatarUploading" @click="chooseAvatar">上传头像</el-button>
            <el-button v-if="authStore.user?.avatarUrl" type="danger" link @click="removeAvatar">删除头像</el-button>
          </div>
          <p class="upload-tip">支持 JPG、PNG、WebP，大小不超过 5 MB</p>
        </aside>

        <el-form ref="formRef" class="profile-form" :model="form" :rules="rules" label-position="top" @submit.prevent="submit">
          <div class="section-heading"><el-icon><User /></el-icon><div><h2>基本资料</h2><p>邮箱和手机号仅本人可见，不会展示在公开个人中心</p></div></div>
          <div class="form-grid">
            <el-form-item label="用户名"><el-input :model-value="authStore.user?.username" disabled /></el-form-item>
            <el-form-item label="昵称" prop="nickname"><el-input v-model="form.nickname" placeholder="请输入昵称" /></el-form-item>
            <el-form-item label="邮箱" prop="email"><el-input v-model="form.email" placeholder="name@example.com" /></el-form-item>
            <el-form-item label="手机号" prop="phone"><el-input v-model="form.phone" placeholder="请输入手机号" /></el-form-item>
            <el-form-item label="性别" prop="gender">
              <el-select v-model="form.gender" placeholder="请选择" style="width: 100%">
                <el-option label="暂不透露" value="UNKNOWN" /><el-option label="男" value="MALE" /><el-option label="女" value="FEMALE" />
              </el-select>
            </el-form-item>
          </div>
          <el-form-item label="个人简介" prop="bio"><el-input v-model="form.bio" maxlength="500" :rows="4" show-word-limit type="textarea" placeholder="介绍一下你的学习方向与目标" /></el-form-item>
          <div class="form-actions"><el-button @click="fillForm">恢复修改</el-button><el-button type="primary" native-type="submit" :loading="authStore.loading">保存资料</el-button></div>
        </el-form>
      </div>

      <div v-if="publicProfile" class="stats-grid">
        <div><el-icon><Collection /></el-icon><strong>{{ publicProfile.statistics.contentCount }}</strong><span>公开资料</span></div>
        <div><el-icon><View /></el-icon><strong>{{ publicProfile.statistics.viewCount }}</strong><span>总浏览量</span></div>
        <div><el-icon><ThumbUpIcon /></el-icon><strong>{{ publicProfile.statistics.likeCount }}</strong><span>总点赞量</span></div>
        <div><el-icon><Star /></el-icon><strong>{{ publicProfile.statistics.favoriteCount }}</strong><span>总收藏量</span></div>
      </div>

      <section v-loading="publicLoading" class="public-content">
        <div class="content-heading"><div><h2>我发布的公开资料</h2><p>其他用户能够在你的公开个人中心看到这些资料</p></div><RouterLink v-if="authStore.user" :to="`/users/${authStore.user.id}`">查看公开主页 →</RouterLink></div>
        <div class="content-grid"><ContentCard v-for="item in publicContents" :key="item.id" :content="item" /></div>
        <el-empty v-if="!publicLoading && publicContents.length === 0" description="暂未发布公开资料" />
      </section>

      <UserSearchPanel />
    </div>
  </section>
</template>

<style scoped>
.profile-page { min-height: calc(100vh - 145px); padding: 56px 0 78px; background: linear-gradient(180deg, #f5f8ff, #f8f9fc 300px); }.page-header { margin-bottom: 30px; }.page-header p { margin: 0 0 7px; color: var(--lp-primary); font-size: 12px; font-weight: 700; letter-spacing: .14em; }.page-header h1 { margin: 0; font-size: 36px; }.page-header span { display: block; margin-top: 9px; color: var(--lp-text-secondary); }
.profile-layout { display: grid; align-items: start; gap: 24px; grid-template-columns: 280px 1fr; }.identity-card, .profile-form, .public-content { border: 1px solid var(--lp-border); border-radius: 20px; background: #fff; box-shadow: var(--lp-shadow); }.identity-card { display: flex; align-items: center; flex-direction: column; padding: 34px 24px 26px; text-align: center; }.identity-card h2 { margin: 18px 0 4px; }.identity-card > p { margin: 0; color: var(--lp-text-secondary); font-size: 13px; }.role-list { display: flex; flex-wrap: wrap; justify-content: center; gap: 6px; margin-top: 16px; }.file-input { display: none; }.avatar-actions { display: flex; align-items: center; flex-direction: column; gap: 5px; border-top: 1px solid var(--lp-border); width: 100%; margin-top: 22px; padding-top: 20px; }.avatar-actions .el-button + .el-button { margin-left: 0; }.identity-card .upload-tip { margin-top: 8px; color: #98a2b3; font-size: 11px; }
.profile-form { padding: 30px; }.section-heading { display: flex; align-items: center; gap: 12px; border-bottom: 1px solid var(--lp-border); margin-bottom: 24px; padding-bottom: 20px; }.section-heading > .el-icon { width: 40px; height: 40px; border-radius: 11px; color: var(--lp-primary); background: #eff6ff; font-size: 19px; }.section-heading h2 { margin: 0; font-size: 18px; }.section-heading p { margin: 5px 0 0; color: var(--lp-text-secondary); font-size: 13px; }.form-grid { display: grid; gap: 0 20px; grid-template-columns: repeat(2, minmax(0, 1fr)); }.form-actions { display: flex; justify-content: flex-end; gap: 10px; }
.stats-grid { display: grid; gap: 16px; margin-top: 24px; grid-template-columns: repeat(4, 1fr); }.stats-grid > div { display: grid; align-items: center; gap: 3px 12px; border: 1px solid var(--lp-border); border-radius: 16px; background: #fff; padding: 20px; grid-template-columns: auto 1fr; }.stats-grid .el-icon { width: 42px; height: 42px; border-radius: 12px; color: var(--lp-primary); background: #eef5ff; font-size: 20px; grid-row: 1 / 3; }.stats-grid strong { font-size: 24px; }.stats-grid span { color: var(--lp-text-secondary); font-size: 12px; }
.public-content { margin-top: 24px; padding: 28px; }.content-heading { display: flex; align-items: flex-end; justify-content: space-between; gap: 20px; margin-bottom: 20px; }.content-heading h2 { margin: 0; }.content-heading p { margin: 7px 0 0; color: var(--lp-text-secondary); }.content-heading a { color: var(--lp-primary); font-weight: 700; }.content-grid { display: grid; gap: 18px; grid-template-columns: repeat(auto-fill, minmax(250px, 1fr)); }
@media (max-width: 800px) { .profile-layout { grid-template-columns: 1fr; }.stats-grid { grid-template-columns: repeat(2, 1fr); } }
@media (max-width: 560px) { .form-grid, .stats-grid { grid-template-columns: 1fr; }.profile-form, .public-content { padding: 22px 18px; }.content-heading { align-items: flex-start; flex-direction: column; } }
</style>
