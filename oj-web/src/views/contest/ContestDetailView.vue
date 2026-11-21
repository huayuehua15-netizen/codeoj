<template>
  <!-- 加载为空态：竞赛不存在/路由 id 非法时不留白屏 -->
  <div v-if="!contest" class="page-container">
    <div class="card empty-sheet">
      <el-empty :description="loadError ? '竞赛不存在或尚未开放' : '加载中…'">
        <el-button v-if="loadError" type="primary" @click="$router.push('/contests')">返回竞赛列表</el-button>
      </el-empty>
    </div>
  </div>

  <div v-else class="race">
    <!-- 竞赛概览 -->
    <header class="race-head">
      <div class="race-main">
        <div class="race-status-row">
          <span class="race-status" :class="`race-${contest.status}`">
            <span v-if="contest.status === 1" class="pulse" aria-hidden="true" />
            {{ statusInfo(contest.status).label }}
          </span>
          <span class="race-no">CONTEST #{{ String(contest.id).padStart(3, '0') }}</span>
        </div>
        <h1 class="race-title">{{ contest.title }}</h1>
        <p class="race-desc">{{ contest.description || '暂无说明' }}</p>
        <div class="race-time">
          <span>{{ formatDateTime(contest.startTime) }}</span>
          <span class="time-sep" aria-hidden="true">→</span>
          <span>{{ formatDateTime(contest.endTime) }}</span>
          <span class="time-note">ACM 赛制 · 解题数优先 · 同数按罚时</span>
        </div>
      </div>

      <div v-if="contest.status !== 2" class="race-clock">
        <span class="clock-label">{{ contest.status === 1 ? 'REMAINING' : 'START IN' }}</span>
        <span class="clock-value" aria-live="off">{{ countdown }}</span>
        <span class="clock-note">以服务器时间为准</span>
      </div>
    </header>

    <section class="race-body">
      <div class="race-nav" role="tablist">
        <el-tabs v-model="activeTab" class="race-tabs">
          <el-tab-pane label="竞赛题目" name="problems">
            <div class="tab-head">
              <span class="kicker">PROBLEMS</span>
              <h2>题目清单</h2>
              <span class="tab-count">{{ contest.problems.length }} 道</span>
            </div>

            <div class="problem-head" aria-hidden="true">
              <span>题号</span>
              <span>题目</span>
              <span>难度</span>
            </div>
            <div v-if="contest.problems.length" class="problem-list">
              <router-link
                v-for="row in contest.problems"
                :key="row.problemId"
                :to="`/problems/${row.problemId}`"
                class="problem-row"
              >
                <span class="label-badge serif-num">{{ row.label }}</span>
                <span class="problem-title">{{ row.title }}</span>
                <span class="oj-diff" :class="`diff-${row.difficulty}`">
                  {{ DIFFICULTY_MAP[row.difficulty]?.label ?? '未知' }}
                </span>
                <svg viewBox="0 0 24 24" width="13" height="13" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round" aria-hidden="true">
                  <path d="M5 12 L19 12 M13 6 L19 12 L13 18" />
                </svg>
              </router-link>
            </div>
            <el-empty v-else description="本竞赛还没有配置题目" :image-size="64" />

            <el-alert
              v-if="contest.status === 1"
              title="比赛进行中：在题目页提交的代码会自动计入本赛排名"
              type="info"
              :closable="false"
              show-icon
              class="live-alert"
            />
          </el-tab-pane>

          <el-tab-pane label="实时排名" name="ranking">
            <div class="tab-head">
              <span class="kicker">STANDINGS</span>
              <h2>实时排名</h2>
              <span class="tab-count">ACM 赛制</span>
            </div>

            <p v-if="contest.status === 0" class="rank-hint">
              竞赛开始后这里会显示实时排名：解题数优先，同解题数按总罚时升序。
            </p>

            <el-table
              :data="ranking"
              v-loading="rankLoading"
              row-key="userId"
              empty-text="还没有提交记录"
              class="rank-table"
            >
              <el-table-column label="名次" width="84">
                <template #default="{ row }">
                  <span class="rank-cell" :class="`rank-${row.rank}`">{{ String(row.rank).padStart(2, '0') }}</span>
                </template>
              </el-table-column>
              <el-table-column label="参赛者" min-width="160">
                <template #default="{ row }">{{ row.username || `用户 ${row.userId}` }}</template>
              </el-table-column>
              <el-table-column prop="solved" label="解题数" width="90" align="right" />
              <el-table-column prop="penalty" label="总罚时（分钟）" width="140" align="right" />
              <el-table-column label="判题明细" min-width="300">
                <template #default="{ row }">
                  <span class="mono detail-cell">{{ row.detail || '—' }}</span>
                </template>
              </el-table-column>
            </el-table>
          </el-tab-pane>
        </el-tabs>
      </div>
    </section>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, onUnmounted, ref, watch } from 'vue'
import { useRoute } from 'vue-router'
import { getContest, getContestRanking, type ContestDetail, type ContestRankItem } from '@/api/contest'
import { formatDateTime, parseServerTime } from '@/utils/datetime'
import { contestStatus, DIFFICULTY_MAP } from '@/utils/status'

