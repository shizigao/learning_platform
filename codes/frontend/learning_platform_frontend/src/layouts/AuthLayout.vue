<script setup lang="ts">
import { ArrowLeft, Lock, Reading } from '@element-plus/icons-vue'

import AppLogo from '@/components/AppLogo.vue'
</script>

<template>
  <div class="auth-layout">
    <RouterLink class="back-home" to="/">
      <el-icon><ArrowLeft /></el-icon>
      返回首页
    </RouterLink>

    <aside class="auth-intro">
      <div class="intro-content">
        <AppLogo />
        <div class="intro-icon"><el-icon><Reading /></el-icon></div>
        <p class="eyebrow">学习 · 考试 · AI 助手</p>
        <h1>让成长的每一步<br />都有清晰记录</h1>
        <p class="intro-description">
          登录智学云考，继续你的学习计划，随时查看课程、考试与个人成长数据。
        </p>
        <div class="privacy-note">
          <el-icon><Lock /></el-icon>
          <span>账户信息经过安全加密保护</span>
        </div>
      </div>
    </aside>

    <main class="auth-main">
      <div class="mobile-brand"><AppLogo /></div>
      <div class="auth-card"><RouterView /></div>
      <p class="copyright">© 2026 智学云考 · 智能在线学习考试平台</p>
    </main>
  </div>
</template>

<style scoped>
.auth-layout {
  display: grid;
  min-height: 100vh;
  background: #f7f9fc;
  grid-template-columns: minmax(380px, 44%) 1fr;
}

.back-home {
  position: fixed;
  z-index: 3;
  top: 28px;
  right: 32px;
  display: inline-flex;
  align-items: center;
  gap: 6px;
  color: var(--lp-text-secondary);
  font-size: 14px;
  transition: color 160ms ease;
}

.back-home:hover {
  color: var(--lp-primary);
}

.auth-intro {
  position: relative;
  display: flex;
  overflow: hidden;
  align-items: center;
  padding: 72px clamp(44px, 7vw, 110px);
  color: var(--lp-text);
  background:
    radial-gradient(circle at 15% 85%, rgb(147 197 253 / 32%), transparent 31%),
    radial-gradient(circle at 85% 10%, rgb(196 181 253 / 28%), transparent 30%),
    linear-gradient(145deg, #eef5ff, #f4f4ff);
}

.auth-intro::after {
  position: absolute;
  right: -100px;
  bottom: -120px;
  width: 340px;
  height: 340px;
  border: 1px solid rgb(37 99 235 / 10%);
  border-radius: 50%;
  content: "";
}

.intro-content {
  position: relative;
  z-index: 1;
  max-width: 500px;
}

.intro-icon {
  display: grid;
  width: 62px;
  height: 62px;
  border-radius: 18px;
  margin-top: 84px;
  color: #fff;
  background: linear-gradient(145deg, #60a5fa, #2563eb);
  box-shadow: 0 18px 36px rgb(37 99 235 / 25%);
  font-size: 28px;
  place-items: center;
}

.eyebrow {
  margin: 24px 0 12px;
  color: var(--lp-primary);
  font-size: 13px;
  font-weight: 700;
  letter-spacing: 0.12em;
}

h1 {
  margin: 0;
  font-size: clamp(38px, 4vw, 58px);
  line-height: 1.16;
  letter-spacing: -0.045em;
}

.intro-description {
  max-width: 440px;
  margin: 24px 0 0;
  color: var(--lp-text-secondary);
  font-size: 16px;
  line-height: 1.8;
}

.privacy-note {
  display: inline-flex;
  align-items: center;
  gap: 8px;
  margin-top: 42px;
  color: #667085;
  font-size: 13px;
}

.auth-main {
  display: flex;
  min-width: 0;
  align-items: center;
  justify-content: center;
  flex-direction: column;
  padding: 88px 28px 28px;
}

.auth-card {
  width: min(460px, 100%);
  border: 1px solid rgb(228 231 236 / 85%);
  border-radius: 24px;
  background: #fff;
  box-shadow: 0 24px 70px rgb(16 24 40 / 9%);
  padding: clamp(28px, 4vw, 44px);
}

.mobile-brand {
  display: none;
  margin-bottom: 28px;
}

.copyright {
  margin: 28px 0 0;
  color: #98a2b3;
  font-size: 12px;
}

@media (max-width: 860px) {
  .auth-layout {
    grid-template-columns: 1fr;
  }

  .auth-intro {
    display: none;
  }

  .mobile-brand {
    display: block;
  }
}

@media (max-width: 520px) {
  .auth-main {
    justify-content: flex-start;
    padding: 76px 16px 24px;
  }

  .auth-card {
    border-radius: 18px;
    padding: 26px 20px;
  }

  .back-home {
    top: 22px;
    right: 20px;
  }
}
</style>
