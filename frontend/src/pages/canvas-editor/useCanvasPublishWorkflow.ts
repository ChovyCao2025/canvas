import { useCallback } from 'react'
import type { Node } from '@xyflow/react'
import { message, Modal } from 'antd'
import { canvasApi } from '../../services/api'
import { realCanvasNodes } from './graphSerialization'
import { shouldSubmitPublishReview } from './canvasPublishApproval'
import { blockingPrePublishMessages, canPublishFromChecks } from './prePublishChecks'
import { validateCanvasBeforePublish } from './publishValidation'
import { editorApiErrorMessage } from './workflowApiAdapters'

/** 发布流程 hook 入参。 */
interface UseCanvasPublishWorkflowOptions {
  /** 当前画布 ID。 */
  canvasId: number

  /** 获取 React Flow 当前节点的函数。 */
  getNodes: () => Node[]

  /** 发布前保存草稿的函数。 */
  saveDraft: (silent?: boolean) => Promise<unknown>

  /** 发布成功后通知外层刷新画布状态。 */
  onStatusChange: (status: 1) => void
}

/** 负责画布发布前校验、审批提交和正式发布的完整流程。 */
export function useCanvasPublishWorkflow({
  canvasId,
  getNodes,
  saveDraft,
  onStatusChange,
}: UseCanvasPublishWorkflowOptions) {
  return useCallback(async () => {
    // 先做前端图结构校验，避免明显不完整流程进入后端检查。
    const errors = validateCanvasBeforePublish(realCanvasNodes(getNodes()))
    if (errors.length > 0) {
      message.error({ content: errors.join('\n'), duration: 5 })
      return
    }
    try {
      // 发布前强制静默保存，确保后端检查和发布使用最新草稿。
      await saveDraft(true)
      const checkResponse = await canvasApi.prePublishChecks(canvasId)
      if (!canPublishFromChecks(checkResponse.data)) {
        Modal.warning({
          title: '发布检查未通过',
          content: blockingPrePublishMessages(checkResponse.data).join('\n'),
        })
        return
      }
      // 有审批策略时只提交评审，不直接上线。
      const approvalStatus = await canvasApi.approvalStatus(canvasId)
      if (shouldSubmitPublishReview(approvalStatus.data)) {
        await canvasApi.submitReview(canvasId, { reason: '发布前审批' })
        message.success('已提交发布审批')
        return
      }
      // 无审批阻断时直接发布并更新外层状态。
      await canvasApi.publish(canvasId)
      message.success('发布成功，已上线')
      onStatusChange(1)
    } catch (e) {
      message.error(editorApiErrorMessage(e, '发布失败'))
    }
  }, [canvasId, getNodes, onStatusChange, saveDraft])
}
