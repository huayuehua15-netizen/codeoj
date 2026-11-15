import { http } from './http'

/** 用户管理接口（管理端，教师） */
export interface AdminUserItem {
  id: number
  username: string
  email: string
  role: number
  /** 0=正常 1=封禁 */
  status: number
  createdAt: string
}

export function pageAdminUsers(params: { keyword?: string; page: number; size: number }) {
  return http.get<never, { records: AdminUserItem[]; total: number }>('/api/admin/users', { params })
}

export function changeUserRole(id: number, role: number) {
  return http.put(`/api/admin/users/${id}/role?role=${role}`)
}

export function changeUserStatus(id: number, status: number) {
  return http.put(`/api/admin/users/${id}/status?status=${status}`)
}

export function resetUserPassword(id: number) {
  return http.put<never, string>(`/api/admin/users/${id}/password`)
}
