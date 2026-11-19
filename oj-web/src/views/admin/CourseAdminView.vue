<template>
  <AdminShell
    title="课程管理"
    section="COURSES · 实验编排"
    description="创建课程、编排题目分组，并查看班级完成情况。"
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
        <el-table-column prop="name" label="课程名称" min-width="180" />
        <el-table-column prop="description" label="描述" min-width="240" show-overflow-tooltip />
        <el-table-column label="创建时间" width="170">
          <template #default="{ row }">
            <span class="mono time-cell">{{ formatDateTime(row.createdAt) }}</span>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="220">
          <template #default="{ row }">
            <el-button link type="primary" @click="showCompletion(row)">完成情况</el-button>
            <el-button link type="primary" @click="openEdit(row)">编辑</el-button>
            <el-button link type="danger" @click="remove(row)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>
    </div>

      <!-- 课程表单：名称/描述 + 题目分组多选（论文 3.3 课程实验题组管理） -->
      <el-dialog v-model="dialogVisible" :title="editingId ? '编辑课程' : '新建课程'" width="560px">
        <el-form :model="form" label-width="90px">
          <el-form-item label="课程名称"><el-input v-model="form.name" maxlength="100" /></el-form-item>
          <el-form-item label="描述"><el-input v-model="form.description" type="textarea" :rows="3" maxlength="500" /></el-form-item>
          <el-form-item label="题目分组">
            <el-select v-model="form.problemIds" multiple filterable style="width: 100%" placeholder="选择课程内题目（按选择顺序排序）">
              <el-option v-for="p in allProblems" :key="p.id" :label="`${p.id}. ${p.title}`" :value="p.id" />
            </el-select>
          </el-form-item>
        </el-form>
        <template #footer>
          <el-button @click="dialogVisible = false">取消</el-button>
          <el-button type="primary" :loading="saving" @click="save">保存</el-button>
        </template>
      </el-dialog>

      <!-- 完成情况（论文 3.3：教师可查班级完成情况，测试 F-15） -->
      <el-dialog v-model="completionVisible" :title="`完成情况 —— ${completionCourse?.name || ''}`" width="640px">
        <el-table :data="completion" v-loading="completionLoading" size="small" empty-text="该课程还没有学生提交记录">
          <el-table-column prop="problemId" label="#" width="70" />
          <el-table-column prop="title" label="题目" min-width="200" />
          <el-table-column prop="submittedUsers" label="提交人数" width="100" />
          <el-table-column prop="acceptedUsers" label="通过人数" width="100" />
          <el-table-column label="完成率" min-width="160">
            <template #default="{ row }">
              <el-progress
                :percentage="row.submittedUsers ? Math.round((row.acceptedUsers / row.submittedUsers) * 100) : 0"
                :stroke-width="10"
              />
            </template>
          </el-table-column>
        </el-table>
      </el-dialog>
  </AdminShell>
</template>

<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import {
  createCourse, updateCourse, deleteCourse, listCourses,
  getCourseProblems, getCourseCompletion,
  type Course, type CompletionItem,
} from '@/api/course'
import { pageProblems, type Problem } from '@/api/problem'
import { formatDateTime } from '@/utils/datetime'
import AdminShell from '@/components/AdminShell.vue'

const courses = ref<Course[]>([])
const allProblems = ref<Problem[]>([])
const loading = ref(false)
const saving = ref(false)
const dialogVisible = ref(false)
const editingId = ref<number>()
const form = reactive({ name: '', description: '', problemIds: [] as number[] })

const completionVisible = ref(false)
const completionLoading = ref(false)
const completionCourse = ref<Course>()
const completion = ref<CompletionItem[]>([])

async function load() {
  loading.value = true
  try {
    courses.value = await listCourses()
  } finally {
    loading.value = false
  }
}

async function loadProblems() {
  // 后端分页上限 maxLimit=100，传 200 会被静默截断
  const data = await pageProblems({ page: 1, size: 100 })
  allProblems.value = data.records || []
}

function openCreate() {
  editingId.value = undefined
  Object.assign(form, { name: '', description: '', problemIds: [] })
  dialogVisible.value = true
}

async function openEdit(row: Course) {
  editingId.value = row.id
  Object.assign(form, {
    name: row.name,
    description: row.description || '',
    problemIds: [],
  })
  // 后端 problems() 已 orderByAsc(CourseProblem::getSortOrder) 升序返回，
  // 这里再做一次本地 sort 是冗余的，直接按返回顺序取 problemId。
  const cps = await getCourseProblems(row.id)
  form.problemIds = cps.map((cp) => cp.problemId)
  dialogVisible.value = true
}

async function save() {
  if (!form.name.trim()) {
    ElMessage.warning('请输入课程名称')
    return
  }
  saving.value = true
  try {
    const payload = { name: form.name, description: form.description, problemIds: form.problemIds }
    if (editingId.value) {
      await updateCourse(editingId.value, payload)
      ElMessage.success('已更新')
    } else {
      await createCourse(payload)
      ElMessage.success('已创建')
    }
    dialogVisible.value = false
    load()
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
  } finally {
    completionLoading.value = false
  }
}

async function remove(row: Course) {
  // 取消会让 confirm reject，吞掉它避免 unhandled rejection；确认后的删除逻辑不变
  try {
    await ElMessageBox.confirm(`确认删除课程「${row.name}」？`, '提示', { type: 'warning' })
  } catch {
    return
  }
  await deleteCourse(row.id)
  ElMessage.success('已删除')
  load()
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
.time-cell {
  font-size: 10px;
  color: var(--oj-text-sub);
}
</style>
