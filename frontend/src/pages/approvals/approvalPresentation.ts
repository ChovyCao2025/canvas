import type { ApprovalStatus, ApprovalTaskView } from '../../services/api'

export function approvalStatusLabel(status?: ApprovalStatus | null): string {
  switch ((status ?? '').toUpperCase()) {
    case 'PENDING':
      return '待审批'
    case 'APPROVED':
      return '已通过'
    case 'REJECTED':
      return '已拒绝'
    case 'CANCELLED':
      return '已取消'
    case 'EXPIRED':
      return '已超时'
    default:
      return status || '-'
  }
}

export function approvalStatusColor(status?: ApprovalStatus | null): string {
  switch ((status ?? '').toUpperCase()) {
    case 'PENDING':
      return 'gold'
    case 'APPROVED':
      return 'green'
    case 'REJECTED':
      return 'red'
    default:
      return 'default'
  }
}

export function canDecideApprovalTask(task: ApprovalTaskView): boolean {
  return task.status?.toUpperCase() === 'PENDING' && typeof task.id === 'number'
}

export function sortApprovalTasks(tasks: ApprovalTaskView[]): ApprovalTaskView[] {
  return [...tasks].sort((a, b) => {
    const aDue = a.dueAt ? Date.parse(a.dueAt) : Number.POSITIVE_INFINITY
    const bDue = b.dueAt ? Date.parse(b.dueAt) : Number.POSITIVE_INFINITY
    if (aDue !== bDue) return aDue - bDue
    return (a.id ?? Number.MAX_SAFE_INTEGER) - (b.id ?? Number.MAX_SAFE_INTEGER)
  })
}
