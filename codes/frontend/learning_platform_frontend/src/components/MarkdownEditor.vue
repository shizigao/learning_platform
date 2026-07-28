<script setup lang="ts">
import { ElMessage, ElMessageBox } from 'element-plus'
import { nextTick, ref } from 'vue'

import ContentReferencePickerDialog from '@/components/ContentReferencePickerDialog.vue'
import MarkdownRenderer from '@/components/MarkdownRenderer.vue'
import type { ContentFile, ContentSummary } from '@/types/content'

const props = withDefaults(defineProps<{
  modelValue: string
  disabled?: boolean
  contentId?: number
}>(), {
  disabled: false,
  contentId: undefined,
})

const emit = defineEmits<{
  'update:modelValue': [value: string]
}>()

const mode = ref<'edit' | 'preview'>('edit')
const textarea = ref<HTMLTextAreaElement>()
const referencePickerVisible = ref(false)

function update(value: string): void {
  emit('update:modelValue', value)
}

function handleInput(event: Event): void {
  update((event.target as HTMLTextAreaElement).value)
}

async function replaceSelection(before: string, after = '', placeholder = '文本'): Promise<void> {
  if (props.disabled) return
  mode.value = 'edit'
  await nextTick()
  const element = textarea.value
  if (!element) return
  const start = element.selectionStart
  const end = element.selectionEnd
  const selected = props.modelValue.slice(start, end) || placeholder
  const nextValue = `${props.modelValue.slice(0, start)}${before}${selected}${after}${props.modelValue.slice(end)}`
  update(nextValue)
  await nextTick()
  element.focus()
  element.setSelectionRange(start + before.length, start + before.length + selected.length)
}

async function insertBlock(snippet: string): Promise<void> {
  if (props.disabled) return
  mode.value = 'edit'
  await nextTick()
  const element = textarea.value
  if (!element) return
  const start = element.selectionStart
  const prefix = start > 0 && props.modelValue[start - 1] !== '\n' ? '\n' : ''
  const value = `${props.modelValue.slice(0, start)}${prefix}${snippet}${props.modelValue.slice(element.selectionEnd)}`
  update(value)
  await nextTick()
  element.focus()
  element.setSelectionRange(start + prefix.length + snippet.length, start + prefix.length + snippet.length)
}

async function insertLink(): Promise<void> {
  if (props.disabled) return
  try {
    const result = await ElMessageBox.prompt('请输入 HTTPS 链接地址', '插入链接', {
      inputPlaceholder: 'https://example.com',
      inputPattern: /^https?:\/\/\S+$/i,
      inputErrorMessage: '请输入有效的 HTTP 或 HTTPS 地址',
    })
    await replaceSelection('[', `](${result.value})`, '链接文字')
  } catch {
    // User cancelled.
  }
}


function insertImage(file: ContentFile): void {
  if (file.fileRole !== 'INLINE_IMAGE') {
    ElMessage.warning('只有“正文图片”用途的文件可以作为图片插入')
    return
  }
  void insertBlock(`![${file.originalName}](content-image://${file.id})\n`)
}

function insertFileReference(file: ContentFile): void {
  if (file.fileRole === 'COVER' || file.fileRole === 'INLINE_IMAGE') {
    ElMessage.warning('请选择正文文件、视频、附件或字幕')
    return
  }
  void insertBlock(`[${file.originalName}](content-file://${file.id})\n`)
}

function escapeMarkdownLabel(value: string): string {
  return value.replaceAll('\\', '\\\\').replaceAll('[', '\\[').replaceAll(']', '\\]')
}

function insertContentReference(content: ContentSummary): void {
  const publisher = content.publisherName || `用户 ${content.publisherId}`
  void insertBlock(
    `[引用资料：${escapeMarkdownLabel(content.title)}（${escapeMarkdownLabel(publisher)}）](content-reference://${content.id})\n`,
  )
}

defineExpose({ insertImage, insertFileReference })
</script>

