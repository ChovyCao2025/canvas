import assert from 'node:assert/strict'
import { mkdirSync, writeFileSync } from 'node:fs'
import { mkdtemp } from 'node:fs/promises'
import { tmpdir } from 'node:os'
import path from 'node:path'
import test from 'node:test'

import { verifyOpenSourceGrowthGuardrails } from './guardrail-verifier.mjs'

const BRIDGE_DECLARATION = [
  'Bridge Declaration required before editing old canvas-engine files:',
  '  exact old service/API:',
  '  exact old files:',
  '  final DDD owner module:',
  '  idempotency rule:',
  '  removal gate:',
  '  rollback path:',
]

async function fixture() {
  const root = await mkdtemp(path.join(tmpdir(), 'osg-guardrails-'))
  mkdirSync(path.join(root, 'docs/open-source-growth/contracts'), { recursive: true })
  mkdirSync(path.join(root, '.github'), { recursive: true })
  mkdirSync(path.join(root, 'backend/canvas-engine/src/main/java/org/chovy/canvas/engine/plugin'), { recursive: true })
  mkdirSync(path.join(root, 'backend/canvas-engine/src/main/java/org/chovy/canvas/engine/handler'), { recursive: true })
  mkdirSync(path.join(root, 'backend/canvas-engine/src/main/java/org/chovy/canvas/engine/template'), { recursive: true })
  mkdirSync(path.join(root, 'backend/canvas-context-execution/src/main/java/org/chovy/canvas/execution/plugin'), { recursive: true })

  mkdirSync(path.join(root, 'docs/program-coordination'), { recursive: true })

  writeFileSync(path.join(root, 'docs/INDEX.md'), [
    '[open-source-growth/README.md](./open-source-growth/README.md)',
    '[program-coordination/README.md](./program-coordination/README.md)',
  ].join('\n'))
  for (const file of [
    'README.md',
    'progress-ledger.md',
    'dispatch-state.json',
    'collaboration-and-recovery-protocol.md',
    'execution-readiness-audit.md',
    'gate-verification-matrix.md',
    'max-parallel-subagent-execution-plan.md',
    'execution-sequencing.md',
    'subagent-worker-packets.md',
  ]) {
    writeFileSync(path.join(root, 'docs/program-coordination', file), file)
  }
  writeFileSync(path.join(root, 'docs/program-coordination/subagent-worker-packets.md'), [
    'Bridge Declaration',
    'exact old service/API',
    'exact old files',
    'final DDD owner module',
    'idempotency rule',
    'removal gate',
    'rollback path',
    '',
    '### OSG-W02: Demo Shell And Mock Catalog',
    '',
    '```text',
    'Program: Open Source Growth',
    'Task id: OSG-W02',
    'Target backend state: DOCS_ONLY or CURRENT_ENGINE_BRIDGE for demo seed only',
    ...BRIDGE_DECLARATION,
    'Allowed write scope:',
    '  backend/canvas-engine/src/main/resources/application-demo.yml only when the Bridge Declaration assigns this exact file',
    '```',
    '',
    '### OSG-W10: Canvas DSL Backend',
    '',
    '```text',
    'Program: Open Source Growth',
    'Task id: OSG-W10',
    'Target backend state: DDD_FINAL_MODULE, or CURRENT_ENGINE_BRIDGE only with a complete Bridge Declaration',
    ...BRIDGE_DECLARATION,
    'Allowed write scope:',
    '  backend/canvas-context-canvas/src/main/java/org/chovy/canvas/canvas/api/dsl/**',
    '```',
  ].join('\n'))
  writeFileSync(path.join(root, '.github/CODEOWNERS'), '/docs/open-source-growth/ @photonpay\n/tools/open-source-growth/ @photonpay\n')
  writeFileSync(path.join(root, '.github/pull_request_template.md'), [
    'OSG requirement',
    'Phase gate',
    'implementation-guardrails.md',
    'guardrail-verifier.mjs',
  ].join('\n'))
  writeFileSync(path.join(root, 'docs/open-source-growth/README.md'), [
    '[open-source-growth-spec.md](./open-source-growth-spec.md)',
    '[open-source-growth-plan.md](./open-source-growth-plan.md)',
    '[implementation-guardrails.md](./implementation-guardrails.md)',
    '[traceability-matrix.md](./traceability-matrix.md)',
    '[phase-gates.md](./phase-gates.md)',
    '[decision-log.md](./decision-log.md)',
    '[contracts/README.md](./contracts/README.md)',
    '[success-metrics.md](./success-metrics.md)',
  ].join('\n'))
  writeFileSync(path.join(root, 'docs/open-source-growth/open-source-growth-spec.md'), [
    'Spec',
    'Backend Path Authority',
    'backend/canvas-engine/src/main/java/example',
    'temporary bridge only',
    'Target backend state',
    'CURRENT_ENGINE_BRIDGE',
    'DDD_FINAL_MODULE',
    'final DDD owner module',
    'worker packet',
    'progress-ledger.md',
  ].join('\n'))
  writeFileSync(path.join(root, 'docs/open-source-growth/open-source-growth-plan.md'), [
    'Backend Path Authority',
    'backend/canvas-engine/src/main/java/example',
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
    'worker packet',
    'owner module',
  ].join('\n'))
  writeFileSync(path.join(root, 'docs/open-source-growth/implementation-guardrails.md'), [
    'PluginRegistryService',
    'HandlerRegistry',
    'TemplateRenderService',
    'mini-spec',
    'License',
    'classloader',
    'coordinator decision',
    'temporary adapter',
    'final DDD module',
    'parallel registry/contract surfaces',
  ].join('\n'))
  writeFileSync(path.join(root, 'docs/open-source-growth/traceability-matrix.md'), [
    'OSG-ENTRY-001',
    'OSG-PLUGIN-001',
    'OSG-DSL-001',
    'OSG-AI-001',
    'Target backend state',
    'worker packet ID',
    'final owner module',
    'bridge removal gate',
    'progress-ledger.md dispatch row',
  ].join('\n'))
  writeFileSync(path.join(root, 'docs/open-source-growth/phase-gates.md'), [
    'Month 1',
    'Month 2',
    'Month 3',
    'Month 4',
    'Month 5',
    'Month 6',
    'bash docs/program-coordination/checks/program-coordination-checks.sh .',
    'Target backend state',
    'CURRENT_ENGINE_BRIDGE',
    'DDD_FINAL_MODULE',
  ].join('\n'))
  writeFileSync(path.join(root, 'docs/open-source-growth/decision-log.md'), 'Decision Log\n')
  writeFileSync(path.join(root, 'docs/open-source-growth/milestone-roadmap.md'), [
    'Roadmap',
    'DDD Coordination Gate',
    'OSG-C07',
    'G10',
    'subagent-worker-packets.md',
    'progress-ledger.md',
  ].join('\n'))
  writeFileSync(path.join(root, 'docs/open-source-growth/success-metrics.md'), 'Metrics')
  for (const file of [
    'README.md',
    'node-handler-contract.md',
    'plugin-manifest-v1.md',
    'template-pack-v1.md',
    'canvas-dsl-v1.md',
    'demo-profile-contract.md',
    'ai-operator-contract.md',
  ]) {
    const placement = [
      file,
      'Backend Placement / Owner',
      'CURRENT_ENGINE_BRIDGE',
      'DDD_FINAL_MODULE',
      'Mirror documents',
    ].join('\n')
    writeFileSync(path.join(root, 'docs/open-source-growth/contracts', file), placement)
  }
  writeFileSync(path.join(root, 'backend/canvas-engine/src/main/java/org/chovy/canvas/engine/plugin/PluginRegistryService.java'), 'class PluginRegistryService {}')
  writeFileSync(path.join(root, 'backend/canvas-engine/src/main/java/org/chovy/canvas/engine/handler/HandlerRegistry.java'), 'class HandlerRegistry {}')
  writeFileSync(path.join(root, 'backend/canvas-engine/src/main/java/org/chovy/canvas/engine/template/TemplateRenderService.java'), 'class TemplateRenderService {}')
  return root
}

