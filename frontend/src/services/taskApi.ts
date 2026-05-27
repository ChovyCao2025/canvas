/**
 * 服务职责：异步任务 API 封装，用于查询导入、批处理等长耗时任务状态。
 *
 * 维护说明：任务状态字段直接映射后端状态机，前端只做展示和轮询。
 */
import type { R } from '../types'
import http from './api'

/** 异步任务状态枚举，和后端 AsyncTaskStatus 保持同名。 */
export type AsyncTaskStatus = 'QUEUED' | 'RUNNING' | 'SUCCEEDED' | 'FAILED' | 'CANCELED'

/** 后端异步任务列表与详情接口返回的统一任务模型。 */
export interface AsyncTask {
  /** 任务唯一 ID，用于详情轮询和通知跳转。 */
  taskId: string

  /** 任务技术类型，例如导入、批量打标、重算等。 */
  taskType: string

  /** 任务所属业务域，用于列表筛选和业务侧定位。 */
  bizType: string

  /** 业务对象 ID，例如人群 ID、导入批次 ID 或画布 ID。 */
  bizId: string

  /** 前端可直接展示的任务标题。 */
  title: string

  /** 当前任务状态，取值与后端状态机保持一致。 */
  status: AsyncTaskStatus

  /** 执行进度百分比，前端进度条直接消费。 */
  progress: number

  /** 成功或部分成功时的结果摘要。 */
  resultSummary?: string

  /** 失败状态下的错误说明。 */
  errorMsg?: string

  /** 任务开始执行时间。 */
  startedAt?: string

  /** 任务结束时间，失败/取消/成功时一般会返回。 */
  finishedAt?: string

  /** 任务创建时间。 */
  createdAt?: string

  /** 任务最近更新时间，用于判断轮询结果是否新鲜。 */
  updatedAt?: string
}

/** 异步任务读取接口，调用方通常配合轮询或通知中心使用。 */
export const taskApi = {
  /** 按任务 ID 查询单个任务的最新状态。 */
  get: (taskId: string) =>
    http.get<R<AsyncTask>, R<AsyncTask>>(`/canvas/async-tasks/${taskId}`),

  /** 按业务域、业务 ID、状态和分页参数查询任务列表。 */
  list: (params: { taskType?: string; bizType?: string; bizIds?: string; statuses?: string; page?: number; size?: number }) =>
    http.get<R<AsyncTask[]>, R<AsyncTask[]>>('/canvas/async-tasks', { params }),
}
