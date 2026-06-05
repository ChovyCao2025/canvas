import { describe, expect, it } from 'vitest'
import { cloneEditorSnapshot } from './editorSnapshot'

describe('editorSnapshot', () => {
  it('deep clones nodes, edges, and nested bizConfig', () => {
    const source = {
      nodes: [{
        id: 'n1',
        position: { x: 0, y: 0 },
        data: { nodeType: 'WAIT', name: '等待', category: '控制', bizConfig: { branches: [{ nextNodeId: 'n2' }] } },
      }],
      edges: [{ id: 'n1->n2', source: 'n1', target: 'n2' }],
      actionName: 'connect',
    }

    const cloned = cloneEditorSnapshot(source)
    source.nodes[0].data.bizConfig.branches[0].nextNodeId = 'mutated'

    expect(cloned.nodes[0].data.bizConfig.branches?.[0].nextNodeId).toBe('n2')
    expect(cloned.edges[0]).toEqual({ id: 'n1->n2', source: 'n1', target: 'n2' })
  })
})
