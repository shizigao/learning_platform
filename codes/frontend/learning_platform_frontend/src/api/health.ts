import axios from 'axios'

import type { ApiResponse, HealthStatus } from '@/types/api'

// 健康检查必须独立于登录会话。复用业务 http 客户端会携带残留 JWT，
// 过期 Token 的 401 响应会被首页误判为后端离线。
const healthHttp = axios.create({
  baseURL: import.meta.env.VITE_API_BASE_URL || '/api',
  timeout: 5_000,
  headers: { Accept: 'application/json' },
})

export async function getHealthStatus(): Promise<HealthStatus> {
  const response = await healthHttp.get<ApiResponse<HealthStatus>>('/health')
  if (response.data.code !== 0 || !response.data.data) {
    throw new Error(response.data.message || '后端健康检查失败')
  }
  return response.data.data
}
