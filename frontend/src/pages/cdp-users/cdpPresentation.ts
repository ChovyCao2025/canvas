import type { BatchTagPayload, TagWritePayload } from '../../services/cdpApi'

export function buildTagWritePayload(values: Record<string, unknown>): TagWritePayload {
  return {
    tagCode: String(values.tagCode || '').trim(),
    tagValue: values.tagValue == null || values.tagValue === '' ? null : String(values.tagValue),
    reason: values.reason == null || values.reason === '' ? null : String(values.reason),
    sourceType: 'MANUAL',
  }
}

export function buildBatchTagPayload(values: Record<string, unknown>): BatchTagPayload {
  const userIds = String(values.userIds || '')
    .split(/[\n,]/)
    .map(item => item.trim())
    .filter(Boolean)

  return {
    operationType: (values.operationType as BatchTagPayload['operationType']) || 'BATCH_SET',
    tagCode: String(values.tagCode || '').trim(),
    tagValue: values.tagValue == null || values.tagValue === '' ? null : String(values.tagValue),
    userIds,
    reason: values.reason == null || values.reason === '' ? null : String(values.reason),
    operator: values.operator == null || values.operator === '' ? null : String(values.operator),
  }
}

export function formatExecutionStatus(status?: string) {
  if (status === 'SUCCESS') return { label: '成功', color: 'success' as const }
  if (status === 'FAILED') return { label: '失败', color: 'error' as const }
  if (status === 'PAUSED') return { label: '挂起', color: 'warning' as const }
  if (status === 'RUNNING') return { label: '执行中', color: 'processing' as const }
  return { label: status || '-', color: 'default' as const }
}

export function tagColor(tagCode: string): string {
  const colors = ['blue', 'green', 'purple', 'cyan', 'geekblue', 'gold']
  let hash = 0
  for (const ch of tagCode) hash = (hash * 31 + ch.charCodeAt(0)) >>> 0
  return colors[hash % colors.length]
}

export function formatDateTime(value?: string | null): string {
  return value ? value.replace('T', ' ').slice(0, 19) : '-'
}
