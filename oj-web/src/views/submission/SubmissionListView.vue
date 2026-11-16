<template>
  <div class="page-container">
    <!-- 顶部概览：个人提交全景（来自统计接口，不随筛选变化） -->
    <div class="summary-bar card">
      <div class="sum-item">
        <span class="sum-num">{{ summary.totalSubmit }}</span>
        <span class="sum-label">累计提交</span>
      </div>
      <div class="sum-divider" />
      <div class="sum-item">
        <span class="sum-num">{{ summary.totalAccepted }}</span>
        <span class="sum-label">判题通过</span>
      </div>
      <div class="sum-divider" />
      <div class="sum-item">
        <span class="sum-num">{{ summary.acceptRate }}%</span>
        <span class="sum-label">整体通过率</span>
      </div>
      <div class="sum-divider" />
      <div class="sum-item">
        <span class="sum-num">{{ summary.solvedCount }}</span>
        <span class="sum-label">已解题目</span>
      </div>
    </div>

    <div class="card">
      <div class="head">
        <div>
          <h2>提交记录</h2>
          <p class="sub">支持按题目、判题状态与时间范围筛选</p>
        </div>
      </div>

      <div class="filter-bar">
        <el-input
          v-model="problemId"
          placeholder="题目 ID"
          style="width: 120px"
          clearable
          @keyup.enter="search"
        />
        <el-select v-model="status" placeholder="判题状态" style="width: 140px" clearable @change="search">
          <el-option v-for="(v, k) in STATUS_MAP" :key="k" :label="v.label" :value="Number(k)" />
        </el-select>
        <div class="quick-range">
          <button
            v-for="p in QUICK_RANGES"
            :key="p.key"
            class="quick-btn"
            :class="{ active: activePreset === p.key }"
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
          style="width: 360px"
          @change="activePreset = ''"
        />
        <el-button type="primary" @click="search">筛选</el-button>
        <el-button @click="reset">重置</el-button>
      </div>

      <el-table :data="records" v-loading="loading" class="sub-table">
        <el-table-column prop="id" label="编号" width="80">
          <template #default="{ row }">
            <span class="mono dim">#{{ row.id }}</span>
          </template>
        </el-table-column>
        <el-table-column label="题目" min-width="230">
          <template #default="{ row }">
            <div class="problem-cell">
              <span class="pid-badge">{{ row.problemId }}</span>
              <router-link :to="`/problems/${row.problemId}`" class="title-link">
                {{ row.problemTitle || `题目 ${row.problemId}` }}
              </router-link>
            </div>
          </template>
        </el-table-column>
        <el-table-column label="语言" width="110">
          <template #default="{ row }">
            <span class="lang-chip">{{ languageLabel(row.language) }}</span>
          </template>
        </el-table-column>
        <el-table-column label="状态" width="120">
          <template #default="{ row }">
            <span class="status-cell" :style="{ color: statusColor(row.status) }">
              <span class="status-dot" :style="{ background: statusColor(row.status) }" />
              {{ statusLabel(row.status) }}
            </span>
          </template>
        </el-table-column>
        <el-table-column label="耗时" width="100" align="right">
          <template #default="{ row }">
            <span class="mono" :class="{ dim: row.timeUsedMs == null }">{{ formatTimeUsed(row.timeUsedMs) }}</span>
          </template>
        </el-table-column>
        <el-table-column label="内存" width="110" align="right">
          <template #default="{ row }">
            <span class="mono" :class="{ dim: row.memoryUsedKb == null }">{{ formatMemory(row.memoryUsedKb) }}</span>
          </template>
        </el-table-column>
        <el-table-column label="提交时间" width="130" align="right">
          <template #default="{ row }">
            <span class="submit-time" :title="formatDateTime(row.createdAt)">{{ relativeTime(row.createdAt) }}</span>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="80" align="center">
          <template #default="{ row }">
            <el-button link type="primary" @click="$router.push(`/submissions/${row.id}`)">
              详情<svg class="arrow" viewBox="0 0 24 24" width="12" height="12" fill="none">
                <path d="M9 6 L15 12 L9 18" stroke="currentColor" stroke-width="2.4" stroke-linecap="round" stroke-linejoin="round" />
              </svg>
            </el-button>
          </template>
        </el-table-column>
        <template #empty>
          <el-empty description="没有符合条件的提交记录" />
        </template>
      </el-table>

      <el-pagination
        v-model:current-page="page"
        v-model:page-size="size"
        :page-sizes="[10, 20, 50]"
        :total="total"
        layout="total, prev, pager, next"
        class="pager"
        @current-change="load"
        @size-change="search"
      />
    </div>
  </div>
