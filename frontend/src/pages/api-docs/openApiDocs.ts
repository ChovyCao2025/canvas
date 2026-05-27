import { API_DOC_OVERRIDES, overrideKey, success } from './apiDocOverrides'
import { endpointId, type ApiDocEndpoint, type ApiDocMethod, type ApiDocParam } from './apiDocs'

type OpenApiMethod = Lowercase<ApiDocMethod>

interface OpenApiParameter {
  name?: unknown
  in?: unknown
  required?: unknown
  description?: unknown
}

interface OpenApiRequestBody {
  required?: unknown
}

interface OpenApiOperation {
  summary?: unknown
  description?: unknown
  parameters?: unknown
  requestBody?: unknown
}

interface OpenApiPathItem extends Partial<Record<OpenApiMethod, OpenApiOperation>> {
  parameters?: unknown
  [key: string]: unknown
}

export interface OpenApiDocument {
  openapi?: string
  paths?: unknown
}

export interface ParseOpenApiResult {
  endpoints: ApiDocEndpoint[]
  warnings: string[]
}

const SUPPORTED_METHODS: ApiDocMethod[] = ['GET', 'POST', 'PUT', 'DELETE']

const isRecord = (value: unknown): value is Record<string, unknown> =>
  typeof value === 'object' && value !== null && !Array.isArray(value)

const humanizePath = (path: string) =>
  path
    .split('/')
    .filter(Boolean)
    .map(part => part.replace(/[{}]/g, '').replace(/-/g, ' '))
    .join(' / ')

const pathParamNames = (path: string) => Array.from(path.matchAll(/\{([^}]+)\}/g), match => match[1])

export async function fetchOpenApiSpec(fetcher?: typeof fetch): Promise<OpenApiDocument> {
  if (!fetcher && typeof window === 'undefined') {
    throw new Error('fetchOpenApiSpec requires an injected fetcher outside the browser.')
  }

  const effectiveFetcher = fetcher ?? fetch
  const response = await effectiveFetcher('/v3/api-docs', { headers: { Accept: 'application/json' } })

  if (!response.ok) {
    throw new Error(`GET /v3/api-docs failed with HTTP ${response.status}`)
  }

  return response.json()
}

export const classifyOpenApiPath = (path: string) => {
  if (path.startsWith('/auth')) return 'auth'
  if (path === '/canvas/events/report' || path === '/canvas/trigger/behavior' || path.startsWith('/canvas/execute/')) {
    return 'external-trigger'
  }
  if (/^\/canvas\/execution\/.+\/(approve|reject)$/.test(path)) return 'approval'
  if (
    path.includes('/execution/') ||
    path.includes('/executions') ||
    path.includes('/stats') ||
    path.includes('/funnel') ||
    path.includes('/trend') ||
    path.startsWith('/canvas/dlq') ||
    path.startsWith('/canvas/execution-requests') ||
    path.startsWith('/canvas/mq-trigger-rejected')
  ) {
    return 'observability'
  }
  if (
    path.startsWith('/ops') ||
    path.startsWith('/canvas/templates') ||
    path.startsWith('/canvas/pending-reviews') ||
    path.startsWith('/canvas/async-tasks') ||
    path.startsWith('/canvas/notifications')
  ) {
    return 'operations'
  }
  if (path.startsWith('/admin/users')) return 'users'
  if (
    path.startsWith('/admin/system-options') ||
    path.includes('-definitions') ||
    path.startsWith('/canvas/audiences') ||
    path.startsWith('/canvas/data-sources') ||
    path.startsWith('/canvas/identity-types') ||
    path.startsWith('/canvas/tag-imports') ||
    path.startsWith('/canvas/tag-import-sources') ||
    path.startsWith('/cdp')
  ) {
    return 'configuration'
  }
  if (path.startsWith('/meta')) return 'metadata'

  return 'canvas'
}

export const inferOpenApiInternalFlag = (method: ApiDocMethod, path: string) => {
  if (
    path.startsWith('/auth') ||
    path === '/canvas/events/report' ||
    path === '/canvas/trigger/behavior' ||
    path.startsWith('/canvas/execute/') ||
    (method === 'POST' && /^\/canvas\/execution\/.+\/(approve|reject)$/.test(path))
  ) {
    return false
  }

  return true
}

