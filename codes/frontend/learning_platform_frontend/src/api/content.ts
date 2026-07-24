import { http } from '@/api/http'
import type { ApiResponse } from '@/types/api'
import type {
  CategoryWritePayload,
  CommentPage,
  ContentCategory,
  ContentDetail,
  ContentFile,
  ContentFileRole,
  ContentListParams,
  ContentPage,
  ContentReaction,
  ContentSummary,
  ContentWritePayload,
  LearningProgress,
} from '@/types/content'

function data<T>(response: { data: ApiResponse<T> }): T {
  return response.data.data
}

export async function listCategories(): Promise<ContentCategory[]> {
  return data(await http.get<ApiResponse<ContentCategory[]>>('/categories'))
}

export async function listContents(params: ContentListParams = {}): Promise<ContentPage> {
  return data(await http.get<ApiResponse<ContentPage>>('/contents', { params }))
}

export async function getContent(contentId: number): Promise<ContentDetail> {
  return data(await http.get<ApiResponse<ContentDetail>>(`/contents/${contentId}`))
}

export async function getContentFileUrl(
  contentId: number,
  fileId: number,
  mode: 'preview' | 'download',
): Promise<string> {
  const result = data(
    await http.get<ApiResponse<{ url: string }>>(
      `/contents/${contentId}/files/${fileId}/${mode}-url`,
    ),
  )
  return result.url
}

export async function startLearning(contentId: number): Promise<LearningProgress> {
  return data(
    await http.post<ApiResponse<LearningProgress>>(`/learning/contents/${contentId}/start`),
  )
}

export async function updateLearningProgress(
  contentId: number,
  progressPercent: number,
  lastPosition: string,
): Promise<LearningProgress> {
  return data(
    await http.put<ApiResponse<LearningProgress>>(`/learning/contents/${contentId}/progress`, {
      progressPercent,
      lastPosition,
    }),
  )
}

export async function listLearningProgress(): Promise<LearningProgress[]> {
  return data(await http.get<ApiResponse<LearningProgress[]>>('/learning/progress'))
}

export async function listFavorites(): Promise<ContentSummary[]> {
  return data(await http.get<ApiResponse<ContentSummary[]>>('/learning/favorites'))
}

export async function getReaction(contentId: number): Promise<ContentReaction> {
  return data(await http.get<ApiResponse<ContentReaction>>(`/contents/${contentId}/reactions`))
}

export async function likeContent(contentId: number): Promise<ContentReaction> {
  return data(await http.post<ApiResponse<ContentReaction>>(`/contents/${contentId}/like`))
}

export async function unlikeContent(contentId: number): Promise<ContentReaction> {
  return data(await http.delete<ApiResponse<ContentReaction>>(`/contents/${contentId}/like`))
}

export async function favoriteContent(contentId: number): Promise<ContentReaction> {
  return data(await http.post<ApiResponse<ContentReaction>>(`/contents/${contentId}/favorite`))
}

export async function unfavoriteContent(contentId: number): Promise<ContentReaction> {
  return data(await http.delete<ApiResponse<ContentReaction>>(`/contents/${contentId}/favorite`))
}

export async function listComments(
  contentId: number,
  pageNumber = 1,
  pageSize = 20,
): Promise<CommentPage> {
  return data(
    await http.get<ApiResponse<CommentPage>>(`/contents/${contentId}/comments`, {
      params: { pageNumber, pageSize },
    }),
  )
}

export async function createComment(
  contentId: number,
  body: string,
  parentId?: number,
): Promise<void> {
  await http.post(`/contents/${contentId}/comments`, { body, parentId })
}

export async function listPublisherContents(params: ContentListParams = {}): Promise<ContentPage> {
  return data(await http.get<ApiResponse<ContentPage>>('/publisher/contents', { params }))
}

export async function getPublisherContent(contentId: number): Promise<ContentDetail> {
  return data(await http.get<ApiResponse<ContentDetail>>(`/publisher/contents/${contentId}`))
}

export async function createContent(payload: ContentWritePayload): Promise<ContentDetail> {
  return data(await http.post<ApiResponse<ContentDetail>>('/publisher/contents', payload))
}

export async function updateContent(
  contentId: number,
  payload: ContentWritePayload,
): Promise<ContentDetail> {
  return data(
    await http.put<ApiResponse<ContentDetail>>(`/publisher/contents/${contentId}`, payload),
  )
}

export async function deleteContent(contentId: number): Promise<void> {
  await http.delete(`/publisher/contents/${contentId}`)
}

export async function submitContent(contentId: number): Promise<ContentDetail> {
  return data(await http.post<ApiResponse<ContentDetail>>(`/publisher/contents/${contentId}/submit`))
}

export async function uploadContentFile(
  contentId: number,
  fileRole: ContentFileRole,
  file: File,
  sortOrder = 0,
): Promise<ContentFile> {
  const formData = new FormData()
  formData.append('file', file)
  return data(
    await http.post<ApiResponse<ContentFile>>(`/publisher/contents/${contentId}/files`, formData, {
      params: { fileRole, sortOrder },
      headers: { 'Content-Type': 'multipart/form-data' },
    }),
  )
}

export async function deleteContentFile(contentId: number, fileId: number): Promise<void> {
  await http.delete(`/publisher/contents/${contentId}/files/${fileId}`)
}

export async function listAdminCategories(): Promise<ContentCategory[]> {
  return data(await http.get<ApiResponse<ContentCategory[]>>('/admin/categories'))
}

export async function createCategory(payload: CategoryWritePayload): Promise<ContentCategory> {
  return data(await http.post<ApiResponse<ContentCategory>>('/admin/categories', payload))
}

export async function updateCategory(
  categoryId: number,
  payload: CategoryWritePayload,
): Promise<ContentCategory> {
  return data(
    await http.put<ApiResponse<ContentCategory>>(`/admin/categories/${categoryId}`, payload),
  )
}

export async function deleteCategory(categoryId: number): Promise<void> {
  await http.delete(`/admin/categories/${categoryId}`)
}

export async function listAdminContents(params: ContentListParams = {}): Promise<ContentPage> {
  return data(await http.get<ApiResponse<ContentPage>>('/admin/contents', { params }))
}

export async function getAdminContent(contentId: number): Promise<ContentDetail> {
  return data(await http.get<ApiResponse<ContentDetail>>(`/admin/contents/${contentId}`))
}

export async function approveContent(contentId: number): Promise<void> {
  await http.post(`/admin/contents/${contentId}/approve`)
}

export async function rejectContent(contentId: number, reason: string): Promise<void> {
  await http.post(`/admin/contents/${contentId}/reject`, { reason })
}

export async function offlineContent(contentId: number): Promise<void> {
  await http.post(`/admin/contents/${contentId}/offline`)
}

export async function publishContent(contentId: number): Promise<void> {
  await http.post(`/admin/contents/${contentId}/publish`)
}
