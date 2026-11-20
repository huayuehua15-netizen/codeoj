<template>
  <div class="problem-page">
    <header class="page-head problem-head">
      <div class="head-main">
        <p class="kicker">PROBLEM SET</p>
        <h1 class="page-head-title">题库</h1>
        <p class="page-head-sub">按难度、标签检索题目。点开即读题面与样例，登录后可直接提交。</p>
        <router-link v-if="announcements.length" class="notice-line" to="/announcements">
          <span class="notice-tag">公告</span>
          <span class="notice-title">{{ announcements[0].title }}</span>
          <span class="notice-body">{{ announcements[0].content }}</span>
        </router-link>
      </div>
      <dl class="head-stats" aria-label="平台概览">
        <div class="stat">
          <dt>PROBLEMS</dt>
          <dd>{{ overview.totalProblems }}</dd>
        </div>
        <div class="stat">
          <dt>SOLVERS</dt>
          <dd>{{ overview.solvers }}</dd>
        </div>
        <div class="stat">
          <dt>SUBMISSIONS</dt>
          <dd>{{ overview.submits }}</dd>
        </div>
        <div class="stat">
          <dt>AC RATE</dt>
          <dd>{{ overview.acceptRate }}<small>%</small></dd>
        </div>
      </dl>
    </header>

    <div class="problem-body">
      <aside class="filter-rail" aria-label="题目筛选">
        <div class="filter-head">
          <span class="filter-title">筛选</span>
          <button v-if="hasFilter" type="button" class="clear-btn" @click="resetFilter">清除</button>
        </div>

        <div class="rail-field rail-field-keyword">
          <label class="rail-label" for="problem-search">关键词</label>
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

        <div class="rail-field rail-field-difficulty">
          <p class="rail-label">难度</p>
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
              <span v-else class="diff-dot dot-all" aria-hidden="true">·</span>
              <span>{{ opt.label }}</span>
              <span v-if="difficulty === opt.value" class="diff-check" aria-hidden="true">✓</span>
            </button>
          </div>
        </div>

        <div class="rail-field rail-field-tag">
          <p class="rail-label">标签</p>
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
        </div>

        <div class="rail-actions">
          <el-button type="primary" class="rail-search" @click="search">检索题目</el-button>
        </div>

        <p class="rail-note">通过率 = 该题 AC 提交数 / 全部提交数。标签按算法类型聚合。</p>
      </aside>

      <main class="list-card" v-loading="loading">
        <div class="list-head">
          <p class="list-count">
            共 <b>{{ total }}</b> 道题
            <span v-if="hasFilter" class="filtered-badge">已筛选</span>
          </p>
          <p class="list-hint">按题号排列</p>
        </div>

        <div class="table-head" aria-hidden="true">
          <span>ID</span>
          <span>题目</span>
          <span>难度</span>
          <span>通过率</span>
          <span></span>
        </div>

        <ul v-if="problems.length" class="entry-list">
          <li v-for="row in problems" :key="row.id">
            <router-link :to="`/problems/${row.id}`" class="entry">
              <span class="entry-id">#{{ row.id }}</span>
              <span class="entry-main">
                <span class="entry-title">{{ row.title }}</span>
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
              <span class="entry-go" aria-hidden="true">→</span>
            </router-link>
          </li>
        </ul>
        <el-empty v-else-if="!loading" description="没有符合条件的题目，换个筛选条件试试" :image-size="72" />

        <div v-if="total > size" class="pager-wrap">
          <el-pagination
            v-model:current-page="page"
            :page-size="size"
            :total="total"
            :pager-count="7"
            layout="prev, pager, next, jumper"
            @current-change="load"
          />
        </div>
      </main>
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
  load()

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
</script>

<style scoped>
.problem-page {
  max-width: 1280px;
  margin: 0 auto;
  padding: 44px 32px 64px;
}

