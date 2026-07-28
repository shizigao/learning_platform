<script setup lang="ts">
import { ElMessage, ElMessageBox } from 'element-plus'
import { onMounted, reactive, ref } from 'vue'

import {
  activateTeacherProfile,
  approveTeacherApplication,
  getAdminTeacherApplication,
  getAdminTeacherProfileByUser,
  listTeacherApplications,
  rejectTeacherApplication,
  suspendTeacherProfile,
} from '@/api/offline-teaching'
import SectionPageHeader from '@/components/SectionPageHeader.vue'
import type {
  EducationLevel,
  TeacherApplication,
  TeacherApplicationStatus,
  TeacherApplicationSummary,
  TeacherGender,
  TeacherProfile,
} from '@/types/offline-teaching'

const loading = ref(false)
const applications = ref<TeacherApplicationSummary[]>([])
const total = ref(0)
const filters = reactive({
  keyword: '',
  status: 'PENDING' as TeacherApplicationStatus | undefined,
  pageNumber: 1,
  pageSize: 10,
})
const drawerVisible = ref(false)
const detailLoading = ref(false)
const detail = ref<TeacherApplication>()
const profile = ref<TeacherProfile>()

const statusLabels: Record<TeacherApplicationStatus, string> = {
  DRAFT: '草稿',
  PENDING: '待审核',
  APPROVED: '已通过',
  REJECTED: '已驳回',
  WITHDRAWN: '已撤回',
}
const educationLabels: Record<EducationLevel, string> = {
  HIGH_SCHOOL: '高中/中专',
  ASSOCIATE: '专科',
  BACHELOR: '本科',
  MASTER: '硕士',
  DOCTOR: '博士',
  OTHER: '其他',
}
const genderLabels: Record<TeacherGender, string> = {
  UNKNOWN: '不公开',
  MALE: '男',
  FEMALE: '女',
}

function dateTime(value?: string): string {
  return value ? new Date(value).toLocaleString('zh-CN') : '—'
}

function errorMessage(error: unknown, fallback: string): string {
  return error instanceof Error ? error.message : fallback
}

async function load(): Promise<void> {
  loading.value = true
  try {
    const page = await listTeacherApplications({
      ...filters,
      keyword: filters.keyword.trim() || undefined,
    })
    applications.value = page.items
    total.value = page.total
  } catch (error) {
    ElMessage.error(errorMessage(error, '教师申请列表加载失败'))
  } finally {
    loading.value = false
  }
}

function search(): void {
  filters.pageNumber = 1
  void load()
}

async function loadProfile(userId: number): Promise<void> {
  profile.value = undefined
  try {
    profile.value = await getAdminTeacherProfileByUser(userId)
  } catch {
    profile.value = undefined
  }
}

async function open(applicationId: number): Promise<void> {
  drawerVisible.value = true
  detailLoading.value = true
  detail.value = undefined
  profile.value = undefined
  try {
    detail.value = await getAdminTeacherApplication(applicationId)
    await loadProfile(detail.value.userId)
  } catch (error) {
    ElMessage.error(errorMessage(error, '教师申请详情加载失败'))
  } finally {
    detailLoading.value = false
  }
}

async function approve(): Promise<void> {
  if (!detail.value) return
  try {
    await ElMessageBox.confirm(
      `确认通过“${detail.value.teacherName}”的线下教师申请吗？通过后资料将公开展示。`,
      '通过教师申请',
      { type: 'warning', confirmButtonText: '确认通过' },
    )
    detail.value = await approveTeacherApplication(detail.value.id)
    await loadProfile(detail.value.userId)
    ElMessage.success('教师申请已通过并生成公开资料')
    await load()
  } catch (error) {
    if (error !== 'cancel') ElMessage.error(errorMessage(error, '审核失败'))
  }
}

async function reject(): Promise<void> {
  if (!detail.value) return
  try {
    const { value } = await ElMessageBox.prompt(
      '请填写明确的驳回原因，申请人将看到此内容。',
      '驳回教师申请',
      {
        type: 'warning',
        inputType: 'textarea',
        inputValidator: (text) => Boolean(text?.trim()) || '驳回原因不能为空',
      },
    )
    detail.value = await rejectTeacherApplication(detail.value.id, value.trim())
    ElMessage.success('教师申请已驳回')
    await load()
  } catch (error) {
    if (error !== 'cancel') ElMessage.error(errorMessage(error, '审核失败'))
  }
}

