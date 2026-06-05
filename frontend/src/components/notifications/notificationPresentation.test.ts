/**
 * 测试职责：验证通知中心展示文案、颜色和未读角标规则。
 *
 * 维护说明：新增通知分类、严重等级或归档状态时，应补充对应展示断言。
 */
import { describe, expect, it } from 'vitest'
import { findAccessibilityIssues } from '../../test/accessibilityChecks'
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

  it('keeps notification controls named and keyboard reachable', () => {
    expect(findAccessibilityIssues([
      { id: 'notification-bell', role: 'button', name: '打开消息中心', focusable: true },
      { id: 'notification-filter', role: 'tablist', name: '通知分类筛选', focusable: true },
      { id: 'notification-row', role: 'button', name: '查看通知 任务完成', focusable: true },
      { id: 'notification-archive', role: 'button', name: '归档通知 任务完成', focusable: true },
    ])).toEqual([])
  })
})
