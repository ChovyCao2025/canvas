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

function writePayload(payload) {
  const dir = mkdtempSync(path.join(tmpdir(), 'plugin-marketplace-'))
  const file = path.join(dir, 'evidence.json')
  writeFileSync(file, JSON.stringify(payload))
  return file
}

function validPayload(overrides = {}) {
  return {
    package: 'p3-001-plugin-marketplace',
    rollout: { migration: 'none', runtimeChange: false },
    candidates: [{
      key: 'plugin-submission',
      owner: 'Platform Product',
      status: 'Needs Evidence',
      evidence: ['Internal plugin foundations define extension boundaries.'],
      proofCommand: 'node --test tools/strategy/plugin-marketplace-evidence.test.mjs',
      launchGate: 'Security owner approves package review checklist.',
      rollback: 'Keep marketplace entry points disabled.',
      dependencies: ['P2-002 plugin foundations'],
      ...overrides,
    }],
  }
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
    'plugin-takedown',
  ])
})

test('rejects missing required evidence fields', () => {
  for (const field of ['owner', 'proofCommand', 'launchGate', 'rollback']) {
    const payload = validPayload({ [field]: '' })
    assert.throws(() => run(writePayload(payload)), new RegExp(`${field} is required`))
  }
})

test('rejects launch gates that do not contain required topic evidence', () => {
  for (const [key, launchGate, message] of [
    ['security-review', 'Generic owner approves launch.', /security-review launchGate must mention security/],
    ['partner-support', 'Generic owner approves launch.', /partner-support launchGate must mention support/],
    ['plugin-takedown', 'Generic owner approves launch.', /plugin-takedown launchGate must mention security/],
  ]) {
    const payload = validPayload({ key, launchGate })
    assert.throws(() => run(writePayload(payload)), message)
  }
})

test('rejects accepted capabilities without child spec path', () => {
  const payload = validPayload({ status: 'Accepted For Child Spec' })

  assert.throws(() => run(writePayload(payload)), /childSpecPath is required/)
})

test('rejects accepted capabilities without internal plugin foundation dependency', () => {
  const payload = validPayload({
    status: 'Accepted For Child Spec',
    childSpecPath: 'docs/product-evolution/specs/p3-001a-plugin-submission.md',
    dependencies: ['security-review'],
  })

  assert.throws(() => run(writePayload(payload)), /P2-002 plugin foundations dependency is required/)
})
