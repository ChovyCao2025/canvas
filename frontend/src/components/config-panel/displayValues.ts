import type { StubOption } from '../../types'

type RawOption = Record<string, unknown>

export interface DisplayValueField {
  key: string
  type: string
  options?: RawOption[]
}

export function normalizeFieldOptions(
  field: DisplayValueField,
  options: Record<string, StubOption[]>,
) {
  return (options[field.key] ?? field.options ?? []).map((option: any) => ({
    label: String(option.label ?? option.option_name ?? option.name ?? option.displayName ?? option.value ?? option.key ?? ''),
    value: option.key ?? option.value,
  }))
}

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
  }

  return typeof value === 'string' ? value : String(value)
}
