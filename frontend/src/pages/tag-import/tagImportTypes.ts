/**
 * 页面职责：标签导入页面的本地表单类型定义。
 *
 * 维护说明：与后端导入源 DTO 分离，便于表单校验和显示脱敏字段。
 */
import type { TagImportBatch, TagImportError, TagImportResult, TagImportSource } from '../../types'

/** 标签拉取来源默认字段映射，左侧是平台字段，右侧是外部来源字段。 */
export const DEFAULT_SOURCE_FIELD_MAPPING = '{"idType":"identity_type","idValue":"identity_value","tagCode":"tag_code","tagValue":"tag_value","tagTime":"tag_time"}'

/** 标签导入来源表单值。 */
export type TagImportSourceFormValues = {
  name: string
  url: string
  method: string
  recordsPath?: string
  fieldMapping: string
  enabled: boolean
}

/** 批次失败明细缓存，按批次 ID 索引。 */
export type BatchErrorState = Record<number, TagImportError[] | undefined>

/** 批次失败明细加载态，按批次 ID 索引。 */
export type BatchErrorLoadingState = Record<number, boolean | undefined>

/** Excel 导入面板的上传状态。 */
export type ExcelUploadState = {
  uploading: boolean
  result?: TagImportResult
}

/** 重新导出标签导入相关后端 DTO，供导入页面子组件统一引用。 */
export type { TagImportBatch, TagImportError, TagImportResult, TagImportSource }
