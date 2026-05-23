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
