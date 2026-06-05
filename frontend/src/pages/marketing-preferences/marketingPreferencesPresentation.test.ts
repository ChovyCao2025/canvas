import { describe, expect, it } from 'vitest'
import {
  channelReachabilityView,
  consentStatusView,
  formatPreferenceDateTime,
  formatPreferenceSummary,
  suppressionStateView,
} from './marketingPreferencesPresentation'

describe('marketingPreferencesPresentation', () => {
  it('maps consent and suppression states to stable labels', () => {
    expect(consentStatusView('OPT_IN')).toEqual({ text: '已同意', color: 'green' })
    expect(consentStatusView('OPT_OUT')).toEqual({ text: '已退订', color: 'red' })
    expect(suppressionStateView('ACTIVE')).toEqual({ text: '生效中', color: 'red' })
    expect(suppressionStateView('EXPIRED')).toEqual({ text: '已过期', color: 'default' })
  })

  it('formats channel reachability and summary text', () => {
    expect(channelReachabilityView({ enabled: true, reachable: true })).toEqual({ text: '可达', color: 'green' })
    expect(channelReachabilityView({ enabled: false, reachable: false })).toEqual({ text: '已关闭', color: 'default' })
    expect(channelReachabilityView({ enabled: true, reachable: false })).toEqual({ text: '缺少地址', color: 'orange' })
    expect(formatPreferenceSummary({
      totalChannels: 2,
      reachableChannelCount: 1,
      optInCount: 1,
      optOutCount: 1,
      activeSuppressionCount: 3,
    })).toContain('生效抑制 3 条')
  })

  it('formats backend datetime values', () => {
    expect(formatPreferenceDateTime('2026-06-05T10:11:12.123')).toBe('2026-06-05 10:11:12')
    expect(formatPreferenceDateTime(null)).toBe('-')
  })
})
