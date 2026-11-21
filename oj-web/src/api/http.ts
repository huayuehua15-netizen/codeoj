import axios from 'axios'
import { ElMessage } from 'element-plus'
import { useUserStore } from '@/stores/user'
import { disconnectWs } from '@/api/ws'
import router from '@/router'

/**
 * 把 axios 原生错误转成用户能读懂的中文提示。
 * 原生 message 是英文技术串（"Network Error"/"timeout of 15000ms exceeded"），
 * 直接弹给用户既吓人又没信息量。
 */
function networkErrorMessage(error: unknown): string {
  if (error instanceof axios.AxiosError) {
    if (error.code === 'ECONNABORTED' || /timeout/i.test(error.message)) {
      return '请求超时，请检查网络后重试'
    }
    if (error.response) {
      // 后端非 2xx：业务码走不到这里，只剩 HTTP 语义
      if (error.response.status === 404) return '资源不存在'
      if (error.response.status >= 500) return '服务暂不可用，请稍后重试'
    }
    if (!error.response) return '网络连接失败，请检查网络'
  }
  return '网络错误，请稍后重试'
}

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
  // token 已失效：显式断开推送连接，不让 STOMP 层带着过期凭证继续重连
  disconnectWs()
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
    ElMessage.error(networkErrorMessage(error))
    return Promise.reject(error)
  },
)
