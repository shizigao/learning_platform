import DOMPurify from 'dompurify'
import { marked } from 'marked'

marked.setOptions({
  gfm: true,
  breaks: true,
})

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

export function findMarkdownContentReferences(source: string): number[] {
  const contentIds = new Set<number>()
  const pattern = /content-reference:\/\/(\d+)/g
  for (const match of source.matchAll(pattern)) {
    contentIds.add(Number(match[1]))
  }
  return [...contentIds]
}

export function replaceMarkdownContentReference(
  source: string,
  contentId: number,
): string {
  return source.replaceAll(
    `content-reference://${contentId}`,
    `/contents/${contentId}`,
  )
}