async function suspendProfile(): Promise<void> {
  if (!profile.value) return
  try {
    const { value } = await ElMessageBox.prompt(
      '暂停后该教师不会出现在寻找教师和AI推荐中，请填写原因。',
      '暂停教师展示',
      {
        type: 'warning',
        inputType: 'textarea',
        inputValidator: (text) => Boolean(text?.trim()) || '暂停原因不能为空',
      },
    )
    profile.value = await suspendTeacherProfile(profile.value.id, value.trim())
    ElMessage.success('教师公开资料已暂停展示')
  } catch (error) {
    if (error !== 'cancel') ElMessage.error(errorMessage(error, '操作失败'))
  }
}

async function activateProfile(): Promise<void> {
  if (!profile.value) return
  try {
    await ElMessageBox.confirm('确认恢复该教师的公开展示吗？', '恢复教师展示')
    profile.value = await activateTeacherProfile(profile.value.id)
    ElMessage.success('教师公开资料已恢复')
  } catch (error) {
    if (error !== 'cancel') ElMessage.error(errorMessage(error, '操作失败'))
  }
}

onMounted(load)
</script>

<template>
  <section class="admin-teacher-page">
    <div class="page-container">
      <SectionPageHeader
        eyebrow="TEACHER REVIEW"
        title="线下教师审核"
        description="核验发布者与管理员提交的教师身份和教学信息，并管理公开教师资料的展示状态。"
      >
        <RouterLink to="/admin"><el-button>返回管理后台</el-button></RouterLink>
      </SectionPageHeader>

      <div class="workspace">
        <div class="toolbar">
          <el-input
            v-model="filters.keyword"
            clearable
            placeholder="申请人账号、教师姓名或机构"
            @keyup.enter="search"
          />
          <el-select v-model="filters.status" clearable placeholder="全部申请状态">
            <el-option
              v-for="(label, value) in statusLabels"
              :key="value"
              :label="label"
              :value="value"
            />
          </el-select>
          <el-button type="primary" @click="search">查询</el-button>
        </div>
        <!-- <el-alert
          title="身份证号属于敏感信息"
          description="只在审核详情中查看，不得复制到日志、截图或其他系统；页面关闭后不再保留。"
          type="warning"
          show-icon
          :closable="false"
        /> -->
        <el-table v-loading="loading" :data="applications">
          <el-table-column label="申请人" min-width="180">
            <template #default="{ row }">
              <strong>{{ row.teacherName }}</strong>
              <div class="subtitle">{{ row.nickname }}（@{{ row.username }}）</div>
            </template>
          </el-table-column>
          <el-table-column label="身份信息" min-width="170">
            <template #default="{ row }">
              {{ row.idCardMasked }}
              <div class="subtitle">用户 ID：{{ row.userId }}</div>
            </template>
          </el-table-column>
          <el-table-column label="地区/机构" min-width="210">
            <template #default="{ row }">
              {{ row.province }} {{ row.city }}
              <div class="subtitle">{{ row.institution || '个人教师' }}</div>
            </template>
          </el-table-column>
          <el-table-column label="状态" width="110">
            <template #default="{ row }">
              <el-tag
                :type="
                  row.status === 'APPROVED'
                    ? 'success'
                    : row.status === 'REJECTED'
                      ? 'danger'
                      : row.status === 'PENDING'
                        ? 'warning'
                        : 'info'
                "
              >
                {{ statusLabels[row.status as TeacherApplicationStatus] }}
              </el-tag>
            </template>
          </el-table-column>
          <el-table-column label="提交时间" min-width="180">
            <template #default="{ row }">{{ dateTime(row.submittedAt) }}</template>
          </el-table-column>
          <el-table-column label="操作" width="100" fixed="right">
            <template #default="{ row }">
              <el-button link type="primary" @click="open(row.id)">查看审核</el-button>
            </template>
          </el-table-column>
          <template #empty><el-empty description="暂无符合条件的教师申请" /></template>
        </el-table>
        <el-pagination
          v-model:current-page="filters.pageNumber"
          background
          layout="total, prev, pager, next"
          :page-size="filters.pageSize"
          :total="total"
          @current-change="load"
        />
      </div>
    </div>

    <el-drawer v-model="drawerVisible" title="教师申请审核详情" size="680px">
      <div v-loading="detailLoading" class="detail">
        <template v-if="detail">
          <div class="detail-heading">
            <div>
              <h2>{{ detail.teacherName }}</h2>
              <el-tag>{{ statusLabels[detail.status] }}</el-tag>
            </div>
            <RouterLink :to="`/users/${detail.userId}`">
              <el-button type="primary" plain>进入申请人主页</el-button>
            </RouterLink>
          </div>
          <el-alert
            v-if="detail.status === 'REJECTED'"
            :title="`驳回原因：${detail.rejectionReason}`"
            type="error"
            show-icon
            :closable="false"
          />
          <el-descriptions :column="2" border>
            <el-descriptions-item label="身份证号" :span="2">
              <strong class="sensitive">{{ detail.idCardNumber }}</strong>
            </el-descriptions-item>
            <el-descriptions-item label="性别">
              {{ genderLabels[detail.gender] }}
            </el-descriptions-item>
            <el-descriptions-item label="学历">
              {{ educationLabels[detail.educationLevel] }}
            </el-descriptions-item>
            <el-descriptions-item label="所在地区" :span="2">
              {{ detail.province }} {{ detail.city }} {{ detail.district || '' }}
            </el-descriptions-item>
            <el-descriptions-item label="所属机构">
              {{ detail.institution || '个人教师' }}
            </el-descriptions-item>
            <el-descriptions-item label="课时价格">
              ¥{{ Number(detail.hourlyRate).toFixed(2) }}
            </el-descriptions-item>
            <el-descriptions-item label="微信">
              {{ detail.contactWechat || '—' }}
            </el-descriptions-item>
            <el-descriptions-item label="QQ">{{ detail.contactQq || '—' }}</el-descriptions-item>
            <el-descriptions-item label="邮箱" :span="2">
              {{ detail.contactEmail || '—' }}
            </el-descriptions-item>
          </el-descriptions>
          <h3>教育背景</h3><p>{{ detail.educationBackground }}</p>
          <h3>教授内容</h3><p>{{ detail.teachingContent }}</p>
          <h3>可上课时间</h3><p>{{ detail.availability || '未填写' }}</p>
          <h3>个人简介</h3><p>{{ detail.bio }}</p>
          <div class="tags">
            <el-tag v-for="tag in detail.teachingTags" :key="tag">{{ tag }}</el-tag>
          </div>

          <el-card v-if="profile" class="profile-status" shadow="never">
            <template #header><strong>当前公开教师资料</strong></template>
            <el-tag :type="profile.status === 'ACTIVE' ? 'success' : 'danger'">
              {{ profile.status === 'ACTIVE' ? '公开展示中' : '已暂停展示' }}
            </el-tag>
            <p v-if="profile.suspendedReason">暂停原因：{{ profile.suspendedReason }}</p>
            <el-button
              v-if="profile.status === 'ACTIVE'"
              type="danger"
              plain
              @click="suspendProfile"
            >
              暂停展示
            </el-button>
            <el-button v-else type="success" plain @click="activateProfile">恢复展示</el-button>
          </el-card>
        </template>
      </div>
      <template #footer>
        <el-button @click="drawerVisible = false">关闭</el-button>
        <template v-if="detail?.status === 'PENDING'">
          <el-button type="danger" plain @click="reject">驳回</el-button>
          <el-button type="success" @click="approve">审核通过</el-button>
        </template>
      </template>
    </el-drawer>
  </section>
