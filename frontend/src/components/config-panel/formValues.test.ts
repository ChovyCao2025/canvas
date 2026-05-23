import { describe, expect, it } from 'vitest'
import type { CanvasNodeData } from '../../types/canvas'
import { buildNodeConfigFormSyncPlan } from './formValues'

const node = (overrides: Partial<CanvasNodeData>): CanvasNodeData => ({
  nodeType: 'API_CALL',
  name: '接口节点',
  category: '行为策略',
  bizConfig: {},
  ...overrides,
})

describe('buildNodeConfigFormSyncPlan', () => {
  it('marks values from the previously selected same-type node as stale when absent on the next node', () => {
    const previousValues = {
      name: '查询订单接口',
      apiKey: 'queryOrder',
      outputPrefix: 'order',
      inputParams: { orderId: '${orderId}' },
    }

    const plan = buildNodeConfigFormSyncPlan(
      previousValues,
      node({
        name: '查询用户接口',
        bizConfig: {
          outputPrefix: 'user',
        },
      }),
    )

    expect(plan.values).toEqual({
      name: '查询用户接口',
      outputPrefix: 'user',
    })
    expect(plan.staleKeys).toEqual(['apiKey', 'inputParams'])
    expect(plan.shouldResetBeforeApply).toBe(true)
  })

  it('clears the form entirely when no node is selected', () => {
    const plan = buildNodeConfigFormSyncPlan({ name: '旧节点', apiKey: 'oldApi' }, null)

    expect(plan.values).toEqual({})
    expect(plan.staleKeys).toEqual(['apiKey', 'name'])
    expect(plan.shouldResetBeforeApply).toBe(true)
  })
})
