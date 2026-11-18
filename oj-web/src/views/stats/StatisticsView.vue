<template>
  <div class="page-container">
    <header class="oj-stage">
      <div>
        <h1 class="oj-stage-title">学习统计</h1>
        <p class="oj-stage-sub">难度覆盖、状态分布与七日趋势——数据不会说谎</p>
      </div>
    </header>
    <!-- 顶部概览 -->
    <div class="card oj-banner">
      <div class="oj-banner-item">
        <span class="oj-banner-num">{{ stat.acceptRate }}%</span>
        <span class="oj-banner-label">总通过率</span>
      </div>
      <div class="oj-banner-divider" aria-hidden="true" />
      <div class="oj-banner-item">
        <span class="oj-banner-num">{{ stat.solvedCount }}</span>
        <span class="oj-banner-label">已解决题目</span>
      </div>
      <div class="oj-banner-divider" aria-hidden="true" />
      <div class="oj-banner-item">
        <span class="oj-banner-num">{{ coverageRate }}%</span>
        <span class="oj-banner-label">题目覆盖率</span>
      </div>
      <div class="oj-banner-divider" aria-hidden="true" />
      <div class="oj-banner-item">
        <span class="oj-banner-num">{{ submitPerSolved }}</span>
        <span class="oj-banner-label">次 / 解决一题</span>
      </div>
      <div class="oj-banner-note">
        通过 {{ stat.totalAccepted }} / 提交 {{ stat.totalSubmit }}<br />
        全站 {{ totalProblems }} 道可用题 · 尝试过 {{ stat.attemptedCount }} 道
      </div>
    </div>

    <section class="card">
      <h2 class="sec-head">难度攻克情况</h2>
      <p class="hint">每张卡片的进度条表示该难度下「已在架且已通过」的题占该难度在架题总数的比例</p>
      <div class="diff-cards">
        <div v-for="d in stat.byDifficulty" :key="d.difficulty" class="diff-card" :class="`diff-${d.difficulty}`">
          <div class="diff-head">
            <span class="oj-diff" :class="`diff-${d.difficulty}`">{{ d.difficultyLabel }}</span>
            <span class="diff-count">{{ d.availableSolvedCount }} / {{ d.totalCount }} 道在架题</span>
          </div>
          <el-progress
            :percentage="d.totalCount ? Math.round((d.availableSolvedCount / d.totalCount) * 100) : 0"
            :stroke-width="10"
            :color="DIFFICULTY_MAP[d.difficulty]?.color"
          />
          <div class="diff-foot">
            <span>提交 <b class="tabular">{{ d.submit }}</b></span>
            <span>通过 <b class="tabular">{{ d.accepted }}</b></span>
            <span>通过率 <b :class="rateClass(d.acceptRate)">{{ d.acceptRate }}%</b></span>
          </div>
        </div>
      </div>
    </section>

    <div class="two-col">
      <section class="card">
        <h2 class="sec-head">判题状态分布</h2>
        <p class="hint">一眼看出短板类型：WA 多说明逻辑有问题，TLE 多说明算法复杂度不够</p>
        <div v-show="stat.byStatus.length" ref="statusChart" class="chart"></div>
        <el-empty
          v-if="!loading && !stat.byStatus.length"
          description="还没有判题结果，提交一次代码后就这里会显示分布"
        />
      </section>

      <section class="card">
        <h2 class="sec-head">近 7 天提交趋势</h2>
        <p class="hint">柱状为提交数，折线为通过数</p>
        <div ref="trendChart" class="chart"></div>
      </section>
    </div>

    <section class="card">
      <h2 class="sec-head">难度对比明细</h2>
      <el-table :data="stat.byDifficulty" v-loading="loading" row-key="difficulty" empty-text="暂无难度数据">
        <el-table-column prop="difficultyLabel" label="难度" width="100">
          <template #default="{ row }">
            <span class="oj-diff" :class="`diff-${row.difficulty}`">{{ row.difficultyLabel }}</span>
          </template>
        </el-table-column>
        <el-table-column label="在架已解 / 总题" width="150">
          <template #default="{ row }">
            <span class="tabular">{{ row.availableSolvedCount }} / {{ row.totalCount }}</span>
          </template>
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
              <span :class="rateClass(row.acceptRate)" class="tabular">{{ row.acceptRate }}%</span>
            </div>
          </template>
        </el-table-column>
      </el-table>
    </section>
  </div>
