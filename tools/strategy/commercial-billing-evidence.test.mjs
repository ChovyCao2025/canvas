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

function writePayload(payload) {
  const dir = mkdtempSync(path.join(tmpdir(), 'commercial-billing-'))
  const file = path.join(dir, 'evidence.json')
  writeFileSync(file, JSON.stringify(payload))
  return file
}

function validPayload(overrides = {}) {
  return {
    package: 'p3-004-commercial-billing',
    rollout: { migration: 'none', runtimeChange: false, customerCharging: false },
    capabilities: [{
      key: 'billable-metrics',
      owner: 'Commercial Product',
      status: 'Needs Evidence',
      metricDefinition: 'Monthly billable executions.',
      sourceEvidence: ['Execution history can be counted by tenant after analytics source is accepted.'],
      financeGate: 'Finance approves metric language.',
      legalGate: 'Legal approves metric language.',
      supportGate: 'Support approves dispute process.',
      proofCommand: 'node --test tools/strategy/commercial-billing-evidence.test.mjs',
      rollback: 'Do not expose customer billing.',
      ...overrides,
    }],
  }
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
    'upgrade-recommendations',
  ])
})

test('rejects accepted billing capabilities without required gates', () => {
  for (const field of ['owner', 'metricDefinition', 'sourceEvidence', 'financeGate', 'legalGate', 'supportGate', 'proofCommand', 'rollback']) {
    const value = field === 'sourceEvidence' ? [] : ''
    const payload = validPayload({
      status: 'Accepted For Child Spec',
      childSpecPath: 'docs/product-evolution/specs/p3-child-billable-metrics.md',
      [field]: value,
    })
    assert.throws(() => run(writePayload(payload)), new RegExp(`${field} is required`))
  }
})

test('rejects accepted billing capabilities without child spec path', () => {
  const payload = validPayload({ status: 'Accepted For Child Spec' })

  assert.throws(() => run(writePayload(payload)), /childSpecPath is required/)
})

test('rejects rollout that implies customer charging', () => {
  const payload = validPayload()
  payload.rollout.customerCharging = true

  assert.throws(() => run(writePayload(payload)), /rollout.customerCharging must be false/)
})
