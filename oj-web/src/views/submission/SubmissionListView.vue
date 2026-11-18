<template>
  <div class="page-container">
    <!-- 顶部概览：个人提交全景（来自 /api/submissions/statistics，全表聚合，不随筛选变化） -->
    <section class="oj-banner card" aria-label="个人提交概览">
      <div class="oj-banner-item">
        <span class="oj-banner-num">{{ summary.totalSubmit }}</span>
        <span class="oj-banner-label">累计提交</span>
      </div>
      <div class="oj-banner-divider" aria-hidden="true"></div>
      <div class="oj-banner-item">
        <span class="oj-banner-num">{{ summary.totalAccepted }}</span>
        <span class="oj-banner-label">判题通过</span>
      </div>
      <div class="oj-banner-divider" aria-hidden="true"></div>
      <div class="oj-banner-item">
        <span class="oj-banner-num">{{ summary.acceptRate }}<small>%</small></span>
        <span class="oj-banner-label">通过率</span>
      </div>
      <div class="oj-banner-divider" aria-hidden="true"></div>
      <div class="oj-banner-item">
        <span class="oj-banner-num">{{ summary.solvedCount }}</span>
        <span class="oj-banner-label">已解题目</span>
      </div>
      <p class="oj-banner-note">保持节奏<br />每一道 AC 都算数</p>
    </section>

    <div class="card">
      <header class="head">
        <div>
          <h2>提交记录</h2>
          <p class="sub">
            按题目、判题状态与时间范围筛选
            <template v-if="hasFilter">（<button type="button" class="link-btn" @click="reset">清除筛选</button>）</template>
          </p>
        </div>
      </header>

      <div class="filter-bar">
        <el-input
          v-model="problemId"
          placeholder="题目 ID"
          class="pid-input"
          clearable
          @keyup.enter="search"
          @clear="search"
        />
        <el-select v-model="status" placeholder="判题状态" class="status-select" clearable @change="search">
          <el-option v-for="(v, k) in STATUS_MAP" :key="k" :label="v.label" :value="Number(k)" />
        </el-select>
        <div class="quick-range" role="group" aria-label="快捷时间范围">
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
        <el-date-picker
          v-model="timeRange"
          type="datetimerange"
          start-placeholder="开始时间"
          end-placeholder="结束时间"
          value-format="YYYY-MM-DDTHH:mm:ss"
          class="range-picker"
          @change="activePreset = ''"
        />
        <el-button type="primary" @click="search">筛选</el-button>
      </div>

      <el-table :data="records" v-loading="loading" row-key="id" class="sub-table" empty-text="没有符合条件的提交记录">
        <el-table-column prop="id" label="编号" width="84">
          <template #default="{ row }">
            <span class="mono dim">#{{ row.id }}</span>
          </template>
        </el-table-column>

        <el-table-column label="题目" min-width="230">
          <template #default="{ row }">
            <div class="problem-cell">
              <span class="oj-badge-id">{{ row.problemId }}</span>
              <router-link :to="`/problems/${row.problemId}`" class="title-link">
                {{ row.problemTitle || `题目 ${row.problemId}` }}
              </router-link>
            </div>
          </template>
        </el-table-column>

        <el-table-column label="语言" width="112">
          <template #default="{ row }">
            <span class="lang-chip">{{ languageLabel(row.language) }}</span>
          </template>
        </el-table-column>

        <el-table-column label="状态" width="118">
          <template #default="{ row }">
            <span class="status-cell" :style="{ color: statusColor(row.status) }">
              <span class="status-dot" :style="{ background: statusColor(row.status) }" aria-hidden="true" />
              {{ statusLabel(row.status) }}
            </span>
          </template>
        </el-table-column>

        <el-table-column label="耗时" width="100" align="right">
          <template #default="{ row }">
            <span class="mono" :class="{ dim: row.timeUsedMs == null }">{{ formatTimeUsed(row.timeUsedMs) }}</span>
          </template>
        </el-table-column>

        <el-table-column label="内存" width="108" align="right">
          <template #default="{ row }">
            <span class="mono" :class="{ dim: row.memoryUsedKb == null }">{{ formatMemory(row.memoryUsedKb) }}</span>
          </template>
        </el-table-column>

        <el-table-column label="提交时间" width="132" align="right">
          <template #default="{ row }">
            <span class="submit-time" :title="formatDateTime(row.createdAt)">{{ relativeTime(row.createdAt) }}</span>
          </template>
        </el-table-column>

        <el-table-column label="操作" width="86" align="center">
          <template #default="{ row }">
            <el-button link type="primary" @click="$router.push(`/submissions/${row.id}`)">
              详情<svg class="arrow" viewBox="0 0 24 24" width="12" height="12" fill="none" aria-hidden="true">
                <path d="M9 6 L15 12 L9 18" stroke="currentColor" stroke-width="2.4" stroke-linecap="round" stroke-linejoin="round" />
              </svg>
            </el-button>
          </template>
        </el-table-column>
      </el-table>

      <el-pagination
        v-if="total > size"
        v-model:current-page="page"
        v-model:page-size="size"
        :page-sizes="[10, 20, 50]"
        :total="total"
        layout="total, prev, pager, next, sizes"
        class="pager"
        @current-change="load"
        @size-change="search"
      />
    </div>
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
    // P2-6：输入框允许任意字符，Number('abc') 是 NaN，直接传给后端会得到 40000。
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
.oj-banner-num small {
  font-size: 14px;
  margin-left: 1px;
}

