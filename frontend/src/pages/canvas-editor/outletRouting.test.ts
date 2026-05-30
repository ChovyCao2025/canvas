/**
 * 测试职责：验证 React Flow 边与后端 bizConfig 后继引用之间的双向转换。
 *
 * 维护说明：新增动态出口或集合型分支时，要补充 patch/clear/derive 三类用例。
 */
import type { Edge } from '@xyflow/react'
import { describe, expect, it } from 'vitest'
import {
  appendDirectCallBranch,
  clearEdgeRef,
  deriveEdges,
  mergeOutletEdge,
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

  it('creates missing branch slots when routing a legacy direct-call branch handle', () => {
    const result = patchBizConfig({}, 'branch-1', 'node_b')

    expect(result.branches).toEqual([
      { label: '分支 1', nextNodeId: undefined },
      { label: '分支 2', nextNodeId: 'node_b' },
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

  it('derives DIRECT_CALL fan-out branches as multiple default outlet edges', () => {
    expect(deriveEdges([
      {
        id: 'direct',
        type: 'DIRECT_CALL',
        name: 'API入口',
        x: 0,
        y: 0,
        config: {
          nextNodeId: 'api_a',
          branches: [
            { label: '渠道 A', nextNodeId: 'api_a' },
            { label: '渠道 B', nextNodeId: 'api_b' },
          ],
        },
      },
    ])).toEqual([
      {
        id: 'direct->api_a',
        source: 'direct',
        target: 'api_a',
        sourceHandle: 'default',
      },
      {
        id: 'direct->api_b',
        source: 'direct',
        target: 'api_b',
        sourceHandle: 'default',
      },
    ])
  })

  it('appends a DIRECT_CALL branch from a default outlet connection', () => {
    expect(appendDirectCallBranch({
      branches: [{ label: '查询用户', nextNodeId: 'api_user' }],
    }, 'api_order', '查询订单')).toEqual({
      branches: [
        { label: '查询用户', nextNodeId: 'api_user' },
        { label: '查询订单', nextNodeId: 'api_order' },
      ],
    })
  })

  it('converts an existing single nextNodeId when appending a fan-out branch', () => {
    expect(appendDirectCallBranch({
      nextNodeId: 'trigger_a',
    }, 'trigger_b', '入口 B')).toEqual({
      nextNodeId: undefined,
      branches: [
        { label: '分支 1', nextNodeId: 'trigger_a' },
        { label: '入口 B', nextNodeId: 'trigger_b' },
      ],
    })
  })

  it('removes the matching DIRECT_CALL branch when deleting a default outlet edge', () => {
    expect(clearEdgeRef({
      branches: [
        { label: '查询用户', nextNodeId: 'api_user' },
        { label: '查询订单', nextNodeId: 'api_order' },
      ],
    }, {
      id: 'direct->api_user',
      source: 'direct',
      target: 'api_user',
      sourceHandle: 'default',
    })).toEqual({
      branches: [
        { label: '查询订单', nextNodeId: 'api_order' },
      ],
    })
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

  it('derives START fan-out branches as multiple default outlet edges', () => {
    expect(deriveEdges([
      {
        id: 'start',
        type: 'START',
        name: '开始',
        x: 0,
        y: 0,
        config: {
          nextNodeId: 'trigger',
          branches: [
            { label: '历史分支 A', nextNodeId: 'api_a' },
            { label: '历史分支 B', nextNodeId: 'api_b' },
          ],
        },
      },
    ])).toEqual([
      {
        id: 'start->api_a',
        source: 'start',
        target: 'api_a',
        sourceHandle: 'default',
      },
      {
        id: 'start->api_b',
        source: 'start',
        target: 'api_b',
        sourceHandle: 'default',
      },
    ])
  })

  it('does not write custom dynamic handles without a target field', () => {
    const schema = JSON.stringify([{ id: 'continue', label: '继续' }])

    expect(patchBizConfig({}, 'continue', 'node_b', schema)).toEqual({})
  })

  it('preserves other branch handle edges from the same source when merging a new outlet edge', () => {
    const existing: Edge[] = [
      { id: 'direct->api_a::branch-0', source: 'direct', target: 'api_a', sourceHandle: 'branch-0' },
    ]

    expect(mergeOutletEdge(existing, {
      id: 'direct->api_b::branch-1',
      source: 'direct',
      target: 'api_b',
      sourceHandle: 'branch-1',
    })).toEqual([
      { id: 'direct->api_a::branch-0', source: 'direct', target: 'api_a', sourceHandle: 'branch-0' },
      { id: 'direct->api_b::branch-1', source: 'direct', target: 'api_b', sourceHandle: 'branch-1' },
    ])
  })
})
