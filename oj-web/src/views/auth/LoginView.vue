<template>
  <AuthShell mode="login" title="欢迎回来" subtitle="登录后即可提交代码，判题结果会实时推送到当前页面。">
    <el-form :model="form" label-width="0" @keyup.enter="handleLogin">
      <el-form-item>
        <el-input
          v-model="form.username"
          placeholder="用户名"
          size="large"
          clearable
          autocomplete="username"
        />
      </el-form-item>
      <el-form-item>
        <el-input
          v-model="form.password"
          type="password"
          placeholder="密码"
          size="large"
          show-password
          autocomplete="current-password"
        />
      </el-form-item>
      <el-button type="primary" size="large" class="auth-btn" :loading="loading" @click="handleLogin">
        登 录
      </el-button>
      <p class="auth-links">
        还没有账号？<router-link to="/register">立即注册 →</router-link>
      </p>
    </el-form>
  </AuthShell>
</template>

<script setup lang="ts">
import { reactive, ref } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { ElMessage } from 'element-plus'
import { login } from '@/api/auth'
import { useUserStore } from '@/stores/user'
import { connectWs } from '@/api/ws'
import AuthShell from '@/components/AuthShell.vue'

const router = useRouter()
const route = useRoute()
const userStore = useUserStore()

// 注册成功跳转而来时预填用户名（只差密码即可登录）
const form = reactive({ username: (route.query.username as string) || '', password: '' })
const loading = ref(false)

/**
 * 开放重定向防护：redirect 是 query 传入的任意字符串。
 * 只放行站内路径（必须以单个 / 开头），拦掉 https://、//evil.com、反斜杠等外站跳转。
 */
function safeRedirect(raw: unknown): string {
  const value = typeof raw === 'string' ? raw : ''
  return value.startsWith('/') && !value.startsWith('//') && !value.includes('\\') ? value : '/problems'
}

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
    router.push(safeRedirect(route.query.redirect))
  } finally {
    loading.value = false
  }
}
</script>
