import { createRouter, createWebHistory } from 'vue-router'

import DefaultLayout from '@/layouts/DefaultLayout.vue'
import { useAuthStore } from '@/stores/auth'

const router = createRouter({
  history: createWebHistory(import.meta.env.BASE_URL),
  routes: [
    {
      path: '/',
      component: DefaultLayout,
      children: [
        {
          path: '',
          name: 'home',
          component: () => import('@/views/HomeView.vue'),
          meta: { title: '首页' },
        },
        {
          path: 'contents',
          name: 'contents',
          component: () => import('@/views/ContentsView.vue'),
          meta: {
            title: '学习资料',
            requiresAuth: true,
            roles: ['USER', 'PUBLISHER', 'ADMIN'],
          },
        },
        {
          path: 'contents/:id',
          name: 'content-detail',
          component: () => import('@/views/ContentDetailView.vue'),
          meta: {
            title: '资料详情',
            requiresAuth: true,
            roles: ['USER', 'PUBLISHER', 'ADMIN'],
          },
        },
        {
          path: 'contents/:id/learn',
          name: 'content-learning',
          component: () => import('@/views/LearningView.vue'),
          meta: {
            title: '在线学习',
            requiresAuth: true,
            roles: ['USER', 'PUBLISHER', 'ADMIN'],
          },
        },
        {
          path: 'exams',
          name: 'exams',
          component: () => import('@/views/ExamsView.vue'),
          meta: {
            title: '考试中心',
            requiresAuth: true,
            roles: ['USER', 'PUBLISHER', 'ADMIN'],
          },
        },
        {
          path: 'exams/:id',
          name: 'exam-entry',
          component: () => import('@/views/ExamEntryView.vue'),
          meta: {
            title: '考试说明',
            requiresAuth: true,
            roles: ['USER', 'PUBLISHER', 'ADMIN'],
          },
        },
        {
          path: 'exams/:id/result',
          name: 'exam-result',
          component: () => import('@/views/ExamResultView.vue'),
          meta: {
            title: '考试成绩',
            requiresAuth: true,
            roles: ['USER', 'PUBLISHER', 'ADMIN'],
          },
        },
        {
          path: 'exams/:id/result/ai-analysis',
          name: 'exam-personal-ai-analysis',
          component: () => import('@/views/ExamAiAnalysisView.vue'),
          meta: {
            title: '考试个人 AI 分析',
            requiresAuth: true,
            roles: ['USER', 'PUBLISHER', 'ADMIN'],
          },
        },
        {
          path: 'ai-assistant',
          name: 'ai-assistant',
          component: () => import('@/views/AiAssistantView.vue'),
          meta: {
            title: 'AI 学习助手',
            requiresAuth: true,
            roles: ['USER', 'PUBLISHER', 'ADMIN'],
          },
        },
        {
          path: 'my-learning',
          name: 'my-learning',
          component: () => import('@/views/MyLearningView.vue'),
          meta: { title: '我的学习', requiresAuth: true, roles: ['USER', 'PUBLISHER', 'ADMIN'] },
        },
        {
          path: 'favorites',
          name: 'favorites',
          component: () => import('@/views/FavoritesView.vue'),
          meta: { title: '我的收藏', requiresAuth: true, roles: ['USER', 'PUBLISHER', 'ADMIN'] },
        },
        {
          path: 'profile',
          name: 'profile',
          component: () => import('@/views/ProfileView.vue'),
          meta: { title: '个人中心', requiresAuth: true, roles: ['USER', 'PUBLISHER', 'ADMIN'] },
        },
        {
          path: 'users/:id',
          name: 'public-user-profile',
          component: () => import('@/views/PublicUserProfileView.vue'),
          meta: { title: '用户个人中心', requiresAuth: true, roles: ['USER', 'PUBLISHER', 'ADMIN'] },
        },
        {
          path: 'classes',
          name: 'my-classes',
          component: () => import('@/views/MyClassesView.vue'),
          meta: {
            title: '我的班级',
            requiresAuth: true,
            roles: ['USER', 'PUBLISHER', 'ADMIN'],
          },
        },
        {
          path: 'offline-teaching',
          name: 'offline-teaching',
          component: () => import('@/views/OfflineTeachingView.vue'),
          meta: {
            title: '线下教学',
            requiresAuth: true,
            roles: ['USER', 'PUBLISHER', 'ADMIN'],
          },
        },
        {
          path: 'class-management',
          name: 'class-management',
          component: () => import('@/views/ClassManagementView.vue'),
          meta: {
            title: '班级管理',
            requiresAuth: true,
            roles: ['PUBLISHER', 'ADMIN'],
          },
        },
        {
          path: 'commerce',
          name: 'commerce',
          component: () => import('@/views/CommerceView.vue'),
          meta: {
            title: '商品、订单与权益',
            requiresAuth: true,
            roles: ['USER', 'PUBLISHER', 'ADMIN'],
          },
        },
        {
          path: 'publisher',
          name: 'publisher',
          component: () => import('@/views/PublisherContentsView.vue'),
          meta: {
            title: '发布者工作台',
            requiresAuth: true,
            roles: ['PUBLISHER', 'ADMIN'],
          },
        },
        {
          path: 'publisher/contents/new',
          name: 'publisher-content-new',
          component: () => import('@/views/PublisherContentEditorView.vue'),
          meta: {
            title: '新建学习资料',
            requiresAuth: true,
            roles: ['PUBLISHER', 'ADMIN'],
          },
        },
        {
          path: 'publisher/contents/:id/edit',
          name: 'publisher-content-edit',
          component: () => import('@/views/PublisherContentEditorView.vue'),
          meta: {
            title: '编辑学习资料',
            requiresAuth: true,
            roles: ['PUBLISHER', 'ADMIN'],
          },
        },
        {
          path: 'publisher/questions',
          name: 'publisher-questions',
          component: () => import('@/views/PublisherQuestionsView.vue'),
          meta: {
            title: '题库管理',
            requiresAuth: true,
            roles: ['PUBLISHER', 'ADMIN'],
          },
        },
        {
          path: 'publisher/papers',
          name: 'publisher-papers',
          component: () => import('@/views/PublisherPapersView.vue'),
          meta: {
            title: '试卷',
            requiresAuth: true,
            roles: ['PUBLISHER', 'ADMIN'],
          },
        },
        {
          path: 'publisher/exams',
          name: 'publisher-exams',
          component: () => import('@/views/PublisherExamsView.vue'),
          meta: {
            title: '考试管理',
            requiresAuth: true,
            roles: ['PUBLISHER', 'ADMIN'],
          },
        },
        {
          path: 'publisher/exams/:id/grading',
          name: 'publisher-exam-grading',
          component: () => import('@/views/PublisherExamGradingView.vue'),
          meta: {
            title: '阅卷与统计',
            requiresAuth: true,
            roles: ['PUBLISHER', 'ADMIN'],
          },
        },
        {
          path: 'publisher/exams/:id/grading/ai-analysis',
          name: 'publisher-exam-ai-analysis',
          component: () => import('@/views/ExamAiAnalysisView.vue'),
          meta: {
            title: '考试整体 AI 分析',
            requiresAuth: true,
            roles: ['PUBLISHER', 'ADMIN'],
          },
        },
        {
          path: 'admin',
          name: 'admin',
          component: () => import('@/views/AdminWorkspaceView.vue'),
          meta: {
            title: '管理后台',
            requiresAuth: true,
            roles: ['ADMIN'],
          },
        },
        {
          path: 'admin/ai',
          name: 'admin-ai',
          component: () => import('@/views/AdminAiConfigView.vue'),
          meta: {
            title: 'AI 运行配置',
            requiresAuth: true,
            roles: ['ADMIN'],
          },
        },
        {
          path: 'admin/offline-teachers',
          name: 'admin-offline-teachers',
          component: () => import('@/views/AdminOfflineTeachersView.vue'),
          meta: {
            title: '线下教师审核',
            requiresAuth: true,
            roles: ['ADMIN'],
          },
        },
      ],
    },
    {
      path: '/auth',
      component: () => import('@/layouts/AuthLayout.vue'),
      children: [
        {
          path: '/login',
          name: 'login',
          component: () => import('@/views/LoginView.vue'),
          meta: { title: '登录', guestOnly: true },
        },
        {
          path: '/register',
          name: 'register',
          component: () => import('@/views/RegisterView.vue'),
          meta: { title: '注册', guestOnly: true },
        },
      ],
    },
    {
      path: '/error',
      name: 'error',
      component: () => import('@/views/ErrorView.vue'),
      meta: { title: '页面加载异常' },
    },
    {
      path: '/:pathMatch(.*)*',
      name: 'not-found',
      component: () => import('@/views/NotFoundView.vue'),
      meta: { title: '页面不存在' },
    },
  ],
  scrollBehavior: () => ({ top: 0 }),
})

router.beforeEach(async (to) => {
  const authStore = useAuthStore()
  await authStore.initialize()

  if (to.meta.guestOnly && authStore.isAuthenticated) {
    return { name: 'home' }
  }
  if (to.meta.requiresAuth && !authStore.isAuthenticated) {
    return { name: 'login', query: { redirect: to.fullPath } }
  }
  if (to.meta.roles?.length && !authStore.hasRole(...to.meta.roles)) {
    return { name: 'home', query: { forbidden: '1' } }
  }
  return true
})

router.afterEach((to) => {
  const pageTitle = typeof to.meta.title === 'string' ? to.meta.title : ''
  const appTitle = import.meta.env.VITE_APP_TITLE || '智能在线学习考试平台'
  document.title = pageTitle ? `${pageTitle} - ${appTitle}` : appTitle
})

router.onError((error, to) => {
  if (to.name === 'error') return
  void router.replace({
    name: 'error',
    query: { message: '页面资源加载失败，请检查网络后重试。' },
  })
  console.error('Route loading failed', error)
})

export default router
