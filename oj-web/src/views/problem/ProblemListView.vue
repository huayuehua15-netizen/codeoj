<template>
  <div class="page-container">
    <header class="oj-stage">
      <div>
        <h1 class="oj-stage-title">题库</h1>
        <p class="oj-stage-sub">从 A+B 开始，把每一道题都变成自己的</p>
      </div>
    </header>
    <!-- 顶部概览 -->
    <section class="oj-banner card" aria-label="平台概览">
      <div class="oj-banner-item">
        <span class="oj-banner-num">{{ overview.totalProblems }}</span>
        <span class="oj-banner-label">题目总数</span>
      </div>
      <div class="oj-banner-divider" aria-hidden="true"></div>
      <div class="oj-banner-item">
        <span class="oj-banner-num">{{ overview.solvers }}</span>
        <span class="oj-banner-label">解题者</span>
      </div>
      <div class="oj-banner-divider" aria-hidden="true"></div>
      <div class="oj-banner-item">
        <span class="oj-banner-num">{{ overview.submits }}</span>
        <span class="oj-banner-label">累计提交</span>
      </div>
      <div class="oj-banner-divider" aria-hidden="true"></div>
      <div class="oj-banner-item">
        <span class="oj-banner-num">{{ overview.acceptRate }}<small>%</small></span>
        <span class="oj-banner-label">整体通过率</span>
      </div>
      <p class="oj-banner-note">写下第一行代码<br />看看能过几个测试用例</p>
    </section>

    <!-- 公告：没有公告时整块不渲染，不留白 -->
    <div v-if="announcements.length" class="card notice-bar">
      <span class="notice-icon" aria-hidden="true">
        <svg viewBox="0 0 24 24" width="16" height="16" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
          <path d="M3 10 L3 14 L7 14 L13 18 L13 6 L7 10 Z" /><path d="M17 9 A5 5 0 0 1 17 15" />
        </svg>
      </span>
      <el-carousel height="28px" direction="vertical" :autoplay="true" :interval="4500" indicator-position="none" arrow="never">
        <el-carousel-item v-for="a in announcements" :key="a.id">
          <span class="notice-title">【{{ a.title }}】</span>
          <span class="notice-content">{{ a.content }}</span>
        </el-carousel-item>
      </el-carousel>
    </div>

    <div class="card">
      <header class="list-head">
        <div>
          <h2>题库</h2>
          <p class="sub">
            共 <b>{{ total }}</b> 道题匹配当前筛选
            <template v-if="hasFilter">（<button type="button" class="link-btn" @click="resetFilter">清除筛选</button>）</template>
          </p>
        </div>
      </header>

      <!-- 筛选条：难度用分段按钮而非下拉，一步点到、少一次弹层 -->
      <div class="toolbar">
        <el-input
          v-model="keyword"
          placeholder="搜索题目名称"
          class="search-input"
          clearable
          @keyup.enter="search"
          @clear="search"
        >
          <template #prefix>
            <svg viewBox="0 0 24 24" width="15" height="15" fill="none" stroke="currentColor" stroke-width="2.2" stroke-linecap="round" aria-hidden="true">
              <circle cx="11" cy="11" r="7" /><path d="M20 20 L16.5 16.5" />
            </svg>
          </template>
        </el-input>

        <div class="seg" role="group" aria-label="按难度筛选">
          <button
            v-for="opt in difficultyOptions"
            :key="String(opt.value)"
            type="button"
            class="seg-btn"
            :class="{ active: difficulty === opt.value }"
            :aria-pressed="difficulty === opt.value"
            @click="pickDifficulty(opt.value)"
          >
            {{ opt.label }}
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

        <el-button type="primary" @click="search">搜索</el-button>
      </div>

      <el-table :data="problems" v-loading="loading" row-key="id" empty-text="没有符合条件的题目">
        <el-table-column label="#" width="72" align="center">
          <template #default="{ $index }">
            <span class="oj-badge-id">{{ (page - 1) * size + $index + 1 }}</span>
          </template>
        </el-table-column>

        <el-table-column label="题目" min-width="300">
          <template #default="{ row }">
            <router-link :to="`/problems/${row.id}`" class="title-link">{{ row.title }}</router-link>
            <div v-if="row.tags && row.tags.length" class="row-tags">
              <span v-for="t in row.tags" :key="t" class="mini-tag">{{ t }}</span>
            </div>
          </template>
        </el-table-column>

        <el-table-column label="难度" width="96" align="center">
          <template #default="{ row }">
            <span class="oj-diff" :class="`diff-${row.difficulty}`">{{ DIFFICULTY_MAP[row.difficulty]?.label || '未知' }}</span>
          </template>
        </el-table-column>

        <el-table-column label="通过率" width="210">
          <template #default="{ row }">
            <div class="rate-cell">
              <el-progress
                :percentage="passRate(row)"
                :stroke-width="8"
                :show-text="false"
                :color="rateColorVar(passRate(row))"
              />
              <span class="rate-text tabular" :class="rateClass(passRate(row))">
                {{ passRate(row) }}%
                <em>({{ row.acceptedCount }}/{{ row.submitCount }})</em>
              </span>
            </div>
          </template>
        </el-table-column>
      </el-table>

      <el-pagination
        v-if="total > size"
        v-model:current-page="page"
        :page-size="size"
        :total="total"
        :pager-count="7"
        layout="prev, pager, next, jumper"
        class="pager"
        @current-change="load"
      />
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { getProblemOverview, pageProblems, type Problem } from '@/api/problem'
import { getLeaderboard } from '@/api/leaderboard'
import { listAnnouncements, type AnnouncementItem } from '@/api/announcement'
import { DIFFICULTY_MAP } from '@/utils/status'

