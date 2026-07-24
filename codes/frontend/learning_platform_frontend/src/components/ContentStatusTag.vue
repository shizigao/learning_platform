<script setup lang="ts">
import { computed } from 'vue'

import type { ContentStatus } from '@/types/content'

const props = defineProps<{ status: ContentStatus }>()

const config = computed(() => {
  const map = {
    DRAFT: { label: '草稿', type: 'info' },
    PENDING_REVIEW: { label: '待审核', type: 'warning' },
    PUBLISHED: { label: '已发布', type: 'success' },
    REJECTED: { label: '已驳回', type: 'danger' },
    OFFLINE: { label: '已下架', type: 'info' },
  } as const
  return map[props.status]
})
</script>

<template>
  <el-tag :type="config.type" effect="light" round>{{ config.label }}</el-tag>
</template>
