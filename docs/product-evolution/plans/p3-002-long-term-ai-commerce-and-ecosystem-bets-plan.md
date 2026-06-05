# Long Term AI Commerce And Ecosystem Bets Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Build a ranked evidence backlog for long-term AI commerce and ecosystem bets with human-approval and child-spec gates.

**Architecture:** Keep this P3 slice as strategy validation. Markdown explains governance, JSON stores bet evidence, and a Node.js validator prevents unsupported AI or commercial bets from being promoted into implementation.

**Tech Stack:** Markdown, JSON, Node.js 18 `node:test`, existing Git workflow.

---

## Spec Reference

- `docs/product-evolution/specs/p3-002-long-term-ai-commerce-and-ecosystem-bets.md`
- Source item: `docs/product-evolution/todo/p3/long-term-ai-commerce-and-ecosystem-bets.md`

## File Structure

**Discovery Package**
- Create: `docs/product-evolution/discovery/p3-002-ai-commerce-bets/README.md`
- Create: `docs/product-evolution/discovery/p3-002-ai-commerce-bets/evidence.json`
- Create: `docs/product-evolution/discovery/p3-002-ai-commerce-bets/governance-policy.md`
- Create: `docs/product-evolution/discovery/p3-002-ai-commerce-bets/decision-log.md`

**Validation**
- Create: `tools/strategy/ai-commerce-bets-evidence.mjs`
- Create: `tools/strategy/ai-commerce-bets-evidence.test.mjs`

No migration is created. This slice does not store model output, customer action data, or commercial data in application tables.

### Task 1: Validator Tests

**Files:**
- Create: `tools/strategy/ai-commerce-bets-evidence.test.mjs`

- [ ] **Step 1: Write failing validator tests**

Create `tools/strategy/ai-commerce-bets-evidence.test.mjs`:

```js
import assert from 'node:assert/strict'
import { execFileSync } from 'node:child_process'
import { mkdtempSync, writeFileSync } from 'node:fs'
import { tmpdir } from 'node:os'
import path from 'node:path'
import test from 'node:test'

const script = path.resolve('tools/strategy/ai-commerce-bets-evidence.mjs')
const evidence = path.resolve('docs/product-evolution/discovery/p3-002-ai-commerce-bets/evidence.json')

function run(file) {
  return execFileSync(process.execPath, [script, file], { encoding: 'utf8' })
}

test('validates the committed AI commerce bets evidence package', () => {
  const output = JSON.parse(run(evidence))

  assert.equal(output.ok, true)
  assert.deepEqual(output.betKeys, [
    'ai-agent-assistance',
    'ai-native-operations',
    'commerce-expansion',
    'industry-packaging',
    'globalization',
    'privacy-readiness',
    'ecosystem-program'
  ])
})

test('rejects accepted AI bets without approval boundary', () => {
  const dir = mkdtempSync(path.join(tmpdir(), 'ai-commerce-bets-'))
  const file = path.join(dir, 'evidence.json')
  writeFileSync(file, JSON.stringify({
    package: 'p3-002-ai-commerce-bets',
    rollout: { migration: 'none', runtimeChange: false },
    bets: [{
      key: 'ai-agent-assistance',
      owner: 'AI Product',
      status: 'Accepted For Child Spec',
      customerEvidence: ['Operators ask for guided campaign optimization.'],
      dependencyStatus: 'P2 analytics evidence required',
      modelRiskStatus: 'Requires review before action automation',
      proofCommand: 'node --test tools/strategy/ai-commerce-bets-evidence.test.mjs',
      rollback: 'Keep AI suggestions hidden behind internal review',
      childSpecPath: 'docs/product-evolution/specs/p3-child-ai-agent-assistance.md'
    }]
  }))

  assert.throws(() => run(file), /approvalBoundary is required/)
})
```

- [ ] **Step 2: Run tests and confirm red state**

Run:

