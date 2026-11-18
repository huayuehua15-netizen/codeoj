<template>
  <div class="page-container">
    <header class="oj-stage">
      <div>
        <h1 class="oj-stage-title">公告中心</h1>
        <p class="oj-stage-sub">课程通知、竞赛安排与平台更新，都在这里</p>
      </div>
      <div class="oj-stage-side">
        <div class="oj-stage-stat">
          <span class="oj-stage-num">{{ announcements.length }}</span>
          <span class="oj-stage-label">则公告</span>
        </div>
      </div>
    </header>

    <div v-loading="loading">
      <article v-for="a in announcements" :key="a.id" class="card ann-card">
        <div class="ann-head">
          <span class="ann-seal" aria-hidden="true">告</span>
          <h2 class="ann-title">{{ a.title }}</h2>
          <span class="ann-date mono tabular">{{ formatDateTime(a.createdAt) }}</span>
        </div>
        <p class="ann-body">{{ a.content }}</p>
      </article>
      <el-empty v-if="!loading && !announcements.length" description="还没有公告" />
    </div>
  </div>
</template>

<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { listAnnouncements, type AnnouncementItem } from '@/api/announcement'
import { formatDateTime } from '@/utils/datetime'

const announcements = ref<AnnouncementItem[]>([])
const loading = ref(false)

onMounted(async () => {
  loading.value = true
  try {
    announcements.value = await listAnnouncements()
  } finally {
    loading.value = false
  }
})
</script>

<style scoped>
.ann-card {
  margin-bottom: 14px;
}
.ann-head {
  display: flex;
  align-items: center;
  gap: 12px;
}
.ann-seal {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 26px;
  height: 26px;
  border-radius: 5px;
  background: var(--oj-seal);
  color: var(--oj-white);
  font-family: var(--oj-font-display);
  font-size: 13px;
  font-weight: 700;
  flex-shrink: 0;
  user-select: none;
}
.ann-title {
  margin: 0;
  font-size: 16px;
  flex: 1;
  min-width: 0;
}
.ann-date {
  font-size: 12px;
  color: var(--oj-text-weak);
  flex-shrink: 0;
}
.ann-body {
  margin: 12px 0 0;
  padding-left: 38px;
  font-size: 13.5px;
  line-height: 1.9;
  color: var(--oj-text-sub);
  white-space: pre-wrap;
  word-break: break-word;
}
@media (max-width: 640px) {
  .ann-body {
    padding-left: 0;
  }
}
</style>
