<template>
  <div class="course-page">
    <header class="page-head course-head">
      <div>
        <p class="kicker">CLASSROOM</p>
        <h1 class="page-head-title">我的课程</h1>
        <p class="page-head-sub">
          教师创建班级后会生成邀请码。加入后，这里会按截止时间跟踪每一道课程的完成情况。
        </p>
      </div>
      <dl class="course-totals" aria-label="课程完成概览">
        <div class="total-item">
          <dt>COURSES</dt>
          <dd>{{ courses.length }}</dd>
        </div>
        <div class="total-item">
          <dt>SOLVED</dt>
          <dd>{{ totalSolved }}<small>/{{ totalProblems }}</small></dd>
        </div>
        <div class="total-item">
          <dt>ON TIME</dt>
          <dd>{{ totalOnTime }}<small>题</small></dd>
        </div>
        <div class="total-item">
          <dt>PROGRESS</dt>
          <dd>{{ overallPercent }}<small>%</small></dd>
        </div>
      </dl>
    </header>

    <section class="join-panel" aria-labelledby="join-title">
      <div class="join-copy">
        <p class="kicker">JOIN A CLASS</p>
        <h2 id="join-title">用邀请码加入班级</h2>
        <p>向任课教师索取 4–12 位邀请码。加入后即可看到课程题目、截止时间与个人进度。</p>
      </div>
      <form class="join-form" @submit.prevent="onJoin">
        <label class="sr-only" for="join-code">课程邀请码</label>
        <el-input
          id="join-code"
          v-model="joinCode"
          class="join-input"
          placeholder="例如 ABCD2345"
          maxlength="12"
          clearable
          @input="normalizeJoinCode"
        >
          <template #prefix>
            <svg viewBox="0 0 24 24" width="15" height="15" fill="none" stroke="currentColor" stroke-width="1.8" stroke-linecap="round" stroke-linejoin="round" aria-hidden="true">
              <path d="M4 7h16v13H4z" /><path d="M8 7V5a4 4 0 0 1 8 0v2" /><path d="M9 13h6" />
            </svg>
          </template>
        </el-input>
        <el-button type="primary" native-type="submit" :loading="joining" :disabled="!joinCode.trim()">
          加入课程
        </el-button>
      </form>
    </section>

    <section class="course-section" aria-labelledby="my-courses-title">
      <div class="section-head">
        <div>
          <p class="kicker">ENROLLED</p>
          <h2 id="my-courses-title">已加入的课程</h2>
        </div>
        <p class="section-meta">按状态与截止时间排序 · 共 {{ courses.length }} 门</p>
      </div>

      <div v-loading="loading" class="course-grid">
        <article
          v-for="course in courses"
          :key="course.id"
          class="course-card"
          :class="{ 'is-ended': deadlineInfo(course.deadline).key === 'ended' }"
        >
          <div class="card-top">
            <span class="course-status" :class="`status-${deadlineInfo(course.deadline).key}`">
              <i aria-hidden="true" />
              {{ deadlineInfo(course.deadline).label }}
            </span>
            <span class="course-id mono">#{{ String(course.id).padStart(3, '0') }}</span>
          </div>

          <h3 class="course-title">
            <router-link :to="`/courses/${course.id}`">{{ course.name }}</router-link>
          </h3>
          <p class="course-desc">{{ course.description || '暂无课程说明' }}</p>

          <div class="course-meta">
            <span>教师 {{ course.teacherName || '未署名' }}</span>
            <span>{{ course.problemCount }} 题</span>
            <span>{{ course.memberCount }} 人</span>
          </div>

          <div class="progress-caption">
            <span>完成 <b>{{ course.solvedCount }}</b> / {{ course.problemCount }}</span>
            <span class="mono">{{ percentOf(course) }}%</span>
          </div>
          <div class="progress-track" role="progressbar" :aria-valuenow="percentOf(course)" aria-valuemin="0" aria-valuemax="100">
            <span :style="{ width: percentOf(course) + '%' }" :class="{ complete: percentOf(course) === 100 }" />
          </div>

          <div class="card-foot">
            <span class="deadline-text">{{ deadlineInfo(course.deadline).text }}</span>
            <router-link :to="`/courses/${course.id}`" class="card-link">
              {{ course.problemCount && course.solvedCount >= course.problemCount ? '回顾课程' : '进入课程' }}
              <svg viewBox="0 0 24 24" width="12" height="12" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round" aria-hidden="true">
                <path d="M5 12h14M13 6l6 6-6 6" />
              </svg>
            </router-link>
          </div>
        </article>
      </div>

      <div v-if="!loading && !courses.length" class="empty-state">
        <div class="empty-mark" aria-hidden="true">
          <svg viewBox="0 0 48 48" width="40" height="40" fill="none" stroke="currentColor" stroke-width="1.6" stroke-linecap="round" stroke-linejoin="round">
            <path d="M9 10h22a4 4 0 0 1 4 4v24H13a4 4 0 0 1-4-4V10Z" />
            <path d="M9 34a4 4 0 0 1 4-4h22" /><path d="M16 18h12M16 24h8" />
          </svg>
        </div>
        <h3>还没有加入任何课程</h3>
        <p>向老师要到邀请码后，在上面输入即可看到作业与进度。没有课程也可以先去题库自由练习。</p>
        <router-link to="/problems" class="empty-link">去题库练习</router-link>
      </div>
    </section>
  </div>
