import test from 'node:test'
import assert from 'node:assert/strict'
import { buildCleanupSql, escapeSql, parseCleanupArgs } from './cleanup.mjs'

test('escapeSql doubles single quotes and backslashes', () => {
  assert.equal(escapeSql("perf'\\1"), "perf''\\\\1")
})

test('buildCleanupSql deletes only perf run and PERF namespace rows', () => {
  const sql = buildCleanupSql('perf_20260523_001')

  assert.match(sql, /canvas_execution_trace/)
  assert.match(sql, /perf_run_id = 'perf_20260523_001'/)
  assert.match(sql, /event_code LIKE 'PERF_%'/)
  assert.match(sql, /message_code LIKE 'PERF_%'/)
  assert.doesNotMatch(sql, /DELETE FROM canvas;$/m)
  assert.doesNotMatch(sql, /DELETE FROM canvas_definition;$/m)
})

test('buildCleanupSql prints counts before and after cleanup', () => {
  const sql = buildCleanupSql('perf_20260523_001')

  assert.match(sql, /before_event_log_rows/)
  assert.match(sql, /after_event_log_rows/)
})

test('parseCleanupArgs defaults to dry run', () => {
  const args = parseCleanupArgs([
    '--perf-run-id', 'perf_20260523_001',
  ])

  assert.equal(args.mysql, 'mysql')
  assert.equal(args.database, 'canvas_db')
  assert.equal(args.execute, false)
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
