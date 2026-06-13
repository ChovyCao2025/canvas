import assert from 'node:assert/strict'
import { spawn, spawnSync } from 'node:child_process'
import { createServer } from 'node:http'
import { mkdtempSync, readFileSync, writeFileSync } from 'node:fs'
import { tmpdir } from 'node:os'
import { join as joinPath } from 'node:path'
import test from 'node:test'
import { fileURLToPath } from 'node:url'
import { dirname, join } from 'node:path'

const testDir = dirname(fileURLToPath(import.meta.url))
const cliPath = join(testDir, '..', 'src', 'index.mjs')
const fixture = (name) => join(testDir, 'fixtures', name)

function runCli(args) {
  return spawnSync(process.execPath, [cliPath, ...args], {
    cwd: join(testDir, '..'),
    encoding: 'utf8'
  })
}

function runCliAsync(args, options = {}) {
  return new Promise((resolve, reject) => {
    const env = {
      ...process.env,
      ...(options.env ?? {})
    }
    for (const [key, value] of Object.entries(env)) {
      if (value === undefined) {
        delete env[key]
      }
    }

    const child = spawn(process.execPath, [cliPath, ...args], {
      cwd: join(testDir, '..'),
      env,
      stdio: ['ignore', 'pipe', 'pipe']
    })
    let stdout = ''
    let stderr = ''

    child.stdout.setEncoding('utf8')
    child.stderr.setEncoding('utf8')
    child.stdout.on('data', (chunk) => {
      stdout += chunk
    })
    child.stderr.on('data', (chunk) => {
      stderr += chunk
    })
    child.on('error', reject)
    child.on('close', (status, signal) => {
      resolve({ status, signal, stdout, stderr })
    })
  })
}

function sendJson(response, status, body) {
  response.statusCode = status
  response.setHeader('content-type', 'application/json')
  response.end(JSON.stringify(body))
}

async function withApiServer(handler, run) {
  const requests = []
  const server = createServer(async (request, response) => {
    const chunks = []
    for await (const chunk of request) {
      chunks.push(chunk)
    }

    requests.push({
      method: request.method,
      url: request.url,
      headers: request.headers,
      body: Buffer.concat(chunks).toString('utf8')
    })

    try {
      await handler(requests.at(-1), response)
    } catch (error) {
      response.statusCode = 500
      response.end(error.stack ?? error.message)
    }
  })

  await new Promise((resolve) => server.listen(0, '127.0.0.1', resolve))
  const { port } = server.address()

  try {
    return await run(`http://127.0.0.1:${port}`, requests)
  } finally {
    await new Promise((resolve, reject) => {
      server.close((error) => error ? reject(error) : resolve())
    })
  }
}

test('help lists local and backend API commands with API URL configuration', () => {
  const result = runCli(['--help'])

  assert.equal(result.status, 0)
  assert.match(result.stdout, /validate <file>/)
  assert.match(result.stdout, /diff <before> <after>/)
  assert.match(result.stdout, /import <file>/)
  assert.match(result.stdout, /export <canvasId>/)
  assert.match(result.stdout, /publish <canvasId>/)
  assert.match(result.stdout, /--api-url <url>/)
  assert.match(result.stdout, /CANVAS_API_URL/)
  assert.equal(result.stderr, '')
})

test('validate accepts a valid Canvas DSL v1 journey', () => {
  const result = runCli(['validate', fixture('valid-journey.json')])

  assert.equal(result.status, 0)
  assert.match(result.stdout, /valid-journey\.json is valid/)
  assert.equal(result.stderr, '')
})

test('import posts a Canvas DSL document to the backend map preview endpoint', async () => {
  const document = JSON.parse(readFileSync(fixture('valid-journey.json'), 'utf8'))
  const responseBody = {
    canvasId: 'preview-123',
    status: 'mapped'
  }

  await withApiServer((request, response) => {
    sendJson(response, 200, responseBody)
  }, async (apiUrl, requests) => {
    const result = await runCliAsync([
      '--api-url',
      apiUrl,
      'import',
      fixture('valid-journey.json')
    ])

    assert.equal(result.status, 0)
    assert.deepEqual(JSON.parse(result.stdout), responseBody)
    assert.equal(result.stderr, '')
    assert.equal(requests.length, 1)
    assert.equal(requests[0].method, 'POST')
    assert.equal(requests[0].url, '/canvas/dsl/map')
    assert.match(requests[0].headers['content-type'], /application\/json/)
    assert.deepEqual(JSON.parse(requests[0].body), { document })
  })
})

