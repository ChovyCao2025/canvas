# 3000 Concurrency Hardening Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Finish the 3000 production-readiness hardening layer by turning the approved hardening spec into repeatable backend test gates, mixed-load profiles, stop gates, and rollback/degrade runbooks.

**Architecture:** Keep the 3000 code foundation plan as the runtime implementation source for lane budgets and Redis admission. This plan adds the operational and verification layer around that code: affected backend tests must pass first, load profiles become machine-readable, performance commands are deterministic, and every promotion has explicit pass, stop, rollback, and degrade actions.

**Tech Stack:** Java 21, Maven, Spring Boot WebFlux, Redis, RocketMQ, MySQL, Node.js 18+, existing `tools/perf` runners, Markdown operational runbooks, JUnit 5, Node test runner.

---

## Scope

This plan implements the remaining hardening required by `docs/superpowers/specs/2026-05-27-3000-concurrency-hardening-design.md`.

It covers:
- affected backend test baseline
- 3000 mixed traffic profile
- retry, heavy, slow downstream, Redis latency, and RocketMQ backlog stress profiles
- lane budget tuning guardrails
- conservative failure behavior expectations
- stop, rollback, and degrade actions
- machine-readable profile data for repeatable local and staging runs
- README wiring so engineers run the same commands

It does not replace `docs/superpowers/plans/2026-05-27-3000-concurrency-code-foundation.md`. If `ExecutionLane`, lane-aware Redis admission, and lane metrics are not present in the branch, execute the code foundation plan before promoting any 3000 load result.

## File Structure

### Existing files to modify

- `tools/perf/README.md`
  - Add the 3000 hardening command sequence and explain how profile files map to staged threshold runs.

### New files to create

- `docs/optimization/3000-concurrency-hardening-checklist.md`
  - Human-readable production gate checklist with mixed profile, stress profiles, lane tuning rules, stop gates, rollback actions, and degrade actions.
- `tools/perf/3000-hardening-profiles.json`
  - Machine-readable definition of the default mixed profile, focused stress profiles, stop gates, and recommended runner stages.
- `tools/perf/hardening-profile.mjs`
  - Small CLI/library that validates `3000-hardening-profiles.json` and prints concrete runner commands for one profile.
- `tools/perf/hardening-profile.test.mjs`
  - Node tests for profile validation, lane total validation, and command rendering.

### Verification commands

- Backend impacted tests:

```bash
JAVA_HOME=$(/usr/libexec/java_home -v 21) mvn -f backend/pom.xml -pl canvas-engine -Dtest=CanvasExecutionServiceTest,CanvasExecutionServiceTriggerNodeTest,InFlightExecutionRegistryTest,CanvasServicePublishTest,CanvasServiceExampleFilterTest,CanvasOpsServiceExampleCloneTest test
```

- Perf profile unit tests:

```bash
node --test tools/perf/hardening-profile.test.mjs
```

- Markdown/profile consistency scan:

```bash
rg -n "3000|LIGHT|STANDARD|HEAVY|RETRY|REGISTRY_UNAVAILABLE|rollback|degrade|stop gate" docs/optimization/3000-concurrency-hardening-checklist.md tools/perf/3000-hardening-profiles.json tools/perf/README.md
```

---

## Task 1: Preserve the affected backend test baseline

**Files:**
- Create: `docs/optimization/3000-concurrency-hardening-checklist.md`

- [ ] **Step 1: Run the impacted backend tests before editing production code**

Run:

```bash
JAVA_HOME=$(/usr/libexec/java_home -v 21) mvn -f backend/pom.xml -pl canvas-engine -Dtest=CanvasExecutionServiceTest,CanvasExecutionServiceTriggerNodeTest,InFlightExecutionRegistryTest,CanvasServicePublishTest,CanvasServiceExampleFilterTest,CanvasOpsServiceExampleCloneTest test
```

Expected:

```text
Tests run: 22, Failures: 0, Errors: 0, Skipped: 0
BUILD SUCCESS
```

- [ ] **Step 2: Create the hardening checklist with the baseline gate**

