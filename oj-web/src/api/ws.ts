import { Client } from '@stomp/stompjs'
import SockJS from 'sockjs-client'
import { useUserStore } from '@/stores/user'

export interface JudgePushPayload {
  type: string
  data: {
    submissionId: number
    status?: number
    timeUsedMs?: number
    memoryUsedKb?: number
    passedCases?: number
    totalCases?: number
    errorInfo?: string
  }
}

type Handler = (payload: JudgePushPayload) => void

let client: Client | null = null
const handlers = new Set<Handler>()

/**
 * WebSocket 推送客户端（论文 4.5/5.5）：SockJS 建立连接并订阅 /user/queue/judge。
 * 消息类型：JUDGE_QUEUED / JUDGING / JUDGE_FINISHED。
 * 连接断开时自动重连（stompjs 内置心跳重连）。
 *
 * 鉴权：服务端在 STOMP CONNECT 帧上校验原生头 Authorization: Bearer <token>，
 * 由拦截器把 LoginUser 设为会话 Principal，convertAndSendToUser(userId) 才能匹配本会话。
 */
export function connectWs() {
  if (client && client.active) {
    return
  }
  const userStore = useUserStore()
  client = new Client({
    webSocketFactory: () => new SockJS('/ws') as WebSocket,
    // 每次建立/重建连接时刷新 STOMP 原生鉴权头，避免重连使用旧 token。
    beforeConnect: () => {
      if (!client) return
      if (!userStore.token) {
        // 已登出但连接还挂在重连循环里：主动停掉，避免带着空鉴权头无限重试
        client.deactivate()
        return
      }
      client.connectHeaders = { Authorization: `Bearer ${userStore.token}` }
    },
    reconnectDelay: 3000,
    onConnect: () => {
      // 用户级定向订阅（论文 5.5）
      client?.subscribe(`/user/queue/judge`, (message) => {
        let payload: JudgePushPayload
        try {
          payload = JSON.parse(message.body) as JudgePushPayload
        } catch {
          console.warn('[ws] 无法解析推送消息：', message.body)
          return
        }
        // 防御：无 data 或无 submissionId 的帧直接忽略，避免打断整轮分发
        if (!payload?.data || typeof payload.data.submissionId !== 'number') return
        // 单个 handler 抛错不影响其它订阅者
        handlers.forEach((h) => {
          try {
            h(payload)
          } catch (e) {
            console.error('[ws] 推送处理异常', e)
          }
        })
      })
    },
    // token 过期/被拉黑时服务端拒绝 CONNECT：此前无任何回调，表现为无限静默重连。
    // 记录日志并在鉴权失败时提示重新登录（HTTP 侧的 40100 拦截器会完成登出跳转）。
    onStompError: (frame) => {
      console.warn('[ws] STOMP 错误帧：', frame.headers['message'] ?? '(无消息)')
    },
    onWebSocketClose: (event) => {
      // 正常登出触发的 deactivate 也会走到这里，仅在异常关闭时留痕
      if (event && event.code !== 1000 && event.code !== 1005) {
        console.warn('[ws] 连接关闭 code=', event.code, '，将在 3s 后自动重连')
      }
    },
  })
  client.activate()
}

/**
 * 断开连接。注意：**不清空 handlers**——订阅者各自通过 onJudgePush 返回的
 * unsubscribe 释放（组件卸载时调用）；在此清空会让仍挂载的页面被静默摘除订阅。
 */
export function disconnectWs() {
  client?.deactivate()
  client = null
}

export function onJudgePush(handler: Handler) {
  handlers.add(handler)
  return () => {
    handlers.delete(handler)
  }
}
