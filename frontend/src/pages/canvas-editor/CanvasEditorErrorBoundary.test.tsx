/* @vitest-environment jsdom */
import { render, screen } from '@testing-library/react'
import { afterEach, describe, expect, it, vi } from 'vitest'
import CanvasEditorErrorBoundary from './CanvasEditorErrorBoundary'

function Boom(): null {
  throw new Error('editor section failed')
}

describe('CanvasEditorErrorBoundary', () => {
  afterEach(() => {
    vi.restoreAllMocks()
  })

  it('renders a named local editor fallback when a section fails', () => {
    vi.spyOn(console, 'error').mockImplementation(() => {})

    render(
      <CanvasEditorErrorBoundary
        sectionName="配置面板"
        canvasId={12}
        graphReloadKey="graph-v1"
        selectedNodeId="node-a"
      >
        <Boom />
      </CanvasEditorErrorBoundary>,
    )

    expect(screen.getByRole('alert')).toHaveTextContent('画布编辑器配置面板加载失败')
    expect(screen.getByRole('button', { name: '重试画布编辑器配置面板' })).toBeInTheDocument()
  })

  it('resets when the selected node changes', () => {
    vi.spyOn(console, 'error').mockImplementation(() => {})
    let shouldThrow = true
    function Flaky() {
      if (shouldThrow) throw new Error('panel failed')
      return <div>配置面板已恢复</div>
    }

    const { rerender } = render(
      <CanvasEditorErrorBoundary
        sectionName="配置面板"
        canvasId={12}
        graphReloadKey="graph-v1"
        selectedNodeId="node-a"
      >
        <Flaky />
      </CanvasEditorErrorBoundary>,
    )

    expect(screen.getByRole('alert')).toBeInTheDocument()
    shouldThrow = false
    rerender(
      <CanvasEditorErrorBoundary
        sectionName="配置面板"
        canvasId={12}
        graphReloadKey="graph-v1"
        selectedNodeId="node-b"
      >
        <Flaky />
      </CanvasEditorErrorBoundary>,
    )

    expect(screen.getByText('配置面板已恢复')).toBeInTheDocument()
  })
})
