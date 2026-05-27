/**
 * 测试职责：验证通知 WebSocket 地址、重连退避和实时消息合并策略。
 *
 * 维护说明：实时同步协议变化时，应先更新这些纯函数测试再改 Provider 生命周期。
 */
import { describe, expect, it } from 'vitest'
import { buildNotificationWebSocketUrl, mergeRealtimeNotifications, nextNotificationReconnectDelay } from './notificationRealtime'

describe('notificationRealtime', () => {
  it('builds websocket url from browser location', () => {
    expect(buildNotificationWebSocketUrl('ticket_1', { protocol: 'https:', host: 'canvas.test' }))
      .toBe('wss://canvas.test/canvas/ws/notifications?ticket=ticket_1')
    expect(buildNotificationWebSocketUrl('ticket_2', { protocol: 'http:', host: 'localhost:3001' }))
      .toBe('ws://localhost:3001/canvas/ws/notifications?ticket=ticket_2')
  })

  it('uses capped exponential reconnect delay', () => {
    expect(nextNotificationReconnectDelay(0)).toBe(1000)
    expect(nextNotificationReconnectDelay(1)).toBe(2000)
    expect(nextNotificationReconnectDelay(5)).toBe(30000)
    expect(nextNotificationReconnectDelay(8)).toBe(30000)
  })

  it('replaces list on sync payload', () => {
    const merged = mergeRealtimeNotifications(
      [{ notificationId: 'old', type: 'TASK_SUCCEEDED', category: 'TASK', severity: 'SUCCESS', status: 'UNREAD', title: 'old', createdAt: '2026-05-22T10:00:00' }],
      {
        eventType: 'SYNC',
        notifications: [
          { notificationId: 'new', type: 'TASK_FAILED', category: 'TASK', severity: 'ERROR', status: 'UNREAD', title: 'new', createdAt: '2026-05-23T10:00:00' },
        ],
      },
    )

    expect(merged.map(item => item.notificationId)).toEqual(['new'])
  })

  it('upserts single realtime notification and keeps newest first', () => {
    const merged = mergeRealtimeNotifications(
      [
        { notificationId: 'ntf_1', type: 'TASK_SUCCEEDED', category: 'TASK', severity: 'SUCCESS', status: 'READ', title: 'old', createdAt: '2026-05-22T10:00:00' },
        { notificationId: 'ntf_2', type: 'TASK_FAILED', category: 'TASK', severity: 'ERROR', status: 'UNREAD', title: 'older', createdAt: '2026-05-21T10:00:00' },
      ],
      {
        eventType: 'NOTIFICATION_UPDATED',
        notification: { notificationId: 'ntf_2', type: 'TASK_FAILED', category: 'TASK', severity: 'ERROR', status: 'READ', title: 'updated', createdAt: '2026-05-23T10:00:00' },
      },
    )

    expect(merged.map(item => item.notificationId)).toEqual(['ntf_2', 'ntf_1'])
    expect(merged[0]?.status).toBe('READ')
  })
})
