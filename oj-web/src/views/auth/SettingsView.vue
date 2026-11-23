<template>
  <div class="settings-page">
    <header class="settings-head">
      <div>
        <h1>账号设置</h1>
        <p class="settings-sub">
          管理公开资料、登录安全与在线编辑器偏好。账号资料写回服务端，编辑器偏好只保存在当前浏览器。
        </p>
      </div>
      <router-link class="back-link" to="/profile">返回个人中心</router-link>
    </header>

    <section class="identity-strip" aria-label="当前账号">
      <button
        class="avatar-button"
        type="button"
        :disabled="uploadingAvatar"
        aria-label="更换头像"
        @click="avatarInput?.click()"
      >
        <img v-if="avatarUrl" class="avatar avatar-img" :src="avatarUrl" alt="我的头像" />
        <span v-else class="avatar">{{ initial }}</span>
        <span class="avatar-mask" aria-hidden="true">{{ uploadingAvatar ? '上传中…' : '更换' }}</span>
      </button>
      <input
        ref="avatarInput"
        type="file"
        accept="image/png,image/jpeg,image/webp"
        class="visually-hidden"
        tabindex="-1"
        @change="onAvatarFile"
      />
      <div class="identity-copy">
        <div class="identity-name">
          <strong>{{ userStore.username || '未登录' }}</strong>
          <span class="role-text">{{ roleLabel }}</span>
        </div>
        <p class="identity-meta">
          ID {{ info?.userId ?? userStore.userId }}
          <span aria-hidden="true">·</span>
          {{ info?.email || '邮箱未设置' }}
        </p>
      </div>
      <span class="identity-state">已登录</span>
    </section>

    <div class="settings-layout">
      <nav class="settings-nav" aria-label="设置分区">
        <button
          v-for="item in SECTION_LINKS"
          :key="item.id"
          type="button"
          class="settings-nav-link"
          :class="{ active: activeSection === item.id }"
          :aria-current="activeSection === item.id ? 'location' : undefined"
          @click="jumpTo(item.id)"
        >
          {{ item.label }}
        </button>
      </nav>

      <main class="settings-main">
        <section id="account" class="settings-panel">
          <div class="panel-head">
            <div>
              <h2>账号资料</h2>
            </div>
            <span class="panel-note">用户名与角色不可修改</span>
          </div>

          <div class="field-grid">
            <label class="field">
              <span class="field-label">用户名</span>
              <el-input :model-value="userStore.username" disabled />
            </label>
            <label class="field">
              <span class="field-label">账号角色</span>
              <el-input :model-value="roleLabel" disabled />
            </label>
          </div>

          <label class="field">
            <span class="field-label">邮箱 <small>用于账号通知与找回，可留空</small></span>
            <el-input v-model="email" placeholder="example@example.com" autocomplete="off" name="oj-email" />
          </label>

          <label class="field">
            <span class="field-label">个性签名 <small>展示在个人中心，1–200 字</small></span>
            <el-input
              v-model="signature"
              type="textarea"
              :rows="3"
              maxlength="200"
              show-word-limit
              placeholder="一句话介绍自己"
            />
          </label>

          <div class="panel-actions">
            <el-button type="primary" :loading="savingProfile" :disabled="!profileDirty" @click="saveProfile">
              保存资料
            </el-button>
            <span class="action-hint">修改邮箱后建议重新登录，确保新联系方式生效。</span>
          </div>
        </section>

        <section id="security" class="settings-panel">
          <div class="panel-head">
            <div>
              <h2>登录安全</h2>
            </div>
            <span class="panel-note">密码修改后当前令牌立即失效</span>
          </div>

          <div class="field-grid">
            <label class="field">
              <span class="field-label">原密码</span>
              <el-input v-model="pwdForm.oldPassword" type="password" show-password autocomplete="current-password" />
            </label>
            <label class="field">
              <span class="field-label">新密码 <small>6–32 位</small></span>
              <el-input v-model="pwdForm.newPassword" type="password" show-password autocomplete="new-password" />
            </label>
          </div>
          <label class="field">
            <span class="field-label">确认新密码</span>
            <el-input v-model="pwdForm.confirm" type="password" show-password autocomplete="new-password" />
          </label>

          <div class="security-note">
            <span class="note-mark" aria-hidden="true">!</span>
            <span>修改密码成功后当前登录状态会立即失效，需要重新登录。请确保新密码没有在其他地方复用。</span>
          </div>

          <div class="panel-actions">
            <el-button type="primary" :loading="savingPwd" @click="savePassword">修改密码</el-button>
          </div>

          <div class="session-row">
            <div>
              <p class="session-title">当前会话</p>
              <p class="session-sub">退出后该令牌进入黑名单，需要重新登录才能继续提交。</p>
            </div>
            <el-button plain @click="signOut">退出登录</el-button>
          </div>
        </section>

        <section id="editor" class="settings-panel">
          <div class="panel-head">
            <div>
              <h2>编辑器偏好</h2>
            </div>
            <span class="panel-note">仅保存在当前浏览器</span>
          </div>

          <div class="pref-grid">
            <div class="field">
              <span class="field-label">默认语言</span>
              <el-select v-model="prefs.defaultLanguage" class="full-select" aria-label="默认编程语言">
                <el-option v-for="item in LANGUAGE_OPTIONS" :key="item.value" :label="item.label" :value="item.value" />
              </el-select>
            </div>

            <div class="field">
              <span class="field-label">字号 <b class="mono pref-value">{{ prefs.fontSize }}px</b></span>
              <div class="range-row">
                <el-slider v-model="prefs.fontSize" :min="FONT_SIZE_MIN" :max="FONT_SIZE_MAX" :step="1" :show-tooltip="false" />
                <span class="range-scale mono">{{ FONT_SIZE_MIN }}–{{ FONT_SIZE_MAX }}</span>
              </div>
            </div>

            <div class="field">
              <span class="field-label">缩进宽度</span>
              <el-radio-group v-model="prefs.tabSize" aria-label="缩进宽度">
                <el-radio v-for="size in TAB_SIZE_OPTIONS" :key="size" :value="size">{{ size }} 空格</el-radio>
              </el-radio-group>
            </div>

            <div class="field">
              <span class="field-label">编辑器选项</span>
              <div class="switch-list">
                <label class="switch-row">
                  <span>自动换行</span>
                  <el-switch v-model="prefs.wordWrap" />
                </label>
                <label class="switch-row">
                  <span>代码地图</span>
                  <el-switch v-model="prefs.minimap" />
                </label>
              </div>
            </div>
          </div>

          <div class="editor-preview">
            <div class="preview-head">
              <span>编辑器预览</span>
              <span>{{ prefs.fontSize }}px · 缩进 {{ prefs.tabSize }}{{ prefs.wordWrap ? ' · 自动换行' : '' }}</span>
            </div>
            <pre
              class="preview-code"
              :class="{ wrap: prefs.wordWrap }"
              :style="{ fontSize: `${prefs.fontSize}px`, tabSize: String(prefs.tabSize) }"
            >{{ previewCode }}</pre>
          </div>

          <div class="panel-actions">
            <el-button plain @click="resetPrefs">恢复默认偏好</el-button>
            <span class="action-hint">修改后立即作用于题目详情页的编辑器，无需刷新。</span>
          </div>
        </section>

        <section id="local" class="settings-panel panel-quiet">
          <div class="panel-head">
            <div>
              <h2>本地数据</h2>
            </div>
          </div>
          <div class="local-row">
            <p class="local-copy">
              编辑器偏好保存在当前浏览器的 localStorage 中，不会同步到其他设备，也不会写入用户数据库。
              清除浏览器站点数据后会回到默认值。
            </p>
            <div class="local-actions">
              <el-button plain @click="refreshInfo">刷新账号资料</el-button>
              <el-button text @click="resetPrefs">恢复默认</el-button>
            </div>
          </div>
        </section>
      </main>
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, onUnmounted, reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import {
  changePassword,
  getUserInfo,
  logout,
  updateAvatar,
  updateEmail,
  updateSignature,
  type LoginVO,
} from '@/api/auth'
import { disconnectWs } from '@/api/ws'
import { useUserStore } from '@/stores/user'
import { usePreferencesStore } from '@/stores/preferences'
import { cropToSquareDataUrl } from '@/utils/avatar'
import { LANGUAGE_OPTIONS } from '@/utils/status'
import { FONT_SIZE_MAX, FONT_SIZE_MIN, TAB_SIZE_OPTIONS } from '@/utils/preferences'