Create `docs/optimization/3000-concurrency-hardening-checklist.md` with this content:

```md
# 3000 Concurrency Hardening Checklist

## Purpose

This checklist is the production-readiness gate for the 3000 Canvas execution concurrency target. 3000 means cluster-level active Canvas executions, not HTTP connections, MQ backlog, DAU, or single-instance concurrency.

## Backend Test Baseline

Run before any 3000 hardening code change:

```bash
JAVA_HOME=$(/usr/libexec/java_home -v 21) mvn -f backend/pom.xml -pl canvas-engine -Dtest=CanvasExecutionServiceTest,CanvasExecutionServiceTriggerNodeTest,InFlightExecutionRegistryTest,CanvasServicePublishTest,CanvasServiceExampleFilterTest,CanvasOpsServiceExampleCloneTest test
```

Pass condition:

```text
Tests run: 22, Failures: 0, Errors: 0, Skipped: 0
BUILD SUCCESS
```

Stop condition:

- Test compilation fails.
- Any listed test class fails.
- A failure is marked unrelated without a file, test name, and reproducible command.

## 3000 Completion Gate

3000 is complete only when:

- affected backend tests pass on Java 21
- the default mixed profile passes the full observation window
- retry surge passes after downstream recovery
- heavy surge does not degrade LIGHT or STANDARD
- slow downstream is contained by timeout, circuit breaker, and bulkhead behavior
- Redis registry latency or outage fails conservatively
- RocketMQ backlog recovery does not let RETRY starve normal traffic
- retry backlog, DLQ growth, Disruptor overflow, and MQ backlog have stop gates
- rollback and degrade actions have been exercised
- 4000 remains blocked until this checklist passes
```

- [ ] **Step 3: Commit the baseline checklist**

```bash
git add docs/optimization/3000-concurrency-hardening-checklist.md
git commit -m "docs: add 3000 hardening baseline gate"
```

---

## Task 2: Add the 3000 mixed profile and lane tuning rules

**Files:**
- Modify: `docs/optimization/3000-concurrency-hardening-checklist.md`

- [ ] **Step 1: Append the mixed traffic model**

Append this section to `docs/optimization/3000-concurrency-hardening-checklist.md`:

```md
## Default Mixed Traffic Profile

The default 3000 profile is:

| Lane | Concurrency | Share | Purpose |
| --- | ---: | ---: | --- |
| LIGHT | 600 | 20% | direct calls, short DAGs, low downstream fanout |
| STANDARD | 1800 | 60% | normal event, MQ, behavior, and API-triggered flows |
| HEAVY | 300 | 10% | audience batch, high fanout DAGs, expensive script or large payload flows |
| RETRY | 300 | 10% | overflow and request retry recovery |
| Global | 3000 | 100% | total cluster active execution budget |

Pass conditions:

- `LIGHT` and `STANDARD` runner p95 stays at or below 1000 ms in `default-mixed-3000`.
- `HEAVY` active execution count never borrows protected `LIGHT` or `STANDARD` budget.
- `RETRY` active execution count never borrows protected `LIGHT` or `STANDARD` budget.
- No unbounded retry backlog, DLQ growth, Disruptor overflow, or MQ backlog growth appears during the observation window.

Stop conditions:

- lane total exceeds global budget
- Redis p95 stays above 20 ms or p99 stays above 50 ms through one observation window
- MySQL active connections stay at or above 85% of pool max, or slow SQL above 1000 ms appears in two consecutive samples
- normal MQ backlog grows while RETRY is draining
- Disruptor overflow grows for two consecutive samples
- DLQ grows after downstream recovery
```

- [ ] **Step 2: Append the lane tuning rules**

Append this section:

