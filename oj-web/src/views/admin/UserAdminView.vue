<template>
  <AdminShell
    title="用户管理"
    section="USERS · 账号与权限"
    description="调整用户角色、封禁或解封账号，以及重置密码。"
  >
    <template #actions>
      <el-input v-model="keyword" placeholder="按用户名搜索" style="width: 210px" clearable @keyup.enter="search" @clear="search" />
      <el-button @click="search">搜索</el-button>
    </template>

    <p class="admin-hint">封禁后账号立即无法登录，既有登录态同时失效。</p>

    <div class="admin-table-shell">
      <el-table :data="users" v-loading="loading" empty-text="没有匹配的用户，试试换个用户名">
        <el-table-column prop="id" label="ID" width="60">
          <template #default="{ row }">
            <span class="mono id-cell">{{ String(row.id).padStart(3, '0') }}</span>
          </template>
        </el-table-column>
        <el-table-column prop="username" label="用户名" min-width="160" />
        <el-table-column prop="email" label="邮箱" min-width="180">
          <template #default="{ row }">
            <span class="email-cell">{{ row.email || '—' }}</span>
          </template>
        </el-table-column>
        <el-table-column label="角色" width="100">
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
        <el-table-column label="注册时间" width="170">
          <template #default="{ row }">
            <span class="mono time-cell">{{ formatDateTime(row.createdAt) }}</span>
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
        class="admin-pager"
        @current-change="load"
      />
    </div>
  </AdminShell>
</template>

<script setup lang="ts">
// 用户管理（教师）：提权/降权、封禁/解封、重置密码
import { onMounted, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { pageAdminUsers, changeUserRole, changeUserStatus, resetUserPassword, type AdminUserItem } from '@/api/adminUser'
import { formatDateTime } from '@/utils/datetime'
import AdminShell from '@/components/AdminShell.vue'

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
  } catch {
    // 拦截器已提示
  } finally {
    loading.value = false
  }
}

/** 搜索/清空从第 1 页开始：停留在末页时输入新关键词会看到假空表 */
function search() {
  page.value = 1
  load()
}

async function setRole(row: AdminUserItem) {
  const target = row.role === 1 ? 0 : 1
  // 提权/降权立即改变别人的能力边界，与其余敏感操作同口径先确认
  const action = target === 1 ? `确认为教师` : `降为学生`
  try {
    await ElMessageBox.confirm(`确认将「${row.username}」${action}？`, '提示', { type: 'warning' })
  } catch {
    return
  }
  try {
    await changeUserRole(row.id, target)
    ElMessage.success(target === 1 ? '已提为教师' : '已降为学生')
    load()
  } catch {
    // 拦截器已提示
  }
}

async function toggleBan(row: AdminUserItem) {
  const banning = row.status !== 1
  // 取消会让 confirm reject，吞掉它避免 unhandled rejection；确认后的封禁/解封逻辑不变
  try {
    await ElMessageBox.confirm(
      banning ? `确认封禁「${row.username}」？其登录态将立即失效。` : `确认解封「${row.username}」？`,
      '提示', { type: 'warning' })
  } catch {
    return
  }
  await changeUserStatus(row.id, banning ? 1 : 0)
  ElMessage.success(banning ? '已封禁' : '已解封')
  load()
}

async function resetPwd(row: AdminUserItem) {
  // 取消会让 confirm reject，吞掉它避免 unhandled rejection；确认后的重置逻辑不变
  try {
    await ElMessageBox.confirm(`确认重置「${row.username}」的密码？`, '提示', { type: 'warning' })
  } catch {
    return
  }
  const temp = await resetUserPassword(row.id)
  ElMessageBox.alert(`临时密码：${temp}（请转告该学生，登录后可在个人中心修改）`, '重置成功')
  load()
}

onMounted(load)
</script>

<style scoped>
.admin-hint {
  margin: 0 0 3px;
  font-size: 10.5px;
  color: var(--oj-text-weak);
}
.id-cell {
  font-size: 11px;
  color: var(--oj-text-weak);
}
.email-cell {
  font-family: var(--oj-font-mono);
  font-size: 11px;
  color: var(--oj-text-sub);
}
.time-cell {
  font-size: 10px;
  color: var(--oj-text-sub);
}
.admin-pager {
  margin: 0;
  padding: 10px 16px;
  justify-content: flex-end;
}
</style>
