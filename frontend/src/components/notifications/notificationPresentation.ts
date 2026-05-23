import type { UserNotification } from '../../services/notificationApi'

export function getNotificationStatusColor(type: string, severity?: string) {
  if (severity === 'SUCCESS' || type === 'TASK_SUCCEEDED') return 'success'
  if (severity === 'ERROR' || type === 'TASK_FAILED') return 'error'
  if (severity === 'WARNING') return 'warning'
  if (severity === 'INFO') return 'processing'
  return 'default'
}

export function shouldShowUnreadBadge(count: number) {
  return count > 0
}

export function getNotificationCategoryLabel(category: string) {
  if (category === 'TASK') return '任务'
  if (category === 'APPROVAL') return '审批'
  if (category === 'ALERT') return '告警'
  if (category === 'CHANGE') return '变更'
  return '通知'
}

export function getNotificationActionLabel(notification: UserNotification) {
  return notification.actionLabel || (notification.archivedAt ? '已归档' : '查看')
}
