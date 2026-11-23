<template>
  <div class="course-detail-page">
    <router-link to="/courses" class="back-link">
      <svg viewBox="0 0 24 24" width="13" height="13" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round" aria-hidden="true">
        <path d="M19 12H5M11 6l-6 6 6 6" />
      </svg>
      班级作业
    </router-link>

    <div v-loading="loading" class="detail-shell">
      <template v-if="course">
        <header class="course-head">
          <div class="head-main">
            <div class="head-kicker">
              <span class="kicker">ASSIGNMENT</span>
              <span class="course-no mono">#{{ String(course.id).padStart(3, '0') }}</span>
            </div>
            <h1 class="course-title">{{ course.name }}</h1>
            <p v-if="course.description" class="course-desc">{{ course.description }}</p>
            <div class="head-meta">
              <span>教师 {{ course.teacherName || '未署名' }}</span>
              <span v-if="course.joinedAt">加入于 {{ formatDateTime(course.joinedAt) }}</span>
            </div>
          </div>

          <aside class="deadline-card" :class="`deadline-${deadlineState.key}`">
            <p class="deadline-label">{{ deadlineState.label }}</p>
            <p class="deadline-value">{{ deadlineState.value }}</p>
            <p class="deadline-hint">{{ deadlineState.hint }}</p>
          </aside>
        </header>

        <section class="progress-strip" aria-label="作业完成情况">
          <div class="progress-main">
            <div class="progress-caption">
              <span>完成进度</span>
              <span class="mono">{{ course.solvedCount }} / {{ course.problemCount }}</span>
            </div>
            <div class="progress-track" role="progressbar" :aria-valuenow="progressPercent" aria-valuemin="0" aria-valuemax="100">
              <span :style="{ width: progressPercent + '%' }" :class="{ complete: progressPercent === 100 }" />
            </div>
            <p class="progress-note">{{ progressNote }}</p>
          </div>
          <dl class="progress-stats">
            <div>
              <dt>SOLVED</dt>
              <dd>{{ course.solvedCount }}<small>/{{ course.problemCount }}</small></dd>
            </div>
            <div>
              <dt>ON TIME</dt>
              <dd>{{ course.onTimeSolvedCount }}<small>题</small></dd>
            </div>
            <div>
              <dt>SUBMITS</dt>
              <dd>{{ course.submitCount }}<small>次</small></dd>
            </div>
            <div>
              <dt>LAST ACTIVE</dt>
              <dd class="small">{{ course.lastSubmitAt ? relativeTime(course.lastSubmitAt) : '暂无' }}</dd>
            </div>
          </dl>
        </section>

        <section class="problem-panel" aria-labelledby="problem-set-title">
          <header class="panel-head">
            <div>
              <p class="kicker">PROBLEM SET</p>
              <h2 id="problem-set-title">作业题目</h2>
              <p class="panel-sub">按教师安排的顺序完成；截止后的通过会标记为迟交，但仍保留记录。</p>
            </div>
            <el-button type="primary" :disabled="!course.problems.length" @click="goNext">
              {{ nextLabel }}
              <svg viewBox="0 0 24 24" width="13" height="13" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round" aria-hidden="true">
                <path d="M5 12h14M13 6l6 6-6 6" />
              </svg>
            </el-button>
          </header>

          <div class="problem-list">
            <article
              v-for="(problem, index) in course.problems"
              :key="problem.problemId"
              class="problem-row"
              :class="`row-${problem.status}`"
            >
              <span class="row-index mono">{{ String(index + 1).padStart(2, '0') }}</span>
              <div class="row-main">
                <router-link :to="`/problems/${problem.problemId}?course=${course.id}`" class="row-title">
                  {{ problem.title }}
                </router-link>
                <div class="row-tags">
                  <span class="difficulty" :class="`diff-${problem.difficulty ?? 0}`">
                    {{ difficultyLabel(problem.difficulty) }}
                  </span>
                  <span v-for="tag in (problem.tags || []).slice(0, 2)" :key="tag" class="tag">{{ tag }}</span>
                </div>
              </div>

              <div class="row-status">
                <span class="status-badge" :class="`badge-${problem.status}`">
                  <i aria-hidden="true" />
                  {{ statusLabel(problem.status) }}
                </span>
                <span v-if="problem.status === 'ac' && problem.onTime === false" class="late-tag">迟交</span>
              </div>

              <div class="row-detail">
                <template v-if="problem.status === 'ac'">
                  <span>首次通过 {{ formatDateTime(problem.firstAcAt) }}</span>
                  <span class="mono">{{ problem.attempts }} 次提交</span>
                </template>
                <template v-else-if="problem.status === 'attempted'">
                  <span>已提交 {{ problem.attempts }} 次 · 尚未通过</span>
                  <span class="mono">最后 {{ relativeTime(problem.lastAt) }}</span>
                </template>
                <template v-else>
                  <span>尚未提交</span>
                </template>
              </div>

              <router-link :to="`/problems/${problem.problemId}?course=${course.id}`" class="row-action">
                {{ actionLabel(problem.status) }}
              </router-link>
            </article>
          </div>

          <div v-if="!course.problems.length" class="empty-problems">
            教师还没有为这次作业添加题目，稍后再来看看。
          </div>
        </section>

        <footer class="course-foot">
          <p>作业进度只统计加入班级后的提交；退出后重新加入会从新的加入时间重新计算。</p>
          <button type="button" class="leave-btn" @click="onLeave">退出班级</button>
        </footer>
      </template>

      <div v-else-if="loadError" class="error-panel">
        <div class="error-mark" aria-hidden="true">
          <svg viewBox="0 0 40 40" width="34" height="34" fill="none" stroke="currentColor" stroke-width="1.6" stroke-linecap="round">
            <circle cx="20" cy="20" r="14" /><path d="M20 12v9M20 26h.01" />
          </svg>
        </div>
        <h2>{{ forbidden ? '你还不是这个班级的成员' : '作业打不开' }}</h2>
        <p>
          {{
            forbidden
              ? '请向任课教师索取邀请码，加入后即可查看作业题目与进度。'
              : loadError
          }}
        </p>
        <form v-if="forbidden" class="error-join" @submit.prevent="onJoin">
          <el-input v-model="joinCode" placeholder="输入班级邀请码" maxlength="12" @input="normalizeCode" />
          <el-button type="primary" native-type="submit" :loading="joining">加入班级</el-button>
        </form>
        <router-link to="/courses" class="error-back">返回班级作业</router-link>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed, onBeforeUnmount, onMounted, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { getCourseDetail, joinCourse, leaveCourse, type CourseDetail, type CourseProblemStatus } from '@/api/course'
