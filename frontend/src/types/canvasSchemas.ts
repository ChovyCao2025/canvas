import { z } from 'zod'
import { OUTLET_TARGET_FIELDS } from '../components/canvas/outletSchema'

const recordSchema = z.record(z.string(), z.unknown())
const stringMappingSchema = z.record(z.string(), z.string())
const outletTargetFieldSchema = z.enum(OUTLET_TARGET_FIELDS)

const outletSchemaItemSchema = z.object({
  id: z.string().min(1),
  label: z.string().min(1),
  color: z.string().optional(),
  targetField: outletTargetFieldSchema.optional(),
})

const outletSchemaStringSchema = z.string().superRefine((value, ctx) => {
  try {
    z.array(outletSchemaItemSchema).parse(JSON.parse(value))
  } catch {
    ctx.addIssue({ code: 'custom', message: 'Invalid outlet schema' })
  }
})

export const canvasGraphEdgeSchema = z.object({
  id: z.string().optional(),
  source: z.string().min(1),
  target: z.string().min(1),
  sourceHandle: z.string().nullable().optional(),
  targetHandle: z.string().nullable().optional(),
})

export const canvasNodeSchema = z.object({
  id: z.string().min(1),
  type: z.string().min(1),
  name: z.string().min(1),
  category: z.string().optional(),
  x: z.number(),
  y: z.number(),
  config: recordSchema.default({}),
  bizConfig: recordSchema.optional(),
  outletSchema: outletSchemaStringSchema.optional(),
})

export const canvasGraphSchema = z.object({
  nodes: z.array(canvasNodeSchema),
  edges: z.array(canvasGraphEdgeSchema).default([]),
})

export const riskDecisionActionRouteSchema = z.enum(['ALLOW', 'REVIEW', 'VERIFY', 'BLOCK', 'LIMIT', 'DELAY'])
export const riskDecisionFailPolicySchema = z.enum(['FAIL_OPEN', 'FAIL_REVIEW', 'FAIL_CLOSED'])

export const riskDecisionConfigSchema = z.object({
  sceneKey: z.string().trim().min(1),
  subjectMapping: stringMappingSchema.refine(value => Object.keys(value).length > 0, {
    message: 'At least one subject mapping is required',
  }),
  eventMapping: stringMappingSchema.refine(value => Object.keys(value).length > 0, {
    message: 'At least one event mapping is required',
  }),
  contextMapping: stringMappingSchema.default({}),
  actionRoutes: z.object({
    ALLOW: z.string().trim().min(1),
    REVIEW: z.string().trim().min(1).optional(),
    VERIFY: z.string().trim().min(1).optional(),
    BLOCK: z.string().trim().min(1).optional(),
    LIMIT: z.string().trim().min(1).optional(),
    DELAY: z.string().trim().min(1).optional(),
  }),
  failPolicy: riskDecisionFailPolicySchema.default('FAIL_REVIEW'),
  timeoutMs: z.number().int().min(10).max(500).default(50),
  includeTrace: z.boolean().default(false),
})

export const nodeTypeRegistrySchema = z.object({
  typeKey: z.string().min(1),
  typeName: z.string().min(1),
  category: z.string().min(1),
  configSchema: z.string(),
  outputSchema: z.string(),
  outletSchema: outletSchemaStringSchema.optional(),
  summaryTemplate: z.string().optional(),
  runtimePolicySchema: z.string().optional(),
  riskLevel: z.string().optional(),
  isTrigger: z.union([z.literal(0), z.literal(1)]),
  isTerminal: z.union([z.literal(0), z.literal(1)]),
  description: z.string().optional(),
  enabled: z.union([z.literal(0), z.literal(1)]),
})

export const canvasDetailSchema = z.object({
  canvas: z.object({
    id: z.number(),
    name: z.string(),
    status: z.number(),
    createdAt: z.string(),
    updatedAt: z.string(),
    triggerType: z.string().optional(),
    editVersion: z.number().optional(),
  }).passthrough(),
  graphJson: z.string(),
  draftVersionId: z.number().optional(),
}).passthrough()

export type ParsedCanvasGraph = z.infer<typeof canvasGraphSchema>
export type ParsedCanvasNode = z.infer<typeof canvasNodeSchema>
export type ParsedNodeTypeRegistry = z.infer<typeof nodeTypeRegistrySchema>

function formatIssues(error: z.ZodError): string {
  return error.issues
    .map(issue => `${issue.path.join('.') || 'root'}: ${issue.message}`)
    .join('; ')
}

export function parseCanvasGraph(value: unknown): ParsedCanvasGraph {
  const result = canvasGraphSchema.safeParse(value)
  if (!result.success) {
    throw new Error(`Invalid canvas graph: ${formatIssues(result.error)}`)
  }
  return result.data
}

export function parseCanvasGraphJson(graphJson: string | null | undefined): ParsedCanvasGraph {
  try {
    return parseCanvasGraph(JSON.parse(graphJson?.trim() ? graphJson : '{"nodes":[],"edges":[]}'))
  } catch (error) {
    if (error instanceof SyntaxError) {
      throw new Error(`Invalid canvas graph JSON: ${error.message}`)
    }
    throw error
  }
}
