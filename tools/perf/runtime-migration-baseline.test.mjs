import test from 'node:test'
import assert from 'node:assert/strict'
import { mkdtempSync } from 'node:fs'
import { tmpdir } from 'node:os'
import { join } from 'node:path'

import {
  REQUIRED_CANDIDATES,
  buildReport,
  validateReport,
} from './runtime-migration-baseline.mjs'

test('printsMachineReadableJson', () => {
  const report = buildReport({ generatedAt: '2026-06-04T00:00:00.000Z' })
  const parsed = JSON.parse(JSON.stringify(report))

  assert.equal(parsed.generatedAt, '2026-06-04T00:00:00.000Z')
  assert.equal(parsed.summary.total, REQUIRED_CANDIDATES.length)
})

test('requiresCandidateKeys', () => {
  const report = buildReport()
  const keys = report.candidates.map(candidate => candidate.key)

  assert.deepEqual(keys, REQUIRED_CANDIDATES)
})

test('includesProofCommandOutputFields', () => {
  const report = buildReport()

  for (const candidate of report.candidates) {
    assert.ok(candidate.sourceEvidence.length > 0)
    assert.ok(candidate.proofCommands[0].command)
    assert.ok(candidate.riskLevel)
    assert.ok(candidate.dependencyStatus)
    assert.ok(candidate.decisionStatus)
    assert.ok(candidate.rollbackNote)
  }
})

test('rejectsMissingMetrics', () => {
  const report = buildReport()
  report.candidates[0].proofCommands = []

  assert.throws(() => validateReport(report), /missing proof command/)
})

test('exitsNonZeroWhenSourceEvidenceIsUnavailable', () => {
  const emptyRoot = mkdtempSync(join(tmpdir(), 'runtime-migration-empty-'))
  const report = buildReport({ root: emptyRoot })

  assert.throws(() => validateReport(report), /source evidence unavailable/)
})
