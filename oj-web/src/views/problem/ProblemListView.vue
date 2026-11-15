<template>
  <div class="page-container">
    <div class="card">
      <div class="toolbar">
        <el-input v-model="keyword" placeholder="按关键词搜索题目" style="width: 240px" clearable @keyup.enter="load" />
        <el-select v-model="difficulty" placeholder="难度" style="width: 120px" clearable @change="load">
          <el-option label="简单" :value="1" />
          <el-option label="中等" :value="2" />
          <el-option label="困难" :value="3" />
        </el-select>
        <el-button type="primary" @click="load">搜索</el-button>
      </div>

      <el-table :data="problems" v-loading="loading">
        <el-table-column prop="id" label="#" width="70" />
        <el-table-column label="题目" min-width="300">
          <template #default="{ row }">
            <router-link :to="`/problems/${row.id}`" class="title-link">{{ row.title }}</router-link>
          </template>
        </el-table-column>
        <el-table-column label="难度" width="100">
          <template #default="{ row }">
            <el-tag :color="difficultyInfo(row.difficulty).color" effect="light" size="small">
              {{ difficultyInfo(row.difficulty).label }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="通过率" width="220">
          <template #default="{ row }">
            <div class="rate-cell">
              <el-progress
                :percentage="passRate(row)"
                :stroke-width="9"
                :color="rateColor(passRate(row))"
              />
              <span class="rate-text">{{ passRate(row) }}% ({{ row.acceptedCount }}/{{ row.submitCount }})</span>
            </div>
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
import { pageProblems, type Problem } from '@/api/problem'
import { DIFFICULTY_MAP } from '@/utils/status'

const problems = ref<Problem[]>([])
const keyword = ref('')
const difficulty = ref<number | undefined>()
const page = ref(1)
const size = ref(10)
const total = ref(0)
const loading = ref(false)

function difficultyInfo(d: number) {
  return DIFFICULTY_MAP[d] || { label: '未知', color: '#909399' }
}

function passRate(row: Problem) {
  if (!row.submitCount) return 0
  return Math.round((row.acceptedCount / row.submitCount) * 100)
}

/** 通过率分级配色（OJ 惯例）：高通过率偏绿、低通过率偏红 */
function rateColor(rate: number) {
  if (rate >= 60) return '#67c23a'
  if (rate >= 30) return '#e6a23c'
  return '#f56c6c'
}

async function load() {
  loading.value = true
  try {
    const data = await pageProblems({
      keyword: keyword.value || undefined,
      difficulty: difficulty.value,
      page: page.value,
      size: size.value,
    })
    problems.value = data.records || []
    total.value = data.total
  } finally {
    loading.value = false
  }
}

onMounted(load)
</script>

<style scoped>
.toolbar {
  display: flex;
  gap: 12px;
  margin-bottom: 16px;
}
.title-link {
  color: #303133;
  font-weight: 500;
}
.title-link:hover {
  color: #2563eb;
}
.rate-cell {
  display: flex;
  align-items: center;
  gap: 8px;
}
.rate-cell :deep(.el-progress) {
  flex: 1;
}
.rate-cell :deep(.el-progress__text) {
  display: none;
}
.rate-text {
  font-size: 12px;
  color: #606266;
  white-space: nowrap;
}
</style>