import { DIFFICULTY_MAP } from '@/utils/status'
import { formatDateTime, parseServerTime, relativeTime } from '@/utils/datetime'

const route = useRoute()
const router = useRouter()
const course = ref<CourseDetail | null>(null)
const loading = ref(false)
const loadError = ref('')
const forbidden = ref(false)
const joining = ref(false)
const joinCode = ref('')
const nowTick = ref(Date.now())

let clock: number | undefined
onMounted(() => {
  clock = window.setInterval(() => { nowTick.value = Date.now() }, 30_000)
  load()
})
onBeforeUnmount(() => window.clearInterval(clock))
// 同一组件被复用到另一门课程时（浏览器前进/后退或直接改 URL），要重新取详情
watch(() => route.params.id, () => load())

const progressPercent = computed(() => {
  const c = course.value
  if (!c || !c.problemCount) return 0
  return Math.min(100, Math.round((c.solvedCount * 100) / c.problemCount))
})

const progressNote = computed(() => {
  const c = course.value
  if (!c || !c.problemCount) return '等待教师添加题目'
  if (c.solvedCount >= c.problemCount) {
    return c.deadline && c.onTimeSolvedCount < c.problemCount
      ? '题目已全部完成，但有迟交题目'
      : '全部完成，可以回顾错题或帮助同学'
  }
  return `还剩 ${c.problemCount - c.solvedCount} 题，继续保持`
})

const deadlineState = computed(() => {
  const c = course.value
  const d = parseServerTime(c?.deadline)
  if (!d) {
    return { key: 'open', label: '长期开放', value: '不限时', hint: '按自己的节奏完成作业题目' }
  }
  const diff = d.getTime() - nowTick.value
  if (diff <= 0) {
    return {
      key: 'ended',
      label: '已截止',
      value: formatDateTime(c?.deadline),
      hint: '迟交仍可提交，但不计入「按时完成」',
    }
  }
  const hours = Math.floor(diff / 3_600_000)
  const days = Math.floor(hours / 24)
  const minutes = Math.max(1, Math.floor(diff / 60_000))
  const remain = days >= 1 ? `${days} 天 ${hours % 24} 小时` : hours >= 1 ? `${hours} 小时` : `${minutes} 分钟`
  return {
    key: hours <= 24 ? 'urgent' : 'running',
    label: hours <= 24 ? '即将截止' : '进行中',
    value: formatDateTime(c?.deadline),
    hint: `剩余 ${remain}`,
  }
})

