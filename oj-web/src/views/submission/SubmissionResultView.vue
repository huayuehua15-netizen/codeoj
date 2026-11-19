<template>
  <div class="result-page" v-loading="loading">
    <template v-if="detail">
      <!-- 判定概览：状态、提交信息、关键读数 -->
      <header class="result-card" :class="verdictTone">
        <div class="result-main">
          <div class="result-kicker">
            <span class="kicker">JUDGEMENT</span>
            <span class="folio">SUBMISSION #{{ String(detail.id).padStart(5, '0') }}</span>
          </div>
          <div class="result-title-row">
            <span class="result-icon" :style="{ color: statusColor(detail.status), borderColor: statusColor(detail.status) }" aria-hidden="true">
              {{ statusGlyph }}
            </span>
            <h1 class="result-label" :style="{ color: statusColor(detail.status) }">{{ statusLabel(detail.status) }}</h1>
          </div>
          <p class="result-sub">
            {{ formatDateTime(detail.createdAt) }}
            <span class="sep" aria-hidden="true">/</span>
            {{ languageLabel(detail.language) }}
          </p>
          <router-link :to="`/problems/${detail.problemId}`" class="result-problem">
            {{ detail.problemTitle || `题目 ${detail.problemId}` }}
            <span aria-hidden="true">→</span>
          </router-link>
        </div>

        <dl class="result-meta" aria-label="提交信息">
          <div class="meta-cell">
            <dt>语言</dt>
            <dd>{{ languageLabel(detail.language) }}</dd>
          </div>
          <div class="meta-cell">
            <dt>耗时</dt>
            <dd class="mono">{{ formatTimeUsed(detail.timeUsedMs) }}</dd>
          </div>
          <div class="meta-cell">
            <dt>内存</dt>
            <dd class="mono">{{ formatMemory(detail.memoryUsedKb) }}</dd>
          </div>
          <div class="meta-cell">
            <dt>提交编号</dt>
            <dd class="mono">#{{ detail.id }}</dd>
          </div>
        </dl>
      </header>

      <!-- 编译/运行错误信息 -->
      <div v-if="detail.errorInfo" class="error-banner" role="alert">
        <p class="error-title">COMPILE / RUNTIME OUTPUT</p>
        <pre class="error-text">{{ detail.errorInfo }}</pre>
      </div>

      <!-- 用例级结果 -->
      <section v-if="detail.cases && detail.cases.length" class="case-card">
        <div class="card-head">
          <div>
            <p class="kicker">TEST CASES</p>
            <h2 class="card-title">用例结果</h2>
            <p class="card-sub">
              通过 <b class="pass-num">{{ passCount }}</b> / {{ detail.cases.length }} 个用例
              <span v-if="firstFailedCase" class="first-fail">· 首个未通过 #{{ firstFailedCase.caseIndex }} {{ firstFailedCase.statusDesc }}</span>
            </p>
          </div>
          <div class="score-figure" aria-label="通过用例比例">
            <span class="score-num">{{ passCount }}</span>
            <span class="score-line">/</span>
            <span class="score-den">{{ detail.cases.length }}</span>
          </div>
        </div>

        <div class="case-bar" aria-hidden="true">
          <span class="case-bar-fill" :class="{ ok: allPassed }" :style="{ width: `${passPercent}%` }" />
        </div>

        <div class="case-head" aria-hidden="true">
          <span>CASE</span>
          <span>状态</span>
          <span>耗时</span>
          <span>内存</span>
        </div>
        <ul class="case-list">
          <li v-for="row in detail.cases" :key="row.caseIndex" class="case-row">
            <span class="case-no mono">#{{ String(row.caseIndex).padStart(2, '0') }}</span>
            <span class="case-status" :style="{ color: statusColor(row.status) }">
              <span class="status-dot" :style="{ background: statusColor(row.status) }" aria-hidden="true" />
              {{ row.statusDesc }}
            </span>
            <span class="mono case-num" :class="{ dim: row.timeUsedMs == null }">{{ formatTimeUsed(row.timeUsedMs) }}</span>
            <span class="mono case-num" :class="{ dim: row.memoryUsedKb == null }">{{ formatMemory(row.memoryUsedKb) }}</span>
          </li>
        </ul>
      </section>

      <!-- 提交代码：仅本人与管理员可查 -->
      <section v-if="canViewCode" class="code-card">
        <div class="card-head code-head">
          <div>
            <p class="kicker">SOURCE</p>
            <h2 class="card-title">提交代码</h2>
            <p class="card-sub">{{ languageLabel(detail.language) }} · {{ codeLines }} 行</p>
          </div>
          <el-button size="small" @click="copyCode">复制代码</el-button>
        </div>
        <div class="code-shell">
          <pre class="code-view"><code>{{ detail.code }}</code></pre>
        </div>
      </section>

      <!-- 他人代码不可见时给出明确说明 -->
      <section v-else class="code-hidden">
        <span class="lock-badge" aria-hidden="true">PRIVATE</span>
        <span>出于公平性考虑，提交代码仅本人与管理员可见。</span>
      </section>

      <div class="foot-actions">
        <router-link class="foot-link" to="/submissions">← 返回提交记录</router-link>
        <el-button type="primary" @click="$router.push(`/problems/${detail.problemId}`)">再做一次</el-button>
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
  formatTimeUsed,
  formatMemory,
  LANGUAGE_OPTIONS,
} from '@/utils/status'
import { formatDateTime } from '@/utils/datetime'

