import { describe, expect, it } from 'vitest'
import { getOutletHandles, getOutletTargetField, hasOutletSchema, parseOutletSchema } from './outletSchema'

describe('outlet schema', () => {
  it('parses dynamic outlet handles from registry json', () => {
    const schema = JSON.stringify([
      { id: 'success', label: '通过', color: '#52c41a', targetField: 'nextNodeId' },
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

  it('resolves explicit target fields before legacy handle defaults', () => {
    const schema = JSON.stringify([
      { id: 'success', label: '继续', targetField: 'nextNodeId' },
    ])

    expect(getOutletTargetField('success', schema)).toBe('nextNodeId')
    expect(getOutletTargetField('success')).toBe('successNodeId')
  })

  it('does not render custom dynamic handles without target fields', () => {
    expect(parseOutletSchema(JSON.stringify([
      { id: 'continue', label: '继续' },
      { id: 'timeout', label: '超时' },
    ]))).toEqual([
      { id: 'timeout', label: '超时', color: '#1677ff' },
    ])
  })

  it('does not fall back to legacy handles when a dynamic schema is present but not routeable', () => {
    const schema = JSON.stringify([{ id: 'continue', label: '继续' }])

    expect(hasOutletSchema(schema)).toBe(true)
    expect(getOutletHandles({
      nodeType: 'IF_CONDITION',
      bizConfig: {},
      outletSchema: schema,
    })).toEqual([])
  })
})
