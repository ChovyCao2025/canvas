import { describe, expect, it } from 'vitest'
import { buildDryRunSummary, buildTraceColorMap } from './dryRunVisualization'

describe('dryRunVisualization helpers', () => {
  it('maps trace status to node colors', () => {
    expect(buildTraceColorMap([
      { nodeId: 'n1', status: 1 },
      { nodeId: 'n2', status: 2 },
      { nodeId: 'n3', status: 3 },
    ])).toEqual({ n1: '#52c41a', n2: '#f5222d', n3: '#d9d9d9' })
  })

  it('summarizes traces by status', () => {
    expect(buildDryRunSummary([{ status: 1 }, { status: 1 }, { status: 2 }, { status: 0 }]))
      .toEqual({ running: 1, success: 2, failed: 1, skipped: 0 })
  })
})
