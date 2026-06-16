import assert from 'node:assert/strict'
import { mkdirSync, writeFileSync } from 'node:fs'
import { mkdtemp } from 'node:fs/promises'
import { tmpdir } from 'node:os'
import path from 'node:path'
import test from 'node:test'

import { verifyPlaygroundRuntimeSmoke } from './playground-runtime-smoke.mjs'

async function fixture() {
  const root = await mkdtemp(path.join(tmpdir(), 'playground-runtime-smoke-'))
  mkdirSync(path.join(root, 'tools/canvas-cli/test/fixtures'), { recursive: true })
  mkdirSync(path.join(root, 'wiremock/mappings'), { recursive: true })
  mkdirSync(path.join(root, 'docs/open-source'), { recursive: true })

  writeFileSync(path.join(root, 'tools/canvas-cli/test/fixtures/playground-new-user-welcome.json'), JSON.stringify({
    apiVersion: 'canvas/v1',
    kind: 'Journey',
    metadata: {
      name: 'new-user-welcome',
      title: 'New User Welcome',
    },
    spec: {
      trigger: { type: 'webhook', event: 'user.registered' },
      nodes: [
        { id: 'segment', type: 'condition', config: { expression: 'user.lifecycleStage == "new"' } },
        { id: 'coupon', type: 'coupon.grant', config: { couponKey: 'WELCOME_10' } },
        { id: 'message', type: 'message.send', config: { channel: 'sms', template: 'welcome_coupon' } },
      ],
      edges: [
        { from: 'segment', to: 'coupon', when: true },
        { from: 'coupon', to: 'message' },
      ],
    },
    samplePayload: {
      event: 'user.registered',
      user: { id: 'u_1001', lifecycleStage: 'new', phone: '+8613800000001' },
    },
  }, null, 2))

  writeFileSync(path.join(root, 'wiremock/mappings/demo-catalog.json'), JSON.stringify({
    mappings: [
      {
        request: { method: 'GET', url: '/mock/demo/golden-path' },
        response: {
          jsonBody: {
            steps: [
              'review new-user-welcome',
              'review mock AI risk audit',
            ],
          },
        },
      },
      {
        request: { method: 'GET', url: '/mock/demo/templates' },
        response: {
          jsonBody: {
            templates: [
              {
                key: 'new-user-welcome',
                requiredPlugins: ['canvas-plugin-webhook', 'canvas-plugin-coupon', 'canvas-plugin-message'],
              },
            ],
          },
        },
      },
      {
        request: { method: 'GET', url: '/mock/demo/plugins' },
        response: {
          jsonBody: {
            plugins: [
              { key: 'canvas-plugin-webhook', enabled: true, mode: 'mock' },
              { key: 'canvas-plugin-coupon', enabled: true, mode: 'mock' },
              { key: 'canvas-plugin-message', enabled: true, mode: 'mock' },
            ],
          },
        },
      },
    ],
  }, null, 2))

  writeFileSync(path.join(root, 'docs/open-source/playground.md'), [
    '# Playground',
    '',
    'Run the offline runtime smoke before a user-facing demo:',
    '',
    '```bash',
    'node tools/open-source-growth/playground-runtime-smoke.mjs',
    '```',
    '',
    'Local runtime smoke is available for the checked-in fixture and WireMock catalog.',
  ].join('\n'))

  return root
}

test('accepts coherent local playground runtime smoke artifacts', async () => {
  const root = await fixture()

  const result = verifyPlaygroundRuntimeSmoke(root)

  assert.equal(result.ok, true)
  assert.deepEqual(result.errors, [])
  assert.equal(result.summary.fixture, 'new-user-welcome')
})

test('rejects fixture drift from new-user-welcome path and sample payload', async () => {
  const root = await fixture()
  writeFileSync(path.join(root, 'tools/canvas-cli/test/fixtures/playground-new-user-welcome.json'), JSON.stringify({
    apiVersion: 'canvas/v1',
    kind: 'Journey',
    metadata: { name: 'new-user-welcome' },
    spec: {
      nodes: [
        { id: 'segment', type: 'condition', config: { expression: 'user.lifecycleStage == "new"' } },
        { id: 'message', type: 'message.send', config: { channel: 'sms' } },
      ],
      edges: [
        { from: 'segment', to: 'message' },
      ],
    },
    samplePayload: { user: { lifecycleStage: 'returning' } },
  }, null, 2))

  const result = verifyPlaygroundRuntimeSmoke(root)

  assert.equal(result.ok, false)
  assert.match(result.errors.join('\n'), /fixture must include coupon node with type coupon\.grant/)
  assert.match(result.errors.join('\n'), /fixture must include segment -> coupon edge/)
  assert.match(result.errors.join('\n'), /samplePayload must satisfy segment expression/)
})

test('rejects WireMock and doc drift from runtime smoke contract', async () => {
  const root = await fixture()
  writeFileSync(path.join(root, 'wiremock/mappings/demo-catalog.json'), JSON.stringify({
    mappings: [
      {
        request: { method: 'GET', url: '/mock/demo/golden-path' },
        response: { jsonBody: { steps: ['review new-user-welcome'] } },
      },
      {
        request: { method: 'GET', url: '/mock/demo/templates' },
        response: { jsonBody: { templates: [{ key: 'new-user-welcome', requiredPlugins: ['canvas-plugin-message'] }] } },
      },
      {
        request: { method: 'GET', url: '/mock/demo/plugins' },
        response: { jsonBody: { plugins: [{ key: 'canvas-plugin-message', enabled: true, mode: 'mock' }] } },
      },
    ],
  }, null, 2))
  writeFileSync(path.join(root, 'docs/open-source/playground.md'), [
    '# Playground',
    '',
    'Runtime smoke remains a future task.',
  ].join('\n'))

  const result = verifyPlaygroundRuntimeSmoke(root)

  assert.equal(result.ok, false)
  assert.match(result.errors.join('\n'), /\/mock\/demo\/golden-path must reference mock AI risk audit/)
  assert.match(result.errors.join('\n'), /\/mock\/demo\/templates new-user-welcome must require canvas-plugin-webhook/)
  assert.match(result.errors.join('\n'), /docs\/open-source\/playground\.md must reference node tools\/open-source-growth\/playground-runtime-smoke\.mjs/)
  assert.match(result.errors.join('\n'), /must not frame local runtime smoke as only a future task/)
})
