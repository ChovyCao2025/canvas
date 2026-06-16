import assert from 'node:assert/strict'
import { mkdirSync, readFileSync, writeFileSync } from 'node:fs'
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
  mkdirSync(path.join(root, 'docs/open-source/templates'), { recursive: true })
  mkdirSync(path.join(root, 'frontend/src/pages/canvas-list'), { recursive: true })
  mkdirSync(path.join(root, 'tools/canvas-cli/src'), { recursive: true })
  mkdirSync(path.join(root, 'tools/canvas-cli/test/fixtures'), { recursive: true })
  mkdirSync(path.join(root, 'tools/open-source-growth'), { recursive: true })
  mkdirSync(path.join(root, '.github'), { recursive: true })
  mkdirSync(path.join(root, 'backend/canvas-web/src/main/java/org/chovy/canvas/web/canvas'), { recursive: true })
  mkdirSync(path.join(root, 'backend/canvas-web/src/test/java/org/chovy/canvas/web/compat'), { recursive: true })
  mkdirSync(path.join(root, 'backend/canvas-engine/src/main/java/org/chovy/canvas/engine/plugin'), { recursive: true })
  mkdirSync(path.join(root, 'backend/canvas-engine/src/main/java/org/chovy/canvas/engine/handler'), { recursive: true })
  mkdirSync(path.join(root, 'backend/canvas-engine/src/main/java/org/chovy/canvas/engine/template'), { recursive: true })
  mkdirSync(path.join(root, 'backend/canvas-context-execution/src/main/java/org/chovy/canvas/execution/plugin'), { recursive: true })
  mkdirSync(path.join(root, 'wiremock/mappings'), { recursive: true })

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
  mkdirSync(path.join(root, '.github/workflows'), { recursive: true })
  for (const workflow of ['ci.yml', 'canvas-ci.yml']) {
    writeFileSync(path.join(root, '.github/workflows', workflow), [
      'jobs:',
      '  open-source-growth-guardrails:',
      '    steps:',
      '      - run: cd tools/canvas-cli && npm test',
      '      - run: docker compose -f docker-compose.demo.yml config',
      '      - run: node tools/open-source-growth/playground-runtime-smoke.mjs',
      '      - run: node --test tools/open-source-growth/guardrail-verifier.test.mjs',
      '      - run: node tools/open-source-growth/guardrail-verifier.mjs',
    ].join('\n'))
  }
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
  writeFileSync(path.join(root, 'docs/open-source/templates/new-user-welcome.md'), 'new-user-welcome')
  writeFileSync(path.join(root, 'docs/open-source/templates/coupon-approval-release.md'), 'coupon-approval-release')
  writeFileSync(path.join(root, 'docs/open-source/playground.md'), [
    '# Playground',
    '',
    'Run the offline runtime smoke:',
    '',
    '```bash',
    'node tools/open-source-growth/playground-runtime-smoke.mjs',
    '```',
    '',
    'Run the optional live backend API smoke only when the backend is running:',
    '',
    '```bash',
    'node tools/open-source-growth/playground-live-api-smoke.mjs --api-url http://localhost:8080',
    '```',
    '',
    'Local/offline runtime smoke covers checked-in fixture and WireMock catalog coherence.',
  ].join('\n'))
  writeFileSync(path.join(root, 'tools/open-source-growth/playground-runtime-smoke.mjs'), 'export function verifyPlaygroundRuntimeSmoke() {}\n')
  writeFileSync(path.join(root, 'tools/open-source-growth/playground-live-api-smoke.mjs'), [
    'const DSL_MAP_PATH = "/canvas/dsl/map"',
    'function validateMappingResponse(errors, responseJson) {',
    '  if (responseJson.templateKey !== "new-user-welcome") errors.push("templateKey")',
    '  if (typeof responseJson.graphJson !== "string") errors.push("graphJson")',
    '  if (!Array.isArray(responseJson.violations)) errors.push("violations")',
    '}',
    'export function verifyPlaygroundLiveApiSmoke() {}',
  ].join('\n'))
  writeFileSync(path.join(root, 'tools/open-source-growth/g10-public-api-stability.mjs'), 'export function verifyG10PublicApiStability() {}\n')
  writeFileSync(path.join(root, 'backend/canvas-web/src/main/java/org/chovy/canvas/web/canvas/CanvasDslController.java'), [
    '@RestController',
    '@RequestMapping("/canvas/dsl")',
    'class CanvasDslController {',
    '  @PostMapping("/validate") void validate() {}',
    '  @PostMapping("/map") void map() {}',
    '  @PostMapping("/import") void importDsl() {}',
    '  @GetMapping("/export/{canvasId}") void exportDsl() {}',
    '  @PostMapping("/diff") void diff() {}',
    '}',
  ].join('\n'))
  writeFileSync(path.join(root, 'backend/canvas-web/src/test/java/org/chovy/canvas/web/compat/CanvasApiCompatibilityTest.java'), [
    'class CanvasApiCompatibilityTest {',
    '  void validateRoute() { webClient().post().uri("/canvas/dsl/validate"); }',
    '  void mapRoute() { webClient().post().uri("/canvas/dsl/map"); }',
    '  void importRoute() { webClient().post().uri("/canvas/dsl/import"); }',
    '  void exportRoute() { webClient().get().uri("/canvas/dsl/export/99"); }',
    '  void diffRoute() { webClient().post().uri("/canvas/dsl/diff"); }',
    '}',
  ].join('\n'))
  writeFileSync(path.join(root, 'docker-compose.demo.yml'), [
    'services:',
    '  mysql:',
    '  redis:',
    '  wiremock:',
    '  rocketmq-namesrv:',
    '  rocketmq-broker:',
  ].join('\n'))
  writeFileSync(path.join(root, 'wiremock/mappings/demo-catalog.json'), JSON.stringify({
    mappings: [
      {
        request: { method: 'GET', url: '/mock/demo/templates' },
        response: {
          jsonBody: {
            templates: [
              {
                key: 'new-user-welcome',
                riskLevel: 'LOW',
                docs: 'docs/open-source/templates/new-user-welcome.md',
                requiredPlugins: ['canvas-plugin-webhook', 'canvas-plugin-coupon', 'canvas-plugin-message'],
              },
              {
                key: 'coupon-approval-release',
                riskLevel: 'HIGH',
                docs: 'docs/open-source/templates/coupon-approval-release.md',
                requiredPlugins: ['canvas-plugin-approval', 'canvas-plugin-coupon', 'canvas-plugin-message'],
              },
            ],
          },
        },
      },
      {
        request: { method: 'GET', url: '/mock/demo/plugins' },
        response: {
          jsonBody: {
            plugins: [
              { key: 'canvas-plugin-webhook', enabled: true, mode: 'mock' },
              { key: 'canvas-plugin-message', enabled: true, mode: 'mock' },
              { key: 'canvas-plugin-coupon', enabled: true, mode: 'mock' },
              { key: 'canvas-plugin-approval', enabled: true, mode: 'mock' },
              { key: 'canvas-plugin-ai', enabled: true, mode: 'mock' },
              { key: 'canvas-plugin-risk', enabled: true, mode: 'mock' },
            ],
          },
        },
      },
      {
        request: { method: 'GET', url: '/mock/demo/golden-path' },
        response: { jsonBody: { steps: ['review new-user-welcome', 'review mock AI risk audit'] } },
      },
      { request: { method: 'POST', url: '/mock/message/sms' }, response: { jsonBody: { status: 'SUCCESS' } } },
      { request: { method: 'POST', url: '/mock/message/email' }, response: { jsonBody: { status: 'SUCCESS' } } },
      { request: { method: 'POST', url: '/mock/approval/start' }, response: { jsonBody: { status: 'PENDING' } } },
      { request: { method: 'POST', url: '/mock/ai/audit' }, response: { jsonBody: { status: 'SUCCESS' } } },
    ],
  }, null, 2))
  writeFileSync(path.join(root, 'frontend/src/pages/canvas-list/templateCatalog.ts'), [
    'export const officialTemplateCatalog = [',
    '  {',
    "    key: 'new-user-welcome',",
    "    riskLevel: 'LOW',",
    "    requiredPlugins: ['canvas-plugin-webhook', 'canvas-plugin-coupon', 'canvas-plugin-message'],",
    "    docs: 'docs/open-source/templates/new-user-welcome.md',",
    "    canvas: journey('new-user-welcome', '新用户欢迎旅程', { type: 'webhook', event: 'user.registered' }, [",
    '      node(\'segment\', \'condition\', \'新客判断\', { expression: \'user.lifecycleStage == "new"\' }),',
    "      node('coupon', 'coupon.grant', '发放首单券', { couponKey: 'WELCOME_10' }),",
    "      node('message', 'message.send', '欢迎短信', { channel: 'sms', template: 'welcome_coupon' }),",
    '    ], [',
    "      edge('segment', 'coupon', true),",
    "      edge('coupon', 'message'),",
    '    ]),',
    "    samplePayload: { event: 'user.registered', user: { id: 'u_1001', lifecycleStage: 'new', phone: '+8613800000001' } },",
    '  },',
    '  {',
    "    key: 'coupon-approval-release',",
    "    riskLevel: 'HIGH',",
    "    requiredPlugins: ['canvas-plugin-approval', 'canvas-plugin-coupon', 'canvas-plugin-message'],",
    "    docs: 'docs/open-source/templates/coupon-approval-release.md',",
    '  },',
    ']',
  ].join('\n'))
  writeFileSync(path.join(root, 'tools/canvas-cli/src/index.mjs'), [
    "const PUBLISH_GATED_MESSAGE = 'Publish is gated until a stable backend publish API is verified; import/export preview is available after G10 unlock.'",
    'const usage = `Canvas CLI',
    '',
    'Usage:',
    '  canvas-cli --help',
    '  canvas-cli validate <file>',
    '  canvas-cli diff <before> <after>',
    '  canvas-cli import <file> --api-url <url>',
    '  canvas-cli export <canvasId> --api-url <url> --tenant-id <tenantId>',
    '',
    'Current boundary:',
    '  import and export are unlocked after G10 public extension/API stability.',
    '  publish remains blocked until a stable backend publish API is verified.`',
    '',
    'function main(command) {',
    "  if (command === 'import') runImport()",
    "  if (command === 'export') runExport()",
    '  requestJson(apiUrl, "POST", "/canvas/dsl/import", { document })',
    '  requestJson(apiUrl, "GET", `/canvas/dsl/export/${canvasId}`, undefined, { "x-tenant-id": tenantId })',
    "  if (command === 'publish') {",
    '    throw new Error(PUBLISH_GATED_MESSAGE)',
    '  }',
    '  return usage',
    '}',
  ].join('\n'))
  writeFileSync(path.join(root, 'tools/canvas-cli/test/fixtures/valid-journey.json'), JSON.stringify({
    apiVersion: 'canvas/v1',
    kind: 'Journey',
    metadata: { name: 'new-user-welcome' },
    spec: {
      trigger: { type: 'webhook', event: 'user.registered' },
      nodes: [
        { id: 'segment', type: 'condition', config: { expression: 'user.lifecycleStage == "new"' } },
        { id: 'coupon', type: 'coupon.grant', config: { couponKey: 'WELCOME_10' } },
        { id: 'message', type: 'message', config: { channel: 'sms', template: 'welcome_coupon' } },
      ],
      edges: [
        { from: 'segment', to: 'coupon' },
        { from: 'coupon', to: 'message' },
      ],
    },
  }, null, 2))
  writeFileSync(path.join(root, 'tools/canvas-cli/test/fixtures/playground-new-user-welcome.json'), JSON.stringify({
    apiVersion: 'canvas/v1',
    kind: 'Journey',
    metadata: {
      name: 'new-user-welcome',
      title: '新用户欢迎旅程',
    },
    spec: {
      trigger: { type: 'webhook', event: 'user.registered' },
      nodes: [
        { id: 'segment', type: 'condition', label: '新客判断', config: { expression: 'user.lifecycleStage == "new"' } },
        { id: 'coupon', type: 'coupon.grant', label: '发放首单券', config: { couponKey: 'WELCOME_10' } },
        { id: 'message', type: 'message.send', label: '欢迎短信', config: { channel: 'sms', template: 'welcome_coupon' } },
      ],
      edges: [
        { from: 'segment', to: 'coupon', when: true },
        { from: 'coupon', to: 'message' },
      ],
    },
    samplePayload: {
      event: 'user.registered',
      user: { id: 'u_1001', lifecycleStage: 'new', phone: '+8613800000001' },
    },
  }, null, 2))
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

