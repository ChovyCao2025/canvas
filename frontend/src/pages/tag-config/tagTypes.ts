/**
 * 页面职责：标签配置页表单类型定义。
 *
 * 维护说明：这些类型把后端标签 DTO 与 Ant Design 表单值隔离开。
 */
import type { TagDefinition, TagValueDefinition } from '../../types'

/** 标签定义列表行，直接复用后端标签定义 DTO。 */
export type TagConfigRecord = TagDefinition

/** 标签值列表行，直接复用后端标签值 DTO。 */
export type TagValueRecord = TagValueDefinition

/** 标签定义表单值。 */
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

/** 标签值表单值。 */
export interface TagValueFormValues {
  value: string
  label: string
  sortOrder?: number
  source?: string
  enabled?: boolean
}