</template>

<script setup lang="ts">
import { computed, onBeforeUnmount, onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { joinCourse, listMyCourses, type StudentCourse } from '@/api/course'
import { formatDateTime, parseServerTime } from '@/utils/datetime'

const router = useRouter()
const courses = ref<StudentCourse[]>([])
const loading = ref(false)
const joining = ref(false)
const joinCode = ref('')
const nowTick = ref(Date.now())
let clock: number | undefined

const totalProblems = computed(() => courses.value.reduce((sum, c) => sum + c.problemCount, 0))
const totalSolved = computed(() => courses.value.reduce((sum, c) => sum + c.solvedCount, 0))
const totalOnTime = computed(() => courses.value.reduce((sum, c) => sum + c.onTimeSolvedCount, 0))
const overallPercent = computed(() =>
  totalProblems.value ? Math.round((totalSolved.value * 100) / totalProblems.value) : 0,
)

type DeadlineState = { key: 'running' | 'ended' | 'open'; label: string; text: string }

function deadlineInfo(value?: string | null): DeadlineState {
  const d = parseServerTime(value)
  if (!d) return { key: 'open', label: '长期开放', text: '不设截止时间' }
  const diff = d.getTime() - nowTick.value
  if (diff <= 0) {
    return { key: 'ended', label: '已截止', text: `${formatDateTime(value)} 截止` }
  }
  const days = Math.ceil(diff / 86_400_000)
  return {
    key: 'running',
    label: '进行中',
    text: days <= 1 ? `${formatDateTime(value)} 截止` : `剩余 ${days} 天 · ${formatDateTime(value)}`,
  }
}

function percentOf(course: StudentCourse) {
  if (!course.problemCount) return 0
  return Math.min(100, Math.round((course.solvedCount * 100) / course.problemCount))
}

function normalizeJoinCode(value: string) {
  joinCode.value = value.toUpperCase().replace(/[^A-Z0-9]/g, '').slice(0, 12)
}

async function load() {
  loading.value = true
  try {
    courses.value = await listMyCourses()
  } catch {
    courses.value = []
  } finally {
    loading.value = false
  }
}

async function onJoin() {
  const code = joinCode.value.trim()
  if (code.length < 4) {
    ElMessage.warning('请输入教师提供的邀请码')
    return
  }
  joining.value = true
  try {
    const course = await joinCourse(code)
    joinCode.value = ''
    ElMessage.success(`已加入《${course.name}》`)
    await load()
    router.push(`/courses/${course.id}`)
  } catch {
    // 拦截器已提示具体错误（邀请码无效等），保留输入方便修改
  } finally {
    joining.value = false
  }
}

onMounted(() => {
  load()
  // 停留页面时跨过截止时间，状态与剩余天数需要自动更新
  clock = window.setInterval(() => { nowTick.value = Date.now() }, 30_000)
})
onBeforeUnmount(() => window.clearInterval(clock))
</script>

<style scoped>
.course-page {
  max-width: 1280px;
  margin: 0 auto;
  padding: 44px 32px 64px;
}

.course-head {
  align-items: flex-end;
}
.course-totals {
  display: flex;
  margin: 0;
  padding: 0;
  flex-shrink: 0;
}
.total-item {
  padding: 0 20px;
  border-left: 1px solid var(--oj-border);
  text-align: right;
}
.total-item:last-child {
  padding-right: 0;
}
.total-item dt {
  font-family: var(--oj-font-mono);
  font-size: 8.5px;
  letter-spacing: 0.1em;
  color: var(--oj-text-weak);
}
.total-item dd {
  margin: 5px 0 0;
  font-family: var(--oj-font-mono);
  font-size: 25px;
  line-height: 1;
  font-weight: 600;
  letter-spacing: -0.04em;
  color: var(--oj-text);
}
.total-item dd small {
  margin-left: 2px;
  font-size: 11px;
  font-weight: 500;
  color: var(--oj-text-sub);
}

/* 加入班级：左侧说明，右侧输入，像一张真正的课程登记卡 */
.join-panel {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 28px;
  margin-top: 22px;
  padding: 22px 26px;
  background: var(--oj-card);
  border: 1px solid var(--oj-border);
  border-left: 3px solid var(--oj-primary);
  border-radius: var(--oj-radius);
  box-shadow: var(--oj-shadow-sm);
}
.join-copy h2 {
  margin: 5px 0 0;
  font-size: 19px;
  font-weight: 750;
  letter-spacing: -0.03em;
  color: var(--oj-text);
}
.join-copy p {
  max-width: 560px;
  margin: 7px 0 0;
  font-size: 12px;
  line-height: 1.7;
  color: var(--oj-text-sub);
}
.join-form {
  display: flex;
  align-items: center;
  gap: 10px;
  flex-shrink: 0;
}
.join-input {
  width: 220px;
}
.join-input :deep(.el-input__inner) {
  font-family: var(--oj-font-mono);
  letter-spacing: 0.12em;
  text-transform: uppercase;
}

.course-section {
  margin-top: 36px;
}
.section-head {
  display: flex;
  align-items: flex-end;
  justify-content: space-between;
  gap: 20px;
  padding-bottom: 10px;
  border-bottom: 1px solid var(--oj-border);
}
.section-head h2 {
  margin: 4px 0 0;
  font-size: 21px;
  font-weight: 750;
  letter-spacing: -0.03em;
  color: var(--oj-text);
}
.section-meta {
  margin: 0 0 2px;
  font-family: var(--oj-font-mono);
  font-size: 10px;
  color: var(--oj-text-weak);
}

.course-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(300px, 1fr));
  gap: 16px;
  margin-top: 18px;
  min-height: 120px;
}

