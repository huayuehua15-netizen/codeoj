<template>
  <div class="layout">
    <header class="topbar">
      <div class="topbar-inner">
        <router-link to="/home" class="brand" aria-label="CodeOJ 首页">
          <span class="brand-mark" aria-hidden="true">&lt;/&gt;</span>
          <span class="brand-word">Code<span>OJ</span></span>
          <span class="brand-sub" aria-hidden="true">ONLINE<br />JUDGE</span>
        </router-link>

        <nav aria-label="主导航" class="nav">
          <router-link
            v-for="item in navItems"
            :key="item.to"
            :to="item.to"
            class="nav-link"
            :class="{ active: activeMenu === item.to }"
          >
            {{ item.label }}
          </router-link>

          <el-dropdown v-if="userStore.isAdmin" trigger="click" @command="goAdmin">
            <button class="nav-link admin-trigger" type="button" :class="{ active: activeMenu === '/admin' }">
              <span>管理</span>
              <svg viewBox="0 0 24 24" width="10" height="10" fill="none" stroke="currentColor" stroke-width="2.2" stroke-linecap="round" stroke-linejoin="round" aria-hidden="true">
                <path d="M6 9 L12 15 L18 9" />
              </svg>
            </button>
            <template #dropdown>
              <el-dropdown-menu>
                <el-dropdown-item command="/admin/problems">题目管理</el-dropdown-item>
                <el-dropdown-item command="/admin/contests">竞赛管理</el-dropdown-item>
                <el-dropdown-item command="/admin/courses">课程实验</el-dropdown-item>
                <el-dropdown-item command="/admin/users">用户管理</el-dropdown-item>
                <el-dropdown-item command="/admin/queue">队列监控</el-dropdown-item>
              </el-dropdown-menu>
            </template>
          </el-dropdown>
        </nav>

        <div class="user-area">
          <button type="button" class="palette-trigger" aria-label="打开命令面板（Ctrl+K）" @click="openPalette">
            <svg viewBox="0 0 24 24" width="13" height="13" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" aria-hidden="true">
              <circle cx="11" cy="11" r="7" /><path d="M20 20 L16.5 16.5" />
            </svg>
            <span class="palette-kbd">Ctrl K</span>
          </button>
          <template v-if="userStore.isLoggedIn">
            <el-dropdown trigger="click" @command="handleCommand">
              <button class="username" type="button" aria-haspopup="menu">
                <img v-if="userStore.avatar" class="avatar avatar-img" :src="userStore.avatar" alt="" aria-hidden="true" />
                <span v-else class="avatar" aria-hidden="true">{{ (userStore.username || '?').slice(0, 1).toUpperCase() }}</span>
                <span class="uname">{{ userStore.username }}</span>
                <span v-if="userStore.isAdmin" class="role-chip">ADMIN</span>
                <svg class="caret" viewBox="0 0 24 24" width="11" height="11" fill="none" stroke="currentColor" stroke-width="2.2" stroke-linecap="round" stroke-linejoin="round" aria-hidden="true">
                  <path d="M6 9 L12 15 L18 9" />
                </svg>
              </button>
              <template #dropdown>
                <el-dropdown-menu>
                  <el-dropdown-item command="profile">个人中心</el-dropdown-item>
                  <el-dropdown-item command="settings">账号设置</el-dropdown-item>
                  <el-dropdown-item command="logout" divided>退出登录</el-dropdown-item>
                </el-dropdown-menu>
              </template>
            </el-dropdown>
          </template>
          <template v-else>
            <router-link class="auth-link" to="/login">登录</router-link>
            <router-link class="auth-cta" to="/register">注册</router-link>
          </template>
        </div>
      </div>
    </header>

    <main class="main">
      <!-- 页面切换用 CSS 动画而不是 <transition>：题目详情等页面在异步加载后会切换根节点，
           Vue Transition 会把 page-enter-from 留在新根节点上，导致整页 opacity: 0 一直不消失。 -->
      <div :key="route.path" class="page-fade">
        <router-view />
      </div>
    </main>

    <footer class="footer">
      <div class="footer-inner">
        <div class="foot-brand">
          <span class="foot-mark" aria-hidden="true">&lt;/&gt;</span>
          <div>
            <p class="foot-name">CodeOJ</p>
            <p class="foot-meta">WRITE CODE. GET VERDICT.</p>
          </div>
        </div>
        <nav class="foot-links" aria-label="页脚导航">
          <router-link to="/about">使用说明</router-link>
          <router-link to="/announcements">公告</router-link>
          <router-link to="/leaderboard">排行榜</router-link>
        </nav>
        <p class="foot-colophon">
          Spring Boot 3 · Vue 3 · Docker<br />
          <span>ONLINE JUDGE / v1.0</span>
        </p>
      </div>
    </footer>

    <CommandPalette ref="palette" />
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, onUnmounted, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { useUserStore } from '@/stores/user'
import { logout } from '@/api/auth'
import { connectWs, disconnectWs } from '@/api/ws'
import CommandPalette from '@/components/CommandPalette.vue'

