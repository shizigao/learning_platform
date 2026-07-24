import axios from 'axios'

import type { ApiResponse } from '@/types/api'
import { clearAuthSession, getAccessToken } from '@/utils/auth-storage'

export const AUTH_UNAUTHORIZED_EVENT = 'learning-platform:unauthorized'

export class ApiError extends Error {
  readonly code?: number
  readonly traceId?: string
  readonly status?: number

  constructor(message: string, options?: { code?: number; traceId?: string; status?: number }) {
    super(message)
    this.name = 'ApiError'
    this.code = options?.code
    this.traceId = options?.traceId
    this.status = options?.status
  }
}

export const http = axios.create({
  baseURL: import.meta.env.VITE_API_BASE_URL || '/api',
  timeout: 15_000,
  headers: { 'Content-Type': 'application/json' },
})

function createRequestId(): string {
  if (typeof globalThis.crypto?.randomUUID === 'function') {
    return globalThis.crypto.randomUUID()
  }
  const randomPart = Math.random().toString(16).slice(2)
  return `${Date.now().toString(16)}-${randomPart}`
}

function isPublicAuthRequest(url?: string): boolean {
  if (!url) return false
  const path = url.split('?', 1)[0] ?? ''
  return path.endsWith('/auth/login') || path.endsWith('/auth/register')
}

http.interceptors.request.use((config) => {
  const token = getAccessToken()
  // 登录、注册必须与旧会话隔离，避免残留或过期 Token 阻断公开认证接口。
  if (token && !isPublicAuthRequest(config.url)) {
    config.headers.Authorization = `Bearer ${token}`
  } else {
    delete config.headers.Authorization
  }
  // randomUUID 仅在安全上下文中稳定可用；局域网 HTTP 地址需要兼容回退。
  config.headers['X-Request-Id'] = createRequestId()
  return config
})

http.interceptors.response.use(
  (response) => {
    const body = response.data as ApiResponse<unknown>
    if (typeof body?.code === 'number' && body.code !== 0) {
      return Promise.reject(
        new ApiError(body.message || '请求失败', {
          code: body.code,
          traceId: body.traceId,
          status: response.status,
        }),
      )
    }
    return response
  },
  (error: unknown) => {
    if (!axios.isAxiosError<ApiResponse<unknown> | string>(error)) {
      return Promise.reject(
        new ApiError(error instanceof Error ? error.message : '请求初始化失败'),
      )
    }
    if (error.response?.status === 401) {
      clearAuthSession()
      window.dispatchEvent(new Event(AUTH_UNAUTHORIZED_EVENT))
    }
    const body = error.response?.data
    const apiBody = typeof body === 'object' && body !== null ? body : undefined
    const responseMessage =
      apiBody?.message ||
      (typeof body === 'string' && body.trim() ? body.trim() : undefined) ||
      (error.response ? `请求失败（HTTP ${error.response.status}）` : undefined)
    return Promise.reject(
      new ApiError(responseMessage || (error.code === 'ECONNABORTED' ? '请求超时' : '网络连接失败'), {
        code: apiBody?.code,
        traceId: apiBody?.traceId,
        status: error.response?.status,
      }),
    )
  },
)
