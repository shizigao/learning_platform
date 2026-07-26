<script setup lang="ts">
import { ElMessage } from 'element-plus'
import { computed, onMounted, ref } from 'vue'
import { useRoute } from 'vue-router'

import {
  generateOverallExamAiAnalysis,
  generatePersonalExamAiAnalysis,
  getOverallExamAiAnalysis,
  getPersonalExamAiAnalysis,
} from '@/api/exam'
import MarkdownRenderer from '@/components/MarkdownRenderer.vue'
import SectionPageHeader from '@/components/SectionPageHeader.vue'
import type { ExamAiAnalysis, ExamAiAnalysisPage } from '@/types/exam'

const route = useRoute()
const examId = Number(route.params.id)
const overall = computed(() => route.name === 'publisher-exam-ai-analysis')
const loading = ref(false)
const generating = ref(false)
const page = ref<ExamAiAnalysisPage>()
const selectedId = ref<number>()
const statusText = ref('')

const selectedReport = computed<ExamAiAnalysis | undefined>(() =>
  page.value?.reports.find((item) => item.id === selectedId.value),
)
const backPath = computed(() =>
  overall.value
    ? `/publisher/exams/${examId}/grading`
    : `/exams/${examId}/result`,
)
const productType = computed(() =>
  overall.value ? 'EXAM_OVERALL_AI_PACKAGE' : 'EXAM_PERSONAL_AI_PACKAGE',
)

function requestId(): string {
  const random = typeof crypto?.randomUUID === 'function'
    ? crypto.randomUUID().replaceAll('-', '')
    : `${Date.now()}${Math.random().toString(16).slice(2)}`
  return `exam-ai-${random}`.slice(0, 64)
}

function dateTime(value: string): string {
  return new Date(value).toLocaleString('zh-CN', { hour12: false })
}

async function load(): Promise<void> {
  loading.value = true
  try {
    page.value = overall.value
      ? await getOverallExamAiAnalysis(examId)
      : await getPersonalExamAiAnalysis(examId)
    if (!selectedId.value && page.value.reports.length) {
      selectedId.value = page.value.reports[0]?.id
    }
  } catch (error) {
    ElMessage.error({
      message: error instanceof Error ? error.message : 'AI 分析信息加载失败',
      duration: 2000,
    })
  } finally {
    loading.value = false
  }
}

async function generate(): Promise<void> {
  if (!page.value?.eligible || generating.value) return
  if (page.value.quotaRemaining <= 0) {
    ElMessage.warning({ message: '可用次数不足，请先购买对应次数包', duration: 2000 })
    return
  }
  generating.value = true
  statusText.value = '请求已提交，AI 正在整理考试数据并生成分析报告，请勿重复点击。'
  try {
    const report = overall.value
      ? await generateOverallExamAiAnalysis(examId, requestId())
      : await generatePersonalExamAiAnalysis(examId, requestId())
    await load()
    selectedId.value = report.id
    statusText.value = 'AI 分析已生成并保存，本次成功调用已扣除 1 次额度。'
    ElMessage.success({ message: '考试 AI 分析已生成', duration: 2000 })
    window.setTimeout(() => {
      statusText.value = ''
    }, 2000)
  } catch (error) {
    statusText.value = error instanceof Error ? error.message : 'AI 分析生成失败'
    ElMessage.error({ message: statusText.value, duration: 2000 })
  } finally {
    generating.value = false
  }
}

onMounted(load)
</script>