</template>

<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { getStatistics, pageSubmissions, type SubmissionDetail } from '@/api/submission'
import { STATUS_MAP, statusColor, statusLabel, formatTimeUsed, formatMemory, LANGUAGE_OPTIONS } from '@/utils/status'
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
    const data = await pageSubmissions({
      problemId: problemId.value ? Number(problemId.value) : undefined,
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
.summary-bar {
  display: flex;
  align-items: center;
  gap: 32px;
  padding: 20px 28px;
  margin-bottom: 16px;
  background: linear-gradient(135deg, #3730a3 0%, #4f46e5 60%, #6366f1 100%);
  color: #fff;
}
.sum-item {
  display: flex;
  flex-direction: column;
  align-items: center;
}
.sum-num {
  font-size: 26px;
  font-weight: 800;
  line-height: 1.1;
  font-variant-numeric: tabular-nums;
}
.sum-label {
  font-size: 12px;
  opacity: 0.85;
  margin-top: 4px;
}
.sum-divider {
  width: 1px;
  height: 36px;
  background: rgba(255, 255, 255, 0.28);
}
.head h2 {
  margin: 0;
}
.sub {
  margin: 4px 0 0;
  font-size: 12.5px;
  color: #909399;
}
.filter-bar {
  display: flex;
  align-items: center;
  flex-wrap: wrap;
  gap: 10px;
  padding: 12px;
  margin-bottom: 16px;
  background: #f8f9fc;
  border: 1px solid var(--oj-border);
  border-radius: 10px;
}
.quick-range {
  display: flex;
  gap: 6px;
}
.quick-btn {
  border: 1px solid var(--oj-border);
  background: #fff;
  color: #606266;
  font-size: 12.5px;
  padding: 5px 12px;
  border-radius: 999px;
  cursor: pointer;
  transition: all 0.15s var(--oj-ease);
}
.quick-btn:hover {
  color: var(--oj-primary);
  border-color: var(--oj-primary-light-5);
}
.quick-btn.active {
  background: var(--oj-primary);
  border-color: var(--oj-primary);
  color: #fff;
}
.problem-cell {
  display: flex;
  align-items: center;
  gap: 8px;
}
.pid-badge {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  min-width: 30px;
  height: 24px;
  padding: 0 6px;
  border-radius: 6px;
  background: #eef2ff;
  color: #4f46e5;
  font-weight: 700;
  font-size: 13px;
  flex-shrink: 0;
  font-variant-numeric: tabular-nums;
}
.title-link {
  color: #303133;
  font-weight: 500;
}
.title-link:hover {
  color: #4f46e5;
}
.lang-chip {
  display: inline-flex;
  align-items: center;
  padding: 2px 10px;
  border-radius: 999px;
  background: #f4f4f5;
  border: 1px solid #e4e7ed;
  color: #606266;
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
  font-family: 'JetBrains Mono', 'Cascadia Code', Consolas, Menlo, monospace;
  font-size: 13px;
  font-variant-numeric: tabular-nums;
}
.mono.dim,
.dim {
  color: #a8abb2;
}
.submit-time {
  color: #909399;
  font-size: 13px;
}
.arrow {
  margin-left: 2px;
}
.pager {
  margin-top: 16px;
  justify-content: flex-end;
}
</style>
