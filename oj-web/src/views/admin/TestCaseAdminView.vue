<template>
  <div class="page-container">
    <div class="card">
      <h2>测试用例管理（题目 #{{ $route.params.id }}）</h2>

      <!-- 批量导入（论文 5.7：文本粘贴解析） -->
      <div class="import-area">
        <el-input
          v-model="pasteText"
          type="textarea"
          :rows="8"
          placeholder="粘贴批量用例，格式：每组用例之间用 ===INPUT=== 与 ===OUTPUT=== 分隔，组间空行分隔（第二阶段实现解析器）"
        />
        <div style="margin-top: 10px">
          <el-button type="primary" :loading="importing" @click="importCases">批量导入</el-button>
        </div>
      </div>

      <h3>已有用例</h3>
      <el-table :data="cases" v-loading="loading">
        <el-table-column prop="id" label="#" width="70" />
        <el-table-column prop="input" label="输入" min-width="200" show-overflow-tooltip />
        <el-table-column prop="expectedOutput" label="期望输出" min-width="200" show-overflow-tooltip />
        <el-table-column label="样例" width="80">
          <template #default="{ row }">{{ row.isSample ? '是' : '否' }}</template>
        </el-table-column>
        <el-table-column label="操作" width="80">
          <template #default="{ row }">
            <el-button link type="danger" @click="remove(row)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>
    </div>
  </div>
</template>

<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { useRoute } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { importTestCases, listTestCases, deleteTestCase } from '@/api/problem'

const route = useRoute()
const problemId = route.params.id as string

const cases = ref<{ id: number; input: string; expectedOutput: string; isSample: boolean }[]>([])
const pasteText = ref('')
const loading = ref(false)
const importing = ref(false)

async function load() {
  loading.value = true
  try {
    cases.value = await listTestCases(problemId)
  } finally {
    loading.value = false
  }
}

async function importCases() {
  // TODO 第二阶段：文本粘贴解析器（===INPUT=== / ===OUTPUT=== 分隔格式）
  ElMessage.info('文本粘贴解析将在第二阶段实现')
}

async function remove(row: { id: number }) {
  await ElMessageBox.confirm('确认删除该用例？', '提示', { type: 'warning' })
  await deleteTestCase(problemId, row.id)
  load()
}

onMounted(load)
</script>

<style scoped>
.import-area {
  margin-bottom: 20px;
}
</style>
