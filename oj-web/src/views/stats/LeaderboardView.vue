<template>
  <div class="leaderboard-page">
    <header class="page-head honor-head">
      <div>
        <p class="kicker">LEADERBOARD</p>
        <h1 class="page-head-title">排行榜</h1>
        <p class="page-head-sub">按解题数排序，同数者先达成者在前。榜单只记录结果，不记录资历。</p>
      </div>
      <p class="honor-meta" aria-label="全站统计">
        <span>{{ summary.solverCount }} 位解题者</span>
        <span>最高 {{ summary.maxAcCount }} 题</span>
        <span>{{ summary.totalSubmit }} 次提交</span>
        <span>通过率 {{ summary.overallAcceptRate }}%</span>
      </p>
    </header>

    <!-- 前三名：一条横向名单，不做领奖台 -->
    <section v-if="podium.length" class="top-three" aria-label="前三名">
      <article
        v-for="entry in podium"
        :key="entry.userId"
        class="top-item"
        :class="`top-${entry.rank}`"
      >
        <span class="top-rank">{{ String(entry.rank).padStart(2, '0') }}</span>
        <span class="top-avatar" :style="{ background: avatarColor(entry.rank) }">
          <img v-if="entry.avatar" class="avatar-img" :src="entry.avatar" alt="" />
          <template v-else>{{ (entry.username || '?').slice(0, 1).toUpperCase() }}</template>
        </span>
        <span class="top-name">{{ entry.username || `用户 ${entry.userId}` }}</span>
        <span class="top-rate">{{ entry.acceptRate ?? 0 }}%</span>
        <span class="top-ac">{{ entry.acCount }}<small>AC</small></span>
      </article>
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
              <span class="avatar" :style="{ background: avatarColor(row.rank) }" aria-hidden="true">
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
                  :style="{ width: `${maxAc ? Math.round((row.acCount / maxAc) * 100) : 0}%` }"
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

/** 通过率分级配色：>=60 绿、>=30 橙、否则红 */
function rateClass(rate?: number) {
  const r = rate ?? 0
  if (r >= 60) return 'good'
  if (r >= 30) return 'mid'
  return 'low'
}

/** 只给第一名一点颜色，其余统一石墨色，避免彩色头像拼盘 */
function avatarColor(rank: number) {
  return rank === 1 ? 'var(--oj-primary)' : 'var(--oj-gray-700)'
}

function rankRowClass({ row }: { row: LeaderboardItem }) {
  return row.rank <= 3 ? `top-row top-row-${row.rank}` : ''
}

onMounted(load)
</script>

<style scoped>
.leaderboard-page {
  max-width: 1120px;
  margin: 0 auto;
  padding: 36px 32px 64px;
}

.honor-head {
  align-items: flex-end;
}
.honor-meta {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  gap: 4px 0;
  margin: 0;
  padding-bottom: 2px;
  font-size: 10.5px;
  color: var(--oj-text-weak);
}
.honor-meta span + span::before {
  content: '·';
  margin: 0 8px;
  color: var(--oj-gray-300);
}

/* ---------- 前三名：横向名单 ---------- */
.top-three {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  margin: 18px 0 20px;
  border-top: 1px solid var(--oj-border);
  border-bottom: 1px solid var(--oj-border);
}
.top-item {
  display: grid;
  grid-template-columns: 24px 30px minmax(0, 1fr) auto auto;
  gap: 10px;
  align-items: center;
  padding: 11px 16px;
  border-right: 1px solid var(--oj-border);
}
.top-item:last-child {
  border-right: 0;
}
.top-rank {
  font-family: var(--oj-font-mono);
  font-size: 10px;
  color: var(--oj-text-weak);
}
.top-1 .top-rank {
  color: var(--oj-primary);
  font-weight: 700;
}
.top-avatar {
  width: 28px;
  height: 28px;
  border-radius: 4px;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  color: #fff;
  font-size: 11px;
  font-weight: 700;
  overflow: hidden;
}
.avatar-img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}
.top-name {
  min-width: 0;
  font-size: 12.5px;
  font-weight: 600;
  color: var(--oj-text);
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}
.top-rate {
  font-family: var(--oj-font-mono);
  font-size: 9.5px;
  color: var(--oj-text-weak);
}
.top-ac {
  font-family: var(--oj-font-mono);
  font-size: 12px;
  color: var(--oj-text);
}
.top-ac small {
  margin-left: 3px;
  font-size: 8px;
  color: var(--oj-text-weak);
}
.good { color: var(--oj-success); }
.mid { color: var(--oj-warning); }
.low { color: var(--oj-danger); }

/* ---------- 完整榜单 ---------- */
.roster-card {
  background: var(--oj-card);
  border: 1px solid var(--oj-border);
  border-radius: 4px;
  overflow: hidden;
}
.roster-head {
  display: flex;
  align-items: flex-end;
  justify-content: space-between;
  gap: 20px;
  padding: 13px 16px 12px;
  border-bottom: 1px solid var(--oj-border);
}
.roster-title h2 {
  margin: 0;
  font-size: 16px;
  font-weight: 700;
  letter-spacing: -0.02em;
}
.roster-tools {
  display: flex;
  align-items: center;
  gap: 12px;
}
.roster-sub {
  font-size: 10.5px;
  color: var(--oj-text-weak);
}

.roster-table {
  width: 100%;
}
.roster-table :deep(.el-table__inner-wrapper::before) {
  display: none;
}
.roster-table :deep(.el-table__row td) {
  border-bottom: 1px solid var(--oj-border);
}
.roster-table :deep(.el-table__row:last-child td) {
  border-bottom: 0;
}
.rank-mark {
  display: inline-block;
  min-width: 22px;
  font-family: var(--oj-font-mono);
  font-size: 10.5px;
  font-weight: 600;
}
.mark-1 {
  color: var(--oj-primary);
}
.mark-2,
.mark-3 {
  color: var(--oj-text-sub);
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
  height: 2px;
  border-radius: 99px;
  background: var(--oj-gray-100);
  overflow: hidden;
}
.ac-fill {
  display: block;
  height: 100%;
  border-radius: 99px;
  background: var(--oj-gray-400);
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
  .honor-meta {
    width: 100%;
    padding-top: 10px;
  }
  .top-three {
    grid-template-columns: 1fr;
    margin: 16px 0;
  }
  .top-item {
    border-right: 0;
    border-bottom: 1px solid var(--oj-border);
  }
  .top-item:last-child {
    border-bottom: 0;
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
