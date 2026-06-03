/**
 * 测试职责：验证通知实时连接的 socket ownership 和重试上限决策。
 *
 * 维护说明：Provider 生命周期依赖这些纯函数避免旧 WebSocket 关闭事件污染新连接。
 */
import { describe, expect, it } from 'vitest'
import {
  MAX_RECONNECT_ATTEMPTS,
  isActiveNotificationSocket,
  nextNotificationReconnectPlan,
} from './NotificationContext'

describe('notification realtime connection lifecycle', () => {
  it('ignores lifecycle events from stale sockets', () => {
    expect(isActiveNotificationSocket('socket-2', 'socket-1')).toBe(false)
    expect(isActiveNotificationSocket('socket-2', 'socket-2')).toBe(true)
  })

  it('switches to polling after retry cap', () => {
    expect(nextNotificationReconnectPlan(0)).toMatchObject({
      mode: 'reconnect',
      nextAttempt: 1,
    })
    expect(nextNotificationReconnectPlan(MAX_RECONNECT_ATTEMPTS)).toEqual({
      mode: 'polling',
      nextAttempt: MAX_RECONNECT_ATTEMPTS,
    })
  })
})
