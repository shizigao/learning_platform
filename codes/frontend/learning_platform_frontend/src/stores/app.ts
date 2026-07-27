import { defineStore } from 'pinia'
import { computed, ref } from 'vue'

import { useAuthStore } from '@/stores/auth'

/** 顶部导航最小数据模型；权限判断在生成菜单时完成。 */
export interface NavigationItem {
  label: string
  path: string
}

/** 提供应用标题和根据认证角色动态计算的全局导航。 */
export const useAppStore = defineStore('app', () => {
  const authStore = useAuthStore()
  const title = ref(import.meta.env.VITE_APP_TITLE || '智能在线学习考试平台')
  /** 菜单只是可见性控制，路由守卫和后端仍会独立执行权限校验。 */
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
      items.push({ label: '教师审核', path: '/admin/offline-teachers' })
      items.push({ label: 'AI 配置', path: '/admin/ai' })
    }
    return items
  })

  return { title, navigationItems }
})
