import type { PageResult } from '@/types/api'
import type { RoleCode, UserStatus } from '@/types/auth'
import type { ExamManagement, ExamStatus, ExamSummary } from '@/types/exam'

export interface AdminUser {
  id: number
  username: string
  nickname: string
  email?: string
  phone?: string
  status: UserStatus
  roles: RoleCode[]
  lastLoginAt?: string
  createdAt: string
  updatedAt: string
}

export interface AdminUserListParams {
  keyword?: string
  status?: UserStatus
  role?: RoleCode
  pageNumber?: number
  pageSize?: number
}

export type AdminUserPage = PageResult<AdminUser>

export interface AdminExamSummary {
  exam: ExamSummary
  publisherUsername: string
  publisherNickname: string
}

export interface AdminExamDetail {
  management: ExamManagement
  publisherUsername: string
  publisherNickname: string
}

export interface AdminExamListParams {
  keyword?: string
  publisherId?: number
  status?: ExamStatus
  pageNumber?: number
  pageSize?: number
}

export type AdminExamPage = PageResult<AdminExamSummary>

export type OperationResult = 'SUCCESS' | 'FAILURE'

export interface OperationLog {
  id: number
  operatorId?: number
  operatorName?: string
  module: string
  action: string
  targetType?: string
  targetId?: string
  requestMethod: string
  requestPath: string
  requestId?: string
  ipAddress?: string
  userAgent?: string
  result: OperationResult
  detailJson?: string
  errorMessage?: string
  durationMs: number
  createdAt: string
}

export interface OperationLogListParams {
  operatorId?: number
  module?: string
  action?: string
  result?: OperationResult
  requestId?: string
  pageNumber?: number
  pageSize?: number
}

export type OperationLogPage = PageResult<OperationLog>
