/**
 * Helper responsibilities: derive template variables available to the selected node.
 *
 * Maintenance note: this module is intentionally pure so graph availability rules can be tested
 * without rendering the config panel.
 */
export type VariableSource = 'trigger' | 'profile' | 'computed' | 'upstream'

export interface VariableField {
  fieldKey: string
  fieldName?: string
}

export interface VariableAvailabilityNode {
  id: string
  label?: string
  outputs?: Array<string | VariableField>
  nextNodeIds?: string[]
}

export interface AvailableVariablesInput {
  selectedNodeId: string | null
  nodes: VariableAvailabilityNode[]
  upstreamNodeIds?: string[]
  triggerFields?: Array<string | VariableField>
  profileFields?: Array<string | VariableField>
  computedFields?: Array<string | VariableField>
}

export interface AvailableVariable {
  token: string
  label: string
  source: VariableSource
  fieldKey: string
  nodeId?: string
  nodeLabel?: string
}

const SOURCE_ORDER: Record<VariableSource, number> = {
  trigger: 0,
  profile: 1,
  computed: 2,
  upstream: 3,
}

export function availableVariables(input: AvailableVariablesInput): AvailableVariable[] {
  const result: AvailableVariable[] = []
  const seen = new Set<string>()
  const upstream = new Set(input.upstreamNodeIds ?? deriveUpstreamNodeIds(input.nodes, input.selectedNodeId))

  const add = (
    field: string | VariableField,
    source: VariableSource,
    node?: VariableAvailabilityNode,
  ) => {
    const normalized = normalizeField(field)
    if (!normalized || seen.has(normalized.fieldKey)) return
    seen.add(normalized.fieldKey)
    result.push({
      token: `{{${normalized.fieldKey}}}`,
      label: normalized.fieldName ?? normalized.fieldKey,
      source,
      fieldKey: normalized.fieldKey,
      nodeId: node?.id,
      nodeLabel: node?.label,
    })
  }

  input.triggerFields?.forEach(field => add(field, 'trigger'))
  input.profileFields?.forEach(field => add(field, 'profile'))
  input.computedFields?.forEach(field => add(field, 'computed'))
  input.nodes
    .filter(node => node.id !== input.selectedNodeId && upstream.has(node.id))
    .forEach(node => node.outputs?.forEach(field => add(field, classifySource(field), node)))

  return result.sort((left, right) =>
    SOURCE_ORDER[left.source] - SOURCE_ORDER[right.source] ||
    left.label.localeCompare(right.label) ||
    left.fieldKey.localeCompare(right.fieldKey)
  )
}

export function deriveUpstreamNodeIds(nodes: VariableAvailabilityNode[], selectedNodeId: string | null): string[] {
  if (!selectedNodeId) return []
  const parents = new Map<string, Set<string>>()
  nodes.forEach(node => {
    node.nextNodeIds?.forEach(target => {
      if (!parents.has(target)) {
        parents.set(target, new Set())
      }
      parents.get(target)!.add(node.id)
    })
  })

  const upstream = new Set<string>()
  const queue = [...(parents.get(selectedNodeId) ?? [])]
  while (queue.length > 0) {
    const current = queue.shift()!
    if (upstream.has(current)) continue
    upstream.add(current)
    parents.get(current)?.forEach(parent => queue.push(parent))
  }
  return [...upstream]
}

function normalizeField(field: string | VariableField): VariableField | null {
  const fieldKey = typeof field === 'string' ? field : field.fieldKey
  const trimmed = fieldKey?.trim()
  if (!trimmed) return null
  return {
    fieldKey: trimmed,
    fieldName: typeof field === 'string' ? undefined : field.fieldName,
  }
}

function classifySource(field: string | VariableField): VariableSource {
  const fieldKey = typeof field === 'string' ? field : field.fieldKey
  if (fieldKey.startsWith('profile.')) return 'profile'
  if (fieldKey.startsWith('computed.')) return 'computed'
  if (fieldKey.startsWith('trigger.') || fieldKey === 'userId') return 'trigger'
  return 'upstream'
}
