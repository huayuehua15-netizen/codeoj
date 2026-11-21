<template>
  <div class="dossier">
    <!-- 个人概览：身份、签名、摘要数据 -->
    <header class="dossier-head">
      <div class="dossier-label-row">
        <span class="kicker">PROFILE</span>
        <span class="folio">ID #{{ String(info?.userId ?? userStore.userId ?? 0).padStart(4, '0') }}</span>
      </div>

      <div class="dossier-id">
        <button
          class="avatar-box"
          type="button"
          :disabled="uploadingAvatar"
          aria-label="更换头像"
          @click="avatarInput?.click()"
        >
          <img v-if="avatarUrl" class="avatar avatar-img" :src="avatarUrl" alt="我的头像" />
          <span v-else class="avatar">{{ initial }}</span>
          <span class="avatar-mask" aria-hidden="true">
            <svg viewBox="0 0 24 24" width="15" height="15" fill="none" stroke="currentColor" stroke-width="1.8" stroke-linecap="round" stroke-linejoin="round">
              <circle cx="12" cy="12" r="3" /><path d="M12 3c2.4 0 4.6.9 6.3 2.4l1.7-1.4v5h-5l1.9-1.6A7 7 0 1 0 19 12h2a9 9 0 1 1-9-9z" />
            </svg>
            {{ uploadingAvatar ? '上传中…' : '更换' }}
          </span>
        </button>
        <input
          ref="avatarInput"
          type="file"
          accept="image/png,image/jpeg,image/webp"
          class="sr-only"
          tabindex="-1"
          @change="onAvatarFile"
        />

        <div class="id-main">
          <div class="name-row">
            <h1 class="name">{{ userStore.username }}</h1>
            <span class="role-chip" :class="{ admin: userStore.isAdmin }">
              {{ userStore.isAdmin ? 'ADMIN' : 'USER' }}
            </span>
          </div>

          <div v-if="!sigEditing" class="sig-row">
            <p class="sig-text" :class="{ empty: !signature }">
              {{ signature || '添加一句简介' }}
            </p>
            <el-button text size="small" class="sig-edit-btn" @click="openSigEdit">编辑签名</el-button>
          </div>
          <div v-else class="sig-editor">
            <el-input
              v-model="sigDraft"
              type="textarea"
              :rows="2"
              maxlength="200"
              show-word-limit
              placeholder="一句话介绍自己（1-200 字）"
            />
            <div class="sig-actions">
              <el-button size="small" @click="sigEditing = false">取消</el-button>
              <el-button type="primary" size="small" :loading="savingSig" @click="saveSig">保存签名</el-button>
            </div>
          </div>

          <div class="meta mono">
            <span>UID {{ info?.userId ?? userStore.userId }}</span>
            <span class="dot" aria-hidden="true">·</span>
            <span>{{ formatDateTime(info?.createdAt) || '—' }} 注册</span>
            <span class="dot" aria-hidden="true">·</span>
            <span>{{ info?.email || '邮箱未设置' }}</span>
          </div>
        </div>
      </div>

      <dl class="dossier-summary" aria-label="个人数据摘要">
        <div class="summary-cell">
          <dt>解题数</dt>
          <dd>{{ statistics?.solvedCount ?? '—' }}</dd>
        </div>
        <div class="summary-cell">
          <dt>提交数</dt>
          <dd>{{ statistics?.totalSubmit ?? '—' }}</dd>
        </div>
        <div class="summary-cell">
          <dt>通过率</dt>
          <dd>{{ statistics ? `${statistics.acceptRate}` : '—' }}<small v-if="statistics">%</small></dd>
        </div>
        <div class="summary-cell">
          <dt>连续刷题</dt>
          <dd>{{ grid?.streak ?? '—' }}<small>天</small></dd>
        </div>
      </dl>
    </header>

    <div class="dossier-body">
      <main class="main-col">
        <!-- 热力图 -->
        <section class="panel">
          <div class="panel-head">
            <span class="panel-kicker">ACTIVITY</span>
            <h2 class="panel-title">刷题热力图</h2>
            <p class="panel-meta">
              近 26 周共 <b>{{ grid?.totalInRange ?? 0 }}</b> 次提交 · 活跃 <b>{{ grid?.activeDays ?? 0 }}</b> 天
            </p>
            <div class="legend" aria-hidden="true">
              <span>少</span>
              <span class="lv lv-0" /><span class="lv lv-1" /><span class="lv lv-2" /><span class="lv lv-3" /><span class="lv lv-4" />
              <span>多</span>
            </div>
          </div>

          <div class="heatmap-scroll" v-loading="heatmapLoading">
            <div v-if="grid" class="heatmap" role="img" aria-label="近 26 周提交热力图">
              <div class="month-row" :style="gridTemplateStyle">
                <span
                  v-for="(m, i) in grid.monthLabels"
                  :key="i"
                  class="month"
                  :style="{ gridColumnStart: m.column + 1 }"
                >
                  {{ m.label }}
                </span>
              </div>
              <div class="heat-body">
                <div class="weekday-col" aria-hidden="true">
                  <span>日</span><span class="wk-blank" /><span>二</span><span class="wk-blank" /><span>四</span><span class="wk-blank" /><span>六</span>
                </div>
                <div class="heat-grid" :style="gridTemplateStyle">
                  <div v-for="(col, ci) in grid.columns" :key="ci" class="heat-col">
                    <span
                      v-for="cell in col"
                      :key="cell.date"
                      class="cell"
                      :class="`lv-${cell.level}`"
                      :title="`${cell.date}：${cell.count} 次提交`"
                    />
                  </div>
                </div>
              </div>
            </div>
            <el-empty v-else-if="!heatmapLoading" description="暂无活动数据" :image-size="64" />
          </div>
        </section>

        <!-- 最近动态 -->
        <section class="panel">
          <div class="panel-head">
            <span class="panel-kicker">RECENT</span>
            <h2 class="panel-title">最近动态</h2>
            <router-link class="panel-link" to="/submissions">全部记录 →</router-link>
          </div>
          <ul v-if="recent.length" class="activity-list">
            <li v-for="item in recent" :key="item.id" class="activity-row">
              <span class="status-dot" :style="{ background: statusColor(item.status) }" :title="statusLabel(item.status)" />
              <div class="activity-main">
                <router-link class="act-problem" :to="`/submissions/${item.id}`">
                  {{ item.problemTitle || `#${item.problemId}` }}
                </router-link>
                <span class="act-meta mono">
                  {{ langLabel(item.language) }}<template v-if="item.timeUsedMs != null"> · {{ formatTimeUsed(item.timeUsedMs) }}</template>
                </span>
              </div>
              <span class="act-status" :style="{ color: statusColor(item.status) }">{{ statusLabel(item.status) }}</span>
              <span class="act-time" :title="formatDateTime(item.createdAt)">{{ relativeTime(item.createdAt) }}</span>
            </li>
          </ul>
          <el-empty v-else description="还没有提交，去题库写第一题吧" :image-size="64" />
        </section>

        <!-- 收藏 -->
        <section class="panel">
          <div class="panel-head">
            <span class="panel-kicker">BOOKMARKS</span>
            <h2 class="panel-title">我的收藏</h2>
            <p class="panel-meta">标记为稍后练习的题目</p>
          </div>
          <el-table
            :data="favorites"
            v-loading="favLoading"
            size="small"
            empty-text="还没有收藏的题目，去题库逛逛吧"
          >
            <el-table-column label="题目" min-width="200">
              <template #default="{ row }">
                <router-link class="fav-link" :to="`/problems/${row.problemId}`">{{ row.title }}</router-link>
              </template>
            </el-table-column>
            <el-table-column label="难度" width="90">
              <template #default="{ row }">
                <span class="oj-diff" :class="`diff-${row.difficulty}`">
                  {{ ({ 1: '简单', 2: '中等', 3: '困难' } as Record<number, string>)[row.difficulty] || '?' }}
                </span>
              </template>
            </el-table-column>
            <el-table-column label="收藏时间" width="150" align="right">
              <template #default="{ row }">
                <span class="mono fav-time">{{ formatDateTime(row.favoritedAt) }}</span>
              </template>
            </el-table-column>
          </el-table>
        </section>
      </main>

      <aside class="side-col">
        <!-- 解题进度 -->
        <section class="panel panel-compact">
          <div class="panel-head">
            <span class="panel-kicker">PROGRESS</span>
            <h2 class="panel-title">解题进度</h2>
            <router-link class="panel-link" to="/statistics">统计 →</router-link>
          </div>
          <div class="diff-list">
            <div v-for="d in difficultyRows" :key="d.difficulty" class="diff-row">
              <span class="oj-diff" :class="`diff-${d.difficulty}`">{{ d.label }}</span>
              <span class="diff-track" aria-hidden="true">
                <span class="diff-bar" :class="`bar-${d.difficulty}`" :style="{ width: `${d.percent}%` }" />
              </span>
              <span class="diff-count mono tabular">
                {{ d.solved }}<span class="of">/{{ d.total }}</span>
              </span>
            </div>
          </div>
        </section>

        <!-- 语言分布 -->
        <section class="panel panel-compact">
          <div class="panel-head">
            <span class="panel-kicker">LANGUAGES</span>
            <h2 class="panel-title">语言分布</h2>
          </div>
          <div v-if="langDist.length" class="lang-list">
            <div v-for="row in langDist" :key="row.language" class="lang-row">
              <span class="lang-name mono">{{ langLabel(row.language) }}</span>
              <span class="lang-track" aria-hidden="true">
                <span class="lang-bar" :style="{ width: `${langPercent(row.total)}%` }" />
              </span>
              <span class="lang-count mono tabular">{{ row.total }} 次 · AC {{ row.accepted }}</span>
            </div>
          </div>
          <el-empty v-else description="还没有提交记录" :image-size="56" />
        </section>

        <!-- 账号设置入口：邮箱/密码/头像/编辑器偏好统一在设置页管理 -->
        <section class="panel panel-compact panel-box settings-entry">
          <div class="panel-head">
            <div>
              <span class="panel-kicker">SETTINGS</span>
              <h2 class="panel-title">账号设置</h2>
            </div>
          </div>
          <p class="settings-copy">
            邮箱、密码、头像与编辑器偏好集中管理。修改密码后当前登录状态会失效。
          </p>
          <router-link class="settings-link" to="/settings">
            打开账号设置
            <span aria-hidden="true">→</span>
          </router-link>
        </section>
      </aside>
    </div>
  </div>
