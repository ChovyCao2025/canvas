import { describe, expect, it } from 'vitest'
import { normalizeTagDefinitionPayload } from './tagConfigPayload'

describe('tagConfigPayload', () => {
  it('normalizes cdp tag definition fields', () => {
    expect(normalizeTagDefinitionPayload({
      name: '高价值',
      tagCode: 'high_value',
      tagType: 'offline',
      enabled: true,
      valueType: 'BOOLEAN',
      manualEnabled: true,
      defaultTtlDays: 30,
      category: '生命周期',
      owner: 'growth',
      writePolicy: 'UPSERT',
    })).toEqual({
      name: '高价值',
      tagCode: 'high_value',
      tagType: 'offline',
      enabled: 1,
      valueType: 'BOOLEAN',
      manualEnabled: 1,
      defaultTtlDays: 30,
      category: '生命周期',
      owner: 'growth',
      writePolicy: 'UPSERT',
    })
  })
})
