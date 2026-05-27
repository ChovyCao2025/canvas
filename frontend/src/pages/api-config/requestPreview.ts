/**
 * 页面职责：API 请求预览工具，把接口定义和参数映射转换为调试时可读的请求摘要。
 *
 * 维护说明：只做前端展示，不实际发送外部请求。
 */
/** API 请求参数 schema 的单个字段。 */
interface ApiRequestParam {
  /** 参数字段名。 */
  name: string

  /** 展示名，用于预览中说明参数含义。 */
  displayName: string

  /** 参数类型。 */
  type: string

  /** 是否必填。 */
  required: boolean
}

/** API 回执状态配置。 */
export interface ApiReceiptStatus {
  /** 业务侧回传的状态码。 */
  code: string

  /** 状态码对应的可读文案。 */
  label: string
}

/** 构造 API 请求预览所需的表单子集。 */
export interface BuildApiRequestPreviewInput {
  requestSchema?: ApiRequestParam[]
  includeContextPayload?: boolean
}

/** 构造 API 回执预览所需的表单子集。 */
export interface BuildApiReceiptPreviewInput {
  receiptEnabled?: boolean
  receiptStatuses?: ApiReceiptStatus[]
}

/** API 定义表单值，提交时会归一化为后端 DTO。 */
export interface ApiDefinitionFormValues {
  requestSchema?: ApiRequestParam[]
  enabled?: boolean
  includeContextPayload?: boolean
  receiptEnabled?: boolean
  receiptExpireMinutes?: number
  receiptStatuses?: ApiReceiptStatus[]
  [key: string]: unknown
}

/** 勾选“携带上下文”时展示的示例结构。 */
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

/** 构造 API_CALL 节点实际请求体的示例。 */
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

/** 格式化预览 JSON，统一缩进，便于复制给接口提供方确认。 */
export function formatApiRequestPreview(preview: unknown): string {
  return JSON.stringify(preview, null, 2)
}

/** 构造回执接口请求示例；未开启回执时返回空数组。 */
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

/** 把表单值转换为后端保存 API 定义需要的 payload。 */
export function normalizeApiDefinitionPayload(values: ApiDefinitionFormValues): Record<string, unknown> {
  return {
    ...values,
    // 表单 Switch 是 boolean，后端配置字段统一用 1/0。
    enabled: values.enabled ? 1 : 0,
    includeContextPayload: values.includeContextPayload ? 1 : 0,
    receiptEnabled: values.receiptEnabled ? 1 : 0,
    // 过期时间不填时使用 1 天，避免开启回执但没有有效期。
    receiptExpireMinutes: values.receiptExpireMinutes ?? 1440,
    // 后端按 JSON 字符串存储 schema/状态列表。
    receiptStatuses: JSON.stringify(values.receiptStatuses ?? []),
    requestSchema: JSON.stringify(values.requestSchema ?? []),
  }
}