const route = useRoute()

const contest = ref<ContestDetail>()
const loadError = ref(false)
const ranking = ref<ContestRankItem[]>([])
const rankLoading = ref(false)
const activeTab = ref('problems')
const nowTick = ref(0)

const countdown = computed(() => {
  void nowTick.value // 依赖 tick 触发重算
  if (!contest.value) return ''
  const target = parseServerTime(contest.value.status === 1 ? contest.value.endTime : contest.value.startTime)
  if (!target) return '—'
  const diff = target.getTime() - Date.now()
  if (diff <= 0) return '已到时'
  const h = Math.floor(diff / 3600000)
  const m = Math.floor((diff % 3600000) / 60000)
  const s = Math.floor((diff % 60000) / 1000)
  return `${h}小时 ${String(m).padStart(2, '0')}分 ${String(s).padStart(2, '0')}秒`
})

function statusInfo(status: number) {
  return contestStatus(status)
}

let timer: number | undefined

async function load() {
  const id = route.params.id
  // P2-9：id 来自地址栏，非正整数直接落到空态，不发请求
  if (!/^\d+$/.test(String(id))) {
    contest.value = undefined
    loadError.value = true
    return
  }
  loadError.value = false
  contest.value = undefined
  ranking.value = []
  try {
    contest.value = await getContest(String(id))
  } catch {
    loadError.value = true
    return
  }
  // 进行中/已结束的竞赛加载排名
  if (contest.value.status !== 0) {
    rankLoading.value = true
    try {
      ranking.value = await getContestRanking(String(id))
    } catch {
      // 排名加载失败不影响题面浏览，由拦截器提示
      ranking.value = []
    } finally {
      rankLoading.value = false
    }
  }
  // 秒表按最新状态决定启停：已结束不跑（省掉每秒一次的无谓重渲染）；
  // 放在 load() 里而不是 onMounted——组件复用切到另一场竞赛时也能正确启停
  window.clearInterval(timer)
  if (contest.value.status !== 2) {
    timer = window.setInterval(() => { nowTick.value++ }, 1000)
  }
}

// P2-4：/contests/1 → /contests/2 复用同一组件时重新加载
watch(() => route.params.id, load)

onMounted(async () => {
  // 秒表的启停决策在 load() 末尾按竞赛最新状态执行（含组件复用切赛的场景）
  await load()
})

onUnmounted(() => window.clearInterval(timer))
</script>

<style scoped>
.empty-sheet {
  border-radius: 10px;
}

.race {
  max-width: 1200px;
  margin: 0 auto;
  padding: 44px 32px 64px;
}

