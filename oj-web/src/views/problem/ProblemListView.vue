<template>
  <div class="problem-page">
    <header class="problem-head">
      <div class="head-copy">
        <h1 class="problem-title">题库</h1>
        <p class="problem-sub">按难度、标签检索；点开即读题面与样例，登录后可直接提交。</p>
      </div>
      <p class="head-meta" aria-label="平台概览">
        <span><b>{{ overview.totalProblems }}</b> 题</span>
        <span><b>{{ overview.solvers }}</b> 人</span>
        <span><b>{{ overview.submits }}</b> 次提交</span>
        <span><b>{{ overview.acceptRate }}%</b> 通过</span>
      </p>
    </header>

    <router-link v-if="announcements.length" class="notice-line" to="/announcements">
      <span class="notice-label">公告</span>
      <span class="notice-title">{{ announcements[0].title }}</span>
      <span class="notice-body">{{ announcements[0].content }}</span>
    </router-link>

    <section class="index-panel" aria-label="题目列表">
      <div class="filter-bar">
        <div class="search-wrap">
          <el-input
            id="problem-search"
            v-model="keyword"
            placeholder="搜索题目名称"
            class="search-input"
            clearable
            @keyup.enter="search"
            @clear="search"
          >
            <template #prefix>
              <svg viewBox="0 0 24 24" width="14" height="14" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" aria-hidden="true">
                <circle cx="11" cy="11" r="7" /><path d="M20 20 L16.5 16.5" />
              </svg>
            </template>
          </el-input>
        </div>

        <div class="diff-filter" role="group" aria-label="按难度筛选">
          <button
            v-for="opt in difficultyOptions"
            :key="String(opt.value)"
            type="button"
            class="diff-item"
            :class="{ active: difficulty === opt.value }"
            :aria-pressed="difficulty === opt.value"
            @click="pickDifficulty(opt.value)"
          >
            <span v-if="opt.value !== undefined" class="diff-dot" :class="`dot-${opt.value}`" aria-hidden="true" />
            <span>{{ opt.label }}</span>
          </button>
        </div>

        <el-select
          v-model="tag"
          placeholder="全部标签"
          class="tag-select"
          clearable
          filterable
          @change="search"
        >
          <el-option v-for="t in tagOptions" :key="t" :label="t" :value="t" />
        </el-select>

        <button type="button" class="search-btn" @click="search">检索</button>
      </div>

      <main class="list-card" v-loading="loading">
        <div class="list-head">
          <p class="list-count">
            共 <b>{{ total }}</b> 道题
            <span v-if="hasFilter" class="filtered-badge">已筛选</span>
          </p>
          <div class="list-tools">
            <button v-if="hasFilter" type="button" class="clear-btn" @click="resetFilter">清除筛选</button>
            <span class="list-hint">按题号</span>
          </div>
        </div>

        <div class="table-head" aria-hidden="true">
          <span>ID</span>
          <span>题目</span>
          <span>难度</span>
          <span>通过率</span>
        </div>

        <ul v-if="problems.length" class="entry-list">
          <li v-for="row in problems" :key="row.id">
            <router-link :to="`/problems/${row.id}`" class="entry">
              <span class="entry-id">#{{ row.id }}</span>
              <span class="entry-main">
                <span class="entry-title">
                  <span v-if="rowStatus(row.id) === 'ac'" class="my-mark mark-ac" title="已通过" aria-label="已通过">✓</span>
                  <span v-else-if="rowStatus(row.id) === 'attempted'" class="my-mark mark-attempted" title="尝试过" aria-label="尝试过">·</span>
                  {{ row.title }}
                </span>
                <span v-if="row.tags && row.tags.length" class="entry-tags">
                  <span v-for="t in row.tags" :key="t" class="mini-tag">{{ t }}</span>
                </span>
              </span>
              <span class="oj-diff entry-diff" :class="`diff-${row.difficulty}`">{{ DIFFICULTY_MAP[row.difficulty]?.label || '未知' }}</span>
              <span class="entry-rate">
                <span class="rate-bar" aria-hidden="true">
                  <span class="rate-fill" :style="{ width: `${passRate(row)}%`, background: rateColorVar(passRate(row)) }" />
                </span>
                <span class="rate-value tabular" :class="rateClass(passRate(row))">{{ passRate(row) }}%</span>
                <span class="rate-frac">{{ row.acceptedCount }}/{{ row.submitCount }}</span>
              </span>
            </router-link>
          </li>
        </ul>
        <el-empty v-else-if="!loading" description="没有符合条件的题目，换个筛选条件试试" :image-size="64" />

        <div v-if="total > size" class="pager-wrap">
          <el-pagination
            v-model:current-page="page"
            :page-size="size"
            :total="total"
            :pager-count="7"
            layout="prev, pager, next, jumper"
            small
            @current-change="load"
          />
        </div>
      </main>
    </section>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { getProblemOverview, pageProblems, type Problem } from '@/api/problem'
