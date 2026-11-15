import { http } from './http'

/** 公告接口（教学场景） */
export interface AnnouncementItem {
  id: number
  title: string
  content: string
  createdAt: string
}

export function listAnnouncements() {
  return http.get<never, AnnouncementItem[]>('/api/announcements')
}

export function publishAnnouncement(data: { title: string; content: string }) {
  return http.post<never, number>('/api/admin/announcements', data)
}

export function deleteAnnouncement(id: number) {
  return http.delete(`/api/admin/announcements/${id}`)
}
