/**
 * 服务职责：CDP 事件接入运营端 API 封装。
 *
 * 维护说明：写入密钥、事件属性发现和 Webhook 订阅都属于事件接入链路配置，页面层只负责表单输入。
 */
import type { R } from '../types'
import http from './api'

/** 创建 CDP 事件写入密钥的表单。 */
export interface WriteKeyCreateForm {
  /** 密钥名称，用于运营端区分不同接入方。 */
  name: string

  /** 接入平台，例如 WEB / APP / SERVER，缺省按 WEB 处理。 */
  platform?: string

  /** 单密钥 QPS 限流，保护事件接入服务。 */
  rateLimitQps?: number

  /** 单日事件写入配额，空值表示不限制或使用后端默认值。 */
  dailyQuota?: number | null

  /** 密钥用途说明。 */
  description?: string
}

/** 写入密钥列表行，隐藏完整 writeKey，仅展示前缀和状态。 */
export interface WriteKeyRow {
  /** 密钥记录 ID。 */
  id: number

  /** 密钥名称。 */
  name: string

  /** 密钥前缀，用于排查请求来源但不泄露完整密钥。 */
  keyPrefix: string

  /** 接入平台。 */
  platform: string

  /** 密钥状态，例如 ACTIVE / DISABLED。 */
  status: string

  /** 当前 QPS 限流。 */
  rateLimitQps?: number

  /** 当前日配额。 */
  dailyQuota?: number | null

  /** 运营备注。 */
  description?: string | null

  /** 创建时间。 */
  createdAt?: string | null

  /** 最近更新时间。 */
  updatedAt?: string | null
}

/** 创建写入密钥成功后的明文返回，只应在创建后展示一次。 */
export interface WriteKeyCreateResp {
  /** 密钥记录 ID。 */
  id: number

  /** 密钥名称。 */
  name: string

  /** 完整写入密钥，调用方需立即保存。 */
  writeKey: string

  /** 密钥前缀。 */
  keyPrefix: string

  /** 接入平台。 */
  platform: string

  /** 生效的 QPS 限流。 */
  rateLimitQps: number

  /** 生效的日配额。 */
  dailyQuota?: number | null
}

/** CDP 从真实事件中自动发现的属性，供运营审核入模。 */
export interface DiscoveredAttribute {
  /** 属性发现记录 ID。 */
  id: number

  /** 事件编码，例如 form.submit 或 order.paid。 */
  eventCode: string

  /** 事件属性名。 */
  attrName: string

  /** 推断出的属性类型。 */
  attrType: string

  /** 审核状态：待审核、已通过或已拒绝。 */
  status: string

  /** 最近采样值，帮助判断字段含义。 */
  sampleValue?: string | null

  /** 首次发现时间。 */
  firstSeenAt?: string | null

  /** 最近一次出现时间。 */
  lastSeenAt?: string | null
}

/** Webhook 订阅表单，事件类型在页面以换行或逗号输入。 */
export interface WebhookSubscriptionForm {
  /** 订阅名称。 */
  name: string

  /** 接收 CDP 事件回调的业务地址。 */
  callbackUrl: string

  /** 原始事件类型文本，提交前会拆分去重。 */
  eventTypesText: string

  /** 最大投递尝试次数，非法值回退为 3。 */
  maxAttempts?: number
}

/** Webhook 订阅提交体，已完成事件类型拆分和默认值兜底。 */
export interface WebhookSubscriptionPayload {
  /** 订阅名称。 */
  name: string

  /** 回调地址。 */
  callbackUrl: string

  /** 要订阅的事件类型集合。 */
  eventTypes: string[]

  /** 最大投递尝试次数。 */
  maxAttempts: number
}

/** Webhook 订阅列表行。 */
export interface WebhookSubscriptionRow {
  /** 订阅 ID。 */
  id: number

  /** 订阅名称。 */
  name: string

  /** 回调地址。 */
  callbackUrl: string