/* ---------- 竞赛概览 ---------- */
.race-head {
  display: grid;
  grid-template-columns: minmax(0, 1fr) 300px;
  gap: 28px;
  padding: 24px 26px;
  background: var(--oj-card);
  border: 1px solid var(--oj-border);
  border-radius: 10px;
  box-shadow: var(--oj-shadow-sm);
}
.race-status-row {
  display: flex;
  align-items: center;
  gap: 10px;
}
.race-status {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  padding: 3px 9px;
  border-radius: 4px;
  font-family: var(--oj-font-mono);
  font-size: 9.5px;
  letter-spacing: 0.04em;
}
.race-0 { color: var(--oj-primary); background: #eef2ff; }
.race-1 { color: var(--oj-success); background: #e7f7f1; }
.race-2 { color: var(--oj-text-sub); background: var(--oj-gray-100); }
.pulse {
  width: 5px;
  height: 5px;
  border-radius: 50%;
  background: var(--oj-success);
  box-shadow: 0 0 0 0 rgba(14, 159, 110, 0.3);
  animation: pulse 1.8s infinite;
}
@keyframes pulse {
  70% { box-shadow: 0 0 0 6px rgba(14, 159, 110, 0); }
  100% { box-shadow: 0 0 0 0 rgba(14, 159, 110, 0); }
}
.race-no {
  font-family: var(--oj-font-mono);
  font-size: 9px;
  letter-spacing: 0.08em;
  color: var(--oj-text-weak);
}
.race-title {
  margin: 13px 0 0;
  font-size: clamp(25px, 3vw, 34px);
  font-weight: 750;
  line-height: 1.25;
  letter-spacing: -0.045em;
  color: var(--oj-text);
}
.race-desc {
  margin: 9px 0 0;
  max-width: 44em;
  font-size: 12.5px;
  line-height: 1.8;
  color: var(--oj-text-sub);
}
.race-time {
  display: flex;
  flex-wrap: wrap;
  align-items: baseline;
  gap: 8px;
  margin-top: 16px;
  font-family: var(--oj-font-mono);
  font-size: 10px;
  color: var(--oj-text-sub);
}
.time-sep {
  color: var(--oj-gray-300);
}
.time-note {
  margin-left: 6px;
  color: var(--oj-text-weak);
  font-size: 9px;
  letter-spacing: 0.03em;
}

.race-clock {
  align-self: stretch;
  display: flex;
  flex-direction: column;
  justify-content: center;
  padding: 16px 20px;
  background: var(--oj-code-bg);
  border-radius: 8px;
  color: #f9fafb;
}
.clock-label {
  font-family: var(--oj-font-mono);
  font-size: 8.5px;
  letter-spacing: 0.12em;
  color: #667085;
}
.clock-value {
  margin: 8px 0 6px;
  font-size: 28px;
  line-height: 1.2;
  color: #f9fafb;
  font-variant-numeric: tabular-nums;
  letter-spacing: -0.03em;
}
.clock-note {
  font-family: var(--oj-font-mono);
  font-size: 8.5px;
  letter-spacing: 0.03em;
  color: #667085;
}

/* ---------- 题目 / 排名 ---------- */
.race-body {
  margin-top: 18px;
}
.race-tabs :deep(.el-tabs__header) {
  margin-bottom: 12px;
}
.race-tabs :deep(.el-tabs__item) {
  font-size: 13px;
  letter-spacing: 0.01em;
  padding: 0 16px;
  height: 36px;
}
.race-tabs :deep(.el-tabs__item:first-child) {
  padding-left: 0;
}
.race-tabs :deep(.el-tabs__nav-wrap::after) {
  background: var(--oj-border);
}
.race-tabs :deep(.el-tabs__content) {
  background: var(--oj-card);
  border: 1px solid var(--oj-border);
  border-radius: 10px;
  padding: 18px 20px 20px;
  box-shadow: var(--oj-shadow-sm);
}

.tab-head {
  display: flex;
  align-items: baseline;
  gap: 10px;
  padding-bottom: 10px;
  border-bottom: 1px solid var(--oj-border);
  margin-bottom: 0;
}
.tab-head h2 {
  margin: 0;
  font-size: 18px;
  font-weight: 750;
  letter-spacing: -0.03em;
}
.tab-count {
  margin-left: auto;
  font-family: var(--oj-font-mono);
  font-size: 9px;
  letter-spacing: 0.06em;
  color: var(--oj-text-weak);
}

.problem-head {
  display: grid;
  grid-template-columns: 56px minmax(0, 1fr) 80px 22px;
  gap: 14px;
  padding: 8px 6px 7px;
  border-bottom: 1px solid var(--oj-border);
  background: var(--oj-gray-50);
  font-family: var(--oj-font-mono);
  font-size: 8.5px;
  letter-spacing: 0.08em;
  color: var(--oj-text-weak);
}
.problem-list {
  border-bottom: 1px solid var(--oj-border);
}
.problem-row {
  display: grid;
  grid-template-columns: 56px minmax(0, 1fr) 80px 22px;
  gap: 14px;
  align-items: center;
  padding: 11px 6px;
  border-bottom: 1px solid var(--oj-border);
  transition: background var(--oj-duration) var(--oj-ease);
}
.problem-row:last-child {
  border-bottom: 0;
}
.problem-row:hover {
  background: var(--oj-gray-50);
}
.label-badge {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 25px;
  height: 25px;
  border-radius: 5px;
  background: var(--oj-primary-soft);
  font-family: var(--oj-font-mono);
  font-size: 11px;
  font-weight: 600;
  color: var(--oj-primary);
}
.problem-title {
  font-size: 13.5px;
  font-weight: 550;
  color: var(--oj-text);
  min-width: 0;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}
.problem-row:hover .problem-title {
  color: var(--oj-primary);
}
.problem-row svg {
  color: var(--oj-text-weak);
}
.problem-row:hover svg {
  color: var(--oj-primary);
}

.live-alert {
  margin-top: 16px;
  background: var(--oj-primary-soft);
  border: 1px solid var(--oj-info-border);
  border-radius: 7px;
  color: var(--oj-primary);
}
.live-alert :deep(.el-alert__title) {
  font-size: 11.5px;
}

.rank-hint {
  margin: 14px 0;
  font-size: 12px;
  color: var(--oj-text-sub);
}
.rank-cell {
  font-family: var(--oj-font-mono);
  font-size: 11px;
  color: var(--oj-text-sub);
}
.rank-cell.rank-1,
.rank-cell.rank-2,
.rank-cell.rank-3 {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 24px;
  height: 24px;
  border-radius: 50%;
  font-weight: 600;
}
.rank-cell.rank-1 { color: #b45309; background: #fef3c7; }
.rank-cell.rank-2 { color: #475467; background: #f1f3f7; }
.rank-cell.rank-3 { color: #9a3412; background: #fff2e8; }
.detail-cell {
  font-size: 10.5px;
  color: var(--oj-text-sub);
}

@media (max-width: 900px) {
  .race {
    padding: 28px 18px 56px;
  }
  .race-head {
    grid-template-columns: 1fr;
    gap: 18px;
  }
  .race-clock {
    padding: 16px 20px;
  }
  .clock-value {
    font-size: 24px;
  }
  .problem-head {
    grid-template-columns: 44px minmax(0, 1fr) 70px 20px;
    gap: 10px;
  }
  .problem-row {
    grid-template-columns: 44px minmax(0, 1fr) 70px 20px;
    gap: 10px;
  }
}
</style>
