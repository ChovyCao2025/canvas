import { describe, expect, it } from 'vitest'
import {
  conversationReplySummary,
  demoExpiryText,
  demoMarkerWarning,
  sandboxReplyEventId,
  sandboxReplyStateText,
  resetStateText,
  sandboxStatusView,
  type DemoSandbox,
  type SandboxConversationReplyResult,
} from './demoSandbox'

describe('demoSandbox helpers', () => {
  const sandbox: DemoSandbox = {
    tenantId: 8,
    demoName: 'Retail Demo',
    demoMarker: 'DEMO_TENANT_8',
    status: 'ACTIVE',
    expiresAt: '2026-06-17T00:00:00Z',
    lastResetAt: null,
  }

  it('formats expiry and marker warnings', () => {
    expect(demoExpiryText(sandbox)).toBe('Expires at 2026-06-17T00:00:00Z')
    expect(demoMarkerWarning(sandbox)).toBe('Demo data marker: DEMO_TENANT_8')
  })

  it('formats reset state and sandbox status', () => {
    expect(resetStateText({ status: 'IDLE' })).toBe('Ready to reset')
    expect(resetStateText({ status: 'RUNNING' })).toBe('Reset running')
    expect(resetStateText({ status: 'FAILED', message: 'sandbox tenant 8 is not installed' }))
      .toBe('Reset failed: sandbox tenant 8 is not installed')
    expect(sandboxStatusView('ACTIVE')).toEqual({ text: '可用', color: 'green' })
  })

  it('formats sandbox conversation reply state and ids', () => {
    const recorded: SandboxConversationReplyResult = {
      sessionId: 100,
      messageId: 200,
      status: 'RECORDED',
      duplicate: false,
      resumedWaitCount: 1,
    }
    const duplicate: SandboxConversationReplyResult = {
      ...recorded,
      duplicate: true,
      resumedWaitCount: 0,
    }

    expect(conversationReplySummary(recorded))
      .toBe('Recorded message 200 in session 100; resumed 1 wait')
    expect(conversationReplySummary(duplicate))
      .toBe('Duplicate message 200 in session 100; no waits resumed')
    expect(sandboxReplyStateText({ status: 'IDLE' })).toBe('Ready to simulate reply')
    expect(sandboxReplyStateText({ status: 'RUNNING' })).toBe('Simulating reply')
    expect(sandboxReplyStateText({ status: 'FAILED', message: 'missing user' }))
      .toBe('Reply failed: missing user')
    expect(sandboxReplyStateText({ status: 'RECORDED', result: recorded }))
      .toBe('Recorded message 200 in session 100; resumed 1 wait')
    expect(sandboxReplyEventId(8, 1717000000000)).toBe('sandbox-reply-8-1717000000000')
  })
})