  /** 签名密钥前缀，完整密钥只在轮换时返回。 */
  secretPrefix: string

  /** 订阅的事件类型集合。 */
  eventTypes: string[]

  /** 订阅状态，例如 ACTIVE / PAUSED / DISABLED。 */
  status: string

  /** 最大投递尝试次数。 */
  maxAttempts: number

  /** 创建时间。 */
  createdAt?: string | null

  /** 最近更新时间。 */
  updatedAt?: string | null
}

/** Webhook 投递流水，用于排查事件通知是否送达。 */
export interface WebhookDeliveryRow {
  /** 流水 ID。 */
  id: number

  /** 投递唯一 ID。 */
  deliveryId: string

  /** 事件类型。 */
  eventType: string

  /** 当前尝试次数。 */
  attempt: number

  /** 回调服务返回的 HTTP 状态码。 */
  httpStatus?: number | null

  /** 投递状态，例如 SUCCESS / RETRYING / FAILED。 */
  status: string

  /** 下一次重试时间。 */
  nextRetryAt?: string | null

  /** 最近一次错误信息。 */
  errorMessage?: string | null

  /** 终止原因，例如超过最大重试次数。 */
  terminalReason?: string | null

  /** 创建时间。 */
  createdAt?: string | null
}

/** 轮换 Webhook 签名密钥后的明文返回。 */
export interface WebhookRotateSecretResp {
  /** 订阅 ID。 */
  subscriptionId: number

  /** 新签名密钥，调用方需立即保存。 */
  secret: string

  /** 新签名密钥前缀。 */
  secretPrefix: string
}

/** 将写入密钥表单归一化成后端契约，避免空平台和限流值直接透传。 */
export function buildCreateWriteKeyPayload(form: WriteKeyCreateForm) {
  return {
    name: form.name.trim(),
    platform: form.platform || 'WEB',
    rateLimitQps: form.rateLimitQps || 100,
    dailyQuota: form.dailyQuota ?? null,
    description: form.description || '',
  }
}

/** 将 Webhook 表单转换为提交体，并对事件类型做拆分、去空和去重。 */
export function buildWebhookSubscriptionPayload(form: WebhookSubscriptionForm): WebhookSubscriptionPayload {
  // 运营可用换行或逗号录入多个事件类型，这里统一成后端需要的数组。
  const eventTypes = Array.from(new Set(form.eventTypesText
    .split(/[\n,]/)
    .map(value => value.trim())
    .filter(Boolean)))
  return {
    name: form.name.trim(),
    callbackUrl: form.callbackUrl.trim(),
    eventTypes,
    maxAttempts: form.maxAttempts && form.maxAttempts > 0 ? form.maxAttempts : 3,
  }
}

const ATTRIBUTE_STATUS_ORDER: Record<string, number> = {
  PENDING_REVIEW: 0,
  APPROVED: 1,
  REJECTED: 2,
}

/** 发现属性按审核优先级、事件编码和属性名稳定排序，便于运营批量处理。 */
export function normalizeDiscoveredAttributeRows(rows: DiscoveredAttribute[]) {
  return [...rows].sort((a, b) => {
    // 待审核属性排在最前，已处理项靠后，减少人工审核遗漏。
    const statusOrder = (ATTRIBUTE_STATUS_ORDER[a.status] ?? 99) - (ATTRIBUTE_STATUS_ORDER[b.status] ?? 99)
    if (statusOrder !== 0) return statusOrder
    const eventOrder = a.eventCode.localeCompare(b.eventCode)
    if (eventOrder !== 0) return eventOrder
    return a.attrName.localeCompare(b.attrName)
  })
}

/** 复制写入密钥列表行的可展示字段，避免页面误依赖潜在敏感字段。 */
export function safeWriteKeyRows(rows: WriteKeyRow[]) {
  return rows.map(row => ({
    id: row.id,
    name: row.name,
    keyPrefix: row.keyPrefix,
    platform: row.platform,
    status: row.status,
    rateLimitQps: row.rateLimitQps,
    dailyQuota: row.dailyQuota,
    description: row.description,
    createdAt: row.createdAt,
    updatedAt: row.updatedAt,
  }))
}

