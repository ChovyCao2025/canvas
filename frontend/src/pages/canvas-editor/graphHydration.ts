import type { NodeTypeRegistry } from '../../types'
import type { BackendNode } from '../../types/canvas'

function hasStoredOutletSchema(node: BackendNode): boolean {
  return typeof node.outletSchema === 'string' && node.outletSchema.trim().length > 0
}

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
    if (hasStoredOutletSchema(node)) return node
    const outletSchema = outletSchemaByType.get(node.type)
    if (!outletSchema) return node
    return { ...node, outletSchema }
  })
}
