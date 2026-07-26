<script setup lang="ts">
import { Search } from '@element-plus/icons-vue'
import { ElMessage } from 'element-plus'
import { ref } from 'vue'
import { useRouter } from 'vue-router'

import { searchPublicUsers } from '@/api/user'
import type { PublicUserSummary } from '@/types/user'

const router = useRouter()
const keyword = ref('')
const loading = ref(false)
const searched = ref(false)
const users = ref<PublicUserSummary[]>([])

async function search(): Promise<void> {
  loading.value = true
  searched.value = true
  try {
    users.value = (await searchPublicUsers(keyword.value, 1, 20)).items
  } catch (error) {
    ElMessage.error(error instanceof Error ? error.message : '用户搜索失败')
  } finally {
    loading.value = false
  }
}

function openUser(userId: number): void {
  void router.push(`/users/${userId}`)
}
</script>

<template>
  <section class="user-search-panel">
    <div class="panel-heading">
      <div>
        <h2>搜索用户</h2>
        <p>根据用户名查找平台用户并访问其个人中心</p>
      </div>
      <div class="search-bar">
        <el-input
          v-model="keyword"
          clearable
          maxlength="64"
          placeholder="输入用户名"
          @keyup.enter="search"
        />
        <el-button type="primary" :icon="Search" :loading="loading" @click="search">搜索</el-button>
      </div>
    </div>
    <div v-if="users.length" class="user-results">
      <button
        v-for="user in users"
        :key="user.id"
        type="button"
        class="user-result"
        @click="openUser(user.id)"
      >
        <el-avatar :size="48" :src="user.avatarUrl">
          {{ (user.nickname || user.username).slice(0, 1).toUpperCase() }}
        </el-avatar>
        <span><strong>{{ user.nickname }}</strong><small>@{{ user.username }}</small></span>
      </button>
    </div>
    <el-empty
      v-else-if="searched && !loading"
      :image-size="62"
      description="没有找到符合条件的用户"
    />
  </section>
</template>

<style scoped>
.user-search-panel { border: 1px solid var(--lp-border); border-radius: 20px; margin-top: 24px; background: #fff; box-shadow: var(--lp-shadow); padding: 26px; }
.panel-heading { display: flex; align-items: flex-end; justify-content: space-between; gap: 24px; }
.panel-heading h2 { margin: 0; font-size: 20px; }.panel-heading p { margin: 7px 0 0; color: var(--lp-text-secondary); font-size: 13px; }
.search-bar { display: grid; width: min(480px, 100%); gap: 10px; grid-template-columns: 1fr auto; }
.user-results { display: grid; gap: 12px; margin-top: 22px; grid-template-columns: repeat(auto-fill, minmax(220px, 1fr)); }
.user-result { display: flex; align-items: center; gap: 12px; border: 1px solid var(--lp-border); border-radius: 14px; color: inherit; background: #fff; cursor: pointer; padding: 14px; text-align: left; }
.user-result:hover { border-color: #b7d0ff; background: #f5f8ff; }.user-result span, .user-result small { display: block; }.user-result small { margin-top: 4px; color: var(--lp-text-secondary); }
@media (max-width: 720px) { .panel-heading { align-items: stretch; flex-direction: column; }.search-bar { width: 100%; } }
</style>
