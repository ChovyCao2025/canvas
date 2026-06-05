import { useCallback, useState } from 'react'
import type { Node } from '@xyflow/react'
import { message } from 'antd'
import { canvasApi } from '../../services/api'
import { buildSaveGraphJson } from './graphSerialization'
import {
  editorApiErrorMessage,
  extractDryRunExecutionId,
  parseTestRunPayload,
} from './workflowApiAdapters'
import { buildDryRunSummary, type TraceStatus } from './dryRunVisualization'

interface UseCanvasTestRunWorkflowOptions {
  canvasId: number
  getNodes: () => Node[]
}

function isTraceStatus(value: unknown): value is TraceStatus {
  return value === 0 || value === 1 || value === 2 || value === 3
}

function dryRunSummaryText(payload: Record<string, unknown>) {
  const traces = Array.isArray(payload.traces)
    ? payload.traces
      .filter((trace): trace is { status: TraceStatus } => {
        return typeof trace === 'object' && trace !== null && isTraceStatus((trace as { status?: unknown }).status)
      })
    : []

  if (traces.length === 0) return ''

  const summary = buildDryRunSummary(traces)
  return `，成功 ${summary.success} / 失败 ${summary.failed} / 跳过 ${summary.skipped}`
}

/** Owns the dry-run modal state and submit workflow for the canvas editor. */
export function useCanvasTestRunWorkflow({
  canvasId,
  getNodes,
}: UseCanvasTestRunWorkflowOptions) {
  const [testModalOpen, setTestModalOpen] = useState(false)
  const [testUserId, setTestUserId] = useState('user_test_001')
  const [testPayload, setTestPayload] = useState('{}')
  const [testRunning, setTestRunning] = useState(false)

  const handleRunTest = useCallback(async () => {
    const parsedPayload = parseTestRunPayload(testPayload)
    if (!parsedPayload.ok) {
      message.error(parsedPayload.message)
      return
    }
    setTestRunning(true)
    try {
      const currentGraphJson = buildSaveGraphJson(getNodes())
      const res = await canvasApi.dryRun(canvasId, testUserId, parsedPayload.payload, currentGraphJson)
      const execId = extractDryRunExecutionId(res.data)
      message.success(`运行完成${execId ? `，执行ID: ${execId.slice(0, 8)}…` : ''}${dryRunSummaryText(res.data)}，可在「轨迹」面板查看结果`)
      setTestModalOpen(false)
    } catch (e) {
      message.error(editorApiErrorMessage(e, '触发失败'))
    } finally {
      setTestRunning(false)
    }
  }, [canvasId, getNodes, testPayload, testUserId])

  return {
    testModalOpen,
    setTestModalOpen,
    testUserId,
    setTestUserId,
    testPayload,
    setTestPayload,
    testRunning,
    handleRunTest,
  }
}
