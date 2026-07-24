<script setup lang="ts">
import { ElMessage, ElMessageBox } from 'element-plus'
import { onMounted, reactive, ref } from 'vue'

import {
  getAdminExam,
  listAdminExams,
  listAdminUsers,
  listOperationLogs,
  updateAdminUserRoles,
  updateAdminUserStatus,
} from '@/api/admin'
import {
  approveContent,
  createCategory,
  deleteCategory,
  getAdminContent,
  listAdminCategories,
  listAdminContents,
  offlineContent,
  publishContent,
  rejectContent,
  updateCategory,
} from '@/api/content'
import { getAdminOrder, listAdminOrders } from '@/api/order'
import ContentStatusTag from '@/components/ContentStatusTag.vue'
import SectionPageHeader from '@/components/SectionPageHeader.vue'
import type {
  AdminExamDetail,
  AdminExamSummary,
  AdminUser,
  OperationLog,
  OperationResult,
} from '@/types/admin'
import type { RoleCode, UserStatus } from '@/types/auth'
import type {
  CategoryWritePayload,
  ContentCategory,
  ContentDetail,
  ContentStatus,
  ContentSummary,
} from '@/types/content'
import type { Order, OrderStatus } from '@/types/order'

const activeTab = ref('review')
const reviewLoading = ref(false)
const contents = ref<ContentSummary[]>([])
const reviewTotal = ref(0)
const reviewFilters = reactive({
  keyword: '',
  status: 'PENDING_REVIEW' as ContentStatus | undefined,
  pageNumber: 1,
  pageSize: 10,
})
const previewVisible = ref(false)
const previewLoading = ref(false)
const preview = ref<ContentDetail>()

const categoryLoading = ref(false)
const categories = ref<ContentCategory[]>([])
const categoryVisible = ref(false)
const categorySaving = ref(false)
const editingCategoryId = ref<number>()
const categoryForm = reactive<CategoryWritePayload>({
  parentId: undefined,
  name: '',
  slug: '',
  description: '',
  sortOrder: 0,
  enabled: true,
})
const orderLoading = ref(false)
const orders = ref<Order[]>([])
const orderTotal = ref(0)
const orderFilters = reactive({
  orderNo: '',
  userId: undefined as number | undefined,
  status: undefined as OrderStatus | undefined,
  pageNumber: 1,
  pageSize: 10,
})
const orderPreviewVisible = ref(false)
const orderPreviewLoading = ref(false)
const orderPreview = ref<Order>()
const orderStatusLabels: Record<OrderStatus, string> = {
  PENDING_PAYMENT: '待模拟支付',
  PAID: '模拟支付成功',
  CANCELLED: '已取消',
  CLOSED: '已关闭',
  REFUNDED: '已退款',
}
const userLoading = ref(false)
const users = ref<AdminUser[]>([])
const userTotal = ref(0)
const userFilters = reactive({
  keyword: '',
  status: undefined as UserStatus | undefined,
  role: undefined as RoleCode | undefined,
  pageNumber: 1,
  pageSize: 10,
})
const roleDialogVisible = ref(false)
const roleSaving = ref(false)
const editingUser = ref<AdminUser>()
const selectedRoles = ref<RoleCode[]>([])
const roleLabels: Record<RoleCode, string> = {
  USER: '普通用户',
  PUBLISHER: '发布者',
  ADMIN: '管理员',
}
const userStatusLabels: Record<UserStatus, string> = {
  ACTIVE: '正常',
  DISABLED: '已禁用',
  LOCKED: '已锁定',
}
const examLoading = ref(false)
const exams = ref<AdminExamSummary[]>([])
const examTotal = ref(0)
const examFilters = reactive({
  keyword: '',
  publisherId: undefined as number | undefined,
  status: undefined as import('@/types/exam').ExamStatus | undefined,
  pageNumber: 1,
  pageSize: 10,
})
const examPreviewVisible = ref(false)
const examPreviewLoading = ref(false)
const examPreview = ref<AdminExamDetail>()
const examStatusLabels: Record<import('@/types/exam').ExamStatus, string> = {
  DRAFT: '草稿',
  PUBLISHED: '已发布',
  ONGOING: '进行中',
  FINISHED: '已结束',
  CANCELLED: '已取消',
}
const auditLoading = ref(false)
const operationLogs = ref<OperationLog[]>([])
const auditTotal = ref(0)
const auditFilters = reactive({
  module: '',
  action: '',
  result: undefined as OperationResult | undefined,
  requestId: '',
  pageNumber: 1,
  pageSize: 10,
})
const auditModuleLabels: Record<string, string> = {
  AUTH: '登录认证',
  USER: '用户与角色',
  CONTENT: '资料审核',
  ORDER: '订单权益',
  EXAM: '考试发布',
  GRADING: '人工阅卷',
}

async function loadReviews(): Promise<void> {
  reviewLoading.value = true
  try {
    const page = await listAdminContents(reviewFilters)
    contents.value = page.items
    reviewTotal.value = page.total
  } catch (error) {
    ElMessage.error(error instanceof Error ? error.message : '资料列表加载失败')
  } finally {
    reviewLoading.value = false
  }
}

