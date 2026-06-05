# Long Term Architecture Evolution Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Build architecture decision evidence gates for long-term platform changes before any high-blast-radius implementation begins.

**Architecture:** Keep this P3 slice to decision records and validation tooling. Markdown records proof plans and decisions, JSON stores machine-checkable candidate evidence, and a Node.js validator blocks architecture candidates that lack current-code evidence, rollback, dependencies, and child-spec gates.

**Tech Stack:** Markdown, JSON, Node.js 18 `node:test`, existing Git workflow.

---

## Spec Reference

- `docs/product-evolution/specs/p3-003-long-term-architecture-evolution.md`
- Source item: `docs/product-evolution/todo/p3/long-term-architecture-evolution.md`

## File Structure

**Discovery Package**
- Create: `docs/product-evolution/discovery/p3-003-architecture-evolution/README.md`
- Create: `docs/product-evolution/discovery/p3-003-architecture-evolution/evidence.json`
- Create: `docs/product-evolution/discovery/p3-003-architecture-evolution/decision-log.md`
- Create: `docs/product-evolution/discovery/p3-003-architecture-evolution/proof-matrix.md`

**Validation**
- Create: `tools/strategy/architecture-evolution-evidence.mjs`
- Create: `tools/strategy/architecture-evolution-evidence.test.mjs`

No migration is created. This slice does not change runtime topology, persistence schema, editor libraries, event processors, or deployment configuration.

### Task 1: Validator Tests

**Files:**
- Create: `tools/strategy/architecture-evolution-evidence.test.mjs`

- [ ] **Step 1: Write failing validator tests**

Create `tools/strategy/architecture-evolution-evidence.test.mjs`:

```js
import assert from 'node:assert/strict'
import { execFileSync } from 'node:child_process'
import { mkdtempSync, writeFileSync } from 'node:fs'
import { tmpdir } from 'node:os'
import path from 'node:path'
import test from 'node:test'

const script = path.resolve('tools/strategy/architecture-evolution-evidence.mjs')
const evidence = path.resolve('docs/product-evolution/discovery/p3-003-architecture-evolution/evidence.json')

function run(file) {
  return execFileSync(process.execPath, [script, file], { encoding: 'utf8' })
}

test('validates the committed architecture evolution evidence package', () => {
  const output = JSON.parse(run(evidence))

  assert.equal(output.ok, true)
  assert.deepEqual(output.candidateKeys, [
    'service-split',
    'editor-canvas-alternative',
    'event-processing-cep',
    'multi-cloud-deployment',
    'serverless-execution',
    'edge-runtime',
    'data-residency'
  ])
})

test('rejects accepted architecture candidates without current code evidence', () => {
  const dir = mkdtempSync(path.join(tmpdir(), 'architecture-evolution-'))
  const file = path.join(dir, 'evidence.json')
  writeFileSync(file, JSON.stringify({
    package: 'p3-003-architecture-evolution',
    rollout: { migration: 'none', runtimeChange: false },
    candidates: [{
      key: 'service-split',
      owner: 'Architecture',
      status: 'Accepted For Child Spec',
      scaleTrigger: 'Sustained module ownership or runtime scaling pressure',
      proofCommand: 'node --test tools/strategy/architecture-evolution-evidence.test.mjs',
      compatibility: 'Single artifact rollback remains available',
      rollback: 'Deploy monolith artifact',
      dependencyStatus: 'P0/P1 safety gates complete',
      childSpecPath: 'docs/product-evolution/specs/p3-child-service-split.md'
    }]
  }))

  assert.throws(() => run(file), /currentCodeEvidence is required/)
})
```

- [ ] **Step 2: Run tests and confirm red state**

Run:

```bash
node --test tools/strategy/architecture-evolution-evidence.test.mjs
```

Expected: FAIL because the validator script and evidence package do not exist.

### Task 2: Evidence Validator

**Files:**
- Create: `tools/strategy/architecture-evolution-evidence.mjs`
- Test: `tools/strategy/architecture-evolution-evidence.test.mjs`

- [ ] **Step 1: Create the validator script**

