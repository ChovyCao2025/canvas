import type { TagDefinition, TagValueDefinition } from '../../types'

export type TagConfigRecord = TagDefinition
export type TagValueRecord = TagValueDefinition

export interface TagFormValues {
  name: string
  tagCode: string
  tagType: TagDefinition['tagType']
  valueType: TagDefinition['valueType']
  description?: string
  enabled: boolean
  manualEnabled?: boolean
  defaultTtlDays?: number | null
  category?: string
  owner?: string
  writePolicy?: string
}

export interface TagValueFormValues {
  value: string
  label: string
  sortOrder?: number
  source?: string
  enabled?: boolean
}
