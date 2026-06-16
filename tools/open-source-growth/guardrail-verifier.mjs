import { existsSync, readdirSync, readFileSync, statSync } from 'node:fs'
import path from 'node:path'
import vm from 'node:vm'

import { verifyG10PublicApiStability } from './g10-public-api-stability.mjs'
import { verifyPlaygroundRuntimeSmoke } from './playground-runtime-smoke.mjs'

const REQUIRED_DOCS = [
  'README.md',
  'open-source-growth-spec.md',
  'open-source-growth-plan.md',
  'implementation-guardrails.md',
  'traceability-matrix.md',
  'phase-gates.md',
  'decision-log.md',
  'milestone-roadmap.md',
  'success-metrics.md',
]

const REQUIRED_CONTRACTS = [
  'README.md',
  'node-handler-contract.md',
  'plugin-manifest-v1.md',
  'template-pack-v1.md',
  'canvas-dsl-v1.md',
  'demo-profile-contract.md',
  'ai-operator-contract.md',
]

const README_LINKS = [
  'open-source-growth-spec.md',
  'open-source-growth-plan.md',
  'implementation-guardrails.md',
  'traceability-matrix.md',
  'phase-gates.md',
  'decision-log.md',
  'contracts/README.md',
  'success-metrics.md',
]

const PLAN_REFERENCES = [
  '../program-coordination/README.md',
  '../program-coordination/execution-readiness-audit.md',
  '../program-coordination/gate-verification-matrix.md',
  '../program-coordination/max-parallel-subagent-execution-plan.md',
  'implementation-guardrails.md',
  'traceability-matrix.md',
  'phase-gates.md',
  'decision-log.md',
  'mini-spec',
  'Target backend state',
  'CURRENT_ENGINE_BRIDGE',
  'DDD_FINAL_MODULE',
]

const GUARDRAIL_REFERENCES = [
  'PluginRegistryService',
  'HandlerRegistry',
  'TemplateRenderService',
  'mini-spec',
  'License',
  'classloader',
]

const TRACEABILITY_IDS = [
  'OSG-ENTRY-001',
  'OSG-PLUGIN-001',
  'OSG-DSL-001',
  'OSG-AI-001',
]

const PHASE_GATES = ['Month 1', 'Month 2', 'Month 3', 'Month 4', 'Month 5', 'Month 6']
const FORBIDDEN_RUNTIME_LOADING = ['URLClassLoader', 'PF4J']
const DEMO_COMPOSE_SERVICES = ['mysql', 'redis', 'wiremock', 'rocketmq-namesrv', 'rocketmq-broker']
const DEMO_WIREMOCK_ENDPOINTS = [
  '/mock/demo/golden-path',
  '/mock/demo/templates',
  '/mock/demo/plugins',
  '/mock/message/sms',
  '/mock/message/email',
  '/mock/approval/start',
  '/mock/ai/audit',
]
const PR_TEMPLATE_REFERENCES = [
  'OSG requirement',
  'Phase gate',
  'implementation-guardrails.md',
  'guardrail-verifier.mjs',
]
const CODEOWNERS_REFERENCES = [
  '/docs/open-source-growth/',
  '/tools/open-source-growth/',
]

const CI_WORKFLOW_FILES = [
  '.github/workflows/ci.yml',
  '.github/workflows/canvas-ci.yml',
]
const PLAYGROUND_RUNTIME_SMOKE = 'tools/open-source-growth/playground-runtime-smoke.mjs'
const PLAYGROUND_RUNTIME_SMOKE_COMMAND = `node ${PLAYGROUND_RUNTIME_SMOKE}`
const PLAYGROUND_LIVE_API_SMOKE = 'tools/open-source-growth/playground-live-api-smoke.mjs'
const PLAYGROUND_LIVE_API_SMOKE_COMMAND = `node ${PLAYGROUND_LIVE_API_SMOKE} --api-url http://localhost:8080`
const G10_PUBLIC_API_STABILITY = 'tools/open-source-growth/g10-public-api-stability.mjs'

const CLI_PUBLISH_GATED_MESSAGE = 'Publish is gated until a stable backend publish API is verified'
const REQUIRED_CLI_IMPORT_EXPORT_SURFACES = [
  { text: 'canvas-cli import <file>', label: 'import command in usage' },
  { text: 'canvas-cli export <canvasId>', label: 'export command in usage' },
  { text: '/canvas/dsl/import', label: 'Canvas DSL import backend path' },
  { text: '/canvas/dsl/export/', label: 'Canvas DSL export backend path' },
]
const FORBIDDEN_CLI_BACKEND_API_SURFACES = [
  { text: 'canvas-cli publish <canvasId>', label: 'publish command in usage' },
  { text: 'fetch(', label: 'network fetch call' },
  { text: '/canvas/dsl/validate', label: 'Canvas DSL validate backend path' },
  { text: '/canvas/dsl/map', label: 'Canvas DSL map backend path' },
  { text: '/canvas/dsl/diff', label: 'Canvas DSL diff backend path' },
  { text: '/publish', label: 'publish backend path' },
]

