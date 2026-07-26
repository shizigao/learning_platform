<script setup lang="ts">
import { ElMessage } from 'element-plus'
import { onMounted, ref } from 'vue'
import { useRoute } from 'vue-router'

import { getExamResult } from '@/api/exam'
import type { ExamResultDetail, QuestionAnswer, QuestionType } from '@/types/exam'

const route = useRoute()
const examId = Number(route.params.id)
const loading = ref(false)
const result = ref<ExamResultDetail>()
const loadError = ref('')

const typeLabels: Record<QuestionType, string> = {
  SINGLE_CHOICE: '单选题',
  MULTIPLE_CHOICE: '多选题',
  TRUE_FALSE: '判断题',
  FILL_BLANK: '填空题',
  SHORT_ANSWER: '简答题',
}

function displayAnswer(values: string[], text?: string): string {
  if (text) return text
  const visible = values.filter((value) => value.trim() !== '')
  return visible.length ? visible.join('；') : '未作答'
}

function displayCorrectAnswer(answer?: QuestionAnswer): string {
  if (!answer) return '考试结束后公布'
  return answer.acceptedAnswers
    .map((group, index) => `${index + 1}. ${group.join(' / ')}`)
    .join('；')
}

async function load(): Promise<void> {
  loading.value = true
  loadError.value = ''
  try {
    result.value = await getExamResult(examId)
  } catch (error) {
    loadError.value = error instanceof Error ? error.message : '成绩加载失败'
    ElMessage.warning(loadError.value)
  } finally {
    loading.value = false
  }
}

onMounted(load)
</script>

<template>
  <section class="result-page">
    <div class="page-container">
      <RouterLink class="back-link" :to="`/exams/${examId}`">← 返回考试</RouterLink>
      <div v-loading="loading" class="result-shell">
        <el-empty v-if="!loading && !result" :description="loadError || '暂时没有可查看的成绩'">
          <el-button type="primary" @click="load">重新查询</el-button>
        </el-empty>
        <template v-else-if="result">
          <header class="result-hero" :class="{ passed: result.result.passed }">
            <div>
              <span>EXAM RESULT</span>
              <h1>{{ result.result.gradingCompleted ? '最终成绩' : '暂定成绩' }}</h1>
              <p v-if="!result.result.gradingCompleted">主观题尚未全部批改，分数可能继续变化。</p>
              <p v-else>{{ result.result.passed ? '恭喜，你已通过本场考试。' : '本次未达到及格分，请继续努力。' }}</p>
            </div>
            <div class="result-actions">
              <strong>{{ Number(result.result.totalScore).toFixed(2) }}<small> 分</small></strong>
              <RouterLink
                v-if="result.result.gradingCompleted && result.answersVisible"
                :to="`/exams/${examId}/result/ai-analysis`"
              >
                <el-button type="primary" plain>AI 分析</el-button>
              </RouterLink>
            </div>
          </header>

          <div class="summary-grid">
            <article><span>及格分</span><strong>{{ Number(result.result.passingScore).toFixed(2) }}</strong></article>
            <article><span>结果</span><strong>{{ result.result.passed ? '及格' : '未及格' }}</strong></article>
            <article><span>正确</span><strong>{{ result.result.correctCount }} 题</strong></article>
            <article><span>错误</span><strong>{{ result.result.incorrectCount }} 题</strong></article>
            <article><span>未作答</span><strong>{{ result.result.unansweredCount }} 题</strong></article>
          </div>

          <div class="answer-rule">
            <el-tag :type="result.answersVisible ? 'success' : 'info'">
              {{ result.answersVisible ? '正确答案与解析已公布' : '正确答案与解析暂未公布' }}
            </el-tag>
          </div>

          <article v-for="question in result.questions" :key="question.answerId" class="question-card">
            <header>
              <div>
                <el-tag size="small">{{ typeLabels[question.questionType] }}</el-tag>
                <strong>{{ question.sortOrder }}. {{ question.stem }}</strong>
              </div>
              <span>{{ question.score == null ? '待批改' : `${Number(question.score).toFixed(2)} / ${Number(question.maxScore).toFixed(2)} 分` }}</span>
            </header>
            <p><b>你的答案：</b>{{ displayAnswer(question.values, question.text) }}</p>
            <p v-if="result.answersVisible"><b>正确答案：</b>{{ displayCorrectAnswer(question.correctAnswer) }}</p>
            <p v-if="result.answersVisible && question.analysis" class="analysis"><b>解析：</b>{{ question.analysis }}</p>
            <p v-if="question.graderComment"><b>批改评语：</b>{{ question.graderComment }}</p>
          </article>
        </template>
      </div>
    </div>
  </section>
</template>

<style scoped>
.result-page { min-height: calc(100vh - 145px); padding: 34px 0 76px; background: #f5f7fb; }
.back-link { display: inline-block; margin-bottom: 18px; color: var(--lp-text-secondary); font-weight: 700; }
.result-shell { min-height: 420px; }
.result-hero { display: flex; border-radius: 22px; align-items: center; justify-content: space-between; color: #fff; background: linear-gradient(135deg, #344054, #667085); padding: 30px 36px; }
.result-hero.passed { background: linear-gradient(135deg, #176b4d, #2fa36b); }
.result-hero span { font-size: 12px; font-weight: 900; letter-spacing: .16em; }.result-hero h1 { margin: 8px 0; }.result-hero p { margin: 0; opacity: .84; }
.result-actions { display: flex; align-items: flex-end; flex-direction: column; gap: 12px; }
.result-actions > strong { font-size: 52px; }.result-hero small { font-size: 18px; }
.summary-grid { display: grid; gap: 14px; margin-top: 18px; grid-template-columns: repeat(5, 1fr); }
.summary-grid article { border: 1px solid var(--lp-border); border-radius: 14px; background: #fff; padding: 18px; }
.summary-grid span { color: var(--lp-text-secondary); font-size: 13px; }.summary-grid strong { display: block; margin-top: 8px; font-size: 20px; }
.answer-rule { margin: 22px 0 12px; }
.question-card { border: 1px solid var(--lp-border); border-radius: 16px; margin-top: 14px; background: #fff; padding: 22px; }
.question-card header, .question-card header > div { display: flex; align-items: flex-start; justify-content: space-between; gap: 14px; }
.question-card header > div { justify-content: flex-start; }.question-card header > span { color: var(--lp-primary); font-weight: 800; white-space: nowrap; }
.question-card p { margin: 14px 0 0; color: #475467; line-height: 1.8; white-space: pre-wrap; }.analysis { border-radius: 10px; background: #f6f8fc; padding: 12px; }
@media (max-width: 760px) {
  .result-hero, .question-card header { align-items: flex-start; flex-direction: column; }
  .summary-grid { grid-template-columns: 1fr 1fr; }
}
</style>
