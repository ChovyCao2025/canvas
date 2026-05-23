import { describe, expect, it } from 'vitest'
import {
  getNotificationActionLabel,
  getNotificationCategoryLabel,
  getNotificationStatusColor,
  shouldShowUnreadBadge,
} from './notificationPresentation'

describe('notificationPresentation', () => {
  it('uses success color for successful task notifications', () => {
    expect(getNotificationStatusColor('TASK_SUCCEEDED')).toBe('success')
  })

  it('uses error color for failed task notifications', () => {
    expect(getNotificationStatusColor('TASK_FAILED')).toBe('error')
  })

  it('shows unread badge only for positive unread count', () => {
    expect(shouldShowUnreadBadge(0)).toBe(false)
    expect(shouldShowUnreadBadge(3)).toBe(true)
  })

  it('maps category labels for message center tabs', () => {
    expect(getNotificationCategoryLabel('TASK')).toBe('任务')
    expect(getNotificationCategoryLabel('APPROVAL')).toBe('审批')
    expect(getNotificationCategoryLabel('ALERT')).toBe('告警')
    expect(getNotificationCategoryLabel('CHANGE')).toBe('变更')
  })

  it('prefers action label and falls back for archived notifications', () => {
    expect(getNotificationActionLabel({
      notificationId: 'ntf_1',
      type: 'TASK_SUCCEEDED',
      category: 'TASK',
      severity: 'SUCCESS',
      status: 'UNREAD',
      title: 'done',
      actionLabel: '查看结果',
    })).toBe('查看结果')
    expect(getNotificationActionLabel({
      notificationId: 'ntf_2',
      type: 'TASK_FAILED',
      category: 'TASK',
      severity: 'ERROR',
      status: 'ARCHIVED',
      title: 'archived',
      archivedAt: '2026-05-23T10:00:00',
    })).toBe('已归档')
  })
})
