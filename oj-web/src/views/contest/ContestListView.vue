<template>
  <div class="programme">
    <header class="page-head programme-head">
      <div>
        <p class="kicker">CONTESTS</p>
        <h1 class="page-head-title">竞赛</h1>
        <p class="page-head-sub">限时竞赛 · ACM 赛制 · 解题数优先，同数按总罚时排序。</p>
      </div>
      <dl class="season" aria-label="赛季统计">
        <div class="season-item">
          <dt>进行中</dt>
          <dd>{{ runningCount }}</dd>
        </div>
        <div class="season-item">
          <dt>未开始</dt>
          <dd>{{ upcomingCount }}</dd>
        </div>
        <div class="season-note">
          <span>ACM 赛制</span>
          <span>按罚时排序</span>
        </div>
      </dl>
    </header>

    <!-- 进行中：深色实时面板 -->
    <template v-if="liveContests.length">
      <router-link v-for="c in liveContests" :key="c.id" :to="`/contests/${c.id}`" class="live-poster">
        <div class="poster-left">
          <div class="poster-status">
            <span class="poster-live">进行中</span>
            <span class="poster-no">#{{ String(c.id).padStart(3, '0') }}</span>
          </div>
          <h2 class="poster-title">{{ c.title }}</h2>
          <p class="poster-desc">{{ c.description || '暂无说明' }}</p>
          <div class="poster-meta">
            <span>{{ formatDateTime(c.startTime) }} 起</span>
            <span class="dot" aria-hidden="true">·</span>
            <span>{{ formatDateTime(c.endTime) }} 止</span>
            <span class="dot" aria-hidden="true">·</span>
            <span>{{ c.problemCount }} 道题</span>
          </div>
          <span class="poster-cta">进入竞赛</span>
        </div>
        <div class="poster-right">
          <span class="countdown-label">剩余时间</span>
          <span class="countdown-value serif-num">{{ countdownOf(c) }}</span>
          <span class="countdown-note">秒级刷新 · 罚时按提交时间计算</span>
        </div>
      </router-link>
    </template>

    <!-- 其余竞赛：赛程表 -->
    <section v-if="otherContests.length || liveContests.length" class="schedule-card">
      <div class="schedule-head">
        <div class="schedule-title">
          <span class="kicker">SCHEDULE</span>
          <h2>全部赛程</h2>
        </div>
        <div class="filters" role="group" aria-label="竞赛状态筛选">
          <button
            v-for="f in FILTERS"
            :key="String(f.value)"
            class="filter-btn"
            :class="{ active: filter === f.value }"
            :aria-pressed="filter === f.value"
            type="button"
            @click="filter = f.value"
          >
            {{ f.label }}
          </button>
        </div>
      </div>

      <div class="schedule-cols" aria-hidden="true">
        <span>日期</span>
        <span>状态</span>
        <span>竞赛</span>
        <span>题量</span>
        <span>倒计时</span>
      </div>

      <router-link v-for="c in filteredOthers" :key="c.id" :to="`/contests/${c.id}`" class="schedule-row">
        <div class="date-block">
          <span class="date-day serif-num">{{ formatDateTime(c.startTime).slice(8, 10) }}</span>
          <span class="date-month">{{ formatDateTime(c.startTime).slice(5, 7) }}月</span>
        </div>
        <span class="row-status" :class="`st-${c.status}`">
          {{ STATUS[c.status]?.label ?? '—' }}
        </span>
        <div class="row-main">
          <p class="row-title">{{ c.title }}</p>
          <p class="row-desc">{{ c.description || '暂无说明' }}</p>
        </div>
        <span class="row-count">{{ c.problemCount }} 题</span>
        <span class="row-extra">
          <template v-if="c.status === 0">{{ countdownOf(c) }} 后开始</template>
          <template v-else>已结束</template>
        </span>
      </router-link>

      <el-empty v-if="!filteredOthers.length" description="该分类下暂无竞赛" :image-size="64" />
    </section>

    <div v-if="!loading && !contests.length" class="contest-empty">
      <el-empty description="还没有竞赛，管理员创建后就会出现在这里" :image-size="72" />
    </div>
  </div>
</template>

<script setup lang="ts">
// 竞赛列表（论文 7.2 展望落地：竞赛模式）
import { computed, onMounted, onUnmounted, ref } from 'vue'
import { listContests, type ContestVO } from '@/api/contest'
import { formatDateTime, parseServerTime } from '@/utils/datetime'
import { CONTEST_STATUS_MAP } from '@/utils/status'

const contests = ref<ContestVO[]>([])
const loading = ref(true)
const filter = ref<number | null>(null)
let timer: number | undefined

// 行内状态文案统一走 status.ts 的竞赛状态映射
const STATUS = CONTEST_STATUS_MAP

const FILTERS = [
  { label: '全部', value: null as number | null },
  { label: '未开始', value: 0 },
  { label: '已结束', value: 2 },
]