test('rejects demo readiness drift when a documented WireMock endpoint is missing', async () => {
  const root = await fixture()
  writeFileSync(path.join(root, 'wiremock/mappings/demo-catalog.json'), JSON.stringify({
    mappings: [
      { request: { method: 'GET', url: '/mock/demo/templates' }, response: { jsonBody: { templates: [] } } },
      { request: { method: 'GET', url: '/mock/demo/plugins' }, response: { jsonBody: { plugins: [] } } },
    ],
  }))

  const result = verifyOpenSourceGrowthGuardrails(root)

  assert.equal(result.ok, false)
  assert.match(result.errors.join('\n'), /wiremock mappings must include \/mock\/demo\/golden-path/)
  assert.match(result.errors.join('\n'), /wiremock mappings must include \/mock\/ai\/audit/)
})

test('rejects demo templates requiring plugins that are not enabled in mock mode', async () => {
  const root = await fixture()
  const catalog = JSON.parse(readFileSync(path.join(root, 'wiremock/mappings/demo-catalog.json'), 'utf8'))
  const pluginMapping = catalog.mappings.find(mapping => mapping.request.url === '/mock/demo/plugins')
  pluginMapping.response.jsonBody.plugins = pluginMapping.response.jsonBody.plugins.filter(plugin => plugin.key !== 'canvas-plugin-coupon')
  writeFileSync(path.join(root, 'wiremock/mappings/demo-catalog.json'), JSON.stringify(catalog, null, 2))

  const result = verifyOpenSourceGrowthGuardrails(root)

  assert.equal(result.ok, false)
  assert.match(result.errors.join('\n'), /new-user-welcome requires demo plugin canvas-plugin-coupon/)
})

