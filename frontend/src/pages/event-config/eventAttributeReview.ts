export function statusLabel(status: string) {
  if (status === 'PENDING_REVIEW') return '待审核'
  if (status === 'APPROVED') return '已通过'
  if (status === 'REJECTED') return '已拒绝'
  return status
}

export function statusColor(status: string) {
  if (status === 'PENDING_REVIEW') return 'orange'
  if (status === 'APPROVED') return 'green'
  if (status === 'REJECTED') return 'red'
  return 'default'
}
