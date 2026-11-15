import { http } from './http'

/** 题目收藏接口：toggle / 状态 / 我的收藏 */
export function toggleFavorite(problemId: number) {
  return http.post<never, boolean>(`/api/problems/${problemId}/favorite`)
}

export function isFavorited(problemId: number) {
  return http.get<never, boolean>(`/api/problems/${problemId}/favorite`)
}

export function myFavorites() {
  return http.get<never, { problemId: number; title: string; difficulty: number; favoritedAt: string }[]>('/api/favorites')
}
