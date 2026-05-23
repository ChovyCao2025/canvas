#!/usr/bin/env node

import { pathToFileURL } from 'node:url'

const DEFAULT_ARGS = {
  mode: 'event',
  baseUrl: 'http://localhost:8080',
  perfRunId: '',
  count: 1000,
  concurrency: 20,
  eventCode: 'PERF_ORDER_PAID',
  canvasId: '',
  audienceId: '',
  userPrefix: 'perf_user_',
  userModulo: 1000,
}

const FLAG_NAMES = {
  '--mode': 'mode',
  '--base-url': 'baseUrl',
  '--perf-run-id': 'perfRunId',
  '--count': 'count',
  '--concurrency': 'concurrency',
  '--event-code': 'eventCode',
  '--canvas-id': 'canvasId',
  '--audience-id': 'audienceId',
  '--user-prefix': 'userPrefix',
  '--user-modulo': 'userModulo',
}

const NUMBER_ARGS = new Set(['count', 'concurrency', 'userModulo'])
const MODES = new Set(['event', 'direct', 'audience'])

function parseIntegerArg(flag, value, { allowZero }) {
  const pattern = allowZero ? /^(0|[1-9]\d*)$/ : /^[1-9]\d*$/

  if (!pattern.test(value)) {
    const kind = allowZero ? 'a non-negative integer' : 'a positive integer'
    throw new Error(`${flag} must be ${kind}`)
  }

  return Number(value)
}

function validateArgs(args) {
  if (!MODES.has(args.mode)) {
    throw new Error('--mode must be one of event, direct, audience')
  }

  if (!Number.isSafeInteger(args.count) || args.count < 0) {
    throw new Error('--count must be a non-negative integer')
  }

  if (!Number.isSafeInteger(args.concurrency) || args.concurrency < 1) {
    throw new Error('--concurrency must be a positive integer')
  }

  if (!Number.isSafeInteger(args.userModulo) || args.userModulo < 1) {
    throw new Error('--user-modulo must be a positive integer')
  }

  if (!args.perfRunId) {
    throw new Error('--perf-run-id is required')
  }

  if (args.mode === 'direct' && !args.canvasId) {
    throw new Error('--canvas-id is required for direct mode')
  }

  if (args.mode === 'audience' && !args.audienceId) {
    throw new Error('--audience-id is required for audience mode')
  }
}

export function parseRunnerArgs(argv) {
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
      args[name] = parseIntegerArg(flag, value, { allowZero: name === 'count' })
    } else {
      args[name] = value
    }
  }

  validateArgs(args)

  return args
}

export function buildEventPayload({
  perfRunId,
  eventCode,
  userPrefix,
  seq,
  userModulo,
}) {
  return {
    eventCode,
    userId: `${userPrefix}${seq % userModulo}`,
    attributes: {
      perfRunId,
      perfInputId: `${perfRunId}:event:${seq}`,
      seq,
      amount: seq % 1000,
    },
  }
}

export function buildDirectPayload({ perfRunId, userPrefix, seq, userModulo }) {
  return {
    userId: `${userPrefix}${seq % userModulo}`,
    idempotencyKey: `${perfRunId}:direct:${seq}`,
    inputParams: {
      perfRunId,
      perfInputId: `${perfRunId}:direct:${seq}`,
      seq,
    },
  }
}

export function* chunkSeq(count, concurrency) {
  for (let start = 1; start <= count; start += concurrency) {
    const chunk = []
    const end = Math.min(start + concurrency - 1, count)

    for (let seq = start; seq <= end; seq += 1) {
      chunk.push(seq)
    }

    yield chunk
  }
}

function buildRequest(args, seq) {
  if (args.mode === 'event') {
    return {
      url: `${args.baseUrl}/canvas/events/report`,
      body: buildEventPayload({
        perfRunId: args.perfRunId,
        eventCode: args.eventCode,
        userPrefix: args.userPrefix,
        seq,
        userModulo: args.userModulo,
      }),
    }
  }

  if (args.mode === 'direct') {
    if (!args.canvasId) {
      throw new Error('--canvas-id is required for direct mode')
    }

    return {
      url: `${args.baseUrl}/canvas/execute/direct/${args.canvasId}`,
      body: buildDirectPayload({
        perfRunId: args.perfRunId,
        userPrefix: args.userPrefix,
        seq,
        userModulo: args.userModulo,
      }),
    }
  }

  if (args.mode === 'audience') {
    if (!args.audienceId) {
      throw new Error('--audience-id is required for audience mode')
    }

    return {
      url: `${args.baseUrl}/canvas/audiences/${args.audienceId}/compute`,
      body: {
        perfRunId: args.perfRunId,
        perfInputId: `${args.perfRunId}:audience:${seq}`,
      },
    }
  }

  throw new Error(`Unsupported mode: ${args.mode}`)
}

async function sendRequest(args, seq) {
  const request = buildRequest(args, seq)
  const startedAt = performance.now()

  try {
    const response = await fetch(request.url, {
      method: 'POST',
      headers: {
        'content-type': 'application/json',
      },
      body: JSON.stringify(request.body),
    })

    return {
      ok: response.ok,
      durationMs: performance.now() - startedAt,
    }
  } catch (error) {
    return {
      ok: false,
      durationMs: performance.now() - startedAt,
      error,
    }
  }
}

export async function run(args) {
  let sent = 0
  let success = 0
  let failed = 0
  const durations = []

  for (const chunk of chunkSeq(args.count, args.concurrency)) {
    const results = await Promise.all(
      chunk.map(async (seq) => {
        sent += 1
        return sendRequest(args, seq)
      }),
    )

    for (const result of results) {
      durations.push(result.durationMs)

      if (result.ok) {
        success += 1
      } else {
        failed += 1
      }
    }
  }

  durations.sort((left, right) => left - right)
  const p95Index = Math.min(
    durations.length - 1,
    Math.max(0, Math.ceil(durations.length * 0.95) - 1),
  )
  const p95Ms = durations.length === 0 ? 0 : durations[p95Index]

  return {
    perfRunId: args.perfRunId,
    mode: args.mode,
    sent,
    success,
    failed,
    p95Ms,
  }
}

export function exitCodeForSummary(summary) {
  return summary.failed > 0 ? 2 : 0
}

export function isCliEntrypoint(moduleUrl, argvPath) {
  return Boolean(argvPath) && moduleUrl === pathToFileURL(argvPath).href
}

async function main() {
  const args = parseRunnerArgs(process.argv.slice(2))
  const summary = await run(args)

  console.log(JSON.stringify(summary, null, 2))
  process.exitCode = exitCodeForSummary(summary)
}

if (isCliEntrypoint(import.meta.url, process.argv[1])) {
  main().catch((error) => {
    console.error(error.message)
    process.exitCode = 1
  })
}
