<template>
  <!-- 加载失败空态：题目不存在/已下架/后端不可用时不白屏 -->
  <div v-if="!problem" class="page-container">
    <div class="card empty-sheet">
      <el-empty :description="loadError ? '题目不存在或已下架' : '题目加载中…'">
        <el-button v-if="loadError" type="primary" @click="$router.push('/problems')">返回题库</el-button>
      </el-empty>
    </div>
  </div>

  <div v-else class="problem-detail">
    <!-- 左：题面 -->
    <article class="statement">
      <header class="prob-head">
        <div class="prob-topline">
          <span class="kicker">PROBLEM #{{ String(problem.id).padStart(4, '0') }}</span>
          <span class="folio">STATEMENT</span>
        </div>

        <div class="title-row">
          <h1 class="prob-title">{{ problem.title }}</h1>
          <button
            v-if="userStore.isLoggedIn"
            class="fav-btn"
            :class="{ active: favorited }"
            type="button"
            :aria-pressed="favorited"
            @click="toggleFav"
          >
            <svg viewBox="0 0 24 24" width="13" height="13" aria-hidden="true"
              :fill="favorited ? 'currentColor' : 'none'"
              stroke="currentColor" stroke-width="1.8" stroke-linejoin="round">
              <path d="M12 3.2 L14.7 8.7 L20.8 9.6 L16.4 13.9 L17.4 20 L12 17.2 L6.6 20 L7.6 13.9 L3.2 9.6 L9.3 8.7 Z" />
            </svg>
            {{ favorited ? '已收藏' : '收藏' }}
          </button>
        </div>

        <dl class="prob-meta">
          <div class="meta-item">
            <dt>难度</dt>
            <dd><span class="oj-diff" :class="`diff-${problem.difficulty}`">{{ difficultyInfo.label }}</span></dd>
          </div>
          <div class="meta-item">
            <dt>时间限制</dt>
            <dd class="mono">{{ limitTime }}</dd>
          </div>
          <div class="meta-item">
            <dt>内存限制</dt>
            <dd class="mono">{{ problem.memoryLimitMb }} MB</dd>
          </div>
          <div class="meta-item">
            <dt>通过率</dt>
            <dd class="mono" :class="rateClass">{{ passRate }}%<span class="meta-frac">{{ problem.acceptedCount }}/{{ problem.submitCount }}</span></dd>
          </div>
        </dl>

        <div class="prob-tags">
          <span class="tag-caption">支持语言</span>
          <span v-for="l in allowedLanguages" :key="l.value" class="tag tag-lang">{{ l.label }}</span>
          <span v-for="t in problem.tags || []" :key="t" class="tag">{{ t }}</span>
        </div>
      </header>

      <section class="statement-section">
        <div class="section-label">
          <h2>题目描述</h2>
          <span>DESCRIPTION</span>
        </div>
        <div class="statement-body">
          <MarkdownPreview :content="problem.description" />
        </div>
      </section>

      <section v-if="problem.samples && problem.samples.length" class="statement-section">
        <div class="section-label">
          <h2>样例</h2>
          <span>SAMPLES</span>
        </div>
        <div class="samples">
          <div v-for="(s, i) in problem.samples" :key="i" class="sample-set">
            <div class="sample-item">
              <div class="sample-head">
                <span class="sample-title">INPUT {{ String(i + 1).padStart(2, '0') }}</span>
                <button class="copy-btn" type="button" @click="copyText(s.input)">复制</button>
              </div>
              <pre>{{ s.input }}</pre>
            </div>
            <div class="sample-item">
              <div class="sample-head">
                <span class="sample-title">OUTPUT {{ String(i + 1).padStart(2, '0') }}</span>
                <button class="copy-btn" type="button" @click="copyText(s.output)">复制</button>
              </div>
              <pre>{{ s.output }}</pre>
            </div>
          </div>
        </div>
      </section>
    </article>

    <!-- 右：代码台 -->
    <aside class="workspace">
      <div class="editor-card">
        <div class="editor-toolbar">
          <span class="editor-label">
            <span class="editor-dot" aria-hidden="true"></span>
            CODE
          </span>
          <el-select v-model="language" class="lang-select" aria-label="编程语言" @change="onLanguageChange">
            <el-option v-for="lang in allowedLanguages" :key="lang.value" :label="lang.label" :value="lang.value" />
          </el-select>
          <div class="toolbar-actions">
            <el-button v-if="language === 'js'" :disabled="running" class="tool-btn" @click="runConsole">
              <svg class="btn-icon" viewBox="0 0 24 24" width="11" height="11" fill="currentColor" aria-hidden="true">
                <path d="M8 5 L19 12 L8 19 Z" />
              </svg>
              运行
            </el-button>
            <el-button type="primary" class="submit-btn" :loading="submitting" :disabled="!userStore.isLoggedIn || !code.trim()" @click="handleSubmit">
              {{ userStore.isLoggedIn ? '提交代码' : '登录后提交' }}
            </el-button>
          </div>
        </div>
        <CodeEditor v-model="code" :language="language" />
        <div class="editor-foot">
          <span>{{ language.toUpperCase() }} · SYNTAX HIGHLIGHT</span>
          <span>Ctrl / ⌘ + C COPY</span>
        </div>
      </div>

      <!-- 控制台（JS 题目：浏览器沙箱自测运行） -->
      <div v-if="language === 'js'" class="console-panel">
        <div class="console-head">
          <span class="console-title">CONSOLE / 自测运行</span>
          <button class="console-run" type="button" :disabled="running" @click="runConsole">
            {{ running ? '运行中…' : '▶ 运行' }}
          </button>
        </div>
        <el-input
          v-model="consoleInput"
          type="textarea"
          :rows="3"
          placeholder="自定义测试输入（留空则使用样例 1 输入）"
          class="console-input"
        />
        <div v-if="consoleResult" class="console-out" :class="{ err: !!consoleResult.error }">
          <div v-if="consoleResult.error" class="console-err">✕ {{ consoleResult.error }}</div>
          <pre class="console-stdout">{{ consoleResult.stdout || '(无输出)' }}</pre>
          <div class="console-meta">耗时 {{ consoleResult.elapsedMs }}ms · 浏览器沙箱（仅 JS）</div>
        </div>
      </div>

      <!-- 判题状态机：IDLE→SUBMITTING→QUEUED→JUDGING→FINISHED -->
      <section v-if="judgeState !== 'IDLE'" class="judge-card" aria-live="polite">
        <div class="judge-head">
          <span class="judge-title">判题状态</span>
          <span class="folio">JUDGEMENT</span>
        </div>

        <el-steps
          v-if="judgeState !== 'FINISHED'"
          :active="stepIndex"
          align-center
          class="judge-steps"
          finish-status="success"
          :process-status="judgeState === 'JUDGING' ? 'process' : 'wait'"
        >
          <el-step title="提交" />
          <el-step title="排队" />
          <el-step title="判题" />
          <el-step title="结果" />
        </el-steps>

        <div v-else-if="result" class="verdict" :class="result.status === 2 ? 'is-ok' : 'is-bad'">
          <span class="verdict-icon" aria-hidden="true">{{ result.status === 2 ? '✓' : '✗' }}</span>
          <div class="verdict-text">
            <p class="verdict-label">{{ statusLabel(result.status) }}</p>
            <p class="verdict-meta">{{ resultSubTitle || '判题结果已推送' }}</p>
          </div>
          <div class="verdict-actions">
            <el-button size="small" @click="$router.push(`/submissions/${result.submissionId}`)">查看明细</el-button>
            <el-button size="small" type="primary" @click="resetJudge">再交一次</el-button>
          </div>
        </div>

        <div v-if="judgeState === 'QUEUED' || judgeState === 'JUDGING'" class="judge-abort">
          <button type="button" class="abort-link" @click="abortJudge">隐藏本次进度（结果可在提交记录查看）</button>
        </div>
      </section>

      <!-- 未提交时的常驻说明：让右栏不是一块空白 -->
      <section v-if="judgeState === 'IDLE'" class="judge-card judge-idle">
        <div class="judge-head">
          <span class="judge-title">判题流程</span>
          <span class="folio">JUDGEMENT</span>
        </div>
        <div class="idle-flow">
          <div class="idle-step">
            <span class="idle-no">01</span>
            <div>
              <strong>提交入队</strong>
              <span>代码进入判题队列</span>
            </div>
          </div>
          <div class="idle-step">
            <span class="idle-no">02</span>
            <div>
              <strong>编译构建</strong>
              <span>在沙箱内编译当前语言</span>
            </div>
          </div>
          <div class="idle-step">
            <span class="idle-no">03</span>
            <div>
              <strong>逐用例运行</strong>
              <span>限制时间与内存并比对输出</span>
            </div>
          </div>
          <div class="idle-step">
            <span class="idle-no">04</span>
            <div>
              <strong>实时判定</strong>
              <span>结果推送到本页并留存记录</span>
            </div>
          </div>
        </div>
        <p class="idle-note">提交后无需手动刷新。评测机空闲时通常几秒内返回；高峰期请以队列状态为准。</p>
      </section>
    </aside>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, onUnmounted, ref, watch } from 'vue'
