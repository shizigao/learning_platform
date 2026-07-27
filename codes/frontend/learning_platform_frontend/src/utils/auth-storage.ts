/** localStorage 中只保存凭证和本地过期时间，不保存角色等可篡改权限数据。 */
const ACCESS_TOKEN_KEY = 'learning-platform.access-token'
const ACCESS_TOKEN_EXPIRES_AT_KEY = 'learning-platform.access-token-expires-at'

/**
 * 读取尚未过期的访问令牌。
 * 若本地过期时间已到，会产生清除会话存储的副作用并返回 `null`。
 */
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

/** 保存访问令牌，并根据后端返回的秒数计算浏览器侧绝对过期时间。 */
export function setAuthSession(token: string, expiresInSeconds: number): void {
  window.localStorage.setItem(ACCESS_TOKEN_KEY, token)
  window.localStorage.setItem(
    ACCESS_TOKEN_EXPIRES_AT_KEY,
    String(Date.now() + expiresInSeconds * 1000),
  )
}

/** 删除全部认证持久化信息；内存中的用户由认证仓库负责清理。 */
export function clearAuthSession(): void {
  window.localStorage.removeItem(ACCESS_TOKEN_KEY)
  window.localStorage.removeItem(ACCESS_TOKEN_EXPIRES_AT_KEY)
}
