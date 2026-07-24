<script setup lang="ts">
import zhCn from 'element-plus/es/locale/lang/zh-cn'

const messageConfig = {
  duration: 1400,
  offset: 84,
  max: 2,
  grouping: true,
}
</script>

<template>
  <el-config-provider :locale="zhCn" :message="messageConfig">
    <RouterView />
  </el-config-provider>
</template>
