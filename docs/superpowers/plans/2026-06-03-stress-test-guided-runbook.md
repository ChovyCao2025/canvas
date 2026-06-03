# Stress Test Guided Runbook Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Replace the current manual stress-test material with a guided, correctness-first local capacity testing workflow that non-specialist users can run safely.

**Architecture:** Keep the existing focused `tools/perf` scripts as low-level building blocks, then add `perf-guide.mjs` as the user-facing orchestration layer. Correctness gates stay in `verifier.mjs`; the guide refuses to report capacity from failed or incomplete runs. Documentation is rebuilt around one `docs/stressTest/README.md` entry point and no parallel legacy runbook.

**Tech Stack:** Node.js ESM, native `node:test`, Node `crypto` HMAC-SHA256, `child_process.spawnSync`, Markdown docs, Docker Compose, MySQL CLI, RocketMQ Maven producer.

---

## Scope Check

The approved spec covers one cohesive workflow: stress-test execution and reporting. It touches scripts and docs, but all changes serve the same user path: doctor -> fixture -> smoke -> threshold -> soak -> report -> cleanup. Keep it in one implementation plan.

## File Structure

- Modify: `tools/perf/perf-runner.mjs`
  - Add event HMAC signing helpers and safe summary metadata.
- Modify: `tools/perf/perf-runner.test.mjs`
  - Cover HMAC signature generation, signed event headers, secret-source metadata, and no secret leakage.
- Modify: `tools/perf/threshold-runner.mjs`
  - Accept event secret flags/env and pass them to `perf-runner.mjs`.
- Modify: `tools/perf/threshold-runner.test.mjs`
  - Cover event secret flag parsing and runner argument rendering.
- Modify: `tools/perf/hardening-profile.mjs`
  - Render event profiles with `--event-secret-env PERF_EVENT_SECRET`.
- Modify: `tools/perf/hardening-profile.test.mjs`
  - Cover rendered event hardening command includes the event secret env flag.
- Modify: `tools/perf/cleanup.mjs`
  - Add `--scope ledger|all`; default to `ledger`.
- Modify: `tools/perf/cleanup.test.mjs`
  - Cover ledger-only cleanup preserves `PERF_%` fixture definitions and all-scope cleanup removes them.
- Create: `tools/perf/perf-guide.mjs`
  - User-facing guide CLI with `doctor`, `fixture`, `smoke`, `threshold`, `soak`, `report`, and `cleanup` subcommands.
- Create: `tools/perf/perf-guide.test.mjs`
  - Cover argument parsing, failure-stop behavior, report gating, dry-run cleanup, and fixture rebuild safety.
- Modify: `tools/perf/README.md`
  - Make `perf-guide.mjs` the recommended interface and keep low-level scripts as advanced usage.
- Create or replace: `docs/stressTest/README.md`
  - Single stress-test entry point.
- Create: `docs/stressTest/local-capacity-runbook.md`
  - Complete guided runbook.
- Create: `docs/stressTest/performance-audit.md`
  - Audit findings and rationale for removing old execution paths.
- Create: `docs/stressTest/report-template.md`
  - Evidence-driven report template.
- Delete: `docs/stressTest/2026-05-27-local-container-capacity-testing-design.md`
  - Superseded by `local-capacity-runbook.md`.
- Delete: `docs/stressTest/老板汇报版-并发评估摘要.md`
  - Not tied to measured run evidence.
- Delete: `docs/stressTest/并发量评估报告.md`
  - Not tied to measured run evidence.

## Task 1: Add Event HMAC Support To Perf Runner

**Files:**
- Modify: `tools/perf/perf-runner.mjs`
- Modify: `tools/perf/perf-runner.test.mjs`

- [ ] **Step 1: Add failing HMAC helper tests**

Add these imports to `tools/perf/perf-runner.test.mjs`:

```js
import {
  buildEventPayload,
  buildDirectPayload,
  buildEventSignatureHeaders,
  buildSignedHeaders,
  chunkSeq,
  exitCodeForSummary,
  isCliEntrypoint,
  parseRunnerArgs,
  resolveEventSecret,
  run,
} from './perf-runner.mjs'
```

Add these tests:

```js
test('buildEventSignatureHeaders signs timestamp and raw body', () => {
  const headers = buildEventSignatureHeaders({
    secret: '12345678901234567890123456789012',
    timestamp: '1760000000000',
    rawBody: '{"eventCode":"PERF_ORDER_PAID"}',
  })

  assert.equal(headers['X-Canvas-Timestamp'], '1760000000000')
  assert.equal(
    headers['X-Canvas-Signature'],
    'sha256=b459181d5e6609aecf71072654181700c178d19e765700e19ce36fa458be21da',
  )
})

test('resolveEventSecret prefers explicit flag over environment', () => {
  assert.deepEqual(resolveEventSecret({
    mode: 'event',
    eventSecret: '12345678901234567890123456789012',
    eventSecretEnv: 'PERF_EVENT_SECRET',
  }, {
    PERF_EVENT_SECRET: 'aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa',
  }), {
    value: '12345678901234567890123456789012',
    source: 'flag',
  })
})

test('resolveEventSecret reads configured environment variable', () => {
  assert.deepEqual(resolveEventSecret({
    mode: 'event',
    eventSecret: '',
    eventSecretEnv: 'PERF_EVENT_SECRET',
  }, {
    PERF_EVENT_SECRET: 'aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa',
  }), {
    value: 'aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa',
    source: 'env:PERF_EVENT_SECRET',
  })
})

test('buildSignedHeaders returns JSON headers when no event secret is configured', () => {
  const headers = buildSignedHeaders({
    args: { mode: 'event', eventSecret: '', eventSecretEnv: 'PERF_EVENT_SECRET' },
    rawBody: '{}',
    nowMs: () => 1760000000000,
    env: {},
  })

  assert.deepEqual(headers, {
    'content-type': 'application/json',
  })
})

test('buildSignedHeaders includes event HMAC headers when secret is configured', () => {
  const headers = buildSignedHeaders({
    args: { mode: 'event', eventSecret: '', eventSecretEnv: 'PERF_EVENT_SECRET' },
    rawBody: '{"eventCode":"PERF_ORDER_PAID"}',
    nowMs: () => 1760000000000,
    env: { PERF_EVENT_SECRET: '12345678901234567890123456789012' },
  })

  assert.equal(headers['content-type'], 'application/json')
  assert.equal(headers['X-Canvas-Timestamp'], '1760000000000')
  assert.equal(
    headers['X-Canvas-Signature'],
    'sha256=b459181d5e6609aecf71072654181700c178d19e765700e19ce36fa458be21da',
  )
})

test('parseRunnerArgs accepts event secret env flag without exposing a secret value', () => {
  const args = parseRunnerArgs([
    '--mode', 'event',
    '--perf-run-id', 'perf_20260523_001',
    '--event-secret-env', 'CANVAS_EVENT_REPORT_SECRET',
  ])

  assert.equal(args.eventSecret, '')
  assert.equal(args.eventSecretEnv, 'CANVAS_EVENT_REPORT_SECRET')
})
```

- [ ] **Step 2: Run the focused failing test**

Run:

```bash
node --test tools/perf/perf-runner.test.mjs
```

Expected: FAIL with an export error for `buildEventSignatureHeaders`.

