<template>
  <div class="home">
    <!-- 开场：全站最大的排版。宋体大标语压在墨卷上，一眼定调 -->
    <section class="hero">
      <div class="hero-inner">
        <p class="hero-kicker mono">ONLINE JUDGE · 在线评测</p>
        <h1 class="hero-title">落笔，<em>即见分晓</em>。</h1>
        <p class="hero-sub">
          五种语言在线编译执行，用例级判题明细逐条可见；<br />
          判题如批改——绿批是掌声，朱批告诉你错在哪一行。
        </p>
        <div class="hero-cta">
          <el-button type="primary" size="large" @click="$router.push('/problems')">浏览题库</el-button>
          <el-button size="large" class="ghost-btn" @click="$router.push('/leaderboard')">查看排行榜</el-button>
        </div>
        <div class="hero-stats">
          <div class="hs">
            <span class="hs-num mono tabular">{{ stats.totalProblems ?? '—' }}</span>
            <span class="hs-label">题目总数</span>
          </div>
          <div class="hs">
            <span class="hs-num mono tabular">{{ stats.solvers ?? '—' }}</span>
            <span class="hs-label">解题者</span>
          </div>
          <div class="hs">
            <span class="hs-num mono tabular">{{ stats.totalSubmit ?? '—' }}</span>
            <span class="hs-label">全站提交</span>
          </div>
          <div class="hs">
            <span class="hs-num mono tabular">{{ stats.maxAcCount ?? '—' }}</span>
            <span class="hs-label">最高解题数</span>
          </div>
        </div>
      </div>
    </section>

    <div class="home-body">
      <!-- 四个能力卡 -->
      <section class="features" aria-label="平台能力">
        <article v-for="f in FEATURES" :key="f.title" class="card feature">
          <span class="feature-icon" v-html="f.icon" />
          <h2 class="feature-title">{{ f.title }}</h2>
          <p class="feature-desc">{{ f.desc }}</p>
        </article>
      </section>

      <!-- 公告速览 + 榜单速览 -->
      <section class="home-grid">
        <div class="card">
          <div class="card-head">
            <h2>最新公告</h2>
            <router-link class="more-link" to="/announcements">公告中心 →</router-link>
          </div>
          <ul v-if="announcements.length" class="ann-list">
            <li v-for="a in announcements" :key="a.id" class="ann-item">
              <span class="ann-dot" aria-hidden="true" />
              <div class="ann-main">
                <p class="ann-title">{{ a.title }}</p>
                <p class="ann-content">{{ a.content }}</p>
              </div>
              <span class="ann-date mono">{{ formatDateTime(a.createdAt) }}</span>
            </li>
          </ul>
          <el-empty v-else description="暂无公告" :image-size="64" />
        </div>

        <div class="card">
          <div class="card-head">
            <h2>榜单前五</h2>
            <router-link class="more-link" to="/leaderboard">完整榜单 →</router-link>
          </div>
          <ul v-if="topFive.length" class="top-list">
            <li v-for="row in topFive" :key="row.rank" class="top-row">
              <span class="rank-badge" :class="`rank-${row.rank}`">{{ row.rank }}</span>
              <span class="rank-name">
                <img v-if="row.avatar" class="top-avatar" :src="row.avatar" alt="" />
                {{ row.username || `用户 ${row.userId}` }}
              </span>
              <span class="rank-ac mono tabular">{{ row.acCount }} 题</span>
            </li>
          </ul>
          <el-empty v-else description="榜单虚位以待" :image-size="64" />
        </div>
      </section>
    </div>
  </div>
</template>

<script setup lang="ts">
// 平台首页（落地页）：全部数据来自公开接口，未登录也能完整展示。
// 三路数据互不依赖，Promise.allSettled 并行取、失败降级为空态。
import { onMounted, reactive, ref } from 'vue'
import { getProblemOverview } from '@/api/problem'
import { getLeaderboard } from '@/api/leaderboard'
import { listAnnouncements, type AnnouncementItem } from '@/api/announcement'
import { formatDateTime } from '@/utils/datetime'

const stats = reactive<{
  totalProblems?: number
  solvers?: number
  totalSubmit?: number
  maxAcCount?: number
}>({})

const announcements = ref<AnnouncementItem[]>([])
const topFive = ref<{ rank: number; userId: number; username?: string; avatar?: string | null; acCount: number }[]>([])

