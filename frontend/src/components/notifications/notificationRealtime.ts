import type { NotificationRealtimePayload, UserNotification } from '../../services/notificationApi'

export function buildNotificationWebSocketUrl(ticket: string, locationLike: Pick<Location, 'protocol' | 'host'>) {
  const protocol = locationLike.protocol === 'https:' ? 'wss:' : 'ws:'
  return `${protocol}//${locationLike.host}/canvas/ws/notifications?ticket=${encodeURIComponent(ticket)}`
}

export function nextNotificationReconnectDelay(attempt: number) {
  const safeAttempt = Math.max(0, attempt)
  return Math.min(30000, 1000 * (2 ** Math.min(safeAttempt, 5)))
}

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

export function sortNotifications(items: UserNotification[]) {
  return [...items].sort((left, right) => {
    const leftTime = left.createdAt ? new Date(left.createdAt).getTime() : 0
    const rightTime = right.createdAt ? new Date(right.createdAt).getTime() : 0
    return rightTime - leftTime
  })
}
