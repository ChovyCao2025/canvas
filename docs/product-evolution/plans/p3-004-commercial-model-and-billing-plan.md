# Commercial Model And Billing Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Build finance/legal-gated evidence for commercial metrics and billing capabilities before metering, entitlement, invoice, payment, renewal, or upgrade implementation starts.

**Architecture:** Keep this P3 slice to commercial discovery. Markdown records governance and decisions, JSON stores billable capability evidence, and a Node.js validator blocks accepted billing child specs without metric source, finance gate, legal gate, rollback, and proof command.

**Tech Stack:** Markdown, JSON, Node.js 18 `node:test`, existing Git workflow.

**Implementation Status:** Discovery package and validator are complete in the current workspace record. Reverified on 2026-06-08 with `node --test tools/strategy/commercial-billing-evidence.test.mjs` (4 tests passing) and `node tools/strategy/commercial-billing-evidence.mjs`. Commit and merge status was not verified in this docs-only audit; the commit boundary is documented because no commit was requested.

---

## Spec Reference

- `docs/product-evolution/specs/p3-004-commercial-model-and-billing.md`
- Source item: `docs/product-evolution/todo/p3/strategic-opportunities-from-filtered-scope.md#commercial-model-and-billing`

## File Structure

**Discovery Package**
- Create: `docs/product-evolution/discovery/p3-004-commercial-billing/README.md`
- Create: `docs/product-evolution/discovery/p3-004-commercial-billing/evidence.json`
- Create: `docs/product-evolution/discovery/p3-004-commercial-billing/governance-policy.md`
- Create: `docs/product-evolution/discovery/p3-004-commercial-billing/decision-log.md`

**Validation**
- Create: `tools/strategy/commercial-billing-evidence.mjs`
- Create: `tools/strategy/commercial-billing-evidence.test.mjs`

No migration is created. Billing ledgers, entitlements, invoices, payments, and renewal state require later child specs after finance and legal gates are accepted.

### Task 1: Validator Tests

**Files:**
- Create: `tools/strategy/commercial-billing-evidence.test.mjs`

- [x] **Step 1: Write failing validator tests**

Create `tools/strategy/commercial-billing-evidence.test.mjs`:

```js
import assert from 'node:assert/strict'
import { execFileSync } from 'node:child_process'
import { mkdtempSync, writeFileSync } from 'node:fs'
import { tmpdir } from 'node:os'
import path from 'node:path'
import test from 'node:test'

const script = path.resolve('tools/strategy/commercial-billing-evidence.mjs')
const evidence = path.resolve('docs/product-evolution/discovery/p3-004-commercial-billing/evidence.json')

function run(file) {
  return execFileSync(process.execPath, [script, file], { encoding: 'utf8' })
}

test('validates the committed commercial billing evidence package', () => {
  const output = JSON.parse(run(evidence))

  assert.equal(output.ok, true)
  assert.deepEqual(output.capabilityKeys, [
    'billable-metrics',
    'plan-tiers',
    'overage-policy',
    'payment-provider',
    'invoice-drafts',
    'renewal-process',
    'upgrade-recommendations'
  ])
})

test('rejects accepted billing capabilities without finance gate', () => {
  const dir = mkdtempSync(path.join(tmpdir(), 'commercial-billing-'))
  const file = path.join(dir, 'evidence.json')
  writeFileSync(file, JSON.stringify({
    package: 'p3-004-commercial-billing',
    rollout: { migration: 'none', runtimeChange: false, customerCharging: false },
    capabilities: [{
      key: 'billable-metrics',
      owner: 'Commercial Product',
      status: 'Accepted For Child Spec',
      metricDefinition: 'Monthly billable executions',
      sourceEvidence: ['Execution history can be counted by tenant after analytics source is accepted.'],
      legalGate: 'Legal approves metric language',
      supportGate: 'Support approves dispute process',
      proofCommand: 'node --test tools/strategy/commercial-billing-evidence.test.mjs',
      rollback: 'Do not expose customer billing',
      childSpecPath: 'docs/product-evolution/specs/p3-child-billable-metrics.md'
    }]
  }))

  assert.throws(() => run(file), /financeGate is required/)
})
```

