import { describe, expect, it } from 'vitest'
import { getNotificationStatusColor, shouldShowUnreadBadge } from './notificationPresentation'

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
})
