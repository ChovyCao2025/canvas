/**
 * 测试职责：验证 api-input-params 控件按 schema 字段写回，避免 SEND_MQ 参数被写到 inputParams。
 */
import { describe, expect, it } from 'vitest'
import { resolveApiInputParamsFieldKey } from './apiInputParams'

describe('api input params helpers', () => {
  it('uses the schema field key as the form binding key', () => {
    expect(resolveApiInputParamsFieldKey('params')).toBe('params')
    expect(resolveApiInputParamsFieldKey('inputParams')).toBe('inputParams')
  })
})
