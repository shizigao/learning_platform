<script setup lang="ts">
import { ElMessage, ElMessageBox } from 'element-plus'
import { computed, onBeforeUnmount, onMounted, reactive, ref } from 'vue'
import { useRoute } from 'vue-router'

import {
  getCandidateExamOverview,
  resumeExam,
  saveExamAnswer,
  saveExamAnswers,
  startExam,
  submitExam,
} from '@/api/exam'
import type {
  CandidateExamOverview,
  CandidatePaperQuestion,
  ExamStartSession,
  ExamSubmission,
  QuestionType,
} from '@/types/exam'
import { examAnswerPayload, minimumAnswerValues } from '@/utils/exam-answer'
import { shouldShowExamCountdown } from '@/utils/ui-state'

const route = useRoute()
const examId = Number(route.params.id)
const loading = ref(false)
const starting = ref(false)
const savingAll = ref(false)
const submitting = ref(false)
const overview = ref<CandidateExamOverview>()
const session = ref<ExamStartSession>()
const submission = ref<ExamSubmission>()
const remainingSeconds = ref(0)
const answerDrafts = reactive<Record<number, { values: string[]; text: string }>>({})
const saveStates = reactive<Record<number, { saving: boolean; savedAt?: string; error?: string }>>({})
const saveTimers = new Map<number, number>()
let timer: number | undefined
let serverTimeAtSync = 0
let clientTimeAtSync = 0
let deadlineAt = 0

const typeLabels: Record<QuestionType, string> = {
  SINGLE_CHOICE: '单选题',
  MULTIPLE_CHOICE: '多选题',
  TRUE_FALSE: '判断题',
  FILL_BLANK: '填空题',
  SHORT_ANSWER: '简答题',
}

const remainingText = computed(() => {
  const seconds = remainingSeconds.value
  const hours = Math.floor(seconds / 3600)
  const minutes = Math.floor((seconds % 3600) / 60)
  const rest = seconds % 60
  return [hours, minutes, rest].map((value) => String(value).padStart(2, '0')).join(':')
})
const answeredCount = computed(() =>
  Object.values(answerDrafts).filter((answer) =>
    answer.values.some((value) => value.trim() !== '') || answer.text.trim() !== '',
  ).length,
)
const locked = computed(() => Boolean(submission.value) || remainingSeconds.value === 0)
const hasSubmittedAttempt = computed(() =>
  submission.value != null
  || overview.value?.eligibility.attemptStatus === 'GRADING'
  || overview.value?.eligibility.attemptStatus === 'COMPLETED'
  || overview.value?.eligibility.attemptStatus === 'SUBMITTED',
)
const showCountdown = computed(() =>
  shouldShowExamCountdown(
    overview.value?.eligibility.attemptId,
    overview.value?.eligibility.attemptStatus,
    submission.value != null,
  ),
)

function displayTime(value?: string): string {
  return value ? new Date(value).toLocaleString('zh-CN', { hour12: false }) : '—'
}

function syncCountdown(serverTime: string, deadline: string): void {
  serverTimeAtSync = new Date(serverTime).getTime()
  clientTimeAtSync = Date.now()
  deadlineAt = new Date(deadline).getTime()
  updateCountdown()
  if (timer !== undefined) window.clearInterval(timer)
  timer = window.setInterval(updateCountdown, 1000)
}

function updateCountdown(): void {
  const estimatedServerNow = serverTimeAtSync + (Date.now() - clientTimeAtSync)
  remainingSeconds.value = Math.max(0, Math.ceil((deadlineAt - estimatedServerNow) / 1000))
  if (remainingSeconds.value === 0 && timer !== undefined) {
    window.clearInterval(timer)
    timer = undefined
  }
}

function hydrateSession(value: ExamStartSession): void {
  session.value = value
  const restored = new Map(value.answers.map((answer) => [answer.questionId, answer]))
  for (const question of value.questions) {
    const answer = restored.get(question.questionId)
    const minimumValues = minimumAnswerValues(question.questionType, question.blankCount)
    const values = [...(answer?.values ?? [])]
    while (values.length < minimumValues) values.push('')
    answerDrafts[question.questionId] = { values, text: answer?.text ?? '' }
    saveStates[question.questionId] = {
      saving: false,
      savedAt: answer?.gradingStatus === 'SAVED' ? answer.savedAt : undefined,
    }
  }
  syncCountdown(value.serverTime, value.deadlineAt)
}

function payload(questionId: number) {
  const answer = answerDrafts[questionId]
  const question = session.value?.questions.find((item) => item.questionId === questionId)
  return examAnswerPayload(question, answer)
}

