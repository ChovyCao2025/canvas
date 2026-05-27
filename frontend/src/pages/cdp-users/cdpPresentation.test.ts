/**
 * 测试职责：验证 CDP 用户页打标 payload、执行状态和时间格式化规则。
 *
 * 维护说明：批量打标表单字段变化时，应先更新 payload 归一化测试。
 */
import { describe, expect, it } from 'vitest'
import { buildBatchTagPayload, buildTagWritePayload, formatDateTime, formatExecutionStatus } from './cdpPresentation'

describe('cdpPresentation', () => {
  it('builds tag payloads', () => {
    expect(buildTagWritePayload({ tagCode: 'vip', tagValue: 'true', reason: 'manual' })).toEqual({
      tagCode: 'vip',
      tagValue: 'true',
      reason: 'manual',
      sourceType: 'MANUAL',
    })

    expect(buildBatchTagPayload({
      operationType: 'BATCH_SET',
      tagCode: 'vip',
      tagValue: 'true',
      userIds: 'u1\nu2, u3',
      reason: 'batch',
      operator: 'admin',
    })).toEqual({
      operationType: 'BATCH_SET',
      tagCode: 'vip',
      tagValue: 'true',
      userIds: ['u1', 'u2', 'u3'],
      reason: 'batch',
      operator: 'admin',
    })
  })

  it('formats status and date time', () => {
    expect(formatExecutionStatus('SUCCESS')).toEqual({ label: '成功', color: 'success' })
    expect(formatDateTime('2026-05-23T10:11:12')).toBe('2026-05-23 10:11:12')
    expect(formatDateTime()).toBe('-')
  })
})
