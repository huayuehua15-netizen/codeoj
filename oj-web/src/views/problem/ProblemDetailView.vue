<template>
  <div class="page-container problem-detail" v-if="problem">
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
          <el-button type="primary" :loading="submitting" :disabled="!userStore.isLoggedIn" @click="handleSubmit">
            {{ userStore.isLoggedIn ? '提交' : '登录后提交' }}
          </el-button>
        </div>
        <CodeEditor v-model="code" :language="language" />

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
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed, onUnmounted, ref } from 'vue'
import { useRoute } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { isFavorited, toggleFavorite } from '@/api/favorite'
import { getProblem, type Problem } from '@/api/problem'
import { submitCode, getSubmission, type SubmissionDetail } from '@/api/submission'
import { useUserStore } from '@/stores/user'
import { connectWs, onJudgePush, disconnectWs } from '@/api/ws'
import { CODE_TEMPLATES, DIFFICULTY_MAP, LANGUAGE_OPTIONS, statusLabel, statusType } from '@/utils/status'
import MarkdownPreview from '@/components/MarkdownPreview.vue'
import CodeEditor from '@/components/CodeEditor.vue'

const route = useRoute()
const userStore = useUserStore()

const problem = ref<Problem>()
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

const favorited = ref(false)

async function loadFav() {
  if (!userStore.isLoggedIn || !problem.value) return
  try {
    favorited.value = await isFavorited(problem.value.id)
  } catch { /* 未登录等场景静默 */ }
}

async function toggleFav() {
  if (!problem.value) return
  favorited.value = await toggleFavorite(problem.value.id)
  ElMessage.success(favorited.value ? '已加入收藏' : '已取消收藏')
}

function onLanguageChange(lang: string) {
  // 论文 5.7：切换语言载入对应模板代码。
  // 当前内容为空或仍是任一模板（未编辑）→ 直接替换；已编辑 → 确认后替换，取消则回退。
  const label = LANGUAGE_OPTIONS.find((l) => l.value === lang)?.label ?? lang
  const untouched = !code.value.trim()
      || Object.values(CODE_TEMPLATES).includes(code.value)
  if (untouched) {
    code.value = CODE_TEMPLATES[lang] ?? ''
    prevLanguage.value = lang
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
    currentSubmissionId.value = data.submissionId
    judgeState.value = 'QUEUED'
    // 订阅推送兜底：若 WebSocket 未送达，定时回查结果
    startPolling()
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

function startPolling() {
  stopPolling()
  pollTimer = setInterval(async () => {
    if (!currentSubmissionId.value) return
    const detail = await getSubmission(currentSubmissionId.value)
    if (detail.status >= 2) {
      judgeState.value = 'FINISHED'
      result.value = { submissionId: detail.id, status: detail.status }
      stopPolling()
    } else if (detail.status === 1) {
      judgeState.value = 'JUDGING'
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
})

load()
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
  background: #f0f7ff;
  color: #2563eb;
  border: 1px solid #dbeafe;
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
.judge-steps {
  padding: 8px 4px 0;
}
</style>