.course-card {
  display: flex;
  flex-direction: column;
  padding: 18px 18px 16px;
  background: var(--oj-card);
  border: 1px solid var(--oj-border);
  border-radius: var(--oj-radius);
  box-shadow: var(--oj-shadow-sm);
  transition: border-color var(--oj-duration) var(--oj-ease), box-shadow var(--oj-duration) var(--oj-ease), transform var(--oj-duration) var(--oj-ease);
}
.course-card:hover {
  border-color: var(--oj-border-strong);
  box-shadow: var(--oj-shadow);
  transform: translateY(-1px);
}
.course-card.is-ended {
  background: linear-gradient(180deg, var(--oj-gray-50), var(--oj-card) 32%);
}
.card-top {
  display: flex;
  align-items: center;
  justify-content: space-between;
}
.course-status {
  display: inline-flex;
  align-items: center;
  gap: 5px;
  padding: 2px 8px;
  border-radius: var(--oj-radius-pill);
  font-size: 10px;
  font-weight: 650;
  letter-spacing: 0.02em;
}
.course-status i {
  width: 5px;
  height: 5px;
  border-radius: 50%;
}
.status-running {
  color: var(--oj-primary);
  background: var(--oj-primary-soft);
}
.status-running i { background: var(--oj-primary); }
.status-ended {
  color: var(--oj-text-sub);
  background: var(--oj-gray-100);
}
.status-ended i { background: var(--oj-gray-400); }
.status-open {
  color: var(--oj-success);
  background: var(--oj-success-soft);
}
.status-open i { background: var(--oj-success); }
.course-id {
  font-size: 9px;
  color: var(--oj-text-weak);
  letter-spacing: 0.04em;
}

