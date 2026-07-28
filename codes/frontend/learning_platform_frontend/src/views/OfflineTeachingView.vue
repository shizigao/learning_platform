<script setup lang="ts">
import { ElMessage, ElMessageBox, type FormInstance, type FormRules } from 'element-plus'
import { computed, onMounted, reactive, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'

import {
  getStudentPreference,
  getTeacher,
  getTeacherApplication,
  recommendTeachers,
  saveTeacherApplication,
  searchTeachers,
  submitTeacherApplication,
} from '@/api/offline-teaching'
import SectionPageHeader from '@/components/SectionPageHeader.vue'
import { useAuthStore } from '@/stores/auth'
import type {
  EducationLevel,
  StudentPreference,
  TeacherApplication,
  TeacherApplicationPayload,
  TeacherApplicationStatus,
  TeacherGender,
  TeacherProfile,
  TeacherRecommendation,
} from '@/types/offline-teaching'

const router = useRouter()
const route = useRoute()
const authStore = useAuthStore()
const canApply = computed(() => authStore.hasRole('PUBLISHER', 'ADMIN'))
const activeTab = ref('find')
const loading = ref(false)
const teachers = ref<TeacherProfile[]>([])
const total = ref(0)
const filters = reactive({
  keyword: '',
  province: '',
  city: '',
  teachingTag: '',
  maxHourlyRate: undefined as number | undefined,
  pageNumber: 1,
  pageSize: 9,
})
const detailVisible = ref(false)
const detail = ref<TeacherProfile>()
const applicationLoading = ref(false)
const applicationSaving = ref(false)
const applicationFormRef = ref<FormInstance>()
const application = ref<TeacherApplication | null>(null)
const applicationForm = reactive<TeacherApplicationPayload>({
  teacherName: '',
  idCardNumber: '',
  gender: 'UNKNOWN',
  educationLevel: 'BACHELOR',
  educationBackground: '',
  institution: '',
  province: '',
  city: '',
  district: '',
  bio: '',
  teachingContent: '',
  teachingTags: [],
  availability: '',
  hourlyRate: 1,
  priceDescription: '',
  contactWechat: '',
  contactQq: '',
  contactEmail: '',
})
const recommendationVisible = ref(false)
const recommendationLoading = ref(false)
const recommendation = ref<TeacherRecommendation>()
const preference = reactive<StudentPreference>({
  subject: '',
  currentLevel: '',
  learningGoals: '',
  weaknesses: '',
  province: '',
  city: '',
  district: '',
  maxHourlyRate: undefined,
  availability: '',
  teacherPreferences: '',
  additionalNotes: '',
})

const statusLabels: Record<TeacherApplicationStatus, string> = {
  DRAFT: '草稿',
  PENDING: '审核中',
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
const applicationLocked = computed(() => application.value?.status === 'PENDING')
const idCardPattern =
  /(^[1-9]\d{5}(18|19|20)\d{2}(0[1-9]|1[0-2])([0-2][1-9]|10|20|30|31)\d{3}[0-9Xx]$)|(^[1-9]\d{7}(0[1-9]|1[0-2])([0-2][1-9]|10|20|30|31)\d{3}$)/
const qqPattern = /^[1-9][0-9]{4,11}$/
const emailPattern = /^[^\s@]+@[^\s@]+\.[^\s@]+$/
const applicationRules: FormRules<TeacherApplicationPayload> = {
  teacherName: [
    { required: true, whitespace: true, message: '请输入教师姓名', trigger: 'blur' },
    { max: 64, message: '教师姓名不能超过64个字符', trigger: 'blur' },
  ],
  idCardNumber: [
    { required: true, whitespace: true, message: '请输入身份证号', trigger: 'blur' },
    { pattern: idCardPattern, message: '身份证号格式不正确', trigger: 'blur' },
  ],
  gender: [{ required: true, message: '请选择性别', trigger: 'change' }],
  educationLevel: [{ required: true, message: '请选择最高学历', trigger: 'change' }],
  educationBackground: [
    { required: true, whitespace: true, message: '请输入教育背景', trigger: 'blur' },
    { max: 1000, message: '教育背景不能超过1000个字符', trigger: 'blur' },
  ],
  institution: [{ max: 200, message: '所属机构不能超过200个字符', trigger: 'blur' }],
  province: [
    { required: true, whitespace: true, message: '请输入所在省份', trigger: 'blur' },
    { max: 100, message: '省份不能超过100个字符', trigger: 'blur' },
  ],
  city: [
    { required: true, whitespace: true, message: '请输入所在城市', trigger: 'blur' },
    { max: 100, message: '城市不能超过100个字符', trigger: 'blur' },
  ],
  district: [{ max: 100, message: '区/县不能超过100个字符', trigger: 'blur' }],
  bio: [
    { required: true, whitespace: true, message: '请输入个人简介', trigger: 'blur' },
    { max: 2000, message: '个人简介不能超过2000个字符', trigger: 'blur' },
  ],
  teachingContent: [
    { required: true, whitespace: true, message: '请输入教授内容', trigger: 'blur' },
    { max: 2000, message: '教授内容不能超过2000个字符', trigger: 'blur' },
  ],
  teachingTags: [
    {
      validator: (_rule, value: string[], callback) => {
        const tags = (value ?? []).map((tag) => tag.trim())
        if (tags.length === 0) callback(new Error('请至少添加一个教学标签'))
        else if (tags.length > 20) callback(new Error('教学标签不能超过20项'))
        else if (tags.some((tag) => !tag || tag.length > 50)) {
          callback(new Error('每个教学标签须为1至50个字符'))
        } else callback()
      },
      trigger: 'change',
    },
  ],
  availability: [
    { required: true, whitespace: true, message: '请输入可上课时间', trigger: 'blur' },
    { max: 1000, message: '可上课时间不能超过1000个字符', trigger: 'blur' },
  ],
  hourlyRate: [
    { required: true, message: '请输入参考课时价格', trigger: 'change' },
    {
      validator: (_rule, value: number, callback) => {
        if (!Number.isFinite(value) || value < 0.01) {
          callback(new Error('参考课时价格不能低于0.01元'))
        } else callback()
      },
      trigger: 'change',
    },
  ],
  priceDescription: [{ max: 500, message: '价格说明不能超过500个字符', trigger: 'blur' }],
  contactWechat: [
    {
      validator: (_rule, _value: string, callback) => {
        if (
          !applicationForm.contactWechat?.trim() &&
          !applicationForm.contactQq?.trim() &&
          !applicationForm.contactEmail?.trim()
        ) {
          callback(new Error('微信、QQ、邮箱至少填写一项'))
        } else callback()
      },
      trigger: 'change',
    },
  ],
  contactQq: [
    {
      validator: (_rule, value: string, callback) => {
        if (value?.trim() && !qqPattern.test(value.trim())) {
          callback(new Error('QQ号应为5至12位数字，且不能以0开头'))
        } else callback()
      },
      trigger: 'blur',
    },
  ],
  contactEmail: [
    {
      validator: (_rule, value: string, callback) => {
        if (value?.trim() && !emailPattern.test(value.trim())) {
          callback(new Error('邮箱格式不正确'))
        } else callback()
      },
      trigger: 'blur',
    },
    { max: 128, message: '邮箱不能超过128个字符', trigger: 'blur' },
  ],
}

function errorMessage(error: unknown, fallback: string): string {
  return error instanceof Error ? error.message : fallback
}

async function loadTeachers(): Promise<void> {
  loading.value = true
  try {
    const page = await searchTeachers({
      ...filters,
      keyword: filters.keyword.trim() || undefined,
      province: filters.province.trim() || undefined,
      city: filters.city.trim() || undefined,
      teachingTag: filters.teachingTag.trim() || undefined,
    })
    teachers.value = page.items
    total.value = page.total
  } catch (error) {
    ElMessage.error(errorMessage(error, '教师列表加载失败'))
  } finally {
    loading.value = false
  }
}

function search(): void {
  filters.pageNumber = 1
  void loadTeachers()
}

function openTeacher(teacher: TeacherProfile): void {
  detail.value = teacher
  detailVisible.value = true
}

async function openTeacherFromQuery(value: unknown): Promise<void> {
  const teacherId = Number(Array.isArray(value) ? value[0] : value)
  if (!Number.isSafeInteger(teacherId) || teacherId <= 0) return
  try {
    openTeacher(await getTeacher(teacherId))
  } catch (error) {
    ElMessage.error(errorMessage(error, '教师信息加载失败'))
  }
}

async function openRecommendation(): Promise<void> {
  recommendation.value = undefined
  recommendationVisible.value = true
  try {
    const saved = await getStudentPreference()
    if (saved) Object.assign(preference, saved)
    else {
      preference.province = filters.province
      preference.city = filters.city
      preference.subject = filters.teachingTag || filters.keyword
      preference.maxHourlyRate = filters.maxHourlyRate
    }
  } catch (error) {
    ElMessage.error(errorMessage(error, '学习需求加载失败'))
  }
}

function validPreference(): boolean {
  if (
    !preference.subject.trim() ||
    !preference.currentLevel.trim() ||
    !preference.learningGoals.trim() ||
    !preference.province.trim() ||
    !preference.city.trim()
  ) {
    ElMessage.warning('请完整填写科目、当前水平、学习目标和所在地区')
    return false
  }
  return true
}

function requestId(): string {
  const suffix =
    typeof crypto.randomUUID === 'function'
      ? crypto.randomUUID().replaceAll('-', '')
      : `${Date.now()}${Math.random().toString(36).slice(2)}`
  return `teacher-${suffix}`.slice(0, 64)
}

// AI生成推荐
async function generateRecommendation(): Promise<void> {
  if (!validPreference()) return
  recommendationLoading.value = true
  try {
    //等待后端返回推荐教师的列表，点击recommendTeachers
    recommendation.value = await recommendTeachers(requestId(), {
      ...preference,
      subject: preference.subject.trim(),
      currentLevel: preference.currentLevel.trim(),
      learningGoals: preference.learningGoals.trim(),
      province: preference.province.trim(),
      city: preference.city.trim(),
    })
    const notify = recommendation.value.aiSucceeded ? ElMessage.success : ElMessage.warning
    notify({ message: recommendation.value.message, duration: 3500 })
  } catch (error) {
    ElMessage.error(errorMessage(error, 'AI 推荐请求失败'))
  } finally {
    recommendationLoading.value = false
  }
}

async function loadApplication(): Promise<void> {
  if (!canApply.value) return
  applicationLoading.value = true
  try {
    application.value = await getTeacherApplication()
    if (application.value) {
      Object.assign(applicationForm, application.value, { idCardNumber: '' })
      applicationForm.availability = application.value.availability ?? ''
    }
  } catch (error) {
    ElMessage.error(errorMessage(error, '教师信息加载失败'))
  } finally {
    applicationLoading.value = false
  }
}

async function saveApplication(): Promise<void> {
  try {
    await applicationFormRef.value?.validate()
  } catch {
    ElMessage.warning('请按表单中的红色提示修正教师信息')
    return
  }
  applicationSaving.value = true
  try {
    application.value = await saveTeacherApplication({
      ...applicationForm,
      teachingTags: applicationForm.teachingTags.map((tag) => tag.trim()).filter(Boolean),
    })
    applicationForm.idCardNumber = ''
    ElMessage.success('教师信息已保存为草稿')
  } catch (error) {
    ElMessage.error(errorMessage(error, '教师信息保存失败'))
  } finally {
    applicationSaving.value = false
  }
}

async function submitApplication(): Promise<void> {
  if (application.value?.status !== 'DRAFT') {
    ElMessage.warning('请先保存教师信息草稿')
    return
  }
  try {
    await ElMessageBox.confirm(
      '提交后管理员将核验教师资料，审核期间不能修改。确认提交吗？',
      '提交教师申请',
      { type: 'warning' },
    )
    application.value = await submitTeacherApplication()
    ElMessage.success('教师申请已提交，请等待管理员审核')
  } catch (error) {
    if (error !== 'cancel') ElMessage.error(errorMessage(error, '提交失败'))
  }
}

onMounted(() => {
  void loadTeachers()
  void loadApplication()
  void openTeacherFromQuery(route.query.teacher)
})

watch(
  () => route.query.teacher,
  (value, previous) => {
    if (value !== previous) void openTeacherFromQuery(value)
  },
)
</script>

<template>
  <section class="offline-page">
    <div class="page-container">
      <SectionPageHeader
        eyebrow="OFFLINE TEACHING"
        title="线下教学"
        description="浏览经过平台审核的线下教师，联系教师协商课程；发布者和管理员也可申请成为教师。"
      />

      <div class="workspace">
        <el-tabs v-model="activeTab">
          <el-tab-pane label="寻找教师" name="find">
            <div class="search-panel">
              <el-input
                v-model="filters.keyword"
                clearable
                placeholder="教师姓名、账号、机构或教授内容"
                @keyup.enter="search"
              />
              <el-input v-model="filters.province" clearable placeholder="省份" />
              <el-input v-model="filters.city" clearable placeholder="城市" />
              <el-input v-model="filters.teachingTag" clearable placeholder="教学标签/科目" />
              <el-input-number
                v-model="filters.maxHourlyRate"
                :min="1"
                :controls="false"
                placeholder="最高课时价"
              />
              <el-button type="primary" @click="search">搜索</el-button>
              <el-button type="success" plain @click="openRecommendation">AI 推荐教师</el-button>
            </div>

            <div v-loading="loading" class="teacher-grid">
              <article
                v-for="teacher in teachers"
                :key="teacher.id"
                class="teacher-card"
                @click="openTeacher(teacher)"
              >
                <div class="teacher-heading">
                  <el-avatar :size="56" :src="teacher.avatarUrl">
                    {{ teacher.teacherName.slice(0, 1) }}
                  </el-avatar>
                  <div>
                    <h3>{{ teacher.teacherName }}</h3>
                    <p>@{{ teacher.username }} · {{ teacher.province }} {{ teacher.city }}</p>
                  </div>
                  <strong>¥{{ Number(teacher.hourlyRate).toFixed(2) }}<small>/课时</small></strong>
                </div>
                <p class="institution">{{ teacher.institution || '个人教师' }}</p>
                <p class="teaching-content">{{ teacher.teachingContent }}</p>
                <p class="availability">
                  可上课时间：{{ teacher.availability || '请联系教师确认' }}
                </p>
                <div class="tag-row">
                  <el-tag v-for="tag in teacher.teachingTags" :key="tag" size="small">
                    {{ tag }}
                  </el-tag>
                </div>
                <el-button link type="primary">查看教师详情与联系方式</el-button>
              </article>
            </div>
            <el-empty v-if="!loading && teachers.length === 0" description="暂无符合条件的教师" />
            <el-pagination
              v-if="total > filters.pageSize"
              v-model:current-page="filters.pageNumber"
              background
              layout="total, prev, pager, next"
              :page-size="filters.pageSize"
              :total="total"
              @current-change="loadTeachers"
            />
          </el-tab-pane>

          <el-tab-pane v-if="canApply" label="教师信息" name="application">
            <div v-loading="applicationLoading" class="application-panel">
              <el-alert
                v-if="application"
                :title="`当前申请状态：${statusLabels[application.status]}`"
                :description="
                  application.status === 'REJECTED'
                    ? `驳回原因：${application.rejectionReason || '未填写'}`
                    : application.status === 'APPROVED'
                      ? '教师信息已公开；修改并重新提交后需再次审核，原公开资料在复审前保持不变。'
                      : application.status === 'PENDING'
                        ? '管理员正在审核，审核完成前不能修改。'
                        : '草稿尚未提交审核。'
                "
                :type="
                  application.status === 'APPROVED'
                    ? 'success'
                    : application.status === 'REJECTED'
                      ? 'error'
                      : application.status === 'PENDING'
                        ? 'warning'
                        : 'info'
                "
                show-icon
                :closable="false"
              />
              <el-form
                ref="applicationFormRef"
                :model="applicationForm"
                :rules="applicationRules"
                label-position="top"
                :disabled="applicationLocked"
                scroll-to-error
              >
                <div class="form-grid">
                  <el-form-item label="教师姓名" prop="teacherName">
                    <el-input v-model="applicationForm.teacherName" maxlength="64" />
                  </el-form-item>
                  <el-form-item label="身份证号" prop="idCardNumber">
                    <el-input
                      v-model="applicationForm.idCardNumber"
                      maxlength="18"
                      :placeholder="
                        application?.idCardMasked
                          ? `已保存 ${application.idCardMasked}，修改时请重新输入`
                          : '仅管理员审核时可查看，数据库加密存储'
                      "
                      show-password
                    />
                  </el-form-item>
                  <el-form-item label="性别" prop="gender">
                    <el-select v-model="applicationForm.gender">
                      <el-option
                        v-for="(label, value) in genderLabels"
                        :key="value"
                        :label="label"
                        :value="value"
                      />
                    </el-select>
                  </el-form-item>
                  <el-form-item label="最高学历" prop="educationLevel">
                    <el-select v-model="applicationForm.educationLevel">
                      <el-option
                        v-for="(label, value) in educationLabels"
                        :key="value"
                        :label="label"
                        :value="value"
                      />
                    </el-select>
                  </el-form-item>
                  <el-form-item label="所属机构" prop="institution">
                    <el-input v-model="applicationForm.institution" maxlength="200" />
                  </el-form-item>
                  <el-form-item label="参考课时价格（元）" prop="hourlyRate">
                    <el-input-number
                      v-model="applicationForm.hourlyRate"
                      :min="0.01"
                      :precision="2"
                      :step="10"
                    />
                  </el-form-item>
                  <el-form-item label="省份" prop="province">
                    <el-input v-model="applicationForm.province" maxlength="100" />
                  </el-form-item>
                  <el-form-item label="城市" prop="city">
                    <el-input v-model="applicationForm.city" maxlength="100" />
                  </el-form-item>
                  <el-form-item label="区/县" prop="district">
                    <el-input v-model="applicationForm.district" maxlength="100" />
                  </el-form-item>
                  <el-form-item label="价格说明" prop="priceDescription">
                    <el-input v-model="applicationForm.priceDescription" maxlength="500" />
                  </el-form-item>
                </div>
                <el-form-item label="教育背景" prop="educationBackground">
                  <el-input
                    v-model="applicationForm.educationBackground"
                    type="textarea"
                    :rows="3"
                    maxlength="1000"
                    show-word-limit
                  />
                </el-form-item>
                <el-form-item label="个人简介" prop="bio">
                  <el-input
                    v-model="applicationForm.bio"
                    type="textarea"
                    :rows="4"
                    maxlength="2000"
                    show-word-limit
                  />
                </el-form-item>
                <el-form-item label="教授内容" prop="teachingContent">
                  <el-input
                    v-model="applicationForm.teachingContent"
                    type="textarea"
                    :rows="4"
                    maxlength="2000"
                    show-word-limit
                  />
                </el-form-item>
                <el-form-item label="教学标签" prop="teachingTags">
                  <el-select
                    v-model="applicationForm.teachingTags"
                    multiple
                    filterable
                    allow-create
                    default-first-option
                    placeholder="输入科目或方向后按回车，最多20项"
                  />
                </el-form-item>
                <el-form-item label="可上课时间" prop="availability">
                  <el-input
                    v-model="applicationForm.availability"
                    type="textarea"
                    :rows="3"
                    maxlength="1000"
                    show-word-limit
                    placeholder="例如：工作日19:00—21:00，周六、周日09:00—18:00"
                  />
                </el-form-item>
                <h3>联系方式（至少填写一项）</h3>
                <div class="form-grid three">
                  <el-form-item label="微信" prop="contactWechat">
                    <el-input
                      v-model="applicationForm.contactWechat"
                      maxlength="100"
                      @input="applicationFormRef?.validateField('contactWechat')"
                    />
                  </el-form-item>
                  <el-form-item label="QQ" prop="contactQq">
                    <el-input
                      v-model="applicationForm.contactQq"
                      maxlength="12"
                      @input="applicationFormRef?.validateField('contactWechat')"
                    />
                  </el-form-item>
                  <el-form-item label="邮箱" prop="contactEmail">
                    <el-input
                      v-model="applicationForm.contactEmail"
                      maxlength="128"
                      @input="applicationFormRef?.validateField('contactWechat')"
                    />
                  </el-form-item>
                </div>
                <div class="form-actions">
                  <el-button
                    type="primary"
                    :loading="applicationSaving"
                    @click="saveApplication"
                  >
                    保存草稿
                  </el-button>
                  <el-button
                    type="success"
                    :disabled="application?.status !== 'DRAFT'"
                    @click="submitApplication"
                  >
                    提交审核
                  </el-button>
                </div>
              </el-form>
            </div>
          </el-tab-pane>
        </el-tabs>
      </div>
    </div>

    <el-dialog v-model="detailVisible" title="线下教师详情" width="min(720px, 94vw)">
      <template v-if="detail">
        <div class="detail-identity">
          <el-avatar :size="72" :src="detail.avatarUrl">
            {{ detail.teacherName.slice(0, 1) }}
          </el-avatar>
          <div>
            <h2>{{ detail.teacherName }}</h2>
            <p>{{ detail.nickname }}（@{{ detail.username }}）</p>
          </div>
          <strong>¥{{ Number(detail.hourlyRate).toFixed(2) }}/课时</strong>
        </div>
        <el-descriptions :column="2" border>
          <el-descriptions-item label="学历">
            {{ educationLabels[detail.educationLevel] }}
          </el-descriptions-item>
          <el-descriptions-item label="所在地区">
            {{ detail.province }} {{ detail.city }} {{ detail.district || '' }}
          </el-descriptions-item>
          <el-descriptions-item label="所属机构">
            {{ detail.institution || '个人教师' }}
          </el-descriptions-item>
          <el-descriptions-item label="价格说明">
            {{ detail.priceDescription || '请联系教师协商' }}
          </el-descriptions-item>
          <el-descriptions-item label="可上课时间" :span="2">
            {{ detail.availability || '请联系教师确认' }}
          </el-descriptions-item>
          <el-descriptions-item label="微信">{{ detail.contactWechat || '—' }}</el-descriptions-item>
          <el-descriptions-item label="QQ">{{ detail.contactQq || '—' }}</el-descriptions-item>
          <el-descriptions-item label="邮箱" :span="2">
            {{ detail.contactEmail || '—' }}
          </el-descriptions-item>
        </el-descriptions>
        <h3>教育背景</h3><p class="detail-text">{{ detail.educationBackground }}</p>
        <h3>教授内容</h3><p class="detail-text">{{ detail.teachingContent }}</p>
        <h3>个人简介</h3><p class="detail-text">{{ detail.bio }}</p>
        <div class="tag-row">
          <el-tag v-for="tag in detail.teachingTags" :key="tag">{{ tag }}</el-tag>
        </div>
      </template>
      <template #footer>
        <el-button @click="detailVisible = false">关闭</el-button>
        <el-button
          v-if="detail"
          type="primary"
          @click="router.push(`/users/${detail.userId}`)"
        >
          进入主页
        </el-button>
      </template>
    </el-dialog>

    <el-dialog
      v-model="recommendationVisible"
      title="AI 推荐线下教师"
      width="min(860px, 95vw)"
      destroy-on-close
    >
      <el-alert
        title="AI 推荐仅用于辅助筛选"
        description="系统先按地区、教授内容和预算选出至多20名候选教师，再由AI推荐至多3名；最终请自行联系、核实并协商。"
        type="info"
        show-icon
        :closable="false"
      />
      <el-form label-position="top" class="preference-form">
        <div class="form-grid">
          <el-form-item label="想学习的科目或内容" required>
            <el-input v-model="preference.subject" maxlength="200" />
          </el-form-item>
          <el-form-item label="当前学习水平" required>
            <el-input v-model="preference.currentLevel" maxlength="500" />
          </el-form-item>
          <el-form-item label="省份" required>
            <el-input v-model="preference.province" maxlength="100" />
          </el-form-item>
          <el-form-item label="城市" required>
            <el-input v-model="preference.city" maxlength="100" />
          </el-form-item>
          <el-form-item label="区/县">
            <el-input v-model="preference.district" maxlength="100" />
          </el-form-item>
          <el-form-item label="最高课时预算">
            <el-input-number
              v-model="preference.maxHourlyRate"
              :min="0.01"
              :precision="2"
            />
          </el-form-item>
        </div>
        <el-form-item label="学习目标" required>
          <el-input v-model="preference.learningGoals" type="textarea" :rows="3" />
        </el-form-item>
        <el-form-item label="薄弱项">
          <el-input v-model="preference.weaknesses" type="textarea" :rows="2" />
        </el-form-item>
        <el-form-item label="可上课时间">
          <el-input v-model="preference.availability" />
        </el-form-item>
        <el-form-item label="对教师的偏好">
          <el-input v-model="preference.teacherPreferences" type="textarea" :rows="2" />
        </el-form-item>
      </el-form>
      <el-alert
        v-if="recommendation"
        :title="recommendation.message"
        :type="recommendation.aiSucceeded ? 'success' : 'warning'"
        show-icon
        :closable="false"
      />
      <div v-if="recommendation" class="recommendation-list">
        <article
          v-for="item in recommendation.recommendations"
          :key="item.teacher.id"
          class="recommendation-item"
        >
          <el-avatar :size="48" :src="item.teacher.avatarUrl">
            {{ item.teacher.teacherName.slice(0, 1) }}
          </el-avatar>
          <div>
            <h3>{{ item.teacher.teacherName }}</h3>
            <p>{{ item.reason }}</p>
            <div class="tag-row">
              <el-tag v-for="tag in item.matchHighlights" :key="tag" type="success">
                {{ tag }}
              </el-tag>
            </div>
          </div>
          <el-button link type="primary" @click="openTeacher(item.teacher)">查看详情</el-button>
        </article>
      </div>
      <template #footer>
        <el-button @click="recommendationVisible = false">关闭</el-button>
        <el-button
          type="primary"
          :loading="recommendationLoading"
          @click="generateRecommendation"
        >
          {{ recommendationLoading ? 'AI 正在匹配教师…' : 'AI 推荐教师（消耗1次额度）' }}
        </el-button>
      </template>
    </el-dialog>
  </section>
</template>

<style scoped>
.offline-page { min-height: calc(100vh - 145px); padding: 48px 0 76px; background: #f6f8fc; }
.workspace { border: 1px solid var(--lp-border); border-radius: 20px; background: #fff; box-shadow: var(--lp-shadow); padding: 10px 26px 30px; }
.search-panel { display: grid; align-items: center; gap: 12px; margin: 10px 0 24px; grid-template-columns: minmax(220px, 1.5fr) repeat(4, minmax(120px, .7fr)) auto auto; }
.teacher-grid { display: grid; min-height: 180px; gap: 18px; grid-template-columns: repeat(3, 1fr); }
.teacher-card { border: 1px solid var(--lp-border); border-radius: 18px; padding: 20px; cursor: pointer; transition: 160ms ease; }
.teacher-card:hover { border-color: #9ec5ff; box-shadow: 0 12px 30px rgb(37 99 235 / 10%); transform: translateY(-2px); }
.teacher-heading { display: grid; align-items: center; gap: 12px; grid-template-columns: auto 1fr auto; }.teacher-heading h3 { margin: 0; }.teacher-heading p { margin: 5px 0 0; color: var(--lp-text-secondary); font-size: 12px; }.teacher-heading strong { color: #f79009; font-size: 18px; }.teacher-heading small { color: var(--lp-text-secondary); font-size: 11px; }
.institution { color: var(--lp-primary); font-size: 13px; font-weight: 600; }.teaching-content { min-height: 52px; color: var(--lp-text-secondary); line-height: 1.6; display: -webkit-box; overflow: hidden; -webkit-box-orient: vertical; -webkit-line-clamp: 2; }
.availability { color: var(--lp-text-secondary); font-size: 13px; line-height: 1.5; }
.tag-row { display: flex; flex-wrap: wrap; gap: 6px; margin: 12px 0; }
.el-pagination { justify-content: center; margin-top: 28px; }
.application-panel { padding: 12px 4px; }.application-panel > .el-alert { margin-bottom: 24px; }.application-panel h3 { margin: 8px 0 16px; }
.form-grid { display: grid; gap: 0 22px; grid-template-columns: repeat(2, 1fr); }.form-grid.three { grid-template-columns: repeat(3, 1fr); }.form-actions { display: flex; justify-content: flex-end; gap: 10px; margin-top: 12px; }
.detail-identity { display: grid; align-items: center; gap: 16px; margin-bottom: 22px; grid-template-columns: auto 1fr auto; }.detail-identity h2 { margin: 0; }.detail-identity p { margin: 5px 0 0; color: var(--lp-text-secondary); }.detail-identity strong { color: #f79009; font-size: 18px; }.detail-text { color: var(--lp-text-secondary); line-height: 1.75; white-space: pre-wrap; }
.preference-form { margin-top: 18px; }.recommendation-list { display: flex; flex-direction: column; gap: 12px; margin-top: 16px; }.recommendation-item { display: grid; align-items: flex-start; gap: 14px; border: 1px solid var(--lp-border); border-radius: 14px; padding: 16px; grid-template-columns: auto 1fr auto; }.recommendation-item h3 { margin: 0; }.recommendation-item p { margin: 7px 0; color: var(--lp-text-secondary); line-height: 1.6; }
@media (max-width: 1080px) { .search-panel { grid-template-columns: repeat(3, 1fr); }.teacher-grid { grid-template-columns: repeat(2, 1fr); } }
@media (max-width: 700px) { .search-panel, .teacher-grid, .form-grid, .form-grid.three { grid-template-columns: 1fr; }.teacher-heading, .detail-identity, .recommendation-item { grid-template-columns: auto 1fr; }.teacher-heading > strong, .detail-identity > strong, .recommendation-item > .el-button { grid-column: 2; }.workspace { padding: 8px 14px 22px; } }
</style>
