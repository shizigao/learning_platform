import { http } from '@/api/http'
import type { ApiResponse } from '@/types/api'
import type {
  ClassAnnouncement,
  ClassContentPage,
  ClassExamPage,
  ClassMemberPage,
  ClassRole,
  Classroom,
} from '@/types/classroom'

function data<T>(response: { data: ApiResponse<T> }): T {
  return response.data.data
}

export async function listMyClasses(): Promise<Classroom[]> {
  return data(await http.get<ApiResponse<Classroom[]>>('/classes'))
}

export async function joinClass(inviteCode: string): Promise<Classroom> {
  return data(await http.post<ApiResponse<Classroom>>('/classes/join', { inviteCode }))
}

export async function getClassDetail(classId: number): Promise<Classroom> {
  return data(await http.get<ApiResponse<Classroom>>(`/classes/${classId}`))
}

export async function leaveClass(classId: number): Promise<void> {
  await http.post(`/classes/${classId}/leave`)
}

export async function listClassMembers(
  classId: number,
  keyword = '',
  pageNumber = 1,
  pageSize = 20,
): Promise<ClassMemberPage> {
  return data(
    await http.get<ApiResponse<ClassMemberPage>>(`/classes/${classId}/members`, {
      params: { keyword: keyword.trim() || undefined, pageNumber, pageSize },
    }),
  )
}

export async function listClassAnnouncements(classId: number): Promise<ClassAnnouncement[]> {
  return data(
    await http.get<ApiResponse<ClassAnnouncement[]>>(`/classes/${classId}/announcements`),
  )
}

export async function createClassAnnouncement(
  classId: number,
  payload: { title: string; body: string; pinned: boolean },
): Promise<ClassAnnouncement> {
  return data(
    await http.post<ApiResponse<ClassAnnouncement>>(
      `/classes/${classId}/announcements`,
      payload,
    ),
  )
}

export async function updateClassAnnouncement(
  classId: number,
  announcementId: number,
  payload: { title: string; body: string; pinned: boolean },
): Promise<ClassAnnouncement> {
  return data(
    await http.put<ApiResponse<ClassAnnouncement>>(
      `/classes/${classId}/announcements/${announcementId}`,
      payload,
    ),
  )
}

export async function deleteClassAnnouncement(
  classId: number,
  announcementId: number,
): Promise<void> {
  await http.delete(`/classes/${classId}/announcements/${announcementId}`)
}

export async function listClassContents(
  classId: number,
  pageNumber = 1,
  pageSize = 12,
): Promise<ClassContentPage> {
  return data(
    await http.get<ApiResponse<ClassContentPage>>(`/classes/${classId}/contents`, {
      params: { pageNumber, pageSize },
    }),
  )
}

export async function listClassExams(
  classId: number,
  pageNumber = 1,
  pageSize = 12,
): Promise<ClassExamPage> {
  return data(
    await http.get<ApiResponse<ClassExamPage>>(`/classes/${classId}/exams`, {
      params: { pageNumber, pageSize },
    }),
  )
}

export async function listManagedClasses(): Promise<Classroom[]> {
  return data(await http.get<ApiResponse<Classroom[]>>('/class-management/classes'))
}

export async function createClass(payload: {
  name: string
  description: string
}): Promise<Classroom> {
  return data(await http.post<ApiResponse<Classroom>>('/class-management/classes', payload))
}

export async function updateClass(
  classId: number,
  payload: { name: string; description: string },
): Promise<Classroom> {
  return data(
    await http.put<ApiResponse<Classroom>>(`/class-management/classes/${classId}`, payload),
  )
}

export async function regenerateClassInvite(classId: number): Promise<Classroom> {
  return data(
    await http.post<ApiResponse<Classroom>>(
      `/class-management/classes/${classId}/invite/regenerate`,
    ),
  )
}

export async function setClassInviteEnabled(
  classId: number,
  enabled: boolean,
): Promise<Classroom> {
  return data(
    await http.put<ApiResponse<Classroom>>(
      `/class-management/classes/${classId}/invite`,
      { enabled },
    ),
  )
}

export async function setClassMemberRole(
  classId: number,
  userId: number,
  role: Exclude<ClassRole, 'OWNER'>,
): Promise<void> {
  await http.put(`/class-management/classes/${classId}/members/${userId}/role`, { role })
}

export async function removeClassMember(classId: number, userId: number): Promise<void> {
  await http.delete(`/class-management/classes/${classId}/members/${userId}`)
}

export async function transferClassOwnership(classId: number, userId: number): Promise<Classroom> {
  return data(
    await http.put<ApiResponse<Classroom>>(`/class-management/classes/${classId}/owner`, {
      userId,
    }),
  )
}

export async function archiveClass(classId: number): Promise<void> {
  await http.delete(`/class-management/classes/${classId}`)
}