</template>

<script setup lang="ts">
// 个人主页：布局参照 LeetCode 个人页（头部 → 难度进度 → 热力图 → 最近提交）。
// 数据源全部来自真实接口：auth/info + statistics + trend(182 天) + languages + pageMy + favorites。
import { computed, onMounted, ref } from 'vue'
import { ElMessage } from 'element-plus'
import {
  getUserInfo,
  updateAvatar,
  updateSignature,
  type LoginVO,
} from '@/api/auth'
import { pageSubmissions, getStatistics, getTrend, getLanguageDist, type SubmissionDetail, type Statistics } from '@/api/submission'
import { myFavorites } from '@/api/favorite'
import { useUserStore } from '@/stores/user'
import { formatDateTime, relativeTime } from '@/utils/datetime'
import { buildHeatmapGrid, type HeatmapGrid } from '@/utils/heatmap'
import { cropToSquareDataUrl } from '@/utils/avatar'
import { statusColor, statusLabel, formatTimeUsed, LANGUAGE_OPTIONS } from '@/utils/status'

const userStore = useUserStore()

const info = ref<LoginVO>()
const statistics = ref<Statistics>()
const grid = ref<HeatmapGrid>()
const heatmapLoading = ref(false)
const langDist = ref<{ language: string; total: number; accepted: number }[]>([])
const recent = ref<SubmissionDetail[]>([])
const favorites = ref<{ problemId: number; title: string; difficulty: number; favoritedAt: string }[]>([])
const favLoading = ref(false)
const avatarInput = ref<HTMLInputElement>()
const uploadingAvatar = ref(false)
const sigEditing = ref(false)
const sigDraft = ref('')
const savingSig = ref(false)