```md
## Lane Budget Tuning Rules

Default budget:

- `LIGHT`: 600
- `STANDARD`: 1800
- `HEAVY`: 300
- `RETRY`: 300
- `global`: 3000

Guardrails:

- `LIGHT` and `STANDARD` are protected lanes.
- `HEAVY` cannot borrow from `LIGHT` or `STANDARD`.
- `RETRY` cannot borrow from `LIGHT` or `STANDARD`.
- Increase `RETRY` only when downstream health is good and retry backlog is shrinking.
- If `LIGHT` or `STANDARD` latency degrades, reduce `HEAVY` and `RETRY` first.
- If Redis or MySQL latency degrades, do not increase any lane budget.
- If a downstream timeout rises, reduce the lane that calls that dependency instead of raising global concurrency.

Incident tuning order:

1. Reduce `RETRY` budget or lengthen retry backoff.
2. Reduce `HEAVY` budget or pause heavy jobs.
3. Disable low-priority scheduled and replay traffic.
4. Preserve `LIGHT` and `STANDARD` if their dependencies remain healthy.
5. Scale application instances only after Redis, MySQL, RocketMQ, and downstream capacity are confirmed.
```

- [ ] **Step 3: Verify the lane total is explicit**

Run:

```bash
rg -n "LIGHT.*600|STANDARD.*1800|HEAVY.*300|RETRY.*300|global.*3000|lane total exceeds global" docs/optimization/3000-concurrency-hardening-checklist.md
```

Expected: at least six matching lines.

- [ ] **Step 4: Commit the profile and tuning rules**

```bash
git add docs/optimization/3000-concurrency-hardening-checklist.md
git commit -m "docs: add 3000 mixed profile tuning rules"
```

---

## Task 3: Add machine-readable hardening profiles

**Files:**
- Create: `tools/perf/3000-hardening-profiles.json`

- [ ] **Step 1: Create the profile file**

Create `tools/perf/3000-hardening-profiles.json`:

```json
{
  "targetConcurrency": 3000,
  "observationWindowSeconds": 1800,
  "lanes": {
    "LIGHT": { "concurrency": 600, "share": 0.2 },
    "STANDARD": { "concurrency": 1800, "share": 0.6 },
    "HEAVY": { "concurrency": 300, "share": 0.1 },
    "RETRY": { "concurrency": 300, "share": 0.1 }
  },
  "stopGates": [
    "RUNNER_FAILED",
    "VERIFIER_FAIL",
    "P95_EXCEEDED",
    "REDIS_REGISTRY_LATENCY_SUSTAINED",
    "REGISTRY_UNAVAILABLE_OVER_ADMIT",
    "MYSQL_POOL_SATURATION",
    "NORMAL_MQ_BACKLOG_STARVED_BY_RETRY",
    "DISRUPTOR_OVERFLOW_GROWING",
    "RETRY_BACKLOG_GROWING_AFTER_RECOVERY",
    "DLQ_GROWING_AFTER_RECOVERY"
  ],
  "profiles": [
    {
      "name": "default-mixed-3000",
      "description": "Default 3000 mixed profile: LIGHT 600, STANDARD 1800, HEAVY 300, RETRY 300.",
      "mode": "event",
      "eventCode": "PERF_ORDER_PAID",
      "stages": [
        { "count": 10000, "concurrency": 600 },
        { "count": 30000, "concurrency": 1800 },
        { "count": 50000, "concurrency": 3000 }
      ],
      "maxFailed": 0,
      "maxP95Ms": 1000,
      "waitAfterRunMs": 10000
    },
    {
      "name": "retry-surge-300",
      "description": "Retry lane reaches 300 while LIGHT and STANDARD stay protected.",
      "mode": "event",
      "eventCode": "PERF_ORDER_PAID",
      "stages": [
        { "count": 5000, "concurrency": 100 },
        { "count": 15000, "concurrency": 300 }
      ],
      "maxFailed": 0,
      "maxP95Ms": 1500,
      "waitAfterRunMs": 30000
    },
    {
      "name": "heavy-surge-300",
      "description": "Heavy lane reaches 300 and must not borrow LIGHT or STANDARD budget.",
      "mode": "event",
      "eventCode": "PERF_HEAVY_FLOW",
      "stages": [
        { "count": 3000, "concurrency": 100 },
        { "count": 10000, "concurrency": 300 }
      ],
      "maxFailed": 0,
      "maxP95Ms": 3000,
      "waitAfterRunMs": 30000
    },
    {
      "name": "slow-downstream-standard",
      "description": "One downstream is slow; timeout, circuit breaker, and bulkhead must contain impact.",
      "mode": "event",
      "eventCode": "PERF_SLOW_DOWNSTREAM",
      "stages": [
        { "count": 5000, "concurrency": 300 },
        { "count": 20000, "concurrency": 1200 }
      ],
      "maxFailed": 100,
      "maxP95Ms": 5000,
      "waitAfterRunMs": 30000
    },
    {
      "name": "redis-registry-latency",
      "description": "Redis registry latency spike must fail conservatively and never over-admit.",
      "mode": "event",
      "eventCode": "PERF_ORDER_PAID",
      "stages": [
        { "count": 5000, "concurrency": 300 },
        { "count": 15000, "concurrency": 1000 }
      ],
      "maxFailed": 500,
      "maxP95Ms": 5000,
      "waitAfterRunMs": 30000
    },
    {
      "name": "rocketmq-backlog-recovery",
      "description": "Normal, retry, and heavy backlog are observed separately; retry recovery must not starve normal traffic.",
      "mode": "event",
      "eventCode": "PERF_MQ",
      "stages": [
        { "count": 10000, "concurrency": 500 },
        { "count": 30000, "concurrency": 1500 }
      ],
      "maxFailed": 0,
      "maxP95Ms": 2000,
      "waitAfterRunMs": 30000
    }
  ]
}
```