const nextProblem = computed(() =>
  course.value?.problems.find((p) => p.status !== 'ac') ?? null,
)
const nextLabel = computed(() => {
  const c = course.value
  if (!c?.problems.length) return '暂无题目'
  if (!nextProblem.value) return '全部完成 · 回顾第一题'
  const index = c.problems.findIndex((p) => p.problemId === nextProblem.value?.problemId)
  return index === 0 ? '开始第 1 题' : `继续第 ${index + 1} 题`
})

function difficultyLabel(value?: number | null) {
  if (value == null) return '未分级'
  return DIFFICULTY_MAP[value]?.label ?? '未分级'
}

function statusLabel(status: CourseProblemStatus) {
  return status === 'ac' ? '已通过' : status === 'attempted' ? '尝试过' : '未开始'
}

function actionLabel(status: CourseProblemStatus) {
  return status === 'ac' ? '再做一次' : status === 'attempted' ? '继续尝试' : '开始做题'
}

function goNext() {
  const c = course.value
  if (!c?.problems.length) return
  const target = nextProblem.value ?? c.problems[0]
  router.push(`/problems/${target.problemId}?course=${c.id}`)
}

function normalizeCode(value: string) {
  joinCode.value = value.toUpperCase().replace(/[^A-Z0-9]/g, '').slice(0, 12)
}

async function load() {
  loading.value = true
  loadError.value = ''
  forbidden.value = false
  try {
    course.value = await getCourseDetail(String(route.params.id))
  } catch (error) {
    course.value = null
    const message = error instanceof Error ? error.message : '作业加载失败'
    loadError.value = message
    forbidden.value = message.includes('无权限')
  } finally {
    loading.value = false
  }
}

async function onJoin() {
  if (joinCode.value.trim().length < 4) {
    ElMessage.warning('请输入完整邀请码')
    return
  }
  joining.value = true
  try {
    await joinCourse(joinCode.value.trim())
    ElMessage.success('加入成功')
    joinCode.value = ''
    await load()
  } catch {
    // 拦截器已提示
  } finally {
    joining.value = false
  }
}

async function onLeave() {
  try {
    await ElMessageBox.confirm(
      '退出后，这个班级将不再出现在「班级作业」；重新加入会从新的加入时间重新计算进度。',
      '退出班级',
      { type: 'warning', confirmButtonText: '确认退出', cancelButtonText: '再想想' },
    )
  } catch {
    return
  }
  try {
    await leaveCourse(String(route.params.id))
    ElMessage.success('已退出班级')
    router.push('/courses')
  } catch {
    // 拦截器已提示
  }
}
</script>

<style scoped>
.course-detail-page {
  max-width: 1180px;
  margin: 0 auto;
  padding: 34px 32px 64px;
}
.back-link {
  display: inline-flex;
  align-items: center;
  gap: 5px;
  margin-bottom: 18px;
  font-size: 11.5px;
  color: var(--oj-text-sub);
}
.back-link:hover {
  color: var(--oj-primary);
}
.detail-shell {
  min-height: 420px;
}

.course-head {
  display: flex;
  align-items: stretch;
  justify-content: space-between;
  gap: 28px;
  padding-bottom: 20px;
  border-bottom: 1px solid var(--oj-border);
}
.head-main {
  min-width: 0;
}
.head-kicker {
  display: flex;
  align-items: center;
  gap: 9px;
}
.course-no {
  font-size: 9px;
  color: var(--oj-text-weak);
  letter-spacing: 0.04em;
}
.course-title {
  margin: 7px 0 0;
  font-size: 30px;
  font-weight: 780;
  letter-spacing: -0.045em;
  line-height: 1.2;
  color: var(--oj-text);
}
.course-desc {
  max-width: 680px;
  margin: 9px 0 0;
  font-size: 13px;
  line-height: 1.75;
  color: var(--oj-text-sub);
}
.head-meta {
  display: flex;
  flex-wrap: wrap;
  gap: 5px 16px;
  margin-top: 12px;
  font-size: 11px;
  color: var(--oj-text-weak);
}

