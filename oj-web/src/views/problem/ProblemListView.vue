<template>
  <div class="page-container">
    <div class="stat-banner card">
      <div class="stat-item">
        <span class="stat-num">{{ total }}</span>
        <span class="stat-label">题目总数</span>
      </div>
      <div class="stat-divider"></div>
      <div class="stat-item">
        <span class="stat-num">{{ lbTotal }}</span>
        <span class="stat-label">榜上解题者</span>
      </div>
      <div class="stat-divider"></div>
      <div class="stat-item">
        <span class="stat-num">{{ totalSubmits }}</span>
        <span class="stat-label">累计提交</span>
      </div>
      <div class="stat-slogan">写下你的第一行代码，看看能过几个测试用例</div>
    </div>

    <div v-if="announcements.length" class="card notice-bar">
      <span class="notice-icon">📢</span>
      <el-carousel height="28px" direction="vertical" :autoplay="true" interval="4500" indicator-position="none" arrow="never">
        <el-carousel-item v-for="a in announcements" :key="a.id">
          <span class="notice-title">【{{ a.title }}】</span>
          <span class="notice-content">{{ a.content }}</span>
        </el-carousel-item>
      </el-carousel>
    </div>

    <div class="card">
      <div class="toolbar">
        <el-input v-model="keyword" placeholder="按关键词搜索题目" style="width: 240px" clearable @keyup.enter="load" />
        <el-select v-model="difficulty" placeholder="难度" style="width: 120px" clearable @change="load">
          <el-option label="简单" :value="1" />
          <el-option label="中等" :value="2" />
          <el-option label="困难" :value="3" />
        </el-select>
        <el-select v-model="tag" placeholder="标签" style="width: 140px" clearable filterable allow-create @change="load">
          <el-option v-for="t in tagOptions" :key="t" :label="t" :value="t" />
        </el-select>
        <el-button type="primary" @click="load">搜索</el-button>
      </div>

      <el-table :data="problems" v-loading="loading">
        <el-table-column prop="id" label="#" width="70">
          <template #default="{ row }">
            <span class="pid-badge">{{ row.id }}</span>
          </template>
        </el-table-column>
        <el-table-column label="题目" min-width="300">
          <template #default="{ row }">
            <router-link :to="`/problems/${row.id}`" class="title-link">{{ row.title }}</router-link>
          </template>
        </el-table-column>
        <el-table-column label="标签" min-width="140">
          <template #default="{ row }">
            <el-tag v-for="t in row.tags || []" :key="t" size="small" type="info" style="margin-right: 4px">{{ t }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="难度" width="100">
          <template #default="{ row }">
            <el-tag :color="difficultyInfo(row.difficulty).color" effect="light" size="small">
              {{ difficultyInfo(row.difficulty).label }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="通过率" width="220">
          <template #default="{ row }">
            <div class="rate-cell">
              <el-progress
                :percentage="passRate(row)"
                :stroke-width="9"
                :color="rateColor(passRate(row))"
              />
              <span class="rate-text">{{ passRate(row) }}% ({{ row.acceptedCount }}/{{ row.submitCount }})</span>
            </div>
          </template>
        </el-table-column>
      </el-table>

      <el-pagination
        v-model:current-page="page"
        :page-size="size"
        :total="total"
        layout="prev, pager, next"
        style="margin-top: 16px; justify-content: flex-end"
        @current-change="load"
      />
    </div>
  </div>
</template>

<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { pageProblems, type Problem } from '@/api/problem'
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

function difficultyInfo(d: number) {
  return DIFFICULTY_MAP[d] || { label: '未知', color: '#909399' }
}

function passRate(row: Problem) {
  if (!row.submitCount) return 0
  return Math.round((row.acceptedCount / row.submitCount) * 100)
}

/** 通过率分级配色（OJ 惯例）：高通过率偏绿、低通过率偏红 */
function rateColor(rate: number) {
  if (rate >= 60) return '#67c23a'
  if (rate >= 30) return '#e6a23c'
  return '#f56c6c'
}

const lbTotal = ref(0)
const announcements = ref<AnnouncementItem[]>([])
const totalSubmits = ref(0)

async function load() {
  loading.value = true
  try {
    const data = await pageProblems({
      keyword: keyword.value || undefined,
      difficulty: difficulty.value,
      tag: tag.value || undefined,
      page: page.value,
      size: size.value,
    })
    problems.value = data.records || []
    total.value = data.total
    // 标签选项：从现有题目聚合（去重）
    const tags = new Set<string>()
    for (const p of problems.value) for (const t of p.tags || []) tags.add(t)
    tagOptions.value = [...tags].sort()
    // 横幅统计：榜上人数 + 本页题目累计提交数（现成接口聚合，呈现层）
    totalSubmits.value = (data.records || []).reduce((sum, p) => sum + (p.submitCount || 0), 0)
  } finally {
    loading.value = false
  }
}

onMounted(async () => {
  load()
  try {
    const lb = await getLeaderboard({ page: 1, size: 1 })
    lbTotal.value = lb.total
  } catch { /* 横幅统计失败不影响列表 */ }
  try {
    announcements.value = (await listAnnouncements()).slice(0, 5)
  } catch { /* 公告栏失败不影响列表 */ }
})
</script>

<style scoped>
.stat-banner {
  display: flex;
  align-items: center;
  gap: 28px;
  padding: 20px 28px;
  margin-bottom: 16px;
  background: linear-gradient(135deg, #3730a3 0%, #4f46e5 60%, #6366f1 100%);
  color: #fff;
}
.stat-item {
  display: flex;
  flex-direction: column;
  align-items: center;
}
.stat-num {
  font-size: 26px;
  font-weight: 800;
}
.stat-label {
  font-size: 12px;
  opacity: 0.85;
  margin-top: 2px;
}
.stat-divider {
  width: 1px;
  height: 36px;
  background: rgba(255, 255, 255, 0.3);
}
.stat-slogan {
  margin-left: auto;
  font-size: 13px;
  opacity: 0.9;
  letter-spacing: 1px;
}
.toolbar {
  display: flex;
  gap: 12px;
  margin-bottom: 16px;
}
.title-link {
  color: #303133;
  font-weight: 500;
}
.title-link:hover {
  color: #4f46e5;
}
.rate-cell {
  display: flex;
  align-items: center;
  gap: 8px;
}
.rate-cell :deep(.el-progress) {
  flex: 1;
}
.rate-cell :deep(.el-progress__text) {
  display: none;
}
.rate-text {
  font-size: 12px;
  color: #606266;
  white-space: nowrap;
}
.notice-bar {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 10px 24px;
  margin-bottom: 16px;
}
.notice-icon {
  font-size: 18px;
}
.notice-bar :deep(.el-carousel) {
  flex: 1;
}
.notice-bar :deep(.el-carousel__container) {
  height: 28px;
}
.notice-title {
  color: #e6a23c;
  font-weight: 700;
  font-size: 13px;
  margin-right: 8px;
}
.notice-content {
  color: #606266;
  font-size: 13px;
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
}
</style>
