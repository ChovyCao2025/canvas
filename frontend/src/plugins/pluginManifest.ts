/**
 * Module role: frontend-only Plugin Manifest v1 types used by local schema metadata.
 *
 * Maintenance note: this module models build-time metadata only. Runtime plugin enablement
 * and handler binding remain backend-owned by the contract documents.
 */

export const PLUGIN_PERMISSION_VOCABULARY = [
  'http:external-call',
  'execution:write-context',
  'message:send',
  'coupon:grant',
  'approval:create',
  'webhook:register',
  'profile:read',
  'ai:generate',
] as const

export type PluginPermission = typeof PLUGIN_PERMISSION_VOCABULARY[number]

export type SchemaFieldType = 'text' | 'textarea' | 'number' | 'boolean' | 'select'

export interface SchemaFieldOption {
  label: string
  value: string
}

export interface SchemaConfigField {
  key: string
  label: string
  type: SchemaFieldType
  required?: boolean
  defaultValue?: unknown
  options?: SchemaFieldOption[]
  helpText?: string
}

export interface NodeConfigSchema {
  fields: SchemaConfigField[]
}

export interface JsonObjectSchema {
  type: 'object'
  properties: Record<string, unknown>
}

export interface PluginManifestV1 {
  id: string
  name: string
  version: string
  canvasCoreVersion: string
  extensionPoints: string[]
  permissions: string[]
  nodes: string[]
  templates: string[]
  configSchema: JsonObjectSchema
  nodeConfigSchemas?: Record<string, NodeConfigSchema>
}

const MANIFEST_ID_PATTERN = /^[a-z0-9]+(?:-[a-z0-9]+)*$/
const SEMVER_PATTERN = /^\d+\.\d+\.\d+(?:[-+][0-9A-Za-z.-]+)?$/
const ALLOWED_PERMISSIONS = new Set<string>(PLUGIN_PERMISSION_VOCABULARY)

export function assertValidPluginManifest(manifest: PluginManifestV1): void {
  if (!MANIFEST_ID_PATTERN.test(manifest.id)) {
    throw new Error(`Invalid plugin id: ${manifest.id}`)
  }
  if (!SEMVER_PATTERN.test(manifest.version)) {
    throw new Error(`Invalid plugin version: ${manifest.version}`)
  }

  for (const permission of manifest.permissions) {
    if (!ALLOWED_PERMISSIONS.has(permission)) {
      throw new Error(`Unsupported plugin permission: ${permission}`)
    }
  }

  const nodeTypes = new Set<string>()
  for (const nodeType of manifest.nodes) {
    if (nodeTypes.has(nodeType)) {
      throw new Error(`Duplicate node type in manifest ${manifest.id}: ${nodeType}`)
    }
    nodeTypes.add(nodeType)
  }

  for (const nodeType of Object.keys(manifest.nodeConfigSchemas ?? {})) {
    if (!nodeTypes.has(nodeType)) {
      throw new Error(`Schema declared for unknown node type ${nodeType} in manifest ${manifest.id}`)
    }
  }
}
