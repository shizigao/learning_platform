<script setup lang="ts">
import { ElMessage } from 'element-plus'
import { onMounted, ref } from 'vue'

import { listAssignedExams } from '@/api/exam'
import SectionPageHeader from '@/components/SectionPageHeader.vue'
import type { ExamStatus, ExamSummary } from '@/types/exam'

const statusLabels: Record<ExamStatus, string> = {
  DRAFT: '草稿',
  PUBLISHED: '已发布',
  ONGOING: '进行中',
  FINISHED: '已结束',
  CANCELLED: '已取消',
}
const loading = ref(false)
const exams = ref<ExamSummary[]>([])

function displayTime(value: string): string {
  return new Date(value).toLocaleString('zh-CN', { hour12: false })
}

async function load(): Promise<void> {
  loading.value = true
  try {
    exams.value = await listAssignedExams()
  } catch (error) {
    ElMessage.error(error instanceof Error ? error.message : '考试列表加载失败')
  } finally {
    loading.value = false
  }
}

onMounted(load)
</script>

<template>
  <section class="exam-center">
    <div class="page-container">
      <SectionPageHeader
        eyebrow="EXAM CENTER"
        title="考试中心"
        description="诚信考试 注意考试截止时间"
      >
        <RouterLink to="/exams/wrong-review">
          <el-button type="primary">错题复习</el-button>
        </RouterLink>
      </SectionPageHeader>
      <div v-loading="loading" class="exam-grid">
        <article v-for="exam in exams" :key="exam.id" class="exam-card">
          <div><el-tag type="success">{{ statusLabels[exam.status] }}</el-tag><span>{{ exam.durationMinutes }} 分钟</span></div>
          <h2>{{ exam.name }}</h2>
          <p>{{ displayTime(exam.startAt) }} — {{ displayTime(exam.endAt) }}</p>
          <footer>
            <span>及格分 {{ Number(exam.passingScore).toFixed(2) }}</span>
            <RouterLink :to="`/exams/${exam.id}`">查看考试 →</RouterLink>
          </footer>
        </article>
        <el-empty v-if="!loading && exams.length === 0" description="暂时没有为你安排的考试" />
      </div>
    </div>
  </section>
</template>

<style scoped>
.exam-center { min-height: calc(100vh - 145px); padding: 48px 0 76px; background: #f6f8fc; }
.exam-grid { display: grid; min-height: 260px; gap: 18px; grid-template-columns: repeat(auto-fit, minmax(300px, 1fr)); }
.exam-card { border: 1px solid var(--lp-border); border-radius: 18px; background: #fff; box-shadow: var(--lp-shadow); padding: 24px; }
.exam-card > div, .exam-card footer { display: flex; align-items: center; justify-content: space-between; color: var(--lp-text-secondary); font-size: 13px; }
.exam-card h2 { margin: 22px 0 10px; font-size: 21px; }.exam-card p { min-height: 46px; color: var(--lp-text-secondary); line-height: 1.7; }
.exam-card footer { border-top: 1px solid var(--lp-border); margin-top: 18px; padding-top: 16px; }
.exam-card footer a { color: var(--lp-primary); font-weight: 800; }
</style>