.deadline-card {
  width: 220px;
  flex-shrink: 0;
  padding: 14px 16px;
  border: 1px solid var(--oj-border);
  border-radius: var(--oj-radius);
  background: var(--oj-card);
}
.deadline-card.deadline-running {
  border-color: var(--oj-info-border);
  background: var(--oj-primary-soft);
}
.deadline-card.deadline-urgent {
  border-color: var(--oj-warning-border);
  background: var(--oj-warning-soft);
}
.deadline-card.deadline-ended {
  background: var(--oj-gray-50);
}
.deadline-label {
  margin: 0;
  font-family: var(--oj-font-mono);
  font-size: 9px;
  letter-spacing: 0.1em;
  color: var(--oj-text-sub);
}
.deadline-value {
  margin: 6px 0 0;
  font-family: var(--oj-font-mono);
  font-size: 13px;
  font-weight: 600;
  color: var(--oj-text);
}
.deadline-hint {
  margin: 5px 0 0;
  font-size: 10.5px;
  line-height: 1.6;
  color: var(--oj-text-sub);
}

.progress-strip {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 32px;
  margin-top: 20px;
  padding: 20px 24px;
  background: var(--oj-card);
  border: 1px solid var(--oj-border);
  border-radius: var(--oj-radius);
  box-shadow: var(--oj-shadow-sm);
}
.progress-main {
  flex: 1;
  min-width: 0;
  max-width: 480px;
}
.progress-caption {
  display: flex;
  justify-content: space-between;
  font-size: 12px;
  color: var(--oj-text-sub);
}
.progress-caption .mono {
  font-size: 11px;
  color: var(--oj-text);
}
.progress-track {
  overflow: hidden;
  height: 6px;
  margin-top: 9px;
  background: var(--oj-gray-100);
  border-radius: 3px;
}
.progress-track span {
  display: block;
  height: 100%;
  background: var(--oj-primary);
  border-radius: 3px;
  transition: width 0.3s var(--oj-ease);
}
.progress-track span.complete {
  background: var(--oj-success);
}
.progress-note {
  margin: 8px 0 0;
  font-size: 10.5px;
  color: var(--oj-text-weak);
}
.progress-stats {
  display: flex;
  margin: 0;
  padding: 0;
  flex-shrink: 0;
}
.progress-stats > div {
  min-width: 78px;
  padding: 0 18px;
  border-left: 1px solid var(--oj-border);
  text-align: right;
}
.progress-stats > div:last-child {
  padding-right: 0;
}
.progress-stats dt {
  font-family: var(--oj-font-mono);
  font-size: 8px;
  letter-spacing: 0.08em;
  color: var(--oj-text-weak);
}
.progress-stats dd {
  margin: 5px 0 0;
  font-family: var(--oj-font-mono);
  font-size: 21px;
  line-height: 1;
  font-weight: 600;
  color: var(--oj-text);
}
.progress-stats dd small {
  margin-left: 1px;
  font-family: var(--oj-font-sans);
  font-size: 9px;
  font-weight: 500;
  color: var(--oj-text-sub);
}
.progress-stats dd.small {
  font-size: 11px;
  line-height: 1.4;
  font-weight: 500;
  color: var(--oj-text-sub);
}

.problem-panel {
  margin-top: 24px;
  background: var(--oj-card);
  border: 1px solid var(--oj-border);
  border-radius: var(--oj-radius);
  box-shadow: var(--oj-shadow-sm);
}
.panel-head {
  display: flex;
  align-items: flex-end;
  justify-content: space-between;
  gap: 20px;
  padding: 18px 20px 14px;
  border-bottom: 1px solid var(--oj-border);
}
.panel-head h2 {
  margin: 4px 0 0;
  font-size: 19px;
  font-weight: 750;
  letter-spacing: -0.03em;
}
.panel-sub {
  margin: 5px 0 0;
  font-size: 11px;
  color: var(--oj-text-sub);
}
.problem-list {
  display: flex;
  flex-direction: column;
}
.problem-row {
  display: grid;
  grid-template-columns: 38px minmax(0, 1.6fr) 118px minmax(0, 1fr) 76px;
  align-items: center;
  gap: 14px;
  padding: 14px 20px;
  border-bottom: 1px solid var(--oj-gray-100);
  transition: background var(--oj-duration) var(--oj-ease);
}
.problem-row:last-child {
  border-bottom: 0;
}
.problem-row:hover {
  background: var(--oj-gray-50);
}
.row-index {
  font-size: 10px;
  color: var(--oj-text-weak);
}
.row-main {
  min-width: 0;
}
.row-title {
  display: inline-block;
  max-width: 100%;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  font-size: 13px;
  font-weight: 650;
  color: var(--oj-text);
}
.row-title:hover {
  color: var(--oj-primary);
}
.row-tags {
  display: flex;
  flex-wrap: wrap;
  gap: 5px;
  margin-top: 4px;
}
.difficulty,
.tag {
  padding: 1px 6px;
  border-radius: 3px;
  font-size: 9px;
  line-height: 1.7;
  color: var(--oj-text-sub);
  background: var(--oj-gray-100);
}
.diff-1 { color: var(--oj-success); background: var(--oj-success-soft); }
.diff-2 { color: var(--oj-warning); background: var(--oj-warning-soft); }
.diff-3 { color: var(--oj-danger); background: var(--oj-danger-soft); }
.tag {
  color: var(--oj-text-weak);
  background: transparent;
  border: 1px solid var(--oj-border);
}

