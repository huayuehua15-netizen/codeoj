<template>
  <!-- 加载为空态：竞赛不存在/路由 id 非法时不留白屏 -->
  <div v-if="!contest" class="page-container">
    <div class="card">
      <el-empty :description="loadError ? '竞赛不存在或尚未开放' : '加载中…'">
        <el-button v-if="loadError" type="primary" @click="$router.push('/contests')">返回竞赛列表</el-button>
      </el-empty>
    </div>
  </div>

  <div v-else class="page-container">
    <header class="head-card">
      <div class="head-row">
        <div class="head-main">
          <span class="head-badge" :class="`badge-${contest.status}`">
            <span v-if="contest.status === 1" class="pulse" aria-hidden="true" />
            {{ statusInfo(contest.status).label }}
          </span>
          <h1 class="head-title">{{ contest.title }}</h1>
          <p class="desc">{{ contest.description || '暂无说明' }}</p>
          <div class="head-meta">
            <span class="meta-item">
              <svg viewBox="0 0 24 24" width="14" height="14" fill="none" stroke="currentColor" stroke-width="2" aria-hidden="true">
                <circle cx="12" cy="12" r="9" /><path d="M12 7v5l3 2" />
              </svg>
              {{ formatDateTime(contest.startTime) }} ~ {{ formatDateTime(contest.endTime) }}
            </span>
          </div>
        </div>
        <div v-if="contest.status !== 2" class="head-countdown">
          <div class="cd-label">{{ contest.status === 1 ? '距结束' : '距开始' }}</div>
          <div class="cd-value" aria-live="off">{{ countdown }}</div>
        </div>
      </div>
    </header>

    <section class="card tabs-card">
      <el-tabs v-model="activeTab">
        <el-tab-pane label="竞赛题目" name="problems">
          <el-table :data="contest.problems" row-key="problemId" empty-text="本竞赛还没有配置题目">
            <el-table-column label="题号" width="80">
              <template #default="{ row }">
                <span class="oj-badge-id">{{ row.label }}</span>
              </template>
            </el-table-column>
            <el-table-column label="题目" min-width="280">
              <template #default="{ row }">
                <router-link :to="`/problems/${row.problemId}`" class="p-link">{{ row.title }}</router-link>
              </template>
            </el-table-column>
            <el-table-column label="难度" width="110">
              <template #default="{ row }">
                <span class="oj-diff" :class="`diff-${row.difficulty}`">
                  {{ DIFFICULTY_MAP[row.difficulty]?.label ?? '未知' }}
                </span>
              </template>
            </el-table-column>
          </el-table>
          <el-alert
            v-if="contest.status === 1"
            title="比赛进行中：在题目页提交的代码会自动计入本赛排名"
            type="info" :closable="false" show-icon class="live-alert" />
        </el-tab-pane>

        <el-tab-pane label="实时排名" name="ranking">
          <p v-if="contest.status === 0" class="rank-hint">竞赛开始后这里会显示实时排名（ACM 赛制：解题数优先，同解题数按总罚时升序）</p>
          <el-table :data="ranking" v-loading="rankLoading" row-key="userId" empty-text="还没有提交记录">
            <el-table-column label="名次" width="80">
              <template #default="{ row }">
                <span class="rank-cell" :class="`rank-${row.rank}`">{{ row.rank }}</span>
              </template>
            </el-table-column>
            <el-table-column label="用户" min-width="160">
              <template #default="{ row }">{{ row.username || `用户 ${row.userId}` }}</template>
            </el-table-column>
            <el-table-column prop="solved" label="解题数" width="90" />
            <el-table-column prop="penalty" label="总罚时(分钟)" width="120" />
            <el-table-column label="明细" min-width="300">
              <template #default="{ row }">
                <span class="mono">{{ row.detail || '—' }}</span>
              </template>
            </el-table-column>
          </el-table>
        </el-tab-pane>
      </el-tabs>
    </section>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, onUnmounted, ref, watch } from 'vue'
