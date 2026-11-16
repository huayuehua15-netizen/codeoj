<template>
  <div class="page-container">
    <!-- 页头 -->
    <div class="page-head">
      <div>
        <h1>竞赛</h1>
        <p class="sub">限时竞赛 · ACM 赛制 · 解题数优先，同解题数按总罚时排序</p>
      </div>
      <div class="head-stat">
        <div class="hs-item">
          <span class="hs-num">{{ runningCount }}</span>
          <span class="hs-label">进行中</span>
        </div>
        <div class="hs-sep" />
        <div class="hs-item">
          <span class="hs-num">{{ upcomingCount }}</span>
          <span class="hs-label">即将开始</span>
        </div>
      </div>
    </div>

    <!-- 进行中优先展示（大卡片） -->
    <template v-if="liveContests.length">
      <div v-for="c in liveContests" :key="c.id" class="live-card" @click="go(c)">
        <div class="live-glow" />
        <div class="live-left">
          <div class="live-badge">
            <span class="pulse" />
            进行中
          </div>
          <h2 class="live-title">{{ c.title }}</h2>
          <p class="live-desc">{{ c.description || '暂无说明' }}</p>
          <div class="live-meta">
            <span class="meta-item">
              <svg viewBox="0 0 24 24" width="14" height="14" fill="none" stroke="currentColor" stroke-width="2">
                <circle cx="12" cy="12" r="9" /><path d="M12 7v5l3 2" />
              </svg>
              {{ formatDateTime(c.startTime) }} 起
            </span>
            <span class="meta-item">
              <svg viewBox="0 0 24 24" width="14" height="14" fill="none" stroke="currentColor" stroke-width="2">
                <path d="M4 6h16M4 12h10M4 18h7" />
              </svg>
              {{ c.problemCount }} 道题
            </span>
          </div>
        </div>
        <div class="live-right">
          <div class="countdown-label">距结束</div>
          <div class="countdown-value">{{ countdownOf(c) }}</div>
          <button class="live-btn" @click.stop="go(c)">进入竞赛</button>
        </div>
      </div>
    </template>

    <!-- 其余竞赛：紧凑列表 -->
    <div v-if="otherContests.length" class="card list-card">
      <div class="list-head">
        <h3>全部竞赛</h3>
        <div class="filters">
          <button
            v-for="f in FILTERS" :key="String(f.value)"
            class="filter-btn" :class="{ active: filter === f.value }"
            @click="filter = f.value"
          >{{ f.label }}</button>
        </div>
      </div>

      <div v-for="c in filteredOthers" :key="c.id" class="row" @click="go(c)">
        <div class="row-status" :class="`st-${c.status}`">
          {{ STATUS[c.status]?.label ?? '—' }}
        </div>
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
        <svg class="row-arrow" viewBox="0 0 24 24" width="16" height="16" fill="none" stroke="currentColor" stroke-width="2">
          <path d="M9 6l6 6-6 6" />
        </svg>
      </div>

      <el-empty v-if="!filteredOthers.length" description="该分类下暂无竞赛" :image-size="80" />
    </div>

    <el-empty v-if="!loading && !contests.length" description="暂无竞赛" />
  </div>
</template>

