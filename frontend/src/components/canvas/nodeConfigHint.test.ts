/**
 * 测试职责：验证画布节点卡片展示关键配置，避免 MQ 节点已配置但画布上不可见。
 */
import { describe, expect, it } from 'vitest'
import { getNodeConfigHint } from './nodeConfigHint'

describe('getNodeConfigHint', () => {
  it('shows selected message code for MQ trigger nodes before legacy topicKey', () => {
    expect(getNodeConfigHint('MQ_TRIGGER', {
      topicKey: 'CANVAS_MQ_TRIGGER',
      messageCodeKey: 'flight_order_status_change',
    })).toBe('消息: flight_order_status_change')
  })

  it('shows selected message code for SEND_MQ nodes', () => {
    expect(getNodeConfigHint('SEND_MQ', { messageCodeKey: 'order_paid_notice' })).toBe('消息: order_paid_notice')
  })
})
