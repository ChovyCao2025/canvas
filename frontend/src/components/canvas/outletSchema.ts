import { getBranchHandles, type BranchHandle } from './branchHandles'

export const OUTLET_TARGET_FIELDS = [
  'nextNodeId',
  'successNodeId',
  'failNodeId',
  'elseNodeId',
  'approveNodeId',
  'rejectNodeId',
  'hitNextNodeId',
  'missNextNodeId',
  'timeoutNodeId',
  'suppressedNodeId',
  'skippedNodeId',
  'maxExceededNodeId',
  'goalMetNodeId',
  'goalNotMetNodeId',
] as const

export type OutletTargetField = typeof OUTLET_TARGET_FIELDS[number]

export interface OutletSchemaItem {
  id: string
  label: string
  color?: string
  targetField?: OutletTargetField
}

const DEFAULT_TARGET_FIELDS: Record<string, OutletTargetField> = {
  default: 'nextNodeId',
  success: 'successNodeId',
  fail: 'failNodeId',
  else: 'elseNodeId',
  approve: 'approveNodeId',
  reject: 'rejectNodeId',
  hit: 'hitNextNodeId',
  miss: 'missNextNodeId',
  timeout: 'timeoutNodeId',
  suppressed: 'suppressedNodeId',
  skipped: 'skippedNodeId',
  max_exceeded: 'maxExceededNodeId',
  goal_met: 'goalMetNodeId',
  goal_not_met: 'goalNotMetNodeId',
}

function isOutletTargetField(value: unknown): value is OutletTargetField {
  return typeof value === 'string' && (OUTLET_TARGET_FIELDS as readonly string[]).includes(value)
}

export function parseOutletSchemaItems(raw: string | undefined): OutletSchemaItem[] {
  if (!raw) return []
  try {
    const parsed = JSON.parse(raw) as unknown
    if (!Array.isArray(parsed)) return []
    return parsed
      .filter((item): item is OutletSchemaItem => {
        if (item == null || typeof item !== 'object') return false
        const candidate = item as Record<string, unknown>
        return typeof candidate.id === 'string' && typeof candidate.label === 'string'
      })
      .map(item => ({
        id: item.id,
        label: item.label,
        color: typeof item.color === 'string' ? item.color : undefined,
        targetField: isOutletTargetField(item.targetField) ? item.targetField : undefined,
      }))
  } catch {
    return []
  }
}

export function parseOutletSchema(raw: string | undefined): BranchHandle[] {
  return parseOutletSchemaItems(raw).map(item => ({
    id: item.id,
    label: item.label,
    color: item.color ?? '#1677ff',
  }))
}

export function getOutletHandles(input: {
  nodeType: string
  bizConfig: Record<string, unknown>
  outletSchema?: string
}): BranchHandle[] {
  const dynamic = parseOutletSchema(input.outletSchema)
  if (dynamic.length > 0) return dynamic
  return getBranchHandles(input.nodeType, input.bizConfig)
}

export function getOutletTargetField(
  handleId: string,
  outletSchema?: string,
): OutletTargetField | undefined {
  const dynamic = parseOutletSchemaItems(outletSchema)
  const item = dynamic.find(candidate => candidate.id === handleId)
  if (item?.targetField) return item.targetField
  return DEFAULT_TARGET_FIELDS[handleId]
}