function queueSave(question: CandidatePaperQuestion): void {
  if (locked.value) return
  const oldTimer = saveTimers.get(question.questionId)
  if (oldTimer !== undefined) window.clearTimeout(oldTimer)
  saveTimers.set(
    question.questionId,
    window.setTimeout(() => {
      saveTimers.delete(question.questionId)
      void saveOne(question)
    }, 650),
  )
}

async function saveOne(question: CandidatePaperQuestion): Promise<void> {
  if (locked.value) return
  const state = saveStates[question.questionId]
  if (!state) return
  state.saving = true
  state.error = undefined
  try {
    const saved = await saveExamAnswer(examId, question.questionId, payload(question.questionId))
    state.savedAt = saved.savedAt
  } catch (error) {
    state.error = error instanceof Error ? error.message : '保存失败'
    ElMessage.error(state.error)
  } finally {
    state.saving = false
  }
}

async function saveAll(): Promise<void> {
  if (!session.value || locked.value) return
  for (const saveTimer of saveTimers.values()) window.clearTimeout(saveTimer)
  saveTimers.clear()
  savingAll.value = true
  try {
    const saved = await saveExamAnswers(
      examId,
      session.value.questions.map((question) => ({
        questionId: question.questionId,
        answer: payload(question.questionId),
      })),
    )
    for (const answer of saved) {
      const state = saveStates[answer.questionId]
      if (state) {
        state.savedAt = answer.savedAt
        state.error = undefined
      }
    }
    ElMessage.success('全部答案已保存')
  } finally {
    savingAll.value = false
  }
}

async function load(): Promise<void> {
  if (!Number.isInteger(examId) || examId <= 0) {
    ElMessage.error('考试编号无效')
    return
  }
  loading.value = true
  try {
    overview.value = await getCandidateExamOverview(examId)
    const eligibility = overview.value.eligibility
    if (showCountdown.value && eligibility.deadlineAt) {
      syncCountdown(eligibility.serverTime, eligibility.deadlineAt)
      if (eligibility.canStart) {
        hydrateSession(await resumeExam(examId))
      }
    }
  } catch (error) {
    ElMessage.error(error instanceof Error ? error.message : '考试信息加载失败')
  } finally {
    loading.value = false
  }
}

async function begin(): Promise<void> {
  try {
    await ElMessageBox.confirm(
      '点击确定后由服务器记录正式开始时间，倒计时不会因关闭或刷新页面而暂停。',
      overview.value?.eligibility.attemptId ? '继续考试' : '开始考试',
      { confirmButtonText: '确定开始', cancelButtonText: '暂不开始', type: 'warning' },
    )
    starting.value = true
    hydrateSession(await startExam(examId))
    overview.value = await getCandidateExamOverview(examId)
    ElMessage.success('考试计时已开始')
  } catch (error) {
    if (error !== 'cancel') {
      ElMessage.error(error instanceof Error ? error.message : '开始考试失败')
    }
  } finally {
    starting.value = false
  }
}

async function handIn(): Promise<void> {
  try {
    await ElMessageBox.confirm(
      `当前已作答 ${answeredCount.value}/${session.value?.questions.length ?? 0} 题，交卷后不能再修改，确定提交吗？`,
      '确认交卷',
      { confirmButtonText: '确认交卷', cancelButtonText: '继续检查', type: 'warning' },
    )
    submitting.value = true
    await saveAll()
    submission.value = await submitExam(examId)
    if (timer !== undefined) {
      window.clearInterval(timer)
      timer = undefined
    }
    ElMessage.success('交卷成功')
    overview.value = await getCandidateExamOverview(examId)
  } catch (error) {
    if (error !== 'cancel') {
      ElMessage.error(error instanceof Error ? error.message : '交卷失败')
    }
  } finally {
    submitting.value = false
  }
}

onMounted(load)
onBeforeUnmount(() => {
  if (timer !== undefined) window.clearInterval(timer)
  for (const saveTimer of saveTimers.values()) window.clearTimeout(saveTimer)
})
</script>

