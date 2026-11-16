<template>
  <div class="page-container">
    <div class="card">
      <div class="toolbar">
        <el-input
          v-model="keyword"
          placeholder="按标题搜索"
          style="width: 220px"
          clearable
          @keyup.enter="load"
          @clear="load"
        />
        <el-button @click="load">搜索</el-button>
        <el-button type="primary" @click="openCreate">新建题目</el-button>
        <el-button @click="noticeVisible = true; loadNotices()">公告管理</el-button>
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

      <!-- 题目表单（含 Markdown 实时预览，论文 5.7） -->
      <el-dialog v-model="dialogVisible" :title="editingId ? '编辑题目' : '新建题目'" width="860px">
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
          <el-form-item label="描述">
            <div class="md-editor">
              <div class="md-pane">
                <div class="md-pane-title">Markdown 源码</div>
                <el-input v-model="form.description" type="textarea" :rows="10" placeholder="支持 Markdown：## 标题、**加粗**、`代码`" />
              </div>
              <div class="md-pane preview">
                <div class="md-pane-title">实时预览</div>
                <MarkdownPreview :content="form.description" />
              </div>
            </div>
          </el-form-item>
          <el-form-item label="样例">
            <div class="samples-editor">
              <div v-for="(s, i) in form.samples" :key="i" class="sample-row">
                <el-input v-model="s.input" type="textarea" :rows="2" placeholder="样例输入" />
                <el-input v-model="s.output" type="textarea" :rows="2" placeholder="样例输出" />
                <el-button link type="danger" @click="form.samples.splice(i, 1)">删除</el-button>
              </div>
              <el-button size="small" @click="form.samples.push({ input: '', output: '' })">
                + 添加样例
              </el-button>
            </div>
          </el-form-item>
          <el-form-item label="标签">
            <el-select v-model="form.tags" multiple filterable allow-create default-first-option
              style="width: 100%" placeholder="选择或输入标签（回车创建）：动态规划/贪心/模拟…">
              <el-option v-for="t in ['动态规划', '贪心', '模拟', '字符串', '数学', '二分', '图论']" :key="t" :label="t" :value="t" />
            </el-select>
          </el-form-item>
          <el-form-item label="语言" required>
            <el-checkbox-group v-model="form.languages">
              <el-checkbox value="c">C</el-checkbox>
              <el-checkbox value="cpp">C++</el-checkbox>
              <el-checkbox value="java">Java</el-checkbox>
              <el-checkbox value="python3">Python3</el-checkbox>
              <el-checkbox value="js">JavaScript</el-checkbox>
            </el-checkbox-group>
          </el-form-item>
          <el-form-item label="上架"><el-switch v-model="form.enabled" /></el-form-item>
        </el-form>
        <template #footer>
          <el-button @click="dialogVisible = false">取消</el-button>
          <el-button type="primary" :loading="saving" @click="save">保存</el-button>
        </template>
      </el-dialog>

      <!-- 公告管理（教学场景：实验截止提醒） -->
      <el-dialog v-model="noticeVisible" title="公告管理" width="560px">
        <div style="display: flex; gap: 8px; margin-bottom: 12px">
          <el-input v-model="noticeForm.title" placeholder="公告标题" style="width: 200px" />
          <el-input v-model="noticeForm.content" placeholder="公告内容（实验截止时间等）" />
          <el-button type="primary" @click="publishNotice">发布</el-button>
        </div>
        <el-table :data="notices" size="small">
          <el-table-column prop="title" label="标题" min-width="140" />
          <el-table-column prop="content" label="内容" min-width="220" show-overflow-tooltip />
          <el-table-column label="操作" width="70">
            <template #default="{ row }">
              <el-button link type="danger" @click="removeNotice(row)">删除</el-button>
            </template>
          </el-table-column>
        </el-table>
      </el-dialog>
    </div>
  </div>
</template>

<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { pageProblems, getProblem, createProblem, updateProblem, deleteProblem, type Problem } from '@/api/problem'
import MarkdownPreview from '@/components/MarkdownPreview.vue'
import { publishAnnouncement, deleteAnnouncement, listAnnouncements, type AnnouncementItem } from '@/api/announcement'

