import { http } from '@/api/http'
import type { ApiResponse } from '@/types/api'
import type {
  CandidateExamOverview,
  ExamCandidateOption,
  ExamCandidatePage,
  ExamAnswer,
  ExamAnswerPayload,
  ExamEligibility,
  ExamListParams,
  ExamManagement,
  ExamPage,
  ExamPaperDetail,
  ExamPaperListParams,
  ExamPaperPage,
  ExamSummary,
  ExamStartSession,
  ExamSubmission,
  ExamResultDetail,
  ExamResultQuestion,
  ExamResultSummary,
  ExamGradingAttempt,
  ExamGradingDetail,
  ExamStatistics,
  ExamWritePayload,
  ExamAiAnalysis,
  ExamAiAnalysisPage,
  Question,
  QuestionBank,
  QuestionListParams,
  QuestionPage,
  QuestionStatus,
  QuestionWritePayload,
} from '@/types/exam'

function data<T>(response: { data: ApiResponse<T> }): T {
  return response.data.data
}

export async function listQuestionBanks(): Promise<QuestionBank[]> {
  return data(await http.get<ApiResponse<QuestionBank[]>>('/publisher/question-banks'))
}

export async function createQuestionBank(payload: {
  name: string
  description: string
  status?: QuestionStatus
}): Promise<QuestionBank> {
  return data(await http.post<ApiResponse<QuestionBank>>('/publisher/question-banks', payload))
}

export async function updateQuestionBank(
  bankId: number,
  payload: { name: string; description: string; status: QuestionStatus },
): Promise<QuestionBank> {
  return data(
    await http.put<ApiResponse<QuestionBank>>(`/publisher/question-banks/${bankId}`, payload),
  )
}

export async function deleteQuestionBank(bankId: number): Promise<void> {
  await http.delete(`/publisher/question-banks/${bankId}`)
}

export async function listQuestions(params: QuestionListParams = {}): Promise<QuestionPage> {
  return data(await http.get<ApiResponse<QuestionPage>>('/publisher/questions', { params }))
}

export async function createQuestion(payload: QuestionWritePayload): Promise<Question> {
  return data(await http.post<ApiResponse<Question>>('/publisher/questions', payload))
}

export async function updateQuestion(
  questionId: number,
  payload: QuestionWritePayload,
): Promise<Question> {
  return data(
    await http.put<ApiResponse<Question>>(`/publisher/questions/${questionId}`, payload),
  )
}

export async function deleteQuestion(questionId: number): Promise<void> {
  await http.delete(`/publisher/questions/${questionId}`)
}

export async function listPapers(params: ExamPaperListParams = {}): Promise<ExamPaperPage> {
  return data(await http.get<ApiResponse<ExamPaperPage>>('/publisher/papers', { params }))
}

export async function getPaper(paperId: number): Promise<ExamPaperDetail> {
  return data(await http.get<ApiResponse<ExamPaperDetail>>(`/publisher/papers/${paperId}`))
}

export async function createPaper(payload: {
  name: string
  description: string
}): Promise<ExamPaperDetail> {
  return data(await http.post<ApiResponse<ExamPaperDetail>>('/publisher/papers', payload))
}

export async function updatePaper(
  paperId: number,
  payload: { name: string; description: string },
): Promise<ExamPaperDetail> {
  return data(await http.put<ApiResponse<ExamPaperDetail>>(`/publisher/papers/${paperId}`, payload))
}

export async function replacePaperQuestions(
  paperId: number,
  questions: Array<{ questionId: number; sortOrder: number; score: number }>,
): Promise<ExamPaperDetail> {
  return data(
    await http.put<ApiResponse<ExamPaperDetail>>(`/publisher/papers/${paperId}/questions`, {
      questions,
    }),
  )
}

export async function deletePaper(paperId: number): Promise<void> {
  await http.delete(`/publisher/papers/${paperId}`)
}

export async function listPublisherExams(params: ExamListParams = {}): Promise<ExamPage> {
  return data(await http.get<ApiResponse<ExamPage>>('/publisher/exams', { params }))
}

export async function getPublisherExam(examId: number): Promise<ExamManagement> {
  return data(await http.get<ApiResponse<ExamManagement>>(`/publisher/exams/${examId}`))
}

export async function createExam(payload: ExamWritePayload): Promise<ExamManagement> {
  return data(await http.post<ApiResponse<ExamManagement>>('/publisher/exams', payload))
}

export async function updateExam(
  examId: number,
  payload: ExamWritePayload,
): Promise<ExamManagement> {
  return data(await http.put<ApiResponse<ExamManagement>>(`/publisher/exams/${examId}`, payload))
}

export async function publishExam(examId: number): Promise<ExamManagement> {
  return data(await http.post<ApiResponse<ExamManagement>>(`/publisher/exams/${examId}/publish`))
}

export async function cancelExam(examId: number): Promise<ExamManagement> {
  return data(await http.post<ApiResponse<ExamManagement>>(`/publisher/exams/${examId}/cancel`))
}

export async function deleteExam(examId: number): Promise<void> {
  await http.delete(`/publisher/exams/${examId}`)
}

export async function getExamQuota(): Promise<number> {
  return data(await http.get<ApiResponse<number>>('/publisher/exams/quota'))
}

