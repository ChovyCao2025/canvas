import type { PredictionRunStatus, RiskBand, RiskDistributionItem } from '../../services/aiPredictionApi'

const RISK_ORDER: RiskBand[] = ['HIGH', 'MEDIUM', 'LOW']

export function riskBandLabel(band?: RiskBand | null) {
  switch (band) {
    case 'HIGH':
      return '高风险'
    case 'MEDIUM':
      return '中风险'
    case 'LOW':
      return '低风险'
    default:
      return band || '-'
  }
}

export function riskBandColor(band?: RiskBand | null) {
  switch (band) {
    case 'HIGH':
      return 'red'
    case 'MEDIUM':
      return 'orange'
    case 'LOW':
      return 'green'
    default:
      return 'default'
  }
}

export function runStatusColor(status?: PredictionRunStatus | null) {
  switch (status) {
    case 'SUCCESS':
      return 'green'
    case 'RUNNING':
      return 'blue'
    case 'FAILED':
      return 'red'
    default:
      return 'default'
  }
}

export function formatDateTime(value?: string | null) {
  if (!value) return '-'
  return value.replace('T', ' ').slice(0, 19)
}

export function formatProbability(value?: number | string | null) {
  const number = Number(value)
  if (!Number.isFinite(number)) return '-'
  return `${(number * 100).toFixed(1)}%`
}

export function distributionTotal(items: RiskDistributionItem[]) {
  return items.reduce((sum, item) => sum + Number(item.count || 0), 0)
}

export function orderedDistribution(items: RiskDistributionItem[]) {
  const byBand = new Map(items.map(item => [item.band, item]))
  const ordered = RISK_ORDER.map(band => byBand.get(band) ?? { band, count: 0 })
  const extras = items.filter(item => !RISK_ORDER.includes(item.band))
  return [...ordered, ...extras]
}

export function distributionPercent(count: number, total: number) {
  if (total <= 0) return 0
  return Math.round((count / total) * 100)
}
