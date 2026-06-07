import type { R } from '../types'
import type {
  DemoSandbox,
  DemoSandboxResetResult,
  SandboxConversationReplyPayload,
  SandboxConversationReplyResult,
} from '../pages/demo-sandbox/demoSandbox'
import http from './api'

export const demoSandboxApi = {
  install: (payload: { tenantId: number; demoName: string; ttlDays: number }) =>
    http.post<R<DemoSandbox>, R<DemoSandbox>>('/demo-sandboxes', payload),

  reset: (tenantId: number) =>
    http.post<R<DemoSandboxResetResult>, R<DemoSandboxResetResult>>(`/demo-sandboxes/${tenantId}/reset`),

  reply: (tenantId: number, payload: SandboxConversationReplyPayload) =>
    http.post<R<SandboxConversationReplyResult>, R<SandboxConversationReplyResult>>(
      `/demo-sandboxes/${tenantId}/conversation-replies`,
      payload,
    ),

  expired: () =>
    http.get<R<DemoSandbox[]>, R<DemoSandbox[]>>('/demo-sandboxes/expired'),
}
