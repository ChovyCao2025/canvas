#!/usr/bin/env node

import { execFileSync } from 'node:child_process'
import { pathToFileURL } from 'node:url'

const DEFAULT_ARGS = {
  mysql: 'mysql',
  database: 'canvas_db',
  mode: 'event',
  perfRunId: '',
  sentSuccess: 0,
  matchedCanvasCount: 1,
  audienceId: 0,
  expectedAudienceCount: -1,
  intentionalDuplicates: 0,
  expectedFailedWithRecord: 0,
  expectedRejectedWithRecord: 0,
  expectedDlq: 0,
}

const FLAG_NAMES = {
  '--mysql': 'mysql',
  '--database': 'database',
  '--mode': 'mode',
  '--perf-run-id': 'perfRunId',
  '--sent-success': 'sentSuccess',
  '--matched-canvas-count': 'matchedCanvasCount',
  '--audience-id': 'audienceId',
  '--expected-audience-count': 'expectedAudienceCount',
  '--intentional-duplicates': 'intentionalDuplicates',
  '--expected-failed-records': 'expectedFailedWithRecord',
  '--expected-rejected-records': 'expectedRejectedWithRecord',
  '--expected-dlq': 'expectedDlq',
}

const NUMBER_ARGS = new Set([
  'sentSuccess',
  'matchedCanvasCount',
  'audienceId',
  'expectedAudienceCount',
  'intentionalDuplicates',
  'expectedFailedWithRecord',
  'expectedRejectedWithRecord',
  'expectedDlq',
])
const MODES = new Set(['event', 'direct', 'mq', 'audience'])

function parseIntegerArg(flag, value, { allowNegative = false } = {}) {
  const pattern = allowNegative ? /^-?(0|[1-9]\d*)$/ : /^(0|[1-9]\d*)$/
  if (!pattern.test(value)) {
    const kind = allowNegative ? 'an integer' : 'a non-negative integer'
    throw new Error(`${flag} must be ${kind}`)
  }

  const parsed = Number(value)
  if (!Number.isSafeInteger(parsed)) {
    throw new Error(`${flag} must be a safe integer`)
  }
  return parsed
}

function validateArgs(args) {
  if (!MODES.has(args.mode)) {
    throw new Error('--mode must be one of event, direct, mq, audience')
  }

  if (!args.perfRunId) {
    throw new Error('--perf-run-id is required')
  }

  if (args.sentSuccess < 0) {
    throw new Error('--sent-success must be a non-negative integer')
  }

  if (args.matchedCanvasCount < 0) {
    throw new Error('--matched-canvas-count must be a non-negative integer')
  }

  if (args.audienceId < 0) {
    throw new Error('--audience-id must be a non-negative integer')
  }

  if (args.expectedAudienceCount < -1) {
    throw new Error('--expected-audience-count must be >= -1')
  }

  if (args.intentionalDuplicates < 0) {
    throw new Error('--intentional-duplicates must be a non-negative integer')
  }

  if (args.expectedFailedWithRecord < 0) {
    throw new Error('--expected-failed-records must be a non-negative integer')
  }

  if (args.expectedRejectedWithRecord < 0) {
    throw new Error('--expected-rejected-records must be a non-negative integer')
  }

  if (args.expectedDlq < 0) {
    throw new Error('--expected-dlq must be a non-negative integer')
  }
}

export function parseVerifierArgs(argv) {
  const args = { ...DEFAULT_ARGS }

  for (let index = 0; index < argv.length; index += 2) {
    const flag = argv[index]
    const name = FLAG_NAMES[flag]

    if (!name) {
      throw new Error(`Unknown flag: ${flag}`)
    }

    if (index + 1 >= argv.length || argv[index + 1] === '' || argv[index + 1].startsWith('--')) {
      throw new Error(`Missing value for ${flag}`)
    }

    const value = argv[index + 1]
    if (NUMBER_ARGS.has(name)) {
      if (name === 'audienceId') {
        const parsed = parseIntegerArg(flag, value)
        if (parsed <= 0) {
          throw new Error('--audience-id must be a positive integer')
        }
        args[name] = parsed
      } else {
        args[name] = parseIntegerArg(flag, value, { allowNegative: name === 'expectedAudienceCount' })
      }
    } else {
      args[name] = value
    }
  }

  validateArgs(args)
  return args
}

export function parseTabularCount(output) {
  const lines = output.trim().split(/\r?\n/).map((line) => line.trim()).filter(Boolean)
  for (let index = lines.length - 1; index >= 0; index -= 1) {
    const value = lines[index].split(/\t/).at(-1)
    if (/^-?(0|[1-9]\d*)$/.test(value)) {
      return Number(value)
    }
  }
  return 0
}

