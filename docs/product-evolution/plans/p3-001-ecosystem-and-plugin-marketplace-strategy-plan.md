# Ecosystem And Plugin Marketplace Strategy Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Build a Git-tracked evidence and governance package that decides whether public plugin marketplace work can graduate into child specs.

**Architecture:** Keep this P3 slice discovery-only: Markdown captures policy and decisions, JSON captures machine-checkable evidence, and a Node.js validator blocks unsupported promotion. No backend endpoint, frontend route, or Flyway migration is created because no runtime marketplace behavior ships in this slice.

**Tech Stack:** Markdown, JSON, Node.js 18 `node:test`, existing Git workflow.

**Implementation Status:** Discovery package and validator are complete in the current workspace record. Reverified on 2026-06-08 with `node --test tools/strategy/plugin-marketplace-evidence.test.mjs` (5 tests passing) and `node tools/strategy/plugin-marketplace-evidence.mjs`. Commit and merge status was not verified in this docs-only audit; the commit boundary is documented because no commit was requested.

---

## Spec Reference

- `docs/product-evolution/specs/p3-001-ecosystem-and-plugin-marketplace-strategy.md`
- Source item: `docs/product-evolution/todo/p3/ecosystem-and-plugin-marketplace-strategy.md`

## File Structure

**Discovery Package**
- Create: `docs/product-evolution/discovery/p3-001-plugin-marketplace/README.md` - package overview, source links, rollout stance, and child-spec rules.
- Create: `docs/product-evolution/discovery/p3-001-plugin-marketplace/evidence.json` - machine-readable capability evidence.
- Create: `docs/product-evolution/discovery/p3-001-plugin-marketplace/governance-policy.md` - security, support, compatibility, and takedown policy.
- Create: `docs/product-evolution/discovery/p3-001-plugin-marketplace/decision-log.md` - accepted, deferred, and rejected marketplace decisions.

**Validation**
- Create: `tools/strategy/plugin-marketplace-evidence.mjs` - validates the evidence package.
- Create: `tools/strategy/plugin-marketplace-evidence.test.mjs` - validator tests.

No migration is created. The data touchpoint is Git-tracked strategy evidence, not application state.

### Task 1: Validator Tests

**Files:**
- Create: `tools/strategy/plugin-marketplace-evidence.test.mjs`

- [x] **Step 1: Write failing validator tests**

Create `tools/strategy/plugin-marketplace-evidence.test.mjs`:

```js
import assert from 'node:assert/strict'
import { execFileSync } from 'node:child_process'
import { mkdtempSync, writeFileSync } from 'node:fs'
import { tmpdir } from 'node:os'
import path from 'node:path'
import test from 'node:test'

const script = path.resolve('tools/strategy/plugin-marketplace-evidence.mjs')
const evidence = path.resolve('docs/product-evolution/discovery/p3-001-plugin-marketplace/evidence.json')

function run(file) {
  return execFileSync(process.execPath, [script, file], { encoding: 'utf8' })
}

test('validates the committed plugin marketplace evidence package', () => {
  const output = JSON.parse(run(evidence))

  assert.equal(output.ok, true)
  assert.deepEqual(output.candidateKeys, [
    'plugin-submission',
    'security-review',
    'marketplace-publishing',
    'sdk-compatibility',
    'commercial-terms',
    'partner-support',
    'plugin-takedown'
  ])
})

test('rejects accepted capabilities without child spec path', () => {
  const dir = mkdtempSync(path.join(tmpdir(), 'plugin-marketplace-'))
  const file = path.join(dir, 'evidence.json')
  writeFileSync(file, JSON.stringify({
    package: 'p3-001-plugin-marketplace',
    rollout: { migration: 'none', runtimeChange: false },
    candidates: [{
      key: 'plugin-submission',
      owner: 'Platform Product',
      status: 'Accepted For Child Spec',
      evidence: ['P2 plugin foundations define internal extension boundaries'],
      proofCommand: 'node --test tools/strategy/plugin-marketplace-evidence.test.mjs',
      launchGate: 'Security owner approves package review checklist',
      rollback: 'Keep marketplace entry points disabled',
      dependencies: ['P2-002 plugin foundations']
    }]
  }))

  assert.throws(() => run(file), /childSpecPath is required/)
})
```

