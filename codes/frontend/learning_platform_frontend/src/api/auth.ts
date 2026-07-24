import { http } from '@/api/http'
import type { ApiResponse } from '@/types/api'
import type {
  LoginPayload,
  LoginResult,
  RegisterPayload,
  UpdateProfilePayload,
  UserProfile,
} from '@/types/auth'

export async function register(payload: RegisterPayload): Promise<UserProfile> {
  const response = await http.post<ApiResponse<UserProfile>>('/auth/register', payload)
  return response.data.data
}

export async function login(payload: LoginPayload): Promise<LoginResult> {
  const response = await http.post<ApiResponse<LoginResult>>('/auth/login', payload)
  return response.data.data
}

export async function logout(): Promise<void> {
  await http.post<ApiResponse<null>>('/auth/logout')
}

export async function getCurrentUser(): Promise<UserProfile> {
  const response = await http.get<ApiResponse<UserProfile>>('/auth/me')
  return response.data.data
}

export async function updateCurrentUser(payload: UpdateProfilePayload): Promise<UserProfile> {
  const response = await http.put<ApiResponse<UserProfile>>('/auth/me', payload)
  return response.data.data
}
