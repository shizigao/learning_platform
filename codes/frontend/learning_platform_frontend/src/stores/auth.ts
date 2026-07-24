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

let unauthorizedListenerRegistered = false

export const useAuthStore = defineStore('auth', () => {
  const user = ref<UserProfile | null>(null)
  const loading = ref(false)
  const initialized = ref(false)
  let initializationPromise: Promise<void> | null = null

  const isAuthenticated = computed(() => Boolean(user.value && getAccessToken()))
  const roles = computed<RoleCode[]>(() => user.value?.roles ?? [])

  function hasRole(...requiredRoles: RoleCode[]): boolean {
    return requiredRoles.some((role) => roles.value.includes(role))
  }

  function clearSession(): void {
    clearAuthSession()
    user.value = null
  }

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

  async function register(payload: RegisterPayload): Promise<UserProfile> {
    loading.value = true
    try {
      return await registerRequest(payload)
    } finally {
      loading.value = false
    }
  }

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

  async function updateProfile(payload: UpdateProfilePayload): Promise<UserProfile> {
    loading.value = true
    try {
      user.value = await updateCurrentUser(payload)
      return user.value
    } finally {
      loading.value = false
    }
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
    clearSession,
  }
})
