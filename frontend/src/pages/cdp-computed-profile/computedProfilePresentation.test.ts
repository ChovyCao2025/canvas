import { describe, expect, it } from 'vitest'
import {
  buildComputedProfilePayload,
  formatPreviewSummary,
  formatRunStatus,
  formatValueChange,
  profileAttributeStatusColor,
  profileAttributeStatusText,
} from './computedProfilePresentation'

describe('computedProfilePresentation', () => {
  it('formats definition and run statuses', () => {
    expect(profileAttributeStatusText('ACTIVE')).toBe('启用')
    expect(profileAttributeStatusText('PAUSED')).toBe('暂停')
    expect(profileAttributeStatusText('DRAFT')).toBe('草稿')
    expect(profileAttributeStatusColor('ACTIVE')).toBe('green')
    expect(formatRunStatus('DUPLICATED')).toBe('重复事件')
  })

  it('formats preview summary counts', () => {
    expect(formatPreviewSummary({ scannedCount: 10, matchedCount: 6, changedCount: 4, unchangedCount: 2 }))
      .toBe('扫描 10，命中 6，变更 4，未变 2')
  })

  it('formats old and new value changes', () => {
    expect(formatValueChange(null, 'VIP')).toBe('(空) -> VIP')
    expect(formatValueChange('Lead', 'VIP')).toBe('Lead -> VIP')
  })

  it('builds computed profile payload with parsed expression JSON', () => {
    expect(buildComputedProfilePayload({
      attrCode: ' lifecycle_stage ',
      displayName: ' 生命周期 ',
      valueType: 'STRING',
      computeType: 'RULE',
      refreshMode: 'MANUAL',
      expressionText: '{ "field": "paidCount", "op": ">=", "value": 2, "then": "VIP" }',
    })).toEqual({
      attrCode: 'lifecycle_stage',
      displayName: '生命周期',
      valueType: 'STRING',
      computeType: 'RULE',
      refreshMode: 'MANUAL',
      expressionJson: '{"field":"paidCount","op":">=","value":2,"then":"VIP"}',
    })
  })
})
