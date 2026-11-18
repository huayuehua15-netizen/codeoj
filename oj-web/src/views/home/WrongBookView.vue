<template>
  <div class="page-container">
    <header class="oj-stage">
      <div>
        <h1 class="oj-stage-title">错题本</h1>
        <p class="oj-stage-sub">尝试过但还没 AC 的题都在这里——重做一道，就少一道</p>
      </div>
      <div class="oj-stage-side">
        <div class="oj-stage-stat">
          <span class="oj-stage-num">{{ items.length }}</span>
          <span class="oj-stage-label">待攻克</span>
        </div>
      </div>
    </header>

    <div class="card" v-loading="loading">
      <el-table :data="items" empty-text="太棒了——没有错题，去题库找新对手吧">
        <el-table-column label="题目" min-width="240">
          <template #default="{ row }">
            <router-link class="wb-link" :to="`/problems/${row.problemId}`">{{ row.title }}</router-link>
          </template>
        </el-table-column>
        <el-table-column label="难度" width="110">
          <template #default="{ row }">
            <span class="oj-diff" :class="`diff-${row.difficulty}`">
              {{ ({1:'简单',2:'中等',3:'困难'} as Record<number,string>)[row.difficulty] || '?' }}
            </span>
          </template>
        </el-table-column>
        <el-table-column label="最近状态" width="140">
          <template #default="{ row }">
            <span class="wb-status" :style="{ color: statusColor(row.lastStatus) }">
              <span class="status-dot" :style="{ background: statusColor(row.lastStatus) }" />
              {{ statusLabel(row.lastStatus) }}
            </span>
          </template>
        </el-table-column>
        <el-table-column label="尝试次数" width="110" align="center">
          <template #default="{ row }">
            <span class="mono tabular">{{ row.attempts }}</span>
          </template>
        </el-table-column>
        <el-table-column label="最近提交" width="160">
          <template #default="{ row }">
            <span class="mono tabular">{{ formatDateTime(row.lastAt) }}</span>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="90" align="center">
          <template #default="{ row }">
            <el-button text type="primary" size="small" @click="$router.push(`/problems/${row.problemId}`)">重做</el-button>
          </template>
        </el-table-column>
      </el-table>
      <p class="wb-note">口径：有提交记录但从未通过的题目；一旦 AC 即移出错题本。</p>
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
.wb-link {
  color: var(--oj-text);
  font-weight: 500;
}
.wb-link:hover {
  color: var(--oj-primary);
}
.wb-status {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  font-size: 12.5px;
  font-weight: 600;
}
.status-dot {
  width: 7px;
  height: 7px;
  border-radius: 50%;
}
.wb-note {
  margin: 12px 0 0;
  font-size: 12px;
  color: var(--oj-text-weak);
}
</style>