const problems = ref<Problem[]>([])
const keyword = ref('')
const tag = ref('')
const tagOptions = ref<string[]>([])
const difficulty = ref<number | undefined>()
const page = ref(1)
const size = ref(10)
const total = ref(0)
const loading = ref(false)

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

/**
 * el-progress 的 color 走内联 style，认 CSS 变量字符串，
 * 这样配色仍然由 index.css 单点控制，SFC 里不写裸色值（P2-18）。
 */
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
 * - totalProblems / tags 走 /api/problems/overview（全站维度，不随分页变化，P2-7）
 * - 解题者 / 累计提交 / 整体通过率 走排行榜 summary：后端是**全表聚合**（COUNT(*) + COUNT(DISTINCT)），
 *   不是当前页求和（P2-8）。三者任一取不到就保持 0，横幅降级但不报错。
 */
onMounted(() => {
  load()

  getLeaderboard({ page: 1, size: 1 })
    .then((lb) => {
      overview.value.solvers = lb.summary?.solverCount ?? 0
      overview.value.submits = lb.summary?.totalSubmit ?? 0
      overview.value.acceptRate = lb.summary?.overallAcceptRate ?? 0
    })
    .catch(() => { /* 横幅统计失败不影响列表 */ })

  getProblemOverview()
    .then((data) => {
      overview.value.totalProblems = data.totalProblems
      tagOptions.value = data.tags
    })
    .catch(() => { /* 概览失败不影响列表 */ })

  listAnnouncements()
    .then((list) => { announcements.value = list.slice(0, 5) })
    .catch(() => { /* 公告栏失败不影响列表 */ })
})
</script>

<style scoped>
.oj-banner-num small {
  font-size: 14px;
  margin-left: 1px;
}

.notice-bar {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 10px 24px;
  margin-bottom: 16px;
}
.notice-icon {
  display: inline-flex;
  align-items: center;
  color: var(--oj-warning);
  flex-shrink: 0;
}
.notice-bar :deep(.el-carousel) {
  flex: 1;
  min-width: 0;
}
.notice-bar :deep(.el-carousel__container) {
  height: 28px;
}
.notice-title {
  color: var(--oj-warning);
  font-weight: 700;
  font-size: 13px;
  margin-right: 8px;
}
.notice-content {
  color: var(--oj-gray-600);
  font-size: 13px;
}

.list-head {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 16px;
  margin-bottom: 16px;
}
.list-head h2 {
  margin: 0;
}
.list-head .sub {
  margin: 6px 0 0;
  font-size: 12.5px;
  color: var(--oj-text-sub);
}
.list-head .sub b {
  color: var(--oj-primary);
  font-variant-numeric: tabular-nums;
}
.link-btn {
  border: none;
  background: none;
  padding: 0;
  font: inherit;
  color: var(--oj-primary);
  cursor: pointer;
  text-decoration: underline;
  text-underline-offset: 2px;
}

.toolbar {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  gap: 12px;
  margin-bottom: 16px;
}
.search-input {
  width: 240px;
}
.tag-select {
  width: 150px;
}

/* 分段控件：难度筛选的"全部/简单/中等/困难" */
.seg {
  display: inline-flex;
  padding: 3px;
  gap: 2px;
  border-radius: var(--oj-radius-sm);
  background: var(--oj-gray-50);
  border: 1px solid var(--oj-border);
}
.seg-btn {
  border: none;
  background: transparent;
  padding: 5px 13px;
  border-radius: 6px;
  font-family: inherit;
  font-size: 13px;
  font-weight: 600;
  color: var(--oj-gray-500);
  cursor: pointer;
  transition: background var(--oj-duration) var(--oj-ease),
    color var(--oj-duration) var(--oj-ease);
}
.seg-btn:hover {
  color: var(--oj-primary);
}
.seg-btn.active {
  background: var(--oj-gray-100);
  color: var(--oj-primary);
  box-shadow: var(--oj-shadow-sm);
}

.title-link {
  color: var(--oj-gray-800);
  font-weight: 600;
  font-size: 14.5px;
  transition: color var(--oj-duration) var(--oj-ease);
}
.title-link:hover {
  color: var(--oj-primary);
}
.row-tags {
  display: flex;
  flex-wrap: wrap;
  gap: 5px;
  margin-top: 5px;
}
.mini-tag {
  padding: 1px 7px;
  border-radius: var(--oj-radius-pill);
  background: var(--oj-gray-50);
  border: 1px solid var(--oj-border);
  color: var(--oj-gray-500);
  font-size: 11.5px;
  line-height: 1.5;
}

.rate-cell {
  display: flex;
  align-items: center;
  gap: 10px;
}
.rate-cell :deep(.el-progress) {
  flex: 1;
  min-width: 60px;
}
.rate-text {
  font-size: 12px;
  font-weight: 700;
  white-space: nowrap;
  flex-shrink: 0;
}
.rate-text em {
  font-style: normal;
  font-weight: 400;
  color: var(--oj-text-weak);
}

.pager {
  margin-top: 16px;
  justify-content: flex-end;
}

@media (max-width: 768px) {
  .search-input,
  .tag-select {
    width: 100%;
  }
  .toolbar {
    gap: 8px;
  }
}
</style>
