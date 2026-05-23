import { describe, expect, it } from 'vitest'
import { toTagValueBody } from './tagValueEditor'

describe('toTagValueBody', () => {
  it('normalizes defaults for enabled, sortOrder, and source', () => {
    expect(toTagValueBody({ value: 'VIP', label: 'VIP', enabled: true })).toEqual({
      value: 'VIP',
      label: 'VIP',
      enabled: 1,
      sortOrder: 0,
      source: 'MANUAL',
    })
  })
})