<template>
  <section class="entry-page">
    <div class="page-container">
      <RouterLink class="back-link" to="/exams">← 返回考试中心</RouterLink>
      <div v-loading="loading" class="entry-shell">
        <template v-if="overview">
          <header class="exam-header">
            <div>
              <span class="eyebrow">EXAM SESSION</span>
              <h1>{{ overview.exam.name }}</h1>
              <p>{{ overview.paper.name }} · {{ overview.paper.questionCount }} 题 · {{ overview.paper.totalScore }} 分</p>
            </div>
            <div v-if="showCountdown" class="timer" :class="{ expired: remainingSeconds === 0 }">
              <small>服务端剩余时间</small>
              <strong>{{ remainingText }}</strong>
            </div>
          </header>

          <div class="summary-grid">
            <article>
              <span>考试开放时间</span>
              <strong>{{ displayTime(overview.exam.startAt) }}</strong>
              <small>至 {{ displayTime(overview.exam.endAt) }}</small>
            </article>
            <article><span>答题时长</span><strong>{{ overview.exam.durationMinutes }} 分钟</strong></article>
            <article><span>试卷总分</span><strong>{{ Number(overview.paper.totalScore).toFixed(2) }} 分</strong></article>
            <article><span>及格分</span><strong>{{ Number(overview.exam.passingScore).toFixed(2) }} 分</strong></article>
          </div>

          <section class="instruction-card">
            <h2>考试说明</h2>
            <p>{{ overview.instructions || '本场考试暂无额外说明，请在规定时间内独立完成。' }}</p>
          </section>

          <section v-if="!session" class="start-card">
            <div>
              <el-tag :type="overview.eligibility.canStart ? 'success' : 'info'">
                {{ overview.eligibility.reason }}
              </el-tag>
              <p v-if="overview.eligibility.attemptId">
                已于 {{ displayTime(overview.eligibility.startedAt) }} 开始，截止时间为
                {{ displayTime(overview.eligibility.deadlineAt) }}。
              </p>
              <p v-else>开始后由服务器记录时间并计算个人截止时间，浏览器倒计时仅用于展示。</p>
            </div>
            <el-button
              v-if="!hasSubmittedAttempt"
              type="primary"
              size="large"
              :loading="starting"
              :disabled="!overview.eligibility.canStart"
              @click="begin"
            >
              {{ overview.eligibility.attemptId ? '继续考试' : '开始考试' }}
            </el-button>
            <RouterLink v-else class="result-button" :to="`/exams/${examId}/result`">
              查看考试成绩
            </RouterLink>
          </section>

          <section v-else class="paper-card">
            <div class="paper-notice">
              <strong v-if="submission">已交卷</strong>
              <strong v-else-if="remainingSeconds === 0">作答时间已结束</strong>
              <strong v-else>考试进行中</strong>
              <span v-if="submission">
                {{ submission.submissionType === 'MANUAL' ? '手动交卷' : '超时交卷' }}，
                已保存 {{ submission.answeredCount }}/{{ submission.totalQuestions }} 题
                · <RouterLink :to="`/exams/${examId}/result`">查看成绩</RouterLink>
              </span>
              <span v-else-if="remainingSeconds === 0">服务器将在后台完成超时交卷，请勿继续修改答案。</span>
              <span v-else>答案修改后自动保存，也可以使用底部“保存全部”。</span>
            </div>
            <article v-for="question in session.questions" :key="question.paperQuestionId" class="question">
              <div>
                <el-tag size="small">{{ typeLabels[question.questionType] }}</el-tag>
                <span>{{ question.score }} 分</span>
              </div>
              <h3>{{ question.sortOrder }}. {{ question.stem }}</h3>
              <el-radio-group
                v-if="question.questionType === 'SINGLE_CHOICE'"
                v-model="answerDrafts[question.questionId].values[0]"
                class="answer-options"
                :disabled="locked"
                @change="queueSave(question)"
              >
                <el-radio v-for="option in question.options" :key="option.key" :value="option.key">
                  {{ option.key }}. {{ option.text }}
                </el-radio>
              </el-radio-group>
              <el-checkbox-group
                v-else-if="question.questionType === 'MULTIPLE_CHOICE'"
                v-model="answerDrafts[question.questionId].values"
                class="answer-options"
                :disabled="locked"
                @change="queueSave(question)"
              >
                <el-checkbox v-for="option in question.options" :key="option.key" :value="option.key">
                  {{ option.key }}. {{ option.text }}
                </el-checkbox>
              </el-checkbox-group>
              <el-radio-group
                v-else-if="question.questionType === 'TRUE_FALSE'"
                v-model="answerDrafts[question.questionId].values[0]"
                class="answer-options"
                :disabled="locked"
                @change="queueSave(question)"
              >
                <el-radio value="TRUE">正确</el-radio>
                <el-radio value="FALSE">错误</el-radio>
              </el-radio-group>
              <div v-else-if="question.questionType === 'FILL_BLANK'" class="blank-answers">
                <el-input
                  v-for="(_, index) in answerDrafts[question.questionId].values"
                  :key="index"
                  v-model="answerDrafts[question.questionId].values[index]"
                  :disabled="locked"
                  :placeholder="`第 ${index + 1} 空`"
                  @input="queueSave(question)"
                />
              </div>
              <el-input
                v-else
                v-model="answerDrafts[question.questionId].text"
                type="textarea"
                :rows="5"
                maxlength="20000"
                show-word-limit
                :disabled="locked"
                placeholder="请输入你的答案"
                @input="queueSave(question)"
              />
              <div class="save-state">
                <span v-if="saveStates[question.questionId]?.saving">正在保存…</span>
                <span v-else-if="saveStates[question.questionId]?.error" class="save-error">
                  {{ saveStates[question.questionId]?.error }}
                </span>
                <span v-else-if="saveStates[question.questionId]?.savedAt">
                  已保存 {{ displayTime(saveStates[question.questionId]?.savedAt) }}
                </span>
                <span v-else>尚未作答</span>
              </div>
            </article>
            <footer v-if="!submission" class="paper-actions">
              <span>已作答 {{ answeredCount }}/{{ session.questions.length }} 题</span>
              <div>
                <el-button :loading="savingAll" :disabled="locked" @click="saveAll">保存全部</el-button>
                <el-button
                  type="primary"
                  :loading="submitting"
                  :disabled="locked"
                  @click="handIn"
                >
                  提交试卷
                </el-button>
              </div>
            </footer>
          </section>
        </template>
      </div>
    </div>
  </section>