- [ ] **Step 2: Verify profile JSON parses**

Run:

```bash
node -e "const p=require('./tools/perf/3000-hardening-profiles.json'); const total=Object.values(p.lanes).reduce((sum,l)=>sum+l.concurrency,0); if (total !== p.targetConcurrency) throw new Error(String(total)); console.log(total)"
```

Expected:

```text
3000
```

- [ ] **Step 3: Commit the profile file**

```bash
git add tools/perf/3000-hardening-profiles.json
git commit -m "test: add 3000 hardening perf profiles"
```

---

## Task 4: Add a hardening profile CLI

**Files:**
- Create: `tools/perf/hardening-profile.mjs`
- Test: `tools/perf/hardening-profile.test.mjs`

- [ ] **Step 1: Write the failing tests**

Create `tools/perf/hardening-profile.test.mjs`:

```js
import test from 'node:test'
import assert from 'node:assert/strict'

import {
  renderThresholdCommand,
  selectProfile,
  validateHardeningProfiles,
} from './hardening-profile.mjs'

const validConfig = {
  targetConcurrency: 3000,
  observationWindowSeconds: 1800,
  lanes: {
    LIGHT: { concurrency: 600, share: 0.2 },
    STANDARD: { concurrency: 1800, share: 0.6 },
    HEAVY: { concurrency: 300, share: 0.1 },
    RETRY: { concurrency: 300, share: 0.1 },
  },
  stopGates: ['RUNNER_FAILED', 'VERIFIER_FAIL'],
  profiles: [
    {
      name: 'default-mixed-3000',
      description: 'default',
      mode: 'event',
      eventCode: 'PERF_ORDER_PAID',
      stages: [
        { count: 10000, concurrency: 600 },
        { count: 50000, concurrency: 3000 },
      ],
      maxFailed: 0,
      maxP95Ms: 1000,
      waitAfterRunMs: 10000,
    },
  ],
}

test('validateHardeningProfiles accepts a 3000 lane total', () => {
  assert.equal(validateHardeningProfiles(validConfig).targetConcurrency, 3000)
})

test('validateHardeningProfiles rejects lane totals above target', () => {
  const invalid = structuredClone(validConfig)
  invalid.lanes.RETRY.concurrency = 301

  assert.throws(
    () => validateHardeningProfiles(invalid),
    /lane total 3001 must equal targetConcurrency 3000/,
  )
})

test('selectProfile finds a configured profile by name', () => {
  const profile = selectProfile(validConfig, 'default-mixed-3000')

  assert.equal(profile.name, 'default-mixed-3000')
})

test('renderThresholdCommand prints a deterministic threshold-runner command', () => {
  const profile = selectProfile(validConfig, 'default-mixed-3000')
  const command = renderThresholdCommand(profile, {
    baseUrl: 'http://localhost:8080',
    outDir: 'tmp/perf-3000-hardening',
    runIdPrefix: 'perf_3000_gate',
  })

  assert.match(command, /node tools\/perf\/threshold-runner\.mjs/)
  assert.match(command, /--mode event/)
  assert.match(command, /--event-code PERF_ORDER_PAID/)
  assert.match(command, /--stages 10000:600,50000:3000/)
  assert.match(command, /--max-failed 0/)
  assert.match(command, /--max-p95-ms 1000/)
})
```