import { getLeaderboard } from '@/api/leaderboard'
import { getMyStatusMap } from '@/api/submission'
import { listAnnouncements, type AnnouncementItem } from '@/api/announcement'
import { useUserStore } from '@/stores/user'
import { DIFFICULTY_MAP } from '@/utils/status'

const route = useRoute()
const router = useRouter()
const userStore = useUserStore()

const problems = ref<Problem[]>([])
const keyword = ref('')
const tag = ref('')
const tagOptions = ref<string[]>([])
const difficulty = ref<number | undefined>()
const page = ref(1)
const size = ref(10)
const total = ref(0)
const loading = ref(false)

/** 从 URL query 恢复筛选状态（分享链接/刷新不丢筛选）；非法值静默回落 */
function readQuery() {
  const kw = route.query.keyword
  const tg = route.query.tag
  const diff = Number(route.query.difficulty)
  const p = Number(route.query.page)
  keyword.value = typeof kw === 'string' ? kw : ''
  tag.value = typeof tg === 'string' ? tg : ''
  difficulty.value = diff === 1 || diff === 2 || diff === 3 ? diff : undefined
  page.value = Number.isInteger(p) && p >= 1 ? Math.min(p, 1000) : 1
}

// ---------- 题库行内"我的状态"（API.md v1.11 §4.4d） ----------
const myStatus = ref<Map<number, 'ac' | 'attempted'>>(new Map())

function rowStatus(id: number): 'ac' | 'attempted' | undefined {
  return myStatus.value.get(id)
}

/** 一次请求拿全量做过题的 AC 标记；未登录清空。失败不渲染标记（不影响列表） */
async function loadMyStatus() {
  if (!userStore.isLoggedIn) {
    myStatus.value = new Map()
    return
  }
  try {
    const m = await getMyStatusMap()
    const map = new Map<number, 'ac' | 'attempted'>()
    for (const id of m.acProblemIds || []) map.set(id, 'ac')
    for (const id of m.attemptedProblemIds || []) if (!map.has(id)) map.set(id, 'attempted')
    myStatus.value = map
  } catch {
    // 拦截器已提示；标记是增强信息，缺省不渲染
  }
}

/** 概览数据集中管理：一次渲染、一处兜底 */
const overview = ref({ totalProblems: 0, solvers: 0, submits: 0, acceptRate: 0 })

const difficultyOptions: { label: string; value: number | undefined }[] = [
  { label: '全部', value: undefined },
  { label: '简单', value: 1 },
  { label: '中等', value: 2 },
  { label: '困难', value: 3 },
]

const hasFilter = computed(() => !!keyword.value || !!tag.value || difficulty.value !== undefined)

function passRate(row: Problem) {
  if (!row.submitCount) return 0
  return Math.round((row.acceptedCount / row.submitCount) * 100)
}

