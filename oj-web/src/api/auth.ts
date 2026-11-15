import { http } from './http'

export interface LoginVO {
  token: string
  userId: number
  username: string
  email?: string
  role: number
  createdAt?: string
}

/** 认证接口（论文表 4-7） */
export function register(data: { username: string; password: string; email?: string }) {
  return http.post<never, LoginVO>('/api/auth/register', data)
}

export function login(data: { username: string; password: string }) {
  return http.post<never, LoginVO>('/api/auth/login', data)
}

export function logout() {
  return http.post('/api/auth/logout')
}

export function getUserInfo() {
  return http.get<never, LoginVO>('/api/auth/info')
}

/** 个人信息维护（论文表 3-1）：邮箱修改与密码修改 */
export function updateEmail(email: string) {
  return http.put('/api/auth/info', { email })
}

export function changePassword(oldPassword: string, newPassword: string) {
  return http.put('/api/auth/password', { oldPassword, newPassword })
}
