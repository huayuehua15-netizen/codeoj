<template>
  <div class="bulletin">
    <header class="page-head bulletin-head">
      <div>
        <p class="kicker">BULLETIN</p>
        <h1 class="page-head-title">公告</h1>
        <p class="page-head-sub">
          课程通知、竞赛安排与平台更新。
          <span v-if="announcements.length" class="head-count">共 {{ announcements.length }} 条</span>
        </p>
      </div>
    </header>

    <div v-loading="loading" class="bulletin-body">
      <article v-for="a in announcements" :key="a.id" class="notice">
        <div class="notice-meta">
          <time class="notice-date">{{ formatDateTime(a.createdAt) }}</time>
        </div>
        <div class="notice-content">
          <h2 class="notice-title">{{ a.title }}</h2>
          <p class="notice-body">{{ a.content }}</p>
        </div>
      </article>

      <div v-if="!loading && !announcements.length" class="notice-empty">
        <el-empty description="还没有公告" :image-size="72" />
      </div>
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
.bulletin {
  max-width: 1000px;
  margin: 0 auto;
  padding: 36px 32px 64px;
}

.bulletin-head {
  align-items: flex-end;
  margin-bottom: 20px;
}
.head-count {
  margin-left: 10px;
  font-size: 10.5px;
  color: var(--oj-text-weak);
}

.bulletin-body {
  min-height: 160px;
  border-top: 1px solid var(--oj-border);
}
.notice {
  display: grid;
  grid-template-columns: 112px minmax(0, 1fr);
  gap: 24px;
  padding: 18px 0 17px;
  border-bottom: 1px solid var(--oj-border);
}
.notice-meta {
  padding-top: 2px;
}
.notice-date {
  font-family: var(--oj-font-mono);
  font-size: 10px;
  line-height: 1.5;
  color: var(--oj-text-weak);
}
.notice-content {
  min-width: 0;
}
.notice-title {
  margin: 0;
  font-size: 16px;
  font-weight: 700;
  line-height: 1.45;
  letter-spacing: -0.02em;
  color: var(--oj-text);
}
.notice-body {
  margin: 7px 0 0;
  max-width: 58em;
  font-size: 12px;
  line-height: 1.85;
  color: var(--oj-text-sub);
  white-space: pre-wrap;
  word-break: break-word;
}
.notice-empty {
  padding: 44px 0;
  border-bottom: 1px solid var(--oj-border);
}

@media (max-width: 680px) {
  .bulletin {
    padding: 28px 18px 52px;
  }
  .notice {
    grid-template-columns: 1fr;
    gap: 5px;
    padding: 16px 0 15px;
  }
  .notice-title {
    font-size: 15px;
  }
}
</style>
