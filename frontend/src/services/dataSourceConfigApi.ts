/**
 * 服务职责：JDBC 数据源配置 API 封装，供人群编辑等页面选择表和列。
 *
 * 维护说明：表结构元数据只用于前端配置辅助，不直接参与画布运行时执行。
 */
import type { PageResult, R } from '../types'
import http from './api'

/** JDBC 数据源配置，供人群规则等页面选择表和字段。 */
export interface DataSourceConfig {
  /** 数据源主键，新建时由后端生成。 */
  id?: number

  /** 数据源展示名称。 */
  name: string

  /** 数据源类型；当前前端只开放 JDBC。 */
  type: 'JDBC'

  /** JDBC 连接地址。 */
  url: string

  /** 登录用户名。 */
  username: string

  /** 登录密码；后端按 write-only 处理，列表接口不会返回明文。 */
  password?: string

  /** JDBC 驱动类名，部分数据库需要显式指定。 */
  driverClassName?: string

  /** 备注说明。 */
  description?: string

  /** 启用状态：1 启用，0 禁用。 */
  enabled: number

  /** 创建人。 */
  createdBy?: string

  /** 创建时间。 */
  createdAt?: string

  /** 更新时间。 */
  updatedAt?: string
}

export interface DataSourceConfigPayload extends Omit<DataSourceConfig, 'id' | 'createdAt' | 'updatedAt'> {
  password: string
}

/** 数据源表结构元数据，供配置表名和用户 ID 列时下拉选择。 */
export interface DataSourceTableMeta {
  /** 表名。 */
  name: string

  /** 表字段列表。 */
  columns: string[]
}

/** 数据源配置接口集合，管理端页面和人群编辑页共用。 */
export const dataSourceConfigApi = {
  /** 分页查询数据源配置。 */
  list: (params: { page?: number; size?: number; type?: string; enabled?: number } = {}) =>
    http.get<R<PageResult<DataSourceConfig>>, R<PageResult<DataSourceConfig>>>('/canvas/data-sources', { params }),

  /** 查询某个数据源下可用表和列。 */
  listTables: (id: number) =>
    http.get<R<DataSourceTableMeta[]>, R<DataSourceTableMeta[]>>(`/canvas/data-sources/${id}/tables`),

  /** 新建数据源配置。 */
  create: (body: DataSourceConfigPayload) =>
    http.post<R<DataSourceConfig>, R<DataSourceConfig>>('/canvas/data-sources', body),

  /** 更新数据源配置。 */
  update: (id: number, body: DataSourceConfigPayload) =>
    http.put<R<void>, R<void>>(`/canvas/data-sources/${id}`, body),

  /** 删除数据源配置；已有业务引用时由后端决定是否允许删除。 */
  delete: (id: number) =>
    http.delete<R<void>, R<void>>(`/canvas/data-sources/${id}`),
}
