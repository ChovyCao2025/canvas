import { describe, expect, it } from 'vitest'
import {
  canRunEmergencyAction,
  formatOpsDateTime,
  opsAlertSeverityColor,
  opsRuntimeStatusColor,
  normalizeOpsAuditEvents,
  summarizeRuntimeStatus,
  isOpsPermissionError,
} from './opsDashboardPresentation'

describe('ops dashboard presentation', () => {
  it('formats runtime state, audit events, and alert severity colors', () => {
    expect(opsRuntimeStatusColor('UP')).toBe('green')
    expect(opsRuntimeStatusColor('DEGRADED')).toBe('orange')
    expect(opsAlertSeverityColor('critical')).toBe('red')
    expect(formatOpsDateTime('2026-06-05T09:10:11')).toBe('2026-06-05 09:10:11')
    expect(summarizeRuntimeStatus({
      status: 'UP',
      role: 'TENANT_ADMIN',
      tenantId: 9,
      username: 'ops-user',
    })).toBe('UP · TENANT_ADMIN · tenant 9')

    expect(normalizeOpsAuditEvents([
      { id: '2', action: 'KILL', canvasId: 12, operator: 'alice', role: 'TENANT_ADMIN', reason: 'bad deploy', createdAt: '2026-06-05T09:00:00' },
      { id: '1', action: 'PAUSE', canvasId: 11, operator: 'bob', role: 'OPERATOR', reason: 'observe only', createdAt: '2026-06-05T08:00:00' },
    ], 1)).toEqual([
      { id: '2', action: 'KILL', canvasId: 12, operator: 'alice', role: 'TENANT_ADMIN', reason: 'bad deploy', createdAt: '2026-06-05T09:00:00' },
    ])
  })

  it('separates read-only operators from emergency actors and detects permission errors', () => {
    expect(canRunEmergencyAction('ADMIN')).toBe(true)
    expect(canRunEmergencyAction('SUPER_ADMIN')).toBe(true)
    expect(canRunEmergencyAction('TENANT_ADMIN')).toBe(true)
    expect(canRunEmergencyAction('OPERATOR')).toBe(false)
    expect(isOpsPermissionError({ kind: 'forbidden' })).toBe(true)
    expect(isOpsPermissionError({ response: { status: 403 } })).toBe(true)
    expect(isOpsPermissionError(new Error('boom'))).toBe(false)
  })
})
