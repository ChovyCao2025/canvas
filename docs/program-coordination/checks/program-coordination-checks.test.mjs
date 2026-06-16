import assert from 'node:assert/strict'
import { chmodSync, mkdirSync, readFileSync, writeFileSync } from 'node:fs'
import { mkdtemp } from 'node:fs/promises'
import { tmpdir } from 'node:os'
import path from 'node:path'
import { execFileSync, spawnSync } from 'node:child_process'
import test from 'node:test'

function dddWorkerSection(task, title) {
  return [
    `### ${task}: ${title}`,
    '',
    '```text',
    'Program: DDD modular rewrite',
    `Task id: ${task}`,
    'Dispatch id: dispatch-example',
    'Readiness gate: R2 / G4',
    'Target backend state: DDD_FINAL_MODULE',
    'Allowed write scope:',
    '  backend/canvas-context-example/**',
    'Inventory rows required:',
    '  exact rows from inventory files',
    'Allowed module POM edits:',
    '  none unless coordinator handoff names an exact dependency or plugin and the',
    '  exact module pom.xml',
    'Forbidden write scope:',
    '  backend/pom.xml',
    'Read scope:',
    '  backend/canvas-engine/src/main/java/example/**',
    'Contracts to read:',
    '  docs/ddd-rewrite/task-packs/example.md',
    'Verification commands:',
    '  cd backend && mvn test -pl canvas-context-example',
    'Can run with:',
    '  docs-only workers',
    'Must not run with:',
    '  coordinator-owned tasks',
    'Rollback path:',
    '  revert assigned files',
    '```',
  ].join('\n')
}

function coordinatorPack({ task, readiness, target }) {
  return [
    '**Program:** DDD modular rewrite',
    `**Task id:** ${task}`,
    `**Readiness level:** ${readiness}`,
    `**Target backend state:** ${target}`,
    '## Allowed Write Scope',
    '```text',
    'docs/ddd-rewrite/**',
    '```',
    '## Forbidden Changes',
    '```text',
    'unassigned worker edits',
    '```',
    '## Run-With Constraints',
    'Can run with:',
    '```text',
    'read-only reviewers',
    '```',
    'Must not run with:',
    '```text',
    'code-writing workers',
    '```',
    '## Verification',
    '```bash',
    'bash docs/ddd-rewrite/guardrails/checks/ddd-guardrail-checks.sh .',
    '```',
    '## Rollback',
    'Revert assigned coordinator files only.',
    '## Coordinator Response',
    '```text',
    'status:',
    'tests run:',
    'guardrail checks:',
    '```',
    'PublishedCanvasDefinitionProvider',
    'CanvasPublishApplicationServiceTest',
    'ExecutionPublicationApplicationServiceTest',
  ].join('\n')
}

function dddWorkerTaskPack() {
  return [
    '## Worker Response',
    '```text',
    'status:',
    'task id:',
    'dispatch id:',
    'base commit:',
    'head commit:',
    'assigned task pack:',
    'contracts changed:',
    'verification output summary/path:',
    'evidence artifact paths:',
    'compatibility evidence:',
    'coordinator actions needed:',
    'ledger update:',
    'rollback path:',
    '```',
    'Do not return a shorter summary.',
  ].join('\n')
}

