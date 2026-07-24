export interface ApiResponse<T> {
  code: number
  message: string
  data: T
  timestamp: string
  traceId?: string
}

export interface PageResult<T> {
  items: T[]
  total: number
  pageNumber: number
  pageSize: number
  totalPages: number
}

export interface HealthStatus {
  status: string
  application: string
}

