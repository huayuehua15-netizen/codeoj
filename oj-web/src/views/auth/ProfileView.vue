<template>
  <div class="page-container profile-page">
    <div class="card">
      <div class="profile-head">
        <span class="avatar">{{ (userStore.username || '?').slice(0, 1) }}</span>
        <div>
          <h2 style="margin: 0">{{ userStore.username }}</h2>
          <p class="sub">{{ userStore.isAdmin ? '管理员（教师）' : '普通用户（学生）' }}</p>
        </div>
      </div>

      <!-- 资料查看 -->
      <el-descriptions :column="2" border style="margin: 16px 0">
        <el-descriptions-item label="用户名">{{ userStore.username }}</el-descriptions-item>
        <el-descriptions-item label="角色">{{ userStore.isAdmin ? '管理员（教师）' : '普通用户（学生）' }}</el-descriptions-item>
        <el-descriptions-item label="邮箱">{{ info?.email || '未设置' }}</el-descriptions-item>
        <el-descriptions-item label="注册时间">{{ info?.createdAt || '-' }}</el-descriptions-item>
      </el-descriptions>

      <!-- 我的收藏 -->
      <h3>我的收藏</h3>
      <el-table :data="favorites" v-loading="favLoading" size="small">
        <el-table-column label="题目" min-width="240">
          <template #default="{ row }">
            <router-link :to="`/problems/${row.problemId}`">{{ row.title }}</router-link>
          </template>
        </el-table-column>
        <el-table-column label="难度" width="90">
          <template #default="{ row }">
            <el-tag :color="({1:'#67c23a',2:'#e6a23c',3:'#f56c6c'} as Record<number,string>)[row.difficulty]" size="small">
              {{ ({1:'简单',2:'中等',3:'困难'} as Record<number,string>)[row.difficulty] || '?' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="favoritedAt" label="收藏时间" width="180" />
      </el-table>

      <el-divider />

      <!-- 邮箱修改 -->
      <h3>修改邮箱</h3>
      <el-form :model="emailForm" label-width="80px" style="max-width: 420px">
        <el-form-item label="新邮箱">
          <el-input v-model="emailForm.email" placeholder="example@example.com" />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" :loading="savingEmail" @click="saveEmail">保存邮箱</el-button>
        </el-form-item>
      </el-form>

      <el-divider />

      <!-- 密码修改 -->
      <h3>修改密码</h3>
      <el-form :model="pwdForm" label-width="80px" style="max-width: 420px">
        <el-form-item label="原密码">
          <el-input v-model="pwdForm.oldPassword" type="password" show-password />
        </el-form-item>
        <el-form-item label="新密码">
          <el-input v-model="pwdForm.newPassword" type="password" show-password placeholder="6-32 位" />
        </el-form-item>
        <el-form-item label="确认密码">
          <el-input v-model="pwdForm.confirm" type="password" show-password />
        </el-form-item>
        <el-form-item>
          <el-button type="warning" :loading="savingPwd" @click="savePassword">修改密码</el-button>
        </el-form-item>
      </el-form>
      <p class="tip">修改密码成功后当前登录状态将失效，需重新登录。</p>
    </div>
  </div>
</template>

<script setup lang="ts">
// 个人中心（论文表 3-1：个人信息维护）
import { onMounted, reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { changePassword, getUserInfo, updateEmail, type LoginVO } from '@/api/auth'
import { myFavorites } from '@/api/favorite'
import { useUserStore } from '@/stores/user'
import { disconnectWs } from '@/api/ws'

const router = useRouter()
const userStore = useUserStore()

const info = ref<LoginVO>()
const favorites = ref<{ problemId: number; title: string; difficulty: number; favoritedAt: string }[]>([])
const favLoading = ref(false)
const emailForm = reactive({ email: '' })
const pwdForm = reactive({ oldPassword: '', newPassword: '', confirm: '' })
const savingEmail = ref(false)
const savingPwd = ref(false)

onMounted(async () => {
  info.value = await getUserInfo()
  emailForm.email = info.value.email || ''
  favLoading.value = true
  try {
    favorites.value = await myFavorites()
  } finally {
    favLoading.value = false
  }
})

async function saveEmail() {
  if (!emailForm.email) {
    ElMessage.warning('请输入邮箱')
    return
  }
  savingEmail.value = true
  try {
    await updateEmail(emailForm.email)
    ElMessage.success('邮箱已更新')
    info.value = await getUserInfo()
  } finally {
    savingEmail.value = false
  }
}

async function savePassword() {
  if (!pwdForm.oldPassword || !pwdForm.newPassword) {
    ElMessage.warning('请填写原密码与新密码')
    return
  }
  if (pwdForm.newPassword !== pwdForm.confirm) {
    ElMessage.warning('两次输入的新密码不一致')
    return
  }
  savingPwd.value = true
  try {
    await changePassword(pwdForm.oldPassword, pwdForm.newPassword)
    ElMessage.success('密码已修改，请重新登录')
    userStore.logout()
    disconnectWs()
    router.push('/login')
  } finally {
    savingPwd.value = false
  }
}
</script>

<style scoped>
.profile-page {
  max-width: 760px;
  margin: 0 auto;
}
.profile-head {
  display: flex;
  align-items: center;
  gap: 14px;
  padding-bottom: 16px;
  border-bottom: 1px solid #f0f0f0;
}
.avatar {
  width: 52px;
  height: 52px;
  border-radius: 50%;
  background: linear-gradient(135deg, #1e3a8a, #2563eb);
  color: #fff;
  font-size: 22px;
  font-weight: 700;
  display: inline-flex;
  align-items: center;
  justify-content: center;
}
.sub {
  color: #909399;
  font-size: 13px;
  margin: 4px 0 0;
}
.tip {
  color: #909399;
  font-size: 12px;
  margin-top: 4px;
}
</style>
