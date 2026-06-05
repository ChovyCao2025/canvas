import { useState } from 'react'
import { message, Modal } from 'antd'
import type { CanvasVersion } from '../../types'
import { canvasApi } from '../../services/api'
import { extractCanvasVersions } from './workflowApiAdapters'

interface UseCanvasVersionHistoryWorkflowOptions {
  canvasId: number
}

/** Owns version history drawer state, loading, and revert actions. */
export function useCanvasVersionHistoryWorkflow({
  canvasId,
}: UseCanvasVersionHistoryWorkflowOptions) {
  const [historyOpen, setHistoryOpen] = useState(false)
  const [versionList, setVersionList] = useState<CanvasVersion[]>([])
  const [historyLoading, setHistoryLoading] = useState(false)

  const openHistory = async () => {
    setHistoryOpen(true)
    setHistoryLoading(true)
    try {
      const res = await canvasApi.getVersions(canvasId)
      setVersionList(extractCanvasVersions(res.data))
    } finally {
      setHistoryLoading(false)
    }
  }

  const handleRevert = (versionId: number) => {
    Modal.confirm({
      title: '回退到此版本',
      content: '将以该版本内容覆盖当前草稿，不影响线上版本。确认继续？',
      okText: '确认回退',
      okType: 'danger',
      cancelText: '取消',
      onOk: async () => {
        await canvasApi.revert(canvasId, versionId)
        message.success('已回退到选定版本，即将刷新画布')
        setTimeout(() => window.location.reload(), 800)
      },
    })
  }

  const handleDiff = async (versionId: number) => {
    const currentDraft = versionList[0]
    if (!currentDraft || currentDraft.id === versionId) return
    const res = await canvasApi.diffVersions(canvasId, versionId, currentDraft.id)
    const data = res.data as {
      summary?: {
        addedCount?: number
        removedCount?: number
        modifiedCount?: number
      }
      addedCount?: number
      removedCount?: number
      modifiedCount?: number
    }
    const summary = data.summary ?? data
    Modal.info({
      title: '版本差异',
      content: `新增节点：${summary.addedCount ?? 0}，删除节点：${summary.removedCount ?? 0}，修改节点：${summary.modifiedCount ?? 0}`,
      okText: '知道了',
    })
  }

  return {
    historyOpen,
    setHistoryOpen,
    versionList,
    historyLoading,
    openHistory,
    handleRevert,
    handleDiff,
  }
}