- [ ] **Step 2: Run tests and verify they fail**

Run:

```bash
node --test tools/perf/hardening-profile.test.mjs
```

Expected: FAIL with module-not-found for `hardening-profile.mjs`.

- [ ] **Step 3: Add the CLI/library**

Create `tools/perf/hardening-profile.mjs`:

```js
#!/usr/bin/env node

import { readFileSync } from 'node:fs'
import path from 'node:path'
import { fileURLToPath } from 'node:url'

const SCRIPT_DIR = path.dirname(fileURLToPath(import.meta.url))
const DEFAULT_PROFILE_FILE = path.join(SCRIPT_DIR, '3000-hardening-profiles.json')

function positiveInteger(label, value) {
  if (!Number.isInteger(value) || value <= 0) {
    throw new Error(`${label} must be a positive integer`)
  }
  return value
}

function nonEmptyString(label, value) {
  if (typeof value !== 'string' || value.trim() === '') {
    throw new Error(`${label} must be a non-empty string`)
  }
  return value
}

export function validateHardeningProfiles(config) {
  positiveInteger('targetConcurrency', config.targetConcurrency)
  positiveInteger('observationWindowSeconds', config.observationWindowSeconds)

  const requiredLanes = ['LIGHT', 'STANDARD', 'HEAVY', 'RETRY']
  const laneTotal = requiredLanes.reduce((sum, lane) => {
    const laneConfig = config.lanes?.[lane]
    if (!laneConfig) {
      throw new Error(`missing lane ${lane}`)
    }
    return sum + positiveInteger(`${lane}.concurrency`, laneConfig.concurrency)
  }, 0)

  if (laneTotal !== config.targetConcurrency) {
    throw new Error(`lane total ${laneTotal} must equal targetConcurrency ${config.targetConcurrency}`)
  }

  if (!Array.isArray(config.stopGates) || config.stopGates.length === 0) {
    throw new Error('stopGates must contain at least one gate')
  }

  if (!Array.isArray(config.profiles) || config.profiles.length === 0) {
    throw new Error('profiles must contain at least one profile')
  }

  for (const profile of config.profiles) {
    nonEmptyString('profile.name', profile.name)
    nonEmptyString(`${profile.name}.description`, profile.description)
    if (!['event', 'direct'].includes(profile.mode)) {
      throw new Error(`${profile.name}.mode must be event or direct`)
    }
    if (profile.mode === 'event') {
      nonEmptyString(`${profile.name}.eventCode`, profile.eventCode)
    }
    if (profile.mode === 'direct') {
      nonEmptyString(`${profile.name}.canvasId`, profile.canvasId)
    }
    if (!Array.isArray(profile.stages) || profile.stages.length === 0) {
      throw new Error(`${profile.name}.stages must contain at least one stage`)
    }
    for (const [index, stage] of profile.stages.entries()) {
      positiveInteger(`${profile.name}.stages[${index}].count`, stage.count)
      positiveInteger(`${profile.name}.stages[${index}].concurrency`, stage.concurrency)
    }
    if (!Number.isInteger(profile.maxFailed) || profile.maxFailed < 0) {
      throw new Error(`${profile.name}.maxFailed must be a non-negative integer`)
    }
    if (!Number.isInteger(profile.maxP95Ms) || profile.maxP95Ms < 0) {
      throw new Error(`${profile.name}.maxP95Ms must be a non-negative integer`)
    }
    if (!Number.isInteger(profile.waitAfterRunMs) || profile.waitAfterRunMs < 0) {
      throw new Error(`${profile.name}.waitAfterRunMs must be a non-negative integer`)
    }
  }

  return config
}

export function selectProfile(config, profileName) {
  const profile = config.profiles.find((candidate) => candidate.name === profileName)
  if (!profile) {
    throw new Error(`unknown profile ${profileName}`)
  }
  return profile
}

export function renderThresholdCommand(profile, options = {}) {
  const baseUrl = options.baseUrl || 'http://localhost:8080'
  const outDir = options.outDir || 'tmp/perf-3000-hardening'
  const runIdPrefix = options.runIdPrefix || `perf_${profile.name}`
  const stages = profile.stages
    .map((stage) => `${stage.count}:${stage.concurrency}`)
    .join(',')

  const args = [
    'node tools/perf/threshold-runner.mjs',
    `--mode ${profile.mode}`,
    `--base-url ${baseUrl}`,
    `--stages ${stages}`,
    '--matched-canvas-count 1',
    `--max-failed ${profile.maxFailed}`,
    `--max-p95-ms ${profile.maxP95Ms}`,
    `--wait-after-run-ms ${profile.waitAfterRunMs}`,
    `--out-dir ${outDir}`,
    `--run-id-prefix ${runIdPrefix}`,
  ]

  if (profile.mode === 'event') {
    args.splice(3, 0, `--event-code ${profile.eventCode}`)
  }

  if (profile.mode === 'direct') {
    args.splice(3, 0, `--canvas-id ${profile.canvasId}`)
  }

  return args.join(' \\\n+  ')
}

export function loadProfileFile(filePath = DEFAULT_PROFILE_FILE) {
  return validateHardeningProfiles(JSON.parse(readFileSync(filePath, 'utf8')))
}

function parseCliArgs(argv) {
  const args = {
    profile: 'default-mixed-3000',
    profileFile: DEFAULT_PROFILE_FILE,
    baseUrl: 'http://localhost:8080',
    outDir: 'tmp/perf-3000-hardening',
    runIdPrefix: '',
  }

  for (let index = 0; index < argv.length; index += 2) {
    const flag = argv[index]
    const value = argv[index + 1]
    if (!value || value.startsWith('--')) {
      throw new Error(`missing value for ${flag}`)
    }
    if (flag === '--profile') args.profile = value
    else if (flag === '--profile-file') args.profileFile = value
    else if (flag === '--base-url') args.baseUrl = value
    else if (flag === '--out-dir') args.outDir = value
    else if (flag === '--run-id-prefix') args.runIdPrefix = value
    else throw new Error(`unknown flag ${flag}`)
  }

  return args
}

if (import.meta.url === `file://${process.argv[1]}`) {
  const args = parseCliArgs(process.argv.slice(2))
  const config = loadProfileFile(args.profileFile)
  const profile = selectProfile(config, args.profile)
  console.log(renderThresholdCommand(profile, args))
}
```

- [ ] **Step 4: Run tests and verify they pass**

Run:

```bash
node --test tools/perf/hardening-profile.test.mjs
```

Expected:

```text
# pass 4
# fail 0
```

- [ ] **Step 5: Verify the default profile command renders**

Run:

```bash
node tools/perf/hardening-profile.mjs --profile default-mixed-3000 --run-id-prefix perf_3000_gate
```

Expected output contains:

```text
node tools/perf/threshold-runner.mjs
--stages 10000:600,30000:1800,50000:3000
--run-id-prefix perf_3000_gate
```

- [ ] **Step 6: Commit the CLI**

```bash
git add tools/perf/hardening-profile.mjs tools/perf/hardening-profile.test.mjs
git commit -m "test: add 3000 hardening profile CLI"
```

---

## Task 5: Add failure-mode runbook and rollout actions

**Files:**
- Modify: `docs/optimization/3000-concurrency-hardening-checklist.md`

- [ ] **Step 1: Append failure-mode strategies**

Append:

```md
## Failure-Mode Strategy

