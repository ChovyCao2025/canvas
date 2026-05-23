export function getNotificationStatusColor(type: string) {
  if (type === 'TASK_SUCCEEDED') return 'success'
  if (type === 'TASK_FAILED') return 'error'
  return 'default'
}

export function shouldShowUnreadBadge(count: number) {
  return count > 0
}
