export type AiTaskType =
  | 'SUMMARY'
  | 'EXPLANATION'
  | 'EXAM_OVERALL_ANALYSIS'
  | 'EXAM_PERSONAL_ANALYSIS'
export type AiTaskStatus = 'PENDING' | 'RUNNING' | 'SUCCEEDED' | 'FAILED'
export type AiMessageRole = 'SYSTEM' | 'USER' | 'ASSISTANT'
export type AiConversationStatus = 'ACTIVE' | 'ARCHIVED'

export interface AiTask {
  id: number
  requestId: string
  contentId?: number
  conversationId?: number
  taskType: AiTaskType
  provider: string
  model: string
  status: AiTaskStatus
  inputChars: number
  quotaCost: number
  errorCode?: string
  errorMessage?: string
  startedAt?: string
  finishedAt?: string
  createdAt: string
}

export interface AiSummary {
  id: number
  task: AiTask
  contentId: number
  summary: string
  knowledgePoints: string[]
  reviewOutline: string
  sourceVersion: string
  createdAt: string
}

export interface AiMessage {
  id: number
  taskId?: number
  role: AiMessageRole
  content: string
  sequenceNo: number
  tokenCount?: number
  createdAt: string
}

export interface AiConversation {
  id: number
  contentId: number
  title: string
  status: AiConversationStatus
  lastMessageAt?: string
  messages: AiMessage[]
  createdAt: string
  updatedAt: string
}

export interface AiExplanation {
  task: AiTask
  conversationId: number
  question: AiMessage
  answer: AiMessage
}

export interface AiUsageRecord {
  id: number
  businessNo: string
  taskId: number
  entitlementId: number
  usageType: AiTaskType
  quantity: number
  balanceBefore: number
  balanceAfter: number
  status: 'CONSUMED'
  remark?: string
  createdAt: string
}

export interface AdminAiConfig {
  provider: string
  model: string
  mockMode: boolean
  mockScenario?: string
  apiKeyConfigured: boolean
  baseUrl?: string
  thinkingEnabled: boolean
  limits: {
    maxInputChars: number
    maxContextMessages: number
    maxContextChars: number
    requestsPerWindow: number
    rateWindowSeconds: number
    maxConcurrentPerUser: number
    requestTimeoutSeconds: number
    providerConnectTimeoutSeconds: number
    providerTimeoutSeconds: number
  }
}
