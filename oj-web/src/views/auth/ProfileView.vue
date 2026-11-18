<template>
  <div class="page-container profile-page">
    <!-- 资料头卡：身份区 + 就地编辑 + 关键数据行。个人主页的"门面" -->
    <div class="card profile-hero">
      <div class="hero-top">
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
            <svg viewBox="0 0 24 24" width="16" height="16" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
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

        <div class="hero-id">
          <div class="name-row">
            <h2 class="name">{{ userStore.username }}</h2>
            <span class="role-chip" :class="{ admin: userStore.isAdmin }">
              {{ userStore.isAdmin ? '管理员' : '学生' }}
            </span>
          </div>
          <!-- 签名：展示态 / 编辑态就地切换 -->
          <div v-if="!sigEditing" class="sig-row">
            <p class="sig-text" :class="{ empty: !signature }">
              {{ signature || '还没有签名，写一句给自己的话吧' }}
            </p>
            <el-button text size="small" @click="openSigEdit">编辑签名</el-button>
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
            <span>注册于 {{ formatDateTime(info?.createdAt) || '—' }}</span>
            <span class="dot" aria-hidden="true">·</span>
            <span>{{ info?.email || '邮箱未设置' }}</span>
          </div>
        </div>

        <!-- 关键数据：等宽大数字 + 灰标签，分隔线用发丝线 -->
        <div class="hero-stats">
          <div class="stat">
            <span class="stat-num mono">{{ statistics?.solvedCount ?? '—' }}</span>
            <span class="stat-label">解题数</span>
          </div>
          <div class="stat">
            <span class="stat-num mono">{{ statistics?.totalSubmit ?? '—' }}</span>
            <span class="stat-label">提交数</span>
          </div>
          <div class="stat">
            <span class="stat-num mono">{{ statistics ? `${statistics.acceptRate}%` : '—' }}</span>
            <span class="stat-label">通过率</span>
          </div>
          <div class="stat">
            <span class="stat-num mono">{{ grid ? grid.streak : '—' }}</span>
            <span class="stat-label">连续刷题(天)</span>
          </div>
        </div>
      </div>
    </div>

    <!-- 刷题热力图：近 26 周逐日提交密度 -->
    <div class="card heatmap-card">
      <div class="card-head">
        <h2>刷题热力图</h2>
        <p class="card-sub">
          近 26 周共 <b>{{ grid?.totalInRange ?? 0 }}</b> 次提交 ·
          活跃 <b>{{ grid?.activeDays ?? 0 }}</b> 天
        </p>
        <div class="legend" aria-hidden="true">
          <span>少</span>
          <span class="lv lv-0" /><span class="lv lv-1" /><span class="lv lv-2" /><span class="lv lv-3" /><span class="lv lv-4" />
          <span>多</span>
        </div>
      </div>
      <div class="heatmap-scroll" v-loading="heatmapLoading">
        <div v-if="grid" class="heatmap" role="img" aria-label="近 26 周提交热力图">
          <div class="month-row">
            <span v-for="(m, i) in grid.monthLabels" :key="i" class="month" :style="{ gridColumn: `${m.column + 1}` }">
              {{ m.label }}
            </span>
          </div>
          <div class="heat-body">
            <div class="weekday-col" aria-hidden="true">
              <span>日</span><span class="wk-blank" /><span>二</span><span class="wk-blank" /><span>四</span><span class="wk-blank" /><span>六</span>
            </div>
            <div class="heat-grid" :style="{ gridTemplateColumns: `repeat(${grid.columns.length}, 1fr)` }">
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
        <el-empty v-else-if="!heatmapLoading" description="暂无活动数据" :image-size="72" />
      </div>
    </div>

    <div class="profile-grid">
      <!-- 左列：语言分布 -->
      <div class="card lang-card">
        <div class="card-head">
          <h2>语言分布</h2>
          <router-link class="more-link" to="/statistics">完整统计 →</router-link>
        </div>
        <div v-if="langDist.length" class="lang-list">
          <div v-for="row in langDist" :key="row.language" class="lang-row">
            <span class="lang-name mono">{{ langLabel(row.language) }}</span>
            <div class="lang-bar-track">
              <div
                class="lang-bar"
                :style="{ width: `${langPercent(row.total)}%` }"
                :aria-label="`${row.language} ${row.total} 次`"
              />
            </div>
            <span class="lang-count mono tabular">{{ row.total }} 次 · AC {{ row.accepted }}</span>
          </div>
        </div>
        <el-empty v-else description="还没有提交记录" :image-size="72" />
      </div>

      <!-- 右列：最近动态 -->
      <div class="card activity-card">
        <div class="card-head">
          <h2>最近动态</h2>
          <router-link class="more-link" to="/submissions">全部记录 →</router-link>
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
        <el-empty v-else description="还没有提交，去题库写第一题吧" :image-size="72" />
      </div>
    </div>

    <div class="profile-grid">
      <!-- 我的收藏 -->
      <div class="card fav-card">
        <div class="card-head">
          <h2>我的收藏</h2>
          <p class="card-sub">"以后要做"的候选题单</p>
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
          <el-table-column label="难度" width="100">
            <template #default="{ row }">
              <span class="oj-diff" :class="`diff-${row.difficulty}`">
                {{ ({1:'简单',2:'中等',3:'困难'} as Record<number,string>)[row.difficulty] || '?' }}
              </span>
            </template>
          </el-table-column>
          <el-table-column label="收藏时间" width="160">
            <template #default="{ row }">
              <span class="mono tabular">{{ formatDateTime(row.favoritedAt) }}</span>
            </template>
          </el-table-column>
        </el-table>
      </div>

      <!-- 账号安全 -->
      <div class="card security-card">
        <h2>账号安全</h2>
        <div class="sec-block">
          <h3>邮箱</h3>
          <el-form :model="emailForm" label-width="72px" class="sec-form">
            <el-form-item label="新邮箱">
              <el-input v-model="emailForm.email" placeholder="example@example.com" />
            </el-form-item>
            <el-form-item>
              <el-button :loading="savingEmail" @click="saveEmail">保存邮箱</el-button>
            </el-form-item>
          </el-form>
        </div>
        <div class="sec-block">
          <h3>密码</h3>
          <el-form :model="pwdForm" label-width="72px" class="sec-form">
            <el-form-item label="原密码">
              <el-input v-model="pwdForm.oldPassword" type="password" show-password />
            </el-form-item>
            <el-form-item label="新密码">
              <el-input v-model="pwdForm.newPassword" type="password" show-password placeholder="6-32 位" />
            </el-form-item>
            <el-form-item label="确认密码">
              <el-input v-model="pwdForm.confirm" type="password" show-password />
            </el-form-item>
            <el-form-item>
              <el-button type="warning" :loading="savingPwd" @click="savePassword">修改密码</el-button>
            </el-form-item>
          </el-form>
          <p class="tip">修改密码成功后当前登录状态将失效，需重新登录。</p>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
