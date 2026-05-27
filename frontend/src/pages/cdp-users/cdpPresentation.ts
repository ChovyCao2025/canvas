/**
 * 页面职责：CDP 用户列表展示工具，统一标签文案、状态颜色和基础统计计算。
 *
 * 维护说明：保持纯函数以便列表页和测试复用。
 */
import type { BatchTagPayload, TagWritePayload } from '../../services/cdpApi'

/** 从单用户打标表单构造接口 payload，空字符串统一转为 null。 */
export function buildTagWritePayload(values: Record<string, unknown>): TagWritePayload {
  return {
    tagCode: String(values.tagCode || '').trim(),
    tagValue: values.tagValue == null || values.tagValue === '' ? null : String(values.tagValue),
    reason: values.reason == null || values.reason === '' ? null : String(values.reason),
    sourceType: 'MANUAL',
  }
}

/** 从批量打标表单构造接口 payload，支持换行或逗号分隔用户 ID。 */
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

/** 将执行状态转换为表格 Tag 可用的文案和颜色。 */
export function formatExecutionStatus(status?: string) {
  if (status === 'SUCCESS') return { label: '成功', color: 'success' as const }
  if (status === 'FAILED') return { label: '失败', color: 'error' as const }
  if (status === 'PAUSED') return { label: '挂起', color: 'warning' as const }
  if (status === 'RUNNING') return { label: '执行中', color: 'processing' as const }
  return { label: status || '-', color: 'default' as const }
}

/** 为标签编码生成稳定颜色，同一个标签在列表刷新后颜色不漂移。 */
export function tagColor(tagCode: string): string {
  const colors = ['blue', 'green', 'purple', 'cyan', 'geekblue', 'gold']
  let hash = 0
  for (const ch of tagCode) hash = (hash * 31 + ch.charCodeAt(0)) >>> 0
  return colors[hash % colors.length]
}

/** 轻量时间格式化，兼容 ISO 字符串和空值。 */
export function formatDateTime(value?: string | null): string {
  return value ? value.replace('T', ' ').slice(0, 19) : '-'
}
