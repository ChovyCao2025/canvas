import assert from 'node:assert/strict'
import { execFileSync } from 'node:child_process'
import { mkdtempSync, writeFileSync } from 'node:fs'
import { tmpdir } from 'node:os'
import path from 'node:path'
import test from 'node:test'

const script = path.resolve('tools/strategy/quickbi-benchmark-audit.mjs')
const evidence = path.resolve('docs/superpowers/evidence/quickbi-capability-benchmark.json')

function run(file = evidence) {
  return execFileSync(process.execPath, [script, file], {
    encoding: 'utf8',
    stdio: ['ignore', 'pipe', 'pipe'],
  })
}

function writePayload(payload) {
  const dir = mkdtempSync(path.join(tmpdir(), 'quickbi-benchmark-'))
  const file = path.join(dir, 'benchmark.json')
  writeFileSync(file, JSON.stringify(payload))
  return file
}

function validPayload(overrides = {}) {
  return {
    package: 'quickbi-platform-benchmark',
    coverageThresholdPercent: 90,
    officialSources: [
      'https://www.aliyun.com/product/quick-bi',
      'https://help.aliyun.com/zh/quick-bi/product-overview/introduction-to-quick-bi-1',
      'https://help.aliyun.com/zh/quick-bi/user-guide/features-supported-by-different-data-sources',
    ],
    capabilities: [
      {
        key: 'dashboard-authoring',
        officialCapability: 'Dashboard authoring',
        canvasEvidence: ['frontend/src/pages/bi/index.tsx'],
        status: 'implemented',
        weight: 5,
      },
      {
        key: 'mobile-app',
        officialCapability: 'Mobile app access',
        canvasEvidence: ['docs/superpowers/specs/2026-06-05-quickbi-platform-design.md'],
        status: 'planned',
        weight: 5,
      },
    ],
    ...overrides,
  }
}

test('validates committed QuickBI benchmark evidence over 90 percent coverage', () => {
  const output = JSON.parse(run())

  assert.equal(output.ok, true)
  assert.equal(output.package, 'quickbi-platform-benchmark')
  assert.equal(output.thresholdPercent, 90)
  assert.equal(output.passesThreshold, true)
  assert.ok(output.coveragePercent >= 90)
  assert.ok(output.capabilityCount >= 20)
  assert.deepEqual(output.remainingStatuses, ['partial', 'planned'])
})

test('rejects evidence below the requested 90 percent threshold', () => {
  const payload = validPayload()

  assert.throws(() => run(writePayload(payload)), /coverage .* is below threshold 90/)
})

test('rejects capabilities without current evidence', () => {
  const payload = validPayload({
    capabilities: [
      {
        key: 'dashboard-authoring',
        officialCapability: 'Dashboard authoring',
        canvasEvidence: [],
        status: 'implemented',
        weight: 10,
      },
    ],
  })

  assert.throws(() => run(writePayload(payload)), /dashboard-authoring: canvasEvidence is required/)
})
