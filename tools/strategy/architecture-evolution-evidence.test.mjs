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

function writePayload(payload) {
  const dir = mkdtempSync(path.join(tmpdir(), 'architecture-evolution-'))
  const file = path.join(dir, 'evidence.json')
  writeFileSync(file, JSON.stringify(payload))
  return file
}

function validPayload(overrides = {}) {
  return {
    package: 'p3-003-architecture-evolution',
    rollout: { migration: 'none', runtimeChange: false },
    candidates: [{
      key: 'service-split',
      owner: 'Architecture',
      status: 'Needs Evidence',
      currentCodeEvidence: ['backend/canvas-engine is currently one deployable application.'],
      scaleTrigger: 'Sustained module ownership or runtime scaling pressure.',
      proofCommand: 'node --test tools/strategy/architecture-evolution-evidence.test.mjs',
      compatibility: 'Single artifact rollback remains available.',
      rollback: 'Deploy monolith artifact.',
      dependencyStatus: 'P0/P1 safety gates complete.',
      ...overrides,
    }],
  }
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
    'data-residency',
  ])
})

test('rejects accepted architecture candidates without current code evidence', () => {
  const payload = validPayload({
    status: 'Accepted For Child Spec',
    childSpecPath: 'docs/product-evolution/specs/p3-child-service-split.md',
    currentCodeEvidence: [],
  })

  assert.throws(() => run(writePayload(payload)), /currentCodeEvidence is required/)
})

test('rejects accepted architecture candidates without proof rollback dependency or child spec', () => {
  const payload = validPayload({
    status: 'Accepted For Child Spec',
    proofCommand: '',
    rollback: '',
    dependencyStatus: '',
  })

  assert.throws(
    () => run(writePayload(payload)),
    /proofCommand is required[\s\S]*rollback is required[\s\S]*dependencyStatus is required[\s\S]*childSpecPath is required/,
  )
})
