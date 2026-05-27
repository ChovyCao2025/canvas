/**
 * 服务职责：通知列表、未读数、归档和 WebSocket ticket API 封装。
 *
 * 维护说明：实时消息体类型与 NotificationContext 的合并逻辑保持一致。
 */
import type { R } from '../types'
import http from './api'

/** 用户通知列表项，覆盖站内信、任务提醒、告警和变更提醒。 */
export interface UserNotification {
  /** 通知唯一 ID，读/归档操作依赖它定位单条记录。 */
  notificationId: string

  /** 通知事件类型，前端已知类型之外仍允许后端扩展。 */
  type: 'TASK_SUCCEEDED' | 'TASK_FAILED' | string

  /** 通知所属业务分类，用于列表筛选。 */
  category: 'TASK' | 'APPROVAL' | 'ALERT' | 'CHANGE' | string

  /** 严重程度，用于颜色和图标展示。 */
  severity: 'SUCCESS' | 'INFO' | 'WARNING' | 'ERROR' | string

  /** 阅读/归档状态。 */
  status: 'UNREAD' | 'READ' | 'ARCHIVED' | string

  /** 通知标题，列表主文案。 */
  title: string

  /** 通知正文，可为空。 */
  content?: string

  /** 点击通知主体跳转的目标地址。 */
  targetUrl?: string

  /** 自定义操作按钮文案。 */
  actionLabel?: string

  /** 自定义操作按钮跳转地址。 */
  actionUrl?: string

  /** 关联异步任务 ID，任务类通知可跳转任务详情或业务页。 */
  taskId?: string

  /** 关联业务类型。 */
  bizType?: string

  /** 关联业务对象 ID。 */
  bizId?: string

  /** 去重键，后端用于合并重复事件。 */
  dedupKey?: string

  /** 扩展载荷 JSON 字符串，前端通常只透传或调试展示。 */
  payloadJson?: string

  /** 阅读时间。 */
  readAt?: string

  /** 归档时间。 */
  archivedAt?: string

  /** 送达时间。 */
  deliveredAt?: string

  /** 创建时间。 */
  createdAt?: string
}

/** WebSocket 推送的通知增量事件。 */
export interface NotificationRealtimePayload {
  /** 推送事件类型：同步全量、创建、更新、心跳等。 */
  eventType: 'SYNC' | 'NOTIFICATION_CREATED' | 'NOTIFICATION_UPDATED' | 'PONG' | string

  /** 单条通知增量，创建/更新事件通常使用。 */
  notification?: UserNotification | null

  /** 全量同步通知列表，SYNC 事件使用。 */
  notifications?: UserNotification[]

  /** 服务端计算的最新未读数，优先用于覆盖本地估算。 */
  unreadCount?: number

  /** 服务端时间，便于排查客户端与服务端时钟差异。 */
  serverTime?: string
}

/** WebSocket 临时 ticket，避免直接把长期 JWT 放到连接 URL。 */
export interface NotificationWebSocketTicket {
  /** 一次性或短期有效的连接凭证。 */
  ticket: string

  /** ticket 剩余有效秒数，前端可据此决定是否提前刷新。 */
  expiresInSeconds: number
}

/** 通知中心 HTTP 接口集合，实时连接所需 ticket 也从这里获取。 */
export const notificationApi = {
  /** 查询通知列表，支持未读、分类、归档和分页筛选。 */
  list: (params?: {
    unreadOnly?: boolean
    category?: string
    archived?: boolean
    page?: number
    size?: number
  }) =>
    http.get<R<UserNotification[]>, R<UserNotification[]>>('/canvas/notifications', { params }),

  /** 查询当前用户未读通知数量。 */
  unreadCount: () =>
    http.get<R<{ count: number }>, R<{ count: number }>>('/canvas/notifications/unread-count'),

  /** 将单条通知标记为已读。 */
  markRead: (notificationId: string) =>
    http.put<R<void>, R<void>>(`/canvas/notifications/${notificationId}/read`),

  /** 将当前用户所有通知标记为已读。 */
  markAllRead: () =>
    http.put<R<void>, R<void>>('/canvas/notifications/read-all'),

  /** 归档单条通知，归档后默认不在主列表展示。 */
  archive: (notificationId: string) =>
    http.put<R<void>, R<void>>(`/canvas/notifications/${notificationId}/archive`),

  /** 创建通知 WebSocket 连接 ticket。 */
  createWsTicket: () =>
    http.post<R<NotificationWebSocketTicket>, R<NotificationWebSocketTicket>>('/canvas/notifications/ws-ticket'),
}
