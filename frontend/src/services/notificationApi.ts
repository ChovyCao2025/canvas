import type { R } from '../types'
import http from './api'

export interface UserNotification {
  notificationId: string
  type: 'TASK_SUCCEEDED' | 'TASK_FAILED' | string
  category: 'TASK' | 'APPROVAL' | 'ALERT' | 'CHANGE' | string
  severity: 'SUCCESS' | 'INFO' | 'WARNING' | 'ERROR' | string
  status: 'UNREAD' | 'READ' | 'ARCHIVED' | string
  title: string
  content?: string
  targetUrl?: string
  actionLabel?: string
  actionUrl?: string
  taskId?: string
  bizType?: string
  bizId?: string
  dedupKey?: string
  payloadJson?: string
  readAt?: string
  archivedAt?: string
  deliveredAt?: string
  createdAt?: string
}

export interface NotificationRealtimePayload {
  eventType: 'SYNC' | 'NOTIFICATION_CREATED' | 'NOTIFICATION_UPDATED' | 'PONG' | string
  notification?: UserNotification | null
  notifications?: UserNotification[]
  unreadCount?: number
  serverTime?: string
}

export interface NotificationWebSocketTicket {
  ticket: string
  expiresInSeconds: number
}

export const notificationApi = {
  list: (params?: {
    unreadOnly?: boolean
    category?: string
    archived?: boolean
    page?: number
    size?: number
  }) =>
    http.get<R<UserNotification[]>, R<UserNotification[]>>('/canvas/notifications', { params }),
  unreadCount: () =>
    http.get<R<{ count: number }>, R<{ count: number }>>('/canvas/notifications/unread-count'),
  markRead: (notificationId: string) =>
    http.put<R<void>, R<void>>(`/canvas/notifications/${notificationId}/read`),
  markAllRead: () =>
    http.put<R<void>, R<void>>('/canvas/notifications/read-all'),
  archive: (notificationId: string) =>
    http.put<R<void>, R<void>>(`/canvas/notifications/${notificationId}/archive`),
  createWsTicket: () =>
    http.post<R<NotificationWebSocketTicket>, R<NotificationWebSocketTicket>>('/canvas/notifications/ws-ticket'),
}
