import { useCallback } from 'react'
import type { Node } from '@xyflow/react'
import { message, Modal } from 'antd'
import { canvasApi } from '../../services/api'
import { realCanvasNodes } from './graphSerialization'
import { blockingPrePublishMessages, canPublishFromChecks } from './prePublishChecks'
import { validateCanvasBeforePublish } from './publishValidation'
import { editorApiErrorMessage } from './workflowApiAdapters'

interface UseCanvasPublishWorkflowOptions {
  canvasId: number
  getNodes: () => Node[]
  saveDraft: (silent?: boolean) => Promise<unknown>
  onStatusChange: (status: 1) => void
}

/** Owns publish validation and submit workflow for the canvas editor. */
export function useCanvasPublishWorkflow({
  canvasId,
  getNodes,
  saveDraft,
  onStatusChange,
}: UseCanvasPublishWorkflowOptions) {
  return useCallback(async () => {
    const errors = validateCanvasBeforePublish(realCanvasNodes(getNodes()))
    if (errors.length > 0) {
      message.error({ content: errors.join('\n'), duration: 5 })
      return
    }
    try {
      await saveDraft(true)
      const checkResponse = await canvasApi.prePublishChecks(canvasId)
      if (!canPublishFromChecks(checkResponse.data)) {
        Modal.warning({
          title: '发布检查未通过',
          content: blockingPrePublishMessages(checkResponse.data).join('\n'),
        })
        return
      }
      await canvasApi.publish(canvasId)
      message.success('发布成功，已上线')
      onStatusChange(1)
    } catch (e) {
      message.error(editorApiErrorMessage(e, '发布失败'))
    }
  }, [canvasId, getNodes, onStatusChange, saveDraft])
}
