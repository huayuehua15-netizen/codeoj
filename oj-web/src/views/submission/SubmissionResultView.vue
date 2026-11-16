<template>
  <div class="page-container" v-loading="loading">
    <template v-if="detail">
      <!-- 判定横幅：状态同族配色（绿=通过 / 红=错误 / 橙=超限 / 灰=等待） -->
      <div class="verdict card" :style="{ background: statusTint(detail.status) }">
        <div class="verdict-main">
          <span class="verdict-icon" :style="{ background: statusColor(detail.status) }">
            <svg viewBox="0 0 24 24" width="22" height="22" fill="none" stroke="#fff" stroke-width="2.6" stroke-linecap="round" stroke-linejoin="round">
              <template v-if="iconKind === 'check'"><path d="M5 13 L10 18 L19 7" /></template>
              <template v-else-if="iconKind === 'cross'"><path d="M6 6 L18 18 M18 6 L6 18" /></template>
              <template v-else-if="iconKind === 'clock'"><circle cx="12" cy="12" r="9" /><path d="M12 7 L12 12 L15.5 14.5" /></template>
              <template v-else-if="iconKind === 'code'"><path d="M8 6 L3 12 L8 18 M16 6 L21 12 L16 18" /></template>
              <template v-else><path d="M12 3 A9 9 0 1 1 3 12" /><path d="M12 8 L12 12 L15 13.5" /></template>
            </svg>
          </span>
          <div class="verdict-text">
            <div class="verdict-label" :style="{ color: statusColor(detail.status) }">
              {{ statusLabel(detail.status) }}
            </div>
            <div class="verdict-sub">提交 #{{ detail.id }} ｜ 提交于 {{ formatDateTime(detail.createdAt) }}</div>
          </div>
        </div>
        <div class="verdict-meta">
          <router-link :to="`/problems/${detail.problemId}`" class="meta-chip link">
            {{ detail.problemTitle || `题目 ${detail.problemId}` }}
          </router-link>
          <span class="meta-chip">{{ languageLabel(detail.language) }}</span>
          <span class="meta-chip mono">耗时 {{ formatTimeUsed(detail.timeUsedMs) }}</span>
          <span class="meta-chip mono">内存 {{ formatMemory(detail.memoryUsedKb) }}</span>
        </div>
      </div>

      <!-- 编译/运行错误信息 -->
      <div v-if="detail.errorInfo" class="error-banner">
        <svg viewBox="0 0 24 24" width="16" height="16" fill="none" stroke="#dc2626" stroke-width="2.2" stroke-linecap="round" stroke-linejoin="round">
          <path d="M12 3 L22 20 L2 20 Z" /><path d="M12 10 L12 14 M12 17 L12 17.01" />
        </svg>
        <span class="error-text">{{ detail.errorInfo }}</span>
      </div>

      <!-- 用例级结果（论文 5.7：用例序号、状态、耗时、内存） -->
      <div v-if="detail.cases && detail.cases.length" class="card">
        <div class="head">
          <h2>用例结果</h2>
          <div class="case-summary">
            <span class="case-sum-text">
              通过 <b class="pass-num">{{ passCount }}</b> / {{ detail.cases.length }} 个用例
            </span>
            <el-progress
              :percentage="passPercent"
              :stroke-width="8"
              :color="passCount === detail.cases.length ? '#16a34a' : '#e6a23c'"
              class="case-bar"
            />
          </div>
        </div>
        <el-table :data="detail.cases" class="case-table">
          <el-table-column prop="caseIndex" label="#" width="70">
            <template #default="{ row }">
              <span class="mono dim">#{{ row.caseIndex }}</span>
            </template>
          </el-table-column>
          <el-table-column label="状态" width="150">
            <template #default="{ row }">
              <span class="status-cell" :style="{ color: statusColor(row.status) }">
                <span class="status-dot" :style="{ background: statusColor(row.status) }" />
                {{ row.statusDesc }}
              </span>
            </template>
          </el-table-column>
          <el-table-column label="耗时" width="120" align="right">
            <template #default="{ row }">
              <span class="mono" :class="{ dim: row.timeUsedMs == null }">{{ formatTimeUsed(row.timeUsedMs) }}</span>
            </template>
          </el-table-column>
          <el-table-column label="内存" width="130" align="right">
            <template #default="{ row }">
              <span class="mono" :class="{ dim: row.memoryUsedKb == null }">{{ formatMemory(row.memoryUsedKb) }}</span>
            </template>
          </el-table-column>
        </el-table>
      </div>

      <!-- 提交代码：仅本人与管理员可查（论文 4.7） -->
      <div v-if="canViewCode" class="card">
        <div class="head">
          <h2>提交代码</h2>
          <div class="code-actions">
            <span class="lang-chip">{{ languageLabel(detail.language) }}</span>
            <el-button size="small" @click="copyCode">
              <svg viewBox="0 0 24 24" width="13" height="13" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
                <rect x="9" y="9" width="12" height="12" rx="2" /><path d="M5 15 L4 15 A2 2 0 0 1 2 13 L2 4 A2 2 0 0 1 4 2 L13 2 A2 2 0 0 1 15 4 L15 5" />
              </svg>
              复制
            </el-button>
          </div>
        </div>
        <pre class="code-view"><code>{{ detail.code }}</code></pre>
      </div>

      <div class="foot-actions">
        <el-button @click="$router.push('/submissions')">返回提交记录</el-button>
        <el-button type="primary" @click="$router.push(`/problems/${detail.problemId}`)">查看题目</el-button>
      </div>
    </template>

    <div v-else-if="!loading" class="card">
      <el-empty description="提交不存在或无权查看">
        <el-button type="primary" @click="$router.push('/submissions')">返回提交记录</el-button>
      </el-empty>
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { useRoute } from 'vue-router'
import { ElMessage } from 'element-plus'
import { getSubmission, type SubmissionDetail } from '@/api/submission'
import { useUserStore } from '@/stores/user'
import { statusColor, statusLabel, statusTint, formatTimeUsed, formatMemory, LANGUAGE_OPTIONS } from '@/utils/status'
import { formatDateTime } from '@/utils/datetime'

