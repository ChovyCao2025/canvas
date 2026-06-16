#!/usr/bin/env node

import { readFileSync } from 'node:fs'
import http from 'node:http'
import https from 'node:https'
import { basename } from 'node:path'

const PUBLISH_GATED_MESSAGE = 'Publish is gated until a stable backend publish API is verified; import/export preview is available after G10 unlock.'

const usage = `Canvas CLI

Usage:
  canvas-cli --help
  canvas-cli validate <file>
  canvas-cli diff <before> <after>
  canvas-cli import <file> --api-url <url>
  canvas-cli export <canvasId> --api-url <url> --tenant-id <tenantId>

Commands:
  validate <file>        Validate a local Canvas DSL v1 JSON Journey document.
  diff <before> <after>  Summarize added, removed, and changed node ids locally.
  import <file>          POST a local Canvas DSL document to /canvas/dsl/import.
  export <canvasId>      GET a Canvas DSL document from /canvas/dsl/export/{canvasId}.

Options:
  --api-url <url>        Backend base URL. Defaults to CANVAS_API_URL.
  --tenant-id <id>       Tenant id for export. Defaults to CANVAS_TENANT_ID.

Current boundary:
  import and export are unlocked after G10 public extension/API stability.
  publish remains blocked until a stable backend publish API is verified.
  validate and diff are local-only and do not call backend APIs.`

function readJson(file) {
  try {
    return JSON.parse(readFileSync(file, 'utf8'))
  } catch (error) {
    throw new Error(`cannot read ${file}: ${error.message}`)
  }
}

function isObject(value) {
  return value !== null && typeof value === 'object' && !Array.isArray(value)
}

function isNonEmptyString(value) {
  return typeof value === 'string' && value.trim().length > 0
}

function validateJourney(document) {
  const errors = []

  if (!isObject(document)) {
    return ['document must be a JSON object']
  }

  if (document.apiVersion !== 'canvas/v1') {
    errors.push('apiVersion must be canvas/v1')
  }
  if (document.kind !== 'Journey') {
    errors.push('kind must be Journey')
  }
  if (!isObject(document.metadata) || document.metadata.name === undefined) {
    errors.push('metadata.name is required')
  } else if (!isNonEmptyString(document.metadata.name)) {
    errors.push('metadata.name must be a non-empty string')
  }
  if (!isObject(document.spec)) {
    errors.push('spec is required')
    return errors
  }
  if (!isObject(document.spec.trigger)) {
    errors.push('spec.trigger is required')
  }
  if (!Array.isArray(document.spec.nodes) || document.spec.nodes.length === 0) {
    errors.push('spec.nodes must be a non-empty array')
    return errors
  }

  const ids = new Set()
  document.spec.nodes.forEach((node, index) => {
    if (!isObject(node)) {
      errors.push(`spec.nodes[${index}] must be an object`)
      return
    }
    if (node.id === undefined) {
      errors.push(`spec.nodes[${index}].id is required`)
    } else if (!isNonEmptyString(node.id)) {
      errors.push(`spec.nodes[${index}].id must be a non-empty string`)
    } else if (ids.has(node.id)) {
      errors.push(`spec.nodes[${index}].id must be unique: ${node.id}`)
    } else {
      ids.add(node.id)
    }
    if (node.type === undefined) {
      errors.push(`spec.nodes[${index}].type is required`)
    } else if (!isNonEmptyString(node.type)) {
      errors.push(`spec.nodes[${index}].type must be a non-empty string`)
    }
  })

  return errors
}

function nodeMap(document) {
  const nodes = isObject(document?.spec) && Array.isArray(document.spec.nodes)
    ? document.spec.nodes
    : []
  return new Map(nodes.filter((node) => isObject(node) && node.id).map((node) => [node.id, node]))
}

function stable(value) {
  if (Array.isArray(value)) {
    return value.map(stable)
  }
  if (isObject(value)) {
    return Object.fromEntries(Object.keys(value).sort().map((key) => [key, stable(value[key])]))
  }
  return value
}

function canonicalJson(value) {
  return JSON.stringify(stable(value))
}

function formatList(ids) {
  return ids.length === 0 ? 'none' : ids.join(', ')
}

function parseGlobalOptions(args) {
  const commandArgs = []
  const options = {
    apiUrl: process.env.CANVAS_API_URL,
    tenantId: process.env.CANVAS_TENANT_ID
  }

  for (let index = 0; index < args.length; index += 1) {
    const arg = args[index]

    if (arg === '--api-url') {
      const value = args[index + 1]
      if (!value) {
        throw new Error('--api-url requires a value')
      }
      options.apiUrl = value
      index += 1
      continue
    }

    if (arg.startsWith('--api-url=')) {
      const value = arg.slice('--api-url='.length)
      if (!value) {
        throw new Error('--api-url requires a value')
      }
      options.apiUrl = value
      continue
    }

    if (arg === '--tenant-id') {
      const value = args[index + 1]
      if (!value) {
        throw new Error('--tenant-id requires a value')
      }
      options.tenantId = value
      index += 1
      continue
    }

    if (arg.startsWith('--tenant-id=')) {
      const value = arg.slice('--tenant-id='.length)
      if (!value) {
        throw new Error('--tenant-id requires a value')
      }
      options.tenantId = value
      continue
    }

    commandArgs.push(arg)
  }

  return {
    commandArgs,
    options
  }
}

