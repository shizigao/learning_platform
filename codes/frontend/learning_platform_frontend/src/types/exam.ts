import type { PageResult } from '@/types/api'

export type QuestionType =
  | 'SINGLE_CHOICE'
  | 'MULTIPLE_CHOICE'
  | 'TRUE_FALSE'
  | 'FILL_BLANK'
  | 'SHORT_ANSWER'
export type QuestionStatus = 'ACTIVE' | 'ARCHIVED'
export type ExamPaperStatus = 'DRAFT' | 'READY' | 'ARCHIVED'
export type ExamStatus = 'DRAFT' | 'PUBLISHED' | 'ONGOING' | 'FINISHED' | 'CANCELLED'
export type ExamCandidateStatus = 'ASSIGNED' | 'STARTED' | 'SUBMITTED' | 'ABSENT'
export type ExamAttemptStatus = 'IN_PROGRESS' | 'SUBMITTED' | 'GRADING' | 'COMPLETED'

export interface QuestionBank {
  id: number
  ownerId: number
  name: string
  description?: string
  status: QuestionStatus
  createdAt: string
  updatedAt: string
}

export interface QuestionAnswer {
  acceptedAnswers: string[][]
}

export interface QuestionOption {
  id?: number
  key: string
  text: string
  sortOrder: number
}

export interface Question {
  id: number
  bankId: number
  creatorId: number
  questionType: QuestionType
  stem: string
  options: QuestionOption[]
  answer: QuestionAnswer
  analysis?: string
  defaultScore: number
  fillBlankAutoGradable: boolean
  caseSensitive: boolean
  status: QuestionStatus
  createdAt: string
  updatedAt: string
}

export interface QuestionWritePayload {
  bankId: number
  questionType: QuestionType
  stem: string
  options: Array<Omit<QuestionOption, 'id'>>
  answer: QuestionAnswer
  analysis: string
  defaultScore: number
  fillBlankAutoGradable: boolean
  caseSensitive: boolean
}

export interface QuestionListParams {
  bankId?: number
  questionType?: QuestionType
  keyword?: string
  pageNumber?: number
  pageSize?: number
}

export type QuestionPage = PageResult<Question>

export interface ExamPaperSummary {
  id: number
  creatorId: number
  name: string
  description?: string
  totalScore: number
  questionCount: number
  status: ExamPaperStatus
  createdAt: string
  updatedAt: string
}

export interface PaperQuestion {
  id: number
  questionId: number
  sortOrder: number
  score: number
  questionType: QuestionType
  stem: string
  options: QuestionOption[]
  answer: QuestionAnswer
  analysis?: string
}

export interface ExamPaperDetail {
  paper: ExamPaperSummary
  questions: PaperQuestion[]
}

export interface ExamPaperListParams {
  status?: ExamPaperStatus
  keyword?: string
  pageNumber?: number
  pageSize?: number
}

export type ExamPaperPage = PageResult<ExamPaperSummary>

export interface ExamCandidate {
  id: number
  userId: number
  username: string
  nickname: string
  status: ExamCandidateStatus
  assignedAt: string
  startedAt?: string
  submittedAt?: string
}

export interface ExamCandidateOption {
  id: number
  username: string
  nickname: string
}

export interface ExamSummary {
  id: number
  publisherId: number
  paperId: number
  name: string
  startAt: string
  endAt: string
  durationMinutes: number
  passingScore: number
  showResultImmediately: boolean
  showAnswerAfterFinish: boolean
  status: ExamStatus
  publishedAt?: string
  createdAt: string
  updatedAt: string
}

export interface ExamManagement {
  exam: ExamSummary
  instructions?: string
  paper: ExamPaperSummary
  candidates: ExamCandidate[]
}

export interface ExamWritePayload {
  paperId: number
  name: string
  instructions: string
  startAt: string
  endAt: string
  durationMinutes: number
  passingScore: number
  showResultImmediately: boolean
  showAnswerAfterFinish: boolean
  candidateUserIds: number[]
}

