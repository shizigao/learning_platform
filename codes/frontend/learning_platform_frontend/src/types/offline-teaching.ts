import type { PageResult } from '@/types/api'
import type { AiTask } from '@/types/ai'

export type TeacherApplicationStatus =
  | 'DRAFT'
  | 'PENDING'
  | 'APPROVED'
  | 'REJECTED'
  | 'WITHDRAWN'
export type TeacherProfileStatus = 'ACTIVE' | 'SUSPENDED'
export type TeacherGender = 'UNKNOWN' | 'MALE' | 'FEMALE'
export type EducationLevel =
  | 'HIGH_SCHOOL'
  | 'ASSOCIATE'
  | 'BACHELOR'
  | 'MASTER'
  | 'DOCTOR'
  | 'OTHER'

export interface TeacherApplicationPayload {
  teacherName: string
  idCardNumber: string
  gender: TeacherGender
  educationLevel: EducationLevel
  educationBackground: string
  institution?: string
  province: string
  city: string
  district?: string
  bio: string
  teachingContent: string
  teachingTags: string[]
  availability: string
  hourlyRate: number
  priceDescription?: string
  contactWechat?: string
  contactQq?: string
  contactEmail?: string
}

export interface TeacherApplication
  extends Omit<TeacherApplicationPayload, 'idCardNumber' | 'availability'> {
  id: number
  userId: number
  idCardMasked: string
  idCardNumber?: string
  availability?: string
  status: TeacherApplicationStatus
  rejectionReason?: string
  submittedAt?: string
  reviewedAt?: string
  updatedAt: string
}

export interface TeacherApplicationSummary {
  id: number
  userId: number
  username: string
  nickname: string
  teacherName: string
  idCardMasked: string
  province: string
  city: string
  institution?: string
  status: TeacherApplicationStatus
  submittedAt?: string
  reviewedAt?: string
  updatedAt: string
}

export interface TeacherProfile {
  id: number
  userId: number
  username: string
  nickname: string
  avatarUrl?: string
  teacherName: string
  gender: TeacherGender
  educationLevel: EducationLevel
  educationBackground: string
  institution?: string
  province: string
  city: string
  district?: string
  bio: string
  teachingContent: string
  teachingTags: string[]
  availability?: string
  hourlyRate: number
  priceDescription?: string
  contactWechat?: string
  contactQq?: string
  contactEmail?: string
  status: TeacherProfileStatus
  suspendedReason?: string
  approvedAt: string
}

export interface TeacherSearchParams {
  keyword?: string
  province?: string
  city?: string
  teachingTag?: string
  maxHourlyRate?: number
  pageNumber?: number
  pageSize?: number
}

export interface StudentPreference {
  subject: string
  currentLevel: string
  learningGoals: string
  weaknesses?: string
  province: string
  city: string
  district?: string
  maxHourlyRate?: number
  availability?: string
  teacherPreferences?: string
  additionalNotes?: string
}

export interface TeacherRecommendationItem {
  teacher: TeacherProfile
  reason: string
  matchHighlights: string[]
  localScore: number
}

export interface TeacherRecommendation {
  aiSucceeded: boolean
  message: string
  task: AiTask
  recommendations: TeacherRecommendationItem[]
  createdAt?: string
}

export type TeacherPage = PageResult<TeacherProfile>
export type TeacherApplicationPage = PageResult<TeacherApplicationSummary>
