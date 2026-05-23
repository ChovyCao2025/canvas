import type { CanvasNodeData } from '../../types/canvas'

export interface NodeConfigFormSyncPlan {
  values: Record<string, unknown>
  staleKeys: string[]
  shouldResetBeforeApply: boolean
}

export function buildNodeConfigFormSyncPlan(
  previousValues: Record<string, unknown>,
  nodeData: CanvasNodeData | null,
): NodeConfigFormSyncPlan {
  const values = nodeData ? { name: nodeData.name, ...nodeData.bizConfig } : {}
  const nextKeys = new Set(Object.keys(values))
  const staleKeys = Object.keys(previousValues)
    .filter((key) => !nextKeys.has(key))
    .sort()

  return {
    values,
    staleKeys,
    shouldResetBeforeApply: staleKeys.length > 0,
  }
}