.problem-head {
  align-items: flex-end;
}
.head-main {
  min-width: 0;
}
.notice-line {
  display: flex;
  align-items: center;
  gap: 10px;
  margin-top: 15px;
  padding: 7px 11px;
  max-width: 100%;
  background: var(--oj-card);
  border: 1px solid var(--oj-border);
  border-left: 3px solid var(--oj-primary);
  border-radius: 5px;
  font-size: 11.5px;
  color: var(--oj-text-sub);
  box-shadow: var(--oj-shadow-sm);
}
.notice-tag {
  flex-shrink: 0;
  font-family: var(--oj-font-mono);
  font-size: 8.5px;
  letter-spacing: 0.06em;
  color: var(--oj-primary);
  background: var(--oj-primary-soft);
  padding: 1px 5px;
  border-radius: 3px;
}
.notice-title {
  flex-shrink: 0;
  color: var(--oj-text);
  font-weight: 600;
}
.notice-body {
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.head-stats {
  display: flex;
  margin: 0;
  padding: 0;
  flex-shrink: 0;
}
.head-stats .stat {
  padding: 0 20px;
  border-left: 1px solid var(--oj-border);
  text-align: right;
}
.head-stats .stat:last-child {
  padding-right: 0;
}
.head-stats dt {
  font-family: var(--oj-font-mono);
  font-size: 8.5px;
  letter-spacing: 0.1em;
  color: var(--oj-text-weak);
}
.head-stats dd {
  margin: 5px 0 0;
  font-family: var(--oj-font-mono);
  font-size: 26px;
  line-height: 1;
  font-weight: 600;
  letter-spacing: -0.04em;
  color: var(--oj-text);
}
.head-stats dd small {
  margin-left: 1px;
  font-size: 11px;
  color: var(--oj-text-sub);
}

/* ---------- 左筛选 + 右列表 ---------- */
.problem-body {
  display: grid;
  grid-template-columns: 220px minmax(0, 1fr);
  gap: 22px;
  align-items: start;
}

.filter-rail {
  position: sticky;
  top: 84px;
  background: var(--oj-card);
  border: 1px solid var(--oj-border);
  border-radius: 10px;
  padding: 18px;
  box-shadow: var(--oj-shadow-sm);
}
.filter-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding-bottom: 12px;
  border-bottom: 1px solid var(--oj-border);
}
.filter-title {
  font-size: 12px;
  font-weight: 700;
}
.clear-btn {
  border: 0;
  background: none;
  padding: 0;
  font-family: inherit;
  font-size: 10.5px;
  color: var(--oj-text-sub);
  cursor: pointer;
}
.clear-btn:hover {
  color: var(--oj-primary);
}

.rail-field {
  min-width: 0;
}

.rail-label {
  display: block;
  margin: 16px 0 7px;
  font-family: var(--oj-font-mono);
  font-size: 9px;
  letter-spacing: 0.08em;
  color: var(--oj-text-sub);
}
.filter-head + .rail-field .rail-label {
  margin-top: 14px;
}
.search-input,
.tag-select {
  width: 100%;
}

.diff-filter {
  display: flex;
  flex-direction: column;
  gap: 3px;
}
.diff-item {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 7px 9px;
  border: 1px solid transparent;
  border-radius: 5px;
  background: transparent;
  font-family: inherit;
  font-size: 12px;
  color: var(--oj-text-sub);
  text-align: left;
  cursor: pointer;
  transition: background var(--oj-duration) var(--oj-ease), color var(--oj-duration) var(--oj-ease);
}
.diff-item:hover {
  background: var(--oj-gray-50);
  color: var(--oj-text);
}
.diff-item.active {
  background: var(--oj-primary-soft);
  border-color: var(--oj-info-border);
  color: var(--oj-text);
  font-weight: 600;
}
.diff-dot {
  width: 6px;
  height: 6px;
  border-radius: 50%;
  flex-shrink: 0;
}
.dot-all {
  width: auto;
  height: auto;
  font-family: var(--oj-font-mono);
  font-size: 9px;
  color: var(--oj-text-weak);
  line-height: 1;
}
.dot-1 { background: var(--oj-success); }
.dot-2 { background: var(--oj-warning); }
.dot-3 { background: var(--oj-danger); }
.diff-check {
  margin-left: auto;
  font-size: 10px;
  color: var(--oj-primary);
}

