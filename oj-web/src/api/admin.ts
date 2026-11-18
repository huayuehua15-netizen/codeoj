import { http } from './http'

/** 运维监控接口（论文表 4-7：GET /api/admin/queue；API.md v1.7：cachedAt → queriedAt） */
export function getQueueStatus() {
  return http.get<never, { backlog: number; judgedToday: number; judgeInstances: number; queriedAt?: string }>(
    '/api/admin/queue',
  )
}