const problems = ref<Problem[]>([])
const keyword = ref('')
const page = ref(1)
const size = ref(10)
const total = ref(0)
const loading = ref(false)
const saving = ref(false)
const dialogVisible = ref(false)
const editingId = ref<number>()
const noticeVisible = ref(false)
const notices = ref<AnnouncementItem[]>([])
const noticeForm = reactive({ title: '', content: '' })

const form = reactive({
  title: '',
  difficulty: 1,
  timeLimitMs: 1000,
  memoryLimitMb: 256,
  description: '',
  tags: [] as string[],
  samples: [] as { input: string; output: string }[],
  languages: ['c', 'cpp', 'java', 'python3'] as string[],
  enabled: true,
})

async function load() {
  loading.value = true
  try {
    const data = await pageProblems({ keyword: keyword.value || undefined, page: page.value, size: size.value })
    problems.value = data.records || []
    total.value = data.total
  } finally {
    loading.value = false
  }
}

function openCreate() {
  editingId.value = undefined
  Object.assign(form, {
    title: '',
    difficulty: 1,
    timeLimitMs: 1000,
    memoryLimitMb: 256,
    description: '',
    tags: [],
    samples: [],
    languages: ['c', 'cpp', 'java', 'python3'],
    enabled: true,
  })
  dialogVisible.value = true
}

function edit(row: Problem) {
  editingId.value = row.id
  dialogVisible.value = true
  // 列表接口按契约瘦身（不返回 description/samples/languages），
  // 必须以详情接口回填，否则保存会用空值覆盖题面。
  getProblem(row.id)
    .then((detail) => {
      Object.assign(form, {
        title: detail.title,
        difficulty: detail.difficulty,
        timeLimitMs: detail.timeLimitMs,
        memoryLimitMb: detail.memoryLimitMb,
        description: detail.description || '',
        tags: detail.tags || [],
        samples: (detail.samples || []).map((s) => ({ input: s.input, output: s.output })),
        languages: detail.languages || [],
        enabled: detail.enabled,
      })
    })
    .catch(() => {
      dialogVisible.value = false
      editingId.value = undefined
    })
}

async function save() {
  // 前端校验（后端也会兜，但空 languages 会让该题任何语言都无法提交，必须提前拦）
  if (!form.title.trim()) {
    ElMessage.warning('请填写题目名称')
    return
  }
  if (!form.languages.length) {
    ElMessage.warning('至少勾选一种允许提交的语言')
    return
  }
  if (!form.timeLimitMs || form.timeLimitMs <= 0) {
    ElMessage.warning('时间限制必须大于 0')
    return
  }
  if (!form.memoryLimitMb || form.memoryLimitMb <= 0) {
    ElMessage.warning('内存限制必须大于 0')
    return
  }
  if (!form.difficulty) {
    ElMessage.warning('请选择难度')
    return
  }
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

async function loadNotices() {
  notices.value = await listAnnouncements()
}

async function publishNotice() {
  if (!noticeForm.title.trim() || !noticeForm.content.trim()) {
    ElMessage.warning('标题与内容不能为空')
    return
  }
  await publishAnnouncement(noticeForm)
  ElMessage.success('公告已发布')
  noticeForm.title = ''
  noticeForm.content = ''
  loadNotices()
}

async function removeNotice(row: AnnouncementItem) {
  await deleteAnnouncement(row.id)
  ElMessage.success('已删除')
  loadNotices()
}

onMounted(load)
</script>

<style scoped>
.toolbar {
  margin-bottom: 16px;
}
.md-editor {
  width: 100%;
  display: flex;
  gap: 12px;
}
.samples-editor {
  width: 100%;
}
.sample-row {
  display: flex;
  gap: 8px;
  margin-bottom: 8px;
  align-items: flex-start;
}
.md-pane {
  flex: 1;
  min-width: 0;
}
.md-pane-title {
  font-size: 12px;
  color: #909399;
  margin-bottom: 6px;
}
.md-pane.preview {
  background: #f5f7fa;
  border: 1px solid #e4e7ed;
  border-radius: 6px;
  padding: 10px;
  max-height: 260px;
  overflow: auto;
}
</style>