</template>

<style scoped>
.entry-page { min-height: calc(100vh - 145px); padding: 32px 0 76px; background: #f5f7fb; }
.back-link { display: inline-block; margin-bottom: 18px; color: var(--lp-text-secondary); font-weight: 700; }
.entry-shell { min-height: 420px; }
.exam-header { display: flex; border: 1px solid #dfe7f5; border-radius: 20px; align-items: center; justify-content: space-between; gap: 24px; background: linear-gradient(135deg, #fff, #edf4ff); padding: 30px 34px; }
.eyebrow { color: var(--lp-primary); font-size: 12px; font-weight: 900; letter-spacing: .16em; }
.exam-header h1 { margin: 10px 0 8px; font-size: 32px; }.exam-header p { margin: 0; color: var(--lp-text-secondary); }
.timer { min-width: 190px; border-radius: 16px; color: #fff; background: #2864e8; padding: 16px 22px; text-align: center; }
.timer small, .timer strong { display: block; }.timer strong { margin-top: 4px; font-size: 30px; font-variant-numeric: tabular-nums; letter-spacing: .05em; }
.timer.expired { background: #d94b4b; }
.summary-grid { display: grid; gap: 14px; margin-top: 18px; grid-template-columns: 2fr repeat(3, 1fr); }
.summary-grid article, .instruction-card, .start-card, .paper-card { border: 1px solid var(--lp-border); border-radius: 16px; background: #fff; padding: 20px; }
.summary-grid span, .summary-grid small { display: block; color: var(--lp-text-secondary); font-size: 13px; }.summary-grid strong { display: block; margin: 8px 0 4px; }
.instruction-card { margin-top: 18px; }.instruction-card h2 { margin-top: 0; font-size: 18px; }.instruction-card p { margin-bottom: 0; color: #475467; line-height: 1.8; white-space: pre-wrap; }
.start-card { display: flex; margin-top: 18px; align-items: center; justify-content: space-between; gap: 24px; }.start-card p { margin: 10px 0 0; color: var(--lp-text-secondary); }
.result-button { border-radius: 8px; color: #fff; background: var(--lp-primary); padding: 12px 20px; font-weight: 800; }
.paper-notice a { color: #175cd3; font-weight: 800; }
.paper-card { margin-top: 18px; }.paper-notice { display: flex; border-radius: 10px; align-items: center; justify-content: space-between; gap: 18px; color: #175cd3; background: #eff6ff; padding: 12px 14px; font-size: 13px; }
.question { border-top: 1px solid var(--lp-border); margin-top: 20px; padding-top: 18px; }.question > div { display: flex; justify-content: space-between; color: var(--lp-text-secondary); font-size: 13px; }
.question h3 { font-size: 16px; line-height: 1.8; }
.answer-options { display: flex; align-items: flex-start; flex-direction: column; gap: 10px; }
.blank-answers { display: grid; gap: 10px; }
.save-state { margin-top: 12px; color: #98a2b3; font-size: 12px; text-align: right; }.save-error { color: #d92d20; }
.paper-actions { position: sticky; bottom: 12px; display: flex; border: 1px solid #cddcf8; border-radius: 14px; align-items: center; justify-content: space-between; margin-top: 26px; background: rgba(255,255,255,.96); box-shadow: 0 10px 28px rgba(32, 61, 118, .12); padding: 14px 18px; }
@media (max-width: 760px) {
  .exam-header, .start-card, .paper-notice { align-items: stretch; flex-direction: column; }
  .summary-grid { grid-template-columns: 1fr 1fr; }
}
</style>
