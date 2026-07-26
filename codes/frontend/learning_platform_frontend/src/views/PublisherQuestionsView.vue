<script setup lang="ts">
import { Delete, Edit, Plus } from '@element-plus/icons-vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { computed, onMounted, reactive, ref } from 'vue'

import {
  createQuestion,
  createQuestionBank,
  deleteQuestion,
  deleteQuestionBank,
  listQuestionBanks,
  listQuestions,
  updateQuestion,
  updateQuestionBank,
} from '@/api/exam'
import PublisherExamNav from '@/components/PublisherExamNav.vue'
import SectionPageHeader from '@/components/SectionPageHeader.vue'
import type {
  Question,
  QuestionBank,
  QuestionOption,
  QuestionPage,
  QuestionType,
  QuestionWritePayload,
} from '@/types/exam'

const typeLabels: Record<QuestionType, string> = {
  SINGLE_CHOICE: '单选题',
  MULTIPLE_CHOICE: '多选题',
  TRUE_FALSE: '判断题',
  FILL_BLANK: '填空题',
  SHORT_ANSWER: '简答题',
}

interface EditorForm {
  bankId: number
  questionType: QuestionType
  stem: string
  options: Array<Omit<QuestionOption, 'id'>>
  singleAnswer: string
  multipleAnswers: string[]
  truthAnswer: 'TRUE' | 'FALSE'
  blankAnswers: string[]
  shortAnswer: string
  analysis: string
  defaultScore: number
  fillBlankAutoGradable: boolean
  caseSensitive: boolean
}

const banks = ref<QuestionBank[]>([])
const page = ref<QuestionPage>({ items: [], total: 0, pageNumber: 1, pageSize: 12, totalPages: 0 })
const loading = ref(false)
const filters = reactive<{
  bankId?: number
  questionType?: QuestionType
  keyword: string
  pageNumber: number
}>({ keyword: '', pageNumber: 1 })
const selectedBank = computed(() => banks.value.find((bank) => bank.id === filters.bankId))
const bankPickerVisible = ref(false)
const bankKeyword = ref('')
const filteredBanks = computed(() => {
  const keyword = bankKeyword.value.trim().toLowerCase()
  if (!keyword) return banks.value
  return banks.value.filter(
    (bank) =>
      bank.name.toLowerCase().includes(keyword) ||
      (bank.description ?? '').toLowerCase().includes(keyword),
  )
})
const deleteBankDialogVisible = ref(false)
const deleteBankId = ref<number>()

const bankDialogVisible = ref(false)
const bankSaving = ref(false)
const editingBankId = ref<number>()
const bankForm = reactive({ name: '', description: '', status: 'ACTIVE' as const })

const editorVisible = ref(false)
const saving = ref(false)
const editingQuestionId = ref<number>()
const form = reactive<EditorForm>({
  bankId: 0,
  questionType: 'SINGLE_CHOICE',
  stem: '',
  options: [
    { key: 'A', text: '', sortOrder: 0 },
    { key: 'B', text: '', sortOrder: 1 },
  ],
  singleAnswer: 'A',
  multipleAnswers: [],
  truthAnswer: 'TRUE',
  blankAnswers: [''],
  shortAnswer: '',
  analysis: '',
  defaultScore: 1,
  fillBlankAutoGradable: false,
  caseSensitive: false,
})

const isChoice = computed(() =>
  ['SINGLE_CHOICE', 'MULTIPLE_CHOICE'].includes(form.questionType),
)
const answerOptionKeys = computed(() => form.options.map((option) => option.key.trim().toUpperCase()))

async function loadBanks(): Promise<void> {
  banks.value = await listQuestionBanks()
  if (filters.bankId && !banks.value.some((bank) => bank.id === filters.bankId)) {
    filters.bankId = undefined
  }
}

