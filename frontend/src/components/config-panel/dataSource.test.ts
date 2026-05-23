import { describe, expect, it } from 'vitest'
import { getDataSourceDependencies, resolveDataSourceTemplate } from './index'

describe('dataSource helpers', () => {
  it('resolves data source templates with encoded field values', () => {
    expect(
      resolveDataSourceTemplate('/meta/tagger-tag-values?tagCode={tagCodeKey}', {
        tagCodeKey: 'user_level',
      }),
    ).toBe('/meta/tagger-tag-values?tagCode=user_level')
  })

  it('returns null when any dependency is missing', () => {
    expect(
      resolveDataSourceTemplate('/meta/tagger-tag-values?tagCode={tagCodeKey}', {}),
    ).toBeNull()

    expect(
      resolveDataSourceTemplate('/meta/tagger-tag-values?tagCode={tagCodeKey}', {
        tagCodeKey: '   ',
      }),
    ).toBeNull()
  })

  it('returns ordered dependency keys from the data source template', () => {
    expect(
      getDataSourceDependencies('/meta/tagger-tag-values?tagCode={tagCodeKey}'),
    ).toEqual(['tagCodeKey'])
  })
})
