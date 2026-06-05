import { describe, expect, it } from 'vitest'
import {
  distributionPercent,
  distributionTotal,
  formatDateTime,
  formatProbability,
  orderedDistribution,
  riskBandColor,
  riskBandLabel,
  runStatusColor,
} from './aiPredictions'

describe('aiPredictions presentation', () => {
  it('formats risk bands status and probability', () => {
    expect(riskBandLabel('HIGH')).toBe('高风险')
    expect(riskBandColor('MEDIUM')).toBe('orange')
    expect(runStatusColor('SUCCESS')).toBe('green')
    expect(formatProbability('0.81234')).toBe('81.2%')
    expect(formatDateTime('2026-06-04T10:30:00')).toBe('2026-06-04 10:30:00')
  })

  it('orders distribution and computes percentages', () => {
    const ordered = orderedDistribution([
      { band: 'LOW', count: 2 },
      { band: 'HIGH', count: 3 },
    ])

    expect(ordered.map(item => item.band)).toEqual(['HIGH', 'MEDIUM', 'LOW'])
    expect(distributionTotal(ordered)).toBe(5)
    expect(distributionPercent(3, 5)).toBe(60)
  })
})
