import { describe, expect, it } from 'vitest'
import {
  deliveryStatusColor,
  deliveryStatusLabel,
  maskWebhookSecret,
  subscriptionStatusColor,
  subscriptionStatusLabel,
} from './webhookSubscriptionPresentation'

describe('webhook subscription presentation', () => {
  it('labels subscription statuses', () => {
    expect(subscriptionStatusLabel('ACTIVE')).toBe('启用')
    expect(subscriptionStatusLabel('PAUSED')).toBe('暂停')
    expect(subscriptionStatusLabel('DISABLED')).toBe('禁用')
    expect(subscriptionStatusColor('ACTIVE')).toBe('green')
  })

  it('marks retrying and dead deliveries clearly', () => {
    expect(deliveryStatusLabel('RETRYING')).toBe('重试中')
    expect(deliveryStatusLabel('DEAD')).toBe('已终止')
    expect(deliveryStatusColor('DEAD')).toBe('red')
  })

  it('masks webhook secrets after creation or rotation', () => {
    expect(maskWebhookSecret('whsec_abcdef123456')).toBe('whsec_ab****')
    expect(maskWebhookSecret('')).toBe('')
  })
})