const PROGRAM_COORDINATION_DOCS = [
  'README.md',
  'progress-ledger.md',
  'dispatch-state.json',
  'collaboration-and-recovery-protocol.md',
  'execution-readiness-audit.md',
  'gate-verification-matrix.md',
  'max-parallel-subagent-execution-plan.md',
  'execution-sequencing.md',
  'subagent-worker-packets.md',
]

const CONTRACT_PLACEMENT_DOCS = [
  'node-handler-contract.md',
  'plugin-manifest-v1.md',
  'template-pack-v1.md',
  'canvas-dsl-v1.md',
  'demo-profile-contract.md',
  'ai-operator-contract.md',
]

const SPEC_BACKEND_AUTHORITY_REFERENCES = [
  'Backend Path Authority',
  'temporary bridge only',
  'Target backend state',
  'CURRENT_ENGINE_BRIDGE',
  'DDD_FINAL_MODULE',
  'final DDD owner module',
  'worker packet',
  'progress-ledger.md',
]

const ROADMAP_COORDINATION_REFERENCES = [
  'DDD Coordination Gate',
  'OSG-C07',
  'G10',
  'subagent-worker-packets.md',
  'progress-ledger.md',
]

const TRACEABILITY_COORDINATION_REFERENCES = [
  'Target backend state',
  'worker packet ID',
  'final owner module',
  'bridge removal gate',
  'progress-ledger.md dispatch row',
]

const BRIDGE_DECLARATION_REFERENCES = [
  'Bridge Declaration',
  'exact old service/API',
  'exact old files',
  'final DDD owner module',
  'idempotency rule',
  'removal gate',
  'rollback path',
]

const PARALLEL_SYSTEM_REFERENCES = [
  'coordinator decision',
  'temporary adapter',
  'final DDD module',
  'parallel registry/contract surfaces',
]

function readIfExists(file) {
  return existsSync(file) ? readFileSync(file, 'utf8') : ''
}

function readJsonIfExists(errors, fileLabel, file) {
  if (!existsSync(file)) {
    errors.push(`${fileLabel} is required`)
    return null
  }
  try {
    return JSON.parse(readFileSync(file, 'utf8'))
  } catch (error) {
    errors.push(`${fileLabel} must be valid JSON: ${error.message}`)
    return null
  }
}

function listFiles(dir) {
  if (!existsSync(dir)) {
    return []
  }
  const out = []
  for (const entry of readdirSync(dir)) {
    const full = path.join(dir, entry)
    const stat = statSync(full)
    if (stat.isDirectory()) {
      out.push(...listFiles(full))
    } else {
      out.push(full)
    }
  }
  return out
}

function listSourceRoots(root) {
  const roots = []
  const backendDir = path.join(root, 'backend')
  if (existsSync(backendDir)) {
    for (const entry of readdirSync(backendDir)) {
      const candidate = path.join(backendDir, entry, 'src/main/java')
      if (existsSync(candidate) && statSync(candidate).isDirectory()) {
        roots.push(candidate)
      }
    }
  }
  const frontendSrc = path.join(root, 'frontend/src')
  if (existsSync(frontendSrc)) {
    roots.push(frontendSrc)
  }
  return roots
}

function extractTaskSections(content, headingPrefix) {
  const sections = []
  const headingPattern = /^### ([^\n]+)$/gm
  let match
  while ((match = headingPattern.exec(content)) !== null) {
    if (!match[1].startsWith(headingPrefix)) {
      continue
    }
    const start = match.index
    const bodyStart = headingPattern.lastIndex
    const nextMatch = /^### /gm
    nextMatch.lastIndex = bodyStart
    const next = nextMatch.exec(content)
    sections.push({
      heading: match[1],
      body: content.slice(start, next ? next.index : undefined),
    })
  }
  return sections
}

function requireContains(errors, fileLabel, content, needle) {
  if (!content.includes(needle)) {
    errors.push(`${fileLabel} must reference ${needle}`)
  }
}

function requireDocContains(errors, fileLabel, content, needle) {
  if (!content.includes(needle)) {
    errors.push(`${fileLabel} must include ${needle}`)
  }
}

function requireBackendPathAuthority(errors, fileLabel, content) {
  if (!content.includes('backend/canvas-engine')) {
    return
  }
  for (const reference of [
    'Backend Path Authority',
    'CURRENT_ENGINE_BRIDGE',
    'worker packet',
    'owner module',
  ]) {
    requireContains(errors, fileLabel, content, reference)
  }
}

function requireSectionBridgeDeclarations(errors, workerPackets) {
  const sections = extractTaskSections(workerPackets, 'OSG-W')
  for (const section of sections) {
    if (!section.body.includes('backend/canvas-engine')) {
      continue
    }
    if (!section.body.includes('CURRENT_ENGINE_BRIDGE')) {
      continue
    }
    for (const reference of BRIDGE_DECLARATION_REFERENCES) {
      if (!section.body.includes(reference)) {
        errors.push(`${section.heading} section must include ${reference}`)
      }
    }
  }
}

