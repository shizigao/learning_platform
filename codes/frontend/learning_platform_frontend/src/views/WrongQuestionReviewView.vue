<script setup lang="ts">
import { ElMessage } from 'element-plus'
import { computed, onMounted, ref } from 'vue'

import {
  generateWrongQuestionAnalysis,
  getWrongQuestionReview,
} from '@/api/exam'
import MarkdownRenderer from '@/components/MarkdownRenderer.vue'
import SectionPageHeader from '@/components/SectionPageHeader.vue'
import type {
  ExamResultQuestion,
  QuestionAnswer,
  WrongQuestionAnalysis,
  WrongQuestionReviewPage,
} from '@/types/exam'

const loading = ref(false)
const generating = ref(false)
const page = ref<WrongQuestionReviewPage>()
const selectedReportId = ref<number>()
const statusText = ref('')
let statusTimer: number | undefined

const selectedReport = computed<WrongQuestionAnalysis | undefined>(() =>
  page.value?.reports.find((report) => report.id === selectedReportId.value),
)

function dateTime(value: string): string {
  return new Date(value).toLocaleString('zh-CN', { hour12: false })
}

function createRequestId(): string {
  const random = typeof crypto?.randomUUID === 'function'
    ? crypto.randomUUID().replaceAll('-', '')
    : `${Date.now()}${Math.random().toString(16).slice(2)}`
  return `wrong-review-${random}`.slice(0, 64)
}

function showStatus(message: string, autoDismiss = false): void {
  if (statusTimer !== undefined) window.clearTimeout(statusTimer)
  statusText.value = message
  if (autoDismiss) {
    statusTimer = window.setTimeout(() => {
      statusText.value = ''
      statusTimer = undefined
    }, 2_000)
  }
}

function answerText(values: string[], text?: string): string {
  if (text?.trim()) return text
  return values.length ? values.join('、') : '未作答'
}

function correctAnswerText(answer?: QuestionAnswer): string {
  if (!answer?.acceptedAnswers.length) return '暂未公布'
  return answer.acceptedAnswers
    .map((values) => values.join('、'))
    .join('；或：')
}

function questionTypeLabel(question: ExamResultQuestion): string {
  const labels = {
    SINGLE_CHOICE: '单选题',
    MULTIPLE_CHOICE: '多选题',
    TRUE_FALSE: '判断题',
    FILL_BLANK: '填空题',
    SHORT_ANSWER: '简答题',
  }
  return labels[question.questionType]
}

async function load(): Promise<void> {
  loading.value = true
  try {
    page.value = await getWrongQuestionReview()
    if (
      page.value.reports.length
      && !page.value.reports.some((report) => report.id === selectedReportId.value)
    ) {
      selectedReportId.value = page.value.reports[0]?.id
    }
  } catch (error) {
    ElMessage.error({
      message: error instanceof Error ? error.message : '错题复习数据加载失败',
      duration: 2_000,
    })
  } finally {
    loading.value = false
  }
}

async function analyze(): Promise<void> {
  if (!page.value || generating.value) return
  if (page.value.analyzableQuestionCount <= 0) {
    ElMessage.warning({
      message: '暂无已公布答案的错题可供 AI 分析',
      duration: 2_000,
    })
    return
  }
  if (page.value.quotaRemaining <= 0) {
    ElMessage.warning({
      message: 'AI 学习助手次数不足，请先购买次数包',
      duration: 2_000,
    })
    return
  }
  generating.value = true
  showStatus('请求已提交，AI 正在整理最近考试错题并生成分析报告，请勿重复点击。')
  try {
    const report = await generateWrongQuestionAnalysis(createRequestId())
    await load()
    selectedReportId.value = report.id
    showStatus('错题 AI 分析已生成并保存，本次成功调用已扣除 1 次 AI 额度。', true)
  } catch (error) {
    const message = error instanceof Error ? error.message : '错题 AI 分析生成失败'
    showStatus(message, true)
    ElMessage.error({ message, duration: 2_000 })
  } finally {
    generating.value = false
  }
}

onMounted(load)
</script>

