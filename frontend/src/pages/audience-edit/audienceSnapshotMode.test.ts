/**
 * 测试职责：验证人群默认发送快照模式的历史空值兼容和中文展示。
 */
import { describe, expect, it } from 'vitest'
import { normalizeAudienceSnapshotMode, snapshotModeLabel } from './audienceSnapshotMode'

describe('audienceSnapshotMode', () => {
  it('defaults empty snapshot mode to static locked', () => {
    expect(normalizeAudienceSnapshotMode()).toBe('STATIC_LOCKED')
    expect(normalizeAudienceSnapshotMode(null)).toBe('STATIC_LOCKED')
    expect(normalizeAudienceSnapshotMode('')).toBe('STATIC_LOCKED')
  })

  it('keeps dynamic refresh mode', () => {
    expect(normalizeAudienceSnapshotMode('DYNAMIC_REFRESH')).toBe('DYNAMIC_REFRESH')
  })

  it('returns Chinese labels', () => {
    expect(snapshotModeLabel('STATIC_LOCKED')).toBe('发布时锁定')
    expect(snapshotModeLabel('DYNAMIC_REFRESH')).toBe('每次刷新')
    expect(snapshotModeLabel()).toBe('发布时锁定')
  })
})
