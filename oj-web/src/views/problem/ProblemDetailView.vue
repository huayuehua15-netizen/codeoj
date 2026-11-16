<template>
  <!-- 加载失败空态：题目不存在/已下架/后端不可用时不要白屏 -->
  <div v-if="!problem" class="page-container">
    <div class="card">
      <el-empty :description="loadError ? '题目不存在或已下架' : '题目加载中…'">
        <el-button v-if="loadError" type="primary" @click="$router.push('/problems')">返回题库</el-button>
      </el-empty>
    </div>
  </div>
  <div v-else class="page-container problem-detail">
    <div class="left">
      <div class="card">
        <h2>
          {{ problem.id }}. {{ problem.title }}
          <el-tag :color="difficultyInfo.color" effect="dark" size="small">{{ difficultyInfo.label }}</el-tag>
        </h2>
        <div class="limits">
          <span class="limit-tag">⏱ {{ problem.timeLimitMs }}ms</span>
          <span class="limit-tag">💾 {{ problem.memoryLimitMb }}MB</span>
          <span v-for="l in allowedLanguages" :key="l.value" class="limit-tag lang">{{ l.label }}</span>
          <span v-for="t in problem.tags || []" :key="t" class="limit-tag lang">{{ t }}</span>
          <el-button
            v-if="userStore.isLoggedIn"
            link :type="favorited ? 'warning' : 'primary'" size="small"
            style="margin-left: auto" @click="toggleFav"
          >
            {{ favorited ? '★ 已收藏' : '☆ 收藏' }}
          </el-button>
        </div>
        <MarkdownPreview :content="problem.description" />

        <h3>样例</h3>
        <div v-for="(s, i) in problem.samples || []" :key="i" class="sample">
          <div class="sample-item">
            <div class="sample-title">输入 {{ i + 1 }}</div>
            <pre>{{ s.input }}</pre>
          </div>
          <div class="sample-item">
            <div class="sample-title">输出 {{ i + 1 }}</div>
            <pre>{{ s.output }}</pre>
          </div>
        </div>
      </div>
    </div>

    <div class="right">
      <div class="card editor-card">
        <div class="editor-toolbar">
          <el-select v-model="language" style="width: 140px" @change="onLanguageChange">
            <el-option v-for="lang in allowedLanguages" :key="lang.value" :label="lang.label" :value="lang.value" />
          </el-select>
          <el-button v-if="language === 'js'" :disabled="running" @click="runConsole">▶ 运行</el-button>
          <el-button type="primary" :loading="submitting" :disabled="!userStore.isLoggedIn" @click="handleSubmit">
            {{ userStore.isLoggedIn ? '提交' : '登录后提交' }}
          </el-button>
        </div>
        <CodeEditor v-model="code" :language="language" />

        <!-- 控制台（JS 题目：浏览器沙箱自测运行） -->
        <div v-if="language === 'js'" class="console-panel">
          <div class="console-head">
            <span class="console-title">🖥 控制台（自测运行）</span>
            <el-button size="small" type="success" :loading="running" @click="runConsole">▶ 运行</el-button>
          </div>
          <el-input
            v-model="consoleInput"
            type="textarea"
            :rows="3"
            placeholder="自定义测试输入（留空则使用样例 1 输入）"
            class="console-input"
          />
          <div v-if="consoleResult" class="console-out" :class="{ err: !!consoleResult.error }">
            <div v-if="consoleResult.error" class="console-err">✖ {{ consoleResult.error }}</div>
            <pre class="console-stdout">{{ consoleResult.stdout || '(无输出)' }}</pre>
            <div class="console-meta">耗时 {{ consoleResult.elapsedMs }}ms ｜ 浏览器沙箱运行（仅 JS）</div>
          </div>
        </div>

        <!-- 判题状态机（论文 5.7：IDLE→SUBMITTING→QUEUED→JUDGING→FINISHED）步骤条呈现 -->
        <div v-if="judgeState !== 'IDLE'" class="judge-status">
          <el-steps
            v-if="judgeState !== 'FINISHED'"
            :active="stepIndex"
            align-center
            class="judge-steps"
            finish-status="success"
            :process-status="judgeState === 'JUDGING' ? 'process' : 'wait'"
          >
            <el-step title="提交代码" />
            <el-step title="排队中" />
            <el-step title="判题中" />
            <el-step title="出结果" />
          </el-steps>
          <el-result
            v-else-if="result"
            :icon="resultIcon"
            :title="statusLabel(result.status)"
            :sub-title="resultSubTitle"
          >
            <template #extra>
              <el-button type="primary" @click="$router.push(`/submissions/${result.submissionId}`)">
                查看判题详情
              </el-button>
              <el-button @click="resetJudge">再提交一次</el-button>
            </template>
          </el-result>
          <!-- 中止出口：排队/判题异常耗时过久时，允许用户主动退出并去看记录 -->
          <div v-if="judgeState === 'QUEUED' || judgeState === 'JUDGING'" class="judge-abort">
            <el-button link type="info" size="small" @click="abortJudge">
              暂时不看结果（可稍后在提交记录中查看）
            </el-button>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, onUnmounted, ref } from 'vue'
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

