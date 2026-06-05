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

function writePayload(payload) {
  const dir = mkdtempSync(path.join(tmpdir(), 'ai-commerce-bets-'))
  const file = path.join(dir, 'evidence.json')
  writeFileSync(file, JSON.stringify(payload))
  return file
}

function validPayload(overrides = {}) {
  return {
    package: 'p3-002-ai-commerce-bets',
    rollout: { migration: 'none', runtimeChange: false },
    bets: [{
      key: 'ai-agent-assistance',
      owner: 'AI Product',
      status: 'Needs Evidence',
      customerEvidence: ['Operators ask for guided campaign optimization.'],
      dependencyStatus: 'P2 analytics evidence required.',
      modelRiskStatus: 'Requires review before action automation.',
      approvalBoundary: 'Human approves customer-facing or spend-affecting changes.',
      proofCommand: 'node --test tools/strategy/ai-commerce-bets-evidence.test.mjs',
      rollback: 'Keep AI suggestions hidden behind internal review.',
      ...overrides,
    }],
  }
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
    'ecosystem-program',
  ])
})

test('rejects promoted bets with missing required gates', () => {
  for (const field of ['owner', 'approvalBoundary', 'proofCommand', 'rollback']) {
    const payload = validPayload({ status: 'Accepted For Child Spec', childSpecPath: 'docs/product-evolution/specs/p3-child-ai-agent-assistance.md', [field]: '' })
    assert.throws(() => run(writePayload(payload)), new RegExp(`${field} is required`))
  }
})

test('rejects accepted bets without child spec path', () => {
  const payload = validPayload({ status: 'Accepted For Child Spec' })

  assert.throws(() => run(writePayload(payload)), /childSpecPath is required/)
})

test('rejects missing demand evidence and model risk status', () => {
  const payload = validPayload({
    customerEvidence: [],
    modelRiskStatus: '',
  })

  assert.throws(() => run(writePayload(payload)), /customerEvidence is required[\s\S]*modelRiskStatus is required/)
})
