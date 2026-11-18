<template>
  <div class="page-container">
    <!-- 页头：标题 + 实时状态统计 -->
    <header class="page-head">
      <div>
        <h1>竞赛</h1>
        <p class="sub">限时竞赛 · ACM 赛制 · 解题数优先，同解题数按总罚时排序</p>
      </div>
      <div class="head-stat">
        <div class="hs-item">
          <span class="hs-num">{{ runningCount }}</span>
          <span class="hs-label">进行中</span>
        </div>
        <div class="hs-sep" aria-hidden="true" />
        <div class="hs-item">
          <span class="hs-num">{{ upcomingCount }}</span>
          <span class="hs-label">即将开始</span>
        </div>
      </div>
    </header>

    <!-- 进行中优先展示（强调大卡片） -->
    <template v-if="liveContests.length">
      <router-link v-for="c in liveContests" :key="c.id" :to="`/contests/${c.id}`" class="live-card">
        <div class="live-left">
          <span class="live-badge">
            <span class="pulse" aria-hidden="true" />
            进行中
          </span>
          <h2 class="live-title">{{ c.title }}</h2>
          <p class="live-desc">{{ c.description || '暂无说明' }}</p>
          <div class="live-meta">
            <span class="meta-item">
              <svg viewBox="0 0 24 24" width="14" height="14" fill="none" stroke="currentColor" stroke-width="2" aria-hidden="true">
                <circle cx="12" cy="12" r="9" /><path d="M12 7v5l3 2" />
              </svg>
              {{ formatDateTime(c.startTime) }} 起
            </span>
            <span class="meta-item">
              <svg viewBox="0 0 24 24" width="14" height="14" fill="none" stroke="currentColor" stroke-width="2" aria-hidden="true">
                <path d="M4 6h16M4 12h10M4 18h7" />
              </svg>
              {{ c.problemCount }} 道题
            </span>
          </div>
        </div>
        <div class="live-right">
          <div class="countdown-label">距结束</div>
          <div class="countdown-value" :aria-label="`距结束 ${countdownOf(c)}`">{{ countdownOf(c) }}</div>
          <span class="live-btn">进入竞赛</span>
        </div>
      </router-link>
    </template>

    <!-- 其余竞赛：紧凑列表 -->
    <section v-if="otherContests.length" class="card list-card">
      <div class="list-head">
        <h2 class="list-title">全部竞赛</h2>
        <div class="filters" role="group" aria-label="竞赛状态筛选">
          <button
            v-for="f in FILTERS" :key="String(f.value)"
            class="filter-btn" :class="{ active: filter === f.value }"
            type="button" :aria-pressed="filter === f.value"
            @click="filter = f.value"
          >{{ f.label }}</button>
        </div>
      </div>

      <router-link v-for="c in filteredOthers" :key="c.id" :to="`/contests/${c.id}`" class="row">
        <span class="row-status" :class="`st-${c.status}`">
          {{ STATUS[c.status]?.label ?? '—' }}
        </span>
        <div class="row-main">
          <div class="row-title">{{ c.title }}</div>
          <div class="row-desc">{{ c.description || '暂无说明' }}</div>
        </div>
        <div class="row-meta">
          <div class="row-time">{{ formatDateTime(c.startTime) }}</div>
          <div class="row-count">{{ c.problemCount }} 题</div>
        </div>
        <div class="row-extra">
          <span v-if="c.status === 0" class="cd">{{ countdownOf(c) }} 后开始</span>
          <span v-else class="cd muted">已结束</span>
        </div>
        <svg class="row-arrow" viewBox="0 0 24 24" width="16" height="16" fill="none" stroke="currentColor" stroke-width="2" aria-hidden="true">
          <path d="M9 6l6 6-6 6" />
        </svg>
      </router-link>

      <el-empty v-if="!filteredOthers.length" description="该分类下暂无竞赛" :image-size="80" />
    </section>

    <el-empty
      v-if="!loading && !contests.length"
      description="还没有竞赛，管理员创建后就会出现在这里"
    />
  </div>
</template>

