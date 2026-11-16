<template>
  <div class="page-container">
    <!-- 顶部概览 -->
    <div class="summary-bar card">
      <div class="sum-item">
        <span class="sum-num">{{ summary.solverCount }}</span>
        <span class="sum-label">上榜解题者</span>
      </div>
      <div class="sum-divider" />
      <div class="sum-item">
        <span class="sum-num">{{ summary.maxAcCount }}</span>
        <span class="sum-label">最高解题数</span>
      </div>
      <div class="sum-divider" />
      <div class="sum-item">
        <span class="sum-num">{{ summary.totalSubmit }}</span>
        <span class="sum-label">全站提交</span>
      </div>
      <div class="sum-divider" />
      <div class="sum-item">
        <span class="sum-num">{{ summary.overallAcceptRate }}%</span>
        <span class="sum-label">整体通过率</span>
      </div>
    </div>

    <div class="card">
      <div class="head">
        <div>
          <h2>排行榜</h2>
          <p class="sub">按解题数排序；同分者，先达成该题数者在前</p>
        </div>
        <el-select v-model="size" style="width: 120px" @change="reload">
          <el-option :value="10" label="每页 10 条" />
          <el-option :value="20" label="每页 20 条" />
          <el-option :value="50" label="每页 50 条" />
        </el-select>
      </div>

      <el-table :data="items" v-loading="loading" :row-class-name="rankRowClass" class="lb-table">
        <el-table-column prop="rank" label="名次" width="88" align="center">
          <template #default="{ row }">
            <span v-if="row.rank <= 3" class="medal">{{ medal(row.rank) }}</span>
            <span v-else class="rank-num">{{ row.rank }}</span>
          </template>
        </el-table-column>

        <el-table-column label="用户" min-width="200">
          <template #default="{ row }">
            <div class="user-cell">
              <span class="avatar" :style="{ background: avatarColor(row.userId) }">
                {{ (row.username || '?').slice(0, 1) }}
              </span>
              <span class="uname">{{ row.username || `用户 ${row.userId}` }}</span>
            </div>
          </template>
        </el-table-column>

        <el-table-column label="解题数" width="180" align="left">
          <template #default="{ row }">
            <div class="ac-cell">
              <span class="ac-num">{{ row.acCount }}</span>
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
              <span class="rate-num" :class="rateClass(row.acceptRate)">{{ row.acceptRate }}%</span>
              <span class="rate-sub">{{ row.acceptedSubmit }}/{{ row.totalSubmit }}</span>
            </div>
          </template>
        </el-table-column>

        <el-table-column label="最后活跃" width="160" align="right">
          <template #default="{ row }">
            <span class="last-active">{{ relativeTime(row.lastSubmitAt) }}</span>
          </template>
        </el-table-column>
      </el-table>

      <el-empty v-if="!loading && !items.length" description="还没有人上榜，快去成为第一个" />

      <el-pagination
        v-model:current-page="page"
        :page-size="size"
        :total="total"
        :page-sizes="[10, 20, 50]"
        layout="total, prev, pager, next"
        style="margin-top: 16px; justify-content: flex-end"
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

const MEDALS: Record<number, string> = { 1: '🥇', 2: '🥈', 3: '🥉' }
function medal(rank: number) {
  return MEDALS[rank] ?? String(rank)
}

/** 前三名用金/银/铜渐变，其余主题色 */
function progressColor(rank: number) {
  if (rank === 1) return '#f59e0b'
  if (rank === 2) return '#94a3b8'
  if (rank === 3) return '#d97706'
  return '#6366f1'
}

/** 通过率分级配色：>=60 绿、>=30 橙、否则红 */
function rateClass(rate: number) {
  if (rate >= 60) return 'good'
  if (rate >= 30) return 'mid'
  return 'low'
}

/** 按 userId 稳定取色的头像底色 */
const AVATAR_COLORS = ['#6366f1', '#10b981', '#f59e0b', '#8b5cf6', '#ec4899', '#06b6d4', '#ef4444']
function avatarColor(userId: number) {
  return AVATAR_COLORS[Math.abs(userId) % AVATAR_COLORS.length]
}

function rankRowClass({ row }: { row: LeaderboardItem }) {
  return row.rank <= 3 ? `top-row top-row-${row.rank}` : ''
}

onMounted(load)
</script>

<style scoped>
.summary-bar {
  display: flex;
  align-items: center;
  gap: 32px;
  padding: 20px 28px;
  margin-bottom: 16px;
  background: linear-gradient(135deg, #3730a3 0%, #4f46e5 60%, #6366f1 100%);
  color: #fff;
}
.sum-item {
  display: flex;
  flex-direction: column;
  align-items: center;
}
.sum-num {
  font-size: 26px;
  font-weight: 800;
  line-height: 1.1;
}
.sum-label {
  font-size: 12px;
  opacity: 0.85;
  margin-top: 4px;
}
.sum-divider {
  width: 1px;
  height: 36px;
  background: rgba(255, 255, 255, 0.28);
}
.head {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  margin-bottom: 12px;
}
.head h2 {
  margin: 0;
}
.sub {
  margin: 4px 0 0;
  font-size: 12.5px;
  color: #909399;
}
.medal {
  font-size: 18px;
}
.rank-num {
  color: #909399;
  font-weight: 600;
  font-variant-numeric: tabular-nums;
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
  color: #fff;
  font-size: 14px;
  font-weight: 700;
  flex-shrink: 0;
}
.uname {
  font-weight: 500;
}
.ac-cell {
  display: flex;
  align-items: center;
  gap: 10px;
}
.ac-num {
  font-weight: 700;
  font-size: 15px;
  min-width: 18px;
  font-variant-numeric: tabular-nums;
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
  font-weight: 600;
  font-variant-numeric: tabular-nums;
}
.rate-num.good {
  color: #16a34a;
}
.rate-num.mid {
  color: #d97706;
}
.rate-num.low {
  color: #dc2626;
}
.rate-sub {
  font-size: 11.5px;
  color: #a8abb2;
  font-variant-numeric: tabular-nums;
}
.last-active {
  color: #909399;
  font-size: 13px;
}
</style>

<style>
/* 前三名行高亮（表格行类名非 scoped，需全局） */
.el-table .top-row-1 {
  background: #fffbf0;
}
.el-table .top-row-2 {
  background: #f8fafc;
}
.el-table .top-row-3 {
  background: #fef9f3;
}
.el-table .top-row-1 td,
.el-table .top-row-2 td,
.el-table .top-row-3 td {
  font-weight: 500;
}
</style>