async function openPreview(contentId: number): Promise<void> {
  previewVisible.value = true
  previewLoading.value = true
  preview.value = undefined
  try {
    preview.value = await getAdminContent(contentId)
  } catch (error) {
    ElMessage.error(error instanceof Error ? error.message : '资料详情加载失败')
  } finally {
    previewLoading.value = false
  }
}

async function approve(contentId: number): Promise<void> {
  try {
    await ElMessageBox.confirm('确认审核通过并发布这份资料吗？', '审核通过', {
      type: 'success',
    })
    await approveContent(contentId)
    ElMessage.success('资料已审核通过')
    await loadReviews()
  } catch (error) {
    if (error !== 'cancel') ElMessage.error(error instanceof Error ? error.message : '审核失败')
  }
}

async function reject(contentId: number): Promise<void> {
  try {
    const { value } = await ElMessageBox.prompt('请填写明确的驳回原因', '驳回资料', {
      inputValidator: (text) => Boolean(text?.trim()) || '驳回原因不能为空',
      inputType: 'textarea',
      type: 'warning',
    })
    await rejectContent(contentId, value.trim())
    ElMessage.success('资料已驳回')
    await loadReviews()
  } catch (error) {
    if (error !== 'cancel') ElMessage.error(error instanceof Error ? error.message : '驳回失败')
  }
}

async function changePublishState(contentId: number, status: ContentStatus): Promise<void> {
  const shouldOffline = status === 'PUBLISHED'
  try {
    await ElMessageBox.confirm(
      shouldOffline ? '下架后用户端将不再显示该资料，确认继续吗？' : '确认重新发布该资料吗？',
      shouldOffline ? '下架资料' : '重新发布',
      { type: shouldOffline ? 'warning' : 'info' },
    )
    if (shouldOffline) await offlineContent(contentId)
    else await publishContent(contentId)
    ElMessage.success(shouldOffline ? '资料已下架' : '资料已重新发布')
    await loadReviews()
  } catch (error) {
    if (error !== 'cancel') ElMessage.error(error instanceof Error ? error.message : '操作失败')
  }
}

async function loadCategories(): Promise<void> {
  categoryLoading.value = true
  try {
    categories.value = await listAdminCategories()
  } catch (error) {
    ElMessage.error(error instanceof Error ? error.message : '分类列表加载失败')
  } finally {
    categoryLoading.value = false
  }
}

function resetCategoryForm(category?: ContentCategory): void {
  editingCategoryId.value = category?.id
  categoryForm.parentId = category?.parentId
  categoryForm.name = category?.name ?? ''
  categoryForm.slug = category?.slug ?? ''
  categoryForm.description = category?.description ?? ''
  categoryForm.sortOrder = category?.sortOrder ?? 0
  categoryForm.enabled = category?.enabled ?? true
  categoryVisible.value = true
}

function editCategory(categoryId: number): void {
  resetCategoryForm(categories.value.find((category) => category.id === categoryId))
}

async function saveCategory(): Promise<void> {
  if (!categoryForm.name.trim() || !categoryForm.slug.trim()) {
    ElMessage.warning('分类名称和标识不能为空')
    return
  }
  categorySaving.value = true
  try {
    const payload = {
      ...categoryForm,
      name: categoryForm.name.trim(),
      slug: categoryForm.slug.trim(),
      description: categoryForm.description.trim(),
    }
    if (editingCategoryId.value) await updateCategory(editingCategoryId.value, payload)
    else await createCategory(payload)
    categoryVisible.value = false
    ElMessage.success(editingCategoryId.value ? '分类已更新' : '分类已创建')
    await loadCategories()
  } catch (error) {
    ElMessage.error(error instanceof Error ? error.message : '分类保存失败')
  } finally {
    categorySaving.value = false
  }
}

async function removeCategory(categoryId: number): Promise<void> {
  const category = categories.value.find((item) => item.id === categoryId)
  if (!category) return
  try {
    await ElMessageBox.confirm(
      `确认删除分类“${category.name}”吗？已有资料使用时服务端会拒绝删除。`,
      '删除分类',
      { type: 'warning' },
    )
    await deleteCategory(category.id)
    ElMessage.success('分类已删除')
    await loadCategories()
  } catch (error) {
    if (error !== 'cancel') ElMessage.error(error instanceof Error ? error.message : '删除失败')
  }
}

async function loadOrders(): Promise<void> {
  orderLoading.value = true
  try {
    const page = await listAdminOrders({
      ...orderFilters,
      orderNo: orderFilters.orderNo.trim() || undefined,
    })
    orders.value = page.items
    orderTotal.value = page.total
  } catch (error) {
    ElMessage.error(error instanceof Error ? error.message : '订单列表加载失败')
  } finally {
    orderLoading.value = false
  }
}

async function loadUsers(): Promise<void> {
  userLoading.value = true
  try {
    const page = await listAdminUsers({
      ...userFilters,
      keyword: userFilters.keyword.trim() || undefined,
    })
    users.value = page.items
    userTotal.value = page.total
  } catch (error) {
    ElMessage.error(error instanceof Error ? error.message : '用户列表加载失败')
  } finally {
    userLoading.value = false
  }
}

function searchUsers(): void {
  userFilters.pageNumber = 1
  void loadUsers()
}

