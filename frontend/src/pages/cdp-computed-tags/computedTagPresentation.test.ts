import { describe, expect, it } from 'vitest'
import {
  buildComputedTagPayload,
  formatComputedTagRunSummary,
  formatLineageImpact,
  statusText,
  validateFallbackImpact,
} from './computedTagPresentation'

describe('computedTagPresentation', () => {
  it('formats status and run counters', () => {
    expect(statusText('ACTIVE')).toBe('启用')
    expect(formatComputedTagRunSummary({
      scannedCount: 5,
      matchedCount: 3,
      updatedCount: 2,
      skippedCount: 1,
      failedCount: 0,
    })).toBe('扫描 5，命中 3，更新 2，跳过 1，失败 0')
  })

  it('formats lineage impacts for operator review', () => {
    expect(formatLineageImpact({
      objectType: 'AUDIENCE',
      objectId: '10',
      objectName: 'VIP campaign audience',
      referencePath: 'audience_definition.rule_json',
    })).toBe('AUDIENCE #10 VIP campaign audience - audience_definition.rule_json')
  })

  it('blocks incompatible type changes with lineage impacts', () => {
    expect(validateFallbackImpact({
      allowed: false,
      reason: 'INCOMPATIBLE_TYPE_CHANGE',
      impacts: [{ objectType: 'COMPUTED_TAG', objectId: 'high_value_user', referencePath: 'cdp_computed_tag_dependency.depends_on_tag_code' }],
    })).toEqual({ disabled: true, reason: 'INCOMPATIBLE_TYPE_CHANGE (1 impact)' })
  })

  it('builds payload with dependencies and compact expression JSON', () => {
    expect(buildComputedTagPayload({
      tagCode: ' vip_likely ',
      displayName: ' VIP likely ',
      valueType: 'BOOLEAN',
      computeType: 'RULE',
      refreshMode: 'MANUAL',
      expressionText: '{ "field": "paidCount", "op": ">=", "value": 2 }',
      dependenciesText: 'paid_user\nhigh_value',
    })).toEqual({
      tagCode: 'vip_likely',
      displayName: 'VIP likely',
      valueType: 'BOOLEAN',
      computeType: 'RULE',
      refreshMode: 'MANUAL',
      expressionJson: '{"field":"paidCount","op":">=","value":2}',
      dependencies: ['paid_user', 'high_value'],
    })
  })
})