<script setup lang="ts">
// 竞赛列表（论文 7.2 展望落地：竞赛模式）
import { computed, onMounted, onUnmounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import { listContests, type ContestVO } from '@/api/contest'
import { formatDateTime, parseServerTime } from '@/utils/datetime'

const router = useRouter()
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

function go(c: ContestVO) {
  router.push(`/contests/${c.id}`)
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
  padding: 4px 2px 18px;
}
.page-head h1 {
  margin: 0;
  font-size: 24px;
  font-weight: 800;
  letter-spacing: -0.3px;
  color: var(--oj-text);
}
.sub {
  margin: 6px 0 0;
  color: var(--oj-text-weak);
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
  font-size: 22px;
  font-weight: 800;
  font-variant-numeric: tabular-nums;
  color: var(--oj-primary);
  line-height: 1.1;
}
.hs-label {
  font-size: 11.5px;
  color: var(--oj-text-weak);
}
.hs-sep {
  width: 1px;
  height: 28px;
  background: var(--oj-border);
}

/* ---------- 进行中：强调卡 ---------- */
.live-card {
  position: relative;
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 24px;
  padding: 24px 28px;
  margin-bottom: 16px;
  border-radius: var(--oj-radius);
  background: linear-gradient(135deg, #312e81 0%, #4338ca 55%, #4f46e5 100%);
  color: #fff;
  cursor: pointer;
  overflow: hidden;
  box-shadow: 0 8px 28px -10px rgba(67, 56, 202, 0.55);
  transition: transform 0.25s var(--oj-ease), box-shadow 0.25s var(--oj-ease);
}
.live-card:hover {
  transform: translateY(-2px);
  box-shadow: 0 14px 36px -12px rgba(67, 56, 202, 0.65);
}
/* 右上角柔光，避免纯色块显得平 */
.live-glow {
  position: absolute;
  top: -70px;
  right: -30px;
  width: 260px;
  height: 260px;
  background: radial-gradient(circle, rgba(139, 92, 246, 0.55), transparent 68%);
  pointer-events: none;
}
.live-left {
  position: relative;
  min-width: 0;
}
.live-badge {
  display: inline-flex;
  align-items: center;
  gap: 7px;
  padding: 3px 11px;
  border-radius: 999px;
  background: rgba(255, 255, 255, 0.16);
  font-size: 11.5px;
  font-weight: 600;
  letter-spacing: 0.3px;
  backdrop-filter: blur(4px);
}
.pulse {
  width: 6px;
  height: 6px;
  border-radius: 50%;
  background: #4ade80;
  box-shadow: 0 0 0 0 rgba(74, 222, 128, 0.7);
  animation: pulse 1.8s infinite;
}
@keyframes pulse {
  70% {
    box-shadow: 0 0 0 7px rgba(74, 222, 128, 0);
  }
  100% {
    box-shadow: 0 0 0 0 rgba(74, 222, 128, 0);
  }
}
.live-title {
  margin: 12px 0 0;
  font-size: 21px;
  font-weight: 800;
  letter-spacing: -0.2px;
}
.live-desc {
  margin: 8px 0 0;
  font-size: 13px;
  color: rgba(255, 255, 255, 0.82);
  max-width: 520px;
  line-height: 1.6;
}
.live-meta {
  display: flex;
  gap: 18px;
  margin-top: 12px;
}
.meta-item {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  font-size: 12.5px;
  color: rgba(255, 255, 255, 0.75);
}
.live-right {
  position: relative;
  text-align: right;
  flex-shrink: 0;
}
.countdown-label {
  font-size: 11.5px;
  color: rgba(255, 255, 255, 0.7);
  letter-spacing: 0.5px;
}
.countdown-value {
  font-family: 'JetBrains Mono', Consolas, monospace;
  font-size: 30px;
  font-weight: 700;
  font-variant-numeric: tabular-nums;
  letter-spacing: 0.5px;
  margin: 2px 0 12px;
}
.live-btn {
  padding: 9px 22px;
  border: none;
  border-radius: 9px;
  background: #fff;
  color: #4338ca;
  font-size: 13.5px;
  font-weight: 700;
  cursor: pointer;
  transition: background 0.2s var(--oj-ease), transform 0.15s var(--oj-ease);
}
.live-btn:hover {
  background: #eef2ff;
}
.live-btn:active {
  transform: scale(0.97);
}

/* ---------- 其余竞赛：紧凑列表 ---------- */
.list-card {
  padding: 20px 0 6px;
}
.list-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 0 24px 14px;
  border-bottom: 1px solid var(--oj-border);
}
.list-head h3 {
  margin: 0;
  font-size: 15px;
  font-weight: 700;
}
.filters {
  display: flex;
  gap: 4px;
  background: #f3f4f6;
  padding: 3px;
  border-radius: 8px;
}
.filter-btn {
  padding: 5px 14px;
  border: none;
  border-radius: 6px;
  background: transparent;
  color: var(--oj-text-sub);
  font-size: 12.5px;
  font-weight: 500;
  cursor: pointer;
  transition: all 0.18s var(--oj-ease);
}
.filter-btn:hover {
  color: var(--oj-text);
}
.filter-btn.active {
  background: #fff;
  color: var(--oj-primary);
  font-weight: 600;
  box-shadow: 0 1px 3px rgba(17, 24, 39, 0.1);
}

.row {
  display: flex;
  align-items: center;
  gap: 16px;
  padding: 15px 24px;
  border-bottom: 1px solid #f3f4f6;
  cursor: pointer;
  transition: background 0.16s var(--oj-ease);
}
.row:last-child {
  border-bottom: none;
}
.row:hover {
  background: #fafbff;
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
  background: #fff7ed;
  color: #c2410c;
}
.st-2 {
  background: #f3f4f6;
  color: #6b7280;
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
  color: var(--oj-text-weak);
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
  font-family: 'JetBrains Mono', Consolas, monospace;
  font-size: 12px;
  color: var(--oj-warning);
  font-weight: 600;
}
.cd.muted {
  color: var(--oj-text-weak);
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