```bash
node --test tools/strategy/ai-commerce-bets-evidence.test.mjs
```

Expected: FAIL because the validator script and evidence package do not exist.

### Task 2: Evidence Validator

**Files:**
- Create: `tools/strategy/ai-commerce-bets-evidence.mjs`
- Test: `tools/strategy/ai-commerce-bets-evidence.test.mjs`

- [ ] **Step 1: Create the validator script**

Create `tools/strategy/ai-commerce-bets-evidence.mjs`:

```js
import { readFileSync } from 'node:fs'
import path from 'node:path'

const file = process.argv[2] || 'docs/product-evolution/discovery/p3-002-ai-commerce-bets/evidence.json'
const payload = JSON.parse(readFileSync(path.resolve(file), 'utf8'))
const required = ['key', 'owner', 'status', 'customerEvidence', 'dependencyStatus', 'modelRiskStatus', 'approvalBoundary', 'proofCommand', 'rollback']
const allowed = new Set(['Accepted For Child Spec', 'Needs Evidence', 'Deferred', 'Rejected'])
const errors = []

if (payload.package !== 'p3-002-ai-commerce-bets') errors.push('package must be p3-002-ai-commerce-bets')
if (payload.rollout?.migration !== 'none') errors.push('rollout.migration must be none')
if (payload.rollout?.runtimeChange !== false) errors.push('rollout.runtimeChange must be false')
if (!Array.isArray(payload.bets) || payload.bets.length === 0) errors.push('bets must be a non-empty array')

for (const bet of payload.bets || []) {
  for (const field of required) {
    if (bet[field] === undefined || bet[field] === '' || (Array.isArray(bet[field]) && bet[field].length === 0)) {
      errors.push(`${bet.key || 'unknown'}: ${field} is required`)
    }
  }
  if (!allowed.has(bet.status)) errors.push(`${bet.key}: unsupported status ${bet.status}`)
  if (bet.status === 'Accepted For Child Spec' && !bet.childSpecPath) {
    errors.push(`${bet.key}: childSpecPath is required for Accepted For Child Spec`)
  }
}

if (errors.length > 0) {
  console.error(errors.join('\n'))
  process.exit(1)
}

console.log(JSON.stringify({ ok: true, package: payload.package, betKeys: payload.bets.map((bet) => bet.key) }, null, 2))
```

- [ ] **Step 2: Run tests and confirm evidence still missing**

Run:

```bash
node --test tools/strategy/ai-commerce-bets-evidence.test.mjs
```

Expected: FAIL because `docs/product-evolution/discovery/p3-002-ai-commerce-bets/evidence.json` does not exist.

### Task 3: Discovery Evidence Package

**Files:**
- Create: `docs/product-evolution/discovery/p3-002-ai-commerce-bets/README.md`
- Create: `docs/product-evolution/discovery/p3-002-ai-commerce-bets/evidence.json`
- Create: `docs/product-evolution/discovery/p3-002-ai-commerce-bets/governance-policy.md`
- Create: `docs/product-evolution/discovery/p3-002-ai-commerce-bets/decision-log.md`

- [ ] **Step 1: Create evidence JSON**

Create `docs/product-evolution/discovery/p3-002-ai-commerce-bets/evidence.json`:

```json
{
  "package": "p3-002-ai-commerce-bets",
  "rollout": {
    "migration": "none",
    "runtimeChange": false,
    "reason": "This strategy slice records AI commerce bet evidence only and does not execute AI actions."
  },
  "bets": [
    {
      "key": "ai-agent-assistance",
      "owner": "AI Product",
      "status": "Needs Evidence",
      "customerEvidence": ["Operators need decision support before autonomous campaign actions are considered."],
      "dependencyStatus": "Requires analytics event traces and operator feedback loops.",
      "modelRiskStatus": "Requires hallucination, spend, and approval review.",
      "approvalBoundary": "AI may draft recommendations; a human approves customer-facing or spend-affecting changes.",
      "proofCommand": "node --test tools/strategy/ai-commerce-bets-evidence.test.mjs",
      "rollback": "Disable AI recommendation entry point and keep manual workflows."
    },
    {
      "key": "ai-native-operations",
      "owner": "Operations Product",
      "status": "Needs Evidence",
      "customerEvidence": ["Ops automation demand must be validated with support and success teams."],
      "dependencyStatus": "Requires stable audit logs and execution timeline evidence.",
      "modelRiskStatus": "Requires bounded action catalog and auditability.",
      "approvalBoundary": "AI cannot trigger production operations without operator confirmation.",
      "proofCommand": "node --test tools/strategy/ai-commerce-bets-evidence.test.mjs",
      "rollback": "Keep automation recommendations read-only."
    },
    {
      "key": "commerce-expansion",
      "owner": "Commerce Product",
      "status": "Deferred",
      "customerEvidence": ["Commerce workflows need industry and billing evidence first."],
      "dependencyStatus": "Depends on commercial model and partner ecosystem decisions.",
      "modelRiskStatus": "No model automation approved for transaction-affecting decisions.",
      "approvalBoundary": "Human approval required for pricing, offer, or purchase-flow changes.",
      "proofCommand": "node --test tools/strategy/ai-commerce-bets-evidence.test.mjs",
      "rollback": "Keep commerce integrations out of product scope."
    },
    {
      "key": "industry-packaging",
      "owner": "Industry Product",
      "status": "Needs Evidence",
      "customerEvidence": ["Vertical package demand must be ranked by revenue and implementation cost."],
      "dependencyStatus": "Requires template library and best-practice evidence.",
      "modelRiskStatus": "AI-generated vertical guidance requires review before publishing.",
      "approvalBoundary": "Industry content must be reviewed by product owner before customer release.",
      "proofCommand": "node --test tools/strategy/ai-commerce-bets-evidence.test.mjs",
      "rollback": "Keep vertical assets as internal discovery notes."
    },
    {
      "key": "globalization",
      "owner": "International Product",
      "status": "Deferred",
      "customerEvidence": ["Regional demand and localization cost are not yet ranked."],
      "dependencyStatus": "Requires privacy, data residency, and localization decisions.",
      "modelRiskStatus": "AI translation requires human review for customer-visible text.",
      "approvalBoundary": "Human reviewer approves locale-specific content before release.",
      "proofCommand": "node --test tools/strategy/ai-commerce-bets-evidence.test.mjs",
      "rollback": "Do not expose unsupported locales."
    },
    {
      "key": "privacy-readiness",
      "owner": "Privacy",
      "status": "Needs Evidence",
      "customerEvidence": ["Privacy expectations must be tied to target regions and enterprise needs."],
      "dependencyStatus": "Requires legal owner and data inventory.",
      "modelRiskStatus": "AI processing must not expand data use without privacy review.",
      "approvalBoundary": "Privacy owner approves data-use changes before child specs.",
      "proofCommand": "node --test tools/strategy/ai-commerce-bets-evidence.test.mjs",
      "rollback": "Block AI and regional features that expand regulated data use."
    },
    {
      "key": "ecosystem-program",
      "owner": "Partnerships",
      "status": "Deferred",
      "customerEvidence": ["Partner demand depends on marketplace and support model evidence."],
      "dependencyStatus": "Depends on P3-001 marketplace governance.",
      "modelRiskStatus": "Partner-facing AI claims require legal and product review.",
      "approvalBoundary": "Partnership owner approves public claims and partner obligations.",
      "proofCommand": "node --test tools/strategy/ai-commerce-bets-evidence.test.mjs",
      "rollback": "Keep partner program in private discovery."
    }
  ]
}
```

- [ ] **Step 2: Create README and governance docs**

Create `docs/product-evolution/discovery/p3-002-ai-commerce-bets/README.md`:

```markdown
# P3-002 AI Commerce Bets Discovery

This package ranks long-term AI and ecosystem bets. It ships no model integration, customer-facing automation, billing behavior, globalization behavior, privacy workflow, or partner feature.

## Verification

Run:

```bash
node --test tools/strategy/ai-commerce-bets-evidence.test.mjs
node tools/strategy/ai-commerce-bets-evidence.mjs
```

Expected: both commands pass and the validator prints all bet keys.
```

Create `docs/product-evolution/discovery/p3-002-ai-commerce-bets/governance-policy.md`:

```markdown
# AI Commerce Bets Governance Policy

- AI may recommend actions in discovery packages, but customer-facing, spend-affecting, privacy-affecting, and partner-facing changes require human approval.
- A bet reaches `Accepted For Child Spec` only with customer evidence, dependency readiness, model-risk review, approval boundary, proof command, rollback path, and child spec path.
- This slice has no Flyway migration and no runtime behavior; rollback is reverting or amending the discovery package.
```

- [ ] **Step 3: Create decision log**

Create `docs/product-evolution/discovery/p3-002-ai-commerce-bets/decision-log.md`:

```markdown
# AI Commerce Bets Decision Log

| Bet | Status | Reason |
| --- | --- | --- |
| AI agent assistance | Needs Evidence | Recommendation demand exists as a hypothesis, but approval and model-risk gates remain open. |
| AI-native operations | Needs Evidence | Operational action catalog and audit evidence are required first. |
| Commerce expansion | Deferred | Commercial model and partner dependencies are not accepted. |
| Industry packaging | Needs Evidence | Vertical demand and content review ownership are required. |
| Globalization | Deferred | Localization, privacy, and data residency decisions are not accepted. |
| Privacy readiness | Needs Evidence | Legal owner and data inventory are required. |
| Ecosystem program | Deferred | Marketplace governance must come first. |
```

- [ ] **Step 4: Run validator tests**

Run:

```bash
node --test tools/strategy/ai-commerce-bets-evidence.test.mjs
node tools/strategy/ai-commerce-bets-evidence.mjs
```

Expected: PASS, and the validator prints all seven bet keys.

### Task 4: Rollout Notes And Commit

**Files:**
- Modify: `docs/product-evolution/specs/p3-002-long-term-ai-commerce-and-ecosystem-bets.md`
- Modify: `docs/product-evolution/plans/p3-002-long-term-ai-commerce-and-ecosystem-bets-plan.md`

- [ ] **Step 1: Confirm no migration references remain**

Run:

```bash
pattern="$(node -e "console.log('db/' + 'migration|__' + '[a-z0-9_]+' + '[.]sql')")"
rg -n "$pattern" docs/product-evolution/specs/p3-002-long-term-ai-commerce-and-ecosystem-bets.md docs/product-evolution/plans/p3-002-long-term-ai-commerce-and-ecosystem-bets-plan.md
```

Expected: no output.

- [ ] **Step 2: Scan for implementation-deferral wording**

Run:

```bash
node - <<'EOF'
const fs = require('node:fs')
const files = [
  'docs/product-evolution/specs/p3-002-long-term-ai-commerce-and-ecosystem-bets.md',
  'docs/product-evolution/plans/p3-002-long-term-ai-commerce-and-ecosystem-bets-plan.md',
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
git add docs/product-evolution/discovery/p3-002-ai-commerce-bets tools/strategy/ai-commerce-bets-evidence.mjs tools/strategy/ai-commerce-bets-evidence.test.mjs docs/product-evolution/specs/p3-002-long-term-ai-commerce-and-ecosystem-bets.md docs/product-evolution/plans/p3-002-long-term-ai-commerce-and-ecosystem-bets-plan.md
git commit -m "docs: add ai commerce bet evidence gates"
```

Expected: commit contains only the P3-002 discovery package, validator, test, spec, and plan.