/** 通过率三级：与列表里的文字配色同一套阈值（60/30） */
function rateClass(rate: number) {
  if (rate >= 60) return 'rate-good'
  if (rate >= 30) return 'rate-mid'
  return 'rate-low'
}

function rateColorVar(rate: number) {
  if (rate >= 60) return 'var(--oj-success)'
  if (rate >= 30) return 'var(--oj-warning)'
  return 'var(--oj-danger)'
}

const announcements = ref<AnnouncementItem[]>([])
let loadVersion = 0

function pickDifficulty(value: number | undefined) {
  if (difficulty.value === value) return
  difficulty.value = value
  search()
}

function resetFilter() {
  keyword.value = ''
  tag.value = ''
  difficulty.value = undefined
  search()
}

function search() {
  page.value = 1
  load()
}

async function load() {
  const version = ++loadVersion
  loading.value = true
  try {
    const data = await pageProblems({
      keyword: keyword.value || undefined,
      difficulty: difficulty.value,
      tag: tag.value || undefined,
      page: page.value,
      size: size.value,
    })
    if (version !== loadVersion) return
    problems.value = data.records || []
    total.value = data.total
  } finally {
    if (version === loadVersion) loading.value = false
  }
}

/**
 * 概览条的数据源：
 * - totalProblems / tags 走 /api/problems/overview（全站维度，不随分页变化）
 * - 解题者 / 累计提交 / 整体通过率 走排行榜 summary（后端是整表聚合）
 */
onMounted(() => {
  readQuery()
  load()
  loadMyStatus()

  getLeaderboard({ page: 1, size: 1 })
    .then((lb) => {
      overview.value.solvers = lb.summary?.solverCount ?? 0
      overview.value.submits = lb.summary?.totalSubmit ?? 0
      overview.value.acceptRate = lb.summary?.overallAcceptRate ?? 0
    })
    .catch(() => { /* 概览失败不影响列表 */ })

  getProblemOverview()
    .then((data) => {
      overview.value.totalProblems = data.totalProblems
      tagOptions.value = data.tags
    })
    .catch(() => { /* 概览失败不影响列表 */ })

  listAnnouncements()
    .then((list) => { announcements.value = list.slice(0, 5) })
    .catch(() => { /* 公告失败不影响列表 */ })
})

/** 把当前筛选序列化为 URL query（空值不占参数） */
function querySnapshot() {
  return {
    keyword: keyword.value || undefined,
    tag: tag.value || undefined,
    difficulty: difficulty.value ?? undefined,
    page: page.value > 1 ? page.value : undefined,
  }
}

/**
 * 筛选/页码 → URL（replace 不进历史栈）：
 * 刷新不丢筛选，「中等 + 动态规划 第 3 页」可以直接发给别人打开还原。
 */
watch([keyword, tag, difficulty, page], () => {
  if (sameAsQuery(querySnapshot())) return
  router.replace({ query: querySnapshot() })
})

/** 当前筛选状态是否与 URL query 一致（用于识别自己 replace 的回声，防止同步环） */
function sameAsQuery(next: ReturnType<typeof querySnapshot>) {
  const current = route.query
  return (
    String(current.keyword ?? '') === (next.keyword ?? '') &&
    String(current.tag ?? '') === (next.tag ?? '') &&
    String(current.difficulty ?? '') === String(next.difficulty ?? '') &&
    Number(current.page ?? 1) === (next.page ?? 1)
  )
}

// 浏览器前进/后退：URL 变了 → 把筛选拨回对应状态（组件复用，setup 不会重跑）。
// 先比对：若是本组件 router.replace 造成的回声（筛选其实没变），只刷新标记不重复 load——
// 否则 keyword 的 v-model 会逐键触发"URL→load"链路，打一个词发 N 个请求。
watch(
  () => route.query,
  () => {
    if (route.name !== 'problems') return
    if (sameAsQuery(querySnapshot())) {
      loadMyStatus()
      return
    }
    readQuery()
    load()
    // 从题目页返回时刷新"我的状态"标记（可能刚 AC 了一题）
    loadMyStatus()
  },
)

