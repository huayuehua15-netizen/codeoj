<template>
  <div class="page-container">
    <div class="card">
      <h2>排行榜</h2>
      <el-table :data="items" v-loading="loading" :row-class-name="rankRowClass">
        <el-table-column prop="rank" label="排名" width="90">
          <template #default="{ row }">
            <span class="medal" :class="`medal-${row.rank}`">{{ medal(row.rank) }}</span>
          </template>
        </el-table-column>
        <el-table-column label="用户" min-width="200">
          <template #default="{ row }">{{ row.username || `用户 ${row.userId}` }}</template>
        </el-table-column>
        <el-table-column prop="acCount" label="通过题数" width="140">
          <template #default="{ row }">
            <b>{{ row.acCount }}</b>
          </template>
        </el-table-column>
      </el-table>
      <el-pagination
        v-model:current-page="page"
        :page-size="size"
        :total="total"
        layout="prev, pager, next"
        style="margin-top: 16px; justify-content: flex-end"
        @current-change="load"
      />
    </div>
  </div>
</template>

<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { getLeaderboard } from '@/api/leaderboard'

const items = ref<{ userId: number; username?: string; acCount: number; rank: number }[]>([])
const page = ref(1)
const size = ref(20)
const total = ref(0)
const loading = ref(false)

async function load() {
  loading.value = true
  try {
    const data = await getLeaderboard({ page: page.value, size: size.value })
    items.value = data.items || []
    total.value = data.total
  } finally {
    loading.value = false
  }
}

const MEDALS: Record<number, string> = { 1: '🥇', 2: '🥈', 3: '🥉' }

function medal(rank: number) {
  return MEDALS[rank] ?? String(rank)
}

function rankRowClass({ row }: { row: { rank: number } }) {
  return row.rank <= 3 ? `top-row top-row-${row.rank}` : ''
}

onMounted(load)
</script>

<style scoped>
.medal {
  font-size: 16px;
}
.medal-1,
.medal-2,
.medal-3 {
  font-weight: 700;
}
</style>

<style>
/* 前三名行高亮（表格行类名非 scoped，需全局） */
.el-table .top-row-1 {
  background: #fdf6ec;
}
.el-table .top-row-2 {
  background: #f4f4f5;
}
.el-table .top-row-3 {
  background: #f0f9eb;
}
</style>