const liveContests = computed(() => contests.value.filter((c) => c.status === 1))
const otherContests = computed(() => contests.value.filter((c) => c.status !== 1))
const filteredOthers = computed(() =>
  filter.value === null ? otherContests.value : otherContests.value.filter((c) => c.status === filter.value),
)
const runningCount = computed(() => liveContests.value.length)
const upcomingCount = computed(() => contests.value.filter((c) => c.status === 0).length)

// 秒级刷新倒计时（ref 才会触发重渲染）
const now = ref(Date.now())
function refreshCountdown() {
  now.value = Date.now()
}

/** 倒计时：进行中显示距结束、未开始显示距开始 */
function countdownOf(c: ContestVO) {
  void now.value
  const d0 = parseServerTime(c.status === 1 ? c.endTime : c.startTime)
  if (!d0) return '—'
  const diff = d0.getTime() - now.value
  if (diff <= 0) return '00:00:00'
  const d = Math.floor(diff / 86400000)
  const h = Math.floor((diff % 86400000) / 3600000)
  const m = Math.floor((diff % 3600000) / 60000)
  const s = Math.floor((diff % 60000) / 1000)
  const pad = (n: number) => String(n).padStart(2, '0')
  return d > 0 ? `${d}天 ${pad(h)}:${pad(m)}:${pad(s)}` : `${pad(h)}:${pad(m)}:${pad(s)}`
}

async function load() {
  loading.value = true
  try {
    contests.value = await listContests()
  } finally {
    loading.value = false
  }
}

onMounted(() => {
  load()
  timer = window.setInterval(refreshCountdown, 1000)
})

onUnmounted(() => {
  if (timer) clearInterval(timer)
})
</script>

<style scoped>
.programme {
  max-width: 1120px;
  margin: 0 auto;
  padding: 36px 32px 64px;
}

.programme-head {
  margin-bottom: 24px;
}
.season {
  display: flex;
  align-items: stretch;
  margin: 0;
  padding: 0;
  flex-shrink: 0;
}
.season-item {
  padding: 0 20px;
  border-left: 1px solid var(--oj-border);
  text-align: right;
}
.season-item dt {
  font-size: 10.5px;
  color: var(--oj-text-weak);
}
.season-item dd {
  margin: 4px 0 0;
  font-family: var(--oj-font-mono);
  font-size: 22px;
  font-weight: 600;
  line-height: 1;
}
.season-note {
  display: flex;
  flex-direction: column;
  justify-content: center;
  padding-left: 16px;
  border-left: 1px solid var(--oj-border);
  font-size: 10.5px;
  line-height: 1.9;
  color: var(--oj-text-weak);
}

/* ---------- 进行中：深色实时面板 ---------- */
.live-poster {
  display: grid;
  grid-template-columns: minmax(0, 1fr) 280px;
  background: var(--oj-code-bg);
  color: #fff;
  border: 1px solid #1f2937;
  border-radius: 4px;
  margin-bottom: 16px;
  overflow: hidden;
  box-shadow: none;
  transition: border-color var(--oj-duration) var(--oj-ease);
}
.live-poster:hover {
  border-color: var(--oj-primary);
}
.poster-left {
  padding: 26px 28px 24px;
  min-width: 0;
}
.poster-status {
  display: flex;
  align-items: center;
  gap: 10px;
  font-size: 10.5px;
  color: #98a2b3;
}
.poster-live {
  color: #57d9a3;
  font-weight: 600;
}
.poster-no {
  margin-left: auto;
  font-family: var(--oj-font-mono);
  color: #667085;
}
.poster-title {
  margin: 14px 0 0;
  font-size: clamp(24px, 2.6vw, 30px);
  font-weight: 750;
  line-height: 1.3;
  letter-spacing: -0.04em;
  color: #f9fafb;
}
.poster-desc {
  margin: 9px 0 0;
  max-width: 42em;
  font-size: 12px;
  line-height: 1.8;
  color: #98a2b3;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
}
.poster-meta {
  display: flex;
  flex-wrap: wrap;
  gap: 7px;
  margin-top: 14px;
  font-family: var(--oj-font-mono);
  font-size: 9.5px;
  letter-spacing: 0.03em;
  color: #667085;
}
.poster-meta .dot {
  opacity: 0.5;
}
.poster-cta {
  display: inline-flex;
  align-items: center;
  gap: 5px;
  margin-top: 18px;
  padding: 6px 10px;
  border: 1px solid rgba(255, 255, 255, 0.16);
  border-radius: 5px;
  background: rgba(255, 255, 255, 0.05);
  font-size: 11px;
  letter-spacing: 0.02em;
  color: #e5e7eb;
  transition: all var(--oj-duration) var(--oj-ease);
}
.live-poster:hover .poster-cta {
  border-color: var(--oj-primary);
  background: var(--oj-primary);
  color: #fff;
}

.poster-right {
  position: relative;
  border-left: 1px solid #1f2937;
  padding: 26px 26px 24px;
  display: flex;
  flex-direction: column;
  justify-content: center;
  background: rgba(255, 255, 255, 0.02);
}
.countdown-label {
  font-family: var(--oj-font-mono);
  font-size: 8.5px;
  letter-spacing: 0.14em;
  color: #667085;
}
.countdown-value {
  margin-top: 8px;
  font-size: 30px;
  line-height: 1.15;
  color: #f9fafb;
  font-variant-numeric: tabular-nums;
}
.countdown-note {
  margin-top: 9px;
  font-family: var(--oj-font-mono);
  font-size: 8.5px;
  letter-spacing: 0.03em;
  color: #667085;
}

