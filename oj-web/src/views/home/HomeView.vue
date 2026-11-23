<template>
  <div class="home">
    <!-- 首屏：左侧主张，右侧判题终端。整页只有这一处深色 -->
    <section class="hero" aria-label="平台首页">
      <div class="hero-copy">
        <div class="statusline">
          <span class="status-dot" aria-hidden="true"></span>
          SYSTEM ONLINE <span class="slash">/</span> JUDGE READY
        </div>
        <h1>提交。<span class="muted">编译。</span><span class="blue">判定。</span></h1>
        <p class="lede">
          五种语言沙箱编译，测试点级结果、<b>耗时与内存</b>逐条返回。<br />
          从按下提交到看见判定，只差几秒。
        </p>
        <div class="hero-actions">
          <router-link class="btn-primary" to="/problems">
            浏览题库
            <svg viewBox="0 0 24 24" width="14" height="14" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round" aria-hidden="true">
              <path d="M5 12h14M13 6l6 6-6 6" />
            </svg>
          </router-link>
          <router-link class="btn-secondary" to="/leaderboard">查看排行榜</router-link>
        </div>
        <p class="hero-footnote">NO LOGIN REQUIRED TO READ · REAL-TIME VERDICT PUSH</p>
      </div>

      <div class="terminal" aria-label="判题流程示例">
        <div class="term-head">
          <span class="term-dots" aria-hidden="true"><i></i><i></i><i></i></span>
          <span class="term-title">codeoj / submission</span>
          <span class="term-lang">C++17</span>
        </div>
        <div class="term-body">
          <p><span class="prompt">$</span> codeoj submit <span class="arg">1001</span> --lang cpp</p>
          <p class="dim">→ queued <span class="hash">#4821</span></p>
          <p class="dim">→ compiling g++ -O2 -std=c++17</p>
          <p class="ok">✓ compile <span class="time">0.42s</span></p>
          <div class="term-rule"></div>
          <p class="case"><span>case 01</span><span class="ok">✓ Accepted</span><span class="num">12ms · 2.1MB</span></p>
          <p class="case"><span>case 02</span><span class="ok">✓ Accepted</span><span class="num">18ms · 2.4MB</span></p>
          <p class="case"><span>case 03</span><span class="ok">✓ Accepted</span><span class="num">15ms · 2.2MB</span></p>
          <p class="case"><span>case 04</span><span class="ok">✓ Accepted</span><span class="num">21ms · 2.6MB</span></p>
          <p class="case"><span>case 05</span><span class="ok">✓ Accepted</span><span class="num">16ms · 2.3MB</span></p>
          <div class="verdict">
            <div class="verdict-left"><span class="check">✓</span><span class="verdict-word">Accepted</span></div>
            <span class="perf">45ms · 6.7MB · 5/5</span>
          </div>
        </div>
        <div class="term-foot"><span>judge example · C++17</span><span class="term-foot-note">STATEMENT → CODE → VERDICT</span></div>
      </div>
    </section>

    <!-- 最新公告：一条就够，不占首屏 -->
    <router-link v-if="announcements[0]" class="notice" to="/announcements">
      <span class="notice-tag">公告</span>
      <span class="notice-title">{{ announcements[0].title }}</span>
      <span class="notice-body">{{ announcements[0].content }}</span>
      <span class="notice-date">{{ formatDateTime(announcements[0].createdAt).slice(0, 10) }}</span>
      <span class="notice-go" aria-hidden="true">→</span>
    </router-link>

    <!-- 平台读数 -->
    <section class="metrics" aria-label="平台数据">
      <div class="metric">
        <div class="metric-label">PROBLEMS</div>
        <div class="metric-value">{{ stats.totalProblems ?? '—' }}<small>题</small></div>
        <div class="metric-note">全站题库</div>
      </div>
      <div class="metric">
        <div class="metric-label">SOLVERS</div>
        <div class="metric-value">{{ stats.solvers ?? '—' }}<small>人</small></div>
        <div class="metric-note">已参与用户</div>
      </div>
      <div class="metric">
        <div class="metric-label">SUBMISSIONS</div>
        <div class="metric-value">{{ formatCount(stats.totalSubmit) }}<small>次</small></div>
        <div class="metric-note">累计提交</div>
      </div>
      <div class="metric">
        <div class="metric-label">MAX AC</div>
        <div class="metric-value">{{ stats.maxAcCount ?? '—' }}<small>题</small></div>
        <div class="metric-note">单人最高解题</div>
      </div>
    </section>

    <div class="section-head">
      <div>
        <h2 class="section-title">开始做题</h2>
        <p class="section-sub">按难度和标签筛选，读题、写码、提交都在一个页面完成。</p>
      </div>
      <router-link class="section-link" to="/problems">全部题目 →</router-link>
    </div>

    <section class="lower">
      <div class="card problems-card">
        <div class="card-head">
          <span class="card-title">题目列表</span>
          <span class="card-meta">SORTED BY ID</span>
        </div>
        <template v-if="problems.length">
          <div class="prob-head" aria-hidden="true">
            <span>ID</span><span>题目</span><span>难度</span><span>通过率</span><span></span>
          </div>
          <router-link v-for="row in problems" :key="row.id" class="prob-row" :to="`/problems/${row.id}`">
            <span class="prob-id">#{{ row.id }}</span>
            <span class="prob-main">
              <span class="prob-title">{{ row.title }}</span>
              <span v-if="row.tags && row.tags.length" class="prob-tags">
                <span v-for="t in row.tags.slice(0, 2)" :key="t" class="tag">{{ t }}</span>
              </span>
            </span>
            <span class="diff" :class="`diff-${row.difficulty}`">{{ DIFFICULTY_MAP[row.difficulty]?.label || '未知' }}</span>
            <span class="acc">
              <span class="acc-bar" aria-hidden="true"><i :style="{ width: `${passRate(row)}%` }" /></span>
              <span class="acc-num">{{ passRate(row) }}%</span>
            </span>
            <span class="prob-go" aria-hidden="true">→</span>
          </router-link>
        </template>
        <el-empty v-else description="暂无题目" :image-size="64" />
      </div>

      <div class="card rank-card">
        <div class="card-head">
          <span class="card-title">全站榜单</span>
          <span class="card-meta">ALL TIME · TOP 5</span>
        </div>
        <template v-if="topFive.length">
          <ul class="rank-list">
            <li v-for="row in topFive" :key="row.rank" class="rank-row">
              <span class="rank-no" :class="{ first: row.rank === 1 }">{{ String(row.rank).padStart(2, '0') }}</span>
              <span class="rank-avatar">
                <img v-if="row.avatar" :src="row.avatar" alt="" />
                <span v-else aria-hidden="true">{{ (row.username || '?').slice(0, 1).toUpperCase() }}</span>
              </span>
              <span class="rank-main">
                <span class="rank-name">{{ row.username || `用户 ${row.userId}` }}</span>
                <span class="rank-bar" aria-hidden="true"><i :style="{ width: `${Math.min(100, (row.acCount / Math.max(1, topFive[0]?.acCount || 1)) * 100)}%` }" /></span>
              </span>
              <span class="rank-ac">{{ row.acCount }}<small>AC</small></span>
            </li>
          </ul>
        </template>
        <el-empty v-else description="榜单暂无数据" :image-size="64" />
      </div>
    </section>

    <section class="features" aria-label="平台能力">
      <article v-for="(f, i) in FEATURES" :key="f.title" class="feature">
        <div class="feature-top">
          <span class="feature-icon" v-html="f.icon" />
          <span class="feature-no">{{ String(i + 1).padStart(2, '0') }}</span>
        </div>
        <h3 class="feature-title">{{ f.title }}</h3>
        <p class="feature-desc">{{ f.desc }}</p>
      </article>
    </section>
  </div>
