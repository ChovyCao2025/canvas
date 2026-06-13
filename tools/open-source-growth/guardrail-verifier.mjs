import { existsSync, readdirSync, readFileSync, statSync } from 'node:fs'
import path from 'node:path'

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
