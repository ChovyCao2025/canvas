import type { UserRole } from '../../services/api'
import type { OpsAuditEvent, OpsRuntimeStatus } from '../../services/opsApi'

export type OpsAlertSeverity = 'critical' | 'warning' | 'info'

export interface OpsAlertSummary {
  key: string
  title: string
  metric: string
  severity: OpsAlertSeverity
  scope: 'tenant' | 'system'
  runbookAnchor: string
}

export const OPS_ALERT_SUMMARIES: OpsAlertSummary[] = [
  {
    key: 'failed-execution-spike',
    title: '执行失败率升高',
    metric: 'canvas.execution.total{status="FAILED"}',
    severity: 'critical',
    scope: 'tenant',
    runbookAnchor: 'failed-execution-spike',
  },
  {
    key: 'dlq-growth',
    title: 'DLQ 积压增长',
    metric: 'canvas.capacity.dlq.backlog',
    severity: 'critical',
    scope: 'tenant',
    runbookAnchor: 'dlq-growth',
  },
  {
    key: 'trace-buffer-overflow',
    title: 'Trace 缓冲区丢弃',
    metric: 'canvas.trace.dropped.total',
    severity: 'warning',
    scope: 'system',
    runbookAnchor: 'trace-buffer-overflow',
  },
  {
    key: 'mysql-pool-pressure',
    title: 'MySQL 连接池压力',
    metric: 'canvas.mysql.pool.pressure.percent',
    severity: 'warning',
    scope: 'system',
    runbookAnchor: 'mysql-pool-pressure',
  },
]

export function opsRuntimeStatusColor(status?: string | null) {
  switch ((status ?? '').toUpperCase()) {
    case 'UP':
      return 'green'
    case 'DEGRADED':
      return 'orange'
    case 'DOWN':
      return 'red'
    default:
      return 'default'
  }
}

export function opsAlertSeverityColor(severity?: string | null) {
  switch ((severity ?? '').toLowerCase()) {
    case 'critical':
      return 'red'
    case 'warning':
      return 'orange'
    case 'info':
      return 'blue'
    default:
      return 'default'
  }
}

export function canRunEmergencyAction(role?: UserRole | string | null) {
  return role === 'ADMIN' || role === 'SUPER_ADMIN' || role === 'TENANT_ADMIN'
}

export function formatOpsDateTime(value?: string | null) {
  if (!value) return '-'
  return value.replace('T', ' ').slice(0, 19)
}

export function summarizeRuntimeStatus(status?: OpsRuntimeStatus | null) {
  if (!status) return '未知'
  const tenant = status.tenantId == null ? 'global' : `tenant ${status.tenantId}`
  return `${status.status} · ${status.role} · ${tenant}`
}

export function normalizeOpsAuditEvents(events: OpsAuditEvent[] | undefined, limit = 20) {
  return [...(events ?? [])]
    .sort((left, right) => String(right.createdAt ?? '').localeCompare(String(left.createdAt ?? '')))
    .slice(0, Math.max(1, Math.min(limit, 100)))
}

export function isOpsPermissionError(error: unknown) {
  const candidate = error as {
    kind?: string
    response?: { status?: number }
    status?: number
  } | null
  return candidate?.kind === 'forbidden'
    || candidate?.status === 403
    || candidate?.response?.status === 403
}
