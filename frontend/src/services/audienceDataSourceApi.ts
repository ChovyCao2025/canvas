import type { R } from '../types'
import http from './api'

export interface AudienceDataSource {
  id?: number
  name: string
  description?: string
  url: string
  username: string
  password?: string
  driverClassName?: string
  enabled: number
  referenceCount?: number
}

export const audienceDataSourceApi = {
  list: () =>
    http.get<R<AudienceDataSource[]>, R<AudienceDataSource[]>>('/canvas/audience-data-sources'),
  create: (body: AudienceDataSource) =>
    http.post<R<AudienceDataSource>, R<AudienceDataSource>>('/canvas/audience-data-sources', body),
  update: (id: number, body: AudienceDataSource) =>
    http.put<R<AudienceDataSource>, R<AudienceDataSource>>(`/canvas/audience-data-sources/${id}`, body),
  delete: (id: number) =>
    http.delete<R<void>, R<void>>(`/canvas/audience-data-sources/${id}`),
}
