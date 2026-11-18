<template>
  <div class="page-container">
    <div class="card">
      <header class="oj-page-head">
        <div>
          <h1 class="oj-page-title">判题队列监控</h1>
          <p class="oj-page-sub">
            查看判题队列积压与今日吞吐，用于排查拥堵
            <template v-if="status?.queriedAt">（查询于 {{ status.queriedAt }}；积压量最多滞后 30s）</template>
          </p>
        </div>
        <el-button :loading="loading" @click="load">刷新</el-button>
      </header>

      <!-- 三个指标做成卡片，比一行 descriptions 更容易一眼看出有没有积压 -->
      <div class="metrics" v-loading="loading">
        <div class="metric" :class="backlogClass">
          <span class="metric-label">队列积压</span>
          <span class="metric-num tabular">{{ status ? status.backlog : '—' }}</span>
          <span class="metric-hint">{{ backlogHint }}</span>
        </div>
        <div class="metric">
          <span class="metric-label">今日判题量</span>
          <span class="metric-num tabular">{{ status ? status.judgedToday : '—' }}</span>
          <span class="metric-hint">自今日 00:00 起累计完成的判题任务</span>
        </div>
        <div class="metric">
          <span class="metric-label">判题实例数</span>
          <span class="metric-num tabular">{{ status ? status.judgeInstances : '—' }}</span>
          <span class="metric-hint">{{ instancesHint }}</span>
        </div>
      </div>

      <p class="foot-note">
        积压量统计的是队列中尚未被消费者取走的任务（不含已取走未确认的），每 30 秒刷新一次缓存。
      </p>
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { getQueueStatus } from '@/api/admin'

const status = ref<Awaited<ReturnType<typeof getQueueStatus>>>()
const loading = ref(false)

/** 积压量分档：0 空闲、<10 正常、>=10 拥堵，颜色跟着变 */
const backlogClass = computed(() => {
  const backlog = status.value?.backlog ?? 0
  if (backlog === 0) return 'is-idle'
  if (backlog < 10) return 'is-normal'
  return 'is-busy'
})

const backlogHint = computed(() => {
  if (!status.value) return '加载中…'
  const backlog = status.value.backlog
  if (backlog === 0) return '队列空闲，没有等待中的任务'
  if (backlog < 10) return '有少量任务排队，属正常波动'
  return '积压较多，请检查判题实例是否在线'
})

const instancesHint = computed(() => {
  if (!status.value) return '来自 Redis 心跳统计'
  return status.value.judgeInstances === 0
    ? '当前没有在线实例，提交将一直排队'
    : '来自 Redis 心跳统计，0 表示判题服务未启动'
})

async function load() {
  loading.value = true
  try {
    status.value = await getQueueStatus()
  } finally {
    loading.value = false
  }
}

onMounted(load)
</script>

<style scoped>
.metrics {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 14px;
}
.metric {
  display: flex;
  flex-direction: column;
  gap: 6px;
  padding: 18px 20px;
  border: 1px solid var(--oj-border);
  border-radius: var(--oj-radius-md);
  background: var(--oj-gray-50);
  transition: border-color var(--oj-duration) var(--oj-ease);
}
.metric.is-idle {
  background: var(--oj-success-soft);
  border-color: var(--oj-success-border);
}
.metric.is-busy {
  background: var(--oj-warning-soft);
  border-color: var(--oj-warning-border);
}
.metric-label {
  font-size: 12.5px;
  color: var(--oj-text-sub);
}
.metric-num {
  font-size: 28px;
  font-weight: 800;
  line-height: 1.1;
  color: var(--oj-gray-900);
}
.metric.is-idle .metric-num {
  color: var(--oj-success);
}
.metric.is-busy .metric-num {
  color: var(--oj-warning);
}
.metric-hint {
  font-size: 12px;
  line-height: 1.6;
  color: var(--oj-text-sub);
}
.foot-note {
  margin: 16px 0 0;
  font-size: 12px;
  line-height: 1.7;
  color: var(--oj-text-weak);
}

@media (max-width: 768px) {
  .metrics {
    grid-template-columns: 1fr;
  }
}
</style>
