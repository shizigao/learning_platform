<script setup lang="ts">
import { ElMessage, ElMessageBox } from 'element-plus'
import { computed, onMounted, reactive, ref } from 'vue'
import { useRoute } from 'vue-router'

import {
  completeExamReview,
  getExamGradingDetail,
  getExamStatistics,
  getPublisherExam,
  gradeExamAnswer,
  listExamGradingAttempts,
} from '@/api/exam'
import PublisherExamNav from '@/components/PublisherExamNav.vue'
import SectionPageHeader from '@/components/SectionPageHeader.vue'
import { useAuthStore } from '@/stores/auth'
import type {
  ExamGradingAttempt,
  ExamGradingDetail,
  ExamManagement,
  ExamResultQuestion,
  ExamStatistics,
  QuestionAnswer,
  QuestionType,
} from '@/types/exam'

const route = useRoute()
const authStore = useAuthStore()
const examId = Number(route.params.id)
const loading = ref(false)
const detailLoading = ref(false)
const completing = ref(false)
const exam = ref<ExamManagement>()
const attempts = ref<ExamGradingAttempt[]>([])
const statistics = ref<ExamStatistics>()
const detail = ref<ExamGradingDetail>()
const detailVisible = ref(false)
const gradeDrafts = reactive<Record<number, { score: number; comment: string; saving: boolean }>>({})

const typeLabels: Record<QuestionType, string> = {
  SINGLE_CHOICE: '单选题',
  MULTIPLE_CHOICE: '多选题',
  TRUE_FALSE: '判断题',
  FILL_BLANK: '填空题',
  SHORT_ANSWER: '简答题',
}
const pendingCount = computed(() =>
  detail.value?.questions.filter((item) => item.gradingStatus === 'PENDING_REVIEW').length ?? 0,
)
const canOpenAiAnalysis = computed(() =>
  Boolean(
    exam.value &&
      statistics.value &&
      exam.value.exam.publisherId === authStore.user?.id &&
      new Date(exam.value.exam.endAt).getTime() <= Date.now() &&
      statistics.value.submittedCount > 0 &&
      statistics.value.gradedCount >= statistics.value.submittedCount,
  ),
)

function displayTime(value?: string): string {
  return value ? new Date(value).toLocaleString('zh-CN', { hour12: false }) : '—'
}

function score(value?: number): string {
  return value == null ? '—' : Number(value).toFixed(2)
}

function displayAnswer(question: ExamResultQuestion): string {
  if (question.text) return question.text
  const values = question.values.filter((value) => value.trim())
  return values.length ? values.join('；') : '未作答'
}

function correctAnswer(answer?: QuestionAnswer): string {
  if (!answer) return '—'
  return answer.acceptedAnswers
    .map((values, index) => `${index + 1}. ${values.join(' / ')}`)
    .join('；')
}

async function load(): Promise<void> {
  loading.value = true
  try {
    const [examDetail, gradingAttempts, stats] = await Promise.all([
      getPublisherExam(examId),
      listExamGradingAttempts(examId),
      getExamStatistics(examId),
    ])
    exam.value = examDetail
    attempts.value = gradingAttempts
    statistics.value = stats
  } catch (error) {
    ElMessage.error(error instanceof Error ? error.message : '阅卷数据加载失败')
  } finally {
    loading.value = false
  }
}

function hydrateDrafts(value: ExamGradingDetail): void {
  for (const question of value.questions) {
    if (question.gradingStatus === 'PENDING_REVIEW' || question.gradingStatus === 'GRADED') {
      gradeDrafts[question.answerId] = {
        score: Number(question.score ?? 0),
        comment: question.graderComment ?? '',
        saving: false,
      }
    }
  }
}

async function openDetail(attempt: ExamGradingAttempt): Promise<void> {
  detailVisible.value = true
  detailLoading.value = true
  try {
    const value = await getExamGradingDetail(examId, attempt.attemptId)
    detail.value = value
    hydrateDrafts(value)
  } catch (error) {
    ElMessage.error(error instanceof Error ? error.message : '阅卷详情加载失败')
  } finally {
    detailLoading.value = false
  }
}

async function saveGrade(question: ExamResultQuestion): Promise<void> {
  if (!detail.value) return
  const draft = gradeDrafts[question.answerId]
  if (!draft) return
  draft.saving = true
  try {
    const updated = await gradeExamAnswer(
      examId,
      detail.value.attempt.attemptId,
      question.answerId,
      { score: Number(draft.score), comment: draft.comment.trim() || undefined },
    )
    const index = detail.value.questions.findIndex((item) => item.answerId === question.answerId)
    if (index >= 0) detail.value.questions[index] = updated
    ElMessage.success(`第 ${question.sortOrder} 题评分已保存`)
    await refreshCurrent()
  } catch (error) {
    ElMessage.error(error instanceof Error ? error.message : '评分保存失败')
  } finally {
    draft.saving = false
  }
}