async function changeUserStatus(user: AdminUser): Promise<void> {
  const nextStatus: UserStatus = user.status === 'ACTIVE' ? 'DISABLED' : 'ACTIVE'
  const disabling = nextStatus === 'DISABLED'
  try {
    await ElMessageBox.confirm(
      disabling
        ? `禁用“${user.username}”后，其现有登录状态将立即失效。确认继续吗？`
        : `确认重新启用“${user.username}”吗？`,
      disabling ? '禁用账号' : '启用账号',
      { type: disabling ? 'warning' : 'success' },
    )
    await updateAdminUserStatus(user.id, nextStatus)
    ElMessage.success(disabling ? '账号已禁用' : '账号已启用')
    await loadUsers()
  } catch (error) {
    if (error !== 'cancel') {
      ElMessage.error(error instanceof Error ? error.message : '账号状态更新失败')
    }
  }
}

function openRoleDialog(user: AdminUser): void {
  editingUser.value = user
  selectedRoles.value = [...user.roles]
  roleDialogVisible.value = true
}

async function saveRoles(): Promise<void> {
  if (!editingUser.value || selectedRoles.value.length === 0) {
    ElMessage.warning('用户至少需要保留一个角色')
    return
  }
  try {
    const roleNames = selectedRoles.value.map((role) => roleLabels[role]).join('、')
    await ElMessageBox.confirm(
      `确定将“${editingUser.value.username}”的角色替换为：${roleNames}？权限变更会立即生效。`,
      '确认分配角色',
      {
        type: selectedRoles.value.includes('ADMIN') ? 'warning' : 'info',
        confirmButtonText: '确认变更',
      },
    )
    roleSaving.value = true
    await updateAdminUserRoles(editingUser.value.id, selectedRoles.value)
    roleDialogVisible.value = false
    ElMessage.success('用户角色已更新')
    await loadUsers()
  } catch (error) {
    if (error !== 'cancel') {
      ElMessage.error(error instanceof Error ? error.message : '角色更新失败')
    }
  } finally {
    roleSaving.value = false
  }
}

async function loadExams(): Promise<void> {
  examLoading.value = true
  try {
    const page = await listAdminExams({
      ...examFilters,
      keyword: examFilters.keyword.trim() || undefined,
    })
    exams.value = page.items
    examTotal.value = page.total
  } catch (error) {
    ElMessage.error(error instanceof Error ? error.message : '考试列表加载失败')
  } finally {
    examLoading.value = false
  }
}

function searchExams(): void {
  examFilters.pageNumber = 1
  void loadExams()
}

async function openExam(examId: number): Promise<void> {
  examPreviewVisible.value = true
  examPreviewLoading.value = true
  examPreview.value = undefined
  try {
    examPreview.value = await getAdminExam(examId)
  } catch (error) {
    ElMessage.error(error instanceof Error ? error.message : '考试详情加载失败')
  } finally {
    examPreviewLoading.value = false
  }
}

async function openOrder(orderId: number): Promise<void> {
  orderPreviewVisible.value = true
  orderPreviewLoading.value = true
  orderPreview.value = undefined
  try {
    orderPreview.value = await getAdminOrder(orderId)
  } catch (error) {
    ElMessage.error(error instanceof Error ? error.message : '订单详情加载失败')
  } finally {
    orderPreviewLoading.value = false
  }
}

async function loadOperationLogs(): Promise<void> {
  auditLoading.value = true
  try {
    const page = await listOperationLogs({
      ...auditFilters,
      module: auditFilters.module.trim() || undefined,
      action: auditFilters.action.trim() || undefined,
      requestId: auditFilters.requestId.trim() || undefined,
    })
    operationLogs.value = page.items
    auditTotal.value = page.total
  } catch (error) {
    ElMessage.error(error instanceof Error ? error.message : '操作日志加载失败')
  } finally {
    auditLoading.value = false
  }
}

function searchOperationLogs(): void {
  auditFilters.pageNumber = 1
  void loadOperationLogs()
}

function searchOrders(): void {
  orderFilters.pageNumber = 1
  void loadOrders()
}

function money(value?: number): string {
  return `¥${Number(value ?? 0).toFixed(2)}`
}

function dateTime(value?: string): string {
  return value ? new Date(value).toLocaleString() : '—'
}

function searchReviews(): void {
  reviewFilters.pageNumber = 1
  void loadReviews()
}

onMounted(() =>
  Promise.all([
    loadReviews(),
    loadCategories(),
    loadOrders(),
    loadUsers(),
    loadExams(),
    loadOperationLogs(),
  ]),
)
</script>