</template>

<script setup lang="ts">
// 平台首页（落地页）：公开数据并行拉取，失败降级为空态。
import { onMounted, reactive, ref } from 'vue'
import { getProblemOverview, pageProblems, type Problem } from '@/api/problem'
import { getLeaderboard } from '@/api/leaderboard'
import { listAnnouncements, type AnnouncementItem } from '@/api/announcement'
import { formatDateTime } from '@/utils/datetime'
import { DIFFICULTY_MAP } from '@/utils/status'

const stats = reactive<{
  totalProblems?: number
  solvers?: number
  totalSubmit?: number
  maxAcCount?: number
}>({})

const announcements = ref<AnnouncementItem[]>([])
const problems = ref<Problem[]>([])
const topFive = ref<{ rank: number; userId: number; username?: string; avatar?: string | null; acCount: number }[]>([])

/** 能力卡：内联 SVG 图标（stroke 继承 currentColor） */
const FEATURES = [
  {
    title: '五语言沙箱',
    desc: 'C / C++ / Java / Python / JavaScript，独立镜像编译执行。',
    icon: '<svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.7" stroke-linecap="round" stroke-linejoin="round"><polyline points="8 6 3 12 8 18"/><polyline points="16 6 21 12 16 18"/><line x1="13.5" y1="4.5" x2="10.5" y2="19.5"/></svg>',
  },
  {
    title: '用例级判定',
    desc: '每个测试点的状态、耗时、内存逐条呈现，错在哪个用例一眼可见。',
    icon: '<svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.7" stroke-linecap="round" stroke-linejoin="round"><path d="M9 11l3 3L22 4"/><path d="M21 12v7a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2V5a2 2 0 0 1 2-2h11"/></svg>',
  },
  {
    title: '实时推送',
    desc: '排队、编译、判题、出结果，状态自动推进，不用刷新页面。',
    icon: '<svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.7" stroke-linecap="round" stroke-linejoin="round"><path d="M13 2 4 14h7l-1 8 9-12h-7l1-8z"/></svg>',
  },
  {
    title: '竞赛与复盘',
    desc: 'ACM 赛制实时排名，错题本与热力图把每次练习沉淀下来。',
    icon: '<svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.7" stroke-linecap="round" stroke-linejoin="round"><path d="M8 21h8M12 17v4"/><path d="M7 4h10v5a5 5 0 0 1-10 0V4z"/><path d="M17 5h3v2a4 4 0 0 1-4 4M7 5H4v2a4 4 0 0 0 4 4"/></svg>',
  },
]