const openApiParamToApiDocParam = (parameter: OpenApiParameter): ApiDocParam | null => {
  if (typeof parameter.name !== 'string' || (parameter.in !== 'path' && parameter.in !== 'query')) {
    return null
  }

  return {
    name: parameter.name,
    in: parameter.in,
    required: parameter.in === 'path' ? true : Boolean(parameter.required),
    desc: typeof parameter.description === 'string' ? parameter.description : `${parameter.name} 路径参数`,
  }
}

const convertParams = (parameters: unknown): ApiDocParam[] =>
  Array.isArray(parameters)
    ? parameters
      .filter(isRecord)
      .map(openApiParamToApiDocParam)
      .filter((param): param is ApiDocParam => param !== null)
    : []

const extractParams = (path: string, pathItem: OpenApiPathItem, operation: OpenApiOperation): ApiDocParam[] => {
  const paramsByKey = new Map<string, ApiDocParam>()

  for (const param of convertParams(pathItem.parameters)) {
    paramsByKey.set(`${param.in}-${param.name}`, param)
  }

  for (const param of convertParams(operation.parameters)) {
    paramsByKey.set(`${param.in}-${param.name}`, param)
  }

  const params = Array.from(paramsByKey.values())

  for (const name of pathParamNames(path)) {
    if (!params.some(param => param.in === 'path' && param.name === name)) {
      params.push({ name, in: 'path', required: true, desc: `${name} 路径参数` })
    }
  }

  if (isRecord(operation.requestBody)) {
    params.push({
      name: 'body',
      in: 'body',
      required: Boolean((operation.requestBody as OpenApiRequestBody).required),
      desc: 'Request body',
    })
  }

  return params
}

const parseOperation = (
  path: string,
  method: ApiDocMethod,
  pathItem: OpenApiPathItem,
  operation: OpenApiOperation,
): ApiDocEndpoint => {
  const override = API_DOC_OVERRIDES[overrideKey(method, path)]
  const summary = typeof operation.summary === 'string'
    ? operation.summary
    : typeof operation.description === 'string'
      ? operation.description
      : `${method} ${path}`

  return {
    id: endpointId(method, path),
    title: override?.title ?? humanizePath(path),
    method,
    path,
    category: override?.category ?? classifyOpenApiPath(path),
    summary: override?.summary ?? summary,
    auth: override?.auth ?? (path === '/auth/login' ? 'none' : 'bearer'),
    internal: override?.internal ?? inferOpenApiInternalFlag(method, path),
    params: override?.params ?? extractParams(path, pathItem, operation),
    requestExample: override?.requestExample,
    responseExample: override?.responseExample ?? success({ id: 'demo_id', status: 'ok' }),
  }
}

export const parseOpenApiEndpoints = (spec: OpenApiDocument): ParseOpenApiResult => {
  if (!isRecord(spec.paths)) {
    return { endpoints: [], warnings: ['OpenAPI document does not contain a paths object.'] }
  }

  const endpoints: ApiDocEndpoint[] = []
  const warnings: string[] = []

  for (const [path, pathItem] of Object.entries(spec.paths)) {
    if (!isRecord(pathItem)) {
      warnings.push(`Skipped malformed path item: ${path}`)
      continue
    }

    for (const method of SUPPORTED_METHODS) {
      const operation = (pathItem as OpenApiPathItem)[method.toLowerCase() as OpenApiMethod]
      if (operation === undefined) continue

      if (!isRecord(operation)) {
        warnings.push(`Skipped malformed path item: ${path}`)
        continue
      }

      endpoints.push(parseOperation(path, method, pathItem, operation))
    }
  }

  endpoints.sort((left, right) => {
    const pathCompare = left.path.localeCompare(right.path)
    if (pathCompare !== 0) return pathCompare
    return SUPPORTED_METHODS.indexOf(left.method) - SUPPORTED_METHODS.indexOf(right.method)
  })

  return { endpoints, warnings }
}
