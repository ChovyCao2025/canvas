import type { Node } from '@xyflow/react'
import { describe, expect, it } from 'vitest'
import type { BizConfig, CanvasNodeData } from '../../types/canvas'
import { validateCanvasBeforePublish } from './publishValidation'

describe('publish validation', () => {
  it('requires at least one trigger node', () => {
    expect(validateCanvasBeforePublish([
      node('groovy', 'GROOVY', '脚本', { code: 'return true' }),
    ])).toContain('画布必须包含至少一个触发器节点（事件触发 / MQ 触发 / 定时触发 / API入口 / 受众）')
  })

  it('validates direct event and mq trigger configuration', () => {
    const errors = validateCanvasBeforePublish([
      node('event', 'EVENT_TRIGGER', '注册事件', {}),
      node('mq', 'MQ_TRIGGER', '订单消息', {}),
    ])

    expect(errors).toContain('节点「注册事件」必须选择触发事件')
    expect(errors).toContain('节点「订单消息」必须选择消息主题')
  })

  it('requires scheduled trigger to point to an audience tagger node', () => {
    const errors = validateCanvasBeforePublish([
      node('scheduled', 'SCHEDULED_TRIGGER', '每日任务', {
        scheduleType: 'CRON',
        cronExpression: '0 0 9 * * ?',
        nextNodeId: 'api',
      }),
      node('api', 'API_CALL', '下游接口', {}),
    ])

    expect(errors).toContain('节点「每日任务」只负责定时，下游必须先连接「Tagger 标签」并选择人群筛选')
  })

  it('accepts a valid scheduled trigger audience chain', () => {
    expect(validateCanvasBeforePublish([
      node('scheduled', 'SCHEDULED_TRIGGER', '每日任务', {
        scheduleType: 'CRON',
        cronExpression: '0 0 9 * * ?',
        nextNodeId: 'audience',
      }),
      node('audience', 'TAGGER', '人群筛选', { mode: 'audience' }),
    ])).toEqual([])
  })

  it('validates split branches without any casts', () => {
    const errors = validateCanvasBeforePublish([
      node('direct', 'DIRECT_CALL', 'API入口', {}),
      node('split', 'SPLIT', '实验分流', {
        branches: [
          { label: 'A', weight: 50 },
          { label: 'B', nextNodeId: 'send' },
        ],
      }),
    ])

    expect(errors).toContain('节点「实验分流」第 1 个分支未连线')
    expect(errors).toContain('节点「实验分流」第 2 个分支必须配置权重')
  })
})

function node(id: string, nodeType: string, name: string, bizConfig: BizConfig): Node<CanvasNodeData> {
  return {
    id,
    type: 'canvasNode',
    position: { x: 0, y: 0 },
    data: {
      nodeType,
      name,
      category: '测试',
      bizConfig,
    },
  }
}
