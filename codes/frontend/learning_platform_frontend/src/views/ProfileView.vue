<script setup lang="ts">
import { Camera, User } from '@element-plus/icons-vue'
import type { FormInstance, FormRules } from 'element-plus'
import { ElMessage } from 'element-plus'
import { computed, reactive, ref, watch } from 'vue'

import { useAuthStore } from '@/stores/auth'

interface ProfileForm {
  nickname: string
  avatarUrl: string
  email: string
  phone: string
  gender: string
  bio: string
}

const authStore = useAuthStore()
const formRef = ref<FormInstance>()
const form = reactive<ProfileForm>({
  nickname: '',
  avatarUrl: '',
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
  avatarUrl: [{ max: 512, message: '头像地址不能超过 512 个字符', trigger: 'blur' }],
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
  form.avatarUrl = user.avatarUrl ?? ''
  form.email = user.email ?? ''
  form.phone = user.phone ?? ''
  form.gender = user.gender ?? 'UNKNOWN'
  form.bio = user.bio ?? ''
}

function errorMessage(error: unknown): string {
  return error instanceof Error ? error.message : '保存失败，请稍后重试'
}

async function submit(): Promise<void> {
  if (!formRef.value || !(await formRef.value.validate())) return

  try {
    await authStore.updateProfile({
      nickname: form.nickname.trim(),
      avatarUrl: form.avatarUrl.trim(),
      email: form.email.trim(),
      phone: form.phone.trim(),
      gender: form.gender,
      bio: form.bio.trim(),
    })
    ElMessage.success('个人资料已保存')
  } catch (error: unknown) {
    ElMessage.error(errorMessage(error))
  }
}

watch(() => authStore.user, fillForm, { immediate: true })
</script>

<template>
  <section class="profile-page">
    <div class="page-container">
      <header class="page-header">
        <div>
          <p>ACCOUNT SETTINGS</p>
          <h1>个人中心</h1>
          <span>管理你的公开资料与联系方式</span>
        </div>
      </header>

      <div class="profile-layout">
        <aside class="identity-card">
          <el-avatar :size="92" :src="form.avatarUrl || undefined">{{ avatarText }}</el-avatar>
          <h2>{{ form.nickname || authStore.user?.username }}</h2>
          <p>@{{ authStore.user?.username }}</p>
          <div class="role-list">
            <el-tag v-for="role in authStore.user?.roles ?? []" :key="role" effect="light" round>
              {{ role }}
            </el-tag>
          </div>
          <div class="identity-tip">
            <el-icon><Camera /></el-icon>
            在右侧填写图片地址即可更新头像
          </div>
        </aside>

        <el-form
          ref="formRef"
          class="profile-form"
          :model="form"
          :rules="rules"
          label-position="top"
          @submit.prevent="submit"
        >
          <div class="section-heading">
            <el-icon><User /></el-icon>
            <div><h2>基本资料</h2><p>这些信息将用于学习记录与账户识别</p></div>
          </div>

          <div class="form-grid">
            <el-form-item label="用户名">
              <el-input :model-value="authStore.user?.username" disabled />
            </el-form-item>
            <el-form-item label="昵称" prop="nickname">
              <el-input v-model="form.nickname" placeholder="请输入昵称" />
            </el-form-item>
            <el-form-item label="邮箱" prop="email">
              <el-input v-model="form.email" placeholder="name@example.com" />
            </el-form-item>
            <el-form-item label="手机号" prop="phone">
              <el-input v-model="form.phone" placeholder="请输入手机号" />
            </el-form-item>
            <el-form-item label="性别" prop="gender">
              <el-select v-model="form.gender" placeholder="请选择" style="width: 100%">
                <el-option label="暂不透露" value="UNKNOWN" />
                <el-option label="男" value="MALE" />
                <el-option label="女" value="FEMALE" />
              </el-select>
            </el-form-item>
            <el-form-item label="头像地址" prop="avatarUrl">
              <el-input v-model="form.avatarUrl" placeholder="https://example.com/avatar.png" />
            </el-form-item>
          </div>
          <el-form-item label="个人简介" prop="bio">
            <el-input
              v-model="form.bio"
              maxlength="500"
              :rows="4"
              show-word-limit
              type="textarea"
              placeholder="介绍一下你的学习方向与目标"
            />
          </el-form-item>
          <div class="form-actions">
            <el-button @click="fillForm">恢复修改</el-button>
            <el-button type="primary" native-type="submit" :loading="authStore.loading">保存资料</el-button>
          </div>
        </el-form>
      </div>
    </div>
  </section>
</template>

<style scoped>
.profile-page { min-height: calc(100vh - 145px); padding: 56px 0 78px; background: linear-gradient(180deg, #f5f8ff, #f8f9fc 300px); }
.page-header { display: flex; align-items: flex-end; justify-content: space-between; margin-bottom: 30px; }
.page-header p { margin: 0 0 7px; color: var(--lp-primary); font-size: 12px; font-weight: 700; letter-spacing: 0.14em; }
.page-header h1 { margin: 0; font-size: 36px; letter-spacing: -0.03em; }
.page-header span { display: block; margin-top: 9px; color: var(--lp-text-secondary); }
.profile-layout { display: grid; align-items: start; gap: 24px; grid-template-columns: 280px 1fr; }
.identity-card, .profile-form { border: 1px solid var(--lp-border); border-radius: 20px; background: #fff; box-shadow: var(--lp-shadow); }
.identity-card { display: flex; align-items: center; flex-direction: column; padding: 34px 24px 26px; text-align: center; }
.identity-card h2 { margin: 18px 0 4px; font-size: 20px; }
.identity-card > p { margin: 0; color: var(--lp-text-secondary); font-size: 13px; }
.role-list { display: flex; flex-wrap: wrap; justify-content: center; gap: 6px; margin-top: 16px; }
.identity-tip { display: flex; align-items: flex-start; gap: 7px; border-top: 1px solid var(--lp-border); margin-top: 24px; color: #98a2b3; font-size: 12px; line-height: 1.6; padding-top: 20px; text-align: left; }
.profile-form { padding: 30px; }
.section-heading { display: flex; align-items: center; gap: 12px; border-bottom: 1px solid var(--lp-border); margin-bottom: 24px; padding-bottom: 20px; }
.section-heading > .el-icon { width: 40px; height: 40px; border-radius: 11px; color: var(--lp-primary); background: #eff6ff; font-size: 19px; }
.section-heading h2 { margin: 0; font-size: 18px; }
.section-heading p { margin: 5px 0 0; color: var(--lp-text-secondary); font-size: 13px; }
.form-grid { display: grid; gap: 0 20px; grid-template-columns: repeat(2, minmax(0, 1fr)); }
.form-actions { display: flex; justify-content: flex-end; gap: 10px; padding-top: 8px; }
@media (max-width: 800px) { .profile-layout { grid-template-columns: 1fr; } .identity-card { align-items: flex-start; text-align: left; } .role-list { justify-content: flex-start; } }
@media (max-width: 560px) { .profile-page { padding-top: 36px; } .form-grid { grid-template-columns: 1fr; } .profile-form { padding: 22px 18px; } }
</style>
