export function subscriptionStatusLabel(status: string) {
  if (status === 'ACTIVE') return '启用'
  if (status === 'PAUSED') return '暂停'
  if (status === 'DISABLED') return '禁用'
  return status || '-'
}

export function subscriptionStatusColor(status: string) {
  if (status === 'ACTIVE') return 'green'
  if (status === 'PAUSED') return 'orange'
  if (status === 'DISABLED') return 'default'
  return 'default'
}

export function deliveryStatusLabel(status: string) {
  if (status === 'PENDING') return '待发送'
  if (status === 'SUCCESS') return '成功'
  if (status === 'FAILED') return '失败'
  if (status === 'RETRYING') return '重试中'
  if (status === 'DEAD') return '已终止'
  return status || '-'
}

export function deliveryStatusColor(status: string) {
  if (status === 'SUCCESS') return 'green'
  if (status === 'RETRYING') return 'orange'
  if (status === 'FAILED') return 'red'
  if (status === 'DEAD') return 'red'
  if (status === 'PENDING') return 'blue'
  return 'default'
}

export function maskWebhookSecret(secret: string) {
  if (!secret) return ''
  if (secret.length <= 8) return `${secret.slice(0, 2)}****`
  return `${secret.slice(0, 8)}****`
}
