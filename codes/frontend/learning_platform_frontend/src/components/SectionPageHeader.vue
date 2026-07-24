<script setup lang="ts">
defineProps<{ eyebrow: string; title: string; description: string }>()
</script>

<template>
  <header class="section-header">
    <div>
      <p>{{ eyebrow }}</p>
      <h1>{{ title }}</h1>
      <span>{{ description }}</span>
    </div>
    <div class="actions"><slot /></div>
  </header>
</template>

<style scoped>
.section-header { display: flex; align-items: flex-end; justify-content: space-between; gap: 24px; margin-bottom: 28px; }
p { margin: 0 0 8px; color: var(--lp-primary); font-size: 12px; font-weight: 800; letter-spacing: .14em; }
h1 { margin: 0; font-size: clamp(30px, 4vw, 40px); letter-spacing: -.03em; }
span { display: block; margin-top: 10px; color: var(--lp-text-secondary); line-height: 1.65; }
.actions { display: flex; flex-wrap: wrap; gap: 10px; }
@media (max-width: 640px) { .section-header { align-items: stretch; flex-direction: column; } }
</style>