const router = useRouter()
const userStore = useUserStore()
const prefs = usePreferencesStore()

/** 左侧分区导航：滚动时高亮当前分区，而不是永久高亮第一项 */
const SECTION_LINKS = [
  { id: 'account', no: '01', label: '账号资料' },
  { id: 'security', no: '02', label: '登录安全' },
  { id: 'editor', no: '03', label: '编辑器偏好' },
  { id: 'local', no: '04', label: '本地数据' },
] as const

type SectionId = (typeof SECTION_LINKS)[number]['id']
const activeSection = ref<SectionId>('account')
let scrollRaf = 0

/** 以视口上方 120px 为基准线：最后一个越过基准线的分区就是当前分区 */
function updateActiveSection() {
  let current: SectionId = SECTION_LINKS[0].id
  for (const item of SECTION_LINKS) {
    const el = document.getElementById(item.id)
    if (!el) continue
    if (el.getBoundingClientRect().top <= 120) current = item.id
  }
  const reachedBottom =
    window.scrollY + window.innerHeight >= document.documentElement.scrollHeight - 4
  if (reachedBottom) current = SECTION_LINKS[SECTION_LINKS.length - 1].id
  activeSection.value = current
}

/** 点击分区导航：立即高亮 + 平滑滚动。不用 <a href="#id">——它会往 URL 里塞 hash，
 *  刷新/回退行为变怪；scroll-spy（onScroll）仍以实际滚动位置为准。 */
