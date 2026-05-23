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

function edgeId(sourceId: string, targetId: string, sourceHandle: string): string {
  return sourceHandle === 'default'
    ? `${sourceId}->${targetId}`
    : `${sourceId}->${targetId}::${sourceHandle}`
}

function legacyFieldForHandle(sourceHandle: string): OutletTargetField | undefined {
  return FIELD_HANDLES.find(item => item.handleId === sourceHandle)?.field
}

function clearShadowedLegacyField(next: BizConfig, sourceHandle: string, field: OutletTargetField): void {
  const legacyField = legacyFieldForHandle(sourceHandle)
  if (legacyField && legacyField !== field) {
    next[legacyField] = undefined
  }
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

export function deriveEdges(backendNodes: BackendNode[]): Edge[] {
  const edges: Edge[] = []

  backendNodes.forEach(node => {
    const config = (node.config ?? {}) as BizConfig
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
    dynamicItems.forEach(item => {
      const field = getOutletTargetField(item.id, node.outletSchema)
      if (!field) return
      push(config[field], item.id)
    })

    if (dynamicItems.length === 0) {
      FIELD_HANDLES.forEach(({ field, handleId }) => {
        push(config[field], handleId)
      })
    }

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

export function patchBizConfig(
  cfg: Record<string, unknown>,
  sourceHandle: string,
  target: string,
  outletSchema?: string,
): BizConfig {
  const next = { ...cfg } as BizConfig
  if (patchIndexedOutlet(next, sourceHandle, target)) return next

  const dynamicItems = parseOutletSchemaItems(outletSchema)
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

export function clearEdgeRef(
  cfg: Record<string, unknown>,
  edge: Edge,
  outletSchema?: string,
): BizConfig {
  const next = { ...(cfg as BizConfig) }
  const sourceHandle = edge.sourceHandle ?? 'default'
  if (clearIndexedOutlet(next, sourceHandle)) return next

  const dynamicItems = parseOutletSchemaItems(outletSchema)
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
