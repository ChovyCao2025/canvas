import { existsSync, readdirSync, readFileSync, statSync } from 'node:fs'
import path from 'node:path'
import { fileURLToPath } from 'node:url'

const FIXTURE_PATH = 'tools/canvas-cli/test/fixtures/playground-new-user-welcome.json'
const PLAYGROUND_DOC_PATH = 'docs/open-source/playground.md'
const SMOKE_COMMAND = 'node tools/open-source-growth/playground-runtime-smoke.mjs'
const REQUIRED_PLUGINS = [
  'canvas-plugin-webhook',
  'canvas-plugin-coupon',
  'canvas-plugin-message',
]

function readIfExists(file) {
  return existsSync(file) ? readFileSync(file, 'utf8') : ''
}

function readJson(errors, label, file) {
  if (!existsSync(file)) {
    errors.push(`${label} is required`)
    return null
  }
  try {
    return JSON.parse(readFileSync(file, 'utf8'))
  } catch (error) {
    errors.push(`${label} must be valid JSON: ${error.message}`)
    return null
  }
}

function listFiles(dir) {
  if (!existsSync(dir)) {
    return []
  }
  const files = []
  for (const entry of readdirSync(dir)) {
    const full = path.join(dir, entry)
    const stat = statSync(full)
    if (stat.isDirectory()) {
      files.push(...listFiles(full))
    } else {
      files.push(full)
    }
  }
  return files
}

function collectWireMockMappings(root, errors) {
  const mappingsDir = path.join(root, 'wiremock/mappings')
  if (!existsSync(mappingsDir)) {
    errors.push('wiremock/mappings is required')
    return []
  }
  const mappings = []
  for (const file of listFiles(mappingsDir).filter(candidate => candidate.endsWith('.json'))) {
    const relative = path.relative(root, file)
    const parsed = readJson(errors, relative, file)
    if (!parsed) {
      continue
    }
    if (Array.isArray(parsed.mappings)) {
      mappings.push(...parsed.mappings.map(mapping => ({ ...mapping, source: relative })))
    } else {
      mappings.push({ ...parsed, source: relative })
    }
  }
  return mappings
}

function findMapping(mappings, url) {
  return mappings.find(mapping => mapping?.request?.url === url)
}

function requireNode(errors, nodes, id, type) {
  const node = nodes.find(candidate => candidate?.id === id)
  if (!node || node.type !== type) {
    errors.push(`fixture must include ${id} node with type ${type}`)
    return null
  }
  return node
}

function requireEdge(errors, edges, from, to) {
  if (!edges.some(edge => edge?.from === from && edge?.to === to)) {
    errors.push(`fixture must include ${from} -> ${to} edge`)
  }
}

function verifyFixture(errors, root) {
  const fixture = readJson(errors, FIXTURE_PATH, path.join(root, FIXTURE_PATH))
  if (!fixture) {
    return null
  }
  if (fixture.apiVersion !== 'canvas/v1') {
    errors.push('fixture apiVersion must be canvas/v1')
  }
  if (fixture.kind !== 'Journey') {
    errors.push('fixture kind must be Journey')
  }
  if (fixture?.metadata?.name !== 'new-user-welcome') {
    errors.push('fixture metadata.name must be new-user-welcome')
  }

  const nodes = Array.isArray(fixture?.spec?.nodes) ? fixture.spec.nodes : []
  const edges = Array.isArray(fixture?.spec?.edges) ? fixture.spec.edges : []
  const segment = requireNode(errors, nodes, 'segment', 'condition')
  requireNode(errors, nodes, 'coupon', 'coupon.grant')
  requireNode(errors, nodes, 'message', 'message.send')
  requireEdge(errors, edges, 'segment', 'coupon')
  requireEdge(errors, edges, 'coupon', 'message')

  const expression = segment?.config?.expression
  if (expression !== 'user.lifecycleStage == "new"') {
    errors.push('segment expression must be user.lifecycleStage == "new"')
  }
  const samplePayload = fixture.samplePayload
  if (!samplePayload || samplePayload?.user?.lifecycleStage !== 'new') {
    errors.push('samplePayload must satisfy segment expression for lifecycleStage new')
  }

  return fixture
}

