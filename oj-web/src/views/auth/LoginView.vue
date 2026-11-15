<template>
  <div class="auth-page">
    <div class="auth-card card">
      <div class="auth-logo">Code<span>OJ</span></div>
      <p class="auth-sub">在线代码评测与编程练习平台</p>
      <h2 class="auth-title">欢迎回来</h2>
      <el-form :model="form" label-width="0" @keyup.enter="handleLogin">
        <el-form-item>
          <el-input v-model="form.username" placeholder="用户名（支持中文）" size="large" clearable />
        </el-form-item>
        <el-form-item>
          <el-input v-model="form.password" type="password" placeholder="密码" size="large" show-password />
        </el-form-item>
        <el-button type="primary" size="large" class="auth-btn" :loading="loading" @click="handleLogin">
          登 录
        </el-button>
        <div class="links">
          <router-link to="/register">还没有账号？立即注册 →</router-link>
        </div>
      </el-form>
    </div>
  </div>
</template>

<script setup lang="ts">
import { reactive, ref } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { ElMessage } from 'element-plus'
import { login } from '@/api/auth'
import { useUserStore } from '@/stores/user'
import { connectWs } from '@/api/ws'

const router = useRouter()
const route = useRoute()
const userStore = useUserStore()

// 注册成功跳转而来时预填用户名（只差密码即可登录）
const form = reactive({ username: (route.query.username as string) || '', password: '' })
const loading = ref(false)

async function handleLogin() {
  if (!form.username || !form.password) {
    ElMessage.warning('请输入用户名与密码')
    return
  }
  loading.value = true
  try {
    const data = await login(form)
    userStore.setLogin(data)
    connectWs()
    ElMessage.success('登录成功')
    router.push((route.query.redirect as string) || '/problems')
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
    radial-gradient(900px 500px at 100% 100%, rgba(139, 92, 246, 0.25), transparent),
    linear-gradient(135deg, #1e1b4b 0%, #4338ca 45%, #6366f1 100%);
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
  color: #3730a3;
}
.auth-logo span {
  color: #4f46e5;
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
  color: #4f46e5;
  font-size: 13px;
}
.links a:hover {
  opacity: 0.85;
}
</style>
