const ACCESS_TOKEN_KEY = 'learning-platform.access-token'
const ACCESS_TOKEN_EXPIRES_AT_KEY = 'learning-platform.access-token-expires-at'

export function getAccessToken(): string | null {
  const token = window.localStorage.getItem(ACCESS_TOKEN_KEY)
  const expiresAt = Number(window.localStorage.getItem(ACCESS_TOKEN_EXPIRES_AT_KEY))
  if (!token) return null
  if (Number.isFinite(expiresAt) && expiresAt > 0 && Date.now() >= expiresAt) {
    clearAuthSession()
    return null
  }
  return token
}

export function setAuthSession(token: string, expiresInSeconds: number): void {
  window.localStorage.setItem(ACCESS_TOKEN_KEY, token)
  window.localStorage.setItem(
    ACCESS_TOKEN_EXPIRES_AT_KEY,
    String(Date.now() + expiresInSeconds * 1000),
  )
}

export function clearAuthSession(): void {
  window.localStorage.removeItem(ACCESS_TOKEN_KEY)
  window.localStorage.removeItem(ACCESS_TOKEN_EXPIRES_AT_KEY)
}