function collectWireMockMappings(root) {
  const mappingsDir = path.join(root, 'wiremock/mappings')
  if (!existsSync(mappingsDir)) {
    return { mappings: [], errors: [] }
  }
  const errors = []
  const mappings = []
  for (const file of listFiles(mappingsDir).filter(candidate => candidate.endsWith('.json'))) {
    const relative = path.relative(root, file)
    const parsed = readJsonIfExists(errors, relative, file)
    if (!parsed) {
      continue
    }
    if (Array.isArray(parsed.mappings)) {
      mappings.push(...parsed.mappings.map(mapping => ({ ...mapping, source: relative })))
    } else {
      mappings.push({ ...parsed, source: relative })
    }
  }
  return { mappings, errors }
}

function findMappingByUrl(mappings, url) {
  return mappings.find(mapping => mapping?.request?.url === url)
}

function extractQuotedStrings(content) {
  const values = []
  const pattern = /['"]([^'"]+)['"]/g
  let match
  while ((match = pattern.exec(content)) !== null) {
    values.push(match[1])
  }
  return values
}

function extractFrontendTemplateCatalog(root) {
  const catalog = readIfExists(path.join(root, 'frontend/src/pages/canvas-list/templateCatalog.ts'))
  const errors = []
  const entries = new Map()
  const evaluated = evaluateFrontendTemplateCatalog(catalog)
  errors.push(...evaluated.errors)
  const evaluatedEntries = evaluated.entries
  for (const template of evaluatedEntries) {
    if (!template?.key) {
      continue
    }
    entries.set(template.key, {
      key: template.key,
      riskLevel: template.riskLevel,
      requiredPlugins: Array.isArray(template.requiredPlugins) ? template.requiredPlugins : [],
      docs: template.docs,
      canvas: template.canvas,
      samplePayload: template.samplePayload,
    })
  }
  if (catalog && evaluated.foundCatalog && entries.size === 0) {
    errors.push('frontend template catalog could not be evaluated into official template entries')
  }
  return { entries, errors }
}

function arraysEqual(left, right) {
  return left.length === right.length && left.every((value, index) => value === right[index])
}

function evaluateFrontendTemplateCatalog(catalog) {
  const catalogExpression = extractOfficialCatalogExpression(catalog)
  if (!catalogExpression.foundCatalog) {
    return { entries: [], errors: [], foundCatalog: false }
  }
  if (catalogExpression.error) {
    return {
      entries: [],
      errors: [`frontend template catalog could not be parsed: ${catalogExpression.error}`],
      foundCatalog: true,
    }
  }
  const declarations = collectCatalogConstDeclarations(catalog, catalogExpression.start)
  try {
    const entries = vm.runInNewContext(`
      function journey(key, title, trigger, nodes, edges) {
        return {
          apiVersion: 'canvas/v1',
          kind: 'Journey',
          metadata: { name: key, title },
          spec: { trigger, nodes, edges },
        }
      }
      function node(id, type, label, config) {
        return { id, type, label, config }
      }
      function edge(from, to, when) {
        return { from, to, when }
      }
      function trace(nodeId, nodeType, outcome, summary) {
        return { nodeId, nodeType, outcome, summary }
      }
      ${declarations.join('\n')}
      const officialTemplateCatalog = ${catalogExpression.expression}
      officialTemplateCatalog
    `, {}, { timeout: 1000 })
    if (!Array.isArray(entries)) {
      return {
        entries: [],
        errors: ['frontend template catalog evaluation did not return an array'],
        foundCatalog: true,
      }
    }
    return { entries, errors: [], foundCatalog: true }
  } catch (error) {
    return {
      entries: [],
      errors: [`frontend template catalog evaluation failed: ${error.message}`],
      foundCatalog: true,
    }
  }
}

function extractOfficialCatalogExpression(catalog) {
  const catalogMatch = /\bofficialTemplateCatalog\b/.exec(catalog)
  if (!catalogMatch) {
    return { foundCatalog: false, start: -1, expression: '', error: '' }
  }
  const assignmentIndex = catalog.indexOf('=', catalogMatch.index)
  if (assignmentIndex === -1) {
    return {
      foundCatalog: true,
      start: catalogMatch.index,
      expression: '',
      error: 'officialTemplateCatalog assignment is missing',
    }
  }
  const start = skipWhitespace(catalog, assignmentIndex + 1)
  if (catalog[start] !== '[') {
    return {
      foundCatalog: true,
      start,
      expression: '',
      error: 'officialTemplateCatalog assignment must be an array literal',
    }
  }
  const end = findBalancedExpressionEnd(catalog, start)
  if (end === -1) {
    return {
      foundCatalog: true,
      start,
      expression: '',
      error: 'officialTemplateCatalog array literal is not balanced',
    }
  }
  return {
    foundCatalog: true,
    start: catalogMatch.index,
    expression: catalog.slice(start, end + 1),
    error: '',
  }
}

