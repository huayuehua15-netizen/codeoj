import { http } from './http'

/** 排行榜接口（论文表 4-7：GET /api/leaderboard 公开） */
export function getLeaderboard(params: { page: number; size: number }) {
  return http.get<never, { total: number; items: { userId: number; username?: string; acCount: number; rank: number }[] }>(
    '/api/leaderboard',
    { params },
  )
}
