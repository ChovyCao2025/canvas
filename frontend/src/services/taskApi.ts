import type { R } from '../types'
import http from './api'

export type AsyncTaskStatus = 'QUEUED' | 'RUNNING' | 'SUCCEEDED' | 'FAILED' | 'CANCELED'

export interface AsyncTask {
  taskId: string
  taskType: string
  bizType: string
  bizId: string
  title: string
  status: AsyncTaskStatus
  progress: number
  resultSummary?: string
  errorMsg?: string
  startedAt?: string
  finishedAt?: string
  createdAt?: string
  updatedAt?: string
}

export const taskApi = {
  list: (params: { taskType?: string; bizType?: string; bizIds?: string; statuses?: string; page?: number; size?: number }) =>
    http.get<R<AsyncTask[]>, R<AsyncTask[]>>('/canvas/async-tasks', { params }),
}