export function computeVerdict(input) {
  const expectedFailedWithRecord = input.expectedFailedWithRecord || 0
  const expectedRejectedWithRecord = input.expectedRejectedWithRecord || 0
  const expectedDlq = input.expectedDlq || 0
  const intentionalDuplicates = input.intentionalDuplicates || 0
  const duplicateExecution = Math.max(
    input.duplicateExecution ?? 0,
    Math.max(0, input.actualExecutions - input.expectedExecutions),
  )
  const deduplicated = input.deduplicated ?? Math.max(0, input.sentSuccess - input.accepted)
  const unexpectedDedup = Math.max(input.unexpectedDedup || 0, deduplicated - intentionalDuplicates)
  const baseAckWithoutLedger = input.ackWithoutLedger ?? Math.max(0, input.sentSuccess - input.accepted)
  const ackWithoutLedger = Math.max(0, baseAckWithoutLedger - intentionalDuplicates)
  const unexpectedFailedWithRecord = Math.max(0, input.failedWithRecord - expectedFailedWithRecord)
  const unexpectedRejectedWithRecord = Math.max(0, input.rejectedWithRecord - expectedRejectedWithRecord)
  const unexpectedDlq = Math.max(0, input.dlq - expectedDlq)
  const accounted = input.success
    + input.failedWithRecord
    + input.dlq
    + input.rejectedWithRecord
    + input.retryPending
  const unexpectedLoss = Math.max(0, input.expectedExecutions - accounted)
  const wrongAudienceCount = input.wrongAudienceCount || 0
  const failures = [
    unexpectedLoss,
    duplicateExecution,
    unexpectedDedup,
    ackWithoutLedger,
    wrongAudienceCount,
    input.retryPending,
    unexpectedFailedWithRecord,
    unexpectedRejectedWithRecord,
    unexpectedDlq,
  ].filter((value) => value > 0)
  const expectedFailureRecords = Math.min(input.failedWithRecord, expectedFailedWithRecord)
    + Math.min(input.rejectedWithRecord, expectedRejectedWithRecord)
    + Math.min(input.dlq, expectedDlq)

  return {
    ...input,
    intentionalDuplicates,
    deduplicated,
    duplicateExecution,
    unexpectedDedup,
    ackWithoutLedger,
    expectedFailedWithRecord,
    expectedRejectedWithRecord,
    expectedDlq,
    unexpectedFailedWithRecord,
    unexpectedRejectedWithRecord,
    unexpectedDlq,
    unexpectedLoss,
    wrongAudienceCount,
    verdict: failures.length > 0
      ? 'FAIL'
      : expectedFailureRecords > 0
        ? 'PASS_WITH_EXPECTED_FAILURES'
        : 'PASS',
  }
}

function escapeSqlString(value) {
  return String(value).replaceAll('\\', '\\\\').replaceAll("'", "''")
}

function queryCount({ mysql, database, perfRunId, sql }) {
  const output = execFileSync(mysql, [
    '--batch',
    '--raw',
    '-uroot',
    '-proot',
    '-D',
    database,
    '-e',
    sql.replaceAll(':perfRunId', `'${escapeSqlString(perfRunId)}'`),
  ], { encoding: 'utf8' })
  return parseTabularCount(output)
}

export function duplicateInputSqlForMode(mode) {
  if (mode === 'event') {
    return `
SELECT COALESCE(SUM(extra_count), 0) AS count
FROM (
  SELECT GREATEST(COUNT(*) - 1, 0) AS extra_count
  FROM event_log
  WHERE perf_run_id = :perfRunId
    AND JSON_UNQUOTE(JSON_EXTRACT(attributes, '$.perfInputId')) IS NOT NULL
  GROUP BY JSON_UNQUOTE(JSON_EXTRACT(attributes, '$.perfInputId'))
  HAVING COUNT(*) > 1
) duplicate_inputs`
  }

  if (mode === 'mq') {
    return `
SELECT COALESCE(SUM(extra_count), 0) AS count
FROM (
  SELECT GREATEST(COUNT(*) - 1, 0) AS extra_count
  FROM canvas_execution_request
  WHERE perf_run_id = :perfRunId
    AND source_msg_id IS NOT NULL
  GROUP BY source_msg_id
  HAVING COUNT(*) > 1
) duplicate_inputs`
  }

  if (mode === 'direct') {
    return `
SELECT COALESCE(SUM(extra_count), 0) AS count
FROM (
  SELECT GREATEST(COUNT(*) - 1, 0) AS extra_count
  FROM canvas_execution
  WHERE perf_run_id = :perfRunId
    AND last_dedup_key IS NOT NULL
  GROUP BY last_dedup_key
  HAVING COUNT(*) > 1
) duplicate_inputs`
  }

  return 'SELECT 0 AS count'
}

