import type { PageResult } from '@/types/api'

export type ContentType = 'GENERAL' | 'ARTICLE' | 'DOCUMENT' | 'VIDEO' | 'ATTACHMENT' | 'MIXED'
export type ContentStatus = 'DRAFT' | 'PENDING_REVIEW' | 'PUBLISHED' | 'REJECTED' | 'OFFLINE'
export type ContentDistributionMode = 'PUBLIC' | 'CLASS'
export type ContentFileRole =
  | 'COVER'
  | 'INLINE_IMAGE'
  | 'CONTENT'
  | 'VIDEO'
  | 'ATTACHMENT'
  | 'SUBTITLE'

export interface ContentCategory {
  id: number
  parentId?: number
  name: string
  slug: string
  description?: string
  sortOrder: number
  enabled: boolean
}

export interface ContentSummary {
  id: number
  publisherId: number
  publisherName: string
  publisherUsername?: string
  publisherAvatarUrl?: string
  categoryId: number
  categoryName?: string
  title: string
  summary?: string
  contentType: ContentType
  coverFileId?: number
  coverUrl?: string
  distributionMode: ContentDistributionMode
  isFree: boolean
  price: number
  status: ContentStatus
  viewCount: number
  likeCount: number
  favoriteCount: number
  publishedAt?: string
  updatedAt: string
}

export interface ContentFile {
  id: number
  fileRole: ContentFileRole
  originalName: string
  mimeType: string
  extension?: string
  sizeBytes: number
  sortOrder: number
  durationSeconds?: number
}

export interface ContentDetail extends ContentSummary {
  articleBody?: string
  classIds: number[]
  hasAccess: boolean
  rejectionReason?: string
  commentCount: number
  submittedAt?: string
  createdAt: string
  files: ContentFile[]
}

export interface ContentWritePayload {
  categoryId: number
  title: string
  summary: string
  articleBody: string
  distributionMode: ContentDistributionMode
  classIds: number[]
  isFree: boolean
  price: number
}

export interface ContentListParams {
  keyword?: string
  categoryId?: number
  free?: boolean
  status?: ContentStatus
  pageNumber?: number
  pageSize?: number
}

export interface CategoryWritePayload {
  parentId?: number
  name: string
  slug: string
  description: string
  sortOrder: number
  enabled: boolean
}

export interface LearningProgress {
  id: number
  contentId: number
  startedAt: string
  lastLearnedAt: string
  progressPercent: number
  lastPosition?: string
  completedAt?: string
}

export interface ContentReaction {
  liked: boolean
  favorited: boolean
  likeCount: number
  favoriteCount: number
}

export interface ContentComment {
  id: number
  contentId: number
  userId: number
  parentId?: number
  body: string
  createdAt: string
}

export type ContentPage = PageResult<ContentSummary>
export type ContentCategoryPage = PageResult<ContentCategory>
export type CommentPage = PageResult<ContentComment>