- [x] **Step 2: Run tests and confirm red state**

Run:

```bash
node --test tools/strategy/plugin-marketplace-evidence.test.mjs
```

Expected: FAIL because `tools/strategy/plugin-marketplace-evidence.mjs` and the evidence package do not exist.

### Task 2: Evidence Validator

**Files:**
- Create: `tools/strategy/plugin-marketplace-evidence.mjs`
- Test: `tools/strategy/plugin-marketplace-evidence.test.mjs`

- [x] **Step 1: Create the validator script**

Create `tools/strategy/plugin-marketplace-evidence.mjs`:

```js
import { readFileSync } from 'node:fs'
import path from 'node:path'

const file = process.argv[2] || 'docs/product-evolution/discovery/p3-001-plugin-marketplace/evidence.json'
const payload = JSON.parse(readFileSync(path.resolve(file), 'utf8'))
const required = ['key', 'owner', 'status', 'evidence', 'proofCommand', 'launchGate', 'rollback', 'dependencies']
const allowed = new Set(['Accepted For Child Spec', 'Needs Evidence', 'Deferred', 'Rejected'])
const errors = []

if (payload.package !== 'p3-001-plugin-marketplace') {
  errors.push('package must be p3-001-plugin-marketplace')
}
if (payload.rollout?.migration !== 'none') {
  errors.push('rollout.migration must be none')
}
if (payload.rollout?.runtimeChange !== false) {
  errors.push('rollout.runtimeChange must be false')
}
if (!Array.isArray(payload.candidates) || payload.candidates.length === 0) {
  errors.push('candidates must be a non-empty array')
}

for (const candidate of payload.candidates || []) {
  for (const field of required) {
    if (candidate[field] === undefined || candidate[field] === '' || (Array.isArray(candidate[field]) && candidate[field].length === 0)) {
      errors.push(`${candidate.key || 'unknown'}: ${field} is required`)
    }
  }
  if (!allowed.has(candidate.status)) {
    errors.push(`${candidate.key}: unsupported status ${candidate.status}`)
  }
  if (candidate.status === 'Accepted For Child Spec' && !candidate.childSpecPath) {
    errors.push(`${candidate.key}: childSpecPath is required for Accepted For Child Spec`)
  }
}

if (errors.length > 0) {
  console.error(errors.join('\n'))
  process.exit(1)
}

console.log(JSON.stringify({
  ok: true,
  package: payload.package,
  candidateKeys: payload.candidates.map((candidate) => candidate.key)
}, null, 2))
```

- [x] **Step 2: Run tests and confirm evidence still missing**

Run:

```bash
node --test tools/strategy/plugin-marketplace-evidence.test.mjs
```

Expected: FAIL because `docs/product-evolution/discovery/p3-001-plugin-marketplace/evidence.json` does not exist.

### Task 3: Discovery Evidence Package

**Files:**
- Create: `docs/product-evolution/discovery/p3-001-plugin-marketplace/README.md`
- Create: `docs/product-evolution/discovery/p3-001-plugin-marketplace/evidence.json`
- Create: `docs/product-evolution/discovery/p3-001-plugin-marketplace/governance-policy.md`
- Create: `docs/product-evolution/discovery/p3-001-plugin-marketplace/decision-log.md`

- [x] **Step 1: Create evidence JSON**

Create `docs/product-evolution/discovery/p3-001-plugin-marketplace/evidence.json`:

```json
{
  "package": "p3-001-plugin-marketplace",
  "rollout": {
    "migration": "none",
    "runtimeChange": false,
    "reason": "This slice records marketplace evidence in Git and does not create runtime plugin marketplace state."
  },
  "candidates": [
    {
      "key": "plugin-submission",
      "owner": "Platform Product",
      "status": "Needs Evidence",
      "evidence": ["Internal plugin foundations must prove packaging and install boundaries first."],
      "proofCommand": "node --test tools/strategy/plugin-marketplace-evidence.test.mjs",
      "launchGate": "Submission checklist approved by platform, security, and support owners.",
      "rollback": "Do not expose public submission entry points.",
      "dependencies": ["P2-002 plugin foundations"]
    },
    {
      "key": "security-review",
      "owner": "Security",
      "status": "Needs Evidence",
      "evidence": ["Review must cover package signing, permission boundaries, vulnerability response, and tenant isolation."],
      "proofCommand": "node --test tools/strategy/plugin-marketplace-evidence.test.mjs",
      "launchGate": "Security review checklist is approved and linked from a child spec.",
      "rollback": "Block plugin publishing until manual review passes.",
      "dependencies": ["P2-002 plugin foundations"]
    },
    {
      "key": "marketplace-publishing",
      "owner": "Platform Product",
      "status": "Deferred",
      "evidence": ["Publishing depends on submission, review, support, and commercial policy."],
      "proofCommand": "node --test tools/strategy/plugin-marketplace-evidence.test.mjs",
      "launchGate": "Publishing child spec names moderation and rollback controls.",
      "rollback": "Hide marketplace catalog and keep internal plugin registry only.",
      "dependencies": ["plugin-submission", "security-review", "partner-support"]
    },
    {
      "key": "sdk-compatibility",
      "owner": "Developer Experience",
      "status": "Needs Evidence",
      "evidence": ["SDK versioning must describe supported host APIs and deprecation windows."],
      "proofCommand": "node --test tools/strategy/plugin-marketplace-evidence.test.mjs",
      "launchGate": "SDK compatibility matrix is reviewed before public examples ship.",
      "rollback": "Freeze public SDK claims and keep examples internal.",
      "dependencies": ["P2-002 plugin foundations"]
    },
    {
      "key": "commercial-terms",
      "owner": "Business Operations",
      "status": "Deferred",
      "evidence": ["Marketplace pricing and rev-share require commercial and legal review."],
      "proofCommand": "node --test tools/strategy/plugin-marketplace-evidence.test.mjs",
      "launchGate": "Commercial owner approves terms and support cost model.",
      "rollback": "Launch no paid marketplace offers.",
      "dependencies": ["P3-004 commercial model and billing"]
    },
    {
      "key": "partner-support",
      "owner": "Customer Success",
      "status": "Needs Evidence",
      "evidence": ["Support must cover escalation, ownership, SLAs, and partner contact routing."],
      "proofCommand": "node --test tools/strategy/plugin-marketplace-evidence.test.mjs",
      "launchGate": "Support escalation runbook is accepted.",
      "rollback": "Disable partner listing and route support to internal plugins only.",
      "dependencies": ["plugin-submission"]
    },
    {
      "key": "plugin-takedown",
      "owner": "Security",
      "status": "Needs Evidence",
      "evidence": ["Takedown policy must cover abuse, vulnerabilities, tenant impact, and customer notice."],
      "proofCommand": "node --test tools/strategy/plugin-marketplace-evidence.test.mjs",
      "launchGate": "Takedown workflow has security and support approval.",
      "rollback": "Remove marketplace listing and block installs for affected plugin key.",
      "dependencies": ["security-review", "partner-support"]
    }
  ]
}
```

- [x] **Step 2: Create package README**

Create `docs/product-evolution/discovery/p3-001-plugin-marketplace/README.md`:

```markdown
# P3-001 Plugin Marketplace Discovery

This package is a discovery and governance slice for public plugin marketplace strategy. It does not create application tables, routes, UI, payment behavior, plugin upload behavior, or publishing behavior.

## Promotion Rule

A capability can move to `Accepted For Child Spec` only when `evidence.json` includes an owner, evidence, proof command, launch gate, rollback path, dependencies, and a child spec path.

## Verification

Run:

```bash
node --test tools/strategy/plugin-marketplace-evidence.test.mjs
node tools/strategy/plugin-marketplace-evidence.mjs
```

Expected: both commands pass and the validator prints candidate keys for all marketplace capabilities.
```