<script setup lang="ts">
// 竞赛列表（论文 7.2 展望落地：竞赛模式）
import { computed, onMounted, onUnmounted, ref } from 'vue'
import { listContests, type ContestVO } from '@/api/contest'
import { formatDateTime, parseServerTime } from '@/utils/datetime'

const contests = ref<ContestVO[]>([])
const loading = ref(true)
const filter = ref<number | null>(null)
let timer: number | undefined

const STATUS: Record<number, { label: string }> = {
  0: { label: '未开始' },
  1: { label: '进行中' },
  2: { label: '已结束' },
}

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

onMounted(async () => {
  try {
    contests.value = await listContests()
  } finally {
    loading.value = false
  }
  timer = window.setInterval(refreshCountdown, 1000)
})

onUnmounted(() => window.clearInterval(timer))
</script>

<style scoped>
.page-head {
  display: flex;
  align-items: flex-end;
  justify-content: space-between;
  gap: 16px;
  flex-wrap: wrap;
  padding: 4px 2px 18px;
}
.page-head h1 {
  margin: 0;
  font-size: 24px;
  font-weight: 600;
  letter-spacing: -0.3px;
  color: var(--oj-text);
}
.sub {
  margin: 6px 0 0;
  color: var(--oj-text-sub);
  font-size: 13px;
}
.head-stat {
  display: flex;
  align-items: center;
  gap: 20px;
}
.hs-item {
  text-align: right;
}
.hs-num {
  display: block;
  font-family: var(--oj-font-mono);
  font-size: 22px;
  font-weight: 600;
  font-variant-numeric: tabular-nums;
  color: var(--oj-text);
  line-height: 1.1;
}
.hs-label {
  font-size: 11.5px;
  color: var(--oj-text-sub);
}
.hs-sep {
  width: 1px;
  height: 28px;
  background: var(--oj-border);
}

/* ---------- 进行中：强调卡（整卡可键盘聚焦，链接语义） ----------
   平面强调：白底 + 3px 绿色左边线 + 绿色呼吸点。
   不再用整块渐变底——强调要靠位置和一根线，而不是一块大色板。 */
.live-card {
  position: relative;
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 24px;
  padding: 20px 24px;
  margin-bottom: 16px;
  border: 1px solid var(--oj-border);
  border-left: 3px solid var(--oj-success);
  border-radius: var(--oj-radius);
  background: var(--oj-card);
  color: var(--oj-text);
  transition: border-color var(--oj-duration) var(--oj-ease);
}
.live-card:hover {
  border-color: var(--oj-border-strong);
}
.live-left {
  position: relative;
  min-width: 0;
}
.live-badge {
  display: inline-flex;
  align-items: center;
  gap: 7px;
  font-size: 12px;
  font-weight: 600;
  color: var(--oj-success);
  letter-spacing: 0.2px;
}
.pulse {
  width: 6px;
  height: 6px;
  border-radius: 50%;
  background: var(--oj-pulse-dot);
  box-shadow: 0 0 0 0 var(--oj-pulse-ring);
  animation: pulse 1.8s infinite;
}
@keyframes pulse {
  70% {
    box-shadow: 0 0 0 6px var(--oj-pulse-ring-out);
  }
  100% {
    box-shadow: 0 0 0 0 var(--oj-pulse-ring-out);
  }
}
.live-title {
  margin: 10px 0 0;
  font-size: 18px;
  font-weight: 600;
  letter-spacing: -0.1px;
}
.live-desc {
  margin: 6px 0 0;
  font-size: 13px;
  color: var(--oj-text-sub);
  max-width: 520px;
  line-height: 1.6;
}
.live-meta {
  display: flex;
  flex-wrap: wrap;
  gap: 18px;
  margin-top: 10px;
}
.meta-item {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  font-size: 12.5px;
  color: var(--oj-text-sub);
}
.live-right {
  position: relative;
  text-align: right;
  flex-shrink: 0;
}
.countdown-label {
  font-size: 11.5px;
  color: var(--oj-text-sub);
  letter-spacing: 0.3px;
}
.countdown-value {
  font-family: var(--oj-font-mono);
  font-size: 26px;
  font-weight: 600;
  font-variant-numeric: tabular-nums;
  letter-spacing: 0.3px;
  margin: 2px 0 12px;
  color: var(--oj-text);
}
.live-btn {
  display: inline-block;
  padding: 7px 16px;
  border-radius: var(--oj-radius-md);
  background: var(--oj-primary);
  color: var(--oj-white);
  font-size: 13px;
  font-weight: 500;
  transition: background 0.15s var(--oj-ease);
}
.live-card:hover .live-btn {
  background: var(--oj-primary-hover);
}

