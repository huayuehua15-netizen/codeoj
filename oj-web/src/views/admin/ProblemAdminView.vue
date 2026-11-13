<template>
  <div class="page-container">
    <div class="card">
      <div class="toolbar">
        <el-button type="primary" @click="dialogVisible = true">新建题目</el-button>
      </div>
      <el-table :data="problems" v-loading="loading">
        <el-table-column prop="id" label="#" width="70" />
        <el-table-column prop="title" label="标题" min-width="240" />
        <el-table-column prop="difficulty" label="难度" width="90" />
        <el-table-column label="状态" width="90">
          <template #default="{ row }">
            <el-tag :type="row.enabled ? 'success' : 'info'" size="small">
              {{ row.enabled ? '已上架' : '已下架' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="220">
          <template #default="{ row }">
            <el-button link type="primary" @click="$router.push(`/admin/problems/${row.id}/testcases`)">用例</el-button>
            <el-button link type="primary" @click="edit(row)">编辑</el-button>
            <el-button link type="danger" @click="remove(row)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>
      <el-pagination
        v-model:current-page="page"
        :page-size="size"
        :total="total"
        layout="prev, pager, next"
        style="margin-top: 16px; justify-content: flex-end"
        @current-change="load"
      />

      <!-- TODO 第二阶段：题目表单完整实现（含 Markdown 实时预览，论文 5.7） -->
      <el-dialog v-model="dialogVisible" :title="editingId ? '编辑题目' : '新建题目'" width="600px">
        <el-form :model="form" label-width="90px">
          <el-form-item label="标题"><el-input v-model="form.title" /></el-form-item>
          <el-form-item label="难度">
            <el-select v-model="form.difficulty" style="width: 160px">
              <el-option label="简单" :value="1" />
              <el-option label="中等" :value="2" />
              <el-option label="困难" :value="3" />
            </el-select>
          </el-form-item>
          <el-form-item label="时间限制"><el-input-number v-model="form.timeLimitMs" :min="100" :step="100" /></el-form-item>
          <el-form-item label="内存限制"><el-input-number v-model="form.memoryLimitMb" :min="16" :step="16" /></el-form-item>
          <el-form-item label="描述"><el-input v-model="form.description" type="textarea" :rows="6" /></el-form-item>
          <el-form-item label="语言">
            <el-checkbox-group v-model="form.languages">
              <el-checkbox value="c">C</el-checkbox>
              <el-checkbox value="cpp">C++</el-checkbox>
              <el-checkbox value="java">Java</el-checkbox>
              <el-checkbox value="python3">Python3</el-checkbox>
            </el-checkbox-group>
          </el-form-item>
          <el-form-item label="上架"><el-switch v-model="form.enabled" /></el-form-item>
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
import { onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { pageProblems, createProblem, updateProblem, deleteProblem, type Problem } from '@/api/problem'

const problems = ref<Problem[]>([])
const page = ref(1)
const size = ref(10)
const total = ref(0)
const loading = ref(false)
const saving = ref(false)
const dialogVisible = ref(false)
const editingId = ref<number>()

const form = reactive({
  title: '',
  difficulty: 1,
  timeLimitMs: 1000,
  memoryLimitMb: 256,
  description: '',
  languages: ['c', 'cpp', 'java', 'python3'] as string[],
  enabled: true,
})

async function load() {
  loading.value = true
  try {
    const data = await pageProblems({ page: page.value, size: size.value })
    problems.value = data.records || []
    total.value = data.total
  } finally {
    loading.value = false
  }
}

function edit(row: Problem) {
  editingId.value = row.id
  Object.assign(form, {
    title: row.title,
    difficulty: row.difficulty,
    timeLimitMs: row.timeLimitMs,
    memoryLimitMb: row.memoryLimitMb,
    description: row.description || '',
    languages: row.languages || [],
    enabled: row.enabled,
  })
  dialogVisible.value = true
}

async function save() {
  saving.value = true
  try {
    if (editingId.value) {
      await updateProblem(editingId.value, { ...form })
      ElMessage.success('已更新')
    } else {
      await createProblem({ ...form })
      ElMessage.success('已创建')
    }
    dialogVisible.value = false
    load()
  } finally {
    saving.value = false
  }
}

async function remove(row: Problem) {
  await ElMessageBox.confirm(`确认删除题目「${row.title}」？`, '提示', { type: 'warning' })
  await deleteProblem(row.id)
  ElMessage.success('已删除')
  load()
}

onMounted(load)
</script>

<style scoped>
.toolbar {
  margin-bottom: 16px;
}
</style>
