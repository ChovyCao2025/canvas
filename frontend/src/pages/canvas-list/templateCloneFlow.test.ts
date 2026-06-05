import { describe, expect, it } from 'vitest'
import { buildTemplateCategoryOptions, buildTemplateCloneSuccessMessage } from './templateCatalog'

describe('template catalog helpers', () => {
  it('builds sorted category options from templates', () => {
    expect(buildTemplateCategoryOptions([
      { id: 1, name: 'A', category: 'retention', useCount: 2 },
      { id: 2, name: 'B', category: 'activation', useCount: 1 },
      { id: 3, name: 'C', category: 'retention', useCount: 9 },
    ])).toEqual([
      { label: 'activation', value: 'activation' },
      { label: 'retention', value: 'retention' },
    ])
  })

  it('builds clone success copy with new canvas id', () => {
    expect(buildTemplateCloneSuccessMessage({ id: 88, name: '新客旅程' }))
      .toBe('已从模板创建「新客旅程」(ID: 88)')
  })
})
