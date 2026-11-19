<template>
  <div class="leaderboard-page">
    <header class="page-head honor-head">
      <div>
        <p class="kicker">LEADERBOARD</p>
        <h1 class="page-head-title">排行榜</h1>
        <p class="page-head-sub">按解题数排序，同数者先达成者在前。榜单只记录结果，不记录资历。</p>
      </div>
      <dl class="honor-totals" aria-label="全站统计">
        <div class="total">
          <dt>SOLVERS</dt>
          <dd>{{ summary.solverCount }}</dd>
        </div>
        <div class="total">
          <dt>MAX AC</dt>
          <dd>{{ summary.maxAcCount }}</dd>
        </div>
        <div class="total">
          <dt>SUBMISSIONS</dt>
          <dd>{{ summary.totalSubmit }}</dd>
        </div>
        <div class="total">
          <dt>AC RATE</dt>
          <dd>{{ summary.overallAcceptRate }}<small>%</small></dd>
        </div>
      </dl>
    </header>

    <!-- 前三名：轻量领奖台，1 号居中 -->
    <section v-if="podium.length" class="podium" aria-label="前三名">
      <div
        v-for="entry in podium"
        :key="entry.userId"
        class="place"
        :class="`place-${entry.rank}`"
      >
        <div class="place-top">
          <span class="place-rank">{{ String(entry.rank).padStart(2, '0') }}</span>
          <span class="place-tag">TOP {{ entry.rank }}</span>
        </div>
        <span class="place-avatar" :style="{ background: avatarColor(entry.userId) }">
          <img v-if="entry.avatar" class="avatar-img" :src="entry.avatar" alt="" />
          <template v-else>{{ (entry.username || '?').slice(0, 1).toUpperCase() }}</template>
        </span>
        <span class="place-name">{{ entry.username || `用户 ${entry.userId}` }}</span>
        <span class="place-ac">
          <b>{{ entry.acCount }}</b>
          <small>AC</small>
        </span>
        <span class="place-rate" :class="rateClass(entry.acceptRate)">
          通过率 {{ entry.acceptRate ?? 0 }}%
        </span>
      </div>
    </section>

    <section class="roster-card">
      <div class="roster-head">
        <div class="roster-title">
          <span class="kicker">THE LIST</span>
          <h2>完整榜单</h2>
        </div>
        <div class="roster-tools">
          <p class="roster-sub">共 {{ total }} 位解题者 · 按 AC 数降序</p>
          <el-select v-model="size" style="width: 118px" aria-label="每页条数" @change="reload">
            <el-option :value="10" label="每页 10 条" />
            <el-option :value="20" label="每页 20 条" />
            <el-option :value="50" label="每页 50 条" />
          </el-select>
        </div>
      </div>

      <el-table
        :data="items"
        v-loading="loading"
        :row-class-name="rankRowClass"
        row-key="userId"
        empty-text="还没有人上榜，快去成为第一个"
        class="roster-table"
      >
        <el-table-column prop="rank" label="名次" width="76">
          <template #default="{ row }">
            <span v-if="row.rank <= 3" class="rank-mark" :class="`mark-${row.rank}`">{{ String(row.rank).padStart(2, '0') }}</span>
            <span v-else class="rank-plain tabular">{{ String(row.rank).padStart(2, '0') }}</span>
          </template>
        </el-table-column>

        <el-table-column label="解题者" min-width="210">
          <template #default="{ row }">
            <div class="user-cell">
              <span class="avatar" :style="{ background: avatarColor(row.userId) }" aria-hidden="true">
                <img v-if="row.avatar" class="avatar-img" :src="row.avatar" alt="" />
                <template v-else>{{ (row.username || '?').slice(0, 1).toUpperCase() }}</template>
              </span>
              <span class="uname">{{ row.username || `用户 ${row.userId}` }}</span>
            </div>
          </template>
        </el-table-column>

        <el-table-column label="解题数" width="200">
          <template #default="{ row }">
            <div class="ac-cell">
              <span class="ac-num tabular">{{ row.acCount }}</span>
              <span class="ac-track" aria-hidden="true">
                <span
                  class="ac-fill"
                  :style="{ width: `${maxAc ? Math.round((row.acCount / maxAc) * 100) : 0}%`, background: progressColor(row.rank) }"
                />
              </span>
            </div>
          </template>
        </el-table-column>

        <el-table-column label="通过率" width="140" align="right">
          <template #default="{ row }">
            <div class="rate-cell">
              <span class="rate-num tabular" :class="rateClass(row.acceptRate)">{{ row.acceptRate ?? 0 }}%</span>
              <span class="rate-sub tabular">{{ row.acceptedSubmit ?? 0 }}/{{ row.totalSubmit ?? 0 }}</span>
            </div>
          </template>
        </el-table-column>

        <el-table-column label="最后活跃" width="140" align="right">
          <template #default="{ row }">
            <span class="last-active">{{ relativeTime(row.lastSubmitAt) }}</span>
          </template>
        </el-table-column>
      </el-table>

      <div v-if="total > size" class="table-foot">
        <el-pagination
          v-model:current-page="page"
          :page-size="size"
          :total="total"
          :pager-count="7"
          layout="prev, pager, next, jumper"
          @current-change="load"
        />
      </div>
    </section>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { getLeaderboard, type LeaderboardItem } from '@/api/leaderboard'