.course-title {
  margin: 13px 0 0;
  font-size: 17px;
  font-weight: 750;
  letter-spacing: -0.025em;
  line-height: 1.35;
}
.course-title a {
  color: var(--oj-text);
}
.course-title a:hover {
  color: var(--oj-primary);
}
.course-desc {
  display: -webkit-box;
  overflow: hidden;
  margin: 7px 0 0;
  min-height: 34px;
  -webkit-box-orient: vertical;
  -webkit-line-clamp: 2;
  font-size: 11.5px;
  line-height: 1.6;
  color: var(--oj-text-sub);
}
.course-meta {
  display: flex;
  flex-wrap: wrap;
  gap: 4px 12px;
  margin-top: 12px;
  font-size: 10.5px;
  color: var(--oj-text-weak);
}

.progress-caption {
  display: flex;
  align-items: baseline;
  justify-content: space-between;
  margin-top: 16px;
  font-size: 11px;
  color: var(--oj-text-sub);
}
.progress-caption b {
  font-family: var(--oj-font-mono);
  font-size: 15px;
  color: var(--oj-text);
}
.progress-caption .mono {
  font-size: 10px;
  color: var(--oj-text-weak);
}
.progress-track {
  overflow: hidden;
  height: 4px;
  margin-top: 7px;
  background: var(--oj-gray-100);
  border-radius: 2px;
}
.progress-track span {
  display: block;
  height: 100%;
  background: var(--oj-primary);
  border-radius: 2px;
  transition: width 0.3s var(--oj-ease);
}
.progress-track span.complete {
  background: var(--oj-success);
}

.card-foot {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 10px;
  margin-top: 15px;
  padding-top: 12px;
  border-top: 1px solid var(--oj-gray-100);
}
.deadline-text {
  font-family: var(--oj-font-mono);
  font-size: 9.5px;
  line-height: 1.5;
  color: var(--oj-text-weak);
}
.card-link {
  display: inline-flex;
  align-items: center;
  gap: 4px;
  flex-shrink: 0;
  font-size: 11px;
  font-weight: 650;
  color: var(--oj-primary);
}
.card-link:hover {
  gap: 6px;
}
.card-link svg {
  transition: transform var(--oj-duration) var(--oj-ease);
}

.empty-state {
  margin-top: 18px;
  padding: 44px 24px;
  text-align: center;
  background: var(--oj-card);
  border: 1px dashed var(--oj-border-strong);
  border-radius: var(--oj-radius);
}
.empty-mark {
  color: var(--oj-gray-300);
}
.empty-state h3 {
  margin: 12px 0 0;
  font-size: 16px;
  font-weight: 700;
  color: var(--oj-text);
}
.empty-state p {
  max-width: 450px;
  margin: 7px auto 0;
  font-size: 12px;
  line-height: 1.8;
  color: var(--oj-text-sub);
}
.empty-link {
  display: inline-block;
  margin-top: 14px;
  font-size: 12px;
  font-weight: 650;
  color: var(--oj-primary);
}

.sr-only {
  position: absolute;
  width: 1px;
  height: 1px;
  padding: 0;
  margin: -1px;
  overflow: hidden;
  clip: rect(0, 0, 0, 0);
  white-space: nowrap;
  border: 0;
}

@media (max-width: 768px) {
  .course-page {
    padding: 28px 18px 48px;
  }
  .join-panel {
    flex-direction: column;
    align-items: stretch;
    gap: 16px;
    padding: 18px;
  }
  .join-form {
    width: 100%;
  }
  .join-input {
    flex: 1;
    width: auto;
  }
  .course-totals {
    display: none;
  }
  .course-grid {
    grid-template-columns: 1fr;
  }
  .section-head {
    align-items: flex-start;
  }
}
</style>