</template>

<style scoped>
.admin-teacher-page { min-height: calc(100vh - 145px); padding: 48px 0 76px; background: #f6f8fc; }
.workspace { border: 1px solid var(--lp-border); border-radius: 18px; background: #fff; box-shadow: var(--lp-shadow); padding: 24px; }
.toolbar { display: grid; max-width: 760px; gap: 12px; margin-bottom: 16px; grid-template-columns: minmax(260px, 1fr) 180px auto; }.toolbar + .el-alert { margin-bottom: 18px; }
.subtitle { margin-top: 5px; color: var(--lp-text-secondary); font-size: 12px; }.el-pagination { justify-content: flex-end; margin-top: 22px; }
.detail { min-height: 300px; }.detail-heading { display: flex; align-items: flex-start; justify-content: space-between; margin-bottom: 20px; }.detail-heading h2 { margin: 0 0 9px; }.detail h3 { margin: 25px 0 8px; }.detail p { color: var(--lp-text-secondary); line-height: 1.75; white-space: pre-wrap; }.sensitive { color: #d92d20; letter-spacing: .08em; }.tags { display: flex; flex-wrap: wrap; gap: 7px; margin-top: 14px; }.profile-status { margin-top: 24px; }.profile-status p { margin: 10px 0; }
@media (max-width: 640px) { .toolbar { grid-template-columns: 1fr; }.workspace { padding: 16px; } }
</style>
