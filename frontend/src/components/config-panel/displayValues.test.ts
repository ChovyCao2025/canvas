/**
 * 测试职责：验证配置展示值从远程字典、schema 选项和原始值中的解析顺序。
 *
 * 维护说明：新增选项字段命名兼容时，应在 normalizeFieldOptions 用例中补覆盖。
 */
import { describe, expect, it } from 'vitest'
import type { StubOption } from '../../types'
import { normalizeFieldOptions, resolveDisplayValue } from './displayValues'

describe('displayValues', () => {
  it('resolves select labels from loaded options using StubOption shape', () => {
    const field = { key: 'audienceId', label: '圈选对象', type: 'select' }
    const options: Record<string, StubOption[]> = {
      audienceId: [{ key: 'audience_vip', label: '高价值用户' }],
    }

    expect(resolveDisplayValue(field, 'audience_vip', options)).toBe('高价值用户')
  })

  it('resolves select labels from schema field options', () => {
    const field = {
      key: 'mode',
      label: '模式',
      type: 'select',
      options: [{ value: 'audience', label: '人群圈选' }],
    }

    expect(resolveDisplayValue(field, 'audience', {})).toBe('人群圈选')
  })

  it('matches numeric and string ids by stringified equality', () => {
    const field = {
      key: 'cityId',
      label: '城市',
      type: 'select',
      options: [{ value: '101', label: '上海' }],
    }

    expect(resolveDisplayValue(field, 101, {})).toBe('上海')
  })

  it('falls back to the raw string when an option cannot be resolved', () => {
    const field = { key: 'mode', label: '模式', type: 'select' }

    expect(resolveDisplayValue(field, 'unknown-mode', {})).toBe('unknown-mode')
    expect(resolveDisplayValue(field, 404, {})).toBe('404')
  })

  it('returns undefined for undefined and null values', () => {
    const field = { key: 'mode', label: '模式', type: 'select' }

    expect(resolveDisplayValue(field, undefined, {})).toBeUndefined()
    expect(resolveDisplayValue(field, null, {})).toBeUndefined()
  })

  it('normalizes both loaded and schema option shapes into readable labels and values', () => {
    const field = {
      key: 'mode',
      label: '模式',
      type: 'radio',
      options: [{ value: 'field', label: '字段值' }],
    }
    const loadedOptions: Record<string, StubOption[]> = {
      mode: [{ key: 'loaded', label: '远程值' }],
    }

    expect(normalizeFieldOptions(field, loadedOptions)).toEqual([{ label: '远程值', value: 'loaded' }])
    expect(normalizeFieldOptions(field, {})).toEqual([{ label: '字段值', value: 'field' }])
  })
})
