import test from 'node:test'
import assert from 'node:assert/strict'
import { computeVerdict, parseTabularCount } from './verifier.mjs'

test('computeVerdict passes when all normal ledgers align', () => {
  const verdict = computeVerdict({
    planned: 100,
    sentSuccess: 100,
    accepted: 100,
    expectedExecutions: 100,
    actualExecutions: 100,
    success: 100,
    failedWithRecord: 0,
    dlq: 0,
    rejectedWithRecord: 0,
    retryPending: 0,
    duplicateExecution: 0,
    unexpectedDedup: 0,
    ackWithoutLedger: 0,
  })

  assert.equal(verdict.verdict, 'PASS')
  assert.equal(verdict.unexpectedLoss, 0)
})

test('computeVerdict fails on unexpected loss', () => {
  const verdict = computeVerdict({
    planned: 100,
    sentSuccess: 100,
    accepted: 100,
    expectedExecutions: 100,
    actualExecutions: 99,
    success: 99,
    failedWithRecord: 0,
    dlq: 0,
    rejectedWithRecord: 0,
    retryPending: 0,
    duplicateExecution: 0,
    unexpectedDedup: 0,
    ackWithoutLedger: 0,
  })

  assert.equal(verdict.verdict, 'FAIL')
  assert.equal(verdict.unexpectedLoss, 1)
})

test('computeVerdict fails on duplicate execution when actual exceeds expected', () => {
  const verdict = computeVerdict({
    planned: 100,
    sentSuccess: 100,
    accepted: 100,
    expectedExecutions: 100,
    actualExecutions: 101,
    success: 101,
    failedWithRecord: 0,
    dlq: 0,
    rejectedWithRecord: 0,
    retryPending: 0,
    unexpectedDedup: 0,
    ackWithoutLedger: 0,
  })

  assert.equal(verdict.verdict, 'FAIL')
  assert.equal(verdict.duplicateExecution, 1)
})

test('computeVerdict fails while retry backlog remains unfinished', () => {
  const verdict = computeVerdict({
    planned: 100,
    sentSuccess: 100,
    accepted: 100,
    expectedExecutions: 100,
    actualExecutions: 99,
    success: 99,
    failedWithRecord: 0,
    dlq: 0,
    rejectedWithRecord: 0,
    retryPending: 1,
    duplicateExecution: 0,
    unexpectedDedup: 0,
    ackWithoutLedger: 0,
  })

  assert.equal(verdict.verdict, 'FAIL')
  assert.equal(verdict.unexpectedLoss, 0)
})

test('computeVerdict fails on audience count mismatch', () => {
  const verdict = computeVerdict({
    planned: 1,
    sentSuccess: 1,
    accepted: 1,
    expectedExecutions: 1,
    actualExecutions: 1,
    success: 1,
    failedWithRecord: 0,
    dlq: 0,
    rejectedWithRecord: 0,
    retryPending: 0,
    duplicateExecution: 0,
    unexpectedDedup: 0,
    ackWithoutLedger: 0,
    wrongAudienceCount: 1,
  })

  assert.equal(verdict.verdict, 'FAIL')
})

test('parseTabularCount reads mysql batch output', () => {
  assert.equal(parseTabularCount('count\n42\n'), 42)
})

test('parseTabularCount returns zero for empty output', () => {
  assert.equal(parseTabularCount(''), 0)
})
