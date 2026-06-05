/**
 * Component responsibilities: searchable variable token picker for template-capable fields.
 *
 * Maintenance note: search and keyboard selection helpers are exported for Node-environment tests.
 */
import { useMemo, useState } from 'react'
import { Button, Input, Tag } from 'antd'

import type { AvailableVariable } from './variableAvailability'

export interface VariablePickerProps {
  variables: AvailableVariable[]
  onInsert: (token: string) => void
  disabled?: boolean
  maxVisible?: number
}

export function filterVariableOptions(variables: AvailableVariable[], query: string): AvailableVariable[] {
  const keyword = query.trim().toLowerCase()
  if (!keyword) return variables
  return variables.filter(variable => [
    variable.token,
    variable.label,
    variable.source,
    variable.nodeLabel,
  ].some(value => value?.toLowerCase().includes(keyword)))
}

export function getKeyboardSelectedVariable(
  variables: AvailableVariable[],
  query: string,
  activeIndex: number,
): AvailableVariable | undefined {
  const filtered = filterVariableOptions(variables, query)
  if (filtered.length === 0) return undefined
  return filtered[Math.max(0, Math.min(activeIndex, filtered.length - 1))]
}

export function insertKeyboardSelection(
  variables: AvailableVariable[],
  query: string,
  activeIndex: number,
  onInsert: (token: string) => void,
) {
  const selected = getKeyboardSelectedVariable(variables, query, activeIndex)
  if (selected) {
    onInsert(selected.token)
  }
}

export default function VariablePicker({
  variables,
  onInsert,
  disabled,
  maxVisible = 6,
}: VariablePickerProps) {
  const [query, setQuery] = useState('')
  const [activeIndex, setActiveIndex] = useState(0)
  const filtered = useMemo(() => filterVariableOptions(variables, query), [query, variables])
  const visible = filtered.slice(0, maxVisible)

  if (variables.length === 0) {
    return null
  }

  const handleInsert = (variable: AvailableVariable) => {
    if (!disabled) {
      onInsert(variable.token)
    }
  }

  return (
    <div style={{ display: 'grid', gap: 6 }}>
      <Input
        size="small"
        disabled={disabled}
        value={query}
        placeholder="搜索变量"
        onChange={event => {
          setQuery(event.target.value)
          setActiveIndex(0)
        }}
        onKeyDown={event => {
          if (event.key === 'ArrowDown') {
            event.preventDefault()
            setActiveIndex(index => Math.min(index + 1, Math.max(filtered.length - 1, 0)))
          } else if (event.key === 'ArrowUp') {
            event.preventDefault()
            setActiveIndex(index => Math.max(index - 1, 0))
          } else if (event.key === 'Enter') {
            event.preventDefault()
            if (!disabled) {
              insertKeyboardSelection(filtered, '', activeIndex, onInsert)
            }
          }
        }}
      />
      <div role="listbox" style={{ display: 'flex', flexWrap: 'wrap', gap: 6 }}>
        {visible.map((variable, index) => (
          <Button
            key={variable.token}
            size="small"
            type={index === activeIndex ? 'primary' : 'default'}
            disabled={disabled}
            onClick={() => handleInsert(variable)}
            style={{ maxWidth: '100%' }}
          >
            <span style={{ display: 'inline-flex', alignItems: 'center', gap: 4, maxWidth: '100%' }}>
              <span style={{ overflow: 'hidden', textOverflow: 'ellipsis' }}>{variable.label}</span>
              <Tag style={{ marginInlineEnd: 0 }}>{variable.source}</Tag>
            </span>
          </Button>
        ))}
      </div>
    </div>
  )
}
