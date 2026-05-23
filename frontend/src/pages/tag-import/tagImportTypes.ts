import type { TagImportBatch, TagImportError, TagImportResult, TagImportSource } from '../../types'

export const DEFAULT_SOURCE_FIELD_MAPPING = '{"idType":"identity_type","idValue":"identity_value","tagCode":"tag_code","tagValue":"tag_value","tagTime":"tag_time"}'

export type TagImportSourceFormValues = {
  name: string
  url: string
  method: string
  recordsPath?: string
  fieldMapping: string
  enabled: boolean
}

export type BatchErrorState = Record<number, TagImportError[] | undefined>

export type BatchErrorLoadingState = Record<number, boolean | undefined>

export type ExcelUploadState = {
  uploading: boolean
  result?: TagImportResult
}

export type { TagImportBatch, TagImportError, TagImportResult, TagImportSource }
