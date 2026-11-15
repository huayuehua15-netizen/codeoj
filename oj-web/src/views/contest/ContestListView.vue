<template>
  <div class="page-container">
    <div class="page-head card">
      <h2>竞赛</h2>
      <p class="sub">参加限时竞赛，解题数优先、总罚时并列排名（ACM 赛制）</p>
    </div>

    <div v-for="c in contests" :key="c.id" class="card contest-card" :class="`status-${c.status}`">
      <div class="contest-main">
        <div class="contest-title-row">
          <el-tag :type="statusInfo(c.status).type" size="small" effect="dark">{{ statusInfo(c.status).label }}</el-tag>
          <span class="contest-title" @click="$router.push(`/contests/${c.id}`)">{{ c.title }}</span>
        </div>
        <p class="contest-desc">{{ c.description || '暂无说明' }}</p>
        <div class="contest-meta">
          <span>⏰ {{ c.startTime.slice(0, 16).replace('T', ' ') }} ~ {{ c.endTime.slice(0, 16).replace('T', ' ') }}</span>
          <span>📋 {{ c.problemCount }} 题</span>
          <span v-if="c.status === 1" class="countdown">⏳ 剩余 {{ countdownOf(c) }}</span>
          <span v-else-if="c.status === 0" class="countdown">🚀 {{ countdownOf(c) }} 后开始</span>
        </div>
      </div>
      <div class="contest-actions">
        <el-button type="primary" @click="$router.push(`/contests/${c.id}`)">
          {{ c.status === 0 ? '查看详情' : c.status === 1 ? '进入竞赛' : '查看排名' }}
        </el-button>
      </div>
    </div>
    <el-empty v-if="!loading && contests.length === 0" description="暂无竞赛" />
  </div>
</template>

<script setup lang="ts">
// 竞赛列表（论文 7.2 展望落地：竞赛模式）
import { onMounted, onUnmounted, ref } from 'vue'
import { listContests, type ContestVO } from '@/api/contest'

const contests = ref<ContestVO[]>([])
const loading = ref(true)
let timer: number | undefined

function statusInfo(status: number) {
  return [{ label: '未开始', type: 'info' }, { label: '进行中', type: 'success' }, { label: '已结束', type: 'info' }][status] ?? { label: '?', type: 'info' }
}

/** 倒计时：进行中显示距结束、未开始显示距开始（每分钟随 tick 刷新） */
function countdownOf(c: ContestVO) {
  const target = new Date(c.status === 1 ? c.endTime : c.startTime).getTime()
  const diff = target - Date.now()
  if (diff <= 0) return '00:00'
  const d = Math.floor(diff / 86400000)
  const h = Math.floor((diff % 86400000) / 3600000)
  const m = Math.floor((diff % 3600000) / 60000)
  return d > 0 ? `${d}天${h}小时` : `${h}小时${m}分钟`
}

// 每分钟刷新倒计时
let tick = 0
function refreshCountdown() {
  tick++
}

onMounted(async () => {
  try {
    contests.value = await listContests()
  } finally {
    loading.value = false
  }
  timer = window.setInterval(refreshCountdown, 60000)
})

onUnmounted(() => window.clearInterval(timer))
</script>

<style scoped>
.page-head {
  margin-bottom: 16px;
}
.page-head h2 {
  margin: 0;
}
.sub {
  color: #909399;
  font-size: 13px;
  margin: 6px 0 0;
}
.contest-card {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 14px;
  border-left: 4px solid #c0c4cc;
}
.contest-card.status-1 {
  border-left-color: #67c23a;
}
.contest-card.status-0 {
  border-left-color: #e6a23c;
}
.contest-title-row {
  display: flex;
  align-items: center;
  gap: 10px;
}
.contest-title {
  font-size: 17px;
  font-weight: 700;
  cursor: pointer;
}
.contest-title:hover {
  color: #4f46e5;
}
.contest-desc {
  color: #606266;
  font-size: 13px;
  margin: 8px 0;
}
.contest-meta {
  display: flex;
  gap: 18px;
  color: #909399;
  font-size: 12px;
}
.countdown {
  color: #e6a23c;
  font-weight: 600;
}
</style>