test('rejects non-golden demo templates requiring disabled plugins', async () => {
  const root = await fixture()
  const catalog = JSON.parse(readFileSync(path.join(root, 'wiremock/mappings/demo-catalog.json'), 'utf8'))
  const pluginMapping = catalog.mappings.find(mapping => mapping.request.url === '/mock/demo/plugins')
  const approvalPlugin = pluginMapping.response.jsonBody.plugins.find(plugin => plugin.key === 'canvas-plugin-approval')
  approvalPlugin.enabled = false
  writeFileSync(path.join(root, 'wiremock/mappings/demo-catalog.json'), JSON.stringify(catalog, null, 2))

  const result = verifyOpenSourceGrowthGuardrails(root)

  assert.equal(result.ok, false)
  assert.match(result.errors.join('\n'), /coupon-approval-release requires demo plugin canvas-plugin-approval/)
})

test('rejects demo catalog entries whose template docs path is missing', async () => {
  const root = await fixture()
  const catalog = JSON.parse(readFileSync(path.join(root, 'wiremock/mappings/demo-catalog.json'), 'utf8'))
  const templateMapping = catalog.mappings.find(mapping => mapping.request.url === '/mock/demo/templates')
  templateMapping.response.jsonBody.templates[0].docs = 'docs/open-source/templates/missing-template.md'
  writeFileSync(path.join(root, 'wiremock/mappings/demo-catalog.json'), JSON.stringify(catalog, null, 2))

  const result = verifyOpenSourceGrowthGuardrails(root)

  assert.equal(result.ok, false)
  assert.match(result.errors.join('\n'), /new-user-welcome demo template docs path is missing/)
})