- [x] **Step 2: Run tests and confirm red state**

Run:

```bash
node --test tools/strategy/commercial-billing-evidence.test.mjs
```

Expected: FAIL because the validator script and evidence package do not exist.

### Task 2: Evidence Validator

**Files:**
- Create: `tools/strategy/commercial-billing-evidence.mjs`
- Test: `tools/strategy/commercial-billing-evidence.test.mjs`

- [x] **Step 1: Create the validator script**

Create `tools/strategy/commercial-billing-evidence.mjs`:

```js
import { readFileSync } from 'node:fs'
import path from 'node:path'

const file = process.argv[2] || 'docs/product-evolution/discovery/p3-004-commercial-billing/evidence.json'
const payload = JSON.parse(readFileSync(path.resolve(file), 'utf8'))
const required = ['key', 'owner', 'status', 'metricDefinition', 'sourceEvidence', 'financeGate', 'legalGate', 'supportGate', 'proofCommand', 'rollback']
const allowed = new Set(['Accepted For Child Spec', 'Needs Evidence', 'Deferred', 'Rejected'])
const errors = []

if (payload.package !== 'p3-004-commercial-billing') errors.push('package must be p3-004-commercial-billing')
if (payload.rollout?.migration !== 'none') errors.push('rollout.migration must be none')
if (payload.rollout?.runtimeChange !== false) errors.push('rollout.runtimeChange must be false')
if (payload.rollout?.customerCharging !== false) errors.push('rollout.customerCharging must be false')
if (!Array.isArray(payload.capabilities) || payload.capabilities.length === 0) errors.push('capabilities must be a non-empty array')

for (const capability of payload.capabilities || []) {
  for (const field of required) {
    if (capability[field] === undefined || capability[field] === '' || (Array.isArray(capability[field]) && capability[field].length === 0)) {
      errors.push(`${capability.key || 'unknown'}: ${field} is required`)
    }
  }
  if (!allowed.has(capability.status)) errors.push(`${capability.key}: unsupported status ${capability.status}`)
  if (capability.status === 'Accepted For Child Spec' && !capability.childSpecPath) {
    errors.push(`${capability.key}: childSpecPath is required for Accepted For Child Spec`)
  }
}

if (errors.length > 0) {
  console.error(errors.join('\n'))
  process.exit(1)
}

console.log(JSON.stringify({ ok: true, package: payload.package, capabilityKeys: payload.capabilities.map((capability) => capability.key) }, null, 2))
```

- [x] **Step 2: Run tests and confirm evidence still missing**

Run:

```bash
node --test tools/strategy/commercial-billing-evidence.test.mjs
```

Expected: FAIL because `docs/product-evolution/discovery/p3-004-commercial-billing/evidence.json` does not exist.

### Task 3: Discovery Evidence Package

**Files:**
- Create: `docs/product-evolution/discovery/p3-004-commercial-billing/README.md`
- Create: `docs/product-evolution/discovery/p3-004-commercial-billing/evidence.json`
- Create: `docs/product-evolution/discovery/p3-004-commercial-billing/governance-policy.md`
- Create: `docs/product-evolution/discovery/p3-004-commercial-billing/decision-log.md`

- [x] **Step 1: Create evidence JSON**

Create `docs/product-evolution/discovery/p3-004-commercial-billing/evidence.json`:

```json
{
  "package": "p3-004-commercial-billing",
  "rollout": {
    "migration": "none",
    "runtimeChange": false,
    "customerCharging": false,
    "reason": "This slice records billing evidence only and does not create ledgers, entitlements, invoices, payments, or charges."
  },
  "capabilities": [
    {
      "key": "billable-metrics",
      "owner": "Commercial Product",
      "status": "Needs Evidence",
      "metricDefinition": "Rank executions, contacts, messages, seats, storage, AI usage, and premium connectors as candidate billable metrics.",
      "sourceEvidence": ["Product usage analytics and execution traces must prove measurable tenant-scoped usage."],
      "financeGate": "Finance approves metric as billable and auditable.",
      "legalGate": "Legal approves customer-facing metric language.",
      "supportGate": "Support approves usage dispute process.",
      "proofCommand": "node --test tools/strategy/commercial-billing-evidence.test.mjs",
      "rollback": "Do not expose billing metrics to customers."
    },
    {
      "key": "plan-tiers",
      "owner": "Commercial Product",
      "status": "Needs Evidence",
      "metricDefinition": "Plan tiers map included usage, seats, connectors, and support level to named packages.",
      "sourceEvidence": ["Customer segmentation and usage distribution are required before tier commitments."],
      "financeGate": "Finance approves package economics.",
      "legalGate": "Legal approves plan terms.",
      "supportGate": "Support approves service-level expectations.",
      "proofCommand": "node --test tools/strategy/commercial-billing-evidence.test.mjs",
      "rollback": "Keep pricing unpublished."
    },
    {
      "key": "overage-policy",
      "owner": "Finance",
      "status": "Deferred",
      "metricDefinition": "Overage defines behavior when measured usage exceeds included plan quantity.",
      "sourceEvidence": ["Billable metric and plan tiers must be accepted first."],
      "financeGate": "Finance approves overage pricing and grace policy.",
      "legalGate": "Legal approves notification and contract language.",
      "supportGate": "Support approves escalation process.",
      "proofCommand": "node --test tools/strategy/commercial-billing-evidence.test.mjs",
      "rollback": "Disable overage enforcement and keep advisory notices internal."
    },
    {
      "key": "payment-provider",
      "owner": "Finance Systems",
      "status": "Deferred",
      "metricDefinition": "Payment provider handles collection after invoice and entitlement requirements are accepted.",
      "sourceEvidence": ["Provider choice depends on regions, invoice requirements, and finance system ownership."],
      "financeGate": "Finance approves provider and reconciliation flow.",
      "legalGate": "Legal approves payment terms and data processing.",
      "supportGate": "Support approves failed-payment handling.",
      "proofCommand": "node --test tools/strategy/commercial-billing-evidence.test.mjs",
      "rollback": "Keep payment collection outside product."
    },
    {
      "key": "invoice-drafts",
      "owner": "Finance",
      "status": "Needs Evidence",
      "metricDefinition": "Invoice drafts summarize tenant usage, plan, discounts, taxes, and adjustments before charge.",
      "sourceEvidence": ["Usage ledger and finance review are required before invoice drafts are generated."],
      "financeGate": "Finance approves invoice format and reconciliation.",
      "legalGate": "Legal approves invoice terms and tax assumptions.",
      "supportGate": "Support approves invoice dispute handling.",
      "proofCommand": "node --test tools/strategy/commercial-billing-evidence.test.mjs",
      "rollback": "Do not generate customer-visible invoices."
    },
    {
      "key": "renewal-process",
      "owner": "Revenue Operations",
      "status": "Deferred",
      "metricDefinition": "Renewal process combines account term, usage evidence, contract status, and owner review.",
      "sourceEvidence": ["Account lifecycle and billing source of truth are not accepted."],
      "financeGate": "Finance approves renewal forecast inputs.",
      "legalGate": "Legal approves renewal notice obligations.",
      "supportGate": "Customer Success approves renewal workflow.",
      "proofCommand": "node --test tools/strategy/commercial-billing-evidence.test.mjs",
      "rollback": "Keep renewals manual."
    },
    {
      "key": "upgrade-recommendations",
      "owner": "Growth Product",
      "status": "Deferred",
      "metricDefinition": "Upgrade recommendations use usage thresholds and plan fit to suggest higher tiers.",
      "sourceEvidence": ["Plan tiers and billable metrics must be accepted first."],
      "financeGate": "Finance approves recommendation economics.",
      "legalGate": "Legal approves customer-facing recommendation language.",
      "supportGate": "Support approves customer escalation path.",
      "proofCommand": "node --test tools/strategy/commercial-billing-evidence.test.mjs",
      "rollback": "Hide upgrade recommendations."
    }
  ]
}
```

