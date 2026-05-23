import { getBranchHandles, type BranchHandle } from './branchHandles'

export interface OutletSchemaItem {
  id: string
  label: string
  color?: string
}

export function parseOutletSchema(raw: string | undefined): BranchHandle[] {
  if (!raw) return []
  try {
    const parsed = JSON.parse(raw) as OutletSchemaItem[]
    if (!Array.isArray(parsed)) return []
    return parsed
      .filter(item => item.id && item.label)
      .map(item => ({
        id: item.id,
        label: item.label,
        color: item.color ?? '#1677ff',
      }))
  } catch {
    return []
  }
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