Create `tools/strategy/architecture-evolution-evidence.mjs`:

```js
import { readFileSync } from 'node:fs'
import path from 'node:path'

const file = process.argv[2] || 'docs/product-evolution/discovery/p3-003-architecture-evolution/evidence.json'
const payload = JSON.parse(readFileSync(path.resolve(file), 'utf8'))
const required = ['key', 'owner', 'status', 'currentCodeEvidence', 'scaleTrigger', 'proofCommand', 'compatibility', 'rollback', 'dependencyStatus']
const allowed = new Set(['Accepted For Child Spec', 'Needs Evidence', 'Deferred', 'Rejected'])
const errors = []

if (payload.package !== 'p3-003-architecture-evolution') errors.push('package must be p3-003-architecture-evolution')
if (payload.rollout?.migration !== 'none') errors.push('rollout.migration must be none')
if (payload.rollout?.runtimeChange !== false) errors.push('rollout.runtimeChange must be false')
if (!Array.isArray(payload.candidates) || payload.candidates.length === 0) errors.push('candidates must be a non-empty array')

for (const candidate of payload.candidates || []) {
  for (const field of required) {
    if (candidate[field] === undefined || candidate[field] === '' || (Array.isArray(candidate[field]) && candidate[field].length === 0)) {
      errors.push(`${candidate.key || 'unknown'}: ${field} is required`)
    }
  }
  if (!allowed.has(candidate.status)) errors.push(`${candidate.key}: unsupported status ${candidate.status}`)
  if (candidate.status === 'Accepted For Child Spec' && !candidate.childSpecPath) {
    errors.push(`${candidate.key}: childSpecPath is required for Accepted For Child Spec`)
  }
}

if (errors.length > 0) {
  console.error(errors.join('\n'))
  process.exit(1)
}

console.log(JSON.stringify({ ok: true, package: payload.package, candidateKeys: payload.candidates.map((candidate) => candidate.key) }, null, 2))
```

- [ ] **Step 2: Run tests and confirm evidence still missing**

Run:

```bash
node --test tools/strategy/architecture-evolution-evidence.test.mjs
```

Expected: FAIL because `docs/product-evolution/discovery/p3-003-architecture-evolution/evidence.json` does not exist.

### Task 3: Discovery Evidence Package

**Files:**
- Create: `docs/product-evolution/discovery/p3-003-architecture-evolution/README.md`
- Create: `docs/product-evolution/discovery/p3-003-architecture-evolution/evidence.json`
- Create: `docs/product-evolution/discovery/p3-003-architecture-evolution/decision-log.md`
- Create: `docs/product-evolution/discovery/p3-003-architecture-evolution/proof-matrix.md`

- [ ] **Step 1: Create evidence JSON**

Create `docs/product-evolution/discovery/p3-003-architecture-evolution/evidence.json`:

```json
{
  "package": "p3-003-architecture-evolution",
  "rollout": {
    "migration": "none",
    "runtimeChange": false,
    "reason": "This slice records architecture evidence only and does not alter runtime topology or schemas."
  },
  "candidates": [
    {
      "key": "service-split",
      "owner": "Architecture",
      "status": "Needs Evidence",
      "currentCodeEvidence": ["backend/canvas-engine contains web, domain, scheduler, and persistence code in one deployable application."],
      "scaleTrigger": "Independent scaling or ownership pain is measured in production readiness work.",
      "proofCommand": "node --test tools/strategy/architecture-evolution-evidence.test.mjs",
      "compatibility": "Child spec must keep monolith deploy rollback until split service proves parity.",
      "rollback": "Deploy the single canvas-engine artifact.",
      "dependencyStatus": "P0/P1 safety and observability gates must be complete."
    },
    {
      "key": "editor-canvas-alternative",
      "owner": "Frontend Architecture",
      "status": "Deferred",
      "currentCodeEvidence": ["Frontend editor behavior is built around current canvas-editor modules and graph helpers."],
      "scaleTrigger": "Measured editor performance or maintainability problem exceeds targeted fixes.",
      "proofCommand": "node --test tools/strategy/architecture-evolution-evidence.test.mjs",
      "compatibility": "Child spec must preserve import/export and saved graph compatibility.",
      "rollback": "Keep the current editor implementation and disable alternate editor route.",
      "dependencyStatus": "Requires editor productivity and graph state evidence."
    },
    {
      "key": "event-processing-cep",
      "owner": "Runtime Architecture",
      "status": "Needs Evidence",
      "currentCodeEvidence": ["Current runtime uses existing scheduler, MQ, and trace paths rather than Flink CEP."],
      "scaleTrigger": "Complex event workloads exceed current scheduler and MQ guarantees.",
      "proofCommand": "node --test tools/strategy/architecture-evolution-evidence.test.mjs",
      "compatibility": "Child spec must prove dual-run or replay compatibility.",
      "rollback": "Route event execution back to current scheduler path.",
      "dependencyStatus": "Requires event trace schema and runtime evidence."
    },
    {
      "key": "multi-cloud-deployment",
      "owner": "Infrastructure",
      "status": "Deferred",
      "currentCodeEvidence": ["Local development and deployment assumptions are centered on the current Docker and service stack."],
      "scaleTrigger": "Customer or resilience requirement demands multiple cloud targets.",
      "proofCommand": "node --test tools/strategy/architecture-evolution-evidence.test.mjs",
      "compatibility": "Child spec must define config, secrets, networking, and data-store compatibility.",
      "rollback": "Deploy only to the current supported environment.",
      "dependencyStatus": "Requires production operability gates and infrastructure owner."
    },
    {
      "key": "serverless-execution",
      "owner": "Runtime Architecture",
      "status": "Rejected",
      "currentCodeEvidence": ["Current execution model depends on long-running application services and local runtime coordination."],
      "scaleTrigger": "No accepted trigger; evidence does not justify serverless execution now.",
      "proofCommand": "node --test tools/strategy/architecture-evolution-evidence.test.mjs",
      "compatibility": "Would require cold-start, timeout, and state compatibility proof.",
      "rollback": "Keep execution in canvas-engine runtime.",
      "dependencyStatus": "Blocked until runtime evidence shows serverless fit."
    },
    {
      "key": "edge-runtime",
      "owner": "Infrastructure",
      "status": "Deferred",
      "currentCodeEvidence": ["Current product runs backend behavior centrally and does not define edge execution contracts."],
      "scaleTrigger": "Latency or regional availability evidence requires edge placement.",
      "proofCommand": "node --test tools/strategy/architecture-evolution-evidence.test.mjs",
      "compatibility": "Child spec must define cache invalidation, auth, and data-access boundaries.",
      "rollback": "Route traffic to central backend only.",
      "dependencyStatus": "Requires data residency and deployment evidence."
    },
    {
      "key": "data-residency",
      "owner": "Privacy Architecture",
      "status": "Needs Evidence",
      "currentCodeEvidence": ["Tenant data locality rules are not represented as a first-class architecture decision in this package yet."],
      "scaleTrigger": "Target regions or enterprise contracts require residency controls.",
      "proofCommand": "node --test tools/strategy/architecture-evolution-evidence.test.mjs",
      "compatibility": "Child spec must define tenant routing, backups, analytics, and support access constraints.",
      "rollback": "Do not sell residency-bound regions until controls are accepted.",
      "dependencyStatus": "Requires privacy and legal owner."
    }
  ]
}
```

- [ ] **Step 2: Create README and proof matrix**

Create `docs/product-evolution/discovery/p3-003-architecture-evolution/README.md`:

```markdown
# P3-003 Architecture Evolution Discovery

This package records long-term architecture candidates and proof gates. It creates no Flyway migration, runtime topology change, frontend library swap, event processor, cloud deployment, serverless function, edge runtime, or data residency behavior.

## Verification

Run:

```bash
node --test tools/strategy/architecture-evolution-evidence.test.mjs
node tools/strategy/architecture-evolution-evidence.mjs
```

Expected: both commands pass and the validator prints all architecture candidate keys.
```

Create `docs/product-evolution/discovery/p3-003-architecture-evolution/proof-matrix.md`:

```markdown
# Architecture Proof Matrix

| Candidate | Required Proof Before Child Spec |
| --- | --- |
| Service split | Current module boundary evidence, traffic ownership, deploy rollback, and parity command. |
| Editor canvas alternative | Measured editor bottleneck, saved graph compatibility, and fallback route. |
| Event processing CEP | Replay proof, dual-run proof, and scheduler rollback. |
| Multi-cloud deployment | Config, secrets, networking, and data-store compatibility proof. |
| Serverless execution | Cold-start, timeout, state, and cost proof. |
| Edge runtime | Auth, cache, data access, and invalidation proof. |
| Data residency | Tenant routing, backup, analytics, and support-access proof. |
```

- [ ] **Step 3: Create decision log**

Create `docs/product-evolution/discovery/p3-003-architecture-evolution/decision-log.md`:

```markdown
# Architecture Evolution Decision Log

| Candidate | Status | Reason |
| --- | --- | --- |
| Service split | Needs Evidence | Monolith pressure must be measured before extraction. |
| Editor canvas alternative | Deferred | Current editor replacement lacks compatibility and performance proof. |
| Event processing CEP | Needs Evidence | Flink-style CEP requires replay and dual-run proof. |
| Multi-cloud deployment | Deferred | No accepted customer or resilience trigger. |
| Serverless execution | Rejected | Current runtime assumptions do not fit serverless without a stronger trigger. |
| Edge runtime | Deferred | Edge placement depends on latency and data boundary evidence. |
| Data residency | Needs Evidence | Privacy and legal owners must define requirements. |
```

- [ ] **Step 4: Run validator tests**

Run:

```bash
node --test tools/strategy/architecture-evolution-evidence.test.mjs
node tools/strategy/architecture-evolution-evidence.mjs
```

Expected: PASS, and the validator prints all seven architecture candidate keys.

### Task 4: Rollout Notes And Commit

**Files:**
- Modify: `docs/product-evolution/specs/p3-003-long-term-architecture-evolution.md`
- Modify: `docs/product-evolution/plans/p3-003-long-term-architecture-evolution-plan.md`

- [ ] **Step 1: Confirm no migration references remain**

Run:

```bash
pattern="$(node -e "console.log('db/' + 'migration|__' + '[a-z0-9_]+' + '[.]sql')")"
rg -n "$pattern" docs/product-evolution/specs/p3-003-long-term-architecture-evolution.md docs/product-evolution/plans/p3-003-long-term-architecture-evolution-plan.md
```

Expected: no output.

- [ ] **Step 2: Scan for implementation-deferral wording**

Run:

```bash
node - <<'EOF'
const fs = require('node:fs')
const files = [
  'docs/product-evolution/specs/p3-003-long-term-architecture-evolution.md',
  'docs/product-evolution/plans/p3-003-long-term-architecture-evolution-plan.md',
]
const blocked = [
  'T' + 'BD',
  'T' + 'ODO',
  'use existing tests as style reference' + 's',
  'first UI slic' + 'e',
  'implement the service behavio' + 'r',
  'add ' + 'the route, page, panel, or componen' + 't',
  'when the spec require' + 's',
]
let found = false
for (const file of files) {
  const text = fs.readFileSync(file, 'utf8')
  for (const phrase of blocked) {
    if (text.includes(phrase)) {
      console.error(`${file}: forbidden phrase ${phrase}`)
      found = true
    }
  }
}
if (found) process.exit(1)
EOF
```

Expected: no output.

- [ ] **Step 3: Commit the discovery slice**

Run:

```bash
git add docs/product-evolution/discovery/p3-003-architecture-evolution tools/strategy/architecture-evolution-evidence.mjs tools/strategy/architecture-evolution-evidence.test.mjs docs/product-evolution/specs/p3-003-long-term-architecture-evolution.md docs/product-evolution/plans/p3-003-long-term-architecture-evolution-plan.md
git commit -m "docs: add architecture evolution evidence gates"
```

Expected: commit contains only the P3-003 discovery package, validator, test, spec, and plan.
