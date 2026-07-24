export type RoleCode = 'USER' | 'PUBLISHER' | 'ADMIN'
export type UserStatus = 'ACTIVE' | 'DISABLED' | 'LOCKED'

export interface UserProfile {
  id: number
  username: string
  nickname: string
  avatarUrl?: string
  email?: string
  phone?: string
  gender?: string
  bio?: string
  status: UserStatus
  roles: RoleCode[]
  createdAt: string
}

export interface RegisterPayload {
  username: string
  password: string
  nickname: string
  email?: string
  phone?: string
}

export interface LoginPayload {
  username: string
  password: string
}

export interface LoginResult {
  accessToken: string
  tokenType: 'Bearer'
  expiresIn: number
  user: UserProfile
}

export interface UpdateProfilePayload {
  nickname: string
  avatarUrl?: string
  email?: string
  phone?: string
  gender?: string
  bio?: string
}
