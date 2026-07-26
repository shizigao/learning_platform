import type { ContentFileRole } from '@/types/content'

const MEBIBYTE = 1024 * 1024

export interface UploadRule {
  role: ContentFileRole
  label: string
  extensions: string[]
  accept: string
  maxBytes: number
  maxSizeLabel: string
  optionLabel: string
  description: string
}

function rule(
  role: ContentFileRole,
  label: string,
  extensions: string[],
  maxMebibytes: number,
): UploadRule {
  const typeLabel = extensions.map((extension) => extension.toUpperCase()).join('、')
  const maxSizeLabel = `${maxMebibytes} MB`
  return {
    role,
    label,
    extensions,
    accept: extensions.map((extension) => `.${extension}`).join(','),
    maxBytes: maxMebibytes * MEBIBYTE,
    maxSizeLabel,
    optionLabel: `${label}（${typeLabel}，≤${maxSizeLabel}）`,
    description: `支持 ${typeLabel}；单个文件不能超过 ${maxSizeLabel}`,
  }
}

export const UPLOAD_RULES: Record<ContentFileRole, UploadRule> = {
  COVER: rule('COVER', '封面', ['jpg', 'jpeg', 'png', 'webp'], 10),
  INLINE_IMAGE: rule('INLINE_IMAGE', '正文图片', ['jpg', 'jpeg', 'png', 'webp'], 10),
  CONTENT: rule(
    'CONTENT',
    '正文文件',
    ['pdf', 'txt', 'md', 'doc', 'docx', 'ppt', 'pptx', 'xls', 'xlsx'],
    50,
  ),
  VIDEO: rule('VIDEO', '视频', ['mp4', 'webm'], 200),
  ATTACHMENT: rule(
    'ATTACHMENT',
    '附件',
    ['pdf', 'zip', 'txt', 'doc', 'docx', 'ppt', 'pptx', 'xls', 'xlsx'],
    200,
  ),
  SUBTITLE: rule('SUBTITLE', '字幕', ['srt', 'vtt'], 5),
}

export const UPLOAD_RULE_OPTIONS = Object.values(UPLOAD_RULES)

export function validateUploadFile(file: File, role: ContentFileRole): string | undefined {
  const uploadRule = UPLOAD_RULES[role]
  const extension = file.name.includes('.') ? file.name.split('.').pop()?.toLowerCase() : undefined

  if (!extension || !uploadRule.extensions.includes(extension)) {
    return `${uploadRule.label}仅支持 ${uploadRule.extensions
      .map((item) => item.toUpperCase())
      .join('、')} 文件`
  }
  if (file.size > uploadRule.maxBytes) {
    return `${uploadRule.label}文件大小超出限制，单个文件不能超过 ${uploadRule.maxSizeLabel}`
  }
  if (file.size <= 0) {
    return '不能上传空文件'
  }
  return undefined
}
