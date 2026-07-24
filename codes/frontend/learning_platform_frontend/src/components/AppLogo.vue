<script setup lang="ts">
import { Reading } from '@element-plus/icons-vue'
</script>

<template>
  <span class="brand">
    <span class="brand-mark">
      <el-icon :size="22"><Reading /></el-icon>
    </span>
    <span class="brand-copy">
      <strong>智学云考</strong>
      <small>LEARNING PLATFORM</small>
    </span>
  </span>
</template>

<style scoped>
.brand {
  display: inline-flex;
  align-items: center;
  gap: 10px;
}

.brand-mark {
  display: grid;
  width: 42px;
  height: 42px;
  border-radius: 13px;
  color: #fff;
  background: linear-gradient(145deg, #3b82f6, #1d4ed8);
  box-shadow: 0 8px 20px rgb(37 99 235 / 26%);
  place-items: center;
}

.brand-copy {
  display: flex;
  flex-direction: column;
  line-height: 1.05;
}

.brand-copy strong {
  font-size: 18px;
  letter-spacing: 0.06em;
}

.brand-copy small {
  margin-top: 5px;
  color: #98a2b3;
  font-size: 8px;
  font-weight: 700;
  letter-spacing: 0.13em;
}
</style>

