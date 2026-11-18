<template>
  <el-container class="layout">
    <el-header class="header">
      <div class="header-inner">
        <!-- P2-17：原来挂在 <div> 上的 @click 键盘不可达，改成真正的 <router-link> -->
        <router-link to="/problems" class="logo" aria-label="CodeOJ 首页">
          <svg class="logo-icon" viewBox="0 0 24 24" width="22" height="22" fill="none" aria-hidden="true">
            <path d="M8 6 L3 12 L8 18" stroke="currentColor" stroke-width="2.4" stroke-linecap="round" stroke-linejoin="round"/>
            <path d="M16 6 L21 12 L16 18" stroke="currentColor" stroke-width="2.4" stroke-linecap="round" stroke-linejoin="round"/>
            <path d="M13.5 4 L10.5 20" stroke="currentColor" stroke-width="2.2" stroke-linecap="round"/>
          </svg>
          <span>Code<em>OJ</em></span>
        </router-link>

        <nav aria-label="主导航" class="nav">
          <el-menu mode="horizontal" :default-active="activeMenu" router :ellipsis="false" class="menu">
            <el-menu-item index="/problems">题目</el-menu-item>
            <el-menu-item v-if="userStore.isLoggedIn" index="/submissions">提交记录</el-menu-item>
            <el-menu-item v-if="userStore.isLoggedIn" index="/statistics">统计</el-menu-item>
            <el-menu-item index="/contests">竞赛</el-menu-item>
            <el-menu-item index="/leaderboard">排行榜</el-menu-item>
            <el-sub-menu v-if="userStore.isAdmin" index="/admin" popper-class="admin-menu-popper">
              <template #title>
                <svg class="menu-icon" viewBox="0 0 24 24" width="14" height="14" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round" aria-hidden="true">
                  <path d="M12 2 L21 7 L21 17 L12 22 L3 17 L3 7 Z" /><path d="M12 22 L12 12" /><path d="M21 7 L12 12 L3 7" />
                </svg>
                管理
              </template>
              <!-- 5 个管理项收进二级菜单：一级导航从 10 项降到 6 项，窄屏不再挤压 -->
              <el-menu-item index="/admin/problems">题目管理</el-menu-item>
              <el-menu-item index="/admin/contests">竞赛管理</el-menu-item>
              <el-menu-item index="/admin/courses">课程实验</el-menu-item>
              <el-menu-item index="/admin/users">用户管理</el-menu-item>
              <el-menu-item index="/admin/queue">队列监控</el-menu-item>
            </el-sub-menu>
          </el-menu>
        </nav>

        <div class="user-area">
          <template v-if="userStore.isLoggedIn">
            <el-dropdown trigger="click" @command="handleCommand">
              <button class="username" type="button" aria-haspopup="menu">
                <span class="avatar" aria-hidden="true">{{ (userStore.username || '?').slice(0, 1).toUpperCase() }}</span>
                <span class="uname">{{ userStore.username }}</span>
                <span v-if="userStore.isAdmin" class="role-chip">管理员</span>
                <svg class="caret" viewBox="0 0 24 24" width="12" height="12" fill="none" stroke="currentColor" stroke-width="2.4" stroke-linecap="round" stroke-linejoin="round" aria-hidden="true">
                  <path d="M6 9 L12 15 L18 9" />
                </svg>
              </button>
              <template #dropdown>
                <el-dropdown-menu>
                  <el-dropdown-item command="profile">个人中心</el-dropdown-item>
                  <el-dropdown-item command="logout" divided>退出登录</el-dropdown-item>
                </el-dropdown-menu>
              </template>
            </el-dropdown>
          </template>
          <template v-else>
            <el-button text @click="$router.push('/login')">登录</el-button>
            <el-button type="primary" @click="$router.push('/register')">注册</el-button>
          </template>
        </div>
      </div>
    </el-header>
    <el-main class="main">
      <router-view v-slot="{ Component }">
        <transition name="page" mode="out-in">
          <component :is="Component" />
        </transition>
      </router-view>
    </el-main>
    <el-footer class="footer">
      <span>CodeOJ · 在线代码评测与编程练习平台</span>
      <span class="dot" aria-hidden="true">·</span>
      <span>Spring Boot 3 + Vue 3 + Docker</span>
    </el-footer>
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