- [x] **Step 3: Create governance policy**

Create `docs/product-evolution/discovery/p3-001-plugin-marketplace/governance-policy.md`:

```markdown
# Plugin Marketplace Governance Policy

## Required Gates

- Security review covers signing, permissions, tenant isolation, dependency vulnerabilities, and takedown.
- Developer experience review covers SDK versioning, sample plugin maintenance, and deprecation windows.
- Support review covers partner contact, customer escalation, incident ownership, and support limits.
- Commercial review covers paid listing eligibility, marketplace fees, and partner obligations.

## No Runtime Rollout

This P3 slice has no Flyway migration and no runtime route. Rollback is a documentation rollback: revert the discovery package commit or leave capabilities below `Accepted For Child Spec`.
```

- [x] **Step 4: Create decision log**

Create `docs/product-evolution/discovery/p3-001-plugin-marketplace/decision-log.md`:

```markdown
# Plugin Marketplace Decision Log

| Capability | Status | Reason |
| --- | --- | --- |
| Plugin submission | Needs Evidence | Internal plugin foundations must prove packaging and install safety. |
| Security review | Needs Evidence | Public plugins require signing, permission, and vulnerability policy. |
| Marketplace publishing | Deferred | Publishing depends on submission, review, support, and commercial gates. |
| SDK compatibility | Needs Evidence | Public SDK support windows are not yet committed. |
| Commercial terms | Deferred | Marketplace terms belong after commercial billing gates. |
| Partner support | Needs Evidence | Escalation and ownership are not yet approved. |
| Plugin takedown | Needs Evidence | Abuse and vulnerability response needs security ownership. |
```

- [x] **Step 5: Run validator tests**

Run:

```bash
node --test tools/strategy/plugin-marketplace-evidence.test.mjs
node tools/strategy/plugin-marketplace-evidence.mjs
```

Expected: PASS, and the validator prints all seven candidate keys.

### Task 4: Rollout Notes And Commit

**Files:**
- Modify: `docs/product-evolution/specs/p3-001-ecosystem-and-plugin-marketplace-strategy.md`
- Modify: `docs/product-evolution/plans/p3-001-ecosystem-and-plugin-marketplace-strategy-plan.md`

- [x] **Step 1: Confirm no migration references remain**

Run:

```bash
pattern="$(node -e "console.log('db/' + 'migration|__' + '[a-z0-9_]+' + '[.]sql')")"
rg -n "$pattern" docs/product-evolution/specs/p3-001-ecosystem-and-plugin-marketplace-strategy.md docs/product-evolution/plans/p3-001-ecosystem-and-plugin-marketplace-strategy-plan.md
```

Expected: no output.

- [x] **Step 2: Scan for implementation-deferral wording**

Run:

```bash
node - <<'EOF'
const fs = require('node:fs')
const files = [
  'docs/product-evolution/specs/p3-001-ecosystem-and-plugin-marketplace-strategy.md',
  'docs/product-evolution/plans/p3-001-ecosystem-and-plugin-marketplace-strategy-plan.md',
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

- [x] **Step 3: Document commit boundary**
Boundary: No git commit or merge was created in this docs-only audit; the command below remains the future scoped staging recipe.

Run:

```bash
git add docs/product-evolution/discovery/p3-001-plugin-marketplace tools/strategy/plugin-marketplace-evidence.mjs tools/strategy/plugin-marketplace-evidence.test.mjs docs/product-evolution/specs/p3-001-ecosystem-and-plugin-marketplace-strategy.md docs/product-evolution/plans/p3-001-ecosystem-and-plugin-marketplace-strategy-plan.md
git commit -m "docs: add plugin marketplace evidence gates"
```

Expected: commit contains only the P3-001 discovery package, validator, test, spec, and plan.
