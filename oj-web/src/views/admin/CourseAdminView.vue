<template>
  <AdminShell
    title="课程班级"
    section="COURSES · 班级与作业"
    description="创建班级、编排题组、设置截止时间；学生凭邀请码加入后，可在这里查看每个人的完成情况。"
  >
    <template #actions>
      <el-button type="primary" @click="openCreate">新建课程</el-button>
    </template>

    <div class="admin-table-shell">
      <el-table :data="courses" v-loading="loading" empty-text="还没有课程，点右上角「新建课程」开始">
        <el-table-column prop="id" label="ID" width="64">
          <template #default="{ row }">
            <span class="mono id-cell">{{ String(row.id).padStart(3, '0') }}</span>
          </template>
        </el-table-column>
        <el-table-column label="课程" min-width="240">
          <template #default="{ row }">
            <div class="course-cell">
              <span class="course-name">{{ row.name }}</span>
              <span class="course-desc">{{ row.description || '暂无描述' }}</span>
            </div>
          </template>
        </el-table-column>
        <el-table-column label="班级 / 题量" width="120">
          <template #default="{ row }">
            <span class="mono count-cell">{{ row.memberCount }} 人</span>
            <span class="count-sub">{{ row.problemCount }} 题</span>
          </template>
        </el-table-column>
        <el-table-column label="邀请码" width="168">
          <template #default="{ row }">
            <div v-if="row.joinCode" class="code-cell">
              <button type="button" class="code-text mono" title="点击复制邀请码" @click="copyCode(row.joinCode)">
                {{ row.joinCode }}
              </button>
              <button type="button" class="code-action" title="轮换邀请码" @click="rotateCode(row)">轮换</button>
            </div>
            <el-button v-else link type="primary" @click="rotateCode(row)">生成邀请码</el-button>
          </template>
        </el-table-column>
        <el-table-column label="截止时间" width="176">
          <template #default="{ row }">
            <span class="mono deadline-cell" :class="{ 'is-overdue': isOverdue(row.deadline) }">
              {{ row.deadline ? formatDateTime(row.deadline) : '不限时' }}
            </span>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="260" fixed="right">
          <template #default="{ row }">
            <el-button link type="primary" @click="openRoster(row)">学情</el-button>
            <el-button link type="primary" @click="showCompletion(row)">完成率</el-button>
            <el-button link type="primary" @click="openEdit(row)">编辑</el-button>
            <el-button link type="danger" @click="remove(row)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>
    </div>

    <p class="admin-hint">
      学生加入后才会计入班级统计；课程进度只统计加入班级之后的提交，历史 AC 不会直接算作作业完成。
    </p>

    <!-- 课程表单：名称/描述 + 题组 + 邀请码 + 截止时间 -->
    <el-dialog
      v-model="dialogVisible"
      :title="editingId ? '编辑课程' : '新建课程'"
      width="min(600px, 94vw)"
      destroy-on-close
    >
      <el-form :model="form" label-width="88px">
        <el-form-item label="课程名称">
          <el-input v-model="form.name" maxlength="100" placeholder="例如：数据结构 · 第 3 周作业" />
        </el-form-item>
        <el-form-item label="课程说明">
          <el-input
            v-model="form.description"
            type="textarea"
            :rows="3"
            maxlength="500"
            show-word-limit
            placeholder="告诉学生本次作业的范围、要求或提示"
          />
        </el-form-item>
        <el-form-item label="题目分组">
          <el-select
            v-model="form.problemIds"
            multiple
            filterable
            style="width: 100%"
            placeholder="选择课程内题目（按选择顺序编号）"
          >
            <el-option v-for="p in allProblems" :key="p.id" :label="`${p.id}. ${p.title}`" :value="p.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="截止时间">
          <el-date-picker
            v-model="form.deadline"
            type="datetime"
            value-format="YYYY-MM-DD HH:mm:ss"
            placeholder="留空表示不限时"
            clearable
            style="width: 100%"
          />
          <p class="form-hint">截止后仍可提交，但只有截止前首次 AC 才计入「按时完成」。</p>
        </el-form-item>
        <el-form-item label="邀请码">
          <el-input
            v-model="form.joinCode"
            maxlength="12"
            placeholder="留空自动生成；编辑时留空保留原码"
            @input="normalizeCode"
          />
          <p class="form-hint">
            4–12 位字母或数字。学生凭此码加入；泄露后可在列表里点「轮换」立即换新。
          </p>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="saving" @click="save">保存</el-button>
      </template>
    </el-dialog>

    <!-- 班级完成率：按成员口径 -->
    <el-dialog
      v-model="completionVisible"
      :title="`完成率 —— ${completionCourse?.name || ''}`"
      width="min(760px, 96vw)"
      destroy-on-close
    >
      <p v-if="completionCourse" class="dialog-note">
        班级 {{ completionCourse.memberCount }} 人 · {{ completionCourse.problemCount }} 题 ·
        {{ completionCourse.deadline ? `截止 ${formatDateTime(completionCourse.deadline)}` : '不限时' }}
      </p>
      <el-table :data="completion" v-loading="completionLoading" size="small" empty-text="课程还没有题目">
        <el-table-column prop="problemId" label="#" width="66" />
        <el-table-column prop="title" label="题目" min-width="180" />
        <el-table-column prop="memberCount" label="班级人数" width="86" />
        <el-table-column prop="submittedUsers" label="提交人数" width="86" />
        <el-table-column prop="acceptedUsers" label="通过人数" width="86" />
        <el-table-column prop="onTimeAcceptedUsers" label="按时通过" width="86" />
        <el-table-column label="班级完成率" min-width="150">
          <template #default="{ row }">
            <el-progress
              :percentage="row.memberCount ? Math.round((row.acceptedUsers / row.memberCount) * 100) : 0"
              :stroke-width="10"
            />
          </template>
        </el-table-column>
      </el-table>
    </el-dialog>

    <!-- 学情矩阵：学生 × 题目，可导出 CSV -->
    <el-dialog
      v-model="rosterVisible"
      :title="`班级学情 —— ${rosterCourse?.name || ''}`"
      width="min(1080px, 96vw)"
      destroy-on-close
    >
      <div v-loading="rosterLoading" class="roster">
        <template v-if="roster">
          <div class="roster-summary">
            <div>
              <span class="summary-label">班级人数</span>
              <span class="summary-value">{{ roster.members.length }}<small>人</small></span>
            </div>
            <div>
              <span class="summary-label">平均完成</span>
              <span class="summary-value">{{ averagePercent }}<small>%</small></span>
            </div>
            <div>
              <span class="summary-label">已提交学生</span>
              <span class="summary-value">{{ activeMembers }}<small>人</small></span>
            </div>
            <div>
              <span class="summary-label">迟交通过</span>
              <span class="summary-value">{{ lateSolved }}<small>题次</small></span>
            </div>
            <div class="summary-actions">
              <el-button v-if="roster.course.joinCode" size="small" @click="copyCode(roster.course.joinCode)">
                复制邀请码
              </el-button>
              <el-button size="small" @click="exportCsv">导出 CSV</el-button>
            </div>
          </div>

          <div class="matrix-toolbar">
            <div class="legend">
              <span><i class="dot dot-ac" />已通过</span>
              <span><i class="dot dot-attempted" />尝试过</span>
              <span><i class="dot dot-none" />未开始</span>
            </div>
            <p>列号对应下方题目顺序 · 鼠标悬停单元格可查看提交次数</p>
          </div>

          <div v-if="roster.members.length" class="matrix-scroll">
            <table class="matrix">
              <thead>
                <tr>
                  <th class="student-col">学生</th>
                  <th v-for="(p, i) in roster.problems" :key="p.problemId" class="problem-col">
                    <span class="problem-index mono">{{ String(i + 1).padStart(2, '0') }}</span>
                    <span class="problem-title" :title="p.title">{{ p.title }}</span>
                  </th>
                  <th class="metric-col">完成</th>
                  <th class="metric-col">按时</th>
                  <th class="metric-col">最后活跃</th>
                  <th class="metric-col action-col">操作</th>
                </tr>
              </thead>
              <tbody>
                <tr v-for="m in roster.members" :key="m.userId">
                  <th class="student-col">
                    <div class="student-cell">
                      <span class="student-avatar" aria-hidden="true">{{ (m.username || '?').slice(0, 1).toUpperCase() }}</span>
                      <span class="student-name" :title="m.username">{{ m.username }}</span>
                    </div>
                  </th>
                  <td v-for="s in m.statuses" :key="s.problemId" class="status-cell">
                    <span
                      class="status-dot"
                      :class="`dot-${s.status}`"
                      :title="statusTitle(s)"
                      :aria-label="statusTitle(s)"
                    >
                      {{ s.status === 'ac' ? '✓' : s.status === 'attempted' ? '!' : '·' }}
                    </span>
                  </td>
                  <td class="metric-col mono">{{ m.solvedCount }}/{{ roster.problems.length }}</td>
                  <td class="metric-col mono">{{ m.onTimeSolvedCount }}</td>
                  <td class="metric-col activity">{{ m.lastSubmitAt ? relativeTime(m.lastSubmitAt) : '—' }}</td>
                  <td class="metric-col action-col">
                    <button type="button" class="row-remove" @click="removeMember(m)">移除</button>
                  </td>
                </tr>
              </tbody>
            </table>
          </div>
          <div v-else class="roster-empty">
            <p>还没有学生加入这个班级。</p>
            <p>把邀请码 <b class="mono">{{ roster.course.joinCode || '—' }}</b> 发给学生，他们输入后即可加入。</p>
          </div>
        </template>
      </div>
    </el-dialog>
  </AdminShell>
