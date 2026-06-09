# 3000 Hardening Profile Contract And Evidence Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Make 3000-concurrency hardening profiles machine-readable, checklist-complete, and able to write an evidence manifest for each run.

**Architecture:** Keep the existing Node.js perf profile tool and JSON profile file, then add contract validation for protected lanes, borrow rules, required failure profiles, actions, and evidence manifest output. This slice does not evaluate live metrics or change backend runtime behavior.

**Tech Stack:** Node.js built-in test runner, ES modules, JSON perf profiles, `tools/perf` scripts.

**Implementation Status:** Implemented and focused-verified on 2026-06-05. The hardening profile contract now validates protected lane borrow rules, required 3000 failure-mode profiles, profile-level gates/actions, and can write an evidence manifest for a run.

---

## Spec Reference

- `docs/product-evolution/specs/p1-004-3000-hardening-profile-contract-and-evidence.md`
- Source: `docs/optimization/archive/3000-concurrency-hardening-checklist.md`

## File Structure

**Perf profile contract**
- Modify: `tools/perf/3000-hardening-profiles.json` - stores lane budgets, protected-lane rules, required profile names, and per-profile actions.
- Modify: `tools/perf/hardening-profile.mjs` - validates profile contract, renders threshold commands, and writes evidence manifests.
- Modify: `tools/perf/hardening-profile.test.mjs` - covers profile validation and evidence manifest output.
- Modify: `tools/perf/README.md` - documents required 3000 profile names and evidence command.

### Task 1: Profile Contract Validation

**Files:**
- Modify: `tools/perf/3000-hardening-profiles.json`
- Modify: `tools/perf/hardening-profile.mjs`
- Modify: `tools/perf/hardening-profile.test.mjs`
- Modify: `tools/perf/README.md`

- [x] **Step 1: Add profile schema tests**

Modify `tools/perf/hardening-profile.test.mjs` so the existing `validConfig` includes:

```js
protectedLanes: ['LIGHT', 'STANDARD'],
borrowRules: {
  HEAVY: { cannotBorrowFrom: ['LIGHT', 'STANDARD'] },
  RETRY: { cannotBorrowFrom: ['LIGHT', 'STANDARD'] },
},
requiredProfiles: ['default-mixed-3000'],
```

Add these fields to the existing `default-mixed-3000` profile in `validConfig.profiles`:

```js
stopGates: ['RUNNER_FAILED'],
rollbackActions: ['restore_previous_concurrency'],
degradeActions: ['reduce_retry_lane'],
```

Add these tests:

```js
test('validateHardeningProfiles requires protected lane borrow rules', () => {
  const config = structuredClone(validConfig)
  config.protectedLanes = ['LIGHT', 'STANDARD']
  config.borrowRules = {
    HEAVY: { cannotBorrowFrom: ['LIGHT', 'STANDARD'] },
    RETRY: { cannotBorrowFrom: ['LIGHT', 'STANDARD'] },
  }

  assert.equal(validateHardeningProfiles(config).protectedLanes.length, 2)
})

test('validateHardeningProfiles rejects heavy borrowing from light', () => {
  const config = structuredClone(validConfig)
  config.borrowRules = {
    HEAVY: { cannotBorrowFrom: ['STANDARD'] },
    RETRY: { cannotBorrowFrom: ['LIGHT', 'STANDARD'] },
  }

  assert.throws(
    () => validateHardeningProfiles(config),
    /HEAVY must not borrow protected lane LIGHT/,
  )
})

test('validateHardeningProfiles requires all 3000 failure-mode profiles', () => {
  const config = structuredClone(validConfig)
  config.requiredProfiles = [
    'default-mixed-3000',
    'retry-surge-3000',
    'heavy-surge-3000',
    'redis-latency-spike-3000',
    'mysql-saturation-3000',
    'rocketmq-backlog-3000',
    'downstream-partial-failure-3000',
    'retry-backlog-explosion-3000',
  ]
  config.profiles = config.requiredProfiles.map((name) => ({
    name,
    description: name,
    mode: 'event',
    eventCode: 'PERF_ORDER_PAID',
    stages: [{ count: 1000, concurrency: 100 }],
    maxFailed: 0,
    maxP95Ms: 1000,
    waitAfterRunMs: 1000,
    stopGates: ['RUNNER_FAILED'],
    rollbackActions: ['restore_previous_concurrency'],
    degradeActions: ['reduce_retry_lane'],
  }))

  assert.equal(validateHardeningProfiles(config).profiles.length, 8)
})
```

