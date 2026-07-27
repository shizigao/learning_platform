import type { ExamAttemptStatus } from '@/types/exam'
import type { UserStatus } from '@/types/auth'

const submittedStatuses: ExamAttemptStatus[] = ['SUBMITTED', 'GRADING', 'COMPLETED']

export function shouldShowExamCountdown(
  attemptId: number | undefined,
  attemptStatus: ExamAttemptStatus | undefined,
  submitted: boolean,
): boolean {
  return Boolean(attemptId) && !submitted && !submittedStatuses.includes(attemptStatus as ExamAttemptStatus)
}

export function canChangeAdminUserStatus(
  userId: number,
  status: UserStatus,
  currentUserId: number | undefined,
): boolean {
  return status !== 'LOCKED' && userId !== currentUserId
}
