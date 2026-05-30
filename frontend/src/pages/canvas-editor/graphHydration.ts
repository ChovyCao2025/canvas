/**
 * 页面职责：后端 graph_json 到编辑器初始图的水合工具。
 *
 * 维护说明：负责兜底空图、解析异常和 START 节点补齐。
 */
import type { NodeTypeRegistry } from '../../types'
import type { BackendNode } from '../../types/canvas'

/** 判断后端节点是否已经保存 outletSchema，避免覆盖用户保存的历史结构。 */
function hasStoredOutletSchema(node: BackendNode): boolean {
  return typeof node.outletSchema === 'string' && node.outletSchema.trim().length > 0
}

/** 用节点注册表中的 outletSchema 补齐旧 graph_json 中缺失的节点出口 schema。 */
export function hydrateBackendNodeOutletSchemas(
  nodes: BackendNode[],
  registry: NodeTypeRegistry[],
): BackendNode[] {
  const outletSchemaByType = new Map(
    registry
      .filter(nodeType => typeof nodeType.outletSchema === 'string' && nodeType.outletSchema.trim().length > 0)
      .map(nodeType => [nodeType.typeKey, nodeType.outletSchema as string]),
  )

  return nodes.map(node => {
    if (node.type === 'DIRECT_CALL') {
      const { outletSchema: _legacyOutletSchema, ...directCallNode } = node
      return directCallNode
    }
    if (hasStoredOutletSchema(node)) return node
    const outletSchema = outletSchemaByType.get(node.type)
    if (!outletSchema) return node
    return { ...node, outletSchema }
  })
}