function verifyWireMock(errors, root) {
  const mappings = collectWireMockMappings(root, errors)
  const goldenPath = findMapping(mappings, '/mock/demo/golden-path')
  const templatesMapping = findMapping(mappings, '/mock/demo/templates')
  const pluginsMapping = findMapping(mappings, '/mock/demo/plugins')

  if (!goldenPath) {
    errors.push('wiremock mappings must include /mock/demo/golden-path')
  }
  if (!templatesMapping) {
    errors.push('wiremock mappings must include /mock/demo/templates')
  }
  if (!pluginsMapping) {
    errors.push('wiremock mappings must include /mock/demo/plugins')
  }

  if (goldenPath) {
    const body = JSON.stringify(goldenPath.response?.jsonBody ?? {})
    if (!body.includes('new-user-welcome')) {
      errors.push('/mock/demo/golden-path must reference new-user-welcome')
    }
    if (!body.includes('mock AI risk audit')) {
      errors.push('/mock/demo/golden-path must reference mock AI risk audit')
    }
  }

  const templates = templatesMapping?.response?.jsonBody?.templates
  const plugins = pluginsMapping?.response?.jsonBody?.plugins
  if (templatesMapping && !Array.isArray(templates)) {
    errors.push('/mock/demo/templates must return a templates array')
  }
  if (pluginsMapping && !Array.isArray(plugins)) {
    errors.push('/mock/demo/plugins must return a plugins array')
  }
  if (!Array.isArray(templates) || !Array.isArray(plugins)) {
    return
  }

  const welcome = templates.find(template => template?.key === 'new-user-welcome')
  if (!welcome) {
    errors.push('/mock/demo/templates must include new-user-welcome')
  } else {
    for (const plugin of REQUIRED_PLUGINS) {
      if (!welcome.requiredPlugins?.includes(plugin)) {
        errors.push(`/mock/demo/templates new-user-welcome must require ${plugin}`)
      }
    }
  }

  const enabledPlugins = new Set(
    plugins
      .filter(plugin => plugin?.enabled === true && plugin?.mode === 'mock')
      .map(plugin => plugin.key),
  )
  for (const plugin of REQUIRED_PLUGINS) {
    if (!enabledPlugins.has(plugin)) {
      errors.push(`/mock/demo/plugins must enable ${plugin} in mock mode`)
    }
  }
}

function verifyDocs(errors, root) {
  const doc = readIfExists(path.join(root, PLAYGROUND_DOC_PATH))
  if (!doc) {
    errors.push(`${PLAYGROUND_DOC_PATH} is required`)
    return
  }
  if (!doc.includes(SMOKE_COMMAND)) {
    errors.push(`${PLAYGROUND_DOC_PATH} must reference ${SMOKE_COMMAND}`)
  }
  if (/runtime smoke remains (?:a )?(?:final )?.*follow-up/i.test(doc) || /runtime smoke remains a future task/i.test(doc)) {
    errors.push(`${PLAYGROUND_DOC_PATH} must not frame local runtime smoke as only a future task`)
  }
}

export function verifyPlaygroundRuntimeSmoke(rootDir = process.cwd()) {
  const root = path.resolve(rootDir)
  const errors = []
  const fixture = verifyFixture(errors, root)
  verifyWireMock(errors, root)
  verifyDocs(errors, root)

  return {
    ok: errors.length === 0,
    errors,
    summary: {
      fixture: fixture?.metadata?.name ?? null,
      path: 'segment -> coupon -> message',
      requiredPlugins: REQUIRED_PLUGINS,
    },
  }
}

function printResult(result) {
  const payload = {
    status: result.ok ? 'PASS' : 'FAIL',
    summary: result.summary,
    errors: result.errors,
  }
  console.log(JSON.stringify(payload, null, 2))
}

const isMain = process.argv[1] && path.resolve(process.argv[1]) === fileURLToPath(import.meta.url)
if (isMain) {
  const result = verifyPlaygroundRuntimeSmoke(process.cwd())
  printResult(result)
  if (!result.ok) {
    process.exitCode = 1
  }
}
