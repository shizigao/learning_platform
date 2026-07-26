<script setup lang="ts">
import { ElMessage } from 'element-plus'
import { computed, ref } from 'vue'

import { listManagedClasses } from '@/api/classroom'
import type { Classroom } from '@/types/classroom'

const props = withDefaults(
  defineProps<{
    modelValue: number[]
    disabled?: boolean
  }>(),
  { disabled: false },
)
const emit = defineEmits<{ 'update:modelValue': [value: number[]] }>()

const visible = ref(false)
const loading = ref(false)
const loaded = ref(false)
const classes = ref<Classroom[]>([])
const keyword = ref('')
const draft = ref<number[]>([])

const selectedClasses = computed(() =>
  props.modelValue
    .map((id) => classes.value.find((item) => item.id === id))
    .filter((item): item is Classroom => Boolean(item)),
)
const filteredClasses = computed(() => {
  const value = keyword.value.trim().toLowerCase()
  if (!value) return classes.value
  return classes.value.filter(
    (item) =>
      item.name.toLowerCase().includes(value) ||
      (item.description ?? '').toLowerCase().includes(value),
  )
})

async function load(): Promise<void> {
  loading.value = true
  try {
    classes.value = await listManagedClasses()
    loaded.value = true
  } catch (error) {
    ElMessage.error(error instanceof Error ? error.message : '班级加载失败')
  } finally {
    loading.value = false
  }
}

async function open(): Promise<void> {
  if (props.disabled) return
  draft.value = [...props.modelValue]
  keyword.value = ''
  visible.value = true
  if (!loaded.value) await load()
}

function confirm(): void {
  emit('update:modelValue', [...draft.value])
  visible.value = false
}

function remove(classId: number): void {
  emit('update:modelValue', props.modelValue.filter((id) => id !== classId))
}
</script>

<template>
  <div class="class-picker">
    <button type="button" class="picker-trigger" :disabled="disabled" @click="open">
      <span v-if="modelValue.length">已选择 {{ modelValue.length }} 个班级</span>
      <span v-else>请选择发放班级</span>
      <strong>选择</strong>
    </button>
    <div v-if="selectedClasses.length" class="selected-tags">
      <el-tag
        v-for="item in selectedClasses"
        :key="item.id"
        :closable="!disabled"
        @close="remove(item.id)"
      >
        {{ item.name }}
      </el-tag>
    </div>

    <el-dialog v-model="visible" title="选择班级" width="min(760px, 94vw)" append-to-body>
      <el-input v-model="keyword" clearable placeholder="搜索班级名称或介绍" />
      <div v-loading="loading" class="class-list">
        <el-checkbox-group v-model="draft">
          <div v-for="item in filteredClasses" :key="item.id" class="class-option">
            <el-checkbox :value="item.id" class="class-choice">
              <span class="class-name">{{ item.name || `班级 #${item.id}` }}</span>
              <span class="class-description">{{ item.description || '暂无班级介绍' }}</span>
            </el-checkbox>
            <el-tag size="small">{{ item.currentRole === 'OWNER' ? '拥有者' : '管理员' }}</el-tag>
          </div>
        </el-checkbox-group>
        <el-empty
          v-if="!loading && filteredClasses.length === 0"
          description="没有可管理的班级"
        />
      </div>
      <template #footer>
        <el-button @click="visible = false">取消</el-button>
        <el-button type="primary" @click="confirm">确认选择</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<style scoped>
.picker-trigger { display: flex; width: 100%; min-height: 40px; border: 1px solid var(--lp-border); border-radius: 8px; align-items: center; justify-content: space-between; background: #fff; cursor: pointer; padding: 0 12px; text-align: left; }.picker-trigger:hover { border-color: var(--lp-primary); }.picker-trigger:disabled { cursor: not-allowed; opacity: .6; }.picker-trigger span { color: var(--lp-text-secondary); }.picker-trigger strong { color: var(--lp-primary); }.selected-tags { display: flex; flex-wrap: wrap; gap: 7px; margin-top: 9px; }.class-list { overflow-y: auto; max-height: 440px; margin-top: 16px; }.class-list :deep(.el-checkbox-group) { width: 100%; }.class-option { display: grid; min-height: 68px; border-bottom: 1px solid var(--lp-border); align-items: center; gap: 12px; grid-template-columns: minmax(0, 1fr) auto; padding: 10px 8px; }.class-option:hover { background: #f8faff; }.class-choice { width: 100%; min-width: 0; margin-right: 0; }.class-choice :deep(.el-checkbox__label) { display: flex; min-width: 0; flex: 1; flex-direction: column; align-items: flex-start; padding-left: 12px; }.class-name, .class-description { display: block !important; overflow: hidden; width: 100%; opacity: 1 !important; text-overflow: ellipsis; visibility: visible !important; white-space: nowrap; }.class-name { color: #101828 !important; font-size: 15px; font-weight: 700; line-height: 22px; }.class-description { margin-top: 3px; color: #667085 !important; font-size: 12px; line-height: 18px; }
</style>
