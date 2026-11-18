<template>
  <div class="page-container profile-page">
    <!-- 资料头卡：头像 + 身份 + 签名。签名与头像都可就地编辑，这是个人页的灵魂区 -->
    <div class="card profile-card">
      <div class="profile-head">
        <!-- 头像：hover 出现"更换"遮罩，点击打开文件选择（API.md v1.8 §1.5b） -->
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
              <path d="M9 12a3 3 0 1 0 6 0 3 3 0 0 0-6 0z" /><path d="M12 3c2.4 0 4.6.9 6.3 2.4l1.7-1.4v5h-5l1.9-1.6A7 7 0 1 0 19 12h2a9 9 0 1 1-9-9z" />
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

        <div class="profile-id">
          <div class="name-row">
            <h2 class="name">{{ userStore.username }}</h2>
            <span class="role-chip" :class="{ admin: userStore.isAdmin }">
              {{ userStore.isAdmin ? '管理员' : '学生' }}
            </span>
          </div>
          <div class="meta mono">
            <span>UID {{ info?.userId ?? userStore.userId }}</span>
            <span class="dot" aria-hidden="true">·</span>
            <span>注册于 {{ formatDateTime(info?.createdAt) || '—' }}</span>
            <span class="dot" aria-hidden="true">·</span>
            <span>{{ info?.email || '邮箱未设置' }}</span>
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
        </div>
      </div>
    </div>

    <div class="profile-grid">
      <!-- 账号安全：邮箱与密码两列并排，密级一致的操作放在一起 -->
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
              <!-- 改密会强制下线，用 warning 与"保存邮箱"区分副作用强度 -->
              <el-button type="warning" :loading="savingPwd" @click="savePassword">修改密码</el-button>
            </el-form-item>
          </el-form>
          <p class="tip">修改密码成功后当前登录状态将失效，需重新登录。</p>
        </div>
      </div>

      <!-- 我的收藏 -->
      <div class="card fav-card">
        <h2>我的收藏</h2>
        <p class="card-sub">收藏是"以后要做"的候选题单，点击直接进入题目</p>
        <el-table
          :data="favorites"
          v-loading="favLoading"
          size="small"
          empty-text="还没有收藏的题目，去题库逛逛吧"
        >
          <el-table-column label="题目" min-width="220">
            <template #default="{ row }">
              <router-link class="fav-link" :to="`/problems/${row.problemId}`">{{ row.title }}</router-link>
            </template>
          </el-table-column>
          <el-table-column label="难度" width="110">
            <template #default="{ row }">
              <span class="oj-diff" :class="`diff-${row.difficulty}`">
                {{ ({1:'简单',2:'中等',3:'困难'} as Record<number,string>)[row.difficulty] || '?' }}
              </span>
            </template>
          </el-table-column>
          <el-table-column label="收藏时间" width="170">
            <template #default="{ row }">
              <span class="mono tabular">{{ formatDateTime(row.favoritedAt) }}</span>
            </template>
          </el-table-column>
        </el-table>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
// 个人中心（论文表 3-1：个人信息维护）。头像/签名走 API.md v1.8 新契约。
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
import { myFavorites } from '@/api/favorite'
import { useUserStore } from '@/stores/user'
import { disconnectWs } from '@/api/ws'
import { formatDateTime } from '@/utils/datetime'

const router = useRouter()
const userStore = useUserStore()

const info = ref<LoginVO>()
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

onMounted(async () => {
  // refreshUserInfo 同时同步顶栏头像（store），并拿回签名/邮箱/注册时间
  info.value = await userStore.refreshUserInfo()
  emailForm.email = info.value.email || ''
  favLoading.value = true
  try {
    favorites.value = await myFavorites()
  } finally {
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

/**
 * 选中头像文件 → 前端居中裁剪成正方形 → 压缩成 webp/jpeg data URL 上传。
 * 前端裁剪的意义：原图直接上传几百 KB 到几 MB，前端压到几十 KB，
 * 落盘小、加载快，也稳稳落在后端 300KB 校验线以内（API.md v1.8 §1.5b）。
 */
async function onAvatarFile(e: Event) {
  const input = e.target as HTMLInputElement
  const file = input.files?.[0]
  // 允许重复选择同一文件：每次选择后清空 value，否则 change 不触发
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

/** 居中裁剪成正方形并编码：256px，webp 优先（体积最小），不支持时回退 jpeg */
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
  max-width: 900px;
  margin: 0 auto;
}
.profile-page > .card + .profile-grid {
  margin-top: 16px;
}
.profile-grid {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 16px;
  align-items: start;
}
@media (max-width: 860px) {
  .profile-grid {
    grid-template-columns: 1fr;
  }
}

/* ---------- 资料头 ---------- */
.profile-head {
  display: flex;
  align-items: flex-start;
  gap: 20px;
}
/* 头像容器：悬停出现半透明"更换"遮罩，是上传入口 */
.avatar-box {
  position: relative;
  padding: 0;
  border: none;
  background: none;
  cursor: pointer;
  border-radius: 6px;
  line-height: 0;
  flex-shrink: 0;
}
.avatar-box:disabled {
  cursor: wait;
}
.avatar {
  width: 72px;
  height: 72px;
  border-radius: 6px;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  background: var(--oj-gray-900);
  color: var(--oj-white);
  font-size: 28px;
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
  gap: 3px;
  border-radius: 6px;
  background: rgba(31, 35, 40, 0.62);
  color: var(--oj-white);
  font-size: 11px;
  line-height: 1;
}
.avatar-box:hover .avatar-mask,
.avatar-box:focus-visible .avatar-mask {
  display: flex;
}

.profile-id {
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
  font-size: 19px;
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
.meta {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  gap: 8px;
  margin: 8px 0 0;
  font-size: 12px;
  color: var(--oj-text-sub);
}
.meta .dot {
  color: var(--oj-gray-300);
}

/* 签名：展示态像一行引言，编辑态就地展开 */
.sig-row {
  display: flex;
  align-items: baseline;
  gap: 12px;
  margin-top: 12px;
  padding: 10px 14px;
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
  margin-top: 12px;
}
.sig-actions {
  display: flex;
  justify-content: flex-end;
  gap: 8px;
  margin-top: 8px;
}

/* ---------- 账号安全 / 收藏 ---------- */
.security-card h2,
.fav-card h2 {
  margin-bottom: 4px;
}
.card-sub {
  margin: 0 0 12px;
  font-size: 12.5px;
  color: var(--oj-text-sub);
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
.fav-link {
  color: var(--oj-text);
  font-weight: 500;
}
.fav-link:hover {
  color: var(--oj-primary);
}
</style>
