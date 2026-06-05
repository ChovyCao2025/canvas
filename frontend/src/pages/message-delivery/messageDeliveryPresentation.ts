import type { DeliveryOutbox, DeliveryReceiptLog, DeliverySearchParams } from '../../services/messageDeliveryApi'

export const DELIVERY_STATUS_OPTIONS = ['PENDING', 'SENDING', 'SENT', 'RETRY', 'DEAD', 'DELIVERED', 'FAILED', 'OPENED', 'CLICKED', 'BOUNCED', 'UNSUBSCRIBED']
export const DELIVERY_CHANNEL_OPTIONS = ['SMS', 'EMAIL', 'PUSH', 'WECHAT', 'IN_APP']

export function deliveryStatusColor(status?: string | null) {
  switch ((status ?? '').toUpperCase()) {
    case 'SENT':
    case 'DELIVERED':
    case 'OPENED':
    case 'CLICKED':
      return 'green'
    case 'PENDING':
    case 'SENDING':
      return 'blue'
    case 'RETRY':
      return 'orange'
    case 'DEAD':
    case 'FAILED':
    case 'BOUNCED':
    case 'UNSUBSCRIBED':
      return 'red'
    default:
      return 'default'
  }
}

export function canReplayDelivery(row?: Pick<DeliveryOutbox, 'status'> | null) {
  return (row?.status ?? '').toUpperCase() === 'DEAD'
}

export function deliveryDetailTitle(row?: Pick<DeliveryOutbox, 'id' | 'status'> | null) {
  if (!row) return '投递详情'
  return `投递 #${row.id} · ${row.status}`
}

export function receiptLine(receipt: Pick<DeliveryReceiptLog, 'receiptType' | 'providerMessageId' | 'receivedAt'>) {
  return `${receipt.receiptType} · ${receipt.providerMessageId} · ${formatDateTime(receipt.receivedAt)}`
}

export function formatDateTime(value?: string | null) {
  if (!value) return '-'
  return value.replace('T', ' ').slice(0, 19)
}

export function normalizeDeliveryFilters(filters: DeliverySearchParams): DeliverySearchParams {
  const normalized: DeliverySearchParams = {
    page: Math.max(1, filters.page ?? 1),
    size: Math.max(1, Math.min(filters.size ?? 20, 100)),
  }
  if (filters.tenantId) normalized.tenantId = filters.tenantId
  if (filters.canvasId) normalized.canvasId = filters.canvasId
  copyText(normalized, 'executionId', filters.executionId)
  copyText(normalized, 'userId', filters.userId)
  copyText(normalized, 'providerMessageId', filters.providerMessageId)
  copyUpper(normalized, 'channel', filters.channel)
  copyUpper(normalized, 'provider', filters.provider)
  copyUpper(normalized, 'status', filters.status)
  return normalized
}

function copyText(target: DeliverySearchParams, key: keyof DeliverySearchParams, value?: string) {
  const text = value?.trim()
  if (text) {
    target[key] = text as never
  }
}

function copyUpper(target: DeliverySearchParams, key: keyof DeliverySearchParams, value?: string) {
  const text = value?.trim()
  if (text) {
    target[key] = text.toUpperCase() as never
  }
}
