export interface ApiReceiptStatus {
  code: string
  label: string
}

interface ApiParamLike {
  name: string
  displayName?: string
  type?: string
  required?: boolean
}

interface RequestPreviewInput {
  requestSchema?: ApiParamLike[]
  includeContextPayload?: boolean
}

interface ReceiptPreviewInput {
  receiptEnabled?: boolean
  receiptStatuses?: ApiReceiptStatus[]
}

export function buildApiRequestPreview(input: RequestPreviewInput) {
  const payload: Record<string, unknown> = {}
  for (const item of input.requestSchema ?? []) {
    if (!item?.name) continue
    payload[item.name] = sampleValue(item.type)
  }
  if (input.includeContextPayload) {
    payload.contextPayload = {
      executionId: 'exec_demo',
      userId: 'user_demo',
    }
  }
  return payload
}

export function buildApiReceiptPreview(input: ReceiptPreviewInput) {
  return {
    receiptEnabled: !!input.receiptEnabled,
    statuses: (input.receiptStatuses ?? []).map(item => ({
      code: item.code,
      label: item.label,
    })),
  }
}

export function formatApiRequestPreview(value: unknown) {
  return JSON.stringify(value, null, 2)
}

export function normalizeApiDefinitionPayload(values: any) {
  return {
    ...values,
    enabled: values.enabled ? 1 : 0,
    includeContextPayload: values.includeContextPayload ? 1 : 0,
    receiptEnabled: values.receiptEnabled ? 1 : 0,
    requestSchema: JSON.stringify(values.requestSchema ?? []),
    receiptStatuses: JSON.stringify(values.receiptStatuses ?? []),
    receiptExpireMinutes: values.receiptEnabled
      ? (values.receiptExpireMinutes ?? 1440)
      : null,
  }
}

function sampleValue(type?: string) {
  switch (type) {
    case 'NUMBER':
      return 0
    case 'DATE':
      return '2026-01-01'
    case 'TEXT':
      return 'text'
    case 'STRING_PARAM':
    case 'STRING':
    default:
      return ''
  }
}
