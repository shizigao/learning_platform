<script setup lang="ts">
import { ArrowLeft, Tools } from '@element-plus/icons-vue'
import { computed } from 'vue'
import { useRoute } from 'vue-router'

const route = useRoute()
const title = computed(() => typeof route.meta.title === 'string' ? route.meta.title : '功能中心')
const description = computed(() =>
  typeof route.meta.description === 'string'
    ? route.meta.description
    : '该功能正在紧锣密鼓地建设中，敬请期待。',
)
</script>

<template>
  <section class="placeholder-page">
    <div class="placeholder-decoration" />
    <div class="placeholder-card">
      <div class="icon-box"><el-icon><Tools /></el-icon></div>
      <el-tag type="primary" effect="light" round>即将开放</el-tag>
      <h1>{{ title }}</h1>
      <p>{{ description }}</p>
      <el-button type="primary" size="large" @click="$router.push('/')">
        <el-icon class="el-icon--left"><ArrowLeft /></el-icon>
        返回首页
      </el-button>
    </div>
  </section>
</template>

<style scoped>
.placeholder-page {
  position: relative;
  display: grid;
  overflow: hidden;
  min-height: calc(100vh - 145px);
  background: linear-gradient(145deg, #f8fbff, #f2f5ff);
  padding: 70px 20px;
  place-items: center;
}

.placeholder-decoration {
  position: absolute;
  top: -180px;
  right: -140px;
  width: 480px;
  height: 480px;
  border-radius: 50%;
  background: rgb(96 165 250 / 12%);
}

.placeholder-card {
  position: relative;
  z-index: 1;
  width: min(560px, 100%);
  border: 1px solid rgb(255 255 255 / 85%);
  border-radius: 28px;
  background: rgb(255 255 255 / 88%);
  box-shadow: 0 30px 80px rgb(29 78 216 / 12%);
  padding: 54px 40px;
  text-align: center;
  backdrop-filter: blur(16px);
}

.icon-box {
  display: grid;
  width: 74px;
  height: 74px;
  border-radius: 22px;
  margin: 0 auto 24px;
  color: #fff;
  background: linear-gradient(145deg, #60a5fa, #2563eb);
  box-shadow: 0 16px 34px rgb(37 99 235 / 25%);
  font-size: 31px;
  place-items: center;
}

h1 {
  margin: 18px 0 0;
  font-size: clamp(30px, 5vw, 42px);
  letter-spacing: -0.035em;
}

p {
  max-width: 430px;
  margin: 16px auto 30px;
  color: var(--lp-text-secondary);
  font-size: 16px;
  line-height: 1.75;
}
</style>
