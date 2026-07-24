<script setup lang="ts">
import { Lock, Message, User } from '@element-plus/icons-vue'
import type { FormInstance, FormRules } from 'element-plus'
import { ElMessage } from 'element-plus'
import { reactive, ref } from 'vue'
import { useRouter } from 'vue-router'

import { useAuthStore } from '@/stores/auth'

interface RegisterForm {
  username: string
  nickname: string
  email: string
  phone: string
  password: string
  confirmPassword: string
}

const router = useRouter()
const authStore = useAuthStore()
const formRef = ref<FormInstance>()
const form = reactive<RegisterForm>({
  username: '',
  nickname: '',
  email: '',
  phone: '',
  password: '',
  confirmPassword: '',
})

const rules: FormRules<RegisterForm> = {
  username: [
    { required: true, message: '请输入用户名', trigger: 'blur' },
    { pattern: /^[A-Za-z0-9_]{4,32}$/, message: '请输入 4–32 位字母、数字或下划线', trigger: 'blur' },
  ],
  nickname: [
    { required: true, message: '请输入昵称', trigger: 'blur' },
    { max: 64, message: '昵称不能超过 64 个字符', trigger: 'blur' },
  ],
  email: [{ type: 'email', message: '请输入正确的邮箱地址', trigger: 'blur' }],
  phone: [{ pattern: /^\+?[0-9]{6,20}$/, message: '请输入正确的手机号', trigger: 'blur' }],
  password: [
    { required: true, message: '请输入密码', trigger: 'blur' },
    { min: 8, max: 64, message: '密码长度应为 8–64 位', trigger: 'blur' },
  ],
  confirmPassword: [
    { required: true, message: '请再次输入密码', trigger: 'blur' },
    {
      validator: (_rule, value: string, callback) => {
        if (value !== form.password) callback(new Error('两次输入的密码不一致'))
        else callback()
      },
      trigger: 'blur',
    },
  ],
}

function errorMessage(error: unknown): string {
  return error instanceof Error ? error.message : '注册失败，请稍后重试'
}

async function submit(): Promise<void> {
  if (!formRef.value || !(await formRef.value.validate())) return

  const username = form.username.trim()
  try {
    await authStore.register({
      username,
      password: form.password,
      nickname: form.nickname.trim(),
      email: form.email.trim(),
      phone: form.phone.trim(),
    })
    ElMessage.success('注册成功')
    await router.push({ path: '/login', query: { registered: '1', username } })
  } catch (error: unknown) {
    ElMessage.error(errorMessage(error))
  }
}
</script>

<template>
  <section>
    <header class="form-heading">
      <p>加入智学云考</p>
      <h2>创建学习账户</h2>
      <span>几步即可开启你的个性化学习旅程</span>
    </header>

    <el-form ref="formRef" :model="form" :rules="rules" label-position="top" @submit.prevent="submit">
      <div class="form-grid">
        <el-form-item label="用户名" prop="username">
          <el-input v-model="form.username" :prefix-icon="User" autocomplete="username" placeholder="4–32 位" />
        </el-form-item>
        <el-form-item label="昵称" prop="nickname">
          <el-input v-model="form.nickname" :prefix-icon="User" placeholder="如何称呼你" />
        </el-form-item>
      </div>
      <el-form-item label="邮箱（选填）" prop="email">
        <el-input v-model="form.email" :prefix-icon="Message" autocomplete="email" placeholder="name@example.com" />
      </el-form-item>
      <el-form-item label="手机号（选填）" prop="phone">
        <el-input v-model="form.phone" autocomplete="tel" placeholder="仅支持数字，可包含 + 前缀" />
      </el-form-item>
      <div class="form-grid">
        <el-form-item label="密码" prop="password">
          <el-input
            v-model="form.password"
            :prefix-icon="Lock"
            autocomplete="new-password"
            placeholder="至少 8 位"
            show-password
            type="password"
          />
        </el-form-item>
        <el-form-item label="确认密码" prop="confirmPassword">
          <el-input
            v-model="form.confirmPassword"
            :prefix-icon="Lock"
            autocomplete="new-password"
            placeholder="再次输入"
            show-password
            type="password"
            @keyup.enter="submit"
          />
        </el-form-item>
      </div>
      <el-button
        class="submit-button"
        type="primary"
        size="large"
        native-type="submit"
        :loading="authStore.loading"
      >
        创建账户
      </el-button>
    </el-form>

    <p class="switch-form">
      已有账户？
      <RouterLink to="/login">直接登录</RouterLink>
    </p>
  </section>
</template>

<style scoped>
.form-heading { margin-bottom: 26px; }
.form-heading p { margin: 0 0 8px; color: var(--lp-primary); font-size: 13px; font-weight: 700; }
.form-heading h2 { margin: 0; font-size: 29px; letter-spacing: -0.025em; }
.form-heading span { display: block; margin-top: 10px; color: var(--lp-text-secondary); font-size: 14px; }
.form-grid { display: grid; gap: 14px; grid-template-columns: 1fr 1fr; }
.submit-button { width: 100%; margin-top: 6px; }
.switch-form { margin: 22px 0 0; color: var(--lp-text-secondary); font-size: 14px; text-align: center; }
.switch-form a { color: var(--lp-primary); font-weight: 600; }
@media (max-width: 520px) { .form-grid { gap: 0; grid-template-columns: 1fr; } }
</style>
