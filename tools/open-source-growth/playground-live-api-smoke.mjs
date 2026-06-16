import { readFileSync } from 'node:fs'
import path from 'node:path'
import { fileURLToPath } from 'node:url'

const FIXTURE_PATH = 'tools/canvas-cli/test/fixtures/playground-new-user-welcome.json'
const DEFAULT_API_URL = 'http://localhost:8080'
const DEFAULT_TIMEOUT_MS = 5000
const DSL_MAP_PATH = '/canvas/dsl/map'

function parsePositiveInteger(value, label) {
  const parsed = Number(value)
  if (!Number.isInteger(parsed) || parsed <= 0) {
    throw new Error(`${label} must be a positive integer`)
  }
  return parsed
}

function parseArgs(argv) {
  const options = {}
  for (let index = 0; index < argv.length; index += 1) {
    const arg = argv[index]
    if (arg === '--api-url') {
      options.apiUrl = argv[++index]
    } else if (arg === '--timeout-ms') {
      options.timeoutMs = parsePositiveInteger(argv[++index], '--timeout-ms')
    } else if (arg === '--token') {
      options.token = argv[++index]
    } else if (arg === '--help' || arg === '-h') {
      options.help = true
    } else {
      throw new Error(`Unknown argument: ${arg}`)
    }
  }
  return options
}

function usage() {
  return [
    'Usage: node tools/open-source-growth/playground-live-api-smoke.mjs [--api-url <url>] [--timeout-ms <n>] [--token <token>]',
    '',
    `Default --api-url: ${DEFAULT_API_URL}`,
    `Default --timeout-ms: ${DEFAULT_TIMEOUT_MS}`,
    'CANVAS_API_URL and CANVAS_API_TOKEN are also supported.',
  ].join('\n')
}

function endpointFor(apiUrl) {
  const base = new URL(apiUrl)
  const basePath = base.pathname.replace(/\/+$/, '')
  base.pathname = `${basePath}${DSL_MAP_PATH}`
  base.search = ''
  base.hash = ''
  return base.toString()
}

function readFixture(root) {
  return JSON.parse(readFileSync(path.join(root, FIXTURE_PATH), 'utf8'))
}

function validateMappingResponse(errors, responseJson, fixture) {
  if (!responseJson || typeof responseJson !== 'object' || Array.isArray(responseJson)) {
    errors.push('response must be a JSON object')
    return
  }
  if (responseJson.templateKey !== fixture?.metadata?.name) {
    errors.push(`response templateKey must be ${fixture?.metadata?.name}`)
  }
  if (typeof responseJson.graphJson !== 'string' || responseJson.graphJson.trim() === '') {
    errors.push('response graphJson must be a non-empty string')
  }
  if (!Array.isArray(responseJson.violations)) {
    errors.push('response violations must be an array')
  } else if (responseJson.violations.length > 0) {
    errors.push(`response violations must be empty: ${JSON.stringify(responseJson.violations)}`)
  }
}

export async function verifyPlaygroundLiveApiSmoke(options = {}) {
  const root = path.resolve(options.rootDir ?? process.cwd())
  const apiUrl = options.apiUrl ?? process.env.CANVAS_API_URL ?? DEFAULT_API_URL
  const timeoutMs = options.timeoutMs ?? DEFAULT_TIMEOUT_MS
  const token = options.token ?? process.env.CANVAS_API_TOKEN
  const endpoint = endpointFor(apiUrl)
  const errors = []
  let fixture = null

  try {
    fixture = readFixture(root)
  } catch (error) {
    errors.push(`${FIXTURE_PATH} must be readable JSON: ${error.message}`)
  }

  if (!fixture) {
    return {
      ok: false,
      status: 'FAIL',
      endpoint,
      fixture: FIXTURE_PATH,
      errors,
    }
  }

  const controller = new AbortController()
  const timeout = setTimeout(() => controller.abort(), timeoutMs)
  let response
  try {
    response = await fetch(endpoint, {
      method: 'POST',
      headers: {
        'content-type': 'application/json',
        ...(token ? { authorization: `Bearer ${token}` } : {}),
      },
      body: JSON.stringify({ document: fixture }),
      signal: controller.signal,
    })
  } catch (error) {
    if (error.name === 'AbortError') {
      errors.push(`POST ${endpoint} timed out after ${timeoutMs}ms`)
    } else {
      errors.push(`POST ${endpoint} failed: ${error.message}`)
    }
    clearTimeout(timeout)
    return {
      ok: false,
      status: 'FAIL',
      endpoint,
      fixture: FIXTURE_PATH,
      errors,
    }
  }
  clearTimeout(timeout)

  const responseText = await response.text()
  if (!response.ok) {
    errors.push(`POST ${endpoint} returned HTTP ${response.status}: ${responseText.slice(0, 300)}`)
    return {
      ok: false,
      status: 'FAIL',
      endpoint,
      fixture: FIXTURE_PATH,
      errors,
    }
  }

  let responseJson
  try {
    responseJson = JSON.parse(responseText)
  } catch (error) {
    errors.push(`POST ${endpoint} returned invalid JSON: ${error.message}`)
  }
  validateMappingResponse(errors, responseJson, fixture)

  return {
    ok: errors.length === 0,
    status: errors.length === 0 ? 'PASS' : 'FAIL',
    endpoint,
    fixture: FIXTURE_PATH,
    errors,
  }
}

function printResult(result) {
  console.log(JSON.stringify({
    status: result.status,
    endpoint: result.endpoint,
    fixture: result.fixture,
    errors: result.errors,
  }))
}

const isMain = process.argv[1] && path.resolve(process.argv[1]) === fileURLToPath(import.meta.url)
if (isMain) {
  try {
    const args = parseArgs(process.argv.slice(2))
    if (args.help) {
      console.log(usage())
    } else {
      const result = await verifyPlaygroundLiveApiSmoke(args)
      printResult(result)
      if (!result.ok) {
        process.exitCode = 1
      }
    }
  } catch (error) {
    printResult({
      status: 'FAIL',
      endpoint: endpointFor(process.env.CANVAS_API_URL ?? DEFAULT_API_URL),
      fixture: FIXTURE_PATH,
      errors: [error.message],
    })
    process.exitCode = 1
  }
}
