import { describe, expect, it } from 'vitest'
import { getCanvasHistoryLabels } from './useCanvasHistoryState'
import type { EditorSnapshot } from './editorSnapshot'

function snapshot(actionName: string): EditorSnapshot {
  return { nodes: [], edges: [], actionName }
}

describe('canvas history state helpers', () => {
  it('builds disabled labels when no history exists', () => {
    expect(getCanvasHistoryLabels([], [])).toEqual({
      undoLabel: '没有可撤销的操作',
      redoLabel: '没有可重做的操作',
    })
  })

  it('uses latest history entry for undo and first future entry for redo', () => {
    expect(getCanvasHistoryLabels(
      [snapshot('添加节点'), snapshot('连线')],
      [snapshot('删除节点')],
    )).toEqual({
      undoLabel: '撤销：连线',
      redoLabel: '重做：删除节点',
    })
  })
})
