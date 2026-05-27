/**
 * 测试职责：验证画布设置摘要、执行限制计数和高级区默认展开规则。
 *
 * 维护说明：画布级限制字段新增时，应同步 countExecutionLimitFields 和摘要断言。
 */
import { describe, expect, it } from 'vitest'
import {
  countExecutionLimitFields,
  getExecutionLimitsSummary,
  getTriggerTypeSummary,
  shouldExpandExecutionLimits,
} from './settingsPresentation'

describe('settingsPresentation', () => {
  it('returns realtime summary label', () => {
    expect(getTriggerTypeSummary()).toBe('当前为实时')
  })

  it('returns scheduled summary label', () => {
    expect(getTriggerTypeSummary('SCHEDULED')).toBe('当前为定时')
  })

  it('returns unknown summary label for unexpected trigger type values', () => {
    expect(getTriggerTypeSummary('MANUAL')).toBe('触发方式未知')
  })

  it('counts valid range as one configured item', () => {
    expect(countExecutionLimitFields({ validStart: '2026-05-23T00:00:00Z' })).toBe(1)
    expect(countExecutionLimitFields({ validEnd: '2026-05-24T00:00:00Z' })).toBe(1)
    expect(
      countExecutionLimitFields({
        validStart: '2026-05-23T00:00:00Z',
        validEnd: '2026-05-24T00:00:00Z',
      }),
    ).toBe(1)
  })

  it('counts individual numeric limits independently', () => {
    expect(
      countExecutionLimitFields({
        maxTotalExecutions: 10,
        perUserDailyLimit: 2,
        perUserTotalLimit: 5,
        cooldownSeconds: 30,
      }),
    ).toBe(4)
  })

  it('returns empty execution limits summary', () => {
    expect(getExecutionLimitsSummary({})).toBe('未设置限制')
  })

  it('returns configured execution limits summary by count', () => {
    expect(
      getExecutionLimitsSummary({
        validStart: '2026-05-23T00:00:00Z',
        maxTotalExecutions: 10,
        cooldownSeconds: 30,
      }),
    ).toBe('已配置 3 项')
  })

  it('keeps advanced section collapsed when nothing is configured', () => {
    expect(shouldExpandExecutionLimits({})).toBe(false)
  })

  it('expands advanced section when any execution limit is configured', () => {
    expect(shouldExpandExecutionLimits({ perUserDailyLimit: 1 })).toBe(true)
  })

  it('expands advanced section when only validStart is configured', () => {
    expect(
      shouldExpandExecutionLimits({
        validStart: '2026-05-23T00:00:00Z',
      }),
    ).toBe(true)
  })
})