// 登录/登出切换：标记随之加载或清空
watch(() => userStore.isLoggedIn, loadMyStatus)
</script>

<style scoped>
.problem-page {
  max-width: 1080px;
  margin: 0 auto;
  padding: 36px 32px 64px;
}

/* ---------- 页头：靠对齐和字重分层，不堆大字和统计块 ---------- */
.problem-head {
  display: flex;
  align-items: flex-end;
  justify-content: space-between;
  gap: 24px;
  padding-bottom: 18px;
  border-bottom: 1px solid var(--oj-border);
}
.head-copy {
  min-width: 0;
}
.problem-title {
  margin: 0;
  font-family: var(--oj-font-display);
  font-size: 28px;
  font-weight: 700;
  line-height: 1.15;
  letter-spacing: -0.035em;
  color: var(--oj-text);
}
.problem-sub {
  margin: 7px 0 0;
  max-width: 52ch;
  font-size: 12.5px;
  line-height: 1.65;
  color: var(--oj-text-sub);
}
.head-meta {
  display: flex;
  flex-wrap: wrap;
  justify-content: flex-end;
  gap: 8px 16px;
  margin: 0;
  font-family: var(--oj-font-mono);
  font-size: 10.5px;
  line-height: 1;
  color: var(--oj-text-weak);
  flex-shrink: 0;
}
.head-meta span {
  display: inline-flex;
  align-items: baseline;
  gap: 3px;
}
.head-meta b {
  font-size: 13px;
  font-weight: 600;
  color: var(--oj-text-ink);
  letter-spacing: -0.02em;
}

/* 公告只做一条细线，不做卡片、不做彩色侧条 */
.notice-line {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 10px 0;
  border-bottom: 1px solid var(--oj-border);
  font-size: 12px;
  color: var(--oj-text-sub);
}
.notice-label {
  flex-shrink: 0;
  font-family: var(--oj-font-mono);
  font-size: 9.5px;
  letter-spacing: 0.06em;
  color: var(--oj-primary);
}
.notice-title {
  flex-shrink: 0;
  font-weight: 600;
  color: var(--oj-text);
}
.notice-body {
  min-width: 0;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}
.notice-arrow {
  flex-shrink: 0;
  margin-left: auto;
  color: var(--oj-text-weak);
  font-size: 11px;
}

/* ---------- 一张索引表：筛选条和列表在同一个面里 ---------- */
.index-panel {
  margin-top: 18px;
  border: 1px solid var(--oj-border);
  border-radius: 2px;
  background: var(--oj-card);
  overflow: hidden;
}

.filter-bar {
  display: grid;
  grid-template-columns: minmax(220px, 1fr) auto 156px 64px;
  gap: 10px;
  align-items: center;
  padding: 12px 16px;
  background: var(--oj-gray-50);
  border-bottom: 1px solid var(--oj-border);
}
.search-wrap {
  min-width: 0;
}
.search-input,
.tag-select {
  width: 100%;
}
:deep(.search-input .el-input__wrapper),
:deep(.tag-select .el-select__wrapper) {
  min-height: 34px;
  padding: 0 10px;
  border-radius: 4px;
  background: var(--oj-card);
  box-shadow: 0 0 0 1px var(--oj-border) inset;
}
:deep(.search-input .el-input__wrapper.is-focus),
:deep(.tag-select .el-select__wrapper.is-focused) {
  box-shadow: 0 0 0 1px var(--oj-primary) inset;
}
:deep(.search-input .el-input__prefix) {
  color: var(--oj-text-weak);
}

