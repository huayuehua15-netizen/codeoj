<template>
  <div class="course-page">
    <header class="page-head course-head">
      <div>
        <p class="kicker">ASSIGNMENTS</p>
        <h1 class="page-head-title">班级作业</h1>
        <p class="page-head-sub">
          教师发作业后会生成邀请码。加入后按截止时间查看题目与完成情况。
        </p>
      </div>
    </header>

    <section class="join-panel" aria-labelledby="join-title">
      <div class="join-copy">
        <p class="kicker">JOIN A CLASS</p>
        <h2 id="join-title">用邀请码加入班级</h2>
        <p>向任课教师索取 4–12 位邀请码。加入后即可看到题目、截止时间与个人进度。</p>
      </div>
      <form class="join-form" @submit.prevent="onJoin">
        <label class="sr-only" for="join-code">班级邀请码</label>
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
          加入班级
        </el-button>
      </form>
    </section>

    <section class="course-section" aria-labelledby="my-courses-title">
      <div class="section-head">
        <div>
          <p class="kicker">JOINED</p>
          <h2 id="my-courses-title">已加入的班级</h2>
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
              {{ deadlineInfo(course.deadline).label }}
            </span>
            <span class="course-id mono">#{{ String(course.id).padStart(3, '0') }}</span>
          </div>

          <h3 class="course-title">
            <router-link :to="`/courses/${course.id}`">{{ course.name }}</router-link>
          </h3>
          <p class="course-desc">{{ course.description || '暂无作业说明' }}</p>

          <div class="course-meta">
            <span>教师 {{ course.teacherName || '未署名' }}</span>
            <span>{{ course.problemCount }} 题</span>
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
              {{ course.problemCount && course.solvedCount >= course.problemCount ? '回顾作业' : '进入作业' }}
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
        <h3>还没有加入任何班级</h3>
        <p>向老师要到邀请码后，在上面输入即可看到作业与进度。没有班级也可以先去题库自由练习。</p>
        <router-link to="/problems" class="empty-link">去题库练习</router-link>
      </div>
    </section>
  </div>
</template>

<script setup lang="ts">
import { onBeforeUnmount, onMounted, ref } from 'vue'
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
  max-width: 1120px;
  margin: 0 auto;
  padding: 36px 32px 64px;
}

.course-head {
  align-items: flex-end;
}

/* 加入班级：左侧说明，右侧输入，像一张真正的课程登记卡 */
.join-panel {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 28px;
  margin-top: 16px;
  padding: 16px 0;
  border-top: 1px solid var(--oj-border);
  border-bottom: 1px solid var(--oj-border);
}
.join-copy h2 {
  margin: 0;
  font-size: 17px;
  font-weight: 700;
  letter-spacing: -0.02em;
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
  margin: 0;
  font-size: 18px;
  font-weight: 700;
  letter-spacing: -0.02em;
  color: var(--oj-text);
}
.section-meta {
  margin: 0 0 1px;
  font-size: 10.5px;
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
  padding: 16px;
  background: var(--oj-card);
  border: 1px solid var(--oj-border);
  border-radius: 4px;
  transition: border-color var(--oj-duration) var(--oj-ease);
}
.course-card:hover {
  border-color: var(--oj-border-strong);
}
.course-card.is-ended {
  background: var(--oj-gray-50);
}
.card-top {
  display: flex;
  align-items: center;
  justify-content: space-between;
}
.course-status {
  font-size: 10.5px;
  font-weight: 600;
}
.status-running {
  color: var(--oj-primary);
}
.status-ended {
  color: var(--oj-text-weak);
}
.status-open {
  color: var(--oj-success);
}
.course-id {
  font-size: 9px;
  color: var(--oj-text-weak);
  letter-spacing: 0.04em;
}

.course-title {
  margin: 10px 0 0;
  font-size: 16px;
  font-weight: 700;
  letter-spacing: -0.02em;
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
  height: 2px;
  margin-top: 7px;
  background: var(--oj-gray-100);
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
  flex-shrink: 0;
  font-size: 11px;
  font-weight: 650;
  color: var(--oj-primary);
}

.empty-state {
  margin-top: 18px;
  padding: 44px 24px;
  text-align: center;
  background: var(--oj-card);
  border: 1px dashed var(--oj-border-strong);
  border-radius: 4px;
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
  .course-grid {
    grid-template-columns: 1fr;
  }
  .section-head {
    align-items: flex-start;
  }
}
</style>