const route = useRoute()
const userStore = useUserStore()

const detail = ref<SubmissionDetail>()
const loading = ref(false)

// 他人代码仅本人与管理员可查
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

const statusGlyph = computed(() => {
  switch (iconKind.value) {
    case 'check': return '✓'
    case 'clock': return '!'
    case 'code': return '</>'
    case 'wait': return '…'
    default: return '✗'
  }
})

const verdictTone = computed(() => {
  const s = detail.value?.status
  if (s === 2) return 'tone-ok'
  if (s === 4 || s === 5) return 'tone-warn'
  if (s === 0 || s === 1) return 'tone-wait'
  return 'tone-bad'
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
  // 路由参数来自地址栏，可能是任意字符串；非正整数直接当作"不存在"，不发请求
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

// 同一组件在 /submissions/1 → /submissions/2 之间复用，onMounted 不会再跑
watch(() => route.params.id, load)

onMounted(load)
</script>

<style scoped>
.result-page {
  max-width: 1160px;
  margin: 0 auto;
  padding: 38px 32px 64px;
}

/* ---------- 判定概览 ---------- */
.result-card {
  display: grid;
  grid-template-columns: minmax(0, 1fr) 360px;
  gap: 40px;
  align-items: center;
  padding: 24px 26px;
  background: var(--oj-card);
  border: 1px solid var(--oj-border);
  border-radius: 10px;
  box-shadow: var(--oj-shadow-sm);
  position: relative;
  overflow: hidden;
}
.result-card::before {
  content: '';
  position: absolute;
  left: 0;
  top: 0;
  bottom: 0;
  width: 3px;
  background: var(--oj-gray-300);
}
.result-card.tone-ok::before { background: var(--oj-success); }
.result-card.tone-warn::before { background: var(--oj-warning); }
.result-card.tone-bad::before { background: var(--oj-danger); }
.result-kicker {
  display: flex;
  align-items: baseline;
  gap: 10px;
}
.result-title-row {
  display: flex;
  align-items: center;
  gap: 14px;
  margin-top: 12px;
}
.result-icon {
  width: 42px;
  height: 42px;
  flex-shrink: 0;
  border: 1px solid;
  border-radius: 50%;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  font-family: var(--oj-font-mono);
  font-size: 17px;
  font-weight: 700;
}
.result-label {
  margin: 0;
  font-family: var(--oj-font-display);
  font-size: 38px;
  font-weight: 800;
  line-height: 1.08;
  letter-spacing: -0.03em;
}
.result-sub {
  margin: 12px 0 0;
  font-family: var(--oj-font-mono);
  font-size: 10px;
  color: var(--oj-text-sub);
}
.result-sub .sep {
  margin: 0 7px;
  color: var(--oj-gray-300);
}
.result-problem {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  margin-top: 16px;
  font-size: 14px;
  font-weight: 650;
  color: var(--oj-text);
  border-bottom: 1px solid var(--oj-border-strong);
  padding-bottom: 2px;
}
.result-problem:hover {
  color: var(--oj-primary);
  border-color: var(--oj-primary);
}

.result-meta {
  display: grid;
  grid-template-columns: 1fr 1fr;
  margin: 0;
  overflow: hidden;
  border: 1px solid var(--oj-border);
  border-radius: 8px;
  background: var(--oj-gray-50);
}
.meta-cell {
  padding: 10px 12px;
  border-right: 1px solid var(--oj-border);
  border-bottom: 1px solid var(--oj-border);
}
.meta-cell:nth-child(2n) {
  border-right: 0;
}
.meta-cell:nth-child(n + 3) {
  border-bottom: 0;
}
.meta-cell dt {
  font-family: var(--oj-font-mono);
  font-size: 8px;
  letter-spacing: 0.08em;
  color: var(--oj-text-weak);
}
.meta-cell dd {
  margin: 4px 0 0;
  font-size: 13px;
  color: var(--oj-gray-800);
}

/* ---------- 错误输出 ---------- */
.error-banner {
  margin-top: 18px;
  padding: 14px 16px;
  border: 1px solid var(--oj-danger-border);
  border-left: 3px solid var(--oj-danger);
  border-radius: 7px;
  background: var(--oj-danger-soft);
}
.error-title {
  margin: 0 0 8px;
  font-family: var(--oj-font-mono);
  font-size: 8.5px;
  letter-spacing: 0.1em;
  color: var(--oj-danger);
}
.error-text {
  margin: 0;
  color: #7a2626;
  font-size: 12px;
  line-height: 1.7;
  white-space: pre-wrap;
  word-break: break-word;
  font-family: var(--oj-font-mono);
}

/* ---------- 判分卡 ---------- */
.case-card {
  margin-top: 18px;
  background: var(--oj-card);
  border: 1px solid var(--oj-border);
  border-radius: 10px;
  box-shadow: var(--oj-shadow-sm);
  overflow: hidden;
}
.card-head {
  display: flex;
  align-items: flex-end;
  justify-content: space-between;
  gap: 20px;
  padding: 18px 20px 14px;
  border-bottom: 1px solid var(--oj-border);
}
.card-title {
  margin: 4px 0 0;
  font-size: 19px;
  font-weight: 750;
  letter-spacing: -0.03em;
}
.card-sub {
  margin: 5px 0 0;
  font-size: 11px;
  color: var(--oj-text-sub);
}
.pass-num {
  color: var(--oj-success);
  font-family: var(--oj-font-mono);
  font-size: 13px;
}
.first-fail {
  color: var(--oj-danger);
}
.score-figure {
  display: flex;
  align-items: baseline;
  gap: 5px;
  flex-shrink: 0;
}
.score-num {
  font-family: var(--oj-font-mono);
  font-size: 34px;
  line-height: 1;
  font-weight: 600;
  color: var(--oj-success);
}
.score-line {
  font-size: 17px;
  color: var(--oj-gray-300);
}
.score-den {
  font-family: var(--oj-font-mono);
  font-size: 18px;
  line-height: 1;
  color: var(--oj-text-sub);
}

.case-bar {
  height: 3px;
  background: var(--oj-gray-100);
}
.case-bar-fill {
  display: block;
  height: 100%;
  background: var(--oj-warning);
  transition: width 0.3s var(--oj-ease);
}
.case-bar-fill.ok {
  background: var(--oj-success);
}

.case-head,
.case-row {
  display: grid;
  grid-template-columns: 78px minmax(0, 1fr) 110px 110px;
  gap: 14px;
  align-items: center;
  padding: 0 20px;
}
.case-head {
  height: 32px;
  background: var(--oj-gray-50);
  border-bottom: 1px solid var(--oj-border);
  font-family: var(--oj-font-mono);
  font-size: 8.5px;
  letter-spacing: 0.09em;
  color: var(--oj-text-weak);
}
.case-head span:nth-child(3),
.case-head span:nth-child(4) {
  text-align: right;
}
.case-list {
  list-style: none;
  margin: 0;
  padding: 0;
}
.case-row {
  min-height: 42px;
  border-bottom: 1px solid var(--oj-border);
}
.case-row:last-child {
  border-bottom: 0;
}
.case-no {
  font-size: 10px;
  color: var(--oj-text-weak);
}
.case-status {
  display: inline-flex;
  align-items: center;
  gap: 7px;
  font-size: 11.5px;
  font-weight: 600;
}
.status-dot {
  width: 5px;
  height: 5px;
  border-radius: 50%;
  flex-shrink: 0;
}
.case-num {
  font-size: 10px;
  color: var(--oj-gray-700);
  text-align: right;
}
.case-num.dim {
  color: var(--oj-text-weak);
}

/* ---------- 代码 ---------- */
.code-card {
  margin-top: 18px;
  background: var(--oj-card);
  border: 1px solid var(--oj-border);
  border-radius: 10px;
  box-shadow: var(--oj-shadow-sm);
  overflow: hidden;
}
.code-head {
  align-items: center;
}
.code-shell {
  background: var(--oj-code-bg);
}
.code-view {
  margin: 0;
  color: var(--oj-code-text);
  padding: 16px 18px;
  max-height: 480px;
  overflow: auto;
  font-family: var(--oj-font-mono);
  font-size: 12px;
  line-height: 1.75;
  white-space: pre-wrap;
  word-break: break-word;
}

/* 他人代码不可见 */
.code-hidden {
  display: flex;
  align-items: center;
  gap: 12px;
  margin-top: 18px;
  padding: 15px 16px;
  border: 1px dashed var(--oj-border-strong);
  border-radius: 8px;
  background: var(--oj-gray-50);
  color: var(--oj-text-sub);
  font-size: 12px;
}
.lock-badge {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  padding: 2px 7px;
  border: 1px solid var(--oj-border-strong);
  border-radius: 3px;
  background: var(--oj-card);
  font-family: var(--oj-font-mono);
  font-size: 8px;
  letter-spacing: 0.08em;
  color: var(--oj-text-sub);
  flex-shrink: 0;
}

.foot-actions {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-top: 22px;
  padding-top: 16px;
  border-top: 1px solid var(--oj-border);
}
.foot-link {
  font-size: 11.5px;
  color: var(--oj-text-sub);
}
.foot-link:hover {
  color: var(--oj-primary);
}

@media (max-width: 860px) {
  .result-page {
    padding: 26px 18px 52px;
  }
  .result-card {
    grid-template-columns: 1fr;
    gap: 22px;
  }
  .result-label {
    font-size: 36px;
  }
  .result-meta {
    max-width: 420px;
  }
  .case-head,
  .case-row {
    grid-template-columns: 56px minmax(0, 1fr) 76px 76px;
    gap: 10px;
    padding: 0 16px;
  }
}
@media (max-width: 560px) {
  .case-head {
    display: none;
  }
  .case-row {
    grid-template-columns: 52px minmax(0, 1fr);
    grid-auto-rows: auto;
    padding: 9px 16px;
  }
  .case-num {
    grid-column: 2;
    text-align: left;
  }
  .case-num + .case-num {
    margin-top: -4px;
  }
  .foot-actions {
    gap: 12px;
    flex-wrap: wrap;
  }
}
</style>
