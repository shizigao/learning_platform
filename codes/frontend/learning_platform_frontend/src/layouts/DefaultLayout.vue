<script setup lang="ts">
import { CollectionTag, Location, Reading, Setting, ShoppingBag, SwitchButton, User, UserFilled } from '@element-plus/icons-vue'
import { ElMessage } from 'element-plus'
import { storeToRefs } from 'pinia'
import { computed } from 'vue'
import { useRouter } from 'vue-router'

import AppLogo from '@/components/AppLogo.vue'
import { useAppStore } from '@/stores/app'
import { useAuthStore } from '@/stores/auth'

const appStore = useAppStore()
const authStore = useAuthStore()
const router = useRouter()
const { navigationItems } = storeToRefs(appStore)
const { user, isAuthenticated } = storeToRefs(authStore)
const userInitial = computed(() => user.value?.nickname?.slice(0, 1) || '学')
const canManageClasses = computed(() => authStore.hasRole('PUBLISHER', 'ADMIN'))

async function handleUserCommand(command: string): Promise<void> {
  if (command === 'profile') {
    await router.push('/profile')
    return
  }
  if (command === 'favorites') {
    await router.push('/favorites')
    return
  }
  if (command === 'commerce') {
    await router.push('/commerce')
    return
  }
  if (command === 'classes') {
    await router.push('/classes')
    return
  }
  if (command === 'class-management') {
    await router.push('/class-management')
    return
  }
  if (command === 'offline-teaching') {
    await router.push('/offline-teaching')
    return
  }
  if (command === 'logout') {
    await authStore.logout()
    ElMessage.success('已安全退出')
    await router.push('/')
  }
}
</script>

<template>
  <div class="app-shell">
    <header class="app-header">
      <div class="page-container header-inner">
        <RouterLink to="/" aria-label="返回首页"><AppLogo /></RouterLink>
        <nav class="desktop-nav" aria-label="主要导航">
          <RouterLink
            v-for="item in navigationItems"
            :key="item.path"
            :to="item.path"
            class="nav-link"
          >
            {{ item.label }}
          </RouterLink>
        </nav>
        <div class="header-actions">
          <RouterLink v-if="isAuthenticated" to="/commerce">
            <el-button text :icon="ShoppingBag">权益商城</el-button>
          </RouterLink>
          <RouterLink v-if="isAuthenticated" to="/my-learning">
            <el-button text :icon="Reading">我的学习</el-button>
          </RouterLink>
          <el-dropdown v-if="isAuthenticated" trigger="click" @command="handleUserCommand">
            <button class="user-trigger" type="button">
              <el-avatar :size="34" :src="user?.avatarUrl">{{ userInitial }}</el-avatar>
              <span>{{ user?.nickname }}</span>
            </button>
            <template #dropdown>
              <el-dropdown-menu>
                <el-dropdown-item command="profile" :icon="User">个人中心</el-dropdown-item>
                <el-dropdown-item command="favorites" :icon="CollectionTag">我的收藏</el-dropdown-item>
                <el-dropdown-item command="commerce" :icon="ShoppingBag">订单与权益</el-dropdown-item>
                <el-dropdown-item command="classes" :icon="UserFilled">我的班级</el-dropdown-item>
                <el-dropdown-item command="offline-teaching" :icon="Location">线下教学</el-dropdown-item>
                <el-dropdown-item
                  v-if="canManageClasses"
                  command="class-management"
                  :icon="Setting"
                >
                  班级管理
                </el-dropdown-item>
                <el-dropdown-item command="logout" :icon="SwitchButton" divided>退出登录</el-dropdown-item>
              </el-dropdown-menu>
            </template>
          </el-dropdown>
          <RouterLink v-else to="/login">
            <el-button type="primary" :icon="User">登录 / 注册</el-button>
          </RouterLink>
        </div>
      </div>
    </header>
    <main class="app-main"><RouterView /></main>
    <footer class="app-footer">
      <div class="page-container footer-inner">
        <span>智能在线学习考试平台</span><span>版本：v1.3</span>
      </div>
    </footer>
  </div>
</template>

<style scoped>
.app-shell { display: flex; min-height: 100vh; flex-direction: column; }
.app-header { position: sticky; z-index: 20; top: 0; border-bottom: 1px solid rgb(228 231 236 / 85%); background: rgb(255 255 255 / 92%); backdrop-filter: blur(16px); }
.header-inner { display: flex; width: min(1680px, calc(100% - 40px)); height: 72px; align-items: center; gap: 28px; }
.header-inner > a:first-child { flex: 0 0 auto; }
.desktop-nav { display: flex; overflow-x: auto; min-width: 0; flex: 1; align-items: center; gap: 4px; scrollbar-width: none; }
.desktop-nav::-webkit-scrollbar { display: none; }
.nav-link { flex: 0 0 auto; border-radius: 9px; color: var(--lp-text-secondary); font-size: 15px; font-weight: 600; padding: 9px 12px; white-space: nowrap; transition: 160ms ease; }
.nav-link:hover, .nav-link.router-link-exact-active { color: var(--lp-primary); background: #eff6ff; }
.header-actions { display: flex; flex: 0 0 auto; align-items: center; white-space: nowrap; }
.user-trigger { display: flex; border: 0; align-items: center; gap: 9px; color: var(--lp-text); background: transparent; cursor: pointer; font-weight: 600; padding: 4px; white-space: nowrap; }
.app-main { flex: 1; }
.app-footer { border-top: 1px solid var(--lp-border); background: var(--lp-surface); }
.footer-inner { display: flex; min-height: 72px; align-items: center; justify-content: space-between; color: var(--lp-text-secondary); font-size: 13px; }
@media (max-width: 840px) {
  .desktop-nav, .header-actions .el-button:first-child { display: none; }
  .header-inner { justify-content: space-between; }
}
</style>
