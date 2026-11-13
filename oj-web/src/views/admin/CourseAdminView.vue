<template>
  <div class="page-container">
    <div class="card">
      <div class="toolbar">
        <el-button type="primary" @click="dialogVisible = true">新建课程</el-button>
      </div>
      <el-table :data="courses" v-loading="loading">
        <el-table-column prop="id" label="#" width="70" />
        <el-table-column prop="name" label="课程名称" min-width="200" />
        <el-table-column prop="description" label="描述" min-width="280" show-overflow-tooltip />
        <el-table-column label="操作" width="140">
          <template #default="{ row }">
            <el-button link type="primary" @click="edit(row)">编辑</el-button>
            <el-button link type="danger" @click="remove(row)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>

      <!-- TODO 第二阶段：课程内题目分组选择与完成情况统计页（论文 3.3） -->
      <el-dialog v-model="dialogVisible" :title="editingId ? '编辑课程' : '新建课程'" width="500px">
        <el-form :model="form" label-width="90px">
          <el-form-item label="课程名称"><el-input v-model="form.name" /></el-form-item>
          <el-form-item label="描述"><el-input v-model="form.description" type="textarea" :rows="3" /></el-form-item>
        </el-form>
        <template #footer>
          <el-button @click="dialogVisible = false">取消</el-button>
          <el-button type="primary" @click="save">保存</el-button>
        </template>
      </el-dialog>
    </div>
  </div>
</template>

<script setup lang="ts">
// 课程实验管理（骨架）。TODO 第二阶段：接入课程 CRUD 接口与题目分组
import { onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'

const courses = ref<{ id: number; name: string; description: string }[]>([])
const loading = ref(false)
const dialogVisible = ref(false)
const editingId = ref<number>()
const form = reactive({ name: '', description: '' })

function edit(_row: { id: number; name: string; description: string }) {
  ElMessage.info('课程编辑将在第二阶段接入接口')
}

async function remove(_row: { id: number }) {
  await ElMessageBox.confirm('确认删除该课程？', '提示', { type: 'warning' })
  ElMessage.info('课程删除将在第二阶段接入接口')
}

function save() {
  dialogVisible.value = false
}

onMounted(() => {
  loading.value = false
})
</script>

<style scoped>
.toolbar {
  margin-bottom: 16px;
}
</style>
