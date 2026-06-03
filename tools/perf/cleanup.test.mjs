import test from 'node:test'
import assert from 'node:assert/strict'
import { buildCleanupSql, escapeSql, parseCleanupArgs } from './cleanup.mjs'

test('escapeSql doubles single quotes and backslashes', () => {
  assert.equal(escapeSql("perf'\\1"), "perf''\\\\1")
})

test('buildCleanupSql with ledger scope preserves PERF namespace rows', () => {
  const sql = buildCleanupSql('perf_20260523_001', { scope: 'ledger' })

  assert.match(sql, /canvas_execution_trace/)
  assert.match(sql, /audience_compute_run/)
  assert.match(sql, /perf_run_id = 'perf_20260523_001'/)
  assert.doesNotMatch(sql, /DELETE FROM event_definition WHERE event_code LIKE 'PERF_%'/)
  assert.doesNotMatch(sql, /DELETE FROM mq_message_definition WHERE message_code LIKE 'PERF_%'/)
  assert.doesNotMatch(sql, /DELETE FROM canvas;$/m)
  assert.doesNotMatch(sql, /DELETE FROM canvas_definition;$/m)
})

test('buildCleanupSql with all scope removes PERF namespace rows', () => {
  const sql = buildCleanupSql('perf_20260523_001', { scope: 'all' })

  assert.match(sql, /DELETE FROM event_definition WHERE event_code LIKE 'PERF_%'/)
  assert.match(sql, /DELETE FROM mq_message_definition WHERE message_code LIKE 'PERF_%'/)
})

test('buildCleanupSql prints counts before and after cleanup', () => {
  const sql = buildCleanupSql('perf_20260523_001')

  assert.match(sql, /before_event_log_rows/)
  assert.match(sql, /before_audience_compute_run_rows/)
  assert.match(sql, /after_event_log_rows/)
  assert.match(sql, /after_audience_compute_run_rows/)
})

test('buildCleanupSql preserves execution ids for post-cleanup trace count', () => {
  const sql = buildCleanupSql('perf_20260523_001')
  const traceDeleteIndex = sql.indexOf('DELETE FROM canvas_execution_trace')
  const executionDeleteIndex = sql.indexOf('DELETE FROM canvas_execution WHERE')
  const afterTraceCountIndex = sql.indexOf('after_execution_trace_rows')

  assert.match(sql, /CREATE TEMPORARY TABLE perf_cleanup_execution_ids/)
  assert.match(sql, /FROM perf_cleanup_execution_ids/)
  assert.ok(traceDeleteIndex > -1)
  assert.ok(executionDeleteIndex > traceDeleteIndex)
  assert.ok(afterTraceCountIndex > executionDeleteIndex)
})

test('parseCleanupArgs defaults to ledger scope dry run', () => {
  const args = parseCleanupArgs([
    '--perf-run-id', 'perf_20260523_001',
  ])

  assert.equal(args.mysql, 'mysql')
  assert.equal(args.database, 'canvas_db')
  assert.equal(args.scope, 'ledger')
  assert.equal(args.execute, false)
})

test('parseCleanupArgs accepts all scope', () => {
  const args = parseCleanupArgs([
    '--perf-run-id', 'perf_20260523_001',
    '--scope', 'all',
  ])

  assert.equal(args.scope, 'all')
})

test('parseCleanupArgs rejects unsupported scope', () => {
  assert.throws(() => parseCleanupArgs([
    '--perf-run-id', 'perf_20260523_001',
    '--scope', 'fixture',
  ]), /--scope must be ledger or all/)
})

test('parseCleanupArgs requires explicit true to execute', () => {
  const args = parseCleanupArgs([
    '--perf-run-id', 'perf_20260523_001',
    '--execute', 'true',
  ])

  assert.equal(args.execute, true)
})

test('parseCleanupArgs rejects missing perf run id', () => {
  assert.throws(() => parseCleanupArgs([]), /--perf-run-id is required/)
})

test('parseCleanupArgs rejects unknown flags', () => {
  assert.throws(() => parseCleanupArgs([
    '--perf-run-id', 'perf_20260523_001',
    '--unknown', '1',
  ]), /Unknown flag: --unknown/)
})
