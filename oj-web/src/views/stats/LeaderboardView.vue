<template>
  <div class="page-container">
    <div class="card">
      <h2>排行榜</h2>
      <el-table :data="items" v-loading="loading">
        <el-table-column prop="rank" label="排名" width="90">
          <template #default="{ row }">
            <span :class="{ top: row.rank <= 3 }">{{ row.rank }}</span>
          </template>
        </el-table-column>
        <el-table-column label="用户" min-width="200">
          <template #default="{ row }">{{ row.nickname || row.username || `用户 ${row.userId}` }}</template>
        </el-table-column>
        <el-table-column prop="acCount" label="通过题数" width="140" />
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

const items = ref<{ userId: number; username?: string; nickname?: string; acCount: number; rank: number }[]>([])
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

onMounted(load)
</script>

<style scoped>
.top {
  color: #e6a23c;
  font-weight: 700;
}
</style>
