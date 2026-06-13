/* @vitest-environment jsdom */
import { describe, expect, it } from 'vitest'
import { fireEvent, render, screen } from '@testing-library/react'

import SchemaConfigPanel, {
  applySchemaDefaults,
  normalizeSchemaFields,
} from './SchemaConfigPanel'
import type { NodeConfigSchema } from '../../plugins/pluginManifest'

const schema: NodeConfigSchema = {
  fields: [
    { key: 'title', label: 'Title', type: 'text', required: true },
    { key: 'limit', label: 'Limit', type: 'number', defaultValue: 5 },
    { key: 'enabled', label: 'Enabled', type: 'boolean', defaultValue: true },
    {
      key: 'mode',
      label: 'Mode',
      type: 'select',
      options: [
        { label: 'Automatic', value: 'auto' },
        { label: 'Manual', value: 'manual' },
      ],
      defaultValue: 'auto',
    },
  ],
}

describe('schemaConfigPanel surface', () => {
  it('normalizes supported basic field metadata', () => {
    expect(normalizeSchemaFields(schema).map(field => field.key)).toEqual([
      'title',
      'limit',
      'enabled',
      'mode',
    ])
  })

  it('applies schema defaults without overwriting existing values', () => {
    expect(applySchemaDefaults(schema, { title: 'Existing', mode: 'manual' })).toEqual({
      title: 'Existing',
      limit: 5,
      enabled: true,
      mode: 'manual',
    })
  })

  it('renders and edits basic schema field types', () => {
    const patches: Record<string, unknown>[] = []

    render(
      <SchemaConfigPanel
        schema={schema}
        value={{ title: 'Welcome' }}
        onChange={next => patches.push(next)}
      />,
    )

    expect(screen.getByLabelText('Title')).toHaveValue('Welcome')
    expect(screen.getByLabelText('Limit')).toHaveValue(5)
    expect(screen.getByLabelText('Enabled')).toBeChecked()
    expect(screen.getByLabelText('Mode')).toHaveValue('auto')

    fireEvent.change(screen.getByLabelText('Title'), { target: { value: 'Updated' } })
    fireEvent.change(screen.getByLabelText('Limit'), { target: { value: '8' } })
    fireEvent.click(screen.getByLabelText('Enabled'))
    fireEvent.change(screen.getByLabelText('Mode'), { target: { value: 'manual' } })

    expect(patches).toEqual([
      { title: 'Updated', limit: 5, enabled: true, mode: 'auto' },
      { title: 'Updated', limit: 8, enabled: true, mode: 'auto' },
      { title: 'Updated', limit: 8, enabled: false, mode: 'auto' },
      { title: 'Updated', limit: 8, enabled: false, mode: 'manual' },
    ])
  })
})
