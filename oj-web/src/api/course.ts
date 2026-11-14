import { http } from './http'

/** 课程实验接口（docs/API.md §6.2，管理员） */
export interface Course {
  id: number
  name: string
  description?: string
  teacherId?: number
  createdAt?: string
  updatedAt?: string
}

export interface CourseProblem {
  id: number
  courseId: number
  problemId: number
  sortOrder: number
}

export interface CompletionItem {
  problemId: number
  title: string
  submittedUsers: number
  acceptedUsers: number
}

export function createCourse(data: { name: string; description?: string; problemIds?: number[] }) {
  return http.post<never, number>('/api/admin/courses', data)
}

export function updateCourse(id: number, data: { name: string; description?: string; problemIds?: number[] }) {
  return http.put(`/api/admin/courses/${id}`, data)
}

export function deleteCourse(id: number) {
  return http.delete(`/api/admin/courses/${id}`)
}

export function listCourses() {
  return http.get<never, Course[]>('/api/admin/courses')
}

export function getCourseProblems(id: number | string) {
  return http.get<never, CourseProblem[]>(`/api/admin/courses/${id}/problems`)
}

export function getCourseCompletion(id: number | string) {
  return http.get<never, CompletionItem[]>(`/api/admin/courses/${id}/completion`)
}
