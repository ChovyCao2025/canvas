import { describe, expect, it } from 'vitest'
import {
  approvalStatusColor,
  approvalStatusLabel,
  canDecideApprovalTask,
  sortApprovalTasks,
} from './approvalPresentation'
import type { ApprovalTaskView } from '../../services/api'

describe('approvalPresentation', () => {
  it('maps approval statuses to labels and colors', () => {
    expect(approvalStatusLabel('PENDING')).toBe('待审批')
    expect(approvalStatusLabel('APPROVED')).toBe('已通过')
    expect(approvalStatusLabel('REJECTED')).toBe('已拒绝')
    expect(approvalStatusColor('CANCELLED')).toBe('default')
  })

  it('allows decisions only for pending tasks', () => {
    expect(canDecideApprovalTask(task({ status: 'PENDING' }))).toBe(true)
    expect(canDecideApprovalTask(task({ status: 'APPROVED' }))).toBe(false)
  })

  it('sorts pending tasks by due time before id', () => {
    const rows = [
      task({ id: 3, dueAt: '2026-06-06T14:00:00' }),
      task({ id: 1, dueAt: '2026-06-06T13:00:00' }),
      task({ id: 2, dueAt: null }),
    ]

    expect(sortApprovalTasks(rows).map(row => row.id)).toEqual([1, 3, 2])
  })

  function task(overrides: Partial<ApprovalTaskView>): ApprovalTaskView {
    return {
      id: 1,
      tenantId: 7,
      instanceId: 101,
      stepNo: 1,
      approver: 'tenant_admin',
      status: 'PENDING',
      dueAt: '2026-06-06T13:00:00',
      actedAt: null,
      actionComment: null,
      ...overrides,
    }
  }
})