test('rejects demo catalog plugin drift from frontend new-user-welcome golden path', async () => {
  const root = await fixture()
  const catalog = JSON.parse(readFileSync(path.join(root, 'wiremock/mappings/demo-catalog.json'), 'utf8'))
  const templateMapping = catalog.mappings.find(mapping => mapping.request.url === '/mock/demo/templates')
  templateMapping.response.jsonBody.templates[0].requiredPlugins = ['message', 'coupon']
  writeFileSync(path.join(root, 'wiremock/mappings/demo-catalog.json'), JSON.stringify(catalog, null, 2))

  const result = verifyOpenSourceGrowthGuardrails(root)

  assert.equal(result.ok, false)
  assert.match(result.errors.join('\n'), /new-user-welcome WireMock requiredPlugins must match frontend catalog/)
})

test('rejects demo catalog missing a frontend official template', async () => {
  const root = await fixture()
  const catalog = JSON.parse(readFileSync(path.join(root, 'wiremock/mappings/demo-catalog.json'), 'utf8'))
  const templateMapping = catalog.mappings.find(mapping => mapping.request.url === '/mock/demo/templates')
  templateMapping.response.jsonBody.templates = templateMapping.response.jsonBody.templates.filter(template => template.key !== 'coupon-approval-release')
  writeFileSync(path.join(root, 'wiremock/mappings/demo-catalog.json'), JSON.stringify(catalog, null, 2))

  const result = verifyOpenSourceGrowthGuardrails(root)

  assert.equal(result.ok, false)
  assert.match(result.errors.join('\n'), /must include frontend official template coupon-approval-release/)
})

test('rejects demo catalog extra template outside frontend official catalog', async () => {
  const root = await fixture()
  const catalog = JSON.parse(readFileSync(path.join(root, 'wiremock/mappings/demo-catalog.json'), 'utf8'))
  const templateMapping = catalog.mappings.find(mapping => mapping.request.url === '/mock/demo/templates')
  templateMapping.response.jsonBody.templates.push({
    key: 'unlisted-demo-template',
    riskLevel: 'LOW',
    docs: 'docs/open-source/templates/new-user-welcome.md',
    requiredPlugins: ['canvas-plugin-message'],
  })
  writeFileSync(path.join(root, 'wiremock/mappings/demo-catalog.json'), JSON.stringify(catalog, null, 2))

  const result = verifyOpenSourceGrowthGuardrails(root)

  assert.equal(result.ok, false)
  assert.match(result.errors.join('\n'), /contains non-frontend official template unlisted-demo-template/)
})

test('rejects demo catalog risk level drift from frontend official catalog', async () => {
  const root = await fixture()
  const catalog = JSON.parse(readFileSync(path.join(root, 'wiremock/mappings/demo-catalog.json'), 'utf8'))
  const templateMapping = catalog.mappings.find(mapping => mapping.request.url === '/mock/demo/templates')
  templateMapping.response.jsonBody.templates.find(template => template.key === 'coupon-approval-release').riskLevel = 'LOW'
  writeFileSync(path.join(root, 'wiremock/mappings/demo-catalog.json'), JSON.stringify(catalog, null, 2))

  const result = verifyOpenSourceGrowthGuardrails(root)

  assert.equal(result.ok, false)
  assert.match(result.errors.join('\n'), /coupon-approval-release WireMock riskLevel must match frontend catalog: HIGH/)
})

