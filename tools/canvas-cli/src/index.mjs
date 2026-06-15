#!/usr/bin/env node

import { readFileSync } from 'node:fs'
import { basename } from 'node:path'

const G10_GATED_MESSAGE = 'Backend API commands are gated until G10 public extension/API stability passes; use local validate and diff for now.'

const usage = `Canvas CLI

Usage:
  canvas-cli --help
  canvas-cli validate <file>
  canvas-cli diff <before> <after>

Commands:
  validate <file>        Validate a local Canvas DSL v1 JSON Journey document.
  diff <before> <after>  Summarize added, removed, and changed node ids locally.

Current boundary:
  import, export, and publish are blocked until G10 public extension/API stability passes.
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

  for (let index = 0; index < args.length; index += 1) {
    const arg = args[index]

    if (arg === '--api-url') {
      const value = args[index + 1]
      if (!value) {
        throw new Error('--api-url requires a value')
      }
      index += 1
      continue
    }

    if (arg.startsWith('--api-url=')) {
      const value = arg.slice('--api-url='.length)
      if (!value) {
        throw new Error('--api-url requires a value')
      }
      continue
    }

    commandArgs.push(arg)
  }

  return {
    commandArgs
  }
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

async function main(args) {
  try {
    const { commandArgs } = parseGlobalOptions(args)
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
    if (['import', 'export', 'publish'].includes(command)) {
      console.error(G10_GATED_MESSAGE)
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
