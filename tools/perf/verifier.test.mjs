import test from 'node:test'
import assert from 'node:assert/strict'
import {
  computeVerdict,
  duplicateInputSqlForMode,
  parseTabularCount,
  parseVerifierArgs,
} from './verifier.mjs'

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

test('computeVerdict fails on unplanned failed execution records', () => {
  const verdict = computeVerdict({
    planned: 100,
    sentSuccess: 100,
    accepted: 100,
    expectedExecutions: 100,
    actualExecutions: 100,
    success: 99,
    failedWithRecord: 1,
    dlq: 0,
    rejectedWithRecord: 0,
    retryPending: 0,
    duplicateExecution: 0,
    unexpectedDedup: 0,
    ackWithoutLedger: 0,
  })

  assert.equal(verdict.verdict, 'FAIL')
  assert.equal(verdict.unexpectedFailedWithRecord, 1)
})

test('computeVerdict fails on unplanned rejected request records', () => {
  const verdict = computeVerdict({
    planned: 100,
    sentSuccess: 100,
    accepted: 100,
    expectedExecutions: 100,
    actualExecutions: 99,
    success: 99,
    failedWithRecord: 0,
    dlq: 0,
    rejectedWithRecord: 1,
    retryPending: 0,
    duplicateExecution: 0,
    unexpectedDedup: 0,
    ackWithoutLedger: 0,
  })

  assert.equal(verdict.verdict, 'FAIL')
  assert.equal(verdict.unexpectedRejectedWithRecord, 1)
})

test('computeVerdict reports expected failures separately', () => {
  const verdict = computeVerdict({
    planned: 100,
    sentSuccess: 100,
    accepted: 100,
    expectedExecutions: 100,
    actualExecutions: 100,
    success: 99,
    failedWithRecord: 1,
    expectedFailedWithRecord: 1,
    dlq: 0,
    rejectedWithRecord: 0,
    retryPending: 0,
    duplicateExecution: 0,
    unexpectedDedup: 0,
    ackWithoutLedger: 0,
  })

  assert.equal(verdict.verdict, 'PASS_WITH_EXPECTED_FAILURES')
  assert.equal(verdict.unexpectedFailedWithRecord, 0)
})

test('computeVerdict accounts for intentional duplicates before flagging bad dedup', () => {
  const verdict = computeVerdict({
    planned: 100,
    sentSuccess: 100,
    accepted: 98,
    expectedExecutions: 98,
    actualExecutions: 98,
    success: 98,
    failedWithRecord: 0,
    dlq: 0,
    rejectedWithRecord: 0,
    retryPending: 0,
    duplicateExecution: 0,
    intentionalDuplicates: 1,
  })

  assert.equal(verdict.verdict, 'FAIL')
  assert.equal(verdict.unexpectedDedup, 1)
  assert.equal(verdict.ackWithoutLedger, 1)
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

test('parseVerifierArgs rejects explicit zero audience id', () => {
  assert.throws(() => parseVerifierArgs([
    '--perf-run-id', 'perf_20260523_001',
    '--audience-id', '0',
  ]), /--audience-id must be a positive integer/)
})

test('duplicateInputSqlForMode groups available unique input keys', () => {
  assert.match(duplicateInputSqlForMode('event'), /perfInputId/)
  assert.match(duplicateInputSqlForMode('mq'), /source_msg_id/)
  assert.match(duplicateInputSqlForMode('direct'), /last_dedup_key/)
})