- [ ] **Step 3: Implement event HMAC signing in `perf-runner.mjs`**

Add this import:

```js
import { createHmac } from 'node:crypto'
```

Extend defaults and flags:

```js
const DEFAULT_ARGS = {
  mode: 'event',
  baseUrl: 'http://localhost:8080',
  perfRunId: '',
  count: 1000,
  concurrency: 20,
  eventCode: 'PERF_ORDER_PAID',
  canvasId: '',
  audienceId: '',
  userPrefix: 'perf_user_',
  userModulo: 1000,
  duplicateRate: 0,
  summaryFile: '',
  eventSecret: '',
  eventSecretEnv: 'PERF_EVENT_SECRET',
}
```

```js
const FLAG_NAMES = {
  '--mode': 'mode',
  '--base-url': 'baseUrl',
  '--perf-run-id': 'perfRunId',
  '--count': 'count',
  '--concurrency': 'concurrency',
  '--event-code': 'eventCode',
  '--canvas-id': 'canvasId',
  '--audience-id': 'audienceId',
  '--user-prefix': 'userPrefix',
  '--user-modulo': 'userModulo',
  '--duplicate-rate': 'duplicateRate',
  '--summary-file': 'summaryFile',
  '--event-secret': 'eventSecret',
  '--event-secret-env': 'eventSecretEnv',
}
```

Add these helpers above `sendRequest`:

```js
export function buildEventSignatureHeaders({ secret, timestamp, rawBody }) {
  const signature = createHmac('sha256', secret)
    .update(`${timestamp}\n${rawBody}`)
    .digest('hex')

  return {
    'X-Canvas-Timestamp': String(timestamp),
    'X-Canvas-Signature': `sha256=${signature}`,
  }
}

export function resolveEventSecret(args, env = process.env) {
  if (args.mode !== 'event') {
    return { value: '', source: 'none' }
  }

  if (args.eventSecret) {
    return { value: args.eventSecret, source: 'flag' }
  }

  const envName = args.eventSecretEnv || 'PERF_EVENT_SECRET'
  const envValue = env[envName] || ''
  if (envValue) {
    return { value: envValue, source: `env:${envName}` }
  }

  return { value: '', source: 'none' }
}

export function buildSignedHeaders({
  args,
  rawBody,
  nowMs = () => Date.now(),
  env = process.env,
}) {
  const headers = {
    'content-type': 'application/json',
  }
  const secret = resolveEventSecret(args, env)

  if (!secret.value) {
    return headers
  }

  return {
    ...headers,
    ...buildEventSignatureHeaders({
      secret: secret.value,
      timestamp: String(nowMs()),
      rawBody,
    }),
  }
}
```

Change `sendRequest` to create the raw body once and sign it:

```js
async function sendRequest(args, seq, { performanceNow, nowMs = () => Date.now(), env = process.env }) {
  const request = buildRequest(args, seq)
  const rawBody = JSON.stringify(request.body)
  const startedAt = performanceNow()

  try {
    const response = await fetch(request.url, {
      method: 'POST',
      headers: buildSignedHeaders({ args, rawBody, nowMs, env }),
      body: rawBody,
    })

    return {
      ok: response.ok,
      durationMs: performanceNow() - startedAt,
    }
  } catch (error) {
    return {
      ok: false,
      durationMs: performanceNow() - startedAt,
      error,
    }
  }
}
```

Change `summarySettings` so it reports only safe metadata:

```js
function summarySettings(args, env = process.env) {
  const eventSecret = resolveEventSecret(args, env)

  return {
    mode: args.mode,
    baseUrl: args.baseUrl,
    count: args.count,
    concurrency: args.concurrency,
    eventCode: args.eventCode,
    canvasId: args.canvasId,
    audienceId: args.audienceId,
    userPrefix: args.userPrefix,
    userModulo: args.userModulo,
    duplicateRate: args.duplicateRate || 0,
    duplicateCount: args.duplicateCount || 0,
    eventSignature: {
      enabled: Boolean(eventSecret.value),
      source: eventSecret.source,
    },
  }
}
```

In `run`, pass `env` and `nowMs` dependencies:

```js
export async function run(args, deps = {}) {
  const now = deps.now || (() => new Date().toISOString())
  const nowMs = deps.nowMs || (() => Date.now())
  const env = deps.env || process.env
  const performanceNow = deps.performanceNow || (() => performance.now())
  const getMachineMetadata = deps.machineMetadata || machineMetadata
  const duplicateCount = args.mode === 'direct'
    ? duplicateCountFor(args.count, args.duplicateRate || 0)
    : 0
  const runArgs = {
    ...args,
    duplicateCount,
  }
  const startedAt = now()
  const startedPerf = performanceNow()
  let sent = 0
  let success = 0
  let failed = 0
  const durations = []

  for (const chunk of chunkSeq(runArgs.count, runArgs.concurrency)) {
    const results = await Promise.all(
      chunk.map(async (seq) => {
        sent += 1
        return sendRequest(runArgs, seq, { performanceNow, nowMs, env })
      }),
    )

    for (const result of results) {
      durations.push(result.durationMs)

      if (result.ok) {
        success += 1
      } else {
        failed += 1
      }
    }
  }

  durations.sort((left, right) => left - right)
  const p95Index = Math.min(
    durations.length - 1,
    Math.max(0, Math.ceil(durations.length * 0.95) - 1),
  )
  const p95Ms = durations.length === 0 ? 0 : durations[p95Index]
  const finishedAt = now()
  const durationMs = performanceNow() - startedPerf

  return {
    perfRunId: runArgs.perfRunId,
    mode: runArgs.mode,
    sent,
    success,
    failed,
    p95Ms,
    startedAt,
    finishedAt,
    durationMs,
    settings: summarySettings(runArgs, env),
    machine: getMachineMetadata(),
  }
}
```

- [ ] **Step 4: Add a run-level no-secret-leakage test**

Add this test:

```js
test('run summary records event signature source without leaking secret', async () => {
  const summary = await run(parseRunnerArgs([
    '--mode', 'event',
    '--perf-run-id', 'perf_20260523_001',
    '--count', '0',
    '--event-secret-env', 'PERF_EVENT_SECRET',
  ]), {
    env: { PERF_EVENT_SECRET: '12345678901234567890123456789012' },
    machineMetadata: () => ({}),
  })

  assert.deepEqual(summary.settings.eventSignature, {
    enabled: true,
    source: 'env:PERF_EVENT_SECRET',
  })
  assert.doesNotMatch(JSON.stringify(summary), /12345678901234567890123456789012/)
})
```

- [ ] **Step 5: Run perf-runner tests**

Run:

```bash
node --test tools/perf/perf-runner.test.mjs
```

Expected: PASS.

- [ ] **Step 6: Commit**

```bash
git add tools/perf/perf-runner.mjs tools/perf/perf-runner.test.mjs
git commit -m "feat: sign event performance requests"
```

## Task 2: Pass Event Secret Through Threshold And Hardening Commands

**Files:**
- Modify: `tools/perf/threshold-runner.mjs`
- Modify: `tools/perf/threshold-runner.test.mjs`
- Modify: `tools/perf/hardening-profile.mjs`
- Modify: `tools/perf/hardening-profile.test.mjs`

