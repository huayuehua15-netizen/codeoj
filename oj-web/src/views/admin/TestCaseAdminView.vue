<template>
  <div class="page-container">
    <div class="card">
      <div class="page-head">
        <div>
          <h2>测试用例管理（题目 #{{ problemId }}）</h2>
          <p class="sub">批量粘贴导入判题用例，并维护样例与分值</p>
        </div>
        <el-button @click="router.push('/admin/problems')">返回题目列表</el-button>
      </div>

      <!-- 批量导入（论文 5.7：管理端文本粘贴解析） -->
      <div class="import-area">
        <div class="import-options">
          <span class="opt-label">样例组数（前 N 组在题面展示）</span>
          <el-input-number v-model="sampleCount" :min="0" :max="20" size="small" />
          <span class="opt-label">每组分值</span>
          <el-input-number v-model="score" :min="0" :max="100" size="small" />
          <el-button type="primary" :loading="importing" :disabled="parsed.length === 0" @click="importCases">
            导入 {{ parsed.length }} 组
          </el-button>
          <el-tag v-if="parseError" type="danger" size="small">{{ parseError }}</el-tag>
        </div>
        <el-input
          v-model="pasteText"
          type="textarea"
          :rows="10"
          placeholder="粘贴批量用例，格式如下（组之间用空行分隔）：&#10;&#10;===INPUT===&#10;1 2&#10;===OUTPUT===&#10;3&#10;&#10;===INPUT===&#10;10 20&#10;===OUTPUT===&#10;30"
          @input="parse"
        />
        <!-- 解析预览 -->
        <el-table v-if="parsed.length" :data="parsed" size="small" style="margin-top: 12px" max-height="220">
          <el-table-column label="#" width="60">
            <template #default="{ $index }">{{ $index + 1 }}</template>
          </el-table-column>
          <el-table-column label="输入" min-width="180" show-overflow-tooltip>
            <template #default="{ row }">
              <pre class="preview-pre">{{ row.input }}</pre>
            </template>
          </el-table-column>
          <el-table-column label="期望输出" min-width="180" show-overflow-tooltip>
            <template #default="{ row }">
              <pre class="preview-pre">{{ row.expectedOutput }}</pre>
            </template>
          </el-table-column>
          <el-table-column label="样例" width="80">
            <template #default="{ $index }">{{ $index < sampleCount ? '是' : '否' }}</template>
          </el-table-column>
        </el-table>
      </div>

      <h3>已有用例</h3>
      <el-table :data="cases" v-loading="loading" empty-text="还没有测试用例，在上方粘贴后点「导入」">
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
import { onMounted, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { importTestCases, listTestCases, deleteTestCase } from '@/api/problem'

const route = useRoute()
const router = useRouter()

/**
 * 路由参数是字符串，直接拼进 URL 会把非法值送到后端（如 /admin/problems/abc/testcases）。
 * 这里先做一次数字校验：不合法就提示并退回列表，完全不发请求。
 */
const problemId = ref(String(route.params.id ?? ''))

const cases = ref<{ id: number; input: string; expectedOutput: string; isSample: boolean }[]>([])
const pasteText = ref('')
const sampleCount = ref(0)
const score = ref(1)
const loading = ref(false)
const importing = ref(false)

interface ParsedCase {
  input: string
  expectedOutput: string
}

const parsed = ref<ParsedCase[]>([])
const parseError = ref('')

/**
 * 文本粘贴解析（论文 5.7）：
 * 以 ===INPUT=== / ===OUTPUT=== 标记分组，组间空行分隔；
 * 标记行须独立成行，内容保留原始换行与前后空白（仅去首尾空行）。
 */
function parse() {
  parseError.value = ''
  parsed.value = []
  const text = pasteText.value
  if (!text.trim()) return

  // 全局匹配每一组：===INPUT=== 内容 ===OUTPUT=== 内容（到下一组标记或结尾）
  const pattern = /===INPUT===\s*\n?([\s\S]*?)\s*===OUTPUT===\s*\n?([\s\S]*?)(?=(?:\n\s*\n)?===INPUT===|$)/g
  const results: ParsedCase[] = []
  let match: RegExpExecArray | null
  while ((match = pattern.exec(text)) !== null) {
    const input = match[1].replace(/^\n+|\n+$/g, '')
    const expectedOutput = match[2].replace(/^\n+|\n+$/g, '')
    if (!input && !expectedOutput) continue
    results.push({ input, expectedOutput })
  }

  // 没有标记时降级：单组整体作为输入，输出留空并提示
  if (results.length === 0 && text.includes('===INPUT===')) {
    parseError.value = '检测到 ===INPUT=== 标记但未解析出有效用例，请检查 ===OUTPUT=== 标记'
    return
  }
  if (results.length === 0) {
    parseError.value = '未检测到用例标记，请使用 ===INPUT=== 与 ===OUTPUT=== 分隔'
    return
  }
  parsed.value = results
}

async function load() {
  if (!isValidProblemId()) return
  loading.value = true
  try {
    cases.value = await listTestCases(problemId.value)
  } finally {
    loading.value = false
  }
}

async function importCases() {
  if (!isValidProblemId()) return
  importing.value = true
  try {
    const payload = parsed.value.map((c, i) => ({
      input: c.input,
      expectedOutput: c.expectedOutput,
      score: score.value,
      isSample: i < sampleCount.value,
    }))
    const count = await importTestCases(problemId.value, payload)
    ElMessage.success(`成功导入 ${count} 组用例`)
    pasteText.value = ''
    parsed.value = []
    load()
  } finally {
    importing.value = false
  }
}

async function remove(row: { id: number }) {
  // 取消按钮会让 confirm reject，吞掉它避免 unhandled rejection；确认后的删除逻辑不变
  try {
    await ElMessageBox.confirm('确认删除该用例？', '提示', { type: 'warning' })
  } catch {
    return
  }
  await deleteTestCase(problemId.value, row.id)
  ElMessage.success('已删除')
  load()
}

function isValidProblemId() {
  const n = Number(problemId.value)
  if (!Number.isFinite(n) || n <= 0) {
    ElMessage.error('题目编号不合法')
    router.push('/admin/problems')
    return false
  }
  return true
}

/**
 * 路由是 /admin/problems/:id/testcases，从题目 A 的用例页跳到题目 B 时组件会被复用、
 * setup 不会重跑；没有这个 watch 就会继续显示 A 的用例。
 */
watch(
  () => route.params.id,
  () => {
    problemId.value = String(route.params.id ?? '')
    cases.value = []
    pasteText.value = ''
    parsed.value = []
    parseError.value = ''
    load()
  },
)

onMounted(load)
</script>

<style scoped>
.page-head {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 16px;
  margin-bottom: 16px;
}
.page-head h2 {
  margin: 0;
}
.sub {
  color: var(--oj-gray-400);
  font-size: 12.5px;
  margin: 4px 0 0;
}
.import-area {
  margin-bottom: 20px;
}
.import-options {
  display: flex;
  align-items: center;
  gap: 10px;
  margin-bottom: 10px;
}
.opt-label {
  color: var(--oj-gray-600);
  font-size: 13px;
}
.preview-pre {
  margin: 0;
  font-family: Consolas, Monaco, monospace;
  font-size: 12px;
  white-space: pre-wrap;
  word-break: break-all;
}
</style>
