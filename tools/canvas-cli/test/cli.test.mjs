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

test('help exposes G10-unlocked import and export while publish stays gated', () => {
  const result = runCli(['--help'])

  assert.equal(result.status, 0)
  assert.match(result.stdout, /validate <file>/)
  assert.match(result.stdout, /diff <before> <after>/)
  assert.match(result.stdout, /import <file>/)
  assert.match(result.stdout, /export <canvasId>/)
  assert.match(result.stdout, /--api-url <url>/)
  assert.match(result.stdout, /CANVAS_API_URL/)
  assert.match(result.stdout, /publish remains blocked until a stable backend publish API is verified/)
  assert.doesNotMatch(result.stdout, /publish <canvasId>/)
  assert.equal(result.stderr, '')
})

test('validate accepts a valid Canvas DSL v1 journey', () => {
  const result = runCli(['validate', fixture('valid-journey.json')])

  assert.equal(result.status, 0)
  assert.match(result.stdout, /valid-journey\.json is valid/)
  assert.equal(result.stderr, '')
})

test('import posts a local DSL document to the G10-unlocked backend import endpoint', async () => {
  await withApiServer((request, response) => {
    assert.equal(request.method, 'POST')
    assert.equal(request.url, '/canvas/dsl/import')
    assert.equal(request.headers['content-type'], 'application/json')
    assert.deepEqual(JSON.parse(request.body), {
      document: JSON.parse(readFileSync(fixture('valid-journey.json'), 'utf8'))
    })

    response.setHeader('content-type', 'application/json')
    response.end(JSON.stringify({
      compatible: true,
      result: {
        canvasId: 'canvas-123',
        imported: true
      }
    }))
  }, async (apiUrl, requests) => {
    const result = await runCliAsync(['--api-url', apiUrl, 'import', fixture('valid-journey.json')])

    assert.equal(result.status, 0)
    assert.deepEqual(JSON.parse(result.stdout), {
      canvasId: 'canvas-123',
      imported: true
    })
    assert.equal(result.stderr, '')
    assert.equal(requests.length, 1)
  })
})

test('import prints a direct JSON response from the backend import endpoint', async () => {
  await withApiServer((request, response) => {
    assert.equal(request.method, 'POST')
    assert.equal(request.url, '/canvas/dsl/import')
    response.setHeader('content-type', 'application/json')
    response.end(JSON.stringify({
      canvasId: 'canvas-direct',
      imported: true
    }))
  }, async (apiUrl) => {
    const result = await runCliAsync(['import', fixture('valid-journey.json')], {
      env: {
        CANVAS_API_URL: apiUrl
      }
    })

    assert.equal(result.status, 0)
    assert.deepEqual(JSON.parse(result.stdout), {
      canvasId: 'canvas-direct',
      imported: true
    })
    assert.equal(result.stderr, '')
  })
})

test('export gets the G10-unlocked backend export endpoint and prints JSON', async () => {
  await withApiServer((request, response) => {
    assert.equal(request.method, 'GET')
    assert.equal(request.url, '/canvas/dsl/export/canvas-123')
    assert.equal(request.body, '')
    assert.equal(request.headers['x-tenant-id'], 'tenant-demo')

    response.setHeader('content-type', 'application/json')
    response.end(JSON.stringify({
      apiVersion: 'canvas/v1',
      kind: 'Journey',
      metadata: {
        name: 'exported-journey'
      },
      spec: {
        trigger: {
          type: 'webhook'
        },
        nodes: [
          {
            id: 'message',
            type: 'message.send'
          }
        ]
      }
    }))
  }, async (apiUrl, requests) => {
    const result = await runCliAsync(['export', 'canvas-123', '--api-url', apiUrl, '--tenant-id', 'tenant-demo'])

    assert.equal(result.status, 0)
    assert.deepEqual(JSON.parse(result.stdout).metadata, {
      name: 'exported-journey'
    })
    assert.equal(result.stderr, '')
    assert.equal(requests.length, 1)
  })
})

test('export accepts tenant id from CANVAS_TENANT_ID', async () => {
  await withApiServer((request, response) => {
    assert.equal(request.method, 'GET')
    assert.equal(request.url, '/canvas/dsl/export/canvas-env')
    assert.equal(request.headers['x-tenant-id'], 'tenant-env')

    response.setHeader('content-type', 'application/json')
    response.end(JSON.stringify({
      apiVersion: 'canvas/v1',
      kind: 'Journey',
      metadata: {
        name: 'env-tenant-export'
      },
      spec: {
        trigger: {
          type: 'webhook'
        },
        nodes: [
          {
            id: 'message',
            type: 'message.send'
          }
        ]
      }
    }))
  }, async (apiUrl) => {
    const result = await runCliAsync(['export', 'canvas-env', '--api-url', apiUrl], {
      env: {
        CANVAS_TENANT_ID: 'tenant-env'
      }
    })

    assert.equal(result.status, 0)
    assert.deepEqual(JSON.parse(result.stdout).metadata, {
      name: 'env-tenant-export'
    })
    assert.equal(result.stderr, '')
  })
})

test('publish stays G10-gated and does not make network requests', async () => {
  await withApiServer((request, response) => {
    response.statusCode = 500
    response.end(`unexpected request: ${request.method} ${request.url}`)
  }, async (apiUrl, requests) => {
    const result = await runCliAsync(['--api-url', apiUrl, 'publish', 'canvas-123'], {
      env: {
        CANVAS_API_URL: apiUrl
      }
    })

    assert.notEqual(result.status, 0)
    assert.equal(result.stdout, '')
    assert.match(result.stderr, /Publish is gated until a stable backend publish API is verified/)
    assert.equal(requests.length, 0)
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