test('rejects WireMock drift when frontend catalog fields use constants and nested arrays', async () => {
  const root = await fixture()
  const frontendCatalogPath = path.join(root, 'frontend/src/pages/canvas-list/templateCatalog.ts')
  writeFileSync(frontendCatalogPath, [
    "const welcomePlugins = [",
    "  'canvas-plugin-webhook',",
    "  'canvas-plugin-coupon',",
    "  'canvas-plugin-message',",
    ']',
    "const welcomeRiskLevel = 'LOW'",
    "const welcomeDocs = 'docs/open-source/templates/new-user-welcome.md'",
    "const approvalPlugins = ['canvas-plugin-approval', 'canvas-plugin-coupon', 'canvas-plugin-message']",
    "const approvalRiskLevel = 'HIGH'",
    "const approvalDocs = 'docs/open-source/templates/coupon-approval-release.md'",
    '',
    'export const officialTemplateCatalog = [',
    '  {',
    "    key: 'new-user-welcome',",
    '    riskLevel: welcomeRiskLevel,',
    '    requiredPlugins: welcomePlugins,',
    '    docs: welcomeDocs,',
    "    canvas: journey('new-user-welcome', '新用户欢迎旅程', { type: 'webhook', event: 'user.registered' }, [",
    '      node(\'segment\', \'condition\', \'新客判断\', { expression: \'user.lifecycleStage == "new"\' }),',
    "      node('coupon', 'coupon.grant', '发放首单券', { couponKey: 'WELCOME_10' }),",
    "      node('message', 'message.send', '欢迎短信', { channel: 'sms', template: 'welcome_coupon' }),",
    '    ], [',
    "      edge('segment', 'coupon', true),",
    "      edge('coupon', 'message'),",
    '    ]),',
    "    samplePayload: { event: 'user.registered', user: { id: 'u_1001', lifecycleStage: 'new', phone: '+8613800000001' } },",
    '  },',
    '  {',
    "    key: 'coupon-approval-release',",
    '    riskLevel: approvalRiskLevel,',
    '    requiredPlugins: approvalPlugins,',
    '    docs: approvalDocs,',
    '  },',
    ']',
  ].join('\n'))
  const catalog = JSON.parse(readFileSync(path.join(root, 'wiremock/mappings/demo-catalog.json'), 'utf8'))
  const templateMapping = catalog.mappings.find(mapping => mapping.request.url === '/mock/demo/templates')
  const welcomeTemplate = templateMapping.response.jsonBody.templates.find(template => template.key === 'new-user-welcome')
  welcomeTemplate.requiredPlugins = ['canvas-plugin-webhook', 'canvas-plugin-message']
  welcomeTemplate.riskLevel = 'MEDIUM'
  welcomeTemplate.docs = 'docs/open-source/templates/coupon-approval-release.md'
  writeFileSync(path.join(root, 'wiremock/mappings/demo-catalog.json'), JSON.stringify(catalog, null, 2))

  const result = verifyOpenSourceGrowthGuardrails(root)

  assert.equal(result.ok, false)
  assert.match(result.errors.join('\n'), /new-user-welcome WireMock requiredPlugins must match frontend catalog: canvas-plugin-webhook, canvas-plugin-coupon, canvas-plugin-message/)
  assert.match(result.errors.join('\n'), /new-user-welcome WireMock riskLevel must match frontend catalog: LOW/)
  assert.match(result.errors.join('\n'), /new-user-welcome WireMock docs path must match frontend catalog: docs\/open-source\/templates\/new-user-welcome\.md/)
})

test('rejects missing mock plugin enablement from frontend official plugin union', async () => {
  const root = await fixture()
  writeFileSync(path.join(root, 'docs/open-source/templates/scheduled-winback.md'), 'scheduled-winback')
  const frontendCatalogPath = path.join(root, 'frontend/src/pages/canvas-list/templateCatalog.ts')
  const frontendCatalog = readFileSync(frontendCatalogPath, 'utf8').replace(
    /\n\]$/,
    [
      '  {',
      "    key: 'scheduled-winback',",
      "    riskLevel: 'MEDIUM',",
      "    requiredPlugins: ['canvas-plugin-schedule', 'canvas-plugin-message'],",
      "    docs: 'docs/open-source/templates/scheduled-winback.md',",
      '  },',
      ']',
    ].join('\n'),
  )
  writeFileSync(frontendCatalogPath, frontendCatalog)
  const catalog = JSON.parse(readFileSync(path.join(root, 'wiremock/mappings/demo-catalog.json'), 'utf8'))
  const templateMapping = catalog.mappings.find(mapping => mapping.request.url === '/mock/demo/templates')
  templateMapping.response.jsonBody.templates.push({
    key: 'scheduled-winback',
    riskLevel: 'MEDIUM',
    docs: 'docs/open-source/templates/scheduled-winback.md',
    requiredPlugins: ['canvas-plugin-schedule', 'canvas-plugin-message'],
  })
  writeFileSync(path.join(root, 'wiremock/mappings/demo-catalog.json'), JSON.stringify(catalog, null, 2))

  const result = verifyOpenSourceGrowthGuardrails(root)

  assert.equal(result.ok, false)
  assert.match(result.errors.join('\n'), /must enable frontend required plugin canvas-plugin-schedule/)
})

test('rejects CLI fixture drift from documented new-user-welcome trace path', async () => {
  const root = await fixture()
  const journey = JSON.parse(readFileSync(path.join(root, 'tools/canvas-cli/test/fixtures/playground-new-user-welcome.json'), 'utf8'))
  journey.spec.nodes = journey.spec.nodes.filter(node => node.id !== 'coupon')
  journey.spec.edges = [{ from: 'segment', to: 'message' }]
  writeFileSync(path.join(root, 'tools/canvas-cli/test/fixtures/playground-new-user-welcome.json'), JSON.stringify(journey, null, 2))

  const result = verifyOpenSourceGrowthGuardrails(root)

  assert.equal(result.ok, false)
  assert.match(result.errors.join('\n'), /playground-new-user-welcome fixture must include coupon node/)
  assert.match(result.errors.join('\n'), /playground-new-user-welcome fixture must include segment -> coupon edge/)
})

