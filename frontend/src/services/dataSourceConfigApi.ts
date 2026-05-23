import type { PageResult, R } from '../types'
import http from './api'

export interface DataSourceConfig {
  id?: number
  name: string
  type: 'JDBC'
  url: string
  username: string
  password: string
  driverClassName?: string
  description?: string
  enabled: number
  createdBy?: string
  createdAt?: string
  updatedAt?: string
}

export interface DataSourceTableMeta {
  name: string
  columns: string[]
}

export const dataSourceConfigApi = {
  list: (params: { page?: number; size?: number; type?: string; enabled?: number } = {}) =>
    http.get<R<PageResult<DataSourceConfig>>, R<PageResult<DataSourceConfig>>>('/canvas/data-sources', { params }),
  listTables: (id: number) =>
    http.get<R<DataSourceTableMeta[]>, R<DataSourceTableMeta[]>>(`/canvas/data-sources/${id}/tables`),
  create: (body: DataSourceConfig) =>
    http.post<R<DataSourceConfig>, R<DataSourceConfig>>('/canvas/data-sources', body),
  update: (id: number, body: DataSourceConfig) =>
    http.put<R<void>, R<void>>(`/canvas/data-sources/${id}`, body),
  delete: (id: number) =>
    http.delete<R<void>, R<void>>(`/canvas/data-sources/${id}`),
}