async function refreshCurrent(): Promise<void> {
  if (!detail.value) return
  const attemptId = detail.value.attempt.attemptId
  const [value, gradingAttempts, stats] = await Promise.all([
    getExamGradingDetail(examId, attemptId),
    listExamGradingAttempts(examId),
    getExamStatistics(examId),
  ])
  detail.value = value
  attempts.value = gradingAttempts
  statistics.value = stats
  hydrateDrafts(value)
}

async function finishReview(): Promise<void> {
  if (!detail.value) return
  try {
    await ElMessageBox.confirm(
      '确认完成后本次成绩将锁定，不能继续修改评分。是否继续？',
      '完成阅卷',
      { type: 'warning', confirmButtonText: '确认完成' },
    )
    completing.value = true
    await completeExamReview(examId, detail.value.attempt.attemptId)
    ElMessage.success('本次阅卷已完成')
    detailVisible.value = false
    detail.value = undefined
    await load()
  } catch (error) {
    if (error !== 'cancel') {
      ElMessage.error(error instanceof Error ? error.message : '完成阅卷失败')
    }
  } finally {
    completing.value = false
  }
}

onMounted(load)
</script>

<template>
  <section class="grading-page">
    <div class="page-container">
      <PublisherExamNav />
      <RouterLink class="back-link" to="/publisher/exams">← 返回考试管理</RouterLink>
      <SectionPageHeader
        eyebrow="GRADING & ANALYTICS"
        :title="exam?.exam.name || '阅卷与统计'"
        description="批改主观题、确认最终成绩，并查看考试参与和答题质量"
      >
        <RouterLink
          v-if="canOpenAiAnalysis"
          :to="`/publisher/exams/${examId}/grading/ai-analysis`"
        >
          <el-button type="primary">AI 分析</el-button>
        </RouterLink>
      </SectionPageHeader>

      <div v-loading="loading">
        <template v-if="statistics">
          <div class="metric-grid">
            <article><span>指定考生</span><strong>{{ statistics.totalCandidates }}</strong></article>
            <article><span>已参加</span><strong>{{ statistics.participatedCount }}</strong></article>
            <article><span>已交卷</span><strong>{{ statistics.submittedCount }}</strong></article>
            <article><span>未参加</span><strong>{{ statistics.notParticipatedCount }}</strong></article>
            <article><span>已出最终分</span><strong>{{ statistics.gradedCount }}</strong></article>
            <article><span>平均分</span><strong>{{ score(statistics.averageScore) }}</strong></article>
            <article><span>最高 / 最低</span><strong>{{ score(statistics.highestScore) }} / {{ score(statistics.lowestScore) }}</strong></article>
            <article><span>及格人数 / 率</span><strong>{{ statistics.passedCount }} / {{ Number(statistics.passRate).toFixed(2) }}%</strong></article>
          </div>

          <div class="panel">
            <h2>考生交卷与阅卷</h2>
            <el-table :data="attempts">
              <el-table-column label="考生" min-width="190">
                <template #default="{ row }"><strong>{{ row.nickname }}</strong><small>{{ row.username }}</small></template>
              </el-table-column>
              <el-table-column label="交卷时间" min-width="180"><template #default="{ row }">{{ displayTime(row.submittedAt) }}</template></el-table-column>
              <el-table-column label="方式" width="100"><template #default="{ row }">{{ row.submissionType === 'TIMEOUT' ? '超时' : '手动' }}</template></el-table-column>
              <el-table-column label="待阅" width="90" prop="pendingReviewCount" />
              <el-table-column label="当前分数" width="110"><template #default="{ row }">{{ score(row.totalScore) }}</template></el-table-column>
              <el-table-column label="状态" width="110">
                <template #default="{ row }"><el-tag :type="row.gradingCompleted ? 'success' : 'warning'">{{ row.gradingCompleted ? '已完成' : '阅卷中' }}</el-tag></template>
              </el-table-column>
              <el-table-column label="操作" width="110">
                <template #default="{ row }"><el-button link type="primary" @click="openDetail(row as ExamGradingAttempt)">查看阅卷</el-button></template>
              </el-table-column>
              <template #empty><el-empty description="暂无已交卷记录" /></template>
            </el-table>
          </div>

          <div class="panel">
            <h2>每题正确率</h2>
            <el-table :data="statistics.questions">
              <el-table-column label="#" width="56" prop="sortOrder" />
              <el-table-column label="题目" min-width="320">
                <template #default="{ row }"><strong>{{ row.stem }}</strong><small>{{ typeLabels[row.questionType as QuestionType] }} · {{ row.maxScore }} 分</small></template>
              </el-table-column>
              <el-table-column label="已评分" width="90" prop="gradedCount" />
              <el-table-column label="作答" width="80" prop="answeredCount" />
              <el-table-column label="全对" width="80" prop="correctCount" />
              <el-table-column label="正确率" width="110"><template #default="{ row }">{{ Number(row.correctRate).toFixed(2) }}%</template></el-table-column>
            </el-table>
          </div>
        </template>
      </div>
    </div>

    <el-dialog v-model="detailVisible" title="考生阅卷详情" width="min(920px, 96vw)" top="4vh">
      <div v-loading="detailLoading">
        <template v-if="detail">
          <div class="attempt-summary">
            <div><strong>{{ detail.attempt.nickname }}</strong><span>{{ detail.attempt.username }}</span></div>
            <el-tag :type="detail.attempt.gradingCompleted ? 'success' : 'warning'">
              {{ detail.attempt.gradingCompleted ? '阅卷完成' : `待阅 ${pendingCount} 题` }}
            </el-tag>
          </div>
          <article v-for="question in detail.questions" :key="question.answerId" class="grading-question">
            <header>
              <div><el-tag size="small">{{ typeLabels[question.questionType] }}</el-tag><strong>{{ question.sortOrder }}. {{ question.stem }}</strong></div>
              <span>{{ score(question.score) }} / {{ score(question.maxScore) }} 分</span>
            </header>
            <p><b>考生答案：</b>{{ displayAnswer(question) }}</p>
            <p><b>参考答案：</b>{{ correctAnswer(question.correctAnswer) }}</p>
            <p v-if="question.analysis" class="analysis"><b>解析：</b>{{ question.analysis }}</p>
            <div
              v-if="gradeDrafts[question.answerId] && !detail.attempt.gradingCompleted"
              class="grade-editor"
            >
              <el-input-number
                v-model="gradeDrafts[question.answerId].score"
                :min="0"
                :max="question.maxScore"
                :precision="2"
              />
              <el-input v-model="gradeDrafts[question.answerId].comment" maxlength="2000" placeholder="批改评语（可选）" />
              <el-button type="primary" :loading="gradeDrafts[question.answerId].saving" @click="saveGrade(question)">保存评分</el-button>
            </div>
            <p v-else-if="question.graderComment"><b>批改评语：</b>{{ question.graderComment }}</p>
          </article>
        </template>
      </div>
      <template #footer>
        <el-button @click="detailVisible = false">关闭</el-button>
        <el-button
          v-if="detail && !detail.attempt.gradingCompleted"
          type="success"
          :disabled="pendingCount > 0"
          :loading="completing"
          @click="finishReview"
        >
          完成阅卷
        </el-button>
      </template>
    </el-dialog>
  </section>
