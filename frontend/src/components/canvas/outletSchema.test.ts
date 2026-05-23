import { describe, expect, it } from 'vitest'
import { getOutletHandles, parseOutletSchema } from './outletSchema'

describe('outlet schema', () => {
  it('parses dynamic outlet handles from registry json', () => {
    const schema = JSON.stringify([
      { id: 'success', label: '通过', color: '#52c41a' },
      { id: 'suppressed', label: '被抑制', color: '#f5222d' },
    ])

    expect(parseOutletSchema(schema)).toEqual([
      { id: 'success', label: '通过', color: '#52c41a' },
      { id: 'suppressed', label: '被抑制', color: '#f5222d' },
    ])
  })

  it('falls back to legacy IF handles', () => {
    expect(getOutletHandles({
      nodeType: 'IF_CONDITION',
      bizConfig: {},
      outletSchema: undefined,
    })).toEqual([
      { id: 'success', label: '条件成立', color: '#52c41a' },
      { id: 'else', label: '否则', color: '#8c8c8c' },
    ])
  })
})
