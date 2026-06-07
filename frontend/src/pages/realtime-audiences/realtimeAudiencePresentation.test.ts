import { describe, expect, it } from 'vitest'
import {
  formatOverlapPercent,
  formatSetOperation,
  formatSnapshotRow,
  realtimeStatusText,
} from './realtimeAudiencePresentation'

describe('realtimeAudiencePresentation', () => {
  it('formats realtime event statuses for operators', () => {
    expect(realtimeStatusText('UPDATED')).toBe('Updated')
    expect(realtimeStatusText('DUPLICATED')).toBe('Duplicated')
    expect(realtimeStatusText('BLOCKED')).toBe('Blocked')
    expect(realtimeStatusText('SKIPPED')).toBe('Skipped')
    expect(realtimeStatusText('UNKNOWN')).toBe('UNKNOWN')
  })

  it('formats overlap percentages with one decimal place', () => {
    expect(formatOverlapPercent(0)).toBe('0.0%')
    expect(formatOverlapPercent(33.333)).toBe('33.3%')
    expect(formatOverlapPercent(100)).toBe('100.0%')
  })

  it('formats guarded set operation results', () => {
    expect(formatSetOperation({
      status: 'BLOCKED',
      reason: 'SAFE_SIZE_LIMIT_EXCEEDED',
      resultSize: 120000,
      safeLimit: 50000,
    })).toBe('Blocked: SAFE_SIZE_LIMIT_EXCEEDED, result size 120000 exceeds safe limit 50000')

    expect(formatSetOperation({
      status: 'READY',
      resultSize: 42,
    })).toBe('Ready: result size 42')
  })

  it('formats snapshot rows for trend lists', () => {
    expect(formatSnapshotRow({
      id: 99,
      audienceId: 10,
      estimatedSize: 42,
      bitmapKey: 'audience:bitmap:10',
      snapshotSource: 'MANUAL',
      createdAt: '2026-06-03T00:00:00Z',
    })).toBe('#10 size 42 from MANUAL at 2026-06-03T00:00:00Z')
  })
})