test('export uses CANVAS_API_URL to GET a Canvas DSL document and print JSON', async () => {
  const responseBody = {
    apiVersion: 'canvas/v1',
    kind: 'Journey',
    metadata: {
      name: 'exported'
    }
  }

  await withApiServer((request, response) => {
    sendJson(response, 200, responseBody)
  }, async (apiUrl, requests) => {
    const result = await runCliAsync(['export', 'canvas-123'], {
      env: {
        CANVAS_API_URL: apiUrl
      }
    })

    assert.equal(result.status, 0)
    assert.deepEqual(JSON.parse(result.stdout), responseBody)
    assert.equal(result.stderr, '')
    assert.equal(requests.length, 1)
    assert.equal(requests[0].method, 'GET')
    assert.equal(requests[0].url, '/canvas/dsl/export/canvas-123')
    assert.equal(requests[0].body, '')
  })
})

test('--api-url overrides CANVAS_API_URL when publishing a canvas', async () => {
  const responseBody = {
    canvasId: 'canvas-123',
    status: 'published'
  }

  await withApiServer((request, response) => {
    sendJson(response, 200, responseBody)
  }, async (apiUrl, requests) => {
    const result = await runCliAsync([
      '--api-url',
      apiUrl,
      'publish',
      'canvas-123'
    ], {
      env: {
        CANVAS_API_URL: 'http://127.0.0.1:1'
      }
    })

    assert.equal(result.status, 0)
    assert.deepEqual(JSON.parse(result.stdout), responseBody)
    assert.equal(result.stderr, '')
    assert.equal(requests.length, 1)
    assert.equal(requests[0].method, 'POST')
    assert.equal(requests[0].url, '/canvas/canvas-123/publish')
    assert.equal(requests[0].body, '')
  })
})

test('API commands report non-2xx responses with method, path, status, and body', async () => {
  await withApiServer((request, response) => {
    sendJson(response, 422, {
      message: 'DAG validation failed',
      errors: ['spec.edges[0].to is unknown']
    })
  }, async (apiUrl) => {
    const result = await runCliAsync([
      '--api-url',
      apiUrl,
      'import',
      fixture('valid-journey.json')
    ])

    assert.notEqual(result.status, 0)
    assert.equal(result.stdout, '')
    assert.match(result.stderr, /API request failed: POST \/canvas\/dsl\/map returned 422/)
    assert.match(result.stderr, /DAG validation failed/)
    assert.match(result.stderr, /spec\.edges\[0\]\.to is unknown/)
  })
})

test('validate rejects invalid Canvas DSL v1 fields with actionable errors', () => {
  const result = runCli(['validate', fixture('invalid-journey.json')])

  assert.notEqual(result.status, 0)
  assert.match(result.stderr, /apiVersion must be canvas\/v1/)
  assert.match(result.stderr, /metadata\.name is required/)
  assert.match(result.stderr, /spec\.trigger is required/)
  assert.match(result.stderr, /spec\.nodes\[0\]\.type is required/)
})

test('validate rejects non-string names, node ids, and node types', () => {
  const result = runCli(['validate', fixture('non-string-identifiers.json')])

  assert.notEqual(result.status, 0)
  assert.match(result.stderr, /metadata\.name must be a non-empty string/)
  assert.match(result.stderr, /spec\.nodes\[0\]\.id must be a non-empty string/)
  assert.match(result.stderr, /spec\.nodes\[0\]\.type must be a non-empty string/)
})

test('diff summarizes added, removed, and changed node ids locally', () => {
  const result = runCli([
    'diff',
    fixture('diff-before.json'),
    fixture('diff-after.json')
  ])

  assert.equal(result.status, 0)
  assert.equal(result.stdout, [
    'Added nodes: ai-copy',
    'Removed nodes: old-coupon',
    'Changed nodes: send-message',
    ''
  ].join('\n'))
  assert.equal(result.stderr, '')
})

test('diff reports same-id nodes changed when long string content differs beyond inspected output', () => {
  const dir = mkdtempSync(joinPath(tmpdir(), 'canvas-cli-'))
  const longPrefix = 'x'.repeat(20000)
  const longSuffix = 'z'.repeat(20000)
  const before = joinPath(dir, 'before.json')
  const after = joinPath(dir, 'after.json')
  const journey = (body) => ({
    apiVersion: 'canvas/v1',
    kind: 'Journey',
    metadata: {
      name: 'long-body'
    },
    spec: {
      trigger: {
        type: 'webhook',
        event: 'message.created'
      },
      nodes: [
        {
          id: 'long-message',
          type: 'message',
          config: {
            body
          }
        }
      ]
    }
  })

  writeFileSync(before, JSON.stringify(journey(`${longPrefix}A${longSuffix}`)))
  writeFileSync(after, JSON.stringify(journey(`${longPrefix}B${longSuffix}`)))

  const result = runCli(['diff', before, after])

  assert.equal(result.status, 0)
  assert.equal(result.stdout, [
    'Added nodes: none',
    'Removed nodes: none',
    'Changed nodes: long-message',
    ''
  ].join('\n'))
  assert.equal(result.stderr, '')
})