const route = useRoute()
const userStore = useUserStore()

const detail = ref<SubmissionDetail>()
const loading = ref(false)

// 他人代码仅本人与管理员可查（论文 4.7）
const canViewCode = computed(
  () => detail.value && (detail.value.userId === userStore.userId || userStore.isAdmin),
)

/** 按状态同族选图标：通过✓ / 错误✕ / 超限⏱ / 编译错 code / 等待↻ */
const iconKind = computed(() => {
  const s = detail.value?.status
  if (s === 2) return 'check'
  if (s === 3 || s === 6 || s === 8) return 'cross'
  if (s === 4 || s === 5) return 'clock'
  if (s === 7) return 'code'
  return 'wait'
})

const passCount = computed(() => (detail.value?.cases || []).filter((c) => c.status === 2).length)

const passPercent = computed(() => {
  const total = detail.value?.cases?.length || 0
  return total ? Math.round((passCount.value / total) * 100) : 0
})

function languageLabel(lang?: string) {
  return LANGUAGE_OPTIONS.find((l) => l.value === lang)?.label || lang || '-'
}

async function copyCode() {
  try {
    await navigator.clipboard.writeText(detail.value?.code || '')
    ElMessage.success('代码已复制到剪贴板')
  } catch {
    ElMessage.error('复制失败，请手动选择复制')
  }
}

onMounted(async () => {
  loading.value = true
  try {
    detail.value = await getSubmission(route.params.id as string)
  } finally {
    loading.value = false
  }
})
</script>

<style scoped>
.verdict {
  display: flex;
  align-items: center;
  justify-content: space-between;
  flex-wrap: wrap;
  gap: 16px;
  padding: 22px 28px;
  margin-bottom: 16px;
}
.verdict-main {
  display: flex;
  align-items: center;
  gap: 16px;
}
.verdict-icon {
  width: 52px;
  height: 52px;
  border-radius: 14px;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
  box-shadow: 0 4px 12px -4px rgba(17, 24, 39, 0.25);
}
.verdict-label {
  font-size: 24px;
  font-weight: 800;
  line-height: 1.2;
}
.verdict-sub {
  margin-top: 4px;
  font-size: 12.5px;
  color: #6b7280;
}
.verdict-meta {
  display: flex;
  align-items: center;
  flex-wrap: wrap;
  gap: 8px;
}
.meta-chip {
  display: inline-flex;
  align-items: center;
  padding: 4px 12px;
  border-radius: 999px;
  background: rgba(255, 255, 255, 0.75);
  border: 1px solid rgba(17, 24, 39, 0.06);
  color: #4b5563;
  font-size: 12.5px;
}
.meta-chip.link {
  color: var(--oj-primary);
  font-weight: 600;
}
.meta-chip.link:hover {
  background: #fff;
}
.mono {
  font-family: 'JetBrains Mono', 'Cascadia Code', Consolas, Menlo, monospace;
  font-variant-numeric: tabular-nums;
}
.error-banner {
  display: flex;
  align-items: flex-start;
  gap: 10px;
  padding: 14px 18px;
  margin-bottom: 16px;
  border: 1px solid #fecaca;
  border-radius: 12px;
  background: #fef2f2;
}
.error-text {
  color: #b91c1c;
  font-size: 13.5px;
  line-height: 1.6;
  white-space: pre-wrap;
  word-break: break-all;
  font-family: 'JetBrains Mono', Consolas, monospace;
}
.head {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  flex-wrap: wrap;
  gap: 12px;
  margin-bottom: 12px;
}
.head h2 {
  margin: 0;
}
.case-summary {
  display: flex;
  align-items: center;
  gap: 12px;
}
.case-sum-text {
  font-size: 13.5px;
  color: #6b7280;
  white-space: nowrap;
}
.pass-num {
  color: #16a34a;
  font-size: 16px;
}
.case-bar {
  width: 140px;
}
.status-cell {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  font-weight: 600;
  font-size: 13px;
}
.status-dot {
  width: 7px;
  height: 7px;
  border-radius: 50%;
  flex-shrink: 0;
}
.mono.dim,
.dim {
  color: #a8abb2;
}
.code-actions {
  display: flex;
  align-items: center;
  gap: 10px;
}
.lang-chip {
  display: inline-flex;
  align-items: center;
  padding: 3px 10px;
  border-radius: 999px;
  background: #f4f4f5;
  border: 1px solid #e4e7ed;
  color: #606266;
  font-size: 12px;
}
.code-view {
  margin: 0;
  background: #0f172a;
  color: #e2e8f0;
  border-radius: 10px;
  padding: 16px 18px;
  max-height: 480px;
  overflow: auto;
  font-family: 'JetBrains Mono', 'Cascadia Code', Consolas, Menlo, monospace;
  font-size: 13px;
  line-height: 1.7;
  white-space: pre-wrap;
  word-break: break-all;
}
.foot-actions {
  display: flex;
  justify-content: flex-end;
  gap: 10px;
  margin-top: 16px;
}
</style>