<template>
  <section class="admin-page">
    <div class="page-container">
      <SectionPageHeader
        eyebrow="ADMIN CONSOLE"
        title="平台管理后台"
        description="统一管理用户与角色、学习资料、分类、考试、模拟订单和 AI 配置"
      >
        <RouterLink to="/admin/ai">
          <el-button type="primary" plain>查看 AI 配置</el-button>
        </RouterLink>
      </SectionPageHeader>

      <div class="workspace-card">
        <el-tabs v-model="activeTab">
          <el-tab-pane label="用户管理" name="users">
            <div class="user-toolbar">
              <el-input
                v-model="userFilters.keyword"
                clearable
                placeholder="用户名、昵称、邮箱或手机号"
                @keyup.enter="searchUsers"
              />
              <el-select v-model="userFilters.status" clearable placeholder="全部账号状态">
                <el-option label="正常" value="ACTIVE" />
                <el-option label="已禁用" value="DISABLED" />
                <el-option label="已锁定" value="LOCKED" />
              </el-select>
              <el-select v-model="userFilters.role" clearable placeholder="全部角色">
                <el-option label="普通用户" value="USER" />
                <el-option label="发布者" value="PUBLISHER" />
                <el-option label="管理员" value="ADMIN" />
              </el-select>
              <el-button type="primary" @click="searchUsers">查询</el-button>
            </div>
            <el-table v-loading="userLoading" :data="users">
              <el-table-column label="用户" min-width="190">
                <template #default="{ row }">
                  <strong>{{ row.username }}</strong>
                  <div class="table-subtitle">{{ row.nickname }}</div>
                </template>
              </el-table-column>
              <el-table-column label="联系方式" min-width="210">
                <template #default="{ row }">
                  <div>{{ row.email || '—' }}</div>
                  <div class="table-subtitle">{{ row.phone || '—' }}</div>
                </template>
              </el-table-column>
              <el-table-column label="角色" min-width="210">
                <template #default="{ row }">
                  <el-tag
                    v-for="role in row.roles"
                    :key="role"
                    class="role-tag"
                    :type="role === 'ADMIN' ? 'danger' : role === 'PUBLISHER' ? 'warning' : 'info'"
                  >
                    {{ roleLabels[role as RoleCode] }}
                  </el-tag>
                </template>
              </el-table-column>
              <el-table-column label="状态" width="110">
                <template #default="{ row }">
                  <el-tag :type="row.status === 'ACTIVE' ? 'success' : 'danger'">
                    {{ userStatusLabels[row.status as UserStatus] }}
                  </el-tag>
                </template>
              </el-table-column>
              <el-table-column label="最后登录" min-width="170">
                <template #default="{ row }">{{ dateTime(row.lastLoginAt) }}</template>
              </el-table-column>
              <el-table-column label="操作" width="180" fixed="right">
                <template #default="{ row }">
                  <el-button link type="primary" @click="openRoleDialog(row as AdminUser)">
                    分配角色
                  </el-button>
                  <el-button
                    link
                    :type="row.status === 'ACTIVE' ? 'danger' : 'success'"
                    :disabled="row.status === 'LOCKED'"
                    @click="changeUserStatus(row as AdminUser)"
                  >
                    {{ row.status === 'ACTIVE' ? '禁用' : '启用' }}
                  </el-button>
                </template>
              </el-table-column>
              <template #empty><el-empty description="暂无符合条件的用户" /></template>
            </el-table>
            <el-pagination
              v-model:current-page="userFilters.pageNumber"
              v-model:page-size="userFilters.pageSize"
              background
              layout="total, prev, pager, next"
              :total="userTotal"
              @current-change="loadUsers"
            />
          </el-tab-pane>

          <el-tab-pane label="资料审核" name="review">
            <div class="toolbar">
              <el-input
                v-model="reviewFilters.keyword"
                clearable
                placeholder="搜索资料标题"
                @keyup.enter="searchReviews"
              />
              <el-select v-model="reviewFilters.status" clearable placeholder="全部状态">
                <el-option label="待审核" value="PENDING_REVIEW" />
                <el-option label="已发布" value="PUBLISHED" />
                <el-option label="已驳回" value="REJECTED" />
                <el-option label="已下架" value="OFFLINE" />
                <el-option label="草稿" value="DRAFT" />
              </el-select>
              <el-button type="primary" @click="searchReviews">查询</el-button>
            </div>

            <el-table v-loading="reviewLoading" :data="contents">
              <el-table-column prop="title" label="资料标题" min-width="230" />
              <el-table-column prop="contentType" label="类型" width="105" />
              <el-table-column label="价格" width="110">
                <template #default="{ row }">
                  {{ row.isFree ? '免费' : `¥${Number(row.price).toFixed(2)}` }}
                </template>
              </el-table-column>
              <el-table-column label="状态" width="120">
                <template #default="{ row }"><ContentStatusTag :status="row.status" /></template>
              </el-table-column>
              <el-table-column prop="updatedAt" label="更新时间" min-width="170" />
              <el-table-column label="操作" width="270" fixed="right">
                <template #default="{ row }">
                  <el-button link type="primary" @click="openPreview(row.id)">查看</el-button>
                  <template v-if="row.status === 'PENDING_REVIEW'">
                    <el-button link type="success" @click="approve(row.id)">通过</el-button>
                    <el-button link type="danger" @click="reject(row.id)">驳回</el-button>
                  </template>
                  <el-button
                    v-if="row.status === 'PUBLISHED' || row.status === 'OFFLINE'"
                    link
                    :type="row.status === 'PUBLISHED' ? 'warning' : 'success'"
                    @click="changePublishState(row.id, row.status)"
                  >
                    {{ row.status === 'PUBLISHED' ? '下架' : '发布' }}
                  </el-button>
                </template>
              </el-table-column>
              <template #empty><el-empty description="暂无符合条件的资料" /></template>
            </el-table>
            <el-pagination
              v-model:current-page="reviewFilters.pageNumber"
              v-model:page-size="reviewFilters.pageSize"
              background
              layout="total, prev, pager, next"
              :total="reviewTotal"
              @current-change="loadReviews"
            />
          </el-tab-pane>

          <el-tab-pane label="分类管理" name="categories">
            <div class="category-actions">
              <p>分类停用后不会出现在发布者和用户端的分类选择中。</p>
              <el-button type="primary" @click="resetCategoryForm()">新建分类</el-button>
            </div>
            <el-table v-loading="categoryLoading" :data="categories">
              <el-table-column prop="name" label="分类名称" min-width="160" />
              <el-table-column prop="slug" label="标识" min-width="150" />
              <el-table-column prop="description" label="说明" min-width="230" />
              <el-table-column prop="sortOrder" label="排序" width="90" />
              <el-table-column label="状态" width="100">
                <template #default="{ row }">
                  <el-tag :type="row.enabled ? 'success' : 'info'">
                    {{ row.enabled ? '启用' : '停用' }}
                  </el-tag>
                </template>
              </el-table-column>
              <el-table-column label="操作" width="150" fixed="right">
                <template #default="{ row }">
                  <el-button link type="primary" @click="editCategory(row.id)">编辑</el-button>
                  <el-button link type="danger" @click="removeCategory(row.id)">删除</el-button>
                </template>
              </el-table-column>
              <template #empty><el-empty description="暂无分类，请先创建" /></template>
            </el-table>
          </el-tab-pane>

          <el-tab-pane label="考试查看" name="exams">
            <div class="exam-toolbar">
              <el-input
                v-model="examFilters.keyword"
                clearable
                placeholder="搜索考试名称"
                @keyup.enter="searchExams"
              />
              <el-input-number
                v-model="examFilters.publisherId"
                :min="1"
                :controls="false"
                placeholder="发布者 ID"
              />
              <el-select v-model="examFilters.status" clearable placeholder="全部考试状态">
                <el-option label="草稿" value="DRAFT" />
                <el-option label="已发布" value="PUBLISHED" />
                <el-option label="进行中" value="ONGOING" />
                <el-option label="已结束" value="FINISHED" />
                <el-option label="已取消" value="CANCELLED" />
              </el-select>
              <el-button type="primary" @click="searchExams">查询</el-button>
            </div>
            <el-table v-loading="examLoading" :data="exams">
              <el-table-column label="考试" min-width="230">
                <template #default="{ row }">
                  <strong>{{ row.exam.name }}</strong>
                  <div class="table-subtitle">ID {{ row.exam.id }} · 试卷 {{ row.exam.paperId }}</div>
                </template>
              </el-table-column>
              <el-table-column label="发布者" min-width="170">
                <template #default="{ row }">
                  {{ row.publisherNickname || row.publisherUsername }}
                  <div class="table-subtitle">
                    {{ row.publisherUsername }} · ID {{ row.exam.publisherId }}
                  </div>
                </template>
              </el-table-column>
              <el-table-column label="状态" width="110">
                <template #default="{ row }">
                  <el-tag :type="row.exam.status === 'PUBLISHED' ? 'success' : 'info'">
                    {{ examStatusLabels[row.exam.status as import('@/types/exam').ExamStatus] }}
                  </el-tag>
                </template>
              </el-table-column>
              <el-table-column label="开放时间" min-width="230">
                <template #default="{ row }">
                  {{ dateTime(row.exam.startAt) }}
                  <div class="table-subtitle">至 {{ dateTime(row.exam.endAt) }}</div>
                </template>
              </el-table-column>
              <el-table-column label="时长" width="100">
                <template #default="{ row }">{{ row.exam.durationMinutes }} 分钟</template>
              </el-table-column>
              <el-table-column label="操作" width="90" fixed="right">
                <template #default="{ row }">
                  <el-button link type="primary" @click="openExam(row.exam.id)">查看</el-button>
                </template>
              </el-table-column>
              <template #empty><el-empty description="暂无符合条件的考试" /></template>
            </el-table>
            <el-pagination
              v-model:current-page="examFilters.pageNumber"
              v-model:page-size="examFilters.pageSize"
              background
              layout="total, prev, pager, next"
              :total="examTotal"
              @current-change="loadExams"
            />
          </el-tab-pane>

          <el-tab-pane label="操作日志" name="audit">
            <div class="audit-toolbar">
              <el-select v-model="auditFilters.module" clearable placeholder="全部模块">
                <el-option label="登录认证" value="AUTH" />
                <el-option label="用户与角色" value="USER" />
                <el-option label="资料审核" value="CONTENT" />
                <el-option label="订单权益" value="ORDER" />
                <el-option label="考试发布" value="EXAM" />
                <el-option label="人工阅卷" value="GRADING" />
              </el-select>
              <el-input
                v-model="auditFilters.action"
                clearable
                placeholder="操作标识"
                @keyup.enter="searchOperationLogs"
              />
              <el-select v-model="auditFilters.result" clearable placeholder="全部结果">
                <el-option label="成功" value="SUCCESS" />
                <el-option label="失败" value="FAILURE" />
              </el-select>
              <el-input
                v-model="auditFilters.requestId"
                clearable
                placeholder="请求 ID"
                @keyup.enter="searchOperationLogs"
              />
              <el-button type="primary" @click="searchOperationLogs">查询</el-button>
            </div>
            <el-alert
              title="日志仅保存操作元数据，不记录密码、令牌和请求正文"
              type="info"
              show-icon
              :closable="false"
            />
            <el-table v-loading="auditLoading" :data="operationLogs">
              <el-table-column label="时间 / 操作者" min-width="190">
                <template #default="{ row }">
                  <strong>{{ dateTime(row.createdAt) }}</strong>
                  <div class="table-subtitle">
                    {{ row.operatorName || '未认证用户' }}
                    <span v-if="row.operatorId"> · ID {{ row.operatorId }}</span>
                  </div>
                </template>
              </el-table-column>
              <el-table-column label="模块 / 操作" min-width="190">
                <template #default="{ row }">
                  {{ auditModuleLabels[row.module] || row.module }}
                  <div class="table-subtitle">{{ row.action }}</div>
                </template>
              </el-table-column>
              <el-table-column label="目标" min-width="140">
                <template #default="{ row }">
                  {{ row.targetType || '—' }}
                  <div class="table-subtitle">{{ row.targetId || '—' }}</div>
                </template>
              </el-table-column>
              <el-table-column label="请求" min-width="260">
                <template #default="{ row }">
                  <code>{{ row.requestMethod }} {{ row.requestPath }}</code>
                  <div class="table-subtitle">请求 ID：{{ row.requestId || '—' }}</div>
                </template>
              </el-table-column>
              <el-table-column label="结果" width="120">
                <template #default="{ row }">
                  <el-tag :type="row.result === 'SUCCESS' ? 'success' : 'danger'">
                    {{ row.result === 'SUCCESS' ? '成功' : '失败' }}
                  </el-tag>
                  <div class="table-subtitle">{{ row.durationMs }} ms</div>
                </template>
              </el-table-column>
              <el-table-column label="来源" min-width="150">
                <template #default="{ row }">
                  {{ row.ipAddress || '—' }}
                </template>
              </el-table-column>
              <template #empty><el-empty description="暂无符合条件的操作日志" /></template>
            </el-table>
            <el-pagination
              v-model:current-page="auditFilters.pageNumber"
              v-model:page-size="auditFilters.pageSize"
              background
              layout="total, prev, pager, next"
              :total="auditTotal"
              @current-change="loadOperationLogs"
            />
          </el-tab-pane>

          <el-tab-pane label="订单管理" name="orders">
            <el-alert
              title="当前仅展示模拟支付订单"
              description="这些订单和支付记录只用于开发验收，不对应任何真实资金交易。"
              type="warning"
              show-icon
              :closable="false"
            />
            <div class="order-toolbar">
              <el-input
                v-model="orderFilters.orderNo"
                clearable
                placeholder="搜索订单号"
                @keyup.enter="searchOrders"
              />
              <el-input-number
                v-model="orderFilters.userId"
                :min="1"
                :controls="false"
                placeholder="用户 ID"
              />
              <el-select v-model="orderFilters.status" clearable placeholder="全部订单状态">
                <el-option label="待模拟支付" value="PENDING_PAYMENT" />
                <el-option label="模拟支付成功" value="PAID" />
                <el-option label="已取消" value="CANCELLED" />
                <el-option label="已关闭" value="CLOSED" />
                <el-option label="已退款" value="REFUNDED" />
              </el-select>
              <el-button type="primary" @click="searchOrders">查询</el-button>
            </div>
            <el-table v-loading="orderLoading" :data="orders">
              <el-table-column prop="orderNo" label="订单号" min-width="230" />
              <el-table-column prop="userId" label="用户 ID" width="100" />
              <el-table-column label="订单状态" width="140">
                <template #default="{ row }">
                  <el-tag :type="row.status === 'PAID' ? 'success' : row.status === 'PENDING_PAYMENT' ? 'warning' : 'info'">
                    {{ orderStatusLabels[row.status as OrderStatus] }}
                  </el-tag>
                </template>
              </el-table-column>
              <el-table-column label="金额" width="120">
                <template #default="{ row }">{{ money(row.payableAmount) }}</template>
              </el-table-column>
              <el-table-column label="支付方式" width="130">
                <template #default="{ row }">
                  {{ row.paymentMethod === 'MOCK' ? '模拟支付' : '未支付' }}
                </template>
              </el-table-column>
              <el-table-column label="创建时间" min-width="180">
                <template #default="{ row }">{{ dateTime(row.createdAt) }}</template>
              </el-table-column>
              <el-table-column label="操作" width="90" fixed="right">
                <template #default="{ row }">
                  <el-button link type="primary" @click="openOrder(row.id)">查看</el-button>
                </template>
              </el-table-column>
              <template #empty><el-empty description="暂无符合条件的模拟订单" /></template>
            </el-table>
            <el-pagination
              v-model:current-page="orderFilters.pageNumber"
              v-model:page-size="orderFilters.pageSize"
              background
              layout="total, prev, pager, next"
              :total="orderTotal"
              @current-change="loadOrders"
            />
          </el-tab-pane>
        </el-tabs>
      </div>
    </div>

    <el-drawer v-model="previewVisible" title="资料审核预览" size="560px">
      <div v-loading="previewLoading" class="preview-content">
        <template v-if="preview">
          <ContentStatusTag :status="preview.status" />
          <h2>{{ preview.title }}</h2>
          <p class="summary">{{ preview.summary || '暂无简介' }}</p>
          <el-descriptions :column="2" border>
            <el-descriptions-item label="资料类型">{{ preview.contentType }}</el-descriptions-item>
            <el-descriptions-item label="收费方式">
              {{ preview.isFree ? '免费' : `¥${Number(preview.price).toFixed(2)}` }}
            </el-descriptions-item>
            <el-descriptions-item label="文件数量">{{ preview.files.length }}</el-descriptions-item>
            <el-descriptions-item label="发布者">{{ preview.publisherName || `用户 ${preview.publisherId}` }}</el-descriptions-item>
          </el-descriptions>
          <h3>图文正文</h3>
          <div class="article-body">{{ preview.articleBody || '该资料没有图文正文。' }}</div>
          <h3>文件</h3>
          <div v-for="file in preview.files" :key="file.id" class="preview-file">
            <strong>{{ file.originalName }}</strong><span>{{ file.fileRole }}</span>
          </div>
        </template>
      </div>
    </el-drawer>

    <el-drawer v-model="examPreviewVisible" title="考试详情" size="640px">
      <div v-loading="examPreviewLoading" class="preview-content">
        <template v-if="examPreview">
          <el-tag>
            {{ examStatusLabels[examPreview.management.exam.status] }}
          </el-tag>
          <h2>{{ examPreview.management.exam.name }}</h2>
          <p class="summary">{{ examPreview.management.instructions || '暂无考试说明' }}</p>
          <el-descriptions :column="2" border>
            <el-descriptions-item label="发布者">
              {{ examPreview.publisherNickname || examPreview.publisherUsername }}
              （{{ examPreview.publisherUsername }}）
            </el-descriptions-item>
            <el-descriptions-item label="试卷">
              {{ examPreview.management.paper.name }}
            </el-descriptions-item>
            <el-descriptions-item label="开始时间">
              {{ dateTime(examPreview.management.exam.startAt) }}
            </el-descriptions-item>
            <el-descriptions-item label="结束时间">
              {{ dateTime(examPreview.management.exam.endAt) }}
            </el-descriptions-item>
            <el-descriptions-item label="答题时长">
              {{ examPreview.management.exam.durationMinutes }} 分钟
            </el-descriptions-item>
            <el-descriptions-item label="及格分">
              {{ examPreview.management.exam.passingScore }}
            </el-descriptions-item>
            <el-descriptions-item label="题目数量">
              {{ examPreview.management.paper.questionCount }}
            </el-descriptions-item>
            <el-descriptions-item label="试卷总分">
              {{ examPreview.management.paper.totalScore }}
            </el-descriptions-item>
          </el-descriptions>
          <h3>指定考生（{{ examPreview.management.candidates.length }}）</h3>
          <el-table :data="examPreview.management.candidates" max-height="320">
            <el-table-column prop="username" label="用户名" min-width="150" />
            <el-table-column prop="nickname" label="昵称" min-width="130" />
            <el-table-column prop="status" label="状态" width="110" />
          </el-table>
        </template>
      </div>
    </el-drawer>

    <el-drawer v-model="orderPreviewVisible" title="模拟订单详情" size="620px">
      <div v-loading="orderPreviewLoading" class="preview-content">
        <template v-if="orderPreview">
          <el-alert
            title="模拟支付记录，不代表真实交易"
            type="warning"
            show-icon
            :closable="false"
          />
          <h2>{{ orderPreview.orderNo }}</h2>
          <el-descriptions :column="2" border>
            <el-descriptions-item label="用户 ID">{{ orderPreview.userId }}</el-descriptions-item>
            <el-descriptions-item label="状态">
              {{ orderStatusLabels[orderPreview.status] }}
            </el-descriptions-item>
            <el-descriptions-item label="订单金额">
              {{ money(orderPreview.totalAmount) }}
            </el-descriptions-item>
            <el-descriptions-item label="实付金额">
              {{ orderPreview.paidAmount == null ? '—' : money(orderPreview.paidAmount) }}
            </el-descriptions-item>
            <el-descriptions-item label="创建时间">
              {{ dateTime(orderPreview.createdAt) }}
            </el-descriptions-item>
            <el-descriptions-item label="模拟支付时间">
              {{ dateTime(orderPreview.paidAt) }}
            </el-descriptions-item>
          </el-descriptions>
          <h3>商品明细</h3>
          <div v-for="item in orderPreview.items" :key="item.id" class="preview-file">
            <strong>{{ item.productName }} × {{ item.quantity }}</strong>
            <span>{{ money(item.subtotalAmount) }}</span>
          </div>
          <h3>模拟支付记录</h3>
          <div v-for="payment in orderPreview.payments" :key="payment.id" class="payment-record">
            <strong>{{ payment.paymentNo }}</strong>
            <span>{{ payment.provider }} · {{ payment.status }} · {{ money(payment.amount) }}</span>
          </div>
          <el-empty
            v-if="orderPreview.payments.length === 0"
            description="订单尚未进行模拟支付"
            :image-size="70"
          />
        </template>
      </div>
    </el-drawer>

    <el-dialog v-model="roleDialogVisible" title="分配用户角色" width="480px">
      <template v-if="editingUser">
        <p class="dialog-description">
          正在修改 <strong>{{ editingUser.username }}</strong> 的角色。角色变更会在其下一次
          API 请求时立即生效。
        </p>
        <el-checkbox-group v-model="selectedRoles" class="role-options">
          <el-checkbox value="USER">普通用户</el-checkbox>
          <el-checkbox value="PUBLISHER">发布者</el-checkbox>
          <el-checkbox value="ADMIN">管理员</el-checkbox>
        </el-checkbox-group>
      </template>
      <template #footer>
        <el-button @click="roleDialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="roleSaving" @click="saveRoles">保存角色</el-button>
      </template>
    </el-dialog>

    <el-dialog
      v-model="categoryVisible"
      :title="editingCategoryId ? '编辑分类' : '新建分类'"
      width="520px"
    >
      <el-form label-position="top">
        <el-form-item label="分类名称" required>
          <el-input v-model="categoryForm.name" maxlength="100" />
        </el-form-item>
        <el-form-item label="分类标识" required>
          <el-input v-model="categoryForm.slug" maxlength="100" placeholder="例如 frontend" />
        </el-form-item>
        <el-form-item label="说明">
          <el-input v-model="categoryForm.description" type="textarea" :rows="3" maxlength="500" />
        </el-form-item>
        <div class="category-form-row">
          <el-form-item label="排序">
            <el-input-number v-model="categoryForm.sortOrder" :min="0" />
          </el-form-item>
          <el-form-item label="状态">
            <el-switch v-model="categoryForm.enabled" active-text="启用" inactive-text="停用" />
          </el-form-item>
        </div>
      </el-form>
      <template #footer>
        <el-button @click="categoryVisible = false">取消</el-button>
        <el-button type="primary" :loading="categorySaving" @click="saveCategory">保存</el-button>
      </template>
    </el-dialog>
  </section>