</template>

<style scoped>
.grading-page { min-height: calc(100vh - 145px); padding: 38px 0 76px; background: #f5f7fb; }
.back-link { display: inline-block; margin-bottom: 14px; color: var(--lp-text-secondary); font-weight: 700; }
.metric-grid { display: grid; gap: 12px; grid-template-columns: repeat(4, 1fr); }
.metric-grid article { border: 1px solid var(--lp-border); border-radius: 14px; background: #fff; padding: 17px; }
.metric-grid span { color: var(--lp-text-secondary); font-size: 13px; }.metric-grid strong { display: block; margin-top: 8px; font-size: 21px; }
.panel { overflow: hidden; border: 1px solid var(--lp-border); border-radius: 17px; margin-top: 18px; background: #fff; padding: 18px; }
.panel h2 { margin: 0 0 15px; font-size: 18px; }.panel strong, .panel small { display: block; }.panel small { margin-top: 4px; color: #98a2b3; }
.attempt-summary { display: flex; border-radius: 12px; align-items: center; justify-content: space-between; background: #f5f8ff; padding: 15px; }
.attempt-summary strong, .attempt-summary span { display: block; }.attempt-summary span { margin-top: 3px; color: var(--lp-text-secondary); font-size: 13px; }
.grading-question { border: 1px solid var(--lp-border); border-radius: 14px; margin-top: 14px; padding: 18px; }
.grading-question header, .grading-question header > div { display: flex; align-items: flex-start; justify-content: space-between; gap: 12px; }
.grading-question header > div { justify-content: flex-start; }.grading-question header > span { color: var(--lp-primary); font-weight: 800; white-space: nowrap; }
.grading-question p { color: #475467; line-height: 1.75; white-space: pre-wrap; }.analysis { border-radius: 8px; background: #f7f9fc; padding: 10px; }
.grade-editor { display: grid; gap: 10px; grid-template-columns: 130px 1fr auto; }
@media (max-width: 760px) {
  .metric-grid { grid-template-columns: 1fr 1fr; }
  .grade-editor { grid-template-columns: 1fr; }
  .grading-question header { flex-direction: column; }
}
</style>