test('rejects missing dedicated playground golden path fixture', async () => {
  const root = await fixture()
  writeFileSync(path.join(root, 'tools/canvas-cli/test/fixtures/playground-new-user-welcome.json'), '')

  const result = verifyOpenSourceGrowthGuardrails(root)

  assert.equal(result.ok, false)
  assert.match(result.errors.join('\n'), /tools\/canvas-cli\/test\/fixtures\/playground-new-user-welcome\.json must be valid JSON/)
})

test('allows generic CLI fixture to diverge from playground golden path', async () => {
  const root = await fixture()
  const journey = JSON.parse(readFileSync(path.join(root, 'tools/canvas-cli/test/fixtures/valid-journey.json'), 'utf8'))
  journey.metadata.name = 'generic-cli-validation'
  journey.spec.nodes = [{ id: 'message', type: 'message.send', config: { channel: 'email', template: 'generic' } }]
  journey.spec.edges = []
  writeFileSync(path.join(root, 'tools/canvas-cli/test/fixtures/valid-journey.json'), JSON.stringify(journey, null, 2))

  const result = verifyOpenSourceGrowthGuardrails(root)

  assert.equal(result.ok, true)
  assert.deepEqual(result.errors, [])
})

test('rejects playground fixture drift from frontend official new-user-welcome canvas and payload', async () => {
  const root = await fixture()
  const journey = JSON.parse(readFileSync(path.join(root, 'tools/canvas-cli/test/fixtures/playground-new-user-welcome.json'), 'utf8'))
  journey.apiVersion = 'canvas/v2'
  journey.kind = 'Workflow'
  journey.metadata.name = 'new-user-welcome-copy'
  journey.spec.nodes.find(node => node.id === 'message').type = 'message'
  journey.spec.edges.find(edge => edge.from === 'segment' && edge.to === 'coupon').when = 'matched'
  journey.samplePayload.user.lifecycleStage = 'returning'
  writeFileSync(path.join(root, 'tools/canvas-cli/test/fixtures/playground-new-user-welcome.json'), JSON.stringify(journey, null, 2))

  const result = verifyOpenSourceGrowthGuardrails(root)

  assert.equal(result.ok, false)
  assert.match(result.errors.join('\n'), /playground-new-user-welcome fixture apiVersion must match frontend catalog canvas apiVersion canvas\/v1/)
  assert.match(result.errors.join('\n'), /playground-new-user-welcome fixture kind must match frontend catalog canvas kind Journey/)
  assert.match(result.errors.join('\n'), /playground-new-user-welcome fixture metadata.name must match frontend catalog canvas metadata.name new-user-welcome/)
  assert.match(result.errors.join('\n'), /playground-new-user-welcome message node must match frontend catalog node type message\.send/)
  assert.match(result.errors.join('\n'), /playground-new-user-welcome segment -> coupon edge must match frontend catalog/)
  assert.match(result.errors.join('\n'), /playground-new-user-welcome samplePayload must match frontend catalog samplePayload/)
})

test('rejects playground fixture missing frontend sample payload', async () => {
  const root = await fixture()
  const journey = JSON.parse(readFileSync(path.join(root, 'tools/canvas-cli/test/fixtures/playground-new-user-welcome.json'), 'utf8'))
  delete journey.samplePayload
  writeFileSync(path.join(root, 'tools/canvas-cli/test/fixtures/playground-new-user-welcome.json'), JSON.stringify(journey, null, 2))

  const result = verifyOpenSourceGrowthGuardrails(root)

  assert.equal(result.ok, false)
  assert.match(result.errors.join('\n'), /playground-new-user-welcome samplePayload must match frontend catalog samplePayload/)
})

test('rejects missing playground runtime smoke verifier and doc command', async () => {
  const root = await fixture()
  writeFileSync(path.join(root, 'tools/open-source-growth/playground-runtime-smoke.mjs'), '')
  writeFileSync(path.join(root, 'docs/open-source/playground.md'), [
    '# Playground',
    '',
    'Local runtime smoke exists for checked-in artifacts.',
  ].join('\n'))

  const result = verifyOpenSourceGrowthGuardrails(root)

  assert.equal(result.ok, false)
  assert.match(result.errors.join('\n'), /tools\/open-source-growth\/playground-runtime-smoke\.mjs is required/)
  assert.match(result.errors.join('\n'), /docs\/open-source\/playground\.md must reference node tools\/open-source-growth\/playground-runtime-smoke\.mjs/)
})

test('rejects missing optional playground live API smoke verifier and doc command', async () => {
  const root = await fixture()
  writeFileSync(path.join(root, 'tools/open-source-growth/playground-live-api-smoke.mjs'), '')
  writeFileSync(path.join(root, 'docs/open-source/playground.md'), [
    '# Playground',
    '',
    'Run the offline runtime smoke:',
    '',
    '```bash',
    'node tools/open-source-growth/playground-runtime-smoke.mjs',
    '```',
  ].join('\n'))

  const result = verifyOpenSourceGrowthGuardrails(root)

  assert.equal(result.ok, false)
  assert.match(result.errors.join('\n'), /tools\/open-source-growth\/playground-live-api-smoke\.mjs is required/)
  assert.match(result.errors.join('\n'), /docs\/open-source\/playground\.md must reference node tools\/open-source-growth\/playground-live-api-smoke\.mjs --api-url http:\/\/localhost:8080/)
})