function passRate(row: Problem) {
  if (!row.submitCount) return 0
  return Math.round((row.acceptedCount / row.submitCount) * 100)
}

/** 大数字用等宽字体，超过一万时给 k 缩写，避免撑破读数卡 */
function formatCount(n?: number) {
  if (n == null) return '—'
  return n >= 10000 ? `${(n / 1000).toFixed(1)}k` : String(n)
}

onMounted(async () => {
  const [overview, board, anns, list] = await Promise.allSettled([
    getProblemOverview(),
    getLeaderboard({ page: 1, size: 5 }),
    listAnnouncements(),
    pageProblems({ page: 1, size: 4 }),
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
  if (list.status === 'fulfilled') problems.value = (list.value.records || []).slice(0, 4)
})
</script>

<style scoped>
.home {
  max-width: 1280px;
  margin: 0 auto;
  padding: 54px 32px 64px;
}

/* ---------- hero ---------- */
.hero {
  display: grid;
  grid-template-columns: minmax(0, 1.02fr) minmax(0, 0.98fr);
  gap: 64px;
  align-items: center;
  padding-bottom: 38px;
}
.statusline {
  display: flex;
  align-items: center;
  gap: 8px;
  font-family: var(--oj-font-mono);
  font-size: 9.5px;
  letter-spacing: 0.11em;
  color: var(--oj-text-sub);
}
.status-dot {
  width: 6px;
  height: 6px;
  border-radius: 50%;
  background: var(--oj-success);
  box-shadow: 0 0 0 3px rgba(14, 159, 110, 0.1);
}
.statusline .slash {
  color: var(--oj-gray-300);
}
.hero h1 {
  margin: 17px 0 0;
  font-size: clamp(42px, 4.7vw, 62px);
  line-height: 1.08;
  letter-spacing: -0.055em;
  font-weight: 800;
  color: var(--oj-text);
}
.hero h1 .muted {
  color: var(--oj-gray-700);
}
.hero h1 .blue {
  color: var(--oj-primary);
}
.lede {
  margin: 21px 0 0;
  max-width: 30em;
  font-size: 14.5px;
  line-height: 1.85;
  color: var(--oj-text-sub);
}
.lede b {
  color: var(--oj-gray-700);
  font-weight: 600;
}
.hero-actions {
  display: flex;
  align-items: center;
  gap: 10px;
  margin-top: 27px;
}
.btn-primary {
  display: inline-flex;
  align-items: center;
  gap: 9px;
  padding: 11px 18px;
  border-radius: 7px;
  background: var(--oj-primary);
  color: #fff;
  font-size: 13px;
  font-weight: 650;
  box-shadow: 0 1px 2px rgba(16, 24, 40, 0.08);
  transition: background var(--oj-duration) var(--oj-ease);
}
.btn-primary:hover {
  background: var(--oj-primary-hover);
}
.btn-secondary {
  display: inline-flex;
  align-items: center;
  padding: 11px 18px;
  border: 1px solid var(--oj-border-strong);
  border-radius: 7px;
  background: var(--oj-card);
  color: var(--oj-gray-700);
  font-size: 13px;
  font-weight: 600;
  transition: border-color var(--oj-duration) var(--oj-ease), color var(--oj-duration) var(--oj-ease);
}
.btn-secondary:hover {
  border-color: var(--oj-gray-300);
  color: var(--oj-text);
}
.hero-footnote {
  margin: 15px 0 0;
  font-family: var(--oj-font-mono);
  font-size: 9px;
  letter-spacing: 0.05em;
  color: var(--oj-text-weak);
}

/* ---------- 判题终端 ---------- */
.terminal {
  position: relative;
  background: var(--oj-code-bg);
  border: 1px solid #232b36;
  border-radius: 10px;
  overflow: hidden;
  box-shadow: 0 24px 50px -22px rgba(16, 24, 40, 0.45), 0 2px 6px rgba(16, 24, 40, 0.08);
}
.terminal::before {
  content: '';
  position: absolute;
  inset: 0 0 auto;
  height: 2px;
  background: var(--oj-primary);
  opacity: 0.9;
}
.term-head {
  height: 42px;
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 0 14px;
  border-bottom: 1px solid var(--oj-code-border);
  font-family: var(--oj-font-mono);
  font-size: 10px;
  color: #7f8b9d;
  letter-spacing: 0.04em;
}
.term-dots {
  display: flex;
  gap: 5px;
}
.term-dots i {
  width: 6px;
  height: 6px;
  border-radius: 50%;
  background: #3b4552;
}
.term-title {
  color: #aab4c3;
}
.term-lang {
  margin-left: auto;
  color: #8ea6ff;
  border: 1px solid rgba(142, 166, 255, 0.25);
  background: rgba(40, 85, 255, 0.1);
  padding: 2px 7px;
  border-radius: 3px;
  font-size: 9px;
}
.term-body {
  padding: 17px 18px 14px;
  font-family: var(--oj-font-mono);
  font-size: 12px;
  line-height: 1.9;
  color: #c7d0dc;
}
.term-body p {
  margin: 0;
}
.prompt {
  color: #8ea6ff;
  margin-right: 8px;
}
.arg {
  color: var(--oj-code-number);
}
.dim {
  color: #7c8899;
}
.hash {
  color: #9aa7b8;
}
.ok {
  color: #57d9a3;
}
.time {
  color: #8d99aa;
}
.case {
  display: grid;
  grid-template-columns: 1fr 1fr auto;
  gap: 12px;
  color: #aeb9c7;
}
.case .num {
  color: #7c8899;
  font-size: 10.5px;
  text-align: right;
}
.term-rule {
  height: 1px;
  background: var(--oj-code-border);
  margin: 10px 0;
}
.verdict {
  margin-top: 14px;
  padding: 12px;
  border: 1px solid rgba(87, 217, 163, 0.22);
  background: rgba(87, 217, 163, 0.07);
  border-radius: 6px;
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 14px;
}
.verdict-left {
  display: flex;
  align-items: center;
  gap: 9px;
}
.check {
  width: 20px;
  height: 20px;
  border-radius: 50%;
  background: rgba(87, 217, 163, 0.14);
  color: #57d9a3;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 11px;
}
.verdict-word {
  font-size: 15px;
  font-weight: 700;
  color: #6ee7b7;
  letter-spacing: -0.01em;
}
.perf {
  font-size: 10px;
  color: #7c8899;
}
.term-foot {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 10px;
  padding: 10px 14px;
  border-top: 1px solid var(--oj-code-border);
  background: #0a0e14;
  font-family: var(--oj-font-mono);
  font-size: 9px;
  color: #6b7687;
  letter-spacing: 0.03em;
}
.term-foot-note {
  color: #8ea6ff;
}

/* ---------- 公告条 ---------- */
.notice {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 11px 16px;
  margin-bottom: 14px;
  background: var(--oj-card);
  border: 1px solid var(--oj-border);
  border-left: 3px solid var(--oj-primary);
  border-radius: 6px;
  box-shadow: var(--oj-shadow-sm);
  font-size: 12px;
  color: var(--oj-text-sub);
}
.notice:hover {
  border-color: var(--oj-info-border);
  border-left-color: var(--oj-primary);
}
.notice-tag {
  flex-shrink: 0;
  font-family: var(--oj-font-mono);
  font-size: 9px;
  letter-spacing: 0.08em;
  color: var(--oj-primary);
  background: var(--oj-primary-soft);
  padding: 2px 6px;
  border-radius: 3px;
}
.notice-title {
  flex-shrink: 0;
  font-weight: 650;
  color: var(--oj-text);
}
.notice-body {
  flex: 1;
  min-width: 0;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  color: var(--oj-text-sub);
}
.notice-date {
  flex-shrink: 0;
  font-family: var(--oj-font-mono);
  font-size: 9.5px;
  color: var(--oj-text-weak);
}
.notice-go {
  flex-shrink: 0;
  color: var(--oj-primary);
}

/* ---------- 读数卡 ---------- */
.metrics {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  overflow: hidden;
  background: var(--oj-card);
  border: 1px solid var(--oj-border);
  border-radius: 10px;
  box-shadow: var(--oj-shadow-sm);
}
.metric {
  padding: 17px 22px;
  border-right: 1px solid var(--oj-border);
}
.metric:last-child {
  border-right: 0;
}
.metric-label {
  font-family: var(--oj-font-mono);
  font-size: 9px;
  letter-spacing: 0.12em;
  color: var(--oj-text-weak);
}
.metric-value {
  margin-top: 7px;
  font-family: var(--oj-font-mono);
  font-size: 28px;
  line-height: 1;
  font-weight: 600;
  letter-spacing: -0.04em;
  color: var(--oj-text);
}
.metric-value small {
  margin-left: 5px;
  font-size: 10px;
  font-weight: 500;
  letter-spacing: 0;
  color: var(--oj-text-sub);
}
.metric-note {
  margin-top: 6px;
  font-family: var(--oj-font-mono);
  font-size: 9px;
  color: var(--oj-text-weak);
}

/* ---------- section ---------- */
.section-head {
  display: flex;
  align-items: flex-end;
  justify-content: space-between;
  gap: 16px;
  margin: 36px 0 14px;
}
.section-title {
  margin: 0;
  font-size: 21px;
  font-weight: 750;
  letter-spacing: -0.03em;
}
.section-sub {
  margin: 6px 0 0;
  font-size: 11px;
  color: var(--oj-text-sub);
}
.section-link {
  font-size: 11.5px;
  font-weight: 650;
  color: var(--oj-primary);
  white-space: nowrap;
}
.section-link:hover {
  color: var(--oj-primary-hover);
}

/* ---------- 题目/榜单 ---------- */
.lower {
  display: grid;
  grid-template-columns: minmax(0, 1.42fr) minmax(0, 1fr);
  gap: 22px;
}
.problems-card,
.rank-card {
  padding: 0;
  overflow: hidden;
}
.card-head {
  height: 48px;
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 0 18px;
  border-bottom: 1px solid var(--oj-border);
}
.card-title {
  font-size: 13px;
  font-weight: 700;
  letter-spacing: -0.01em;
}
.card-meta {
  font-family: var(--oj-font-mono);
  font-size: 8.5px;
  letter-spacing: 0.08em;
  color: var(--oj-text-weak);
}
.prob-head,
.prob-row {
  display: grid;
  grid-template-columns: 48px minmax(0, 1fr) 72px 104px 22px;
  gap: 12px;
  align-items: center;
  padding: 0 18px;
}
.prob-head {
  height: 32px;
  background: var(--oj-gray-50);
  border-bottom: 1px solid var(--oj-border);
  font-family: var(--oj-font-mono);
  font-size: 8.5px;
  letter-spacing: 0.09em;
  color: var(--oj-text-weak);
}
.prob-row {
  min-height: 58px;
  border-bottom: 1px solid var(--oj-border);
  transition: background var(--oj-duration) var(--oj-ease);
}
.prob-row:last-child {
  border-bottom: 0;
}
.prob-row:hover {
  background: var(--oj-gray-50);
}
.prob-id {
  font-family: var(--oj-font-mono);
  font-size: 10.5px;
  color: var(--oj-text-weak);
}
.prob-main {
  min-width: 0;
}
.prob-title {
  display: block;
  font-size: 13.5px;
  font-weight: 600;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
  color: var(--oj-text);
}
.prob-tags {
  display: flex;
  gap: 4px;
  margin-top: 4px;
}
.tag {
  font-family: var(--oj-font-mono);
  font-size: 8px;
  line-height: 1.5;
  color: var(--oj-text-sub);
  border: 1px solid var(--oj-border);
  background: var(--oj-gray-50);
  padding: 1px 5px;
  border-radius: 3px;
}
.diff {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  font-size: 11.5px;
  font-weight: 600;
}
.diff::before {
  content: '';
  width: 5px;
  height: 5px;
  border-radius: 50%;
  background: currentColor;
}
.diff-1 { color: var(--oj-success); }
.diff-2 { color: var(--oj-warning); }
.diff-3 { color: var(--oj-danger); }
.acc {
  display: flex;
  align-items: center;
  gap: 8px;
}
.acc-bar {
  width: 44px;
  height: 3px;
  border-radius: 99px;
  background: var(--oj-gray-100);
  overflow: hidden;
}
.acc-bar i {
  display: block;
  height: 100%;
  border-radius: 99px;
  background: var(--oj-primary);
}
.acc-num {
  width: 38px;
  text-align: right;
  font-family: var(--oj-font-mono);
  font-size: 9.5px;
  color: var(--oj-text-sub);
}
.prob-go {
  text-align: right;
  font-size: 13px;
  color: var(--oj-text-weak);
}
.prob-row:hover .prob-go {
  color: var(--oj-primary);
}

.rank-list {
  list-style: none;
  margin: 0;
  padding: 3px 0;
}
.rank-row {
  display: grid;
  grid-template-columns: 28px 28px minmax(0, 1fr) 52px;
  gap: 10px;
  align-items: center;
  padding: 11px 18px;
  border-bottom: 1px solid var(--oj-border);
}
.rank-row:last-child {
  border-bottom: 0;
}
.rank-no {
  font-family: var(--oj-font-mono);
  font-size: 11px;
  color: var(--oj-text-weak);
}
.rank-no.first {
  color: var(--oj-primary);
  font-weight: 700;
}
.rank-avatar {
  width: 26px;
  height: 26px;
  border-radius: 5px;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  background: var(--oj-gray-100);
  color: var(--oj-gray-600);
  font-size: 11px;
  font-weight: 700;
  overflow: hidden;
}
.rank-avatar img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}
.rank-row:first-child .rank-avatar {
  background: var(--oj-primary-soft);
  color: var(--oj-primary);
}
.rank-main {
  min-width: 0;
}
.rank-name {
  display: block;
  font-size: 12px;
  font-weight: 600;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
  color: var(--oj-text);
}
.rank-bar {
  display: block;
  height: 2px;
  margin-top: 5px;
  background: var(--oj-gray-100);
  border-radius: 99px;
  overflow: hidden;
}
.rank-bar i {
  display: block;
  height: 100%;
  background: var(--oj-gray-300);
  border-radius: 99px;
}
.rank-row:first-child .rank-bar i {
  background: var(--oj-primary);
}
.rank-ac {
  text-align: right;
  font-family: var(--oj-font-mono);
  font-size: 11px;
  color: var(--oj-gray-700);
}
.rank-ac small {
  margin-left: 3px;
  font-size: 8px;
  color: var(--oj-text-weak);
}

/* ---------- 能力 ---------- */
.features {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  margin-top: 22px;
  overflow: hidden;
  background: var(--oj-card);
  border: 1px solid var(--oj-border);
  border-radius: 10px;
  box-shadow: var(--oj-shadow-sm);
}
.feature {
  padding: 19px 20px 20px;
  border-right: 1px solid var(--oj-border);
}
.feature:last-child {
  border-right: 0;
}
.feature-top {
  display: flex;
  align-items: center;
  justify-content: space-between;
}
.feature-icon {
  width: 30px;
  height: 30px;
  border: 1px solid var(--oj-border);
  border-radius: 6px;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  color: var(--oj-text-sub);
}
.feature-icon :deep(svg) {
  width: 14px;
  height: 14px;
}
.feature-no {
  font-family: var(--oj-font-mono);
  font-size: 8.5px;
  color: var(--oj-text-weak);
}
.feature-title {
  margin: 13px 0 5px;
  font-size: 12.5px;
  font-weight: 700;
  letter-spacing: -0.01em;
}
.feature-desc {
  font-size: 10.5px;
  line-height: 1.7;
  color: var(--oj-text-sub);
}

@media (max-width: 1040px) {
  .hero {
    grid-template-columns: 1fr;
    gap: 40px;
  }
  .terminal {
    max-width: 620px;
  }
  .features {
    grid-template-columns: repeat(2, 1fr);
  }
  .feature:nth-child(2) {
    border-right: 0;
  }
  .feature:nth-child(n + 3) {
    border-top: 1px solid var(--oj-border);
  }
  .lower {
    grid-template-columns: 1fr;
  }
}
@media (max-width: 680px) {
  .home {
    padding: 30px 18px 52px;
  }
  .hero h1 {
    font-size: 40px;
  }
  .metrics {
    grid-template-columns: 1fr 1fr;
  }
  .metric:nth-child(2) {
    border-right: 0;
  }
  .metric:nth-child(n + 3) {
    border-top: 1px solid var(--oj-border);
  }
  .notice-body,
  .notice-date {
    display: none;
  }
  .prob-head {
    display: none;
  }
  .prob-row {
    grid-template-columns: 44px minmax(0, 1fr) 52px;
    gap: 10px;
    padding: 12px 16px;
  }
  .diff,
  .acc {
    display: none;
  }
  .features {
    grid-template-columns: 1fr;
  }
  .feature,
  .feature:nth-child(2) {
    border-right: 0;
  }
  .feature + .feature {
    border-top: 1px solid var(--oj-border);
  }
  .term-body {
    font-size: 11px;
  }
}
</style>
