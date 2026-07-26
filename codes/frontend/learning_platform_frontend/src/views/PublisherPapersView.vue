<script setup lang="ts">
import { ArrowDown, ArrowUp, Delete, Edit, Plus, View } from '@element-plus/icons-vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { computed, onMounted, reactive, ref } from 'vue'

import {
  createPaper,
  deletePaper,
  getPaper,
  listPapers,
  listQuestionBanks,
  listQuestions,
  replacePaperQuestions,
  updatePaper,
} from '@/api/exam'
import PublisherExamNav from '@/components/PublisherExamNav.vue'
import SectionPageHeader from '@/components/SectionPageHeader.vue'
import type {
  ExamPaperDetail,
  ExamPaperPage,
  ExamPaperStatus,
  ExamPaperSummary,
  Question,
  QuestionBank,
  QuestionPage,
  QuestionType,
} from '@/types/exam'

const typeLabels: Record<QuestionType, string> = {
  SINGLE_CHOICE: '单选',
  MULTIPLE_CHOICE: '多选',
  TRUE_FALSE: '判断',
  FILL_BLANK: '填空',
  SHORT_ANSWER: '简答',
}
const statusLabels: Record<ExamPaperStatus, string> = {
  DRAFT: '草稿',
  READY: '可用',
  ARCHIVED: '已归档',
}

interface SelectedQuestion {
  questionId: number
  stem: string
  questionType: QuestionType
  score: number
}

const loading = ref(false)
const page = ref<ExamPaperPage>({ items: [], total: 0, pageNumber: 1, pageSize: 10, totalPages: 0 })
const filters = reactive<{ keyword: string; status?: ExamPaperStatus; pageNumber: number }>({
  keyword: '',
  pageNumber: 1,
})

const editorVisible = ref(false)
const editorLoading = ref(false)
const saving = ref(false)
const editingPaperId = ref<number>()
const paperForm = reactive({ name: '', description: '' })
const banks = ref<QuestionBank[]>([])
const questionPage = ref<QuestionPage>({
  items: [],
  total: 0,
  pageNumber: 1,
  pageSize: 20,
  totalPages: 0,
})
const questionLoading = ref(false)
const questionFilters = reactive<{ bankId?: number; keyword: string; pageNumber: number }>({
  keyword: '',
  pageNumber: 1,
})
const selectedQuestions = ref<SelectedQuestion[]>([])

const previewVisible = ref(false)
const previewLoading = ref(false)
const preview = ref<ExamPaperDetail>()

const availableQuestions = computed(() => {
  const selectedIds = new Set(selectedQuestions.value.map((item) => item.questionId))
  return questionPage.value.items.filter((question) => !selectedIds.has(question.id))
})
const totalScore = computed(() =>
  selectedQuestions.value.reduce((sum, question) => sum + Number(question.score || 0), 0),
)

async function load(): Promise<void> {
  loading.value = true
  try {
    page.value = await listPapers({ ...filters, pageSize: 10 })
  } catch (error) {
    ElMessage.error(error instanceof Error ? error.message : '试卷加载失败')
  } finally {
    loading.value = false
  }
}

async function loadQuestionPool(): Promise<void> {
  questionLoading.value = true
  try {
    questionPage.value = await listQuestions({
      bankId: questionFilters.bankId,
      keyword: questionFilters.keyword.trim() || undefined,
      pageNumber: questionFilters.pageNumber,
      pageSize: 20,
    })
  } catch (error) {
    ElMessage.error(error instanceof Error ? error.message : '题目加载失败')
  } finally {
    questionLoading.value = false
  }
}

function searchQuestionPool(): void {
  questionFilters.pageNumber = 1
  void loadQuestionPool()
}

async function openEditor(paper?: ExamPaperSummary): Promise<void> {
  editorVisible.value = true
  editorLoading.value = true
  editingPaperId.value = paper?.id
  paperForm.name = paper?.name ?? ''
  paperForm.description = paper?.description ?? ''
  questionFilters.bankId = undefined
  questionFilters.keyword = ''
  questionFilters.pageNumber = 1
  selectedQuestions.value = []
  try {
    const [loadedBanks] = await Promise.all([listQuestionBanks(), loadQuestionPool()])
    banks.value = loadedBanks
    if (paper) {
      const detail = await getPaper(paper.id)
      selectedQuestions.value = detail.questions.map((item) => ({
        questionId: item.questionId,
        stem: item.stem,
        questionType: item.questionType,
        score: Number(item.score),
      }))
    }
  } catch (error) {
    ElMessage.error(error instanceof Error ? error.message : '试卷编辑数据加载失败')
  } finally {
    editorLoading.value = false
  }
}

