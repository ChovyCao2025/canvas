import test from 'node:test'
import assert from 'node:assert/strict'
import {
  computeVerdict,
  evaluateTraceEvidence,
  expectedTraceCount,
  duplicateInputSqlForMode,
  expectedExecutionCount,
  parseTraceExpectation,
  parseTabularCount,
  parseVerifierArgs,
  verify,
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

test('parseVerifierArgs collects repeated trace expectations', () => {
  const args = parseVerifierArgs([
    '--perf-run-id', 'perf_20260523_001',
    '--expect-trace', 'event:success=all',
    '--expect-trace', 'odd_branch:skipped=even',
  ])

  assert.deepEqual(args.traceExpectations, [
    { nodeId: 'event', status: 1, statusName: 'success', expected: 'all' },
    { nodeId: 'odd_branch', status: 3, statusName: 'skipped', expected: 'even' },
  ])
})

test('parseTraceExpectation rejects invalid expectation syntax', () => {
  assert.throws(() => parseTraceExpectation('event-success-all'), /expected NODE:STATUS=COUNT/)
  assert.throws(() => parseTraceExpectation('event:unknown=all'), /trace status must be/)
})

test('expectedTraceCount supports all even odd and none tokens', () => {
  const args = {
    mode: 'event',
    sentSuccess: 10,
    matchedCanvasCount: 2,
    intentionalDuplicates: 0,
  }

  assert.equal(expectedTraceCount('all', args), 20)
  assert.equal(expectedTraceCount('even', args), 10)
  assert.equal(expectedTraceCount('odd', args), 10)
  assert.equal(expectedTraceCount('none', args), 0)
  assert.equal(expectedTraceCount(7, args), 7)
})

test('evaluateTraceEvidence fails on mismatched node counts and duplicate successes', () => {
  const evidence = evaluateTraceEvidence({
    traceExpectations: [
      { nodeId: 'event', status: 1, statusName: 'success', expected: 'all' },
    ],
    traceCounts: [
      { nodeId: 'event', status: 1, actual: 99, expected: 100 },
    ],
    traceFailed: 0,
    traceDuplicateSuccess: 1,
    traceBufferPending: 0,
  })

  assert.equal(evidence.traceMismatch, 1)
  assert.equal(evidence.traceDuplicateSuccess, 1)
  assert.equal(evidence.traceVerdict, 'FAIL')
})

test('computeVerdict fails when trace evidence fails', () => {
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
    traceMismatch: 1,
    traceFailed: 0,
    traceDuplicateSuccess: 0,
    traceBufferPending: 0,
  })

  assert.equal(verdict.verdict, 'FAIL')
})

test('verify includes trace evidence and fails on trace count mismatch', () => {
  const queryCount = ({ sql }) => {
    if (sql.includes('FROM event_log')) return 100
    if (sql.includes('FROM canvas_execution_request') && sql.includes("status IN ('PENDING','RETRY','RUNNING')")) return 0
    if (sql.includes('FROM canvas_execution_request') && sql.includes("status = 'FAILED'")) return 0
    if (sql.includes('COUNT(DISTINCT source_msg_id)')) return 0
    if (sql.includes('FROM canvas_execution_request')) return 0
    if (sql.includes('FROM canvas_execution_dlq')) return 0
    if (sql.includes('FROM audience_compute_run')) return 0
    if (sql.includes('duplicate_trace_success')) return 0
    if (sql.includes('failed_trace')) return 0
    if (sql.includes("t.node_id = 'event'") && sql.includes('t.status = 1')) return 99
    if (sql.includes('status = 2')) return 0
    if (sql.includes('status = 3')) return 0
    if (sql.includes('FROM canvas_execution WHERE') && sql.includes('status = 2')) return 100
    if (sql.includes('FROM canvas_execution WHERE') && sql.includes('status = 3')) return 0
    if (sql.includes('FROM canvas_execution WHERE')) return 100
    return 0
  }

  const result = verify({
    mode: 'event',
    perfRunId: 'perf_trace_001',
    sentSuccess: 100,
    matchedCanvasCount: 1,
    traceExpectations: [
      { nodeId: 'event', status: 1, statusName: 'success', expected: 'all' },
    ],
    intentionalDuplicates: 0,
    expectedFailedWithRecord: 0,
    expectedRejectedWithRecord: 0,
    expectedDlq: 0,
    audienceId: 0,
    expectedAudienceCount: -1,
  }, { queryCount })

  assert.equal(result.traceMismatch, 1)
  assert.equal(result.traceCounts[0].expected, 100)
  assert.equal(result.traceCounts[0].actual, 99)
  assert.equal(result.verdict, 'FAIL')
})

test('duplicateInputSqlForMode groups available unique input keys', () => {
  assert.match(duplicateInputSqlForMode('event'), /perfInputId/)
  assert.match(duplicateInputSqlForMode('mq'), /source_msg_id/)
  assert.match(duplicateInputSqlForMode('direct'), /last_dedup_key/)
})

test('expectedExecutionCount subtracts intentional duplicates', () => {
  assert.equal(expectedExecutionCount({
    mode: 'direct',
    sentSuccess: 100,
    intentionalDuplicates: 2,
    matchedCanvasCount: 1,
  }), 98)
})

test('duplicateInputSqlForMode groups audience perf input ids', () => {
  assert.match(duplicateInputSqlForMode('audience'), /audience_compute_run/)
  assert.match(duplicateInputSqlForMode('audience'), /perf_input_id/)
})
