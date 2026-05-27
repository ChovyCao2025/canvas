/**
 * 测试职责：验证系统字典 Hook 的 option 转换和历史值补回规则。
 *
 * 维护说明：字典 DTO 字段命名或已禁用值展示规则变化时，要同步这些用例。
 */
import { describe, expect, it } from 'vitest'
import { mergeCurrentValueOption, toSelectOptions } from './useSystemOptions'

describe('system option helpers', () => {
  it('normalizes system options for Select', () => {
    expect(toSelectOptions([{ optionKey: 'POST', label: 'POST', enabled: 1 } as any]))
      .toEqual([{ value: 'POST', label: 'POST' }])
  })

  it('adds disabled current value when missing from active options', () => {
    expect(mergeCurrentValueOption([{ value: 'POST', label: 'POST' }], 'GET'))
      .toEqual([
        { value: 'GET', label: '已禁用：GET' },
        { value: 'POST', label: 'POST' },
      ])
  })
})