</template>

<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import {
  createCourse,
  updateCourse,
  deleteCourse,
  listCourses,
  getCourseProblems,
  getCourseCompletion,
  getCourseRoster,
  removeCourseMember,
  rotateCourseJoinCode,
  type Course,
  type CourseCompletionItem,
  type CourseForm,
  type CourseMember,
  type CourseMemberStatus,
  type CourseRoster,
} from '@/api/course'
import { pageProblems, type Problem } from '@/api/problem'
import { formatDateTime, parseServerTime, relativeTime } from '@/utils/datetime'
import AdminShell from '@/components/AdminShell.vue'

const courses = ref<Course[]>([])
const allProblems = ref<Problem[]>([])
const loading = ref(false)
const saving = ref(false)
const dialogVisible = ref(false)
const editingId = ref<number>()

const form = reactive<{
  name: string
  description: string
  problemIds: number[]
  joinCode: string
  deadline: string | null
}>({
  name: '',
  description: '',
  problemIds: [],
  joinCode: '',
  deadline: null,
})

const completionVisible = ref(false)
const completionLoading = ref(false)
const completionCourse = ref<Course>()
const completion = ref<CourseCompletionItem[]>([])

const rosterVisible = ref(false)
const rosterLoading = ref(false)
const rosterCourse = ref<Course>()
const roster = ref<CourseRoster | null>(null)

