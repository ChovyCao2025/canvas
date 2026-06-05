import { describe, expect, it } from 'vitest'
import { renderToString } from 'react-dom/server'

import VariablePicker, {
  filterVariableOptions,
  getKeyboardSelectedVariable,
  insertKeyboardSelection,
} from './VariablePicker'
import type { AvailableVariable } from './variableAvailability'

const variables: AvailableVariable[] = [
  {
    token: '{{profile.email}}',
    label: 'Email',
    source: 'profile',
    fieldKey: 'profile.email',
  },
  {
    token: '{{api_user.name}}',
    label: 'API User Name',
    source: 'upstream',
    fieldKey: 'api_user.name',
    nodeId: 'api',
    nodeLabel: 'User API',
  },
]

describe('VariablePicker', () => {
  it('renders variable labels and source tags', () => {
    const html = renderToString(<VariablePicker variables={variables} onInsert={() => undefined} />)

    expect(html).toContain('Email')
    expect(html).toContain('profile')
    expect(html).toContain('API User Name')
  })

  it('filters variables by label token source or node label', () => {
    expect(filterVariableOptions(variables, 'api').map(item => item.token)).toEqual(['{{api_user.name}}'])
    expect(filterVariableOptions(variables, 'profile').map(item => item.token)).toEqual(['{{profile.email}}'])
    expect(filterVariableOptions(variables, 'User API').map(item => item.token)).toEqual(['{{api_user.name}}'])
  })

  it('resolves keyboard selection by active index', () => {
    expect(getKeyboardSelectedVariable(variables, 'api', 0)?.token).toBe('{{api_user.name}}')
    expect(getKeyboardSelectedVariable(variables, '', 99)?.token).toBe('{{api_user.name}}')
  })

  it('calls insertion callback with selected token', () => {
    const inserted: string[] = []

    insertKeyboardSelection(variables, 'email', 0, token => inserted.push(token))

    expect(inserted).toEqual(['{{profile.email}}'])
  })
})