async function loadQuestions(): Promise<void> {
  if (!filters.bankId) {
    page.value = { items: [], total: 0, pageNumber: 1, pageSize: 12, totalPages: 0 }
    return
  }
  loading.value = true
  try {
    page.value = await listQuestions({ ...filters, pageSize: 12 })
  } catch (error) {
    ElMessage.error(error instanceof Error ? error.message : '题目加载失败')
  } finally {
    loading.value = false
  }
}

function openBank(bank?: QuestionBank): void {
  editingBankId.value = bank?.id
  bankForm.name = bank?.name ?? ''
  bankForm.description = bank?.description ?? ''
  bankDialogVisible.value = true
}

async function saveBank(): Promise<void> {
  if (!bankForm.name.trim()) {
    ElMessage.warning('请填写题库名称')
    return
  }
  bankSaving.value = true
  try {
    const payload = {
      name: bankForm.name.trim(),
      description: bankForm.description.trim(),
      status: 'ACTIVE' as const,
    }
    if (editingBankId.value) await updateQuestionBank(editingBankId.value, payload)
    else await createQuestionBank(payload)
    bankDialogVisible.value = false
    await loadBanks()
    ElMessage.success(editingBankId.value ? '题库已更新' : '题库已创建')
  } catch (error) {
    ElMessage.error(error instanceof Error ? error.message : '题库保存失败')
  } finally {
    bankSaving.value = false
  }
}

async function removeBank(bank: QuestionBank): Promise<void> {
  try {
    await ElMessageBox.confirm(`确定删除题库“${bank.name}”吗？题库非空时不能删除。`, '删除题库', {
      type: 'warning',
    })
    await deleteQuestionBank(bank.id)
    if (filters.bankId === bank.id) filters.bankId = undefined
    deleteBankId.value = undefined
    deleteBankDialogVisible.value = false
    await loadBanks()
    ElMessage.success('题库已删除')
  } catch (error) {
    if (error !== 'cancel') ElMessage.error(error instanceof Error ? error.message : '删除失败')
  }
}

function selectBank(bank: QuestionBank): void {
  filters.bankId = bank.id
  filters.keyword = ''
  filters.questionType = undefined
  filters.pageNumber = 1
  bankPickerVisible.value = false
  void loadQuestions()
}

function openDeleteBank(): void {
  deleteBankId.value = undefined
  deleteBankDialogVisible.value = true
}

function confirmDeleteBank(): void {
  const bank = banks.value.find((item) => item.id === deleteBankId.value)
  if (!bank) {
    ElMessage.warning('请选择要删除的题库')
    return
  }
  void removeBank(bank)
}

function resetQuestionForm(question?: Question): void {
  if (!filters.bankId) {
    ElMessage.warning('请先选择题库')
    return
  }
  editingQuestionId.value = question?.id
  form.bankId = filters.bankId
  form.questionType = question?.questionType ?? 'SINGLE_CHOICE'
  form.stem = question?.stem ?? ''
  form.options = question?.options.map((option) => ({
    key: option.key,
    text: option.text,
    sortOrder: option.sortOrder,
  })) ?? [
    { key: 'A', text: '', sortOrder: 0 },
    { key: 'B', text: '', sortOrder: 1 },
  ]
  const answer = question?.answer.acceptedAnswers ?? []
  form.singleAnswer = answer[0]?.[0] ?? 'A'
  form.multipleAnswers = [...(answer[0] ?? [])]
  form.truthAnswer = answer[0]?.[0] === 'FALSE' ? 'FALSE' : 'TRUE'
  form.blankAnswers =
    question?.questionType === 'FILL_BLANK'
      ? answer.map((group) => group.join(' | '))
      : ['']
  form.shortAnswer = question?.questionType === 'SHORT_ANSWER' ? answer[0]?.[0] ?? '' : ''
  form.analysis = question?.analysis ?? ''
  form.defaultScore = Number(question?.defaultScore ?? 1)
  form.fillBlankAutoGradable = question?.fillBlankAutoGradable ?? false
  form.caseSensitive = question?.caseSensitive ?? false
  editorVisible.value = true
}

