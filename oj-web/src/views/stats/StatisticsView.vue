<template>
  <div class="page-container">
    <!-- 概览卡 -->
    <div class="overview">
      <div class="ov-card main">
        <div class="ov-label">总通过率</div>
        <div class="ov-value">{{ stat.acceptRate }}<span class="unit">%</span></div>
        <div class="ov-sub">通过 {{ stat.totalAccepted }} / 提交 {{ stat.totalSubmit }}</div>
      </div>
      <div class="ov-card">
        <div class="ov-label">已解决题目</div>
        <div class="ov-value">{{ stat.solvedCount }}</div>
        <div class="ov-sub">尝试过 {{ stat.attemptedCount }} 道</div>
      </div>
      <div class="ov-card">
        <div class="ov-label">题目覆盖率</div>
        <div class="ov-value">{{ coverageRate }}<span class="unit">%</span></div>
        <div class="ov-sub">全站 {{ totalProblems }} 道可用题</div>
      </div>
      <div class="ov-card">
        <div class="ov-label">平均每解一题</div>
        <div class="ov-value">{{ submitPerSolved }}</div>
        <div class="ov-sub">次提交</div>
      </div>
    </div>

    <div class="card">
      <h3>难度攻克情况</h3>
      <div class="diff-cards">
        <div v-for="d in stat.byDifficulty" :key="d.difficulty" class="diff-card">
          <div class="diff-head">
            <el-tag :color="DIFFICULTY_MAP[d.difficulty]?.color" effect="dark" size="small">
              {{ d.difficultyLabel }}
            </el-tag>
            <span class="diff-count">{{ d.solvedCount }} / {{ d.totalCount }} 题</span>
          </div>
          <el-progress
            :percentage="d.totalCount ? Math.round((d.solvedCount / d.totalCount) * 100) : 0"
            :stroke-width="10"
            :color="DIFFICULTY_MAP[d.difficulty]?.color"
          />
          <div class="diff-foot">
            <span>提交 {{ d.submit }}</span>
            <span>通过 {{ d.accepted }}</span>
            <span>通过率 <b :class="rateClass(d.acceptRate)">{{ d.acceptRate }}%</b></span>
          </div>
        </div>
      </div>
    </div>

    <div class="two-col">
      <div class="card">
        <h3>判题状态分布</h3>
        <p class="hint">一眼看出短板类型：WA 多说明逻辑有问题，TLE 多说明算法复杂度不够</p>
        <div ref="statusChart" class="chart"></div>
      </div>

      <div class="card">
        <h3>近 7 天提交趋势</h3>
        <p class="hint">柱状为提交数，折线为通过数</p>
        <div ref="trendChart" class="chart"></div>
      </div>
    </div>

    <div class="card">
      <h3>难度对比明细</h3>
      <el-table :data="stat.byDifficulty" v-loading="loading">
        <el-table-column prop="difficultyLabel" label="难度" width="100">
          <template #default="{ row }">
            <el-tag :color="DIFFICULTY_MAP[row.difficulty]?.color" effect="light" size="small">
              {{ row.difficultyLabel }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="已解题 / 总题" width="140">
          <template #default="{ row }">{{ row.solvedCount }} / {{ row.totalCount }}</template>
        </el-table-column>
        <el-table-column prop="submit" label="提交次数" width="110" />
        <el-table-column prop="accepted" label="通过次数" width="110" />
        <el-table-column label="通过率" min-width="200">
          <template #default="{ row }">
            <div class="tbl-rate">
              <el-progress
                :percentage="row.acceptRate"
                :stroke-width="10"
                :color="rateColor(row.acceptRate)"
                :show-text="false"
                class="tbl-bar"
              />
              <span :class="rateClass(row.acceptRate)">{{ row.acceptRate }}%</span>
            </div>
          </template>
        </el-table-column>
      </el-table>
      <el-empty v-if="!loading && !stat.byDifficulty.length" description="还没有提交记录，去做一道题吧" />
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed, nextTick, onMounted, onUnmounted, ref } from 'vue'
import * as echarts from 'echarts'
import { getStatistics, getTrend, type Statistics } from '@/api/submission'
import { DIFFICULTY_MAP } from '@/utils/status'