function jumpTo(id: SectionId) {
  activeSection.value = id
  document.getElementById(id)?.scrollIntoView({ behavior: 'smooth', block: 'start' })
}

function onScroll() {
  if (scrollRaf) return
  scrollRaf = requestAnimationFrame(() => {
    scrollRaf = 0
    updateActiveSection()
  })
}

const info = ref<LoginVO>()
const email = ref('')
const signature = ref('')
const initialEmail = ref('')
const initialSignature = ref('')
const savingProfile = ref(false)
const savingPwd = ref(false)
const uploadingAvatar = ref(false)
const avatarInput = ref<HTMLInputElement>()

const pwdForm = reactive({ oldPassword: '', newPassword: '', confirm: '' })
const EMAIL_RE = /^[^\s@]+@[^\s@]+\.[^\s@]+$/

const previewCode = `#include <iostream>

int main() {
    std::cout << "Hello, CodeOJ" << std::endl;
    return 0;
}`

const initial = computed(() => (userStore.username || '?').slice(0, 1).toUpperCase())
const avatarUrl = computed(() => userStore.avatar || info.value?.avatar || '')
const roleLabel = computed(() => (userStore.isAdmin ? '管理员' : '普通用户'))
const profileDirty = computed(
  () => email.value.trim() !== initialEmail.value || signature.value.trim() !== initialSignature.value,
)

async function load() {
  const data = await getUserInfo()
  info.value = data
  email.value = data.email || ''
  signature.value = data.signature || ''
  initialEmail.value = email.value
  initialSignature.value = signature.value
}

onMounted(() => {
  load().catch(() => ElMessage.error('账号资料加载失败，请稍后重试'))
  updateActiveSection()
  window.addEventListener('scroll', onScroll, { passive: true })
  window.addEventListener('resize', onScroll)
})

onUnmounted(() => {
  window.removeEventListener('scroll', onScroll)
  window.removeEventListener('resize', onScroll)
  if (scrollRaf) cancelAnimationFrame(scrollRaf)
})

