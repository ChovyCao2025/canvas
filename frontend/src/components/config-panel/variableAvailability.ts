/**
 * 工具职责：推导当前选中节点可插入模板的变量列表。
 *
 * 维护说明：本模块保持纯函数，方便在不渲染配置面板的情况下测试变量可用性规则。
 */
/** 变量来源分类，用于变量选择器分组和排序。 */
export type VariableSource = 'trigger' | 'profile' | 'computed' | 'upstream'

/** 单个可引用字段的最小描述。 */
export interface VariableField {
  /** 运行时上下文字段 key，也是生成 {{fieldKey}} token 的来源。 */
  fieldKey: string

  /** 展示名称；缺省时使用 fieldKey。 */
  fieldName?: string
}

/** 参与变量可用性计算的画布节点最小形态。 */
export interface VariableAvailabilityNode {
  /** 节点 ID。 */
  id: string

  /** 节点展示名。 */
  label?: string

  /** 当前节点产生的输出变量。 */
  outputs?: Array<string | VariableField>

  /** 当前节点直接指向的后继节点 ID。 */
  nextNodeIds?: string[]
}

/** 计算可用变量所需的图和基础变量输入。 */
export interface AvailableVariablesInput {
  /** 当前选中节点 ID；为空时只返回基础变量。 */
  selectedNodeId: string | null

  /** 画布节点列表，包含输出字段和后继关系。 */
  nodes: VariableAvailabilityNode[]

  /** 外部已计算的上游节点 ID；不传时会从 nextNodeIds 反推。 */
  upstreamNodeIds?: string[]

  /** 触发事件天然提供的变量。 */
  triggerFields?: Array<string | VariableField>

  /** 用户画像变量。 */
  profileFields?: Array<string | VariableField>

  /** 系统或脚本计算变量。 */
  computedFields?: Array<string | VariableField>
}

/** 变量选择器可直接展示和插入的变量项。 */
export interface AvailableVariable {
  /** 插入模板中的 token，例如 {{userId}}。 */
  token: string

  /** 变量展示名。 */
  label: string

  /** 变量来源。 */
  source: VariableSource

  /** 原始上下文字段 key。 */
  fieldKey: string

  /** 产出该变量的上游节点 ID。 */
  nodeId?: string

  /** 产出该变量的上游节点名称。 */
  nodeLabel?: string
}

const SOURCE_ORDER: Record<VariableSource, number> = {
  trigger: 0,
  profile: 1,
  computed: 2,
  upstream: 3,
}

/** 汇总基础变量和当前节点所有上游节点的输出变量。 */
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
    // 同一 fieldKey 只展示一次，基础变量优先级高于上游节点输出。
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

/** 从节点的 nextNodeIds 反向遍历，找到当前节点可见的全部上游节点。 */
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
    // 继续向上找父节点，保证跨多跳链路的变量也可用。
    parents.get(current)?.forEach(parent => queue.push(parent))
  }
  return [...upstream]
}

/** 归一化字符串字段和对象字段，过滤空 key。 */
function normalizeField(field: string | VariableField): VariableField | null {
  const fieldKey = typeof field === 'string' ? field : field.fieldKey
  const trimmed = fieldKey?.trim()
  if (!trimmed) return null
  return {
    fieldKey: trimmed,
    fieldName: typeof field === 'string' ? undefined : field.fieldName,
  }
}

/** 根据字段 key 前缀推断变量来源，无法识别时归为上游节点输出。 */
function classifySource(field: string | VariableField): VariableSource {
  const fieldKey = typeof field === 'string' ? field : field.fieldKey
  if (fieldKey.startsWith('profile.')) return 'profile'
  if (fieldKey.startsWith('computed.')) return 'computed'
  if (fieldKey.startsWith('trigger.') || fieldKey === 'userId') return 'trigger'
  return 'upstream'
}
