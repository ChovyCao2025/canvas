/**
 * Module role: build a frontend read-only index of plugin node config schemas.
 */
import {
  assertValidPluginManifest,
  type NodeConfigSchema,
  type PluginManifestV1,
} from './pluginManifest'

export interface RegisteredPluginNodeSchema {
  nodeType: string
  pluginId: string
  schema: NodeConfigSchema
}

export interface PluginRegistry {
  manifests: PluginManifestV1[]
  nodeSchemas: Map<string, RegisteredPluginNodeSchema>
}

export function buildPluginRegistry(manifests: PluginManifestV1[]): PluginRegistry {
  const pluginIds = new Set<string>()
  const nodeSchemas = new Map<string, RegisteredPluginNodeSchema>()

  for (const manifest of manifests) {
    assertValidPluginManifest(manifest)

    if (pluginIds.has(manifest.id)) {
      throw new Error(`Duplicate plugin id: ${manifest.id}`)
    }
    pluginIds.add(manifest.id)

    for (const [nodeType, schema] of Object.entries(manifest.nodeConfigSchemas ?? {})) {
      if (nodeSchemas.has(nodeType)) {
        throw new Error(`Duplicate plugin node type: ${nodeType}`)
      }
      nodeSchemas.set(nodeType, {
        nodeType,
        pluginId: manifest.id,
        schema,
      })
    }
  }

  return {
    manifests: [...manifests],
    nodeSchemas,
  }
}

export function listConfigurableNodeTypes(registry: PluginRegistry): string[] {
  return [...registry.nodeSchemas.keys()].sort()
}

export function getNodeConfigSchema(
  registry: PluginRegistry,
  nodeType: string,
): NodeConfigSchema | undefined {
  return registry.nodeSchemas.get(nodeType)?.schema
}
