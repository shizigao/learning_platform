<script setup lang="ts">
import { Connection, Key, Lock, Refresh } from '@element-plus/icons-vue'
import { ElMessage } from 'element-plus'
import { computed, onMounted, ref } from 'vue'

import { getAdminAiConfig } from '@/api/ai'
import SectionPageHeader from '@/components/SectionPageHeader.vue'
import type { AdminAiConfig } from '@/types/ai'

const loading = ref(true)
const config = ref<AdminAiConfig>()

const providerLabel = computed(() =>
  config.value?.mockMode ? 'Mock 模拟 AI' : config.value?.provider || '—',
)

async function load(): Promise<void> {
  loading.value = true
  try {
    config.value = await getAdminAiConfig()
  } catch (error) {
    ElMessage.error(error instanceof Error ? error.message : 'AI 配置加载失败')
  } finally {
    loading.value = false
  }
}

const limitRows = computed(() => {
  if (!config.value) return []
  const limits = config.value.limits
  return [
    { name: '资料输入字符', value: limits.maxInputChars, variable: 'AI_MAX_INPUT_CHARS' },
    { name: '上下文消息数', value: limits.maxContextMessages, variable: 'AI_MAX_CONTEXT_MESSAGES' },
    { name: '上下文字符数', value: limits.maxContextChars, variable: 'AI_MAX_CONTEXT_CHARS' },
    { name: '窗口调用次数', value: limits.requestsPerWindow, variable: 'AI_REQUESTS_PER_WINDOW' },
    { name: '限流窗口', value: `${limits.rateWindowSeconds} 秒`, variable: 'AI_RATE_WINDOW_SECONDS' },
    { name: '单用户并发', value: limits.maxConcurrentPerUser, variable: 'AI_MAX_CONCURRENT_PER_USER' },
    { name: '统一请求超时', value: `${limits.requestTimeoutSeconds} 秒`, variable: 'AI_REQUEST_TIMEOUT_SECONDS' },
    { name: '供应商连接超时', value: `${limits.providerConnectTimeoutSeconds} 秒`, variable: 'DEEPSEEK_CONNECT_TIMEOUT_SECONDS' },
    { name: '供应商响应超时', value: `${limits.providerTimeoutSeconds} 秒`, variable: 'DEEPSEEK_TIMEOUT_SECONDS' },
  ]
})

onMounted(load)
</script>