import { useRoute } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { isFavorited, toggleFavorite } from '@/api/favorite'
import { runInSandbox, type RunResult } from '@/utils/jsRunner'
import { getProblem, type Problem } from '@/api/problem'
import { submitCode, getSubmission } from '@/api/submission'
import { useUserStore } from '@/stores/user'
import { onJudgePush } from '@/api/ws'
import { CODE_TEMPLATES, DIFFICULTY_MAP, LANGUAGE_OPTIONS, statusLabel } from '@/utils/status'
import MarkdownPreview from '@/components/MarkdownPreview.vue'
import CodeEditor from '@/components/CodeEditor.vue'

const route = useRoute()
const userStore = useUserStore()

const problem = ref<Problem>()
const loadError = ref(false)
const language = ref('cpp')
const prevLanguage = ref('cpp')
const code = ref('')
const submitting = ref(false)

// 判题状态机（论文 5.7）：WebSocket 消息与 HTTP 响应共同推进状态迁移
const judgeState = ref<'IDLE' | 'SUBMITTING' | 'QUEUED' | 'JUDGING' | 'FINISHED'>('IDLE')
const result = ref<{ submissionId: number; status: number; timeUsedMs?: number; memoryUsedKb?: number }>()
const currentSubmissionId = ref<number>()