- [x] **Step 2: Run profile tests and confirm red state**

Run:

```bash
node --test tools/perf/hardening-profile.test.mjs
```

Expected: FAIL because `validateHardeningProfiles` does not validate `protectedLanes`, `borrowRules`, `requiredProfiles`, profile-level `stopGates`, `rollbackActions`, or `degradeActions`.

Observed: FAIL on 2026-06-05 because `validateHardeningProfiles` did not reject HEAVY borrowing from protected LIGHT.

- [x] **Step 3: Update profile validator**

Modify `tools/perf/hardening-profile.mjs`:

```js
function requireStringArray(label, value) {
  if (!Array.isArray(value) || value.length === 0 || value.some((item) => typeof item !== 'string' || item.trim() === '')) {
    throw new Error(`${label} must be a non-empty string array`)
  }
  return value
}

function validateProtectedLaneRules(config) {
  const protectedLanes = requireStringArray('protectedLanes', config.protectedLanes)
  for (const lane of ['HEAVY', 'RETRY']) {
    const blocked = config.borrowRules?.[lane]?.cannotBorrowFrom
    requireStringArray(`${lane}.cannotBorrowFrom`, blocked)
    for (const protectedLane of protectedLanes) {
      if (!blocked.includes(protectedLane)) {
        throw new Error(`${lane} must not borrow protected lane ${protectedLane}`)
      }
    }
  }
}

function validateRequiredProfiles(config) {
  const requiredProfiles = requireStringArray('requiredProfiles', config.requiredProfiles)
  const names = new Set(config.profiles.map((profile) => profile.name))
  for (const name of requiredProfiles) {
    if (!names.has(name)) {
      throw new Error(`missing required profile ${name}`)
    }
  }
}
```

Call `validateProtectedLaneRules(config)` inside `validateHardeningProfiles(config)` after lane total validation. Inside the profile loop, add:

```js
requireStringArray(`${profile.name}.stopGates`, profile.stopGates)
requireStringArray(`${profile.name}.rollbackActions`, profile.rollbackActions)
requireStringArray(`${profile.name}.degradeActions`, profile.degradeActions)
```

After the profile loop, add:

```js
validateRequiredProfiles(config)
```

- [x] **Step 4: Align `3000-hardening-profiles.json` with checklist names**

Modify `tools/perf/3000-hardening-profiles.json` so it includes this top-level contract:

```json
{
  "targetConcurrency": 3000,
  "observationWindowSeconds": 1800,
  "protectedLanes": ["LIGHT", "STANDARD"],
  "borrowRules": {
    "HEAVY": { "cannotBorrowFrom": ["LIGHT", "STANDARD"] },
    "RETRY": { "cannotBorrowFrom": ["LIGHT", "STANDARD"] }
  },
  "requiredProfiles": [
    "default-mixed-3000",
    "retry-surge-3000",
    "heavy-surge-3000",
    "redis-latency-spike-3000",
    "mysql-saturation-3000",
    "rocketmq-backlog-3000",
    "downstream-partial-failure-3000",
    "retry-backlog-explosion-3000"
  ],
  "lanes": {
    "LIGHT": { "concurrency": 600, "share": 0.2 },
    "STANDARD": { "concurrency": 1800, "share": 0.6 },
    "HEAVY": { "concurrency": 300, "share": 0.1 },
    "RETRY": { "concurrency": 300, "share": 0.1 }
  }
}
```

Replace the `profiles` array with profiles using exactly these names and nonempty `stopGates`, `rollbackActions`, and `degradeActions`. Use this pattern for every profile:

```json
{
  "name": "retry-surge-3000",
  "description": "Retry lane reaches 300 while LIGHT and STANDARD stay protected.",
  "mode": "event",
  "eventCode": "PERF_RETRY_SURGE",
  "stages": [
    { "count": 5000, "concurrency": 100 },
    { "count": 15000, "concurrency": 300 }
  ],
  "maxFailed": 0,
  "maxP95Ms": 1500,
  "waitAfterRunMs": 30000,
  "stopGates": ["RETRY_BACKLOG_GROWING_AFTER_RECOVERY", "DLQ_GROWING_AFTER_RECOVERY"],
  "rollbackActions": ["restore_previous_lane_budgets", "pause_retry_replay"],
  "degradeActions": ["lower_retry_lane", "lengthen_retry_backoff"]
}
```

- [x] **Step 5: Update perf README profile list**

Modify `tools/perf/README.md` under `3000 Hardening Profiles` so the required profile list is exactly:

```markdown
- `default-mixed-3000`
- `retry-surge-3000`
- `heavy-surge-3000`
- `redis-latency-spike-3000`
- `mysql-saturation-3000`
- `rocketmq-backlog-3000`
- `downstream-partial-failure-3000`
- `retry-backlog-explosion-3000`
```

- [x] **Step 6: Run profile tests**

Run:

```bash
node --test tools/perf/hardening-profile.test.mjs
```

Expected: PASS with lane total, borrow rules, required profiles, action fields, and command rendering validated.

Observed: PASS on 2026-06-05.

### Task 2: Evidence Manifest Output

**Files:**
- Modify: `tools/perf/hardening-profile.mjs`
- Modify: `tools/perf/hardening-profile.test.mjs`

- [x] **Step 1: Add evidence manifest tests**

Modify `tools/perf/hardening-profile.test.mjs` imports:

```js
import {
  buildEvidenceManifest,
  renderThresholdCommand,
  selectProfile,
  validateHardeningProfiles,
} from './hardening-profile.mjs'
```

Add this test:

```js
test('buildEvidenceManifest includes run id, command, lane budget, gates, and sample files', () => {
  const profile = selectProfile(validateHardeningProfiles(validConfig), 'default-mixed-3000')
  const manifest = buildEvidenceManifest(validConfig, profile, {
    baseUrl: 'http://localhost:8080',
    outDir: 'tmp/perf-3000-hardening',
    runIdPrefix: 'perf_3000_gate',
    now: '2026-06-03T10:00:00.000Z',
  })

  assert.equal(manifest.targetConcurrency, 3000)
  assert.equal(manifest.profileName, 'default-mixed-3000')
  assert.equal(manifest.lanes.STANDARD.concurrency, 1800)
  assert.deepEqual(manifest.protectedLanes, ['LIGHT', 'STANDARD'])
  assert.match(manifest.command, /threshold-runner\.mjs/)
  assert.deepEqual(manifest.metricSampleFiles, [
    'redis-latency.json',
    'mysql-pool.json',
    'rocketmq-backlog.json',
    'retry-backlog.json',
    'dlq-count.json',
    'trace-buffer.json',
    'downstream-latency.json',
  ])
})
```

- [x] **Step 2: Run evidence test and confirm red state**

Run:

```bash
node --test tools/perf/hardening-profile.test.mjs
```

Expected: FAIL because `buildEvidenceManifest` is not exported.

Observed: FAIL on 2026-06-05 because `hardening-profile.mjs` did not export `buildEvidenceManifest`.

- [x] **Step 3: Add evidence manifest builder**

Modify `tools/perf/hardening-profile.mjs`:

```js
export function buildEvidenceManifest(config, profile, options = {}) {
  const now = options.now || new Date().toISOString()
  const runIdPrefix = options.runIdPrefix || `perf_${profile.name}`
  return {
    schemaVersion: 1,
    generatedAt: now,
    runIdPrefix,
    profileName: profile.name,
    targetConcurrency: config.targetConcurrency,
    observationWindowSeconds: config.observationWindowSeconds,
    lanes: config.lanes,
    protectedLanes: config.protectedLanes,
    borrowRules: config.borrowRules,
    stopGates: profile.stopGates,
    rollbackActions: profile.rollbackActions,
    degradeActions: profile.degradeActions,
    command: renderThresholdCommand(profile, options),
    metricSampleFiles: [
      'redis-latency.json',
      'mysql-pool.json',
      'rocketmq-backlog.json',
      'retry-backlog.json',
      'dlq-count.json',
      'trace-buffer.json',
      'downstream-latency.json',
    ],
  }
}
```

