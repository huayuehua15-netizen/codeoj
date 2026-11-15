import { http } from './http'

/** 提交接口（论文表 4-7） */
export interface SubmissionDetail {
  id: number
  userId: number
  problemId: number
  problemTitle?: string
  language: string
  code?: string
  status: number
  statusDesc?: string
  timeUsedMs?: number
  memoryUsedKb?: number
  errorInfo?: string
  judgedAt?: string
  createdAt?: string
  cases?: { caseIndex: number; status: number; statusDesc: string; timeUsedMs?: number; memoryUsedKb?: number }[]
}

export function submitCode(data: { problemId: number; language: string; code: string }) {
  return http.post<never, { submissionId: number; status: number }>('/api/submissions', data)
}

export function getSubmission(id: number | string) {
  return http.get<never, SubmissionDetail>(`/api/submissions/${id}`)
}

export function pageSubmissions(params: {
  problemId?: number
  status?: number
  begin?: string
  end?: string
  page: number
  size: number
}) {
  return http.get<never, { records: SubmissionDetail[]; total: number }>('/api/submissions', { params })
}

export function getStatistics() {
  return http.get<never, { byDifficulty: { difficulty: number; difficultyLabel: string; submit: number; accepted: number }[]; totalSubmit: number; totalAccepted: number }>(
    '/api/submissions/statistics',
  )
}
