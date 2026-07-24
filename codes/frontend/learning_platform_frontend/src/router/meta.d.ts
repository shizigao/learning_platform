import 'vue-router'

import type { RoleCode } from '@/types/auth'

declare module 'vue-router' {
  interface RouteMeta {
    title?: string
    description?: string
    requiresAuth?: boolean
    guestOnly?: boolean
    roles?: RoleCode[]
  }
}

export {}