const difficultyInfo = computed(() => DIFFICULTY_MAP[problem.value?.difficulty || 1] ?? { label: '未知' })
const allowedLanguages = computed(() =>
  LANGUAGE_OPTIONS.filter((l) => (problem.value?.languages || []).includes(l.value)),
)

/** 时间限制呈现：<1s 用 ms，否则转秒（1000ms → "1 s"，1500ms → "1.5 s"） */
const limitTime = computed(() => {
  const ms = problem.value?.timeLimitMs
  if (ms == null) return '—'
  return ms < 1000 ? `${ms} ms` : `${+(ms / 1000).toFixed(1)} s`
})

/** 题目维度通过率（数据随题目接口自带，纯呈现层聚合） */
const passRate = computed(() => {
  const p = problem.value
  if (!p?.submitCount) return 0
  return Math.round(((p.acceptedCount || 0) / p.submitCount) * 100)
})

/** 通过率分级配色：>=60 绿、>=30 橙、否则红 */
const rateClass = computed(() => {
  if (passRate.value >= 60) return 'rate-good'
  if (passRate.value >= 30) return 'rate-mid'
  return 'rate-low'
})

const resultSubTitle = computed(() => {
  const s = result.value
  if (!s) return ''
  const parts: string[] = []
  if (s.timeUsedMs != null) parts.push(`耗时 ${s.timeUsedMs}ms`)
  if (s.memoryUsedKb != null) parts.push(`内存 ${s.memoryUsedKb}KB`)
  return parts.join(' ｜ ')
})

/** 步骤条进度：SUBMITTING=1(提交完成) QUEUED=2 JUDGING=3 FINISHED=4 */
const stepIndex = computed(() => {
  switch (judgeState.value) {
    case 'SUBMITTING': return 1
    case 'QUEUED': return 2
    case 'JUDGING': return 3
    default: return 4
  }
})

/** 再提交一次：重置状态机与代码（保留用户编辑） */
function resetJudge() {
  judgeState.value = 'IDLE'
  result.value = undefined
  currentSubmissionId.value = undefined
}

/** 主动中止等待：停止轮询并回到可操作状态（结果仍可在提交记录里查到） */
function abortJudge() {
  stopPolling()
  judgeState.value = 'IDLE'
  result.value = undefined
  currentSubmissionId.value = undefined
}

const favorited = ref(false)
const running = ref(false)
const consoleInput = ref('')
const consoleResult = ref<RunResult>()

/** 剪贴板复制（样例输入/输出）：失败静默提示，不打断答题流程 */
async function copyText(text: string) {
  try {
    await navigator.clipboard.writeText(text)
    ElMessage.success('已复制到剪贴板')
  } catch {
    ElMessage.error('复制失败，请手动选择复制')
  }
}

/** 控制台运行：浏览器沙箱执行当前代码（仅 JS 题）。
 *  输入框为空时显式填入样例 1 输入（所见即所得）。 */