<template>
  <section class="analysis-page">
    <div class="page-container">
      <RouterLink class="back-link" :to="backPath">← 返回</RouterLink>
      <SectionPageHeader
        eyebrow="AI EXAM INSIGHT"
        :title="overall ? '考试整体 AI 分析' : '考试个人 AI 分析'"
        :description="overall
          ? '依据考试统计和匿名化逐题作答情况，生成教学诊断与指导建议'
          : '依据你的最终成绩和逐题作答情况，生成个性化查缺补漏报告'"
      />

      <div v-loading="loading" class="analysis-shell">
        <template v-if="page">
          <header class="analysis-toolbar">
            <div>
              <span>分析对象</span>
              <strong>{{ page.examName }}</strong>
            </div>
            <div class="quota">
              <span>剩余次数</span>
              <strong>{{ page.quotaRemaining }}</strong>
            </div>
            <RouterLink :to="{ path: '/commerce', query: { type: productType } }">
              <el-button>购买次数</el-button>
            </RouterLink>
            <el-button
              type="primary"
              :loading="generating"
              :disabled="!page.eligible || page.quotaRemaining <= 0"
              @click="generate"
            >
              {{ generating ? 'AI 正在生成' : (page.reports.length ? '重新生成分析' : '生成 AI 分析') }}
            </el-button>
          </header>

          <el-alert
            v-if="!page.eligible"
            :title="page.ineligibleReason || '当前暂不满足生成条件'"
            type="warning"
            :closable="false"
            show-icon
          />
          <el-alert
            v-else-if="page.quotaRemaining <= 0"
            title="对应 AI 分析次数不足，请先购买次数包"
            type="warning"
            :closable="false"
            show-icon
          />
          <div v-if="statusText" class="operation-status" :class="{ generating }">
            <span class="status-dot" />
            <span>{{ statusText }}</span>
          </div>

          <div v-if="page.reports.length" class="report-layout">
            <aside>
              <h2>历史报告</h2>
              <button
                v-for="report in page.reports"
                :key="report.id"
                type="button"
                :class="{ active: report.id === selectedId }"
                @click="selectedId = report.id"
              >
                <strong>{{ dateTime(report.createdAt) }}</strong>
                <span>{{ report.task.model }}</span>
              </button>
            </aside>
            <article class="report-card">
              <header v-if="selectedReport">
                <div>
                  <span>生成时间</span>
                  <strong>{{ dateTime(selectedReport.createdAt) }}</strong>
                </div>
                <el-tag type="success">已生成并保存</el-tag>
              </header>
              <MarkdownRenderer
                v-if="selectedReport"
                :source="selectedReport.reportMarkdown"
              />
            </article>
          </div>
          <el-empty
            v-else
            description="暂无分析报告，满足条件并拥有次数后即可生成"
          />
        </template>
      </div>
    </div>
  </section>
</template>

<style scoped>
.analysis-page { min-height: calc(100vh - 145px); padding: 36px 0 76px; background: #f5f7fb; }
.back-link { display: inline-block; margin-bottom: 14px; color: var(--lp-text-secondary); font-weight: 700; }
.analysis-shell { min-height: 420px; }
.analysis-toolbar { display: flex; border: 1px solid var(--lp-border); border-radius: 16px; align-items: center; gap: 14px; background: #fff; padding: 18px; }
.analysis-toolbar > div:first-child { min-width: 0; flex: 1; }
.analysis-toolbar span, .analysis-toolbar strong { display: block; }
.analysis-toolbar span { color: var(--lp-text-secondary); font-size: 12px; }
.analysis-toolbar strong { overflow: hidden; margin-top: 5px; text-overflow: ellipsis; white-space: nowrap; }
.analysis-toolbar .quota { min-width: 90px; text-align: center; }
.analysis-toolbar .quota strong { color: var(--lp-primary); font-size: 24px; }
.el-alert, .operation-status { margin-top: 14px; }
.operation-status { display: flex; border: 1px solid #bfdbfe; border-radius: 10px; align-items: center; gap: 10px; color: #1d4ed8; background: #eff6ff; padding: 12px 14px; }
.status-dot { width: 10px; height: 10px; border-radius: 50%; background: currentcolor; }
.operation-status.generating .status-dot { border: 2px solid currentcolor; border-right-color: transparent; background: transparent; animation: spin .8s linear infinite; }
.report-layout { display: grid; gap: 18px; margin-top: 18px; grid-template-columns: 230px minmax(0, 1fr); }
.report-layout aside, .report-card { border: 1px solid var(--lp-border); border-radius: 16px; background: #fff; }
.report-layout aside { align-self: start; overflow: hidden; padding: 14px; }
.report-layout aside h2 { margin: 2px 4px 12px; font-size: 16px; }
.report-layout aside button { width: 100%; border: 0; border-radius: 9px; margin-top: 6px; color: var(--lp-text); background: transparent; cursor: pointer; padding: 10px; text-align: left; }
.report-layout aside button.active { color: #1d4ed8; background: #eef4ff; }
.report-layout aside button strong, .report-layout aside button span { display: block; }
.report-layout aside button span { margin-top: 4px; color: #98a2b3; font-size: 11px; }
.report-card { min-width: 0; padding: 24px 28px; }
.report-card > header { display: flex; border-bottom: 1px solid var(--lp-border); align-items: center; justify-content: space-between; margin-bottom: 20px; padding-bottom: 15px; }
.report-card > header span, .report-card > header strong { display: block; }
.report-card > header span { color: #98a2b3; font-size: 12px; }
.report-card > header strong { margin-top: 3px; font-size: 14px; }
@keyframes spin { to { transform: rotate(360deg); } }
@media (max-width: 760px) {
  .analysis-toolbar { align-items: stretch; flex-wrap: wrap; }
  .analysis-toolbar > div:first-child { width: 100%; flex-basis: 100%; }
  .report-layout { grid-template-columns: 1fr; }
  .report-card { padding: 18px; }
}
</style>