/**
 * 当前高亮项。
 * 1) 管理端二级路由统一高亮到父级 /admin（菜单里只有这一个父项）；
 * 2) 其余按最长前缀匹配：/problems/12 要亮的是「题目」而不是什么都不亮——
 *    直接返回 route.path 时，详情页会让整条导航都没有 active 态。
 */
const NAV_ROOTS = ['/problems', '/submissions', '/statistics', '/contests', '/leaderboard']

const activeMenu = computed(() => {
  if (route.path.startsWith('/admin')) return '/admin'
  const root = NAV_ROOTS.find((p) => route.path === p || route.path.startsWith(`${p}/`))
  return root ?? route.path
})

onMounted(async () => {
  // 登录用户建立 WebSocket 连接（论文 5.5）
  if (userStore.isLoggedIn) {
    connectWs()
    // 校验登录态与角色：token 过期/被降权/被封禁时同步本地状态（失败静默，不打断页面）
    try {
      await userStore.refreshUserInfo()
    } catch {
      // 40100 由响应拦截器统一处理；其它错误忽略
    }
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
  min-height: 100%;
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
  gap: 28px;
  height: 100%;
  padding: 0 20px;
}
.logo {
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 20px;
  font-weight: 800;
  color: var(--oj-primary-deep);
  letter-spacing: 0.5px;
  flex-shrink: 0;
  transition: opacity var(--oj-duration) var(--oj-ease);
}
.logo:hover {
  opacity: 0.82;
}
.logo em {
  font-style: normal;
  color: var(--oj-primary);
}
.logo-icon {
  color: var(--oj-primary);
}
.nav {
  flex: 1;
  min-width: 0;
}
.menu {
  border-bottom: none;
  background: transparent;
}
.menu-icon {
  margin-right: 4px;
  vertical-align: -2px;
}
.user-area {
  display: flex;
  align-items: center;
  gap: 8px;
  flex-shrink: 0;
}
.username {
  display: inline-flex;
  align-items: center;
  gap: 8px;
  padding: 4px 10px 4px 4px;
  border: 1px solid transparent;
  border-radius: var(--oj-radius-pill);
  background: transparent;
  color: var(--oj-gray-800);
  font-family: inherit;
  font-size: 14px;
  font-weight: 600;
  cursor: pointer;
  transition: background var(--oj-duration) var(--oj-ease),
    border-color var(--oj-duration) var(--oj-ease);
}
.username:hover {
  background: var(--oj-primary-soft);
  border-color: var(--oj-border);
}
/* 首字母头像：比纯文本用户名更像"账号"，也与排行榜头像风格统一 */
.avatar {
  width: 28px;
  height: 28px;
  border-radius: 50%;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  background: var(--oj-gradient-brand);
  color: var(--oj-white);
  font-size: 13px;
  font-weight: 800;
  flex-shrink: 0;
}
.uname {
  max-width: 120px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}
.role-chip {
  padding: 1px 7px;
  border-radius: var(--oj-radius-pill);
  background: var(--oj-primary-soft);
  color: var(--oj-primary);
  font-size: 11px;
  font-weight: 700;
}
.caret {
  color: var(--oj-gray-400);
}
.main {
  padding: 0;
  flex: 1;
  min-height: 0;
}
.footer {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 8px;
  height: auto;
  padding: 20px 16px 28px;
  font-size: 12.5px;
  color: var(--oj-text-weak);
  background: transparent;
}
.footer .dot {
  opacity: 0.6;
}

/* 窄屏：导航允许横向滚动而不是挤成一团 */
@media (max-width: 860px) {
  .header-inner {
    gap: 12px;
  }
  .logo span {
    display: none;
  }
  .uname,
  .role-chip {
    display: none;
  }
}
</style>

<style>
/* 管理二级菜单弹层：EP 的 popper 挂在 body 上，scoped 选不中 */
.admin-menu-popper .el-menu--popup {
  min-width: 148px;
  border-radius: var(--oj-radius-md);
  padding: 6px;
}
.admin-menu-popper .el-menu-item {
  border-radius: var(--oj-radius-sm);
  height: 38px;
  line-height: 38px;
}
</style>