/** 能力卡：内联 SVG 图标（stroke 继承 currentColor，随主题走） */
const FEATURES = [
  {
    title: '五语言沙箱判题',
    desc: 'C / C++ / Java / Python / JavaScript，独立镜像编译执行，逐用例测量时间与内存。',
    icon: '<svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.8" stroke-linecap="round" stroke-linejoin="round"><polyline points="8 6 3 12 8 18"/><polyline points="16 6 21 12 16 18"/><line x1="13.5" y1="4.5" x2="10.5" y2="19.5"/></svg>',
  },
  {
    title: '用例级判题明细',
    desc: '每个测试点的通过状态、耗时、内存逐条呈现——错在哪个用例，一目了然。',
    icon: '<svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.8" stroke-linecap="round" stroke-linejoin="round"><path d="M9 11l3 3L22 4"/><path d="M21 12v7a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2V5a2 2 0 0 1 2-2h11"/></svg>',
  },
  {
    title: '实时推送',
    desc: 'WebSocket 三态推送：排队 → 判题中 → 出结果，全程无需刷新页面。',
    icon: '<svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.8" stroke-linecap="round" stroke-linejoin="round"><path d="M13 2 4 14h7l-1 8 9-12h-7l1-8z"/></svg>',
  },
  {
    title: '竞赛与成长',
    desc: 'ACM 赛制实时排名、刷题热力图、错题本——把每一次练习变成看得见的进步。',
    icon: '<svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.8" stroke-linecap="round" stroke-linejoin="round"><path d="M8 21h8M12 17v4M17 4h3v3a5 5 0 0 1-5 5H9a5 5 0 0 1-5-5V4h3"/><path d="M7 4v4a5 5 0 0 0 10 0V4H7z"/></svg>',
  },
]

onMounted(async () => {
  const [overview, board, anns] = await Promise.allSettled([
    getProblemOverview(),
    getLeaderboard({ page: 1, size: 5 }),
    listAnnouncements(),
  ])
  if (overview.status === 'fulfilled') {
    stats.totalProblems = overview.value.totalProblems
  }
  if (board.status === 'fulfilled') {
    stats.solvers = board.value.summary?.solverCount
    stats.totalSubmit = board.value.summary?.totalSubmit
    stats.maxAcCount = board.value.summary?.maxAcCount
    topFive.value = (board.value.items || []).slice(0, 5)
  }
  if (anns.status === 'fulfilled') announcements.value = (anns.value || []).slice(0, 3)
})
</script>

