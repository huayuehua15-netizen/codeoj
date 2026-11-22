import { http } from './http'

/** 题目接口（论文表 4-7） */
export interface Problem {
  id: number
  title: string
  tags?: string[]
  description?: string
  samples?: { input: string; output: string }[]
  difficulty: number
  timeLimitMs: number
  memoryLimitMb: number
  languages?: string[]
  /** JS 场景题（函数实现题）入口函数名；null/undefined = 标准 stdin/stdout 题（API.md v1.12） */
  entryFunction?: string | null
  submitCount: number
  acceptedCount: number
  enabled: boolean
}

export function pageProblems(params: { keyword?: string; difficulty?: number; tag?: string; page: number; size: number }) {
  return http.get<never, { records: Problem[]; total: number }>('/api/problems', { params })
}

export function getProblem(id: number | string) {
  return http.get<never, Problem>(`/api/problems/${id}`)
}

export function getProblemOverview() {
  return http.get<never, { totalProblems: number; tags: string[] }>('/api/problems/overview')
}

export function createProblem(data: Record<string, unknown>) {
  return http.post('/api/problems', data)
}

export function updateProblem(id: number, data: Record<string, unknown>) {
  return http.put(`/api/problems/${id}`, data)
}

export function deleteProblem(id: number) {
  return http.delete(`/api/problems/${id}`)
}

/** 用例接口（论文表 4-7：POST /api/problems/{id}/testcases） */
export function importTestCases(problemId: number | string, cases: { input: string; expectedOutput: string; score: number; isSample: boolean }[]) {
  return http.post(`/api/problems/${problemId}/testcases`, { cases })
}

export function listTestCases(problemId: number | string) {
  return http.get<never, { id: number; input: string; expectedOutput: string; score: number; isSample: boolean }[]>(
    `/api/problems/${problemId}/testcases`,
  )
}

export function deleteTestCase(problemId: number | string, caseId: number) {
  return http.delete(`/api/problems/${problemId}/testcases/${caseId}`)
}
