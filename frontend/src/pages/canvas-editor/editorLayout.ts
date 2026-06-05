import type { CSSProperties } from 'react'

export const CANVAS_EDITOR_LAYOUT = {
  toolbarHeight: 56,
  nodeLibraryWidth: 320,
  inspectorWidth: 280,
} as const

type CanvasEditorRegion = 'nodeLibrary' | 'graphCanvas' | 'inspector'

interface CanvasEditorRegionProps {
  role: 'region'
  'aria-label': string
  className: string
  style: CSSProperties
}

export function getCanvasEditorRegionProps(region: CanvasEditorRegion): CanvasEditorRegionProps {
  if (region === 'nodeLibrary') {
    return {
      role: 'region',
      'aria-label': '节点库',
      className: 'canvas-editor-region canvas-editor-node-library',
      style: {
        width: CANVAS_EDITOR_LAYOUT.nodeLibraryWidth,
        borderRight: '1px solid #f0f0f0',
        background: '#fafafa',
        flexShrink: 0,
      },
    }
  }
  if (region === 'inspector') {
    return {
      role: 'region',
      'aria-label': '配置面板',
      className: 'canvas-editor-region canvas-editor-inspector',
      style: {
        width: CANVAS_EDITOR_LAYOUT.inspectorWidth,
        borderLeft: '1px solid #f0f0f0',
        background: '#fff',
        flexShrink: 0,
        overflow: 'auto',
      },
    }
  }
  return {
    role: 'region',
    'aria-label': '画布区',
    className: 'canvas-editor-region canvas-editor-graph',
    style: {
      flex: 1,
      minWidth: 0,
    },
  }
}