import { relativeTime } from '@/utils/datetime'

const items = ref<LeaderboardItem[]>([])
const page = ref(1)
const size = ref(20)
const total = ref(0)
const loading = ref(false)
const summary = ref({
  solverCount: 0,
  maxAcCount: 0,
  totalSubmit: 0,
  overallAcceptRate: 0,
})

/** 进度条基准：全站最高解题数（而非当前页第一，避免翻页时比例跳变） */
const maxAc = computed(() => summary.value.maxAcCount || 1)

/** 领奖台只取第一页的前三名，且必须真的存在 */
const podium = computed(() => (page.value === 1 ? items.value.filter((i) => i.rank <= 3) : []))

async function load() {
  loading.value = true
  try {
    const data = await getLeaderboard({ page: page.value, size: size.value })
    items.value = data.items || []
    total.value = data.total
    if (data.summary) summary.value = data.summary
  } finally {
    loading.value = false
  }
}

function reload() {
  page.value = 1
  load()
}

/** 前三名用金/银/铜，其余用品牌蓝 */
function progressColor(rank: number) {
  if (rank === 1) return 'var(--oj-gold)'
  if (rank === 2) return 'var(--oj-silver)'
  if (rank === 3) return 'var(--oj-bronze)'
  return 'var(--oj-primary)'
}

/** 通过率分级配色：>=60 绿、>=30 橙、否则红 */
function rateClass(rate?: number) {
  const r = rate ?? 0
  if (r >= 60) return 'good'
  if (r >= 30) return 'mid'
  return 'low'
}

/** 稳定取色的头像底色：统一深灰蓝，不做彩虹拼盘 */
const AVATAR_COLORS = [
  'var(--oj-gray-700)',
  'var(--oj-gray-800)',
  'var(--oj-gray-900)',
  'var(--oj-primary)',
  'var(--oj-bronze)',
  'var(--oj-silver)',
]
function avatarColor(userId: number) {
  return AVATAR_COLORS[Math.abs(userId) % AVATAR_COLORS.length]
}

function rankRowClass({ row }: { row: LeaderboardItem }) {
  return row.rank <= 3 ? `top-row top-row-${row.rank}` : ''
}

onMounted(load)
</script>

<style scoped>
.leaderboard-page {
  max-width: 1200px;
  margin: 0 auto;
  padding: 44px 32px 64px;
}

