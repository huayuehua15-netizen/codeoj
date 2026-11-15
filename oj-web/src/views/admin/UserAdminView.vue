<template>
  <div class="page-container">
    <div class="card">
      <div class="toolbar">
        <el-input v-model="keyword" placeholder="按用户名搜索" style="width: 220px" clearable @keyup.enter="load" />
        <el-button @click="load">搜索</el-button>
        <span class="hint">封禁后账号立即无法登录，既有登录态同时失效</span>
      </div>
      <el-table :data="users" v-loading="loading">
        <el-table-column prop="id" label="#" width="60" />
        <el-table-column prop="username" label="用户名" min-width="160" />
        <el-table-column prop="email" label="邮箱" min-width="180" />
        <el-table-column label="角色" width="110">
          <template #default="{ row }">
            <el-tag :type="row.role === 1 ? 'warning' : 'info'" size="small">
              {{ row.role === 1 ? '教师' : '学生' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="状态" width="90">
          <template #default="{ row }">
            <el-tag :type="row.status === 1 ? 'danger' : 'success'" size="small">
              {{ row.status === 1 ? '已封禁' : '正常' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="260">
          <template #default="{ row }">
            <el-button link :type="row.role === 1 ? 'info' : 'warning'" @click="setRole(row)">
              {{ row.role === 1 ? '降为学生' : '提为教师' }}
            </el-button>
            <el-button link :type="row.status === 1 ? 'success' : 'danger'" @click="toggleBan(row)">
              {{ row.status === 1 ? '解封' : '封禁' }}
            </el-button>
            <el-button link type="primary" @click="resetPwd(row)">重置密码</el-button>
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
    </div>
  </div>
</template>

<script setup lang="ts">
// 用户管理（教师）：提权/降权、封禁/解封、重置密码
import { onMounted, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { pageAdminUsers, changeUserRole, changeUserStatus, resetUserPassword, type AdminUserItem } from '@/api/adminUser'

const users = ref<AdminUserItem[]>([])
const keyword = ref('')
const page = ref(1)
const size = ref(10)
const total = ref(0)
const loading = ref(false)

async function load() {
  loading.value = true
  try {
    const data = await pageAdminUsers({ keyword: keyword.value || undefined, page: page.value, size: size.value })
    users.value = data.records || []
    total.value = data.total
  } finally {
    loading.value = false
  }
}

async function setRole(row: AdminUserItem) {
  const target = row.role === 1 ? 0 : 1
  await changeUserRole(row.id, target)
  ElMessage.success(target === 1 ? '已提为教师' : '已降为学生')
  load()
}

async function toggleBan(row: AdminUserItem) {
  const banning = row.status !== 1
  await ElMessageBox.confirm(
    banning ? `确认封禁「${row.username}」？其登录态将立即失效。` : `确认解封「${row.username}」？`,
    '提示', { type: 'warning' })
  await changeUserStatus(row.id, banning ? 1 : 0)
  ElMessage.success(banning ? '已封禁' : '已解封')
  load()
}

async function resetPwd(row: AdminUserItem) {
  await ElMessageBox.confirm(`确认重置「${row.username}」的密码？`, '提示', { type: 'warning' })
  const temp = await resetUserPassword(row.id)
  ElMessageBox.alert(`临时密码：${temp}（请转告该学生，登录后可在个人中心修改）`, '重置成功')
  load()
}

onMounted(load)
</script>

<style scoped>
.toolbar {
  display: flex;
  align-items: center;
  gap: 12px;
  margin-bottom: 16px;
}
.hint {
  color: #909399;
  font-size: 12px;
  margin-left: auto;
}
</style>
