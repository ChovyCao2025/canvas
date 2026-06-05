import { describe, expect, it } from 'vitest'
import { contactabilityCheckView, contactabilityStatusView } from './contactabilityPresentation'

describe('contactabilityPresentation', () => {
  it('maps overall allowed state to stable tag presentation', () => {
    expect(contactabilityStatusView({ userId: 'u1', channel: 'SMS', allowed: true, checks: [] }))
      .toEqual({ label: '可触达', color: 'green' })
    expect(contactabilityStatusView({ userId: 'u1', channel: 'SMS', allowed: false, checks: [] }))
      .toEqual({ label: '已拦截', color: 'red' })
  })

  it('maps policy checks to labels, colors, and reason text', () => {
    expect(contactabilityCheckView({
      checkKey: 'SUPPRESSION',
      allowed: false,
      reasonCode: 'MARKETING_SUPPRESSED',
      reasonMessage: '用户命中营销抑制名单',
    })).toEqual({
      label: '抑制名单',
      color: 'red',
      text: '用户命中营销抑制名单',
    })

    expect(contactabilityCheckView({
      checkKey: 'FREQUENCY',
      allowed: true,
      reasonCode: null,
      reasonMessage: null,
    })).toEqual({
      label: '频控',
      color: 'green',
      text: '通过',
    })
  })
})