.honor-head {
  align-items: flex-end;
}
.honor-totals {
  display: flex;
  margin: 0;
  padding: 0;
  flex-shrink: 0;
}
.total {
  padding: 0 20px;
  border-left: 1px solid var(--oj-border);
  text-align: right;
}
.total:last-child {
  padding-right: 0;
}
.total dt {
  font-family: var(--oj-font-mono);
  font-size: 8.5px;
  letter-spacing: 0.1em;
  color: var(--oj-text-weak);
}
.total dd {
  margin: 5px 0 0;
  font-family: var(--oj-font-mono);
  font-size: 25px;
  line-height: 1;
  font-weight: 600;
  letter-spacing: -0.04em;
}
.total dd small {
  font-size: 11px;
  color: var(--oj-text-sub);
}

/* ---------- 前三名 ---------- */
.podium {
  display: grid;
  grid-template-areas: 'second first third';
  grid-template-columns: 1fr 1.24fr 1fr;
  gap: 14px;
  margin: 30px 0 24px;
  align-items: end;
}
.place {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 7px;
  padding: 18px 18px 18px;
  background: var(--oj-card);
  border: 1px solid var(--oj-border);
  border-top: 3px solid var(--oj-border-strong);
  border-radius: 10px;
  box-shadow: var(--oj-shadow-sm);
}
.place-1 {
  grid-area: first;
  padding-top: 24px;
  padding-bottom: 22px;
  border-top-color: var(--oj-primary);
  box-shadow: 0 14px 30px -24px rgba(16, 24, 40, 0.28);
}
.place-2 {
  grid-area: second;
  border-top-color: var(--oj-silver);
}
.place-3 {
  grid-area: third;
  border-top-color: var(--oj-bronze);
}
.place-top {
  width: 100%;
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
}
.place-rank {
  font-family: var(--oj-font-mono);
  font-size: 38px;
  font-weight: 600;
  line-height: 0.9;
  letter-spacing: -0.06em;
  color: var(--oj-gray-300);
}
.place-1 .place-rank {
  font-size: 48px;
  color: var(--oj-primary);
}
.place-tag {
  padding: 1px 6px;
  border: 1px solid var(--oj-border);
  border-radius: 3px;
  background: var(--oj-gray-50);
  font-family: var(--oj-font-mono);
  font-size: 8px;
  letter-spacing: 0.06em;
  color: var(--oj-text-weak);
}
.place-1 .place-tag {
  color: var(--oj-primary);
  border-color: var(--oj-info-border);
  background: var(--oj-primary-soft);
}
.place-avatar {
  width: 44px;
  height: 44px;
  border-radius: 8px;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  color: #fff;
  font-size: 17px;
  font-weight: 700;
  overflow: hidden;
  margin-top: 4px;
}
.place-1 .place-avatar {
  width: 52px;
  height: 52px;
  font-size: 20px;
}
.avatar-img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}
.place-name {
  max-width: 100%;
  font-size: 15px;
  font-weight: 700;
  letter-spacing: -0.02em;
  color: var(--oj-text);
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}
.place-1 .place-name {
  font-size: 17px;
}
.place-ac {
  display: flex;
  align-items: baseline;
  gap: 3px;
}
.place-ac b {
  font-family: var(--oj-font-mono);
  font-size: 28px;
  line-height: 1;
  color: var(--oj-text);
  font-weight: 600;
  letter-spacing: -0.04em;
}
.place-1 .place-ac b {
  font-size: 34px;
}
.place-ac small {
  font-family: var(--oj-font-mono);
  font-size: 9px;
  color: var(--oj-text-weak);
}
.place-rate {
  font-size: 10px;
  letter-spacing: 0.01em;
}
.good { color: var(--oj-success); }
.mid { color: var(--oj-warning); }
.low { color: var(--oj-danger); }

