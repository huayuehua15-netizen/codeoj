<template>
  <div class="submission-page">
    <header class="page-head log-head">
      <div>
        <p class="kicker">SUBMISSIONS</p>
        <h1 class="page-head-title">提交记录</h1>
        <p class="page-head-sub">每一次提交都有完整判题明细。按题目、状态或时间筛选，点开查看用例级结果。</p>
      </div>
      <dl class="log-totals" aria-label="个人提交概览">
        <div class="total-item">
          <dt>SUBMITS</dt>
          <dd>{{ summary.totalSubmit }}</dd>
        </div>
        <div class="total-item">
          <dt>ACCEPTED</dt>
          <dd>{{ summary.totalAccepted }}</dd>
        </div>
        <div class="total-item">
          <dt>AC RATE</dt>
          <dd>{{ summary.acceptRate }}<small>%</small></dd>
        </div>
        <div class="total-item">
          <dt>SOLVED</dt>
          <dd>{{ summary.solvedCount }}</dd>
        </div>
      </dl>
    </header>

    <!-- 筛选：一张横向的工具条 -->
    <section class="query-bar" aria-label="筛选提交记录">
      <div class="query-field query-pid">
        <label class="query-label" for="pid">题目编号</label>
        <el-input
          id="pid"
          v-model="problemId"
          placeholder="如 1001"
          clearable
          @keyup.enter="search"
          @clear="search"
        />
      </div>
      <div class="query-field query-status">
        <label class="query-label">判题状态</label>
        <el-select v-model="status" placeholder="全部状态" clearable @change="search">
          <el-option v-for="(v, k) in STATUS_MAP" :key="k" :label="v.label" :value="Number(k)" />
        </el-select>
      </div>
      <div class="query-field query-range">
        <label class="query-label">时间范围</label>
        <el-date-picker
          v-model="timeRange"
          type="datetimerange"
          start-placeholder="开始时间"
          end-placeholder="结束时间"
          value-format="YYYY-MM-DDTHH:mm:ss"
          @change="onRangeChange"
        />
      </div>
      <div class="query-presets" role="group" aria-label="快捷时间范围">
        <button
          v-for="p in QUICK_RANGES"
          :key="p.key"
          class="quick-btn"
          :class="{ active: activePreset === p.key }"
          :aria-pressed="activePreset === p.key"
          type="button"
          @click="applyPreset(p.key)"
        >
          {{ p.label }}
        </button>
      </div>
      <div class="query-actions">
        <el-button type="primary" @click="search">筛选</el-button>
        <button v-if="hasFilter" type="button" class="reset-link" @click="reset">清除</button>
      </div>
    </section>

    <section class="table-card">
      <div class="list-head">
        <div class="list-title-block">
          <span class="kicker">LOG</span>
          <h2 class="list-title">全部记录</h2>
        </div>
        <p class="list-meta">按提交时间倒序 · 共 {{ total }} 条</p>
      </div>

      <el-table :data="records" v-loading="loading" row-key="id" class="log-table" empty-text="没有符合条件的提交记录">
        <el-table-column label="编号" width="96">
          <template #default="{ row }">
            <span class="id-cell mono">#{{ String(row.id).padStart(5, '0') }}</span>
          </template>
        </el-table-column>

        <el-table-column label="题目" min-width="250">
          <template #default="{ row }">
            <div class="problem-cell">
              <span class="oj-badge-id">{{ row.problemId }}</span>
              <router-link :to="`/problems/${row.problemId}`" class="title-link">
                {{ row.problemTitle || `题目 ${row.problemId}` }}
              </router-link>
            </div>
          </template>
        </el-table-column>

        <el-table-column label="语言" width="104">
          <template #default="{ row }">
            <span class="lang-text mono">{{ languageLabel(row.language) }}</span>
          </template>
        </el-table-column>

        <el-table-column label="状态" width="128">
          <template #default="{ row }">
            <span class="status-cell" :style="{ color: statusColor(row.status) }">
              <span class="status-dot" :style="{ background: statusColor(row.status) }" aria-hidden="true" />
              {{ statusLabel(row.status) }}
            </span>
          </template>
        </el-table-column>

        <el-table-column label="耗时" width="100" align="right">
          <template #default="{ row }">
            <span class="mono num" :class="{ dim: row.timeUsedMs == null }">{{ formatTimeUsed(row.timeUsedMs) }}</span>
          </template>
        </el-table-column>

        <el-table-column label="内存" width="104" align="right">
          <template #default="{ row }">
            <span class="mono num" :class="{ dim: row.memoryUsedKb == null }">{{ formatMemory(row.memoryUsedKb) }}</span>
          </template>
        </el-table-column>

        <el-table-column label="提交时间" width="132" align="right">
          <template #default="{ row }">
            <span class="submit-time" :title="formatDateTime(row.createdAt)">{{ relativeTime(row.createdAt) }}</span>
          </template>
        </el-table-column>

        <el-table-column label="" width="48" align="right">
          <template #default="{ row }">
            <router-link class="row-go" :to="`/submissions/${row.id}`" :aria-label="`查看提交 ${row.id} 详情`">
              <svg viewBox="0 0 24 24" width="13" height="13" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round" aria-hidden="true">
                <path d="M5 12 L19 12 M13 6 L19 12 L13 18" />
              </svg>
            </router-link>
          </template>
        </el-table-column>
      </el-table>

      <div v-if="total > 0" class="table-foot">
        <el-pagination
          v-model:current-page="page"
          v-model:page-size="size"
          :page-sizes="[10, 20, 50]"
          :total="total"
          layout="total, prev, pager, next, sizes"
          @current-change="load"
          @size-change="search"
        />
      </div>
    </section>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { getStatistics, pageSubmissions, type SubmissionDetail } from '@/api/submission'
