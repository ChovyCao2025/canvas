/**
 * Component role: render and edit basic fields from plugin-provided schema metadata.
 */
import { useEffect, useMemo, useState, type ChangeEvent } from 'react'

import type {
  NodeConfigSchema,
  SchemaConfigField,
} from '../../plugins/pluginManifest'

export interface SchemaConfigPanelProps {
  schema: NodeConfigSchema
  value: Record<string, unknown>
  onChange: (next: Record<string, unknown>) => void
  readonly?: boolean
}

const SUPPORTED_FIELD_TYPES = new Set<SchemaConfigField['type']>([
  'text',
  'textarea',
  'number',
  'boolean',
  'select',
])

export function normalizeSchemaFields(schema: NodeConfigSchema): SchemaConfigField[] {
  return schema.fields.filter(field => SUPPORTED_FIELD_TYPES.has(field.type))
}

export function applySchemaDefaults(
  schema: NodeConfigSchema,
  value: Record<string, unknown>,
): Record<string, unknown> {
  return normalizeSchemaFields(schema).reduce<Record<string, unknown>>((next, field) => {
    if (next[field.key] === undefined && field.defaultValue !== undefined) {
      next[field.key] = field.defaultValue
    }
    return next
  }, { ...value })
}

export default function SchemaConfigPanel({
  schema,
  value,
  onChange,
  readonly,
}: SchemaConfigPanelProps) {
  const fields = useMemo(() => normalizeSchemaFields(schema), [schema])
  const defaultedValue = useMemo(() => applySchemaDefaults(schema, value), [schema, value])
  const [currentValue, setCurrentValue] = useState<Record<string, unknown>>(defaultedValue)

  useEffect(() => {
    setCurrentValue(defaultedValue)
  }, [defaultedValue])

  const patchValue = (key: string, nextValue: unknown) => {
    const next = {
      ...currentValue,
      [key]: nextValue,
    }
    setCurrentValue(next)
    onChange(next)
  }

  const renderField = (field: SchemaConfigField) => {
    const commonProps = {
      id: field.key,
      name: field.key,
      disabled: readonly,
      required: field.required,
      'aria-describedby': field.helpText ? `${field.key}-help` : undefined,
    }

    switch (field.type) {
      case 'textarea':
        return (
          <textarea
            {...commonProps}
            value={String(currentValue[field.key] ?? '')}
            onChange={(event: ChangeEvent<HTMLTextAreaElement>) =>
              patchValue(field.key, event.target.value)}
          />
        )
      case 'number':
        return (
          <input
            {...commonProps}
            type="number"
            value={Number(currentValue[field.key] ?? 0)}
            onChange={(event: ChangeEvent<HTMLInputElement>) =>
              patchValue(field.key, Number(event.target.value))}
          />
        )
      case 'boolean':
        return (
          <input
            {...commonProps}
            type="checkbox"
            checked={Boolean(currentValue[field.key])}
            onChange={(event: ChangeEvent<HTMLInputElement>) =>
              patchValue(field.key, event.target.checked)}
          />
        )
      case 'select':
        return (
          <select
            {...commonProps}
            value={String(currentValue[field.key] ?? '')}
            onChange={(event: ChangeEvent<HTMLSelectElement>) =>
              patchValue(field.key, event.target.value)}
          >
            {(field.options ?? []).map(option => (
              <option key={option.value} value={option.value}>
                {option.label}
              </option>
            ))}
          </select>
        )
      case 'text':
      default:
        return (
          <input
            {...commonProps}
            type="text"
            value={String(currentValue[field.key] ?? '')}
            onChange={(event: ChangeEvent<HTMLInputElement>) =>
              patchValue(field.key, event.target.value)}
          />
        )
    }
  }

  return (
    <div>
      {fields.map(field => (
        <div key={field.key}>
          <label htmlFor={field.key}>{field.label}</label>
          {renderField(field)}
          {field.helpText && <div id={`${field.key}-help`}>{field.helpText}</div>}
        </div>
      ))}
    </div>
  )
}
