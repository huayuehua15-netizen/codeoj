import { defineStore } from 'pinia'
import { ref, computed } from 'vue'
import { getUserInfo } from '@/api/auth'

/** 本应用在 localStorage 中使用的键（登出时只清这些，避免误删同域其它数据） */
const KEY_TOKEN = 'oj_token'
const KEY_USER_ID = 'oj_user_id'
const KEY_USERNAME = 'oj_username'
const KEY_ROLE = 'oj_role'
const STORAGE_KEYS = [KEY_TOKEN, KEY_USER_ID, KEY_USERNAME, KEY_ROLE]

/**
 * 用户状态（论文 4.3 前端用户模块）：token 与用户信息本地持久化。
 */
export const useUserStore = defineStore('user', () => {
  const token = ref<string>(localStorage.getItem(KEY_TOKEN) || '')
  const userId = ref<number>(Number(localStorage.getItem(KEY_USER_ID) || 0))
  const username = ref<string>(localStorage.getItem(KEY_USERNAME) || '')
  const role = ref<number>(Number(localStorage.getItem(KEY_ROLE) || 0))

  const isLoggedIn = computed(() => !!token.value)
  const isAdmin = computed(() => role.value === 1)

  function setLogin(data: { token: string; userId: number; username: string; role: number }) {
    token.value = data.token
    userId.value = data.userId
    username.value = data.username
    role.value = data.role
    localStorage.setItem(KEY_TOKEN, data.token)
    localStorage.setItem(KEY_USER_ID, String(data.userId))
    localStorage.setItem(KEY_USERNAME, data.username)
    localStorage.setItem(KEY_ROLE, String(data.role))
  }

  async function refreshUserInfo() {
    const info = await getUserInfo()
    setLogin({
      token: token.value,
      userId: info.userId,
      username: info.username,
      role: info.role,
    })
  }

  function logout() {
    token.value = ''
    userId.value = 0
    username.value = ''
    role.value = 0
    // 只清本应用自己的键：localStorage.clear() 会连带抹掉同域下其它数据
    for (const key of STORAGE_KEYS) localStorage.removeItem(key)
  }

  return { token, userId, username, role, isLoggedIn, isAdmin, setLogin, refreshUserInfo, logout }
})
