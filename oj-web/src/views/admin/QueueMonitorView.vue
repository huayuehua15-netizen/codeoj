<template>
  <AdminShell
    title="判题队列监控"
    section="QUEUE · 判题调度"
    description="查看判题队列积压与今日吞吐，用于排查拥堵。"
  >
    <template #actions>
      <el-button :loading="loading" @click="load">刷新</el-button>
    </template>

    <div class="monitor" v-loading="loading">
      <div class="monitor-cell" :class="backlogClass">
        <span class="monitor-label">BACKLOG / 队列积压</span>
        <span class="monitor-num">{{ status ? status.backlog : '—' }}</span>
        <span class="monitor-state">
          <span class="state-dot" aria-hidden="true" />
          {{ backlogState }}
        </span>
        <p class="monitor-hint">{{ backlogHint }}</p>
      </div>

      <div class="monitor-cell">
        <span class="monitor-label">JUDGED TODAY / 今日判题量</span>
        <span class="monitor-num">{{ status ? status.judgedToday : '—' }}</span>
        <span class="monitor-state neutral">累计完成</span>
        <p class="monitor-hint">自今日 00:00 起累计完成的判题任务。</p>
      </div>

      <div class="monitor-cell">
        <span class="monitor-label">INSTANCES / 判题实例数</span>
        <span class="monitor-num">{{ status ? status.judgeInstances : '—' }}</span>
        <span class="monitor-state" :class="{ alarm: status && status.judgeInstances === 0 }">
          <span class="state-dot" aria-hidden="true" />
          {{ status && status.judgeInstances === 0 ? '无在线实例' : '实例在线' }}
        </span>
        <p class="monitor-hint">{{ instancesHint }}</p>
      </div>
    </div>

    <footer class="monitor-foot">
      <p class="foot-note">
        积压量统计的是队列中尚未被消费者取走的任务（不含已取走未确认的），每 30 秒刷新一次缓存。
      </p>
      <div v-if="status?.queriedAt" class="monitor-meta">
        <span>查询于 <b class="mono">{{ status.queriedAt }}</b></span>
        <span aria-hidden="true">·</span>
        <span>积压量最多滞后 30s</span>
      </div>
    </footer>
  </AdminShell>
</template>

<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { getQueueStatus } from '@/api/admin'
import AdminShell from '@/components/AdminShell.vue'

const status = ref<Awaited<ReturnType<typeof getQueueStatus>>>()
const loading = ref(false)

/** 积压量分档：0 空闲、<10 正常、>=10 拥堵，颜色跟着变 */
const backlogClass = computed(() => {
  const backlog = status.value?.backlog ?? 0
  if (backlog === 0) return 'is-idle'
  if (backlog < 10) return 'is-normal'
  return 'is-busy'
})

const backlogState = computed(() => {
  const backlog = status.value?.backlog ?? 0
  if (!status.value) return '读取中'
  if (backlog === 0) return '空闲'
  if (backlog < 10) return '正常'
  return '拥堵'
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
.monitor {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 12px;
}
.monitor-cell {
  display: flex;
  flex-direction: column;
  gap: 7px;
  padding: 20px 22px 18px;
  min-height: 190px;
  background: var(--oj-card);
  border: 1px solid var(--oj-border);
  border-radius: 10px;
  box-shadow: var(--oj-shadow-sm);
}
.monitor-cell.is-idle {
  border-top-color: var(--oj-success);
}
.monitor-cell.is-busy {
  border-top-color: var(--oj-warning);
}
.monitor-label {
  font-family: var(--oj-font-mono);
  font-size: 8.5px;
  letter-spacing: 0.06em;
  color: var(--oj-text-weak);
}
.monitor-num {
  font-family: var(--oj-font-mono);
  font-size: 48px;
  font-weight: 600;
  line-height: 1;
  letter-spacing: -0.05em;
  color: var(--oj-text);
}
.monitor-cell.is-idle .monitor-num {
  color: var(--oj-success);
}
.monitor-cell.is-busy .monitor-num {
  color: var(--oj-warning);
}
.monitor-state {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  font-size: 11px;
  font-weight: 600;
  letter-spacing: 0.04em;
  color: var(--oj-success);
}
.monitor-state.neutral {
  color: var(--oj-text-sub);
}
.monitor-state.alarm {
  color: var(--oj-danger);
}
.state-dot {
  width: 5px;
  height: 5px;
  border-radius: 50%;
  background: currentColor;
  flex-shrink: 0;
}
.monitor-hint {
  margin: auto 0 0;
  font-size: 11px;
  line-height: 1.8;
  color: var(--oj-text-sub);
}

.monitor-foot {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 24px;
  margin-top: 16px;
  padding-top: 12px;
  border-top: 1px solid var(--oj-border);
}
.foot-note {
  margin: 0;
  max-width: 54em;
  font-size: 10.5px;
  line-height: 1.8;
  color: var(--oj-text-weak);
}
.monitor-meta {
  display: flex;
  gap: 8px;
  font-family: var(--oj-font-mono);
  font-size: 9px;
  color: var(--oj-text-weak);
  white-space: nowrap;
}
.monitor-meta b {
  color: var(--oj-text-sub);
  font-weight: 500;
}

@media (max-width: 860px) {
  .monitor {
    grid-template-columns: 1fr;
  }
  .monitor-cell {
    min-height: 0;
    padding: 16px 18px;
  }
  .monitor-num {
    font-size: 40px;
  }
  .monitor-foot {
    flex-direction: column;
    gap: 8px;
  }
}
</style>