function collectCatalogConstDeclarations(catalog, beforeIndex) {
  const declarations = []
  const declarationPattern = /\b(?:export\s+)?const\s+([A-Za-z_$][\w$]*)/g
  let match
  while ((match = declarationPattern.exec(catalog)) !== null && match.index < beforeIndex) {
    const name = match[1]
    if (name === 'officialTemplateCatalog') {
      continue
    }
    let cursor = skipWhitespace(catalog, declarationPattern.lastIndex)
    if (catalog[cursor] === ':') {
      cursor = catalog.indexOf('=', cursor)
    }
    if (cursor === -1 || catalog[cursor] !== '=') {
      continue
    }
    const expressionStart = skipWhitespace(catalog, cursor + 1)
    const expressionEnd = findConstExpressionEnd(catalog, expressionStart)
    if (expressionEnd === -1) {
      continue
    }
    declarations.push(`const ${name} = ${catalog.slice(expressionStart, expressionEnd + 1)}`)
    declarationPattern.lastIndex = expressionEnd + 1
  }
  return declarations
}

function skipWhitespace(content, index) {
  let cursor = index
  while (cursor < content.length && /\s/.test(content[cursor])) {
    cursor += 1
  }
  return cursor
}

function findConstExpressionEnd(content, start) {
  const first = content[start]
  if (['[', '{', '('].includes(first)) {
    return findBalancedExpressionEnd(content, start)
  }
  if (first === '"' || first === "'" || first === '`') {
    return skipStringLiteral(content, start) - 1
  }
  let cursor = start
  while (cursor < content.length && content[cursor] !== '\n' && content[cursor] !== ';') {
    cursor += 1
  }
  return cursor > start ? cursor - 1 : -1
}

function findBalancedExpressionEnd(content, start) {
  const pairs = new Map([
    ['[', ']'],
    ['{', '}'],
    ['(', ')'],
  ])
  const opening = content[start]
  if (!pairs.has(opening)) {
    return -1
  }
  const stack = [pairs.get(opening)]
  let cursor = start + 1
  while (cursor < content.length) {
    const char = content[cursor]
    const next = content[cursor + 1]
    if (char === '"' || char === "'" || char === '`') {
      cursor = skipStringLiteral(content, cursor)
      continue
    }
    if (char === '/' && next === '/') {
      cursor = skipLineComment(content, cursor + 2)
      continue
    }
    if (char === '/' && next === '*') {
      cursor = skipBlockComment(content, cursor + 2)
      continue
    }
    if (pairs.has(char)) {
      stack.push(pairs.get(char))
      cursor += 1
      continue
    }
    if (char === stack[stack.length - 1]) {
      stack.pop()
      if (stack.length === 0) {
        return cursor
      }
    }
    cursor += 1
  }
  return -1
}

function skipStringLiteral(content, start) {
  const quote = content[start]
  let cursor = start + 1
  while (cursor < content.length) {
    if (content[cursor] === '\\') {
      cursor += 2
      continue
    }
    if (content[cursor] === quote) {
      return cursor + 1
    }
    cursor += 1
  }
  return content.length
}

function skipLineComment(content, start) {
  const end = content.indexOf('\n', start)
  return end === -1 ? content.length : end + 1
}

function skipBlockComment(content, start) {
  const end = content.indexOf('*/', start)
  return end === -1 ? content.length : end + 2
}

function jsonEqual(left, right) {
  return JSON.stringify(left) === JSON.stringify(right)
}

function requirePlaygroundGoldenPathFixture(errors, root, frontendTemplates) {
  const expectedTemplate = frontendTemplates.get('new-user-welcome')
  if (!expectedTemplate?.canvas) {
    errors.push('frontend template catalog must define new-user-welcome canvas')
    return
  }
  const fixture = readJsonIfExists(
    errors,
    'tools/canvas-cli/test/fixtures/playground-new-user-welcome.json',
    path.join(root, 'tools/canvas-cli/test/fixtures/playground-new-user-welcome.json'),
  )
  if (!fixture) {
    return
  }
  const expectedCanvas = expectedTemplate.canvas
  const expectedNodes = Array.isArray(expectedCanvas?.spec?.nodes) ? expectedCanvas.spec.nodes : []
  const expectedEdges = Array.isArray(expectedCanvas?.spec?.edges) ? expectedCanvas.spec.edges : []
  const nodes = Array.isArray(fixture?.spec?.nodes) ? fixture.spec.nodes : []
  const edges = Array.isArray(fixture?.spec?.edges) ? fixture.spec.edges : []
  const nodeIds = nodes.map(node => node?.id)
  if (fixture?.apiVersion !== expectedCanvas?.apiVersion) {
    errors.push(`playground-new-user-welcome fixture apiVersion must match frontend catalog canvas apiVersion ${expectedCanvas?.apiVersion}`)
  }
  if (fixture?.kind !== expectedCanvas?.kind) {
    errors.push(`playground-new-user-welcome fixture kind must match frontend catalog canvas kind ${expectedCanvas?.kind}`)
  }
  if (fixture?.metadata?.name !== expectedCanvas?.metadata?.name) {
    errors.push(`playground-new-user-welcome fixture metadata.name must match frontend catalog canvas metadata.name ${expectedCanvas?.metadata?.name}`)
  }
  if (!jsonEqual(fixture?.metadata, expectedCanvas?.metadata)) {
    errors.push('playground-new-user-welcome fixture metadata must match frontend catalog canvas metadata')
  }
  if (!jsonEqual(fixture?.spec?.trigger, expectedCanvas?.spec?.trigger)) {
    errors.push('playground-new-user-welcome fixture trigger must match frontend catalog canvas trigger')
  }
  for (const nodeId of ['segment', 'coupon', 'message']) {
    if (!nodeIds.includes(nodeId)) {
      errors.push(`playground-new-user-welcome fixture must include ${nodeId} node`)
    }
  }
  for (const expectedNode of expectedNodes) {
    const actualNode = nodes.find(node => node?.id === expectedNode.id)
    if (!actualNode) {
      continue
    }
    if (!jsonEqual(actualNode, expectedNode)) {
      errors.push(`playground-new-user-welcome ${expectedNode.id} node must match frontend catalog node type ${expectedNode.type}`)
    }
  }
  for (const [from, to] of [['segment', 'coupon'], ['coupon', 'message']]) {
    if (!edges.some(edge => edge?.from === from && edge?.to === to)) {
      errors.push(`playground-new-user-welcome fixture must include ${from} -> ${to} edge`)
    }
  }
  for (const expectedEdge of expectedEdges) {
    if (!edges.some(edge => jsonEqual(edge, expectedEdge))) {
      errors.push(`playground-new-user-welcome ${expectedEdge.from} -> ${expectedEdge.to} edge must match frontend catalog`)
    }
  }
  if (expectedTemplate.samplePayload && !jsonEqual(fixture?.samplePayload, expectedTemplate.samplePayload)) {
    errors.push('playground-new-user-welcome samplePayload must match frontend catalog samplePayload')
  }
}

