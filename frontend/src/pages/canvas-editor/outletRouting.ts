/**
 * 页面职责：画布出口路由工具，把 React Flow 边关系写回节点 bizConfig。
 *
 * 维护说明：后端以节点配置保存后继关系，因此所有连线变化最终都要经过这里归一化。
 */
import type { Edge } from '@xyflow/react'
import { getOutletTargetField, parseOutletSchemaItems, type OutletTargetField } from '../../components/canvas/outletSchema'
import type { BackendNode, BizConfig } from '../../types/canvas'

/** 固定出口 handle 到后端 bizConfig 目标字段的兼容映射。 */
const FIELD_HANDLES: Array<{ field: OutletTargetField; handleId: string }> = [
  { field: 'nextNodeId', handleId: 'default' },
  { field: 'successNodeId', handleId: 'success' },
  { field: 'failNodeId', handleId: 'fail' },
  { field: 'elseNodeId', handleId: 'else' },
  { field: 'approveNodeId', handleId: 'approve' },
  { field: 'rejectNodeId', handleId: 'reject' },
  { field: 'hitNextNodeId', handleId: 'hit' },
  { field: 'missNextNodeId', handleId: 'miss' },
  { field: 'timeoutNodeId', handleId: 'timeout' },
  { field: 'suppressedNodeId', handleId: 'suppressed' },
  { field: 'skippedNodeId', handleId: 'skipped' },
  { field: 'allowedNodeId', handleId: 'allowed' },
  { field: 'quietNodeId', handleId: 'quiet' },
  { field: 'availableNodeId', handleId: 'available' },
  { field: 'unavailableNodeId', handleId: 'unavailable' },
  { field: 'passNodeId', handleId: 'pass' },
  { field: 'cappedNodeId', handleId: 'capped' },
  { field: 'fallbackNodeId', handleId: 'fallback' },
  { field: 'exitNodeId', handleId: 'exit' },
  { field: 'loopStartNodeId', handleId: 'loop' },
  { field: 'targetNodeId', handleId: 'goto' },
  { field: 'maxExceededNodeId', handleId: 'max_exceeded' },
  { field: 'goalMetNodeId', handleId: 'goal_met' },
  { field: 'goalNotMetNodeId', handleId: 'goal_not_met' },
]

/** 生成 React Flow edge ID；非默认出口把 handle 写入 ID，避免多出口边冲突。 */
function edgeId(sourceId: string, targetId: string, sourceHandle: string): string {
  return sourceHandle === 'default'
    ? `${sourceId}->${targetId}`
    : `${sourceId}->${targetId}::${sourceHandle}`
}

/** 旧版固定 handle 对应的后端字段，用于动态 outlet 迁移时清理影子字段。 */
function legacyFieldForHandle(sourceHandle: string): OutletTargetField | undefined {
  return FIELD_HANDLES.find(item => item.handleId === sourceHandle)?.field
}

/** 清理同一 handle 曾经写入过的旧字段，避免保存后出现两条等价后继关系。 */
function clearShadowedLegacyField(next: BizConfig, sourceHandle: string, field: OutletTargetField): void {
  const legacyField = legacyFieldForHandle(sourceHandle)
  if (legacyField && legacyField !== field) {
    next[legacyField] = undefined
  }
}

/** 写入带索引/业务 key 的动态集合出口，例如 branch-0、group-A、variant-B。 */
function patchIndexedOutlet(next: BizConfig, sourceHandle: string, target: string): boolean {
  if (sourceHandle.startsWith('branch-')) {
    const idx = parseInt(sourceHandle.split('-')[1], 10)
    next.branches = (next.branches ?? []).map((branch, i) =>
      i === idx ? { ...branch, nextNodeId: target } : branch,
    )
    return true
  }
  if (sourceHandle.startsWith('priority-')) {
    const idx = parseInt(sourceHandle.split('-')[1], 10)
    next.priorities = (next.priorities ?? []).map((priority, i) =>
      i === idx ? { ...priority, nextNodeId: target } : priority,
    )
    return true
  }
  if (sourceHandle.startsWith('group-')) {
    const key = sourceHandle.replace('group-', '')
    next.groups = (next.groups ?? []).map(group =>
      group.groupKey === key ? { ...group, nextNodeId: target } : group,
    )
    return true
  }
  if (sourceHandle.startsWith('path-')) {
    const key = sourceHandle.replace('path-', '')
    next.paths = (next.paths ?? []).map(path => {
      const id = String(path.pathId ?? path.id ?? '')
      return id === key ? { ...path, nextNodeId: target } : path
    })
    return true
  }
  if (sourceHandle.startsWith('variant-')) {
    const key = sourceHandle.replace('variant-', '')
    next.variants = (next.variants ?? []).map(variant => {
      const id = String(variant.variantId ?? variant.id ?? '')
      return id === key ? { ...variant, nextNodeId: target } : variant
    })
    return true
  }
  if (sourceHandle.startsWith('band-')) {
    const key = sourceHandle.replace('band-', '')
    next.bands = (next.bands ?? []).map(band => {
      const id = String(band.bandId ?? band.id ?? '')
      return id === key ? { ...band, nextNodeId: target } : band
    })
    return true
  }
  return false
}

