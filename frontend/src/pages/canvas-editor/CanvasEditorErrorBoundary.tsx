import type { ReactNode } from 'react'
import ErrorBoundary from '../../components/layout/ErrorBoundary'

interface CanvasEditorErrorBoundaryProps {
  children: ReactNode
  sectionName: string
  canvasId: number
  graphReloadKey?: string
  selectedNodeId?: string | null
}

export default function CanvasEditorErrorBoundary({
  children,
  sectionName,
  canvasId,
  graphReloadKey,
  selectedNodeId,
}: CanvasEditorErrorBoundaryProps) {
  const routeName = `画布编辑器${sectionName}`
  const resetKey = [
    canvasId,
    graphReloadKey ?? 'graph',
    selectedNodeId ?? 'none',
  ].join(':')

  return (
    <ErrorBoundary
      routeName={routeName}
      resetKey={resetKey}
      subTitle="该编辑区域渲染异常，重试后仍失败可切换节点或刷新画布。"
    >
      {children}
    </ErrorBoundary>
  )
}