.head h2 {
  margin: 0;
}
.head .sub {
  margin: 6px 0 0;
  font-size: 12.5px;
  color: var(--oj-text-sub);
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

.filter-bar {
  display: flex;
  align-items: center;
  flex-wrap: wrap;
  gap: 10px;
  padding: 12px;
  margin: 14px 0 16px;
  background: var(--oj-gray-50);
  border: 1px solid var(--oj-border);
  border-radius: var(--oj-radius-md);
}
.pid-input {
  width: 120px;
}
.status-select {
  width: 140px;
}
.range-picker {
  width: 360px;
}

.quick-range {
  display: flex;
  gap: 6px;
}
.quick-btn {
  border: 1px solid var(--oj-border);
  background: var(--oj-white);
  color: var(--oj-gray-600);
  font-family: inherit;
  font-size: 12.5px;
  padding: 5px 12px;
  border-radius: var(--oj-radius-pill);
  cursor: pointer;
  transition: color var(--oj-duration) var(--oj-ease),
    border-color var(--oj-duration) var(--oj-ease),
    background var(--oj-duration) var(--oj-ease);
}
.quick-btn:hover {
  color: var(--oj-primary);
  border-color: var(--el-color-primary-light-5);
}
.quick-btn.active {
  background: var(--oj-primary);
  border-color: var(--oj-primary);
  color: var(--oj-white);
  font-weight: 600;
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
.lang-chip {
  display: inline-flex;
  align-items: center;
  padding: 2px 10px;
  border-radius: var(--oj-radius-pill);
  background: var(--oj-gray-50);
  border: 1px solid var(--oj-border);
  color: var(--oj-gray-600);
  font-size: 12px;
}
.status-cell {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  font-weight: 600;
  font-size: 13px;
}
.status-dot {
  width: 7px;
  height: 7px;
  border-radius: 50%;
  flex-shrink: 0;
}
.mono {
  font-family: var(--oj-font-mono);
  font-size: 13px;
  font-variant-numeric: tabular-nums;
}
.mono.dim,
.dim {
  color: var(--oj-text-weak);
}
.submit-time {
  color: var(--oj-text-sub);
  font-size: 13px;
}
.arrow {
  margin-left: 2px;
}
.pager {
  margin-top: 16px;
  justify-content: flex-end;
}

@media (max-width: 900px) {
  .pid-input,
  .status-select,
  .range-picker {
    width: 100%;
  }
}
</style>
