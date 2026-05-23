import type { Edge } from '@xyflow/react'
import { getOutletTargetField, parseOutletSchemaItems, type OutletTargetField } from '../../components/canvas/outletSchema'
import type { BackendNode, BizConfig } from '../../types/canvas'

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
  { field: 'maxExceededNodeId', handleId: 'max_exceeded' },
  { field: 'goalMetNodeId', handleId: 'goal_met' },
  { field: 'goalNotMetNodeId', handleId: 'goal_not_met' },
]

function edgeId(sourceId: string, targetId: string, sourceHandle: string): string {
  return sourceHandle === 'default'
    ? `${sourceId}->${targetId}`
    : `${sourceId}->${targetId}::${sourceHandle}`
}

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
  return false
}

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
  return false
}

export function deriveEdges(backendNodes: BackendNode[]): Edge[] {
  const edges: Edge[] = []

  backendNodes.forEach(node => {
    const config = (node.config ?? {}) as BizConfig
    const consumedFields = new Set<OutletTargetField>()
    const push = (target: unknown, sourceHandle: string) => {
      if (typeof target !== 'string' || target.length === 0) return
      edges.push({
        id: edgeId(node.id, target, sourceHandle),
        source: node.id,
        target,
        sourceHandle,
      })
    }

    parseOutletSchemaItems(node.outletSchema).forEach(item => {
      const field = getOutletTargetField(item.id, node.outletSchema)
      if (!field) return
      consumedFields.add(field)
      push(config[field], item.id)
    })

    FIELD_HANDLES.forEach(({ field, handleId }) => {
      if (consumedFields.has(field)) return
      push(config[field], handleId)
    })

    config.branches?.forEach((branch, index) => push(branch.nextNodeId, `branch-${index}`))
    config.priorities?.forEach((priority, index) => push(priority.nextNodeId, `priority-${index}`))
    config.groups?.forEach(group => push(group.nextNodeId, `group-${group.groupKey}`))
  })

  return edges
}

export function patchBizConfig(
  cfg: Record<string, unknown>,
  sourceHandle: string,
  target: string,
  outletSchema?: string,
): BizConfig {
  const next = { ...cfg } as BizConfig
  if (patchIndexedOutlet(next, sourceHandle, target)) return next

  const field = getOutletTargetField(sourceHandle, outletSchema) ?? 'nextNodeId'
  next[field] = target
  return next
}

export function clearEdgeRef(
  cfg: Record<string, unknown>,
  edge: Edge,
  outletSchema?: string,
): BizConfig {
  const next = { ...(cfg as BizConfig) }
  const sourceHandle = edge.sourceHandle ?? 'default'
  if (clearIndexedOutlet(next, sourceHandle)) return next

  const field = getOutletTargetField(sourceHandle, outletSchema) ?? 'nextNodeId'
  next[field] = undefined
  return next
}