export async function searchExamCandidates(keyword = ''): Promise<ExamCandidateOption[]> {
  return data(
    await http.get<ApiResponse<ExamCandidateOption[]>>('/publisher/exam-candidates', {
      params: { keyword },
    }),
  )
}

export async function searchExamCandidatePage(
  params: { keyword?: string; pageNumber?: number; pageSize?: number } = {},
): Promise<ExamCandidatePage> {
  return data(
    await http.get<ApiResponse<ExamCandidatePage>>('/publisher/exam-candidates/search', {
      params,
    }),
  )
}

export async function listAssignedExams(): Promise<ExamSummary[]> {
  return data(await http.get<ApiResponse<ExamSummary[]>>('/exams'))
}

export async function getCandidateExamOverview(examId: number): Promise<CandidateExamOverview> {
  return data(
    await http.get<ApiResponse<CandidateExamOverview>>(`/exams/${examId}/overview`),
  )
}

export async function checkExamEligibility(examId: number): Promise<ExamEligibility> {
  return data(
    await http.get<ApiResponse<ExamEligibility>>(`/exams/${examId}/eligibility`),
  )
}

export async function startExam(examId: number): Promise<ExamStartSession> {
  return data(
    await http.post<ApiResponse<ExamStartSession>>(`/exams/${examId}/start`),
  )
}

export async function resumeExam(examId: number): Promise<ExamStartSession> {
  return data(
    await http.get<ApiResponse<ExamStartSession>>(`/exams/${examId}/session`),
  )
}

export async function saveExamAnswer(
  examId: number,
  questionId: number,
  payload: ExamAnswerPayload,
): Promise<ExamAnswer> {
  return data(
    await http.put<ApiResponse<ExamAnswer>>(
      `/exams/${examId}/answers/${questionId}`,
      payload,
    ),
  )
}

export async function saveExamAnswers(
  examId: number,
  answers: Array<{ questionId: number; answer: ExamAnswerPayload }>,
): Promise<ExamAnswer[]> {
  return data(
    await http.put<ApiResponse<ExamAnswer[]>>(`/exams/${examId}/answers`, { answers }),
  )
}

export async function submitExam(examId: number): Promise<ExamSubmission> {
  return data(
    await http.post<ApiResponse<ExamSubmission>>(`/exams/${examId}/submit`),
  )
}

export async function getExamResult(examId: number): Promise<ExamResultDetail> {
  return data(
    await http.get<ApiResponse<ExamResultDetail>>(`/exams/${examId}/result`),
  )
}

export async function listExamGradingAttempts(examId: number): Promise<ExamGradingAttempt[]> {
  return data(
    await http.get<ApiResponse<ExamGradingAttempt[]>>(
      `/publisher/exams/${examId}/grading/attempts`,
    ),
  )
}

export async function getExamGradingDetail(
  examId: number,
  attemptId: number,
): Promise<ExamGradingDetail> {
  return data(
    await http.get<ApiResponse<ExamGradingDetail>>(
      `/publisher/exams/${examId}/grading/attempts/${attemptId}`,
    ),
  )
}

export async function gradeExamAnswer(
  examId: number,
  attemptId: number,
  answerId: number,
  payload: { score: number; comment?: string },
): Promise<ExamResultQuestion> {
  return data(
    await http.put<ApiResponse<ExamResultQuestion>>(
      `/publisher/exams/${examId}/grading/attempts/${attemptId}/answers/${answerId}`,
      payload,
    ),
  )
}

export async function completeExamReview(
  examId: number,
  attemptId: number,
): Promise<ExamResultSummary> {
  return data(
    await http.post<ApiResponse<ExamResultSummary>>(
      `/publisher/exams/${examId}/grading/attempts/${attemptId}/complete`,
    ),
  )
}

export async function getExamStatistics(examId: number): Promise<ExamStatistics> {
  return data(
    await http.get<ApiResponse<ExamStatistics>>(
      `/publisher/exams/${examId}/grading/statistics`,
    ),
  )
}

export async function getOverallExamAiAnalysis(
  examId: number,
): Promise<ExamAiAnalysisPage> {
  return data(
    await http.get<ApiResponse<ExamAiAnalysisPage>>(
      `/publisher/exams/${examId}/grading/ai-analysis`,
    ),
  )
}

export async function generateOverallExamAiAnalysis(
  examId: number,
  requestId: string,
): Promise<ExamAiAnalysis> {
  return data(
    await http.post<ApiResponse<ExamAiAnalysis>>(
      `/publisher/exams/${examId}/grading/ai-analysis`,
      { requestId },
      { timeout: 630_000 },
    ),
  )
}

export async function getPersonalExamAiAnalysis(
  examId: number,
): Promise<ExamAiAnalysisPage> {
  return data(
    await http.get<ApiResponse<ExamAiAnalysisPage>>(
      `/exams/${examId}/result/ai-analysis`,
    ),
  )
}

export async function generatePersonalExamAiAnalysis(
  examId: number,
  requestId: string,
): Promise<ExamAiAnalysis> {
  return data(
    await http.post<ApiResponse<ExamAiAnalysis>>(
      `/exams/${examId}/result/ai-analysis`,
      { requestId },
      { timeout: 630_000 },
    ),
  )
}
