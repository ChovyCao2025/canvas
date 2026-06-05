import { describe, expect, it } from 'vitest'
import {
  CANVAS_EDITOR_LAYOUT,
  getCanvasEditorRegionProps,
} from './editorLayout'

describe('editorLayout', () => {
  it('keeps fixed editor panel dimensions stable', () => {
    expect(CANVAS_EDITOR_LAYOUT.toolbarHeight).toBe(56)
    expect(CANVAS_EDITOR_LAYOUT.nodeLibraryWidth).toBe(320)
    expect(CANVAS_EDITOR_LAYOUT.inspectorWidth).toBe(280)
  })

  it('returns labelled region props for the main editor columns', () => {
    expect(getCanvasEditorRegionProps('nodeLibrary')).toMatchObject({
      role: 'region',
      'aria-label': '节点库',
      className: 'canvas-editor-region canvas-editor-node-library',
      style: expect.objectContaining({ width: 320, flexShrink: 0 }),
    })
    expect(getCanvasEditorRegionProps('graphCanvas')).toMatchObject({
      role: 'region',
      'aria-label': '画布区',
      className: 'canvas-editor-region canvas-editor-graph',
      style: expect.objectContaining({ flex: 1, minWidth: 0 }),
    })
    expect(getCanvasEditorRegionProps('inspector')).toMatchObject({
      role: 'region',
      'aria-label': '配置面板',
      className: 'canvas-editor-region canvas-editor-inspector',
      style: expect.objectContaining({ width: 280, flexShrink: 0 }),
    })
  })
})