async function saveProfile() {
  if (!profileDirty.value) return
  const nextEmail = email.value.trim()
  const nextSignature = signature.value.trim()

  if (nextEmail && !EMAIL_RE.test(nextEmail)) {
    ElMessage.warning('请输入合法的邮箱地址')
    return
  }
  if (nextEmail !== initialEmail.value && !nextEmail) {
    ElMessage.warning('邮箱不能清空；如需更换请联系管理员')
    return
  }
  if (nextSignature !== initialSignature.value && !nextSignature) {
    ElMessage.warning('个性签名不能为空')
    return
  }

  savingProfile.value = true
  try {
    if (nextEmail !== initialEmail.value) await updateEmail(nextEmail)
    if (nextSignature !== initialSignature.value) await updateSignature(nextSignature)
    const refreshed = await getUserInfo()
    info.value = refreshed
    initialEmail.value = nextEmail
    initialSignature.value = nextSignature
    ElMessage.success('账号资料已更新')
  } finally {
    savingProfile.value = false
  }
}

async function onAvatarFile(event: Event) {
  const input = event.target as HTMLInputElement
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

async function savePassword() {
  if (!pwdForm.oldPassword || !pwdForm.newPassword) {
    ElMessage.warning('请填写原密码与新密码')
    return
  }
  if (pwdForm.newPassword.length < 6 || pwdForm.newPassword.length > 32) {
    ElMessage.warning('新密码需为 6–32 位')
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
    await router.replace({ name: 'login', query: { redirect: '/settings' } })
  } finally {
    savingPwd.value = false
  }
}

async function signOut() {
  try {
    await logout()
  } catch {
    // token 已过期时也继续清本地状态
  } finally {
    userStore.logout()
    disconnectWs()
    await router.push('/login')
  }
}

function resetPrefs() {
  prefs.reset()
  ElMessage.success('已恢复默认编辑器偏好')
}

async function refreshInfo() {
  try {
    await load()
    ElMessage.success('账号资料已刷新')
  } catch {
    ElMessage.error('刷新失败，请稍后重试')
  }
}
</script>

<style scoped>
.settings-page {
  max-width: 1120px;
  margin: 0 auto;
  padding: 36px 32px 80px;
}

.settings-head {
  display: flex;
  align-items: flex-end;
  justify-content: space-between;
  gap: 24px;
  margin-bottom: 24px;
}

.settings-head h1 {
  margin: 0;
  color: var(--oj-text);
  font-family: var(--oj-font-display);
  font-size: 26px;
  font-weight: 750;
  letter-spacing: -0.03em;
}

.settings-sub {
  max-width: 640px;
  margin: 10px 0 0;
  color: var(--oj-text-sub);
  font-size: 14px;
  line-height: 1.7;
}

.back-link {
  flex: none;
  padding-bottom: 4px;
  color: var(--oj-primary);
  font-size: 13px;
  font-weight: 700;
  text-decoration: none;
}

.back-link:hover {
  text-decoration: underline;
}

.identity-strip {
  display: flex;
  align-items: center;
  gap: 16px;
  padding: 18px 20px;
  margin-bottom: 24px;
  background: var(--oj-card);
  border: 1px solid var(--oj-border);
  border-radius: 4px;
}

.avatar-button {
  position: relative;
  flex: none;
  width: 58px;
  height: 58px;
  padding: 0;
  overflow: hidden;
  border: 1px solid var(--oj-border-strong);
  border-radius: 50%;
  background: var(--oj-gray-100);
  cursor: pointer;
}

.avatar-button:disabled {
  cursor: wait;
}

.avatar {
  display: grid;
  width: 100%;
  height: 100%;
  place-items: center;
  color: var(--oj-primary);
  font-family: var(--oj-font-display);
  font-size: 22px;
  font-weight: 800;
}

.avatar-img {
  object-fit: cover;
}

.avatar-mask {
  position: absolute;
  inset: auto 0 0;
  padding: 4px 0;
  color: #fff;
  background: rgba(16, 24, 40, 0.68);
  font-size: 10px;
  text-align: center;
  opacity: 0;
  transition: opacity 0.16s ease;
}

.avatar-button:hover .avatar-mask,
.avatar-button:focus-visible .avatar-mask {
  opacity: 1;
}

.identity-copy {
  min-width: 0;
}

.identity-name {
  display: flex;
  align-items: center;
  gap: 8px;
  flex-wrap: wrap;
}

.identity-name strong {
  color: var(--oj-text);
  font-family: var(--oj-font-display);
  font-size: 18px;
  font-weight: 800;
}

.role-text {
  color: var(--oj-text-weak);
  font-size: 10.5px;
}

.identity-meta {
  margin: 5px 0 0;
  color: var(--oj-text-sub);
  font-size: 12px;
}

.identity-meta span {
  margin: 0 5px;
  color: var(--oj-gray-400);
}

.identity-state {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  margin-left: auto;
  color: var(--oj-text-sub);
  font-size: 10.5px;
  white-space: nowrap;
}

.settings-layout {
  display: grid;
  grid-template-columns: 188px minmax(0, 1fr);
  gap: 28px;
  align-items: start;
}

.settings-nav {
  position: sticky;
  top: 92px;
  display: grid;
  gap: 4px;
  padding: 4px 0;
}

.settings-nav button {
  padding: 9px 0;
  border: 0;
  background: none;
  text-align: left;
  font-size: 12px;
  font-weight: 600;
  color: var(--oj-text-sub);
  cursor: pointer;
  transition: color var(--oj-duration) var(--oj-ease);
}

.settings-nav button:hover {
  color: var(--oj-text);
}

.settings-nav button.active {
  color: var(--oj-primary);
  background: none;
}

.settings-main {
  display: grid;
  grid-template-columns: minmax(0, 1fr);
  gap: 20px;
  min-width: 0;
}

.settings-panel {
  min-width: 0;
  padding: 22px;
  background: var(--oj-card);
  border: 1px solid var(--oj-border);
  border-radius: 4px;
  scroll-margin-top: 92px;
}

.panel-quiet {
  background: var(--oj-gray-50);
}

.panel-head {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 16px;
  margin-bottom: 22px;
}

.panel-head > div {
  min-width: 0;
}

.panel-head h2 {
  margin: 6px 0 0;
  color: var(--oj-text);
  font-family: var(--oj-font-display);
  font-size: 22px;
  font-weight: 800;
  letter-spacing: -0.02em;
}

.panel-note {
  padding-top: 4px;
  color: var(--oj-text-sub);
  font-family: var(--oj-font-mono);
  font-size: 10px;
  text-align: right;
}

.field {
  display: grid;
  gap: 7px;
  margin-bottom: 16px;
}

.field-label {
  display: flex;
  align-items: baseline;
  justify-content: space-between;
  gap: 8px;
  color: var(--oj-text);
  font-size: 12px;
  font-weight: 700;
}

.field-label small {
  color: var(--oj-text-sub);
  font-size: 10px;
  font-weight: 500;
}

.field-grid {
  display: grid;
  grid-template-columns: minmax(0, 1fr) minmax(0, 1fr);
  gap: 0 16px;
}

.field-grid .field {
  min-width: 0;
}

.panel-actions {
  display: flex;
  align-items: center;
  gap: 12px;
  flex-wrap: wrap;
  margin-top: 6px;
}

.action-hint {
  color: var(--oj-text-sub);
  font-size: 11px;
  line-height: 1.6;
}

.security-note {
  display: flex;
  align-items: flex-start;
  gap: 9px;
  padding: 11px 12px;
  margin: 2px 0 16px;
  color: var(--oj-warning);
  background: var(--oj-warning-soft);
  border: 1px solid var(--oj-warning-border);
  border-radius: var(--oj-radius-md);
  font-size: 11px;
  line-height: 1.6;
}

.note-mark {
  display: grid;
  flex: none;
  width: 16px;
  height: 16px;
  place-items: center;
  border: 1px solid currentColor;
  border-radius: 50%;
  font-family: var(--oj-font-mono);
  font-size: 10px;
  font-weight: 800;
}

.session-row {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
  padding-top: 18px;
  margin-top: 20px;
  border-top: 1px solid var(--oj-border);
}

.session-title {
  margin: 0;
  color: var(--oj-text);
  font-size: 12px;
  font-weight: 800;
}

.session-sub {
  margin: 4px 0 0;
  color: var(--oj-text-sub);
  font-size: 11px;
  line-height: 1.6;
}

.pref-grid {
  display: grid;
  grid-template-columns: minmax(0, 1fr) minmax(0, 1fr);
  gap: 0 18px;
}

.full-select {
  width: 100%;
}

.pref-value {
  color: var(--oj-primary);
  font-size: 11px;
  font-weight: 700;
}

.range-row {
  display: flex;
  align-items: center;
  gap: 14px;
}

.range-row :deep(.el-slider) {
  flex: 1;
}

.range-scale {
  flex: none;
  color: var(--oj-text-sub);
  font-size: 10px;
}

.switch-list {
  display: flex;
  align-items: center;
  gap: 22px;
  flex-wrap: wrap;
  min-height: 32px;
}

.switch-row {
  display: inline-flex;
  align-items: center;
  gap: 12px;
  color: var(--oj-text-sub);
  font-size: 12px;
}

.editor-preview {
  overflow: hidden;
  margin-top: 6px;
  border: 1px solid var(--oj-code-border);
  border-radius: var(--oj-radius-md);
  background: var(--oj-code-bg);
}

.preview-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  padding: 10px 14px;
  color: var(--oj-code-text-sub);
  border-bottom: 1px solid var(--oj-code-border);
  font-size: 10px;
}

