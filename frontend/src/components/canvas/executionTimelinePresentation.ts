/**
 * Presentation helpers for execution trace timelines.
 *
 * Backend trace contracts stay intentionally small here: helpers accept the
 * fields already returned by execution trace APIs and only format UI state.
 */

export type TraceStatusValue = 0 | 1 | 2 | 3 | number | string | null | undefined

export interface TraceStatusPresentation {
  color: string
  label: string
  pathKey: 'running' | 'success' | 'error' | 'skipped' | 'unknown'
}

export interface DownloadableTrace {
  nodeId?: string
  nodeType?: string
  nodeName?: string
  status?: TraceStatusValue
  durationMs?: number | null
  errorMsg?: string | null
}

const STATUS_PRESENTATION: Record<number, TraceStatusPresentation> = {
  0: { color: 'processing', label: '执行中', pathKey: 'running' },
  1: { color: 'success', label: '成功', pathKey: 'success' },
  2: { color: 'error', label: '失败', pathKey: 'error' },
  3: { color: 'default', label: '跳过', pathKey: 'skipped' },
}

export const TRACE_ERROR_PREVIEW_LENGTH = 180

export function formatTraceStatus(status: TraceStatusValue): TraceStatusPresentation {
  const numericStatus = Number(status)
  return STATUS_PRESENTATION[numericStatus] ?? {
    color: 'default',
    label: '未知',
    pathKey: 'unknown',
  }
}

export function isLongError(errorMsg?: string | null): boolean {
  return (errorMsg?.length ?? 0) > TRACE_ERROR_PREVIEW_LENGTH
}

export function downloadErrorText(trace: DownloadableTrace, executionId?: string | null): string {
  const status = formatTraceStatus(trace.status)
  const duration = trace.durationMs != null ? `${trace.durationMs}ms` : '-'

  return [
    'Execution Trace Error',
    `Execution ID: ${executionId || '-'}`,
    `Node ID: ${trace.nodeId || '-'}`,
    `Node Name: ${trace.nodeName || '-'}`,
    `Node Type: ${trace.nodeType || '-'}`,
    `Status: ${status.label}`,
    `Duration: ${duration}`,
    '',
    'Error:',
    trace.errorMsg || '',
  ].join('\n')
}

export function tracePathClass(trace: { status?: TraceStatusValue } | TraceStatusValue): string {
  const status = typeof trace === 'object' && trace !== null ? trace.status : trace
  const { pathKey } = formatTraceStatus(status)
  return `execution-trace-path execution-trace-path--${pathKey}`
}
