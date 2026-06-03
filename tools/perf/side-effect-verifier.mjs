#!/usr/bin/env node

import { pathToFileURL } from 'node:url'

const DEFAULT_ARGS = {
  wiremockUrl: 'http://localhost:8099',
  path: '/mock/reach/send',
  perfRunId: '',
  sentSuccess: 0,
  intentionalDuplicates: 0,
}

const FLAG_NAMES = {
  '--wiremock-url': 'wiremockUrl',
  '--path': 'path',
  '--perf-run-id': 'perfRunId',
  '--sent-success': 'sentSuccess',
  '--intentional-duplicates': 'intentionalDuplicates',
}

const NUMBER_ARGS = new Set(['sentSuccess', 'intentionalDuplicates'])

function parseNonNegativeInteger(flag, value) {
  if (!/^(0|[1-9]\d*)$/.test(value)) {
    throw new Error(`${flag} must be a non-negative integer`)
  }
  return Number(value)
}

function validateArgs(args) {
  if (!args.perfRunId) {
    throw new Error('--perf-run-id is required')
  }
  if (args.sentSuccess < 0) {
    throw new Error('--sent-success must be a non-negative integer')
  }
  if (args.intentionalDuplicates < 0) {
    throw new Error('--intentional-duplicates must be a non-negative integer')
  }
}

export function parseSideEffectArgs(argv) {
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
    args[name] = NUMBER_ARGS.has(name) ? parseNonNegativeInteger(flag, value) : value
  }
  validateArgs(args)
  return args
}

function uniqueInputCount({ sentSuccess, intentionalDuplicates = 0 }) {
  return Math.max(0, sentSuccess - intentionalDuplicates)
}

function expectedCounts(args) {
  const uniqueInputs = uniqueInputCount(args)
  return {
    total: uniqueInputs,
    even: Math.floor(uniqueInputs / 2),
    odd: Math.ceil(uniqueInputs / 2),
  }
}

function tryParseJson(text) {
  if (typeof text !== 'string' || text.trim() === '') {
    return null
  }
  try {
    return JSON.parse(text)
  } catch {
    return null
  }
}

function findNestedValue(value, key) {
  if (value == null || typeof value !== 'object') {
    return undefined
  }
  if (Object.prototype.hasOwnProperty.call(value, key)) {
    return value[key]
  }
  if (Array.isArray(value)) {
    for (const item of value) {
      const found = findNestedValue(item, key)
      if (found !== undefined) return found
    }
    return undefined
  }
  for (const item of Object.values(value)) {
    const found = findNestedValue(item, key)
    if (found !== undefined) return found
  }
  return undefined
}

function normalizeRequest(record) {
  return record?.request || record
}

function requestBody(request) {
  if (typeof request?.body === 'string') return request.body
  if (typeof request?.bodyAsString === 'string') return request.bodyAsString
  if (typeof request?.bodyAsBase64 === 'string') {
    return Buffer.from(request.bodyAsBase64, 'base64').toString('utf8')
  }
  return ''
}

function sideEffectEntryFromRequest(request) {
  const parsed = tryParseJson(requestBody(request))
  if (!parsed) return null
  const perfRunId = findNestedValue(parsed, 'perfRunId')
  const perfInputId = findNestedValue(parsed, 'perfInputId')
  const branch = findNestedValue(parsed, 'branch')
  const seq = findNestedValue(parsed, 'seq')
  return {
    url: request.url || request.absoluteUrl || '',
    branch: branch == null ? '' : String(branch),
    perfRunId: perfRunId == null ? '' : String(perfRunId),
    perfInputId: perfInputId == null ? '' : String(perfInputId),
    seq: typeof seq === 'number' ? seq : Number(seq),
    idempotencyKey: String(findNestedValue(parsed, 'idempotencyKey') || ''),
    templateId: String(findNestedValue(parsed, 'templateId') || ''),
  }
}

export function extractSideEffectEntries(journal, { perfRunId, path }) {
  const requests = Array.isArray(journal?.requests) ? journal.requests : []
  return requests
    .map(normalizeRequest)
    .filter((request) => request && String(request.url || '').startsWith(path))
    .map(sideEffectEntryFromRequest)
    .filter((entry) => entry && entry.perfRunId === perfRunId)
}

function duplicateCount(entries) {
  const seen = new Set()
  let duplicates = 0
  for (const entry of entries) {
    const key = `${entry.branch}:${entry.perfInputId}`
    if (seen.has(key)) {
      duplicates += 1
    } else {
      seen.add(key)
    }
  }
  return duplicates
}

export function evaluateSideEffectEvidence({
  sentSuccess,
  intentionalDuplicates = 0,
  entries = [],
}) {
  const expected = expectedCounts({ sentSuccess, intentionalDuplicates })
  const actualEven = entries.filter((entry) => entry.branch === 'even').length
  const actualOdd = entries.filter((entry) => entry.branch === 'odd').length
  const actualTotal = entries.length
  const missingInputId = entries.filter((entry) => !entry.perfInputId).length
  const duplicateSideEffects = duplicateCount(entries.filter((entry) => entry.perfInputId))
  const totalMismatch = actualTotal === expected.total ? 0 : 1
  const branchMismatch = actualEven === expected.even && actualOdd === expected.odd ? 0 : 1
  const failures = [
    totalMismatch,
    branchMismatch,
    duplicateSideEffects,
    missingInputId,
  ].filter((value) => value > 0)

  return {
    sideEffectEnabled: true,
    expectedTotal: expected.total,
    actualTotal,
    expectedEven: expected.even,
    actualEven,
    expectedOdd: expected.odd,
    actualOdd,
    totalMismatch,
    branchMismatch,
    duplicateSideEffects,
    missingInputId,
    sideEffectVerdict: failures.length > 0 ? 'FAIL' : 'PASS',
    entries,
  }
}

export function computeSideEffectVerdict(input) {
  const failures = [
    input.totalMismatch || 0,
    input.branchMismatch || 0,
    input.duplicateSideEffects || 0,
    input.missingInputId || 0,
  ].filter((value) => value > 0)
  return {
    ...input,
    verdict: failures.length > 0 ? 'FAIL' : 'PASS',
  }
}

async function defaultFetchJournal({ wiremockUrl }) {
  const response = await fetch(`${wiremockUrl}/__admin/requests`)
  const text = await response.text()
  if (!response.ok) {
    throw new Error(`WireMock journal request failed with HTTP ${response.status}: ${text}`)
  }
  return JSON.parse(text)
}

export async function verifySideEffects(args, deps = {}) {
  const fetchJournal = deps.fetchJournal || (() => defaultFetchJournal(args))
  const journal = await fetchJournal(args)
  const entries = extractSideEffectEntries(journal, args)
  return computeSideEffectVerdict({
    perfRunId: args.perfRunId,
    sentSuccess: args.sentSuccess,
    path: args.path,
    ...evaluateSideEffectEvidence({
      sentSuccess: args.sentSuccess,
      intentionalDuplicates: args.intentionalDuplicates,
      entries,
    }),
  })
}

export function isCliEntrypoint(moduleUrl, argvPath) {
  return Boolean(argvPath) && moduleUrl === pathToFileURL(argvPath).href
}

async function main() {
  const result = await verifySideEffects(parseSideEffectArgs(process.argv.slice(2)))
  console.log(JSON.stringify(result, null, 2))
  process.exitCode = result.verdict === 'FAIL' ? 2 : 0
}

if (isCliEntrypoint(import.meta.url, process.argv[1])) {
  main().catch((error) => {
    console.error(error.message)
    process.exitCode = 1
  })
}
