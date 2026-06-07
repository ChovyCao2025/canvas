import type { ComputedTagPayload, ImpactCheck, LineageImpact } from '../../services/cdpApi'

export interface ComputedTagRunSummary {
  scannedCount: number
  matchedCount: number
  updatedCount: number
  skippedCount: number
  failedCount: number
}

export interface ComputedTagFormValues {
  tagCode: string
  displayName: string
  valueType: ComputedTagPayload['valueType']
  computeType: ComputedTagPayload['computeType']
  refreshMode: ComputedTagPayload['refreshMode']
  expressionText: string
  dependenciesText?: string
}

export function statusText(status: string): string {
  if (status === 'ACTIVE') return '启用'
  if (status === 'PAUSED') return '暂停'
  if (status === 'DRAFT') return '草稿'
  return status || '-'
}

export function statusColor(status: string): string {
  if (status === 'ACTIVE') return 'green'
  if (status === 'PAUSED') return 'orange'
  if (status === 'DRAFT') return 'blue'
  return 'default'
}

export function formatComputedTagRunSummary(summary: ComputedTagRunSummary): string {
  return `扫描 ${summary.scannedCount}，命中 ${summary.matchedCount}，更新 ${summary.updatedCount}，跳过 ${summary.skippedCount}，失败 ${summary.failedCount}`
}

export function formatLineageImpact(impact: LineageImpact): string {
  const name = impact.objectName ? ` ${impact.objectName}` : ''
  return `${impact.objectType} #${impact.objectId ?? '-'}${name} - ${impact.referencePath}`
}

export function validateFallbackImpact(check: ImpactCheck): { disabled: boolean; reason: string } {
  if (check.allowed) return { disabled: false, reason: '' }
  return { disabled: true, reason: `${check.reason ?? 'BLOCKED'} (${check.impacts.length} impact)` }
}

export function buildComputedTagPayload(values: ComputedTagFormValues): ComputedTagPayload {
  return {
    tagCode: values.tagCode.trim(),
    displayName: values.displayName.trim(),
    valueType: values.valueType,
    computeType: values.computeType,
    refreshMode: values.refreshMode,
    expressionJson: JSON.stringify(JSON.parse(values.expressionText)),
    dependencies: Array.from(new Set((values.dependenciesText || '')
      .split(/[\n,]/)
      .map(value => value.trim())
      .filter(Boolean))),
  }
}
