<script setup lang="ts">
import { Delete, UploadFilled } from '@element-plus/icons-vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { computed, onMounted, reactive, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'

import {
  createContent,
  deleteContentFile,
  getPublisherContent,
  updateContent,
  uploadContentFile,
} from '@/api/content'
import CategoryPickerDialog from '@/components/CategoryPickerDialog.vue'
import ClassPickerDialog from '@/components/ClassPickerDialog.vue'
import ContentStatusTag from '@/components/ContentStatusTag.vue'
import MarkdownEditor from '@/components/MarkdownEditor.vue'
import SectionPageHeader from '@/components/SectionPageHeader.vue'
import type {
  ContentDetail,
  ContentFile,
  ContentFileRole,
  ContentWritePayload,
} from '@/types/content'
import {
  UPLOAD_RULE_OPTIONS,
  UPLOAD_RULES,
  validateUploadFile,
} from '@/utils/upload-rules'

const route = useRoute()
const router = useRouter()
const contentId = computed(() => (route.params.id ? Number(route.params.id) : undefined))
const saving = ref(false)
const loading = ref(false)
const loadError = ref('')
const uploading = ref(false)
const detail = ref<ContentDetail>()
const markdownEditor = ref<InstanceType<typeof MarkdownEditor>>()
const selectedFile = ref<File>()
const fileInput = ref<HTMLInputElement>()
const fileRole = ref<ContentFileRole>('CONTENT')
const currentUploadRule = computed(() => UPLOAD_RULES[fileRole.value])
const form = reactive<ContentWritePayload>({
  categoryId: 0,
  title: '',
  summary: '',
  articleBody: '',
  distributionMode: 'PUBLIC',
  classIds: [],
  isFree: true,
  price: 0,
})
const formErrors = reactive({
  categoryId: '',
  title: '',
  price: '',
  classIds: '',
})
const editable = computed(() => !detail.value || ['DRAFT', 'REJECTED'].includes(detail.value.status))

function fill(content: ContentDetail): void {
  detail.value = content
  form.categoryId = content.categoryId
  form.title = content.title
  form.summary = content.summary ?? ''
  form.articleBody = content.articleBody ?? ''
  form.distributionMode = content.distributionMode ?? 'PUBLIC'
  form.classIds = [...(content.classIds ?? [])]
  form.isFree = content.isFree
  form.price = Number(content.price)
}

async function load(): Promise<void> {
  loading.value = true
  loadError.value = ''
  try {
    if (contentId.value) fill(await getPublisherContent(contentId.value))
  } catch (error) {
    loadError.value = error instanceof Error ? error.message : '资料加载失败'
  } finally {
    loading.value = false
  }
}

async function refreshFiles(): Promise<void> {
  if (!contentId.value) return
  detail.value = await getPublisherContent(contentId.value)
}

function updateCategory(categoryId?: number): void {
  form.categoryId = categoryId ?? 0
}

function insertImage(file: ContentFile): void {
  markdownEditor.value?.insertImage(file)
}

function insertFileReference(file: ContentFile): void {
  markdownEditor.value?.insertFileReference(file)
}

function fileRoleLabel(role: ContentFileRole): string {
  return UPLOAD_RULES[role].label
}

function validateForm(): boolean {
  formErrors.categoryId = form.categoryId ? '' : '请选择资料分类'
  formErrors.title = form.title.trim() ? '' : '请输入资料标题'
  formErrors.price = !form.isFree && Number(form.price) < 0.01
    ? '付费资料价格必须不少于 0.01 元'
    : ''
  formErrors.classIds = form.distributionMode === 'CLASS' && form.classIds.length === 0
    ? '请选择至少一个发放班级'
    : ''
  return !formErrors.categoryId && !formErrors.title && !formErrors.price && !formErrors.classIds
}

// 保存草稿
async function save(): Promise<void> {
  if (!validateForm()) {
    ElMessage.warning('请修正表单中的错误后再保存')
    return
  }
  saving.value = true
  try {
    const payload = {
      ...form,
      title: form.title.trim(),
      summary: form.summary.trim(),
      articleBody: form.articleBody.trim(),
      isFree: form.distributionMode === 'CLASS' ? true : form.isFree,
      price: form.distributionMode === 'CLASS' || form.isFree ? 0 : Number(form.price),
    }
    // 发送保存草稿请求到后端
    const saved = contentId.value
      //点击updateContent
    
      ? await updateContent(contentId.value, payload) //更新，若没有则创建
      : await createContent(payload) // 创建
    fill(saved)
    ElMessage.success('草稿已保存')
    if (!contentId.value) await router.replace(`/publisher/contents/${saved.id}/edit`)
  } catch (error) {
    ElMessage.error(error instanceof Error ? error.message : '保存失败')
  } finally {
    saving.value = false
  }
}

function clearSelectedFile(): void {
  selectedFile.value = undefined
  if (fileInput.value) fileInput.value.value = ''
}

