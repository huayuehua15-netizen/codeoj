<template>
  <div class="page-container">
    <section class="oj-banner card" aria-label="全站统计">
      <div class="oj-banner-item">
        <span class="oj-banner-num">{{ summary.solverCount }}</span>
        <span class="oj-banner-label">上榜解题者</span>
      </div>
      <div class="oj-banner-divider" aria-hidden="true"></div>
      <div class="oj-banner-item">
        <span class="oj-banner-num">{{ summary.maxAcCount }}</span>
        <span class="oj-banner-label">最高解题数</span>
      </div>
      <div class="oj-banner-divider" aria-hidden="true"></div>
      <div class="oj-banner-item">
        <span class="oj-banner-num">{{ summary.totalSubmit }}</span>
        <span class="oj-banner-label">全站提交</span>
      </div>
      <div class="oj-banner-divider" aria-hidden="true"></div>
      <div class="oj-banner-item">
        <span class="oj-banner-num">{{ summary.overallAcceptRate }}<small>%</small></span>
        <span class="oj-banner-label">整体通过率</span>
      </div>
      <p class="oj-banner-note">按解题数排名<br />同分者先达成者在前</p>
    </section>

    <!-- 领奖台：只在第一页出现（第二页起没有前三名），避免翻页时视觉跳变 -->
    <section v-if="podium.length" class="podium" aria-label="前三名">
      <div
        v-for="entry in podium"
        :key="entry.userId"
        class="podium-card"
        :class="`place-${entry.rank}`"
      >
        <span class="podium-medal" :class="`medal-${entry.rank}`" aria-hidden="true">
          <svg viewBox="0 0 24 24" width="18" height="18" fill="none" stroke="currentColor" stroke-width="2.2" stroke-linecap="round" stroke-linejoin="round">
            <path d="M8 21 L12 17 L16 21" /><path d="M12 3 L14.6 8.4 L20.5 9.2 L16.2 13.3 L17.3 19.1 L12 16.3 L6.7 19.1 L7.8 13.3 L3.5 9.2 L9.4 8.4 Z" />
          </svg>
        </span>
        <span class="podium-avatar" :style="{ background: avatarColor(entry.userId) }">
          {{ (entry.username || '?').slice(0, 1).toUpperCase() }}
        </span>
        <span class="podium-name">{{ entry.username || `用户 ${entry.userId}` }}</span>
        <span class="podium-ac">{{ entry.acCount }}<small> 题</small></span>
        <span class="podium-rate" :class="rateClass(entry.acceptRate)">通过率 {{ entry.acceptRate ?? 0 }}%</span>
      </div>
    </section>

    <div class="card">
      <header class="head">
        <div>
          <h2>排行榜</h2>
          <p class="sub">共 {{ total }} 位解题者，按解题数降序排列</p>
        </div>
        <el-select v-model="size" style="width: 122px" aria-label="每页条数" @change="reload">
          <el-option :value="10" label="每页 10 条" />
          <el-option :value="20" label="每页 20 条" />
          <el-option :value="50" label="每页 50 条" />
        </el-select>
      </header>

      <el-table
        :data="items"
        v-loading="loading"
        :row-class-name="rankRowClass"
        row-key="userId"
        empty-text="还没有人上榜，快去成为第一个"
        class="lb-table"
      >
        <el-table-column prop="rank" label="名次" width="84" align="center">
          <template #default="{ row }">
            <span v-if="row.rank <= 3" class="rank-badge" :class="`rank-${row.rank}`">{{ row.rank }}</span>
            <span v-else class="rank-num tabular">{{ row.rank }}</span>
          </template>
        </el-table-column>

        <el-table-column label="用户" min-width="200">
          <template #default="{ row }">
            <div class="user-cell">
              <span class="avatar" :style="{ background: avatarColor(row.userId) }" aria-hidden="true">
                {{ (row.username || '?').slice(0, 1).toUpperCase() }}
              </span>
              <span class="uname">{{ row.username || `用户 ${row.userId}` }}</span>
            </div>
          </template>
        </el-table-column>

        <el-table-column label="解题数" width="190">
          <template #default="{ row }">
            <div class="ac-cell">
              <span class="ac-num tabular">{{ row.acCount }}</span>
              <el-progress
                :percentage="maxAc ? Math.round((row.acCount / maxAc) * 100) : 0"
                :stroke-width="8"
                :show-text="false"
                :color="progressColor(row.rank)"
                class="ac-bar"
              />
            </div>
          </template>
        </el-table-column>

        <el-table-column label="通过率" width="150" align="center">
          <template #default="{ row }">
            <div class="rate-cell">
              <span class="rate-num tabular" :class="rateClass(row.acceptRate)">{{ row.acceptRate ?? 0 }}%</span>
              <span class="rate-sub tabular">{{ row.acceptedSubmit ?? 0 }}/{{ row.totalSubmit ?? 0 }}</span>
            </div>
          </template>
        </el-table-column>

        <el-table-column label="最后活跃" width="150" align="right">
          <template #default="{ row }">
            <span class="last-active">{{ relativeTime(row.lastSubmitAt) }}</span>
          </template>
        </el-table-column>
      </el-table>

      <el-pagination
        v-if="total > size"
        v-model:current-page="page"
        :page-size="size"
        :total="total"
        :pager-count="7"
        layout="prev, pager, next, jumper"
        class="pager"
        @current-change="load"
      />
    </div>
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

/** 前三名用金/银/铜，其余主题色（颜色统一来自设计 token） */
function progressColor(rank: number) {
  if (rank === 1) return 'var(--oj-gold-light)'
  if (rank === 2) return 'var(--oj-silver-light)'
  if (rank === 3) return 'var(--oj-bronze-light)'
  return 'var(--oj-primary-hover)'
}