export interface ExamListParams {
  status?: ExamStatus
  keyword?: string
  pageNumber?: number
  pageSize?: number
}

export type ExamPage = PageResult<ExamSummary>

export interface CandidatePaperQuestion {
  paperQuestionId: number
  questionId: number
  sortOrder: number
  score: number
  questionType: QuestionType
  stem: string
  options: QuestionOption[]
  blankCount: number
}

export type ExamAnswerGradingStatus =
  | 'UNANSWERED'
  | 'SAVED'
  | 'AUTO_GRADED'
  | 'PENDING_REVIEW'
  | 'GRADED'

export interface ExamAnswer {
  id: number
  questionId: number
  paperQuestionId: number
  values: string[]
  text?: string
  gradingStatus: ExamAnswerGradingStatus
  savedAt: string
}

export interface ExamAnswerPayload {
  values: string[]
  text?: string | null
}

export interface ExamSubmission {
  attemptId: number
  status: ExamAttemptStatus
  submittedAt: string
  submissionType: 'MANUAL' | 'TIMEOUT' | 'ADMIN'
  answeredCount: number
  totalQuestions: number
}

export interface ExamEligibility {
  examId: number
  eligible: boolean
  canStart: boolean
  reason: string
  candidateStatus: ExamCandidateStatus
  serverTime: string
  startAt: string
  endAt: string
  durationMinutes: number
  attemptId?: number
  attemptStatus?: ExamAttemptStatus
  startedAt?: string
  deadlineAt?: string
  remainingSeconds: number
}

export interface CandidateExamOverview {
  exam: ExamSummary
  instructions?: string
  paper: ExamPaperSummary
  eligibility: ExamEligibility
}

export interface ExamStartSession {
  attemptId: number
  status: ExamAttemptStatus
  startedAt: string
  deadlineAt: string
  serverTime: string
  remainingSeconds: number
  exam: ExamSummary
  instructions?: string
  paper: ExamPaperSummary
  questions: CandidatePaperQuestion[]
  answers: ExamAnswer[]
}

export interface ExamResultSummary {
  id: number
  examId: number
  attemptId: number
  userId: number
  totalScore: number
  passingScore: number
  passed: boolean
  correctCount: number
  incorrectCount: number
  unansweredCount: number
  gradingCompleted: boolean
  generatedAt: string
}

export interface ExamResultQuestion {
  answerId: number
  questionId: number
  sortOrder: number
  questionType: QuestionType
  stem: string
  options: QuestionOption[]
  maxScore: number
  values: string[]
  text?: string
  score?: number
  correct?: boolean
  gradingStatus: ExamAnswerGradingStatus
  correctAnswer?: QuestionAnswer
  analysis?: string
  graderComment?: string
}

export interface ExamResultDetail {
  result: ExamResultSummary
  answersVisible: boolean
  questions: ExamResultQuestion[]
}

export interface ExamGradingAttempt {
  attemptId: number
  userId: number
  username: string
  nickname: string
  status: ExamAttemptStatus
  submittedAt: string
  submissionType: 'MANUAL' | 'TIMEOUT' | 'ADMIN'
  pendingReviewCount: number
  totalScore: number
  gradingCompleted: boolean
}

export interface ExamGradingDetail {
  attempt: ExamGradingAttempt
  questions: ExamResultQuestion[]
}

export interface ExamQuestionStatistics {
  questionId: number
  sortOrder: number
  questionType: QuestionType
  stem: string
  maxScore: number
  gradedCount: number
  answeredCount: number
  correctCount: number
  correctRate: number
}

export interface ExamStatistics {
  examId: number
  totalCandidates: number
  participatedCount: number
  submittedCount: number
  notParticipatedCount: number
  gradedCount: number
  averageScore?: number
  highestScore?: number
  lowestScore?: number
  passedCount: number
  passRate: number
  questions: ExamQuestionStatistics[]
}
