import { describe, expect, it } from 'vitest'
import { isCdpAudienceSource, toQueryBuilderFields } from './cdpAudienceFields'

describe('cdpAudienceFields', () => {
  it('detects cdp data source types', () => {
    expect(isCdpAudienceSource('CDP_TAG')).toBe(true)
    expect(isCdpAudienceSource('CDP_PROFILE')).toBe(true)
    expect(isCdpAudienceSource('CDP_IDENTITY')).toBe(true)
    expect(isCdpAudienceSource('JDBC')).toBe(false)
  })

  it('maps source fields to query builder fields', () => {
    expect(toQueryBuilderFields([
      { name: 'high_value', label: '高价值用户', valueType: 'STRING' },
      { name: 'score', label: '分数', valueType: 'NUMBER' },
    ])).toEqual([
      { name: 'high_value', label: '高价值用户', value: 'high_value' },
      { name: 'score', label: '分数', value: 'score' },
    ])
  })
})
