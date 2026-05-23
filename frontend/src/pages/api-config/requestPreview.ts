interface ApiRequestParam {
  name: string
  displayName: string
  type: string
  required: boolean
}

export interface ApiReceiptStatus {
  code: string
  label: string
}

export interface BuildApiRequestPreviewInput {
  requestSchema?: ApiRequestParam[]
  includeContextPayload?: boolean
}

export interface BuildApiReceiptPreviewInput {
  receiptEnabled?: boolean
  receiptStatuses?: ApiReceiptStatus[]
}

export interface ApiDefinitionFormValues {
  requestSchema?: ApiRequestParam[]
  enabled?: boolean
  includeContextPayload?: boolean
  receiptEnabled?: boolean
  receiptExpireMinutes?: number
  receiptStatuses?: ApiReceiptStatus[]
  [key: string]: unknown
}

const CONTEXT_PREVIEW = {
  user_profile: {
    target_type: 'OPEN_ID',
    target_id: '1917810',
    customer_id: '1917810',
  },
  callback_params: {
    webhook_id: '',
    send_time: '1625037472000',
    nodeId: '节点Id',
    instanceId: '实例Id',
    batchId: '执行动作批次Id，可做批次幂等ID',
    actionId: '执行动作实例Id，可做单条幂等ID',
    customerId: '用户Id，customerId',
  },
  process_info: {
    processInstanceId: '新版：旅程周期中，每个用户的旅程实例ID',
    processInstanceStartTime: '新版：旅程周期中，每个用户的旅程实例开始时间，时间戳格式',
    processNodeInstanceId: '新版：旅程节点实例ID（每次不同）',
    processNodeInstanceStartTime: '新版：旅程周期中，每个用户的旅程的节点实例开始时间，时间戳格式',
    groupName: 'nodeId:nodeName:groupResult(node.result),groupName(node.resultExt)',
  },
}

export function buildApiRequestPreview(input: BuildApiRequestPreviewInput): unknown[] {
  const params = Object.fromEntries(
    (input.requestSchema ?? [])
      .filter(param => param.name?.trim())
      .map(param => [param.name.trim(), param.displayName?.trim() || param.name.trim()]),
  )

  const item = input.includeContextPayload
    ? {
        user_profile: CONTEXT_PREVIEW.user_profile,
        params,
        callback_params: CONTEXT_PREVIEW.callback_params,
        process_info: CONTEXT_PREVIEW.process_info,
      }
    : { params }

  return [item]
}

export function formatApiRequestPreview(preview: unknown): string {
  return JSON.stringify(preview, null, 2)
}

export function buildApiReceiptPreview(input: BuildApiReceiptPreviewInput): unknown[] {
  if (!input.receiptEnabled) return []
  const status = input.receiptStatuses?.find(item => item.code?.trim())?.code.trim() || '200'
  return [
    {
      msg_id: '业务侧唯一ID',
      status,
      cst_id: '用户Id，customerId',
      send_time: '回执发送时间，时间戳格式',
      callback_params: CONTEXT_PREVIEW.callback_params,
    },
  ]
}

export function normalizeApiDefinitionPayload(values: ApiDefinitionFormValues): Record<string, unknown> {
  return {
    ...values,
    enabled: values.enabled ? 1 : 0,
    includeContextPayload: values.includeContextPayload ? 1 : 0,
    receiptEnabled: values.receiptEnabled ? 1 : 0,
    receiptExpireMinutes: values.receiptExpireMinutes ?? 1440,
    receiptStatuses: JSON.stringify(values.receiptStatuses ?? []),
    requestSchema: JSON.stringify(values.requestSchema ?? []),
  }
}