const emptyStat = (): Statistics => ({
  byDifficulty: [],
  byStatus: [],
  totalSubmit: 0,
  totalAccepted: 0,
  acceptRate: 0,
  solvedCount: 0,
  attemptedCount: 0,
})

const stat = ref<Statistics>(emptyStat())
const loading = ref(false)
const statusChart = ref<HTMLElement>()
const trendChart = ref<HTMLElement>()

let statusInstance: echarts.ECharts | null = null
let trendInstance: echarts.ECharts | null = null

/** 全站可用题目总数（各难度 totalCount 之和） */
const totalProblems = computed(() =>
  stat.value.byDifficulty.reduce((sum, d) => sum + (d.totalCount || 0), 0))

/** 题目覆盖率：已解决 / 全站题数 */
const coverageRate = computed(() =>
  totalProblems.value ? Math.round((stat.value.solvedCount / totalProblems.value) * 1000) / 10 : 0)

/** 平均每解一题需要多少次提交（衡量效率，越低越好） */
const submitPerSolved = computed(() =>
  stat.value.solvedCount ? Math.round((stat.value.totalSubmit / stat.value.solvedCount) * 10) / 10 : 0)

function rateClass(rate: number) {
  if (rate >= 60) return 'good'
  if (rate >= 30) return 'mid'
  return 'low'
}
function rateColor(rate: number) {
  if (rate >= 60) return '#16a34a'
  if (rate >= 30) return '#d97706'
  return '#dc2626'
}

/** 各判题状态的配色（与项目内状态语义一致） */
const STATUS_COLORS: Record<number, string> = {
  2: '#16a34a', // AC 绿
  3: '#f59e0b', // WA 橙
  4: '#8b5cf6', // TLE 紫
  5: '#ec4899', // MLE 粉
  6: '#ef4444', // RE 红
  7: '#64748b', // CE 灰
  8: '#111827', // SE 黑
}

function renderCharts(trend: { date: string; total: number; accepted: number }[]) {
  // 状态分布：环形图（比柱状更省空间，且适合看占比）
  if (statusChart.value && stat.value.byStatus.length) {
    statusInstance = echarts.getInstanceByDom(statusChart.value) ?? echarts.init(statusChart.value)
    statusInstance.setOption({
      tooltip: {
        trigger: 'item',
        formatter: '{b}: {c} 次 ({d}%)',
      },
      legend: { bottom: 0, icon: 'circle', itemWidth: 9, itemHeight: 9, textStyle: { fontSize: 12 } },
      series: [
        {
          type: 'pie',
          radius: ['42%', '66%'],
          center: ['50%', '44%'],
          avoidLabelOverlap: true,
          itemStyle: { borderRadius: 6, borderColor: '#fff', borderWidth: 2 },
          label: { show: false },
          emphasis: { label: { show: true, fontSize: 14, fontWeight: 'bold' } },
          data: stat.value.byStatus.map((s) => ({
            name: s.statusDesc,
            value: s.count,
            itemStyle: { color: STATUS_COLORS[s.status] ?? '#94a3b8' },
          })),
        },
      ],
    })
  }

  // 趋势：柱状 + 折线
  if (trendChart.value) {
    trendInstance = echarts.getInstanceByDom(trendChart.value) ?? echarts.init(trendChart.value)
    const dates = trend.map((t) => t.date.slice(5))
    trendInstance.setOption({
      tooltip: { trigger: 'axis' },
      legend: { data: ['提交', '通过'], top: 0, icon: 'roundRect', itemWidth: 12, itemHeight: 8 },
      grid: { left: 44, right: 20, top: 40, bottom: 28 },
      xAxis: { type: 'category', data: dates, axisTick: { show: false } },
      yAxis: { type: 'value', minInterval: 1, splitLine: { lineStyle: { color: '#f0f2f5' } } },
      series: [
        {
          name: '提交', type: 'bar', data: trend.map((t) => t.total), barWidth: '42%',
          itemStyle: { color: '#6366f1', borderRadius: [4, 4, 0, 0] },
        },
        {
          name: '通过', type: 'line', data: trend.map((t) => t.accepted), smooth: true,
          symbol: 'circle', symbolSize: 6,
          itemStyle: { color: '#16a34a' }, lineStyle: { width: 3 },
        },
      ],
    })
  }
}

