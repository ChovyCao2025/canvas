/**
 * 组件职责：通知实时同步工具，封装 WebSocket URL、重连退避、合并和排序逻辑。
 *
 * 维护说明：这些纯函数让 NotificationContext 只关注连接生命周期。
 */
import type { NotificationRealtimePayload, UserNotification } from '../../services/notificationApi'

/** 根据当前页面协议生成通知 WebSocket 地址，HTTPS 页面必须使用 WSS。 */
export function buildNotificationWebSocketUrl(ticket: string, locationLike: Pick<Location, 'protocol' | 'host'>) {
  const protocol = locationLike.protocol === 'https:' ? 'wss:' : 'ws:'
  return `${protocol}//${locationLike.host}/canvas/ws/notifications?ticket=${encodeURIComponent(ticket)}`
}

/** 指数退避重连间隔，最大 30 秒，避免断网时频繁打满服务端连接。 */
export function nextNotificationReconnectDelay(attempt: number) {
  const safeAttempt = Math.max(0, attempt)
  return Math.min(30000, 1000 * (2 ** Math.min(safeAttempt, 5)))
}

/**
 * 将实时推送合并到当前通知列表。
 *
 * SYNC 代表服务端给出全量快照，直接覆盖；增量事件则按 notificationId 去重后前置。
 */
export function mergeRealtimeNotifications(
  currentItems: UserNotification[],
  payload: NotificationRealtimePayload,
) {
  if (payload.eventType === 'SYNC') {
    return sortNotifications(payload.notifications ?? [])
  }
  if (!payload.notification) {
    return currentItems
  }
  const nextItems = [payload.notification, ...currentItems.filter(item => item.notificationId !== payload.notification?.notificationId)]
  return sortNotifications(nextItems)
}

/** 按创建时间倒序排序通知；缺失时间的旧数据排在最后。 */
export function sortNotifications(items: UserNotification[]) {
  return [...items].sort((left, right) => {
    const leftTime = left.createdAt ? new Date(left.createdAt).getTime() : 0
    const rightTime = right.createdAt ? new Date(right.createdAt).getTime() : 0
    return rightTime - leftTime
  })
}
