<template>
  <div class="page-container" v-if="contest">
    <div class="head-card" :class="`hs-${contest.status}`">
      <div class="head-row">
        <div class="head-main">
          <div class="head-badge" :class="`badge-${contest.status}`">
            <span v-if="contest.status === 1" class="pulse" />
            {{ statusInfo(contest.status).label }}
          </div>
          <h1 class="head-title">{{ contest.title }}</h1>
          <p class="desc">{{ contest.description }}</p>
          <div class="head-meta">
            <span class="meta-item">
              <svg viewBox="0 0 24 24" width="14" height="14" fill="none" stroke="currentColor" stroke-width="2">
                <circle cx="12" cy="12" r="9" /><path d="M12 7v5l3 2" />
              </svg>
              {{ formatDateTime(contest.startTime) }} ~ {{ formatDateTime(contest.endTime) }}
            </span>
          </div>
        </div>
        <div v-if="contest.status !== 2" class="head-countdown">
          <div class="cd-label">{{ contest.status === 1 ? '距结束' : '距开始' }}</div>
          <div class="cd-value">{{ countdown }}</div>
        </div>
      </div>
    </div>

    <div class="card" style="margin-top: 16px">
      <el-tabs v-model="activeTab">
        <el-tab-pane label="竞赛题目" name="problems">
          <el-table :data="contest.problems">
            <el-table-column label="题号" width="80">
              <template #default="{ row }">
                <span class="label-badge">{{ row.label }}</span>
              </template>
            </el-table-column>
            <el-table-column label="题目" min-width="280">
              <template #default="{ row }">
                <router-link :to="`/problems/${row.problemId}`" class="p-link">{{ row.title }}</router-link>
              </template>
            </el-table-column>
            <el-table-column label="难度" width="110">
              <template #default="{ row }">
                <el-tag :color="diffColor(row.difficulty)" size="small">
                  {{ ({1:'简单',2:'中等',3:'困难'} as Record<number,string>)[row.difficulty] || '?' }}
                </el-tag>
              </template>
            </el-table-column>
          </el-table>
          <el-alert
            v-if="contest.status === 1"
            title="比赛进行中：在题目页提交的代码会自动计入本赛排名"
            type="info" :closable="false" show-icon style="margin-top: 12px" />
        </el-tab-pane>

        <el-tab-pane label="实时排名" name="ranking">
          <el-table :data="ranking" v-loading="rankLoading">
            <el-table-column prop="rank" label="名次" width="80" />
            <el-table-column label="用户" min-width="160">
              <template #default="{ row }">{{ row.username || `用户 ${row.userId}` }}</template>
            </el-table-column>
            <el-table-column prop="solved" label="解题数" width="90" />
            <el-table-column prop="penalty" label="总罚时(分钟)" width="120" />
            <el-table-column label="明细" min-width="300">
              <template #default="{ row }">{{ row.detail || '—' }}</template>
            </el-table-column>
          </el-table>
        </el-tab-pane>
      </el-tabs>
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, onUnmounted, ref } from 'vue'
import { useRoute } from 'vue-router'
import { getContest, getContestRanking, type ContestDetail, type ContestRankItem } from '@/api/contest'
import { formatDateTime, parseServerTime } from '@/utils/datetime'

const route = useRoute()
const id = route.params.id as string

const contest = ref<ContestDetail>()
const ranking = ref<ContestRankItem[]>([])
const rankLoading = ref(false)
const activeTab = ref('problems')
const nowTick = ref(0)

const countdown = computed(() => {
  nowTick.value // 依赖 tick 触发重算
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
  return [{ label: '未开始', type: 'info' }, { label: '进行中', type: 'success' }, { label: '已结束', type: 'info' }][status] ?? { label: '?', type: 'info' }
}

function diffColor(d: number) {
  return ({1:'#67c23a',2:'#e6a23c',3:'#f56c6c'} as Record<number,string>)[d] || '#909399'
}

let timer: number | undefined

onMounted(async () => {
  contest.value = await getContest(id)
  // 进行中/已结束的竞赛加载排名
  if (contest.value.status !== 0) {
    rankLoading.value = true
    try {
      ranking.value = await getContestRanking(id)
    } finally {
      rankLoading.value = false
    }
  }
  timer = window.setInterval(() => { nowTick.value++ }, 1000)
})

onUnmounted(() => window.clearInterval(timer))
</script>

<style scoped>
.head-card {
  position: relative;
  overflow: hidden;
  background: linear-gradient(135deg, #312e81 0%, #4338ca 55%, #4f46e5 100%);
  color: #fff;
  border-radius: var(--oj-radius);
  padding: 28px 32px;
  box-shadow: 0 8px 28px -10px rgba(67, 56, 202, 0.5);
}
/* 右上柔光，避免纯色块发闷 */
.head-card::before {
  content: '';
  position: absolute;
  right: -70px;
  top: -90px;
  width: 280px;
  height: 280px;
  border-radius: 50%;
  background: radial-gradient(circle, rgba(139, 92, 246, 0.55), transparent 68%);
}
/* 细网格纹理，弱化"大色块"感 */
.head-card::after {
  content: '';
  position: absolute;
  inset: 0;
  background-image:
    linear-gradient(rgba(255, 255, 255, 0.04) 1px, transparent 1px),
    linear-gradient(90deg, rgba(255, 255, 255, 0.04) 1px, transparent 1px);
  background-size: 30px 30px;
  pointer-events: none;
}
.head-row {
  position: relative;
  z-index: 1;
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
  padding: 3px 11px;
  border-radius: 999px;
  background: rgba(255, 255, 255, 0.16);
  font-size: 11.5px;
  font-weight: 600;
  letter-spacing: 0.3px;
}
.pulse {
  width: 6px;
  height: 6px;
  border-radius: 50%;
  background: #4ade80;
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
.head-title {
  margin: 12px 0 0;
  font-size: 24px;
  font-weight: 800;
  letter-spacing: -0.3px;
  line-height: 1.3;
}
.desc {
  margin: 8px 0 0;
  font-size: 13px;
  color: rgba(255, 255, 255, 0.82);
  line-height: 1.65;
  max-width: 640px;
}
.head-meta {
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
.head-countdown {
  flex-shrink: 0;
  text-align: right;
}
.cd-label {
  font-size: 11.5px;
  color: rgba(255, 255, 255, 0.7);
  letter-spacing: 0.5px;
}
.cd-value {
  margin-top: 2px;
  font-family: 'JetBrains Mono', Consolas, monospace;
  font-size: 28px;
  font-weight: 700;
  font-variant-numeric: tabular-nums;
  letter-spacing: 0.5px;
}
.label-badge {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 30px;
  height: 30px;
  border-radius: 8px;
  background: var(--oj-primary-soft);
  color: var(--oj-primary);
  font-weight: 800;
  font-family: 'JetBrains Mono', Consolas, monospace;
  font-size: 13.5px;
}
.p-link {
  color: var(--oj-text);
  font-weight: 600;
  transition: color 0.15s var(--oj-ease);
}
.p-link:hover {
  color: var(--oj-primary);
}
.head-card + .card {
  margin-top: 16px;
}
@media (max-width: 820px) {
  .head-row {
    flex-direction: column;
    align-items: flex-start;
  }
  .head-countdown {
    text-align: left;
  }
}
</style>