/** 创建 CDP 事件接入 API，允许测试注入 mock client。 */
export function createCdpEventApi(client = http) {
  return {
    /** 查询所有写入密钥。 */
    listWriteKeys: () =>
      client.get<R<WriteKeyRow[]>, R<WriteKeyRow[]>>('/cdp/write-keys'),
    /** 创建写入密钥，并返回一次性可见的完整 writeKey。 */
    createWriteKey: (form: WriteKeyCreateForm) =>
      client.post<R<WriteKeyCreateResp>, R<WriteKeyCreateResp>>('/cdp/write-keys', buildCreateWriteKeyPayload(form)),
    /** 停用写入密钥，阻止继续接收该来源事件。 */
    disableWriteKey: (id: number) =>
      client.delete<R<void>, R<void>>(`/cdp/write-keys/${id}`),
    /** 查询自动发现的事件属性，可按审核状态筛选。 */
    listDiscoveredAttributes: (status?: string) =>
      status
        ? client.get<R<DiscoveredAttribute[]>, R<DiscoveredAttribute[]>>(
          '/canvas/event-attributes/discovered',
          { params: { status } },
        )
        : client.get<R<DiscoveredAttribute[]>, R<DiscoveredAttribute[]>>('/canvas/event-attributes/discovered'),
    /** 查询 Webhook 订阅配置。 */
    listWebhookSubscriptions: () =>
      client.get<R<WebhookSubscriptionRow[]>, R<WebhookSubscriptionRow[]>>('/cdp/webhooks'),
    /** 创建 Webhook 订阅。 */
    createWebhookSubscription: (form: WebhookSubscriptionForm) =>
      client.post<R<WebhookSubscriptionRow>, R<WebhookSubscriptionRow>>(
        '/cdp/webhooks',
        buildWebhookSubscriptionPayload(form),
      ),
    /** 更新 Webhook 订阅，事件类型和重试次数在提交前归一化。 */
    updateWebhookSubscription: (id: number, form: WebhookSubscriptionForm) =>
      client.put<R<WebhookSubscriptionRow>, R<WebhookSubscriptionRow>>(
        `/cdp/webhooks/${id}`,
        buildWebhookSubscriptionPayload(form),
      ),
    /** 暂停订阅，保留配置但停止后续投递。 */
    pauseWebhookSubscription: (id: number) =>
      client.put<R<void>, R<void>>(`/cdp/webhooks/${id}/pause`),
    /** 恢复已暂停订阅。 */
    resumeWebhookSubscription: (id: number) =>
      client.put<R<void>, R<void>>(`/cdp/webhooks/${id}/resume`),
    /** 禁用订阅，停止投递并从运营列表中视为不可用。 */
    disableWebhookSubscription: (id: number) =>
      client.delete<R<void>, R<void>>(`/cdp/webhooks/${id}`),
    /** 轮换订阅签名密钥，完整密钥只在响应中返回一次。 */
    rotateWebhookSecret: (id: number) =>
      client.post<R<WebhookRotateSecretResp>, R<WebhookRotateSecretResp>>(`/cdp/webhooks/${id}/rotate-secret`, {}),
    /** 发送一次测试投递，用于验证回调地址和签名处理。 */
    testWebhookDelivery: (id: number) =>
      client.post<R<void>, R<void>>(`/cdp/webhooks/${id}/test`, {}),
    /** 查询订阅的历史投递流水和重试结果。 */
    listWebhookDeliveries: (id: number) =>
      client.get<R<WebhookDeliveryRow[]>, R<WebhookDeliveryRow[]>>(`/cdp/webhooks/${id}/deliveries`),
  }
}

export const cdpEventApi = createCdpEventApi()