### Redis Registry Unavailable

Expected behavior:

- reject new execution admission conservatively
- surface the typed rejection reason `REGISTRY_UNAVAILABLE`
- keep existing executions running
- rely on ZSET TTL self-healing for stale slots

Stop gate:

- any registry failure causes over-admission beyond the configured global budget

### Redis Latency Spike

Expected behavior:

- protect `LIGHT` and `STANDARD`
- reduce or pause `HEAVY` and `RETRY`
- record registry latency and rejection metrics
- stop promotion if Redis p95 stays above 20 ms or p99 stays above 50 ms through one observation window

### MySQL Saturation

Expected behavior:

- do not increase execution concurrency
- batch or buffer trace, audit, stats, and console-view updates when supported
- degrade weak-online writes when supported
- stop promotion if active connections stay at or above 85% of pool max, or slow SQL above 1000 ms appears in two consecutive samples

### RocketMQ Backlog Growth

Expected behavior:

- inspect normal, retry, and heavy backlog independently
- never let retry recovery starve normal traffic
- pause low-priority scheduled, replay, or heavy jobs before increasing consumer pressure
- stop promotion if backlog grows for the full observation window

### Downstream Partial Failure

Expected behavior:

- apply dependency-level timeout, circuit breaker, and bulkhead behavior
- reduce the lane that calls the degraded dependency
- keep unrelated lanes running if their dependencies are healthy
- do not raise global concurrency to compensate for slow downstream