<template>
  <div class="markdown-editor" :class="{ disabled }">
    <header class="editor-toolbar">
      <div class="format-actions">
        <el-button-group>
          <el-button :disabled="disabled" title="一级标题" @click="insertBlock('# 标题\n')">H1</el-button>
          <el-button :disabled="disabled" title="二级标题" @click="insertBlock('## 标题\n')">H2</el-button>
          <el-button :disabled="disabled" title="粗体" @click="replaceSelection('**', '**')"><strong>B</strong></el-button>
          <el-button :disabled="disabled" title="斜体" @click="replaceSelection('*', '*')"><em>I</em></el-button>
          <el-button :disabled="disabled" title="引用" @click="insertBlock('> 引用内容\n')">引用</el-button>
          <el-button :disabled="disabled" title="无序列表" @click="insertBlock('- 列表项\n')">列表</el-button>
          <el-button :disabled="disabled" title="代码块" @click="insertBlock('```text\n代码内容\n```\n')">代码</el-button>
          <el-button :disabled="disabled" title="链接" @click="insertLink">链接</el-button>
          <el-button
            :disabled="disabled"
            title="搜索并引用平台内的其他学习资料"
            @click="referencePickerVisible = true"
          >
            引用资料
          </el-button>
          <el-button :disabled="disabled" title="表格" @click="insertBlock('| 列一 | 列二 |\n| --- | --- |\n| 内容 | 内容 |\n')">表格</el-button>
          <el-button :disabled="disabled" title="分隔线" @click="insertBlock('\n---\n')">分隔线</el-button>
        </el-button-group>
      </div>
      <el-radio-group v-model="mode" size="small">
        <el-radio-button value="edit">编辑</el-radio-button>
        <el-radio-button value="preview">预览</el-radio-button>
      </el-radio-group>
    </header>

    <textarea
      v-show="mode === 'edit'"
      ref="textarea"
      class="markdown-input"
      :disabled="disabled"
      :value="modelValue"
      placeholder="使用 Markdown 编写正文。上传正文图片或资料文件后，可在右侧文件列表将其插入当前光标位置。"
      @input="handleInput"
    />
    <div v-show="mode === 'preview'" class="markdown-preview">
      <MarkdownRenderer
        :source="modelValue"
        :content-id="contentId"
        publisher-mode
      />
    </div>
    <footer>支持 Markdown；正文图片和资料引用使用平台内部标记，访问时仍会执行权限校验。</footer>
    <ContentReferencePickerDialog
      v-model="referencePickerVisible"
      :exclude-content-id="contentId"
      @select="insertContentReference"
    />
  </div>
</template>

<style scoped>
.markdown-editor { overflow: hidden; width: 100%; border: 1px solid #dcdfe6; border-radius: 8px; background: #fff; }
.markdown-editor:focus-within { border-color: var(--lp-primary); box-shadow: 0 0 0 1px var(--lp-primary) inset; }
.markdown-editor.disabled { background: #f5f7fa; opacity: .75; }
.editor-toolbar { display: flex; align-items: center; justify-content: space-between; gap: 12px; border-bottom: 1px solid #e4e7ed; background: #fafbfc; padding: 8px 10px; }
.format-actions { overflow-x: auto; padding-bottom: 1px; }
.format-actions :deep(.el-button) { padding-inline: 10px; }
.markdown-input { display: block; width: 100%; min-height: 430px; resize: vertical; border: 0; outline: 0; color: var(--lp-text); background: transparent; padding: 20px; font-family: Consolas, "Courier New", monospace; font-size: 14px; line-height: 1.75; }
.markdown-preview { overflow-y: auto; min-height: 430px; max-height: 680px; padding: 24px; }
footer { border-top: 1px solid #ebeef5; color: #98a2b3; background: #fafbfc; padding: 8px 12px; font-size: 11px; }
@media (max-width: 720px) { .editor-toolbar { align-items: stretch; flex-direction: column; }.markdown-input, .markdown-preview { min-height: 360px; } }
</style>