const difficultyInfo = computed(() => DIFFICULTY_MAP[problem.value?.difficulty || 1])
const allowedLanguages = computed(() =>
  LANGUAGE_OPTIONS.filter((l) => (problem.value?.languages || []).includes(l.value)),
)

const resultIcon = computed(() => {
  const s = result.value?.status
  if (s === 2) return 'success'
  return 'error'
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

/** 控制台运行：浏览器沙箱执行当前代码（仅 JS 题）。
 *  输入框为空时显式填入样例 1 输入（所见即所得——用户能看到运行用了什么输入）。 */
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

function onLanguageChange(lang: string) {
  // 论文 5.7：切换语言载入对应模板代码。
  // 当前内容为空或仍是任一模板（未编辑）→ 直接替换；已编辑 → 确认后替换，取消则回退。
  const label = LANGUAGE_OPTIONS.find((l) => l.value === lang)?.label ?? lang
  const untouched = !code.value.trim() || Object.values(CODE_TEMPLATES).includes(code.value)
  if (untouched) {
    code.value = CODE_TEMPLATES[lang] ?? ''
    prevLanguage.value = lang
    consoleInput.value = problem.value?.samples?.[0]?.input ?? ''
    return
  }
  ElMessageBox.confirm(
    `切换到 ${label} 将载入对应模板，当前已编辑的代码会被替换。是否继续？`,
    '切换语言',
    { type: 'warning', confirmButtonText: '替换', cancelButtonText: '保留当前代码' },
  )
    .then(() => {
      code.value = CODE_TEMPLATES[lang] ?? ''
      prevLanguage.value = lang
      // 与未编辑分支保持一致：切换语言后重新预填样例 1
      consoleInput.value = problem.value?.samples?.[0]?.input ?? ''
    })
    .catch(() => {
      language.value = prevLanguage.value
    })
}

async function load() {
  problem.value = await getProblem(route.params.id as string)
  if (!allowedLanguages.value.some((l) => l.value === language.value)) {
    language.value = allowedLanguages.value[0]?.value || 'cpp'
  }
  prevLanguage.value = language.value
  code.value = CODE_TEMPLATES[language.value] ?? ''
  // 控制台输入预填样例 1（进页面即可见，运行时所见即所得）
  consoleInput.value = problem.value.samples?.[0]?.input ?? ''
  loadFav()
}

async function handleSubmit() {
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

// WebSocket 订阅（论文 5.5）：三阶段 QUEUED/JUDGING/FINISHED 幂等推进状态机
const unsubscribe = onJudgePush((payload) => {
  if (payload.data.submissionId !== currentSubmissionId.value) return
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

// HTTP 兜底轮询：连接断开重连后以提交编号回查结果（论文 5.5）
let pollTimer: ReturnType<typeof setInterval> | null = null
let pollStartedAt = 0
/** 轮询总时长上限：判题排队久或实例掉线时不无限轮询 */
const POLL_TIMEOUT_MS = 60_000

function startPolling() {
  stopPolling()
  pollStartedAt = Date.now()
  pollTimer = setInterval(async () => {
    if (!currentSubmissionId.value) {
      stopPolling()
      return
    }
    if (Date.now() - pollStartedAt > POLL_TIMEOUT_MS) {
      stopPolling()
      judgeState.value = 'IDLE'
      ElMessage.warning('判题超时，请稍后到「提交记录」中查看结果')
      return
    }
    try {
      const detail = await getSubmission(currentSubmissionId.value)
      if (detail.status >= 2) {
        judgeState.value = 'FINISHED'
        result.value = { submissionId: detail.id, status: detail.status }
        stopPolling()
      } else if (detail.status === 1) {
        judgeState.value = 'JUDGING'
      }
    } catch {
      // 单次失败即停（40100/40400/网络抖动），避免每 2s 刷一次错误提示
      stopPolling()
      judgeState.value = 'IDLE'
    }
  }, 2000)
}

function stopPolling() {
  if (pollTimer) {
    clearInterval(pollTimer)
    pollTimer = null
  }
}

onUnmounted(() => {
  stopPolling()
  unsubscribe()
  currentSubmissionId.value = undefined
})

onMounted(async () => {
  try {
    await load()
  } catch {
    // 40002 题目不存在/未上架、40400、后端不可用：保持 problem 为空，由模板渲染空态
    loadError.value = true
  }
})
</script>

<style scoped>
.problem-detail {
  display: flex;
  gap: 16px;
  align-items: flex-start;
}
.left {
  flex: 1;
}
.right {
  flex: 1;
}
.limits {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  margin-bottom: 14px;
}
.limit-tag {
  background: #eef2ff;
  color: #4f46e5;
  border: 1px solid #e0e7ff;
  border-radius: 999px;
  padding: 2px 12px;
  font-size: 12px;
}
.limit-tag.lang {
  background: #f4f4f5;
  color: #606266;
  border-color: #e4e7ed;
}
.sample {
  display: flex;
  gap: 12px;
  margin-bottom: 12px;
}
.sample-item {
  flex: 1;
  background: #f5f7fa;
  border-radius: 6px;
  padding: 10px;
}
.sample-title {
  font-size: 12px;
  color: #909399;
  margin-bottom: 6px;
}
.sample-item pre {
  margin: 0;
  white-space: pre-wrap;
  font-family: Consolas, Monaco, monospace;
  font-size: 13px;
}
.editor-toolbar {
  display: flex;
  justify-content: space-between;
  margin-bottom: 10px;
}
.judge-status {
  margin-top: 12px;
}
.console-panel {
  margin-top: 14px;
  border: 1px solid var(--oj-border);
  border-radius: 10px;
  overflow: hidden;
}
.console-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  background: #0f172a;
  color: #e2e8f0;
  padding: 8px 14px;
}
.console-title {
  font-size: 13px;
  font-weight: 600;
  font-family: 'JetBrains Mono', Consolas, monospace;
}
.console-input :deep(textarea) {
  font-family: 'JetBrains Mono', Consolas, monospace;
  font-size: 13px;
}
.console-out {
  background: #0f172a;
  color: #e2e8f0;
  padding: 10px 14px;
  min-height: 60px;
}
.console-err {
  color: #f87171;
  font-size: 13px;
  margin-bottom: 6px;
}
.console-stdout {
  margin: 0;
  font-family: 'JetBrains Mono', Consolas, monospace;
  font-size: 13px;
  white-space: pre-wrap;
  word-break: break-all;
}
.console-meta {
  color: #64748b;
  font-size: 11px;
  margin-top: 8px;
}
.judge-steps {
  padding: 8px 4px 0;
}
.judge-abort {
  text-align: center;
  padding-bottom: 4px;
}
</style>
