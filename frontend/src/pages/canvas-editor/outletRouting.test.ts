import type { Edge } from '@xyflow/react'
import { describe, expect, it } from 'vitest'
import {
  clearEdgeRef,
  deriveEdges,
  patchBizConfig,
} from './outletRouting'

describe('outlet routing helpers', () => {
  const waitOutletSchema = JSON.stringify([
    { id: 'success', label: '继续', targetField: 'nextNodeId' },
    { id: 'timeout', label: '超时', targetField: 'timeoutNodeId' },
  ])

  it('writes dynamic handle targets to the configured target field', () => {
    const result = patchBizConfig({ successNodeId: 'legacy_branch' }, 'success', 'node_b', waitOutletSchema)

    expect(result.nextNodeId).toBe('node_b')
    expect(result.successNodeId).toBeUndefined()
  })

  it('keeps legacy handle target fields when no dynamic outlet schema is present', () => {
    const result = patchBizConfig({}, 'success', 'node_b')

    expect(result.successNodeId).toBe('node_b')
    expect(result.nextNodeId).toBeUndefined()
  })

  it('preserves indexed branch routing', () => {
    const result = patchBizConfig({
      branches: [
        { label: 'A', nextNodeId: undefined },
        { label: 'B', nextNodeId: undefined },
      ],
    }, 'branch-1', 'node_b', waitOutletSchema)

    expect(result.branches).toEqual([
      { label: 'A', nextNodeId: undefined },
      { label: 'B', nextNodeId: 'node_b' },
    ])
  })

  it('clears dynamic handle targets and shadowed legacy fields', () => {
    const edge: Edge = {
      id: 'wait_1->node_b::success',
      source: 'wait_1',
      target: 'node_b',
      sourceHandle: 'success',
    }

    const result = clearEdgeRef({
      nextNodeId: 'node_b',
      successNodeId: 'legacy_branch',
    }, edge, waitOutletSchema)

    expect(result.nextNodeId).toBeUndefined()
    expect(result.successNodeId).toBeUndefined()
  })

  it('derives dynamic handle ids from configured target fields', () => {
    expect(deriveEdges([
      {
        id: 'wait_1',
        type: 'WAIT',
        name: '等待',
        x: 0,
        y: 0,
        config: { nextNodeId: 'node_b' },
        outletSchema: waitOutletSchema,
      },
    ])).toEqual([
      {
        id: 'wait_1->node_b::success',
        source: 'wait_1',
        target: 'node_b',
        sourceHandle: 'success',
      },
    ])
  })

  it('does not derive shadowed legacy handles when dynamic schema declares the handle id', () => {
    expect(deriveEdges([
      {
        id: 'wait_1',
        type: 'WAIT',
        name: '等待',
        x: 0,
        y: 0,
        config: {
          nextNodeId: 'node_b',
          successNodeId: 'legacy_branch',
        },
        outletSchema: waitOutletSchema,
      },
    ])).toEqual([
      {
        id: 'wait_1->node_b::success',
        source: 'wait_1',
        target: 'node_b',
        sourceHandle: 'success',
      },
    ])
  })

  it('does not write custom dynamic handles without a target field', () => {
    const schema = JSON.stringify([{ id: 'continue', label: '继续' }])

    expect(patchBizConfig({}, 'continue', 'node_b', schema)).toEqual({})
  })
})
