export interface DemoSandbox {
  tenantId: number
  demoName: string
  demoMarker: string
  status: string
  expiresAt: string
  lastResetAt: string | null
}

export interface DemoSandboxResetResult {
  tenantId: number
  demoMarker: string
  resetAt: string
}

export interface SandboxConversationReplyPayload {
  canvasId?: number
  versionId?: number
  executionId?: string
  userId: string
  externalMessageId?: string
  eventId?: string
  text?: string
  intent?: string
  attributes?: Record<string, unknown>
}

export interface SandboxConversationReplyResult {
  sessionId: number
  messageId: number
  status: string
  duplicate: boolean
  resumedWaitCount: number
}

export type ResetState =
  | { status: 'IDLE' }
  | { status: 'RUNNING' }
  | { status: 'FAILED'; message: string }

export type SandboxReplyState =
  | { status: 'IDLE' }
  | { status: 'RUNNING' }
  | { status: 'FAILED'; message: string }
  | { status: 'RECORDED'; result: SandboxConversationReplyResult }

export function demoExpiryText(sandbox: DemoSandbox) {
  return `Expires at ${sandbox.expiresAt}`
}

export function demoMarkerWarning(sandbox: DemoSandbox) {
  return `Demo data marker: ${sandbox.demoMarker}`
}

export function resetStateText(state: ResetState) {
  if (state.status === 'RUNNING') return 'Reset running'
  if (state.status === 'FAILED') return `Reset failed: ${state.message}`
  return 'Ready to reset'
}

export function conversationReplySummary(result: SandboxConversationReplyResult) {
  if (result.duplicate) {
    return `Duplicate message ${result.messageId} in session ${result.sessionId}; no waits resumed`
  }
  const waitLabel = result.resumedWaitCount === 1 ? 'wait' : 'waits'
  return `Recorded message ${result.messageId} in session ${result.sessionId}; resumed ${result.resumedWaitCount} ${waitLabel}`
}

export function sandboxReplyStateText(state: SandboxReplyState) {
  if (state.status === 'RUNNING') return 'Simulating reply'
  if (state.status === 'FAILED') return `Reply failed: ${state.message}`
  if (state.status === 'RECORDED') return conversationReplySummary(state.result)
  return 'Ready to simulate reply'
}

export function sandboxReplyEventId(tenantId: number, timestamp = Date.now()) {
  return `sandbox-reply-${tenantId}-${timestamp}`
}

export function sandboxStatusView(status: string) {
  const views: Record<string, { text: string; color: string }> = {
    ACTIVE: { text: '可用', color: 'green' },
    EXPIRED: { text: '已过期', color: 'red' },
    RESETTING: { text: '重置中', color: 'gold' },
  }
  return views[status] ?? { text: status || '-', color: 'default' }
}