/** 通过率分级配色：>=60 绿、>=30 橙、否则红 */
function rateClass(rate?: number) {
  const r = rate ?? 0
  if (r >= 60) return 'good'
  if (r >= 30) return 'mid'
  return 'low'
}

/**
 * 稳定取色的头像底色。
 * 只保留主色系与少量点缀色，避免彩虹色撞进靛蓝主题里显得花。
 */
const AVATAR_COLORS = [
  'var(--oj-indigo-600)',
  'var(--oj-indigo-500)',
  'var(--oj-indigo-400)',
  'var(--oj-success)',
  'var(--oj-warning)',
  'var(--oj-info)',
  'var(--oj-indigo-800)',
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
.oj-banner-num small {
  font-size: 14px;
  margin-left: 1px;
}

/* ---------- 领奖台 ---------- */
.podium {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 14px;
  margin-bottom: 16px;
}
.podium-card {
  position: relative;
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 6px;
  padding: 26px 16px 18px;
  border-radius: var(--oj-radius);
  background: var(--oj-white);
  border: 1px solid var(--oj-border);
  box-shadow: var(--oj-shadow-sm);
  overflow: hidden;
}
/* 冠军卡抬高一点，形成领奖台的层次 */
.podium-card.place-1 {
  border-color: var(--oj-warning-border);
  background: linear-gradient(180deg, var(--oj-warning-soft) 0%, var(--oj-white) 62%);
  box-shadow: var(--oj-shadow);
}
.podium-card.place-2 {
  border-color: var(--oj-border);
  background: linear-gradient(180deg, var(--oj-gray-50) 0%, var(--oj-white) 62%);
}
.podium-card.place-3 {
  border-color: var(--oj-warning-border);
  background: linear-gradient(180deg, var(--oj-warning-soft) 0%, var(--oj-white) 62%);
}
.podium-medal {
  position: absolute;
  top: 12px;
  right: 12px;
  display: inline-flex;
  color: var(--oj-gray-300);
}
.podium-medal.medal-1 {
  color: var(--oj-gold-light);
}
.podium-medal.medal-2 {
  color: var(--oj-silver);
}
.podium-medal.medal-3 {
  color: var(--oj-bronze);
}
.podium-avatar {
  width: 46px;
  height: 46px;
  border-radius: 50%;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  color: var(--oj-white);
  font-size: 19px;
  font-weight: 800;
  box-shadow: var(--oj-shadow-sm);
}
.podium-name {
  font-weight: 700;
  font-size: 14.5px;
  color: var(--oj-gray-800);
  max-width: 100%;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}
.podium-ac {
  font-size: 22px;
  font-weight: 800;
  color: var(--oj-primary);
  font-variant-numeric: tabular-nums;
  line-height: 1.1;
}
.podium-ac small {
  font-size: 12px;
  font-weight: 600;
  color: var(--oj-text-sub);
}
.podium-rate {
  font-size: 12px;
  color: var(--oj-text-sub);
}

/* ---------- 表格 ---------- */
.head {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 16px;
  margin-bottom: 12px;
}
.head h2 {
  margin: 0;
}
.head .sub {
  margin: 6px 0 0;
  font-size: 12.5px;
  color: var(--oj-text-sub);
}

.rank-badge {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 30px;
  height: 30px;
  border-radius: var(--oj-radius-md);
  color: var(--oj-white);
  font-weight: 800;
  font-size: 14px;
  font-variant-numeric: tabular-nums;
  box-shadow: var(--oj-shadow-sm);
}
.rank-badge.rank-1 {
  background: linear-gradient(135deg, var(--oj-gold-light), var(--oj-gold));
}
.rank-badge.rank-2 {
  background: linear-gradient(135deg, var(--oj-silver-light), var(--oj-silver));
}
.rank-badge.rank-3 {
  background: linear-gradient(135deg, var(--oj-bronze-light), var(--oj-bronze));
}
.rank-num {
  color: var(--oj-text-weak);
  font-weight: 600;
}

.user-cell {
  display: flex;
  align-items: center;
  gap: 10px;
}
.avatar {
  width: 32px;
  height: 32px;
  border-radius: 50%;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  color: var(--oj-white);
  font-size: 14px;
  font-weight: 800;
  flex-shrink: 0;
}
.uname {
  font-weight: 600;
  color: var(--oj-gray-800);
}

.ac-cell {
  display: flex;
  align-items: center;
  gap: 10px;
}
.ac-num {
  font-weight: 800;
  font-size: 15px;
  min-width: 20px;
}
.ac-bar {
  flex: 1;
  min-width: 90px;
}

.rate-cell {
  display: flex;
  flex-direction: column;
  align-items: center;
  line-height: 1.3;
}
.rate-num {
  font-weight: 700;
}
.rate-num.good {
  color: var(--oj-success);
}
.rate-num.mid {
  color: var(--oj-warning);
}
.rate-num.low {
  color: var(--oj-danger);
}
.rate-sub {
  font-size: 11.5px;
  color: var(--oj-text-weak);
}

.last-active {
  color: var(--oj-text-sub);
  font-size: 13px;
}

.pager {
  margin-top: 16px;
  justify-content: flex-end;
}

@media (max-width: 720px) {
  .podium {
    grid-template-columns: 1fr;
  }
}
</style>

<style>
/* 前三名行高亮：表格行类名由 row-class-name 生成，scoped 选不中，只能全局 */
.el-table .top-row-1 {
  background: var(--oj-warning-soft);
}
.el-table .top-row-2 {
  background: var(--oj-gray-50);
}
.el-table .top-row-3 {
  background: var(--oj-warning-soft);
}
.el-table .top-row-1 td,
.el-table .top-row-2 td,
.el-table .top-row-3 td {
  font-weight: 500;
}
</style>
