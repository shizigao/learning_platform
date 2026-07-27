import { defineStore } from 'pinia'
import { computed, ref } from 'vue'

import {
  getCurrentUser,
  login as loginRequest,
  logout as logoutRequest,
  register as registerRequest,
  updateCurrentUser,
} from '@/api/auth'
import { AUTH_UNAUTHORIZED_EVENT } from '@/api/http'
import type {
  LoginPayload,
  RegisterPayload,
  RoleCode,
  UpdateProfilePayload,
  UserProfile,
} from '@/types/auth'
import { clearAuthSession, getAccessToken, setAuthSession } from '@/utils/auth-storage'

/** 防止 Pinia 热更新或重复初始化时注册多个 401 事件监听器。 */
let unauthorizedListenerRegistered = false

/**
 * 全局认证仓库。
 *
 * 本地存储只保存 Token；用户资料和角色始终通过 `/auth/me` 校验后写入内存，
 * 从而避免把浏览器中可篡改的数据当作权限依据。
 */
export const useAuthStore = defineStore('auth', () => {
  /** 已通过后端校验的当前用户；`null` 表示无有效登录会话。 */
  const user = ref<UserProfile | null>(null)
  /** 登录、注册、退出或资料更新过程中的互斥标志。 */
  const loading = ref(false)
  /** 是否完成本次页面生命周期中的首次会话恢复。 */
  const initialized = ref(false)
  /** 合并路由守卫并发触发的初始化请求，确保 `/auth/me` 只调用一次。 */
  let initializationPromise: Promise<void> | null = null

  const isAuthenticated = computed(() => Boolean(user.value && getAccessToken()))
  const roles = computed<RoleCode[]>(() => user.value?.roles ?? [])

  /** 判断当前用户是否至少具有一个所需角色；空参数始终返回 false。 */
  function hasRole(...requiredRoles: RoleCode[]): boolean {
    return requiredRoles.some((role) => roles.value.includes(role))
  }

  /** 同时清除持久化 Token 和内存用户，不发起网络请求。 */
  function clearSession(): void {
    clearAuthSession()
    user.value = null
  }

  /**
   * 从本地 Token 恢复会话并向后端校验。
   * 失败会静默退回未登录状态，供路由守卫决定是否跳转登录页。
   */
  async function initialize(): Promise<void> {
    if (initialized.value) return
    if (initializationPromise) return initializationPromise
    initializationPromise = (async () => {
      if (!unauthorizedListenerRegistered) {
        window.addEventListener(AUTH_UNAUTHORIZED_EVENT, clearSession)
        unauthorizedListenerRegistered = true
      }
      const token = getAccessToken()
      if (!token) {
        initialized.value = true
        return
      }
      try {
        user.value = await getCurrentUser()
      } catch {
        clearSession()
      } finally {
        initialized.value = true
      }
    })()
    try {
      await initializationPromise
    } finally {
      initializationPromise = null
    }
  }

  /** 登录成功后持久化 Token，并以响应中的用户信息建立当前会话。 */
  async function login(payload: LoginPayload): Promise<UserProfile> {
    loading.value = true
    try {
      const result = await loginRequest(payload)
      setAuthSession(result.accessToken, result.expiresIn)
      user.value = result.user
      initialized.value = true
      return result.user
    } finally {
      loading.value = false
    }
  }

  /** 创建账户但不自动登录，避免注册流程意外覆盖现有会话。 */
  async function register(payload: RegisterPayload): Promise<UserProfile> {
    loading.value = true
    try {
      return await registerRequest(payload)
    } finally {
      loading.value = false
    }
  }

  /** 尝试通知后端注销；无论网络结果如何都保证清理本地会话。 */
  async function logout(): Promise<void> {
    loading.value = true
    try {
      if (getAccessToken()) await logoutRequest()
    } finally {
      clearSession()
      initialized.value = true
      loading.value = false
    }
  }

  /** 更新当前用户资料，并用后端返回值刷新全局用户快照。 */
  async function updateProfile(payload: UpdateProfilePayload): Promise<UserProfile> {
    loading.value = true
    try {
      user.value = await updateCurrentUser(payload)
      return user.value
    } finally {
      loading.value = false
    }
  }

  /** 主动刷新当前用户资料；没有 Token 时不产生请求。 */
  async function refreshProfile(): Promise<UserProfile | null> {
    if (!getAccessToken()) return null
    user.value = await getCurrentUser()
    return user.value
  }

  return {
    user,
    loading,
    initialized,
    isAuthenticated,
    roles,
    hasRole,
    initialize,
    login,
    register,
    logout,
    updateProfile,
    refreshProfile,
    clearSession,
  }
})
