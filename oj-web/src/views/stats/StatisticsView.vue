<template>
  <div class="report">
    <header class="report-head">
      <div class="report-title-block">
        <p class="kicker">STATISTICS</p>
        <h1 class="page-head-title">学习统计</h1>
        <p class="page-head-sub">难度覆盖、判题状态与七日趋势。数据按全部历史提交统计。</p>
      </div>
      <dl class="report-meta">
        <div class="meta-row">
          <dt>SCOPE</dt>
          <dd>全部历史提交</dd>
        </div>
        <div class="meta-row">
          <dt>PROBLEMS</dt>
          <dd class="mono">{{ totalProblems }} 道</dd>
        </div>
        <div class="meta-row">
          <dt>ATTEMPTED</dt>
          <dd class="mono">{{ stat.attemptedCount }} 道</dd>
        </div>
      </dl>
    </header>

    <!-- 摘要：四个读数 -->
    <section class="summary" aria-label="数据摘要">
      <div class="summary-item">
        <span class="summary-label">AC RATE</span>
        <span class="summary-value serif-num">{{ stat.acceptRate }}<small>%</small></span>
        <span class="summary-note">通过 {{ stat.totalAccepted }} / 提交 {{ stat.totalSubmit }}</span>
      </div>
      <div class="summary-item">
        <span class="summary-label">SOLVED</span>
        <span class="summary-value serif-num">{{ stat.solvedCount }}</span>
        <span class="summary-note">在所有在架题中</span>
      </div>
      <div class="summary-item">
        <span class="summary-label">COVERAGE</span>
        <span class="summary-value serif-num">{{ coverageRate }}<small>%</small></span>
        <span class="summary-note">已解 / 全站题数</span>
      </div>
      <div class="summary-item">
        <span class="summary-label">SUBMITS / SOLVE</span>
        <span class="summary-value serif-num">{{ submitPerSolved }}</span>
        <span class="summary-note">次提交 / 解决一题</span>
      </div>
    </section>

    <!-- 01 难度攻克 -->
    <section class="block">
      <div class="block-head">
        <span class="block-no">01</span>
        <h2 class="block-title">难度覆盖</h2>
        <span class="block-line" aria-hidden="true"></span>
        <span class="block-en">DIFFICULTY</span>
      </div>
      <p class="block-hint">进度条表示该难度下已通过的题占该难度在架题总数的比例。</p>

      <div class="diff-list">
        <div v-for="d in stat.byDifficulty" :key="d.difficulty" class="diff-row">
          <span class="diff-name">
            <span class="oj-diff" :class="`diff-${d.difficulty}`">{{ d.difficultyLabel }}</span>
          </span>
          <span class="diff-figure serif-num">
            {{ d.availableSolvedCount }}<i>/</i><small>{{ d.totalCount }}</small>
          </span>
          <span class="diff-track" aria-hidden="true">
            <span
              class="diff-fill"
              :class="`fill-${d.difficulty}`"
              :style="{ width: `${d.totalCount ? Math.round((d.availableSolvedCount / d.totalCount) * 100) : 0}%` }"
            />
          </span>
          <span class="diff-stats">
            <span>提交 <b class="tabular">{{ d.submit }}</b></span>
            <span>通过 <b class="tabular">{{ d.accepted }}</b></span>
            <span>通过率 <b :class="rateClass(d.acceptRate)" class="tabular">{{ d.acceptRate }}%</b></span>
          </span>
        </div>
        <el-empty v-if="!stat.byDifficulty.length && !loading" description="暂无难度数据" />
      </div>
    </section>

    <!-- 02/03 图表 -->
    <div class="chart-grid">
      <section class="block">
        <div class="block-head">
          <span class="block-no">02</span>
          <h2 class="block-title">判题状态分布</h2>
          <span class="block-line" aria-hidden="true"></span>
          <span class="block-en">VERDICTS</span>
        </div>
        <p class="block-hint">WA 多说明逻辑有问题，TLE 多说明复杂度不够。</p>
        <div v-show="stat.byStatus.length" ref="statusChart" class="chart"></div>
        <el-empty
          v-if="!loading && !stat.byStatus.length"
          description="还没有判题结果，提交一次代码后这里会显示分布"
        />
      </section>

      <section class="block">
        <div class="block-head">
          <span class="block-no">03</span>
          <h2 class="block-title">近 7 天提交趋势</h2>
          <span class="block-line" aria-hidden="true"></span>
          <span class="block-en">7 DAYS</span>
        </div>
        <p class="block-hint">柱状为提交数，折线为通过数。</p>
        <div ref="trendChart" class="chart"></div>
      </section>
    </div>

    <!-- 04 明细 -->
    <section class="block table-block">
      <div class="block-head">
        <span class="block-no">04</span>
        <h2 class="block-title">难度明细</h2>
        <span class="block-line" aria-hidden="true"></span>
        <span class="block-en">DETAIL</span>
      </div>
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
        <el-table-column label="通过率" min-width="220">
          <template #default="{ row }">
            <div class="tbl-rate">
              <span class="rate-track" aria-hidden="true">
                <span class="rate-fill" :style="{ width: `${row.acceptRate}%`, background: rateColor(row.acceptRate) }" />
              </span>
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
import { cssVar, statusColor } from '@/utils/status'

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
  if (rate >= 60) return cssVar('--oj-success', '#2c6b4b')
  if (rate >= 30) return cssVar('--oj-warning', '#9a681e')
  return cssVar('--oj-danger', '#a33326')
}