const initial = computed(() => (userStore.username || '?').slice(0, 1).toUpperCase())
const avatarUrl = computed(() => userStore.avatar || info.value?.avatar || '')
const signature = computed(() => info.value?.signature || '')

/** 热力图网格：格子 20px 定宽 + 4px 间距，与月份标签行共用同一栅格模板 */
const gridTemplateStyle = computed(() => ({
  display: 'grid',
  gridTemplateColumns: `repeat(${grid.value?.columns.length ?? 0}, var(--heat-cell))`,
  columnGap: 'var(--heat-gap)',
}))

/** 难度解题进度：solvedCount / totalCount（题库口径），三条固定 */
const difficultyRows = computed(() => {
  const byD = statistics.value?.byDifficulty ?? []
  const LABELS: Record<number, string> = { 1: '简单', 2: '中等', 3: '困难' }
  return [1, 2, 3].map((difficulty) => {
    const row = byD.find((x) => x.difficulty === difficulty)
    const solved = row?.solvedCount ?? 0
    const total = row?.totalCount ?? 0
    return {
      difficulty,
      label: LABELS[difficulty],
      solved,
      total,
      percent: total > 0 ? Math.round((solved / total) * 100) : 0,
    }
  })
})

function langLabel(code: string) {
  // 统一走 status.ts 的语言表（python3 → Python3），与提交记录页同一口径
  return LANGUAGE_OPTIONS.find((l) => l.value === code)?.label ?? code
}
function langPercent(total: number) {
  const max = langDist.value[0]?.total || 1
  return Math.max(6, Math.round((total / max) * 100))
}

