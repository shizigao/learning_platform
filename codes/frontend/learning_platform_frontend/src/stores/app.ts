import { defineStore } from 'pinia'
import { computed, ref } from 'vue'

import { useAuthStore } from '@/stores/auth'

export interface NavigationItem {
  label: string
  path: string
}

export const useAppStore = defineStore('app', () => {
  const authStore = useAuthStore()
  const title = ref(import.meta.env.VITE_APP_TITLE || '智能在线学习考试平台')
  const navigationItems = computed<NavigationItem[]>(() => {
    const items: NavigationItem[] = [
      { label: '首页', path: '/' },
      { label: '学习资料', path: '/contents' },
      { label: '考试中心', path: '/exams' },
      { label: 'AI 学习助手', path: '/ai-assistant' },
    ]
    if (authStore.hasRole('PUBLISHER', 'ADMIN')) {
      items.push({ label: '发布者工作台', path: '/publisher' })
    }
    if (authStore.hasRole('ADMIN')) {
      items.push({ label: '管理后台', path: '/admin' })
      items.push({ label: 'AI 配置', path: '/admin/ai' })
    }
    return items
  })

  return { title, navigationItems }
})
