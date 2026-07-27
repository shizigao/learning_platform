import DOMPurify from 'dompurify'
import { marked } from 'marked'

marked.setOptions({
  gfm: true,
  breaks: true,
})

/**
 * 将 Markdown 转为经过白名单清洗的 HTML。
 * 清洗是渲染用户资料的安全边界，调用方不得直接使用 `marked.parse` 的原始结果。
 */
export function renderSafeMarkdown(source: string): string {
  const html = marked.parse(source || '', { async: false }) as string
  return DOMPurify.sanitize(html, {
    ALLOWED_TAGS: [
      'p', 'br', 'strong', 'em', 'del', 'blockquote', 'pre', 'code',
      'ul', 'ol', 'li', 'h1', 'h2', 'h3', 'h4', 'h5', 'h6',
      'a', 'img', 'hr', 'table', 'thead', 'tbody', 'tr', 'th', 'td',
    ],
    ALLOWED_ATTR: ['href', 'src', 'alt', 'title', 'class', 'loading'],
    ALLOW_DATA_ATTR: false,
    ALLOWED_URI_REGEXP: /^(?:(?:https?|mailto):|[#/])/i,
  })
}

export interface MarkdownResourceReference {
  kind: 'image' | 'file'
  fileId: number
}

/** 找出正文中去重后的平台图片/附件占位协议，供页面批量换取授权 URL。 */
export function findMarkdownResourceReferences(source: string): MarkdownResourceReference[] {
  const references = new Map<string, MarkdownResourceReference>()
  const pattern = /content-(image|file):\/\/(\d+)/g
  for (const match of source.matchAll(pattern)) {
    const kind = match[1] as 'image' | 'file'
    const fileId = Number(match[2])
    references.set(`${kind}:${fileId}`, { kind, fileId })
  }
  return [...references.values()]
}

/** 用短期授权地址替换指定图片或附件占位协议。 */
export function replaceMarkdownResourceReference(
  source: string,
  reference: MarkdownResourceReference,
  url: string,
): string {
  return source.replaceAll(
    `content-${reference.kind}://${reference.fileId}`,
    url,
  )
}

/** 找出正文中去重后的站内学习资料引用 ID。 */
export function findMarkdownContentReferences(source: string): number[] {
  const contentIds = new Set<number>()
  const pattern = /content-reference:\/\/(\d+)/g
  for (const match of source.matchAll(pattern)) {
    contentIds.add(Number(match[1]))
  }
  return [...contentIds]
}

/** 将站内资料引用协议转换为前端详情路由；是否允许跳转由渲染组件决定。 */
export function replaceMarkdownContentReference(
  source: string,
  contentId: number,
): string {
  return source.replaceAll(
    `content-reference://${contentId}`,
    `/contents/${contentId}`,
  )
}