function addOption(): void {
  const key = String.fromCharCode(65 + form.options.length)
  form.options.push({ key, text: '', sortOrder: form.options.length })
}

function removeOption(index: number): void {
  if (form.options.length <= 2) {
    ElMessage.warning('选择题至少需要两个选项')
    return
  }
  form.options.splice(index, 1)
  form.options.forEach((option, order) => {
    option.sortOrder = order
  })
}

function addBlank(): void {
  form.blankAnswers.push('')
}

function buildAnswer(): string[][] {
  if (form.questionType === 'SINGLE_CHOICE') return [[form.singleAnswer]]
  if (form.questionType === 'MULTIPLE_CHOICE') return [[...form.multipleAnswers]]
  if (form.questionType === 'TRUE_FALSE') return [[form.truthAnswer]]
  if (form.questionType === 'SHORT_ANSWER') return [[form.shortAnswer.trim()]]
  return form.blankAnswers.map((value) =>
    value
      .split('|')
      .map((item) => item.trim())
      .filter(Boolean),
  )
}

function validateQuestion(): boolean {
  if (!form.bankId || !form.stem.trim()) {
    ElMessage.warning('请选择题库并填写题干')
    return false
  }
  if (isChoice.value) {
    const keys = answerOptionKeys.value
    if (
      form.options.length < 2 ||
      new Set(keys).size !== keys.length ||
      form.options.some((option) => !option.key.trim() || !option.text.trim())
    ) {
      ElMessage.warning('请填写至少两个标识不重复的完整选项')
      return false
    }
    if (
      (form.questionType === 'SINGLE_CHOICE' && !form.singleAnswer) ||
      (form.questionType === 'MULTIPLE_CHOICE' && form.multipleAnswers.length === 0)
    ) {
      ElMessage.warning('请选择正确答案')
      return false
    }
  }
  if (form.questionType === 'FILL_BLANK' && buildAnswer().some((group) => group.length === 0)) {
    ElMessage.warning('每个填空都需要至少一个答案，可用“|”分隔可接受写法')
    return false
  }
  if (form.questionType === 'SHORT_ANSWER' && !form.shortAnswer.trim()) {
    ElMessage.warning('请填写简答题参考答案')
    return false
  }
  return true
}

async function saveQuestion(): Promise<void> {
  if (!validateQuestion()) return
  saving.value = true
  try {
    const payload: QuestionWritePayload = {
      bankId: form.bankId,
      questionType: form.questionType,
      stem: form.stem.trim(),
      options: isChoice.value
        ? form.options.map((option, index) => ({
            key: option.key.trim().toUpperCase(),
            text: option.text.trim(),
            sortOrder: index,
          }))
        : [],
      answer: { acceptedAnswers: buildAnswer() },
      analysis: form.analysis.trim(),
      defaultScore: Number(form.defaultScore),
      fillBlankAutoGradable: form.fillBlankAutoGradable,
      caseSensitive: form.caseSensitive,
    }
    if (editingQuestionId.value) await updateQuestion(editingQuestionId.value, payload)
    else await createQuestion(payload)
    editorVisible.value = false
    await loadQuestions()
    ElMessage.success(editingQuestionId.value ? '题目已更新' : '题目已创建')
  } catch (error) {
    ElMessage.error(error instanceof Error ? error.message : '题目保存失败')
  } finally {
    saving.value = false
  }
}

async function removeQuestion(question: Question): Promise<void> {
  try {
    await ElMessageBox.confirm('删除后题目将不再出现在题库中，确定继续吗？', '删除题目', {
      type: 'warning',
    })
    await deleteQuestion(question.id)
    await loadQuestions()
    ElMessage.success('题目已删除')
  } catch (error) {
    if (error !== 'cancel') ElMessage.error(error instanceof Error ? error.message : '删除失败')
  }
}

function search(): void {
  filters.pageNumber = 1
  void loadQuestions()
}

