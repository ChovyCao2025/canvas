import test from 'node:test'
import assert from 'node:assert/strict'

import {
  computeSideEffectVerdict,
  evaluateSideEffectEvidence,
  extractSideEffectEntries,
  parseSideEffectArgs,
  verifySideEffects,
} from './side-effect-verifier.mjs'

test('parseSideEffectArgs parses required run and count settings', () => {
  const args = parseSideEffectArgs([
    '--perf-run-id', 'perf_accuracy_001',
    '--sent-success', '10',
    '--wiremock-url', 'http://localhost:8099',
    '--path', '/mock/reach/send',
  ])

  assert.equal(args.perfRunId, 'perf_accuracy_001')
  assert.equal(args.sentSuccess, 10)
  assert.equal(args.wiremockUrl, 'http://localhost:8099')
  assert.equal(args.path, '/mock/reach/send')
})

test('extractSideEffectEntries reads reach payload variables from WireMock journal', () => {
  const entries = extractSideEffectEntries({
    requests: [
      {
        request: {
          url: '/mock/reach/send',
          body: JSON.stringify({
            templateId: 'perf-engine-even',
            variables: {
              branch: 'even',
              perfRunId: 'perf_accuracy_001',
              perfInputId: 'perf_accuracy_001:direct:2',
              seq: 2,
            },
            idempotencyKey: 'exec-1:send_even:EMAIL',
          }),
        },
      },
      {
        request: {
          url: '/mock/reach/send',
          body: JSON.stringify({
            templateId: 'other',
            variables: { perfRunId: 'other', perfInputId: 'other:1', branch: 'odd' },
          }),
        },
      },
    ],
  }, {
    perfRunId: 'perf_accuracy_001',
    path: '/mock/reach/send',
  })

  assert.deepEqual(entries, [
    {
      url: '/mock/reach/send',
      branch: 'even',
      perfRunId: 'perf_accuracy_001',
      perfInputId: 'perf_accuracy_001:direct:2',
      seq: 2,
      idempotencyKey: 'exec-1:send_even:EMAIL',
      templateId: 'perf-engine-even',
    },
  ])
})

test('evaluateSideEffectEvidence fails on count mismatch and duplicate branch input', () => {
  const evidence = evaluateSideEffectEvidence({
    sentSuccess: 4,
    entries: [
      { branch: 'odd', perfInputId: 'run:direct:1' },
      { branch: 'even', perfInputId: 'run:direct:2' },
      { branch: 'even', perfInputId: 'run:direct:2' },
    ],
  })

  assert.equal(evidence.actualTotal, 3)
  assert.equal(evidence.expectedTotal, 4)
  assert.equal(evidence.duplicateSideEffects, 1)
  assert.equal(evidence.sideEffectVerdict, 'FAIL')
})

test('computeSideEffectVerdict fails when side effect evidence fails', () => {
  const verdict = computeSideEffectVerdict({
    expectedTotal: 4,
    actualTotal: 3,
    totalMismatch: 1,
    duplicateSideEffects: 0,
    missingInputId: 0,
    branchMismatch: 0,
  })

  assert.equal(verdict.verdict, 'FAIL')
})

test('verifySideEffects fetches WireMock journal and returns pass evidence', async () => {
  const fetchJournal = async () => ({
    requests: [
      {
        request: {
          url: '/mock/reach/send',
          body: JSON.stringify({
            variables: {
              branch: 'odd',
              perfRunId: 'perf_accuracy_001',
              perfInputId: 'perf_accuracy_001:direct:1',
              seq: 1,
            },
          }),
        },
      },
      {
        request: {
          url: '/mock/reach/send',
          body: JSON.stringify({
            variables: {
              branch: 'even',
              perfRunId: 'perf_accuracy_001',
              perfInputId: 'perf_accuracy_001:direct:2',
              seq: 2,
            },
          }),
        },
      },
    ],
  })

  const result = await verifySideEffects({
    perfRunId: 'perf_accuracy_001',
    sentSuccess: 2,
    intentionalDuplicates: 0,
    path: '/mock/reach/send',
    wiremockUrl: 'http://localhost:8099',
  }, { fetchJournal })

  assert.equal(result.verdict, 'PASS')
  assert.equal(result.perfRunId, 'perf_accuracy_001')
  assert.equal(result.sentSuccess, 2)
  assert.equal(result.actualTotal, 2)
  assert.equal(result.actualEven, 1)
  assert.equal(result.actualOdd, 1)
})
