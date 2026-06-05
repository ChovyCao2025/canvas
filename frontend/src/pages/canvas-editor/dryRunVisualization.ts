export type TraceStatus = 0 | 1 | 2 | 3

type TraceLike = {
  nodeId?: string
  status: TraceStatus
}

export const TRACE_COLORS = {
  0: '#faad14',
  1: '#52c41a',
  2: '#f5222d',
  3: '#d9d9d9',
} as const

export function buildTraceColorMap(traces: TraceLike[]) {
  return Object.fromEntries(
    traces
      .filter(trace => trace.nodeId)
      .map(trace => [trace.nodeId as string, TRACE_COLORS[trace.status]]),
  )
}

export function buildDryRunSummary(traces: Array<Pick<TraceLike, 'status'>>) {
  return traces.reduce((acc, trace) => {
    if (trace.status === 0) acc.running += 1
    if (trace.status === 1) acc.success += 1
    if (trace.status === 2) acc.failed += 1
    if (trace.status === 3) acc.skipped += 1
    return acc
  }, { running: 0, success: 0, failed: 0, skipped: 0 })
}
