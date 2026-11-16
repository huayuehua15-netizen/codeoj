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

      <h3>近 7 天提交趋势</h3>
      <div ref="trendChart" class="chart"></div>

      <h3>难度分布对比</h3>
      <div ref="diffChart" class="chart" v-show="stat.byDifficulty.length"></div>

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
import { computed, onMounted, onUnmounted, ref, nextTick } from 'vue'
import * as echarts from 'echarts'
import { getStatistics, getTrend } from '@/api/submission'
import { DIFFICULTY_MAP } from '@/utils/status'

const stat = ref<{
  byDifficulty: { difficulty: number; difficultyLabel: string; submit: number; accepted: number }[]
  totalSubmit: number
  totalAccepted: number
}>({ byDifficulty: [], totalSubmit: 0, totalAccepted: 0 })
const loading = ref(false)
const trendChart = ref<HTMLElement>()
const diffChart = ref<HTMLElement>()

const totalRate = computed(() =>
  stat.value.totalSubmit ? Math.round((stat.value.totalAccepted / stat.value.totalSubmit) * 100) : 0)

/** ECharts：趋势折线柱状混合 + 难度对比柱状 */
let trendInstance: echarts.ECharts | null = null
let diffInstance: echarts.ECharts | null = null

function renderCharts(trend: { date: string; total: number; accepted: number }[]) {
  if (trendChart.value) {
    // 复用已有实例，避免重复 init 报 "There is a chart instance already initialized"
    trendInstance = echarts.getInstanceByDom(trendChart.value) ?? echarts.init(trendChart.value)
    const dates = trend.map((t) => t.date.slice(5))
    trendInstance.setOption({
      tooltip: { trigger: 'axis' },
      legend: { data: ['提交', '通过'] },
      grid: { left: 40, right: 20, top: 36, bottom: 28 },
      xAxis: { type: 'category', data: dates },
      yAxis: { type: 'value', minInterval: 1 },
      series: [
        { name: '提交', type: 'bar', data: trend.map((t) => t.total), barWidth: '40%',
          itemStyle: { color: '#6366f1', borderRadius: [4, 4, 0, 0] } },
        { name: '通过', type: 'line', data: trend.map((t) => t.accepted), smooth: true,
          itemStyle: { color: '#16a34a' }, lineStyle: { width: 3 } },
      ],
    })
  }
  if (diffChart.value && stat.value.byDifficulty.length) {
    diffInstance = echarts.getInstanceByDom(diffChart.value) ?? echarts.init(diffChart.value)
    const labels = stat.value.byDifficulty.map((d) => d.difficultyLabel)
    diffInstance.setOption({
      tooltip: { trigger: 'axis' },
      legend: { data: ['提交', '通过'] },
      grid: { left: 40, right: 20, top: 36, bottom: 28 },
      xAxis: { type: 'category', data: labels },
      yAxis: { type: 'value', minInterval: 1 },
      series: [
        { name: '提交', type: 'bar', data: stat.value.byDifficulty.map((d) => d.submit), barWidth: '40%',
          itemStyle: { color: '#6366f1', borderRadius: [4, 4, 0, 0] } },
        { name: '通过', type: 'bar', data: stat.value.byDifficulty.map((d) => d.accepted), barWidth: '40%',
          itemStyle: { color: '#16a34a', borderRadius: [4, 4, 0, 0] } },
      ],
    })
  }
}

/** 窗口尺寸变化时重排图表（图表宽度为 100%） */
function handleResize() {
  trendInstance?.resize()
  diffInstance?.resize()
}

onMounted(async () => {
  window.addEventListener('resize', handleResize)
  loading.value = true
  try {
    stat.value = await getStatistics()
    const trend = await getTrend(7)
    await nextTick()
    renderCharts(trend)
  } catch {
    // 统计接口失败（如未登录 40100）时保持空态，不阻塞页面
  } finally {
    loading.value = false
  }
})

onUnmounted(() => {
  window.removeEventListener('resize', handleResize)
  trendInstance?.dispose()
  diffInstance?.dispose()
  trendInstance = null
  diffInstance = null
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
.chart {
  width: 100%;
  height: 300px;
}
.summary {
  color: #606266;
}
</style>
