<template>
  <div class="bulletin">
    <header class="page-head bulletin-head">
      <div>
        <p class="kicker">BULLETIN</p>
        <h1 class="page-head-title">公告</h1>
        <p class="page-head-sub">课程通知、竞赛安排与平台更新。</p>
      </div>
      <div class="bulletin-count">
        <span class="count-num">{{ String(announcements.length).padStart(2, '0') }}</span>
        <span class="count-label">NOTICES</span>
      </div>
    </header>

    <div v-loading="loading" class="bulletin-body">
      <article v-for="a in announcements" :key="a.id" class="notice">
        <div class="notice-meta">
          <span class="notice-date">{{ formatDateTime(a.createdAt) }}</span>
          <span class="notice-tag">NOTICE</span>
          <span class="notice-id">#{{ a.id }}</span>
        </div>
        <h2 class="notice-title">{{ a.title }}</h2>
        <p class="notice-body">{{ a.content }}</p>
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
  max-width: 1040px;
  margin: 0 auto;
  padding: 44px 32px 64px;
}

.bulletin-head {
  align-items: flex-end;
  margin-bottom: 22px;
}
.bulletin-count {
  display: flex;
  align-items: baseline;
  gap: 8px;
  padding-bottom: 2px;
}
.count-num {
  font-family: var(--oj-font-mono);
  font-size: 26px;
  font-weight: 600;
  line-height: 1;
  color: var(--oj-text);
}
.count-label {
  font-family: var(--oj-font-mono);
  font-size: 8.5px;
  letter-spacing: 0.1em;
  color: var(--oj-text-weak);
}

.bulletin-body {
  min-height: 160px;
}
.notice {
  padding: 18px 20px 16px;
  margin-bottom: 10px;
  background: var(--oj-card);
  border: 1px solid var(--oj-border);
  border-radius: 10px;
  box-shadow: var(--oj-shadow-sm);
  transition: border-color var(--oj-duration) var(--oj-ease);
}
.notice:hover {
  border-color: var(--oj-border-strong);
}
.notice-meta {
  display: flex;
  align-items: center;
  gap: 8px;
}
.notice-date {
  font-family: var(--oj-font-mono);
  font-size: 9.5px;
  color: var(--oj-text-weak);
}
.notice-tag {
  padding: 2px 6px;
  border-radius: 4px;
  background: var(--oj-primary-soft);
  font-family: var(--oj-font-mono);
  font-size: 8px;
  letter-spacing: 0.08em;
  color: var(--oj-primary);
}
.notice-id {
  margin-left: auto;
  font-family: var(--oj-font-mono);
  font-size: 9px;
  color: var(--oj-gray-300);
}
.notice-title {
  margin: 9px 0 0;
  font-size: 17px;
  font-weight: 700;
  line-height: 1.45;
  letter-spacing: -0.02em;
  color: var(--oj-text);
}
.notice-body {
  margin: 7px 0 0;
  max-width: 52em;
  font-size: 12px;
  line-height: 1.85;
  color: var(--oj-text-sub);
  white-space: pre-wrap;
  word-break: break-word;
}
.notice-empty {
  padding: 44px 20px;
  background: var(--oj-card);
  border: 1px solid var(--oj-border);
  border-radius: 10px;
  box-shadow: var(--oj-shadow-sm);
}

@media (max-width: 680px) {
  .bulletin {
    padding: 30px 18px 52px;
  }
  .notice {
    padding: 16px 16px 14px;
  }
  .notice-title {
    font-size: 16px;
  }
}
</style>
