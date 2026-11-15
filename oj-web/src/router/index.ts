import { createRouter, createWebHistory } from 'vue-router'
import { useUserStore } from '@/stores/user'

/**
 * 路由表（论文 4.3 前端模块：用户、题目、在线编辑器、提交结果、统计排行与管理）。
 */
const router = createRouter({
  history: createWebHistory(),
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
        { path: '', redirect: '/problems' },
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
          path: 'leaderboard',
          name: 'leaderboard',
          component: () => import('@/views/stats/LeaderboardView.vue'),
          meta: { title: '排行榜' },
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
          path: 'admin/queue',
          name: 'admin-queue',
          component: () => import('@/views/admin/QueueMonitorView.vue'),
          meta: { title: '队列监控', requiresAuth: true, requiresAdmin: true },
        },
      ],
    },
    // 404 兜底：未匹配路由重定向回题目列表
    { path: '/:pathMatch(.*)*', redirect: '/problems' },
  ],
})

// 登录与权限守卫（论文 4.5：认证采用 JWT）
router.beforeEach((to) => {
  const userStore = useUserStore()
  if (to.meta.requiresAuth && !userStore.isLoggedIn) {
    return { name: 'login', query: { redirect: to.fullPath } }
  }
  if (to.meta.requiresAdmin && !userStore.isAdmin) {
    return { name: 'problems' }
  }
})

export default router
