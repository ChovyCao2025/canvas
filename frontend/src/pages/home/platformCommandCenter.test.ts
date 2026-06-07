import { describe, expect, it } from 'vitest'
import {
  blockedWorkstreamCount,
  groupWorkstreamsByStatus,
  workstreamStatusText,
  type PlatformWorkstreamStatus,
} from './platformCommandCenter'

describe('platformCommandCenter', () => {
  const rows: PlatformWorkstreamStatus[] = [
    {
      workstreamKey: 'platformization',
      displayName: 'Platformization',
      priority: 'P2',
      status: 'BLOCKED_CHILD_SPEC_REQUIRED',
      childSpecPath: null,
      summary: 'Extension points',
    },
    {
      workstreamKey: 'data-assets',
      displayName: 'Data Assets',
      priority: 'P2',
      status: 'READY_FOR_CHILD_EXECUTION',
      childSpecPath: 'docs/product-evolution/specs/p2-016-analytics-event-trace-schema-and-sink.md',
      summary: 'Events',
    },
  ]

  it('counts blocked workstreams', () => {
    expect(blockedWorkstreamCount(rows)).toBe(1)
  })

  it('groups workstreams by status', () => {
    expect(groupWorkstreamsByStatus(rows).BLOCKED_CHILD_SPEC_REQUIRED).toHaveLength(1)
    expect(groupWorkstreamsByStatus(rows).READY_FOR_CHILD_EXECUTION).toHaveLength(1)
  })

  it('formats stable status labels', () => {
    expect(workstreamStatusText('BLOCKED_CHILD_SPEC_REQUIRED')).toBe('Child spec required')
    expect(workstreamStatusText('READY_FOR_CHILD_EXECUTION')).toBe('Ready for child execution')
  })
})
