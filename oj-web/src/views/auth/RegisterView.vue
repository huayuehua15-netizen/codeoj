<template>
  <AuthShell mode="register" title="创建账号" subtitle="注册后即可提交代码。判题结果实时推送，无需手动刷新。">
    <el-form :model="form" label-width="0" @keyup.enter="handleRegister">
      <el-form-item>
        <el-input
          v-model="form.username"
          placeholder="用户名（支持中文/字母/数字/下划线）"
          size="large"
          clearable
          autocomplete="username"
        />
      </el-form-item>
      <el-form-item>
        <el-input
          v-model="form.email"
          placeholder="邮箱（选填，用于找回密码）"
          size="large"
          clearable
          autocomplete="email"
        />
      </el-form-item>
      <el-form-item>
        <el-input
          v-model="form.password"
          type="password"
          placeholder="密码（6-32 位）"
          size="large"
          show-password
          autocomplete="new-password"
        />
      </el-form-item>
      <el-button type="primary" size="large" class="auth-btn" :loading="loading" @click="handleRegister">
        注 册
      </el-button>
      <p class="auth-links">
        已有账号？<router-link to="/login">直接登录 →</router-link>
      </p>
    </el-form>
  </AuthShell>
</template>

<script setup lang="ts">
import { reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { register } from '@/api/auth'
import AuthShell from '@/components/AuthShell.vue'

const router = useRouter()

const form = reactive({ username: '', email: '', password: '' })
const loading = ref(false)

async function handleRegister() {
  if (!form.username || !form.password) {
    ElMessage.warning('请输入用户名与密码')
    return
  }
  if (form.password.length < 6 || form.password.length > 32) {
    ElMessage.warning('密码长度需在 6-32 位之间')
    return
  }
  loading.value = true
  try {
    await register(form)
    ElMessage.success('注册成功，请登录')
    // 注册不自动登录：携带用户名跳转登录页，用户输入密码完成登录
    router.push({ name: 'login', query: { username: form.username } })
  } finally {
    loading.value = false
  }
}
</script>