function queryLedgers(args) {
  const eventLog = queryCount({
    ...args,
    sql: 'SELECT COUNT(*) AS count FROM event_log WHERE perf_run_id = :perfRunId',
  })
  const requestRows = queryCount({
    ...args,
    sql: 'SELECT COUNT(*) AS count FROM canvas_execution_request WHERE perf_run_id = :perfRunId',
  })
  const requestSources = queryCount({
    ...args,
    sql: 'SELECT COUNT(DISTINCT source_msg_id) AS count FROM canvas_execution_request WHERE perf_run_id = :perfRunId AND source_msg_id IS NOT NULL',
  })
  const executions = queryCount({
    ...args,
    sql: 'SELECT COUNT(*) AS count FROM canvas_execution WHERE perf_run_id = :perfRunId',
  })
  const success = queryCount({
    ...args,
    sql: 'SELECT COUNT(*) AS count FROM canvas_execution WHERE perf_run_id = :perfRunId AND status = 2',
  })
  const failed = queryCount({
    ...args,
    sql: 'SELECT COUNT(*) AS count FROM canvas_execution WHERE perf_run_id = :perfRunId AND status = 3',
  })
  const dlq = queryCount({
    ...args,
    sql: 'SELECT COUNT(*) AS count FROM canvas_execution_dlq WHERE perf_run_id = :perfRunId',
  })
  const retryPending = queryCount({
    ...args,
    sql: "SELECT COUNT(*) AS count FROM canvas_execution_request WHERE perf_run_id = :perfRunId AND status IN ('PENDING','RETRY','RUNNING')",
  })
  const rejectedWithRecord = queryCount({
    ...args,
    sql: "SELECT COUNT(*) AS count FROM canvas_execution_request WHERE perf_run_id = :perfRunId AND status = 'FAILED'",
  })
  const duplicateInput = queryCount({
    ...args,
    sql: duplicateInputSqlForMode(args.mode),
  })

  return {
    eventLog,
    requestRows,
    requestSources,
    executions,
    success,
    failed,
    dlq,
    retryPending,
    rejectedWithRecord,
    duplicateInput,
  }
}

function acceptedInputCount(args, ledgers) {
  if (args.mode === 'event') {
    return ledgers.eventLog
  }
  if (args.mode === 'mq') {
    return ledgers.requestSources || ledgers.requestRows
  }
  if (args.mode === 'direct') {
    return ledgers.executions
  }
  return args.sentSuccess
}

export function verify(args) {
  const ledgers = queryLedgers(args)
  let wrongAudienceCount = 0

  if (args.audienceId > 0 && args.expectedAudienceCount >= 0) {
    const actualAudienceCount = queryCount({
      ...args,
      sql: `SELECT COALESCE(MAX(estimated_size), -1) AS count FROM audience_stat WHERE audience_id = ${args.audienceId}`,
    })
    wrongAudienceCount = actualAudienceCount === args.expectedAudienceCount ? 0 : 1
  }

  const accepted = acceptedInputCount(args, ledgers)
  const expectedExecutions = args.mode === 'audience'
    ? 0
    : args.sentSuccess * args.matchedCanvasCount

  return computeVerdict({
    planned: args.sentSuccess,
    sentSuccess: args.sentSuccess,
    accepted,
    expectedExecutions,
    actualExecutions: ledgers.executions,
    success: ledgers.success,
    failedWithRecord: ledgers.failed,
    dlq: ledgers.dlq,
    rejectedWithRecord: ledgers.rejectedWithRecord,
    retryPending: ledgers.retryPending,
    duplicateExecution: ledgers.duplicateInput,
    intentionalDuplicates: args.intentionalDuplicates,
    expectedFailedWithRecord: args.expectedFailedWithRecord,
    expectedRejectedWithRecord: args.expectedRejectedWithRecord,
    expectedDlq: args.expectedDlq,
    ackWithoutLedger: Math.max(0, args.sentSuccess - accepted),
    wrongAudienceCount,
    ledgerCounts: ledgers,
  })
}

export function isCliEntrypoint(moduleUrl, argvPath) {
  return Boolean(argvPath) && moduleUrl === pathToFileURL(argvPath).href
}

function main() {
  const result = verify(parseVerifierArgs(process.argv.slice(2)))
  console.log(JSON.stringify(result, null, 2))
  process.exitCode = result.verdict === 'FAIL' ? 2 : 0
}

if (isCliEntrypoint(import.meta.url, process.argv[1])) {
  try {
    main()
  } catch (error) {
    console.error(error.message)
    process.exitCode = 1
  }
}
