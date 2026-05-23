import type { R, PageResult } from '../types'
import http from './api'

export interface AudienceDefinition {
  id?: number
  name: string
  description?: string
  ruleJson: string
  engineType: 'AVIATOR' | 'QL'
  dataSourceType: 'TAGGER_API' | 'JDBC'
  dataSourceId?: number
  dataSourceConfig?: string
  evaluationStrategy: 'ONLINE' | 'OFFLINE_BATCH' | 'HYBRID'
  cronExpression?: string
  enabled: number
  createdBy?: string
  createdAt?: string
  updatedAt?: string
}

export interface AudienceStat {
  audienceId: number
  estimatedSize?: number
  bitmapSizeKb?: number
  status: 'PENDING' | 'COMPUTING' | 'READY' | 'FAILED'
  computedAt?: string
  errorMsg?: string
}

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
    http.post<R<void>, R<void>>(`/canvas/audiences/${id}/compute`),
  stat: (id: number) =>
    http.get<R<AudienceStat>, R<AudienceStat>>(`/canvas/audiences/${id}/stat`),
}