onMounted(async () => {
  try {
    await loadBanks()
  } catch (error) {
    ElMessage.error(error instanceof Error ? error.message : '题库加载失败')
  }
})
</script>

<template>
  <section class="exam-page">
    <div class="page-container">
      <PublisherExamNav />
      <SectionPageHeader
        eyebrow="QUESTION BANK"
        title="题库管理"
        description="维护五类题目、标准答案和解析，题目数据仅对所属发布者开放"
      >
        <el-button @click="openBank()">新建题库</el-button>
        <el-button :disabled="banks.length === 0" @click="bankPickerVisible = true">选择题库</el-button>
        <el-button type="danger" plain :disabled="banks.length === 0" @click="openDeleteBank">
          删除题库
        </el-button>
        <el-button type="primary" :icon="Plus" :disabled="!selectedBank" @click="resetQuestionForm()">
          新建题目
        </el-button>
      </SectionPageHeader>

      <div v-if="selectedBank" class="selected-bank-card">
        <div>
          <span>当前题库</span>
          <strong>{{ selectedBank.name }}</strong>
          <p>{{ selectedBank.description || '暂无说明' }}</p>
        </div>
        <el-button type="primary" plain @click="openBank(selectedBank)">编辑题库</el-button>
      </div>
      <el-empty
        v-else
        :description="banks.length ? '请先选择一个题库' : '暂无题库，请先新建题库'"
      >
        <el-button v-if="banks.length" type="primary" @click="bankPickerVisible = true">选择题库</el-button>
      </el-empty>

      <template v-if="selectedBank">
        <div class="toolbar">
          <el-input v-model="filters.keyword" clearable placeholder="搜索题干" @keyup.enter="search" />
          <el-select v-model="filters.questionType" clearable placeholder="全部题型">
            <el-option v-for="(label, value) in typeLabels" :key="value" :label="label" :value="value" />
          </el-select>
          <el-button type="primary" @click="search">查询</el-button>
        </div>

        <div class="table-card">
          <el-table v-loading="loading" :data="page.items">
            <el-table-column label="题干" min-width="330">
              <template #default="{ row }">
                <strong class="stem">{{ row.stem }}</strong>
                <small>{{ row.analysis || '暂无解析' }}</small>
              </template>
            </el-table-column>
            <el-table-column label="题型" width="110">
              <template #default="{ row }">{{ typeLabels[row.questionType as QuestionType] }}</template>
            </el-table-column>
            <el-table-column prop="defaultScore" label="默认分值" width="100" />
            <el-table-column label="答案" min-width="170">
              <template #default="{ row }">
                {{ row.answer.acceptedAnswers.map((group: string[]) => group.join(' / ')).join('；') }}
              </template>
            </el-table-column>
            <el-table-column label="操作" width="145" fixed="right">
              <template #default="{ row }">
                <el-button link type="primary" :icon="Edit" @click="resetQuestionForm(row as Question)">编辑</el-button>
                <el-button link type="danger" :icon="Delete" @click="removeQuestion(row as Question)">删除</el-button>
              </template>
            </el-table-column>
            <template #empty><el-empty description="暂无符合条件的题目" /></template>
          </el-table>
        </div>
        <el-pagination
          v-if="page.total > page.pageSize"
          v-model:current-page="filters.pageNumber"
          class="pagination"
          layout="total, prev, pager, next"
          :page-size="page.pageSize"
          :total="page.total"
          @current-change="loadQuestions"
        />
      </template>
    </div>

    <el-dialog v-model="bankPickerVisible" title="选择题库" width="min(680px, 94vw)">
      <el-input v-model="bankKeyword" clearable placeholder="搜索题库名称或说明" />
      <div class="bank-picker-list">
        <button
          v-for="bank in filteredBanks"
          :key="bank.id"
          type="button"
          :class="{ active: bank.id === filters.bankId }"
          @click="selectBank(bank)"
        >
          <strong>{{ bank.name }}</strong>
          <span>{{ bank.description || '暂无说明' }}</span>
        </button>
        <el-empty v-if="filteredBanks.length === 0" :image-size="70" description="没有匹配的题库" />
      </div>
    </el-dialog>

    <el-dialog v-model="deleteBankDialogVisible" title="删除题库" width="min(520px, 94vw)">
      <el-alert
        title="非空题库不能删除，请先删除题库中的题目。"
        type="warning"
        :closable="false"
        show-icon
      />
      <el-form label-position="top" class="delete-bank-form">
        <el-form-item label="请选择要删除的题库" required>
          <el-select v-model="deleteBankId" filterable placeholder="输入题库名称搜索">
            <el-option v-for="bank in banks" :key="bank.id" :label="bank.name" :value="bank.id" />
          </el-select>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="deleteBankDialogVisible = false">取消</el-button>
        <el-button type="danger" :disabled="!deleteBankId" @click="confirmDeleteBank">删除题库</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="bankDialogVisible" :title="editingBankId ? '编辑题库' : '新建题库'" width="500px">
      <el-form label-position="top">
        <el-form-item label="题库名称" required><el-input v-model="bankForm.name" maxlength="150" /></el-form-item>
        <el-form-item label="题库说明"><el-input v-model="bankForm.description" type="textarea" :rows="3" maxlength="1000" /></el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="bankDialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="bankSaving" @click="saveBank">保存</el-button>
      </template>
    </el-dialog>

    <el-drawer v-model="editorVisible" :title="editingQuestionId ? '编辑题目' : '新建题目'" size="720px">
      <el-form label-position="top">
        <div class="form-grid">
          <el-form-item label="所属题库" required>
            <el-input :model-value="selectedBank?.name ?? ''" disabled />
          </el-form-item>
          <el-form-item label="题型" required>
            <el-select v-model="form.questionType">
              <el-option v-for="(label, value) in typeLabels" :key="value" :label="label" :value="value" />
            </el-select>
          </el-form-item>
          <el-form-item label="默认分值" required>
            <el-input-number v-model="form.defaultScore" :min="0.01" :precision="2" />
          </el-form-item>
        </div>
        <el-form-item label="题干" required>
          <el-input v-model="form.stem" type="textarea" :rows="4" maxlength="10000" />
        </el-form-item>

        <template v-if="isChoice">
          <div class="subheading"><strong>题目选项</strong><el-button link type="primary" @click="addOption">添加选项</el-button></div>
          <div v-for="(option, index) in form.options" :key="index" class="option-row">
            <el-input v-model="option.key" class="option-key" maxlength="16" />
            <el-input v-model="option.text" placeholder="选项内容" maxlength="2000" />
            <el-button link type="danger" :icon="Delete" @click="removeOption(index)" />
          </div>
          <el-form-item v-if="form.questionType === 'SINGLE_CHOICE'" label="正确答案" required>
            <el-select v-model="form.singleAnswer">
              <el-option v-for="key in answerOptionKeys" :key="key" :label="key" :value="key" />
            </el-select>
          </el-form-item>
          <el-form-item v-else label="正确答案（可多选）" required>
            <el-select v-model="form.multipleAnswers" multiple>
              <el-option v-for="key in answerOptionKeys" :key="key" :label="key" :value="key" />
            </el-select>
          </el-form-item>
        </template>

        <el-form-item v-if="form.questionType === 'TRUE_FALSE'" label="正确答案" required>
          <el-radio-group v-model="form.truthAnswer">
            <el-radio-button value="TRUE">正确</el-radio-button>
            <el-radio-button value="FALSE">错误</el-radio-button>
          </el-radio-group>
        </el-form-item>

        <template v-if="form.questionType === 'FILL_BLANK'">
          <div class="subheading"><strong>填空答案</strong><el-button link type="primary" @click="addBlank">添加空位</el-button></div>
          <div v-for="(_, index) in form.blankAnswers" :key="index" class="blank-row">
            <span>第 {{ index + 1 }} 空</span>
            <el-input v-model="form.blankAnswers[index]" placeholder="多个可接受写法使用 | 分隔" />
            <el-button v-if="form.blankAnswers.length > 1" link type="danger" :icon="Delete" @click="form.blankAnswers.splice(index, 1)" />
          </div>
          <div class="switch-row">
            <el-switch v-model="form.fillBlankAutoGradable" active-text="允许自动评分" />
            <el-switch v-model="form.caseSensitive" active-text="区分大小写" />
          </div>
        </template>

        <el-form-item v-if="form.questionType === 'SHORT_ANSWER'" label="参考答案" required>
          <el-input v-model="form.shortAnswer" type="textarea" :rows="5" />
        </el-form-item>
        <el-form-item label="答案解析">
          <el-input v-model="form.analysis" type="textarea" :rows="4" maxlength="10000" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="editorVisible = false">取消</el-button>
        <el-button type="primary" :loading="saving" @click="saveQuestion">保存题目</el-button>
      </template>
    </el-drawer>
  </section>