function requireCliImportExportUnlockWithPublishGate(errors, root) {
  const cliSourceDir = path.join(root, 'tools/canvas-cli/src')
  const cliSourcePath = path.join(root, 'tools/canvas-cli/src/index.mjs')
  const cliSource = readIfExists(cliSourcePath)
  if (!cliSource) {
    errors.push('tools/canvas-cli/src/index.mjs is required for G10 CLI import/export unlock gate')
    return
  }
  const cliSources = listFiles(cliSourceDir)
    .filter(file => file.endsWith('.mjs') || file.endsWith('.js'))
    .map(file => readFileSync(file, 'utf8'))
    .join('\n')
  if (!cliSource.includes(CLI_PUBLISH_GATED_MESSAGE)) {
    errors.push('canvas-cli must gate publish until a stable backend publish API is verified')
  }
  for (const surface of REQUIRED_CLI_IMPORT_EXPORT_SURFACES) {
    if (!cliSources.includes(surface.text)) {
      errors.push(`canvas-cli must include ${surface.label} after G10 import/export unlock`)
    }
  }
  for (const surface of FORBIDDEN_CLI_BACKEND_API_SURFACES) {
    if (cliSources.includes(surface.text)) {
      errors.push(`canvas-cli must stay within G10 import/export unlock; remove ${surface.label}`)
    }
  }
}

function workflowCommandLabel(command) {
  return typeof command === 'string' ? command : command.label
}

function normalizeWorkflowScalar(value) {
  const trimmed = value.trim()
  if (
    (trimmed.startsWith('"') && trimmed.endsWith('"')) ||
    (trimmed.startsWith("'") && trimmed.endsWith("'"))
  ) {
    return trimmed.slice(1, -1)
  }
  return trimmed
}

function extractWorkflowSteps(jobBody) {
  const steps = []
  let current = null
  for (const line of jobBody.split(/\r?\n/)) {
    const itemMatch = line.match(/^      -\s+(.*)$/)
    if (itemMatch) {
      current = { run: '', workingDirectory: '' }
      steps.push(current)
      const runMatch = itemMatch[1].match(/^run:\s*(.*)$/)
      if (runMatch) {
        current.run = normalizeWorkflowScalar(runMatch[1])
      }
      continue
    }
    if (!current) {
      continue
    }
    const runMatch = line.match(/^        run:\s*(.*)$/)
    if (runMatch) {
      current.run = normalizeWorkflowScalar(runMatch[1])
      continue
    }
    const workingDirectoryMatch = line.match(/^        working-directory:\s*(.*)$/)
    if (workingDirectoryMatch) {
      current.workingDirectory = normalizeWorkflowScalar(workingDirectoryMatch[1])
    }
  }
  return steps
}

function workflowStepMatchesCommand(step, command) {
  if (typeof command === 'string') {
    return step.run === command
  }
  if (command.kind === 'canvas-cli-test') {
    return (
      step.run === 'cd tools/canvas-cli && npm test' ||
      (step.run === 'npm test' && step.workingDirectory === 'tools/canvas-cli')
    )
  }
  return false
}

function requireOrderedWorkflowCommands(errors, file, steps, commands) {
  let cursor = -1
  for (const command of commands) {
    const index = steps.findIndex((step, stepIndex) => (
      stepIndex > cursor && workflowStepMatchesCommand(step, command)
    ))
    if (index === -1) {
      errors.push(`${file} must run ${workflowCommandLabel(command)} before publishing OSG guardrail results`)
      continue
    }
    cursor = index
  }
}

