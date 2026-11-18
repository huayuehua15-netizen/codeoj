<template>
  <div class="page-container" v-loading="loading">
    <template v-if="detail">
      <!-- 判定横幅：状态同族配色（绿=通过 / 红=错误 / 橙=超限 / 灰=等待） -->
      <section class="verdict" :style="{ background: statusTint(detail.status) }" aria-label="判题结果">
        <div class="verdict-main">
          <span class="verdict-icon" :style="{ background: statusColor(detail.status) }">
            <svg viewBox="0 0 24 24" width="24" height="24" fill="none" stroke="currentColor" stroke-width="2.6" stroke-linecap="round" stroke-linejoin="round" aria-hidden="true">
              <template v-if="iconKind === 'check'"><path d="M5 13 L10 18 L19 7" /></template>
              <template v-else-if="iconKind === 'cross'"><path d="M6 6 L18 18 M18 6 L6 18" /></template>
              <template v-else-if="iconKind === 'clock'"><circle cx="12" cy="12" r="9" /><path d="M12 7 L12 12 L15.5 14.5" /></template>
              <template v-else-if="iconKind === 'code'"><path d="M8 6 L3 12 L8 18 M16 6 L21 12 L16 18" /></template>
              <template v-else><path d="M12 3 A9 9 0 1 1 3 12" /><path d="M12 8 L12 12 L15 13.5" /></template>
            </svg>
          </span>
          <div class="verdict-text">
            <h1 class="verdict-label" :style="{ color: statusColor(detail.status) }">
              {{ statusLabel(detail.status) }}
            </h1>
            <p class="verdict-sub">
              提交 <span class="mono">#{{ detail.id }}</span>
              <span class="sep" aria-hidden="true">·</span>
              {{ formatDateTime(detail.createdAt) }}
            </p>
          </div>
        </div>
        <div class="verdict-meta">
          <router-link :to="`/problems/${detail.problemId}`" class="meta-chip link">
            {{ detail.problemTitle || `题目 ${detail.problemId}` }}
          </router-link>
          <span class="meta-chip">{{ languageLabel(detail.language) }}</span>
          <span class="meta-chip mono">{{ formatTimeUsed(detail.timeUsedMs) }}</span>
          <span class="meta-chip mono">{{ formatMemory(detail.memoryUsedKb) }}</span>
        </div>
      </section>

      <!-- 编译/运行错误信息 -->
      <div v-if="detail.errorInfo" class="error-banner" role="alert">
        <svg viewBox="0 0 24 24" width="16" height="16" fill="none" stroke="currentColor" stroke-width="2.2" stroke-linecap="round" stroke-linejoin="round" aria-hidden="true">
          <path d="M12 3 L22 20 L2 20 Z" /><path d="M12 10 L12 14" /><path d="M12 17 L12 17.01" />
        </svg>
        <pre class="error-text">{{ detail.errorInfo }}</pre>
      </div>

      <!-- 用例级结果（论文 5.7：用例序号、状态、耗时、内存） -->
      <div v-if="detail.cases && detail.cases.length" class="card">
        <header class="head">
          <div>
            <h2>用例结果</h2>
            <p class="sub">
              通过 <b class="pass-num">{{ passCount }}</b> / {{ detail.cases.length }} 个用例
              <span v-if="firstFailedCase" class="first-fail">首个未通过：#{{ firstFailedCase.caseIndex }} {{ firstFailedCase.statusDesc }}</span>
            </p>
          </div>
          <el-progress
            :percentage="passPercent"
            :stroke-width="8"
            :show-text="false"
            :color="allPassed ? 'var(--oj-success)' : 'var(--oj-warning)'"
            class="case-bar"
          />
        </header>
        <el-table :data="detail.cases" class="case-table" row-key="caseIndex">
          <el-table-column prop="caseIndex" label="#" width="76">
            <template #default="{ row }">
              <span class="mono dim">#{{ row.caseIndex }}</span>
            </template>
          </el-table-column>
          <el-table-column label="状态" width="160">
            <template #default="{ row }">
              <span class="status-cell" :style="{ color: statusColor(row.status) }">
                <span class="status-dot" :style="{ background: statusColor(row.status) }" aria-hidden="true" />
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
        <header class="head">
          <div>
            <h2>提交代码</h2>
            <p class="sub">{{ languageLabel(detail.language) }} · {{ codeLines }} 行</p>
          </div>
          <el-button size="small" @click="copyCode">
            <svg viewBox="0 0 24 24" width="13" height="13" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round" aria-hidden="true">
              <rect x="9" y="9" width="12" height="12" rx="2" /><path d="M5 15 L4 15 A2 2 0 0 1 2 13 L2 4 A2 2 0 0 1 4 2 L13 2 A2 2 0 0 1 15 4 L15 5" />
            </svg>
            复制代码
          </el-button>
        </header>
        <div class="code-shell">
          <pre class="code-view"><code>{{ detail.code }}</code></pre>
        </div>
      </div>

      <!-- 他人代码不可见时给出明确说明，而不是空着 -->
      <div v-else class="card code-hidden">
        <svg viewBox="0 0 24 24" width="18" height="18" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round" aria-hidden="true">
          <rect x="3" y="11" width="18" height="10" rx="2" /><path d="M7 11 L7 7 A5 5 0 0 1 17 7 L17 11" />
        </svg>
        <span>出于公平性考虑，提交代码仅本人与管理员可见（论文 4.7）</span>
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
import { computed, onMounted, ref, watch } from 'vue'
import { useRoute } from 'vue-router'
import { ElMessage } from 'element-plus'
import { getSubmission, type SubmissionDetail } from '@/api/submission'
import { useUserStore } from '@/stores/user'
import {
  statusColor,
  statusLabel,
  statusTint,
  formatTimeUsed,
  formatMemory,
  LANGUAGE_OPTIONS,
} from '@/utils/status'
import { formatDateTime } from '@/utils/datetime'

