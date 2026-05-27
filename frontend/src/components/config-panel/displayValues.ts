/**
 * 组件职责：配置面板展示值解析工具，将原始配置值转换为用户可读标签。
 *
 * 维护说明：用于摘要和只读视图，解析不到选项时回退展示原始值。
 */
import type { StubOption } from '../../types'

/** 后端 schema、系统字典或历史配置中可能出现的原始选项对象。 */
type RawOption = Record<string, unknown>

/** 可解析展示值的 schema 字段子集。 */
export interface DisplayValueField {
  /** 字段 key，对应节点配置里的属性名。 */
  key: string

  /** 控件类型，当前只对 select/radio 做选项 label 反查。 */
  type: string

  /** schema 内联选项。 */
  options?: RawOption[]

  /** 系统字典分类，用于从批量字典 options 中读取远程选项。 */
  optionCategory?: string
}

/** 统一 schema 内联选项和系统字典选项的 label/value 结构。 */
export function normalizeFieldOptions(
  field: DisplayValueField,
  options: Record<string, StubOption[]>,
) {
  return (options[field.key] ?? field.options ?? []).map((option: any) => ({
    // 兼容后端字典、节点 schema 和旧版本配置中不同的字段命名。
    label: String(option.label ?? option.option_name ?? option.name ?? option.displayName ?? option.value ?? option.key ?? ''),
    value: option.key ?? option.value,
  }))
}

/** 将原始配置值解析为可读文案，解析不到选项时保留原值以便排障。 */
export function resolveDisplayValue(
  field: DisplayValueField,
  value: unknown,
  options: Record<string, StubOption[]>,
) {
  if (value === undefined || value === null) return undefined

  if (field.type === 'select' || field.type === 'radio') {
    const normalizedOptions = normalizeFieldOptions(field, options)
    const matchedOption = normalizedOptions.find((option) => option.value === value)
    if (matchedOption?.label) return matchedOption.label

    const stringValue = String(value)
    const looseMatch = normalizedOptions.find((option) => String(option.value) === stringValue)
    if (looseMatch?.label) return looseMatch.label
    if (normalizedOptions.length > 0) return `已禁用：${stringValue}`
  }

  return typeof value === 'string' ? value : String(value)
}
