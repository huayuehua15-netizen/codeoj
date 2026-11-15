<template>
  <el-container class="layout">
    <el-header class="header">
      <div class="header-inner">
        <div class="logo" @click="$router.push('/problems')">
          <svg class="logo-icon" viewBox="0 0 24 24" width="22" height="22" fill="none">
            <path d="M8 6 L3 12 L8 18" stroke="currentColor" stroke-width="2.4" stroke-linecap="round" stroke-linejoin="round"/>
            <path d="M16 6 L21 12 L16 18" stroke="currentColor" stroke-width="2.4" stroke-linecap="round" stroke-linejoin="round"/>
            <path d="M13.5 4 L10.5 20" stroke="currentColor" stroke-width="2.2" stroke-linecap="round"/>
          </svg>
          <span>Code<em>OJ</em></span>
        </div>
        <el-menu mode="horizontal" :default-active="activeMenu" router :ellipsis="false" class="menu">
          <el-menu-item index="/problems">题目</el-menu-item>
          <el-menu-item v-if="userStore.isLoggedIn" index="/submissions">提交记录</el-menu-item>
          <el-menu-item v-if="userStore.isLoggedIn" index="/statistics">统计</el-menu-item>
          <el-menu-item index="/contests">竞赛</el-menu-item>
          <el-menu-item index="/leaderboard">排行榜</el-menu-item>
          <template v-if="userStore.isAdmin">
            <el-menu-item index="/admin/problems">题目管理</el-menu-item>
            <el-menu-item index="/admin/contests">竞赛管理</el-menu-item>
            <el-menu-item index="/admin/users">用户管理</el-menu-item>
            <el-menu-item index="/admin/courses">课程实验</el-menu-item>
            <el-menu-item index="/admin/queue">队列监控</el-menu-item>
          </template>
        </el-menu>
        <div class="user-area">
          <template v-if="userStore.isLoggedIn">
            <el-dropdown trigger="click" @command="handleCommand">
              <span class="username">{{ userStore.username }}</span>
              <template #dropdown>
                <el-dropdown-menu>
                  <el-dropdown-item command="profile">个人中心</el-dropdown-item>
                  <el-dropdown-item command="logout" divided>退出登录</el-dropdown-item>
                </el-dropdown-menu>
              </template>
            </el-dropdown>
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

function handleCommand(command: string) {
  if (command === 'profile') {
    router.push('/profile')
  } else if (command === 'logout') {
    handleLogout()
  }
}

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
  background: rgba(255, 255, 255, 0.82);
  backdrop-filter: blur(14px) saturate(1.4);
  -webkit-backdrop-filter: blur(14px) saturate(1.4);
  padding: 0;
  border-bottom: 1px solid rgba(17, 24, 39, 0.06);
  box-shadow: 0 1px 2px rgba(17, 24, 39, 0.04);
  position: sticky;
  top: 0;
  z-index: 20;
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
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 20px;
  font-weight: 800;
  color: #3730a3;
  cursor: pointer;
  letter-spacing: 0.5px;
}
.logo em {
  font-style: normal;
  color: #4f46e5;
}
.logo-icon {
  color: #4f46e5;
}
.menu {
  flex: 1;
  border-bottom: none;
}
.username {
  margin-right: 8px;
  cursor: pointer;
  outline: none;
  font-weight: 600;
  color: #303133;
  display: inline-flex;
  align-items: center;
  gap: 6px;
  padding: 5px 12px;
  border-radius: 8px;
  transition: background 0.15s var(--oj-ease), color 0.15s var(--oj-ease);
}
/* 下拉箭头：明确提示可点击展开菜单 */
.username::after {
  content: '';
  display: inline-block;
  border-left: 4px solid transparent;
  border-right: 4px solid transparent;
  border-top: 5px solid #909399;
  transition: transform 0.2s var(--oj-ease), border-top-color 0.15s var(--oj-ease);
}
.username:hover {
  background: var(--oj-primary-soft);
  color: var(--oj-primary);
}
.username:hover::after {
  border-top-color: var(--oj-primary);
  transform: translateY(1px);
}
.main {
  padding: 0;
}
</style>