function handleResize() {
  statusInstance?.resize()
  trendInstance?.resize()
}

onMounted(async () => {
  window.addEventListener('resize', handleResize)
  loading.value = true
  try {
    const [s, trend] = await Promise.all([getStatistics(), getTrend(7)])
    stat.value = s ?? emptyStat()
    await nextTick()
    renderCharts(trend)
  } catch {
    // 未登录(40100)或接口异常：保持空态，由拦截器提示
  } finally {
    loading.value = false
  }
})

onUnmounted(() => {
  window.removeEventListener('resize', handleResize)
  statusInstance?.dispose()
  trendInstance?.dispose()
  statusInstance = null
  trendInstance = null
})
</script>

<style scoped>
.overview {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 16px;
  margin-bottom: 16px;
}
.ov-card {
  background: #fff;
  border: 1px solid #eef2f7;
  border-radius: 12px;
  padding: 20px 22px;
  box-shadow: 0 1px 2px rgba(17, 24, 39, 0.04);
}
.ov-card.main {
  background: linear-gradient(135deg, #3730a3 0%, #4f46e5 60%, #6366f1 100%);
  border: none;
  color: #fff;
}
.ov-label {
  font-size: 12.5px;
  color: #909399;
}
.ov-card.main .ov-label {
  color: rgba(255, 255, 255, 0.85);
}
.ov-value {
  font-size: 30px;
  font-weight: 800;
  line-height: 1.15;
  margin-top: 6px;
  font-variant-numeric: tabular-nums;
  color: #303133;
}
.ov-card.main .ov-value {
  color: #fff;
}
.unit {
  font-size: 16px;
  font-weight: 600;
  margin-left: 2px;
}
.ov-sub {
  font-size: 12px;
  color: #a8abb2;
  margin-top: 4px;
}
.ov-card.main .ov-sub {
  color: rgba(255, 255, 255, 0.8);
}

.two-col {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 16px;
}
@media (max-width: 900px) {
  .overview,
  .two-col {
    grid-template-columns: 1fr;
  }
}

.diff-cards {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(240px, 1fr));
  gap: 14px;
}
.diff-card {
  background: #fafbfc;
  border: 1px solid #eef2f7;
  border-radius: 10px;
  padding: 16px 18px;
}
.diff-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 10px;
}
.diff-count {
  font-size: 12.5px;
  color: #606266;
  font-weight: 600;
  font-variant-numeric: tabular-nums;
}
.diff-foot {
  display: flex;
  gap: 14px;
  margin-top: 10px;
  font-size: 12px;
  color: #909399;
}
.chart {
  width: 100%;
  height: 300px;
}
.hint {
  margin: -6px 0 10px;
  font-size: 12.5px;
  color: #a8abb2;
}
.tbl-rate {
  display: flex;
  align-items: center;
  gap: 10px;
}
.tbl-bar {
  flex: 1;
  max-width: 130px;
}
.good {
  color: #16a34a;
  font-weight: 600;
}
.mid {
  color: #d97706;
  font-weight: 600;
}
.low {
  color: #dc2626;
  font-weight: 600;
}
h3 {
  margin: 0 0 4px;
}
</style>
