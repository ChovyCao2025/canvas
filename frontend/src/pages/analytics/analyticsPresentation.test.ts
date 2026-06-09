import { describe, expect, it } from 'vitest'
import {
  exportStateText,
  formatEventCount,
  requireDateRangeMessage,
  timelineRowText,
  toEventCountRows,
} from './analyticsPresentation'

describe('analyticsPresentation', () => {
  it('requires both date range ends before querying', () => {
    expect(requireDateRangeMessage({ startDate: undefined, endDate: '2026-06-03' })).toBe('请选择开始和结束日期')
    expect(requireDateRangeMessage({ startDate: '2026-06-01', endDate: undefined })).toBe('请选择开始和结束日期')
    expect(requireDateRangeMessage({ startDate: '2026-06-01', endDate: '2026-06-03' })).toBeNull()
  })

  it('formats event counts and timeline rows', () => {
    expect(formatEventCount({ eventCode: 'OrderPaid', count: 12 })).toBe('OrderPaid: 12')
    expect(timelineRowText({ eventCode: 'OrderPaid', eventTime: '2026-06-02T10:00:00' }))
      .toBe('2026-06-02 10:00:00 - OrderPaid')
  })

  it('sorts event count rows by count descending', () => {
    expect(toEventCountRows([
      { eventCode: 'Open', count: 2 },
      { eventCode: 'OrderPaid', count: 12 },
    ])).toEqual([
      { key: 'OrderPaid', eventCode: 'OrderPaid', count: 12, label: 'OrderPaid: 12' },
      { key: 'Open', eventCode: 'Open', count: 2, label: 'Open: 2' },
    ])
  })

  it('formats export states used by the frontend queue model', () => {
    expect(exportStateText('QUEUED')).toBe('已排队')
    expect(exportStateText('RUNNING')).toBe('生成中')
    expect(exportStateText('DONE')).toBe('已完成')
    expect(exportStateText('FAILED')).toBe('失败')
    expect(exportStateText('UNAVAILABLE')).toBe('暂不可用')
  })
})