### Retry Backlog Explosion

Expected behavior:

- lengthen retry backoff
- lower `RETRY` lane budget
- move entries to DLQ after max attempts
- stop promotion if retry backlog growth outpaces recovery after downstream recovery
```

- [ ] **Step 2: Append rollout actions**

Append:

```md
## Rollout Actions

### Pass Actions

- keep the 3000 configuration for the next observation window
- preserve profile output JSON under the release evidence directory
- record Redis p95/p99, MySQL active connections, RocketMQ backlog, Disruptor overflow, retry backlog, DLQ count, and downstream p95/p99
- unlock 4000 readiness discussion only after every 3000 profile passes

### Stop Actions

- stop the current profile
- keep the failed run artifacts
- do not raise global concurrency
- identify whether the limiting resource is app worker, Redis, MySQL, RocketMQ, downstream dependency, retry backlog, or heavy-lane starvation

### Rollback Actions

- restore the previous `canvas.execution.max-concurrency` value
- restore previous lane budgets
- pause scheduled, replay, and heavy traffic when needed
- keep normal traffic on the last passing profile
- rerun the affected backend tests after rollback

### Degrade Actions

- lower `RETRY` or lengthen retry backoff
- lower `HEAVY` or pause heavy jobs
- disable low-priority scheduled/replay traffic
- keep `LIGHT` and `STANDARD` protected when their dependencies remain healthy
- reject new admission conservatively if Redis registry health is unknown
```

- [ ] **Step 3: Verify rollback and degrade actions are present**

Run:

```bash
rg -n "Pass Actions|Stop Actions|Rollback Actions|Degrade Actions|REGISTRY_UNAVAILABLE|Retry Backlog Explosion" docs/optimization/3000-concurrency-hardening-checklist.md
```

Expected: each heading or token appears at least once.

- [ ] **Step 4: Commit the runbook**

```bash
git add docs/optimization/3000-concurrency-hardening-checklist.md
git commit -m "docs: add 3000 failure-mode runbook"
```

---

## Task 6: Wire the hardening profiles into the perf README

**Files:**
- Modify: `tools/perf/README.md`

- [ ] **Step 1: Add the 3000 hardening section**

Add this section after `## Threshold Runner`:

```md
## 3000 Hardening Profiles

The 3000 production gate is driven by `tools/perf/3000-hardening-profiles.json`.

Validate the profile file:

```bash
node -e "const p=require('./tools/perf/3000-hardening-profiles.json'); const total=Object.values(p.lanes).reduce((sum,l)=>sum+l.concurrency,0); if (total !== p.targetConcurrency) throw new Error(String(total)); console.log(total)"
```

Render the default mixed 3000 command:

```bash
node tools/perf/hardening-profile.mjs \
  --profile default-mixed-3000 \
  --run-id-prefix "perf_3000_gate_$(date +%Y%m%d_%H%M%S)"