const route = useRoute()
const userStore = useUserStore()

const detail = ref<SubmissionDetail>()
const loading = ref(false)

// 他人代码仅本人与管理员可查（论文 4.7）
const canViewCode = computed(
  () => !!(detail.value && (detail.value.userId === userStore.userId || userStore.isAdmin)),
)

/** 按状态同族选图标：通过 check / 错误 cross / 超限 clock / 编译错 code / 等待 wait */
const iconKind = computed(() => {
  const s = detail.value?.status
  if (s === 2) return 'check'
  if (s === 3 || s === 6 || s === 8) return 'cross'
  if (s === 4 || s === 5) return 'clock'
  if (s === 7) return 'code'
  return 'wait'
})

const passCount = computed(() => (detail.value?.cases || []).filter((c) => c.status === 2).length)
const allPassed = computed(() => !!detail.value?.cases?.length && passCount.value === detail.value.cases.length)

/** 第一个未通过的用例：比"通过 3/10"更能直接告诉用户卡在哪 */
const firstFailedCase = computed(() => (detail.value?.cases || []).find((c) => c.status !== 2))

const passPercent = computed(() => {
  const total = detail.value?.cases?.length || 0
  return total ? Math.round((passCount.value / total) * 100) : 0
})

const codeLines = computed(() => (detail.value?.code ? detail.value.code.split('\n').length : 0))

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

async function load() {
  const id = route.params.id
  // P2-9：路由参数来自地址栏，可能是任意字符串；非正整数直接当作"不存在"，
  // 不发请求（后端会返回 40000，弹一个用户看不懂的错）
  if (!/^\d+$/.test(String(id))) {
    detail.value = undefined
    return
  }
  loading.value = true
  try {
    detail.value = await getSubmission(String(id))
  } finally {
    loading.value = false
  }
}

// P2-4：同一组件在 /submissions/1 → /submissions/2 之间复用，onMounted 不会再跑
watch(() => route.params.id, load)

onMounted(load)
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
  border-radius: var(--oj-radius);
  border: 1px solid var(--oj-border);
}
.verdict-main {
  display: flex;
  align-items: center;
  gap: 16px;
  min-width: 0;
}
.verdict-icon {
  width: 52px;
  height: 52px;
  border-radius: var(--oj-radius);
  display: inline-flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
  color: var(--oj-white);
  box-shadow: var(--oj-shadow-sm);
}
.verdict-label {
  margin: 0;
  font-size: 24px;
  font-weight: 600;
  line-height: 1.2;
}
.verdict-sub {
  margin: 4px 0 0;
  font-size: 12.5px;
  color: var(--oj-gray-500);
}
.verdict-sub .sep {
  margin: 0 6px;
  opacity: 0.6;
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
  border-radius: var(--oj-radius-pill);
  background: var(--oj-white);
  border: 1px solid var(--oj-border);
  color: var(--oj-gray-700);
  font-size: 12.5px;
  font-weight: 500;
}
.meta-chip.link {
  color: var(--oj-primary);
  font-weight: 700;
}
.meta-chip.link:hover {
  border-color: var(--oj-primary);
}
.mono {
  font-family: var(--oj-font-mono);
  font-variant-numeric: tabular-nums;
}

.error-banner {
  display: flex;
  align-items: flex-start;
  gap: 10px;
  padding: 14px 18px;
  margin-bottom: 16px;
  border: 1px solid var(--oj-danger-border);
  border-radius: var(--oj-radius-md);
  background: var(--oj-danger-soft);
  color: var(--oj-danger);
}
.error-banner svg {
  flex-shrink: 0;
  margin-top: 1px;
}
.error-text {
  margin: 0;
  color: var(--oj-danger);
  font-size: 13px;
  line-height: 1.65;
  white-space: pre-wrap;
  word-break: break-word;
  font-family: var(--oj-font-mono);
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
.head .sub {
  margin: 6px 0 0;
  font-size: 12.5px;
  color: var(--oj-text-sub);
}
.pass-num {
  color: var(--oj-success);
  font-size: 16px;
  font-weight: 600;
}
.first-fail {
  margin-left: 10px;
  color: var(--oj-warning);
  font-weight: 600;
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
  color: var(--oj-text-weak);
}

.code-shell {
  border-radius: var(--oj-radius-md);
  overflow: hidden;
  background: var(--oj-code-bg);
  border: 1px solid var(--oj-code-border);
}
.code-view {
  margin: 0;
  background: transparent;
  color: var(--oj-code-text);
  padding: 16px 18px;
  max-height: 480px;
  overflow: auto;
  font-family: var(--oj-font-mono);
  font-size: 13px;
  line-height: 1.7;
  white-space: pre-wrap;
  word-break: break-word;
}

.code-hidden {
  display: flex;
  align-items: center;
  gap: 10px;
  color: var(--oj-text-sub);
  font-size: 13px;
}
.code-hidden svg {
  flex-shrink: 0;
  color: var(--oj-text-weak);
}

.foot-actions {
  display: flex;
  justify-content: flex-end;
  gap: 10px;
  margin-top: 16px;
}
</style>
