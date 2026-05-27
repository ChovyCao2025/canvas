/**
 * 服务职责：人群定义与人群计算 API 类型和请求封装。
 *
 * 维护说明：该文件只描述前端 DTO 和 HTTP 调用，复杂表单组装在页面层完成。
 */
import type { R, PageResult } from '../types'
import http from './api'

/**
 * 人群定义。ruleJson 由前端规则编辑器生成，后端按 engineType 解释执行。
 */
export interface AudienceDefinition {
  /** 人群 ID（新建时为空）。 */
  id?: number

  /** 人群名称。 */
  name: string

  /** 人群描述。 */
  description?: string

  /** 规则 JSON（树结构）。 */
  ruleJson: string

  /** 规则引擎类型。 */
  engineType: 'AVIATOR' | 'QL'

  /** 数据源类型。 */
  dataSourceType: 'TAGGER_API' | 'JDBC'

  /** 数据源配置 JSON 字符串。 */
  dataSourceConfig?: string

  /** 计算策略。 */
  evaluationStrategy: 'ONLINE' | 'OFFLINE_BATCH' | 'HYBRID'

  /** 定时策略 cron（按策略可选）。 */
  cronExpression?: string

  /** 启用状态：1 启用，0 禁用。 */
  enabled: number

  /** 创建人。 */
  createdBy?: string

  /** 创建时间。 */
  createdAt?: string

  /** 更新时间。 */
  updatedAt?: string
}

/** 人群计算状态摘要。 */
export interface AudienceStat {
  /** 人群 ID。 */
  audienceId: number

  /** 估算人群规模。 */
  estimatedSize?: number

  /** bitmap 占用大小（KB）。 */
  bitmapSizeKb?: number

  /** 计算状态。 */
  status: 'PENDING' | 'COMPUTING' | 'READY' | 'FAILED'

  /** 最近一次计算完成时间。 */
  computedAt?: string

  /** 失败原因。 */
  errorMsg?: string
}

/** 创建人群计算任务后的响应。 */
export interface ComputeTaskResp {
  taskId: string
  status: 'QUEUED' | 'RUNNING' | 'SUCCEEDED' | 'FAILED' | 'CANCELED'
}

/** 人群定义、统计和计算任务接口集合。 */
export const audienceApi = {
  list: (page = 1, size = 20) =>
    http.get<R<PageResult<AudienceDefinition>>, R<PageResult<AudienceDefinition>>>('/canvas/audiences', { params: { page, size } }),
  listReady: () =>
    http.get<R<AudienceDefinition[]>, R<AudienceDefinition[]>>('/canvas/audiences/ready'),
  get: (id: number) =>
    http.get<R<AudienceDefinition>, R<AudienceDefinition>>(`/canvas/audiences/${id}`),
  create: (body: AudienceDefinition) =>
    http.post<R<AudienceDefinition>, R<AudienceDefinition>>('/canvas/audiences', body),
  update: (id: number, body: AudienceDefinition) =>
    http.put<R<void>, R<void>>(`/canvas/audiences/${id}`, body),
  delete: (id: number) =>
    http.delete<R<void>, R<void>>(`/canvas/audiences/${id}`),
  compute: (id: number) =>
    http.post<R<ComputeTaskResp>, R<ComputeTaskResp>>(`/canvas/audiences/${id}/compute`),
  stat: (id: number) =>
    http.get<R<AudienceStat>, R<AudienceStat>>(`/canvas/audiences/${id}/stat`),
}