async function runConsole() {
  if (!problem.value) return
  if (!consoleInput.value.trim() && problem.value.samples?.[0]?.input) {
    consoleInput.value = problem.value.samples[0].input
  }
  running.value = true
  try {
    consoleResult.value = await runInSandbox(code.value, consoleInput.value, 5000)
  } finally {
    running.value = false
  }
}

async function loadFav() {
  if (!userStore.isLoggedIn || !problem.value) return
  try {
    favorited.value = await isFavorited(problem.value.id)
  } catch { /* 未登录等场景静默 */ }
}

async function toggleFav() {
  if (!problem.value) return
  try {
    favorited.value = await toggleFavorite(problem.value.id)
    ElMessage.success(favorited.value ? '已加入收藏' : '已取消收藏')
  } catch {
    // 未登录/会话过期返回 40100，拦截器已提示并跳转登录
  }
}

/** 切换语言后统一收尾：换模板 + 重新预填样例 1 */
function applyLanguage(lang: string) {
  code.value = CODE_TEMPLATES[lang] ?? ''
  prevLanguage.value = lang
  consoleInput.value = problem.value?.samples?.[0]?.input ?? ''
  // 换语言后上一次的控制台输出不再对应当前代码，清掉避免误读
  consoleResult.value = undefined
}

function onLanguageChange(lang: string) {
  // 切换语言载入对应模板代码。
  // 当前内容为空或仍是任一模板（未编辑）→ 直接替换；已编辑 → 确认后替换，取消则回退。
  const label = LANGUAGE_OPTIONS.find((l) => l.value === lang)?.label ?? lang
  const untouched = !code.value.trim() || Object.values(CODE_TEMPLATES).includes(code.value)
  if (untouched) {
    applyLanguage(lang)
    return
  }
  ElMessageBox.confirm(
    `切换到 ${label} 将载入对应模板，当前已编辑的代码会被替换。是否继续？`,
    '切换语言',
    { type: 'warning', confirmButtonText: '替换', cancelButtonText: '保留当前代码' },
  )
    .then(() => applyLanguage(lang))
    .catch(() => {
      // 用户取消：把下拉框拨回原来的语言（否则界面显示的语言与编辑区内容不符）
      language.value = prevLanguage.value
    })
}

async function load() {
  const id = route.params.id
  // 路由参数来自地址栏，可能不是数字；非正整数直接落到"不存在"空态，不发请求
  if (!/^\d+$/.test(String(id))) {
    problem.value = undefined
    loadError.value = true
    return
  }
  loadError.value = false
  try {
    problem.value = await getProblem(String(id))
  } catch {
    // 40002 题目不存在/未上架、40400、后端不可用：由模板渲染空态
    problem.value = undefined
    loadError.value = true
    return
  }
  if (!allowedLanguages.value.some((l) => l.value === language.value)) {
    language.value = allowedLanguages.value[0]?.value || 'cpp'
  }
  prevLanguage.value = language.value
  code.value = CODE_TEMPLATES[language.value] ?? ''
  // 控制台输入预填样例 1（进页面即可见，运行时所见即所得）
  consoleInput.value = problem.value.samples?.[0]?.input ?? ''
  consoleResult.value = undefined
  favorited.value = false
  loadFav()
}

async function handleSubmit() {
  stopPolling()
  currentSubmissionId.value = undefined
  result.value = undefined
  submitting.value = true
  judgeState.value = 'SUBMITTING'
  try {
    const data = await submitCode({
      problemId: problem.value!.id,
      language: language.value,
      code: code.value,
    })
    // 尽快赋值：QUEUED 推送若晚于本响应即可命中（早到的已在 handler 里忽略）
    currentSubmissionId.value = data.submissionId
    judgeState.value = 'QUEUED'
    // 订阅推送兜底：若 WebSocket 未送达，定时回查结果
    startPolling()
  } catch {
    // 拦截器已提示具体原因（42901/40003/40002/网络），此处只负责退出不可操作状态
    judgeState.value = 'IDLE'
    currentSubmissionId.value = undefined
  } finally {
    submitting.value = false
  }
}

