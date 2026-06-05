import { describe, expect, it } from 'vitest'
import { buildAttributionKpis, buildReceiptStatusRows } from './effectClosure'

describe('effectClosure helpers', () => {
  it('builds receipt status rows in fixed order', () => {
    expect(buildReceiptStatusRows({ sent: 3, failed: 1, skipped: 2 })).toEqual([
      { status: 'SENT', label: '已发送', count: 3 },
      { status: 'FAILED', label: '失败', count: 1 },
      { status: 'SKIPPED', label: '策略跳过', count: 2 },
    ])
  })

  it('builds attribution KPI copy', () => {
    expect(buildAttributionKpis({
      conversions: 4,
      conversionAmount: 99.5,
      attributedSends: 3,
      model: 'LAST_TOUCH',
    })).toContainEqual({ label: '转化金额', value: '99.50' })
  })
})
