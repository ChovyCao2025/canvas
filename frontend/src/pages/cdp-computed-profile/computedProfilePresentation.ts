import type { ComputedProfileAttributePayload } from '../../services/cdpApi'

export interface PreviewSummary {
  scannedCount: number
  matchedCount: number
  changedCount: number
  unchangedCount: number
}

export interface ComputedProfileFormValues {
  attrCode: string
  displayName: string
  valueType: ComputedProfileAttributePayload['valueType']
  computeType: ComputedProfileAttributePayload['computeType']
  refreshMode: ComputedProfileAttributePayload['refreshMode']
  expressionText: string
}

export function profileAttributeStatusText(status: string): string {
  if (status === 'ACTIVE') return '启用'
  if (status === 'PAUSED') return '暂停'
  if (status === 'DRAFT') return '草稿'
  return status || '-'
}

export function profileAttributeStatusColor(status: string): string {
  if (status === 'ACTIVE') return 'green'
  if (status === 'PAUSED') return 'orange'
  if (status === 'DRAFT') return 'blue'
  return 'default'
}

export function formatRunStatus(status: string): string {
  if (status === 'SUCCESS') return '成功'
  if (status === 'FAILED') return '失败'
  if (status === 'DUPLICATED') return '重复事件'
  if (status === 'RUNNING') return '运行中'
  return status || '-'
}

export function formatPreviewSummary(summary: PreviewSummary): string {
  return `扫描 ${summary.scannedCount}，命中 ${summary.matchedCount}，变更 ${summary.changedCount}，未变 ${summary.unchangedCount}`
}

export function formatValueChange(oldValue?: string | null, newValue?: string | null): string {
  return `${oldValue ?? '(空)'} -> ${newValue ?? '(空)'}`
}

export function buildComputedProfilePayload(values: ComputedProfileFormValues): ComputedProfileAttributePayload {
  return {
    attrCode: values.attrCode.trim(),
    displayName: values.displayName.trim(),
    valueType: values.valueType,
    computeType: values.computeType,
    refreshMode: values.refreshMode,
    expressionJson: JSON.stringify(JSON.parse(values.expressionText)),
  }
}
