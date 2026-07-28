import { http } from '@/api/http'
import type { ApiResponse } from '@/types/api'
import type {
  StudentPreference,
  TeacherApplication,
  TeacherApplicationPage,
  TeacherApplicationPayload,
  TeacherApplicationStatus,
  TeacherPage,
  TeacherProfile,
  TeacherRecommendation,
  TeacherSearchParams,
} from '@/types/offline-teaching'

const AI_REQUEST_TIMEOUT = 645_000

function data<T>(response: { data: ApiResponse<T> }): T {
  return response.data.data
}

export async function searchTeachers(params: TeacherSearchParams = {}): Promise<TeacherPage> {
  return data(await http.get<ApiResponse<TeacherPage>>('/offline-teaching/teachers', { params }))
}

export async function getTeacher(teacherId: number): Promise<TeacherProfile> {
  return data(
    await http.get<ApiResponse<TeacherProfile>>(`/offline-teaching/teachers/${teacherId}`),
  )
}

export async function getTeacherByUser(userId: number): Promise<TeacherProfile> {
  return data(
    await http.get<ApiResponse<TeacherProfile>>(`/offline-teaching/teachers/by-user/${userId}`),
  )
}

export async function getTeacherApplication(): Promise<TeacherApplication | null> {
  return data(
    await http.get<ApiResponse<TeacherApplication | null>>('/offline-teaching/application'),
  )
}

export async function saveTeacherApplication(
  payload: TeacherApplicationPayload,
): Promise<TeacherApplication> {
  return data(
    await http.put<ApiResponse<TeacherApplication>>('/offline-teaching/application', payload),
  )
}

export async function submitTeacherApplication(): Promise<TeacherApplication> {
  return data(
    await http.post<ApiResponse<TeacherApplication>>('/offline-teaching/application/submit'),
  )
}

export async function getStudentPreference(): Promise<StudentPreference | null> {
  return data(
    await http.get<ApiResponse<StudentPreference | null>>('/offline-teaching/preference'),
  )
}

export async function saveStudentPreference(
  payload: StudentPreference,
): Promise<StudentPreference> {
  return data(
    await http.put<ApiResponse<StudentPreference>>('/offline-teaching/preference', payload),
  )
}

export async function recommendTeachers(
  requestId: string,
  preference: StudentPreference,
): Promise<TeacherRecommendation> {
  return data(
    // 向后端发起请求
    await http.post<ApiResponse<TeacherRecommendation>>(
      '/offline-teaching/recommendations',
      { requestId, preference },
      { timeout: AI_REQUEST_TIMEOUT },
    ),
  )
}

export async function listTeacherApplications(params: {
  keyword?: string
  status?: TeacherApplicationStatus
  pageNumber?: number
  pageSize?: number
}): Promise<TeacherApplicationPage> {
  return data(
    await http.get<ApiResponse<TeacherApplicationPage>>(
      '/admin/offline-teachers/applications',
      { params },
    ),
  )
}

export async function getAdminTeacherApplication(id: number): Promise<TeacherApplication> {
  return data(
    await http.get<ApiResponse<TeacherApplication>>(
      `/admin/offline-teachers/applications/${id}`,
    ),
  )
}

export async function getAdminTeacherProfileByUser(userId: number): Promise<TeacherProfile> {
  return data(
    await http.get<ApiResponse<TeacherProfile>>(
      `/admin/offline-teachers/profiles/by-user/${userId}`,
    ),
  )
}

export async function approveTeacherApplication(id: number): Promise<TeacherApplication> {
  return data(
    await http.post<ApiResponse<TeacherApplication>>(
      `/admin/offline-teachers/applications/${id}/approve`,
    ),
  )
}

export async function rejectTeacherApplication(
  id: number,
  reason: string,
): Promise<TeacherApplication> {
  return data(
    await http.post<ApiResponse<TeacherApplication>>(
      `/admin/offline-teachers/applications/${id}/reject`,
      { reason },
    ),
  )
}

export async function suspendTeacherProfile(
  id: number,
  reason: string,
): Promise<TeacherProfile> {
  return data(
    await http.put<ApiResponse<TeacherProfile>>(
      `/admin/offline-teachers/profiles/${id}/suspend`,
      { reason },
    ),
  )
}

export async function activateTeacherProfile(id: number): Promise<TeacherProfile> {
  return data(
    await http.put<ApiResponse<TeacherProfile>>(
      `/admin/offline-teachers/profiles/${id}/activate`,
    ),
  )
}