test('accepts a repository with all anti-drift artifacts wired together', async () => {
  const root = await fixture()

  const result = verifyOpenSourceGrowthGuardrails(root)

  assert.equal(result.ok, true)
  assert.deepEqual(result.errors, [])
})

test('rejects missing traceability matrix', async () => {
  const root = await fixture()
  writeFileSync(path.join(root, 'docs/open-source-growth/traceability-matrix.md'), '')

  const result = verifyOpenSourceGrowthGuardrails(root)

  assert.equal(result.ok, false)
  assert.match(result.errors.join('\n'), /traceability-matrix\.md must include OSG-ENTRY-001/)
})

test('rejects plans that do not require guardrail references before execution', async () => {
  const root = await fixture()
  writeFileSync(path.join(root, 'docs/open-source-growth/open-source-growth-plan.md'), 'plain plan')

  const result = verifyOpenSourceGrowthGuardrails(root)

  assert.equal(result.ok, false)
  assert.match(result.errors.join('\n'), /open-source-growth-plan\.md must reference implementation-guardrails\.md/)
  assert.match(result.errors.join('\n'), /open-source-growth-plan\.md must reference traceability-matrix\.md/)
  assert.match(result.errors.join('\n'), /open-source-growth-plan\.md must reference phase-gates\.md/)
  assert.match(result.errors.join('\n'), /open-source-growth-plan\.md must reference \.\.\/program-coordination\/README\.md/)
})

test('rejects source code that introduces runtime plugin classloader dependencies', async () => {
  const root = await fixture()
  writeFileSync(
    path.join(root, 'backend/canvas-engine/src/main/java/org/chovy/canvas/engine/plugin/RuntimeLoader.java'),
    'class RuntimeLoader { URLClassLoader loader; }',
  )

  const result = verifyOpenSourceGrowthGuardrails(root)

  assert.equal(result.ok, false)
  assert.match(result.errors.join('\n'), /forbidden runtime plugin loading keyword URLClassLoader/)
})

