<script setup lang="ts">
import { Delete, UploadFilled } from '@element-plus/icons-vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { computed, onMounted, reactive, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'

import {
  createContent,
  deleteContentFile,
  getPublisherContent,
  listCategories,
  updateContent,
  uploadContentFile,
} from '@/api/content'
import ContentStatusTag from '@/components/ContentStatusTag.vue'
import SectionPageHeader from '@/components/SectionPageHeader.vue'
import type {
  ContentCategory,
  ContentDetail,
  ContentFileRole,
  ContentWritePayload,
} from '@/types/content'

const route = useRoute()
const router = useRouter()
const contentId = computed(() => (route.params.id ? Number(route.params.id) : undefined))
const saving = ref(false)
const uploading = ref(false)
const categories = ref<ContentCategory[]>([])
const detail = ref<ContentDetail>()
const selectedFile = ref<File>()
const fileRole = ref<ContentFileRole>('CONTENT')
const form = reactive<ContentWritePayload>({
  categoryId: 0,
  title: '',
  summary: '',
  contentType: 'ARTICLE',
  articleBody: '',
  isFree: true,
  price: 0,
})
const editable = computed(() => !detail.value || ['DRAFT', 'REJECTED'].includes(detail.value.status))

function fill(content: ContentDetail): void {
  detail.value = content
  form.categoryId = content.categoryId
  form.title = content.title
  form.summary = content.summary ?? ''
  form.contentType = content.contentType
  form.articleBody = content.articleBody ?? ''
  form.isFree = content.isFree
  form.price = Number(content.price)
}

async function load(): Promise<void> {
  categories.value = await listCategories()
  if (contentId.value) fill(await getPublisherContent(contentId.value))
  else if (categories.value.length) form.categoryId = categories.value[0]!.id
}

async function save(): Promise<void> {
  if (!form.categoryId || !form.title.trim()) {
    ElMessage.warning('请填写分类和标题')
    return
  }
  saving.value = true
  try {
    const payload = { ...form, title: form.title.trim(), summary: form.summary.trim(), articleBody: form.articleBody.trim(), price: form.isFree ? 0 : Number(form.price) }
    const saved = contentId.value
      ? await updateContent(contentId.value, payload)
      : await createContent(payload)
    fill(saved)
    ElMessage.success('草稿已保存')
    if (!contentId.value) await router.replace(`/publisher/contents/${saved.id}/edit`)
  } catch (error) {
    ElMessage.error(error instanceof Error ? error.message : '保存失败')
  } finally {
    saving.value = false
  }
}

function chooseFile(event: Event): void {
  selectedFile.value = (event.target as HTMLInputElement).files?.[0]
}

async function upload(): Promise<void> {
  if (!contentId.value || !selectedFile.value) {
    ElMessage.warning(contentId.value ? '请选择文件' : '请先保存草稿')
    return
  }
  uploading.value = true
  try {
    await uploadContentFile(contentId.value, fileRole.value, selectedFile.value)
    fill(await getPublisherContent(contentId.value))
    selectedFile.value = undefined
    ElMessage.success('文件上传成功')
  } catch (error) {
    ElMessage.error(error instanceof Error ? error.message : '上传失败')
  } finally {
    uploading.value = false
  }
}

async function removeFile(fileId: number): Promise<void> {
  if (!contentId.value) return
  try {
    await ElMessageBox.confirm('确定删除这个文件吗？', '删除文件', { type: 'warning' })
    await deleteContentFile(contentId.value, fileId)
    fill(await getPublisherContent(contentId.value))
  } catch (error) {
    if (error !== 'cancel') ElMessage.error(error instanceof Error ? error.message : '删除失败')
  }
}

onMounted(() => load().catch((error) => ElMessage.error(error instanceof Error ? error.message : '加载失败')))
</script>