const route = useRoute()
const router = useRouter()
const userStore = useUserStore()
const palette = ref<InstanceType<typeof CommandPalette>>()

function openPalette() {
  palette.value?.openPalette()
}

const NAV_ROOTS = ['/home', '/problems', '/submissions', '/wrongbook', '/statistics', '/contests', '/leaderboard', '/announcements']

/** 主导航：不编号，不用目录感；只保留清楚的功能入口 */
const navItems = computed(() => {
  const base = [
    { to: '/home', label: '首页' },
    { to: '/problems', label: '题库' },
    { to: '/submissions', label: '提交记录', auth: true },
    { to: '/wrongbook', label: '错题本', auth: true },
    { to: '/statistics', label: '统计', auth: true },
    { to: '/contests', label: '竞赛' },
    { to: '/leaderboard', label: '排行榜' },
    { to: '/announcements', label: '公告' },
  ]
  return base.filter((item) => !item.auth || userStore.isLoggedIn)
})

const activeMenu = computed(() => {
  if (route.path.startsWith('/admin')) return '/admin'
  const root = NAV_ROOTS.find((p) => route.path === p || route.path.startsWith(`${p}/`))
  return root ?? route.path
})

onMounted(async () => {
  if (userStore.isLoggedIn) {
    connectWs()
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

function goAdmin(path: string) {
  router.push(path)
}

function handleCommand(command: string) {
  if (command === 'profile') {
    router.push('/profile')
  } else if (command === 'settings') {
    router.push('/settings')
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
  min-height: 100%;
  display: flex;
  flex-direction: column;
}

/* ---------- 顶栏：一条干净的工具栏，不是报头 ---------- */
.topbar {
  position: sticky;
  top: 0;
  z-index: 30;
  background: rgba(255, 255, 255, 0.92);
  border-bottom: 1px solid var(--oj-border);
  backdrop-filter: blur(12px);
}
.topbar-inner {
  max-width: 1280px;
  height: 62px;
  margin: 0 auto;
  padding: 0 32px;
  display: flex;
  align-items: center;
  gap: 34px;
}

.brand {
  display: flex;
  align-items: center;
  gap: 10px;
  flex-shrink: 0;
}
.brand-mark {
  width: 30px;
  height: 30px;
  border-radius: 7px;
  background: var(--oj-gray-900);
  color: #fff;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  font-family: var(--oj-font-mono);
  font-size: 11px;
  font-weight: 700;
  letter-spacing: -0.06em;
  line-height: 1;
  user-select: none;
}
.brand-word {
  font-size: 18px;
  font-weight: 750;
  letter-spacing: -0.035em;
  color: var(--oj-text);
  line-height: 1;
}
.brand-word span {
  color: var(--oj-primary);
}
.brand-sub {
  margin-left: 2px;
  padding-left: 11px;
  border-left: 1px solid var(--oj-border-strong);
  font-family: var(--oj-font-mono);
  font-size: 8.5px;
  line-height: 1.45;
  letter-spacing: 0.1em;
  color: var(--oj-text-weak);
}

.nav {
  flex: 1;
  min-width: 0;
  display: flex;
  align-items: center;
  gap: 2px;
  overflow-x: auto;
  scrollbar-width: none;
}
.nav::-webkit-scrollbar {
  display: none;
}
.nav-link {
  display: inline-flex;
  align-items: center;
  gap: 5px;
  padding: 7px 11px;
  border: 0;
  border-radius: 6px;
  background: transparent;
  font-family: inherit;
  font-size: 12.5px;
  font-weight: 500;
  color: var(--oj-text-sub);
  white-space: nowrap;
  cursor: pointer;
  transition: color var(--oj-duration) var(--oj-ease), background var(--oj-duration) var(--oj-ease);
}
.nav-link:hover {
  color: var(--oj-text);
  background: var(--oj-gray-100);
}
.nav-link.active {
  color: var(--oj-text);
  background: var(--oj-gray-100);
  font-weight: 650;
}
.admin-trigger svg {
  opacity: 0.55;
}

.user-area {
  display: flex;
  align-items: center;
  gap: 14px;
  flex-shrink: 0;
}
.auth-link {
  font-size: 12.5px;
  font-weight: 500;
  color: var(--oj-text-sub);
  padding: 6px 2px;
}
.auth-link:hover {
  color: var(--oj-text);
}
/* 命令面板入口：细边幽灵按钮 + 等宽键位徽标 */
.palette-trigger {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  padding: 5px 9px;
  border: 1px solid var(--oj-border);
  border-radius: 6px;
  background: var(--oj-gray-50);
  color: var(--oj-text-sub);
  font-family: inherit;
  cursor: pointer;
  transition: color var(--oj-duration) var(--oj-ease), border-color var(--oj-duration) var(--oj-ease);
}
.palette-trigger:hover {
  color: var(--oj-text);
  border-color: var(--oj-border-strong);
}
.palette-kbd {
  font-family: var(--oj-font-mono);
  font-size: 8.5px;
  letter-spacing: 0.05em;
  color: var(--oj-text-weak);
  border: 1px solid var(--oj-border);
  border-radius: 3px;
  padding: 1px 4px;
  background: #fff;
}
.auth-cta {
  padding: 8px 15px;
  border-radius: 6px;
  background: var(--oj-gray-900);
  color: #fff;
  font-size: 12.5px;
  font-weight: 650;
  transition: background var(--oj-duration) var(--oj-ease);
}
.auth-cta:hover {
  background: var(--oj-primary);
}

.username {
  display: inline-flex;
  align-items: center;
  gap: 8px;
  padding: 3px 0 3px 3px;
  border: 0;
  background: transparent;
  color: var(--oj-gray-700);
  font-family: inherit;
  font-size: 12.5px;
  font-weight: 500;
  cursor: pointer;
}
.avatar {
  width: 27px;
  height: 27px;
  border-radius: 6px;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  background: var(--oj-primary-soft);
  color: var(--oj-primary);
  font-size: 12px;
  font-weight: 700;
  flex-shrink: 0;
}
.avatar-img {
  object-fit: cover;
  border: 1px solid var(--oj-border);
}
.uname {
  max-width: 110px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}
.role-chip {
  font-family: var(--oj-font-mono);
  font-size: 8px;
  font-weight: 600;
  letter-spacing: 0.08em;
  color: var(--oj-primary);
  background: var(--oj-primary-soft);
  border: 1px solid var(--oj-info-border);
  padding: 0 4px;
  line-height: 15px;
  border-radius: 3px;
}
.caret {
  color: var(--oj-gray-400);
}

.main {
  flex: 1;
  min-height: 0;
}

/* ---------- 页脚 ---------- */
.footer {
  border-top: 1px solid var(--oj-border);
  background: var(--oj-card);
}
.footer-inner {
  max-width: 1280px;
  margin: 0 auto;
  padding: 22px 32px 24px;
  display: flex;
  align-items: center;
  gap: 28px;
  flex-wrap: wrap;
}
.foot-brand {
  display: flex;
  align-items: center;
  gap: 10px;
}
.foot-mark {
  width: 26px;
  height: 26px;
  border-radius: 6px;
  background: var(--oj-gray-900);
  color: #fff;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  font-family: var(--oj-font-mono);
  font-size: 10px;
  font-weight: 700;
  flex-shrink: 0;
}
.foot-name {
  margin: 0;
  font-size: 12.5px;
  font-weight: 700;
  letter-spacing: -0.02em;
  color: var(--oj-gray-800);
}
.foot-meta {
  margin: 2px 0 0;
  font-family: var(--oj-font-mono);
  font-size: 8px;
  letter-spacing: 0.1em;
  color: var(--oj-text-weak);
}
.foot-links {
  display: flex;
  gap: 20px;
  font-size: 11.5px;
  color: var(--oj-text-sub);
}
.foot-links a:hover {
  color: var(--oj-primary);
}
.foot-colophon {
  margin-left: auto;
  text-align: right;
  font-family: var(--oj-font-mono);
  font-size: 9px;
  line-height: 1.7;
  letter-spacing: 0.04em;
  color: var(--oj-text-weak);
}
.foot-colophon span {
  color: var(--oj-gray-400);
}

@media (max-width: 1080px) {
  .topbar-inner {
    gap: 18px;
    padding: 0 18px;
  }
  .brand-sub {
    display: none;
  }
  .uname,
  .role-chip,
  .palette-kbd {
    display: none;
  }
}
@media (max-width: 860px) {
  .brand-word {
    display: none;
  }
  .topbar-inner {
    height: auto;
    padding: 10px 18px 9px;
    flex-wrap: wrap;
    gap: 8px 14px;
  }
  .brand {
    order: 1;
  }
  .user-area {
    order: 2;
    margin-left: auto;
  }
  .nav {
    order: 3;
    flex: 1 0 100%;
    flex-wrap: wrap;
    gap: 2px;
    padding-top: 7px;
    border-top: 1px solid var(--oj-border);
    overflow: visible;
    -webkit-overflow-scrolling: touch;
    mask-image: none;
  }
  .nav-link {
    padding: 5px 9px;
    font-size: 12px;
  }
  .footer-inner {
    padding: 20px 18px 22px;
    gap: 16px;
  }
  .foot-colophon {
    margin-left: 0;
    text-align: left;
  }
}
</style>

<style>
/* 管理菜单弹层：挂在 body 上，scoped 选不中 */
.el-dropdown__popper.admin-menu-popper .el-dropdown-menu {
  padding: 4px;
}
.el-dropdown-menu__item {
  font-size: 12.5px;
  padding: 7px 14px;
}
.el-dropdown-menu__item:not(.is-disabled):hover {
  background: var(--oj-gray-100);
  color: var(--oj-primary);
}
</style>
