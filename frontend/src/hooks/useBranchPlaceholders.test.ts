/**
 * 测试职责：验证分支占位节点的派生规则和 useMemo 依赖稳定性。
 *
 * 维护说明：占位节点不能写回真实图结构，相关拖拽/连线改动要补充这里的场景。
 */
import { describe, expect, it, vi } from 'vitest'
import type { Edge, Node } from '@xyflow/react'
import type { CanvasNodeData } from '../components/canvas/constants'

/** 捕获 useMemo 依赖数组，验证 Hook 不因无关渲染重复计算。 */
let latestDeps: unknown[] | undefined

vi.mock('react', async (importOriginal) => {
  const actual = await importOriginal<typeof import('react')>()
  return {
    ...actual,
    useMemo: <T>(factory: () => T, deps: unknown[]) => {
    latestDeps = deps
    return factory()
    },
  }
})

describe('useBranchPlaceholders', () => {
  it('builds placeholders from dynamic outlet schema handles', async () => {
    const { useBranchPlaceholders } = await import('./useBranchPlaceholders')
    const nodes: Node<CanvasNodeData>[] = [
      {
        id: 'wait_1',
        type: 'canvasNode',
        position: { x: 100, y: 100 },
        width: 200,
        height: 80,
        data: {
          nodeType: 'WAIT',
          name: '等待',
          category: '流程控制',
          bizConfig: {},
          outletSchema: JSON.stringify([
            { id: 'success', label: '继续', color: '#52c41a' },
            { id: 'timeout', label: '超时', color: '#faad14' },
          ]),
        },
      },
    ]

    const result = useBranchPlaceholders(nodes, [], null)

    expect(result.nodes.map(node => node.data.handleId)).toEqual(['success', 'timeout'])
    expect(result.edges.map(edge => edge.sourceHandle)).toEqual(['success', 'timeout'])
  })

  it('recomputes when outlet schema changes', async () => {
    const { useBranchPlaceholders } = await import('./useBranchPlaceholders')
    const nodes: Node<CanvasNodeData>[] = [
      {
        id: 'wait_1',
        type: 'canvasNode',
        position: { x: 100, y: 100 },
        data: {
          nodeType: 'WAIT',
          name: '等待',
          category: '流程控制',
          bizConfig: {},
          outletSchema: JSON.stringify([{ id: 'success', label: '继续' }]),
        },
      },
    ]
    useBranchPlaceholders(nodes, [] as Edge[], null)

    expect(latestDeps).toContain(nodes)
  })
})
