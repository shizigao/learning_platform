import { http } from '@/api/http'
import type {
  AdminExamDetail,
  AdminExamListParams,
  AdminExamPage,
  AdminUser,
  AdminUserListParams,
  AdminUserPage,
  OperationLogListParams,
  OperationLogPage,
} from '@/types/admin'
import type { ApiResponse } from '@/types/api'
import type { RoleCode, UserStatus } from '@/types/auth'

function data<T>(response: { data: ApiResponse<T> }): T {
  return response.data.data
}

export async function listAdminUsers(
  params: AdminUserListParams = {},
): Promise<AdminUserPage> {
  return data(await http.get<ApiResponse<AdminUserPage>>('/admin/users', { params }))
}

export async function updateAdminUserStatus(
  userId: number,
  status: UserStatus,
): Promise<AdminUser> {
  return data(
    await http.put<ApiResponse<AdminUser>>(`/admin/users/${userId}/status`, { status }),
  )
}

export async function updateAdminUserRoles(
  userId: number,
  roles: RoleCode[],
): Promise<AdminUser> {
  return data(
    await http.put<ApiResponse<AdminUser>>(`/admin/users/${userId}/roles`, { roles }),
  )
}

export async function listAdminExams(
  params: AdminExamListParams = {},
): Promise<AdminExamPage> {
  return data(await http.get<ApiResponse<AdminExamPage>>('/admin/exams', { params }))
}

export async function getAdminExam(examId: number): Promise<AdminExamDetail> {
  return data(await http.get<ApiResponse<AdminExamDetail>>(`/admin/exams/${examId}`))
}

export async function listOperationLogs(
  params: OperationLogListParams = {},
): Promise<OperationLogPage> {
  return data(
    await http.get<ApiResponse<OperationLogPage>>('/admin/operation-logs', { params }),
  )
}