async function fixture() {
  const root = await mkdtemp(path.join(tmpdir(), 'program-coordination-'))
  mkdirSync(path.join(root, 'docs/program-coordination/checks'), { recursive: true })
  mkdirSync(path.join(root, 'docs/program-coordination/evidence'), { recursive: true })
  mkdirSync(path.join(root, 'docs/open-source-growth'), { recursive: true })
  mkdirSync(path.join(root, 'docs/ddd-rewrite/guardrails/checks'), { recursive: true })
  mkdirSync(path.join(root, 'docs/ddd-rewrite/child-specs'), { recursive: true })
  mkdirSync(path.join(root, 'docs/ddd-rewrite/inventory'), { recursive: true })
  mkdirSync(path.join(root, 'tools/program-coordination'), { recursive: true })

  const script = path.resolve('docs/program-coordination/checks/program-coordination-checks.sh')
  const targetScript = path.join(root, 'docs/program-coordination/checks/program-coordination-checks.sh')
  writeFileSync(targetScript, await import('node:fs').then(fs => fs.readFileSync(script, 'utf8')))
  chmodSync(targetScript, 0o755)
  for (const file of [
    'check-dispatch-state.mjs',
    'check-dispatch-state.test.mjs',
    'generate-worker-prompt.mjs',
    'generate-worker-prompt.test.mjs',
  ]) {
    writeFileSync(
      path.join(root, 'tools/program-coordination', file),
      readFileSync(path.resolve('tools/program-coordination', file), 'utf8'),
    )
  }

  const workerPacket = [
    'Do not edit docs/program-coordination/progress-ledger.md directly.',
    'status packet; the coordinator records accepted status in the ledger.',
    'dispatch id:',
    'base commit:',
    'verification output summary/path:',
    'evidence artifact paths:',
    'Bridge Declaration',
    'exact old service/API',
    'final DDD owner module',
    'removal gate',
    'If the bridge declaration is absent or incomplete',
    dddWorkerSection('DDD-W01', 'Platform Worker'),
    dddWorkerSection('DDD-W02', 'Risk Worker'),
    dddWorkerSection('DDD-W03', 'Marketing Worker'),
    dddWorkerSection('DDD-W04', 'CDP Worker'),
    dddWorkerSection('DDD-W05', 'BI Worker'),
    dddWorkerSection('DDD-W06', 'Conversation Worker'),
    dddWorkerSection('DDD-W07', 'Canvas Worker'),
    dddWorkerSection('DDD-W08', 'Execution Worker'),
    'OSG-W07A Through OSG-W07F',
    'backend/pom.xml is coordinator-owned',
    'Globs and package names are not ownership proof',
    'backend/canvas-context-execution/src/test/java/org/chovy/canvas/execution/adapter/plugin/official/webhook/**',
    'TemplateImportServiceTest.java',
    'CanvasDslControllerCompatibilityTest.java',
    'TraceExplanationFacadeTest.java',
    'OSG-W14: Playground Flow',
  ].join('\n')

  const docs = {
    'README.md': [
      'progress-ledger.md',
      'dispatch-state.json',
      'invalid parallel groups',
      'tools/program-coordination/check-dispatch-state.mjs',
      'tools/program-coordination/generate-worker-prompt.mjs',
      'collaboration-and-recovery-protocol.md',
      'backup-and-rollback-runbook.md',
      'execution-readiness-audit.md',
      'gate-verification-matrix.md',
      'isolated-worktree-protocol.md',
      'max-parallel-subagent-execution-plan.md',
      'subagent-worker-packets.md',
    ].join('\n'),
    'ddd-open-source-growth-integration.md': 'integration',
    'combined-roadmap.md': 'roadmap',
    'conflict-matrix.md': 'conflict',
    'execution-sequencing.md': 'CURRENT_ENGINE_BRIDGE or DDD_FINAL_MODULE or DOCS_ONLY\nGate E: Wave Closure',
    'backup-and-rollback-runbook.md': [
      '# Backup And Rollback Runbook',
      'No code-writing dispatch may start',
      'docs/program-coordination/evidence/pre-rewrite-backup-manifest.md',
      'git bundle create',
      'Per-Worker Rollback Contract',
    ].join('\n'),
    'max-parallel-subagent-execution-plan.md': 'Shared workspace mode\nbackup-and-rollback-runbook.md\nG0B\nDispatch id:\nbase commit:\nhead commit:\nverification output summary/path:\nevidence artifact paths:\nledger update:\nDDD-C00\nOSG-W01\nOSG-W05A\nOSG-C05B',
    'subagent-worker-packets.md': workerPacket,
    'execution-readiness-audit.md': [
      'R0: Documentation Ready',
      'R1: Backup And Baseline Captured',
      'R6: Cutover Ready',
      'record the actual worker id/nickname',
      'fallback reason:',
    ].join('\n'),
    'collaboration-and-recovery-protocol.md': [
      '# Collaboration And Recovery Protocol',
      'dispatch-state.json',
      'backup-and-rollback-runbook.md',
      'pre-rewrite-backup-manifest.md',
      'node tools/program-coordination/check-dispatch-state.mjs .',
      'Active Dispatch Registry',
      'Worker State Machine',
      'Worker Return Contract',
      'Reviewer Contract',
      'Breakpoint Recovery',
      'Wave Closure Checklist',
      'dispatch id:',
      'base commit:',
      'head commit:',
      'verification output summary/path:',
      'evidence artifact paths:',
      'exact reserved files:',
      '`RUNNING` means the handoff was actually sent',
      'fallback reason:',
      'must have a matching active dispatch row',
    ].join('\n'),
    'progress-ledger.md': [
      '# Program Progress Ledger',
      'The coordinator is the single writer for this file.',
      'dispatch-state.json',
      'backup-and-rollback-runbook.md',
      'Pre-rewrite backup manifest',
      'machine-readable',
      'node tools/program-coordination/check-dispatch-state.mjs .',
      'node --test tools/program-coordination/*.test.mjs',
      'Reopen Checklist',
      'git status --short',
      'git worktree list',
      'Compare active dispatch registry rows with actual branches, worktrees, and changed paths',
      'Record the recovery audit',
      'fallback reason:',
      '## Current Snapshot',
      '| Field | Value |',
      '| --- | --- |',
      '| Current backend target | no cutover evidence yet |',
      '## Active Dispatch Registry',
      'Last Verified Evidence',
      '## Worker Board',
      '## Reviewer Board',
      'Recovery Audit',
      'Worker Result Recording Template',
      'Stop Conditions',
      'dispatch id:',
      'base commit:',
      'head commit:',
      'base SHA:',
      'exact reserved files:',
      'last command/result:',
      'verification output summary/path:',
      'evidence artifact paths:',
      'ledger update:',
      'DDD-W01',
      'OSG-W14',
      'OSG-W07A official webhook plugin',
      'OSG-W07F official risk-check plugin',
    ].join('\n'),
    'gate-verification-matrix.md': [
      'node tools/program-coordination/check-dispatch-state.mjs .',
      'node --test tools/program-coordination/*.test.mjs',
      'G0B: Backup and rollback checkpoint captured',
      'pre-rewrite-backup-manifest.md',
      'PublishedCanvasDefinition.java',
      'PublishedCanvasDefinitionProvider.java',
      'PublishedCanvasNodeDefinition.java',
      'PublishedCanvasEdgeDefinition.java',
      'ExecutionPublicationPort.java',
      'CanvasPublishApplicationServiceTest',
      'ExecutionPublicationApplicationServiceTest',
      'CanvasExecutionFacade.java',
      'NodeMetadataView.java',
      'PluginEnablementView.java',
      'ExecutionDryRunFacade.java',
      'TemplateValidationPort.java',
      'AiJourneyDraftProposal.java',
      'NodeMetadataContractTest',
      'Public extension and API stability gate',
      "mvn test -pl canvas-context-execution -Dtest='*Plugin*Test'",
      'CanvasDslControllerCompatibilityTest',
      'TraceExplanationFacadeTest',
      'G12: Final cutover',
      'Coordination Closure Gates',
    ].join('\n'),
    'isolated-worktree-protocol.md': 'git worktree add\nbackup-and-rollback-runbook.md\nG0B\nactive dispatch registry row\ndispatch id:\nverification output summary/path:',
  }

  for (const [file, content] of Object.entries(docs)) {
    writeFileSync(path.join(root, 'docs/program-coordination', file), content)
  }
  writeFileSync(path.join(root, 'docs/program-coordination/evidence/README.md'), [
    'pre-rewrite-backup-manifest.md',
    'Do not store database dumps',
    'commands.txt',
    'worker-return.txt',
    'reviewer-return.txt',
    'rollback.txt',
  ].join('\n'))
  docs['max-parallel-subagent-execution-plan.md'] = [
    docs['max-parallel-subagent-execution-plan.md'],
    'dispatch-state.json',
    'parallelGroups',
  ].join('\n')
  writeFileSync(path.join(root, 'docs/program-coordination/max-parallel-subagent-execution-plan.md'), docs['max-parallel-subagent-execution-plan.md'])

  writeFileSync(path.join(root, 'docs/program-coordination/dispatch-state.json'), `${JSON.stringify({
    schemaVersion: 1,
    updatedAt: '2026-06-09',
    readiness: {
      level: 'R0',
      gate: 'G0/G0B-before-code-writing/G1/G2',
      backendTarget: 'no code worker active',
      writeMode: 'coordinator-only for planning docs',
    },
    activeDispatches: [],
    workerBoard: [
      {
        taskId: 'DDD-E01',
        status: 'READY',
        mode: 'read-only',
        gate: 'R0',
        writeScope: [],
      },
      {
        taskId: 'OSG-W01',
        status: 'READY',
        mode: 'code-writing',
        gate: 'G0/G1',
        writeScope: ['README.md'],
      },
      {
        taskId: 'OSG-W02',
        status: 'READY',
        mode: 'code-writing',
        gate: 'G0/G1',
        writeScope: ['docker-compose.demo.yml', 'wiremock/**', 'docs/open-source/playground.md'],
      },
      {
        taskId: 'OSG-W07F',
        status: 'NOT_STARTED',
        mode: 'code-writing',
        gate: 'G9/G10 plus OSG-C07',
        writeScope: ['backend/canvas-context-execution/src/main/java/org/chovy/canvas/execution/adapter/plugin/official/risk/**'],
      },
    ],
    parallelGroups: [
      {
        groupId: 'P1-immediate-shell',
        startCondition: 'Coordination docs accepted',
        maxCodeWriters: 6,
        maxReadOnlyReviewers: 4,
        taskIds: ['DDD-E01', 'OSG-W01'],
        notes: 'Fixture group.',
      },
      {
        groupId: 'P7-plugin-burst',
        startCondition: 'DDD-W08 integrated and OSG-C07 done',
        maxCodeWriters: 6,
        maxReadOnlyReviewers: 2,
        taskIds: ['OSG-W07F'],
        notes: 'Fixture group.',
      },
    ],
    reviewerBoard: [],
    recoveryAudit: {
      status: 'clean',
      activeDispatches: 0,
      commandsToRunBeforeDispatch: ['G0', 'G0B before code-writing', 'G1', 'G2'],
    },
    lastVerifiedEvidence: [
      {
        command: 'node tools/program-coordination/check-dispatch-state.mjs .',
        result: 'passed',
      },
    ],
  }, null, 2)}\n`)
  writeFileSync(path.join(root, 'docs/open-source-growth/implementation-guardrails.md'), '不要直接新建平行的 `CanvasPluginRegistry` 作为第二套注册中心。')
  writeFileSync(path.join(root, 'docs/ddd-rewrite/README.md'), 'DDD')
  writeFileSync(path.join(root, 'docs/ddd-rewrite/guardrails/README.md'), 'subagent-worker-packets.md\ngate-verification-matrix.md')
  writeFileSync(path.join(root, 'docs/ddd-rewrite/2026-06-08-ddd-modular-rewrite-spec.md'), 'Dispatch Scope Clarification')
  writeFileSync(path.join(root, 'docs/ddd-rewrite/child-specs/canvas-execution-contract-spec.md'), 'PluginEnablementView\nAiJourneyDraftBoundaryContractTest')
  writeFileSync(path.join(root, 'docs/ddd-rewrite/guardrails/checks/ddd-guardrail-checks.sh'), 'check_package_prefix')
  writeFileSync(path.join(root, 'docs/ddd-rewrite/inventory/README.md'), [
    '142 backend controllers',
    '284 persistence data objects',
    '283 MyBatis mappers',
    '731 backend tests',
  ].join('\n'))
  writeFileSync(path.join(root, 'docs/ddd-rewrite/inventory/check-inventory-readiness.sh'), 'old class:\ntarget module:')
  mkdirSync(path.join(root, 'docs/ddd-rewrite/task-packs'), { recursive: true })
  writeFileSync(path.join(root, 'docs/ddd-rewrite/task-packs/00-coordinator-foundation.md'), coordinatorPack({
    task: 'DDD-C00',
    readiness: 'R1 foundation',
    target: 'DDD_FINAL_MODULE skeleton only',
  }))
  writeFileSync(path.join(root, 'docs/ddd-rewrite/task-packs/06a-coordinator-canvas-execution-contract-freeze.md'), coordinatorPack({
    task: 'DDD-C07',
    readiness: 'R4 candidate',
    target: 'DDD_FINAL_MODULE',
  }))
  writeFileSync(path.join(root, 'docs/ddd-rewrite/task-packs/09-coordinator-web-boot-cutover.md'), coordinatorPack({
    task: 'DDD-C09',
    readiness: 'R6 cutover',
    target: 'DDD_FINAL_MODULE',
  }))
  for (const file of [
    '01-worker-platform.md',
    '02-worker-risk.md',
    '03-worker-marketing.md',
    '04-worker-cdp.md',
    '05-worker-bi.md',
    '06-worker-conversation.md',
    '07-worker-canvas.md',
    '08-worker-execution.md',
  ]) {
    writeFileSync(path.join(root, 'docs/ddd-rewrite/task-packs', file), dddWorkerTaskPack())
  }

  return root
}

