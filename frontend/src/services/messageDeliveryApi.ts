/**
 * 服务职责：消息投递 outbox、回执、对账和重放 API 封装。
 *
 * 维护说明：outbox 状态是消息投递的主状态机，回执只补充渠道侧送达、失败或转化证据。
 */
import type { PageResult, R } from '../types'
import http from './api'

/** 消息投递 outbox 记录，代表一次待发送或已发送的渠道消息。 */
export interface DeliveryOutbox {
  /** Outbox ID。 */
  id: number

  /** 租户 ID。 */
  tenantId?: number | null

  /** 消息发送记录 ID，关联画布消息节点的业务发送流水。 */
  messageSendRecordId: number

  /** 画布执行 ID。 */
  executionId: string

  /** 画布 ID。 */
  canvasId: number

  /** 用户 ID。 */
  userId: string

  /** 画布节点 ID。 */
  nodeId: string

  /** 投递渠道，例如 SMS / EMAIL / PUSH。 */
  channel: string

  /** 渠道供应商。 */
  provider: string

  /** 投递请求体 JSON。 */
  payloadJson?: string | null

  /** 幂等键，防止同一画布消息重复投递。 */
  idempotencyKey: string

  /** Outbox 状态，例如 PENDING / SENDING / SUCCESS / FAILED / DEAD。 */
  status: string

  /** 已尝试发送次数。 */
  attemptCount: number

  /** 下一次重试时间。 */
  nextRetryAt?: string | null

  /** 当前锁持有者，用于分布式投递 worker 并发控制。 */
  lockedBy?: string | null

  /** 锁定时间。 */
  lockedAt?: string | null

  /** 供应商消息 ID。 */
  providerMessageId?: string | null

  /** 供应商响应 JSON。 */
  providerResponseJson?: string | null

  /** 最近一次错误。 */
  lastError?: string | null

  /** 创建时间。 */
  createdAt: string

  /** 更新时间。 */
  updatedAt: string

  /** 是否由幂等命中识别为重复 outbox。 */
  duplicate?: boolean
}

/** 渠道回执日志，记录供应商异步通知或对账结果。 */
export interface DeliveryReceiptLog {
  /** 回执日志 ID。 */
  id?: number

  /** 租户 ID。 */
  tenantId?: number | null

  /** 关联 outbox ID。 */
  outboxId: number

  /** 渠道供应商。 */
  provider: string

  /** 供应商消息 ID。 */
  providerMessageId: string

  /** 回执类型，例如 DELIVERED / FAILED / OPENED。 */
  receiptType: string

  /** 原始回执 JSON。 */
  rawPayloadJson?: string | null

  /** 回执幂等键。 */
  idempotencyKey: string

  /** 供应商回执发生时间。 */
  receivedAt: string

  /** 系统记录时间。 */
  createdAt?: string | null
}

/** 投递 outbox 查询条件。 */
export interface DeliverySearchParams {
  /** 租户 ID。 */
  tenantId?: number

  /** 画布 ID。 */
  canvasId?: number

  /** 画布执行 ID。 */
  executionId?: string

  /** 用户 ID。 */
  userId?: string

  /** 投递渠道。 */
  channel?: string

  /** 渠道供应商。 */
  provider?: string

  /** Outbox 状态。 */
  status?: string

  /** 供应商消息 ID。 */
  providerMessageId?: string

  /** 页码。 */
  page?: number

  /** 每页条数。 */
  size?: number
}

/** 单条投递重放结果。 */
export interface DeliveryReplayResult {
  /** 被重放的 outbox ID。 */
  outboxId: number

  /** 重放后的状态。 */
  status: string
}

/** 投递对账结果。 */
export interface DeliveryReconcileResult {
  /** 对账后重新入队的 outbox 数量。 */
  requeued: number
}

/** 创建消息投递 API，允许测试注入 mock client。 */
export function createMessageDeliveryApi(client = http) {
  return {
    /** 分页查询投递 outbox，用于排查发送状态和重试队列。 */
    list: (params: DeliverySearchParams) =>
      client.get<R<PageResult<DeliveryOutbox>>, R<PageResult<DeliveryOutbox>>>('/message-deliveries', { params }),
    /** 查询单条 outbox 详情。 */
    detail: (id: number) =>
      client.get<R<DeliveryOutbox>, R<DeliveryOutbox>>(`/message-deliveries/${id}`),
    /** 查询单条 outbox 的渠道回执日志。 */
    receipts: (id: number) =>
      client.get<R<DeliveryReceiptLog[]>, R<DeliveryReceiptLog[]>>(`/message-deliveries/${id}/receipts`),
    /** 重放指定 outbox，通常用于失败或死信消息重新进入投递状态机。 */
    replay: (id: number) =>
      client.post<R<DeliveryReplayResult>, R<DeliveryReplayResult>>(`/message-deliveries/${id}/replay`),
    /** 触发投递对账，将卡住或需重试的记录重新入队。 */
    reconcile: () =>
      client.post<R<DeliveryReconcileResult>, R<DeliveryReconcileResult>>('/message-deliveries/reconcile'),
  }
}

export const messageDeliveryApi = createMessageDeliveryApi()
