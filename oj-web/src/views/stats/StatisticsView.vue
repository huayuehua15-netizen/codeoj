<template>
  <div class="page-container">
    <div class="card">
      <h2>个人通过率统计</h2>
      <p class="summary">
        总提交 <b>{{ stat.totalSubmit }}</b> 次 ｜ 通过 <b>{{ stat.totalAccepted }}</b> 次
      </p>
      <el-table :data="stat.byDifficulty" v-loading="loading">
        <el-table-column prop="difficultyLabel" label="难度" width="120" />
        <el-table-column prop="submit" label="提交次数" />
        <el-table-column prop="accepted" label="通过次数" />
        <el-table-column label="通过率">
          <template #default="{ row }">
            <el-progress
              :percentage="row.submit ? Math.round((row.accepted / row.submit) * 100) : 0"
              :stroke-width="10"
            />
          </template>
        </el-table-column>
      </el-table>
    </div>
  </div>
</template>

<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { getStatistics } from '@/api/submission'

const stat = ref<{
  byDifficulty: { difficulty: number; difficultyLabel: string; submit: number; accepted: number }[]
  totalSubmit: number
  totalAccepted: number
}>({ byDifficulty: [], totalSubmit: 0, totalAccepted: 0 })
const loading = ref(false)

onMounted(async () => {
  loading.value = true
  try {
    stat.value = await getStatistics()
  } finally {
    loading.value = false
  }
})
</script>

<style scoped>
.summary {
  color: #606266;
}
</style>
