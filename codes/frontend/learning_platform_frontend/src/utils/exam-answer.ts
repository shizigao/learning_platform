import type {
  CandidatePaperQuestion,
  ExamAnswerPayload,
  QuestionType,
} from '@/types/exam'

export interface ExamAnswerDraft {
  values: string[]
  text: string
}

/** 返回题型需要初始化的最少答案槽位数，填空题按题目空格数展开。 */
export function minimumAnswerValues(questionType: QuestionType, blankCount: number): number {
  if (questionType === 'FILL_BLANK') return Math.max(1, blankCount)
  if (questionType === 'SINGLE_CHOICE' || questionType === 'TRUE_FALSE') return 1
  return 0
}

/**
 * 把页面草稿规范化为后端作答协议。
 * 简答题只提交 `text`，选择/判断/填空题只提交 `values`，避免题型字段混用。
 */
export function examAnswerPayload(
  question: Pick<CandidatePaperQuestion, 'questionType'> | undefined,
  answer: ExamAnswerDraft | undefined,
): ExamAnswerPayload {
  if (question?.questionType === 'SHORT_ANSWER') {
    return {
      values: [],
      text: answer?.text || null,
    }
  }
  if (question?.questionType === 'MULTIPLE_CHOICE') {
    return {
      values: (answer?.values ?? []).filter((value) => value.trim() !== ''),
      text: null,
    }
  }
  if (question?.questionType === 'SINGLE_CHOICE' || question?.questionType === 'TRUE_FALSE') {
    const value = answer?.values[0]?.trim()
    return {
      values: value ? [value] : [],
      text: null,
    }
  }
  return {
    values: [...(answer?.values ?? [])],
    text: null,
  }
}