/** 各判题状态的配色：直接复用 utils/status.ts 的 statusColor，图表与全站同族同色不再漂移 */
function statusChartColor(status: number): string {
  return statusColor(status)
}

function renderCharts(trend: { date: string; total: number; accepted: number }[]) {
  const axisLine = cssVar('--oj-border', '#e4e8ee')
  const labelColor = cssVar('--oj-text-sub', '#667085')
  const cardBg = cssVar('--oj-card', '#ffffff')

  // 状态分布：环形图（比柱状更省空间，且适合看占比）
  if (statusChart.value && stat.value.byStatus.length) {
    statusInstance = echarts.getInstanceByDom(statusChart.value) ?? echarts.init(statusChart.value)
    statusInstance.setOption({
      tooltip: { trigger: 'item', formatter: '{b}: {c} 次 ({d}%)' },
      legend: {
        bottom: 0,
        icon: 'circle',
        itemWidth: 8,
        itemHeight: 8,
        itemGap: 14,
        textStyle: { fontSize: 11, color: labelColor },
      },
      series: [
        {
          type: 'pie',
          radius: ['44%', '68%'],
          center: ['50%', '43%'],
          avoidLabelOverlap: true,
          itemStyle: { borderRadius: 0, borderColor: cardBg, borderWidth: 2 },
          label: { show: false },
          emphasis: { label: { show: true, fontSize: 13, fontWeight: 'bold' } },
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
        icon: 'rect',
        itemWidth: 10,
        itemHeight: 6,
        textStyle: { color: labelColor, fontSize: 11 },
      },
      grid: { left: 40, right: 16, top: 36, bottom: 24 },
      xAxis: {
        type: 'category',
        data: dates,
        axisTick: { show: false },
        axisLine: { lineStyle: { color: axisLine } },
        axisLabel: { color: labelColor, fontSize: 10 },
      },
      yAxis: {
        type: 'value',
        minInterval: 1,
        axisLabel: { color: labelColor, fontSize: 10 },
        splitLine: { lineStyle: { color: axisLine, type: 'dotted' } },
      },
      series: [
        {
          name: '提交',
          type: 'bar',
          data: trend.map((t) => t.total),
          barWidth: '36%',
          itemStyle: { color: cssVar('--oj-primary'), borderRadius: 0 },
        },
        {
          name: '通过',
          type: 'line',
          data: trend.map((t) => t.accepted),
          smooth: true,
          symbol: 'circle',
          symbolSize: 5,
          itemStyle: { color: cssVar('--oj-success') },
          lineStyle: { width: 2 },
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
.report {
  max-width: 1280px;
  margin: 0 auto;
  padding: 44px 32px 64px;
}

.report-head {
  display: flex;
  align-items: flex-end;
  justify-content: space-between;
  gap: 32px;
  padding-bottom: 16px;
  border-bottom: 1px solid var(--oj-border);
  margin-bottom: 18px;
}
.report-title-block {
  min-width: 0;
}
.report-meta {
  display: flex;
  margin: 0;
  padding: 0;
  border-left: 1px solid var(--oj-border);
  flex-shrink: 0;
}
.meta-row {
  padding: 0 18px;
  border-right: 1px solid var(--oj-border);
}
.meta-row:last-child {
  border-right: 0;
  padding-right: 0;
}
.meta-row dt {
  font-family: var(--oj-font-mono);
  font-size: 9px;
  letter-spacing: 0.12em;
  color: var(--oj-text-weak);
}
.meta-row dd {
  margin: 5px 0 0;
  font-size: 12px;
  color: var(--oj-gray-800);
  white-space: nowrap;
}

.summary {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  overflow: hidden;
  background: var(--oj-card);
  border: 1px solid var(--oj-border);
  border-radius: 10px;
  box-shadow: var(--oj-shadow-sm);
}
.summary-item {
  display: flex;
  flex-direction: column;
  gap: 4px;
  padding: 18px 22px 16px;
  border-right: 1px solid var(--oj-border);
}
.summary-item:last-child {
  border-right: 0;
}
.summary-item + .summary-item {
  border-left: 0;
}
.summary-label {
  font-family: var(--oj-font-mono);
  font-size: 9px;
  letter-spacing: 0.1em;
  color: var(--oj-text-weak);
}
.summary-value {
  font-size: 34px;
  line-height: 1.05;
  color: var(--oj-text);
}
.summary-value small {
  font-size: 14px;
  color: var(--oj-text-sub);
  margin-left: 2px;
}
.summary-note {
  font-size: 10px;
  color: var(--oj-text-weak);
}

.block {
  margin-top: 18px;
  padding: 18px 20px 20px;
  background: var(--oj-card);
  border: 1px solid var(--oj-border);
  border-radius: 10px;
  box-shadow: var(--oj-shadow-sm);
}
.block-head {
  display: flex;
  align-items: baseline;
  gap: 12px;
  padding-bottom: 10px;
  border-bottom: 1px solid var(--oj-border);
}
.block-no {
  font-family: var(--oj-font-mono);
  font-size: 10px;
  color: var(--oj-primary);
}
.block-title {
  margin: 0;
  font-size: 18px;
  font-weight: 750;
  letter-spacing: -0.03em;
}
.block-line {
  flex: 1;
  border-top: 1px solid var(--oj-border);
}
.block-en {
  font-family: var(--oj-font-mono);
  font-size: 9px;
  letter-spacing: 0.16em;
  color: var(--oj-text-weak);
}
.block-hint {
  margin: 8px 0 20px;
  font-size: 11.5px;
  color: var(--oj-text-weak);
}

/* 难度：条状读数 */
.diff-list {
  border-top: 0;
}
.diff-row {
  display: grid;
  grid-template-columns: 80px 110px minmax(0, 1fr) 260px;
  gap: 24px;
  align-items: center;
  padding: 13px 0;
  border-bottom: 1px solid var(--oj-border);
}
.diff-row:last-child {
  border-bottom: 0;
}
.diff-name {
  font-size: 12.5px;
}
.diff-figure {
  font-size: 24px;
  line-height: 1;
  color: var(--oj-text);
}
.diff-figure i {
  font-style: normal;
  color: var(--oj-gray-300);
  margin: 0 3px;
  font-size: 14px;
}
.diff-figure small {
  font-size: 13px;
  color: var(--oj-text-sub);
}
.diff-track {
  height: 6px;
  border-radius: 99px;
  background: var(--oj-gray-100);
  display: block;
  overflow: hidden;
}
.diff-fill {
  display: block;
  height: 100%;
  border-radius: 99px;
  transition: width 0.3s var(--oj-ease);
}
.fill-1 { background: var(--oj-success); }
.fill-2 { background: var(--oj-warning); }
.fill-3 { background: var(--oj-danger); }
.diff-stats {
  display: flex;
  justify-content: flex-end;
  gap: 14px;
  font-size: 10px;
  color: var(--oj-text-sub);
}
.diff-stats b {
  font-weight: 600;
  color: var(--oj-text);
}

.chart-grid {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 18px;
  margin-top: 0;
}
.chart-grid .block {
  margin-top: 18px;
}
.chart {
  width: 100%;
  height: 300px;
}
.table-block {
  padding: 0;
  overflow: hidden;
}
.table-block .block-head {
  padding: 16px 20px 12px;
}

.tbl-rate {
  display: flex;
  align-items: center;
  gap: 12px;
}
.rate-track {
  flex: 1;
  max-width: 150px;
  height: 2px;
  background: var(--oj-gray-200);
}
.rate-fill {
  display: block;
  height: 100%;
}

@media (max-width: 1000px) {
  .report-head {
    flex-direction: column;
    align-items: flex-start;
  }
  .report-meta {
    border-left: 0;
    border-top: 1px solid var(--oj-border);
    padding-top: 12px;
    width: 100%;
    justify-content: space-between;
  }
  .meta-row {
    padding: 0 12px 0 0;
  }
  .summary {
    grid-template-columns: 1fr 1fr;
  }
  .summary-item:nth-child(2) {
    border-right: 0;
  }
  .summary-item:nth-child(3) {
    border-top: 1px solid var(--oj-border);
  }
  .summary-item:nth-child(4) {
    border-top: 1px solid var(--oj-border);
    border-right: 0;
  }
  .diff-row {
    grid-template-columns: 70px 1fr;
    gap: 10px 16px;
  }
  .diff-track {
    grid-column: 2;
  }
  .diff-stats {
    grid-column: 2;
    justify-content: flex-start;
  }
  .chart-grid {
    grid-template-columns: 1fr;
    gap: 0;
  }
}
</style>
