#!/usr/bin/env node

import { execFileSync } from 'node:child_process'
import { pathToFileURL } from 'node:url'

const DEFAULT_ARGS = {
  mysql: 'mysql',
  database: 'canvas_db',
  perfRunId: '',
  execute: false,
}

const FLAG_NAMES = {
  '--mysql': 'mysql',
  '--database': 'database',
  '--perf-run-id': 'perfRunId',
  '--execute': 'execute',
}

export function escapeSql(value) {
  return String(value)
    .replaceAll('\\', '\\\\')
    .replaceAll("'", "''")
}

export function buildCleanupSql(perfRunId) {
  const id = escapeSql(perfRunId)

  return `
DROP TEMPORARY TABLE IF EXISTS perf_cleanup_execution_ids;
CREATE TEMPORARY TABLE perf_cleanup_execution_ids (
  id VARCHAR(64) NOT NULL PRIMARY KEY
);
INSERT INTO perf_cleanup_execution_ids (id)
SELECT id FROM canvas_execution WHERE perf_run_id = '${id}';

SELECT COUNT(*) AS before_event_log_rows FROM event_log WHERE perf_run_id = '${id}';
SELECT COUNT(*) AS before_execution_rows FROM canvas_execution WHERE perf_run_id = '${id}';
SELECT COUNT(*) AS before_execution_trace_rows
FROM canvas_execution_trace
WHERE execution_id IN (
  SELECT id FROM perf_cleanup_execution_ids
);
SELECT COUNT(*) AS before_request_rows FROM canvas_execution_request WHERE perf_run_id = '${id}';
SELECT COUNT(*) AS before_dlq_rows FROM canvas_execution_dlq WHERE perf_run_id = '${id}';
SELECT COUNT(*) AS before_perf_event_definitions FROM event_definition WHERE event_code LIKE 'PERF_%';
SELECT COUNT(*) AS before_perf_mq_definitions FROM mq_message_definition WHERE message_code LIKE 'PERF_%';

DELETE FROM canvas_execution_trace
WHERE execution_id IN (
  SELECT id FROM perf_cleanup_execution_ids
);
DELETE FROM canvas_execution_dlq WHERE perf_run_id = '${id}';
DELETE FROM canvas_execution_request WHERE perf_run_id = '${id}';
DELETE FROM canvas_execution WHERE perf_run_id = '${id}';
DELETE FROM event_log WHERE perf_run_id = '${id}';

DELETE FROM event_definition WHERE event_code LIKE 'PERF_%';
DELETE FROM mq_message_definition WHERE message_code LIKE 'PERF_%';

SELECT COUNT(*) AS after_event_log_rows FROM event_log WHERE perf_run_id = '${id}';
SELECT COUNT(*) AS after_execution_rows FROM canvas_execution WHERE perf_run_id = '${id}';
SELECT COUNT(*) AS after_execution_trace_rows
FROM canvas_execution_trace
WHERE execution_id IN (
  SELECT id FROM perf_cleanup_execution_ids
);
SELECT COUNT(*) AS after_request_rows FROM canvas_execution_request WHERE perf_run_id = '${id}';
SELECT COUNT(*) AS after_dlq_rows FROM canvas_execution_dlq WHERE perf_run_id = '${id}';
SELECT COUNT(*) AS after_perf_event_definitions FROM event_definition WHERE event_code LIKE 'PERF_%';
SELECT COUNT(*) AS after_perf_mq_definitions FROM mq_message_definition WHERE message_code LIKE 'PERF_%';

DROP TEMPORARY TABLE IF EXISTS perf_cleanup_execution_ids;
`.trim()
}

function parseBoolean(flag, value) {
  if (value === 'true') {
    return true
  }
  if (value === 'false') {
    return false
  }
  throw new Error(`${flag} must be true or false`)
}

export function parseCleanupArgs(argv) {
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
    args[name] = name === 'execute' ? parseBoolean(flag, value) : value
  }

  if (!args.perfRunId) {
    throw new Error('--perf-run-id is required')
  }

  return args
}

export function isCliEntrypoint(moduleUrl, argvPath) {
  return Boolean(argvPath) && moduleUrl === pathToFileURL(argvPath).href
}

function main() {
  const args = parseCleanupArgs(process.argv.slice(2))
  const sql = buildCleanupSql(args.perfRunId)

  if (!args.execute) {
    console.log(sql)
    return
  }

  execFileSync(args.mysql, [
    '--batch',
    '--raw',
    '-uroot',
    '-proot',
    '-D',
    args.database,
    '-e',
    sql,
  ], { stdio: 'inherit' })
}

if (isCliEntrypoint(import.meta.url, process.argv[1])) {
  try {
    main()
  } catch (error) {
    console.error(error.message)
    process.exitCode = 1
  }
}