test('rejects runtime plugin classloader dependencies in DDD-final backend modules', async () => {
  const root = await fixture()
  writeFileSync(
    path.join(root, 'backend/canvas-context-execution/src/main/java/org/chovy/canvas/execution/plugin/RuntimeLoader.java'),
    'class RuntimeLoader { PF4J pluginManager; }',
  )

  const result = verifyOpenSourceGrowthGuardrails(root)

  assert.equal(result.ok, false)
  assert.match(result.errors.join('\n'), /canvas-context-execution/)
  assert.match(result.errors.join('\n'), /forbidden runtime plugin loading keyword PF4J/)
})

test('rejects missing pull request anti-drift checklist', async () => {
  const root = await fixture()
  writeFileSync(path.join(root, '.github/pull_request_template.md'), 'plain template')

  const result = verifyOpenSourceGrowthGuardrails(root)

  assert.equal(result.ok, false)
  assert.match(result.errors.join('\n'), /pull_request_template\.md must reference OSG requirement/)
  assert.match(result.errors.join('\n'), /pull_request_template\.md must reference implementation-guardrails\.md/)
})

test('rejects contracts without backend placement ownership', async () => {
  const root = await fixture()
  writeFileSync(path.join(root, 'docs/open-source-growth/contracts/plugin-manifest-v1.md'), 'plain contract')

  const result = verifyOpenSourceGrowthGuardrails(root)

  assert.equal(result.ok, false)
  assert.match(result.errors.join('\n'), /contracts\/plugin-manifest-v1\.md must reference Backend Placement \/ Owner/)
  assert.match(result.errors.join('\n'), /contracts\/plugin-manifest-v1\.md must reference CURRENT_ENGINE_BRIDGE/)
})

test('rejects node handler and demo contracts without backend placement ownership', async () => {
  const root = await fixture()
  writeFileSync(path.join(root, 'docs/open-source-growth/contracts/node-handler-contract.md'), 'plain node handler contract')
  writeFileSync(path.join(root, 'docs/open-source-growth/contracts/demo-profile-contract.md'), 'plain demo profile contract')

  const result = verifyOpenSourceGrowthGuardrails(root)

  assert.equal(result.ok, false)
  assert.match(result.errors.join('\n'), /contracts\/node-handler-contract\.md must reference Backend Placement \/ Owner/)
  assert.match(result.errors.join('\n'), /contracts\/demo-profile-contract\.md must reference Backend Placement \/ Owner/)
  assert.match(result.errors.join('\n'), /contracts\/node-handler-contract\.md must reference DDD_FINAL_MODULE/)
  assert.match(result.errors.join('\n'), /contracts\/demo-profile-contract\.md must reference CURRENT_ENGINE_BRIDGE/)
})

test('rejects OSG specs with old backend paths but no backend path authority', async () => {
  const root = await fixture()
  writeFileSync(path.join(root, 'docs/open-source-growth/open-source-growth-spec.md'), [
    'Spec',
    'backend/canvas-engine/src/main/java/org/chovy/canvas/engine/plugin',
  ].join('\n'))

  const result = verifyOpenSourceGrowthGuardrails(root)

  assert.equal(result.ok, false)
  assert.match(result.errors.join('\n'), /open-source-growth-spec\.md must reference Backend Path Authority/)
  assert.match(result.errors.join('\n'), /open-source-growth-spec\.md must reference worker packet/)
})

test('rejects missing bridge declaration in worker packets', async () => {
  const root = await fixture()
  writeFileSync(path.join(root, 'docs/program-coordination/subagent-worker-packets.md'), 'plain packets')

  const result = verifyOpenSourceGrowthGuardrails(root)

  assert.equal(result.ok, false)
  assert.match(result.errors.join('\n'), /subagent-worker-packets\.md must reference Bridge Declaration/)
  assert.match(result.errors.join('\n'), /subagent-worker-packets\.md must reference final DDD owner module/)
})

test('rejects OSG worker sections that allow old-engine bridge work without a complete section bridge declaration', async () => {
  const root = await fixture()
  writeFileSync(path.join(root, 'docs/program-coordination/subagent-worker-packets.md'), [
    'Bridge Declaration',
    'exact old service/API',
    'exact old files',
    'final DDD owner module',
    'idempotency rule',
    'removal gate',
    'rollback path',
    '',
    '### OSG-W02: Demo Shell And Mock Catalog',
    '',
    '```text',
    'Program: Open Source Growth',
    'Task id: OSG-W02',
    'Target backend state: DOCS_ONLY or CURRENT_ENGINE_BRIDGE for demo seed only',
    'Allowed write scope:',
    '  backend/canvas-engine/src/main/resources/application-demo.yml only when the Bridge Declaration assigns this exact file',
    '```',
  ].join('\n'))

  const result = verifyOpenSourceGrowthGuardrails(root)

  assert.equal(result.ok, false)
  assert.match(result.errors.join('\n'), /OSG-W02/)
  assert.match(result.errors.join('\n'), /section must include exact old service\/API/)
  assert.match(result.errors.join('\n'), /section must include rollback path/)
})