<template>
  <section v-loading="loading" class="admin-ai-page">
    <div class="page-container">
      <SectionPageHeader
        eyebrow="ADMIN · AI CONFIGURATION"
        title="AI 运行配置"
        description="查看后端当前生效的供应商、模型与安全限制；密钥始终只保存在后端环境变量中"
      >
        <RouterLink to="/admin"><el-button>返回管理后台</el-button></RouterLink>
        <el-button type="primary" :icon="Refresh" @click="load">刷新配置</el-button>
      </SectionPageHeader>

      <template v-if="config">
        <div class="status-grid">
          <article>
            <el-icon><Connection /></el-icon>
            <div><span>当前供应商</span><strong>{{ providerLabel }}</strong><small>{{ config.model }}</small></div>
            <el-tag :type="config.mockMode ? 'warning' : 'success'">
              {{ config.mockMode ? '模拟模式' : '真实模式' }}
            </el-tag>
          </article>
          <article>
            <el-icon><Key /></el-icon>
            <div><span>真实 API Key</span><strong>{{ config.apiKeyConfigured ? '已配置' : '未配置' }}</strong><small>页面永不返回密钥内容</small></div>
            <el-tag :type="config.apiKeyConfigured ? 'success' : 'info'">
              {{ config.apiKeyConfigured ? '安全就绪' : '保持 Mock' }}
            </el-tag>
          </article>
          <article>
            <el-icon><Lock /></el-icon>
            <div>
              <span>{{ config.mockMode ? 'Mock 场景' : '模型思考模式' }}</span>
              <strong>
                {{ config.mockMode ? (config.mockScenario || 'success') : (config.thinkingEnabled ? '已启用' : '已关闭') }}
              </strong>
              <small>{{ config.mockMode ? 'success / failure / timeout' : '简单学习任务默认关闭以降低延迟' }}</small>
            </div>
            <el-tag effect="plain">环境变量控制</el-tag>
          </article>
        </div>

        <div class="config-grid">
          <section class="config-card">
            <header><div><h2>调用限制</h2><p>以下均为后端当前实际生效值</p></div></header>
            <el-table :data="limitRows">
              <el-table-column prop="name" label="配置项" min-width="145" />
              <el-table-column prop="value" label="当前值" min-width="115" />
              <el-table-column prop="variable" label="环境变量" min-width="220">
                <template #default="{ row }"><code>{{ row.variable }}</code></template>
              </el-table-column>
            </el-table>
          </section>

          <aside class="config-card guidance">
            <h2>安全配置说明</h2>
            <el-alert
              type="success"
              :closable="false"
              show-icon
              title="前端不会接触供应商密钥"
            />
            <ol>
              <li>本地开发默认保持 <code>AI_PROVIDER=mock</code>。</li>
              <li>真实联调时仅在 IDEA 运行配置或操作系统环境变量中填写 <code>DEEPSEEK_API_KEY</code>。</li>
              <li>修改环境变量后重启后端，再点击“刷新配置”确认生效。</li>
              <li>学习总结和讲解建议保持 <code>DEEPSEEK_THINKING_ENABLED=false</code>，减少等待时间。</li>
              <li>切勿将真实密钥写入仓库、截图、浏览器或聊天消息。</li>
            </ol>
            <div class="safe-fields">
              <span>供应商 Base URL</span>
              <code>{{ config.baseUrl || '未配置' }}</code>
            </div>
            <p class="notice">此页面是安全的只读运行面板。环境变量是唯一配置源，避免数据库或前端产生第二份密钥。</p>
          </aside>
        </div>
      </template>
    </div>
  </section>
</template>

<style scoped>
.admin-ai-page { min-height: calc(100vh - 145px); padding: 48px 0 76px; background: #f6f8fc; }
.status-grid { display: grid; gap: 16px; margin-bottom: 18px; grid-template-columns: repeat(3, 1fr); }
.status-grid article { display: grid; align-items: center; gap: 14px; border: 1px solid var(--lp-border); border-radius: 17px; background: #fff; box-shadow: var(--lp-shadow); padding: 20px; grid-template-columns: auto 1fr auto; }.status-grid .el-icon { display: grid; width: 42px; height: 42px; border-radius: 12px; place-items: center; color: var(--lp-primary); background: #eff6ff; font-size: 21px; }.status-grid article div { display: flex; flex-direction: column; gap: 4px; }.status-grid span, .status-grid small { color: #98a2b3; font-size: 12px; }.status-grid strong { font-size: 17px; }
.config-grid { display: grid; align-items: start; gap: 18px; grid-template-columns: minmax(0, 1.5fr) minmax(330px, .7fr); }.config-card { overflow: hidden; border: 1px solid var(--lp-border); border-radius: 18px; background: #fff; box-shadow: var(--lp-shadow); }.config-card > header { border-bottom: 1px solid var(--lp-border); padding: 22px 24px; }.config-card h2 { margin: 0; font-size: 19px; }.config-card header p { margin: 6px 0 0; color: var(--lp-text-secondary); font-size: 13px; }.config-card code { border-radius: 5px; color: #1d4ed8; background: #eff6ff; padding: 2px 6px; }
.guidance { padding: 24px; }.guidance .el-alert { margin: 18px 0; }.guidance ol { color: var(--lp-text-secondary); line-height: 1.8; padding-left: 22px; }.guidance li { margin-bottom: 10px; }.safe-fields { display: flex; flex-direction: column; gap: 8px; border-radius: 12px; margin-top: 20px; background: #f8fafc; padding: 14px; }.safe-fields span { color: #667085; font-size: 12px; }.safe-fields code { overflow-wrap: anywhere; }.notice { margin: 20px 0 0; color: #98a2b3; font-size: 12px; line-height: 1.7; }
@media (max-width: 1050px) { .status-grid { grid-template-columns: 1fr; }.config-grid { grid-template-columns: 1fr; } }
</style>
