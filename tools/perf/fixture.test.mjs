import test from 'node:test'
import assert from 'node:assert/strict'

import {
  buildEngineAccuracyCanvasPayload,
  buildFixtureCanvases,
  createPerfFixtures,
  engineAccuracyTraceVerifierArgs,
} from './fixture.mjs'

test('buildEngineAccuracyCanvasPayload creates deterministic direct branch graph', () => {
  const payload = buildEngineAccuracyCanvasPayload()
  const graph = JSON.parse(payload.graphJson)
  const nodes = new Map(graph.nodes.map((node) => [node.id, node]))

  assert.equal(payload.name, 'PERF_ENGINE_ACCURACY')
  assert.equal(nodes.get('direct').type, 'DIRECT_CALL')
  assert.equal(nodes.get('normalize').type, 'GROOVY')
  assert.equal(nodes.get('route_even').type, 'IF_CONDITION')
  assert.equal(nodes.get('send_even').type, 'SEND_MESSAGE')
  assert.equal(nodes.get('send_odd').type, 'SEND_MESSAGE')
  assert.equal(nodes.get('join').type, 'HUB')
  assert.equal(nodes.get('end').type, 'END')
  assert.equal(nodes.get('route_even').config.successNodeId, 'send_even')
  assert.equal(nodes.get('route_even').config.failNodeId, 'send_odd')
  assert.equal(nodes.get('send_even').config.variables.branch, 'even')
  assert.equal(nodes.get('send_even').config.variables.perfInputId, '$perfInputId')
  assert.equal(nodes.get('send_odd').config.variables.branch, 'odd')
})

test('engineAccuracyTraceVerifierArgs covers success and skipped branch evidence', () => {
  assert.deepEqual(engineAccuracyTraceVerifierArgs(), [
    '--expect-trace', 'direct:success=all',
    '--expect-trace', 'normalize:success=all',
    '--expect-trace', 'route_even:success=all',
    '--expect-trace', 'send_even:success=even',
    '--expect-trace', 'send_odd:success=odd',
    '--expect-trace', 'send_even:skipped=odd',
    '--expect-trace', 'send_odd:skipped=even',
    '--expect-trace', 'join:success=all',
    '--expect-trace', 'end:success=all',
  ])
})

test('buildFixtureCanvases returns light and engine accuracy canvases', () => {
  assert.deepEqual(buildFixtureCanvases().map((payload) => payload.name), [
    'PERF_DIRECT_LIGHT',
    'PERF_EVENT_LIGHT',
    'PERF_ENGINE_ACCURACY',
  ])
})

test('createPerfFixtures archives old perf canvases and publishes fresh fixtures', async () => {
  const calls = []
  const client = {
    async request(method, path, { body, token } = {}) {
      calls.push({ method, path, body, token })
      if (path === '/auth/login') return { code: 0, data: { token: 'token-1' } }
      if (path.startsWith('/canvas/event-definitions')) return { code: 0, data: { total: 0, list: [] } }
      if (path.startsWith('/canvas/list')) {
        return {
          code: 0,
          data: {
            total: 2,
            list: [
              { id: 7, name: 'PERF_EVENT_LIGHT', status: 1 },
              { id: 8, name: 'PERF_ENGINE_ACCURACY', status: 0 },
            ],
          },
        }
      }
      if (method === 'POST' && path === '/canvas') {
        return { code: 0, data: { id: 100 + calls.filter((call) => call.path === '/canvas').length } }
      }
      if (method === 'POST' && path.endsWith('/publish?operator=perf')) {
        return { code: 0, data: { id: 900 + calls.filter((call) => call.path.endsWith('/publish?operator=perf')).length } }
      }
      if (method === 'POST' && path.includes('/archive?operator=perf')) return { code: 0, data: null }
      throw new Error(`unexpected ${method} ${path}`)
    },
  }

  const result = await createPerfFixtures({
    baseUrl: 'http://localhost:8080',
    rebuild: true,
  }, {
    client,
    env: {
      PERF_ADMIN_USERNAME: 'admin',
      PERF_ADMIN_PASSWORD: 'Admin@123',
    },
  })

  assert.equal(result.status, 'READY')
  assert.equal(result.directCanvasId, 101)
  assert.equal(result.eventCanvasId, 102)
  assert.equal(result.engineAccuracyCanvasId, 103)
  assert.equal(calls[0].path, '/auth/login')
  assert.ok(calls.some((call) => call.path === '/canvas/7/archive?operator=perf'))
  assert.ok(calls.some((call) => call.path === '/canvas/8/archive?operator=perf'))
  assert.equal(calls.filter((call) => call.path.endsWith('/publish?operator=perf')).length, 3)
  assert.ok(result.engineAccuracyVerifierArgs.includes('send_even:success=even'))
})
