import type { PageResult } from '@/types/api'
import type { ContentSummary } from '@/types/content'
import type { ExamSummary } from '@/types/exam'

export type ClassRole = 'OWNER' | 'ADMIN' | 'MEMBER'
export type ClassStatus = 'ACTIVE' | 'ARCHIVED'
export type ClassMemberStatus = 'ACTIVE' | 'LEFT' | 'REMOVED'

export interface Classroom {
  id: number
  ownerId: number
  name: string
  description?: string
  status: ClassStatus
  currentRole: ClassRole
  memberCount: number
  inviteCode?: string
  inviteEnabled?: boolean
  createdAt: string
  updatedAt: string
}

export interface ClassMember {
  id: number
  userId: number
  username: string
  nickname: string
  avatarUrl?: string
  role: ClassRole
  status: ClassMemberStatus
  joinedAt: string
}

export interface ClassAnnouncement {
  id: number
  classId: number
  authorId: number
  authorName: string
  authorAvatarUrl?: string
  title: string
  body: string
  pinned: boolean
  createdAt: string
  updatedAt: string
}

export type ClassMemberPage = PageResult<ClassMember>
export type ClassContentPage = PageResult<ContentSummary>
export type ClassExamPage = PageResult<ExamSummary>
