<script setup lang="ts">
import {
  ChatDotRound,
  MagicStick,
  Plus,
  Refresh,
  ShoppingBag,
} from '@element-plus/icons-vue'
import DOMPurify from 'dompurify'
import { ElMessage } from 'element-plus'
import { marked } from 'marked'
import { computed, nextTick, onBeforeUnmount, onMounted, ref } from 'vue'
import type { Ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'

import {
  createAiRequestId,
  createConversation,
  explain,
  explainWithTemplate,
  generateSummary,
  getConversation,
  getLatestSummary,
  listAiUsageRecords,
  listConversations,
} from '@/api/ai'
import { listContents } from '@/api/content'
import { ApiError } from '@/api/http'
import { getEntitlementBalances } from '@/api/order'
import SectionPageHeader from '@/components/SectionPageHeader.vue'
import type {
  AiConversation,
  AiConversationTemplate,
  AiSummary,
  AiUsageRecord,
} from '@/types/ai'
import type { ContentSummary } from '@/types/content'
import type { EntitlementBalances } from '@/types/order'

const route = useRoute()
const router = useRouter()
const loading = ref(true)
const switching = ref(false)
const summaryLoading = ref(false)
const chatLoading = ref(false)
const sending = ref(false)
const contents = ref<ContentSummary[]>([])
const selectedContentId = ref<number>()
const summary = ref<AiSummary>()
const conversations = ref<AiConversation[]>([])
const activeConversation = ref<AiConversation>()
const question = ref('')
const usageRecords = ref<AiUsageRecord[]>([])
const balances = ref<EntitlementBalances>({
  aiQuota: 0,
  examQuota: 0,
  examOverallAiQuota: 0,
  examPersonalAiQuota: 0,
})
const messagesElement = ref<HTMLElement>()

type OperationPhase = 'idle' | 'submitting' | 'generating' | 'success' | 'error'
interface OperationState {
  phase: OperationPhase
  title: string
  detail: string
  startedAt?: number
}

const idleOperation = (): OperationState => ({
  phase: 'idle',
  title: '',
  detail: '',
})
const summaryStatus = ref<OperationState>(idleOperation())
const chatStatus = ref<OperationState>(idleOperation())
const clock = ref(Date.now())
let clockTimer: number | undefined
let summaryStatusTimer: number | undefined
let chatStatusTimer: number | undefined

marked.setOptions({
  gfm: true,
  breaks: true,
})

const selectedContent = computed(() =>
  contents.value.find((item) => item.id === selectedContentId.value),
)
const hasQuota = computed(() => balances.value.aiQuota > 0)

function clearStatusTimer(target: Ref<OperationState>): void {
  if (target === summaryStatus && summaryStatusTimer !== undefined) {
    window.clearTimeout(summaryStatusTimer)
    summaryStatusTimer = undefined
  }
  if (target === chatStatus && chatStatusTimer !== undefined) {
    window.clearTimeout(chatStatusTimer)
    chatStatusTimer = undefined
  }
}

function setOperationStatus(
  target: Ref<OperationState>,
  status: OperationState,
  autoDismiss = status.phase === 'success' || status.phase === 'error',
): void {
  clearStatusTimer(target)
  target.value = status
  if (!autoDismiss) return
  const timer = window.setTimeout(() => {
    target.value = idleOperation()
    clearStatusTimer(target)
  }, 2_000)
  if (target === summaryStatus) summaryStatusTimer = timer
  if (target === chatStatus) chatStatusTimer = timer
}

function beginOperation(target: Ref<OperationState>, action: string): () => void {
  const startedAt = Date.now()
  setOperationStatus(target, {
    phase: 'submitting',
    title: `${action}请求已提交`,
    detail: '后端正在校验资料访问权限与 AI 额度。',
    startedAt,
  }, false)
  const generatingTimer = window.setTimeout(() => {
    if (target.value.startedAt !== startedAt || target.value.phase === 'error') return
    setOperationStatus(target, {
      phase: 'generating',
      title: `AI 正在${action}`,
      detail: '真实模型正在生成内容，请勿重复点击或切换资料。',
      startedAt,
    }, false)
  }, 700)
  const waitingTimer = window.setTimeout(() => {
    if (target.value.startedAt !== startedAt || target.value.phase !== 'generating') return
    setOperationStatus(target, {
      ...target.value,
      title: `AI 仍在${action}`,
      detail: '较长资料可能需要更多时间，页面会继续等待明确结果。',
    }, false)
  }, 20_000)
  return () => {
    window.clearTimeout(generatingTimer)
    window.clearTimeout(waitingTimer)
  }
}

function failedOperation(action: string, error: unknown): OperationState {
  const message = error instanceof Error ? error.message : `${action}失败`
  let title = `${action}未完成`
  if (message.includes('超时')) {
    title = `${action}超时`
  } else if (error instanceof ApiError && error.status === 429) {
    title = `${action}未开始：当前请求过多`
  } else if (error instanceof ApiError && error.status === 403) {
    title = `${action}未开始：权限或额度不足`
  } else if (error instanceof ApiError && !error.status) {
    title = `${action}未开始：未连接到后端`
  }
  return { phase: 'error', title, detail: message }
}

function operationDetail(status: OperationState): string {
  if (
    (status.phase === 'submitting' || status.phase === 'generating') &&
    status.startedAt
  ) {
    const seconds = Math.max(0, Math.floor((clock.value - status.startedAt) / 1000))
    return `${status.detail} 已等待 ${seconds} 秒。`
  }
  return status.detail
}

async function scrollMessagesToBottom(): Promise<void> {
  await nextTick()
  if (messagesElement.value) {
    messagesElement.value.scrollTop = messagesElement.value.scrollHeight
  }
}

function renderMarkdown(content: string): string {
  const html = marked.parse(content, { async: false }) as string
  return DOMPurify.sanitize(html, {
    ALLOWED_TAGS: [
      'p', 'br', 'strong', 'em', 'del', 'blockquote', 'pre', 'code',
      'ul', 'ol', 'li', 'h1', 'h2', 'h3', 'h4', 'h5', 'h6',
      'a', 'hr', 'table', 'thead', 'tbody', 'tr', 'th', 'td',
    ],
    ALLOWED_ATTR: ['href', 'title', 'class'],
    ALLOW_DATA_ATTR: false,
    ALLOWED_URI_REGEXP: /^(?:(?:https?|mailto):|[#/])/i,
  })
}

async function loadBalanceAndUsage(): Promise<void> {
  ;[balances.value, usageRecords.value] = await Promise.all([
    getEntitlementBalances(),
    listAiUsageRecords(),
  ])
}

async function loadInitial(): Promise<void> {
  loading.value = true
  try {
    const page = await listContents({ pageNumber: 1, pageSize: 100 })
    contents.value = page.items
    await loadBalanceAndUsage()
    const requestedId = Number(route.query.contentId)
    selectedContentId.value =
      Number.isFinite(requestedId) && contents.value.some((item) => item.id === requestedId)
        ? requestedId
        : contents.value[0]?.id
    if (selectedContentId.value) await loadContentAiData()
  } catch (error) {
    ElMessage.error(error instanceof Error ? error.message : 'AI 学习助手加载失败')
  } finally {
    loading.value = false
  }
}

async function loadContentAiData(): Promise<void> {
  if (!selectedContentId.value) return
  switching.value = true
  summary.value = undefined
  conversations.value = []
  activeConversation.value = undefined
  setOperationStatus(summaryStatus, idleOperation(), false)
  setOperationStatus(chatStatus, idleOperation(), false)
  try {
    try {
      summary.value = await getLatestSummary(selectedContentId.value)
    } catch (error) {
      if (!(error instanceof ApiError) || error.status !== 404) throw error
    }
    conversations.value = await listConversations(selectedContentId.value)
    if (conversations.value[0]) {
      activeConversation.value = await getConversation(conversations.value[0].id)
      await scrollMessagesToBottom()
    }
    await router.replace({
      query: { ...route.query, contentId: String(selectedContentId.value) },
    })
  } catch (error) {
    ElMessage.error(error instanceof Error ? error.message : '资料 AI 记录加载失败')
  } finally {
    switching.value = false
  }
}

// 生成总结函数
async function runSummary(): Promise<void> {
  if (!selectedContentId.value) return
  if (!hasQuota.value) {
    setOperationStatus(summaryStatus, {
      phase: 'error',
      title: '总结未开始：AI 额度不足',
      detail: '请先购买 AI 次数包，成功生成内容后才会扣除额度。',
    })
    return
  }
  summaryLoading.value = true
  const stopProgress = beginOperation(summaryStatus, '生成总结')
  try {
    // 等待后端返回总结内容,点击generateSummary
    summary.value = await generateSummary(
      selectedContentId.value,
      createAiRequestId('summary'),
    )
    await loadBalanceAndUsage()
    setOperationStatus(summaryStatus, {
      phase: 'success',
      title: 'AI 总结已生成并保存',
      detail: '本次成功调用已扣除 1 次 AI 额度。',
    })
  } catch (error) {
    setOperationStatus(summaryStatus, failedOperation('生成总结', error))
    await loadBalanceAndUsage()
  } finally {
    stopProgress()
    summaryLoading.value = false
  }
}

async function newConversation(): Promise<AiConversation | undefined> {
  if (!selectedContentId.value) return undefined
  chatLoading.value = true
  try {
    const created = await createConversation(selectedContentId.value)
    conversations.value = [created, ...conversations.value]
    activeConversation.value = created
    return created
  } catch (error) {
    ElMessage.error(error instanceof Error ? error.message : 'AI 会话创建失败')
    return undefined
  } finally {
    chatLoading.value = false
  }
}

async function chooseConversation(conversationId: number): Promise<void> {
  chatLoading.value = true
  try {
    activeConversation.value = await getConversation(conversationId)
    await scrollMessagesToBottom()
  } catch (error) {
    ElMessage.error(error instanceof Error ? error.message : '会话加载失败')
  } finally {
    chatLoading.value = false
  }
}

async function sendQuestion(): Promise<void> {
  const text = question.value.trim()
  if (!text || sending.value) return
  if (!hasQuota.value) {
    setOperationStatus(chatStatus, {
      phase: 'error',
      title: '讲解未开始：AI 额度不足',
      detail: '请先购买 AI 次数包，成功生成回答后才会扣除额度。',
    })
    return
  }
  let conversation = activeConversation.value
  if (!conversation) conversation = await newConversation()
  if (!conversation) return
  sending.value = true
  const stopProgress = beginOperation(chatStatus, '生成讲解')
  try {
    await explain(conversation.id, text, createAiRequestId('explain'))
    question.value = ''
    activeConversation.value = await getConversation(conversation.id)
    conversations.value = await listConversations(conversation.contentId)
    await loadBalanceAndUsage()
    setOperationStatus(chatStatus, {
      phase: 'success',
      title: 'AI 讲解已生成并保存',
      detail: '本次成功调用已扣除 1 次 AI 额度，可以继续追问。',
    })
    await scrollMessagesToBottom()
  } catch (error) {
    setOperationStatus(chatStatus, failedOperation('生成讲解', error))
    await loadBalanceAndUsage()
  } finally {
    stopProgress()
    sending.value = false
  }
}

async function sendTemplate(
  template: AiConversationTemplate,
  label: string,
): Promise<void> {
  if (sending.value) return
  if (!hasQuota.value) {
    setOperationStatus(chatStatus, {
      phase: 'error',
      title: `${label}未开始：AI 额度不足`,
      detail: '请先购买 AI 次数包，成功生成回答后才会扣除额度。',
    })
    return
  }
  let conversation = activeConversation.value
  if (!conversation) conversation = await newConversation()
  if (!conversation) return
  sending.value = true
  const stopProgress = beginOperation(chatStatus, label)
  try {
    await explainWithTemplate(
      conversation.id,
      template,
      createAiRequestId(template === 'QUIZ_REINFORCEMENT' ? 'quiz' : 'diverge'),
    )
    activeConversation.value = await getConversation(conversation.id)
    conversations.value = await listConversations(conversation.contentId)
    await loadBalanceAndUsage()
    setOperationStatus(chatStatus, {
      phase: 'success',
      title: `${label}内容已生成并保存`,
      detail: '本次成功调用已扣除 1 次 AI 额度，可以继续追问。',
    })
    await scrollMessagesToBottom()
  } catch (error) {
    setOperationStatus(chatStatus, failedOperation(label, error))
    await loadBalanceAndUsage()
  } finally {
    stopProgress()
    sending.value = false
  }
}

function dateTime(value?: string): string {
  return value ? new Date(value).toLocaleString() : '—'
}

onMounted(() => {
  clockTimer = window.setInterval(() => {
    clock.value = Date.now()
  }, 1_000)
  void loadInitial()
})

onBeforeUnmount(() => {
  if (clockTimer !== undefined) window.clearInterval(clockTimer)
  clearStatusTimer(summaryStatus)
  clearStatusTimer(chatStatus)
})
</script>

<template>
  <section v-loading="loading" class="ai-page">
    <div class="page-container">
      <SectionPageHeader
        eyebrow="AI LEARNING ASSISTANT"
        title="AI 学习助手"
        description="针对当前资料生成摘要、知识点和复习提纲，并在资料范围内继续追问"
      >
        <div class="quota-card" :class="{ empty: !hasQuota }">
          <span>AI 可用次数</span>
          <strong>{{ balances.aiQuota }}</strong>
          <RouterLink to="/commerce?type=AI_PACKAGE">
            <el-button :icon="ShoppingBag" size="small">购买次数</el-button>
          </RouterLink>
        </div>
      </SectionPageHeader>

      <div class="content-picker">
        <div>
          <strong>当前学习资料</strong>
          <small>当前版本仅处理标题、简介、正文以及 TXT/Markdown 文本</small>
        </div>
        <el-select
          v-model="selectedContentId"
          filterable
          placeholder="请选择资料"
          @change="loadContentAiData"
        >
          <el-option
            v-for="item in contents"
            :key="item.id"
            :label="item.title"
            :value="item.id"
          >
            <span>{{ item.title }}</span>
          </el-option>
        </el-select>
      </div>

      <el-alert
        v-if="!hasQuota"
        type="warning"
        :closable="false"
        show-icon
        title="当前没有可用 AI 次数"
        description="你仍可查看已有总结和会话；生成新内容前请到权益商城购买并完成模拟支付。"
      />

      <div v-if="selectedContent" v-loading="switching" class="assistant-grid">
        <aside class="conversation-panel">
          <header>
            <div><strong>讲解会话</strong><small>{{ selectedContent.title }}</small></div>
            <el-button
              circle
              :icon="Plus"
              :disabled="chatLoading || sending"
              aria-label="新建会话"
              @click="newConversation"
            />
          </header>
          <div class="conversation-list">
            <button
              v-for="item in conversations"
              :key="item.id"
              type="button"
              class="conversation-item"
              :class="{ active: item.id === activeConversation?.id }"
              :disabled="sending"
              @click="chooseConversation(item.id)"
            >
              <span>{{ item.title }}</span>
              <small>{{ dateTime(item.lastMessageAt || item.createdAt) }}</small>
            </button>
            <el-empty
              v-if="conversations.length === 0"
              description="还没有讲解会话"
              :image-size="62"
            />
          </div>
        </aside>

        <main v-loading="chatLoading" class="chat-panel">
          <header>
            <div>
              <el-icon><ChatDotRound /></el-icon>
              <strong>{{ activeConversation?.title || '新建资料讲解' }}</strong>
            </div>
            <el-tag type="success" effect="plain">仅依据当前资料回答</el-tag>
          </header>
          <div
            v-if="chatStatus.phase !== 'idle'"
            class="operation-status"
            :class="chatStatus.phase"
            aria-live="polite"
          >
            <span class="status-dot" />
            <div>
              <strong>{{ chatStatus.title }}</strong>
              <small>{{ operationDetail(chatStatus) }}</small>
            </div>
          </div>
          <div ref="messagesElement" class="messages">
            <div
              v-for="message in activeConversation?.messages ?? []"
              :key="message.id"
              class="message"
              :class="message.role.toLowerCase()"
            >
              <small>{{ message.role === 'USER' ? '我' : 'AI 助手' }}</small>
              <p v-if="message.role === 'USER'" class="message-content">
                {{ message.content }}
              </p>
              <div
                v-else
                class="message-content markdown-body"
                v-html="renderMarkdown(message.content)"
              />
            </div>
            <div v-if="!activeConversation?.messages.length && !sending" class="chat-empty">
              <el-icon><MagicStick /></el-icon>
              <strong>从一个具体问题开始</strong>
              <p>例如：“请用简单语言解释这份资料的核心概念。”</p>
            </div>
            <div v-if="sending" class="message assistant pending-message">
              <small>AI 助手</small>
              <p class="message-content"><span class="typing-dot" /><span class="typing-dot" /><span class="typing-dot" /> 正在生成回答</p>
            </div>
          </div>
          <div class="question-box">
            <div class="template-actions">
              <span>快捷学习模板</span>
              <el-button
                plain
                type="primary"
                :disabled="!hasQuota || sending"
                @click="sendTemplate('QUIZ_REINFORCEMENT', '出题巩固')"
              >
                出题巩固
              </el-button>
              <el-button
                plain
                type="success"
                :disabled="!hasQuota || sending"
                @click="sendTemplate('DIVERGENT_THINKING', '发散思维')"
              >
                发散思维
              </el-button>
            </div>
            <el-input
              v-model="question"
              type="textarea"
              :rows="3"
              maxlength="4000"
              show-word-limit
              placeholder="输入你希望 AI 根据当前资料讲解的问题"
              @keydown.ctrl.enter.prevent="sendQuestion"
            />
            <div>
              <small>Ctrl + Enter 发送 · 成功回答后扣除 1 次</small>
              <el-button
                type="primary"
                :loading="sending"
                :disabled="!question.trim() || !hasQuota || sending"
                @click="sendQuestion"
              >
                {{ sending ? 'AI 正在生成' : '发送问题' }}
              </el-button>
            </div>
          </div>
        </main>

        <aside class="summary-panel">
          <header>
            <div><strong>资料总结</strong><small>摘要 · 知识点 · 复习提纲</small></div>
            <el-button
              type="primary"
              :icon="summary ? Refresh : MagicStick"
              :loading="summaryLoading"
              :disabled="!hasQuota || summaryLoading || sending"
              @click="runSummary"
            >
              {{ summaryLoading ? 'AI 正在生成' : (summary ? '重新生成' : '生成总结') }}
            </el-button>
          </header>
          <div
            v-if="summaryStatus.phase !== 'idle'"
            class="operation-status"
            :class="summaryStatus.phase"
            aria-live="polite"
          >
            <span class="status-dot" />
            <div>
              <strong>{{ summaryStatus.title }}</strong>
              <small>{{ operationDetail(summaryStatus) }}</small>
            </div>
          </div>
          <div class="summary-scroll">
            <template v-if="summary">
              <section>
                <h3>内容摘要</h3>
                <p>{{ summary.summary }}</p>
              </section>
              <section>
                <h3>核心知识点</h3>
                <ol>
                  <li v-for="point in summary.knowledgePoints" :key="point">{{ point }}</li>
                </ol>
              </section>
              <section>
                <h3>复习提纲</h3>
                <p class="outline">{{ summary.reviewOutline }}</p>
              </section>
              <small>生成于 {{ dateTime(summary.createdAt) }}</small>
            </template>
            <el-empty v-else description="尚未生成该资料的 AI 总结" :image-size="78" />
          </div>
        </aside>
      </div>

      <el-empty
        v-else-if="!loading"
        description="暂无可选择的已发布资料"
      />

      <section class="usage-card">
        <header><div><strong>最近 AI 使用记录</strong><small>成功保存后才会产生扣次记录</small></div></header>
        <el-table :data="usageRecords.slice(0, 8)" empty-text="暂无使用记录">
          <el-table-column prop="businessNo" label="业务编号" min-width="190" />
          <el-table-column prop="usageType" label="类型" width="120" />
          <el-table-column prop="quantity" label="扣次" width="80" />
          <el-table-column label="余额变化" width="130">
            <template #default="{ row }">{{ row.balanceBefore }} → {{ row.balanceAfter }}</template>
          </el-table-column>
          <el-table-column label="时间" min-width="170">
            <template #default="{ row }">{{ dateTime(row.createdAt) }}</template>
          </el-table-column>
        </el-table>
      </section>
    </div>
  </section>
</template>

<style scoped>
.ai-page { min-height: calc(100vh - 145px); padding: 42px 0 76px; background: #f4f7fc; }
.quota-card { display: grid; min-width: 210px; align-items: center; gap: 4px 12px; border: 1px solid #bfdbfe; border-radius: 15px; background: #eff6ff; padding: 12px 14px; grid-template-columns: 1fr auto; }
.quota-card span { color: var(--lp-text-secondary); font-size: 12px; }.quota-card strong { color: var(--lp-primary); font-size: 28px; grid-row: span 2; }.quota-card.empty { border-color: #fed7aa; background: #fff7ed; }.quota-card.empty strong { color: #ea580c; }
.content-picker, .conversation-panel, .chat-panel, .summary-panel, .usage-card { border: 1px solid var(--lp-border); background: #fff; box-shadow: var(--lp-shadow); }
.content-picker { display: flex; align-items: center; justify-content: space-between; gap: 20px; border-radius: 16px; margin-bottom: 16px; padding: 18px 20px; }.content-picker div { display: flex; flex-direction: column; gap: 5px; }.content-picker small, header small { color: #98a2b3; font-size: 12px; }.content-picker .el-select { width: min(520px, 55vw); }.option-type { float: right; margin-left: 30px; color: #98a2b3; }
.ai-page :deep(.el-alert) { margin-bottom: 16px; }
.assistant-grid { display: grid; height: clamp(620px, 70vh, 760px); min-height: 0; gap: 16px; grid-template-columns: 235px minmax(360px, 1fr) 360px; }
.conversation-panel, .chat-panel, .summary-panel { overflow: hidden; height: 100%; min-height: 0; border-radius: 18px; }
.conversation-panel header, .chat-panel header, .summary-panel header, .usage-card header { display: flex; align-items: center; justify-content: space-between; gap: 12px; border-bottom: 1px solid var(--lp-border); padding: 18px; }.conversation-panel header > div, .summary-panel header > div, .usage-card header > div { display: flex; flex-direction: column; gap: 5px; }
.conversation-panel, .summary-panel { display: flex; flex-direction: column; }
.conversation-list { overflow-y: auto; min-height: 0; flex: 1; scrollbar-gutter: stable; }
.conversation-item { display: flex; width: calc(100% - 20px); border: 0; border-radius: 11px; flex-direction: column; gap: 6px; margin: 10px; color: var(--lp-text); background: transparent; cursor: pointer; padding: 13px; text-align: left; }.conversation-item:hover, .conversation-item.active { color: var(--lp-primary); background: #eff6ff; }.conversation-item span { overflow: hidden; font-weight: 650; text-overflow: ellipsis; white-space: nowrap; }.conversation-item small { color: #98a2b3; }
.chat-panel { display: flex; flex-direction: column; }.chat-panel header > div { display: flex; align-items: center; gap: 8px; }.messages { display: flex; overflow-y: auto; min-height: 0; flex: 1; flex-direction: column; gap: 16px; padding: 22px; scrollbar-gutter: stable; }.message { max-width: 85%; }.message small { display: block; margin-bottom: 5px; color: #98a2b3; }.message > .message-content { overflow-wrap: anywhere; border-radius: 15px 15px 15px 4px; margin: 0; line-height: 1.75; background: #f2f4f7; padding: 13px 15px; }.message.user { align-self: flex-end; }.message.user small { text-align: right; }.message.user > .message-content { border-radius: 15px 15px 4px; color: #fff; background: var(--lp-primary); white-space: pre-wrap; }.chat-empty { display: flex; align-items: center; justify-content: center; flex: 1; flex-direction: column; color: var(--lp-text-secondary); text-align: center; }.chat-empty .el-icon { color: var(--lp-primary); font-size: 36px; }.chat-empty strong { margin-top: 12px; color: var(--lp-text); }.chat-empty p { font-size: 13px; }
.markdown-body :deep(:first-child) { margin-top: 0; }.markdown-body :deep(:last-child) { margin-bottom: 0; }.markdown-body :deep(p) { margin: .65em 0; }.markdown-body :deep(h1), .markdown-body :deep(h2), .markdown-body :deep(h3), .markdown-body :deep(h4) { margin: 1em 0 .5em; color: var(--lp-text); line-height: 1.4; }.markdown-body :deep(h1) { font-size: 1.35em; }.markdown-body :deep(h2) { font-size: 1.22em; }.markdown-body :deep(h3) { font-size: 1.12em; }.markdown-body :deep(ul), .markdown-body :deep(ol) { margin: .65em 0; padding-left: 1.7em; }.markdown-body :deep(li + li) { margin-top: .25em; }.markdown-body :deep(blockquote) { border-left: 4px solid #93c5fd; margin: .8em 0; color: #475467; background: #e8eef7; padding: .45em .8em; }.markdown-body :deep(code) { border-radius: 5px; color: #b42318; background: #e5e7eb; padding: .12em .35em; font-family: Consolas, "Courier New", monospace; font-size: .9em; }.markdown-body :deep(pre) { overflow-x: auto; border-radius: 9px; margin: .8em 0; color: #e2e8f0; background: #0f172a; padding: 12px 14px; white-space: pre; }.markdown-body :deep(pre code) { color: inherit; background: transparent; padding: 0; }.markdown-body :deep(table) { display: block; overflow-x: auto; width: 100%; border-collapse: collapse; margin: .8em 0; }.markdown-body :deep(th), .markdown-body :deep(td) { border: 1px solid #cbd5e1; padding: 6px 9px; text-align: left; }.markdown-body :deep(th) { background: #e8eef7; }.markdown-body :deep(a) { color: var(--lp-primary); text-decoration: underline; }.markdown-body :deep(hr) { border: 0; border-top: 1px solid #cbd5e1; margin: 1em 0; }
.pending-message { opacity: .82; }.typing-dot { display: inline-block; width: 5px; height: 5px; border-radius: 50%; margin-right: 3px; background: var(--lp-primary); animation: typing 1.1s infinite ease-in-out; }.typing-dot:nth-child(2) { animation-delay: .15s; }.typing-dot:nth-child(3) { animation-delay: .3s; }
.question-box { border-top: 1px solid var(--lp-border); padding: 16px; }.question-box > div { display: flex; align-items: center; justify-content: space-between; gap: 12px; margin-top: 10px; }.question-box small { color: #98a2b3; }
.question-box > .template-actions { justify-content: flex-start; margin: 0 0 12px; }.template-actions > span { margin-right: auto; color: var(--lp-text-secondary); font-size: 13px; font-weight: 700; }
.summary-scroll { overflow-y: auto; min-height: 0; flex: 1; padding-bottom: 20px; scrollbar-gutter: stable; }.summary-panel section { margin: 20px; }.summary-panel h3 { margin: 0 0 9px; font-size: 15px; }.summary-panel p, .summary-panel li { color: var(--lp-text-secondary); font-size: 14px; line-height: 1.8; }.summary-panel ol { margin: 0; padding-left: 22px; }.summary-panel .outline { white-space: pre-wrap; }.summary-scroll > small { display: block; margin: 20px; color: #98a2b3; }
.operation-status { display: flex; align-items: flex-start; gap: 10px; border-bottom: 1px solid #dbeafe; color: #1e40af; background: #eff6ff; padding: 11px 15px; }.operation-status > div { display: flex; min-width: 0; flex-direction: column; gap: 3px; }.operation-status strong { font-size: 13px; }.operation-status small { color: inherit; font-size: 12px; line-height: 1.5; }.status-dot { width: 9px; height: 9px; border: 2px solid currentcolor; border-radius: 50%; flex: 0 0 auto; margin-top: 3px; }.operation-status.submitting .status-dot, .operation-status.generating .status-dot { border-right-color: transparent; animation: spin .8s linear infinite; }.operation-status.success { border-color: #bbf7d0; color: #166534; background: #f0fdf4; }.operation-status.success .status-dot { background: currentcolor; }.operation-status.error { border-color: #fecaca; color: #b42318; background: #fef2f2; }.operation-status.error .status-dot { border-radius: 2px; transform: rotate(45deg); }
.conversation-list::-webkit-scrollbar, .messages::-webkit-scrollbar, .summary-scroll::-webkit-scrollbar { width: 8px; }.conversation-list::-webkit-scrollbar-thumb, .messages::-webkit-scrollbar-thumb, .summary-scroll::-webkit-scrollbar-thumb { border: 2px solid transparent; border-radius: 8px; background: #cbd5e1; background-clip: padding-box; }
.usage-card { border-radius: 18px; margin-top: 18px; padding-bottom: 8px; }
@keyframes spin { to { transform: rotate(360deg); } }@keyframes typing { 0%, 60%, 100% { opacity: .25; transform: translateY(0); } 30% { opacity: 1; transform: translateY(-2px); } }
@media (max-width: 1180px) { .assistant-grid { height: auto; grid-template-columns: 210px minmax(0, 1fr); }.conversation-panel, .chat-panel { height: 640px; }.summary-panel { height: 620px; grid-column: 1 / -1; } }
@media (max-width: 760px) { .content-picker { align-items: stretch; flex-direction: column; }.content-picker .el-select { width: 100%; }.assistant-grid { grid-template-columns: 1fr; }.summary-panel { height: 580px; grid-column: auto; }.conversation-panel { height: 320px; }.chat-panel { height: 620px; }.quota-card { min-width: 170px; } }
</style>