- [x] **Step 2: Create README and governance docs**

Create `docs/product-evolution/discovery/p3-004-commercial-billing/README.md`:

```markdown
# P3-004 Commercial Billing Discovery

This package evaluates commercial model and billing capabilities. It ships no migration, no charging behavior, no entitlement enforcement, no invoice generation, no payment provider integration, no renewal automation, and no customer-facing upgrade UI.

## Verification

Run:

```bash
node --test tools/strategy/commercial-billing-evidence.test.mjs
node tools/strategy/commercial-billing-evidence.mjs
```

Expected: both commands pass and the validator prints all commercial capability keys.
```

Create `docs/product-evolution/discovery/p3-004-commercial-billing/governance-policy.md`:

```markdown
# Commercial Billing Governance Policy

- Billing capabilities require finance, legal, support, product, and engineering owners before child specs.
- Discovery metrics are not billable commitments.
- Customer charging, entitlement enforcement, invoices, payments, renewals, and upgrade recommendations require separate child specs.
- This slice has no Flyway migration; rollback is reverting or amending the discovery package.
```

- [x] **Step 3: Create decision log**

Create `docs/product-evolution/discovery/p3-004-commercial-billing/decision-log.md`:

```markdown
# Commercial Billing Decision Log

| Capability | Status | Reason |
| --- | --- | --- |
| Billable metrics | Needs Evidence | Tenant-scoped metric source and finance approval are required. |
| Plan tiers | Needs Evidence | Usage distribution and package economics are not accepted. |
| Overage policy | Deferred | Depends on accepted metrics and tiers. |
| Payment provider | Deferred | Depends on invoice, region, and finance system decisions. |
| Invoice drafts | Needs Evidence | Usage ledger and invoice reconciliation proof are required. |
| Renewal process | Deferred | Account lifecycle source of truth is not accepted. |
| Upgrade recommendations | Deferred | Depends on accepted plan tiers and customer language review. |
```

- [x] **Step 4: Run validator tests**

Run:

```bash
node --test tools/strategy/commercial-billing-evidence.test.mjs
node tools/strategy/commercial-billing-evidence.mjs
```

Expected: PASS, and the validator prints all seven commercial capability keys.

### Task 4: Rollout Notes And Commit

**Files:**
- Modify: `docs/product-evolution/specs/p3-004-commercial-model-and-billing.md`
- Modify: `docs/product-evolution/plans/p3-004-commercial-model-and-billing-plan.md`

- [x] **Step 1: Confirm no migration references remain**

Run:

```bash
pattern="$(node -e "console.log('db/' + 'migration|__' + '[a-z0-9_]+' + '[.]sql')")"
rg -n "$pattern" docs/product-evolution/specs/p3-004-commercial-model-and-billing.md docs/product-evolution/plans/p3-004-commercial-model-and-billing-plan.md
```

Expected: no output.

- [x] **Step 2: Scan for implementation-deferral wording**

Run:

```bash
node - <<'EOF'
const fs = require('node:fs')
const files = [
  'docs/product-evolution/specs/p3-004-commercial-model-and-billing.md',
  'docs/product-evolution/plans/p3-004-commercial-model-and-billing-plan.md',
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
git add docs/product-evolution/discovery/p3-004-commercial-billing tools/strategy/commercial-billing-evidence.mjs tools/strategy/commercial-billing-evidence.test.mjs docs/product-evolution/specs/p3-004-commercial-model-and-billing.md docs/product-evolution/plans/p3-004-commercial-model-and-billing-plan.md
git commit -m "docs: add commercial billing evidence gates"
```

Expected: commit contains only the P3-004 discovery package, validator, test, spec, and plan.