import { useRoute } from 'vue-router'
import { getContest, getContestRanking, type ContestDetail, type ContestRankItem } from '@/api/contest'
import { formatDateTime, parseServerTime } from '@/utils/datetime'
import { DIFFICULTY_MAP } from '@/utils/status'

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
  return [{ label: '未开始', type: 'info' }, { label: '进行中', type: 'success' }, { label: '已结束', type: 'info' }][status]
    ?? { label: '未知', type: 'info' }
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
}

// P2-4：/contests/1 → /contests/2 复用同一组件时重新加载
watch(() => route.params.id, load)

onMounted(async () => {
  await load()
  timer = window.setInterval(() => { nowTick.value++ }, 1000)
})

onUnmounted(() => window.clearInterval(timer))
</script>

<style scoped>
/* 竞赛详情头：墨色舞台。状态靠文字与呼吸点表达 */
.head-card {
  background-image:
    radial-gradient(640px 260px at 12% -40%, rgba(94, 106, 210, 0.28), transparent 70%),
    linear-gradient(180deg, rgba(255, 255, 255, 0.03), transparent 55%),
    var(--oj-card);
  border: 1px solid var(--oj-border);
  border-radius: 10px;
  padding: 24px 28px;
  color: var(--oj-text);
}
.head-row {
  display: flex;
  align-items: flex-end;
  justify-content: space-between;
  gap: 24px;
}
.head-main {
  min-width: 0;
}
.head-badge {
  display: inline-flex;
  align-items: center;
  gap: 7px;
  font-size: 12.5px;
  font-weight: 600;
  color: var(--oj-text-sub);
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
.head-title {
  margin: 10px 0 0;
  font-size: 24px;
  font-weight: 600;
  letter-spacing: -0.4px;
  line-height: 1.3;
  color: var(--oj-text);
}
.desc {
  margin: 8px 0 0;
  font-size: 13px;
  color: var(--oj-text-sub);
  line-height: 1.65;
  max-width: 640px;
}
.head-meta {
  display: flex;
  flex-wrap: wrap;
  gap: 18px;
  margin-top: 12px;
}
.meta-item {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  font-size: 12.5px;
  color: var(--oj-text-sub);
}
.head-countdown {
  flex-shrink: 0;
  text-align: right;
}
.cd-label {
  font-size: 11.5px;
  color: var(--oj-text-sub);
  letter-spacing: 0.3px;
}
.cd-value {
  margin-top: 2px;
  font-family: var(--oj-font-mono);
  font-size: 26px;
  font-weight: 600;
  font-variant-numeric: tabular-nums;
  letter-spacing: 0.3px;
  color: var(--oj-text);
}

.tabs-card {
  margin-top: 16px;
  padding-bottom: 8px;
}
.live-alert {
  margin-top: 12px;
}
.rank-hint {
  margin: 0 0 12px;
  font-size: 12.5px;
  color: var(--oj-text-sub);
}
.rank-cell {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  min-width: 26px;
  height: 24px;
  padding: 0 6px;
  border-radius: var(--oj-radius-sm);
  background: var(--oj-gray-50);
  color: var(--oj-text-sub);
  font-weight: 700;
  font-variant-numeric: tabular-nums;
}
.rank-cell.rank-1 {
  background: var(--oj-warning-soft);
  color: var(--oj-gold);
}
.rank-cell.rank-2 {
  background: var(--oj-gray-50);
  color: var(--oj-silver);
}
.rank-cell.rank-3 {
  background: var(--oj-warning-soft);
  color: var(--oj-bronze);
}
.p-link {
  color: var(--oj-text);
  font-weight: 600;
  transition: color 0.15s var(--oj-ease);
}
.p-link:hover {
  color: var(--oj-primary);
}

@media (max-width: 820px) {
  .head-row {
    flex-direction: column;
    align-items: flex-start;
  }
  .head-countdown {
    text-align: left;
  }
  .head-card {
    padding: 22px 20px;
  }
}
</style>