/** 清理带索引/业务 key 的动态集合出口引用。 */
function clearIndexedOutlet(next: BizConfig, sourceHandle: string): boolean {
  if (sourceHandle.startsWith('branch-')) {
    const idx = parseInt(sourceHandle.split('-')[1], 10)
    next.branches = (next.branches ?? []).map((branch, i) =>
      i === idx ? { ...branch, nextNodeId: undefined } : branch,
    )
    return true
  }
  if (sourceHandle.startsWith('priority-')) {
    const idx = parseInt(sourceHandle.split('-')[1], 10)
    next.priorities = (next.priorities ?? []).map((priority, i) =>
      i === idx ? { ...priority, nextNodeId: undefined } : priority,
    )
    return true
  }
  if (sourceHandle.startsWith('group-')) {
    const key = sourceHandle.replace('group-', '')
    next.groups = (next.groups ?? []).map(group =>
      group.groupKey === key ? { ...group, nextNodeId: undefined } : group,
    )
    return true
  }
  if (sourceHandle.startsWith('path-')) {
    const key = sourceHandle.replace('path-', '')
    next.paths = (next.paths ?? []).map(path => {
      const id = String(path.pathId ?? path.id ?? '')
      return id === key ? { ...path, nextNodeId: undefined } : path
    })
    return true
  }
  if (sourceHandle.startsWith('variant-')) {
    const key = sourceHandle.replace('variant-', '')
    next.variants = (next.variants ?? []).map(variant => {
      const id = String(variant.variantId ?? variant.id ?? '')
      return id === key ? { ...variant, nextNodeId: undefined } : variant
    })
    return true
  }
  if (sourceHandle.startsWith('band-')) {
    const key = sourceHandle.replace('band-', '')
    next.bands = (next.bands ?? []).map(band => {
      const id = String(band.bandId ?? band.id ?? '')
      return id === key ? { ...band, nextNodeId: undefined } : band
    })
    return true
  }
  return false
}

/**
 * 从后端节点配置推导 React Flow 边集合。
 *
 * 后端主存是节点 config 中的 nextNodeId/branches/groups 等字段；
 * 编辑器加载时通过这个函数还原可视化连线。
 */
export function deriveEdges(backendNodes: BackendNode[]): Edge[] {
  const edges: Edge[] = []

  backendNodes.forEach(node => {
    const config = (node.config ?? {}) as BizConfig
    /** 追加一条有效目标边；空目标直接忽略。 */
    const push = (target: unknown, sourceHandle: string) => {
      if (typeof target !== 'string' || target.length === 0) return
      edges.push({
        id: edgeId(node.id, target, sourceHandle),
        source: node.id,
        target,
        sourceHandle,
      })
    }

    const dynamicItems = parseOutletSchemaItems(node.outletSchema)
    // 动态 outletSchema 优先于固定 FIELD_HANDLES，支持后端配置驱动的新增出口。
    dynamicItems.forEach(item => {
      const field = getOutletTargetField(item.id, node.outletSchema)
      if (!field) return
      push(config[field], item.id)
    })

    // 没有动态 schema 的旧节点继续按固定字段映射生成连线。
    if (dynamicItems.length === 0) {
      FIELD_HANDLES.forEach(({ field, handleId }) => {
        push(config[field], handleId)
      })
    }

    // 集合型分支用 handle ID 保存索引或业务 key，便于拖线时回写到正确项。
    config.branches?.forEach((branch, index) => push(branch.nextNodeId, `branch-${index}`))
    config.priorities?.forEach((priority, index) => push(priority.nextNodeId, `priority-${index}`))
    config.groups?.forEach(group => push(group.nextNodeId, `group-${group.groupKey}`))
    config.paths?.forEach((path, index) =>
      push(path.nextNodeId, `path-${String(path.pathId ?? path.id ?? `path_${index + 1}`)}`),
    )
    config.variants?.forEach((variant, index) =>
      push(variant.nextNodeId, `variant-${String(variant.variantId ?? variant.id ?? `variant_${index + 1}`)}`),
    )
    config.bands?.forEach((band, index) =>
      push(band.nextNodeId, `band-${String(band.bandId ?? band.id ?? `band_${index + 1}`)}`),
    )
  })

  return edges
}

/** 把一次连线操作写回源节点 bizConfig。 */
export function patchBizConfig(
  cfg: Record<string, unknown>,
  sourceHandle: string,
  target: string,
  outletSchema?: string,
): BizConfig {
  const next = { ...cfg } as BizConfig
  if (patchIndexedOutlet(next, sourceHandle, target)) return next

  const dynamicItems = parseOutletSchemaItems(outletSchema)
  // 有动态 schema 时，只允许写入 schema 声明过的出口，避免非法 handle 污染配置。
  if (dynamicItems.length > 0 && !dynamicItems.some(item => item.id === sourceHandle)) {
    return next
  }

  const field = getOutletTargetField(sourceHandle, outletSchema)
    ?? (dynamicItems.length === 0 ? 'nextNodeId' : undefined)
  if (!field) return next

  next[field] = target
  clearShadowedLegacyField(next, sourceHandle, field)
  return next
}

/** 删除边时同步清空源节点 bizConfig 中对应的后继引用。 */
export function clearEdgeRef(
  cfg: Record<string, unknown>,
  edge: Edge,
  outletSchema?: string,
): BizConfig {
  const next = { ...(cfg as BizConfig) }
  const sourceHandle = edge.sourceHandle ?? 'default'
  if (clearIndexedOutlet(next, sourceHandle)) return next

  const dynamicItems = parseOutletSchemaItems(outletSchema)
  // 与 patchBizConfig 保持对称：动态 schema 外的 handle 不做任何写回。
  if (dynamicItems.length > 0 && !dynamicItems.some(item => item.id === sourceHandle)) {
    return next
  }

  const field = getOutletTargetField(sourceHandle, outletSchema)
    ?? (dynamicItems.length === 0 ? 'nextNodeId' : undefined)
  if (!field) return next

  next[field] = undefined
  clearShadowedLegacyField(next, sourceHandle, field)
  return next
}