function requireApiUrl(options) {
  if (!isNonEmptyString(options.apiUrl)) {
    throw new Error('--api-url or CANVAS_API_URL is required')
  }
  return options.apiUrl
}

function requireTenantId(options) {
  if (!isNonEmptyString(options.tenantId)) {
    throw new Error('--tenant-id or CANVAS_TENANT_ID is required for export')
  }
  return options.tenantId
}

function requestJson(apiUrl, method, pathname, body, headers = {}) {
  const base = new URL(apiUrl)
  const target = new URL(pathname, base)
  const transport = target.protocol === 'https:' ? https : http
  const payload = body === undefined ? undefined : JSON.stringify(body)

  return new Promise((resolve, reject) => {
    const request = transport.request(target, {
      method,
      headers: {
        accept: 'application/json',
        ...(payload === undefined ? {} : {
          'content-type': 'application/json',
          'content-length': Buffer.byteLength(payload)
        }),
        ...headers
      }
    }, (response) => {
      const chunks = []
      response.on('data', (chunk) => {
        chunks.push(chunk)
      })
      response.on('end', () => {
        const text = Buffer.concat(chunks).toString('utf8')
        let parsed = null
        if (text.trim()) {
          try {
            parsed = JSON.parse(text)
          } catch (error) {
            reject(new Error(`backend returned invalid JSON: ${error.message}`))
            return
          }
        }
        if (response.statusCode < 200 || response.statusCode >= 300) {
          reject(new Error(`backend request failed with ${response.statusCode}: ${text}`))
          return
        }
        resolve(parsed)
      })
    })

    request.on('error', reject)
    if (payload !== undefined) {
      request.write(payload)
    }
    request.end()
  })
}

function unwrapCompatibilityEnvelope(response) {
  if (isObject(response) && Object.hasOwn(response, 'result')) {
    return response.result
  }
  return response
}

function runValidate(file) {
  const document = readJson(file)
  const errors = validateJourney(document)
  if (errors.length > 0) {
    console.error(`Invalid Canvas DSL v1 document: ${file}`)
    for (const error of errors) {
      console.error(`- ${error}`)
    }
    return 1
  }

  console.log(`${basename(file)} is valid`)
  return 0
}

function runDiff(beforeFile, afterFile) {
  const before = nodeMap(readJson(beforeFile))
  const after = nodeMap(readJson(afterFile))

  const added = [...after.keys()].filter((id) => !before.has(id)).sort()
  const removed = [...before.keys()].filter((id) => !after.has(id)).sort()
  const changed = [...after.keys()]
    .filter((id) => before.has(id) && canonicalJson(before.get(id)) !== canonicalJson(after.get(id)))
    .sort()

  console.log(`Added nodes: ${formatList(added)}`)
  console.log(`Removed nodes: ${formatList(removed)}`)
  console.log(`Changed nodes: ${formatList(changed)}`)
  return 0
}

async function runImport(file, options) {
  const document = readJson(file)
  const response = await requestJson(requireApiUrl(options), 'POST', '/canvas/dsl/import', {
    document
  })
  console.log(JSON.stringify(unwrapCompatibilityEnvelope(response), null, 2))
  return 0
}

async function runExport(canvasId, options) {
  if (!isNonEmptyString(canvasId)) {
    throw new Error('export requires a canvas id')
  }
  const response = await requestJson(
    requireApiUrl(options),
    'GET',
    `/canvas/dsl/export/${encodeURIComponent(canvasId)}`,
    undefined,
    {
      'x-tenant-id': requireTenantId(options)
    }
  )
  console.log(JSON.stringify(unwrapCompatibilityEnvelope(response), null, 2))
  return 0
}

async function main(args) {
  try {
    const { commandArgs, options } = parseGlobalOptions(args)
    const [command, ...rest] = commandArgs

    if (!command || command === '--help' || command === '-h') {
      console.log(usage)
      return 0
    }
    if (command === 'validate' && rest.length === 1) {
      return runValidate(rest[0])
    }
    if (command === 'diff' && rest.length === 2) {
      return runDiff(rest[0], rest[1])
    }
    if (command === 'import' && rest.length === 1) {
      return await runImport(rest[0], options)
    }
    if (command === 'export' && rest.length === 1) {
      return await runExport(rest[0], options)
    }
    if (command === 'publish') {
      console.error(PUBLISH_GATED_MESSAGE)
      return 1
    }

    console.error(usage)
    return 1
  } catch (error) {
    console.error(error.message)
    return 1
  }
}

process.exitCode = await main(process.argv.slice(2))
