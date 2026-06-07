import type { Edge, Node } from '@xyflow/react'
import { describe, expect, it, vi } from 'vitest'
import type { CanvasNodeData } from '../../types/canvas'
import { createEditorStore, selectEditorHistoryLabels } from './editorStore'

function node(id: string, nextNodeId?: string): Node<CanvasNodeData> {
  return {
    id,
    type: 'canvasNode',
    position: { x: 0, y: 0 },
    data: {
      nodeType: 'SEND_MESSAGE',
      name: id,
      category: '触达',
      bizConfig: { nextNodeId },
    },
  }
}

function edge(id: string): Edge {
  return {
    id,
    source: 'a',
    target: 'b',
  }
}

describe('editorStore', () => {
  it('tracks selected node, dirty state, and modal state', () => {
    const store = createEditorStore()

    store.getState().setSelectedNodeId('node-1')
    store.getState().setModalOpen('publish', true)
    store.getState().markDirty()

    expect(store.getState().selectedNodeId).toBe('node-1')
    expect(store.getState().modals.publish).toBe(true)
    expect(store.getState().dirty).toBe(true)
  })

  it('pushes immutable history and supports undo redo', () => {
    const store = createEditorStore()
    store.getState().replaceGraph([node('a', 'b')], [], 'load')
    store.getState().replaceGraph([node('a', 'c')], [edge('a->c')], 'edit')

    const labels = selectEditorHistoryLabels(store.getState())
    expect(labels.undoLabel).toBe('撤销：edit')
    expect(labels.redoLabel).toBe('没有可重做的操作')

    const undo = store.getState().undo()
    expect(undo?.nodes[0].data.bizConfig.nextNodeId).toBe('b')
    undo!.nodes[0].data.bizConfig.nextNodeId = 'mutated'

    const redo = store.getState().redo()
    expect(redo?.nodes[0].data.bizConfig.nextNodeId).toBe('c')
    expect(redo?.edges[0].id).toBe('a->c')
  })

  it('updates save state without clearing conflict data accidentally', () => {
    const store = createEditorStore()

    store.getState().setConflict({ serverVersion: 3, localVersion: 2 })
    store.getState().setSaveStatus('retrying', 1)

    expect(store.getState().saveStatus).toBe('retrying')
    expect(store.getState().saveAttempt).toBe(1)
    expect(store.getState().conflict?.serverVersion).toBe(3)
  })

  it('isolates selector subscriptions to the selected node slice', () => {
    const store = createEditorStore()
    const onSelectedNodeChange = vi.fn()
    const unsubscribe = store.subscribe(
      state => state.selectedNodeId,
      onSelectedNodeChange,
    )

    store.getState().setModalOpen('settings', true)
    store.getState().setSaveStatus('saving')
    store.getState().markDirty()

    expect(onSelectedNodeChange).not.toHaveBeenCalled()

    store.getState().setSelectedNodeId('node-2')

    expect(onSelectedNodeChange).toHaveBeenCalledTimes(1)
    unsubscribe()
  })
})