function addQuestion(question: Question): void {
  selectedQuestions.value.push({
    questionId: question.id,
    stem: question.stem,
    questionType: question.questionType,
    score: Number(question.defaultScore),
  })
}

function moveQuestion(index: number, direction: -1 | 1): void {
  const target = index + direction
  if (target < 0 || target >= selectedQuestions.value.length) return
  const copy = [...selectedQuestions.value]
  const current = copy[index]
  const neighbor = copy[target]
  if (!current || !neighbor) return
  copy[index] = neighbor
  copy[target] = current
  selectedQuestions.value = copy
}

async function savePaper(): Promise<void> {
  if (!paperForm.name.trim() || selectedQuestions.value.length === 0) {
    ElMessage.warning('请填写试卷名称并至少选择一道题')
    return
  }
  if (selectedQuestions.value.some((item) => Number(item.score) <= 0)) {
    ElMessage.warning('每道题的分值必须大于0')
    return
  }
  saving.value = true
  try {
    const metadata = {
      name: paperForm.name.trim(),
      description: paperForm.description.trim(),
    }
    const saved = editingPaperId.value
      ? await updatePaper(editingPaperId.value, metadata)
      : await createPaper(metadata)
    const paperId = saved.paper.id
    await replacePaperQuestions(
      paperId,
      selectedQuestions.value.map((item, index) => ({
        questionId: item.questionId,
        sortOrder: index + 1,
        score: Number(item.score),
      })),
    )
    editorVisible.value = false
    await load()
    ElMessage.success(editingPaperId.value ? '试卷已更新' : '试卷已创建')
  } catch (error) {
    ElMessage.error(error instanceof Error ? error.message : '试卷保存失败')
  } finally {
    saving.value = false
  }
}

async function openPreview(paperId: number): Promise<void> {
  previewVisible.value = true
  previewLoading.value = true
  preview.value = undefined
  try {
    preview.value = await getPaper(paperId)
  } catch (error) {
    ElMessage.error(error instanceof Error ? error.message : '试卷预览加载失败')
  } finally {
    previewLoading.value = false
  }
}

async function removePaper(paper: ExamPaperSummary): Promise<void> {
  try {
    await ElMessageBox.confirm(
      `确定删除试卷“${paper.name}”吗？已被考试使用的试卷不能删除。`,
      '删除试卷',
      { type: 'warning' },
    )
    await deletePaper(paper.id)
    await load()
    ElMessage.success('试卷已删除')
  } catch (error) {
    if (error !== 'cancel') ElMessage.error(error instanceof Error ? error.message : '删除失败')
  }
}

function search(): void {
  filters.pageNumber = 1
  void load()
}

onMounted(load)
</script>