onMounted(async () => {
  // 资料接口失败（非 40100 的网络抖动/500）不能拖垮整页：
  // 拦截器已弹出原因，这里继续发出数据请求，让各区块按各自失败态呈现
  try {
    info.value = await userStore.refreshUserInfo()
  } catch {
    // 40100 由响应拦截器统一登出跳转；其余错误吞掉，下方数据块照常加载
  }
  heatmapLoading.value = true
  favLoading.value = true
  try {
    const [stat, trend, langs, recentPage, favs] = await Promise.allSettled([
      getStatistics(),
      getTrend(182),
      getLanguageDist(),
      pageSubmissions({ page: 1, size: 8 }),
      myFavorites(),
    ])
    if (stat.status === 'fulfilled') statistics.value = stat.value
    if (trend.status === 'fulfilled') grid.value = buildHeatmapGrid(new Date(), trend.value, 26)
    if (langs.status === 'fulfilled') langDist.value = langs.value
    if (recentPage.status === 'fulfilled') recent.value = recentPage.value.records || []
    if (favs.status === 'fulfilled') favorites.value = favs.value
  } finally {
    heatmapLoading.value = false
    favLoading.value = false
  }
})

function openSigEdit() {
  sigDraft.value = signature.value
  sigEditing.value = true
}

async function saveSig() {
  const text = sigDraft.value.trim()
  if (!text) {
    ElMessage.warning('签名不能为空')
    return
  }
  savingSig.value = true
  try {
    await updateSignature(text)
    info.value = await getUserInfo()
    ElMessage.success('签名已更新')
    sigEditing.value = false
  } finally {
    savingSig.value = false
  }
}

async function onAvatarFile(e: Event) {
  const input = e.target as HTMLInputElement
  const file = input.files?.[0]
  input.value = ''
  if (!file) return
  if (!/^image\/(png|jpeg|webp)$/.test(file.type)) {
    ElMessage.warning('仅支持 PNG / JPG / WebP 图片')
    return
  }
  if (file.size > 5 * 1024 * 1024) {
    ElMessage.warning('原图请控制在 5MB 以内')
    return
  }
  uploadingAvatar.value = true
  try {
    let dataUrl: string
    try {
      dataUrl = await cropToSquareDataUrl(file)
    } catch {
      ElMessage.error('图片处理失败，请换一张图片重试')
      return
    }
    await updateAvatar(dataUrl)
    await userStore.refreshUserInfo()
    info.value = await getUserInfo()
    ElMessage.success('头像已更新')
  } finally {
    uploadingAvatar.value = false
  }
}
</script>

<style scoped>
.dossier {
  max-width: 1200px;
  margin: 0 auto;
  padding: 44px 32px 64px;
}

