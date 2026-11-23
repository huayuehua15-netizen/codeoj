<template>
  <AdminShell
    title="班级作业"
    section="ASSIGNMENTS · 班级发题"
    description="把题组发给班级：设置邀请码与截止时间，学生加入后即可领取作业。"
  >
    <template #actions>
      <el-button type="primary" @click="openCreate">新建作业</el-button>
    </template>

    <div class="admin-table-shell">
      <el-table :data="courses" v-loading="loading" empty-text="还没有作业，点右上角「新建作业」开始">
        <el-table-column prop="id" label="ID" width="64">
          <template #default="{ row }">
            <span class="mono id-cell">{{ String(row.id).padStart(3, '0') }}</span>
          </template>
        </el-table-column>
        <el-table-column label="作业" min-width="240">
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
        <el-table-column label="操作" width="160" fixed="right">
          <template #default="{ row }">
            <el-button link type="primary" @click="openEdit(row)">编辑</el-button>
            <el-button link type="danger" @click="remove(row)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>
    </div>

    <!-- 作业表单：名称/描述 + 题组 + 邀请码 + 截止时间 -->
    <el-dialog
      v-model="dialogVisible"
      :title="editingId ? '编辑作业' : '新建作业'"
      width="min(600px, 94vw)"
      destroy-on-close
    >
      <el-form :model="form" label-width="88px">
        <el-form-item label="作业名称">
          <el-input v-model="form.name" maxlength="100" placeholder="例如：数据结构 · 第 3 周作业" />
        </el-form-item>
        <el-form-item label="作业说明">
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
            placeholder="选择作业题目（按选择顺序编号）"
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
  </AdminShell>
</template>

<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import {
  createCourse,
  updateCourse,
  deleteCourse,
  listCourses,
  getCourseProblems,
  rotateCourseJoinCode,
  type Course,
  type CourseForm,
} from '@/api/course'
import { pageProblems, type Problem } from '@/api/problem'
import { formatDateTime, parseServerTime } from '@/utils/datetime'
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
    ElMessage.warning('请输入作业名称')
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
    ElMessage.success(`新邀请码：${joinCode}`)
  } catch {
    // 拦截器已提示；保留旧码，避免教师误以为已轮换
  }
}

async function remove(row: Course) {
  try {
    await ElMessageBox.confirm(
      `确认删除作业「${row.name}」？班级成员关系会一并清除，题目本身不受影响。`,
      '删除作业',
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
.code-action {
  padding: 0;
  border: 0;
  background: none;
  font-size: 9.5px;
  color: var(--oj-text-weak);
  cursor: pointer;
}
.code-action:hover {
  color: var(--oj-primary);
}
.deadline-cell {
  font-size: 9.5px;
  color: var(--oj-text-sub);
}
.deadline-cell.is-overdue {
  color: var(--oj-text-weak);
  text-decoration: line-through;
}
.form-hint {
  margin: 5px 0 0;
  font-size: 10px;
  line-height: 1.6;
  color: var(--oj-text-weak);
}
</style>