</template>

<style scoped>
.admin-page { min-height: calc(100vh - 145px); padding: 48px 0 76px; background: #f6f8fc; }
.workspace-card { border: 1px solid var(--lp-border); border-radius: 18px; background: #fff; box-shadow: var(--lp-shadow); padding: 10px 24px 24px; }
.toolbar { display: grid; max-width: 760px; align-items: center; gap: 12px; margin: 8px 0 20px; grid-template-columns: minmax(220px, 1fr) 180px auto; }
.user-toolbar, .exam-toolbar { display: grid; align-items: center; gap: 12px; margin: 8px 0 20px; grid-template-columns: minmax(240px, 1fr) 170px 170px auto; }
.order-toolbar { display: grid; align-items: center; gap: 12px; margin: 18px 0 20px; grid-template-columns: minmax(220px, 1fr) 150px 180px auto; }
.audit-toolbar { display: grid; align-items: center; gap: 12px; margin: 8px 0 16px; grid-template-columns: 150px minmax(150px, 1fr) 140px minmax(220px, 1fr) auto; }
.audit-toolbar + .el-alert { margin-bottom: 18px; }
.el-pagination { justify-content: flex-end; margin-top: 22px; }
.table-subtitle { margin-top: 4px; color: var(--lp-text-secondary); font-size: 12px; }
.role-tag { margin: 2px 6px 2px 0; }
.dialog-description { color: var(--lp-text-secondary); line-height: 1.7; }
.role-options { display: flex; flex-direction: column; gap: 8px; margin-top: 18px; }
.category-actions { display: flex; align-items: center; justify-content: space-between; margin: 8px 0 20px; }
.category-actions p { margin: 0; color: var(--lp-text-secondary); font-size: 13px; }
.preview-content { min-height: 260px; }
.preview-content h2 { margin: 16px 0 8px; font-size: 26px; }
.preview-content h3 { margin: 26px 0 10px; font-size: 16px; }
.summary { color: var(--lp-text-secondary); line-height: 1.7; }
.article-body { border-radius: 12px; color: #344054; background: #f8faff; line-height: 1.8; padding: 18px; white-space: pre-wrap; }
.preview-file { display: flex; border-bottom: 1px solid var(--lp-border); align-items: center; justify-content: space-between; padding: 12px 2px; }
.preview-file span { color: var(--lp-text-secondary); font-size: 12px; }
.payment-record { display: flex; border: 1px solid var(--lp-border); border-radius: 10px; flex-direction: column; gap: 5px; margin-top: 9px; padding: 12px; }
.payment-record span { color: var(--lp-text-secondary); font-size: 12px; }
.category-form-row { display: grid; gap: 20px; grid-template-columns: 1fr 1fr; }
@media (max-width: 640px) {
  .workspace-card { padding: 8px 12px 18px; }
  .toolbar { grid-template-columns: 1fr; }
  .user-toolbar, .exam-toolbar { grid-template-columns: 1fr; }
  .order-toolbar { grid-template-columns: 1fr; }
  .audit-toolbar { grid-template-columns: 1fr; }
  .category-actions { align-items: stretch; flex-direction: column; gap: 12px; }
}
</style>