test('program coordination checks ignore their own regex literals', async () => {
  const root = await fixture()

  const output = execFileSync('bash', ['docs/program-coordination/checks/program-coordination-checks.sh', '.'], {
    cwd: root,
    encoding: 'utf8',
  })

  assert.match(output, /Program coordination checks passed/)
})

function runCheck(root) {
  return spawnSync('bash', ['docs/program-coordination/checks/program-coordination-checks.sh', '.'], {
    cwd: root,
    encoding: 'utf8',
  })
}

function cutoverReadyDispatchState() {
  return {
    schemaVersion: 1,
    updatedAt: '2026-06-15T18:53:56+08:00',
    readiness: {
      level: 'R5',
      gate: 'Cutover preflight ready with frontend validation dependency gates',
      backendTarget: 'cutoverReady true with blockers empty',
      writeMode: 'No active dispatches',
    },
    activeDispatches: [],
    workerBoard: [
      {
        taskId: 'DDD-E01',
        status: 'DONE',
        mode: 'read-only',
        gate: 'R0',
        writeScope: [],
      },
      {
        taskId: 'OSG-W02',
        status: 'DONE',
        mode: 'code-writing',
        gate: 'G0/G1',
        writeScope: ['docker-compose.demo.yml', 'wiremock/**', 'docs/open-source/playground.md'],
      },
      {
        taskId: 'OSG-W07F',
        status: 'NOT_STARTED',
        mode: 'code-writing',
        gate: 'G9/G10 plus OSG-C07',
        writeScope: ['backend/canvas-context-execution/src/main/java/org/chovy/canvas/execution/adapter/plugin/official/risk/**'],
      },
    ],
    parallelGroups: [
      {
        groupId: 'P1-immediate-shell',
        startCondition: 'Coordination docs accepted',
        maxCodeWriters: 6,
        maxReadOnlyReviewers: 4,
        taskIds: ['DDD-E01', 'OSG-W02'],
        notes: 'Fixture group.',
      },
      {
        groupId: 'P7-plugin-burst',
        startCondition: 'DDD-W08 integrated and OSG-C07 done',
        maxCodeWriters: 6,
        maxReadOnlyReviewers: 2,
        taskIds: ['OSG-W07F'],
        notes: 'Fixture group.',
      },
    ],
    reviewerBoard: [],
    recoveryAudit: {
      status: 'clean',
      activeDispatches: 0,
      commandsToRunBeforeDispatch: ['G0', 'G0B before code-writing'],
    },
    lastVerifiedEvidence: [
      {
        command: 'node tools/program-coordination/cutover-compatibility-preflight.mjs . --require-ready --json',
        result: 'cutoverReady true; blockers empty',
      },
    ],
    lastVerifiedAt: '2026-06-15T18:53:56+08:00',
    lastEvent: 'cutoverReady true with blockers empty',
  }
}