- [ ] **Step 1: Add failing threshold runner argument tests**

In `tools/perf/threshold-runner.mjs`, export `runnerArgs`:

```js
export function runnerArgs({ config, stage, perfRunId, summaryFile }) {
```

In `tools/perf/threshold-runner.test.mjs`, add `parseThresholdArgs` and `runnerArgs` imports:

```js
import {
  classifyStage,
  parseStages,
  parseThresholdArgs,
  runnerArgs,
  runThresholdPlan,
} from './threshold-runner.mjs'
```

Add tests:

```js
test('parseThresholdArgs accepts event secret env', () => {
  const args = parseThresholdArgs([
    '--mode', 'event',
    '--event-secret-env', 'CANVAS_EVENT_REPORT_SECRET',
  ])

  assert.equal(args.eventSecret, '')
  assert.equal(args.eventSecretEnv, 'CANVAS_EVENT_REPORT_SECRET')
})

test('runnerArgs passes event secret env to perf runner for event mode', () => {
  const args = runnerArgs({
    config: {
      mode: 'event',
      baseUrl: 'http://localhost:8080',
      eventCode: 'PERF_ORDER_PAID',
      eventSecret: '',
      eventSecretEnv: 'PERF_EVENT_SECRET',
    },
    stage: { count: 100, concurrency: 10 },
    perfRunId: 'perf_20260523_001',
    summaryFile: 'tmp/perf_20260523_001.json',
  })

  assert.deepEqual(args.slice(-2), ['--event-secret-env', 'PERF_EVENT_SECRET'])
})

test('runnerArgs passes explicit event secret when configured', () => {
  const args = runnerArgs({
    config: {
      mode: 'event',
      baseUrl: 'http://localhost:8080',
      eventCode: 'PERF_ORDER_PAID',
      eventSecret: '12345678901234567890123456789012',
      eventSecretEnv: 'PERF_EVENT_SECRET',
    },
    stage: { count: 100, concurrency: 10 },
    perfRunId: 'perf_20260523_001',
    summaryFile: 'tmp/perf_20260523_001.json',
  })

  assert.deepEqual(args.slice(-2), ['--event-secret', '12345678901234567890123456789012'])
})
```

- [ ] **Step 2: Run threshold tests to verify failure**

Run:

```bash
node --test tools/perf/threshold-runner.test.mjs
```

Expected: FAIL with `Unknown flag: --event-secret-env` or missing export.

- [ ] **Step 3: Implement threshold event secret flags**

In `tools/perf/threshold-runner.mjs`, extend defaults:

```js
const DEFAULT_ARGS = {
  mode: 'event',
  baseUrl: 'http://localhost:8080',
  eventCode: 'PERF_ORDER_PAID',
  canvasId: '',
  stages: '1000:10,5000:50,10000:100,30000:200,50000:400',
  matchedCanvasCount: 1,
  maxFailed: 0,
  maxP95Ms: 0,
  waitAfterRunMs: 10000,
  outDir: 'tmp/perf-threshold',
  runIdPrefix: '',
  mysql: 'mysql',
  database: 'canvas_db',
  eventSecret: '',
  eventSecretEnv: 'PERF_EVENT_SECRET',
}
```

Extend flags:

```js
const FLAG_NAMES = {
  '--mode': 'mode',
  '--base-url': 'baseUrl',
  '--event-code': 'eventCode',
  '--canvas-id': 'canvasId',
  '--stages': 'stages',
  '--matched-canvas-count': 'matchedCanvasCount',
  '--max-failed': 'maxFailed',
  '--max-p95-ms': 'maxP95Ms',
  '--wait-after-run-ms': 'waitAfterRunMs',
  '--out-dir': 'outDir',
  '--run-id-prefix': 'runIdPrefix',
  '--mysql': 'mysql',
  '--database': 'database',
  '--event-secret': 'eventSecret',
  '--event-secret-env': 'eventSecretEnv',
}
```

At the end of `runnerArgs`, before return:

```js
  if (config.mode === 'event') {
    if (config.eventSecret) {
      args.push('--event-secret', config.eventSecret)
    } else if (config.eventSecretEnv) {
      args.push('--event-secret-env', config.eventSecretEnv)
    }
  }

  return args
```

- [ ] **Step 4: Run threshold tests**

Run:

```bash
node --test tools/perf/threshold-runner.test.mjs
```

Expected: PASS.

- [ ] **Step 5: Add failing hardening render test**

In `tools/perf/hardening-profile.test.mjs`, extend the render test:

```js
  assert.match(command, /--event-secret-env PERF_EVENT_SECRET/)
```

Run:

```bash
node --test tools/perf/hardening-profile.test.mjs
```

Expected: FAIL because rendered command does not include the event secret env flag.

- [ ] **Step 6: Implement hardening render secret flag**

In `tools/perf/hardening-profile.mjs`, inside `renderThresholdCommand`, after event code insertion:

```js
  if (profile.mode === 'event') {
    args.push('--event-secret-env PERF_EVENT_SECRET')
  }
```

- [ ] **Step 7: Run focused tests**

Run:

```bash
node --test tools/perf/threshold-runner.test.mjs tools/perf/hardening-profile.test.mjs
```

Expected: PASS.

- [ ] **Step 8: Commit**

```bash
git add tools/perf/threshold-runner.mjs tools/perf/threshold-runner.test.mjs tools/perf/hardening-profile.mjs tools/perf/hardening-profile.test.mjs
git commit -m "feat: pass event signing settings through perf thresholds"
```

## Task 3: Make Cleanup Scope Safe By Default

**Files:**
- Modify: `tools/perf/cleanup.mjs`
- Modify: `tools/perf/cleanup.test.mjs`

- [ ] **Step 1: Add failing cleanup scope tests**

Update imports in `tools/perf/cleanup.test.mjs`:

```js
import test from 'node:test'
import assert from 'node:assert/strict'
import { buildCleanupSql, escapeSql, parseCleanupArgs } from './cleanup.mjs'
```

Replace the existing namespace deletion test with:

```js
test('buildCleanupSql with ledger scope preserves PERF namespace rows', () => {
  const sql = buildCleanupSql('perf_20260523_001', { scope: 'ledger' })

  assert.match(sql, /canvas_execution_trace/)
  assert.match(sql, /audience_compute_run/)
  assert.match(sql, /perf_run_id = 'perf_20260523_001'/)
  assert.doesNotMatch(sql, /DELETE FROM event_definition WHERE event_code LIKE 'PERF_%'/)
  assert.doesNotMatch(sql, /DELETE FROM mq_message_definition WHERE message_code LIKE 'PERF_%'/)
  assert.doesNotMatch(sql, /DELETE FROM canvas;$/m)
  assert.doesNotMatch(sql, /DELETE FROM canvas_definition;$/m)
})

test('buildCleanupSql with all scope removes PERF namespace rows', () => {
  const sql = buildCleanupSql('perf_20260523_001', { scope: 'all' })

  assert.match(sql, /DELETE FROM event_definition WHERE event_code LIKE 'PERF_%'/)
  assert.match(sql, /DELETE FROM mq_message_definition WHERE message_code LIKE 'PERF_%'/)
})

test('parseCleanupArgs defaults to ledger scope dry run', () => {
  const args = parseCleanupArgs([
    '--perf-run-id', 'perf_20260523_001',
  ])

  assert.equal(args.scope, 'ledger')
  assert.equal(args.execute, false)
})

test('parseCleanupArgs accepts all scope', () => {
  const args = parseCleanupArgs([
    '--perf-run-id', 'perf_20260523_001',
    '--scope', 'all',
  ])

  assert.equal(args.scope, 'all')
})

test('parseCleanupArgs rejects unsupported scope', () => {
  assert.throws(() => parseCleanupArgs([
    '--perf-run-id', 'perf_20260523_001',
    '--scope', 'fixture',
  ]), /--scope must be ledger or all/)
})
```