<template>
  <section class="exam-page">
    <div class="page-container">
      <PublisherExamNav />
      <SectionPageHeader
        eyebrow="EXAM PAPER"
        title="试卷"
        description="从自己的题库手工选题，设置顺序与分值并核对试卷快照"
      >
        <el-button type="primary" :icon="Plus" @click="openEditor()">新建试卷</el-button>
      </SectionPageHeader>

      <div class="toolbar">
        <el-input v-model="filters.keyword" clearable placeholder="搜索试卷名称" @keyup.enter="search" />
        <el-select v-model="filters.status" clearable placeholder="全部状态">
          <el-option v-for="(label, value) in statusLabels" :key="value" :label="label" :value="value" />
        </el-select>
        <el-button type="primary" @click="search">查询</el-button>
      </div>

      <div class="table-card">
        <el-table v-loading="loading" :data="page.items">
          <el-table-column label="试卷" min-width="280">
            <template #default="{ row }">
              <strong>{{ row.name }}</strong><small>{{ row.description || '暂无说明' }}</small>
            </template>
          </el-table-column>
          <el-table-column prop="questionCount" label="题数" width="90" />
          <el-table-column label="总分" width="100">
            <template #default="{ row }">{{ Number(row.totalScore).toFixed(2) }}</template>
          </el-table-column>
          <el-table-column label="状态" width="100">
            <template #default="{ row }">
              <el-tag :type="row.status === 'READY' ? 'success' : 'info'">{{ statusLabels[row.status as ExamPaperStatus] }}</el-tag>
            </template>
          </el-table-column>
          <el-table-column prop="updatedAt" label="更新时间" min-width="175" />
          <el-table-column label="操作" width="210" fixed="right">
            <template #default="{ row }">
              <el-button link type="primary" :icon="View" @click="openPreview(row.id)">预览</el-button>
              <el-button link type="primary" :icon="Edit" @click="openEditor(row as ExamPaperSummary)">编辑</el-button>
              <el-button link type="danger" :icon="Delete" @click="removePaper(row as ExamPaperSummary)">删除</el-button>
            </template>
          </el-table-column>
          <template #empty><el-empty description="暂无试卷，请先创建" /></template>
        </el-table>
      </div>
      <el-pagination
        v-if="page.total > page.pageSize"
        v-model:current-page="filters.pageNumber"
        class="pagination"
        layout="total, prev, pager, next"
        :page-size="page.pageSize"
        :total="page.total"
        @current-change="load"
      />
    </div>

    <el-dialog
      v-model="editorVisible"
      :title="editingPaperId ? '编辑试卷' : '新建试卷'"
      width="min(1100px, 94vw)"
      top="5vh"
    >
      <div v-loading="editorLoading">
        <div class="paper-meta">
          <el-form-item label="试卷名称" required><el-input v-model="paperForm.name" maxlength="200" /></el-form-item>
          <el-form-item label="试卷说明"><el-input v-model="paperForm.description" maxlength="1000" /></el-form-item>
        </div>
        <div class="paper-editor">
          <section class="question-pool">
            <div class="panel-title"><strong>可选题目</strong><span>共 {{ questionPage.total }} 道</span></div>
            <div class="question-filters">
              <el-select
                v-model="questionFilters.bankId"
                clearable
                placeholder="全部题库"
                @change="searchQuestionPool"
              >
                <el-option v-for="bank in banks" :key="bank.id" :label="bank.name" :value="bank.id" />
              </el-select>
              <el-input
                v-model="questionFilters.keyword"
                clearable
                placeholder="搜索题干"
                @keyup.enter="searchQuestionPool"
              />
              <el-button type="primary" @click="searchQuestionPool">查询</el-button>
            </div>
            <div v-loading="questionLoading" class="question-list">
              <button v-for="question in availableQuestions" :key="question.id" type="button" @click="addQuestion(question)">
                <el-tag size="small">{{ typeLabels[question.questionType] }}</el-tag>
                <span>{{ question.stem }}</span><strong>＋</strong>
              </button>
              <el-empty v-if="availableQuestions.length === 0" :image-size="60" description="没有可添加的题目" />
            </div>
            <el-pagination
              v-if="questionPage.total > questionPage.pageSize"
              v-model:current-page="questionFilters.pageNumber"
              class="question-pagination"
              small
              layout="prev, pager, next"
              :page-size="questionPage.pageSize"
              :total="questionPage.total"
              @current-change="loadQuestionPool"
            />
          </section>
          <section class="selected-panel">
            <div class="panel-title">
              <strong>已选题目</strong><span>{{ selectedQuestions.length }} 题 · {{ totalScore.toFixed(2) }} 分</span>
            </div>
            <div class="selected-list">
              <div v-for="(item, index) in selectedQuestions" :key="item.questionId" class="selected-item">
                <span class="order">{{ index + 1 }}</span>
                <div class="selected-stem"><el-tag size="small">{{ typeLabels[item.questionType] }}</el-tag><strong>{{ item.stem }}</strong></div>
                <el-input-number v-model="item.score" :min="0.01" :precision="2" size="small" />
                <div class="sort-actions">
                  <el-button link :icon="ArrowUp" :disabled="index === 0" @click="moveQuestion(index, -1)" />
                  <el-button link :icon="ArrowDown" :disabled="index === selectedQuestions.length - 1" @click="moveQuestion(index, 1)" />
                  <el-button link type="danger" :icon="Delete" @click="selectedQuestions.splice(index, 1)" />
                </div>
              </div>
              <el-empty v-if="selectedQuestions.length === 0" description="从左侧添加题目" />
            </div>
          </section>
        </div>
      </div>
      <template #footer>
        <el-button @click="editorVisible = false">取消</el-button>
        <el-button type="primary" :loading="saving" @click="savePaper">保存试卷</el-button>
      </template>
    </el-dialog>

    <el-drawer v-model="previewVisible" title="试卷预览" size="680px">
      <div v-loading="previewLoading" class="preview">
        <template v-if="preview">
          <h2>{{ preview.paper.name }}</h2>
          <p>{{ preview.paper.description || '暂无说明' }}</p>
          <el-descriptions :column="2" border>
            <el-descriptions-item label="题目数量">{{ preview.paper.questionCount }}</el-descriptions-item>
            <el-descriptions-item label="试卷总分">{{ Number(preview.paper.totalScore).toFixed(2) }}</el-descriptions-item>
          </el-descriptions>
          <article v-for="question in preview.questions" :key="question.id" class="preview-question">
            <h3>{{ question.sortOrder }}. {{ question.stem }} <small>（{{ question.score }} 分）</small></h3>
            <p v-for="option in question.options" :key="option.key">{{ option.key }}. {{ option.text }}</p>
            <div class="answer">答案：{{ question.answer.acceptedAnswers.map((group) => group.join(' / ')).join('；') }}</div>
            <div class="analysis">解析：{{ question.analysis || '暂无解析' }}</div>
          </article>
        </template>
      </div>
    </el-drawer>
  </section>