/* ---------- 个人概览 ---------- */
.dossier-head {
  padding: 20px 22px 0;
  background: var(--oj-card);
  border: 1px solid var(--oj-border);
  border-radius: 10px;
  box-shadow: var(--oj-shadow-sm);
}
.dossier-label-row {
  display: flex;
  align-items: baseline;
  justify-content: space-between;
}
.dossier-id {
  display: flex;
  align-items: flex-start;
  gap: 18px;
  margin-top: 16px;
}
.avatar-box {
  position: relative;
  padding: 0;
  border: 0;
  border-radius: 12px;
  background: var(--oj-gray-100);
  cursor: pointer;
  line-height: 0;
  flex-shrink: 0;
  overflow: hidden;
}
.avatar-box:disabled {
  cursor: wait;
}
.avatar {
  width: 80px;
  height: 80px;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  background: var(--oj-gray-900);
  color: var(--oj-white);
  font-family: var(--oj-font-sans);
  font-size: 30px;
  font-weight: 700;
  line-height: 1;
}
.avatar-img {
  object-fit: cover;
}
.avatar-mask {
  position: absolute;
  inset: 0;
  display: none;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 4px;
  background: rgba(16, 24, 40, 0.72);
  color: var(--oj-white);
  font-size: 10px;
  line-height: 1;
  font-family: var(--oj-font-sans);
}
.avatar-box:hover .avatar-mask,
.avatar-box:focus-visible .avatar-mask {
  display: flex;
}

.id-main {
  min-width: 0;
  flex: 1;
  padding-top: 1px;
}
.name-row {
  display: flex;
  align-items: center;
  gap: 10px;
  flex-wrap: wrap;
}
.name {
  margin: 0;
  font-size: 30px;
  font-weight: 750;
  line-height: 1.2;
  letter-spacing: -0.04em;
  color: var(--oj-text);
}
.role-chip {
  padding: 2px 6px;
  border-radius: 4px;
  background: var(--oj-gray-100);
  font-family: var(--oj-font-mono);
  font-size: 8px;
  letter-spacing: 0.06em;
  color: var(--oj-text-sub);
}
.role-chip.admin {
  color: var(--oj-primary);
  background: var(--oj-primary-soft);
}

.sig-row {
  display: flex;
  align-items: baseline;
  gap: 8px;
  margin-top: 8px;
  max-width: 640px;
}
.sig-text {
  margin: 0;
  flex: 1;
  min-width: 0;
  font-size: 12px;
  line-height: 1.7;
  color: var(--oj-text-sub);
}
.sig-text.empty {
  color: var(--oj-text-weak);
}
.sig-editor {
  margin-top: 10px;
  max-width: 640px;
}
.sig-actions {
  display: flex;
  justify-content: flex-end;
  gap: 8px;
  margin-top: 8px;
}
.meta {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  gap: 7px;
  margin: 8px 0 0;
  font-size: 9.5px;
  letter-spacing: 0.02em;
  color: var(--oj-text-weak);
}
.meta .dot {
  color: var(--oj-gray-300);
}

.dossier-summary {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  margin: 18px -22px 0;
  border-top: 1px solid var(--oj-border);
}
.summary-cell {
  padding: 14px 22px 13px;
  border-right: 1px solid var(--oj-border);
}
.summary-cell:last-child {
  border-right: 0;
}
.summary-cell dt {
  font-family: var(--oj-font-mono);
  font-size: 8px;
  letter-spacing: 0.08em;
  color: var(--oj-text-weak);
}
.summary-cell dd {
  margin: 5px 0 0;
  font-family: var(--oj-font-mono);
  font-size: 28px;
  font-weight: 600;
  line-height: 1;
  color: var(--oj-text);
}
.summary-cell dd small {
  font-size: 11px;
  font-weight: 400;
  color: var(--oj-text-sub);
  margin-left: 2px;
}

