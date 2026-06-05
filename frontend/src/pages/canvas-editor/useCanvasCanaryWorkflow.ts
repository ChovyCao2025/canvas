import { useState } from 'react'
import { message, Modal } from 'antd'
import { canvasApi } from '../../services/api'

interface UseCanvasCanaryWorkflowOptions {
  canvasId: number
  existingCanaryVersionId?: number
  existingCanaryPercent?: number
}

/** Owns canary modal state and canary version actions for the canvas editor. */
export function useCanvasCanaryWorkflow({
  canvasId,
  existingCanaryVersionId,
  existingCanaryPercent,
}: UseCanvasCanaryWorkflowOptions) {
  const [canaryModalOpen, setCanaryModalOpen] = useState(false)
  const [canaryPercent, setCanaryPercent] = useState(20)

  const handleStartCanary = async () => {
    try {
      const hasExisting = Boolean(existingCanaryVersionId)
      if (hasExisting) {
        const confirmed = await new Promise<boolean>(resolve =>
          Modal.confirm({
            title: '覆盖灰度版本',
            content: `当前已有灰度版本（${existingCanaryPercent}%），确认覆盖？`,
            okText: '确认覆盖',
            okButtonProps: { danger: true },
            cancelText: '取消',
            onOk: () => resolve(true),
            onCancel: () => resolve(false),
          })
        )
        if (!confirmed) return
      }
      await canvasApi.canary(canvasId, canaryPercent)
      message.success(`灰度发布成功，${canaryPercent}% 用户将收到新版本`)
      setCanaryModalOpen(false)
      window.location.reload()
    } catch {
      message.error('灰度发布失败，请稍后重试')
    }
  }

  const handlePromoteCanary = () => {
    Modal.confirm({
      title: '晋升灰度为全量',
      content: '灰度版本将成为正式版本，所有用户切换到新版本。确认晋升？',
      okText: '确认晋升',
      onOk: async () => {
        try {
          await canvasApi.promoteCanary(canvasId)
          message.success('已晋升为全量版本')
          window.location.reload()
        } catch {
          message.error('晋升灰度失败，请稍后重试')
        }
      },
    })
  }

  const handleRollbackCanary = () => {
    Modal.confirm({
      title: '回滚灰度',
      content: '灰度版本将被丢弃，所有用户恢复到正式版本。确认回滚？',
      okType: 'danger',
      okText: '确认回滚',
      onOk: async () => {
        try {
          await canvasApi.rollbackCanary(canvasId)
          message.warning('灰度已回滚')
          window.location.reload()
        } catch {
          message.error('回滚灰度失败，请稍后重试')
        }
      },
    })
  }

  return {
    canaryModalOpen,
    setCanaryModalOpen,
    canaryPercent,
    setCanaryPercent,
    handleStartCanary,
    handlePromoteCanary,
    handleRollbackCanary,
  }
}