/* ---------- 完整榜单 ---------- */
.roster-card {
  background: var(--oj-card);
  border: 1px solid var(--oj-border);
  border-radius: 10px;
  box-shadow: var(--oj-shadow-sm);
  overflow: hidden;
}
.roster-head {
  display: flex;
  align-items: flex-end;
  justify-content: space-between;
  gap: 20px;
  padding: 16px 18px 13px;
  border-bottom: 1px solid var(--oj-border);
}
.roster-title h2 {
  margin: 4px 0 0;
  font-size: 19px;
  font-weight: 750;
  letter-spacing: -0.03em;
}
.roster-tools {
  display: flex;
  align-items: center;
  gap: 12px;
}
.roster-sub {
  font-family: var(--oj-font-mono);
  font-size: 8.5px;
  letter-spacing: 0.05em;
  color: var(--oj-text-weak);
}

.roster-table {
  width: 100%;
}
.roster-table :deep(.el-table__inner-wrapper::before) {
  display: none;
}
.rank-mark {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  min-width: 26px;
  height: 22px;
  padding: 0 5px;
  border-radius: 4px;
  background: var(--oj-gray-50);
  border: 1px solid var(--oj-border);
  font-family: var(--oj-font-mono);
  font-size: 10px;
  font-weight: 600;
}
.mark-1 {
  color: var(--oj-primary);
  background: var(--oj-primary-soft);
  border-color: var(--oj-info-border);
}
.mark-2 {
  color: var(--oj-silver);
}
.mark-3 {
  color: var(--oj-bronze);
}
.rank-plain {
  font-family: var(--oj-font-mono);
  font-size: 10px;
  color: var(--oj-text-weak);
}

.user-cell {
  display: flex;
  align-items: center;
  gap: 9px;
}
.avatar {
  width: 26px;
  height: 26px;
  border-radius: 5px;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  color: #fff;
  font-size: 11px;
  font-weight: 700;
  flex-shrink: 0;
  overflow: hidden;
}
.uname {
  font-weight: 600;
  color: var(--oj-gray-800);
}

.ac-cell {
  display: flex;
  align-items: center;
  gap: 11px;
}
.ac-num {
  font-family: var(--oj-font-mono);
  font-weight: 600;
  font-size: 12px;
  min-width: 24px;
}
.ac-track {
  flex: 1;
  max-width: 120px;
  height: 3px;
  border-radius: 99px;
  background: var(--oj-gray-100);
  overflow: hidden;
}
.ac-fill {
  display: block;
  height: 100%;
  border-radius: 99px;
  transition: width 0.3s var(--oj-ease);
}

.rate-cell {
  display: flex;
  align-items: baseline;
  justify-content: flex-end;
  gap: 8px;
}
.rate-num {
  font-family: var(--oj-font-mono);
  font-weight: 600;
}
.rate-sub {
  font-size: 9px;
  color: var(--oj-text-weak);
}
.last-active {
  color: var(--oj-text-sub);
  font-size: 11px;
}

.table-foot {
  display: flex;
  justify-content: flex-end;
  padding: 14px 18px;
  border-top: 1px solid var(--oj-border);
}

@media (max-width: 860px) {
  .leaderboard-page {
    padding: 28px 18px 52px;
  }
  .honor-totals {
    width: 100%;
    margin-top: 16px;
    padding-top: 14px;
    border-top: 1px solid var(--oj-border);
  }
  .total:first-child {
    padding-left: 0;
    border-left: 0;
    text-align: left;
  }
  .podium {
    grid-template-areas: 'first first';
    grid-template-columns: 1fr;
    margin: 24px 0;
  }
  .place-2,
  .place-3 {
    display: none;
  }
  .roster-head {
    flex-direction: column;
    align-items: flex-start;
  }
  .roster-sub {
    display: none;
  }
}
</style>

<style>
/* 前三名行：只做轻微强调，不刷底色 — 名次由徽章承担 */
.el-table .top-row-1 td,
.el-table .top-row-2 td,
.el-table .top-row-3 td {
  font-weight: 500;
}
</style>