function requireCanvasCliCiGate(errors, root) {
  for (const file of CI_WORKFLOW_FILES) {
    const content = readIfExists(path.join(root, file))
    if (!content) {
      errors.push(`${file} is required for Open Source Growth CI gates`)
      continue
    }
    const guardrailJob = extractWorkflowJobBody(content, 'open-source-growth-guardrails')
    if (!guardrailJob) {
      errors.push(`${file} must define open-source-growth-guardrails job`)
      continue
    }
    requireOrderedWorkflowCommands(errors, file, extractWorkflowSteps(guardrailJob), [
      {
        label: 'tools/canvas-cli npm test',
        kind: 'canvas-cli-test',
      },
      'docker compose -f docker-compose.demo.yml config',
      PLAYGROUND_RUNTIME_SMOKE_COMMAND,
      'node --test tools/open-source-growth/guardrail-verifier.test.mjs',
      'node tools/open-source-growth/guardrail-verifier.mjs',
    ])
  }
}

function extractWorkflowJobBody(content, jobName) {
  const lines = content.split(/\r?\n/)
  let inJobs = false
  let inJob = false
  const body = []

  for (const line of lines) {
    if (/^jobs:\s*$/.test(line)) {
      inJobs = true
      continue
    }
    if (!inJobs) {
      continue
    }
    if (/^[^ \t#][^:]*:\s*$/.test(line)) {
      break
    }

    const jobMatch = line.match(/^  ([A-Za-z0-9_-]+):\s*$/)
    if (jobMatch) {
      if (inJob) {
        break
      }
      inJob = jobMatch[1] === jobName
      continue
    }
    if (inJob) {
      body.push(line)
    }
  }

  return body.length > 0 ? body.join('\n') : ''
}

function requireDemoReadinessGate(errors, root) {
  const composeFile = path.join(root, 'docker-compose.demo.yml')
  const compose = readIfExists(composeFile)
  if (!compose) {
    errors.push('docker-compose.demo.yml is required')
  }
  for (const service of DEMO_COMPOSE_SERVICES) {
    if (!new RegExp(`^\\s{2}${service}:\\s*$`, 'm').test(compose)) {
      errors.push(`docker-compose.demo.yml must define ${service} service`)
    }
  }

  const collected = collectWireMockMappings(root)
  errors.push(...collected.errors)
  const mappings = collected.mappings
  for (const endpoint of DEMO_WIREMOCK_ENDPOINTS) {
    if (!findMappingByUrl(mappings, endpoint)) {
      errors.push(`wiremock mappings must include ${endpoint}`)
    }
  }

  const templatesMapping = findMappingByUrl(mappings, '/mock/demo/templates')
  const pluginsMapping = findMappingByUrl(mappings, '/mock/demo/plugins')
  const goldenPathMapping = findMappingByUrl(mappings, '/mock/demo/golden-path')
  const templates = templatesMapping?.response?.jsonBody?.templates
  const plugins = pluginsMapping?.response?.jsonBody?.plugins
  const goldenPathBody = goldenPathMapping?.response?.jsonBody

  if (!Array.isArray(templates)) {
    errors.push('/mock/demo/templates must return a templates array')
  }
  if (!Array.isArray(plugins)) {
    errors.push('/mock/demo/plugins must return a plugins array')
  }
  if (goldenPathBody && !JSON.stringify(goldenPathBody).includes('mock AI risk audit')) {
    errors.push('/mock/demo/golden-path must include mock AI risk audit coverage')
  }

  if (!Array.isArray(templates) || !Array.isArray(plugins)) {
    return
  }

  const enabledPlugins = new Set(
    plugins
      .filter(plugin => plugin?.enabled === true && plugin?.mode === 'mock')
      .map(plugin => plugin.key),
  )
  const frontendCatalog = extractFrontendTemplateCatalog(root)
  errors.push(...frontendCatalog.errors)
  const frontendTemplates = frontendCatalog.entries
  if (!templates.some(template => template?.key === 'new-user-welcome')) {
    errors.push('/mock/demo/templates must include new-user-welcome')
  }
  if (frontendTemplates.size === 0) {
    errors.push('frontend template catalog must define official templates')
  }
  const wireMockTemplatesByKey = new Map(templates.filter(template => template?.key).map(template => [template.key, template]))
  const frontendTemplateKeys = new Set(frontendTemplates.keys())
  for (const template of templates) {
    if (template?.key && frontendTemplates.size > 0 && !frontendTemplateKeys.has(template.key)) {
      errors.push(`/mock/demo/templates contains non-frontend official template ${template.key}`)
    }
  }
  for (const frontendTemplate of frontendTemplates.values()) {
    const wireMockTemplate = wireMockTemplatesByKey.get(frontendTemplate.key)
    if (!wireMockTemplate) {
      errors.push(`/mock/demo/templates must include frontend official template ${frontendTemplate.key}`)
      continue
    }
    if (!arraysEqual(wireMockTemplate.requiredPlugins || [], frontendTemplate.requiredPlugins)) {
      errors.push(`${frontendTemplate.key} WireMock requiredPlugins must match frontend catalog: ${frontendTemplate.requiredPlugins.join(', ')}`)
    }
    if (frontendTemplate.riskLevel && wireMockTemplate.riskLevel !== frontendTemplate.riskLevel) {
      errors.push(`${frontendTemplate.key} WireMock riskLevel must match frontend catalog: ${frontendTemplate.riskLevel}`)
    }
    if (frontendTemplate.docs && wireMockTemplate.docs !== frontendTemplate.docs) {
      errors.push(`${frontendTemplate.key} WireMock docs path must match frontend catalog: ${frontendTemplate.docs}`)
    }
  }
  const frontendRequiredPlugins = new Set([...frontendTemplates.values()].flatMap(template => template.requiredPlugins))
  for (const plugin of frontendRequiredPlugins) {
    if (!enabledPlugins.has(plugin)) {
      errors.push(`/mock/demo/plugins must enable frontend required plugin ${plugin} in mock mode`)
    }
  }
  requirePlaygroundGoldenPathFixture(errors, root, frontendTemplates)
  requirePlaygroundRuntimeSmokeGate(errors, root)
  requireCliImportExportUnlockWithPublishGate(errors, root)
  requireCanvasCliCiGate(errors, root)
  for (const template of templates) {
    if (!template?.key) {
      errors.push('/mock/demo/templates entries must include key')
      continue
    }
    if (template.docs && !existsSync(path.join(root, template.docs))) {
      errors.push(`${template.key} demo template docs path is missing: ${template.docs}`)
    }
    for (const plugin of template.requiredPlugins || []) {
      if (!enabledPlugins.has(plugin)) {
        errors.push(`${template.key} requires demo plugin ${plugin}, but /mock/demo/plugins does not enable it in mock mode`)
      }
    }
  }
}

function requirePlaygroundRuntimeSmokeGate(errors, root) {
  const smokeScript = readIfExists(path.join(root, PLAYGROUND_RUNTIME_SMOKE))
  if (!smokeScript) {
    errors.push(`${PLAYGROUND_RUNTIME_SMOKE} is required`)
  }
  const liveSmokeScript = readIfExists(path.join(root, PLAYGROUND_LIVE_API_SMOKE))
  if (!liveSmokeScript) {
    errors.push(`${PLAYGROUND_LIVE_API_SMOKE} is required`)
  }
  const smokeResult = verifyPlaygroundRuntimeSmoke(root)
  for (const error of smokeResult.errors) {
    if (!errors.includes(error)) {
      errors.push(error)
    }
  }
  const playgroundDoc = readIfExists(path.join(root, 'docs/open-source/playground.md'))
  if (!playgroundDoc) {
    errors.push('docs/open-source/playground.md is required')
    return
  }
  if (!playgroundDoc.includes(PLAYGROUND_RUNTIME_SMOKE_COMMAND)) {
    errors.push(`docs/open-source/playground.md must reference ${PLAYGROUND_RUNTIME_SMOKE_COMMAND}`)
  }
  if (!playgroundDoc.includes(PLAYGROUND_LIVE_API_SMOKE_COMMAND)) {
    errors.push(`docs/open-source/playground.md must reference ${PLAYGROUND_LIVE_API_SMOKE_COMMAND}`)
  }
  if (/runtime smoke remains (?:a )?(?:final )?.*follow-up/i.test(playgroundDoc) || /runtime smoke remains a future task/i.test(playgroundDoc)) {
    errors.push('docs/open-source/playground.md must not frame local runtime smoke as only a future task')
  }
}

export function verifyOpenSourceGrowthGuardrails(rootDir = process.cwd()) {
  const root = path.resolve(rootDir)
  const errors = []
  const osgDir = path.join(root, 'docs/open-source-growth')
  const contractsDir = path.join(osgDir, 'contracts')
  const coordinationDir = path.join(root, 'docs/program-coordination')

  for (const file of REQUIRED_DOCS) {
    if (!existsSync(path.join(osgDir, file))) {
      errors.push(`docs/open-source-growth/${file} is required`)
    }
  }
  for (const file of REQUIRED_CONTRACTS) {
    if (!existsSync(path.join(contractsDir, file))) {
      errors.push(`docs/open-source-growth/contracts/${file} is required`)
    }
  }
  for (const file of PROGRAM_COORDINATION_DOCS) {
    if (!existsSync(path.join(coordinationDir, file))) {
      errors.push(`docs/program-coordination/${file} is required`)
    }
  }

  const rootIndex = readIfExists(path.join(root, 'docs/INDEX.md'))
  if (!rootIndex.includes('open-source-growth/README.md')) {
    errors.push('docs/INDEX.md must link to open-source-growth/README.md')
  }
  if (!rootIndex.includes('program-coordination/README.md')) {
    errors.push('docs/INDEX.md must link to program-coordination/README.md')
  }
  if (rootIndex.includes('product-evolution/open-source-growth')) {
    errors.push('docs/INDEX.md must not link to product-evolution/open-source-growth')
  }

  const prTemplate = readIfExists(path.join(root, '.github/pull_request_template.md'))
  if (!prTemplate) {
    errors.push('.github/pull_request_template.md is required')
  }
  for (const reference of PR_TEMPLATE_REFERENCES) {
    requireContains(errors, 'pull_request_template.md', prTemplate, reference)
  }

  const codeowners = readIfExists(path.join(root, '.github/CODEOWNERS'))
  if (!codeowners) {
    errors.push('.github/CODEOWNERS is required')
  }
  for (const reference of CODEOWNERS_REFERENCES) {
    requireContains(errors, 'CODEOWNERS', codeowners, reference)
  }

  const readme = readIfExists(path.join(osgDir, 'README.md'))
  for (const link of README_LINKS) {
    requireContains(errors, 'README.md', readme, link)
  }

  const plan = readIfExists(path.join(osgDir, 'open-source-growth-plan.md'))
  for (const reference of PLAN_REFERENCES) {
    requireContains(errors, 'open-source-growth-plan.md', plan, reference)
  }
  requireBackendPathAuthority(errors, 'open-source-growth-plan.md', plan)

  const spec = readIfExists(path.join(osgDir, 'open-source-growth-spec.md'))
  for (const reference of SPEC_BACKEND_AUTHORITY_REFERENCES) {
    requireContains(errors, 'open-source-growth-spec.md', spec, reference)
  }
  requireBackendPathAuthority(errors, 'open-source-growth-spec.md', spec)

  const milestoneRoadmap = readIfExists(path.join(osgDir, 'milestone-roadmap.md'))
  for (const reference of ROADMAP_COORDINATION_REFERENCES) {
    requireContains(errors, 'milestone-roadmap.md', milestoneRoadmap, reference)
  }

  for (const file of CONTRACT_PLACEMENT_DOCS) {
    const content = readIfExists(path.join(contractsDir, file))
    requireContains(errors, `contracts/${file}`, content, 'Backend Placement / Owner')
    requireContains(errors, `contracts/${file}`, content, 'CURRENT_ENGINE_BRIDGE')
    requireContains(errors, `contracts/${file}`, content, 'DDD_FINAL_MODULE')
    requireContains(errors, `contracts/${file}`, content, 'Mirror documents')
  }

  const guardrails = readIfExists(path.join(osgDir, 'implementation-guardrails.md'))
  for (const reference of GUARDRAIL_REFERENCES) {
    requireContains(errors, 'implementation-guardrails.md', guardrails, reference)
  }
  for (const reference of PARALLEL_SYSTEM_REFERENCES) {
    requireContains(errors, 'implementation-guardrails.md', guardrails, reference)
  }
  requireDemoReadinessGate(errors, root)

  const g10Verifier = readIfExists(path.join(root, G10_PUBLIC_API_STABILITY))
  if (!g10Verifier.trim()) {
    errors.push(`${G10_PUBLIC_API_STABILITY} is required`)
  }
  errors.push(...verifyG10PublicApiStability(root).errors)

  const traceability = readIfExists(path.join(osgDir, 'traceability-matrix.md'))
  for (const id of TRACEABILITY_IDS) {
    requireDocContains(errors, 'traceability-matrix.md', traceability, id)
  }
  for (const reference of TRACEABILITY_COORDINATION_REFERENCES) {
    requireContains(errors, 'traceability-matrix.md', traceability, reference)
  }

  const workerPackets = readIfExists(path.join(coordinationDir, 'subagent-worker-packets.md'))
  for (const reference of BRIDGE_DECLARATION_REFERENCES) {
    requireContains(errors, 'subagent-worker-packets.md', workerPackets, reference)
  }
  requireSectionBridgeDeclarations(errors, workerPackets)

  const phaseGates = readIfExists(path.join(osgDir, 'phase-gates.md'))
  for (const gate of PHASE_GATES) {
    requireDocContains(errors, 'phase-gates.md', phaseGates, gate)
  }
  requireContains(
    errors,
    'phase-gates.md',
    phaseGates,
    'bash docs/program-coordination/checks/program-coordination-checks.sh .',
  )
  requireContains(errors, 'phase-gates.md', phaseGates, 'Target backend state')
  requireContains(errors, 'phase-gates.md', phaseGates, 'CURRENT_ENGINE_BRIDGE')
  requireContains(errors, 'phase-gates.md', phaseGates, 'DDD_FINAL_MODULE')

  const allDocs = listFiles(path.join(root, 'docs'))
  for (const file of allDocs) {
    const content = readIfExists(file)
    if (content.includes('product-evolution/open-source-growth')) {
      errors.push(`${path.relative(root, file)} must not reference product-evolution/open-source-growth`)
    }
  }

  for (const sourceRoot of listSourceRoots(root)) {
    for (const file of listFiles(sourceRoot)) {
      const content = readIfExists(file)
      for (const keyword of FORBIDDEN_RUNTIME_LOADING) {
        if (content.includes(keyword)) {
          errors.push(`${path.relative(root, file)} contains forbidden runtime plugin loading keyword ${keyword}`)
        }
      }
    }
  }

  return { ok: errors.length === 0, errors }
}

if (import.meta.url === `file://${process.argv[1]}`) {
  const result = verifyOpenSourceGrowthGuardrails(process.argv[2] || process.cwd())
  if (!result.ok) {
    console.error(result.errors.join('\n'))
    process.exit(1)
  }
  console.log(JSON.stringify({ ok: true }, null, 2))
}
