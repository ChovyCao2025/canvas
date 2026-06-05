import { describe, expect, it } from 'vitest'
import {
  audienceMembershipStatusView,
  channelCandidateView,
  formatDateTime,
  formatWindowSeconds,
  healthCheckView,
  healthScoreView,
  journeyStepStatusView,
  suppressionStateView,
} from './mauticInsightsPresentation'

describe('mauticInsightsPresentation', () => {
  it('maps audience membership states to operator-facing badges', () => {
    expect(audienceMembershipStatusView('MATCHED')).toEqual({ text: '命中人群', color: 'green' })
    expect(audienceMembershipStatusView('NOT_MATCHED')).toEqual({ text: '未命中', color: 'red' })
    expect(audienceMembershipStatusView('NOT_READY')).toEqual({ text: '未就绪', color: 'orange' })
  })

  it('maps journey, channel, suppression, and health states', () => {
    expect(journeyStepStatusView('FAILED')).toEqual({ text: 'FAILED', color: 'red' })
    expect(channelCandidateView('SUPPRESSED')).toEqual({ text: '已抑制', color: 'red' })
    expect(suppressionStateView('ACTIVE')).toEqual({ text: '生效中', color: 'red' })
    expect(healthCheckView(false)).toEqual({ text: '未通过', color: 'red' })
  })

  it('formats scores, time windows, and datetimes consistently', () => {
    expect(healthScoreView(95)).toEqual({ text: '健康', color: 'green' })
    expect(healthScoreView(60)).toEqual({ text: '需关注', color: 'orange' })
    expect(healthScoreView(20)).toEqual({ text: '阻塞', color: 'red' })
    expect(formatWindowSeconds(86_400)).toBe('1 天')
    expect(formatWindowSeconds(7_200)).toBe('2 小时')
    expect(formatDateTime('2026-06-05T09:30:12')).toBe('2026-06-05 09:30:12')
  })
})