```

Run the rendered command only after:

- the small-flow smoke passes
- impacted backend tests pass on Java 21
- the 3000 code foundation is present in the branch
- rollback and degrade actions from `docs/optimization/3000-concurrency-hardening-checklist.md` are ready

Profiles required for 3000 completion:

- `default-mixed-3000`
- `retry-surge-300`
- `heavy-surge-300`
- `slow-downstream-standard`
- `redis-registry-latency`
- `rocketmq-backlog-recovery`
```

- [ ] **Step 2: Verify README links to the hardening files**

Run:

```bash
rg -n "3000-hardening-profiles.json|hardening-profile.mjs|3000-concurrency-hardening-checklist.md|default-mixed-3000" tools/perf/README.md
```

Expected: each file/profile name appears at least once.

- [ ] **Step 3: Commit the README wiring**

```bash
git add tools/perf/README.md
git commit -m "docs: wire 3000 hardening perf profiles"
```

---

## Task 7: Final verification and consistency review

**Files:**
- Verify: `docs/optimization/3000-concurrency-hardening-checklist.md`
- Verify: `tools/perf/3000-hardening-profiles.json`
- Verify: `tools/perf/hardening-profile.mjs`
- Verify: `tools/perf/hardening-profile.test.mjs`
- Verify: `tools/perf/README.md`

- [ ] **Step 1: Run impacted backend tests**

```bash
JAVA_HOME=$(/usr/libexec/java_home -v 21) mvn -f backend/pom.xml -pl canvas-engine -Dtest=CanvasExecutionServiceTest,CanvasExecutionServiceTriggerNodeTest,InFlightExecutionRegistryTest,CanvasServicePublishTest,CanvasServiceExampleFilterTest,CanvasOpsServiceExampleCloneTest test
```

Expected:

```text
Tests run: 22, Failures: 0, Errors: 0, Skipped: 0
BUILD SUCCESS
```

- [ ] **Step 2: Run hardening profile tests**

```bash
node --test tools/perf/hardening-profile.test.mjs
```

Expected:

```text
# pass 4
# fail 0
```

- [ ] **Step 3: Validate lane total**

```bash
node -e "const p=require('./tools/perf/3000-hardening-profiles.json'); const total=Object.values(p.lanes).reduce((sum,l)=>sum+l.concurrency,0); if (total !== p.targetConcurrency) throw new Error(String(total)); console.log(total)"
```

Expected:

```text
3000
```

- [ ] **Step 4: Render all required profile commands**

```bash
for profile in default-mixed-3000 retry-surge-300 heavy-surge-300 slow-downstream-standard redis-registry-latency rocketmq-backlog-recovery; do
  node tools/perf/hardening-profile.mjs --profile "$profile" --run-id-prefix "perf_${profile}"
done
```

Expected: six threshold-runner commands print, one for each profile.

- [ ] **Step 5: Scan for missing hardening terms**

```bash
rg -n "LIGHT|STANDARD|HEAVY|RETRY|REGISTRY_UNAVAILABLE|rollback|degrade|stop gate|4000" docs/optimization/3000-concurrency-hardening-checklist.md tools/perf/3000-hardening-profiles.json tools/perf/README.md
```

Expected: all terms appear in the hardening files.

- [ ] **Step 6: Run whitespace validation**

```bash
git diff --check -- docs/optimization/3000-concurrency-hardening-checklist.md tools/perf/3000-hardening-profiles.json tools/perf/hardening-profile.mjs tools/perf/hardening-profile.test.mjs tools/perf/README.md
```

Expected: no output.

- [ ] **Step 7: Commit final consistency fixes if any were needed**

```bash
git add docs/optimization/3000-concurrency-hardening-checklist.md tools/perf/3000-hardening-profiles.json tools/perf/hardening-profile.mjs tools/perf/hardening-profile.test.mjs tools/perf/README.md
git commit -m "chore: verify 3000 hardening gates"
```

If no files changed in this final task, do not create an empty commit.
