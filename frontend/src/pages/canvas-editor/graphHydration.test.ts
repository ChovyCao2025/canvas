import { describe, expect, it } from 'vitest'
import type { NodeTypeRegistry } from '../../types'
import type { BackendNode } from '../../types/canvas'
import { hydrateBackendNodeOutletSchemas } from './graphHydration'

describe('graph hydration', () => {
  const sendOutletSchema = JSON.stringify([
    { id: 'success', label: '成功', targetField: 'successNodeId' },
    { id: 'fail', label: '失败', targetField: 'failNodeId' },
  ])

  it('hydrates missing backend node outlet schemas from node registry metadata', () => {
    const nodes: BackendNode[] = [
      {
        id: 'send_1',
        type: 'SEND_EMAIL',
        name: '发送邮件',
        category: '消息触达',
        x: 0,
        y: 0,
        config: {},
      },
    ]
    const registry = [
      {
        typeKey: 'SEND_EMAIL',
        typeName: '发送邮件',
        category: '消息触达',
        configSchema: '[]',
        outputSchema: '[]',
        outletSchema: sendOutletSchema,
        isTrigger: 0,
        isTerminal: 0,
        enabled: 1,
      },
    ] satisfies NodeTypeRegistry[]

    expect(hydrateBackendNodeOutletSchemas(nodes, registry)).toEqual([
      { ...nodes[0], outletSchema: sendOutletSchema },
    ])
  })

  it('keeps outlet schema already stored in graph json', () => {
    const storedOutletSchema = JSON.stringify([
      { id: 'custom', label: '自定义', targetField: 'nextNodeId' },
    ])
    const nodes: BackendNode[] = [
      {
        id: 'send_1',
        type: 'SEND_EMAIL',
        name: '发送邮件',
        category: '消息触达',
        x: 0,
        y: 0,
        config: {},
        outletSchema: storedOutletSchema,
      },
    ]
    const registry = [
      {
        typeKey: 'SEND_EMAIL',
        typeName: '发送邮件',
        category: '消息触达',
        configSchema: '[]',
        outputSchema: '[]',
        outletSchema: sendOutletSchema,
        isTrigger: 0,
        isTerminal: 0,
        enabled: 1,
      },
    ] satisfies NodeTypeRegistry[]

    expect(hydrateBackendNodeOutletSchemas(nodes, registry)[0].outletSchema).toBe(storedOutletSchema)
  })
})