// 个人主页（论文表 3-1 个人信息维护 + 活动总览）。
// 数据源：auth/info + statistics + trend(182 天热力图) + languages + pageMy(最近 8 条) + favorites。
import { computed, onMounted, reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import {
  changePassword,
  getUserInfo,
  updateAvatar,
  updateEmail,
  updateSignature,
  type LoginVO,
} from '@/api/auth'
import { pageSubmissions, getStatistics, getTrend, getLanguageDist, type SubmissionDetail, type Statistics } from '@/api/submission'
import { myFavorites } from '@/api/favorite'
import { useUserStore } from '@/stores/user'
import { disconnectWs } from '@/api/ws'
import { formatDateTime, relativeTime } from '@/utils/datetime'
import { buildHeatmapGrid, type HeatmapGrid } from '@/utils/heatmap'
import { statusColor, statusLabel, formatTimeUsed } from '@/utils/status'

const router = useRouter()
const userStore = useUserStore()

const info = ref<LoginVO>()
const statistics = ref<Statistics>()
const grid = ref<HeatmapGrid>()
const heatmapLoading = ref(false)
const langDist = ref<{ language: string; total: number; accepted: number }[]>([])
const recent = ref<SubmissionDetail[]>([])
const favorites = ref<{ problemId: number; title: string; difficulty: number; favoritedAt: string }[]>([])
const favLoading = ref(false)
const emailForm = reactive({ email: '' })
const pwdForm = reactive({ oldPassword: '', newPassword: '', confirm: '' })
const savingEmail = ref(false)
const savingPwd = ref(false)
const avatarInput = ref<HTMLInputElement>()
const uploadingAvatar = ref(false)
const sigEditing = ref(false)
const sigDraft = ref('')
const savingSig = ref(false)

const initial = computed(() => (userStore.username || '?').slice(0, 1).toUpperCase())
const avatarUrl = computed(() => userStore.avatar || info.value?.avatar || '')
const signature = computed(() => info.value?.signature || '')

/** 语言 code → 显示名（Language 枚举口径） */
const LANG_LABELS: Record<string, string> = {
  c: 'C', cpp: 'C++', java: 'Java', python3: 'Python', js: 'JavaScript',
}
function langLabel(code: string) {
  return LANG_LABELS[code] ?? code
}
function langPercent(total: number) {
  const max = langDist.value[0]?.total || 1
  return Math.max(6, Math.round((total / max) * 100))
}

onMounted(async () => {
  // refreshUserInfo 同时同步顶栏头像（store），并拿回签名/邮箱/注册时间
  info.value = await userStore.refreshUserInfo()
  emailForm.email = info.value.email || ''
  heatmapLoading.value = true
  try {
    // 五路数据互不依赖，并行拉取；任何一路失败不影响其它区块（Promise.allSettled）
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

/** 选图 → 居中裁剪 256px → webp 压缩 → data URL 上传（API.md v1.8 §1.5b） */
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
    const dataUrl = await cropToSquareDataUrl(file)
    await updateAvatar(dataUrl)
    await userStore.refreshUserInfo()
    info.value = await getUserInfo()
    ElMessage.success('头像已更新')
  } finally {
    uploadingAvatar.value = false
  }
}

async function cropToSquareDataUrl(file: File, size = 256): Promise<string> {
  const bitmap = await createImageBitmap(file)
  const side = Math.min(bitmap.width, bitmap.height)
  const sx = (bitmap.width - side) / 2
  const sy = (bitmap.height - side) / 2
  const canvas = document.createElement('canvas')
  canvas.width = size
  canvas.height = size
  const ctx = canvas.getContext('2d')!
  ctx.imageSmoothingQuality = 'high'
  ctx.drawImage(bitmap, sx, sy, side, side, 0, 0, size, size)
  bitmap.close()
  const webp = canvas.toDataURL('image/webp', 0.85)
  return webp.startsWith('data:image/webp') ? webp : canvas.toDataURL('image/jpeg', 0.85)
}

async function saveEmail() {
  if (!emailForm.email) {
    ElMessage.warning('请输入邮箱')
    return
  }
  savingEmail.value = true
  try {
    await updateEmail(emailForm.email)
    ElMessage.success('邮箱已更新')
    info.value = await getUserInfo()
  } finally {
    savingEmail.value = false
  }
}

async function savePassword() {
  if (!pwdForm.oldPassword || !pwdForm.newPassword) {
    ElMessage.warning('请填写原密码与新密码')
    return
  }
  if (pwdForm.newPassword !== pwdForm.confirm) {
    ElMessage.warning('两次输入的新密码不一致')
    return
  }
  savingPwd.value = true
  try {
    await changePassword(pwdForm.oldPassword, pwdForm.newPassword)
    ElMessage.success('密码已修改，请重新登录')
    userStore.logout()
    disconnectWs()
    router.push('/login')
  } finally {
    savingPwd.value = false
  }
}
</script>

<style scoped>
.profile-page {
  max-width: 980px;
  margin: 0 auto;
}
.profile-grid {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 16px;
  margin-top: 16px;
  align-items: start;
}
@media (max-width: 860px) {
  .profile-grid {
    grid-template-columns: 1fr;
  }
}
.card-head {
  display: flex;
  align-items: baseline;
  gap: 12px;
  flex-wrap: wrap;
  margin-bottom: 14px;
}
.card-head h2 {
  margin: 0;
}
.card-sub {
  margin: 0;
  font-size: 12.5px;
  color: var(--oj-text-sub);
}
.card-sub b {
  color: var(--oj-text);
  font-variant-numeric: tabular-nums;
}
.more-link {
  margin-left: auto;
  font-size: 12.5px;
  color: var(--oj-primary);
}
.more-link:hover {
  text-decoration: underline;
}

/* ---------- 资料头 ---------- */
.profile-hero {
  padding: 24px 28px;
}
.hero-top {
  display: flex;
  align-items: flex-start;
  gap: 22px;
}
.avatar-box {
  position: relative;
  padding: 0;
  border: none;
  background: none;
  cursor: pointer;
  border-radius: 8px;
  line-height: 0;
  flex-shrink: 0;
}
.avatar-box:disabled {
  cursor: wait;
}
.avatar {
  width: 88px;
  height: 88px;
  border-radius: 8px;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  background: var(--oj-gray-900);
  color: var(--oj-white);
  font-size: 34px;
  font-weight: 600;
}
.avatar-img {
  object-fit: cover;
  border: 1px solid var(--oj-border);
}
.avatar-mask {
  position: absolute;
  inset: 0;
  display: none;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 4px;
  border-radius: 8px;
  background: rgba(31, 35, 40, 0.62);
  color: var(--oj-white);
  font-size: 11px;
  line-height: 1;
}
.avatar-box:hover .avatar-mask,
.avatar-box:focus-visible .avatar-mask {
  display: flex;
}

.hero-id {
  min-width: 0;
  flex: 1;
}
.name-row {
  display: flex;
  align-items: center;
  gap: 10px;
}
.name {
  margin: 0;
  font-size: 20px;
  font-weight: 600;
}
.role-chip {
  font-size: 11px;
  font-weight: 600;
  color: var(--oj-gray-500);
  border: 1px solid var(--oj-border);
  border-radius: 4px;
  padding: 1px 6px;
}
.role-chip.admin {
  color: var(--oj-primary);
  border-color: var(--oj-info-border);
  background: var(--oj-primary-soft);
}
.sig-row {
  display: flex;
  align-items: baseline;
  gap: 12px;
  margin-top: 10px;
  padding: 8px 14px;
  border: 1px dashed var(--oj-border-strong);
  border-radius: var(--oj-radius-md);
  background: var(--oj-gray-50);
}
.sig-text {
  margin: 0;
  font-size: 13.5px;
  line-height: 1.6;
  color: var(--oj-text);
  flex: 1;
  min-width: 0;
}
.sig-text.empty {
  color: var(--oj-text-weak);
}
.sig-editor {
  margin-top: 10px;
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
  gap: 8px;
  margin: 10px 0 0;
  font-size: 12px;
  color: var(--oj-text-sub);
}
.meta .dot {
  color: var(--oj-gray-300);
}

/* 关键数据：数字说话，位置代替色块 */
.hero-stats {
  display: flex;
  gap: 0;
  align-self: stretch;
  align-items: center;
  flex-shrink: 0;
  border-left: 1px solid var(--oj-border);
  padding-left: 24px;
  margin-left: 8px;
}
.stat {
  display: flex;
  flex-direction: column;
  align-items: center;
  min-width: 76px;
  padding: 0 14px;
}
.stat + .stat {
  border-left: 1px solid var(--oj-border);
}
.stat-num {
  font-size: 24px;
  font-weight: 600;
  line-height: 1.15;
  color: var(--oj-text);
  letter-spacing: -0.5px;
  font-variant-numeric: tabular-nums;
}
.stat-label {
  margin-top: 4px;
  font-size: 11.5px;
  color: var(--oj-text-sub);
  white-space: nowrap;
}
@media (max-width: 760px) {
  .hero-top {
    flex-direction: column;
  }
  .hero-stats {
    border-left: none;
    margin-left: 0;
    padding-left: 0;
    width: 100%;
    justify-content: space-between;
    border-top: 1px solid var(--oj-border);
    padding-top: 16px;
  }
}

/* ---------- 热力图 ---------- */
.heatmap-card {
  margin-top: 16px;
}
.legend {
  display: flex;
  align-items: center;
  gap: 4px;
  margin-left: auto;
  font-size: 11px;
  color: var(--oj-text-weak);
}
.lv {
  width: 10px;
  height: 10px;
  border-radius: 2px;
  display: inline-block;
}
.lv-0 { background: var(--oj-gray-100); }
.lv-1 { background: #c6e6c9; }
.lv-2 { background: #7fce8a; }
.lv-3 { background: #3aa04b; }
.lv-4 { background: var(--oj-success); }
.heatmap-scroll {
  overflow-x: auto;
  min-height: 120px;
}
.heatmap {
  min-width: 620px;
}
.month-row {
  position: relative;
  height: 16px;
  font-size: 11px;
  color: var(--oj-text-weak);
  margin-left: 26px;
}
.month {
  position: absolute;
  transform: translateX(2px);
}
.heat-body {
  display: flex;
  gap: 6px;
}
.weekday-col {
  display: grid;
  grid-template-rows: repeat(7, 11px);
  row-gap: 3px;
  font-size: 10px;
  color: var(--oj-text-weak);
  width: 18px;
  flex-shrink: 0;
  text-align: right;
  line-height: 11px;
}
.wk-blank {
  visibility: hidden;
}
.heat-grid {
  display: grid;
  gap: 3px;
  flex: 1;
}
.heat-col {
  display: grid;
  grid-template-rows: repeat(7, 11px);
  row-gap: 3px;
}
.cell {
  width: 11px;
  height: 11px;
  border-radius: 2px;
  background: var(--oj-gray-100);
}
.cell.lv-0 { background: var(--oj-gray-100); }
.cell.lv-1 { background: #c6e6c9; }
.cell.lv-2 { background: #7fce8a; }
.cell.lv-3 { background: #3aa04b; }
.cell.lv-4 { background: var(--oj-success); }

/* ---------- 语言分布 ---------- */
.lang-list {
  display: flex;
  flex-direction: column;
  gap: 12px;
}
.lang-row {
  display: grid;
  grid-template-columns: 84px 1fr auto;
  align-items: center;
  gap: 12px;
}
.lang-name {
  font-size: 12.5px;
  color: var(--oj-gray-700);
}
.lang-bar-track {
  height: 8px;
  border-radius: 4px;
  background: var(--oj-gray-100);
  overflow: hidden;
}
.lang-bar {
  height: 100%;
  border-radius: 4px;
  background: var(--oj-primary);
  transition: width 0.3s var(--oj-ease);
}
.lang-count {
  font-size: 11.5px;
  color: var(--oj-text-sub);
  white-space: nowrap;
}

/* ---------- 最近动态 ---------- */
.activity-list {
  list-style: none;
  margin: 0;
  padding: 0;
}
.activity-row {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 9px 0;
}
.activity-row + .activity-row {
  border-top: 1px solid var(--oj-gray-100);
}
.status-dot {
  width: 7px;
  height: 7px;
  border-radius: 50%;
  flex-shrink: 0;
}
.activity-main {
  flex: 1;
  min-width: 0;
}
.act-problem {
  font-size: 13.5px;
  font-weight: 500;
  color: var(--oj-text);
  display: block;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}
.act-problem:hover {
  color: var(--oj-primary);
}
.act-meta {
  font-size: 11.5px;
  color: var(--oj-text-sub);
}
.act-status {
  font-size: 12px;
  font-weight: 600;
  flex-shrink: 0;
}
.act-time {
  font-size: 11.5px;
  color: var(--oj-text-weak);
  width: 64px;
  text-align: right;
  flex-shrink: 0;
}

/* ---------- 收藏 / 账号安全 ---------- */
.fav-link {
  color: var(--oj-text);
  font-weight: 500;
}
.fav-link:hover {
  color: var(--oj-primary);
}
.security-card h2 {
  margin-bottom: 4px;
}
.sec-block + .sec-block {
  margin-top: 8px;
  padding-top: 12px;
  border-top: 1px solid var(--oj-border);
}
.sec-block h3 {
  margin: 0 0 10px;
  font-size: 13px;
  color: var(--oj-gray-600);
}
.sec-form {
  max-width: 340px;
}
.tip {
  color: var(--oj-text-weak);
  font-size: 12px;
  margin: 4px 0 0;
}
</style>
