import { defineStore } from 'pinia'
import { ref, computed } from 'vue'
import { getUserInfo } from '@/api/auth'

/**
 * 用户状态（论文 4.3 前端用户模块）：token 与用户信息本地持久化。
 */
export const useUserStore = defineStore('user', () => {
  const token = ref<string>(localStorage.getItem('oj_token') || '')
  const userId = ref<number>(Number(localStorage.getItem('oj_user_id') || 0))
  const username = ref<string>(localStorage.getItem('oj_username') || '')
  const nickname = ref<string>(localStorage.getItem('oj_nickname') || '')
  const role = ref<number>(Number(localStorage.getItem('oj_role') || 0))

  const isLoggedIn = computed(() => !!token.value)
  const isAdmin = computed(() => role.value === 1)

  function setLogin(data: { token: string; userId: number; username: string; nickname: string; role: number }) {
    token.value = data.token
    userId.value = data.userId
    username.value = data.username
    nickname.value = data.nickname
    role.value = data.role
    localStorage.setItem('oj_token', data.token)
    localStorage.setItem('oj_user_id', String(data.userId))
    localStorage.setItem('oj_username', data.username)
    localStorage.setItem('oj_nickname', data.nickname)
    localStorage.setItem('oj_role', String(data.role))
  }

  async function refreshUserInfo() {
    const info = await getUserInfo()
    setLogin({
      token: token.value,
      userId: info.userId,
      username: info.username,
      nickname: info.nickname,
      role: info.role,
    })
  }

  function logout() {
    token.value = ''
    userId.value = 0
    username.value = ''
    nickname.value = ''
    role.value = 0
    localStorage.clear()
  }

  return { token, userId, username, nickname, role, isLoggedIn, isAdmin, setLogin, refreshUserInfo, logout }
})