</template>

<script setup lang="ts">
import { computed, nextTick, onMounted, onUnmounted, ref } from 'vue'
import * as echarts from 'echarts'
import { getStatistics, getTrend, type Statistics } from '@/api/submission'
import { DIFFICULTY_MAP, cssVar } from '@/utils/status'

const emptyStat = (): Statistics => ({
  byDifficulty: [],
  byStatus: [],
  totalSubmit: 0,
  totalAccepted: 0,
  acceptRate: 0,
  solvedCount: 0,
  availableSolvedCount: 0,
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

/** 题目覆盖率：已解决 / 全站题数（保留一位小数） */
const coverageRate = computed(() =>
  totalProblems.value ? Math.round((stat.value.availableSolvedCount / totalProblems.value) * 1000) / 10 : 0)

/** 平均每解一题需要多少次提交（衡量效率，越低越好） */
const submitPerSolved = computed(() =>
  stat.value.solvedCount ? Math.round((stat.value.totalSubmit / stat.value.solvedCount) * 10) / 10 : 0)

/** 通过率三级配色类（与全局 .rate-good/.rate-mid/.rate-low 对应） */
function rateClass(rate: number) {
  if (rate >= 60) return 'rate-good'
  if (rate >= 30) return 'rate-mid'
  return 'rate-low'
}

/** 通过率三级配色值：图表需要真实色值，从设计 token 读，避免在 JS 里再写一份色板 */
function rateColor(rate: number) {
  if (rate >= 60) return cssVar('--oj-success', '#1d7a44')
  if (rate >= 30) return cssVar('--oj-warning', '#a26a15')
  return cssVar('--oj-danger', '#bc3f2c')
}

/** 各判题状态的配色：与 utils/status.ts 的 STATUS_COLOR 语义保持一致 */
function statusChartColor(status: number): string {
  switch (status) {
    case 2: return cssVar('--oj-success')
    case 3: return cssVar('--oj-warning')
    case 4: return cssVar('--oj-info')
    case 5: return cssVar('--oj-bronze-light')
    case 6: return cssVar('--oj-danger')
    case 7: return cssVar('--oj-silver')
    case 8: return cssVar('--oj-gray-900')
    default: return cssVar('--oj-gray-300')
  }
}

function renderCharts(trend: { date: string; total: number; accepted: number }[]) {
  const axisLine = cssVar('--oj-border', '#e6e2d3')
  const labelColor = cssVar('--oj-text-sub', '#5f6d62')
  const cardBg = cssVar('--oj-white', '#ffffff')

  // 状态分布：环形图（比柱状更省空间，且适合看占比）
  if (statusChart.value && stat.value.byStatus.length) {
    statusInstance = echarts.getInstanceByDom(statusChart.value) ?? echarts.init(statusChart.value)
    statusInstance.setOption({
      tooltip: { trigger: 'item', formatter: '{b}: {c} 次 ({d}%)' },
      legend: {
        bottom: 0,
        icon: 'circle',
        itemWidth: 9,
        itemHeight: 9,
        textStyle: { fontSize: 12, color: labelColor },
      },
      series: [
        {
          type: 'pie',
          radius: ['42%', '66%'],
          center: ['50%', '44%'],
          avoidLabelOverlap: true,
          itemStyle: { borderRadius: 6, borderColor: cardBg, borderWidth: 2 },
          label: { show: false },
          emphasis: { label: { show: true, fontSize: 14, fontWeight: 'bold' } },
          data: stat.value.byStatus.map((s) => ({
            name: s.statusDesc,
            value: s.count,
            itemStyle: { color: statusChartColor(s.status) },
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
      legend: {
        data: ['提交', '通过'],
        top: 0,
        icon: 'roundRect',
        itemWidth: 12,
        itemHeight: 8,
        textStyle: { color: labelColor },
      },
      grid: { left: 44, right: 20, top: 40, bottom: 28 },
      xAxis: {
        type: 'category',
        data: dates,
        axisTick: { show: false },
        axisLine: { lineStyle: { color: axisLine } },
        axisLabel: { color: labelColor },
      },
      yAxis: {
        type: 'value',
        minInterval: 1,
        axisLabel: { color: labelColor },
        splitLine: { lineStyle: { color: axisLine } },
      },
      series: [
        {
          name: '提交',
          type: 'bar',
          data: trend.map((t) => t.total),
          barWidth: '42%',
          itemStyle: { color: cssVar('--oj-primary-hover'), borderRadius: [4, 4, 0, 0] },
        },
        {
          name: '通过',
          type: 'line',
          data: trend.map((t) => t.accepted),
          smooth: true,
          symbol: 'circle',
          symbolSize: 6,
          itemStyle: { color: cssVar('--oj-success') },
          lineStyle: { width: 3 },
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
.two-col,
.diff-cards {
  margin-top: 4px;
}
.two-col {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 16px;
  margin-top: 16px;
}
.two-col .card {
  margin: 0;
}

/* 章节标题：左侧强调竖条，与题面章节一致 */
.sec-head {
  display: flex;
  align-items: center;
  gap: 8px;
  margin: 0 0 6px;
  font-size: 16px;
  font-weight: 600;
}
.sec-head::before {
  content: '';
  width: 4px;
  height: 15px;
  border-radius: 2px;
  background: var(--oj-primary);
}

.hint {
  margin: 0 0 14px;
  font-size: 12.5px;
  color: var(--oj-text-weak);
}

.diff-cards {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(240px, 1fr));
  gap: 14px;
}
.diff-card {
  background: var(--oj-gray-50);
  border: 1px solid var(--oj-border);
  border-radius: var(--oj-radius-md);
  padding: 16px 18px;
  transition: border-color var(--oj-duration) var(--oj-ease),
    box-shadow var(--oj-duration) var(--oj-ease);
}
.diff-card:hover {
  box-shadow: var(--oj-shadow-sm);
}
.diff-card.diff-1:hover {
  border-color: var(--oj-success-border);
}
.diff-card.diff-2:hover {
  border-color: var(--oj-warning-border);
}
.diff-card.diff-3:hover {
  border-color: var(--oj-danger-border);
}
.diff-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 12px;
}
.diff-count {
  font-size: 12.5px;
  color: var(--oj-gray-600);
  font-weight: 600;
  font-variant-numeric: tabular-nums;
}
.diff-foot {
  display: flex;
  flex-wrap: wrap;
  gap: 14px;
  margin-top: 12px;
  font-size: 12px;
  color: var(--oj-text-sub);
}
.diff-foot b {
  font-weight: 700;
  color: var(--oj-text);
  font-variant-numeric: tabular-nums;
}
.diff-foot b.rate-good,
.diff-foot b.rate-mid,
.diff-foot b.rate-low {
  font-weight: 700;
}
.diff-foot b.rate-good {
  color: var(--oj-success);
}
.diff-foot b.rate-mid {
  color: var(--oj-warning);
}
.diff-foot b.rate-low {
  color: var(--oj-danger);
}

.chart {
  width: 100%;
  height: 300px;
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

@media (max-width: 900px) {
  .two-col {
    grid-template-columns: 1fr;
  }
}
</style>
