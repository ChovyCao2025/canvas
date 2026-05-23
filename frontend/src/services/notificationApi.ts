import type { R } from '../types'
import http from './api'

export interface UserNotification {
  notificationId: string
  type: 'TASK_SUCCEEDED' | 'TASK_FAILED' | string
  title: string
  content?: string
  targetUrl?: string
  taskId?: string
  readAt?: string
  createdAt?: string
}

export const notificationApi = {
  list: (params?: { unreadOnly?: boolean; page?: number; size?: number }) =>
    http.get<R<UserNotification[]>, R<UserNotification[]>>('/canvas/notifications', { params }),
  unreadCount: () =>
    http.get<R<{ count: number }>, R<{ count: number }>>('/canvas/notifications/unread-count'),
  markRead: (notificationId: string) =>
    http.put<R<void>, R<void>>(`/canvas/notifications/${notificationId}/read`),
  markAllRead: () =>
    http.put<R<void>, R<void>>('/canvas/notifications/read-all'),
}