</template>

<style scoped>
.exam-page { min-height: calc(100vh - 145px); padding: 42px 0 76px; background: linear-gradient(180deg, #f4f7ff, #f8f9fc 330px); }
.toolbar { display: grid; max-width: 760px; gap: 12px; margin-bottom: 18px; grid-template-columns: 1fr 170px auto; }
.table-card { overflow: hidden; border: 1px solid var(--lp-border); border-radius: 18px; background: #fff; box-shadow: var(--lp-shadow); padding: 8px 16px 16px; }
.table-card strong, .table-card small { display: block; }.table-card small { margin-top: 6px; color: #98a2b3; }
.pagination { justify-content: center; margin-top: 24px; }
.paper-meta { display: grid; gap: 16px; grid-template-columns: 1fr 1fr; }
.paper-editor { display: grid; min-height: 510px; gap: 16px; grid-template-columns: 38% 62%; }
.question-pool, .selected-panel { overflow: hidden; border: 1px solid var(--lp-border); border-radius: 14px; padding: 16px; }
.panel-title { display: flex; align-items: center; justify-content: space-between; margin-bottom: 12px; }.panel-title span { color: var(--lp-text-secondary); font-size: 12px; }
.question-filters { display: grid; gap: 8px; grid-template-columns: 130px minmax(150px, 1fr) auto; }
.question-list, .selected-list { overflow-y: auto; max-height: 440px; margin-top: 12px; }
.question-pagination { justify-content: center; margin-top: 12px; }
.question-list button { display: grid; width: 100%; border: 0; border-bottom: 1px solid var(--lp-border); align-items: center; gap: 8px; background: #fff; cursor: pointer; grid-template-columns: auto 1fr auto; padding: 12px 4px; text-align: left; }
.question-list button:hover { background: #f8faff; }.question-list button span { overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }
.selected-item { display: grid; border-bottom: 1px solid var(--lp-border); align-items: center; gap: 9px; grid-template-columns: 28px minmax(180px, 1fr) 125px 92px; padding: 12px 0; }
.order { display: grid; width: 26px; height: 26px; border-radius: 50%; place-items: center; color: var(--lp-primary); background: #eff6ff; font-size: 12px; font-weight: 800; }
.selected-stem { min-width: 0; }.selected-stem strong { display: block; overflow: hidden; margin-top: 5px; font-size: 13px; text-overflow: ellipsis; white-space: nowrap; }
.sort-actions { display: flex; }.preview { min-height: 260px; }.preview > p { color: var(--lp-text-secondary); }
.preview-question { border-top: 1px solid var(--lp-border); margin-top: 24px; padding-top: 18px; }.preview-question h3 { font-size: 16px; line-height: 1.7; }.preview-question h3 small { color: var(--lp-primary); }
.preview-question p { margin-left: 18px; color: #475467; }.answer, .analysis { border-radius: 8px; margin-top: 8px; background: #f7f9fc; padding: 10px 12px; }.analysis { color: var(--lp-text-secondary); }
@media (max-width: 800px) { .toolbar, .paper-meta, .paper-editor, .question-filters { grid-template-columns: 1fr; }.paper-editor { min-height: auto; }.selected-item { grid-template-columns: 28px 1fr; }.selected-item > .el-input-number, .sort-actions { grid-column: 2; } }
</style>
