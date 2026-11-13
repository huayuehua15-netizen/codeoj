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
 */
export function connectWs() {
  if (client && client.active) {
    return
  }
  const userStore = useUserStore()
  client = new Client({
    webSocketFactory: () => new SockJS('/ws') as WebSocket,
    reconnectDelay: 3000,
    onConnect: () => {
      // 用户级定向订阅（论文 5.5）
      client?.subscribe(`/user/queue/judge`, (message) => {
        const payload = JSON.parse(message.body) as JudgePushPayload
        handlers.forEach((h) => h(payload))
      })
    },
  })
  client.activate()
}

export function disconnectWs() {
  client?.deactivate()
  client = null
  handlers.clear()
}

export function onJudgePush(handler: Handler) {
  handlers.add(handler)
  return () => handlers.delete(handler)
}