function writeCutoverReadyDispatchState(root) {
  writeFileSync(
    path.join(root, 'docs/program-coordination/dispatch-state.json'),
    `${JSON.stringify(cutoverReadyDispatchState(), null, 2)}\n`,
  )
}

test('program coordination checks reject incomplete worker packet sections', async () => {
  const root = await fixture()
  const packetPath = path.join(root, 'docs/program-coordination/subagent-worker-packets.md')
  writeFileSync(packetPath, readFileSync(packetPath, 'utf8').replace(
    'Allowed module POM edits:\n  none unless coordinator handoff names an exact dependency or plugin and the\n  exact module pom.xml',
    'Allowed module POM edits removed for negative fixture',
  ))

  const result = runCheck(root)
  assert.notEqual(result.status, 0)
  assert.match(`${result.stdout}\n${result.stderr}`, /DDD-W01.*Allowed module POM edits/)
})

test('program coordination checks reject incomplete coordinator packets', async () => {
  const root = await fixture()
  const c07Path = path.join(root, 'docs/ddd-rewrite/task-packs/06a-coordinator-canvas-execution-contract-freeze.md')
  writeFileSync(c07Path, readFileSync(c07Path, 'utf8').replace('## Forbidden Changes\n```text\nunassigned worker edits\n```\n', ''))

  const result = runCheck(root)
  assert.notEqual(result.status, 0)
  assert.match(`${result.stdout}\n${result.stderr}`, /06a-coordinator-canvas-execution-contract-freeze\.md must contain: ## Forbidden Changes/)
})

test('program coordination checks reject missing progress ledger write rule', async () => {
  const root = await fixture()
  const ledgerPath = path.join(root, 'docs/program-coordination/progress-ledger.md')
  writeFileSync(ledgerPath, readFileSync(ledgerPath, 'utf8').replace(
    'The coordinator is the single writer for this file.',
    'Multiple workers may update this file.',
  ))

  const result = runCheck(root)
  assert.notEqual(result.status, 0)
  assert.match(`${result.stdout}\n${result.stderr}`, /progress-ledger\.md must contain: The coordinator is the single writer for this file\./)
})

test('program coordination checks reject incomplete ledger recovery checklist', async () => {
  const root = await fixture()
  const ledgerPath = path.join(root, 'docs/program-coordination/progress-ledger.md')
  writeFileSync(ledgerPath, readFileSync(ledgerPath, 'utf8').replace(
    'git status --short',
    'status command omitted',
  ))

  const result = runCheck(root)
  assert.notEqual(result.status, 0)
  assert.match(`${result.stdout}\n${result.stderr}`, /progress-ledger\.md must contain: git status --short/)
})

test('program coordination checks reject active worker board rows when active registry is none', async () => {
  const root = await fixture()
  const ledgerPath = path.join(root, 'docs/program-coordination/progress-ledger.md')
  const ledger = readFileSync(ledgerPath, 'utf8')
    .replace('## Active Dispatch Registry', '## Active Dispatch Registry\n```text\nnone\n```')
    .replace('## Worker Board', [
      '## Worker Board',
      '| Task | Status | Gate |',
      '| --- | --- | --- |',
      '| OSG-W08 template content/catalog | RUNNING | G0/G1 |',
    ].join('\n'))
    .replace('## Reviewer Board', '## Reviewer Board')
  writeFileSync(ledgerPath, ledger)

  const result = runCheck(root)
  assert.notEqual(result.status, 0)
  assert.match(`${result.stdout}\n${result.stderr}`, /Active Dispatch Registry is none but Worker Board contains active worker status/)
})

test('program coordination checks reject stale current snapshot when dispatch state is cutover-ready', async () => {
  const root = await fixture()
  const ledgerPath = path.join(root, 'docs/program-coordination/progress-ledger.md')
  writeFileSync(ledgerPath, readFileSync(ledgerPath, 'utf8').replace(
    /## Current Snapshot[\s\S]*?## Active Dispatch Registry/,
    [
      '## Current Snapshot',
      '',
      '| Field | Value |',
      '| --- | --- |',
      '| Current backend target | DDD-C09 final cutover remains blocked by production canvas-web controller/endpoint gaps; next preflight top gap is `route:/canvas/batch` |',
      '',
      '## Active Dispatch Registry',
    ].join('\n'),
  ))
  writeCutoverReadyDispatchState(root)

  const result = runCheck(root)
  assert.notEqual(result.status, 0)
  assert.match(`${result.stdout}\n${result.stderr}`, /Current Snapshot is stale: cutover-ready evidence exists but snapshot still says cutover is blocked/)
  assert.match(`${result.stdout}\n${result.stderr}`, /Current Snapshot is stale: cutover-ready evidence exists but snapshot still names a next preflight route gap/)
})

test('program coordination checks reject stale latest closed dispatch when dispatch state is cutover-ready', async () => {
  const root = await fixture()
  const ledgerPath = path.join(root, 'docs/program-coordination/progress-ledger.md')
  const ledger = readFileSync(ledgerPath, 'utf8')
    .replace(
      /## Current Snapshot[\s\S]*?## Active Dispatch Registry/,
      [
        '## Current Snapshot',
        '',
        '| Field | Value |',
        '| --- | --- |',
        '| Current backend target | DDD-C09 cutover preflight ready; cutoverReady true with blockers empty |',
        '',
        '## Active Dispatch Registry',
      ].join('\n'),
    )
    .replace(
      'Last Verified Evidence',
      [
        'Last Verified Evidence',
        '',
        'Latest closed dispatch:',
        '- DDD-C09: global cutover remains blocked; next top gap is `route:/canvas/batch`.',
      ].join('\n'),
    )
  writeFileSync(ledgerPath, ledger)
  writeCutoverReadyDispatchState(root)

  const result = runCheck(root)
  assert.notEqual(result.status, 0)
  assert.match(`${result.stdout}\n${result.stderr}`, /Latest closed dispatch is stale: cutover-ready evidence exists but closed dispatch still says cutover is blocked/)
  assert.match(`${result.stdout}\n${result.stderr}`, /Latest closed dispatch is stale: cutover-ready evidence exists but closed dispatch still names a next preflight route gap/)
})

test('program coordination checks reject missing collaboration recovery protocol', async () => {
  const root = await fixture()
  writeFileSync(path.join(root, 'docs/program-coordination/collaboration-and-recovery-protocol.md'), 'missing protocol sections')

  const result = runCheck(root)
  assert.notEqual(result.status, 0)
  assert.match(`${result.stdout}\n${result.stderr}`, /collaboration-and-recovery-protocol\.md must contain: Active Dispatch Registry/)
})

test('program coordination checks reject invalid dispatch state', async () => {
  const root = await fixture()
  writeFileSync(path.join(root, 'docs/program-coordination/dispatch-state.json'), JSON.stringify({
    schemaVersion: 1,
    updatedAt: '2026-06-09',
    readiness: {
      level: 'R0',
      gate: 'G0/G1/G2',
      backendTarget: 'no code worker active',
      writeMode: 'coordinator-only for planning docs',
    },
    activeDispatches: [
      {
        dispatchId: 'DISPATCH-BAD',
        taskId: 'DDD-W01',
        status: 'RUNNING',
        worker: 'worker-1',
        mode: 'code-writing',
        branch: 'work/bad',
        worktree: '../bad',
        integrationTarget: 'DDD_FINAL_MODULE',
        exactReservedFiles: ['docs/program-coordination/progress-ledger.md'],
        coordinatorOwnedExceptions: [],
        gateAtDispatch: 'G4',
        lastCommandResult: 'not run',
        evidencePath: 'docs/program-coordination/evidence/DISPATCH-BAD/',
        nextAction: 'bad',
        rollbackPointer: 'none',
      },
    ],
    workerBoard: [
      {
        taskId: 'DDD-W01',
        status: 'READY',
        mode: 'code-writing',
        gate: 'G4',
        writeScope: ['backend/canvas-platform/**'],
      },
    ],
    reviewerBoard: [],
    recoveryAudit: {
      status: 'dirty',
      activeDispatches: 1,
      commandsToRunBeforeDispatch: ['G0', 'G1', 'G2'],
    },
    lastVerifiedEvidence: [],
  }, null, 2))

  const result = runCheck(root)
  assert.notEqual(result.status, 0)
  assert.match(`${result.stdout}\n${result.stderr}`, /dispatch-state\.json failed machine validation/)
  assert.match(`${result.stdout}\n${result.stderr}`, /progress-ledger\.md cannot be reserved/)
})

test('program coordination checks reject stale OSG-W02 demo doc dispatch scope', async () => {
  const root = await fixture()
  const statePath = path.join(root, 'docs/program-coordination/dispatch-state.json')
  writeFileSync(statePath, readFileSync(statePath, 'utf8').replace(
    'docs/open-source/playground.md',
    'docs/open-source/demo.md',
  ))

  const result = runCheck(root)
  assert.notEqual(result.status, 0)
  assert.match(`${result.stdout}\n${result.stderr}`, /stale OSG-W02 demo\.md path/)
})

test('program coordination checks reject incomplete G7 contract evidence', async () => {
  const root = await fixture()
  const matrixPath = path.join(root, 'docs/program-coordination/gate-verification-matrix.md')
  writeFileSync(matrixPath, readFileSync(matrixPath, 'utf8').replace(
    'PublishedCanvasDefinitionProvider.java',
    'PublishedCanvasDefinitionProvider omitted',
  ))

  const result = runCheck(root)
  assert.notEqual(result.status, 0)
  assert.match(`${result.stdout}\n${result.stderr}`, /gate-verification-matrix\.md must contain: PublishedCanvasDefinitionProvider\.java/)
})

test('program coordination checks reject stale DDD inventory README snapshot counts', async () => {
  const root = await fixture()
  const inventoryReadmePath = path.join(root, 'docs/ddd-rewrite/inventory/README.md')
  writeFileSync(inventoryReadmePath, readFileSync(inventoryReadmePath, 'utf8').replace(
    '142 backend controllers',
    '141 backend controllers',
  ))

  const result = runCheck(root)
  assert.notEqual(result.status, 0)
  assert.match(`${result.stdout}\n${result.stderr}`, /inventory\/README\.md must contain: 142 backend controllers/)
})
