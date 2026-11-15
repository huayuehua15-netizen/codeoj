<template>
  <div class="auth-page">
    <div class="card auth-card">
      <h2>注册 CodeOJ</h2>
      <el-form :model="form" label-width="0">
        <el-form-item>
          <el-input v-model="form.username" placeholder="用户名（支持中文/字母/数字/下划线）" size="large" />
        </el-form-item>
        <el-form-item>
          <el-input v-model="form.email" placeholder="邮箱（选填）" size="large" />
        </el-form-item>
        <el-form-item>
          <el-input v-model="form.password" type="password" placeholder="密码（6-32 位）" size="large" show-password />
        </el-form-item>
        <el-button type="primary" size="large" style="width: 100%" :loading="loading" @click="handleRegister">
          注册
        </el-button>
        <div class="links">
          <router-link to="/login">已有账号？去登录</router-link>
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
    ElMessage.success('注册成功')
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
}
.auth-card {
  width: 360px;
}
.links {
  margin-top: 12px;
  text-align: center;
}
.links a {
  color: #409eff;
  font-size: 13px;
}
</style>
