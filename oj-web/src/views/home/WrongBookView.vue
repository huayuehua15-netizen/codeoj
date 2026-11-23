<template>
  <div class="errata">
    <header class="page-head errata-head">
      <div>
        <h1 class="page-head-title">错题本</h1>
        <p class="page-head-sub">
          有提交记录但尚未 AC 的题目，AC 后自动移出。
          <span v-if="items.length" class="head-count">共 {{ items.length }} 道</span>
        </p>
      </div>
    </header>

    <div class="errata-body" v-loading="loading">
      <div v-if="items.length" class="errata-cols" aria-hidden="true">
        <span>题目</span>
        <span>标题</span>
        <span>难度</span>
        <span>最近状态</span>
        <span>尝试</span>
        <span>最近提交</span>
        <span></span>
      </div>

      <ul v-if="items.length" class="errata-list">
        <li v-for="row in items" :key="row.problemId">
          <router-link :to="`/problems/${row.problemId}`" class="errata-row">
            <span class="row-no mono">#{{ row.problemId }}</span>
            <span class="row-title">{{ row.title }}</span>
            <span class="oj-diff" :class="`diff-${row.difficulty}`">
              {{ ({ 1: '简单', 2: '中等', 3: '困难' } as Record<number, string>)[row.difficulty] || '?' }}
            </span>
            <span class="row-status" :style="{ color: statusColor(row.lastStatus) }">
              {{ statusLabel(row.lastStatus) }}
            </span>
            <span class="row-attempts mono tabular">{{ row.attempts }} 次</span>
            <span class="row-time mono">{{ formatDateTime(row.lastAt) }}</span>
            <span class="row-action">重做</span>
          </router-link>
        </li>
      </ul>

      <el-empty v-else-if="!loading" description="没有错题，去题库找新题目" />
    </div>
  </div>
</template>

<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { getWrongBook, type WrongBookItem } from '@/api/submission'
import { statusColor, statusLabel } from '@/utils/status'
import { formatDateTime } from '@/utils/datetime'

const items = ref<WrongBookItem[]>([])
const loading = ref(false)

onMounted(async () => {
  loading.value = true
  try {
    items.value = await getWrongBook()
  } finally {
    loading.value = false
  }
})
</script>

<style scoped>
.errata {
  max-width: 1120px;
  margin: 0 auto;
  padding: 36px 32px 64px;
}

.errata-head {
  align-items: flex-end;
  margin-bottom: 20px;
}
.head-count {
  margin-left: 10px;
  font-size: 10.5px;
  color: var(--oj-text-weak);
}

.errata-body {
  background: var(--oj-card);
  border: 1px solid var(--oj-border);
  border-radius: 4px;
  overflow: hidden;
}
.errata-cols {
  display: grid;
  grid-template-columns: 62px minmax(0, 1fr) 72px 108px 64px 132px 52px;
  gap: 14px;
  padding: 8px 16px 7px;
  border-bottom: 1px solid var(--oj-border);
  font-size: 10px;
  color: var(--oj-text-weak);
}
.errata-list {
  list-style: none;
  margin: 0;
  padding: 0;
}
.errata-row {
  display: grid;
  grid-template-columns: 62px minmax(0, 1fr) 72px 108px 64px 132px 52px;
  gap: 14px;
  align-items: center;
  padding: 11px 18px;
  border-bottom: 1px solid var(--oj-border);
  transition: background var(--oj-duration) var(--oj-ease);
}
.errata-list li:last-child .errata-row {
  border-bottom: 0;
}
.errata-row:hover {
  background: var(--oj-gray-50);
}
.row-no {
  font-size: 9.5px;
  color: var(--oj-text-weak);
}
.row-title {
  font-size: 13.5px;
  font-weight: 550;
  color: var(--oj-text);
  min-width: 0;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}
.errata-row:hover .row-title {
  color: var(--oj-primary);
}
.row-status {
  font-size: 11.5px;
  font-weight: 600;
}
.row-attempts {
  font-size: 10.5px;
  color: var(--oj-text-sub);
}
.row-time {
  font-size: 9.5px;
  color: var(--oj-text-weak);
}
.row-action {
  font-size: 11.5px;
  font-weight: 600;
  color: var(--oj-primary);
  text-align: right;
  white-space: nowrap;
}

@media (max-width: 900px) {
  .errata {
    padding: 28px 18px 52px;
  }
  .errata-cols {
    display: none;
  }
  .errata-row {
    grid-template-columns: 62px minmax(0, 1fr) 52px;
    gap: 9px;
    padding: 12px 14px;
  }
  .row-attempts,
  .row-time {
    display: none;
  }
  .row-status {
    grid-column: 2;
  }
  .row-action {
    grid-column: 3;
    grid-row: 1;
  }
}
</style>
