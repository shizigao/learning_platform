import axios from 'axios'

import type { ApiResponse } from '@/types/api'
import { clearAuthSession, getAccessToken } from '@/utils/auth-storage'

/** 401 响应触发的全局事件；认证仓库监听它并同步清空内存中的用户。 */
export const AUTH_UNAUTHORIZED_EVENT = 'learning-platform:unauthorized'

/**
 * 前端统一接口异常。
 *
 * `code` 是后端业务码，`status` 是 HTTP 状态，`traceId` 可交给后端定位日志。
 * 网络未建立时三者可能为空，调用方应优先展示 `message`。
 */
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

/**
 * 全局 Axios 客户端。
 * 业务接口以 `/api` 为默认前缀；页面只应调用本目录导出的接口函数。
 */
export const http = axios.create({
  baseURL: import.meta.env.VITE_API_BASE_URL || '/api',
  timeout: 15_000,
  headers: { 'Content-Type': 'application/json' },
})

/** 创建链路请求号；兼容非安全上下文中不可用的 `crypto.randomUUID`。 */
function createRequestId(): string {
  if (typeof globalThis.crypto?.randomUUID === 'function') {
    return globalThis.crypto.randomUUID()
  }
  const randomPart = Math.random().toString(16).slice(2)
  return `${Date.now().toString(16)}-${randomPart}`
}

/** 登录和注册是公开接口，不能携带可能已经失效的历史 Token。 */
function isPublicAuthRequest(url?: string): boolean {
  if (!url) return false
  const path = url.split('?', 1)[0] ?? ''
  return path.endsWith('/auth/login') || path.endsWith('/auth/register')
}

// 请求拦截器统一注入身份和链路信息，避免各业务 API 重复实现。
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

// 响应拦截器把业务失败、HTTP 失败和网络失败归一为 ApiError。
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
    const payloadTooLargeMessage =
      error.response?.status === 413
        ? '上传文件大小超出服务器限制，请选择符合页面大小限制的文件'
        : undefined
    const responseMessage =
      payloadTooLargeMessage ||
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