</template>

<style scoped>
.exam-page { min-height: calc(100vh - 145px); padding: 42px 0 76px; background: linear-gradient(180deg, #f4f7ff, #f8f9fc 330px); }
.selected-bank-card { display: flex; border: 1px solid #dbe6ff; border-radius: 18px; align-items: center; justify-content: space-between; gap: 20px; margin-bottom: 20px; background: #fff; box-shadow: var(--lp-shadow); padding: 20px 24px; }
.selected-bank-card span, .selected-bank-card strong { display: block; }.selected-bank-card span { color: var(--lp-primary); font-size: 12px; font-weight: 800; }.selected-bank-card strong { margin-top: 5px; font-size: 20px; }.selected-bank-card p { margin: 5px 0 0; color: var(--lp-text-secondary); }
.bank-picker-list { display: grid; overflow-y: auto; max-height: 420px; gap: 10px; margin-top: 16px; }
.bank-picker-list button { display: block; width: 100%; border: 1px solid var(--lp-border); border-radius: 12px; background: #fff; cursor: pointer; padding: 14px 16px; text-align: left; }
.bank-picker-list button:hover, .bank-picker-list button.active { border-color: var(--lp-primary); background: #f4f8ff; }
.bank-picker-list strong, .bank-picker-list span { display: block; }.bank-picker-list span { margin-top: 5px; color: var(--lp-text-secondary); font-size: 13px; }
.delete-bank-form { margin-top: 18px; }.delete-bank-form .el-select { width: 100%; }
.toolbar { display: grid; gap: 12px; margin-bottom: 18px; grid-template-columns: minmax(220px, 1fr) 150px auto; }
.table-card { overflow: hidden; border: 1px solid var(--lp-border); border-radius: 18px; background: #fff; box-shadow: var(--lp-shadow); padding: 8px 16px 16px; }
.stem, .table-card small { display: block; }
.table-card small { overflow: hidden; max-width: 480px; margin-top: 6px; color: #98a2b3; text-overflow: ellipsis; white-space: nowrap; }
.pagination { justify-content: center; margin-top: 24px; }
.form-grid { display: grid; gap: 16px; grid-template-columns: 1fr 1fr 130px; }
.form-grid .el-select, .form-grid .el-input-number, .el-form-item .el-select { width: 100%; }
.subheading { display: flex; align-items: center; justify-content: space-between; margin: 4px 0 10px; }
.option-row, .blank-row { display: flex; align-items: center; gap: 10px; margin-bottom: 10px; }
.option-key { width: 74px; }
.blank-row > span { width: 58px; color: var(--lp-text-secondary); font-size: 13px; }
.switch-row { display: flex; gap: 24px; margin: 14px 0 22px; }
@media (max-width: 760px) { .toolbar, .form-grid { grid-template-columns: 1fr; }.selected-bank-card { align-items: flex-start; flex-direction: column; } }
</style>
