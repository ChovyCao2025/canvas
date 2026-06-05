import type { Node } from '@xyflow/react'
import type { CanvasNodeData } from '../../types/canvas'
import { PUBLISH_TRIGGER_NODE_TYPES } from '../../components/canvas/constants'

/** Validate graph-level requirements before publishing a canvas draft. */
export function validateCanvasBeforePublish(rfNodes: Node<CanvasNodeData>[]): string[] {
  const errors: string[] = []
  const hasTrigger = rfNodes.some(node => PUBLISH_TRIGGER_NODE_TYPES.has(node.data.nodeType))
  if (!hasTrigger) {
    errors.push('画布必须包含至少一个触发器节点（事件触发 / MQ 触发 / 定时触发 / API入口 / 受众）')
  }

  rfNodes.forEach(node => {
    const data = node.data
    const cfg = data.bizConfig
    switch (data.nodeType) {
      case 'EVENT_TRIGGER':
        if (!cfg.eventCode) errors.push(`节点「${data.name}」必须选择触发事件`)
        break
      case 'MQ_TRIGGER':
        if (!cfg.topicKey) errors.push(`节点「${data.name}」必须选择消息主题`)
        break
      case 'SCHEDULED_TRIGGER':
        if (!cfg.scheduleType) {
          errors.push(`节点「${data.name}」必须选择触发类型`)
        } else if (cfg.scheduleType === 'CRON' && !cfg.cronExpression) {
          errors.push(`节点「${data.name}」必须配置 Cron 表达式`)
        } else if (cfg.scheduleType === 'ONCE' && !cfg.triggerTime) {
          errors.push(`节点「${data.name}」必须配置触发时间`)
        }
        if (!cfg.nextNodeId) {
          errors.push(`节点「${data.name}」必须连接人群筛选节点`)
        } else {
          const nextNode = rfNodes.find(candidate => candidate.id === cfg.nextNodeId)
          const nextData = nextNode?.data
          if (nextData?.nodeType !== 'TAGGER' || nextData.bizConfig?.mode !== 'audience') {
            errors.push(`节点「${data.name}」只负责定时，下游必须先连接「Tagger 标签」并选择人群筛选`)
          }
        }
        break
      case 'THRESHOLD': {
        if (!cfg.thresholdMode) {
          errors.push(`节点「${data.name}」必须配置触发条件`)
          break
        }
        const needsN = cfg.thresholdMode === 'min_success' || cfg.thresholdMode === 'min_done'
        if (needsN && !cfg.threshold) errors.push(`节点「${data.name}」必须填写阈值 N`)
        if (!cfg.successNodeId) errors.push(`节点「${data.name}」未配置"达到阈值"分支（连线到 success handle）`)
        if (!cfg.failNodeId) errors.push(`节点「${data.name}」未配置"未达阈值"分支（连线到 fail handle）`)
        break
      }
      case 'AGGREGATE':
        if (!cfg.evaluateMode) {
          errors.push(`节点「${data.name}」必须配置评估方式`)
          break
        }
        if (cfg.evaluateMode === 'count' && !cfg.minCount) errors.push(`节点「${data.name}」必须填写最少成功数`)
        if (cfg.evaluateMode === 'rate' && cfg.minRate == null) errors.push(`节点「${data.name}」必须填写最低成功率`)
        if (cfg.evaluateMode === 'script' && !cfg.evaluateScript) errors.push(`节点「${data.name}」必须填写评估脚本`)
        if (!cfg.successNodeId) errors.push(`节点「${data.name}」未配置"条件满足"分支（连线到 success handle）`)
        if (!cfg.failNodeId) errors.push(`节点「${data.name}」未配置"条件不满足"分支（连线到 fail handle）`)
        break
      case 'IF_CONDITION':
        if (!cfg.successNodeId) errors.push(`节点「${data.name}」未配置成功分支（连线到 success handle）`)
        if (!cfg.failNodeId) errors.push(`节点「${data.name}」未配置失败分支（连线到 fail handle）`)
        break
      case 'SPLIT':
        if (!cfg.branches?.length) {
          errors.push(`节点「${data.name}」至少需要配置一个分支`)
          break
        }
        cfg.branches.forEach((branch, index) => {
          if (!branch.nextNodeId) errors.push(`节点「${data.name}」第 ${index + 1} 个分支未连线`)
          if (branch.weight == null) errors.push(`节点「${data.name}」第 ${index + 1} 个分支必须配置权重`)
        })
        break
      case 'GROOVY':
        if (!cfg.code) errors.push(`节点「${data.name}」Groovy 脚本不能为空`)
        break
      case 'SEND_MESSAGE':
        if (!cfg.channel) errors.push(`节点「${data.name}」必须选择消息渠道`)
        break
      case 'COMMIT_ACTION':
        if (!cfg.actionType) {
          errors.push(`节点「${data.name}」必须选择动作类型`)
        } else if (cfg.actionType === 'ISSUE_COUPON' && !cfg.couponTypeKey) {
          errors.push(`节点「${data.name}」选择发券动作时必须选择券类型`)
        } else if (cfg.actionType === 'POINTS' && cfg.points == null) {
          errors.push(`节点「${data.name}」选择积分动作时必须配置积分`)
        }
        break
    }
  })
  return errors
}
