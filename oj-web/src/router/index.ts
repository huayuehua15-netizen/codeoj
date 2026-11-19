import { createRouter, createWebHistory } from 'vue-router'
import { useUserStore } from '@/stores/user'

const APP_NAME = 'CodeOJ'

/**
 * 路由表（论文 4.3 前端模块：用户、题目、在线编辑器、提交结果、统计排行与管理）。
 */
const router = createRouter({
  history: createWebHistory(),
  // P2-3：同组件不同参数（题目 1 → 题目 2）默认沿用旧滚动位置，这里显式回到顶部
  scrollBehavior(_to, _from, savedPosition) {
    return savedPosition ?? { top: 0 }
  },
  routes: [
    {
      path: '/login',
      name: 'login',
      component: () => import('@/views/auth/LoginView.vue'),
      meta: { title: '登录' },
    },
    {
      path: '/register',
      name: 'register',
      component: () => import('@/views/auth/RegisterView.vue'),
      meta: { title: '注册' },
    },
    {
      path: '/',
      component: () => import('@/layouts/MainLayout.vue'),
      children: [
        { path: '', redirect: '/home' },
        {
          path: 'home',
          name: 'home',
          component: () => import('@/views/home/HomeView.vue'),
          meta: { title: '首页' },
        },
        {
          path: 'problems',
          name: 'problems',
          component: () => import('@/views/problem/ProblemListView.vue'),
          meta: { title: '题目列表' },
        },
        {
          path: 'problems/:id',
          name: 'problem-detail',
          component: () => import('@/views/problem/ProblemDetailView.vue'),
          meta: { title: '题目详情' },
        },
        {
          path: 'submissions',
          name: 'submissions',
          component: () => import('@/views/submission/SubmissionListView.vue'),
          meta: { title: '提交记录', requiresAuth: true },
        },
        {
          path: 'submissions/:id',
          name: 'submission-result',
          component: () => import('@/views/submission/SubmissionResultView.vue'),
          meta: { title: '判题结果', requiresAuth: true },
        },
        {
          path: 'statistics',
          name: 'statistics',
          component: () => import('@/views/stats/StatisticsView.vue'),
          meta: { title: '个人统计', requiresAuth: true },
        },
        {
          path: 'contests',
          name: 'contests',
          component: () => import('@/views/contest/ContestListView.vue'),
          meta: { title: '竞赛' },
        },
        {
          path: 'contests/:id',
          name: 'contest-detail',
          component: () => import('@/views/contest/ContestDetailView.vue'),
          meta: { title: '竞赛详情' },
        },
        {
          path: 'leaderboard',
          name: 'leaderboard',
          component: () => import('@/views/stats/LeaderboardView.vue'),
          meta: { title: '排行榜' },
        },
        {
          path: 'announcements',
          name: 'announcements',
          component: () => import('@/views/home/AnnouncementsView.vue'),
          meta: { title: '公告中心' },
        },
        {
          path: 'wrongbook',
          name: 'wrongbook',
          component: () => import('@/views/home/WrongBookView.vue'),
          meta: { title: '错题本', requiresAuth: true },
        },
        {
          path: 'about',
          name: 'about',
          component: () => import('@/views/home/AboutView.vue'),
          meta: { title: '帮助与关于' },
        },
        {
          path: 'profile',
          name: 'profile',
          component: () => import('@/views/auth/ProfileView.vue'),
          meta: { title: '个人中心', requiresAuth: true },
        },
        {
          path: 'admin/problems',
          name: 'admin-problems',
          component: () => import('@/views/admin/ProblemAdminView.vue'),
          meta: { title: '题目管理', requiresAuth: true, requiresAdmin: true },
        },
        {
          path: 'admin/problems/:id/testcases',
          name: 'admin-testcases',
          component: () => import('@/views/admin/TestCaseAdminView.vue'),
          meta: { title: '用例管理', requiresAuth: true, requiresAdmin: true },
        },
        {
          path: 'admin/courses',
          name: 'admin-courses',
          component: () => import('@/views/admin/CourseAdminView.vue'),
          meta: { title: '课程实验', requiresAuth: true, requiresAdmin: true },
        },
        {
          path: 'admin/contests',
          name: 'admin-contests',
          component: () => import('@/views/admin/ContestAdminView.vue'),
          meta: { title: '竞赛管理', requiresAuth: true, requiresAdmin: true },
        },
        {
          path: 'admin/users',
          name: 'admin-users',
          component: () => import('@/views/admin/UserAdminView.vue'),
          meta: { title: '用户管理', requiresAuth: true, requiresAdmin: true },
        },
        {
          path: 'admin/queue',
          name: 'admin-queue',
          component: () => import('@/views/admin/QueueMonitorView.vue'),
          meta: { title: '队列监控', requiresAuth: true, requiresAdmin: true },
        },
        // 404 兜底放在 MainLayout 里：静默重定向会让用户以为"链接点对了但页面坏了"，
        // 给出可读的提示并提供返回入口更符合预期。
        {
          path: ':pathMatch(.*)*',
          name: 'not-found',
          component: () => import('@/views/NotFoundView.vue'),
          meta: { title: '页面不存在' },
        },
      ],
    },
  ],
})

// 登录与权限守卫（论文 4.5：认证采用 JWT）
router.beforeEach((to) => {
  const userStore = useUserStore()
  if (to.meta.requiresAuth && !userStore.isLoggedIn) {
    return { name: 'login', query: { redirect: to.fullPath } }
  }
  // P2-1：越权时不能静默回题库——用户会以为"页面坏了"。
  // 这里用动态 import 引入 ElMessage，避免把整个 Element Plus 拉进首屏路由块。
  if (to.meta.requiresAdmin && !userStore.isAdmin) {
    void import('element-plus')
      .then(({ ElMessage }) => ElMessage.warning('该页面需要管理员权限'))
      .catch(() => {})
    return { name: 'problems' }
  }
})

// P2-2：meta.title 定义了 20 处却从未消费，所有页签都显示 index.html 的固定标题
router.afterEach((to) => {
  const title = to.meta.title as string | undefined
  document.title = title ? `${title} · ${APP_NAME}` : APP_NAME
})

export default router
