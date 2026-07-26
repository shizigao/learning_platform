<script setup lang="ts">
import { computed, ref, watch } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'

import { getContentFileUrl, getPublisherContentFileUrl } from '@/api/content'
import {
  findMarkdownContentReferences,
  findMarkdownResourceReferences,
  renderSafeMarkdown,
  replaceMarkdownContentReference,
  replaceMarkdownResourceReference,
} from '@/utils/markdown'

const props = withDefaults(defineProps<{
  source: string
  contentId?: number
  publisherMode?: boolean
  contentReferenceNavigationEnabled?: boolean
}>(), {
  contentId: undefined,
  publisherMode: false,
  contentReferenceNavigationEnabled: true,
})

const resolving = ref(false)
const resolvedSource = ref('')
const router = useRouter()
let resolutionVersion = 0

const html = computed(() => renderSafeMarkdown(resolvedSource.value))

async function resolveResources(): Promise<void> {
  const version = ++resolutionVersion
  const sourceWithContentLinks = findMarkdownContentReferences(props.source).reduce(
    (source, contentReferenceId) =>
      replaceMarkdownContentReference(source, contentReferenceId),
    props.source,
  )
  const references = findMarkdownResourceReferences(props.source)
  if (!references.length || !props.contentId) {
    resolvedSource.value = sourceWithContentLinks
    resolving.value = false
    return
  }
  resolving.value = true
  const replacements = await Promise.all(references.map(async (reference) => {
    try {
      const mode = reference.kind === 'image' ? 'preview' : 'download'
      const url = props.publisherMode
        ? await getPublisherContentFileUrl(props.contentId!, reference.fileId, mode)
        : await getContentFileUrl(props.contentId!, reference.fileId, mode)
      return { reference, url }
    } catch {
      return { reference, url: '#resource-unavailable' }
    }
  }))
  const result = replacements.reduce(
    (source, replacement) =>
      replaceMarkdownResourceReference(source, replacement.reference, replacement.url),
    sourceWithContentLinks,
  )
  if (version === resolutionVersion) {
    resolvedSource.value = result
    resolving.value = false
  }
}

function handleClick(event: MouseEvent): void {
  const target = event.target
  if (!(target instanceof Element)) return
  const anchor = target.closest('a')
  const href = anchor?.getAttribute('href')
  if (!href || !/^\/contents\/\d+$/.test(href)) return
  event.preventDefault()
  if (!props.contentReferenceNavigationEnabled) {
    ElMessage.info({
      message: '资料预览暂不支持打开引用，请点击“开始学习”后访问引用资料',
      duration: 2000,
    })
    return
  }
  void router.push(href)
}

watch(
  () => [props.source, props.contentId, props.publisherMode] as const,
  () => void resolveResources(),
  { immediate: true },
)
</script>

<template>
  <div class="markdown-renderer">
    <div v-if="resolving" class="resource-status">正在加载正文资源…</div>
    <div v-if="source.trim()" class="markdown-body" v-html="html" @click="handleClick" />
    <el-empty v-else :image-size="64" description="暂无正文内容" />
  </div>
</template>

<style scoped>
.resource-status { border-radius: 8px; margin-bottom: 12px; color: #475467; background: #f2f6fc; padding: 8px 12px; font-size: 12px; }
.markdown-body { overflow-wrap: anywhere; color: var(--lp-text); font-size: 16px; line-height: 1.9; }
.markdown-body :deep(:first-child) { margin-top: 0; }
.markdown-body :deep(:last-child) { margin-bottom: 0; }
.markdown-body :deep(h1), .markdown-body :deep(h2), .markdown-body :deep(h3), .markdown-body :deep(h4), .markdown-body :deep(h5), .markdown-body :deep(h6) { margin: 1.2em 0 .55em; line-height: 1.4; }
.markdown-body :deep(h1) { border-bottom: 1px solid #e5e7eb; padding-bottom: .3em; font-size: 2em; }
.markdown-body :deep(h2) { border-bottom: 1px solid #edf0f5; padding-bottom: .25em; font-size: 1.55em; }
.markdown-body :deep(h3) { font-size: 1.3em; }
.markdown-body :deep(p) { margin: .8em 0; }
.markdown-body :deep(ul), .markdown-body :deep(ol) { padding-left: 1.8em; }
.markdown-body :deep(blockquote) { border-left: 4px solid #93c5fd; margin: 1em 0; color: #475467; background: #f5f8ff; padding: .55em 1em; }
.markdown-body :deep(code) { border-radius: 5px; color: #b42318; background: #f2f4f7; padding: .12em .35em; font-family: Consolas, "Courier New", monospace; font-size: .9em; }
.markdown-body :deep(pre) { overflow-x: auto; border-radius: 10px; color: #e2e8f0; background: #0f172a; padding: 14px 16px; line-height: 1.65; }
.markdown-body :deep(pre code) { color: inherit; background: transparent; padding: 0; }
.markdown-body :deep(a) { color: var(--lp-primary); text-decoration: underline; }
.markdown-body :deep(a[href^="/contents/"]) { display: inline-flex; border: 1px solid #bfdbfe; border-radius: 8px; align-items: center; color: #1d4ed8; background: #eff6ff; padding: .22em .6em; font-weight: 650; text-decoration: none; }
.markdown-body :deep(a[href^="/contents/"]::before) { margin-right: .35em; content: "↗"; }
.markdown-body :deep(img) { display: block; max-width: 100%; max-height: 720px; border-radius: 10px; margin: 18px auto; object-fit: contain; }
.markdown-body :deep(table) { display: block; overflow-x: auto; width: 100%; border-collapse: collapse; margin: 1em 0; }
.markdown-body :deep(th), .markdown-body :deep(td) { border: 1px solid #d0d5dd; padding: 8px 11px; text-align: left; }
.markdown-body :deep(th) { background: #f2f4f7; }
.markdown-body :deep(hr) { border: 0; border-top: 1px solid #d0d5dd; margin: 1.6em 0; }
</style>
