export interface RealtimeSetOperationView {
  status: string
  reason?: string | null
  resultSize?: number | null
  safeLimit?: number | null
}

export interface RealtimeSnapshotView {
  id?: number | string | null
  audienceId?: number | string | null
  estimatedSize: number
  bitmapKey?: string | null
  snapshotSource: string
  createdAt?: string | null
}

export function realtimeStatusText(status: string): string {
  if (status === 'UPDATED') return 'Updated'
  if (status === 'DUPLICATED') return 'Duplicated'
  if (status === 'BLOCKED') return 'Blocked'
  if (status === 'SKIPPED') return 'Skipped'
  return status || '-'
}

export function formatOverlapPercent(value?: number | null): string {
  return `${(value ?? 0).toFixed(1)}%`
}

export function formatSetOperation(result: RealtimeSetOperationView): string {
  const resultSize = result.resultSize ?? 0
  if (result.status === 'BLOCKED') {
    const reason = result.reason ?? 'BLOCKED'
    return `Blocked: ${reason}, result size ${resultSize} exceeds safe limit ${result.safeLimit ?? 0}`
  }
  if (result.status === 'READY') return `Ready: result size ${resultSize}`
  return `${realtimeStatusText(result.status)}: result size ${resultSize}`
}

export function formatSnapshotRow(row: RealtimeSnapshotView): string {
  return `#${row.audienceId ?? row.id ?? '-'} size ${row.estimatedSize} from ${row.snapshotSource} at ${row.createdAt ?? '-'}`
}