.rail-search {
  width: 100%;
  margin-top: 16px;
}
.rail-note {
  margin-top: 16px;
  padding-top: 12px;
  border-top: 1px solid var(--oj-border);
  font-size: 10px;
  line-height: 1.7;
  color: var(--oj-text-weak);
}

/* ---------- 列表 ---------- */
.list-card {
  min-width: 0;
  min-height: 300px;
  background: var(--oj-card);
  border: 1px solid var(--oj-border);
  border-radius: 10px;
  box-shadow: var(--oj-shadow-sm);
  overflow: hidden;
}
.list-head {
  height: 48px;
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
  padding: 0 18px;
  border-bottom: 1px solid var(--oj-border);
}
.list-count {
  font-size: 12px;
  color: var(--oj-text-sub);
}
.list-count b {
  margin: 0 2px;
  font-family: var(--oj-font-mono);
  font-size: 15px;
  color: var(--oj-text);
}
.filtered-badge {
  margin-left: 8px;
  padding: 1px 6px;
  border-radius: 3px;
  background: var(--oj-primary-soft);
  color: var(--oj-primary);
  font-size: 9px;
}
.list-hint {
  font-family: var(--oj-font-mono);
  font-size: 8.5px;
  letter-spacing: 0.06em;
  color: var(--oj-text-weak);
}

.table-head,
.entry {
  display: grid;
  grid-template-columns: 52px minmax(0, 1fr) 72px 176px 22px;
  gap: 14px;
  align-items: center;
  padding: 0 18px;
}
.table-head {
  height: 32px;
  background: var(--oj-gray-50);
  border-bottom: 1px solid var(--oj-border);
  font-family: var(--oj-font-mono);
  font-size: 8.5px;
  letter-spacing: 0.09em;
  color: var(--oj-text-weak);
}