/* ---------- 赛程表 ---------- */
.contest-empty {
  margin-top: 18px;
  padding: 48px 24px;
  background: var(--oj-card);
  border: 1px solid var(--oj-border);
  border-radius: 4px;
}
.schedule-card {
  margin-top: 18px;
  background: var(--oj-card);
  border: 1px solid var(--oj-border);
  border-radius: 4px;
  overflow: hidden;
}
.schedule-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
  padding: 13px 16px;
  border-bottom: 1px solid var(--oj-border);
}
.schedule-title {
  display: flex;
  align-items: baseline;
  gap: 10px;
}
.schedule-title h2 {
  margin: 0;
  font-size: 16px;
  font-weight: 700;
  letter-spacing: -0.02em;
}
.filters {
  display: flex;
  gap: 16px;
}
.filter-btn {
  padding: 4px 0;
  border: 0;
  border-bottom: 1px solid transparent;
  background: transparent;
  color: var(--oj-text-sub);
  font-family: inherit;
  font-size: 11.5px;
  cursor: pointer;
  transition: color var(--oj-duration) var(--oj-ease), border-color var(--oj-duration) var(--oj-ease);
}
.filter-btn:hover {
  color: var(--oj-text);
}
.filter-btn.active {
  border-bottom-color: var(--oj-text);
  color: var(--oj-text);
  font-weight: 600;
}

.schedule-cols {
  display: grid;
  grid-template-columns: 72px 76px minmax(0, 1fr) 60px 166px;
  gap: 18px;
  align-items: center;
  padding: 8px 16px 7px;
  border-bottom: 1px solid var(--oj-border);
  font-size: 10px;
  color: var(--oj-text-weak);
}
.schedule-row {
  display: grid;
  grid-template-columns: 72px 76px minmax(0, 1fr) 60px 166px;
  gap: 18px;
  align-items: center;
  padding: 11px 16px;
  border-bottom: 1px solid var(--oj-border);
  transition: background var(--oj-duration) var(--oj-ease);
}
.schedule-row:last-of-type {
  border-bottom: 0;
}
.schedule-row:hover {
  background: var(--oj-gray-50);
}
.date-block {
  display: flex;
  align-items: baseline;
  gap: 4px;
}
.date-day {
  font-family: var(--oj-font-mono);
  font-size: 21px;
  font-weight: 600;
  line-height: 1;
  color: var(--oj-text);
}
.date-month {
  font-family: var(--oj-font-mono);
  font-size: 8.5px;
  color: var(--oj-text-weak);
}
.row-status {
  width: max-content;
  font-size: 10.5px;
  color: var(--oj-text-sub);
}
.st-0 { color: var(--oj-primary); }
.st-1 { color: var(--oj-success); }
.st-2 { color: var(--oj-text-weak); }
.st-2 { color: var(--oj-text-weak); }
.row-main {
  min-width: 0;
}
.row-title {
  font-size: 13.5px;
  font-weight: 650;
  color: var(--oj-text);
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}
.row-desc {
  margin: 3px 0 0;
  font-size: 11px;
  color: var(--oj-text-sub);
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}
.row-count {
  font-family: var(--oj-font-mono);
  font-size: 10.5px;
  color: var(--oj-text-sub);
}
.row-extra {
  font-family: var(--oj-font-mono);
  font-size: 9.5px;
  color: var(--oj-text-weak);
  text-align: right;
  white-space: nowrap;
}

@media (max-width: 980px) {
  .live-poster {
    grid-template-columns: 1fr;
  }
  .poster-right {
    border-left: 0;
    border-top: 1px solid rgba(255, 253, 247, 0.16);
  }
  .poster-seal {
    display: none;
  }
  .schedule-cols {
    display: none;
  }
  .schedule-row {
    grid-template-columns: 64px 76px minmax(0, 1fr);
    gap: 12px;
  }
  .row-count,
  .row-extra {
    display: none;
  }
}
@media (max-width: 720px) {
  .programme {
    padding: 28px 18px 56px;
  }
  .season {
    border-left: 0;
    border-top: 1px solid var(--oj-border);
    padding-top: 12px;
    width: 100%;
    justify-content: space-between;
  }
  .season-item {
    text-align: left;
    padding: 0 14px 0 0;
  }
  .season-note {
    display: none;
  }
  .schedule-head {
    flex-direction: column;
    align-items: flex-start;
  }
  .schedule-row {
    grid-template-columns: 56px minmax(0, 1fr);
  }
  .row-status {
    display: none;
  }
  .poster-left {
    padding: 24px 22px 22px;
  }
  .poster-title {
    font-size: 24px;
    text-wrap: balance;
  }
  .poster-right {
    padding: 22px;
  }
}
</style>
