import axios from 'axios'
import { ElMessage } from 'element-plus'
import { useUserStore } from '@/stores/user'
import router from '@/router'

/**
 * HTTP 客户端（论文 4.5）：统一返回结构 {code, message, data}，
 * 请求头携带 Authorization: Bearer <token>。
 */
export const http = axios.create({
  baseURL: import.meta.env.VITE_API_BASE || '',
  timeout: 15000,
})

http.interceptors.request.use((config) => {
  const userStore = useUserStore()
  if (userStore.token) {
    config.headers.Authorization = `Bearer ${userStore.token}`
  }
  return config
})

/**
 * 40100 并发去重：页面同时发多个请求时（如个人中心一次拉 info + favorites），
 * 只登出/跳转/提示一次，避免多弹 toast 与多次 router.push。
 */
let sessionExpiredHandling = false

function handleSessionExpired(message?: string) {
  if (sessionExpiredHandling) return
  sessionExpiredHandling = true
  useUserStore().logout()
  const current = router.currentRoute.value
  ElMessage.error(message || '登录已过期，请重新登录')
  router
    .replace({
      name: 'login',
      // 保留回跳地址（登录页已实现 query.redirect 回跳）
      query: current.name === 'login' ? {} : { redirect: current.fullPath },
    })
    .finally(() => {
      // 留一个短窗口吸收同一批并发请求，之后恢复可再次处理新会话的过期
      setTimeout(() => {
        sessionExpiredHandling = false
      }, 1500)
    })
}

http.interceptors.response.use(
  (response) => {
    const body = response.data
    if (body && typeof body.code === 'number') {
      if (body.code === 0) {
        return body.data
      }
      if (body.code === 40100) {
        // 未登录或登录已过期
        handleSessionExpired(body.message)
        return Promise.reject(new Error(body.message))
      }
      ElMessage.error(body.message || '请求失败')
      return Promise.reject(new Error(body.message))
    }
    return body
  },
  (error) => {
    ElMessage.error(error.message || '网络错误')
    return Promise.reject(error)
  },
)