function chooseFile(event: Event): void {
  const input = event.target as HTMLInputElement
  const file = input.files?.[0]
  if (!file) {
    selectedFile.value = undefined
    return
  }
  const validationError = validateUploadFile(file, fileRole.value)
  if (validationError) {
    clearSelectedFile()
    ElMessage.error(validationError)
    return
  }
  selectedFile.value = file
}

// 上传文件
async function upload(): Promise<void> {
  if (!contentId.value || !selectedFile.value) {
    ElMessage.warning(contentId.value ? '请选择文件' : '请先保存草稿')
    return
  }
  const validationError = validateUploadFile(selectedFile.value, fileRole.value)
  if (validationError) {
    clearSelectedFile()
    ElMessage.error(validationError)
    return
  }
  uploading.value = true
  try {
    // 点击uploadContentFile
    await uploadContentFile(contentId.value, fileRole.value, selectedFile.value)
    await refreshFiles()
    clearSelectedFile()
    ElMessage.success('文件上传成功')
  } catch (error) {
    ElMessage.error(error instanceof Error ? error.message : '上传失败')
  } finally {
    uploading.value = false
  }
}

async function removeFile(fileId: number): Promise<void> {
  if (!contentId.value) return
  if (
    form.articleBody.includes(`content-image://${fileId}`)
    || form.articleBody.includes(`content-file://${fileId}`)
  ) {
    ElMessage.warning('该文件仍被正文引用，请先从正文中移除对应图片或文件链接')
    return
  }
  try {
    await ElMessageBox.confirm('确定删除这个文件吗？', '删除文件', { type: 'warning' })
    await deleteContentFile(contentId.value, fileId)
    await refreshFiles()
  } catch (error) {
    if (error !== 'cancel') ElMessage.error(error instanceof Error ? error.message : '删除失败')
  }
}

watch(fileRole, clearSelectedFile)
watch(
  () => form.title,
  (title) => {
    if (title.trim()) formErrors.title = ''
  },
)
watch(
  () => form.categoryId,
  (categoryId) => {
    if (categoryId > 0) formErrors.categoryId = ''
  },
)
watch(
  () => [form.isFree, form.price] as const,
  ([isFree, price]) => {
    if (isFree || Number(price) >= 0.01) formErrors.price = ''
  },
)
watch(
  () => [form.distributionMode, form.classIds.length] as const,
  ([mode, count]) => {
    if (mode !== 'CLASS' || count > 0) formErrors.classIds = ''
  },
)
onMounted(load)
</script>

<template>
  <section v-loading="loading" class="editor-page"><div class="page-container">
    <SectionPageHeader eyebrow="CONTENT EDITOR" :title="contentId ? '编辑学习资料' : '新建学习资料'" description="完善内容信息并上传资料文件">
      <ContentStatusTag v-if="detail" :status="detail.status" />
      <RouterLink to="/publisher"><el-button>返回工作台</el-button></RouterLink>
      <el-button type="primary" :loading="saving" :disabled="!editable" @click="save">保存草稿</el-button>
    </SectionPageHeader>
    <el-alert
      v-if="loadError"
      :title="loadError"
      description="请检查网络或账号权限后重试。"
      type="error"
      show-icon
      :closable="false"
      class="load-alert"
    >
      <template #default><el-button link type="primary" @click="load">重新加载</el-button></template>
    </el-alert>
    <el-alert v-if="detail?.rejectionReason" :title="`驳回原因：${detail.rejectionReason}`" type="error" show-icon :closable="false" class="reject-alert" />
    <div class="editor-layout">
      <el-form class="form-card" label-position="top" :disabled="!editable">
        <div class="form-grid">
          <el-form-item label="资料标题" required :error="formErrors.title"><el-input v-model="form.title" maxlength="200" show-word-limit /></el-form-item>
          <el-form-item label="资料分类" required :error="formErrors.categoryId">
            <CategoryPickerDialog
              :model-value="form.categoryId || undefined"
              :disabled="!editable"
              @update:model-value="updateCategory"
            />
          </el-form-item>
          <el-form-item label="发放方式">
            <el-radio-group v-model="form.distributionMode">
              <el-radio-button value="PUBLIC">公开发放</el-radio-button>
              <el-radio-button value="CLASS">班级发放</el-radio-button>
            </el-radio-group>
          </el-form-item>
          <el-form-item
            v-if="form.distributionMode === 'PUBLIC'"
            label="公开收费方式"
          >
            <el-radio-group v-model="form.isFree">
              <el-radio-button :value="true">免费</el-radio-button>
              <el-radio-button :value="false">付费</el-radio-button>
            </el-radio-group>
          </el-form-item>
          <el-form-item
            v-if="form.distributionMode === 'PUBLIC' && !form.isFree"
            label="价格（元）"
            :error="formErrors.price"
          >
            <el-input-number v-model="form.price" :min="0.01" :precision="2" />
          </el-form-item>
          <el-form-item
            v-if="form.distributionMode === 'CLASS'"
            label="发放班级"
            required
            :error="formErrors.classIds"
          >
            <ClassPickerDialog v-model="form.classIds" :disabled="!editable" />
          </el-form-item>
        </div>
        <el-form-item label="资料简介"><el-input v-model="form.summary" maxlength="1000" :rows="3" show-word-limit type="textarea" /></el-form-item>
        <el-form-item label="资料正文（支持Markdown）">
          <MarkdownEditor
            ref="markdownEditor"
            v-model="form.articleBody"
            :content-id="contentId"
            :disabled="!editable"
          />
        </el-form-item>
      </el-form>
      <aside class="upload-card">
        <h2><el-icon><UploadFilled /></el-icon>文件管理</h2>
        <p>先保存草稿，再选择文件用途和本地文件上传。</p>
        <el-select v-model="fileRole" :disabled="!editable">
          <el-option
            v-for="rule in UPLOAD_RULE_OPTIONS"
            :key="rule.role"
            :label="rule.optionLabel"
            :value="rule.role"
          />
        </el-select>
        <div class="upload-rule" role="note">
          <strong>{{ currentUploadRule.label }}上传限制</strong>
          <span>{{ currentUploadRule.description }}</span>
        </div>
        <label class="file-picker" :class="{ disabled: !editable }"><input ref="fileInput" type="file" :accept="currentUploadRule.accept" :disabled="!editable" @change="chooseFile" /><span>{{ selectedFile?.name || '选择本地文件' }}</span></label>
        <el-button type="primary" :loading="uploading" :disabled="!editable || !selectedFile || !contentId" @click="upload">上传文件</el-button>
        <div class="file-list">
          <div v-for="file in detail?.files ?? []" :key="file.id" class="file-item">
            <div class="file-summary">
              <strong>{{ file.originalName }}</strong>
              <small>{{ fileRoleLabel(file.fileRole) }} · {{ (file.sizeBytes / 1024 / 1024).toFixed(2) }} MB</small>
            </div>
            <div class="file-actions">
              <el-button
                v-if="file.fileRole === 'INLINE_IMAGE'"
                text
                type="primary"
                :disabled="!editable"
                @click="insertImage(file)"
              >
                插入图片
              </el-button>
              <el-button
                v-else-if="!['COVER'].includes(file.fileRole)"
                text
                type="primary"
                :disabled="!editable"
                @click="insertFileReference(file)"
              >
                插入引用
              </el-button>
              <el-button text type="danger" :icon="Delete" :disabled="!editable" @click="removeFile(file.id)" />
            </div>
          </div>
          <el-empty v-if="!loading && (detail?.files.length ?? 0) === 0" :image-size="54" description="暂无文件" />
        </div>
      </aside>
    </div>
  </div></section>
