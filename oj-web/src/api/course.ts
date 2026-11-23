import { http } from './http'

/** 课程班级接口（docs/API.md §6.2 / §6.3） */

export interface Course {
  id: number
  name: string
  description?: string | null
  teacherId?: number | null
  teacherName?: string | null
  joinCode?: string | null
  deadline?: string | null
  memberCount: number
  problemCount: number
  createdAt?: string
  updatedAt?: string
}

export interface CourseForm {
  name: string
  description?: string
  problemIds?: number[]
  joinCode?: string
  deadline?: string | null
}

export interface CourseProblem {
  id: number
  courseId: number
  problemId: number
  sortOrder: number
}

export interface CourseCompletionItem {
  problemId: number
  title: string
  memberCount: number
  submittedUsers: number
  acceptedUsers: number
  onTimeAcceptedUsers: number
}

export type CourseProblemStatus = 'ac' | 'attempted' | 'none'

export interface StudentCourse {
  id: number
  name: string
  description?: string | null
  teacherName?: string | null
  deadline?: string | null
  problemCount: number
  memberCount: number
  solvedCount: number
  onTimeSolvedCount: number
  submitCount: number
  joinedAt?: string | null
  lastSubmitAt?: string | null
}

export interface CourseProblemProgress {
  problemId: number
  title: string
  difficulty: number | null
  tags: string[]
  sortOrder: number
  status: CourseProblemStatus
  firstAcAt?: string | null
  attempts: number
  lastAt?: string | null
  onTime?: boolean | null
}

export interface CourseDetail extends StudentCourse {
  problems: CourseProblemProgress[]
}

export interface CourseMemberStatus {
  problemId: number
  status: CourseProblemStatus
  firstAcAt?: string | null
  attempts: number
  lastAt?: string | null
  onTime?: boolean | null
}

export interface CourseMember {
  userId: number
  username: string
  avatar?: string | null
  joinedAt?: string | null
  solvedCount: number
  onTimeSolvedCount: number
  submitCount: number
  lastSubmitAt?: string | null
  statuses: CourseMemberStatus[]
}

export interface CourseRoster {
  course: Course
  problems: { problemId: number; title: string; difficulty: number | null; sortOrder: number }[]
  members: CourseMember[]
}

/* ------------------------------------------------------------------ 管理端 */

export function createCourse(data: CourseForm) {
  return http.post<never, number>('/api/admin/courses', data)
}

export function updateCourse(id: number, data: CourseForm) {
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
  return http.get<never, CourseCompletionItem[]>(`/api/admin/courses/${id}/completion`)
}

export function getCourseRoster(id: number | string) {
  return http.get<never, CourseRoster>(`/api/admin/courses/${id}/members`)
}

export function removeCourseMember(courseId: number | string, userId: number) {
  return http.delete(`/api/admin/courses/${courseId}/members/${userId}`)
}

export function rotateCourseJoinCode(id: number | string) {
  return http.post<never, { joinCode: string }>(`/api/admin/courses/${id}/join-code`)
}

/* ------------------------------------------------------------------ 学生端 */

export function listMyCourses() {
  return http.get<never, StudentCourse[]>('/api/courses')
}

export function joinCourse(code: string) {
  return http.post<never, StudentCourse>('/api/courses/join', { code })
}

export function getCourseDetail(id: number | string) {
  return http.get<never, CourseDetail>(`/api/courses/${id}`)
}

export function leaveCourse(id: number | string) {
  return http.post(`/api/courses/${id}/leave`)
}
