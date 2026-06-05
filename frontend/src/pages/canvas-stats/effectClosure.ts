import type { AttributionSummary } from '../../services/api'

export interface ReceiptStatusRow {
  status: string
  label: string
  count: number
}

export interface AttributionKpi {
  label: string
  value: string
}

const RECEIPT_STATUS_ORDER = [
  { key: 'sent', status: 'SENT', label: '已发送' },
  { key: 'failed', status: 'FAILED', label: '失败' },
  { key: 'skipped', status: 'SKIPPED', label: '策略跳过' },
  { key: 'pending', status: 'PENDING', label: '待发送' },
]

export function buildReceiptStatusRows(counts: Record<string, number>): ReceiptStatusRow[] {
  return RECEIPT_STATUS_ORDER
    .map(item => ({
      status: item.status,
      label: item.label,
      count: Number(counts[item.key] ?? counts[item.status] ?? 0),
    }))
    .filter(row => row.count > 0)
}

export function buildAttributionKpis(summary: AttributionSummary): AttributionKpi[] {
  return [
    { label: '转化次数', value: Number(summary.conversions ?? 0).toLocaleString() },
    { label: '转化金额', value: Number(summary.conversionAmount ?? 0).toFixed(2) },
    { label: '归因触达', value: Number(summary.attributedSends ?? 0).toLocaleString() },
    { label: '归因模型', value: summary.model || 'LAST_TOUCH' },
  ]
}
