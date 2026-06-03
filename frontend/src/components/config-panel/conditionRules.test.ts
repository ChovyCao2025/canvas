/**
 * 测试职责：验证条件规则列表字段名的兜底和 schema key 绑定行为。
 *
 * 维护说明：ConfigPanel 中条件控件字段名变更时，要保证旧 schema 仍可落到 rules。
 */
import { describe, expect, it } from 'vitest'
import { getConditionRuleListFieldKey } from './index'

describe('condition rule list helpers', () => {
  it('binds generic condition lists to rules', () => {
    expect(getConditionRuleListFieldKey('rules')).toBe('rules')
  })

  it('binds validation rule lists to the schema field key', () => {
    expect(getConditionRuleListFieldKey('validateRules')).toBe('validateRules')
  })
})
