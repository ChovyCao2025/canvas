import type { EventCountRow, UserTimelineRow } from '../../services/analyticsApi'

export type AnalyticsExportState = 'QUEUED' | 'RUNNING' | 'DONE' | 'FAILED' | 'UNAVAILABLE' | string

export interface AnalyticsDateRangeInput {
  startDate?: string
  endDate?: string
}

export interface EventCountTableRow extends EventCountRow {
  key: string
  label: string
}

export function requireDateRangeMessage(input: AnalyticsDateRangeInput): string | null {
  return input.startDate && input.endDate ? null : '请选择开始和结束日期'
}

export function formatEventCount(row: EventCountRow): string {
  return `${row.eventCode}: ${row.count}`
}

export function timelineRowText(row: UserTimelineRow): string {
  return `${formatDateTime(row.eventTime)} - ${row.eventCode}`
}

export function toEventCountRows(rows: EventCountRow[]): EventCountTableRow[] {
  return [...rows]
    .sort((a, b) => Number(b.count || 0) - Number(a.count || 0))
    .map(row => ({
      ...row,
      key: row.eventCode,
      label: formatEventCount(row),
    }))
}

export function formatDateTime(value?: string | null): string {
  if (!value) return '-'
  return value.replace('T', ' ').slice(0, 19)
}

export function exportStateText(status: AnalyticsExportState): string {
  switch (status) {
    case 'QUEUED':
      return '已排队'
    case 'RUNNING':
      return '生成中'
    case 'DONE':
      return '已完成'
    case 'FAILED':
      return '失败'
    case 'UNAVAILABLE':
      return '暂不可用'
    default:
      return status || '-'
  }
}