- [ ] **Step 2: Run cleanup tests to verify failure**

Run:

```bash
node --test tools/perf/cleanup.test.mjs
```

Expected: FAIL because `--scope` is not supported and ledger scope still deletes fixture definitions.

- [ ] **Step 3: Implement scoped cleanup**

In `tools/perf/cleanup.mjs`, update defaults and flags:

```js
const DEFAULT_ARGS = {
  mysql: 'mysql',
  database: 'canvas_db',
  perfRunId: '',
  execute: false,
  scope: 'ledger',
}

const FLAG_NAMES = {
  '--mysql': 'mysql',
  '--database': 'database',
  '--perf-run-id': 'perfRunId',
  '--execute': 'execute',
  '--scope': 'scope',
}
```

Change the builder signature:

```js
export function buildCleanupSql(perfRunId, { scope = 'ledger' } = {}) {
  const id = escapeSql(perfRunId)
  const fixtureCleanupSql = scope === 'all'
    ? `
DELETE FROM event_definition WHERE event_code LIKE 'PERF_%';
DELETE FROM mq_message_definition WHERE message_code LIKE 'PERF_%';
`.trim()
    : ''
```

Replace the unconditional fixture deletes with:

```js
${fixtureCleanupSql}
```

Add scope parsing:

```js
function parseScope(flag, value) {
  if (value === 'ledger' || value === 'all') {
    return value
  }
  throw new Error(`${flag} must be ledger or all`)
}
```

Update `parseCleanupArgs` value assignment:

```js
    if (name === 'execute') {
      args[name] = parseBoolean(flag, value)
    } else if (name === 'scope') {
      args[name] = parseScope(flag, value)
    } else {
      args[name] = value
    }
```

Update `main`:

```js
  const sql = buildCleanupSql(args.perfRunId, { scope: args.scope })
```

- [ ] **Step 4: Run cleanup tests**

Run:

```bash
node --test tools/perf/cleanup.test.mjs
```

Expected: PASS.

- [ ] **Step 5: Commit**

```bash
git add tools/perf/cleanup.mjs tools/perf/cleanup.test.mjs
git commit -m "fix: default performance cleanup to run ledger scope"
```

## Task 4: Add Perf Guide Foundation And Report Gates

**Files:**
- Create: `tools/perf/perf-guide.mjs`
- Create: `tools/perf/perf-guide.test.mjs`

- [ ] **Step 1: Write failing guide parser and report gate tests**

Create `tools/perf/perf-guide.test.mjs`:

```js
import test from 'node:test'
import assert from 'node:assert/strict'

import {
  assertCapacityReportable,
  commandForCleanup,
  parseGuideArgs,
  runGuide,
} from './perf-guide.mjs'

test('parseGuideArgs parses subcommand and common flags', () => {
  const config = parseGuideArgs([
    'doctor',
    '--base-url', 'http://localhost:8080',
    '--run-root', 'tmp/perf-runs',
  ])

  assert.equal(config.command, 'doctor')
  assert.equal(config.baseUrl, 'http://localhost:8080')
  assert.equal(config.runRoot, 'tmp/perf-runs')
})

test('parseGuideArgs rejects unknown subcommand', () => {
  assert.throws(() => parseGuideArgs(['unknown']), /command must be one of/)
})

test('assertCapacityReportable accepts PASS verifier for capacity report', () => {
  assert.doesNotThrow(() => assertCapacityReportable({
    verdict: 'PASS',
  }, {
    reportType: 'capacity',
  }))
})

test('assertCapacityReportable rejects FAIL verifier', () => {
  assert.throws(() => assertCapacityReportable({
    verdict: 'FAIL',
  }, {
    reportType: 'capacity',
  }), /verifier verdict FAIL cannot be used for capacity reporting/)
})

test('assertCapacityReportable rejects expected failures for capacity report', () => {
  assert.throws(() => assertCapacityReportable({
    verdict: 'PASS_WITH_EXPECTED_FAILURES',
  }, {
    reportType: 'capacity',
  }), /PASS_WITH_EXPECTED_FAILURES is only allowed for fault reports/)
})

test('assertCapacityReportable accepts expected failures for fault report', () => {
  assert.doesNotThrow(() => assertCapacityReportable({
    verdict: 'PASS_WITH_EXPECTED_FAILURES',
  }, {
    reportType: 'fault',
  }))
})

test('commandForCleanup defaults to ledger dry run', () => {
  assert.deepEqual(commandForCleanup({
    perfRunId: 'perf_20260523_001',
    scope: 'ledger',
    execute: false,
  }), [
    process.execPath,
    [
      'tools/perf/cleanup.mjs',
      '--perf-run-id', 'perf_20260523_001',
      '--scope', 'ledger',
      '--execute', 'false',
    ],
  ])
})

test('runGuide dispatches doctor command', async () => {
  const calls = []
  const result = await runGuide(parseGuideArgs(['doctor']), {
    doctor: async () => {
      calls.push('doctor')
      return { status: 'PASS' }
    },
  })

  assert.equal(result.status, 'PASS')
  assert.deepEqual(calls, ['doctor'])
})
```

- [ ] **Step 2: Run guide tests to verify failure**

Run:

```bash
node --test tools/perf/perf-guide.test.mjs
```

Expected: FAIL because `perf-guide.mjs` does not exist.

- [ ] **Step 3: Create guide foundation**

Create `tools/perf/perf-guide.mjs`:

```js
#!/usr/bin/env node

import { existsSync, mkdirSync, readFileSync, writeFileSync } from 'node:fs'
import { spawnSync } from 'node:child_process'
import path from 'node:path'
import { pathToFileURL } from 'node:url'

const COMMANDS = new Set(['doctor', 'fixture', 'smoke', 'threshold', 'soak', 'report', 'cleanup'])

const DEFAULTS = {
  baseUrl: 'http://localhost:8080',
  runRoot: 'tmp/perf-runs',
  thresholdRoot: 'tmp/perf-threshold',
  perfRunId: '',
  mode: 'event',
  eventCode: 'PERF_ORDER_PAID',
  canvasId: '',
  mqTopic: 'CANVAS_MQ_TRIGGER',
  mqTag: 'PERF_MQ',
  matchedCanvasCount: 1,
  eventSecretEnv: 'PERF_EVENT_SECRET',
  mysql: 'mysql',
  database: 'canvas_db',
  scope: 'ledger',
  execute: false,
  rebuild: false,
  reportType: 'capacity',
  minDurationMin: 30,
}

const FLAG_NAMES = {
  '--base-url': 'baseUrl',
  '--run-root': 'runRoot',
  '--threshold-root': 'thresholdRoot',
  '--perf-run-id': 'perfRunId',
  '--mode': 'mode',
  '--event-code': 'eventCode',
  '--canvas-id': 'canvasId',
  '--mq-topic': 'mqTopic',
  '--mq-tag': 'mqTag',
  '--matched-canvas-count': 'matchedCanvasCount',
  '--event-secret-env': 'eventSecretEnv',
  '--mysql': 'mysql',
  '--database': 'database',
  '--scope': 'scope',
  '--execute': 'execute',
  '--rebuild': 'rebuild',
  '--report-type': 'reportType',
  '--min-duration-min': 'minDurationMin',
}

const NUMBER_FLAGS = new Set(['matchedCanvasCount', 'minDurationMin'])
const BOOLEAN_FLAGS = new Set(['execute', 'rebuild'])

function parseBoolean(flag, value) {
  if (value === 'true') return true
  if (value === 'false') return false
  throw new Error(`${flag} must be true or false`)
}

function parsePositiveInteger(flag, value) {
  if (!/^[1-9]\d*$/.test(value)) {
    throw new Error(`${flag} must be a positive integer`)
  }
  return Number(value)
}

export function parseGuideArgs(argv) {
  const [command, ...rest] = argv
  if (!COMMANDS.has(command)) {
    throw new Error(`command must be one of ${[...COMMANDS].join(', ')}`)
  }

  const args = { ...DEFAULTS, command }
  for (let index = 0; index < rest.length; index += 2) {
    const flag = rest[index]
    const name = FLAG_NAMES[flag]
    if (!name) {
      throw new Error(`Unknown flag: ${flag}`)
    }
    if (index + 1 >= rest.length || rest[index + 1] === '' || rest[index + 1].startsWith('--')) {
      throw new Error(`Missing value for ${flag}`)
    }
    const value = rest[index + 1]
    if (NUMBER_FLAGS.has(name)) {
      args[name] = parsePositiveInteger(flag, value)
    } else if (BOOLEAN_FLAGS.has(name)) {
      args[name] = parseBoolean(flag, value)
    } else {
      args[name] = value
    }
  }

  if (!['ledger', 'all'].includes(args.scope)) {
    throw new Error('--scope must be ledger or all')
  }
  if (!['capacity', 'fault'].includes(args.reportType)) {
    throw new Error('--report-type must be capacity or fault')
  }
  return args
}

export function runDirectory(config, perfRunId = config.perfRunId) {
  if (!perfRunId) {
    throw new Error('--perf-run-id is required')
  }
  return path.join(config.runRoot, perfRunId)
}

export function writeJson(filePath, value) {
  mkdirSync(path.dirname(filePath), { recursive: true })
  writeFileSync(filePath, `${JSON.stringify(value, null, 2)}\n`)
}

export function readJson(filePath) {
  return JSON.parse(readFileSync(filePath, 'utf8'))
}

export function assertCapacityReportable(verifier, { reportType }) {
  if (verifier.verdict === 'FAIL') {
    throw new Error('verifier verdict FAIL cannot be used for capacity reporting')
  }
  if (reportType === 'capacity' && verifier.verdict === 'PASS_WITH_EXPECTED_FAILURES') {
    throw new Error('PASS_WITH_EXPECTED_FAILURES is only allowed for fault reports')
  }
  if (verifier.verdict !== 'PASS' && verifier.verdict !== 'PASS_WITH_EXPECTED_FAILURES') {
    throw new Error(`unknown verifier verdict ${verifier.verdict}`)
  }
}

export function commandForCleanup(config) {
  return [
    process.execPath,
    [
      'tools/perf/cleanup.mjs',
      '--perf-run-id', config.perfRunId,
      '--scope', config.scope || 'ledger',
      '--execute', String(Boolean(config.execute)),
    ],
  ]
}

export function runCommand(command, args, options = {}) {
  const result = spawnSync(command, args, {
    cwd: options.cwd || process.cwd(),
    encoding: 'utf8',
    timeout: options.timeout,
  })
  if (result.error) {
    throw result.error
  }
  return result
}

async function defaultDoctor() {
  return { status: 'PASS' }
}

async function defaultFixture(config) {
  if (!config.rebuild) {
    return {
      status: 'DRY_RUN',
      message: 'fixture command requires --rebuild true to recreate PERF_ resources',
    }
  }
  return { status: 'READY' }
}

async function defaultCleanup(config) {
  const [command, args] = commandForCleanup(config)
  const result = runCommand(command, args)
  return {
    status: result.status === 0 ? 'PASS' : 'FAIL',
    command,
    args,
    stdout: result.stdout,
    stderr: result.stderr,
  }
}

async function defaultReport(config) {
  const directory = runDirectory(config)
  const verifierPath = path.join(directory, 'verifier.json')
  if (!existsSync(verifierPath)) {
    throw new Error(`missing verifier evidence at ${verifierPath}`)
  }
  const verifier = readJson(verifierPath)
  assertCapacityReportable(verifier, { reportType: config.reportType })
  return { status: 'PASS', verifierPath, verifierVerdict: verifier.verdict }
}

export async function runGuide(config, deps = {}) {
  const handlers = {
    doctor: deps.doctor || defaultDoctor,
    fixture: deps.fixture || defaultFixture,
    smoke: deps.smoke || (async () => ({ status: 'NOT_IMPLEMENTED' })),
    threshold: deps.threshold || (async () => ({ status: 'NOT_IMPLEMENTED' })),
    soak: deps.soak || (async () => ({ status: 'NOT_IMPLEMENTED' })),
    report: deps.report || defaultReport,
    cleanup: deps.cleanup || defaultCleanup,
  }
  return handlers[config.command](config)
}

export function isCliEntrypoint(moduleUrl, argvPath) {
  return Boolean(argvPath) && moduleUrl === pathToFileURL(argvPath).href
}

async function main() {
  const result = await runGuide(parseGuideArgs(process.argv.slice(2)))
  console.log(JSON.stringify(result, null, 2))
  process.exitCode = result.status === 'FAIL' ? 2 : 0
}

if (isCliEntrypoint(import.meta.url, process.argv[1])) {
  main().catch((error) => {
    console.error(error.message)
    process.exitCode = 1
  })
}
```

- [ ] **Step 4: Run guide foundation tests**

Run:

```bash
node --test tools/perf/perf-guide.test.mjs
```

Expected: PASS.

- [ ] **Step 5: Commit**

```bash
git add tools/perf/perf-guide.mjs tools/perf/perf-guide.test.mjs
git commit -m "feat: add performance guide foundation"
```

## Task 5: Implement Guide Doctor, Smoke, Threshold, Soak, And Fixture Safety

**Files:**
- Modify: `tools/perf/perf-guide.mjs`
- Modify: `tools/perf/perf-guide.test.mjs`

- [ ] **Step 1: Add failing guide workflow tests**

Append tests to `tools/perf/perf-guide.test.mjs`:

```js
test('fixture command refuses rebuild without explicit flag', async () => {
  const result = await runGuide(parseGuideArgs(['fixture']), {})

  assert.equal(result.status, 'DRY_RUN')
  assert.match(result.message, /--rebuild true/)
})

test('smoke stops after first verifier failure', async () => {
  const calls = []
  const result = await runGuide(parseGuideArgs([
    'smoke',
    '--perf-run-id', 'perf_smoke_001',
    '--canvas-id', '42',
  ]), {
    runScenario: async ({ mode }) => {
      calls.push(mode)
      return mode === 'direct'
        ? { summary: { success: 50, failed: 0 }, verifier: { verdict: 'FAIL' } }
        : { summary: { success: 100, failed: 0 }, verifier: { verdict: 'PASS' } }
    },
  })

  assert.equal(result.status, 'FAIL')
  assert.equal(result.failedMode, 'direct')
  assert.deepEqual(calls, ['direct'])
})

test('smoke runs direct and event when both pass', async () => {
  const calls = []
  const result = await runGuide(parseGuideArgs([
    'smoke',
    '--perf-run-id', 'perf_smoke_001',
    '--canvas-id', '42',
  ]), {
    runScenario: async ({ mode }) => {
      calls.push(mode)
      return { summary: { success: 1, failed: 0 }, verifier: { verdict: 'PASS' } }
    },
  })

  assert.equal(result.status, 'PASS')
  assert.deepEqual(calls, ['direct', 'event'])
})

test('threshold delegates to threshold-runner with event secret env', async () => {
  const calls = []
  const result = await runGuide(parseGuideArgs([
    'threshold',
    '--mode', 'event',
    '--event-secret-env', 'PERF_EVENT_SECRET',
  ]), {
    runCommand: (command, args) => {
      calls.push([command, args])
      return {
        status: 0,
        stdout: JSON.stringify({ verdict: 'MAX_STAGE_STABLE' }),
        stderr: '',
      }
    },
  })

  assert.equal(result.status, 'PASS')
  assert.equal(calls[0][0], process.execPath)
  assert.ok(calls[0][1].includes('--event-secret-env'))
  assert.ok(calls[0][1].includes('PERF_EVENT_SECRET'))
})

test('soak rejects run shorter than minimum duration', async () => {
  const result = await runGuide(parseGuideArgs([
    'soak',
    '--perf-run-id', 'perf_soak_001',
    '--min-duration-min', '30',
  ]), {
    runScenario: async () => ({
      summary: { durationMs: 60_000, success: 100, failed: 0 },
      verifier: { verdict: 'PASS' },
    }),
  })

  assert.equal(result.status, 'FAIL')
  assert.match(result.reason, /duration/)
})
```

- [ ] **Step 2: Run guide tests to verify failure**

Run:

```bash
node --test tools/perf/perf-guide.test.mjs
```

Expected: FAIL because smoke, threshold, and soak still return `NOT_IMPLEMENTED`.

- [ ] **Step 3: Implement command helpers in `perf-guide.mjs`**

Add this helper:

```js
export function parseJsonCommandResult(result, commandName) {
  if (result.status !== 0 && result.status !== 2) {
    throw new Error(`${commandName} failed: ${result.stderr || result.stdout}`)
  }
  try {
    return JSON.parse(result.stdout)
  } catch (error) {
    throw new Error(`${commandName} did not output JSON: ${error.message}`)
  }
}
```

Add scenario orchestration:

```js
export async function defaultRunScenario(config, { mode, count, concurrency }) {
  const perfRunId = `${config.perfRunId}_${mode}`
  const directory = runDirectory(config, perfRunId)
  mkdirSync(directory, { recursive: true })
  const summaryFile = path.join(directory, 'runner-summary.json')
  const runnerArgs = [
    'tools/perf/perf-runner.mjs',
    '--mode', mode,
    '--base-url', config.baseUrl,
    '--perf-run-id', perfRunId,
    '--count', String(count),
    '--concurrency', String(concurrency),
    '--summary-file', summaryFile,
  ]

  if (mode === 'direct') {
    runnerArgs.push('--canvas-id', config.canvasId)
  }
  if (mode === 'event') {
    runnerArgs.push('--event-code', config.eventCode)
    runnerArgs.push('--event-secret-env', config.eventSecretEnv)
  }

  const runner = parseJsonCommandResult(
    runCommand(process.execPath, runnerArgs),
    'perf-runner',
  )
  const verifierArgs = [
    'tools/perf/verifier.mjs',
    '--mysql', config.mysql,
    '--database', config.database,
    '--mode', mode,
    '--perf-run-id', perfRunId,
    '--sent-success', String(runner.success),
    '--matched-canvas-count', String(config.matchedCanvasCount),
  ]
  const verifier = parseJsonCommandResult(
    runCommand(process.execPath, verifierArgs),
    'verifier',
  )
  writeJson(path.join(directory, 'verifier.json'), verifier)
  return { summary: runner, verifier, directory, perfRunId }
}
```

- [ ] **Step 4: Implement smoke**

Replace the `smoke` default handler with:

```js
async function defaultSmoke(config, deps = {}) {
  if (!config.perfRunId) {
    throw new Error('--perf-run-id is required for smoke')
  }
  if (!config.canvasId) {
    throw new Error('--canvas-id is required for direct smoke')
  }
  const runScenario = deps.runScenario || ((options) => defaultRunScenario(config, options))
  const modes = [
    { mode: 'direct', count: 50, concurrency: 5 },
    { mode: 'event', count: 100, concurrency: 10 },
  ]
  const runs = []
  for (const scenario of modes) {
    const run = await runScenario(scenario)
    runs.push({ mode: scenario.mode, verifier: run.verifier, summary: run.summary })
    if (run.verifier.verdict !== 'PASS') {
      return { status: 'FAIL', failedMode: scenario.mode, runs }
    }
  }
  return { status: 'PASS', runs }
}
```

Update `runGuide` handler wiring so injected `runScenario` is used:

```js
    smoke: deps.smoke || ((activeConfig) => defaultSmoke(activeConfig, deps)),
```

- [ ] **Step 5: Implement threshold**

Add:

```js
async function defaultThreshold(config, deps = {}) {
  const run = deps.runCommand || runCommand
  const args = [
    'tools/perf/threshold-runner.mjs',
    '--mode', config.mode,
    '--base-url', config.baseUrl,
    '--matched-canvas-count', String(config.matchedCanvasCount),
    '--event-secret-env', config.eventSecretEnv,
  ]
  if (config.mode === 'event') {
    args.push('--event-code', config.eventCode)
  }
  if (config.mode === 'direct') {
    args.push('--canvas-id', config.canvasId)
  }
  const result = parseJsonCommandResult(run(process.execPath, args), 'threshold-runner')
  return {
    status: result.verdict === 'NO_STABLE_STAGE' ? 'FAIL' : 'PASS',
    ...result,
  }
}
```

Update `runGuide`:

```js
    threshold: deps.threshold || ((activeConfig) => defaultThreshold(activeConfig, deps)),
```

- [ ] **Step 6: Implement soak duration gate**

Add:

```js
async function defaultSoak(config, deps = {}) {
  if (!config.perfRunId) {
    throw new Error('--perf-run-id is required for soak')
  }
  const runScenario = deps.runScenario || ((options) => defaultRunScenario(config, options))
  const run = await runScenario({
    mode: config.mode,
    count: 300000,
    concurrency: 100,
  })
  const requiredMs = config.minDurationMin * 60 * 1000
  if ((run.summary.durationMs || 0) < requiredMs) {
    return {
      status: 'FAIL',
      reason: `duration ${run.summary.durationMs || 0}ms is below required ${requiredMs}ms`,
      summary: run.summary,
      verifier: run.verifier,
    }
  }
  if (run.verifier.verdict !== 'PASS') {
    return {
      status: 'FAIL',
      reason: `verifier verdict ${run.verifier.verdict}`,
      summary: run.summary,
      verifier: run.verifier,
    }
  }
  return { status: 'PASS', summary: run.summary, verifier: run.verifier }
}
```

