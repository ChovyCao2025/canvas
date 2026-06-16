import assert from 'node:assert/strict'
import http from 'node:http'
import test from 'node:test'

import { verifyPlaygroundLiveApiSmoke } from './playground-live-api-smoke.mjs'

async function withServer(handler, fn) {
  const server = http.createServer(handler)
  server.keepAliveTimeout = 1
  await new Promise(resolve => server.listen(0, '127.0.0.1', resolve))
  const address = server.address()
  try {
    return await fn(`http://127.0.0.1:${address.port}`)
  } finally {
    await new Promise((resolve, reject) => {
      server.close(error => (error ? reject(error) : resolve()))
    })
  }
}

async function readJsonBody(request) {
  let raw = ''
  for await (const chunk of request) {
    raw += chunk
  }
  return JSON.parse(raw)
}

test('posts the playground fixture envelope to the live DSL map endpoint', async () => {
  await withServer(async (request, response) => {
    assert.equal(request.method, 'POST')
    assert.equal(request.url, '/canvas/dsl/map')
    assert.equal(request.headers['content-type'], 'application/json')

    const body = await readJsonBody(request)
    assert.equal(body.document.metadata.name, 'new-user-welcome')
    assert.equal(body.document.spec.nodes[0].id, 'segment')

    response.writeHead(200, { 'content-type': 'application/json' })
    response.end(JSON.stringify({
      templateKey: 'new-user-welcome',
      graphJson: '{"kind":"Journey","nodes":[]}',
      violations: [],
    }))
  }, async apiUrl => {
    const result = await verifyPlaygroundLiveApiSmoke({ apiUrl, timeoutMs: 1000 })

    assert.equal(result.ok, true)
    assert.equal(result.status, 'PASS')
    assert.equal(result.endpoint, `${apiUrl}/canvas/dsl/map`)
    assert.equal(result.fixture, 'tools/canvas-cli/test/fixtures/playground-new-user-welcome.json')
    assert.deepEqual(result.errors, [])
  })
})

test('fails clearly on non-2xx live DSL map responses', async () => {
  await withServer((request, response) => {
    response.writeHead(503, { 'content-type': 'application/json' })
    response.end(JSON.stringify({ error: 'backend unavailable' }))
  }, async apiUrl => {
    const result = await verifyPlaygroundLiveApiSmoke({ apiUrl, timeoutMs: 1000 })

    assert.equal(result.ok, false)
    assert.equal(result.status, 'FAIL')
    assert.match(result.errors.join('\n'), /POST .*\/canvas\/dsl\/map returned HTTP 503/)
  })
})

test('fails when the live DSL map response is not usable mapping JSON', async () => {
  await withServer((request, response) => {
    response.writeHead(200, { 'content-type': 'application/json' })
    response.end(JSON.stringify({
      templateKey: 'new-user-welcome',
      violations: [],
    }))
  }, async apiUrl => {
    const result = await verifyPlaygroundLiveApiSmoke({ apiUrl, timeoutMs: 1000 })

    assert.equal(result.ok, false)
    assert.match(result.errors.join('\n'), /response graphJson must be a non-empty string/)
  })
})

test('fails when the live DSL map response is not JSON', async () => {
  await withServer((request, response) => {
    response.writeHead(200, { 'content-type': 'application/json' })
    response.end('not-json')
  }, async apiUrl => {
    const result = await verifyPlaygroundLiveApiSmoke({ apiUrl, timeoutMs: 1000 })

    assert.equal(result.ok, false)
    assert.match(result.errors.join('\n'), /returned invalid JSON/)
  })
})

test('fails clearly when the live DSL map request times out', async () => {
  await withServer((request, response) => {
    setTimeout(() => {
      response.writeHead(200, { 'content-type': 'application/json' })
      response.end(JSON.stringify({
        templateKey: 'new-user-welcome',
        graphJson: '{}',
        violations: [],
      }))
    }, 50)
  }, async apiUrl => {
    const result = await verifyPlaygroundLiveApiSmoke({ apiUrl, timeoutMs: 10 })

    assert.equal(result.ok, false)
    assert.match(result.errors.join('\n'), /timed out after 10ms/)
  })
})