// WebSocket 订阅：三阶段 QUEUED/JUDGING/FINISHED 幂等推进状态机
const unsubscribe = onJudgePush((payload) => {
  if (payload.data.submissionId !== currentSubmissionId.value || judgeState.value === 'FINISHED') return
  if (payload.type === 'JUDGE_QUEUED') {
    if (judgeState.value === 'SUBMITTING') judgeState.value = 'QUEUED'
  } else if (payload.type === 'JUDGING') {
    judgeState.value = 'JUDGING'
  } else if (payload.type === 'JUDGE_FINISHED' && payload.data.status != null) {
    judgeState.value = 'FINISHED'
    result.value = { submissionId: payload.data.submissionId, status: payload.data.status }
    stopPolling()
  }
})

// HTTP 兜底轮询：连接断开重连后以提交编号回查结果
let pollTimer: ReturnType<typeof setInterval> | null = null
let pollStartedAt = 0
let pollGeneration = 0
let pollBusy = false
/** 轮询总时长上限：判题排队久或实例掉线时不无限轮询 */
const POLL_TIMEOUT_MS = 180_000

function startPolling() {
  stopPolling()
  const generation = pollGeneration
  pollStartedAt = Date.now()
  pollTimer = setInterval(async () => {
    if (generation !== pollGeneration || pollBusy) return
    if (!currentSubmissionId.value) {
      stopPolling()
      return
    }
    if (Date.now() - pollStartedAt > POLL_TIMEOUT_MS) {
      abortJudge()
      ElMessage.warning('仍在排队或处理中，请稍后到「提交记录」中查看结果')
      return
    }
    const submissionId = currentSubmissionId.value
    pollBusy = true
    try {
      const detail = await getSubmission(submissionId)
      // 用户可能已停止等待、提交新代码，或先收到 WS 终态。
      if (generation !== pollGeneration || submissionId !== currentSubmissionId.value) return
      if (detail.status >= 2) {
        judgeState.value = 'FINISHED'
        result.value = {
          submissionId: detail.id,
          status: detail.status,
          timeUsedMs: detail.timeUsedMs ?? undefined,
          memoryUsedKb: detail.memoryUsedKb ?? undefined,
        }
        stopPolling()
      } else if (detail.status === 1) {
        judgeState.value = 'JUDGING'
      }
    } catch {
      if (generation !== pollGeneration || submissionId !== currentSubmissionId.value) return
      // 单次失败即停（40100/40400/网络抖动），避免每 2s 刷一次错误提示
      abortJudge()
    } finally {
      if (generation === pollGeneration) pollBusy = false
    }
  }, 2000)
}

function stopPolling() {
  pollGeneration++
  pollBusy = false
  if (pollTimer) {
    clearInterval(pollTimer)
    pollTimer = null
  }
}

// 同一组件在 /problems/1 → /problems/2 之间复用，只靠 onMounted 不会重新加载
watch(
  () => route.params.id,
  () => {
    stopPolling()
    resetJudge()
    load()
  },
)

onUnmounted(() => {
  stopPolling()
  unsubscribe()
  currentSubmissionId.value = undefined
})

onMounted(load)
</script>

<style scoped>
.empty-sheet {
  border-top: 3px solid var(--oj-primary);
}

.problem-detail {
  max-width: 1320px;
  margin: 0 auto;
  padding: 34px 32px 60px;
  display: grid;
  grid-template-columns: minmax(0, 1.04fr) minmax(0, 0.96fr);
  gap: 22px;
  align-items: start;
}

/* ---------- 左：题面 ---------- */
.statement {
  min-width: 0;
  background: var(--oj-card);
  border: 1px solid var(--oj-border);
  border-radius: 10px;
  padding: 24px 26px 28px;
  box-shadow: var(--oj-shadow-sm);
}
.prob-topline {
  display: flex;
  align-items: baseline;
  justify-content: space-between;
  gap: 12px;
  padding-bottom: 12px;
  border-bottom: 1px solid var(--oj-border);
}
.title-row {
  display: flex;
  align-items: flex-start;
  gap: 16px;
  margin-top: 16px;
}
.prob-title {
  margin: 0;
  flex: 1;
  min-width: 0;
  font-size: 28px;
  font-weight: 750;
  line-height: 1.35;
  letter-spacing: -0.04em;
  color: var(--oj-text);
}
.fav-btn {
  display: inline-flex;
  align-items: center;
  gap: 5px;
  flex-shrink: 0;
  margin-top: 5px;
  padding: 4px 9px;
  border: 1px solid var(--oj-border);
  border-radius: 4px;
  background: var(--oj-gray-50);
  color: var(--oj-text-sub);
  font-family: inherit;
  font-size: 10.5px;
  cursor: pointer;
  transition: color var(--oj-duration) var(--oj-ease), border-color var(--oj-duration) var(--oj-ease);
}
.fav-btn:hover,
.fav-btn.active {
  color: var(--oj-primary);
  border-color: var(--oj-info-border);
  background: var(--oj-primary-soft);
}

