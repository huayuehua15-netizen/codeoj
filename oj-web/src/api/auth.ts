import { http } from './http'

export interface LoginVO {
  token: string
  userId: number
  username: string
  nickname: string
  role: number
}

/** 认证接口（论文表 4-7） */
export function register(data: { username: string; password: string; nickname?: string; email?: string }) {
  return http.post<never, LoginVO>('/api/auth/register', data)
}

export function login(data: { username: string; password: string }) {
  return http.post<never, LoginVO>('/api/auth/login', data)
}

export function logout() {
  return http.post('/api/auth/logout')
}

export function getUserInfo() {
  return http.get<never, { userId: number; username: string; nickname: string; role: number }>('/api/auth/info')
}