<template>
  <section class="editor-page"><div class="page-container">
    <SectionPageHeader eyebrow="CONTENT EDITOR" :title="contentId ? '编辑学习资料' : '新建学习资料'" description="完善内容信息并上传经过安全校验的资料文件">
      <ContentStatusTag v-if="detail" :status="detail.status" />
      <RouterLink to="/publisher"><el-button>返回工作台</el-button></RouterLink>
      <el-button type="primary" :loading="saving" :disabled="!editable" @click="save">保存草稿</el-button>
    </SectionPageHeader>
    <el-alert v-if="detail?.rejectionReason" :title="`驳回原因：${detail.rejectionReason}`" type="error" show-icon :closable="false" class="reject-alert" />
    <div class="editor-layout">
      <el-form class="form-card" label-position="top" :disabled="!editable">
        <div class="form-grid">
          <el-form-item label="资料标题" required><el-input v-model="form.title" maxlength="200" show-word-limit /></el-form-item>
          <el-form-item label="资料分类" required><el-select v-model="form.categoryId"><el-option v-for="item in categories" :key="item.id" :label="item.name" :value="item.id" /></el-select></el-form-item>
          <el-form-item label="资料类型" required><el-select v-model="form.contentType"><el-option label="图文" value="ARTICLE" /><el-option label="文档" value="DOCUMENT" /><el-option label="视频" value="VIDEO" /><el-option label="附件" value="ATTACHMENT" /><el-option label="综合" value="MIXED" /></el-select></el-form-item>
          <el-form-item label="收费方式"><el-radio-group v-model="form.isFree"><el-radio-button :value="true">免费</el-radio-button><el-radio-button :value="false">付费</el-radio-button></el-radio-group></el-form-item>
          <el-form-item v-if="!form.isFree" label="价格（元）"><el-input-number v-model="form.price" :min="0.01" :precision="2" /></el-form-item>
        </div>
        <el-form-item label="资料简介"><el-input v-model="form.summary" maxlength="1000" :rows="3" show-word-limit type="textarea" /></el-form-item>
        <el-form-item label="图文正文"><el-input v-model="form.articleBody" :rows="12" type="textarea" placeholder="图文资料需填写正文；文档和视频资料可使用文件上传" /></el-form-item>
      </el-form>
      <aside class="upload-card">
        <h2><el-icon><UploadFilled /></el-icon>文件管理</h2>
        <p>先保存草稿，再选择文件用途和本地文件上传。</p>
        <el-select v-model="fileRole" :disabled="!editable">
          <el-option label="封面" value="COVER" /><el-option label="正文文件" value="CONTENT" /><el-option label="视频" value="VIDEO" /><el-option label="附件" value="ATTACHMENT" /><el-option label="字幕" value="SUBTITLE" />
        </el-select>
        <label class="file-picker" :class="{ disabled: !editable }"><input type="file" :disabled="!editable" @change="chooseFile" /><span>{{ selectedFile?.name || '选择本地文件' }}</span></label>
        <el-button type="primary" :loading="uploading" :disabled="!editable || !selectedFile || !contentId" @click="upload">上传文件</el-button>
        <div class="file-list">
          <div v-for="file in detail?.files ?? []" :key="file.id" class="file-item"><div><strong>{{ file.originalName }}</strong><small>{{ file.fileRole }} · {{ (file.sizeBytes / 1024 / 1024).toFixed(2) }} MB</small></div><el-button text type="danger" :icon="Delete" :disabled="!editable" @click="removeFile(file.id)" /></div>
        </div>
      </aside>
    </div>
  </div></section>
</template>

<style scoped>
.editor-page { min-height: calc(100vh - 145px); padding: 48px 0 76px; background: #f6f8fc; }.reject-alert { margin-bottom: 20px; }.editor-layout { display: grid; align-items: start; gap: 22px; grid-template-columns: minmax(0, 1fr) 320px; }.form-card, .upload-card { border: 1px solid var(--lp-border); border-radius: 18px; background: #fff; box-shadow: var(--lp-shadow); padding: 28px; }.form-grid { display: grid; gap: 0 18px; grid-template-columns: repeat(2, 1fr); }.form-grid .el-select { width: 100%; }.upload-card { position: sticky; top: 94px; }.upload-card h2 { display: flex; align-items: center; gap: 8px; margin-top: 0; font-size: 18px; }.upload-card > p { color: var(--lp-text-secondary); font-size: 13px; line-height: 1.7; }.upload-card > .el-select, .upload-card > .el-button { width: 100%; margin-top: 12px; }.file-picker { display: block; overflow: hidden; border: 1px dashed #b8c5dc; border-radius: 10px; margin-top: 12px; color: var(--lp-primary); background: #f8faff; cursor: pointer; padding: 14px; text-align: center; text-overflow: ellipsis; white-space: nowrap; }.file-picker input { display: none; }.file-picker.disabled { cursor: not-allowed; opacity: .55; }.file-list { margin-top: 20px; }.file-item { display: flex; align-items: center; justify-content: space-between; gap: 8px; border-top: 1px solid var(--lp-border); padding: 13px 0; }.file-item strong { display: block; overflow: hidden; max-width: 205px; font-size: 13px; text-overflow: ellipsis; white-space: nowrap; }.file-item small { display: block; margin-top: 5px; color: #98a2b3; font-size: 11px; }
@media (max-width: 850px) { .editor-layout { grid-template-columns: 1fr; }.upload-card { position: static; } }@media (max-width: 560px) { .form-grid { grid-template-columns: 1fr; }.form-card, .upload-card { padding: 20px 16px; } }
</style>