.prob-meta {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  margin: 18px 0 0;
  overflow: hidden;
  border: 1px solid var(--oj-border);
  border-radius: 7px;
  background: var(--oj-gray-50);
}
.prob-meta .meta-item {
  display: flex;
  flex-direction: column;
  gap: 4px;
  padding: 9px 12px;
  border-right: 1px solid var(--oj-border);
}
.prob-meta .meta-item:last-child {
  border-right: 0;
}
.prob-meta dt {
  font-family: var(--oj-font-mono);
  font-size: 8px;
  letter-spacing: 0.09em;
  color: var(--oj-text-weak);
}
.prob-meta dd {
  margin: 0;
  font-size: 11.5px;
  color: var(--oj-gray-700);
}
.prob-meta dd .meta-frac {
  margin-left: 5px;
  font-size: 8.5px;
  color: var(--oj-text-weak);
}
.prob-tags {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  gap: 5px;
  margin-top: 12px;
}
.tag-caption {
  margin-right: 2px;
  font-family: var(--oj-font-mono);
  font-size: 9px;
  letter-spacing: 0.04em;
  color: var(--oj-text-weak);
}
.tag {
  padding: 1px 7px;
  border: 1px solid var(--oj-border);
  border-radius: 3px;
  background: var(--oj-gray-50);
  font-family: var(--oj-font-mono);
  font-size: 9px;
  line-height: 18px;
  color: var(--oj-text-sub);
}
.tag-lang {
  color: var(--oj-primary);
  border-color: var(--oj-info-border);
  background: var(--oj-primary-soft);
}

.statement-section {
  margin-top: 24px;
}
.section-label {
  display: flex;
  align-items: baseline;
  justify-content: space-between;
  gap: 12px;
  padding-bottom: 8px;
  border-bottom: 1px solid var(--oj-border);
  margin-bottom: 14px;
}
.section-label h2 {
  margin: 0;
  font-size: 15px;
  font-weight: 700;
  letter-spacing: -0.02em;
}
.section-label span {
  font-family: var(--oj-font-mono);
  font-size: 8.5px;
  letter-spacing: 0.08em;
  color: var(--oj-text-weak);
}
.statement-body {
  max-width: 46em;
}

/* 样例 */
.samples {
  display: flex;
  flex-direction: column;
  gap: 12px;
}
.sample-set {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 10px;
}
.sample-item {
  border: 1px solid var(--oj-border);
  border-radius: 7px;
  background: var(--oj-gray-50);
  overflow: hidden;
  min-width: 0;
}
.sample-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 6px 11px;
  border-bottom: 1px solid var(--oj-border);
  background: var(--oj-card);
}
.sample-title {
  font-family: var(--oj-font-mono);
  font-size: 8.5px;
  letter-spacing: 0.08em;
  color: var(--oj-text-sub);
}
.copy-btn {
  border: 0;
  background: none;
  padding: 0;
  font-family: inherit;
  font-size: 10px;
  color: var(--oj-text-weak);
  cursor: pointer;
}
.copy-btn:hover {
  color: var(--oj-primary);
}
.sample-item pre {
  margin: 0;
  padding: 11px 12px;
  min-height: 50px;
  white-space: pre-wrap;
  word-break: break-word;
  font-family: var(--oj-font-mono);
  font-size: 12px;
  line-height: 1.65;
  color: var(--oj-gray-800);
}

