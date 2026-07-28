<script setup lang="ts">
import { Delete, Edit, Plus, Promotion } from '@element-plus/icons-vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { onMounted, reactive, ref } from 'vue'

import {
  cancelExam,
  createExam,
  deleteExam,
  getExamQuota,
  getPublisherExam,
  listPapers,
  listPublisherExams,
  publishExam,
  searchExamCandidatePage,
  updateExam,
} from '@/api/exam'
import PublisherExamNav from '@/components/PublisherExamNav.vue'
import ClassPickerDialog from '@/components/ClassPickerDialog.vue'
import SectionPageHeader from '@/components/SectionPageHeader.vue'
import type {
  ExamCandidateOption,
  ExamCandidatePage,
  ExamPage,
  ExamPaperPage,
  ExamPaperSummary,
  ExamStatus,
  ExamSummary,
  ExamWritePayload,
} from '@/types/exam'

const statusLabels: Record<ExamStatus, string> = {
  DRAFT: '草稿',
  PUBLISHED: '已发布',
  ONGOING: '进行中',
  FINISHED: '已结束',
  CANCELLED: '已取消',
}

const loading = ref(false)
const page = ref<ExamPage>({ items: [], total: 0, pageNumber: 1, pageSize: 10, totalPages: 0 })
const filters = reactive<{ keyword: string; status?: ExamStatus; pageNumber: number }>({
  keyword: '',
  pageNumber: 1,
})
const quota = ref(0)

const editorVisible = ref(false)
const editorLoading = ref(false)
const saving = ref(false)
const editingExamId = ref<number>()
const selectedPaper = ref<ExamPaperSummary>()
const selectedCandidates = ref<ExamCandidateOption[]>([])

const paperPickerVisible = ref(false)
const paperPickerLoading = ref(false)
const paperPage = ref<ExamPaperPage>({
  items: [],
  total: 0,
  pageNumber: 1,
  pageSize: 10,
  totalPages: 0,
})
const paperSearch = reactive({ keyword: '', pageNumber: 1 })

const candidatePickerVisible = ref(false)
const candidatePickerLoading = ref(false)
const candidatePage = ref<ExamCandidatePage>({
  items: [],
  total: 0,
  pageNumber: 1,
  pageSize: 10,
  totalPages: 0,
})
const candidateSearch = reactive({ keyword: '', pageNumber: 1 })
const candidateDraft = ref<Map<number, ExamCandidateOption>>(new Map())
const form = reactive<ExamWritePayload>({
  paperId: 0,
  name: '',
  instructions: '',
  assignmentMode: 'INDIVIDUAL',
  classIds: [],
  startAt: '',
  endAt: '',
  durationMinutes: 60,
  passingScore: 60,
  showResultImmediately: false,
  showAnswerAfterFinish: true,
  candidateUserIds: [],
})

function localDateTime(offsetMinutes: number): string {
  const date = new Date(Date.now() + offsetMinutes * 60_000)
  const pad = (value: number) => String(value).padStart(2, '0')
  return `${date.getFullYear()}-${pad(date.getMonth() + 1)}-${pad(date.getDate())}T${pad(date.getHours())}:${pad(date.getMinutes())}:00`
}

function displayTime(value: string): string {
  return new Date(value).toLocaleString('zh-CN', { hour12: false })
}

function statusTagType(status: ExamStatus): 'success' | 'warning' | 'info' | 'danger' | 'primary' {
  if (status === 'PUBLISHED' || status === 'ONGOING') return 'success'
  if (status === 'DRAFT') return 'info'
  if (status === 'CANCELLED') return 'danger'
  return 'warning'
}

async function load(): Promise<void> {
  loading.value = true
  try {
    const [examPage, availableQuota] = await Promise.all([
      listPublisherExams({ ...filters, pageSize: 10 }),
      getExamQuota(),
    ])
    page.value = examPage
    quota.value = availableQuota
  } catch (error) {
    ElMessage.error(error instanceof Error ? error.message : '考试列表加载失败')
  } finally {
    loading.value = false
  }
}

async function loadPaperOptions(): Promise<void> {
  paperPickerLoading.value = true
  try {
    paperPage.value = await listPapers({
      status: 'READY',
      keyword: paperSearch.keyword.trim() || undefined,
      pageNumber: paperSearch.pageNumber,
      pageSize: 10,
    })
  } catch (error) {
    ElMessage.error(error instanceof Error ? error.message : '试卷搜索失败')
  } finally {
    paperPickerLoading.value = false
  }
}