.entry-list {
  list-style: none;
  margin: 0;
  padding: 0;
}
.entry {
  min-height: 58px;
  border-bottom: 1px solid var(--oj-border);
  transition: background var(--oj-duration) var(--oj-ease), box-shadow var(--oj-duration) var(--oj-ease);
}
.entry:last-child {
  border-bottom: 0;
}
.entry:hover {
  background: var(--oj-gray-50);
  box-shadow: inset 2px 0 0 var(--oj-primary);
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
  gap: 4px;
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
.entry-tags {
  display: flex;
  flex-wrap: wrap;
  gap: 4px;
}
.mini-tag {
  padding: 0 5px;
  border: 1px solid var(--oj-border);
  border-radius: 3px;
  background: var(--oj-gray-50);
  color: var(--oj-text-weak);
  font-family: var(--oj-font-mono);
  font-size: 8px;
  line-height: 15px;
}
.entry-diff {
  font-size: 11.5px;
}
.entry-rate {
  display: grid;
  grid-template-columns: minmax(44px, 1fr) 38px 50px;
  align-items: center;
  gap: 7px;
}
.rate-bar {
  height: 3px;
  border-radius: 99px;
  background: var(--oj-gray-100);
  overflow: hidden;
}
.rate-fill {
  display: block;
  height: 100%;
  border-radius: 99px;
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
.entry-go {
  display: flex;
  justify-content: flex-end;
  font-size: 12px;
  color: var(--oj-text-weak);
  transition: color var(--oj-duration) var(--oj-ease), transform var(--oj-duration) var(--oj-ease);
}
.entry:hover .entry-go {
  color: var(--oj-primary);
  transform: translateX(2px);
}

.pager-wrap {
  display: flex;
  justify-content: flex-end;
  padding: 16px 18px 18px;
  border-top: 1px solid var(--oj-border);
}

@media (max-width: 1080px) {
  .head-stats {
    width: 100%;
    margin-top: 18px;
    padding-top: 14px;
    border-top: 1px solid var(--oj-border);
    display: grid;
    grid-template-columns: repeat(4, minmax(0, 1fr));
    gap: 0;
  }
  .head-stats .stat {
    padding: 0 0 0 18px;
    border-left: 1px solid var(--oj-border);
    text-align: left;
  }
  .head-stats .stat:first-child {
    padding-left: 0;
    border-left: 0;
  }
  .head-stats .stat:last-child {
    padding-right: 0;
  }
  .problem-body {
    grid-template-columns: 1fr;
  }
  /* 筛选区改为一行工具条：关键词 / 难度 / 标签 / 按钮同排，不再出现
     “标签在左、输入框在右”的错位两列布局。 */
  .filter-rail {
    position: static;
    display: grid;
    grid-template-columns: minmax(190px, 1.15fr) minmax(280px, 1.4fr) minmax(170px, 0.9fr) auto;
    gap: 14px 18px;
    align-items: end;
  }
  .filter-head {
    grid-column: 1 / -1;
    padding-bottom: 10px;
  }
  .filter-head + .rail-field .rail-label,
  .rail-field .rail-label {
    margin: 0 0 7px;
  }
  .diff-filter {
    flex-direction: row;
    flex-wrap: wrap;
    gap: 4px;
  }
  .diff-item {
    flex: 1 1 auto;
    justify-content: center;
    padding: 6px 8px;
    font-size: 11px;
  }
  .diff-check {
    margin-left: 4px;
  }
  .rail-search {
    width: 100%;
    margin-top: 0;
    height: 32px;
  }
  .rail-note {
    display: none;
  }
}
@media (max-width: 880px) {
  /* 平板窄宽度：关键词独占一行，难度与标签并排，避免工具条横向溢出 */
  .filter-rail {
    grid-template-columns: 1fr 1fr;
    gap: 12px 18px;
  }
  .rail-field-keyword,
  .rail-actions {
    grid-column: 1 / -1;
  }
  .rail-search {
    margin-top: 0;
  }
}
@media (max-width: 700px) {
  .problem-page {
    padding: 28px 18px 52px;
  }
  .head-stats {
    display: grid;
    grid-template-columns: repeat(2, minmax(0, 1fr));
    gap: 0;
  }
  .head-stats .stat {
    padding: 0 0 10px 14px;
    border-left: 1px solid var(--oj-border);
    text-align: left;
  }
  .head-stats .stat:first-child {
    padding-left: 0;
    border-left: 0;
  }
  .head-stats .stat:nth-child(n + 3) {
    padding-top: 10px;
    border-top: 1px solid var(--oj-border);
  }
  .filter-rail {
    grid-template-columns: 1fr;
    gap: 12px;
  }
  .rail-field .rail-label,
  .filter-head + .rail-field .rail-label {
    margin: 0 0 7px;
  }
  .diff-filter {
    flex-direction: column;
    flex-wrap: nowrap;
  }
  .diff-item {
    justify-content: flex-start;
    padding: 7px 9px;
    font-size: 12px;
  }
  .rail-actions {
    margin-top: 2px;
  }
  .rail-search {
    margin-top: 0;
  }
  .notice-body {
    display: none;
  }
  .table-head {
    display: none;
  }
  .entry {
    grid-template-columns: 46px minmax(0, 1fr) 20px;
    gap: 10px;
    padding: 12px 16px;
  }
  .entry-diff,
  .entry-rate {
    display: none;
  }
  .head-stats dd {
    font-size: 22px;
  }
}
</style>
