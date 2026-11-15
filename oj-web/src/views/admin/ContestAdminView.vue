<template>
  <div class="page-container">
    <div class="card">
      <div class="toolbar">
        <el-button type="primary" @click="openCreate">创建竞赛</el-button>
      </div>
      <el-table :data="contests" v-loading="loading">
        <el-table-column prop="id" label="#" width="60" />
        <el-table-column prop="title" label="比赛名称" min-width="200" />
        <el-table-column label="时间" min-width="280">
          <template #default="{ row }">
            {{ row.startTime.slice(0, 16).replace('T', ' ') }} ~ {{ row.endTime.slice(0, 16).replace('T', ' ') }}
          </template>
        </el-table-column>
        <el-table-column label="状态" width="90">
          <template #default="{ row }">
            <el-tag :type="statusInfo(row.status).type" size="small">{{ statusInfo(row.status).label }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="problemCount" label="题数" width="70" />
        <el-table-column label="操作" width="140">
          <template #default="{ row }">
            <el-button link type="primary" @click="openEdit(row)">编辑</el-button>
            <el-button link type="danger" @click="remove(row)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>

      <el-dialog v-model="dialogVisible" :title="editingId ? '编辑竞赛' : '创建竞赛'" width="620px">
        <el-form :model="form" label-width="90px">
          <el-form-item label="比赛名称"><el-input v-model="form.title" maxlength="200" /></el-form-item>
          <el-form-item label="说明"><el-input v-model="form.description" type="textarea" :rows="3" maxlength="500" /></el-form-item>
          <el-form-item label="起止时间">
            <el-date-picker v-model="timeRange" type="datetimerange" value-format="YYYY-MM-DDTHH:mm:ss"
              start-placeholder="开始时间" end-placeholder="结束时间" style="width: 100%" />
          </el-form-item>
          <el-form-item label="比赛题目">
            <el-select v-model="form.problemIds" multiple filterable style="width: 100%"
              placeholder="选择题目（按顺序对应字母题号 A/B/C…）">
              <el-option v-for="p in allProblems" :key="p.id" :label="`${p.id}. ${p.title}`" :value="p.id" />
            </el-select>
          </el-form-item>
        </el-form>
        <template #footer>
          <el-button @click="dialogVisible = false">取消</el-button>
          <el-button type="primary" :loading="saving" @click="save">保存</el-button>
        </template>
      </el-dialog>
    </div>
  </div>
</template>

<script setup lang="ts">
// 竞赛管理（教师）：创建/编辑/删除，时间窗 + 题目集
import { onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import {
  createContest, updateContest, deleteContest, listContests, getContest,
  type ContestVO,
} from '@/api/contest'
import { pageProblems, type Problem } from '@/api/problem'

const contests = ref<ContestVO[]>([])
const allProblems = ref<Problem[]>([])
const loading = ref(false)
const saving = ref(false)
const dialogVisible = ref(false)
const editingId = ref<number>()
const form = reactive({ title: '', description: '', startTime: '', endTime: '', problemIds: [] as number[] })
const timeRange = ref<[string, string] | null>(null)

const statusInfo = (s: number) =>
  [{ label: '未开始', type: 'info' }, { label: '进行中', type: 'success' }, { label: '已结束', type: 'info' }][s] ?? { label: '?', type: 'info' }

async function load() {
  loading.value = true
  try {
    contests.value = await listContests()
  } finally {
    loading.value = false
  }
}

async function loadProblems() {
  const data = await pageProblems({ page: 1, size: 100 })
  allProblems.value = data.records || []
}

function openCreate() {
  editingId.value = undefined
  Object.assign(form, { title: '', description: '', startTime: '', endTime: '', problemIds: [] })
  timeRange.value = null
  dialogVisible.value = true
}

async function openEdit(row: ContestVO) {
  editingId.value = row.id
  const detail = await import('@/api/contest').then((m) => m.getContest(row.id))
  Object.assign(form, {
    title: detail.title,
    description: detail.description || '',
    problemIds: detail.problems.map((p) => p.problemId),
  })
  timeRange.value = [detail.startTime.slice(0, 19), detail.endTime.slice(0, 19)]
  dialogVisible.value = true
}

async function save() {
  if (!form.title || !timeRange.value) {
    ElMessage.warning('请填写名称与起止时间')
    return
  }
  saving.value = true
  try {
    const payload = { ...form, startTime: timeRange.value[0], endTime: timeRange.value[1] }
    if (editingId.value) {
      await updateContest(editingId.value, payload)
      ElMessage.success('已更新')
    } else {
      await createContest(payload)
      ElMessage.success('已创建')
    }
    dialogVisible.value = false
    load()
  } finally {
    saving.value = false
  }
}

async function remove(row: ContestVO) {
  await ElMessageBox.confirm(`确认删除竞赛「${row.title}」？`, '提示', { type: 'warning' })
  await deleteContest(row.id)
  ElMessage.success('已删除')
  load()
}

onMounted(() => {
  load()
  loadProblems()
})
</script>

<style scoped>
.toolbar {
  margin-bottom: 16px;
}
</style>