</template>

<style scoped>
.editor-page { min-height: calc(100vh - 145px); padding: 48px 0 76px; background: #f6f8fc; }.load-alert, .reject-alert { margin-bottom: 20px; }.editor-layout { display: grid; align-items: start; gap: 22px; grid-template-columns: minmax(0, 1fr) 390px; }.form-card, .upload-card { border: 1px solid var(--lp-border); border-radius: 18px; background: #fff; box-shadow: var(--lp-shadow); padding: 28px; }.form-grid { display: grid; gap: 0 18px; grid-template-columns: repeat(2, 1fr); }.form-grid .el-select { width: 100%; }.upload-card { position: sticky; top: 94px; }.upload-card h2 { display: flex; align-items: center; gap: 8px; margin-top: 0; font-size: 18px; }.upload-card > p { color: var(--lp-text-secondary); font-size: 13px; line-height: 1.7; }.upload-card > .el-select, .upload-card > .el-button { width: 100%; margin-top: 12px; }.upload-rule { display: flex; border: 1px solid #cfe0ff; border-radius: 10px; margin-top: 12px; flex-direction: column; gap: 5px; color: #475467; background: #f5f8ff; font-size: 12px; line-height: 1.5; padding: 11px 12px; }.upload-rule strong { color: #2457c5; font-size: 13px; }.file-picker { display: block; overflow: hidden; border: 1px dashed #b8c5dc; border-radius: 10px; margin-top: 12px; color: var(--lp-primary); background: #f8faff; cursor: pointer; padding: 14px; text-align: center; text-overflow: ellipsis; white-space: nowrap; }.file-picker input { display: none; }.file-picker.disabled { cursor: not-allowed; opacity: .55; }.file-list { margin-top: 20px; }.file-list .el-empty { padding: 18px 0 4px; }.file-item { display: flex; align-items: center; justify-content: space-between; gap: 8px; border-top: 1px solid var(--lp-border); padding: 13px 0; }.file-summary { min-width: 0; }.file-item strong { display: block; overflow: hidden; max-width: 185px; font-size: 13px; text-overflow: ellipsis; white-space: nowrap; }.file-item small { display: block; margin-top: 5px; color: #98a2b3; font-size: 11px; }.file-actions { display: flex; flex: none; align-items: center; gap: 0; }.file-actions .el-button { margin-left: 0; padding-inline: 6px; }
@media (max-width: 850px) { .editor-layout { grid-template-columns: 1fr; }.upload-card { position: static; } }@media (max-width: 560px) { .form-grid { grid-template-columns: 1fr; }.form-card, .upload-card { padding: 20px 16px; } }
</style>
