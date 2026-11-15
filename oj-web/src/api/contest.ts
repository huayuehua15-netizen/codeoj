import { http } from './http'

/** 竞赛接口（论文 7.2 展望落地：竞赛模式） */
export interface ContestVO {
  id: number
  title: string
  description?: string
  startTime: string
  endTime: string
  /** 0=未开始 1=进行中 2=已结束 */
  status: number
  problemCount: number
}

export interface ContestDetail extends Omit<ContestVO, 'problemCount'> {
  problems: { label: string; problemId: number; title: string; difficulty: number }[]
}

export interface ContestRankItem {
  rank: number
  userId: number
  username?: string
  solved: number
  penalty: number
  detail: string
}

export function listContests() {
  return http.get<never, ContestVO[]>('/api/contests')
}

export function getContest(id: number | string) {
  return http.get<never, ContestDetail>(`/api/contests/${id}`)
}

export function getContestRanking(id: number | string) {
  return http.get<never, ContestRankItem[]>(`/api/contests/${id}/ranking`)
}

export function createContest(data: {
  title: string
  description?: string
  startTime: string
  endTime: string
  problemIds: number[]
}) {
  return http.post<never, number>('/api/admin/contests', data)
}

export function updateContest(id: number, data: {
  title: string
  description?: string
  startTime: string
  endTime: string
  problemIds: number[]
}) {
  return http.put(`/api/admin/contests/${id}`, data)
}

export function deleteContest(id: number) {
  return http.delete(`/api/admin/contests/${id}`)
}
