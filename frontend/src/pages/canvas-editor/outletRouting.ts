/**
 * 页面职责：画布出口路由工具，把 React Flow 边关系写回节点 bizConfig。
 *
 * 维护说明：后端以节点配置保存后继关系，因此所有连线变化最终都要经过这里归一化。
 */
import type { Edge } from '@xyflow/react'
import { getOutletTargetField, parseOutletSchemaItems, type OutletTargetField } from '../../components/canvas/outletSchema'
import type { BackendNode, BizConfig } from '../../types/canvas'

/** 固定出口 handle 到后端 bizConfig 目标字段的映射。 */
const FIELD_HANDLES: Array<{ field: OutletTargetField; handleId: string }> = [
  { field: 'nextNodeId', handleId: 'default' },
  { field: 'successNodeId', handleId: 'success' },
  { field: 'failNodeId', handleId: 'fail' },
  { field: 'hitNextNodeId', handleId: 'hit' },
  { field: 'missNextNodeId', handleId: 'miss' },
  { field: 'timeoutNodeId', handleId: 'timeout' },
]

/** 生成 React Flow edge ID；非默认出口把 handle 写入 ID，避免多出口边冲突。 */
function edgeId(sourceId: string, targetId: string, sourceHandle: string): string {
  return sourceHandle === 'default'
    ? `${sourceId}->${targetId}`
    : `${sourceId}->${targetId}::${sourceHandle}`
}

/** 合并一条出口边：同一 source + handle 替换，不同 handle 保留。 */
export function mergeOutletEdge(edges: Edge[], edge: Edge): Edge[] {
  const sourceHandle = edge.sourceHandle ?? 'default'
  return [
    ...edges.filter(item =>
      item.id !== edge.id
      && (item.source !== edge.source || (item.sourceHandle ?? 'default') !== sourceHandle),
    ),
    edge,
  ]
}

/** API 入口使用单个可视出口，所有下游连线反推为 branches。 */
export function appendDirectCallBranch(
  cfg: Record<string, unknown>,
  target: string,
  targetLabel?: string,
): BizConfig {
  const next = { ...(cfg as BizConfig) }
  const branches = [...(next.branches ?? [])]
  let seededFromNextNode = false
  if (branches.length === 0 && typeof next.nextNodeId === 'string' && next.nextNodeId.length > 0) {
    branches.push({ label: '分支 1', nextNodeId: next.nextNodeId })
    seededFromNextNode = true
  }
  const existing = branches.find(branch => branch.nextNodeId === target)
  if (existing) {
    if (seededFromNextNode) {
      next.nextNodeId = undefined
      next.branches = branches
    }
    return next
  }

  next.nextNodeId = undefined
  next.branches = [
    ...branches,
    {
      label: targetLabel?.trim() || `分支 ${branches.length + 1}`,
      nextNodeId: target,
    },
  ]
  return next
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

function branchHandleKey(branch: Record<string, unknown>, index: number): string {
  return String(branch.branchId ?? branch.id ?? `branch_${index + 1}`)
}

/** 写入带业务 key 的动态集合出口，例如 SPLIT 的 branch-a。 */
function patchIndexedOutlet(next: BizConfig, sourceHandle: string, target: string): boolean {
  if (sourceHandle.startsWith('branch-')) {
    const key = sourceHandle.replace('branch-', '')
    const branches = [...(next.branches ?? [])]
    const index = branches.findIndex((branch, i) => branchHandleKey(branch, i) === key)
    if (index === -1) {
      branches.push({ branchId: key, label: `分支 ${branches.length + 1}`, nextNodeId: target })
    } else {
      branches[index] = { ...branches[index], nextNodeId: target }
    }
    next.branches = branches
    return true
  }
  return false
}

/** 清理带索引/业务 key 的动态集合出口引用。 */
function clearIndexedOutlet(next: BizConfig, sourceHandle: string): boolean {
  if (sourceHandle.startsWith('branch-')) {
    const key = sourceHandle.replace('branch-', '')
    next.branches = (next.branches ?? []).map((branch, i) =>
      branchHandleKey(branch, i) === key ? { ...branch, nextNodeId: undefined } : branch,
    )
    return true
  }
  return false
}

/**
 * 从后端节点配置推导 React Flow 边集合。
 *
 * 后端主存是节点 config 中的 nextNodeId/branches 等字段；
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
    const singleOutletFanOut = (node.type === 'DIRECT_CALL' || node.type === 'START')
      && Array.isArray(config.branches)
      && config.branches.length > 0
    // 动态 outletSchema 优先于固定 FIELD_HANDLES，支持后端配置驱动的新增出口。
    dynamicItems.forEach(item => {
      const field = getOutletTargetField(item.id, node.outletSchema)
      if (!field) return
      push(config[field], item.id)
    })

    // 没有动态 schema 的旧节点继续按固定字段映射生成连线。
    if (dynamicItems.length === 0 && !singleOutletFanOut) {
      FIELD_HANDLES.forEach(({ field, handleId }) => {
        push(config[field], handleId)
      })
    }

    // START / DIRECT_CALL 的多下游分支由同一个可视出口连出，避免出现不可删除的“新增分支”假出口。
    if (node.type === 'DIRECT_CALL' || node.type === 'START') {
      config.branches?.forEach(branch => push(branch.nextNodeId, 'default'))
    } else {
      config.branches?.forEach((branch, index) => push(branch.nextNodeId, `branch-${branchHandleKey(branch, index)}`))
    }
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
  if (sourceHandle === 'default' && Array.isArray(next.branches)) {
    const branches = next.branches.filter(branch => branch.nextNodeId !== edge.target)
    if (branches.length !== next.branches.length) {
      next.branches = branches
      next.nextNodeId = undefined
      return next
    }
  }
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
