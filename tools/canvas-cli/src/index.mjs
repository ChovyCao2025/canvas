#!/usr/bin/env node

import { readFileSync } from 'node:fs'
import { basename } from 'node:path'

const DEFAULT_API_URL = 'http://localhost:8080'
const IMPORT_PATH = '/canvas/dsl/map'

const usage = `Canvas CLI

Usage:
  canvas-cli [--api-url <url>] --help
  canvas-cli validate <file>
  canvas-cli import <file>
  canvas-cli export <canvasId>
  canvas-cli diff <before> <after>
  canvas-cli publish <canvasId>

Commands:
  validate <file>        Validate a local Canvas DSL v1 JSON Journey document.
  import <file>          POST { document } to /canvas/dsl/map for import preview.
  export <canvasId>      GET /canvas/dsl/export/<canvasId> and print JSON.
  diff <before> <after>  Summarize added, removed, and changed node ids locally.
  publish <canvasId>     POST /canvas/<canvasId>/publish and print JSON.

Options:
  --api-url <url>        Backend API base URL. Overrides CANVAS_API_URL.
                         Default: ${DEFAULT_API_URL}

validate and diff are local-only. import, export, and publish call backend APIs.`

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
  let apiUrl

  for (let index = 0; index < args.length; index += 1) {
    const arg = args[index]

    if (arg === '--api-url') {
      const value = args[index + 1]
      if (!value) {
        throw new Error('--api-url requires a value')
      }
      apiUrl = value
      index += 1
      continue
    }

    if (arg.startsWith('--api-url=')) {
      const value = arg.slice('--api-url='.length)
      if (!value) {
        throw new Error('--api-url requires a value')
      }
      apiUrl = value
      continue
    }

    commandArgs.push(arg)
  }

  return {
    apiUrl: apiUrl ?? process.env.CANVAS_API_URL ?? DEFAULT_API_URL,
    commandArgs
  }
}

function buildApiUrl(apiUrl, path) {
  const normalizedBase = apiUrl.endsWith('/') ? apiUrl : `${apiUrl}/`
  const normalizedPath = path.replace(/^\/+/, '')
  return new URL(normalizedPath, normalizedBase).toString()
}

async function requestJson(apiUrl, method, path, body) {
  let url
  try {
    url = buildApiUrl(apiUrl, path)
  } catch (error) {
    throw new Error(`invalid API URL ${apiUrl}: ${error.message}`)
  }

  const options = {
    method,
    headers: {
      accept: 'application/json'
    }
  }

  if (body !== undefined) {
    options.headers['content-type'] = 'application/json'
    options.body = JSON.stringify(body)
  }

  let response
  try {
    response = await fetch(url, options)
  } catch (error) {
    throw new Error(`API request failed: ${method} ${path}: ${error.message}`)
  }

  const responseText = await response.text()
  if (!response.ok) {
    const details = responseText.trim().length > 0 ? `: ${responseText}` : ''
    throw new Error(`API request failed: ${method} ${path} returned ${response.status}${details}`)
  }

  if (responseText.trim().length === 0) {
    return {}
  }

  try {
    return JSON.parse(responseText)
  } catch (error) {
    throw new Error(`API response from ${method} ${path} was not valid JSON: ${error.message}`)
  }
}

function printJson(value) {
  console.log(JSON.stringify(value, null, 2))
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

async function runImport(file, apiUrl) {
  const document = readJson(file)
  const response = await requestJson(apiUrl, 'POST', IMPORT_PATH, { document })
  printJson(response)
  return 0
}

async function runExport(canvasId, apiUrl) {
  const path = `/canvas/dsl/export/${encodeURIComponent(canvasId)}`
  const response = await requestJson(apiUrl, 'GET', path)
  printJson(response)
  return 0
}

async function runPublish(canvasId, apiUrl) {
  const path = `/canvas/${encodeURIComponent(canvasId)}/publish`
  const response = await requestJson(apiUrl, 'POST', path)
  printJson(response)
  return 0
}

async function main(args) {
  try {
    const { apiUrl, commandArgs } = parseGlobalOptions(args)
    const [command, ...rest] = commandArgs

    if (!command || command === '--help' || command === '-h') {
      console.log(usage)
      return 0
    }
    if (command === 'validate' && rest.length === 1) {
      return runValidate(rest[0])
    }
    if (command === 'import' && rest.length === 1) {
      return await runImport(rest[0], apiUrl)
    }
    if (command === 'export' && rest.length === 1) {
      return await runExport(rest[0], apiUrl)
    }
    if (command === 'diff' && rest.length === 2) {
      return runDiff(rest[0], rest[1])
    }
    if (command === 'publish' && rest.length === 1) {
      return await runPublish(rest[0], apiUrl)
    }

    console.error(usage)
    return 1
  } catch (error) {
    console.error(error.message)
    return 1
  }
}

process.exitCode = await main(process.argv.slice(2))
