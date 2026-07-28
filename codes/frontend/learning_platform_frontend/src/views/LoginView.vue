<script setup lang="ts">
import { Lock, User } from '@element-plus/icons-vue'
import type { FormInstance, FormRules } from 'element-plus'
import { ElMessage } from 'element-plus'
import { computed, onMounted, reactive, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'

import { useAuthStore } from '@/stores/auth'

interface LoginForm {
  username: string
  password: string
}

const route = useRoute()
const router = useRouter()
const authStore = useAuthStore()
const formRef = ref<FormInstance>()
const form = reactive<LoginForm>({
  username: '',
  password: '',
})

const rules: FormRules<LoginForm> = {
  username: [{ required: true, message: '请输入用户名', trigger: 'blur' }],
  password: [{ required: true, message: '请输入密码', trigger: 'blur' }],
}

const registered = computed(() => route.query.registered === '1')

function errorMessage(error: unknown): string {
  return error instanceof Error ? error.message : '登录失败，请稍后重试'
}

async function submit(): Promise<void> {
  if (!formRef.value || !(await formRef.value.validate())) return

  try {
    // 发送登录请求，点击login
    await authStore.login({
      username: form.username.trim(),
      password: form.password,
    })
    ElMessage.success('登录成功，欢迎回来')
    const redirect = typeof route.query.redirect === 'string' ? route.query.redirect : '/'
    await router.push(redirect)
  } catch (error: unknown) {
    ElMessage.error(errorMessage(error))
  }
}

onMounted(() => {
  if (typeof route.query.username === 'string') {
    form.username = route.query.username
  }
})
</script>

<template>
  <section>
    <header class="form-heading">
      <p>欢迎回来</p>
      <h2>登录你的账户</h2>
      <span>继续探索专属于你的学习空间</span>
    </header>

    <el-alert
      v-if="registered"
      class="registered-alert"
      title="注册成功，请使用新账户登录"
      type="success"
      :closable="false"
      show-icon
    />

    <!-- 发送登录请求，点击submit -->
    <el-form ref="formRef" :model="form" :rules="rules" label-position="top" @submit.prevent="submit">
      <el-form-item label="用户名" prop="username">
        <el-input
          v-model="form.username"
          :prefix-icon="User"
          autocomplete="username"
          placeholder="请输入用户名"
          size="large"
        />
      </el-form-item>
      <el-form-item label="密码" prop="password">
        <el-input
          v-model="form.password"
          :prefix-icon="Lock"
          autocomplete="current-password"
          placeholder="请输入密码"
          show-password
          size="large"
          type="password"
          @keyup.enter="submit"
        />
      </el-form-item>
      <el-button
        class="submit-button"
        type="primary"
        size="large"
        native-type="submit"
        :loading="authStore.loading"
      >
        登录
      </el-button>
    </el-form>

    <p class="switch-form">
      还没有账户？
      <RouterLink to="/register">立即注册</RouterLink>
    </p>
  </section>
</template>

<style scoped>
.form-heading {
  margin-bottom: 30px;
}

.form-heading p {
  margin: 0 0 8px;
  color: var(--lp-primary);
  font-size: 13px;
  font-weight: 700;
}

.form-heading h2 {
  margin: 0;
  font-size: 29px;
  letter-spacing: -0.025em;
}

.form-heading span {
  display: block;
  margin-top: 10px;
  color: var(--lp-text-secondary);
  font-size: 14px;
}

.registered-alert {
  margin: -8px 0 22px;
}

.submit-button {
  width: 100%;
  margin-top: 8px;
}

.switch-form {
  margin: 25px 0 0;
  color: var(--lp-text-secondary);
  font-size: 14px;
  text-align: center;
}

.switch-form a {
  color: var(--lp-primary);
  font-weight: 600;
}
</style>
