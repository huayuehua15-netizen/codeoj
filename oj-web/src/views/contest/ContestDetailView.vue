<template>
  <div class="page-container" v-if="contest">
    <div class="card head-card">
      <div class="head-row">
        <div>
          <h2 style="margin: 0">
            <el-tag :type="statusInfo(contest.status).type" effect="dark" size="small">{{ statusInfo(contest.status).label }}</el-tag>
            {{ contest.title }}
          </h2>
          <p class="desc">{{ contest.description }}</p>
          <p class="meta">⏰ {{ contest.startTime.slice(0, 16).replace('T', ' ') }} ~ {{ contest.endTime.slice(0, 16).replace('T', ' ') }}</p>
          <p class="meta countdown" v-if="contest.status !== 2">
            {{ contest.status === 1 ? '⏳ 距离结束还剩' : '🚀 距离开始还剩' }}：{{ countdown }}
          </p>
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
  const target = new Date(contest.value.status === 1 ? contest.value.endTime : contest.value.startTime).getTime()
  const diff = target - Date.now()
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
  background: linear-gradient(130deg, #0f2757 0%, #1e40af 55%, #3b82f6 100%);
  color: #fff;
  border: none;
  padding: 30px 34px;
}
.head-card::before {
  /* 装饰光斑 */
  content: '';
  position: absolute;
  right: -60px;
  top: -80px;
  width: 260px;
  height: 260px;
  border-radius: 50%;
  background: radial-gradient(circle, rgba(255, 255, 255, 0.14), transparent 65%);
}
.head-card::after {
  /* 网格纹理 */
  content: '';
  position: absolute;
  inset: 0;
  background-image:
    linear-gradient(rgba(255, 255, 255, 0.05) 1px, transparent 1px),
    linear-gradient(90deg, rgba(255, 255, 255, 0.05) 1px, transparent 1px);
  background-size: 28px 28px;
  pointer-events: none;
}
.head-card :deep(h2),
.head-card :deep(.meta),
.head-card :deep(.desc) {
  position: relative;
  z-index: 1;
}
.head-row h2 {
  display: flex;
  align-items: center;
  gap: 10px;
}
.desc {
  opacity: 0.9;
  font-size: 13px;
  margin: 8px 0;
}
.meta {
  font-size: 13px;
  opacity: 0.9;
  margin: 4px 0;
}
.countdown {
  font-weight: 700;
  font-size: 15px;
}
.label-badge {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 32px;
  height: 32px;
  border-radius: 10px;
  background: linear-gradient(135deg, #1e40af, #3b82f6);
  color: #fff;
  font-weight: 800;
  font-family: 'JetBrains Mono', Consolas, monospace;
  box-shadow: 0 2px 6px rgba(37, 99, 235, 0.35);
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
</style>