/* ---------- 右：代码台 ---------- */
.workspace {
  position: sticky;
  top: 84px;
  min-width: 0;
  display: flex;
  flex-direction: column;
  gap: 14px;
}
.editor-card {
  border: 1px solid var(--oj-code-border);
  border-radius: 10px;
  overflow: hidden;
  background: var(--oj-code-bg);
  box-shadow: var(--oj-shadow);
}
.editor-toolbar {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 10px 12px;
  background: var(--oj-code-bg);
  border-bottom: 1px solid var(--oj-code-border);
}
.editor-label {
  display: inline-flex;
  align-items: center;
  gap: 7px;
  font-family: var(--oj-font-mono);
  font-size: 9px;
  letter-spacing: 0.1em;
  color: var(--oj-code-text-sub);
  white-space: nowrap;
}
.editor-dot {
  width: 5px;
  height: 5px;
  border-radius: 50%;
  background: #57d9a3;
}
.editor-toolbar .lang-select {
  width: 118px;
}
.editor-toolbar :deep(.el-select .el-select__wrapper),
.editor-toolbar :deep(.el-select .el-input__wrapper) {
  background: rgba(255, 255, 255, 0.06);
  box-shadow: 0 0 0 1px rgba(255, 255, 255, 0.16) inset;
  min-height: 28px;
}
.editor-toolbar :deep(.el-select .el-select__placeholder),
.editor-toolbar :deep(.el-select .el-select__selected-item),
.editor-toolbar :deep(.el-select .el-input__inner) {
  color: var(--oj-code-text);
  font-family: var(--oj-font-mono);
  font-size: 12px;
}
.editor-toolbar :deep(.el-select .el-select__suffix),
.editor-toolbar :deep(.el-select .el-input__suffix) {
  color: var(--oj-code-text-sub);
}
.editor-toolbar .toolbar-actions {
  margin-left: auto;
  display: flex;
  gap: 8px;
}
.editor-toolbar :deep(.el-button) {
  height: 28px;
  padding: 0 12px;
  font-size: 12px;
  border-radius: 5px;
}
.editor-toolbar .tool-btn {
  background: transparent;
  border-color: rgba(255, 255, 255, 0.22);
  color: var(--oj-code-text);
}
.editor-toolbar .tool-btn:hover {
  background: rgba(255, 255, 255, 0.08);
  border-color: rgba(255, 255, 255, 0.4);
  color: var(--oj-code-text);
}
.editor-toolbar .submit-btn {
  border-color: var(--oj-primary);
  background: var(--oj-primary);
  color: #fff;
}
.editor-toolbar .submit-btn:hover {
  background: var(--oj-primary-hover);
  border-color: var(--oj-primary-hover);
}
.btn-icon {
  margin-right: 4px;
}
.editor-foot {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 6px 12px 7px;
  border-top: 1px solid var(--oj-code-border);
  font-family: var(--oj-font-mono);
  font-size: 8.5px;
  letter-spacing: 0.05em;
  color: var(--oj-code-comment);
}

/* 控制台 */
.console-panel {
  border: 1px solid var(--oj-code-border);
  border-radius: 8px;
  overflow: hidden;
  background: var(--oj-code-bg);
}
.console-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 8px 12px;
  border-bottom: 1px solid var(--oj-code-border);
}
.console-title {
  font-family: var(--oj-font-mono);
  font-size: 9px;
  letter-spacing: 0.1em;
  color: var(--oj-code-text-sub);
}
.console-run {
  border: 1px solid rgba(255, 255, 255, 0.24);
  border-radius: 4px;
  background: transparent;
  color: var(--oj-code-text);
  font-family: inherit;
  font-size: 10.5px;
  padding: 2px 9px;
  cursor: pointer;
}
.console-run:hover {
  border-color: var(--oj-code-text);
}
.console-input :deep(textarea) {
  font-family: var(--oj-font-mono);
  font-size: 12px;
  background: var(--oj-code-bg-soft);
  box-shadow: none;
  color: var(--oj-code-text);
  border-radius: 0;
  border-bottom: 1px solid var(--oj-code-border);
}
.console-out {
  padding: 10px 12px;
  min-height: 52px;
  color: var(--oj-code-text);
}
.console-err {
  color: #e08a7d;
  font-size: 12px;
  margin-bottom: 6px;
  word-break: break-word;
}
.console-stdout {
  margin: 0;
  font-family: var(--oj-font-mono);
  font-size: 12px;
  white-space: pre-wrap;
  word-break: break-word;
}
.console-meta {
  margin-top: 8px;
  font-family: var(--oj-font-mono);
  font-size: 8.5px;
  letter-spacing: 0.04em;
  color: var(--oj-code-comment);
}

