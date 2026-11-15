<template>
  <el-container class="layout">
    <el-header class="header">
      <div class="header-inner">
        <div class="logo" @click="$router.push('/problems')">CodeOJ</div>
        <el-menu mode="horizontal" :default-active="activeMenu" router :ellipsis="false" class="menu">
          <el-menu-item index="/problems">题目</el-menu-item>
          <el-menu-item v-if="userStore.isLoggedIn" index="/submissions">提交记录</el-menu-item>
          <el-menu-item v-if="userStore.isLoggedIn" index="/statistics">统计</el-menu-item>
          <el-menu-item index="/leaderboard">排行榜</el-menu-item>
          <template v-if="userStore.isAdmin">
            <el-menu-item index="/admin/problems">题目管理</el-menu-item>
            <el-menu-item index="/admin/courses">课程实验</el-menu-item>
            <el-menu-item index="/admin/queue">队列监控</el-menu-item>
          </template>
        </el-menu>
        <div class="user-area">
          <template v-if="userStore.isLoggedIn">
            <span class="nickname">{{ userStore.username }}</span>
            <el-button link type="danger" @click="handleLogout">退出</el-button>
          </template>
          <template v-else>
            <el-button link type="primary" @click="$router.push('/login')">登录</el-button>
            <el-button link type="primary" @click="$router.push('/register')">注册</el-button>
          </template>
        </div>
      </div>
    </el-header>
    <el-main class="main">
      <router-view />
    </el-main>
  </el-container>
</template>

<script setup lang="ts">
import { computed, onMounted, onUnmounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { useUserStore } from '@/stores/user'
import { logout } from '@/api/auth'
import { connectWs, disconnectWs } from '@/api/ws'

const route = useRoute()
const router = useRouter()
const userStore = useUserStore()

const activeMenu = computed(() => route.path)

onMounted(() => {
  // 登录用户建立 WebSocket 连接（论文 5.5）
  if (userStore.isLoggedIn) {
    connectWs()
  }
})

onUnmounted(() => {
  disconnectWs()
})

async function handleLogout() {
  try {
    await logout()
  } finally {
    userStore.logout()
    disconnectWs()
    ElMessage.success('已退出登录')
    router.push('/problems')
  }
}
</script>

<style scoped>
.layout {
  height: 100%;
}
.header {
  background: #fff;
  border-bottom: 1px solid #e4e7ed;
  padding: 0;
}
.header-inner {
  max-width: 1200px;
  margin: 0 auto;
  display: flex;
  align-items: center;
  gap: 24px;
  height: 100%;
}
.logo {
  font-size: 20px;
  font-weight: 700;
  color: #409eff;
  cursor: pointer;
}
.menu {
  flex: 1;
  border-bottom: none;
}
.nickname {
  margin-right: 8px;
}
.main {
  padding: 0;
}
</style>