<template>
  <section class="wrong-review-page">
    <div class="page-container">
      <RouterLink class="back-link" to="/exams">← 返回考试中心</RouterLink>
      <SectionPageHeader
        eyebrow="WRONG QUESTION REVIEW"
        title="错题复习"
        description="汇总最近至多 5 场已完成考试的错题，帮助你集中复盘薄弱知识点"
      >
        <RouterLink to="/commerce?type=AI_PACKAGE">
          <el-button>购买 AI 次数</el-button>
        </RouterLink>
        <el-button
          type="primary"
          :loading="generating"
          :disabled="!page || page.analyzableQuestionCount <= 0 || page.quotaRemaining <= 0"
          @click="analyze"
        >
          {{ generating ? 'AI 正在分析' : 'AI 分析错题' }}
        </el-button>
      </SectionPageHeader>

      <div v-loading="loading" class="review-shell">
        <template v-if="page">
          <div class="summary-cards">
            <div><span>最近考试</span><strong>{{ page.exams.length }}</strong></div>
            <div><span>错题总数</span><strong>{{ page.totalQuestionCount }}</strong></div>
            <div><span>可供 AI 分析</span><strong>{{ page.analyzableQuestionCount }}</strong></div>
            <div><span>AI 剩余次数</span><strong>{{ page.quotaRemaining }}</strong></div>
          </div>

          <div v-if="statusText" class="operation-status" :class="{ generating }">
            <span class="status-dot" />
            <span>{{ statusText }}</span>
          </div>

          <el-alert
            v-if="page.totalQuestionCount > page.analyzableQuestionCount"
            title="部分考试尚未开放正确答案与解析：错题仍会展示，但不会发送给 AI，以免提前泄露答案。"
            type="warning"
            :closable="false"
            show-icon
          />

          <section v-if="page.reports.length" class="report-section">
            <header>
              <div>
                <h2>AI 错题分析报告</h2>
                <span>选择历史报告查看已保存的分析结果</span>
              </div>
              <el-select v-model="selectedReportId" style="width: 220px">
                <el-option
                  v-for="report in page.reports"
                  :key="report.id"
                  :label="dateTime(report.createdAt)"
                  :value="report.id"
                />
              </el-select>
            </header>
            <div v-if="selectedReport" class="report-meta">
              分析 {{ selectedReport.examCount }} 场考试、{{ selectedReport.questionCount }} 道错题
            </div>
            <MarkdownRenderer
              v-if="selectedReport"
              :source="selectedReport.reportMarkdown"
            />
          </section>

          <section class="exam-groups">
            <article
              v-for="exam in page.exams"
              :key="exam.resultId"
              class="exam-group"
            >
              <header>
                <div>
                  <h2>{{ exam.examName }}</h2>
                  <span>成绩生成于 {{ dateTime(exam.generatedAt) }}</span>
                </div>
                <div class="score">
                  <strong>{{ Number(exam.totalScore).toFixed(2) }}</strong>
                  <span>/ {{ Number(exam.fullScore).toFixed(2) }} 分</span>
                  <el-tag :type="exam.passed ? 'success' : 'danger'">
                    {{ exam.passed ? '已及格' : '未及格' }}
                  </el-tag>
                </div>
              </header>

              <el-empty
                v-if="exam.questions.length === 0"
                :image-size="58"
                description="本场考试没有错题"
              />
              <div v-else class="question-list">
                <article
                  v-for="question in exam.questions"
                  :key="question.answerId"
                  class="question-card"
                >
                  <header>
                    <el-tag effect="plain">{{ questionTypeLabel(question) }}</el-tag>
                    <strong>第 {{ question.sortOrder }} 题</strong>
                    <span>
                      {{ question.score == null ? '未评分' : Number(question.score).toFixed(2) }}
                      / {{ Number(question.maxScore).toFixed(2) }} 分
                    </span>
                  </header>
                  <h3>{{ question.stem }}</h3>
                  <ul v-if="question.options.length" class="options">
                    <li v-for="option in question.options" :key="option.key">
                      <b>{{ option.key }}.</b> {{ option.text }}
                    </li>
                  </ul>
                  <div class="answers">
                    <p><b>你的答案：</b>{{ answerText(question.values, question.text) }}</p>
                    <p v-if="exam.answersVisible">
                      <b>正确答案：</b>{{ correctAnswerText(question.correctAnswer) }}
                    </p>
                    <p v-else class="locked">正确答案与解析尚未公布</p>
                    <p v-if="exam.answersVisible && question.analysis">
                      <b>原题解析：</b>{{ question.analysis }}
                    </p>
                    <p v-if="question.graderComment">
                      <b>评阅意见：</b>{{ question.graderComment }}
                    </p>
                  </div>
                </article>
              </div>
            </article>
            <el-empty
              v-if="page.exams.length === 0"
              description="暂无已完成阅卷的考试记录"
            />
          </section>
        </template>
      </div>
    </div>
  </section>