- [x] **Step 4: Add CLI evidence write option**

Import filesystem helpers:

```js
import { mkdirSync, readFileSync, writeFileSync } from 'node:fs'
```

Extend `parseCliArgs` defaults:

```js
writeEvidence: false,
```

In the CLI parser, accept:

```js
else if (flag === '--write-evidence') args.writeEvidence = value === 'true'
```

Change the CLI block:

```js
const command = renderThresholdCommand(profile, args)
if (args.writeEvidence) {
  const manifest = buildEvidenceManifest(config, profile, args)
  const runDir = path.join(args.outDir, args.runIdPrefix || `perf_${profile.name}`)
  mkdirSync(runDir, { recursive: true })
  writeFileSync(path.join(runDir, 'evidence-manifest.json'), JSON.stringify(manifest, null, 2))
}
console.log(command)
```

- [x] **Step 5: Run evidence tests**

Run:

```bash
node --test tools/perf/hardening-profile.test.mjs
```

Expected: PASS with evidence manifest construction covered.

Observed: PASS on 2026-06-05.

### Task 3: Verification And Commit

**Files:**
- Modify: `tools/perf/3000-hardening-profiles.json`
- Modify: `tools/perf/hardening-profile.mjs`
- Modify: `tools/perf/hardening-profile.test.mjs`
- Modify: `tools/perf/README.md`
- Modify: `docs/product-evolution/specs/p1-004-3000-hardening-profile-contract-and-evidence.md`
- Modify: `docs/product-evolution/plans/p1-004-3000-hardening-profile-contract-and-evidence-plan.md`

- [x] **Step 1: Run focused Node tests**

Run:

```bash
node --test tools/perf/hardening-profile.test.mjs
```

Expected: PASS.

Observed: PASS on 2026-06-05, 9 tests passed.

- [x] **Step 2: Validate profile total**

Run:

```bash
node -e "const p=require('./tools/perf/3000-hardening-profiles.json'); const total=Object.values(p.lanes).reduce((sum,l)=>sum+l.concurrency,0); if (total !== p.targetConcurrency) throw new Error(String(total)); console.log(total)"
```

Expected: prints `3000`.

Observed: printed `3000`.

- [x] **Step 3: Render evidence manifest**

Run:

```bash
node tools/perf/hardening-profile.mjs \
  --profile-file tools/perf/3000-hardening-profiles.json \
  --profile default-mixed-3000 \
  --out-dir tmp/perf-3000-hardening \
  --run-id-prefix perf_3000_hardening_doc_check \
  --write-evidence true
```

Expected: prints a `threshold-runner.mjs` command and writes `tmp/perf-3000-hardening/perf_3000_hardening_doc_check/evidence-manifest.json`.

Observed: command printed and `tmp/perf-3000-hardening/perf_3000_hardening_doc_check/evidence-manifest.json` was written with profile `default-mixed-3000`, target concurrency `3000`, two protected lanes, and seven metric sample files.

- [x] **Step 4: Document commit boundary**
Boundary: No git commit or merge was created in this docs-only audit; the command below remains the future scoped staging recipe.

Run:

```bash
git add \
  tools/perf/3000-hardening-profiles.json \
  tools/perf/hardening-profile.mjs \
  tools/perf/hardening-profile.test.mjs \
  tools/perf/README.md \
  docs/product-evolution/specs/p1-004-3000-hardening-profile-contract-and-evidence.md \
  docs/product-evolution/plans/p1-004-3000-hardening-profile-contract-and-evidence-plan.md
git commit -m "test: validate 3000 hardening profiles"
```

Expected: commit contains only profile contract, evidence manifest, docs, and tests for this slice.