Update `runGuide`:

```js
    soak: deps.soak || ((activeConfig) => defaultSoak(activeConfig, deps)),
```

- [ ] **Step 7: Run guide tests**

Run:

```bash
node --test tools/perf/perf-guide.test.mjs
```

Expected: PASS.

- [ ] **Step 8: Commit**

```bash
git add tools/perf/perf-guide.mjs tools/perf/perf-guide.test.mjs
git commit -m "feat: add guided performance workflow gates"
```

## Task 6: Rebuild Stress Test Documentation And Remove Misleading Entrypoints

**Files:**
- Modify: `tools/perf/README.md`
- Create or replace: `docs/stressTest/README.md`
- Create: `docs/stressTest/local-capacity-runbook.md`
- Create: `docs/stressTest/performance-audit.md`
- Create: `docs/stressTest/report-template.md`
- Delete: `docs/stressTest/2026-05-27-local-container-capacity-testing-design.md`
- Delete: `docs/stressTest/老板汇报版-并发评估摘要.md`
- Delete: `docs/stressTest/并发量评估报告.md`

- [ ] **Step 1: Replace `docs/stressTest/README.md` with single entry point**

Create or replace `docs/stressTest/README.md` with:

```markdown
# Canvas Stress Testing

This directory has one supported execution path: local capacity testing through `tools/perf/perf-guide.mjs`.

Do not use archived capacity estimates as measured results. A capacity number is valid only when it is backed by:

- `perfRunId`
- runner summary JSON
- verifier JSON with `verdict: "PASS"`
- monitor snapshots
- environment details
- capacity input parameters
- cleanup record

## Quick Start

```bash
node --test tools/perf/*.test.mjs
export PERF_EVENT_SECRET=canvas-event-report-secret-2026!!
node tools/perf/perf-guide.mjs doctor
node tools/perf/perf-guide.mjs fixture --rebuild true
node tools/perf/perf-guide.mjs smoke --perf-run-id "perf_$(date +%Y%m%d_%H%M%S)" --canvas-id "$DIRECT_CANVAS_ID"
```

Continue with the full runbook:

- [Local capacity runbook](./local-capacity-runbook.md)
- [Performance audit](./performance-audit.md)
- [Report template](./report-template.md)

## Hard Rules

- If verifier is not `PASS`, do not use the run for capacity planning.
- `PASS_WITH_EXPECTED_FAILURES` is for fault reports only.
- Do not report QPS without the matching `perfRunId`.
- Cleanup defaults to ledger-only. Full cleanup requires `--scope all --execute true`.
```

- [ ] **Step 2: Create `performance-audit.md`**

Create `docs/stressTest/performance-audit.md`:

```markdown
# Performance Testing Audit

## Verdict

The old local capacity document had the right direction but was not safe enough for non-specialist execution. It mixed long manual command sequences with unstated assumptions about event signing, fixture reuse, monitoring, and report evidence.

## What Was Reasonable

- The backend is tested inside fixed-resource Docker containers.
- Load generation stays on the host so the backend resource limit is meaningful.
- `perfRunId` isolates ledger data.
- `verifier.mjs` checks correctness, not just HTTP success.
- Capacity estimation rejects verifier `FAIL`.

## Blocking Issues

- Event pressure testing omitted `X-Canvas-Timestamp` and `X-Canvas-Signature`; the backend requires HMAC validation.
- The old report files were capacity narratives, not measured reports backed by run evidence.
- Cleanup could remove `PERF_%` event and MQ definitions when users only intended to clean one run.
- Soak testing required 30 minutes but did not force duration evidence.
- Monitoring snapshots were manual and easy to skip.

## New Rule

Only reports generated from `tools/perf/perf-guide.mjs report` and backed by verifier `PASS` are valid capacity evidence.
```

- [ ] **Step 3: Create `local-capacity-runbook.md`**

Create `docs/stressTest/local-capacity-runbook.md`:

```markdown
# Local Capacity Runbook

## 1. Prepare Environment

```bash
cd /Users/photonpay/project/canvas
export JAVA_HOME=$(/usr/libexec/java_home -v 21)
export PATH="$JAVA_HOME/bin:$PATH"
export PERF_EVENT_SECRET=canvas-event-report-secret-2026!!
```

The backend container must use the same event secret:

```bash
-e CANVAS_EVENT_REPORT_SECRET="$PERF_EVENT_SECRET"
```

## 2. Verify Tools

```bash
node --test tools/perf/*.test.mjs
node tools/perf/perf-guide.mjs doctor
```

Do not continue when either command fails.

## 3. Start Dependencies

```bash
docker compose -f docker-compose.local.yml up -d
docker compose -f docker-compose.local.yml ps
```

## 4. Build Backend Image

```bash
cd /Users/photonpay/project/canvas/backend
mvn -q -pl canvas-engine -am clean package -DskipTests
cd /Users/photonpay/project/canvas
docker build -f backend/canvas-engine/Dockerfile.perf -t canvas-engine:perf .
```

## 5. Start Fixed-Resource Backend

```bash
docker rm -f canvas-backend-perf 2>/dev/null || true
docker run -d \
  --name canvas-backend-perf \
  --cpus=2 \
  --memory=2g \
  --memory-swap=2g \
  -p 8080:8080 \
  -e JAVA_OPTS="-XX:+UseZGC -XX:+ZGenerational -XX:MaxRAMPercentage=70 -Djava.security.egd=file:/dev/./urandom" \
  -e SPRING_DATASOURCE_URL="jdbc:mysql://host.docker.internal:3306/canvas_db?useUnicode=true&characterEncoding=utf8&useSSL=false&serverTimezone=Asia/Shanghai&allowPublicKeyRetrieval=true" \
  -e SPRING_DATASOURCE_USERNAME=root \
  -e SPRING_DATASOURCE_PASSWORD=root \
  -e SPRING_DATA_REDIS_HOST=host.docker.internal \
  -e SPRING_DATA_REDIS_PORT=6379 \
  -e ROCKETMQ_NAME_SERVER=host.docker.internal:9876 \
  -e CANVAS_EVENT_REPORT_SECRET="$PERF_EVENT_SECRET" \
  canvas-engine:perf
```

## 6. Fixture

```bash
node tools/perf/perf-guide.mjs fixture --rebuild true
```

Record the printed direct canvas ID:

```bash
export DIRECT_CANVAS_ID=<printed-direct-canvas-id>
```

## 7. Smoke

```bash
export PERF_RUN_ID=perf_$(date +%Y%m%d_%H%M%S)_smoke
node tools/perf/perf-guide.mjs smoke \
  --perf-run-id "$PERF_RUN_ID" \
  --canvas-id "$DIRECT_CANVAS_ID" \
  --event-secret-env PERF_EVENT_SECRET
```

The smoke result must be `PASS`.

## 8. Threshold

```bash
node tools/perf/perf-guide.mjs threshold \
  --mode event \
  --event-code PERF_ORDER_PAID \
  --event-secret-env PERF_EVENT_SECRET \
  --matched-canvas-count 1
```

Stop at the first failed stage.

## 9. Soak

```bash
export PERF_RUN_ID=perf_$(date +%Y%m%d_%H%M%S)_soak
node tools/perf/perf-guide.mjs soak \
  --perf-run-id "$PERF_RUN_ID" \
  --mode event \
  --event-secret-env PERF_EVENT_SECRET \
  --min-duration-min 30
```

The run is invalid if actual duration is below 30 minutes.

## 10. Report

```bash
node tools/perf/perf-guide.mjs report \
  --perf-run-id "$PERF_RUN_ID" \
  --report-type capacity
```

Capacity reports require verifier `PASS`.

## 11. Cleanup

Preview ledger cleanup:

```bash
node tools/perf/perf-guide.mjs cleanup --perf-run-id "$PERF_RUN_ID"
```

Execute ledger cleanup:

```bash
node tools/perf/perf-guide.mjs cleanup --perf-run-id "$PERF_RUN_ID" --execute true
```

Full cleanup is only for the end of all testing:

```bash
node tools/perf/perf-guide.mjs cleanup --perf-run-id "$PERF_RUN_ID" --scope all --execute true
```
```

- [ ] **Step 4: Create `report-template.md`**

Create `docs/stressTest/report-template.md`:

```markdown
# Capacity Report Template

## Test Identity

- `perfRunId`:
- Scenario:
- Resource profile:
- Backend image:
- Backend container CPU/memory:

## Evidence

- Runner summary file:
- Verifier file:
- Monitor directory:
- Cleanup record:

## Correctness

- Verifier verdict:
- Unexpected loss:
- Duplicate execution:
- Retry pending:
- DLQ:

If verifier verdict is not `PASS`, stop here and do not publish capacity numbers.

## Performance

- Requested count:
- Successful sends:
- Failed sends:
- Duration:
- QPS:
- p95:
- p99 source:

## Bottlenecks

- App CPU:
- JVM/GC:
- MySQL:
- Redis:
- RocketMQ:
- Downstream:

## Capacity Inputs

- localStableQps:
- localAppCores:
- prodAppCoresTotal:
- writesPerEvent:
- prodDbSafeWriteQps:
- redisOpsPerEvent:
- prodRedisSafeOps:
- rocketmqCapacity:
- disruptorWorkerCapacity:
- downstreamRateLimitPerSec:
- downstreamCallsPerEvent:
- safetyFactor:

## Conclusion

- Recommended capacity:
- Alert threshold:
- Rate limit threshold:
- Primary bottleneck:
- Evidence gaps:
```

- [ ] **Step 5: Update `tools/perf/README.md`**

At the top of `tools/perf/README.md`, add:

```markdown
## Recommended Entry Point

Use the guided workflow first:

```bash
node tools/perf/perf-guide.mjs doctor
node tools/perf/perf-guide.mjs fixture --rebuild true
node tools/perf/perf-guide.mjs smoke --perf-run-id "$PERF_RUN_ID" --canvas-id "$DIRECT_CANVAS_ID"
```

The lower-level scripts in this directory are kept for debugging and advanced usage. Ordinary capacity reports should be generated from guide-produced run directories.
```

In every event example in `tools/perf/README.md`, add:

```bash
  --event-secret-env PERF_EVENT_SECRET \
```

For cleanup docs, state:

```markdown
Cleanup defaults to `--scope ledger`. Use `--scope all` only after all local capacity testing is complete.
```

- [ ] **Step 6: Delete old stress-test entrypoint files**

Run:

```bash
git rm docs/stressTest/2026-05-27-local-container-capacity-testing-design.md
git rm docs/stressTest/老板汇报版-并发评估摘要.md
git rm docs/stressTest/并发量评估报告.md
```

Expected: all three files staged for deletion.

- [ ] **Step 7: Run documentation sanity checks**

Run:

```bash
rg -n "2026-05-27-local-container-capacity-testing-design|老板汇报版|并发量评估报告" docs tools/perf
```

Expected: no references in active docs or `tools/perf`.

- [ ] **Step 8: Commit**

```bash
git add docs/stressTest tools/perf/README.md
git commit -m "docs: replace stress testing runbook with guided workflow"
```

## Task 7: Full Verification Pass

**Files:**
- Verify only. No planned file edits.

- [ ] **Step 1: Run all perf unit tests**

Run:

```bash
node --test tools/perf/*.test.mjs
```

Expected: PASS for all tests.

- [ ] **Step 2: Run guide doctor helpfully without environment side effects**

Run:

```bash
node tools/perf/perf-guide.mjs doctor
```

Expected: JSON output with `status` of `PASS` or actionable failed checks. It must not create or delete fixture data.

- [ ] **Step 3: Verify cleanup default is dry-run ledger scope**

Run:

```bash
node tools/perf/cleanup.mjs --perf-run-id perf_plan_check
```

Expected: SQL output contains `DELETE FROM canvas_execution WHERE perf_run_id = 'perf_plan_check'` and does not contain `DELETE FROM event_definition WHERE event_code LIKE 'PERF_%'`.

- [ ] **Step 4: Verify all-scope cleanup remains explicit**

Run:

```bash
node tools/perf/cleanup.mjs --perf-run-id perf_plan_check --scope all
```

Expected: SQL output contains `DELETE FROM event_definition WHERE event_code LIKE 'PERF_%'` and `DELETE FROM mq_message_definition WHERE message_code LIKE 'PERF_%'`.

- [ ] **Step 5: Verify event runner exposes HMAC settings without leaking secret**

Run:

```bash
PERF_EVENT_SECRET=12345678901234567890123456789012 \
node tools/perf/perf-runner.mjs \
  --mode event \
  --perf-run-id perf_plan_check \
  --count 0
```

Expected: JSON output includes `"eventSignature": { "enabled": true, "source": "env:PERF_EVENT_SECRET" }` and does not include the literal secret.

- [ ] **Step 6: Check active docs have one stress-test entry point**

Run:

```bash
rg -n "only supported execution path|perf-guide.mjs|verifier is not `PASS`" docs/stressTest tools/perf/README.md
```

Expected: matches in `docs/stressTest/README.md`, `docs/stressTest/local-capacity-runbook.md`, and `tools/perf/README.md`.

- [ ] **Step 7: Commit final verification notes if any docs changed**

If verification required no edits, do not commit. If verification required doc wording fixes, commit only those files:

```bash
git add docs/stressTest tools/perf/README.md
git commit -m "docs: clarify stress testing verification guidance"
```

## Self-Review Checklist

- Spec coverage: event HMAC is covered in Tasks 1 and 2; cleanup safety in Task 3; guide CLI, report gates, smoke/threshold/soak in Tasks 4 and 5; docs restructuring and old-file deletion in Task 6; verification in Task 7.
- Placeholder scan: no task uses unresolved markers, deferred implementation, or unspecified tests.
- Type consistency: `eventSecret`, `eventSecretEnv`, `reportType`, `scope`, `rebuild`, and `execute` names are consistent across runner, threshold, cleanup, guide, and tests.
