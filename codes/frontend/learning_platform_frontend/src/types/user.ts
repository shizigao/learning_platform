import type { PageResult } from '@/types/api'
import type { RoleCode } from '@/types/auth'
import type { ContentSummary } from '@/types/content'

export interface UserPublicationStatistics {
  contentCount: number
  viewCount: number
  likeCount: number
  favoriteCount: number
}

export interface PublicUserSummary {
  id: number
  username: string
  nickname: string
  avatarUrl?: string
  bio?: string
}

export interface PublicUserProfile extends PublicUserSummary {
  roles: RoleCode[]
  createdAt: string
  statistics: UserPublicationStatistics
}

export type PublicUserPage = PageResult<PublicUserSummary>
export type PublicUserContentPage = PageResult<ContentSummary>