function openPaperPicker(): void {
  paperSearch.keyword = ''
  paperSearch.pageNumber = 1
  paperPickerVisible.value = true
  void loadPaperOptions()
}

function searchPapers(): void {
  paperSearch.pageNumber = 1
  void loadPaperOptions()
}

function choosePaper(paper: ExamPaperSummary): void {
  selectedPaper.value = paper
  form.paperId = paper.id
  paperPickerVisible.value = false
}

async function loadCandidateOptions(): Promise<void> {
  candidatePickerLoading.value = true
  try {
    candidatePage.value = await searchExamCandidatePage({
      keyword: candidateSearch.keyword.trim() || undefined,
      pageNumber: candidateSearch.pageNumber,
      pageSize: 10,
    })
  } catch (error) {
    ElMessage.error(error instanceof Error ? error.message : '考生搜索失败')
  } finally {
    candidatePickerLoading.value = false
  }
}

function openCandidatePicker(): void {
  candidateDraft.value = new Map(selectedCandidates.value.map((item) => [item.id, item]))
  candidateSearch.keyword = ''
  candidateSearch.pageNumber = 1
  candidatePickerVisible.value = true
  void loadCandidateOptions()
}

function searchCandidateOptions(): void {
  candidateSearch.pageNumber = 1
  void loadCandidateOptions()
}

function toggleCandidate(candidate: ExamCandidateOption, checked: boolean): void {
  if (checked) candidateDraft.value.set(candidate.id, candidate)
  else candidateDraft.value.delete(candidate.id)
}

function confirmCandidates(): void {
  selectedCandidates.value = [...candidateDraft.value.values()]
  form.candidateUserIds = selectedCandidates.value.map((candidate) => candidate.id)
  candidatePickerVisible.value = false
}

function removeSelectedCandidate(userId: number): void {
  selectedCandidates.value = selectedCandidates.value.filter((candidate) => candidate.id !== userId)
  form.candidateUserIds = selectedCandidates.value.map((candidate) => candidate.id)
}

async function openEditor(exam?: ExamSummary): Promise<void> {
  editorVisible.value = true
  editorLoading.value = true
  editingExamId.value = exam?.id
  form.paperId = exam?.paperId ?? 0
  form.name = exam?.name ?? ''
  form.instructions = ''
  form.assignmentMode = exam?.assignmentMode ?? 'INDIVIDUAL'
  form.classIds = []
  form.startAt = exam?.startAt ?? localDateTime(60)
  form.endAt = exam?.endAt ?? localDateTime(180)
  form.durationMinutes = exam?.durationMinutes ?? 60
  form.passingScore = Number(exam?.passingScore ?? 60)
  form.showResultImmediately = exam?.showResultImmediately ?? false
  form.showAnswerAfterFinish = exam?.showAnswerAfterFinish ?? true
  form.candidateUserIds = []
  selectedPaper.value = undefined
  selectedCandidates.value = []
  try {
    if (exam) {
      const detail = await getPublisherExam(exam.id)
      form.instructions = detail.instructions ?? ''
      form.assignmentMode = detail.exam.assignmentMode ?? 'INDIVIDUAL'
      form.classIds = [...(detail.classIds ?? [])]
      selectedPaper.value = detail.paper
      selectedCandidates.value = detail.candidates.map((candidate) => ({
        id: candidate.userId,
        username: candidate.username,
        nickname: candidate.nickname,
      }))
      form.candidateUserIds = selectedCandidates.value.map((candidate) => candidate.id)
    }
  } catch (error) {
    ElMessage.error(error instanceof Error ? error.message : '考试编辑数据加载失败')
  } finally {
    editorLoading.value = false
  }
}

async function save(): Promise<void> {
  if (
    !form.paperId ||
    !form.name.trim() ||
    !form.startAt ||
    !form.endAt ||
    (form.assignmentMode === 'INDIVIDUAL' && form.candidateUserIds.length === 0) ||
    (form.assignmentMode === 'CLASS' && form.classIds.length === 0)
  ) {
    ElMessage.warning('请完整填写试卷、考试名称、时间和发放对象')
    return
  }
  saving.value = true
  try {
    const payload: ExamWritePayload = {
      ...form,
      name: form.name.trim(),
      instructions: form.instructions.trim(),
      durationMinutes: Number(form.durationMinutes),
      passingScore: Number(form.passingScore),
      candidateUserIds: [...form.candidateUserIds],
      classIds: [...form.classIds],
    }
    if (editingExamId.value) await updateExam(editingExamId.value, payload)
    else await createExam(payload)
    editorVisible.value = false
    await load()
    ElMessage.success(editingExamId.value ? '考试已更新' : '考试草稿已创建')
  } catch (error) {
    ElMessage.error(error instanceof Error ? error.message : '考试保存失败')
  } finally {
    saving.value = false
  }
}