test('rejects missing G10 public API stability verifier script', async () => {
  const root = await fixture()
  writeFileSync(path.join(root, 'tools/open-source-growth/g10-public-api-stability.mjs'), '')

  const result = verifyOpenSourceGrowthGuardrails(root)

  assert.equal(result.ok, false)
  assert.match(result.errors.join('\n'), /tools\/open-source-growth\/g10-public-api-stability\.mjs is required/)
})

test('rejects G10 public API stability evidence drift', async () => {
  const root = await fixture()
  writeFileSync(path.join(root, 'backend/canvas-web/src/test/java/org/chovy/canvas/web/compat/CanvasApiCompatibilityTest.java'), [
    'class CanvasApiCompatibilityTest {',
    '  void validateRoute() { webClient().post().uri("/canvas/dsl/validate"); }',
    '  void mapRoute() { webClient().post().uri("/canvas/dsl/map"); }',
    '  void importRoute() { webClient().post().uri("/canvas/dsl/import"); }',
    '  void exportRoute() { webClient().get().uri("/canvas/dsl/export/99"); }',
    '}',
  ].join('\n'))

  const result = verifyOpenSourceGrowthGuardrails(root)

  assert.equal(result.ok, false)
  assert.match(result.errors.join('\n'), /CanvasApiCompatibilityTest\.java must cover \/canvas\/dsl\/diff/)
})

test('rejects canvas-cli publish unlock after G10 import/export unlock', async () => {
  const root = await fixture()
  writeFileSync(path.join(root, 'tools/canvas-cli/src/index.mjs'), [
    'const usage = `Canvas CLI',
    'Usage:',
    '  canvas-cli import <file>',
    '  canvas-cli export <canvasId>',
    '  canvas-cli publish <canvasId>`',
    '',
    'async function runPublish(canvasId) {',
    "  return requestJson(apiUrl, 'POST', `/canvas/publish/${canvasId}`)",
    '}',
  ].join('\n'))

  const result = verifyOpenSourceGrowthGuardrails(root)

  assert.equal(result.ok, false)
  assert.match(result.errors.join('\n'), /canvas-cli must keep publish gated until a stable backend publish API is verified/)
  assert.match(result.errors.join('\n'), /canvas-cli must stay within G10 import\/export unlock; remove publish command in usage/)
  assert.match(result.errors.join('\n'), /canvas-cli must stay within G10 import\/export unlock; remove publish backend path/)
})

test('rejects canvas-cli backend API drift outside import/export after G10 unlock', async () => {
  const root = await fixture()
  writeFileSync(path.join(root, 'tools/canvas-cli/src/backend-api.mjs'), [
    'export function callBackend(document) {',
    "  return requestJson(apiUrl, 'POST', '/canvas/dsl/map', document)",
    '}',
  ].join('\n'))

  const result = verifyOpenSourceGrowthGuardrails(root)

  assert.equal(result.ok, false)
  assert.match(result.errors.join('\n'), /remove Canvas DSL map backend path/)
})

test('rejects CI that skips canvas-cli behavior tests before OSG guardrails', async () => {
  const root = await fixture()
  writeFileSync(path.join(root, '.github/workflows/ci.yml'), [
    'jobs:',
    '  open-source-growth-guardrails:',
    '    steps:',
    '      - run: docker compose -f docker-compose.demo.yml config',
    '      - run: node --test tools/open-source-growth/guardrail-verifier.test.mjs',
    '      - run: node tools/open-source-growth/guardrail-verifier.mjs',
  ].join('\n'))

  const result = verifyOpenSourceGrowthGuardrails(root)

  assert.equal(result.ok, false)
  assert.match(result.errors.join('\n'), /\.github\/workflows\/ci\.yml must run tools\/canvas-cli/)
})

test('rejects CI that skips demo compose config validation before OSG guardrails', async () => {
  const root = await fixture()
  writeFileSync(path.join(root, '.github/workflows/canvas-ci.yml'), [
    'jobs:',
    '  open-source-growth-guardrails:',
    '    steps:',
    '      - run: cd tools/canvas-cli && npm test',
    '      - run: node --test tools/open-source-growth/guardrail-verifier.test.mjs',
    '      - run: node tools/open-source-growth/guardrail-verifier.mjs',
  ].join('\n'))

  const result = verifyOpenSourceGrowthGuardrails(root)

  assert.equal(result.ok, false)
  assert.match(result.errors.join('\n'), /\.github\/workflows\/canvas-ci\.yml must run docker compose -f docker-compose\.demo\.yml config/)
})

