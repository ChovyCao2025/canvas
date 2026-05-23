import { describe, expect, it } from 'vitest'
import { mergeCurrentValueOption, toSelectOptions } from './useSystemOptions'

describe('system option helpers', () => {
  it('normalizes system options for Select', () => {
    expect(toSelectOptions([{ optionKey: 'POST', label: 'POST', enabled: 1 } as any]))
      .toEqual([{ value: 'POST', label: 'POST' }])
  })

  it('adds disabled current value when missing from active options', () => {
    expect(mergeCurrentValueOption([{ value: 'POST', label: 'POST' }], 'GET'))
      .toEqual([
        { value: 'GET', label: '已禁用：GET' },
        { value: 'POST', label: 'POST' },
      ])
  })
})
