<template>
  <div class="auth-page">
    <div class="auth-card card">
      <div class="auth-logo">Code<span>OJ</span></div>
      <p class="auth-sub">在线代码评测与编程练习平台</p>
      <h2 class="auth-title">创建账号</h2>
      <el-form :model="form" label-width="0" @keyup.enter="handleRegister">
        <el-form-item>
          <el-input v-model="form.username" placeholder="用户名（支持中文/字母/数字/下划线）" size="large" clearable />
        </el-form-item>
        <el-form-item>
          <el-input v-model="form.email" placeholder="邮箱（选填）" size="large" clearable />
        </el-form-item>
        <el-form-item>
          <el-input v-model="form.password" type="password" placeholder="密码（6-32 位）" size="large" show-password />
        </el-form-item>
        <el-button type="primary" size="large" class="auth-btn" :loading="loading" @click="handleRegister">
          注 册
        </el-button>
        <div class="links">
          <router-link to="/login">已有账号？直接登录 →</router-link>
        </div>
      </el-form>
    </div>
  </div>
</template>

<script setup lang="ts">
import { reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { register } from '@/api/auth'
import { useUserStore } from '@/stores/user'
import { connectWs } from '@/api/ws'

const router = useRouter()
const userStore = useUserStore()

const form = reactive({ username: '', email: '', password: '' })
const loading = ref(false)

async function handleRegister() {
  if (!form.username || !form.password) {
    ElMessage.warning('请输入用户名与密码')
    return
  }
  loading.value = true
  try {
    const data = await register(form)
    userStore.setLogin(data)
    connectWs()
    ElMessage.success('注册成功，欢迎加入 CodeOJ')
    router.push('/problems')
  } finally {
    loading.value = false
  }
}
</script>

<style scoped>
.auth-page {
  min-height: 100vh;
  display: flex;
  align-items: center;
  justify-content: center;
  background:
    radial-gradient(1000px 500px at 10% -10%, rgba(59, 130, 246, 0.35), transparent),
    radial-gradient(900px 500px at 100% 100%, rgba(14, 165, 233, 0.28), transparent),
    linear-gradient(135deg, #0f2757 0%, #1e3a8a 45%, #2563eb 100%);
}
.auth-card {
  width: 400px;
  padding: 36px 32px;
  border-radius: 14px;
}
.auth-logo {
  font-size: 30px;
  font-weight: 800;
  letter-spacing: 1px;
  color: #1e3a8a;
}
.auth-logo span {
  color: #2563eb;
}
.auth-sub {
  color: #909399;
  font-size: 12px;
  margin: 6px 0 22px;
}
.auth-title {
  font-size: 18px;
  margin: 0 0 16px;
  color: #303133;
}
.auth-btn {
  width: 100%;
  margin-top: 4px;
  letter-spacing: 4px;
}
.links {
  margin-top: 16px;
  text-align: center;
}
.links a {
  color: #2563eb;
  font-size: 13px;
}
.links a:hover {
  opacity: 0.85;
}
</style>
