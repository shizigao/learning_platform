import { http } from '@/api/http'
import type { ApiResponse } from '@/types/api'
import type {
  PublicUserContentPage,
  PublicUserPage,
  PublicUserProfile,
} from '@/types/user'

function data<T>(response: { data: ApiResponse<T> }): T {
  return response.data.data
}

export async function getPublicUser(userId: number): Promise<PublicUserProfile> {
  return data(await http.get<ApiResponse<PublicUserProfile>>(`/users/${userId}`))
}

export async function searchPublicUsers(
  keyword: string,
  pageNumber = 1,
  pageSize = 12,
): Promise<PublicUserPage> {
  return data(
    await http.get<ApiResponse<PublicUserPage>>('/users/search', {
      params: { keyword: keyword.trim() || undefined, pageNumber, pageSize },
    }),
  )
}

export async function listPublicUserContents(
  userId: number,
  pageNumber = 1,
  pageSize = 12,
): Promise<PublicUserContentPage> {
  return data(
    await http.get<ApiResponse<PublicUserContentPage>>(`/users/${userId}/contents`, {
      params: { pageNumber, pageSize },
    }),
  )
}

export async function uploadCurrentUserAvatar(file: File): Promise<string> {
  const formData = new FormData()
  formData.append('file', file)
  const result = data(
    await http.post<ApiResponse<{ avatarUrl: string }>>('/users/me/avatar', formData, {
      headers: { 'Content-Type': 'multipart/form-data' },
      timeout: 60_000,
    }),
  )
  return result.avatarUrl
}

export async function deleteCurrentUserAvatar(): Promise<void> {
  await http.delete('/users/me/avatar')
}
