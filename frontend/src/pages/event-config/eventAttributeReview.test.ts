import { describe, expect, it } from 'vitest'
import { statusColor, statusLabel } from './eventAttributeReview'

describe('event attribute review presentation', () => {
  it('labels review statuses in Chinese', () => {
    expect(statusLabel('PENDING_REVIEW')).toBe('待审核')
    expect(statusLabel('APPROVED')).toBe('已通过')
    expect(statusLabel('REJECTED')).toBe('已拒绝')
  })

  it('uses warning color for pending rows', () => {
    expect(statusColor('PENDING_REVIEW')).toBe('orange')
    expect(statusColor('APPROVED')).toBe('green')
    expect(statusColor('REJECTED')).toBe('red')
  })
})