import {
  STATUS_MAP,
  statusColor,
  statusLabel,
  formatTimeUsed,
  formatMemory,
  LANGUAGE_OPTIONS,
} from '@/utils/status'
import { formatDateTime, relativeTime, toQueryDateTime } from '@/utils/datetime'

const records = ref<SubmissionDetail[]>([])
const problemId = ref('')
const status = ref<number | undefined>()
const timeRange = ref<[string, string] | null>(null)
const page = ref(1)
const size = ref(10)
const total = ref(0)
const loading = ref(false)

const summary = ref({ totalSubmit: 0, totalAccepted: 0, acceptRate: 0, solvedCount: 0 })

const hasFilter = computed(() => !!problemId.value || status.value !== undefined || !!timeRange.value)

/** 时间快捷范围：与日期选择器联动，选中自定义区间后自动取消高亮 */
const QUICK_RANGES = [
  { key: 'today', label: '今天' },
  { key: '7d', label: '近 7 天' },
  { key: '30d', label: '近 30 天' },
] as const
type PresetKey = (typeof QUICK_RANGES)[number]['key']
const activePreset = ref<PresetKey | ''>('')

const pad = (n: number) => String(n).padStart(2, '0')
function fmtPicker(d: Date) {
  return `${d.getFullYear()}-${pad(d.getMonth() + 1)}-${pad(d.getDate())}T${pad(d.getHours())}:${pad(d.getMinutes())}:${pad(d.getSeconds())}`
}

function applyPreset(key: PresetKey) {
  const now = new Date()
  const start = new Date(now)
  start.setHours(0, 0, 0, 0)
  if (key === '7d') start.setDate(now.getDate() - 6)
  if (key === '30d') start.setDate(now.getDate() - 29)
  timeRange.value = [fmtPicker(start), fmtPicker(now)]
  activePreset.value = key
  search()
}

function languageLabel(lang?: string) {
  return LANGUAGE_OPTIONS.find((l) => l.value === lang)?.label || lang || '-'
}

async function load() {
  loading.value = true
  try {
    // 输入框允许任意字符，Number('abc') 是 NaN，直接传给后端会得到 40000。
    // 只有解析出正整数才作为筛选条件，否则视为"未筛选"。
    const pid = Number(problemId.value)
    const data = await pageSubmissions({
      problemId: Number.isInteger(pid) && pid > 0 ? pid : undefined,
      status: status.value,
      // Query 参数走 @DateTimeFormat(ISO.DATE_TIME)，必须是 T 分隔
      begin: toQueryDateTime(timeRange.value?.[0]),
      end: toQueryDateTime(timeRange.value?.[1]),
      page: page.value,
      size: size.value,
    })
    records.value = data.records || []
    total.value = data.total
  } finally {
    loading.value = false
  }
}

/** 筛选条件变化后回到第一页再查 */
function search() {
  page.value = 1
  load()
}

/** 时间范围改动即查（与状态选择器同口径），并清掉快捷区间高亮，避免"改完以为生效了" */
function onRangeChange() {
  activePreset.value = ''
  search()
}

function reset() {
  problemId.value = ''
  status.value = undefined
  timeRange.value = null
  activePreset.value = ''
  search()
}

async function loadSummary() {
  try {
    const s = await getStatistics()
    summary.value = {
      totalSubmit: s.totalSubmit,
      totalAccepted: s.totalAccepted,
      acceptRate: s.acceptRate,
      solvedCount: s.solvedCount,
    }
  } catch {
    // 概览失败不影响列表主功能
  }
}

onMounted(() => {
  load()
  loadSummary()
})
</script>

<style scoped>
.submission-page {
  max-width: 1280px;
  margin: 0 auto;
  padding: 44px 32px 64px;
}

.log-head {
  align-items: flex-end;
}
.log-totals {
  display: flex;
  margin: 0;
  padding: 0;
  flex-shrink: 0;
}
.total-item {
  padding: 0 20px;
  border-left: 1px solid var(--oj-border);
  text-align: right;
}
.total-item:last-child {
  padding-right: 0;
}
.total-item dt {
  font-family: var(--oj-font-mono);
  font-size: 8.5px;
  letter-spacing: 0.1em;
  color: var(--oj-text-weak);
}
.total-item dd {
  margin: 5px 0 0;
  font-family: var(--oj-font-mono);
  font-size: 25px;
  line-height: 1;
  font-weight: 600;
  letter-spacing: -0.04em;
}
.total-item dd small {
  margin-left: 1px;
  font-size: 11px;
  color: var(--oj-text-sub);
}

