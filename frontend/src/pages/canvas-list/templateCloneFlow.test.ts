import { describe, expect, it } from 'vitest'
import {
  buildTemplateCategoryOptions,
  buildTemplateCloneSuccessMessage,
  getPlaygroundGoldenPath,
} from './templateCatalog'

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

  it('exposes the deterministic playground golden path from the welcome template', () => {
    const goldenPath = getPlaygroundGoldenPath()

    expect(goldenPath.template.key).toBe('new-user-welcome')
    expect(goldenPath.template.title).toBe('新用户欢迎旅程')
    expect(goldenPath.template.requiredPlugins).toEqual([
      'canvas-plugin-webhook',
      'canvas-plugin-coupon',
      'canvas-plugin-message',
    ])
    expect(goldenPath.template.samplePayload).toEqual({
      event: 'user.registered',
      user: { id: 'u_1001', lifecycleStage: 'new', phone: '+8613800000001' },
    })
    expect(goldenPath.template.expectedTrace.map(step => step.nodeId)).toEqual([
      'segment',
      'coupon',
      'message',
    ])
    expect(goldenPath.steps.map(step => step.id)).toEqual([
      'demo-compose-config',
      'template-import-draft',
      'dry-run-trace',
      'dsl-export-cli-validate',
      'mock-ai-risk-audit',
    ])
    expect(goldenPath.steps.find(step => step.id === 'dsl-export-cli-validate')?.command)
      .toBe('cd tools/canvas-cli && node src/index.mjs validate test/fixtures/valid-journey.json')
    expect(goldenPath.steps.find(step => step.id === 'mock-ai-risk-audit')?.safety)
      .toBe('mock-provider-preview-only')
    expect(goldenPath.publishBoundary).toBe('draft-preview-only')
  })
})