async function load() {
  loading.value = true
  try {
    courses.value = await listCourses()
  } catch {
    courses.value = []
  } finally {
    loading.value = false
  }
}

async function loadProblems() {
  // 后端 maxLimit=100；逐页拉全，避免题量超过 100 时教师选不到后面的题。
  allProblems.value = []
  try {
    let page = 1
    while (page <= 20) {
      const data = await pageProblems({ page, size: 100 })
      const records = data.records || []
      allProblems.value.push(...records)
      if (records.length < 100 || allProblems.value.length >= data.total) break
      page++
    }
  } catch {
    // 题目下拉加载失败不应阻断课程列表；拦截器已提示，保留已拿到的部分
    ElMessage.warning('题目列表加载失败，可稍后重新打开页面')
  }
}

function normalizeCode(value: string) {
  form.joinCode = value.toUpperCase().replace(/[^A-Z0-9]/g, '').slice(0, 12)
}

function openCreate() {
  editingId.value = undefined
  Object.assign(form, { name: '', description: '', problemIds: [], joinCode: '', deadline: null })
  dialogVisible.value = true
}

async function openEdit(row: Course) {
  editingId.value = row.id
  try {
    Object.assign(form, {
      name: row.name,
      description: row.description || '',
      problemIds: [],
      joinCode: row.joinCode || '',
      deadline: row.deadline || null,
    })
    const cps = await getCourseProblems(row.id)
    form.problemIds = cps.map((cp) => cp.problemId)
    dialogVisible.value = true
  } catch {
    editingId.value = undefined
  }
}

async function save() {
  if (!form.name.trim()) {
    ElMessage.warning('请输入课程名称')
    return
  }
  saving.value = true
  try {
    const payload: CourseForm = {
      name: form.name.trim(),
      description: form.description.trim() || undefined,
      problemIds: form.problemIds,
      joinCode: form.joinCode.trim() ? form.joinCode.trim().toUpperCase() : undefined,
      deadline: form.deadline || null,
    }
    if (editingId.value) {
      await updateCourse(editingId.value, payload)
      ElMessage.success('已更新')
    } else {
      await createCourse(payload)
      ElMessage.success('已创建，邀请码已就绪')
    }
    dialogVisible.value = false
    await load()
  } catch {
    // 拦截器已提示（邀请码冲突、题目不存在等），保留表单让教师修正
  } finally {
    saving.value = false
  }
}