</template>

<style scoped>
.wrong-review-page { min-height: calc(100vh - 145px); padding: 36px 0 76px; background: #f5f7fb; }
.back-link { display: inline-block; margin-bottom: 14px; color: var(--lp-text-secondary); font-weight: 700; }
.review-shell { min-height: 420px; }
.summary-cards { display: grid; gap: 14px; grid-template-columns: repeat(4, minmax(0, 1fr)); }
.summary-cards > div { border: 1px solid var(--lp-border); border-radius: 14px; background: #fff; padding: 18px 20px; }
.summary-cards span, .summary-cards strong { display: block; }
.summary-cards span { color: var(--lp-text-secondary); font-size: 13px; }
.summary-cards strong { margin-top: 8px; color: var(--lp-primary); font-size: 28px; }
.operation-status { display: flex; border: 1px solid #bfdbfe; border-radius: 10px; align-items: center; gap: 10px; margin-top: 14px; color: #1d4ed8; background: #eff6ff; padding: 12px 14px; }
.status-dot { width: 10px; height: 10px; border-radius: 50%; background: currentcolor; }
.operation-status.generating .status-dot { border: 2px solid currentcolor; border-right-color: transparent; background: transparent; animation: spin .8s linear infinite; }
.el-alert { margin-top: 14px; }
.report-section, .exam-group { border: 1px solid var(--lp-border); border-radius: 18px; background: #fff; box-shadow: var(--lp-shadow); }
.report-section { margin-top: 20px; padding: 24px 28px; }
.report-section > header, .exam-group > header, .question-card > header { display: flex; align-items: center; gap: 12px; }
.report-section > header, .exam-group > header { justify-content: space-between; }
.report-section h2, .exam-group h2 { margin: 0; }
.report-section header span, .exam-group header span { display: block; margin-top: 5px; color: var(--lp-text-secondary); font-size: 12px; }
.report-meta { border-top: 1px solid var(--lp-border); margin: 18px 0; color: var(--lp-text-secondary); padding-top: 14px; font-size: 13px; }
.exam-groups { display: flex; flex-direction: column; gap: 20px; margin-top: 20px; }
.exam-group { overflow: hidden; padding: 24px; }
.score { display: flex; align-items: baseline; gap: 6px; }
.score strong { color: var(--lp-primary); font-size: 27px; }
.score .el-tag { margin-left: 8px; }
.question-list { display: flex; flex-direction: column; gap: 14px; border-top: 1px solid var(--lp-border); margin-top: 20px; padding-top: 20px; }
.question-card { border: 1px solid #e5eaf2; border-radius: 13px; background: #fbfcff; padding: 18px; }
.question-card > header > span { margin-left: auto; color: var(--lp-text-secondary); font-size: 13px; }
.question-card h3 { margin: 16px 0 12px; line-height: 1.65; }
.options { display: grid; gap: 8px; margin: 0; padding: 0; list-style: none; }
.answers { border-top: 1px dashed #d7deea; margin-top: 15px; padding-top: 10px; }
.answers p { margin: 8px 0; color: #475467; line-height: 1.7; white-space: pre-wrap; }
.answers .locked { color: #b54708; }
@keyframes spin { to { transform: rotate(360deg); } }
@media (max-width: 760px) {
  .summary-cards { grid-template-columns: repeat(2, minmax(0, 1fr)); }
  .report-section > header, .exam-group > header { align-items: stretch; flex-direction: column; }
  .score { margin-top: 10px; }
}
</style>
