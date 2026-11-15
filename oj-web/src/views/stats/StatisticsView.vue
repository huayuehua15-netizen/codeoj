<template>
  <div class="page-container">
    <div class="card">
      <h2>个人通过率统计</h2>
      <div class="stat-cards">
        <div class="stat-card">
          <el-progress type="dashboard" :width="120" :stroke-width="10"
            :percentage="totalRate" :color="totalRate >= 60 ? '#67c23a' : totalRate >= 30 ? '#e6a23c' : '#f56c6c'">
            <template #default="{ percentage }">
              <span class="ring-num">{{ percentage }}%</span>
              <span class="ring-label">总通过率</span>
            </template>
          </el-progress>
          <p class="ring-cap">总提交 {{ stat.totalSubmit }} · 通过 {{ stat.totalAccepted }}</p>
        </div>
        <div class="stat-card" v-for="d in stat.byDifficulty" :key="d.difficulty">
          <el-progress type="circle" :width="120" :stroke-width="10"
            :percentage="d.submit ? Math.round((d.accepted / d.submit) * 100) : 0"
            :color="DIFFICULTY_MAP[d.difficulty]?.color || '#409eff'">
            <template #default="{ percentage }">
              <span class="ring-num">{{ percentage }}%</span>
              <span class="ring-label">{{ d.difficultyLabel }}</span>
            </template>
          </el-progress>
          <p class="ring-cap">提交 {{ d.submit }} · 通过 {{ d.accepted }}</p>
        </div>
      </div>
      <el-table :data="stat.byDifficulty" v-loading="loading" style="margin-top: 8px">
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
import { computed, onMounted, ref } from 'vue'
import { getStatistics } from '@/api/submission'
import { DIFFICULTY_MAP } from '@/utils/status'

const stat = ref<{
  byDifficulty: { difficulty: number; difficultyLabel: string; submit: number; accepted: number }[]
  totalSubmit: number
  totalAccepted: number
}>({ byDifficulty: [], totalSubmit: 0, totalAccepted: 0 })
const loading = ref(false)

const totalRate = computed(() =>
  stat.value.totalSubmit ? Math.round((stat.value.totalAccepted / stat.value.totalSubmit) * 100) : 0)

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
.stat-cards {
  display: flex;
  gap: 16px;
  flex-wrap: wrap;
  margin: 16px 0 20px;
}
.stat-card {
  background: #f8fafc;
  border: 1px solid #eef2f7;
  border-radius: 10px;
  padding: 18px 24px;
  text-align: center;
}
.ring-num {
  font-size: 22px;
  font-weight: 800;
  color: #303133;
}
.ring-label {
  display: block;
  font-size: 12px;
  color: #909399;
  margin-top: 2px;
}
.ring-cap {
  color: #606266;
  font-size: 12px;
  margin: 8px 0 0;
}
.summary {
  color: #606266;
}
</style>