/* ---------- 其余竞赛：紧凑列表 ---------- */
.list-card {
  padding: 20px 0 6px;
}
.list-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  flex-wrap: wrap;
  padding: 0 24px 14px;
  border-bottom: 1px solid var(--oj-border);
}
.list-title {
  margin: 0;
  font-size: 15px;
  font-weight: 700;
}
.filters {
  display: flex;
  gap: 4px;
  background: var(--oj-gray-50);
  border: 1px solid var(--oj-border);
  padding: 3px;
  border-radius: var(--oj-radius-sm);
}
.filter-btn {
  padding: 5px 14px;
  border: none;
  border-radius: 6px;
  background: transparent;
  color: var(--oj-text-sub);
  font-family: inherit;
  font-size: 12.5px;
  font-weight: 500;
  cursor: pointer;
  transition: color var(--oj-duration) var(--oj-ease),
    background var(--oj-duration) var(--oj-ease);
}
.filter-btn:hover {
  color: var(--oj-text);
}
.filter-btn.active {
  background: var(--oj-white);
  color: var(--oj-primary);
  font-weight: 600;
  box-shadow: var(--oj-shadow-sm);
}

.row {
  display: flex;
  align-items: center;
  gap: 16px;
  padding: 15px 24px;
  border-bottom: 1px solid var(--oj-border);
  transition: background 0.16s var(--oj-ease);
}
.row:last-child {
  border-bottom: none;
}
.row:hover {
  background: var(--oj-primary-soft);
}
.row:hover .row-arrow {
  color: var(--oj-primary);
  transform: translateX(2px);
}
.row-status {
  flex-shrink: 0;
  width: 62px;
  padding: 3px 0;
  border-radius: 6px;
  font-size: 11.5px;
  font-weight: 600;
  text-align: center;
}
.st-0 {
  background: var(--oj-warning-soft);
  color: var(--oj-warning);
}
.st-2 {
  background: var(--oj-gray-50);
  color: var(--oj-gray-500);
}
.row-main {
  flex: 1;
  min-width: 0;
}
.row-title {
  font-size: 14.5px;
  font-weight: 600;
  color: var(--oj-text);
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}
.row-desc {
  margin-top: 3px;
  font-size: 12.5px;
  color: var(--oj-text-sub);
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}
.row-meta {
  flex-shrink: 0;
  text-align: right;
  width: 150px;
}
.row-time {
  font-size: 12.5px;
  color: var(--oj-text-sub);
  font-variant-numeric: tabular-nums;
}
.row-count {
  margin-top: 2px;
  font-size: 11.5px;
  color: var(--oj-text-weak);
}
.row-extra {
  flex-shrink: 0;
  width: 120px;
  text-align: right;
}
.cd {
  font-family: var(--oj-font-mono);
  font-size: 12px;
  color: var(--oj-warning);
  font-weight: 600;
  font-variant-numeric: tabular-nums;
}
.cd.muted {
  color: var(--oj-text-sub);
  font-family: inherit;
  font-weight: 400;
}
.row-arrow {
  flex-shrink: 0;
  color: var(--oj-text-weak);
  transition: color 0.16s var(--oj-ease), transform 0.16s var(--oj-ease);
}

@media (max-width: 820px) {
  .live-card {
    flex-direction: column;
    align-items: flex-start;
  }
  .live-right {
    text-align: left;
    width: 100%;
  }
  .row-meta,
  .row-extra {
    display: none;
  }
}
</style>