.diff-filter {
  display: inline-flex;
  align-items: center;
  gap: 2px;
  padding: 2px;
  border: 1px solid var(--oj-border);
  border-radius: 5px;
  background: var(--oj-card);
}
.diff-item {
  display: inline-flex;
  align-items: center;
  gap: 5px;
  height: 28px;
  padding: 0 9px;
  border: 0;
  border-radius: 3px;
  background: transparent;
  font-family: inherit;
  font-size: 11.5px;
  color: var(--oj-text-sub);
  white-space: nowrap;
  cursor: pointer;
  transition: background var(--oj-duration) var(--oj-ease), color var(--oj-duration) var(--oj-ease);
}
.diff-item:hover {
  background: var(--oj-gray-100);
  color: var(--oj-text);
}
.diff-item.active {
  background: var(--oj-gray-100);
  color: var(--oj-text);
  font-weight: 600;
}
.diff-dot {
  width: 5px;
  height: 5px;
  border-radius: 50%;
  flex-shrink: 0;
}
.dot-1 { background: var(--oj-success); }
.dot-2 { background: var(--oj-warning); }
.dot-3 { background: var(--oj-danger); }

.search-btn {
  height: 34px;
  border: 1px solid var(--oj-text);
  border-radius: 4px;
  background: var(--oj-text);
  color: var(--oj-card);
  font-family: inherit;
  font-size: 12px;
  font-weight: 600;
  letter-spacing: 0.02em;
  cursor: pointer;
  transition: background var(--oj-duration) var(--oj-ease), border-color var(--oj-duration) var(--oj-ease);
}
.search-btn:hover {
  background: var(--oj-primary);
  border-color: var(--oj-primary);
}
.search-btn:active {
  transform: translateY(1px);
}

.list-card {
  min-width: 0;
  min-height: 320px;
  background: var(--oj-card);
}
.list-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  height: 44px;
  padding: 0 16px;
  border-bottom: 1px solid var(--oj-border);
}
.list-count {
  font-size: 12px;
  color: var(--oj-text-sub);
}
.list-count b {
  margin: 0 3px;
  font-family: var(--oj-font-mono);
  font-size: 13px;
  color: var(--oj-text);
}
.filtered-badge {
  margin-left: 6px;
  font-family: var(--oj-font-mono);
  font-size: 9px;
  color: var(--oj-primary);
}
.list-tools {
  display: flex;
  align-items: center;
  gap: 12px;
}
.clear-btn {
  padding: 0;
  border: 0;
  background: none;
  font-family: inherit;
  font-size: 10.5px;
  color: var(--oj-text-sub);
  cursor: pointer;
}
.clear-btn:hover {
  color: var(--oj-primary);
}
.list-hint {
  font-family: var(--oj-font-mono);
  font-size: 9px;
  letter-spacing: 0.02em;
  color: var(--oj-text-weak);
}

.table-head,
.entry {
  display: grid;
  grid-template-columns: 50px minmax(0, 1fr) 76px 148px;
  gap: 14px;
  align-items: center;
  padding: 0 16px;
}
.table-head {
  height: 30px;
  border-bottom: 1px solid var(--oj-border);
  font-family: var(--oj-font-mono);
  font-size: 8.5px;
  letter-spacing: 0.1em;
  color: var(--oj-text-weak);
}