async function showCompletion(row: Course) {
  completionCourse.value = row
  completionVisible.value = true
  completionLoading.value = true
  try {
    completion.value = await getCourseCompletion(row.id)
  } catch {
    completion.value = []
  } finally {
    completionLoading.value = false
  }
}

async function openRoster(row: Course) {
  rosterCourse.value = row
  roster.value = null
  rosterVisible.value = true
  rosterLoading.value = true
  try {
    roster.value = await getCourseRoster(row.id)
  } catch {
    rosterVisible.value = false
  } finally {
    rosterLoading.value = false
  }
}

async function copyCode(code?: string | null) {
  if (!code) return
  try {
    await navigator.clipboard.writeText(code)
    ElMessage.success(`邀请码 ${code} 已复制`)
  } catch {
    // http 环境或浏览器拒绝剪贴板权限时，至少把码展示出来
    ElMessage.info(`邀请码：${code}`)
  }
}

async function rotateCode(row: Course) {
  if (row.joinCode) {
    try {
      await ElMessageBox.confirm(
        `轮换后旧邀请码「${row.joinCode}」立即失效，已加入的学生不受影响。`,
        '轮换邀请码',
        { type: 'warning', confirmButtonText: '确认轮换' },
      )
    } catch {
      return
    }
  }
  try {
    const { joinCode } = await rotateCourseJoinCode(row.id)
    row.joinCode = joinCode
    if (rosterCourse.value?.id === row.id && roster.value) roster.value.course.joinCode = joinCode
    ElMessage.success(`新邀请码：${joinCode}`)
  } catch {
    // 拦截器已提示；保留旧码，避免教师误以为已轮换
  }
}

async function removeMember(member: CourseMember) {
  if (!roster.value) return
  try {
    await ElMessageBox.confirm(
      `将「${member.username}」移出班级？其历史提交会保留，但重新加入后进度从新的加入时间重新计算。`,
      '移除学生',
      { type: 'warning', confirmButtonText: '确认移除' },
    )
  } catch {
    return
  }
  try {
    await removeCourseMember(roster.value.course.id, member.userId)
    ElMessage.success('已移除')
    await openRoster(roster.value.course)
    await load()
  } catch {
    // 拦截器已提示
  }
}

async function remove(row: Course) {
  try {
    await ElMessageBox.confirm(
      `确认删除课程「${row.name}」？班级成员关系会一并清除，题目本身不受影响。`,
      '删除课程',
      { type: 'warning' },
    )
  } catch {
    return
  }
  try {
    await deleteCourse(row.id)
    ElMessage.success('已删除')
    await load()
  } catch {
    // 拦截器已提示
  }
}

function isOverdue(value?: string | null) {
  const d = parseServerTime(value)
  return !!d && d.getTime() < Date.now()
}

const averagePercent = computed(() => {
  if (!roster.value || !roster.value.members.length || !roster.value.problems.length) return 0
  const total = roster.value.members.length * roster.value.problems.length
  const solved = roster.value.members.reduce((sum, m) => sum + m.solvedCount, 0)
  return Math.round((solved * 100) / total)
})

const activeMembers = computed(() =>
  roster.value ? roster.value.members.filter((m) => m.submitCount > 0).length : 0,
)

const lateSolved = computed(() =>
  roster.value
    ? roster.value.members.reduce(
        (sum, m) => sum + Math.max(0, m.solvedCount - m.onTimeSolvedCount),
        0,
      )
    : 0,
)

function statusTitle(status: CourseMemberStatus) {
  if (status.status === 'ac') {
    return `已通过 · 首次 ${formatDateTime(status.firstAcAt)} · ${status.attempts} 次提交`
  }
  if (status.status === 'attempted') {
    return `尝试 ${status.attempts} 次，尚未通过 · 最后 ${formatDateTime(status.lastAt)}`
  }
  return '未提交'
}

function csvCell(value: unknown) {
  const text = String(value ?? '')
  // 防 CSV 公式注入：Excel 会把 = + - @ 开头的单元格当公式执行
  const safe = /^[=+\-@]/.test(text) ? `'${text}` : text
  return `"${safe.replace(/"/g, '""')}"`
}