.preview-code {
  min-height: 150px;
  padding: 16px 18px;
  margin: 0;
  overflow: auto;
  color: var(--oj-code-text);
  font-family: var(--oj-font-mono);
  line-height: 1.75;
  white-space: pre;
}

.preview-code.wrap {
  white-space: pre-wrap;
  word-break: break-word;
}

.local-row {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 22px;
}

.local-copy {
  max-width: 660px;
  margin: 0;
  color: var(--oj-text-sub);
  font-size: 12px;
  line-height: 1.7;
}

.local-actions {
  display: flex;
  flex: none;
  gap: 8px;
  flex-wrap: wrap;
}

.visually-hidden {
  position: absolute;
  width: 1px;
  height: 1px;
  padding: 0;
  overflow: hidden;
  clip: rect(0, 0, 0, 0);
  white-space: nowrap;
  border: 0;
}

@media (max-width: 900px) {
  .settings-page {
    padding: 36px 20px 64px;
  }

  .settings-head {
    align-items: flex-start;
    flex-direction: column;
    gap: 12px;
  }

  .settings-layout {
    grid-template-columns: 1fr;
    gap: 18px;
  }

  .settings-nav {
    position: static;
    display: flex;
    flex-wrap: wrap;
    gap: 6px;
    padding: 0;
  }

  .settings-nav button {
    border: 1px solid var(--oj-border);
    border-left: 1px solid var(--oj-border);
    border-radius: var(--oj-radius-pill);
    background: var(--oj-card);
    padding: 7px 10px;
  }

  .settings-nav button.active {
    border-color: var(--oj-primary);
  }

  .identity-strip {
    flex-wrap: wrap;
  }

  .identity-state {
    width: 100%;
    margin-left: 74px;
  }

  .settings-panel {
    padding: 22px 18px;
    scroll-margin-top: 80px;
  }

  .panel-head {
    flex-direction: column;
    gap: 6px;
  }

  .panel-note {
    padding: 0;
    text-align: left;
  }

  .field-grid,
  .pref-grid {
    grid-template-columns: 1fr;
    gap: 0;
  }

  .session-row,
  .local-row {
    align-items: flex-start;
    flex-direction: column;
  }

  .local-actions {
    flex: none;
  }
}

@media (max-width: 560px) {
  .settings-head h1 {
    font-size: 30px;
  }

  .identity-strip {
    gap: 12px;
    padding: 15px;
  }

  .avatar-button {
    width: 48px;
    height: 48px;
  }

  .identity-state {
    margin-left: 60px;
  }

  .settings-panel {
    padding: 18px 14px;
  }

  .panel-actions :deep(.el-button) {
    width: 100%;
  }

  .preview-head {
    align-items: flex-start;
    flex-direction: column;
  }
}
</style>