async function publish(exam: ExamSummary): Promise<void> {
  try {
    await ElMessageBox.confirm(
      `发布“${exam.name}”将消耗 1 次考试发布额度，当前余额 ${quota.value} 次。确定继续吗？`,
      '发布考试',
      { type: 'warning' },
    )
    await publishExam(exam.id)
    ElMessage.success('考试已发布')
    await load()
  } catch (error) {
    if (error !== 'cancel') ElMessage.error(error instanceof Error ? error.message : '发布失败')
  }
}

async function cancel(exam: ExamSummary): Promise<void> {
  try {
    await ElMessageBox.confirm('取消后考生将无法参加，已扣额度不会返还。确定继续吗？', '取消考试', {
      type: 'warning',
    })
    await cancelExam(exam.id)
    ElMessage.success('考试已取消')
    await load()
  } catch (error) {
    if (error !== 'cancel') ElMessage.error(error instanceof Error ? error.message : '取消失败')
  }
}

async function remove(exam: ExamSummary): Promise<void> {
  try {
    await ElMessageBox.confirm(`确定删除草稿考试“${exam.name}”吗？`, '删除考试', {
      type: 'warning',
    })
    await deleteExam(exam.id)
    ElMessage.success('考试已删除')
    await load()
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
        eyebrow="EXAM PUBLISHING"
        title="考试管理"
        description="配置考试时间、答题时长、及格分、成绩规则和指定考生"
      >
        <el-tag size="large" type="success">发布额度：{{ quota }} 次</el-tag>
        <RouterLink :to="{ path: '/commerce', query: { type: 'EXAM_PACKAGE' } }">
          <el-button type="warning">购买发布次数</el-button>
        </RouterLink>
        <el-button type="primary" :icon="Plus" @click="openEditor()">新建考试</el-button>
      </SectionPageHeader>

      <div class="toolbar">
        <el-input v-model="filters.keyword" clearable placeholder="搜索考试名称" @keyup.enter="search" />
        <el-select v-model="filters.status" clearable placeholder="全部状态">
          <el-option v-for="(label, value) in statusLabels" :key="value" :label="label" :value="value" />
        </el-select>
        <el-button type="primary" @click="search">查询</el-button>
      </div>

      <div class="table-card">
        <el-table v-loading="loading" :data="page.items">
          <el-table-column label="考试" min-width="250">
            <template #default="{ row }"><strong>{{ row.name }}</strong><small>试卷 ID：{{ row.paperId }}</small></template>
          </el-table-column>
          <el-table-column label="考试时间" min-width="260">
            <template #default="{ row }">
              {{ displayTime(row.startAt) }}<br /><small>至 {{ displayTime(row.endAt) }}</small>
            </template>
          </el-table-column>
          <el-table-column label="时长" width="90"><template #default="{ row }">{{ row.durationMinutes }} 分钟</template></el-table-column>
          <el-table-column label="考生范围" width="105">
            <template #default="{ row }">{{ row.assignmentMode === 'CLASS' ? '指定班级' : '指定考生' }}</template>
          </el-table-column>
          <el-table-column label="及格分" width="90"><template #default="{ row }">{{ Number(row.passingScore).toFixed(2) }}</template></el-table-column>
          <el-table-column label="状态" width="120">
            <template #default="{ row }">
              <el-tag :type="statusTagType(row.status)">{{ statusLabels[row.status as ExamStatus] }}</el-tag>
            </template>
          </el-table-column>
          <el-table-column label="操作" width="300">
            <template #default="{ row }">
              <el-button v-if="row.status === 'DRAFT'" link type="primary" :icon="Edit" @click="openEditor(row as ExamSummary)">编辑</el-button>
              <el-button v-if="row.status === 'DRAFT'" link type="success" :icon="Promotion" @click="publish(row as ExamSummary)">发布</el-button>
              <el-button v-if="row.status === 'PUBLISHED'" link type="warning" @click="cancel(row as ExamSummary)">取消</el-button>
              <RouterLink
                v-if="row.status !== 'DRAFT' && row.status !== 'CANCELLED'"
                class="grading-link"
                :to="`/publisher/exams/${row.id}/grading`"
              >
                阅卷/统计
              </RouterLink>
              <el-button v-if="row.status === 'DRAFT'" link type="danger" :icon="Delete" @click="remove(row as ExamSummary)">删除</el-button>
            </template>
          </el-table-column>
          <template #empty><el-empty description="暂无考试，请先创建" /></template>
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
      :title="editingExamId ? '编辑考试' : '新建考试'"
      width="min(760px, 94vw)"
      top="5vh"
    >
      <el-form v-loading="editorLoading" label-position="top">
        <div class="form-grid">
          <el-form-item label="考试名称" required>
            <el-input v-model="form.name" maxlength="200" />
          </el-form-item>
          <el-form-item label="试卷" required>
            <button type="button" class="picker-trigger" @click="openPaperPicker">
              <template v-if="selectedPaper">
                <strong>{{ selectedPaper.name }}</strong>
                <span>{{ selectedPaper.questionCount }} 题 · {{ Number(selectedPaper.totalScore).toFixed(2) }} 分</span>
              </template>
              <span v-else>请选择可用试卷</span>
              <b>选择</b>
            </button>
          </el-form-item>
          <el-form-item label="开始时间" required>
            <el-date-picker v-model="form.startAt" type="datetime" value-format="YYYY-MM-DDTHH:mm:ss" />
          </el-form-item>
          <el-form-item label="结束时间" required>
            <el-date-picker v-model="form.endAt" type="datetime" value-format="YYYY-MM-DDTHH:mm:ss" />
          </el-form-item>
          <el-form-item label="答题时长（分钟）" required>
            <el-input-number v-model="form.durationMinutes" :min="1" :max="10080" />
          </el-form-item>
          <el-form-item label="及格分" required>
            <el-input-number v-model="form.passingScore" :min="0" :precision="2" />
          </el-form-item>
        </div>
        <el-form-item label="考试对象" required>
          <el-radio-group v-model="form.assignmentMode">
            <el-radio-button value="INDIVIDUAL">指定考生</el-radio-button>
            <el-radio-button value="CLASS">指定班级</el-radio-button>
          </el-radio-group>
        </el-form-item>
        <el-form-item v-if="form.assignmentMode === 'INDIVIDUAL'" label="指定考生" required>
          <div class="candidate-field">
            <el-button type="primary" plain @click="openCandidatePicker">
              选择考生（已选 {{ selectedCandidates.length }} 人）
            </el-button>
            <div v-if="selectedCandidates.length" class="selected-candidates">
              <el-tag
                v-for="candidate in selectedCandidates"
                :key="candidate.id"
                closable
                @close="removeSelectedCandidate(candidate.id)"
              >
                {{ candidate.nickname }}（{{ candidate.username }}）
              </el-tag>
            </div>
            <span v-else class="picker-placeholder">尚未选择考生</span>
          </div>
        </el-form-item>
        <el-form-item v-else label="指定班级" required>
          <ClassPickerDialog v-model="form.classIds" />
        </el-form-item>
        <el-form-item label="考试说明">
          <el-input v-model="form.instructions" type="textarea" :rows="4" maxlength="5000" />
        </el-form-item>
        <div class="rule-switches">
          <el-switch v-model="form.showResultImmediately" active-text="提交后立即显示成绩" />
          <el-switch v-model="form.showAnswerAfterFinish" active-text="考试结束后显示答案解析" />
        </div>
      </el-form>
      <template #footer>
        <el-button @click="editorVisible = false">取消</el-button>
        <el-button type="primary" :loading="saving" @click="save">保存草稿</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="paperPickerVisible" title="选择试卷" width="min(820px, 94vw)" top="7vh">
      <div class="picker-search">
        <el-input
          v-model="paperSearch.keyword"
          clearable
          placeholder="搜索试卷名称"
          @keyup.enter="searchPapers"
        />
        <el-button type="primary" @click="searchPapers">查询</el-button>
      </div>
      <el-table v-loading="paperPickerLoading" :data="paperPage.items" height="430">
        <el-table-column label="试卷" min-width="300">
          <template #default="{ row }">
            <strong>{{ row.name }}</strong>
            <small>{{ row.description || '暂无说明' }}</small>
          </template>
        </el-table-column>
        <el-table-column prop="questionCount" label="题目数" width="90" />
        <el-table-column label="总分" width="100">
          <template #default="{ row }">{{ Number(row.totalScore).toFixed(2) }}</template>
        </el-table-column>
        <el-table-column label="操作" width="90">
          <template #default="{ row }">
            <el-button link type="primary" @click="choosePaper(row as ExamPaperSummary)">选择</el-button>
          </template>
        </el-table-column>
        <template #empty><el-empty description="没有匹配的可用试卷" /></template>
      </el-table>
      <el-pagination
        v-if="paperPage.total > paperPage.pageSize"
        v-model:current-page="paperSearch.pageNumber"
        class="picker-pagination"
        layout="total, prev, pager, next"
        :page-size="paperPage.pageSize"
        :total="paperPage.total"
        @current-change="loadPaperOptions"
      />
    </el-dialog>

    <el-dialog
      v-model="candidatePickerVisible"
      title="选择考生"
      width="min(820px, 94vw)"
      top="7vh"
    >
      <div class="picker-search">
        <el-input
          v-model="candidateSearch.keyword"
          clearable
          placeholder="搜索用户名或昵称"
          @keyup.enter="searchCandidateOptions"
        />
        <el-button type="primary" @click="searchCandidateOptions">查询</el-button>
      </div>
      <div class="picker-selection-count">当前已选择 {{ candidateDraft.size }} 人，切换搜索或页码不会丢失选择。</div>
      <el-table v-loading="candidatePickerLoading" :data="candidatePage.items" height="400">
        <el-table-column label="选择" width="75">
          <template #default="{ row }">
            <el-checkbox
              :model-value="candidateDraft.has(row.id)"
              @change="toggleCandidate(row as ExamCandidateOption, Boolean($event))"
            />
          </template>
        </el-table-column>
        <el-table-column prop="username" label="用户名" min-width="220" />
        <el-table-column prop="nickname" label="昵称" min-width="220" />
        <template #empty><el-empty description="没有匹配的用户" /></template>
      </el-table>
      <el-pagination
        v-if="candidatePage.total > candidatePage.pageSize"
        v-model:current-page="candidateSearch.pageNumber"
        class="picker-pagination"
        layout="total, prev, pager, next"
        :page-size="candidatePage.pageSize"
        :total="candidatePage.total"
        @current-change="loadCandidateOptions"
      />
      <template #footer>
        <el-button @click="candidatePickerVisible = false">取消</el-button>
        <el-button type="primary" @click="confirmCandidates">确认选择</el-button>
      </template>
    </el-dialog>
  </section>
</template>

<style scoped>
.exam-page { min-height: calc(100vh - 145px); padding: 42px 0 76px; background: linear-gradient(180deg, #f4f7ff, #f8f9fc 330px); }
.toolbar { display: grid; max-width: 760px; gap: 12px; margin-bottom: 18px; grid-template-columns: 1fr 170px auto; }
.table-card { overflow: hidden; border: 1px solid var(--lp-border); border-radius: 18px; background: #fff; box-shadow: var(--lp-shadow); padding: 8px 16px 16px; }
.table-card strong, .table-card small { display: block; }.table-card small { margin-top: 5px; color: #98a2b3; }
.pagination { justify-content: center; margin-top: 24px; }
.form-grid { display: grid; gap: 0 18px; grid-template-columns: 1fr 1fr; }
.form-grid .el-select, .form-grid .el-date-editor, .form-grid .el-input-number, .el-form-item > .el-select { width: 100%; }
.picker-trigger { display: grid; width: 100%; min-height: 40px; border: 1px solid var(--lp-border); border-radius: 8px; align-items: center; gap: 2px 10px; background: #fff; cursor: pointer; grid-template-columns: minmax(0, 1fr) auto; padding: 7px 11px; text-align: left; }
.picker-trigger:hover { border-color: var(--lp-primary); }.picker-trigger strong, .picker-trigger span { overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }.picker-trigger span { color: var(--lp-text-secondary); font-size: 12px; }.picker-trigger b { color: var(--lp-primary); font-size: 14px; grid-column: 2; grid-row: 1 / span 2; }
.candidate-field { width: 100%; }.selected-candidates { display: flex; flex-wrap: wrap; gap: 8px; margin-top: 10px; }.picker-placeholder { display: block; margin-top: 8px; color: var(--lp-text-secondary); font-size: 13px; }
.picker-search { display: grid; gap: 10px; margin-bottom: 14px; grid-template-columns: 1fr auto; }.picker-pagination { justify-content: center; margin-top: 16px; }.picker-selection-count { margin: -2px 0 12px; color: var(--lp-text-secondary); font-size: 13px; }
.rule-switches { display: flex; flex-wrap: wrap; gap: 28px; border-radius: 10px; background: #f7f9fc; padding: 14px 16px; }
.grading-link { margin-left: 12px; color: var(--lp-primary); font-size: 14px; font-weight: 700; }
@media (max-width: 720px) { .toolbar, .form-grid, .picker-search { grid-template-columns: 1fr; } }
</style>
