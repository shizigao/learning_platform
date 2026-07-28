import { http } from '@/api/http'
import type { ApiResponse } from '@/types/api'
import type {
  AdminAiConfig,
  AiConversation,
  AiExplanation,
  AiSummary,
  AiTask,
  AiUsageRecord,
  AiConversationTemplate,
} from '@/types/ai'

// 前端超时必须晚于后端统一超时，才能展示后端返回的明确失败原因。
const AI_REQUEST_TIMEOUT = 645_000

function data<T>(response: { data: ApiResponse<T> }): T {
  return response.data.data
}

export function createAiRequestId(prefix: string): string {
  const value =
    typeof globalThis.crypto?.randomUUID === 'function'
      ? globalThis.crypto.randomUUID().replaceAll('-', '')
      : `${Date.now().toString(36)}${Math.random().toString(36).slice(2)}`
  return `${prefix}-${value}`.slice(0, 64)
}

export async function generateSummary(
  contentId: number,
  requestId: string,
): Promise<AiSummary> {
  return data(
    // 发起请求
    await http.post<ApiResponse<AiSummary>>(
      `/ai/contents/${contentId}/summaries`,
      { requestId },
      { timeout: AI_REQUEST_TIMEOUT },
    ),
  )
}

export async function getLatestSummary(contentId: number): Promise<AiSummary> {
  return data(await http.get<ApiResponse<AiSummary>>(`/ai/contents/${contentId}/summaries/latest`))
}

export async function createConversation(
  contentId: number,
  title?: string,
): Promise<AiConversation> {
  return data(
    await http.post<ApiResponse<AiConversation>>(`/ai/contents/${contentId}/conversations`, {
      title: title?.trim() || undefined,
    }),
  )
}

export async function listConversations(contentId: number): Promise<AiConversation[]> {
  return data(
    await http.get<ApiResponse<AiConversation[]>>(`/ai/contents/${contentId}/conversations`),
  )
}

export async function getConversation(conversationId: number): Promise<AiConversation> {
  return data(await http.get<ApiResponse<AiConversation>>(`/ai/conversations/${conversationId}`))
}

export async function explain(
  conversationId: number,
  question: string,
  requestId: string,
): Promise<AiExplanation> {
  return data(
    await http.post<ApiResponse<AiExplanation>>(
      `/ai/conversations/${conversationId}/messages`,
      { question, requestId },
      { timeout: AI_REQUEST_TIMEOUT },
    ),
  )
}

export async function explainWithTemplate(
  conversationId: number,
  template: AiConversationTemplate,
  requestId: string,
): Promise<AiExplanation> {
  return data(
    await http.post<ApiResponse<AiExplanation>>(
      `/ai/conversations/${conversationId}/templates`,
      { template, requestId },
      { timeout: AI_REQUEST_TIMEOUT },
    ),
  )
}

export async function listAiTasks(): Promise<AiTask[]> {
  return data(await http.get<ApiResponse<AiTask[]>>('/ai/tasks'))
}

export async function listAiUsageRecords(): Promise<AiUsageRecord[]> {
  return data(await http.get<ApiResponse<AiUsageRecord[]>>('/ai/usage-records'))
}

export async function getAdminAiConfig(): Promise<AdminAiConfig> {
  return data(await http.get<ApiResponse<AdminAiConfig>>('/admin/ai/config'))
}
