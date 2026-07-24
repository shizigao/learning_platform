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
  searchExamCandidates,
  updateExam,
} from '@/api/exam'
import PublisherExamNav from '@/components/PublisherExamNav.vue'
import SectionPageHeader from '@/components/SectionPageHeader.vue'
import type {
  ExamCandidateOption,
  ExamPage,
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
const readyPapers = ref<ExamPaperSummary[]>([])
const candidateOptions = ref<ExamCandidateOption[]>([])
const candidateSearching = ref(false)
const form = reactive<ExamWritePayload>({
  paperId: 0,
  name: '',
  instructions: '',
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

async function loadEditorOptions(): Promise<void> {
  const [papers, candidates] = await Promise.all([
    listPapers({ status: 'READY', pageNumber: 1, pageSize: 100 }),
    searchExamCandidates(),
  ])
  readyPapers.value = papers.items
  candidateOptions.value = candidates
}

async function searchCandidates(keyword: string): Promise<void> {
  candidateSearching.value = true
  try {
    const result = await searchExamCandidates(keyword)
    const selected = candidateOptions.value.filter((item) => form.candidateUserIds.includes(item.id))
    const merged = new Map([...selected, ...result].map((item) => [item.id, item]))
    candidateOptions.value = [...merged.values()]
  } catch (error) {
    ElMessage.error(error instanceof Error ? error.message : '考生搜索失败')
  } finally {
    candidateSearching.value = false
  }
}

async function openEditor(exam?: ExamSummary): Promise<void> {
  editorVisible.value = true
  editorLoading.value = true
  editingExamId.value = exam?.id
  form.paperId = exam?.paperId ?? 0
  form.name = exam?.name ?? ''
  form.instructions = ''
  form.startAt = exam?.startAt ?? localDateTime(60)
  form.endAt = exam?.endAt ?? localDateTime(180)
  form.durationMinutes = exam?.durationMinutes ?? 60
  form.passingScore = Number(exam?.passingScore ?? 60)
  form.showResultImmediately = exam?.showResultImmediately ?? false
  form.showAnswerAfterFinish = exam?.showAnswerAfterFinish ?? true
  form.candidateUserIds = []
  try {
    await loadEditorOptions()
    if (exam) {
      const detail = await getPublisherExam(exam.id)
      form.instructions = detail.instructions ?? ''
      form.candidateUserIds = detail.candidates.map((candidate) => candidate.userId)
      const existing = detail.candidates.map((candidate) => ({
        id: candidate.userId,
        username: candidate.username,
        nickname: candidate.nickname,
      }))
      const merged = new Map([...existing, ...candidateOptions.value].map((item) => [item.id, item]))
      candidateOptions.value = [...merged.values()]
    } else if (readyPapers.value.length) {
      form.paperId = readyPapers.value[0]!.id
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
    form.candidateUserIds.length === 0
  ) {
    ElMessage.warning('请完整填写试卷、考试名称、时间并指定至少一名考生')
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
          <el-button type="warning">模拟购买发布次数</el-button>
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
          <el-table-column label="及格分" width="90"><template #default="{ row }">{{ Number(row.passingScore).toFixed(2) }}</template></el-table-column>
          <el-table-column label="状态" width="105">
            <template #default="{ row }">
              <el-tag :type="statusTagType(row.status)">{{ statusLabels[row.status as ExamStatus] }}</el-tag>
            </template>
          </el-table-column>
          <el-table-column label="操作" width="300" fixed="right">
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
          <el-form-item label="固定试卷" required>
            <el-select v-model="form.paperId" placeholder="请选择已完成组卷的试卷">
              <el-option
                v-for="paper in readyPapers"
                :key="paper.id"
                :label="`${paper.name}（${paper.questionCount}题 / ${paper.totalScore}分）`"
                :value="paper.id"
              />
            </el-select>
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
        <el-form-item label="指定考生" required>
          <el-select
            v-model="form.candidateUserIds"
            multiple
            filterable
            remote
            reserve-keyword
            :remote-method="searchCandidates"
            :loading="candidateSearching"
            placeholder="输入用户名或昵称搜索，可多选"
          >
            <el-option
              v-for="candidate in candidateOptions"
              :key="candidate.id"
              :label="`${candidate.nickname}（${candidate.username}）`"
              :value="candidate.id"
            />
          </el-select>
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
.rule-switches { display: flex; flex-wrap: wrap; gap: 28px; border-radius: 10px; background: #f7f9fc; padding: 14px 16px; }
.grading-link { margin-left: 12px; color: var(--lp-primary); font-size: 14px; font-weight: 700; }
@media (max-width: 720px) { .toolbar, .form-grid { grid-template-columns: 1fr; } }
</style>