/* 判题状态 */
.judge-card {
  padding: 16px 18px 14px;
  background: var(--oj-card);
  border: 1px solid var(--oj-border);
  border-radius: 10px;
  box-shadow: var(--oj-shadow-sm);
}
.judge-head {
  display: flex;
  align-items: baseline;
  justify-content: space-between;
  margin-bottom: 12px;
}
.judge-title {
  font-size: 12.5px;
  font-weight: 700;
}
.judge-steps {
  padding: 4px 0 0;
}
.judge-steps :deep(.el-step__title) {
  font-size: 10.5px;
  font-weight: 500;
  letter-spacing: 0.01em;
}
.judge-steps :deep(.el-step__title.is-process) {
  color: var(--oj-primary);
  font-weight: 600;
}
.judge-steps :deep(.el-step__title.is-success) {
  color: var(--oj-success);
}
.judge-steps :deep(.el-step__title.is-wait) {
  color: var(--oj-text-weak);
}
.judge-steps :deep(.el-step__head.is-process) {
  color: var(--oj-primary);
  border-color: var(--oj-primary);
}
.judge-steps :deep(.el-step__head.is-success) {
  color: var(--oj-success);
  border-color: var(--oj-success);
}
.judge-steps :deep(.el-step__head.is-wait) {
  color: var(--oj-text-weak);
  border-color: var(--oj-border-strong);
}
.judge-steps :deep(.el-step__line) {
  background: var(--oj-border);
}
.judge-steps :deep(.el-step__icon) {
  width: 22px;
  height: 22px;
  font-size: 10px;
}

/* 未提交时的常驻说明 */
.judge-idle {
  padding-bottom: 16px;
}
.idle-flow {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 0 18px;
}
.idle-step {
  display: flex;
  gap: 10px;
  padding: 10px 0;
  border-top: 1px solid var(--oj-border);
}
.idle-no {
  padding-top: 1px;
  font-family: var(--oj-font-mono);
  font-size: 9px;
  letter-spacing: 0.04em;
  color: var(--oj-text-weak);
}
.idle-step strong {
  display: block;
  font-size: 12px;
  font-weight: 600;
  color: var(--oj-text);
}
.idle-step div > span {
  display: block;
  margin-top: 3px;
  font-size: 10.5px;
  line-height: 1.5;
  color: var(--oj-text-sub);
}
.idle-note {
  margin: 10px 0 0;
  padding-top: 11px;
  border-top: 1px solid var(--oj-border);
  font-size: 10.5px;
  line-height: 1.65;
  color: var(--oj-text-sub);
}

.verdict {
  display: flex;
  align-items: center;
  gap: 14px;
  padding: 13px 14px;
  border: 1px solid var(--oj-border);
  border-radius: 7px;
  background: var(--oj-gray-50);
}
.verdict.is-ok {
  border-left: 3px solid var(--oj-success);
  background: var(--oj-success-soft);
}
.verdict.is-bad {
  border-left: 3px solid var(--oj-danger);
  background: var(--oj-danger-soft);
}
.verdict-icon {
  width: 34px;
  height: 34px;
  flex-shrink: 0;
  border-radius: 50%;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  font-family: var(--oj-font-mono);
  font-size: 16px;
  font-weight: 700;
  border: 1px solid currentColor;
}
.verdict.is-ok .verdict-icon {
  color: var(--oj-success);
}
.verdict.is-bad .verdict-icon {
  color: var(--oj-danger);
}
.verdict-text {
  flex: 1;
  min-width: 0;
}
.verdict-label {
  font-size: 18px;
  font-weight: 750;
  line-height: 1.2;
  letter-spacing: -0.02em;
}
.verdict.is-ok .verdict-label {
  color: var(--oj-success);
}
.verdict.is-bad .verdict-label {
  color: var(--oj-danger);
}
.verdict-meta {
  margin: 3px 0 0;
  font-family: var(--oj-font-mono);
  font-size: 9.5px;
  color: var(--oj-text-sub);
}
.verdict-actions {
  display: flex;
  gap: 6px;
  flex-shrink: 0;
}
.judge-abort {
  margin-top: 9px;
  text-align: center;
}
.abort-link {
  border: 0;
  background: none;
  padding: 0;
  font-family: inherit;
  font-size: 10.5px;
  color: var(--oj-text-weak);
  cursor: pointer;
}
.abort-link:hover {
  color: var(--oj-text-sub);
}

@media (max-width: 1080px) {
  .problem-detail {
    grid-template-columns: 1fr;
    padding: 24px 20px 52px;
  }
  .workspace {
    position: static;
  }
  .sample-set {
    grid-template-columns: 1fr;
  }
}
@media (max-width: 620px) {
  .prob-meta {
    grid-template-columns: 1fr 1fr;
  }
  .prob-meta .meta-item:nth-child(2) {
    border-right: 0;
  }
  .prob-meta .meta-item:nth-child(n + 3) {
    border-top: 1px solid var(--oj-border);
  }
  .verdict {
    flex-wrap: wrap;
  }
  .verdict-actions {
    width: 100%;
    padding-left: 48px;
  }
}
</style>
