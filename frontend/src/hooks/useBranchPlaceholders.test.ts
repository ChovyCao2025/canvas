import { describe, expect, it, vi } from 'vitest'
import type { Edge, Node } from '@xyflow/react'
import type { CanvasNodeData } from '../components/canvas/constants'

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