.row-status {
  display: flex;
  align-items: center;
  gap: 6px;
}
.status-badge {
  display: inline-flex;
  align-items: center;
  gap: 4px;
  font-size: 10.5px;
  font-weight: 600;
}
.status-badge i {
  width: 6px;
  height: 6px;
  border-radius: 50%;
}
.badge-ac { color: var(--oj-success); }
.badge-ac i { background: var(--oj-success); }
.badge-attempted { color: var(--oj-warning); }
.badge-attempted i { background: var(--oj-warning); }
.badge-none { color: var(--oj-text-weak); }
.badge-none i { background: var(--oj-gray-300); }
.late-tag {
  padding: 1px 5px;
  border-radius: 3px;
  font-size: 9px;
  color: var(--oj-danger);
  background: var(--oj-danger-soft);
}

.row-detail {
  display: flex;
  flex-direction: column;
  gap: 2px;
  min-width: 0;
  font-size: 10px;
  color: var(--oj-text-sub);
}
.row-detail .mono {
  font-size: 9px;
  color: var(--oj-text-weak);
}
.row-action {
  justify-self: end;
  font-size: 11px;
  font-weight: 600;
  color: var(--oj-primary);
  white-space: nowrap;
}
.row-action:hover {
  text-decoration: underline;
}

.empty-problems {
  padding: 36px 20px;
  text-align: center;
  font-size: 12px;
  color: var(--oj-text-weak);
}

.course-foot {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 20px;
  margin-top: 18px;
  font-size: 10px;
  line-height: 1.6;
  color: var(--oj-text-weak);
}
.course-foot p {
  margin: 0;
}
.leave-btn {
  flex-shrink: 0;
  padding: 0;
  border: 0;
  background: none;
  font-size: 10.5px;
  color: var(--oj-text-sub);
  cursor: pointer;
}
.leave-btn:hover {
  color: var(--oj-danger);
}

.error-panel {
  padding: 64px 24px;
  text-align: center;
  background: var(--oj-card);
  border: 1px solid var(--oj-border);
  border-radius: var(--oj-radius);
}
.error-mark {
  color: var(--oj-gray-300);
}
.error-panel h2 {
  margin: 12px 0 0;
  font-size: 19px;
  font-weight: 750;
  color: var(--oj-text);
}
.error-panel p {
  max-width: 420px;
  margin: 8px auto 0;
  font-size: 12px;
  line-height: 1.8;
  color: var(--oj-text-sub);
}
.error-join {
  display: flex;
  justify-content: center;
  gap: 8px;
  max-width: 360px;
  margin: 18px auto 0;
}
.error-back {
  display: inline-block;
  margin-top: 16px;
  font-size: 11px;
  font-weight: 600;
  color: var(--oj-primary);
}

@media (max-width: 768px) {
  .course-detail-page {
    padding: 24px 18px 48px;
  }
  .course-head {
    flex-direction: column;
    gap: 16px;
  }
  .course-title {
    font-size: 24px;
  }
  .deadline-card {
    width: auto;
  }
  .progress-strip {
    flex-direction: column;
    align-items: stretch;
    gap: 18px;
    padding: 16px;
  }
  .progress-main {
    max-width: none;
  }
  .progress-stats {
    justify-content: space-between;
  }
  .progress-stats > div {
    min-width: 0;
    padding: 0 12px;
    text-align: left;
  }
  .progress-stats > div:first-child {
    padding-left: 0;
  }
  .panel-head {
    flex-direction: column;
    align-items: stretch;
  }
  .problem-row {
    grid-template-columns: 26px minmax(0, 1fr) auto;
    gap: 8px 10px;
    padding: 13px 16px;
  }
  .row-index {
    grid-row: 1 / span 2;
  }
  .row-status {
    grid-column: 3;
    grid-row: 1;
    justify-self: end;
  }
  .row-detail {
    grid-column: 2;
    grid-row: 2;
  }
  .row-action {
    grid-column: 3;
    grid-row: 2;
  }
  .course-foot {
    flex-direction: column;
    align-items: flex-start;
  }
}
</style>
