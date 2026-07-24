<template>
  <nav class="exam-nav" aria-label="发布者考试模块">
    <RouterLink to="/publisher/questions">题库</RouterLink>
    <RouterLink to="/publisher/papers">固定试卷</RouterLink>
    <RouterLink to="/publisher/exams">考试管理</RouterLink>
    <RouterLink to="/publisher">学习资料</RouterLink>
  </nav>
</template>

<style scoped>
.exam-nav { display: flex; flex-wrap: wrap; gap: 8px; margin-bottom: 24px; }
.exam-nav a { border: 1px solid var(--lp-border); border-radius: 9px; color: var(--lp-text-secondary); background: #fff; font-size: 13px; font-weight: 700; padding: 9px 14px; }
.exam-nav a:hover, .exam-nav a.router-link-exact-active { border-color: #b9d2ff; color: var(--lp-primary); background: #eff6ff; }
</style>
