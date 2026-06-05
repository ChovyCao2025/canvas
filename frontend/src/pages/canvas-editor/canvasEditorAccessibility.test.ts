import { describe, expect, it } from 'vitest'
import {
  buildCanvasEditorAccessibilityRows,
  getCanvasEditorFocusableControls,
} from './canvasEditorAccessibility'

describe('canvasEditorAccessibility', () => {
  it('documents keyboard and screen-reader coverage for core editor workflows', () => {
    const rows = buildCanvasEditorAccessibilityRows()

    expect(rows.map(row => row.workflow)).toEqual([
      'login',
      'home',
      'canvas list',
      'canvas editor',
      'api docs',
      'tenant admin',
      'notification bell',
    ])
    expect(rows.every(row => row.keyboardOrder && row.visibleFocus && row.screenReaderName)).toBe(true)
    expect(rows.find(row => row.workflow === 'canvas editor')?.focusTarget).toContain('node library')
  })

  it('names the focusable editor controls that need ARIA labels', () => {
    const controls = getCanvasEditorFocusableControls()

    expect(controls).toEqual(expect.arrayContaining([
      expect.objectContaining({ id: 'node-library-item', ariaName: '节点库条目' }),
      expect.objectContaining({ id: 'edge-insert-action', ariaName: '插入到连线' }),
      expect.objectContaining({ id: 'edge-delete-action', ariaName: '删除连线' }),
      expect.objectContaining({ id: 'notification-bell', ariaName: '打开消息中心' }),
    ]))
    expect(controls.every(control => control.focusable && control.ariaName.length > 0)).toBe(true)
  })
})