function exportCsv() {
  const data = roster.value
  if (!data) return
  const now = new Date()
  const localDate = `${now.getFullYear()}-${String(now.getMonth() + 1).padStart(2, '0')}-${String(now.getDate()).padStart(2, '0')}`
  const header = [
    '学生',
    '完成题数',
    '按时完成',
    '提交次数',
    '最后活跃',
    ...data.problems.map((p, i) => `${i + 1}. ${p.title}`),
  ]
  const rows = data.members.map((m) => [
    m.username,
    m.solvedCount,
    m.onTimeSolvedCount,
    m.submitCount,
    formatDateTime(m.lastSubmitAt),
    ...m.statuses.map((s) => (s.status === 'ac' ? 'AC' : s.status === 'attempted' ? '尝试' : '未开始')),
  ])
  const csv = '\ufeff' + [header, ...rows].map((row) => row.map(csvCell).join(',')).join('\r\n')
  const blob = new Blob([csv], { type: 'text/csv;charset=utf-8' })
  const url = URL.createObjectURL(blob)
  const a = document.createElement('a')
  const safeName = (data.course.name || 'course').replace(/[\\/:*?"<>|]/g, '_')
  a.href = url
  a.download = `${safeName}-学情-${localDate}.csv`
  a.style.display = 'none'
  document.body.appendChild(a)
  a.click()
  a.remove()
  URL.revokeObjectURL(url)
}

onMounted(() => {
  load()
  loadProblems()
})
</script>

<style scoped>
.id-cell {
  font-size: 11px;
  color: var(--oj-text-weak);
}
.course-cell {
  display: flex;
  flex-direction: column;
  gap: 2px;
  min-width: 0;
}
.course-name {
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  font-size: 12px;
  font-weight: 650;
  color: var(--oj-text);
}
.course-desc {
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  font-size: 10px;
  color: var(--oj-text-weak);
}
.count-cell {
  display: block;
  font-size: 11px;
  color: var(--oj-text);
}
.count-sub {
  display: block;
  margin-top: 1px;
  font-size: 9px;
  color: var(--oj-text-weak);
}
.code-cell {
  display: flex;
  align-items: center;
  gap: 6px;
}
.code-text {
  padding: 2px 6px;
  border: 1px dashed var(--oj-border-strong);
  border-radius: 3px;
  background: var(--oj-gray-50);
  font-size: 10px;
  letter-spacing: 0.08em;
  color: var(--oj-text);
  cursor: pointer;
}
.code-text:hover {
  border-color: var(--oj-primary);
  color: var(--oj-primary);
}
.code-action,
.row-remove {
  padding: 0;
  border: 0;
  background: none;
  font-size: 9.5px;
  color: var(--oj-text-weak);
  cursor: pointer;
}
.code-action:hover,
.row-remove:hover {
  color: var(--oj-primary);
}
.row-remove:hover {
  color: var(--oj-danger);
}
.deadline-cell {
  font-size: 9.5px;
  color: var(--oj-text-sub);
}
.deadline-cell.is-overdue {
  color: var(--oj-text-weak);
  text-decoration: line-through;
}
.admin-hint {
  margin: 12px 2px 0;
  font-size: 10.5px;
  line-height: 1.7;
  color: var(--oj-text-weak);
}
.form-hint {
  margin: 5px 0 0;
  font-size: 10px;
  line-height: 1.6;
  color: var(--oj-text-weak);
}
.dialog-note {
  margin: 0 0 12px;
  font-size: 10.5px;
  color: var(--oj-text-sub);
}

/* 学情矩阵 */
.roster {
  min-height: 120px;
}
.roster-summary {
  display: flex;
  align-items: center;
  gap: 0;
  padding: 12px 14px;
  background: var(--oj-gray-50);
  border: 1px solid var(--oj-border);
  border-radius: var(--oj-radius-md);
}
.roster-summary > div {
  padding: 0 18px;
  border-left: 1px solid var(--oj-border);
}
.roster-summary > div:first-child {
  padding-left: 0;
  border-left: 0;
}
.summary-label {
  display: block;
  font-family: var(--oj-font-mono);
  font-size: 8px;
  letter-spacing: 0.08em;
  color: var(--oj-text-weak);
}
.summary-value {
  display: block;
  margin-top: 3px;
  font-family: var(--oj-font-mono);
  font-size: 20px;
  line-height: 1;
  font-weight: 600;
  color: var(--oj-text);
}
.summary-value small {
  margin-left: 1px;
  font-family: var(--oj-font-sans);
  font-size: 9px;
  font-weight: 500;
  color: var(--oj-text-sub);
}
.summary-actions {
  margin-left: auto;
  padding-right: 0;
  border-left: 0 !important;
}
.matrix-toolbar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  margin: 14px 0 8px;
}
.legend {
  display: flex;
  gap: 14px;
  font-size: 10px;
  color: var(--oj-text-sub);
}
.legend span {
  display: inline-flex;
  align-items: center;
  gap: 4px;
}
.dot {
  width: 7px;
  height: 7px;
  border-radius: 50%;
}
.dot-ac { background: var(--oj-success); }
.dot-attempted { background: var(--oj-warning); }
.dot-none { background: var(--oj-gray-300); }
.matrix-toolbar p {
  margin: 0;
  font-size: 9.5px;
  color: var(--oj-text-weak);
}
.matrix-scroll {
  overflow: auto;
  max-height: 56vh;
  border: 1px solid var(--oj-border);
  border-radius: var(--oj-radius-md);
}
.matrix {
  width: 100%;
  border-collapse: separate;
  border-spacing: 0;
  font-size: 11px;
}
.matrix th,
.matrix td {
  border-bottom: 1px solid var(--oj-gray-100);
  border-right: 1px solid var(--oj-gray-100);
  white-space: nowrap;
}
.matrix thead th {
  position: sticky;
  top: 0;
  z-index: 2;
  padding: 7px 9px;
  background: var(--oj-gray-50);
  text-align: left;
  font-weight: 600;
  color: var(--oj-text-sub);
}
.matrix tbody th,
.matrix tbody td {
  padding: 6px 9px;
  background: var(--oj-card);
}
.student-col {
  position: sticky;
  left: 0;
  z-index: 3;
  min-width: 150px;
  max-width: 180px;
  text-align: left !important;
  background: var(--oj-card);
  box-shadow: 1px 0 0 var(--oj-border);
}
.matrix thead .student-col {
  background: var(--oj-gray-50);
  z-index: 4;
}
.problem-col {
  min-width: 58px;
  max-width: 82px;
  text-align: center !important;
}
.problem-index {
  display: block;
  font-size: 8px;
  color: var(--oj-text-weak);
}
.problem-title {
  display: block;
  overflow: hidden;
  max-width: 72px;
  text-overflow: ellipsis;
  font-size: 9px;
  line-height: 1.4;
  color: var(--oj-text);
}
.metric-col {
  min-width: 62px;
  text-align: center !important;
  color: var(--oj-text-sub);
}
.action-col {
  min-width: 48px;
  border-right: 0;
}
.student-cell {
  display: flex;
  align-items: center;
  gap: 7px;
  min-width: 0;
}
.student-avatar {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 20px;
  height: 20px;
  flex-shrink: 0;
  border-radius: 50%;
  background: var(--oj-primary-soft);
  color: var(--oj-primary);
  font-size: 10px;
  font-weight: 700;
}
.student-name {
  overflow: hidden;
  text-overflow: ellipsis;
  font-size: 10.5px;
  font-weight: 600;
  color: var(--oj-text);
}
.status-cell {
  text-align: center !important;
}
.status-dot {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 19px;
  height: 19px;
  border-radius: 50%;
  font-family: var(--oj-font-mono);
  font-size: 9px;
  font-weight: 700;
  cursor: default;
}
.dot-ac {
  color: var(--oj-success);
  background: var(--oj-success-soft);
}
.dot-attempted {
  color: var(--oj-warning);
  background: var(--oj-warning-soft);
}
.dot-none {
  color: var(--oj-gray-400);
  background: var(--oj-gray-100);
}
.activity {
  font-size: 9.5px;
  color: var(--oj-text-weak);
}
.roster-empty {
  padding: 34px 16px;
  text-align: center;
  color: var(--oj-text-sub);
  border: 1px dashed var(--oj-border-strong);
  border-radius: var(--oj-radius-md);
}
.roster-empty p {
  margin: 4px 0;
  font-size: 11px;
}
.roster-empty b {
  color: var(--oj-primary);
}
@media (max-width: 768px) {
  .roster-summary {
    flex-wrap: wrap;
    gap: 12px 0;
  }
  .roster-summary > div {
    padding: 0 12px;
  }
  .summary-actions {
    width: 100%;
    margin-left: 0;
    padding: 8px 0 0 !important;
    border-top: 1px solid var(--oj-border);
  }
  .matrix-toolbar {
    align-items: flex-start;
    flex-direction: column;
  }
}
</style>