<style scoped>
/* ---------- 开场 hero：全站最大排版 ---------- */
.hero {
  position: relative;
  overflow: hidden;
  border: 1px solid #1a261f;
  border-radius: var(--oj-radius);
  background-image:
    url("data:image/svg+xml,%3Csvg xmlns='http://www.w3.org/2000/svg' width='160' height='160'%3E%3Cfilter id='n'%3E%3CfeTurbulence type='fractalNoise' baseFrequency='0.9' numOctaves='2'/%3E%3CfeColorMatrix type='saturate' values='0'/%3E%3CfeComponentTransfer%3E%3CfeFuncA type='linear' slope='0.05'/%3E%3C/feComponentTransfer%3E%3C/filter%3E%3Crect width='160' height='160' filter='url(%23n)'/%3E%3C/svg%3E"),
    repeating-linear-gradient(
      to right,
      transparent 0,
      transparent 31px,
      rgba(159, 214, 176, 0.055) 31px,
      rgba(159, 214, 176, 0.055) 32px
    ),
    linear-gradient(150deg, #14211a 0%, #1d2c24 55%, #16231c 100%);
  color: var(--oj-white);
}
.hero-inner {
  padding: 56px 48px 44px;
}
.hero-kicker {
  margin: 0;
  font-size: 12px;
  letter-spacing: 0.28em;
  color: rgba(159, 214, 176, 0.75);
}
.hero-title {
  margin: 14px 0 0;
  font-family: var(--oj-font-display);
  font-size: 46px;
  font-weight: 700;
  line-height: 1.25;
  letter-spacing: 0.04em;
  color: var(--oj-white);
}
.hero-title em {
  font-style: normal;
  color: #9fd6b0;
}
.hero-sub {
  margin: 16px 0 0;
  font-size: 14px;
  line-height: 1.9;
  color: rgba(215, 228, 216, 0.72);
}
.hero-cta {
  display: flex;
  gap: 12px;
  margin-top: 26px;
}
/* 幽灵按钮：纸白描边，悬浮变实底 */
.ghost-btn :deep(.el-button),
.hero-cta .el-button--default {
  background: transparent;
  border-color: rgba(255, 254, 249, 0.35);
  color: var(--oj-white);
}
.hero-cta .el-button--default:hover {
  background: rgba(255, 254, 249, 0.08);
  border-color: rgba(255, 254, 249, 0.6);
  color: var(--oj-white);
}
.hero-stats {
  display: flex;
  flex-wrap: wrap;
  margin-top: 34px;
  border-top: 1px solid rgba(255, 254, 249, 0.14);
  padding-top: 20px;
}
.hs {
  min-width: 130px;
  padding: 0 24px;
}
.hs:first-child {
  padding-left: 0;
}
.hs + .hs {
  border-left: 1px solid rgba(255, 254, 249, 0.14);
}
.hs-num {
  display: block;
  font-size: 28px;
  font-weight: 600;
  line-height: 1.15;
  letter-spacing: -0.5px;
  color: var(--oj-white);
}
.hs-label {
  margin-top: 4px;
  font-size: 12px;
  color: rgba(215, 228, 216, 0.6);
}

.home-body {
  margin-top: 16px;
}
/* ---------- 能力卡 ---------- */
.features {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 16px;
}
@media (max-width: 1000px) {
  .features {
    grid-template-columns: repeat(2, 1fr);
  }
}
@media (max-width: 640px) {
  .features {
    grid-template-columns: 1fr;
  }
  .hero-inner {
    padding: 40px 28px 34px;
  }
  .hero-title {
    font-size: 34px;
  }
}
.feature {
  padding: 22px 22px 20px;
}
.feature-icon {
  display: inline-flex;
  width: 34px;
  height: 34px;
  border-radius: 8px;
  background: var(--oj-primary-soft);
  color: var(--oj-primary);
  display: inline-flex;
  align-items: center;
  justify-content: center;
}
.feature-icon :deep(svg) {
  width: 19px;
  height: 19px;
}
.feature-title {
  margin: 14px 0 6px;
  font-size: 15.5px;
  font-weight: 700;
  font-family: var(--oj-font-display);
  letter-spacing: 0.02em;
}
.feature-desc {
  margin: 0;
  font-size: 12.5px;
  line-height: 1.8;
  color: var(--oj-text-sub);
}

/* ---------- 公告 + 榜单 ---------- */
.home-grid {
  display: grid;
  grid-template-columns: 1.2fr 1fr;
  gap: 16px;
  margin-top: 16px;
  align-items: start;
}
@media (max-width: 860px) {
  .home-grid {
    grid-template-columns: 1fr;
  }
}
.card-head {
  display: flex;
  align-items: baseline;
  gap: 12px;
  margin-bottom: 12px;
}
.card-head h2 {
  margin: 0;
}
.more-link {
  margin-left: auto;
  font-size: 12.5px;
  color: var(--oj-primary);
}
.more-link:hover {
  text-decoration: underline;
}

.ann-list {
  list-style: none;
  margin: 0;
  padding: 0;
}
.ann-item {
  display: flex;
  gap: 12px;
  padding: 12px 0;
  align-items: flex-start;
}
.ann-item + .ann-item {
  border-top: 1px solid var(--oj-gray-100);
}
.ann-dot {
  width: 7px;
  height: 7px;
  border-radius: 50%;
  background: var(--oj-seal);
  margin-top: 6px;
  flex-shrink: 0;
}
.ann-main {
  flex: 1;
  min-width: 0;
}
.ann-title {
  margin: 0;
  font-size: 14px;
  font-weight: 600;
  color: var(--oj-text);
}
.ann-content {
  margin: 4px 0 0;
  font-size: 12.5px;
  line-height: 1.7;
  color: var(--oj-text-sub);
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
}
.ann-date {
  font-size: 11.5px;
  color: var(--oj-text-weak);
  flex-shrink: 0;
}

.top-list {
  list-style: none;
  margin: 0;
  padding: 0;
}
.top-row {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 10px 0;
}
.top-row + .top-row {
  border-top: 1px solid var(--oj-gray-100);
}
.rank-badge {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 24px;
  height: 24px;
  border-radius: 4px;
  color: var(--oj-white);
  font-family: var(--oj-font-display);
  font-weight: 700;
  font-size: 12.5px;
  flex-shrink: 0;
}
.rank-badge.rank-1 { background: var(--oj-seal); }
.rank-badge.rank-2 { background: var(--oj-gray-800); }
.rank-badge.rank-3 { background: var(--oj-bronze); }
.rank-badge.rank-4,
.rank-badge.rank-5 {
  background: var(--oj-gray-50);
  border: 1px solid var(--oj-border);
  color: var(--oj-gray-600);
}
.rank-name {
  flex: 1;
  min-width: 0;
  font-weight: 600;
  font-size: 13.5px;
  color: var(--oj-text);
  display: flex;
  align-items: center;
  gap: 8px;
}
.top-avatar {
  width: 22px;
  height: 22px;
  border-radius: 4px;
  object-fit: cover;
  border: 1px solid var(--oj-border);
}
.rank-ac {
  font-size: 12.5px;
  color: var(--oj-text-sub);
}
</style>