/* ---------- 两栏 ---------- */
.dossier-body {
  display: grid;
  grid-template-columns: minmax(0, 1fr) 336px;
  gap: 20px;
  margin-top: 18px;
  align-items: start;
}
.main-col,
.side-col {
  min-width: 0;
}
.side-col {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.panel {
  padding: 16px 18px;
  margin-bottom: 16px;
  background: var(--oj-card);
  border: 1px solid var(--oj-border);
  border-radius: 10px;
  box-shadow: var(--oj-shadow-sm);
}
.main-col .panel:last-child {
  margin-bottom: 0;
}
.panel-compact {
  margin-bottom: 0;
}
.side-col .panel {
  margin-bottom: 0;
}
.panel-box {
  padding: 16px 18px 18px;
}
.panel-head {
  display: flex;
  align-items: baseline;
  gap: 10px;
  padding-bottom: 9px;
  margin-bottom: 12px;
  border-bottom: 1px solid var(--oj-border);
}
.panel-kicker {
  font-family: var(--oj-font-mono);
  font-size: 8px;
  letter-spacing: 0.08em;
  color: var(--oj-text-weak);
}
.panel-title {
  margin: 0;
  font-size: 17px;
  font-weight: 750;
  letter-spacing: -0.03em;
  white-space: nowrap;
}
.panel-meta {
  font-size: 10.5px;
  color: var(--oj-text-sub);
}
.panel-meta b {
  color: var(--oj-text);
  font-family: var(--oj-font-mono);
  font-weight: 600;
}
.panel-link {
  margin-left: auto;
  font-size: 10.5px;
  color: var(--oj-primary);
  white-space: nowrap;
}
.panel-link:hover {
  color: var(--oj-primary-hover);
}

/* ---------- 热力图 ---------- */
.legend {
  display: flex;
  align-items: center;
  gap: 3px;
  margin-left: auto;
  font-size: 10px;
  color: var(--oj-text-weak);
  flex-shrink: 0;
}
.lv {
  width: 9px;
  height: 9px;
  display: inline-block;
  border-radius: 2px;
}
.lv-0 { background: var(--oj-heat-0); }
.lv-1 { background: var(--oj-heat-1); }
.lv-2 { background: var(--oj-heat-2); }
.lv-3 { background: var(--oj-heat-3); }
.lv-4 { background: var(--oj-heat-4); }
.heatmap-scroll {
  overflow-x: auto;
  min-height: 120px;
  padding-bottom: 4px;
  scrollbar-width: thin;
  scrollbar-color: var(--oj-border-strong) transparent;
}
.heatmap-scroll::-webkit-scrollbar {
  height: 4px;
}
.heatmap-scroll::-webkit-scrollbar-thumb {
  background: var(--oj-border-strong);
  border-radius: 2px;
}
.heatmap {
  min-width: 620px;
  --heat-cell: 20px;
  --heat-gap: 4px;
}
.month-row {
  margin-left: 24px;
  height: var(--heat-cell);
  font-family: var(--oj-font-mono);
  font-size: 9px;
  color: var(--oj-text-weak);
  white-space: nowrap;
}
.heat-body {
  display: flex;
  gap: 6px;
}
.weekday-col {
  display: grid;
  grid-template-rows: repeat(7, var(--heat-cell));
  row-gap: var(--heat-gap);
  font-family: var(--oj-font-mono);
  font-size: 9px;
  color: var(--oj-text-weak);
  width: 18px;
  flex-shrink: 0;
  text-align: right;
  line-height: var(--heat-cell);
}
.wk-blank {
  visibility: hidden;
}
.heat-grid {
  row-gap: var(--heat-gap);
}
.heat-col {
  display: grid;
  grid-template-rows: repeat(7, var(--heat-cell));
  row-gap: var(--heat-gap);
  width: var(--heat-cell);
}
.cell {
  width: var(--heat-cell);
  height: var(--heat-cell);
  border-radius: 3px;
}
.cell.lv-0 { background: var(--oj-heat-0); }
.cell.lv-1 { background: var(--oj-heat-1); }
.cell.lv-2 { background: var(--oj-heat-2); }
.cell.lv-3 { background: var(--oj-heat-3); }
.cell.lv-4 { background: var(--oj-heat-4); }

/* ---------- 最近动态 ---------- */
.activity-list {
  list-style: none;
  margin: 0;
  padding: 0;
}
.activity-row {
  display: flex;
  align-items: center;
  gap: 9px;
  padding: 9px 0;
  border-bottom: 1px solid var(--oj-border);
}
.activity-list li:last-child .activity-row {
  border-bottom: 0;
}
.status-dot {
  width: 6px;
  height: 6px;
  border-radius: 50%;
  flex-shrink: 0;
}
.activity-main {
  flex: 1;
  min-width: 0;
}
.act-problem {
  display: block;
  font-size: 13px;
  font-weight: 500;
  color: var(--oj-text);
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}
.act-problem:hover {
  color: var(--oj-primary);
}
.act-meta {
  font-size: 9.5px;
  color: var(--oj-text-weak);
}
.act-status {
  font-size: 11.5px;
  font-weight: 600;
  flex-shrink: 0;
}
.act-time {
  width: 68px;
  text-align: right;
  font-family: var(--oj-font-mono);
  font-size: 9.5px;
  color: var(--oj-text-weak);
  flex-shrink: 0;
}

/* ---------- 收藏 ---------- */
.fav-link {
  color: var(--oj-text);
  font-weight: 500;
}
.fav-link:hover {
  color: var(--oj-primary);
}
.fav-time {
  font-size: 10px;
  color: var(--oj-text-weak);
}

/* ---------- 进度 ---------- */
.diff-list {
  display: flex;
  flex-direction: column;
  gap: 13px;
}
.diff-row {
  display: grid;
  grid-template-columns: 58px 1fr 48px;
  align-items: center;
  gap: 10px;
}
.diff-track {
  height: 4px;
  border-radius: 99px;
  background: var(--oj-gray-100);
  display: block;
}
.diff-bar {
  display: block;
  height: 100%;
  border-radius: 99px;
  transition: width 0.3s var(--oj-ease);
}
.bar-1 { background: var(--oj-success); }
.bar-2 { background: var(--oj-warning); }
.bar-3 { background: var(--oj-danger); }
.diff-count {
  font-size: 11px;
  color: var(--oj-text);
  text-align: right;
}
.diff-count .of {
  color: var(--oj-text-weak);
}

/* ---------- 语言 ---------- */
.lang-list {
  display: flex;
  flex-direction: column;
  gap: 11px;
}
.lang-row {
  display: grid;
  grid-template-columns: 62px 1fr;
  grid-template-rows: auto auto;
  gap: 2px 8px;
  align-items: center;
}
.lang-name {
  font-size: 10.5px;
  color: var(--oj-gray-700);
}
.lang-track {
  height: 4px;
  border-radius: 99px;
  background: var(--oj-gray-100);
  display: block;
  overflow: hidden;
}
.lang-bar {
  display: block;
  height: 100%;
  border-radius: 99px;
  background: var(--oj-primary);
  transition: width 0.3s var(--oj-ease);
}
.lang-count {
  grid-column: 2;
  font-size: 9px;
  color: var(--oj-text-weak);
}

/* ---------- 账号设置入口 ---------- */
.settings-entry .settings-copy {
  margin: 0 0 14px;
  font-size: 11px;
  line-height: 1.7;
  color: var(--oj-text-sub);
}
.settings-entry .settings-link {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  color: var(--oj-primary);
  font-size: 11.5px;
  font-weight: 700;
  text-decoration: none;
}
.settings-entry .settings-link:hover {
  text-decoration: underline;
}

@media (max-width: 1120px) {
  .heatmap {
    --heat-cell: 16px;
    --heat-gap: 3px;
    min-width: 520px;
  }
}
@media (max-width: 1020px) {
  .dossier-body {
    grid-template-columns: 1fr;
    gap: 0;
  }
  .side-col {
    display: grid;
    grid-template-columns: 1fr 1fr;
    gap: 16px;
    margin-top: 0;
  }
  .settings-entry {
    grid-column: 1 / -1;
  }
}
@media (max-width: 760px) {
  .dossier {
    padding: 30px 18px 52px;
  }
  .dossier-head {
    padding: 16px 16px 0;
  }
  .dossier-id {
    gap: 14px;
  }
  .avatar,
  .avatar-img {
    width: 64px;
    height: 64px;
    font-size: 24px;
  }
  .name {
    font-size: 24px;
  }
  .dossier-summary {
    grid-template-columns: 1fr 1fr;
    margin: 14px -16px 0;
  }
  .summary-cell {
    padding: 12px 16px;
  }
  .summary-cell:nth-child(2) {
    border-right: 0;
  }
  .summary-cell:nth-child(3) {
    border-top: 1px solid var(--oj-border);
  }
  .summary-cell:nth-child(4) {
    border-top: 1px solid var(--oj-border);
  }
  .dossier-body {
    margin-top: 14px;
  }
  .side-col {
    grid-template-columns: 1fr;
    gap: 16px;
  }
  .panel-title {
    font-size: 16px;
  }
  .panel-head {
    flex-wrap: wrap;
    gap: 6px 10px;
  }
  .legend {
    margin-left: 0;
  }
}
</style>