/* 筛选工具条 */
.query-bar {
  display: flex;
  align-items: flex-end;
  flex-wrap: wrap;
  gap: 12px;
  padding: 16px 18px;
  margin-bottom: 18px;
  background: var(--oj-card);
  border: 1px solid var(--oj-border);
  border-radius: 10px;
  box-shadow: var(--oj-shadow-sm);
}
.query-field {
  display: flex;
  flex-direction: column;
  gap: 5px;
}
.query-label {
  font-family: var(--oj-font-mono);
  font-size: 8.5px;
  letter-spacing: 0.08em;
  color: var(--oj-text-weak);
}
.query-pid {
  width: 118px;
}
.query-status {
  width: 138px;
}
.query-range {
  width: 330px;
}
.query-range :deep(.el-date-editor) {
  width: 100%;
}
.query-presets {
  display: flex;
  gap: 4px;
  padding-bottom: 1px;
}
.quick-btn {
  padding: 6px 10px;
  border: 1px solid var(--oj-border);
  border-radius: 5px;
  background: var(--oj-gray-50);
  color: var(--oj-text-sub);
  font-family: inherit;
  font-size: 10.5px;
  cursor: pointer;
  transition: color var(--oj-duration) var(--oj-ease), border-color var(--oj-duration) var(--oj-ease),
    background var(--oj-duration) var(--oj-ease);
}
.quick-btn:hover {
  border-color: var(--oj-border-strong);
  color: var(--oj-text);
}
.quick-btn.active {
  background: var(--oj-primary-soft);
  border-color: var(--oj-info-border);
  color: var(--oj-primary);
  font-weight: 600;
}
.query-actions {
  display: flex;
  align-items: center;
  gap: 10px;
  margin-left: auto;
  padding-bottom: 1px;
}
.reset-link {
  border: 0;
  background: none;
  padding: 0;
  font-family: inherit;
  font-size: 10.5px;
  color: var(--oj-text-sub);
  cursor: pointer;
}
.reset-link:hover {
  color: var(--oj-primary);
}

/* 表卡 */
.table-card {
  background: var(--oj-card);
  border: 1px solid var(--oj-border);
  border-radius: 10px;
  box-shadow: var(--oj-shadow-sm);
  overflow: hidden;
}
.list-head {
  display: flex;
  align-items: flex-end;
  justify-content: space-between;
  gap: 16px;
  padding: 16px 18px 13px;
  border-bottom: 1px solid var(--oj-border);
}
.list-title {
  margin: 4px 0 0;
  font-size: 19px;
  font-weight: 750;
  letter-spacing: -0.03em;
}
.list-meta {
  font-family: var(--oj-font-mono);
  font-size: 8.5px;
  letter-spacing: 0.05em;
  color: var(--oj-text-weak);
}

.log-table {
  width: 100%;
}
.log-table :deep(.el-table__inner-wrapper::before) {
  display: none;
}
.id-cell {
  font-size: 10px;
  color: var(--oj-text-weak);
}
.problem-cell {
  display: flex;
  align-items: center;
  gap: 8px;
}
.title-link {
  color: var(--oj-gray-800);
  font-weight: 600;
  transition: color var(--oj-duration) var(--oj-ease);
}
.title-link:hover {
  color: var(--oj-primary);
}
.lang-text {
  font-size: 10.5px;
  color: var(--oj-text-sub);
}
.status-cell {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  font-size: 11px;
  font-weight: 600;
}
.status-dot {
  width: 5px;
  height: 5px;
  border-radius: 50%;
  flex-shrink: 0;
}
.num {
  font-size: 10.5px;
  color: var(--oj-gray-700);
}
.num.dim,
.dim {
  color: var(--oj-text-weak);
}
.submit-time {
  font-size: 11px;
  color: var(--oj-text-sub);
}
.row-go {
  display: inline-flex;
  color: var(--oj-text-weak);
  transition: color var(--oj-duration) var(--oj-ease), transform var(--oj-duration) var(--oj-ease);
}
.row-go:hover {
  color: var(--oj-primary);
  transform: translateX(2px);
}
.table-foot {
  display: flex;
  justify-content: flex-end;
  padding: 14px 18px;
  border-top: 1px solid var(--oj-border);
}

@media (max-width: 900px) {
  .submission-page {
    padding: 28px 18px 52px;
  }
  .log-totals {
    width: 100%;
    margin-top: 16px;
    padding-top: 14px;
    border-top: 1px solid var(--oj-border);
  }
  .total-item:first-child {
    padding-left: 0;
    border-left: 0;
    text-align: left;
  }
  .query-pid,
  .query-status,
  .query-range {
    width: 100%;
  }
  .query-actions {
    margin-left: 0;
  }
  .query-presets {
    width: 100%;
  }
  .list-meta {
    display: none;
  }
}
</style>