.entry-list {
  list-style: none;
  margin: 0;
  padding: 0;
}
.entry {
  min-height: 54px;
  border-bottom: 1px solid var(--oj-border);
  transition: background var(--oj-duration) var(--oj-ease);
}
.entry:last-child {
  border-bottom: 0;
}
.entry:hover {
  background: var(--oj-gray-50);
}
.entry-id {
  font-family: var(--oj-font-mono);
  font-size: 10.5px;
  color: var(--oj-text-weak);
}
.entry-main {
  min-width: 0;
  display: flex;
  flex-direction: column;
  gap: 3px;
}
.entry-title {
  font-size: 13.5px;
  font-weight: 600;
  line-height: 1.4;
  color: var(--oj-text);
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}
.entry:hover .entry-title {
  color: var(--oj-primary);
}
/* 行内"我的状态"：AC 绿勾实心圆 / 尝试过橙色圆点 */
.my-mark {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 14px;
  height: 14px;
  margin-right: 6px;
  border-radius: 50%;
  flex-shrink: 0;
  vertical-align: -2px;
}
.mark-ac {
  background: var(--oj-success);
  color: #fff;
  font-size: 9px;
  font-weight: 800;
  line-height: 1;
}
.mark-attempted {
  border: 1.5px solid var(--oj-warning);
  color: var(--oj-warning);
  font-size: 16px;
  font-weight: 800;
  line-height: 0;
}
/* 标签不再做成一颗颗小胶囊，只保留等宽小字和间隔点 */
.entry-tags {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  gap: 0;
}
.mini-tag {
  padding: 0;
  border: 0;
  background: none;
  font-family: var(--oj-font-mono);
  font-size: 9.5px;
  line-height: 1.4;
  color: var(--oj-text-sub);
}
.mini-tag + .mini-tag::before {
  content: '·';
  margin: 0 4px;
  color: var(--oj-border-strong);
}
.entry-diff {
  font-size: 11px;
  font-weight: 500;
}
.entry-rate {
  display: grid;
  grid-template-columns: minmax(32px, 1fr) 34px 46px;
  align-items: center;
  gap: 6px;
}
.rate-bar {
  height: 2px;
  background: var(--oj-gray-200);
  overflow: hidden;
}
.rate-fill {
  display: block;
  height: 100%;
  transition: width 0.3s var(--oj-ease);
}
.rate-value {
  font-family: var(--oj-font-mono);
  font-size: 10px;
  font-weight: 600;
  text-align: right;
}
.rate-frac {
  font-family: var(--oj-font-mono);
  font-size: 8.5px;
  color: var(--oj-text-weak);
  text-align: right;
}

.pager-wrap {
  display: flex;
  justify-content: flex-end;
  padding: 12px 16px;
  border-top: 1px solid var(--oj-border);
}

:deep(.el-empty) {
  padding: 48px 0;
}
:deep(.el-empty__description p) {
  font-size: 12px;
  color: var(--oj-text-sub);
}

@media (max-width: 820px) {
  .problem-page {
    padding: 28px 20px 56px;
  }
  .problem-head {
    flex-direction: column;
    align-items: flex-start;
    gap: 12px;
  }
  .head-meta {
    justify-content: flex-start;
  }
  .filter-bar {
    grid-template-columns: minmax(0, 1fr) 64px;
    grid-template-areas:
      "search search"
      "diff diff"
      "tag button";
    gap: 8px;
  }
  .search-wrap { grid-area: search; }
  .diff-filter {
    grid-area: diff;
    justify-self: start;
  }
  .tag-select { grid-area: tag; }
  .search-btn {
    grid-area: button;
    width: 100%;
  }
}

@media (max-width: 700px) {
  .problem-page {
    padding: 22px 16px 48px;
  }
  .problem-title {
    font-size: 24px;
  }
  .problem-sub {
    font-size: 12px;
  }
  .head-meta {
    gap: 7px 12px;
    font-size: 10px;
  }
  .notice-line {
    gap: 8px;
    padding: 9px 0;
    font-size: 11.5px;
  }
  .notice-body {
    display: none;
  }
  .index-panel {
    margin-top: 14px;
  }
  .filter-bar {
    padding: 10px 12px;
  }
  .list-head {
    height: 42px;
    padding: 0 12px;
  }
  .list-hint {
    display: none;
  }
  .table-head {
    display: none;
  }
  .entry {
    grid-template-columns: 38px minmax(0, 1fr);
    gap: 8px;
    padding: 10px 12px;
    min-height: 46px;
  }
  .entry-diff,
  .entry-rate {
    display: none;
  }
  .entry-title {
    font-size: 13px;
  }
  .pager-wrap {
    justify-content: center;
    padding: 12px;
  }
}
</style>
