import { http } from './http'

/** 排行榜接口（论文表 4-7：GET /api/leaderboard 公开） */
export interface LeaderboardItem {
  userId: number
  username?: string
  acCount: number
  rank: number
  /** 总提交次数 */
  totalSubmit?: number
  /** AC 提交次数 */
  acceptedSubmit?: number
  /** 通过率百分比（0-100，一位小数） */
  acceptRate?: number
  /** 最后一次提交时间 */
  lastSubmitAt?: string
}

export interface LeaderboardSummary {
  solverCount: number
  maxAcCount: number
  totalSubmit: number
  overallAcceptRate: number
}

export function getLeaderboard(params: { page: number; size: number }) {
  return http.get<never, { total: number; items: LeaderboardItem[]; summary?: LeaderboardSummary }>(
    '/api/leaderboard',
    { params },
  )
}