test('rejects CI that omits playground runtime smoke before OSG verifier results', async () => {
  const root = await fixture()
  writeFileSync(path.join(root, '.github/workflows/ci.yml'), [
    'jobs:',
    '  open-source-growth-guardrails:',
    '    steps:',
    '      - run: cd tools/canvas-cli && npm test',
    '      - run: docker compose -f docker-compose.demo.yml config',
    '      - run: node --test tools/open-source-growth/guardrail-verifier.test.mjs',
    '      - run: node tools/open-source-growth/guardrail-verifier.mjs',
  ].join('\n'))
  writeFileSync(path.join(root, '.github/workflows/canvas-ci.yml'), [
    'jobs:',
    '  open-source-growth-guardrails:',
    '    steps:',
    '      - run: cd tools/canvas-cli && npm test',
    '      - run: docker compose -f docker-compose.demo.yml config',
    '      - run: node --test tools/open-source-growth/guardrail-verifier.test.mjs',
    '      - run: node tools/open-source-growth/guardrail-verifier.mjs',
    '      - run: node tools/open-source-growth/playground-runtime-smoke.mjs',
  ].join('\n'))

  const result = verifyOpenSourceGrowthGuardrails(root)

  assert.equal(result.ok, false)
  assert.match(result.errors.join('\n'), /\.github\/workflows\/ci\.yml must run node tools\/open-source-growth\/playground-runtime-smoke\.mjs before publishing OSG guardrail results/)
  assert.match(result.errors.join('\n'), /\.github\/workflows\/canvas-ci\.yml must run node --test tools\/open-source-growth\/guardrail-verifier\.test\.mjs before publishing OSG guardrail results/)
})

test('rejects CI that runs OSG verifier before CLI and demo compose gates', async () => {
  const root = await fixture()
  writeFileSync(path.join(root, '.github/workflows/ci.yml'), [
    'jobs:',
    '  open-source-growth-guardrails:',
    '    steps:',
    '      - run: node --test tools/open-source-growth/guardrail-verifier.test.mjs',
    '      - run: node tools/open-source-growth/guardrail-verifier.mjs',
    '      - run: cd tools/canvas-cli && npm test',
    '      - run: docker compose -f docker-compose.demo.yml config',
  ].join('\n'))

  const result = verifyOpenSourceGrowthGuardrails(root)

  assert.equal(result.ok, false)
  assert.match(result.errors.join('\n'), /\.github\/workflows\/ci\.yml must run node --test tools\/open-source-growth\/guardrail-verifier\.test\.mjs before publishing OSG guardrail results/)
})

test('rejects CI when OSG commands are ordered outside the OSG guardrail job', async () => {
  const root = await fixture()
  writeFileSync(path.join(root, '.github/workflows/ci.yml'), [
    'jobs:',
    '  unrelated-validation:',
    '    steps:',
    '      - run: cd tools/canvas-cli && npm test',
    '      - run: docker compose -f docker-compose.demo.yml config',
    '      - run: node --test tools/open-source-growth/guardrail-verifier.test.mjs',
    '      - run: node tools/open-source-growth/guardrail-verifier.mjs',
    '  open-source-growth-guardrails:',
    '    steps:',
    '      - run: node tools/open-source-growth/guardrail-verifier.mjs',
  ].join('\n'))

  const result = verifyOpenSourceGrowthGuardrails(root)

  assert.equal(result.ok, false)
  assert.match(result.errors.join('\n'), /\.github\/workflows\/ci\.yml must run tools\/canvas-cli npm test before publishing OSG guardrail results/)
})

test('rejects CI when OSG commands are only echoed inside the OSG guardrail job', async () => {
  const root = await fixture()
  writeFileSync(path.join(root, '.github/workflows/ci.yml'), [
    'jobs:',
    '  open-source-growth-guardrails:',
    '    steps:',
    '      - run: echo "cd tools/canvas-cli && npm test"',
    '      - run: echo "docker compose -f docker-compose.demo.yml config"',
    '      - run: echo "node --test tools/open-source-growth/guardrail-verifier.test.mjs"',
    '      - run: echo "node tools/open-source-growth/guardrail-verifier.mjs"',
  ].join('\n'))

  const result = verifyOpenSourceGrowthGuardrails(root)

  assert.equal(result.ok, false)
  assert.match(result.errors.join('\n'), /\.github\/workflows\/ci\.yml must run tools\/canvas-cli npm test before publishing OSG guardrail results/)
})

test('rejects CI when canvas-cli directory and npm test are split across OSG steps', async () => {
  const root = await fixture()
  writeFileSync(path.join(root, '.github/workflows/ci.yml'), [
    'jobs:',
    '  open-source-growth-guardrails:',
    '    steps:',
    '      - run: npm test',
    '      - run: echo "tools/canvas-cli"',
    '      - run: docker compose -f docker-compose.demo.yml config',
    '      - run: node --test tools/open-source-growth/guardrail-verifier.test.mjs',
    '      - run: node tools/open-source-growth/guardrail-verifier.mjs',
  ].join('\n'))

  const result = verifyOpenSourceGrowthGuardrails(root)

  assert.equal(result.ok, false)
  assert.match(result.errors.join('\n'), /\.github\/workflows\/ci\.yml must run tools\/canvas-cli npm test before publishing OSG guardrail results/)
})
