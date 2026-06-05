import { describe, expect, it } from 'vitest'
import { connectorModeBadge, connectorWarning, formatDecisionRow, formatLimitRow } from './channelConnectorPresentation'

describe('channelConnectorPresentation', () => {
  it('formats connector mode badges and warnings', () => {
    expect(connectorModeBadge('REAL')).toEqual({ text: 'Real', color: 'green' })
    expect(connectorModeBadge('SANDBOX')).toEqual({ text: 'Sandbox', color: 'gold' })
    expect(connectorWarning({ channel: 'SMS', provider: 'ALIYUN', mode: 'DISABLED' }))
      .toBe('SMS/ALIYUN is disabled')
  })

  it('formats fallback decision rows', () => {
    expect(formatDecisionRow({
      originalChannel: 'PUSH',
      finalChannel: 'SMS',
      decisionReason: 'PRIMARY_THROTTLED',
      createdAt: '2026-06-03T00:00:00Z',
    })).toBe('PUSH -> SMS: PRIMARY_THROTTLED at 2026-06-03T00:00:00Z')
  })

  it('formats provider limits', () => {
    expect(formatLimitRow({
      channel: 'SMS',
      provider: 'ALIYUN',
      operation: 'SEND',
      perSecondLimit: 20,
      dailyLimit: null,
    })).toBe('SMS/ALIYUN SEND: 20/s, daily unlimited')
  })
})
